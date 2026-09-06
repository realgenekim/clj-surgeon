# Cohort-I ethnography — 4 native vs 4 informed-route arms (Opus reader, read-only, 17:0xZ; every number from rollout events under /var/tmp/forge/cell-prep/runner-b/cohort-I/)

## Headline facts
- Zero refusals: 4/4 I arms had their single apply_clojure_changes call accepted first attempt; server-reported 2353 / 2169 / 2451 / 2049 ms.
- No I arm ran any verification — the receipt ("atomic commit complete · written bytes read back and verified · verification_complete=true · next action none") replaced it.
- N1, N2, N3 each wrote their own JVM forwarding proof (with-redefs jdbc/execute!) and each FAILED it first on their own seq/vector bug `(= args (last @calls))`, then re-ran: 21 s, 18 s, 32 s wasted. N4 did a byte-check only and was the fastest native arm (66.2 s).
- N3 (audited incorrect): its patch generator inserted the mdb require AT the `(:require` head, pushing `[maven.relaxed-search :as rx]` to a new line; the witness's textual delta check failed ("no seed line removed"). Three proof runs never caught it. 385,679 tokens.

## Where the I wall goes
| arm | wall | first-call latency | discovery | edit (compose + MCP) | verify | tool calls | MCP attempts/refusals | tokens |
|---|---|---|---|---|---|---|---|---|
| I1 | 69.2 | 6.56 | 45.33 (incl. 16.9 s truncated-JSON dead end) | 8.96 | 0 | 5 | 1/0 | 278,505 |
| I2 | 48.2 | 8.85 | 10.42 | 21.40 (19.1 s hand-typing the 54-owner table) | 0 | 3 | 1/0 | 187,310 |
| I3 | 49.8 | 7.89 | 11.09 | 22.61 (20.1 s hand-typing) | 0 | 3 | 1/0 | 185,367 |
| I4 | 57.0 | 7.22 | 31.01 (scripted regex walk) | 9.18 | 0 | 4 | 1/0 | 229,832 |
| N1 | 103.2 | 6.42 | 28.18 | 0.16 | 21.97 | 5 | 0 | 279,292 |
| N2 | 90.0 | 9.37 | 27.45 | 0.17 | 19.06 | 5 | 0 | 276,182 |
| N3 ✗ | 107.2 | 7.97 | 27.75 | 0.09 | 53.48 | 7 | 0 | 385,679 |
| N4 | 66.2 | 7.44 | 25.98 | 0.11 | 0.18 | 4 | 0 | 224,562 |
MCP startup ≈ 0.1–0.3 s (prompt arrival 1.67–1.92 s vs 1.43–1.62 s native).

## The biggest remaining sink, and the fix
Composing the `edits` argument: 19–20 s of hand-typed model output (I2, I3) or 31–45 s of scripted discovery (I4, I1) to build the 54-owner × 59-site [{file, within{form}, matches}] list — against a 2 s MCP call. Server-side fix: one inspect_clojure / prepare-change call that, given `next.jdbc/execute!`, returns exactly that list in the shape the same server accepts: 10–45 s → ~1 s; median I arm ~53 s → ~35 s. The cohort FORBADE those verbs (deterministic-surface hangover), so it measured the cost of denying the tool its own discovery path.

## Paper cuts (friction ledger)
1. Harness "Warning: truncated output (original token count: N)" is prepended to machine-readable stdout: killed I1's JSON builder (SyntaxError), polluted N3's JVM output and N4's sed output.
2. The tool description opens with "Use prepare-change when one fully qualified Var names the goal but exact sites are unknown" — advice for exactly this task, pointing at a verb the route forbade. All four I arms re-read the description in their first call.
3. Fixture has no .git → `git status` fatal in N2/N3/N4.
4. Linter: "namespace next.jdbc is required but never used" on every arm (correct under a qualifier-only spec; will fire on every future run).
5. Native self-proof is the real defect surface: 3/3 self-written oracles failed first on the oracle's own bug; the one real defect (N3) escaped all of them.
