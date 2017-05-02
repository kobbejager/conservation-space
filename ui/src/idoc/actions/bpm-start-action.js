import {Injectable, Inject} from 'app/app';
import {BpmTransitionAction} from 'idoc/actions/bpm-action';
import {SaveDialogService} from 'idoc/save-idoc-dialog/save-dialog-service';
import {ActionsService} from 'services/rest/actions-service';
import {ValidationService} from 'form-builder/validation/validation-service';
import {BpmService} from 'services/rest/bpm-service';
import {Logger} from 'services/logging/logger';
import {Eventbus} from 'services/eventbus/eventbus';
import {InstanceRestService} from 'services/rest/instance-service';
import {TranslateService} from 'services/i18n/translate-service';
import {NotificationService} from 'services/notification/notification-service';
import {PromiseAdapter} from 'adapters/angular/promise-adapter';

const OPERATION = 'bpmStart';

/**
 * Action handler for BpmStart.
 */
@Injectable()
@Inject(ActionsService,ValidationService, SaveDialogService,InstanceRestService, Eventbus, Logger, PromiseAdapter, BpmService, TranslateService, NotificationService)
export class BpmStartAction extends BpmTransitionAction {

  constructor(actionService, validationService, saveDialogService, instanceRestService, eventbus, logger,promiseAdapter, bpmService, translateService, notificationService) {
    super(actionService, validationService, saveDialogService,instanceRestService, eventbus, logger,promiseAdapter, bpmService, translateService, notificationService);
  }


  /**
   * Executes the bpmTransition.
   *
   * @param context The action context
   * @param actionDefinition the action definition
   * @param models Models for the object for which this transition should be performed.
   */
  executeTransition(context, actionDefinition, models) {
    let currentObjectId = context.currentObject.getId();
    let actionPayload = this.bpmService.buildBPMActionPayload(currentObjectId, actionDefinition, models, OPERATION);
    return this.bpmService.startBpm(currentObjectId, actionPayload).then((response) => {
        let createdObjectHeader = this.translateService.translateInstantWithInterpolation('bpm.transition', { transitionId : actionDefinition.label });
        if(response.data.length > 0){
          createdObjectHeader += this.translateService.translateInstant('bpm.created');
        }
        response.data.reduce((item) => {
            createdObjectHeader += response.data[item].headers.breadcrumb_header + '<br>';
        }, 0);
        this.notificationService.success(createdObjectHeader);
        this.notifyOnUpdate(undefined, response);
        return response;
    });
  }

}
