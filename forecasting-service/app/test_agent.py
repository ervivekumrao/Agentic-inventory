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

from app.agent import InventoryAIAgent

def test_agent_low_stock_query():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SKU-001", "name": "Laptop Pro", "quantity": 2, "reorderThreshold": 10, "warehouseLocation": "Warehouse-North", "price": 1200.00},
        {"sku": "SKU-002", "name": "Wireless Mouse", "quantity": 50, "reorderThreshold": 10, "warehouseLocation": "Warehouse-South", "price": 25.00}
    ]
    res = agent.process_query("Which items are low in stock and need reordering?", inv_context)

    assert res["risk_level"] in ["HIGH", "CRITICAL"]
    assert "SKU-001" in res["answer"]
    assert len(res["recommendations"]) > 0

def test_agent_low_in_stock_query_variation():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SKU-001", "name": "Laptop", "quantity": 3, "reorderThreshold": 10},
        {"sku": "SKU-002", "name": "Mouse", "quantity": 50, "reorderThreshold": 10}
    ]
    res = agent.process_query("Which items are low in stock?", inv_context)
    assert res["risk_level"] in ["HIGH", "CRITICAL"]
    assert "SKU-001" in res["answer"]

def test_agent_sku_count_query():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SKU-001", "quantity": 10, "reorderThreshold": 5},
        {"sku": "SKU-002", "quantity": 20, "reorderThreshold": 5}
    ]
    res = agent.process_query("how many different skus stock has?", inv_context)
    assert "2 distinct SKUs" in res["answer"]

def test_agent_tabular_report_query():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SKU-001", "name": "Laptop", "quantity": 10, "reorderThreshold": 5, "price": 1000.0, "warehouseLocation": "Warehouse-North"},
        {"sku": "SKU-002", "name": "Mouse", "quantity": 3, "reorderThreshold": 10, "price": 25.0, "warehouseLocation": "Warehouse-South"}
    ]
    res = agent.process_query("can you share this data in more details in tabular form?", inv_context)
    assert "| SKU | Product Name | Category |" in res["answer"]
    assert "SKU-001" in res["answer"]

def test_agent_top_n_sorted_by_quantity():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SKU-001", "name": "Item A", "quantity": 10, "reorderThreshold": 5, "price": 100.0, "warehouseLocation": "Warehouse-North"},
        {"sku": "SKU-002", "name": "Item B", "quantity": 50, "reorderThreshold": 5, "price": 20.0, "warehouseLocation": "Warehouse-South"},
        {"sku": "SKU-003", "name": "Item C", "quantity": 30, "reorderThreshold": 5, "price": 50.0, "warehouseLocation": "Central-Hub"}
    ]
    res = agent.process_query("can you show top 10 inventory data sorted by quantity", inv_context)
    assert "| Rank | SKU | Product Name |" in res["answer"]
    assert "Sorted by Quantity Descending" in res["answer"]
    idx_002 = res["answer"].index("SKU-002")
    idx_003 = res["answer"].index("SKU-003")
    idx_001 = res["answer"].index("SKU-001")
    assert idx_002 < idx_003 < idx_001

def test_agent_lowest_sorted_by_stock():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SKU-001", "name": "Item A", "quantity": 10, "reorderThreshold": 5, "price": 100.0},
        {"sku": "SKU-002", "name": "Item B", "quantity": 50, "reorderThreshold": 5, "price": 20.0},
        {"sku": "SKU-003", "name": "Item C", "quantity": 2, "reorderThreshold": 5, "price": 50.0}
    ]
    res = agent.process_query("show lowest 2 items by stock", inv_context)
    assert "Ascending" in res["answer"]
    assert "SKU-003" in res["answer"]
    assert "SKU-001" in res["answer"]
    idx_003 = res["answer"].index("SKU-003")
    idx_001 = res["answer"].index("SKU-001")
    assert idx_003 < idx_001

def test_agent_which_item_has_highest_value():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SKU-CHEAP", "name": "Cheap Item", "quantity": 100, "reorderThreshold": 10, "price": 10.0, "warehouseLocation": "Warehouse-North"},
        {"sku": "SKU-EXPENSIVE", "name": "Expensive Item", "quantity": 5, "reorderThreshold": 10, "price": 2000.0, "warehouseLocation": "Warehouse-South"},
        {"sku": "SKU-HIGH-VAL", "name": "High Value Holding", "quantity": 50, "reorderThreshold": 10, "price": 500.0, "warehouseLocation": "Central-Hub"}
    ]
    res = agent.process_query("which item has highest value", inv_context)
    assert "Highest Value Inventory Item Analysis" in res["answer"]
    assert "SKU-HIGH-VAL" in res["answer"]
    assert "SKU-EXPENSIVE" in res["answer"]

def test_agent_stock_type_query():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SKU-001", "category": "Electronics", "quantity": 10},
        {"sku": "SKU-002", "category": "Apparel", "quantity": 20}
    ]
    res = agent.process_query("give me total stock count stock type wise", inv_context)
    assert "Electronics" in res["answer"]
    assert "Apparel" in res["answer"]

def test_agent_specific_sku_lookup():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "LAPTOP-PRO-15", "name": "Pro 15-inch Laptop", "quantity": 8, "reorderThreshold": 10, "price": 1299.99, "warehouseLocation": "Warehouse-North"}
    ]
    res = agent.process_query("what is the stock level of LAPTOP-PRO-15?", inv_context)
    assert "LAPTOP-PRO-15" in res["answer"]
    assert "Single Item Ledger" in res["answer"]
    assert "8 units" in res["answer"]

def test_agent_demand_forecast_intent():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SMARTPHONE-X", "name": "SmartPhone X", "quantity": 15, "reorderThreshold": 10, "price": 899.50}
    ]
    res = agent.process_query("forecast demand for SMARTPHONE-X over the next 14 days", inv_context)
    assert "Demand Forecast" in res["answer"]
    assert "SMARTPHONE-X" in res["answer"]
    assert "14 Days" in res["answer"]

def test_agent_warehouse_filter():
    agent = InventoryAIAgent()
    inv_context = [
        {"sku": "SKU-001", "name": "Item A", "quantity": 10, "warehouseLocation": "Warehouse-North"},
        {"sku": "SKU-002", "name": "Item B", "quantity": 20, "warehouseLocation": "Warehouse-South"}
    ]
    res = agent.process_query("show me all items in Warehouse-North", inv_context)
    assert "Warehouse-North" in res["answer"]
    assert "SKU-001" in res["answer"]

def test_agent_help_capabilities():
    agent = InventoryAIAgent()
    res = agent.process_query("what can you do?", [])
    assert "Capabilities" in res["answer"]
    assert "Tabular & Custom Sorting" in res["answer"]

def test_agent_empty_database():
    agent = InventoryAIAgent()
    res = agent.process_query("What is our stock?", [])
    assert "0 active inventory records" in res["answer"]
