package com.business.erp.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Resolves safe request metadata for the existing IAM audit trail. */
@Component
public class IamRequestContextResolver {

    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 500;

    private final boolean trustProxyHeaders;

    public IamRequestContextResolver(
            @Value("${app.security.trust-proxy-headers:false}") boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
    }

    public RequestContext resolve() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return RequestContext.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        String clientIp = sanitize(request.getRemoteAddr(), MAX_IP_LENGTH);
        if (trustProxyHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String forwardedIp = sanitize(forwardedFor.split(",", 2)[0], MAX_IP_LENGTH);
                if (isIpLiteral(forwardedIp)) {
                    clientIp = forwardedIp;
                }
            } else {
                String realIp = sanitize(request.getHeader("X-Real-IP"), MAX_IP_LENGTH);
                if (isIpLiteral(realIp)) {
                    clientIp = realIp;
                }
            }
        }

        String userAgent = sanitize(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH);
        return new RequestContext(clientIp, userAgent, null);
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replace("\r", "").replace("\n", "").trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        return sanitized.substring(0, Math.min(maxLength, sanitized.length()));
    }

    private boolean isIpLiteral(String value) {
        return value != null && value.matches("[0-9A-Fa-f:.]+");
    }

    public record RequestContext(String ipAddress, String userAgent, String device) {
        private static RequestContext empty() {
            return new RequestContext(null, null, null);
        }
    }
}
