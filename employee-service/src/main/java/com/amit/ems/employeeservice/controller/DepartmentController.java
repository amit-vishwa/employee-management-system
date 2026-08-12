package com.amit.ems.employeeservice.controller;

import com.amit.ems.employeeservice.config.OpenApiConfig;
import com.amit.ems.employeeservice.dto.DepartmentDto;
import com.amit.ems.employeeservice.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(
        name = "Departments",
        description = "ADMIN and HR operations for managing departments"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @Operation(
            summary = "Create department",
            description = "Creates department and sends a best-effort "
                    + "department-created notification. Notification failure "
                    + "does not roll back department creation."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Department created"
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
            )
    })
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Department found"
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
                    description = "Department was not found"
            )
    })
    public ResponseEntity<DepartmentDto> getDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @GetMapping
    @Operation(summary = "List all departments")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Departments returned; the list may be empty"
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
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace department's editable details")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Department updated"
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
                    description = "Department was not found"
            )
    })
    public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentDto dto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete department")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Department deleted"
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
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}