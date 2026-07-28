package com.business.erp.payroll.engine;

import com.business.erp.common.exception.InvalidPayrollStateException;
import com.business.erp.payroll.entity.PayrollRun;
import com.business.erp.payroll.enums.PayrollStatus;
import org.springframework.stereotype.Component;

@Component
public class PayrollValidationService {

    public void validateRecalculate(PayrollRun current) {
        requireStatus(current, "Recalculate", PayrollStatus.DRAFT, PayrollStatus.CALCULATED);
    }

    public void validateVerify(PayrollRun run) {
        requireStatus(run, "Verify", PayrollStatus.CALCULATED);
    }

    public void validateApprove(PayrollRun run) {
        requireStatus(run, "Approve", PayrollStatus.VERIFIED);
    }

    public void validateProcess(PayrollRun run) {
        requireStatus(run, "Process (disburse)", PayrollStatus.APPROVED, PayrollStatus.PROCESSED);
    }

    public void validateMarkPaid(PayrollRun run, boolean allDetailsPaid) {
        requireStatus(run, "Mark as fully paid", PayrollStatus.PROCESSED);
        if (!allDetailsPaid)
            throw new InvalidPayrollStateException(
                    "Cannot mark payroll as PAID — one or more employees have not been paid yet.");
    }

    public void validateLock(PayrollRun run) {
        if (run.getStatus() != PayrollStatus.PAID)
            throw new InvalidPayrollStateException(
                    "Only a fully PAID payroll run can be locked. Current status: " + run.getStatus());
    }

    public void validateReopen(PayrollRun run) {
        if (run.getStatus() == PayrollStatus.LOCKED)
            throw new InvalidPayrollStateException(
                    "Locked payroll cannot be reopened. It is permanently closed for audit integrity.");
        if (run.getStatus() == PayrollStatus.PAID)
            throw new InvalidPayrollStateException(
                    "Fully paid payroll cannot be reopened directly. Void/reverse the relevant payments first.");
        if (run.getStatus() == PayrollStatus.DRAFT || run.getStatus() == PayrollStatus.CALCULATED)
            throw new InvalidPayrollStateException(
                    "Payroll is still in " + run.getStatus() + " — just recalculate directly, no need to reopen.");
    }

    private void requireStatus(PayrollRun run, String action, PayrollStatus... allowed) {
        for (PayrollStatus s : allowed) {
            if (run.getStatus() == s) return;
        }
        throw new InvalidPayrollStateException(
                action + " is not allowed from status " + run.getStatus() +
                        ". Expected one of: " + java.util.Arrays.toString(allowed));
    }
}