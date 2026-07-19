package com.business.erp.monthclosing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "month_closings",
        uniqueConstraints = @UniqueConstraint(name = "uq_month_closing", columnNames = {"year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthClosing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    @Builder.Default
    private MonthStatus status = MonthStatus.OPEN;

    @Column(name = "closed_by", length = 50)
    private String closedBy;

    @Column(name = "closed_date")
    private LocalDateTime closedDate;
    @Column(name = "reopened_by", length = 50)

    private String reopenedBy;
    @Column(name = "reopened_date")

    private LocalDateTime reopenedDate;
    @Column(name = "reopen_remarks", columnDefinition = "TEXT")

    private String reopenRemarks;
    @Column(name = "created_by", length = 50)

    private String createdBy;
    @Column(name = "created_date", nullable = false)

    private LocalDateTime createdDate;

    @PrePersist
    public void prePersist() {
        if (createdDate == null) createdDate = LocalDateTime.now();
    }

    public enum MonthStatus {OPEN, CLOSED}
}
