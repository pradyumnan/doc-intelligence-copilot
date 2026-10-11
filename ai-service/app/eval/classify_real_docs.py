import sys
import os
import json
import time

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.join(SCRIPT_DIR, "..", "agent"))

from classify import classify_document

OCR_PATH = "../data/synthetic/ocr_results.json"
OUT_PATH = "../evaluation/results/real_scan_classification.json"

with open(OCR_PATH, encoding="utf-8") as f:
    docs = json.load(f)


def classify_with_retry(text):
    for attempt in range(3):
        try:
            return classify_document(text)
        except Exception as e:
            print(f"  retry {attempt + 1} after error: {str(e)[:80]}")
            time.sleep(15)  # free-tier rate limit: wait, then try again
    return "error"


rows = []
for i, d in enumerate(docs):
    label = int(d["filename"].split("label")[1].split(".")[0])
    category = classify_with_retry(d["extracted_text"])
    rows.append({
        "filename": d["filename"],
        "rvl_label": label,
        "category": category,
        "snippet": d["extracted_text"][:300].replace("\n", " "),
    })
    if i % 20 == 0:
        print(f"{i}/{len(docs)}")
    time.sleep(2.2)  # stay under the free-tier request limit

os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
with open(OUT_PATH, "w", encoding="utf-8") as f:
    json.dump(rows, f, indent=2, ensure_ascii=False)

counts = {}
for r in rows:
    counts[r["category"]] = counts.get(r["category"], 0) + 1
print("\nCategory counts:", counts)

invoices = [r for r in rows if r["rvl_label"] == 11]
found = sum(1 for r in invoices if r["category"] == "invoice")
print(f"\nReal invoices (RVL-CDIP label 11) classified as invoice: {found}/{len(invoices)}")
for r in invoices:
    print(f"  {r['filename']} -> {r['category']}")

flagged = [r for r in rows if r["category"] not in ("other", "error") and r["rvl_label"] != 11]
print(f"\nNon-invoice-class documents classified as a policy category: {len(flagged)}")
for r in flagged:
    print(f"\n{r['filename']} -> {r['category']}\n  {r['snippet']}")

errors = [r for r in rows if r["category"] == "error"]
print(f"\nDocuments that failed to classify: {len(errors)}")