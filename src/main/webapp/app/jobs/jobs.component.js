(function() {
    angular
        .module('app.jobs', ['ngMaterial'])
        .component('jobs', {
            templateUrl: 'app/jobs/jobs.html',
            controller: JobsComponent
        });

    function JobsComponent() {

    }
})();
