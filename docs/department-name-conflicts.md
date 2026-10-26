# Department-name conflicts

Department create and rename return HTTP 409 with the existing ErrorResponse shape
and the message "A department with that name already exists" when the database
reports a duplicate on uk_departments_name. Unchanged-name and available-name
updates succeed. Unrelated integrity failures retain the existing safe HTTP 500.
No trimming, case folding or new name-equality policy is introduced; equality
follows the database collation. The cases use identical names on H2 and MySQL.

Both write paths use saveAndFlush so the immediate uniqueness constraint is
checked inside the service transaction, including rename SQL otherwise delayed
until commit. There is no check-then-insert race: database uniqueness remains
authoritative. The classifier requires Hibernate constraint metadata plus a
MySQL duplicate-key error (1062/23000) or H2 unique error (23505). Unknown
constraints and missing metadata are rethrown. SQL messages are not client errors.
The entity now names the same unique constraint already present in Flyway V1;
no migration, index removal or existing-data rewrite is required.

## Validation

Run locally without Docker:

    ./mvnw --batch-mode --no-transfer-progress -pl employee-service -am test

DepartmentConflictIntegrationTest runs seven shared full MVC/security/service/JPA
cases on an isolated H2 database, including competing requests and persisted state
after failed rename. Tests have no outer transaction and no mocked repositories.
An ADMIN/HR test principal exercises real authorization rules without requiring
JWT issuance. Unit cases check recognized and unrelated constraint metadata.

DepartmentConflictMySqlIT runs the same seven cases against its own disposable
MySQL 8.0 Testcontainer with Flyway migrations and Hibernate validation. Existing
CI already selects it through mysql-integration-tests; do not activate that profile
on the laptop. Hosted MySQL execution remains required before claiming MySQL
results or closing issue #66. The test container owns its database and is removed
by Testcontainers. No production database is touched.
