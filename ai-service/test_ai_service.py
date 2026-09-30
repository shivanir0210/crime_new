import os
import sys
import uuid
from unittest.mock import patch, MagicMock

# Add parent directory to python path
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))

from fastapi.testclient import TestClient
from app.main import app
from app.config import get_settings

client = TestClient(app)
settings = get_settings()
API_KEY = settings.AI_SERVICE_API_KEY

SAMPLE_CASE_PAYLOAD = {
    "caseId": "c1111111-9c0b-4ef8-bb6d-6bb9bd380001",
    "caseNumber": "CS-CBE-2026-0001",
    "caseTitle": "Commercial Jewelry Burglary",
    "caseType": "THEFT",
    "caseStatus": "UNDER_INVESTIGATION",
    "investigationState": "EVIDENCE_UNDER_REVIEW",
    "investigationCompleteness": 83,
    "fir": {
        "fir_number": "FIR-CBE-2026-0001",
        "police_station": "Coimbatore Central",
        "registration_date": "2026-09-01T10:00:00Z",
        "incident_location": "Cross Cut Road, Gandhipuram",
        "offence_type": "THEFT",
        "sections_of_law": "IPC 380, IPC 457",
        "description": "Store broken into through rear shutter after midnight."
    },
    "victims": [
        {
            "full_name": "R. Sundaram",
            "relationship_to_case": "Store Owner",
            "status": "ACTIVE"
        }
    ],
    "suspects": [
        {
            "full_name": "K. Mani",
            "alias": "Minnal",
            "investigation_status": "UNDER_INVESTIGATION"
        }
    ],
    "evidence": [
        {
            "evidence_number": "EVD-CBE-2026-0001",
            "title": "CCTV Footage - Rear Alley",
            "evidence_type": "CCTV",
            "status": "UNDER_REVIEW"
        }
    ],
    "evidenceGaps": [
        {
            "category": "FORENSIC",
            "title": "Fingerprint lifting report pending",
            "severity": "HIGH",
            "status": "OPEN"
        }
    ]
}

MOCK_GROQ_LLM_OUTPUT = {
    "summary": "Commercial burglary inquiry at Cross Cut Road jewelry shop involving forced entry through rear shutter.",
    "key_observations": [
        {
            "observation": "CCTV footage recorded a masked suspect in the rear alley at 01:15 AM.",
            "evidence_references": ["EVD-CBE-2026-0001", "EVD-CBE-2026-9999"],  # EVD-CBE-2026-9999 is invalid!
            "reasoning": "Establishes approximate time of forced entry."
        }
    ],
    "potential_leads": [
        {
            "lead": "Analyze lockpick tool for latent ridge impressions or DNA.",
            "reasoning": "Tool recovered directly from entry point.",
            "priority": "HIGH",
            "evidence_references": ["EVD-CBE-2026-0001"]
        }
    ],
    "evidence_gaps": [
        {
            "gap": "Fingerprint lifting forensic report",
            "reason": "Required to match physical impressions against state database.",
            "severity": "HIGH"
        }
    ],
    "contradictions": [],
    "recommended_actions": [
        {
            "action": "Dispatch physical lockpick tool to forensic ballistics/fingerprint lab.",
            "reason": "Crucial to corroborate suspect identity.",
            "priority": "HIGH",
            "evidence_references": ["EVD-CBE-2026-0001"]
        }
    ],
    "uncertainties": [
        "Suspect identity cannot be established solely from rear alley CCTV due to facial covering."
    ],
    "limitations": [
        "Advisory intelligence only. Investigator verification required."
    ]
}


def test_health_endpoint():
    response = client.get("/api/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "CrimeSphere AI Service"


def test_analysis_missing_auth_header():
    response = client.post("/api/analysis/case", json=SAMPLE_CASE_PAYLOAD)
    assert response.status_code == 401
    assert "Missing required X-AI-Service-Key" in response.json()["detail"]


def test_analysis_invalid_auth_header():
    response = client.post(
        "/api/analysis/case",
        json=SAMPLE_CASE_PAYLOAD,
        headers={"X-AI-Service-Key": "wrong-key"}
    )
    assert response.status_code == 401
    assert "Invalid X-AI-Service-Key" in response.json()["detail"]


def test_analysis_missing_groq_api_key_error_handling():
    with patch("app.services.groq_service.get_settings") as mock_settings, \
         patch.dict(os.environ, {"GROQ_API_KEY": ""}):
        mock_conf = MagicMock()
        mock_conf.GROQ_API_KEY = ""
        mock_conf.GROQ_MODEL = "groq/compound-mini"
        mock_conf.GROQ_TIMEOUT_SECONDS = 60
        mock_settings.return_value = mock_conf

        response = client.post(
            "/api/analysis/case",
            json=SAMPLE_CASE_PAYLOAD,
            headers={"X-AI-Service-Key": API_KEY}
        )
        assert response.status_code == 503
        detail = response.json()["detail"]
        assert detail["error"] == "AI_ANALYSIS_CONFIG_ERROR"


@patch("app.services.groq_service.GroqService.generate_investigation_analysis")
def test_analysis_valid_groq_mocked_analysis(mock_groq):
    mock_groq.return_value = MOCK_GROQ_LLM_OUTPUT

    response = client.post(
        "/api/analysis/case",
        json=SAMPLE_CASE_PAYLOAD,
        headers={"X-AI-Service-Key": API_KEY}
    )
    assert response.status_code == 200
    data = response.json()

    assert data["caseId"] == "c1111111-9c0b-4ef8-bb6d-6bb9bd380001"
    assert data["caseNumber"] == "CS-CBE-2026-0001"
    assert data["analysisVersion"] == "phase-9"
    assert data["status"] == "REVIEW_REQUIRED"
    assert "Commercial burglary inquiry" in data["summary"]

    # Verify Evidence Traceability Filter: EVD-CBE-2026-9999 MUST BE FILTERED OUT!
    obs = data["keyObservations"][0]
    assert "EVD-CBE-2026-0001" in obs["evidenceReferences"]
    assert "EVD-CBE-2026-9999" not in obs["evidenceReferences"]


def test_analysis_invalid_payload_validation():
    payload = {"caseTitle": "Incomplete Payload"}
    response = client.post(
        "/api/analysis/case",
        json=payload,
        headers={"X-AI-Service-Key": API_KEY}
    )
    assert response.status_code == 422
