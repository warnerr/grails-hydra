package com.seven10.hydra

import org.bson.types.ObjectId

class Device {

    ObjectId id
    String name
    DeviceType type
    DeviceSubType subType
    List address
    long managementPort
    String managementUser
    String managementPassword
    String status
    String domain

    static constraints = {
        
    }
}
