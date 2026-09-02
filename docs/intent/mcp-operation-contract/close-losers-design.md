---
parent: mcp-operation-contract-design
prefix: MCP-OP
---

 #Closing the Measured Losers at the MCP Server

# #Order

Gene, 2026-09-02, verbatim from the terminal: "Close all surgeon paths that are
losers." This leaf implements that order **at the server** as typed refusals with
executable redirects, not as deletions. The winners share the same public verbs
(`apply_clojure_changes`, `edit_clojure`), so removing a tool would remove the
winners with the losers. A typed refusal closes exactly the measured shape and
hands the caller the measured winner in the same return.

# #Measured Evidence

All figures below are receipts in
`docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`
(81 attested arm-runs, two blind judges, cohort `l1`: a 21-owner change across 11
namespaces).

| receipt | finding |
|---|---|
| 08:51Z | native 215 s mean; shipped Surgeon arm 457 s (2.1x); substitution mandate 625 s (2.9x) |
| 08:57Z | two shipped diffs rewrote **425 lines across five files**, semantically inert; one mandate diff did the same to **153 lines of one file**; zero native diffs did |
| 08:58Z | collateral +508/-476 against the canonical +59/-34 — nine times the review burden for the same 93 lines of work |
| 09:01Z | churn is deterministic per file (`reducer_session.clj` reformatted by exactly 158 lines in three runs, `reducer_lab.clj` by exactly 156 in two). Separation by verb shape, **3 of 3 against 0 of 4**: every run using `apply_clojure_changes` with `owner {kind "namespace"}` plus `find` and insert or replace (A-1, A-3), or `find` plus `forms` plus `replace` (Y-3), reformatted. Every run using `within` plus `from`/`to` (A-0, A-4, Y-0), or the dedicated `require_change` verb (Y-5, nine namespaces), did not. |
| `docs/vision.md` "The battlefield" | "Measured losers, closed: owner-kind-namespace insertion (re-prints the whole file), per-form writes for fan-out, the CLI wrapper as an MCP substitute, prompt mandates." |

# #Correction to the 09:01Z Mechanism Attribution

The 09:01Z receipt attributed the churn to "Surgeon's printer re-emits the whole
file under owner kind `namespace` and forms-scoped replace." **The verb-shape
separation it measured is correct. The named mechanism is not.**

Direct probe of the pure compiler, `clj-surgeon.intent-transaction/compile-transaction`,
on a source literal carrying deliberate non-canonical whitespace
(`(str/join ","    [x x])`, `(defn b [y]\n      (set/union #{y}   #{1}))`) and a
non-canonical `:import` layout:

- owner-kind-`namespace` + `find` + `insert_after`: every byte outside the
  inserted span preserved exactly, including the odd indentation.
- forms-scoped `find` + `forms` + `replace`: every byte outside the replaced span
  preserved exactly, including the odd indentation in the *other* form.

The compiler splices. `apply-raw-edit` is a literal `subs`/`str` splice for
deletions and insertions; `z/root-string` is format-preserving for replacements.
The whole-file re-emission comes from one stage further downstream:

`clj-surgeon.mcp-tool/execute-request-in-context!` installs `prepare-compiled!`
**only when the request is not an editor gesture**:

```clojure
editor-gesture? (some #(contains? normalized-params %)
                      [:edits :programs :delete_owners :create_files
                       :symbol_migration :require_change])
...
config (if (and (:formatter config) (not editor-gesture?)) ... )
```

That hook runs `clj-surgeon.mcp-formatter/format-candidates!`, which passes each
**complete staged future file** through
`npx @chrisoakman/standard-clojure-style fix`. So the `changes` route is
whole-file reformatted before commit and the `edits` / `require_change` routes are
not — which reproduces the measured 3-of-3 against 0-of-4 separation exactly,
including why Y-5 (`require_change`, nine namespaces) had zero churn.

The 09:01Z receipt ruled the formatter out because "zero cljfmt, zprint,
standard-clj, cljstyle, clojure-lsp format invocations" appeared in the rollouts.
That count is of the **agent's** tool calls. It is blind to Surgeon's own
subprocess, which is exactly the class the test doctrine names: a receipt whose
evidence source can omit its subject.

Two consequences for this design:

1. The order's escape clause for case 2 — "UNLESS the implementation can splice
   the replacement into the original source text byte-preserving outside the
   matched span" — is already satisfied by the compiler, at zero lines of change.
   So the forms-scoped shape is **not** refused on its verb shape. It is gated on
   the measured property instead: `byte_drift_outside_span` must be 0.
2. The gate that actually catches the l1 churn must run on the **final** future
   bytes, after `prepare-compiled!`, not on the compiler's output. It does.

# #Three Defects the Red Team Found, and What They Change

An adversarial review of the first implementation returned NO-GO with executed
probes. All three findings reproduced exactly; all three are fixed, and each is
now a witness test with its own intent id. They are recorded here because two of
them are the same failure class the test doctrine names.

**1. The refusal was dead on the real server (`MCP-OP-CLOSE-013`).**
`mcp-tool/execute-request!` round-trips every request through
`json/parse-string ... true`, so the map that reaches validation carries
**keyword** keys. The refusal read string keys (`(get change "owner")`) and
returned `nil` for every real request, while its own unit tests — which built
string-key maps by hand — passed. Measured before the fix:

```text
validate-tool-params, string keys  -> :whole-file-reprint-refused
validate-tool-params, keyword keys -> nil, ok = true
```

This is *the verifier blind to its own subject*: a green suite proving nothing
about the running system. `mcp-contract` had solved this long ago with its
`field`/`present?` accessors; the refusal simply did not use one. The fix
normalizes keys once at the entry of the refusal, and the witness now drives
`mcp-tool/execute-request!` — the same entry point production uses — and asserts
both the refusal and that zero bytes were written. A second existing test,
`namespace-owner-transaction-crosses-the-complete-writer-boundary`, had been
committing the exact closed loser through the real path and passing; it is now
`namespace-owner-transaction-is-refused-across-the-writer-boundary`, and its
former green was itself evidence the refusal was dead.

**2. The drift arithmetic scored churn as zero (`MCP-OP-CLOSE-010`, `-011`, `-012`).**
Gap alignment used `str/index-of` from a moving cursor, which lets a gap float to
wherever it happens to match. Three probes scored `0 :exact` before the fix:

| probe | reference → candidate | before | after |
|---|---|---|---|
| F1 junk adjacent to a span | `AA\nSPAN\nBB` → `AA\nSPANJUNKJUNKJUNK\nBB` | 0 `:exact` | 12 `:unlocatable` |
| F2 whitespace after an edited form | `…(b)\n(keep)…` → `…(b)   \n(keep)…` | 0 `:exact` | 3 `:unlocatable` |
| F3 insertion at a zero-length span | `abcdef` → `abZZZcdef` | 0 `:exact` | 3 `:unlocatable` |

In each case a search-based alignment let the span absorb content that was
outside it. The fix is positional and does no searching at all: each untouched
gap must occur at **exactly** the offset it occupies in the reference, and the
candidate must end where the reference ends. A span's *content* may still differ
freely; its *length* may not, because once a span changes length every offset
after it is unknowable and the honest answer is `unlocatable`, which fails closed
at commit. That is the doctrine's rule — when evidence cannot distinguish two
states, return unverified rather than a guess.

**3. `assoc_entry` evaded the refusal (`MCP-OP-CLOSE-014`).**
`owner {kind "namespace"}` plus `find` plus `assoc_entry` restages the whole file
exactly as `replace` does and was not refused. All four restaging actions are now
enumerated and refused. `delete`, `rename_binding`, and an insertion without
`find` are already refused under a namespace owner by the kernel's own typed
reasons and keep them.

# #Round Two: The Committing Routes the Gate Did Not Cover

A second adversarial review confirmed round one's fixes on the `changes` and
`edits` routes — keys refused in five shapes, the drift oracle catching
same-length byte changes, CRLF, BOM, trailing newline, tabs and multibyte,
`:unlocatable` refusing at commit, second-file drift refusing the whole
transaction — and independently confirmed the mechanism by instrumenting
`format-candidates!`. It then returned NO-GO on three further points.

**The drift gate covered one committing route out of three.** Probe q4 committed
whole-file formatter churn with `ok true` and no drift field **on the route the
server's own instructions recommend** (`prepare-change` → basis). Two routes
commit without passing through `execute-change-with-context!`:

| route | formatter reaches | gated before | gated now |
|---|---|---|---|
| direct `changes` | whole staged file | yes | yes |
| `edits` and other editor gestures | nothing (exempt) | n/a | yes |
| prepared **basis** | whole staged file | **no** | yes |
| **extraction** | created files only | **no** | yes |

The requested fix is the formatter's scope (bead `clj-surgeon-46o`): on every
committing route, format only the spans the transaction names. Span-scoped
formatting is not achievable in this pass — `standard-clojure-style fix` needs
whole-file context to decide indentation, so feeding it a fragment changes the
output it produces. The authorized fallback is taken instead:

*Round three note: the premise that span-scoped formatting is unachievable was
measured and refuted — see "The Formatter Finding That Re-scopes Bead 46o". The
fallbacks below stand as shipped, but they are a stopgap, not the answer.*

- **Basis**: whole-file formatting is now disabled, exactly as it already is for
  editor gestures, by widening the formatter-exemption predicate in
  `mcp-tool/execute-request-in-context!` to include `basis?`. The route also
  carries the same `:splice-guard` the direct route carries — `compile-addressed-transaction`
  runs through the same `compile-file`, so the spans were already computed and
  simply were not published — and `apply-basis!` now calls `gate-splice-drift`
  before it commits, and publishes `byte_drift_outside_span`.
- **Extraction**: its formatter is already span-scoped in the only sense that
  matters. `execute-extraction!` passes `(select-keys (:future-sources compiled)
  (:created-files compiled))`, so the formatter only ever sees files the
  extraction *creates*, which are wholly the edit and have no untouched source to
  churn. It is therefore not disabled. The drift gate is added as a second,
  independent backstop proving every file the extraction *modifies* reaches
  commit byte-identical to the bytes its compiler produced.
- **Basis and the closed-shape refusal**: a basis request never reaches
  `validate-tool-params`, so `refuse-closed-losers` was simply absent on that
  route. It is now applied explicitly there and its result normalized, so the
  refusal and its redirect look identical to the caller on both routes.

**The gate failed open with no guard (`MCP-OP-CLOSE-018`).** `gate-splice-drift`
scored a missing `:splice-guard` as drift 0 and returned the transaction
untouched — precisely the fail-open the gate exists to prevent, and precisely how
the basis route would have stayed silent had the guard been forgotten. A
committing transaction that carries future bytes and no byte-preserving reference
is now refused with `splice-guard-missing`. An unmeasurable write is refused,
never assumed clean.

**A span covering the whole source scored a vacuous `:exact 0`.** When the named
spans cover the entire file there are no untouched gaps left to compare, so the
gap measurement is trivially satisfied and says nothing.

*Round two recorded this as a known limit and left it. That was wrong: probe R5
turned it into a committed false green, and round three replaced the measurement
outright — see below. The entry is kept as written so the misjudgement stays in
the record rather than being tidied away.*

**What is still not reachable, stated rather than claimed.** On the extraction
route `with-future-sources` already refuses a formatter that returns a file set
it was not given (`invalid-future-sources`), and the formatter is only given the
created files. So no staging step can reach a modified file there today, and the
drift gate on that route has no reachable failing path. It is kept as defence in
depth and the witness records the guard that actually fires, rather than
asserting a catch that does not occur.

# #Round Three: The Measurement Itself Was Wrong

A third review confirmed round two's fixes — basis smuggling refused in every
key shape, a basis commit with a reformatting formatter now drift 0 with
untouched runs preserved, `splice-guard-missing` refusing — and returned NO-GO on
three further points. Two of them are the same failure again in a new place: the
gate reporting a confident zero about something it had not actually looked at.

**1. The vacuous span was a false green, not just a limit (probe R5).** Round two
recorded "a span covering the whole source scores a vacuous `:exact 0`" as a
*known limit*. It is not a limit; it is a hole, and R5 walked through it. Given a
file `(def x  [1  2])`, a `changes` edit whose `find` covers the whole form, and
a formatter mapping double spaces to tabs, the result was `ok true`,
`byte_drift_outside_span 0`, and on disk `(def x\t\t[9\t\t9])`. The gate compared
gaps, there were no gaps, and it passed.

Recording a hole as a limit is how a false green survives a review. The fix is to
stop measuring gaps and start measuring the **expected post-image**: the original
source with each span replaced by *the caller's own replacement text*. Drift is
now every byte where the candidate differs from that image, **inside the spans
too** — because a staging step that rewrites the replacement text is changing
what was asked for, which is churn wearing a different hat. Two numbers are
published, and they answer different questions:

| field | measures | gates a commit |
|---|---|---|
| `byte_drift_from_expected` | every differing byte, spans included | **yes** |
| `byte_drift_outside_span` | the untouched gaps only | no |

The weaker number is kept because earlier receipts published it and it is still
meaningful, but it must never gate alone: R5 is precisely the case where it reads
zero for a file rewritten end to end. The refusal reports both, so a reader can
see when the gap comparison was vacuous.

**2. Partial guard coverage failed open, and a nil reference threw.**
`gate-splice-drift` iterated the *guard*, so a file present in `:future-sources`
but absent from the guard was committed unmeasured — the same fail-open as round
two's missing guard, one level down. A guard entry whose `:reference` was nil
threw an uncaught NPE rather than refusing. Both fixed by iterating the set of
files actually about to be written: a staged file with no guard entry, or with an
entry carrying no reference bytes, is a typed refusal **naming that file**.

The set at risk is the set about to be written, so that is the set to walk. And
because absence can no longer mean "fine", the one legitimate exemption — the
files an extraction *creates*, whose formatter pass is authorized — is now
declared in the guard as `{:exempt :created-file}` rather than inferred from a
missing key. An exemption is a decision; absence is not.

**3. The extraction route committed with no drift field.** `MCP-OP-CLOSE-008` was
checked off while probe R4 showed the extraction route returning a public result
with no `byte_drift_outside_span` at all, so a caller could not tell whether that
route had been measured. The field is now published on every committing route —
direct, basis, and extraction — alongside `byte_drift_from_expected`.

# #The Formatter Finding That Re-scopes Bead 46o

Round two took the authorized fallback for bead `clj-surgeon-46o` on the grounds
that span-scoped formatting was not achievable, because `standard-clojure-style
fix` needs whole-file context to decide indentation. **The red team measured that
claim and it is wrong at the granularity that matters.**

Running `standard-clojure-style fix` on a **complete top-level form in isolation**
produced bytes identical to formatting that same form inside the full file. The
output differs only for **sub-form fragments**, and only by the starting column
the fragment lost when it was cut out of its parent.

So span-scoped formatting **is** feasible, provided the unit formatted is the
enclosing top-level form rather than the matched sub-form. That is the real fix
for 46o, and it is better than either of the fallbacks this branch took: it keeps
managed formatting working and makes the drift refusal stop firing on ordinary
work, instead of trading one for the other.

It is deliberately **not implemented here**. It changes what the formatter does
on every route rather than what the gate refuses, it needs its own cohort, and
this branch was scoped to closing the losers. Stated so 46o can be planned
against a measured fact rather than the assumption this branch shipped with.

# #What Is Proven, and What Is Not

- `whole-file-reprint-refused` is witnessed **end to end** through
  `mcp-tool/execute-request!` on both the default-root and explicit-`workspace_root`
  branches, with zero bytes written.
- `byte-drift-outside-span` is witnessed **end to end** through the public MCP
  boundary with a staging step that reformats untouched source.
- `reprint-outside-span-refused` cannot occur against the current compiler, which
  splices. It is witnessed end to end by injecting a re-printing `apply-edits`
  through the real path (`MCP-OP-CLOSE-015`); that proves the refusal reaches the
  public result and writes nothing, and it is a regression sentinel, not a defect
  observed in the field.
- **Preview is not proven end to end.** Both change contexts hardcode
  `:lifecycle :commit`, and the one public surface named "preview" — prepared
  confirmation — calls `compile-transaction` directly and never stages, so it
  never reaches the drift gate. `MCP-OP-CLOSE-007` is therefore witnessed at the
  pure decision function only, and the requirement says so.
- **No speed claim is made.** No arm was run for this change.

# #Success and Refusal Matrix

`changes` denotes the direct `apply_clojure_changes` route (a top-level `changes`
array). `edits` denotes the compact `edit_clojure` route. Editor gestures are
never routed through these refusals.

| # | request shape | route | outcome | reason | next_call | complete? |
|---|---|---|---|---|---|---|
| 1 | `owner {kind namespace}` + `find` + `replace` | changes | **refuse** | `whole-file-reprint-refused` | `edit_clojure` `edits[]` with `within.namespace` + `from`/`to`, one item per file | yes — executable unchanged |
| 2 | `owner {kind namespace}` + `find` + `insert_before`/`insert_after`, insertion is a require | changes | **refuse** | `whole-file-reprint-refused` | `edit_clojure` `require_change` (`add` from the inserted lib/alias, `files` from `files`) | no — `missing: ["symbol_migration"]` |
| 3 | `owner {kind namespace}` + `find` + `insert_before`/`insert_after`, otherwise | changes | **refuse** | `whole-file-reprint-refused` | `edit_clojure` `edits[]` with `within.namespace`, `from` = the refused `find` | no — `missing: ["from","to"]` |
| 3b | `owner {kind namespace}` + `find` + `assoc_entry` | changes | **refuse** | `whole-file-reprint-refused` | `edit_clojure` `edits[]`, `to` derived by splicing the entry before the closing brace exactly as the kernel does | no — `missing: ["from"]` |
| 4 | `forms` + `find` + `replace` | changes | **accept**, gated | — | — | — |
| 5 | any change whose compiled future source departs from the raw splice | changes | **refuse** | `reprint-outside-span-refused` | — (server defect; report it) | — |
| 6 | any write whose final future bytes drift outside the replaced spans, commit mode | any | **refuse** | `byte-drift-outside-span`, number reported | `edit_clojure` route note in `remedy` | — |
| 7 | same, non-committing decision | pure gate only | **accept**, number reported | — | — | — |
| 8 | `edits` + `within` + `from`/`to` | edits | **accept** | — | — | — |
| 9 | `symbol_migration` + `require_change` | edits | **accept** | — | — | — |
| 10 | `extraction`, `delete_owners`, `create_files`, `programs`, `transform_clojure`, all `inspect_clojure` modes | — | **accept** | — | — | — |

Rows 1-3 refuse **before any source is read**: they are pure wire-shape decisions
made in `clj-surgeon.mcp-close-losers` from `clj-surgeon.mcp-contract/validate-tool-params`.
`source_unchanged` is `true`, `mutation_attempted` is `false`, `write_authority`
is `false`.

# #Why a Namespace-Owner Insertion Cannot Carry a Complete Redirect

`edit_clojure`'s `from`/`to` lower to the direct route's `find`/`replace`, and
both are parsed by `parse-one-form`: **exactly one complete Clojure form each**.
An insertion adds a *second* sibling, so it has no one-form-to-one-form spelling
unless the caller widens `from` to the enclosing form — which requires the source
the caller already holds and the pure wire validator deliberately does not read.

`require_change` is the measured zero-churn verb for the require case (Y-5), but
`clj-surgeon.mcp-schema/editor-hybrid-schema` binds it to `symbol_migration`:

```clojure
:anyOf [... {:required ["symbol_migration" "require_change"]}]
:allOf [{:not {:oneOf [{:required ["symbol_migration"] :not {:required ["require_change"]}}
                       {:required ["require_change"]  :not {:required ["symbol_migration"]}}]}}]
```

A `require_change`-only call is therefore schema-invalid and would not be
executable "unchanged". So rows 2 and 3 emit the best derivable call and name the
exact fields the caller must supply in the existing `missing` field, with
`next_action` `"fill_next_call_then_call_edit_clojure_once"`. A refusal that
handed back an unexecutable call and called it executable would be the false-green
failure the doctrine forbids.

# #byte_drift_outside_span

Defined against the **byte-preserving splice**, not against a formatter opinion.

- `reference` — the original source with each matched span replaced by its
  replacement text, spliced by byte offset. This is what `compile-file` produces.
- `candidate` — the bytes about to be written (after `prepare-compiled!`).
- `spans` — the replacement regions in `reference` coordinates, known by
  construction from the compiled edits.
- `byte_drift_outside_span` — the number of UTF-8 bytes by which `candidate`
  departs from `reference` **outside** those spans. Equal candidates score 0
  without any alignment work. Otherwise each span's replacement text is located in
  `candidate` left to right and the gaps are compared pairwise; if a replacement
  cannot be located verbatim the whole difference is charged and
  `span_alignment` reports `"unlocatable"`, never a guess.

The unit is bytes because the field says bytes. The l1 receipts quote lines (425,
158, 156); a line figure accompanies the byte figure in the refusal so the two
records can be compared.

The order named `raw-span-source`, `raw-between` and `span-gaps` in
`structural_lens.clj` as the helpers to build the splice on. Those three address
**sibling spans in a zipper**, not byte offsets in a source string, and are
private to that namespace. The byte arithmetic this leaf needs already exists in
`intent_transaction.clj` (`line-offsets`, `node-offsets`, `prepare-raw-addressed-edit`,
`apply-raw-edit`) and is reused instead; no zipper helper is copied or widened.

# #A Note on Reading the Diff

This branch was cut from `origin/main` at `df432c4`. `origin/main` has since
advanced, so a diff taken against the current remote tip shows documentation
files as *deleted* that this branch never touched: they were added after the
base. Only the files listed under Verification, plus this document and the
intent registry, are changed here.

# #Open Decisions for SURGEON1

**The `require_change` schema, and why a namespace-owner require insertion has no
one-call redirect.** `mcp-schema/editor-hybrid-schema` binds `require_change` to
`symbol_migration`: neither is legal without the other. So the measured
zero-churn verb for "add this require to N namespaces" (Y-5) cannot be called for
a require insertion on its own. The red team's finding sharpens this: the only
*legal* completion of such a call **rewrites call sites the caller did not ask to
change**, because `symbol_migration` is by definition a set of owner-scoped
symbol replacements. Offering that as the redirect would be the tool widening a
caller's intent, which the vision explicitly forbids ("The operation should not
decide the desired architecture, infer intent, name a new abstraction, or
silently widen its scope").

The refusal therefore names `symbol_migration` in `missing` rather than inventing
migration rows. Whether to unbind the two in the schema — making
`require_change` legal alone — is SURGEON1's call, not this branch's: it changes
a public input schema, which this work was scoped out of.

**The drift gate refuses managed formatting on the `changes` route.** Any commit
there on a file the formatter reformats now refuses. That is the order and it
closes the churn, but the durable fix is bead `clj-surgeon-46o` — span-scoped
formatting — after which the refusal would stop firing on ordinary work. Refuse
now versus don't-restage later is a sequencing call above this branch.

*Resolved, 2026-09-02, on `bridge/format-form-scope` — see
[format-scope-design.md](format-scope-design.md).* 46o was built at the unit
round three's finding identified: the enclosing top-level form. The formatter is
handed one complete form at a time and can no longer see a byte the transaction
did not edit, so the two fallbacks this branch shipped are lifted — the basis
route is formatted again, and the `changes` route's refusal stops firing on
ordinary work. **What a reviewer must weigh:** 46o replaces the transaction's
splice guard with the post-format image, so `MCP-OP-CLOSE-021` no longer bounds
the formatter by "may not change a byte." **Exactly one check bounds it now:**
inside an edited form the tokens and comments must match as an ordered stream,
with only the sibling clauses of an ns `:require` / `:import` list normalised by
sorting whole subtrees. Everything outside those forms is untouched by
construction rather than by a check. Probe R5's tabbing
formatter therefore commits rather than refusing, and its witness here was
re-scoped, with a sibling test proving that same formatter cannot reach a form
the change did not edit. The deliberate narrowing of the ratified gate is stated
in full under "What This Changes About the Drift Gate, Stated Plainly".

# #Non-Goals

- Removing or renaming any public tool, and any change to an input schema.
- Changing the SCI allowlist, the evaluation fence, or path confinement in
  `edit_dsl.clj`. Nothing in this leaf touches them.
- Changing whether or how `prepare-compiled!` runs the formatter. That is a
  separate decision with its own blast radius; this leaf only makes its effect on
  untouched source visible and refusable.
- Any speed claim. No arm was run for this change.

# #Verification

- `test/clj_surgeon/mcp_close_losers_test.clj` — the wire-shape matrix on both
  key shapes, redirect derivation per action, the winner shapes that must stay
  accepted, and integration witnesses that drive `mcp-tool/execute-request!`.
- `test/clj_surgeon/splice_drift_test.clj` — drift arithmetic, the three
  alignment holes, and a reproduction of a whole-file reformat at commit.
- `test/clj_surgeon/mcp_contract_test.clj`, `test/clj_surgeon/mcp_tool_test.clj` —
  the three existing tests that encoded the closed shapes as happy paths, updated
  to the new contract.
- `make test`, `make mcp-test`, `make mcp-smoke`, `make analyzer-contract-test`.
