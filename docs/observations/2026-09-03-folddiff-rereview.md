# fold-diff re-review at a765d859 — NO-GO as specified; GO-WITH-FIX after one mechanical blocker (2026-09-03T04:30Z)

Prior seven: 1 CLOSED (guard asserts count before the pool opens, receipt names 4 vars + 2 files, "NO FILE
DIGEST COVERS IT" under postgres); 2 CLOSED (MAX(seq), gap refusal exit 2, `ALLOW_GAP` labels UNEXAMINED);
3 CLOSED in design / BROKEN in implementation (below); 4 CLOSED; 5 CLOSED and MEASURED: 5,000 real events,
8.4 MB checkpoint → peak RSS 952 MB, 17.9 s at 4g, still passes at 160m; 6 PARTIAL (tool-side 3 on bad
env / OOM / unreachable DB, driver-side see 1); 7 PARTIAL (`cfpuser:p@ssw0rd@host` → tail of the password
survives; `a/b/c@` unredacted).

| # | sev | finding | fix |
|---|---|---|---|
| 1 | BLOCK | `make_worktree` copies only fold_diff.clj; the tool now needs `checkpoint/validate` (added on this branch) → `BASELINE_REF=origin/main` dies compiling in the baseline worktree and the driver exits 1 = "differences". The self-test runs both sides at the same ref so it cannot catch this class. No production harm (baseline runs with STORE_BACKEND unset). | copy `store_checkpoint.clj` too (not a fold source); remap emit failure to exit 3; a two-different-refs self-test |
| 2 | fix | `make` collapses exit codes (1→2, 3→2) | run `bin/fold-diff-checkpoint` directly; say so in Makefile + doc |
| 3 | clean | namespace trace under postgres: 6 cfp ns load, `db/ds-atom` nil, `store-pg/started?` false — no path to `ensure-schema!`; `db/start-pool!` the only opener; no root `:main-opts` at either ref | harden: add `db/migrate!`, `db/start!`, `store-pg/start!` (+ ensure-schema!, ensure-idempotency-index!) to the write guard |
| 4 | note | `secrets`/`data`/`cache` symlinked into BOTH worktrees (baseline never needs creds); cleanup verified on exit 0/1/2/3 and SIGINT/SIGTERM; the candidate ref's own deps.edn/namespaces run with production creds reachable — only point CANDIDATE_REF at refs you have read; worktree is 755 under world-writable /tmp | link secrets only into the candidate |
| 5 | clean | SQL parameterised; table name interpolated only from a compile-time constant validated against the checkpoint's `:table`; the two SELECTs remain unverified against a live server (first execution will be production; result keys match `store-pg/max-seq`) | — |
| 6 | fix | `DEPLOYED_REVISION` mismatch warns on stderr only; the pasted report carries no trace | in `render`'s header |
| 7 | note | with ALLOW_GAP and an identical prefix, exit 0 is indistinguishable from a gap-free pass (a script cannot see UNEXAMINED) | deliberate per FOLD-DIFF-005 |
| 8 | fix | the report prints unredacted production values (400 chars × 200 paths/relation) incl. `[:sessions <id>]` payloads | redact `sessions`, `api-keys` values by default |
| 9 | check | no hooks in this clone; `git worktree add` fires `post-checkout` — re-check on the skiff (`ls .git/hooks | grep -v sample; git config --get core.hooksPath`) | mayor |

Scope caveat for the mayor: `origin/main..a765d859` is 24 commits — the verdict covers the whole store-idempotency
stack; the fold-diff commits touch neither fold source. Round three launched on `~/src/curtaincall-cfp-folddiff`.
