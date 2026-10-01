from fastapi import FastAPI

app = FastAPI(title="Doc Intelligence Copilot - AI Service")

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "ai-service"}