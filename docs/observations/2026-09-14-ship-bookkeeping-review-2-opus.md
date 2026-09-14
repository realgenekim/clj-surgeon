# Opus review round 2 of the ship bookkeeping branch (336e818a..f630957) — 2026-09-14

REVIEW: NO-GO the census allowlist and the `census-regenerate` rule are both read from the patched fix worktree, so the reviewer patch it constrains supplies its own permission — proved appending unreviewed, uncapped bytes to a protected file, blessed as `census=+1/-0`

All eight round-1 findings are closed. Subject `/var/tmp/forge/ship-v3.13b` @ `f630957`, clean tree, 3 commits on `v3.13-bookkeeping`, author `forge-anvil <forge-anvil@anvil>`. Isolated copy at `…/review2/subject`, `WITNESS_ROOT=…/review2`, `LEDGER_FIXTURE=true`.

| # | State | Evidence |
|---|---|---|
| 1 | closed | `git status --porcelain` empty; `91e243a` red → `0e3abdf` fix → `f630957` docs; `report.edn :commits` has both shas, both resolve. Independently re-ran `test/bookkeeping.py` at **91e243a**: **15 RED** (`AssertionError [...15 names]`); at HEAD **15 PASS**, original 12 retained → red-first is real, not asserted. |
| 2 | closed | `ship:1224 reviewer_tree=$(git write-tree)` before Make; diff taken **against that tree** (1229-1231); unexpected paths refuse at 1252; oracle rebuilt in `GIT_INDEX_FILE=$roundir/expected.index` from `reviewer_tree`+`census.patch` (1270-1274), never the live index. Path list in the event: `census_paths=…` (`ship:1424`). Witnesses `unexpected`/`unexpected-staged` → `SHIP-STOP reason=fix-census-touched-unexpected-paths seed`, rc 3. |
| 3 | closed | `ship:1206` requires `^census-regenerate:` in the Makefile **and** rejects `Nothing to be done`. `target-file` and `target-dir` impostors both → `fix-adds-deftest-without-census`, rc 3 (were `census=+0/-0` at 91e243a). |
| 4 | closed | Scanner replaced by a reader-aware tokenizer over complete postimages, added-lines only (`ship:1165-1199`). All four of my shapes + 3 new: `comment`, `string`, `discard`, `nested-discard`, `multiline-string`, `multiline-discard`, `header` (`test/(deftest core.clj`) → `census=+0/-0`, rc 0. All 7 were RED at 91e243a. |
| 5 | closed | `registry.edn:57` AUTOFIX-CENSUS-001 now carries `:boundaries [… my-deftest, deftest-with-fixtures and defspec are not detected …]` plus the `census-paths` semantics. Additive; no `:status` flipped. |
| 6 | closed | `ship:770` awk now `/^fable\/battery-receipt-[0-9a-f]{7,40}$/`. Witnessed: `astra/battery-receipt-append`, `fix/battery-receipt-format`, `fable/battery-receipts-archive` → `chosen=<name>`; only `fable/battery-receipt-abcdef0` → `excluded=fable/battery-receipt-abcdef0` in the `fix-no-branch` text (`ship:1136`). All four RED at 91e243a. |
| 7 | closed | `test/bookkeeping-autofix.sh` drives the **real `ship`** against real repos. My run: `AUTOFIX applied n=1 census=+1/-0 census_paths=test/clj_surgeon/deftest_census.edn added=1 deleted=0 cumulative=1 witnesses=2/2 new_tip=d02a5d4… branch=feat producer='Sol Fence…'` → `SHIP status=LANDED … auto_closes=1`. `FIX_CANDIDATES` scope and the `census=` field are now proven by execution, not by reading. |
| 8 | unchanged (INFO) | `bash test/bookkeeping.sh` standalone still needs `ROOT`; it now **also** needs `INSTALL_WITNESS_DEST` (the new fixture refuses `destination-unproven` without it) — `test/run.sh:4,11` supplies both. Fails closed. |

New findings

- **A. MED (fail-open, the chartered class) — `ship:1206,1217`.** The `^census-regenerate:` rule and `census-paths` are read from `$fixdir` **after** the reviewer patch is applied (`git apply` at `ship:1150`, `add -A` at 1157). A patch that adds both therefore writes its own permission slip. Reproduced (`…/review2/probe.py`): base repo has no `census-paths` and no Makefile; the patch adds a deftest, `census-paths` containing `ship-protected`, and a rule appending to it → `rc 0`, `census=+1/-0 paths=ship-protected`, file on disk `'original\nINJECTED\n'`. Those bytes bypass the fix line ceiling and the independent delta review, then land. Smallest repair: read both from the reviewed base — `git -C "$GIT_REPO" show "$CUR_TIP":census-paths` and `…:Makefile` — or refuse when the reviewer patch touches `census-paths` or `Makefile`.
- **B. MED (residual of #4) — `ship:1172`.** The detector parses *every* changed text file as Clojure. Reproduced (`probe2.py`): a prose-only patch adding ``Use `(deftest foo)` to declare a test.`` to `docs/note.md`, and `Run (deftest bar) in your editor.` to `README.md`, both → `SHIP-STOP reason=fix-adds-deftest-without-census`, rc 3. This is the hard stop #4 warned about, on patches v3.12 handled. Smallest repair: gate the loop on `name.endswith(('.clj','.cljc','.cljs'))`.
- **C. LOW — `ship:770`.** `{7,40}` is a GNU-awk interval. Verified `awk` here is gawk 5.3.2 and the interval binds correctly, but under mawk/busybox awk the brace is literal, nothing is excluded, and finding 6's original defect returns silently. Repair: `[0-9a-f][0-9a-f]*` or `--re-interval`/`gawk` explicitly.
- **D. INFO — `ship:1130`.** Excluded refs are named only on `fix-no-branch`; the `fix-branch-ambiguous` stop does not list them. `FIX_EXCLUDED` also contains `HEAD` (cosmetic).

Not weakened / green

- Zero refusals or `SHIP-STOP` lines removed across `336e818a..HEAD` — the single deleted line is the `fix-no-branch` echo, replaced by a superset. `registry.edn` deletions are `:misreadings` vectors replaced by supersets; three intents stay `:active`. `sha256sum -c MANIFEST.staged.sha256` → 50/50 OK. `bash -n` clean on `ship`, `receipt-chain`, `install.sh`, both test scripts.
- `bash test/measure.sh` → rc 0. `bash test/bookkeeping.sh` (with `ROOT`+`INSTALL_WITNESS_DEST`) → **29 PASS, 0 RED, 0 MISMATCH**, including the real-ship fixture. Production-ledger isolation asserted on every exit (`0 unrelated new events`).
- `install.sh` `FILES` already inventories `ship`, `receipt-chain`, `docs/intent/registry.edn`; the only diff is a comment. No new runtime file is introduced (the scanner is a heredoc inside `ship`), so the inventory is complete. `test/` uninstalled by design.
- `REPORT.md`/`report.edn` are honest: `:verdict :blocked`, `:commits` non-empty and accurate, `:least_sure` names the wrapper-macro and stub-gate limits, `:owed` names N1/circuits/battery. The `:exact_output` on finding 5 matches my independent run modulo shas.

Could not verify

- `bash test/run.sh` in the subject (seat suite running) — I ran the two brief-named witnesses on the copy instead. The builder's own `round2-suite.log` shows N1 `PermissionError` and `circuits.sh` `SCOPED CIRCUITS OWED`; both were already blocked in round 1 and are unrelated to this change.
- `test/install.sh`, `make landing-gate-prewarm`, `make test-battery`, and any real product-repository census run — outside this envelope; no installed-PROVEN claim is made or implied.
- Whether finding A is reachable through the live reviewer command in production; I proved it through the real census block with the real bytes, not through a live `SHIP_INDEPENDENT_REVIEWER_CMD`.
