# Ground truth: document text + the category it SHOULD be classified as.
# TEST_SET is the tuning set. HELDOUT_SET is run once, after prompts are final.

TEST_SET = [
    # --- original clean cases ---
    {
        "filename": "eval_invoice_small.txt",
        "text": "INVOICE\nInvoice Number: INV-001\nVendor: ABC Supplies\nGSTIN: 27AABCS1234A1Z5\nAmount: ₹5,000.00\nDate: October 1, 2026",
        "expected_category": "invoice",
        "expected_route_contains": "Finance",
    },
    {
        "filename": "eval_invoice_large.txt",
        "text": "INVOICE\nInvoice Number: INV-002\nVendor: XYZ Corp\nGSTIN: 29AABCT5678B1Z3\nAmount: ₹65,000.00\nDate: October 1, 2026",
        "expected_category": "invoice",
        "expected_route_contains": "Approval",
    },
    {
        "filename": "eval_kyc.txt",
        "text": "KYC VERIFICATION\nApplicant: Rohan Gupta\nPAN Card: ABCPG1234D\nAddress Proof: Utility Bill (dated last month)\nNames match exactly.",
        "expected_category": "kyc",
        "expected_route_contains": None,
    },
    {
        "filename": "eval_loan.txt",
        "text": "LOAN APPLICATION\nApplicant: Priya Sharma\nLoan Amount: ₹15,00,000\nIncome Proof: Salary Slip attached\nCredit Score: 720",
        "expected_category": "loan_application",
        "expected_route_contains": None,
    },
    {
        "filename": "eval_contract.txt",
        "text": "VENDOR CONTRACT\nVendor: TechCore Systems\nContract Value: ₹8,00,000\nTerm: 12 months\nStandard liability clause included.",
        "expected_category": "contract",
        "expected_route_contains": "Legal",
    },
    {
        "filename": "eval_other_memo.txt",
        "text": "INTERNAL MEMO\nTo: All Staff\nSubject: Office closure on October 15th for maintenance.\nPlease plan accordingly.",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    # --- hard negatives: mention a category but are NOT that document ---
    {
        "filename": "hard_payment_advice_letter.txt",
        "text": "August 17, 1979\n\nDr. S. J. Feinhandler\nSocial Systems Analysts\nWatertown, MA\n\nEnclosed are two checks in payment of our invoices dated August 2, 1979:\n\nConsulting services, July 1-31 $3,982.00\nProject studies, July 1-31 $4,484.70\n\nRemaining balance for 1979: $54,153.52\n\nSincerely,\nSecretary to Dr. H. Wakeham",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    {
        "filename": "hard_quotation.txt",
        "text": "QUOTATION\nQuotation No: Q-2026-114\nValid until: November 15, 2026\nTo: Newgen Procurement\nItem: 20 laptops\nEstimated total: ₹14,00,000\nThis is not a bill. Please confirm to proceed with an order.",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    {
        "filename": "hard_purchase_order.txt",
        "text": "PURCHASE ORDER\nPO Number: PO-7781\nBuyer: Newgen Operations\nSupplier: TechCore Systems\nItems: 10 monitors\nOrder value: ₹2,50,000\nDelivery within 14 days.",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    {
        "filename": "hard_receipt.txt",
        "text": "PAYMENT RECEIPT\nReceived with thanks from Newgen Finance the sum of ₹8,500 against invoice INV-2026-0453.\nDate: October 5, 2026\nPayment mode: bank transfer",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    {
        "filename": "hard_loan_rejection.txt",
        "text": "Dear Ms. Sharma,\nWe have reviewed your loan application dated September 12, 2026 for ₹15,00,000. We regret to inform you that it has not been approved because of a low credit score.\nRegards, Retail Lending Team",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    {
        "filename": "hard_bank_statement.txt",
        "text": "ACCOUNT STATEMENT\nAccount: XXXX4421\nPeriod: September 2026\nSep 03 Salary credit 85,000.00\nSep 10 Payment to TechCore Systems 75,000.00\nClosing balance 22,340.50",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    # --- positives with different layout and wording ---
    {
        "filename": "varied_invoice_services.txt",
        "text": "TAX INVOICE\nBill No: 2026/118\nFrom: Iyer Consulting LLP (GSTIN 33AAJFI4321K1Z7)\nTo: Newgen Software Technologies\nProfessional services, September 2026\nAmount due: ₹42,000\nDue date: October 30, 2026",
        "expected_category": "invoice",
        "expected_route_contains": None,
    },
    {
        "filename": "varied_loan.txt",
        "text": "APPLICATION FOR PERSONAL LOAN\nApplicant: Arjun Mehta\nAmount requested: ₹6,00,000\nTenure: 36 months\nMonthly income: ₹95,000 (salary slips for 3 months attached)\nSigned: Arjun Mehta",
        "expected_category": "loan_application",
        "expected_route_contains": None,
    },
    {
        "filename": "varied_kyc.txt",
        "text": "CUSTOMER VERIFICATION RECORD\nCustomer: Fatima Khan\nIdentity proof: Aadhaar card verified\nAddress proof: Passport, valid\nName on both documents matches.\nVerified by: branch officer",
        "expected_category": "kyc",
        "expected_route_contains": None,
    },
    {
        "filename": "varied_contract.txt",
        "text": "SERVICE AGREEMENT\nThis agreement is made on October 1, 2026 between Newgen Software Technologies and Iyer Consulting LLP.\nTerm: 12 months. Fees: ₹5,00,000 payable quarterly.\nEither party may terminate with 30 days' written notice.\nSigned for both parties.",
        "expected_category": "contract",
        "expected_route_contains": None,
    },
]

# Do NOT tune prompts against these. Run once when the prompt is final.
HELDOUT_SET = [
    {
        "filename": "heldout_invoice_goods.txt",
        "text": "INVOICE NO. 5531\nSeller: Rao Hardware Traders, GSTIN 36AAEPR8891M1Z2\nBuyer: Newgen Admin Office\n50 office chairs @ ₹3,200\nTotal payable: ₹1,60,000\nPayment due in 30 days",
        "expected_category": "invoice",
        "expected_route_contains": None,
    },
    {
        "filename": "heldout_payment_confirmation.txt",
        "text": "Subject: Payment released\n\nHi team,\nFinance has released payment for the vendor bills we received in September (three invoices, total ₹1,12,000). Please update the tracker.\nThanks",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    {
        "filename": "heldout_home_loan.txt",
        "text": "HOME LOAN APPLICATION FORM\nApplicant name: Priya Sharma\nProperty value: ₹60,00,000\nLoan amount requested: ₹45,00,000\nEmployer: Newgen; annual income ₹14,00,000\nIncome proof and address proof enclosed",
        "expected_category": "loan_application",
        "expected_route_contains": None,
    },
    {
        "filename": "heldout_loan_approval.txt",
        "text": "Dear Mr. Gupta,\nWe are pleased to inform you that your home loan of ₹30,00,000 has been sanctioned subject to the terms in the attached sanction letter.\nCongratulations.",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    {
        "filename": "heldout_kyc_corporate.txt",
        "text": "CORPORATE KYC CHECKLIST\nCompany: TechCore Systems Ltd\nCertificate of incorporation: received\nBoard resolution: received\nAuthorized signatory list: received\nAddress proof: utility bill, September 2026",
        "expected_category": "kyc",
        "expected_route_contains": None,
    },
    {
        "filename": "heldout_contract_vendor.txt",
        "text": "VENDOR SUPPLY CONTRACT\nBetween Newgen Procurement and Rao Hardware Traders.\nTerm: 24 months from November 1, 2026.\nTotal contract value: ₹9,00,000.\nLiability limited to the value of goods supplied.\nBoth parties agree to the terms above.",
        "expected_category": "contract",
        "expected_route_contains": None,
    },
    {
        "filename": "heldout_email_about_contract.txt",
        "text": "Subject: Draft vendor agreement\n\nHi,\nCould you review the attached draft agreement with TechCore before Friday? I think the notice period needs discussing. No need to sign anything yet.\nRegards, Ananya",
        "expected_category": "other",
        "expected_route_contains": None,
    },
    {
        "filename": "heldout_circular.txt",
        "text": "CIRCULAR\nAll branches must complete the annual fire safety drill by November 20, 2026.\nAttendance sheets to be sent to Admin.",
        "expected_category": "other",
        "expected_route_contains": None,
    },
]