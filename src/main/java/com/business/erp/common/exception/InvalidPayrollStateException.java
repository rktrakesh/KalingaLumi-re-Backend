package com.business.erp.common.exception;

public class InvalidPayrollStateException extends RuntimeException {
    public InvalidPayrollStateException(String message) {
        super(message);
    }
}