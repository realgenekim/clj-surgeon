**Astra: make the supplied split plan executable, and use the REPL to interrogate the candidate**

Gene’s question: “Riff with Astra on clojure strengths and how ideal tool could take maximal advantage of these strengths to minimize wall time for the views refactor. See if we can reduce the planning time. Using repl??!!”

My commitment: **target 40 seconds of planning and approximately 100 seconds from orientation through all four oracles on this task.** These are engineering forecasts, not measured results. The enabling combination is a machine-ready mapping, a reusable projection over exact source and resolved references, and an emitter that consumes that projection directly. Merely providing a running JVM, or replacing Python with Clojure, does not deliver that reduction. R1 and R2 demonstrate why.

The strongest Clojure idea here is to make the proposed refactor a persistent value that the model can query, revise, and execute. Most of the four minutes were spent constructing that value and the machinery around it from scratch. The architectural partition had largely already been chosen.

This investigation was read-only apart from this report. I read the six stopwatch logs, saved scripts, supplied plan and architecture oracle, and bounded tool-call/result portions of the six caller sessions. No Surgeon operations or runtime connections were made. The study-agent-usage collector’s existing Claude parser was used in memory, with bytecode writes disabled; I did not run its writing/collection workflow or advance a study marker. The user’s single-output restriction takes precedence over those skill defaults. Primary documentation was checked for the language/tooling claims below. No implementations, scripts, or tests were rerun against the refactor worktrees.

**What the clocks actually measure**

All figures below are seconds from `orient-start`, rounded to tenths, from the original [N1](/var/tmp/forge/plan2/N1/stopwatch.log), [N2](/var/tmp/forge/plan2/N2/stopwatch.log), [T1](/var/tmp/forge/plan2/T1/stopwatch.log), [T2](/var/tmp/forge/plan2/T2/stopwatch.log), [R1](/var/tmp/forge/plan2/R1/stopwatch.log), and [R2](/var/tmp/forge/plan2/R2/stopwatch.log) logs. The requested planning interval has a median of 232.1 seconds across these six observations. This is a descriptive cohort, with only two observations per route, and no variance-based causal inference.

| Arm | `plan-complete` | `moves-complete` | `callers-complete` | All four oracles stamped | `done` |
|---|---:|---:|---:|---:|---:|
| N1 | 248.4 | 281.9 | 337.5 | 392.5 | 392.6 |
| N2 | 183.0 | 183.2 | 247.7 | 330.6 | 330.6 |
| T1 | 230.6 | 660.3 | 933.9 | 1065.3 | 1065.3 |
| T2 | 220.6 | 750.1 | 957.9 | 1039.3 | 1039.3 |
| R1 | 266.4 | 266.4 | 365.1 | 450.6 | 459.7 |
| R2 | 233.6 | 362.0 | 434.2 | 519.8 | 537.2 |

The phase label is not a consistent “before first move” boundary:

- N1 generated the destination files at **21:20:13**, before its **21:20:21** plan stamp. The generator, including promotions, positional rewrites, imports, and requires, is inside its planning interval.
- N2 first generated files at **21:18:52**, repaired its dependency data and namespace header generation, then stamped planning at **21:19:22**. Its 0.15-second plan-to-moves interval does not measure the cost of generation.
- R1 generated the files at **21:44:07**, before stamping both phases at **21:44:16**.
- R2 stamped planning at **21:43:46**, then queried promotions at **21:43:52**, wrote its generator at **21:44:29**, and repaired it before successful emission at **21:45:39**. Considerable work that N1/N2/R1 counted as planning sits in R2’s moves phase.
- T1/T2 mostly leave mutation machinery and extraction execution after their plan stamps. Their much longer subsequent phases are not evidence of a more expensive architectural partition.

Therefore the observation to explain is **183–266 seconds in a self-stamped preparation bucket**, not six independently measured four-minute architectural planning operations. Future measurement should distinguish `mapping-accepted`, `projection-ready`, `candidate-ready`, `first-source-write`, and `all-oracles-green`; the writer should emit the write stamp itself. Keep complete wall as the final authority.

The source was 4,594 lines, but the sessions do not establish that each model read every line into context. They chiefly read the namespace header, definition inventories, the architecture test, and selected bodies or suspicious references. Their programs scanned the full source. File ingestion and model comprehension are different costs.

**The supplied plan still needed repair**

The [plan](/var/tmp/forge/plan2/split/plan.md) contains **139 destination bullets: 127 literal-name entries, of which only 126 are distinct, plus 12 `^:private` placeholders**. `row-controls` appears twice; `row-controls*` is missing. `fb-tags` and `submissions-page` are explicitly left for the caller to place. The source inventory is **141 actual definitions: 73 `defn`, 55 `defn-`, and 13 `def`**, plus three `declare` forms and one `ns` form. “129” is consistent with excluding the twelve metadata-bearing private constants from the 141-definition inventory; it is not the actual number of unique literal names in this plan.

The reconciliation is:

`126 distinct bullet names + 12 named replacements for placeholders + row-controls* + fb-tags + submissions-page = 141`.

The twelve placeholder assignments are unambiguous in hindsight: `face-pool-size` to avatar; `default-start-date`, `default-end-date`, `example-name`, `example-location`, and `example-website` to event-setup; `date-fmt`, `datetime-fmt`, `when-fmt`, and `iso-date-fmt` to format; `speaker-inputs` and `md-token` to public-cfp. The callers placed `row-controls*` and `submissions-page` in review, and `fb-tags` in form-builder.

That last pair was actual remaining design discretion. The rest was repairing a lossy representation of already supplied intent. A machine should not silently invent a missing destination from the spelling of a name. Repair this fixture’s manifest once, preserve the decisions in it, and stop charging every arm for recovering them.

There is a second inventory trap: N1’s saved kondo output contains **146 var-definition records**, because the three `declare` forms account for five declaration records. A tool must distinguish declaration occurrences from the 141 defining forms. A map keyed only by the first name in a top-level form loses that distinction.

**What those minutes contained, and the best attribution I can make**

The scripts establish the operations; the stamps and session tool timestamps establish broad intervals. They do not separately time model reading, drafting, and interpretation. The following is a **representative 232-second attribution model**, calibrated to the observed preparation median, not a measured per-stage trace. The ranges express uncertainty and route differences; their endpoints are not additive. Generator work is counted here when it occurred before the stamp, and must not be counted again as later movement.

| Sub-step | Best representative cost | Plausible range | Evidence and work performed |
|---|---:|---:|---|
| Read task, plan, source header/inventory, and relevant bodies | 23 s | 15–35 s | All six orient in the worktree, read the plan and source, and locate exceptional names. This does not mean displaying all 4,594 lines. |
| Reconcile names, placeholders, duplicate, and unmapped forms | 30 s | 20–55 s | Native `map.py`; R1 `f3.clj`/`f4.clj`; R2 `work/map.clj`; T2 `mapping.txt`. Each produces an executable owner map. |
| Obtain source boundaries and form classification | 20 s | 10–30 s | N1/N2 write scanners; R1/R2 write line/chunk scanners in Clojure; T2 reconstructs `outline.json` despite its earlier tool outline. |
| Build the dependency graph and investigate false edges | 45 s | 25–80 s | Token scans, string/comment scrubbing, suspicious-reference reads, then kondo in N1/T1/R1/R2. N2 removes a known false reference manually; T2 still uses a heuristic graph. |
| Derive and inspect necessary promotions | 12 s | 5–20 s | Join private definitions against cross-destination references; eight actual promotions. Some generators do this internally. |
| Handle source order, forward references, and declarations | 15 s | 5–30 s | N2 `fwd.py`; R1 `f9.clj`; assignment/filtering of the three old declarations. |
| Synthesize view requires, external aliases, imports, and caller strategy | 22 s | 10–40 s | N1 `gen.py`; N2 `build.py`; R1 `f7.clj`/`gen.clj`; R2 generator. Includes inspecting kondo field shapes. |
| Check the projected architecture and choose execution order | 15 s | 5–30 s | Compare edges with foundation/public rules, investigate apparent cycles, choose a dependency-respecting extraction/load order. |
| Write the generator or extraction orchestration, including early repair | 40 s | 30–65 s | Positional edits, header construction, trivia ownership, path conversion, promotion edits, file grouping. Some arms defer most of this past the stamp. |
| Setup and phase-boundary slack | 10 s | 0–30 s | Tool discovery/documentation or REPL-client preparation, plus uncovered gaps. REPL readiness overlaps other work; this row is not the full readiness interval. |
| **Representative total** | **232 s** | — | Attribution estimate; do not treat these rows as independent timers. |

Three concrete timelines constrain that estimate:

**N2, the shortest preparation bucket:** by +21.7 s it submits `parse.py`; +27.8 s inspects unnamed forms; +56.8 s submits the mapping/dependency scripts; +62.7 s checks `edit-form`; +85.7 s inspects callers; +114.4 s submits the forward-reference scan; +153.7 s submits the generator; +166.5 s removes the false `edit-form` dependency and regenerates; +176.3 s repairs header generation; +183.0 s stamps planning/moves. The final approximately 69 seconds after the forward-reference probe are substantially generator construction, correction, and inspection. [N2 session, starting at parser call](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-a39eed948b82f3bdf.jsonl:25).

**R1:** readiness is stamped at +30.4 s; the chunk parser runs at +51.5 s; private constants/declarations at +60.1 s; name reconciliation at +71.8 s; provisional mapping/graph at +86.4 s; false-edge details at +100.0 s; suspicious-source reads at +105.6 s; kondo runs at +116.4 s; the resolved graph is returned at +123.2 s; the architecture test is read at +131.4 s; external requires/promotions are returned at +167.9 s; kondo field inspection at +193.6 s; declares/imports at +221.4 s; generation at +257.1 s; stamps at +266.4 s. This is an interactive analysis-and-generation program, spread over many evaluations. [R1 session](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-a33bb678998e2d45f.jsonl:33).

**R2:** readiness is stamped at +82.8 s, but source inventory and architecture reading already occurred during that interval. At +130.4 s it submits the chunk parser, +145.4 s validates its mapping, +165.4 s submits the token graph, +169.9 s checks the false `edit-form` edge, +179.8 s invokes kondo, and +190.7 s receives the resolved graph. The next tool action is the plan stamp at +233.6 s: approximately **43 seconds with no intervening tool action**. That gap cannot honestly be labeled graph execution, architectural deliberation, or generator drafting from the recorded tool transitions alone. [R2 session](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-a6246532b92ef7ee4.jsonl:68).

The execution arithmetic is revealing. N1’s saved kondo summary reports **173 milliseconds** for the one-file analysis, with 2,801 var usages, zero errors, and fourteen warnings. R1’s graph, promotion, and generation calls generally return in roughly 0.06–0.19 seconds of visible request/result span. These spans are not profiler measurements and omit background startup. Nevertheless, another 10× speedup in the graph algorithm will not recover minutes. Removing the need to author and interpret successive graph programs could.

**The graph work was semantic, but largely mechanical**

The recurring false edge is `form-builder-page → portal/edit-form`: `edit-form` is a local binding in the form-builder body. Namespace-level symbol resolution alone will happily find the unrelated Var with that name. R1’s less selective scan also finds false dependencies through documentation/markup text, including `board-region` and `form-edit-panel`. T1’s initial private set contains the spurious `edit-form` and misses `initials`; kondo corrects both. T2’s third scanner strips strings and comments but still reports the local-binding false edge. These are consequences of using identifier spelling without lexical resolution.

The routes differ materially. [N1’s generator](/var/tmp/forge/plan2/N1/gen.py) consumes kondo references and rewrites symbol-name positions. [N2’s dependency scan](/var/tmp/forge/plan2/N2/deps.py) stays lexical; its caller explicitly patches `usage['form-builder-page']` to remove `edit-form`, then uses `:refer` between view namespaces. [T2’s analyzers](/var/tmp/forge/plan2/T2/analyze3.py) remain heuristic. A report that says all six switched to kondo during planning would be false.

I recomputed the mapping join in memory from N1’s saved data, without running an analyzer or touching the worktree. For this snapshot it produces **43 distinct inter-view namespace edges and 173 distinct cross-destination symbol sites**. The necessary promotions are exactly:

`answer-input`, `datastar-script`, `field-error`, `field-errors`, `header`, `initials`, `not-blank`, `req-mark`.

The two retained same-destination forward references are `dev-strip → time-travel-bar` and `row-controls → row-controls*`. Preserve the corresponding declarations in organizer-layout and review. The old declaration of `datastar-script`, `cfp-note`, and `portal-draft-status` spans destinations after the split; requires replace that relationship. This is not a license to remove arbitrary declarations: the compiler must check definition occurrences, source order, and evaluation context. Also, an intra-namespace function cycle can be legal; rejecting every cycle in the form graph would confuse it with a forbidden cycle in the projected namespace graph.

Promotion is almost entirely determined by the already authorized policy: publish private helpers used across new namespaces. Require synthesis is likewise mostly a join between source ownership, reference targets, and the mapping. Alias collision handling and fully qualified references still need implementation. Architecture checking is a graph predicate once the supplied policy is represented: targets belong to the six foundations or review; auth/portal/public-cfp do not acquire organizer-layout; views do not depend on server; the namespace graph has no cycle. The [actual architecture oracle](/var/tmp/forge/plan2/split/view_architecture_test.clj) remains an independent acceptance check.

**What R1/R2 actually did with the live JVM**

Both [REPL briefs](/var/tmp/forge/plan2/split/brief-R1.md) requested loading the application and inspecting public Vars and metadata. The recorded planning actions do **not** do that. Neither uses `ns-publics`, `ns-interns`, `ns-resolve`, `resolve`, or Var `meta` to establish the plan. They start a JVM, verify `(+ 1 2)`, and keep analysis values/functions in it. The application namespaces are first explicitly loaded in the post-generation flow: R1 at 21:44:22, R2 at 21:45:46. A live analysis JVM is not yet a loaded, current application image.

R1’s substantive planning is [f1.clj through f9.clj](/var/tmp/forge/plan2/R1/f1.clj), followed by [gen.clj](/var/tmp/forge/plan2/R1/gen.clj). R2’s is [analyze.clj](/var/tmp/forge/plan2/R2/work/analyze.clj), [map.clj](/var/tmp/forge/plan2/R2/work/map.clj), [graph.clj](/var/tmp/forge/plan2/R2/work/graph.clj), and [graph2.clj](/var/tmp/forge/plan2/R2/work/graph2.clj). These really execute in Clojure. They still implement line chunking, regex name recognition, mapping joins, graph repair, alias selection, and string splicing afresh.

**They did not leave the main graph/generator in Python.** Python supplies the handwritten bencode transport in both arms. R1 additionally uses Python for a later qualified-caller correction and the grep oracle; R2 uses it for its grep oracle/refinement and stamp reporting. Bash still reads source, starts the JVM, invokes kondo, and drives each evaluation. The corrected criticism is “they recreated the native scripting algorithm in Clojure,” not “they forgot to move it out of Python.”

The 30.4/82.8-second readiness intervals are not clean JVM startup costs. R1 launches at +15.6 s and subsequently writes its client. R2 launches at +23.9 s, writes its client, reads source and the architecture test, and checks the already-existing port at +78.6 s. R2 deliberately starts with `:run-tests:nrepl`, while R1 starts with `:nrepl`; the former already includes the test dependencies that R1 later has to add. We cannot infer an 83-second startup penalty, or save 83 seconds simply by prestarting the JVM.

R2 also illustrates the cost of an unnormalized analysis API: the first generator uses usage `:row/:col/:end-col` as though those were symbol-name spans, fails with out-of-bounds replacements, then switches to `:name-row/:name-col/:name-end-col`. Its diagnostic reports 172 bad positions, then zero after the correction, with 173 valid rewrites. This repair occurs after its planning stamp, but belongs economically to learning and implementing the transformation. An exact-spans snapshot should make that entire mistake unavailable. [R2 generator repair](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-a6246532b92ef7ee4.jsonl:98).

The REPL does supply a real later benefit: affected tests take approximately **0.8 seconds** in both arms; R1 records 36 tests and 301 assertions passing. R2 runs the architecture test warm in roughly **0.3 seconds**. But R1 first discovers a missing `ring-mock` test dependency and adds it; both still perform the requested cold checks. These runs support fast warm feedback. They do not establish a planning benefit from live introspection that was never exercised.

**The five counterfactuals**

Savings here are estimates against the observed preparation work, holding other affordances fixed. They overlap and must not be added. For (d), distinguish a bare interpreter from a reusable transformation library.

| Change | Work that vanishes or shrinks | Work that remains | Predicted planning effect on this task |
|---|---|---|---|
| **(a) A current, loaded image answers resolution and metadata directly** | Inventory/private-status/arglist probes become a small batch. Existing aliases/imports are available as maps. A warm test classpath removes setup surprises. | Destination choices, source-preserving spans, lexical-use edges, declaration ordering, requires after repartitioning, generator construction. `ns-publics` omits privates; `ns-interns` is needed. | About **5–20 s** saved by introspection alone, assuming the image already matches the files. Cold loading can erase the gain. It does not collapse 230 s to 30 s. |
| **(b) Forms as data with exact positions** | Caller-written chunk scanners and name/metadata parsing disappear; declaration occurrences remain distinct; symbol spans no longer require schema archaeology. | Mapping repair, lexical binding resolution unless included, graph projection, policy checking, emitter construction. A plain outline is insufficient. | About **20–40 s** planning saved; an additional **20–40 s** of R2-like positional repair may disappear later. T2 warns that a returned outline must remain directly reusable by the next operation. |
| **(c) `split_plan(mapping)` returns edges, promotions, cycles, requires** | Token graph, false-edge investigation, promotion joins, declaration analysis, require/import planning, and repeated architecture checks become one projection. It can return witnesses instead of asking the model to reconstruct its logic. | Repairing a malformed mapping, reviewing actual exceptions, applying the candidate and running the oracles. If it returns only a report, the caller still writes a generator. | Approximately **80–120 s** saved when built on (b). A read-only projection with the current rough plan suggests **100–150 s** to a reviewed projection, before any separately authored emitter. |
| **(d) Submit the transformation program as Clojure to execute in the image** | With a small existing split library: Python/Clojure translation, repeated shell scripts, serial “print intermediate result” turns, and regenerated intermediate files shrink. The program can consume the returned value directly. | Someone must specify the mapping and policies. Arbitrary code does not invent a correct parser/emitter or prove behavior. Errors still need witnesses and repair. | **Bare eval: no demonstrated saving; R1/R2 are close to that experiment.** With reusable snapshot/project/emit functions, **30–60 s additional caller construction** can disappear; overlap with (c) is substantial. |
| **(e) Machine-ready plan with all names and dispositions** | All twelve placeholders, the duplicate-name bug, missing `row-controls*`, and two open placements disappear from the task-time loop. Mapping entry and verification become ingestion and an exact coverage check. | Source-hash/identity validation, edge projection, review, execution, and proof. | **25–45 s** saved; perhaps more if it also prevents retyping 141 assignments into a new language. This does not remove graph analysis by itself. |

Why isn’t `resolve` the graph oracle? It answers namespace resolution, not whether a particular token was bound by a `let`, destructuring, or a function parameter at that source location. Loaded Vars do not expose an ordinary complete outgoing-source-reference relation. Tracing executed calls also misses untaken paths. Macro expansion can expose useful semantics but can evaluate macro code and discard the original written shape. Keep the analyzed source relation and supplement it with the image where the image answers a distinct question. The distinction between `ns-interns`, `ns-publics`, and namespace resolution is explicit in the [Clojure core API](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/ns-interns).

**The planning entrance I would build**

Give the model a single task-shaped entrance, backed by pure Clojure functions. It should accept **the mapping itself or a mapping artifact reference**, not require an outline call followed by a 141-entry transcription into another call. The following is a proposed API sketch, not an existing callable operation or an executed request:

```clojure
(split-plan
  {:workspace-root worktree
   :source "src/cfp_scheduler_killer/views.clj"
   :mapping-file "views-split.edn"
   :scope {:paths ["src" "test"]}
   :expect {:defining-forms 141 :destinations 20}
   :policy {:visibility :promote-cross-destination
            :body-order :preserve
            :trivia :preserve
            :aliases :preserve-existing-then-disambiguate
            :declarations :retain-required
            :architecture architecture-policy}
   :return :review-and-prepared-candidate})
```

The machine-ready manifest names every source definition and destination namespace/path, distinguishes definitions from declarations, and includes the chosen destinations of the previously unmapped forms. It carries a source identity or is validated against one on ingestion. Namespace names are explicit; the tool must not guess them from the first `/src/` in an absolute pathname. The architecture policy is a small explicit data representation of the supplied rules, with the real oracle retained for acceptance. Do not pretend arbitrary test code can automatically be converted into policy data.

In **one external call**, the service captures a coherent immutable source snapshot, obtains exact form and symbol anchors, runs or reuses compatible kondo analysis, joins reference owners with the mapping, projects the new namespace graph, derives promotions and declaration obligations, synthesizes requires/imports and collision-free aliases, locates all old-namespace callers in scope, and prepares the candidate source edits. It reparses and checks the candidate’s projected structure before returning. No source mutation is part of this planning call.

This has straightforward relational semantics. For every resolved usage `u`, find its defining owner `o`, its target Var `v`, and their destinations `M(o)` and `M(v)`. If the destinations differ, contribute an edge and a positional rewrite; if `v` is private, contribute a promotion. Group external usages/import requirements by the owner’s destination. Compute strongly connected components on the **namespace projection** and return a witness for each violating component. For same-destination forward references, retain the necessary declarations or an explicitly selected safe ordering strategy. Unknown ownership/resolution must remain explicit instead of silently dropping rows.

The return should have a small review surface and a retained full value. A usable first reply should usually fit in roughly 2–4 KB; the complete plan stays available by ID and as EDN. That size is a design target, not a claim that every exceptional split fits it:

- Snapshot ID plus source/config/classpath-analysis identities, mapping identity, scope and coverage. Exact coordinate convention and original-token guards belong to the retained edit records; callers should not interpret raw kondo columns.
- `:status :ready`, `:needs-decision`, or `:unsupported`, with precise reasons. A ready plan has no hidden unresolved obligations within its declared coverage.
- For this fixture: 141 definitions covered once, 20 destinations, 43 projected inter-view edges, eight named promotions with referencing-owner witnesses, the two required forward declarations, and the disposition of the old cross-destination declaration.
- Requires, imports, aliases, external caller edits, cycle/architecture results, and a prepared candidate ID. Return full collections as queryable data; show a bounded summary by default. Never turn truncation into apparent completeness.
- An exact diff or changed-form projection when requested, along with named outstanding checks. **A prepared candidate is not four green oracles.**

If the current Markdown plan is submitted instead, return all missing/duplicate/placeholder problems in one response. Do not solve one placeholder per round trip. For this already understood task, the best entrance accepts the repaired manifest directly.

The model’s REPL interaction should be with that value, for example:

```clojure
;; Proposed library sketch; not executed in this investigation.
(def p (split-plan spec))
(select-keys p [:coverage :promotions :declarations :violations])

;; One batch of actual questions, if the compact review needs explanation.
(mapv #(explain p %)
      [[:promotion 'answer-input]
       [:edge 'cfp-scheduler-killer.views.form-builder
              'cfp-scheduler-killer.views.portal]])

;; If intent changes, derive a new value against the same source snapshot.
(def p2 (reproject p (update spec :policy revised-policy)))
(plan-diff p p2)
```

`explain` for the hypothetical form-builder→portal edge should say that there is no resolved edge, with the relevant local-binding witness if that rejected candidate relation is retained. It should not print a page of source and ask the model to rediscover scope. `reproject` reuses the source/analysis snapshot and computes only consequences of the new mapping/policy. If the sources changed, the service reports that and refreshes the affected evidence; immutable data does not make external files immutable.

When the plan is ready, the separate application entrance consumes its candidate ID without re-enumerating the forms or regenerating the transformation program. It guards the snapshot, applies the prepared move plus caller rewrites, and returns named checks and remaining obligations. The REPL can reload affected namespaces and run focused behavioral probes. The final cold proof remains. This is where a full tool earns the 100-second forecast; a read-only graph report alone cannot claim it.

For exceptional transformations, let the model submit a small Clojure function over the same snapshot and plan values. Keep candidate construction as a pure computation, with source writes owned by the application layer. That provides Lisp’s expressive advantage without making every caller rebuild string-splicing machinery. Starting with a large unrestricted “eval anything and spit files” interface would preserve R1/R2’s principal costs and greatly enlarge what must be tested.

**Why the Clojure strengths matter specifically to planning**

**Homoiconicity buys cheap semantic representation and composition.** Forms, symbol metadata, destination maps, edges, and proposed edits can participate in the same sequence/map operations. The model can supply a transformation instead of narrating one and then translating it through JSON, Python, and shell. But a read Clojure form is not lossless source: comments, whitespace, reader-discarded forms, and original reader syntax need a concrete syntax representation or retained source spans. For this refactor, preserve bodies and rewrite proven symbol tokens. Reprinting every form is unnecessary churn. The [reader reference](https://clojure.org/reference/reader) describes the distinction between source characters and Clojure data; my tool-design conclusion is an inference from it.

**The live image buys answers and cheap experiments.** It can provide current namespaces, Var metadata, aliases, imports, and testable function behavior. That makes an unresolved semantic question inexpensive when the application is already loaded. It should not force this static partitioning task to boot the application before obtaining a dependency projection. A live image of the tool’s analysis values is already useful even when the application is not loaded. R1/R2 obtained that persistence; the missing asset was a reusable analysis library.

**Immutability buys cheap alternative plans.** Keep one snapshot and compare two destination maps, visibility policies, or alias policies without mutating the project or rebuilding an index. A cycle witness can be explained as a consequence of a small mapping change. The original plan stays available when the experimental one fails. Clojure’s persistent collections make this a natural implementation approach, although a Python program can also implement it. The economic claim is fewer rebuilding and recovery steps, not a magical runtime advantage. [Clojure data structures](https://clojure.org/reference/data_structures).

**REPL/reload buys a cheaper uncertainty budget.** With a ready test image, it is reasonable to probe a disputed rendering behavior immediately rather than accumulating speculative checks before the first move. That can reduce defensive planning as well as testing time. However, plain `require :reload` can leave removed definitions alive and conceal a bad split; macro consumers and captured function values add reload obligations. A scoped reload strategy helps, and the cold suite remains valuable. These are documented limitations in [tools.namespace](https://github.com/clojure/tools.namespace), and the 0.8-second affected tests are this cohort’s concrete benefit.

**Kondo as data is the most immediate planning advantage.** The needed reference relation already exists in these artifacts. It discriminates local bindings from Vars and supplies symbol-name locations. The missing tool is primarily a join/projection and normalization layer, not a new parser or a new compiler analysis. Analysis completeness, hooks, reader branches, and unresolved cases still require explicit treatment. The [analysis data documentation](https://cljdoc.org/d/clj-kondo/clj-kondo/2023.07.13/doc/analysis-data) distinguishes usage spans from name spans—the distinction R2 had to rediscover. This is why I would begin here before developing runtime call-graph introspection.

**nREPL buys a stable conversation with the image.** Multiple response messages, request IDs, completion status, and retained evaluation state support one batch of questions and subsequent evaluations. None of those protocol properties computes the split projection for us. Ship the client and return readable EDN results; do not make every caller write bencode framing and scrape printed diagnostics. R1/R2’s dozens of quick evaluations still incur model/tool boundaries. A program composed of several pure queries can return one result. [nREPL protocol overview](https://nrepl.org/nrepl/design/overview.html).

**Babashka buys a cheap entrance when there is no warm image.** A `bb` command can ingest the manifest, call the serialized kondo wrapper, run the pure projection, and produce the same EDN result. That avoids paying JVM readiness merely to join maps. Babashka is not the application JVM and cannot substitute for arbitrary app dependencies or the cold acceptance suite. Its fast startup and substantial-but-incomplete Clojure support are described in the [Babashka book](https://book.babashka.org/). Prefer the same pure planning functions behind both a Babashka entrance and a persistent service, where their dependencies permit it.

**The argument against my proposal, and the decision**

The skeptical case is strong: this task was unusually favorable to mechanization. It had an almost-complete mapping, ordinary named definitions, known callers, and a concrete architecture guard. Writing a robust general split compiler can cost much more than repeatedly writing a 150-line script. Kondo is not omniscient; a current runtime is not guaranteed; preserving reader behavior and declaration semantics is real work. A rich projection can become another giant thing the model reads, then ignores while writing its own script. T2’s reconstructed outline is a warning. A new mandatory entrance could simply add latency.

The affirmative case is equally concrete: six independent callers repeatedly built inventories, maps, dependency approximations, exception handling, and generators. The same `edit-form` trap recurs across routes. The required graph contains only 43 namespace edges. N1 already demonstrates 173 exact positional rewrites from a static analysis whose reported execution took 173 milliseconds. The repeated cost is caller construction and interpretation, not difficult graph computation. A reusable compiler for this witnessed class can eliminate whole actions.

I would build the **small source-snapshot plus kondo projection first**, with a machine-ready plan and a reusable prepared candidate. Expose it as an ordinary Clojure function and a thin one-call adapter. Add runtime introspection where a question actually needs it. I would not lead with bytecode graph extraction, a general macro evaluator, or a new per-form editing grammar. The REPL should be the place to explore the candidate and test uncertainties; the default path for a supplied, valid mapping should need little exploration.

**Wall forecast, with no oracle hidden**

The 40-second planning estimate assumes: the repaired machine manifest is already supplied; the service is warm or uses a fast static entrance; existing analysis can be validated or rebuilt cheaply; the scoped source constructs are supported; the tool returns a directly applicable candidate; and the model trusts the stated coverage enough to review the summary rather than reread all bodies. It does not assume zero model latency or zero interpretation.

| Remaining planning work | Budget |
|---|---:|
| Read task scope, supplied mapping identity, and architecture obligations | 6 s |
| Form and submit the one task-shaped request | 6 s |
| Snapshot, exact-position normalization, analysis, graph/policy projection, candidate preparation | 3 s |
| Review coverage and the eight promotions/two declarations, plus caller scope | 12 s |
| One bounded semantic/exception review, or equivalent judgment time if no query is needed | 8 s |
| Decide the candidate is ready and transition to application | 5 s |
| **Planning total** | **40 s** |

The 3-second service allocation is a prediction, supported only in scale by the existing small analysis/script runtimes. It must be measured for the full projection, including scope reads and candidate construction. A slow serialized-analyzer queue is additional wall; it cannot be reported as zero merely because the CPU computation is cheap. The 8-second review allowance is not a mandatory redundant tool call.

| Complete task stage | Additional budget | Cumulative wall |
|---|---:|---:|
| Reviewed, applicable split plan | 40 s | 40 s |
| Consume candidate, guarded move and all caller rewrites; model/tool transition included | 15 s | 55 s |
| Load/reload affected code and run focused warm probes | 6 s | 61 s |
| Oracle 1: one cold `bin/kaocha unit --fail-fast` | 22 s | 83 s |
| Oracle 2: old file absent; oracle 3: placement/count check | 1 s | 84 s |
| Oracle 4: the actual architecture test, through its requested cold command | 10 s | 94 s |
| Read the four results and establish completion | 6 s | **100 s** |

Cold unit verification is supported by approximately 21–23 seconds in the stamps. R1’s cold architecture call takes about 9.7 seconds, R2’s about 10.2 seconds. The forecast keeps the four oracles in the specified order and retains the separate architecture command. The warm probes are additional feedback, not substitutes. Install the supplied architecture test before the final cold suite to avoid discovering late that another full suite is needed after changing the test tree. The cold focused architecture command still runs afterward as requested.

I would preregister **30–60 seconds planning and 80–140 seconds to all four oracles** as the plausible range for a successful supported run, centered at 40/100. With the existing Markdown plan instead of a repaired machine manifest, add roughly 25–45 seconds. With a read-only projection but no reusable emitter/caller rewrite, expect roughly 150–220 seconds total rather than 100: the missing construction work must be paid somewhere. A capability refusal or source conflict can exceed those ranges; report its repair wall and first-attempt failure, not only the eventual successful execution.

The native endpoints are 331–393 seconds, so 100 seconds predicts approximately 231–293 seconds saved, or about 3.3–3.9× throughput for this exact complete task. That forecast combines planning and application improvements. **Planning-only savings are nearer 143–208 seconds against the native stamps**, subject to their mixed phase boundaries. It would be wrong to credit the tool arms’ many later extraction/repair minutes to planning optimization. Build and measure before making a performance claim.

**The three cheapest interventions, ranked by expected planning seconds saved per build hour**

These are rough incremental build estimates for this witnessed task, assuming the existing parser/kondo facilities are reusable. They include a focused validation of the fixture behavior; they do not price a general production refactoring platform. Savings are per eligible future run, not cumulative fleet savings. The interventions overlap.

| Rank | Concrete intervention | Expected planning seconds saved/run | Build estimate | Seconds saved per build hour |
|---|---|---:|---:|---:|
| **1** | Repair the plan producer to emit exact EDN names/destinations, validate 141 distinct definitions and declaration identity, and record the three missing-name dispositions once | **40 s** | **1 h** (0.5–1.5 h) | **40 s/h** |
| **2** | A reusable read-only `split-facts`/`split-plan` function: exact owners and name spans + kondo join → projected edges, promotions, required declares, requires/imports, and architecture witnesses; one compact result | **105 s** | **4 h** (3–6 h) | **26 s/h** |
| **3** | Ship the nREPL client and a ready analysis/test setup; batch initial probes; remove per-caller bencode authoring and dependency discovery | **15 s** on an R-style run | **0.75 h** (0.5–1 h) | **20 s/h**, R-eligible only |

The third intervention saves essentially zero on a native/static run that never needs nREPL. Weighting equally across this six-arm cohort would reduce its estimate to roughly 5 seconds/run, or 7 seconds per build hour. Do not mandate a REPL just to manufacture its saving, and do not award it the whole 30/83-second readiness interval. If every future task has an already prepared image, omit this intervention because its marginal saving is zero.

A reusable source-preserving emitter and caller rewriter is the next larger investment: approximately 6–12 build hours as a scoped prototype, perhaps 40–60 additional planning seconds plus substantial post-plan savings per run. It is necessary to approach the complete 100-second target, even though its planning-only return per build hour is lower than the first two. Generalizing its correctness contract beyond this fixture costs more. A bare “send Clojure code to the JVM” feature ranks poorly because these arms already had that capability.

For the next experiment I would compare native with the same machine-ready manifest against projection-assisted execution with that manifest, then separately compare static and warm-image entrances to the **same** pure planner. Freeze source/config identities, measure request-to-all-oracles wall, stamp first actual source write, retain failures and unknowns, and count whether the caller still writes a scanner, graph script, or emitter. The mechanism succeeds when those artifacts disappear from the caller’s work. The decisive product promise is: **supply the partition; receive an explainable, applicable candidate; spend REPL time on the few questions that remain.**

**Evidence index**

The primary native scripts are [N1 parse](/var/tmp/forge/plan2/N1/parse.py), [N1 map](/var/tmp/forge/plan2/N1/map.py), [N1 provisional analysis](/var/tmp/forge/plan2/N1/analyze.py), [N1 saved kondo data](/var/tmp/forge/plan2/N1/kondo.json), [N2 parse](/var/tmp/forge/plan2/N2/parse.py), [N2 forward references](/var/tmp/forge/plan2/N2/fwd.py), and [N2 generator](/var/tmp/forge/plan2/N2/build.py). The tool-arm planning artifacts include [T1 dependencies](/var/tmp/forge/plan2/T1/deps.py), [T1 corrected graph](/var/tmp/forge/plan2/T1/ns-edges-kondo.json), [T2 first analyzer](/var/tmp/forge/plan2/T2/analyze.py), [T2 promotion analyzer](/var/tmp/forge/plan2/T2/analyze2.py), and [T2 reconstructed outline](/var/tmp/forge/plan2/T2/mkoutline.py).

Additional bounded session anchors: [N1 first generator/write](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-ae0bc32bf242ba523.jsonl:78); [N2 false-edge repair](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-a39eed948b82f3bdf.jsonl:53); [T1 corrected kondo projection](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-a3df8e09a7ced9303.jsonl:72); [T2 outline call](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-a27500dd74762b9a2.jsonl:27); [R1 focused warm tests and cold proof](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-a33bb678998e2d45f.jsonl:146); [R2 warm architecture test](/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/subagents/agent-a6246532b92ef7ee4.jsonl:174).


Signed: **Astra — gpt-6-astra**

`date -u`: **Mon Sep  7 22:09:27 UTC 2026**
