/* App Module */

var myWebApp = angular.module('myWebApp', [
    'ngRoute',
    'appServices',
    'appControllers',
    'appFilters',
    'appComponents',
    'ngSanitize'
  ])
  .provider('ConfigService', function () {
    var options = {};
    this.config = function (opt) {
      angular.extend(options, opt);
    };

    this.$get = [function () {
      if (!options) {
        throw new Error('Config options must be configured');
      }

      return options;
    }];
  })

  .config(['$routeProvider',
    function($routeProvider) {
      $routeProvider.
        when('/', {
          templateUrl: 'partials/page1.html',
          controller: 'MySampleCtrl'
        }).
        when('/page2', {
          templateUrl: 'partials/page2.html',
          controller: 'AnotherSampleCtrl'
        }).
        otherwise({
          redirectTo: '/'
        });
    }]);

// manually bootstrap the app when the document is ready and both config files have been loaded
function bootstrapApplication() {
  angular.element(document).ready(function() {
    angular.bootstrap(document, ["myWebApp"]);

    // enable 'copy to clipboard' functionality on all buttons with the copy-to-clipboard class
    new Clipboard('.copy-to-clipboard');
  });
}

// load config files in a specific order before bootstrapping the web app
(function loadConfigFilesAndBootstrapTheApp() {
  var initInjector = angular.injector(["ng"]);
  var $http = initInjector.get("$http");
  $http.get('conf/config.json')
    .then(function(json) {

      myWebApp.config(['ConfigServiceProvider', function (ConfigServiceProvider) {
        ConfigServiceProvider.config(json.data);

//        console.log("conf/config.json loaded successfully", json.data);
      }]);

      return $http.get('conf/config2.json');
    })
    .then(function(json) {

      myWebApp.config(['ConfigServiceProvider', function (ConfigServiceProvider) {
        ConfigServiceProvider.config(json.data);

//        console.log("conf/config2.json loaded successfully", json.data);
      }]);

      bootstrapApplication();
    });
}());
