package com.business.erp.leave.entity;

import com.business.erp.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(name = "uq_leave_balance_emp_month",
                columnNames = {"employee_id", "year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer allocated;

    @Column(nullable = false)
    @Builder.Default
    private Integer used = 0;

    @Column(nullable = false)
    private Integer balance;

    @Column(name = "carried_forward_in", nullable = false)
    @Builder.Default
    private Integer carriedForwardIn = 0;

    public void useLeave() {
        if (balance <= 0) throw new IllegalStateException("No leave balance available");
        used++;
        balance--;
    }

    public void restoreLeave() {
        if (used > 0) {
            used--;
            balance++;
        }
    }
}