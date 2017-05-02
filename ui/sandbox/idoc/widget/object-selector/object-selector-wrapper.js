import {Component, View, Inject, NgCompile, NgScope} from 'app/app';
import {PromiseAdapter} from 'adapters/angular/promise-adapter';
import {InstanceObject} from 'idoc/idoc-context';
import _ from 'lodash';
import objectSelectorWrapperTemplate from 'object-selector-wrapper-template!text';

const CONTAINER = ".container";

@View({template: objectSelectorWrapperTemplate})
@Component({selector: 'object-selector-wrapper'})
@Inject(PromiseAdapter, NgCompile, NgScope)
export class ObjectSelectorWrapper {

  constructor(promiseAdapter, $compile, $scope) {
    this.$compile = $compile;
    this.$scope = $scope;

    this.context = this.getCurrentContext();
    this.execute(1);
  }

  build(modification) {
    this.selectorConfig = _.merge(this.getConfig(), modification);
    var component = $(`<seip-object-selector config="objectSelectorWrapper.selectorConfig"
                        context="objectSelectorWrapper.context"></seip-object-selector>`);
    $(CONTAINER).html(this.$compile(component)(this.$scope)[0]);
  }

  execute(operation) {
    switch (operation) {
      // default operation executed on startup
      case 1:
        this.build();
        break;
      // excluding option 'current'
      case 2:
        this.build({
          excludeOptions: ['current']
        });
        break;
      // hide object option bar
      case 3:
        this.build({
          renderOptions: false
        });
        break;
      case 4:
        this.build({
          showIncludeCurrent: true
        });
        break;
    }
  }

  getCurrentContext() {
    return {
      getCurrentObject: () => {
        return Promise.resolve({
          getContextPath: () => {
            return [{'id': '1'}];
          },
          getId: () => {
            return '1';
          }
        });
      }
    };
  }

  getConfig() {
    var today = new Date();
    if (today.getDate() === 1) {
      today.setDate(2);
    }
    var yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);
    yesterday.setHours(0, 0, 0, 0);
    today.setHours(0, 0, 0, 0);

    return {
      criteria: {
        condition: 'OR',
        rules: [
          {
            condition: 'AND',
            rules: [
              {
                field: 'types',
                type: 'object',
                operation: 'equal',
                value: ['http://ittruse.ittbg.com/ontology/enterpriseManagementFramework#Document']
              }, {
                condition: 'AND',
                rules: [
                  {field: 'freeText', type: 'fts', operation: 'contains', value: 'initial'},
                  {field: 'emf:createdBy', type: 'object', operation: 'set_to', value: ['test_id']},
                  {
                    field: 'emf:createdOn',
                    type: 'dateTime',
                    operation: 'between',
                    value: [yesterday.toISOString(), today.toISOString()]
                  },
                  {field: 'rel:that', type: 'object', operation: 'set_to', value: ['test_id']}
                ]
              }
            ]
          }
        ]
      }
    };
  }
}