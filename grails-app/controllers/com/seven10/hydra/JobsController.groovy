package com.seven10.hydra

import grails.converters.JSON

/**
 * Created by root on 4/25/16.
 */
class JobsController {

    def jobsService

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index () {
        render jobsService.getJobs(params.request)
    }

    def save () {
        def job = request.JSON
        render jobsService.createJob(job)
    }
}
