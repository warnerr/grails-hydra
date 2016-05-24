package com.seven10.hydra.device

import com.seven10.hydra.resource.discovery.ResourceDiscovery
import com.seven10.restobjects.devices.resource.DiscoveredResource
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

    def saveResources(String deviceId, List resources) {
        resources.each { resource ->
            if (resource.id) {
                updateExport(resource.id, resource)
            } else {
                addExport(resource)
            }
        }
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
                export.save(flush: true, failOnError: true)
            } else {
                throw new ValidationException("Device is not valid", export.errors)
            }
        }

        export
    }

    def getResources(String deviceId, boolean discover) {
        Device device = Device.get(new ObjectId(deviceId))
        List shares = Export.findAllByParentId(deviceId) ?: []

        if (discover) {
            shares = discoverResources(device, shares)
        }

        shares
    }

    def discoverResources(Device device, List shares) {
        List discoveredShares = []
        discoveredShares.addAll(shares)
        String port = device.managementPort.toString()

        ResourceDiscovery resourceDiscoverer = new ResourceDiscovery(device.address[0].toString(),
                device.managementUser.toString(),
                device.managementPassword.toString(),
                port);

        device.address.each { address ->
            resourceDiscoverer.setDeviceAddress(address)
            if (device.managementPassword && device.managementUser) {
                try {
                    resourceDiscoverer.getAllShares().each { discoveredResource ->
                        setPath(discoveredResource, device, discoveredShares, PathType.SHARE)
                    }
                } catch (IllegalArgumentException exception) {
                    log.info("bad url or user credentials when mapping shares " + exception)
                }
            }
            resourceDiscoverer.getAllExports().each { discoveredResource ->
                setPath(discoveredResource, device, discoveredShares, PathType.EXPORT)
            }
        }

        discoveredShares
    }

    def setPath(DiscoveredResource discoveredResource, Device device, List discoveredShares, PathType type) {
        def path = discoveredResource.path
        def domain = device.domain
        if (isNewPath(discoveredShares, path)) {
            Export newExport = new Export(path: path, name: discoveredResource.name, parentId: device.id.toString(), authGroup: domain, type: type, manual: false)
            newExport = newExport.save(flush: true, failOnError: true)
            discoveredShares.push(newExport)
        }
    }

    def isNewPath(deviceExports, path) {
        def matchingExport = deviceExports.find { export ->
            export.path == path
        }

        matchingExport == null
    }
}
