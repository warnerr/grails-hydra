/**
 * Created by rwarner on 4/7/16.
 */
(function () {
    angular
        .module('app.dashboard')
        .component('jobThroughput', {
            templateUrl: 'app/dashboard/charts/cardChart.html',
            controller: JobThroughputChart
        }).service('jobThroughputService', JobThroughputService);

    function JobThroughputChart(jobThroughputService) {
        var $ctrl = this;
        $ctrl.options = {
            chart: {
                type: 'lineChart',
                height: 250,
                margin: {
                    top: 20,
                    right: 20,
                    bottom: 40,
                    left: 55
                },
                x: function (d) {
                    return d.x;
                },
                y: function (d) {
                    return d.y;
                },
                useInteractiveGuideline: true,
                dispatch: {
                    stateChange: function (e) {
                        console.log("stateChange");
                    },
                    changeState: function (e) {
                        console.log("changeState");
                    },
                    tooltipShow: function (e) {
                        console.log("tooltipShow");
                    },
                    tooltipHide: function (e) {
                        console.log("tooltipHide");
                    }
                },
                xAxis: {
                    axisLabel: 'Time (minutes)'
                },
                yAxis: {
                    axisLabel: 'MB per Second',
                    tickFormat: function (d) {
                        return d3.format('.02f')(d);
                    },
                    axisLabelDistance: -10
                },
                callback: function (chart) {
                    console.log("!!! lineChart callback !!!");
                }
            }
        };

        $ctrl.data = [];

        $ctrl.$onInit = function () {
            // Load up the status list for this view
            return jobThroughputService.getThroughput().then(function (throughput) {
                $ctrl.data = throughput;
            });
        };
    }

    function JobThroughputService($q) {
        var data = [],
            i = 0;
        for (i; i < 100; i++) {
            data.push({x: i, y: (Math.random() * 100)});
        }
        var throughput = $q.when([
                        {
                            values: data,      //values - represents the array of {x,y} data points
                            key: 'Throughput Of Jobs',
                            color: '#ff7f0e',  //color - optional: choose your own line color.
                            strokeWidth: 2
                        }
            ]);

        this.getThroughput = function () {
            return throughput;
        };
    }
})();