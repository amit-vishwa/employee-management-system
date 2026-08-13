# Employee Management System

[![CI](https://github.com/amit-vishwa/employee-management-system/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/amit-vishwa/employee-management-system/actions/workflows/ci.yml)

A Spring Boot microservices learning project demonstrating employee and department management, JWT authentication, role-based authorization, service-to-service communication, automated testing, static analysis, and Docker-based local orchestration.

## Services

| Service | Port | Responsibility |
|---|---:|---|
| employee-service | 8081 | Employee and department management |
| auth-service | 8082 | Registration, authentication, and JWT generation |
| notification-service | 8083 | Employee-created notifications |

The project uses separate MySQL databases for employee and authentication data.

## Prerequisites

The required toolchain is available without paid software:

- OpenJDK 17
- Maven Wrapper included in the repository
- Git
- Docker Engine, an eligible Docker Desktop installation, or a compatible free container runtime
- OpenSSL for generating local secrets

## Build and Test

Run the complete Maven reactor:

```bash
./mvnw clean verify
```

The Maven `verify` lifecycle performs:

- Compilation and packaging for the complete multi-module reactor
- 53 automated tests across the four code modules
- JaCoCo coverage-report generation
- SpotBugs static analysis
- Build failure for medium-or-higher-confidence SpotBugs findings

Tests use isolated H2-backed test configuration. They do not require local MySQL, Docker, or production credentials.

Local reports are generated under each module:

```text
<module>/target/site/jacoco/index.html
<module>/target/site/jacoco/jacoco.xml
<module>/target/spotbugsXml.xml
```

JaCoCo currently provides an honest coverage baseline rather than enforcing an arbitrary global percentage. The initial employee-service baseline is:

- Instruction coverage: 68%
- Branch coverage: 52%
- Service implementation instruction coverage: 94%
- Service implementation branch coverage: 83%

The baseline shows that core business services have strong focused coverage while controller, security, mapper, strategy, and framework-wiring coverage still has room for improvement.

## Continuous Integration

GitHub Actions runs on:

- Pushes to `master`
- Pull requests targeting `master`
- Manual workflow dispatch

The pipeline executes two ordered jobs.

### 1. Maven Verify

The first job:

- Uses Eclipse Temurin Java 17
- Runs the complete Maven reactor
- Executes all automated tests
- Generates JaCoCo coverage reports
- Runs the SpotBugs quality gate
- Uploads `jacoco-reports`
- Uploads `spotbugs-reports`
- Retains report artifacts for seven days

### 2. Docker Image Build

The second job:

- Runs only after Maven verification succeeds
- Creates a temporary CI environment file from `.env.example`
- Validates the Docker Compose model
- Builds the `auth-service` image
- Builds the `employee-service` image
- Builds the `notification-service` image
- Does not start containers
- Does not publish images to a registry

The dependency between the jobs ensures that Docker resources are not spent building code that has already failed compilation, tests, coverage generation, or static analysis.

## Dependency Update Automation

Dependabot checks the repository weekly for:

- Maven dependency and plugin updates
- GitHub Actions updates

Maven minor and patch updates are grouped to reduce pull-request noise. Major updates remain separate because they may require migration work.

GitHub Actions updates are grouped under the same CI concern.

Dependabot never merges updates automatically. Every proposed update must be reviewed and pass the complete CI pipeline.

No paid GitHub Advanced Security feature is required for this workflow.

## Secure Local Configuration

Production credentials and JWT signing material are not stored in Git.

Copy the safe configuration template:

```bash
cp .env.example .env
```

Generate a separate value for every password and for the JWT signing secret:

```bash
openssl rand -hex 32
```

Replace every `CHANGE_ME` value in `.env`.

Never commit `.env`. It is intentionally excluded through `.gitignore`.

Validate the Compose configuration without printing resolved credentials:

```bash
docker compose config --quiet
```

## Docker Runtime Operations

Build the service images:

```bash
docker compose build
```

Start the complete stack and wait until every service is healthy:

```bash
docker compose up -d --wait --wait-timeout 300
```

Check container status:

```bash
docker compose ps
```

The expected healthy stack contains:

- `mysql-ems`
- `mysql-auth`
- `auth-service`
- `employee-service`
- `notification-service`

Verify the public health endpoints:

```bash
curl --fail http://localhost:8081/actuator/health
curl --fail http://localhost:8082/actuator/health
curl --fail http://localhost:8083/actuator/health
```

Each endpoint should return:

```json
{"status":"UP"}
```

Health endpoints are public so Docker can call them. Employee and department business APIs remain JWT-protected. Authentication endpoints are intentionally public, while notification-service currently exposes an unauthenticated internal endpoint as a known limitation.

Stop all containers while preserving database data:

```bash
docker compose down
```

## Logs and Troubleshooting

View the status of running and failed containers:

```bash
docker compose ps --all
```

View logs for all services:

```bash
docker compose logs --tail=200
```

View logs for a specific service:

```bash
docker compose logs --tail=200 employee-service
docker compose logs --tail=200 auth-service
docker compose logs --tail=200 notification-service
```

Follow logs in real time:

```bash
docker compose logs --follow employee-service
```

### Docker daemon is unavailable

An error containing the following text means the Docker engine is not running:

```text
failed to connect to the docker API
```

Start Docker Desktop or the selected compatible container engine, wait until it is ready, and rerun the command.

### Maven reports missing child modules during an image build

Every service Dockerfile must copy the root POM and all module POMs before Maven constructs the reactor. Source code is copied only for the modules required by that image.

This preserves Docker dependency-layer caching while allowing Maven to validate the complete multi-module project.

### Container reports `no main manifest attribute`

The service JAR was packaged as a regular Maven JAR rather than an executable Spring Boot JAR.

The parent Maven configuration binds the Spring Boot `repackage` goal for service modules. A valid executable service JAR contains `BOOT-INF/` entries and can run with:

```bash
java -jar app.jar
```

### Container is unhealthy

Inspect status and application logs:

```bash
docker compose ps --all
docker compose logs --tail=200 <service-name>
```

Database containers may take longer during first-time initialization. Use the Compose wait option rather than assuming that a started container is ready.

### Existing-volume credentials

The official MySQL image processes `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_ROOT_PASSWORD` only when initializing an empty data directory.

Changing values in `.env` does not update users or passwords stored in an existing volume. Rotate credentials through MySQL administration or deliberately recreate disposable local volumes.

### SpotBugs fails the Maven build

SpotBugs findings appear near the end of the failing module’s Maven output.

Do not immediately suppress the finding or reduce the configured threshold. First determine whether it represents:

- A real correctness defect
- A security or portability risk
- Generated-code noise
- A justified false positive

For example, SpotBugs identified that JWT signing used `String.getBytes()` without an explicit encoding. The implementation was corrected to use UTF-8 so signing keys remain deterministic across Windows and Linux.

### Coverage reports are missing

JaCoCo reports are generated during the Maven `verify` phase.

Use:

```bash
./mvnw clean verify
```

Running only `test` may execute tests without completing every configured reporting step.

The root project is a POM-only aggregator, so it does not produce a meaningful code-coverage report. Reports are expected in the four code modules.

## Resetting Local Database Data

To remove containers and permanently delete both local database volumes:

```bash
docker compose down --volumes
```

This deletes all locally stored:

- Authentication users
- Employees
- Departments
- Database schemas and data

Use this only when the local data is disposable or has been backed up.

The next startup recreates the databases and dedicated application users:

```bash
docker compose up -d --build --wait --wait-timeout 300
```

Never use volume deletion as a routine fix for an unexplained startup problem. Inspect status and logs first.

## Running Services Directly

Docker Compose automatically maps service-specific credentials to the generic Spring properties `DB_USERNAME` and `DB_PASSWORD`.

When starting a service directly from an IDE or terminal, configure these variables explicitly.

For `auth-service`:

```text
DB_HOST=localhost
DB_PORT=3308
DB_USERNAME=<value of AUTH_DB_USERNAME>
DB_PASSWORD=<value of AUTH_DB_PASSWORD>
JWT_SECRET=<local JWT secret>
```

For `employee-service`:

```text
DB_HOST=localhost
DB_PORT=3307
DB_USERNAME=<value of EMS_DB_USERNAME>
DB_PASSWORD=<value of EMS_DB_PASSWORD>
JWT_SECRET=<same local JWT secret used by auth-service>
NOTIFICATION_HOST=localhost
```

Both services must use the same JWT secret: auth-service signs tokens and employee-service verifies them.

## Configuration Model

| Environment | Configuration source |
|---|---|
| Automated tests | Test-profile YAML and H2 |
| Local Docker | Ignored `.env` file |
| Direct IDE execution | IDE environment variables |
| Deployment | Runtime environment variables or platform secrets |

Required configuration fails fast when missing. The application does not contain fallback production passwords or a fallback JWT signing key.

## API Documentation

Each HTTP service generates an independent OpenAPI specification and Swagger UI.

| Service | OpenAPI JSON | Swagger UI |
|---|---|---|
| employee-service | http://localhost:8081/v3/api-docs | http://localhost:8081/swagger-ui.html |
| auth-service | http://localhost:8082/v3/api-docs | http://localhost:8082/swagger-ui.html |
| notification-service | http://localhost:8083/v3/api-docs | http://localhost:8083/swagger-ui.html |

Employee-service Swagger UI defines a `bearerAuth` security scheme.

To call a protected endpoint:

1. Register and log in through auth-service.
2. Copy the returned JWT without the `Bearer` prefix.
3. Select **Authorize** in employee-service Swagger UI.
4. Enter the JWT.
5. Swagger UI adds the `Bearer` prefix automatically.

Public registration creates an `EMPLOYEE` account. That token can access only `GET /api/v1/employees/me`, and only after an ADMIN or HR links an employee record through `authUsername`.

Employee and department management operations require an ADMIN or HR token.

The generated contract documents operation summaries, validation responses, authorization failures, resource-not-found responses, and uniqueness conflicts.

## Authorization Model

| Role | Employee permissions | Department permissions |
|---|---|---|
| ADMIN | Full management | Full management |
| HR | Full management | Full management |
| EMPLOYEE | Read the explicitly linked record through `GET /api/v1/employees/me` | None |

Employee ownership is represented by:

```text
JWT subject username <-> employee.authUsername
```

The employee service stores only the external authentication username. It does not share the auth-service user entity or database.

An EMPLOYEE cannot:

- List all employees
- Access an employee by numeric ID
- Create, update, or delete employees
- Access department endpoints

Existing employee records may have no authentication username. This allows employee records and login accounts to remain separate concepts.

## API Error Contract

Handled authentication and employee-domain errors use this standard response structure:

```json
{
  "timestamp": "2026-08-12T12:00:00",
  "status": 409,
  "message": "Employee email already exists: employee@example.com",
  "path": "/api/v1/employees"
}
```

Common status codes:

| Status | Meaning |
|---:|---|
| 400 | Request validation failed or an unsupported criterion was supplied |
| 401 | Authentication credentials were rejected by an auth-service operation |
| 403 | A JWT is missing or invalid, or the authenticated role is not permitted |
| 404 | The requested or linked resource was not found |
| 409 | A unique username, employee email, or employee ownership link already exists |
| 500 | An unexpected server error occurred; complete cross-service normalization remains pending |

Database uniqueness constraints remain the final protection against concurrent requests. Application-level checks provide clearer errors during normal execution.

## Important API Limitations

- Public registration always creates an `EMPLOYEE` account.
- No production administrative account-provisioning API exists yet.
- Local smoke tests may promote a disposable account directly in the local database.
- Notification-service currently logs intended email activity; it does not send real email.
- Employee-to-auth ownership is linked by username rather than a cross-database foreign key.
- Database schema evolution currently relies on Hibernate `ddl-auto`; versioned migrations remain pending.
- Notification-service is intended for internal service-to-service use but currently has no authentication or network-level access restriction.
- JWT invalid or missing-token responses currently return HTTP 403 from employee-service; consistent HTTP 401 authentication-entry-point handling remains a future improvement.
- Container images are verified in CI but are not published to a registry.

## Security Rules

- Never commit `.env`.
- Never place real credentials in `.env.example`.
- Never commit JWT tokens or generated signing secrets.
- Use different root and application database passwords.
- Applications connect through dedicated database users rather than MySQL root.
- Rotate a credential immediately if it is printed, shared, or committed.
- Keep the JWT signing secret identical across token-producing and token-validating services.
- JWT secret text is converted to key bytes using explicit UTF-8 encoding for deterministic behavior across operating systems.
- Dependabot pull requests must pass CI before they are merged.