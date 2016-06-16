angular.module('appControllers').controller('FlareFakeCtrl', ['$scope', 'socket',
  function($scope, socket) {

    function fakeAudioDiction(text) {
      console.log("emit", text);
      socket.emit('broadcast-message', text);
    }
    $scope.buttons = [
      {
        text: "open window",
        callback: {
          fn: fakeAudioDiction,
          msg: 'can you open the window?'
        }
      },
      {
        text: "show all devices",
        callback: {
          fn: fakeAudioDiction,
          msg: 'show all devices'
        }
      }
    ];

  }]
);
