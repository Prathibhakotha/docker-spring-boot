pipeline {
    agent any
    stages {
         stage('git') {
            steps {
                git url:'https://github.com/Prathibhakotha/docker-spring-boot.git'
            }
         }
		stage('build') {
            steps {
               sh 'mvn clean install'
            }
        }
    }
}
