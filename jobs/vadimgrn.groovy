// vadimgrn: the folder of jobs for vadimgrn's projects, and its jobs.
folder('vadimgrn') {
  description("Submissions of vadimgrn's projects to winget, as regubot.")
}

// usbip-win2's releases, submitted to winget as vadimgrn.usbip-win2 by regubot,
// with winget-token, a credential in Jenkins's own store. Each run reads its
// pipeline from this repository's main branch.
pipelineJob('vadimgrn/usbip-win2') {
  description("usbip-win2's releases, from https://github.com/vadimgrn/usbip-win2, submitted to winget as vadimgrn.usbip-win2 by regubot.")
  // As its Jenkinsfile declares it, so that even the job's first build can be a
  // dry run: Jenkins learns a pipeline's own parameters only from a run.
  parameters {
    booleanParam('DRY_RUN', false, 'Print the manifests that would be submitted, without submitting them')
  }
  // Its trigger too, as its Jenkinsfile declares it. Each seed run writes the
  // job anew, without what its runs added, so a trigger only the Jenkinsfile
  // declares would be gone after the seed, with no run left to bring it back.
  properties {
    pipelineTriggers {
      triggers {
        cron {
          spec('H * * * *')
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
      scriptPath('pipelines/vadimgrn/usbip-win2/Jenkinsfile')
      lightweight(true)
    }
  }
}
