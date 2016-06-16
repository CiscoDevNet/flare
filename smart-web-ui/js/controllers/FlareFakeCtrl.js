angular.module('appControllers').controller('FlareFakeCtrl', ['$scope', 'socket',
  function($scope, socket) {

    function audioCommand(text, command) {
      console.log("emit", text, command);
      var message = {
        type: "audio",
        text: text,
        command: command
      }
      socket.emit('broadcast-message', message);
    }

    function reminder(text, command) {
      console.log("emit", text, command);
      var message = {
        type: "reminder",
        text: text,
        command: command
      }
      socket.emit('broadcast-message', message);
    }

    function reveal(text, command) {
      console.log("emit", command);
      var message = {
        type: "reveal",
        command: command
      }
      socket.emit('broadcast-message', message);
    }

    $scope.buttons = [
      {
        text: "start the kettle",
        callback: {
          fn: audioCommand,
          msg: 'pop the kettle on',
          command: {
            target: '57603f47e7829cf86f8f0ddb',
            action: 'on',
            allowed: true
          }
        }
      },
      {
        text: "close the window",
        callback: {
          fn: audioCommand,
          msg: 'close the window in the living room',
          command: {
            target: '576276cd28c5cad47d5ce51c',
            action: 'close',
            allowed: false
          }
        }
      },
      {
        text: "confirm closing window",
        callback: {
          fn: reminder,
          msg: 'do you still want to close the window in the living room?',
          command: {
            target: '576276cd28c5cad47d5ce51c',
            action: 'close',
            allowed: true
          }
        }
      },
      {
        text: "temperature sensor",
        callback: {
          fn: reveal,
          command: {
            target: '#5762774b28c5cad47d5ce51d'
          }
        }
      }
    ];

  }]
);
