/**
 * Created by rwarner on 4/7/16.
 */
(function () {
    angular
        .module('app')
        .component('appLayout', {
            controllerAs: 'vm',
            templateUrl: 'app/layout/layoutShell.html',
            controller: ['$rootRouter', '$ocLazyLoad', '$mdSidenav', '$location', 'menuService', LayoutController]
        });

        function LayoutController ($router, $ocLazyLoad, $mdSidenav, $location, menuService) {

            var vm = this;
            vm.sideMenu;
            /*[
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
                }];*/
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

            vm.$onInit = function () {
                var locationPath = $location.path();
                menuService.getSideMenu().then(function (menuItems) {
                    vm.sideMenu = menuItems;
                    angular.forEach(vm.sideMenu, function (item) {
                        if (item.url.substr(1) === locationPath.substr(1)) {
                            vm.selectMenu(item);
                        }
                    });
                });
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
                    path: '/jobs', name: 'Jobs', loader: function () {
                    return $ocLazyLoad.load('app/jobs/jobs.component.js')
                        .then(function () {
                            return 'jobs';
                        });
                }
                },
                {
                    path: '/devices', name: 'Devices', loader: function () {
                    return $ocLazyLoad.load('app/devices/devices.component.js')
                        .then(function () {
                            return 'devices';
                        });
                }
                }
            ]);

        }
})();