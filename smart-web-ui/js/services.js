
var myServices = angular.module('appServices', ['ngResource']);

myServices.factory('socket', function ($rootScope) {
  var host = "localhost", port = 1234;
  var socket = io.connect('http://' + host + ':' + port);
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
});
