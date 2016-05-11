package com.seven10.hydra.device

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationException

@Secured('ROLE_ADMIN')
class DeviceController {

    def deviceService

    def index() {
        respond deviceService.getDevices()
    }

    def create() {
        def device = request.JSON
        def newDevice = deviceService.addDevice(device)
        respond newDevice
    }

    def show() {
        def id = params.id
        def device =  deviceService.getDevice(id)
        respond device
    }

    def delete() {
        def id = params.id
        render deviceService.deleteDevice(id) as JSON
    }

    def update() {
        def id = params.id
        def device = request.JSON
        respond deviceService.updateDevice(id, device)
    }

    def handleValidationException(ValidationException validationException) {
        [problemDescription: validationException.errors[0]]
    }
}
