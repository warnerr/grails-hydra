/**
 * Created by rwarner on 4/7/16.
 */
(function () {
    angular
        .module('app.dashboard')
        .component('deviceType', {
            templateUrl: 'app/dashboard/charts/cardChart.html',
            controller: DashboardCharts
        }).service('deviceTypeService', DeviceTypeService);

    function DashboardCharts(deviceTypeService) {
        var $ctrl = this;
        $ctrl.options = {
            chart: {
                type: 'discreteBarChart',
                height: 250,
                forceY: [0, 15],
                margin: {
                    top: 20,
                    right: 20,
                    bottom: 50,
                    left: 55
                },
                x: function (d) {
                    return d.label;
                },
                y: function (d) {
                    return d.total;
                },
                showValues: true,
                duration: 500,
                xAxis: {
                    axisLabel: 'Type'
                },
                yAxis: {
                    axisLabel: 'Number',
                    axisLabelDistance: -10
                }
            }
        };
        $ctrl.data = [];

        $ctrl.$onInit = function () {
            // Load up the status list for this view
            return deviceTypeService.getDeviceTypes().then(function (deviceTypes) {
                $ctrl.data = deviceTypes;
            });
        };
    }

    function DeviceTypeService($q) {
        var deviceTypes = $q.when([
                {
                    key: "DevicesByType",
                    values: [{
                        type: 'SHARE',
                        label: 'Share',
                        total: '3'
                    },
                        {
                            type: 'EXPORT',
                            label: 'Export',
                            total: '10'
                        },
                        {
                            type: 'FOLDER',
                            label: 'Folder',
                            total: '8'
                        },
                        {
                            type: 'POOL',
                            label: 'Pool',
                            total: '11'
                        }]
                }
            ])
            ;

        this.getDeviceTypes = function () {
            return deviceTypes;
        };

        this.getDeviceType = function (deviceType) {
            return deviceTypes.then(function (types) {
                for (var i = 0; i < types.length; i++) {
                    if (types[i].type == deviceType) return types[i];
                }
            });
        };
    }


})();