#!/usr/bin/env python3
"""Prove prepared confirmation and preview through the installed HTTP route."""

import argparse
import hashlib
import http.client
import json
from pathlib import Path
from urllib.parse import urlparse


ORIGINAL = "(ns demo)\n(def alpha :old)\n"
COMMITTED = "(ns demo)\n(def alpha :new)\n"


def encoded(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode()


def sha256(value):
    return hashlib.sha256(value).hexdigest()


def response_json(body):
    if body.startswith(b"{"):
        return json.loads(body)
    for line in body.splitlines():
        if line.startswith(b"data: "):
            return json.loads(line[6:])
    raise RuntimeError("MCP response contained no JSON payload")


class Client:
    def __init__(self, url, evidence_dir):
        parsed = urlparse(url)
        self.host = parsed.hostname
        self.port = parsed.port
        self.path = parsed.path
        self.evidence_dir = evidence_dir
        self.session_id = None

    def post(self, stem, value, *, expect_json=True):
        request_body = encoded(value)
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
        }
        if self.session_id:
            headers["Mcp-Session-Id"] = self.session_id
        conn = http.client.HTTPConnection(self.host, self.port, timeout=30)
        conn.request("POST", self.path, body=request_body, headers=headers)
        response = conn.getresponse()
        response_body = response.read()
        response_headers = dict(response.getheaders())
        conn.close()
        (self.evidence_dir / f"{stem}.request.json").write_bytes(request_body)
        (self.evidence_dir / f"{stem}.response.body").write_bytes(response_body)
        retained_headers = {
            key: (
                sha256(value.encode())
                if key.lower() == "mcp-session-id"
                else value
            )
            for key, value in response_headers.items()
        }
        (self.evidence_dir / f"{stem}.response.headers.json").write_bytes(
            encoded(retained_headers)
        )
        if response.status not in (200, 202):
            raise RuntimeError(f"{stem} returned HTTP {response.status}")
        if expect_json:
            return response_json(response_body), response_headers
        return None, response_headers


def tool_request(call_id, name, arguments):
    return {
        "jsonrpc": "2.0",
        "id": call_id,
        "method": "tools/call",
        "params": {"name": name, "arguments": arguments},
    }


def structured(response):
    return (response.get("result") or {}).get("structuredContent") or {}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://127.0.0.1:7888/mcp")
    parser.add_argument("--result-dir", required=True)
    args = parser.parse_args()
    result_dir = Path(args.result_dir).resolve()
    require(not result_dir.exists(), "result directory must not already exist")
    evidence_dir = result_dir / "wire"
    workspace = result_dir / "workspace"
    source_file = workspace / "src/demo.clj"
    evidence_dir.mkdir(parents=True)
    source_file.parent.mkdir(parents=True)
    source_file.write_text(ORIGINAL)

    client = Client(args.url, evidence_dir)
    initialized, headers = client.post(
        "00-initialize",
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2025-03-26",
                "capabilities": {},
                "clientInfo": {
                    "name": "installed-prepared-actions-proof",
                    "version": "1",
                },
            },
        },
    )
    require(
        ((initialized.get("result") or {}).get("serverInfo") or {}).get("name")
        == "clj-surgeon",
        "initialize did not reach clj-surgeon",
    )
    client.session_id = next(
        (value for key, value in headers.items() if key.lower() == "mcp-session-id"),
        None,
    )
    require(bool(client.session_id), "initialize returned no MCP session id")
    client.post(
        "01-initialized",
        {"jsonrpc": "2.0", "method": "notifications/initialized"},
        expect_json=False,
    )

    tools, _ = client.post(
        "02-tools-list",
        {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}},
    )
    edit_tool = next(
        tool
        for tool in (tools.get("result") or {}).get("tools", [])
        if tool.get("name") == "edit_clojure"
    )
    schema_bytes = encoded(edit_tool.get("inputSchema"))
    require(b'"confirm"' in schema_bytes, "installed edit schema lacks confirm")
    require(b'"preview"' in schema_bytes, "installed edit schema lacks preview")

    inspect_arguments = {
        "workspace_root": str(workspace),
        "requests": [
            {
                "id": "forms",
                "operation": "forms",
                "file": "src/demo.clj",
                "forms": ["alpha"],
                "expect": {"forms": 1},
            }
        ],
        "expect": {"requests": 1, "files": 1},
    }
    inspected, _ = client.post(
        "03-inspect", tool_request(3, "inspect_clojure", inspect_arguments)
    )
    descriptor = structured(inspected).get("prepared_confirmation") or {}
    digest = descriptor.get("descriptor_sha256")
    require(
        isinstance(digest, str) and len(digest) == 64,
        "eligible installed read served no prepared confirmation digest",
    )
    require(descriptor.get("executable") is False, "descriptor became executable")
    require(descriptor.get("write_authority") is False, "descriptor gained authority")
    require(source_file.read_text() == ORIGINAL, "inspect changed source")

    compact = {
        "confirm": digest,
        "fill": {"arguments.edits[0].to": "(def alpha :new)"},
    }
    previewed, _ = client.post(
        "04-preview", tool_request(4, "edit_clojure", {**compact, "preview": True})
    )
    preview = structured(previewed)
    require(preview.get("operation") == "edit_clojure-preview", "preview route missed")
    require(preview.get("source_unchanged") is True, "preview did not report unchanged")
    require(preview.get("mutation_attempted") is False, "preview attempted mutation")
    require("(def alpha :old)" in preview.get("diff", ""), "preview omitted old form")
    require("(def alpha :new)" in preview.get("diff", ""), "preview omitted new form")
    require(source_file.read_text() == ORIGINAL, "preview changed source")

    committed, _ = client.post(
        "05-commit", tool_request(5, "edit_clojure", compact)
    )
    commit = structured(committed)
    require(commit.get("ok") is True, "prepared commit was not ok")
    require(commit.get("committed") is True, "prepared commit did not commit")
    require(
        commit.get("verification_complete") is True,
        "prepared commit did not complete verification",
    )
    require(source_file.read_text() == COMMITTED, "prepared commit source mismatch")

    replayed, _ = client.post(
        "06-replay", tool_request(6, "edit_clojure", compact)
    )
    replay = structured(replayed)
    require(
        replay.get("error_type") == "prepared-confirmation-consumed",
        "consumed replay did not refuse with the typed error",
    )
    require(replay.get("source_unchanged") is True, "replay did not preserve source")
    require(source_file.read_text() == COMMITTED, "replay changed committed source")

    report = {
        "ok": True,
        "url": args.url,
        "workspace_root": str(workspace),
        "session_id_sha256": sha256(client.session_id.encode()),
        "installed_schema": {"confirm": True, "preview": True},
        "descriptor": {
            "served": True,
            "digest": digest,
            "executable": descriptor.get("executable"),
            "write_authority": descriptor.get("write_authority"),
        },
        "preview": {
            "operation": preview.get("operation"),
            "source_unchanged": source_file.read_text() == COMMITTED
            and preview.get("source_unchanged") is True,
            "diff_sha256": sha256(preview.get("diff", "").encode()),
        },
        "commit": {
            "ok": commit.get("ok"),
            "committed": commit.get("committed"),
            "verification_complete": commit.get("verification_complete"),
            "read_back_hash": (commit.get("read_back_hashes") or {}).get(
                "src/demo.clj"
            ),
        },
        "replay": {
            "ok": replay.get("ok"),
            "error_type": replay.get("error_type"),
            "source_unchanged": replay.get("source_unchanged"),
        },
        "source": {
            "original_sha256": sha256(ORIGINAL.encode()),
            "committed_sha256": sha256(COMMITTED.encode()),
            "actual_sha256": sha256(source_file.read_bytes()),
        },
    }
    (result_dir / "report.json").write_bytes(encoded(report) + b"\n")
    print(json.dumps(report, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
