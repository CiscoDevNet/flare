
//angular.module('appServices').factory('FlareService', ['$resource', 'ConfigService', '$http', '$q',
//  function($resource, ConfigService, $http, $q) {
//    var flareAPI = ConfigService.flare;
//    var url = flareAPI.host + flareAPI.get_users;
//    var FlareService = $resource(url, {}, {
//      query: { method:'GET', params:{ metricId:'' }, isArray:true, transformResponse: function(data) {
//        var json = JSON.parse(data);
//
//        // if we received an array, it's the list of users
//        if (angular.isArray(json)) {
//          // for each users, load its details asynchronously using the /users/userId API (for demo purposes)
//          var users = [];
//          json.map(function(user) {
//            // defer getting its value using a promise (asynchronously)
//            var getUserInfo = function(u) {
//              var deferred = $q.defer();
//              $http.get(url + "/" + u.id)
//                .then(function successCallback(response) {
//                  // this callback will be called asynchronously
//                  // when the response is available
//                  var resObj = {
//                    company: response.data.company.name,
//                    name: response.data.name,
//                    email: response.data.email,
//                    phone: response.data.phone
//                  };
//
//                  return deferred.resolve(resObj);
//                }, function errorCallback(response) {
//                  // called asynchronously if an error occurs
//                  // or server returns response with an error status.
//                  deferred.reject('error ' + response);
//                });
//
//              return deferred.promise;
//            };
//
//            // add this metric to the array
//            // at the moment we just have its name but we're using a promise to get its latest value from the API
//            users.push({ id: user.id, info: $q.all([getUserInfo(user)]) });
//          });
//          return users;
//        } else {
//          console.log("not an array", json);
//          return [json];
//        }
//
//      } }
//    });
//
//    return FlareService;
//  }]
//);

angular.module('appServices').factory('FlareService', ['$resource', 'ConfigService', '$http', '$q',
  function($resource, ConfigService, $http, $q) {
    var environments = [];
    var timestamp;
    var flareAPI = ConfigService.flare;
    var environmentsAPI = flareAPI.host + "/environments";

    return {
      getEnvironmentInfo: function(environmentId) {
        var url = environmentsAPI + "/" + environmentId;
        // $q.all will wait for an array of promises to resolve,
        // then will resolve its own promise (which it returns)
        // with an array of results in the same order.
        return $q.all([
            $http.get(url),
            $http.get(url + "/zones")
          ])
          .then(function(results) {
            console.log("environments", results);
            environments = results[0].data;
            environments.zones = results[1].data;
            timestamp = Date.now();
            return environments;
          });
      },
      getDeviceInfoInEnvironment: function(deviceId, environmentId) {
        var url = environmentsAPI + "/" + environmentId + "/devices/" + deviceId;
        return $q.all([
            $http.get(url)
          ])
          .then(function(results) {
            console.log("device", results[0].data);
            return results[0].data;
          });
      },
      getThingsForZoneInEnvironment: function(zoneId, environmentId) {
        var url = environmentsAPI + "/" + environmentId + "/zones/" + zoneId + "/things";
        return $q.all([
            $http.get(url)
          ])
          .then(function(results) {
            console.log("zone things", zoneId, results[0].data);
            return results[0].data;
          });
      }

//      getDeployedPackages: function(force) {
//        // get the list of deployed packages from the backend data manager
//
//        // if the list of deployed packages is already available and we got the list recently, send this cached list
//        // otherwise (or if we're forcing a refresh), get an updated list
//        var now = Date.now();
//        if (deployedPackages.length > 0 && force === undefined
//          && deployedPackagesTimestamp !== undefined
//          && now - deployedPackagesTimestamp < Constants.PACKAGES_CACHING_DURATION) {
//
//          var deferred = $q.defer();
//          var promise = deferred.promise;
//          deferred.resolve(deployedPackages);
//          return promise;
//        }
//
//        // $q.all will wait for an array of promises to resolve,
//        // then will resolve its own promise (which it returns)
//        // with an array of results in the same order.
//        return $q.all([
//            $http.get(packagesAPI + "/deployed")
//          ])
//          .then(function(results) {
//            deployedPackages = [];
//            angular.forEach(results[0].data.deployedPackages, function(p) {
//              var match;
//              if ((match = p.match(/^(.*)-([\d\.]*)$/i)) !== null) {
//                var packageName = match[1];
//                var version = match[2];
//                deployedPackages.push({ name: packageName, version: version });
//              }
//            });
//            deployedPackagesTimestamp = Date.now();
//            return deployedPackages;
//          });
//      },
//      getPackageInfo: function(package) {
//        // get the list of packages from the backend data manager
//
//        // $q.all will wait for an array of promises to resolve,
//        // then will resolve its own promise (which it returns)
//        // with an array of results in the same order.
//        return $q.all([
//            $http.get(packagesAPI + "/" + package)
//          ])
//          .then(function(results) {
//            packages = results[0].data;
//            return packages;
//          });
//      },
//
//      deploy: function(package) {
//        return $http.put(packagesAPI + "/" + package);
//      },
//      undeploy: function(package) {
//        return $http.delete(packagesAPI + "/" + package);
//      },
//      getPackageStatus: function(name) {
//        var result = {};
//        var packagesApiStatus = packagesAPI + "/" + name + "/status";
//        return $q.all([
//            $http.get(packagesApiStatus)
//          ])
//          .then(function(results) {
//            result.status = results[0].data.status;
//            result.information = results[0].data.information;
//            return result;
//          });
//      },
//      getApplications: function() {
//        var applicationsApi = "http://" + dataManager.host + ":" + dataManager.port + "/applications";
//
//        // $q.all will wait for an array of promises to resolve,
//        // then will resolve its own promise (which it returns)
//        // with an array of results in the same order.
//        return $q.all([
//            $http.get(applicationsApi)
//          ])
//
//          // process all of the results from the two promises
//          // above, and join them together into a single result.
//          // since then() returns a promise that resolves to the
//          // return value of its callback, this is all we need
//          // to return from our service method.
//          .then(function(results) {
//            if (typeof results[0].data === 'string') {
//              // something went wrong. return status string
//              return results[0].data;
//            } else {
//              // return application array
//              return angular.isArray(results[0].data.applications) ? results[0].data.applications : [];
//            }
//          });
//      },
//      getApplicationInfo: function(name) {
//        var dataManager = ConfigService.backend["data-manager"];
//        var applicationsApi = "http://" + dataManager.host + ":" + dataManager.port + "/applications/";
//        return $q.all([
//            $http.get(applicationsApi + "/" + name)
//          ])
//          .then(function(results) {
//            packages = results[0].data;
//            return packages;
//          });
//      },
//      createApplication: function(name, body) {
//        var dataManager = ConfigService.backend["data-manager"];
//        var applicationsApi = "http://" + dataManager.host + ":" + dataManager.port + "/applications/" + name;
//        var res = $http.put(applicationsApi, body);
//        return res;
//      },
//      getApplicationStatus: function(name) {
//        var result = {};
//        var dataManager = ConfigService.backend["data-manager"];
//        var applicationsApi = "http://" + dataManager.host + ":" + dataManager.port + "/applications/"
//          + name + "/status";
//        return $q.all([
//            $http.get(applicationsApi)
//          ])
//          .then(function(results) {
//            result.status = results[0].data.status;
//            result.information = results[0].data.information;
//            return result;
//          });
//      },
//      destroyApplication: function(name) {
//        var dataManager = ConfigService.backend["data-manager"];
//        var applicationsApi = "http://" + dataManager.host + ":" + dataManager.port + "/applications/" + name;
//        var res = $http.delete(applicationsApi);
//        return res;
//      },
//      performApplicationAction: function(name, action) {
//        var applicationsApi = "http://" + dataManager.host + ":" + dataManager.port
//          + "/applications/" + name + "/" + action;
//        var res = $http.post(applicationsApi);
//        return res;
//      },
//      getEndpoints: function() {
//        var endpointsAPI = "http://" + dataManager.host + ":" + dataManager.port + "/endpoints";
//
//        // $q.all will wait for an array of promises to resolve,
//        // then will resolve its own promise (which it returns)
//        // with an array of results in the same order.
//        return $q.all([
//            $http.get(endpointsAPI),
//            $http.get('/conf/dm_address_mapping.json')
//          ])
//
//          // process all of the results from the two promises
//          // above, and join them together into a single result.
//          // since then() returns a promise that resolves to the
//          // return value of its callback, this is all we need
//          // to return from our service method.
//          .then(function(results) {
//            var endpoints = results[0].data.endpoints;
//            function replaceValuesInObject(obj, regexMatch, replacement) {
//              var output = {};
//              angular.forEach(obj, function(value, key) {
//                if (angular.isObject(value)) {
//                  var subObj = replaceValuesInObject(value, regexMatch, replacement);
//                  output[key] = subObj;
//                } else {
//                  output[key] = value.replace(regexMatch, replacement);
//
////                  if (output[key] !== value) console.log(value, " -> ", output[key]);
//                }
//              });
//              return output;
//            }
//
//            angular.forEach(results[1].data, function(replacement, key) {
//              endpoints = replaceValuesInObject(endpoints, key, replacement);
//            });
//            return endpoints;
//          });
//      }
    };
  }]
);
