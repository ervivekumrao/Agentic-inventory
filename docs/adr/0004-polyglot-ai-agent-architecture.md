# ADR 0004: Polyglot Dual-Mode AI Agent (Local Scikit-Learn ML + Cloud LLMs)

## Status
Approved

## Context
The platform requires an intelligent inventory assistant capable of understanding natural language queries (e.g., "Which items are low in stock?", "Show top 5 expensive products", "Export tabular report"). Relying solely on external cloud LLM APIs (Google Gemini or OpenAI) introduces operational latency, cloud API cost overhead, and failures when API keys are absent or internet connectivity is lost.

## Decision
We implemented a **Dual-Mode AI Architecture** in `forecasting-service` (`app/agent.py`):

1. **Trained Local ML Intent Classifier**: Scikit-Learn TF-IDF vectorizer + Calibrated Logistic Regression model (`intent_classifier.joblib`) trained on 1,700+ balanced queries covering 15 operational intents.
2. **Local Deep Human Synthesis Engine**: If cloud API keys are absent, the local Python agent performs Named Entity Recognition (NER), slot extraction, inventory metrics analysis, statistical safety stock calculations ($Z=1.65$), and renders styled Markdown tables and operational recommendations.
3. **Cloud LLM Delegation**: When `OPENAI_API_KEY` or `GEMINI_API_KEY` environment variables are present, the agent automatically routes complex conversational prompts to external cloud LLMs with strict system instruction sanitization.
4. **Spring Boot Orchestration**: `inventory-service` hydrates inventory context and forwards requests via REST to `forecasting-service`, handling fallback gracefully if downstream calls fail.

## Consequences
### Positive
- **100% Offline Capability**: AI chat workspace works fully without external cloud dependencies or API keys.
- **Sub-Second Latency**: Local intent classification and synthesis respond in under 50ms.
- **Privacy Preservation**: Operational inventory data stays local when running offline.

### Negative
- **Model Training Maintenance**: Retraining `intent_classifier.joblib` is required when adding new operational intent categories.
