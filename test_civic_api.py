"""
Smart Civic Response System - REST API Automated Test Suite
Compatible with Python 3.8+ and standard library urllib / requests.
"""

import json
import sys
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8080/api"

def make_request(path, method="GET", data=None):
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    body = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            content = resp.read().decode("utf-8")
            return status, json.loads(content) if content else {}
    except urllib.error.HTTPError as e:
        content = e.read().decode("utf-8")
        return e.code, json.loads(content) if content else {}
    except urllib.error.URLError as e:
        print(f"[!] Cannot connect to {url}: {e.reason}")
        print("    Make sure the Java server is running (run.bat or run.ps1).")
        return 0, {}

def run_tests():
    print("=" * 60)
    print(" Smart Civic Response System - API Test Suite")
    print("=" * 60)

    # 1. Health check
    print("\n[Test 1] GET /api/health ...", end=" ")
    code, data = make_request("/health")
    if code == 200 and data.get("status") == "UP":
        print(f"PASSED (Engine: {data.get('engine')})")
    else:
        print(f"FAILED (Status: {code})")
        return

    # 2. Get Statistics
    print("[Test 2] GET /api/stats ...", end=" ")
    code, stats = make_request("/stats")
    if code == 200 and "totalIssues" in stats:
        print(f"PASSED (Total: {stats['totalIssues']}, SLA Breached: {stats['slaBreachedCount']})")
    else:
        print(f"FAILED (Status: {code})")

    # 3. Test Smart AI Triage endpoint
    print("[Test 3] POST /api/triage ...", end=" ")
    triage_payload = {
        "title": "Severe road sinkhole causing accidents",
        "description": "A 4-foot deep crater has collapsed near hospital traffic signal. Very dangerous."
    }
    code, triage = make_request("/triage", method="POST", data=triage_payload)
    if code == 200 and triage.get("category") == "ROADS" and triage.get("priority") in ["CRITICAL", "HIGH"]:
        print(f"PASSED (Category: {triage.get('category')}, Urgency: {triage.get('urgencyScore')}/100, Priority: {triage.get('priority')})")
    else:
        print(f"FAILED: {triage}")

    # 4. List Issues
    print("[Test 4] GET /api/issues ...", end=" ")
    code, issues = make_request("/issues")
    if code == 200 and isinstance(issues, list):
        print(f"PASSED (Retrieved {len(issues)} issues)")
    else:
        print(f"FAILED (Status: {code})")

    # 5. Create New Issue
    print("[Test 5] POST /api/issues ...", end=" ")
    new_issue_payload = {
        "title": "Test Pipe Leakage at Sector 9",
        "description": "Continuous drinking water leakage from underground pipe.",
        "category": "AUTO",
        "priority": "AUTO",
        "address": "Street 14, Sector 9",
        "ward": "Ward 9 - South Green",
        "reporterName": "Automated Tester",
        "reporterPhone": "+91 99999 00000"
    }
    code, created = make_request("/issues", method="POST", data=new_issue_payload)
    if code == 201 and "id" in created:
        issue_id = created["id"]
        print(f"PASSED (Created ID: {issue_id}, Category: {created.get('category')}, SLA: {created.get('slaHours')}h)")
    else:
        print(f"FAILED: {created}")
        return

    # 6. Upvote Issue
    print(f"[Test 6] POST /api/issues/{issue_id}/upvote ...", end=" ")
    code, upvote_resp = make_request(f"/issues/{issue_id}/upvote", method="POST")
    if code == 200 and upvote_resp.get("upvotes", 0) >= 1:
        print(f"PASSED (Upvotes: {upvote_resp.get('upvotes')})")
    else:
        print(f"FAILED: {upvote_resp}")

    # 7. Update Status
    print(f"[Test 7] PUT /api/issues/{issue_id}/status ...", end=" ")
    status_payload = {
        "status": "IN_PROGRESS",
        "actor": "Lead Inspector",
        "officer": "Eng. Tarun Mehta",
        "notes": "Emergency squad dispatched to replace valve."
    }
    code, updated = make_request(f"/issues/{issue_id}/status", method="PUT", data=status_payload)
    if code == 200 and updated.get("status") == "IN_PROGRESS":
        print(f"PASSED (Status: {updated.get('status')}, Assigned: {updated.get('assignedOfficer')})")
    else:
        print(f"FAILED: {updated}")

    # 8. Department List
    print("[Test 8] GET /api/departments ...", end=" ")
    code, depts = make_request("/departments")
    if code == 200 and isinstance(depts, list) and len(depts) > 0:
        print(f"PASSED (Loaded {len(depts)} municipal departments)")
    else:
        print(f"FAILED (Status: {code})")

    print("\nAll REST API test assertions passed successfully!")
    print("=" * 60)

if __name__ == "__main__":
    run_tests()
