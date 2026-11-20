// mcMMO-Dev: jobs for mcMMO-Dev's projects. The mcMMO-Dev folder itself is
// regulad/homelab's, as every folder of these jobs is.

// mcMMO, built from the head of its master branch. Each run reads its pipeline
// from this repository's main branch.
pipelineJob('mcMMO-Dev/mcMMO') {
  description("mcMMO, built from the head of https://github.com/mcMMO-Dev/mcMMO's master branch, with each build's mcMMO.jar kept as its artifact.")
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
