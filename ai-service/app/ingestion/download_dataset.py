import os
from datasets import load_dataset
from dotenv import load_dotenv
from huggingface_hub import login
from PIL import Image

load_dotenv()
login(token=os.getenv("HUGGINGFACE_TOKEN"))

dataset = load_dataset("nielsr/rvl_cdip_10_examples_per_class", split="train")

OUTPUT_DIR = "../data/rvl-cdip-sample"
os.makedirs(OUTPUT_DIR, exist_ok=True)

print(f"Loaded {len(dataset)} rows")

for i, example in enumerate(dataset):
    image = example["image"]
    label = example["label"]
    image.save(os.path.join(OUTPUT_DIR, f"doc_{i}_label{label}.png"))

print(f"Saved {len(dataset)} images. Done.")