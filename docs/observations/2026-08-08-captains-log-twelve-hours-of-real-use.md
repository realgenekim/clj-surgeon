# Captain's Log: Twelve hours of real use

<!-- agent-usage-window-end: 2026-08-08T07:00:00Z -->

**Window:** 2026-08-07 19:00Z through 2026-08-08 07:00Z; noon through
midnight Pacific.

**Question:** After a day of heavy dogfood use, is clj-surgeon becoming the
structural editing exocortex described in the vision, or is it adding ceremony?

## Bottom line

Both claims are true at different layers.

The transaction compiler is already valuable. Ninety-seven successful MCP
write transactions materialized 246 verified edits across 127 changed files.
Half of those transactions contained two or more edits. One transaction made
38 exact edits. Another changed 14 sites across eight files. Those are the
operations that native patching forces the model to carry as mechanical state.

The complete caller route is still too long. A Codex turn that used Surgeon had
a median 8.5 Surgeon calls; the 90th percentile was 27. The tools themselves
were rarely the delay. Direct Surgeon work occupied 0.83% of complete turn wall
at the median. Most elapsed time was the model deciding, recovering from a
refusal, reconstructing what remained, or waiting for semantic evidence.

The product has crossed the mutation threshold but not the interaction
threshold:

```text
exact mutation compilation       proven
multi-edit transaction value     proven
one decision -> one transaction  proven in selected runs
one intent -> short full route   not yet the default
semantic map reliability         failed during this window
```

This complements the benchmark result in
[Last Night's Hill Climb](2026-08-08-captains-log-last-nights-hill-climb.md).
The controlled portfolio proved that one-shot MCP can be 2.46 times faster than
native. This study shows how often real callers actually reach that route.

## Post-window field report: availability can erase the value

A later agent scored Surgeon **7/10 overall** and its current MCP availability
**3/10**. The distinction matters.

The structural read path was excellent. Repeated `:ls` and named-form `:cat`
calls exposed changed form shapes and prevented a guessed patch. When the
semantic MCP connection works, its exact caller surfaces provide evidence that
narrow text tools cannot provide.

The complete paved road did not work in this run. The semantic request returned
`invalid-mcp-session`. The sanctioned CLI recovery then encountered the known
SCI `FileLock.close` refusal. The agent completed its bounded inspection with
CLI structural reads and used narrow native patches, but the MCP route was not
available for the transaction it was meant to serve.

This is not evidence against structural reading or exact caller surfaces. It is
evidence that operational availability is part of correctness. A tool that is
excellent only after manual session repair is not yet one shot. P1 issue
`clj-surgeon-9gj` tracks recovery from an invalid MCP session into one live
semantic transaction.

## Method

`make study-agent-usage` produced one schema-versioned receipt for the exact
window. The collector now joins four evidence surfaces:

1. Codex and Claude task histories;
2. clj-surgeon MCP telemetry;
3. cclsp MCP admission telemetry when present; and
4. clojure-lsp request telemetry emitted by cclsp.

The receipt contains no transcript prose, source bodies, commands, raw service
events, or workspace paths. Session and workspace identities are hashed.
Service timings and outcomes are aggregates.

One bounded transcript inspection sampled three high-call turns after the
aggregate route could not explain their task boundary. The samples were
anonymized as a broad service refactor, a small rendered-markup repair, and a
viewer application. No project source or domain context is retained here.

The aggregate receipt remains temporary at
`/tmp/clj-surgeon-agent-usage-2026-08-07-12h.json`. Raw logs were not copied into
the repository.

## Evidence coverage

| Evidence surface | In-window coverage |
|---|---:|
| Codex sessions | 20 |
| Codex sessions with Clojure-relevant activity | 10 |
| Codex task turns | 120 |
| Codex turns using Surgeon | 66 |
| Claude sessions | 5 |
| Claude sessions with Clojure-relevant activity | 1 |
| Surgeon MCP service sessions | 4 |
| cclsp/clojure-lsp workspaces | 5 |

The Claude sample does not support a caller comparison. Its single relevant
session used native reads and no Surgeon call during this exact window. Earlier
Claude work fell outside the noon-to-midnight bound. Treat every Codex-versus-
Claude conclusion as unavailable, not as zero adoption.

## The write path crossed the usefulness threshold

| MCP write evidence | Result |
|---|---:|
| Calls to `apply_clojure_changes` | 146 |
| Successful transactions | 97 |
| Verified edits committed | 246 |
| Changed files | 127 |
| Transactions with at least 2 edits | 48 |
| Transactions with at least 2 files | 24 |
| Median edits per successful transaction | 1 |
| 90th-percentile edits per transaction | 4 |
| Maximum edits in one transaction | 38 |
| Median direct apply wall | 222 ms |
| 90th-percentile direct apply wall | 1.420 s |

Four production-shaped transactions show the range:

| Edits | Files | Direct wall |
|---:|---:|---:|
| 10 | 1 | 134 ms |
| 14 | 8 | 5.180 s |
| 22 | 1 | 1.119 s |
| 38 | 1 | 7.286 s |

This is the strongest evidence for keeping the feature. The compiler owned
matching, write order, atomicity, read-back verification, and the inverse
receipt. The caller did not have to perform 84 independent patch operations.

The evidence also prevents an exaggerated claim. Native Clojure patch actions
still outnumbered Surgeon apply route actions, 386 to 183. Some native patches
were appropriate one-off edits. Others show that the new route has not yet
become the irresistible default.

## The read path is bounded but still conversational

| MCP read evidence | Result |
|---|---:|
| Calls to `inspect_clojure` | 557 |
| Successful calls | 476 |
| Structural requests carried | 804 |
| Distinct file requests carried | 789 |
| Median requests per successful call | 1 |
| 90th-percentile requests per call | 3 |
| Total source characters returned | 1,507,486 |
| Mean source characters per inspect call | 2,706 |
| Median direct inspect wall | 101 ms |
| 90th-percentile direct inspect wall | 1.234 s |

The read surface is doing useful token control. It normally returns one or a
few exact forms rather than a namespace. Batching exists, but the median caller
still asks one question per call.

The operation mix shows how agents understand the product:

| Inspect operation | Count |
|---|---:|
| Named forms | 572 |
| Outline | 329 |
| Structural match | 39 |
| X-ray computation | 4 |

Agents use Surgeon as a precise `cat` and `ls` far more often than as a
structural computer. X-ray remains a demonstrated capability, not an adopted
habit. The next design should not assume that more X-ray documentation alone
will change this distribution.

## Tool latency is not the main cost

Across the 703 MCP calls recorded by the service:

| Direct service metric | Result |
|---|---:|
| Total direct tool wall | 11.82 min |
| Median call | 111 ms |
| 90th-percentile call | 1.264 s |
| Longest call | 58.194 s |

The 66 Surgeon-using Codex turns represented 24 aggregate agent-hours because
several sessions ran concurrently. Their median complete turn was 527 seconds.
Direct Surgeon work consumed 9.53 minutes inside those turns. At the median,
direct tool wall was 0.83% of turn wall; at the 90th percentile it was 2.89%.

The comparison is not a controlled performance result. User steering and long
goals can extend a turn. It is strong enough to reject one hypothesis: shaving
another 50 ms from a normal MCP call will not produce the next large gain.

The 58.194-second outlier was a successful two-file structural read containing
one outline and one named form. It returned only 1,135 source characters. That
should be impossible on the ordinary hot path and needs its own causal trace.

## Refusals were safe and too common

The MCP service recorded 573 successful calls and 130 refusals. The refusal
rate was 18.5%.

| Leading refusal | Count | Interpretation |
|---|---:|---|
| `batch-form-selection-failed` | 47 | Caller named a stale or wrong owner; atomic refusal worked |
| `invalid-intent-form` | 15 | Caller and compact-edit grammar disagreed |
| `invalid-mcp-request` | 15 | Published contract or caller construction failed |
| semantic provider unavailable/refusal | 15 | The graph could not supply bounded evidence |
| `verification-failed` | 7 | Safety gate correctly blocked a write |
| `expect-count-mismatch` | 6 | Cardinality guard correctly blocked a write |
| `source-file-not-found` | 5 | Workspace or path context was wrong |

Several refusals are product success. Verification and count mismatches must
remain hard failures. The aggregate nevertheless identifies avoidable recovery
rounds. Wrong owner names and malformed intent payloads account for most of the
largest category cluster.

One small markup repair illustrates the cost. The agent began with three
parallel structural reads and eventually accumulated 92 Surgeon calls over
about 16 minutes. The direct reads took fractions of a second. The task did not
need 92 distinct structural decisions. The caller kept converting each local
question into a new interaction instead of compiling the already-understood
repair.

A refusal reported after this window sharpened the safety result. A caller
classified a Hiccup selector as one complete vector because `read-string`
returned a value. The submitted text actually contained the vector followed by
an extra `]` copied from its parent. `read-string` ignored the trailing input;
Surgeon's complete-input parser refused it. The MCP remedy now reports the
unmatched delimiter and its line and column. A regression preserves both the
refusal and the unchanged-source guarantee.

## The semantic layer was the weak link

The cclsp log contained 31 bounded clojure-lsp request completions or timeouts:

| LSP method | Complete | Timeout | Timeout rate |
|---|---:|---:|---:|
| `initialize` | 13 | 7 | 35% |
| `workspace/symbol` | 4 | 4 | 50% |
| `textDocument/documentSymbol` | 0 | 3 | 100% |
| **Total** | **17** | **14** | **45%** |

Median LSP request wall was 25.291 seconds. The 31 requests consumed 599.589
seconds of direct service wall. This is the concrete meaning of “the scalpel
was fast, the map timed out.” Structural reads and writes were normally
subsecond; resolved semantic proof frequently paid the full 30-second deadline.

Yesterday's log predates durable cclsp MCP-admission events, so the receipt
cannot correlate every cclsp tool call with one LSP sequence. This turn adds
durable `mcp_request_complete` events for future studies. A cross-service trace
identifier is still missing.

## Progress against the vision

The vision says that the model should supply judgment once and the compiler
should own addresses, hashes, write order, rollback, and receipts.

| Vision claim | Twelve-hour evidence |
|---|---|
| Exact, bounded reading | Strong: 1.5 million returned characters across 557 calls, normally one to three requests per call |
| Failure-atomic mutation | Strong: 246 edits through 97 successful transactions |
| Multi-file compilation | Real: 24 successful multi-file transactions |
| One decision remains one transaction | Real in best cases; not the median route |
| Semantic graph can authorize broad change | Weak: 45% observed LSP timeout rate |
| MCP replaces CLI ceremony | Incomplete: 604 classified CLI Surgeon calls remained |
| MCP replaces native patch for structural repetition | Improving, but native patch actions still dominated |
| Tool wall explains task wall | False: direct tool work was usually below 3% of turn wall |

This is convergence, not boofarama. The project now has a proven moat:
failure-atomic structural transactions that compile repeated decisions. The
remaining work is narrower than the original vision and more important than
adding commands: make the shortest correct route become the route agents take.

## Recommendations

### 1. Set a caller-round acceptance gate

Tracked by `clj-surgeon-q7l`.

For a supplied semantic change, the target route is:

```text
prepare once -> decide once -> apply once -> test
```

Count structural interactions, not only kernel time. The next benchmark gate
should require a median of at most three Surgeon calls and no recovery call for
the supplied-decision portfolio. For exploratory work, permit one additional
inspect call.

### 2. Make `prepare-change` the default entrance for mutation

Agents still assemble changes through repeated `forms`, outline, and exact
replacement calls. When the user names a Var or coherent change goal, the tool
should return the complete surface, decision sites, and executable apply call
in one response. Exact-source `file`/`form` preparation must remain the bounded
route when semantic evidence is unavailable.

### 3. Turn selection failures into one executable retry

Tracked by `clj-surgeon-p24`.

Keep atomic refusal. Add enough typed data for the caller to correct the request
without another discovery call: the failing request ID, available owners,
nearest bounded candidates, and one corrected request skeleton when no guess is
required. Measure whether `batch-form-selection-failed` falls below 5% of calls.

### 4. Keep the LSP graph off the critical path when exact source is enough

Do not use `workspace/symbol` to rediscover an owner that the caller already
identified by exact file, form, hash, and range. Use source anchors first.
Start or recover only the authoritative workspace. Fail fast with retained
evidence when the provider cannot become ready inside its bounded deadline.

### 5. Add one trace ID across all layers

Tracked by `clj-surgeon-9iy`.

One identifier should connect:

```text
agent turn -> Surgeon request -> cclsp admission -> LSP requests -> receipt
```

The next study should reconstruct a slow or refused route from aggregates
without opening any transcript or raw log. This is the missing observability
join.

### 6. Explain or remove the 58-second structural-read outlier

A plain two-file structural snapshot cannot share a deadline with semantic
startup. Add per-phase timings to `inspect_clojure` and refuse if an unrelated
workspace initialization blocks a structural read. The hot structural p99
target should be below two seconds.

### 7. Treat X-ray as an advanced lane until adoption changes

Four X-rays versus 901 form/outline operations is decisive behavior. Keep X-ray
for aggregation and structural computation, but do not make ordinary edits pay
for its conceptual weight. The primary MCP contract should remain inspect,
prepare, decide, apply.

### 8. Repeat the same receipt after the current fixes

This study captured the system before exact-source publication, canonical root
identity, lossless concurrent onboarding, and durable cclsp admission logging
were complete. Repeat the same 12-hour schema after real use. The falsifiers
are concrete:

- refusal rate does not fall;
- semantic timeouts remain near 45%;
- median Surgeon calls per successful turn remains above eight; or
- native patches continue to dominate repeated structural edits.

## Tooling improvement made by the study

The study itself is now one shot:

```bash
make study-agent-usage \
  AGENT_USAGE_ARGS='--since START --until END'
```

The command reads agent histories and local service telemetry once, prints one
bounded aggregate, writes the complete receipt to its reported temporary path,
and advances no marker by itself. `--receipt-out PATH` selects a durable
destination when required. The observation author writes the receipt's
`next_marker` only after the analysis is complete.
`make study-agent-usage-self-test` covers both agent formats, CLI and MCP route
classification, Surgeon aggregation, cclsp/clojure-lsp aggregation, and the
privacy contract.

## Conclusion

The mutation compiler is the breakthrough. The current constraint is the
conversation around it.

The next large gain will not come from another micro-command or a faster parser.
It will come from making one coherent model decision stay one coherent tool
transaction, and from ensuring that the semantic graph either answers quickly
or gets out of the way with a precise refusal.
