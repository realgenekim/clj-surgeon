## NO-GO

Independent round-five review of `bridge/admit-gate-r3` at
`d8cb9a0452be704203e24ebeee3e9836e4ede7ca` (Sol's content filter refused the brief; this review was
run with substituted paths — fixtures under `/var/tmp/forge/gate5-review-fx/opus`, verdict at
`/home/forge/tmp/sol/gate5-opus-review.md`, Surgeon port range 8156–8158, unused).

Provenance, verbatim:

```text
$ cd /home/forge/tmp/sol/gate4-wt && git rev-parse HEAD && git status --porcelain
d8cb9a0452be704203e24ebeee3e9836e4ede7ca
```

(`git status --porcelain` produced no output; the tree is clean. Nothing in the clone was committed,
pushed, stashed, staged or edited. All sabotage was done on `git archive` exports under the fixture
directory. No Surgeon server was needed or started.)

**Round four's blocker — the text block as a strict subset of structuredContent — is genuinely
closed, and I could not break it at 20, 40, 60 or 100 files. MCP-OP-ADMIT-136, 137 and 138 all hold
under attack. MCP-OP-ADMIT-139 does not: its own EARS sentence, "no receipt clj-surgeon's admit gate
publishes shall exceed the number its own refusal text calls the public payload budget," is false at
the MCP handler edge on a caller-controlled input.** A `mode` string of 60,000 characters publishes a
61,214-byte structuredContent — 28,574 bytes over — carrying a refusal sentence that names 32,640 as
the budget and blames a 389-character `next_call`. That is the single blocking finding.

---

## 1. BLOCKING — a caller-supplied `mode` publishes a 61,214-byte receipt whose own sentence calls 32,640 the budget, and blames a 389-character `next_call` for it

### 1a. The mechanism

`src/clj_surgeon/mcp_admit_tool.clj:1546-1559` — `execute-in-context!` takes the caller's `mode`
verbatim into `context`, and `refusal` (`:287`) merges `(empty-receipt (or (:mode context) "preview"))`,
so the caller's string lands in the receipt's `:mode`.

`:mode` is a member of `receipt-identity-keys` (`src/clj_surgeon/mcp_admit_tool.clj:2008-2011`) — the
keys `reduce-receipt-to-budget` may never drop. Reduction's last resort, `cut`
(`:2049-2060`), shortens only `:error` and `:remedy`. So a bulky `:mode` is unreachable by every arm
of the reduction.

Worse, `bound-receipt` (`:2107-2124`) publishes the oversize refusal **without ever bounding it**:

```clojure
(checked-refusal-kind! (or (oversize-next-call-refusal bounded)
                          bounded))
```

and `oversize-next-call-refusal` (`:1969-1972`) builds that replacement from
`(merge (empty-receipt (or (:mode receipt) "preview")) …)` — re-echoing the same 60,000 characters
into the receipt that is supposed to be the answer to "this did not fit". Nothing downstream of that
`or` measures anything.

### 1b. Executed at the MCP handler edge — the surface a real client hits

`handle-admit-clojure-patch` (`:2585`) hands `mcp-operation/invoke!` the receipt; `invoke!`
(`src/clj_surgeon/mcp_operation.clj:58-65`) calls `(callback [summary] (not (:ok result)) result)`,
so the third callback argument *is* the published `structuredContent`. I drove the real handler.

Exact command:

```text
cd /home/forge/tmp/sol/gate4-wt && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate5-review-fx/opus/probe-d.clj'
```

Verbatim output:

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:error-type :next-call-exceeds-public-budget, :published-text-chars 2038, :receipt_omitted_fields nil, :receipt_reduced nil, :payload_truncated nil, :probe :handler-edge-huge-mode, :budget 32640, :published-structured-json-bytes 61214, :OVER-BUDGET-BY 28574}
--- the refusal's own sentence, verbatim ---
this receipt's next_call is 389 characters and the receipt that would carry it is 61201 bytes; the public payload budget is 32640 bytes, envelope included, so this call cannot be published verbatim, and a next_call a caller cannot send back byte for byte is not a next_call
PROBE-D-DONE
```

Read the sentence against the measurement. The receipt says the budget is 32,640 bytes. The receipt
is 61,214 bytes. `receipt_reduced` is `nil`, `receipt_omitted_fields` is `nil`, `payload_truncated`
is `nil` — **there is no annotation of any kind**; a reader is told nothing was omitted while the
gate publishes 187% of the number it just quoted. And the blame is wrong: the `next_call` it refuses
to publish is **389 characters**, three orders of magnitude inside the budget it is accused of
breaking. MCP-OP-ADMIT-139's docstring at `:1962-1968` says the decision "fires only when the
next_call is the REASON the receipt cannot fit… blaming it is then a fact rather than a guess." Here
it is a guess, and the guess is wrong.

The same input through `execute-request!` (the entrance, one layer down) reproduces it:

```text
{:omitted nil, :error-type :next-call-exceeds-public-budget, :error-truncated nil, :reduced nil, :mode-echoed-chars 60000, :OVER-BUDGET true, :face {:json-bytes 61208, :text-chars 2031}, :mode-chars 60000, :ok false, :probe :caller-supplied-mode, :budget 32640}
```

At 1,000 characters the same path is well behaved (`:json-bytes 2103, :OVER-BUDGET false`), so this
is a bound that is simply absent, not a bound with an off-by-one.

### 1c. The class, not just the instance — any identity key carrying bulk escapes

`mode` is the reachable one, but the hole is the identity-key set as a whole. Driven through
`bound-receipt` (production path) with bulk in `:lock_scope`, an identity key the gate itself fills:

```text
{:probe :identity-key-bulk-lock-scope, :ok false, :error-type :invalid-patch, :reduced true, :omitted ["payload_truncation" "payload_omitted_bytes" "payload_truncated" "payload_omitted"], :error-truncated true, :after {:json-bytes 60563, :text-chars 1143}, :OVER-BUDGET true}
{:probe :identity-key-bulk-mode, :ok false, :error-type :invalid-patch, :after {:json-bytes 60540, :text-chars 1122}, :OVER-BUDGET true}
```

Note what reduction did in the first line: it ran, it exhausted every droppable field, it cut the
sentences (`:error-truncated true`) — **and then returned the receipt anyway, 60,563 bytes, still
over budget, with `receipt_reduced true` on it as if the reduction had succeeded.** The tail of
`reduce-receipt-to-budget` (`:2085-2087`) is

```clojure
(if (or (:error_truncated current) (public-faces-fit? final))
  final
  final)
```

— both arms return `final`. The predicate is computed and thrown away; there is no branch in which
failing to fit is treated as failing to fit. That dead `if` is the code's own admission that "we ran
out of things to drop" was never given an answer.

Note also that reduction here dropped `payload_truncated` and `payload_omitted` — the annotations
that tell the reader the payload was cut — because they are neither identity keys nor
`receipt-reduction-keys` and they are small, so they sort into the droppable set like bulk. A
receipt that discards its own truncation notice to make room is losing exactly the field a reader
needs.

### 1d. And the replacement drops a true safety claim to `false`

The same `oversize-next-call-refusal` path is the one §139's own docstring says must never swallow a
safety-critical receipt. It does — one field over from where the builder looked. Driven through
`bound-receipt` with a `transaction-recovery-required` receipt carrying an oversize `next_call`:

```text
{:error-type :next-call-exceeds-public-budget, :mutation_attempted false, :error-now "this receipt's next_call is 40054 characters and the receipt that would carry it is 40785 bytes; the public payload budg", :after {:json-bytes 1227, :text-chars 1840}, :remedy-now "narrow the request so its follow-up call and the receipt carrying it fit 32640 bytes; fewer files in one patch is the le", :ok false, :probe :oversize-safety-critical-next-call, :blocked_next_call_for :transaction-recovery-required, :source-unchanged false}
```

The input said `:mutation_attempted true` — a write was attempted and rolled back badly. The
published receipt says **`:mutation_attempted false`**, because `empty-receipt` seeds it `false`
(`:161`) and `oversize-next-call-refusal` copies forward only `:source-unchanged`. MCP-OP-ADMIT-139's
EARS text is explicit: "It shall NEVER change the receipt's `error-type`, its `source-unchanged`, its
`mutation_attempted` or its remedy: a 64-file rolled-back transaction … must not reach the caller as
a size complaint whose remedy is `use fewer files`." The remedy on that line is, verbatim, "fewer
files in one patch is the lever." The kind survives only in `:blocked_next_call_for`, which no
`error-type` consumer reads. The reduction arm was fixed for this; the next_call arm was not, and it
is the arm that fires.

Contrast the arm that *was* fixed, which behaves exactly as advertised — same receipt, oversized by
a map instead:

```text
{:omitted ["hashes"], :error-type :transaction-recovery-required, :remedy-head "restore src/a/f000.clj from your VCS by hand before retrying", :mutation_attempted true, :fits true, :reduced true, :after {:json-bytes 668, :text-chars 988}, :ok false, :probe :oversize-safety-critical-map, :before {:json-bytes 42901, :text-chars 32625}, :source-unchanged false}
```

Kind kept, remedy kept, `mutation_attempted` kept, `hashes` named as dropped, both faces inside the
budget. That is the correct behaviour, and it makes the next_call arm's divergence a defect rather
than a design choice.

### 1f. The witness's name is a universal claim; its fixture is one shape

`test/clj_surgeon/admit_patch_test.clj:5093` is called
`a-published-receipt-never-exceeds-the-number-it-calls-a-budget`, and it does assert exactly the
right predicate — `(is (<= (write-refusal/json-bytes published) budget))`. But every receipt it
publishes comes from one skeleton:

```clojure
(publish (fn [chars]
           (#'admit/bound-receipt
             {:ok true :operation :admit-patch-preview :mode "preview"
              :files [] :next_call (call chars)})))
```

`:mode "preview"`, `:files []`, and all the bulk in `next_call`. The whole space the bound has to
cover is "the receipt is large for some reason"; the fixture covers "the receipt is large because
`next_call` is". Change `"preview"` to a long string — which a caller does by sending one — and the
assertion the witness is named for is false. This is the third round in a row where the defect is
not a wrong predicate but **a universal claim standing on a partial witness**, which is the phrase
the spec document itself uses at `docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md:917`.

### 1e. Why blocking

The brief's rule is that a receipt whose own text is not executable/consistent is blocking, and
§139's EARS sentence is a universal claim about every receipt the gate publishes. It is falsified by
a single string argument from an ordinary caller, at the public handler, with no annotation. It is
also the same failure shape round four blocked on and round three before it: a bound asserted in the
docstring and the EARS text, and a path that does not pass through it. The witness set does not
contain a case where `bound-receipt`'s output is measured against `public-byte-budget` for a receipt
whose bulk is in an unreducible key — which is why 748 tests are green.

**Remedy that would clear this finding.** Three parts, all small:
(1) run the oversize refusal through the same bound as everything else — `reduce-receipt-to-budget`
on `(oversize-next-call-refusal bounded)` before publishing it, not around it;
(2) give `reduce-receipt-to-budget` a real terminal answer instead of `(if p final final)`: when
nothing droppable remains and the sentences are already cut, cut the remaining non-scalar identity
values too (`:mode` is a caller string, not a safety claim) and say so, or publish a typed
`receipt-exceeds-public-budget` — and either way assert the postcondition, since the function's
entire job is a postcondition;
(3) carry `:mutation_attempted`, `:error-type` and `:remedy` forward in
`oversize-next-call-refusal` exactly as the reduction arm now carries them, or route it through
reduction so it inherits that behaviour.
Then add the fixture the suite lacks: for a set of receipts whose bulk sits in each identity key in
turn, assert `(<= (json-bytes (bound-receipt r)) public-byte-budget)`. It goes red today, which is
the point.


---

## 2. PASS — MCP-OP-ADMIT-136: the invented half-budget is gone, and I could not make the text a subset at any width

`admit-fact-section-byte-budget` is deleted. `summary`
(`src/clj_surgeon/mcp_admit_tool.clj:2500-2523`) renders the head and the verbatim `next_call` first,
measures them, and charges the fact walk `(- budget (count fixed) 1)` — the exact remainder.
`admit-receipt-fact-head` (`:2282-2298`) pins the nine fields round four's path-alphabetical sort
stranded. `public-faces-fit?` (`:1925-1936`) is one predicate over two faces, and
`write-refusal/bound-public-payload` now takes it as an argument
(`src/clj_surgeon/mcp_write_refusal.clj:248-291`), so the STRUCTURE gives ground in one pass with one
cumulative omission record.

I drove 20-, 40-, 60- and 100-file previews through `execute-request!` and walked the published
receipt with **my own** JSON leaf implementation (no function, no constant, no exclusion list shared
with the renderer or with the builder's witness).

Exact command:

```text
cd /home/forge/tmp/sol/gate4-wt && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate5-review-fx/opus/probe-a.clj'
```

Verbatim output:

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:missing-from-text 0, :sample-missing [], :error-type nil, :payload-truncated nil, :error-truncated nil, :elided-note-present false, :receipt-reduced nil, :facts-elided-stated nil, :short-leaves 424, :receipt-omitted-fields nil, :structured-json-bytes 14918, :leaves 424, :ok true, :probe :wide-20, :text-over-budget false, :files 20, :budget 32640, :payload-omitted nil, :structured-over-budget false, :text-chars 23285}
{:missing-from-text 0, :sample-missing [], :error-type nil, :payload-truncated true, :error-truncated nil, :elided-note-present false, :receipt-reduced nil, :facts-elided-stated nil, :short-leaves 433, :receipt-omitted-fields nil, :structured-json-bytes 18718, :leaves 433, :ok true, :probe :wide-40, :text-over-budget false, :files 40, :budget 32640, :payload-omitted {:files 25}, :structured-over-budget false, :text-chars 29298}
{:missing-from-text 0, :sample-missing [], :error-type nil, :payload-truncated true, :error-truncated nil, :elided-note-present false, :receipt-reduced nil, :facts-elided-stated nil, :short-leaves 363, :receipt-omitted-fields nil, :structured-json-bytes 20414, :leaves 363, :ok true, :probe :wide-60, :text-over-budget false, :files 60, :budget 32640, :payload-omitted {:files 55}, :structured-over-budget false, :text-chars 32188}
{:missing-from-text 0, :sample-missing [], :error-type nil, :payload-truncated true, :error-truncated nil, :elided-note-present false, :receipt-reduced true, :facts-elided-stated nil, :short-leaves 254, :receipt-omitted-fields ["hashes"], :structured-json-bytes 13009, :leaves 254, :ok true, :probe :wide-100, :text-over-budget false, :files 100, :budget 32640, :payload-omitted {:files 100}, :structured-over-budget false, :text-chars 28029}
{:missing-from-text 0, :sample-missing [], :error-type :next-call-exceeds-public-budget, :payload-truncated nil, :error-truncated nil, :elided-note-present false, :receipt-reduced nil, :facts-elided-stated nil, :short-leaves 34, :receipt-omitted-fields nil, :structured-json-bytes 1200, :leaves 35, :ok false, :probe :deep-60, :text-over-budget false, :budget 32640, :payload-omitted nil, :structured-over-budget false, :text-chars 1784}
PROBE-A-DONE
```

`:missing-from-text 0` at every width. Round four measured 68 missing at 20 files and 396 at 40;
both are now zero, and the four stranded fields (`source-unchanged`, `pre_image_binding`,
`lock_scope`, `mutation_attempted`) are in the pinned head. Neither face exceeds the budget at any
of the four. The structure is what gives ground, exactly as claimed and in the right order:
`payload_omitted {:files 25}` at 40, `{:files 55}` at 60, and at 100 the map-valued `hashes` is
dropped whole and **named** in `receipt_omitted_fields`.

The `:deep-60` line is round five's own newly-found defect — a 60-file preview under 190-character
directory names, which the builder measured at 125,104 bytes with `:ok true` before the fix. It is
now bounded. (It is bounded by turning an `:ok true` preview into a refusal, which I take up in
finding 5.)

**MCP-OP-ADMIT-136 is genuinely closed.** This was round four's blocker and it is the one thing I
tried hardest to break.

---

## 3. PASS — MCP-OP-ADMIT-137: the guard is now at least as wide as the renderer, and I could not find a falsey `:ok` it misses

`checked-refusal-kind!` (`src/clj_surgeon/mcp_admit_tool.clj:268-272`) fires on
`(not (true? (:ok receipt)))`, and the `:once` execution recorder
(`test/clj_surgeon/admit_patch_test.clj:5349-5353`) was moved to the identical predicate, so the
recorder cannot see a narrower refusal set than the guard checks. I planted an unenumerated kind and
walked `:ok` across the falsey values:

```text
{:probe :guard-predicate, :ok false, :guard :threw, :renderer-calls-it :refusal}
{:probe :guard-predicate, :ok nil, :guard :threw, :renderer-calls-it :refusal}
{:probe :guard-predicate, :ok "false", :guard :threw, :renderer-calls-it :success}
{:probe :guard-predicate, :ok 0, :guard :threw, :renderer-calls-it :success}
{:probe :guard-predicate, :ok true, :guard :PASSED, :renderer-calls-it :success}
{:probe :guard-predicate, :ok :no, :guard :threw, :renderer-calls-it :success}
```

Round four's escape (`:ok nil` rendered as a refusal, never checked) throws now. Note the two
predicates are **not identical** — `"false"`, `0` and `:no` are truthy to the renderer and are still
checked by the guard — but the relation runs the safe way: everything the renderer calls a refusal is
checked, and the extra strictness costs a `true`-valued receipt nothing. The builder's own witness
(`:5625-5648`) asserts exactly that implication (`(or (not refusal-text?) checked?)`) rather than
equality, so the EARS phrase "shall be the SAME" is stated more strongly than the code or the witness
delivers. That is a wording defect, not a hole: I could not construct an `:ok` value the renderer
shows as a refusal and the guard skips. **Closed.**


---

## 4. Advisories that should ride the same fix

**4a. `reduce-receipt-to-budget` drops the receipt's own truncation notice as if it were bulk.**
Its droppable set is "everything that is not an identity key and not a reduction key"
(`src/clj_surgeon/mcp_admit_tool.clj:2074-2077`), and `:payload_truncated`, `:payload_omitted`,
`:payload_omitted_bytes` and `:payload_truncation` are in neither list. Measured in finding 1c:
`:omitted ["payload_truncation" "payload_omitted_bytes" "payload_truncated" "payload_omitted"]`. A
receipt that jettisons the field telling the reader it was cut, in order to make room, is losing the
only annotation that makes the cut honest. Those four belong in `receipt-reduction-keys`.

**4b. `bound-public-payload`'s reported `payload_omitted_bytes` no longer measures what forced the
trim.** `src/clj_surgeon/mcp_write_refusal.clj:265-289`: `content` and `original-bytes` are still
JSON byte counts, while the loop's exit condition is now the caller's `fits?` — which, for the admit
gate, can be the TEXT face. So a payload trimmed because its text face did not fit reports a byte
figure that was never the binding constraint. Not wrong, but it answers a question the reader is not
asking; the record should name which face forced it.

**4c. `MCP-OP-ADMIT-137`'s EARS text overclaims relative to its own witness.** "The predicate … shall
be the SAME on both of its faces" — they are not the same; the guard is strictly wider (finding 3).
The witness correctly asserts the implication rather than equality. Say `at least as wide as` in the
EARS text so the ratchet and the sentence agree; today a future reader who makes them literally equal
would be weakening the guard while satisfying the spec.

**4d. `MCP-OP-ADMIT-122`'s witness was weakened, and the product behaviour behind it changed.**
`test/clj_surgeon/admit_patch_test.clj:3765-3768` now asserts `(= 21 (+ (count (:files result))
(get-in result [:payload_omitted :files] 0)))` where it previously asserted the carried count. **My
ruling: acceptable, and correctly declared — not blocking.** The accounting is exact, the omission is
named, and the alternative is precisely the defect §136 closed (carry 21 in structuredContent and
name 13 of them in the text). But it is a real regression in what an ordinary caller receives from an
ordinary 21-file `clj-kondo` fan-out (the builder reports 13 carried plus 8 accounted; I verified the
witness change that permits it, not that split), and it is invisible in the EARS text for 122. It should be stated there, and the receipt should offer a way to get the rest (a `next_call`
that pages the omitted files would cost nothing and is the natural home for it).

**4e. The `MCP-OP-ADMIT-138` exemption is proved by a substring of source text, not by execution.**
`test/clj_surgeon/admit_patch_test.clj:5314-5329` checks that the battery script FILE mentions the
kind's name and that the Makefile contains the target. Neither is evidence that the battery drives
that kind — a comment satisfies both. The set-equality assertion is what actually holds the line
today (any kind the fast suite drives cannot be excused), so the exemption is not currently
forgeable in one step; but the check as written is marker-presence, not a record of execution. The
honest version is to have the battery emit a machine-readable receipt naming the kinds it actually
produced, and have the suite read that.

**4f. `NAME_MAX` is hardcoded at 255 in the witness rather than read from the filesystem.**
`test/clj_surgeon/admit_patch_test.clj` pins `name-max 255` and derives the at-bound stem from it.
That is an ambient precondition: the same commit will go red on any filesystem with a different
`NAME_MAX` (eCryptfs is 143), for a reason that has nothing to do with the gate. Read it with
`pathconf`/`getconf` on the actual temp directory, or name the precondition in the test's message.


**4g. The fact section's elision note has a ~191-character floor it can exceed its own budget to
print.** Driven directly at shrinking budgets:

```text
{:probe :fact-elision, :budget 1200, :rendered-chars 1196, :within-budget true, :stated-elided 47, :paths-named 46, :unnamed-note false}
{:probe :fact-elision, :budget 600, :rendered-chars 595, :within-budget true, :stated-elided 63, :paths-named 32, :unnamed-note true}
{:probe :fact-elision, :budget 300, :rendered-chars 297, :within-budget true, :stated-elided 63, :paths-named 7, :unnamed-note true}
{:probe :fact-elision, :budget 150, :rendered-chars 191, :within-budget false, :stated-elided 63, :paths-named 0, :unnamed-note true}
{:probe :fact-elision, :budget 80, :rendered-chars 191, :within-budget false, :stated-elided 63, :paths-named 0, :unnamed-note true}
```

The elided count is **exact at every step** (63) and the names shrink gracefully with a
`path(s) not named here` tail — clause (3) and (4) of the docstring hold, and this is the behaviour
MCP-OP-ADMIT-136 asks for. But below roughly 191 characters of remainder the section renders longer
than the budget it was handed, because the note that announces the elision is itself unelidable. Not
reachable at 32,640 today (the head and `next_call` cannot plausibly consume 32,450), and it is the
same shape as finding 1 — a bound announced by a thing that is not charged against it — so it should
be closed by the same fix rather than separately.

---

## 5. PASS with a ruling — MCP-OP-ADMIT-133: the NAME_MAX disagreement is genuinely explained, and I reproduce both sides

Round four's advisory 5c reported that a "300-character basename" creation target previewed clean,
contradicting the not-reachable excuse. Both readings were right about different inputs: `NAME_MAX`
is 255 and the kernel applies it to the whole file **name**, so a 300-character *stem* is a
304-character name. I drove it myself, three lengths, both modes:

```text
{:probe :name-max, :stem 251, :file-name-chars 255, :mode "preview", :ok true, :error-type nil, :enumerated false}
{:probe :name-max, :stem 251, :file-name-chars 255, :mode "commit", :ok false, :error-type :verification-incomplete, :enumerated true}
{:probe :name-max, :stem 252, :file-name-chars 256, :mode "preview", :ok false, :error-type :admit-tool-failure, :enumerated true}
{:probe :name-max, :stem 252, :file-name-chars 256, :mode "commit", :ok false, :error-type :admit-tool-failure, :enumerated true}
{:probe :name-max, :stem 300, :file-name-chars 304, :mode "preview", :ok false, :error-type :admit-tool-failure, :enumerated true}
{:probe :name-max, :stem 300, :file-name-chars 304, :mode "commit", :ok false, :error-type :admit-tool-failure, :enumerated true}
```

At 255 it is created; at 256 it refuses in both modes; the round-four measurement
(`:verification-incomplete` on commit) reproduces exactly at the 255 row, which is the input round
four actually used. The witness now asserts behaviour AT the bound rather than at either side's
example, and the disagreement is recorded instead of one side being deleted. That is the right way
to close a contested measurement.

**Ruling on the standing typed-path-refusal gap: acceptable-declared, NOT blocking.** A 256-character
file name refuses under `:admit-tool-failure` where a typed path refusal belongs. But the claim this
gate is about is that every kind the entrance publishes is enumerated, and `:admit-tool-failure` is
enumerated. The imprecise typing is a real defect, it is filed, and it is not papered over in the
justification text. It should not hold the enumeration ratchet hostage. Same ruling as round four,
now on a reproduced measurement rather than a contradicted one.

---

## 6. PASS — MCP-OP-ADMIT-138: the busy-spin race is out of the fast gate and the battery is deterministic here

`test/admit_transaction_recovery_battery.clj` is new, is reachable only as
`make admit-transaction-recovery-battery`, and is wired into no suite (I confirmed `test`,
`test-fast` and `mcp-test` do not depend on it in the Makefile, and the `:once` set-equality
assertion now subtracts `battery-only-refusal-kinds`). The fast suite contains no busy-spinning
watcher thread against a 64-file write, and the enumeration proof can no longer go red naming an
unrelated cause.

I ran the battery three times (brief asked for three; round four ran the old in-suite fixture
twenty). All 27 arm-attempts landed first try:

```text
### BATTERY RUN 1
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=259
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=235
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=299
admit-transaction-recovery-battery: 3/3 arms passed
### BATTERY RUN 2
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=248
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=226
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=216
admit-transaction-recovery-battery: 3/3 arms passed
### BATTERY RUN 3
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=187
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=200
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=266
admit-transaction-recovery-battery: 3/3 arms passed
```

9/9 arms, 3/3 runs, at load average ~16 on a 16-core box. **My round-four ruling ("not blocking at
this evidence, but it must not stay here") is discharged as asked.** The house rule — a resource or
timing bound is a battery, not a fast-suite witness — is now obeyed. Advisory 4e is the residue: the
exemption's evidence check is a substring of the battery script, not a record of what the battery
executed.


---

## 7. Every named gate reproduces at the tip

All commands run from `/home/forge/tmp/sol/gate4-wt` at `d8cb9a04`, JVM suites through
`~/bin/suite-run`, at load average ~16 on a 16-core box.

```text
$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 748 tests containing 10312 assertions.
0 failures, 0 errors.
EXIT=0
```

```text
$ ~/bin/suite-run bb test/run_all.clj
Ran 814 tests containing 6724 assertions.
0 failures, 0 errors.
BB_EXIT=0
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
{:ok true, :specs 359, :violations 0}
```

```text
$ make admit-analyzer-memory-self-test
PASS n=100 findings=600 analyzer-bytes=83606 ran=true introduced=300 heap-start-MiB=24 heap-peak-MiB=35 budget-MiB=409 max-heap-MiB=512 wall-ms=106
PASS n=1000 findings=6000 analyzer-bytes=847706 ran=true introduced=3000 heap-start-MiB=23 heap-peak-MiB=78 budget-MiB=409 max-heap-MiB=512 wall-ms=382
PASS n=10000 findings=60000 analyzer-bytes=8596706 ran=true introduced=30000 heap-start-MiB=23 heap-peak-MiB=88 budget-MiB=409 max-heap-MiB=512 wall-ms=2096
admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m
MEM_EXIT=0
```

`make admit-transaction-recovery-battery` — 3/3 arms, three consecutive runs, quoted in finding 6.

Every claimed figure matches: mcp-test **748/10312/0**, bb **814/6724/0**, oracle pass, intent audit
**359 specs / 0 violations**, analyzer **3/3 at -Xmx512m**, battery **3/3**.

`:receipt-exceeds-public-budget` is genuinely deleted from the enumeration — `rg` across `src/`,
`test/` and `docs/` finds exactly one occurrence, inside a docstring narrating why it was removed
(`src/clj_surgeon/mcp_admit_tool.clj:2005`).


---

## 8. Every sabotage figure executed, and every sabotage patch confirmed applied

The builder's own honest negative — a first sabotage patch silently no-op'd and the export ran green
— is exactly why each patch below was verified to have landed before its suite was run. All work on
fresh `git archive` exports of `d8cb9a04` under `/var/tmp/forge/gate5-review-fx/opus/sab/`; nothing
in the clone was touched. Where the fix could be reverted cleanly I reverted the GREEN's `src` diff
(`git apply -R --check` then `git apply -R`, printing `REVERT-APPLIED ok`); where later commits had
moved the code I wrote a targeted re-introduction of the exact defect and asserted the substitution
count was 1 before writing the file.

| # | sabotage (independently constructed) | applied? | result |
|---|---|---|---|
| 136 | reintroduce a fixed half of `public-byte-budget` as the fact section's share | `136 sabotage applied`, `grep` confirms line 2521 | mcp-test **959 failures** / 748 tests / 10681 assertions |
| 137 | `git apply -R` the guard fix (`not true?` → `false?`) | `REVERT-APPLIED ok` | mcp-test **4 failures** / 748 tests / 10309 assertions |
| 138 | delete the `battery-only-refusal-kinds` exemption | `138 sabotage: exemption removed` | admit-patch-test **2 failures** / 151 / 3993 |
| 138b | excuse a kind the fast suite DOES drive (`:invalid-patch`) to the battery | `138b sabotage: a driven kind excused` | admit-patch-test **2 failures** / 151 / 4003 |
| 139a | decide the oversize refusal on the call's CHARACTERS again, as round four did | `139a sabotage applied`, `grep` confirms line 1970 | admit-patch-test **5 failures, 1 error** / 151 / 3998 |
| 139b | `git apply -R` the reduction fix | `REVERT-APPLIED ok` | admit-patch-test **9 failures, 1 error** / 151 / 3978 |

**139a reproduces the brief's figure exactly — 5 failures and 1 error** — and my patch was written
without sight of the builder's. The 136 and 137 numbers differ from the brief's (959 vs 748; 4 vs 2)
because I ran the whole `mcp-test` suite rather than `admit-patch-test` alone and because my
re-introduction of the 136 defect is my own, not the builder's; the point each control establishes —
that removing the fix makes the suite loudly, unambiguously red — holds in every row.

**138's failure is instructive and is the evidence for advisory 4e.** The assertion that caught 138b
was, verbatim from the run:

```text
actual: (not (str/includes? ";; @spec MCP-OP-ADMIT-138\n;;\n;; The `transaction-recovery-required` proof, as a battery target.\n;;\n…" "invalid-patch"))
```

— a substring search of the battery script's source text. It is a real check and it fired, but what
it proves is that the file mentions the kind, not that the battery produces it.

**And the battery's own sabotage figure reproduces exactly.** Running
`make admit-transaction-recovery-battery`'s script against the 139b export (reduction reverted):

```text
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=170
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=166
FAIL n=64 attempts=3 kind=:receipt-exceeds-public-budget source-unchanged=false enumerated=true wall-ms=128
admit-transaction-recovery-battery: 2/3 arms passed
```

**2/3, red at n=64, and the kind it reports is `:receipt-exceeds-public-budget`** — the exact
relabelling the builder's commit message says the battery caught. This is the single best piece of
evidence in the whole branch: the battery target created in §138 immediately earned its keep by
catching a safety-critical defect in §139 that no fast-suite fixture saw. It is also, precisely, the
defect finding 1d shows still lives on the `next_call` arm.

---

## 9. Every RED is red at its own sha for its stated reason, and every GREEN is green

`clj-surgeon.admit-patch-test` alone, on a fresh `git archive` export of each sha.

```text
### 5d24a791 :: 472 failures, 0 errors.    (RED §136)   {:test 145, :pass 3760, :fail 472}
### 905d55d4 ::   0 failures, 0 errors.    (GREEN §136) {:test 146, :pass 3875}
### b0131793 ::   2 failures, 0 errors.    (RED §137)   {:test 147, :pass 3882}
### 82585050 ::   0 failures, 0 errors.    (GREEN §137) {:test 147, :pass 3884}
### 5e8e278f ::   2 failures, 0 errors.    (RED §138)   {:test 146, :pass 3879}
### c53ce759 ::   0 failures, 0 errors.    (GREEN §138) {:test 147, :pass 3887}
### 92c9e837 ::   3 failures, 0 errors.    (RED §133)   {:test 148, :pass 3890}
### 45734371 ::   0 failures, 0 errors.    (GREEN §133) {:test 148, :pass 3896}
### f80c85d8 ::   3 failures, 0 errors.    (RED §139)   {:test 149, :pass 3902}
### 64878e92 ::   0 failures, 0 errors.    (GREEN §139) {:test 150, :pass 3928}
### d8cb9a04 ::   0 failures, 0 errors.    (TIP)        {:test 151, :pass 3998}
```

**Every RED count matches the builder's own commit message exactly** — 472/145/4232 at `5d24a791`,
2 at `b0131793`, 2 at `5e8e278f`, 3 at `92c9e837`, 3 at `f80c85d8` — and the tip is **151 tests /
3998 assertions / 0 failures**, exactly the claimed figure. The RED→GREEN discipline on this branch
is genuine and I found nothing staged about it.

A note on method: `~/bin/suite-run`'s three lanes were saturated for most of this review by other
seats (twelve jobs queued on `suite-2.lock` at once), so the per-sha runs above were executed
directly, strictly one JVM at a time — the same concurrency a single lane would have granted, at a
box load average of 12–16 throughout.

---

## 10. Cleanup

```text
$ ls -A /var/tmp/forge/gate5-review-fx/opus 2>&1; echo LS_EXIT=$?
ls: cannot access '/var/tmp/forge/gate5-review-fx/opus': No such file or directory
LS_EXIT=2

$ ss -ltnp | awk '$4 ~ /:(8156|8157|8158)$/'
(no output — no listener on 8156-8158; no Surgeon server was started for this review)

$ cd /home/forge/tmp/sol/gate4-wt && git rev-parse HEAD && git status --porcelain
d8cb9a0452be704203e24ebeee3e9836e4ede7ca
(`git status --porcelain` produced no output — the tree is clean; nothing was committed, pushed,
stashed or staged, and the working tree is byte-identical to the tip)
```

---

## Mergeability

```text
$ git merge-tree --write-tree HEAD origin/MCP/main
c4bf1013f23e871ffe348acfd4e4f2ad8195c6b7
100644 38494a98e70b30f7634b3be3ba94878fb2dda94c 1	Makefile
100644 3d276e3a29cc5055fbcf2e9dab56ecbc0ee9d26f 2	Makefile
100644 2270c7df068c2f647285d45833d10679afd73edf 3	Makefile

Auto-merging Makefile
CONFLICT (content): Merge conflict in Makefile
MERGE_EXIT=1
```

`origin/MCP/main` is at **`e2b623077e7a4c776c22d47537fa616202331ec3`** (the trunk sha; the branch has
advanced past the `6c54089e` the round-four brief named). **Exactly one path conflicts — `Makefile` —
and I confirmed by diffing all three stages that the conflict is the single `.PHONY` line and nothing
else:** HEAD appends `admit-transaction-recovery-battery` to it, trunk appends `fanout-selftests` to
it, and each side's other hunk (HEAD's new target near line 211, trunk's new `fanout-selftests` recipe
near line 1009) auto-merges. The builder's report is accurate; keep both words on the `.PHONY` line.

---

## NO-GO

`d8cb9a04` merges into `origin/MCP/main` at `e2b62307` with exactly one trivial keep-both conflict on
the `.PHONY` line, and it closes round four's blocker and three of its four advisories with real,
sabotage-verified witnesses — but it does not land, because MCP-OP-ADMIT-139's own universal
sentence is false at the public MCP handler: a 60,000-character `mode` argument publishes a
61,214-byte receipt, 28,574 bytes past the 32,640 its own refusal text calls the budget, unannotated
and blaming a 389-character `next_call`, and the same unbounded path drops a `mutation_attempted
true` to `false` on a `transaction-recovery-required` refusal.
