package com.seven10.hydra.device

import grails.validation.ValidationException
import org.bson.types.ObjectId

/**
 * Created by rwarner on 5/17/16.
 */
class ExportService {

    def getExports() {
        Export.list(max: 1, sort: 'name', order: 'desc')
    }

    def getExport(String exportId) {
        def export = Export.findById(new ObjectId(exportId))
        export ?: []
    }

    def addExport(Map export) {
        def newExport = new Export(export)
        if (newExport.validate()) {
            newExport.save()
        }
        else {
            throw new ValidationException("Device is not valid", newExport.errors)
        }

        newExport
    }

    def deleteExport(String exportId) {
        def export = Export.findById(new ObjectId(exportId))
        def status = export.delete()
        ['id': exportId, 'status': status]

    }

    def updateExport(String exportId, Map exportUpdates) {
        def export = Export.get(new ObjectId(exportId))
        if (export) {
            export.properties = exportUpdates
            if (export.validate()) {
                export.save()
            } else {
                throw new ValidationException("Device is not valid", export.errors)
            }
        }

        export
    }
}
