PluginRegistry.add('picker', [{
  'order': 10,
  'name': 'seip-object-picker-search',
  'label': 'picker.search.tab',
  'component': 'seip-search',
  'module': 'search/components/search'
}, {
  'order': 20,
  'name': 'seip-object-picker-basket',
  'label': 'picker.basket.tab',
  'component': 'picker-basket',
  'module': 'search/picker/picker-basket'
}, {
  'order': 30,
  'name': 'seip-object-picker-recent',
  'label': 'picker.recent.tab',
  'component': 'picker-recent-objects',
  'module': 'search/picker/picker-recent-objects'
}, {
  'order': 40,
  'name': 'seip-object-picker-create',
  'label': 'picker.create.tab',
  'component': 'picker-create',
  'module': 'search/picker/picker-create'
}]);