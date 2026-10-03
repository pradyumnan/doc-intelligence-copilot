# Ground truth test set: document text + the category it SHOULD be classified as
TEST_SET = [
    {
        "filename": "eval_invoice_small.txt",
        "text": "INVOICE\nInvoice Number: INV-001\nVendor: ABC Supplies\nGSTIN: 27AABCS1234A1Z5\nAmount: ₹5,000.00\nDate: October 1, 2026",
        "expected_category": "invoice",
        "expected_route_contains": "Finance"
    },
    {
        "filename": "eval_invoice_large.txt",
        "text": "INVOICE\nInvoice Number: INV-002\nVendor: XYZ Corp\nGSTIN: 29AABCT5678B1Z3\nAmount: ₹65,000.00\nDate: October 1, 2026",
        "expected_category": "invoice",
        "expected_route_contains": "Approval"
    },
    {
        "filename": "eval_kyc.txt",
        "text": "KYC VERIFICATION\nApplicant: Rohan Gupta\nPAN Card: ABCPG1234D\nAddress Proof: Utility Bill (dated last month)\nNames match exactly.",
        "expected_category": "kyc",
        "expected_route_contains": None  # just check category, not exact route
    },
    {
        "filename": "eval_loan.txt",
        "text": "LOAN APPLICATION\nApplicant: Priya Sharma\nLoan Amount: ₹15,00,000\nIncome Proof: Salary Slip attached\nCredit Score: 720",
        "expected_category": "loan_application",
        "expected_route_contains": None
    },
    {
        "filename": "eval_contract.txt",
        "text": "VENDOR CONTRACT\nVendor: TechCore Systems\nContract Value: ₹8,00,000\nTerm: 12 months\nStandard liability clause included.",
        "expected_category": "contract",
        "expected_route_contains": "Legal"
    },
    {
        "filename": "eval_other_memo.txt",
        "text": "INTERNAL MEMO\nTo: All Staff\nSubject: Office closure on October 15th for maintenance.\nPlease plan accordingly.",
        "expected_category": "other",
        "expected_route_contains": None
    },
]