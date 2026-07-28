package com.business.erp.common.exception;

/**
 * Thrown by any validator in the payroll generation pipeline
 * ({@code PayrollGenerationPolicyValidator}, {@code DuplicatePayrollValidator},
 * {@code PayrollLockValidator}, {@code AttendanceLockValidator}, {@code SnapshotValidator})
 * when generation must not proceed. When this is thrown, nothing has been persisted yet
 * — no PayrollRun, no snapshot, no calculation — so the transaction rollback is a no-op
 * by construction; a "Payroll Generation Blocked" audit event is still recorded
 * (asynchronously, in its own transaction) before this propagates to the controller.
 */
public class PayrollGenerationNotAllowedException extends RuntimeException {
    public PayrollGenerationNotAllowedException(String message) {
        super(message);
    }
}