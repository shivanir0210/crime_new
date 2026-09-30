import uuid
from typing import Set, List, Dict, Any
from app.schemas.analysis import (
    CaseAnalysisRequest,
    InvestigationAnalysisResponse,
    AiKeyObservationSchema,
    AiPotentialLeadSchema,
    AiEvidenceGapAnalysisSchema,
    AiContradictionSchema,
    AiRecommendedActionSchema,
)
from app.services.groq_service import GroqService
from app.services.investigation_prompt import SYSTEM_PROMPT, SYSTEM_PROMPT_VERSION
from app.config import get_settings
from app.core.logging_config import logger

class InvestigationAgentService:
    """
    Phase 9 Investigation Agent Service.
    Compiles minimized case payload, requests Groq LLM reasoning, enforces evidence traceability validation,
    and returns validated InvestigationAnalysisResponse.
    """

    def __init__(self):
        self.groq_service = GroqService()
        self.settings = get_settings()

    def analyze_case(self, request: CaseAnalysisRequest) -> InvestigationAnalysisResponse:
        request_id = str(uuid.uuid4())
        case_id_str = str(request.caseId)
        case_number_str = str(request.caseNumber)

        logger.info("Executing Phase 9 Groq AI investigation reasoning [RequestID: %s, CaseID: %s, CaseNumber: %s]",
                    request_id, case_id_str, case_number_str)

        # 1. Build Data-Minimized User Prompt Context
        user_prompt = self._build_case_context_prompt(request)

        # 2. Call GroqService
        raw_response = self.groq_service.generate_investigation_analysis(
            SYSTEM_PROMPT, user_prompt, request_id=request_id, case_id=case_id_str
        )

        # 3. Extract Valid Case Evidence Numbers for Traceability Validation
        valid_evidence_numbers: Set[str] = self._extract_valid_evidence_numbers(request.evidence)

        # 4. Parse & Validate Findings with Evidence Traceability Filter
        key_obs = self._parse_key_observations(raw_response.get("key_observations", []), valid_evidence_numbers)
        leads = self._parse_potential_leads(raw_response.get("potential_leads", []), valid_evidence_numbers)
        gaps = self._parse_evidence_gaps(raw_response.get("evidence_gaps", []))
        contradictions = self._parse_contradictions(raw_response.get("contradictions", []), valid_evidence_numbers)
        actions = self._parse_recommended_actions(raw_response.get("recommended_actions", []), valid_evidence_numbers)
        uncertainties = self._parse_string_list(raw_response.get("uncertainties", []))
        limitations = self._parse_string_list(raw_response.get("limitations", []))

        # Default summary fallback if missing
        summary = raw_response.get("summary")
        if not summary or not isinstance(summary, str):
            summary = f"Investigative dossier evaluation completed for case {case_number_str}."

        # Always append responsible AI limitation disclaimer
        standard_limitation = "AI recommendations are decision-support outputs and require investigator validation before operational enforcement."
        if standard_limitation not in limitations:
            limitations.append(standard_limitation)

        return InvestigationAnalysisResponse(
            requestId=request_id,
            caseId=case_id_str,
            caseNumber=case_number_str,
            analysisVersion="phase-9",
            modelName=self.groq_service.model,
            promptVersion=SYSTEM_PROMPT_VERSION,
            status="REVIEW_REQUIRED",
            summary=summary,
            keyObservations=key_obs,
            potentialLeads=leads,
            evidenceGaps=gaps,
            contradictions=contradictions,
            recommendedActions=actions,
            uncertainties=uncertainties,
            limitations=limitations,
            analysisAvailable=True
        )

    def _build_case_context_prompt(self, request: CaseAnalysisRequest) -> str:
        """
        Formats case dossier data into structured text prompt.
        """
        lines = [
            "=================== CURRENT CASE CONTEXT (AUTHORITATIVE FACTS) ===================",
            f"Case ID: {request.caseId}",
            f"Case Number: {request.caseNumber}",
            f"Case Title: {request.caseTitle or request.title or 'N/A'}",
            f"Case Type: {request.caseType or 'N/A'}",
            f"Case Status: {request.caseStatus or request.status or 'N/A'}",
            f"Current Investigation State: {request.investigationState or 'N/A'}",
            f"Completeness Indicator: {request.investigationCompleteness}%",
            ""
        ]

        if request.fir:
            lines.extend([
                "### FIRST INFORMATION REPORT (FIR)",
                f"FIR Number: {request.fir.get('fir_number') or request.fir.get('firNumber') or 'N/A'}",
                f"Police Station: {request.fir.get('police_station') or request.fir.get('policeStation') or 'N/A'}",
                f"Incident Location: {request.fir.get('incident_location') or request.fir.get('incidentLocation') or 'N/A'}",
                f"Offence Type: {request.fir.get('offence_type') or request.fir.get('offenceType') or 'N/A'}",
                f"Sections of Law: {request.fir.get('sections_of_law') or request.fir.get('sectionsOfLaw') or 'N/A'}",
                f"FIR Description: {request.fir.get('description') or 'N/A'}",
                ""
            ])

        if request.victims:
            lines.append("### VICTIMS / COMPLAINANTS")
            for idx, v in enumerate(request.victims, 1):
                lines.append(f"{idx}. Name: {v.get('full_name') or v.get('fullName') or 'N/A'} | Relationship: {v.get('relationship_to_case') or v.get('relationshipToCase') or 'N/A'} | Status: {v.get('status') or 'N/A'}")
            lines.append("")

        if request.suspects:
            lines.append("### SUSPECTS / PERSONS OF INTEREST")
            for idx, s in enumerate(request.suspects, 1):
                lines.append(f"{idx}. Name: {s.get('full_name') or s.get('fullName') or 'N/A'} | Alias: {s.get('alias') or 'None'} | Status: {s.get('investigation_status') or s.get('investigationStatus') or 'N/A'}")
            lines.append("")

        if request.evidence:
            lines.append("### EVIDENCE ITEMS (COLLECTED & VERIFIED)")
            for idx, e in enumerate(request.evidence, 1):
                ev_num = e.get('evidence_number') or e.get('evidenceNumber') or f"EVD-{idx}"
                ev_title = e.get('title') or 'Untitled Evidence'
                ev_type = e.get('evidence_type') or e.get('evidenceType') or 'OTHER'
                ev_status = e.get('status') or 'N/A'
                ev_desc = e.get('description') or 'N/A'
                lines.append(f"{idx}. [{ev_num}] Title: {ev_title} | Type: {ev_type} | Status: {ev_status} | Description: {ev_desc}")
            lines.append("")

        if request.evidenceGaps:
            lines.append("### EXISTING EVIDENCE GAPS")
            for idx, g in enumerate(request.evidenceGaps, 1):
                lines.append(f"{idx}. Category: {g.get('category')} | Title: {g.get('title')} | Severity: {g.get('severity')} | Reason: {g.get('reason') or 'N/A'}")
            lines.append("")

        if request.retrievedSources:
            lines.append("=================== HISTORICAL RETRIEVED CONTEXT (UNTRUSTED REFERENCE DATA) ===================")
            lines.append("NOTICE: The following items are retrieved historical reference cases. They are NOT current case facts or current evidence.")
            for idx, r in enumerate(request.retrievedSources, 1):
                s_type = r.get("source_type") or r.get("sourceType") or "HISTORICAL"
                case_num = r.get("case_number") or r.get("caseNumber") or "N/A"
                sim = r.get("similarity") or 0.0
                content = r.get("content") or r.get("content_excerpt") or ""
                lines.append(f"{idx}. [HISTORICAL | {s_type} | Case: {case_num} | Similarity: {sim:.2f}] Excerpt: {content}")
            lines.append("")

        lines.append("Analyze the supplied case dossier and return the structured analysis JSON according to system prompt instructions.")
        return "\n".join(lines)

    def _extract_valid_evidence_numbers(self, evidence_list: List[Dict[str, Any]]) -> Set[str]:
        valid_refs = set()
        if not evidence_list:
            return valid_refs
        for ev in evidence_list:
            ev_num = ev.get('evidence_number') or ev.get('evidenceNumber')
            if ev_num:
                valid_refs.add(str(ev_num).strip())
            ev_id = ev.get('id')
            if ev_id:
                valid_refs.add(str(ev_id).strip())
        return valid_refs

    def _filter_evidence_refs(self, refs: List[Any], valid_refs: Set[str]) -> List[str]:
        if not refs or not isinstance(refs, list):
            return []
        filtered = []
        for r in refs:
            if not r:
                continue
            r_str = str(r).strip()
            if r_str in valid_refs:
                filtered.append(r_str)
            else:
                logger.warning("Stripped invalid/hallucinated evidence reference '%s' from AI finding", r_str)
        return filtered

    def _parse_key_observations(self, items: List[Dict[str, Any]], valid_refs: Set[str]) -> List[AiKeyObservationSchema]:
        result = []
        if not isinstance(items, list):
            return result
        for item in items:
            if isinstance(item, dict) and "observation" in item:
                refs = self._filter_evidence_refs(item.get("evidence_references") or item.get("evidenceReferences"), valid_refs)
                result.append(AiKeyObservationSchema(
                    observation=str(item["observation"]),
                    evidenceReferences=refs,
                    reasoning=item.get("reasoning")
                ))
        return result

    def _parse_potential_leads(self, items: List[Dict[str, Any]], valid_refs: Set[str]) -> List[AiPotentialLeadSchema]:
        result = []
        if not isinstance(items, list):
            return result
        for item in items:
            if isinstance(item, dict) and "lead" in item:
                refs = self._filter_evidence_refs(item.get("evidence_references") or item.get("evidenceReferences"), valid_refs)
                priority = str(item.get("priority", "MEDIUM")).upper()
                if priority not in ("LOW", "MEDIUM", "HIGH"):
                    priority = "MEDIUM"
                result.append(AiPotentialLeadSchema(
                    lead=str(item["lead"]),
                    reasoning=item.get("reasoning"),
                    evidenceReferences=refs,
                    priority=priority
                ))
        return result

    def _parse_evidence_gaps(self, items: List[Dict[str, Any]]) -> List[AiEvidenceGapAnalysisSchema]:
        result = []
        if not isinstance(items, list):
            return result
        for item in items:
            if isinstance(item, dict) and "gap" in item:
                severity = str(item.get("severity", "MEDIUM")).upper()
                if severity not in ("LOW", "MEDIUM", "HIGH"):
                    severity = "MEDIUM"
                result.append(AiEvidenceGapAnalysisSchema(
                    gap=str(item["gap"]),
                    reason=item.get("reason"),
                    severity=severity
                ))
        return result

    def _parse_contradictions(self, items: List[Dict[str, Any]], valid_refs: Set[str]) -> List[AiContradictionSchema]:
        result = []
        if not isinstance(items, list):
            return result
        for item in items:
            if isinstance(item, dict) and "description" in item:
                refs = self._filter_evidence_refs(item.get("evidence_references") or item.get("evidenceReferences"), valid_refs)
                result.append(AiContradictionSchema(
                    description=str(item["description"]),
                    evidenceReferences=refs
                ))
        return result

    def _parse_recommended_actions(self, items: List[Dict[str, Any]], valid_refs: Set[str]) -> List[AiRecommendedActionSchema]:
        result = []
        if not isinstance(items, list):
            return result
        for item in items:
            if isinstance(item, dict) and "action" in item:
                refs = self._filter_evidence_refs(item.get("evidence_references") or item.get("evidenceReferences"), valid_refs)
                priority = str(item.get("priority", "MEDIUM")).upper()
                if priority not in ("LOW", "MEDIUM", "HIGH"):
                    priority = "MEDIUM"
                result.append(AiRecommendedActionSchema(
                    action=str(item["action"]),
                    reason=item.get("reason"),
                    priority=priority,
                    evidenceReferences=refs
                ))
        return result

    def _parse_string_list(self, items: List[Any]) -> List[str]:
        if not isinstance(items, list):
            return []
        return [str(i) for i in items if i and isinstance(i, (str, int, float))]
