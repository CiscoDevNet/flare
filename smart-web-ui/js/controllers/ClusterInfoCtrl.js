/*-------------------------------------------------------------------------------
 * Name:        ClusterInfoCtrl.js
 * Purpose:     Controller providing information about the cluster.
 *-------------------------------------------------------------------------------*/

angular.module('appControllers').controller('ClusterInfoCtrl', ['$scope', 'ConfigService',
  function($scope, ConfigService) {

//    $scope.clustername = ConfigService.clustername;
    $scope.title = "web-app-template (" + ConfigService.version + ")";

  }]
);
