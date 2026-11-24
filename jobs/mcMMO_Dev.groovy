// mcMMO-Dev: the folder of jobs for mcMMO-Dev's projects, and its jobs.
folder('mcMMO-Dev') {
  description("Builds of mcMMO-Dev's projects, kept as each build's artifacts.")
}

// mcMMO, built from the head of its master branch. Each run reads its pipeline
// from this repository's main branch.
pipelineJob('mcMMO-Dev/mcMMO') {
  description("mcMMO, built from the head of https://github.com/mcMMO-Dev/mcMMO's master branch, with each build's mcMMO.jar kept as its artifact.")
  // Its trigger, as its Jenkinsfile declares it. Each seed run writes the job
  // anew, without what its runs added, so a trigger only the Jenkinsfile
  // declares would be gone after the seed, with no run left to bring it back.
  properties {
    pipelineTriggers {
      triggers {
        pollSCM {
          scmpoll_spec('H * * * *')
        }
      }
    }
  }
  definition {
    cpsScm {
      scm {
        git {
          remote {
            url('https://github.com/regulad/jobs.git')
          }
          branch('*/main')
        }
      }
      scriptPath('pipelines/mcMMO-Dev/mcMMO/Jenkinsfile')
      lightweight(true)
    }
  }
}
