#!/bin/sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
fixture=$(mktemp -d "${TMPDIR:-/tmp}/direct-cclsp-audit.XXXXXX")
trap 'rm -rf "$fixture"' EXIT HUP INT TERM

mkdir -p "$fixture/app/.codex" "$fixture/app/.claude" "$fixture/clj-surgeon-probe"
printf '%s\n' '[mcp_servers.clj-surgeon]' 'url = "http://127.0.0.1:7888/mcp"' > "$fixture/app/.codex/config.toml"
printf '%s\n' '{"enabledMcpjsonServers": ["clj-surgeon"]}' > "$fixture/app/.claude/settings.local.json"
printf '%s\n' 'cclsp-start:' '\t@true' > "$fixture/clj-surgeon-probe/Makefile"

python3 "$repo_root/dev/audit_direct_cclsp_clients.py" --root "$fixture" > "$fixture/green.json"
python3 - "$fixture/green.json" <<'PY'
import json, sys
result = json.load(open(sys.argv[1]))
assert result["ok"] is True
assert result["violations"] == []
PY

printf '%s\n' '[mcp_servers.cclsp]' 'url = "http://127.0.0.1:7890/mcp"' >> "$fixture/app/.codex/config.toml"
printf '%s\n' 'install-cclsp:' '\t@true' > "$fixture/app/Makefile"
if python3 "$repo_root/dev/audit_direct_cclsp_clients.py" --root "$fixture" > "$fixture/red.json"; then
  echo "expected direct-client audit to refuse" >&2
  exit 1
fi
python3 - "$fixture/red.json" <<'PY'
import json, sys
result = json.load(open(sys.argv[1]))
assert result["ok"] is False
assert {(item["kind"], item["line"]) for item in result["violations"]} == {
    ("codex-server", 3),
    ("make-target", 1),
}
assert all(set(item) == {"kind", "file", "line"} for item in result["violations"])
PY

echo "direct cclsp client audit self-test: ok"
