package com.java.cartridge

import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that JavaReferenceApplication/Reference_Application_Regression_Tests job works as expected.
 */
class RegressionTestsReferenceApplicationJobSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/java_reference_application_jobs.groovy')

    @Shared
    def Node node = new XmlParser().parseText(helper.jm.savedConfigs["${helper.projectName}/Reference_Application_Regression_Tests"])

    def 'Reference_Application_Regression_Tests job is exists'() {
        expect:
            helper.jm.savedConfigs[jobName] != null

        where:
            jobName = "${helper.projectName}/Reference_Application_Regression_Tests"
    }

    def 'job parameters exists'() {
        expect:
            node.properties.size() == 1
            node.properties['hudson.model.ParametersDefinitionProperty'].size() == 1
    }

    def 'job parameters, "B" string parameter without default value exists'() {
        expect:
            node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'].size() == 1

            with(node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'][0]) {
                children().size() == 3

                with(children()[0]) {
                    name() == 'hudson.model.StringParameterDefinition'
                    children().size() == 3

                    with(name) {
                        text() == 'B'
                    }

                    with(description) {
                        text() == 'Parent build number'
                    }

                    with(defaultValue) {
                        text() == ''
                    }
                }
            }
    }

    def 'job parameters, "PARENT_BUILD" string parameter with default value "Reference_Application_Build" exists'() {
        expect:
            node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'].size() == 1

            with(node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'][0]) {
                children().size() == 3

                with(children()[1]) {
                    name() == 'hudson.model.StringParameterDefinition'
                    children().size() == 3

                    with(name) {
                        text() == 'PARENT_BUILD'
                    }

                    with(description) {
                        text() == 'Parent build name'
                    }

                    with(defaultValue) {
                        text() == 'Reference_Application_Build'
                    }
                }
            }
    }

    def 'job parameters, "ENVIRONMENT_NAME" string parameter with default value "CI" exists'() {
        expect:
            node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'].size() == 1

            with(node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'][0]) {
                children().size() == 3

                with(children()[2]) {
                    name() == 'hudson.model.StringParameterDefinition'
                    children().size() == 3

                    with(name) {
                        text() == 'ENVIRONMENT_NAME'
                    }

                    with(description) {
                        text() == 'Name of the environment.'
                    }

                    with(defaultValue) {
                        text() == 'CI'
                    }
                }
            }
    }

    def 'workspace_name and project_name env variables injected'() {
        expect:
            node.properties.EnvInjectJobProperty.size() == 1

            with(node.properties.EnvInjectJobProperty) {
                info.size() == 1

                with(info) {
                    propertiesContent.size() == 1

                    with(propertiesContent) {
                        text() == "WORKSPACE_NAME=${workspaceName}\nPROJECT_NAME=${projectName}"
                    }
                }
            }

        where:
            workspaceName = helper.workspaceName
            projectName = helper.projectName
    }

    def 'job assigned to java8 node'() {
        expect:
            node.assignedNode.size() == 1
            node.assignedNode.text() == 'java8'
    }

    def 'wrappers exists'() {
        expect:
            node.buildWrappers.size() == 1
    }

    @Unroll
    def 'wrappers "#name" exists'() {
        expect:
            node.buildWrappers[key].size() == 1

        where:
            name              | key
            'preBuildCleanup' | 'hudson.plugins.ws__cleanup.PreBuildCleanup'
            'injectPasswords' | 'EnvInjectPasswordWrapper'
            'maskPasswords'   | 'com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper'
            'sshAgent'        | 'com.cloudbees.jenkins.plugins.sshagent.SSHAgentBuildWrapper'
    }

    @Unroll
    def 'wrappers sshAgent with "#sshCredentials" value chosen'() {
        expect:
            node.buildWrappers['com.cloudbees.jenkins.plugins.sshagent.SSHAgentBuildWrapper'].size() == 1

            with(node.buildWrappers['com.cloudbees.jenkins.plugins.sshagent.SSHAgentBuildWrapper']) {
                text() == sshCredentials
            }

        where:
            sshCredentials = "adop-jenkins-master"
    }

    def 'scm block with settings exists'() {
        expect:
            node.scm.size() == 1

            with(node.scm) {
                userRemoteConfigs.size() == 1

                with(userRemoteConfigs[0]) {
                    children().size() == 1

                    with(children()[0]) {
                        name() == 'hudson.plugins.git.UserRemoteConfig'
                    }
                }
            }
    }

    def 'scm remote name is not specified'() {
        expect:
            with(node.scm.userRemoteConfigs[0].children()[0]) {
                name.size() == 0
            }
    }

    @Unroll
    def 'scm remote url is "#referenceAppGitUrl"'() {
        expect:
            with(node.scm.userRemoteConfigs[0].children()[0]) {
                url.size() == 1

                with(url) {
                    text() == referenceAppGitUrl
                }
            }

        where:
            referenceAppGitUrl = "ssh://jenkins@gerrit:29418/${helper.projectName}/adop-cartridge-java-regression-tests"
    }

    @Unroll
    def 'scm credentials specified as "#gitCredentials"'() {
        expect:
            with(node.scm.userRemoteConfigs[0].children()[0]) {
                credentialsId.size() == 1

                with(credentialsId) {
                    text() == gitCredentials
                }
            }

        where:
            gitCredentials = "adop-jenkins-master"
    }

    @Unroll
    def 'scm branch is "#branchName"'() {
        expect:
            with(node.scm) {
                branches.size() == 1
                branches['hudson.plugins.git.BranchSpec'].text() == branchName
            }

        where:
            branchName = '*/master'
    }

    @Unroll
    def 'steps with #configBlocksNum configuration blocks exists'() {
        expect:
            node.builders.size() == 1

            with(node.builders[0]) {
                children().size() == configBlocksNum
            }

        where:
            configBlocksNum = 4
    }

    @Unroll
    def 'steps, #shellBlocksNum shell with #commandBlocksNum command blocks exists'() {
        expect:
            node.builders['hudson.tasks.Shell'].size() == shellBlocksNum

            with(node.builders['hudson.tasks.Shell']) {
                command.size() == shellBlocksNum
            }

        where:
            shellBlocksNum = 2
            commandBlocksNum = 2
    }

    def 'step inject environmentVariables exists'() {
        expect:
            node.builders.size() == 1
            node.builders['EnvInjectBuilder'].size() == 1
    }

    @Unroll
    def 'step inject environmentVariables properties file path is #filePath'() {
        expect:
            with(node.builders['EnvInjectBuilder']) {
                info.size() == 1

                with(info) {
                    propertiesFilePath.size() == 1

                    with(propertiesFilePath) {
                        text() == filePath
                    }
                }
            }

        where:
            filePath = 'env.properties'
    }

    def 'step Maven configuration block exists'() {
        expect:
            node.builders.size() == 1
            node.builders['hudson.tasks.Maven'].size() == 1
    }

    @Unroll
    def 'step Maven target goal is "#goal"'() {
        expect:
            with(node.builders['hudson.tasks.Maven']) {
                targets.size() == 1
                targets.text() == goal
            }

        where:
            goal = 'clean -B test -DPETCLINIC_URL=${APP_URL} -DZAP_IP=${ZAP_IP} -DZAP_PORT=${ZAP_PORT} -DZAP_ENABLED=${ZAP_ENABLED}'
    }

    @Unroll
    def 'step Maven installation is "#installation" chosen'() {
        expect:
            with(node.builders['hudson.tasks.Maven']) {
                mavenName.size() == 1
                mavenName.text() == installation
            }

        where:
            installation = "ADOP Maven"
    }

    def 'pipeline automatic trigger exists'() {
        expect:
            node.publishers.size() == 1
            node.publishers['hudson.plugins.parameterizedtrigger.BuildTrigger'].size() == 1
    }

    @Unroll
    def 'downstream parameterized trigger on "#triggerJobName" job exists'() {
        expect:
            with(node.publishers['hudson.plugins.parameterizedtrigger.BuildTrigger']['configs']['hudson.plugins.parameterizedtrigger.BuildTriggerConfig']) {
                projects.size() == 1

                with(projects) {
                    text() == triggerJobName
                }
            }

        where:
            triggerJobName = "${helper.projectName}/Reference_Application_Performance_Tests"
    }

    @Unroll
    def 'downstream parameterized trigger condition is "#triggerCondition"'() {
        expect:
            with(node.publishers['hudson.plugins.parameterizedtrigger.BuildTrigger']['configs']['hudson.plugins.parameterizedtrigger.BuildTriggerConfig']) {
                condition.size() == 1

                with(condition) {
                    text() == triggerCondition
                }
            }

        where:
            triggerCondition = "UNSTABLE_OR_BETTER"
    }

    @Unroll
    def 'downstream parameterized trigger with predefined parameter "#key=#value" exists'() {
        expect:
            with(node.publishers['hudson.plugins.parameterizedtrigger.BuildTrigger']['configs']['hudson.plugins.parameterizedtrigger.BuildTriggerConfig']) {
                triggerWithNoParameters.size() == 1
                triggerWithNoParameters.text() == 'false'

                configs['hudson.plugins.parameterizedtrigger.PredefinedBuildParameters'].size() == 1

                with(configs['hudson.plugins.parameterizedtrigger.PredefinedBuildParameters']) {
                    properties.text().contains("${key}=${value}")
                }
            }

        where:
            key            | value
            'B'            | '${B}'
            'PARENT_BUILD' | '${PARENT_BUILD}'
    }

    def 'post build "Publish cucumber results as a report" with default settings exists'() {
        expect:
            node.publishers['net.masterthought.jenkins.CucumberReportPublisher'].size() == 1

            with(node.publishers['net.masterthought.jenkins.CucumberReportPublisher'][0]) {
                attributes()['plugin'] == 'cucumber-reports@0.1.0'

                jsonReportDirectory.size() == 1
                jsonReportDirectory.text() == ""

                pluginUrlPath.size() == 1
                pluginUrlPath.text() == ""

                fileIncludePattern.size() == 1
                fileIncludePattern.text() == ""

                fileExcludePattern.size() == 1
                fileExcludePattern.text() == ""

                skippedFails.size() == 1
                skippedFails.text() == "false"

                pendingFails.size() == 1
                pendingFails.text() == "false"

                undefinedFails.size() == 1
                undefinedFails.text() == "false"

                missingFails.size() == 1
                missingFails.text() == "false"

                noFlashCharts.size() == 1
                noFlashCharts.text() == "false"

                ignoreFailedTests.size() == 1
                ignoreFailedTests.text() == "false"

                parallelTesting.size() == 1
                parallelTesting.text() == "false"
            }
    }
}
