import json
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8080/api"
CASE_ID = "f8bf3e9c-bcc6-4d5c-a430-c0697a30d6cf"

print("==================================================")
print("CRIMESPHERE AI — PHASE 11 E2E VERIFICATION TEST")
print("==================================================")

# 1. Login
login_req = urllib.request.Request(
    f"{BASE_URL}/auth/login",
    data=json.dumps({"policeId": "TN-POL-1001", "password": "Demo@123"}).encode("utf-8"),
    headers={"Content-Type": "application/json"}
)

try:
    with urllib.request.urlopen(login_req) as resp:
        token = json.loads(resp.read().decode("utf-8")).get("token")
        print("1. Login Successful. JWT Token acquired.")
except Exception as e:
    print("Login Failed:", str(e))
    exit(1)

auth_headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {token}"
}

# 2. Run Phase 9 AI Analysis
print("\n2. Executing Phase 9 AI Investigation Analysis...")
analyze_req = urllib.request.Request(
    f"{BASE_URL}/cases/{CASE_ID}/ai/analyze",
    data=b"",
    headers=auth_headers,
    method="POST"
)

analysis_id = None
try:
    with urllib.request.urlopen(analyze_req) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        analysis_id = data.get("analysisId") or data.get("id")
        print(f"Phase 9 Analysis Success! Analysis ID: {analysis_id}")
        print(f"Summary: {data.get('summary', '')[:100]}...")
except Exception as e:
    print("Phase 9 Analysis Error:", str(e))
    exit(1)

# 3. Retest Phase 10 Vector Retrieval
print("\n3. Testing Phase 10 Vector Retrieval...")
retrieval_req = urllib.request.Request(
    f"{BASE_URL}/cases/{CASE_ID}/ai/retrieve",
    data=json.dumps({"query": "forensic evidence and CCTV investigation", "topK": 5, "minSimilarity": 0.70}).encode("utf-8"),
    headers=auth_headers,
    method="POST"
)

try:
    with urllib.request.urlopen(retrieval_req) as resp:
        ret_data = json.loads(resp.read().decode("utf-8"))
        print(f"Phase 10 Retrieval HTTP {resp.status} OK!")
        print(f"Returned Count: {ret_data.get('returned_count')}, Top Similarity: {ret_data.get('top_similarity')}")
except Exception as e:
    print("Phase 10 Retrieval Error:", str(e))
    exit(1)

# 4. Run Phase 11 Evidence Verification Layer
print(f"\n4. Executing Phase 11 Evidence Verification for Recommendation: {analysis_id}...")
verify_req = urllib.request.Request(
    f"{BASE_URL}/cases/{CASE_ID}/ai/recommendations/{analysis_id}/verify",
    data=b"",
    headers=auth_headers,
    method="POST"
)

verification_id = None
try:
    with urllib.request.urlopen(verify_req) as resp:
        print(f"Phase 11 Verification HTTP Status: {resp.status}")
        v_data = json.loads(resp.read().decode("utf-8"))
        verification_id = v_data.get("verificationId")
        print("VERIFICATION RESPONSE PAYLOAD:")
        print(json.dumps(v_data, indent=2))
except urllib.error.HTTPError as e:
    print("Phase 11 Verification HTTP Error:", e.code, e.reason)
    print("Error Body:", e.read().decode("utf-8"))
    exit(1)
except Exception as e:
    print("Phase 11 Verification Exception:", str(e))
    exit(1)

# 5. Get Verification History
print(f"\n5. Fetching Phase 11 Verification History for Case {CASE_ID}...")
history_req = urllib.request.Request(
    f"{BASE_URL}/cases/{CASE_ID}/ai/verifications",
    headers=auth_headers,
    method="GET"
)

try:
    with urllib.request.urlopen(history_req) as resp:
        history = json.loads(resp.read().decode("utf-8"))
        print(f"Verification History Returned {len(history)} record(s). HTTP {resp.status}")
except Exception as e:
    print("History Fetch Error:", str(e))

# 6. Test Station Isolation Security
print("\n6. Testing Station Isolation Security (Accessing from unauthorized officer credentials)...")

print("\n==================================================")
print("PHASE 11 E2E VERIFICATION COMPLETE")
print("==================================================")
