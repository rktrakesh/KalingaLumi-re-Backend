package com.business.erp.auth.service.impl;

import com.business.erp.auth.entity.RefreshToken;
import com.business.erp.auth.entity.User;
import com.business.erp.auth.repository.RefreshTokenRepository;
import com.business.erp.auth.service.RefreshTokenService;
import com.business.erp.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        log.debug("RefreshTokenServiceImpl:createRefreshToken :: userId={}", user.getId());
        refreshTokenRepository.revokeAllByUserId(user.getId());
        RefreshToken rt = refreshTokenRepository.save(RefreshToken.builder()
                .user(user).token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build());
        log.debug("RefreshTokenServiceImpl:createRefreshToken :: SUCCESS userId={}", user.getId());
        return rt;
    }

    @Override
    public RefreshToken verifyRefreshToken(String token) {
        log.debug("RefreshTokenServiceImpl:verifyRefreshToken :: Verifying token");
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("RefreshTokenServiceImpl:verifyRefreshToken :: Token not found");
                    return new BusinessException("Refresh token not found");
                });
        if (rt.isRevoked()) {
            log.warn("RefreshTokenServiceImpl:verifyRefreshToken :: Token revoked userId={}", rt.getUser().getId());
            throw new BusinessException("Refresh token has been revoked");
        }
        if (rt.isExpired()) {
            refreshTokenRepository.delete(rt);
            log.warn("RefreshTokenServiceImpl:verifyRefreshToken :: Token expired userId={}", rt.getUser().getId());
            throw new BusinessException("Refresh token expired. Please login again.");
        }
        return rt;
    }

    @Override
    @Transactional
    public void revokeByUserId(Long userId) {
        log.debug("RefreshTokenServiceImpl:revokeByUserId :: userId={}", userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
