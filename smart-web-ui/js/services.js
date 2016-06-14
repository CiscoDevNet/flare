
var myServices = angular.module('appServices', ['ngResource']);

myServices.factory('SampleService', ['$resource', 'ConfigService', '$http', '$q',
  function($resource, ConfigService, $http, $q) {
    var restAPISampleService = ConfigService.rest_api;
    var url = restAPISampleService.host + restAPISampleService.get_users;
    var SampleService = $resource(url, {}, {
      query: { method:'GET', params:{ metricId:'' }, isArray:true, transformResponse: function(data) {
        var json = JSON.parse(data);

        // if we received an array, it's the list of users
        if (angular.isArray(json)) {
          // for each users, load its details asynchronously using the /users/userId API (for demo purposes)
          var users = [];
          json.map(function(user) {
            // defer getting its value using a promise (asynchronously)
            var getUserInfo = function(u) {
              var deferred = $q.defer();
              $http.get(url + "/" + u.id)
                .then(function successCallback(response) {
                  // this callback will be called asynchronously
                  // when the response is available
                  var resObj = {
                      company: response.data.company.name,
                      name: response.data.name,
                      email: response.data.email,
                      phone: response.data.phone
                    };

                  return deferred.resolve(resObj);
                }, function errorCallback(response) {
                  // called asynchronously if an error occurs
                  // or server returns response with an error status.
                  deferred.reject('error ' + response);
                });

              return deferred.promise;
            };

            // add this metric to the array
            // at the moment we just have its name but we're using a promise to get its latest value from the API
            users.push({ id: user.id, info: $q.all([getUserInfo(user)]) });
          });
          return users;
        } else {
          console.log("not an array", json);
          return [json];
        }

      } }
    });

    return SampleService;
  }]
);

// Uncomment and adapt the code below to enable a socket.io connection

//myServices.factory('socket', function ($rootScope) {
//  var host = "localhost", port = 1234;
//  var socket = io.connect('http://' + host + ':' + port);
//  return {
//    on: function (eventName, callback) {
//      socket.on(eventName, function () {
//        var args = arguments;
//        $rootScope.$apply(function () {
//          callback.apply(socket, args);
//        });
//      });
//    },
//    emit: function (eventName, data, callback) {
//      socket.emit(eventName, data, function () {
//        var args = arguments;
//        $rootScope.$apply(function () {
//          if (callback) {
//            callback.apply(socket, args);
//          }
//        });
//      })
//    }
//  };
//});
