---
name: Bug report
about: Report something that is broken or behaves unexpectedly
title: "[BUG] "
labels: bug, triage
assignees: ''
---

## Summary
A clear, one-sentence description of the bug.

## Steps to reproduce
1. Log in as `...` (author / reviewer / admin)
2. Go to `...`
3. Click `...`
4. See error

## Expected behaviour
What you expected to happen.

## Actual behaviour
What actually happened. Include the HTTP status and error message if relevant.

## Evidence
- Screenshots (Selenium failures are saved under `target/screenshots/`)
- Relevant log lines (`docker logs recipe-app` or console output)

## Environment
- Version / image tag: (e.g. `recipe-management-system:42`)
- Run mode: [ ] `mvn spring-boot:run`  [ ] JAR  [ ] Docker  [ ] Ansible-deployed
- Database: [ ] H2  [ ] MySQL
- Browser + version (UI bugs):
- OS:

## Severity
- [ ] Critical – data loss / app down
- [ ] Major – feature unusable, no workaround
- [ ] Minor – workaround exists
- [ ] Cosmetic
