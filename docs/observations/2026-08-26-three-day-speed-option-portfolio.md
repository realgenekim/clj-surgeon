# Three-day speed option review: 5x exists, but not on every stratum

<!-- agent-usage-window-end: 2026-08-27T02:24:00Z -->

**Window:** 2026-08-24T07:00:00Z through 2026-08-27T02:24:00Z.

**Question:** Which ideas from the last three days were implemented, which
were disproved, and which still have credible option value for reaching a
matched-correctness 5x gain over native editing?

## Answer

The 5x goal has already been reached on one important task shape. A complete
owner-level cleanup finished in 30.418 seconds versus 176.346 seconds for a
correct native route: **5.80x**. The independently replicated Anvil cohort on
the same source-volume-eliding mechanism reached **4.61x**. Small batches did
not: six replacements were only **1.05x** faster. The product win is therefore
not "Surgeon is a faster text editor." It is:

> When the decision is complete and names structural owners, Surgeon can avoid
> making the model read and reproduce hundreds of source lines.

The current 15-owner extraction benchmark is the harder remaining hill. Direct
Surgeon is 37.871 seconds versus 122.278 seconds for matched-correctness native,
or **3.23x**. A 5x result on that same control requires at most **24.456
seconds**, a further **13.415-second** or **35.4%** reduction from Surgeon.

That reduction will not come from one parser micro-optimization. The credible
portfolio combines one cheaper extraction proof, one fewer verification/model
boundary, and—if the platform permits it—a cheaper materializer phase after
architectural judgment is complete.

## What shipped and what it bought

| Idea | Status | Evidence and conclusion |
|---|---|---|
| Compact `edit_clojure` transactions | Shipped | One guarded call now spans exact replacements, named owners, files, namespace forms, owner deletion, and bounded computed programs. This is the foundation of the 4.61x and 5.80x cleanup wins. |
| Exact owner deletion and `within.namespace=true` | Shipped | Removed source reproduction and one redundant namespace-name/refusal round. A nine-file cohort produced 3.67x and 8.24x correct paired wins; the one-file cleanup later reached 5.80x median. |
| Grouped root EDN editing | Shipped | Twelve files changed atomically in one 240 ms local round trip and 110.55 ms server time. This lowers mechanics and risk, but has no matched native wall-time claim. |
| SCI programmable edits | Shipped, selectively routed | One expression can compile many guarded changes, and caller effort stayed flat from 1 to 60 sites. It remained near a 20–26 second model floor; site count alone did not create 5x because native can also batch homogeneous patches. |
| Short/deferred advanced skill | Shipped | Reduced one matched skill route from 48.465 to 35.367 seconds. The remaining skill read still cost 8.577 seconds relative to direct MCP, so ordinary compact routing moved into always-loaded instructions. The cheapest common skill invocation is no invocation. |
| Global compact routing for Codex and Claude | Shipped | Agents are taught to use one structural transaction only when it removes material work and to keep native editing for small visible changes, prose, and new files. A post-install behavior cohort is still required. |
| Direct supplied extraction | Shipped | Removed the public plan phase. Matched result: 37.871 seconds versus 49.941 seconds for plan-plus-apply, saving 12.070 seconds and one action. Against native it is 3.23x. |
| Internal extraction compiler for mechanical omissions | Shipped | `65e72b7` compiles required visibility and counts once inside apply. Independent acceptance: supplied 39.150 seconds, mechanically derived 37.500 seconds; both one apply plus one exact lint. Genuine ambiguity and stale hashes refused before effects. |
| Complete owner vocabulary plus Levenshtein hypotheses | Shipped | Hard rank-7 recovery improved 26.8% and removed native discovery. Easy rank-1 recovery initially regressed 15.5%; combined improvement was only 2.35%. The value is complete evidence, not fuzzy authority. |
| Selector decision contract | Shipped | The 162-byte instruction tells the model that listed owners are real snapshot evidence, rank is non-authoritative, semantic selection is allowed, and exact retry verifies it. In the learning cohort it removed an easy-case native search and saved 4.013–6.427 seconds of refusal-to-retry time. |
| Uniform MCP clocks | Shipped | Every success and typed refusal now has one server-owned `elapsed_ms`. This made it possible to distinguish subsecond tool work from model/route delay. |
| Plain nREPL and 512 MiB heap | Shipped | Old-to-new peak RSS fell 53.6%, from 1,003.6 to 465.9 MiB, while preserving hot reload. `make nrepl` is now also capped at 512 MiB. This improves box capacity and experiment cycle time, not the warm task's 5x ratio. |

## Attractive ideas that failed or reached a stop gate

| Idea | Decision | Why |
|---|---|---|
| Expose fewer MCP tools | Stop as a latency hill | An edit-only profile removed 90.7% of catalog text but was 15.6% slower in the small cohort; total model input remained about 45k tokens. Keep the tested profile seam for experiments, not as a claimed speed mechanism. |
| Compact snapshot `plan_id` | Do not ship | It cut visible boundary bytes only 49.5–52.4%, below the 60% gate, and did not independently reduce deliberation. Best decision interval remained 7.088 seconds, above the 3-second gate. |
| Mandatory public planning | Removed for complete decisions | It added one model boundary and 12.070 seconds. Planning now appears only for genuine unknowns; mechanically provable facts compile inside apply. |
| Automatic generic `verify=fast` inside extraction | Reverted | It rejected warnings even though the task's exact contract was `clj-kondo --fail-level error`. The rollback was safe. Verification compression remains possible only with byte-for-byte equivalent repository-declared semantics. |
| Broad 780-line fuzzy ranker | Rejected | The simple ranker already put all six strict corrections in the top ten. Complete source-free owner vocabulary carried more safety and recall than more ranking machinery. |
| Levenshtein as automatic selection authority | Forbidden | Ranking helps presentation only. It cannot resolve semantic paraphrases, wrong-file owners, or vocabulary migration safely. |
| More narration suppression | Stop | It did not remove the planning/model boundary. The winning change was deleting the boundary, not making prose around it shorter. |
| Formatter as the extraction's 12-second culprit | Disproved for the current route | Direct formatter work is subsecond at repository scale; later extraction replays measured about 1.0–1.7 seconds. The 12.06-second attribution was a wrapper/stage-accounting outlier, not formatting computation. |
| A universal SCI/editor catalog | Do not pursue | The bitter-lesson-compatible surface is a small mechanical algebra plus model judgment, not an expanding taxonomy of refactor opinions. |

## What the current extraction clock says

The independent mechanically-derived acceptance run gives the cleanest
available decomposition:

```text
complete wall                                      37.500 s
  extraction apply/kernel                           8.297 s
  non-kernel residual                               29.203 s

5x target against native 122.278 / 5                24.456 s
remaining reduction required                        13.044 s
```

The 29.203-second residual is not a claim that all of it is hidden reasoning.
The retained event stream lacks timestamps between model items and does not
report the exact lint command's duration. It includes that lint process, prompt
ingestion, service scheduling, reasoning, tool-call serialization, receipt
interpretation, two short narrations, and final response generation. The
`commands.tsv` value `1407` is command-output character count, not 1.407
seconds. This is why more item-level clocks are the first experiment, not a
speculative product rewrite.

The broader three-day telemetry supports the same boundary. The MCP service
median was 196 ms; `inspect_clojure` median was 177 ms; and
`apply_clojure_changes` median was 648 ms. The receipt mixes development,
tests, and natural use, so it is not a clean production latency cohort, but it
shows that ordinary direct service time is usually much smaller than complete
agent work. Inspection batches still had a median of one request despite 449
requests across 261 batches. The dominant remaining opportunity is fewer
decision boundaries and larger coherent calls.

## Remaining options, ranked by expected information value

### 1. Attribute the residual model boundary

Add monotonic timestamps for model-item start/completion, MCP call start/end,
command start/end, and final response. Do not request private chain-of-thought.
Measure the observable decision interval. Cost is low and it prevents another
formatter-style false attribution.

**Falsifier:** If no residual interval contributes at least three seconds,
there is no single boundary worth productizing.

### 2. Make the extraction proof hot, incremental, and still complete

The single-pass compiler still owns 8.297–11.121 seconds on the 4,594-line
fixture. Profile source enumeration, parsing, owner closure, quoted-Var scans,
caller scans, and rendering separately. Reuse parsed snapshots or indexes only
when content hashes prove they correspond to the exact transaction snapshot.

**Projected option:** Reduce the kernel to 3–4 seconds, saving about 4–7
seconds. This is not the primary architecture, but the kernel is now large
enough to matter.

**Falsifier:** A phase profile shows that no safely reusable stage costs at
least three seconds, or cache validation costs as much as recomputation.

### 3. Collapse exact verification without changing its law

The route still has `apply -> model boundary -> exact lint`. A repository-owned
verification profile could run the exact declared command inside the atomic
transaction and roll back on the same exit semantics. The prior generic
`verify=fast` attempt is negative evidence: no warning policy, scope, or command
may change.

**Projected option:** Unmeasured. The current harness proves one separate
command action and 1,407 output characters, but not its wall time. Add command
start/end timestamps before assigning a savings range.

**Falsifier:** Four different existing verifier contracts do not produce
identical exit/output decisions, or complete wall improves less than 15%.

### 4. Use a cheaper materializer after judgment is complete

The SCI model-routing probe moved from Sol/high at 25.712 seconds to Terra/low
at 20.262 seconds, with both exact and one-shot. A later counterbalanced ABBA
screen on the frozen 15-owner extraction measured Sol/high at 33.409 and
35.981 seconds versus Terra/low at 30.616 and 33.068 seconds. Both models were
2/2 correct, emitted the byte-identical extraction request, used one apply plus
one exact lint, and produced the same normalized source diff. Terra/low saved
2.853 seconds or 8.2% at the median; non-kernel residual fell 2.668 seconds.
This is a real small-n signal, not yet a production design.

**Measured option:** Save 2.853 seconds in the first ABBA screen. The model and
reasoning effort changed together, so the screen does not isolate the cause.
Stop here unless item clocks show model materialization is a dominant removable
interval; this campaign is optimizing the Sol/high route, not smaller models.

**Falsifier:** The same frozen extraction does not preserve correctness and
route geometry across counterbalanced fast-materializer replicas.

### 5. Generalize decision-gap routing, not public planning

Extraction now proves the pattern:

```text
zero mechanical gaps  -> apply once
one exact missing fact -> one targeted read, then apply
genuine decision       -> refuse with completed frozen evidence
```

Apply this law to other operation families and to read missions. The three-day
receipt contains far more structural reads than applies, and inspection batches
still have median width one. A declarative read mission or snapshot continuation
can remove repeated `inspect -> think -> inspect` transitions without granting
write authority.

**Projected option:** This is the largest production complete-turn opportunity,
although it does not by itself lower the already two-action extraction
benchmark.

### 6. Expand compiled decision chords across historical tasks

The 5.80x result came from source-volume elision; the 1.05x result came from a
small batch where both tools fit in one turn. Sample historical tasks before
looking at their diffs and test at least three non-extraction shapes. The goal
is not to force one fixture from 3.23x to 5x. It is to make 5x the normal result
for the task strata where structural compression has enough work to remove.

**Gate:** At least five of six correct, at least four materially faster, and a
2x cross-task claim only after eight matched-correctness tasks.

### 7. Keep formatter/process reuse as a small-edit option

Direct formatting of the repository is fast, but wrappers and cold processes
have produced 1–27 second field outliers on small writes. Direct executable
resolution, batching, or persistent formatting may recover 0.5–2 seconds and
is important for `clj-surgeon-tmr.1`. It cannot explain or close a 13-second
extraction gap by itself.

## Credible path to a 5x extraction result

These projections are a portfolio, not additive promises:

| Independent seam | Current evidence |
|---|---:|
| Hot complete extraction proof | Projected 4–7 s; phase profile still required |
| Exact verifier inside the same transaction | Unmeasured; command timestamps and semantic parity required |
| Cheaper materializer/decision boundary | Measured 2.853 s median in a 2+2 ABBA screen |

The cheaper materializer moved the contemporaneous median from 34.695 to
31.842 seconds. Against the retained 122.278-second native median, that is
3.84x and still 7.386 seconds above the 24.456-second 5x threshold. A hot
complete proof could plausibly close much of that gap; verification compression
must not be counted until its wall and exact semantics are measured. Formatter
work or tool-schema subtraction alone cannot do it.

Run the options in increasing cost order:

```text
observable item clocks
        |
        +--> planner phase profile and hot microbenchmark
        |
        +--> exact-verifier shadow equivalence
        |
        `--> fast-materializer ablation
                   |
                   v
        combine only independently winning cells
                   |
                   v
      two counterbalanced correct replicas <= 24.456 s
                   |
                   v
        expand to representative historical corpus
```

## Issue map

- `clj-surgeon-tmr.7`: production-owned extraction-speed and historical
  extraction cleanup hill.
- `clj-surgeon-tmr.1` and `clj-surgeon-dkj`: small-write formatter/process
  latency, important but not the primary extraction lever.
- `clj-surgeon-9iy`: cross-layer request/phase timing.
- `clj-surgeon-wjz`, `clj-surgeon-p24`, and `clj-surgeon-q7l`: read/refusal
  route compression.
- `clj-surgeon-90l`: closed; keep the decision-prompt finding and reject the
  compact plan handle.

## Evidence

- `docs/observations/2026-08-26-three-day-progress-assessment.md`
- `docs/observations/2026-08-25-captains-log-the-compiled-cleanup-hit-four-point-six-x.md`
- `docs/observations/2026-08-26-captains-log-the-best-plan-was-no-plan.md`
- `docs/observations/2026-08-26-captains-log-single-pass-extraction-survived-adversarial-acceptance.md`
- `docs/observations/2026-08-26-captains-log-prompt-wins-plan-handle-stops.md`
- `docs/observations/2026-08-26-captains-log-selector-evidence-has-a-crossover.md`
- `docs/observations/2026-08-26-captains-log-read-refusals-become-codebase-esp.md`
- `docs/observations/2026-08-24-compact-editor-versus-native-pilots.md`
- `docs/observations/2026-08-24-mcp-startup-heap-breakthrough.md`
- `/tmp/clj-surgeon-agent-usage-20260824-26.json`
- `/tmp/clj-surgeon-surgeon2-65e72b7-success-arms-r2/runs.tsv`
