package com.java.cartridge

import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that JavaReferenceApplication/Reference_Application_Code_Analysis job works as expected.
 */
class CodeAnalysisReferenceApplicationJobSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/java_reference_application_jobs.groovy')

    @Shared
    def Node node = new XmlParser().parseText(helper.jm.savedConfigs["${helper.projectName}/Reference_Application_Code_Analysis"])

    def 'Reference_Application_Code_Analysis job is exists'() {
        expect:
            helper.jm.savedConfigs[jobName] != null

        where:
            jobName = "${helper.projectName}/Reference_Application_Code_Analysis"
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
    
    def 'job parameters, "UTB" string parameter without default value exists'() {
        expect:
            node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'].size() == 1

            with(node.properties['hudson.model.ParametersDefinitionProperty']['parameterDefinitions'][0]) {
                children().size() == 3

                with(children()[2]) {
                    name() == 'hudson.model.StringParameterDefinition'
                    children().size() == 3

                    with (name) {
                        text() == 'UTB'
                    }

                    with (description) {
                        text() == 'Unit Tests job build number'
                    }

                    with (defaultValue) {
                        text() == ''
                    }
                }
            }
    }

    def 'workspace_name, project_name and project_name_key env variables injected'() {
        expect:
            node.properties.EnvInjectJobProperty.size() == 1

            with(node.properties.EnvInjectJobProperty) {
                info.size() == 1

                with(info) {
                    propertiesContent.size() == 1

                    with(propertiesContent) {
                        text() == "WORKSPACE_NAME=${workspaceName}\nPROJECT_NAME=${projectName}\nPROJECT_NAME_KEY=${projectNameKey}"
                    }
                }
            }

        where:
            workspaceName = helper.workspaceName
            projectName = helper.projectName
            projectNameKey = helper.projectName.toLowerCase().replace("/", "-")
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

    def 'step Copy Artifacts configuration block exists'() {
        expect:
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
            buildNumberRef = '${UTB}'
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
            jenkinsJobName = 'Reference_Application_Unit_Tests'
    }

    def 'step SonarQube configuration block exists'() {
        expect:
            node.builders['hudson.plugins.sonar.SonarRunnerBuilder'].size() == 1
            node.builders['hudson.plugins.sonar.SonarRunnerBuilder'][0].attributes()['plugin'] == 'sonar@2.2.1'
    }

    def 'SonarQube project use sonar-project.properties'() {
        expect:
            with(node.builders['hudson.plugins.sonar.SonarRunnerBuilder']) {
                project.size() == 1

                with(project) {
                    text() == 'sonar-project.properties'
                }
            }
    }

    def 'SonarQube properties content check'() {
        expect:
            with(node.builders['hudson.plugins.sonar.SonarRunnerBuilder']) {
                properties.size() == 1

                with(properties) {
                    text() == 'sonar.projectKey=${PROJECT_NAME_KEY}' + "\n" +
                        'sonar.projectName=${PROJECT_NAME}' + "\n" +
                        'sonar.projectVersion=1.0.${B}' + "\n" +
                        "sonar.sources=src/main/java\n" +
                        "sonar.language=java\n" +
                        "sonar.sourceEncoding=UTF-8\n" +
                        "sonar.scm.enabled=false"
                }
            }
    }

    def 'SonarQube jdk, javaOpts and task settings check'() {
        expect:
            println node.builders['hudson.plugins.sonar.SonarRunnerBuilder']
            with(node.builders['hudson.plugins.sonar.SonarRunnerBuilder']) {
                task.size() == 1
                javaOpts.size() == 1

                jdk.size() == 1
                with(jdk) {
                    text() == '(Inherit From Job)'
                }
            }
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
            triggerJobName = "${helper.projectName}/Reference_Application_Deploy"
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