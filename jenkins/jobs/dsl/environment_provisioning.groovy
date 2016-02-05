// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def environmentTemplateGitUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/adop-cartridge-java-environment-template"

// Jobs
def environmentProvisioningPipelineView = buildPipelineView(projectFolderName + "/Environment_Provisioning")
def createEnvironmentJob = freeStyleJob(projectFolderName + "/Create_Environment")
def destroyEnvironmentJob = freeStyleJob(projectFolderName + "/Destroy_Environment")
def listEnvironmentJob = freeStyleJob(projectFolderName + "/List_Environment")

// Create Environment
createEnvironmentJob.with{
    description('''This job creates the environment to deploy the java application.
Note : If you running this job for the first time then please keep the environment name to default value. 
The reference application deploy job is expecting the default environment to be available.''')
    parameters{
        stringParam("ENVIRONMENT_NAME",'CI',"Name of the environment to be created.")
    }
    label("docker")
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
    steps {
        shell('''set +x
                |export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
                |docker-compose -p ${SERVICE_NAME} up -d
                |## Add nginx configuration
                |sed -i "s/###TOMCAT_SERVICE_NAME###/${SERVICE_NAME}/" tomcat.conf
                |docker cp tomcat.conf proxy:/etc/nginx/sites-enabled/${SERVICE_NAME}.conf
                |## Reload nginx
                |docker exec proxy /usr/sbin/nginx -s reload
                |set -x'''.stripMargin())
    }
    scm {
        git {
            remote {
                name("origin")
                url("${environmentTemplateGitUrl}")
                credentials("adop-jenkins-master")
            }
            branch("*/master")
        }
    }
    publishers {
        buildPipelineTrigger("${PROJECT_NAME}/Destroy_Environment") {
            parameters {
                currentBuild()
            }
        }
    }
}

// Destroy Environment
destroyEnvironmentJob.with{
    description("This job deletes the environment.")
    parameters{
        stringParam("ENVIRONMENT_NAME",'CI',"Name of the environment to be created.")
    }
    label("docker")
    environmentVariables {
        env('WORKSPACE_NAME',workspaceFolderName)
        env('PROJECT_NAME',projectFolderName)
    }
    scm {
        git {
            remote {
                name("origin")
                url("${environmentTemplateGitUrl}")
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
    steps {
        shell('''set +x
            |export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
            |docker-compose -p ${SERVICE_NAME} stop
            |docker-compose -p ${SERVICE_NAME} rm -f
            |## Deleted nginx configuration
            |docker exec proxy rm -f /etc/nginx/sites-enabled/${SERVICE_NAME}.conf
            |## Reload nginx
            |docker exec proxy /usr/sbin/nginx -s reload
            |set -x'''.stripMargin())
    }
}

// Pipeline
environmentProvisioningPipelineView.with{
    title('Environment Provisioning Pipeline')
    displayedBuilds(5)
    selectedJob("Create_Environment")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}


// Create Environment
listEnvironmentJob.with{
    description("This job list the running environments for project")
    label("docker")
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
    steps {
        shell('''set +x
                |docker ps --filter status=running --filter "label=PROJECT_NAME=${PROJECT_NAME}"
                |echo "=.=.=.=.=.=.=.=.=.=.=.=."
                |echo "=.=.=.=.=.=.=.=.=.=.=.=."
                |echo "List of running environments -" 
                |docker ps --filter status=running --filter "label=PROJECT_NAME=${PROJECT_NAME}" --format "\t{{.Names}}"
                |echo "=.=.=.=.=.=.=.=.=.=.=.=."
                |echo "=.=.=.=.=.=.=.=.=.=.=.=."
                |set -x'''.stripMargin())
    }
}
