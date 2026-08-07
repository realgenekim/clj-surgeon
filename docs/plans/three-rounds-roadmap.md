# Three Rounds: the road from a fast transaction to a 3x task

**Status:** Proposed design direction

**2026-08-07 update:** The integrated inspect surface passed correctness but
missed its keep gate. The
[3x assessment](../observations/2026-08-07-captains-log-the-3x-mechanism-exists-but-the-product-is-not-there.md)
revises the next sequence to workspace-scoped inspection, a hash-bound
decision register, and closed verification profiles. Result-register
drill-down now waits for measured output truncation.

**Motivating evidence:**

- [One call crossed the double-digit gate](../observations/2026-08-07-captains-log-one-call-crossed-the-double-digit-gate.md)
  — typed hot write MCP: 24.530 s median vs 43.190 s native (−43.2%), 4/4
  replicated; the transaction itself ran in 89.875 ms.
- [The transaction landed, but reading still paid per question](../observations/2026-08-06-captains-log-the-transaction-landed-but-reading-still-paid-per-question.md)
  — 26 read calls and 45.7 s direct wall in one diagnosis; the 45-form read
  transaction lost by 42.7% until the semantic format recovered a 5.5% win;
  both read losses were transcript-truncation losses.
- [To beat apply_patch, become a native tool](../observations/2026-08-06-captains-log-to-beat-apply-patch-become-a-native-tool.md)
  — direct tool wall was 4.4% of aggregate task wall; median Surgeon and
  native-patch actions were within 25 ms.
- [Representative MCP read portfolio](representative-read-portfolio.md)
  — the accepted, frozen read benchmark that gates the read surface.

## The accounting: rounds, not milliseconds

Direct tool execution is 4–11% of complete task wall. Babashka startup,
parse sharing, and even the measured 3.1x one-process read speedup all live
inside a budget too small to produce a threefold end-to-end gain.

Every measured win and loss in the evidence base moved with one variable:
**the number of model deliberation rounds, and the tokens each round
re-carries.** The write MCP won 18.7 seconds by deleting rounds — skill read,
stdin ceremony, post-edit diff, recovery reads — not by executing faster. The
45-form read lost twice for the same reason inverted: an oversized visible
result crossed the transcript boundary and forced reread rounds that returned
the entire batching gain.

A representative native task spends roughly 8–10 rounds:

```text
locate -> read -> read more -> edit -> context mismatch -> reread
       -> retry -> inspect diff -> test -> answer
```

The target, already named in the field logs, is three:

```text
inspect -> decide -> change and verify
```

That collapse — not faster parsing — is the 3x. Every candidate feature below
is scored by how many model rounds it deletes.

## The levers, ranked

Each lever names its editor ancestor, because the vi/Emacs analogy has been
predictive at every stage of this project:

```text
vi             address -> operator -> motion -> repeat
Emacs          scope   -> command  -> macro  -> execute
clj-surgeon    in/forms -> find/path -> do -> guarded transaction
```

### 1. Ship `inspect_clojure` (largest single lever; now live)

Reads paid one skill-load action, one shell-quoting decision, and one process
per question. The write result is the existence proof that a typed hot
entrance deletes those rounds. The tool is now served: batched
`forms`/`outline`/`match`/`xray` requests, snapshot-hash-bound,
all-or-nothing, semantic addresses, aggregate `expect` guards. The frozen
read portfolio is the standing experiment that gates keeping it.
*Ancestor: buffers — you do not reopen the file for every question.*

### 2. Manifest symmetry: the read output is the write input

Emacs `occur-edit`/`wgrep`: run a query, get a results buffer, edit the
results buffer itself, commit back. Translate exactly: `inspect_clojure`
match results return **shaped as an `apply_clojure_changes` manifest
skeleton** — `:in`, `:forms`, `:find`, and match counts pre-filled, `:do`
empty. The caller fills in transforms and submits the same artifact back.

The model never translates between a read representation and a write
representation, never re-derives addresses it was just shown, and the
`:expect` counts arrive pre-proven. Two tools, one artifact, zero impedance.
This is the strongest amplifier of "compiling a decision": the inspect call
primes the compiler.

### 3. Transaction-embedded verification

The route still ends `-> test`, a separate round with its own shell call and
output. Offer an optional post-commit gate inside the transaction boundary:

```clojure
:verify {:cmd "make runtests-once"}
```

Run it after commit, report pass/fail in the receipt, optionally roll back on
failure (the undo receipt already exists; this is composition, not new
machinery). Tests remain separate semantic authorities — the transaction does
not judge, it schedules the authority and reports. The route becomes
`understand -> one call -> answer`, with green tests inside the receipt.
*Ancestor: `:w | make`.*

### 4. Result registers: never lose a win to the transcript boundary

Both read-benchmark losses were truncation losses; 47,651 visible characters
still forced five rereads. Adopt registers: the hot server keeps every full
result server-side under a handle; the tool returns semantic-format headers
plus the handle under a hard visible budget (~40,000 characters); a follow-up
call fetches named members by reference. Reread-after-truncation — the
failure mode that twice erased the batching gain — becomes structurally
impossible.
*Ancestor: vi registers; Emacs result buffers.*

### 5. Query-addressed broadcast edits

For edits where many sites share one shape, the pattern is the address: the
caller states pattern, transform, and expected count, and never sees the
sites. This deletes the read phase entirely for repetitive edits. The
15-paper-edit exercise in
[Structural change language](structural-change-language.md) already
right-sized the algebra (per-owner distribution guards; lens `:path` over
variadic captures). Sequence after levers 1–2, per the evidence-first
doctrine.
*Ancestor: `:g/pattern/normal` and `:cdo`.*

### 6. Activation: solved — spend nothing more

One project-rule sentence moved adoption from 0/4 to 4/4. The MCP
server-instructions field now carries the routing rule into every session with
zero actions. Extend the same sentence for `inspect_clojure` when it ships.

## The internal substrate: atlas inside, algebra outside

A design review of an independent "code atlas" proposal (a lossless
multiscale code tree with graph overlays, node dossiers, and three-layer call
graphs) produced a synthesis worth recording as architecture doctrine.

**Adopt the atlas as the internal architecture.** One internal node-and-edge
model makes most future read features projections of the same substrate
instead of new subsystems: semantic zoom is a tree projection, result
registers are node handles, form-level diff is snapshot comparison, call
graphs are edge overlays.

The substrate is composition, not construction. A sibling-repository audit
(2026-08-07) found the semantic layer already built and installed on this
machine:

- **clojure-lsp 2026.02.20** (embedding clj-kondo 2026.01.19) maintains a
  persistent incremental workspace index and answers references,
  definitions, call hierarchy, workspace symbols, and diagnostics;
- **cclsp** (`../cclsp`) already wraps it as a persistent MCP server with
  `find_references`, `prepare_call_hierarchy`, `find_workspace_symbols`,
  and rewrite tools that the studied sessions did not use;
- **Mothership** (`../mothership/src/app/analysis.clj`, 27 functions)
  already consumes kondo var definitions/usages into caller/callee
  projections — proof the projections are cheap, and a warning: an atlas
  that indexes kondo directly would be the *third* implementation of the
  same graph.

The revised architecture delegates every semantic fact and keeps only what
no sibling owns:

```text
clojure-lsp (via cclsp or JVM API) ──── semantic oracle:
                                        references, call hierarchy,
                                        symbols, diagnostics
                     ↓ re-anchor by snapshot hash; refuse on drift
clj-surgeon persistent server ───────── lossless snapshots, structural
                                        match, xray, batching, budgets,
                                        intent compilation, guarded
                                        transactions, receipts
named nREPL ─────────────────────────── runtime overlay (later, gated)
```

clj-surgeon's surviving unique value: lossless concrete syntax and exact
bytes, hash-bound coherent snapshots, all-or-nothing batched reads with
guards and budgets, structural pattern match (syntax as data — no LSP
offers a form-pattern query), sandboxed computed X-ray, the transaction
engine with cardinality guards, atomic commit, receipts, and undo, and the
one-call agent-shaped result. That is the moat; the graph facts are
rented.

Substrate rules, each earned by prior evidence:

1. **Internal node ids never become the caller's address vocabulary.**
   Opaque handles in caller context are cursor state — the mechanical
   bookkeeping this project exists to eliminate. Caller addresses stay
   semantic (`(form 'transition)`, `:find` + `:expect`) because they are
   re-derivable from understanding and survive a lost context. Handles
   appear only as optional continuation tokens for drilling into an
   oversized server-held result (lever 4), never as required names.
2. **Every edge carries a type and an authority, and absence means
   "no statically resolved edge," never "cannot be called."** Direct var
   call, higher-order reference, protocol dispatch, callback registration,
   unresolved — kept distinct. Claims no listed authority can prove (for
   example effect purity from static analysis) are omitted, not guessed.
3. **Collapsed nodes describe their negative space.** A summary must state
   what it is not showing — line count, binding count, call count, subtree
   hash — so collapsing trades bytes for bounded uncertainty, not blind
   spots. Both read-benchmark losses were representation losses; the hard
   visible budget from lever 4 is load-bearing in every projection default.
4. **Candidate futures are snapshots with the same read surface as the
   present.** Preview stops meaning "print a diff and reread it" and starts
   meaning "ask bounded questions of the future tree." A write receipt may
   then carry a relationship delta — edges added, removed, newly
   unreachable — answering "did the future program's structure change the
   way I intended" without a diff-reread round. This composes with lever 2:
   inspecting a candidate returns a manifest-shaped view.
5. **Runtime layers wait for a field flail.** Statically-possible edges are
   justified today by the observed 26-call diagnosis. Currently-loaded and
   actually-observed call graphs (nREPL and trace overlays) are built only
   when a recurring observed failure names them, exactly as broadcast edits
   wait for theirs.
6. **The read bridge converts positions to structure, both directions, and
   refuses on drift.** LSP answers are position-based (file, line, column
   against current editor state) — exactly the cursor-state this project
   eliminates. Position addressing must never reach the caller. The bridge
   resolves the caller's semantic address to a position for the LSP
   question, then re-anchors every LSP answer to the smallest containing
   hash-bound structural node in the current snapshot. If the snapshot hash
   and the LSP index disagree, the batch refuses with a drift diagnostic
   rather than returning stale positions as fact. The bridge supplies
   evidence only. It does not grant LSP mutation authority.
7. **Persistent-to-persistent only.** A cold `clojure-lsp` CLI call costs
   roughly nine seconds of JVM startup; the observed sibling walls confirm
   it. The bridge connects the hot clj-surgeon server to the hot LSP index
   (cclsp's process or the JVM API in-process) and never pays a per-question
   cold start — the same lesson the write MCP already proved.
8. **LSP rewrite integration waits for field evidence.** The studied sessions
   used cclsp navigation and did not use its rewrite tools. A future repeated
   failure may justify importing an LSP WorkspaceEdit as untrusted candidate
   input. Until then, the model declares structural intent and clj-surgeon
   remains the only writer.

New capability therefore arrives as new request operations on
`inspect_clojure` — `callers`, `callees`, `witness-path`,
`neighborhood-delta` — never as a new tool family or a new address language.
The caller keeps one coherent structural universe; the server keeps the
graph.

## Sequencing and gates

Build order:

1. `inspect_clojure` is live — run the frozen read portfolio against it now;
   that is the standing experiment, with lever 4's result registers baked in
   before any projection grows past the visible budget.
2. Run the frozen relationship tasks against hot cclsp. Add read-only
   `callers`, `callees`, or `witness-path` operations only when one composed
   answer removes measured caller rounds. Re-anchor results to hash-bound
   nodes (substrate rules 6–7). Do not build a kondo index in clj-surgeon.
3. Manifest symmetry (lever 2), then queryable candidate snapshots with a
   relationship delta in the write receipt (substrate rule 4 composed with
   lever 3).
4. Runtime overlays and broadcast edits (lever 5) wait for field evidence,
   exactly as the structural-change-language plan insists.

Levers 1+4 alone should clear the read portfolio's 2x gate on read-heavy
tasks. Levers 2+3 convert the write-side 43% and the read-side 2x into a
compounding whole-loop 3x, because they eliminate the translation and
verification rounds *between* the two tools.

Every lever inherits the existing keep-gate discipline: correctness gates all
timing, matched native and CLI controls, counterbalanced replicates, and
separate mechanism/activation/bootstrap accounting. No lever is kept because
it is elegant; each must delete measured rounds on a frozen task.

## Bitter-Lesson boundary

All six levers remove mechanics: tool-entrance ceremony, representation
translation, transcript overflow, verification plumbing, and repeated
addressing. None encodes architectural judgment. The model still decides what
to read, what a change means, and whether the semantic authorities' verdicts
are acceptable.
