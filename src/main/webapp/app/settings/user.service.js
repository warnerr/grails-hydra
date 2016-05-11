/**
 * Created by rwarner on 5/2/16.
 */
(function() {
    angular
        .module('app.settings')
        .service('userService', UserService);


    function UserService($http) {
        var usersPromise = $http.get('user/index', { headers: { 'Cache-Control' : 'no-cache' } });

        /*var usersPromise = $q.when([
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
         ]);*/

        this.getUsers = function() {
            return $http.get('user/index');
        };

        this.getUser = function(id) {
            return usersPromise.then(function(users) {
                for(var i=0; i<users.length; i++) {
                    if ( users[i].id == id) return users[i];
                }
            });
        };

        this.addUser = function (user) {
            console.log(user);
            return $http.post('user/create', user);
        }
    }
})();
