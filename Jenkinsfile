#! groovy

properties([
    [$class: 'BuildBlockerProperty',
        blockLevel: '',
        blockingJobs: '',
        scanQueueFor: '',
        useBuildBlocker: false],
		pipelineTriggers()
    ])

@Library('ace_shared_lib@security_updates') _
    pipeline {
        options { disableConcurrentBuilds() }
    agent {
              kubernetes {
                label "k8s-maven-${cto.devops.jenkins.Utils.getTimestamp()}" // give your podtemplate a unique name, this can be achieved by adding a suffix that contains the date. Note that labels can't be longer than 63 characters
                inheritFrom 'k8s-build' // select a podtemplate to inherit from, the default one is k8s-build which already has a JNLP image
                containerTemplate {
				name 'maven' // name your container something useful
				image "aado-docker-releases.${ARTIFACTORY_FQDN}/build_openjdk8_maven3:latest" //this image is the image used in the original docker agent
				alwaysPullImage true //always pull image from artifactory, don't rely on the local tags to be accurate
				workingDir '/home/jenkins' // specify the workingdir, /home/jenkins is shared between the different containers in the pod
				ttyEnabled true // DO NOT CHANGE THIS LINE // set to true
				command 'cat' // DO NOT CHANGE THIS LINE // specify a long running command as entrypoint, otherwise the container will exit as soon as the command has finished, using 'cat' here is what the kubernetes jenkins plugin recommends
				args '' // DO NOT CHANGE THIS LINE // specify the arguments to pass to the long running command. Even when you don't specify any arguments this should be present
                }
              }
    }
    environment {
      BUILDID = "${env.BUILD_ID}"
    }

     triggers {
        cron("0 0 * * *")

        gerrit(customUrl: '',
            gerritProjects: [[
                branches:
                    [[compareType: 'PLAIN',
                      pattern: "${params.BRANCH}"
                    ]],
                    compareType: 'PLAIN',
                    disableStrictForbiddenFileVerification: false,
                    pattern: "Prathibhakotha/docker-spring-boot"
            ]],
            serverName: 'github',
            triggerOnEvents:
                [changeMerged(),
                 patchsetCreated(excludeDrafts: false,
                                 excludeNoCodeChange: false,
                                 excludeTrivialRebase: true)
                ]
        )
    }
        stages {

     stage('Checkout') {
      steps {
        container('maven') {
          checkout scm
        }
      }
    }
        stage('Build') {
        steps {
      container('maven') {
     // Use -B flag to not get a progress link for artifact download progress update
				// Update the version in the POM

				sh 'echo "Building kubernetes build"'

				sh '/opt/apache-maven/bin/mvn -s SourceCode/bhp-adapter-1/bhp_setting_file/settings.xml -B -f SourceCode/bhp-adapter-1/pom.xml versions:set -DnewVersion=${BUILDID} -DskipTests=true'

				// Build but skip the tests. We will run the tests in the next stage so that we get a nice visualization of the pipeline.
			    sh '/opt/apache-maven/bin/mvn -s SourceCode/bhp-adapter-1/bhp_setting_file/settings.xml -B -f SourceCode/bhp-adapter-1/pom.xml install -DskipTests=true'

				//sh 'echo "Moving gz files to 05_Others/buildRepo "'
				//sh 'mv 01_SourceCode/target/*.tar.gz 05_Others/buildRepo'

				sh 'echo "Building non-kubernetes build"'
				sh '/opt/apache-maven/bin/mvn -s SourceCode/bhp-adapter-1/bhp_setting_file/settings.xml -B -f SourceCode/bhp-adapter-1/pom.xml versions:set -DnewVersion=${BUILDID} -DskipTests=true'

				sh '/opt/apache-maven/bin/mvn -s SourceCode/bhp-adapter-1/bhp_setting_file/settings.xml -B -f SourceCode/bhp-adapter-1/pom.xml install -DskipTests=true'

				//sh 'echo "Moving docker images "'
				//sh 'mv 05_Others/docker/kubernetes*.gz 01_SourceCode/target/Nokia-EAS-k8-1.0.${BUILDID}.tar.gz'

				//sh 'echo "Restoring gz files to 01_SourceCode/target"'
				//sh 'mv 05_Others/buildRepo/*.tar.gz 01_SourceCode/target/*.tar.gz'
      }
    }
      }
	  
	   

    stage ('Archive Artifacts') {
      steps {
    container('maven') {

      sh 'echo "Archiving the WAR file"'
      archiveArtifacts 'SourceCode/bhp-adapter-1/target/*.jar'

      //sh 'echo "Archiving the RPM file"'
    }
    }
  }
    stage('Publish candidate') {


       when { environment name: 'GERRIT_EVENT_TYPE', value: 'change-merged' }


      steps {
    container('maven') {
          script {
            // Lookup the Artifactory server from the global environment variable:
            // https://confluence.app.alcatel-lucent.com/display/AACTODEVOPS/Jenkins+environment+and+contract
            def artifactoryServer = Artifactory.newServer url: env.ARTIFACTORY_HTTPS_URL, credentialsId: "${PU_ARTIFACTORY}-artifactory"

            // Load the build info generated by the Maven Artifactory plugin
           // def buildInfo = readJSON file: "03_Deployment/Scripts/Package/target/build-info.json"

       // Collect build info
             def buildInfo = Artifactory.newBuildInfo()

                     buildInfo.env.capture = true
                     buildInfo.env.collect()

                     def uploadSpec = """{
						  "files": [
							  {
								"pattern":"SourceCode/bhp-adapter-1/target/*.jar",
								"target": "nswps-mvn-candidates-local/nswps/deccm/DECCMCUSBHPBHPIMPIDC/bhp-adapter-1/${env.BUILD_NUMBER}/"
							 }
							//{
							//	"pattern":"SourceCode/bhp-adapter-1/target/rpm/*/RPMS/noarch/*.rpm",
							//	"target": "nswps-mvn-candidates-local/nswps/deccm/DECCMCUSBHPBHPIMPIDC/bhp-adapter-1/${env.BUILD_NUMBER}/"
							//}
						  ]
						 }"""

                     // Upload files and publish build
                     artifactoryServer.upload(uploadSpec, buildInfo)
                     artifactoryServer.publishBuildInfo(buildInfo)

            // Allow interactive promotion of the candidate build
            def promotionConfig = [
              'buildName'          : buildInfo.name,
              'buildNumber'        : buildInfo.number,
              'status'             : 'Released',
              'targetRepo'         : 'nswps-mvn-releases',
        'includeDependencies': false,
              'copy'               : true, // "copy" must be used because "move" requires delete permission
              'failFast'           : true
            ]
            Artifactory.addInteractivePromotion server: artifactoryServer, promotionConfig: promotionConfig, displayName: "Promote release candidate"
          }
    }
    }
    }
        }
        post {
          always {
           deleteDir()
         }


         success {

            emailext (
                attachLog: true,
                subject: "Jenkins build Success: Job ** '${env.JOB_NAME.split('/')[2]}' ** ",
                body: """
                 Dear All,
				  
				${currentBuild.projectName} Build Execution Success
					
				Project Name:   ${currentBuild.projectName}
					
				Build Location: ${currentBuild.absoluteUrl}
				  
				Build Status:   ${currentBuild.currentResult}

				Also please do refer to the logs attached for more details.

                  
				Regards,				  
				BHP-Development Team.

                """,
                to: ""
                
            )

         } // Success Block
      } // Post Block
  }