import sys
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
AGENT_DIR = os.path.join(SCRIPT_DIR, "..", "agent")
sys.path.append(AGENT_DIR)
sys.path.append(SCRIPT_DIR)

from graph import build_graph
from test_set import TEST_SET

def run_eval():
    app = build_graph()
    results = []
    correct_category = 0
    correct_route = 0
    route_checked = 0

    for case in TEST_SET:
        initial_state = {
            "document_text": case["text"],
            "filename": case["filename"],
            "category": None, "policy_source": None, "policy_text": None,
            "route": None, "justification": None, "confidence": None,
            "final_status": None
        }
        result = app.invoke(initial_state)

        category_match = result["category"] == case["expected_category"]
        if category_match:
            correct_category += 1

        route_match = None
        if case["expected_route_contains"]:
            route_checked += 1
            route_match = case["expected_route_contains"].lower() in result["route"].lower()
            if route_match:
                correct_route += 1

        results.append({
            "filename": case["filename"],
            "expected_category": case["expected_category"],
            "actual_category": result["category"],
            "category_correct": category_match,
            "route_match": route_match,
            "confidence": result["confidence"],
            "final_status": result["final_status"]
        })

        print(f"{case['filename']}: expected={case['expected_category']}, got={result['category']}, "
              f"{'✓' if category_match else '✗'} (confidence: {result['confidence']})")

    total = len(TEST_SET)
    print(f"\n=== Classification Accuracy: {correct_category}/{total} ({correct_category/total*100:.0f}%) ===")
    if route_checked > 0:
        print(f"=== Route Accuracy (where checked): {correct_route}/{route_checked} ({correct_route/route_checked*100:.0f}%) ===")

    output_path = "../data/synthetic/eval_results.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump({
            "results": results,
            "classification_accuracy": correct_category / total,
            "route_accuracy": correct_route / route_checked if route_checked > 0 else None
        }, f, indent=2)
    print(f"\nResults saved to {output_path}")

if __name__ == "__main__":
    run_eval()