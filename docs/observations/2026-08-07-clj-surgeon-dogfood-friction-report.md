# Clj-surgeon dogfood friction report: benchmark instrumentation

Date: 2026-08-07

Context: While working on Beads issue `clj-surgeon-xey`, I used the shared
`inspect_clojure`, `apply_clojure_changes`, and cclsp services to add interaction
metrics to the clean Codex benchmark. The source work involved a Babashka script
under `bench/`, not a normal source-path namespace. Four distinct friction events
occurred before the successful atomic transaction.

## 1. A wrong named form invalidated an otherwise useful batched read

Beads: `clj-surgeon-mr8`

I requested `numeric-columns`; the file contains `numeric-fields`. The same batch
also requested an outline that would have revealed the correct name.

What worked: `inspect_clojure` refused the all-or-nothing batch with
`batch-form-selection-failed`, identified the failing request, changed no source,
and did not guess.

Friction: the refusal discarded the successful outline result and did not return
nearby named-form candidates. The caller needed another round trip to correct a
one-token naming mistake.

Smallest useful improvement: preserve atomic source disclosure, but include
bounded correction evidence in the refusal—such as the exact available owner
names from the already-parsed outline or the nearest few names. This should not
publish partial requested source or weaken batch atomicity.

Classification: mostly caller error; recovery contract can be better.

## 2. The optional prepare-change label grammar was discovered by refusal

Beads: `clj-surgeon-e3n`

I supplied `interaction_rates`. Labels accept lowercase letters, digits, and
hyphens, so the valid spelling was `interaction-rates`.

What worked: the tool refused in about 70 ms with `invalid-change-label`, stated
the complete grammar, and confirmed `source-unchanged=true`. Recovery required
only one corrected call.

Friction: the visible tool description called the label lowercase but did not
make the underscore prohibition salient enough to prevent the call.

Smallest useful improvement: expose the exact label pattern in the generated
tool schema or description, for example `^[a-z][a-z0-9-]*$`.

Classification: caller/schema-discoverability error; refusal quality was good.

## 3. Prepare-change had no file-and-owner route for a script namespace

Beads: `clj-surgeon-t7b`

I tried to prepare a change for
`summarize-clean-codex/summarize-group`. A direct cclsp resolution did not return
within two minutes and was terminated. The subsequent Surgeon request returned
`semantic-provider-refusal`: cclsp found zero workspace symbols for the Var.
The form exists and `inspect_clojure` can select it exactly by file and owner, but
the `bench/` script is not indexed as an ordinary workspace Var.

What worked: prepare-change refused instead of fabricating semantic evidence or
silently falling back to text editing.

Friction: after exact structural inspection had already established the file,
owner, and source hash, there was no proof-carrying path from that evidence to a
whole-owner transaction. The only prepare-change entrance requires a semantic
Var. This makes script files a second-class mutation surface and encourages the
caller to reconstruct a direct `changes` request.

Smallest useful improvement: add a file-plus-owner prepare route, or let a
completed structural read publish a retained basis that can be promoted to a
change decision. It should retain the same file hash, exact owner, cardinality,
and verification guarantees without pretending cclsp resolved a Var.

The cclsp timeout should also be bounded and reported distinctly from a clean
zero-symbol result.

Classification: product gap for scripts and other structurally valid,
semantically unindexed files.

## 4. Exact replacement could not express insertion of collection members

Beads: `clj-surgeon-rcy`

To add two numeric fields, I attempted to replace the set member `:wall-ms` with
`:wall-ms :user-turns :tool-round-trips`. The replacement is three sibling forms,
not one form, so `apply_clojure_changes` refused the entire three-change
transaction with `invalid-intent-form`. No source changed.

What worked: the one-form invariant prevented an invalid structural splice and
the transaction remained failure-atomic. A later two-owner transaction committed
successfully, read back the written bytes, and returned an undo receipt.

Friction: adding members to a vector, set, map, binding vector, or peer sequence
is common, but the direct MCP route exposes replacement rather than guarded
sibling insertion. The safe workaround was to replace a much larger owner and
carry more source through the model.

Smallest useful improvement: expose one compact collection/peer insertion edit,
such as `insert-after` with an exact selected member, owner guard, expected match
count, and one-or-more complete forms to insert. The kernel should continue to
reject detached trivia, ambiguous peers, and stale owner hashes.

Classification: intentional safety boundary with a high-value missing operation.

## Overall assessment

This session was not boofarama. Every malformed request failed closed, and the
valid multi-edit transaction was genuinely one-shot and atomic. The convergence
problem is now concentrated in recovery latency and expressiveness:

1. make trivial selection mistakes self-correcting without returning partial
   source;
2. make small schema constraints visible before the call;
3. support proof-carrying changes for exact script owners without inventing LSP
   semantics; and
4. support guarded sibling insertion so safety does not force whole-owner
   reconstruction.

The performance question remains empirical. The benchmark instrumentation now
separates user-visible turns, internal tool round trips, discovery work, and
post-decision work. Replicated treatment-versus-native runs are in progress.

## Resolution after the shared-server release audit

The report produced two immediate contract repairs and two explicit next
slices.

1. **Wrong owner names now return bounded correction evidence.** The failed
   batch still returns no partial requested source. It now reports the total
   available owner count and at most eight owner names ranked by common prefix.
   The exact `numeric-columns` replay returned `numeric-fields` first in 58 ms,
   read the file once, and kept `read_complete=false`.
2. **The label grammar was already present in the live machine schema.** The
   `label` property publishes `^[a-z][a-z0-9-]{0,39}$`. The documentation must
   not replace this exact constraint with the vague word “lowercase.”
3. **Exact-source owner preparation is implemented.** `mode=prepare-change`
   now accepts exactly one of semantic `subject`/`subjects` or a closed
   `file`/`form` coordinate. The latter carries `authority=exact-source`,
   publishes zero inferred references, and retains the same source hash,
   structural address, verification profile, and one-use basis guarantees. A
   live benchmark-script call prepared `numeric-fields` as one decision.
4. **Guarded sibling insertion is implemented.** Direct changes accept exactly
   one of `replace`, `insert_before`, or `insert_after`. The insertion route
   accepts one or more complete forms, preserves the existing whitespace
   separator, composes with the same atomic transaction and inverse receipt,
   and refuses comment-bearing gaps. A live isolated-project call inserted two
   vector members, verified exact read-back, and restored the starting hash
   through its receipt.

The release does not hide items 3 and 4 behind a fallback claim. They are
bounded product gaps. They also do not invalidate the shared-server contract:
multi-project routing, exact source confinement, semantic preparation, direct
replacement, deletion, rollback, receipts, and failure diagnostics are
independently complete.

## A live refusal found one final evidence defect

The first comment-bearing insertion probe refused before mutation, but the MCP
adapter reported `source_unchanged=false`. The kernel had behaved correctly;
the adapter inferred mutation state from an allowlist of known error names and
did not recognize the new insertion diagnostic.

The lasting fix is phase evidence, not a longer allowlist. A failed transaction
compile now returns `phase=compile` and `source-unchanged=true`. Refusal
normalization preserves explicit kernel evidence. A focused boundary test and
the repeated live probe now return:

```text
refused · ambiguous-insertion-gap
✓ source unchanged
kernel_phase=compile
```

This matters because a ten-millisecond safe refusal must not trigger a
twenty-second recovery investigation. Failure output is part of performance.

## 5. Shared-service readiness was green while structural reads were unavailable

Beads: `clj-surgeon-swr`

During the server2 parameter-shadow repair, both shared service health endpoints
reported `ok=true`. The server2 workspace also appeared initialized in cclsp,
and `clj-surgeon up` returned `:ok true`, shared-service mode, no configuration
changes, and no restart requirement. Despite those signals, two real
`inspect_clojure` calls returned `server-not-initialized` with
`read_complete=false`.

The workspace semantic session was later replaced without restarting the shared
cclsp process or the Codex session. The exact read that had failed then returned
14 forms with `read_complete=true` in 140.56 ms.

What worked: the failure was explicit and did not return stale or partial source.
Per-workspace semantic recovery restored service without interrupting the shared
process.

Friction: the readiness surfaces described process and workspace health, but did
not predict whether the next Surgeon request could enter its application
session. A caller could satisfy every published preflight condition and still
fail its first real operation.

Smallest useful improvement: make `clj-surgeon up` and `/healthz` expose the
same application-session readiness gate used by `inspect_clojure`, or name the
states separately. Include the current per-workspace `lsp_session`, application
initialization state, recovery count, and last recovery reason/time. A green
preflight must either guarantee that an immediate structural read can start or
say precisely which remaining state is not ready.

Classification: correctness-preserving observability and readiness-contract gap.

Resolution: readiness now crosses the application boundary before it reports
green. A second live defect tightened the contract further: port 7890 held a
healthy managed cclsp process that watched the repository demo config, while
`clj-surgeon up` wrote the shared workspace config. cclsp now publishes its
canonical `config_path`. The startup gate reuses a process only when that path
matches. It reloads the known launchd service once when the path differs and
refuses to replace an unknown process. The live server2 onboarding then
completed in 2.0 seconds and a real semantic request initialized one LSP
session; health returned zero outstanding, active, or queued work afterward.

## 6. Local binding rename required a mixed native/MCP transaction

Beads: `clj-surgeon-cee`

The server2 URL boundary intentionally exposes the key `:sort-by`, but the local
binding named `sort-by` shadows `clojure.core/sort-by`. One occurrence caused a
silent ordering bug because invoking the keyword as a function returned a
fallback value instead of throwing. The correct refactor was to preserve the
external key while renaming only the local binding to `sort-field`.

Surgeon had no binding-aware local rename operation that could distinguish the
destructuring key from its local symbol. The workaround was:

1. use a native patch to make each destructuring binding explicit as
   `sort-by :sort-by`; and
2. use one direct MCP transaction to replace exactly 38 `sort-by` symbols with
   `sort-field` across nine named owners.

The MCP transaction was the strong part of the experience. `match-form`
established the exact count before mutation. `apply_clojure_changes` committed
all 38 edits in one file atomically and returned verified read-back evidence for
all nine owners. There was no partial edit or post-success source reread.

Friction: the semantic intent was one commonplace refactor, but expressing it
required a native preparatory edit plus a lexical structural transaction. That
split weakens the otherwise excellent proof chain and asks the caller to reason
about binding identity manually.

Smallest useful improvement: add a binding-aware local rename whose selector is
an exact binding occurrence inside a named owner. It must preserve map keys and
keywords, update only references resolved to that binding, publish the expected
reference count before mutation, remain failure-atomic, and return the usual
hash and read-back verification evidence. Refuse when binding identity is
ambiguous; do not degrade to file-wide textual replacement.

Classification: high-value semantic edit gap with a safe structural workaround.

## Updated awesomeness assessment: 8 / 10

The product earns an 8 because its hard guarantees held under real refactoring
pressure: malformed requests failed closed, exact counts prevented broad edits,
and a 38-edit nine-owner transaction was atomic and self-verifying. Those are
the difficult parts, and they materially outperform an unguarded patch workflow.

The remaining two points are concrete rather than aspirational:

1. **Operational trust:** every green readiness signal must predict a usable
   next request, with per-workspace recovery state visible when it cannot.
2. **Refactor ergonomics:** binding-aware local rename should make the safe path
   one transaction while preserving external data contracts such as `:sort-by`.

Closing those two gaps would make the common path both safer and simpler than
native editing, rather than safer only after the caller assembles the right
hybrid workflow.

## 7. Comment-bearing nested maps lost structural identity

Beads: `clj-surgeon-w1e`

The reddit-scraper CI repair initially appeared to require adding one field to
seven expected maps. Two expected maps were logically identical, but one
contained line comments between its entries. A direct map match found only the
comment-free occurrence. A second request used the complete, unique enclosing
`is` assertion and still matched zero because the nested comments changed the
source shape.

Three transaction attempts refused before mutation:

1. the first request used scalar `expect` instead of the required
   `{matches: N}` object and returned `expected-object`;
2. the corrected request declared two identical map matches but found one; and
3. the unique enclosing assertion declared one match but found zero.

What worked: every refusal returned `source_unchanged=true`, named the failing
change, and preserved the file. After the test design was corrected, Surgeon
atomically redirected 22 preview calls through permissive contract projections
and verified the written file with an undo receipt.

Friction: a caller cannot perform a small nested edit in a comment-bearing map
without either reconstructing the full owner or using a native patch. Logical
Clojure structure and source trivia need separate identities at this boundary.

Smallest useful improvement: permit a named-owner nested selector to identify a
comment-bearing map by structure and distinguish equivalent siblings by an
ancestor or site selector. Preserve the existing comments and separators. If
that preservation is impossible, refuse before mutation with a precise
`trivia-would-be-lost` diagnostic.

Classification: structural edit gap; the safety behavior is already correct.

### Resolution

`assoc_entry` now compares the selected map by Clojure value and preserves its
existing source text while it inserts one key/value. An optional `inside`
selector compares one complete ancestor by Clojure value. It can distinguish
equivalent nested maps even when comments change the ancestor's source shape.
Permanent tests preserve line comments byte-for-byte, refuse an existing key,
and refuse an ambiguous count before source production.

The binding incident is also closed by `rename_binding`. The action uses
clj-kondo local binding IDs from the exact source snapshot. It preserves a
`:keys` keyword, updates the corresponding `:or` local, and changes only usages
with the same binding ID. The production-shaped proof completed one verified
five-occurrence transaction, preserved `clojure.core/sort-by`, and restored the
original bytes through its undo receipt.

## 8. Six parallel semantic queries hid queue state

Beads: `clj-surgeon-bfn`

One caller submitted six `resolve_var_surface` requests for the same workspace.
The aggregate produced no completed result or progress for more than 90
seconds. The caller could not distinguish active LSP work, queued work,
recovery, or deadlock.

The cclsp scheduler now admits two semantic surface requests per workspace.
Queued requests expose their position and have a five-second queue deadline.
Active requests have a 60-second caller deadline. `/healthz` reports active
semantic requests, queue depth, queued positions, oldest queue age, and the LSP
session beside the existing JSON-RPC request state. A six-query test proves
bounded results and proves that saturation in one workspace does not delay a
second workspace.

Live proof used the shared server2 workspace after identity-aware onboarding.
One real `resolve_var_surface` request returned a bounded zero-symbol result in
21.4 seconds. The workspace remained initialized with the same `lsp_session`,
zero JSON-RPC requests, zero active semantic requests, and an empty queue. A
clean miss is no longer observationally indistinguishable from a wedged call.

## 9. A stale MCP session looked like an unbounded semantic request

Beads: `clj-surgeon-fhv`

Post-repair acceptance submitted one sequential request for
`reddit.mongodb.mongodb/start`. The caller waited more than 100 seconds, but
cclsp showed no semantic or child-LSP work after cancellation. A fresh MCP
client sent the identical request and received a typed miss in 515 ms.

The failure was below the semantic scheduler. A prior cclsp reload invalidated
the coding agent's Streamable HTTP session. cclsp rejected the stale session
immediately with HTTP 400, but its JSON-RPC response used `id: null`. A strict
client waited forever for the original request ID.

The server now echoes the request ID and returns
`invalid-mcp-session: reconnect, then retry the same request`. `/healthz`
retains active and recent MCP admissions with JSON-RPC ID, tool, workspace,
subject, phase, status, and elapsed time. The live stale-session probe returned
ID 314 in zero milliseconds and recorded `session-validation/refused`.

Acceptance then ran in the intended order. The exact sequential request
returned a typed miss in 12.0 seconds. The original six-request shape completed
all six typed results in 5.21 seconds; the slowest took 5.11 seconds. The ledger
retained every completion and no semantic or LSP work remained active.

`clj-surgeon up` now also records whether it restarted the managed cclsp
service. Its result sets `agent-session-restart-required=true` when client MCP
sessions were invalidated. Config edits and ordinary reuse do not set that
flag.

## 10. Mothership semantic recovery overlapped the old and new provider

Beads: `clj-surgeon-766`

Four related Mothership Vars originally returned opaque 30-second
`workspace/symbol` timeouts after about 102 seconds of aggregate waiting. A
later exact-anchor replay found a second timeout in
`textDocument/documentSymbol`. A direct isolated control initialized
`clojure-lsp` in 3.111 seconds and returned document symbols in 3.739 seconds,
so repository size was not the root cause.

The recovery implementation sent `SIGTERM` and immediately started the
replacement. It did not wait for the old child to exit. Old and new processes
could overlap, and the replacement could remain uninitialized until the retry
timed out.

Recovery now has a lifecycle barrier. It waits up to three seconds for a
graceful exit, escalates to `SIGKILL`, refuses if the child still lives, and
does not report success until the replacement finishes initialization. The
receipt names both sessions and PIDs, the old exit mode, and termination time.
Interactive requests now use a 10-second timeout separate from the 30-second
cold-initialization budget.

The real four-Var anchored replay completed in 2.539 seconds. A deliberate
`SIGSTOP` wedge recovered in 12.457 seconds, down from 32.452 seconds. Named
forms now return directly callable source anchors, and
`resolve_var_surfaces` submits up to four related anchors as one bounded audit.
Provider failures retain the exact anchor and return one executable retry
before the MCP client deadline.

Classification: closed provider lifecycle, timeout-policy, and caller-contract
gap. The durable rule is to preserve exact evidence across tool boundaries and
wait for old process death before publishing replacement readiness.

## 11. A sibling Var caused a six-workspace semantic fan-out

Bead: `clj-surgeon-6j5`

Server2 requested `reddit.mongodb.mongodb/start` using its own canonical
workspace root. The definition lives in the configured sibling module. The
first correct cross-workspace implementation queried all six configured
Clojure language servers. It returned the exact definition, source SHA-256,
owner, and 81 references in 27.371 seconds, but the search cost grew with the
number of unrelated workspaces.

Onboarding now publishes deterministic confined source roots from `deps.edn`,
`bb.edn`, and the standard Clojure roots. cclsp maps the requested namespace to
candidate source files before it asks an LSP. The live replay queried only the
sibling workspace. It returned the same definition SHA and 81 references in
9.237 seconds on the first run and 0.210 seconds warm. The structured receipt
reported `source-root-shortlist`, all six configured roots, the one candidate
root and file, and the one actual workspace outcome.

The optimization remains failure-closed. Missing metadata selects the original
all-configured fallback. Unit tests retain typed clean-miss, ambiguity, partial
timeout, and completed-candidate evidence. A separate P1, `clj-surgeon-5ss`,
tracks the lost-update race found when two `clj-surgeon up` processes modified
the shared configuration concurrently.

Classification: closed cross-workspace identity and fan-out gap. A fully
qualified Var now names code, not an LSP process; the source-root index chooses
the provider and the semantic proof still authorizes the result.
