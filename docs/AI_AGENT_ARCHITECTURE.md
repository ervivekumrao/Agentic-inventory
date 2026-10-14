<!--
  Copyright (c) 2026 Vivek Umrao.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Agentic AI Assistant Architecture & Engineering Specs

This document details the architecture, machine learning intent classification pipeline, entity extraction engine, demand forecasting algorithms, and response generation engine of the **Agentic AI Assistant**.

---

## 🎯 1. Overview & Operational Objectives

The Agentic AI Assistant acts as a virtual **Senior Inventory Operations Manager & Supply Chain Director**. It ingests live database context (relational database stock records and MongoDB catalog metadata), performs quantitative analytics, extracts named entities (SKU, Warehouse, Category), and generates articulate, human-like answers.

---

## 🏗️ 2. Processing Pipeline

```mermaid
graph TD
    Request[HTTP Request: query + context] --> Preprocessor[1. Text Preprocessor]
    Request --> Calculator[2. Context Analytics Calculator]

    Preprocessor --> MLClassifier[3. Trained ML Intent Classifier<br/>TF-IDF + Calibrated Classifier]
    Preprocessor --> EntityExtractor[4. Named Entity Extractor<br/>SKU, Warehouse, Category]
    
    MLClassifier --> Intent{Classified Intent}
    Calculator --> Metrics[Quantitative Metrics<br/>Valuation, Deficits, Warehouse Breakdown]

    Intent -->|LLM API Key Set| ExternalLLM[OpenAI / Gemini API]
    Intent -->|No API Key / Offline| LocalEngine[5. Human Synthesis & Entity Filtering Engine]
    
    Metrics --> LocalEngine
    EntityExtractor --> LocalEngine
    Metrics --> ExternalLLM

    ExternalLLM --> Response[AIAgentQueryResponse]
    LocalEngine --> Response
    
    Response --> Frontend[6. Web Chat UI: marked.js Markdown Table & Multi-Format Exporter]
```

---

## 🧠 3. Intent Classification Matrix (15 Operational Intents)

The agent classifies user queries using a **hybrid machine learning pipeline** (TF-IDF vectorizer with unigram/bigram/trigram features + Calibrated Logistic Regression) serialized via `joblib`, backed by semantic pattern fallback:

| Intent Identifier | Trigger / Semantics | Output Strategy |
| :--- | :--- | :--- |
| **`TABULAR_REPORT`** | `"table"`, `"tabular"`, `"grid"`, `"matrix"`, `"details in tabular"` | Renders complete Markdown Table containing SKU, Name, Category, Warehouse, Qty, Reorder Point, Price, Valuation, Status. |
| **`SKU_COUNT_QUANTITY`** | `"how many sku"`, `"sku count"`, `"distinct sku"`, `"number of skus"` | Direct conversational answer stating total distinct SKUs, physical unit count, portfolio valuation, category spread, and facility footprint. |
| **`STOCK_TYPE_CATEGORY`** | `"stock type"`, `"type wise"`, `"category"`, `"product type"` | Category-wise stock unit count and monetary value breakdown. |
| **`LOW_STOCK_REORDER`** | `"low stock"`, `"low in stock"`, `"reorder"`, `"shortage"`, `"restock"`, `"out of stock"`, `"depleted"` | Identifies items below threshold, calculates deficit units needed to restore 2x threshold safety stock, estimates procurement outlay. |
| **`DEMAND_FORECAST`** | `"forecast"`, `"predict demand"`, `"future sales"`, `"projected demand"` | Generates 14-day consumption projections, statistical safety stock cushion ($Z=1.65$), and dynamic Reorder Point (ROP). |
| **`SPECIFIC_SKU_LOOKUP`** | `"stock level of {sku}"`, `"check {sku}"` | Dedicated single-item operational ledger card (quantity, reorder point, unit price, holding value, deficit status). |
| **`WAREHOUSE_FILTER`** | `"items in {warehouse}"`, `"filter {warehouse}"` | Filtered warehouse inventory table and asset valuation for a specific facility. |
| **`CATEGORY_FILTER`** | `"items in {category}"`, `"filter {category}"` | Filtered product catalog table and unit volume for a specific category. |
| **`FINANCIAL_VALUATION`** | `"value"`, `"valuation"`, `"worth"`, `"cost"`, `"financial"`, `"capital"` | Total inventory holding value, top high-value stock items, and warehouse asset allocation. |
| **`WAREHOUSE_LOGISTICS`** | `"warehouse"`, `"location"`, `"distribution"`, `"storage"` | Unit and SKU count breakdown per storage facility (`Warehouse-North`, `Warehouse-South`, `Warehouse-East`, `Warehouse-West`, `Central-Hub`). |
| **`RISK_OVERSTOCK`** | `"risk"`, `"overstock"`, `"dead stock"`, `"excess"`, `"obsolete"` | Flags stockout vulnerabilities and capital lockup in overstocked items (qty > 5x threshold). |
| **`EXECUTIVE_OVERVIEW`** | `"summary"`, `"overview"`, `"executive"`, `"health"`, `"report"` | High-level executive brief with operational risk levels (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`). |
| **`CATALOG_SEARCH`** | `"search"`, department or keyword queries | Catalog scope, product references, and category allocations. |
| **`HELP_CAPABILITIES`** | `"what can you do"`, `"help"`, `"commands"` | Explains supported operations and suggested quick prompts. |
| **`GENERAL_ASSISTANT`** | Conversational greetings or open queries | Conversational summary of database health, stock counts, and suggested prompt actions. |

---

## 📦 4. Training Data Pipeline & Model Specifications

The project includes an automated synthetic data generation and ML training pipeline:

1. **`scripts/generate_training_data.py`**:
   - Generates **1,700+ balanced intent classification samples** saved to `forecasting-service/data/intent_dataset.json` and `intent_dataset.csv`.
   - Generates **350+ conversational instruction-tuning dialogues** saved to `forecasting-service/data/instruction_tuning_dataset.jsonl` in OpenAI Chat & Alpaca SFT format.
2. **`forecasting-service/train_classifier.py`**:
   - Trains `TfidfVectorizer(ngram_range=(1, 3), max_features=5000)` + `CalibratedClassifierCV(LogisticRegression(C=5.0, max_iter=1000))` pipeline.
   - Evaluates with 5-fold cross-validation.
   - Exports serialized model to `forecasting-service/app/models/intent_classifier.joblib`.

---

## 📈 5. Demand Forecasting & Inventory Science Formulas

The `/forecast` endpoint calculates:
- **Exponential Smoothing**:
  $$\hat{y}_{t+1} = \alpha y_t + (1 - \alpha) \hat{y}_t \quad (\alpha = 0.3)$$
- **Linear Trend Regression**: Blended with momentum for $N$-day horizon projections.
- **Statistical Safety Stock**:
  $$\text{Safety Stock} = Z \times \sigma_{\text{demand}} \times \sqrt{L}$$
  where $Z = 1.65$ (95% service level) and $L = \text{Lead Time (days)}$.
- **Dynamic Reorder Point (ROP)**:
  $$\text{ROP} = (\text{Daily Demand} \times L) + \text{Safety Stock}$$
- **Statistical Anomaly Detection**: Outlier detection flagging sales records where $|x - \mu| > 2\sigma$.

---

## 💻 6. Web Chat UI & Multi-Format Document Exporters

The frontend is served from `inventory-service` static resources ([`index.html`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/resources/static/index.html) and [`chat.html`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/resources/static/chat.html)).
- **`marked.js` Integration**: Formats tabular markdown, status badges (`🟢 Healthy`, `🟡 Low Stock`, `🔴 Out of Stock`), and actionable recommendations.
- **Document Exporters**:
  - **SheetJS (`xlsx.full.min.js`)**: Converts markdown tables into genuine Microsoft Excel OpenXML (`.xlsx`) workbooks.
  - **docx.js (`docx.umd.js`)**: Converts conversational responses and markdown tables into genuine Microsoft Word (`.docx`) OpenXML documents.
  - **html2pdf (`html2pdf.bundle.min.js`)**: Generates vector PDF reports.
  - **Smart Export Mode**: Auto-detects tabular structure vs narrative document and selects the optimal format automatically.
- **API Resilience**: Pydantic `@model_validator` allows full bidirectional support for camelCase (`inventoryContext`) and snake_case (`inventory_context`).

---

## 📜 7. License & Copyright

```text
Copyright (c) 2026 Vivek Umrao.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
