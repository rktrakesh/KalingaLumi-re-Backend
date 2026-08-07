package com.business.erp.common.storage;

import java.util.Locale;

public enum StorageFileType {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp"),
    PDF("pdf", "application/pdf");

    private final String canonicalExtension;
    private final String mimeType;

    StorageFileType(String canonicalExtension, String mimeType) {
        this.canonicalExtension = canonicalExtension;
        this.mimeType = mimeType;
    }

    public String canonicalExtension() {
        return canonicalExtension;
    }

    public String mimeType() {
        return mimeType;
    }

    public static StorageFileType fromExtension(String extension) {
        if (extension == null) return null;
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "png" -> PNG;
            case "jpg", "jpeg" -> JPEG;
            case "webp" -> WEBP;
            case "pdf" -> PDF;
            default -> null;
        };
    }

    public static StorageFileType fromMimeType(String mimeType) {
        if (mimeType == null) return null;
        String normalized = mimeType.toLowerCase(Locale.ROOT).trim();
        for (StorageFileType type : values()) {
            if (type.mimeType.equals(normalized)) return type;
        }
        return null;
    }

    public static StorageFileType fromSignature(byte[] bytes) {
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4E && bytes[3] == 0x47 && bytes[4] == 0x0D
                && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) return PNG;
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return JPEG;
        if (bytes.length >= 12 && asciiEquals(bytes, 0, "RIFF")
                && asciiEquals(bytes, 8, "WEBP")) return WEBP;
        if (bytes.length >= 5 && asciiEquals(bytes, 0, "%PDF-")) return PDF;
        return null;
    }

    private static boolean asciiEquals(byte[] bytes, int offset, String expected) {
        if (offset + expected.length() > bytes.length) return false;
        for (int i = 0; i < expected.length(); i++) {
            if ((byte) expected.charAt(i) != bytes[offset + i]) return false;
        }
        return true;
    }
}
