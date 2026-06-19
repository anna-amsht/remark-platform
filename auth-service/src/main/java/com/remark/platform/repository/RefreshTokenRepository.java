package com.remark.platform.repository;

import com.remark.platform.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revokedAt = :revokedAt
            WHERE rt.tokenHash = :tokenHash
              AND rt.revokedAt IS NULL
            """)
    int revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revokedAt = :revokedAt
            WHERE rt.userId = :userId
              AND rt.revokedAt IS NULL
            """)
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
