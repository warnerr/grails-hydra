/**
 * Created by rwarner on 5/16/16.
 */
(function() {
    angular
        .module('app.devices')
        .service('deviceService', DeviceService);


    function DeviceService($http) {

        this.getDevices = function() {
            return $http.get('device/index');
        };

        this.getDevice = function(id) {
            var i = 0;
            return this.getDevices().then(function(devices) {
                for(i; i < devices.length; i++) {
                    if ( devices[i].id == id) return devices[i];
                }
            });
        };

        this.addDevice = function (device) {
            return $http.post('device/create', device);
        }
    }
})();