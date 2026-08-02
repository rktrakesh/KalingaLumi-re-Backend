package com.business.erp.auth.util;

import java.security.SecureRandom;

public final class TemporaryPasswordGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "Kl@";

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        int number = 10000 + RANDOM.nextInt(90000);
        return PREFIX + number;
    }
}