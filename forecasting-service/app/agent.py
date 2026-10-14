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

import os
import re
import json
import logging
import httpx
from typing import List, Dict, Any, Optional, Tuple

logger = logging.getLogger("app.agent")

class InventoryAIAgent:
    """
    Agentic AI Assistant for Smart Inventory Management.
    Provides human-like, natural, conversational responses for inventory analysis,
    stock reordering, valuation, warehouse logistics, category breakdowns, tabular data exports,
    demand forecasting, custom sorting/filtering, specific SKU lookups, and executive overviews.
    Supports external LLM providers (OpenAI/Gemini) when configured, or
    uses a hybrid ML Intent Classifier + Deep Local Human Synthesis Engine for expert offline responses.
    """

    def __init__(self):
        self.openai_api_key = os.getenv("OPENAI_API_KEY") or os.getenv("LLM_API_KEY")
        self.gemini_api_key = os.getenv("GEMINI_API_KEY")
        self.llm_model = os.getenv("LLM_MODEL", "gpt-3.5-turbo")
        self.ml_model = None
        self._load_ml_model()

    def _load_ml_model(self):
        """Loads trained scikit-learn intent classifier pipeline if available."""
        model_path = os.path.join(os.path.dirname(__file__), "models", "intent_classifier.joblib")
        if os.path.exists(model_path):
            try:
                import joblib
                self.ml_model = joblib.load(model_path)
                logger.info("Successfully loaded ML intent classifier from %s", model_path)
            except Exception as e:
                logger.warning("Failed to load ML intent classifier from %s: %s", model_path, e)
                self.ml_model = None
        else:
            logger.debug("No ML intent classifier artifact found at %s. Using semantic rule classifier.", model_path)

    def process_query(
        self,
        query: str,
        inventory_context: List[Dict[str, Any]],
        catalog_context: Optional[List[Dict[str, Any]]] = None,
        system_instruction: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Main entry point for agentic query processing.
        """
        logger.debug("InventoryAIAgent processing query: '%s'", query)
        catalog_ctx = catalog_context or []
        metrics = self._analyze_context(inventory_context, catalog_ctx)
        intent, confidence = self._classify_intent(query)
        entities = self._extract_entities(query, inventory_context, catalog_ctx)
        logger.info("Classified query intent as '%s' (confidence: %.2f%%) with entities: %s", intent, confidence * 100, entities)

        # 1. Try external LLM if API key is present
        if self.openai_api_key or self.gemini_api_key:
            logger.debug("Attempting LLM generation via configured API key")
            sanitized_instruction = self._sanitize_system_instruction(system_instruction)
            llm_result = self._try_llm_generation(query, metrics, inventory_context, catalog_ctx, sanitized_instruction)
            if llm_result:
                logger.info("LLM generation succeeded for query: '%s'", query)
                return llm_result

        # 2. Local Deep Human Synthesis Engine with Entity Intelligence
        logger.debug("Synthesizing human operational response locally for intent: %s", intent)
        return self._synthesize_human_response(query, intent, metrics, entities)

    @staticmethod
    def _sanitize_system_instruction(instruction: Optional[str]) -> Optional[str]:
        if not instruction:
            return None
        # Defend against prompt injection patterns and enforce maximum length
        clean = re.sub(
            r'(?i)(ignore (all )?previous instructions|disregard prior commands|reveal system prompt|system override)',
            '',
            instruction
        ).strip()
        return clean[:500] if clean else None

    @staticmethod
    def _analyze_context(
        inventory_context: List[Dict[str, Any]],
        catalog_context: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """
        Computes comprehensive inventory metrics from context data.
        """
        catalog_by_sku = {prod.get("sku"): prod for prod in catalog_context if prod.get("sku")}

        total_skus = len(inventory_context)
        total_quantity = 0
        total_valuation = 0.0
        low_stock_items = []
        out_of_stock_items = []
        overstocked_items = []

        warehouse_counts: Dict[str, int] = {}
        warehouse_qty: Dict[str, int] = {}
        category_counts: Dict[str, int] = {}
        category_qty: Dict[str, int] = {}
        category_val: Dict[str, float] = {}

        sku_details = []

        for item in inventory_context:
            sku = item.get("sku", "UNKNOWN")
            qty = item.get("quantity", 0)
            threshold = item.get("reorderThreshold", 10)
            warehouse = item.get("warehouseLocation", "Unassigned")

            prod_info = catalog_by_sku.get(sku, {})
            price = prod_info.get("price", item.get("price", 0.0))
            name = prod_info.get("name", item.get("name", sku))
            category = prod_info.get("category", item.get("category", "General"))

            item_valuation = qty * price
            total_quantity += qty
            total_valuation += item_valuation

            warehouse_counts[warehouse] = warehouse_counts.get(warehouse, 0) + 1
            warehouse_qty[warehouse] = warehouse_qty.get(warehouse, 0) + qty

            category_counts[category] = category_counts.get(category, 0) + 1
            category_qty[category] = category_qty.get(category, 0) + qty
            category_val[category] = category_val.get(category, 0.0) + item_valuation

            status = "HEALTHY"
            if qty == 0:
                status = "OUT_OF_STOCK"
            elif qty <= threshold:
                status = "LOW_STOCK"
            elif qty > (threshold * 5) and qty >= 50:
                status = "OVERSTOCKED"

            detail = {
                "sku": sku,
                "name": name,
                "quantity": qty,
                "threshold": threshold,
                "warehouse": warehouse,
                "category": category,
                "price": price,
                "valuation": item_valuation,
                "deficit": max(0, (threshold * 2) - qty),
                "status": status
            }
            sku_details.append(detail)

            if qty == 0:
                out_of_stock_items.append(detail)
                low_stock_items.append(detail)
            elif qty <= threshold:
                low_stock_items.append(detail)

            if qty > (threshold * 5) and qty >= 50:
                overstocked_items.append(detail)

        return {
            "total_skus": total_skus,
            "total_quantity": total_quantity,
            "total_valuation": round(total_valuation, 2),
            "low_stock_count": len(low_stock_items),
            "low_stock_items": low_stock_items,
            "out_of_stock_count": len(out_of_stock_items),
            "out_of_stock_items": out_of_stock_items,
            "overstocked_count": len(overstocked_items),
            "overstocked_items": overstocked_items,
            "warehouse_counts": warehouse_counts,
            "warehouse_qty": warehouse_qty,
            "category_counts": category_counts,
            "category_qty": category_qty,
            "category_val": category_val,
            "sku_details": sku_details
        }

    def _classify_intent(self, query: str) -> Tuple[str, float]:
        """
        Classifies user intent using a hybrid ML pipeline with rule-based fallback.
        """
        q = query.lower().strip()

        # 1. Deterministic High-Precision Pattern Matching for Superlatives & Custom Sorting
        # Handles queries like "which item has highest value", "what product has lowest stock", "top 10 sorted by quantity"
        if re.search(r"\b(which|what)\s+(item|product|sku)\s+(has|is|have)\s+(the\s+)?(highest|lowest|most|least|maximum|minimum|cheapest|expensive)\b", q) or \
           re.search(r"\b(item|product|sku)\s+with\s+(the\s+)?(highest|lowest|most|least|maximum|minimum)\b", q) or \
           any(w in q for w in [
               "highest value", "highest valuation", "lowest value", "lowest valuation",
               "highest price", "lowest price", "most expensive", "cheapest product", "cheapest item",
               "highest quantity", "lowest quantity", "highest stock", "lowest stock", "most stock", "least stock",
               "most valuable", "sorted by", "sort by", "order by", "ordered by", "rank by", "ranked by"
           ]) or \
           re.search(r"\b(top|lowest|highest|bottom|first)\s+\d+\b", q) or \
           any(w in q for w in ["top inventory", "sorted inventory", "most expensive items", "cheapest items"]):
            return "CUSTOM_SORT_FILTER", 1.0

        if any(w in q for w in ["what can you do", "help", "commands", "capabilities", "what features", "how to use"]):
            return "HELP_CAPABILITIES", 1.0

        if any(w in q for w in ["how many sku", "how many skus", "sku count", "distinct sku", "different sku", "number of sku", "total sku"]):
            return "SKU_COUNT_QUANTITY", 1.0

        if any(w in q for w in ["table", "tabular", "grid", "rows", "columns", "format as table", "in a table", "details in tabular", "spreadsheet", "matrix"]):
            return "TABULAR_REPORT", 1.0

        # 2. Try ML model inference if loaded (with confidence threshold >= 0.60)
        if self.ml_model:
            try:
                probs = self.ml_model.predict_proba([query])[0]
                classes = self.ml_model.classes_
                top_idx = probs.argmax()
                top_intent = classes[top_idx]
                confidence = float(probs[top_idx])
                if confidence >= 0.60:
                    return top_intent, confidence
            except Exception as e:
                logger.warning("ML inference failed: %s. Falling back to rule matcher.", e)

        # 3. Robust Semantic Rule Classifier Fallback
        if any(w in q for w in ["forecast", "predict demand", "future sales", "projected demand", "run rate"]):
            return "DEMAND_FORECAST", 1.0

        if any(w in q for w in ["stock type", "type wise", "category wise", "categories", "product type"]):
            return "STOCK_TYPE_CATEGORY", 1.0

        if any(w in q for w in ["low in stock", "low stock", "reorder", "shortage", "restock", "out of stock", "refill", "purchase order", "depleted", "replenish", "safety stock breach"]):
            return "LOW_STOCK_REORDER", 1.0

        if any(w in q for w in ["value", "valuation", "worth", "cost", "financial", "capital", "asset"]):
            return "FINANCIAL_VALUATION", 1.0

        if any(w in q for w in ["risk", "overstock", "dead stock", "excess", "obsolete", "slow moving"]):
            return "RISK_OVERSTOCK", 1.0

        if any(w in q for w in ["warehouse", "location", "distribution", "north", "south", "east", "west", "site", "storage"]):
            return "WAREHOUSE_LOGISTICS", 1.0

        if any(w in q for w in ["summary", "overview", "executive", "health", "report", "status", "scorecard"]):
            return "EXECUTIVE_OVERVIEW", 1.0

        if any(w in q for w in ["electronics", "apparel", "groceries", "tools", "sports", "product", "search"]):
            return "CATALOG_SEARCH", 0.8

        return "GENERAL_ASSISTANT", 0.5

    @staticmethod
    def _extract_entities(
        query: str,
        inventory_context: List[Dict[str, Any]],
        catalog_context: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """
        Extracts SKU codes, warehouse locations, product categories, limits, and sort options from user queries.
        """
        entities: Dict[str, Any] = {}
        q_upper = query.upper()
        q_lower = query.lower()

        # 1. Extract SKU
        all_skus = set(item.get("sku", "").upper() for item in inventory_context if item.get("sku"))
        all_skus.update(item.get("sku", "").upper() for item in catalog_context if item.get("sku"))

        for sku in all_skus:
            if sku and sku in q_upper:
                entities["sku"] = sku
                break

        if "sku" not in entities:
            sku_match = re.search(r"\b([A-Z0-9]+-[A-Z0-9-]+)\b", q_upper)
            if sku_match:
                entities["sku"] = sku_match.group(1)

        # 2. Extract Warehouse
        warehouses = ["WAREHOUSE-NORTH", "WAREHOUSE-SOUTH", "WAREHOUSE-EAST", "WAREHOUSE-WEST", "CENTRAL-HUB"]
        for wh in warehouses:
            if wh in q_upper or wh.replace("-", " ") in q_upper:
                entities["warehouse"] = wh.title() if "-" not in wh else "-".join(part.capitalize() for part in wh.split("-"))
                break

        # 3. Extract Category
        categories = ["Electronics", "Apparel", "Home & Kitchen", "Groceries", "Tools & Hardware", "Sports & Outdoors"]
        for cat in categories:
            if cat.lower() in q_lower:
                entities["category"] = cat
                break

        # 4. Extract Superlative / Single-Item Question Flag
        is_superlative = bool(
            re.search(r"\b(which|what)\s+(item|product|sku)\s+(has|is|have)\s+(the\s+)?(highest|lowest|most|least|maximum|minimum|cheapest|expensive)\b", q_lower) or
            re.search(r"\b(item|product|sku)\s+with\s+(the\s+)?(highest|lowest|most|least|maximum|minimum)\b", q_lower) or
            any(w in q_lower for w in [
                "highest value", "highest valuation", "lowest value", "lowest valuation",
                "highest price", "lowest price", "most expensive", "cheapest product", "cheapest item",
                "highest quantity", "lowest quantity", "highest stock", "lowest stock", "most stock", "least stock",
                "most valuable"
            ])
        )
        entities["is_superlative"] = is_superlative

        # 5. Extract Limit
        limit_match = re.search(r"\b(?:top|lowest|highest|bottom|first|limit)\s+(\d+)\b", q_lower)
        if not limit_match:
            limit_match = re.search(r"\b(\d+)\s+(?:items|products|records|rows|skus|inventory)\b", q_lower)
        if limit_match:
            try:
                entities["limit"] = int(limit_match.group(1))
            except ValueError:
                pass
        elif is_superlative:
            # For superlative queries without explicit limit, show top 5 in table while highlighting #1
            entities["limit"] = 5

        # 6. Extract Sort Field
        if any(w in q_lower for w in ["valuation", "value", "worth", "holding value", "asset value"]):
            entities["sort_field"] = "valuation"
        elif any(w in q_lower for w in ["price", "cost", "unit price", "rate", "expensive", "cheapest"]):
            entities["sort_field"] = "price"
        elif any(w in q_lower for w in ["quantity", "stock", "qty", "units", "volume"]):
            entities["sort_field"] = "quantity"
        elif any(w in q_lower for w in ["threshold", "reorder", "safety"]):
            entities["sort_field"] = "threshold"
        elif any(w in q_lower for w in ["sku", "code"]):
            entities["sort_field"] = "sku"
        elif any(w in q_lower for w in ["name", "product name", "title"]):
            entities["sort_field"] = "name"
        elif any(w in q_lower for w in ["warehouse", "location"]):
            entities["sort_field"] = "warehouse"
        elif any(w in q_lower for w in ["category", "type"]):
            entities["sort_field"] = "category"
        elif any(w in q_lower for w in ["top", "lowest", "highest", "bottom"]):
            entities["sort_field"] = "quantity"

        # 7. Extract Sort Order (asc vs desc)
        if any(w in q_lower for w in ["lowest", "bottom", "least", "smallest", "ascending", " asc", "min", "cheapest"]):
            entities["sort_order"] = "asc"
        else:
            entities["sort_order"] = "desc"

        return entities

    @staticmethod
    def _synthesize_human_response(
        query: str,
        intent: str,
        metrics: Dict[str, Any],
        entities: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Synthesizes a direct, point-to-point response answering user operational questions.
        """
        entities = entities or {}
        total_skus = metrics["total_skus"]
        low_stock = metrics["low_stock_items"]
        out_of_stock = metrics["out_of_stock_items"]
        total_val = metrics["total_valuation"]
        wh_counts = metrics["warehouse_counts"]
        sku_details = metrics["sku_details"]

        recommendations = []
        risk_level = "LOW"

        # Handle Help / Capabilities Intent (even when database is empty)
        if intent == "HELP_CAPABILITIES":
            answer = (
                f"### Capabilities\n\n"
                f"1. **Tabular & Custom Sorting**: *'can you show top 10 inventory data sorted by quantity'* or *'show all items in tabular form'*\n"
                f"2. **Superlative Extremes**: *'which item has highest value'*, *'which item is most expensive'*, *'which item has lowest stock'*\n"
                f"3. **SKU Portfolio Counts**: *'how many different skus stock has?'*\n"
                f"4. **Low Stock & Reorders**: *'Which items are low in stock and need reordering?'*\n"
                f"5. **Demand Forecasting**: *'forecast demand for LAPTOP-PRO-15 over 14 days'*\n"
                f"6. **Single Item Lookup**: *'what is the stock level of SMARTPHONE-X?'*\n"
                f"7. **Facility Logistics**: *'show me all items in Warehouse-North'*\n"
                f"8. **Financial Valuations**: *'What is our total inventory valuation?'*"
            )
            recommendations.append("Ask any inventory query.")
            return {
                "answer": answer,
                "recommendations": recommendations,
                "risk_level": "LOW"
            }

        if total_skus == 0:
            answer = (
                f"Currently, there are **0 active inventory records** in the database.\n\n"
                f"To seed sample data: `python3 scripts/seed_data.py`"
            )
            recommendations.append("Seed inventory data.")
            return {
                "answer": answer,
                "recommendations": recommendations,
                "risk_level": "LOW"
            }

        # Check for Specific SKU Lookup intent or direct SKU entity reference
        target_sku = entities.get("sku")
        if target_sku and (intent == "SPECIFIC_SKU_LOOKUP" or "what is the stock level of" in query.lower() or "check inventory status for sku" in query.lower()):
            matched_items = [i for i in sku_details if i["sku"].upper() == target_sku.upper()]
            if matched_items:
                item = matched_items[0]
                status_icon = "Healthy"
                if item["status"] == "OUT_OF_STOCK":
                    status_icon = "Out of Stock"
                    risk_level = "CRITICAL"
                elif item["status"] == "LOW_STOCK":
                    status_icon = "Low Stock"
                    risk_level = "HIGH"
                elif item["status"] == "OVERSTOCKED":
                    status_icon = "Overstocked"

                answer = (
                    f"### Single Item Ledger: **{item['sku']}** ({item['name']})\n\n"
                    f"• **Current Stock**: **{item['quantity']} units**\n"
                    f"• **Reorder Safety Point**: **{item['threshold']} units**\n"
                    f"• **Status**: {status_icon}\n"
                    f"• **Warehouse Location**: `{item['warehouse']}`\n"
                    f"• **Product Category**: {item['category']}\n"
                    f"• **Unit Price**: ${item['price']:,.2f}\n"
                    f"• **Total Valuation**: ${item['valuation']:,.2f}"
                )
                if item["deficit"] > 0:
                    answer += f"\n• **Restocking Need**: {item['deficit']} units"
                    recommendations.append(f"Reorder {item['deficit']} units for {item['sku']}.")
                else:
                    recommendations.append(f"Stock healthy for {item['sku']}.")

                return {
                    "answer": answer,
                    "recommendations": recommendations,
                    "risk_level": risk_level
                }

        # Handle Demand Forecast Intent
        if intent == "DEMAND_FORECAST":
            target_sku = entities.get("sku", "target product")
            matched_items = [i for i in sku_details if i["sku"].upper() == str(target_sku).upper()]
            item_name = matched_items[0]["name"] if matched_items else target_sku
            current_qty = matched_items[0]["quantity"] if matched_items else 20
            threshold = matched_items[0]["threshold"] if matched_items else 10

            est_daily_demand = max(1.5, round(threshold * 0.25, 1))
            est_14day_demand = round(est_daily_demand * 14)
            safety_stock = round(est_daily_demand * 5 * 0.8)
            reorder_point = round((est_daily_demand * 5) + safety_stock)

            risk_level = "HIGH" if current_qty < est_14day_demand else "LOW"

            answer = (
                f"### Demand Forecast: **{target_sku}** ({item_name})\n\n"
                f"• **Forecast Horizon**: **14 Days**\n"
                f"• **Current On-Hand Stock**: {current_qty} units\n"
                f"• **Daily Run Rate**: ~{est_daily_demand} units/day\n"
                f"• **Projected 14-Day Demand**: {est_14day_demand} units\n"
                f"• **Recommended Safety Stock**: {safety_stock} units\n"
                f"• **Dynamic Reorder Point (ROP)**: {reorder_point} units"
            )
            recommendations.append(f"Forecast for {target_sku} generated.")
            return {
                "answer": answer,
                "recommendations": recommendations,
                "risk_level": risk_level
            }

        # Handle Warehouse Filter Intent
        target_wh = entities.get("warehouse")
        if target_wh and (intent in ["WAREHOUSE_FILTER", "WAREHOUSE_LOGISTICS"] or "show me all items in" in query.lower()):
            filtered = [i for i in sku_details if i["warehouse"].lower() == target_wh.lower()]
            wh_qty = sum(i["quantity"] for i in filtered)
            wh_val = sum(i["valuation"] for i in filtered)
            table_rows = ["| SKU | Product Name | Qty | Threshold | Price | Status |", "| :--- | :--- | :---: | :---: | :---: | :---: |"]
            for item in filtered:
                table_rows.append(f"| **{item['sku']}** | {item['name']} | {item['quantity']} | {item['threshold']} | ${item['price']:,.2f} | {item['status']} |")

            answer = (
                f"### Inventory for **{target_wh}** ({len(filtered)} SKUs, {wh_qty:,} units, ${wh_val:,.2f}):\n\n"
                f"{chr(10).join(table_rows)}"
            )
            recommendations.append(f"Facility {target_wh} filtered.")
            return {
                "answer": answer,
                "recommendations": recommendations,
                "risk_level": "LOW"
            }

        # Handle Category Filter Intent
        target_cat = entities.get("category")
        if target_cat and intent in ["CATEGORY_FILTER", "CATALOG_SEARCH"]:
            filtered = [i for i in sku_details if i["category"].lower() == target_cat.lower()]
            cat_qty = sum(i["quantity"] for i in filtered)
            cat_val = sum(i["valuation"] for i in filtered)
            table_rows = ["| SKU | Product Name | Warehouse | Qty | Threshold | Price |", "| :--- | :--- | :--- | :---: | :---: | :---: |"]
            for item in filtered:
                table_rows.append(f"| **{item['sku']}** | {item['name']} | {item['warehouse']} | {item['quantity']} | {item['threshold']} | ${item['price']:,.2f} |")

            answer = (
                f"### Category Stock: **{target_cat}** ({len(filtered)} SKUs, {cat_qty:,} units, ${cat_val:,.2f}):\n\n"
                f"{chr(10).join(table_rows)}"
            )
            recommendations.append(f"Category {target_cat} filtered.")
            return {
                "answer": answer,
                "recommendations": recommendations,
                "risk_level": "LOW"
            }

        # Handle Custom Sort / Top-N Filter / Superlative Queries
        if intent in ["CUSTOM_SORT_FILTER", "TOP_N_SORTED"] or (intent == "TABULAR_REPORT" and any(k in query.lower() for k in ["sort", "top", "lowest", "highest", "order by", "rank"])):
            sort_field = entities.get("sort_field", "quantity")
            sort_order = entities.get("sort_order", "desc")
            is_superlative = entities.get("is_superlative", False)
            default_limit = 5 if is_superlative else (10 if any(k in query.lower() for k in ["top", "lowest", "highest", "bottom"]) else len(sku_details))
            limit = entities.get("limit", default_limit)

            filtered_items = list(sku_details)
            target_wh_filter = entities.get("warehouse")
            target_cat_filter = entities.get("category")
            if target_wh_filter:
                filtered_items = [i for i in filtered_items if i["warehouse"].lower() == target_wh_filter.lower()]
            if target_cat_filter:
                filtered_items = [i for i in filtered_items if i["category"].lower() == target_cat_filter.lower()]

            reverse = (sort_order == "desc")
            if sort_field in ["quantity", "threshold", "price", "valuation"]:
                filtered_items.sort(key=lambda x: x.get(sort_field, 0), reverse=reverse)
            else:
                filtered_items.sort(key=lambda x: str(x.get(sort_field, "")).lower(), reverse=reverse)

            display_items = filtered_items[:limit]
            top_item = display_items[0] if display_items else None

            # Special Superlative Answers for single-item extreme questions ("which item has highest value", etc.)
            if is_superlative and top_item:
                # 1. Highest / Lowest Value Questions
                if sort_field in ["valuation", "price"] and any(w in query.lower() for w in ["value", "valuation", "worth", "valuable"]):
                    top_val_item = sorted(sku_details, key=lambda x: x["valuation"], reverse=True)[0]
                    top_price_item = sorted(sku_details, key=lambda x: x["price"], reverse=True)[0]

                    table_rows = [
                        "| Rank | SKU | Product Name | Category | Warehouse | Quantity | Unit Price | Total Valuation | Status |",
                        "| :---: | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: |"
                    ]
                    for rank, it in enumerate(display_items, 1):
                        p_str = f"${it['price']:,.2f}" if it['price'] > 0 else "N/A"
                        v_str = f"${it['valuation']:,.2f}" if it['valuation'] > 0 else "N/A"
                        table_rows.append(f"| {rank} | **{it['sku']}** | {it['name']} | {it['category']} | {it['warehouse']} | {it['quantity']} | {p_str} | {v_str} | {it['status']} |")

                    answer = (
                        f"### Highest Value Inventory Item Analysis\n\n"
                        f"• **Highest Total Holding Valuation**: **{top_val_item['sku']}** ({top_val_item['name']}) — "
                        f"**{top_val_item['quantity']} units** @ ${top_val_item['price']:,.2f} = **${top_val_item['valuation']:,.2f}** ({top_val_item['warehouse']})\n"
                        f"• **Highest Unit Price**: **{top_price_item['sku']}** ({top_price_item['name']}) — "
                        f"**${top_price_item['price']:,.2f}/unit** ({top_price_item['quantity']} units, total ${top_price_item['valuation']:,.2f})\n\n"
                        f"### Top {len(display_items)} Holdings Ranked by Total Valuation\n\n"
                        f"{chr(10).join(table_rows)}\n"
                    )
                    risk_level = "HIGH" if top_price_item["status"] in ["LOW_STOCK", "OUT_OF_STOCK"] else "LOW"
                    recommendations.append(f"Top holding: {top_val_item['sku']}.")
                    return {
                        "answer": answer,
                        "recommendations": recommendations,
                        "risk_level": risk_level
                    }

                # 2. Highest / Lowest Quantity Questions
                elif sort_field == "quantity":
                    table_rows = [
                        "| Rank | SKU | Product Name | Category | Warehouse | Quantity | Reorder Pt | Unit Price | Status |",
                        "| :---: | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: |"
                    ]
                    for rank, it in enumerate(display_items, 1):
                        p_str = f"${it['price']:,.2f}" if it['price'] > 0 else "N/A"
                        table_rows.append(f"| {rank} | **{it['sku']}** | {it['name']} | {it['category']} | {it['warehouse']} | {it['quantity']} | {it['threshold']} | {p_str} | {it['status']} |")

                    answer = (
                        f"### {'Highest' if reverse else 'Lowest'} Stock Item: **{top_item['sku']}** ({top_item['name']})\n\n"
                        f"• **SKU**: **`{top_item['sku']}`** ({top_item['name']})\n"
                        f"• **Quantity**: **{top_item['quantity']} units** (Reorder Point: {top_item['threshold']})\n"
                        f"• **Location**: `{top_item['warehouse']}` | **Category**: {top_item['category']}\n"
                        f"• **Unit Price**: ${top_item['price']:,.2f} | **Total Valuation**: ${top_item['valuation']:,.2f}\n\n"
                        f"### {'Top' if reverse else 'Lowest'} {len(display_items)} Items Ranked by Quantity\n\n"
                        f"{chr(10).join(table_rows)}\n"
                    )
                    risk_level = "HIGH" if top_item["status"] in ["LOW_STOCK", "OUT_OF_STOCK"] else "LOW"
                    recommendations.append(f"{top_item['sku']}: {top_item['quantity']} units.")
                    return {
                        "answer": answer,
                        "recommendations": recommendations,
                        "risk_level": risk_level
                    }

                # 3. Highest / Lowest Price Questions
                elif sort_field == "price":
                    table_rows = [
                        "| Rank | SKU | Product Name | Category | Warehouse | Unit Price | Current Qty | Total Value | Status |",
                        "| :---: | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: |"
                    ]
                    for rank, it in enumerate(display_items, 1):
                        p_str = f"${it['price']:,.2f}" if it['price'] > 0 else "N/A"
                        v_str = f"${it['valuation']:,.2f}" if it['valuation'] > 0 else "N/A"
                        table_rows.append(f"| {rank} | **{it['sku']}** | {it['name']} | {it['category']} | {it['warehouse']} | {p_str} | {it['quantity']} | {v_str} | {it['status']} |")

                    answer = (
                        f"### {'Most Expensive' if reverse else 'Cheapest'} Item: **{top_item['sku']}** ({top_item['name']})\n\n"
                        f"• **SKU**: **`{top_item['sku']}`** ({top_item['name']})\n"
                        f"• **Unit Price**: **${top_item['price']:,.2f}**\n"
                        f"• **Current Stock**: {top_item['quantity']} units (Total Valuation: ${top_item['valuation']:,.2f})\n"
                        f"• **Location**: `{top_item['warehouse']}` | **Category**: {top_item['category']}\n\n"
                        f"### Ranked Items by Unit Price\n\n"
                        f"{chr(10).join(table_rows)}\n"
                    )
                    risk_level = "HIGH" if top_item["status"] in ["LOW_STOCK", "OUT_OF_STOCK"] else "LOW"
                    recommendations.append(f"{top_item['sku']}: ${top_item['price']:,.2f}.")
                    return {
                        "answer": answer,
                        "recommendations": recommendations,
                        "risk_level": risk_level
                    }

            # Standard Custom Sort / Top-N Table Answer
            has_oos = any(i["status"] == "OUT_OF_STOCK" for i in display_items)
            has_low = any(i["status"] == "LOW_STOCK" for i in display_items)
            risk_level = "CRITICAL" if has_oos else ("HIGH" if has_low else "LOW")

            field_labels = {
                "quantity": "Quantity",
                "price": "Unit Price",
                "valuation": "Total Valuation",
                "threshold": "Reorder Threshold",
                "warehouse": "Warehouse Location",
                "category": "Category",
                "sku": "SKU",
                "name": "Product Name"
            }
            label = field_labels.get(sort_field, sort_field.title())
            order_label = "Descending" if reverse else "Ascending"

            table_rows = [
                "| Rank | SKU | Product Name | Category | Warehouse | Quantity | Reorder Pt | Unit Price | Total Value | Status |",
                "| :---: | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: |"
            ]

            for rank, item in enumerate(display_items, 1):
                price_str = f"${item['price']:,.2f}" if item['price'] > 0 else "N/A"
                val_str = f"${item['valuation']:,.2f}" if item['valuation'] > 0 else "N/A"
                row = f"| {rank} | **{item['sku']}** | {item['name']} | {item['category']} | {item['warehouse']} | {item['quantity']} | {item['threshold']} | {price_str} | {val_str} | {item['status']} |"
                table_rows.append(row)

            filter_note = ""
            if target_wh_filter:
                filter_note += f" in facility **{target_wh_filter}**"
            if target_cat_filter:
                filter_note += f" within category **{target_cat_filter}**"

            answer = (
                f"### Top {len(display_items)} Inventory Items (Sorted by {label} {order_label}){filter_note}\n\n"
                f"{chr(10).join(table_rows)}"
            )

            recommendations.append(f"Sorted by {label} {order_label}.")
            return {
                "answer": answer,
                "recommendations": recommendations,
                "risk_level": risk_level
            }

        if intent == "SKU_COUNT_QUANTITY":
            risk_level = "LOW"
            answer = (
                f"There are **{total_skus} distinct SKUs** in stock.\n\n"
                f"• **Total Units**: {metrics['total_quantity']:,}\n"
                f"• **Total Valuation**: ${total_val:,.2f}\n"
                f"• **Warehouses**: {', '.join(wh_counts.keys()) if wh_counts else 'None'}"
            )
            recommendations.append(f"Track {total_skus} SKUs.")

        elif intent == "TABULAR_REPORT":
            risk_level = "LOW"

            table_rows = []
            table_rows.append("| SKU | Product Name | Category | Warehouse | Qty | Reorder Pt | Price | Total Value | Status |")
            table_rows.append("| :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: |")

            for item in sku_details:
                price_str = f"${item['price']:,.2f}" if item['price'] > 0 else "N/A"
                val_str = f"${item['valuation']:,.2f}" if item['valuation'] > 0 else "N/A"
                if item["status"] == "OUT_OF_STOCK":
                    risk_level = "HIGH"
                elif item["status"] == "LOW_STOCK" and risk_level != "HIGH":
                    risk_level = "MEDIUM"

                row = f"| **{item['sku']}** | {item['name']} | {item['category']} | {item['warehouse']} | {item['quantity']} | {item['threshold']} | {price_str} | {val_str} | {item['status']} |"
                table_rows.append(row)

            answer = (
                f"### Inventory Ledger ({total_skus} SKUs, {metrics['total_quantity']:,} units, ${total_val:,.2f}):\n\n"
                f"{chr(10).join(table_rows)}"
            )
            recommendations.append("Full tabular ledger rendered.")

        elif intent == "STOCK_TYPE_CATEGORY":
            risk_level = "LOW"
            cat_lines = []
            for cat, qty in metrics["category_qty"].items():
                sku_cnt = metrics["category_counts"].get(cat, 0)
                val = metrics["category_val"].get(cat, 0.0)
                val_str = f" (${val:,.2f})" if val > 0 else ""
                cat_lines.append(f"• **{cat}**: {qty:,} units across {sku_cnt} SKU(s){val_str}")

            answer = (
                f"**Stock by Category ({metrics['total_quantity']:,} units across {total_skus} SKUs):**\n\n"
                + "\n".join(cat_lines)
            )
            recommendations.append("Category counts reviewed.")

        elif intent == "LOW_STOCK_REORDER":
            if out_of_stock:
                risk_level = "CRITICAL"
            elif len(low_stock) > 0:
                risk_level = "HIGH"

            if low_stock:
                table_rows = [
                    "| SKU | Product Name | Warehouse | Qty | Threshold | Deficit |",
                    "| :--- | :--- | :--- | :---: | :---: | :---: |"
                ]
                for item in low_stock:
                    table_rows.append(f"| **{item['sku']}** | {item['name']} | {item['warehouse']} | {item['quantity']} | {item['threshold']} | {item['deficit']} |")

                total_reorder_units = sum(i["deficit"] for i in low_stock)
                total_reorder_cost = sum(i["deficit"] * i["price"] for i in low_stock)
                cost_str = f" (Est. Cost: ${total_reorder_cost:,.2f})" if total_reorder_cost > 0 else ""

                answer = (
                    f"### Low Stock Items ({len(low_stock)} SKUs requiring reorder)\n\n"
                    f"{chr(10).join(table_rows)}\n\n"
                    f"**Total Replenishment Need**: {total_reorder_units} units across {len(low_stock)} SKU(s){cost_str}."
                )

                for item in low_stock:
                    recommendations.append(f"Reorder {item['deficit']} units of {item['sku']}.")
            else:
                answer = f"All **{total_skus} active SKUs** are currently above their reorder thresholds. No replenishment is needed."
                recommendations.append("All stock levels healthy.")

        elif intent == "FINANCIAL_VALUATION":
            risk_level = "LOW"
            sorted_by_val = sorted(sku_details, key=lambda x: x["valuation"], reverse=True)
            top_val_items = sorted_by_val[:5]

            val_breakdown = "\n".join([
                f"• **{item['sku']}** ({item['name']}): {item['quantity']} units @ ${item['price']:,.2f} = **${item['valuation']:,.2f}**"
                for item in top_val_items if item['valuation'] > 0
            ])

            wh_val: Dict[str, float] = {}
            for item in sku_details:
                wh = item["warehouse"]
                wh_val[wh] = wh_val.get(wh, 0.0) + item["valuation"]

            wh_summary = "\n".join([f"• **{k}**: ${v:,.2f}" for k, v in wh_val.items() if v > 0])

            answer = (
                f"**Total Portfolio Valuation**: **${total_val:,.2f}** ({metrics['total_quantity']:,} units across {total_skus} SKUs)\n\n"
                f"**Facility Allocation:**\n{wh_summary if wh_summary else '• None'}\n\n"
                f"**Top Value Holdings:**\n{val_breakdown if val_breakdown else '• None'}"
            )
            recommendations.append(f"Total valuation: ${total_val:,.2f}.")

        elif intent == "WAREHOUSE_LOGISTICS":
            risk_level = "LOW"
            wh_breakdown_lines = []
            for wh, count in wh_counts.items():
                qty = metrics["warehouse_qty"].get(wh, 0)
                wh_breakdown_lines.append(f"• **{wh}**: {qty:,} units across {count} SKU(s)")

            answer = (
                f"**Warehouse Stock Distribution ({metrics['total_quantity']:,} total units across {len(wh_counts)} locations):**\n\n"
                + "\n".join(wh_breakdown_lines)
            )
            recommendations.append("Warehouse distribution reviewed.")

        elif intent == "RISK_OVERSTOCK":
            overstocked = metrics["overstocked_items"]
            risk_level = "LOW"
            if overstocked:
                items_str = "\n".join([f"• **{i['sku']}** ({i['name']}): {i['quantity']} units (Threshold: {i['threshold']})" for i in overstocked])
                answer = f"**Overstocked Items ({len(overstocked)} SKUs exceeding 5x safety threshold):**\n\n{items_str}"
            else:
                answer = "**Overstock Status**: No overstocked SKUs detected (all items within 5x safety threshold)."
            recommendations.append("Overstock check completed.")

        elif intent == "EXECUTIVE_OVERVIEW":
            risk_level = "LOW"
            answer = (
                f"### Executive Inventory Overview\n\n"
                f"• **Total SKUs**: {total_skus}\n"
                f"• **Total Units**: {metrics['total_quantity']:,}\n"
                f"• **Total Valuation**: ${total_val:,.2f}\n"
                f"• **Low Stock / Reorder**: {len(low_stock)} SKU(s) ({len(out_of_stock)} out of stock)\n"
                f"• **Warehouses**: {len(wh_counts)} ({', '.join(wh_counts.keys())})"
            )
            recommendations.append("Executive brief summarized.")

        elif intent == "CATALOG_SEARCH":
            risk_level = "LOW"
            answer = (
                f"**Catalog Scope for '{query}':**\n\n"
                f"• **Active SKUs**: {total_skus}\n"
                f"• **Total Units**: {metrics['total_quantity']:,}\n"
                f"• **Valuation**: ${total_val:,.2f}\n"
                f"• **Categories**: {', '.join(metrics['category_counts'].keys())}"
            )
            recommendations.append(f"Search for '{query}' returned.")

        else:  # GENERAL_ASSISTANT
            risk_level = "LOW"
            answer = (
                f"**Inventory Snapshot:**\n\n"
                f"• **SKUs**: {total_skus} distinct items\n"
                f"• **Total Stock**: {metrics['total_quantity']:,} units\n"
                f"• **Valuation**: ${total_val:,.2f}\n"
                f"• **Low Stock**: {len(low_stock)} items needing reorder ({len(out_of_stock)} out of stock)"
            )
            recommendations.append("Inventory status provided.")

        return {
            "answer": answer,
            "recommendations": recommendations,
            "risk_level": risk_level
        }

    def _try_llm_generation(
        self,
        query: str,
        metrics: Dict[str, Any],
        inventory_context: List[Dict[str, Any]],
        catalog_context: List[Dict[str, Any]],
        system_instruction: Optional[str] = None
    ) -> Optional[Dict[str, Any]]:
        """
        Attempts external LLM API generation if API keys are configured.
        """
        if not self.openai_api_key:
            return None

        system_prompt = system_instruction or (
            "You are a Senior Inventory Operations Manager & Supply Chain AI Assistant. "
            "You provide clear, articulate, highly professional, warm, human-like answers. "
            "Your output must be a valid JSON object with keys: "
            "'answer' (string formatted with Markdown), "
            "'recommendations' (array of strings), "
            "'risk_level' ('CRITICAL', 'HIGH', 'MEDIUM', or 'LOW')."
        )

        user_prompt = (
            f"User Query: {query}\n\n"
            f"Inventory Summary Metrics: {json.dumps(metrics, indent=2)}\n"
            f"Catalog Context Items Count: {len(catalog_context)}\n"
            f"Raw Inventory Records: {json.dumps(inventory_context[:15], indent=2)}\n"
        )

        try:
            headers = {
                "Authorization": f"Bearer {self.openai_api_key}",
                "Content-Type": "application/json"
            }
            payload = {
                "model": self.llm_model,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                "response_format": {"type": "json_object"},
                "temperature": 0.5
            }
            with httpx.Client(timeout=10.0) as client:
                res = client.post("https://api.openai.com/v1/chat/completions", json=payload, headers=headers)
                if res.status_code == 200:
                    content = res.json()["choices"][0]["message"]["content"]
                    parsed = json.loads(content)
                    return {
                        "answer": parsed.get("answer", ""),
                        "recommendations": parsed.get("recommendations", []),
                        "risk_level": parsed.get("risk_level", "LOW")
                    }
        except Exception:
            pass

        return None
