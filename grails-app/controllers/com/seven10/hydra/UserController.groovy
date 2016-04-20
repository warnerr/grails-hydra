package com.seven10.hydra

import grails.converters.JSON

class UserController {

    def userService

    def index() {
        respond userService.getUsers()
    }

    def create() {
        def user = request.JSON
        def newUser = userService.addUser(user)
        respond newUser
    }

    def show() {
        def id = params.id
        def user =  userService.getUser(id)
        respond user
    }

    def delete() {
        def id = params.id
        render userService.deleteUser(id) as JSON
    }

    def update() {
        def id = params.id
        def user = request.JSON
        respond userService.updateUser(id, user)
    }
}
