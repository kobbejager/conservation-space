import {View, Inject} from 'app/app';
import {Widget, WidgetConfig} from 'idoc/widget/widget';

@Widget
@View({
  template: `<div>
    <div class="idoc-mode">{{ debugWidget.mode }}</div>
    <div class="idoc-modeling">{{ debugWidget.modeling }}</div>
    <div class="current-object-type">{{ debugWidget.type }}</div>
    <div class="current-object-id">{{ debugWidget.id }}</div>
  </div>`
})
class DebugWidget {

  ngOnInit() {
    this.mode = this.context.getMode();
    this.modeling = this.context.isModeling();

    this.context.getCurrentObject().then(currentObject => {
      this.type = currentObject.instanceType;
      this.id = currentObject.id;
    });
  }
}

@WidgetConfig
@View({
  template: '<div></div>'
})
class DebugWidgetConfig {

  constructor() {
  }
}