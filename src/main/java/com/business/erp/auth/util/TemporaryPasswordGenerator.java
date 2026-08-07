package com.business.erp.auth.util;

import java.security.SecureRandom;

public final class TemporaryPasswordGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "Kl@7";
    private static final String RANDOM_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        return generate(12);
    }

    public static String generate(int minimumLength) {
        int targetLength = Math.max(minimumLength, 12);
        StringBuilder password = new StringBuilder(PREFIX);
        while (password.length() < targetLength) {
            password.append(RANDOM_CHARACTERS.charAt(RANDOM.nextInt(RANDOM_CHARACTERS.length())));
        }
        return password.toString();
    }
}
