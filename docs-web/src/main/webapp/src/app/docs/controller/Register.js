'use strict';

/**
 * Login controller.
 */
angular.module('docs').controller('Register', function(Restangular, $scope, $rootScope, $state, $stateParams, $dialog, User, $translate, $uibModal) {
      // Get the app configuration
    Restangular.one('app').get().then(function(data) {
        $rootScope.app = data;
    });

    // Register
    $scope.register = function() {
        if ($scope.user.password != $scope.user.confirmPassword) {
            var title = "Passwords do not match";
            var msg = "Please make sure that the passwords match.";
            var btns = [{result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary'}];
            $dialog.messageBox(title, msg, btns);
            return;
        }

        User.register({
          username: $scope.user.username,
          email: $scope.user.email,
          password: $scope.user.password
        }).then(function() {
          User.userInfo(true).then(function(data) {
            $rootScope.userInfo = data;
          });

          if ($stateParams.redirectState !== undefined && $stateParams.redirectParams !== undefined) {
            $state.go($stateParams.redirectState, JSON.parse($stateParams.redirectParams))
              .catch(function() {
                $state.go('login');
              });
          } else {
            $state.go('login');
          }
        });
    };
});
