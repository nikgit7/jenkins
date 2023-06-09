pipeline {
    agent none
    environment {
        AWS_ACCOUNT_ID = "055214151635"
        AWS_DEFAULT_REGION = "us-east-1"
        IMAGE_REPO_NAME = "qwe"
        IMAGE_TAG = "v-25"
        REPOSITORY_URI = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${IMAGE_REPO_NAME}"
    }
    stages {
        stage('job1 build docker image and push it to ECR') {
            agent {
                label "master"
            }
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    submoduleCfg: [],
                    userRemoteConfigs: [[credentialsId: '', url: 'https://github.com/nikgit7/class-projects.git']]
                ])
                script {
                    dockerImage = docker.build "${IMAGE_REPO_NAME}:${IMAGE_TAG}"
                }
                script {
                    sh "aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 055214151635.dkr.ecr.us-east-1.amazonaws.com"
                    sh "docker tag ${IMAGE_REPO_NAME}:${IMAGE_TAG} ${REPOSITORY_URI}:${IMAGE_TAG}"
                    sh "docker push ${REPOSITORY_URI}:${IMAGE_TAG}"
                }
            }
        }
        stage('S2') {
            agent {
                label "agent1"
            }
            steps {
                script {
                    sh "sudo yum install git -y"
                }
 
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [],
                    submoduleCfg: [],
                    userRemoteConfigs: [[credentialsId: '', url: 'https://github.com/nikgit7/class-projects.git']]
                ]) 
                script {
                    dockerImage = docker.build "nikhildocker28/repo1:from-pipeline"
                }
                script {
                    sh "sudo yum install docker -y"
                    sh "sudo systemctl start docker && sudo chmod 666 /var/run/docker.sock && docker login --username=nikhildocker28 --password=nikhildocker $DOCKER_HOST"
                    sh "docker push nikhildocker28/repo1:from-pipeline"
                }               

            }
        }
    }
}
