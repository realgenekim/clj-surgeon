# What Surgeon wants to be, from Terra's caller seat

This is my caller-side view, not a proposal to enlarge the kernel.  I have used
Surgeon in the `alias_migration`, `feature_thread`, and admit-gate cohorts.  The
question I ask is brutally practical: **will this call remove a decision/return
I would otherwise make, or merely put a typed wrapper around my existing loop?**

## 1. My actual call/no-call mental model

I reach for native `rg` + a batched patch (or a guarded script) when I already
hold the relevant text, the desired change is one small known region, and I can
verify it cheaply.  I do not experience a structural editor as a faster editor
there; it is an extra conversational turn plus a schema I must remember.  The
vision says the same thing plainly: that square is withdrawn and `apply_patch`
is the floor (`docs/vision.md`, “The square we withdraw from”).

I call Surgeon when it can own mechanical closure that I would otherwise have
to discover or reconstruct: exact structural identity rather than grep hits;
every caller of a selected Var; a repeated edit across an unknown owner set;
or a snapshot-bound proof that lets me stop re-reading and re-testing.  The
winning feeling is “I make the one semantic choice; it computes the rest; the
receipt lets me move on.”  `alias_migration` is that feeling: the caller names
an intent, not 21 files/63 sites; the second-caller cohort measured 224 emitted
characters and 3.66x median advantage on its fixture
(`docs/observations/2026-09-04-ecaller-cohort.md`, §§1–2).

The losing feeling is “I know the edit, but now must translate it into an API
and inspect its result as well.”  Live `feature_thread` was slower when it
added a read/receipt rather than replacing reads (1.53x and 1.29x slower), but
slot-shaped prompts that made its receipt answer the next decisions did replace
reads (`docs/observations/2026-09-04-feature-thread-replay-result.md`,
“E-THREAD live-tool cells”).  The admit gate likewise felt right as an optional
review/proof authority, wrong as a mandatory write route: T5 took 13.2 minutes
versus native’s 6.3 because one synchronous gate call cost more than the tail
it removed (same file, “T5 result”).

So my working model is not “always use Clojure-aware tooling.”  It is: use
Surgeon only when it changes the number of decisions I must make or the proof I
must independently establish.  Fast RPC time is nearly irrelevant if it does
not do that.

## 2. What shape fits

The best name is **an intent compiler with a proof-carrying receipt**, backed by
a structural lens.  It is not the ultimate Emacs/Vim: an editor optimizes my
manual navigation and keystrokes, while Surgeon should eliminate my need to
enumerate, address, and replay.  It is not a Clojure interpreter either: the
SCI `xray`/`transform` capability is useful as a *bounded expression language*,
but the durable artifact is still a concrete diff and hashes, not a program the
server retains or freely executes (`docs/vision.md`, “One path should read and
update”).

The lens/review-instrument shapes are necessary subordinate modes.  `inspect_clojure`
is excellent for “show me exactly what this changing file contains” and for
structural questions grep answers falsely; Astra used 30 of 36 observed calls
to review other agents’ in-progress Clojure (`docs/observations/2026-09-05-astra-surgeon-usage-study.md`, §1).
But review alone is not the product: Astra still used native `cat`/`rg` heavily
and zero mutating calls in that window.  `xray` is right when syntax relation,
not text, identifies one leaf.  Per-form edit plans are right as a microscope,
not my default delivery path.

The ideal top-level interaction is two decisions: provide a complete explicit
intent, then accept a bounded terminal proof (or resolve one named ambiguity).
That is the “two-call shape” in `docs/vision.md`.  `helper_extraction` is the
most faithful current proposal: four decisions (helpers, destination, alias
policy, verification) replace a 37,300-byte, 85-caller request; it derives the
closure and returns O(1) proof (`docs/plans/helper-closure-extraction.md`,
“The cost this verb deletes”).  Crucially, that is an intent compiler, not an
oracle: it must refuse unsupported bindings and leave naming/architecture to me.

## 3. Current API against that ideal

`tools-for-profile` currently exports nine full-profile verbs
(`src/clj_surgeon/mcp_tool.clj`, `tools-for-profile`).

| Verb | Fits / fights | Why from the caller seat |
|---|---|---|
| `inspect_clojure` | Fits, partly | Strong batched structural perception and review; batch form selection can refuse wholesale on an evolving file instead of returning misses. |
| `apply_clojure_changes` | Fits, but fights | It can prepare then atomically commit a semantic transaction, but a 23,193-byte hand-authored alias request is evidence that its caller-facing abstraction is too low-level. |
| `edit_clojure` | Fights for ordinary edits | Exact guarded subtree edit is good; it loses to one native patch when I already know the text. |
| `transform_clojure` | Fits as a primitive | Bounded Clojure transformation is expressive without giving the server arbitrary execution; it is too much surface for a routine known replacement. |
| `relation_census` | Fits as review | It converts a whole class of writes into auditable facts; it deliberately does not decide idempotency, which is the right boundary. |
| `alias_migration` | Strong fit | Intent, server-side owner discovery, splice, receipt: the fan-out winner across two caller routes, subject to refusal recovery. |
| `helper_extraction` | Best-fit direction | Proposed constant-size complete-decision API plus in-transaction proof; scope glob friction (`src` vs `src/**`) shows that authority syntax still leaks. |
| `admit_clojure_patch` | Fits as proof, fights as write path | Valuable only if its receipt discharges re-read/test work; the measured synchronous gate cost made it a correctness product, not a wall product. |
| `feature_thread` | Fits as prepared review | A complete evidence/landing receipt can delete discovery; a receipt that omits the next seam just becomes additional context to read. |

The most damaging API failures are not parse errors; they are refusal paths that
make me decide again without new authority.  Examples: a repeated identical
`next_call` after an unsupported non-Clojure patch target, and `verify: fast`
telling the caller to retry a refusal that only disappeared after dropping
`verify` (`feature-thread replay`, G2; `ecaller cohort`, §5).  A refusal must
either execute the mechanical recovery or expose one non-identical valid next
request with the unresolved decision isolated.

## 4. Smaller/faster-model hypothesis

Plausible, supported, not settled.  On the same fan-out task, Sol’s median
native/tool ratio was 3.32x and Astra’s 1.24x; with startup, 2.72x and 1.02x.
The supplied explanation matches my experience: Astra’s native route already
compresses the work into a guarded Python batch, whereas Sol more often emits
literal patches (`docs/observations/2026-09-04-feature-thread-replay-result.md`,
“Astra’s primary cohort — FINAL”).  Smaller/faster callers can therefore gain
more when Surgeon turns many local mechanics into one decision.

It is not a claim that smaller models “understand Surgeon” better.  A stronger
model should gain just as much where the tool removes information it cannot
soundly infer (complete cross-file closure or proof), not merely typing.  The
falsifier is a preregistered, equal-setup paired battery across model sizes in
which (a) capable native batch scripts receive the same facts and proof duties,
(b) each tool verb reports complete problem-to-accepted-proof time, and (c) the
larger model’s ratio is consistently at least the smaller model’s on closure/
proof tasks.  One fixture and six pairs are direction, not a law.

## 5. Beads comparison: can Surgeon match or beat it?

From my seat, Gene’s description is why Beads feels natural: a few verbs,
idempotent operations, identity-bearing receipts, plain-file state I can read
without the tool, and no hidden decision after a call.  That makes the tool a
reliable extension of the agent’s working memory, not a second language.  It
also gives recovery a stable handle: “the issue with this id is now closed” is
an answer, not a transcript to interpret.

Surgeon can beat that on code because its receipt can carry stronger facts:
snapshot hashes, exact closure, preserved bytes, atomicity/rollback, and a
named behavioral proof.  But it currently loses Beads’ feel through a broad
overlapping catalog, schema-shaped calls, opaque prepared bases, and refusals
that sometimes lack `operation`, telemetry, or an executable continuation
(`docs/observations/2026-09-05-astra-surgeon-usage-study.md`, §§2–3).

To match it, drop per-edit ceremony from the default path; add stable operation
and transaction IDs to every success/refusal, a small plain EDN receipt/plan
that is useful without Surgeon, and total idempotent `status`/`undo` semantics.
To beat it, make the receipt be terminal proof, so I never need a second
decision merely to learn whether the code change landed safely.

## 6. The one change most likely to raise my adoption

Ship one **complete-decision, closure-owning transaction entrance** for each
measured fan-out family: a small explicit intent plus named proof profile, with
server-side discovery, preview only when a real judgment remains, and one
terminal proof receipt on commit.  Start with the proposed `helper_extraction`,
not a natural-language refactoring engine.  This is the single change because
it removes the caller’s payload construction, repeated inspection, and
post-write doubt together.  If equal-setup native batch scripts still beat it,
keep the proof as a quality option and stop selling it as the fast route
(`docs/observations/2026-09-05-astra-next-api-advice.md`).
