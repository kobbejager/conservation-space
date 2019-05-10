import {View, Component, Inject, NgScope, NgElement, NgTimeout} from 'app/app';
import {Eventbus} from 'services/eventbus/eventbus';
import {WidgetReadyEvent} from 'idoc/widget/widget-ready-event';
import {IdocEditorFactory} from './editor';
import {DynamicElementsRegistry} from 'idoc/dynamic-elements-registry';
import {IdocEditorListeners} from './idoc-editor-listeners';
import {EditorReadyEvent} from 'idoc/editor/editor-ready-event';
import {EditorResizedEvent} from 'idoc/editor/editor-resized-event';
import {IdocContentModelUpdateEvent} from 'idoc/events/idoc-content-model-update-event';
import {IdocUpdateUndoManagerEvent} from 'idoc/events/idoc-update-undo-manager-event';
import {RefreshWidgetsCommand} from 'idoc/actions/events/refresh-widgets-command';
import {BeforeIdocSaveEvent} from 'idoc/actions/events/before-idoc-save-event';
import {IdocTabOpenedEvent} from 'idoc/idoc-tabs/idoc-tab-opened-event';
import {ActionExecutedEvent} from 'services/actions/events';
import {AfterIdocSaveEvent} from 'idoc/actions/events/after-idoc-save-event';
import {TranslateService} from 'services/i18n/translate-service';
import {PromiseAdapter} from 'adapters/angular/promise-adapter';
import {PluginsService} from 'services/plugin/plugins-service';
import {ResizeDetectorAdapter} from 'adapters/resize-detector-adapter';
import {SanitizeIdocContentCommand} from 'idoc/events/sanitize-idoc-content-command';
import {IdocContentSanitizedEvent} from 'idoc/events/idoc-content-sanitized-event';
import {WidgetRemovedEvent} from 'idoc/widget/widget';

import 'idoc/widget/widget';
import editorColumnTemplate from './idoc-editor.html!text';

export const EDITOR_LINKS_SELECTOR = '[data-cke-saved-href]';

@Component({
  selector: 'idoc-editor',
  properties: {
    'editmode': 'editmode',
    'tab': 'tab',
    'context': 'context'
  }
})
@View({
  template: editorColumnTemplate
})
@Inject(NgScope, NgTimeout, NgElement, IdocEditorFactory, Eventbus, TranslateService, PromiseAdapter, PluginsService, ResizeDetectorAdapter, DynamicElementsRegistry)
export class Editor {

  constructor($scope, $timeout, $element, idocEditorFactory, eventbus, translateService, promiseAdapter, pluginsService, resizeDetectorAdapter, dynamicElementsRegistry) {
    this.$scope = $scope;
    this.$timeout = $timeout;
    this.$element = $element;
    this.idocEditorFactory = idocEditorFactory;
    this.eventbus = eventbus;
    this.translateService = translateService;
    this.promiseAdapter = promiseAdapter;
    this.pluginsService = pluginsService;
    this.resizeDetectorAdapter = resizeDetectorAdapter;
    this.dynamicElementsRegistry = dynamicElementsRegistry;

    this.ngModel = $element.controller('ngModel');
    this.editorClass = Editor.editorClass;
    this.events = [];
  }

  ngAfterViewInit() {
    this.init();

    this.$element.addClass('idoc-content-container idoc-editor');
    // tab id cannot be used because CKEditor has issues with UUIDs
    this.editorId = 'editor' + Math.round(Math.random() * 10000);
    this.$element.attr('id', this.editorId);

    let editorWrapper = this.$element.parent();
    this.editorDimensions = {
      width: editorWrapper.width(),
      height: editorWrapper.height()
    };
    this.editorIdocEditorResizeListener = this.resizeDetectorAdapter.addResizeListener(editorWrapper[0], this.onIdocEditorResize.bind(this, editorWrapper));
  }

  init() {
    this.subscribeEventHandlers();
    this.initEditModeWatcher();
  }

  initEditModeWatcher() {
    //This $watch is a workaround placed here because of the following problem:
    //Each time idoc is switched between edit/preview mode, its child components (navigation,idoc-editor,comments) are destroyed for some reason(?).
    //This implies recreating idoc-editor every time and the need to synchronize its full initialization with the editmode change.
    //Currently, there are too many synchronizations here because of this, which makes this code error-prone.
    this.$scope.$watch(() => {
      return this.editmode;
    }, () => {
      // editor is recreated because there is no other way to load proper plugins at runtime
      if (this.editor) {
        this._setModelValue(this.editor.getData());
        this.idocEditorFactory.destroy(this.editor);
      }
      this.idocEditorFactory.init(this.$element[0], this.editmode).then((editor) => {
        this.editor = editor;

        if (this.editmode) {
          new IdocEditorListeners({
            eventbus: {
              instance: this.eventbus,
              channel: this.tab.id
            }
          }).listen(this.editor);
        }

        //wait for the editor to be fully initialized
        this.onEditorReady(this.editor).then(() => {
          this.handleEditModeChange();
          this.forceImageContextMenuToShow();

          // Subscribe after editor is initialized to avoid overriding content with empty value
          this.events.push(this.eventbus.subscribe(IdocContentModelUpdateEvent, () => {
            this._setModelValue(this.editor.getData());
          }));

          this.events.push(this.eventbus.subscribe(SanitizeIdocContentCommand, (event) => {
            if (this.tab.id === event[0].tabId) {
              // empty spans are appended with Zero Width Non-Joiner (&#8204) to prevent missing empty lines, missing font-size and format, missing
              // inline widget and malformed export to word
              this.chainProcessors([this.contentProcessors.IdocEditorContentProcessor], 'preprocessContent').then(() => {
                this.eventbus.publish(new IdocContentSanitizedEvent({
                  sanitizedContent: this.editor.getData(),
                  tabId: this.tab.id
                }));
              });
            }
          }));

          this.calculateMinimumEditorHeight();
          this.eventbus.publish(new EditorReadyEvent({editorId: this.editorId, editorName: this.editor.name}));

          this.editor.resetDirty();
          this.context.getCurrentObject().then((object) => {
            this.onEditorChange(object);
          });
        });
      });
    });
  }

  subscribeEventHandlers() {
    this.events = [
      this.eventbus.subscribe(IdocTabOpenedEvent, (tab) => {
        // Timeout is needed to execute editor focus after tab click event
        // otherwise focus is gained by tab.
        if (this.editmode && this.tab.id === tab.id) {
          this.$timeout(() => {
            this.editor.focus();
          }, 0);
        }
      }),
      this.eventbus.subscribe(IdocUpdateUndoManagerEvent, (tab) => {
        if (this.editmode && this.tab.id === tab.id) {
          this.editor.undoManager.update();
        }
      }),
      // As part of it's undo mechanism, CKEditor adds a hidden node to the DOM every time a widged is added. Part of the code as follows:
      // http://docs.ckeditor.com/source/selection.html
      //
      // Creates cke_hidden_sel container and puts real selection there.
      // function hideSelection( editor, ariaLabel ) {
      //  var content = ariaLabel || '&nbsp;',
      //    style = CKEDITOR.env.ie && CKEDITOR.env.version < 14 ? 'display:none' : 'position:fixed;top:0;left:-1000px',
      //    hiddenEl = CKEDITOR.dom.element.createFromHtml(
      //      '<div data-cke-hidden-sel="1" data-cke-temp="1" style="' + style + '">' + content + '</div>',
      //      editor.document );
      //    ...
      // The problem occurs by inserting all types of widgets.
      // This subscribe resets the content of variable "content", as is not removed by CKEditor when the node is deleted.
      this.eventbus.subscribe(WidgetReadyEvent, () => {
        if (this.editor._.hiddenSelectionContainer) {
          this.editor._.hiddenSelectionContainer.$.innerText = '';
        }
        // unlock undo manager and save editor state
        this.editor.fire('unlockSnapshot');
        this.editor.fire('saveSnapshot', {contentOnly: true});
      }),
      this.eventbus.subscribe(ActionExecutedEvent, (action) => {
        // frorceRefresh is an action's indicator, that this action does some entity changes.
        // Used to reapply lazyload in order to be able to reload widgets
        if (action.action.forceRefresh) {
          this.processWidgetsForReload();
        }
      }),

      // Command is fired that should refresh widgets although is not performed from an action.
      this.eventbus.subscribe(RefreshWidgetsCommand, () => {
        if (!this.editmode) {
          this.processWidgetsForReload();
        }
      }),

      this.eventbus.subscribe(AfterIdocSaveEvent, () => {
        this.editor.resetDirty();
        this.context.getCurrentObject().then((object) => {
          object.setDirty(false);
        });
      }),

      this.eventbus.subscribe(BeforeIdocSaveEvent, () => {
        // managing snapshots while saving is obsolete and leads to problems with the editor.
        this.editor.fire('lockSnapshot');
      })
    ];
  }

  onEditorChange(currentObject) {
    this.currentObject = currentObject;
    this.editor.on('change', () => {
      if (!this.currentObject.isDirty()) {
        this.currentObject.setDirty(this.editor.checkDirty());
      }
      // When new image is inserted or copy-pasted corresponding mouseup handler should be bind.
      // TODO: We can listen for editor 'insertElement' and 'paste' events and call this function there.
      this.forceImageContextMenuToShow();
    });
  }

  processWidgetsForReload() {
    if (this.isInitialized) {
      this.context.setAllSharedObjectsShouldReload(true);
      this.chainProcessors([this.contentProcessors.EditorContentWidgetProcessor], 'postprocessContent').then(() => {
        this.editor.fire('unlockSnapshot');
      });
    }
  }

  onIdocEditorResize(editorWrapper) {
    this.calculateMinimumEditorHeight();
    let newWidth = editorWrapper.width();
    let newHeight = editorWrapper.height();
    let evtData = {
      editorId: this.editorId,
      width: newWidth,
      height: newHeight,
      widthChanged: this.editorDimensions.width !== newWidth,
      heightChanged: this.editorDimensions.height !== newHeight
    };
    this.editorDimensions.width = evtData.width;
    this.editorDimensions.height = evtData.height;
    this.eventbus.publish(new EditorResizedEvent(evtData));
  }

  handleEditModeChange() {
    this.editor.setReadOnly(!this.editmode);
    let editorElement = $(this.editor.element.$);
    this.toggleContentEditable(editorElement, this.editmode);
    //edit/preview css classes are needed to prevent widgets' drag handlers to appear when the document
    //is in preview mode. They are put inside this watch to avoid concurrency issue in the e2e tests.
    this.$element.removeClass('edit preview');
    this.$element.addClass(this.editmode ? 'edit' : 'preview');
    if (this.editmode) {
      editorElement.find(EDITOR_LINKS_SELECTOR).unbind('click');
    } else {
      this.attachClickHandlersToLinks(editorElement);
    }
  }

  forceImageContextMenuToShow() {
    // When image is placed in layout in IE11 focus of element is lost after right click and gained by layout.
    // This cause problems because image context menu can not be open. To prevent this layout is forced to lose
    // it's focus and image is focused again
    if (CKEDITOR.env.ie) {
      let editorElement = $(this.editor.element.$);
      let images = editorElement.find('.layout-container').find('img');
      images.off('mouseup');
      images.on('mouseup', (event) => {
        event.stopPropagation();
        $(this).focus();
        editorElement.find('.layout-column-editable').blur();
      });
    }
  }

  /**
   * Strips editor content for accurate comparison. All of the steps below are done so that comparison between
   * two contents is accurate:
   *  - Widgets are emptied from the content as their internal html is not needed for the scope of comparison.
   *  - layouts have specific ckeditor attributes removed
   *  - dinamically added html like drag handlers is removed
   *  - classes that are dinamically added are removed.
   * @param contents - idoc editor content
   * @param widgets - idoc editor widget
   * @returns strippedContent - object containin stripped content and widget references
   */
  stripContent(contents, widgets) {
    let strippedContent = {};
    strippedContent.widgetSelector = '[widget]';
    if (contents && contents.indexOf('widget-wrapper') !== -1) {
      // save reference to actual widgets in document.
      strippedContent.widgetRefs = {};
      Object.keys(widgets).forEach((widgetInstance)=> {
        var widget = widgets[widgetInstance].element;
        // layouts are widgets without Id, which are unneeded.
        if (widget.getId()) {
          strippedContent.widgetRefs[widget.getId()] = widget;
        }
      });

      // wrap contents using jquery object for easy manipulation.
      strippedContent.$contents = $('<div />').html(contents);
      var removeCKEAttributes = function (element) {
        // there attributes are not stored in the attribute nodemap
        element.removeAttribute('data-cke-filter');
        element.removeAttribute('data-cke-widget-id');
        element.removeAttribute('data-cke-widget-data');
        element.removeAttribute('data-cke-widget-keep-attr');
        element.removeAttribute('tabindex');
        // remove because of DTW widget
        element.removeAttribute('data-widget');
        element.removeAttribute('style');
        // remove because of image widget
        element.removeAttribute('config');
        element.removeAttribute('temp-save');
        element.removeAttribute('data-value');
        // can be not present depending in some off cases like spamming the undo button.
        element.classList.remove('initialized');
        element.innerHTML = '';
        // remove other cke attributes that might be present.
        $.each(element.attributes, function () {
          if (this && this.name.indexOf('cke') !== -1) {
            element.removeAttribute(this.name);
          }
        });
      };
      // strip and store widget information.
      strippedContent.$contents.find('[widget]').each(function () {
        var $this = $(this);
        removeCKEAttributes(this);
        // remove contents of widget
        $this.children().eq(0).empty();
      });
      // these items might be dinamically added even on hover so they are removed.
      strippedContent.$contents.find('.cke_widget_drag_handler_container,#configToolbar-panel,[data-cke-hidden-sel]').remove();
      // layouts are a form of widgets to CKEDITOR
      var layouts = strippedContent.$contents.find('[data-cke-widget-id]');
      if (layouts.length) {
        $.each(layouts, function () {
          this.removeAttribute('data-cke-widget-id');
        });
      }
      // classes that are dinamically added to layouts must be removed also.
      strippedContent.strippedContents = strippedContent.$contents.html().replace('cke_widget_editable_focused', '').replace(/cke_widget_editable\s{0,1}/g, '');
    }
    return strippedContent;
  }

  /**
   * Returns a promise indicating when the editor is fully initialized.
   *
   * @param editor is the editor instance
   * @returns {Promise} when the editor is fully initialized
   */
  onEditorReady(editor) {
    return this.promiseAdapter.promise((resolve) => {

      editor.stripContentCallback = this.stripContent;

      this.editor.on('beforeCommandExec', (command) => {
        if (command.data.name === 'undo' || command.data.name === 'redo') {
          this.doNotResetUndo = true;
          this.$scope.$broadcast(WidgetRemovedEvent.EVENT_NAME, new WidgetRemovedEvent());
        }
      });

      // Fix for http://ckeditor.com/forums/Support/inline-editor-div-contenteditable-when-loaded-hidden-doesnt-work.
      editor.on('instanceReady', (ev) => {
        // in the event of switching the content while editor is initializing,
        // further executions must be stopped in order to prevent leakage/falsy initialization.
        if (this.destroyed) {
          return this.promiseAdapter.resolve(this.destroyed);
        }
        this.dynamicElementsRegistry.handleEditor(this.editor.name);
        let editorElement = $(ev.editor.element.$);
        editor.lang.button.selectedLabel = '%1';
        let shouldSkipDrop;
        editor.on('dragstart', (ev) => {
          editorElement.one('mouseup', (mouseEv) => {
            let $target = $(mouseEv.target);
            // if cursor does not move away from the handler box, cancel drop event.
            shouldSkipDrop = $target.is('img') && $target.hasClass('cke_widget_drag_handler');
          });

          let dragHandlerWrapper = ev.data.target.getParent();
          let draggedElement = dragHandlerWrapper.getParent();
          // if the drag icon is used to drag a widget check if empty paragraph needs to be added.
          if (dragHandlerWrapper.hasClass('cke_widget_drag_handler_container') && draggedElement.hasClass('widget-wrapper')) {
            let elementContainer = draggedElement.getParent();
            // if only the widget is a child of its container add an empty paragraph.
            if (elementContainer.getChildCount() === 1) {
              let p = new CKEDITOR.dom.element('p');
              p.append(new CKEDITOR.dom.element('br'));
              elementContainer.append(p);
            }
          }
        }, null, null, 1);

        // When clicking on widgets inside layouts might register a drop event to the layoutmanager
        // which causes inserting widgets between layout container and row containers. see CMF-27606
        editor.on('drop', (ev) => {
          if (shouldSkipDrop || ev.data.target.hasClass('layoutmanager')) {
            shouldSkipDrop = false;
            ev.cancel();
          }
        }, null, null, 1);

        //When layout is inserted we have to wait awhile before adding a new line
        editor.on('layoutmanager:layout-inserted', () => {
          setTimeout(() => {
            let ranges = editor.getSelection().getRanges();
            editor.getSelection().selectRanges(ranges);
          }, 100);
        });

        // After instating all plugins, invokes their before editor set content function. chainProcessors function ensures "synchronous" execution.
        // After editor.setData() the chainProcessors function is executed again with after editor set content processors' function.
        return this.pluginsService.loadPluginServiceModules('editor-content-processors', 'name', false).then((loadedProcessors) => {
          this.contentProcessors = loadedProcessors;
          let processors = $.map(loadedProcessors, function (value) {
            return [value];
          });
          this.processContent(processors, editor, resolve);
        });
      });
    });
  }

  processContent(processors, editor, resolve) {
    return this.chainProcessors(processors, 'preprocessContent', this.ngModel.$viewValue).then((result) => {
      editor.setData(result, {
        noSnapshot: true,
        callback: () => {
          return this.chainProcessors(processors, 'postprocessContent').then(() => {
            if (this.editmode) {
              // sets focus over CKEditor editable range and moves to the root position
              let range = editor.createRange();
              range.moveToElementEditEnd(range.root);
              editor.getSelection().selectRanges([range]);
              editor.focus();
            }
            if (!this.doNotResetUndo) {
              this.editor.resetUndo();
            }
            this.doNotResetUndo = false;
            this.$element.addClass('initialized');
            this.isInitialized = true;
            resolve(editor);
          });
        }
      });
    });
  }

  // Synchronous invokes processors functions.
  chainProcessors(processors, funcName, initialValue, index) {
    if (!index) {
      index = 0;
    }

    if (processors.length < index) {
      return this.promiseAdapter.resolve(initialValue);
    }

    let converterExecutor = this.promiseAdapter.resolve(processors[index][funcName](this, initialValue));

    if (processors[index + 1]) {
      return converterExecutor.then((result) => {
        return this.chainProcessors(processors, funcName, result, index + 1);
      });
    }
    return converterExecutor;
  }

  /**
   * Attaches click handlers to the anchor elements within the given parent.
   * This is needed, because CKEditor prevents navigating to external pages when it is in preview mode.
   *
   * @param parentElement is the parent element as jquery object
   */
  attachClickHandlersToLinks(parentElement) {
    parentElement.find(EDITOR_LINKS_SELECTOR).click(function (event) {
      if (this.href) {
        window.open(this.href, 'new' + event.screenX);
      }
    });
  }

  /**
   * Toggles the elements' contenteditable attribute, according to the passed editMode.
   * This is a workaround for the following CkEditor bug: http://dev.ckeditor.com/ticket/12134
   * which is expected to be fixed in CkEditor release 4.6.0
   *
   * @param parentElement
   *                      is the parent element as jquery object
   * @param editMode
   *                is the edit mode of the idoc
   */
  toggleContentEditable(parentElement, editMode) {
    parentElement.find('.cke_widget_editable').each(function () {
      $(this).attr('contenteditable', editMode.toString());
    });
  }

  ngOnDestroy() {
    // if page is switched, before ckeditor throws `instanceReady` event,
    // notify handlers to stop further execution to prevent leakage.
    this.destroyed = true;
    for (let event of this.events) {
      event.unsubscribe();
    }
    if (this.editor) {
      this.idocEditorFactory.destroy(this.editor);
    }
    // remove the resize listener
    this.editorIdocEditorResizeListener();
    this.stripContent = null;
  }

  /**
   * Safely sets model value.
   * @param newValue to be set
   */
  _setModelValue(newValue) {
    if (this.ngModel) {
      this.ngModel.$setViewValue(newValue);
    }
  }

  /**
   * Stretches the editor to the visible window bottom if there is no content that would otherwise expand it.
   */
  calculateMinimumEditorHeight() {
    if (this.$element.css('visibility') !== 'hidden') {
      let topOffset = this.$element.offset().top;
      this.$element.css({
        'min-height': `calc(100vh - ${topOffset}px)`
      });
    }
  }
}

Editor.editorClass = 'ck-editor-area';
