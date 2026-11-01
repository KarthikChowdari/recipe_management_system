# Rollback Guide

Every release is an immutable Docker image tagged with the Jenkins build number
(`recipe-management-system:<BUILD_NUMBER>`). Rolling back means running an earlier tag
again. There are three layers of protection:

| Layer | Where                         | Trigger                       | What happens                                           |
|-------|-------------------------------|-------------------------------|--------------------------------------------------------|
| 1     | Jenkins `Deploy` stage        | New container not healthy     | `scripts/deploy-docker.sh` restarts the previous image, build fails |
| 2     | Ansible `tasks/deploy.yml`    | Health check fails on a host  | `rescue` reactivates the previous stable release, rollout stops |
| 3     | Manual (Ansible or Docker)    | Problem found after release   | Operator rolls back to the previous or a chosen release |

Tests are the layer before all of these: a failing unit or Selenium test stops the pipeline
before an image is built, pushed or deployed.

---

## How releases are tracked

On every Ansible-managed host:

```
/opt/recipe-app/releases/stable       version currently known to be healthy, e.g. 43
/opt/recipe-app/releases/previous     version that was stable before it, e.g. 42
/opt/recipe-app/releases/history.log  2026-11-01T10:15:02Z DEPLOY 42 -> 43 OK
                                      2026-11-01T11:02:40Z AUTO-ROLLBACK 44 -> 43
/etc/recipe-app/release.env           APP_IMAGE / APP_VERSION the systemd unit runs
```

The newest `keep_images` (default 5) release images stay on each host so a rollback works
even if the registry is unreachable.

Health is defined as: `GET /actuator/health` returns `{"status":"UP"}`, `GET /login`
returns 200, and the running container's image equals the expected release.

---

## 1. Automatic rollback during an Ansible deploy

Nothing to do — just read the output:

```
TASK [Report the failed release]
  msg: Release 44 failed on prod-app-01 (...). Rolling back to 43.
TASK [Reactivate the previous stable release] ...
TASK [Fail the play after a successful rollback]
  msg: Deployment of 44 failed its health check; 43 was restored and is healthy.
```

Because of `serial: 1` and `max_fail_percentage: 0`, hosts after the failing one were never
touched. Fix the problem, build a new release (45) and deploy again.

## 2. Manual rollback with Ansible (preferred)

```bash
cd ansible

# Previous stable release on every host (one host at a time)
ansible-playbook playbook.yml --tags rollback

# Same, with the dedicated playbook
ansible-playbook rollback.yml

# A specific release
ansible-playbook rollback.yml -e rollback_version=41

# Only one environment / host
ansible-playbook rollback.yml --limit staging
ansible-playbook rollback.yml --limit prod-app-01 -e rollback_version=42
```

What it does: reads `stable`/`previous`, pulls the target image if missing, rewrites
`release.env`, restarts `recipe-app.service`, runs the health check, then swaps the markers
(`previous` ← old stable, `stable` ← rollback version) and logs `ROLLBACK` in `history.log`.
Running it twice is safe: the second run finds the release already active and changes nothing.

## 3. Rollback from Jenkins

Re-run the pipeline for an older commit, or — faster — deploy an existing image without
rebuilding. On the agent:

```bash
scripts/deploy-docker.sh recipe-management-system:42 8085 recipe-app
```

Or in Jenkins: *Build with Parameters* on a build whose image still exists, with
`RUN_SELENIUM=false` only if that build already passed its tests.

## 4. Manual rollback with plain Docker (emergency)

```bash
docker images recipe-management-system                # find available tags
docker inspect --format '{{.Config.Image}}' recipe-app  # what is running now
docker rm -f recipe-app
docker run -d --name recipe-app -p 8080:8080 --restart unless-stopped recipe-management-system:42
docker inspect --format '{{.State.Health.Status}}' recipe-app   # wait for "healthy"
```

On an Ansible-managed host prefer editing through systemd so the change survives reboots:

```bash
sudo sed -i 's/^APP_IMAGE=.*/APP_IMAGE=docker.io\/<user>\/recipe-management-system:42/; s/^APP_VERSION=.*/APP_VERSION=42/' /etc/recipe-app/release.env
sudo systemctl restart recipe-app
curl -s http://localhost:8080/actuator/health
echo 42 | sudo tee /opt/recipe-app/releases/stable
```

Then run `ansible-playbook rollback.yml -e rollback_version=42` at the next opportunity so
the markers and history are consistent.

## 5. JAR deployments (DEPLOY_MODE=jar)

`scripts/deploy-jar.sh` / `deploy-jar.ps1` keep the previous JAR as
`<deploy-dir>/app-previous.jar`:

```bash
cd ~/recipe-deploy            # or %JENKINS_HOME%\recipe-deploy
kill "$(cat app.pid)"
cp app-previous.jar app.jar
nohup java -jar app.jar --server.port=8085 > app.log 2>&1 & echo $! > app.pid
```

## 6. Database considerations

- The default H2 database is in-memory: a rollback starts with fresh seed data.
- With MySQL (`ddl-auto: update`) schema changes are additive, so an older release can run
  against a newer schema. Before introducing destructive schema changes, adopt a migration
  tool (Flyway/Liquibase) with expand/contract migrations and take a backup before deploying:
  `mysqldump -u root -p recipes > recipes-before-<version>.sql`.

## 7. After a rollback

1. Open an issue with the bug template; attach `history.log`, `journalctl -u recipe-app`
   output and the failing Jenkins build link.
2. Add a test that reproduces the failure (unit or Selenium).
3. Ship the fix as a new build number — never re-tag an old image as "fixed".

## Verification log

The automatic rollback path of `scripts/deploy-docker.sh` was exercised locally: a
deliberately broken image (invalid JVM option) was deployed over a healthy container; the
script detected the failure, restored the previous image, confirmed it healthy and exited
with status 1 so the pipeline fails.
