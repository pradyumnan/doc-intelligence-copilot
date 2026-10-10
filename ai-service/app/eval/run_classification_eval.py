import sys
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
AGENT_DIR = os.path.join(SCRIPT_DIR, "..", "agent")
sys.path.append(AGENT_DIR)
sys.path.append(SCRIPT_DIR)

from graph import build_graph
from test_set import TEST_SET, HELDOUT_SET

SETS = {"tuning": TEST_SET, "heldout": HELDOUT_SET}


def run_eval(set_name):
    cases = SETS[set_name]
    app = build_graph()
    results = []
    correct_category = 0
    correct_route = 0
    route_checked = 0
    wrong_and_auto_routed = 0

    for case in cases:
        initial_state = {
            "document_text": case["text"],
            "filename": case["filename"],
            "category": None, "policy_source": None, "policy_text": None,
            "route": None, "justification": None, "confidence": None,
            "final_status": None,
        }
        result = app.invoke(initial_state)

        category_match = result["category"] == case["expected_category"]
        if category_match:
            correct_category += 1
        elif result["final_status"] == "auto_routed":
            wrong_and_auto_routed += 1  # the dangerous failure: wrong AND no human sees it

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
            "final_status": result["final_status"],
        })
        mark = "OK  " if category_match else "FAIL"
        print(f"{mark} {case['filename']}: expected={case['expected_category']}, "
              f"got={result['category']}, confidence={result['confidence']}, status={result['final_status']}")

    total = len(cases)
    print(f"\n=== [{set_name}] Classification accuracy: {correct_category}/{total} ({correct_category / total * 100:.0f}%) ===")
    if route_checked:
        print(f"=== [{set_name}] Route accuracy (where checked): {correct_route}/{route_checked} ===")
    print(f"=== [{set_name}] Wrong AND auto-routed: {wrong_and_auto_routed} ===")

    output_path = f"../data/synthetic/eval_results_{set_name}.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump({
            "set": set_name,
            "results": results,
            "classification_accuracy": correct_category / total,
            "wrong_and_auto_routed": wrong_and_auto_routed,
        }, f, indent=2)
    print(f"Results saved to {output_path}")


if __name__ == "__main__":
    name = sys.argv[1] if len(sys.argv) > 1 else "tuning"
    if name not in SETS:
        sys.exit(f"Unknown set '{name}'. Use: {', '.join(SETS)}")
    run_eval(name)