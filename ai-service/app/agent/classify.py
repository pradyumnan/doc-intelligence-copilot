import os
from langchain_groq import ChatGroq
from dotenv import load_dotenv

load_dotenv()

llm = ChatGroq(
    model="openai/gpt-oss-120b",
    api_key=os.getenv("GROQ_API_KEY"),
    temperature=0
)

CATEGORIES = ["invoice", "loan_application", "kyc", "contract", "other"]

def classify_document(text: str) -> str:
    """Classify document text into one of our policy categories."""
    prompt = f"""Classify the following document into EXACTLY ONE of these categories:
invoice, loan_application, kyc, contract, other

Respond with ONLY the category word, nothing else.

Document text:
{text[:1500]}

Category:"""

    response = llm.invoke(prompt)
    result = response.content.strip().lower()

    # safety check — ensure it's a valid category, default to "other" if not
    for category in CATEGORIES:
        if category in result:
            return category
    return "other"

if __name__ == "__main__":
    import json
    with open("../data/synthetic/ocr_results.json", "r", encoding="utf-8") as f:
        ocr_data = json.load(f)

    # test on first 5 documents
    for doc in ocr_data[:5]:
        category = classify_document(doc["extracted_text"])
        print(f"{doc['filename']}: {category}")