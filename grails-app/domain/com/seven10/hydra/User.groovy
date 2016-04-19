package com.seven10.hydra

import org.bson.types.ObjectId

class User {

    ObjectId id
    String userName
    String password
    Date timeCreated
    Date lastLogin
    String role

    static constraints = {
    }

    static mapping = {
        userName attr: "username"
        timeCreated attr: "time_created"
        lastLogin attr: "last_login_time"

    }
}
