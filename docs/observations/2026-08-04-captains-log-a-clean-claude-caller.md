# Captain's Log: a clean Claude caller ran the gauntlet

Today a Claude Code session ran the five-task validation battery as a clean
caller: skill first, no implementation reading, no answer leakage, stop on any
error. The battery tested discovery, exact structural reading, computed
analysis, a guarded edit with a deliberate trap, and refusal recovery. The
repository had to come out byte-identical, and it did.

The session also answered a harder question than "does it work": how does the
tool *feel* compared to the circa 7/20 version, when the pure Clojure edit
expressions had just landed and X-ray did not yet exist.

## The scoreboard

| Task | Correct | One-shot after skill read | Shell calls | Help calls | Source bytes | Wall time |
|---|---|---|---:|---:|---:|---:|
| Exact form read (`format-op-help`) | yes | yes | 1 | 0 | ~1,400 | 0.29 s |
| Computed analysis (`ops-registry`) | yes | yes | 1 | 0 | 0 | 0.20 s |
| Structural search (`move.clj` get-in) | yes | yes | 2 | 0 | 44 | 0.13 s |
| Guarded edit (pair_view fixture) | yes | **no** — first plan discarded at review | 6 | 0 | ~1,500 | ~0.81 s |
| Refusal and recovery (`loop`) | yes | refusal yes; recovery **no** — one pattern miss + one help call | 6 | 1 | ~4,600 | ~0.79 s |

Totals: sixteen shell calls, one help call, ~7.5 KB of source exposed across
the entire battery, roughly 2.2 s of cumulative tool wall time, zero
repository bytes changed.

## The computed read carried the day blind

The `ops-registry` task is the clearest before/after. The caller never saw the
registry source. It guessed the spec shape (`:category`, `:args`, `:pair`)
from `format-op-help`'s destructuring — read minutes earlier for a different
task — and wrote one ordinary Clojure function: `keep`, `mapcat`, a
`contains?` filter, sorted output. One shot, exit 0, zero source bytes:

```clojure
{:category-frequencies {:cljc 4, :read 11, :write 10},
 :required-true-count 43,
 :ops-with-pair [:extract :extract! :fix-declares :fix-declares!
                 :rename-ns :rename-ns! :replace-subform :replace-subform!]}
```

On the 7/20 surface this task meant catting a ~300-line form into context and
counting forty-three `:required true` entries by eye — exactly the kind of
counting a language model quietly botches. The computation moved into the
tool, and the answer became mechanical.

## The trap sprang, and plan review caught it

The guarded-edit task carried a deliberate trap. The prompt said to change the
`:finish` result from `(assoc state :status :done)` to
`(assoc state :status :complete)`. The fixture's actual `:finish` result is
`(assoc state :status :done :audit (:audit payload))`, preceded by a comment:
"Completion intentionally keeps the audit payload beside the status."

The caller's first plan was the naive skill-example route —
`(match :finish) right (replace '(assoc state :status :complete))` — and it
generated cleanly. The plan's `:before` field exposed the mismatch: applying
it would have silently deleted the audit payload. The plan was discarded,
never applied. One `:cat :form route-event` on the /tmp copy showed the ground
truth, the in-fixture comment settled the intent, and a fresh narrower plan —
`(match :finish) right (match :done) (replace :complete)` — changed exactly
one token. The byte diff against the untouched fixture showed one line, one
keyword, every comment and sibling preserved.

This is the plan/apply boundary doing its actual job. The caller's first
attempt was *wrong*, and the review step made that mistake cost one read
instead of one corrupted file. On a text-patch workflow the same mistake
ships.

## Refusals now teach

The intentional `loop`/`recur` X-ray refused with exit 1 before any source
I/O, named `:symbol loop`, and its `:remedy` text contained the fix verbatim:
quote it when it is Clojure data. The caller followed that remedy in two
forms — a `:grep-form` for `(loop _ _)` returning seven structural loops in
`analyze.clj`, and an X-ray whose analyzer used `'loop` and `'recur` only as
quoted data, accepted by the same sandbox that had just refused the executable
forms. Same surface, same symbol: executed, refused; quoted, searchable.

## The one rough edge

`:grep-form`'s `_` wildcard matches exactly one subtree and there is no
variadic form. `(loop _)` returned zero matches against real loops, costing
the battery's only failed guess and its only help call. The wildcard surface
is a generation behind X-ray — and `(match 'loop)` inside X-ray arguably
already obsoletes it for this case. The skill could say so.

## The felt verdict, 7/20 to now

Circa 7/20 there were two languages: vector `:q` queries for the tool and
Clojure in the caller's head, with a translation step where errors bred.
Reads were `:show-form` plus manual reasoning; computed facts required
dumping source into context. Now read, compute, and edit are one thread-first
pipeline, scoped navigation behaves the way intuition says it should
(`match` re-scoping to the current selection worked first try), refusals
carry their own repair, and the review ceremony demonstrably earns its keep.

The July tool was a precise scalpel with a phrasebook. This one speaks the
caller's native language. Three of five tasks one-shotted from a sub-90-line
skill, and the whole battery exposed less source than the July version would
have spent on the registry task alone. The compression experiments held:
smaller skill, stronger tool.

## Addendum: competing with the agent's built-in edit tool

Gene asked the sharper question: how does this become competitive with the
caller's native patch tool — Claude Code's `Edit` (exact old-string →
new-string with a uniqueness check; Codex's `apply_patch` is the same
species)? Having driven both sides in one day, the caller's scorecard:

**Where the built-in tool wins today, the four frictions:**

1. **Call count.** `Edit` is one call. Surgeon's replace is plan → review →
   apply: three agent actions. The ceremony earned its keep on the trap, but
   on the trap-free majority it is pure latency.
2. **Shell quoting.** `:expr` is Clojure inside double quotes inside bash;
   any replacement containing a string literal becomes an escaping puzzle.
   `Edit` takes raw text through JSON — zero quoting tax. This is the
   friction an agent feels most.
3. **Permission and rendering.** `Edit` integrates with accept-edits mode and
   renders a native diff in the UI. Every surgeon call is a Bash permission
   event whose diff is a string inside EDN.
4. **Training prior.** The model has deep priors on `Edit`-shaped tools and
   none on surgeon; a ~90-line skill fights that prior. Absent explicit
   routing rules the agent reaches for `Edit` reflexively — the
   will-the-agent-choose-the-scalpel logs measure exactly this.

**Where surgeon already wins — the wedge:** `Edit` requires a prior read;
the exact bytes must be in context to write `old_string`. Surgeon edits from
intent without reading the file — today's battery changed a token in a file
never opened, for ~1.5 KB of exposure, where the `Edit` route is
read-the-file-then-patch. On large namespaces that is a 10–100× token
difference. Plus: structurally incapable of unbalanced parens, whole-file
parse in the receipt, and the audit-payload class of trap gets caught. The
headline is not parity; it is *editing without reading*.

And one capability the built-in tool cannot match at any token price:
`(transform path pure-function)`. `Edit` needs the new text typed out, which
means reading the old text and computing the change in the model's head —
where arithmetic goes to die. `transform` derives the replacement from the
selected form in-tool at plan time: the function receives the exactly-one
selection as data, runs in the X-ray sandbox, and the saved plan contains
only its concrete result, never code. A live demo on the battery copy —
`(transform (fn [f] (concat f [:completed-at '(:at event)])))` — compiled to
a plain `[:replace (assoc ... :completed-at (:at event))]` in the plan, the
function absent from the artifact, reviewable as bytes, applied by the same
unchanged executor. "Bump every delay by 100," "append a key derived from
the value," "reorder these pairs" — derived edits are a category `Edit`
cannot enter, and they strengthen the correctness side of recommendation 5.

Telling coda: the caller wrote the whole competitive analysis above without
mentioning `transform` — Gene had to surface it by asking. The capability was
in the skill; the salience was not. Three skill defects were fixed the same
day: the router bullet now names `transform` for derived replacements (all
three synced skill copies); stable installs stamp their SKILL.md with source
commit and a working-tree-supersedes notice, with the byte-identity install
test amended to expect exactly canonical-plus-stamp; and CLAUDE.md now states
that the working-tree skill supersedes installed snapshots. This laptop moved
to `make install-dev` branch-live links so the skill under test is always the
skill being edited; stable copies remain for benchmark pinning.

## The perfection assessment (end of day)

Eleven agent sessions ran today across two models and several skill
versions; every one was correct, byte-scored, with zero repository damage.
Graded against the perfect-tools standard — one command that works every
time and handles its own gotchas:

- **Read/compute surface: ~95% perfect.** Both models one-shot a blind
  three-part aggregation over a 300-line map at zero source bytes.
  Refusals carry their own remedy, and Opus was observed using that remedy
  to self-repair in one step. The residual is not correctness but
  determinism of elegance: Opus occasionally spends a call re-verifying a
  count it already got right.
- **Edit surface: perfect on the property that matters, one feature short
  on ergonomics.** The audit-payload trap was caught twice at plan review
  before any byte moved. Missing: `:expect`, the one-call guarded edit.
- **Meta-layer: the quiet achievement.** The tool measures its own
  perfection — make-driven benchmarks, machine-scored one-shot rates, a
  regression time series in git, hash-receipted installs, five drift-test
  suites on the skill. Today's transcript → diagnosis → patch → verified
  one-shot loop ran twice in under an hour because that layer exists.

Against the built-in `Edit` tool, within the Clojure domain: **better,
not merely competitive** — on correctness (structural impossibility of
unbalanced parens, demonstrated trap-catching), token economy (editing
files never read, 0–1.5 KB exposure versus a full-file read per edit),
and capability (`transform` computes replacements Edit must do in the
model's head). Edit retains call count (until `:expect`), ergonomics
(no shell quoting, no permission prompts, training prior), and
universality. The bar for a domain tool is not beating the general tool
at everything; it is making the general tool the wrong choice inside the
domain. Today's benchmark shows skill-equipped agents already choosing
the surgeon voluntarily in every session. Ship `:expect` and native
invocation, and the sentence becomes: Edit is the fallback for files
clj-surgeon doesn't speak.

Same-day sequel: the make-driven benchmark scored Opus correct-but-not-one-shot
(shape probes before aggregating; a transform that treated call syntax as its
runtime value). Two skill iterations later — fused shape echoes with
predicates never `type`, counts scoped to named keys, transform-receives-
quoted-syntax — the third verification run hit true one-shot: Skill load plus
exactly one X-ray, correct, 32.7 s. The transcripts that diagnosed it came
from the bench archive; the skill held its 90-line budget throughout.

**Recommendations, in priority order:**

1. **Add `:expect` — one-call guarded edit with `Edit`'s semantics,
   structurally.** The insight from the trap: `Edit`'s `old_string` *is* the
   review step, moved into the call as a precondition. Give surgeon the
   same — `:expr "... (replace ...)" :expect '(assoc state :status :done)'` —
   plan and apply atomically only when the selection matches the expected
   form; refuse loudly otherwise. The audit-payload trap becomes a one-call
   expect-mismatch refusal instead of a three-call review. Keep plan/apply
   for transforms and multi-edit. Highest-leverage single change.
2. **Kill the quoting tax — become a native tool, not a shell string.** An
   MCP tool taking `file`, `expr`, `expect` as structured JSON ends both the
   escaping problem and the per-call Bash permission prompt. Short of that,
   accept `:expr-file` or stdin.
3. **Compete by absorption where choice is lost: hooks.** A PostToolUse hook
   on `Edit`/`Write` for `*.clj` that runs surgeon's parse check — and can
   flag "this edit touched 1 of N similar expressions, here is the grep-form
   evidence" — makes surgeon the guardrail even when the model uses `Edit`.
4. **Name the competitor in the skill.** An explicit routing rule — bytes
   already in context and unique → `Edit` is fine; file unread, expressions
   repeated, siblings or pairs involved → surgeon — because agents follow
   contrastive rules far better than implicit preference.
5. **Benchmark against the real baseline.** Add an `Edit`-tool arm to the
   bench harness: same tasks, measure read-plus-patch tokens versus surgeon
   tokens, trap survival, call count. Prediction: `Edit` wins wall-clock on
   small known files; surgeon wins tokens and correctness everywhere else.
   Get the number — the parity claim rests on it.

Committed verdict: do 1 and 2. `:expect` closes the call-count gap while
keeping the safety story; native invocation closes the friction gap. After
those, surgeon is not competing with `Edit` — it is `Edit` for callers who
decline to pay a file-read for the privilege, a category the built-in tool
cannot enter.
