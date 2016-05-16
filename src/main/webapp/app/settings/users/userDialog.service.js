(function() {
    angular
        .module('app.settings')
        .service('userDialog', AddUserController);

    function AddUserController ($mdDialog, userService) {
        this.addUser = function ($scope, ev) {
            $mdDialog.show({
                controller: UserDialogController,
                templateUrl: 'app/settings/users/addUserDialog.html',
                targetEvent: ev,
                clickOutsideToClose: true,
                locals: {
                    roles: [{id: 'SECURITY_ADMINISTRATOR', label: 'Security Administrator'}, {
                        id: 'ADMINISTRATOR',
                        label: 'Administrator'
                    }, {id: 'USER', label: 'User'}]
                }
            }).then(function (answer) {
                 return userService.addUser(answer).then(function () {
                     $scope.loadUsers();
                 });
            }, function () {
                console.log('You cancelled the dialog.');
            });
        }
    }

    function UserDialogController($scope, $mdDialog, roles) {
        $scope.roles = roles;
        $scope.hide = function() {
            $mdDialog.hide();
        };
        $scope.cancel = function() {
            $mdDialog.cancel();
        };
        $scope.answer = function(answer) {
            $mdDialog.hide(answer);
        };
    }
})();