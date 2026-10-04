import os
import sys
import json
from typing import TypedDict, Optional
from langgraph.graph import StateGraph, END
from langchain_groq import ChatGroq
from dotenv import load_dotenv

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RETRIEVAL_DIR = os.path.join(SCRIPT_DIR, "..", "retrieval")
sys.path.append(RETRIEVAL_DIR)

from search import search_policies

load_dotenv()

llm = ChatGroq(
    model=os.getenv("GROQ_MODEL", "openai/gpt-oss-120b"),
    api_key=os.getenv("GROQ_API_KEY"),
    temperature=0
)

CATEGORIES = ["invoice", "loan_application", "kyc", "contract", "other"]
CONFIDENCE_THRESHOLD = float(os.getenv("CONFIDENCE_THRESHOLD", "0.75"))  # below this → human review

# --- State definition: this is what flows through every node ---
class AgentState(TypedDict):
    document_text: str
    filename: str
    category: Optional[str]
    policy_source: Optional[str]
    policy_text: Optional[str]
    route: Optional[str]
    justification: Optional[str]
    confidence: Optional[float]
    final_status: Optional[str]  # "auto_routed" or "needs_human_review"


# --- Node 1: Classify ---
def classify_node(state: AgentState) -> AgentState:
    prompt = f"""Classify the following document into EXACTLY ONE of these categories:
invoice, loan_application, kyc, contract, other

Respond with ONLY the category word, nothing else.

Document text:
{state['document_text'][:1500]}

Category:"""
    response = llm.invoke(prompt)
    result = response.content.strip().lower()

    category = "other"
    for c in CATEGORIES:
        if c in result:
            category = c
            break

    return {**state, "category": category}


# --- Node 2: Retrieve relevant policy ---
def retrieve_node(state: AgentState) -> AgentState:
    if state["category"] == "other":
        return {**state, "policy_source": None, "policy_text": None}

    query = f"policy rules for {state['category']} documents"
    results = search_policies(query, top_k=1)
    source, text, _ = results[0]
    return {**state, "policy_source": source, "policy_text": text}


# --- Node 3: Decide route + justification ---
def decide_node(state: AgentState) -> AgentState:
    if state["category"] == "other":
        return {
            **state,
            "route": "General Review Queue",
            "justification": "Document does not match any specific policy category.",
            "confidence": 0.5
        }

    prompt = f"""You are a document routing assistant. Given the document and policy, decide the route.

Respond with ONLY valid JSON:
{{"route": "<queue name>", "justification": "<1-2 sentences citing the specific policy rule>", "confidence": <float 0.0-1.0>}}

Document category: {state['category']}
Document text: {state['document_text'][:1000]}
Relevant policy: {state['policy_text']}

JSON response:"""

    try:
        response = llm.invoke(prompt)
        decision = json.loads(response.content.strip())

        # validate the response has what we need, with sane fallbacks
        if not all(k in decision for k in ["route", "justification", "confidence"]):
            raise ValueError("LLM response missing required fields")

        decision["confidence"] = max(0.0, min(1.0, float(decision["confidence"])))  # clamp to valid range

    except json.JSONDecodeError:
        decision = {
            "route": "Manual Review Queue",
            "justification": "AI could not produce a structured decision; flagged for manual review.",
            "confidence": 0.0
        }
    except Exception as e:
        decision = {
            "route": "Manual Review Queue",
            "justification": f"Decision process encountered an error: {str(e)[:100]}. Flagged for manual review.",
            "confidence": 0.0
        }

    return {**state, **decision}


# --- Node 4: Confidence gate (this is the branching logic) ---
def confidence_gate_node(state: AgentState) -> AgentState:
    if state["confidence"] >= CONFIDENCE_THRESHOLD:
        return {**state, "final_status": "auto_routed"}
    else:
        return {**state, "final_status": "needs_human_review"}


# --- Build the graph ---
def build_graph():
    graph = StateGraph(AgentState)

    graph.add_node("classify", classify_node)
    graph.add_node("retrieve", retrieve_node)
    graph.add_node("decide", decide_node)
    graph.add_node("confidence_gate", confidence_gate_node)

    graph.set_entry_point("classify")
    graph.add_edge("classify", "retrieve")
    graph.add_edge("retrieve", "decide")
    graph.add_edge("decide", "confidence_gate")
    graph.add_edge("confidence_gate", END)

    return graph.compile()


if __name__ == "__main__":
    app = build_graph()

    with open("../data/synthetic/ocr_results.json", "r", encoding="utf-8") as f:
        ocr_data = json.load(f)

    for doc in ocr_data[:3]:
        print(f"\n=== {doc['filename']} ===")
        initial_state = {
            "document_text": doc["extracted_text"],
            "filename": doc["filename"],
            "category": None, "policy_source": None, "policy_text": None,
            "route": None, "justification": None, "confidence": None,
            "final_status": None
        }
        result = app.invoke(initial_state)
        print(json.dumps({
            "category": result["category"],
            "route": result["route"],
            "justification": result["justification"],
            "confidence": result["confidence"],
            "final_status": result["final_status"]
        }, indent=2))