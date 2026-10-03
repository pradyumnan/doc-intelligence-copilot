import os
import sys
import json
from langchain_groq import ChatGroq
from dotenv import load_dotenv

from classify import classify_document, CATEGORIES

# Build an absolute path to the retrieval folder, relative to this file
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RETRIEVAL_DIR = os.path.join(SCRIPT_DIR, "..", "retrieval")
sys.path.append(RETRIEVAL_DIR)

from search import search_policies

load_dotenv()

llm = ChatGroq(
    model="openai/gpt-oss-120b",
    api_key=os.getenv("GROQ_API_KEY"),
    temperature=0
)

DECISION_PROMPT = """You are a document routing assistant for an enterprise document management system.

Given a document's content and the relevant policy, decide:
1. Which workflow queue this document should be routed to
2. A clear justification citing the SPECIFIC policy rule that applies
3. A confidence score from 0.0 to 1.0 for your decision

Respond with ONLY valid JSON in this exact format, nothing else:
{{
  "route": "<queue name>",
  "justification": "<1-2 sentence explanation citing the specific policy rule>",
  "confidence": <float between 0.0 and 1.0>
}}

Document category: {category}

Document text (excerpt):
{document_text}

Relevant policy:
{policy_text}

JSON response:"""

def decide_route(document_text: str) -> dict:
    # Step 1: classify
    category = classify_document(document_text)

    if category == "other":
        return {
            "category": category,
            "route": "General Review Queue",
            "justification": "Document does not match any specific policy category (invoice, loan, KYC, contract).",
            "confidence": 0.5,
            "policy_source": None
        }

    # Step 2: retrieve relevant policy
    query = f"policy rules for {category} documents"
    results = search_policies(query, top_k=1)
    policy_source, policy_text, distance = results[0]

    # Step 3: reason + decide
    prompt = DECISION_PROMPT.format(
        category=category,
        document_text=document_text[:1000],
        policy_text=policy_text
    )
    response = llm.invoke(prompt)

    try:
        decision = json.loads(response.content.strip())
    except json.JSONDecodeError:
        # fallback if LLM doesn't return clean JSON
        decision = {
            "route": "Manual Review Queue",
            "justification": "Could not parse structured decision; flagged for human review.",
            "confidence": 0.0
        }

    decision["category"] = category
    decision["policy_source"] = policy_source
    return decision

if __name__ == "__main__":
    with open("../data/synthetic/ocr_results.json", "r", encoding="utf-8") as f:
        ocr_data = json.load(f)

    # test on a few documents
    for doc in ocr_data[:3]:
        print(f"\n=== {doc['filename']} ===")
        result = decide_route(doc["extracted_text"])
        print(json.dumps(result, indent=2))