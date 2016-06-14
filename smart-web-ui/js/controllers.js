var myControllers = angular.module('appControllers', ['mgcrea.ngStrap']);

myControllers.controller('MySampleCtrl', ['$scope', '$filter', '$interval', '$location',
  function($scope, $filter, $interval, $location) {

    var locationParameters = $location.search();
    $scope.theme = (locationParameters.theme === undefined ? '' : 'css/generated/themes/' +
      locationParameters.theme + '.css');

    $scope.changeTheme = function(theme) {
      $scope.theme = (theme === undefined ? '' : 'css/generated/themes/' + theme + '.css');
      $("link#theme").attr("href", $scope.theme);
    };

    $scope.changeTheme(locationParameters.theme);

    $scope.numberOfSecondsSinceLaunch = 0;
    var stop = $interval(function() {
      $scope.numberOfSecondsSinceLaunch++;
    }, 1000);

    $scope.stopUpdateTimer = function() {
      if (angular.isDefined(stop)) {
        $interval.cancel(stop);
        stop = undefined;
      }
    };

    /* Window events and other custom events that force a redraw */

    $(document).scroll(function() {
      // if anything needs to be done when the users scrolls the page, this is where you would do it
    });

    $(window).resize(function() {
      // if anything needs to be done when the window has been resized, this is where you would do it
    });

    // when the scope of the controller gets destroyed, do some clean up
    $scope.$on('$destroy', function() {
      // If you have set up timeouts or intervals, this is where you would destroy these timers
      $scope.stopUpdateTimer();
    });

    $(document).ready(function() {
      $('button').hover(
        function() {
          TweenLite.to($(this), 0.3, { scale:1.2 });
        },
        function() {
          TweenLite.to($(this), 0.15, { scale:1 });
        }
      );

      var treeToTraverse = $(".panel");
      treeToTraverse.each(function(elementIndex, element) {
        var textDiv = $(element).find(".title")[0];
        var defaultDuration = 0.5;
        var delay = 0.15;
        var defaultEase = Power3.easeOut;
        var staggeredFromTos = [
          {
            element: element,
            function: TweenLite.fromTo,
            from: { opacity: 0, scale: 0.5 },
            to: { opacity: 1, scale: 1, ease: defaultEase }
          }//,

        ];
        angular.forEach(staggeredFromTos, function(fromTo, fromToIndex) {
          var to, from, set;
          if (fromTo.function === TweenLite.fromTo) {
            to = fromTo.to;
            to.delay = fromToIndex * defaultDuration + elementIndex * delay;
            fromTo.function(fromTo.element, defaultDuration, fromTo.from, to, elementIndex * delay);
          } else if (fromTo.function === TweenLite.to) {
            to = fromTo.to;
            to.delay = fromToIndex * defaultDuration + elementIndex * delay;
            fromTo.function(fromTo.element, defaultDuration, to, elementIndex * delay);
          } else if (fromTo.function === TweenLite.from) {
            from = fromTo.from;
            from.delay = fromToIndex * defaultDuration + elementIndex * delay;
            fromTo.function(fromTo.element, defaultDuration, from, elementIndex * delay);
          } else if (fromTo.function === TweenLite.set) {
            set = fromTo.set;
            set.delay = fromToIndex * defaultDuration + elementIndex * delay;
            fromTo.function(fromTo.element, set);
          }
        });
      });

    });
  }]
);

myControllers.controller('AnotherSampleCtrl', ['$scope', 'SampleService', 'ConfigService', /*'socket', */'$filter',
  function($scope, SampleService, ConfigService/*socket, */) {

    $scope.users = SampleService.query(function(response) {
      angular.forEach(response, function(user) {
        // turn the 'value' promise into its value when it's available
        user.info.then(function(response) {
          user.info = response[0];
        });
      });
    });

    $scope.orderProp = "info.name";
    $scope.reverseOrder = false;
    $scope.orderArrow = function() {
      return $scope.reverseOrder ? "glyphicon-triangle-bottom" : "glyphicon-triangle-top";
    };

    $scope.sortBy = function(order) {
      if ($scope.orderProp === order) {
        $scope.reverseOrder = !$scope.reverseOrder;
      } else {
        $scope.orderProp = order;
        $scope.reverseOrder = false;
      }
    };

    $scope.links = ConfigService.interesting_links;

    /* Window events and other custom events that force a redraw */

    $(document).scroll(function() {
      // if anything needs to be done when the users scrolls the page, this is where you would do it
    });

    $(window).resize(function() {
      // if anything needs to be done when the window has been resized, this is where you would do it
    });

    // when the scope of the controller gets destroyed, do some clean up
    $scope.$on('$destroy', function() {
      // If you have set up timeouts or intervals, this is where you would destroy these timers
    });

    // Uncomment and adapt the section below to update the model when updates arrive using the socket.io connection

//    // update users when socket.io updates are received.
//    var socketupdate = function(obj) {
//      var found = $scope.users.find(function(element, index, array) {
//        if (element.name === obj.user) {
//          element.info.address = obj.address;
//          element.info.phone = obj.phoneNumber;
//          element.info.age = obj.age;
//          return element;
//        }
//      });
//      // if the element wasn't found, it's a new user
//      if (found === undefined) {
//        $scope.users.push({
//          name: obj.user,
//          info: {
//            address: obj.address,
//            phone: obj.phoneNumber,
//            age: obj.age
//          }
//        });
//      }
//    };
//    socket.on('web-app-template-socket-update', socketupdate);
  }]
);
