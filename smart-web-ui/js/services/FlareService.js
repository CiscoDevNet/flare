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
            return results[0].data;
          });
      },
      getThingsForZoneInEnvironment: function(zoneId, environmentId) {
        var url = environmentsAPI + "/" + environmentId + "/zones/" + zoneId + "/things";
        return $q.all([
            $http.get(url)
          ])
          .then(function(results) {
            return results[0].data;
          });
      }
    };
  }]
);
