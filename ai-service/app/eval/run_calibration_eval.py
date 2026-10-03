import sys
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
AGENT_DIR = os.path.join(SCRIPT_DIR, "..", "agent")
sys.path.append(AGENT_DIR)
sys.path.append(SCRIPT_DIR)

from graph import build_graph
from test_set import TEST_SET

def run_calibration():
    app = build_graph()

    # Use the labeled test set (known-correct) to check calibration
    # In a larger project we'd hand-label more RVL-CDIP docs too — noted as a future improvement
    buckets = {"high (0.8-1.0)": [], "medium (0.6-0.8)": [], "low (0.0-0.6)": []}

    for case in TEST_SET:
        initial_state = {
            "document_text": case["text"],
            "filename": case["filename"],
            "category": None, "policy_source": None, "policy_text": None,
            "route": None, "justification": None, "confidence": None,
            "final_status": None
        }
        result = app.invoke(initial_state)

        is_correct = result["category"] == case["expected_category"]
        conf = result["confidence"]

        if conf >= 0.8:
            bucket = "high (0.8-1.0)"
        elif conf >= 0.6:
            bucket = "medium (0.6-0.8)"
        else:
            bucket = "low (0.0-0.6)"

        buckets[bucket].append(is_correct)

    print("=== Confidence Calibration ===")
    for bucket, outcomes in buckets.items():
        if outcomes:
            accuracy = sum(outcomes) / len(outcomes)
            print(f"{bucket}: {sum(outcomes)}/{len(outcomes)} correct ({accuracy*100:.0f}%)")
        else:
            print(f"{bucket}: no samples")

    print("\nNote: evaluated on labeled test set (n=6). A production system would expand this")
    print("with a larger hand-labeled sample, including real noisy documents, for statistically")
    print("meaningful calibration curves.")

if __name__ == "__main__":
    run_calibration()