import sys
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.join(SCRIPT_DIR, "..", "app"))

from queues import QUEUES, GENERAL_REVIEW, normalize_route


def test_exact_queue_name_is_accepted():
    assert normalize_route("Finance") == "Finance"
    assert normalize_route("Legal Escalation") == "Legal Escalation"


def test_case_underscores_and_trailing_queue_are_tolerated():
    assert normalize_route("approval_workflow") == "Approval Workflow"
    assert normalize_route("  COMPLIANCE   review ") == "Compliance Review"
    assert normalize_route("General Review Queue") == GENERAL_REVIEW


def test_invented_queue_names_are_rejected():
    assert normalize_route("KYC_Automated") is None
    assert normalize_route("Manual Review Queue") is None
    assert normalize_route("") is None


def test_non_strings_are_rejected():
    assert normalize_route(None) is None
    assert normalize_route(42) is None


def test_every_queue_maps_to_itself():
    for queue in QUEUES:
        assert normalize_route(queue) == queue
