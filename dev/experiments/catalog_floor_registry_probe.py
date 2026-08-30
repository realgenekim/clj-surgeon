#!/usr/bin/env python3
"""Time Codex app-server initialization and MCP registry ingestion, token-free."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import select
import subprocess
import time


TIMEOUT_SECONDS = 30


def compact(value):
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


def send(process, message):
    process.stdin.write(json.dumps(message, separators=(",", ":")) + "\n")
    process.stdin.flush()


def read_message(process, deadline):
    remaining = deadline - time.monotonic()
    if remaining <= 0 or not select.select([process.stdout], [], [], remaining)[0]:
        raise TimeoutError("Codex app-server response timed out")
    line = process.stdout.readline()
    if not line:
        raise RuntimeError("Codex app-server closed stdout")
    return json.loads(line)


def request(process, request_id, method, params, deadline):
    send(process, {"id": request_id, "method": method, "params": params})
    while True:
        message = read_message(process, deadline)
        if message.get("id") == request_id:
            if "error" in message:
                raise RuntimeError(f"app-server {method} failed: {message['error']}")
            return message


def projection(selected):
    tools = selected.get("tools", {})
    return sorted(
        ({"name": name,
          "description": tool.get("description"),
          "input-schema": tool.get("inputSchema"),
          "output-schema": tool.get("outputSchema"),
          "annotations": tool.get("annotations")}
         for name, tool in tools.items()),
        key=lambda item: item["name"])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--codex", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--server", required=True)
    parser.add_argument("--expected-count", type=int, required=True)
    args = parser.parse_args()
    started_ns = time.monotonic_ns()
    process = subprocess.Popen(
        [args.codex, "app-server", "--stdio"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1,
        env=os.environ.copy())
    deadline = time.monotonic() + TIMEOUT_SECONDS
    receipt = {"schema": "clj-surgeon.catalog-floor-registry-probe.v1",
               "ok": False,
               "server": args.server,
               "expected_count": args.expected_count}
    try:
        request(process, 1, "initialize",
                {"clientInfo": {"name": "catalog-floor-probe",
                                "title": "catalog floor probe",
                                "version": "1"},
                 "capabilities": {"experimentalApi": True,
                                  "requestAttestation": False}},
                deadline)
        initialized_ns = time.monotonic_ns()
        send(process, {"method": "initialized"})
        request_id = 2
        attempts = 0
        while True:
            attempts += 1
            response = request(process, request_id, "mcpServerStatus/list",
                               {"detail": "toolsAndAuthOnly"}, deadline)
            request_id += 1
            servers = response.get("result", {}).get("data", [])
            matches = [server for server in servers
                       if server.get("name") == args.server]
            if args.expected_count == 0:
                ready = not matches
                selected = {"tools": {}}
            else:
                ready = (len(matches) == 1
                         and len(matches[0].get("tools", {})) == args.expected_count)
                selected = matches[0] if matches else {"tools": {}}
            if ready:
                break
            if attempts >= 60:
                raise RuntimeError("Codex did not ingest expected MCP catalog")
            time.sleep(0.25)
        ready_ns = time.monotonic_ns()
        visible = projection(selected)
        visible_json = compact(visible)
        receipt.update({
            "ok": True,
            "attempts": attempts,
            "tool_count": len(visible),
            "client_bytes": len(visible_json.encode()),
            "client_sha256": hashlib.sha256(visible_json.encode()).hexdigest(),
            "client_projection": visible,
            "process_to_initialize_ms": (initialized_ns - started_ns) / 1_000_000,
            "initialize_to_registry_ms": (ready_ns - initialized_ns) / 1_000_000,
            "process_to_registry_ms": (ready_ns - started_ns) / 1_000_000,
        })
    except Exception as error:
        receipt["error"] = {"class": type(error).__name__, "message": str(error)}
    finally:
        process.terminate()
        try:
            process.wait(timeout=2)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=2)
        receipt["process_exit_code"] = process.returncode
        receipt["stderr"] = process.stderr.read()
        args.output.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    if not receipt["ok"]:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
