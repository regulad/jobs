# jobs

The jobs of Jenkins at https://ci.regulad.xyz/, other than the `regulad`
organization folder, which builds each of regulad's repositories that has a
`Jenkinsfile`. This repository's own `Jenkinsfile` is the seed that applies the
rest, so that folder builds it, as `regulad/jobs`.

## Layout

| Path | What it holds |
| --- | --- |
| `jobs/<folder>.groovy` | [Job DSL](https://jenkinsci.github.io/job-dsl-plugin/) defining a folder and its jobs. Job DSL makes a Groovy class of each script, so a character a class name can't hold, such as `-`, is written `_`: `mcMMO-Dev`'s jobs are in `jobs/mcMMO_Dev.groovy` |
| `pipelines/<folder>/<job>/Jenkinsfile` | Each pipeline job's pipeline, which the job reads from `main` at every run |
| `Jenkinsfile` | The seed, which applies `jobs/` in `main`'s build, `regulad/jobs/main` |
| `.sops.yaml` | Encrypts files named `*.sops.<json\|yaml\|yml\|env>` with SOPS, to Parker's age key and to this repository's, which Jenkins holds as `jobs-sops-age-key` |
| `winget.sops.yaml` | `ssh_signing_key`, the private half of `jenkins-winget`, one of regubot's SSH signing keys |
| `.github/settings.yml` | This repository's name, description and topics, which the Settings app applies |

| Job | What it does |
| --- | --- |
| `mcMMO-Dev/mcMMO` | Builds [mcMMO](https://github.com/mcMMO-Dev/mcMMO) from the head of its `master` branch whenever it moves, keeping each build's `mcMMO.jar` as its artifact |
| `vadimgrn/usbip-win2` | Submits [usbip-win2](https://github.com/vadimgrn/usbip-win2)'s latest release to winget as `vadimgrn.usbip-win2`, with [Komac](https://github.com/russellbanks/Komac), as [regubot](https://github.com/regubot), regulad's machine account |
| `z4yx/pam_rssh` | Uploads [pam_rssh](https://github.com/z4yx/pam_rssh)'s releases to Nexus's `pam-rssh-*` repositories at https://repo.regulad.xyz/; [its README](pipelines/z4yx/pam_rssh/README.md) says how to install from them |

## How a change reaches Jenkins

At each push, GitHub tells Jenkins through its App's webhook, and the `regulad`
organization folder builds the pushed branch or pull request of `regulad/jobs`.
On `main`, the seed runs Job DSL over `jobs/**/*.groovy` at the pushed commit,
creating and updating what they define and deleting the items they no longer
define; Jenkins reports the result on the commit. Other branches and pull
requests apply nothing.

A pipeline's change needs no seeding: its job reads its `Jenkinsfile` from `main`
at each run. The seed can also be built by hand, as `regulad/jobs/main`.

Jenkins itself is [regulad/homelab](https://github.com/regulad/homelab)'s, under
Configuration as Code
(`ansible-generated/files/jenkins.regulad.internal/etc/jenkins/casc.d/`):

- **Script approval:** Job DSL runs without it, as only regulad writes here.
- **Credentials:** every credential these jobs name is in Jenkins's own store,
  from SOPS, and so every job's. The folders this repository defines hold none,
  since each seed run writes them anew. A job names its credentials by ID; a new
  one starts in homelab, before the job that names it is added here. Among them:
  - `regulad-jenkins`, a credential of Jenkins's GitHub App. Bound as a
    username and password, it gives the App's ID and a fresh installation
    token, which lasts at most an hour. It reads `regulad/jobs` alone, which is
    enough to read GitHub's public data at an installation's rate limit. A job
    that needs more asks for it in homelab; a permission the App itself lacks
    is first granted to the App on GitHub. Inside the `regulad` organization
    folder, that folder's own, broader `regulad-jenkins` takes its place.
  - `winget-token`, regubot's token for its fork of winget-pkgs.
  - `jobs-sops-age-key`, the age key that opens this repository's SOPS files.
- **Agents:** the `podman` label starts a container for each build, under
  rootless Podman beside the controller, from `jenkins/inbound-agent`, which
  has `curl`, `git`, coreutils and a JDK 21, but neither `jq` nor Python.
- **Maven:** the `maven` tool, a pinned Maven release, which Jenkins installs
  on the agent of each build that names it (`tools { maven 'maven' }`).

## Writing a pipeline

Pipelines from this repository run in Jenkins's Groovy sandbox, without
script approvals:

- **Parse JSON** with `groovy.json.JsonSlurper` inside a `@NonCPS` method that
  returns plain lists, maps and strings, since its own maps can't outlive a
  step. `JsonSlurperClassic` isn't allowed.
- **Keep secrets out of Groovy strings:** pass values to `sh` through `withEnv`,
  and write its script in single quotes, so that `withCredentials` values reach
  the shell as environment variables.
