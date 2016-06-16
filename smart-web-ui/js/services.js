
var myServices = angular.module('appServices', ['ngResource']);

myServices.factory('socket', ['$rootScope', 'ConfigService', function ($rootScope, ConfigService) {
  console.log("socket.io", ConfigService.flare.host);
  var socket = io.connect(ConfigService.flare.host);
  return {
    on: function (eventName, callback) {
      socket.on(eventName, function () {
        var args = arguments;
        $rootScope.$apply(function () {
          callback.apply(socket, args);
        });
      });
    },
    emit: function (eventName, data, callback) {
      socket.emit(eventName, data, function () {
        var args = arguments;
        $rootScope.$apply(function () {
          if (callback) {
            callback.apply(socket, args);
          }
        });
      });
    }
  };
}]);
