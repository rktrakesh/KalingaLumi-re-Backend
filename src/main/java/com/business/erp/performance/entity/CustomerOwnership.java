package com.business.erp.performance.entity;

import com.business.erp.common.audit.AuditableEntity;
import com.business.erp.customer.entity.Customer;
import com.business.erp.employee.entity.Employee;
import com.business.erp.performance.enums.OwnershipStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "customer_ownership")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOwnership extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Set only for temporary assignments; null means "until further notice". */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_temporary", nullable = false)
    @Builder.Default
    private Boolean isTemporary = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private OwnershipStatus status = OwnershipStatus.ACTIVE;

    @Column(length = 255)
    private String remarks;
}