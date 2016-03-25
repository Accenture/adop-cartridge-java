package com.java.cartridge

import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that EnvironmentProvisioning Pipeline View dsl works as expected.
 */
class EnvironmentProvisioningPipelineSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/environment_provisioning.groovy')

    @Shared
    def Node node = new XmlParser().parseText(helper.jm.savedViews["${helper.projectName}/Environment_Provisioning"])

    def 'view "Environment_Provisioning" exists'() {
        expect:
            helper.jm.savedViews[viewName] != null

        where:
            viewName = "${helper.projectName}/Environment_Provisioning"
    }

    @Unroll
    def 'title of view is "#pipelineTitle"'() {
        expect:
            node.buildViewTitle.size() == 1
            node.buildViewTitle.text() == pipelineTitle

        where:
            pipelineTitle = 'Environment Provisioning Pipeline'
    }

    @Unroll
    def 'first trigger on job in view is "#jenkinsJobName"'() {
        expect:
            node.selectedJob.size() == 1
            node.selectedJob.text() == jenkinsJobName

        where:
            jenkinsJobName = 'Create_Environment'
    }

    @Unroll
    def 'number of display builds in view is "#numberOfBuilds"'() {
        expect:
            node.noOfDisplayedBuilds.size() == 1
            node.noOfDisplayedBuilds.text() == numberOfBuilds

        where:
            numberOfBuilds = '5'
    }
}
