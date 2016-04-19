/**
 * Created by rwarner on 4/7/16.
 */
(function() {
    angular
        .module('app.dashboard', ['ngMaterial', 'ngComponentRouter', 'oc.lazyLoad'])
        .component('dashboard', {
            templateUrl: 'app/dashboard/dashboard.html',
            controller: function () {
                console.log('test dashboard controller');
            }
        });
})();