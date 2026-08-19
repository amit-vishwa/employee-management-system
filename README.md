# Employee Management System

[![CI](https://github.com/amit-vishwa/employee-management-system/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/amit-vishwa/employee-management-system/actions/workflows/ci.yml)

A Spring Boot microservices learning project demonstrating employee and department management, JWT authentication, role-based authorization, Flyway database migrations, resilient service-to-service communication, correlation-based observability, automated testing, static analysis, and Docker-based local orchestration.

## Services

| Service | Port | Responsibility |
|---|---:|---|
| employee-service | 8081 | Employee and department management |
| auth-service | 8082 | User registration, authentication, and JWT generation |
| notification-service | 8083 | Processing employee-created notifications |

The project uses separate MySQL databases:

| Database | Host port | Used by |
|---|---:|---|
| `ems_db` | 3307 | employee-service |
| `auth_db` | 3308 | auth-service |

## Technology Stack

- Java 17
- Spring Boot 3.3
- Spring Web MVC
- Spring Data JPA
- Spring Security
- JWT authentication
- MySQL 8
- H2 for automated tests
- Flyway Community
- Resilience4j Circuit Breaker
- Maven multi-module reactor
- JUnit 5, Mockito, AssertJ, and MockMvc
- JaCoCo
- SpotBugs
- Docker and Docker Compose
- GitHub Actions
- Dependabot
- Springdoc OpenAPI and Swagger UI

All required project capabilities use free or open-source tooling. No paid service is required to build, test, or run the project locally.

## Project Modules

```text
employee-management-system/
├── common/
│   ├── shared exception contracts
│   ├── JWT utilities
│   └── correlation-ID infrastructure
├── auth-service/
│   ├── registration and login
│   ├── JWT generation
│   └── authentication database
├── employee-service/
│   ├── employee management
│   ├── department management
│   ├── role-based authorization
│   ├── employee self-service
│   └── resilient notification client
├── notification-service/
│   └── employee-created notification processing
├── .github/
│   ├── workflows/ci.yml
│   └── dependabot.yml
├── docker-compose.yml
├── .env.example
├── pom.xml
└── README.md
```

## Prerequisites

Install the following free tools:

- OpenJDK 17
- Git
- Docker Engine, an eligible Docker Desktop installation, or another compatible container runtime
- OpenSSL for generating local secrets

The Maven Wrapper is included, so a separate Maven installation is not required.

Verify the toolchain:

```bash
java -version
./mvnw --version
docker version
docker compose version
git --version
```

## Build and Test

Run the complete Maven reactor:

```bash
./mvnw clean verify
```

The Maven `verify` lifecycle performs:

- Compilation of all modules
- Execution of 57 automated tests
- Packaging of executable Spring Boot JARs
- JaCoCo coverage-report generation
- SpotBugs static analysis
- Build failure for medium-or-higher-confidence SpotBugs findings

Current test distribution:

| Module | Tests |
|---|---:|
| common | 4 |
| employee-service | 37 |
| auth-service | 13 |
| notification-service | 3 |
| **Total** | **57** |

Most tests use isolated H2-backed configuration. Dedicated Flyway tests execute versioned migrations against H2 in MySQL compatibility mode and require Hibernate to validate the resulting schemas.

Docker runtime verification remains the authoritative MySQL integration check. Maven tests do not require production credentials, paid infrastructure, or running Docker containers.

Local quality reports are generated under each module:

```text
<module>/target/site/jacoco/index.html
<module>/target/site/jacoco/jacoco.xml
<module>/target/spotbugsXml.xml
```

JaCoCo currently provides an honest coverage baseline rather than enforcing an arbitrary global percentage.

The initial employee-service baseline was:

- Instruction coverage: 68%
- Branch coverage: 52%
- Service implementation instruction coverage: 94%
- Service implementation branch coverage: 83%

This baseline shows strong focused coverage for the core business services while controller, security, mapper, strategy, and framework-wiring coverage still has room for improvement.

## Running Focused Maven Tests

When selecting a module that depends on another reactor module, include `-am`:

```bash
./mvnw \
  -pl employee-service \
  -am \
  test
```

To run one employee-service test while also building required modules:

```bash
./mvnw \
  -pl employee-service \
  -am \
  -Dtest=NotificationClientCircuitBreakerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

`-am` means “also make” required reactor dependencies, including `common`.

Without `-am`, Maven may resolve an older `common` artifact from the local Maven repository.

## Continuous Integration

GitHub Actions runs on:

- Pushes to `master`
- Pull requests targeting `master`
- Manual workflow dispatch

The pipeline executes two ordered jobs.

### Maven Verify

The first job:

- Uses Eclipse Temurin Java 17
- Runs the complete Maven reactor
- Executes all automated tests
- Generates JaCoCo reports
- Runs the SpotBugs quality gate
- Uploads `jacoco-reports`
- Uploads `spotbugs-reports`
- Retains report artifacts for seven days

### Docker Image Build

The second job:

- Runs only after Maven verification succeeds
- Creates a temporary CI environment file from `.env.example`
- Validates the Docker Compose model
- Builds auth-service
- Builds employee-service
- Builds notification-service
- Does not start containers
- Does not publish images to a registry

The job dependency avoids spending Docker resources on code that has already failed compilation, tests, coverage generation, or static analysis.

## Dependency Update Automation

Dependabot checks weekly for:

- Maven dependency and plugin updates
- GitHub Actions updates

Maven minor and patch updates are grouped to reduce pull-request noise. Major updates remain separate because they may require migration work.

GitHub Actions updates are grouped under the same CI concern.

Dependabot does not merge updates automatically. Every proposed update must be reviewed and pass the complete CI pipeline.

No paid GitHub Advanced Security capability is required.

## Secure Local Configuration

Production credentials and JWT signing material are not stored in Git.

Copy the safe template:

```bash
cp .env.example .env
```

Generate separate random values for database passwords and the JWT signing secret:

```bash
openssl rand -hex 32
```

Replace every `CHANGE_ME` value in `.env`.

Never commit `.env`. It is intentionally excluded through `.gitignore`.

Confirm:

```bash
git check-ignore -v .env
```

Validate the Compose configuration without printing resolved credentials:

```bash
docker compose config --quiet
```

## Docker Runtime Operations

Build and start the complete stack:

```bash
docker compose up -d --build --wait --wait-timeout 300
```

Use `--build` after changing:

- Java application source
- Maven dependencies
- Dockerfiles
- Shared common-module source
- Packaged configuration

`./mvnw clean verify` updates local `target` artifacts but does not modify existing Docker images.

Check status:

```bash
docker compose ps
```

The expected stack contains:

- `mysql-ems`
- `mysql-auth`
- `auth-service`
- `employee-service`
- `notification-service`

All containers should report `healthy`.

Verify public health endpoints:

```bash
curl --fail http://localhost:8081/actuator/health
curl --fail http://localhost:8082/actuator/health
curl --fail http://localhost:8083/actuator/health
```

Expected:

```json
{"status":"UP"}
```

Stop containers while preserving database data:

```bash
docker compose down
```

Delete containers and local database volumes only when the data is disposable:

```bash
docker compose down --volumes
```

## Configuration Model

| Environment | Configuration source |
|---|---|
| Automated tests | Test-profile YAML and H2 |
| Local Docker | Ignored `.env` |
| Direct IDE execution | IDE environment variables |
| Deployment | Runtime environment variables or platform secrets |

Required configuration fails fast when missing. The application does not contain fallback production passwords or a fallback JWT signing key.

## Running Services Directly

Docker Compose maps service-specific credentials to the generic Spring properties `DB_USERNAME` and `DB_PASSWORD`.

When starting a service directly from an IDE, configure the environment explicitly.

For auth-service:

```text
DB_HOST=localhost
DB_PORT=3308
DB_USERNAME=<AUTH_DB_USERNAME>
DB_PASSWORD=<AUTH_DB_PASSWORD>
JWT_SECRET=<local JWT secret>
```

For employee-service:

```text
DB_HOST=localhost
DB_PORT=3307
DB_USERNAME=<EMS_DB_USERNAME>
DB_PASSWORD=<EMS_DB_PASSWORD>
JWT_SECRET=<same secret used by auth-service>
NOTIFICATION_HOST=localhost
NOTIFICATION_CONNECT_TIMEOUT=2s
NOTIFICATION_READ_TIMEOUT=3s
```

Auth-service and employee-service must use the same JWT signing secret:

```text
auth-service signs JWTs
employee-service verifies JWTs
```

## Database Schema Management

Flyway Community manages the authentication and employee schemas.

Migration locations:

```text
auth-service/src/main/resources/db/migration/
employee-service/src/main/resources/db/migration/
```

Current baselines:

```text
auth-service:
V1__create_users_table.sql

employee-service:
V1__create_employee_schema.sql
```

Flyway executes pending migrations before Hibernate initializes.

Hibernate uses:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Hibernate validates entity-to-schema compatibility but does not create or silently modify production tables.

Flyway records migration history in:

```text
flyway_schema_history
```

Inspect auth-service migration history:

```bash
docker compose exec mysql-auth sh -c \
  'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  -e "SELECT installed_rank, version, description, success FROM flyway_schema_history;"'
```

Inspect employee-service migration history:

```bash
docker compose exec mysql-ems sh -c \
  'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  -e "SELECT installed_rank, version, description, success FROM flyway_schema_history;"'
```

An applied migration must not be edited. Future schema changes require a new version:

```text
V2__add_employee_status.sql
V3__create_audit_table.sql
```

Editing an applied migration changes its checksum and causes Flyway validation to fail.

## Transaction Boundaries and OSIV

Open Session in View is disabled:

```yaml
spring:
  jpa:
    open-in-view: false
```

Database access and entity-to-DTO mapping must complete inside the service transaction.

Service classes use:

- Class-level `@Transactional(readOnly = true)` for queries
- Method-level `@Transactional` for state-changing operations

This prevents controllers and JSON serialization from triggering unexpected lazy database queries.

## Notification Resilience

Employee creation is the primary transaction. Notification delivery is secondary.

```text
Persist employee
    ↓
Commit database transaction
    ↓
Publish EmployeeCreatedEvent
    ↓
AFTER_COMMIT listener
    ↓
NotificationClient
    ↓
Circuit breaker
    ↓
notification-service
```

Connection and read waits are bounded:

```yaml
notification:
  client:
    connect-timeout: ${NOTIFICATION_CONNECT_TIMEOUT:2s}
    read-timeout: ${NOTIFICATION_READ_TIMEOUT:3s}
```

The Resilience4j circuit breaker:

- Uses a count-based sliding window
- Records notification failures
- Opens after the configured failure threshold
- Rejects calls quickly while open
- Permits a trial call in the half-open state
- Prevents notification failure from rolling back employee data

Automatic retry is intentionally absent.

A read timeout is an ambiguous distributed-system outcome: notification-service may finish processing after employee-service has stopped waiting. Retrying that request could duplicate a real email.

The current notification implementation logs intended email delivery. It does not provide:

- Real email delivery
- Durable messaging
- Delivery guarantees
- Idempotency
- Deduplication

## Correlation IDs

Every HTTP service supports:

```text
X-Correlation-ID
```

If a request supplies a valid identifier containing 1–64 letters, numbers, dots, underscores, or hyphens, the service preserves it.

Invalid or missing identifiers are replaced by a generated UUID.

The correlation ID is:

- Returned in the response header
- Stored temporarily in SLF4J MDC
- Included in the console log pattern
- Propagated from employee-service to notification-service
- Removed after request completion to protect reused servlet threads

Example:

```bash
curl -i \
  -H "X-Correlation-ID: manual-health-check-001" \
  http://localhost:8081/actuator/health
```

Expected response header:

```text
X-Correlation-ID: manual-health-check-001
```

Cross-service verification:

```bash
docker compose logs employee-service |
  grep "manual-correlation-id"

docker compose logs notification-service |
  grep "manual-correlation-id"
```

## Security-Relevant Logging

Security logs use searchable key-value fields:

```text
security_event=registration_attempt username=employee_demo
security_event=registration_succeeded username=employee_demo role=EMPLOYEE
security_event=registration_rejected username=employee_demo reason=username_exists
security_event=login_succeeded username=admin_demo role=ADMIN
security_event=login_failed username=admin_demo reason=bad_credentials
security_event=jwt_rejected method=GET path=/api/v1/employees reason=MalformedJwtException
security_event=access_denied username=employee_demo method=GET path=/api/v1/employees
```

Logs must never contain:

- Passwords
- Password hashes
- JWT values
- JWT signing secrets
- Database passwords
- Database root credentials

Audit local logs:

```bash
docker compose logs auth-service employee-service |
  grep -E "Bearer eyJ|JWT_SECRET|DB_PASSWORD"
```

Expected: no output.

## API Documentation

Each HTTP service provides an independent OpenAPI specification and Swagger UI.

| Service | OpenAPI JSON | Swagger UI |
|---|---|---|
| employee-service | http://localhost:8081/v3/api-docs | http://localhost:8081/swagger-ui.html |
| auth-service | http://localhost:8082/v3/api-docs | http://localhost:8082/swagger-ui.html |
| notification-service | http://localhost:8083/v3/api-docs | http://localhost:8083/swagger-ui.html |

### Using Swagger UI

1. Register and log in through auth-service Swagger UI.
2. Copy the JWT value without the `Bearer` prefix.
3. Open employee-service Swagger UI.
4. Select **Authorize**.
5. Paste only the JWT.
6. Swagger UI adds `Bearer` automatically.
7. Execute the protected operation.

Public registration always creates an `EMPLOYEE` account.

An EMPLOYEE token can access only:

```text
GET /api/v1/employees/me
```

The employee record must be linked through:

```text
employee.authUsername = JWT subject username
```

Employee and department management operations require ADMIN or HR.

## Authorization Model

| Role | Employee permissions | Department permissions |
|---|---|---|
| ADMIN | Full management | Full management |
| HR | Full management | Full management |
| EMPLOYEE | Read the explicitly linked record using `/employees/me` | None |

An EMPLOYEE cannot:

- List all employees
- Read another employee by numeric ID
- Create employees
- Update employees
- Delete employees
- Access department endpoints

Existing employee records may have no authentication username. This allows employee records and authentication accounts to remain separate concepts.

## Authentication Status Contract

| Scenario | Status |
|---|---:|
| Missing JWT | 401 |
| Malformed JWT | 401 |
| Expired or invalid JWT | 401 |
| Valid JWT with insufficient role | 403 |
| Valid ADMIN or HR request | Endpoint-specific success status |

`401` means no valid authenticated identity exists.

`403` means authentication succeeded, but the identity does not have the required authority.

## API Error Contract

Handled authentication and employee-domain errors use this structure:

```json
{
  "timestamp": "2026-08-19T12:00:00",
  "status": 409,
  "message": "Employee email already exists: employee@example.com",
  "path": "/api/v1/employees"
}
```

Common status codes:

| Status | Meaning |
|---:|---|
| 400 | Request validation failed or a search criterion is unsupported |
| 401 | Authentication is missing, malformed, expired, or invalid |
| 403 | Authentication is valid, but the role is not permitted |
| 404 | The requested or linked resource was not found |
| 409 | A username, employee email, or authentication-ownership link already exists |
| 500 | An unexpected internal error occurred |

Unexpected exception details are logged internally but are not returned to API clients.

Database uniqueness constraints remain the final protection against concurrent requests. Application-level checks provide clearer errors for normal conflicts.

## Logs and Troubleshooting

Inspect all containers:

```bash
docker compose ps --all
```

View all service logs:

```bash
docker compose logs --tail=200
```

View one service:

```bash
docker compose logs --tail=200 employee-service
docker compose logs --tail=200 auth-service
docker compose logs --tail=200 notification-service
```

Follow logs:

```bash
docker compose logs --follow employee-service
```

### Docker daemon is unavailable

An error containing:

```text
failed to connect to the docker API
```

means the Docker engine is unavailable.

Start Docker Desktop or the selected compatible engine, wait until it is ready, and rerun the command.

### Container temporarily becomes unhealthy

Some development computers require additional Spring Boot startup time.

Inspect:

```bash
docker compose ps --all
docker compose logs --tail=300 <service-name>
```

Application health checks use a startup grace period so slow initialization is not treated as immediate failure.

Do not increase health-check timeouts before inspecting application logs. A real startup exception should not be hidden by a longer wait.

### Docker reused an old application image

A Maven build does not update existing Docker images.

After changing application code, run:

```bash
docker compose up -d --build --wait --wait-timeout 300
```

### Maven reports missing child modules during image build

Every service Dockerfile copies the root POM and all module POMs before Maven constructs the reactor.

Source is copied only for modules required by that image.

A service depending on `common` must include:

```dockerfile
COPY common/pom.xml common/
COPY common/src common/src
```

Maven `-am` selects dependencies but cannot compile source that was never copied into the Docker build filesystem.

### Flyway reports no migrations

Flyway expects nested directories:

```text
db/migration/
```

This differs from one directory named:

```text
db.migration/
```

Verify migration packaging:

```bash
jar tf employee-service/target/employee-service-1.0.0.jar |
  grep "db/migration"
```

Expected:

```text
BOOT-INF/classes/db/migration/V1__create_employee_schema.sql
```

### Hibernate reports a missing table

If Hibernate uses `ddl-auto: validate` and Flyway found zero migrations, startup fails with a missing-table error.

Inspect earlier logs for:

```text
No migrations found
```

Then verify the migration directory and executable JAR contents.

Do not enable `baseline-on-migrate` merely to hide an incorrectly packaged migration.

### Existing-volume credentials fail

The MySQL image applies:

```text
MYSQL_DATABASE
MYSQL_USER
MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD
```

only while initializing an empty data directory.

Changing `.env` does not update credentials stored in an existing volume.

For disposable local data:

```bash
docker compose down --volumes
docker compose up -d --build --wait --wait-timeout 300
```

For important data, rotate credentials through MySQL administration instead of deleting the volume.

### Container reports `no main manifest attribute`

The service was packaged as a regular Maven JAR rather than an executable Spring Boot JAR.

A valid executable JAR contains:

```text
BOOT-INF/
```

and runs with:

```bash
java -jar app.jar
```

The parent Maven configuration binds Spring Boot’s `repackage` goal for service modules.

### SpotBugs fails the build

Do not immediately suppress the finding or reduce the threshold.

Determine whether it represents:

- A correctness defect
- A security risk
- A portability issue
- Generated-code noise
- A justified false positive

For example, JWT signing previously used `String.getBytes()` without an explicit encoding. It was corrected to use UTF-8 for deterministic signing across Windows and Linux.

### Coverage reports are missing

JaCoCo reports are generated during `verify`:

```bash
./mvnw clean verify
```

Running only `test` may not execute every reporting phase.

The root project is a POM-only aggregator and does not produce a meaningful code-coverage report.

### A focused module test cannot find a common class

Use `-am`:

```bash
./mvnw \
  -pl employee-service \
  -am \
  -Dtest=YourTestClass \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Without `-am`, Maven may use an older locally installed `common` artifact.

### Git reports LF-to-CRLF warnings

Messages such as:

```text
LF will be replaced by CRLF the next time Git touches it
```

are line-ending conversion notices, not compilation failures.

Use:

```bash
git diff --check
```

Actual whitespace errors are reported separately.

## Resetting Local Data

Delete containers and both database volumes:

```bash
docker compose down --volumes
```

This permanently removes local:

- Authentication users
- Employees
- Departments
- Flyway schema history
- MySQL data

Use this only when the data is disposable or backed up.

The next startup recreates both databases and applies Flyway migrations:

```bash
docker compose up -d --build --wait --wait-timeout 300
```

Never use volume deletion as the first response to an unexplained startup failure. Inspect status and logs first.

## Important Limitations

- Public registration always creates an EMPLOYEE account.
- No production administrative account-provisioning API exists yet.
- Local smoke tests may promote a disposable user directly in `auth_db`.
- Notification-service logs intended email delivery but does not send real email.
- Notification-service is intended for internal use but currently has no authentication or network-level restriction.
- Employee-to-auth ownership is linked by username rather than a cross-database foreign key.
- Flyway tests use H2 in MySQL compatibility mode; final database verification still uses Dockerized MySQL.
- Notification delivery is synchronous after commit and is not backed by a durable queue.
- A read timeout has an ambiguous result because the remote service may finish after the caller stops waiting.
- Notification delivery has no idempotency or deduplication, so automatic retry is disabled.
- Docker images are verified in CI but are not published to a registry.

## Security Rules

- Never commit `.env`.
- Never place real credentials in `.env.example`.
- Never commit or share JWT tokens.
- Never log passwords, password hashes, tokens, or secrets.
- Use different root and application database passwords.
- Connect through dedicated application users rather than MySQL root.
- Keep the JWT signing secret identical across token-producing and token-validating services.
- Rotate credentials immediately if they are printed, shared, or committed.
- Use explicit UTF-8 conversion for JWT signing-key bytes.
- Treat every client-supplied correlation ID as untrusted input.
- Review Dependabot pull requests and require CI success before merging.

## Final Verification Checklist

Before committing a milestone:

```bash
./mvnw clean verify
docker compose config --quiet
git diff --check
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
```

Verify health:

```bash
curl --fail http://localhost:8081/actuator/health
curl --fail http://localhost:8082/actuator/health
curl --fail http://localhost:8083/actuator/health
```

Verify Flyway:

```bash
docker compose exec mysql-auth sh -c \
  'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  -e "SELECT version, description, success FROM flyway_schema_history;"'

docker compose exec mysql-ems sh -c \
  'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  -e "SELECT version, description, success FROM flyway_schema_history;"'
```

Stop the stack while preserving data:

```bash
docker compose down
```