pipeline {
    agent any

    environment {
        IMAGE_NAME = 'ruizhepang/teedy:latest'
        DEPLOYMENT_NAME = 'hello-node'
        CONTAINER_NAME = 'docs'
    }

    stages {
        stage('Start Minikube') {
            steps {
                sh '''
                if ! minikube status | grep -q "Running"; then
                    echo "Starting Minikube..."
                    minikube start
                else
                    echo "Minikube already running."
                fi
                '''
            }
        }

        stage('Set Image') {
            steps {
                sh '''
                echo "Setting image for deployment..."
                kubectl set image deployment/${DEPLOYMENT_NAME} ${CONTAINER_NAME}=${IMAGE_NAME}
                '''
            }
        }

        stage('Verify') {
            steps {
                sh '''
                echo "Checking rollout status..."
                kubectl rollout status deployment/${DEPLOYMENT_NAME}
                kubectl get pods
                '''
            }
        }
    }
}
