package com.business.erp.auth.entity;

import com.business.erp.common.audit.AuditableEntity;
import com.business.erp.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Role role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private Set<Role> roles = new LinkedHashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * Set when the account is auto-locked after MAX_FAILED_LOGIN_ATTEMPTS. Null = not locked.
     */
    @Column(name = "locked_at")
    private java.time.LocalDateTime lockedAt;

    @Column(name = "temporary_password_issued_at")
    private java.time.LocalDateTime temporaryPasswordIssuedAt;
    @Column(name = "temporary_password_expires_at")
    private java.time.LocalDateTime temporaryPasswordExpiresAt;
    @Column(name = "last_login_at")
    private java.time.LocalDateTime lastLoginAt;
    @Column(name = "credentials_expired", nullable = false)
    @Builder.Default
    private Boolean credentialsExpired = false;
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Long tokenVersion = 0L;

    public Long getEmployeeId() {
        return employee != null ? employee.getId() : null;
    }

    public Set<Role> getRoles() {
        if (roles == null) {
            roles = new LinkedHashSet<>();
        }
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roles);
    }

    public static Role determineCompatibilityRole(Collection<Role> assignedRoles) {
        if (assignedRoles == null || assignedRoles.isEmpty()) {
            throw new IllegalStateException("A user must have at least one authoritative role");
        }
        List<Role> precedence = List.of(
                Role.ROLE_ADMIN,
                Role.ROLE_MANAGER,
                Role.ROLE_SUPERVISOR,
                Role.ROLE_HR,
                Role.ROLE_FINANCE,
                Role.ROLE_SALES,
                Role.ROLE_EMPLOYEE);
        return precedence.stream()
                .filter(assignedRoles::contains)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A user has no supported role"));
    }

    @PrePersist
    @PreUpdate
    private void onPreSave() {
        if (this.username != null) {
            this.username = this.username.trim();
        }
        if (getRoles().isEmpty()) {
            throw new IllegalStateException("A user must have at least one authoritative role");
        }
        this.role = determineCompatibilityRole(this.roles);

        if (this.tokenVersion == null) {
            this.tokenVersion = 0L;
        }
        if (this.credentialsExpired == null) {
            this.credentialsExpired = false;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !Boolean.TRUE.equals(credentialsExpired);
    }

    @Override
    public boolean isEnabled() {
        if (status != UserStatus.ACTIVE) {
            return false;
        }
        if (employee == null) {
            return true;
        }
        return employee.getStatus() == Employee.EmployeeStatus.ACTIVE
                || employee.getStatus() == Employee.EmployeeStatus.ON_NOTICE;
    }

    public enum Role {
        ROLE_ADMIN,
        ROLE_MANAGER,
        ROLE_SUPERVISOR,
        ROLE_HR,
        ROLE_FINANCE,
        ROLE_SALES,
        ROLE_EMPLOYEE
    }

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        LOCKED
    }
}
