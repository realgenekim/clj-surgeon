## NO-GO

Round-five review of `bridge/integration-2026-09-03-mem003` at `dc6ee93f` (MEM-003 measured
clock, second landing). Taken with Sol's round-five brief and its ROUND-FIVE ADDENDUM after
Sol's content filter refused it and a previous Opus reviewer was killed by an API session limit;
paths substituted per the integrator's instruction. Clone `/home/forge/tmp/sol/mem003r4-wt`,
proven clean at the tip; nothing in it committed, pushed, stashed, added or edited; no server
started on any port; sabotage only on `git archive dc6ee93f` exports under
`/var/tmp/forge/mem003r5-review-fx/opus`, removed at the end.

```sh
cd /home/forge/tmp/sol/mem003r4-wt && git rev-parse HEAD && git status --porcelain && git log --oneline -1
```

```text
dc6ee93f6907d0effb5b6b17c73da58accbc9c41
dc6ee93f docs: round-5 build record and the round-4 NO-GO review it answers
```

(`git status --porcelain` printed nothing between the two lines.)

**Housekeeping, as instructed:** a previous reviewer left fixtures under
`/var/tmp/forge/mem003r5-review-fx` — `opus/` (264 MB) and `equality-plant/` (44 MB), both
timestamped this morning. I removed both before starting, and worked in a fresh
`/var/tmp/forge/mem003r5-review-fx/opus`. Nothing else under `/var/tmp/forge` was touched.

---

## Headline

Round five is a genuine advance and it closes the two round-four blockers **at their named
sites**. `unwrap-readings` is private, `field` is deleted, the three call sites are rerouted
through `measured/attach` and `get-in`, `hashCode` is a constant, and the clock pattern is
derived from eleven JDK classes by reflection instead of typed out. Every gate I ran is green at
the claimed numbers, and I reproduced each of the builder's own witnesses going red on the
round-four code.

**But §1 is still not closed, and it fails for the third round running in the same shape.** The
type is opaque; the namespace's public surface is now probed; but the *reachability* of the
laundering machinery is neither. Two routes, both ordinary Clojure, both landing a clock number
in an undeclared receipt field inside the hashed parity subject with all nineteen invariant tests
and seventy-five assertions green:

1. `(._launder r)` — the protocol's own interface method, one character away from the sanctioned
   `measured/-launder`, and matched by no pattern in the file.
2. `((ns-resolve 'clj-surgeon.measured 'unwrap-readings) x)` — the private verb, reached by the
   attack the brief named, and invisible to the naming rule because `ns-resolve` spells the
   namespace without a trailing slash.

The var-quote route the brief also named (`#'clj-surgeon.measured/unwrap-readings`) *is* caught,
by two witnesses. The other two are not.

§2 and §3 are materially closed, with three residuals worth naming and one of them a real gap in
the derivation (a constructor is not a method, so `(hash (java.util.Date.))` is a clock-varying
integer the derived pattern cannot see).

Findings 1 and 2 below are the brief's blocking criterion: "a tagged reading that can be
laundered into a number by ordinary code."

---

## 1. BLOCKING — `(._launder r)` is the sanctioned door spelled as interop, and no witness in the file knows it

`measured/-launder` is the protocol method, correctly sanctioned and correctly in
`escape-hatch-pattern` (`test/clj_surgeon/measured_invariant_test.clj:243`):

```clojure
  #"measured/raw-nanos|measured/raw-ms|measured/value|measured/-launder|launderable")
```

Every alternative in that pattern is anchored to the literal text `measured/`. A protocol method
compiles to a Java interface method whose name is munged (`-launder` → `_launder`), so on the JVM
the *same door* is reachable as plain interop, with no namespace token at all:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && ~/bin/suite-run clojure -Sdeps '{:paths ["src"] :deps {org.clojure/clojure {:mvn/version "1.12.1"} cheshire/cheshire {:mvn/version "5.13.0"} org.babashka/sci {:mvn/version "0.10.47"} rewrite-clj/rewrite-clj {:mvn/version "1.2.50"}}}' -M /var/tmp/forge/mem003r5-review-fx/opus/probe1.clj
```

```text
protocol iface interop ._launder           => 987654.321
protocol var -launder                      => 987654.321
```

### 1a. The plant

`git archive dc6ee93f` exported to `/var/tmp/forge/mem003r5-review-fx/opus/plantP`, one line added
at `src/clj_surgeon/mcp_hot_verify.clj:114` — the same victim rounds three and four used:

```diff
-               :elapsed_ms (measured/elapsed-ms started)})))))))
+               :elapsed_ms (measured/elapsed-ms started)
+               :verification_wall_ms (._launder (measured/elapsed-ms started))})))))))
```

```sh
cd /var/tmp/forge/mem003r5-review-fx/opus/plantP && ~/bin/suite-run clojure -Sdeps '{:paths ["src" "test"] :deps {org.clojure/clojure {:mvn/version "1.12.1"} cheshire/cheshire {:mvn/version "5.13.0"} org.babashka/sci {:mvn/version "0.10.47"} rewrite-clj/rewrite-clj {:mvn/version "1.2.50"} nrepl/nrepl {:mvn/version "1.3.1"}}}' -M -e '
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
{:public-result {:ok false, :status :failed, :error-type :hot-verification-connection-failed, :error "/var/tmp/forge/mem003r5-review-fx/opus/plantP/definitely-missing-port (No such file or directory)", :verification_wall_ms 3.410159, :measured {:elapsed_ms 1.0}}}
{:undeclared-field 3.410159, :hashed-field 3.410159, :unpartitioned []}

Testing clj-surgeon.measured-invariant-test

Ran 19 tests containing 75 assertions.
0 failures, 0 errors.
{:test 19, :pass 75, :fail 0, :error 0, :type :summary}
```

An undeclared clock-derived field, inside the parity hash subject, `unpartitioned []`, nineteen
tests and seventy-five assertions green — at the tip whose commit message reads *"GREEN §1: the
laundering verbs are private and the probe knows the namespace, not its names."*

The scan is textual, so it is equally blind under babashka, which is the runtime the scanning
gate actually runs in:

```sh
cd /var/tmp/forge/mem003r5-review-fx/opus/plantP && ~/bin/suite-run bb -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

```text
Ran 19 tests containing 75 assertions.
0 failures, 0 errors.
{:test 19, :pass 75, :fail 0, :error 0, :type :summary}
```

### 1b. Why the new reflective probe cannot see this

`the-measured-namespace-exposes-no-unsanctioned-laundering-verb` (`:771`) is a good witness and it
does what it says: it calls every public var and finds exactly the two sanctioned launderers. But
it is a witness about **which vars launder**, not about **how a laundering var can be spelled at a
call site**. `-launder` is a *sanctioned* launderer; the finding is that its call sites are
supposed to cost an `escape-hatch-allow-list` line, and `(._launder x)` costs nothing. The
witness at `:788` even checks that every sanctioned verb's name is matched by
`escape-hatch-pattern` — which is precisely the check that is being routed around, because the
pattern matches the *var* spelling and the JVM offers a second one.

This is the file's own stated design goal turned against it
(`measured_invariant_test.clj:241-242`): *"The pattern is the type's whole surface, deliberately.
A door the scanner does not know is the exact defect the round-three review walked through."*

### What would close it

`\._launder\b` in `escape-hatch-pattern` is the one-line half of it, and it is worth taking. The
real ratchet is the shape the builder already reached for once: **derive the escape-hatch pattern
from the namespace rather than typing it**, exactly as `clock-pattern` is now derived from the
JDK — enumerate `(ns-interns 'clj-surgeon.measured)` plus the protocol's interface methods
(`(.getMethods clj_surgeon.measured.Launderable)`), and build the alternation from both the
`measured/<name>` and the `.<munged-name>` spellings. A hand-written list of five alternatives is
the same instrument the round-four review just condemned as "a list of the names somebody thought
of," moved one level down.

---

## 2. BLOCKING — the private verb is reached by `ns-resolve`, which the naming rule cannot see

The brief named this attack. It works. `unwrap-readings` being private is a compile-time
convention in Clojure, not a boundary; the round-five defence against reaching past it is the
naming rule plus the public-var scan, and `ns-resolve` defeats both:

- `measured-naming-offence` (`measured_invariant_test.clj:411-424`) fires `:fully-qualified` only
  on `#"clj-surgeon\.measured/"` — a **trailing slash**. `(ns-resolve 'clj-surgeon.measured
  'unwrap-readings)` spells the namespace followed by a space, so the second cond clause matches
  the "names the namespace" regex, none of `:refer`/`:use`/`:alias` applies, and the `cond` falls
  off the end returning `nil`. No offence.
- `unknown-measured-verbs` (`:810-823`) scans for `#"measured/([A-Za-z0-9?!*<>=+_'-]+)"`. There
  is no `measured/` token on the line, so there is no verb to check.

### 2a. The plant

Same export shape, `/var/tmp/forge/mem003r5-review-fx/opus/plantN`, one line at
`src/clj_surgeon/mcp_hot_verify.clj:114`:

```diff
-               :elapsed_ms (measured/elapsed-ms started)})))))))
+               :elapsed_ms (measured/elapsed-ms started)
+               :verification_wall_ms ((ns-resolve 'clj-surgeon.measured 'unwrap-readings)
+                                      (measured/elapsed-ms started))})))))))
```

```sh
cd /var/tmp/forge/mem003r5-review-fx/opus/plantN && ~/bin/suite-run clojure -Sdeps '{:paths ["src" "test"] :deps {org.clojure/clojure {:mvn/version "1.12.1"} cheshire/cheshire {:mvn/version "5.13.0"} org.babashka/sci {:mvn/version "0.10.47"} rewrite-clj/rewrite-clj {:mvn/version "1.2.50"} nrepl/nrepl {:mvn/version "1.3.1"}}}' -M -e '
(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)
         (quote [clj-surgeon.mcp-hot-verify :as hot])
         (quote [clj-surgeon.mcp-operation :as op])
         (quote [clj-surgeon.measured :as measured]))
(let [raw (hot/verify! "." {:port-file "definitely-missing-port" :reload [] :tests []})
      seen (atom nil) ticks (atom [0 1000000])]
  (op/invoke! {:clock-nanos #(let [x (first @ticks)] (swap! ticks subvec 1) x)
               :execute (constantly raw) :summarize (constantly "ok") :serialize pr-str
               :callback (fn [_ _ r] (reset! seen r))})
  (prn {:undeclared-field (:verification_wall_ms @seen)
        :hashed-field (:verification_wall_ms (measured/hashed-channel @seen))
        :unpartitioned (vec (measured/unpartitioned-measured-paths @seen))}))
(t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

```text
{:undeclared-field 1.668085, :hashed-field 1.668085, :unpartitioned []}

Testing clj-surgeon.measured-invariant-test

Ran 19 tests containing 75 assertions.
0 failures, 0 errors.
{:test 19, :pass 75, :fail 0, :error 0, :type :summary}
```

`unwrap-readings` strips tags **at any depth**, so this is the strictly more dangerous of the two
routes: it is the round-four blocking finding, unchanged in power, reached one indirection away.

### 2b. The var-quote route the brief also named IS closed — credit where due

`/var/tmp/forge/mem003r5-review-fx/opus/plantV`, `@#'clj-surgeon.measured/unwrap-readings` in the
same place:

```sh
cd /var/tmp/forge/mem003r5-review-fx/opus/plantV && ~/bin/suite-run bb -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

```text
FAIL in (the-measured-namespace-is-named-only-by-the-sanctioned-require) (.../measured_invariant_test.clj:441)
clj-surgeon.measured named other than `[clj-surgeon.measured :as measured]`: [["src/clj_surgeon/mcp_hot_verify.clj" 115 :fully-qualified ":verification_wall_ms (@#'clj-surgeon.measured/unwrap-readings"]]

FAIL in (every-measured-verb-named-in-source-is-a-public-var) (.../measured_invariant_test.clj:826)
measured/<verb> references that are not public vars of clj-surgeon.measured: [["src/clj_surgeon/mcp_hot_verify.clj" 115 "unwrap-readings"]]

Ran 19 tests containing 75 assertions.
2 failures, 0 errors.
```

Two independent witnesses, both red. That is the right shape, and it is why the gap is narrow and
specific: the naming rule is anchored on the **slash**, and the two spellings that reach the
namespace without one (`ns-resolve`/`ns-interns`/`ns-publics` with a quoted symbol, and interop on
the protocol interface) are the whole hole.

### What would close it

Add a third `measured-naming-offence` clause: naming `clj-surgeon.measured` as a **quoted symbol
argument** — `'clj-surgeon.measured` or `(quote clj-surgeon.measured)` — outside the sanctioned
require line is `:reflective`, because every var-resolution API takes the namespace that way and
none of them respects privacy. That is a two-line change to a `cond` that already exists, it costs
nothing legitimate (nothing in `src/` needs to resolve a measured var at runtime), and it closes
`ns-resolve`, `ns-interns`, `ns-publics`, `find-var` and `requiring-resolve` in one clause. I
verified there is no such reference in the tree today:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && rg -n "ns-resolve|ns-interns|find-var|requiring-resolve" src dev/experiments | grep -i measured
```

(no output — so the clause would be green on the current tree from the moment it lands.)

---

## 3. NON-BLOCKING (ratchet gap, no live site) — the clock pattern is derived from a hand-written list of CLASSES, and four ordinary spellings fall outside it

§2's repair is real and the mechanism is a genuine improvement: the pattern is now built by
reflection over eleven JDK classes and yields 46 spellings, not four.

```sh
cd /home/forge/tmp/sol/mem003r4-wt && ~/bin/suite-run bb -e '
(require (quote clj-surgeon.measured-invariant-test) (quote [clojure.string :as str]))
(let [p (var-get (ns-resolve (quote clj-surgeon.measured-invariant-test) (quote clock-pattern)))
      alts (str/split (str p) #"\|")]
  (println "n-alternatives:" (count alts))
  (doseq [row (partition-all 6 (sort alts))] (println (str/join "  " row))))'
```

```text
n-alternatives: 46
\.atTime\b  \.creationTime\b  \.getDate\b  \.getEpochSecond\b  \.getNano\b  \.getTime\b
\.getTimezoneOffset\b  \.instant\b  \.lastAccessTime\b  \.lastModifiedTime\b  \.lastModified\b  \.millis\b
\.minusMillis\b  \.minusNanos\b  \.plusMillis\b  \.plusNanos\b  \.toEpochDay\b  \.toEpochMilli\b
\.toEpochSecond\b  \.toInstant\b  \.toLocalDateTime\b  \.toLocalDate\b  \.toLocalTime\b  \.toMillis\b
\.toOffsetDateTime\b  \.withNano\b  \.withZoneSameInstant\b  \QClock/systemDefaultZone\E  \QClock/systemUTC\E  \QClock/system\E
\QClock/tickMillis\E  \QFileTime/fromMillis\E  \QFiles/getLastModifiedTime\E  \QInstant/now\E  \QInstant/ofEpochMilli\E  \QInstant/ofEpochSecond\E
\QLocalDate/now\E  \QLocalDate/ofEpochDay\E  \QLocalDate/ofInstant\E  \QLocalDateTime/now\E  \QLocalDateTime/ofEpochSecond\E  \QLocalDateTime/ofInstant\E
\QSystem/currentTimeMillis\E  \QSystem/nanoTime\E  \QZonedDateTime/now\E  \QZonedDateTime/ofInstant\E
```

But `clock-source-classes` (`measured_invariant_test.clj:142-152`) is itself a hand-written list,
and a **constructor is not a method**, so `.getMethods` cannot see one. Four ordinary spellings
fall outside the 46, planted together at
`/var/tmp/forge/mem003r5-review-fx/opus/plantO/src/clj_surgeon/mcp_hot_verify.clj:114`:

```diff
+               :verification_at (str (java.time.OffsetDateTime/now))
+               :verification_cal (.getTimeInMillis (java.util.Calendar/getInstance))
+               :verification_raw (. System nanoTime)
+               :verification_date (hash (java.util.Date.))})))))))
```

```sh
cd /var/tmp/forge/mem003r5-review-fx/opus/plantO && ~/bin/suite-run bb -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

```text
Ran 19 tests containing 75 assertions.
0 failures, 0 errors.
{:test 19, :pass 75, :fail 0, :error 0, :type :summary}
```

Four clock-varying receipt fields, all green. Why each is invisible:

- `java.time.OffsetDateTime` and `java.time.LocalTime` are in `clock-return-types` but **not** in
  `clock-source-classes`, so `OffsetDateTime/now` is never derived even though the derivation
  already knows the type is a time.
- `java.util.Calendar` is in neither list.
- `(. System nanoTime)` is the dot special form: the text is not `System/nanoTime`, and
  `\.nanoTime\b` is not an alternative because `nanoTime` is static.
- `(java.util.Date.)` reads the clock in a **constructor**, and `Date.hashCode` is derived from
  `getTime` — a clock-varying integer whose spelling contains no time morpheme.

**Why this is not blocking:** I checked, and there is no live instance in the tree.

```sh
cd /home/forge/tmp/sol/mem003r4-wt && rg -n "OffsetDateTime|LocalTime/|java\.util\.Calendar|Calendar/getInstance|getTimeInMillis|\(\. System|java\.util\.Date\.|\(Date\.\)|new java\.util\.Date|java\.sql\.Timestamp|Duration/|System\.nanoTime|Year/|YearMonth" src dev/experiments
```

```text
src/clj_surgeon/mcp_semantic_client.clj:31:                      (.connectTimeout (Duration/ofSeconds 3))
src/clj_surgeon/mcp_semantic_client.clj:34:                   (.initializationTimeout (Duration/ofSeconds 5))
src/clj_surgeon/mcp_semantic_client.clj:35:                   (.requestTimeout (Duration/ofSeconds 40))
src/clj_surgeon/mcp_recovery.clj:39:          (.timeout (Duration/ofMillis (long (or timeout-ms 10000))))
```

All four are `Duration/of*` from literal constants — timeouts, not clock reads. Unlike round-four
finding 2, this is a hole a future commit could walk through, not a receipt shipping today.

**What would close it, cheaply:** add `java.time.OffsetDateTime`, `java.time.LocalTime`,
`java.util.Calendar` and `java.sql.Timestamp` to `clock-source-classes`; add the declared
constructors of those classes (`(.getDeclaredConstructors c)` with a zero-arg or clock-shaped
signature) as a `ClassSimpleName\.` alternative; and add `#"\(\.\s+System\s+(nanoTime|currentTimeMillis)"`
for the dot special form. `clock-expressions-the-ratchet-must-carry` (`:210-231`) is the right
place to nail each of the four down as a fail-first witness — it is a good idea and it should
carry these.

---

## 4. CLOSED with a declared residual — §3, the hash

`hashCode` is now `(hash ::reading)` / `(hash ::tick)` (`measured.clj:157-174`), and the RED
commit `a67e3d4f` genuinely fails on the old code (see the fail-first table below). Confirmed at
the tip, from the probe above:

```text
(hash r)                                   => -1169916561        ; r is 987654.321
(hash r2) other number                     => -1169916561        ; r2 is 1.5
equality oracle exact                      => true
equality oracle wrong                      => false
set-membership oracle                      => true
map-key oracle                             => :hit
```

Two readings with different numbers hash **equally** — no clock bits — and equality is by VALUE,
not identity, so the `hashCode`/`equals` contract holds and `(= (reading 1.5) (reading 1.5))`
stays true. That is the correct trade and the builder reasoned about it correctly in the comment.

**Residual worth declaring (I rule it non-blocking, at the same tier as the `setAccessible`
residual the builder already declares):** value equality is an oracle, and now that hashing is
constant it is a *total* one — a reading is usable as a map key, so both `=` and set membership
answer "is the hidden number exactly x?" Ordinary code recovers the number by bisection with no
measured verb but the public `measured/reading` constructor:

```text
--- binary-search laundering via = (ordinary code) ---
recovered by bisection: 12345.678 in 67 steps
```

(from `probe1.clj`, command above.) Nobody writes that by accident, which is exactly the
round-four reasoning about `setAccessible`, and it is why I do not block on it. But it deserves
the same one-paragraph declaration in `measured.clj`, because it is the one remaining accessor
that consults the number and the file currently claims `value`/`-launder` are the whole surface.

**Second residual, cheap to close:** `unwrap-readings` / `partition-measured` /
`unpartitioned-measured-paths` walk Clojure collections only, so a reading inside a Java
collection is neither unwrapped, relocated, nor diagnosed:

```text
attach fed a reading in ArrayList   => {:ok true, :measured {:xs [#clj-surgeon.measured/reading]}}
partition-measured w/ ArrayList     => {:ok true, :xs [#clj-surgeon.measured/reading]}
unpartitioned paths w/ ArrayList    => []
```

The failure mode is loud rather than silent — cheshire refuses to encode a `Reading`, and the
printed form carries no number — so this is a diagnostic blind spot, not a leak. Adding
`(instance? java.util.Collection node)` to the three walkers, or a typed refusal, would make it
`:unpartitioned-measured-field` like the other five placements.

---

## 5. CLOSED — round-four's finding 2 is genuinely fixed, and the fix works at the boundary

`mcp_admit_tool.clj:742` now reads `(measured/file-modified-ms report)`, and `file-modified-ms`
(`measured.clj:251-268`) returns `(reading ms)`, nil when the file is absent — the right call,
since `.lastModified` returns 0 for a missing file and 0 reads as the epoch. Driven through the
real finalizer with two files whose mtimes differ by 999 seconds:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && ~/bin/suite-run bb -e '
(require (quote [clj-surgeon.measured :as measured]) (quote [clj-surgeon.mcp-operation :as op]) (quote [clojure.java.io :as io]))
... (spit f1 "x") (spit f2 "x") (.setLastModified f1 1757000000000) (.setLastModified f2 1757000999000) ...
      dom (fn [f] {:ok true :tests {:ran true :passed 3
                                    :report_written_at (measured/file-modified-ms f)
                                    :report_started_at (measured/wall-clock-ms)}})'
```

```text
{:public-a {:ok true, :tests {:ran true, :passed 3, :measured {:report_written_at 1757000000000, :report_started_at 1788517189332}}, :measured {:elapsed_ms 1.0}}}
{:hashed-a {:ok true, :tests {:ran true, :passed 3}}}
{:parity-identical true, :parity-= true, :unpartitioned []}
{:absent-file nil}
```

Round four's exact counterexample (`:parity-identical false`) is now `true` under both `pr-str`
and `=`. `:report_written_at` and `:report_started_at` are the only two references in the tree,
both inside that one map, so there is no schema or consumer left declaring them top-level:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && grep -rn "report_written_at\|report_started_at" src test docs/contracts
```

```text
src/clj_surgeon/measured.clj:260:  `:report_written_at` inside the hashed parity subject, two lines above a wall
src/clj_surgeon/mcp_admit_tool.clj:742:                 :report_written_at (when written?
src/clj_surgeon/mcp_admit_tool.clj:744:                 :report_started_at started
```

**Is `wall-clock-ms` / `file-modified-ms` "a launder by another name"?** No, and the reasoning is
right: both return `(reading n)`, so the boundary relocates them. An epoch number is a number, but
the partition is about the TAG, not the units, and the tag is present — proven by
`:parity-identical true` above.

**The seven newly-`:control` sites.** I read each and drove the one the brief singled out.
`mcp_alias_migration/prune-details!` (`:1350-1396`) reads `(.lastModified file)` only inside the
`sort-by` key that orders deletion candidates; it returns `write-detail-manifest!`'s value — file
names — and publishes no timestamp. `txn_journal/path-stat` (`:215-242`) puts `:mtime-ns` in a
stat map, and every consumer takes `(select-keys stat [:kind :file-key])` before publishing a
conflict (`txn_journal.clj:2251`), so `:mtime-ns` is a comparison field that never reaches a
receipt — exactly what its `:why` string claims. `process-start-ticks`, `evidence-stat`,
`lock-age-basis-ms`, `touch-tombstone!` and the two raised prune counts are lease/identity/
retention control of the same kind. **No `:receipt` entry exists, and the witness at `:392-395`
still refuses one.**

---

## 6. Rulings on the declared-open items

- **`bench/event_timing.clj` `fail!` file-fatal.** Correctly left open, unchanged from round four:
  a benchmark summarizer is not a published surface, and `bench/` is outside `scanned-roots` by
  design. Not a merge condition. (Worth noting for a later round: `bench/` contains 90+ `.clj`
  files and is a third unscanned root, but nothing in it constructs an MCP result.)
- **§5b, a reading in METADATA.** Re-verified at this tip; the declaration holds:

  ```text
  {:route :metadata, :unpartitioned [], :meta-on-hashed {:wall #clj-surgeon.measured/reading}, :wire "{:ok true, :receipt {:stable :fact}}", :meta-in-= true}
  ```

  The reading rides on the hashed value's metadata and is inert on the wire; `:meta-in-= true`
  confirms metadata does not participate in map equality, so the parity subject is unaffected, and
  the only thing `*print-meta*` could ever leak is the opaque marker. **Accept as declared.**
- **A clock read in `dev/` outside `dev/experiments`.** No such file exists — `dev/` contains only
  `dev/experiments/**` `.clj` (verified by `find`), so the scanned root covers the whole of `dev/`
  today. The gap is latent, not live.
- **Four UNMEASURED reserved-peak lines and two `held-scales-with-n` FAILs.** MEM-001's lane,
  pre-existing at the base, unchanged in kind by this branch; the battery's verdict is INCOMPLETE
  for exactly that reason. **Not this branch's to close**, and not a reason to hold it. The reason
  to hold it is findings 1 and 2.
- **The `setAccessible`-with-a-computed-field-name residual.** Declared verbatim at
  `measured.clj:139-146`, quoting the round-four ruling. Correctly recorded, correctly not chased.
  I add one residual of the same tier in finding 4 (value equality as an oracle) that should be
  declared alongside it.

---

## 7. Fail-first discipline: every RED commit genuinely fails

I exported each RED tree and ran its own witness. All three are honest RED commits, not
retroactive labels.

| RED commit | witness output at that tree |
|---|---|
| `8e546ba0` §1 | `Ran 16 tests containing 55 assertions. 5 failures` — `unsanctioned ["unwrap-readings"]`, `unsanctioned ["field"]`, and the unwrap plant returning `[]` |
| `dfbc47d3` §2 | `Ran 18 tests containing 70 assertions. 2 failures` — the widened derivation surfaces `mcp_admit_tool/default-test-runner`, `mcp_alias_migration/prune-details!`, `txn_journal/{lock-age-basis-ms,evidence-stat,path-stat,process-start-ticks,touch-tombstone!}` and raises `mcp_telemetry/prune!` and `ls_tree_snapshot/prune!` from 1 to 2 |
| `a67e3d4f` §3 | `Ran 19 tests containing 75 assertions. 2 failures` — `two readings with different numbers hash differently: 1073217536 vs -445368160` |

Command shape (per sha):

```sh
cd /var/tmp/forge/mem003r5-review-fx/opus/red-<sha> && ~/bin/suite-run bb -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

The §2 RED output is also the answer to the brief's question about other trunk sites the ratchet
should have caught: the widened derivation found **eight more forms and two raised counts**, and
the only one that was publishing rather than controlling — `default-test-runner` — was routed, not
allow-listed.

---

## 8. One claim in the build record is now false

`docs/observations/mem003-second-landing-round5-build.md` §1 says:

> **Impossible from ordinary code.** `unwrap-readings` is private — reachable only from
> `measured`, `attach` and `partition-measured` […]

`ns-resolve` reaches it (finding 2), and `._launder` reaches the door beneath it (finding 1).
Clojure's `^:private` is a resolution convention, not a boundary; the sentence should say
"inconvenient from ordinary code, and an offence when named" once the naming rule covers the
quoted-symbol spelling. This matters beyond wording: the whole round-five argument is that the
complement no longer depends on names, and it still does — it depends on the name appearing
**with a slash after it**.

---

## 9. Scope note — the two other unscanned roots, and the temp-dir count

**Unscanned roots.** `scanned-roots` is `["src" "dev/experiments"]`. `dev/` contains nothing else
(verified), but `test/` (which constructs results in fixtures) and `bench/` (90+ `.clj`, including
the declared-open `bench/event_timing.clj`) are outside every scan in the file. Neither
constructs a published MCP result today, so this is a note rather than a finding — but the same
sentence that justified adding `dev/experiments` ("a scan whose boundary is a directory name
rather than a classpath is a scan with a blind spot in it") applies verbatim to `bench/`.

**Leaked suite temp dirs, counted as asked.** The builder reported ~6,100. I count far fewer, and
the number churns hard because several seats are running JVM suites on this box right now:

```sh
ls -1 /var/tmp/forge | wc -l          # 253 at 10:15Z, 650 at 10:38Z
find /var/tmp/forge -maxdepth 1 -type d | wc -l   # 621
df -i /var/tmp | tail -1                          # 4827071 / 39438480 inodes, 13% used
```

621 directories at the time of measurement, overwhelmingly
`clj-surgeon-change-buffer-*` (112), `clj-surgeon-kondo-admission-*` (33) and
`clj-surgeon-fake-kondo-*` (15). Inode pressure is 13%, so nothing is at risk today. **I deleted
none of them** — the tree is multi-tenant and they are not this branch's. This is not a merge
condition; it is a housekeeping bead for whoever owns the suite harness, and the honest statement
is that the "~6,100" figure did not reproduce at either sample.

---

## 10. The rest of the brief's §1 attack list, each with a verdict

Enumerated from the tip's own namespace (`probe1.clj`, output quoted in findings 1 and 4):

| attack the brief named | result | verdict |
|---|---|---|
| `#'clj-surgeon.measured/unwrap-readings` (var quote defeats privacy) | reaches the number **but** two witnesses go red (§2b) | CLOSED |
| `(ns-interns …)` / `ns-resolve` + deref | reaches the number, **all witnesses green** | **BLOCKING — finding 2** |
| a public var whose arity the probe does not exercise | no public var of `clj-surgeon.measured` is variadic or keyword-arg'd; the four with `nil` `:arglists` (`Launderable`, `measured-key`, `measured-field-names`, and the `->Reading`/`->Tick` factories) are probed at arities 1 and 2 by `probed-arities`, and `text-measured-prefix` is correctly reported inert (`ifn? false`) rather than skipped | CLOSED (empty today; the drop-the-variadic-tail rule is a latent gap worth a comment) |
| a protocol method reached through the protocol's own var | `(Launderable …)` is a map lookup → `nil`; `measured/-launder` is sanctioned and pattern-matched | CLOSED |
| the same method reached through the compiled interface | `(._launder r)` → the number, all witnesses green | **BLOCKING — finding 1** |
| `measured/attach` fed a bare number — can it publish one as a reading? | `{:ok true, :measured {:wall 1757000000000}}` — the number lands **inside** the block, which is where MEM-005 requires a bare number; it cannot place one in the hashed channel | NO — attack fails, correctly |
| a reading inside a Java collection handed to `attach` | survives untouched inside the block; `unpartitioned-measured-paths` reports `[]`; cheshire refuses to encode it, so the failure is loud | non-blocking residual (finding 4) |
| `(bean r)`, `(pr-str r)` + `read-string`, `(str r)`, `(into {} r)`, `seq`, `deref`, sorted-map, map key, Java serialization, cheshire | all shut, re-verified at this tip; the six-placement typed-refusal witness passes | CLOSED |
| `(hash r)` / `(= r r2)` | hash is constant; equality is by value, which is a bisection oracle | CLOSED for hash; declared-residual for equality (finding 4) |
| a reading in METADATA | inert on the wire, invisible to `=` | accept as declared (finding 6) |
| babashka vs JVM difference | the textual scans behave identically in both; `(.-launderable r)` is enforced on the JVM and not under bb, which is why `launderable` is in the pattern — that mitigation still works. **`(._launder r)` needs no privacy bypass in either runtime.** | see finding 1 |

### 10a. Runtime addendum to findings 1 and 2 — which route works where

```sh
cd /home/forge/tmp/sol/mem003r4-wt && bb -e '
(require (quote [clj-surgeon.measured :as measured]))
(let [r (measured/reading 987654.321)]
  (prn {:runtime :babashka
        :iface-interop (try (._launder r) (catch Throwable e [:threw (.getMessage e)]))
        :dash-field    (try (.-launderable r) (catch Throwable e [:threw (.getMessage e)]))
        :ns-resolve    (try ((ns-resolve (quote clj-surgeon.measured) (quote unwrap-readings)) {:x r}) (catch Throwable e [:threw (.getMessage e)]))
        :var-quote     (try (@#(quote clj-surgeon.measured/unwrap-readings) {:x r}) (catch Throwable e [:threw (.getMessage e)]))}))'
```

```text
{:runtime :babashka, :iface-interop [:threw "Method _launder on class sci.impl.deftype.SciType not allowed!"], :dash-field 987654.321, :ns-resolve {:x 987654.321}, :var-quote [:threw "sci.impl.fns$fun$arity_0__1461 cannot be cast to java.util.concurrent.Future"]}
```

Stated honestly: **finding 1's `._launder` executes on the JVM and is refused by sci**, so its
runtime blast radius is the JVM — which is the runtime the production MCP server actually runs in,
and where I demonstrated the number reaching the parity subject. **Finding 2's `ns-resolve` works
in both runtimes.** In both cases the ratchet — a textual scan that runs under babashka — is blind
in *both* runtimes, and that is the part being reviewed. `.-launderable` under bb is the case the
`launderable` alternative already covers, and it still works.

---

## 11. The new intents encode the hole, so fixing the witness means editing the requirement too

`docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md`, added by this branch:

```text
- [x] **MCP-OP-TIME-006**: When `clj-surgeon.measured` exposes a public var, that var shall not turn a tagged clock reading into a bare number outside a `measured` block, ... and every `measured/<verb>` reference under `src/` and `dev/experiments` shall name a var that is public in that namespace.
```

Both halves are scoped to the **public var** and to the **`measured/` spelling**. Findings 1 and 2
are both outside the requirement as written, which is why the witness that implements it faithfully
still passes. A correct round six edits the requirement first — something closer to *"no expression
in `src/` or `dev/experiments` shall obtain the number a tagged reading holds, except at a site
named in the escape-hatch allow-list"* — and then derives the witness from it. `MCP-OP-TIME-007` is
well-formed by contrast and only needs its class list widened (finding 3).

---

## 12. Gates at `dc6ee93f` — every one I ran, verbatim

All JVM suites through `~/bin/suite-run`; `memory-red`/`memory-red-kernel` take
`/home/forge/tmp/suite.lock` inside the Makefile, so I did not nest a second one; the battery
under an explicit `flock /home/forge/tmp/suite.lock`, `MEMBAT_ROOT=/home/forge/tmp/membat-mem003r5-opusrev`
(fresh, removed at the end), `MEMBAT_ALLOW_ANY_ROOT` never set. No server was started, on any port.
The box was heavily contended by other seats throughout (load 12–15 on 16 cores), which shows in
wall times, not in verdicts.

| gate | result | exit | claim |
|---|---|---|---|
| `~/bin/suite-run bb test/run_all.clj` | `Ran 899 tests containing 7107 assertions. 0 failures, 0 errors.` | 0 | 899/7107/0 ✓ |
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 716 tests containing 8496 assertions. 0 failures, 0 errors.` | 0 | 716/8496/0 ✓ |
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | 0 | ✓ |
| `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` | 0 | 2/0 ✓ |
| intent audit (`clj-surgeon.mcp-intent-contract/audit-current-repository ".")` | `{:ok true, :specs 356, :violations 0}` | 0 | 356/0 ✓ |
| `make memory-battery-self-test` | `generate_tree root-marker self-test: ok` / `generate_tree self-test: ok` / `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` | 0 | 32/171/0 ✓ |
| `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)`, with the MEM-021 `:host` block present (`{:cores 16, :load "15.27 11.15 7.69 …"}`) | 0 | 6/6 ✓ |
| `make memory-red-kernel` | `Ran 4 tests containing 25 assertions. 0 failures, 0 errors.`; `FLATNESS 600 {… :xmx-mb 256.0, :heap-used-peak-mb 254.60977935791016 …}` at `-Xmx256m` | 0 | 4/25/0, peak 254.61 vs claimed 254.83 — same shape, run-to-run ✓ |
| `clj-surgeon.admit-patch-test` alone (`java -cp $(clojure -Spath -M:clj-surgeon/mcp-test) clojure.main -e …`) | `Ran 114 tests containing 2122 assertions. 0 failures, 0 errors.` `{:test 114, :pass 2122, :fail 0, :error 0, :type :summary}` | 0 | 114/2122/0 ✓ |
| `make admit-analyzer-memory-self-test` | `admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m` (n=100/1000/10000, heap-peak 35/79/159 MiB against budget 409) | 0 | 3/3 ✓ |
| `clj-surgeon.measured-invariant-test` at the tip | `Ran 19 tests containing 75 assertions. 0 failures, 0 errors.` — **and identically green with the finding-1 and finding-2 plants in `src/`** | 0 | ✓ |
| `make memory-battery` (ONCE, flock, fresh root, attested `head-sha dc6ee93f`) | **`verdict: FAIL (INCOMPLETE)   exit 1`** | 1 (make wraps as 2) | expected shape ✓ |

### The battery run, verbatim

```sh
export MEMBAT_ROOT=/home/forge/tmp/membat-mem003r5-opusrev
flock /home/forge/tmp/suite.lock make memory-battery-reference MEMBAT_ROOT=$MEMBAT_ROOT
flock /home/forge/tmp/suite.lock make memory-battery           MEMBAT_ROOT=$MEMBAT_ROOT
```

The reference attested to the tip, not to an ancestor — this replaces the builder's declared
caveat about a receipt naming an earlier sha:

```text
attested to {:head-sha "dc6ee93f6907d0effb5b6b17c73da58accbc9c41", :jvm "21.0.12"}
receipt: /home/forge/tmp/membat-mem003r5-opusrev/receipts/20260904T103820.158845512Z-reference.edn
```

Battery verdict block, verbatim:

```text
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 10.1, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 41.1, :limit 6.5, :small-n-observed 4.5, :slack-mb 2.0}
  TREND peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :fresh, :profile :default, :observed 267.1, :limit 248.8}
  TREND peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :warm, :profile :default, :observed 290.5, :limit 248.8}
  TREND peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :fresh, :profile :default, :observed 286.9, :limit 248.8}
  TREND peak-over-budget {:op :cli-ls-tree, :n 10000, :phase :warm, :profile :default, :observed 289.0, :limit 248.8}
  TREND peak-over-budget {:op :rename-ns-plan-narrow, :n 1, :phase :fresh, :profile :giant, :observed 293.7, :limit 249.3}
  TREND peak-over-budget {:op :rename-ns-plan-narrow, :n 1, :phase :warm, :profile :giant, :observed 303.2, :limit 249.3}
  TREND peak-over-budget {:op :rename-ns-plan-full-match, :n 1, :phase :fresh, :profile :giant, :observed 296.8, :limit 249.3}
  TREND peak-over-budget {:op :rename-ns-plan-full-match, :n 1, :phase :warm, :profile :giant, :observed 297.3, :limit 244.9}
  TREND peak-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 208.3, :limit 107.5, :small-n-observed 75.5, :slack-mb 32}
  UNMEASURED reserved-peak-over-budget {:op :cli-ls-tree, ...}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-full-match, ...}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-narrow, ...}
  UNMEASURED reserved-peak-over-budget {:op :workspace-sources-read-all, ...}

receipt: /home/forge/tmp/membat-mem003r5-opusrev/receipts/20260904T104506.952909305Z-battery.edn
```

**Zero `reference-mismatch` lines** — I checked the count rather than merely not noticing any:

```sh
grep -c "reference-mismatch" gate-battery.txt   # 0
grep -c "TREND"              gate-battery.txt   # 10
```

Exactly the expected shape: the two known `held-scales-with-n`, four UNMEASURED, ten TREND lines
(never gated), and `cli-ls-tree`'s own `held` flat at 9.5–9.7 MB from N=1000 to N=10000. One run,
one reference, no retry.

**Every claimed number I could re-run matched:** 899/7107/0, 716/8496/0, 356/0, 32/171/0, 6/6,
4/25/0, 114/2122/0, 3/3, and the battery's exact FAIL(INCOMPLETE) shape. **The measurements are
honest. The problem is not the numbers; it is that two of the three witnesses behind finding 1's
subject can be walked past by ordinary code, and the numbers cannot show that.**

---

## What has to change before this is a GO

Both blockers are small — this should be a short round six, not a rewrite.

1. **Finding 1** — add `\._launder\b` to `escape-hatch-pattern`, and better, derive that pattern
   from `(ns-interns 'clj-surgeon.measured)` plus `(.getMethods clj_surgeon.measured.Launderable)`
   so both the `measured/<verb>` and the munged `.<method>` spellings come from the namespace
   rather than a list. Then re-run the `plantP` command and show it RED.
2. **Finding 2** — add a `:reflective` clause to `measured-naming-offence` for the quoted-symbol
   spelling of the namespace (`'clj-surgeon.measured` / `(quote clj-surgeon.measured)`), which
   closes `ns-resolve`, `ns-interns`, `ns-publics`, `find-var` and `requiring-resolve` at once and
   is green on the tree today. Then re-run the `plantN` command and show it RED.
3. **Amend `MCP-OP-TIME-006`** so the requirement is about obtaining a reading's number from
   anywhere in the scanned roots, not about the namespace's public vars (§11) — otherwise the next
   witness will faithfully implement the same hole.
4. **Finding 3** is a ratchet gap with no live site: widen `clock-source-classes` and add the
   constructor and dot-special-form spellings, with the four plants of `plantO` added to
   `clock-expressions-the-ratchet-must-carry`. Worth doing in the same round; not worth blocking on
   alone.
5. **Findings 4 and 6** are declarations, not conditions.

Everything else is done and done well. The type is the right repair, the require rule is the right
shape, the merge and the `file-modified-ms` routing are correct, the fail-first discipline is real,
and every number is honest. Round four's diagnosis still applies to what remains, one level in:
**a spelling the scanner does not know is a hole in every scanner at once**, and this tip closed
the names while leaving two spellings.

---

## NO-GO

**Mergeability:** the tip merges cleanly onto current `origin/MCP/main` —
`git merge-tree --write-tree HEAD origin/MCP/main` at `origin/MCP/main` `e2b62307` ("log:
ceremony-free arm pair launched", past the builder's claimed `73764392`; I did not fetch, so this
is the ref as the clone holds it) exits 0 and prints a single trunk tree oid
`00239d98e61dd5be194f78ce284e2623bc0026bf` with no conflict markers — so it is mergeable but not
mergeable-and-correct: **do not land it until findings 1 and 2 are closed**, because it would ship
a publication boundary whose escape-hatch witness can be walked past by a protocol method spelled
as interop and whose naming rule can be walked past by `ns-resolve`, both of which I demonstrated
placing a clock number inside the parity hash with all nineteen invariant tests green.
