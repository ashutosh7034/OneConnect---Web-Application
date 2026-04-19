from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, List

import numpy as np
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity


BASE_DIR = Path(__file__).resolve().parent
KB_PATH = BASE_DIR / "knowledge_base.json"


class QueryRequest(BaseModel):
    query: str


class ServiceDoc(BaseModel):
    name: str
    url: str
    description: str
    emoji: str
    keywords: List[str]


def _load_docs() -> List[ServiceDoc]:
    data = json.loads(KB_PATH.read_text(encoding="utf-8"))
    return [ServiceDoc(**item) for item in data]


def _build_text(doc: ServiceDoc) -> str:
    return " ".join([doc.name, doc.description] + doc.keywords).lower()


SYNONYMS: Dict[str, List[str]] = {
    "hungry": ["food", "eat", "delivery", "restaurant", "lunch", "dinner"],
    "food": ["hungry", "eat", "delivery", "restaurant"],
    "eat": ["food", "hungry", "restaurant"],
    "shopping": ["buy", "fashion", "electronics", "products"],
    "buy": ["shopping", "products", "order"],
    "movie": ["movies", "series", "shows", "watch", "entertainment"],
    "music": ["songs", "playlist", "podcast", "audio"],
    "travel": ["cab", "taxi", "ride", "maps", "route", "ticket"],
    "cab": ["travel", "taxi", "ride", "commute"],
    "payment": ["pay", "upi", "bill", "recharge", "money"],
    "pay": ["payment", "upi", "bill", "money"],
    "health": ["doctor", "medicine", "medical", "clinic"],
    "doctor": ["health", "appointment", "medical"],
}


def _expand_query(query: str) -> str:
    tokens = query.lower().split()
    expanded = tokens[:]
    for token in tokens:
        if token in SYNONYMS:
            expanded.extend(SYNONYMS[token])
    return " ".join(expanded)


def _reason(query: str, doc: ServiceDoc) -> str:
    q = set(query.lower().split())
    keys = set(k.lower() for k in doc.keywords)
    overlap = [k for k in keys if k in q]
    if overlap:
        return f"Matches your need: {', '.join(overlap[:2])}"
    return "Recommended by AI guide"


DOCS = _load_docs()
DOC_TEXTS = [_build_text(doc) for doc in DOCS]
VECTORIZER = TfidfVectorizer(ngram_range=(1, 2), stop_words="english")
DOC_MATRIX = VECTORIZER.fit_transform(DOC_TEXTS)


app = FastAPI(title="OneConnect Python RAG")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok"}


@app.post("/recommend")
def recommend(req: QueryRequest) -> Dict:
    query = (req.query or "").strip()
    if not query:
        return {
            "success": False,
            "reply": "Tell me what you need, for example: I am hungry or I need a cab.",
            "suggestions": [],
        }

    expanded = _expand_query(query)
    q_vec = VECTORIZER.transform([expanded])
    sims = cosine_similarity(q_vec, DOC_MATRIX).flatten()

    top_idx = np.argsort(sims)[::-1][:5]

    suggestions = []
    for idx in top_idx:
        score = float(sims[idx])
        if score < 0.03:
            continue
        doc = DOCS[idx]
        suggestions.append(
            {
                "name": doc.name,
                "url": doc.url,
                "description": doc.description,
                "emoji": doc.emoji,
                "reason": _reason(query, doc),
                "score": round(score, 4),
            }
        )

    if not suggestions:
        # fallback: top popular entries
        for doc in DOCS[:5]:
            suggestions.append(
                {
                    "name": doc.name,
                    "url": doc.url,
                    "description": doc.description,
                    "emoji": doc.emoji,
                    "reason": "Popular option",
                    "score": 0.01,
                }
            )

    top_names = [item["name"] for item in suggestions[:3]]
    if len(top_names) == 1:
        list_text = top_names[0]
    elif len(top_names) == 2:
        list_text = f"{top_names[0]} and {top_names[1]}"
    else:
        list_text = f"{top_names[0]}, {top_names[1]} and {top_names[2]}"

    return {
        "success": True,
        "reply": f"Based on your query, try {list_text}.",
        "suggestions": suggestions,
    }
