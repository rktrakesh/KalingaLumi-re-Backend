package com.business.erp.settings.service;

import java.time.LocalDate;

/** Immutable data shared by employee-code and username template rendering. */
public record IdentifierTemplateContext(
        String companyName,
        String companyShortName,
        String employeeName,
        long employeeNumber,
        LocalDate joiningDate) {
}
