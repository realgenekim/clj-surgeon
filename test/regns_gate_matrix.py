"""REGNS-006/012: real Make gates in copies, observed from the caller tree."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time


# INTENT: REGNS-012
# @spec REGNS-012
def cell_environment(inherited, scratch):
    """Keep tool discovery, discard inherited checkout/state/write routing."""
    removed = {"MAKEFLAGS", "MFLAGS", "MAKELEVEL", "MAKEOVERRIDES", "GNUMAKEFLAGS",
               "MAKEFILES", "CENSUS_REGENERATE", "BATTERY_LEDGER_APPEND",
               "CLASSPATH", "CLJ_CONFIG", "CLJ_CACHE", "CLJ_JVM_OPTS", "CLJ_OPTS",
               "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS",
               "XDG_STATE_HOME", "PWD", "OLDPWD"}
    env = {k: v for k, v in inherited.items()
           if k not in removed and not k.startswith(("CLJ_SURGEON_", "GIT_", "BATTERY_"))}
    temp = scratch / "temp"
    state = scratch / "state"
    env.update(TMPDIR=str(temp), TMP=str(temp), TEMP=str(temp),
               CLJ_SURGEON_STATE_HOME=str(state),
               CLJ_SURGEON_ARTIFACT_ROOT=str(state),
               JAVA_TOOL_OPTIONS="-Xmx1024m -XX:-UsePerfData -Djava.io.tmpdir=" + str(temp))
    return env


def prepare_environment(scratch):
    env = cell_environment(os.environ, scratch)
    Path(env["TMPDIR"]).mkdir(parents=True)
    Path(env["CLJ_SURGEON_STATE_HOME"]).mkdir(parents=True)
    return env


def state_root(env):
    return Path(env.get("CLJ_SURGEON_STATE_HOME") or
                str(Path(env.get("XDG_STATE_HOME") or str(Path.home() / ".local/state")) /
                    "clj-surgeon")).resolve()


def live_snapshot(root, state):
    """Hash actual bytes, including absent/new files; git status is independent."""
    git = subprocess.run(["git", "-C", str(root), "status", "--porcelain"],
                         capture_output=True, text=True)
    # A copied fixture deliberately has no .git. Do not discover an ancestor repo.
    status = git.stdout if (root / ".git").exists() else None
    if (root / ".git").exists():
        assert git.returncode == 0, git.stderr
    files = list((root / "test").rglob("*"))
    files += list((root / "docs/observations/2026-09-12-bbtower-block-b/attempt22").rglob("*"))
    files += [root / "docs/observations" / name for name in
              ["battery-ledger.edn", "battery-namespace-walls.edn"]]
    hashes = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
              for p in files if p.is_file() and "__pycache__" not in p.parts}
    hashes.update({"STATE/" + str(p.relative_to(state)): hashlib.sha256(p.read_bytes()).hexdigest()
                   for p in state.rglob("*") if p.is_file()
                   and ("wall" in str(p.relative_to(state)) or "control" in str(p.relative_to(state)))})
    return {"status": status, "sha256": hashes}


def assert_unchanged(before, after):
    changed = sorted(k for k in before["sha256"].keys() | after["sha256"].keys()
                     if before["sha256"].get(k) != after["sha256"].get(k))
    assert before == after, {"live_changed": changed, "status_before": before["status"],
                             "status_after": after["status"]}


def copy_repository(source, root):
    shutil.copytree(source, root, ignore=shutil.ignore_patterns(
        ".git", ".cpcache", "target", "node_modules", ".clj-kondo", ".lsp", "__pycache__"))


def run(argv, root, env, log):
    tick = time.monotonic()
    with log.open("w") as output:
        result = subprocess.run(argv, cwd=root, env=env, stdout=output,
                                stderr=subprocess.STDOUT, timeout=180)
    return result.returncode, time.monotonic() - tick


# INTENT-TEST: REGNS-012
# @spec REGNS-012
def run_matrix(source, scratch):
    started = time.monotonic()
    source = source.resolve()
    caller_state = state_root(os.environ)
    before = live_snapshot(source, caller_state)
    try:
        with tempfile.TemporaryDirectory(prefix="regns-real-gate-", dir=scratch) as tmp:
            owner = Path(tmp)
            seed = owner / "seed"
            copy_repository(source, seed)
            env = prepare_environment(owner / "seed-runtime")
            setup = ["bb", "-Djava.io.tmpdir=" + env["TMPDIR"], "-m",
                     "clj-surgeon.registration-gate-fixture"]
            observer_env = prepare_environment(owner / "observer")
            observer = ["clojure", "-J-Xmx1024m", "-M:clj-surgeon/test-deps",
                        "-m", "clj-surgeon.mcp-test-runner", "--ns",
                        "clj-surgeon.lane-manifest-test"]
            # Run the live observer during actual seed registration. Direct mode
            # folds isolation/budget violations; --emit-edn only emits facts.
            with ThreadPoolExecutor(max_workers=1) as pool:
                live = pool.submit(run, observer, source, observer_env, owner / "live.log")
                code, _ = run(setup + [str(seed), "seed"], seed, env, owner / "seed.log")
                live_code, live_wall = live.result()
            assert code == 0, (owner / "seed.log").read_text()
            assert live_code == 0, (owner / "live.log").read_text()
            assert_unchanged(before, live_snapshot(source, caller_state))
            print("LIVE-CONCURRENT", json.dumps({"root": str(source), "argv": observer,
                  "phase": "seed-registration", "exit": live_code,
                  "wall_s": round(live_wall, 3), "live_unchanged": True}), flush=True)
            receipts = []
            for mask in [0] + list(range(1, 32)):
                with tempfile.TemporaryDirectory(prefix=f"cell-{mask}-", dir=owner) as cell:
                    cell = Path(cell)
                    root = cell / "repo"
                    copy_repository(seed, root)
                    env = prepare_environment(cell / "gate")
                    setup = ["bb", "-Djava.io.tmpdir=" + env["TMPDIR"], "-m",
                             "clj-surgeon.registration-gate-fixture", str(root), str(mask)]
                    code, _ = run(setup, root, env, cell / "setup.log")
                    assert code == 0, (cell / "setup.log").read_text()
                    log = cell / "gate.log"
                    argv = ["make", "-C", str(root), "test-fast"]
                    code, wall = run(argv, root, env, log)
                    output = log.read_text()
                    names = re.findall(r"Registration checklist for ([^: ]+):", output)
                    failure = re.search(r"FAIL in \(([^)]+)\).*?(?=\n\n|\Z)", output, re.S)
                    first_failure = failure.group(0) if failure else ""
                    walls = re.findall(r"(\d+) ms  clj-surgeon.lane-manifest-test", output)
                    lane_wall = int(walls[0]) if walls else None
                    expected = "clj-surgeon.sol-first-contact-test"
                    if mask == 0:
                        diagnostic_ok = code == 0 and not names
                    elif mask == 4:
                        diagnostic_ok = (code != 0 and not names
                                         and "Repository registration count" in first_failure)
                    else:
                        remedy = f"Remedy: make register-test-ns NS='{expected}' LANE='battery' RUNTIME='jvm'"
                        diagnostic_ok = (code != 0 and bool(names) and set(names) == {expected}
                                         and remedy in first_failure)
                    if mask:
                        diagnostic_ok = (diagnostic_ok and first_failure.count("Repository registration count:")
                                         == (1 if mask & 4 else 0))
                    budget_ok = lane_wall is not None and lane_wall < 8000
                    okay = diagnostic_ok and budget_ok and "ERROR in (" not in output
                    assert_unchanged(before, live_snapshot(source, caller_state))
                    row = {"mask": mask, "exit": code, "wall_s": round(wall, 3),
                           "root": str(root), "argv": argv, "lane_wall_ms": lane_wall,
                           "first_gate": failure.group(1) if failure else None,
                           "first": names[0] if names else None, "diagnostic_ok": diagnostic_ok,
                           "budget_ok": budget_ok, "live_unchanged": True, "okay": okay}
                    receipts.append(row)
                    print("REAL-FIRST-CONTACT", json.dumps(row), flush=True)
                    if not okay:
                        print("COPIED-GATE", str(root), output, flush=True)
                        raise AssertionError(row)
            print("REAL-FIRST-CONTACT-WALL", round(time.monotonic() - started, 3), flush=True)
            return receipts
    finally:
        assert_unchanged(before, live_snapshot(source, caller_state))
        print("LIVE-SNAPSHOT byte-identical", json.dumps({"root": str(source),
              "files": len(before["sha256"]), "state": str(caller_state)}), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True)
    parser.add_argument("--scratch", required=True)
    args = parser.parse_args()
    run_matrix(Path(args.root), Path(args.scratch).resolve())
