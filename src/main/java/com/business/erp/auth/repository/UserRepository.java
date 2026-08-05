package com.business.erp.auth.repository;

import com.business.erp.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.employee WHERE u.username = :username")
    Optional<User> findByUsernameWithEmployee(@Param("username") String username);

    boolean existsByUsername(String username);

    List<User> findByRole(User.Role role);

    List<User> findByStatusAndRoleIn(User.UserStatus status, List<User.Role> roles);

    Optional<User> findByEmployee_Id(Long employeeId);
}