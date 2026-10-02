import os
import json
from ocr import extract_text

DOC_DIR = "../data/rvl-cdip-sample"
OUTPUT_PATH = "../data/synthetic/ocr_results.json"

results = []
doc_files = sorted(f for f in os.listdir(DOC_DIR) if f.endswith(".png"))

for i, filename in enumerate(doc_files):
    path = os.path.join(DOC_DIR, filename)
    text = extract_text(path)
    results.append({
        "filename": filename,
        "extracted_text": text,
        "char_count": len(text)
    })
    if i % 20 == 0:
        print(f"Processed {i}/{len(doc_files)}")

os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2, ensure_ascii=False)

print(f"Done. OCR results saved for {len(results)} documents → {OUTPUT_PATH}")

avg_chars = sum(r["char_count"] for r in results) / len(results)
empty_count = sum(1 for r in results if r["char_count"] < 20)
print(f"Average characters extracted: {avg_chars:.0f}")
print(f"Documents with near-empty OCR (<20 chars): {empty_count}")