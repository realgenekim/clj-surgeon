## NO-GO

Round-four review of `bridge/integration-2026-09-03-mem003` at `694f538d` (MEM-003 measured
clock, second landing). Taken with Sol's round-four brief after Sol's content filter refused it;
paths substituted per the integrator's instruction. Clone `/home/forge/tmp/sol/mem003r4-wt`,
proven clean at the tip; no commit, push, stash or `git add`; no source edited; no server
started; sabotage only on `git archive` exports under `/var/tmp/forge/mem003r4-review-fx/opus`,
removed at the end.

```sh
cd /home/forge/tmp/sol/mem003r4-wt && git rev-parse HEAD && git status --porcelain
```

```text
694f538d235c7bd5f9bab4153a313d15d0f867ab
```

(`git status --porcelain` printed nothing.)

---

## Headline

The rung-5 repair is real and it closes both of the round-three plants: `Reading` and `Tick` are
opaque `deftype`s, a keyword lookup yields `nil`, `:refer`/`:use`/another alias/a fully-qualified
call are each a typed offence, and I reproduced every one of those closures. The trunk merge is
composed correctly, the two defects the type surfaced are real and fixed, and every gate I could
run is green at the claimed numbers.

**But §1 is still not closed, and it fails in exactly the shape the round-three review named.**
The type is opaque; the NAMESPACE is not. `measured/unwrap-readings` is a public verb in
`clj-surgeon.measured` that turns a tagged reading into a bare number at any depth, and the
escape-hatch scan — which enumerates laundering verbs **by name** — has never heard of it. I
reproduced round-three's §1a counterexample line for line at this tip with that verb: an
undeclared clock field inside the parity hash subject, `unpartitioned []`, and all twelve
invariant tests green. The builder's own §4 fix uses that verb, twice, in `src/`, with no
allow-list entry, and the witness said nothing.

And the ratchet the merge added has a second miss on the trunk it merged: two lines above the
raw read it caught, `mcp_admit_tool.clj:737` publishes `(.lastModified report)` — a wall-clock
epoch reading — as the receipt field `:report_written_at`, inside the hashed parity subject. The
clock scanner's pattern does not contain `.lastModified`.

Both are the brief's blocking criterion.

---

## 1. BLOCKING — `measured/unwrap-readings` is a second laundering door, and no scan knows it. §1a reproduces verbatim in effect.

`measured/value` is documented as "THE ONE LAUNDERING VERB, and now that is a fact about the type
rather than a promise in a docstring" (`src/clj_surgeon/measured.clj:188-195`). It is not a fact
about the type. `src/clj_surgeon/measured.clj:226-246` defines

```clojure
(defn unwrap-readings
  "`x` with every tagged reading, at any depth, replaced by its bare number.
  ...
  [x]
  (cond
    (reading? x) (value x)
    ...
```

— public, arbitrary depth, and it calls `value` internally so no call site of ITS callers ever
matches. The escape-hatch pattern is
`test/clj_surgeon/measured_invariant_test.clj:145`:

```clojure
  #"measured/raw-nanos|measured/raw-ms|measured/value|measured/-launder|launderable"
```

`measured/unwrap-readings` matches none of those five alternatives. Neither does `measured/field`
(`measured.clj:328-331`), which also hands back a bare number.

### 1a. The plant

`git archive 694f538d` exported to `/var/tmp/forge/mem003r4-review-fx/opus/plantU`, one line
changed at `src/clj_surgeon/mcp_hot_verify.clj:114` — the same victim the round-three review used:

```diff
--- a/src/clj_surgeon/mcp_hot_verify.clj
+++ b/src/clj_surgeon/mcp_hot_verify.clj
@@ -111,4 +111,6 @@
                :error-type (or (:error-type (ex-data error))
                                :hot-verification-connection-failed)
                :error (.getMessage error)
-               :elapsed_ms (measured/elapsed-ms started)})))))))
+               :elapsed_ms (measured/elapsed-ms started)
+               :verification_wall_ms (measured/unwrap-readings
+                                       (measured/elapsed-ms started))})))))))
```

Exact command, run from that export:

```sh
cd /var/tmp/forge/mem003r4-review-fx/opus/plantU && ~/bin/suite-run clojure -Sdeps '{:paths ["src" "test"] :deps {org.clojure/clojure {:mvn/version "1.12.1"} cheshire/cheshire {:mvn/version "5.13.0"} org.babashka/sci {:mvn/version "0.10.47"} rewrite-clj/rewrite-clj {:mvn/version "1.2.50"} nrepl/nrepl {:mvn/version "1.3.1"}}}' -M -e '
(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)
         (quote [clj-surgeon.mcp-hot-verify :as hot])
         (quote [clj-surgeon.mcp-operation :as op])
         (quote [clj-surgeon.measured :as measured]))
(let [raw (hot/verify! "." {:port-file "definitely-missing-port" :reload [] :tests []})
      seen (atom nil) ticks (atom [0 1000000])]
  (op/invoke! {:clock-nanos #(let [x (first @ticks)] (swap! ticks subvec 1) x)
               :execute (constantly raw) :summarize (constantly "ok") :serialize pr-str
               :callback (fn [_ _ r] (reset! seen r))})
  (prn {:public-result @seen})
  (prn {:undeclared-field (:verification_wall_ms @seen)
        :hashed-field (:verification_wall_ms (measured/hashed-channel @seen))
        :unpartitioned (vec (measured/unpartitioned-measured-paths @seen))}))
(t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

Verbatim output:

```text
{:public-result {:ok false, :status :failed, :error-type :hot-verification-connection-failed, :error "/var/tmp/forge/mem003r4-review-fx/opus/plantU/definitely-missing-port (No such file or directory)", :verification_wall_ms 2.487443, :measured {:elapsed_ms 1.0}}}
{:undeclared-field 2.487443, :hashed-field 2.487443, :unpartitioned []}

Testing clj-surgeon.measured-invariant-test

Ran 12 tests containing 46 assertions.
0 failures, 0 errors.
```

That is the round-three review's §1a output, reproduced at the tip whose commit message reads
*"GREEN §1: the reading is an opaque type — a keyword lookup and a `:refer` are no longer doors."*
A keyword lookup is not a door any more; a **public function in the same namespace** is. No raw
clock read, no `value` call, no allow-list entry, no changed count, no naming offence — and an
undeclared clock-derived field inside the parity hash subject with all twelve invariant tests and
forty-six assertions green.

The same result under babashka alone (the scanner's own runtime), from the same export:

```sh
cd /var/tmp/forge/mem003r4-review-fx/opus/plantU && ~/bin/suite-run bb -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

```text
Ran 12 tests containing 46 assertions.
0 failures, 0 errors.
{:test 12, :pass 46, :fail 0, :error 0, :type :summary}
```

### 1b. This is not hypothetical — the round-four fix itself uses the verb

`src/clj_surgeon/recovery.clj:69` and `:91`, added by `37a1cf9c` as the §4 fix:

```clojure
         measured/measured-key (measured/unwrap-readings
                                 {:elapsed-ms {:up up-elapsed
                                               :probe probe-elapsed
                                               :total (elapsed-ms started)}})
```

Those two uses are correct in intent — a measured block holds bare numbers — but they are two
laundering call sites in `src/` that the "every untagged-clock verb call site is named" witness
(`measured_invariant_test.clj:288-298`) does not see. The escape-hatch allow-list carries twelve
entries; neither of these is among them:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && awk '/^\(def escape-hatch-allow-list/,/^\(def [^e]/' test/clj_surgeon/measured_invariant_test.clj | grep -c ":calls"
```

```text
12
```

`measured/field` is the same class, reached from an already-published block:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && bb -e '
(require (quote [clj-surgeon.measured :as measured]) (quote [clj-surgeon.mcp-operation :as op]))
(defn publish [dom]
  (let [seen (atom nil) ticks (atom [0 1000000])]
    (op/invoke! {:clock-nanos #(let [x (first @ticks)] (swap! ticks subvec 1) x)
                 :execute (constantly dom) :summarize (constantly "ok")
                 :serialize pr-str :callback (fn [_ _ r] (reset! seen r))})
    @seen))
(let [r (measured/elapsed-ms (measured/start))
      inner (measured/measured {:elapsed_ms r})
      leaked (measured/field inner :elapsed_ms)
      p (publish {:ok true :receipt {:stable :fact} :verification_wall_ms leaked})]
  (prn {:route :measured-field
        :hashed (measured/hashed-channel p)
        :unpartitioned (vec (measured/unpartitioned-measured-paths p))}))'
```

```text
{:route :measured-field, :hashed {:ok true, :receipt {:stable :fact}, :verification_wall_ms 0.005108}, :unpartitioned []}
```

The witness's own docstring states the design goal — *"The pattern is the type's whole surface,
deliberately. A door the scanner does not know is the exact defect the round-three review walked
through"* (`measured_invariant_test.clj:142-144`). The pattern is the type's surface; it is not
the NAMESPACE's surface, and the namespace exports two more functions that yield bare numbers.

### What would close it

Two shapes, either of which is a real ratchet rather than a longer list of names:

1. **Scan the namespace, not the verbs.** Match `measured/<anything>` against a declared
   allow-list of SANCTIONED verbs, so a call to a measured verb nobody classified fails the
   witness — the same inversion the require rule already applies to the namespace's NAME. Adding a
   public function to `clj-surgeon.measured` would then cost an entry, which is the cost on
   purpose.
2. Or make `unwrap-readings` (and `field`) private and expose only the block-building verbs
   (`measured`, `attach`, `partition-measured`) that never hand a bare number to a caller. The two
   `recovery.clj` sites can call `measured/measured` instead.

Either way the escape-hatch allow-list must gain the recovery sites, and the plant above must go
red.

---

## 2. BLOCKING — a raw wall-clock reading is published in a receipt field inside the parity hash: `mcp_admit_tool.clj:737`, two lines above the one the ratchet caught.

The round-four record says the branch's new ratchet went red on trunk code and the offending read
was ROUTED rather than allow-listed, via the new `measured/wall-clock-ms`. That is true of
`:report_started_at`. The line immediately above it was not looked at:

`src/clj_surgeon/mcp_admit_tool.clj:737-738`:

```clojure
                 :report_written_at (when written? (.lastModified report))
                 :report_started_at started
```

`started` is `(measured/wall-clock-ms)` — a tagged reading, correctly relocated.
`(.lastModified report)` is the file-modification epoch of a report file the runner has just
written **in this operation**: a wall clock reading by any definition, varying every run, and it
is published straight into the hashed channel. The clock scanner's pattern
(`measured_invariant_test.clj:136`) is

```clojure
  #"System/nanoTime|System/currentTimeMillis|Instant/now|\.getTime"
```

`.lastModified` is not in it, so this site was invisible to the very scan that caught its
neighbour. The map flows into the published result at `mcp_admit_tool.clj:989` →
`verify-snapshot!`'s `:tests` key.

Proof that it survives the projection and moves the parity hash (the two publications differ only
in that field):

```sh
cd /home/forge/tmp/sol/mem003r4-wt && bb -e '
(require (quote [clj-surgeon.measured :as measured]) (quote [clj-surgeon.mcp-operation :as op]))
(defn publish [dom]
  (let [seen (atom nil) ticks (atom [0 1000000])]
    (op/invoke! {:clock-nanos #(let [x (first @ticks)] (swap! ticks subvec 1) x)
                 :execute (constantly dom) :summarize (constantly "ok")
                 :serialize pr-str :callback (fn [_ _ r] (reset! seen r))})
    @seen))
(defn dom [written-at]
  {:ok true
   :tests {:ran true :passed 3
           :report_written_at written-at
           :report_started_at (measured/wall-clock-ms)}})
(let [a (publish (dom 1757000000000)) b (publish (dom 1757000000123))]
  (prn {:hashed-a (measured/hashed-channel a)})
  (prn {:hashed-b (measured/hashed-channel b)})
  (prn {:parity-identical (= (pr-str (measured/hashed-channel a)) (pr-str (measured/hashed-channel b)))
        :unpartitioned (vec (measured/unpartitioned-measured-paths a))}))'
```

```text
{:hashed-a {:ok true, :tests {:ran true, :passed 3, :report_written_at 1757000000000}}}
{:hashed-b {:ok true, :tests {:ran true, :passed 3, :report_written_at 1757000000123}}}
{:parity-identical false, :unpartitioned []}
```

`:report_started_at` was correctly relocated out of the hashed channel; `:report_written_at`, a
clock number produced two lines earlier in the same `let`, was not, and the diagnostic reports
nothing. **This is the brief's own question — "is there any OTHER trunk site the ratchet should
have caught?" — answered in the same function the ratchet fired on.**

Fix: `(measured/wall-clock-ms)` is not the right verb here (the number is a file's mtime, not
"now"), so either wrap it — `(measured/reading (.lastModified report))` — or drop the field; and
add `\.lastModified` (and `lastModifiedTime`, `toEpochMilli`, `LocalDate*`) to `clock-pattern`
with the existing control sites allow-listed. The other `.lastModified` readers I found are
control, not receipt, and would take allow-list entries:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && rg -n "lastModified|toEpochMilli" src dev/experiments | grep -vE "^\S+: *;;|;; "
```

```text
src/clj_surgeon/mcp_alias_migration.clj:1385:                                         (- (.lastModified file)))
src/clj_surgeon/ls_tree_snapshot.clj:301:                         (> (- now (.lastModified f)) snapshot-ttl-ms))]
src/clj_surgeon/mcp_admit_tool.clj:737:                 :report_written_at (when written? (.lastModified report))
src/clj_surgeon/txn_journal.clj:235:       :mtime-ns (.to (.lastModifiedTime attrs) java.util.concurrent.TimeUnit/NANOSECONDS)
src/clj_surgeon/txn_journal.clj:363:          (.toEpochMilli ^java.time.Instant (.get started)))))
src/clj_surgeon/txn_journal.clj:436:   nil when the file is not there: `lastModified` returns 0 for an absent
src/clj_surgeon/txn_journal.clj:440:    (let [mtime (.lastModified lock)
src/clj_surgeon/txn_journal.clj:889:   present empty one: `lastModified` of a missing file is 0, which reads as
src/clj_surgeon/txn_journal.clj:901:                                      "unix:size,lastModifiedTime,ctime"
src/clj_surgeon/txn_journal.clj:904:          mtime (.toMillis ^FileTime (get attrs "lastModifiedTime"))
src/clj_surgeon/txn_journal.clj:953:   `lastModified` of a missing file is 0, which reads as infinitely old and
src/clj_surgeon/txn_journal.clj:1034:   the stat. They are deliberately NOT in `:found`: `lastModified` of a
src/clj_surgeon/mcp_telemetry.clj:54:                          (< (.lastModified %) cutoff)))
```

Only `mcp_admit_tool.clj:737` publishes into a public receipt; the rest are lease/identity
control. That makes this one line the blocker and the pattern gap the ratchet.

---

## 3. NON-BLOCKING — `(hash r)` / `(.hashCode r)` returns a number derived from the clock reading, with no verb any scan knows.

`measured.clj:130`:

```clojure
  (hashCode [_] (hash launderable)))
```

Measured on the JVM:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && ~/bin/suite-run clojure -Sdeps '{:paths ["src"] :deps {org.clojure/clojure {:mvn/version "1.12.1"} cheshire/cheshire {:mvn/version "5.13.0"} org.babashka/sci {:mvn/version "0.10.47"} rewrite-clj/rewrite-clj {:mvn/version "1.2.50"}}}' -M -e '(require (quote [clj-surgeon.measured :as measured])) (let [r (measured/elapsed-ms (measured/start)) r2 (measured/elapsed-ms (measured/start))] (prn {:pr-str (pr-str r) :str (str r) :hash (hash r) :hashCode (.hashCode r) :equals-self (= r r) :equals-other (= r r2) :bean (bean r) :reflect-declared-fields (mapv #(.getName %) (.getDeclaredFields (class r))) :reflect-setAccessible (let [f (.getDeclaredField (class r) "launderable")] (.setAccessible f true) (.get f r)) :serializable (instance? java.io.Serializable r) :cheshire (try (cheshire.core/generate-string {:a r}) (catch Throwable e [:threw (.getSimpleName (class e))])) :read-back (try (read-string (pr-str r)) (catch Throwable e [:threw (.getSimpleName (class e))])) :into-map (try (into {} r) (catch Throwable e [:threw (.getSimpleName (class e))])) :sorted-set (try (pr-str (sorted-set r)) (catch Throwable e [:threw (.getSimpleName (class e))]))}))'
```

```text
{:bean {:class clj_surgeon.measured.Reading}, :format "#clj-surgeon.measured/reading", :hash -1263434913, :reflect-declared-fields ["launderable" "__cached_class__0" "const__3" "const__4"], :cheshire [:threw "JsonGenerationException"], :pr-str "#clj-surgeon.measured/reading", :sorted-set [:threw "ClassCastException"], :reflect-setAccessible 0.070236, :into-map [:threw "IllegalArgumentException"], :read-back [:threw "RuntimeException"], :hashCode -1263434913, :str "#clj-surgeon.measured/reading", :serializable false, :equals-self true, :equals-other false, :prn-in-map "{:x #clj-surgeon.measured/reading}"}
```

Everything else the brief asked me to try is genuinely shut: `bean` yields only `:class`;
`into {}`, `seq`, `deref` and `read-string` throw; the type is not `Serializable`; cheshire
refuses to encode it (fail-loud, which is right); `pr-str`, `str` and `format` withhold the
number and print deterministically, nested or not.

Two residuals, both worth a line and neither worth blocking:

- `(hash r)` is a deterministic function of the reading, so `:some_field (hash r)` puts a
  clock-varying integer into the hashed parity subject with no verb the scanner matches. It is
  not the reading, and nobody writes that by accident, but `hashCode` does not need to consult
  the number at all — `(hash Reading)` or a constant would keep `equals`/`hashCode` consistent
  for the only use these types have.
- `setAccessible` reflection yields the number. The field name `launderable` IS in the
  escape-hatch pattern, so a literal plant is caught; `(.getDeclaredField (class r) (str "launder" "able"))`
  is not. Textual scanning cannot close that, and a deliberate attacker is not the threat model
  here — record it, do not chase it.

Under babashka the private field is not enforced at all — `(.-launderable r)` returned
`0.005809` — which is exactly why `launderable` is in the pattern. That mitigation works.

---

## 4. CLOSED — the trunk merge, composed so both ceilings hold

`git show 154787c0 --stat` names one conflict; the resolution at
`src/clj_surgeon/mcp_change_buffer.clj:1262-1282` gives the 3-arity a delegation and puts the
measured clock in the new 4-arity:

```clojure
  ([project-root command timeout-ms]
   (run-process! project-root command timeout-ms
                 exact-verification-visible-bytes))
  ([project-root command timeout-ms visible-byte-limit]
   (let [started (measured/start)]
```

Both MCP-OP-ADMIT-122's read ceiling and the measured clock hold. Nothing else in the merge
diff touches the partition.

**Is `wall-clock-ms` "a launder by another name"?** No. `measured.clj:206-214` returns
`(reading (raw-ms))` — a tagged reading, indistinguishable at the boundary from a duration, and
my §2 probe shows it relocated out of the hashed channel. An epoch number is a number, but the
point of the partition is the TAG, not the units, and the tag is present. Ruling: correct, and
the right shape.

---

## 5. CLOSED — the two defects the opaque type surfaced are real, and the fixes work

**`recovery/recover!` wrote tagged readings into an on-disk EDN receipt.** Round three's code
(`git diff 3692e9ba..694f538d -- src/clj_surgeon/recovery.clj`) placed the readings directly in a
`:measured` block that is `pprint`'d to disk and read back with `clojure.edn`; the fix wraps them.
The defect is real: at round three the tag was a map so the round trip parsed silently, and at
this tip an unwrapped reading would print as `#clj-surgeon.measured/reading` and fail `edn/read`.
(Caveat: the fix's chosen verb is the subject of finding 1.)

**The pre-publication byte estimate measured the raw map.** `mcp_write_refusal/json-bytes` now
measures `(measured/partition-measured value)`. The undercount reproduces:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && bb -e '(require (quote [clj-surgeon.mcp-write-refusal :as wr]) (quote [clj-surgeon.measured :as measured]) (quote [cheshire.core :as json])) (let [v {:ok true :elapsed_ms (measured/reading 12.5) :rows [1 2 3]}] (prn {:json-bytes-r4 (wr/json-bytes v) :partitioned (json/generate-string (measured/partition-measured v)) :r3-raw-map-estimate (count (.getBytes (json/generate-string {:ok true :elapsed_ms 12.5 :rows [1 2 3]}) "UTF-8")) :undercount (- (wr/json-bytes v) (count (.getBytes (json/generate-string {:ok true :elapsed_ms 12.5 :rows [1 2 3]}) "UTF-8")))}))'
```

```text
{:json-bytes-r4 57, :partitioned "{\"ok\":true,\"rows\":[1,2,3],\"measured\":{\"elapsed_ms\":12.5}}", :r3-raw-map-estimate 44, :undercount 13}
```

13 bytes on this shape (the builder measured 17 on theirs); the budget check now sizes the shape
the finalizer actually publishes.

---

## 6. Rulings on the declared-open items

- **`bench/event_timing.clj` `fail!` file-fatal.** Correctly left open. It is a benchmark
  summarizer, not a published surface, and the requested change (typed per-item refusal) is a
  design change with its own witness. Not a merge condition.
- **§5b reading-in-METADATA.** The builder's declaration holds and I verified it. A reading
  attached as metadata survives `hashed-channel` (structure sharing preserves meta) and
  `unpartitioned-measured-paths` reports nothing, but both wire encodings drop it:

  ```text
  {:route :metadata, :unpartitioned [], :meta-on-hashed {:wall #clj-surgeon.measured/reading}, :wire "{:ok true, :receipt {:stable :fact}}"}
  ```

  Metadata does not participate in Clojure map `=` or `hash` either, so the parity subject is
  unaffected. **Ruling: accept as declared.** The residual is cosmetic — if `*print-meta*` were
  ever true, the leak is the opaque marker, never a number.
- **Four UNMEASURED reserved-peak lines and two `held-scales-with-n` FAILs.** MEM-001's lane,
  pre-existing at the base, unchanged in kind by this branch, and the battery's verdict is
  INCOMPLETE for exactly that reason. **Ruling: not this branch's to close**, and not a reason to
  hold it — the reason to hold it is findings 1 and 2.
- **§1d, the `:control` allow-list's `:why` strings.** Materially improved: the new
  `a-clock-reading-never-becomes-a-raw-number-in-any-placement` witness proves the property the
  `:why` strings are really claiming, in six placements, through the real finalizer, with typed
  refusals rather than raw exceptions. It still does not bind any of the 33 entries to
  non-publication. Non-blocking, because finding 1 is the live route and this one is now defended
  at the boundary.

---

## 7. CLOSED — the other non-blocking items I re-checked

- **§2 hardening gap, `CallToolResult` construction.** Scanned at
  `test/clj_surgeon/measured_provenance_test.clj:208-260` with the pattern
  `#"CallToolResult/builder|CallToolResult\."` and a plant that proves the scan goes red.
  Construction rather than `.structuredContent` is the right subject, for the reason the file
  gives: the setter is also how a client reads a response.
- **§3 residual (a), `dev/experiments` unscanned.** Now a scanned root
  (`measured_invariant_test.clj:41-54`, `scanned-roots ["src" "dev/experiments"]`), and the four
  raw sites are gone:

  ```sh
  cd /home/forge/tmp/sol/mem003r4-wt && rg -n "System/nanoTime|System/currentTimeMillis|Instant/now" dev/experiments
  ```

  ```text
  dev/experiments/owner_aware_call_capture_server.clj:36:;; `System/nanoTime`, bypassing `mcp-operation/invoke!` entirely (round-three
  ```

  One comment line, which the scanner strips before matching.
- **§5b reading as a map KEY and inside a string-keyed sorted map.** Both are now typed
  `:unpartitioned-measured-field` refusals rather than a bare number or a raw
  `ClassCastException`, witnessed in six placements through the real finalizer
  (`measured_invariant_test.clj:514-566`), and that test is among the twelve that pass.
- **The require rule.** `measured-naming-offence` (`measured_invariant_test.clj:300-312`) types
  `:fully-qualified`, `:refer`, `:use` and `:alias`, with a planted-`:refer` witness at `:339-364`
  that reproduces round-three's §1b exactly. This is the right shape and it works.
- **MEM-021 `:host` in `timing-sample/detail`, and `best` no longer coercing with `long`.**
  Confirmed present; `make memory-red` prints the host block (see the gate table).

---

## 8. Gates at `694f538d` — every one I ran, verbatim

All JVM suites through `~/bin/suite-run`; the memory battery and `memory-red*` under
`flock /home/forge/tmp/suite.lock` (the Makefile takes that lock itself for the `memory-red*`
targets, so I did not nest a second one); `MEMBAT_ROOT=/home/forge/tmp/membat-mem003r4-opusrev`,
fresh; `MEMBAT_ALLOW_ANY_ROOT` never set. No server was started, on any port.

| gate | result | exit |
|---|---|---|
| `~/bin/suite-run bb test/run_all.clj` | `Ran 892 tests containing 7078 assertions. 0 failures, 0 errors.` | 0 |
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 716 tests containing 8496 assertions. 0 failures, 0 errors.` | 0 |
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | 0 |
| `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` | 0 |
| `make memory-battery-self-test` | `generate_tree verification self-test: ok` / `root-marker: ok` / `self-test: ok` / `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` | 0 |
| `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` | 0 |
| `make admit-analyzer-memory-self-test` | `admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m` (n=100/1000/10000, heap-peak 35/30/114 MiB against budget 409) | 0 |
| `clj-surgeon.admit-patch-test` alone | `Ran 114 tests containing 2122 assertions. 0 failures, 0 errors.` `{:test 114, :pass 2122, :fail 0, :error 0, :type :summary}` | 0 |
| intent audit (`audit-current-repository`) | `{:ok true, :specs 354, :violations 0}` — exactly the claimed 354/0 | 0 |
| `make memory-red-kernel` | `Ran 4 tests containing 25 assertions. 0 failures, 0 errors.`; `FLATNESS 600 {... :xmx-mb 256.0, :heap-used-peak-mb 254.866 ...}` at `-Xmx256m` | 0 |
| `make memory-battery` (ONCE, flock, fresh root) | **`verdict: FAIL (INCOMPLETE)   exit 1`** — **reference-mismatch lines: 0** (`grep -c "reference-mismatch"` on the battery section returned `0`); the two known `held-scales-with-n` (`rename-ns-plan-full-match` observed 10.1 / limit 3.0; `workspace-sources-read-all` observed 41.0 / limit 6.5); four `UNMEASURED reserved-peak-over-budget`; ten TREND lines, never gated. Receipt `/home/forge/tmp/membat-mem003r4-opusrev/receipts/20260904T071539.228934181Z-battery.edn` | 1 (make wraps it as 2) |
| `clj-surgeon.measured-invariant-test` at the tip | `Ran 12 tests containing 46 assertions. 0 failures, 0 errors.` — **and identically green with the finding-1 plant in `src/`** | 0 |

Every claimed number the builder published and I could re-run matched exactly: 892/7078/0,
716/8496/0, 32/171/0, 6/6, 3/3, 114/2122/0. **The measurements are honest. The problem is not the
numbers; it is that the witness behind them has a hole the numbers cannot show.**

The one caveat the builder declared — the battery attestation naming `head-sha 154787c0` for a run
on the tree that became `37a1cf9c` — is replaced by my own run above.


### The battery run, verbatim

```sh
flock /home/forge/tmp/suite.lock make memory-battery-reference MEMBAT_ROOT=/home/forge/tmp/membat-mem003r4-opusrev
flock /home/forge/tmp/suite.lock make memory-battery           MEMBAT_ROOT=/home/forge/tmp/membat-mem003r4-opusrev
```

```text
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 10.1, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 41.0, :limit 6.5, :small-n-observed 4.5, :slack-mb 2.0}
  UNMEASURED reserved-peak-over-budget {:op :cli-ls-tree, ...}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-full-match, ...}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-narrow, ...}
  UNMEASURED reserved-peak-over-budget {:op :workspace-sources-read-all, ...}
```

**Zero `reference-mismatch` lines** — the twelve that opened this whole lane are gone and I
confirmed the count is 0, not merely that I did not notice any. `cli-ls-tree`'s own
`held-scales-with-n` is closed and flat, measured:

```text
  cli-ls-tree                  default   N=1000   fresh     505 ms  peak   272.1 MB  held     9.7 MB
  cli-ls-tree                  default   N=10000  fresh    1817 ms  peak   270.4 MB  held     9.6 MB
```

Reference built once by an explicit `make memory-battery-reference`; `MEMBAT_ALLOW_ANY_ROOT`
never set; a fresh root; one run, no retry. Exactly the expected shape.

---

## What has to change before this is a GO

1. **Finding 1** — close the laundering door at the namespace, not at a list of verb names:
   either scan every `measured/…` call against a declared verb allow-list, or make
   `unwrap-readings` and `field` private and route `recovery.clj` through `measured/measured`.
   Then re-run the plant in §1a and show it RED.
2. **Finding 2** — stop publishing `(.lastModified report)` raw in `mcp_admit_tool.clj:737`
   (tag it or drop it), and widen `clock-pattern` to the file-time and epoch-conversion APIs,
   allow-listing the control sites that then appear.
3. **Finding 3** is a note, not a condition.

Everything else in this round is genuinely done, and the round-four work is a large step
forward — the type is the right repair, the require rule is the right shape, the merge is
correct, and the numbers are all honest. Two doors were left open because both witnesses
enumerate NAMES, and the round-three review's own sentence is the diagnosis of both:
**a name the scanner does not know is a hole in every scanner at once.**

---

## NO-GO

**Mergeability:** the tip merges cleanly onto current `origin/MCP/main` —
`git merge-tree --write-tree HEAD origin/MCP/main` at `origin/MCP/main` `8aa45491` (past the
builder's claimed `252a7cea`) exits 0 and prints a single tree oid
`125b52dc07b6e0be7da6718692ad33c81e0dcb8d` with no conflict markers — so it is mergeable but not
mergeABLE-and-correct: **do not land it until findings 1 and 2 are closed**, because it would
ship a publication boundary whose two witnesses can both be walked past by ordinary code, and a
trunk receipt field that puts a wall clock inside the parity hash.
