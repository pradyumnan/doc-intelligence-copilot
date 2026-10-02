import os
import json
import random
from datetime import datetime, timedelta

OUTPUT_DIR = "../data/policy-corpus"  # keeping case metadata alongside policy for now
DOC_SAMPLE_DIR = "../data/rvl-cdip-sample"

random.seed(42)  # reproducible output

DOC_TYPES = {
    0: "letter", 1: "form", 2: "email", 3: "handwritten",
    4: "advertisement", 5: "scientific_report", 6: "scientific_publication",
    7: "specification", 8: "file_folder", 9: "news_article",
    10: "budget", 11: "invoice", 12: "presentation",
    13: "questionnaire", 14: "resume", 15: "memo"
}

SUBMITTERS = ["Priya Sharma", "Arjun Mehta", "Fatima Khan", "Rohan Gupta", "Ananya Iyer", "Vikram Nair"]
DEPARTMENTS = ["Finance", "Legal", "Operations", "Compliance", "HR"]

def random_date(days_back=90):
    return (datetime.now() - timedelta(days=random.randint(0, days_back))).strftime("%Y-%m-%d")

records = []

doc_files = sorted(os.listdir(DOC_SAMPLE_DIR))
for i, filename in enumerate(doc_files):
    if not filename.endswith(".png"):
        continue
    label = int(filename.split("label")[1].split(".")[0])
    doc_type = DOC_TYPES.get(label, "unknown")

    record = {
        "case_id": f"CASE-{1000 + i}",
        "document_filename": filename,
        "document_type": doc_type,
        "submitted_by": random.choice(SUBMITTERS),
        "department": random.choice(DEPARTMENTS),
        "submission_date": random_date(),
        "amount_inr": round(random.uniform(500, 2500000), 2) if doc_type in ["invoice", "budget"] else None,
        "status": "pending_review"
    }
    records.append(record)

output_path = os.path.join(OUTPUT_DIR, "..", "synthetic", "case_metadata.json")
os.makedirs(os.path.dirname(output_path), exist_ok=True)

with open(output_path, "w", encoding="utf-8") as f:
    json.dump(records, f, indent=2)

print(f"Generated {len(records)} case records → {output_path}")