package com.business.erp.leave.service.impl;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.leave.dto.response.LeaveSettlementResult;
import com.business.erp.leave.entity.LeaveBalance;
import com.business.erp.leave.entity.LeaveSettlementLog;
import com.business.erp.leave.policy.UnusedLeaveOutcome;
import com.business.erp.leave.policy.UnusedLeavePolicyStrategy;
import com.business.erp.leave.repository.LeaveBalanceRepository;
import com.business.erp.leave.repository.LeaveSettlementLogRepository;
import com.business.erp.leave.service.LeaveSettlementService;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.enums.UnusedLeavePolicy;
import com.business.erp.settings.service.SettingsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveSettlementServiceImpl implements LeaveSettlementService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveSettlementLogRepository settlementLogRepository;
    private final EmployeeService employeeService;
    private final SettingsService settingsService;
    /** One strategy bean per {@link UnusedLeavePolicy} value — see item 8, no if/else on policy type. */
    private final List<UnusedLeavePolicyStrategy> policyStrategies;
    private final Logger log = LoggerFactory.getLogger(LeaveSettlementServiceImpl.class);

    private Map<UnusedLeavePolicy, UnusedLeavePolicyStrategy> strategiesByPolicy;

    @PostConstruct
    void indexStrategies() {
        strategiesByPolicy = new EnumMap<>(UnusedLeavePolicy.class);
        for (UnusedLeavePolicyStrategy strategy : policyStrategies) {
            strategiesByPolicy.put(strategy.policyType(), strategy);
        }
        log.info("LeaveSettlementServiceImpl:indexStrategies :: registered={}", strategiesByPolicy.keySet());
    }

    @Override
    @Transactional
    public int getRemainingBalance(Long employeeId, int year, int month) {
        return getOrCreateBalance(employeeId, year, month).getBalance();
    }

    @Override
    @Transactional
    public int consumeAutomaticLeave(Long employeeId, int year, int month, int requestedDays) {
        if (requestedDays <= 0) return 0;
        LeaveBalance balance = getOrCreateBalance(employeeId, year, month);
        int consume = Math.min(requestedDays, balance.getBalance());
        if (consume > 0) {
            balance.setUsed(balance.getUsed() + consume);
            balance.setBalance(balance.getBalance() - consume);
            leaveBalanceRepository.save(balance);
        }
        log.debug("LeaveSettlementServiceImpl:consumeAutomaticLeave :: empId={} {}/{} requested={} consumed={}",
                employeeId, year, month, requestedDays, consume);
        return consume;
    }

    @Override
    @Transactional
    public void restoreAutomaticLeave(Long employeeId, int year, int month, int days) {
        if (days <= 0) return;
        LeaveBalance balance = getOrCreateBalance(employeeId, year, month);
        balance.setUsed(Math.max(0, balance.getUsed() - days));
        balance.setBalance(balance.getBalance() + days);
        leaveBalanceRepository.save(balance);
        log.debug("LeaveSettlementServiceImpl:restoreAutomaticLeave :: empId={} {}/{} restored={}",
                employeeId, year, month, days);
    }

    @Override
    @Transactional
    public LeaveSettlementResult settleUnusedLeave(Long payrollRunId, Long employeeId, int year, int month,
                                                   UnusedLeavePolicy policy, int carryForwardLimit,
                                                   boolean encashmentEnabled, BigDecimal dailySalary, String settledBy) {
        Optional<LeaveSettlementLog> existing = settlementLogRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month);
        if (existing.isPresent()) {
            log.debug("LeaveSettlementServiceImpl:settleUnusedLeave :: already settled empId={} {}/{}, skipping", employeeId, year, month);
            LeaveSettlementLog s = existing.get();
            return LeaveSettlementResult.builder().unusedDays(s.getUnusedDays()).policyApplied(s.getPolicyApplied().name())
                    .expiredDays(s.getExpiredDays()).encashedDays(s.getEncashedDays())
                    .encashmentAmount(s.getEncashmentAmount()).carriedForwardDays(s.getCarriedForwardDays()).build();
        }

        LeaveBalance balance = getOrCreateBalance(employeeId, year, month);
        int unused = Math.max(balance.getBalance(), 0);

        UnusedLeavePolicy effectivePolicy = (!encashmentEnabled && policy == UnusedLeavePolicy.ENCASH)
                ? UnusedLeavePolicy.EXPIRE : policy;

        UnusedLeavePolicyStrategy strategy = strategiesByPolicy.get(effectivePolicy);
        if (strategy == null)
            throw new BusinessException("No UnusedLeavePolicyStrategy registered for policy: " + effectivePolicy);

        UnusedLeaveOutcome outcome = strategy.apply(unused, carryForwardLimit, dailySalary);
        int expiredDays = outcome.getExpiredDays();
        int encashedDays = outcome.getEncashedDays();
        BigDecimal encashmentAmount = outcome.getEncashmentAmount();
        int carriedForwardDays = outcome.getCarriedForwardDays();

        // Zero out this month's balance — it has been settled.
        balance.setBalance(0);
        leaveBalanceRepository.save(balance);

        // Credit carry-forward onto next month's balance.
        if (carriedForwardDays > 0) {
            YearMonth next = YearMonth.of(year, month).plusMonths(1);
            LeaveBalance nextBalance = getOrCreateBalance(employeeId, next.getYear(), next.getMonthValue());
            nextBalance.setCarriedForwardIn(nextBalance.getCarriedForwardIn() + carriedForwardDays);
            nextBalance.setAllocated(nextBalance.getAllocated() + carriedForwardDays);
            nextBalance.setBalance(nextBalance.getBalance() + carriedForwardDays);
            leaveBalanceRepository.save(nextBalance);
        }

        settlementLogRepository.save(LeaveSettlementLog.builder()
                .payrollRunId(payrollRunId).employeeId(employeeId).year(year).month(month)
                .unusedDays(unused).policyApplied(effectivePolicy)
                .expiredDays(expiredDays).encashedDays(encashedDays).encashmentAmount(encashmentAmount)
                .carriedForwardDays(carriedForwardDays).carryForwardLimit(carryForwardLimit)
                .settledBy(settledBy).settledDate(LocalDateTime.now()).build());

        log.info("LeaveSettlementServiceImpl:settleUnusedLeave :: empId={} {}/{} unused={} policy={} expired={} encashed={} carried={}",
                employeeId, year, month, unused, effectivePolicy, expiredDays, encashedDays, carriedForwardDays);

        return LeaveSettlementResult.builder().unusedDays(unused).policyApplied(effectivePolicy.name())
                .expiredDays(expiredDays).encashedDays(encashedDays)
                .encashmentAmount(encashmentAmount).carriedForwardDays(carriedForwardDays).build();
    }

    @Override
    @Transactional
    public void reverseSettlement(Long employeeId, int year, int month) {
        settlementLogRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month).ifPresent(s -> {
            if (s.getCarriedForwardDays() > 0) {
                YearMonth next = YearMonth.of(year, month).plusMonths(1);
                leaveBalanceRepository.findByEmployeeIdAndYearAndMonth(employeeId, next.getYear(), next.getMonthValue())
                        .ifPresent(nb -> {
                            nb.setCarriedForwardIn(Math.max(0, nb.getCarriedForwardIn() - s.getCarriedForwardDays()));
                            nb.setAllocated(nb.getAllocated() - s.getCarriedForwardDays());
                            nb.setBalance(Math.max(0, nb.getBalance() - s.getCarriedForwardDays()));
                            leaveBalanceRepository.save(nb);
                        });
            }
            leaveBalanceRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                    .ifPresent(b -> {
                        b.setBalance(s.getUnusedDays());
                        leaveBalanceRepository.save(b);
                    });
            settlementLogRepository.delete(s);
            log.info("LeaveSettlementServiceImpl:reverseSettlement :: empId={} {}/{} reversed", employeeId, year, month);
        });
    }

    private LeaveBalance getOrCreateBalance(Long employeeId, int year, int month) {
        return leaveBalanceRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month).orElseGet(() -> {
            int allocation = settingsService.getIntValue(SettingKey.PAID_LEAVES_PER_MONTH);
            Employee emp = employeeService.getEmployee(employeeId);
            return leaveBalanceRepository.save(LeaveBalance.builder().employee(emp).year(year).month(month)
                    .allocated(allocation).used(0).balance(allocation).build());
        });
    }
}