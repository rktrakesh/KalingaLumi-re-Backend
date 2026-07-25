package com.business.erp.payroll.engine;

import com.business.erp.payroll.engine.calendar.PayrollCalendar;
import com.business.erp.payroll.entity.PayrollSettingsSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Responsible ONLY for converting a {@link PayrollCalendar} into money. This is a PURE
 * calculation engine:
 * <ul>
 *     <li>Its only input is one immutable {@link PayrollCalculationContext}.</li>
 *     <li>Its only output is one immutable {@link PayrollCalculationResult}.</li>
 *     <li>It performs NO repository access, NO EntityManager/JdbcTemplate use, NO live
 *     settings reads, and calls no service that fetches data from anywhere. Its only
 *     collaborators — {@link PayrollPolicyService} and {@link CalculationBreakdownBuilder}
 *     — are themselves pure/stateless (arithmetic and string formatting only).</li>
 *     <li>It never touches persistence and produces no side effects, so it behaves like
 *     a mathematical function: same input always produces the same output. This is what
 *     makes it trivially unit-testable in isolation and safe to run as a batch/offline
 *     calculation without a database at all.</li>
 * </ul>
 * All data gathering (attendance, holidays, leave, settings) happens upstream in
 * {@code PayrollServiceImpl} and {@code PayrollCalendarEngine} — by the time this class
 * runs, every fact it needs is already sitting in the context.
 * <p>
 * Formulas:
 * <pre>
 * Overtime Pay   = Approved OT minutes (PRESENT days only) x Hourly Rate x Overtime Multiplier
 * Weekly Off Pay = Worked minutes on WEEKLY_OFF days        x Hourly Rate x Weekly Off Multiplier
 * Holiday OT Pay = Worked minutes on work-allowed holidays  x Hourly Rate x Holiday OT Multiplier
 * Loss of Pay    = Hourly Rate x Working Hours Per Day x Absent Days
 * Net Salary     = Basic + Overtime + WeeklyOffPay + HolidayOT + LeaveEncashment - LossOfPay
 * </pre>
 * Holiday OT, Weekly Off pay and normal Overtime are mutually exclusive by construction —
 * each is summed strictly from the {@link com.business.erp.payroll.engine.calendar.PayrollDay}
 * flags {@code PayrollCalendarEngine} already resolved, so a minute can never be paid twice.
 */
@Component
@RequiredArgsConstructor
public class PayrollCalculationEngine {

    /**
     * Stamped onto every {@link PayrollCalculationResult} (and persisted on
     * {@code PayrollCalculationLog}) so a future change to this engine's formulas can
     * never be silently mistaken for having applied to historical payroll — see item 4,
     * Payroll Calculation Versioning. Bump this whenever the calculation logic changes.
     */
    public static final String ENGINE_VERSION = "V1";

    private final PayrollPolicyService policyService;
    private final CalculationBreakdownBuilder breakdownBuilder;
    private final Logger log = LoggerFactory.getLogger(PayrollCalculationEngine.class);

    public PayrollCalculationResult calculate(PayrollCalculationContext ctx) {
        PayrollCalendar calendar = ctx.getCalendar();
        PayrollSettingsSnapshot snapshot = ctx.getSnapshot();
        BigDecimal hourlyRate = ctx.getHourlyRate();
        BigDecimal hourlyRateFractional = hourlyRate.divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
        BigDecimal dailyRate = policyService.computeDailySalary(ctx.getSalary(), snapshot);

        int otMinutes = calendar.approvedOtMinutes();
        BigDecimal otAmount = hourlyRateFractional
                .multiply(BigDecimal.valueOf(otMinutes))
                .multiply(policyService.overtimeMultiplier(snapshot))
                .setScale(2, RoundingMode.HALF_UP);

        int weeklyOffOtMinutes = calendar.weeklyOffWorkedMinutes();
        BigDecimal weeklyOffAmount = hourlyRateFractional
                .multiply(BigDecimal.valueOf(weeklyOffOtMinutes))
                .multiply(policyService.weeklyOffMultiplier(snapshot))
                .setScale(2, RoundingMode.HALF_UP);

        int holidayOtMinutes = calendar.holidayWorkedMinutes();
        BigDecimal holidayOtAmount = hourlyRateFractional
                .multiply(BigDecimal.valueOf(holidayOtMinutes))
                .multiply(policyService.holidayOtMultiplier(snapshot))
                .setScale(2, RoundingMode.HALF_UP);

        int absentDays = (int) calendar.absentDays();
        BigDecimal lossOfPay = hourlyRate
                .multiply(BigDecimal.valueOf(policyService.workingHoursPerDay(snapshot)))
                .multiply(BigDecimal.valueOf(absentDays))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossSalary = ctx.getSalary()
                .add(otAmount).add(weeklyOffAmount).add(holidayOtAmount).add(ctx.getLeaveEncashmentAmount());
        BigDecimal netSalary = grossSalary.subtract(lossOfPay).setScale(2, RoundingMode.HALF_UP);

        PayrollAmounts amounts = PayrollAmounts.builder()
                .basicSalary(ctx.getSalary()).hourlyRate(hourlyRate).dailyRate(dailyRate)
                .otAmount(otAmount).weeklyOffAmount(weeklyOffAmount).holidayOtAmount(holidayOtAmount)
                .leaveEncashmentAmount(ctx.getLeaveEncashmentAmount()).lossOfPayAmount(lossOfPay)
                .grossSalary(grossSalary.setScale(2, RoundingMode.HALF_UP)).netSalary(netSalary)
                .build();

        var breakdown = breakdownBuilder.build(
                ctx.getSalary(), hourlyRate,
                otMinutes, policyService.overtimeMultiplier(snapshot), otAmount,
                (int) calendar.weeklyOffWorkedDays(), weeklyOffOtMinutes, policyService.weeklyOffMultiplier(snapshot), weeklyOffAmount,
                (int) calendar.holidayWorkedDays(), holidayOtMinutes, policyService.holidayOtMultiplier(snapshot), holidayOtAmount,
                ctx.getLeaveEncashmentDays(), dailyRate, ctx.getLeaveEncashmentAmount(),
                absentDays, lossOfPay,
                netSalary);

        log.debug("PayrollCalculationEngine:calculate :: engine={} empId={} hourlyRate={} ot={} weeklyOff={} " +
                        "holidayOt={} encashment={} lop={} net={}",
                ENGINE_VERSION, ctx.getEmployee().getId(), hourlyRate, otAmount, weeklyOffAmount, holidayOtAmount,
                ctx.getLeaveEncashmentAmount(), lossOfPay, netSalary);

        return PayrollCalculationResult.builder()
                .engineVersion(ENGINE_VERSION)
                .amounts(amounts)
                .workedDays((int) calendar.presentDays())
                .weeklyOffCount((int) calendar.weeklyOffDays())
                .weeklyOffWorkedCount((int) calendar.weeklyOffWorkedDays())
                .holidayCount((int) calendar.holidayDays())
                .holidayWorkedCount((int) calendar.holidayWorkedDays())
                .paidLeaveCount((int) calendar.approvedPaidLeaveDays())
                .automaticPaidLeaveCount((int) calendar.automaticPaidLeaveDays())
                .absentCount(absentDays)
                .otMinutes(otMinutes)
                .holidayOtMinutes(holidayOtMinutes)
                .weeklyOffOtMinutes(weeklyOffOtMinutes)
                .leaveEncashmentDays(ctx.getLeaveEncashmentDays())
                .breakdown(breakdown)
                .build();
    }
}