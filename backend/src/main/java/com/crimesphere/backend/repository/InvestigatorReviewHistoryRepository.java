package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.InvestigatorReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvestigatorReviewHistoryRepository extends JpaRepository<InvestigatorReviewHistory, UUID> {

    List<InvestigatorReviewHistory> findByInvestigatorReviewIdOrderByCreatedAtDesc(UUID investigatorReviewId);

    void deleteByInvestigatorReviewId(UUID investigatorReviewId);
}
