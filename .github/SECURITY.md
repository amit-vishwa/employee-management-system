# Security Policy

## Supported Version

This repository is a learning and portfolio project rather than a production
service. Security fixes are applied only to the latest code on the `main`
branch.

| Version | Supported |
|---|---|
| Latest `main` branch | Yes |
| Older commits and branches | No |

## Reporting a Vulnerability

Please do not disclose suspected vulnerabilities, credentials, access tokens,
database passwords, JWT signing material, or other sensitive information in a
public issue, pull request, discussion, or commit.

Use GitHub's private vulnerability reporting feature when it is available:

1. Open the repository's **Security** tab.
2. Select **Advisories**.
3. Select **Report a vulnerability**.
4. Include the affected component, reproduction steps, potential impact, and
   any suggested remediation.

If private vulnerability reporting is unavailable, open a public issue
containing only a minimal, non-sensitive description and request a private
communication channel. Do not include exploit details or secrets.

## What to Include

A useful report should include:

- The affected service, dependency, endpoint, workflow, or configuration.
- The relevant version, branch, or commit.
- Clear reproduction steps.
- The expected and actual behavior.
- The potential security impact.
- Any known workaround or proposed remediation.
- Confirmation that no real credentials or personal data are included.

## Response Expectations

Reports are reviewed on a best-effort basis. Because this is a personal
learning project, no commercial service-level agreement is provided.

The intended process is:

1. Acknowledge the report.
2. Reproduce and assess the issue.
3. Prepare and test a fix.
4. Publish the remediation and update affected dependencies or documentation.

## Security Scope

The project currently demonstrates:

- JWT-based authentication and role-based authorization.
- Runtime-provided secrets and database credentials.
- Dedicated non-root database users.
- Non-root application containers.
- Automated Maven verification and SpotBugs analysis.
- CycloneDX SBOM generation.
- Trivy vulnerability scanning.
- Kubernetes manifest validation.
- Dependabot dependency updates.

The following are learning-environment limitations rather than production
security guarantees:

- JWT verification currently uses a shared symmetric signing key.
- Notification delivery is best-effort HTTP communication.
- Local Docker Compose and Kind deployments are development environments.
- Example configuration files contain placeholders and must never contain real
  credentials.
- Public cloud production deployment is not currently supported.

## Secret-Handling Rules

- Never commit `.env`, `secrets.local.yaml`, access tokens, passwords, private
  keys, or real JWT signing material.
- Never paste secrets into issues, logs, screenshots, test reports, or commit
  messages.
- Rotate a credential immediately if accidental exposure is suspected.
- Use only synthetic accounts and test data when demonstrating the project.