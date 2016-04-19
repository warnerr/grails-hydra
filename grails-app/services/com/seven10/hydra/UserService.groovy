package com.seven10.hydra

import grails.transaction.Transactional
import org.bson.types.ObjectId

@Transactional
class UserService {

    def getUsers() {
        User.list()
    }

    def getUser(String userId) {
        def user = User.findById(new ObjectId(userId))
        user ?: []
    }

    def addUser(Map user) {
        def newUser = new User(user)
        if (newUser.validate()) {
            newUser.save(flush: true)
        }
        else {
            newUser.errors.allErrors.each {
                println it
            }
        }

        newUser
    }

    def deleteUser(String userId) {
        def user = User.findById(new ObjectId(userId))
        def status = user.delete(flush: true)
        ['id': userId, 'status': status]

    }

    def updateUser(String userId, Map userUpdates) {
        def user = User.findById(new ObjectId(userId))
        user.properties = userUpdates
        if (user.validate()) {
            user.save(flush: true)
        }
        else {
            user.errors.allErrors.each {
                println it
            }
        }
        user
    }
}
