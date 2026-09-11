# Astra consult: native parity without an infinite test burden

Verdict: adopt the economic constraint; reject the proposed rule and sort table as written. Keep rewrite-clj. Reduce Surgeon’s policy and duplicated proof machinery, not its ability to distinguish syntax from text.

Reviewed source: fetched origin and checked out detached `origin/MCP/main`, exactly `04648059bd666fb3e40bfdaee7e0e327c273740d`. Read both contracts, both build reports, both fix1 reports and both Opus red-teams in the records observations directory; inspected all requested production/test files. This is a consult, not another executed red-team or ship approval. No tests, servers, ports, code changes or Git commits. “Commit” below means take a position.

## 1. Both sides, then the rule I would adopt

**Gene’s side is right about the incentives.** Every imagined syntax variant must not become a new Surgeon requirement. E4 demonstrates a concrete substring-edit failure; it does not justify reproducing the Clojure reader’s test suite. A refusal on an unrelated README symlink was product damage. The rename fix already removed that refusal, skipped-file duplicate/mutation blockers, and comment-ancestry mutation blockers. The document describes some historical burden as if it were still current.

**The opposing case is also concrete.** Both Opus F1 reviews deleted candidate guards and retained green headline suites; a perturbed splice then committed a nested defn or `eevx`. Rename F2 found preservation defined tautologically and read-back facts copied from the plan. Those fixes are not parser paranoia. They make Surgeon’s own additional claims truthful. Conversely, the reports’ unmutated boundary probes found no wrong-byte commits; do not present the mutants as field incidents.

“Same edit natively” is undefined. `sed`, an in-place Python write, an atomic file replacement and a context-aware patch differ in target selection, link behavior, race behavior and permissions. A careful native patch can reject stale context and avoid URLs. The universal “would have” is unknowable before executing the counterfactual; “might have” admits every imaginary case. Nor does a form count distinguish the right two sites from two wrong ones. E4’s protection comes primarily from the role selector, with count as a caller disagreement check. Stale/anchor/structure guards were not the cause of that particular save.

**Replacement rule:** Within a named supported contract, accept when the declared target/count/snapshot and exact splice/preservation checks pass. Refuse when a declared promise fails, the target is unauthorized, or a measured resource envelope prevents completing proof. Unsupported capability returns an explicit no-write fallback, never a speculative write carrying the normal success receipt. Every *additional semantic refusal* must identify a concrete native-edit failure, its smallest reproducer and why an existing check cannot catch it. Preserve otherwise opaque bytes without inventing new semantics.

That capability escape is essential: a native editor can repair an already malformed file; this structural verb need not acquire that capability. An unavailable proof is not evidence native would fail. Record capability refusals separately and make their adoption cost visible; do not let “unsupported” become an unmeasured permanent refuge.

### Revised sort (one justification per row)

| Input / condition | Disposition and native failure, or its absence |
|---|---|
| Wrong declared form/reference/file count | Keep: incomplete or excessive native selection; never advise changing the number before reviewing the actual sites. |
| Stale digest, target identity or repository membership | Keep: overwriting another edit or omitting newly in-scope files; retain final rechecks and the explicit non-CAS limitation. |
| Missing, multiple or out-of-range anchor | Keep: native search can choose the wrong owner, arity or testing body; absence does not authorize insertion elsewhere. |
| Candidate parses but changes structure/roles/preserved bytes | Keep: a native splice can swallow a sibling, change a token boundary or land inside a form. |
| Source/payload/candidate parse failure | Keep within this contract: malformed payload/candidate can break reading; malformed source has no inevitable native failure, but structural proof is unavailable. |
| Alias collision, capture, wrong library, ambiguous selected binding | Keep, missing from the original table: native alias replacement can retarget `ev/x` or rename a binding for a different library while all four headline checks pass. |
| Runtime namespace mutation | Narrow to evidence relevant to the selected binding where mechanically decidable; rebinding can make static selection wrong, but unrelated/comment/skipped-file occurrences do not establish that failure. |
| Mixed CRLF/LF; BOM | Accept only after byte/span handling works: no inherent failure for a byte-preserving native patch; deleting the checks alone does not repair newline/BOF assumptions. |
| Symlink components | Do not blanket-demote: following can escape scope, replacing can destroy the link, and two paths can identify one target; admit confined links only with explicit identity/publication semantics. |
| Hard-link count >1 | Do not blanket-demote: in-place writes change every alias while atomic replacement separates one name; “native behavior” has two materially different answers. |
| `.cljs` / `.cljc` / `#?` / `#?@` | Extension alone is not a hazard; admit opaque nonselected syntax when the contract remains provable, but branch-dependent bindings/anchors require a branch contract, not merely a reader that accepts `#?`. |
| Request/source/payload/depth/unit/candidate limits | Keep a shared operational envelope: no intrinsic native byte error, but parser exhaustion or indentation amplification prevents proof and can exhaust the service; a large byte bound alone does not bound recursive depth. |
| Comment-only / complete discard-only payload | Current positive `payload.forms` still mismatches zero: writing would violate the caller’s count; zero-form insertion requires a deliberate contract change, not ignoring that assertion. |
| Discards mixed with effective payload forms | Drop blanket ban once counted/preserved correctly: a complete `#_1 2` has one effective form; reject malformed or boundary-swallowing syntax through parse/structure checks. |
| NUL path | Keep one shared invalid-path translation: ordinary OS/native APIs reject it too; no successful “native write” to imitate, and no bespoke per-verb suite needed. |
| Invalid UTF-8 / unpaired surrogate | Keep when conversion would replace bytes silently; a byte-native editor may succeed, but the present String-based planner cannot claim exact preservation after lossy decoding. |
| Tabs in insertion indentation | Capability limitation until a deterministic layout rule exists: no inherent native failure; rename already preserves tabs and must not inherit this restriction. |
| Closed request / duplicate keys / trailing EDN | Keep minimal protocol validation: ignoring a second request or conflicting intent is not native parity; avoid repeating the same grammar matrix per transport and verb. |
| Reader-eval / request data-reader execution | Keep nonexecution as a shared invariant: native text edits do not execute input; parsing must not introduce execution. |
| Long legal path makes receipt exceed 4 KiB | Remove policy-induced refusal after compact projection exists: native edit is sound; move evidence to detail while retaining truthful state and recovery identity. |
| Error sentence wording | Remove exact prose checks; retain state, stable type, actual evidence and actionable remedy because bad retry guidance can overwrite or reapply a committed edit. |

**Under-refusal beyond the table:** `(ns x (:require [lib :as events]))` followed by `(def wire-id 'events/x)` is syntactically renamed to `'ev/x`; a protocol identifier can change while count/stale/structure/anchor all pass. The current contract explicitly chooses that syntactic behavior. It must not be marketed as “never wrong bytes” or general semantic equivalence. Likewise macro-hidden alias mutation is expressly outside the guarantee. Do not respond with an exhaustive macro/quote blacklist: bound the claim and make the literal-data policy visible. The four promises also do not alone prove write-back, rollback or receipt truth; those are separate transaction promises already sold by these verbs.

## 2. Count the burden, then cut it honestly

A reader-only census at this checkout counted actual `deftest` and `is` forms, not grep hits in strings. It did not execute tests. Supporting/oracle namespaces are excluded from the namespace column.

| Current suite | Test namespaces | deftests | Static `is` sites | Expanded assertions in retained successful runs |
|---|---:|---:|---:|---:|
| `insert_forms*_test.clj` | 22 | 24 | 109 | 548 |
| `rename_alias*_test.clj` | 7 | 25 | 131 | 558, reconstructed below |
| Shared `receipt_booleans_test.clj` | 1 | 2 | 9 | 45 inferred from the current loops/registry, not separately rerun |
| Total | 30 | 51 | 249 | 1,151 on that accounting |

Insertion’s shared `insert_forms_support.clj` adds six static `is` sites; its accepted/refused helpers contribute three assertions per fixture. Rename’s seven analogous helper sites are already inside its 131. This explains much of the expanded total; it is not 1,151 independent hazards.

Count provenance: insertion fix1 focused log = 24/548; rename build focused = 11/507 (10/349 core + 1/158 parity), then fix1 adds candidate 5/11, receipt 3/15, positions 1/9, performance 1/2, scope 4/14: 25/558. The requested test files have no diff between final rename fix1 `273a8bbf` and this trunk. The combined 100/1,945 log also includes lane-manifest and MCP-intent-contract tests; do not attribute those all to these verbs. Logs live in `/var/tmp/forge/insert-fx/` and `/var/tmp/forge/rename-fx/`; report names are listed below.

**The document’s “removes most” claim is unsupported.** Literal application of its demotion column removes roughly 80–120 expanded assertions across both verbs, depending on treatment of limits and protocol checks: about 7–10%, not most. Many are rows inside a single deftest. Corrected demotions mostly need replacement acceptance witnesses. The immediately clear deletion is `insert-forms-refusal-remedies`: one deftest, 38 assertions matching English. Deleting its checks does not eliminate the need for useful errors.

### Smallest set I would sign for these existing contracts

Use table rows for distinct mechanisms, never Cartesian products of syntax, transport, newline and filesystem shape. Proposed organization: three namespaces per verb (pure contract, transaction/receipt, entrance), plus shared envelope/boolean namespaces. Ten deftests per verb; four shared. These are consolidation targets, not a claim that renaming tests reduces work.

| Insertion witness group (10) | Retained proof / existing witnesses folded into it |
|---|---|
| 1. Exact root anchor | `insert-forms-after-prefix-named-defn`, `insert-forms-comment-string-anchor-decoys`, `insert-forms-anchor-cardinality`, `insert-forms-candidate-envelope`: one real/decoy fixture, zero/one/two matches and useful candidate identity. |
| 2. Body anchor | `insert-forms-deftest-nested-testing`, `insert-forms-body-boundaries`, `insert-forms-defn-headers-and-arities`: direct labels, empty/first/last/N, one header-rich single arity and one disambiguated multi-arity. |
| 3. Count and order | Keep `insert-forms-payload-count-and-order`: zero/one/many, mismatch, ordered quoted/tagged payload; preserve the zero-count contract until changed. |
| 4. Snapshot guard | Fold `insert-forms-stale-hash-refuses` and `insert-forms-portable-read-receipt`: changed comment byte, complete path-bound guard, one real inspect-to-write handoff. |
| 5. Layout/literal preservation | Fold `insert-forms-trivia-and-indentation` and `insert-forms-existing-separators`: retain tail/standalone comments, empty closer, inline sibling, blank gap, CRLF and multiline-literal cases that exercise different decisions. |
| 6. Parse stages | Fold `insert-forms-unbalanced-payload-refuses` into `insert-forms-parse-errors-refuse`: source, payload including malformed tail, injected candidate; exact type and no disk write. |
| 7. Structure guard | Keep `insert-forms-candidate-structure-refuses`: the existing parse-valid wrong-offset mutant must go red if the guard disappears. |
| 8. Independent preservation | Keep `insert-forms-other-top-level-hashes`: duplicate unnamed roots, discard/comment survival, wrong-body insertion and forged digest; assert raw A/B interval identity as well. |
| 9. Publication and evidence | Fold `insert-forms-atomic-multiform-and-race`, `insert-forms-planned-receipt-recovery`, `insert-forms-receipt-accounting`, `insert-forms-terminal-response-eligibility`; stage, final stale, read-back, external-write/recovery outcomes and disk-derived facts. |
| 10. Entrance | Keep `insert-forms-cli-mcp-parity`, reduced to each dispatch spelling once and representative success/refusal/I/O projection; full reader grammar belongs in one shared test. |

| Rename witness group (10) | Retained proof / existing witnesses folded into it |
|---|---|
| 1. E4 | Keep `rename-alias-e4-verbatim`: 31→2 refusal, corrected exact diff/hash and route strings unchanged; one frozen real-program witness. |
| 2. Role selection | Fold `rename-alias-prefix-trap`, `rename-alias-reader-roles`, `rename-alias-string-decoy`, `rename-alias-discard-decoy`: one compact role table retaining quote, auto/literal keyword/map, tag/payload, metadata, comment and discard distinctions. |
| 3. Binding anchor | Keep `rename-alias-binding-matrix`: selected lib, absent/duplicate, capture/collision, unused alias, :as-alias; reduce malformed-token synonyms to equivalence classes. |
| 4. Scope | Fold four `rename-alias-scope-test` deftests and scope selection/count rows from `rename-alias-scope-guards-counts`; include unrelated/skipped furniture and complete inspected inventory. |
| 5. Stale | Remaining `rename-alias-scope-guards-counts`: digest and membership drift plus path-bound guard; no automatic refresh/retry. |
| 6. Parse | Source parse row from `rename-alias-discard-decoy`/`rename-alias-source-boundaries` plus `candidate-parse-refuses`; execute one disk no-write boundary per parse stage. |
| 7. Candidate integrity | Fold `candidate-role-recount-refuses`, `candidate-form-preservation-refuses`, `candidate-inverse-identity-refuses`; retain each injected fault while its independent guard/claim remains. |
| 8. Preservation/address | Fold `rename-alias-receipt-oracle` and `column-one-reference-addresses`; raw intervals/inverse, forged evidence, UTF-8 offsets, column-one line/preorder. |
| 9. Publication/evidence | Fold `rename-alias-transaction-faults`, `replacement-read-back-refuses` and three `rename-alias-receipt-test` deftests; retain second-file failure, foreign bytes, disk-hash inequality and missing artifact section. |
| 10. Entrance | Reduce `rename-alias-cli-mcp-parity` as for insertion; share protocol cases, not the two verbs’ dispatch wiring. |

Shared four: retain `every-receipt-boolean-has-a-driven-false-witness` and `unregistered-verbs-and-fields-fail-by-name`; add/consolidate one bounded-input/path/encoding test and one exactly-one-EDN-request test from existing rows. Keep confined-link and invalid-encoding scenarios there. Remove per-verb copies of those scenarios, not the safety invariant. Keep `four-thousand-line-planning-under-five-seconds` as a separately budgeted performance measurement (it is already in the battery lane), with a named machine/budget; it is not proof of a four-promise semantic property. Its retained benchmark is outside the proposed semantic-suite totals.

Delete the English-regex `insert-forms-refusal-remedies` outright; fold its actionable-state check into guard/anchor cases. Fold the named suites above and delete their superseded standalone definitions. Convert BOM/mixed-newline/discard/long-path refusal rows to a few exact-byte acceptance cases only after admission works. Retain branch capability refusals until their contract changes. Stop requiring the same closed-schema/trailing-EDN matrix for both CLI spellings and both verbs.

**Boolean seams stay, but execute each scenario once per verb and reuse its observed receipt across all registered paths.** Seven insertion keys and ten rename keys need not mean that many disk transactions. Rename has eleven checked paths because preservation appears both inline and per-file. Keep literal-false evidence; absence is not false. Keep disk hash assertions from the receipt tests: hashes are not booleans. Remove the two docstring-slogan assertions. The current discovery starts from committed receipts, so failure-only fields such as `receipt_persistence_failed` are not covered by its “every” claim; narrow that claim now, and cover failure variants through the existing transaction matrix rather than inventing more semantic refusals.

**Budget/ratchet:** target ≤10 deftests, ≤250 expanded assertions per verb; shared envelope/boolean work ≤80 assertions, ≤8 total test namespaces. That is a target of ≤580 versus roughly 1,151 now, reached chiefly by removing repetition; it is an estimate, not a measured post-refactor result. Also register fixture-row counts and process launches so folding cannot conceal growth. Do not remove a unique required witness merely to hit a number.

At review rung 0, require registry fields `promise`, `native_failure` (or explicit `none`), `native_method`, `minimal_reproducer`, `class` (semantic/capability/resource/protocol), `existing_check`, `owner`, `witness`, and `retirement_condition`. A semantic refusal with `native_failure=none` is rejected. A capability/resource exception needs a bounded rationale and fallback, not a fictitious native hazard. Review one representative per decision/guard, then reintroduce the witnessed defect once. No further round for a new spelling of the same mechanism. New rows require replacing a redundant row or an explicit budget exception justified by a distinct failure/changed promise; shared parser defects get one adapter regression and an upstream issue, never a new per-verb matrix.

## 3. What belongs upstream

Checked the installed rewrite-clj **1.2.50** source jar as well as upstream documentation. No maintainer was consulted; acceptance assessments below are engineering judgments, not promises.

API facts: `parser/parse-string-all` returns a forms node; `node/children`, `node/tag`, `node/string` expose syntax. Parsed-node metadata has row/col/end-row/end-col; end is exclusive. Zippers with `:track-position? true` expose `z/position`/`z/position-span`; metadata describes the original parse, whereas tracking follows edits. These are not UTF-8 byte offsets. [Upstream guide](https://github.com/clj-commons/rewrite-clj/blob/main/doc/01-user-guide.adoc).

Ordinary zipper movement skips whitespace/comments; starred movement includes them. `insert-left*`/`insert-right*` avoid automatic whitespace treatment; `replace*` avoids coercion. `z/splice` raises children out of their parent, not an arbitrary byte-interval replacement. [Actual zipper API](https://github.com/clj-commons/rewrite-clj/blob/main/src/rewrite_clj/zip.cljc).

Important adapter detail from the installed jar: a one-line StringNode has tag `:token`; a physically multiline string has tag `:multi-line`, not `:string`. A string containing escaped `\n` on one source line is still `:token`. Node type is `:string`; tag alone is not a symbol classifier. Multiline literal spelling must be taken from the original source slice, not reconstructed via sexpr/printing. The existing insertion `spellings` handles `:multi-line`; the lexical scan is also tracking literal ranges.

| Mechanism | Existing capability; upstream disposition; Surgeon retains |
|---|---|
| Per-form byte hash + span | Positions/raw rendering exist, but not the whole UTF-8 snapshot/hash inventory contract. Propose precise source-span support and the synthetic `:map-qualifier` coordinate gap as a small rewrite-clj issue; plausibly acceptable. Hash policy, form ordinals, snapshot identity and receipts stay Surgeon. Do not upstream SHA/transaction schema merely because it uses nodes. |
| Symbol-role classifier | Node/keyword/namespaced-map APIs and `:auto-resolve` supply syntax building blocks; the complete ns-binding/capture/quote/discard selection policy is not supplied. Generic lexical predicates or missing qualifier spans fit rewrite-clj; resolved-reference facts fit clj-kondo. Surgeon keeps which roles this edit owns and whether quoted data changes. Neither library should inherit this verb’s namespace-mutation blacklist. |
| Exact-byte splice with gap preservation | Starred zipper edits already avoid whitespace adjustment; source-preserving tree edits exist. This exact A-prefix + inserted bytes + A-suffix contract, plus header/comment ownership, is not `z/splice`. A generic source-edit utility might interest rewrite-clj, but acceptance is uncertain and ordinary byte splicing needs little library machinery. Keep authorized intervals, gap/layout policy and commit/rollback in Surgeon; report reproducible lossless-parser defects upstream. |
| Bounded exactly-one-value EDN | `clojure.edn/read` with a unique EOF sentinel and a second read already enforces one value; it is a tiny wrapper, not a new parser. Map-only/closed keys/tag policy and transport byte caps stay shared Surgeon protocol code. General parser depth/node limits are a plausible rewrite-clj or reader-library contribution; rewrite-edn is an editing layer over rewrite-clj, not the natural request-validation owner. |
| Structure recount after splice | Reparse + child traversal already exist; “effective form”, selected body, expected role set and authorized changed ordinals are operation policy. Keep the small comparison in Surgeon; upstream only a demonstrated parse/span defect. Clj-kondo lint success is not a substitute for preservation/placement comparison. |

The bounded EDN wrapper is grounded in the [Clojure EDN API](https://clojure.github.io/clojure/clojure.edn-api.html); rewrite-edn’s own [project description](https://github.com/borkdude/rewrite-edn) identifies its editing role. Do not claim any existing library already supplies the five complete Surgeon mechanisms.

**What actually smells like another parser:** `insert_forms_plan/lexical!` plus `shape-limit!` separately track strings, escapes, comments, reader prefixes, delimiter depth and expression completion before rewrite-clj runs. That is a partial lexer/reader model, even though rewrite-clj remains the main parser. Move generic recursion/work limits into the parser when possible; remove duplicated grammatical preflight only when an equivalent operational bound exists. Don’t build more prefix grammar in Surgeon to defend the claim “we are not building a parser.”

## 4. Can we drop the CST?

**Best argument for it:** stale is pure byte hashing/identity, and preservation is pure interval algebra. A reader can count complete values; clj-kondo can locate definitions and references. With caller-supplied exact intervals and a weaker syntax-only postcondition, a native-patch gate could be small. A materialized full CST is not mathematically necessary: a source-aware event reader could also supply spans and nesting.

**Why not for these contracts:** an ordinary reader erases comments/discards, collapses literal spelling, resolves auto-keywords, expands reader sugar and lacks complete raw spans for all values. `clojure.edn` is not a Clojure-source reader. Exact root/body placement, direct testing labels, metadata ownership, zero-count discards and literal-safe indentation need the missing structure. Reconstructing it around a reader is another lexer/parser adapter.

Clj-kondo analysis does more than var usages: it offers `:symbols` for quoted data, keyword alias/auto-resolution facts and definition/name coordinates. Still, that analysis is not a complete CST/gap inventory or the contract’s exact one-site-per-`#::alias{}` census; an empty auto-map especially exposes the distinction from keyword-use counting. Analysis configuration/hooks and CLJC language projections also require a completeness policy. These conclusions concern its documented analysis API, not a claim that its internal parser cannot do the job. [Clj-kondo analysis API](https://github.com/clj-kondo/clj-kondo/blob/master/analysis/README.md).

**Commit:** keep rewrite-clj for these verbs, use its parser/node layer directly where sufficient, and retain original bytes as the write authority. Investigate eliminating the duplicate parse in rename `annotate` → `addresses` before replacing the library. Use clj-kondo as optional independent semantic evidence where appropriate; do not make it a new mandatory planning dependency. We need a parser for the current promises, not a new general-purpose parser maintained by Surgeon. A CST-free interval gate is a different, narrower contract and must be evaluated as such.

## 5. Three things I am least sure of

1. **Quoted-data policy.** The present syntactic rename is explicit but can surprise callers who mean runtime alias equivalence. Caller evidence should decide visibility/defaults; a speculative semantic classifier would recreate the infinite burden.
2. **Upstream acceptance and span semantics.** Source offsets/resource limits look reusable, but maintainer appetite, cross-platform column conventions, newline normalization and synthetic-node positions need an upstream discussion and a tiny reproducer; no upstream acceptance was obtained.
3. **The achievable cut and resource ceiling.** Static counts are exact; expanded totals use retained logs/inference, and the ≤580 proposal has not been implemented or timed. Fixture/process reduction must preserve the demonstrated guard mutants and disk-derived booleans; workload measurements, not round numbers, should set parser ceilings.

Decision: revise the refusal rule before applying it. Consolidate witnesses and receipts, admit benign byte shapes only with their existing promises intact, and send small parser-level mechanisms upstream. Do not ship the document’s blanket demotions.

Local evidence: records observations `2026-09-11-refusals-native-parity-rule.md`; `2026-09-10-{insert-forms,rename-alias}-contract-astra.md`; both `*-build-astra.md`; `2026-09-10-insert-forms-fix1-astra.md`; `2026-09-11-rename-alias-fix1-astra.md`; both `2026-09-10-*-opus-redteam.md`. Code entry points: insertion `lexical!`, `shape-limit!`, `tree`, `resolve-anchor`, `facts`, `target!`, `receipt-projector`; rename `references`, `binding!`, `collision!`, `candidate!`, `receipt-projector`; shared `receipt_booleans_test.clj` and its registry.
