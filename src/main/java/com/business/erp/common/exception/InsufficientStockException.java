package com.business.erp.common.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String materialName, double available, double required) {
        super("Insufficient stock for '" + materialName + "'. Available: " + available + ", Required: " + required);
    }
}
