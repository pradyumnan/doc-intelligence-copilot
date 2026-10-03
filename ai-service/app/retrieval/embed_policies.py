import os
import glob
from sentence_transformers import SentenceTransformer
import psycopg
from dotenv import load_dotenv

load_dotenv()

model = SentenceTransformer("all-MiniLM-L6-v2")

def chunk_text(text, chunk_size=300, overlap=50):
    """Simple word-based chunking with overlap."""
    words = text.split()
    chunks = []
    i = 0
    while i < len(words):
        chunk = " ".join(words[i:i + chunk_size])
        chunks.append(chunk)
        i += chunk_size - overlap
    return chunks

def main():
    conn = psycopg.connect(os.getenv("SUPABASE_DB_URL"))
    cur = conn.cursor()

    policy_files = glob.glob("../data/policy-corpus/*.txt")
    total_chunks = 0

    for filepath in policy_files:
        filename = os.path.basename(filepath)
        with open(filepath, "r", encoding="utf-8") as f:
            text = f.read()

        chunks = chunk_text(text)
        for chunk in chunks:
            embedding = model.encode(chunk).tolist()
            cur.execute(
                "INSERT INTO policy_chunks (source_file, chunk_text, embedding) VALUES (%s, %s, %s)",
                (filename, chunk, embedding)
            )
            total_chunks += 1

        print(f"Embedded {filename}: {len(chunks)} chunks")

    conn.commit()
    cur.close()
    conn.close()
    print(f"Done. {total_chunks} total chunks embedded and stored.")

if __name__ == "__main__":
    main()