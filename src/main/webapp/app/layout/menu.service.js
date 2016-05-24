/**
 * Created by rwarner on 5/16/16.
 */
(function() {
    angular
        .module('app')
        .service('menuService', MenuService);


    function MenuService($q) {

        var sideMenu = $q.when([
                        {
                            label: 'Dashboard',
                            url: 'Dashboard',
                            selected: true,
                            icon: 'zmdi-view-dashboard'
                        },
                        {
                            label: 'Jobs',
                            url: 'Jobs',
                            selected: false,
                            icon: 'zmdi-case'
                        },
                        {
                            label: 'Devices',
                            url: 'Devices',
                            selected: false,
                            icon: 'zmdi-devices'
                        },
                        {
                            label: 'Settings',
                            url: 'Settings',
                            selected: false,
                            icon: 'zmdi-settings'
                        }]
                        );
        this.getSideMenu = function () {
                    return sideMenu;
                };


    }
})();