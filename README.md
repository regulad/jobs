# jobs

The jobs of Jenkins at https://ci.regulad.xyz/, other than the `regulad`
organization folder, which builds each of regulad's repositories that has a
`Jenkinsfile`. This repository has none of its own, so that folder leaves it
alone.

## Layout

| Path | What it holds |
| --- | --- |
| `jobs/<folder>.groovy` | [Job DSL](https://jenkinsci.github.io/job-dsl-plugin/) defining a folder's jobs; the folder itself is homelab's (below) |
| `pipelines/<folder>/<job>/Jenkinsfile` | Each pipeline job's pipeline, which the job reads from `main` at every run |
| `seed/config.xml` | The seed job, `jobs`, which applies `jobs/` |
| `seed/apply.sh` | Writes the seed job into Jenkins, builds it at a commit, and waits for its result |
| `.github/workflows/seed.yml` | Runs `seed/apply.sh` at each push to `main` |
| `.github/settings.yml` | This repository's name, description and topics, which the Settings app applies |

| Job | What it does |
| --- | --- |
| `z4yx/pam_rssh` | Uploads [pam_rssh](https://github.com/z4yx/pam_rssh)'s releases to Nexus's `pam-rssh-*` repositories at https://repo.regulad.xyz/; [its README](pipelines/z4yx/pam_rssh/README.md) says how to install from them |

## How a change reaches Jenkins

At each push to `main`, the `seed` workflow signs in to Jenkins as `svc-jobs`
and:

1. writes the seed job, `jobs`, from `seed/config.xml`, creating it where
   Jenkins lacks it;
2. builds it at the pushed commit, which runs Job DSL over `jobs/**/*.groovy`,
   creating and updating what they define and deleting the items they no longer
   define;
3. prints the seed's log, and fails if the seed does.

A pipeline's change needs no seeding: its job reads its `Jenkinsfile` from `main`
at each run. The workflow can also be run by hand, from the Actions tab.

`seed/config.xml` is the XML Job DSL itself generates for the seed job, so that
it matches the plugins Jenkins runs. To change the seed, write the change as Job
DSL, run it through Job DSL without applying it, and take the XML.

Jenkins itself is [regulad/homelab](https://github.com/regulad/homelab)'s, under
Configuration as Code
(`ansible-generated/files/jenkins.regulad.internal/etc/jenkins/casc.d/`):

- **svc-jobs:** a FreeIPA user, as every Jenkins user is, and so an
  administrator. homelab keeps its password in SOPS, and its `tofu/github`
  sets this repository's `JENKINS_USER` and `JENKINS_PASSWORD` secrets from it.
- **Script approval:** Job DSL runs without it, as only regulad writes here.
- **Folders and credentials:** each folder these jobs live in, such as `z4yx`,
  is defined there with the credentials only its items can use, from SOPS. This
  repository defines the jobs inside, which name those credentials by ID, and
  never a folder: the seed would rewrite it without its credentials. A new
  folder starts there, before its jobs are added here.
- **Agents:** the `podman` label starts a container for each build, under
  rootless Podman beside the controller, from `jenkins/inbound-agent`, which
  has `curl`, `git` and coreutils, but neither `jq` nor Python.

## Writing a pipeline

Pipelines from this repository run in Jenkins's Groovy sandbox, without
script approvals:

- **Parse JSON** with `groovy.json.JsonSlurper` inside a `@NonCPS` method that
  returns plain lists, maps and strings, since its own maps can't outlive a
  step. `JsonSlurperClassic` isn't allowed.
- **Keep secrets out of Groovy strings:** pass values to `sh` through `withEnv`,
  and write its script in single quotes, so that `withCredentials` values reach
  the shell as environment variables.
