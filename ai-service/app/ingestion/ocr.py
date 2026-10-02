import pytesseract
from PIL import Image

def extract_text(image_path: str) -> str:
    """Run OCR on a document image and return raw extracted text."""
    image = Image.open(image_path)
    text = pytesseract.image_to_string(image)
    return text.strip()

if __name__ == "__main__":
    import sys
    path = sys.argv[1] if len(sys.argv) > 1 else "../data/rvl-cdip-sample/doc_0_label0.png"
    result = extract_text(path)
    print(f"--- OCR output for {path} ---")
    print(result[:1000])  # first 1000 chars for a quick look
    print(f"\n[{len(result)} total characters extracted]")