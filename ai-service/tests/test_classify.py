import sys
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
AGENT_DIR = os.path.join(SCRIPT_DIR, "..", "app", "agent")
sys.path.append(AGENT_DIR)

from classify import classify_document

def test_classify_clean_invoice():
    text = "INVOICE\nAmount: ₹5,000\nVendor: ABC Corp\nGSTIN: 27AABCS1234A1Z5"
    result = classify_document(text)
    assert result == "invoice"

def test_classify_clean_kyc():
    text = "KYC VERIFICATION\nPAN Card: ABCPG1234D\nAddress Proof: Utility Bill"
    result = classify_document(text)
    assert result == "kyc"

def test_classify_unrelated_memo():
    text = "INTERNAL MEMO\nOffice closed Friday for maintenance."
    result = classify_document(text)
    assert result == "other"