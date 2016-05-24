package com.seven10.hydra.device

import org.bson.types.ObjectId

class Device {

    ObjectId id
    String name
    String description
    DeviceType type
    DeviceSubType subType
    String[] address
    int managementPort
    String managementUser
    String managementPassword
    String status = DeviceStatus.CONNECTED
    String domain
    boolean hasShares

    static constraints = {
    }

    static mapping = {
        version false
        subType attr: 'sub_type'
        managementPort attr: 'mgmt_port'
        managementUser attr: 'mgmt_user'
        managementPassword attr: 'mgmt_pass'
    }

    List<Export> getExports() {
        return Export.findAllByParentId(id)
    }
}
