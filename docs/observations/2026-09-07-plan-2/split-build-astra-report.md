# Namespace split build — Astra

Worktree `/home/forge/src/clj-surgeon-split`, branch `astra/namespace-split`.
No push. No battery. Retained build artifacts are in `/var/tmp/forge/plan2/build/`.
Development uses `make nrepl` with an inline alias override binding 127.0.0.1:7950,
64 MB initial / 512 MB maximum heap. PID 1604856. Warm reload and focused tests
avoid repeated JVM startup. `clj-nrepl-eval` is absent; a temporary bencode client
speaks only to this owned port. CLI and cold processes remain milestone gates.

## Batch 1 — Andon inb-731473

Registered SPLIT-REPAIR-001..004 with misreadings and boundaries. Red log:
`build/batch1-red.log`: three arity errors for missing anchored path semantics,
plus failures for library mismatch, remanufactured docstring candidate and absent
continuation. Green: 51 affected tests / 265 assertions; 588 fast tests / 6055
assertions, zero failures/errors, zero isolation violations. Oracle passed.
Kondo: zero errors/warnings, exit 0 (informational findings only).

The workspace-aware compiler derives namespaces relative to configured deps.edn
:paths anchored at workspace_root. The helper's authoritative lib is checked before
write. The helper planner now passes its classified caller set internally, including
an explicit empty set; the lower layer cannot broaden it through substring search.
A direct kernel decisions-required refusal includes its files and a planning next
call; the public extraction route retains its editable apply continuation.

The actual helper planner → boundary → kernel was hand-driven on a source defining
`versioned` and an unrelated docstring saying `versioned data`: plan and compile both
succeeded with zero caller candidates (`build/batch1-helper-handoff.log`). A second
filesystem witness verifies the public plan under `src/work/src/clj` and custom roots.

No tool schema or CLI surface changed in this repair. Legacy one/two-argument path
inference remains compatibility-only; workspace-bound public extraction uses the
new anchored arity. No speed claim is established by these correctness repairs.
b53714fb Fix extraction namespace identity and classified caller handoff
 .../intent/helper-extraction/split-repair-specs.md | 14 +++++++
 docs/plans/namespace-split.md                      | 29 ++++++++++++++
 docs/tech-tree.md                                  |  5 +++
 src/clj_surgeon/extract.clj                        | 44 ++++++++++++++++++----
 src/clj_surgeon/mcp_extraction.clj                 | 17 ++++++++-
 src/clj_surgeon/mcp_extraction_plan.clj            |  2 +-
 src/clj_surgeon/mcp_helper_extraction.clj          |  6 +++
 src/clj_surgeon/mcp_tool.clj                       |  4 +-
 test/clj_surgeon/lane_manifest_test.clj            |  3 +-
 test/clj_surgeon/mcp_extraction_plan_test.clj      | 21 +++++++++++
 test/clj_surgeon/mcp_extraction_test.clj           | 32 ++++++++++++++++
 test/clj_surgeon/mcp_intent_contract_test.clj      |  5 ++-
 12 files changed, 169 insertions(+), 13 deletions(-)



Batch 1 witness matrix:

| Intent | Red | Green |
|---|---|---|
| SPLIT-REPAIR-001 | Three missing-arity errors, including the double-/src/ path | Anchored custom-root and public filesystem plan witnesses pass |
| SPLIT-REPAIR-002 | Mismatched caller library was accepted | Typed pre-write lib/path refusal passes |
| SPLIT-REPAIR-003 | English docstring bystander reappeared as a candidate | Planner handoff and actual versioned helper probe pass |
| SPLIT-REPAIR-004 | Decisions refusal lacked planning continuation | Candidate list, relative continuation and helper envelope witnesses pass |

## Batch 2 — one whole partition, MCP and CLI

Implemented the pure compiler, both public entrances, and guarded whole-file-set
publication through the existing extraction kernel. The shared synchronous profile
and subprocess functions were moved to dependency-light leaves; existing MCP
callers delegate to them. This avoids importing nREPL into Babashka and does not
introduce a second transaction, evaluator, or reference resolver.

Commit: `913020c8` — Compile whole namespace partitions through shared proof and undo. The design is
`docs/intent/helper-extraction/namespace-split-design.md`, linked from HLD, with
NS-SPLIT-001..015, direct @spec implementation/test witnesses, and the existing
bidirectional registry/debt ratchet. No new debt was waived. Lane pins were
recomputed: 52 fast namespaces, 6 integration, 32 battery; 90 total, 464 adopted
tests, 1,462 tests in the lane corpus. The battery lane was not run.

The handoff review also completed SPLIT-REPAIR-004: kernel planning continuations
now use relative paths even when compilation receives absolute paths, and the
helper boundary preserves the decisions-required continuation and error type.
Other helper refusals continue to have no invented continuation.

### Red/green witness record

The first Batch 2 red is saved in `build/batch2-red.log`: the test namespace could
not load because the compiler did not yet exist. This is an absent-feature red,
not a claim that every assertion independently failed against a working old
compiler. The tests were written before the compiler. Later concrete paper cuts
were exposed while developing and replaying the implementation.

| Witness | Red / finding | Green evidence |
|---|---|---|
| Three destinations, cross-deps, forward declaration, private helper, alias collision, test caller | Absent compiler baseline | Literal expected graph, body references, declaration, promoted helper and caller alias all pass |
| Cycle, unmapped owner, stale reviewed hash, undecided promotion, lib/path mismatch | Absent compiler baseline | Pure grouped blockers plus filesystem boundary refusal cases; source bytes retained, no destination publication |
| Failure atomicity | Kernel previously required a non-nil future for every original file | All four write failure positions, deletion denial, successful whole commit and exact inverse pass |
| Proof failure and foreign drift | Initial completeness did not guard the post-proof inventory | Failed proof restores source/callers and mode 0640; new source file triggers rollback; foreign destination edit yields recovery-required without overwriting it |
| Recovery receipt source status | Test expected true but receipt reported source_retired=false after blocked undo | Source status now reflects the actual absent source |
| Two definitions on one line | Concrete StringIndexOutOfBoundsException from assigning the second reference to the first owner | Span/column ownership rewrites the correct body |
| Load-only caller with top-level literal | Concrete IllegalArgumentException: cannot create ISeq from Long | Literal caller parses and retained load relation becomes partition requires |
| Destination outside authorized roots | Concrete two assertion failures: plan was accepted | Typed destination-outside-roots refusal before mutation |
| Quoted private Var missing from kondo | Real test caller needed structural quoted-Var coverage | Existing quoted-Var reader supplements the captured facts; only eight necessary promotions remain |
| External alias used only by a keyword or quoted Var | Coverage expansion identified during review | Both libspec-retention witnesses pass |
| MCP surface and output | Warm schema test initially saw an old output shape | Closed schema, registered tool, callback outcome classes and complete text face pass cold |
| Actual CLI plan/commit/refusal | Babashka failed on the MCP/nREPL dependency chain | Real subprocess dispatch, dry run, successful runtime assertion and nonzero refusal all pass |
| CLI help catalog | Babashka corpus reported one missing canonical op in its expected set | Registry expectation updated; full corpus green |

Focused final validation: 26 tests / 201 assertions, zero failures/errors,
plus three existing helper profile/continuation witnesses. This includes the last
expanded before-write refusal and inverse-integrity cases. Logs:
`build/batch2-gate-affected.log`, `build/batch2-gate-fast.log`,
`build/batch2-gate-mcp.log`, `build/batch2-gate-bb.log`.
Final gate exit statuses and counts are recorded below. The last 24 additional assertions expanded existing refusal/inverse witnesses and passed in the focused run; no production behavior changed after the final cold gate launch.

No blanket analyzer success is inferred from a successful parse. The project lint
gate uses ~/bin/clj-kondo and exits zero with zero errors/warnings. The isolated
reference-analysis pass on the copied application has existing unresolved lint
findings; its exit 3 is exposed in the receipt as completed analysis, not a clean
lint result. The application suite and its architecture analyzer are separate
successful checks.

### Contract as implemented

MCP: `namespace_split`, registered in the full profile. All input objects are
closed. No per-site table or caller list is accepted. `expect` supports optional
file/form/destination/caller-file/caller-site counts. `snapshot_hash` binds an
optional reviewed plan. Plan-only is available directly on this tool; an additional
inspect_clojure mode was not needed for this slice.

CLI, same EDN request:

```sh
clj-surgeon :op :split-ns! :request-file split.edn :plan-only true
clj-surgeon :op :split-ns! :request-file split.edn
clj-surgeon :op :undo-extract! :receipt /path/from/undo_receipt.edn
```

Direct `:request '{...}'` and direct request keys are also accepted. The local
build launcher is `build/clj-surgeon`, using `bb --classpath` against this worktree;
the installed shared CLI was not replaced. Plan and mutation share the same
compiler. The receipt remains EDN and leads with state. MCP text leads with state
and contains the entire structured JSON receipt.

Required fields: workspace_root; source {file,lib}; destinations
[{file,lib,forms,alias_policy}]; promotion_policy (`promote-required` or authorized
names); source_retirement (`delete` or `retain-empty`); bounded roots; verification
{profile}. Optional constraints support forbidden_edges. Named verification
profiles come from the workspace's `.clj-surgeon.edn` and must contain synchronous
command argv vectors. Relative executables are anchored at the workspace root.

Exact executable input schema, also saved as `build/namespace-split-schema.json`:

```json
{
  "type" : "object",
  "additionalProperties" : false,
  "properties" : {
    "verification" : {
      "type" : "object",
      "additionalProperties" : false,
      "properties" : {
        "profile" : {
          "type" : "string",
          "minLength" : 1
        }
      },
      "required" : [ "profile" ]
    },
    "workspace_root" : {
      "type" : "string",
      "minLength" : 1
    },
    "constraints" : {
      "type" : "object",
      "additionalProperties" : false,
      "properties" : {
        "forbidden_edges" : {
          "type" : "array",
          "items" : {
            "type" : "array",
            "items" : {
              "type" : "string",
              "minLength" : 1
            },
            "minItems" : 2,
            "maxItems" : 2
          }
        }
      },
      "required" : [ ]
    },
    "plan_only" : {
      "type" : "boolean"
    },
    "roots" : {
      "type" : "array",
      "items" : {
        "type" : "string",
        "minLength" : 1
      },
      "uniqueItems" : true,
      "minItems" : 1,
      "maxItems" : 32
    },
    "expect" : {
      "type" : "object",
      "additionalProperties" : false,
      "properties" : {
        "files" : {
          "type" : "integer",
          "minimum" : 0
        },
        "forms" : {
          "type" : "integer",
          "minimum" : 0
        },
        "destinations" : {
          "type" : "integer",
          "minimum" : 0
        },
        "caller_files" : {
          "type" : "integer",
          "minimum" : 0
        },
        "caller_sites" : {
          "type" : "integer",
          "minimum" : 0
        }
      },
      "required" : [ ]
    },
    "promotion_policy" : {
      "oneOf" : [ {
        "type" : "string",
        "enum" : [ "promote-required" ]
      }, {
        "type" : "array",
        "items" : {
          "type" : "string",
          "minLength" : 1
        },
        "uniqueItems" : true
      } ]
    },
    "snapshot_hash" : {
      "type" : "string",
      "minLength" : 1
    },
    "source_retirement" : {
      "type" : "string",
      "enum" : [ "delete", "retain-empty" ]
    },
    "destinations" : {
      "type" : "array",
      "minItems" : 1,
      "maxItems" : 1000,
      "items" : {
        "type" : "object",
        "additionalProperties" : false,
        "properties" : {
          "lib" : {
            "type" : "string",
            "minLength" : 1
          },
          "file" : {
            "type" : "string",
            "minLength" : 1
          },
          "forms" : {
            "type" : "array",
            "items" : {
              "type" : "string",
              "minLength" : 1
            },
            "uniqueItems" : true
          },
          "alias_policy" : {
            "type" : "array",
            "items" : {
              "type" : "string",
              "minLength" : 1
            },
            "uniqueItems" : true,
            "minItems" : 1
          }
        },
        "required" : [ "lib", "file", "forms", "alias_policy" ]
      }
    },
    "source" : {
      "type" : "object",
      "additionalProperties" : false,
      "properties" : {
        "file" : {
          "type" : "string",
          "minLength" : 1
        },
        "lib" : {
          "type" : "string",
          "minLength" : 1
        }
      },
      "required" : [ "file", "lib" ]
    }
  },
  "required" : [ "workspace_root", "source", "destinations", "promotion_policy", "source_retirement", "roots", "verification" ]
}
```

### Real fixture provenance and acceptance

Copied `/home/forge/src/cc-split-N1` with `cp -a` excluding `.git`, removed the
copied generated views directory, and overlaid a git archive of d9205abc before
`git init` in `build/cc-copy`. The original N1 worktree was not modified. The copied
architecture guard is byte-identical to the supplied file: SHA-256
`4cc53bb54b59dc09294bcd8fb9b62f3f274e2ee5a8e768c1a0825ee06b50255a`.

The prose plan required the same repairs identified in the design of record:
replace the twelve ^:private placeholders with actual names, distinguish
row-controls and row-controls*, and assign fb-tags to form-builder and
submissions-page to review. The resulting mapping has 141 definitions across 20
destinations. Requests are saved in `build/cc-split.edn` and
`build/cc-split-guarded.edn`; the independent owner oracle uses the corresponding
JSON mapping, not compiler-generated future sources.

Configured `split-unit` in the copy with two commands:
`bin/kaocha unit --fail-fast` and `python3 build/cc-oracle.py` (absolute oracle path
in the actual argv). Kaocha's unit selection includes the copied architecture
test. The second command asserts views.clj absence and exactly one copy of every
mapped owner in its listed destination. All four required oracles therefore ran
inside the single mutation's proof and before its successful terminal receipt.

Hand-driven successful runs:

| Run | Entrance and conditioning | Verified call wall |
|---|---|---|
| First successful candidate | Warm nREPL boundary, ready mapping | 27.902 s reported boundary time |
| CLI replay | Real Babashka CLI, ready mapping | 30.05 s external wall |
| Final guarded replay | Real CLI, ready mapping plus reviewed snapshot/expect guards | 30.33 s external wall; 29.958 s boundary time |

Existing guarded inverses restored the source/callers and removed all 20 generated
files between replays; the second inverse was exercised through the actual CLI.
The final copy is left split and green for review. The optional preceding CLI plan
was 8.093 s; it is separate from the final mutation's 30.33 s. Planning is not a
mandatory extra call. Mapping repair, profile setup and fixture copy are excluded
from these ready-mapping timings.

These successful replays are not first-attempt-success statistics for development:
there were pre-write profile/compiler failures and repairs on the way to the first
success. This was hand-driving, not a controlled fresh-caller adoption experiment.

Final receipt (complete; durable inverse/details paths included):

```json
{
  "promotions": [
    {
      "form": "answer-input",
      "lib": "cfp-scheduler-killer.views.form-controls",
      "reason": "referenced across destination boundary",
      "reference_count": 6
    },
    {
      "form": "datastar-script",
      "lib": "cfp-scheduler-killer.views.shell",
      "reason": "referenced across destination boundary",
      "reference_count": 2
    },
    {
      "form": "field-error",
      "lib": "cfp-scheduler-killer.views.form-controls",
      "reason": "referenced across destination boundary",
      "reference_count": 7
    },
    {
      "form": "field-errors",
      "lib": "cfp-scheduler-killer.views.form-controls",
      "reason": "referenced across destination boundary",
      "reference_count": 15
    },
    {
      "form": "header",
      "lib": "cfp-scheduler-killer.views.organizer-layout",
      "reason": "referenced across destination boundary",
      "reference_count": 17
    },
    {
      "form": "initials",
      "lib": "cfp-scheduler-killer.views.avatar",
      "reason": "referenced across destination boundary",
      "reference_count": 1
    },
    {
      "form": "not-blank",
      "lib": "cfp-scheduler-killer.views.format",
      "reason": "referenced across destination boundary",
      "reference_count": 41
    },
    {
      "form": "req-mark",
      "lib": "cfp-scheduler-killer.views.form-controls",
      "reason": "referenced across destination boundary",
      "reference_count": 6
    }
  ],
  "destination_libs": [
    "cfp-scheduler-killer.views.auth",
    "cfp-scheduler-killer.views.avatar",
    "cfp-scheduler-killer.views.committee",
    "cfp-scheduler-killer.views.communications",
    "cfp-scheduler-killer.views.dashboard",
    "cfp-scheduler-killer.views.event-setup",
    "cfp-scheduler-killer.views.form-builder",
    "cfp-scheduler-killer.views.form-controls",
    "cfp-scheduler-killer.views.format",
    "cfp-scheduler-killer.views.integrations",
    "cfp-scheduler-killer.views.live-drafts",
    "cfp-scheduler-killer.views.log",
    "cfp-scheduler-killer.views.organizer-layout",
    "cfp-scheduler-killer.views.people",
    "cfp-scheduler-killer.views.portal",
    "cfp-scheduler-killer.views.public-cfp",
    "cfp-scheduler-killer.views.replay",
    "cfp-scheduler-killer.views.review",
    "cfp-scheduler-killer.views.schedule",
    "cfp-scheduler-killer.views.shell"
  ],
  "blockers": [],
  "details_path": "/var/tmp/forge/plan2/build/namespace-split-receipts/21c708fb-5c64-4ce1-a729-c71518757b85-details.edn",
  "elapsed_ms": 29958.280775,
  "counts": {
    "destinations": 20,
    "forms": 141,
    "caller_files": 5,
    "caller_sites": 87,
    "files": 26
  },
  "undo_command": [
    "clj-surgeon",
    ":op",
    ":undo-extract!",
    ":receipt",
    "/var/tmp/forge/plan2/build/namespace-split-receipts/21c708fb-5c64-4ce1-a729-c71518757b85-undo.edn"
  ],
  "verification_complete": true,
  "operation": "namespace_split",
  "coverage": {
    "roots": [
      "src",
      "test"
    ],
    "reference_authority": "clj-kondo captured snapshot plus structural quoted-Var supplement",
    "dynamic_references": "not claimed"
  },
  "mutation_attempted": true,
  "state": "committed",
  "checks": [
    {
      "name": "captured-reference-analysis",
      "exit": 3,
      "duration_ms": 3327.193151,
      "status": "completed",
      "findings": {
        "error": 108,
        "warning": 195,
        "info": 5,
        "type": "summary",
        "duration": 1228,
        "files": 56
      }
    },
    {
      "name": "future-source-parse",
      "exit": 0,
      "duration_ms": 48.100941,
      "status": "passed"
    },
    {
      "name": "split-unit/1",
      "command": [
        "/var/tmp/forge/plan2/build/cc-copy/bin/kaocha",
        "unit",
        "--fail-fast"
      ],
      "exit": 0,
      "duration_ms": 21524.526943,
      "status": "passed"
    },
    {
      "name": "split-unit/2",
      "command": [
        "/usr/bin/python3",
        "/var/tmp/forge/plan2/build/cc-oracle.py"
      ],
      "exit": 0,
      "duration_ms": 41.544018,
      "status": "passed"
    },
    {
      "name": "verified-snapshot-guard",
      "exit": 0,
      "duration_ms": 12.421597,
      "status": "passed"
    }
  ],
  "next_call": null,
  "committed": true,
  "source_retired": true,
  "map_hash": "b3ac9adc5a060e6c080db9e4b8cb1d21fde589f9dbcc06036c97b3eea588b818",
  "snapshot_hash": "43b15c857ebcc69b0a4bb8c051d13a319b6eed823a6330a382191e1bb762f938",
  "ok": true,
  "graph": {
    "acyclic": true,
    "unknown_count": 0,
    "edge_count": 540
  },
  "receipt_hash": "86eade77beb55cde563c41dddc9ab742cd3784dbe3552d3bf54e57975209b4e9",
  "undo_receipt": "/var/tmp/forge/plan2/build/namespace-split-receipts/21c708fb-5c64-4ce1-a729-c71518757b85-undo.edn"
}
```

Final single-call wall output:

```text
CLI wall_seconds=30.33 exit=0

```

Application unit + included architecture oracle output (complete retained command
output, including existing warnings):

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
WARNING: update-vals already refers to: #'clojure.core/update-vals in namespace: clojure.tools.analyzer.utils, being replaced by: #'clojure.tools.analyzer.utils/update-vals
WARNING: update-keys already refers to: #'clojure.core/update-keys in namespace: clojure.tools.analyzer.utils, being replaced by: #'clojure.tools.analyzer.utils/update-keys
WARNING: update-vals already refers to: #'clojure.core/update-vals in namespace: clojure.tools.analyzer, being replaced by: #'clojure.tools.analyzer.utils/update-vals
WARNING: update-keys already refers to: #'clojure.core/update-keys in namespace: clojure.tools.analyzer, being replaced by: #'clojure.tools.analyzer.utils/update-keys
WARNING: update-vals already refers to: #'clojure.core/update-vals in namespace: clojure.tools.analyzer.passes, being replaced by: #'clojure.tools.analyzer.utils/update-vals
WARNING: update-vals already refers to: #'clojure.core/update-vals in namespace: clojure.tools.analyzer.passes.uniquify, being replaced by: #'clojure.tools.analyzer.utils/update-vals
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  Async? false  Throw? false
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
[(.....................................................................................................)(..................................................................................................................................................................................................................................................................................................................)(................................................................................................................................................................................)(..........................................................................................................)(........................................)(.................................................................................)(...................................................................................................................................................................................................................................)(...)(..............................................)(.....................................................................................)(........................................)(....................................................................................................................................................................)(...........................................................)(.......)(.....................................................................................................)(..............................................................................................................................................)(........................................................................................................)(................................................)(...................................................................................................................................................)(.............................................................................)(..........................................................................)(..........)(..................................................................................................................)]
[32m231 tests, 2258 assertions, 0 failures.[m

```

Source-retirement and exact-owner oracle output:

```text
PASS: views.clj gone; all 141 owners occur exactly once in their 20 listed destination files.

```

The full analysis projection is in `build/cc-cli-plan.edn` and in the receipt's
details file. It reports edges with witnesses, 540 final namespace edges, forward
references, callers, external requires/imports, promotions with callers, and empty
unmapped/duplicate/cycle/rule/unknown lists. The source map is hash-bound; reference
analysis is performed once per call, while pre/post publication guards check bytes
and inventory without recomputing references.

### What I did not build, and doubts

- No architecture recommender, custom evaluator/macroexpander, second transaction
  system, site-table language, twenty-extraction loop, or multi-source orchestration.
  Those would defeat the narrow compiler contract or exceed its witnessed scope.
- No new routing installation or shared CLI/MCP deployment. Native remains the
  default outside already authorized routes. The skill was not touched, so no
  skill sync was needed.
- No arbitrary dynamic-reference guarantee. The source slice admits .clj
  def/defn/defn- owners; unsupported ownership and namespace-sensitive syntax
  refuse. Static unknown_count=0 is explicitly scoped to captured roots and the
  stated reference authority. Macros, runtime resolution, external callers and
  changing runtime dependencies are not universally proved.
- Failure atomicity is not filesystem reader isolation or power-loss durability.
  Candidate files are visible while the configured commands run. A conflicting
  edit can require recovery; guarded inverse refuses to clobber it. The named
  checks are evidence for their actual coverage, not arbitrary semantic equivalence.
- Generated destination namespace headers are owned output. Body comments/trivia
  and caller namespace metadata/trivia are retained where witnessed; this is not
  a general round-trip identity promise for every reader/namespace grammar.
- No decisive native crossover claim. The 30.33 s includes all four acceptance
  checks once a mapping/profile is ready. Comparing it with the original native
  callers' 331/393 s complete workflow would use different denominators. A fresh
  same-prose-plan, same-model paired replay and independent acceptance still need
  to establish the full decision-to-verified speedup. No battery was run to invent
  that evidence. The compiler feasibility slice is demonstrated; “perfect” and
  months-dogfooded winner are not claims this build earns.
- The initial MCP gate override supplied only -J-Xmx512m; its launcher self-test
  correctly failed because the documented default also contains -J-Xms64m. The
  final gate uses Makefile defaults. CLI development also exposed Babashka ignoring
  JAVA_TOOL_OPTIONS for temporary directories; the temporary fixture was removed,
  and the CLI now adopts TMPDIR before allocating. Retained artifacts are confined
  to the requested build directory.

### Batch 2 diff stat

```text
 docs/high-level-design.md                          |   3 +
 .../helper-extraction/namespace-split-design.md    | 162 +++++++++
 .../helper-extraction/namespace-split-specs.md     |  33 ++
 docs/plans/namespace-split.md                      |   5 +
 docs/tech-tree.md                                  |  10 +
 src/clj_surgeon/core.clj                           |  10 +
 src/clj_surgeon/mcp_change_buffer.clj              |  54 +--
 src/clj_surgeon/mcp_extraction.clj                 |  82 ++++-
 src/clj_surgeon/mcp_helper_extraction.clj          | 314 ++--------------
 src/clj_surgeon/mcp_namespace_split.clj            |  24 ++
 src/clj_surgeon/mcp_tool.clj                       |   2 +
 src/clj_surgeon/namespace_split.clj                | 399 +++++++++++++++++++++
 src/clj_surgeon/namespace_split_io.clj             | 304 ++++++++++++++++
 src/clj_surgeon/synchronous_verification.clj       | 301 ++++++++++++++++
 src/clj_surgeon/verification_process.clj           |  59 +++
 test-fixtures/namespace-split/analysis.edn         |   1 +
 test-fixtures/namespace-split/deps.edn             |   1 +
 test-fixtures/namespace-split/src/app/views.clj    |   7 +
 test-fixtures/namespace-split/test/app/caller.clj  |   3 +
 test/clj_surgeon/cli_dispatch_test.clj             |  38 ++
 test/clj_surgeon/help_test.clj                     |   2 +-
 test/clj_surgeon/lane_manifest.clj                 |   2 +
 test/clj_surgeon/lane_manifest_test.clj            |  12 +-
 test/clj_surgeon/mcp_extraction_test.clj           |   6 +
 test/clj_surgeon/mcp_helper_extraction_test.clj    |   9 +
 test/clj_surgeon/mcp_http_server_test.clj          |   9 +-
 test/clj_surgeon/mcp_intent_contract_test.clj      |   3 +-
 test/clj_surgeon/mcp_namespace_split_test.clj      | 147 ++++++++
 test/clj_surgeon/mcp_operation_registry_test.clj   |   5 +
 test/clj_surgeon/mcp_server_test.clj               |   8 +-
 test/clj_surgeon/namespace_split_test.clj          | 155 ++++++++
 31 files changed, 1798 insertions(+), 372 deletions(-)

```

## Final gate receipts and closeout

| Gate | Outcome |
|---|---|
| Affected namespaces + expanded witnesses | 26 tests, 201 assertions, 0 failures/errors; three existing helper profile/envelope witnesses also pass |
| make mcp-operation-oracle | Exit 0; also executed as the first prerequisite of each make mcp-test |
| clojure -M:clj-surgeon/test-fast | Exit 0; 603 tests, 6,177 assertions; zero isolation violations across 52 namespaces |
| make mcp-test | Exit 0; 768 tests, 9,682 assertions; zero isolation violations across 58 namespaces; all Make prerequisites/self-tests pass |
| bb test/run_all.clj | Exit 0; 873 tests, 7,511 assertions |
| ~/bin/clj-kondo | Exit 0 across all changed Clojure implementation/tests, plus final expanded witnesses; zero errors/warnings |
| Widened LID audit and per-ID debt ledger | Green in ordinary gates; no new debt exception |
| Lane manifest and pins | Green; recomputed counts above |
| git diff --cached --check | Exit 0 before commit |

The temporary-directory self-tests intentionally exercise named refused paths and
print expected exit-97 messages; those are passing negative witnesses. The cclsp
launcher self-tests print a mocked 7890 URL using fake curl/launchctl programs;
no retired MCP endpoint was contacted. No application battery was launched.

Final commits, both authored from the environment as forge-anvil
<forge-anvil@anvil>, both with the requested Gene Kim and Claude Fable trailers:

- b53714fb — Batch 1 repair, 12 files, +169/-13.
- 913020c8 — Batch 2 compiler/entrances, 31 files, +1798/-372.

No push, no main change, no shared CLI install. The worktree is clean. The owned
127.0.0.1:7950 dev nREPL was stopped by exact PID 1604856. The final copied
application remains split for review; its receipt and inverse remain under build/.

Completed 2026-09-07 within the 2 h 30 min budget. The build and fixture acceptance
are complete. A decisive matched native speed/adoption claim remains unestablished;
that uncertainty is deliberately not converted into a routing recommendation.
