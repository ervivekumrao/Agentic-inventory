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
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, model_validator
import numpy as np
import pandas as pd
from typing import List, Optional, Dict, Any
from app.agent import InventoryAIAgent

# Configure structured logging for Python FastAPI
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger("app.main")

app = FastAPI(
    title="Smart Inventory AI & Forecasting Service",
    description="Provides AI-driven demand forecasting, stock anomaly detection, and human-like RAG/LLM inventory assistant insights.",
    version="1.0.0"
)

# Enable CORS for web dashboard access securely without wildcard credentials (CWE-942)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

agent = InventoryAIAgent()

class LoggerLevelRequest(BaseModel):
    level: str

class ForecastRequest(BaseModel):
    sku: str
    historical_sales: List[float]
    horizon: int = 14
    lead_time_days: int = 5

class ForecastResponse(BaseModel):
    sku: str
    horizon: int
    predictions: List[float]
    confidence_score: float
    trend: str
    safety_stock_recommendation: Optional[float] = None
    reorder_point_recommendation: Optional[float] = None
    anomalies: Optional[List[int]] = None
    confidence_interval_lower: Optional[List[float]] = None
    confidence_interval_upper: Optional[List[float]] = None

class AIAgentQueryRequest(BaseModel):
    query: str
    inventory_context: Optional[List[Dict[str, Any]]] = None
    catalog_context: Optional[List[Dict[str, Any]]] = None
    system_instruction: Optional[str] = None
    conversation_history: Optional[List[Dict[str, str]]] = None

    @model_validator(mode="before")
    @classmethod
    def handle_camel_case(cls, values: Any) -> Any:
        if isinstance(values, dict):
            if "inventoryContext" in values and "inventory_context" not in values:
                values["inventory_context"] = values["inventoryContext"]
            if "catalogContext" in values and "catalog_context" not in values:
                values["catalog_context"] = values["catalogContext"]
            if "systemInstruction" in values and "system_instruction" not in values:
                values["system_instruction"] = values["systemInstruction"]
            if "conversationHistory" in values and "conversation_history" not in values:
                values["conversation_history"] = values["conversationHistory"]
            if "inventory_context" not in values or values["inventory_context"] is None:
                values["inventory_context"] = []
        return values

class AIAgentQueryResponse(BaseModel):
    answer: str
    recommendations: List[str]
    risk_level: str
    riskLevel: Optional[str] = None

# --- REST Logging Management Endpoints ---

@app.get("/api/admin/loggers")
def get_loggers():
    """
    Returns a dictionary of all registered Python loggers and their effective / configured log levels.
    """
    logger.debug("GET /api/admin/loggers requested")
    loggers_info = {}
    root_logger = logging.getLogger()
    loggers_info["root"] = {
        "effectiveLevel": logging.getLevelName(root_logger.getEffectiveLevel()),
        "configuredLevel": logging.getLevelName(root_logger.level) if root_logger.level != logging.NOTSET else "NOTSET"
    }

    # Pre-register app loggers if not yet accessed
    for app_name in ["app.main", "app.agent", "uvicorn", "uvicorn.access", "fastapi"]:
        logging.getLogger(app_name)

    for name in sorted(logging.Logger.manager.loggerDict.keys()):
        log_obj = logging.getLogger(name)
        loggers_info[name] = {
            "effectiveLevel": logging.getLevelName(log_obj.getEffectiveLevel()),
            "configuredLevel": logging.getLevelName(log_obj.level) if log_obj.level != logging.NOTSET else "NOTSET"
        }
    return {"loggers": loggers_info}

@app.get("/api/admin/loggers/{logger_name}")
def get_logger_level(logger_name: str):
    """
    Retrieves effective and configured log level for a specific logger.
    """
    logger.debug("GET /api/admin/loggers/%s requested", logger_name)
    log_obj = logging.getLogger(logger_name if logger_name != "root" else "")
    return {
        "name": logger_name,
        "effectiveLevel": logging.getLevelName(log_obj.getEffectiveLevel()),
        "configuredLevel": logging.getLevelName(log_obj.level) if log_obj.level != logging.NOTSET else "NOTSET"
    }

@app.post("/api/admin/loggers/{logger_name}")
def set_logger_level(logger_name: str, req: LoggerLevelRequest):
    """
    Dynamically updates the log level of a target logger at runtime without restarting the service.
    """
    target_logger = logging.getLogger(logger_name if logger_name != "root" else "")
    level_str = req.level.upper()
    valid_levels = {
        "TRACE": logging.DEBUG,
        "DEBUG": logging.DEBUG,
        "INFO": logging.INFO,
        "WARN": logging.WARNING,
        "WARNING": logging.WARNING,
        "ERROR": logging.ERROR,
        "CRITICAL": logging.CRITICAL,
        "OFF": logging.CRITICAL + 10,
        "NOTSET": logging.NOTSET
    }
    if level_str not in valid_levels:
        raise HTTPException(status_code=400, detail=f"Invalid log level '{req.level}'. Valid levels: {list(valid_levels.keys())}")

    target_logger.setLevel(valid_levels[level_str])
    logger.info("Dynamically updated logger '%s' level to %s", logger_name, level_str)
    return {
        "name": logger_name,
        "configuredLevel": level_str,
        "effectiveLevel": logging.getLevelName(target_logger.getEffectiveLevel())
    }

# --- Service Functional Endpoints ---

@app.post("/forecast", response_model=ForecastResponse)
def forecast_demand(req: ForecastRequest):
    """
    Performs machine learning demand forecasting, trend modeling,
    statistical anomaly detection, and safety stock / reorder point calculations.
    """
    logger.info("Demand forecast requested for SKU: %s over horizon: %d days", req.sku, req.horizon)
    if not req.historical_sales:
        logger.warning("No historical sales provided for SKU: %s, using fallback baseline prediction", req.sku)
        predictions = [10.0] * req.horizon
        return ForecastResponse(
            sku=req.sku,
            horizon=req.horizon,
            predictions=predictions,
            confidence_score=0.75,
            trend="STABLE",
            safety_stock_recommendation=15.0,
            reorder_point_recommendation=65.0,
            anomalies=[]
        )

    series = pd.Series(req.historical_sales, dtype=float)
    n = len(series)

    # 1. Statistical Baseline & Linear Trend Regression
    x = np.arange(n)
    y = series.values
    if n >= 2:
        slope, intercept = np.polyfit(x, y, 1)
    else:
        slope, intercept = 0.0, float(y[0])

    std_dev = float(series.std()) if n > 1 else 0.0
    mean_val = float(series.mean())

    # 2. Anomaly Detection (Z-score > 2.0)
    anomalies = []
    if std_dev > 0:
        z_scores = np.abs((y - mean_val) / std_dev)
        anomalies = [int(idx) for idx, z in enumerate(z_scores) if z > 2.0]

    # 3. Dynamic Future Horizon Predictions & Intervals
    predictions = []
    lower_bound = []
    upper_bound = []

    last_val = series.iloc[-1]
    for step in range(1, req.horizon + 1):
        # Blend autoregressive momentum with linear trend projection
        pred = max(0.0, float(last_val + (slope * step * 0.5)))
        margin = max(1.0, 1.96 * (std_dev if std_dev > 0 else 1.0))
        predictions.append(round(pred, 2))
        lower_bound.append(round(max(0.0, pred - margin), 2))
        upper_bound.append(round(pred + margin, 2))

    trend_str = "INCREASING" if slope > 0.2 else ("DECREASING" if slope < -0.2 else "STABLE")

    # 4. Inventory Supply Chain Safety Stock & Reorder Point (ROP) Formulas
    # Safety Stock = Z * sigma * sqrt(lead_time), where Z = 1.65 (95% service level)
    lead_time = req.lead_time_days
    safety_stock = round(1.65 * (std_dev if std_dev > 0 else 1.5) * np.sqrt(lead_time), 1)
    daily_demand = max(1.0, mean_val)
    reorder_point = round((daily_demand * lead_time) + safety_stock, 1)

    confidence = round(min(0.95, max(0.70, 0.90 - (std_dev / (mean_val + 1e-5) * 0.1))), 2)

    logger.debug("Forecast calculated for SKU %s: trend=%s, ROP=%.1f, horizon=%d", req.sku, trend_str, reorder_point, req.horizon)

    return ForecastResponse(
        sku=req.sku,
        horizon=req.horizon,
        predictions=predictions,
        confidence_score=confidence,
        trend=trend_str,
        safety_stock_recommendation=safety_stock,
        reorder_point_recommendation=reorder_point,
        anomalies=anomalies,
        confidence_interval_lower=lower_bound,
        confidence_interval_upper=upper_bound
    )

@app.post("/api/ai/inventory-assistant", response_model=AIAgentQueryResponse)
def ai_inventory_assistant(req: AIAgentQueryRequest):
    """
    RAG / Agentic AI assistant that analyzes inventory and catalog context
    to answer operational questions, handle complex requests, and generate natural, human-like replies.
    """
    logger.info("AI inventory assistant query received: '%s'", req.query)
    res = agent.process_query(
        query=req.query,
        inventory_context=req.inventory_context or [],
        catalog_context=req.catalog_context,
        system_instruction=req.system_instruction
    )
    logger.info("AI assistant query processed. Risk level: %s", res["risk_level"])
    return AIAgentQueryResponse(
        answer=res["answer"],
        recommendations=res["recommendations"],
        risk_level=res["risk_level"],
        riskLevel=res["risk_level"]
    )

@app.post("/api/ai/agent/chat", response_model=AIAgentQueryResponse)
def ai_agent_chat(req: AIAgentQueryRequest):
    """
    Interactive chat interface for the AI Agent.
    """
    logger.debug("Routing chat request to ai_inventory_assistant")
    return ai_inventory_assistant(req)

@app.get("/health")
def health_check():
    logger.debug("Health check requested")
    return {"status": "healthy", "service": "forecasting-ai-service"}
