#!/usr/bin/env python3
import re
import subprocess
import sys

ENTRY = re.compile(r'(\s*)(<(string|plurals|string-array)\b[^>]*\bname="([^"]+)"[^>]*?(?:/>|>.*?</\3>))', re.S)


def show(stage, path):
    return subprocess.run(["git", "show", f":{stage}:{path}"], capture_output=True, text=True, check=True).stdout


def entries(text):
    return {m.group(4): m.group(2) for m in ENTRY.finditer(text)}


def resolve(path):
    base_text = show(3, path)
    incoming = entries(show(2, path))
    seen = set()

    def sub(m):
        name = m.group(4)
        seen.add(name)
        return m.group(1) + incoming.get(name, m.group(2))

    out = ENTRY.sub(sub, base_text)
    extra = [v for k, v in incoming.items() if k not in seen]
    if extra:
        idx = out.rfind("</resources>")
        out = out[:idx] + "".join(f"    {e}\n" for e in extra) + out[idx:]
    with open(path, "w", encoding="utf-8") as f:
        f.write(out)
    subprocess.run(["git", "add", path], check=True)


if __name__ == "__main__":
    for p in sys.argv[1:]:
        resolve(p)
