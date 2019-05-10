"use strict";

class ContextSelector {

  constructor(element) {
    if (!element) {
      throw new Error('Context selector element must be provided to the constructor!');
    }
    this.element = element;
  }

  getSelectButton() {
    return this.element.$('.open-picker-btn');
  }

  isSelectContextButtonVisible() {
    browser.wait(EC.visibilityOf(this.getSelectButton()), DEFAULT_TIMEOUT, 'Select context button should be visible!');
    return true;
  }

  clickSelectButton() {
    let selectButton = this.getSelectButton();
    browser.wait(EC.elementToBeClickable(selectButton), DEFAULT_TIMEOUT, 'Context select button should be clickable!');
    selectButton.click();
  }

  getClearContextButton() {
    return this.element.$('.clear-context-btn');
  }

  isClearContextButtonVisible() {
    browser.wait(EC.visibilityOf(this.getClearContextButton()), DEFAULT_TIMEOUT, 'Clear context button should be visible in context selector!');
    return true;
  }

  clickClearContextButton() {
    this.getClearContextButton().click();
  }

  getContextPathElement() {
    return this.element.$('.context-path');
  }

  getContextPathText() {
    return this.getContextPathElement().getText();
  }

  getContextIdElement() {
    return element(by.id('selected-item'));
  }

  getContextIdText() {
    return this.getContextIdElement().getAttribute('value');
  }

  hasError() {
    return this.element.$('.text-danger').isPresent();
  }

  getErrorMessageElement() {
    let errorMessageElement = this.element.$('.text-danger');
    browser.wait(EC.visibilityOf(errorMessageElement), DEFAULT_TIMEOUT);
    return errorMessageElement;
  }

  getErrorMessage() {
    return this.getErrorMessageElement().getText();
  }

  selectContextElement(objectNumber) {
    let items = element.all(by.css('.instance-header'));
    let item = items.get(objectNumber);
    browser.wait(EC.visibilityOf(item), DEFAULT_TIMEOUT);
    item.click();
    element(by.css('.seip-btn-ok')).click();
  }
}

module.exports = ContextSelector;
ContextSelector.SELECTOR_CONTEXT_SELECTOR = '.selectionModeDefault';
ContextSelector.SELECTOR_CONTEXT_SELECTOR_SELECTION_MODE_BOTH = '.selectionModeBoth';
ContextSelector.SELECTOR_CONTEXT_SELECTOR_SELECTION_MODE_IN_CONTEXT = '.selectionModeInContext';
ContextSelector.SELECTOR_CONTEXT_SELECTOR_SELECTION_MODE_WITHOUT_CONTEXT = '.selectionModeWithout';