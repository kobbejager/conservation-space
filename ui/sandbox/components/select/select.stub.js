import {Component, View, Inject} from 'app/app';
import selectTemplateStub from 'select-template!text';
import {Select} from 'components/select/select';
import {ResourceSelect} from 'components/select/resource/resource-select';
import {DataLoaderService} from 'data-loader-service';

@Component({
  selector: 'seip-select-stub'
})
@View({
  template: selectTemplateStub
})
@Inject('DataLoaderService')
export class SelectStub {
  constructor(dataLoaderService) {
    this.selectConfig = {
      dataLoader: dataLoaderService.getData(),
      dataConverter: this._dataConverter,
      data: [{id: 'key4', text: 'Value 4'}],
      defaultValue: 'key4'
    };

    this.selectDefaultToSingleConfig = {
      data: [{id: 'key', text: 'Value'}],
      multiple: false,
      defaultToSingleValue: true
    };

    this.selectDefaultToFirstConfig = {
      data: [
        {id: 'key1', text: 'Value 1', disabled: true},
        {id: 'key2', text: 'Value 2'}
      ],
      multiple: false,
      defaultToFirstValue: true
    };

    this.resourceSelectConfig = {
      dataLoader: () => {
        return new Promise((resolve, reject) => {
          resolve({
            data: {
              items: [{
                id: "emf:admin",
                label: "Administrator",
                type: "user",
                value: "admin"
              }]
            }
          })
        });
      }
    }
  }

  _dataConverter(data) {
    let result = [];
    for (let record in data) {
      result.push({
        id: record,
        text: data[record]
      })
    }
    return result;
  }
}
