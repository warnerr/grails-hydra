(function() {
    angular
        .module('app.devices', ['ngMaterial', 'md.data.table', ['app/devices/device.service.js', 'app/devices/deviceList.component.js', 'app/devices/deviceDetail.component.js']])
        .component('devices', {
            template: '<ng-outlet></ng-outlet>',
            $routeConfig: [
                  {path:'/',    name: 'DeviceList',   component: 'deviceList', useAsDefault: true},
                  {path:'/:id', name: 'DeviceDetail', component: 'deviceDetail'}
                ]
        });

   /* function DevicesComponent(deviceService) {
        var $ctrl = this;
        $ctrl.optionsClick = function (option) {
            console.log(" option has been clicked  " + option);
        }
        $ctrl.$onInit = function() {
            return deviceService.getDevices().then(function(devices) {
                $ctrl.devices = devices.data;
                $ctrl.totalDevices = devices.data.length;
            });
        };
    }*/
})();
