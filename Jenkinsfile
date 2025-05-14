pipeline {
    agent any
    tools {
        maven 'M4'
    }
    environment {
        DOCKER_HUB_CREDENTIALS = credentials('docker-hub')
        DOCKER_IMAGE = 'ruizhepang/teedy'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    stages {
        stage('Build') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/master']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/RuizhePang/Teedy.git']]
                )
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Building image') {
            steps {
                script {
                    docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }
        stage('Upload image') {
            steps {
                script {
                    sh """
                        docker login -u ruizhepang -p 'Docker190504'
                        docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                        docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} ${env.DOCKER_IMAGE}:latest
                        docker push ${env.DOCKER_IMAGE}:latest
                    """
                }
            }
        }
        stage('Run containers') {
            steps {
                script {
                    sh 'docker stop teedy-container-8081 || true'
                    sh 'docker rm teedy-container-8081 || true'
                    sh "docker run --name teedy-container-8081 -d -p 8081:8080 ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
}
