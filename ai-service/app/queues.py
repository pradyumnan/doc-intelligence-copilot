"""The only queues a document can be routed to.

The model used to write route names freely, which produced near-duplicates such as
"KYC_Automated" and "KYC_Processing" for the same thing. The agent now has to pick from this
closed list, and anything else is held for a person.

Keep this list in sync with WORK_QUEUES in bpm-service (Queues.java).
"""
import re

GENERAL_REVIEW = "General Review"

QUEUES = [
    "Finance",
    "Approval Workflow",
    "Compliance Review",
    "Fraud Review",
    "Document Collection",
    "Underwriting",
    "Senior Review",
    "Risk Assessment",
    "KYC Cleared",
    "KYC Pending",
    "Manual KYC Review",
    "Department Head Approval",
    "Legal Review",
    "Legal Escalation",
    "Vendor Onboarding",
    GENERAL_REVIEW,
]


def _key(name: str) -> str:
    text = name.lower().replace("_", " ")
    text = re.sub(r"\s+", " ", text).strip()
    return re.sub(r"\s+queue$", "", text)


def normalize_route(name):
    """Return the canonical queue name for a model-written route, or None if it is not a real queue."""
    if not isinstance(name, str):
        return None
    wanted = _key(name)
    for queue in QUEUES:
        if _key(queue) == wanted:
            return queue
    return None
