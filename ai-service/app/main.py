from fastapi import FastAPI
from app.api.routes import router as agent_router

app = FastAPI(title="Doc Intelligence Copilot - AI Service")

app.include_router(agent_router)

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "ai-service"}