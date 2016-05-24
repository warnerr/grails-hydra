package com.seven10.hydra.device

import org.bson.types.ObjectId

class Export {

    ObjectId id
    PathType type
    String authUser
    String authPassword
    String authGroup
    String parentId
    String name
    String path
    boolean manual = true
    boolean selected = true
    Date dateCreated
    Date lastUpdated

    static constraints = {
        authPassword nullable: true
        authUser nullable: true
    }

    static mapping = {
        sort "lastUpdated"
        authUser attr: 'auth_user'
        authPassword attr: 'auth_pass'
        authGroup attr: 'auth_group'
    }
}
