#!/usr/bin/env python3
"""Serialize one analyzer and transfer the OS lock across exec.

The caller supplies one absolute analyzer executable after ``--``.  This
wrapper acquires a per-user record lock, writes bounded owner evidence, makes
the lock descriptor inheritable, and replaces itself with the analyzer.  The
analyzer therefore owns the lock until it exits, even if its original JVM or
agent caller dies.
"""

import argparse
import errno
import fcntl
import json
import os
import shutil
import sys
import time
from datetime import datetime, timezone
from types import SimpleNamespace


TEMPORARY_FAILURE = 75
MAX_TIMEOUT_MS = 30 * 60 * 1000
MAX_OWNER_BYTES = 4096
MAX_STATUS_BYTES = 32768
DEFAULT_MAX_NORMALIZED_LOAD = 4.0


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--lock", required=True)
    parser.add_argument("--timeout-ms", required=True, type=int)
    parser.add_argument("--entrance", required=True)
    parser.add_argument("--evidence")
    parser.add_argument("--events")
    parser.add_argument("--pressure-status")
    parser.add_argument(
        "--max-normalized-load", type=float, default=DEFAULT_MAX_NORMALIZED_LOAD
    )
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if args.command[:1] == ["--"]:
        args.command = args.command[1:]
    if not args.command or not os.path.isabs(args.command[0]):
        parser.error("command must start with an absolute executable path")
    if not 1 <= args.timeout_ms <= MAX_TIMEOUT_MS:
        parser.error(f"timeout-ms must be in 1..{MAX_TIMEOUT_MS}")
    return args


def write_evidence(path, evidence):
    if not path:
        return
    stage = f"{path}.tmp.{os.getpid()}"
    with open(stage, "w", encoding="utf-8") as output:
        json.dump(evidence, output, sort_keys=True)
        output.write("\n")
        output.flush()
        os.fsync(output.fileno())
    os.replace(stage, path)


def event_path(args):
    return args.events or os.environ.get(
        "CLJ_SURGEON_CLJ_KONDO_EVENTS",
        os.path.expanduser("~/.local/state/clj-surgeon/clj-kondo-events.jsonl"),
    )


def append_event(args, event):
    path = event_path(args)
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    record = {**event, "recorded_at": datetime.now(timezone.utc).isoformat()}
    encoded = (json.dumps(record, sort_keys=True) + "\n").encode("utf-8")
    descriptor = os.open(path, os.O_APPEND | os.O_CREAT | os.O_WRONLY, 0o600)
    try:
        os.write(descriptor, encoded)
    finally:
        os.close(descriptor)


def pressure_status_path(args):
    return args.pressure_status or os.environ.get(
        "CLJ_SURGEON_PRESSURE_STATUS",
        os.path.expanduser(
            "~/.local/state/diagnose-skiff-cpu-memory/monitor/status.json"
        ),
    )


def read_pressure_status(args):
    try:
        with open(pressure_status_path(args), "r", encoding="utf-8") as source:
            status = json.loads(source.read(MAX_STATUS_BYTES))
        sample = status.get("last_sample") or {}
        sampled_at = datetime.fromisoformat(sample.get("as_of", ""))
        age_seconds = (datetime.now(timezone.utc) - sampled_at).total_seconds()
        if age_seconds <= 180 and sample.get("severity") in ("red", "critical"):
            return {
                "source": "flight-recorder",
                "severity": sample.get("severity"),
                "sample_age_seconds": round(age_seconds, 3),
                "normalized_one_minute_load": sample.get(
                    "normalized_one_minute_load"
                ),
            }
    except (OSError, ValueError, TypeError):
        pass

    logical_cpus = max(1, os.cpu_count() or 1)
    normalized_load = os.getloadavg()[0] / logical_cpus
    maximum = args.max_normalized_load
    if normalized_load >= maximum:
        return {
            "source": "current-load",
            "severity": "red",
            "normalized_one_minute_load": round(normalized_load, 3),
            "maximum_normalized_load": maximum,
        }
    return None


def command_shape(command):
    arguments = command[1:]
    lint_targets = []
    if "--lint" in arguments:
        index = arguments.index("--lint") + 1
        while index < len(arguments) and not arguments[index].startswith("--"):
            lint_targets.append(arguments[index])
            index += 1
    return {
        "executable": command[0],
        "lint_target_count": len(lint_targets),
        "stdin": "-" in lint_targets,
        "broad_scope": any(target in (".", "src", "test") for target in lint_targets),
    }


def pressure_defer(args, started_ns, pressure):
    evidence = {
        "status": "pressure-deferred",
        "error_type": "clj-kondo-pressure-deferred",
        "waited_ms": (time.monotonic_ns() - started_ns) / 1_000_000,
        "cwd": os.path.realpath(os.getcwd()),
        "entrance": args.entrance,
        "pressure": pressure,
        **command_shape(args.command),
    }
    write_evidence(args.evidence, evidence)
    append_event(args, evidence)
    print(json.dumps(evidence, sort_keys=True), file=sys.stderr)
    return TEMPORARY_FAILURE


def shell_shim_args():
    executable = os.environ.get("CLJ_SURGEON_CLJ_KONDO_REAL")
    if not executable:
        own_path = os.path.realpath(sys.argv[0])
        for directory in os.environ.get("PATH", "").split(os.pathsep):
            candidate = os.path.join(directory, "clj-kondo")
            if os.path.isfile(candidate) and os.access(candidate, os.X_OK):
                if os.path.realpath(candidate) != own_path:
                    executable = os.path.realpath(candidate)
                    break
    if not executable:
        executable = shutil.which("clj-kondo")
    if not executable or os.path.realpath(executable) == os.path.realpath(sys.argv[0]):
        raise SystemExit("clj-kondo admission shim cannot resolve the real analyzer")
    args = SimpleNamespace(
        lock=os.environ.get(
            "CLJ_SURGEON_CLJ_KONDO_LOCK",
            os.path.expanduser("~/.local/state/clj-surgeon/clj-kondo.lock"),
        ),
        timeout_ms=int(os.environ.get("CLJ_SURGEON_CLJ_KONDO_TIMEOUT_MS", "120000")),
        entrance=os.environ.get("CLJ_SURGEON_CLJ_KONDO_ENTRANCE", "agent-shell"),
        evidence=None,
        events=os.environ.get("CLJ_SURGEON_CLJ_KONDO_EVENTS"),
        pressure_status=os.environ.get("CLJ_SURGEON_PRESSURE_STATUS"),
        max_normalized_load=float(
            os.environ.get(
                "CLJ_SURGEON_CLJ_KONDO_MAX_NORMALIZED_LOAD",
                str(DEFAULT_MAX_NORMALIZED_LOAD),
            )
        ),
        command=[os.path.realpath(executable), *sys.argv[1:]],
    )
    if not 1 <= args.timeout_ms <= MAX_TIMEOUT_MS:
        raise SystemExit(f"timeout-ms must be in 1..{MAX_TIMEOUT_MS}")
    return args


def read_owner(lock_file):
    try:
        lock_file.seek(0)
        return json.loads(lock_file.read(MAX_OWNER_BYTES) or "{}")
    except (OSError, ValueError):
        return {"status": "owner-evidence-unreadable"}


def main():
    args = shell_shim_args() if os.path.basename(sys.argv[0]) == "clj-kondo" else parse_args()
    os.makedirs(os.path.dirname(os.path.abspath(args.lock)), exist_ok=True)
    deadline = time.monotonic() + args.timeout_ms / 1000.0
    started_ns = time.monotonic_ns()
    pressure = read_pressure_status(args)
    if pressure:
        return pressure_defer(args, started_ns, pressure)
    with open(args.lock, "a+", encoding="utf-8") as lock_file:
        while True:
            try:
                fcntl.lockf(lock_file.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except OSError as error:
                if error.errno not in (errno.EACCES, errno.EAGAIN):
                    raise
                pressure = read_pressure_status(args)
                if pressure:
                    return pressure_defer(args, started_ns, pressure)
                if time.monotonic() >= deadline:
                    evidence = {
                        "status": "admission-timeout",
                        "error_type": "clj-kondo-admission-timeout",
                        "waited_ms": (time.monotonic_ns() - started_ns) / 1_000_000,
                        "owner": read_owner(lock_file),
                    }
                    write_evidence(args.evidence, evidence)
                    append_event(args, {**evidence, **command_shape(args.command)})
                    print(json.dumps(evidence, sort_keys=True), file=sys.stderr)
                    return TEMPORARY_FAILURE
                time.sleep(0.01)

        pressure = read_pressure_status(args)
        if pressure:
            return pressure_defer(args, started_ns, pressure)

        evidence = {
            "status": "admitted",
            "pid": os.getpid(),
            "cwd": os.path.realpath(os.getcwd()),
            "entrance": args.entrance,
            "executable": args.command[0],
            "waited_ms": (time.monotonic_ns() - started_ns) / 1_000_000,
            "acquired_monotonic_ns": time.monotonic_ns(),
            **command_shape(args.command),
        }
        lock_file.seek(0)
        lock_file.truncate()
        json.dump(evidence, lock_file, sort_keys=True)
        lock_file.write("\n")
        lock_file.flush()
        os.fsync(lock_file.fileno())
        write_evidence(args.evidence, evidence)
        append_event(args, evidence)

        os.set_inheritable(lock_file.fileno(), True)
        try:
            os.execve(args.command[0], args.command, os.environ)
        except OSError as error:
            failed = {
                **evidence,
                "status": "exec-failed",
                "error_type": "clj-kondo-exec-failed",
                "errno": error.errno,
            }
            write_evidence(args.evidence, failed)
            append_event(args, failed)
            print(json.dumps(failed, sort_keys=True), file=sys.stderr)
            return 126


if __name__ == "__main__":
    raise SystemExit(main())
