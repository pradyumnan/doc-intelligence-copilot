import sys
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.join(SCRIPT_DIR, "..", "agent"))

from graph import build_graph

with open("../evaluation/results/real_scan_classification.json", encoding="utf-8") as f:
    rows = json.load(f)
with open("../data/synthetic/ocr_results.json", encoding="utf-8") as f:
    ocr = {d["filename"]: d["extracted_text"] for d in json.load(f)}

print("=== Real invoices (label 11) NOT classified as invoice ===")
for r in rows:
    if r["rvl_label"] == 11 and r["category"] != "invoice":
        text = ocr[r["filename"]]
        print(f"\n{r['filename']} ({len(text)} characters)\n{text[:500]}")

print("\n\n=== Full pipeline on the two flagged documents ===")
app = build_graph()
for name in ["doc_89_label8.png", "doc_9_label0.png"]:
    state = {
        "document_text": ocr[name], "filename": name,
        "category": None, "policy_source": None, "policy_text": None,
        "route": None, "justification": None, "confidence": None,
        "final_status": None,
    }
    result = app.invoke(state)
    print(f"\n{name}: category={result['category']}, route={result['route']}, "
          f"confidence={result['confidence']}, status={result['final_status']}")
    print(f"  {result['justification']}")