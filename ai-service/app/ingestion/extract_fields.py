import re
import spacy

nlp = spacy.load("en_core_web_sm")

DATE_PATTERN = re.compile(r'\b(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\w+ \d{1,2},? \d{4})\b')
AMOUNT_PATTERN = re.compile(r'[\$₹]\s?[\d,]+(?:\.\d{2})?|\b\d{1,3}(?:,\d{3})+(?:\.\d{2})?\b')

def extract_fields(text: str) -> dict:
    doc = nlp(text)

    people = [ent.text for ent in doc.ents if ent.label_ == "PERSON" and len(ent.text) > 4 and not ent.text.isdigit()]
    orgs = [ent.text for ent in doc.ents if ent.label_ == "ORG" and len(ent.text) > 3]
    dates_spacy = [ent.text for ent in doc.ents if ent.label_ == "DATE" and not ent.text.isdigit()]

    dates_regex = DATE_PATTERN.findall(text)
    amounts = AMOUNT_PATTERN.findall(text)

    return {
        "people": list(set(people))[:5],
        "organizations": list(set(orgs))[:5],
        "dates": list(set(dates_spacy + dates_regex))[:5],
        "amounts": list(set(amounts))[:5],
    }

if __name__ == "__main__":
    import json
    with open("../data/synthetic/ocr_results.json", "r", encoding="utf-8") as f:
        ocr_data = json.load(f)

    sample = ocr_data[0]
    fields = extract_fields(sample["extracted_text"])
    print(f"--- Extracted fields for {sample['filename']} ---")
    print(json.dumps(fields, indent=2))