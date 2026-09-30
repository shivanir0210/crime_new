import json
import urllib.request
import urllib.error

LOGIN_URL = "http://localhost:8080/api/auth/login"
RETRIEVE_URL = "http://localhost:8080/api/cases/f8bf3e9c-bcc6-4d5c-a430-c0697a30d6cf/ai/retrieve"

# 1. Login to get JWT Token
login_payload = {
    "policeId": "TN-POL-1001",
    "password": "Demo@123"
}

login_req = urllib.request.Request(
    LOGIN_URL,
    data=json.dumps(login_payload).encode("utf-8"),
    headers={"Content-Type": "application/json"}
)

try:
    with urllib.request.urlopen(login_req) as resp:
        login_data = json.loads(resp.read().decode("utf-8"))
        token = login_data.get("token")
        print("LOGIN SUCCESSFUL. Token received.")
except Exception as e:
    print("LOGIN FAILED:", str(e))
    exit(1)

# 2. Retest Phase 10 Retrieval Endpoint
retrieve_payload = {
    "query": "forensic evidence and CCTV investigation",
    "topK": 5,
    "minSimilarity": 0.70
}

retrieve_headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {token}"
}

retrieve_req = urllib.request.Request(
    RETRIEVE_URL,
    data=json.dumps(retrieve_payload).encode("utf-8"),
    headers=retrieve_headers,
    method="POST"
)

try:
    with urllib.request.urlopen(retrieve_req) as resp:
        print("RETRIEVE HTTP STATUS:", resp.status)
        body = json.loads(resp.read().decode("utf-8"))
        print("RETRIEVE RESPONSE:")
        print(json.dumps(body, indent=2))
except urllib.error.HTTPError as e:
    print("RETRIEVE HTTP ERROR:", e.code, e.reason)
    print("ERROR BODY:", e.read().decode("utf-8"))
except Exception as e:
    print("RETRIEVE EXCEPTION:", str(e))
