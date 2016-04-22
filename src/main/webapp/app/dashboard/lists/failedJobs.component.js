/**
 * Created by rwarner on 4/22/16.
 */
(function () {
    angular
        .module('app.dashboard')
        .component('failedJobs', {
            templateUrl: 'app/dashboard/lists/failedJobs.html',
            controller: FailedJobs
        }).service('failedJobsService', FailedJobsService);

    function FailedJobs(failedJobsService) {
        var $ctrl = this;

        $ctrl.failedJobs = [];


        $ctrl.$onInit = function () {
            // Load up the status list for this view
            return failedJobsService.getFailedJobs().then(function (failedJobs) {
                $ctrl.failedJobs = failedJobs;
            });
        };
    }

    function FailedJobsService($q) {
        var failedJobs = $q.when([
            {
                error: 'Permission denied',
                jobName: 'Export Job',
                createDate: '2016-03-25 15:21:28.621Z',
                lastExcuted: '2016-03-29 12:47:02.620Z'
            },
            {
                error: 'Failed to read source',
                jobName: 'Share Job',
                createDate: '2016-01-25 15:21:28.621Z',
                lastExcuted: '2016-01-29 12:47:02.620Z'
            },
            {
                error: 'Failed to copy to destination',
                jobName: 'Large Job',
                createDate: '2016-02-25 15:21:28.621Z',
                lastExcuted: '2016-02-29 12:47:02.620Z'
            },
            {
                error: 'Permission denied',
                jobName: 'Windows Job',
                createDate: '2016-03-15 15:21:28.621Z',
                lastExcuted: '2016-03-19 12:47:02.620Z'
            },
            {
                error: 'Failed to connect',
                jobName: 'Linux Job',
                createDate: '2016-03-12 15:21:28.621Z',
                lastExcuted: '2016-03-22 12:47:02.620Z'
            }
            ]);

        this.getFailedJobs = function () {
            return failedJobs;
        };
    }


})();