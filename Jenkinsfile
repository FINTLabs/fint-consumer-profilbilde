pipeline {
    parameters {
        string(name: 'BUILD_FLAGS', defaultValue: '', description: 'Gradle build flags')
    }
    agent { label 'docker' }
    stages {
        stage('Build') {
            steps {
                script {
                    props = readProperties file: 'gradle.properties'
                }
                sh "docker build --tag ${GIT_COMMIT} --build-arg apiVersion=${props.apiVersion} --build-arg buildFlags=${params.BUILD_FLAGS} ."
            }
        }
        stage('Publish') {
            when {
                branch 'master'
            }
            steps {
                sh "docker tag ${GIT_COMMIT} fintlabs.azurecr.io/consumer-profilbilde:build.${BUILD_NUMBER}"
                withDockerRegistry([credentialsId: 'fintlabs.azurecr.io', url: 'https://fintlabs.azurecr.io']) {
                    sh "docker push fintlabs.azurecr.io/consumer-profilbilde:build.${BUILD_NUMBER}"
                }
            }
        }
        stage('Publish PR') {
            when { changeRequest() }
            steps {
                sh "docker tag ${GIT_COMMIT} fintlabs.azurecr.io/consumer-profilbilde:${BRANCH_NAME}.${BUILD_NUMBER}"
                withDockerRegistry([credentialsId: 'fintlabs.azurecr.io', url: 'https://fintlabs.azurecr.io']) {
                    sh "docker push fintlabs.azurecr.io/consumer-profilbilde:${BRANCH_NAME}.${BUILD_NUMBER}"
                }
            }
        }
        stage('Publish Version') {
            when {
                tag pattern: "v\\d+\\.\\d+\\.\\d+(-\\w+-\\d+)?", comparator: "REGEXP"
            }
            steps {
                script {
                    VERSION = TAG_NAME[1..-1]
                }
                sh "docker tag ${GIT_COMMIT} fintlabs.azurecr.io/consumer-profilbilde:${VERSION}"
                withDockerRegistry([credentialsId: 'fintlabs.azurecr.io', url: 'https://fintlabs.azurecr.io']) {
                    sh "docker push fintlabs.azurecr.io/consumer-profilbilde:${VERSION}"
                }
            }
        }
    }
}
