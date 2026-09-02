---
parent: mcp-operation-contract-design
prefix: MCP-OP
---

 #Scoping the Managed Formatter to the Edited Forms

# #Order

Bead `clj-surgeon-46o`, opened by round two of
[close-losers-design.md](close-losers-design.md) and re-scoped by its round-three
finding: *"on every committing route, format only the spans the transaction
names."* This leaf implements that at the only unit where it is achievable — the
**enclosing top-level form** — and re-enables the formatter on the route round
two had to switch off.

# #The Defect

On the 2026-09-02 `l1` cohort a staging step reformatted whole staged files
before commit. Receipts in
`docs/observations/2026-09-02-captains-log-the-big-aha-and-reset.md`
("churn attributed") and
`docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`:

| receipt | finding |
|---|---|
| 08:57Z | two shipped diffs rewrote 425 lines across five files, semantically inert |
| 08:58Z | collateral **+508/-476** against the canonical **+59/-34** — nine times the review burden for the same 93 lines of work |
| 09:01Z | deterministic per file: `reducer_session.clj` by exactly 158 lines in three runs, `reducer_lab.clj` by exactly 156 in two |

`clj-surgeon.mcp-tool/execute-request-in-context!` installed a `prepare-compiled!`
hook that ran `clj-surgeon.mcp-formatter/format-candidates!` over every **complete
staged future file**, on the `changes`, prepared-`basis` and extraction routes.

Round two closed it by refusing: the drift gate refuses any commit whose final
bytes differ from what the request asked for, and the basis route's formatter was
switched off outright. That stopped the churn and cost managed formatting.

# #The Measured Premise

Round two believed span-scoped formatting was unachievable, "because
`standard-clojure-style fix` needs whole-file context to decide indentation."
The red team refuted it at the granularity that matters: formatting a **complete
top-level form in isolation** produces bytes identical to formatting that form
inside the full file. Only **sub-form fragments** differ, and only by the
starting column the fragment loses when cut out of its parent.

This leaf re-measured that claim over a whole real tree before building on it.

**Probe, 2026-09-02, `clj-surgeon` `src/` at `205e13a`, `@chrisoakman/standard-clojure-style` 0.29.0:**

| quantity | value |
|---|---|
| files | 68 |
| top-level forms formatted in isolation | **1735** |
| files where whole-file output equals isolated-form output spliced back | **67 / 68** |
| files a whole-file staging pass would change at all | 13 |

The single disagreement, `src/clj_surgeon/splice_drift.clj`, is **one blank line
between two top-level forms** that the whole-file pass collapses and the scoped
pass leaves alone. That byte is outside every form by definition, so leaving it
is not a divergence to fix — it is the guarantee this leaf makes. **No form,
anywhere in 1735, formatted differently in isolation than in place.** The premise
holds, and the negative that would have killed the bead did not appear.

**The census certifies this repository's style; the fixture carries what the
census cannot.** A 1735-form sweep of `src/` proves only that the check admits
what the formatter does *to code written the way this repository writes it*. Two
shapes absent from that corpus were where both remaining defects lived: an
unspaced `;;comment`, and a comment inside a `(:require ...)` list.

**So the probe is committed, and it carries those shapes.** Asserting a
measurement in prose and leaving its script in a scratch directory is the
failure class this project keeps naming: a receipt nobody can re-run.
`test/clj_surgeon/mcp_format_scope_real_test.clj` executes the **real pinned
binary** over the committed fixture `test-fixtures/format-scope/premise.clj` —
an ns form with unsorted requires and imports **and a comment inside its
`:require` list**, a defn with an **unspaced `;;comment`**, a defn with an
**end-of-line comment inside the form**, a defn containing an `if`, a defn
containing a non-commutative `-`, and a defn holding a **multi-line string with
runs of spaces** — and asserts that every form comes back admissible under the
check that bounds a scoped format, that the ns clauses really are sorted, that
the comment inside the `:require` list stays in front of the clause it
preceded, that the comment spacing the binary really does rewrite is admitted,
that the `if` branches are not reordered, that no byte inside the multi-line
string is touched, and that the scoped stage over the whole fixture leaves the
comment block between forms byte-identical.

It is gated: it runs only when `CLJ_SURGEON_REAL_FORMATTER` is set, which
`make mcp-test-formatter` does. `make mcp-test` leaves it unset and the test
prints the reason it skipped, so the ordinary suite never depends on `npx`, a
network, or an npm cache. Verified both ways on 2026-09-02: green with the gate
set (4194 assertions), cleanly skipped without it (4169).

# #The Fix

`clj-surgeon.format-scope` (pure, no I/O) and
`clj-surgeon.mcp-formatter/format-scoped-candidates!` replace the whole-file
staging call on the transaction routes.

For every staged future-source file:

0. **Refuse anything unmeasurable**, purely, in `format-scope/file-plan`, before
   a formatter is launched. Four decisions and three of them are refusals: an
   exemption recorded in the guard leaves the file alone; **no guard entry or no
   reference bytes** is `format-scope-unmeasurable`; **staged bytes that are not
   the guard's reference** are `format-scope-candidate-mismatch`, because some
   earlier staging step already churned the file and formatting it would rewrite
   the guard to point at that churned image — laundering the churn through the
   very commit gate that exists to catch it (red-team probe p3(c)); and a
   candidate that **does not parse while the guard names spans** is
   `format-scope-unparseable-candidate`, which was previously a silent no-op
   that still overwrote the guard (probes p3(b), p4(b2)).
1. **Select** the top-level forms the transaction actually edited. The
   transaction's own `:splice-guard` already carries, per file, the
   byte-preserving reference and the replacement spans in that reference's
   coordinates. `enclosing-form-spans` keeps the top-level forms those spans
   touch. Whitespace and comments **between** top-level forms are not forms and
   are never selected — which is the whole mechanism. A zero-length span on a
   form's boundary belongs to that form; a zero-length span in the whitespace
   between two forms encloses none, and nothing is staged.
2. **Stage** each selected form's text as its own file, keeping the source
   file's suffix so a `.cljc` form still reaches the formatter as `.cljc`. All
   forms across all files go through **one** formatter process, exactly as the
   whole-file stage did. A formatter that returns `nil`, returns a non-map, or
   throws is a **typed** `formatter-failed` refusal carrying the cause in
   `detail` and `source_unchanged true` (`MCP-OP-FMT-009`); before this, both
   paths produced an untyped error that reported `source_unchanged false` while
   the file on disk was in fact unchanged — a receipt telling the caller the
   opposite of the truth (red-team probe p4).
   Two forms that would derive the same staging key are a typed refusal
   (`format-scope-staging-collision`) rather than a silent shared result.
3. **Check** each returned form before it goes near a splice: it must parse to
   exactly one top-level form (`format-fragment-not-one-form`), and it must
   carry the same tokens and comments (`format-altered-form`). See "What bounds
   the formatter" below for why that check counts a bag rather than a sequence.
4. **Splice** the formatted text back at each form's exact original span, in
   **descending** order, so a form whose formatting changed its own length
   cannot move the offsets of a region that has not been spliced yet. The
   formatter's trailing newline — it was writing a file — is trimmed, because a
   top-level form's span never ends in a raw newline.
5. **Assert the splice arithmetic held.** Every byte of the pre-format candidate
   outside the formatted forms is carried into the post-format candidate
   **guaranteed by construction** — `splice-forms` builds the result by
   concatenating those exact gaps — and `scope-drift` asserts, gap by gap and
   without searching, that it did. Red-team probe p1 established that this
   cannot fail for any output of `splice-forms`, including garbage and the empty
   string. **It is a self-test of the splice, not a bound on the formatter, and
   it must not be described as a proof.** It is kept as a regression sentinel:
   if the splice arithmetic is ever changed and gets it wrong, this fires
   (`format-scope-drift`) instead of writing quietly.

Every refusal is typed and carries `source_unchanged true`,
`mutation_attempted false`, `write_authority false`.

# #What Bounds the Formatter

Round three's drift gate bounded the formatter by refusing every byte it
changed. That is unavailable to a leaf whose purpose is to let managed
formatting work, so it is replaced — and the replacement is **one check, not
two.**

**The only bound is the clause-normalised token stream (`MCP-OP-FMT-005`).**
The text the formatter returns for a form must carry the same tokens and
comments, **in the same order**, as the form it replaces, with exactly two
things the formatter owns normalised away. An unreadable stream on either side
is a refusal, never a match.

**Normalised away, 1: the clause order of an ns `(:require ...)` /
`(:import ...)` list.** Sorting those clauses is what
`standard-clojure-style fix` visibly does and must stay admissible. What is
sorted is whole clause **groups** — the leading comments plus the clause they
precede — and that is what makes it safe: a clause changing position normalises
away, while a symbol changing **which clause it lives in**, or a comment being
reattached to a different require, does not. Sorting bare siblings did admit
that comment reattachment, and it committed (red-team probe p8 i-b/i-c).

**Normalised away, 2: the spaces after a comment's semicolons.**
`standard-clojure-style` 0.29.0 rewrites `;;foo` to `;; foo`, `;;;foo` to
`;;; foo`, and an end-of-line `;;t` to `;; t`. Without this the check refused an
**ordinary** `apply_clojure_changes` transaction on the real wire route, with no
doubles, on source whose only sin was an unspaced comment — and the message
accused the formatter of changing code (red-team probe p11). The semicolons
themselves and the comment's text are still compared exactly; `;;` becoming
`;;;` is content, not layout.

**Why not a multiset.** This branch first shipped a token *bag* — order-blind
everywhere — on the argument that a bag "cannot tell `(- a b)` from `(- b a)`,
and that is a stated limit." A red-team review measured the alternative instead
of accepting the limit, and the limit was not a limit; it was an unmeasured
alternative that was strictly better on both axes. Four rewrites that a bag
admits, three of which changed what the code does:

| rewrite | bag | clause-normalised stream |
|---|---|---|
| `(if p (grant) (deny))` → `(if p (deny) (grant))` | admits | **refuses** |
| `(- credit debit)` → `(- debit credit)` | admits | **refuses** |
| `(compare (first x) (second))` → `(compare (first) (second x))` | admits | **refuses** |
| `:refer [check]` moved from `app.safe` to `app.unsafe` | admits | **refuses** |
| a comment reattached from one require clause to another | admits | **refuses** |
| requires sorted (sanctioned) | admits | admits |
| pure whitespace (sanctioned) | admits | admits |
| `;;foo` → `;; foo`, what 0.29.0 really does (sanctioned) | admits | admits |

And the cost of the stricter check, measured against the real
`standard-clojure-style` 0.29.0 over **all 1735 top-level forms** of this
repository formatted one at a time: **zero** false refusals, the same as the bag.
Re-measured on the branch as it now stands — **1740 forms**, formatter exit 0 on
every batch, **16** forms whose bytes the real binary changed — the shipped
check still refuses **zero**.
The bag bought nothing and admitted a semantic rewrite that probe p2b drove onto
disk end to end.

`format.reordered-form-count`, which published a reorder instead of refusing it,
is **removed**. Nothing consumed it, and a field whose only purpose was to
soften a check that should not have been soft is a receipt telling the reader
something happened that must not happen.

**A stated scope note, not a limit.** The clause normalisation keys on the head
keyword of a list, so a `(:require ...)`-shaped list anywhere — not only inside
an `ns` form — gets its siblings sorted before comparison. This is faithful to
the measured reference implementation, and it is why the end-to-end p2b witness
places its clause list inside a `defn`: no accepted wire shape edits an `ns`
form, so that is the only way to drive the same code path through
`execute-request!`. Narrowing the rule to ns forms is an open decision below.

# #What This Changes About the Drift Gate, Stated Plainly

This is the part a reviewer should read twice.

`MCP-OP-CLOSE-021` gates a commit on `byte_drift_from_expected`: the candidate
must equal the expected post-image, which is the caller's own splice. A scoped
format that actually changes a form's bytes would fail that gate, so this leaf
**replaces the transaction's splice guard with the post-format image and the
formatted form spans** (`MCP-OP-FMT-008`). The commit gate therefore measures
every *later* staging step against the bytes the scoped format produced, and
reports `byte_drift_from_expected` 0 for an ordinary edit with the formatter on.

Consequences, both directions:

- **What is preserved.** The gate still runs on every committing route, still
  walks the files about to be written, still refuses `splice-guard-missing`, and
  still refuses any staging step that runs after the formatter. `MCP-OP-CLOSE-021`
  is still witnessed directly at the pure decision function and end to end
  through `intent-transaction/execute-change!` with an injected
  `prepare-compiled!` (`splice_drift_test/a-whole-file-reformat-between-compile-and-commit-is-refused`).
- **What is given up.** The formatter is no longer bounded by "may not change a
  byte." It is bounded by **exactly one** thing, the clause-normalised token
  stream. Nothing else bounds it: the scope of the splice is guaranteed by
  construction rather than checked, and `scope-drift` is a self-test of that
  construction. A reviewer weighing this leaf should weigh that one check. A
  formatter that rewrites the
  caller's own replacement text *as layout* — R5's tabbing probe — now commits,
  and its witness in `mcp_close_losers_test` was re-scoped accordingly from
  `a-span-covering-the-whole-form-is-still-measured` to
  `a-span-covering-the-whole-form-is-still-bounded`, with a sibling test proving
  the same formatter cannot reach a form the change did not edit.

That is a deliberate narrowing of a ratified gate and it belongs in a review, not
in a commit message. It is the trade the close-losers design itself named: *"the
durable fix is bead `clj-surgeon-46o` — span-scoped formatting — after which the
refusal would stop firing on ordinary work."*

# #Routes

| route | formatter reaches | before this leaf | now |
|---|---|---|---|
| direct `changes` | whole staged file | whole file, then refused by the drift gate | the edited top-level forms only |
| prepared **basis** | nothing (disabled round two) | disabled | the edited top-level forms only |
| **extraction** | files it creates only | unchanged | unchanged |
| `edits`, `require_change`, `delete_owners`, `create_files`, `programs`, `symbol_migration` | nothing (exempt) | unchanged | unchanged |

**Editor gestures stay exempt.** That exemption predates the churn finding and
lifting it is a separate decision with its own blast radius; it is listed as an
open decision below rather than taken here. The measured winner `require_change`
across three namespaces is witnessed committing with both drift numbers zero and
**zero formatter calls**, so the exemption is proved to have survived the
rewiring.

**Extraction is unchanged.** Its formatter already receives only
`(select-keys (:future-sources compiled) (:created-files compiled))` — files that
are wholly the edit — and its modified files carry `{:reference source :spans []}`
guards. Under `format-scoped-candidates!` an empty span list selects no forms, so
the scoped stage would be a no-op there anyway.

# #Verification

- `test/clj_surgeon/format_scope_test.clj` — the pure arithmetic and the pure
  decisions: form spans excluding inter-form comments, region selection
  including the zero-length boundary case, descending splice with a form that
  grows, the splice self-test and its two failure shapes, the clause-normalised
  stream against the four bag-preserving corruptions and the two sanctioned
  changes, and `file-plan`'s three refusals. Runs under babashka in
  `make test-fast`.
- `test/clj_surgeon/mcp_format_scope_test.clj` — the l1 churn fixture with its
  measured byte counts, an edit inside one defn, two edits in two forms, a form
  that grows, `require_change` across three namespaces, the prepared-basis
  route, the two bag-preserving semantic rewrites driven end to end through
  `execute-request!`, the churned and unparseable candidates, and the typed
  refusals including a formatter that returns `nil`, returns a non-map, or
  throws.
- `test/clj_surgeon/mcp_format_scope_real_test.clj` — the premise probe,
  executed against the **real pinned binary** over
  `test-fixtures/format-scope/premise.clj`. Gated on
  `CLJ_SURGEON_REAL_FORMATTER`; run it with `make mcp-test-formatter`.
- `test/clj_surgeon/mcp_close_losers_test.clj` — the re-scoped R5 witness and its
  new sibling.
- `test/clj_surgeon/mcp_formatter_test.clj` — the version pin, and that the
  pinned `check` counterpart is still recognised and removed from the profile.
- `test/clj_surgeon/mcp_tool_test.clj` —
  `same-file-form-edits-format-commit-and-undo-once` now witnesses
  `format-fragment-not-one-form`, because the refusal it used to witness is
  unreachable through a formatter that only ever sees one form.

**Fails-first, measured 2026-09-02.** Every witness was run against an
implementation without the thing it witnesses.

| harness | failing assertions | tests |
|---|---|---|
| base `mcp_tool.clj` wiring only (whole-file staging, basis formatter off) | **21** | 6 |
| base wiring **plus** the pre-red-team implementation (token bag, no `file-plan` refusals, no typed formatter failure, unpinned version) | **39** | 13 |
| `sig` without the comment-spacing normalisation, sorting bare siblings instead of clause groups — the two defects the real binary found | **4** pure, **5** against the real binary | 3 |

The second row is the honest number for the branch as it now stands: 37
failures and 2 errors across `a-formatter-that-reorders-tokens-inside-a-form-is-refused`,
`a-bag-preserving-semantic-rewrite-does-not-commit`,
`a-formatter-that-changes-code-rather-than-layout-is-refused`,
`the-default-formatter-command-is-version-pinned`,
`staged-formatting-removes-only-its-redundant-post-commit-check`,
`a-staged-candidate-that-does-not-match-its-guard-reference-is-refused`,
`an-unparseable-candidate-with-named-spans-is-refused`,
`a-file-churned-before-the-formatter-is-refused-not-laundered`,
`an-unparseable-staged-file-is-refused-even-beside-a-good-one`,
`a-formatter-that-returns-nil-or-throws-is-a-typed-refusal`,
`an-edit-inside-one-defn-reformats-only-that-defn`,
`two-edits-in-two-forms-format-both-and-nothing-between-them`, and
`a-form-that-grows-does-not-corrupt-the-form-after-it`.

The third row is the N1/N2 pair, measured 2026-09-02:
`the-real-formatters-comment-spacing-is-layout` loses 3 assertions,
`a-comment-reattached-to-a-different-require-clause-is-refused` loses 1, and
`the-real-pinned-formatter-agrees-with-the-clause-normalised-stream` — run
against the real binary over the extended fixture — loses 5 (3 failures, 2
errors). That last one is the point of committing the fixture: the 1740-form
census of this repository refuses nothing either way, because neither defect's
shape occurs in this repository's own style.

An earlier record in this document said 13 assertions. That was measured against
an earlier and smaller test set, and it was corrected to 23 by the red team for
the set that existed then; both are superseded by the two rows above, which are
the numbers for the tests actually in the branch.

- `make mcp-test`, `make test-fast`, and `make mcp-test-formatter`.

# #Open Decisions

**Should editor gestures be formatted too?** They were exempted because the only
formatter available restaged whole files. That reason is gone. Lifting the
exemption would make `edits` and `require_change` format the forms they touch —
which is arguably what a managed formatter is for — but it changes the
observable bytes of the two measured winners, so it wants its own cohort and
Gene's call. Two tests currently assert zero formatter calls on those routes and
would be the first things to change.

**Should the clause normalisation be narrowed to `ns` forms?** It keys on a
list's head keyword, so any `(:require ...)`- or `(:import ...)`-shaped list has
its siblings sorted before comparison, wherever it sits. That is faithful to the
measured reference and costs nothing on this repository, but it is a slightly
wider hole than the sanctioned behaviour requires. Narrowing it means teaching
the check what an `ns` form is.

**Follow-up bead, F5: an exempt guard entry is rewritten when another file in
the same transaction has forms.** `format-scoped-candidates!` runs its splice
reduce over every planned file, so a file whose entry says
`{:exempt :created-file}` comes out carrying `{:reference <source> :spans []}`.
The bytes are identical and the downstream gate still passes, so nothing is
mis-measured today — but an exemption is a *decision*, and converting it into a
measured entry loses that decision. Not built here: it is a change to the reduce
that wants its own witness. Red-team probe p3(a2).

**Follow-up bead, F7: one temp file per form does not scale.**
`format-candidates!` stages each fragment as its own temp file and passes them
all on one command line. Measured: a single invocation with all 1735 fragments
of this repository exits **249**; batches of 150 exit 0. Real transactions edit
a handful of forms, so nothing in the field hits this, but the staging step has
no batching and no documented ceiling. Red-team probe p6.

**A formatter that returns `{:ok true}` with no `:future-sources`** surfaces as
`format-fragment-not-one-form` rather than `formatter-failed`. It fails closed,
writes nothing, and reports `source_unchanged true`, so it is safe — but the
reason it gives is about the wrong thing. Red-team probe p4.

**Should the reordering check be strict for a project that wants layout-only?**
Today a project cannot ask for "no reordering at all", because the ns clause
normalisation is unconditional. Not built, because no measured shape asked for
it.

# #Non-Goals

- The SCI allowlist, the evaluation fence, path confinement, and the kernel
  commit path (`intent-transaction/commit-compiled!`) are untouched.
- No public input schema changes.
- No speed claim. No arm was run for this change.
