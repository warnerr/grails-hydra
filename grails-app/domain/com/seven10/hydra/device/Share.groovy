package com.seven10.hydra.device

import org.bson.types.ObjectId

class Share {

    ObjectId id
    String authUser
    String authPassword
    String authGroup
    String parentId
    String name
    String path

    static constraints = {
    }

    static mapping = {
        authUser attr: 'auth_user'
        authPassword attr: 'auth_pass'
        authGroup attr: 'auth_group'
        parentId attr: 'parent_id'
    }
}
