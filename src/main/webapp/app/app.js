(function() {
    var app = angular.module('app', ['oc.lazyLoad', 'ngComponentRouter', 'ngMaterial'])
        .config(function ($mdIconProvider, $mdThemingProvider, $locationProvider) {

            $mdIconProvider
                .defaultIconSet("./assets/svg/avatars.svg", 128);

            $mdThemingProvider.theme('default')
                .primaryPalette('indigo', {
                    'default': '600'
                })
                .accentPalette('red');
            $mdThemingProvider.theme('input', 'default')
                .primaryPalette('grey');

        })
        .value('$routerRootComponent', 'app')
        .component('app', {
            controllerAs: 'vm',
            controller: ['$rootRouter', '$ocLazyLoad', function ($router, $ocLazyLoad) {
                var vm = this;
                vm.test = 'test';
                vm.addUser = function(ev) {
                    console.log(ev);
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
                                    }
                                );
                        }
                    },
                    {path: '/jobs', name: 'Jobs', component: 'jobs'},
                    {path: '/devices', name: 'Devices', component: 'devices'}
                ]);
        }]
    });
    app.component('jobs', {
        template: 'Jobs'
    });

    app.component('devices', {
        template: 'Devices'
    });
})();