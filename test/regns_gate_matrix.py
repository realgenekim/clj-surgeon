"""REAL-GATE-001: actual copied files and the coordinator's exact lane argv.

The single registered seed executes fresh controls. Each independent mask restores
seed bytes before removing surfaces. No runner source is rewritten except the
literal count pin that is itself surface 3. No test-body eval or Var replacement.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time


def run_matrix(scratch):
    started = time.monotonic()
    source = Path.cwd().resolve()
    with tempfile.TemporaryDirectory(prefix="regns-real-gate-", dir=scratch) as tmp:
        root = Path(tmp) / "repo"
        shutil.copytree(source, root, ignore=shutil.ignore_patterns(
            ".git", ".cpcache", "target", "node_modules", ".clj-kondo", ".lsp"))
        env = dict(os.environ)
        env.pop("CLJ_SURGEON_TMPDIR_REEXEC", None)
        env["TMPDIR"] = str(Path(tmp) / "temp")
        Path(env["TMPDIR"]).mkdir()
        env["JAVA_TOOL_OPTIONS"] = "-Djava.io.tmpdir=" + env["TMPDIR"]

        def run(argv, log):
            tick = time.monotonic()
            with log.open("w") as output:
                result = subprocess.run(argv, cwd=root, env=env, stdout=output,
                                        stderr=subprocess.STDOUT, timeout=180)
            return result.returncode, time.monotonic() - tick

        setup = ["bb", "-Djava.io.tmpdir=" + env["TMPDIR"], "-m",
                 "clj-surgeon.registration-gate-fixture"]
        code, _ = run(setup + ["seed", "seed.json"], Path(tmp) / "seed.log")
        assert code == 0, (Path(tmp) / "seed.log").read_text()
        config = json.loads((root / "seed.json").read_text())
        saved = {p: (root / p).read_bytes() if (root / p).exists() else None
                 for p in config["files"]}
        receipts = []
        for mask in [0] + list(range(1, 32)):
            for path, data in saved.items():
                if data is None:
                    (root / path).unlink(missing_ok=True)
                else:
                    (root / path).write_bytes(data)
            code, _ = run(setup + [str(mask)], Path(tmp) / "setup.log")
            assert code == 0, (Path(tmp) / "setup.log").read_text()
            log = Path(tmp) / f"mask-{mask}.log"
            code, wall = run(config["argv"], log)
            output = log.read_text()
            names = re.findall(r"Registration checklist for ([^: ]+):", output)
            expected = "clj-surgeon.sol-first-contact-test"
            if mask == 0:
                okay = code == 0 and not names
            elif mask == 4:
                # A pin-only defect has no disk-minus-manifest subject.
                okay = code != 0 and not names and "Repository registration count" in output
            else:
                remedy = f"Remedy: make register-test-ns NS='{expected}' LANE='battery' RUNTIME='jvm'"
                okay = (code != 0 and bool(names) and set(names) == {expected}
                        and remedy in output.split("Registration checklist for ", 1)[1].splitlines()[0])
            row = {"mask": mask, "exit": code, "wall_s": round(wall, 3),
                   "first": names[0] if names else None, "okay": okay}
            receipts.append(row)
            print("REAL-FIRST-CONTACT", json.dumps(row), flush=True)
            if not okay:
                print(output, flush=True)
                raise AssertionError(row)
        print("REAL-FIRST-CONTACT-WALL", round(time.monotonic() - started, 3), flush=True)
        return receipts


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--scratch", required=True)
    args = parser.parse_args()
    run_matrix(Path(args.scratch).resolve())
