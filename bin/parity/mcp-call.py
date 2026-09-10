#!/usr/bin/env python3
"""mcp-call — minimal MCP JSON-RPC client for the byte-parity harness.

    mcp-call.py <url> <tool-name> <request.json>

Sends ONE tools/call with the frozen request read verbatim from <request.json>
and writes the structured result to stdout as JSON.  It exists because
alias_migration and apply_clojure_changes have no CLI op: the executor is only
reachable over MCP, so a parity run of those specimens must drive a server
built from each side's own source tree.

Exit 0 on a transport-complete call (accepted OR refused: a refusal is a result
the harness compares).  Exit 4 when the call never completed, which is NOT a
parity verdict and is reported as unverified.
"""
import json
import sys
import urllib.error
import urllib.request

FORBIDDEN_PORTS = {"7888", "7890", "7894", "7895"}


def post(url, payload, headers=None, timeout=900, want_headers=False):
    body = json.dumps(payload).encode()
    hdrs = {"Content-Type": "application/json",
            "Accept": "application/json, text/event-stream"}
    hdrs.update(headers or {})
    req = urllib.request.Request(url, data=body, headers=hdrs, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        raw = resp.read().decode("utf-8", "replace")
        if want_headers:
            return raw, dict(resp.headers.items())
        return raw, None


def parse(raw):
    """The server may answer as plain JSON or as a one-event SSE frame."""
    for line in raw.splitlines():
        line = line.strip()
        if line.startswith("data:"):
            line = line[5:].strip()
        if line.startswith("{"):
            try:
                return json.loads(line)
            except json.JSONDecodeError:
                continue
    raise RuntimeError("no JSON object in response: " + raw[:400])


def main():
    if len(sys.argv) != 4:
        print(__doc__, file=sys.stderr)
        return 2
    url, tool, request_file = sys.argv[1:4]
    port = url.split(":")[2].split("/")[0]
    if port in FORBIDDEN_PORTS:
        print(f"refusing to talk to reserved port {port}", file=sys.stderr)
        return 2
    arguments = json.load(open(request_file))
    arguments.pop("op", None)  # the op name is the tool name on the wire
    try:
        raw, hdrs = post(url, {"jsonrpc": "2.0", "id": 0, "method": "initialize",
                               "params": {"protocolVersion": "2025-06-18",
                                          "capabilities": {},
                                          "clientInfo": {"name": "parity-harness",
                                                         "version": "1"}}},
                         want_headers=True, timeout=60)
        session = next((v for k, v in hdrs.items() if k.lower() == "mcp-session-id"), None)
        if not session:
            print("no MCP session id", file=sys.stderr)
            return 4
        post(url, {"jsonrpc": "2.0", "method": "notifications/initialized"},
             {"Mcp-Session-Id": session}, timeout=60)
        raw, _ = post(url, {"jsonrpc": "2.0", "id": 1, "method": "tools/call",
                            "params": {"name": tool, "arguments": arguments}},
                      {"Mcp-Session-Id": session})
    except (urllib.error.URLError, OSError, RuntimeError) as exc:
        print(f"transport unverified: {exc}", file=sys.stderr)
        return 4
    reply = parse(raw)
    result = reply.get("result", reply)
    structured = result.get("structuredContent")
    payload = {"structured": structured,
               "text": [c.get("text") for c in result.get("content", []) if isinstance(c, dict)],
               "isError": result.get("isError"),
               "error": reply.get("error")}
    json.dump(payload, sys.stdout, indent=1, sort_keys=True)
    print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
