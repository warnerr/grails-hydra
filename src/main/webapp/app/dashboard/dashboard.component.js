/**
 * Created by rwarner on 4/7/16.
 */
(function() {
    angular
        .module('app.dashboard', ['ngMaterial', 'nvd3', 'oc.lazyLoad'])
        .component('dashboard', {
            templateUrl: 'app/dashboard/dashboard.html',
            controller: DashboardCharts
        });

    function DashboardCharts($ocLazyLoad) {
       
    }

})();