# Ansible: provisioning, deployment and rollback

Ansible prepares target servers and deploys versioned Docker images of the Recipe
Management System, with a health check after every change and automatic rollback.

```
ansible/
├── ansible.cfg              defaults (inventory, sudo, SSH pipelining)
├── inventory.ini            target nodes: staging, production, local
├── requirements.yml         collections (community.general, ansible.posix)
├── group_vars/all.yml       every tunable variable, documented
├── playbook.yml             provision + deploy (tags below)
├── rollback.yml             stand-alone rollback playbook
├── tasks/
│   ├── install_jdk.yml      OpenJDK 21 (apt / dnf)
│   ├── install_docker.yml   Docker engine, service enabled
│   ├── setup_app.yml        user, /opt/recipe-app, /etc/recipe-app, env file, systemd unit, firewall
│   ├── deploy.yml           pull → activate → health check → record; rescue = auto-rollback
│   ├── activate_release.yml point unit at an image, restart only on change, health check
│   ├── health_check.yml     /actuator/health = UP, /login = 200, running image = expected
│   ├── rollback.yml         manual rollback to previous/requested release
│   └── prune_images.yml     keep the newest N release images
└── templates/
    ├── recipe-app.service.j2  systemd unit that runs the container
    ├── app.env.j2             container environment (0600, contains secrets)
    └── release.env.j2         which image/version/port the unit runs
```

## What gets configured on each node

| Item                | Location / value                                                   |
|---------------------|--------------------------------------------------------------------|
| Packages            | `openjdk-21-jre-headless` (or `java-21-openjdk-headless`), Docker  |
| Service account     | `recipeapp` (system user, no login shell)                          |
| Application folders | `/opt/recipe-app/{releases,logs}`                                  |
| Configuration       | `/etc/recipe-app/app.env` (0600), `/etc/recipe-app/release.env`    |
| systemd service     | `recipe-app.service` → `docker run ... -p <app_port>:8080 <image>` |
| Release markers     | `/opt/recipe-app/releases/{stable,previous,history.log}`           |
| Port                | `app_port` (staging 8080, production 80 in the sample inventory)   |

## Prerequisites

- Control node: Linux, macOS or WSL with `ansible-core` ≥ 2.15 (Ansible does not run natively on Windows).
- Targets: Ubuntu 22.04/24.04, Debian 12, or RHEL/Rocky/Alma 9; SSH access with sudo.
- The image `app_image_repo:app_version` must be pullable from the targets (Docker Hub,
  a private registry, or already present locally).

```bash
python3 -m pip install --user ansible-core
cd ansible
ansible-galaxy collection install -r requirements.yml
ansible all -m ping                     # connectivity check
```

## Usage

```bash
# 1. Full provisioning + first deploy of build 42
ansible-playbook playbook.yml -e app_version=42 \
  -e app_image_repo=docker.io/<user>/recipe-management-system

# 2. Deploy a new release only (idempotent; restarts only if something changed)
ansible-playbook playbook.yml --tags deploy -e app_version=43

# 3. Staging only, or one host
ansible-playbook playbook.yml --limit staging -e app_version=43
ansible-playbook playbook.yml --limit prod-app-01 -e app_version=43

# 4. Deploy to the machine running Ansible
ansible-playbook playbook.yml -e target_hosts=local -e app_version=43

# 5. Health check only
ansible-playbook playbook.yml --tags health

# 6. Rollback (see ../ROLLBACK.md)
ansible-playbook playbook.yml --tags rollback
ansible-playbook rollback.yml -e rollback_version=41

# Dry run / diff
ansible-playbook playbook.yml --check --diff -e app_version=43
```

### Tags

| Tag         | Runs                                                        |
|-------------|-------------------------------------------------------------|
| `provision` | `jdk`, `docker`, `config` (packages, user, dirs, env, unit) |
| `deploy`    | pull image, activate, health check, record release, prune   |
| `health`    | stand-alone health check (only when requested)              |
| `rollback`  | manual rollback (only when requested)                       |
| `cleanup`   | prune old images                                            |

## Idempotency

Re-running the playbook with the same `app_version` changes nothing:

- packages, user, folders, templates use declarative modules (`apt`, `user`, `file`, `template`);
- the service is restarted **only** when `release.env`, `app.env` or the unit file changed;
- `docker pull` reports *changed* only when a newer image was downloaded;
- health checks and version reads are `changed_when: false`;
- release markers are rewritten only when the version actually changes.

A second run should therefore end with `changed=0`.

## Rolling deployment and automatic rollback

- `serial: 1` (override with `-e deploy_serial=2` or `-e deploy_serial=50%`) deploys one host
  at a time; `max_fail_percentage: 0` stops the rollout at the first failing host, so the
  remaining hosts keep running the old release.
- In `tasks/deploy.yml`, the activation + health check run in a `block`. If anything fails,
  `rescue` prints the container logs, reactivates the previous stable release, verifies its
  health, writes `AUTO-ROLLBACK` to `history.log` and fails the play.

## Secrets

`group_vars/all.yml` contains demo values only. For real environments create an encrypted
vars file and never commit plain-text secrets:

```bash
mkdir -p group_vars/production
ansible-vault create group_vars/production/vault.yml
#   app_admin_password: "..."
#   app_db_password: "..."
#   registry_password: "..."
ansible-playbook playbook.yml --limit production --ask-vault-pass -e app_version=43
```

## Calling Ansible from Jenkins

After the `Docker Push` stage, a deploy to remote servers is one command:

```groovy
stage('Ansible Deploy') {
    steps {
        dir('ansible') {
            sh "ansible-playbook playbook.yml --tags deploy -e app_version=${env.BUILD_NUMBER} -e app_image_repo=${params.DOCKER_REGISTRY}/recipe-management-system"
        }
    }
}
```

The Jenkins agent needs `ansible-core`, the SSH key of the deploy user (store it as a
*SSH Username with private key* credential and wrap the step in `sshagent([...])`), and the
vault password (a *Secret file* credential passed with `--vault-password-file`).

## Validation performed

The playbooks were checked with `ansible-playbook --syntax-check` and `ansible-lint`
(production profile, 0 findings) on ansible-core 2.21. Run them against a staging host before
production; see the troubleshooting table in [`../FINAL_PROJECT_REPORT.md`](../FINAL_PROJECT_REPORT.md).
