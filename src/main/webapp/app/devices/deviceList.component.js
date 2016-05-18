(function() {
    angular
        .module('app.devices')
        .component('deviceList', {
            templateUrl: 'app/devices/devices.html',
            controller: DeviceListComponent
        });

    function DeviceListComponent(deviceService) {
        var $ctrl = this;
        $ctrl.optionsClick = function (option) {
            console.log(" option has been clicked  " + option);
        }

        $ctrl.$onInit = function() {
            return deviceService.getDevices().then(function(devices) {
                $ctrl.devices = devices.data;
                $ctrl.totalDevices = devices.data.length;
            });
        }
    }
})();
