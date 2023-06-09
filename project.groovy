pipeline {
    agent any
    environment {
        AWS_ACCOUNT_ID="573086865842"
        AWS_DEFAULT_REGION="us-east-1"
        IMAGE_REPO_NAME="repo3"
        IMAGE_TAG="v1"
        REPOSITORY_URI = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${IMAGE_REPO_NAME}"
        ECS_CLUSTER = "cluster-3"
        ECS_TASK_DEFINITION_FAMILY = "my-task-definition-3"
    }
   
    stages {
        
        stage('Cloning Git') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '*/main']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '', url: 'https://github.com/nikgit7/org-task.git']]])     
            }
        }

        stage('Logging into AWS ECR') {
            steps {
                script {
                sh "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
                }
                 
            }
        }

        stage('Building image') {
            steps{
                script {
                dockerImage = docker.build "${IMAGE_REPO_NAME}:${IMAGE_TAG}"
                }
            }
        }

        stage('Testing'){
            steps{
                script{
                sh "docker run -dp 8099:80 --name test ${IMAGE_REPO_NAME}:${IMAGE_TAG}"
                def commandOutput = sh(script: 'curl localhost:8099', returnStatus: true)
                if (commandOutput == 0) {
                    echo "success"
                } else {
                    error "This pipeline stops here!"
                }
                sh "docker rm -f test"
             }
            }
        }   

        stage('Pushing to ECR') {
            steps{  
                script {
                sh "docker tag ${IMAGE_REPO_NAME}:${IMAGE_TAG} ${REPOSITORY_URI}:$IMAGE_TAG"
                sh "docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${IMAGE_REPO_NAME}:${IMAGE_TAG}"
                }
            }
        }

        stage('Creating task definition') {
            steps{
                script {
                sh "aws ecs register-task-definition --memory 200 --cpu 512 --network-mode awsvpc --family ${ECS_TASK_DEFINITION_FAMILY} --container-definitions '[{\"name\": \"my-container\",\"image\": \"${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${IMAGE_REPO_NAME}:${IMAGE_TAG}\",\"portMappings\": [{\"containerPort\": 80,\"hostPort\": 80,\"protocol\": \"tcp\"}]}]'"
                }
            }
        }
        stage('Creating Service') {
            steps{
                script {
                sh "aws ecs create-service --cluster ${ECS_CLUSTER} --service-name my-service --task-definition ${ECS_TASK_DEFINITION_FAMILY} --desired-count 1 --launch-type EC2 --network-configuration 'awsvpcConfiguration={subnets=[subnet-07e51abbdc54b0fa5],securityGroups=[sg-060894e3594418d68]}'"
                }
            }
        }
    }
}
