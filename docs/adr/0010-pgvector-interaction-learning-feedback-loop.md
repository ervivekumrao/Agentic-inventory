# ADR 0010: pgvector Interaction Learning Feedback Loop & Persistent Active Learning Engine

## Status
Approved

## Context
Standard NLP intent classifiers use static training datasets. When users enter novel, ambiguous, or mistyped queries (e.g., "lowes per value item") and receive an undesired answer, changing business source code for every phrasing variation is brittle and anti-pattern. The AI Assistant requires a closed-loop active learning architecture to capture user corrections in real time, store dense semantic vector embeddings, and persist learning across container restarts without data loss.

## Decision
We implement a **pgvector Active Learning Feedback Loop**:

1. **PostgreSQL Feedback Persistence (`user_feedback_logs`)**: Persist raw user queries, user corrections, ratings (`THUMBS_UP` / `THUMBS_DOWN`), and target intent mappings in PostgreSQL table `user_feedback_logs`.
2. **Dense Vector Embeddings & Similarity Matching (`pgvector`)**: Store $384\text{-D}$ semantic vector embeddings (`query_embeddings`). Incoming queries perform cosine similarity lookup (`1 - (embedding <=> query_vector)`). If similarity exceeds $0.85$ to a previously corrected query, the system automatically overrides the classifier prediction with the user's ground-truth correction.
3. **Container Restart Learning Survival**: Database rows and vector tables are saved to mounted host volumes (`postgres_data`). Serialized model artifacts (`intent_classifier.joblib`) are updated on disk. Container restarts or reboot events preserve 100% of learned feedback.
4. **Interactive UI Affordances**: Add "👍 Helpful" and "👎 Correct" buttons to Chat UI answer cards ([`chat.html`](file:///H:/GitHubProjects/AgenticInventory/inventory-service/src/main/resources/static/chat.html)) allowing operators to submit corrections on-the-fly via `/api/ai/feedback`.

## Consequences
### Positive
- **Self-Improving AI Engine**: Learns automatically from operator corrections without modifying application source code.
- **Zero Learning Loss**: All feedback and vector embeddings persist across container restarts.
- **Sub-Millisecond Vector Lookup**: Fast HNSW cosine similarity search handles semantic matches instantly.

### Negative
- **Validation Guard**: User feedback requires basic validation to prevent malicious operators from submitting intentionally incorrect training labels.
