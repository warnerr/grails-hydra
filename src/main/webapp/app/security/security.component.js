/**
 * Created by rwarner on 4/7/16.
 */
(function() {
    angular
        .module('app.security', ['ngMaterial', 'ngComponentRouter', 'md.data.table', ['app/security/user.service.js']])
        .component('security', {
            templateUrl: 'app/security/security.html',
            controller: UserListComponent
        });

    function UserListComponent(userService, $mdDialog) {
        var selectedId = null;
        var $ctrl = this;
        $ctrl.selected = [];
        $ctrl.isAddingUser = false;

        $ctrl.limitOptions = [5, 10, 15];
        $ctrl.query = {
            order: 'userName',
            limit: 5,
            page: 1
        };

        $ctrl.options = {
            rowSelection: false,
            multiSelect: false,
            autoSelect: false,
            decapitate: false,
            largeEditDialog: false,
            boundaryLinks: false,
            limitSelect: true,
            pageSelect: true
        };

        $ctrl.logItem = function (item) {
            console.log(item.name, 'was selected');
        };

        $ctrl.logOrder = function (order) {
            console.log('order: ', order);
        };

        $ctrl.logPagination = function (page, limit) {
            console.log('page: ', page);
            console.log('limit: ', limit);
        };
       /* function success(users) {
            $ctr.users = users;
        }*/
        $ctrl.$routerOnActivate = function(next, previous) {
            // Load up the heroes for this view
            return userService.getUsers().then(function(users) {
                $ctrl.users = users.data;
                $ctrl.totalUsers = users.data.length;
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
                console.log("user == " + answer.userName + "   status == " + status);
            }, function() {
                console.log('You cancelled the dialog.');
            });
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

