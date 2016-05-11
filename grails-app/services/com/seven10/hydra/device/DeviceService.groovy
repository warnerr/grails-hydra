package com.seven10.hydra.device

import grails.transaction.Transactional
import org.bson.types.ObjectId

@Transactional
class DeviceService {

    def getDevices() {
        Device.list()
    }

    def getDevice(String deviceId) {
        def device = Device.findById(new ObjectId(deviceId))
        device ?: []
    }

    def addDevice(Map device) {
        def newDevice = new Device(device)
        if (newDevice.validate()) {
            newDevice.save()
        }
        else {
            newDevice.errors.allErrors.each {
                println it
            }
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
        if (device) {
            device.properties = deviceUpdates
            if (device.validate()) {
                device.save()
            } else {
                throw new ValidationException("Device is not valid", device.errors)
            }
        }
        device
    }
}
