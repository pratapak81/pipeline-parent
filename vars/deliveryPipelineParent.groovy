def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def remote = [:]
    remote.name = 'VM Server'
    remote.host = '10.12.44.121'
    remote.user = 'ubuntu'
    remote.password = 'ubuntu123'
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
            choice(description: 'Choose a value?', name: 'environment', choices: ['LOCAL', 'PROD'])
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
                    sh 'mvn clean -Dmaven.test.failure.ignore=true install'
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

            /*stage('Deploy') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'VM_USERNAME_PASSWORD', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'echo $PASSWORD'
                        echo USERNAME
                        echo PASSWORD
                        echo "username is $USERNAME"
                    }
                }
            }*/

            stage('Remote SSH') {
                steps {
                    sh 'ls -a'
                    writeFile file: 'target/spring-hello-world-0.0.1-SNAPSHOT.jar', text: 'ls -lrt'
                    sshPut remote: remote, from: 'target/spring-hello-world-0.0.1-SNAPSHOT.jar', into: 'Downloads'
                }
            }

            stage('Docker Initialize'){
                steps {
                    script {
                        def dockerHome = tool 'pratap-docker'
                        env.PATH = "${dockerHome}/bin:${env.PATH}"
                    }
                }
            }

            stage('Docker') {
                /*agent {
                    dockerfile true
                }*/
                steps {
                    script {
                        docker.withRegistry('https://hub.docker.com/', 'DOCKER_HUB_CREDENTIAL') {

                            def customImage = docker.build("hello-world:latest")

                            /* Push the container to the custom Registry */
                            customImage.push()
                        }
                    }
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