/**
 * Created by rwarner on 4/7/16.
 */
(function() {
    angular
        .module('app')
        .component('layoutShell', {
            templateUrl: 'layout/layoutShell.html',
            controller: ['$rootRouter', '$ocLazyLoad', function ($router, $ocLazyLoad) {
;
                this.sideMenu = [{
                        label: 'Dashboard',
                        url: './dashboard',
                        selected: 'false'
                    },
                    {
                        label: 'Jobs',
                        url: './jobs',
                        selected: 'false'
                    },
                    {
                        label: 'Devices',
                        url: './devices',
                        selected: 'false'
                    },
                    {
                        label: 'Settings',
                        url: './security',
                        selected: 'false'
                    }];
                $router.config([
                    {
                        path: '/dashboard',
                        name: 'Dashboard',
                        useAsDefault: true,
                        loader: function () {
                            $ocLazyLoad.load('dashboard/charts/jobByStatus.component.js');
                            $ocLazyLoad.load('dashboard/charts/deviceType/deviceType.component.js');
                            return $ocLazyLoad.load('dashboard/dashboard.component.js')
                                .then(function () {
                                    return 'dashboard';
                                });
                        }
                    },
                    {
                        path: '/security',
                        name: 'Security',
                        loader: function () {
                            return $ocLazyLoad.load('security/security.component.js')
                                .then(function () {
                                    return 'security';
                                });
                        }
                    },
                    {path: '/jobs', name: 'Jobs', component: 'jobs'},
                    {path: '/devices', name: 'Devices', component: 'devices'},
                    {
                        path: '/user/...',
                        name: 'User',
                        loader: function () {
                            // lazy load the user module
                            return $ocLazyLoad.load('security/user.js')
                                .then(function () {
                                    // return the user component name
                                    return 'user';
                                });
                        }
                    }
                ]);

            }]
        });
})();