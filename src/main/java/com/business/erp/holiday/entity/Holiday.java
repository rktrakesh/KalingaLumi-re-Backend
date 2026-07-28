package com.business.erp.holiday.entity;

import com.business.erp.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private HolidayType holidayType;

    /**
     * Whether attendance is allowed on this holiday.
     * - FACTORY_HOLIDAY: always effectively "not allowed" from a payroll standpoint (normal salary, no OT),
     *   this flag is not consulted for FACTORY_HOLIDAY.
     * - NATIONAL_HOLIDAY / STATE_HOLIDAY: if true, employees may check in and will earn Holiday OT pay;
     *   if false, attendance is blocked and normal salary is paid.
     * Defaults to false (blocked) to preserve prior behaviour for existing rows.
     */
    @Column(name = "work_allowed", nullable = false)
    @Builder.Default
    private Boolean workAllowed = false;

    /** Only relevant when holidayType = STATE_HOLIDAY. Free-text state/region name. */
    @Column(name = "applicable_state", length = 100)
    private String applicableState;

    public enum HolidayType {FACTORY_HOLIDAY, NATIONAL_HOLIDAY, STATE_HOLIDAY}
}