package com.business.erp.employee.entity;

import com.business.erp.common.audit.AuditableEntity;
import com.business.erp.employee.enums.EmploymentType;
import com.business.erp.employee.enums.Gender;
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

    @Column(length = 150)
    private String email;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "designation", length = 100)
    private String legacyDesignationText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private DesignationMaster designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentMaster department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_category_id", nullable = false)
    private EmployeeCategoryMaster employeeCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private EmploymentType employmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id")
    private Employee reportingManager;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Gender gender;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "current_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentSalary;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 15)
    private String bankIfsc;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_holder_name", length = 100)
    private String bankAccountHolderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    public enum EmployeeStatus {DRAFT, ACTIVE, ON_NOTICE, RESIGNED, INACTIVE}
}