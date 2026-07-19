package com.business.erp.common.exception;

public class MonthClosedException extends RuntimeException {
    public MonthClosedException(int year, int month) {
        super("Month " + year + "-" + String.format("%02d", month) + " is closed. Operation not permitted.");
    }
}
