import sys
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(SCRIPT_DIR)

from graph import build_graph

CLEAN_TEST_DOCS = {
    "clean_invoice_small.txt": """
INVOICE

Invoice Number: INV-2026-0453
Vendor: Sharma Office Supplies Pvt Ltd
GSTIN: 27AABCS1234A1Z5
Date: October 2, 2026

Bill To: Newgen Operations Department

Item: Office stationery supplies
Amount: ₹8,500.00

Total Due: ₹8,500.00
Payment Terms: Net 30
""",
    "clean_invoice_large.txt": """
INVOICE

Invoice Number: INV-2026-0891
Vendor: TechCore Systems Ltd
GSTIN: 29AABCT5678B1Z3
Date: October 2, 2026

Bill To: Newgen IT Procurement

Item: Annual server hosting contract
Amount: ₹75,000.00

Total Due: ₹75,000.00
Payment Terms: Net 45
""",
    "clean_kyc_complete.txt": """
KYC VERIFICATION FORM

Applicant Name: Rohan Gupta
Date of Birth: 1990-04-15

Identity Proof Submitted: PAN Card (Number: ABCPG1234D)
Address Proof Submitted: Utility Bill dated September 10, 2026 (within last 3 months)

Address: 42 MG Road, Bangalore, Karnataka 560001

Verification Status: All documents match applicant name exactly.
""",
}

def main():
    app = build_graph()

    for filename, text in CLEAN_TEST_DOCS.items():
        print(f"\n=== {filename} ===")
        initial_state = {
            "document_text": text.strip(),
            "filename": filename,
            "category": None, "policy_source": None, "policy_text": None,
            "route": None, "justification": None, "confidence": None,
            "final_status": None
        }
        result = app.invoke(initial_state)
        print(json.dumps({
            "category": result["category"],
            "route": result["route"],
            "justification": result["justification"],
            "confidence": result["confidence"],
            "final_status": result["final_status"]
        }, indent=2))

if __name__ == "__main__":
    main()