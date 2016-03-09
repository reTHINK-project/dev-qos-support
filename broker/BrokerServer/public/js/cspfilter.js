angular.module('cspFilters', []).filter('cspfilter', function() {
  return function(input) {
    return input.split(":")[1];
  };
});
