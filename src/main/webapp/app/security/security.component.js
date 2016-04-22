/**
 * Created by rwarner on 4/7/16.
 */
(function() {
    angular
        .module('app.security', ['ngMaterial', 'ngComponentRouter', 'oc.lazyLoad', 'md.data.table'])
        .component('security', {
            templateUrl: 'app/security/security.html',
            controller: UserListComponent
        }).service('userService', UserService);

    function UserListComponent(userService, $mdDialog) {
        var selectedId = null;
        var $ctrl = this;
        $ctrl.selected = [];
        $ctrl.isAddingUser = false;

        $ctrl.query = {
            order: 'name',
            limit: 5,
            page: 1
        };

       /* function success(users) {
            $ctr.users = users;
        }*/
        $ctrl.$routerOnActivate = function(next, previous) {
            // Load up the heroes for this view
            return userService.getUsers().then(function(users) {
                $ctrl.users = users;
                selectedId = next.params.id;
            });
        };

        $ctrl.isSelected = function(user) {
            return (user.id == selectedId);
        };

        $ctrl.addUser = function(ev) {
            console.log('adding user');
            $mdDialog.show({
                controller: UserDialogController,
                templateUrl: 'app/security/add-user-dialog.html',
                targetEvent: ev,
                clickOutsideToClose: true,
                locals: {
                    roles: [ {id: 'SECURITY_ADMINISTRATOR', label: 'Security Administrator'}, {id: 'ADMINISTRATOR', label: 'Administrator'}, {id: 'USER', label: 'User'}]
                }
            }).then(function(answer) {
                var status = userService.addUser(answer);
                console.log("user == " + answer.username + "   status == " + status);
            }, function() {
                console.log('You cancelled the dialog.');
            });
        }

    }

    function UserService($q) {
        var usersPromise = $q.when([
            {
                "id": "56f2b8a0cd7338182f63ca68",
                "username": "seven10",
                "password": "seven10",
                "time_created": "Mar 23, 2016 11:39:12 AM",
                "last_login_time": "Mar 23, 2016 11:39:12 AM",
                "role": "ADMINISTRATOR"
            },
            {
                "id": "56f2b8a0cd7338182f63ca69",
                "username": "secadmin",
                "password": "seven10",
                "time_created": "Mar 23, 2016 11:39:12 AM",
                "last_login_time": "Mar 23, 2016 11:39:12 AM",
                "role": "SECURITY_ADMINISTRATOR"
            },
            {
                "id": "57053c95c694ae42c0a61b90",
                "username": "testUser",
                "password": "seven10",
                "time_created": "Apr 6, 2016 12:43:01 PM",
                "last_login_time": "Apr 6, 2016 12:43:01 PM",
                "role": "SECURITY_ADMINISTRATOR"
            }
        ]);

        this.getUsers = function() {
            return usersPromise;
        };

        this.getUser = function(id) {
            return usersPromise.then(function(users) {
                for(var i=0; i<users.length; i++) {
                    if ( users[i].id == id) return users[i];
                }
            });
        };

        this.addUser = function (user) {
            return true;
        }
    }

    function UserDialogController($scope, $mdDialog, roles) {
        $scope.roles = roles;
        $scope.user = {};
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

