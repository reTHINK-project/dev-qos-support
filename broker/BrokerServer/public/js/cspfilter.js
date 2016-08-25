angular.module('cspFilters', []).filter('cspfilter', function() {
  return function(input) {
    if (input != null) {
      return input.split(":")[1];
    } else {
      return "";
    }
  };
});
