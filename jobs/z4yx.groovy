// z4yx: jobs for z4yx's projects. The z4yx folder itself, with the credentials
// only its jobs can use, is regulad/homelab's: a folder defined here would be
// rewritten by the seed without them.

// pam_rssh's releases into Nexus's pam-rssh repositories, as svc-pam-rssh-ci, a
// credential of the z4yx folder. Each run reads its pipeline from this
// repository's main branch.
pipelineJob('z4yx/pam_rssh') {
  description("pam_rssh's releases, from https://github.com/z4yx/pam_rssh, into Nexus's pam-rssh repositories.")
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
      scriptPath('pipelines/z4yx/pam_rssh/Jenkinsfile')
      lightweight(true)
    }
  }
}
