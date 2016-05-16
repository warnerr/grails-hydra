/**
 * Created by rwarner on 5/2/16.
 */
(function() {
    angular
        .module('app.settings')
        .service('userService', UserService);


    function UserService($http) {
        var usersPromise = $http.get('user/index', { headers: { 'Cache-Control' : 'no-cache' } });

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
