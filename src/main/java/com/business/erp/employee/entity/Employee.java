package com.business.erp.employee.entity;

import com.business.erp.common.audit.AuditableEntity;
import com.business.erp.employee.enums.EmployeeCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(length = 100)
    private String designation;

    @Column(length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_category", nullable = false, length = 30)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private EmployeeCategory employeeCategory = EmployeeCategory.FACTORY;

    @Column(name = "current_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentSalary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    public enum EmployeeStatus {ACTIVE, INACTIVE}
}