package com.java.cartridge

import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that JavaReferenceApplication/Reference_Application_Deploy job works as expected.
 */
class DeployReferenceApplicationJobSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/java_reference_application_jobs.groovy')

    @Shared
    def Node node = new XmlParser().parseText(helper.jm.savedConfigs["${helper.projectName}/Reference_Application_Deploy"])

    def 'Reference_Application_Deploy job is exists'() {
        expect:
            helper.jm.savedConfigs[jobName] != null

        where:
            jobName = "${helper.projectName}/Reference_Application_Deploy"
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

                    with (name) {
                        text() == 'B'
                    }

                    with (description) {
                        text() == 'Parent build number'
                    }

                    with (defaultValue) {
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

                    with (name) {
                        text() == 'PARENT_BUILD'
                    }

                    with (description) {
                        text() == 'Parent build name'
                    }

                    with (defaultValue) {
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

                    with (name) {
                        text() == 'ENVIRONMENT_NAME'
                    }

                    with (description) {
                        text() == 'Name of the environment.'
                    }

                    with (defaultValue) {
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

    def 'job assigned to docker node'() {
        expect:
            node.assignedNode.size() == 1
            node.assignedNode.text() == 'docker'
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

    def 'scm block not exists'() {
        expect:
            node.scm.size() == 1
            node.scm[0].attributes()['class'] == 'hudson.scm.NullSCM'
    }

    def 'steps with two configuration blocks exists'() {
        expect:
            node.builders.size() == 1

            with(node.builders[0]) {
                children().size() == 2
            }
    }

    def 'step shell with command block exists'() {
        expect:
            node.builders['hudson.tasks.Shell'].size() == 1

            with(node.builders['hudson.tasks.Shell']) {
                command.size() == 1
            }
    }

    def 'step Copy Artifacts configuration block exists'() {
        expect:
            node.builders.size() == 1
            node.builders['hudson.plugins.copyartifact.CopyArtifact'].size() == 1
    }

    @Unroll
    def 'Copy Artifacts build selector specified on "#buildNumberRef" build number'() {
        expect:
            with(node.builders['hudson.plugins.copyartifact.CopyArtifact']) {
                selector.size() == 1
                selector[0].attributes()['class'] == 'hudson.plugins.copyartifact.SpecificBuildSelector'

                with(selector) {
                    buildNumber.size() == 1

                    with(buildNumber) {
                        text() == buildNumberRef
                    }
                }
            }

        where:
            buildNumberRef = '${B}'
    }

    @Unroll
    def 'Copy Artifacts project specified on "#jenkinsJobName" jenkins job'() {
        expect:
            with(node.builders['hudson.plugins.copyartifact.CopyArtifact']) {
                project.size() == 1

                with(project) {
                    text() == jenkinsJobName
                }
            }

        where:
            jenkinsJobName = 'Reference_Application_Build'
    }

    @Unroll
    def 'Copy Artifacts include pattern specified only on "#includePattern" file'() {
        expect:
            with(node.builders['hudson.plugins.copyartifact.CopyArtifact']) {
                filter.size() == 1

                with(filter) {
                    text() == includePattern
                }
            }

        where:
            includePattern = 'target/petclinic.war'
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
            triggerJobName = "${helper.projectName}/Reference_Application_Regression_Tests"
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
}