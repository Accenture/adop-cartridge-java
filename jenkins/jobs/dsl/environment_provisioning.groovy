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
    parameters {
        choiceParam('ENVIRONMENT_TYPE', ['DEV', 'PROD'], 'Create Environment for development(named: CI) or production (named: PRODA and PRODB)')
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
                |function createDockerContainer() {
                |	echo $1, $2
                |    export ENVIRONMENT_NAME=$1
                |    export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
                |    docker-compose -p ${SERVICE_NAME} up -d
                |    ## Add nginx configuration
                |    sed -i "s/###TOMCAT_SERVICE_NAME###/${SERVICE_NAME}/" $2
                |    docker cp $2 proxy:/etc/nginx/sites-enabled/${SERVICE_NAME}.conf
                |}
                |
                |if [ "$ENVIRONMENT_TYPE" == "DEV" ]; then
                | 	createDockerContainer "CI" tomcat.conf
                |elif [ "$ENVIRONMENT_TYPE" == "PROD" ]; then
                |	##Creating 2 environment PRODA and PRODB, with a upstream ngix configuration in prod-tomcat.conf
                |    mv tomcat.conf tomcatA.conf &&cp tomcatA.conf tomcatB.conf
                |    createDockerContainer "PRODA" "tomcatA.conf"
                |    createDockerContainer "PRODB" "tomcatB.conf"
                |
                |	SERVICE_NAME_PRODA="$(echo ${PROJECT_NAME} | tr '/' '_')_PRODA"
                |    SERVICE_NAME_PRODB="$(echo ${PROJECT_NAME} | tr '/' '_')_PRODB"
                |    TOMCAT_1_IP=$( docker inspect --format '{{ .NetworkSettings.Networks.'"$DOCKER_NETWORK_NAME"'.IPAddress }}' ${SERVICE_NAME_PRODA} )
                |    TOMCAT_2_IP=$( docker inspect --format '{{ .NetworkSettings.Networks.'"$DOCKER_NETWORK_NAME"'.IPAddress }}' ${SERVICE_NAME_PRODB} )
                |    PROJECT_KEY_PROD="$(echo ${PROJECT_NAME} | tr '/' '_')_PROD"
                |    TOKEN_UPSTREAM_NAME="###TOKEN_UPSTREAM_NAME###"
                |    TOKEN_NAMESPACE="###TOKEN_NAMESPACE###"
                |    TOKEN_TOMCAT_1_IP="###TOKEN_TOMCAT_1_IP###"
                |    TOKEN_TOMCAT_1_PORT="###TOKEN_TOMCAT_1_PORT###"
                |    TOKEN_TOMCAT_2_IP="###TOKEN_TOMCAT_2_IP###"
                |    TOKEN_TOMCAT_2_PORT="###TOKEN_TOMCAT_2_PORT###"
                |
                |    sed -i "s/${TOKEN_UPSTREAM_NAME}/${PROJECT_KEY_PROD}/g" prod-tomcat.conf
                |    sed -i "s/${TOKEN_NAMESPACE}/${PROJECT_KEY_PROD}/g" prod-tomcat.conf
                |    sed -i "s/${TOKEN_TOMCAT_1_IP}/${TOMCAT_1_IP}/g" prod-tomcat.conf
                |    sed -i "s/${TOKEN_TOMCAT_1_PORT}/8080/g" prod-tomcat.conf
                |    sed -i "s/${TOKEN_TOMCAT_2_IP}/${TOMCAT_2_IP}/g" prod-tomcat.conf
                |    sed -i "s/${TOKEN_TOMCAT_2_PORT}/8080/g" prod-tomcat.conf
                |    docker cp prod-tomcat.conf proxy:/etc/nginx/sites-enabled/${PROJECT_KEY_PROD}.conf
                |fi
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
queue(createEnvironmentJob)
// Destroy Environment
destroyEnvironmentJob.with{
    description("This job deletes the environment.")
    parameters {
        choiceParam('ENVIRONMENT_TYPE', ['DEV', 'PROD'], 'Destroy Environment for development(named: CI) or production (named: PRODA and PRODB)')
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
                |function deleteDockerContainer() {
                |	echo $1, $2
                |    export ENVIRONMENT_NAME=$1
                |	export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
                |    docker-compose -p ${SERVICE_NAME} stop
                |    docker-compose -p ${SERVICE_NAME} rm -f
                |    ## Deleted nginx configuration
                |    docker exec proxy rm -f /etc/nginx/sites-enabled/${SERVICE_NAME}.conf
                |}
                |
                |if [ "$ENVIRONMENT_TYPE" == "DEV" ]; then
                |	deleteDockerContainer "CI" tomcat.conf
                |elif [ "$ENVIRONMENT_TYPE" == "PROD" ]; then
                |	mv tomcat.conf tomcatA.conf &&cp tomcatA.conf tomcatB.conf
                |    deleteDockerContainer "PRODA" "tomcatA.conf"
                |    deleteDockerContainer "PRODB" "tomcatB.conf"
                |    PROJECT_KEY_PROD="$(echo ${PROJECT_NAME} | tr '/' '_')_PROD"
                |    docker exec proxy rm -f /etc/nginx/sites-enabled/${PROJECT_KEY_PROD}.conf
                |fi
                |
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
