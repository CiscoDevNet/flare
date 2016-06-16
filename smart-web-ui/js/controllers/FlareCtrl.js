angular.module('appControllers').controller('FlareCtrl', ['$scope', 'FlareService', 'socket', '$timeout',
  '$filter', function($scope, FlareService, socket, $timeout) {

    $scope.environmentId = "57603ea3e7829cf86f8f0dd5"; // Home
    $scope.deviceId = "576040ace7829cf86f8f0dde"; // Andrew's phone

    //send the updated object
    FlareService.setEnvironmentCallback(function(){
        $scope.environment = Object.assign({},$scope.environment);
    });



    $scope.getEnvironmentInfo = function() {
      FlareService.getEnvironmentInfo($scope.environmentId).then(function(data) {
          console.log("New Data",Object.keys(data.zones[0]));
          $scope.environment = data;
      });
    };

    // define the function getting the list of packages and launch it straight away
    $scope.getDeviceInfo = function() {
      FlareService.getDeviceInfoInEnvironment($scope.deviceId, $scope.environmentId).then(function(data) {
        $scope.device = data;
      });
    };

    $scope.getEnvironmentInfo();
    $scope.getDeviceInfo();

    $scope.performAction = function(deviceId, action) {
      console.log("performAction", deviceId, action);
      socket.emit("performAction", { thing: deviceId, action: action });
    };

    /* Window events and other custom events that force a redraw */

    $(document).scroll(function() {
      // if anything needs to be done when the users scrolls the page, this is where you would do it
    });

    $(window).resize(function() {
      // if anything needs to be done when the window has been resized, this is where you would do it
    });

    // Uncomment and adapt the section below to update the model when updates arrive using the socket.io connection

    var subscriptionMsg = {
      environment: $scope.environmentId,
      all: true
    };
    socket.emit('subscribe', subscriptionMsg);

    // update users when socket.io updates are received.
    socket.on('data', function(obj) {
      console.log("FlareCtrl socket update data", obj);
      if (obj.thing !== undefined) {
        var foundThing;
        for (var i = 0 ; i < $scope.environment.zones.length && foundThing === undefined ; i++) {
          foundThing = $scope.environment.zones[i].things.find(function findThing(t) {
            if (t._id === obj.thing) {
              return t;
            }
          });
          if (foundThing !== undefined) {
            for (var key in obj.data) {
//              console.log("found thing", obj.thing, "in zone", $scope.environment.zones[i].name, foundThing);
//              console.log("key", key, foundThing.data[key], obj.data[key]);
              foundThing.data[key] = obj.data[key];
            }
          } else {
//            console.log("didn't find thing", obj.thing, "in zone", $scope.environment.zones[i].name);
          }
        }
      }
    });

    socket.on('position', function(obj) {
//      console.log("FlareCtrl socket update position", obj);
      if (obj.thing !== undefined) {
        var foundThing;
        for (var i = 0 ; i < $scope.environment.zones.length && foundThing === undefined ; i++) {
          foundThing = $scope.environment.zones[i].things.find(function(t) {
            if (t._id === obj.thing) {
              return t;
            }
          });
          if (foundThing !== undefined) {
            foundThing.position = obj.position;
          }
        }
      }
    });

//    // update users when socket.io updates are received.
//    socket.on('handleAction', function(obj) {
//      console.log("FlareCtrl socket update action", obj);
//      if (obj.thing !== undefined) {
//        var foundThing;
//        for (var i = 0 ; i < $scope.environment.zones.length && foundThing === undefined ; i++) {
//          foundThing = $scope.environment.zones[i].things.find(function(t) {
//            if (t._id === obj.thing) {
//              return t;
//            }
//          });
//          if (foundThing !== undefined) {
////            for (var key in obj.data) {
//////              console.log("found thing", obj.thing, "in zone", $scope.environment.zones[i].name, foundThing);
//////              console.log("key", key, foundThing.data[key], obj.data[key]);
////              foundThing.data[key] = obj.data[key];
////            }
////          } else {
//////            console.log("didn't find thing", obj.thing, "in zone", $scope.environment.zones[i].name);
//          }
//        }
//      }
//    });

    $scope.audioMessageOn = false;
    $scope.searchText = "";

    var timer;

    // handle messages from FlareFakeCtrl
    socket.on('broadcast-message', function(msg) {
      console.log("broadcast-message", msg);
      $scope.audioMessageOn = true;

      var words = msg.split(" ");
      var wordIndex = 0;
      $scope.searchText = "";
      function displayNextWord() {
        if (wordIndex >= words.length)
          return;
        $scope.searchText += words[wordIndex] + " ";
        console.log("search", $scope.searchText);
        // move the caret to the end of the line
//        $('#searchText').focus(function() {
//          $(this).val($(this).val());
//        }).focus();

        wordIndex++;
        if (wordIndex < words.length) {
          timer = $timeout(displayNextWord, 250+Math.random()*200);
        }
      }
      timer = $timeout(displayNextWord, 500);

      timer = $timeout(function() {
        $scope.audioMessageOn = false;
        $scope.searchText = "";
      }, 5000);
    });

    // when the scope of the controller gets destroyed, do some clean up
    $scope.$on('$destroy', function() {
      // If you have set up timeouts or intervals, this is where you would destroy these timers
      $timeout.cancel(timer);
    });
  }]
);
