package com.business.erp.settings.service;

import com.business.erp.common.exception.BusinessException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Shared renderer for identifiers generated for newly-created employees only. */
public final class IdentifierTemplateRenderer {

    private static final Set<String> PLACEHOLDERS = Set.of(
            "CompanyShortName", "CompanyName", "FirstName", "FirstName4",
            "LastName", "EmployeeNumber", "JoiningYear");
    private static final java.util.regex.Pattern PLACEHOLDER_PATTERN = java.util.regex.Pattern.compile("\\{([^{}]+)}");

    private IdentifierTemplateRenderer() {
    }

    public static void validateTemplate(String template) {
        if (template == null || template.isBlank()) {
            throw new BusinessException("Identifier template is required");
        }
        java.util.regex.Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        boolean containsPlaceholder = false;
        while (matcher.find()) {
            containsPlaceholder = true;
            if (!PLACEHOLDERS.contains(matcher.group(1))) {
                throw new BusinessException("Unsupported identifier placeholder: {" + matcher.group(1) + "}");
            }
        }
        if (!containsPlaceholder) {
            throw new BusinessException("Identifier template must contain a supported placeholder");
        }
        if (!template.contains("{EmployeeNumber}")) {
            throw new BusinessException("Identifier template must contain {EmployeeNumber}");
        }
        if (PLACEHOLDER_PATTERN.matcher(template).replaceAll("").contains("{")
                || PLACEHOLDER_PATTERN.matcher(template).replaceAll("").contains("}")) {
            throw new BusinessException("Identifier template contains an invalid placeholder");
        }
    }

    public static String render(String template, IdentifierTemplateContext context) {
        validateTemplate(template);
        if (context == null || context.employeeNumber() < 1) {
            throw new BusinessException("A valid employee joining sequence is required for identifier generation");
        }

        String companyName = requiredCompanyName(context.companyName());
        String firstName = firstName(context.employeeName());
        String lastName = lastName(context.employeeName());
        String companyShortName = usableShortName(context.companyShortName(), companyName);
        LocalDate joiningDate = context.joiningDate();
        if (template.contains("{JoiningYear}") && joiningDate == null) {
            throw new BusinessException("Joining date is required when template contains {JoiningYear}");
        }

        Map<String, String> values = new LinkedHashMap<>();
        values.put("{CompanyShortName}", companyShortName);
        values.put("{CompanyName}", companyName);
        values.put("{FirstName}", firstName);
        values.put("{FirstName4}", firstAlphabeticCharacters(firstName, 4));
        values.put("{LastName}", lastName);
        values.put("{EmployeeNumber}", String.format("%04d", context.employeeNumber()));
        values.put("{JoiningYear}", joiningDate == null ? "" : String.valueOf(joiningDate.getYear()));

        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace(entry.getKey(), entry.getValue());
        }
        return rendered;
    }

    public static String renderEmployeeCode(String template, IdentifierTemplateContext context) {
        String code = employeeCode(render(template, context));
        if (code.isBlank()) {
            throw new BusinessException("Employee code template produced no letters or digits");
        }
        return code;
    }

    public static String renderUsername(String template, IdentifierTemplateContext context) {
        String username = username(render(template, context));
        if (username.isBlank()) {
            throw new BusinessException("Username template produced no usable characters");
        }
        return username;
    }

    public static String employeeCode(String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    public static String username(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "");
    }

    private static String requiredCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new BusinessException("Company name is required to generate employee identifiers");
        }
        return companyName.trim();
    }

    private static String usableShortName(String companyShortName, String companyName) {
        if (companyShortName != null && !companyShortName.isBlank()
                && companyShortName.codePoints().anyMatch(Character::isAlphabetic)) {
            return companyShortName.trim();
        }

        StringBuilder abbreviation = new StringBuilder();
        boolean atWordStart = true;
        for (int codePoint : companyName.codePoints().toArray()) {
            if (Character.isAlphabetic(codePoint)) {
                if (atWordStart) abbreviation.appendCodePoint(codePoint);
                atWordStart = false;
            } else {
                atWordStart = true;
            }
        }
        if (abbreviation.length() == 0) {
            throw new BusinessException("Company short name is blank and no usable abbreviation can be derived from company name");
        }
        return abbreviation.toString();
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("Employee name is required to generate employee identifiers");
        }
        return fullName.trim().split("\\s+")[0];
    }

    private static String lastName(String fullName) {
        String[] tokens = fullName.trim().split("\\s+");
        return tokens.length > 1 ? tokens[tokens.length - 1] : "";
    }

    private static String firstAlphabeticCharacters(String value, int maximum) {
        StringBuilder result = new StringBuilder();
        value.codePoints().filter(Character::isAlphabetic).limit(maximum).forEach(result::appendCodePoint);
        return result.toString();
    }
}
