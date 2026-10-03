import sys
import os
from fastapi import APIRouter, UploadFile, File
from pydantic import BaseModel
import tempfile

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
AGENT_DIR = os.path.join(SCRIPT_DIR, "..", "agent")
INGESTION_DIR = os.path.join(SCRIPT_DIR, "..", "ingestion")
sys.path.append(AGENT_DIR)
sys.path.append(INGESTION_DIR)

from graph import build_graph
from ocr import extract_text

router = APIRouter()
agent_app = build_graph()

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

def run_agent(document_text: str, filename: str) -> DecisionResponse:
    initial_state = {
        "document_text": document_text,
        "filename": filename,
        "category": None, "policy_source": None, "policy_text": None,
        "route": None, "justification": None, "confidence": None,
        "final_status": None
    }
    result = agent_app.invoke(initial_state)
    return DecisionResponse(
        filename=filename,
        category=result["category"],
        route=result["route"],
        justification=result["justification"],
        confidence=result["confidence"],
        final_status=result["final_status"]
    )

@router.post("/agent/decide", response_model=DecisionResponse)
def decide(request: DecisionRequest):
    return run_agent(request.document_text, request.filename)

@router.post("/agent/decide-from-image", response_model=DecisionResponse)
async def decide_from_image(file: UploadFile = File(...)):
    # Save uploaded file temporarily, run OCR, then run the agent
    with tempfile.NamedTemporaryFile(delete=False, suffix=".png") as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    document_text = extract_text(tmp_path)
    os.unlink(tmp_path)  # clean up temp file

    return run_agent(document_text, file.filename)