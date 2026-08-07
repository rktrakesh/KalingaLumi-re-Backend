package com.business.erp.auth.repository;

import com.business.erp.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.employee")
    List<User> findAllWithEmployee();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.employee WHERE u.id = :id")
    Optional<User> findByIdWithEmployee(@Param("id") Long id);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.employee " +
            "WHERE LOWER(u.username) = LOWER(:username)")
    Optional<User> findByUsernameIgnoreCase(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.employee " +
            "WHERE LOWER(u.username) = LOWER(:username)")
    List<User> findAllByUsernameIgnoreCaseWithEmployee(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.employee e " +
            "WHERE LOWER(e.employeeCode) = LOWER(:employeeCode)")
    List<User> findAllByEmployeeCodeIgnoreCaseWithEmployee(
            @Param("employeeCode") String employeeCode);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles assignedRole WHERE assignedRole = :role")
    List<User> findByRole(@Param("role") User.Role role);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles assignedRole " +
            "WHERE u.status = :status AND assignedRole IN :roles")
    List<User> findByStatusAndRoleIn(@Param("status") User.UserStatus status,
                                     @Param("roles") List<User.Role> roles);

    Optional<User> findByEmployee_Id(Long employeeId);
}
