#!/usr/bin/python3
"""Deterministic JSONL app-server double for lifecycle contract tests."""

import json
import os
import subprocess
import sys
import time
import uuid


MODEL = "gpt-5.3-codex-spark"
MODE = os.path.basename(sys.argv[0])
rate_reads = 0


if sys.argv[1:] == ["--version"]:
    print("codex-cli 0.149.1")
    raise SystemExit(0)


def send(message):
    sys.stdout.write(json.dumps(message, separators=(",", ":")) + "\n")
    sys.stdout.flush()


def response(request_id, result):
    send({"id": request_id, "result": result})


for raw_line in sys.stdin:
    message = json.loads(raw_line)
    method = message.get("method")
    request_id = message.get("id")
    has_params = "params" in message and isinstance(message["params"], dict)
    params = message.get("params") or {}

    if request_id is None:
        continue
    if method == "initialize":
        if "slow-boot" in MODE:
            time.sleep(0.25)
        response(request_id, {"userAgent": "fake-app-server/0.149.1"})
    elif method == "account/read":
        response(
            request_id,
            {
                "account": {
                    "type": "chatgpt",
                    "planType": "pro",
                    "email": "fake@example.invalid",
                }
            },
        )
    elif method == "account/rateLimits/read":
        if not has_params:
            send(
                {
                    "id": request_id,
                    "error": {"code": -32600, "message": "missing params object"},
                }
            )
            continue
        rate_reads += 1
        used_percent = 9 if "meter-backward" in MODE and rate_reads == 4 else 10
        response(
            request_id,
            {
                "rateLimitsByLimitId": {
                    "codex_bengalfox": {
                        "limitId": "codex_bengalfox",
                        "limitName": "GPT-5.3-Codex-Spark",
                        "primary": {
                            "usedPercent": used_percent,
                            "windowDurationMins": 300,
                            "resetsAt": 2000000000,
                        },
                        "secondary": {
                            "usedPercent": 5,
                            "windowDurationMins": 10080,
                            "resetsAt": 2000600000,
                        },
                        "email": "must-not-persist@example.invalid",
                        "accessToken": "must-not-persist",
                        "nested": {"token": "must-not-persist"},
                    }
                }
            },
        )
    elif method == "account/usage/read":
        response(request_id, {"usage": None})
    elif method == "model/list":
        if not has_params:
            send(
                {
                    "id": request_id,
                    "error": {"code": -32600, "message": "missing params object"},
                }
            )
            continue
        slug = (
            "gpt-5.3-codex"
            if "missing-model" in MODE or "metadata-model" in MODE
            else MODEL
        )
        row = {"model": slug, "id": slug, "displayName": "Fake model"}
        if "metadata-model" in MODE:
            row["displayName"] = MODEL
        response(request_id, {"data": [row]})
    elif method == "thread/start":
        response(
            request_id,
            {
                "model": MODEL,
                "modelProvider": "other" if "other-provider" in MODE else "openai",
                "thread": {"id": "thread-" + uuid.uuid4().hex},
            },
        )
    elif method == "turn/start":
        turn_id = "turn-" + uuid.uuid4().hex
        thread_id = params["threadId"]
        prompt = params["input"][0]["text"]
        replacement = "ready" if prompt.startswith("Return exactly") else ":complete"
        candidate = json.dumps({"replacement": replacement}, separators=(",", ":"))
        response(request_id, {"turn": {"id": turn_id}})
        if "crash-descendant" in MODE and not prompt.startswith("Return exactly"):
            subprocess.Popen(["/bin/sleep", "30"])
            os._exit(17)
        if "oversize-stall" in MODE and not prompt.startswith("Return exactly"):
            send(
                {
                    "method": "item/agentMessage/delta",
                    "params": {
                        "threadId": thread_id,
                        "turnId": turn_id,
                        "delta": "x" * 33000,
                    },
                }
            )
            time.sleep(5)
        if "reroute" in MODE and not prompt.startswith("Return exactly"):
            send(
                {
                    "method": "model/rerouted",
                    "params": {"threadId": thread_id, "turnId": turn_id},
                }
            )
        if "action" in MODE and not prompt.startswith("Return exactly"):
            send(
                {
                    "method": "item/started",
                    "params": {
                        "threadId": thread_id,
                        "turnId": turn_id,
                        "item": {"type": "commandExecution", "id": "hostile"},
                    },
                }
            )
        for item in (
            {"type": "userMessage", "id": "user"},
            {"type": "reasoning", "id": "reasoning"},
            {
                "type": "agentMessage",
                "id": "agent",
                "phase": "final_answer",
                "text": candidate,
            },
        ):
            send(
                {
                    "method": "item/completed",
                    "params": {"threadId": thread_id, "turnId": turn_id, "item": item},
                }
            )
        send(
            {
                "method": "thread/tokenUsage/updated",
                "params": {
                    "threadId": thread_id,
                    "turnId": turn_id,
                    "tokenUsage": {
                        "last": {"inputTokens": 12, "outputTokens": 3}
                    },
                },
            }
        )
        send(
            {
                "method": "turn/completed",
                "params": {
                    "threadId": thread_id,
                    "turn": {"id": turn_id, "status": "completed"},
                },
            }
        )
    elif method == "turn/interrupt":
        response(request_id, {"accepted": True})
    else:
        response(request_id, {"error": {"message": "unsupported method"}})
