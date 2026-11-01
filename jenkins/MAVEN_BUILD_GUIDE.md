# Jenkins + Maven Integration Guide

Two ways to build the Maven artifact `target/recipe-management-system.jar` in Jenkins.
Complete [`README.md`](README.md) first (Jenkins installed, `jdk21` and `maven3` tools configured).

---

## Option A — Freestyle job

Good for a quick "build and archive the JAR" job.

1. **New Item** → name `recipe-management-system-freestyle` → **Freestyle project** → OK.
2. **General**
   - Tick *Discard old builds* → keep 20 builds.
   - (Optional) *This project is parameterized* → *String parameter* `MAVEN_GOALS`,
     default `clean package`.
3. **Source Code Management** → **Git**
   - Repository URL: your repository URL (or `file:///path/to/recipe_management_system` for a local repo)
   - Credentials: `git-credentials` if private
   - Branch Specifier: `*/develop` (or `*/main`)
4. **Build Triggers** → *Poll SCM* → `H/5 * * * *` (or a GitHub webhook).
5. **Build Environment** → tick *Delete workspace before build starts* and *Add timestamps*.
6. **Build Steps** → **Invoke top-level Maven targets**
   - Maven Version: `maven3`
   - Goals: `-B clean package` (or `-B ${MAVEN_GOALS}`)
   - Advanced → JVM options / Properties: none required

   *Alternative without the Maven plugin:*
   - Linux: **Execute shell** → `./mvnw -B clean package`
   - Windows: **Execute Windows batch command** → `mvnw.cmd -B clean package`

   `clean package` also runs the unit/integration tests (Selenium tests are excluded by
   default; add `-Pselenium` in a separate step to run them).

7. **Post-build Actions**
   - **Publish JUnit test result report** → Test report XMLs: `target/surefire-reports/*.xml`
   - **Archive the artifacts** → Files to archive: `target/*.jar` → Advanced → tick
     *Fingerprint all archived artifacts* and *Archive artifacts only if build is successful*.
8. **Save** → **Build Now**.

Result: each successful build page shows `recipe-management-system.jar` under
*Build Artifacts*, and the *Test Result* trend chart.

Download the latest successful JAR:

```
http://<jenkins>/job/recipe-management-system-freestyle/lastSuccessfulBuild/artifact/target/recipe-management-system.jar
```

---

## Option B — Pipeline job (recommended)

Uses the declarative [`Jenkinsfile`](../Jenkinsfile) in the repository root.

1. **New Item** → name `recipe-management-system` → **Pipeline** → OK.
2. **General** → *Discard old builds* (keep 20). Parameters are declared in the
   `Jenkinsfile`; they appear after the first run.
3. **Build Triggers** → *Poll SCM* `H/5 * * * *` or GitHub webhook.
4. **Pipeline**
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**, repository URL + credentials
   - Branches to build: `*/develop` (or `*/main`)
   - Script Path: `Jenkinsfile`
   - Tick *Lightweight checkout*
5. **Save** → **Build Now**. The first run registers the parameters; then use
   **Build with Parameters**.

For branch-per-PR builds create a **Multibranch Pipeline** instead and point it at the
repository; every branch with a `Jenkinsfile` gets its own job.

### Pipeline stages

| Stage            | Command (Linux / Windows)                                      | Output                              |
|------------------|----------------------------------------------------------------|-------------------------------------|
| Checkout         | `checkout scm`                                                 | Workspace                           |
| Build & Package  | `mvn -B clean package -DskipTests`                             | `target/recipe-management-system.jar` (archived) |
| Run Unit Tests   | `mvn -B test`                                                  | JUnit report                        |
| Selenium Test    | `mvn -B test -Pselenium`                                       | JUnit report + `target/screenshots/**` |
| Docker Build     | `docker build -t recipe-management-system:${BUILD_NUMBER} .`   | Versioned image                     |
| Docker Push      | `docker push <registry>/recipe-management-system:${BUILD_NUMBER}` | Image in registry                |
| Deploy           | `docker run -d -p ${APP_PORT}:8080 ...` or `java -jar` on the agent | Running app + health check     |

### Parameters

| Parameter         | Default                     | Meaning                                           |
|-------------------|-----------------------------|---------------------------------------------------|
| `APP_PORT`        | `8085`                      | Host port the deployed app listens on             |
| `DEPLOY_MODE`     | `docker`                    | `docker` (container) or `jar` (process on the agent) |
| `RUN_SELENIUM`    | `true`                      | Run the Selenium stage                            |
| `PUSH_IMAGE`      | `false`                     | Push the image to the registry                    |
| `DOCKER_REGISTRY` | `docker.io/<your-user>`     | Registry/namespace, e.g. `localhost:5000`         |

### Archiving the JAR in a pipeline

```groovy
post {
    success {
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
    }
}
```

### Minimal pipeline (build + archive only)

If you only need the Maven artifact, this is the smallest useful `Jenkinsfile`:

```groovy
pipeline {
    agent any
    tools { jdk 'jdk21'; maven 'maven3' }
    stages {
        stage('Checkout') { steps { checkout scm } }
        stage('Build & Package') {
            steps {
                script {
                    if (isUnix()) { sh 'mvn -B clean package' } else { bat 'mvn -B clean package' }
                }
            }
        }
    }
    post {
        always  { junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml' }
        success { archiveArtifacts artifacts: 'target/*.jar', fingerprint: true }
    }
}
```

---

## Verifying a build locally (same commands Jenkins runs)

```bash
./mvnw -B clean package -DskipTests
./mvnw -B test
./mvnw -B test -Pselenium
ls -l target/recipe-management-system.jar
java -jar target/recipe-management-system.jar --server.port=8085
curl http://localhost:8085/actuator/health
```
