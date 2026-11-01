// Declarative CI/CD pipeline for the Recipe Management System.
//
//   Checkout -> Build & Package -> Run Unit Tests -> Selenium Test
//            -> Docker Build -> Docker Push -> Deploy
//
// Runs on Linux (sh) and Windows (bat/powershell) agents. Any failing stage stops the
// pipeline, so nothing is pushed or deployed unless every test passed.

def run(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
    }

    parameters {
        string(name: 'APP_PORT', defaultValue: '8085', description: 'Host port the deployed application listens on')
        choice(name: 'DEPLOY_MODE', choices: ['docker', 'jar', 'none'],
               description: 'docker = run the new image as a container; jar = run the JAR as a process on this agent; none = skip')
        booleanParam(name: 'RUN_SELENIUM', defaultValue: true, description: 'Run the Selenium end-to-end stage (needs Chrome on the agent)')
        booleanParam(name: 'PUSH_IMAGE', defaultValue: false, description: 'Push the image to DOCKER_REGISTRY')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'docker.io/your-dockerhub-user',
               description: 'Registry/namespace, e.g. docker.io/<user> or localhost:5000')
        string(name: 'DOCKER_CREDENTIALS_ID', defaultValue: 'dockerhub-credentials',
               description: 'Jenkins username/password credential for the registry (ignored for localhost registries)')
    }

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME       = 'recipe-management-system'
        CONTAINER_NAME = 'recipe-app'
        IMAGE_LOCAL    = "recipe-management-system:${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = isUnix()
                            ? sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                            : bat(script: '@git rev-parse --short HEAD', returnStdout: true).trim()
                    String registry = params.DOCKER_REGISTRY.replaceAll('/+$', '')
                    env.IMAGE_REMOTE = "${registry}/${env.APP_NAME}:${env.BUILD_NUMBER}"
                    env.IMAGE_REMOTE_LATEST = "${registry}/${env.APP_NAME}:latest"
                    // docker login needs a host only for non-Docker-Hub registries (contains '.' or ':' or is localhost)
                    String first = registry.tokenize('/')[0]
                    env.REGISTRY_HOST = (first.contains('.') || first.contains(':') || first == 'localhost') ? first : ''
                }
                echo "Building ${env.APP_NAME} commit ${env.GIT_SHORT} (build #${env.BUILD_NUMBER})"
            }
        }

        stage('Build & Package') {
            steps {
                run 'mvn -B clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Run Unit Tests') {
            steps {
                run 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Selenium Test') {
            when {
                expression { params.RUN_SELENIUM }
            }
            steps {
                // Starts the app on a random port and drives headless Chrome through the user journeys.
                run 'mvn -B test -Pselenium'
            }
            post {
                always {
                    // Unit-test XML from the previous stage is already published; pick up only the browser tests.
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-com.recipes.rms.selenium.*.xml'
                    archiveArtifacts artifacts: 'target/screenshots/**', allowEmptyArchive: true
                }
            }
        }

        stage('Docker Build') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' && params.DEPLOY_MODE != 'jar' }
            }
            steps {
                run "docker build --pull --build-arg VERSION=${env.BUILD_NUMBER} --build-arg GIT_COMMIT=${env.GIT_SHORT} -t ${env.IMAGE_LOCAL} -t ${env.APP_NAME}:latest ."
                run "docker image inspect ${env.IMAGE_LOCAL} --format \"Built {{.Id}} ({{.Size}} bytes)\""
            }
        }

        stage('Docker Push') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' && params.PUSH_IMAGE && params.DEPLOY_MODE != 'jar' }
            }
            steps {
                script {
                    run "docker tag ${env.IMAGE_LOCAL} ${env.IMAGE_REMOTE}"
                    run "docker tag ${env.IMAGE_LOCAL} ${env.IMAGE_REMOTE_LATEST}"
                    if (env.REGISTRY_HOST.startsWith('localhost') || env.REGISTRY_HOST.startsWith('127.0.0.1')) {
                        echo "Local registry ${env.REGISTRY_HOST}: pushing without login"
                        run "docker push ${env.IMAGE_REMOTE}"
                        run "docker push ${env.IMAGE_REMOTE_LATEST}"
                    } else {
                        withCredentials([usernamePassword(credentialsId: params.DOCKER_CREDENTIALS_ID,
                                usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                            if (isUnix()) {
                                sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin $REGISTRY_HOST'
                            } else {
                                bat '@echo %DOCKER_PASS%| docker login -u %DOCKER_USER% --password-stdin %REGISTRY_HOST%'
                            }
                            try {
                                run "docker push ${env.IMAGE_REMOTE}"
                                run "docker push ${env.IMAGE_REMOTE_LATEST}"
                            } finally {
                                run "docker logout ${env.REGISTRY_HOST}"
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                // Never deploy a build whose tests failed or were marked unstable.
                expression { currentBuild.currentResult == 'SUCCESS' && params.DEPLOY_MODE != 'none' }
            }
            steps {
                script {
                    if (params.DEPLOY_MODE == 'docker') {
                        String image = params.PUSH_IMAGE ? env.IMAGE_REMOTE : env.IMAGE_LOCAL
                        // Waits for the container health check and restores the previous image on failure.
                        if (isUnix()) {
                            sh "chmod +x scripts/*.sh && scripts/deploy-docker.sh ${image} ${params.APP_PORT} ${env.CONTAINER_NAME}"
                        } else {
                            powershell ".\\scripts\\deploy-docker.ps1 -Image ${image} -Port ${params.APP_PORT} -Name ${env.CONTAINER_NAME}"
                        }
                    } else {
                        if (isUnix()) {
                            sh "chmod +x scripts/*.sh && scripts/deploy-jar.sh ${params.APP_PORT} \"${env.JENKINS_HOME}/recipe-deploy\""
                        } else {
                            powershell ".\\scripts\\deploy-jar.ps1 -Port ${params.APP_PORT} -DeployDir \"${env.JENKINS_HOME}\\recipe-deploy\""
                        }
                    }
                }
                echo "Deployed build #${env.BUILD_NUMBER} (${params.DEPLOY_MODE}) on port ${params.APP_PORT}"
            }
        }
    }

    post {
        success {
            echo "SUCCESS: ${env.APP_NAME} build #${env.BUILD_NUMBER} (${env.GIT_SHORT}) is tested and deployed."
        }
        unstable {
            echo 'UNSTABLE: some tests failed - image was not pushed and nothing was deployed.'
        }
        failure {
            echo 'FAILURE: pipeline stopped - see the failed stage, JUnit report and target/screenshots artifacts.'
        }
        always {
            script {
                if (params.DEPLOY_MODE != 'jar') {
                    // Remove dangling layers left by rebuilds; tagged release images are kept for rollback.
                    try { run 'docker image prune -f' } catch (ignored) { echo 'docker image prune skipped' }
                }
            }
        }
    }
}
