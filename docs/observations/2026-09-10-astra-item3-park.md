Generated 2026-09-10T02:16:44.280400+00:00

# Astra item 3 — PARK 10b77576

Status: **FINAL — PARK**. Design probes, original replay and the verification ledger are complete.

## Decision

**Park `10b77576307c6c1a7922baa82cc08a32d024ba2e`. Do not land it, including as the proposed experimental preservation certifier. Do not consume its output at any review entrance.** Retain the branch and its replay corpus as research evidence. No production source, test, routing, installation, or branch history has been changed by this review; no commit or push was made.

A useful experimental **source-comparison brief** remains possible, under the contract below. This is a different claim from certifying preserved bodies or completing a review obligation. The current program does not implement that boundary consistently: its headline and machine status promise more than its footer permits. Adding EXPERIMENTAL above the existing certificate would not repair that contradiction. I am not spending another fence round adding a denylist entry or a table-cell assertion to keep the old promise alive.

The decisive result is not Sol's eighteenth possible edge. Six valid, executed namespace-move specimens receive `clear:true`, exit 0 and a positive preserved count while their observable value or generated interface changes. One uses the resolver-backed requalification tier, with correct core Var resolution. The implication **same projected body ⇒ preserved behavior / no remaining decision** is false, even when the resolver makes no mistake.

## Subject and custody

- Work began at 2026-09-10 02:05:30 UTC, read from the clock. Two-hour deadline: 04:05:30 UTC; interim due 03:05:30 UTC if still active.
- Owned checkout: `/home/forge/src/clj-surgeon-astra-consult2`, initially clean and already at the requested commit on `astra/proof-burden`, tracking `origin/fable/proof-burden`.
- Executed `git fetch origin fable/proof-burden`, then `git switch astra/proof-burden`. The actual `fable/proof-burden` branch is occupied in the Opus builder's worktree; I did not check it out twice or touch that worktree.
- Requested tip and round-17 sealed merge candidate `3beee531f8301633ca9d67b8a2238cf3041cf959` have the **same Git tree**, `34b306363615ad2d0bf5e22527d4c5df96321107`. My probes concern the requested tip, not a newer builder repair.
- Toolchain observed through the paved entrance: clj-kondo v2026.08.04; Babashka v1.13.219.
- No subagents, model-review calls, MCP, nREPL, or server calls. The literal runtime probes used fresh Clojure CLI processes, not a service. Temp/JVM temp paths were under `/var/tmp/forge/`, with owned evidence and JVM temp under `/var/tmp/forge/astra-item3/`. The unmodified replay also writes its fixed symlink-test sentinel `/var/tmp/forge/item3/sol-proof-external.clj`; it is not deleted during cleanup. Kondo was invoked only through `~/bin/clj-kondo`.
- The builder's observation and Sol rounds 1–16 are in the records checkout, not this branch: `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-item3-preservation-brief.md` and its sibling verdicts. Read-only. The branch adds four `bin/` files, 4,322 lines, relative to its stated trunk base.
- Evidence and runnable probe scripts: `/var/tmp/forge/astra-item3/`. This file is the sole requested report.

## 1. Strongest-rung red team: what is and is not sound

Clojure reads source in a context and compiles/evaluates the resulting forms in a context. Namespace relocation changes that context. Kondo describes a static approximation to parts of it. Correct analysis of which Var a token names does not establish that invoking that Var, reading that keyword, expanding that macro or loading that initializer has the same result in the two programs.

The primary documentation states the relevant boundaries: [clj-kondo describes its static analysis and inability to execute arbitrary macros](https://github.com/clj-kondo/clj-kondo); [the Clojure reader resolves double-colon keywords and syntax-quoted symbols using context](https://clojure.org/reference/reader); [defprotocol generates a namespace-qualified JVM interface](https://clojure.org/reference/protocols). These are semantic reasons, not conjectured parser defects.

### Executed counterexamples

Each specimen is a new two-commit Git repository. Both versions are valid Clojure. The unmodified requested tool compares those committed trees; only afterward does a fresh JVM load the literal base and candidate source under their distinct namespaces and print the observations. All six runtime probes exited 0. No producer receipt was supplied.

| specimen | source relation | tool result | executed observation |
|---|---|---|---|
| `reader-keyword` | `(defn f [] ::id)` moved from `probe.a` to `probe.b`, byte identical | clear, exit 0, 1 byte-identical owner preserved | `:probe.a/id` versus `:probe.b/id` |
| `macro-context` | `(defn f [] (m/where))`, byte identical; unchanged macro returns the expansion namespace | clear, exit 0, 1 byte-identical owner preserved | `"probe.a"` versus `"probe.b"` |
| `load-context` | `(def origin (str (ns-name *ns*)))`, byte identical | clear, exit 0, 1 byte-identical owner preserved | `"probe.a"` versus `"probe.b"` |
| `whitespace-string` | function's multiline string loses two trailing spaces before a newline | clear, exit 0, 1 whitespace-tier owner preserved | the returned strings differ by those two spaces |
| `protocol-identity` | `(defprotocol P (f [x]))`, byte identical | clear, exit 0, 2 byte-identical owner rows preserved | interface `probe.a.P` versus `probe.b.P` |
| `resolved-load-v2` | base `(def origin (str *ns*))`; moved candidate qualifies `str` to `clojure.core/str` | clear, exit 0, **1 requalification-tier** owner preserved | `"probe.a"` versus `"probe.b"` |

The protocol probe establishes changed generated interface identity; it is not represented as an executed dispatch-regression test. Interface/package and existing implementer compatibility are obligations that this output leaves undisclosed by its CLEAR headline. Likewise, a namespace-sensitive change could be intentional: the point is that the checker cannot conclude **no decision remains** without that authorization.

The controls are retained too. `resolved-load-context`, the first fully-qualified initializer probe using `ns-name`, was **refused**, with one `unmodelled-macro-context` row and exit 3, because `ns-name` is outside the accepted function set. This is not counted as a checker failure. Reducing that probe to the accepted `str` example above reaches the same semantic counterexample through the admitted requalification path. The experiment did not stop at an apparent byte-shortcut defect.

Reproduction and exact commits/results:

- `design-probes.py`, `resolved-probe.py`, `resolved-v2-probe.py` under the evidence root.
- `probes/results.json`, `probes/resolved-results.json`, `probes/resolved-v2-results.json`.
- Each `probes/<name>/` contains its committed two-tree specimen, generated `brief.md`, `brief.json`, and `runtime.txt`.

### Why the current proof does not compose

`bin/preservation-brief:1064` (`body-tier`) returns byte equality and `ws-normal` equality before the resolver/body checks at lines 1071–1072. Literal equality therefore bypasses even the tool's own macro/head boundary. `ws-normal` at line 465 trims line endings and blank lines without respecting strings. Section D acknowledges the string limitation, but section A still adds that tier to “mechanically preserved.”

The later `canon-stream` at line 1049 rewrites analyzer-labelled Var tokens to inferred base owner homes. This can establish equality of the resulting data, not equality of runtime effects. The `str/*ns*` witness needs no resolver error. Correct binding identity is insufficient for context independence.

`clear?` at line 1893 is just `(empty? hard-stops)`. The output at line 1912 promotes that bounded absence of detected signals to “Every changed body was mechanically preserved and no decision was left open.” At line 1959 byte identity becomes “nothing in the body to review.” These are the claims being rejected. Section D's statement that behavior was not checked does not cancel them, and JSON has no mandatory semantic-review status that could prevent a consumer making the same inference.

The owner mapping is inferred by same-home, bytes and unique name/family, not supplied as authorized intent. A matched new Var is not the old Var object, its metadata, or its generated class. No exactly-once count can discharge those obligations.

“Independent” needs two qualifiers. A/B are independently recomputed from Git rather than copied from the producer receipt, and this program imports no `clj-surgeon.*` implementation. **They are not independent semantic oracles.** The split compiler itself uses clj-kondo (`src/clj_surgeon/namespace_split.clj:3`, reference authority at line 696). Shared analyzer blind spots can survive two independently executed derivations. Two-way reconciliation also cannot establish completeness when both inventories omit the same semantic object.

Reader-conditionals are similarly bounded. The current implementation declares `:clj` and `:cljs`, and refuses files with other feature keys; that is useful scope accounting. It does not establish execution on either platform, compiler/configuration equivalence, or completeness for another reader feature set. No live reader-conditional runtime claim was tested in my new JVM probes; the existing `.cljc` replay rows exercise that static boundary.

## 2. The contract a revival must implement

This is the **normative proposed contract**, not a description of a repaired implementation. Under this kondo-based design the tool may certify an exact relation over captured evidence. It may not certify semantic preservation of a general Clojure namespace move. A stronger future claim requires a separately specified, sound restricted semantic model or appropriately bounded execution evidence; changing the linter or expanding the allowlist is not sufficient.

> Given two immutable Git tree identities, explicit path/extension scope, and a recorded analyzer/configuration identity, produce an experimental comparison of captured source spans and analyzer records. Report the exact comparison relation, its matched source locations, and every known unsupported or refused input. A match means only that this specified relation holds for those captured inputs. It does not mean that the programs behave alike, that the pairing was authorized, that all references were found, that review is complete, or that any repository gate passed.

Required observable surface:

1. **Subject and coverage.** Record both resolved commit/tree IDs; included roots and file/feature sets; skipped/refused files; analyzer version/configuration/hook/cache identity; tool version. Negative findings quantify only over that inventory. “No recorded stale site” is permitted; “no stale references in the program” is not. Parsed changed files plus textually selected mentions must not be described as every possible caller.
2. **Three distinct evidence relations.** `source-bytes-equal` means exactly equal captured source spans. `analyzed-reference-projection-equal` means equal explicitly defined token projections using recorded analyzer classifications and the declared pairing map. Neither proves reader-form or behavior equality. An actual whitespace-only tier must preserve literal, reader-discard, metadata and comment content; this candidate's lossy `ws-normal` may only be labelled `lossy-text-normalization-equal`, and must confer no preservation credit. A match by inferred name must expose that inference.
3. **No universal certificate.** Output always states `status: experimental`, `semantic_preservation: not-established`, `semantic_review_required: true`, `review_entrance_eligible: false`. These semantics must be present in machine output as well as before the human headline. No aggregate called `preserved_bodies`, unqualified `certified`, “nothing to review,” or “no decision left open.” Existing legacy field spellings cannot silently retain their old meaning in a new version.
4. **No acceptance boolean.** Completion of analysis is separate from whether observations were found. A successful process exit means the comparison completed. A separate `detected_obligations` list is explicitly non-exhaustive. Missing analysis, unsupported platform, inventory disagreement or capped input stays named and unclassified; it cannot become a successful absence claim. Tables may locate review work; they are not an exhaustive review checklist.
5. **Context obligations remain even for byte-identical source.** Reader namespace/aliases and auto-resolved data; macro expansion, `&form`/`&env`, compile-time state and source metadata; actual Var identity/metadata/privacy and dynamic resolution; protocol/record/type identity and implementers; dependency/classpath/runtime versions; namespace/file/form load order, `def`/`defonce` initializers, registrations and side effects; platform conditionals; resources and callers outside scope. These cannot be marked discharged by kondo token equality.
6. **Operation limits.** Read only the declared Git objects; do not read a working copy as candidate evidence, follow source symlinks, load target namespaces or run target tests. Generate analyzer scratch/output under the declared temp root. Report failures explicitly. Section C may compare named producer assertions but confers no custody, behavioral, authorization, or test authority. Analyzer results alone are not a clean-lint result.
7. **Consumption admission.** Until an authorized, independently planted blind falsifier passes on this exact honest output and scope, no review entrance may reduce assigned obligations, suppress diffs, reuse checks or issue GO from it. A passing study could admit it as a navigation aid for the measured task class; it still could not establish the general semantic theorem refuted above.

This contract preserves the valuable option: give a reviewer a map of matching text and unresolved context. It withdraws the unsafe shortcut: tell the reviewer which bodies no longer need semantic attention.

## 3. Cost and revised forecast

**I withdraw the 90–240 s landing-saving forecast for the tool as it stands.** My current planning point is approximately **30 seconds slower** per move-heavy review, with a deliberately broad range from **120 seconds slower to 60 seconds faster**. This is a forecast, not a measured review effect. No blind review has been run here. A redesigned compact navigation brief might earn a different forecast; its future savings cannot be credited to this candidate.

Fresh complete Cell C analysis (one observation): 37.034 s internal checker wall, 37.089 s external invocation wall, of which 5.930 s is the two resolver runs. It reports 713/720 owner rows, 141 moved, 99 matched in the three “preserved” tiers, and 32/64 in-place body changes matched. Those add to **131/205**, not the builder report's stale 135/205. Of the 99 moved matches, **83 are byte-identical and only 16 use reference projection**. All still need context review. Ordinary Git move/word-diff facilities also help the control reviewer, so these 83 matches are not automatically 83 pieces of newly removed work.

The supplied brief says 59 frozen heads. This exact replay emits **57**, as does the current records document's displayed Cell C output; I retain that discrepancy rather than treating a two-head difference as a win. It has 34 moved and 30 in-place bodies in the unmodelled tier, 8 moved and 2 in-place changed bodies, and 7 walker disagreements. The fresh default Markdown is **94,931 bytes / 1,597 lines**. Row and inline-diff caps do not make it a short brief.

A transparent planning calculation, not a timing result: suppose the map removes 60–120 s of locating/matching work. Charge 37 s generation and 60–120 s additional brief orientation/reading. Net saving is then **−97 to +23 s**, before any duplicated context review. The broader range above allows for skimming, a particularly favorable change, and slower readers. The earlier 0–30 s generation budget is already exceeded, and the preregistration's ≈20 s assumption is stale. No cache benefit was measured. A future cache must include configuration/tool inputs and charge population/validation at the prospective release boundary.

Even a genuine review saving becomes a landing saving only while review is on the critical path: compare `max(review, other lanes)` before and after. The historical 573 s review does not establish today's native Cell C baseline or today's landing schedule. Native must receive the same generic comparison aid if it qualifies; no Surgeon-specific advantage has been established.

## 4. Minimum blind falsifier and disposition of §9

Because the branch is parked, I have **not rewritten the builder's historical report or started a cohort**. Its §9 is not an executable admission plan for this candidate. In particular, its semantic kill switch has fired; its time forecast and overhead are stale; six protected classes miss the context failures above; its non-substitution requirement treats legitimate context inspection as failure; and 21 related specimens from five hosts are not automatically 21 independent time observations. A separately named native-treatment arm on the identical input is not another treatment mechanism.

For a revival, replace §9 before any run with the following **two-stage falsifier**. It tests the narrower comparison aid, never rehabilitates the semantic certificate.

### First, a small rejection pilot

- Freeze the honest experimental output contract above, tool/tree/configuration hashes, command, prompts, scoring rubric and assignment before disclosure. An independent person plants and seals the defects and validates each baseline/candidate behavior. The tool author cannot supply the answer key as independent truth.
- Run **six identical native-control reviews first**, in independent fresh sessions, to estimate the control noise floor and qualify timing/log capture. This is the apparatus/variance stage, not six extra favorable treatment pairs.
- Use **16 specimens**: four clean real change hosts (Cell C, Cell B, an explicitly labelled constructed alias migration, and one small behavioral null), plus twelve defective variants assigned to applicable mechanical hosts. One variant each covers wrong binding, local-shadow substitution, unauthorized promotion, lost comment, omitted owner, load order, macro context, reader namespace/keyword, protocol/class/dispatch compatibility, an excluded reader platform, string content hidden by normalization, and dynamic/resource references outside the analyzed surface. Freeze actual host assignments and independent executable answer keys before any review.
- Two arms on every specimen: **C**, complete diff/trees/task with ordinary Git facilities; **T**, exactly C plus the brief. Native- and Surgeon-produced changes are represented in both arms; source producer is blinded. There is no separate N arm with the same treatment. The generative comparison has no receipt dependency.
- Each specimen gets one C and one T review in different fresh sessions of one fixed, attested reviewer model/runtime/prompt configuration. Randomize the 32 cell releases under a frozen seed; no session receives another specimen or prior verdict. Blind answer keys, mutation labels, producer and other verdicts. The brief's presence cannot be blinded; call this answer-key/producer blinding, not double blinding. Two independent blinded judges, at least one human, score the findings against the sealed behavioral key and record disagreements. The six floor runs use that same fixed reviewer configuration. This pilot makes no cross-model or human-reviewer performance claim; those require additional arms.
- Total: **16 × 2 + 6 = 38 reviews**, with **one observation per protected class per arm**. This can find a failure and assess feasibility. It cannot estimate a per-class catch rate or certify safety.
- Start T's clock at prospective task release, before generating its brief. Charge analyzer queueing, generation, failures/retries, reading, tools and verdict revisions. Freeze a 1,800 s timeout; retain every timeout/refusal and its elapsed cost. Keep complete diff access in both arms. Log actual artifact access and tool/turn counts; do not infer comprehension from bytes read.
- Every verdict needs decision, supported findings with locators, and evidence for independently specified context obligations. A bare NO-GO is not a catch. Any new unsupported GO, any protected defect caught in C and missed in T, any false claim about a captured mechanical relation, or any skipped required context obligation attributable to the brief rejects the tested route. Misses by both arms are reported as uncovered risk, not success. Analyze clean false alarms separately.
- Time improvement is assessed on the mechanical specimens; the small behavioral case is a separately reported null/control, not pooled to manufacture a general effect. Reading most of the diff is no longer an automatic rejection: it can be necessary context review. The criterion is complete safe verdict time and observed work, not a target byte ratio.

At the historical 573 s per review, 38 reviews cost **about 6.1 reviewer-hours** before fixture/answer-key/scoring work; 16 treatment generations add about **10 minutes** of machine wall, included in treatment endpoint measurement. Parallelism can reduce calendar time but not total reviewer work. The old 63-review plan was roughly **10 reviewer-hours** at the same planning rate, not a cheap confidence check. These costs exclude building the honest output and study runner. I would allocate a bounded day for preparation only after a small output redesign has an arithmetic reason to beat the current forecast; otherwise keep it parked.

### Then, only if the pilot merits qualification

A 16-pair pilot cannot honestly decide a 0–30 s effect with multi-minute noise. A normal-approximation planning example for a *paired mean* endpoint, one-sided 5% test and 80% power, is `n ≈ ((1.645 + 0.842) × SD_pair / effect)^2`. At paired SD 180 s, detecting 60 s needs about **56 independent pairs**; 30 s needs **223 pairs**. This is not a power calculation for the old paired median/bootstrap rule. Repeated mutants of one host and repeated reviewers introduce clustering, so nominal rows can substantially overstate effective n.

Use the pilot/floor to pre-register a **new held-out** sample size and estimator before qualification; do not keep adding arms until a favorable interval appears. A practical admission requires a predeclared meaningful benefit (I would retain at least 60 s net review saving), an uncertainty interval excluding no benefit under the chosen endpoint, all safety checks satisfied, and a scope restricted to the exercised classes. If variance or available independent hosts makes that infeasible, call it inconclusive and keep the route unconsumed. A finite study can qualify observed utility; it cannot prove the universal absence of semantic blind spots.

Given today's negative central forecast and failed contract, the pilot is an option to preserve, not a study I recommend funding now.

## 5. Why seventeen rounds happened — paragraph for Gene

Gene: this is Astra's design review; the seventeen fence rounds were Sol reviewing an Opus builder, and Sol's findings were real. The evidence here shows overclaimed assurance, not evidence that a previously certified runtime behavior regressed: the builder's first 141/141 count was never that certificate. We kept repairing witnesses beneath an unstable claim—first handwritten name resolution, then inventories, then refusal dispatch, then the table parser—while the headline still promoted a static match to “nothing left to review.” My original bet explicitly left namespace context and load order open, and I should have required that boundary in the first emitted contract. The process rule is: after a second finding in the same assurance class, stop the fix/re-review loop, re-derive the consumer's contract from the strongest claim, delete or narrow the unsupported authority, and test that whole boundary with independent valid examples plus a cold run before another fence. A more capable reviewer should move that decision earlier, not supply a more expensive eighteenth edge. Park this candidate; only an honest comparison aid with a blind utility result can earn review consumption.

## 6. Verification ledger

### Fresh design witnesses

Six false-clear witnesses plus one refused control, all with actual JVM observations and captured tree pairs, as above. The source under review is unmodified. This is direct design falsification, not an exhaustive hostile-input/security certification.

### R1–R8, clean-start evidence

The suite hardcodes shared paths under `/var/tmp/forge/item3/`. To avoid touching another seat's fixtures, copied the four `bin/` files into `baseline-bin/`; changed **only those literal temp prefixes in the copied test harness** to `baseline-tests/`. The brief/scanner/replay copies are byte-identical to the requested tree.

The clean-start baseline **fails** before completing R8: the renderer drift controls at lines 547–568 use `r8-ns-count`, but the fixture is created by the later specimen loop at lines 587 onward. `baseline-tests/r8-ns-count` does not exist, so `git` cannot start in its requested cwd. Log: `evidence/baseline-tables.log`. This is retained as a failure, not described as all-green.

For diagnosis only, a second disposable copy moves the existing drift-control block after the existing fixture loop, without changing assertions, specimens or product code. That run passes R1–R8: 202 function entries; 72 modelled/frozen Vars; 31 claim Vars; 38 executed resolver witnesses; 274 generated shadow witnesses (269 actually shadowed); four stale-caveat checks; 14 scanner-read entries; seven refusal specimens plus one defensive kind; the 384-map dispatch property; five drift controls; 20 required replay-row names. Log: `evidence/diagnostic-tables.log`. **This is not a green result on unmodified 10b77576**, and no diagnostic change was applied to the branch.

### Round 17, received during this review

Sol's verdict arrived at `/var/tmp/forge/ship/20260910T014707Z-10b77576307c/verdict-1.md`: **NO-GO, PB-FENCE-024**. Sequence multiplicity/order now works, but the strict Markdown table parser accepts a blank required `why` cell. Sol blanked the real producer's detail field; the existing R1–R8 suite still passed because both compared projections omit it. He restored the candidate. His reported pre-existing-fixture PASS does not conflict with my isolated cold-start failure.

That is a real finding. It does not address or overturn the stronger design counterexamples here, and fixing it alone would not change PARK.

### Full original replay

The original `preservation-replay` completed with exit **0**, producing all **27** JSON/Markdown rows in `baseline-replay/out/`. A separate observation checker (`verify-replay.py`) passes **38 targeted assertions** over those outputs: clean Cell C counts; the binding/macro collision signals; promotion, comment, omission, form/require-order and symlink failures; metadata owner/declaration handling; `.cljc` scope; spliced and comment requires; the two-namespace refusal; and the shapes controls. These are the historical targeted signals, not assertions that every control is globally CLEAR. The controls legitimately retain unrelated obligations. Logs: `evidence/baseline-replay.log`, `evidence/replay-assertions.log`; machine result: `evidence/replay-assertions.json`.

The observation check's first draft made one wrong expectation: it grouped `kind-collision` with probes that become `changed`. The fixture changes a macro to a function, and the correct signal is **35 frozen bodies rather than 34**, 98 matched rather than 99, and one explicit kind disagreement. After reading that fixture, I corrected the observer to require exactly that signal. The initial red assertion and first-draft checker remain as `evidence/replay-assertions-first-draft.*` and `verify-replay-first-draft.py`. No tool or replay bytes were changed to obtain the pass.

All 27 row names and their JSON are retained; no failing row was dropped. SHA-256 confirms the executed brief, scanner and replay copies are byte-identical to the requested tree. No full repository `make test` was run: no repository change is being offered for landing, and a generic green suite could not repair the refuted semantic claim.

### Definition of done for any authorized revival — follow-up disposition

**PARK remains in force. PB-FENCE-024 is open, not waived by an experimental label. No further Opus rounds; the cap is in force.** The follow-up authorizes recording this condition on the parked path, not starting another repair/review cycle.

Before any revived candidate can be offered, its DoD must include:

- `parse-refusal-table` rejects every blank required cell (`tree`, `file`, `refusal`, `why`), including whitespace-only content, as a parse error. Preserve rejection of escaped pipes, embedded newlines, a missing final column and a second header, plus exact row order and multiplicity.
- A behavioral drift control blanks a required cell in the real producer/renderer path and makes the oracle fail; retain Sol's blank-`why` mutation explicitly. Nonblank valid rows must continue to pass. A comparison that omits `why` cannot be the only assertion covering that field.
- R1–R8 pass from a fresh owned temp root, with fixture creation before use; every original replay row remains present and passes its targeted assertions.
- The narrowed comparison contract in §2 is implemented in both human and machine output; the six semantic counterexamples no longer receive a claim that review is complete. A passing parser test alone does not satisfy this condition.
- The revised preregistration and an independently planted blind falsifier qualify the exact experimental output before any review entrance consumes it. Until then, review consumption remains prohibited.

No branch fix, commit, push or additional Opus round was performed for this follow-up.

### Final handoff

The requested checkout remains clean at `10b77576307c6c1a7922baa82cc08a32d024ba2e` on `astra/proof-burden`. No files were changed in the builder worktree. No branch was merged, shipped, installed, pushed or committed. The original branch and observation history are retained. The full replay and both table-suite processes have terminated; evidence fixtures are retained for audit. This completed before the 60-minute interim deadline, so no separate interim was needed.

An exact counterexample can be regenerated from retained Git objects with:

```bash
bin/preservation-brief b6647e0e90e56118f65eaff984accd1ef3067900 5c53452b808ed56026a4de03a511690d1b2ba2cc --repo /var/tmp/forge/astra-item3/probes/resolved-load-v2 --roots src
```

That command returns the misleading requalification-tier CLEAR on the parked code. The corresponding `runtime.txt` and source literals show the two distinct values. The specimen-construction scripts were executed once in empty directories; reconstruct into a fresh owned root rather than rerunning them over populated fixture repositories.


Completed 2026-09-10T02:21:34.518430+00:00; elapsed 16.1 minutes of the 120-minute timebox.

Follow-up disposition recorded 2026-09-10T02:22:24.665488+00:00.
