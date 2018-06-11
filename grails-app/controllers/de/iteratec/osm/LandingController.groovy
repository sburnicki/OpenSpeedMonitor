package de.iteratec.osm

import grails.plugin.springsecurity.LoginController

class LandingController extends LoginController {

    ConfigService configService
    OsmStateService osmStateService

    def index() {
        String postUrl = request.contextPath + conf.apf.filterProcessesUrl
        def model = [postUrl            : postUrl,
                     rememberMeParameter: conf.rememberMe.parameter,
                     usernameParameter  : conf.apf.usernameParameter,
                     passwordParameter  : conf.apf.passwordParameter,
                     gspLayout          : conf.gsp.layoutAuth,
                     isSetupFinished    : false]

        if (configService.infrastructureSetupRan != OsmConfiguration.InfrastructureSetupStatus.FINISHED) {
            if (osmStateService.untouched()) {
                if (configService.infrastructureSetupRan == OsmConfiguration.InfrastructureSetupStatus.NOT_STARTED) {
                    forward(controller: 'InfrastructureSetup', action: 'index')
                }
                if (configService.infrastructureSetupRan == OsmConfiguration.InfrastructureSetupStatus.ABORTED) {
                    return model
                }
            } else {
                OsmConfiguration config = configService.getConfig()
                config.infrastructureSetupRan = OsmConfiguration.InfrastructureSetupStatus.FINISHED
                config.save(failOnError: true)
            }
        }
        model.isSetupFinished = true
        return model
    }
}
