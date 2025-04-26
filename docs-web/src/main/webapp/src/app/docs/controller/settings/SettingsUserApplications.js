'use strict';

/**
 * Settings user edition page controller.
 */
angular.module('docs').controller('SettingsUserApplications', function($scope, $dialog, $state, $stateParams, Restangular, $translate, $http) {
    $scope.loadApplications = function() {
        $http.post('/docs-web/api/registerRequest/display')
            .then(function(response) {
            const data = response.data;
            $scope.registerRequests = [];

            for (const id in data) {
                if (data.hasOwnProperty(id)) {
                    const item = data[id];
                    item.id = id;
                    $scope.registerRequests.push(item);
                }
            }
        }, function(error) {
            console.error('Error loading applications:', error);
        });
    };

    $scope.loadApplications();

    $scope.handleApprove = function(req) {
        $http.post('/docs-web/api/registerRequest/approve', { id: req.id})
          .then(function(response) {
            req.status = "Approved";
          }, function(error) {
            console.error('Error approving request:', error);
          });
  };

    $scope.handleReject = function(req) {
        $http.post('/docs-web/api/registerRequest/reject', { id: req.id })
          .then(function(response) {
            req.status = "Rejected"; 
          }, function(error) {
            console.error('Error rejecting request:', error);
          });
      };
});
