import sys
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RETRIEVAL_DIR = os.path.join(SCRIPT_DIR, "..", "retrieval")
sys.path.append(RETRIEVAL_DIR)

from search import search_policies

RETRIEVAL_TEST_SET = [
    {"query": "What approval is needed for a 60000 rupee invoice?", "expected_source": "invoice_policy.txt"},
    {"query": "What documents are required for KYC address proof?", "expected_source": "kyc_policy.txt"},
    {"query": "Does a vendor contract need legal review?", "expected_source": "contract_review_policy.txt"},
    {"query": "What credit score is needed for loan approval?", "expected_source": "loan_application_policy.txt"},
    {"query": "What happens if an invoice is missing GSTIN?", "expected_source": "invoice_policy.txt"},
    {"query": "Is co-applicant KYC verification required separately?", "expected_source": "loan_application_policy.txt"},
    {"query": "What address proof documents are acceptable?", "expected_source": "kyc_policy.txt"},
    {"query": "When can a contract skip legal review?", "expected_source": "contract_review_policy.txt"},
]

def run_eval():
    correct_top1 = 0
    results = []

    for case in RETRIEVAL_TEST_SET:
        search_results = search_policies(case["query"], top_k=1)
        top_source, top_text, distance = search_results[0]

        is_correct = top_source == case["expected_source"]
        if is_correct:
            correct_top1 += 1

        results.append({
            "query": case["query"],
            "expected": case["expected_source"],
            "retrieved": top_source,
            "correct": is_correct,
            "distance": distance
        })

        print(f"{'✓' if is_correct else '✗'} \"{case['query'][:50]}...\" → {top_source} (expected {case['expected_source']})")

    total = len(RETRIEVAL_TEST_SET)
    accuracy = correct_top1 / total
    print(f"\n=== Retrieval Top-1 Accuracy: {correct_top1}/{total} ({accuracy*100:.0f}%) ===")

    output_path = "../data/synthetic/retrieval_eval_results.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump({"results": results, "top1_accuracy": accuracy}, f, indent=2)
    print(f"Results saved to {output_path}")

if __name__ == "__main__":
    run_eval()