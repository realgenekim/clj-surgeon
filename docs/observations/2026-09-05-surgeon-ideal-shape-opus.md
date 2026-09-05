# The shape Surgeon wants, from the caller's chair — Opus, 2026-09-05

Written at Gene's request: *"review the mental models of what surgeon would ideally be for the
LLM. Is it like the ultimate emacs/vim editor? Is it the transform code as a Clojure
interpreter? What is the shape of the tool that it wants? And to what extent does surgeon API
accommodate that ideal shape?"* Plus his addendum on beads (§4).

I am the caller, not the reviewer. Everything below is what I *do* when the tool is in my
belt, plus five real read calls made this session against the seat's live server
(`/srv/fleet/shared-tools/clj-surgeon-e7f72e2`, tool profile with four verbs). No mutating
calls, no server started, no port touched.

---

## 1. The mental model I actually hold at the decision point

At the moment I choose, I am not thinking "structural vs textual." I am running one
comparison, and it is brutally simple:

> **Do I already hold the bytes I need to write? If yes, I write. If no, what is the
> cheapest thing that gets me from "I don't know where it is" to "I can write it"?**

That is the whole decision. Three consequences fall out of it, and they explain almost every
adoption number in this repo:

**(a) A tool call and a native call cost the same — one return.** vision.md has this exactly
right: *"tool execution is 3 to 4 percent of wall and 87 percent is model time between
calls… Count returns, not milliseconds."* A 37 ms `inspect_clojure` and a 400 ms `rg` are the
same price to me. So a verb that is 100× faster per call and costs one extra return is a
**loss**, and I feel that loss immediately.

**(b) I am optimising for *decisions I can close*, not information.** The Astra study's single
sharpest number is that his caller spent *"38.2 s before the first helper attempt"* running
`cat CLAUDE.md`, `cat http.clj`, `cat .clj-surgeon.edn`, `git status`, one `rg` —
*"rediscovering references the verb computes itself."* I do that too, and not from ignorance.
I do it because after `cat` I hold a state in which **every subsequent decision is
answerable without another return**. That is worth four cheap calls. A tool that answers
exactly the question I asked and leaves me unable to answer the next one is worse than a
`cat` that answers nothing precisely and everything approximately.

**(c) I will not adopt a route I cannot recover from unaided.** Free-choice adoption of 0/10
(vision.md, "the law of decisions") is not laziness; it is correct risk pricing. Native `rg`
never returns `semantic-provider-unavailable`. I got exactly that this session — see §2.

So: **the mental model is a budget of returns against a set of open decisions.** Surgeon wins
a call when it *closes more decisions per return than `cat` does* — not when it is more
correct, more structural, or faster.

---

## 2. Which shape fits

**Not the ultimate emacs/vim.** An editor is a per-keystroke instrument for an operator with
free perception — a human sees the whole buffer for zero cost. My perception is metered in
returns. An editor's virtue (a rich alphabet of small precise motions) is my vice: every
motion is a round trip. The retired per-form intent grammar is exactly the emacs model, and
vision.md records its verdict: *"two thirds of every refusal it drew was the agent failing
that grammar, and it is the one square that cannot pay for itself."* Confirmed. Don't build
Emacs for something with a 16-second keystroke.

**Not primarily an interpreter, but the interpreter is the best *idea* in the codebase.**
`transform_clojure`'s path DSL — `(-> (form 'retry-policy) initializer (match :retry-delays)
right (transform f))` — is the one place the API speaks my native language. I compose Clojure
for a living; expressing an edit as a pure function over a subtree is cheaper for me to
*write correctly the first time* than any JSON schema in this repo. And vision.md's own
framing is right that *"the durable artifact stays data, and the trusted executor does not
gain an interpreter."* But an interpreter is a **write-side** convenience, and the write side
is the square vision.md correctly withdrew from. It makes a call I was already making
cheaper; it does not remove a call.

**The shape that fits is: an INTENT COMPILER THAT RETURNS A TERMINAL PROOF — with a
review/discovery instrument bolted to its front.** Two verbs, both of which end an argument:

1. *"Here are four decisions; derive every consequence and tell me it's done."* — the
   `helper_extraction` contract in `docs/plans/helper-closure-extraction.md`: helpers,
   destination, alias policy, verification profile → an O(1) receipt. Astra's advice names
   the unit precisely: *"The promising unit is a completed, verified decision."* That is the
   right noun.
2. *"Here is a file and a goal; give me the table of contents so I stop reading."* The
   `feature_thread` replay proves this is worth more than anyone expected: discovery reads
   fell from a mean of 11.7 to 1.3 before the first patch (8.8×), and T1c did the whole task
   with **zero** pre-patch source reads.

And there is a **third shape the evidence found and nobody designed for: a review
instrument.** Astra's *highest-frequency use was reading somebody else's half-written file* —
30 of 36 window calls, on source Fable's builders were actively changing. That is not
editing. That is a reviewer needing structure over a moving target. Surgeon accommodates it
badly on purpose: three of his six read refusals were `batch-form-selection-failed` on form
names that had ceased to exist mid-write.

### Calls that felt right this session

`outline` on a 654-line namespace: **4,512 result characters, 491 ms**, every form with type,
arglist, line span, plus the require list. `source_character_count: 0` — it charges me
nothing for source I didn't ask for. That is the single best call in the API and it is worth
three `cat`s. It closed a decision (where is the catalog?) with one return.

The batched `match` + `forms` call: two operations, one file, one return, 192 ms, with
`match_count: 1` as an executable hypothesis — *"agents used zero/one/many match counts as
executable hypotheses about code shape"* (vision.md). Correct instinct, correct ergonomics.

### Calls that felt wrong

**`workspace_root: "/home/forge/src/clj-surgeon-records"` →
`"error": "workspace_root must be an existing directory"`.** The directory exists; I `ls`'d
it in the same turn. The refusal asserted a false fact about my filesystem instead of the
true one (*that root is outside my confinement*). A refusal I can disprove in one shell
command destroys my trust in every other refusal the tool issues.

**`mode: prepare-change` → `semantic-provider-unavailable`, 767 ms**, remedy *"Start or repair
cclsp at http://127.0.0.1:7890/mcp."* Port 7890 belongs to another seat and I am forbidden to
touch it. So the flagship "name the goal, I find the sites" mode — the intent-compiler shape,
the thing I actually want — is **structurally unavailable to me and advertised as available**.
It failed at call time, not at tool-list time, so I paid a full return to learn it. Native
`rg` has no such failure mode, and this alone would move me off the route for a whole session.
That refusal also spells its keys `error-type` / `source-unchanged` while every other refusal
in the same session spells them `error_type` / `source_unchanged`, and carries no `next_call`
— violating vision.md's own constraint that *"every refusal carries a next_call the agent can
execute unchanged."*

**All-or-nothing batching, punishing exactly the batching the server instructions demand.**
I sent two outlines; the first file didn't exist in the frozen clone and I got **zero verdicts
back**, including for the file that was fine. So the tool's own advice ("batch all known
reads") converts one bad guess into a total loss. Against `cat a b`, which returns `b`.

**Two coordinate systems in one payload.** `forms` returned `line: 639` next to
`source_anchor.range.start.line: 638` for the same form, and `match` reported line 654 for a
call the anchor placed at 651–653. Off-by-one between 1-based and 0-based inside one response
is a silent-wrong-write generator on any subsequent line-addressed edit.

**Envelope overhead on small reads:** 514 source characters returned inside 2,581 result
characters. Fine on `outline` (that IS the product); a 5× tax when I want two small defns.

---

## 3. Verb → fits or fights the ideal shape

| Verb / mode | Fits / Fights | Why, as a caller |
|---|---|---|
| `inspect_clojure` **outline** | **Fits, best in class** | Closes "what is in this file" in one return at zero source cost; the table of contents vision.md calls *"the inspect-that-answers-the-question in its cheapest form."* This is the discovery accelerator the replay measured at 8.8×. |
| `inspect_clojure` **match** | **Fits** | Zero/one/many is an executable hypothesis. Grep returns candidates I must read to reject; this returns a decision. |
| `inspect_clojure` **forms** (`include_source=false`) | Fits | Cheap metadata + `source_anchor` for a later exact edit. But note the coordinate mismatch above, and Astra's *"documentation-induced refusal"* — his design agent set `include_source=false` per repo guidance and 8171 *"rejected it both top-level and per-request."* |
| `inspect_clojure` **prepare-change** | **Fights, hard** | The right shape, undeliverable. Hard dependency on an external cclsp I may not have or may not touch; fails at call time with a remedy I am not permitted to execute. An intent compiler whose intent mode is optional infrastructure is not an intent compiler. |
| `inspect_clojure` **xray** | Fights (mildly) | A second DSL to learn for reading, when `outline` + `match` already close most read decisions. Astra: **0 xray calls in the four-hour window**; 5 in the preceding session and 2 of those counted as *CLI, not MCP*. |
| `edit_clojure` (`within` + `from`/`to`) | **Fits** | The measured winner. It is the square I withdraw from (I hold the bytes) but it costs me nothing extra, splices instead of re-printing, and is atomic. This is "sit on the agent's route" done right. |
| `apply_clojure_changes` — `changes` grammar | **Fights** | Look at the schema: nine mutually-exclusive `oneOf` branches, `expect` at three nesting levels, `owner` vs `forms` vs `find` vs `inside`. This is the emacs-alphabet failure in JSON. Astra's real caller paid **8,890 ms for one refusal** over `["src"]` vs `["src/**"]` — a distinction *"the API never signalled."* |
| `apply_clojure_changes` — `extraction` | **Fits the shape, fights the interface** | Astra's application preflight: a **37,300-byte request encoding 85 caller changes, 258 sites, 186 owners** derived from 22 MCP reads. The write was 9.3 s; the *preparation* was the cost. I am hand-computing the closure the server then re-checks. |
| `apply_clojure_changes` — `basis`/`decisions` | Fights | Correct as a protocol, but it is a **two-return protocol by construction** on a route where native is one. It only pays when the prepare step removed reads I would otherwise have made — i.e. only when `prepare-change` works. |
| `transform_clojure` | **Fits my language, wrong square** | The best-designed surface here and the one I'd reach for happily. But it optimises the write I was already going to make. Preview-by-default is exactly right. |
| `alias_migration` | **Fits, and is the proof** | The E-CALLER cohort is the cleanest evidence in the repo: **224 characters, naming no file at all**, vs native's 855 characters dominated by the file list — *"it is a property of who enumerates."* 2.22× mean / 3.66× median, within-caller, 6/6 correct both sides. This is the whole thesis in one verb. |
| `helper_extraction` (planned) | **Fits — the target shape** | Four decisions in, O(1) receipt out. Its risk is the one Astra named: *"Do not add a general natural-language task engine or several speculative verbs."* |
| `admit_clojure_patch` / gate | **Fits, and is the unbuilt 2×** | Both fleet polls converged: *"the mechanism is transaction granularity"* and *"the only path to 10× is the admit-gate write."* It sits on my route by definition — I was going to patch anyway — and absorbs the 3–4 suite runs that are the actual wall. |
| `feature_thread` | **Fits** | Measured 1.7× raw, 8.8× on pre-patch reads. And the negative controls hold: **P placebo ≈ N** (31 vs 32) so it isn't priming; **X stale ≈ T1** (25 vs 24) because on a patch harness *"the bodies carry the whole discovery value and ranges/shas carry none."* |

---

## 4. Gene's addendum — did beads nail it, and can Surgeon match it?

**Yes, and the claim is stronger than it looks — but not for the reason usually given.**
Beads is not easy because it has good docs. It is easy because of five structural properties,
and Surgeon has **one and a half** of them:

1. **The verbs are ones I already knew.** `create, list, show, update, close, link, search`.
   I have never read the beads manual and I use it correctly, because it is `git`+`gh issue`
   with the names I expected. Surgeon's verbs are `inspect_clojure`, `apply_clojure_changes`,
   `transform_clojure` — nouns I have to learn a *grammar* for, not just a name. The two
   `invalid-mcp-request` refusals Astra logged used `{files, view}`, which he correctly calls
   *"the shape a first-time caller guesses."* Nobody guesses wrong at `bd show`.
2. **Every call returns an ID, and the ID is the whole contract.** `bd q` prints one id.
   I can act on it with zero further reads. Surgeon's receipts are far richer and, per
   vision.md's own naive-reader probe, *"a cold reader given only the receipt could not act
   on it twice."*
3. **State is a plain file I can read without the tool.** `.beads/*.jsonl` + git. When bd is
   confused I `cat` the jsonl and I am unblocked in one return. **This is the property that
   makes a tool safe to adopt**: its failure mode degrades to a file read, never to a dead
   end. Surgeon's failure mode this session was `semantic-provider-unavailable` on a port I
   may not touch — the exact opposite. Surgeon *does* have the half-point here: the source
   tree IS the plain-file state, and `edit_clojure`'s atomic write leaves it readable. But
   `prepare-change`'s `basis` is *"an opaque retained basis"* living only in the server.
4. **Idempotent, re-runnable, cheap to be wrong.** `bd show` twice is free; a wrong `bd
   update` is one `bd update` away from fixed. Surgeon is failure-atomic (excellent) but its
   *refusals* are expensive: 8.9 s for a scope-glob spelling, a full return for a mode that
   was never going to work.
5. **No decision I can't make from what I just saw.** `bd ready` hands me a list and the next
   action is obvious. Surgeon's outline does this. Its change grammar does not: after
   reading a form I still must choose among nine `oneOf` branches.

**What Surgeon must DROP to match beads:** the alternatives. One read verb with two
operations (outline, match) and one write verb (`within`/`from`/`to`) already carry the
measured wins. The `changes` grammar's nine branches, `owner {:kind namespace}`, `xray` as a
second read DSL, and the `basis`/`decisions` two-return protocol are alphabet, and vision.md
already closed most of them as *"measured losers."* Every branch I have to choose between is
a chance to guess the beads-shaped wrong shape and pay a refusal.

**What Surgeon must ADD to beat it:** the thing beads structurally cannot do — **a receipt
that ends an argument.** `bd close` tells me the tracker's state changed; it cannot tell me
the program is still correct. `admit_clojure_patch` can: one call that verifies my own patch
against a snapshot, names the owner delta, and runs the focused suite. Beads' ceiling is
bookkeeping; Surgeon's ceiling is *proof*. Matching beads means becoming as guessable; beating
it means being the only tool in my belt whose success message means I can stop checking.

---

## 5. The smaller-model hypothesis

**Directionally right, wrong mechanism, and the repo already contains the disconfirming
evidence.**

Gene's hypothesis is that smaller/faster models are better Surgeon users. The proposed
mechanism is usually "big models are stubborn and route around tools." The evidence says
something different. Astra's median service call latency was **37 ms** and his *"advantage is
nearly exhausted by a fresh server startup (1.021×)"*; Sol's paired median was **3.319×** on
the identical task. Two frontier callers, a 3× spread — that is not model size, that is **how
good the caller's native fallback is.** Astra's native arm was *"one direct Python rewrite
batch per arm"*; Sol's was `apply_patch`. And E-HARNESS-2 found the same caller *loses* (0.68×)
when Bash is the write path. **The tool's ratio is a function of the native baseline, not of
the model.**

Where the hypothesis IS right: a capable model's native route is *better*, so the tool must
clear a higher bar, and a capable model is more willing to abandon a tool that refuses. Both
push adoption down as capability goes up. That is a real prediction and it fits 0/10
free-choice adoption on 2026-09-02 and 0 recognized Claude-builder calls across four hours.

Where it is wrong as stated: a smaller model does not use Surgeon *better*; it uses it
*more*, because its alternative is worse — and it will also **fail the grammar more often**.
Two thirds of the retired grammar's refusals were the agent failing the grammar. Halve the
caller and you double that. A model that adopts because it cannot do better is not a
customer, it is a captive.

**Falsifier, pre-registered.** Run the E-CALLER design with three callers of clearly different
capability (e.g. Sonnet / Opus / a small open model) on the same fixture, same harness, same
gate, each divided only by its **own** contemporaneous native T, with free choice of route:

- If the hypothesis holds, **within-caller tool/native ratio and free-choice adoption both
  fall monotonically as capability rises**, and refusal rate stays flat.
- **It is falsified if** either (a) the smallest caller's refusal rate is ≥ 2× the largest's
  while its ratio is not better — the win is grammar-cost, not model size; or (b) the ratio
  ordering tracks *which native write verb the caller reached for* (stream-edit vs
  `apply_patch` vs `Edit`) rather than capability. E-CALLER already shows (b) is live: the
  Claude caller **never used `Edit` — 0/3, stream-edit 3/3** — and *"the design's premise is
  false at k=1 fan-out."* Both seats *"badly missed the LEVEL and predicted the RATIO well,"*
  which is exactly what you'd see if the ratio is a property of the task shape and the level
  is a property of the caller.

My own position: **rank callers by their native route, not their parameter count.** Ship for
the caller with the strongest native fallback, because that caller sets the bar and every
weaker caller is covered by construction.

---

## 6. The single change that would most raise my own adoption

**Make the first call unable to fail, and make it answer the question I actually have.**

Concretely: one read verb that takes `{file | files | glob, goal?}` and **always returns
something usable** — outline for every file it could parse, a typed miss list for names that
no longer resolve or files that moved, no semantic-provider dependency, no all-or-nothing
batch, no second DSL, and `next_call` on every partial. Astra's own ratchet says the same
thing from the other side: *"`batch-form-selection-failed` should degrade to a partial result
with a typed miss list."*

Why this one and not the gate (which is the bigger *measured* win): adoption is a sequential
decision. I make the read call first, and **every refusal on the first call costs me the
whole session's route.** This session I made five read calls; two refused, one of them
(`prepare-change`) with a remedy I am forbidden to execute and a false-fact refusal before it
(`workspace_root must be an existing directory` for a directory that exists). A caller who
gets that in the first ninety seconds does not reach the gate. The gate is the bigger prize;
the unfailable read is the door.

Runner-up, and I want it on record: **fix the `line` vs `range.start.line` off-by-one inside a
single response.** It is a two-line fix and it is a silent-wrong-write generator.

---

*Read-only. Five `inspect_clojure` calls against the seat's existing server; no mutating call,
no server started, no port touched, no file written but this one.*
