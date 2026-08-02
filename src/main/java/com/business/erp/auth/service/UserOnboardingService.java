package com.business.erp.auth.service;

import com.business.erp.auth.entity.User;
import com.business.erp.employee.entity.Employee;

public interface UserOnboardingService {

    User onboard(Employee employee, String actor);
}