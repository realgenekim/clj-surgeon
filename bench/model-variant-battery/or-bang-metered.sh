#!/bin/bash
# Metered gpt-oss-120b worksheet -> Cerebras -> guarded edit.
set -euo pipefail

if [[ $# -ne 5 ]]; then
  echo "usage: $0 <workspace-root> <file> <owner-form> <decision> <receipt.json>" >&2
  exit 2
fi

workspace_root=$1
file=$2
owner=$3
decision=$4
receipt=$5
mcp_port=${OR_BANG_PORT:-7889}
secret_file=/Users/genekim/src.local/secrets/openrouter.edn
endpoint="http://127.0.0.1:${mcp_port}/mcp"
content_type='Content-Type: application/json'
accept='Accept: application/json, text/event-stream'
scratch=$(mktemp -d /private/tmp/or-bang-metered.XXXXXX)
trap 'rm -rf "$scratch"' EXIT

key=$(grep -o 'sk-or-[a-zA-Z0-9-]*' "$secret_file" | head -1)
if [[ -z "$key" ]]; then
  echo "OpenRouter key not found in supplied secret file" >&2
  exit 1
fi

post() {
  curl -sS -X POST "$endpoint" -H "$content_type" -H "$accept" \
    ${session_id:+-H "Mcp-Session-Id: $session_id"} -d @-
}

now() { python3 -c 'import time; print(time.time())'; }

t0=$(now)
curl -sS -D "$scratch/headers" -X POST "$endpoint" -H "$content_type" -H "$accept" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"or-bang-metered","version":"1"}}}' \
  > /dev/null
session_id=$(awk 'tolower($1)=="mcp-session-id:" {gsub("\\r", "", $2); print $2}' "$scratch/headers")
if [[ -z "$session_id" ]]; then
  echo "MCP initialization returned no session id" >&2
  exit 1
fi
echo '{"jsonrpc":"2.0","method":"notifications/initialized"}' | post > /dev/null

python3 - "$workspace_root" "$file" "$owner" > "$scratch/inspect-request.json" <<'PY'
import json, sys
workspace, file, owner = sys.argv[1:]
print(json.dumps({
    "jsonrpc": "2.0", "id": 2, "method": "tools/call",
    "params": {"name": "inspect_clojure", "arguments": {
        "workspace_root": workspace,
        "requests": [{"id": "owner", "operation": "forms", "file": file,
                      "forms": [owner], "expect": {"forms": 1}}],
        "expect": {"requests": 1, "files": 1}}}}))
PY
post < "$scratch/inspect-request.json" > "$scratch/inspect-response.sse"
t1=$(now)

python3 - "$scratch/inspect-response.sse" "$scratch/source.clj" <<'PY'
import json, sys
body_path, source_path = sys.argv[1:]
source = None
for line in open(body_path):
    if not line.startswith("data:"):
        continue
    payload = json.loads(line[5:])
    structured = payload.get("result", {}).get("structuredContent", {})
    for result in structured.get("results", []):
        for form in result.get("forms", []):
            source = form.get("source")
            break
if not source:
    raise SystemExit("inspect_clojure returned no owner source")
open(source_path, "w").write(source)
PY

python3 - "$scratch/source.clj" "$decision" > "$scratch/openrouter-request.json" <<'PY'
import json, sys
source = open(sys.argv[1]).read()
decision = sys.argv[2]
print(json.dumps({
    "model": "openai/gpt-oss-120b",
    "provider": {"only": ["Cerebras"]},
    "usage": {"include": True},
    "messages": [{"role": "user", "content":
        "You write one Clojure form. Output ONLY the replacement form, no prose, no fences.\n\n"
        + "Current form:\n" + source + "\n\nRewrite it so that: " + decision}]}))
PY

t2=$(now)
http_code=$(curl -sS --max-time 60 -o "$scratch/openrouter-response.json" -w '%{http_code}' \
  https://openrouter.ai/api/v1/chat/completions \
  -H "Authorization: Bearer $key" -H 'Content-Type: application/json' \
  -d @"$scratch/openrouter-request.json")
t3=$(now)

python3 - "$scratch/openrouter-response.json" "$scratch/replacement.clj" <<'PY'
import json, sys
response = json.load(open(sys.argv[1]))
choices = response.get("choices") or []
replacement = choices[0].get("message", {}).get("content", "").strip() if choices else ""
open(sys.argv[2], "w").write(replacement)
PY

if [[ -s "$scratch/replacement.clj" ]]; then
  python3 - "$workspace_root" "$file" "$owner" "$scratch/source.clj" "$scratch/replacement.clj" \
    > "$scratch/edit-request.json" <<'PY'
import json, sys
workspace, file, owner, source_path, replacement_path = sys.argv[1:]
source = open(source_path).read()
replacement = open(replacement_path).read().strip()
print(json.dumps({
    "jsonrpc": "2.0", "id": 3, "method": "tools/call",
    "params": {"name": "edit_clojure", "arguments": {
        "workspace_root": workspace,
        "edits": [{"file": file, "within": {"form": owner},
                   "from": source, "to": replacement, "matches": 1}]}}}))
PY
  post < "$scratch/edit-request.json" > "$scratch/edit-response.sse"
else
  : > "$scratch/edit-response.sse"
fi
t4=$(now)

mkdir -p "$(dirname "$receipt")"
python3 - "$receipt" "$scratch/openrouter-response.json" "$scratch/edit-response.sse" \
  "$scratch/source.clj" "$scratch/replacement.clj" "$workspace_root" "$file" "$owner" \
  "$decision" "$http_code" "$t0" "$t1" "$t2" "$t3" "$t4" <<'PY'
import hashlib, json, sys

(receipt_path, response_path, edit_path, source_path, replacement_path,
 workspace, file, owner, decision, http_code, *times) = sys.argv[1:]
t0, t1, t2, t3, t4 = map(float, times)
response = json.load(open(response_path))
replacement = open(replacement_path).read().strip()

structured = None
for line in open(edit_path):
    if line.startswith("data:"):
        payload = json.loads(line[5:])
        candidate = payload.get("result", {}).get("structuredContent")
        if candidate is not None:
            structured = candidate

committed = bool(structured and structured.get("committed") is True)
usage = response.get("usage") or {}
record = {
    "schema": "clj-surgeon.or-bang-metered/v1",
    "intent": {"file": file, "owner": owner, "decision": decision},
    "model_requested": "openai/gpt-oss-120b",
    "provider_requested": "Cerebras",
    "model_returned": response.get("model"),
    "provider_returned": response.get("provider"),
    "http_status": int(http_code),
    "source_sha256": hashlib.sha256(open(source_path, "rb").read()).hexdigest(),
    "replacement": replacement or None,
    "usage": usage,
    "provider_cost_usd": usage.get("cost"),
    "model_error": response.get("error"),
    "guarded_edit": structured,
    "committed": committed,
    "timing_s": {
        "read": round(t1 - t0, 3),
        "elaborate": round(t3 - t2, 3),
        "apply": round(t4 - t3, 3),
        "total": round(t4 - t0, 3),
    },
}
with open(receipt_path, "w") as handle:
    json.dump(record, handle, indent=2, sort_keys=True)
    handle.write("\n")
print(json.dumps({
    "receipt": receipt_path,
    "committed": committed,
    "cost_usd": record["provider_cost_usd"],
    "timing_s": record["timing_s"],
    "error": record["model_error"],
}, sort_keys=True))
PY
