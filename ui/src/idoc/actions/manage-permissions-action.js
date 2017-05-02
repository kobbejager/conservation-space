import {Injectable, Inject} from 'app/app';
import {Logger} from 'services/logging/logger';
import {StateParamsAdapter} from 'adapters/router/state-params-adapter';
import {Router} from 'adapters/router/router';
import {InstanceAction} from 'idoc/actions/instance-action';
import {STATE_PARAM_ID, PERMISSIONS_TAB_ID} from 'idoc/idoc-page';
import {IDOC_STATE} from 'idoc/idoc-constants';
import {EDIT_PERMISSIONS_PARAM} from 'idoc/system-tabs/permissions/permissions';

@Injectable()
@Inject(Logger, Router, StateParamsAdapter)
export class ManagePermissionsAction extends InstanceAction {

  constructor(logger, router, stateParamsAdapter) {
    super(logger);
    this.router = router;
    this.stateParamsAdapter = stateParamsAdapter;
  }

  execute(action, actionContext) {
    let currentObjectId = actionContext.currentObject.getId();
    this.stateParamsAdapter.setStateParam(STATE_PARAM_ID, currentObjectId);
    this.stateParamsAdapter.setStateParam('#', PERMISSIONS_TAB_ID);
    this.stateParamsAdapter.setStateParam(EDIT_PERMISSIONS_PARAM, true);
    this.router.navigate(IDOC_STATE, this.stateParamsAdapter.getStateParams(), {reload: true});
  }
}