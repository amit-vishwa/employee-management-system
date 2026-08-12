package com.amit.ems.employeeservice.controller;

import com.amit.ems.employeeservice.config.OpenApiConfig;
import com.amit.ems.employeeservice.dto.EmployeeDto;
import com.amit.ems.employeeservice.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(
        name = "Employees",
        description = "ADMIN and HR operations for managing employees"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @Operation(
            summary = "Create an employee",
            description = "Creates an employee and sends a best-effort "
                    + "employee-created notification. Notification failure "
                    + "does not roll back employee creation."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Employee created"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT is missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user lacks ADMIN or HR role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Referenced department was not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email or authentication username is already assigned"
            )
    })
    public ResponseEntity<EmployeeDto> createEmployee(
            @Valid @RequestBody EmployeeDto dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(dto));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get the authenticated employee's record",
            description = "Returns only the employee record explicitly "
                    + "linked to the username in the JWT subject."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authenticated employee record returned"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT is missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user lacks EMPLOYEE role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No employee record is linked to the user"
            )
    })
    public ResponseEntity<EmployeeDto> getCurrentEmployee(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                employeeService.getEmployeeByAuthUsername(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee found"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT is missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user lacks ADMIN or HR role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee was not found"
            )
    })
    public ResponseEntity<EmployeeDto> getEmployee(
            @Parameter(
                    description = "Employee identifier",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping
    @Operation(summary = "List all employees")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Employees returned; the list may be empty"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT is missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user lacks ADMIN or HR role"
            )
    })
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an employee's editable details")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee updated"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT is missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user lacks ADMIN or HR role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee or referenced department was not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email or authentication username is already assigned"
            )
    })
    public ResponseEntity<EmployeeDto> updateEmployee(
            @Parameter(
                    description = "Employee identifier",
                    example = "1"
            )
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDto dto
    ) {
        return ResponseEntity.ok(
                employeeService.updateEmployee(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Employee deleted"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT is missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user lacks ADMIN or HR role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee was not found"
            )
    })
    public ResponseEntity<Void> deleteEmployee(
            @Parameter(
                    description = "Employee identifier",
                    example = "1"
            )
            @PathVariable Long id
    ) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search employees",
            description = "Supported criteria values are `designation` "
                    + "and `department`. Matching currently uses exact values."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Matching employees returned; "
                            + "the list may be empty"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Search criterion is unsupported"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT is missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user lacks ADMIN or HR role"
            )
    })
    public ResponseEntity<List<EmployeeDto>> searchEmployees(
            @Parameter(
                    name = "criteria",
                    description = "Search strategy",
                    example = "designation",
                    in = ParameterIn.QUERY,
                    required = true
            )
            @RequestParam String criteria,

            @Parameter(
                    name = "value",
                    description = "Exact designation or department name",
                    example = "Senior Engineer",
                    in = ParameterIn.QUERY,
                    required = true
            )
            @RequestParam String value
    ) {
        return ResponseEntity.ok(
                employeeService.searchEmployees(criteria, value)
        );
    }
}