from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "healthy", "service": "forecasting-ai-service"}

def test_forecast_demand():
    payload = {
        "sku": "SKU-001",
        "historical_sales": [10.0, 12.0, 15.0, 14.0, 16.0],
        "horizon": 7,
        "lead_time_days": 5
    }
    response = client.post("/forecast", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["sku"] == "SKU-001"
    assert data["horizon"] == 7
    assert len(data["predictions"]) == 7
    assert "safety_stock_recommendation" in data
    assert data["safety_stock_recommendation"] is not None
    assert "reorder_point_recommendation" in data
    assert "confidence_interval_lower" in data

def test_forecast_demand_empty():
    payload = {
        "sku": "SKU-EMPTY",
        "historical_sales": [],
        "horizon": 5
    }
    response = client.post("/forecast", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert len(data["predictions"]) == 5

def test_ai_inventory_assistant_low_stock():
    payload = {
        "query": "Which items are low in stock?",
        "inventory_context": [
            {"sku": "SKU-001", "name": "Laptop", "quantity": 3, "reorderThreshold": 10},
            {"sku": "SKU-002", "name": "Mouse", "quantity": 50, "reorderThreshold": 10}
        ]
    }
    response = client.post("/api/ai/inventory-assistant", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["risk_level"] in ["HIGH", "CRITICAL"]
    assert "SKU-001" in data["answer"]

def test_ai_agent_chat_camel_case_compatibility():
    # Validates that Java's camelCase payload works seamlessly without 422 error
    payload = {
        "query": "Which items are low in stock?",
        "inventoryContext": [
            {"sku": "SKU-001", "name": "Laptop", "quantity": 3, "reorderThreshold": 10},
            {"sku": "SKU-002", "name": "Mouse", "quantity": 50, "reorderThreshold": 10}
        ],
        "catalogContext": [
            {"sku": "SKU-001", "price": 1000.0, "category": "Electronics"}
        ]
    }
    response = client.post("/api/ai/agent/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["risk_level"] in ["HIGH", "CRITICAL"]
    assert "SKU-001" in data["answer"]

def test_ai_agent_chat_top_n_sorted():
    payload = {
        "query": "can you show top 10 inventory data sorted by quantity",
        "inventoryContext": [
            {"sku": "SKU-001", "name": "Laptop", "quantity": 10, "reorderThreshold": 5, "price": 1000.0, "warehouseLocation": "Warehouse-North"},
            {"sku": "SKU-002", "name": "Mouse", "quantity": 50, "reorderThreshold": 10, "price": 25.0, "warehouseLocation": "Warehouse-South"}
        ]
    }
    response = client.post("/api/ai/agent/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "Sorted by Quantity Descending" in data["answer"]
    assert "SKU-002" in data["answer"]
    assert data["answer"].index("SKU-002") < data["answer"].index("SKU-001")

def test_ai_agent_chat_which_item_has_highest_value():
    payload = {
        "query": "which item has highest value",
        "inventoryContext": [
            {"sku": "SKU-MONITOR", "name": "4K Monitor", "quantity": 50, "reorderThreshold": 10, "price": 300.0, "warehouseLocation": "Central-Hub"},
            {"sku": "SKU-LAPTOP", "name": "Laptop Pro", "quantity": 2, "reorderThreshold": 10, "price": 1500.0, "warehouseLocation": "Warehouse-North"}
        ]
    }
    response = client.post("/api/ai/agent/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "Highest Value Inventory Item Analysis" in data["answer"]
    assert "SKU-MONITOR" in data["answer"]
    assert "SKU-LAPTOP" in data["answer"]

def test_ai_agent_chat_valuation():
    payload = {
        "query": "What is our total inventory value?",
        "inventory_context": [
            {"sku": "SKU-001", "name": "Laptop", "quantity": 10, "price": 1000.0, "reorderThreshold": 5}
        ]
    }
    response = client.post("/api/ai/agent/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "10,000" in data["answer"] or "10000" in data["answer"] or "Total" in data["answer"]

def test_admin_get_loggers():
    response = client.get("/api/admin/loggers")
    assert response.status_code == 200
    data = response.json()
    assert "loggers" in data
    assert "root" in data["loggers"]
    assert "app.main" in data["loggers"]

def test_admin_set_logger_level():
    response = client.post("/api/admin/loggers/app.agent", json={"level": "DEBUG"})
    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "app.agent"
    assert data["configuredLevel"] == "DEBUG"

    get_res = client.get("/api/admin/loggers/app.agent")
    assert get_res.status_code == 200
