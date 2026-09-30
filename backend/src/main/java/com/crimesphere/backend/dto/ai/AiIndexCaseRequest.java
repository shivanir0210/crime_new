package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AiIndexCaseRequest {

    @JsonProperty("case_id")
    private UUID caseId;

    @JsonProperty("chunks")
    private List<AiKnowledgeChunkDto> chunks = new ArrayList<>();

    public AiIndexCaseRequest() {
    }

    public AiIndexCaseRequest(UUID caseId, List<AiKnowledgeChunkDto> chunks) {
        this.caseId = caseId;
        this.chunks = chunks != null ? chunks : new ArrayList<>();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public List<AiKnowledgeChunkDto> getChunks() {
        return chunks;
    }

    public void setChunks(List<AiKnowledgeChunkDto> chunks) {
        this.chunks = chunks != null ? chunks : new ArrayList<>();
    }
}
