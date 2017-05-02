import {View, Inject, NgElement} from 'app/app';
import {Widget, WidgetConfig} from 'idoc/widget/widget';

@Widget
@View({
  template: `<div>
    <div class="hello-title">Title: {{ helloWidget.title }}</div>
    <div><span class="hello-message">Hello {{ helloWidget.name }}</span></div>
  </div>`
})
@Inject('$scope', NgElement)
class HelloWidget {
  constructor($scope, element) {
    this.name = this.config.name;

    $scope.$watch(() => this.config.name, () => {
      this.name = this.config.name;
    });

    $scope.$watch(() => this.config.title, () => {
      this.title = this.config.title;
    });

    this.element = element;
  }

  ngOnInit() {
    $('body').append('<div id="onInit"></div>');
  }

  ngAfterViewInit() {
    $('body').append('<div id="ngAfterViewInit">' + this.element.find('.hello-message').text() + '</div>');
  }

  ngOnDestroy() {
    $('body').append('<div id="onDestroy"></div>');
  }
}

@WidgetConfig
@View({
  template: '<div><input class="name" ng-model="helloWidgetConfig.config.name" /></div>'
})
class HelloWidgetConfig {

  constructor() {
  }
}