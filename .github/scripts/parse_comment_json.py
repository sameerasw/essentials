import os
import json
import re

body = os.environ.get("COMMENT_BODY", "")
match = re.search(r"```(?:json)?\s*(.*?)\s*```", body, re.DOTALL)

if match:
    json_str = match.group(1).strip()
else:
    # Fallback to finding raw JSON array [ ... ]
    match_arr = re.search(r"\[\s*\{.*\}\s*\]", body, re.DOTALL)
    json_str = match_arr.group(0).strip() if match_arr else ""

if json_str:
    with open("/tmp/edits.json", "w", encoding="utf-8") as f:
        f.write(json_str)
else:
    with open("/tmp/edits.json", "w", encoding="utf-8") as f:
        f.write("[]")
    print("Warning: No valid JSON block found in comment body")

