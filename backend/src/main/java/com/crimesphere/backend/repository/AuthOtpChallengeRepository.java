package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.AuthOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthOtpChallengeRepository extends JpaRepository<AuthOtpChallenge, UUID> {

    @Query("SELECT c FROM AuthOtpChallenge c WHERE c.id = :id AND c.isUsed = false AND c.isInvalidated = false")
    Optional<AuthOtpChallenge> findActiveById(@Param("id") UUID id);

    @Query("SELECT c FROM AuthOtpChallenge c WHERE c.officer.id = :officerId AND c.isUsed = false AND c.isInvalidated = false ORDER BY c.createdAt DESC")
    List<AuthOtpChallenge> findActiveByOfficerId(@Param("officerId") UUID officerId);

    @Query("SELECT COUNT(c) FROM AuthOtpChallenge c WHERE c.officer.id = :officerId AND c.createdAt >= :since")
    long countChallengesCreatedSince(@Param("officerId") UUID officerId, @Param("since") OffsetDateTime since);
}
