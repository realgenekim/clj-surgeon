"""Standalone full-Make/manifest agreement witness; never run by the battery.

Invoke: python3 -B test/regns_seed_witness.py --root "$PWD" --scratch DIR
Last recorded run: Round 8, full Make 33.893 s / manifest 11.038 s, exits 0,
identical first failure block. Receipt:
docs/observations/2026-09-14-register-test-ns-round8/alone-seed.json
Report: docs/observations/2026-09-14-register-test-ns-report-astra.md#round-8
"""
import argparse
import json
from pathlib import Path
import tempfile

from regns_gate_matrix import (
    MANIFEST_ARGV, assert_unchanged, copy_repository, first_failure_block,
    live_snapshot, prepare_environment, run, state_root,
)
import os


def witness(source, scratch):
    source = source.resolve()
    state = state_root(os.environ)
    before = live_snapshot(source, state)
    try:
        with tempfile.TemporaryDirectory(prefix="regns-seed-witness-", dir=scratch) as tmp:
            owner = Path(tmp)
            root = owner / "repo"
            copy_repository(source, root)
            env = prepare_environment(owner / "runtime")
            setup = ["bb", "-Djava.io.tmpdir=" + env["TMPDIR"], "-m",
                     "clj-surgeon.registration-gate-fixture", str(root), "seed"]
            code, _ = run(setup, root, env, owner / "setup.log")
            assert code == 0, (owner / "setup.log").read_text()
            log = owner / "manifest.log"
            code, wall = run(MANIFEST_ARGV, root, env, log, mask=0)
            full_argv = ["make", "-C", str(root), "test-fast"]
            full_log = owner / "full-fast.log"
            full_code, full_wall = run(full_argv, root, env, full_log, mask=0)
            agreement = (full_code == code == 0
                         and first_failure_block(full_log.read_text())
                         == first_failure_block(log.read_text()))
            print("SEED-ENTRANCE-AGREEMENT", json.dumps({
                "mask": 0, "argv": full_argv, "exit": full_code,
                "full_wall_s": round(full_wall, 3), "runner_wall_s": round(wall, 3),
                "first_failure_block": first_failure_block(log.read_text()),
                "agreement": agreement}), flush=True)
            assert agreement, full_log.read_text()
    finally:
        assert_unchanged(before, live_snapshot(source, state))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", required=True)
    parser.add_argument("--scratch", required=True)
    args = parser.parse_args()
    witness(Path(args.root), Path(args.scratch).resolve())
