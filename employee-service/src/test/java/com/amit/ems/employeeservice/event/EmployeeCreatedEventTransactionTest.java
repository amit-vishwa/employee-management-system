package com.amit.ems.employeeservice.event;

import com.amit.ems.common.web.CorrelationIdFilter;
import com.amit.ems.employeeservice.client.NotificationClient;
import com.amit.ems.employeeservice.dto.EmployeeDto;
import com.amit.ems.employeeservice.repository.EmployeeRepository;
import com.amit.ems.employeeservice.service.EmployeeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employee_event_transactions;"
                + "MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@ActiveProfiles("test")
class EmployeeCreatedEventTransactionTest {

    private static final String EMAIL = "ada@example.com";
    private static final String CORRELATION_ID = "employee-event-transaction-test";
    private static final EmployeeCreatedEvent EXPECTED_EVENT =
            new EmployeeCreatedEvent(EMAIL, "Ada Lovelace", CORRELATION_ID);

    @Autowired
    private EmployeeService service;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // Keep the real listener and service; prevent external notification delivery.
    @MockitoBean
    private NotificationClient notifications;

    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        clearDatabase();
    }

    @AfterEach
    void clearDatabase() {
        transaction.executeWithoutResult(status -> employees.deleteAll());
    }

    @Test
    void commit_shouldNotifyExactlyOnceAfterTransactionCompletes() {
        AtomicBoolean visibleFromSeparateTransaction = new AtomicBoolean();
        doAnswer(invocation -> {
            visibleFromSeparateTransaction.set(Boolean.TRUE.equals(
                    transaction.execute(status -> employees.existsByEmail(EMAIL))));
            return null;
        }).when(notifications).sendEmployeeCreated(EXPECTED_EVENT);

        EmployeeDto created = createInTransaction(false);

        assertThat(visibleFromSeparateTransaction.get()).isTrue();
        assertPersisted(created);
        verify(notifications).sendEmployeeCreated(EXPECTED_EVENT);
        verifyNoMoreInteractions(notifications);
    }

    @Test
    void rollback_shouldDiscardEmployeeAndSuppressNotification() {
        EmployeeDto created = createInTransaction(true);

        transaction.executeWithoutResult(status -> {
            assertThat(employees.existsById(created.getId())).isFalse();
            assertThat(employees.count()).isZero();
        });
        verifyNoInteractions(notifications);
    }

    @Test
    void notificationExceptionAfterCommit_shouldLeaveEmployeePersisted() {
        doThrow(new IllegalStateException("controlled notification failure"))
                .when(notifications).sendEmployeeCreated(EXPECTED_EVENT);

        // AFTER_COMMIT transactional listeners run from afterCompletion;
        // Spring logs this callback failure without failing the completed caller.
        // The double bypasses NotificationClient's own circuit-breaker fallback.
        EmployeeDto created = assertDoesNotThrow(() -> createInTransaction(false));

        assertPersisted(created);
        verify(notifications).sendEmployeeCreated(EXPECTED_EVENT);
        verifyNoMoreInteractions(notifications);
    }

    private EmployeeDto createInTransaction(boolean rollback) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(
                CorrelationIdFilter.MDC_KEY, CORRELATION_ID)) {
            return Objects.requireNonNull(transaction.execute(status -> {
                EmployeeDto created = service.createEmployee(EmployeeDto.builder()
                        .firstName("Ada").lastName("Lovelace")
                        .email(EMAIL).authUsername("ada-transaction-test").build());
                assertThat(created.getId()).isNotNull();
                employees.flush();
                assertThat(employees.existsById(created.getId())).isTrue();
                verifyNoInteractions(notifications);
                if (rollback) {
                    status.setRollbackOnly();
                }
                return created;
            }));
        }
    }

    private void assertPersisted(EmployeeDto created) {
        // Read in a fresh transaction, outside the completed create transaction.
        transaction.executeWithoutResult(status -> {
            var persisted = employees.findById(created.getId()).orElseThrow();
            assertThat(persisted.getEmail()).isEqualTo(EMAIL);
            assertThat(persisted.getFirstName()).isEqualTo("Ada");
            assertThat(persisted.getLastName()).isEqualTo("Lovelace");
            assertThat(persisted.getAuthUsername()).isEqualTo("ada-transaction-test");
            assertThat(employees.count()).isEqualTo(1);
        });
    }
}
