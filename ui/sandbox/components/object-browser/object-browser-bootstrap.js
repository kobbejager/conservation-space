import {Component, View, Inject} from 'app/app';
import 'components/object-browser/object-browser';
import template from './template.html!text';
import {WindowAdapter} from 'adapters/angular/window-adapter';
import data from 'sandbox/components/object-browser/object-browser-data.json!';
import _ from 'lodash';

@Component({
  selector: 'seip-object-browser-bootstrap'
})
@View({
  template: template
})
@Inject(WindowAdapter)
class ObjectBrowserBootstrap {

  constructor(window) {
    var hash = window.location.hash;
    var entityId = hash.substring(hash.indexOf('=') + 1);
    var nodePath = this.constructNodePath(entityId);

    this.config = {
      id: entityId,
      rootId: 'emf:99fd21fc-678e-405a-84f0-eb83d69aa415',
      rootText: 'Root node',
      selectable: true,
      onSelectionChanged: (id) => {
        this.selectedId = id;
      },
      nodePath: nodePath
    }
  }

  constructNodePath(entityId) {
    var path = [];

    var cnPath = this.findPathById(entityId);
    cnPath.split('/').forEach((value) => {
      // if the node exists in the data model means that it's not synthetic (like documents, cases, etc. nodes)
      if (this.findPathById(value)) {
        path.push({
          id: value
        });
      }
    });

    return path;
  }

  findPathById(id) {
    var cnPath;

    _.forEach(data, function (value, key) {
      if (_.endsWith(key, id)) {
        cnPath = key;
        return false;
      }
    });

    return cnPath;
  }
}