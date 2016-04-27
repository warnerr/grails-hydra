/**
 * Created by rwarner on 4/7/16.
 */
(function () {
    angular
        .module('app')
        .component('appLayout', {
            controllerAs: 'vm',
            templateUrl: 'app/layout/layoutShell.html',
            controller: ['$rootRouter', '$ocLazyLoad', '$mdSidenav', function ($router, $ocLazyLoad, $mdSidenav) {
                var vm = this;
                vm.sideMenu = [
                    {
                        label: 'Dashboard',
                        url: '#dashboard',
                        selected: true,
                        icon: 'zmdi-view-dashboard'
                    },
                    {
                        label: 'Jobs',
                        url: '#jobs',
                        selected: false,
                        icon: 'zmdi-case'
                    },
                    {
                        label: 'Devices',
                        url: '#devices',
                        selected: false,
                        icon: 'zmdi-devices'
                    },
                    {
                        label: 'Settings',
                        url: '#security',
                        selected: false,
                        icon: 'zmdi-settings'
                    }];
                vm.selectMenu = function (menuItem) {
                    console.log(menuItem);
                    angular.forEach(vm.sideMenu, function (item) {
                        item.selected = false;
                    });
                    menuItem.selected = true;
                };

                vm.toggleMenu = function() {
                    $mdSidenav('left').toggle();
                };
                $router.config([
                    {
                        path: '/dashboard',
                        name: 'Dashboard',
                        useAsDefault: true,
                        loader: function () {
                            return $ocLazyLoad.load('app/dashboard/dashboard.component.js')
                                .then(function () {
                                    return 'dashboard';
                                });
                        }
                    },
                    {
                        path: '/security',
                        name: 'Security',
                        loader: function () {
                            return $ocLazyLoad.load('app/security/security.component.js')
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
                            return $ocLazyLoad.load('app/security/user.js')
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