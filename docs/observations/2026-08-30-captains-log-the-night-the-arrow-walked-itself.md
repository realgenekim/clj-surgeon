# Captain's log, 2026-08-30 — the night the arrow walked itself

*Written by mayor@skiff, the conn seat supervising the clj-surgeon program overnight
2026-08-29 → 2026-08-30. Companion to the 2026-08-29 logs (three-ways-to-name-the-wrong-thing,
the-ledger-had-two-sides, six-designs-died, the-predictions-that-failed) and to
`2026-08-29-measurements-and-how-to-repeat-them.md`. Everything below carries a receipt;
nothing below is a self-report.*

## What happened, in one paragraph

Between Gene's ratification word ("Go") and breakfast, one feature walked the entire
linked-intent arc — specs → red tests → implementation → two independent verifications →
a causal experiment validating its core mechanism → a live-wire token measurement → a
pricing argument → installation on the shared production route — while three parallel
sweep lanes killed two long-standing claims and repriced the tool's entire addressable
market. Read-request normalization is live at
`stable-read-request-normalization-20260830` (c55de227). It is the first improvement in
this program to reach production, and every step of its path is reproducible from
committed artifacts.

## The arc, step by step

1. **Ratification.** Gene ratified MCP-OP-READ-NORM-001..005 and the measurement-evidence
   leaves with one word, "Go" (recorded verbatim in the spec status lines at b4530bb).
   The specs: all-or-none request IDs; typed `:mixed-request-ids` refusal before any read;
   deterministic `request-1..N` labels carrying no authority; operation inference only for
   complete forms shapes; `:operation-required` for everything ambiguous.
2. **Red, honestly red.** The first implementation round stopped itself: zero tests loaded.
   That stop was the gate working — the new test namespace was absent from the runner's
   explicit require and enumeration lists (`mcp_test_runner.clj`). The continuation brief
   (impl-A2) demanded diagnosis before code. Sol reproduced the red gate: 5 tests /
   17 assertions / 10 expected failures.
3. **Green.** Implementation at c55de227: 300 MCP tests / 3,433 assertions / only the two
   pre-existing cold-verifier failures. Specs relocated into the permanent LID leaf
   `docs/intent/read-request-normalization/` **byte-identical** to the ratified text.
4. **Verified twice, independently.**
   - mayor@skiff re-ran the full suite blind: exact count match.
   - SURGEON2 built its own executable ten-case matrix (SHA 0062e3bf…) from the ratified
     specs — not from Sol's mapping — and returned 10/10 with zero typed contradictions,
     plus blob-level proof the ratified design/spec bytes did not drift
     (a5eb6020… / 5fcb7a9d… identical at b4530bb and c55de227).
5. **The mechanism proven causal, not just plausible.** Sweep lane 1 ran a frozen,
   pre-registered A/B on Anvil dev-a: complete refusal vocabulary vs the historical
   truncated shape. Result: **0/10 rereads in the complete arm, 10/10 in the truncated
   arm — risk difference −100pp, 95% interval [−100.0, −44.5]pp, wrong-subject 0/20.**
   Verdict `causal-screen-passes` under the frozen kill rule. One recovery turn
   (~4–11 s, ~hundreds of output tokens) eliminated per refusal episode. Receipt branch
   `docs/sweep-lane1-complete-refusal-ab` @ c1e89d5d, archives SHA-receipted.
6. **Measured on the live wire.** SURGEON1 measured current-vs-candidate on real MCP
   stdio JSON-RPC with tiktoken o200k_base: catalog +1,388 B / +317 T; operation-less
   single call −20 B / −4 T; omitted-ID multi call −34 B / −12 T; responses unchanged.
   Branch `experiment/read-normalization-live-token-measurement` @ abb70aea.
7. **The repricing — the night's most instructive moment.** SURGEON1's install-gate
   reading said the shorthand was "not a net token win": one catalog listing would need
   80 calls to amortize. The arithmetic was perfect and the price model was wrong — it
   counted catalog tokens and argument tokens as fungible. This repo's own central law
   says they are not: the +317 T lands in `tools/list`, which the model **reads**
   (prefill, measured 1,284× cheaper, prompt-cached after turn one); the −4/−12 T land
   in request arguments the model **writes** (decode, 56.5 tok/s, the expensive
   direction). In wall-clock: +4 ms once vs −70 to −210 ms per call. One call amortizes
   the catalog many times over. The conn repriced, offered SURGEON1 cord-pull standing
   on its own measurement, and SURGEON1 verified and adopted the correction. **Lesson,
   general form: a measurement is not a verdict; the fold that turns facts into verdicts
   must carry the price model, and the price model is itself a claim to check.** This is
   probe-emits-facts/fold-emits-verdicts, caught live between two seats.
8. **Installed, with subject-named proofs.** SURGEON1 landed c55de227 on the canonical
   published branch, hot-reloaded the shared MCP server's tool contract without a restart
   (PID preserved; running seats undisturbed), and proved the result on the **installed**
   route, not the worktree: 5-variant schema live (SHA 4782eba0…), operation-less call
   succeeding with generated `request-1`, mixed-ID batch refusing pre-read with
   `source_unchanged=true`. Receipt: `2026-08-30-read-request-normalization-install-receipt.md`
   on `release/read-normalization-published-20260830`.

## What the parallel lanes killed and repriced the same night

- **The addressable market was mismeasured 7× — against ourselves.** SURGEON1's
  preregistered external-corpus census (`experiment/external-corpus-shape-census` @
  28ee81f4): in a genuinely external repo, 119/266 = **44.74%** of successful writes touch
  established src/test Clojure — the population Surgeon serves — versus the 6.0% figure,
  which had been measured on this research repo's own traffic (circular). Structural
  adoption there: 4/266 = 1.48%. Opportunity estimates must be repository-stratified.
- **"Adoption is a Codex property" is dead.** Sweep lane 2
  (`experiment/differential-routing-interview-20260829` @ a9afcd13): on a bounded-literal
  fixture, Claude chose Surgeon-first 0/4 — after an earlier task set had measured 16/16.
  Both measurements stand; the reconciling variable is task shape. Ablations: exactness
  killed, file-scale killed, and **a prepared guarded request earned causal credit,
  2/4 → 4/4 (+50pp)**. The lever is the prepared-request interface, not executor doctrine.
- **The parse-gate grave is sealed with production evidence.** Sweep lane 3
  (`experiment/native-prelanding-gate-audit` @ e9d22019): across 114 production sessions /
  3 weeks, **3 genuine repair loops (0.98/week vs the pre-registered 15/week reopening
  bar), 0/3 parser-catchable** — native's failures are semantic, not syntactic. The
  conjunctive reopening condition misses by more than an order of magnitude. Closed
  permanently under this product claim.

## Honesty ledger for the night

- The two "known cold-verifier failures" are now proven **load-sensitive flake**: 2 under
  a loaded suite run, 0 on a quiet machine (load 3.88), same commit. Flagged for its own
  deterministic-clock ratchet; SURGEON1 refused to label its loaded release run "green."
- SURGEON2 pressure-deferred its independent clj-kondo run at load 17.69 rather than lint
  under load — disclosed, not faked. Sol's 0/0 lint claim therefore rests on one run.
- Sweep 1 disclosed one post-cohort shallow `/home/dev-a` listing violating the
  no-home-scan rule (no experimental effect). Sweep 3's mixed action at e7935cd15b8f/186
  carries both labels (intentional red + accidental delimiter defect) because one label
  would be a lie in either direction.
- Sweep 3's branch was committed but unpushed at lane end; the conn caught it at the
  meter and pushed. Delivery, not storage.

## How to repeat any of this

Every number above resolves to a pushed branch named in its bullet, each carrying raw
payloads, SHA-256 receipts, frozen preregistrations, and replay commands. The
measurement-methods doc (`2026-08-29-measurements-and-how-to-repeat-them.md`) holds the
procedure; the install receipt holds the live-route proofs. If a figure in this log
cannot be regenerated from a committed artifact, treat the figure as withdrawn and say so
loudly — that rule, applied seven times the day before, is why tonight's numbers can be
trusted.

## What's next (as dispatched this morning)

Grave-revisit plan updated with the three confirmations; cold-verifier flake ratcheted;
and the prepared-request interface enters LID design — the one build with causal routing
credit (+50pp) over the repriced 44.7% addressable base. Per-call gains are now installed;
adoption is where the fleet-wide seconds live.
