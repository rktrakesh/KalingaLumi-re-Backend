package com.business.erp.auth.repository;

import com.business.erp.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(User.Role role);

    List<User> findByStatusAndRoleIn(User.UserStatus status, List<User.Role> roles);
}
