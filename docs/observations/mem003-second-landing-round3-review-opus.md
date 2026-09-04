## NO-GO

Round-three review of `bridge/integration-2026-09-03-mem003` at `3692e9b`, taken with Sol's
round-two brief after Sol's content filter refused it. Clone
`/home/forge/tmp/sol/mem003r2-wt`, proven clean at the tip; no commit, push, stash or
`git add`; no server started; fixtures under `/home/forge/tmp/mem003r3-review-fx`, removed.

```sh
git rev-parse HEAD; git status --short; git diff --exit-code; printf '[exit=%d]\n' "$?"
```

```text
3692e9baf49ac4fa4fe97dd85c41affe33491344
[exit=0]
```

**Headline.** Three of Sol's four blocking findings are genuinely closed and I reproduced each
closure. The fourth — §1 — is **not** closed. The round-three record's central claim, *"An
undeclared measured field cannot be CONSTRUCTED from a sanctioned clock read"*, is false at this
tip, and I reproduced Sol's exact counterexample twice by two independent routes, each requiring
no raw clock read, no `measured/value` call, and no allow-list entry. The public result and the
parity subject carry an undeclared clock-derived field, the diagnostic reports none, and every
invariant assertion passes. That is the same defect Sol blocked on, unchanged in effect.

---

## 1. BLOCKING — §1 is not closed. The tag rides the value, but nothing owns the tag's removal, and the source scan does not see the sanctioned verbs when they are `:refer`red.

The design is right and the raw-clock route really is shut. What is not shut is the *sanctioned*
route, and the round-3 record rests its whole argument on that route being impossible.

The mechanism: `reading` is a **plain one-key map**, `src/clj_surgeon/measured.clj:129-132`, and
`value` is documented as *"THE ONE LAUNDERING VERB. Every call site in `src/` is named in the
invariant witness's allow-list"* (`measured.clj:148-154`). Both statements are untrue of a map. A
keyword lookup opens it, and the witness's escape-hatch pattern
(`test/clj_surgeon/measured_invariant_test.clj:99-102`) matches only
`measured/raw-nanos|measured/raw-ms|measured/value`.

### 1a. Laundering with the reading's own key — Sol's §1 counterexample, verbatim in effect

Scratch archive of `3692e9b`, one line changed at `src/clj_surgeon/mcp_hot_verify.clj`:

```diff
--- /home/forge/tmp/sol/mem003r2-wt/src/clj_surgeon/mcp_hot_verify.clj
+++ /home/forge/tmp/mem003r3-review-fx/plantP2/src/clj_surgeon/mcp_hot_verify.clj
@@ -111,4 +111,7 @@
                :error-type (or (:error-type (ex-data error))
                                :hot-verification-connection-failed)
                :error (.getMessage error)
-               :elapsed_ms (measured/elapsed-ms started)})))))))
+               :elapsed_ms (:clj-surgeon.measured/reading
+                             (measured/elapsed-ms started))
+               :verification_wall_ms (:clj-surgeon.measured/reading
+                                       (measured/elapsed-ms started))})))))))
```

Exact command, run from that archive:

```sh
~/bin/suite-run clojure -Sdeps '{:paths ["src" "test"] :deps {org.clojure/clojure {:mvn/version "1.12.1"} cheshire/cheshire {:mvn/version "5.13.0"} org.babashka/sci {:mvn/version "0.10.47"} rewrite-clj/rewrite-clj {:mvn/version "1.2.50"} nrepl/nrepl {:mvn/version "1.3.1"}}}' -M -e "
(require '[clojure.test :as t] '[clj-surgeon.measured-invariant-test :as inv] '[clj-surgeon.mcp-hot-verify :as hot] '[clj-surgeon.mcp-operation :as op] '[clj-surgeon.measured :as measured])
(let [raw (hot/verify! \".\" {:port-file \"definitely-missing-port\" :reload [] :tests []})
      seen (atom nil) ticks (atom [0 1000000])]
  (op/invoke! {:clock-nanos #(let [x (first @ticks)] (swap! ticks subvec 1) x)
               :execute (constantly raw) :summarize (constantly \"ok\") :serialize pr-str
               :callback (fn [_ _ r] (reset! seen r))})
  (prn {:public-result @seen})
  (prn {:undeclared-field (:verification_wall_ms @seen)
        :hashed-field (:verification_wall_ms (measured/hashed-channel @seen))
        :unpartitioned (vec (measured/unpartitioned-measured-paths @seen))}))
(t/run-tests 'clj-surgeon.measured-invariant-test)"
```

Verbatim output:

```text
{:public-result {:ok false, :status :failed, :error-type :hot-verification-connection-failed, :error "/home/forge/tmp/mem003r3-review-fx/plantP2/definitely-missing-port (No such file or directory)", :verification_wall_ms 7.049342, :measured {:elapsed_ms 1.0}}}
{:undeclared-field 7.049342, :hashed-field 7.049342, :unpartitioned []}

Testing clj-surgeon.measured-invariant-test

Ran 9 tests containing 21 assertions.
0 failures, 0 errors.
{:test 9, :pass 21, :fail 0, :error 0, :type :summary}
```

This is Sol's round-two output reproduced line for line at the tip that claims to have closed it:
the undeclared clock field is inside the hash subject, the diagnostic reports none, and every
invariant test passes. No clock-read count changed, and no allow-list entry was needed.

### 1b. `:refer` — a brand-new namespace reads the clock raw and the scanner reports nothing

`src/clj_surgeon/planted_refer.clj`, planted into a scratch archive of `3692e9b`:

```clojure
(ns clj-surgeon.planted-refer
  (:require
   [clj-surgeon.measured :refer [raw-nanos]]))

(defn publish-an-undeclared-clock-field
  [started]
  {:ok false
   :verification_wall_ms (/ (double (- (raw-nanos) started)) 1000000.0)})
```

Exact command:

```sh
~/bin/suite-run bb -e "
(require '[clojure.test :as t] '[clj-surgeon.measured-invariant-test :as inv] '[clj-surgeon.planted-refer :as p] '[clj-surgeon.mcp-operation :as op] '[clj-surgeon.measured :as measured])
(prn {:scan-of-planted-file (into {} (filter (fn [[k _]] (re-find #\"planted\" (first k))) (@#'clj-surgeon.measured-invariant-test/scan @#'clj-surgeon.measured-invariant-test/clock-pattern)))})
(let [seen (atom nil) ticks (atom [0 1000000])]
  (op/invoke! {:clock-nanos #(let [x (first @ticks)] (swap! ticks subvec 1) x)
               :execute (constantly (p/publish-an-undeclared-clock-field (System/nanoTime)))
               :summarize (constantly \"ok\") :serialize pr-str :callback (fn [_ _ r] (reset! seen r))})
  (prn {:public @seen :hashed (measured/hashed-channel @seen) :unpartitioned (vec (measured/unpartitioned-measured-paths @seen))}))
(t/run-tests 'clj-surgeon.measured-invariant-test)"
```

Verbatim output:

```text
{:scan-of-planted-file {}}
{:public {:ok false, :verification_wall_ms 0.003434, :measured {:elapsed_ms 1.0}}, :hashed {:ok false, :verification_wall_ms 0.003434}, :unpartitioned []}

Testing clj-surgeon.measured-invariant-test

Ran 9 tests containing 21 assertions.
0 failures, 0 errors.
{:test 9, :pass 21, :fail 0, :error 0, :type :summary}
```

`the-measured-namespace-is-never-aliased-to-another-name`
(`measured_invariant_test.clj:256-268`) matches `clj-surgeon\.measured :as ([a-z-]+)` only. It has
nothing to say about `:refer`, and `(:require [clj-surgeon.measured :as measured :refer [raw-nanos]])`
satisfies it while defeating both scans.

### 1c. What DOES hold — the raw-API route is genuinely closed

Sol's own §1 diff, replanted at `3692e9b` (raw `System/nanoTime` under two names):

```sh
~/bin/suite-run bb -e "(require '[clojure.test :as t] 'clj-surgeon.measured-invariant-test) (t/run-tests 'clj-surgeon.measured-invariant-test)"
```

```text
FAIL in (no-raw-clock-read-lives-outside-the-measured-namespace) (measured_invariant_test.clj:226)
raw clock reads with no allow-list entry: (["src/clj_surgeon/mcp_hot_verify.clj" "verify!"]) ; allow-listed sites that no longer exist: ()
...
2 failures, 0 errors.
```

The scanner names the site exactly. Credit where due: the raw route, which is what Sol actually
planted, is shut.

### 1d. The allow-list is prose, not a control

`clock-allow-list` (`measured_invariant_test.clj:108-194`) carries 33 `:control` entries whose
`:why` asserts the value is never published. The test checks only that the *channel keyword* is
`:control` (`:237-240`). Nothing binds a control read to non-publication, so any of those 33 forms
can be edited to publish its raw value under an undeclared name with the counts unchanged — the
same class as 1a, from a different starting point. `txn_journal/lock-broken-line` already puts its
`Instant/now` into what the code itself calls "the typed receipt line a broken lock leaves behind"
(as a string, so harmless today).

### What would close it

Either make the reading **intrinsically typed** — a `deftype`/`defrecord` whose contents no
keyword lookup opens, so `value` really is the one door — or bind every published clock value to a
declared output name, which is the alternative Sol named. Additionally: forbid `:refer` on
`clj-surgeon.measured` in the alias witness, and scan for the bare verb names, not just the
`measured/`-qualified spellings.

---

## 2. CLOSED — §2, one boundary means one boundary (with one named hardening gap)

Sol's exact command at `3692e9b`:

```sh
~/bin/suite-run clojure -Sdeps '{:deps {io.modelcontextprotocol.sdk/mcp {:mvn/version "0.17.2"} io.github.bhauman/clojure-mcp {:git/tag "v0.2.6" :git/sha "35a660b"} org.eclipse.jetty.ee10/jetty-ee10-servlet {:mvn/version "12.0.13"} org.slf4j/slf4j-nop {:mvn/version "2.0.17"} nrepl/nrepl {:mvn/version "1.3.1"}}}' -M -e "
(require '[clj-surgeon.mcp-server :as server])
(let [spec (server/create-structured-async-tool {:name \"boom\" ... :tool-fn (fn [_ _ _] (throw (ex-info \"boom\" {})))})
      result (.block (.apply (.call spec) nil {})) structured (.structuredContent result)]
  (prn {:is-error (.isError result) :structured-content structured :has-measured (contains? structured \"measured\")}))"
```

```text
{:is-error true, :structured-content {"measured" {"elapsed_ms" 0.711296}, "error_type" "mcp-adapter-failure", "ok" false, "error" "boom", "operation" "boom"}, :has-measured true}
```

The adapter catch now routes through `mcp-operation/finalize-failure`
(`src/clj_surgeon/mcp_server.clj:154-172`, `src/clj_surgeon/mcp_operation.clj:64-71`). All five
registered tools are `:structured? true` with `measured.elapsed_ms` required and no top-level
`elapsed_ms`:

```text
{:name "inspect_clojure", :structured? true, :measured-required ["elapsed_ms"], :top-level-elapsed nil, :required ["ok" "operation" "measured"]}
{:name "apply_clojure_changes", ...} {:name "edit_clojure", ...} {:name "transform_clojure", ...} {:name "alias_migration", ...}
```

**The witness does fail on a planted publish site.** I added `planted-helper-publish` (calling
`structured-call-result`) and `planted-direct-publish` (building `McpSchema$CallToolResult/builder`
by hand) to `mcp_server.clj` in a scratch archive:

```text
undeclared public-result publish sites: (["src/clj_surgeon/mcp_server.clj" "planted-helper-publish"]) ; declared sites that no longer exist: ()
Ran 7 tests containing 15 assertions. 2 failures, 0 errors.
```

**Hardening gap (non-blocking):** `planted-direct-publish` was invisible. `publish-sites`
(`test/clj_surgeon/measured_provenance_test.clj:161-181`) greps for the literal
`(structured-call-result`, so a publish site that builds the SDK result directly — which is exactly
what `structured-call-result` itself does at `mcp_server.clj:109-115` — is not enumerated. No such
site exists today; the docstring's promise ("a NEW publish site fails this test") is narrower than
it reads. Same shape weakness as §1's scanner.

---

## 3. CLOSED for the clj-surgeon wire — §3, the silent zero. Two residuals named.

Equal one-event fixtures differing only in result shape:

```sh
~/bin/suite-run bb bench/event_timing.clj summarize /home/forge/tmp/mem003r3-review-fx/event-current.jsonl /home/forge/tmp/mem003r3-review-fx/event-current.tsv
~/bin/suite-run bb bench/event_timing.clj summarize /home/forge/tmp/mem003r3-review-fx/event-legacy.jsonl  /home/forge/tmp/mem003r3-review-fx/event-legacy.tsv
~/bin/suite-run bb bench/event_timing.clj --self-test
```

```text
CURRENT: {... :server-authoritative-elapsed-ms 12.5 ...}
LEGACY:  benchmark event timing failed: MCP result carries a top-level elapsed_ms; the server clock lives in measured.elapsed_ms (MCP-OP-TIME-005)
         {:item-id "m", :tool "inspect_clojure", :legacy-elapsed-ms 12.5}
SELFTEST: benchmark event timing self-test passed
```

Sol's inversion is complete: the current shape reads, the old shape refuses loudly. The self-test
carries the current fixture and asserts the refusal, and `bb test/run_all.clj` runs it through
`measured-provenance-test`.

**Residual (a), a producer the sweep table missed.**
`dev/experiments/owner_aware_call_capture_server.clj:48` is a live MCP tool handler — it replaces
`:tool-fn` on the registered `edit_clojure` tool — that publishes

```clojure
:elapsed_ms (/ (- (System/nanoTime) started) 1000000.0)
```

directly to the SDK callback, bypassing `mcp-operation/invoke!` entirely. `dev/experiments` is on
the `:clj-surgeon/mcp-test` classpath (`deps.edn:31`) but outside `src/`, so neither the clock scan
nor the publish-site scan reaches it. Its results are now invalid against every canonical output
schema, and the branch's own record claims *"Every other in-repository `elapsed_ms` reader was
swept and none is stale"* — the table has five rows and no `dev/` row.

**Residual (b), the loud refusal is file-fatal.** `fail!` throws
(`bench/event_timing.clj:16-17`), so one legacy event aborts the whole summarize. A mixed corpus of
three events (current, legacy, current) yields nothing at all:

```text
benchmark event timing failed: MCP result carries a top-level elapsed_ms; the server clock lives in measured.elapsed_ms (MCP-OP-TIME-005)
{:item-id "m2", :tool "inspect_clojure", :legacy-elapsed-ms 12.5}
```

Every arm-run event corpus recorded before this branch — and every future run through the capture
server in residual (a) — is now unsummarizable rather than partially summarizable. Loud beats
silent, agreed; a typed per-item refusal that still summarizes the rest would beat both.

**Not stale, confirming the record and Sol:** `dev/experiments/formatter_process_canary.clj:61`
reads `(:elapsed_ms result)` off `mcp-tool/execute-request!` — but that domain function never set a
top-level `elapsed_ms`, at `dd9d8b9` or now (`git show dd9d8b9:src/clj_surgeon/mcp_tool.clj | rg ':elapsed_ms'`
shows only the `measured-field` reads and the `0.0` byte-count normalizer). A pre-existing dead
read, not a regression from this branch.

**The usage collector is unmoved.** `skills/` is untouched in `dd9d8b9..3692e9b`
(`git diff --name-only dd9d8b9..3692e9b -- skills/` is empty), `mcp_telemetry.clj` likewise, and
`mcp_tool/record-result!` still writes a bare `total_ms`. `make study-agent-usage` on this seat's
telemetry root:

```text
"tools": {"inspect_clojure": 147}
"outcomes": {"ok": 100, "refused": 47}
```

147 / 100 / 47 — the stated figures exactly. The receipt shape change moves no figure.

---

## 4. CLOSED for the three named files — §4. The next disagreement is one directory over.

The seven round-two lines are gone and the witness is green:

```sh
rg -n 'shared finalizer records elapsed_ms|Associates authoritative|top-level public request clock|output schema declares|single publication choke|adds elapsed_ms only' docs/intent/mcp-operation-contract/mcp-operation-contract-design.md
rg -n 'Every public MCP operation returns.*elapsed_ms' docs/high-level-design.md
for f in docs/high-level-design.md docs/intent/mcp-operation-contract/mcp-operation-contract-design.md docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md; do rg -n 'elapsed_ms' $f | rg -v 'measured'; done
```

```text
126:Every public output schema declares `measured.elapsed_ms` as a required number
296:- the outcome reaches the single publication choke point and finalizer ;
1001:- Every public MCP operation returns a finite, non-negative `measured.elapsed_ms`
[no unqualified elapsed_ms line in any of the three files]
```

**The next disagreement, as asked.** It is not in a schema JSON (no `.json`/`.edn`/`.pl` artifact
in the repo mentions `elapsed_ms`). It is in two *other ratified intent lanes* and in copyable wire
examples, none of which `intent-chain-files`
(`measured_provenance_test.clj:224-228`) covers:

| location | line | says |
|---|---|---|
| `docs/intent/write-refusal-completeness/write-refusal-completeness-specs.md` | 28, 39 | the byte meter measures the finalized result including `elapsed_ms`; a **128-byte timing reserve** sized on the old shape |
| `docs/intent/2026-08-30-prepared-request-ratification/prepared-request-specs...md` | 17 (`MCP-OP-PREP-REQ-001`) | "With `elapsed_ms=0.0`, the candidate public result shall be at most 32,768 bytes" |
| `docs/intent/alias-migration/alias-migration-design.md` | 51 | worked wire example `"elapsed_ms": 12.34` |
| `docs/plans/typed-mcp-inspect-entrance.md` | 158 | worked wire example `"elapsed_ms": 42.0` |
| `docs/plans/uniform-mcp-elapsed-time.md` | 11, 26-30, 40, 49, 51 | the entire plan, in the old wire |

The prepared-request row still matches the code — `mcp_inspect_tool.clj:1069,1079,1085,1119` and
`mcp_tool.clj:1129` still estimate with `(assoc candidate :elapsed_ms 0.0)` — but the code no
longer matches the wire, and the estimate now **undercounts** the published result:

```sh
~/bin/suite-run bb -e "(require '[cheshire.core :as json] '[clj-surgeon.mcp-operation :as op]) (let [raw {:ok true :operation \"inspect_clojure\" :forms [{:file \"a.clj\" :src \"(defn f [])\"}]}] (prn {:estimated-json-bytes (count (json/generate-string (assoc raw :elapsed_ms 0.0))) :published-json-bytes (count (json/generate-string (op/finalize-result raw 123.456)))}))"
```

```text
{:estimated-json-bytes 105, :published-json-bytes 122, :undercount-bytes 17}
{:estimate-shape   "{...,\"elapsed_ms\":0.0}"}
{:published-shape  "{...,\"measured\":{\"elapsed_ms\":123.456}}"}
```

Seventeen bytes on a 32,768-byte cap is not an emergency, but it is a ratified budget row asserting
against a shape the wire no longer has — the same class §4 was raised for, one lane over. Filed,
not blocking.

---

## 5. The core ruling holds on a REAL result, at both entrances

Not a synthetic domain map: a real corpus scan through the real `parse-admission` meter, published
twice with the real clock, through `invoke!` and through `finalize-failure`.

```sh
bb /home/forge/tmp/mem003r3-review-fx/parity.clj   # real scan of 25 src files, meter installed
```

```text
{:entrance :invoke!,           :scan-ms-a 1127.223, :scan-ms-b 1120.883, :elapsed-a 5.11E-4, :elapsed-b 5.01E-4, :clocks-differ true, :hashes-equal true}
{:entrance :finalize-failure,  :scan-ms-a 1127.223, :scan-ms-b 1120.883, :elapsed-a 0.001532, :elapsed-b 4.6E-4, :clocks-differ true, :hashes-equal true}
{:cross-entrance-hashes-equal true, :unpartitioned-invoke [], :unpartitioned-failure [], :bytes-scanned-a 604893, :bytes-scanned-b 604893}
```

Two real scans, genuinely different `scan_ms` (1127.223 vs 1120.883) and different request clocks,
identical `bytes_scanned` (604,893), identical hashed channels — within each entrance and across
them. This is the strongest evidence in the branch's favour and it reproduces.

## 5b. Boundary behaviour under attack — three gaps, all non-blocking

Probes on the real boundary (`/home/forge/tmp/mem003r3-review-fx/probe1.clj`):

| probe | result |
|---|---|
| reading under an undeclared key, nested two levels down, and inside rows of a vector | **relocated correctly**; `:hashed {:ok true, :receipt {:inner {:deeper {}}}, :rows [{:file "a"} {:file "b", :nested {}}]}` |
| reading directly in a vector (no key) | **typed refusal** `[:threw :unpartitioned-measured-field [:rows 0]]` |
| reading directly in a set | **typed refusal** `[:threw :unpartitioned-measured-field [:s 0]]` |
| reading inside a lazy seq | relocated |
| **reading used as a map KEY** | **survives into the hashed channel**: `:hashed {:ok true, :m {#:clj-surgeon.measured{:reading 8.71E-4} :v}}`, `:unpart ()` |
| **reading in metadata** | survives on the hashed value's metadata (inert on the wire — `pr-str` and JSON both drop meta) |
| **reading inside a sorted-map keyed by strings** | **`ClassCastException`, not a typed refusal**: `java.lang.String cannot be cast to clojure.lang.Keyword` |

The third of these is the one worth fixing: `partition-measured` ends with
`(update base measured-key merge found)` (`measured.clj:322`), which `assoc`es the keyword
`:measured` into whatever map it found the value in. `reading?`'s own docstring
(`measured.clj:134-146`) says the repository legitimately holds "SORTED maps keyed by file-name
strings (the formatter's staged sources, for one)". A per-file timing in such a map crashes the
publication boundary with a raw JVM exception instead of publishing or refusing. Latent today; the
code documents the shape that triggers it.

Note also that a **bare** number under an undeclared name still reaches the hash unchanged —
`{:verification_wall_ms 1.234}` → `:undeclared-in-hash true` — which is by design (the value has no
provenance) and is precisely why finding 1's construction routes matter.

---

## 6. MEM-021 — the rule is real; the receipt is not durable

`clj-surgeon.timing-sample/best` refuses `< 3` probes (`:insufficient-timing-probes`) or a probe
with no reading (`:missing-timing-reading`), and `red_witness.clj` now resolves `best`, `detail`
and `host-line` from that namespace instead of re-implementing them. Sol's ruling — a lower-envelope
gate estimates uncontended cost and may not be cited as a tail-latency guarantee — is stated in the
namespace docstring in those words (`timing_sample.clj:22-25`). I agree with it, and my own run
demonstrates why it is needed *and* why it is optimistic:

```text
host — 16 cores, load 10.51 6.34 5.70 9/1970 3352773
PASS   nested cold: refuses in under 50 ms            {:best-wall-ms 37, :wall-ms [47 58 37], :scan-ms [22 24 23]}
PASS   nested warm: refuses in under 50 ms            {:best-wall-ms 18, :wall-ms [22 18 21], :scan-ms [3 2 2]}
PASS   giant 128m: admission scan under 50 ms         {:best-scan-ms 13, :scan-ms [13 13 14], :wall-ms [103 107 117]}
memory-red: 6/6 assertions held (expect=green)
```

One cold rep at **58 ms** — above the bound — and the gate passed on the minimum of 37. That is the
rule working exactly as ratified, and it is also the reason the caveat has to stay attached to the
number wherever the number travels.

**Answering the brief's question directly: no, the load receipt is not carried into the receipt
text.** `host-line` is `println`'d once at the top of the run (`red_witness.clj:152`); `detail`
(`timing_sample.clj:61-70`) returns `{:best-wall-ms n :wall-ms [...] :scan-ms [...]}` with **no host
field**, and `red_witness.clj` writes no receipt file at all (its only `spit`s are the corpus cache
and a fixture). So the per-assertion detail map — the thing anyone will quote, as the round-3 record
itself quotes `{:best-scan-ms 13, :scan-ms [14 13 13]}` — is separable from the load that explains
it, and the whole receipt lives only in captured stdout. The disagreement MEM-021 exists to settle
(reviewer 52 ms, builder 13 ms) would still not be settleable from a durable artifact. Fix: put
`:host` (or at least `:load`) inside `detail`'s map, and write the run to a receipt file.

Minor: `best` coerces with `(map long readings)` (`timing_sample.clj:59`), truncating toward zero, so
a 49.9 ms reading is asserted as 49 against a `< 50` bound. Permissive direction; worth a `Math/round`
or an explicit note.

---

## 7. The builder's flagged judgment call — legitimate, not a workaround

`test/clj_surgeon/mcp_process_test.clj:317` and `test/clj_surgeon/txn_journal_test.clj:300,339` now
wrap assertions in `measured/value`. Both subjects are **pre-publication internal shapes**:
`mcp_process.clj:407` returns `:elapsed_ms (measured/elapsed-ms started)` from a function that is not
a public MCP result, and `txn_journal.clj:2358` builds `:max-ns (measured/reading window-ns)` inside a
receipt the boundary partitions later. Asserting `(pos? (measured/value ...))` on an internal tagged
reading is the honest assertion; asserting on the wrapper map would be worse. The escape-hatch
allow-list correctly scopes to `src/`, so these test-side unwraps need no entry. **Legitimate.**

---

## 8. RED → GREEN and every ordinary gate reproduces

```sh
git archive 6882eea | tar -x -C .../at-6882eea ; git archive 8b13684 | tar -x -C .../at-8b13684
~/bin/suite-run clojure -Sdeps '{:paths ["src" "test"] ...}' -M -e "(require 'clj-surgeon.measured-invariant-test 'clj-surgeon.measured-provenance-test 'clojure.test) (clojure.test/run-tests 'clj-surgeon.measured-invariant-test 'clj-surgeon.measured-provenance-test)"
```

```text
SHA 6882eeac8fee1acd656a4a2952cb1e775985cfaf
Ran 16 tests containing 36 assertions.
15 failures, 0 errors.
{:test 16, :pass 21, :fail 15, :error 0, :type :summary}

SHA 8b136845fd50f7d2a230e1b1449e8329527de042
Ran 16 tests containing 36 assertions.
0 failures, 0 errors.
{:test 16, :pass 36, :fail 0, :error 0, :type :summary}
```

RED 15 failures at its sha, GREEN at `8b13684` — exactly as claimed.

| gate | claimed | reproduced |
|---|---|---|
| `~/bin/suite-run bb test/run_all.clj` | 886 / 7043 / 0 | `Ran 886 tests containing 7043 assertions. 0 failures, 0 errors.` EXIT 0 |
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | 602 / 6365 / 0 | `Ran 602 tests containing 6365 assertions. 0 failures, 0 errors.` EXIT 0 |
| `make mcp-operation-oracle` | pass | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` exit 0 |
| intent audit | 260 / 0 | `{:ok true, :specs 260, :violations 0}` |
| `make txn-kernel-warning-check` | 0 warnings | `kernel warning check: 2 namespace(s), 0 warning(s)` exit 0 |
| `make memory-battery-self-test` | 32 / 171 / 0 | `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` |
| `make memory-red PARSER_RED_EXPECT=green` | 6/6 | `memory-red: 6/6 assertions held (expect=green)` exit 0 |
| `make memory-red-kernel` | 4 / 25 / 0 | `Ran 4 tests containing 25 assertions. 0 failures, 0 errors.` exit 0; RED exit 3 `Terminating due to java.lang.OutOfMemoryError` → GREEN journal committed 600/600 at `-Xmx256m`, `heap-used-peak-mb 254.18` |
| `make memory-battery` (once, flock, fresh root) | FAIL(INCOMPLETE), 0 reference-mismatch, 2 held-scales, 4 UNMEASURED | reproduced exactly — see below |

**The battery, run once.** Reference and battery under one `flock /home/forge/tmp/suite.lock`,
`MEMBAT_ROOT=/home/forge/tmp/membat-mem003r3-review` (fresh, did not exist),
`MEMBAT_ALLOW_ANY_ROOT` never set, no retry.

```text
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 10.1, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 41.1, :limit 6.5, :small-n-observed 4.5, :slack-mb 2.0}
  UNMEASURED reserved-peak-over-budget {:op :cli-ls-tree, ...}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-full-match, ...}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-narrow, ...}
  UNMEASURED reserved-peak-over-budget {:op :workspace-sources-read-all, ...}

reference: /home/forge/tmp/membat-mem003r3-review/receipts/20260904T052024.760977192Z-reference.edn
receipt:   /home/forge/tmp/membat-mem003r3-review/receipts/20260904T052705.933254173Z-battery.edn
[battery exit=2]
```

```sh
printf 'reference-mismatch-count='; rg -c 'reference-mismatch' /home/forge/tmp/membat-mem003r3-review/receipts/20260904T052705.933254173Z-battery.edn
```

```text
reference-mismatch-count=0
```

Exactly the expected state: **zero** `reference-mismatch` lines — the parity claim this whole branch
exists to protect — exactly two `held-scales-with-n` FAILs, both the ones named pre-existing at
`2556a38` (9.8/3.0 and 40.9/6.5 there; 10.1/3.0 and 41.1/6.5 here), four UNMEASURED reserved-peak
lines belonging to the MEM-001 lane, and no `cli-ls-tree` held-scales line.

---

## 9. Merge dry-run onto `MCP/main` — one conflict, and the new ratchet will fire

The trunk has moved past `9b7220c3` (which is an ancestor of it); `origin/MCP/main` is now
`cf8aebd095f7f7f758aa5593e11a68ea697ef1ab`.

```sh
git merge-tree --write-tree HEAD origin/MCP/main
```

```text
[exit=1]
297826f8dc1f5dfacd2b0f5ac6dc670c59f35dfd
100644 ... 1  src/clj_surgeon/mcp_change_buffer.clj
100644 ... 2  src/clj_surgeon/mcp_change_buffer.clj
100644 ... 3  src/clj_surgeon/mcp_change_buffer.clj

CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_change_buffer.clj
```

One conflict, one hunk, in `run-process!` — the trunk's admit-gate work added a
`visible-byte-limit` arity while this branch converted the same line to `measured/start`:

```clojure
<<<<<<< HEAD
   (let [started (measured/start)]
=======
   (run-process! project-root command timeout-ms
                 exact-verification-visible-bytes))
  ([project-root command timeout-ms visible-byte-limit]
   (let [started (System/nanoTime)]
>>>>>>> origin/MCP/main
```

**More important than the conflict: the merge is not mechanical, because this branch's new ratchet
goes red on trunk code it has never seen.** I scanned `origin/MCP/main`'s `src/` with this branch's
scanner and then simulated the merge (branch `src/` plus the trunk's new namespace):

```text
{:post-merge-unallowed (["src/clj_surgeon/mcp_admit_tool.clj" "default-test-runner"])}
```

`src/clj_surgeon/mcp_admit_tool.clj` does not exist on this branch and reads
`(System/currentTimeMillis)` at line 710. After any merge,
`no-raw-clock-read-lives-outside-the-measured-namespace` fails until that read is either converted
to `measured/start` or given a reasoned `:control` allow-list entry. Whoever merges must do that in
the merge commit, not after.

---

## 10. Non-mutation proof

No commit, push, stash, `git add`, or `git add -A` on any tree. No Surgeon MCP server started; no
server started at all, on any port. No process signalled that I did not start. All heavy
measurement ran under `flock /home/forge/tmp/suite.lock` (via each recipe's own lock for
`memory-red`/`memory-red-kernel`, and an explicit outer `flock` around the single
reference+battery pair) with `MEMBAT_ROOT=/home/forge/tmp/membat-mem003r3-review`, fresh, run once,
`MEMBAT_ALLOW_ANY_ROOT` never set. Fixtures under `/home/forge/tmp/mem003r3-review-fx`, removed at
the end. Unit suites through `~/bin/suite-run`; `clojure -M:clj-surgeon/mcp-test` directly, never
`make mcp-test`. Files read with `rg`/`sed`, never the Surgeon MCP tools.

---

## NO-GO

`3692e9b` may **not** land on `MCP/main`: Sol's §1 blocking finding reproduces at this tip through
two independent, three-token bypasses of the new provenance tag (a keyword lookup on the reading
map, and `:refer`ring the raw clock verb), so an undeclared clock-derived field still reaches the
parity hash with every witness green — and separately, the merge itself conflicts in
`src/clj_surgeon/mcp_change_buffer.clj` and would leave the branch's own new clock ratchet red on
the trunk's `mcp_admit_tool.clj`.
