package com.business.erp.common.storage;

public record StoredFile(
        String storageKey,
        String originalFilename,
        String contentType,
        long size) {
}
