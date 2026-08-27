# Employee Management System - Spring Boot 3.3

[![CI](https://github.com/amit-vishwa/employee-management-system/actions/workflows/ci.yml/badge.svg)](https://github.com/amit-vishwa/employee-management-system/actions/workflows/ci.yml)

A multi-module Spring Boot microservices project for employee and department management. It demonstrates JWT authentication, role-based authorization, database migrations, resilient service-to-service communication, automated quality gates, Docker Compose, and local Kubernetes deployment.

All required capabilities use free or open-source tooling. No paid service is required to build, test, or run the project locally.

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
- Springdoc OpenAPI and Swagger UI
- Maven multi-module reactor
- JUnit 5, Mockito, AssertJ, and MockMvc
- JaCoCo
- SpotBugs
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
|   `-- correlation-ID infrastructure
|-- auth-service/
|   |-- registration and login
|   |-- JWT generation
|   `-- authentication database
|-- employee-service/
|   |-- employee and department management
|   |-- role-based authorization
|   `-- resilient notification integration
|-- notification-service/
|   `-- simulated employee notification processing
|-- k8s/
|   |-- base/
|   |   |-- Deployments and Services
|   |   |-- MySQL StatefulSets and persistent storage
|   |   |-- ConfigMaps and Secret template
|   |   `-- Kustomize configuration
|   `-- kind/
|       `-- local cluster configuration
|-- .github/workflows/ci.yml
|-- docker-compose.yml
|-- .env.example
|-- pom.xml
`-- README.md
```

## Prerequisites

Install these free tools:

- OpenJDK 17
- Git
- Docker Engine, an eligible Docker Desktop installation, or another compatible Linux container runtime
- OpenSSL
- `kubectl` for Kubernetes operations
- Kind for local Kubernetes deployment

The Maven Wrapper is included, so a separate Maven installation is not required.

```bash
java -version
./mvnw --version
docker version
docker compose version
kubectl version --client
kind version
git --version
```

## Build and Test

Run the complete quality gate:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

The Maven `verify` lifecycle performs:

- Compilation of all modules
- Execution of 60 automated tests
- Packaging of executable Spring Boot JARs
- JaCoCo coverage-report generation
- SpotBugs static analysis
- Build failure for medium-or-higher-confidence SpotBugs findings

Current test distribution:

| Module | Tests |
|---|---:|
| common | 4 |
| employee-service | 40 |
| auth-service | 13 |
| notification-service | 3 |
| **Total** | **60** |

Most tests use isolated H2-backed configuration. Dedicated Flyway tests execute migrations against H2 in MySQL compatibility mode. Docker and Kubernetes runtime verification remain the authoritative MySQL integration checks.

Quality reports are generated under each module:

```text
<module>/target/site/jacoco/index.html
<module>/target/site/jacoco/jacoco.xml
<module>/target/spotbugsXml.xml
```

JaCoCo currently records an honest baseline rather than enforcing an arbitrary global threshold. The original employee-service baseline was 68% instruction coverage and 52% branch coverage, while the core service implementation reached 94% instruction coverage and 83% branch coverage.

### Focused Maven tests

When a selected module depends on another reactor module, include `-am`:

```bash
./mvnw \
  -pl employee-service \
  -am \
  test
```

For one test class:

```bash
./mvnw \
  -pl employee-service \
  -am \
  -Dtest=NotificationClientCircuitBreakerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

`-am` means “also make” required reactor dependencies such as `common`. Without it, Maven may resolve an older locally installed dependency.

## Continuous Integration

GitHub Actions runs on pushes to `main`, pull requests targeting `main`, and manual workflow dispatch.

The ordered pipeline contains:

1. **Maven Verify**
   - Sets up Eclipse Temurin Java 17
   - Runs the complete Maven reactor
   - Executes tests, JaCoCo, and SpotBugs
   - Uploads coverage and SpotBugs reports for seven days
2. **Docker Image Build**
   - Runs only after Maven verification succeeds
   - Creates a temporary CI environment file from `.env.example`
   - Validates the Docker Compose model
   - Builds all three application images
   - Does not start containers or publish images

Dependabot checks Maven and GitHub Actions dependencies weekly. Minor and patch Maven updates are grouped; major updates remain separate. Updates are never merged automatically and must pass CI.

## Secure Local Configuration

Production credentials and JWT signing material are not stored in Git.

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

Do not use `docker compose config` without `--quiet` when resolved configuration may contain sensitive values.

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

Verify health:

```bash
curl --fail http://localhost:8081/actuator/health/readiness
curl --fail http://localhost:8082/actuator/health/readiness
curl --fail http://localhost:8083/actuator/health/readiness
```

Expected:

```json
{"status":"UP"}
```

Stop containers while preserving database volumes:

```bash
docker compose down
```

Delete disposable local data only when intentionally resetting the environment:

```bash
docker compose down --volumes
```

The MySQL image applies its initialization credentials only to an empty data directory. Changing `.env` does not rotate credentials inside an existing volume.

## Local Kubernetes Deployment

The complete stack can run on a free local Kubernetes cluster using Kind. No cloud platform, paid ingress product, or external image registry is required.

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

Load the images directly into Kind:

```bash
kind load docker-image \
  employee-management-system-auth-service:latest \
  employee-management-system-employee-service:latest \
  employee-management-system-notification-service:latest \
  --name ems-local
```

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

The local Secret manifest is ignored by Git:

```bash
git check-ignore -v k8s/base/secrets.local.yaml
```

Apply the namespace and local Secrets:

```bash
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/secrets.local.yaml
```

`secrets.example.yaml` is safe to commit because it contains placeholders. `secrets.local.yaml` must never be committed.

### 4. Deploy with Kustomize

Validate against the Kubernetes API:

```bash
kubectl apply --dry-run=server -k k8s/base
kubectl kustomize k8s/base > /dev/null
```

Deploy:

```bash
kubectl apply -k k8s/base
```

Wait for application rollouts:

```bash
kubectl rollout status deployment/auth-service -n ems --timeout=700s
kubectl rollout status deployment/employee-service -n ems --timeout=700s
kubectl rollout status deployment/notification-service -n ems --timeout=700s
```

Wait for MySQL StatefulSets:

```bash
kubectl rollout status statefulset/mysql-auth -n ems --timeout=700s
kubectl rollout status statefulset/mysql-ems -n ems --timeout=700s
```

Inspect the runtime:

```bash
kubectl get deployments,statefulsets,pods,services,pvc -n ems
kubectl get endpointslices -n ems
```

Expected state:

- Three application Deployments are Ready.
- Two MySQL StatefulSets are Ready.
- All Pods are `Running`.
- Both MySQL persistent volume claims are `Bound`.
- Services have EndpointSlice addresses.
- Application containers run as a non-root user.
- Resource requests and limits are configured.
- Startup, liveness, and readiness probes are healthy.

### 5. Local access with port-forwarding

The Services use `ClusterIP`, so expose each service temporarily in a separate terminal:

```bash
kubectl port-forward -n ems service/employee-service 8081:8081
kubectl port-forward -n ems service/auth-service 8082:8082
kubectl port-forward -n ems service/notification-service 8083:8083
```

Verify readiness:

```bash
curl --fail http://localhost:8081/actuator/health/readiness
curl --fail http://localhost:8082/actuator/health/readiness
curl --fail http://localhost:8083/actuator/health/readiness
```

Stop a port-forward with `Ctrl+C`. This closes only the local tunnel and does not stop the Pod.

### 6. Persistence verification

MySQL uses StatefulSets and persistent volume claims. Deleting a database Pod must not delete its schema history or data:

```bash
kubectl get pvc -n ems
kubectl delete pod mysql-auth-0 -n ems
kubectl rollout status statefulset/mysql-auth -n ems --timeout=700s
```

The recreated Pod should attach the same PVC. Flyway history should remain available:

```bash
kubectl exec -n ems mysql-auth-0 -- sh -c \
  'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  -e "SELECT installed_rank, version, description, success FROM flyway_schema_history;"'
```

The same persistence test applies to `mysql-ems-0`.

### Delete the local cluster

```bash
kind delete cluster --name ems-local
```

This deletes the cluster and its local persistent data. Run it only when that data is disposable.

## Configuration Model

| Environment | Configuration source |
|---|---|
| Automated tests | Test-profile YAML and H2 |
| Local Docker | Ignored `.env` |
| Local Kubernetes | ConfigMaps and ignored `secrets.local.yaml` |
| Direct IDE execution | IDE environment variables |
| Production deployment | Platform-managed runtime configuration and secrets |

Required configuration fails fast when missing. The application contains no fallback production password or JWT signing key.

Auth-service and employee-service must use the same JWT signing secret: auth-service signs tokens and employee-service verifies them.

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

Flyway executes pending migrations before Hibernate initializes. Hibernate uses `ddl-auto: validate`, so it verifies entity-to-schema compatibility without creating or silently modifying production tables.

Flyway records applied migrations in `flyway_schema_history`. Never edit an applied migration; add a new version instead:

```text
V2__add_employee_status.sql
V3__create_audit_table.sql
```

Editing an applied migration changes its checksum and causes validation to fail.

## Transaction Boundaries

Open Session in View is disabled:

```yaml
spring:
  jpa:
    open-in-view: false
```

Service classes use class-level `@Transactional(readOnly = true)` for queries and method-level `@Transactional` for state changes. Database access and entity-to-DTO mapping therefore complete inside explicit service transactions.

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

Automatic retry is intentionally absent. A read timeout has an ambiguous outcome: notification-service may complete after employee-service stops waiting, so retrying could duplicate a real email.

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

Public registration always creates an `EMPLOYEE` account. Client-supplied role fields are not accepted. This prevents public privilege escalation.

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
|---|---:|
| Missing JWT | 401 |
| Malformed, expired, or invalid JWT | 401 |
| Valid JWT with insufficient role | 403 |
| Valid ADMIN or HR request | Endpoint-specific success status |

`401` means no valid authenticated identity exists. `403` means authentication succeeded but the identity lacks the required authority.

There is intentionally no public ADMIN-registration flow. A production system should provision privileged accounts through a controlled bootstrap or protected administration process. Direct database role changes are acceptable only for disposable local testing.

## API Documentation

| Service | OpenAPI JSON | Swagger UI |
|---|---|---|
| employee-service | http://localhost:8081/v3/api-docs | http://localhost:8081/swagger-ui.html |
| auth-service | http://localhost:8082/v3/api-docs | http://localhost:8082/swagger-ui.html |
| notification-service | http://localhost:8083/v3/api-docs | http://localhost:8083/swagger-ui.html |

For protected employee-service operations:

1. Register and log in through auth-service.
2. Copy the returned JWT without the `Bearer` prefix.
3. Open employee-service Swagger UI.
4. Select **Authorize**.
5. Paste only the JWT.
6. Execute an operation permitted for that role.

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
|---:|---|
| 400 | Validation failed or a criterion is unsupported |
| 401 | Authentication is missing or invalid |
| 403 | Authentication is valid but the role is not permitted |
| 404 | A requested or linked resource was not found |
| 409 | A username, email, or ownership link already exists |
| 500 | An unexpected internal error occurred |

Database constraints remain the final protection against concurrent uniqueness conflicts.

## Security-Relevant Logging

Security logs use searchable structured fields:

```text
security_event=registration_succeeded username=employee_demo role=EMPLOYEE
security_event=login_succeeded username=admin_demo role=ADMIN
security_event=login_failed username=admin_demo reason=bad_credentials
security_event=jwt_rejected method=GET path=/api/v1/employees reason=MalformedJwtException
security_event=access_denied username=employee_demo method=GET path=/api/v1/employees
```

Logs must never contain passwords, password hashes, JWT values, signing secrets, or database credentials.

## Troubleshooting

### Docker or BuildKit becomes unavailable

Errors such as `failed to connect to the docker API`, unexpected EOF, or a BuildKit panic indicate an engine failure rather than an application compilation failure. Restart the Docker engine, confirm `docker info`, and rebuild the affected image.

### Container reports `no main manifest attribute`

The JAR was packaged as a regular Maven JAR rather than an executable Spring Boot JAR. A valid executable JAR contains `BOOT-INF/` and runs with `java -jar app.jar`. The parent build binds Spring Boot's `repackage` goal for application modules.

### MySQL rejects changed credentials

MySQL initialization environment variables apply only to an empty data directory. Changing `.env` or Kubernetes Secrets does not alter credentials already stored in an existing volume. Rotate important credentials through MySQL administration; delete volumes only when their data is disposable.

### Kubernetes Pod is unhealthy

```bash
kubectl get pods -n ems
kubectl describe pod <pod-name> -n ems
kubectl logs <pod-name> -n ems --tail=300
```

Inspect the application exception before changing probe thresholds. A longer timeout should not hide a genuine startup failure.

### A Kubernetes image change is not visible

Kind does not automatically receive a rebuilt Docker image. Rebuild, load, and restart the relevant Deployment:

```bash
docker compose build employee-service
kind load docker-image employee-management-system-employee-service:latest --name ems-local
kubectl rollout restart deployment/employee-service -n ems
kubectl rollout status deployment/employee-service -n ems --timeout=700s
```

### Actuator probes return 401

Kubernetes startup, liveness, and readiness endpoints must be explicitly permitted by Spring Security. Business endpoints must remain protected. Verify both behaviors:

```bash
curl --fail http://localhost:8081/actuator/health/readiness
curl -i http://localhost:8081/api/v1/employees
```

Expected: health returns `200`; the protected endpoint without a JWT returns `401`.

### Flyway finds no migrations

Flyway expects nested directories named `db/migration/`, not one directory named `db.migration`. Verify the executable JAR:

```bash
jar tf employee-service/target/employee-service-1.0.0.jar |
  grep "db/migration"
```

### A focused test cannot find a common class

Include `-am` and, when selecting a single test, set `-Dsurefire.failIfNoSpecifiedTests=false` so dependency modules without that test name do not fail selection.

### Git reports LF-to-CRLF warnings

These are line-ending conversion notices, not compilation failures. Use `git diff --check`; actual whitespace errors are reported separately.

## Important Limitations

- Public registration always creates an EMPLOYEE account.
- No production privileged-account provisioning API exists yet.
- Notification-service logs intended email delivery but does not send email.
- Notification-service has no durable message broker or delivery guarantees.
- Notification-service is intended for internal use but has no NetworkPolicy yet.
- Employee-to-auth ownership is linked by username rather than a cross-database foreign key.
- Flyway tests use H2 in MySQL compatibility mode; MySQL runtime verification is still required.
- Notification delivery is synchronous after commit and has no idempotency or deduplication.
- Docker images are built in CI but are not published to a registry.
- The Kubernetes configuration is designed for local Kind learning, not production deployment.
- TLS, ingress, autoscaling, centralized logging, metrics dashboards, backups, and disaster recovery are not yet implemented.

## Security Rules

- Never commit `.env` or `k8s/base/secrets.local.yaml`.
- Never place real credentials in example files.
- Never commit, print, or share JWT tokens.
- Never log passwords, hashes, tokens, signing secrets, or database credentials.
- Use different root and application database passwords.
- Connect through dedicated application users instead of MySQL root.
- Keep the JWT signing secret identical between auth-service and employee-service.
- Run application containers as a non-root user.
- Treat client-supplied correlation IDs as untrusted input.
- Review dependency updates and require CI success before merging.

## Final Verification Checklist

Before committing a milestone:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
docker compose config --quiet
kubectl kustomize k8s/base > /dev/null
kubectl apply --dry-run=server -k k8s/base
git diff --check
git status --short
```

For Docker runtime verification:

```bash
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
docker compose down
```

For Kubernetes runtime verification:

```bash
kubectl get deployments,statefulsets,pods,services,pvc -n ems
kubectl get endpointslices -n ems
```

Do not commit until tests, static analysis, configuration validation, runtime health, and the staged diff have all been reviewed.
