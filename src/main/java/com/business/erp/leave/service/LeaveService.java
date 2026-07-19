package com.business.erp.leave.service;

import com.business.erp.leave.dto.request.LeaveRequestDto;
import com.business.erp.leave.dto.response.LeaveBalanceResponse;
import com.business.erp.leave.dto.response.LeaveResponse;
import com.business.erp.leave.entity.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LeaveService {
    LeaveResponse createRequest(LeaveRequestDto request, String createdBy);

    LeaveResponse approve(Long id, String approvedBy);

    LeaveResponse reject(Long id, String rejectionReason, String rejectedBy);

    Page<LeaveResponse> search(Long empId, LeaveRequest.LeaveStatus status, Pageable pageable);

    List<LeaveResponse> getMyLeaves(Long empId);

    LeaveBalanceResponse getBalance(Long empId, int year, int month);

    void allocateMonthlyLeaves(int year, int month);
}