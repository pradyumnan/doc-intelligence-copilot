import os
import sys
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.join(SCRIPT_DIR, "..", "agent"))

from graph import verify_node

POLICY_DIR = "../data/policy-corpus"


def policy(filename):
    with open(os.path.join(POLICY_DIR, filename), encoding="utf-8") as f:
        return f.read()


INVOICE_SMALL = "INVOICE\nInvoice Number: INV-001\nVendor: ABC Supplies\nGSTIN: 27AABCS1234A1Z5\nAmount: ₹5,000.00\nDate: October 1, 2026"
INVOICE_LARGE = "INVOICE\nInvoice Number: INV-002\nVendor: XYZ Corp\nGSTIN: 29AABCT5678B1Z3\nAmount: ₹65,000.00\nDate: October 1, 2026"
INVOICE_NO_GSTIN = "INVOICE\nInvoice Number: INV-009\nVendor: Local Traders\nAmount: ₹12,000.00\nDate: October 3, 2026"
INVOICE_NO_AMOUNT = "INVOICE\nInvoice Number: INV-777\nVendor: XYZ Corp\nDate: October 3, 2026"
KYC = "KYC VERIFICATION\nApplicant: Rohan Gupta\nPAN Card: ABCPG1234D\nAddress Proof: Utility Bill (dated last month)\nNames match exactly."
LOAN = "LOAN APPLICATION\nApplicant: Priya Sharma\nLoan Amount: ₹15,00,000\nIncome Proof: Salary Slip attached\nCredit Score: 720"
CONTRACT = "VENDOR CONTRACT\nVendor: TechCore Systems\nContract Value: ₹8,00,000\nTerm: 12 months\nStandard liability clause included."
COVER_LETTER = "July 11, 1990\n\nMr. C. E. Altman\nSRO/PACE Promotions\nLos Angeles, CA 90036\n\nDear Mr. Altman:\n\nPlease find enclosed an executed addendum to the contract between SRO/PACE Promotions and Sports Marketing Enterprises regarding sponsorship of the Supercross Series.\n\nSincerely,\nContracts Administrator"

# expected_supported=True: the justification is backed by the document.
# expected_supported=False: the justification claims something the document does not contain.
CASES = [
    {"name": "invoice small, correct", "category": "invoice", "policy": "invoice_policy.txt", "document": INVOICE_SMALL, "route": "Finance",
     "justification": "The invoice total is ₹5,000, under ₹10,000, so per policy rule 1 it is auto-approved and routed to Finance.", "expected_supported": True},
    {"name": "invoice large, correct", "category": "invoice", "policy": "invoice_policy.txt", "document": INVOICE_LARGE, "route": "Approval Workflow",
     "justification": "The invoice total is ₹65,000, above ₹50,000, so per policy rule 3 it requires manager approval.", "expected_supported": True},
    {"name": "kyc, correct", "category": "kyc", "policy": "kyc_policy.txt", "document": KYC, "route": "KYC Processing",
     "justification": "The PAN card is acceptable identity proof and the utility bill, dated last month, is acceptable address proof; the names match.", "expected_supported": True},
    {"name": "invoice missing GSTIN, correct absence claim", "category": "invoice", "policy": "invoice_policy.txt", "document": INVOICE_NO_GSTIN, "route": "Compliance Review",
     "justification": "The invoice has no vendor GSTIN, so per policy rule 4 it goes to Compliance Review regardless of amount.", "expected_supported": True},

    {"name": "cover letter, invented liability clause", "category": "contract", "policy": "contract_review_policy.txt", "document": COVER_LETTER, "route": "Legal Escalation",
     "justification": "Policy rule 3 mandates that any contract containing a non-standard liability clause be flagged for Legal Escalation regardless of value.", "expected_supported": False},
    {"name": "invoice small, wrong amount claimed", "category": "invoice", "policy": "invoice_policy.txt", "document": INVOICE_SMALL, "route": "Approval Workflow",
     "justification": "The invoice total is ₹75,000, above ₹50,000, so per rule 3 it goes to Approval Workflow.", "expected_supported": False},
    {"name": "kyc, wrong proof claimed", "category": "kyc", "policy": "kyc_policy.txt", "document": KYC, "route": "KYC Processing",
     "justification": "The address proof is a passport, which is acceptable under rule 1.", "expected_supported": False},
    {"name": "loan, wrong credit score claimed", "category": "loan_application", "policy": "loan_application_policy.txt", "document": LOAN, "route": "Risk Assessment",
     "justification": "The credit score is 610, below 650, so per rule 4 it goes to Risk Assessment.", "expected_supported": False},
    {"name": "contract, standard clause called non-standard", "category": "contract", "policy": "contract_review_policy.txt", "document": CONTRACT, "route": "Legal Escalation",
     "justification": "The contract contains a non-standard liability clause, so per rule 3 it requires Legal Escalation.", "expected_supported": False},
    {"name": "invoice with no amount, amount claimed", "category": "invoice", "policy": "invoice_policy.txt", "document": INVOICE_NO_AMOUNT, "route": "Approval Workflow",
     "justification": "The invoice total of ₹60,000 exceeds ₹50,000, so per rule 3 it requires manager approval.", "expected_supported": False},
]


def main():
    results = []
    for case in CASES:
        state = {
            "document_text": case["document"], "filename": case["name"], "category": case["category"],
            "policy_source": case["policy"], "policy_text": policy(case["policy"]),
            "route": case["route"], "justification": case["justification"], "confidence": 0.95,
            "final_status": None, "verified": None, "verification_note": None,
        }
        result = verify_node(state)
        got = result["verified"]
        ok = got == case["expected_supported"]
        results.append({"name": case["name"], "expected_supported": case["expected_supported"],
                        "verified": got, "correct": ok, "note": result["verification_note"]})
        print(f"{'OK  ' if ok else 'FAIL'} {case['name']}: expected_supported={case['expected_supported']}, got={got}")

    good = [r for r in results if r["expected_supported"]]
    bad = [r for r in results if not r["expected_supported"]]
    caught = sum(1 for r in bad if r["verified"] is False)
    wrongly_held = sum(1 for r in good if r["verified"] is False)
    print(f"\n=== Unsupported decisions caught: {caught}/{len(bad)} ===")
    print(f"=== Supported decisions wrongly held: {wrongly_held}/{len(good)} ===")

    out = "../evaluation/results/verifier_eval.json"
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8") as f:
        json.dump({"results": results, "caught": caught, "unsupported_total": len(bad),
                   "wrongly_held": wrongly_held, "supported_total": len(good)}, f, indent=2, ensure_ascii=False)
    print(f"Saved {out}")


if __name__ == "__main__":
    main()