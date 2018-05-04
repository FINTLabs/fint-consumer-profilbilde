pipeline {
    agent none
    stages {
        stage('Build') {
            agent { label 'docker' }
            steps {
                script {
                    props=readProperties file: 'gradle.properties'
                    VERSION="${props.version}-${props.apiVersion}"
                }
                sh "docker build --tag 'dtr.rogfk.no/fint-beta/consumer-avatar:${VERSION}' --build-arg apiVersion=${props.apiVersion} ."
            }
        }
        stage('Publish') {
            agent { label 'docker' }
            when {
                branch 'master'
            }
            steps {
                withDockerRegistry([credentialsId: 'dtr-rogfk-no', url: 'https://dtr.rogfk.no']) {
                    sh "docker push 'dtr.rogfk.no/fint-beta/consumer-avatar:${VERSION}'"
                }
            }
        }
    }
}
