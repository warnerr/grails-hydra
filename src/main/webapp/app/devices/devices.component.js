(function() {
    angular
        .module('app.devices', ['ngMaterial'])
        .component('devices', {
            templateUrl: 'app/devices/devices.html',
            controller: DevicesComponent
        });

    function DevicesComponent() {

    }
})();
