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

import logging
import os
import httpx
import numpy as np
import pandas as pd
from typing import List, Dict, Any, Optional
from app.agent import InventoryAIAgent

logger = logging.getLogger("app.mcp_server")

agent = InventoryAIAgent()
INVENTORY_SERVICE_URL = os.getenv("INVENTORY_SERVICE_URL", "http://localhost:8080")

def _fetch_live_inventory_context() -> List[Dict[str, Any]]:
    """Fetches live inventory data from inventory-service REST endpoint."""
    try:
        with httpx.Client(timeout=3.0) as client:
            res = client.get(f"{INVENTORY_SERVICE_URL}/api/inventory")
            if res.status_code == 200:
                return res.json()
    except Exception as e:
        logger.debug("Could not fetch live inventory from %s: %s", INVENTORY_SERVICE_URL, e)
    return []

# Native python tool functions (can be executed directly or registered via FastMCP)

def get_low_stock_reorder_list(
    inventory_context: Optional[list] = None,
    deficit_multiplier: float = 2.0
) -> Dict[str, Any]:
    """
    Identifies inventory items below threshold and calculates replenishment deficits.
    """
    context = inventory_context if inventory_context else _fetch_live_inventory_context()
    if not context:
        return {"total_low_stock": 0, "total_low_stock_skus": 0, "reorder_items": [], "total_procurement_cost": 0.0, "total_estimated_procurement_cost": 0.0, "risk_assessment": "HEALTHY"}

    low_stock_items = []
    total_cost = 0.0

    for item in inventory_context:
        qty = float(item.get("quantity", 0))
        threshold = float(item.get("reorderPoint") or item.get("reorder_point") or 10)
        unit_price = float(item.get("unitPrice") or item.get("unit_price") or item.get("price") or 0)

        if qty <= threshold:
            target_qty = threshold * deficit_multiplier
            deficit = max(0.0, target_qty - qty)
            estimated_cost = deficit * unit_price
            total_cost += estimated_cost

            low_stock_items.append({
                "sku": item.get("sku"),
                "name": item.get("name") or item.get("productName"),
                "quantity": qty,
                "reorder_point": threshold,
                "deficit_units": deficit,
                "unit_price": unit_price,
                "estimated_procurement_cost": estimated_cost,
                "warehouse": item.get("warehouseLocation") or item.get("warehouse_location") or "Main Hub"
            })

    return {
        "total_low_stock_skus": len(low_stock_items),
        "reorder_items": low_stock_items,
        "total_estimated_procurement_cost": round(total_cost, 2),
        "risk_assessment": "CRITICAL" if len(low_stock_items) > 5 else ("WARNING" if len(low_stock_items) > 0 else "HEALTHY")
    }


def forecast_sku_demand(
    sku: str,
    historical_sales: List[float],
    horizon: int = 14,
    lead_time_days: int = 5
) -> Dict[str, Any]:
    """
    Calculates demand forecast, safety stock, dynamic ROP, and anomaly detection for a specific SKU.
    """
    if not historical_sales:
        return {
            "sku": sku,
            "horizon": horizon,
            "predictions": [0.0] * horizon,
            "confidence_score": 0.0,
            "trend": "NEUTRAL",
            "error": "No historical sales data provided"
        }

    sales = np.array(historical_sales, dtype=float)
    alpha = 0.3

    # Exponential smoothing
    last_val = sales[0]
    for val in sales:
        last_val = alpha * val + (1 - alpha) * last_val

    # Future projection
    recent_trend = (sales[-1] - sales[0]) / max(1, len(sales) - 1) if len(sales) > 1 else 0.0
    predictions = []
    for i in range(1, horizon + 1):
        pred = max(0.0, last_val + (recent_trend * i * 0.1))
        predictions.append(round(float(pred), 2))

    # Anomaly detection (z-score > 2.0)
    mean_sales = float(np.mean(sales))
    std_sales = float(np.std(sales))
    anomalies = []
    if std_sales > 0:
        z_scores = np.abs((sales - mean_sales) / std_sales)
        anomalies = [int(idx) for idx, z in enumerate(z_scores) if z > 2.0]

    # Safety stock & ROP
    avg_daily_demand = max(0.1, mean_sales)
    safety_stock = round(1.65 * std_sales * np.sqrt(lead_time_days), 2)
    reorder_point = round((avg_daily_demand * lead_time_days) + safety_stock, 2)

    trend_str = "UPWARD" if recent_trend > 0.5 else ("DOWNWARD" if recent_trend < -0.5 else "STABLE")

    return {
        "sku": sku,
        "horizon": horizon,
        "predictions": predictions,
        "confidence_score": 0.88 if len(sales) >= 10 else 0.70,
        "trend": trend_str,
        "safety_stock_recommendation": safety_stock,
        "reorder_point_recommendation": reorder_point,
        "anomalies_detected": anomalies,
        "mean_daily_sales": round(mean_sales, 2)
    }


def get_inventory_valuation(inventory_context: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Computes total inventory valuation, high-value SKUs, physical unit volume, and warehouse asset allocation.
    """
    if not inventory_context:
        return {
            "total_valuation": 0.0,
            "total_skus": 0,
            "total_units": 0,
            "warehouse_breakdown": {}
        }

    total_val = 0.0
    total_units = 0
    warehouse_val: Dict[str, float] = {}

    for item in inventory_context:
        qty = int(item.get("quantity", 0))
        price = float(item.get("unitPrice") or item.get("unit_price") or item.get("price") or 0.0)
        val = qty * price

        total_val += val
        total_units += qty

        wh = item.get("warehouseLocation") or item.get("warehouse_location") or "Unassigned"
        warehouse_val[wh] = warehouse_val.get(wh, 0.0) + val

    return {
        "total_holding_valuation": round(total_val, 2),
        "total_distinct_skus": len(inventory_context),
        "total_physical_units": total_units,
        "warehouse_asset_allocation": {k: round(v, 2) for k, v in warehouse_val.items()}
    }


def lookup_sku_details(sku: str, inventory_context: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Searches for a single SKU in inventory context and returns operational card ledger.
    """
    if not inventory_context or not sku:
        return {"error": "SKU not found", "found": False}

    sku_clean = sku.strip().upper()
    for item in inventory_context:
        item_sku = (item.get("sku") or "").strip().upper()
        if item_sku == sku_clean:
            qty = float(item.get("quantity", 0))
            threshold = float(item.get("reorderPoint") or item.get("reorder_point") or 10)
            price = float(item.get("unitPrice") or item.get("unit_price") or item.get("price") or 0.0)
            holding_val = qty * price

            return {
                "found": True,
                "sku": item.get("sku"),
                "name": item.get("name") or item.get("productName"),
                "quantity": qty,
                "reorder_point": threshold,
                "unit_price": price,
                "holding_value": round(holding_val, 2),
                "is_low_stock": qty <= threshold,
                "warehouse": item.get("warehouseLocation") or item.get("warehouse_location") or "Main Hub"
            }

    return {"error": f"SKU '{sku}' not found in inventory context", "found": False}


def query_inventory_agent(
    query: str,
    inventory_context: Optional[list] = None,
    catalog_context: Optional[list] = None
) -> Dict[str, Any]:
    """
    Delegates natural language query processing to InventoryAIAgent.
    """
    return agent.process_query(
        query=query,
        inventory_context=inventory_context or [],
        catalog_context=catalog_context or []
    )


def create_purchase_order_draft(
    supplier_code: str,
    items: List[Dict[str, Any]]
) -> Dict[str, Any]:
    """
    Creates a draft purchase order with human approval requirement.
    """
    import random
    po_number = f"PO-AI-{random.randint(1000, 9999)}"
    payload = {
        "orderNumber": po_number,
        "supplierCode": supplier_code,
        "status": "DRAFT",
        "items": items
    }
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.post(f"{INVENTORY_SERVICE_URL}/api/inventory/po", json=payload)
            if res.status_code in (200, 201):
                return {
                    "status": "DRAFT_CREATED",
                    "purchase_order": res.json(),
                    "requires_approval": True,
                    "approval_action": f"POST /api/inventory/po/{po_number}/approve"
                }
            else:
                logger.warning("PO draft creation failed with HTTP status %s: %s", res.status_code, res.text)
                return {
                    "status": "DRAFT_FAILED",
                    "error": f"Failed to create PO draft on inventory service. HTTP {res.status_code}",
                    "purchase_order": payload
                }
    except Exception as e:
        logger.warning("Could not submit PO draft to live inventory-service: %s", e)
        return {
            "status": "DRAFT_FAILED",
            "error": f"Network exception calling inventory-service: {str(e)}",
            "purchase_order": payload
        }


# FastMCP Server Registration (if mcp package is installed)
mcp = None
try:
    from mcp.server.fastmcp import FastMCP
    mcp = FastMCP("SmartInventoryAgent")

    @mcp.tool()
    def mcp_get_low_stock(inventory_context: List[Dict[str, Any]], deficit_multiplier: float = 2.0) -> Dict[str, Any]:
        """Identifies inventory items below reorder point and estimates procurement cost."""
        return get_low_stock_reorder_list(inventory_context, deficit_multiplier)

    @mcp.tool()
    def mcp_forecast_demand(sku: str, historical_sales: List[float], horizon: int = 14, lead_time_days: int = 5) -> Dict[str, Any]:
        """Calculates demand forecast, statistical safety stock, ROP, and anomaly detection."""
        return forecast_sku_demand(sku, historical_sales, horizon, lead_time_days)

    @mcp.tool()
    def mcp_get_valuation(inventory_context: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Computes total inventory holding valuation and warehouse asset distribution."""
        return get_inventory_valuation(inventory_context)

    @mcp.tool()
    def mcp_lookup_sku(sku: str, inventory_context: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Searches for a specific SKU and returns its stock ledger card."""
        return lookup_sku_details(sku, inventory_context)

    @mcp.tool()
    def mcp_query_agent(query: str, inventory_context: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Processes a natural language query using intent classification and local synthesis."""
        return query_inventory_agent(query, inventory_context)

    logger.info("FastMCP SmartInventoryAgent initialized with 5 tools.")

except ImportError:
    logger.info("MCP SDK not installed. Native tool functions active.")
