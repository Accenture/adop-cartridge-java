package com.java.cartridge

import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that JavaReferenceApplication/OwaspDependencyCheck job works as expected.
 */
class OwaspDependencyCheckJobSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/java_reference_application_jobs.groovy')

    @Shared
    def Node node = new XmlParser().parseText(helper.jm.savedConfigs["${helper.projectName}/OWASP_Dependency_Check"])

    def 'OWASP_Dependency_Check job is exists'() {
        expect:
            helper.jm.savedConfigs[jobName] != null

        where:
            jobName = "${helper.projectName}/OWASP_Dependency_Check"
    }

    def 'job parameters not exists'() {
        expect:
            node.properties.size() == 1
            node.properties['hudson.model.ParametersDefinitionProperty'].size() == 0
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

    def 'steps with two Maven blocks exists'() {
        expect:
            node.builders.size() == 1

            with(node.builders[0]) {
                children().size() == 2

                with(children()[0]) {
                    name() == 'hudson.tasks.Maven'
                }
            }
    }

    @Unroll
    def 'step Maven target goal is "#goal"'() {
        expect:
            with(node.builders['hudson.tasks.Maven']) {
                targets.size() == 1
                targets.text() == goal
            }

        where:
            goal = "clean install -DskipTests"
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
            referenceAppGitUrl = "ssh://jenkins@gerrit:29418/${helper.projectName}/spring-petclinic"
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

}
