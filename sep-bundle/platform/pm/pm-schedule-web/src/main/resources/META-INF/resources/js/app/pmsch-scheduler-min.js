Ext.define("PMSch.GanttPanel",{extend:"Gnt.panel.Gantt",rightLabelField:"Responsible",highlightWeekends:true,showTodayLine:true,loadMask:true,cascadeChanges:true,enableProgressBarResize:true,initComponent:function(){Ext.apply(this,{lockedGridConfig:{width:500,title:"Tasks",collapsible:true,listeners:{beforeedit:function(a,b){if(b.field==="sm"){return b.record.data.s&&b.record.data.s==="Submitted"}}}},lockedViewConfig:{getRowClass:function(a){return a.isRoot()?"root-row":""},plugins:{ptype:"treeviewdragdrop",containerScroll:true}},schedulerConfig:{collapsible:true,title:"Schedule"},leftLabelField:{dataIndex:"title",editor:{xtype:"textfield"}},plugins:[Ext.create("PMSch.TaskContextMenu",{triggerEvent:"itemcontextmenu",scheduleController:this.scheduleController,ignoreParentClicks:true}),Ext.create("Sch.plugin.Pan"),Ext.create("Sch.plugin.TreeCellEditing",{clicksToEdit:2}),Ext.create("Gnt.plugin.TaskEditor",{tabsConfig:{items:[{title:"Log work",id:"logWorkTab",overflowY:"scroll",items:[{xtype:"container",id:"logWorkContainer"}],listeners:{show:function(d){var a=this.taskEditor.taskEditor.task;if(a!=null){var c=$.trim(a.data.aiid);var b=$("#logWorkContainer").attr("taskId");if(!b||b!==c){$("#logWorkContainer").attr("taskId",c);$("#logWorkContainer").empty();$("#logWorkContainer").extjsLogWorkOnTask({contextPath:SF.config.contextPath,taskId:c,preview:false})}}}.bind(this)}}]},taskFormClass:"PMSch.TaskForm",height:400,listeners:{loadtask:function(f,c){var e=c.data.et;var d=c.data.aiid;var a=f.tabs.activeTab;var b=f.tabs.child("#logWorkTab");if(e&&(e=="taskinstance"||e=="standalonetaskinstance")&&d&&d!=""){if(!b.tab.isVisible()){f.tabs.child("#logWorkTab").tab.show()}if(a&&a.title=="Log work"){a.fireEvent("show",a)}}else{b.tab.hide();if(a==b){f.tabs.setActiveTab(0)}}f.taskForm.openTaskForEdit(c);Ext.getCmp("generalTabForm").getForm().findField("e").setFieldLabel("Estimated effort")},afterupdatetask:function(b,a){Ext.getCmp("generalTabForm").updateRecord();b.task.setEndDate(new Date(b.task.getEndDate()-1));b.taskForm.destroyElements()},close:function(){this.taskEditor.taskForm.destroyElements()}}}),Ext.create("Ext.grid.plugin.BufferedRenderer")],tooltipTpl:new Ext.XTemplate('<strong class="tipHeader">{Name}</strong>','<table class="taskTip">','<tr><td>Start:</td> <td align="right">{[values._record.getDisplayStartDate("y-m-d")]}</td></tr>','<tr><td>End:</td> <td align="right">{[values._record.getDisplayEndDate("y-m-d")]}</td></tr>','<tr><td>Progress:</td><td align="right">{[ Math.round(values.pd)]}%</td></tr>',"</table>"),eventRenderer:function(a){if(a.get("Color")){var b=Ext.String.format("background-color: #{0};border-color:#{0}",a.get("Color"));return{style:b}}},columns:[{xtype:"sequencecolumn",align:"left",width:32,tdCls:"id"},{xtype:"wbscolumn",align:"left",width:48,tdCls:"id"},{xtype:"treecolumn",header:"Tasks",sortable:true,dataIndex:"title",width:200,tdCls:"title-column",field:{allowBlank:false},renderer:function(a,e,d){var c=d.raw.defType,b=a;if(c){b="("+c+") "+b}return b}},{header:"Task ID",dataIndex:"uid",id:"task-id",width:60,align:"center",renderer:function(a,g,f){var d=f.data.et;var c=f.data.aiid||f.raw.aiid;if(d&&c){var b=EMF.bookmarks.buildLink(d,c);var e='<a href="'+b+'" target="_blank">'+a+"</a>";return e}return a}},{header:"Start date",xtype:"startdatecolumn",id:"start-date",width:90,editor:{parentGanttPanel:this,listeners:{specialkey:function(d,c){if(c.getKey()==c.ENTER){var b=this.self.superclass.validateValue.call(this,d.getRawValue());if(!b){var a=Ext.String.format(d.invalidText,d.getRawValue(),d.format);this.parentGanttPanel.showErrorMessage(a)}}},validitychange:function(d,c,a){if(!c){var b=Ext.String.format(d.invalidText,d.getRawValue(),d.format);this.parentGanttPanel.showErrorMessage(b)}}}}},{header:"End date",xtype:"enddatecolumn",id:"end-date",width:90,editor:{parentGanttPanel:this,listeners:{specialkey:function(d,c){if(c.getKey()==c.ENTER){var b=this.self.superclass.validateValue.call(this,d.getRawValue());if(!b){var a=Ext.String.format(d.invalidText,d.getRawValue(),d.format);this.parentGanttPanel.showErrorMessage(a)}}},validitychange:function(d,c,a){if(!c){var b=Ext.String.format(d.invalidText,d.getRawValue(),d.format);this.parentGanttPanel.showErrorMessage(b)}}}}},{header:"Assignee",id:"assignee",width:100,xtype:"resourceassignmentcolumn",renderer:function(c,b,a){if(a.modified.resourceassignment&&b.tdCls.indexOf("x-grid-dirty-cell")==-1){b.tdCls+=" x-grid-dirty-cell"}return this.field.getDisplayValue(a)}},{header:"Task Status",id:"task-status",dataIndex:"s",width:70},{header:"Task Type",id:"task-type",dataIndex:"et",width:100},{header:"Subtype",id:"subtype",dataIndex:"tp",width:100},{header:"Trigger to start",id:"trigger-start",dataIndex:"sm",width:100,editor:{xtype:"combo",store:[["Auto","Auto"],["Manual","Manual"]]}},{xtype:"durationcolumn"},{xtype:"percentdonecolumn",width:50},{xtype:"addnewcolumn"}],tbar:new PMSch.Toolbar({gantt:this,scheduleController:this.scheduleController})});this.callParent(arguments);this.on("afterlayout",this.triggerLoad,this,{single:true,delay:100});this.on("afterrender",function(b){var d=null;var a=b.lockedGrid.getView().plugins;if(a){for(var c in a){if(a[c].ptype==="treeviewdragdrop"){d=a[c];break}}}var e=d.dropZone;e.isValidDropPoint=Ext.Function.createInterceptor(e.isValidDropPoint,function(q,x,f,v,y){if(!q||!y.item){return false}var p=this.view,u=p.getRecord(q),l=u.data.et?u.data.et:"",j=u.data.aiid?u.data.aiid:"",w=y.records,m=w.length,o=w.length,s,g;for(var s in w){var h=w[s];var t=h.data.et?h.data.et:"";var r=h.data.aiid?h.data.aiid:"";if(t==="project"||t==="taskinstance"){return false}else{var k=h.parentNode.data.Id;if(r!==""){var j=null;if(x==="append"){j=u.data.aiid}else{j=u.parentNode.data.aiid}if(j===""){return false}}if(l==="taskinstance"){return false}if(l==="workflowinstancecontext"&&x==="append"){return false}if(t==="caseinstance"||t==="workflowinstancecontext"||t==="taskinstance"){var n=null;if(x==="append"){n=u.data.Id}else{n=u.parentNode.data.Id}if(k!==n){return false}}}}})});this.taskStore.on({load:function(){this.body.unmask()},save:this.showLoadingMask,commit:this.showLoadingMask,scope:this});if(this.showTeamPanel){this.createAndAddTeamPanel()}},showLoadingMask:function(){this.body.mask("Loading...",".x-mask-loading")},showErrorMessage:function(a){Ext.Msg.show({title:"Error",msg:a,buttons:Ext.MessageBox.OK,icon:Ext.MessageBox.ERROR})},triggerLoad:function(){this.scheduleController.loadDataSet()},createAndAddTeamPanel:function(){var a=this.projectData.projectId;this.teamGrid=Ext.create("Ext.grid.Panel",{store:Ext.data.StoreManager.lookup("resourceStore"),region:"east",collapsible:true,border:false,split:true,width:400,layout:"fit",title:"Team",columns:[{text:"Name",flex:1,dataIndex:"Name",renderer:function(b,f,e){var c=SF.config.contextPath+"/project/resource-allocation-open.jsf?projectId="+a+"&usernames="+e.data.Id;var d='<a href="'+c+'">'+b+"</a>";return d}},{text:"Role",flex:1,dataIndex:"Role"},{text:"Job Title",flex:1,dataIndex:"JobTitle"}]});this.add(this.teamGrid)}});Ext.define("PMSch.TaskContextMenu",{extend:"Gnt.plugin.TaskContextMenu",id:"task-context-menu",menuConfig:{hideInactive:true},tasksWriter:new Ext.data.writer.Writer({writeAllFields:true}),entryToInstanceTypes:{projectinstance:"project",caseinstance:"case",workflowinstancecontext:"workflow",taskinstance:"task",standalonetaskinstance:"stask"},entryTypes:{projectinstance:"projectinstance",caseinstance:"caseinstance",workflowinstancecontext:"workflowinstancecontext",taskinstance:"taskinstance",standalonetaskinstance:"standalonetaskinstance"},entry_status:{},createMenuItems:function(){var a=this.callParent(arguments);return[{itemId:"openTask",text:"Open",id:"open-task",requiresTask:true,iconCls:"icon-open",scope:this,handler:function(){var d=this.rec;var e=d.data.et;var c=d.data.aiid||d.raw.aiid;if(e&&c){var b=EMF.bookmarks.buildLink(e,c);window.open(b,e+c+"")}else{}}},{itemId:"editDetails",text:"Edit task",id:"edit-task",requiresTask:true,iconCls:"icon-edit-task",scope:this,handler:function(){this.grid.taskEditor.showTask(this.rec)}},{itemId:"approve",text:"Commit task(s)",id:"commit-task",requiresTask:true,iconCls:"icon-commit-task",scope:this,handler:function(){this.scheduleController.commitSelectedTasks()}},{itemId:"stop",requiresTask:true,id:"stop-task",scope:this,iconCls:"icon-cancel-task",text:"Stop task(s)",handler:this.cancelTask},{itemId:"delete",handler:this.deleteTask,id:"delete-task",requiresTask:true,scope:this,iconCls:"icon-delete-task",text:"Delete task(s)"},{itemId:"indentTask",text:"Indent task(s)",id:"indent-task",requiresTask:true,iconCls:"icon-indent-task",scope:this,handler:function(){this.changeTaskIndentation(true)}},{itemId:"outdentTask",text:"Outdent task(s)",id:"outdent-task",requiresTask:true,iconCls:"icon-outdent-task",scope:this,handler:function(){this.changeTaskIndentation(false)}},{itemId:"changeTaskColor",text:"Change task color",id:"change-task-color",requiresTask:true,isColorMenu:true,iconCls:"icon-color-picker",menu:{showSeparator:false,id:"color-picker",items:[Ext.create("Ext.ColorPalette",{listeners:{select:function(c,b){this.rec.set("Color",b);this.hide()},scope:this}})]}},{itemId:"editLeftLabel",id:"edit-left-label",handler:this.editLeftLabel,requiresTask:true,scope:this,iconCls:"icon-edit-label",itemId:"editLeftLabel",text:"Edit left label"},{itemId:"editRightLabel",id:"edit-right-label",text:"Edit right label",handler:this.editRightLabel,requiresTask:true,scope:this,iconCls:"icon-edit-label"},{itemId:"addTaskMenu",text:"Add...",id:"add-task",iconCls:"icon-add-task",menu:{plain:true,id:"add-menu",items:[{itemId:"addTaskAbove",text:"Task above",handler:function(){this.addTaskAbove(this.addDefaultTaskDetails(this.rec))},requiresTask:true,scope:this,iconCls:"icon-add-above"},{itemId:"addTaskBelow",text:"Task below",handler:function(){this.addTaskBelow(this.addDefaultTaskDetails(this.rec))},scope:this,iconCls:"icon-add-below"},{itemId:"addMilestone",id:"milestone",text:"Milestone",handler:this.addMilestone,requiresTask:true,scope:this,iconCls:"icon-add-milestone"},{itemId:"addChild",text:"Sub-task",id:"subtask",handler:function(){var b=this.rec;b.addSubtask(this.addDefaultTaskDetails(b))},requiresTask:true,scope:this},{itemId:"addSuccessor",text:"Successor",handler:this.addSuccessor,requiresTask:true,scope:this},{itemId:"addPredecessor",text:"Predecessor",handler:this.addPredecessor,requiresTask:true,scope:this}]}},{itemId:"deleteDependencyMenu",text:"Delete dependency",requiresTask:true,iconCls:"icon-delete-dependency",isDependenciesMenu:true,menu:{plain:true,listeners:{beforeshow:this.populateDependencyMenu,mouseover:this.onDependencyMouseOver,mouseleave:this.onDependencyMouseOut,scope:this}}}]},cancelTask:function(){var a={confirm:{key:"Confirm",value:"Are you sure that you want the selected task to be stopped ?"},btnYResponse:"yes"};Ext.Msg.confirm(a.confirm.key,a.confirm.value,function(c){var b=c;if(b==a.btnYResponse){this.scheduleController.cancelTasks()}},this)},deleteTask:function(){var a={confirm:{key:"Confirm",value:"Are you sure that you want the selected task to be deleted ?"},btnYResponse:"yes"};Ext.Msg.confirm(a.confirm.key,a.confirm.value,function(c){var b=c;if(b==a.btnYResponse){var d=this.grid.getSelectionModel().selected;var e=this.scheduleController.getTopNodes(d.getRange());this.grid.taskStore.remove(e)}},this)},addDefaultTaskDetails:function(b){var c="1";var a=this.copyTask(b);a.setDuration(c);return a},configureMenuItems:function(){var a=this.rec;if(!a){return}this.evaluateActions(a)},activateMenu:function(b,a){a.stopEvent();this.rec=b;this.evaluated=false;this.contextMenuEvent=a;this.configureMenuItems()},contextMenuEvent:null,showMenu:function(a){this.showAt(a.getXY())},evaluateActions:function(b){var a=this.grid.getView().getSelectionModel().getSelection();if(a.length>1){this.evaluateActionsMultipleSelection(b,a)}else{this.evaluateActionsSingleSelection(b)}},evaluateActionsSingleSelection:function(f){var g=this;var c=f.data.Id||f.data.phid;var e={projectId:this.scheduleController.projectData.projectId,entry:{},parentEntry:null};var a=f.parentNode;if(a){e.parentEntry=g.tasksWriter.getRecordData(a);var d=a.childNodes;if(d&&d.length>0){e.parentEntry.children=[];Ext.each(d,function(h){e.parentEntry.children.push(g.tasksWriter.getRecordData(h))})}}e.entry=g.tasksWriter.getRecordData(f);if(f.getAllDependencies().length>0){e.entry.hasDependencies=true}var b=f.childNodes;if(b&&b.length>0){e.entry.children=[];Ext.each(b,function(h){e.entry.children.push(g.tasksWriter.getRecordData(h))})}Ext.Ajax.request({url:this.scheduleController.serviceUrl.evaluateActions,method:"POST",jsonData:e,entryId:c,success:this.buildMenu,scope:this})},evaluateActionsMultipleSelection:function(d,b){var e=this;var a=d.data.Id||d.data.phid;var c={projectId:this.scheduleController.projectData.projectId,entryId:a,entries:[]};Ext.each(b,function(g){var f={};f.entry=e.tasksWriter.getRecordData(g);f.parentEntry=g.parentNode?e.tasksWriter.getRecordData(g.parentNode):g.parentNode;c.entries.push(f)});Ext.Ajax.request({url:this.scheduleController.serviceUrl.evaluateMultipleActions,method:"POST",jsonData:c,entryId:a,success:this.buildMenu,scope:this})},buildMenu:function(g,j){var e={openTask:false,editDetails:false,approve:false,stop:false,"delete":false,indentTask:false,outdentTask:false,changeTaskColor:false,editLeftLabel:false,editRightLabel:false,addTaskMenu:false,addTaskAbove:false,addTaskBelow:false,addMilestone:false,addChild:false,addSuccessor:false,addPredecessor:false,deleteDependencyMenu:false};var f=Ext.decode(g.responseText)[j.entryId];var h=f.length;if(!f||h==0){return}var b=false;for(var a in e){var c=false;if(f[a]){c=true;b=true}var d="[itemId="+a+"]";var i=this.query(d);if(i&&i.length>0){i[0].setVisible(c)}}if(b){this.showMenu(this.contextMenuEvent)}},configColorChangeMenu:function(e){var b=this.query("[isColorMenu]")[0].menu.items.first(),d=b.getValue(),a=e.get("TaskColor"),c=null;if(b.el){if(d&&a&&a!==d){b.el.down("a.color-"+d).removeCls(b.selectedCls);if(b.el.down("a.color-"+a)){b.select(a.toUpperCase())}}else{if(d&&!a){b.el.down("a.color-"+d).removeCls(b.selectedCls)}}}},changeTaskIndentation:function(a){var b=this.grid.getView().getSelectionModel().getSelection();if(a){Ext.each(b,function(c){c.indent()})}else{Ext.each(b,function(c){c.outdent()})}}});Ext.define("PMSch.TaskForm",{extend:"Gnt.widget.taskeditor.TaskForm",id:"generalTabForm",startConfig:{format:SF.config.dateExtJSFormatPattern},finishConfig:{format:SF.config.dateExtJSFormatPattern},topSplitter:{},typeCombo:null,subTypeCombo:null,templates:{beforeLabelTextTpl:'<table class="gnt-fieldcontainer-label-wrap"><td width="1" class="gnt-fieldcontainer-label">',afterLabelTextTpl:'<td><div class="gnt-fieldcontainer-separator"></div></table>'},customComponents:["type","subType","err-label","labelSubType","splitterSubType","labelType","startMode","labelStartMode","splitterStartMode"],data:{typeStoreData:[],subTypeStoreData:[]},storeHolder:{typeStore:{},subTypeStore:{}},task:{},typeToCls:{projectinstance:"project",caseinstance:"case",workflowinstancecontext:"workflow",taskinstance:"task",standalonetaskinstance:"standalonetask"},constructor:function(a){this.callParent(arguments)},getSplitter:function(a,c){var b={id:c,xtype:"fieldcontainer",fieldLabel:a,labelAlign:"top",labelSeparator:"",beforeLabelTextTpl:this.templates.beforeLabelTextTpl,afterLabelTextTpl:this.templates.afterLabelTextTpl,layout:"hbox",defaults:{labelWidth:110,flex:1,allowBlank:false}};return b},openTaskForEdit:function(a){this.populateStartMode(a);this.populateTypeStore(a)},populateStartMode:function(a){Ext.getCmp("generalTabForm").add(this.getSplitter("Start Mode","splitterStartMode"));if(a.data.s&&a.data.s==="Submitted"){var c=Ext.create("Ext.data.Store",{fields:["value","label"],data:[{value:"Auto",label:"Auto"},{value:"Manual",label:"Manual"}]});var f=Ext.create("Ext.form.field.ComboBox",{id:"startMode",store:c,mode:"local",fieldLabel:"Start Mode",displayField:"label",valueField:"value",inputValue:"sm",name:"sm",width:286,labelWidth:110,defaultValue:"Auto"});Ext.getCmp("generalTabForm").add(f);var d=c.findExact("value",a.data.sm);if(d>=0){var b=c.getAt(d);f.setValue(b);f.fireEvent("select",f,b,d)}}else{var e={xtype:"label",id:"labelStartMode",text:"Start Mode: "+a.data.sm,margins:"0 0 0 10",style:{size:"13px",color:"#A8A8A8"}};Ext.getCmp("generalTabForm").add(e)}},populateTypeStore:function(a){Ext.getCmp("generalTabForm").add(this.getSplitter("Task Types","splitterSubType"));this.task=a;var l="/emf/service/schedule/task/allowedChildren";var j={};_this=this;var m=a.raw.tp;var f=a.parentNode.data.tp;var n=a.parentNode.data.et;var i=$.trim(a.data.aiid);if(i){var c={xtype:"label",id:"labelType",text:"Selected type: "+a.raw.et,style:{size:"13px",color:"#A8A8A8",display:"block"},margins:"0 0 0 10"};var o={xtype:"label",id:"labelSubType",text:"Selected definition: "+m,margins:"0 0 0 10",style:{size:"13px",color:"#A8A8A8"}};Ext.getCmp("generalTabForm").add(c);Ext.getCmp("generalTabForm").add(o)}else{if(f&&n){j=a.parentNode.data;var d=a.parentNode.childNodes;j.children=[];Ext.each(d,function(p){j.children.push(p.data)});var g={};Ext.Ajax.request({url:l,method:"POST",jsonData:j,async:false,success:function(p,t){var r=[];var q={};var s=Ext.decode(p.responseText);r=s.EntryType;q=s.type;g=q;_this.data.typeStoreData=r},failure:function(p,q){console.info("PMSch_AllowedChildrens_FAILED",p)}});if(!this.data.typeStoreData){return}this.storeHolder.typeStore=Ext.create("Ext.data.Store",{fields:["name","type"],autoLoad:true,autoSync:true,data:this.data.typeStoreData});var b=this.task.data.et;var h=this.storeHolder.typeStore.findExact("type",b);this.typeCombo=Ext.create("Ext.form.field.ComboBox",{fieldLabel:"Select Type",displayField:"name",valueField:"type",inputValue:"et",name:"et",width:286,id:"type",forceSelection:true,fireSelectEvent:true,labelWidth:110,mode:"local",store:this.storeHolder.typeStore,listeners:{select:function(s,p,q){var r=s.getValue();a.data.cls=_this.typeToCls[r];_this.data.subTypeStoreData=g[r];_this.generateNestedCombo(_this.task.data.tp);_this.storeHolder.subTypeStore.loadData(_this.data.subTypeStoreData,false);_this.storeHolder.subTypeStore.sync();Ext.getCmp("generalTabForm").add(_this.subTypeCombo)}}});Ext.getCmp("generalTabForm").add(_this.typeCombo);if(h>=0){var k=this.typeCombo.getStore().getAt(h);this.typeCombo.setValue(k);this.typeCombo.fireEvent("select",this.typeCombo,k,h)}}else{var e={xtype:"label",id:"err-label",text:"You must specify the type of parent element first",style:{size:"13px",color:"red"},margins:"0 0 0 10"};Ext.getCmp("generalTabForm").add(e)}}},destroyElements:function(){var c=this.customComponents;for(var b in c){var a=Ext.getCmp(c[b]);if(a!=null&&a!=undefined){a.destroy()}}},generateNestedCombo:function(d){var b=Ext.getCmp("subType");var c=undefined;if(b!=null&&b!=undefined){b.destroy()}this.storeHolder.subTypeStore=Ext.create("Ext.data.Store",{fields:["name","type"],autoLoad:true,autoSync:true,data:this.data.subTypeStoreData});c=this.storeHolder.subTypeStore.getAt(0);if(d){var a=this.storeHolder.subTypeStore.findExact("type",d);if(a>=0){c=d}}this.subTypeCombo=Ext.create("Ext.form.field.ComboBox",{fieldLabel:"Select sub-type",displayField:"name",valueField:"type",inputValue:"tp",name:"tp",width:286,id:"subType",labelWidth:110,mode:"local",value:c,store:this.storeHolder.subTypeStore})}});Ext.define("PMSch.Toolbar",{extend:"Ext.Toolbar",gantt:null,initComponent:function(){var a=this.gantt;var b=this.scheduleController;a.taskStore.on({"filter-set":function(){this.down("[iconCls=icon-collapseall]").disable();this.down("[iconCls=icon-expandall]").disable()},"filter-clear":function(){this.down("[iconCls=icon-collapseall]").enable();this.down("[iconCls=icon-expandall]").enable()},scope:this});Ext.apply(this,{items:[{xtype:"buttongroup",title:"Actions",columns:2,items:[{text:"Save",id:"save-schedule",iconCls:"icon-save-schedule",handler:function(){b.saveDataSet()}},{text:"Disable edit",id:"disable-edit",iconCls:"icon-disable-edit",handler:function(){var c=!a.isReadOnly();a.setReadOnly(c);if(c){this.setText("Enable edit");this.setIconCls("icon-enable-edit")}else{this.setText("Disable edit");this.setIconCls("icon-disable-edit")}}}]},{xtype:"buttongroup",title:"View tools",columns:3,items:[{iconCls:"icon-prev",text:"Previous",id:"previous",handler:function(){a.shiftPrevious()}},{iconCls:"icon-next",text:"Next",id:"next",handler:function(){a.shiftNext()}},{text:"Collapse all",id:"collapse-all",iconCls:"icon-collapseall",handler:function(){a.collapseAll()}},{text:"View full screen",id:"view-full-screen",iconCls:"icon-fullscreen",disabled:!this._fullScreenFn,scope:this,handler:function(){this.showFullScreen()}},{text:"Zoom to fit",id:"zoom-fit",iconCls:"zoomfit",handler:function(){a.zoomToFit()}},{text:"Expand all",id:"expand-all",iconCls:"icon-expandall",handler:function(){a.expandAll()}}]},{xtype:"buttongroup",title:"View resolution",columns:2,items:[{text:"6 weeks",id:"6-weeks",handler:function(){a.switchViewPreset("weekAndMonth")}},{text:"10 weeks",id:"10-weeks",handler:function(){a.switchViewPreset("weekAndDayLetter")}},{text:"1 year",id:"1-year",handler:function(){a.switchViewPreset("monthAndYear")}},{text:"5 years",id:"5-years",handler:function(){var c=new Date(a.getStart().getFullYear(),0);a.switchViewPreset("monthAndYear",c,Ext.Date.add(c,Ext.Date.YEAR,5))}}]},{xtype:"buttongroup",title:"Set percent complete",columns:5,defaults:{scale:"large",width:40},items:[{text:'0%<div class="percent percent0"></div>',id:"0-percent",scope:this,handler:function(){this.applyPercentDone(0)}},{text:'25%<div class="percent percent25"><div></div></div>',id:"25-percent",scope:this,handler:function(){this.applyPercentDone(25)}},{text:'50%<div class="percent percent50"><div></div></div>',id:"50-percent",scope:this,handler:function(){this.applyPercentDone(50)}},{text:'75%<div class="percent percent75"><div></div></div>',id:"75-percent",scope:this,handler:function(){this.applyPercentDone(75)}},{text:'100%<div class="percent percent100"><div></div></div>',id:"100-percent",scope:this,handler:function(){this.applyPercentDone(100)}}]},{xtype:"buttongroup",title:"More actions",columns:3,items:[{text:"Highlight critical path",iconCls:"togglebutton",id:"highlight-critical",enableToggle:true,handler:function(d){var c=a.getSchedulingView();if(d.pressed){c.highlightCriticalPaths(true)}else{c.unhighlightCriticalPaths(true)}}},{iconCls:"action",text:"Highlight tasks longer than 8 days",id:"longer-than-8-days",handler:function(c){a.taskStore.queryBy(function(d){if(d.data.leaf&&d.getDuration()>8){var e=a.getSchedulingView().getElementFromEventRecord(d);e&&e.frame("lime")}},this)}},{iconCls:"togglebutton",text:"Filter: Tasks with progress < 30%",id:"less-than-30-percent",enableToggle:true,toggleGroup:"filter",handler:function(c){if(c.pressed){a.taskStore.filterTreeBy(function(d){return d.get("pd")<30})}else{a.taskStore.clearTreeFilter()}}},{iconCls:"togglebutton",text:"Cascade changes",id:"cascade-changes",enableToggle:true,handler:function(c){a.setCascadeChanges(c.pressed)}},{iconCls:"action",text:"Scroll to last task",id:"scroll-last-task",handler:function(e){var d=new Date(0),c;a.taskStore.getRootNode().cascadeBy(function(f){if(f.get("ed")>=d){d=f.get("ed");c=f}});a.getSchedulingView().scrollEventIntoView(c,true)}},{xtype:"textfield",emptyText:"Search for task...",id:"search-task",width:150,enableKeyEvents:true,listeners:{keyup:{fn:function(g,f){var d=g.getValue();var c=new RegExp(Ext.String.escapeRegex(d),"i");if(d){a.taskStore.filterTreeBy(function(e){return c.test(e.get("Name"))})}else{a.taskStore.clearTreeFilter()}},buffer:300,scope:this},specialkey:{fn:function(d,c){if(c.getKey()===c.ESC){d.reset();a.taskStore.clearTreeFilter()}}}}}]}]});this.callParent(arguments)},applyPercentDone:function(a){this.gantt.getSelectionModel().selected.each(function(b){b.setPercentDone(a)})},showFullScreen:function(){this.gantt.el.down(".x-panel-body").dom[this._fullScreenFn]()},_fullScreenFn:(function(){var a=document.documentElement;if(a.requestFullscreen){return"requestFullscreen"}else{if(a.mozRequestFullScreen){return"mozRequestFullScreen"}else{if(a.webkitRequestFullScreen){return"webkitRequestFullScreen"}}}})()});Ext.define("PMSch.controller.ScheduleController",{gantt:null,taskStore:null,resourceStore:null,assignmentStore:null,dependencyStore:null,projectData:null,serviceUrl:{deleteTask:"/emf/service/schedule/task/delete",commitTask:"/emf/service/schedule/task/commit",updateTask:"/emf/service/schedule/task/update",view:"/emf/service/schedule/task/view",cancel:"/emf/service/schedule/task/cancel",evaluateActions:"/emf/service/schedule/actions/evaluate",evaluateMultipleActions:"/emf/service/schedule/actions/evaluateMultiple"},deleteCompleted:false,updateCompleted:false,constructor:function(a){Ext.apply(this,a||{})},reloadData:function(){this.dependencyStore.load();this.assignmentStore.load();this.loadDataSet()},extract:function(b){var a=[];Ext.each(b,function(c){a.push(c.data)});return a},populateDataStores:function(a,b){var d=Ext.decode(a.responseText);var c=d.data;this.scheduleConfig=d.scheduleConfig;if(c.dependencies){this.dependencyStore.loadData(c.dependencies)}if(c.assignments){this.assignmentStore.loadData(c.assignments)}if(c.resources){this.resourceStore.loadData(c.resources)}this.taskStore.removed=[];this.taskStore.setRootNode(c)},loadDataSet:function(){if(this.gantt){this.gantt.showLoadingMask()}var a=this;Ext.Ajax.request({url:this.serviceUrl.view+"?projectId="+this.projectData.projectId,method:"GET",success:this.populateDataStores,callback:function(){this.taskStore.fireEvent("load");this.checkProjectRestrictions.checkEditMode(this)},scope:this})},cancelTasks:function(){var c=this.getSelectedTasks();if(c&&c.length>0){var a=[];for(rec in c){a.push(c[rec].data.Id)}var b={recordId:a};Ext.Ajax.request({url:this.serviceUrl.cancel,method:"POST",jsonData:b,callback:function(){this.taskStore.fireEvent("cancel");this.loadDataSet()},scope:this})}},deleteData:function(){var c=this.taskStore.getRemovedRecords();c=this.sortSelectionHierarchically(c,true,false);var b=[];var h=this.assignmentStore.getRemovedRecords();Ext.each(h,function(k){b.push(k.getId())});var i=[];var j=this.dependencyStore.getRemovedRecords();Ext.each(j,function(k){i.push(k.getId())});var f=c.length!=0||b.length!=0||i.length!=0;if(f){var d=this.extract(c);var a=[];for(step in d){var g=d[step].Id;a.push(g);this.assignmentStore.each(function(k){if(k.data.TaskId===g){if(!Ext.Array.contains(b,k.getId())){b.push(k.getId())}}})}var e={tasks:a,assignments:b,dependencies:i};Ext.Ajax.request({url:this.serviceUrl.deleteTask,method:"POST",jsonData:e,callback:function(){this.taskStore.fireEvent("delete");this.onDeleteComplete()},scope:this})}else{this.onDeleteComplete()}return f},saveDataSet:function(){var f=this.deleteData();var e=this.resourceStore.getModifiedRecords();var a=this.assignmentStore.getModifiedRecords();var d=this.dependencyStore.getModifiedRecords();var g=this.taskStore.getModifiedRecords();var b=g.length!=0||d.length!=0||a.length!=0||e.length!=0;if(b){var c={projectId:this.projectData.projectId,resources:this.extract(e),assignments:this.extract(a),dependencies:this.extract(d),tasks:this.extract(g)};Ext.Ajax.request({url:this.serviceUrl.updateTask,method:"POST",jsonData:c,callback:function(){this.taskStore.fireEvent("save");this.onUpdateComplete()},scope:this})}else{if(f){this.onUpdateComplete()}else{this.deleteCompleted=false}}},onDeleteComplete:function(){if(this.updateCompleted){this.updateCompleted=false;this.reloadData()}else{this.deleteCompleted=true}},onUpdateComplete:function(){if(this.deleteCompleted){this.deleteCompleted=false;this.reloadData()}else{this.updateCompleted=true}},commitSelectedTasks:function(){var d=this;var c={confirm:{key:"Confirm",value:"Are you sure that you want the selected task(s) to be started ?"},status:"Status",taskNotSaved:"Please, save the project schedule, before to commit the task.",missingRequiredData:"Please, fill the reqired task(s) data, before commit.",alreadyCommited:"The task(s) cannot be commited.",btnYResponse:"yes"};var b=d.gantt.getView().getSelectionModel().getSelection();var a=d.commitValidator(b);b=this.sortSelectionHierarchically(b,false,true);if(typeof a!="object"&&a){Ext.Msg.confirm(c.confirm.key,c.confirm.value,function(f){var e=f;if(e==c.btnYResponse){d.showLoadingMask();var g={projectId:d.projectData.projectId,tasks:d.extract(b)};Ext.Ajax.request({scope:d,url:d.serviceUrl.commitTask,method:"POST",jsonData:g,callback:function(){d.taskStore.fireEvent("commit");d.reloadData()}})}})}else{if(a.needSave){Ext.Msg.alert(c.status,c.taskNotSaved)}else{if(a.needData){Ext.Msg.alert(c.status,c.missingRequiredData)}else{if(a.committed){Ext.Msg.alert(c.status,c.alreadyCommited)}}}}},commitValidator:function(f){var f=f;var b=this.taskStore.getModifiedRecords();var d={needSave:false,committed:false,needData:false};if(b.length){d.needSave=true}else{for(var c in f){var a=f[c];var e={assignment:a.hasAssignments(),type:($.trim(a.data.et).length),subtype:($.trim(a.data.tp).length),taskCommited:($.trim(a.data.uid).length),parentAlreadyCommited:($.trim(a.parentNode.data.uid).length),parentSendForCommit:this.isNodeInList(f,a.parentNode)};if(!e.assignment||!e.type||!e.subtype){d.needData=true;break}else{if(!(e.parentAlreadyCommited||e.parentSendForCommit)||e.taskcommit){d.committed=true;break}}}}if(!d.needSave&&!d.needData&&!d.committed){return true}return d},isNodeInList:function(a,c){for(var b=0;b<a.length;b++){if(a[b]===c){return true}}return false},checkProjectRestrictions:{checkEditMode:function(c){var b=c.scheduleConfig.disabled;var d=Ext.getCmp("disableEdit");var a=[d];if(b){this.restrictionUtil.groupElementVisibilityChange(a,"hide")}else{this.restrictionUtil.groupElementVisibilityChange(a,"show")}c.gantt.setReadOnly(b)},restrictionUtil:{groupElementVisibilityChange:function(c,a){for(var b in c){if($.trim(a)=="show"){c[b].show()}else{if($.trim(a)=="hide"){c[b].hide()}}}}}},getSelectedTasks:function(){var a=this.gantt.getView().getSelectionModel().getSelection();return a},setGantt:function(a){this.gantt=a},showLoadingMask:function(){if(this.gantt){this.gantt.showLoadingMask()}},sortSelectionHierarchically:function(c,b,d){var a=[];if(b){a=this.getNodesDescendants(c,d)}else{Ext.each(c,function(e){var f=this.findClosestAncestorIndex(a,e);if(d){if(f===-1){a.unshift(e)}else{a.splice(f+1,0,e)}}else{if(f===-1){a.push(e)}else{if(f===0){a.unshift(e)}else{a.splice(f,0,e)}}}},this)}return a},findClosestAncestorIndex:function(c,b){if(b.parentNode){var a=Ext.Array.indexOf(c,b.parentNode);if(a==-1){a=this.findClosestAncestorIndex(c,b.parentNode)}return a}else{return -1}},getNodeDescendants:function(b,a){var c=[];if(b.childNodes){Ext.each(b.childNodes,function(d){if(a){c.push(d);this.getNodeDescendants(d,a).forEach(function(e){c.push(e)})}else{this.getNodeDescendants(d,a).forEach(function(e){c.push(e)});c.push(d)}},this)}return c},getNodesDescendants:function(c,b){var a=[];Ext.each(c,function(e){var d=this.findClosestAncestorIndex(c,e);if(d===-1){if(b){a.push(e);this.getNodeDescendants(e,b).forEach(function(f){a.push(f)})}else{this.getNodeDescendants(e,b).forEach(function(f){a.push(f)});a.push(e)}}},this);return a},getTopNodes:function(b){var a=[];Ext.each(b,function(d){var c=this.findClosestAncestorIndex(b,d);if(c===-1){a.push(d)}},this);return a}});Ext.define("PMSch.model.PMResourceAllocationModel",{extend:"Gnt.model.Task",autoLoad:false,autoSync:false,nameField:"ResourceName",fields:[{name:"aiid",type:"string"},{name:"Status",type:"string"},{name:"ProjectTitle",type:"string"},{name:"ProjectId",type:"string"}]});Ext.define("PMSch.model.PMResourceStore",{extend:"Gnt.model.Resource",fields:[{name:"Role",type:"string"},{name:"JobTitle",type:"string"}]});Ext.define("PMSch.model.PMTaskModel",{extend:"Gnt.model.Task",autoLoad:false,autoSync:false,nameField:"title",defType:"defType",clsField:"et",startDateField:"sd",endDateField:"ed",durationField:"d",durationUnitField:"du",baselineStartDateField:"bsd",baselineEndDateField:"bed",baselinePercentDoneField:"bpd",phantomIdField:"phid",phantomParentIdField:"phpid",effortField:"e",effortUnitField:"eu",calendarIdField:"c",noteField:"n",percentDoneField:"pd",manuallyScheduledField:"ms",schedulingModeField:"schm",fields:[{name:"et",type:"string"},{name:"tp",type:"string"},{name:"aiid",type:"string"},{name:"Color",type:"string"},{name:"s",type:"string",defaultValue:"Submitted"},{name:"uid",type:"string"},{name:"sm",type:"string",defaultValue:"Auto"},{name:"index",type:"int"}]});Ext.ns("PM");Ext.Loader.setConfig({enabled:true,disableCaching:true,paths:{Gnt:"../libs/extGantt",Sch:"../libs/extSch",PMSch:"../js/app/"}});;Ext.onReady(function(){Ext.QuickTips.init();PM.Gantt.init()});PM.Gantt={scheduleConfig:{},init:function(b){this.gantt=this.createGantt(PM.Sch.projectData);var a=Ext.create("Ext.panel.Panel",{layout:"fit",renderTo:"scheduleContainer",height:900,items:this.gantt})},createGantt:function(f){var c=Ext.create("Gnt.data.TaskStore",{model:"PMSch.model.PMTaskModel",rootVisible:true,root:{expanded:false},proxy:{type:"ajax",method:"POST",url:"/emf/service/schedule/task/view?projectId="+f.projectId,reader:{type:"json"},writer:{type:"json",writeAllFields:true,encode:false,allowSingle:false,root:"data"},listeners:{exception:function(k,i,h){var j=null;if(!h.getError()){j=Ext.JSON.decode(i.responseText);j=j.message}else{j=h.getError().statusText}Ext.MessageBox.show({title:"REMOTE EXCEPTION",msg:j,icon:Ext.MessageBox.ERROR,buttons:Ext.Msg.OK})}}}});var b=Ext.create("Gnt.data.DependencyStore",{autoLoad:true,proxy:{type:"ajax",url:"/emf/service/schedule/resourse/dependency?projectId="+f.projectId,method:"GET",reader:{type:"json"}}});var e=Ext.create("Gnt.data.ResourceStore",{model:"PMSch.model.PMResourceStore",storeId:"resourceStore"});var g=Ext.create("Gnt.data.AssignmentStore",{autoLoad:true,resourceStore:e,proxy:{type:"ajax",url:"/emf/service/schedule/resourse/assignment?projectId="+f.projectId,method:"GET",reader:{type:"json",root:"assignments"}},listeners:{load:function(){e.loadData(this.proxy.reader.jsonData.resources)},add:function(j,i,l,m){var h=i[0];var k=c.getNodeById(h.getTaskId());k.modified.resourceassignment=true},remove:function(j,h,l,i,m){var k=c.getNodeById(h.getTaskId());k.modified.resourceassignment=true}}});var d=Ext.create("PMSch.controller.ScheduleController",{taskStore:c,resourceStore:e,assignmentStore:g,dependencyStore:b,projectData:f,scheduleConfig:this.scheduleConfig});var a=Ext.create("PMSch.GanttPanel",{layout:"border",region:"center",rowHeight:26,scheduleController:d,taskStore:c,dependencyStore:b,assignmentStore:g,resourceStore:e,selModel:new Ext.selection.TreeModel({ignoreRightMouseSelection:false,mode:"MULTI"}),columnLines:false,rowLines:false,autoFitOnLoad:true,showTodayLine:true,viewPreset:"weekAndDayLetter",showTeamPanel:true,projectData:f});d.setGantt(a);a.on({dependencydblclick:function(i,h){var k=c.getNodeById(h.get("From")).get("Name"),j=c.getNodeById(h.get("To")).get("Name")},timeheaderdblclick:function(i,j,h){}});return a}};Ext.define("PMSch.widget.PMAssignmentGrid",{override:"Gnt.widget.AssignmentGrid",constructor:function(a){this.store=Ext.create("Ext.data.JsonStore",{model:Ext.define("Gnt.model.AssignmentEditing",{extend:"Gnt.model.Assignment",fields:["ResourceName"]})});this.columns=[{xtype:"resourcenamecolumn"}];if(!this.readOnly){this.plugins=this.buildPlugins()}Ext.apply(this,{selModel:{allowDeselect:true,selType:"rowmodel",mode:"SINGLE",selectByPosition:function(b){var c=this.store.getAt(b.row);this.select(c,true)}}});this.callSuper(arguments)}});Ext.define("PMSch.widget.taskeditor.PMTaskEditor",{override:"Gnt.widget.taskeditor.TaskEditor",initComponent:function(){this.callParent(arguments);var a=this.assignmentGrid;var b=a.getStore();if(!a.addBtn){a.addBtn=a.down('[iconCls="gnt-action-add"]')}b.on("add",function(){a.addBtn&&a.addBtn.setDisabled(true)});b.on("remove",function(){a.addBtn&&a.addBtn.setDisabled(false)})},listeners:{loadTask:function(e,a,b){var c=e.assignmentGrid;var d=c.getStore();if(!c.addBtn){c.addBtn=c.down('[iconCls="gnt-action-add"]')}c.addBtn&&c.addBtn.setDisabled(d.getRange().length)}}});