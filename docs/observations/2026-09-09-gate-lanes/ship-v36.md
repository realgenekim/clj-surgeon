# ship v3.6 — STAGED, NOT INSTALLED: read the real prewarm receipt (Sol GATE-LANES-FENCE-002-R2)

Written 2026-09-09T07:38:16Z. Staged at `/var/tmp/forge/ship-v3.6/` from the installed v3.5. **Nothing under `~/bin` was
touched and `install.sh` was not run** — `~/bin/ship` still hashes `53644e5c9cc81a27…` = the v3.5 staged
bytes, and `~/bin/land` `95b2e3c27153810e…` = the v3.5 staged bytes.

## Headline

| arm | fixture | bytes under test | rows | mismatches |
|---|---|---|---|---|
| GREEN | run-ship-v3.6 (new) | **v3.6 staged** | 8 | **0** |
| GREEN | run-ship-v3.5, stub corrected to the producer's real shape | **v3.6 staged** | 5 | **0** |
| GREEN | whole corpus, 10 fixtures | **v3.6 staged** | **142** | **0** |
| **RED (the point)** | run-ship-v3.6 | installed **v3.5** | 8 | **7** |
| **RED (the point)** | run-ship-v3.5, corrected stub | installed **v3.5** | 5 | **1** — row 1, the consumption |

One line of learning: **the v3.5 fixture was green because its stub agreed with the code instead of with
the producer** — it printed a flat vector of strings into `target/landing-gate.edn`, which is neither the
shape nor the path `landing-gate-prewarm` writes. A fixture that mints its own input in the reader's own
image proves only that the reader is self-consistent.

One caveat: **no real `landing-gate-prewarm` receipt was retained** from ship run
`20260909T071227Z-1c762f73bf49` — its directory holds only the brief, the events log, the verdict and the
swept fence worktree. The real-shape receipt used in the direct proof below is *derived from the retained
FULL landing receipt of the same candidate*, `clj-surgeon-astra-consult/target/landing-gate.edn`,
`:git-head 1c762f73bf49db62a1996b2e2afd8b76badb8f8f`, by removing `battery-fresh` and setting
`:landing? false :prewarm? true` — exactly what `gate-stages` and `landing-eligible?` do in
`test/clj_surgeon/battery_parallel_runner.clj`. Its `:stages` maps are the producer's own bytes.

## What was wrong, and what v3.6 does

Sol GATE-LANES-FENCE-002-R2, both halves confirmed against the producer's source
(`battery_parallel_runner.clj` lines 699–701 and 726–756):

1. **The path.** `--prewarm true` writes `target/landing-gate-prewarm.edn` and **deletes**
   `target/landing-gate.edn`. v3.5 watched the landing path, which is empty *exactly when the prewarm
   succeeded*; it fell back to its command's own targets and reported `ran-stages=landing-gate-prewarm`.
2. **The shape.** `:stages` is a vector of **maps** `{:target ".." :exit n :wall-ms n}`. v3.5's
   `sed …:stages\[\([^]]*\)\]… | tr -d '":'` assumes a flat vector of strings; on the real receipt it
   would have manufactured stage names out of keywords and numbers.

v3.6:

- **`ship`** reads `target/landing-gate-prewarm.edn` (`SHIP_GATES_PREWARM_RECEIPT`), falling back to the
  landing path **only** for a receipt that passes the prewarm reader; requires `:prewarm? true` **and**
  `:landing? false`; parses with `bb -e` — a real EDN reader, already a gate-toolchain member whose
  version every receipt records — printing one `:target` per stage; and refuses unless `:stages` is a
  non-empty vector of maps with a string `:target` and **every `:exit` 0**. Reader exit codes: 0 ok,
  10 not-a-prewarm, anything else unparseable.
- Every rejection is a **typed source** — `not-a-prewarm`, `unparseable`, `edn-reader-unavailable` — and
  ship **never** falls back to command targets after finding a receipt it refused. The receipt gains
  `:ran-stages-receipt`, naming the file the stages actually came from.
- **`land`** turns a refused source into a **named** rerun (`ran_source_refused`) instead of letting it
  surface as `stage-set-mismatch`, which would read as "the prewarm ran the wrong stages" when the truth
  is "nobody could read what it ran". A receipt with no source field at all reports
  `ran-stages-source-missing`, never an empty `reason=`. The set comparison against
  `make -s print-gate-stages` on the merged tree is unchanged.
- **Consumption is still never authority**: every refusal above is a RERUN of the full gate on the
  merged tree, never a refusal to land.

## Direct proof against the producer's real bytes (not a fixture)

The reader string was extracted **from the staged `ship` itself** and run by hand:

| input | result |
|---|---|
| real-shape prewarm receipt for `1c762f73bf49` | exit 0; prints `admit-transaction-recovery-battery alias-migration-test mcp-test test-bb repository-hygiene intent-audit` — Sol's six, exactly |
| the retained **full landing** receipt, 242 KB, same candidate | exit **10** = `not-a-prewarm` |
| parsed set ∪ phase-B (`battery-fresh`) vs `gate-stage-manifest` | **identical**, `diff` empty |

Artifacts: `/var/tmp/forge/ship-v3.6/proof/` — `reader.clj`, `real-prewarm.edn`, `union.txt`, `manifest.txt`.

## The fixture rows

`fixtures/run-ship-v3.6.sh` — 8 rows, each a real `ship` run against a real bare origin and clone, with
the receipt written by `$FXBIN/mk-prewarm-receipt` whose shapes are copied from `run-gate!`:

| # | row | expected |
|---|---|---|
| 1 | producer's real receipt, real path, real shape | **consumed**; `source=prewarm-receipt`, `receipt=target/landing-gate-prewarm.edn`, ran-stages = the five stage targets, and not a map key or a number among them |
| 2 | one stage with `:exit 2` | rerun `reason=unparseable`, **no** stage claim |
| 3 | `:landing? true` at the prewarm path | rerun `reason=not-a-prewarm` |
| 4 | the v3.5 flat-string shape | rerun `reason=unparseable`, no stage claim |
| 5 | bytes that are not EDN at all | rerun `reason=unparseable` |
| 6 | full landing receipt at the landing path, no prewarm receipt | **not adopted**; `source=command-targets`, rerun `reason=stage-set-mismatch` |
| 7 | a real **prewarm** receipt at the legacy landing path | **consumed**, `receipt=target/landing-gate.edn` |
| 8 | `SHIP_GATES_EDN_BIN=/nonexistent/bb` | rerun `reason=edn-reader-unavailable` — a missing reader never degrades to a guess |

Rows 2–8 all still **LAND**: a rerun is the fallback, never a refusal.

`fixtures/run-ship-v3.5.sh` keeps its five stage-set rows unchanged; only its stub receipt moved to the
producer's real path and real shape. Against v3.5 bytes its row 1 now fails — the field defect, reproduced.

## Hashes (staged, `/var/tmp/forge/ship-v3.6/`)

```
88af948e2fc64f145ff2cc66a35d7934f842e5f6da6905f785620e907ea197b1  ship          (v3.5 was 53644e5c9cc81a27…)
8bd62ed821c631811b94b9e368d9e6b70a1613e96189f3e8266037755c2bb18f  land          (v3.5 was 95b2e3c27153810e…)
08ab84125ca22df7970d55a14dd5a63d3cf6beb32daf3b0a29ac6b507f5d3b89  install.sh
0939a6264359d7bada6c372a2487df000d8e68dd0f8d8389d6578760fae0ca1c  fixtures/run-ship-v3.5.sh
c746f6cdf5ae14939d4dd2c9b426718bd17513c6c176137b2f6e656a8c5ce84c  fixtures/run-ship-v3.6.sh
d254ce71cca9cc1f2e5ff49c1d4ec7eb85464b442d9ab76df135e09f7aa283ea  patch-ship8.py
```

Unchanged from v3.5 and carried through: `fence-run`, `receipt-chain`, `land-auto`, `records-push`,
`run-bg`, `sol-yolo`, `ship-fix-block-spec.md`.

## Install command (NOT RUN)

```bash
SRC=/var/tmp/forge/ship-v3.6 bash /var/tmp/forge/ship-v3.6/install.sh
```

It refuses while any target script is running or a ship lease names a live pid, backs every replaced file
up under `/var/tmp/forge/<name>.bak.<stamp>`, installs by atomic rename, and re-runs all ten fixtures
against the installed bytes. **`bb` must be on `PATH`** for a prewarm receipt to be consumed — it already
is, it is a gate lane and its version is in every receipt — and a box without it degrades to a typed
`edn-reader-unavailable` rerun. The v3.5 caveat still stands: until Astra's `print-gate-stages` and
`landing-gate-prewarm` are on MCP/main, land reruns on every real landing (`reason=no-stage-manifest`).

## Evidence paths

- corpus and RED logs: `/var/tmp/forge/ship-v3.6/corpus/` — `CORPUS-SUMMARY.txt`,
  `RED-run-ship-v3.6-against-v3.5.log`, `RED-run-ship-v3.5-against-v3.5.log`
- the patch, with its exact anchors: `/var/tmp/forge/ship-v3.6/patch-ship8.py`
- Sol's verdict: `/var/tmp/forge/ship/20260909T071227Z-1c762f73bf49/verdict-1.md`
