# Department deletion policy and regression

Issue: https://github.com/amit-vishwa/employee-management-system/issues/68

## Agreed behavior

An empty department may be deleted with HTTP 204. A populated department must
return HTTP 409 and preserve its employees and department. Missing departments
retain HTTP 404. Database referential integrity must also protect concurrent
assignment/deletion; a preliminary existence check alone is insufficient.

## Reproduced baseline: October 30, 2026

On the production sources merged in 279fe15, the focused H2 regression made a
committed department through the real service and inserted a committed employee
through JDBC. It called DELETE as an ADMIN through the real MVC/security/service/
JPA flow, with no outer test transaction and no mocked repositories.

Observed response: HTTP 204. Separate JDBC reads after request completion found
zero department rows and zero employee rows for the fixtures. The test correctly
failed its expected HTTP 409 assertion, reporting both persisted row counts.
This demonstrates employee removal through the current CascadeType.ALL mapping.
Only the disposable in-memory H2 database was used; no MySQL result is claimed.

## Run the focused regression without Docker

    ./mvnw --batch-mode --no-transfer-progress -pl employee-service -am -Dtest=DepartmentDeletionIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test

The original draft failed with expected 409, actual 204 and committed department
rows=0, employee rows=0. With the safeguard implemented, expect all five shared
deletion cases to pass. Do not merge until relevant hosted checks pass.

## Safeguard

The deletion service issues one department-only JPQL DELETE. Its affected-row
count preserves HTTP 404 for a missing department, without a check/delete race.
The restrictive fk_employees_department already present in Flyway V1 rejects a
delete when employees reference the department. The service translates only that
named parent-deletion violation (MySQL 1451/23000 or H2 23503) to HTTP 409 with
"Cannot delete a department that contains employees". Unknown constraints and
unrelated integrity failures are rethrown for existing safe HTTP 500 handling.
The SQL statement executes inside the transaction, so the failure is translated
before service completion. Employee cascading excludes REMOVE; the remaining
cascade operations are preserved. No migration or data rewrite is required.

An assignment holding an uncommitted foreign-key reference and an overlapping
DELETE are exercised in separate transactions. If assignment commits, deletion
must return 409 and preserve both rows. A separate case verifies assignment to a
successfully deleted department cannot create an orphan. These are persistence
checks, not a new policy for the employee endpoint's response during that race.

## Validation

Five shared MVC/database cases cover populated, empty and missing departments,
overlapped assignment/deletion, and orphan prevention. All use committed fixtures,
no outer test transaction and post-request JDBC checks. Seven classification
unit cases distinguish the intended foreign key from unrelated or missing metadata.

Run local tests and quality checks without Docker:

    ./mvnw --batch-mode --no-transfer-progress -pl employee-service -am verify

DepartmentDeletionMySqlIT runs the same five cases against a disposable MySQL 8.0
container with Flyway and Hibernate schema validation via the existing hosted
mysql-integration-tests profile. Hosted execution is pending; do not activate that
profile on the laptop or claim MySQL results before reviewing CI.

Employee reassignment, soft deletion and bulk deletion are outside this issue.
