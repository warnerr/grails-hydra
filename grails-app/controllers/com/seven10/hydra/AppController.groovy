package com.seven10.hydra

import grails.plugin.springsecurity.annotation.Secured

class AppController {

    @Secured('ROLE_ADMIN')
    def index() {
        //load angular application
        def htmlContent = grailsApplication.mainContext.getResource('/app/index.html').file.text
        render text: htmlContent, contentType:"text/html", encoding:"UTF-8"
    }

}
