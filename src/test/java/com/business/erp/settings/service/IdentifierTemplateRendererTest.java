package com.business.erp.settings.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentifierTemplateRendererTest {

    @Test
    void rendersEmployeeCodeWithFirstFourAlphabeticCharacters() {
        IdentifierTemplateContext context = new IdentifierTemplateContext(
                "Incense World", "IW", "Ra-ni Das", 1, LocalDate.of(2026, 8, 6));

        assertEquals("IWRANI0001", IdentifierTemplateRenderer.renderEmployeeCode(
                "{CompanyShortName}{FirstName4}{EmployeeNumber}", context));
    }

    @Test
    void rendersUsernameUsingSameSequenceAndUnderscoreTemplate() {
        IdentifierTemplateContext context = new IdentifierTemplateContext(
                "Incense World", "IW", "Rakesh Kumar", 1, LocalDate.of(2026, 8, 6));

        assertEquals("iw_rakesh_0001", IdentifierTemplateRenderer.renderUsername(
                "{CompanyShortName}_{FirstName}_{EmployeeNumber}", context));
    }

    @Test
    void derivesShortNameWithoutPersistingIt() {
        IdentifierTemplateContext context = new IdentifierTemplateContext(
                "Incense World", "", "Rani Das", 1, LocalDate.of(2026, 8, 6));

        assertEquals("IWRANI0001", IdentifierTemplateRenderer.renderEmployeeCode(
                "{CompanyShortName}{FirstName4}{EmployeeNumber}", context));
    }

    @Test
    void rejectsTemplateWithoutEmployeeNumber() {
        assertThrows(com.business.erp.common.exception.BusinessException.class,
                () -> IdentifierTemplateRenderer.validateTemplate("{CompanyShortName}{FirstName4}"));
    }

    @Test
    void rejectsJoiningYearWhenJoiningDateIsMissing() {
        IdentifierTemplateContext context = new IdentifierTemplateContext("ERP System", "ERP", "Rani Das", 1, null);

        assertThrows(com.business.erp.common.exception.BusinessException.class,
                () -> IdentifierTemplateRenderer.renderEmployeeCode("{JoiningYear}{EmployeeNumber}", context));
    }
}
