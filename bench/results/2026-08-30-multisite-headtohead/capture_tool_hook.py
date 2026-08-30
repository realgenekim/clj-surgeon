#!/usr/bin/env python3
"""Append the exact Codex hook event to the episode's bounded receipt."""

from __future__ import annotations

import os
import sys
from pathlib import Path


def main() -> int:
    destination = os.environ.get("MULTISITE_HOOK_LOG")
    if not destination:
        return 2
    payload = sys.stdin.buffer.read()
    target = Path(destination)
    target.parent.mkdir(parents=True, exist_ok=True)
    descriptor = os.open(target, os.O_APPEND | os.O_CREAT | os.O_WRONLY, 0o600)
    try:
        os.write(descriptor, payload + b"\n")
        os.fsync(descriptor)
    finally:
        os.close(descriptor)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
