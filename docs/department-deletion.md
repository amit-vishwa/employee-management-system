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

At this draft checkpoint, expect one assertion failure: expected 409, actual 204,
with committed department rows=0 and employee rows=0. Keep the regression enabled;
do not merge the draft until the implementation and required checks pass.

## Remaining implementation and validation

Prevent employee removal through department cascading, return a specific conflict
for populated departments, and preserve unrelated server-error behavior. Extend
coverage for empty/missing departments, rollback and concurrency. Validate the
real foreign-key behavior through hosted MySQL integration tests. Local Docker
is not required. Employee reassignment, soft deletion and bulk deletion are
outside this issue.
