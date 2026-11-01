// Declarative CI/CD pipeline for the Recipe Management System.
// Runs on Linux (sh) and Windows (bat/powershell) agents.

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
        string(name: 'APP_PORT', defaultValue: '8085', description: 'Port the deployed application listens on')
        booleanParam(name: 'RUN_SELENIUM', defaultValue: true, description: 'Run the Selenium end-to-end stage (needs Chrome on the agent)')
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME = 'recipe-management-system'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = isUnix()
                            ? sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                            : bat(script: '@git rev-parse --short HEAD', returnStdout: true).trim()
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
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/screenshots/**', allowEmptyArchive: true
                }
            }
        }

        stage('Deploy') {
            when {
                // Never deploy a build whose tests failed or were marked unstable.
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                script {
                    if (isUnix()) {
                        sh "chmod +x scripts/*.sh && scripts/deploy-jar.sh ${params.APP_PORT} \"${env.JENKINS_HOME}/recipe-deploy\""
                    } else {
                        powershell ".\\scripts\\deploy-jar.ps1 -Port ${params.APP_PORT} -DeployDir \"${env.JENKINS_HOME}\\recipe-deploy\""
                    }
                }
                echo "Deployed: http://localhost:${params.APP_PORT}"
            }
        }
    }

    post {
        success {
            echo "Pipeline succeeded for build #${env.BUILD_NUMBER}"
        }
        failure {
            echo 'Pipeline failed - deployment was skipped or rolled back. Check the stage logs above.'
        }
    }
}
