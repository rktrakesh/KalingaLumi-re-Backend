package com.business.erp.employee.enums;

import com.business.erp.common.storage.StorageFileType;

import java.util.Set;

public enum EmployeeDocumentType {
    PROFILE_PHOTO(false, true, Set.of(StorageFileType.PNG, StorageFileType.JPEG, StorageFileType.WEBP)),
    IDENTITY_PROOF(false, true, Set.of(StorageFileType.PDF, StorageFileType.PNG, StorageFileType.JPEG)),
    PAN(false, false, Set.of(StorageFileType.PDF, StorageFileType.PNG, StorageFileType.JPEG)),
    BANK_PROOF(false, false, Set.of(StorageFileType.PDF, StorageFileType.PNG, StorageFileType.JPEG)),
    CERTIFICATE(true, false, Set.of(StorageFileType.PDF, StorageFileType.PNG, StorageFileType.JPEG)),
    DRIVING_LICENSE(false, false, Set.of(StorageFileType.PDF, StorageFileType.PNG, StorageFileType.JPEG)),
    APPOINTMENT_LETTER(false, false, Set.of(StorageFileType.PDF, StorageFileType.PNG, StorageFileType.JPEG)),
    SALARY_SLIP_ACKNOWLEDGEMENT(false, false, Set.of(StorageFileType.PDF, StorageFileType.PNG, StorageFileType.JPEG));

    private final boolean multipleCurrent;
    private final boolean requiredForActivation;
    private final Set<StorageFileType> allowedFileTypes;

    EmployeeDocumentType(boolean multipleCurrent, boolean requiredForActivation,
                         Set<StorageFileType> allowedFileTypes) {
        this.multipleCurrent = multipleCurrent;
        this.requiredForActivation = requiredForActivation;
        this.allowedFileTypes = allowedFileTypes;
    }

    public boolean allowsMultipleCurrent() {
        return multipleCurrent;
    }

    public boolean isRequiredForActivation() {
        return requiredForActivation;
    }

    public Set<StorageFileType> allowedFileTypes() {
        return allowedFileTypes;
    }
}
