package com.business.erp.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMasterDataRequest {
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be upper-case letters, numbers, and underscores only")
    private String code;
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 255)
    private String description;
}