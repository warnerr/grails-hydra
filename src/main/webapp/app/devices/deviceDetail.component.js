(function() {
    angular
        .module('app.devices')
        .component('deviceDetail', {
            templateUrl: 'app/devices/deviceDetail.html',
            controller: DeviceDetailComponent,
            bindings: { $router: '<' }
        });

    function DeviceDetailComponent(deviceService, $mdToast) {
        var $ctrl = this;
        $ctrl.deviceId;
        $ctrl.device = {};
        $ctrl.resource = {};
        $ctrl.resources = [];
        $ctrl.subTypes;
        $ctrl.types;
        $ctrl.resourceTypes;
        $ctrl.discoveryTypes;
        $ctrl.selected = [];
        $ctrl.isAddingResource = false;

        $ctrl.limitOptions = [5, 10, 15];
        $ctrl.query = {
            order: 'path',
            limit: 5,
            page: 1
        };

        $ctrl.options = {
            rowSelection: true,
            multiSelect: true,
            autoSelect: false,
            decapitate: false,
            largeEditDialog: false,
            boundaryLinks: false,
            limitSelect: true,
            pageSelect: true
        };

        $ctrl.selectResource = function (item) {
            item.selected = true;
        };

        $ctrl.deselectResource = function (item) {
            item.selected = false;
        };

        $ctrl.$routerOnActivate = function(next) {
            // Get the crisis identified by the route parameter
            var id = next.params.id;
            $ctrl.subTypes = deviceService.getSubTypes();
            $ctrl.types = deviceService.getTypes();
            if (id) {
                deviceService.getDevice(id).then(function(device) {
                    if (device) {
                        $ctrl.device = device.data;
                        $ctrl.setResources($ctrl.device.resources);
                    } else { // id not found
                        $ctrl.gotoDevices();
                    }
                });
            } else {
                //new device
                $ctrl.device = {};
                $ctrl.subTypes = deviceService.getSubTypes();
                $ctrl.types = deviceService.getTypes();
                $ctrl.setResources([]);
            }
        };

        $ctrl.setResources = function (resources) {
            $ctrl.resources = resources
            angular.forEach(resources, function (value) {
                if (!value.hasOwnProperty('selected') || value.selected) {
                    value.selected = true;
                    $ctrl.selected.push(value);
                }
            });
        };

        $ctrl.discoverResources = function(id) {
            var deviceId = id ? id : $ctrl.device.id;
            deviceService.discoverResources(deviceId).then(function (resources) {
                $ctrl.setResources(resources.data);
            });
        };

        $ctrl.showSimpleToast = function(message, type) {
            $mdToast.show($mdToast.simple().content(message).theme(type + "-toast").hideDelay(6000).position('top right'));
        };

        $ctrl.saveDevice = function () {
            if ($ctrl.device.id) {
                deviceService.updateDevice($ctrl.device, $ctrl.resources).then(function () {
                    $ctrl.showSimpleToast('Device saved.', 'success');
                    $ctrl.gotoDevices();
                }, function () {
                    $ctrl.showSimpleToast('Failed to save device.', 'error');
                });
            } else {
                deviceService.addDevice($ctrl.device).then(function (device) {
                    $ctrl.device = device.data;
                    $ctrl.showSimpleToast('Device saved.', 'success');
                }, function () {
                    $ctrl.showSimpleToast('Failed to save device.', 'error');
                });
            }
        };

        $ctrl.gotoDevices = function() {
            this.$router.navigate(['DeviceList']);
        };
    }
})();
