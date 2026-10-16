package com.amit.ems.employeeservice;

import com.amit.ems.employeeservice.client.NotificationClient;
import com.amit.ems.employeeservice.entity.Department;
import com.amit.ems.employeeservice.entity.Employee;
import com.amit.ems.employeeservice.repository.DepartmentRepository;
import com.amit.ems.employeeservice.repository.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real service transactions and H2 persistence; authentication has separate tests.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:create_id_contract;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CreateIdContractTest {
    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper mapper;
    @Autowired private EmployeeRepository employees;
    @Autowired private DepartmentRepository departments;
    @MockitoBean private NotificationClient notifications;

    @BeforeEach
    @AfterEach
    void clearIsolatedDatabase() {
        employees.deleteAll();
        departments.deleteAll();
    }

    @Test
    void employeeCreateCannotOverwriteExistingRecordOrOwnership() throws Exception {
        Employee original = seedEmployee();
        reject("employees", original.getId(), "Employee");
        Employee persisted = employees.findById(original.getId()).orElseThrow();
        assertThat(persisted.getFirstName()).isEqualTo("Original");
        assertThat(persisted.getLastName()).isEqualTo("Employee");
        assertThat(persisted.getEmail()).isEqualTo("original@example.com");
        assertThat(persisted.getAuthUsername()).isEqualTo("original-user");
        assertThat(employees.count()).isEqualTo(1);
    }

    @Test
    void departmentCreateCannotOverwriteExistingRecord() throws Exception {
        Department original = seedDepartment();
        reject("departments", original.getId(), "Department");
        assertThat(departments.findById(original.getId()).orElseThrow().getName())
                .isEqualTo("Original department");
        assertThat(departments.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(longs = {1000000L, 0L, -1L})
    void suppliedIdsCannotInsertRecords(Long id) throws Exception {
        reject("employees", id, "Employee");
        reject("departments", id, "Department");
        assertThat(employees.count()).isZero();
        assertThat(departments.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void omittedAndNullIdsAllowNormalCreates(boolean omitId) throws Exception {
        for (String resource : new String[]{"employees", "departments"}) {
            MvcResult result = mvc.perform(post("/api/v1/" + resource)
                            .contentType("application/json")
                            .content(body(resource, omitId ? "" : "\"id\":null,")))
                    .andExpect(status().isCreated()).andReturn();
            long id = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
            assertThat(id).isPositive();
            assertThat(resource.equals("employees") ? employees.existsById(id)
                    : departments.existsById(id)).isTrue();
        }
        assertThat(employees.count()).isEqualTo(1);
        assertThat(departments.count()).isEqualTo(1);
    }

    @Test
    void putStillUpdatesTheRecordIdentifiedByItsPath() throws Exception {
        Employee employee = seedEmployee();
        Department department = seedDepartment();
        mvc.perform(put("/api/v1/employees/" + employee.getId())
                        .contentType("application/json")
                        .content(body("employees", "\"id\":" + employee.getId() + ",")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employee.getId()));
        mvc.perform(put("/api/v1/departments/" + department.getId())
                        .contentType("application/json")
                        .content(body("departments", "\"id\":" + department.getId() + ",")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(department.getId()));
        Employee updated = employees.findById(employee.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Replacement");
        assertThat(updated.getEmail()).isEqualTo("replacement@example.com");
        assertThat(updated.getAuthUsername()).isEqualTo("replacement-user");
        assertThat(departments.findById(department.getId()).orElseThrow().getName())
                .isEqualTo("Replacement department");
        assertThat(employees.count()).isEqualTo(1);
        assertThat(departments.count()).isEqualTo(1);
    }

    private void reject(String resource, Long id, String entity) throws Exception {
        String path = "/api/v1/" + resource;
        mvc.perform(post(path).contentType("application/json")
                        .content(body(resource, "\"id\":" + id + ",")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(entity
                        + " ID must not be supplied when creating a " + entity.toLowerCase()))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        verifyNoInteractions(notifications);
    }

    private String body(String resource, String idField) {
        return resource.equals("employees")
                ? "{" + idField + "\"firstName\":\"Replacement\",\"lastName\":\"Record\","
                    + "\"email\":\"replacement@example.com\",\"authUsername\":\"replacement-user\"}"
                : "{" + idField + "\"name\":\"Replacement department\"}";
    }

    private Employee seedEmployee() {
        return employees.saveAndFlush(Employee.builder().firstName("Original")
                .lastName("Employee").email("original@example.com")
                .authUsername("original-user").build());
    }

    private Department seedDepartment() {
        return departments.saveAndFlush(Department.builder().name("Original department").build());
    }
}
