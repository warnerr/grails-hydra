/**
 * Created by rwarner on 4/7/16.
 */
(function() {
    angular
        .module('app.dashboard', ['app', 'ngMaterial', 'nvd3', 'oc.lazyLoad', [
            'app/dashboard/charts/jobStatus/jobByStatus.component.js',
            'app/dashboard/charts/deviceType/deviceType.component.js',
            'app/dashboard/charts/migrations/migrations.component.js',
            'app/dashboard/charts/throughput/fileThroughput.component.js',
            'app/dashboard/lists/failedJobs.component.js',
            'app/dashboard/charts/throughput/jobThroughput.component.js'
            ]
        ])
        .component('dashboard', {
            templateUrl: 'app/dashboard/dashboard.html',
            controller: DashboardCharts
        });

    function DashboardCharts($ocLazyLoad) {

    }

})();