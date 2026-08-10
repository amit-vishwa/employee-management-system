# Employee Management System

A Spring Boot microservices learning project demonstrating employee and department management, JWT authentication, service-to-service communication, automated testing, and Docker-based local orchestration.

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

Tests use isolated test configuration. They do not require local MySQL or production credentials.

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

Health endpoints are public so Docker can call them, but business APIs remain protected by JWT authorization.

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

## Security Rules

- Never commit `.env`.
- Never place real credentials in `.env.example`.
- Use different root and application database passwords.
- Applications connect through dedicated database users rather than MySQL root.
- Rotate a credential immediately if it is printed, shared, or committed.
- Keep the JWT signing secret identical across token-producing and token-validating services.