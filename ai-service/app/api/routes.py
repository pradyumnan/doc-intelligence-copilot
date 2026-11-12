import sys
import os
import json
import tempfile
from typing import Optional
from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel
from PIL import Image, UnidentifiedImageError

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
APP_DIR = os.path.join(SCRIPT_DIR, "..")
AGENT_DIR = os.path.join(SCRIPT_DIR, "..", "agent")
INGESTION_DIR = os.path.join(SCRIPT_DIR, "..", "ingestion")
sys.path.append(APP_DIR)
sys.path.append(AGENT_DIR)
sys.path.append(INGESTION_DIR)

from logging_config import logger
from graph import build_graph
from ocr import extract_text
from extract_fields import extract_fields

router = APIRouter()
agent_app = build_graph()  # built once at startup, reused for every request

MAX_FILE_SIZE_MB = 10
ALLOWED_CONTENT_TYPES = {"image/png", "image/jpeg"}
MAX_TEXT_CHARS = 20000  # how much OCR text we hand back to be stored with the case


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
    policy_source: Optional[str] = None
    verified: Optional[bool] = None
    extracted_text: Optional[str] = None
    fields_json: Optional[str] = None  # dates, amounts, people and organisations, as a JSON string


def run_agent(document_text: str, filename: str) -> DecisionResponse:
    initial_state = {
        "document_text": document_text,
        "filename": filename,
        "category": None, "policy_source": None, "policy_text": None,
        "route": None, "justification": None, "confidence": None,
        "verified": None, "verification_note": None, "final_status": None,
    }
    result = agent_app.invoke(initial_state)

    try:
        fields_json = json.dumps(extract_fields(document_text))
    except Exception as e:
        logger.warning(f"Field extraction failed for {filename}: {e}")
        fields_json = None

    return DecisionResponse(
        filename=filename,
        category=result["category"],
        route=result["route"],
        justification=result["justification"],
        confidence=result["confidence"],
        final_status=result["final_status"],
        policy_source=result.get("policy_source"),
        verified=result.get("verified"),
        extracted_text=document_text[:MAX_TEXT_CHARS],
        fields_json=fields_json,
    )


@router.post("/agent/decide", response_model=DecisionResponse)
def decide(request: DecisionRequest):
    return run_agent(request.document_text, request.filename)


@router.post("/agent/decide-from-image", response_model=DecisionResponse)
async def decide_from_image(file: UploadFile = File(...)):
    # Layer 1: declared content type (client-supplied, so only a first filter)
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(status_code=400, detail=f"Unsupported file type: {file.content_type}")

    # Layer 2: size limit
    content = await file.read()
    size_mb = len(content) / (1024 * 1024)
    if size_mb > MAX_FILE_SIZE_MB:
        raise HTTPException(status_code=400, detail=f"File too large: {size_mb:.1f}MB (max {MAX_FILE_SIZE_MB}MB)")

    with tempfile.NamedTemporaryFile(delete=False, suffix=".png") as tmp:
        tmp.write(content)
        tmp_path = tmp.name

    try:
        # Layer 3: inspect the real file contents, not the header
        try:
            with Image.open(tmp_path) as img:
                img.verify()
        except (UnidentifiedImageError, OSError):
            raise HTTPException(status_code=400, detail="File is not a valid image")

        try:
            document_text = extract_text(tmp_path)
        except Exception as e:
            logger.warning(f"OCR failed for {file.filename}: {e}")
            document_text = ""  # empty text flows through as "other" -> human review
    finally:
        os.unlink(tmp_path)

    return run_agent(document_text, file.filename)
