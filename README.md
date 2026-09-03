# Employee Management System - Spring Boot 3.5

[![CI](https://github.com/amit-vishwa/employee-management-system/actions/workflows/ci.yml/badge.svg)](https://github.com/amit-vishwa/employee-management-system/actions/workflows/ci.yml)
[![CodeQL](https://github.com/amit-vishwa/employee-management-system/actions/workflows/codeql.yml/badge.svg)](https://github.com/amit-vishwa/employee-management-system/actions/workflows/codeql.yml)

A multi-module Spring Boot microservices project for employee and department management. It demonstrates JWT authentication, role-based authorization, database migrations, resilient service-to-service communication, automated testing and security gates, Docker Compose, and local Kubernetes deployment.

The project uses free or open-source tooling. No paid cloud service is required to build, test, or run it locally.

## Technology Stack

- Java 17
- Spring Boot 3.5
- Spring Web MVC
- Spring Data JPA
- Spring Security
- JWT authentication
- MySQL 8
- H2 for fast database-backed tests
- Testcontainers for MySQL integration tests
- Flyway Community
- Resilience4j Circuit Breaker
- Springdoc OpenAPI and Swagger UI
- Maven multi-module reactor
- Maven Surefire and Failsafe
- JUnit 5, Mockito, AssertJ, and MockMvc
- JaCoCo
- SpotBugs
- CycloneDX SBOM generation
- Trivy vulnerability scanning
- CodeQL code scanning
- GitHub Dependency Review
- Kubeconform manifest validation
- Docker and Docker Compose
- Kubernetes, Kind, and Kustomize
- GitHub Actions
- Dependabot

## Architecture

```text
Client
  |
  +--> auth-service :8082
  |      +--> registration and login
  |      +--> JWT generation
  |      +--> mysql-auth
  |
  +--> employee-service :8081
         +--> employee and department management
         +--> JWT validation and role authorization
         +--> mysql-ems
         +--> notification-service :8083
```

Notification delivery occurs after the employee database transaction commits. A notification failure does not roll back the employee record.

## Project Structure

```text
employee-management-system/
|-- common/
|   |-- shared exception contracts
|   |-- JWT utilities
|   |-- correlation-ID infrastructure
|   `-- log-injection sanitization
|-- auth-service/
|   |-- registration and login
|   |-- JWT generation
|   |-- authentication database and Flyway migrations
|   |-- default and dev logging profiles
|   `-- unit, configuration, and MySQL integration tests
|-- employee-service/
|   |-- employee and department management
|   |-- role-based authorization
|   |-- resilient notification integration
|   |-- employee database and Flyway migrations
|   |-- default and dev logging profiles
|   `-- unit, configuration, and MySQL integration tests
|-- notification-service/
|   |-- simulated employee notification processing
|   |-- default and dev logging profiles
|   `-- controller and configuration tests
|-- k8s/
|   |-- base/
|   |   |-- Deployments and Services
|   |   |-- MySQL StatefulSets and persistent storage
|   |   |-- ConfigMaps and Secret template
|   |   `-- Kustomize configuration
|   `-- kind/
|       `-- local cluster configuration
|-- .github/
|   |-- workflows/
|   |   |-- ci.yml
|   |   |-- codeql.yml
|   |   `-- dependency-review.yml
|   |-- dependabot.yml
|   `-- SECURITY.md
|-- LICENSE
|-- docker-compose.yml
|-- .env.example
|-- pom.xml
`-- README.md
```

## Prerequisites

Install the tools required for your workflow:

- OpenJDK 17
- Git
- Docker Engine, an eligible Docker Desktop installation, or a compatible Docker API runtime for Testcontainers and container deployment
- OpenSSL for generating local secrets
- `kubectl` for Kubernetes operations
- Kind for local Kubernetes deployment

The Maven Wrapper is included, so a separate Maven installation is not required.

Examples below use Bash or Git Bash. In Windows PowerShell, use `.\mvnw.cmd` and adapt shell-specific syntax.

```bash
java -version
./mvnw --version
git --version
docker version
docker compose version
kubectl version --client
kind version
```

Docker is not required for the fast test suite. It is required for the MySQL Testcontainers integration tests.

## Build and Test

### Fast verification without Docker

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

This runs the default Maven verification lifecycle without activating the MySQL integration-test profile.

Verification includes:

- Compilation of all modules
- Unit, controller, configuration, and applicable H2-backed tests
- Packaging of executable Spring Boot JARs
- JaCoCo coverage-report generation
- SpotBugs static analysis
- CycloneDX SBOM generation

The configured SpotBugs check fails the build when findings meet its configured threshold.

Test counts are intentionally not hard-coded here. Consult the generated reports for the current execution totals.

### Full verification with MySQL Testcontainers

Start a compatible Docker runtime, then run:

```bash
docker info

./mvnw \
  --batch-mode \
  --no-transfer-progress \
  -Pmysql-integration-tests \
  clean verify
```

The `mysql-integration-tests` profile enables Maven Failsafe integration tests.

The integration-test classes are:

- `AuthMySqlIntegrationIT`
- `EmployeeMySqlIntegrationIT`

They use disposable MySQL 8.0 containers to verify Flyway migrations and database schema behavior against MySQL rather than relying only on H2 compatibility mode.

Testcontainers supplies container connection details. These tests do not require the application's Docker Compose stack or a manually created application database.

Run through `verify`, not just `integration-test`, so Failsafe checks the results and fails the build when necessary.

### Verification when Docker is unavailable

To keep the profile enabled but intentionally skip MySQL integration-test execution:

```bash
./mvnw \
  --batch-mode \
  --no-transfer-progress \
  -Pmysql-integration-tests \
  -DskipITs \
  clean verify
```

This is partial verification: a successful result does not demonstrate that the MySQL integration tests passed.

Do not add `-DskipTests` when the intention is to skip only integration tests. It also skips unit-test execution and is honored by Failsafe.

CI runs the MySQL integration-test profile without these skip flags.

### Test layers

| Test layer | Purpose | Docker required |
|---|---|---|
| Unit and controller tests | Business behavior, validation, security, and HTTP contracts | No |
| H2-backed tests | Fast persistence and migration checks where configured | No |
| Logging-profile tests | Default and dev configuration values and correlation-ID pattern | No |
| MySQL integration tests | Flyway migrations and database behavior on MySQL 8.0 | Yes |

MySQL schema integration tests complement application tests. They do not replace end-to-end Docker Compose or Kubernetes verification.

### Focused Maven tests

When a selected module depends on another reactor module, include `-am`:

```bash
./mvnw \
  -pl employee-service \
  -am \
  test
```

For one unit-test class:

```bash
./mvnw \
  -pl employee-service \
  -am \
  -Dtest=NotificationClientCircuitBreakerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

For all three logging-profile test classes:

```bash
./mvnw \
  -pl auth-service,employee-service,notification-service \
  -am \
  -Dtest=LoggingProfileTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

`-am` means “also make” required reactor dependencies such as `common`. Without it, Maven may resolve an older locally installed dependency.

### Reports

Reports are generated under the relevant modules:

```text
<module>/target/surefire-reports/
<module>/target/failsafe-reports/
<module>/target/site/jacoco/index.html
<module>/target/site/jacoco/jacoco.xml
<module>/target/spotbugsXml.xml
```

Failsafe reports are produced when integration tests execute.

The aggregate CycloneDX SBOM is generated at:

```text
target/bom.json
```

JaCoCo records coverage for review rather than enforcing an arbitrary global coverage percentage. Use reports from the current build when assessing coverage.

## Continuous Integration

GitHub Actions runs CI and CodeQL on pushes to `main`, pull requests targeting `main`, and manual workflow dispatch. CodeQL also runs on a weekly schedule. Dependency Review runs on pull requests targeting `main`.

The pipeline contains:

### Maven Verify

- Sets up Eclipse Temurin Java 17.
- Runs the complete Maven reactor with the `mysql-integration-tests` profile.
- Executes unit and configuration tests.
- Executes MySQL Testcontainers integration tests through Failsafe.
- Runs JaCoCo and SpotBugs.
- Generates and uploads JaCoCo, SpotBugs, and CycloneDX artifacts.
- Scans the CycloneDX SBOM with Trivy.
- Fails on fixable HIGH or CRITICAL vulnerabilities under the configured scan policy.

### Kubernetes Manifest Validation

- Renders the Kustomize base.
- Validates rendered manifests with Kubeconform in strict mode.

### Docker Image Build and Scan

- Runs after Maven and Kubernetes validation succeed.
- Validates the Docker Compose model.
- Pulls current base images and builds all three application images.
- Uses explicit application-image references tagged with the Git commit SHA.
- Scans operating-system and application-library packages with Trivy.
- Fails on fixable HIGH or CRITICAL vulnerabilities under the configured scan policy.
- Does not deploy the application stack or publish images to a registry.

### CodeQL

- Builds and analyzes Java code using the `security-extended` query suite.
- Publishes results to GitHub code scanning.

### Dependency Review

- Reviews dependency changes in pull requests.
- Fails when newly introduced dependency vulnerabilities meet the configured HIGH or CRITICAL threshold.

The protected `main` branch requires Maven Verify, Kubernetes Manifest Validation, Docker Image Build, and Dependency Review to pass before merging. Changes are merged through pull requests.

Dependabot checks Maven and GitHub Actions dependencies. Updates must be reviewed and pass the required checks before merging.

## Secure Local Configuration

Application database credentials and JWT signing material are supplied through runtime configuration.

Create your ignored local environment file:

```bash
cp .env.example .env
openssl rand -hex 32
```

Replace every `CHANGE_ME` value in `.env`. Generate different values for database credentials and the JWT signing secret.

Never commit `.env`:

```bash
git check-ignore -v .env
docker compose config --quiet
```

Avoid displaying resolved Docker Compose configuration when it may contain sensitive values. Use `--quiet` for validation.

Auth-service and employee-service must use the same JWT signing secret: auth-service signs tokens and employee-service verifies them.

## Docker Compose Deployment

Build and start the complete stack:

```bash
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
```

Expected services:

- `mysql-auth`
- `mysql-ems`
- `auth-service`
- `employee-service`
- `notification-service`

Verify application health:

```bash
curl --fail http://localhost:8081/actuator/health
curl --fail http://localhost:8082/actuator/health
curl --fail http://localhost:8083/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

Where readiness probes are enabled, also verify `/actuator/health/readiness`.

Stop containers while preserving database volumes:

```bash
docker compose down
```

Only when intentionally deleting disposable local database data:

```bash
docker compose down --volumes
```

MySQL initialization credentials apply only to an empty data directory. Changing `.env` does not rotate credentials inside an existing database volume.

## Local Kubernetes Deployment

The complete stack can run on a local Kubernetes cluster using Kind. No cloud platform or external image registry is required.

### 1. Create the Kind cluster

```bash
kind create cluster \
  --name ems-local \
  --config k8s/kind/cluster-config.yaml

kubectl config current-context
kubectl get nodes
```

Expected context:

```text
kind-ems-local
```

### 2. Build and load application images

```bash
docker compose build \
  auth-service \
  employee-service \
  notification-service
```

Load the default local image tags directly into Kind:

```bash
kind load docker-image \
  employee-management-system-auth-service:latest \
  employee-management-system-employee-service:latest \
  employee-management-system-notification-service:latest \
  --name ems-local
```

If you override the Compose image references, use matching image names in the load command and Kubernetes configuration.

Verify images on the Kind node:

```bash
docker exec ems-local-control-plane \
  crictl images | \
  grep employee-management-system
```

### 3. Create local Kubernetes secrets

```bash
cp k8s/base/secrets.example.yaml \
  k8s/base/secrets.local.yaml
```

Replace every `CHANGE_ME` value with a locally generated value:

```bash
openssl rand -hex 32
```

Verify that the local Secret manifest is ignored:

```bash
git check-ignore -v k8s/base/secrets.local.yaml
```

Apply the namespace and local Secrets:

```bash
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/secrets.local.yaml
```

`secrets.example.yaml` contains placeholders. `secrets.local.yaml` must never be committed.

### 4. Deploy with Kustomize

Validate against the Kubernetes API:

```bash
kubectl kustomize k8s/base > /dev/null
kubectl apply --dry-run=server -k k8s/base
```

Deploy:

```bash
kubectl apply -k k8s/base
```

Wait for MySQL StatefulSets:

```bash
kubectl rollout status statefulset/mysql-auth -n ems --timeout=700s
kubectl rollout status statefulset/mysql-ems -n ems --timeout=700s
```

Wait for application rollouts:

```bash
kubectl rollout status deployment/auth-service -n ems --timeout=700s
kubectl rollout status deployment/employee-service -n ems --timeout=700s
kubectl rollout status deployment/notification-service -n ems --timeout=700s
```

Inspect the runtime:

```bash
kubectl get deployments,statefulsets,pods,services,pvc -n ems
kubectl get endpointslices -n ems
```

Expected state:

- Three application Deployments are Ready.
- Two MySQL StatefulSets are Ready.
- All application and database Pods are Running and Ready.
- Both MySQL persistent volume claims are Bound.
- Services have EndpointSlice addresses.
- Application containers run as a non-root user.
- Resource requests and limits are configured.
- Configured startup, liveness, and readiness probes are healthy.

### 5. Local access with port-forwarding

The Services use `ClusterIP`. Run each port-forward in a separate terminal:

```bash
kubectl port-forward -n ems service/employee-service 8081:8081
```

```bash
kubectl port-forward -n ems service/auth-service 8082:8082
```

```bash
kubectl port-forward -n ems service/notification-service 8083:8083
```

Verify readiness for the Kubernetes deployment:

```bash
curl --fail http://localhost:8081/actuator/health/readiness
curl --fail http://localhost:8082/actuator/health/readiness
curl --fail http://localhost:8083/actuator/health/readiness
```

Stop a port-forward with `Ctrl+C`. This closes the local tunnel without stopping the Pod.

### 6. Persistence verification

Perform this check only in a disposable local environment. Deleting a database Pod causes a temporary interruption, but its PVC should remain.

```bash
kubectl get pvc -n ems
kubectl delete pod mysql-auth-0 -n ems
kubectl rollout status statefulset/mysql-auth -n ems --timeout=700s
```

The recreated Pod should attach the same PVC. Verify that existing application data and Flyway schema history remain available.

For an interactive database session:

```bash
kubectl exec -it -n ems mysql-auth-0 -- sh -c \
  'mysql -u"$MYSQL_USER" -p "$MYSQL_DATABASE"'
```

Enter the local application database password when prompted. Do not paste credentials into commands or shared output.

Then inspect migration history:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history;
```

The same persistence check applies to `mysql-ems-0`.

### Delete the local cluster

Only when all cluster data is disposable:

```bash
kind delete cluster --name ems-local
```

This deletes the cluster and its local persistent data.

## Configuration Model

| Environment or test layer | Configuration source |
|---|---|
| Fast database-backed tests | Test-profile YAML and H2 where configured |
| Logging-profile tests | Application YAML and explicitly selected profile |
| MySQL integration tests | Testcontainers connection details and Failsafe migration location |
| Local Docker | Ignored `.env` and Docker Compose environment configuration |
| Local Kubernetes | ConfigMaps and ignored `secrets.local.yaml` |
| Direct IDE execution | IDE environment variables and optional profile selection |
| Production deployment | Platform-managed runtime configuration and secrets |

Application configuration contains no fallback production database password or JWT signing key.

A `.env` file used by Docker Compose is not automatically loaded by a directly launched Java application. Configure the required variables in the IDE or process environment.

## Database Schema Management

Flyway Community manages both schemas:

```text
auth-service/src/main/resources/db/migration/
employee-service/src/main/resources/db/migration/
```

Current migrations:

```text
auth-service:     V1__create_users_table.sql
employee-service: V1__create_employee_schema.sql
```

During application startup, Flyway executes pending migrations before Hibernate initializes. Hibernate uses `ddl-auto: validate`, so it verifies entity-to-schema compatibility without creating or silently modifying tables.

Flyway records applied migrations in `flyway_schema_history`. Never edit an applied migration; add a new version instead.

Example future migration names:

```text
V2__add_employee_status.sql
V3__create_audit_table.sql
```

Editing an applied migration changes its checksum and can cause validation to fail.

### Migration loading in MySQL integration tests

The MySQL integration tests load the module's copied production migrations from its build output directory.

The Failsafe profile supplies:

```text
mysql.migration.location=filesystem:<module>/target/classes/db/migration
```

This is an integration-test setting. The applications continue to use:

```text
classpath:db/migration
```

Tests fail when the expected migration location is missing and verify that migrations actually execute. A successful container startup alone does not demonstrate that schema initialization succeeded.

## Transaction Boundaries

Open Session in View is disabled:

```yaml
spring:
  jpa:
    open-in-view: false
```

Service classes use read-only transactions for queries and explicit write transactions for state changes. Database access and entity-to-DTO mapping complete inside service transaction boundaries.

## Notification Resilience

```text
Persist employee
    |
Commit transaction
    |
Publish EmployeeCreatedEvent
    |
AFTER_COMMIT listener
    |
NotificationClient
    |
Resilience4j circuit breaker
    |
notification-service
```

The notification client uses bounded connection and read timeouts. The circuit breaker records failures, opens after the configured threshold, rejects calls quickly while open, and permits controlled trial calls in the half-open state.

Automatic retry is intentionally absent. A read timeout has an ambiguous outcome: notification-service may complete after employee-service stops waiting, so retrying could duplicate a real notification.

The current notification implementation logs intended delivery. It does not provide real email, durable messaging, delivery guarantees, idempotency, or deduplication.

## Correlation IDs

Every HTTP service supports:

```text
X-Correlation-ID
```

A valid client identifier contains 1–64 letters, numbers, dots, underscores, or hyphens. Missing or invalid identifiers are replaced by a generated UUID.

The correlation ID is:

- Returned in the response header
- Stored temporarily in SLF4J MDC
- Included in application logs
- Propagated from employee-service to notification-service
- Removed after request completion to protect reused servlet threads

Example:

```bash
curl -i \
  -H "X-Correlation-ID: manual-health-check-001" \
  http://localhost:8081/actuator/health
```

## Authentication and Authorization

Public registration always creates an `EMPLOYEE` account. Client-supplied role fields are not accepted.

| Role | Employee permissions | Department permissions |
|---|---|---|
| ADMIN | Full management | Full management |
| HR | Full management | Full management |
| EMPLOYEE | Read its linked record through `/employees/me` | None |

The self-service link is:

```text
employee.authUsername = JWT subject username
```

Authentication status contract:

| Scenario | Status |
|---|---|
| Missing JWT | 401 |
| Malformed, expired, or invalid JWT | 401 |
| Valid JWT with insufficient role | 403 |
| Valid ADMIN or HR request | Endpoint-specific success status |

`401` means no valid authenticated identity exists. `403` means authentication succeeded but the identity lacks the required authority.

There is no public ADMIN-registration flow. Production privileged accounts require a controlled provisioning process. Direct database role changes are appropriate only for disposable local testing.

## API Documentation

| Service | OpenAPI JSON | Swagger UI |
|---|---|---|
| employee-service | [OpenAPI](http://localhost:8081/v3/api-docs) | [Swagger UI](http://localhost:8081/swagger-ui.html) |
| auth-service | [OpenAPI](http://localhost:8082/v3/api-docs) | [Swagger UI](http://localhost:8082/swagger-ui.html) |
| notification-service | [OpenAPI](http://localhost:8083/v3/api-docs) | [Swagger UI](http://localhost:8083/swagger-ui.html) |

For protected employee-service operations:

1. Register and log in through auth-service.
2. Copy the returned JWT without the `Bearer` prefix.
3. Open employee-service Swagger UI.
4. Select **Authorize**.
5. Paste only the JWT.
6. Execute an operation permitted for that role.

Do not share tokens in screenshots, logs, issues, or pull requests.

## API Error Contract

Handled errors use a consistent structure:

```json
{
  "timestamp": "2026-08-20T12:00:00",
  "status": 409,
  "message": "Employee email already exists: employee@example.com",
  "path": "/api/v1/employees"
}
```

| Status | Meaning |
|---|---|
| 400 | Validation failed or a criterion is unsupported |
| 401 | Authentication is missing or invalid |
| 403 | Authentication is valid but the role is not permitted |
| 404 | A requested or linked resource was not found |
| 409 | A username, email, or ownership link already exists |
| 500 | An unexpected internal error occurred |

Database constraints remain the final protection against concurrent uniqueness conflicts.

## Runtime Logging Profiles

Default configuration keeps routine logging at INFO and preserves the correlation-ID console pattern.

| Setting | Default | Dev profile |
|---|---|---|
| Root logger | INFO | INFO |
| `com.amit.ems` | INFO | DEBUG |
| Hibernate SQL logger | WARN | DEBUG |
| Direct JPA SQL output | Disabled | Disabled |
| Hibernate SQL formatting | Disabled | Enabled |
| JDBC bind-value logging | OFF | OFF |
| JDBC extraction logging | OFF | OFF |
| Correlation-ID console pattern | Preserved | Preserved |

Hibernate and JPA settings apply to auth-service and employee-service. Notification-service changes only its application logging level in the dev profile.

### Opt-in local diagnostics

Each application provides an `application-dev.yml` file.

To activate it for a directly launched application, set this environment variable in the application's launch configuration:

```text
SPRING_PROFILES_ACTIVE=dev
```

Alternatively, supply this application argument:

```text
--spring.profiles.active=dev
```

The dev profile enables application DEBUG logging. For auth-service and employee-service, formatted SQL is emitted through the Hibernate logger rather than direct `show-sql` output.

Use this profile only in disposable local environments with synthetic data. Do not activate it in shared environments.

Disabling JDBC bind and extraction logs does not guarantee that all application DEBUG messages are free of sensitive information. Continue reviewing log statements and output.

The profile does not supply database credentials or JWT secrets. Existing runtime configuration is still required.

Setting a variable in the host shell does not automatically pass it into Docker Compose containers. Container profile activation requires explicit environment mapping.

### Logging-profile regression tests

Each application includes `LoggingProfileTest` with checks for:

- Default logging configuration
- Dev-profile overrides
- Preservation of the correlation-ID console pattern
- SQL and JDBC logging settings where applicable

These tests verify configuration loading and values. They do not capture or validate actual emitted log messages.

## Security-Relevant Logging

Security logs use searchable fields such as:

```text
security_event=registration_succeeded username=employee_demo role=EMPLOYEE
security_event=login_succeeded username=admin_demo role=ADMIN
security_event=login_failed username=admin_demo reason=bad_credentials
security_event=jwt_rejected method=GET path=/api/v1/employees reason=MalformedJwtException
security_event=access_denied username=employee_demo method=GET path=/api/v1/employees
```

Logs must never contain passwords, password hashes, JWT values, signing secrets, or database credentials.

Untrusted values at the remediated logging call sites are passed through the shared log sanitizer. Carriage-return and newline characters are replaced to prevent forged additional log entries.

Sanitization is not redaction. Usernames, email addresses, and other personal data still require appropriate access controls, retention policies, and careful logging decisions.

## Automated Security Controls

The repository uses layered checks:

- SpotBugs detects implementation defects during Maven verification.
- CycloneDX produces a machine-readable software bill of materials.
- Trivy scans the SBOM for fixable HIGH and CRITICAL dependency vulnerabilities.
- Trivy scans all three application images for operating-system and application-library vulnerabilities.
- CodeQL performs scheduled and pull-request-aware static security analysis.
- Dependency Review checks dependency changes in pull requests.
- Dependabot monitors supported dependencies.
- Secret scanning detects supported credentials committed to the repository.
- Push protection blocks supported secrets before they are pushed.
- Private vulnerability reporting provides a non-public reporting channel.

Log-injection findings were addressed through a shared sanitizer and focused tests. Runtime-image package updates were added after container scanning identified vulnerable Alpine packages.

A successful scan is evidence for the configured tools, database versions, and scan scope at that time. It is not a guarantee that the application has no vulnerabilities.

## Troubleshooting

### Docker or BuildKit becomes unavailable

Errors such as `failed to connect to the docker API`, unexpected EOF, or a BuildKit panic can indicate a runtime failure.

Check:

```bash
docker info
docker version
```

Restore the runtime before retrying container builds or Testcontainers tests.

### MySQL integration tests do not run

Confirm that:

- The `mysql-integration-tests` profile is active.
- The command reaches `verify`.
- Neither `-DskipITs` nor `-DskipTests` is present.
- Failsafe reports exist for the expected integration-test classes.

A `BUILD SUCCESS` result with skipped tests or “No tests to run” is not evidence that MySQL integration tests passed.

### Flyway finds no migrations in an integration test

Confirm that production migration files were copied to:

```text
auth-service/target/classes/db/migration/
employee-service/target/classes/db/migration/
```

Verify that Failsafe supplies a module-specific `mysql.migration.location`. It must not point every module at the root project's output directory.

Use `clean verify` to avoid relying on stale build output. Do not hide missing migrations by disabling the missing-location check.

### Flyway finds no migrations during application startup

Flyway expects nested directories named `db/migration/`, not one directory named `db.migration`.

Inspect the executable JAR:

```bash
jar tf employee-service/target/employee-service-1.0.0.jar |
  grep "db/migration"
```

The application uses `classpath:db/migration`; the explicit filesystem location is specific to the integration tests.

### Container reports no main manifest attribute

The JAR may have been packaged as a regular Maven JAR rather than an executable Spring Boot JAR.

An executable Spring Boot JAR contains `BOOT-INF/` and runs with:

```bash
java -jar app.jar
```

The parent build binds Spring Boot's `repackage` goal for application modules.

### MySQL rejects changed credentials

MySQL initialization variables apply only to an empty data directory. Changing `.env` or Kubernetes Secrets does not change credentials already stored in an existing volume.

Rotate important credentials through MySQL administration. Delete volumes only when their data is disposable.

### Kubernetes Pod is unhealthy

```bash
kubectl get pods -n ems
kubectl describe pod <pod-name> -n ems
kubectl logs <pod-name> -n ems --tail=300
```

Inspect the startup exception before changing probe thresholds. A longer timeout should not hide a genuine application failure.

Review logs before sharing them and remove any sensitive information.

### A Kubernetes image change is not visible

Kind does not automatically receive a rebuilt Docker image:

```bash
docker compose build employee-service

kind load docker-image \
  employee-management-system-employee-service:latest \
  --name ems-local

kubectl rollout restart deployment/employee-service -n ems
kubectl rollout status deployment/employee-service -n ems --timeout=700s
```

### Actuator probes return 401

Configured health probes must be permitted by Spring Security while business endpoints remain protected.

```bash
curl --fail http://localhost:8081/actuator/health/readiness
curl -i http://localhost:8081/api/v1/employees
```

Expected: readiness returns `200`; the protected endpoint without a JWT returns `401`.

### DEBUG logs do not appear locally

Confirm that the application process actually received the `dev` profile.

- Check the IDE run configuration or application arguments.
- For containers, check the explicit environment mapping.
- Do not assume a host-shell variable was passed into a container.
- JDBC bind-value and extraction logging intentionally remain OFF in dev.

### A focused test cannot find a common class

Include `-am`. When selecting a single test class, add:

```text
-Dsurefire.failIfNoSpecifiedTests=false
```

This allows dependency modules without that named test to complete.

### Git reports LF-to-CRLF warnings

These are line-ending conversion notices, not compilation failures.

```bash
git diff --check
```

Actual whitespace errors are reported separately.

## Important Limitations

- Public registration always creates an EMPLOYEE account.
- No production privileged-account provisioning API exists yet.
- Notification-service logs intended email delivery but does not send email.
- Notification-service has no durable message broker or delivery guarantees.
- Notification-service is intended for internal use but has no NetworkPolicy yet.
- Employee-to-auth ownership is linked by username rather than a cross-database foreign key.
- MySQL integration tests verify database behavior but do not replace full application-stack tests.
- Notification delivery is synchronous after commit and has no idempotency or deduplication.
- Docker images are built and vulnerability-scanned in CI but are not published to a registry.
- Runtime logging is console-based; centralized collection, retention, and access controls are not implemented.
- The Kubernetes configuration is designed for local Kind learning, not production deployment.
- TLS, ingress, autoscaling, metrics dashboards, backups, and disaster recovery are not yet implemented.

## Security Rules

- Never commit `.env` or `k8s/base/secrets.local.yaml`.
- Never place real credentials in example files.
- Never commit, print, or share JWT tokens.
- Never log passwords, hashes, tokens, signing secrets, or database credentials.
- Sanitize untrusted values before writing them to logs.
- Do not treat sanitization as personal-data redaction.
- Keep the dev logging profile restricted to disposable local environments.
- Use different root and application database passwords.
- Connect through dedicated application users instead of MySQL root.
- Keep the JWT signing secret identical between auth-service and employee-service.
- Run application containers as a non-root user.
- Treat client-supplied correlation IDs as untrusted input.
- Review dependency updates and require CI success before merging.
- Keep CodeQL, Dependency Review, SBOM scanning, and container-image scanning enabled.
- Do not suppress a vulnerability without an evidence-based, documented risk assessment.

## Repository Governance

The repository uses the MIT License. See [LICENSE](LICENSE).

Report security vulnerabilities privately according to [.github/SECURITY.md](.github/SECURITY.md). Do not disclose credentials, JWT signing material, or other sensitive information in a public issue.

The repository uses GitHub secret scanning, push protection, Dependabot alerts, private vulnerability reporting, CodeQL, Dependency Review, SBOM scanning, and container-image scanning.

Changes to protected `main` go through pull requests and required status checks. Direct force pushes and deletion of the protected branch are blocked.

## Final Verification Checklist

### Source and fast build verification

```bash
./mvnw --batch-mode --no-transfer-progress clean verify

git diff --check
git status --short
git diff --stat
```

Remember that ordinary `git diff` does not include untracked files. Review new files explicitly or inspect them after staging.

### Full MySQL verification

With Docker available:

```bash
./mvnw \
  --batch-mode \
  --no-transfer-progress \
  -Pmysql-integration-tests \
  clean verify
```

Without Docker, explicitly record the limitation and let CI execute the integration tests:

```bash
./mvnw \
  --batch-mode \
  --no-transfer-progress \
  -Pmysql-integration-tests \
  -DskipITs \
  clean verify
```

### Deployment configuration checks

When deployment configuration changes:

```bash
docker compose config --quiet
kubectl kustomize k8s/base > /dev/null
```

With the intended Kubernetes cluster available:

```bash
kubectl config current-context
kubectl apply --dry-run=server -k k8s/base
```

### Runtime checks

For Docker Compose changes:

```bash
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
docker compose down
```

For Kubernetes changes:

```bash
kubectl get deployments,statefulsets,pods,services,pvc -n ems
kubectl get endpointslices -n ems
```

### Staged review

After staging only the intended files:

```bash
git diff --cached --check
git diff --cached --stat
git diff --cached
```

Review the test results, configuration changes, new files, and staged diff before committing. Do not merge until required CI checks pass. Clearly distinguish locally skipped checks from checks that actually executed.