package com.seven10.hydra.user

import grails.transaction.Transactional
import grails.validation.ValidationException
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
            newUser.save()
        }
        else {
            throw new ValidationException("User is not valid", newUser.errors)
        }

        newUser
    }

    def deleteUser(String userId) {
        def user = User.findById(new ObjectId(userId))
        def status = user.delete()
        ['id': userId, 'status': status]

    }

    def updateUser(String userId, Map userUpdates) {
        def user = User.get(new ObjectId(userId))
        if (user) {
            user.properties = userUpdates
            if (user.validate()) {
                user.save()
            } else {
                throw new ValidationException("User is not valid", user.errors)
            }
        }
        user
    }
}
