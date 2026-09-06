# Cohort-J ethnography — served discovery (Opus reader, read-only, 17:2xZ; numbers from rollout events under /var/tmp/forge/cell-prep/runner-b/cohort-J/)

## Timeline (s from task_started)
| event | J1 | J2 | J3 | J4 |
|---|---|---|---|---|
| helper + require FileChange | 28.52 | 19.77 | 27.68 | 28.60 |
| rg -l | 28.57 | 19.81 | 34.12 | 28.64 |
| inspect_clojure done (server 231–277 ms) | 36.75 | 27.34 | 42.26 | 36.34 |
| apply_clojure_changes done (server 1.9–2.4 s, 1 attempt, 0 refusals) | 49.87 | 39.26 | 55.23 | 48.16 |
| final message | 54.31 | 42.72 | 59.68 | 52.44 |
Compose from the served list: 10.7 / 10.0 / 10.6 / 9.8 s, in-context, no script; all four produced the identical 54-entry / 59-match edits list the I arms produced. Inspect result: 24,563 chars for 767 chars of source. Tokens averaged 287,530 vs I's 220,253 (+31%).

## Why all four wrote (apply next.jdbc/execute! args)
Not the dossier (differs from I's on line 1 only; both carry "next.jdbc/execute!" twice in the task prose). Not tool text (inspect's `source` is the constant "jdbc/execute!" on all 59 matches). THE ROUTE: J's mandated order is patch the helper FIRST, then rg/inspect the same tree, then "build the edits list from that result". A helper spelled jdbc/execute! becomes a third match in db.clj with inside "execute!", and the mandated rule would rewrite the helper body to (apply execute! args) — self-recursion. Writing the qualified form hid the helper from its own scan (db.clj returned exactly 2 matches in every arm). A rational defence against the route's self-match hazard, scored as failure by the spelling-sensitive witness variadic-forwarder? (suite 495 pass / 1 fail; gate 58/0). The route author (Fable) owns this defect.

## Did served discovery remove the compose sink?
It flattened it: I's "patched tree → correct edits list" spread was 26–52 s (bimodal: hand-derive 19–20 s, or script 31–45 s); J's is 17.3–24.9 s (σ ≈ 3.7 s). The saving did not reach wall (J median 55.2 vs I 53.4): 7.3–14.3 s went to the extra rg -l the route needs to fill expect.requests/files, and 24.5 KB of inspect output entered context. Served discovery bought determinism and one extra round trip, not speed.

## Paper cuts (friction ledger)
1. inspect `source` is a constant echo of the caller's `match` — ~65% of the payload, zero information.
2. The result does not count per (file, inside); the server has the grouping; every arm re-tallied duplicates by hand (~10 s).
3. Results keyed m00…m19, not by file; the actor must hold its own ordering.
4. `expect {requests, files}` forces a separate rg -l pre-count (7–14 s) for a guard the server could compute.
5. Patch-then-scan self-match is unguarded: nothing flags a match inside a form named like the target, or an edit whose within.form is the new helper.
6. Terminality announced twice; the one derived field the route consumes (counts) omitted.
7. J3/J4 re-read pool_relaxed.clj (and one the skill file) for the ns/alias fact missing from the first read window.
