pipeline {
    agent any
    stages {
        stage('build') {
		 git url: 'https://github.com/Prathibhakotha/docker-spring-boot'
            withMaven {
                sh 'mvn --version'
            }
        }
    }
}