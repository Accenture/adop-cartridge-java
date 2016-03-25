package com.java.cartridge

import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that EnvironmentProvisioning dsl works as expected.
 */
class EnvironmentProvisioningSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/environment_provisioning.groovy')

    List<String> expectedJobs = [
        "${helper.projectName}/Create_Environment",
        "${helper.projectName}/Destroy_Environment",
        "${helper.projectName}/List_Environment"
    ]

    List<String> expectedViews = [
        "${helper.projectName}/Environment_Provisioning"
    ]

    def 'load jobs'() {
        expect:
            helper.items.getJobs() != null
            helper.items.getJobs().size() == expectedJobs.size()
    }

    def 'load views'() {
        expect:
            helper.items.getViews() != null
            helper.items.getViews().size() == expectedViews.size()
    }

    def 'should generate exactly the expected jobs'() {
        expect:
            actualJobs == expectedJobs

        where:
            actualJobs = helper.items.getJobs().jobName
    }

    def 'should generate exactly the expected views'() {
        expect:
            actualViews == expectedViews

        where:
            actualViews = helper.items.getViews().name
    }
}
