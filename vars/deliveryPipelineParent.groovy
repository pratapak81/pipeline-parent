def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def remote = [:]
    remote.name = 'VM Server'
    remote.host = '10.184.96.145'
    remote.user = 'digital'
    remote.password = 'Password@123'
    remote.allowAnyHosts = true

    pipeline {
        agent any

        tools {
            maven 'Maven'
            jdk 'jdk8'
        }

        environment {
            // place to declare variables
            author = "Pratap A K"
        }

        parameters {
            choice(description: 'Choose a value?',name: 'environment',choices: ['LOCAL', 'PROD'])
            string(name: 'Greeting', defaultValue: 'Hello', description: 'How should I greet the world?')
        }

        stages {
            stage("Initialize") {
                steps {
                    echo "selectedEnvironment: ${params.environment}"
                    echo "Running ${env.BUILD_ID} on ${env.JENKINS_URL}"
                    echo "${params.Greeting} World!"
                    sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
                }
            }

//            stage('checkout git') {
//                steps {
//                    git branch: pipelineParams.branch, credentialsId: 'fa186de2-1a7e-4d16-9be3-fed9dc6c80ea', url: pipelineParams.scmUrl
//                }
//            }

            stage('Build') {
                steps {
                    sh 'mvn -Dmaven.test.failure.ignore=true install'
                }
                post {
                    success {
                        junit 'target/surefire-reports/**/*.xml'
                    }
                }
            }

            /*stage('SonarQube Analysis') {
                steps {
                    withSonarQubeEnv('sonar-1') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }*/

            stage('Deploy') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'VM_USERNAME_PASSWORD', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'echo $PASSWORD'
                        echo USERNAME
                        echo PASSWORD
                        echo "username is $USERNAME"
                    }
                }
            }

            stage('Remote SSH') {
                steps {
                    sshCommand remote: remote, command: "ls -lrt"
                    sshCommand remote: remote, command: "for i in {1..5}; do echo -n \"Loop \$i \"; date ; sleep 1; done"
                }
            }
        }

        post {
            always {
                echo "post build always"
            }
            failure {
                echo "failure happen"
            }
        }
    }
}