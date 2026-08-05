package com.business.erp.employee.validation;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.employee.entity.DesignationMaster;
import com.business.erp.employee.entity.EmployeeCategoryMaster;
import org.springframework.stereotype.Component;

@Component
public class DesignationCategoryValidator {

    public void validate(DesignationMaster designation, EmployeeCategoryMaster category) {
        if (!designation.getCategory().getId().equals(category.getId())) {
            throw new BusinessException("DESIGNATION_CATEGORY_MISMATCH: designation '" + designation.getName() +
                    "' belongs to category '" + designation.getCategory().getName() + "', not '" + category.getName() + "'");
        }
    }
}