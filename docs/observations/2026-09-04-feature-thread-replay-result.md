# Replay result — Dequote/Format on social-media-writer @ 2df99c98 (2026-09-04 07:30–07:5xZ), counted from the codex rollout files

Two arms, same model (gpt-5.6-sol), same request text (Gene's three messages verbatim), same clone, same gate (`make runtests-unit` + `make test-js`), run concurrently on Anvil (load 9–16).
- N = native: no feature_thread. T1 = the same, with ONE feature_thread receipt (24,420 B: six legs with bodies/ranges/sha/anchors, verify row, governance anchors, next_call) placed in the prompt as "your first tool call's output".

| meter (from the rollout, hand-classified) | N native | T1 receipt | ratio |
|---|---|---|---|
| RAW tool calls, everything the harness recorded | 32 | 24 (+1 receipt = 25) | 1.3× |
| calls before the first patch | 13 | 6 (+1) | 2.2× |
| SOURCE READS before the first patch (net of CLAUDE.md/bd) | 10 (≈20 sed ranges, 5 rg) | 2 (rg selectionStart; sed 40-90p, both batched inside bd calls) | 5× |
| substantive calls, whole task (reads + patches + test runs; excl. CLAUDE.md, bd, .beads patches, stdin waits, git-status checks) | 21 | 9 (+1) | 2.1–2.3× |
| patches (real files) | 3 | 2 | — |
| suite runs | 4 | 4 | equal |
| repo ceremony (CLAUDE.md, bd prime/create/claim/close, .beads patches, status checks) | 8 | 12 | equal-ish; T1 spent MORE on beads tidy-up at the end |
| files changed | 9 (+1 untracked JS test) | 7 | both land on the six real sites |
| gates (agent's claim) | unit 227/712/0, once 934/3694/0, js exit 0 | same figures | independent re-run pending |

Pre-registered predictions and the honest scoring: N ≈ 15–20 → MISSED (32 raw / 21 net); T1 ≈ 3–5 → MISSED on raw (24) and on net (9), HIT on calls-before-write (6 raw / 2 net + the receipt). Withdrawal line as written ("if T1 needs ≥ 8 calls the receipt is not an edit basis") — by the letter T1 crosses it; by the mechanism it does not: the receipt removed 8 of 10 discovery reads, and every remaining T1 call is a test run, a patch, or the repo's own ceremony, none of which a read-side receipt can remove.
What T1 still read, and why: the selection precedent (openTransformFromSelection / selectionStart) and the test-suite classification (`:fast`) — both named as round-three gaps by the naive readers before this run. T1 wrote its handler AFTER handle-format (L682) rather than extending mechanical-format (L133 in Gene's real edit) — a design choice, both green.
What the 10× would need: (a) the round-three receipt (peer commands + request contract) to take the last two reads to zero; (b) a harness where the write is the admit gate (one call: verify + commit) instead of patch → 3–4 suite runs; (c) a repo whose CLAUDE.md does not mandate eight calls of ceremony per task — or counting them as the fixed cost they are. On THIS harness the honest sentence is: discovery 5× (10 reads → 2), whole task 2.1–2.3× net, 1.3× raw.
