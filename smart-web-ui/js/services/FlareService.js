angular.module('appServices').factory('FlareService', ['$resource', 'ConfigService', '$http', '$q',
  function($resource, ConfigService, $http, $q) {
    var environments = [];
    var timestamp;
    var flareAPI = ConfigService.flare;
    var environmentsAPI = flareAPI.host + "/environments";
    var enviromentCallback;
    function getThingsForZoneInEnvironment(zoneId, environmentId) {
      var url = environmentsAPI + "/" + environmentId + "/zones/" + zoneId + "/things";
      return $q.all([
          $http.get(url)
        ])
        .then(function(results) {
          return results[0].data;
        });
    }

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
            var i, zoneId;
            function addThingsForZone(data) {
              if (data !== undefined && data.length > 0) {
                var foundZone = environments.zones.find(function(z) {
                  if (z._id === data[0].zone) {
                    return z;
                  }
                });
                if (foundZone !== undefined) {
                  foundZone.things = data;
                  if(enviromentCallback){enviromentCallback()};
                }
              }
            }
            for (i = 0 ; i < environments.zones.length ; i++) {
              var zone = environments.zones[i];
              getThingsForZoneInEnvironment(zone._id, zone.environment).then(addThingsForZone);
            }
            timestamp = Date.now();
            return environments;
          });
      },
      setEnviromentCallback: function(cb){enviromentCallback=cb},
      getDeviceInfoInEnvironment: function(deviceId, environmentId) {
        var url = environmentsAPI + "/" + environmentId + "/devices/" + deviceId;
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
