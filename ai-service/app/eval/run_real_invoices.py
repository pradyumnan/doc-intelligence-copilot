import os
import sys
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.join(SCRIPT_DIR, "..", "agent"))

from graph import build_graph

with open("../data/synthetic/ocr_results.json", encoding="utf-8") as f:
    ocr = {d["filename"]: d["extracted_text"] for d in json.load(f)}

NAMES = ["doc_110_label11.png", "doc_112_label11.png", "doc_113_label11.png", "doc_116_label11.png"]

app = build_graph()
rows = []
for name in NAMES:
    state = {
        "document_text": ocr[name], "filename": name,
        "category": None, "policy_source": None, "policy_text": None,
        "route": None, "justification": None, "confidence": None,
        "verified": None, "verification_note": None, "final_status": None,
    }
    r = app.invoke(state)
    rows.append({k: r[k] for k in ["filename", "category", "route", "confidence", "verified", "final_status", "justification"]})
    print(f"\n{name}: category={r['category']}, route={r['route']}, confidence={r['confidence']}, "
          f"verified={r['verified']}, status={r['final_status']}")
    print(f"  {r['justification']}")

out = "../evaluation/results/real_invoices_pipeline.json"
os.makedirs(os.path.dirname(out), exist_ok=True)
with open(out, "w", encoding="utf-8") as f:
    json.dump(rows, f, indent=2, ensure_ascii=False)
print(f"\nSaved {out}")