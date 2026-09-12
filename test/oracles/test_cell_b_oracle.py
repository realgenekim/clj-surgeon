"""Cell B A8 regression: exercise the actual shell oracle's lint check."""
import os
import pathlib
import subprocess
import unittest

ORACLE = pathlib.Path(__file__).with_name("cell_b_oracle.sh")


def lint_check(output, rc=3, baseline="linting took 1ms, errors: 0, warnings: 0", baseline_rc=0):
    source = ORACLE.read_text()
    fragment = source.split('hdr "A8.')[1].split('# ---------------------------------------------------------------------------')[0]
    fragment = fragment[fragment.index('\n') + 1:]
    import tempfile
    import shlex
    with tempfile.TemporaryDirectory(dir=os.environ.get("TMPDIR") or "/var/tmp", prefix="b07-lint-") as tmp:
        root = pathlib.Path(tmp, "root")
        root.mkdir()
        subprocess.run(["git", "init", "-q", str(root)], check=True)
        (root / "source.clj").write_text("(ns example)\n")
        subprocess.run(["git", "-C", str(root), "add", "source.clj"], check=True)
        subprocess.run(["git", "-C", str(root), "-c", "user.name=B07 Witness", "-c", "user.email=b07@fixture.invalid", "commit", "-qm", "fixture"], check=True)
        base = subprocess.check_output(["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip()
        out = pathlib.Path(tmp, "out")
        out.mkdir()
        candidate_file = pathlib.Path(tmp, "candidate.txt")
        baseline_file = pathlib.Path(tmp, "baseline.txt")
        candidate_file.write_text(output + "\n")
        baseline_file.write_text(baseline + "\n")
        fake = pathlib.Path(tmp, "kondo")
        fake.write_text("#!/bin/sh\ncase \"$PWD\" in */a8-base.*) cat " + shlex.quote(str(baseline_file)) + "; exit " + str(baseline_rc) + ";; *) cat " + shlex.quote(str(candidate_file)) + "; exit " + str(rc) + ";; esac\n")
        fake.chmod(0o755)
        prefix = 'FAILS=0; fail(){ FAILS=$((FAILS+1)); }; pass(){ :; }; info(){ :; };\n'
        return subprocess.run(["bash", "-c", prefix + fragment + '\nexit "$FAILS"', str(ORACLE.resolve())],
                              cwd=root,
                              env={"KONDO": str(fake), "ROOT": str(root), "BASE": base,
                                   "TMP": str(out), "PATH": "/usr/bin:/bin"},
                              capture_output=True).returncode


# @spec NS-SPLIT-042
# INTENT-TEST: NS-SPLIT-042
# @spec NS-SPLIT-046
# INTENT-TEST: NS-SPLIT-046
class CellBLintCaseWitness(unittest.TestCase):
    def test_unresolved_all_cases_fail(self):
        for category in ["var", "namespace", "symbol"]:
            for word in ["unresolved", "Unresolved", "UNRESOLVED"]:
                with self.subTest(word=word, category=category):
                    self.assertNotEqual(0, lint_check(f"src/example.clj:2:4: error: {word} {category}: broken"))
        self.assertEqual(0, lint_check("linting took 1ms, errors: 0, warnings: 0", 0))

    def test_analyzer_failure_is_not_clean(self):
        self.assertNotEqual(0, lint_check("", 127))

    def test_baseline_is_exact_and_multiplicity_is_preserved(self):
        old = "src/example.clj:2:4: error: Unresolved symbol: old"
        moved = old.replace(":2:4:", ":20:7:")
        new = old.replace("symbol: old", "symbol: new")
        self.assertEqual(0, lint_check(moved, baseline=old, baseline_rc=3))
        self.assertNotEqual(0, lint_check(moved + "\n" + moved, baseline=old, baseline_rc=3))
        self.assertNotEqual(0, lint_check(new, baseline=old, baseline_rc=3))
        self.assertNotEqual(0, lint_check(old.replace("example.clj", "another.clj"), baseline=old, baseline_rc=3))
        self.assertNotEqual(0, lint_check("linting took 1ms, errors: 0", 0, baseline="", baseline_rc=127))


if __name__ == "__main__":
    unittest.main()
