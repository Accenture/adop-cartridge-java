package com.java.cartridge

import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that JavaReferenceApplication/ReferenceApplication_Build job works as expected.
 */
class BuildReferenceApplicationJobSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/java_reference_application_jobs.groovy')

    @Shared
    def Node node = new XmlParser().parseText(helper.jm.savedConfigs["${helper.projectName}/Reference_Application_Build"])

    def 'Reference_Application_Build job is exists'() {
        expect:
            helper.jm.savedConfigs[jobName] != null

        where:
            jobName = "${helper.projectName}/Reference_Application_Build"
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

    def 'steps with one Maven block exists'() {
        expect:
            node.builders.size() == 1

            with(node.builders[0]) {
                children().size() == 1

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

    def 'gerrit scm trigger exists'() {
        expect:
            node.triggers.size() == 1
            node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger'].size() == 1
    }

    def 'gerrit scm trigger event on refUpdated exists'() {
        expect:
            node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['triggerOnEvents'].size() == 1
            with(node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['triggerOnEvents'][0]) {
                children().size() == 1

                with(children()[0]) {
                    name() == 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginRefUpdatedEvent'
                }
            }
    }

    def 'gerrit scm trigger project configuration exits'() {
        expect:
            node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['gerritProjects'].size() == 1

            with(node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['gerritProjects'][0]) {
                children().size() == 1

                with(children()[0]) {
                    name() == 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'
                }
            }
    }

    @Unroll
    def 'gerrit scm trigger project pattern "#projectPattern" value and "#projectCompareType" type'() {
        expect:
            node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['gerritProjects']['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'].size() == 1

            with(node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['gerritProjects']['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject']) {
                compareType.size() == 1
                with(compareType) {
                    text() == projectCompareType
                }

                pattern.size() == 1
                with(pattern) {
                    text() == projectPattern
                }
            }

        where:
            projectPattern = "${helper.projectName}/spring-petclinic"
            projectCompareType = 'PLAIN'
    }

    def 'gerrit scm trigger one branch configuration exits'() {
        expect:
            node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['gerritProjects']['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'].size() == 1

            with(node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['gerritProjects']['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject']) {
                branches.size() == 1

                with(branches[0]) {
                    children().size() == 1

                    with(children()[0]) {
                        name() == 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.Branch'
                    }
                }
            }
    }

    @Unroll
    def 'gerrit scm trigger with "#branchName" branch value and compare "#branchCompareType" type'() {
        expect:
            with(node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['gerritProjects']['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject']) {
                branches.size() == 1

                with(branches['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.Branch']) {
                    compareType.size() == 1
                    with(compareType) {
                        text() == branchCompareType
                    }

                    pattern.size() == 1
                    with(pattern) {
                        text() == branchName
                    }
                }
            }

        where:
            branchName = 'master'
            serverName = 'ADOP Gerrit'
            branchCompareType = 'PLAIN'
    }

    @Unroll
    def 'gerrit scm trigger with "#gerritServerName" server chosen'() {
        expect:
            with(node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']) {
                serverName.size() == 1

                with(serverName) {
                    text() == gerritServerName
                }
            }

        where:
            gerritServerName = 'ADOP Gerrit'
    }

    @Unroll
    def 'scm branch configuration on "#scmBranchName" name should be the same for scm trigger'() {
        expect:
            node.triggers['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger']['gerritProjects']['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject']['branches']['com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.Branch']['pattern'].text().contains(scmBranchName)
            node.scm.branches['hudson.plugins.git.BranchSpec'].text().contains(scmBranchName)

        where:
            scmBranchName = 'master'
    }

    def 'archive artifacts post-build action exists'() {
        expect:
            node.publishers['hudson.tasks.ArtifactArchiver'].size() == 1
            with(node.publishers['hudson.tasks.ArtifactArchiver']) {
                artifacts.size() == 1
            }
    }

    @Unroll
    def 'archive artifacts, files to archive is "#filesToArchive"'() {
        expect:
            with(node.publishers['hudson.tasks.ArtifactArchiver']) {
                artifacts.text() == filesToArchive
            }

        where:
            filesToArchive = '**/*'
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
            triggerJobName = "${helper.projectName}/Reference_Application_Unit_Tests"
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
            'B'            | '${BUILD_NUMBER}'
            'PARENT_BUILD' | '${JOB_NAME}'
    }
}