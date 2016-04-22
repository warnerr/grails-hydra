/**
 * Created by rwarner on 4/7/16.
 */
(function() {
    angular
        .module('app.dashboard.charts', ['ngMaterial', 'nvd3'])
        .component('jobStatus', {
            templateUrl: 'app/dashboard/charts/cardChart.html',
            controller: JobStatusChart
        }).service('jobStatusService', JobStatusService);

    function JobStatusChart(jobStatusService) {
        var $ctrl = this;
        $ctrl.options = {
            chart: {
                type: 'pieChart',
                height: 250,
                x: function (d) {
                    return d.label;
                },
                y: function (d) {
                    return d.total;
                },
                showLabels: false,
                duration: 500,
                labelThreshold: 0.01,
                labelSunbeamLayout: true,
                labelOutside: true,
                pie: {
                    dispatch: {
                        elementClick: function(e) {
                            console.log(e.data.label);
                        }
                    }
                },
                legend: {
                    margin: {
                        top: 5,
                        right: 35,
                        bottom: 5,
                        left: 0
                    }
                }
            }
        };
        $ctrl.data = [];

        $ctrl.$onInit = function() {
            // Load up the status list for this view
            return jobStatusService.getJobStatusList().then(function(statusList) {
                $ctrl.data = statusList;
            });
        };
    }

    function JobStatusService($q) {
        var jobStatusPromise = $q.when([
            {
                status: 'NOT_STARTED',
                label: 'Not Started',
                total: '4'
            },
            {
                status: 'RUNNING',
                label: 'Running',
                total: '3'
            },
            {
                status: 'PAUSED',
                label: 'Paused',
                total: '5'
            },
            {
                status: 'COMPLETED',
                label: 'Completed',
                total: '6'
            },
            {
                status: 'STOPPED',
                label: 'Stopped',
                total: '7'
            }
        ]);

        this.getJobStatusList = function() {
            return jobStatusPromise;
        };

        this.getJobStatus = function(status) {
            return jobStatusPromise.then(function(statusList) {
                for(var i=0; i< statusList.length; i++) {
                    if ( statusList[i].status == status) return statusList[i];
                }
            });
        };
    }
})();