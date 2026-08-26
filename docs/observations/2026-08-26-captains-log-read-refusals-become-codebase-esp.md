# Captain's Log: Read refusals become codebase ESP

Date: 2026-08-26  
Workstream: SURGEON2  
Parent issue: `clj-surgeon-wjz`  
First vertical slice: `clj-surgeon-p24`

## Mission

Turn a hallucinated top-level Clojure owner from a multi-tool recovery detour
into one evidence-rich refusal followed by one exact retry.

```text
current: refusal -> rg/outline/sed -> inspect -> exact read
target:  refusal with codebase ESP -> exact read
```

The target is at most two Surgeon calls, zero native discovery calls, and zero
automatic fuzzy selections.

## Trigger

Two real `inspect_clojure` batches refused safely but displayed only:

```text
inspect_clojure
  refused · batch-form-selection-failed

→ correct_request
```

The hidden structured evidence identified the failed request and candidates,
but the visible response did not name the missing owner or show a correction.

The first request used `resolve-source-file`. The real owner was
`resolve-source-path`. The second request used
`compiles-owner-relative-top-level-insertion`. The subsequent exact owner was
`validates-top-level-insertion-without-repeating-owner-source`.

## Observed route economics

The reconstructed read route used ten read actions over 163.6 seconds. Known
direct read-tool wall time was approximately 4.54 seconds. The route returned
at least 42,245 source or shell-output characters.

```text
0 F----9 C-------25 H-------------55 L-I----------------------113 G---126 T------149 G--157 F---164
```

`F` is refusal, `C` is corrected core read, `H` is support read, `L` is
outline, `I` is intent read, `G` is native search, `T` is test read, and the
last `F` is native fallback.

A better refusal alone can remove approximately 11 to 13 seconds from the
observed route. A credible tenfold mission improvement needs coherent read
compilation in addition to refusal repair. The first hill climb nevertheless
targets refusal repair because it has low coupling and immediate field value.

## Option-value strategy

The experiment portfolio uses this working model:

```text
option value scales with (N × K / t) × sigma
```

- `N` increases when diagnostics, ranking, exact authority, continuation, and
  mission compilation remain independent modules.
- `K` increases when Brain Fleet and Anvil run alternatives in parallel.
- `sigma` is highest at uncertain seams such as owner hallucination, stale
  snapshots, partial batches, and unknown mutation state.
- `t` falls when isolated worktrees, small pure seams, executable evidence, and
  terminal receipts reduce complete verified decision time.

## Brain Fleet convergence

Fable, Sol, and two Anvil agents converged on five product modules:

1. Complete selector diagnostics.
2. Selector-local snapshot continuation.
3. Exact clue resolve-and-read.
4. Snapshot-bound retry compilation.
5. A declarative read-mission graph.

The safety agents rejected fuzzy selection, partial results presented as
complete, write authority from refused reads, and blind mutation retry after a
missing receipt.

## Bitter Lesson pivot

Gene proposed viewing the options through the Bitter Lesson: give smarter
models better general perception instead of teaching the tool an expanding set
of special-case corrections.

The design now has two disjoint channels:

- `hypotheses` contain bounded real owner evidence for model reasoning;
- `authority` contains only exact mechanical proof.

The model may say, "I think you meant this owner." Surgeon decides only whether
the claim has proof. Rank, score, lexical order, a unique top result, or a large
score gap never grants authority.

The cheapest winning route is stateless:

```text
perception -> hypothesis -> exact caller retry
```

Continuation and automatic retry remain deferred until this route fails its
field gates.

## Corpus and ranking experiment

A bounded Codex-history pass found one dense session with 15 selector failures.
It produced eleven useful patterns. Six are strict one-to-one corrections
confirmed by a subsequent exact read. Five are negative, inferred, ambiguous,
or wrong-file cases and do not belong in ranker acceptance metrics.

The corpus includes character typos, token substitutions, semantic test-name
paraphrases, older API vocabulary, and correct owner names sent to the wrong
file. It is recorded in
`2026-08-25-owner-hallucinations-need-one-shot-evidence.md`.

The first broad ranker experiment evaluated 13 hypotheses. It treated 11 as
recoverable and two as negative. A skeptical review later found that only six
recoveries had strict one-to-one evidence, so the broad table is exploratory
rather than an acceptance result.

| Ranker | Rank@1 | Rank@3 | Rank@10 | MRR | Returned characters | False authority |
|---|---:|---:|---:|---:|---:|---:|
| normalized Levenshtein | 4/11 | 7/11 | 9/11 | 0.5009 | 19,013 | 0 |
| Damerau-Levenshtein | 4/11 | 7/11 | 9/11 | 0.5009 | 19,013 | 0 |
| kebab-token Jaccard | 6/11 | 9/11 | 9/11 | 0.6667 | 17,660 | 0 |
| character trigrams | 5/11 | 8/11 | 9/11 | 0.5939 | 18,794 | 0 |
| token/trigram hybrid | 6/11 | 8/11 | 9/11 | 0.6439 | 18,839 | 0 |

Token Jaccard won the tested set. The broad experimental implementation was
correct but cost 780 lines: 270 production ranker lines, 95 exact-resolver
lines, 291 ranker-test lines, 120 resolver-test lines, and four test-runner
lines. It is retained as an option receipt, not accepted as production code.

An independent evaluation used only the six strict pairs:

| Ranker | Rank@1 | Rank@3 | Rank@10 | MRR |
|---|---:|---:|---:|---:|
| normalized Levenshtein | 50.0% | 83.3% | 100% | 0.644 |
| normalized Damerau-Levenshtein | 50.0% | 83.3% | 100% | 0.644 |
| kebab-token ordered features | 66.7% | 100% | 100% | 0.806 |
| character trigram Dice | 66.7% | 83.3% | 100% | 0.756 |
| 80% token and 20% Levenshtein | 66.7% | 100% | 100% | 0.806 |

All tested rankers put all six strict corrections in the top ten. This makes
normalized Levenshtein a credible minimal presentation ranker. The more complex
hybrid improves top-three order but not top-ten recall.

The inferred and vocabulary-migration cases show that ranking alone cannot
guarantee semantic recovery. The refusal should therefore return every real
top-level owner name when the name-only vector fits the public result budget,
plus a ranked top ten for fast scanning. General evidence is cheaper and more
reliable than more ranking machinery.

## Implemented checkpoints

The integration branch is `codex/read-path-option-portfolio` in the isolated
worktree `/Users/genekim/src.local/clj-surgeon-read-path-option-portfolio`.

| Commit | Checkpoint |
|---|---|
| `8175b65` | Design exact read-mission compilation. |
| `492bc02` | Separate read hypotheses from authority. |
| `21b46f9` | Record the real owner-hallucination corpus. |
| `4fedb92` | Design bounded owner hypotheses on refusal. |
| `e832bdd` | Specify the read-selector recovery portfolio. |
| `b37c35e` | Integrate the stateless diagnostic foundation. |

The branch rebased onto SURGEON1 commit `3b0278a`. The merge preserved
`MCP-OP-EDIT-005` and the `within.namespace=true` design.

The synchronized pre-read-change baseline passed 217 tests with 1,802
assertions and zero failures.

## Parallel experiment receipts

- Diagnostic probe `5f8e5daf702eb6b4a9f741d6bd1472c489403f8d` removed
  continuation state and passed 219 MCP tests with 1,822 assertions.
- Ranker probe `7599a6ecbfd22dde213c34140bf9f3b721048618` passed 11
  focused tests with 83 assertions and 620 core tests with 5,318 assertions.
- The ranker probe remains experimental because its production surface is too
  large for the measured benefit.

## SURGEON1 coordination

SURGEON2 registered in Code Directory and proved the bridge route to
SURGEON1. SURGEON1 reported that complete-turn cost is dominated by decision
fragmentation rather than direct MCP execution. His corrected compact-write
arms were 3.63 and 4.69 times faster than native controls.

The shared structural seam is a pure recovery compiler with hypothesis and
authority outputs. Read continuation and write authorization remain separate.
The first read slice avoids SURGEON1's production files. Shared runtime
`:7888` remains load-bearing and has not been restarted or schema-swapped.

## Safety laws

- Fuzzy evidence may rank but never select.
- Each missing owner receives an independent candidate ranking.
- The complete candidate universe owns ranking, even when presentation is
  bounded.
- A refused read remains `ok=false` and `read_complete=false`.
- A refused read returns no ordinary successful results and grants no write
  authority.
- A missing mutation receipt means `unverified`, not failed, and cannot trigger
  blind retry.
- Repeated top-level names require cardinality and content evidence.

## Completed gate

The accepted ranker is normalized Levenshtein, limited to ten names per
missing owner. It exposes rank, basis, and `authority=false`, but no numeric
score. The refusal also returns every unique source-free owner name that fits
a 32,768-character presentation budget. It reports exact returned and omitted
counts when that vector is larger. The rejected 780-line experiment remains
an option receipt.

Direct red-to-green witnesses passed 38 tests with 337 assertions. The cold
core milestone passed 612 tests with 5,272 assertions. The cold MCP milestone
passed 221 tests with 1,849 assertions, zero failures, and zero errors.
clj-kondo reported zero errors and zero warnings. Heap and cclsp regression
gates also passed.

### Clean-context replay

The original four-file refusal returned all six owners from `mcp_paths.clj`
and ranked `resolve-source-path` first for `resolve-source-file`. The refusal
took 1,076 ms. The corrected four-file, six-form retry took 1,272 ms and
returned `read_complete=true`.

The original test-owner refusal returned all 31 owners from
`mcp_contract_test.clj`. The intended
`validates-top-level-insertion-without-repeating-owner-source` owner ranked
seventh. The refusal took 354 ms. The corrected exact retry took 385 ms and
returned `read_complete=true`.

Neither refusal contained source bodies, executable authority, ordinary
success results, or retained continuation state. Both missions completed in
two calls without `rg`, outline, `sed`, or whole-file reads.

An independent replay found that the first human summary exposed only the
wrong rank-one hypothesis for the semantic test-name case. The structured
result contained the correct rank-seven owner, but a text-only caller could
still waste a retry. The summary now prints the complete bounded owner
vocabulary. The same evidence compiler moved into the transport-neutral
exact-form selector, so CLI and MCP refusals cannot drift. A direct CLI handler
replay returned all six `mcp_paths.clj` owners and ranked
`resolve-source-path` first without source bodies or authority.

The inner-loop instructions in `CLAUDE.md` now tell agents to start
`make nrepl` when discovery finds no worktree-local server, verify the JVM
working directory, reload changed namespaces, and reserve cold processes for
milestone gates. On the implementation replay, the warm nREPL ran three MCP
test namespaces (38 tests and 337 assertions) in 2.4 seconds. This does not
replace cold verification; it lowers experiment latency between gates.

The first cold milestone found two implemented requirements whose second
`@spec` IDs shared a comment line and were therefore invisible to the intent
scanner. The warm nREPL exposed the exact two missing implementation witnesses;
separate annotations made the audit coherent, and the repeated cold MCP gate
passed. This is the intended division of labor: warm probes make change cheap,
while cold gates preserve startup, transport, and repository-contract safety.

### Projected route economics

The conservative projection counts only the discovery call that complete
owner vocabulary makes unnecessary. It does not claim that lexical rank is
always correct.

```text
BEFORE
  T0       T1              T2               T3
  | refuse | rg/outline    | inspect owner  | exact retry |
  |--------|---------------|----------------|-------------|
           +1 to +3 discovery calls; model reconstructs vocabulary

AFTER
  T0                         T1
  | evidence-rich refusal   | exact retry |
  |-------------------------|-------------|
    all owner names + top 10      terminal read
```

Projected calls per hallucination incident fall from 3-5 to 2, a 33-60%
reduction. Native discovery actions fall from 1-3 to zero. Applied to the
fleet's observed 10-call, 163-second read route, removing two discovery calls
projects 8 calls and approximately 130 seconds if latency scales linearly: a
20% route-time reduction. The measured two-call replays completed in 2.35
seconds and 0.74 seconds inside an isolated warm JVM; these are mechanism
measurements, not fleet wall-time claims.

In option-value terms, the slice reduces `t` without reducing `N`, `K`, or
sigma: the model sees more real alternatives, pays fewer discovery turns, and
still must choose an exact owner. Lower change cost increases the number of
safe experiments per unit time.

The hill climb deliberately prefers one small evidence-rich refusal over a
large adaptive recovery engine. The simpler tool removed `rg` and `sed`, so it
wins this round.
