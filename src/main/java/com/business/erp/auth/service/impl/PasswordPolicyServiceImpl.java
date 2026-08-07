package com.business.erp.auth.service.impl;

import com.business.erp.auth.entity.PasswordHistory;
import com.business.erp.auth.entity.User;
import com.business.erp.auth.repository.PasswordHistoryRepository;
import com.business.erp.auth.service.PasswordPolicyService;
import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private static final Pattern SPECIAL_CHAR = Pattern.compile("[^A-Za-z0-9]");

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettingsService settingsService;
    private final ClockProvider clockProvider;
    private final Logger log = LoggerFactory.getLogger(PasswordPolicyServiceImpl.class);

    @Override
    public void validate(String candidatePassword) {
        int minLength = settingsService.getIntValue(SettingKey.PASSWORD_MIN_LENGTH);
        if (candidatePassword == null || candidatePassword.length() < minLength) {
            throw new BusinessException("PASSWORD_TOO_SHORT: password must be at least " + minLength + " characters");
        }
        if (settingsService.getBooleanValue(SettingKey.PASSWORD_REQUIRE_UPPERCASE)
                && candidatePassword.chars().noneMatch(Character::isUpperCase)) {
            throw new BusinessException("PASSWORD_REQUIRES_UPPERCASE: password must contain at least one uppercase letter");
        }
        if (settingsService.getBooleanValue(SettingKey.PASSWORD_REQUIRE_LOWERCASE)
                && candidatePassword.chars().noneMatch(Character::isLowerCase)) {
            throw new BusinessException("PASSWORD_REQUIRES_LOWERCASE: password must contain at least one lowercase letter");
        }
        if (settingsService.getBooleanValue(SettingKey.PASSWORD_REQUIRE_NUMBER)
                && candidatePassword.chars().noneMatch(Character::isDigit)) {
            throw new BusinessException("PASSWORD_REQUIRES_NUMBER: password must contain at least one digit");
        }
        if (settingsService.getBooleanValue(SettingKey.PASSWORD_REQUIRE_SPECIAL_CHAR)
                && !SPECIAL_CHAR.matcher(candidatePassword).find()) {
            throw new BusinessException("PASSWORD_REQUIRES_SPECIAL_CHAR: password must contain at least one special character");
        }
    }

    @Override
    public void validateNotReused(Long userId, String candidatePassword) {
        int historyCount = settingsService.getIntValue(SettingKey.PASSWORD_HISTORY_COUNT);
        if (historyCount <= 0) return;

        List<PasswordHistory> recent = passwordHistoryRepository.findByUserIdOrderByCreatedDateDesc(userId)
                .stream().limit(historyCount).toList();
        boolean reused = recent.stream().anyMatch(h -> passwordEncoder.matches(candidatePassword, h.getPasswordHash()));
        if (reused) {
            throw new BusinessException("PASSWORD_REUSED: cannot reuse any of your last " + historyCount + " password(s)");
        }
    }

    @Override
    @Transactional
    public void recordPasswordChange(User user, String newPasswordHash) {
        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(user).passwordHash(newPasswordHash).createdDate(clockProvider.now())
                .build());
        log.info("PasswordPolicyServiceImpl:recordPasswordChange :: userId={}", user.getId());
    }
}
