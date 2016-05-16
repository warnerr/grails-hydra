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
                        }]
                        );
        this.getSideMenu = function () {
                    return sideMenu;
                };


    }
})();