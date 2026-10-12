#!/usr/bin/env python3
"""
Smart Inventory Management System - ML Intent Classifier Training Pipeline
Trains a high-performance TF-IDF + Calibrated Classifier pipeline using scikit-learn.
Evaluates accuracy, precision, recall, and F1-score across all 16 inventory intents.
Serializes the trained model to `app/models/intent_classifier.joblib` for runtime inference.
"""

import os
import json
import logging
import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.calibration import CalibratedClassifierCV
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split, cross_val_score, StratifiedKFold
from sklearn.metrics import classification_report, accuracy_score

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("train_classifier")

DATASET_PATH = os.path.join(os.path.dirname(__file__), "data", "intent_dataset.json")
MODEL_DIR = os.path.join(os.path.dirname(__file__), "app", "models")
MODEL_PATH = os.path.join(MODEL_DIR, "intent_classifier.joblib")

def train_intent_model():
    if not os.path.exists(DATASET_PATH):
        logger.error("Dataset not found at %s. Please run scripts/generate_training_data.py first.", DATASET_PATH)
        return

    logger.info("Loading training data from %s...", DATASET_PATH)
    with open(DATASET_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    queries = [item["query"] for item in data]
    labels = [item["intent"] for item in data]
    unique_intents = sorted(list(set(labels)))
    logger.info("Loaded %d training samples across %d distinct intents: %s", len(queries), len(unique_intents), unique_intents)

    # Train-test split
    X_train, X_test, y_train, y_test = train_test_split(
        queries, labels, test_size=0.2, random_state=42, stratify=labels
    )

    # Build Pipeline: TF-IDF n-grams + Calibrated Logistic Regression
    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(
            ngram_range=(1, 3),
            sublinear_tf=True,
            strip_accents="unicode",
            lowercase=True,
            min_df=1
        )),
        ("classifier", CalibratedClassifierCV(
            estimator=LogisticRegression(C=5.0, max_iter=1000, random_state=42),
            cv=3
        ))
    ])

    logger.info("Training intent classifier pipeline...")
    pipeline.fit(X_train, y_train)

    # Evaluate on Test Set
    y_pred = pipeline.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    logger.info("Test Set Accuracy: %.4f (%.2f%%)", acc, acc * 100)

    # Stratified K-Fold Cross Validation
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    cv_scores = cross_val_score(pipeline, queries, labels, cv=cv, scoring="accuracy")
    logger.info("5-Fold Cross Validation Mean Accuracy: %.4f (+/- %.4f)", cv_scores.mean(), cv_scores.std() * 2)

    report = classification_report(y_test, y_pred, zero_division=0)
    print("\n" + "=" * 60)
    print("CLASSIFICATION REPORT")
    print("=" * 60)
    print(report)

    # Serialize Model
    os.makedirs(MODEL_DIR, exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)
    logger.info("Successfully serialized trained model to %s", MODEL_PATH)

    # Sanity checks with key inventory queries
    test_queries = [
        "which item has highest value",
        "which item has the highest value",
        "which product is the most expensive",
        "which item has lowest stock",
        "can you show top 10 inventory data sorted by quantity",
        "Which items are low in stock and need reordering?",
        "which items are low in stock?",
        "can you share this data in more details in tabular form?",
        "how many different skus stock has?",
        "give me total stock count stock type wise",
        "What is our total inventory valuation and warehouse breakdown?",
        "forecast demand for LAPTOP-PRO-15 over the next 14 days",
        "what is the stock level of SMARTPHONE-X?",
        "show me all items in Warehouse-North",
        "filter products by Electronics category",
        "what can you do?"
    ]

    print("\n" + "=" * 60)
    print("INFERENCE SANITY CHECKS")
    print("=" * 60)
    for q in test_queries:
        probs = pipeline.predict_proba([q])[0]
        classes = pipeline.classes_
        top_idx = probs.argmax()
        top_class = classes[top_idx]
        confidence = probs[top_idx]
        print(f"Query: '{q}'\n  -> Predicted: {top_class} (Confidence: {confidence:.2%})\n")

if __name__ == "__main__":
    train_intent_model()
