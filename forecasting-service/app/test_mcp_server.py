# Copyright (c) 2026 Vivek Umrao.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.mcp_server import (
    get_low_stock_reorder_list,
    forecast_sku_demand,
    get_inventory_valuation,
    lookup_sku_details,
    query_inventory_agent
)

client = TestClient(app)

SAMPLE_INVENTORY = [
    {
        "sku": "SKU-TEST-001",
        "name": "Industrial Sensor",
        "quantity": 3,
        "reorderPoint": 10,
        "unitPrice": 50.0,
        "warehouseLocation": "Warehouse-North"
    },
    {
        "sku": "SKU-TEST-002",
        "name": "Microcontroller Board",
        "quantity": 100,
        "reorderPoint": 20,
        "unitPrice": 15.0,
        "warehouseLocation": "Warehouse-South"
    }
]


def test_get_low_stock_reorder_list():
    res = get_low_stock_reorder_list(SAMPLE_INVENTORY, deficit_multiplier=2.0)
    assert res["total_low_stock_skus"] == 1
    assert len(res["reorder_items"]) == 1
    assert res["reorder_items"][0]["sku"] == "SKU-TEST-001"
    assert res["reorder_items"][0]["deficit_units"] == 17.0  # 10 * 2 - 3
    assert res["total_estimated_procurement_cost"] == 850.0  # 17 * 50


def test_get_low_stock_empty():
    res = get_low_stock_reorder_list([])
    assert res["total_low_stock"] == 0
    assert res["total_procurement_cost"] == 0.0


def test_forecast_sku_demand():
    sales = [10.0, 12.0, 15.0, 14.0, 18.0, 20.0, 22.0, 25.0, 24.0, 28.0]
    res = forecast_sku_demand(sku="SKU-TEST-001", historical_sales=sales, horizon=7, lead_time_days=5)
    assert res["sku"] == "SKU-TEST-001"
    assert res["horizon"] == 7
    assert len(res["predictions"]) == 7
    assert res["safety_stock_recommendation"] > 0
    assert res["reorder_point_recommendation"] > 0
    assert res["trend"] == "UPWARD"


def test_get_inventory_valuation():
    res = get_inventory_valuation(SAMPLE_INVENTORY)
    assert res["total_distinct_skus"] == 2
    assert res["total_physical_units"] == 103
    assert res["total_holding_valuation"] == 1650.0  # (3*50) + (100*15) = 150 + 1500
    assert res["warehouse_asset_allocation"]["Warehouse-North"] == 150.0
    assert res["warehouse_asset_allocation"]["Warehouse-South"] == 1500.0


def test_lookup_sku_details():
    res_found = lookup_sku_details("SKU-TEST-001", SAMPLE_INVENTORY)
    assert res_found["found"] is True
    assert res_found["sku"] == "SKU-TEST-001"
    assert res_found["is_low_stock"] is True
    assert res_found["holding_value"] == 150.0

    res_missing = lookup_sku_details("SKU-UNKNOWN", SAMPLE_INVENTORY)
    assert res_missing["found"] is False


def test_mcp_info_endpoint():
    response = client.get("/mcp/info")
    assert response.status_code == 200
    data = response.json()
    assert data["mcp_server_name"] == "SmartInventoryAgent"
    assert data["status"] == "active"
    assert len(data["tools"]) == 5


def test_execute_mcp_tool_endpoint_success():
    payload = {
        "tool_name": "get_low_stock_reorder_list",
        "arguments": {
            "inventory_context": SAMPLE_INVENTORY,
            "deficit_multiplier": 2.0
        }
    }
    response = client.post("/mcp/tools/execute", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["total_low_stock_skus"] == 1


def test_create_purchase_order_draft():
    from app.mcp_server import create_purchase_order_draft
    res = create_purchase_order_draft("SUP-001", [{"sku": "SKU-001", "orderedQuantity": 50, "unitCost": 12.5}])
    assert res["status"] == "DRAFT_CREATED"
    assert res["requires_approval"] is True
    assert "PO-AI-" in res["purchase_order"]["orderNumber"]


def test_execute_mcp_tool_endpoint_not_found():
    payload = {
        "tool_name": "non_existent_tool",
        "arguments": {}
    }
    response = client.post("/mcp/tools/execute", json=payload)
    assert response.status_code == 404
