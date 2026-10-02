import os

OUTPUT_DIR = "../data/policy-corpus"
os.makedirs(OUTPUT_DIR, exist_ok=True)

policies = {
    "invoice_policy.txt": """
INVOICE PROCESSING POLICY

1. Invoices under ₹10,000 are auto-approved and routed to the Finance queue for payment.
2. Invoices between ₹10,000 and ₹50,000 require review by a department supervisor before payment.
3. Invoices above ₹50,000 require manager approval and must be routed to the Approval Workflow queue.
4. Any invoice missing a vendor GSTIN must be routed to the Compliance Review queue regardless of amount.
5. Duplicate invoice numbers from the same vendor within 30 days must be flagged for Fraud Review.
""",
    "loan_application_policy.txt": """
LOAN APPLICATION PROCESSING POLICY

1. Loan applications must include proof of income (salary slip or IT returns) to proceed to underwriting.
2. Applications missing income proof are routed to the Document Collection queue, not rejected outright.
3. Loan amounts above ₹20,00,000 require senior underwriter approval and are routed to Senior Review.
4. Applicants with a credit score below 650 are routed to Risk Assessment before any approval queue.
5. Co-applicant documents must be verified separately; incomplete co-applicant KYC routes the full case to KYC Pending.
""",
    "kyc_policy.txt": """
KYC (KNOW YOUR CUSTOMER) DOCUMENT POLICY

1. Acceptable address proof documents: utility bill (last 3 months), passport, voter ID, Aadhaar card.
2. Acceptable identity proof: PAN card, passport, voter ID, Aadhaar card.
3. Documents older than 90 days (for utility bills specifically) are not accepted as valid address proof.
4. Mismatched name spelling between identity and address proof routes the case to Manual KYC Review.
5. Corporate KYC requires additional documents: certificate of incorporation, board resolution, and authorized signatory list.
""",
    "contract_review_policy.txt": """
VENDOR CONTRACT REVIEW POLICY

1. Contracts under ₹5,00,000 in total value can be approved by department heads directly.
2. Contracts above ₹5,00,000 require Legal review before signature, routed to the Legal Review queue.
3. Any contract containing a non-standard liability clause must be flagged for Legal Escalation regardless of value.
4. Contracts with vendors not in the approved vendor list are routed to Vendor Onboarding before proceeding.
5. Renewal contracts with no material changes from the prior term can skip Legal review if value is unchanged.
"""
}

for filename, content in policies.items():
    path = os.path.join(OUTPUT_DIR, filename)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip())
    print(f"Created {filename}")

print("Done.")