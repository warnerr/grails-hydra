(function() {
    angular
        .module('app.devices', ['app', 'ngMaterial', ['app/devices/device.service.js']])
        .component('devices', {
            templateUrl: 'app/devices/devices.html',
            controller: DevicesComponent
        });

    function DevicesComponent(deviceService) {
        var $ctrl = this;

        $ctrl.$onInit = function() {
            return deviceService .getDevices().then(function(devices) {
                $ctrl.devices = devices.data;
                $ctrl.totalDevices = devices.data.length;
            });
        };
    }
})();
