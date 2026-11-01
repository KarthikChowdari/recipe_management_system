# Jenkins Setup Guide

This guide sets up a Jenkins controller that can build, test, containerise and deploy the
Recipe Management System on **Linux** or **Windows**.

| Document                                      | Contents                                              |
|-----------------------------------------------|-------------------------------------------------------|
| `README.md` (this file)                       | Installing Jenkins, plugins, tools, credentials       |
| [`MAVEN_BUILD_GUIDE.md`](MAVEN_BUILD_GUIDE.md) | Freestyle job and Pipeline job for `mvn clean package` + archiving the JAR |
| [`plugins.txt`](plugins.txt)                  | Plugin list for scripted/Docker installs              |
| [`docker-compose.yml`](docker-compose.yml)    | Optional: run Jenkins itself in Docker                |
| [`../Jenkinsfile`](../Jenkinsfile)            | The pipeline definition used by the Pipeline job      |

---

## 1. Requirements on the Jenkins node

| Tool          | Version | Needed for                          | Check                  |
|---------------|---------|-------------------------------------|------------------------|
| Java (JDK)    | 21      | Jenkins itself and the build        | `java -version`        |
| Maven         | 3.9+    | Build (or use the repo's `mvnw`)    | `mvn -v`               |
| Git           | 2.x     | Checkout                            | `git --version`        |
| Google Chrome | current | Selenium stage (headless)           | `google-chrome --version` / `chrome.exe --version` |
| Docker        | 24+     | Docker build / push / deploy stages | `docker version`       |

Memory: 4 GB RAM minimum (Spring Boot tests + headless Chrome), 10 GB free disk.

---

## 2. Install Jenkins

### 2.1 Linux (Ubuntu/Debian)

```bash
# JDK 21
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk git maven

# Jenkins LTS repository
sudo wget -O /etc/apt/keyrings/jenkins-keyring.asc https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" \
  | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update
sudo apt-get install -y jenkins
sudo systemctl enable --now jenkins
sudo systemctl status jenkins

# Chrome for the Selenium stage
wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo apt-get install -y ./google-chrome-stable_current_amd64.deb

# Docker, and allow the jenkins user to use it
sudo apt-get install -y docker.io
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

RHEL/Rocky: use `https://pkg.jenkins.io/redhat-stable` with `dnf install jenkins java-21-openjdk`.

Initial admin password:

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

### 2.2 Windows

1. Install **Temurin JDK 21** (MSI) and tick *Set JAVA_HOME*.
2. Install **Git for Windows** and **Maven** (unzip to `C:\tools\maven`, add `bin` to `PATH`).
3. Download the **Jenkins LTS MSI** from <https://www.jenkins.io/download/> and run it.
   - Run the service as a **local/domain user** (not *LocalSystem*) if the pipeline must use
     Docker Desktop — Docker Desktop's engine is only reachable by users in the `docker-users` group.
   - Choose port `8080` (or another free port, e.g. `8081`, if the app will use 8080).
   - Select the JDK 21 installation when prompted.
4. Install **Google Chrome** and **Docker Desktop** (enable the WSL 2 backend).
   Add the Jenkins service account to the local `docker-users` group, then restart the service:
   ```powershell
   Add-LocalGroupMember -Group "docker-users" -Member "<jenkins-service-user>"
   Restart-Service jenkins
   ```
5. Initial admin password: `C:\ProgramData\Jenkins\.jenkins\secrets\initialAdminPassword`.

The root `Jenkinsfile` detects the OS with `isUnix()` and uses `sh` on Linux and `bat` on
Windows, so the same pipeline runs on both.

### 2.3 Docker (optional)

```bash
cd jenkins
docker compose up -d
docker compose logs -f jenkins   # copy the initial admin password
```

See [`docker-compose.yml`](docker-compose.yml). The image installs the plugins from
`plugins.txt`; the host Docker socket is mounted so pipelines can build images.

---

## 3. First-run configuration

1. Browse to `http://<jenkins-host>:8080`, paste the initial admin password.
2. Choose **Install suggested plugins**, then create the admin user.
3. **Manage Jenkins → Plugins → Available** and install (or use `plugins.txt`):

   | Plugin                 | Why                                            |
   |------------------------|------------------------------------------------|
   | Pipeline (workflow-aggregator) | Declarative `Jenkinsfile`              |
   | Git                    | SCM checkout                                   |
   | JUnit                  | Publish `target/surefire-reports/*.xml`        |
   | Docker Pipeline        | Docker helpers and registry credentials        |
   | Credentials Binding    | `withCredentials` for Docker Hub login         |
   | Timestamper            | Timestamps in console output                   |
   | Workspace Cleanup      | `cleanWs()`                                    |
   | Pipeline Stage View    | Stage visualisation                            |
   | Blue Ocean (optional)  | Modern pipeline UI                             |

4. **Manage Jenkins → Tools**:
   - **JDK installations** → *Add JDK* → Name: `jdk21`, `JAVA_HOME` = `/usr/lib/jvm/java-21-openjdk-amd64`
     (Linux) or `C:\Program Files\Eclipse Adoptium\jdk-21...` (Windows).
   - **Maven installations** → *Add Maven* → Name: `maven3`, either *Install automatically*
     (choose 3.9.x) or point `MAVEN_HOME` to the local install.

   The tool names `jdk21` and `maven3` are referenced by the `Jenkinsfile`'s `tools {}` block.

5. **Manage Jenkins → Credentials → System → Global → Add Credentials**:

   | Kind                  | ID                      | Used by                         |
   |-----------------------|-------------------------|---------------------------------|
   | Username with password| `dockerhub-credentials` | `Docker Push` stage             |
   | Username with password| `git-credentials`       | Checkout of a private repo      |

   For Docker Hub use an **access token**, not the account password.

---

## 4. Create the jobs

Follow [`MAVEN_BUILD_GUIDE.md`](MAVEN_BUILD_GUIDE.md):

- **Freestyle job** — simplest: build the JAR and archive it.
- **Pipeline job** — recommended: uses the root `Jenkinsfile` (build → unit tests →
  Selenium → Docker → deploy).

## 5. Triggers

| Trigger                 | How                                                                  |
|-------------------------|----------------------------------------------------------------------|
| Poll SCM                | Job → *Build Triggers* → *Poll SCM* → `H/5 * * * *`                  |
| GitHub webhook          | Install *GitHub* plugin, tick *GitHub hook trigger*, add webhook `http://<jenkins>/github-webhook/` |
| Manual with parameters  | *Build with Parameters* (port, registry, push/deploy toggles)        |

## 6. Troubleshooting

| Symptom                                           | Fix                                                                 |
|---------------------------------------------------|---------------------------------------------------------------------|
| `mvn: command not found` / `'mvn' is not recognized` | Configure the `maven3` tool or use `./mvnw` / `mvnw.cmd`         |
| `invalid target release: 21`                      | The `jdk21` tool points to an older JDK                             |
| Selenium: `SessionNotCreatedException` / Chrome not found | Install Chrome on the agent; on Linux containers also install fonts/libs (`apt-get install -y google-chrome-stable`) |
| Selenium: `DevToolsActivePort file doesn't exist` | Already handled with `--no-sandbox --disable-dev-shm-usage`; ensure `/dev/shm` ≥ 256 MB in containers |
| `permission denied ... /var/run/docker.sock`      | `sudo usermod -aG docker jenkins && sudo systemctl restart jenkins` |
| Windows: `error during connect ... docker_engine` | Docker Desktop not running, or the service user is not in `docker-users` |
| Deploy stage: port already in use                 | Change the `APP_PORT` build parameter or stop the process using it  |
| Tests pass locally but fail in Jenkins            | Check `target/screenshots/` artifact and JUnit report for the failing journey |
