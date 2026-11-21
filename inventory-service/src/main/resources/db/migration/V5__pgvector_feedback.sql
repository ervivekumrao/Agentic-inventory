-- V5 Migration: pgvector Feedback Table Schema & Indexes
CREATE TABLE IF NOT EXISTS user_feedback_vectors (
    id BIGSERIAL PRIMARY KEY,
    original_query TEXT NOT NULL,
    user_correction TEXT,
    corrected_intent VARCHAR(100),
    sort_field VARCHAR(64),
    sort_order VARCHAR(32),
    rating VARCHAR(32) DEFAULT 'THUMBS_DOWN',
    embedding TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedback_intent ON user_feedback_vectors(corrected_intent);
