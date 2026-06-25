package com.remark.platform.repository;

import com.remark.platform.entity.RoleCode;
import com.remark.platform.entity.User;
import com.remark.platform.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.roles
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.roles
            WHERE u.id = :id
            """)
    Optional<User> findByIdWithRoles(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            WHERE u.status = :status
              AND u.branchId = :branchId
              AND r.code IN :roleCodes
            ORDER BY u.createdAt ASC
            """)
    List<User> findPendingByBranchAndRoleCodes(
            @Param("status") UserStatus status,
            @Param("branchId") UUID branchId,
            @Param("roleCodes") Collection<RoleCode> roleCodes
    );

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.roles r
            WHERE u.status = :status
              AND u.branchId = :branchId
              AND r.code = :roleCode
            ORDER BY u.fullName ASC
            """)
    List<User> findActiveByBranchAndRole(
            @Param("status") UserStatus status,
            @Param("branchId") UUID branchId,
            @Param("roleCode") RoleCode roleCode
    );

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.roles r
            WHERE u.status = :status
              AND u.branchId = :branchId
              AND r.code IN :roleCodes
            ORDER BY u.fullName ASC
            """)
    List<User> findActiveByBranchAndRoleCodes(
            @Param("status") UserStatus status,
            @Param("branchId") UUID branchId,
            @Param("roleCodes") Collection<RoleCode> roleCodes
    );
}
