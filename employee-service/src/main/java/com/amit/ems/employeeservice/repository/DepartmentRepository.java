package com.amit.ems.employeeservice.repository;

import com.amit.ems.employeeservice.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // A single SQL delete lets the existing restrictive foreign key arbitrate concurrent writes.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Department d where d.id = :id")
    int deleteDepartmentRowById(@Param("id") Long id);
}
