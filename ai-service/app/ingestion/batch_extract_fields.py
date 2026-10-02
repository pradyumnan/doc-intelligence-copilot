import json
from extract_fields import extract_fields

with open("../data/synthetic/ocr_results.json", "r", encoding="utf-8") as f:
    ocr_data = json.load(f)

results = []
for i, doc in enumerate(ocr_data):
    fields = extract_fields(doc["extracted_text"])
    results.append({
        "filename": doc["filename"],
        **fields
    })
    if i % 20 == 0:
        print(f"Processed {i}/{len(ocr_data)}")

output_path = "../data/synthetic/extracted_fields.json"
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2, ensure_ascii=False)

print(f"Done. Extracted fields for {len(results)} documents → {output_path}")

with_dates = sum(1 for r in results if r["dates"])
with_amounts = sum(1 for r in results if r["amounts"])
with_orgs = sum(1 for r in results if r["organizations"])
print(f"Documents with at least one date found: {with_dates}/{len(results)}")
print(f"Documents with at least one amount found: {with_amounts}/{len(results)}")
print(f"Documents with at least one org found: {with_orgs}/{len(results)}")