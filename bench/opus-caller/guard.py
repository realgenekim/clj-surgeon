#!/usr/bin/env python3
"""guard.py — the protected-tree inventory, taken with ASTRA'S OWN `snapshot`.

Round two hashed `test/` and `bin/fan-test` with find+sha256sum.  His review: that is
not equivalent to the shared helper.  `snapshot` records **bytes AND mode**, and it
**refuses a symlink anywhere in the protected tree** rather than following or ignoring
it.  A mode flip on `bin/fan-test`, or a symlink swapped in for a test file, is exactly
the kind of tampering the guard exists to catch, and find+sha256sum sees neither.

So the inventory is now his function, imported through astra_policy, and the selected
verified profile `.clj-surgeon.edn` is inside the protected set — it was outside it in
round two, which meant an arm could have rewritten its own verification profile.

  guard.py snapshot <worktree> <out.json>      take the inventory
  guard.py compare  <before.json> <after.json> exit 0 identical, 3 with the differences
"""
import json
import pathlib
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from astra_policy import load_adapter  # noqa: E402

PROTECTED = ["test", "bin/fan-test", ".clj-surgeon.edn"]


def take(worktree):
    module, sha = load_adapter()
    root = pathlib.Path(worktree)
    names = [n for n in PROTECTED if (root / n).exists()]
    missing = [n for n in PROTECTED if not (root / n).exists()]
    return {"inventory": module.snapshot(root, names),
            "names": names, "missing": missing,
            "helper": "astra adapter.snapshot (bytes+mode, symlink-refusing)",
            "astra_adapter_sha256": sha}


def main():
    try:
        if sys.argv[1] == "snapshot":
            pathlib.Path(sys.argv[3]).write_text(
                json.dumps(take(sys.argv[2]), indent=2, sort_keys=True) + "\n")
            return 0
        before = json.loads(pathlib.Path(sys.argv[2]).read_text())["inventory"]
        after = json.loads(pathlib.Path(sys.argv[3]).read_text())["inventory"]
        if before == after:
            print(f"GUARD ok {len(before)} protected files unchanged (bytes and mode)")
            return 0
        changed = sorted(set(before) ^ set(after)) + \
            sorted(k for k in set(before) & set(after) if before[k] != after[k])
        print(f"GUARD VIOLATED: {changed}", file=sys.stderr)
        return 3
    except Exception as error:                      # a guard that cannot look is a FAIL
        print(f"GUARD REFUSED: {error}", file=sys.stderr)
        return 3


if __name__ == "__main__":
    sys.exit(main())
