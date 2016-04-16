package com.java.cartridge

import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that EnvironmentProvisioning/DestroyEnvironment job works as expected.
 */
class DestroyEnvironmentJobSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/environment_provisioning.groovy')

    @Shared
    def Node node = new XmlParser().parseText(helper.jm.savedConfigs["${helper.projectName}/Destroy_Environment"])

    def 'Destroy_Environment job is exists'() {
        expect:
            helper.jm.savedConfigs[jobName] != null

        where:
            jobName = "${helper.projectName}/Destroy_Environment"
    }

    def 'job parameters exists'() {
        expect:
            node.properties.size() == 1
            node.properties['hudson.model.ParametersDefinitionProperty'].size() == 1
    }

    @Unroll
    def '"#parameterName" choice parameter exists'() {
        expect:
            node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'].size() == 1

            with(node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'][0]) {
                children().size() == 1

                with(children()[0]) {
                    name() == 'hudson.model.ChoiceParameterDefinition'
                    children().size() == 3

                    with(name) {
                        text() == parameterName
                    }

                    with(description) {
                        text() == 'Destroy Environment for development(named: CI) or production (named: PRODA and PRODB)'
                    }
                }
            }

        where:
            parameterName = 'ENVIRONMENT_TYPE'
    }

    @Unroll
    def '"ENVIRONMENT_TYPE" choice parameter contains "#choiceValue" value'() {
        expect:
            with(node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions']['hudson.model.ChoiceParameterDefinition'][0]) {
                with(choices) {
                    with(value()['a'][0]) {
                        with(children().getAt(key)) {
                            text() == choiceValue
                        }
                    }
                }
            }

        where:
            key | choiceValue
            0   | 'DEV'
            1   | 'PROD'
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

    def 'job assigned to Docker node'() {
        expect:
            node.assignedNode.size() == 1
            node.assignedNode.text() == 'docker'
    }

    def 'wrappers exists'() {
        expect:
            node.buildWrappers.size() == 1
    }

    @Unroll
    def 'wrappers #name exists'() {
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
    def 'wrappers sshAgent with #sshCredentials value chosen'() {
        expect:
            node.buildWrappers['com.cloudbees.jenkins.plugins.sshagent.SSHAgentBuildWrapper'].size() == 1

            with(node.buildWrappers['com.cloudbees.jenkins.plugins.sshagent.SSHAgentBuildWrapper']) {
                text() == sshCredentials
            }

        where:
            sshCredentials = "adop-jenkins-master"
    }

    def 'steps with one shell block exists'() {
        expect:
            node.builders.size() == 1

            with(node.builders[0]) {
                children().size() == 1

                with(children()[0]) {
                    name() == 'hudson.tasks.Shell'
                }
            }
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

    @Unroll
    def 'scm remote name is #remoteName'() {
        expect:
            with(node.scm.userRemoteConfigs[0].children()[0]) {
                name.size() == 1

                with(name) {
                    text() == remoteName
                }
            }

        where:
            remoteName = 'origin'
    }

    @Unroll
    def 'scm remote url is #environmentTemplateGitUrl'() {
        expect:
            with(node.scm.userRemoteConfigs[0].children()[0]) {
                url.size() == 1

                with(url) {
                    text() == environmentTemplateGitUrl
                }
            }

        where:
            environmentTemplateGitUrl = "ssh://jenkins@gerrit:29418/${helper.projectName}/adop-cartridge-java-environment-template"
    }

    @Unroll
    def 'scm credentials specified as #gitCredentials'() {
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
    def 'scm branch is #branchName'() {
        expect:
            with(node.scm) {
                branches.size() == 1
                branches['hudson.plugins.git.BranchSpec'].text() == branchName
            }

        where:
            branchName = '*/master'
    }

    def 'pipeline trigger should not exists'() {
        expect:
            node.publishers.size() == 1
            node.publishers[0].value().size() == 0
    }
}