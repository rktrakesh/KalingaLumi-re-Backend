package com.business.erp.payroll.engine.calendar;

import java.util.Optional;

public interface DayClassificationRule {
    Optional<PayrollDay> classify(DayClassificationInput input);
}