# Reviewer guide

This is a short route through existing EMS documentation and executable examples, not a replacement for the setup runbook.

## Prerequisites and verification paths

| Path | Prerequisites | What it demonstrates |
|---|---|---|
| Source review | Browser or checkout | Architecture, API contracts, tests and CI configuration; no runtime claim |
| Fast verification | Java 17, Git Bash/Bash and the included Maven Wrapper; dependency downloads require network access | Default tests, applicable H2 persistence tests, packaging and configured quality checks; no Docker required |
| Runtime walkthrough | Configured running stack, its databases and a provisioned ADMIN or HR account | HTTP behavior against live services |
| Hosted Compose smoke check | GitHub Actions runner with Docker and Python 3 | Disposable full-stack registration/login, role checks, CRUD, self-service, search and log-only notification assertions |

For fast verification, run from the repository root:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

PowerShell users can invoke `.\mvnw.cmd` instead. See [test layers and reports](../README.md#build-and-test). A fast build does not execute the MySQL Testcontainers profile or start the application stack.

For runtime setup, follow [secure configuration](../README.md#secure-local-configuration) and [Docker Compose deployment](../README.md#docker-compose-deployment). Use the documented health checks before the API journey. If Docker is unavailable locally, use fast verification and inspect the [hosted workflow results](https://github.com/amit-vishwa/employee-management-system/actions/workflows/ci.yml) for MySQL and Compose execution.

## Login, create and search

The sequence below is code-reviewed against the controllers and [existing smoke script](../scripts/compose-smoke.py). It has not been rerun locally for this documentation change because Docker is unavailable. Expected responses below are contracts, not a new execution report.

For a live walkthrough, use the [Swagger UI endpoints](../README.md#api-documentation) on a running disposable stack. You need an already provisioned ADMIN or HR account. Public registration creates EMPLOYEE accounts and cannot grant management access. The CI script creates and temporarily promotes its own disposable HR user inside its isolated database; it is **CI-only**, not a setup command for a shared or persistent environment.

| Step | Request | Expected result |
|---|---|---|
| Log in | Auth service `POST /api/v1/auth/login`, with `username` and `password` for the provisioned HR/ADMIN account | `200`, JSON containing `token` |
| Authorize | In employee-service Swagger UI, select **Authorize** and paste the JWT without the `Bearer` prefix | Subsequent protected requests use that identity |
| Create department | Employee service `POST /api/v1/departments` with `{"name":"Reviewer Engineering"}` | `201`, JSON containing the generated department `id` |
| Create employee | `POST /api/v1/employees` with the payload below and the returned department ID | `201`, JSON containing the generated employee `id` |
| Search | `GET /api/v1/employees/search?criteria=designation&value=Reviewer%20Engineer` | `200`, a JSON array containing the created employee |

Use a unique department name and employee email for each run. Omit IDs from create payloads. Replace `departmentId` with the ID returned by the department request:

```json
{
  "firstName": "Reviewer",
  "lastName": "Example",
  "email": "reviewer-unique@example.invalid",
  "designation": "Reviewer Engineer",
  "dateOfJoining": "2026-01-01",
  "departmentId": 1
}
```

`authUsername` is optional for this management journey. The smoke script additionally registers an EMPLOYEE account, links it through `authUsername` and checks `/api/v1/employees/me`. An EMPLOYEE JWT cannot create departments or employees: expect `403`; a missing JWT produces `401`.

Use Swagger DELETE operations to clean up only records created for this walkthrough: delete the employee first, then the empty department. A populated department returns `409` on deletion. Do not share JWTs, passwords or resolved secret configuration in screenshots or issues.

## Existing executable evidence

The [Compose smoke script](../scripts/compose-smoke.py) asserts status codes, returned IDs, search membership and linked self-service behavior. The [CI workflow](../.github/workflows/ci.yml) invokes it after starting a disposable Compose stack. Inspect the **Docker Image Build** job in a selected workflow run for actual smoke results; inspect **Maven Verify** for MySQL integration results. These are different verification layers.

Employee-created notifications are synchronous, best-effort calls after commit. The notification service logs intended delivery; it does not send email. The smoke check examines that log path and does not demonstrate durable messaging or delivery guarantees. See [notification behavior](../README.md#important-limitations) and the full runbook for constraints.
