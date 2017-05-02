import {Component, Inject, NgElement, NgScope} from 'app/app';
import {Select} from 'components/select/select';

@Component({
  selector: 'seip-instance-select',
  properties: {
    'config': 'config'
  }
})

@Inject(NgElement, NgScope, '$timeout')
export class InstanceSelect extends Select {

  constructor($element, $scope, $timeout) {
    super($element, $scope, $timeout);

    this.$scope.$watch(()=> {
      return this.config.data;
    }, () => {
      this.createActualConfig();
      this.$element.empty();
      this.initSelect();
    });
  }
}