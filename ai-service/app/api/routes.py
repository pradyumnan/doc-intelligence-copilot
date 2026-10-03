import sys
import os
from fastapi import APIRouter
from pydantic import BaseModel

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
AGENT_DIR = os.path.join(SCRIPT_DIR, "..", "agent")
sys.path.append(AGENT_DIR)

from graph import build_graph

router = APIRouter()
agent_app = build_graph()  # built once at startup, reused for every request

class DecisionRequest(BaseModel):
    document_text: str
    filename: str

class DecisionResponse(BaseModel):
    filename: str
    category: str
    route: str
    justification: str
    confidence: float
    final_status: str

@router.post("/agent/decide", response_model=DecisionResponse)
def decide(request: DecisionRequest):
    initial_state = {
        "document_text": request.document_text,
        "filename": request.filename,
        "category": None, "policy_source": None, "policy_text": None,
        "route": None, "justification": None, "confidence": None,
        "final_status": None
    }
    result = agent_app.invoke(initial_state)

    return DecisionResponse(
        filename=request.filename,
        category=result["category"],
        route=result["route"],
        justification=result["justification"],
        confidence=result["confidence"],
        final_status=result["final_status"]
    )