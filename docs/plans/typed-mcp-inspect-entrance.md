# Typed MCP inspect entrance

**Status:** Implemented experiment; keep threshold not met

**Motivating evidence:**

- [One call crossed the double-digit gate](../observations/2026-08-07-captains-log-one-call-crossed-the-double-digit-gate.md)
- [Batched inspect missed the keep gate](../observations/2026-08-07-captains-log-batched-inspect-missed-the-keep-gate.md)
- The counterbalanced write keep gate: `apply_clojure_changes` was correct in
  4/4 runs with a 24.53-second median, versus 43.19 seconds for the native
  control (43.2% lower wall time).

## Outcome

Add exactly one read-only MCP tool, `inspect_clojure`, beside the existing
`apply_clojure_changes` mutation tool. One typed call states every currently
knowable structural read question. The server captures each distinct file once,
evaluates the ordered request batch against those immutable in-memory sources,
and either returns complete bounded evidence or refuses the complete batch.

The first complete vertical slice is ordered named-form retrieval. The same
batch contract then carries compact outlines, structural matches, and
capability-limited X-ray expressions. The server invokes existing parsing,
outline, match, and X-ray kernels in-process. It never invokes the CLI or a
subprocess.

The performance hypothesis is that persistent batched reads can be at least 2x
faster than the current CLI structural route. That is a hypothesis, not a
release claim. The minimum credible keep signal is 4/4 correct, one MCP call
for batchable tasks, no shell or failed calls, at least 30% lower median task
wall, and materially fewer source-envelope bytes.

## Bitter-Lesson Boundary

The caller chooses explicit files, named forms, structural patterns, enclosing
owners, X-ray expressions, and cardinalities. The tool performs mechanical
snapshot capture, structural addressing, exact evidence preservation, and
bounded presentation. It does not choose what to inspect, infer architecture,
expand the request, repair a refused selector, or add evaluation capabilities.

Explicit non-goals:

- mirroring the CLI registry as MCP tools;
- filesystem browsing, arbitrary reads, shell access, resources, or prompts;
- unrestricted evaluation or any SCI capability beyond the shipped X-ray;
- writes, edit plans, receipts, manifests, or temporary files;
- a custom MCP Apps widget or transcript renderer;
- remote roots, OAuth, multi-user hosting, or daemon installation;
- claiming a 2x improvement unless counterbalanced measurements establish it.
- launching clj-kondo or any other subprocess to enrich an outline; the
  in-process outline leaves `forward_refs` empty rather than violating the MCP
  process boundary.

## Public Contract

### Server surface

The server exposes exactly two tools, in this order:

1. `inspect_clojure` — read-only structural perception;
2. `apply_clojure_changes` — guarded structural mutation.

It exposes no resources or prompts. `inspect_clojure` carries standard MCP
annotations when supported by the pinned SDK: `readOnlyHint=true`,
`destructiveHint=false`, `idempotentHint=true`, and `openWorldHint=false`.

### Top-level input

```json
{
  "requests": [
    {
      "id": "summary-fields",
      "operation": "forms",
      "file": "bench/summarize_clean_codex.clj",
      "forms": ["numeric-fields", "boolean-fields"],
      "expect": {"forms": 2}
    }
  ],
  "expect": {"requests": 1, "files": 1}
}
```

`requests` and `expect` are required and unknown top-level fields refuse.
`requests` is non-empty and contains at most 64 entries. IDs are nonblank and
unique after exact string comparison. `expect.requests` and `expect.files` are
required positive integers and must equal the actual request and distinct-file
counts. Java JSON containers are recursively normalized before validation.

All request objects require nonblank `id`, exact string `operation`, and
nonblank `file`. The operation is an explicit schema discriminator. Fields not
allowed by that variant refuse; unknown operations refuse.

### `forms`

Required fields are `id`, `operation`, `file`, non-empty `forms`, and
`expect.forms`. No other fields are allowed. A batch can request at most 128
form names. Names are nonblank strings and must be unique within the request.
`expect.forms` is a positive integer and must equal the requested-name count.

Every name must identify exactly one top-level form in its captured file
snapshot. A missing name, an ambiguous name, or a duplicate requested name
refuses the complete call. Success preserves requested form order and returns
for each form:

- exact source, including attached comments, metadata, commas, reader macros,
  Unicode, and `#()` spelling;
- one-based `line` and `end_line` boundaries;
- `form_type`, `name`, and `platforms`;
- the containing file and its SHA-256 snapshot hash.

### `outline`

Only `id`, `operation`, and `file` are allowed. It returns the compact
structural outline equivalent to `:ls`, including namespace, file line count,
form count, requires, forward references, and ordered form classifications and
boundaries. It never returns whole-file source.

### `match`

Required fields are `id`, `operation`, `file`, and nonblank `match`. Optional
fields are nonblank `inside` and `expect.matches`. No others are allowed.
`expect.matches`, when present, is a nonnegative integer so callers can assert
zero matches deliberately. The pattern is parsed as the existing exact
structural matcher input, never as regex or textual search.

Success returns ordered exact matches with source, zero-based structural
addresses, one-based boundaries, SHA-256 evidence hashes, reusable enclosing
owner identity (`inside`) when one exists, file hash, and total cardinality. An
explicit count mismatch, invalid pattern, missing owner, or ambiguous owner
refuses the complete call.

### `xray`

Only `id`, `operation`, `file`, and nonblank `expression` are allowed. The
expression executes through the shipped SCI X-ray entry point over the captured
source. No SCI allowlist or evaluator changes are in scope. Literal selections
retain exact source, boundaries, addresses, trace, cardinality, and hashes.
Computed analysis retains the shipped bounded EDN `value` plus compact evidence
and hashes. Every existing X-ray refusal remains a complete-batch refusal.

### Successful result

> **RETIRED 2026-09-04 — superseded by `MCP-OP-STUDY-041` and
> `MCP-OP-STUDY-044`** (`docs/intent/study-ops/study-ops-specs.md`). The
> "source-free companion" rule below — *MCP `content` contains only a concise
> human summary* — no longer holds and has not held since the O2 round-2
> change. `content[0].text` is now a SUPERSET of every `structuredContent`
> leaf: it carries each mode's rows in receipt order, the verbatim source a
> `forms` or `match` receipt returns, and a bounded `path: value` line for
> every remaining leaf, all inside the public output budget
> (`MCP-OP-STUDY-040`). The reversal is a real behavioural change for a
> consumer that logged or displayed only the formerly concise, source-free
> text channel; that channel now includes source bodies which were already
> available to structured-content consumers. The paragraph is kept, struck
> through, rather than deleted, so the promise that was made stays legible
> beside the promise that replaced it.

Full evidence is returned in MCP `structuredContent`, and so, since
`MCP-OP-STUDY-044`, is the text block — see the retirement notice above.
Stable top-level fields are:

```json
{
  "ok": true,
  "operation": "inspect_clojure",
  "read_complete": true,
  "request_count": 2,
  "file_count": 2,
  "results": [],
  "file_hashes": {},
  "source_character_count": 11204,
  "result_character_count": 14871,
  "elapsed_ms": 42.0,
  "next_action": "none"
}
```

Results preserve request order and include `id`, `operation`, and `file` even
when operation-specific evidence is empty. `file_hashes` maps project-relative
paths to lowercase SHA-256 strings. `source_character_count` is the sum of
exact source bodies returned by `forms`, `match`, and literal X-ray results;
the count does not include outlines or JSON envelope text.

The human summary names the tool, request and file counts, operation evidence
counts, ordered-snapshot/hash status, returned source characters, and elapsed
milliseconds. It contains no source bodies.

### Refusal

Any schema, path, parse, lookup, ambiguity, cardinality, kernel, or output-limit
failure refuses the complete batch as an MCP tool error. No successful request
results are returned. Stable fields are:

```json
{
  "ok": false,
  "operation": "inspect_clojure",
  "error_type": "inspect-validation-error",
  "error": "...",
  "path": ["requests", 0, "forms"],
  "read_complete": false,
  "source_unchanged": true,
  "next_action": "correct_request"
}
```

Kernel-specific stable diagnostic fields are preserved. Source bodies are not
included in refusals. Refusal creates no source, plan, receipt, manifest, or
temporary file.

### Limits

- maximum requests per call: 64;
- maximum distinct files per call: 32;
- maximum requested form names per `forms` request: 128;
- maximum exact-source output per request: 65,536 characters, inclusive;
- maximum serialized operation result per request: 65,536 characters,
  inclusive;
- maximum aggregate serialized `results`: 262,144 characters, inclusive.

Below and exactly at a limit succeed. One character above refuses the complete
batch with `inspect-output-limit`. Limits are measured on normalized Clojure
data using deterministic JSON encoding, except the exact-source counter which
counts JVM string characters. No result is truncated.

## Safety Invariants

- Only project-relative `.clj`, `.cljs`, and `.cljc` paths are accepted.
- Absolute paths, `..` segments, unsupported extensions, missing or
  non-regular files, symlink escapes, and canonical paths outside the
  configured project root refuse before file content is exposed.
- Path confinement is shared with `apply_clojure_changes`; there is one
  authority for canonical root and source resolution.
- Each distinct canonical file is opened and read exactly once per call.
- Every request evaluates against the immutable `{relative-path source hash}`
  snapshots captured before evaluation starts.
- Result and file order are derived from input order; map iteration cannot
  reorder requests.
- Any failure discards all accumulated successes. There is no partial success
  and no truncation.
- Every returned source or result is covered by its captured file hash.
- The tool never writes source, plans, receipts, temporary manifests, or
  telemetry source bodies in metrics mode.
- The imperative shell performs no subprocess launch and no unrestricted
  evaluation.

## Implementation Shape

### Functional core

Add one Babashka/JVM-compatible inspect contract namespace containing public
pure functions for:

- recursive JSON-container normalization (shared with the write contract);
- request validation and discriminator dispatch;
- request-to-existing-kernel translation;
- evaluation over supplied ordered snapshot maps;
- stable result normalization and concise summary construction;
- per-request and aggregate output-budget accounting.

Parsing helpers accept source strings or rewrite-clj locations, not file paths.
Where an existing kernel couples file I/O to computation, extract the smallest
source-taking function and leave its CLI wrapper intact.

### Imperative shell

The MCP callback:

1. validates JSON shape and lexical paths;
2. resolves every distinct path with the shared project-root confinement
   authority;
3. captures each canonical file exactly once in first-reference order;
4. computes and attaches SHA-256 hashes;
5. calls the pure batch evaluator once;
6. enforces the final aggregate budget;
7. records source-free metrics telemetry; and
8. returns callback success or error with structured evidence.

The HTTP and stdio servers use the same live handler Var so same-session nREPL
redefinition affects the next request without reconnecting.

### Repository surfaces

- add the inspect core, tool schema, callback, and two-tool registry;
- add focused pure and JVM MCP tests and protocol tests;
- add a representative read benchmark portfolio and verifier;
- document the experimental two-tool server, schema, safety boundary, and
  benchmark result;
- keep raw requests, transcripts, events, and telemetry out of Git.

## Behavior Matrix

| Dimension | Success | Refusal |
|---|---|---|
| Batch | singleton; ordered multi-request; one/many files | empty; >64 requests; expected request/file mismatch; >32 files |
| IDs | distinct nonblank strings | missing; blank; non-string; duplicate |
| Fields | exact top-level and variant fields | missing; unknown; illegal cross-variant field at every level |
| Paths | relative regular `.clj`/`.cljs`/`.cljc` | absolute; `..`; normalized escape; unsupported extension; symlink escape; missing |
| Forms | one/many ordered unique names | empty; duplicate name; missing; ambiguous; expected-count mismatch |
| Source fidelity | comments; metadata; commas; reader macros; Unicode; `#()` | no canonicalized substitute accepted as exact evidence |
| Outline | compact ordered structural data | parse failure; whole-file source is never returned |
| Match | zero/many matches; optional owner; exact expected count | regex interpretation; malformed form; missing/ambiguous owner; count mismatch |
| X-ray | literal and computed shipped contracts | extra SCI capability; shipped parser/cardinality/sandbox refusal |
| Containers | Clojure maps/vectors and Java `LinkedHashMap`/`ArrayList` | scalar/container type mismatch |
| Limits | below and equal per-request/aggregate limits | one character above either limit; no truncation |
| Snapshot | one read per canonical file; several requests share one source/hash | any read/parse/evaluation failure discards all results |
| Telemetry | metrics contain shapes/counts/timing only | source bodies never recorded in metrics mode |

## Test Plan

### Pure tests

- valid singleton and multi-file batches;
- all four variants and mixed ordered batches;
- request-order and requested-form-order preservation;
- duplicate IDs and duplicate requested form names;
- missing and unknown fields at every nesting level;
- illegal fields for each operation;
- empty requests and forms;
- aggregate request/file expectation mismatches;
- missing, ambiguous, and duplicate forms;
- exact comments, metadata, commas, reader macros, Unicode, and `#()` source;
- real Java `LinkedHashMap` and `ArrayList` input;
- output limits below, equal to, and above every boundary;
- deterministic normalization and ~~concise source-free summaries~~ text
  blocks that carry every receipt leaf (RETIRED 2026-09-04; superseded by
  `MCP-OP-STUDY-041` and `MCP-OP-STUDY-044`).

### Boundary and protocol tests

- absolute path, parent traversal, missing path, extension, and symlink escape;
- each distinct file read exactly once through an injected reader;
- one coherent snapshot for several requests using one file;
- the real two-file/seven-form benchmark request;
- real outline, structural match with textual decoys, and X-ray calls;
- HTTP `initialize`, `tools/list`, and `tools/call`;
- exactly two tools and read-only inspect annotations;
- same-session hot-handler reload;
- before/after tree proof of no source, plan, receipt, or manifest changes;
- metrics telemetry without source bodies.

## Benchmark

Freeze a representative read portfolio containing:

1. the known seven forms across the two benchmark summary files;
2. one outline of a source file larger than 500 lines;
3. one structural match whose file contains textual decoys;
4. one computed X-ray aggregation.

Compare persistent `inspect_clojure`, the current CLI structural route, and a
native read/grep control with counterbalanced correct runs. Correctness gates
timing. Record complete task wall, MCP and shell calls, source-bearing actions,
process startups, input/output tokens, source/result bytes, failed calls, and
exact correctness. Preserve negative results and report cold readiness,
direct-call latency, and complete task wall separately.

## Documentation and Release Checklist

- update the plan index, README, make help, vision, and changelog for the
  experimental two-tool server;
- document exact server startup, temporary Codex-home registration, schema,
  refusal, annotations, and removal;
- do not modify global Codex configuration or teach installed skills to depend
  on MCP in this slice;
- record dogfood and benchmark evidence without tracking raw transcripts;
- convert caller confusion or oversized transcripts into permanent tests or
  explicit documented non-goals.

## Verification Gates

- format every changed Clojure file with Standard Clojure Style;
- run focused pure tests and focused JVM MCP tests with exact counts;
- run HTTP and stdio protocol smoke tests on a non-live port such as 7889;
- run clj-kondo on changed Clojure source and tests;
- run `make test` and report exact test/assertion counts;
- run `git diff --check`;
- prove no raw benchmark transcripts or telemetry payloads are tracked;
- show the real two-file/seven-form invocation and observed result;
- benchmark only after protocol and boundary gates pass;
- do not claim 2x unless correct counterbalanced measurements establish it.

## Definition of Done

One isolated experimental branch exposes exactly `inspect_clojure` and
`apply_clojure_changes`. A single real two-file request returns seven exact
ordered forms from two once-read snapshots with attached hashes. Outline,
structural match, and existing capability-limited X-ray work through the same
failure-atomic bounded batch. Pure, boundary, JVM, HTTP, hot-reload, telemetry,
lint, full-suite, and no-write proofs pass. A representative three-lane
benchmark records correct results honestly, including any failure to reach the
2x hypothesis, and no raw transcript or global configuration is changed.

## Observed Outcome

The implementation completed the forms-first dogfood and then added outline,
match, and X-ray through the same batch evaluator. The representative call
resolved five ordered requests over four files with four physical reads: seven
named forms, a 713-line outline with 45 named forms, two structural matches in
a textual-decoy fixture, and a computed X-ray value of 20. The direct result
returned 12,426 exact source characters and complete snapshot hashes.

Four counterbalanced correct runs produced these correctness-gated medians:

| Route | Correct | Median wall | Shell calls | MCP calls | Result bytes |
|---|---:|---:|---:|---:|---:|
| persistent `inspect_clojure` | 4/4 | 27.969 s | 0 | 1 | 24,093.5 |
| CLI structural route | 4/4 | 32.442 s | 10 | 0 | 33,182.0 |
| native read/grep | 3/4 | 51.500 s | 4 | 0 | 24,724.0 |

The MCP route was 13.8% faster than CLI and returned 27.4% fewer result bytes.
It was 45.7% faster than the native control, whose fourth run was correctly
preserved as a source-character-count failure. The direct in-process tool
median was 132.5 ms, showing that most complete-task time remained caller
overhead. The experiment passed 4/4 correctness, one-call, zero-shell, and
zero-failure gates, but it did not establish 2x and did not reach the minimum
30% median improvement. Do not promote it on performance evidence from this
run. See the accompanying Captain's Log for caller defects converted to tests
and prompt changes.
