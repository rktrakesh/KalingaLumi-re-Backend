package com.business.erp.performance.entity;

import com.business.erp.customer.entity.Customer;
import com.business.erp.employee.entity.Employee;
import com.business.erp.performance.enums.VisitOutcome;
import com.business.erp.performance.enums.VisitPurpose;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visited_by_employee_id", nullable = false)
    private Employee visitedBy;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_purpose", nullable = false, length = 30)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private VisitPurpose visitPurpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_outcome", nullable = false, length = 30)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private VisitOutcome visitOutcome;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}