import sys
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.join(SCRIPT_DIR, "..", "app", "agent"))

from graph import verify_node, confidence_gate_node

CONTRACT_POLICY = (
    "VENDOR CONTRACT REVIEW POLICY 1. Contracts under ₹5,00,000 in total value can be approved by "
    "department heads directly. 2. Contracts above ₹5,00,000 require Legal review before signature. "
    "3. Any contract containing a non-standard liability clause must be flagged for Legal Escalation "
    "regardless of value."
)


def make_state(**overrides):
    state = {
        "document_text": "", "filename": "test.txt", "category": "contract",
        "policy_source": "contract_review_policy.txt", "policy_text": CONTRACT_POLICY,
        "route": "Legal Escalation", "justification": "", "confidence": 0.92,
        "final_status": None, "verified": None, "verification_note": None,
    }
    state.update(overrides)
    return state


def test_verifier_rejects_invented_clause():
    state = make_state(
        document_text="July 11, 1990\n\nDear Mr. Altman:\n\nPlease find enclosed an executed addendum to the contract regarding sponsorship of the Supercross Series.\n\nSincerely,\nContracts Administrator",
        justification="Policy rule 3 mandates that any contract containing a non-standard liability clause be flagged for Legal Escalation regardless of value.",
    )
    result = verify_node(state)
    assert result["verified"] is False
    assert result["confidence"] <= 0.4


def test_verifier_accepts_supported_claim():
    state = make_state(
        document_text="VENDOR CONTRACT\nVendor: TechCore Systems\nContract Value: ₹8,00,000\nTerm: 12 months\nStandard liability clause included.",
        route="Legal Review",
        justification="The contract value of ₹8,00,000 is above ₹5,00,000, so per rule 2 it requires Legal review.",
        confidence=0.95,
    )
    result = verify_node(state)
    assert result["verified"] is True


def test_gate_holds_unverified_decision_even_at_high_confidence():
    result = confidence_gate_node(make_state(confidence=0.99, verified=False))
    assert result["final_status"] == "needs_human_review"