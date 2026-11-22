# z4yx/pam_rssh

This pipeline publishes [pam_rssh](https://github.com/z4yx/pam_rssh)'s
packages, the PAM module that authenticates `sudo` and other services with keys
from your SSH agent. Every hour it takes the packages from pam_rssh's GitHub
releases that Nexus at https://repo.regulad.xyz/ lacks, and uploads them to the
repositories below.
Machines install pam_rssh from those repositories with `apt` or `dnf`, and get
upstream's updates the same way as any other package's.

The repositories, their signing keys and the upload account are
[regulad/homelab](https://github.com/regulad/homelab)'s, in
`ansible-generated/inventory/group_vars/nexus_servers.yml`. This directory only
fills them.

## Channels

Each target has two channels:

- **Release:** packages from pam_rssh's releases. A release's files are never
  replaced once uploaded.
- **Testing:** packages from its pre-releases, any release GitHub marks as one,
  or whose tag has a SemVer pre-release part, such as `v1.2.2-rc2`, whatever
  GitHub's flag says. A later pre-release's build of the same file replaces an
  earlier one's.

Use the release channel, unless you need a build that has not been released
yet. A machine can use both: `apt` and `dnf` then install whichever version is
newer.

Packages reach a release repository only when a release builds for its
target. On 2026-11-19, the release repositories for Ubuntu held v1.2.1's
`1.2.0-1`, the Debian ones and the RPMs' release channel held nothing yet, and
every testing repository held v1.2.2-rc2's `1.2.2-1`.

## Debian and Ubuntu: APT

One repository per target and channel, each serving its target's suite with one
component, `main`, for `amd64` and `arm64`. Nexus signs each repository's
`InRelease` with a key of its own, which `keys` serves.

| Target | Suite | Release repository | Testing repository |
| --- | --- | --- | --- |
| Debian 12 | `bookworm` | `pam-rssh-debian-bookworm` | `pam-rssh-debian-bookworm-testing` |
| Debian 13 | `trixie` | `pam-rssh-debian-trixie` | `pam-rssh-debian-trixie-testing` |
| Ubuntu 22.04 | `jammy` | `pam-rssh-ubuntu-jammy` | `pam-rssh-ubuntu-jammy-testing` |
| Ubuntu 24.04 | `noble` | `pam-rssh-ubuntu-noble` | `pam-rssh-ubuntu-noble-testing` |
| Ubuntu 26.04 | `resolute` | `pam-rssh-ubuntu-resolute` | `pam-rssh-ubuntu-resolute-testing` |

Each repository is at `https://repo.regulad.xyz/repository/<repository>`, and
its key at `https://repo.regulad.xyz/repository/keys/<repository>.asc`. For
Ubuntu 24.04's release channel:

```sh
sudo install -d -m 0755 /etc/apt/keyrings
sudo curl -fsSL -o /etc/apt/keyrings/pam-rssh-ubuntu-noble.asc \
  https://repo.regulad.xyz/repository/keys/pam-rssh-ubuntu-noble.asc
sudo tee /etc/apt/sources.list.d/pam-rssh.sources >/dev/null <<'EOF'
Types: deb
URIs: https://repo.regulad.xyz/repository/pam-rssh-ubuntu-noble
Suites: noble
Components: main
Signed-By: /etc/apt/keyrings/pam-rssh-ubuntu-noble.asc
EOF
sudo apt update
sudo apt install pam-rssh
```

For another target, use its repository, suite and key. For its testing channel,
put `-testing` on the repository and key names; the suite stays the same.

One repository per suite works around a gap in Nexus: an APT hosted repository
serves one distribution, and Nexus cannot merge several. homelab marks the
workaround with a `FIXME`, linking
[sonatype/nexus-public#309](https://github.com/sonatype/nexus-public/issues/309).

## Fedora and Enterprise Linux: YUM

One repository, `pam-rssh-rpm`, laid out as `<channel>/<target>/<arch>/`. The
channel is `stable` or `testing`; the target is `el9` or `el10` for Enterprise
Linux, and `fc42`, `fc43`, `fc44` or `fc45` for Fedora; the arch is `x86_64`
or `aarch64`. Each directory is a repository of its own, with its own metadata.

Nexus signs the metadata, not the RPMs, and upstream leaves those unsigned too.
So `dnf` checks the signed metadata (`repo_gpgcheck=1`) and each RPM's checksum
against it, but no signature on the RPMs themselves (`gpgcheck=0`). For
Enterprise Linux's release channel, in `/etc/yum.repos.d/pam-rssh.repo`:

```ini
[pam-rssh]
name=pam_rssh
baseurl=https://repo.regulad.xyz/repository/pam-rssh-rpm/stable/el$releasever/$basearch/
enabled=1
repo_gpgcheck=1
gpgcheck=0
gpgkey=https://repo.regulad.xyz/repository/keys/pam-rssh-rpm.asc
```

Then `sudo dnf install pam-rssh`. For Fedora, write `fc$releasever` in place of
`el$releasever`. For the testing channel, write `testing` in place of `stable`.
Where `$releasever` is not the release's bare major version, write the target out,
as in `el9`. The testing channel also holds v1.2.2-rc1's `pam-rssh-debuginfo`
and `pam-rssh-debugsource` RPMs for Fedora.

## Keys

Check a key's fingerprint before you trust it:
`gpg --show-keys <repository>.asc` prints it.

| Repository | Fingerprint |
| --- | --- |
| `pam-rssh-debian-bookworm` | `5174F095439C7F66A8EA661F1B84509B94A9627B` |
| `pam-rssh-debian-bookworm-testing` | `11E12389280BC2054893C1F632048ADE38A46AE5` |
| `pam-rssh-debian-trixie` | `773F77761F2A7F882B345F9ED2336ABC1C9ADC42` |
| `pam-rssh-debian-trixie-testing` | `C7C88899C72A95F3CE1F4701FF49F81A84E98444` |
| `pam-rssh-ubuntu-jammy` | `B3C060D09317A9CBFCF9C7DCF2F495EAC2F2E571` |
| `pam-rssh-ubuntu-jammy-testing` | `74896A98AAACB6250CE81BFCFF04075FB5B30B49` |
| `pam-rssh-ubuntu-noble` | `6BCD2B2B5ADE28AA79FF63D5B95253151CE29087` |
| `pam-rssh-ubuntu-noble-testing` | `699E240248E2F187246ADB28F1F7A669E4B4E775` |
| `pam-rssh-ubuntu-resolute` | `78E0EADBD0F1DD795BC2545CB932890A70FA6DB9` |
| `pam-rssh-ubuntu-resolute-testing` | `90E5F9EF825BC632F519E3E618D29F73C7FB26F1` |
| `pam-rssh-rpm` | `0646556A5A5A02FF79995D0D10E199D27AFA1EED` |

## How the pipeline fills them

- It reads pam_rssh's releases through GitHub's API, and places each asset by
  its name.
  - A deb, `[debian-]<suite>-<arch>-…` or `ubuntu-<version>[-arm]-…`, goes to
    its target's APT repository.
  - An RPM, `[fedora-|rockylinux-]pam-rssh…<dist>.<arch>.rpm`, goes to
    `pam-rssh-rpm-hosted`, the hosted repository behind `pam-rssh-rpm`, under
    its name without that prefix.
- It uploads what Nexus lacks, compared by SHA-256, after checking each download
  against the digest GitHub reports for it.
- An asset it has no place for, such as a new target's, marks the build
  unstable. That target needs its repositories in homelab first.
- `DRY_RUN` lists what a run would upload, without uploading.
