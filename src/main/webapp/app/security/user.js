/**
 * Created by rwarner on 4/7/16.
 */
(function() {
    var user = angular.module('ap.userService', []);

    user.service('userService', {
        template: 'User <br><br><ng-outlet></ng-outlet>',
        $routeConfig: [
            { path: '/profile', name: 'Profile', component: 'userProfile' }
        ]
    });

    user.component('userProfile', {
        template: 'Profile'
    });
})();