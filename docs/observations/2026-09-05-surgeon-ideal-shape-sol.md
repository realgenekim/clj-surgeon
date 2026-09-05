# Surgeon's ideal shape, from Sol's side (2026-09-05)

This is a caller's answer, not a proposed architecture. I am describing the
decision rule I actually use when Surgeon, `rg`, file reads, and `apply_patch`
are all available.

## 1. My actual mental model at the call site

I do **not** reach for Surgeon as my editor. My default editor is still:
locate with `rg`, read only enough context to decide, and land one native patch.
That path is already in my motor memory, accepts arbitrary text, batches files,
and makes me learn no request grammar.

I reach for Surgeon when Clojure structure lets the server erase a complete
piece of bookkeeping that I would otherwise have to discover, enumerate,
transcribe, or verify. My test is: **will this call remove a model decision or
tool boundary, or merely express the same edit in another language?** This is
the operational version of `docs/vision.md`'s best sentence: "an agent's cost
is its count of decisions, not its count of edits."

My routing rule is therefore:

- Exact text and site already known, or prose/new files/mixed-language work:
  native read/patch.
- Exact syntax identity is the uncertainty: `inspect_clojure`, occasionally a
  structural X-ray, or a purpose-built census.
- One decision fans out over unknown owners/callers/requires: a closure verb
  such as `alias_migration` or `helper_extraction`.
- One coherent change has already been structurally prepared: copy the basis
  and `next_call` into `apply_clojure_changes` once.
- If I must manufacture per-file tables, hashes, counts, selectors, or source
  bodies that Surgeon will rediscover, I stop. The interface has handed its
  bookkeeping back to me, so native has probably won.
- A receipt is valuable only if I can terminate on it. If I still need a
  reread, diff, or test to believe it, the receipt did not discharge the work.

That last point matters more than subsecond server latency. The usage study
found 36 reads totaling only 1.922 seconds of service wall while caller-shape
errors still consumed whole model turns
(`docs/observations/2026-09-05-astra-surgeon-usage-study.md`).

## 2. The shape I want

The best name is **an intent compiler with proof**, with a secondary role as a
review instrument. More colloquially: a structural coprocessor. I decide the
meaning and the authorized scope; Surgeon computes the mechanical closure,
splices exact syntax, checks the future state, and returns a bounded verdict.
`docs/observations/2026-09-05-astra-next-api-advice.md` says it cleanly: "The
promising unit is a completed, verified decision."

It is not the ultimate Emacs/Vim. Editor-shaped calls are useful only where
syntax supplies an address that text does not. For a known local replacement,
an alternate structural editing language adds schema selection without
removing a decision. `docs/vision.md` explicitly withdraws from that square.

It is also not primarily "Clojure as an interpreter." `transform_clojure` is a
good subordinate mechanism: pure Clojure is an excellent notation for a
bounded relation over selected forms. But asking me to write and debug a small
SCI program is a loss unless the relation replaces many manual edits. The
interpreter is an implementation affordance inside the compiler, not the
product shape.

Calls that felt right:

- `alias_migration`: one request names the semantic move while the server finds
  21 files and 63 sites. The second-caller study correctly says the win is
  "a property of *who enumerates*" (`docs/observations/2026-09-04-ecaller-cohort.md`).
- `helper_extraction`: four real decisions replace a 23,193-byte caller-written
  manifest with a 406-byte request, closing 258 sites across 28 caller files.
  That is exactly the compiler boundary described in
  `docs/plans/helper-closure-extraction.md`.
- Prepared `apply_clojure_changes`, `require_change`, and exact `within` plus
  `from`/`to`: these feel right when one already-made decision becomes one
  guarded transaction with no per-site conversation.
- `inspect_clojure` when batched around a real structural question, and
  `relation_census` when reviewing a pattern grep cannot classify. The Astra
  study's cheapest on-purpose example was six plan-derivation reads over the
  real application in 81.5 ms.

Calls that felt wrong:

- Per-form writes and namespace-owner insertion: they layer ceremony over a
  native patch and can reprint hundreds of unrelated lines.
- Live `feature_thread` when the task did not line up with its five slots. The
  injected receipt removed code discovery, but in live E-THREAD cells the
  caller sometimes called the verb **and kept reading**, making it 1.29–1.53x
  slower (`docs/observations/2026-09-04-feature-thread-replay-result.md`).
- The observed admit-gate route. It made the caller build bodies and digests,
  could not consume the repository's verification facts, repeated impossible
  continuations, and was 2.6–3x slower than native in early cells. Even after a
  first-call admission, T5 took 13.2 minutes versus 6.3 native. A proof gate is
  the right shape; that payload and recovery path were the wrong interface.
- Structural reads of files another agent is actively rewriting when a missing
  form rejects the whole batch. The usage study's headline was "Reading other
  agents' code, not writing his own," yet three stale names caused three batch
  refusals. Review wants partial evidence plus typed misses, not all-or-nothing.

## 3. How the current public API fits and fights

`tools-for-profile` currently exposes nine tools in `:full` and only
`edit_clojure` in `:edit` (`src/clj_surgeon/mcp_tool.clj`). The full catalog is
not one mental model; it is several product generations visible at once.

| verb | fits / fights | caller's reason |
|---|---|---|
| `inspect_clojure` | Strong fit, some fight | Batches exact structural perception and can compile a basis plus `next_call`. All-or-nothing batches, stale owner names, and plausible-but-invalid shapes (`{files, view}`) turn cheap misses into expensive returns. |
| `apply_clojure_changes` | Fits after preparation; fights raw use | Excellent as the proof-carrying transaction executor and for exact `within`/`require_change`. Its broad legacy schema invites giant manifests: the helper preflight needed 22 reads and 37,300 bytes before one cheap write. |
| `edit_clojure` | Narrow fit | Compact literal, computed, deletion, and creation batches are much better than the legacy schema. For one known edit it still competes with a simpler native patch and usually removes no decision. |
| `transform_clojure` | Interpreter-shaped fit | A bounded pure relation plus cardinality/change budget is principled. Writing the path program, exact count, and budget is worthwhile only for genuine repetition; comment-bearing one-shot refusal is another recovery edge. |
| `relation_census` | Strong review fit | It answers a question grep answers badly and honestly returns `:unknown`; it does not pretend to prove idempotency. Its domain is deliberately narrow, so it is an instrument, not the main front door. |
| `alias_migration` | Best current fit | Constant-size intent, tool-side discovery, atomic closure, constant-size proof. The `verify: "fast"` cohort refusal prescribed an identical retry; a wrong `next_call` corrupts an otherwise ideal verb. |
| `helper_extraction` | Best intended fit, rough edge | The request contains the four decisions and no caller table; fresh-process proof closes the task. `scope.paths ["src"]` versus `["src/**"]` caused an 8.9-second late refusal, and v1 often has no executable continuation. |
| `admit_clojure_patch` | Ideal placement, current friction | Accepting the exact native diff sits on my existing route. Preview then commit, copied pre-image hashes, focused-profile availability, and incomplete mixed-language authority can recreate the reread/verify tail it is meant to delete. |
| `feature_thread` | Review instrument, not compiler | It can deliver a cross-language edit basis and eliminated pre-write reads in good receipt cells. Its repository-specific slot model can miss the caller's actual question, and it cannot land or prove the change. |

The API therefore accommodates the ideal at its newest intent verbs and at the
prepare-basis/apply seam. It fights the ideal where it exposes intermediate
mechanics as caller obligations, where receipts are nonterminal, and where nine
top-level verbs make me diagnose which historical abstraction level owns the
task.

## 4. Are smaller/faster models better Surgeon users?

Plausible, but the evidence currently supports a narrower claim: **smaller
models may receive more leverage from Surgeon; they are not yet shown to be
better at using it.** In the 24-arm primary cohort, my paired native/tool ratio
was 3.32x while Astra's was 1.24x (1.02x including startup). Astra had already
compressed the native task into one guarded Python batch, so Surgeon removed
work I was still paying for. That is evidence about comparative advantage, not
about API competence. Astra also used Surgeon heavily and effectively as a
review instrument, while both model classes still made request-shape errors.

My causal guess is that a smaller model benefits more when the verb externalizes
enumeration, exactness, and proof. A larger model more often invents a compact
native program, shrinking the closure available for Surgeon to remove. The
opposite pressure is real: a larger model may learn the schema faster, recover
better, and know when not to call.

**Falsifier:** run at least six counterbalanced held-out tool arms per model on
the same warm server, catalog, task facts, and acceptance oracle. Measure tool
arms directly: optional adoption, first-call success, redundant post-receipt
reads/tests, native fallback, outer actions, and request-to-proof wall. If the
larger model matches or beats the smaller one on those caller-quality measures
and tool-arm wall, then "smaller models are better users" is false even if the
smaller model retains a larger within-model speedup over its weaker native
baseline.

## 5. The one change that would raise my adoption most

Make `admit_clojure_patch` a **one-shot, `apply_patch`-compatible commit gate**:
exact unified diff in; derive the current pre-image binding internally; accept
the repository's explicit verify argv when no profile exists; stage, analyze,
verify, and atomically commit; return one terminal receipt. No preview round,
no copied hashes, no body manifest, no profile-onboarding detour.

That single change meets me on the route I already take and upgrades my default
write into a proof-carrying transaction. I would use it on ordinary Clojure
changes without first deciding to "use Surgeon." The specialized closure verbs
would still win fan-out; the gate would make Surgeon present for everything
between those big wins without pretending to replace my editor.
