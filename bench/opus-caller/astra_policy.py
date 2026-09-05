#!/usr/bin/env python3
"""astra_policy.py — Astra's REVIEWED policy, imported and CALLED, not re-implemented.

The lead's ruling for round two: prefer his shared meter/lifecycle policies over a
duplicate runner.  So this module loads his adapter

    /var/tmp/forge/astra-program/repo/bench/astra/adapter.py
    sha256 8f6c909ffe25836a3599a2eec45f5da5a35d3fdc94541356c4778b440372b449

as a module (its `main()` never runs), verifies that sha BEFORE importing it, and
re-exports the predicates the Opus flank needs unchanged:

    digest, file_digest      the hashing his receipts use
    snapshot                 the protected-tree inventory (and its symlink refusal)
    pid_listens              "this pid owns that listener", read from /proc
    validate_ready           the whole server-ready attestation: healthz bound to the
                             MCP port, expected url/sha/project_root, a positive int
                             pid, healthz bytes hashed, and the functional-readiness
                             fields (ok, server, tool_runtime, tool_registry)

THE ONE SUBSTITUTION, and it is deliberate and narrow.  His `validate_url` requires
ports 8300-8339, his cohort's band.  This flank is forbidden to contact that band at
all, so it runs on 8340-8379.  We therefore rebind ONLY the module-global
`validate_url` to a band-substituted copy of his own predicate -- same scheme, same
loopback host, same rejection of credentials/query/fragment, different band -- before
calling `validate_ready`.  Nothing on disk is modified; the rebinding lives in this
process.  Every other clause of his attestation runs exactly as he wrote it, and the
receipt records both his adapter's sha and the substituted band so a reader can see
precisely what was and was not his.

CLI:  astra_policy.py validate-ready --ready F --url U --server-sha S --worktree W
      exit 0 and print the validated ready record, or exit 2 and print the refusal.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
import pathlib
import sys
from urllib.parse import urlsplit
from urllib.request import urlopen

ADAPTER = pathlib.Path(os.environ.get(
    "ASTRA_ADAPTER", "/var/tmp/forge/astra-program/repo/bench/astra/adapter.py"))
ADAPTER_SHA = os.environ.get(
    "ASTRA_ADAPTER_SHA256",
    "8f6c909ffe25836a3599a2eec45f5da5a35d3fdc94541356c4778b440372b449")
# this flank's own band; never 8300-8339, never 7888/7890/7894/7895/8171/8173-8175
PORT_LO = int(os.environ.get("OPUS_PORT_LO", "8340"))
PORT_HI = int(os.environ.get("OPUS_PORT_HI", "8379"))


class PolicyError(RuntimeError):
    pass


def load_adapter():
    """Import his adapter only after proving it is the frozen bytes."""
    if not ADAPTER.is_file():
        raise PolicyError(f"astra adapter missing: {ADAPTER}")
    actual = hashlib.sha256(ADAPTER.read_bytes()).hexdigest()
    if actual != ADAPTER_SHA:
        raise PolicyError(f"astra adapter sha mismatch: {ADAPTER} is {actual}, "
                          f"pinned {ADAPTER_SHA}")
    spec = importlib.util.spec_from_file_location("astra_adapter", ADAPTER)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)          # defines only; main() is guarded
    module.validate_url = band_validator     # THE ONE SUBSTITUTION (see docstring)
    return module, actual


def band_validator(url):
    """His validate_url, with this flank's band substituted for his."""
    parsed = urlsplit(url)
    if (parsed.scheme != "http" or parsed.hostname != "127.0.0.1"
            or not parsed.port or not PORT_LO <= parsed.port <= PORT_HI
            or parsed.username or parsed.password or parsed.query or parsed.fragment):
        raise ValueError(f"require isolated loopback HTTP URL on ports {PORT_LO}-{PORT_HI}")


def validate_ready_file(ready_path, url, server_sha, worktree):
    """His validate_ready plus his two /proc cross-checks, on our ready file."""
    module, adapter_sha = load_adapter()
    ready = json.loads(pathlib.Path(ready_path).read_text())
    band_validator(url)
    band_validator(ready["healthz_url"])
    with urlopen(ready["healthz_url"], timeout=5) as response:
        health = response.read(1024 * 1024)
    module.validate_ready(ready, url, server_sha, pathlib.Path(worktree), health)
    server_cwd = pathlib.Path(ready["server_cwd"]).resolve()
    if pathlib.Path(f'/proc/{ready["port_pid"]}/cwd').resolve() != server_cwd:
        raise ValueError("server PID cwd mismatch")
    if not module.pid_listens(ready["port_pid"], urlsplit(url).port):
        raise ValueError("server PID does not own MCP listener")
    return {"ready": ready, "healthz_sha256": module.digest(health),
            "astra_adapter": str(ADAPTER), "astra_adapter_sha256": adapter_sha,
            "band_substituted": f"{PORT_LO}-{PORT_HI}",
            "policy_source": "astra adapter.validate_ready + pid_listens, called directly"}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["validate-ready", "adapter-sha"])
    for key in ("ready", "url", "server-sha", "worktree"):
        parser.add_argument("--" + key)
    args = parser.parse_args()
    try:
        if args.action == "adapter-sha":
            _, sha = load_adapter()
            print(sha)
            return 0
        missing = [k for k in ("ready", "url", "server_sha", "worktree")
                   if getattr(args, k) is None]
        if missing:
            raise PolicyError("validate-ready requires " + ", ".join(missing))
        print(json.dumps(validate_ready_file(args.ready, args.url,
                                             args.server_sha, args.worktree),
                         indent=2, sort_keys=True))
        return 0
    except (PolicyError, ValueError, OSError, KeyError, json.JSONDecodeError) as error:
        print(f"ASTRA-POLICY REFUSED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
