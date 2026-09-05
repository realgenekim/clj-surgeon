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

ROUND THREE.  His review was blunt about the limit of round two: a field-compatible
ready JSON is not lifecycle parity, and narrow helper imports must not be called
"policy parity".  Three checks his `prepare` does that round two omitted are added
here, and they are checks of *authorship and birth*, not of a port answering:

  * the server's OWN `ready.edn` is read and cross-checked against the launcher's
    ready.json.  A label the launcher wrote is not evidence about the server; the
    server's own file is.  Missing or disagreeing => refusal.
  * PID BIRTH: the pid + start-ticks + boot id recorded at spawn must still describe
    the live process.  A pid alone is not identity (pids are reused); start-ticks
    repeat across reboots, so the boot id travels with them.
  * SERVER CHECKOUT HEAD is read from the server's own cwd and compared to the
    expected sha -- his `prepare` does this and the round-two wrapper did not, so a
    server running the wrong source could have passed.

CLI:  astra_policy.py validate-ready --ready F --url U --server-sha S --worktree W
      astra_policy.py attest-server  --ready F --ready-edn F --spawned F --url U
                                     --server-sha S --worktree W
      exit 0 and print the validated record, or exit 2 and print the refusal.
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


def read_spawn_record(path):
    """pid, start-ticks, boot id -- the triple that proves we are looking at OUR server."""
    fields = pathlib.Path(path).read_text().split()
    if len(fields) < 3:
        raise ValueError(f"spawn record {path} is not 'pid start_ticks boot_id'")
    return fields[0], fields[1], fields[2]


def verify_birth(spawned_path, expected_pid):
    """The recorded birth must still describe the LIVE process wearing that pid."""
    pid, start_ticks, boot_id = read_spawn_record(spawned_path)
    if int(pid) != int(expected_pid):
        raise ValueError(f"ready.json pid {expected_pid} is not the spawned pid {pid}")
    live_boot = pathlib.Path("/proc/sys/kernel/random/boot_id").read_text().strip()
    if boot_id != live_boot:
        raise ValueError(f"spawn record is from another boot ({boot_id} != {live_boot}); "
                         "the recorded start time cannot describe any live process")
    stat = pathlib.Path(f"/proc/{pid}/stat").read_text()
    live_ticks = stat.split(")", 1)[1].split()[19]
    if live_ticks != start_ticks:
        raise ValueError(f"pid {pid} start-ticks {live_ticks} != recorded {start_ticks}; "
                         "this is NOT the process we spawned (pid reuse)")
    return {"pid": int(pid), "start_ticks": start_ticks, "boot_id": boot_id}


def verify_ready_edn(ready_edn_path, worktree, port):
    """The SERVER's own claim about itself, cross-checked against the launcher's."""
    path = pathlib.Path(ready_edn_path)
    if not path.is_file():
        raise ValueError(f"server wrote no ready file at {path} — a port that answers is "
                         "not the server declaring itself ready")
    text = path.read_text()
    if str(worktree) not in text:
        raise ValueError(f"server ready.edn does not name the arm worktree {worktree}")
    if str(port) not in text:
        raise ValueError(f"server ready.edn does not name port {port}")
    return {"path": str(path), "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "bytes": len(text)}


def verify_checkout(server_cwd, expected_sha):
    """His prepare() reads the server's ACTUAL HEAD.  Round two omitted this."""
    import subprocess
    actual = subprocess.run(["git", "-C", str(server_cwd), "rev-parse", "HEAD"],
                            capture_output=True, text=True)
    if actual.returncode != 0:
        raise ValueError(f"cannot read server checkout HEAD at {server_cwd}: "
                         f"{actual.stderr.strip()}")
    head = actual.stdout.strip()
    if head != expected_sha:
        raise ValueError(f"server checkout HEAD {head} != expected {expected_sha}")
    return head


def attest_server(ready_path, ready_edn, spawned, url, server_sha, worktree):
    """Everything validate_ready_file does, PLUS birth, the server's own ready file,
    and the actual checkout HEAD.  This is the complete lifecycle attestation."""
    record = validate_ready_file(ready_path, url, server_sha, worktree)
    ready = record["ready"]
    record["birth"] = verify_birth(spawned, ready["port_pid"])
    record["server_ready_edn"] = verify_ready_edn(ready_edn, worktree,
                                                  urlsplit(url).port)
    record["server_checkout_head"] = verify_checkout(ready["server_cwd"], server_sha)
    record["policy_source"] = ("astra adapter.validate_ready + pid_listens, called "
                               "directly; plus birth (pid+start-ticks+boot-id), the "
                               "server's own ready.edn, and prepare()'s checkout-HEAD "
                               "check")
    return record


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action",
                        choices=["validate-ready", "attest-server", "adapter-sha"])
    for key in ("ready", "ready-edn", "spawned", "url", "server-sha", "worktree"):
        parser.add_argument("--" + key)
    args = parser.parse_args()
    try:
        if args.action == "adapter-sha":
            _, sha = load_adapter()
            print(sha)
            return 0
        needed = ("ready", "url", "server_sha", "worktree")
        if args.action == "attest-server":
            needed += ("ready_edn", "spawned")
        missing = [k for k in needed if getattr(args, k) is None]
        if missing:
            raise PolicyError(args.action + " requires " + ", ".join(missing))
        if args.action == "attest-server":
            record = attest_server(args.ready, args.ready_edn, args.spawned,
                                   args.url, args.server_sha, args.worktree)
        else:
            record = validate_ready_file(args.ready, args.url,
                                         args.server_sha, args.worktree)
        print(json.dumps(record, indent=2, sort_keys=True))
        return 0
    except (PolicyError, ValueError, OSError, KeyError, json.JSONDecodeError) as error:
        print(f"ASTRA-POLICY REFUSED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
