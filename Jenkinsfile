pipeline{
  agent{
    node { label 'master' }
  }
  options{
    timestamps()
    ansiColor('xterm')
  }
  stages{
    stage('Checkout Source'){
      steps{
        checkout scm
      }
    }
    stage('Building'){
      steps{
        // Determine what kind of build we are building
        csdBuild env.JOB_NAME
      }
    }
  }
  post{
    success{
      build job: 'jenkins-merge-when-green',
            parameters: [
              string(name: 'jenkinsJob', value: env.JOB_NAME ),
              string(name: 'jobNumber', value: env.BUILD_NUMBER),
              string(name: 'repo', value: 'spark')
            ],
            wait: false
    }
  }
}

def set_properties(){
  // Enabling JTTP on builds
  properties([
    disableConcurrentBuilds(),
    pipelineTriggers([
    issueCommentTrigger('.*jttp.*')
    ])
  ])
}

def prBuilder(){
  def mvnArgs="-Pdeb -U -Phadoop-2.7 -Dhadoop.version=2.8.2 -Pkinesis-asl -Pyarn -Phive -Phive-thriftserver -Dpyspark -Dsparkr"
  stage('PR Build'){
    sh "build/mvn ${mvnArgs} -DskipTests clean install"
  }
  stage('PR Test'){
    sh "build/mvn ${mvnArgs} install"
  }
}

def releaseBuilder(){
  def mvnArgs="-Pdeb -U -Phadoop-2.7 -Dhadoop.version=2.8.2 -Pkinesis-asl -Pyarn -Phive -Phive-thriftserver -Dpyspark -Dsparkr -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true"
  stage('Release Clean'){
    sh "build/mvn ${mvnArgs} release:clean"
  }
  stage('Release Prepare'){
    sh "build/mvn ${mvnArgs} release:prepare"
  }
  stage('Release Perform'){
    sh "build/mvn ${mvnArgs} release:perform"
  }
  stage('Stage Binaries'){
    sh """
    scp target/checkout/assembly/target/spark*_all.deb \
      mash@apt.clearstorydatainc.com:/var/repos/apt/private/dists/precise/misc/binary-all
    """
  }
}
def csdBuild(val) {
    switch (val) {
        case ~/.*PR.*/:
            prBuilder()
            break
        default:
            releaseBuilder()
            break
    }
}
