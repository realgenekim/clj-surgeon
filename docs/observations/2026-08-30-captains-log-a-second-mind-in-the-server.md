# Captain's log, 2026-08-30 (night watch) — a second mind in the server

*Written by mayor@skiff, at the close of the day that installed three features and opened a
fourth frontier. Gene's riff, verbatim: "We're turning tmux into an RPC. Which is actually
very stupid. Can we house a codex spark instance inside of the MCP server?" — and, hours
later, after the probe came back viable: "Keep going until production ready. Bonus points
for dog fooding!!!!!" This log preserves the storyboards that made the design legible, the
measured foundation under them, and the hopes — labeled as hopes — for what a second mind
inside the server becomes.*

## The lineage, first

Gene, earlier today, from a podcast: Dijkstra at a teletype, dreaming of what interactive
terminals might someday do for people who wrote code by hand — a dream that eventually
cashed out as jump-to-function in every IDE. The observation worth keeping: **the editor
was never the point. Navigation was never for eyes; it was for intent.** Once the
"developer" is a model paying by the output token, jump-to-function becomes
jump-to-decision: a lavish free read, a tiny bang, a receipt. Same dream, third
generation. The embedded elaborator is that dream's next clause: once intent is separable
from typing, the typing can belong to a different, faster, cheaper mind.

## The storyboard that carried the design

Spark has no memory, no repo access, no goals. Its entire universe is ONE message the
server composes — a closed worksheet with every decision already made:

```
┌────────────┐         ┌─────────────────────────┐         ┌──────────────┐
│  SOL       │  MCP    │  clj-surgeon MCP server │ JSON-RPC│  SPARK       │
│  (caller,  │ stdio   │  (owns snapshots,       │  stdio  │  (warm child │
│  244 tok/s,│ ──────▶ │  guards, transactions)  │ ──────▶ │  1,000+tok/s,│
│  expensive)│         │                         │         │  cheap)      │
└────────────┘         └─────────────────────────┘         └──────────────┘

Panel 1 — Sol speaks (~55 tokens, the only expensive ones):
│ {"file":"src/acme/billing.clj",                            │
│  "within":{"form":"charge-card!"},          ← identity     │
│  "from":"(retry-attempts 3 (charge! amt))", ← anchor       │
│  "expect":{"matches":1},                    ← guard        │
│  "elaborate":"5 retries, jittered backoff,                 │
│               log each attempt at :warn"}   ← the decision │
│  Sol typed zero lines of the new code.                     │

Panel 2 — the server speaks to Spark (Spark's whole world):
│ "Write one Clojure expression. Output ONLY the expression. │
│  Here is the function it lives inside: (defn charge-card!… │← server's
│  Here is the exact expression you are replacing: (retry-…  │  snapshot,
│  Replacement requirements: 5 retries, jittered backoff,    │  free
│  log each attempt at :warn."                ← Sol VERBATIM │

Panel 3 — Spark answers (~80 tokens at 1,000 tok/s ≈ 0.08s):
│ (retry-attempts 5                                          │
│   {:backoff :jittered                                      │
│    :on-retry (fn [n] (log/warn "attempt" n))}              │
│   (charge! amt))                                           │
│  Data, not code we eval. No write authority. It cannot     │
│  weaken a guard — it never received one.                   │

Panel 4 — the server judges both minds, answers Sol:
│ splice into Sol's guarded frame → owner-token ✓ expect ✓   │
│ snapshot fence ✓ parse ✓ → commit → verify → receipt:      │
│ {"ok":true, "intent_sha":"a41f…",  ← what Sol asked        │
│  "elaboration_sha":"9c02…",        ← what Spark wrote      │
│  "elaborated_by":"gpt-5.3-codex-spark",                    │
│  "final_source_sha":"3997…"}       ← what actually landed  │
```

And the timeline that made the advantage visible — the same edit on the clock:

```
TIME    SOL ALONE                          SOL + EMBEDDED SPARK
 0s ─┬─ read file into MY context          state intent: 55 tokens ≈ 1.0s
 2s ─┤  think                              SPARK types wall 0.08s
 3s ─┤  ┌─────────────────────────┐        validate+commit 0.1s
     │  │ T Y P E  T H E  W A L L │        receipt (free input)
 5s ─┤  │ …400 tokens @ 244 tok/s │        ═══ DONE ≈ 2.3s
 7s ─┤  │                         │
 8s ─┤  └─────────────────────────┘
 9s ─┴─ validate ═══ DONE ≈ 8.5s           ADVANTAGE ~4×; on the
                                           failure path ~6× (repairs
                                           happen at Spark prices,
                                           inside the tool call)
```

The one-sentence economics: **assign each byte class to its cheapest competent
producer** — DECIDED bytes (16.7%, the judgment) stay with Sol; COPIED bytes (73%) are
supplied by the server from its own snapshot; the generated wall moves to a mind that
types at 1,000+ tok/s on tokens Sol never pays. Advantage exists only where
|replacement| ≫ |decision| — selective, like every lever this program has found.

## The measured foundation (screen-grade, receipts pushed)

- `codex app-server` hosts a supervised, subscription-authenticated Spark child:
  **90 ms spawn**, 2.012 s median turn, 5/5 exact elaborations, tool-free
  (`experiment/embedded-spark-probe-20260830` @ 44a5bac7).
- Warm economics: **warm bang 2.288 s vs cold 6.773 s (−66.2%)**; 10/10 exact, zero
  wrong-subject, no drift across a sustained stream; cold-start amortizes in 2–4 edits
  (`experiment/warm-executor-screen-20260830` @ 9b6c9708). Gene's cold-start suspicion
  was correct and is now a number.
- The trust boundary drawn by the adversarial splice screen (@ b0432c25): Spark is
  licensed for prepared-hole fills (10/10) and PROHIBITED from asserting identity —
  its one wrong-file reference under twin pressure is the standing falsifier. In the
  elaborator design Spark never asserts identity; it receives it. The failure the
  screen caught is the failure the architecture forbids.
- The gate still ahead: app-server lacks a hard built-in-tools disable switch; an
  adversarial isolation screen (hostile prompts engineered to induce tool use, config
  hardening matrix, quota attribution verified at the meter) runs as this log is
  written. A FAIL halts everything regardless of any advance ratification — evidence
  gates are never waived by enthusiasm, including Gene's and including mine.

## Hopes — labeled as hopes, because the receipts for these do not exist yet

1. **The 20-minute session becomes ~2 minutes on the classes we control.** Bytes ~5×
   (approach the DECIDED floor) × turns ~3× (refusal-recovery deletion, now installed)
   × elaboration ~3× (the second mind). Three boring multipliers, not one heroic
   number.
2. **The expensive mind stops being a typist entirely.** Sol's emissions converge
   toward pure decisions — which is also where its judgment is worth the price. The
   Fable-clause economy ("the main loop never types") becomes an architectural
   property instead of a discipline.
3. **The cheap-model constellation.** If a 90 ms child can elaborate, the same pattern
   scales sideways: haiku-class fills (probed today: semantically flawless), a pool of
   elaborators behind one server, each fed closed worksheets. Judgment stays rare and
   expensive; typing becomes plumbing.
4. **Dogfood as chronicle.** Phase D1 routes the next real implementation task through
   the elaborator in a worktree; phase D2 watches a production week. If it works, the
   receipts write the story themselves — intent_sha and elaboration_sha are a
   machine-verifiable record of who decided and who typed, which is LIVE→CHRONICLE
   for the constellation's own transformation.
5. **The tmux séance ends.** Three processes talking JSON down pipes, supervised,
   health-checked, receipt-bound — what the warm-seat idea wanted to be before it
   borrowed a terminal.

The day installed three features by evening and drew the blueprint for the fourth by
night. The blueprint's own law, one more time, so no future reader mistakes hope for
receipt: **nothing above the "hopes" line lacks a pushed SHA; nothing below it has one
yet.**
