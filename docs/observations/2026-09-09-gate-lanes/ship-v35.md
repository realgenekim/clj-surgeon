# ship/land v3.5 — consume only a COMPLETE prewarm (Sol GATE-LANES-FENCE-002)

**STAGED, NOT INSTALLED.** Nothing under `~/bin` was touched; `install.sh` was not run.
Staging root: `/var/tmp/forge/ship-v3.5/` (copied from the installed `/var/tmp/forge/ship-v3.4/`).

---

## Results table — every corpus run against the STAGED v3.5 bytes

| fixture | rows | mismatches | rc | log |
|---|---|---|---|---|
| **run-ship-v3.5 (new)** | **5** | **0** | 0 | `corpus/run-ship-v3.5.log` |
| run-ship-v3.4 | 14 | 0 | 0 | `corpus/run-ship-v3.4.log` |
| run-ship-v3.3 | 13 | 0 | 0 | `corpus/run-ship-v3.3.log` |
| run-ship-v3.2 (fixture Makefile extended) | 8 | 0 | 0 | `corpus/run-ship-v3.2.log` |
| run-ship-v3.1 | 13 | 0 | 0 | `corpus/run-ship-v3.1.log` |
| run-ship-v3 | 22 | 0 | 0 | `corpus/run-ship-v3.log` |
| run-ship-v2 | 28 | 0 | 0 | `corpus/run-ship-v2.log` |
| run-land-publication-truth | 12 | 0 | 0 | `corpus/run-land-publication-truth.log` |
| run-land-auto | 19 | 0 | 0 | `corpus/run-land-auto.log` |
| **total** | **134** | **0** | | `corpus/CORPUS-SUMMARY.txt` |

### RED witness — the same five rows against the INSTALLED v3.4 bytes

| row | v3.5 | v3.4 |
|---|---|---|
| 1 complete prewarm consumed, stage set from the tree | ok | **MISMATCH** — no mode, no stage fields at all |
| 2 prewarm that OMITTED a stage → rerun naming it | ok | **MISMATCH — `LAND-GATES consumed`** |
| 3 EXTRA stage → rerun naming it | ok | **MISMATCH — `LAND-GATES consumed`** |
| 4 tree without `print-gate-stages` → rerun `no-stage-manifest` | ok | **MISMATCH** |
| 5 re-sealed receipt with a shortened manifest → `gate-manifest-mismatch` | ok | (not reached; row 2 already consumed) |

`corpus/RED-witness-against-v3.4.log` (rc=1, 4/4 mismatches). **Row 2 is Sol's finding reproduced
verbatim**: a self-consistent receipt whose phase-A target list omits `alias-migration-test` is
CONSUMED by installed v3.4, and the alias battery ran nowhere in the landing path.

Caveat on the RED run: it pins `SHIP_GATES_PREWARM_CMD="make landing-gate-prewarm"` because v3.4 has
no mode probe and would otherwise reach for the installed `~/bin/suite-run` inside a fixture repo.
That pin makes row 4's red partly an artifact (v3.4's lane dies on a retired target rather than
falling back); rows 1–3 are clean reds.

---

## Hashes (staged bytes; every corpus row above ran against exactly these)

```
53644e5c9cc81a276de8fd465b39d8e4e50d1f6ba14a6b7b8812c2690d6c6aac  ship
95b2e3c27153810eac0de60808ae21151130d8aaee597788ac44c393be98fd02  land
955e95e2e97692506eaedef4bfdfbb9bd1cf0435edc74e0b62ffa13e1ecab487  install.sh
0c68aba96349fbcd35e5d7a4d6b5312f22d1c2ddec662ceb1eefbf69c99418af  fixtures/run-ship-v3.5.sh
33688838023c32adfe39ed8872e4dffe15fc1ef96c502b293771ae8abbb8ab87  fixtures/run-ship-v3.2.sh
```

Predecessors: `ship` 8e0833b7132b43ef… → 53644e5c9cc81a27…; `land` 4a1eae94a7e289b6… → 95b2e3c27153810e….
`fence-run`, `receipt-chain`, `land-auto`, `records-push`, `run-bg`, `sol-yolo`,
`ship-fix-block-spec.md` are **byte-identical to v3.4** — v3.5 changes two executables and two fixtures.

Patch script (auditable, refuses on any anchor that is not unique):
`/var/tmp/forge/ship-v3.5/patch-ship7.py`.

---

## What changed, against the contract

**1. ship: the REPOSITORY names the prewarm.** `GATES_PREWARM_CMD` is no longer a constant. Inside the
fast-lane block — after the candidate worktree exists, so the answer is a fact about *this tree* —
ship probes `make -n landing-gate-prewarm` and prints the mode it chose:

- `mode=prewarm-target` → `$HOME/bin/suite-run make landing-gate-prewarm`
- `mode=decomposed` → the old four-target list, with the line *"land will not consume without a stage manifest"*
- `mode=pinned` → `SHIP_GATES_PREWARM_CMD` set by the caller (fixtures, and the `''` disable switch,
  which is detected with `${VAR+1}` so an empty value stays a deliberate setting)

**2. `GATES_LANES` metadata is replaced by the actual stage list.** After phase A the receipt carries
`:expected-stages` (from `make -s print-gate-stages` on the candidate tree), `:ran-stages`,
`:ran-stages-source`, `:phase-b-stages`, `:stage-manifest-cmd`, `:prewarm-mode`. `:ran-stages` comes
from Astra's prewarm receipt (`target/landing-gate.edn`, `:stages` guarded by `:prewarm? true`) when
there is one, and from the command's own make targets when there is not — recorded either way, so a
reader can see *which*. `:lanes` is retained for old-style trees. ship also prints the comparison
itself (`stage set complete: …` / `STAGE SET does not match … missing=… extra=…`) and journals it.

**3. land consumes only a complete prewarm.** New checks in the elif chain, after the manifest hash:

- `no-stage-manifest` — `make -s print-gate-stages` on the MERGED tree printed nothing
- `stage-set-mismatch missing=<…> extra=<…>` — `(:ran-stages ∪ :phase-b-stages) ≠` the merged tree's list

Both are TYPED RERUNS, never refusals: the fallback is exactly today's full gate, and rows 2–4 assert
the landing still completes. The consumed path now also prints the stage set it proved and where the
set came from, so `LAND-GATES consumed` is auditable without opening the receipt.

**4. The manifest hash carries the expected stages.**
`sha256(recipe ‖ "--lanes--" ‖ lanes ‖ "--stages--" ‖ expected-stages)` on both sides. Row 5 witnesses
it: a receipt re-sealed with `:expected-stages "mcp-test"` (and a fresh digest, so the digest check
cannot be what catches it) reruns as `gate-manifest-mismatch`.

---

## The one place the brief is self-contradictory — read this before installing

The brief asks for both:

- rule 4: *"Trees without print-gate-stages: rerun with reason=no-stage-manifest (never consume by the
  old lane-list hash alone)"*, and
- fixture 4: *"old-style tree → old decomposed lane list still works (existing v3.2 rows)"*.

For **consumption** these cannot both hold: v3.2's rows 1, 2 and 4 assert `gates=consumed` on a
fixture repository that has no `print-gate-stages`. Measured, unpatched, against v3.5:
`corpus/run-ship-v3.2-UNPATCHED-fixture.log` — 3 mismatches, all `reason=no-stage-manifest`.

I resolved it the way the brief itself authorizes (*"stub them in fixtures"*): **the v3.2 fixture
repository gains Astra's two targets**, so its consumption rows keep asserting what they were written
to assert, and "the old decomposed lane list still works" is proved on the ship side — v3.5 row 4
shows an old-style tree still resolves `mode=decomposed`, still writes a receipt, still carries the
identical five-name `:lanes` string, and still lands. It just does not shortcut.

**Operational consequence, and it is not small:** until Astra's `print-gate-stages` is on MCP/main,
**every real landing will rerun its gates** (`reason=no-stage-manifest`). Consumption — the whole
wall saving of stage 2 — is OFF on today's trunk. That is fail-closed and it is exactly what Sol
asked for, but it should be a decision somebody makes on purpose, not a surprise on the first
landing. If the wall matters more than the fence for a day, `~/bin/land` without
`--consume-gate-receipt` is unchanged, and v3.4 remains installed until someone runs the installer.

Two further caveats:

- **The `:stages` reader is written against a contract Astra has not landed.** It expects
  `:stages ["a" "b"]` and `:prewarm? true` in `target/landing-gate.edn`. If Astra's shape differs,
  `:ran-stages-source` degrades to `command-targets` rather than failing — visible in the receipt and
  on the ship line, and land still fails closed on the set comparison. Re-check it against the real
  target before this is called proven on trunk.
- **GATE-LANES-FENCE-001 (box-wide worker admission) is untouched.** v3.5 answers 002 only.

---

## Install command (NOT RUN)

```bash
SRC=/var/tmp/forge/ship-v3.5 bash /var/tmp/forge/ship-v3.5/install.sh
```

It refuses while any target script is running or a ship lease names a live pid, backs every replaced
file up under `/var/tmp/forge/<name>.bak.<STAMP>`, installs by atomic rename, copies both shipped
fixtures into `/var/tmp/forge/tighten/fixtures` (backing up the replaced `run-ship-v3.2.sh`), and then
re-runs all nine fixtures against the INSTALLED bytes, exiting 3 on any mismatch.

Rollback: `cp /var/tmp/forge/ship.bak.<STAMP> ~/bin/ship` (same for `land`, `run-ship-v3.2.sh`).
