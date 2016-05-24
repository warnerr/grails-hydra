/**
 * Created by rwarner on 5/16/16.
 */
(function () {
    angular
        .module('app.devices')
        .service('deviceService', DeviceService);


    function DeviceService($http, $q) {
        var devices,
            types = [
                {
                    id: 'FILE',
                    label: 'File'
                }
            ],
            subTypes = [
                {
                    id: 'LOCAL',
                    label: 'Local'
                },
                {
                    id: 'GENERIC',
                    label: 'Generic'
                },
                {
                    id: 'WINDOWS',
                    label: 'Windows'
                },
                {
                    id: 'LINUX',
                    label: 'Linux'
                },
                {
                    id: 'ISILON',
                    label: 'Isilon'
                },
                {
                    id: 'VNX',
                    label: 'VNX'
                },
                {
                    id: 'NETAPP',
                    label: 'NetApp'
                },
                {
                    id: 'CENTERA',
                    label: 'Centera'
                },
                {
                    id: 'ECS',
                    label: 'ECS'
                },
                {
                    id: 'S3',
                    label: 'S3'
                },
                {
                    id: 'SWIFT',
                    label: 'Swift'


                }
            ],
            discoveryTypes = [{id: 'DISCOVER', label: 'Discover'}, {id: 'MANUAL', label: 'Manual'}],
            resourceTypes = [{id: 'EXPORT', label: 'Export'}, {id: 'LABEL', label: 'Share'}];

        this.getDiscoveryTypes = function() {
            return discoveryTypes;
        };

        this.getResourceTypes = function() {
            return resourceTypes;
        };

        this.getSubTypes = function () {
            return subTypes;
        };

        this.getTypes = function () {
            return types;
        };

        this.getDevices = function () {
            return $http.get('device/index');
        };

        this.getDevicesWithResources = function () {
            return $http.get('device/resources');
        };

        this.getDevice = function (id) {
            return $http.get('device/show/' + id);
        };

        this.updateDevice = function (device, resources) {
            var updateDevice = $http.put('device/update/' + device.id, device),
                updateResources = $http.put('export/save/' + device.id, resources);

            return $q.all([updateDevice, updateResources]);
        };

        this.addDevice = function (device) {
            return $http.post('device/create', device);
        }

        this.discoverResources = function (id) {
            return $http.get('export/discover/' + id);
        }

        this.getResources = function (id) {
            return $http.get('export/resources');
        }

        this.saveResources = function (id, resources) {
            return $http.put('export/save/' + id, resources);
        }
    }
})();