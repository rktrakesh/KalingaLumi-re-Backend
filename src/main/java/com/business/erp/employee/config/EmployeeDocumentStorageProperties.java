package com.business.erp.employee.config;

import com.business.erp.employee.enums.EmployeeDocumentType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "app.storage.employee-documents")
@Getter
@Setter
public class EmployeeDocumentStorageProperties {

    private DataSize profilePhotoMaxSize = DataSize.ofKilobytes(200);
    private DataSize panMaxSize = DataSize.ofMegabytes(1);
    private DataSize identityProofMaxSize = DataSize.ofMegabytes(2);
    private DataSize bankProofMaxSize = DataSize.ofMegabytes(5);
    private DataSize certificateMaxSize = DataSize.ofMegabytes(2);
    private DataSize drivingLicenseMaxSize = DataSize.ofMegabytes(1);
    private DataSize appointmentLetterMaxSize = DataSize.ofMegabytes(2);
    private DataSize salarySlipAcknowledgementMaxSize = DataSize.ofMegabytes(2);

    public long maxBytes(EmployeeDocumentType type) {
        return switch (type) {
            case PROFILE_PHOTO -> profilePhotoMaxSize.toBytes();
            case PAN -> panMaxSize.toBytes();
            case IDENTITY_PROOF -> identityProofMaxSize.toBytes();
            case BANK_PROOF -> bankProofMaxSize.toBytes();
            case CERTIFICATE -> certificateMaxSize.toBytes();
            case DRIVING_LICENSE -> drivingLicenseMaxSize.toBytes();
            case APPOINTMENT_LETTER -> appointmentLetterMaxSize.toBytes();
            case SALARY_SLIP_ACKNOWLEDGEMENT -> salarySlipAcknowledgementMaxSize.toBytes();
        };
    }
}
