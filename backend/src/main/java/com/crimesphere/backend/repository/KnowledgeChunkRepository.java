package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findByCaseEntityId(UUID caseId);

    Optional<KnowledgeChunk> findBySourceTypeAndSourceId(String sourceType, UUID sourceId);

    Optional<KnowledgeChunk> findByCaseEntityIdAndSourceTypeAndSourceId(UUID caseId, String sourceType, UUID sourceId);

    @Query("SELECT kc FROM KnowledgeChunk kc JOIN kc.caseEntity c WHERE c.id = :currentCaseId OR (c.retrievalEnabled = true AND c.station.id = :stationId)")
    List<KnowledgeChunk> findRetrievableChunksForStation(@Param("currentCaseId") UUID currentCaseId, @Param("stationId") UUID stationId);

    @Query("SELECT kc FROM KnowledgeChunk kc JOIN kc.caseEntity c WHERE c.retrievalEnabled = true AND c.station.id = :stationId")
    List<KnowledgeChunk> findAllRetrievalEnabledChunksForStation(@Param("stationId") UUID stationId);

    void deleteByCaseEntityId(UUID caseId);

    void deleteByCaseEntityIdAndSourceType(UUID caseId, String sourceType);
}
