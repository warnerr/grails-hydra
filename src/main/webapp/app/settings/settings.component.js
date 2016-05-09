/**
 * Created by rwarner on 4/7/16.
 */
(function() {
    angular
        .module('app.settings', ['ngMaterial', 'ngComponentRouter', 'md.data.table', ['app/settings/user.service.js', 'app/settings/users/users.component.js', 'app/settings/users/userDialog.service.js']])
        .component('settings', {
            templateUrl: 'app/settings/settings.html',
            controller: SettingsComponent
        });

    function SettingsComponent() {

    }
})();

