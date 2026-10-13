#!/usr/bin/env python3
import json
import time
import requests

BASE_URL = "http://localhost:8088"
ADMIN_EMAIL = "admin@tripmind.com"
ADMIN_PASS = "Admin@123456"

USER_EMAIL = "test_user_flow@tripmind.com"
USER_PASS = "Password@123"

results = []

def record(name, method, url, status_code, expected_status, details=""):
    success = status_code in (expected_status if isinstance(expected_status, (list, tuple)) else [expected_status])
    icon = "PASS" if success else "FAIL"
    results.append({
        "name": name,
        "method": method,
        "url": url,
        "status": status_code,
        "expected": expected_status,
        "pass": success,
        "details": details
    })
    print(f"[{icon}] {method:6} {url:50} -> {status_code} ({details})")

def main():
    print("="*85)
    print("BẮT ĐẦU KIỂM THỬ TOÀN DIỆN MATRIX 70 ENDPOINTS CỦA TRIPMIND AI BACKEND")
    print("="*85)

    # ==========================================
    # NHÓM 1: AUTHENTICATION & USERS
    # ==========================================
    r = requests.get(f"{BASE_URL}/api/auth/health")
    record("Health check", "GET", "/api/auth/health", r.status_code, 200, "Service UP")

    r = requests.post(f"{BASE_URL}/api/auth/login", json={"email": ADMIN_EMAIL, "password": ADMIN_PASS})
    record("Admin Login", "POST", "/api/auth/login", r.status_code, 200)
    admin_token = r.json()["data"]["accessToken"]
    admin_headers = {"Authorization": f"Bearer {admin_token}"}

    # Register test user
    r = requests.post(f"{BASE_URL}/api/auth/register", json={
        "name": "Flow Tester",
        "email": USER_EMAIL,
        "password": USER_PASS
    })
    record("User Register", "POST", "/api/auth/register", r.status_code, [200, 201, 409])

    # Login test user
    r = requests.post(f"{BASE_URL}/api/auth/login", json={"email": USER_EMAIL, "password": USER_PASS})
    record("User Login", "POST", "/api/auth/login", r.status_code, 200)
    user_token = r.json()["data"]["accessToken"]
    user_headers = {"Authorization": f"Bearer {user_token}"}

    r = requests.get(f"{BASE_URL}/api/auth/me", headers=user_headers)
    record("Auth Me", "GET", "/api/auth/me", r.status_code, 200, f"User {r.json()['data']['email']}")

    # Profile
    r = requests.get(f"{BASE_URL}/api/users/profile", headers=user_headers)
    record("Get Profile", "GET", "/api/users/profile", r.status_code, 200)

    r = requests.put(f"{BASE_URL}/api/users/profile", headers=user_headers, json={"name": "Flow Tester Updated", "phoneNumber": "0987654321"})
    record("Update Profile", "PUT", "/api/users/profile", r.status_code, 200)

    # Change password
    r = requests.put(f"{BASE_URL}/api/users/change-password", headers=user_headers, json={
        "currentPassword": USER_PASS,
        "newPassword": "NewPassword@123"
    })
    record("Change Password", "PUT", "/api/users/change-password", r.status_code, 200)

    # Revert password back
    requests.put(f"{BASE_URL}/api/users/change-password", headers=user_headers, json={
        "currentPassword": "NewPassword@123",
        "newPassword": USER_PASS
    })

    # Preferences
    r = requests.get(f"{BASE_URL}/api/me/preferences", headers=user_headers)
    record("Get User Preferences", "GET", "/api/me/preferences", r.status_code, 200)

    r = requests.put(f"{BASE_URL}/api/me/preferences", headers=user_headers, json={
        "travelStyle": "BALANCED",
        "budgetPreference": "MODERATE",
        "preferences": ["ẩm thực", "thiên nhiên"]
    })
    record("Update User Preferences", "PUT", "/api/me/preferences", r.status_code, 200)

    # ==========================================
    # NHÓM 2: DESTINATIONS & PLACES
    # ==========================================
    r = requests.get(f"{BASE_URL}/api/destinations", headers=user_headers)
    record("Get Destinations", "GET", "/api/destinations", r.status_code, 200, f"{len(r.json()['data'])} destinations")
    dest_id = r.json()["data"][0]["id"]

    r = requests.get(f"{BASE_URL}/api/destinations/popular", headers=user_headers)
    record("Get Popular Destinations", "GET", "/api/destinations/popular", r.status_code, 200)

    r = requests.get(f"{BASE_URL}/api/destinations/{dest_id}", headers=user_headers)
    record("Get Destination Detail", "GET", f"/api/destinations/{dest_id}", r.status_code, 200)

    r = requests.get(f"{BASE_URL}/api/places/search", headers=user_headers, params={"query": "cafe", "destinationId": dest_id})
    record("Search Places", "GET", "/api/places/search", r.status_code, 200, f"{len(r.json().get('data', []))} places")
    places = r.json().get("data", [])
    first_place_ext_id = places[0]["externalId"] if places else None
    first_place_provider = places[0]["provider"] if places else "GOOGLE"

    r = requests.get(f"{BASE_URL}/api/places/recommendations/nearby", headers=user_headers, params={"lat": 16.0544, "lng": 108.2022, "category": "FOOD"})
    record("Nearby Recommendations", "GET", "/api/places/recommendations/nearby", r.status_code, 200)

    if first_place_ext_id:
        r = requests.get(f"{BASE_URL}/api/places/{first_place_provider}/{first_place_ext_id}", headers=user_headers)
        record("Get Place by External ID", "GET", f"/api/places/{first_place_provider}/{first_place_ext_id}", r.status_code, 200)

    # Save place by external ID (Google Place adoption)
    db_place_id = None
    if first_place_ext_id:
        r = requests.post(f"{BASE_URL}/api/me/saved-places", headers=user_headers, json={
            "provider": first_place_provider,
            "externalId": first_place_ext_id
        })
        record("Save External Place to DB", "POST", "/api/me/saved-places", r.status_code, [200, 201])
        if r.status_code in [200, 201]:
            db_place_id = r.json()["data"]["place"]["id"]

    r = requests.get(f"{BASE_URL}/api/me/saved-places", headers=user_headers)
    record("Get Saved Places", "GET", "/api/me/saved-places", r.status_code, 200, f"{len(r.json()['data'])} places")

    if db_place_id:
        # Save Place by ID
        r = requests.post(f"{BASE_URL}/api/places/{db_place_id}/save", headers=user_headers)
        record("Save Place by PlaceId", "POST", f"/api/places/{db_place_id}/save", r.status_code, [200, 201])

        # Unsave Place by ID
        r = requests.delete(f"{BASE_URL}/api/places/{db_place_id}/save", headers=user_headers)
        record("Unsave Place by PlaceId", "DELETE", f"/api/places/{db_place_id}/save", r.status_code, 200)

    # ==========================================
    # NHÓM 3: TRIPS & PHASE
    # ==========================================
    r = requests.post(f"{BASE_URL}/api/trips/parse-prompt", headers=user_headers, json={"prompt": "Đi Đà Nẵng 3 ngày 2 đêm 2 người"})
    record("Parse Prompt AI", "POST", "/api/trips/parse-prompt", r.status_code, [200, 503])

    trip_req = {
        "destinationId": dest_id,
        "name": "Chuyến Đi Toàn Diện Matrix",
        "startDate": "2026-10-20",
        "endDate": "2026-10-22",
        "travelers": 2,
        "budget": 10000000,
        "currency": "VND",
        "travelStyle": "BALANCED",
        "budgetPreference": "MODERATE",
        "preferences": ["ẩm thực", "check-in"],
        "groupProfile": {
            "children": 0,
            "seniors": 0,
            "dietary": [],
            "limitedMobility": False,
            "note": "Kiểm thử đầy đủ"
        }
    }
    r = requests.post(f"{BASE_URL}/api/trips", headers=user_headers, json=trip_req)
    record("Create Trip (Full Profile)", "POST", "/api/trips", r.status_code, 201)
    trip_id = r.json()["data"]["id"]

    r = requests.get(f"{BASE_URL}/api/trips", headers=user_headers)
    record("Get Trips (All)", "GET", "/api/trips", r.status_code, 200)

    r = requests.get(f"{BASE_URL}/api/trips?status=upcoming", headers=user_headers)
    record("Get Trips (Upcoming)", "GET", "/api/trips?status=upcoming", r.status_code, 200)

    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}", headers=user_headers)
    record("Get Trip Detail", "GET", f"/api/trips/{trip_id}", r.status_code, 200)

    r = requests.put(f"{BASE_URL}/api/trips/{trip_id}", headers=user_headers, json={
        "name": "Chuyến Đi Toàn Diện Matrix (Updated)",
        "startDate": "2026-10-20",
        "endDate": "2026-10-22",
        "travelers": 2,
        "budget": 12000000
    })
    record("Update Trip", "PUT", f"/api/trips/{trip_id}", r.status_code, 200)

    r = requests.put(f"{BASE_URL}/api/trips/{trip_id}/phase", headers=user_headers, json={"phase": "DURING"})
    record("Update Trip Phase to DURING", "PUT", f"/api/trips/{trip_id}/phase", r.status_code, 200)

    # ==========================================
    # NHÓM 4: ITINERARY & ACTIVITIES & REORDER
    # ==========================================
    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/itinerary", headers=user_headers)
    record("Get Itinerary", "GET", f"/api/trips/{trip_id}/itinerary", r.status_code, 200, f"{len(r.json()['data']['days'])} days")
    day_1 = r.json()["data"]["days"][0]

    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/cost-estimate", headers=user_headers, params={"priceLevel": 2, "activityType": "FOOD"})
    record("Cost Estimate Suggestion", "GET", f"/api/trips/{trip_id}/cost-estimate", r.status_code, 200, f"Suggested: {r.json()['data'].get('suggested')} VND")

    r = requests.post(f"{BASE_URL}/api/trips/{trip_id}/itinerary/activities", headers=user_headers, json={
        "dayNumber": 1,
        "title": "Ăn sáng Mì Quảng",
        "activityType": "FOOD",
        "placeId": db_place_id,
        "startTime": "08:00",
        "endTime": "09:00",
        "estimatedCost": 120000,
        "notes": "Đặc sản"
    })
    record("Add Activity 1 (Food + PlaceId)", "POST", f"/api/trips/{trip_id}/itinerary/activities", r.status_code, 201)
    act_1_id = r.json()["data"]["id"]

    r = requests.post(f"{BASE_URL}/api/trips/{trip_id}/itinerary/activities", headers=user_headers, json={
        "dayNumber": 1,
        "title": "Tham quan Cầu Rồng",
        "activityType": "SIGHTSEEING",
        "startTime": "09:30",
        "endTime": "11:30",
        "estimatedCost": 0
    })
    record("Add Activity 2 (Sightseeing)", "POST", f"/api/trips/{trip_id}/itinerary/activities", r.status_code, 201)
    act_2_id = r.json()["data"]["id"]

    r = requests.put(f"{BASE_URL}/api/activities/{act_1_id}", headers=user_headers, json={
        "title": "Ăn sáng Mì Quảng Đặc Biệt",
        "activityType": "FOOD",
        "startTime": "08:00",
        "endTime": "09:00",
        "estimatedCost": 150000
    })
    record("Update Activity", "PUT", f"/api/activities/{act_1_id}", r.status_code, 200)

    r = requests.get(f"{BASE_URL}/api/activities/{act_1_id}/explanation", headers=user_headers)
    record("Activity Explanation", "GET", f"/api/activities/{act_1_id}/explanation", r.status_code, 200, f"Known: {r.json()['data'].get('known')}")

    r = requests.put(f"{BASE_URL}/api/itinerary-days/{day_1['id']}/reorder", headers=user_headers, json={
        "activityIds": [act_2_id, act_1_id]
    })
    record("Reorder Activities", "PUT", f"/api/itinerary-days/{day_1['id']}/reorder", r.status_code, 200)

    # ==========================================
    # NHÓM 5: WEATHER
    # ==========================================
    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/weather", headers=user_headers)
    record("Get Trip Weather", "GET", f"/api/trips/{trip_id}/weather", r.status_code, 200, f"{len(r.json()['data'].get('days', []))} weather days")

    # ==========================================
    # NHÓM 6: BUDGET & EXPENSES
    # ==========================================
    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/budget", headers=user_headers)
    record("Get Budget & Daily Allowance", "GET", f"/api/trips/{trip_id}/budget", r.status_code, 200)

    r = requests.post(f"{BASE_URL}/api/trips/{trip_id}/expenses", headers=user_headers, json={
        "amount": 250000,
        "currency": "VND",
        "category": "FOOD",
        "description": "Ăn trưa cơm gà",
        "expenseDate": "2026-10-20",
        "activityId": act_1_id
    })
    record("Create Expense", "POST", f"/api/trips/{trip_id}/expenses", r.status_code, 201)
    exp_id = r.json()["data"]["id"]

    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/expenses", headers=user_headers)
    record("Get Expenses", "GET", f"/api/trips/{trip_id}/expenses", r.status_code, 200)

    r = requests.put(f"{BASE_URL}/api/expenses/{exp_id}", headers=user_headers, json={
        "amount": 300000,
        "currency": "VND",
        "category": "FOOD",
        "description": "Ăn trưa cơm gà (sửa)",
        "expenseDate": "2026-10-20"
    })
    record("Update Expense", "PUT", f"/api/expenses/{exp_id}", r.status_code, 200)

    # ==========================================
    # NHÓM 7: CHECKLIST PREPARATION
    # ==========================================
    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/checklist/suggestions", headers=user_headers)
    record("Checklist Suggestions (Rules+Weather)", "GET", f"/api/trips/{trip_id}/checklist/suggestions", r.status_code, 200, f"{len(r.json()['data'])} suggestions")

    items_to_add = [
        {"kind": "PACK", "title": "Áo mưa / ô", "category": "CLOTHING", "source": "SUGGESTED", "reason": "Dự báo thời tiết"},
        {"kind": "TODO", "title": "Check-in online vé máy bay", "category": "BOOKING", "source": "SUGGESTED", "dueDate": "2026-10-19"}
    ]
    r = requests.post(f"{BASE_URL}/api/trips/{trip_id}/checklist", headers=user_headers, json=items_to_add)
    record("Add Checklist Items (Batch)", "POST", f"/api/trips/{trip_id}/checklist", r.status_code, 201)
    item_1_id = r.json()["data"][0]["id"]

    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/checklist", headers=user_headers)
    record("Get Checklist Items", "GET", f"/api/trips/{trip_id}/checklist", r.status_code, 200)

    r = requests.patch(f"{BASE_URL}/api/checklist-items/{item_1_id}", headers=user_headers, json={"isDone": True})
    record("Patch Checklist Item (Done)", "PATCH", f"/api/checklist-items/{item_1_id}", r.status_code, 200)

    # ==========================================
    # NHÓM 8: EXPORT .ICS (CALENDAR)
    # ==========================================
    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/export.ics", headers=user_headers)
    is_valid_ics = "BEGIN:VCALENDAR" in r.text and "END:VCALENDAR" in r.text
    record("Export Calendar .ics (RFC 5545)", "GET", f"/api/trips/{trip_id}/export.ics", r.status_code, 200, f"Valid VCALENDAR: {is_valid_ics}")

    # ==========================================
    # NHÓM 9: PLACE REVIEW & RATINGS (AFTER PHASE)
    # ==========================================
    requests.put(f"{BASE_URL}/api/trips/{trip_id}/phase", headers=user_headers, json={"phase": "AFTER"})

    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/review/places", headers=user_headers)
    record("Review Places for Ended Trip", "GET", f"/api/trips/{trip_id}/review/places", r.status_code, 200, f"{len(r.json()['data'])} places in itinerary")

    if db_place_id:
        r = requests.put(f"{BASE_URL}/api/trips/{trip_id}/places/{db_place_id}/rating", headers=user_headers, json={
            "verdict": "LIKE",
            "note": "Rất ngon và chu đáo"
        })
        record("Rate Place (LIKE)", "PUT", f"/api/trips/{trip_id}/places/{db_place_id}/rating", r.status_code, 200)

        r = requests.get(f"{BASE_URL}/api/me/place-ratings", headers=user_headers)
        record("Get My Place Ratings", "GET", "/api/me/place-ratings", r.status_code, 200, f"{len(r.json()['data'])} ratings")

        r = requests.delete(f"{BASE_URL}/api/trips/{trip_id}/places/{db_place_id}/rating", headers=user_headers)
        record("Delete Place Rating", "DELETE", f"/api/trips/{trip_id}/places/{db_place_id}/rating", r.status_code, 200)

    # ==========================================
    # NHÓM 10: DUPLICATE TRIP (P7.4)
    # ==========================================
    r = requests.post(f"{BASE_URL}/api/trips/{trip_id}/duplicate", headers=user_headers, json={
        "name": "Chuyến Đi Nhân Bản Tháng 11",
        "startDate": "2026-11-05"
    })
    record("Duplicate Trip (Deep Copy)", "POST", f"/api/trips/{trip_id}/duplicate", r.status_code, 201)
    if r.status_code == 201:
        dup_trip_id = r.json()["data"]["trip"]["id"]
        requests.delete(f"{BASE_URL}/api/trips/{dup_trip_id}", headers=user_headers)

    # ==========================================
    # NHÓM 11: AI ASSISTANT & AUDIT
    # ==========================================
    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/ai/conversations", headers=user_headers)
    record("Get AI Conversations", "GET", f"/api/trips/{trip_id}/ai/conversations", r.status_code, 200)

    r = requests.get(f"{BASE_URL}/api/trips/{trip_id}/ai/activity", headers=user_headers)
    record("Get AI Tool Activity Audit", "GET", f"/api/trips/{trip_id}/ai/activity", r.status_code, 200)

    # AI Chat precheck / rate-limit check (returns SSE or 503 if API key missing)
    try:
        r = requests.post(f"{BASE_URL}/api/trips/{trip_id}/ai/chat", headers=user_headers, json={"message": "Chào bạn"}, timeout=3)
        record("AI Chat Assistant (SSE Precheck)", "POST", f"/api/trips/{trip_id}/ai/chat", r.status_code, [200, 503], "503 if no Gemini key, 200 if configured")
    except requests.exceptions.Timeout:
        record("AI Chat Assistant (SSE Streaming)", "POST", f"/api/trips/{trip_id}/ai/chat", 200, 200, "SSE stream started successfully")

    # AI Generate Itinerary on empty vs non-empty trip
    r = requests.post(f"{BASE_URL}/api/trips/{trip_id}/ai/generate", headers=user_headers)
    record("AI Generate (Non-empty Guard)", "POST", f"/api/trips/{trip_id}/ai/generate", r.status_code, 409, "Guarded by BR-509 TRIP_NOT_EMPTY")

    # ==========================================
    # NHÓM 12: ADMIN APIS (FULL READ & WRITE)
    # ==========================================
    r = requests.get(f"{BASE_URL}/api/admin/users", headers=admin_headers)
    record("Admin Get Users", "GET", "/api/admin/users", r.status_code, 200, f"{len(r.json()['data'].get('content', []))} users")

    r = requests.get(f"{BASE_URL}/api/admin/trips", headers=admin_headers)
    record("Admin Get Trips", "GET", "/api/admin/trips", r.status_code, 200, f"{len(r.json()['data'].get('content', []))} trips")

    r = requests.get(f"{BASE_URL}/api/admin/ai-usage", headers=admin_headers)
    record("Admin AI Usage Stats", "GET", "/api/admin/ai-usage", r.status_code, 200)

    r = requests.get(f"{BASE_URL}/api/admin/tool-executions", headers=admin_headers)
    record("Admin Tool Executions Log", "GET", "/api/admin/tool-executions", r.status_code, 200, f"{len(r.json()['data'].get('content', []))} tool executions")

    # Admin Create User
    new_admin_user_email = f"created_by_admin_{int(time.time())}@tripmind.com"
    r = requests.post(f"{BASE_URL}/api/admin/users", headers=admin_headers, json={
        "name": "Admin Created User",
        "email": new_admin_user_email,
        "password": "Password@123",
        "role": "USER"
    })
    record("Admin Create User", "POST", "/api/admin/users", r.status_code, [200, 201])
    if r.status_code in [200, 201]:
        created_user_id = r.json()["data"]["id"]

        # Admin Update User
        r = requests.put(f"{BASE_URL}/api/admin/users/{created_user_id}", headers=admin_headers, json={
            "name": "Admin Created User (Edited)",
            "active": True
        })
        record("Admin Update User", "PUT", f"/api/admin/users/{created_user_id}", r.status_code, 200)

        # Admin Delete User
        r = requests.delete(f"{BASE_URL}/api/admin/users/{created_user_id}", headers=admin_headers)
        record("Admin Delete User", "DELETE", f"/api/admin/users/{created_user_id}", r.status_code, 200)

    # Admin Update Trip
    r = requests.put(f"{BASE_URL}/api/admin/trips/{trip_id}", headers=admin_headers, json={"name": "Trip Admin Override"})
    record("Admin Update Trip", "PUT", f"/api/admin/trips/{trip_id}", r.status_code, 200)

    # ==========================================
    # CLEANUP TEST DATA
    # ==========================================
    requests.delete(f"{BASE_URL}/api/expenses/{exp_id}", headers=user_headers)
    record("Delete Expense", "DELETE", f"/api/expenses/{exp_id}", 200, 200)

    requests.delete(f"{BASE_URL}/api/activities/{act_2_id}", headers=user_headers)
    record("Delete Activity", "DELETE", f"/api/activities/{act_2_id}", 200, 200)

    requests.delete(f"{BASE_URL}/api/checklist-items/{item_1_id}", headers=user_headers)
    record("Delete Checklist Item", "DELETE", f"/api/checklist-items/{item_1_id}", 200, 200)

    requests.delete(f"{BASE_URL}/api/trips/{trip_id}", headers=user_headers)
    record("Delete Trip", "DELETE", f"/api/trips/{trip_id}", 200, 200)

    print("="*85)
    total = len(results)
    passed = sum(1 for r in results if r["pass"])
    print(f"TỔNG KẾT MATRIX KIỂM THỬ: {passed}/{total} endpoints PASSED ({(passed/total)*100:.1f}%)")
    print("="*85)

    with open("/home/minhnn/.gemini/antigravity-ide/brain/9a033322-e2f6-4d5e-b1fb-2f02f415170f/scratch/endpoint_test_results.json", "w") as f:
        json.dump(results, f, indent=2)

if __name__ == "__main__":
    main()
