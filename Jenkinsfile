pipeline {
agent any
stages {
stage('Clean') {
steps {
sh 'mvn clean'
}
}
stage('Compile') {
steps {
sh 'mvn compile'
}
}
stage('Test') {
steps {
}
sh 'mvn test -Dmaven.test.failure.ignore=true'
}
stage('PMD') {
steps {
sh 'mvn pmd:pmd'
}
}
stage('JaCoCo') {
steps {
}
sh 'mvn jacoco:report'
}
stage('Javadoc') {
steps {
}
sh 'mvn javadoc:javadoc'
}
stage('Site') {
steps {
sh 'mvn site'
}
}
stage('Package') {
steps {
sh 'mvn package -DskipTests'
}
}
}
post {
always {
archiveArtifacts artifacts: '**/target/site/**/*
archiveArtifacts artifacts: '**/target/**/*
archiveArtifacts artifacts: '**/target/**/*
.jar'
.war'
junit '**/target/surefire-reports/*
.xml'
*'
.
, fingerprint: true
, fingerprint: true
, fingerprint: true
}
}
}
