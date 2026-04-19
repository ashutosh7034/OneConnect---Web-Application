# OneConnect Python RAG Service

This service provides a retrieval-based AI guide for OneConnect.

## What It Does

- Accepts user intent like: "I am hungry"
- Retrieves top matching apps from a knowledge base using TF-IDF + cosine similarity
- Returns grounded suggestions and reasons

## Run Locally

1. Open a terminal in `ai_rag_service`.
2. Install dependencies:

```bash
pip install -r requirements.txt
```

3. Start the API:

```bash
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

4. Health check:

```bash
curl http://localhost:8000/health
```

5. Recommend test:

```bash
curl -X POST http://localhost:8000/recommend -H "Content-Type: application/json" -d "{\"query\":\"I am hungry\"}"
```

## Integration

The OneConnect frontend `src/main/webapp/js/ai-guide.js` calls this Python endpoint first:

- `http://localhost:8000/recommend`

If Python service is not running, it falls back to Java endpoint:

- `/api/ai-guide`

## Add More Applications

Edit `knowledge_base.json` and add entries with:

- `name`
- `url`
- `description`
- `emoji`
- `keywords` (important for retrieval quality)
