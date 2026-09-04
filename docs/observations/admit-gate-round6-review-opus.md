## GO-WITH-FIX

Independent round-six review of `bridge/admit-gate-r3` at
`ed20fa359356799dde787d892c83931d9d4d2c4b`. Sol's content filter refused this brief after 3 KB, so
this review was run with substituted paths: fixtures under `/var/tmp/forge/gate6-review-fx/opus`,
verdict at `/home/forge/tmp/sol/gate6-opus-review.md`, Surgeon port range 8156–8158 (unused — no
server was needed), clone at `/home/forge/tmp/sol/gate4-wt`.

Provenance, verbatim:

```text
$ cd /home/forge/tmp/sol/gate4-wt && git rev-parse HEAD && git status --porcelain
ed20fa359356799dde787d892c83931d9d4d2c4b
```

`git status --porcelain` produced no output; the tree is clean. Nothing in the clone was committed,
pushed, stashed, staged or edited; every sabotage was applied to a `git archive` export under the
fixture directory. No Surgeon server was started and no listener was opened on 8156–8158; none of
7888 / 7890 / 7894 / 7895 / 7906–7910 / 7941–8155 / 8159–8170 was contacted.

**Round five's blocker is genuinely closed, and MCP-OP-ADMIT-140 and -142 held against every attack
I could build — including the six caller shapes the builder's table did not drive.** A
60,000-character unknown key NAME, a 60,000-character key inside `expect_pre_sha256`, a
60,000-character VALUE inside `expect_pre_sha256`, a 60,000-character file path in a patch header, a
60,000-character `workspace_root`, bulk in `note`, and the cut marker planted inside the caller's own
value all publish inside the budget on both faces, with zero leaves missing from the text and no
value echoed verbatim. `transaction-recovery-required` kept its kind, its remedy,
`mutation_attempted` and `source-unchanged` at **eight** reduction rungs, including three the
builder's table does not contain.

**MCP-OP-ADMIT-141 and -143 do not hold, and both fail on the ordinary path.** Round five's blocker
was a receipt at 187% of the budget saying nothing had been omitted. Round six's replacement is the
same dishonesty pointed the other way: a **1,672-byte** receipt — 5% of the budget — publishing
`receipt_over_budget=true`, `receipt_residual_bytes 30179`, `error_truncated true` and a sentence cut
"to fit the public payload budget", on **both faces**, at the MCP handler's own callback, for a
60-file patch with one clj-kondo finding. That is finding 1. Finding 2 is `payload_truncated true`
alongside `payload_omitted {}` and `payload_omitted_bytes 0` — MCP-OP-ADMIT-143's own EARS sentence
("`payload_truncated` names what it omitted") falsified by its own honesty predicate, which tests
`nil?` one line below where it correctly tests `empty?`.

Neither over-publishes, loses a leaf, mislabels a kind, nor produces an unexecutable `next_call` —
the four things the brief calls blocking — so this is GO-WITH-FIX rather than NO-GO. But both are
receipts lying about themselves in the fields this round added, on inputs an ordinary caller sends,
and the round's own honesty witness is green over both. They must be fixed and the named witnesses
re-run at the tip before this merges.

---

## 1. MUST FIX — a 1,672-byte receipt publishes `receipt_over_budget=true`, `receipt_residual_bytes 30179` and a sentence "cut to fit the public payload budget", on both faces, on the ordinary wide-fan-out path

### 1a. The mechanism

`src/clj_surgeon/mcp_admit_tool.clj:2296-2334` — `bound-receipt` runs `reduce-receipt-to-budget`
**before** it considers the oversize `next_call`:

```clojure
        bounded (reduce-receipt-to-budget faced)]
    (checked-refusal-kind!
      (if-let [replacement (oversize-next-call-refusal bounded)]
        (reduce-receipt-to-budget (bound-identity-values replacement))
        bounded))
```

When the receipt is over budget *because of its `next_call`*, that first reduction cannot reach the
budget: `next_call` is an identity key, `bound-identity-values` exempts it, and `cut` shortens only
`:error` and `:remedy`. So reduction walks the whole ladder — drops every droppable field, cuts both
sentences (`:error_truncated true`), and lands in MCP-OP-ADMIT-141's new terminal branch
(`:2246-2270`), which stamps `:receipt_over_budget true`, `:receipt_residual_bytes`,
`:receipt_residual_text_characters` and `:receipt_unreducible_fields` onto the receipt.

`oversize-next-call-refusal` then takes MCP-OP-ADMIT-142's already-refusing arm (`:2076-2089`),
which is `(merge receipt measurements {:next_call nil …})` — it carries all four terminal
annotations and the cut sentences forward. Dropping the `next_call` is what actually makes the
receipt fit, and it fits with room to spare. **Nothing clears the annotations, and nothing re-tests
whether the statement they make is still true.** All four are in `receipt-reduction-keys`
(`:2113-2126`) so reduction may never drop them, and `:receipt_over_budget` and
`:receipt_residual_bytes` are in `admit-receipt-fact-head` (`:2501-2513`) so the text face can never
elide them either — both by design, both this round's changes.

### 1b. Executed end to end at the MCP handler's own callback, on an ordinary caller input

A 60-file patch under 200-character directory names (the shape the builder's own `deep-60` fixture
uses) with one blocking clj-kondo finding. The `next_call` carries one `expect_pre_sha256` digest
per file, which is what puts it over.

Exact command:

```text
cd /home/forge/tmp/sol/gate4-wt && ~/bin/suite-run bash -lc 'exec java -Xss2m -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate6-review-fx/opus/probe-e.clj'
```

Verbatim output:

```text
=== E2 the STALE over-budget flag end to end: a refusal AFTER expect-pre is set ===
{:error-chars 121, :remedy nil, :receipt_over_budget true, :OVER-TEXT false, :json-bytes 1672, :OVER-STRUCTURED false, :ok false, :kind :verification-failed, :error_truncated true, :probe :E2-blocking-lint-60-deep-files, :receipt_residual_bytes 30179, :next_call_omitted true, :text-chars 2475}
  text says receipt_over_budget=true?  true
  TEXT-HEAD: admit_clojure_patch refused · verification-failed · 186.80 ms
Snapshot verification failed (blocking-lint-findings)…[cut to fit the public payload budget; full text in the server log]
source unchanged
facts · ok=false · operation=admit-patch-refused · source-unchanged=true · mutation_attempted=false · pre_image_binding=unbound · lock_scope=none · error=Snapshot verification failed (blocking-lint-findings)…[cut to fit the public payload budget; full text in the server log] · next_call=null · rece
```

Read the numbers against the sentences. The published receipt is **1,672 bytes** and its text face is
**2,475 characters**; the budget is **32,640**. The receipt says `receipt_over_budget true` and names
a residual of **30,179 bytes** it is not. Its 52-character error sentence, "Snapshot verification
failed (blocking-lint-findings)", is complete — and carries a marker saying it was cut to fit a
budget it is 0.16% of, pointing an MCP client at a server log it cannot read.

This is the most common wide-fan-out refusal the gate publishes. The lint finding is the reason for
the refusal; the caller is being told, in the receipt's own words on both faces, that the gate could
not fit its answer inside its budget.

### 1c. And on the safety-critical receipt it damages the recovery instruction

The same shape on `transaction-recovery-required`, driven through `bound-receipt` — the one point
every published receipt passes:

```text
=== D1 an ALREADY-REFUSING receipt with an oversize next_call ===
{:receipt_residual_text_characters 86967, :receipt_over_budget true, :receipt_reduced true, :json-bytes 1414, :receipt_unreducible_fields ["next_call" "remedy" "error" "error-type" "operation" "mutation_attempted" "source-unchanged" "mode" "ok"], :receipt_bytes_before 36006, :ok false, :kind :transaction-recovery-required, :receipt_residual_bytes 36315, :next_call_omitted true, :text-chars 2030}
  TEXT-HEAD: admit_clojure_patch refused · transaction-recovery-required · 1.00 ms
the rollback could not restore src/a/f000.clj…[cut to fit the public payload budget; full text in the server log]
whether the workspace was changed is unverified
remedy · restore src/a/f000.clj from version control by hand…[cut to fit the public payload budget; full text in the server log]
```

A 1,414-byte receipt. `receipt_unreducible_fields` lists nine fields it claims it could not drop,
including a `next_call` it did drop. **The remedy a human needs after a failed rollback — "restore
src/a/f000.clj from version control by hand" — is 49 characters and is published with a cut marker
telling the reader it is incomplete and to go find the rest in a server log.** Round five's verdict
blocked on this receipt reaching the caller as a size complaint; it no longer does, but its recovery
instruction is now labelled as truncated for a budget that is not binding on it.

### 1d. Why the round's own witness is green over it

`receipt-self-description-holds?` (`test/clj_surgeon/admit_patch_test.clj:6148-6183`) checks
over-budget-without-saying-so:

```clojure
          (and (> (write-refusal/json-bytes receipt) budget)
               (not (:receipt_over_budget receipt)))
          (conj "over budget without saying so")
```

There is no clause for the converse — under budget *while* saying so. And
`a-safety-critical-refusal-keeps-its-kind-at-every-reduction-rung` (`:6057-6144`) drives exactly the
"next_call oversize" rung and asserts the kind, the safety claims and the sizes; it never asks
whether `receipt_over_budget` is true of the receipt it just measured at 1,414 bytes. This is the
fourth round running in which the defect is a universal claim standing on a partial witness — the
phrase the spec document uses at
`docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md:917`.

### 1e. Remedy

Two parts, both small.

1. In `bound-receipt`, after the replacement path, **re-derive rather than inherit**: if
   `public-faces-fit?` is true of what is about to be published, `dissoc` `:receipt_over_budget`,
   `:receipt_residual_bytes`, `:receipt_residual_text_characters` and `:receipt_unreducible_fields`,
   and clear `:error_truncated` only if the sentences were not in fact shortened (or, better, do not
   cut the sentences on a pass whose failure will be answered by dropping the `next_call`: test
   `public-faces-fit?` on `(dissoc receipt :next_call)` before entering `cut`, so the cheap
   correct move is taken first).
2. Add the missing clause to `receipt-self-description-holds?`:
   `(and (:receipt_over_budget receipt) (<= (json-bytes receipt) budget))` →
   `"says it is over budget while inside it"`, and the same for `:error_truncated` on a sentence
   shorter than the cut ceiling. Then extend
   `a-safety-critical-refusal-keeps-its-kind-at-every-reduction-rung` and
   `every-field-a-caller-can-influence-is-driven-with-bulk` to run it. Both go red today, which is
   the point.

---

## 2. MUST FIX — `payload_truncated true` with `payload_omitted {}` and `payload_omitted_bytes 0`: MCP-OP-ADMIT-143's own EARS sentence, falsified by its own predicate

### 2a. The mechanism

`src/clj_surgeon/mcp_write_refusal.clj:274-278` — when the receipt does not fit and **nothing in
`trimmable` has any content**, `bound-public-payload` still stamps the full truncation annotation:

```clojure
          (if (empty? candidates)
            (assoc current
                   :payload_truncated true
                   :payload_truncation "public-byte-budget"
                   :payload_omitted omitted
                   :payload_omitted_bytes (- original-bytes (content current)))
```

`omitted` is `{}` and the byte delta is `0`. `trimmable-receipt-keys` is `[:hazards :files]`
(`mcp_admit_tool.clj:2276-2278`), so any oversize receipt that carries neither — every refusal
raised before the patch is applied — takes this branch. `bound-receipt` then computes
`:payload_binding_face` from it (`:2306-2311`), attaching a face attribution to a trim that removed
nothing.

### 2b. Executed at the MCP handler edge on an ordinary caller input

A caller sends `expect_pre_sha256` with a long value — a wrong digest pasted from the wrong place:

```text
cd /home/forge/tmp/sol/gate4-wt && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate6-review-fx/opus/probe-b.clj'
```

Verbatim:

```text
{:payload_binding_face "structured", :receipt_omitted_fields ["drifted"], :remedy nil, :patch_bytes nil, :payload_omitted {}, :next_call {...}, :payload_truncated true, :kind :source-hash-mismatch, :probe :B3-expect-pre-VALUE-60k, :error "The workspace moved since the preview that authorized this commit: src/app/core.clj", :payload_omitted_bytes 0}
```

`payload_truncated true`, `payload_omitted {}`, `payload_omitted_bytes 0`. The actual drop was done
by reduction and is correctly recorded in `receipt_omitted_fields ["drifted"]`. The payload
annotation is a fossil of a trim that never ran, and it tells a reader that content was withheld
when none was.

### 2c. Why the witness is green

`receipt-self-description-holds?` (`test/clj_surgeon/admit_patch_test.clj:6157-6160`):

```clojure
          (and (:payload_truncated receipt)
               (nil? (:payload_omitted receipt)))
          (conj "payload_truncated with no payload_omitted")
```

`{}` is not `nil`. Four lines above, the same predicate correctly uses `empty?` for the receipt-level
case (`(empty? (:receipt_omitted_fields receipt))`). One operator, and it is the operator that makes
MCP-OP-ADMIT-143's EARS sentence — "`payload_truncated` names what it omitted" — testable.

### 2d. Remedy

Change `nil?` to `empty?` in the predicate (it goes red on `B3`'s shape), and in
`bound-public-payload` do not annotate a trim that trimmed nothing: when `candidates` is empty,
return `current` unchanged and let the caller's own bound answer, or annotate with a distinct typed
statement ("no trimmable collection was present") rather than the truncation record.

---

## 3. SHOULD FIX — `edge-throwable-refusal` publishes around `bound-receipt`: a 60,617-byte receipt at the public handler, no annotation

`handle-admit-clojure-patch` (`src/clj_surgeon/mcp_admit_tool.clj:2813-2841`) wraps its two catch
arms in `checked-refusal-kind!` **only** — neither is passed through `bound-receipt`. Both take the
exception message verbatim: the `Exception` arm as `(or (.getMessage error) (.getName (class error)))`,
and `edge-throwable-refusal` (`:2780-2812`) as `message` with no bound at all.

Driven with a `Throwable` raised inside the verification seam:

```text
=== D4 same, but the throw escapes as a Throwable (edge-throwable-refusal) ===
{:ok false, :kind :admit-tool-error, :error-chars 60000, :text-chars 60159, :OVER-STRUCTURED true, :json-bytes 60617}
```

**60,617 bytes published as structuredContent, 60,159 characters of text, 27,977 bytes over the
budget, with `receipt_over_budget` and `receipt_identity_bounded` both absent.** This is exactly
round five's blocking shape — a bound asserted in the EARS text with a path that does not pass
through it — on the one arm round six did not touch. MCP-OP-ADMIT-139's sentence is universal: "no
receipt clj-surgeon's admit gate publishes shall exceed the number its own refusal text calls the
public payload budget."

**The honest caveat, and why this is not finding 1:** I could not build a caller input that reaches
this arm with a large message. I drove four candidates — a 400,000-paren nested patch, a
60,000-character create-file basename, a 60,000-character line inside a hunk, and a `workspace_root`
pointing at a regular file — and every one produced a typed, bounded refusal
(`:patch-too-large` 845 B, `:verification-incomplete` 1,734 B, `:patch-does-not-apply` 1,510 B,
`:invalid-workspace-root` 667 B). The vector I used is an injected seam, not a caller. So this is a
real hole in a universal claim with no demonstrated caller path — worth the two-line fix (route both
arms through `bound-receipt`) rather than worth blocking on.

---

## 4. PASS — MCP-OP-ADMIT-140: I could not find a caller field with bulk that escapes

Twelve shapes at `handle-admit-clojure-patch`'s own callback, each measured for both faces, walked
for leaf-supersetness with **my own** JSON walker sharing no function or constant with the renderer
or with the builder's witness, and scanned for any leaf carrying the bulk verbatim.

```text
cd /home/forge/tmp/sol/gate4-wt && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate6-review-fx/opus/probe-a.clj'
```

| probe | kind | JSON bytes | text chars | missing from text | bulk echoed |
|---|---|---|---|---|---|
| A1 unknown KEY name, 60 k | `:patch-too-large` | 831 | 1,162 | 0 | none |
| A2 `expect_pre_sha256` VALUE, 60 k | `:source-hash-mismatch` | 1,471 | 2,110 | 0 | none |
| A3 `expect_pre_sha256` KEY, 60 k | `:patch-too-large` | 830 | 1,160 | 0 | none |
| A4 patch-header PATH, 60 k | `:invalid-source-path` | 1,464 | 2,542 | 0 | none |
| A5 `workspace_root`, 60 k | `:invalid-workspace-root` | 655 | 837 | 0 | none |
| A6 `note`, 60 k | *(none — `:ok true`)* | 2,052 | 3,246 | 0 | none |
| A7 `mode` carrying the CUT MARKER, 60 k | `:invalid-admit-request` | 1,350 | 2,146 | 0 | none |
| A8 `mode` exactly 120 chars | `:invalid-admit-request` | 1,256 | 1,985 | 0 | none |
| A9 `mode` 121 chars | `:invalid-admit-request` | 1,346 | 2,142 | 0 | none |
| A10 `verify`, 60 k | `:invalid-admit-request` | 1,350 | 2,146 | 0 | none |
| A11 `allow_partial` wrong-typed, 60 k | *(none — `:ok true`)* | 2,052 | 3,246 | 0 | none |
| A12 baseline clean preview | *(none)* | 2,051 | 3,244 | 0 | none |

Budget 32,640. Every kind published is in `admit-refusal-kinds`. The 120-character bound behaves at
the boundary (A8 quotes the value whole, A9 quotes 120 and states the cut). Planting the cut marker
itself inside the caller's value (A7) changes nothing — the marker is appended, never searched for.
`note` and `allow_partial` are unknown/wrong-typed keys: `execute-in-context!` destructures only
declared keys, so they are ignored and the call proceeds exactly as the baseline, which matches the
builder's table.

Identity-key bulk through `bound-receipt`, both `:ok` values, every key: bounded, named in
`receipt_identity_bounded`, both faces inside budget (probe C1) — with the two exceptions in
findings 3 and 5.

## 5. PASS — MCP-OP-ADMIT-142: the kind and the safety claim survive eight rungs

`transaction-recovery-required` through `bound-receipt` at every rung, including three the builder's
table does not carry (identity bulk in `mode`; identity bulk in `lock_scope` *with* an oversize
`next_call`; and everything at once):

```text
{:rung "fits", :KIND-KEPT true, :mutation_attempted true, :source-unchanged false, :remedy-says-fewer-files false, :OVER false, :json-bytes 277, :text-kind true, :text-mutation true}
{:rung "bulk dropped", :KIND-KEPT true, :mutation_attempted true, … :json-bytes 575}
{:rung "sentences cut", :KIND-KEPT true, :mutation_attempted true, … :json-bytes 895}
{:rung "next_call oversize", :KIND-KEPT true, :mutation_attempted true, :next_call_omitted true, … :json-bytes 1414}
{:rung "next_call oversize AND bulk", :KIND-KEPT true, :mutation_attempted true, :next_call_omitted true, … :json-bytes 1424}
{:rung "identity bulk (mode)", :KIND-KEPT true, :mutation_attempted true, … :json-bytes 528}
{:rung "identity bulk (lock_scope) + next_call", :KIND-KEPT true, :mutation_attempted true, :next_call_omitted true, … :json-bytes 1706}
{:rung "everything at once", :KIND-KEPT true, :mutation_attempted true, :next_call_omitted true, … :json-bytes 2242}
```

The kind is kept at every rung, `mutation_attempted true` and `source-unchanged false` survive, the
remedy never becomes "fewer files", and both faces state the kind and the mutation claim. Round
five's finding 1d is closed. `next_call` at the boundary (1 / 100 / 32,000 / 32,300 / 32,400 /
32,500 / 32,600 / 32,640 / 32,641 / 40,000 padding characters) shows no off-by-one and the
replacement's own `next_call` is `nil`, so it always fits.

## 6. PASS — the battery, and the battery receipt under attack

Three runs of `make admit-transaction-recovery-battery` on the export, all 3/3:

```text
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=144
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=131
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=200
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · kinds #{:transaction-recovery-required}
```

(runs two and three: identical, wall-ms 144/136/216 and 156/449/520.)

`a-battery-only-kind-names-a-target-that-exists-and-drives-it` under four receipt states. *(Running
one deftest in isolation makes the `:once` MCP-OP-ADMIT-133 fixture fail three times in every state,
because the enumeration recorder sees only the tests that ran — a harness artefact of my isolation,
not a defect. The signal is the delta above that constant baseline.)*

| receipt state | result | delta |
|---|---|---|
| present and correct | 9 pass, 3 fail (fixture artefact only) | — |
| **deleted** | `PRECONDITION · no battery receipt at target/… · run \`make admit-transaction-recovery-battery\`` printed; 7 pass, 3 fail | never green about the kind |
| **corrupted** (truncated EDN) | **1 ERROR** — `java.lang.RuntimeException: EOF while reading string` at `admit_patch_test.clj:5337` | red |
| **names the wrong kind** | **+1 FAIL** — "the battery ran and did NOT publish the kind its exemption claims: :transaction-recovery-required · receipt {… :kinds-published #{:invalid-patch}}" | red |
| **names the wrong target** | **+1 FAIL** — "the receipt names the target that wrote it" | red |

Never green on a bad receipt; PRECONDITION on an absent one, exactly as declared.

**Advisory 6a — the PRECONDITION is a `println` in a 4,143-assertion run, and it changes the count.**
`clj-surgeon.admit-patch-test` alone is **159 tests / 4,141 assertions / 0 failures** in my clone
(no battery receipt) and **159 / 4,143 / 0** in the export after the battery ran. The builder's
claimed 4,143 is only reproducible on a machine that has run the battery; on a fresh clone the
exemption rests on the structural checks alone and nothing says so above a println. Per house rule
17 the precondition needs a named, counted, visibly non-zero bucket — not a line of stdout inside a
suite that prints thousands.

## 7. RED→GREEN pairs

Each RED sha's new deftests run at that sha and at the following fix. *(The constant floor of three
failures per var is the `:once` enumeration fixture artefact described above.)*

| pair | at RED | at GREEN | real delta |
|---|---|---|---|
| `311ec9c8` → `a53313f0` (§140) | 21 pass, **20 fail** | 36 pass, 5 fail (artefact) | 15 |
| `733f4212` → `8f7747ff` (§141) | 14 pass, **20 fail, 2 error** | 27 pass, 9 fail (artefact) | 11 + 2 |
| `77842fd4` → `70b64178` (§142) | 33 pass, **13 fail** | 43 pass, 3 fail (artefact) | 10 |
| `bb0de868` → `a737d64d` (§143) | 52 pass, 6 fail | 53 pass, 5 fail | **1** |

The last line is the builder's own disclosed negative, confirmed: `every-field-a-caller-can-influence-is-driven-with-bulk`
is already green at its own RED sha (48 pass / 2 artefact failures at both), and only
`a-trimmed-payload-names-the-face-that-forced-the-trim` goes red. §143's red is proved by sabotage
143b instead, which I executed — see below.

## 8. Sabotage — every figure reproduced, every patch confirmed applied

Baseline on the `git archive` export at `ed20fa35` with the battery receipt present:
**159 tests / 4,143 assertions / 0 failures, 0 errors.**

Each sabotage was applied to a fresh copy of that export and confirmed with
`git diff --no-index --stat` before running, so a silently no-op'd patch could not run green.

| figure | sabotage | diff | claimed | measured |
|---|---|---|---|---|
| 140a | drop the `mode` normalisation in `execute-in-context!` | 1 file, +1/−3 | 1 | **1 failure** |
| 140b | make `bound-identity-values` the identity | 1 file, +1/−1 | 9 | **9 failures** |
| 141a | restore round five's dead tail `(if (or (:error_truncated current) (public-faces-fit? final)) final final)` | 1 file, +2/−26 | 4+2 | **4 failures, 2 errors** |
| 141b | remove the four `payload_*` keys from `receipt-reduction-keys` | 1 file, +1/−2 | 4 | **4 failures** |
| 142 | make `oversize-next-call-refusal` always build the fresh replacement | 1 file, +1/−1 | 7 | **7 failures** |
| 143 | drop `payload_binding_face` | 1 file, +1/−4 | 1 | **1 failure** |
| 143b | drop the `mode` normalisation AND exempt `:mode` from the identity bound | 1 file, +2/−4 | 6 | **6 failures** |

Seven for seven.

## 9. Gates, verbatim, with exit codes

```text
$ cd /home/forge/tmp/sol/gate4-wt && ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 756 tests containing 10455 assertions.
0 failures, 0 errors.
EXIT=0

$ ~/bin/suite-run bb test/run_all.clj
Ran 814 tests containing 6724 assertions.
0 failures, 0 errors.
EXIT=0

$ ~/bin/suite-run make mcp-operation-oracle
swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
EXIT=0

$ ~/bin/suite-run make admit-analyzer-memory-self-test
PASS n=100 findings=600 analyzer-bytes=83606 ran=true introduced=300 heap-start-MiB=24 heap-peak-MiB=35 budget-MiB=409 max-heap-MiB=512 wall-ms=66
PASS n=1000 findings=6000 analyzer-bytes=847706 ran=true introduced=3000 heap-start-MiB=23 heap-peak-MiB=78 budget-MiB=409 max-heap-MiB=512 wall-ms=252
PASS n=10000 findings=60000 analyzer-bytes=8596706 ran=true introduced=30000 heap-start-MiB=23 heap-peak-MiB=74 budget-MiB=409 max-heap-MiB=512 wall-ms=1946
admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m
EXIT=0

$ clj-surgeon.admit-patch-test alone
Ran 159 tests containing 4141 assertions.     (4143 with the battery receipt present — see 6a)
0 failures, 0 errors.
```

mcp-test 756/10455/0, bb 814/6724/0, oracle pass, analyzer 3/3, recovery battery 3/3 — all match the
builder's claims exactly.

**The intent audit (claimed 363/0) I could not locate:** there is no Makefile target, no `bin/`
directory in the repo, and nothing in `~/opt/claude-skills/linked-intent-testing` that runs it, so I
ran an equivalent of my own over the same corpus:

```text
ADMIT declared=106
ADMIT unlinked=0
markers with no declaration = 0
```

106 `MCP-OP-ADMIT-###` intents declared in `docs/intent/`, every one carrying at least one `@spec`
marker in `src/` or `test/`, and no `@spec` marker naming an undeclared id. (Across all families the
same scan reports 38 unlinked — every one of them `MCP-OP-ELAB-*` or `MCP-OP-SUBST-*`, pre-existing
and unrelated to this branch.) I report the number I measured, not the number claimed, because I
could not run the builder's tool.

## 10. Advisories

**10a. A 60,000-character key NAME publishes `:error-type :patch-too-large` on a 230-byte patch.**
Jackson's `StreamReadConstraints.getMaxNameLength()` is 50,000, so the decode throws before
`execute-in-context!` and `execute-request!` (`:2378-2384`) maps every `StreamConstraints` failure to
`:patch-too-large`. The sentence is honest — "request could not be decoded: Name length (60000)
exceeds the maximum allowed (50000, from `StreamReadConstraints.getMaxNameLength()`)" — but the
machine-readable field, and `next_call.blocked_by`, both say the patch is too large, `patch_bytes` is
absent, and there is no `remedy`. An agent that branches on `error-type` will split a 230-byte patch.
Give the name-length case its own enumerated kind, or at least a remedy that names the field.

**10b. `:error-type` bulk on an `:ok true` receipt escapes both bounds.** `bound-identity-values`
exempts `:error-type` "because `checked-refusal-kind!` already bounds it to an enumerated keyword"
(`:2129-2143`) — but `checked-refusal-kind!` fires only on `(not (true? (:ok …)))`. Measured through
`bound-receipt`: `{:ok-out true, :key :error-type, :json-bytes 60751, :OVER true}`. It sets
`receipt_over_budget true`, so it is not silent, and the gate never constructs an `:ok true` receipt
with an `error-type`, so this is bound-by-construction rather than caller-reachable. Narrow the
exemption to refusals.

**10c. MCP-OP-ADMIT-141's terminal answer appears to be unreachable through the production path in
its truthful form.** Every identity value is bounded to 1,024 bytes (twelve of them ≈ 12 KB), both
sentences are cut, everything else is droppable, and the `next_call` has its own answer — so
`bound-receipt` cannot return a genuinely over-budget receipt. I could reach `receipt_over_budget
true` only via `reduce-receipt-to-budget` directly (the builder's own witness shape) and via the
stale carry of finding 1. Once finding 1 is fixed, the intent's live surface may be empty; that is
fine, but the witness should say so — assert that `bound-receipt` *cannot* produce one, rather than
witnessing the annotation on a function the entrance never publishes from.

---

## GO-WITH-FIX

**Mergeability:** `git merge-tree --write-tree HEAD origin/MCP/main` against trunk
**`5e75bf8580f745df8cafa9556a235b8faf2b642f`** produces exactly one conflict, the `Makefile`
`.PHONY` line — this branch adds `admit-transaction-recovery-battery`, trunk adds `fanout-selftests`
and `tmp-leak-ratchet-self-test`, and keeping all three words resolves it — so once findings 1 and 2
are fixed and their named witnesses run red-then-green at the new tip on current main, this branch is
GO on its own for `MCP/main`.
