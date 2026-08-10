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

Start the complete application:

```bash
docker compose up --build
```

Stop the application:

```bash
docker compose down
```

To also remove local database volumes:

```bash
docker compose down --volumes
```

Removing volumes permanently deletes the local container database data.

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