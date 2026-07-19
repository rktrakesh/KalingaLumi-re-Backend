package com.business.erp.auth.service;

import com.business.erp.auth.entity.RefreshToken;
import com.business.erp.auth.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);

    RefreshToken verifyRefreshToken(String token);

    void revokeByUserId(Long userId);
}
