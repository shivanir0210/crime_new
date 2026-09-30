import os
import sys
import json
import urllib.request
import urllib.error

API_KEY = "crime-sphere-internal-ai-key-2026-prod"
FASTAPI_URL = "http://localhost:8000/api/analysis/case"

payload = {
    "caseId": "f8bf3e9c-bcc6-4d5c-a430-c0697a30d6cf",
    "caseNumber": "CS-2026-0100",
    "caseTitle": "Test Investigation Case",
    "caseType": "MURDER",
    "caseStatus": "UNDER_INVESTIGATION",
    "investigationState": "EVIDENCE_UNDER_REVIEW",
    "investigationCompleteness": 75,
    "fir": {
        "fir_number": "FIR-CBE-2026-0100",
        "police_station": "Coimbatore Central",
        "registration_date": "2026-09-01T10:00:00Z",
        "incident_location": "Coimbatore Park",
        "offence_type": "MURDER",
        "sections_of_law": "IPC 302",
        "description": "Victim discovered in park area."
    },
    "victims": [
        {
            "full_name": "John Doe",
            "relationship_to_case": "Deceased",
            "status": "ACTIVE"
        }
    ],
    "suspects": [],
    "evidence": [
        {
            "evidence_number": "EVD-CBE-2026-0100",
            "title": "Weapon Found",
            "evidence_type": "WEAPON",
            "status": "STORED"
        }
    ],
    "evidenceGaps": []
}

headers = {
    "Content-Type": "application/json",
    "X-AI-Service-Key": API_KEY
}

req = urllib.request.Request(FASTAPI_URL, data=json.dumps(payload).encode('utf-8'), headers=headers)

try:
    with urllib.request.urlopen(req) as resp:
        print("STATUS:", resp.status)
        print("BODY:", resp.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("HTTP ERROR CODE:", e.code)
    print("HTTP ERROR REASON:", e.reason)
    print("HTTP ERROR BODY:", e.read().decode('utf-8'))
except Exception as e:
    print("EXCEPT:", str(e))
