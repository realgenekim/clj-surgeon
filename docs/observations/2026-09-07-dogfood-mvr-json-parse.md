# Dogfood: a real safe refactor on the Marvin dictation tool through the installed Surgeon plate (2026-09-07 00:39–00:42Z; servers on trunk 2a2126d0; branch fable/json-parse-helper 9f9cf61 in marvin-voice-remote, pushed, NOT merged)

Task: one JSON parsing policy — new ns marvin-voice-remote.json/parse (read-str :key-fn keyword) and migrate the 9 inbound-body call sites in 8 namespaces. Class: known-intent fan-out (routed). Route as the plate says: helper + requires natively; ONE inspect_clojure match batch (wildcard pattern; owners + per-site source); ONE apply_clojure_changes with 9 within/from/to edits built from the served sites; the repo's own tests (kaocha).

| phase | at (s from start) | Δ from previous |
|---|---|---|
| orient-start | 0.0 | 0.0 |
| worktree | 26.6 | 26.6 |
| inspect-1 | 27.4 | 0.8 |
| native-helper | 47.4 | 20.0 |
| requires-complete | 75.4 | 28.0 |
| apply-1 | 77.7 | 2.3 |
| verify-start | 78.1 | 0.4 |
| verify-done | 151.8 | 73.7 |
| done | 153.3 | 1.5 |

Total wall orient-start → verify-done: 151.8 s; → done (commit+push): 153.3 s

Receipts: inspect 8 requests / 8 files / 9 matches, 0.70 s client (671 ms server); apply 9 edits / 8 files, 2.10 s, FIRST ATTEMPT, no refusal; receipt text (landed tonight): '✓ verification: none requested — bytes read back only'; kaocha: 579 tests, 7833 assertions, 0 failures. rc=0 . Files in docs/observations/2026-09-07-dogfood-mvr/.

Honest reading: this is ONE run with NO native arm, so it is a wall FIGURE, not a gain: 103 s from first orientation to green tests for a 9-site/8-file change plus a new namespace, of which the tool's own time was 2.8 s. What it dogfooded: the rebuilt server's landed receipts (the 'none requested' line), the routed read (owner_counts + per-site source in one call), the batched edit list built from served sites with zero refusals, and the plate's route as written. Paper cuts: none in the tool this run; my own regex for the require insertion missed two files whose data.json require sits on the (:require line (native-side friction, 30 s). Gain claim: NONE from this run; the routed class's earlier pairs (1.75x) plus this first-attempt success are consistent, n stays 4 pairs + 1 real task.
