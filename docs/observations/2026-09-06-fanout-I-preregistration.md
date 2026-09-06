# Fan-out I — pre-registration, INFORMED BATCHED ROUTE vs NATIVE (written 16:44Z, before any I arm has run)

Gene's order (16:4xZ): "Go… Stay focused on wall improvements!… You have 2 hours to generate done wins". Meter: native WALL.

What is new vs cohort B (10:13–10:45Z): ONE named change — the tool arm is told the route. The I arm's dossier prefix mandates: helper + missing require natively first, then ONE apply_clojure_changes call shaped {workspace_root, edits:[{file, within:{form}, from "jdbc/execute!", to "<alias>/execute!", matches}]} — the exact shape Astra's prepared probe executed at 10:55Z (59 edits / 54 owners / 20 files, 2.413 s outer, proofs PASS). No deterministic verbs (symbol_migration / alias_migration / require_change) are permitted. inspect_clojure allowed for discovery. Same frozen seed (0eecb55a), owners.json (54/59/20), proof-b witness (19 tests), runner/detector, registration by command-line -c (servers 7906/8171 still on trunk 181c365c; recorded per row). Runner change: an 'I' arm kind added (dossier prefix + same registration argv as D); re-frozen; freeze output recorded in the result doc.

Difference from Astra's 10:55Z probe: the I actor must DISCOVER the 54 owners itself (charged), and construct the request itself (charged). His probe had the owners supplied. This is therefore a fresh-actor A/B, which his probe explicitly was not.

Arms: N = fresh gpt-6-astra medium (Codex CLI 0.153.3), native tools, scripts allowed, no MCP (unchanged from B). I = identical fresh session + MCP registered + INFORMED_PREFIX. Identical task text otherwise; I is charged MCP startup, discovery, request construction, refusals and repairs, and the proof.
Schedule: N1, I1, I2, N2, N3, I3, I4, N4 (same pair shape as B). Variance floor: cohort B's six controls (median 100.7 s, SD 17.07 s sample, 2SD floor 34.1 s) retained; no new controls (2-hour clock; drift noted as a limitation, not hidden). No retries, no pooling with B's D arms, no substituted models. Losses retained whatever the outcome.
Primary wall: per-arm seed copy → actor completion → external proof (gate 58/190 + witness 19/496) + scope audit. Correctness = proof pass AND audit true.
Hypothesis (falsifiable): I median wall < N median wall by more than the 2SD floor (34 s) with ≥ 3/4 I arms correct. Secondary: refusals per I arm ≤ 2. If I loses or refusals ≥ 5, the informed route does not transfer to a fresh actor at this size and the 10:55Z figure stays capability-only.
Pre-outcome expectations, written before launch:
- Fable: N 80–115 s (as B). I: discovery 20–40 s + one call ~3 s + proof ≈ 40–70 s if the request lands in ≤ 2 attempts; 120–200 s if the actor stumbles on the matches counts. I wins 3/4 pairs is my expectation; I do not expect a 10x, because discovery is the wall now, not typing.
- Astra: (his line, appended verbatim when posted; the cohort does not wait on it — Gene's clock).
Exclusions: no typist arm; no adoption claim (the route is mandated); no model-rank claim; the 38 execute-one! sites out of scope; no real DB behaviour proven.
