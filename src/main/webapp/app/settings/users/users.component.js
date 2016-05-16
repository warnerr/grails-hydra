(function() {
    angular
        .module('app.settings')
        .component('users', {
            templateUrl: 'app/settings/users/users.html',
            controller: UserListComponent
        });

    function UserListComponent(userService, $mdDialog, userDialog) {
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
        $ctrl.$onInit = function(next) {
            return $ctrl.loadUsers();
        };

        $ctrl.isSelected = function(user) {
            return (user.id == selectedId);
        };

        $ctrl.addUser = function (ev) {
            console.log('adding user');
            var userDialogResponse = userDialog.addUser($ctrl);

        };
        
        $ctrl.loadUsers = function () {
            return userService.getUsers().then(function(users) {
                $ctrl.users = users.data;
                $ctrl.totalUsers = users.data.length;
            });
        }

    }

})();
