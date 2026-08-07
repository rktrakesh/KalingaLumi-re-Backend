package com.business.erp.auth.service;

import com.business.erp.auth.entity.User;
import com.business.erp.common.clock.ClockProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final ClockProvider clockProvider;

    public JwtService(ClockProvider clockProvider) {
        this.clockProvider = clockProvider;
    }

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    private final Logger log = LoggerFactory.getLogger(JwtService.class);

    public String generateToken(UserDetails userDetails) {
        log.debug("JwtService:generateToken :: generating token for user={}", userDetails.getUsername());
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof User user) {
            claims.put("tokenVersion", user.getTokenVersion() == null ? 0L : user.getTokenVersion());
            claims.put("role", user.getRole().name());
            claims.put("roles", user.getRoles().stream().map(Enum::name).sorted().toList());
        }
        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> claims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(claims).subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignKey()).compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            if (!extractUsername(token).equals(userDetails.getUsername()) || isTokenExpired(token)) return false;
            if (!(userDetails instanceof User user)) {
                return true;
            }
            long currentVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
            return extractTokenVersion(token) == currentVersion
                    && user.isEnabled()
                    && user.isAccountNonLocked()
                    && user.isCredentialsNonExpired()
                    && temporaryPasswordIsValid(user);
        } catch (Exception e) {
            log.warn("JwtService:isTokenValid :: Invalid token :: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Password change is the recovery path for expired credentials and temporary
     * passwords. Keep signature, subject, token-version and account-state checks,
     * but do not reject the request solely because credentials require renewal.
     */
    public boolean isTokenValidForPasswordChange(String token, UserDetails userDetails) {
        try {
            if (!extractUsername(token).equals(userDetails.getUsername()) || isTokenExpired(token)) return false;
            if (!(userDetails instanceof User user)) {
                return true;
            }
            long currentVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
            return extractTokenVersion(token) == currentVersion
                    && user.isEnabled()
                    && user.isAccountNonLocked();
        } catch (Exception e) {
            log.warn("JwtService:isTokenValidForPasswordChange :: Invalid token :: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public long extractTokenVersion(String token) {
        Number value = extractClaim(token, claims -> claims.get("tokenVersion", Number.class));
        return value == null ? -1L : value.longValue();
    }

    private boolean temporaryPasswordIsValid(User user) {
        return !Boolean.TRUE.equals(user.getMustChangePassword())
                || user.getTemporaryPasswordExpiresAt() == null
                || user.getTemporaryPasswordExpiresAt().isAfter(clockProvider.now());
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(Jwts.parser().verifyWith(getSignKey()).build()
                .parseSignedClaims(token).getPayload());
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
