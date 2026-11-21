# Employee-list query and payload baseline

Baseline application revision: `8d3ff84fb76829b6105a2e2df6ab23e3f0264e71` (after dependency PR #78). The baseline contribution (PR #79) changed tests and documentation only; endpoint, fetching and pagination behavior were unchanged in that PR.

## Reproduce without Docker

From the repository root, with Java 17 and Bash/Git Bash:

```bash
./mvnw --batch-mode --no-transfer-progress -pl employee-service -am \
  -Dtest=EmployeeListBaselineTest -Dsurefire.failIfNoSpecifiedTests=false test
```

PowerShell can use `.\mvnw.cmd` and put the arguments on one line. The command prints `BASELINE_ENV` and seven `BASELINE` CSV rows. The generated report is `employee-service/target/EmployeeListBaselineTest.csv`; it is build output, not a file to commit. Run the harness without parallel test execution: Hibernate statistics belong to the shared SessionFactory.

## Measurement boundary

The harness calls `GET /api/v1/employees` through MockMvc with the real security chain, controller, service, JPA persistence and JSON serialization. Spring Security supplies a synthetic ADMIN principal; JWT issuance/parsing and network transport are outside this measurement.

Each suite owns an isolated test database; each case resets its synthetic fixture rows. JDBC inserts commit before measurement. There is no outer test transaction, and production `open-in-view=false` is retained. No entities are seeded or preloaded through a JPA persistence context. Each request therefore gets the service's normal transaction/persistence context. Second-level/query caching is disabled; cache eviction and statistics reset occur immediately before each request. Fixture SQL is excluded.

`sql_statements` is Hibernate's prepared-statement count during the request, including association loads, rather than HQL execution count. `response_bytes` is the uncompressed serialized body length, excluding HTTP headers and transfer compression. ASCII synthetic values produce deterministic UTF-8 byte lengths; explicit IDs and fixed dates keep payload values stable. Every case runs twice as separate cold requests and checks identical counts/lengths. Returned IDs, row counts and department IDs/names are checked; no elapsed-time threshold is asserted.

The cases vary employees and distinct departments independently: empty, no department, one shared department, ten departments and one department per employee. This distinguishes per-distinct-department loading from one extra query for every employee regardless of sharing.

## H2 observations

Executed November 16, 2026 on Windows with Java 17.0.20.1, H2 2.4.240, Hibernate 7.4.13.Final, Spring Boot 4.1.2 and Jackson 3.3.0. The review run used installed Maven 3.10.0; the repository wrapper now declares 3.10.1. Seven cases passed, with both cold requests in every case returning identical query counts and byte lengths.

| Employees | Distinct departments | SQL statements | Body bytes |
|---:|---:|---:|---:|
| 0 | 0 | 1 | 2 |
| 10 | 0 | 1 | 2,084 |
| 10 | 1 | 2 | 2,154 |
| 10 | 10 | 11 | 2,156 |
| 100 | 1 | 2 | 21,777 |
| 100 | 10 | 11 | 21,797 |
| 100 | 100 | 101 | 21,961 |

For these fixtures the observed pattern is one employee-list SELECT plus one SELECT per distinct referenced department. With one department per employee this is the N+1 case; shared departments are loaded once per request, not once per employee. The endpoint returned all 100 fixture employees, and the serialized body grew from roughly 2 KB for ten employees to 22 KB for one hundred. This is evidence for query/payload growth, not a latency benchmark.
 H2 compatibility mode is not MySQL: query counts describe this ORM code path, not production latency, query plans, throughput or memory consumption. Response size is measured for these exact fixtures and serializer settings. No speedup is claimed.

## MySQL comparison in CI

`EmployeeListBaselineMySqlIT` reuses the same measurement cases against disposable MySQL 8.0 with production Flyway migrations and Hibernate validation. The existing `mysql-integration-tests` profile discovers it automatically. With a compatible Docker runtime:

```bash
./mvnw --batch-mode --no-transfer-progress -pl employee-service -am \
  -Pmysql-integration-tests verify
```

Its report is `employee-service/target/EmployeeListBaselineMySqlIT.csv`; `BASELINE_ENV`/`BASELINE` lines also appear in Failsafe output. The MySQL baseline has not been executed locally because Docker is unavailable. Inspect the PR's Maven Verify job for actual execution before claiming a MySQL result.

## Follow-up

Issue #80 uses the measured SQL growth to justify a focused fetch plan. `EmployeeRepository.findAll()` now declares an entity graph for `department`. Hibernate fetches the optional to-one association with the list query; employees with no department remain present. Other repository methods and the entity's lazy mapping are unchanged. The controller, mapper, array response and authorization are unchanged.

The existing seven-case harness now asserts one request-time SQL statement for each cold request and checks all employee DTO fields against the fixture. It also asserts the pre-optimization fixture byte lengths above. These byte expectations intentionally detect serialization drift and must be reviewed if the API contract changes. No list ordering guarantee is introduced.

Before the production change, the new query assertion failed in the five scenarios containing departments, including 101 statements for 100 employees in 100 departments; the empty and null-department scenarios passed. This confirms the regression catches the original issue.

Local H2 after-change results and hosted MySQL status are recorded below. The MySQL subclass inherits these same assertions and runs in the existing CI profile. Local Docker is unavailable; do not treat local success as MySQL validation.

Separately, the endpoint still returns every employee. Response bounds and pagination remain a separate API-design concern; reducing SQL statements does not bound payload size or prove a latency improvement.

### Focused fetch-plan results (issue #80)

Executed November 19, 2026 in the isolated review copy with Java 17.0.20.1, H2 2.4.240 and Hibernate 7.4.13.Final, using installed Maven 3.10.0 (wrapper: 3.10.1). All seven scenarios passed, each with two cold requests:

| Employees | Distinct departments | Before SQL | After SQL | Body bytes (unchanged) |
|---:|---:|---:|---:|---:|
| 0 | 0 | 1 | 1 | 2 |
| 10 | 0 | 1 | 1 | 2,084 |
| 10 | 1 | 2 | 1 | 2,154 |
| 10 | 10 | 11 | 1 | 2,156 |
| 100 | 1 | 2 | 1 | 21,777 |
| 100 | 10 | 11 | 1 | 21,797 |
| 100 | 100 | 101 | 1 | 21,961 |

The pre-change MySQL baseline in PR #79 matched these before counts and bytes (MySQL 8.0.46, Java 17.0.21). The after-change MySQL result is pending hosted CI; do not infer it from H2. Reuse the commands above to run the regression and compare its printed report.
