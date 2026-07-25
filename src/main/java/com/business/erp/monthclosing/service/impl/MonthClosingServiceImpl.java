package com.business.erp.monthclosing.service.impl;

import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.monthclosing.dto.response.MonthClosingResponse;
import com.business.erp.monthclosing.dto.response.PreCloseCheckResponse;
import com.business.erp.monthclosing.entity.MonthClosing;
import com.business.erp.monthclosing.repository.MonthClosingRepository;
import com.business.erp.monthclosing.service.MonthClosingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MonthClosingServiceImpl implements MonthClosingService {

    private final MonthClosingRepository monthClosingRepository;
    private final AuditService auditService;
    // Using field injection with @Lazy to avoid circular dependencies with repositories
    private final org.springframework.context.ApplicationContext ctx;
    private final Logger log = LoggerFactory.getLogger(MonthClosingServiceImpl.class);

    public MonthClosingServiceImpl(MonthClosingRepository monthClosingRepository,
                                   AuditService auditService,
                                   org.springframework.context.ApplicationContext ctx) {
        this.monthClosingRepository = monthClosingRepository;
        this.auditService = auditService;
        this.ctx = ctx;
    }

    @Override
    public boolean isMonthClosed(int year, int month) {
        return monthClosingRepository.findByYearAndMonth(year, month)
                .map(mc -> mc.getStatus() == MonthClosing.MonthStatus.CLOSED)
                .orElse(false);
    }

    @Override
    public PreCloseCheckResponse preCheck(int year, int month) {
        log.info("MonthClosingServiceImpl:preCheck :: {}/{}", year, month);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        // Lazy-load to avoid circular dependency
        var attendanceRepo = ctx.getBean(com.business.erp.attendance.repository.AttendanceRepository.class);
        var overtimeRepo = ctx.getBean(com.business.erp.overtime.repository.OvertimeRepository.class);
        var payrollRepo = ctx.getBean(com.business.erp.payroll.repository.PayrollRunRepository.class);
        var settingsService = ctx.getBean(com.business.erp.settings.service.SettingsService.class);

        long pendingCheckouts = attendanceRepo.countByDateRangeAndStatus(from, to,
                com.business.erp.attendance.entity.AttendanceRecord.AttendanceStatus.PENDING_CHECKOUT);
        long pendingOT = overtimeRepo.countByMonthAndStatus(from, to,
                com.business.erp.overtime.entity.OvertimeRequest.OvertimeStatus.PENDING);
        boolean payrollGenerated = payrollRepo.existsByYearAndMonthAndIsCurrentVersionTrue(year, month);
        boolean requiresPayroll = settingsService.getBooleanValue(
                com.business.erp.settings.enums.SettingKey.MONTH_CLOSING_REQUIRES_PAYROLL);

        List<String> blockers = new ArrayList<>();
        if (pendingCheckouts > 0) blockers.add(pendingCheckouts + " pending checkout(s) must be resolved");
        if (pendingOT > 0) blockers.add(pendingOT + " pending overtime request(s) must be approved/rejected");
        if (requiresPayroll && !payrollGenerated) blockers.add("Payroll has not been generated for this month");

        log.info("MonthClosingServiceImpl:preCheck :: canClose={} blockers={}", blockers.isEmpty(), blockers);
        return PreCloseCheckResponse.builder()
                .canClose(blockers.isEmpty()).pendingCheckouts(pendingCheckouts)
                .pendingOvertime(pendingOT).payrollGenerated(payrollGenerated).blockers(blockers).build();
    }

    @Override
    @Transactional
    public MonthClosingResponse closeMonth(int year, int month, String closedBy) {
        log.info("MonthClosingServiceImpl:closeMonth :: {}/{} by={}", year, month, closedBy);
        if (isMonthClosed(year, month))
            throw new BusinessException("Month " + year + "-" + month + " is already closed");

        PreCloseCheckResponse check = preCheck(year, month);
        if (!check.isCanClose())
            throw new BusinessException("Cannot close month. Blockers: " + String.join(", ", check.getBlockers()));

        MonthClosing mc = monthClosingRepository.findByYearAndMonth(year, month)
                .orElse(MonthClosing.builder().year(year).month(month).createdBy(closedBy).build());
        mc.setStatus(MonthClosing.MonthStatus.CLOSED);
        mc.setClosedBy(closedBy);
        mc.setClosedDate(LocalDateTime.now());
        MonthClosing saved = monthClosingRepository.save(mc);
        auditService.log("MONTH_CLOSING", "CLOSE", "MonthClosing", saved.getId());
        log.info("MonthClosingServiceImpl:closeMonth :: SUCCESS {}/{}", year, month);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MonthClosingResponse reopenMonth(int year, int month, String remarks, String reopenedBy) {
        log.info("MonthClosingServiceImpl:reopenMonth :: {}/{} by={}", year, month, reopenedBy);
        MonthClosing mc = monthClosingRepository.findByYearAndMonth(year, month)
                .orElseThrow(() -> new BusinessException("Month " + year + "-" + month + " has never been closed"));
        if (mc.getStatus() != MonthClosing.MonthStatus.CLOSED)
            throw new BusinessException("Month is not closed");
        mc.setStatus(MonthClosing.MonthStatus.OPEN);
        mc.setReopenedBy(reopenedBy);
        mc.setReopenedDate(LocalDateTime.now());
        mc.setReopenRemarks(remarks);
        MonthClosing saved = monthClosingRepository.save(mc);
        auditService.log("MONTH_CLOSING", "REOPEN", "MonthClosing", saved.getId());
        log.info("MonthClosingServiceImpl:reopenMonth :: SUCCESS {}/{}", year, month);
        return toResponse(saved);
    }

    @Override
    public MonthClosingResponse getStatus(int year, int month) {
        return monthClosingRepository.findByYearAndMonth(year, month)
                .map(this::toResponse)
                .orElse(MonthClosingResponse.builder().year(year).month(month).status("OPEN").build());
    }

    @Override
    public List<MonthClosingResponse> getHistory() {
        return monthClosingRepository.findAllByOrderByYearDescMonthDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private MonthClosingResponse toResponse(MonthClosing mc) {
        return MonthClosingResponse.builder()
                .id(mc.getId()).year(mc.getYear()).month(mc.getMonth()).status(mc.getStatus().name())
                .closedBy(mc.getClosedBy()).closedDate(mc.getClosedDate())
                .reopenedBy(mc.getReopenedBy()).reopenedDate(mc.getReopenedDate())
                .reopenRemarks(mc.getReopenRemarks()).build();
    }
}