package com.business.erp.auth.service.impl;

import com.business.erp.auth.dto.request.ChangePasswordRequest;
import com.business.erp.auth.dto.request.LoginRequest;
import com.business.erp.auth.dto.response.TokenResponse;
import com.business.erp.auth.dto.response.UserProfileResponse;
import com.business.erp.auth.entity.RefreshToken;
import com.business.erp.auth.entity.User;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.auth.service.AuthService;
import com.business.erp.auth.service.JwtService;
import com.business.erp.auth.service.RefreshTokenService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final Logger log =  LoggerFactory.getLogger(AuthServiceImpl.class);

    @Override
    public TokenResponse login(LoginRequest request) {
        log.info("AuthServiceImpl:login :: Attempting login for username={}", request.getUsername());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        log.info("AuthServiceImpl:login :: SUCCESS username={} role={}", user.getUsername(), user.getRole());
        return buildTokenResponse(user, accessToken, refreshToken.getToken());
    }

    @Override
    public TokenResponse refreshToken(String token) {
        log.debug("AuthServiceImpl:refreshToken :: Processing refresh token");
        RefreshToken rt = refreshTokenService.verifyRefreshToken(token);
        User user = rt.getUser();
        String accessToken = jwtService.generateToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user).getToken();
        log.info("AuthServiceImpl:refreshToken :: SUCCESS username={}", user.getUsername());
        return buildTokenResponse(user, accessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String username) {
        log.info("AuthServiceImpl:logout :: username={}", username);
        userRepository.findByUsername(username)
                .ifPresent(user -> refreshTokenService.revokeByUserId(user.getId()));
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        log.info("AuthServiceImpl:changePassword :: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("AuthServiceImpl:changePassword :: Current password mismatch for username={}", username);
            throw new BusinessException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeByUserId(user.getId());
        log.info("AuthServiceImpl:changePassword :: SUCCESS username={}", username);
    }

    @Override
    public UserProfileResponse getProfile(String username) {
        log.debug("AuthServiceImpl:getProfile :: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserProfileResponse.builder()
                .id(user.getId()).username(user.getUsername()).fullName(user.getFullName())
                .role(user.getRole().name()).employeeId(user.getEmployeeId()).status(user.getStatus().name())
                .build();
    }

    private TokenResponse buildTokenResponse(User user, String accessToken, String refreshToken) {
        return TokenResponse.builder()
                .accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
                .username(user.getUsername()).fullName(user.getFullName()).role(user.getRole().name())
                .build();
    }
}
