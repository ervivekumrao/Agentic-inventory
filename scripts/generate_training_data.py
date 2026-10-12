#!/usr/bin/env python3
"""
Smart Inventory Management System - AI Agent Training Data Generator
Generates:
1. Intent Classification Dataset (JSON and CSV) with annotated intents and entity slots.
2. Instruction-Tuning Dataset (JSONL) in OpenAI Chat & Alpaca SFT format.

Supports offline and repeatable dataset generation for training the Smart Inventory AI Agent.
"""

import os
import json
import csv
import random
import argparse
from typing import List, Dict, Any

# Domain Entities from Smart Inventory System
CATEGORIES = [
    "Electronics", "Apparel", "Home & Kitchen", "Groceries",
    "Tools & Hardware", "Sports & Outdoors"
]

WAREHOUSES = [
    "Warehouse-North", "Warehouse-South", "Warehouse-East",
    "Warehouse-West", "Central-Hub"
]

PRODUCTS = [
    {"sku": "LAPTOP-PRO-15", "name": "Pro 15-inch Laptop", "category": "Electronics", "price": 1299.99},
    {"sku": "SMARTPHONE-X", "name": "SmartPhone X", "category": "Electronics", "price": 899.50},
    {"sku": "WIRELESS-BUDS", "name": "Wireless Earbuds", "category": "Electronics", "price": 149.99},
    {"sku": "MECHANICAL-KEYBOARD", "name": "RGB Mechanical Keyboard", "category": "Electronics", "price": 89.99},
    {"sku": "4K-MONITOR", "name": "27-inch 4K Monitor", "category": "Electronics", "price": 349.00},
    {"sku": "DENIM-JACKET", "name": "Classic Denim Jacket", "category": "Apparel", "price": 79.99},
    {"sku": "RUNNING-SHOES", "name": "AirStride Running Shoes", "category": "Apparel", "price": 119.50},
    {"sku": "MERINO-SWEATER", "name": "Merino Wool Sweater", "category": "Apparel", "price": 89.00},
    {"sku": "COTTON-HOODIE", "name": "Fleece Pullover Hoodie", "category": "Apparel", "price": 55.00},
    {"sku": "LEATHER-BELT", "name": "Genuine Leather Belt", "category": "Apparel", "price": 39.99},
    {"sku": "BLENDER-PRO", "name": "High-Speed Blender", "category": "Home & Kitchen", "price": 129.99},
    {"sku": "COFFEE-MAKER", "name": "Espresso Machine", "category": "Home & Kitchen", "price": 199.99},
    {"sku": "AIR-FRYER", "name": "Digital Air Fryer 5.8L", "category": "Home & Kitchen", "price": 109.99},
    {"sku": "CHEF-KNIFE", "name": "Damascus Chef Knife", "category": "Home & Kitchen", "price": 85.00},
    {"sku": "CERAMIC-MUG-SET", "name": "Ceramic Coffee Mug Set", "category": "Home & Kitchen", "price": 32.50},
    {"sku": "ORGANIC-HONEY", "name": "Raw Organic Honey", "category": "Groceries", "price": 18.99},
    {"sku": "ESPRESSO-BEANS", "name": "Artisan Espresso Coffee Beans", "category": "Groceries", "price": 24.50},
    {"sku": "ALMOND-BUTTER", "name": "Natural Creamy Almond Butter", "category": "Groceries", "price": 12.99},
    {"sku": "GREEN-TEA", "name": "Japanese Sencha Green Tea", "category": "Groceries", "price": 15.00},
    {"sku": "DARK-CHOCOLATE", "name": "70% Dark Chocolate Bar", "category": "Groceries", "price": 4.99},
    {"sku": "CORDLESS-DRILL", "name": "20V Cordless Drill Driver", "category": "Tools & Hardware", "price": 149.99},
    {"sku": "TOOL-SET", "name": "150-Piece Mechanics Tool Set", "category": "Tools & Hardware", "price": 129.00},
    {"sku": "LED-WORK-LIGHT", "name": "Rechargeable LED Work Light", "category": "Tools & Hardware", "price": 59.99},
    {"sku": "DIGITAL-MULTIMETER", "name": "Auto-Ranging Digital Multimeter", "category": "Tools & Hardware", "price": 39.99},
    {"sku": "STAINLESS-SCREWS", "name": "Assorted Stainless Screws Box", "category": "Tools & Hardware", "price": 24.99},
    {"sku": "YOGA-MAT", "name": "Eco-Friendly Yoga Mat", "category": "Sports & Outdoors", "price": 29.99},
    {"sku": "CAMPING-TENT", "name": "4-Person Waterproof Camping Tent", "category": "Sports & Outdoors", "price": 179.99},
    {"sku": "HIKING-BACKPACK", "name": "50L Hiking Backpack", "category": "Sports & Outdoors", "price": 109.00},
    {"sku": "INSULATED-FLASK", "name": "Stainless Steel Insulated Flask", "category": "Sports & Outdoors", "price": 34.99},
    {"sku": "BIKE-HELMET", "name": "Commuter Cycling Helmet", "category": "Sports & Outdoors", "price": 49.99}
]

# Query Templates per Intent
INTENT_TEMPLATES = {
    "CUSTOM_SORT_FILTER": [
        "which item has highest value",
        "which item has the highest value",
        "which product has highest value",
        "which product has the highest value",
        "which item has the highest total value",
        "which item has highest valuation",
        "which product has highest valuation",
        "which item has lowest value",
        "which item has highest price",
        "which item has lowest price",
        "which item is the most expensive",
        "which product is the most expensive",
        "which item is the cheapest",
        "which product is the cheapest",
        "which item has highest stock",
        "which item has highest quantity",
        "which item has the most stock",
        "which item has lowest stock",
        "which item has lowest quantity",
        "which item has the least stock",
        "what is our most valuable item",
        "what is the highest value item",
        "what is our most expensive product",
        "what item has the highest stock",
        "what product has the lowest stock",
        "item with highest quantity",
        "item with highest value",
        "item with lowest quantity",
        "item with lowest stock",
        "product with highest valuation",
        "can you show top 10 inventory data sorted by quantity",
        "show top 10 inventory data sorted by quantity",
        "show top 5 items sorted by quantity",
        "top 10 inventory data sorted by quantity",
        "top 10 items sorted by quantity descending",
        "top 5 inventory items sorted by stock",
        "show top 10 products sorted by quantity",
        "can you show top 10 items sorted by price",
        "show top 5 items sorted by valuation",
        "top 10 inventory data sorted by valuation",
        "show lowest 5 items by stock",
        "show lowest 5 items sorted by quantity",
        "bottom 5 items by quantity",
        "sort inventory data by quantity",
        "show inventory sorted by quantity",
        "display all items sorted by price ascending",
        "show top 10 items sorted by reorder threshold",
        "list top 10 products with highest quantity",
        "list lowest 10 products by quantity",
        "rank items by stock quantity",
        "show top 10 inventory records ordered by quantity",
        "can you show the top 10 products sorted by available stock?",
        "sort all inventory items by valuation descending",
        "show top 5 most expensive products in stock",
        "show lowest 5 inventory items by stock count",
        "order inventory by quantity descending",
        "show items ordered by price",
        "sort stock by valuation",
        "top 20 inventory items sorted by quantity",
        "top 3 products by stock"
    ],
    "TABULAR_REPORT": [
        "can you share this data in more details in tabular form?",
        "show me all items in a table",
        "format the inventory as a table",
        "give me a tabular report of all stock",
        "display details in tabular format please",
        "render a markdown grid with all inventory columns",
        "show full inventory spreadsheet view",
        "can you list all products in tabular form with quantities and prices?",
        "generate a complete stock ledger table",
        "table view of current stock items",
        "show inventory matrix with reorder points and locations",
        "provide tabular summary of all inventory records",
        "i need a tabular breakdown of every SKU",
        "print inventory grid",
        "output stock items in table layout"
    ],
    "SKU_COUNT_QUANTITY": [
        "how many different skus stock has?",
        "how many distinct skus do we carry?",
        "what is our total sku count?",
        "how many unique products are in the system?",
        "number of distinct skus in stock",
        "total count of active skus",
        "how many different items are in inventory?",
        "tell me the total number of skus",
        "what is the total physical unit count and sku count?",
        "how many distinct product skus are registered in the warehouse?",
        "distinct sku count check",
        "give me the total sku number"
    ],
    "STOCK_TYPE_CATEGORY": [
        "give me total stock count stock type wise",
        "show inventory breakdown by category",
        "break down stock units and valuation by product type",
        "how is inventory distributed across categories?",
        "what are our quantities per category?",
        "show category-wise stock allocation",
        "category distribution report",
        "how many units belong to each product type?",
        "give me category breakdown with valuations",
        "category level stock aggregation",
        "stock type wise volume and dollar value summary"
    ],
    "LOW_STOCK_REORDER": [
        "Which items are low in stock and need reordering?",
        "which items are low in stock?",
        "are any products running out of stock?",
        "show low stock items",
        "what items need replenishment?",
        "check items below reorder threshold",
        "which skus require purchase orders immediately?",
        "do we have any inventory shortage?",
        "show out of stock and critically depleted products",
        "which products reached minimum threshold?",
        "list all skus that have low inventory cushions",
        "calculate deficit units needed to restock safety cushion",
        "replenishment order audit for depleted items",
        "what items are out of stock right now?",
        "restock recommendations for low inventory"
    ],
    "FINANCIAL_VALUATION": [
        "What is our total inventory valuation and warehouse breakdown?",
        "what is the total worth of our inventory?",
        "calculate overall portfolio valuation",
        "how much capital is tied up in stock?",
        "what is the total dollar value of our holdings?",
        "financial audit of inventory value",
        "show total inventory cost and valuation",
        "what are our most expensive inventory holdings?",
        "capital valuation breakdown across facilities",
        "how much is our inventory portfolio worth?",
        "what is the balance sheet asset value of inventory?"
    ],
    "WAREHOUSE_LOGISTICS": [
        "show warehouse distribution of inventory",
        "how is stock distributed across facilities?",
        "break down inventory by warehouse location",
        "facility footprint and unit breakdown",
        "what is the inventory count in each distribution center?",
        "show stock volume per warehouse",
        "logistics breakdown across all sites",
        "unit and sku spread across our 5 warehouses",
        "where is our inventory physically stored?",
        "warehouse storage capacity and current stock allocation"
    ],
    "RISK_OVERSTOCK": [
        "what is our risk assessment for overstocked items?",
        "do we have excess capital locked in overstock?",
        "identify slow-moving or dead stock risks",
        "flag skus holding more than 5x safety threshold",
        "inventory carrying cost risk analysis",
        "show overstocked items and stockout risks",
        "which items have too much stock sitting idle?",
        "analyze obsolete and surplus inventory exposure",
        "evaluate capital tie-up risk across products"
    ],
    "EXECUTIVE_OVERVIEW": [
        "Give me an executive inventory health brief",
        "high level executive summary of operations",
        "give me an executive brief on inventory health",
        "operations leadership overview report",
        "executive inventory scorecard and status",
        "summarize current supply chain health and risk status",
        "what is the overall health score of our stock?",
        "generate senior management inventory dashboard brief",
        "operational summary for the chief operations officer"
    ],
    "CATALOG_SEARCH": [
        "search catalog for {category}",
        "what products do we have under {category}?",
        "find all items in the {category} department",
        "look up products matching {category}",
        "show catalog listings for {category}"
    ],
    "SPECIFIC_SKU_LOOKUP": [
        "what is the current stock level of {sku}?",
        "check inventory status for SKU {sku}",
        "how many units of {sku} are available?",
        "where is {sku} stored and how much is in stock?",
        "give me details on product {name} with SKU {sku}",
        "is {sku} below its reorder point?",
        "lookup SKU {sku}",
        "what is the price and stock level of {sku}?"
    ],
    "WAREHOUSE_FILTER": [
        "show me all items in {warehouse}",
        "what stock is currently stored at {warehouse}?",
        "filter inventory for {warehouse}",
        "list all products available at {warehouse}",
        "how many units and skus are located in {warehouse}?"
    ],
    "CATEGORY_FILTER": [
        "show all inventory items in category {category}",
        "list stock items belonging to {category}",
        "filter products by {category} category",
        "how much stock do we hold for {category}?"
    ],
    "DEMAND_FORECAST": [
        "what is the demand forecast for {sku}?",
        "forecast sales for {sku} over the next 14 days",
        "predict demand trend and reorder timing for {sku}",
        "how much demand is expected for {name} ({sku})?",
        "run a 14-day demand prediction for {sku}",
        "will {sku} run out based on projected demand?"
    ],
    "HELP_CAPABILITIES": [
        "what can you do?",
        "what questions can I ask you?",
        "help me understand your capabilities",
        "how do I use the inventory ai assistant?",
        "list all commands and queries you support",
        "what features does this assistant offer?"
    ],
    "GENERAL_ASSISTANT": [
        "hello",
        "hi there",
        "good morning assistant",
        "what is the status of the inventory database?",
        "can you assist me with inventory management?",
        "who are you and what do you do?",
        "are all systems operational?"
    ]
}

def generate_intent_dataset(target_samples: int = 1700) -> List[Dict[str, Any]]:
    """
    Generates a balanced dataset of queries mapped to intents and entities.
    """
    dataset = []
    intents = list(INTENT_TEMPLATES.keys())
    per_intent_count = max(35, target_samples // len(intents))

    for intent, templates in INTENT_TEMPLATES.items():
        intent_queries_dict: Dict[str, Dict[str, Any]] = {}

        for tmpl in templates:
            # Expand parameterized templates
            if "{sku}" in tmpl or "{name}" in tmpl:
                for prod in PRODUCTS:
                    q = tmpl.format(sku=prod["sku"], name=prod["name"], category=prod["category"])
                    intent_queries_dict[q] = {"sku": prod["sku"], "category": prod["category"]}
            elif "{category}" in tmpl:
                for cat in CATEGORIES:
                    q = tmpl.format(category=cat)
                    intent_queries_dict[q] = {"category": cat}
            elif "{warehouse}" in tmpl:
                for wh in WAREHOUSES:
                    q = tmpl.format(warehouse=wh)
                    intent_queries_dict[q] = {"warehouse": wh}
            else:
                intent_queries_dict[tmpl] = {}

        # Add variations (prefixes, suffixes, casual styles)
        prefixes = [
            "", "please ", "can you ", "could you ", "kindly ",
            "i want to ", "i need to ", "tell me ", "show me "
        ]
        expanded = []
        for text, entities in intent_queries_dict.items():
            expanded.append({"query": text, "intent": intent, "entities": entities})
            for p in prefixes:
                if p and not text.lower().startswith(p):
                    expanded.append({
                        "query": f"{p}{text[0].lower()}{text[1:]}",
                        "intent": intent,
                        "entities": entities
                    })

        # Sample or replicate to reach target count
        random.shuffle(expanded)
        selected = expanded[:per_intent_count]
        dataset.extend(selected)

    random.shuffle(dataset)
    return dataset

def generate_instruction_tuning_dataset(target_samples: int = 350) -> List[Dict[str, Any]]:
    """
    Generates multi-turn / single-turn dialogues formatted for LLM instruction fine-tuning.
    Compatible with OpenAI Chat JSONL format.
    """
    dialogues = []
    system_prompt = (
        "You are the Senior Inventory Operations AI Director for Smart Inventory. "
        "You provide structured, articulate, actionable supply chain answers with markdown tables, "
        "operational risk levels, and clear recommendations."
    )

    for i in range(target_samples):
        prod = random.choice(PRODUCTS)
        wh = random.choice(WAREHOUSES)
        cat = random.choice(CATEGORIES)

        scenarios = [
            {
                "user": "which item has highest value",
                "assistant": (
                    "### 🏆 Highest Value Inventory Item Analysis\n\n"
                    "The inventory item with the **highest total asset valuation** is:\n\n"
                    f"• **SKU**: `{prod['sku']}`\n"
                    f"• **Product Name**: **{prod['name']}**\n"
                    f"• **Category**: {prod['category']}\n"
                    f"• **Warehouse Location**: `{wh}`\n"
                    f"• **Current Stock Quantity**: 57 units\n"
                    f"• **Unit Price**: ${prod['price']:,.2f}\n"
                    f"• **Total Holding Valuation**: ${57 * prod['price']:,.2f}\n"
                    "• **Status**: 🔵 Overstocked\n\n"
                    "*(Note: In terms of unit price, **LAPTOP-PRO-15** is our most expensive item at **$1,299.99/unit**).* \n\n"
                    "**Recommendations:**\n"
                    "1. Evaluate holding costs and insurance coverage for top-tier capital holdings.\n"
                    "2. Maintain routine cycle counts for high-value warehouse bins."
                )
            },
            {
                "user": "can you show top 10 inventory data sorted by quantity",
                "assistant": (
                    "### Top 10 Inventory Items (Sorted by Quantity Descending)\n\n"
                    "Here are the top 10 inventory items ranked by physical stock quantity:\n\n"
                    "| Rank | SKU | Product Name | Category | Warehouse | Quantity | Reorder Pt | Unit Price | Total Value | Status |\n"
                    "| :---: | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: |\n"
                    "| 1 | **CORDLESS-DRILL** | 20V Cordless Drill Driver | Tools & Hardware | Warehouse-South | 210 | 15 | $149.99 | $31,497.90 | 🔵 Overstocked |\n"
                    "| 2 | **RUNNING-SHOES** | AirStride Running Shoes | Apparel | Warehouse-North | 180 | 20 | $119.50 | $21,510.00 | 🔵 Overstocked |\n"
                    "| 3 | **LAPTOP-PRO-15** | Pro 15-inch Laptop | Electronics | Central-Hub | 95 | 10 | $1,299.99 | $123,499.05 | 🟢 Healthy |\n\n"
                    "**Recommendations:**\n"
                    "1. Evaluate carrying cost of top-volume items to optimize capital allocation.\n"
                    "2. Maintain routine cycle counts for top inventory holdings."
                )
            },
            {
                "user": f"What is the stock level and status of {prod['sku']} ({prod['name']})?",
                "assistant": (
                    f"### Operational Audit: **{prod['sku']}** ({prod['name']})\n\n"
                    f"- **Product Category**: {prod['category']}\n"
                    f"- **Warehouse Location**: {wh}\n"
                    f"- **Current Stock**: 4 units (Threshold: 10 units)\n"
                    f"- **Unit Price**: ${prod['price']:,.2f}\n"
                    f"- **Total Holding Value**: ${4 * prod['price']:,.2f}\n"
                    f"- **Inventory Status**: ⚠️ **LOW_STOCK** (Deficit: 16 units to reach 2x safety stock)\n\n"
                    f"**Recommendations:**\n"
                    f"1. Issue an expedited purchase order for 16 units to restore buffer stock at {wh}.\n"
                    f"2. Verify lead time with primary vendor for {prod['category']}."
                )
            },
            {
                "user": f"Can you give me a tabular report of items stored in {wh}?",
                "assistant": (
                    f"Here is the inventory ledger for **{wh}**:\n\n"
                    f"| SKU | Product Name | Category | Quantity | Reorder Point | Unit Price | Status |\n"
                    f"| :--- | :--- | :--- | :---: | :---: | :---: | :---: |\n"
                    f"| **{prod['sku']}** | {prod['name']} | {prod['category']} | 25 | 10 | ${prod['price']:,.2f} | 🟢 Healthy |\n\n"
                    f"**Summary**: Warehouse operations at {wh} are normal with no active fulfillment blockers."
                )
            },
            {
                "user": f"Which items in {cat} are critically low in stock?",
                "assistant": (
                    f"### Critical Replenishment Audit: **{cat}** Category\n\n"
                    f"A scan across our 5 facility locations identified depleted inventory in {cat}:\n\n"
                    f"- **{prod['sku']}** ({prod['name']}): Current quantity is **3 units** against a reorder threshold of **10 units**.\n"
                    f"- **Estimated Procurement Outlay**: ${17 * prod['price']:,.2f} to restore 2x threshold cushion.\n\n"
                    f"**Operational Risk**: **HIGH**.\n\n"
                    f"**Actionable Next Steps:**\n"
                    f"1. Create purchase requisition for SKU {prod['sku']}.\n"
                    f"2. Prioritize supplier delivery to regional distribution center."
                )
            },
            {
                "user": f"What is the 14-day demand forecast for {prod['sku']}?",
                "assistant": (
                    f"### Demand Forecast & Safety Stock Analysis: **{prod['sku']}**\n\n"
                    f"- **Forecast Horizon**: 14 Days\n"
                    f"- **Projected Daily Run Rate**: ~2.8 units/day\n"
                    f"- **Projected 14-Day Demand**: 39 units (Confidence: 91%)\n"
                    f"- **Recommended Safety Stock**: 12 units (based on 5-day lead time volatility)\n"
                    f"- **Dynamic Reorder Point (ROP)**: 26 units\n\n"
                    f"**Recommendation**: With current inventory below projected 14-day consumption, trigger restocking order immediately."
                )
            }
        ]

        scen = random.choice(scenarios)
        dialogues.append({
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": scen["user"]},
                {"role": "assistant", "content": scen["assistant"]}
            ]
        })

    return dialogues

def main():
    parser = argparse.ArgumentParser(description="Smart Inventory AI Training Data Generator")
    parser.add_argument("--output-dir", default="forecasting-service/data", help="Target output directory")
    parser.add_argument("--intent-samples", type=int, default=1700, help="Number of intent samples to generate")
    parser.add_argument("--sft-samples", type=int, default=350, help="Number of instruction-tuning dialogues")
    args = parser.parse_args()

    os.makedirs(args.output_dir, exist_ok=True)

    # 1. Generate Intent Classification Dataset
    print(f"📦 Generating {args.intent_samples} intent classification samples...")
    intent_data = generate_intent_dataset(args.intent_samples)

    json_path = os.path.join(args.output_dir, "intent_dataset.json")
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(intent_data, f, indent=2)

    csv_path = os.path.join(args.output_dir, "intent_dataset.csv")
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["query", "intent", "entities"])
        writer.writeheader()
        for item in intent_data:
            writer.writerow({
                "query": item["query"],
                "intent": item["intent"],
                "entities": json.dumps(item["entities"])
            })

    print(f"✅ Saved Intent Dataset: {json_path} ({len(intent_data)} records)")
    print(f"✅ Saved Intent CSV: {csv_path}")

    # 2. Generate Instruction-Tuning Dataset
    print(f"🧠 Generating {args.sft_samples} SFT instruction-tuning dialogues...")
    sft_data = generate_instruction_tuning_dataset(args.sft_samples)

    jsonl_path = os.path.join(args.output_dir, "instruction_tuning_dataset.jsonl")
    with open(jsonl_path, "w", encoding="utf-8") as f:
        for entry in sft_data:
            f.write(json.dumps(entry) + "\n")

    print(f"✅ Saved Instruction Tuning Dataset: {jsonl_path} ({len(sft_data)} dialogues)")
    print("🎉 Dataset generation completed successfully!")

if __name__ == "__main__":
    main()
