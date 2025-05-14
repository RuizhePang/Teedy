pipeline {
    agent any
    environment {
        // Define environment variable
        DOCKER_HUB_CREDENTIALS = credentials('docker-hub')  // Docker Hub credentials ID store in Jenkins
        DOCKER_IMAGE = 'xx/teedy-app'  // Your Docker Hub username and Repository's name
        DOCKER_TAG = "${env.BUILD_NUMBER}"  // Use build number as tag
    }
    stages {
        stage('Build') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/master']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/RuizhePang/Teedy.git']]  // Your GitHub Repository
                )
                sh 'mvn -B -DskipTests clean package'
            }
        }
        
        // Build Docker images
        stage('Building image') {
            steps {
                script {
                    // Assuming Dockerfile is located at root
                    docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }

        // Upload Docker image to Docker Hub
        stage('Upload image') {
            steps {
                script {
                    // Sign in to Docker Hub
                    docker.withRegistry('https://registry.hub.docker.com', 'DOCKER_HUB_CREDENTIALS') {
                        // Push image
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push()
                        // Optional: Push 'latest' tag
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push('latest')
                    }
                }
            }
        }

        // Run Docker containers
        stage('Run containers') {
            steps {
                script {
                    // Stop and remove existing containers if they exist
                    sh 'docker stop teedy-container-8082 || true'
                    sh 'docker rm teedy-container-8082 || true'
                    sh 'docker stop teedy-container-8083 || true'
                    sh 'docker rm teedy-container-8083 || true'
                    sh 'docker stop teedy-container-8084 || true'
                    sh 'docker rm teedy-container-8084 || true'

                    // Run three containers with different ports
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run(
                        '--name teedy-container-8082 -d -p 8082:8080'
                    )
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run(
                        '--name teedy-container-8083 -d -p 8083:8080'
                    )
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run(
                        '--name teedy-container-8084 -d -p 8084:8080'
                    )

                    // Optional: List all teedy-containers
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
}
