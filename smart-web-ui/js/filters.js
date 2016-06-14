var myFilters = angular.module('appFilters', []);

myFilters.filter('simplifyTime', function() {
  return function(millis, days, hours, minutes) {

    var res = "";
    if (days > 0) {
      res = days + " day" + (days > 1 ? "s " : " ");
    } else if (hours > 0) {
      res = hours + " hour" + (hours > 1 ? "s" : "");
    } else if (minutes > 0) {
      res = minutes + " minute" + (minutes > 1 ? "s" : "");
    } else {
      res = "just now";
    }

    return res;
  };
});
