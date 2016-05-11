package com.seven10.hydra.device

import org.bson.types.ObjectId

class Device {

    ObjectId id
    String name
    DeviceType type
    DeviceSubType subType
    String[] address
    int managementPort
    String managementUser
    String managementPassword
    String status
    String domain

    static constraints = {
    }

    static mapping = {
        subType attr: 'sub_type'
        managementPort attr: 'mgmt_port'
        managementUser attr: 'mgmt_user'
        managementPassword attr: 'mgmt_pass'
    }
}
