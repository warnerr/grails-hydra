(function() {
    angular
        .module('app.settings')
        .service('userDialog', AddUserController);

    function AddUserController ($mdDialog, userService) {
        this.addUser = function (users, ev) {
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
                var user = userService.addUser(answer);
                users.push(user);
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