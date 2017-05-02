import {Component,View,Inject,NgTimeout} from 'app/app';
import 'idoc/sidebar/sidebar';
import template from './template.html!text';
import {WindowAdapter} from 'adapters/angular/window-adapter';
import {Eventbus} from 'services/eventbus/eventbus';

@Component({
  selector: 'seip-sidebar-bootstrap'
})
@View({
  template: template
})
@Inject(NgTimeout, Eventbus)
class SidebarBootstrap {

  constructor($timeout, eventbus) {
    var path = [{
      compactHeader: 'Root',
      id: "emf:660390b4-2951-4250-ae40-8c8ba49eed54",
      type: "projectinstance"
    }, {
      compactHeader: 'Child',
      id: "emf:660390b4-2951-4250-ae40-8c8ba49eed54",
      type: "documentinstance"
    }];

    this.context = {
      getCurrentObject: function () {
        var entity = {
          id: path[1].id,
          getContextPath: function () {
            return path;
          }
        };

        return {
          then: function (f) {
            f(entity);
          }
        };
      }
    }
  }
}