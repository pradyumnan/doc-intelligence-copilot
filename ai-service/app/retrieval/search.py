import os
from sentence_transformers import SentenceTransformer
import psycopg
from dotenv import load_dotenv

load_dotenv()

model = SentenceTransformer("all-MiniLM-L6-v2")

def search_policies(query: str, top_k: int = 3):
    conn = psycopg.connect(os.getenv("SUPABASE_DB_URL"))
    cur = conn.cursor()

    query_embedding = model.encode(query).tolist()

    # pgvector's <-> operator computes distance (lower = more similar)
    cur.execute(
        """
        SELECT source_file, chunk_text, embedding <-> %s::vector AS distance
        FROM policy_chunks
        ORDER BY distance ASC
        LIMIT %s
        """,
        (query_embedding, top_k)
    )
    results = cur.fetchall()
    cur.close()
    conn.close()
    return results

if __name__ == "__main__":
    test_queries = [
        "What approval is needed for a 60000 rupee invoice?",
        "What documents are required for KYC address proof?",
        "Does a vendor contract need legal review?",
    ]

    for query in test_queries:
        print(f"\n=== Query: {query} ===")
        results = search_policies(query, top_k=2)
        for source, text, distance in results:
            print(f"\n[{source}] (distance: {distance:.4f})")
            print(text[:200])