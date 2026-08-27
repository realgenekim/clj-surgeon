# Captain's Log: The Language Server Was Mostly Starting Itself

This log is a reproducible record of the August 27 cclsp substitution spike.
It records what was measured, how to rerun it, what remains uncertain, and the
next bounded hill. It deliberately separates direct evidence from inference.

## The question cclsp answers

Coding agents call cclsp to answer one family of questions:

> What code is semantically connected to this Var?

The common cases are exact definition discovery, reference/caller enumeration
before a rename or move, and less often incoming/outgoing call hierarchy,
workspace symbols, or diagnostics. This can be materially better than text
search because an LSP may distinguish namespace aliases, referred Vars, local
shadowing, implementations, generated relationships, and classpath symbols.

The user rarely sees cclsp for three reasons:

1. `inspect_clojure mode=prepare-change` builds a local source anchor and calls
   the semantic provider internally. The visible action is Surgeon; the hidden
   route may initialize clojure-lsp, synchronize documents, and query symbols.
2. Most direct cclsp use was concentrated early in the tool campaign. Seventy
   percent of the 255 observed Codex cclsp tool actions occurred August 7–10.
3. A semantic query is usually one planning preflight before several reads or
   edits, so it can be visually small while machine ceremony is large.

The parsed Claude Code history contained no direct cclsp MCP tool use. This has
been overwhelmingly a Codex route. That does not prove Claude never ran an LSP
through some other shell or product path.

## Reproduction boundaries

Two evidence windows were used.

### Bounded reconstructable window

```text
since: 2026-08-24T07:00:00Z
until: 2026-08-27T04:45:04Z
```

Collector receipt:

```text
/tmp/clj-surgeon-cclsp-replay-source-20260827-v2.json
sha256 fa7a04854ac209b639ea4c9dd24af81b2ead8e6adc6f9b2fd58553c6cbb38db8
```

Regenerate it from the repository root:

```sh
python3 skills/study-agent-usage/scripts/collect_agent_usage.py \
  --since 2026-08-24T07:00:00Z \
  --until 2026-08-27T04:45:04Z \
  --pretty > /tmp/clj-surgeon-cclsp-replay-source-20260827-v2.json
```

### All retained broker and agent history

The cclsp broker log was aggregated from August 1 through
`2026-08-27T14:45:00Z`. Actual Codex tool actions were selected from
`response_item/custom_tool_call` records, not by counting tool descriptions
repeated in session prompts.

Privacy-safe Codex action receipt:

```text
/tmp/clj-surgeon-cclsp-codex-actual-calls-20260827.json
sha256 29b58e66a7164794a6a27697ad5d0e92f67a35f16d64d7ddd2dd7809dcdc713d
```

The history action count is an outer tool-action count. One action may contain
a `Promise.all` that emits several broker admissions. Broker telemetry is the
authority for actual cclsp request count.

## Greatest hits

### 1. Definition/reference surfaces dominate demand

The retained broker log contains 406 cclsp MCP admissions across 53 workspace
roots:

| cclsp operation | Admissions | Share |
|---|---:|---:|
| `resolve_var_surface` | 334 | 82.3% |
| `find_references` | 48 | 11.8% |
| `resolve_var_surfaces` | 2 | 0.5% |
| All definition/reference surface | **384** | **94.6%** |
| `get_outgoing_calls` | 3 | 0.7% |
| `get_diagnostics` | 1 | 0.2% |
| Runtime/restart operations | 18 | 4.4% |

The earlier 382/406 figure excluded the two batched surface operations. The
correct complete surface count is 384/406, or 94.6%.

The outer Codex history independently shows the same shape: 142
`resolve_var_surface` actions and 64 `find_references` actions out of 255 total
cclsp actions. Direct Codex call hierarchy was rare.

This changes the architecture question. The likely replacement surface is not
all of clojure-lsp. It is a small, exact definition-plus-qualified-reference
relation, followed by semantic escalation for named gaps.

### 2. Initialization dominated retained LSP wall

The all-history broker aggregate contains:

| Measurement | Result |
|---|---:|
| cclsp MCP admissions | 406 |
| LSP sessions | 319 |
| LSP requests | 673 |
| Initialize attempts | 305 |
| Initialize complete / failed / timed out | 148 / 89 / 68 |
| Semantic LSP requests | 368 |
| Summed initialization wall | 7,813.672s |
| Summed semantic-request wall | 2,460.053s |
| Initialization share of LSP wall | **76.05%** |

Only 48.5% of initialize attempts completed. Failed plus timed-out initialize
attempts were 157/305, or 51.5%. This does not mean every failure was visible
to the caller; retries and concurrent child work can overlap one parent MCP
request.

The narrower three-day window is even more ceremonial: five initialize calls
and one semantic call. Initialization consumed 24.939 seconds of 27.275 seconds
of underlying LSP wall, or 91.4%.

### 3. The expensive operation is exactly the substitutable one

`resolve_var_surface` accounted for 2,374.705 seconds of the broker's
2,435.640 seconds of summed MCP wall: 97.5%. Its median was 2.874 seconds, p90
13.990 seconds, and maximum 56.840 seconds.

This does not prove syntax can replace every surface query. It does show that
the largest wall-time pool and the largest capability pool are the same place.

### 4. Repetition is large enough to justify a cache experiment

Of 335 subject-bearing broker admissions, 173 had unique unfenced
`[operation, workspace_root, subject]` keys. Eighty-four keys repeated, for 162
requests after the first: 48.4% of subject-bearing demand.

These are cache candidates, not safe cache hits. A correct semantic cache must
also bind the subject to frozen source hashes, project configuration, provider
version, platform/session identity, and the exact authority level returned.
The collector therefore reports `safe_cache_hits_claimed=0`.

### 5. macOS RSS hid the real footprint

A controlled cold replay created two clojure-lsp children:

| PID | CWD | macOS physical footprint |
|---:|---|---:|
| 91545 | `/Users/genekim/src.local/clj-surgeon` | 417MB |
| 98936 | `/Users/genekim/src.local/social-media-writer` | 463MB |
| 3893 | `/Users/genekim/src.local/cclsp-structural-results` | 61MB broker |
| Combined controlled family | — | **about 941MB** |

`ps` RSS showed only about 30MB for the broker and about 60MB for each child.
For these processes, RSS materially understated the footprint visible to macOS
pressure accounting.

The two children were not killed manually. The broker reaped PID 91545, CWD
`/Users/genekim/src.local/clj-surgeon`, at `2026-08-27T14:33:40.932Z`, and PID
98936, CWD `/Users/genekim/src.local/social-media-writer`, at
`2026-08-27T14:38:40.942Z`. Both processes were then absent. Broker PID 3893,
CWD `/Users/genekim/src.local/cclsp-structural-results`, remained.

This proves meaningful memory/process cost. It does not prove that cclsp caused
the historical load-average spikes to 200–400. CPU/load causality remains an
open measurement.

## The bounded replay

Eight admissions in the three-day window collapsed into approximately four
logical questions:

1. Resolve one Var surface in isolated worktree A after a stale-session refusal.
2. Prepare and execute one outgoing-call query in isolated worktree B.
3. Resolve one Var surface in isolated worktree B.
4. Resolve the same tool-repository Var twice, with an idle-TTL reap between
   the historical requests.

Three Var-surface questions could be faithfully reconstructed from the broker
log and preserved source worktrees. The outgoing-call position and user intent
could not. It remains `insufficient-evidence`, and general call hierarchy stays
a semantic escalation.

### Controlled comparison

The spike paired existing exact definition reads with a new pure scanner for
fully namespace-qualified and proven alias-qualified symbols.

| Reconstructed case | Exact definition | Syntax scan median | cclsp comparison | Verdict |
|---|---:|---:|---:|---|
| Tool repository | 23.75ms | 74.96ms | 2,907ms cold; 30ms warm | Partial: 9 exact alias references, but one bare same-namespace call still requires proof |
| Isolated worktree A | 118.91ms | 133.05ms | 151ms warm; 3,221ms retained cold | Syntax found the one real alias-qualified caller that cclsp omitted |
| Isolated worktree B | 195.29ms | 335.06ms | 5,422ms cold | Syntax returned four root-local callers; cclsp duplicated canonical/worktree identities |

For worktree B, exact definition plus syntax scan was about 530ms if performed
sequentially, versus 5,422ms for cold cclsp: about 10.2x faster. A future
single-snapshot implementation can share captured sources and avoid paying two
independent reads.

For worktree A, cclsp's 151ms warm response was close to the 133ms syntax scan,
but it omitted the actual alias-qualified caller. Fast incomplete evidence is
not a win.

The tool-repository case is the important stop condition. The syntax scanner
correctly refuses to call a bare `managed-begin` occurrence semantic evidence.
It covers the nine alias-qualified references, while the exact form reader owns
the definition. A remaining same-namespace bare call must be proven against
lexical shadowing or escalated. Two of the three reconstructed Var-surface
questions are complete with current syntax authority; the third is partial.

## What was implemented

### Permanent ethnographic telemetry

`skills/study-agent-usage/scripts/collect_agent_usage.py` now reports:

- actual cclsp tool methods from Codex and Claude tool-use records;
- MCP wall distributions by cclsp operation;
- initialization versus semantic request count and wall;
- complete, failed, and timed-out LSP outcomes;
- initialization share of LSP wall;
- a deliberately named initialization-to-parent-wall ratio, which may exceed
  one when retries or concurrent children overlap;
- privacy-safe unfenced repeat candidates with zero claimed safe cache hits.

The self-test includes Codex and Claude semantic-read calls, initialize,
semantic complete/fail/timeout outcomes, repeated subjects, and privacy checks.

### Pure syntax authority module

`src/clj_surgeon/syntax_var_refs.clj` scans one captured map of source files.
It publishes only:

- fully namespace-qualified references;
- references qualified by an alias proven in the file's `ns` form.

It excludes quote, syntax quote, `comment`, unevaluated forms, strings, and
ordinary comments. Bare symbols receive no authority. Parse failure refuses the
whole captured scan, candidate files are bounded, ordering is deterministic,
and each location carries a source hash and zero-based range.

Permanent tests are in `test/clj_surgeon/syntax_var_refs_test.clj`. The design
and stop/continue gates are in
`docs/plans/syntax-first-var-surface-spike.md`.

## Instrumentation defects found

The current cclsp `inspect_runtime.initialization_elapsed_ms` is not a frozen
duration. In the sibling cclsp checkout, `src/lsp-client.ts` computes it as:

```text
Date.now() - state.startTime
```

It therefore grows with worker age. For controlled PID 91545, CWD
`/Users/genekim/src.local/clj-surgeon`, runtime later reported 8,992ms while the
authoritative event recorded a 2,264ms initialize. For PID 98936, CWD
`/Users/genekim/src.local/social-media-writer`, runtime later reported 20,116ms
while the event recorded 2,679ms.

The sibling checkout was already heavily dirty, so this spike did not patch it
in place. The broker event log, not `inspect_runtime`, is the authority for the
measurements above.

## How to rerun the syntax replay

Use the standalone analysis nREPL, not a cold test JVM. At the time of the
spike it was port 53926, CWD `/Users/genekim/src.local/clj-surgeon`, with a
512MiB max heap.

1. Reload the new namespace:

   ```sh
   clj-nrepl-eval --port 53926 \
     "(require 'clj-surgeon.syntax-var-refs :reload)"
   ```

2. Capture a sorted map of relative `.clj`, `.cljs`, and `.cljc` files while
   excluding `.git`, `.cpcache`, `.clj-kondo`, `.lsp`, `target`, and
   `node_modules`.
3. Call:

   ```clojure
   (clj-surgeon.syntax-var-refs/scan-sources
     captured-sources
     "source.namespace/owner")
   ```

4. Run five warm repetitions. Record `:reference-count`, exact locations,
   candidate-file count, scanned-file count, and complete wall.
5. Compare with one anchored cold cclsp request and one warm request. Record
   the broker's `mcp_request_complete`, every underlying LSP event, child PID
   and CWD, physical footprint, and TTL reap.
6. Classify missing bare or semantic relations as proof gaps. Do not score them
   as syntax successes.

## The next hill: a syntax-first semantic ladder

Durable owner: `clj-surgeon-tmr.8`, **Build syntax-first Var-surface
escalation and guarded cclsp cache**.

Do not rewrite clojure-lsp. First make one Var-surface route progressively pay
only for the proof it still lacks:

```text
one frozen workspace snapshot
        |
        +-- exact named definition
        +-- fully qualified references
        +-- ns-alias-qualified references
        +-- exact quoted-Var references
        |
        +-- no unresolved bare/semantic candidates?
        |       |
        |       +-- yes --> terminal surface; do not start cclsp
        |
        +-- named proof gap
                |
                +-- valid snapshot/config/provider cache hit --> reuse
                |
                +-- miss --> one bounded cclsp escalation
```

The first product experiment should reuse the existing `prepare-change`
captured-source path, not add another workspace engine. The structural relation
must return exact authority and an explicit vector of unresolved proof gaps.
The semantic result should be memoized inside Surgeon with source/config/provider
guards, then projected through the same response algebra.

### Frozen acceptance cohort

Sample at least 20 retained `resolve_var_surface` or `find_references` requests
whose source snapshots can be reconstructed. Stratify them:

- alias/fully qualified only;
- same-namespace or explicitly referred bare symbols;
- lexical shadowing;
- protocol or implementation lookup;
- macro/generated relationship;
- hard reader conditional or classpath dependency.

For each arm report correctness, complete wall, cclsp admissions,
initializations, process count, physical footprint, CPU/load contribution, and
cache result. Do not pool warm and cold strata.

Continue if at least 60% of faithfully reconstructable questions terminate
without cclsp, every published location is exact, cold wall is at least 30%
lower, and no later step in the same route immediately needs semantics. Stop if
syntax chooses among ambiguous bare symbols, source guards cannot make cache
reuse exact, or cclsp is still needed for most routes.

## Bottom line

cclsp is solving a real problem, but history shows a surprisingly narrow one.
The system spent most of its LSP time initializing workers so it could answer
definition/reference-surface questions. A small exact syntax relation already
answers two of three reconstructable Var-surface cases, starts no process, and
in one case is more correct than the current worktree-mapped LSP response.

The earned architecture is not “delete cclsp.” It is:

> Keep structural facts hot and local. Memoize exact semantic evidence. Rent a
> language server only for a named proof gap.

That is the next hill.

## The fork changes the option boundary

The semantic broker is our fork in
`../cclsp-structural-results`. We can change both sides of the interface. This
means `cclsp` does not need to remain an agent-facing product, a per-repository
MCP registration, or even the long-term implementation name.

Use role names at the new boundary:

```text
coding agent
    |
    v
clj-surgeon                  public tool boundary
    |
    v
semantic-provider            internal capability contract
    |
    v
surgeon-semantic-broker      daemon role
    |
    v
clojure-lsp                  replaceable engine
```

The first migration should remove direct `[mcp_servers.cclsp]` registrations
from generated and already managed workspace configuration. Surgeon remains
the sole client and may answer structurally, reuse snapshot-bound semantic
evidence, or escalate to the broker. A workspace onboarding regression must
prove that internal broker preparation still works without direct agent
registration.

Do not physically rename the sibling checkout in the same change. First make
the old name disappear from the public contract and prove the cutover. Preserve
the repository path and old configuration key as explicit migration inputs and
rollback anchors. Rename the repository and implementation namespaces later as
a mechanical, independently green checkpoint.
