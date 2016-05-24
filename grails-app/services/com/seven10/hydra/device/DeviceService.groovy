package com.seven10.hydra.device

import com.seven10.hydra.resource.discovery.ResourceDiscovery
import grails.transaction.Transactional
import grails.validation.ValidationException
import org.bson.types.ObjectId

@Transactional
class DeviceService {

    def exportService

    def getDevices() {
        List<Device> allDevices = Device.list()
        allDevices
    }

    def getDevice(String deviceId) {
        def device = Device.findById(new ObjectId(deviceId))
        device ?: []
    }

    def addDevice(Map device) {
        def newDevice = new Device(device)
        if (newDevice.validate()) {
            newDevice.save()
        } else {
            throw new ValidationException("Device is not valid", newDevice.errors)
        }

        newDevice
    }

    def deleteDevice(String deviceId) {
        def device = Device.findById(new ObjectId(deviceId))
        def status = device.delete()
        ['id': deviceId, 'status': status]

    }

    def updateDevice(String deviceId, Map deviceUpdates) {
        def device = Device.get(new ObjectId(deviceId))
        def deviceData = [:] << deviceUpdates
        deviceData.remove('id')
        if (device) {
            device.properties = deviceData
            if (device.validate()) {
                device.save()
            } else {
                throw new ValidationException("Device is not valid", device.errors)
            }
        }
        device
    }
}
