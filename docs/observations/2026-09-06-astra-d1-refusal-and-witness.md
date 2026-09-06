# Astra — completed D1 utility review, frozen result retained

Only completed D1 inspected; no D2, actor feedback, model/probe/test execution, source fixes or rejudging. Receipt: runner-b/cohort/D1/result.json.
Bound rollout: rollout-2026-09-06T10-25-29-01a07640-6e21-7b50-a3bf-c4dcc16c6d65.jsonl;
SHA fca781bdceca5fdac25fa75a36f2afb312d8de2c30144952ac08d02029fbc947 matched.

**Frozen result remains correct=false.** Actor wall274.054s;
copy-through-actor274.058s; complete wall279.015s. General gate58tests/190assertions
passes; independent witness19tests/495assertions yields2failures,0errors.
Separate scope/outcome audit passes:59sites/20files, aliases/local helper correct,
no outside-owner byte changes. These authorities disagree on the textual rule.

## Exact failure: require insertion layout, not mass deletion

Both witness failures are e4-qualifier-only-textual-delta at484/487, solely
pool_relaxed.clj. The added require moves the old require block's closing `))`
from the clojure.string line to the new maven.db libspec line. The two migrated
call qualifiers are expected; no broader behavioral defect is demonstrated.
Witness delta at452–469 treats original lines as an exact subsequence; changing
that first closing line makes it report the entire remaining seed tail as
“removed.” This is an artifact of that walker, not evidence the tail was deleted.
Its rule at522–530 admits at most one complete added libspec line and no removed
seed line. It therefore rejects an ordinary end-of-require insertion layout.

preregistration-DN.md:3/8 fixes qualifier-only migration, outside-file identity
and this witness. It does not separately prescribe insertion before the last
libspec or forbid moving the require's closing delimiter. The frozen witness
is narrower than the high-level require-addition authority. Preserve the failure;
call it a textual-acceptance/layout mismatch, not demonstrated semantic failure,
and do not revise/rejudge acceptance after seeing D1.

## Actual MCP use and native repair sequence

19outerexec actions;9registered MCP attempts,7refused,2successful commits.
All are completed-call metadata, not tool mentions. Seven apply_clojure_changes calls and two alias_migration calls; no successful alias migration.
- Lines49/51: native patch adds helper/local calls; first MCP refuses object file
  records because compact relation requires [file,rows]. Lines55/56 correct that
  but refuse mismatched require/migration file vectors.
- Lines60/61 refuse already-required target. Line69 reads the skill. Lines76/77
  remove require_change and refuse because it is mandatory. Lines83/84 try alias
  migration with verify=none (unknown profile);88/90 retry and refuse an indirect
  macro-mediated reference (typed alias-migration-indirect-reference; reason
  unsupported-binding-scope). Lines95/96 refuse removing the target alias.
- Line106 uses native Python writes to remove18existing require bindings, keeping
  their original headers in memory;109 is the first successful MCP migration.
  Lines114/116 commit the second alias group. Line119 restores original headers
  for those18files. The initially missing require is not among restored headers.
This is hybrid workaround, not a clean deterministic batch or total native fallback.

## Where wall went, as observed

First MCP begins10:26:45, roughly76s after actor start; last succeeds10:28:33.
Nine completed MCP durations sum3.561s, including3.073s in the first success;
they do not explain274s actor wall. Catalog reading, ordinary discovery/snapshots,
request reshaping, refusal interpretation and native header repair span many
inter-call decisions. Do not relabel every gap model reasoning or predict savings.
Line127/130 own byte audit reports no unexpected changes;136/139 custom forwarding
proof fails its empty-args sequence expectation, then143/146 passes after vec.
External proof still runs afterward and catches the narrower layout rule. Cumulative input1,152,491 includes1,094,912cached; this is repeated context, not
unique prompt volume or free cached work. No overall paired conclusion is drawn.
