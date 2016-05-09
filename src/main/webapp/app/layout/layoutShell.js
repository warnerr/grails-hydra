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
                        url: '#settings',
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

                vm.toggleMenu = function () {
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
                        path: '/settings',
                        name: 'Settings',
                        loader: function () {
                            return $ocLazyLoad.load('app/settings/settings.component.js')
                                .then(function () {
                                    return 'settings';
                                });
                        }
                    },
                    {
                        path: '/jobs', name: 'Jobs', component: 'jobs'
                    },
                    {
                        path: '/devices', name: 'Devices', component: 'devices'
                    }
                ]);
            }]
        });
})();