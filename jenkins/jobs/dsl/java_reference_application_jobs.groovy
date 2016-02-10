// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def referenceAppgitRepo = "spring-petclinic"
def regressionTestGitRepo = "adop-cartridge-java-regression-tests"
def referenceAppGitUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + referenceAppgitRepo
def regressionTestGitUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + regressionTestGitRepo

// Jobs
def buildAppJob = freeStyleJob(projectFolderName + "/Reference_Application_Build")
def unitTestJob = freeStyleJob(projectFolderName + "/Reference_Application_Unit_Tests")
def codeAnalysisJob = freeStyleJob(projectFolderName + "/Reference_Application_Code_Analysis")
def deployJob = freeStyleJob(projectFolderName + "/Reference_Application_Deploy")
def regressionTestJob = freeStyleJob(projectFolderName + "/Reference_Application_Regression_Tests")

// Views
def pipelineView = buildPipelineView(projectFolderName + "/Java_Reference_Application")

pipelineView.with{
    title('Reference Application Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + "/Reference_Application_Build")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}

buildAppJob.with{
  description("This job builds Java Spring reference application")
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  scm{
    git{
      remote{
        url(referenceAppGitUrl)
        credentials("adop-jenkins-master")
      }
      branch("*/master")
    }
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("java8")
  triggers{
    gerrit{
      events{
        refUpdated()
      }
      configure { gerritxml ->
        gerritxml / 'gerritProjects' {
          'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject' {
            compareType("PLAIN")
            pattern(projectFolderName + "/" + referenceAppgitRepo)
            'branches' {
              'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.Branch' {
                compareType("PLAIN")
                pattern("master")
              }
            }
          }
        }
        gerritxml / serverName("ADOP Gerrit")
      }
    }
  }
  steps {
    maven{
      goals('clean install -DskipTests')
      mavenInstallation("ADOP Maven")
    }
  }
  publishers{
    archiveArtifacts("**/*")
    downstreamParameterized{
      trigger(projectFolderName + "/Reference_Application_Unit_Tests"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${BUILD_NUMBER}')
          predefinedProp("PARENT_BUILD", '${JOB_NAME}')
        }
      }
    }
  }
}

unitTestJob.with{
  description("This job runs unit tests on Java Spring reference application.")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("java8")
  steps {
  }
  steps {
    copyArtifacts("Reference_Application_Build") {
        buildSelector {
          buildNumber('${B}')
      }
    }
    maven{
      goals('clean test')
      mavenInstallation("ADOP Maven")
    }
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Reference_Application_Code_Analysis"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD",'${PARENT_BUILD}')
        }
      }
    }
  }
}

codeAnalysisJob.with{
  description("This job runs code quality analysis for Java reference application using SonarQube.")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  label("java8")
  steps {
    copyArtifacts('Reference_Application_Build') {
        buildSelector {
          buildNumber('${B}')
      }
    }
  }
  configure { myProject ->
    myProject / builders << 'hudson.plugins.sonar.SonarRunnerBuilder'(plugin:"sonar@2.2.1"){
      project('sonar-project.properties')
      properties('''sonar.projectKey=org.java.reference-application
sonar.projectName=Reference application
sonar.projectVersion=1.0.0
sonar.sources=src
sonar.language=java
sonar.sourceEncoding=UTF-8
sonar.scm.enabled=false''')
      javaOpts()
      jdk('(Inherit From Job)')
      task()
    }
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Reference_Application_Deploy"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD", '${PARENT_BUILD}')
        }
      }
    }
  }
}

deployJob.with{
  description("This job deploys the java reference application to the CI environment")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
    stringParam("ENVIRONMENT_NAME","CI","Name of the environment.")
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("docker")
  steps {
    copyArtifacts("Reference_Application_Build") {
        buildSelector {
          buildNumber('${B}')
      }
    }
    shell('''set +x
            |export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
            |docker cp ${WORKSPACE}/target/petclinic.war  ${SERVICE_NAME}:/usr/local/tomcat/webapps/
            |docker restart ${SERVICE_NAME}
            |COUNT=1
            |while ! curl -q http://${SERVICE_NAME}:8080/petclinic -o /dev/null 
            |do
            |  if [ ${COUNT} -gt 10 ]; then
            |      echo "Docker build failed even after ${COUNT}. Please investigate."
            |      exit 1
            |  fi
            |  echo "Application is not up yet. Retrying ..Attempt (${COUNT})"
            |  sleep 5  
            |  COUNT=$((COUNT+1))
            |done
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "Environment URL (replace PUBLIC_IP with your public ip address where you access jenkins from) : http://${SERVICE_NAME}.PUBLIC_IP.xip.io/petclinic"
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |set -x'''.stripMargin())
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Reference_Application_Regression_Tests"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD", '${PARENT_BUILD}')
          predefinedProp("ENVIRONMENT_NAME", '${ENVIRONMENT_NAME}')
        }
      }
    }
  }
}

regressionTestJob.with{
  description("This job runs regression tests on deployed java application")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
    stringParam("ENVIRONMENT_NAME","CI","Name of the environment.")
  }
  scm{
    git{
      remote{
        url(regressionTestGitUrl)
        credentials("adop-jenkins-master")
      }
      branch("*/master")
    }
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("java8")
  steps {
    shell('''set +x
            |export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
            |echo "SERVICE_NAME=${SERVICE_NAME}" > env.properties
            |set -x'''.stripMargin())
    environmentVariables {
      propertiesFile('env.properties')
    }
    maven{
      goals('clean test -B -DPETCLINIC_URL=http://${SERVICE_NAME}:8080/petclinic')
      mavenInstallation("ADOP Maven")
    }
  }
  configure{myProject ->
    myProject / 'publishers' << 'net.masterthought.jenkins.CucumberReportPublisher'(plugin:'cucumber-reports@0.1.0'){
      jsonReportDirectory("")
      pluginUrlPath("")
      fileIncludePattern("")
      fileExcludePattern("")
      skippedFails("false")
      pendingFails("false")
      undefinedFails("false")
      missingFails("false")
      noFlashCharts("false")
      ignoreFailedTests("false")
      parallelTesting("false")
    }
  }
}

