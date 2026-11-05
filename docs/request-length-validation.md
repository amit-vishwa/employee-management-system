# Employee-service request length limits

Issue: https://github.com/amit-vishwa/employee-management-system/issues/72

Department name and employee firstName, lastName, email, authUsername and
designation accept at most 255 UTF-16 code units on POST and PUT. This uses
standard Jakarta Bean Validation @Size semantics (Java String.length), not
MySQL character counting for every Unicode input. A supplementary character
such as an emoji consumes two code units. ASCII255/256 boundary cases and a
supplementary-Unicode MVC case make this convention explicit.

The persisted columns remain VARCHAR(255); no migration, truncation or trimming
is performed. Existing required-field and email-format validation remains active.
Optional authUsername and designation still accept null or empty values.
Employee departmentName is derived response data and is excluded. Authentication
requests and password policy are outside this change.

Oversized input returns HTTP400 with a field-specific message before the write
service is called. Rejected creates add no rows; rejected updates preserve the
existing value. Exact-limit valid requests retain the normal201/200 responses.
Email boundary fixtures also satisfy the existing email validator: local part64,
three domain labels no longer than63, total length255 or256.

## Tests

RequestLengthValidationTest uses MVC slices to prove oversized requests cannot
reach either write service, and verifies optional fields, required fields, email
format and UTF-16 counting. AbstractRequestLengthContract supplies24 shared real
MVC/security/service/database cases: six fields x POST/PUT x255/256. Fixtures are
committed, tests have no outer transaction, and JDBC verifies final row counts
and persisted values. Notifications are mocked so accepted employee creates do
not require a running notification service.

The shared department-conflict500 regression now installs a temporary CHECK rule
in its owned test database and submits a valid-sized name rejected by that rule.
It still verifies a genuine unrelated database failure becomes a safe500. The
fixture rule is dropped in finally; it is not an application migration.

Run locally without Docker:

    ./mvnw --batch-mode --no-transfer-progress -pl employee-service -am verify

RequestLengthMySqlIT runs the same24 cases through the existing hosted MySQL
profile with a disposable container, Flyway and Hibernate schema validation.
Hosted results must be reviewed before merge; local MySQL execution is not claimed.
