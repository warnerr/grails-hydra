package com.seven10.hydra.device

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationException

/**
 * Created by rwarner on 5/17/16.
 */
@Secured('ROLE_ADMIN')
class ExportController {

    def exportService

    def index() {
        respond exportService.getExports()
    }

    def create() {
        def export = request.JSON
        def newExport = exportService.addExport(export)
        respond newExport
    }

    def show() {
        def id = params.id
        def export =  exportService.getExport(id)
        respond export
    }

    def delete() {
        def id = params.id
        render exportService.deleteExport(id) as JSON
    }

    def update() {
        def id = params.id
        def export = request.JSON
        respond exportService.updateExport(id, export)
    }

    def save() {
        def id = params.id
        def exports = request.JSON
        render exportService.saveResources(id, exports) as JSON
    }

    def discover() {
        def deviceId = params.id
        respond exportService.getResources(deviceId, true)
    }

    def resources() {
        def deviceId = params.id
        respond exportService.getResources(deviceId, false)
    }

    def handleValidationException(ValidationException validationException) {
        [problemDescription: validationException.errors.toString()]
    }
}
