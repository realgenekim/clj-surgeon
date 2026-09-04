## NO-GO

Independent round-four review of `bridge/admit-gate-r3` at `72357b6cff48fb89ad0d1c0c99ad76d575365798`
(Sol's content filter refused the brief; this review was run with substituted paths — fixtures under
`/var/tmp/forge/gate4-review-fx/opus`, verdict at `/home/forge/tmp/sol/gate4-opus-review.md`).

Provenance, verbatim:

```text
$ cd /home/forge/tmp/sol/gate4-wt && git rev-parse HEAD && git status --porcelain
72357b6cff48fb89ad0d1c0c99ad76d575365798
```

(`git status --porcelain` produced no output; the tree is clean. Nothing in the clone was committed,
pushed, stashed, staged or edited. All sabotage was done on `git archive` exports under the fixture
directory. No Surgeon server was needed or started.)

**Three of the four round-three blockers are genuinely closed and I proved each by sabotage or by
boundary execution. The fourth — "the text block is a superset of structuredContent" — is still
false, and it is now false on an ORDINARY receipt rather than a synthetic one. The builder's own
witness function, unmodified, fails 68 assertions on a live 20-file preview.** That is the single
blocking finding.

---

## 1. BLOCKING — the text block is still a strict SUBSET of structuredContent, on an ordinary 20-file preview, and the builder's own witness proves it

`src/clj_surgeon/mcp_admit_tool.clj:2135-2144` replaces round three's flat 40-fact cap with

```clojure
(def admit-fact-section-byte-budget
  (quot write-refusal/public-byte-budget 2))     ; = 16320
```

and `src/clj_surgeon/mcp_admit_tool.clj:2216-2236` drops whole leaves from the tail of the
path-sorted order until the rendered line fits that number. The structured face is bounded by the
FULL budget (`write-refusal/public-byte-budget` = 32640, `src/clj_surgeon/mcp_write_refusal.clj:13`).
So for every receipt between roughly 16 KB and 32 KB of JSON, structuredContent is published whole
and the text block is cut in half. **The text face is the one that loses.**

This is not a corner. A twenty-file preview does it.

### 1a. The builder's OWN witness, unmodified, on a live receipt

`test/clj_surgeon/admit_patch_test.clj:4675` `assert-text-names-every-structured-leaf!` is the
round-four witness for MCP-OP-ADMIT-134 — the second implementation that "shares no function and no
constant with the renderer". I called that exact Var, unaltered, on receipts produced by
`admit/execute-request!` on a real workspace.

Exact command:

```text
cd /home/forge/tmp/sol/gate4-wt && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate4-review-fx/opus/probe-d.clj'; echo EXIT=$?
```

Verbatim output:

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:witness-fn "#'clj-surgeon.admit-patch-test/assert-text-names-every-structured-leaf!"}
{:probe :builders-own-witness-on-a-real-receipt, :files 12, :ok true, :structured-json-bytes 9484, :public-byte-budget 32640, :witness-pass 265, :witness-FAIL 0, :witness-error 0}
{:probe :builders-own-witness-on-a-real-receipt, :files 20, :ok true, :structured-json-bytes 15086, :public-byte-budget 32640, :witness-pass 349, :witness-FAIL 68, :witness-error 0}
PROBE-D-DONE
EXIT=0
```

Read that second line carefully: **structured JSON is 15,086 bytes — less than HALF the 32,640-byte
budget, not truncated — and 68 leaves that structuredContent spells are absent from the text.** The
receipt is `:ok true`, an unremarkable successful preview of twenty one-line changes.

The witness is correct. The implementation is not. And the reason the suite is green is that every
fixture in `admit_patch_test.clj` produces a receipt below the 16,320-char fact budget — the witness
has never once been handed an input where the bound it is supposed to police actually bites.

### 1b. What is missing is not obscure metadata — it is top-level receipt fields

Same live drive, my own independent walk, at four sizes:

Exact command:

```text
cd /home/forge/tmp/sol/gate4-wt && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate4-review-fx/opus/probe-b.clj'; echo EXIT=$?
```

Verbatim output (leaves longer than the per-leaf ceiling excluded from the count, as the builder's
witness also excludes them):

```text
{:structured-leaves 150, :sample-missing [], :error-type nil, :text-says-it-dropped false, :payload-truncated false, :structured-json-bytes 5312, :ok true, :probe :live-preview-superset, :files 6, :leaves-missing-from-text 0, :public-byte-budget 32640, :text-chars 8408}
{:structured-leaves 264, :sample-missing [], :error-type nil, :text-says-it-dropped false, :payload-truncated false, :structured-json-bytes 9482, :ok true, :probe :live-preview-superset, :files 12, :leaves-missing-from-text 0, :public-byte-budget 32640, :text-chars 14901}
{:structured-leaves 416, :sample-missing [["source-unchanged" "true"] ["pre_image_binding" "unbound"] ["lock_scope" "none"] ["mutation_attempted" "false"]], :error-type nil, :text-says-it-dropped true, :payload-truncated false, :structured-json-bytes 15082, :ok true, :probe :live-preview-superset, :files 20, :leaves-missing-from-text 68, :public-byte-budget 32640, :text-chars 18845}
{:structured-leaves 796, :sample-missing [["source-unchanged" "true"] ["pre_image_binding" "unbound"] ["lock_scope" "none"] ["mutation_attempted" "false"]], :error-type nil, :text-says-it-dropped true, :payload-truncated false, :structured-json-bytes 29082, :ok true, :probe :live-preview-superset, :files 40, :leaves-missing-from-text 396, :public-byte-budget 32640, :text-chars 20747}
```

The four fields the path sort strands at the tail are
`source-unchanged`, `pre_image_binding`, `lock_scope`, `mutation_attempted` — the receipt's answers
to *did you touch my files*, *is this commit bound to the bytes I read*, *what did you lock*, *did
you try to write*. Path-alphabetical ordering means the fields a caller most needs are dropped
because their names sort late, not because they matter least. At forty files, **396 of 796 leaves**
are gone and the receipt still reads `ok` in text.

### 1c. The elision is not forced — the budget it obeys is invented, and the text block has no bound at all

The docstring at `src/clj_surgeon/mcp_admit_tool.clj:2135-2144` justifies half-budget as "the other
half is headroom for the header, error sentence, remedy, detector note and the next_call". At the
20-file case the whole receipt is 15,082 bytes and the text is 18,845 characters: the headroom was
never contested, and 68 leaves were dropped anyway. Round three's blocking defect was a second,
invented budget (1,024 chars) with no relation to the real one; MCP-OP-ADMIT-135 correctly deleted
that one for `next_call`, and MCP-OP-ADMIT-134 reintroduced the identical mistake one field over.

And the text block itself is not bounded by anything. From the same battery:

```text
{:probe :huge-leaf, :structured-error-chars 30000, :structured-json-bytes 30161, :text-chars 30524, :text-has-full-error true, :text-has-elision-note true}
```

A 30,524-character text block publishes without complaint. So the 16,320 cap is a self-imposed
ceiling that costs leaves and buys nothing measurable.

### 1d. Why this is blocking and not an advisory

The brief's rule: *"a text receipt that omits a leaf the structured receipt carries … is BLOCKING."*
It is also the exact defect round three blocked on (findings 2 and 3), restated at a different
constant. The docstrings at `mcp_admit_tool.clj:2098-2122` and `2185-2214` assert without
qualification that "a receipt has two faces and neither is allowed to say less" while the code four
lines down implements *the text says less above 16 KB*. The witness, the EARS text and the code
disagree; the code wins in the field.

**Remedy that would clear this finding:** either (a) budget the fact section against what is
actually left of `public-byte-budget` after the header/error/remedy/detector/next_call lines are
counted — one budget, as MCP-OP-ADMIT-135 already established for `next_call` — and prove it with a
fixture whose receipt exceeds the bound; or (b) if leaves genuinely must be droppable, say so in the
EARS text and in `assert-text-names-every-structured-leaf!`, and stop claiming supersetness. Either
way the suite must contain a fixture ABOVE the bound. Add one that drives a ≥20-file preview
through `execute-request!` and runs `assert-text-names-every-structured-leaf!` on the result — it
goes red today, which is the point.

---

## 2. PASS — §1: the declared enumeration is real, is enforced outside every catch, and a planted dynamic kind is caught

Round three's blocker was that a kind built dynamically had no literal to scan for and every witness
stayed green. That is closed. `src/clj_surgeon/mcp_admit_tool.clj:184-238` declares
`admit-refusal-kinds` (33 keywords); `:244-268` `checked-refusal-kind!` throws a plain
`IllegalArgumentException` — correctly NOT an `ex-info`, since an `ex-info` with `:error-type` is
exactly what this namespace's catch clauses relabel; and it is called from `bound-receipt`
(`:1947-1961`), which sits outside every catch on `execute-request!`'s path, and from the handler
edge (`:2348-2373`).

I planted the round-three attack on a `git archive` export — the reachable invalid-mode refusal at
`src/clj_surgeon/mcp_admit_tool.clj:1541`, rewritten to
`(refusal context (keyword (str "planted" "-runtime-kind")) …)`.

Exact command:

```text
cd /var/tmp/forge/gate4-review-fx/opus/export-plant && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main plant-probe.clj'
```

Verbatim output:

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:probe :planted-kind-through-execute-request, :result {:threw java.lang.IllegalArgumentException, :msg "admit gate refusal kind is not enumerated: :planted-runtime-kind -- add it to clj-surgeon.mcp-admit-tool/admit-refusal-kinds with a fixture that drives it through the entrance, or stop constructing it"}, :enumeration-has-planted false}
PLANT-PROBE-DONE
```

And the suite goes loudly red on the same export — the `:once` execution recorder names the planted
kind by hand:

Exact command:

```text
cd /var/tmp/forge/gate4-review-fx/opus/export-plant && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main run-admit.clj'; echo EXIT=$?
```

Verbatim output (tail):

```text
expected: (= enumerated observed)
  actual: (not (= #{... :invalid-patch :invalid-workspace-root ...} #{... :invalid-patch :planted-runtime-kind :invalid-workspace-root ...}))

Ran 143 tests containing 2997 assertions.
2 failures, 1 errors.
{:test 143, :pass 2994, :fail 2, :error 0 ... :error 1, :type :summary}
EXIT=1
```

I also checked the two escapes the brief named. **No publish path escapes the recorder:** the admit
tool is registered exactly once outside its own namespace (`src/clj_surgeon/mcp_tool.clj:1555`,
`admit-tool/admit-clojure-patch-tool`), and every branch of `execute-request!`
(`src/clj_surgeon/mcp_admit_tool.clj:1999, 2013, 2025, 2054`) returns through `bound-receipt`,
including the arbitrary-`ex-data` forwarding at `:2033-2038`. **The six "not reachable" excuses at
`test/clj_surgeon/admit_patch_test.clj:5355-5394` are argued, not asserted**, and the two I could
attack behaved as claimed.

---

## 3. PASS — §4: the 1,024-char ceiling is gone and the boundary is exact

`src/clj_surgeon/mcp_admit_tool.clj:1901-1943` `oversize-next-call-refusal`; `:2238-2270`
`admit-rendered-next-call` now renders the JSON verbatim at any size. I drove the boundary at
32,639 / 32,640 / 32,641 / 42,640 encoded characters through `bound-receipt` (production path,
`elapsed_ms` restored as `mcp-operation/finalize-result` does):

Verbatim output (from `probe-b.clj`, long padding elided as `aaa…aaa` for readability only):

```text
{:own-next-call {...}, :sample-missing [], :error-type :invalid-patch, :verbatim-in-text true, :encoded 32639, :structured-json-bytes 32911, :probe :next-call-boundary, :next-call-characters nil, :target 32639, :leaves-missing-from-text 0, :text-chars 33281}
{... :error-type :invalid-patch, :verbatim-in-text true, :encoded 32640 ...}
{:own-next-call nil, :sample-missing [], :error-type :next-call-exceeds-public-budget, :verbatim-in-text false, :encoded 32641, :structured-json-bytes 1084, :probe :next-call-boundary, :next-call-characters 32641, :target 32641, :leaves-missing-from-text 0, :text-chars 1598}
{:own-next-call nil, :sample-missing [], :error-type :next-call-exceeds-public-budget, :verbatim-in-text false, :encoded 42640, :structured-json-bytes 1084, :probe :next-call-boundary, :next-call-characters 42640, :target 42640, :leaves-missing-from-text 0, :text-chars 1598}
```

Exactly at the budget it is verbatim; one byte over it is a typed refusal that names the size, the
budget and the lever; **the refusal's own `next_call` is `nil`, so the refusal cannot itself
oversize** (`:own-next-call nil`) — and its text loses no leaf. A `next_call` whose JSON contains the
renderer's own "none — this receipt has no follow-up call" marker still renders verbatim:

```text
{:probe :marker-in-next-call, :verbatim true, :tail "clojure_patch · ok=false · operation=admit-patch-refused · source-unchanged=true\nnext_call · {\"tool\":\"admit_clojure_patch\",\"arguments\":{\"note\":\"next_call · none — this receipt has no follow-up call\"}}"}
```

---

## 4. PASS — §2/§3 shape and encoding handling, below the bound

The empty exclusion set (`src/clj_surgeon/mcp_admit_tool.clj:2098-2122`, `#{}`) and the value-less
shapes are correct. Every attack the brief named comes back clean **as long as the receipt is under
the fact budget**:

```text
{:probe :depth-40, :missing-leaf-count 0, :sample [], :bottom-rendered true}
{:probe :marker-in-leaf, :missing 0, :text-has-fake-marker true, :sample []}
{:probe :unicode, :structured-error-spelling "né中😀 tail", :text-contains-raw true, :missing 0, :sample []}
{:probe :huge-leaf, :structured-error-chars 30000, :structured-json-bytes 30161, :text-chars 30524, :text-has-full-error true, :text-has-elision-note true}
```

Depth-40 nesting reaches the bottom leaf; a leaf whose value spells the overflow marker does not
confuse the walk; non-ASCII and a surrogate pair (😀) render byte-identically in both faces; a
30,000-character leaf is elided with a stated character count rather than dropped. These are real
improvements over round three and I found nothing wrong with them.

---

## 5. Advisories (not blocking, but they should ride the same fix)

**5a. The guard's predicate and the renderer's predicate disagree about what a refusal is.**
`checked-refusal-kind!` (`src/clj_surgeon/mcp_admit_tool.clj:261`) fires only on
`(false? (:ok receipt))`; `summary` (`:2271`) branches on truthiness, `(if (:ok result) …)`. So a
receipt with `:ok nil` is rendered to the caller as a refusal and is never checked:

```text
{:probe :guard-ok-predicate, :ok-false-unenumerated :threw, :ok-nil-unenumerated :passed, :ok-missing-unenum :passed, :ok-string-false-unenum :passed, :ok-zero-unenum :passed, :summary-of-ok-nil-refusal "admit_clojure_patch refused · planted-runtime-kind · 1.00 ms"}
```

I could not reach it: every construction merges `empty-receipt` and then a literal `:ok false`. But
`refusal` (`:271-279`) merges its caller's `data` map LAST, so an override is one keyword away, and
the guard exists precisely because "nobody would write that" was wrong last round. One-character
fix: `(not (true? (:ok receipt)))`.

**5b. The `transaction-recovery-required` fixture is a busy-spin race in a fast merge gate.** I ran
`test/clj_surgeon/admit_patch_test.clj:5277` twenty times:

```text
{:probe :transaction-recovery-required-20-runs, :runs 20, :runs-with-failures 0, :detail [[3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0] [3 0 0]]}
```

20/20 green at load average ~9 on a 16-core box. **My ruling on the builder's flag: not blocking at
this evidence, but it must not stay here.** Two reasons. First, the house rule the brief cites — a
resource/timing bound is a battery, not a fast-suite witness — and this is a 64-file write racing a
spinning watcher thread. Second and worse, it is now *load-bearing for a different witness*: if it
flakes, the `:once` set-equality assertion reports "the enumeration claims kinds no fixture drives"
and the entire enumeration proof goes red for an unrelated reason. Move it to a battery target and
let the `:once` witness accept a named battery-only kind.

**5c. The builder's self-flagged 300-character-basename gap did not reproduce as described.** The
justification at `test/clj_surgeon/admit_patch_test.clj:5388-5394` says such a target "returns
`admit-tool-failure` from an escaped ENAMETOOLONG IOException". I drove it through
`execute-request!` in both modes:

```text
{:probe :long-basename, :mode "preview", :ok true, :error-type nil, :enumerated false, :error nil}
{:probe :long-basename, :mode "commit", :ok false, :error-type :verification-incomplete, :enumerated true, :error "Verification did not complete (unverified: verification-not-requested); nothing was written. Run mode preview to see the same receipt without a write, and repair verification-not-requested before comm"}
```

Preview succeeds; commit refuses under an ENUMERATED kind. **Ruling: acceptable-declared, not
blocking** — under both readings the entrance publishes no unenumerated kind, which is the only
thing this gate is about. The imprecise typing (`admit-tool-failure` where a path refusal belongs)
is a real but separate defect and is correctly filed rather than papered over. Note for the record
that the docstring's claim is not reproducible on this platform as written; it should name the exact
mode/verify combination that produces it, or be corrected.

**5d. The "one budget" is breached by the field that never gives ground.** At `next_call` = 32,640
characters the published receipt is `:structured-json-bytes 32911` — 271 bytes over the
`public-byte-budget` the refusal text calls "the public payload budget". This is the deliberate
consequence of MCP-OP-ADMIT-135 and I would not change the priority, but the receipt should not
describe as a budget a number it is allowed to exceed.

---

## 6. Every named gate reproduces, and every RED/GREEN pair is genuine

All commands run from `/home/forge/tmp/sol/gate4-wt` at `72357b6c`.

```text
$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 740 tests containing 9314 assertions.
0 failures, 0 errors.
EXIT_CODE=0
```

```text
$ ~/bin/suite-run bb test/run_all.clj
Ran 814 tests containing 6724 assertions.
0 failures, 0 errors.
EXIT_CODE=0
```

```text
$ make mcp-operation-oracle
# @spec MCP-OP-ORACLE-001
swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
ORACLE_EXIT=0
```

```text
$ ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :violations (count (:violations r))}))"
{:ok true, :specs 355, :violations 0}
AUDIT_EXIT=0
```

```text
$ make admit-analyzer-memory-self-test
PASS n=100 findings=600 analyzer-bytes=83606 ran=true introduced=300 heap-start-MiB=24 heap-peak-MiB=35 budget-MiB=409 max-heap-MiB=512 wall-ms=68
PASS n=1000 findings=6000 analyzer-bytes=847706 ran=true introduced=3000 heap-start-MiB=23 heap-peak-MiB=30 budget-MiB=409 max-heap-MiB=512 wall-ms=234
PASS n=10000 findings=60000 analyzer-bytes=8596706 ran=true introduced=30000 heap-start-MiB=23 heap-peak-MiB=46 budget-MiB=409 max-heap-MiB=512 wall-ms=1857
admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m
MEM_EXIT=0
```

RED/GREEN, each on a fresh `git archive` export of the exact sha, `clj-surgeon.admit-patch-test`
alone:

```text
### 2d5290b2 :: 3 failures, 0 errors.      (RED §1)
{:test 131, :pass 2981, :fail 3, :error 0, :type :summary}
### c9fcc345 :: 97 failures, 0 errors.     (RED §2)
{:test 123, :pass 2581, :fail 97, :error 0, :type :summary}
### 3af55304 :: 164 failures, 0 errors.    (RED §3)
{:test 127, :pass 2701, :fail 164, :error 0, :type :summary}
### ad0ff7b3 :: 13 failures, 0 errors.     (RED §4)
{:test 131, :pass 2957, :fail 13, :error 0, :type :summary}
### 8de375da :: 0 failures, 0 errors.      (GREEN §2)
{:test 126, :pass 2688, :fail 0, :error 0, :type :summary}
### 162ea3eb :: 0 failures, 0 errors.      (GREEN §3)
{:test 127, :pass 2951, :fail 0, :error 0, :type :summary}
### 79257657 :: 0 failures, 0 errors.      (GREEN §4)
{:test 131, :pass 2980, :fail 0, :error 0, :type :summary}
### 72357b6c :: 0 failures, 0 errors.      (GREEN §1, the tip)
{:test 143, :pass 3000, :fail 0, :error 0, :type :summary}
```

Every RED is red at its own sha for its stated reason and every GREEN is green; the tip is
**143 tests / 3000 assertions / 0 failures**, exactly the builder's claim. The sabotage control is
finding 2's plant: 143/2997 with 2 failures and 1 error, the set-equality assertion naming
`:planted-runtime-kind` by hand. These controls are genuine — **and none of them catches finding 1**,
for the same structural reason as last round: no fixture drives an input past the bound.

## 7. Cleanup

```text
$ ls -A /var/tmp/forge/gate4-review-fx/opus 2>&1; echo LS_EXIT=$?
ls: cannot access '/var/tmp/forge/gate4-review-fx/opus': No such file or directory
LS_EXIT=2
$ ss -ltnp 2>/dev/null | awk '$4 ~ /:(8144|8145|8146)$/'
(no output — no listener on 8144-8146; no Surgeon server was started for this review)
$ cd /home/forge/tmp/sol/gate4-wt && git rev-parse HEAD && git status --porcelain
72357b6cff48fb89ad0d1c0c99ad76d575365798
(clean)
```

---

## NO-GO

`72357b6c` does not land on MCP/main: it merges cleanly — `git merge-tree --write-tree HEAD
origin/MCP/main` returned exit 0 against `origin/MCP/main` at `51da9d85` (not the `6c54089e` the
builder claimed; MCP/main has advanced since) — but a clean merge is not a GO, and this tip
publishes an ordinary twenty-file preview whose text block omits 68 of the leaves its own
structuredContent carries, which the builder's own witness confirms and no fixture drives.
