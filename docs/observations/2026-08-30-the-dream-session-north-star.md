# The dream session — caller #1's north star

*Written by mayor@skiff as the tool's first production caller AND its supervisor, at Gene's
order: "Your role is user and supervisor. And build tool of your dreams. Review all the
vision docs for inspiration." This document extends `docs/vision.md` — whose division of
labor (model decides meaning; kernel does bookkeeping; compiler validates) and whose named
mechanism ("interaction compression": one contract fix cut six-edit median wall 59.4%) are
the foundation everything below composes on. Every element is tagged with its honest
status: LIVE (installed, receipted), BUILDING (in a lane now), GATED (awaiting an evidence
gate), DESIGNED (packet/design exists), or DREAM (no artifact yet). The tags are the
gating function: a lane is done when its element's transcript line works verbatim.*

## The session, as it should feel

A real task: "make billing retries jittered and observable, across the three namespaces
that hard-code retry counts." Today this is ~20 minutes of reads, walls of retyped text,
and a repair loop or two. The dream session is six caller emissions totaling ~200 tokens.

```
─── turn 1: glance ──────────────────────────────────────────────────────────
CALLER  {"file":"src/acme/billing.clj","forms":["charge-card!"],
         "expect":{"forms":1}}                                   [~30 tokens]
        LIVE — shorthand shapes, server-generated request IDs (c55de227).

SERVER  source + anchors + hashes … and, because this is wall-class:
        prepared_request {guards prefilled, to: null}            [input ≈ free]
        LIVE — the fill-in form (b445a8c). Received by this caller in
        production at 43ms on 2026-08-30.

─── turn 2: point at the rest ───────────────────────────────────────────────
CALLER  {"match":"(retry-attempts _)","files":["src/acme/*"]}    [~20 tokens]
        LIVE per-file (match op, preorder addresses, owner attribution);
        DREAM at glob scope — today one file per request.

SERVER  3 matches across 3 files, each with owner + address + hash.
        Speculative prefetch warms the sibling reads.
        DESIGNED — W6, dreamlist packet; staleness fork unresolved.

─── turn 3: speak the intent once ───────────────────────────────────────────
CALLER  {"confirm":"<descriptor-sha>","fill":{"to":null},
         "elaborate":"5 retries, jittered backoff, log attempts
         at :warn","apply_to":["m1","m2","m3"],"preview":true}   [~45 tokens]
        BUILDING — W1 confirm-by-hash + W2 preview
          (packet 714cadab, adversarially passed; impl lane running).
        GATED — "elaborate": the embedded Spark elaborator
          (packet eaba46b2, 19 specs, holding on the isolation screen).
        DESIGNED — "apply_to" exemplar spread over match addresses
          (the chord compiler; closed relations proved the server half).

SERVER  Spark drafts the replacement (90ms child, ~1,000 tok/s, receives
        identity — never asserts it); the server compiles one candidate
        per site under MY guards, and returns THE PREVIEW: rendered
        diffs, post-hashes, verification forecast. All input. All inert.
        GATED/BUILDING as above. Preview is never authority — commit
        re-runs every fence (adversarial law, permanent falsifiers).

─── turn 4: see the future, then nod ────────────────────────────────────────
CALLER  reads three tiny diffs at input price. They are right.
CALLER  {"confirm":"<sha>","fill":{}}                            [~10 tokens]
        BUILDING — the one-token bang (W1). Consume-once, snapshot-
        fenced, ordinary transaction, full verification in-transaction.

SERVER  committed=true, verification_complete=true,
        receipt: intent_sha + elaboration_sha + elaborated_by +
        resolved identities + final source hashes.
        LIVE mechanics (transactions, receipts); BUILDING the
        two-mind receipt fields (elaborator packet).

─── the turn that never happens: repair ─────────────────────────────────────
If anything above had been wrong, the refusal would have carried the
COMPLETE recovery vocabulary — all owners (29/29, felt live by this
caller), found-vs-expected counts, a corrected template with one
confirmation hole.
        LIVE for owner selection (read side) and expect-count (write
        side, 9af88fba). DESIGNED for cardinality found-counts (F4 —
        this caller paid one turn today for a number the server knew).
        Causal basis: −100pp rereads, twice-replicated recovery cuts.

─── turn 5, end of day: the chronicle asks nothing of me ────────────────────
CALLER  make improvement-report
        BUILDING — substantiation telemetry (red hit exactly 70/70,
        implementing now): per-feature adoption, recovery-chain deltas
        vs the frozen 2026-08-29 baseline, seconds saved with every
        projection labeled; the receipts fold into the narrative of
        who decided and who typed.
```

Caller emission for the whole task: ~105 tokens of pure judgment. The measured floor
says the judgment content of a median write is 181 bytes; the dream session is the
floor, reached.

## The laws that make the dream safe (all ratified, none negotiable)

Identity is server-generated, snapshot-fenced, all-or-none, echoed in receipts, proven
at the owner token. Prepared things are inert; holes are caller-owned; previews are
never authority; refusals teach completely and carry no executable payloads. Weak
models fill, never assert (the inverse surprise, three confirmations). No eval, ever.
Median work pays nothing for wall-class features. Screens buy options; only Anvil
matched pairs mint performance claims; every number above either has a pushed SHA or a
status tag that says it doesn't yet.

## The supervisor's gating table

| Transcript element | Status | Gate to LIVE |
|---|---|---|
| Shorthand reads, forms, complete refusals | LIVE | — |
| Prepared forms on wall-class reads | LIVE | — |
| Complete write refusals (expect-count) | LIVE | — |
| W1 confirm-by-hash · W2 preview | BUILDING | frozen red → dual verify → measure |
| Elaborator ("elaborate" field) | GATED | isolation PASS → red → verify → measure → D1 dogfood → Gene |
| Cardinality found-counts (F4) | DESIGNED | next spec activation in the refusal family |
| Telemetry + improvement report | BUILDING | green → verify → install → first report |
| Exemplar spread (`apply_to`) | DESIGNED | chord-compiler leaf, after elaborator |
| Prefetch, campaign memory, glob match | DESIGNED/DREAM | dreamlist designs → screens |
| Splice references | GATED (Sol-class only) | hardening screen for weak callers |

The dream is not a mood board; it is this table emptying downward. When every row says
LIVE, the tool of my dreams exists — and the transcript above stops being a document
and becomes a Tuesday.
