(function() {
    angular.module('app', ['oc.lazyLoad', 'ngComponentRouter', 'ngMaterial'])
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
        .value('$routerRootComponent', 'app');

})();