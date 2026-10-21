#!/usr/bin/env python3
import urllib.request
import urllib.error
import json
import time

def req(url, method="GET", data=None, headers=None):
    if headers is None:
        headers = {"Content-Type": "application/json"}
    body = json.dumps(data).encode("utf-8") if data else None
    r = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r) as resp:
            content = resp.read().decode("utf-8")
            try:
                data = json.loads(content or "{}")
            except Exception:
                data = content
            return resp.status, data
    except urllib.error.HTTPError as e:
        body = {}
        try:
            body = json.loads(e.read().decode("utf-8") or "{}")
        except Exception:
            pass
        return e.code, body

def run_tests():
    print("=================================================================")
    print("   LIVE DISTRIBUTED ENVIRONMENT COMPREHENSIVE TEST RUNNER        ")
    print("=================================================================")

    print("\n--- 1. Health Checks Across Services ---")
    s, b = req("http://localhost:8081/actuator/health")
    print(f"Inventory Service Health: status={s}, statusField={b.get('status')}")
    assert s == 200 and b.get("status") == "UP"

    s, b = req("http://localhost:8082/actuator/health")
    print(f"Catalog Service Health:   status={s}, statusField={b.get('status')}")
    assert s == 200 and b.get("status") == "UP"

    s, b = req("http://localhost:8083/health")
    print(f"Forecasting Service Health: status={s}, statusField={b.get('status')}")
    assert s == 200

    print("\n--- 2. Web UI Asset Verification ---")
    s, _ = req("http://localhost:8081/index.html")
    print(f"Dashboard UI (/index.html): status={s} (Accessible)")
    assert s == 200

    print("\n--- 3. Create New Inventory Item (POST /api/inventory) ---")
    sku = f"SKU-LIVE-{int(time.time())}"
    payload = {"sku": sku, "quantity": 100, "reorderThreshold": 15, "warehouseLocation": "Warehouse-East"}
    s, b = req("http://localhost:8081/api/inventory", method="POST", data=payload)
    print(f"Create SKU: status={s}, sku={b.get('sku')}, initialQuantity={b.get('quantity')}")
    assert s in (200, 201) and b.get("sku") == sku

    print("\n--- 4. Stock Adjustment (+20 Units) (POST /api/inventory/{sku}/adjust) ---")
    adj = {"quantityChange": 20, "transactionType": "PURCHASE_RECEIPT", "referenceId": "PO-LIVE-001"}
    s, b = req(f"http://localhost:8081/api/inventory/{sku}/adjust", method="POST", data=adj)
    print(f"Adjust Stock: status={s}, newQuantity={b.get('quantity')}")
    assert s == 200 and b.get("quantity") == 120

    print("\n--- 5. Shopify Bounded Unit Pool Initial State ---")
    s, b = req(f"http://localhost:8081/api/inventory/reservations/pool/{sku}")
    print(f"Initial Pool State: masterStock={b.get('masterStock')}, availableUnits={b.get('availableUnits')}, reservedUnits={b.get('reservedUnits')}")
    assert s == 200 and b.get("masterStock") == 120

    print("\n--- 6. Perform Non-Blocking Unit Reservation (FOR UPDATE SKIP LOCKED) ---")
    res_payload = {"sku": sku, "quantity": 5, "reservationId": f"RES-LIVE-{int(time.time()*1000)}", "ttlSeconds": 300}
    t0 = time.time()
    s, b = req("http://localhost:8081/api/inventory/reservations/reserve", method="POST", data=res_payload)
    lat_ms = (time.time() - t0) * 1000
    print(f"Reservation Result: status={s}, reservedQty={b.get('quantityReserved')}, resId={b.get('reservationId')}, latency={lat_ms:.2f}ms")
    assert s == 201 and b.get("quantityReserved") == 5

    print("\n--- 7. Test Idempotency Replay (Same Reservation ID) ---")
    s2, b2 = req("http://localhost:8081/api/inventory/reservations/reserve", method="POST", data=res_payload)
    print(f"Idempotent Replay: status={s2}, reservedQty={b2.get('quantityReserved')}, message={b2.get('message')}")
    assert s2 == 201 and b2.get("quantityReserved") == 5

    print("\n--- 8. Pool State Inspection (Reserved Units Count) ---")
    s, b = req(f"http://localhost:8081/api/inventory/reservations/pool/{sku}")
    print(f"Pool status after reserve: reservedUnits={b.get('reservedUnits')}, availableUnits={b.get('availableUnits')}")
    assert b.get("reservedUnits") == 5

    print("\n--- 9. Claim Reservation (Customer Completed Payment) ---")
    s, b = req("http://localhost:8081/api/inventory/reservations/claim/" + res_payload["reservationId"], method="POST")
    print(f"Claim status={s}, claimMessage={b.get('message')}")
    assert s == 200 and b.get("status") == "CLAIMED"

    s, b = req(f"http://localhost:8081/api/inventory/{sku}")
    print(f"Master Inventory Item quantity after claim: {b.get('quantity')} (Expected: 115)")
    assert b.get("quantity") == 115

    print("\n--- 10. Reserve & Release Cycle (Buyer Cancels Cart) ---")
    res_cancel = {"sku": sku, "quantity": 10, "reservationId": f"RES-CANCEL-{int(time.time()*1000)}", "ttlSeconds": 300}
    s, b = req("http://localhost:8081/api/inventory/reservations/reserve", method="POST", data=res_cancel)
    print(f"Reserved 10 units: status={s}")
    s, b = req("http://localhost:8081/api/inventory/reservations/release/" + res_cancel["reservationId"], method="POST")
    print(f"Released reservation: status={s}, statusField={b.get('status')}")
    assert s == 200 and b.get("status") == "RELEASED"

    print("\n--- 11. Multi-Threaded Flash Sale Benchmark (Shopify Non-Blocking Concurrency) ---")
    flash_sim = {"sku": sku, "concurrentCheckouts": 50, "qtyPerCheckout": 2}
    t0 = time.time()
    s, b = req("http://localhost:8081/api/inventory/reservations/simulate-flash-sale", method="POST", data=flash_sim)
    total_ms = (time.time() - t0) * 1000
    print(f"Flash Sale Simulation Response (50 parallel checkouts):")
    print(f"  - Checkouts Attempted: {b.get('concurrentCheckoutsAttempted')}")
    print(f"  - Successful Reservations: {b.get('successfulReservations')}")
    print(f"  - Out Of Stock Rejections: {b.get('insufficientStockRejections')}")
    print(f"  - Total Execution Time: {b.get('totalExecutionTimeMs')}ms")
    print(f"  - Avg Latency Per Reservation: {b.get('averageLatencyPerReservationMs'):.2f}ms")
    print(f"  - Full HTTP Round-Trip Time: {total_ms:.2f}ms")
    assert s == 200 and b.get("concurrentCheckoutsAttempted") == 50

    print("\n--- 12. Delete Inventory Item (DELETE /api/inventory/{sku}) ---")
    s, b = req(f"http://localhost:8081/api/inventory/{sku}", method="DELETE")
    print(f"Delete SKU status: {s}")
    assert s == 204

    s, b = req(f"http://localhost:8081/api/inventory/{sku}")
    print(f"Verify deleted from DB: status={s}")
    assert s == 404

    print("\n=================================================================")
    print("   ALL 12 LIVE ENVIRONMENT TESTS PASSED WITH 100% SUCCESS!       ")
    print("=================================================================")

if __name__ == "__main__":
    run_tests()
