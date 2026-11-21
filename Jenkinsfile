// The seed: applies jobs/ with Job DSL. The regulad organization folder builds
// this repository as regulad/jobs, each of its branches and pull requests at
// each push to it; only main's build applies anything, at the commit pushed,
// and the others start no agent.
//
// Job DSL creates and updates the items jobs/**/*.groovy define, and deletes
// the items they no longer define. Their names are full names from Jenkins's
// root, such as z4yx/pam_rssh, not from the seed's place in regulad.

pipeline {
  agent none
  options {
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '50'))
    timeout(time: 30, unit: 'MINUTES')
  }
  stages {
    stage('Apply jobs/') {
      agent { label 'podman' }
      when {
        beforeAgent true
        branch 'main'
      }
      steps {
        jobDsl(targets: 'jobs/**/*.groovy',
               sandbox: false,
               ignoreExisting: false,
               ignoreMissingFiles: false,
               failOnMissingPlugin: true,
               failOnSeedCollision: true,
               unstableOnDeprecation: true,
               removedJobAction: 'DELETE',
               removedViewAction: 'DELETE',
               removedConfigFilesAction: 'IGNORE',
               lookupStrategy: 'JENKINS_ROOT')
      }
    }
  }
}
