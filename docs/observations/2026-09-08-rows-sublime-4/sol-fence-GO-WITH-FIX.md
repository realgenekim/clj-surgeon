+
+# Sol fence: rows sublime batch 4 (`a95ac349`)
+
+Reviewed `astra/namespace-split` at `a95ac349` (`65a542bb` plus trunk merge).
+The review worktree began at `f8cc0c4d`; its tree was byte-identical to
+`a95ac349` (`0a098fb4add3ef3f0177676a356e5366dc8a164c`). I read Astra's report
+and the retained in-batch Sol fence and did not repeat its probes. This delta
+attacked the five requested escape shapes, dual serialization boundaries,
+proof/text agreement, merge counters, and the frozen Cell C inputs.
+
+The fixes are small and are applied in this worktree:
+
+- `src/clj_surgeon/namespace_split.clj` now publishes facade exclusions for
+  macro expansion, arbitrary forwarding bodies, and dynamic resolution.
+- `test/clj_surgeon/namespace_split_test.clj` retains the macro counterexample
+  as a bounded-scope witness without adding a new `deftest` or moving census
+  pins.
+- `docs/intent/helper-extraction/namespace-split-design.md`, `README.md`, and
+  `CHANGELOG.md` name the same boundary.
+
+No design hole remains inside the claimed static/captured transaction contract.
+
+## Five transaction-negative attacks
+
+Executed red-if-undetected tests are retained at
+`/var/tmp/forge/rows4-sol-fence/probes.clj`. They ran as seven tests / 24
+assertions. Three assertions intentionally went red at declared exclusions;
+the other attacks were caught. The exclusions are now all carried by the
+receipt itself.
+
+1. **Stale reference outside roots:** red, as expected. A real source outside
+   `roots=[src,test]` is absent from the transaction snapshot. The receipt does
+   not claim otherwise: `stale_references.scope.roots` names the exact roots,
+   file count, and snapshot hash. This is bounded scope, not a false zero over
+   the repository.
+2. **Reader conditional or string:** a `.cljc` reader conditional containing
+   `app.views/moved` produced two `retired-symbol` sites, one in each branch.
+   The same spelling inside a string produced zero and made the red assertion
+   fail. `stale_references.scope.excludes` already says `strings`, `dynamic
+   resolution`, semicolon prose, and unresolved unqualified symbols.
+3. **Macro facade:** `(defmacro legacy [& xs] \`(u/moved ~@xs))` produced no
+   facade row. That is outside the deliberately syntactic facade grammar, not
+   evidence of absence. Before this review the report/design described that
+   limit, but the structured receipt only carried a positive `scan` sentence.
+   The applied fix adds explicit receipt-local exclusions. General macro
+   expansion remains unclaimed.
+4. **Owner duplicated under another arity:** two top-level `moved` definitions
+   produced `[["moved" 2 0]]` and `passed=false`. Adding an arity within the
+   one destination definition leaves exactly-once at `[["moved" 1 0]]` but
+   independently produces `bodies_preserved equal=0, unequal=1`. The combined
+   prepublication gate therefore refuses either shape.
+5. **Metadata/docstring drift:** changing `moved` metadata and changing its
+   docstring each independently produced `equal=0, unequal=1`. The preservation
+   hash covers complete owner bytes after only the enumerated authorized replay;
+   it is not a body-expression-only hash.
+
+These facts remain transaction facts, not timeless guarantees. Outside roots,
+strings/dynamic construction, macro expansion, and arbitrary forwarding bodies
+remain explicit non-claims.
+
+## Encoding and receipt ceiling
+
+`review-facts` applies lossless JSON string-content encoding to every string in
+the complete negative-fact tree. The hostile probe included a newline in a file
+name plus quote, backslash, U+2028, and U+202E content and round-tripped after
+wrapping once in quotes and JSON-decoding. No raw newline/U+2028/U+202E survived
+in the encoded scalar.
+
+The constructed maximum-fit boundary measured both serializations:
+
+```text
+limit after fixed transport reservation: 65,280 bytes
+fitting EDN:                           65,250 bytes
+fitting escaped JSON:                  65,279 bytes
+charged maximum:                       65,279 bytes
+one more backslash, EDN:               65,252 bytes
+one more backslash, escaped JSON:      65,281 bytes -> receipt-size-bound
+```
+
+Thus the larger escaped-JSON representation controls the boundary; EDN being
+smaller cannot mint publication authority. The final real Cell C pending receipt,
+projected with the new exclusion field, is still below every 65,536-byte public
+ceiling: EDN 62,698; escaped JSON 63,249; full MCP text 63,373 bytes. Headroom is
+2,838 / 2,287 / 2,163 bytes respectively.
+
+## Proof context and text/structured agreement
+
+No disagreement path was found. `proof-context` derives `proof.status` and both
+copies of `next_call` from the finalized `verification_complete` value.
+`committed-facts!` selects those fields together from the immutable original
+receipt. The MCP summary appends the entire escaped JSON result, rather than a
+lossy projection.
+
+For both pending and complete synthetic receipts, and for the actual final Cell C
+pending receipt, parsing the payload after the first text line was equal to the
+structured result. `verification_complete`, `proof.status`, and `next_call` were
+identical; the human header said `proof pending` exactly when structured status
+was pending. This satisfies text superset structured by byte-complete inclusion.
+
+## Merge counters are tree-derived
+
+Confirmed by executing the same source readers the tests call, not by reading the
+literal assertions:
+
+```edn
+{:round-one 1019
+ :adopted 591
+ :total 1610
+ :arithmetic-closes true
+ :non-mcp 246
+ :per-prefix {"WTL-" 53, "PERF-SENT-" 50, "OP-ALG-" 39,
+              "TEST-ISO-" 20, "MEASURE-" 4,
+              "TELEMETRY-EVENTS-" 1, "ROUTING-" 3}}
+```
+
+`deftest-count` reads every manifest namespace source and counts actual top-level
+`deftest` forms. The intent census derives spec documents from the tree, parses
+their IDs, removes `MCP-OP-*`, and independently groups the requested prefixes.
+The literals are ratchet expectations over those derived values, not restatements.
+
+## Frozen Cell C contract
+
+**Yes, unchanged.** Comparing the landed wave-5/6 build `2b39bd37` to
+`a95ac349` gives identical SHA-256 values for the frozen inputs:
+
+```text
+D1-request.edn                         5e7e5052274a773a24005c62108fd831ca415314bc314a93d44d2563704e50ea
+cellC/manifest.edn                     1982c78be180554afdcae7f35c4c3905aaf9fed83cdd86f7b1edd1cfac132f93
+namespace_split_papercut_oracle.py     501ba2ce75fb9d670c09c500cff1c1b25053cc80b9fe0a83577d78ebe777813c
+external cc-oracle.py                  3f37ed778ed6658f60254706edac2db8ee8d82dc7e3fcfbb4e2c4774383a3607
+```
+
+The retained actual receipt still reports map hash
+`b3ac9adc5a060e6c080db9e4b8cb1d21fde589f9dbcc06036c97b3eea588b818`
+and 141 owners / 20 destinations / 87 sites / five caller files. The requested
+fix changes receipt scope disclosure only; it does not change source shape,
+mapping, policy, or oracle.
+
+## Verification
+
+- Warm targeted suites after the fix: 71 tests / 525 assertions, zero failures
+  or errors (`namespace-split-test` and `mcp-namespace-split-test`).
+- `~/bin/clj-kondo` on the two changed Clojure files: zero errors, zero warnings.
+- Formatter and `git diff --check`: pass.
+- Tree-derived counter evaluation: values above, arithmetic closed.
+- No `make test` was run. Astra's retained cold receipt at
+  `/var/tmp/forge/rows-sublime-4/make-test-receipt.json` remains the cold gate:
+  exit 0, 497.989 s, identical before/after source inventory hash
+  `c9bf36fccac68b297e2adc4ab3a75bee49ed45aead8980c745d568326ae26ced`.
+
+Verdict: **GO-WITH-FIX** with the five files named above. The only newly exposed
+escape is a documented, encoded, bounded macro/dynamic non-claim; it is not a
+design hole in the frozen static Cell C contract.
diff --git a/src/clj_surgeon/namespace_split.clj b/src/clj_surgeon/namespace_split.clj
index bf6a10c89c2f3cb5face162b01404811dce465c3..0b2bc573fa4de8e577824df43dd0be0674c4d0b4
--- a/src/clj_surgeon/namespace_split.clj
+++ b/src/clj_surgeon/namespace_split.clj
@@ -927,7 +927,9 @@
        :stale_references {:count (count stale) :sites stale :expected [] :scope scope}
        :facades {:forms facades :expected expected-facades :unexpected unexpected-facades
                  :policy "no-new-forwarders; retain existing unmapped forwarding owners"
-                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets] :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"}}
+                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets]
+                         :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"
+                         :excludes ["macro expansion" "arbitrary forwarding bodies" "dynamic resolution"]}}
        :exactly_once {:columns [:owner :destination_count :elsewhere_count] :rows counts
                       :preexisting_namesakes
                       (mapv (fn [x] (assoc x :after_hashes
diff --git a/test/clj_surgeon/namespace_split_test.clj b/test/clj_surgeon/namespace_split_test.clj
index 66cec67229f25ff49eb1b6b06d7e921e5eb91c8a..0f03fccdd6014131a146c403c7611df8ca4b672b
--- a/test/clj_surgeon/namespace_split_test.clj
+++ b/test/clj_surgeon/namespace_split_test.clj
@@ -937,7 +937,12 @@
                 "(def legacy (fn [x] (u/moved x)))"]]
     (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
                      (str "(ns app.views (:require [app.util :as u]))\n" body "\n"))]
-      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body))))
+      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body)))
+  (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
+                   "(ns app.views (:require [app.util :as u]))\n(defmacro legacy [& xs] `(u/moved ~@xs))\n")
+        fact (:facades (split/review-facts compiled))]
+    (is (= [] (:forms fact)))
+    (is (some #{"macro expansion"} (get-in fact [:scope :excludes])))))
 
 ;; @spec NS-SPLIT-062
 (deftest retention-policy-reports-existing-forwarders

diff --git a//var/tmp/forge/rows4-sol-fence/probes.clj b//var/tmp/forge/rows4-sol-fence/probes.clj
new file mode 100644
index 0000000000000000000000000000000000000000..110b48546318002e08a21bac95e7b83867659d3f
--- /dev/null
+++ b//var/tmp/forge/rows4-sol-fence/probes.clj
@@ -0,0 +1,116 @@
+(ns rows4-sol-fence.probes
+  (:require [cheshire.core :as json]
+            [clj-surgeon.mcp-namespace-split :as tool]
+            [clj-surgeon.namespace-split :as split]
+            [clj-surgeon.namespace-split-test :as fixture]
+            [clj-surgeon.split-proof-gate :as gate]
+            [clojure.string :as str]
+            [clojure.test :refer [deftest is run-tests testing]]))
+
+(defn facts-with [path source]
+  (split/review-facts
+    (-> (fixture/negative-fixture)
+        (assoc-in [:guard-sources path] source)
+        (assoc-in [:future-sources path] source))))
+
+(def boundary-metrics (atom nil))
+
+(deftest stale-reference-scope-escape-turns-red
+  (let [outside "(ns app.escape (:require [app.views :as old]))\n(def escaped (old/moved 1))\n"
+        facts (split/review-facts (fixture/negative-fixture))]
+    (testing "a real source outside declared roots is intentionally absent from the captured transaction"
+      (is (str/includes? outside "old/moved"))
+      (is (pos? (get-in facts [:stale_references :count] 0))
+          "RED: outside-root stale references escape; receipt scope must make that exclusion explicit"))))
+
+(deftest string-and-reader-conditional-stale-reference-probes
+  (let [string-facts (facts-with "test/app/string_escape.clj"
+                                 "(ns app.string-escape)\n(def escaped \"app.views/moved\")\n")
+        reader-facts (facts-with "test/app/reader_escape.cljc"
+                                 "(ns app.reader-escape)\n#?(:clj (def escaped app.views/moved) :cljs (def escaped app.views/moved))\n")]
+    (is (pos? (get-in string-facts [:stale_references :count] 0))
+        "RED: string-spelled references are intentionally excluded and must be disclosed")
+    (is (pos? (get-in reader-facts [:stale_references :count] 0))
+        "reader-conditional qualified symbols must not evade the structural token scan")))
+
+(deftest macro-forwarding-facade-turns-red
+  (let [tick (str (char 96))
+        source (str "(ns app.views (:require [app.util :as u]))\n"
+                    "(defmacro legacy [& xs] " tick "(u/moved ~@xs))\n")
+        facts (facts-with "src/app/views.clj" source)]
+    (is (seq (get-in facts [:facades :forms]))
+        "RED: macro forwarders are outside the facade grammar; receipt scope must say so")))
+
+(deftest different-arity-duplicate-is-caught
+  (let [compiled (fixture/negative-fixture)
+        bad (update-in compiled [:future-sources "src/app/util.clj"]
+                       str "\n(defn moved [x y] (+ x y))\n")
+        facts (split/review-facts bad)]
+    (is (= [["moved" 2 0]] (get-in facts [:exactly_once :rows])))
+    (is (false? (get-in facts [:exactly_once :passed])))))
+
+(deftest metadata-and-docstring-changes-are-body-changes
+  (doseq [[label rewrite]
+          [[:metadata #(str/replace % "(defn moved" "(defn ^:private moved")]
+           [:docstring #(str/replace % "[x]" "\"changed docs\" [x]")]]]
+    (let [compiled (fixture/negative-fixture)
+          bad (update-in compiled [:future-sources "src/app/util.clj"] rewrite)
+          facts (split/review-facts bad)]
+      (is (= 1 (get-in facts [:bodies_preserved :unequal])) (name label)))))
+
+(deftest negative-facts-are-losslessly-encoded-and-dual-format-bounded
+  (let [hostile "quote \" slash \\ literal-newline \\n line-separator\u2028bidi\u202e"
+        path "test/app/hostile\nname.clj"
+        before (str "(ns app.hostile)\n;; " hostile "\n(def escaped \"app.views/moved\")\n")
+        after (str/replace before ";; " ";;  ")
+        facts (split/review-facts
+                (-> (fixture/negative-fixture)
+                    (assoc-in [:guard-sources path] before)
+                    (assoc-in [:future-sources path] after)))
+        encoded (-> facts :comment_edits first :lines first (nth 2))
+        decode #(json/parse-string (str "\"" % "\""))
+        base {:ok true :facts facts}
+        edn-bytes #(alength (.getBytes (pr-str %) "UTF-8"))
+        json-bytes #(alength (.getBytes (json/generate-string % {:escape-non-ascii true}) "UTF-8"))
+        limit (- gate/max-receipt-bytes 256)
+        fitting-n (loop [low 0 high 60000]
+                    (if (= low high)
+                      low
+                      (let [mid (quot (inc (+ low high)) 2)
+                            r (assoc base :padding (apply str (repeat mid "\\")))]
+                        (if (<= (gate/receipt-size r) limit)
+                          (recur mid high)
+                          (recur low (dec mid))))))
+        fit (assoc base :padding (apply str (repeat fitting-n "\\")))
+        over (update fit :padding str "\\")]
+    (reset! boundary-metrics
+            {:edn (edn-bytes fit) :json (json-bytes fit) :max (gate/receipt-size fit)
+             :over-edn (edn-bytes over) :over-json (json-bytes over)
+             :over-max (gate/receipt-size over) :limit limit})
+    (is (= (str ";; " hostile) (decode encoded)))
+    (is (not (re-find #"[\n\u2028\u202e]" encoded)))
+    (is (= (max (edn-bytes fit) (json-bytes fit)) (gate/receipt-size fit)))
+    (is (<= (edn-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (<= (json-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (= fit (gate/bounded-receipt! fit)))
+    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"exceeds 65536"
+                          (gate/bounded-receipt! over)))))
+
+(deftest proof-text-is-the-structured-result
+  (doseq [result [{:ok true :state "committed-probe-only" :verification_complete false
+                   :proof {:tier :warm :status :pending
+                           :next_call {:op :proof-status :receipt "/var/tmp/r.edn"}}}
+                  {:ok true :state "committed" :verification_complete true
+                   :proof {:tier :cold :status :complete :next_call nil}}]]
+    (let [[header payload] (str/split (tool/summary result) #"\n" 2)]
+      (is (= (json/parse-string (json/generate-string result))
+             (json/parse-string payload)))
+      (is (= (:verification_complete result)
+             (get (json/parse-string payload) "verification_complete")))
+      (is (= (name (get-in result [:proof :status]))
+             (get-in (json/parse-string payload) ["proof" "status"])))
+      (is (= (= :pending (get-in result [:proof :status]))
+             (str/includes? header "proof pending"))))))
+
+(def result (run-tests 'rows4-sol-fence.probes))
+result
diff --git a/CHANGELOG.md b/CHANGELOG.md
index 0b12645111b8523053fa39bbdc5a12c1fcec61b3..bfa599293d51973c4129287eff80ca7926422391
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -11,6 +11,7 @@
 - Namespace-split receipts now carry candidate comment edits, scoped stale-reference
   and facade scans, actual per-owner definition counts, and body hashes separating
   raw equality from authorized replay. Contradictory evidence refuses before write.
+  Facade scope explicitly names macro/dynamic exclusions.
   Committed incomplete receipts explain proof tier/status and name `:proof-status`
   as the next read; `verification_complete` retains its existing meaning.
 
diff --git a/README.md b/README.md
index 2e8eff2df58925f2e6f07dababf92d605c9b4054..7dc749e80219cd76555ce6194d4750f8340a0f3a
--- a/README.md
+++ b/README.md
@@ -842,6 +842,8 @@
   `defn`/`apply` forwarders,
   with retention-policy expectations and scan scope. Existing unmapped forwarders
   are listed in `:expected`; new wrappers appear in `:unexpected` and refuse.
+  The scope explicitly excludes macro expansion, arbitrary forwarding bodies and
+  dynamic resolution.
 - `:exactly_once`: per-owner destination/elsewhere counts. Pre-existing
   namesakes in unrelated namespaces have separate file/hash evidence; adding
   another copy still fails.
diff --git a/docs/intent/helper-extraction/namespace-split-design.md b/docs/intent/helper-extraction/namespace-split-design.md
index ab007ace2b6d710e161a62fe50c7b2c39762c373..b129608f6938a10b7be9466b2ba039316327a6e7
--- a/docs/intent/helper-extraction/namespace-split-design.md
+++ b/docs/intent/helper-extraction/namespace-split-design.md
@@ -476,6 +476,8 @@
 scans cover symbol, Var, partial and fn aliases plus single/multi-arity direct
 call/apply bodies. Retention explicitly permits existing unmapped forwarding
 owners, reports them as expected, and refuses newly introduced wrappers.
+The receipt scope explicitly excludes macro expansion, arbitrary forwarding
+bodies and dynamic resolution.
 
 The delta fence also binds permitted forwarders to canonical mapped targets and
 forwarding-kind/arity positions. Any forwarding arity is enumerated, including
diff --git a/docs/observations/rows4-fence.md b/docs/observations/rows4-fence.md
new file mode 100644
index 0000000000000000000000000000000000000000..5c053387822384d340eb66148b2b6b1ac59bd040
--- /dev/null
+++ b/docs/observations/rows4-fence.md
@@ -0,0 +1,154 @@
+GO-WITH-FIX
+
+# Sol fence: rows sublime batch 4 (`a95ac349`)
+
+Reviewed `astra/namespace-split` at `a95ac349` (`65a542bb` plus trunk merge).
+The review worktree began at `f8cc0c4d`; its tree was byte-identical to
+`a95ac349` (`0a098fb4add3ef3f0177676a356e5366dc8a164c`). I read Astra's report
+and the retained in-batch Sol fence and did not repeat its probes. This delta
+attacked the five requested escape shapes, dual serialization boundaries,
+proof/text agreement, merge counters, and the frozen Cell C inputs.
+
+The fixes are small and are applied in this worktree:
+
+- `src/clj_surgeon/namespace_split.clj` now publishes facade exclusions for
+  macro expansion, arbitrary forwarding bodies, and dynamic resolution.
+- `test/clj_surgeon/namespace_split_test.clj` retains the macro counterexample
+  as a bounded-scope witness without adding a new `deftest` or moving census
+  pins.
+- `docs/intent/helper-extraction/namespace-split-design.md`, `README.md`, and
+  `CHANGELOG.md` name the same boundary.
+
+No design hole remains inside the claimed static/captured transaction contract.
+
+## Five transaction-negative attacks
+
+Executed red-if-undetected tests are retained at
+`/var/tmp/forge/rows4-sol-fence/probes.clj`. They ran as seven tests / 24
+assertions. Three assertions intentionally went red at declared exclusions;
+the other attacks were caught. The exclusions are now all carried by the
+receipt itself.
+
+1. **Stale reference outside roots:** red, as expected. A real source outside
+   `roots=[src,test]` is absent from the transaction snapshot. The receipt does
+   not claim otherwise: `stale_references.scope.roots` names the exact roots,
+   file count, and snapshot hash. This is bounded scope, not a false zero over
+   the repository.
+2. **Reader conditional or string:** a `.cljc` reader conditional containing
+   `app.views/moved` produced two `retired-symbol` sites, one in each branch.
+   The same spelling inside a string produced zero and made the red assertion
+   fail. `stale_references.scope.excludes` already says `strings`, `dynamic
+   resolution`, semicolon prose, and unresolved unqualified symbols.
+3. **Macro facade:** `(defmacro legacy [& xs] \`(u/moved ~@xs))` produced no
+   facade row. That is outside the deliberately syntactic facade grammar, not
+   evidence of absence. Before this review the report/design described that
+   limit, but the structured receipt only carried a positive `scan` sentence.
+   The applied fix adds explicit receipt-local exclusions. General macro
+   expansion remains unclaimed.
+4. **Owner duplicated under another arity:** two top-level `moved` definitions
+   produced `[["moved" 2 0]]` and `passed=false`. Adding an arity within the
+   one destination definition leaves exactly-once at `[["moved" 1 0]]` but
+   independently produces `bodies_preserved equal=0, unequal=1`. The combined
+   prepublication gate therefore refuses either shape.
+5. **Metadata/docstring drift:** changing `moved` metadata and changing its
+   docstring each independently produced `equal=0, unequal=1`. The preservation
+   hash covers complete owner bytes after only the enumerated authorized replay;
+   it is not a body-expression-only hash.
+
+These facts remain transaction facts, not timeless guarantees. Outside roots,
+strings/dynamic construction, macro expansion, and arbitrary forwarding bodies
+remain explicit non-claims.
+
+## Encoding and receipt ceiling
+
+`review-facts` applies lossless JSON string-content encoding to every string in
+the complete negative-fact tree. The hostile probe included a newline in a file
+name plus quote, backslash, U+2028, and U+202E content and round-tripped after
+wrapping once in quotes and JSON-decoding. No raw newline/U+2028/U+202E survived
+in the encoded scalar.
+
+The constructed maximum-fit boundary measured both serializations:
+
+```text
+limit after fixed transport reservation: 65,280 bytes
+fitting EDN:                           65,250 bytes
+fitting escaped JSON:                  65,279 bytes
+charged maximum:                       65,279 bytes
+one more backslash, EDN:               65,252 bytes
+one more backslash, escaped JSON:      65,281 bytes -> receipt-size-bound
+```
+
+Thus the larger escaped-JSON representation controls the boundary; EDN being
+smaller cannot mint publication authority. The final real Cell C pending receipt,
+projected with the new exclusion field, is still below every 65,536-byte public
+ceiling: EDN 62,698; escaped JSON 63,249; full MCP text 63,373 bytes. Headroom is
+2,838 / 2,287 / 2,163 bytes respectively.
+
+## Proof context and text/structured agreement
+
+No disagreement path was found. `proof-context` derives `proof.status` and both
+copies of `next_call` from the finalized `verification_complete` value.
+`committed-facts!` selects those fields together from the immutable original
+receipt. The MCP summary appends the entire escaped JSON result, rather than a
+lossy projection.
+
+For both pending and complete synthetic receipts, and for the actual final Cell C
+pending receipt, parsing the payload after the first text line was equal to the
+structured result. `verification_complete`, `proof.status`, and `next_call` were
+identical; the human header said `proof pending` exactly when structured status
+was pending. This satisfies text superset structured by byte-complete inclusion.
+
+## Merge counters are tree-derived
+
+Confirmed by executing the same source readers the tests call, not by reading the
+literal assertions:
+
+```edn
+{:round-one 1019
+ :adopted 591
+ :total 1610
+ :arithmetic-closes true
+ :non-mcp 246
+ :per-prefix {"WTL-" 53, "PERF-SENT-" 50, "OP-ALG-" 39,
+              "TEST-ISO-" 20, "MEASURE-" 4,
+              "TELEMETRY-EVENTS-" 1, "ROUTING-" 3}}
+```
+
+`deftest-count` reads every manifest namespace source and counts actual top-level
+`deftest` forms. The intent census derives spec documents from the tree, parses
+their IDs, removes `MCP-OP-*`, and independently groups the requested prefixes.
+The literals are ratchet expectations over those derived values, not restatements.
+
+## Frozen Cell C contract
+
+**Yes, unchanged.** Comparing the landed wave-5/6 build `2b39bd37` to
+`a95ac349` gives identical SHA-256 values for the frozen inputs:
+
+```text
+D1-request.edn                         5e7e5052274a773a24005c62108fd831ca415314bc314a93d44d2563704e50ea
+cellC/manifest.edn                     1982c78be180554afdcae7f35c4c3905aaf9fed83cdd86f7b1edd1cfac132f93
+namespace_split_papercut_oracle.py     501ba2ce75fb9d670c09c500cff1c1b25053cc80b9fe0a83577d78ebe777813c
+external cc-oracle.py                  3f37ed778ed6658f60254706edac2db8ee8d82dc7e3fcfbb4e2c4774383a3607
+```
+
+The retained actual receipt still reports map hash
+`b3ac9adc5a060e6c080db9e4b8cb1d21fde589f9dbcc06036c97b3eea588b818`
+and 141 owners / 20 destinations / 87 sites / five caller files. The requested
+fix changes receipt scope disclosure only; it does not change source shape,
+mapping, policy, or oracle.
+
+## Verification
+
+- Warm targeted suites after the fix: 71 tests / 525 assertions, zero failures
+  or errors (`namespace-split-test` and `mcp-namespace-split-test`).
+- `~/bin/clj-kondo` on the two changed Clojure files: zero errors, zero warnings.
+- Formatter and `git diff --check`: pass.
+- Tree-derived counter evaluation: values above, arithmetic closed.
+- No `make test` was run. Astra's retained cold receipt at
+  `/var/tmp/forge/rows-sublime-4/make-test-receipt.json` remains the cold gate:
+  exit 0, 497.989 s, identical before/after source inventory hash
+  `c9bf36fccac68b297e2adc4ab3a75bee49ed45aead8980c745d568326ae26ced`.
+
+Verdict: **GO-WITH-FIX** with the five files named above. The only newly exposed
+escape is a documented, encoded, bounded macro/dynamic non-claim; it is not a
+design hole in the frozen static Cell C contract.
diff --git a/src/clj_surgeon/namespace_split.clj b/src/clj_surgeon/namespace_split.clj
index bf6a10c89c2f3cb5face162b01404811dce465c3..0b2bc573fa4de8e577824df43dd0be0674c4d0b4
--- a/src/clj_surgeon/namespace_split.clj
+++ b/src/clj_surgeon/namespace_split.clj
@@ -927,7 +927,9 @@
        :stale_references {:count (count stale) :sites stale :expected [] :scope scope}
        :facades {:forms facades :expected expected-facades :unexpected unexpected-facades
                  :policy "no-new-forwarders; retain existing unmapped forwarding owners"
-                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets] :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"}}
+                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets]
+                         :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"
+                         :excludes ["macro expansion" "arbitrary forwarding bodies" "dynamic resolution"]}}
        :exactly_once {:columns [:owner :destination_count :elsewhere_count] :rows counts
                       :preexisting_namesakes
                       (mapv (fn [x] (assoc x :after_hashes
diff --git a/test/clj_surgeon/namespace_split_test.clj b/test/clj_surgeon/namespace_split_test.clj
index 66cec67229f25ff49eb1b6b06d7e921e5eb91c8a..0f03fccdd6014131a146c403c7611df8ca4b672b
--- a/test/clj_surgeon/namespace_split_test.clj
+++ b/test/clj_surgeon/namespace_split_test.clj
@@ -937,7 +937,12 @@
                 "(def legacy (fn [x] (u/moved x)))"]]
     (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
                      (str "(ns app.views (:require [app.util :as u]))\n" body "\n"))]
-      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body))))
+      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body)))
+  (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
+                   "(ns app.views (:require [app.util :as u]))\n(defmacro legacy [& xs] `(u/moved ~@xs))\n")
+        fact (:facades (split/review-facts compiled))]
+    (is (= [] (:forms fact)))
+    (is (some #{"macro expansion"} (get-in fact [:scope :excludes])))))
 
 ;; @spec NS-SPLIT-062
 (deftest retention-policy-reports-existing-forwarders

2026-09-08T21:10:20.192200Z ERROR codex_models_manager::manager: failed to renew cache TTL: missing field `supports_parallel_tool_calls` at line 132 column 5
 exited 1 in 1199923ms:
cd /home/forge/src/clj-surgeon-fence/ && clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test:clj-surgeon/nrepl
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
nREPL server started on port 36881 on host localhost - nrepl://localhost:36881
^Cmake: *** [Makefile:732: nrepl] Interrupt

diff --git a//var/tmp/forge/rows4-sol-fence/probes.clj b//var/tmp/forge/rows4-sol-fence/probes.clj
new file mode 100644
index 0000000000000000000000000000000000000000..110b48546318002e08a21bac95e7b83867659d3f
--- /dev/null
+++ b//var/tmp/forge/rows4-sol-fence/probes.clj
@@ -0,0 +1,116 @@
+(ns rows4-sol-fence.probes
+  (:require [cheshire.core :as json]
+            [clj-surgeon.mcp-namespace-split :as tool]
+            [clj-surgeon.namespace-split :as split]
+            [clj-surgeon.namespace-split-test :as fixture]
+            [clj-surgeon.split-proof-gate :as gate]
+            [clojure.string :as str]
+            [clojure.test :refer [deftest is run-tests testing]]))
+
+(defn facts-with [path source]
+  (split/review-facts
+    (-> (fixture/negative-fixture)
+        (assoc-in [:guard-sources path] source)
+        (assoc-in [:future-sources path] source))))
+
+(def boundary-metrics (atom nil))
+
+(deftest stale-reference-scope-escape-turns-red
+  (let [outside "(ns app.escape (:require [app.views :as old]))\n(def escaped (old/moved 1))\n"
+        facts (split/review-facts (fixture/negative-fixture))]
+    (testing "a real source outside declared roots is intentionally absent from the captured transaction"
+      (is (str/includes? outside "old/moved"))
+      (is (pos? (get-in facts [:stale_references :count] 0))
+          "RED: outside-root stale references escape; receipt scope must make that exclusion explicit"))))
+
+(deftest string-and-reader-conditional-stale-reference-probes
+  (let [string-facts (facts-with "test/app/string_escape.clj"
+                                 "(ns app.string-escape)\n(def escaped \"app.views/moved\")\n")
+        reader-facts (facts-with "test/app/reader_escape.cljc"
+                                 "(ns app.reader-escape)\n#?(:clj (def escaped app.views/moved) :cljs (def escaped app.views/moved))\n")]
+    (is (pos? (get-in string-facts [:stale_references :count] 0))
+        "RED: string-spelled references are intentionally excluded and must be disclosed")
+    (is (pos? (get-in reader-facts [:stale_references :count] 0))
+        "reader-conditional qualified symbols must not evade the structural token scan")))
+
+(deftest macro-forwarding-facade-turns-red
+  (let [tick (str (char 96))
+        source (str "(ns app.views (:require [app.util :as u]))\n"
+                    "(defmacro legacy [& xs] " tick "(u/moved ~@xs))\n")
+        facts (facts-with "src/app/views.clj" source)]
+    (is (seq (get-in facts [:facades :forms]))
+        "RED: macro forwarders are outside the facade grammar; receipt scope must say so")))
+
+(deftest different-arity-duplicate-is-caught
+  (let [compiled (fixture/negative-fixture)
+        bad (update-in compiled [:future-sources "src/app/util.clj"]
+                       str "\n(defn moved [x y] (+ x y))\n")
+        facts (split/review-facts bad)]
+    (is (= [["moved" 2 0]] (get-in facts [:exactly_once :rows])))
+    (is (false? (get-in facts [:exactly_once :passed])))))
+
+(deftest metadata-and-docstring-changes-are-body-changes
+  (doseq [[label rewrite]
+          [[:metadata #(str/replace % "(defn moved" "(defn ^:private moved")]
+           [:docstring #(str/replace % "[x]" "\"changed docs\" [x]")]]]
+    (let [compiled (fixture/negative-fixture)
+          bad (update-in compiled [:future-sources "src/app/util.clj"] rewrite)
+          facts (split/review-facts bad)]
+      (is (= 1 (get-in facts [:bodies_preserved :unequal])) (name label)))))
+
+(deftest negative-facts-are-losslessly-encoded-and-dual-format-bounded
+  (let [hostile "quote \" slash \\ literal-newline \\n line-separator\u2028bidi\u202e"
+        path "test/app/hostile\nname.clj"
+        before (str "(ns app.hostile)\n;; " hostile "\n(def escaped \"app.views/moved\")\n")
+        after (str/replace before ";; " ";;  ")
+        facts (split/review-facts
+                (-> (fixture/negative-fixture)
+                    (assoc-in [:guard-sources path] before)
+                    (assoc-in [:future-sources path] after)))
+        encoded (-> facts :comment_edits first :lines first (nth 2))
+        decode #(json/parse-string (str "\"" % "\""))
+        base {:ok true :facts facts}
+        edn-bytes #(alength (.getBytes (pr-str %) "UTF-8"))
+        json-bytes #(alength (.getBytes (json/generate-string % {:escape-non-ascii true}) "UTF-8"))
+        limit (- gate/max-receipt-bytes 256)
+        fitting-n (loop [low 0 high 60000]
+                    (if (= low high)
+                      low
+                      (let [mid (quot (inc (+ low high)) 2)
+                            r (assoc base :padding (apply str (repeat mid "\\")))]
+                        (if (<= (gate/receipt-size r) limit)
+                          (recur mid high)
+                          (recur low (dec mid))))))
+        fit (assoc base :padding (apply str (repeat fitting-n "\\")))
+        over (update fit :padding str "\\")]
+    (reset! boundary-metrics
+            {:edn (edn-bytes fit) :json (json-bytes fit) :max (gate/receipt-size fit)
+             :over-edn (edn-bytes over) :over-json (json-bytes over)
+             :over-max (gate/receipt-size over) :limit limit})
+    (is (= (str ";; " hostile) (decode encoded)))
+    (is (not (re-find #"[\n\u2028\u202e]" encoded)))
+    (is (= (max (edn-bytes fit) (json-bytes fit)) (gate/receipt-size fit)))
+    (is (<= (edn-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (<= (json-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (= fit (gate/bounded-receipt! fit)))
+    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"exceeds 65536"
+                          (gate/bounded-receipt! over)))))
+
+(deftest proof-text-is-the-structured-result
+  (doseq [result [{:ok true :state "committed-probe-only" :verification_complete false
+                   :proof {:tier :warm :status :pending
+                           :next_call {:op :proof-status :receipt "/var/tmp/r.edn"}}}
+                  {:ok true :state "committed" :verification_complete true
+                   :proof {:tier :cold :status :complete :next_call nil}}]]
+    (let [[header payload] (str/split (tool/summary result) #"\n" 2)]
+      (is (= (json/parse-string (json/generate-string result))
+             (json/parse-string payload)))
+      (is (= (:verification_complete result)
+             (get (json/parse-string payload) "verification_complete")))
+      (is (= (name (get-in result [:proof :status]))
+             (get-in (json/parse-string payload) ["proof" "status"])))
+      (is (= (= :pending (get-in result [:proof :status]))
+             (str/includes? header "proof pending"))))))
+
+(def result (run-tests 'rows4-sol-fence.probes))
+result
diff --git a/CHANGELOG.md b/CHANGELOG.md
index 0b12645111b8523053fa39bbdc5a12c1fcec61b3..bfa599293d51973c4129287eff80ca7926422391
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -11,6 +11,7 @@
 - Namespace-split receipts now carry candidate comment edits, scoped stale-reference
   and facade scans, actual per-owner definition counts, and body hashes separating
   raw equality from authorized replay. Contradictory evidence refuses before write.
+  Facade scope explicitly names macro/dynamic exclusions.
   Committed incomplete receipts explain proof tier/status and name `:proof-status`
   as the next read; `verification_complete` retains its existing meaning.
 
diff --git a/README.md b/README.md
index 2e8eff2df58925f2e6f07dababf92d605c9b4054..7dc749e80219cd76555ce6194d4750f8340a0f3a
--- a/README.md
+++ b/README.md
@@ -842,6 +842,8 @@
   `defn`/`apply` forwarders,
   with retention-policy expectations and scan scope. Existing unmapped forwarders
   are listed in `:expected`; new wrappers appear in `:unexpected` and refuse.
+  The scope explicitly excludes macro expansion, arbitrary forwarding bodies and
+  dynamic resolution.
 - `:exactly_once`: per-owner destination/elsewhere counts. Pre-existing
   namesakes in unrelated namespaces have separate file/hash evidence; adding
   another copy still fails.
diff --git a/docs/intent/helper-extraction/namespace-split-design.md b/docs/intent/helper-extraction/namespace-split-design.md
index ab007ace2b6d710e161a62fe50c7b2c39762c373..b129608f6938a10b7be9466b2ba039316327a6e7
--- a/docs/intent/helper-extraction/namespace-split-design.md
+++ b/docs/intent/helper-extraction/namespace-split-design.md
@@ -476,6 +476,8 @@
 scans cover symbol, Var, partial and fn aliases plus single/multi-arity direct
 call/apply bodies. Retention explicitly permits existing unmapped forwarding
 owners, reports them as expected, and refuses newly introduced wrappers.
+The receipt scope explicitly excludes macro expansion, arbitrary forwarding
+bodies and dynamic resolution.
 
 The delta fence also binds permitted forwarders to canonical mapped targets and
 forwarding-kind/arity positions. Any forwarding arity is enumerated, including
diff --git a/docs/observations/rows4-fence.md b/docs/observations/rows4-fence.md
new file mode 100644
index 0000000000000000000000000000000000000000..5c053387822384d340eb66148b2b6b1ac59bd040
--- /dev/null
+++ b/docs/observations/rows4-fence.md
@@ -0,0 +1,154 @@
+GO-WITH-FIX
+
+# Sol fence: rows sublime batch 4 (`a95ac349`)
+
+Reviewed `astra/namespace-split` at `a95ac349` (`65a542bb` plus trunk merge).
+The review worktree began at `f8cc0c4d`; its tree was byte-identical to
+`a95ac349` (`0a098fb4add3ef3f0177676a356e5366dc8a164c`). I read Astra's report
+and the retained in-batch Sol fence and did not repeat its probes. This delta
+attacked the five requested escape shapes, dual serialization boundaries,
+proof/text agreement, merge counters, and the frozen Cell C inputs.
+
+The fixes are small and are applied in this worktree:
+
+- `src/clj_surgeon/namespace_split.clj` now publishes facade exclusions for
+  macro expansion, arbitrary forwarding bodies, and dynamic resolution.
+- `test/clj_surgeon/namespace_split_test.clj` retains the macro counterexample
+  as a bounded-scope witness without adding a new `deftest` or moving census
+  pins.
+- `docs/intent/helper-extraction/namespace-split-design.md`, `README.md`, and
+  `CHANGELOG.md` name the same boundary.
+
+No design hole remains inside the claimed static/captured transaction contract.
+
+## Five transaction-negative attacks
+
+Executed red-if-undetected tests are retained at
+`/var/tmp/forge/rows4-sol-fence/probes.clj`. They ran as seven tests / 24
+assertions. Three assertions intentionally went red at declared exclusions;
+the other attacks were caught. The exclusions are now all carried by the
+receipt itself.
+
+1. **Stale reference outside roots:** red, as expected. A real source outside
+   `roots=[src,test]` is absent from the transaction snapshot. The receipt does
+   not claim otherwise: `stale_references.scope.roots` names the exact roots,
+   file count, and snapshot hash. This is bounded scope, not a false zero over
+   the repository.
+2. **Reader conditional or string:** a `.cljc` reader conditional containing
+   `app.views/moved` produced two `retired-symbol` sites, one in each branch.
+   The same spelling inside a string produced zero and made the red assertion
+   fail. `stale_references.scope.excludes` already says `strings`, `dynamic
+   resolution`, semicolon prose, and unresolved unqualified symbols.
+3. **Macro facade:** `(defmacro legacy [& xs] \`(u/moved ~@xs))` produced no
+   facade row. That is outside the deliberately syntactic facade grammar, not
+   evidence of absence. Before this review the report/design described that
+   limit, but the structured receipt only carried a positive `scan` sentence.
+   The applied fix adds explicit receipt-local exclusions. General macro
+   expansion remains unclaimed.
+4. **Owner duplicated under another arity:** two top-level `moved` definitions
+   produced `[["moved" 2 0]]` and `passed=false`. Adding an arity within the
+   one destination definition leaves exactly-once at `[["moved" 1 0]]` but
+   independently produces `bodies_preserved equal=0, unequal=1`. The combined
+   prepublication gate therefore refuses either shape.
+5. **Metadata/docstring drift:** changing `moved` metadata and changing its
+   docstring each independently produced `equal=0, unequal=1`. The preservation
+   hash covers complete owner bytes after only the enumerated authorized replay;
+   it is not a body-expression-only hash.
+
+These facts remain transaction facts, not timeless guarantees. Outside roots,
+strings/dynamic construction, macro expansion, and arbitrary forwarding bodies
+remain explicit non-claims.
+
+## Encoding and receipt ceiling
+
+`review-facts` applies lossless JSON string-content encoding to every string in
+the complete negative-fact tree. The hostile probe included a newline in a file
+name plus quote, backslash, U+2028, and U+202E content and round-tripped after
+wrapping once in quotes and JSON-decoding. No raw newline/U+2028/U+202E survived
+in the encoded scalar.
+
+The constructed maximum-fit boundary measured both serializations:
+
+```text
+limit after fixed transport reservation: 65,280 bytes
+fitting EDN:                           65,250 bytes
+fitting escaped JSON:                  65,279 bytes
+charged maximum:                       65,279 bytes
+one more backslash, EDN:               65,252 bytes
+one more backslash, escaped JSON:      65,281 bytes -> receipt-size-bound
+```
+
+Thus the larger escaped-JSON representation controls the boundary; EDN being
+smaller cannot mint publication authority. The final real Cell C pending receipt,
+projected with the new exclusion field, is still below every 65,536-byte public
+ceiling: EDN 62,698; escaped JSON 63,249; full MCP text 63,373 bytes. Headroom is
+2,838 / 2,287 / 2,163 bytes respectively.
+
+## Proof context and text/structured agreement
+
+No disagreement path was found. `proof-context` derives `proof.status` and both
+copies of `next_call` from the finalized `verification_complete` value.
+`committed-facts!` selects those fields together from the immutable original
+receipt. The MCP summary appends the entire escaped JSON result, rather than a
+lossy projection.
+
+For both pending and complete synthetic receipts, and for the actual final Cell C
+pending receipt, parsing the payload after the first text line was equal to the
+structured result. `verification_complete`, `proof.status`, and `next_call` were
+identical; the human header said `proof pending` exactly when structured status
+was pending. This satisfies text superset structured by byte-complete inclusion.
+
+## Merge counters are tree-derived
+
+Confirmed by executing the same source readers the tests call, not by reading the
+literal assertions:
+
+```edn
+{:round-one 1019
+ :adopted 591
+ :total 1610
+ :arithmetic-closes true
+ :non-mcp 246
+ :per-prefix {"WTL-" 53, "PERF-SENT-" 50, "OP-ALG-" 39,
+              "TEST-ISO-" 20, "MEASURE-" 4,
+              "TELEMETRY-EVENTS-" 1, "ROUTING-" 3}}
+```
+
+`deftest-count` reads every manifest namespace source and counts actual top-level
+`deftest` forms. The intent census derives spec documents from the tree, parses
+their IDs, removes `MCP-OP-*`, and independently groups the requested prefixes.
+The literals are ratchet expectations over those derived values, not restatements.
+
+## Frozen Cell C contract
+
+**Yes, unchanged.** Comparing the landed wave-5/6 build `2b39bd37` to
+`a95ac349` gives identical SHA-256 values for the frozen inputs:
+
+```text
+D1-request.edn                         5e7e5052274a773a24005c62108fd831ca415314bc314a93d44d2563704e50ea
+cellC/manifest.edn                     1982c78be180554afdcae7f35c4c3905aaf9fed83cdd86f7b1edd1cfac132f93
+namespace_split_papercut_oracle.py     501ba2ce75fb9d670c09c500cff1c1b25053cc80b9fe0a83577d78ebe777813c
+external cc-oracle.py                  3f37ed778ed6658f60254706edac2db8ee8d82dc7e3fcfbb4e2c4774383a3607
+```
+
+The retained actual receipt still reports map hash
+`b3ac9adc5a060e6c080db9e4b8cb1d21fde589f9dbcc06036c97b3eea588b818`
+and 141 owners / 20 destinations / 87 sites / five caller files. The requested
+fix changes receipt scope disclosure only; it does not change source shape,
+mapping, policy, or oracle.
+
+## Verification
+
+- Warm targeted suites after the fix: 71 tests / 525 assertions, zero failures
+  or errors (`namespace-split-test` and `mcp-namespace-split-test`).
+- `~/bin/clj-kondo` on the two changed Clojure files: zero errors, zero warnings.
+- Formatter and `git diff --check`: pass.
+- Tree-derived counter evaluation: values above, arithmetic closed.
+- No `make test` was run. Astra's retained cold receipt at
+  `/var/tmp/forge/rows-sublime-4/make-test-receipt.json` remains the cold gate:
+  exit 0, 497.989 s, identical before/after source inventory hash
+  `c9bf36fccac68b297e2adc4ab3a75bee49ed45aead8980c745d568326ae26ced`.
+
+Verdict: **GO-WITH-FIX** with the five files named above. The only newly exposed
+escape is a documented, encoded, bounded macro/dynamic non-claim; it is not a
+design hole in the frozen static Cell C contract.
diff --git a/src/clj_surgeon/namespace_split.clj b/src/clj_surgeon/namespace_split.clj
index bf6a10c89c2f3cb5face162b01404811dce465c3..0b2bc573fa4de8e577824df43dd0be0674c4d0b4
--- a/src/clj_surgeon/namespace_split.clj
+++ b/src/clj_surgeon/namespace_split.clj
@@ -927,7 +927,9 @@
        :stale_references {:count (count stale) :sites stale :expected [] :scope scope}
        :facades {:forms facades :expected expected-facades :unexpected unexpected-facades
                  :policy "no-new-forwarders; retain existing unmapped forwarding owners"
-                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets] :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"}}
+                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets]
+                         :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"
+                         :excludes ["macro expansion" "arbitrary forwarding bodies" "dynamic resolution"]}}
        :exactly_once {:columns [:owner :destination_count :elsewhere_count] :rows counts
                       :preexisting_namesakes
                       (mapv (fn [x] (assoc x :after_hashes
diff --git a/test/clj_surgeon/namespace_split_test.clj b/test/clj_surgeon/namespace_split_test.clj
index 66cec67229f25ff49eb1b6b06d7e921e5eb91c8a..0f03fccdd6014131a146c403c7611df8ca4b672b
--- a/test/clj_surgeon/namespace_split_test.clj
+++ b/test/clj_surgeon/namespace_split_test.clj
@@ -937,7 +937,12 @@
                 "(def legacy (fn [x] (u/moved x)))"]]
     (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
                      (str "(ns app.views (:require [app.util :as u]))\n" body "\n"))]
-      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body))))
+      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body)))
+  (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
+                   "(ns app.views (:require [app.util :as u]))\n(defmacro legacy [& xs] `(u/moved ~@xs))\n")
+        fact (:facades (split/review-facts compiled))]
+    (is (= [] (:forms fact)))
+    (is (some #{"macro expansion"} (get-in fact [:scope :excludes])))))
 
 ;; @spec NS-SPLIT-062
 (deftest retention-policy-reports-existing-forwarders

exec
/bin/bash -lc "sed -n '1,24p' docs/observations/rows4-fence.md && git diff --check && git status --short && clj-nrepl-eval --discover-ports" in /home/forge/src/clj-surgeon-fence
 succeeded in 94ms:
Discovered nREPL servers:

In other directories:
  localhost:36013 (clj) - /var/tmp/forge/coldstart/canary-2026-09-08-mvr-sol-W/wt
  localhost:38759 (clj) - /var/tmp/forge/item4b-fx/mvr
  localhost:35487 (clj) - /var/tmp/forge/item4b-fx/cc

Total: 3 servers

diff --git a//var/tmp/forge/rows4-sol-fence/probes.clj b//var/tmp/forge/rows4-sol-fence/probes.clj
new file mode 100644
index 0000000000000000000000000000000000000000..110b48546318002e08a21bac95e7b83867659d3f
--- /dev/null
+++ b//var/tmp/forge/rows4-sol-fence/probes.clj
@@ -0,0 +1,116 @@
+(ns rows4-sol-fence.probes
+  (:require [cheshire.core :as json]
+            [clj-surgeon.mcp-namespace-split :as tool]
+            [clj-surgeon.namespace-split :as split]
+            [clj-surgeon.namespace-split-test :as fixture]
+            [clj-surgeon.split-proof-gate :as gate]
+            [clojure.string :as str]
+            [clojure.test :refer [deftest is run-tests testing]]))
+
+(defn facts-with [path source]
+  (split/review-facts
+    (-> (fixture/negative-fixture)
+        (assoc-in [:guard-sources path] source)
+        (assoc-in [:future-sources path] source))))
+
+(def boundary-metrics (atom nil))
+
+(deftest stale-reference-scope-escape-turns-red
+  (let [outside "(ns app.escape (:require [app.views :as old]))\n(def escaped (old/moved 1))\n"
+        facts (split/review-facts (fixture/negative-fixture))]
+    (testing "a real source outside declared roots is intentionally absent from the captured transaction"
+      (is (str/includes? outside "old/moved"))
+      (is (pos? (get-in facts [:stale_references :count] 0))
+          "RED: outside-root stale references escape; receipt scope must make that exclusion explicit"))))
+
+(deftest string-and-reader-conditional-stale-reference-probes
+  (let [string-facts (facts-with "test/app/string_escape.clj"
+                                 "(ns app.string-escape)\n(def escaped \"app.views/moved\")\n")
+        reader-facts (facts-with "test/app/reader_escape.cljc"
+                                 "(ns app.reader-escape)\n#?(:clj (def escaped app.views/moved) :cljs (def escaped app.views/moved))\n")]
+    (is (pos? (get-in string-facts [:stale_references :count] 0))
+        "RED: string-spelled references are intentionally excluded and must be disclosed")
+    (is (pos? (get-in reader-facts [:stale_references :count] 0))
+        "reader-conditional qualified symbols must not evade the structural token scan")))
+
+(deftest macro-forwarding-facade-turns-red
+  (let [tick (str (char 96))
+        source (str "(ns app.views (:require [app.util :as u]))\n"
+                    "(defmacro legacy [& xs] " tick "(u/moved ~@xs))\n")
+        facts (facts-with "src/app/views.clj" source)]
+    (is (seq (get-in facts [:facades :forms]))
+        "RED: macro forwarders are outside the facade grammar; receipt scope must say so")))
+
+(deftest different-arity-duplicate-is-caught
+  (let [compiled (fixture/negative-fixture)
+        bad (update-in compiled [:future-sources "src/app/util.clj"]
+                       str "\n(defn moved [x y] (+ x y))\n")
+        facts (split/review-facts bad)]
+    (is (= [["moved" 2 0]] (get-in facts [:exactly_once :rows])))
+    (is (false? (get-in facts [:exactly_once :passed])))))
+
+(deftest metadata-and-docstring-changes-are-body-changes
+  (doseq [[label rewrite]
+          [[:metadata #(str/replace % "(defn moved" "(defn ^:private moved")]
+           [:docstring #(str/replace % "[x]" "\"changed docs\" [x]")]]]
+    (let [compiled (fixture/negative-fixture)
+          bad (update-in compiled [:future-sources "src/app/util.clj"] rewrite)
+          facts (split/review-facts bad)]
+      (is (= 1 (get-in facts [:bodies_preserved :unequal])) (name label)))))
+
+(deftest negative-facts-are-losslessly-encoded-and-dual-format-bounded
+  (let [hostile "quote \" slash \\ literal-newline \\n line-separator\u2028bidi\u202e"
+        path "test/app/hostile\nname.clj"
+        before (str "(ns app.hostile)\n;; " hostile "\n(def escaped \"app.views/moved\")\n")
+        after (str/replace before ";; " ";;  ")
+        facts (split/review-facts
+                (-> (fixture/negative-fixture)
+                    (assoc-in [:guard-sources path] before)
+                    (assoc-in [:future-sources path] after)))
+        encoded (-> facts :comment_edits first :lines first (nth 2))
+        decode #(json/parse-string (str "\"" % "\""))
+        base {:ok true :facts facts}
+        edn-bytes #(alength (.getBytes (pr-str %) "UTF-8"))
+        json-bytes #(alength (.getBytes (json/generate-string % {:escape-non-ascii true}) "UTF-8"))
+        limit (- gate/max-receipt-bytes 256)
+        fitting-n (loop [low 0 high 60000]
+                    (if (= low high)
+                      low
+                      (let [mid (quot (inc (+ low high)) 2)
+                            r (assoc base :padding (apply str (repeat mid "\\")))]
+                        (if (<= (gate/receipt-size r) limit)
+                          (recur mid high)
+                          (recur low (dec mid))))))
+        fit (assoc base :padding (apply str (repeat fitting-n "\\")))
+        over (update fit :padding str "\\")]
+    (reset! boundary-metrics
+            {:edn (edn-bytes fit) :json (json-bytes fit) :max (gate/receipt-size fit)
+             :over-edn (edn-bytes over) :over-json (json-bytes over)
+             :over-max (gate/receipt-size over) :limit limit})
+    (is (= (str ";; " hostile) (decode encoded)))
+    (is (not (re-find #"[\n\u2028\u202e]" encoded)))
+    (is (= (max (edn-bytes fit) (json-bytes fit)) (gate/receipt-size fit)))
+    (is (<= (edn-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (<= (json-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (= fit (gate/bounded-receipt! fit)))
+    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"exceeds 65536"
+                          (gate/bounded-receipt! over)))))
+
+(deftest proof-text-is-the-structured-result
+  (doseq [result [{:ok true :state "committed-probe-only" :verification_complete false
+                   :proof {:tier :warm :status :pending
+                           :next_call {:op :proof-status :receipt "/var/tmp/r.edn"}}}
+                  {:ok true :state "committed" :verification_complete true
+                   :proof {:tier :cold :status :complete :next_call nil}}]]
+    (let [[header payload] (str/split (tool/summary result) #"\n" 2)]
+      (is (= (json/parse-string (json/generate-string result))
+             (json/parse-string payload)))
+      (is (= (:verification_complete result)
+             (get (json/parse-string payload) "verification_complete")))
+      (is (= (name (get-in result [:proof :status]))
+             (get-in (json/parse-string payload) ["proof" "status"])))
+      (is (= (= :pending (get-in result [:proof :status]))
+             (str/includes? header "proof pending"))))))
+
+(def result (run-tests 'rows4-sol-fence.probes))
+result
diff --git a/CHANGELOG.md b/CHANGELOG.md
index 0b12645111b8523053fa39bbdc5a12c1fcec61b3..bfa599293d51973c4129287eff80ca7926422391
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -11,6 +11,7 @@
 - Namespace-split receipts now carry candidate comment edits, scoped stale-reference
   and facade scans, actual per-owner definition counts, and body hashes separating
   raw equality from authorized replay. Contradictory evidence refuses before write.
+  Facade scope explicitly names macro/dynamic exclusions.
   Committed incomplete receipts explain proof tier/status and name `:proof-status`
   as the next read; `verification_complete` retains its existing meaning.
 
diff --git a/README.md b/README.md
index 2e8eff2df58925f2e6f07dababf92d605c9b4054..7dc749e80219cd76555ce6194d4750f8340a0f3a
--- a/README.md
+++ b/README.md
@@ -842,6 +842,8 @@
   `defn`/`apply` forwarders,
   with retention-policy expectations and scan scope. Existing unmapped forwarders
   are listed in `:expected`; new wrappers appear in `:unexpected` and refuse.
+  The scope explicitly excludes macro expansion, arbitrary forwarding bodies and
+  dynamic resolution.
 - `:exactly_once`: per-owner destination/elsewhere counts. Pre-existing
   namesakes in unrelated namespaces have separate file/hash evidence; adding
   another copy still fails.
diff --git a/docs/intent/helper-extraction/namespace-split-design.md b/docs/intent/helper-extraction/namespace-split-design.md
index ab007ace2b6d710e161a62fe50c7b2c39762c373..b129608f6938a10b7be9466b2ba039316327a6e7
--- a/docs/intent/helper-extraction/namespace-split-design.md
+++ b/docs/intent/helper-extraction/namespace-split-design.md
@@ -476,6 +476,8 @@
 scans cover symbol, Var, partial and fn aliases plus single/multi-arity direct
 call/apply bodies. Retention explicitly permits existing unmapped forwarding
 owners, reports them as expected, and refuses newly introduced wrappers.
+The receipt scope explicitly excludes macro expansion, arbitrary forwarding
+bodies and dynamic resolution.
 
 The delta fence also binds permitted forwarders to canonical mapped targets and
 forwarding-kind/arity positions. Any forwarding arity is enumerated, including
diff --git a/docs/observations/rows4-fence.md b/docs/observations/rows4-fence.md
new file mode 100644
index 0000000000000000000000000000000000000000..5c053387822384d340eb66148b2b6b1ac59bd040
--- /dev/null
+++ b/docs/observations/rows4-fence.md
@@ -0,0 +1,154 @@
+GO-WITH-FIX
+
+# Sol fence: rows sublime batch 4 (`a95ac349`)
+
+Reviewed `astra/namespace-split` at `a95ac349` (`65a542bb` plus trunk merge).
+The review worktree began at `f8cc0c4d`; its tree was byte-identical to
+`a95ac349` (`0a098fb4add3ef3f0177676a356e5366dc8a164c`). I read Astra's report
+and the retained in-batch Sol fence and did not repeat its probes. This delta
+attacked the five requested escape shapes, dual serialization boundaries,
+proof/text agreement, merge counters, and the frozen Cell C inputs.
+
+The fixes are small and are applied in this worktree:
+
+- `src/clj_surgeon/namespace_split.clj` now publishes facade exclusions for
+  macro expansion, arbitrary forwarding bodies, and dynamic resolution.
+- `test/clj_surgeon/namespace_split_test.clj` retains the macro counterexample
+  as a bounded-scope witness without adding a new `deftest` or moving census
+  pins.
+- `docs/intent/helper-extraction/namespace-split-design.md`, `README.md`, and
+  `CHANGELOG.md` name the same boundary.
+
+No design hole remains inside the claimed static/captured transaction contract.
+
+## Five transaction-negative attacks
+
+Executed red-if-undetected tests are retained at
+`/var/tmp/forge/rows4-sol-fence/probes.clj`. They ran as seven tests / 24
+assertions. Three assertions intentionally went red at declared exclusions;
+the other attacks were caught. The exclusions are now all carried by the
+receipt itself.
+
+1. **Stale reference outside roots:** red, as expected. A real source outside
+   `roots=[src,test]` is absent from the transaction snapshot. The receipt does
+   not claim otherwise: `stale_references.scope.roots` names the exact roots,
+   file count, and snapshot hash. This is bounded scope, not a false zero over
+   the repository.
+2. **Reader conditional or string:** a `.cljc` reader conditional containing
+   `app.views/moved` produced two `retired-symbol` sites, one in each branch.
+   The same spelling inside a string produced zero and made the red assertion
+   fail. `stale_references.scope.excludes` already says `strings`, `dynamic
+   resolution`, semicolon prose, and unresolved unqualified symbols.
+3. **Macro facade:** `(defmacro legacy [& xs] \`(u/moved ~@xs))` produced no
+   facade row. That is outside the deliberately syntactic facade grammar, not
+   evidence of absence. Before this review the report/design described that
+   limit, but the structured receipt only carried a positive `scan` sentence.
+   The applied fix adds explicit receipt-local exclusions. General macro
+   expansion remains unclaimed.
+4. **Owner duplicated under another arity:** two top-level `moved` definitions
+   produced `[["moved" 2 0]]` and `passed=false`. Adding an arity within the
+   one destination definition leaves exactly-once at `[["moved" 1 0]]` but
+   independently produces `bodies_preserved equal=0, unequal=1`. The combined
+   prepublication gate therefore refuses either shape.
+5. **Metadata/docstring drift:** changing `moved` metadata and changing its
+   docstring each independently produced `equal=0, unequal=1`. The preservation
+   hash covers complete owner bytes after only the enumerated authorized replay;
+   it is not a body-expression-only hash.
+
+These facts remain transaction facts, not timeless guarantees. Outside roots,
+strings/dynamic construction, macro expansion, and arbitrary forwarding bodies
+remain explicit non-claims.
+
+## Encoding and receipt ceiling
+
+`review-facts` applies lossless JSON string-content encoding to every string in
+the complete negative-fact tree. The hostile probe included a newline in a file
+name plus quote, backslash, U+2028, and U+202E content and round-tripped after
+wrapping once in quotes and JSON-decoding. No raw newline/U+2028/U+202E survived
+in the encoded scalar.
+
+The constructed maximum-fit boundary measured both serializations:
+
+```text
+limit after fixed transport reservation: 65,280 bytes
+fitting EDN:                           65,250 bytes
+fitting escaped JSON:                  65,279 bytes
+charged maximum:                       65,279 bytes
+one more backslash, EDN:               65,252 bytes
+one more backslash, escaped JSON:      65,281 bytes -> receipt-size-bound
+```
+
+Thus the larger escaped-JSON representation controls the boundary; EDN being
+smaller cannot mint publication authority. The final real Cell C pending receipt,
+projected with the new exclusion field, is still below every 65,536-byte public
+ceiling: EDN 62,698; escaped JSON 63,249; full MCP text 63,373 bytes. Headroom is
+2,838 / 2,287 / 2,163 bytes respectively.
+
+## Proof context and text/structured agreement
+
+No disagreement path was found. `proof-context` derives `proof.status` and both
+copies of `next_call` from the finalized `verification_complete` value.
+`committed-facts!` selects those fields together from the immutable original
+receipt. The MCP summary appends the entire escaped JSON result, rather than a
+lossy projection.
+
+For both pending and complete synthetic receipts, and for the actual final Cell C
+pending receipt, parsing the payload after the first text line was equal to the
+structured result. `verification_complete`, `proof.status`, and `next_call` were
+identical; the human header said `proof pending` exactly when structured status
+was pending. This satisfies text superset structured by byte-complete inclusion.
+
+## Merge counters are tree-derived
+
+Confirmed by executing the same source readers the tests call, not by reading the
+literal assertions:
+
+```edn
+{:round-one 1019
+ :adopted 591
+ :total 1610
+ :arithmetic-closes true
+ :non-mcp 246
+ :per-prefix {"WTL-" 53, "PERF-SENT-" 50, "OP-ALG-" 39,
+              "TEST-ISO-" 20, "MEASURE-" 4,
+              "TELEMETRY-EVENTS-" 1, "ROUTING-" 3}}
+```
+
+`deftest-count` reads every manifest namespace source and counts actual top-level
+`deftest` forms. The intent census derives spec documents from the tree, parses
+their IDs, removes `MCP-OP-*`, and independently groups the requested prefixes.
+The literals are ratchet expectations over those derived values, not restatements.
+
+## Frozen Cell C contract
+
+**Yes, unchanged.** Comparing the landed wave-5/6 build `2b39bd37` to
+`a95ac349` gives identical SHA-256 values for the frozen inputs:
+
+```text
+D1-request.edn                         5e7e5052274a773a24005c62108fd831ca415314bc314a93d44d2563704e50ea
+cellC/manifest.edn                     1982c78be180554afdcae7f35c4c3905aaf9fed83cdd86f7b1edd1cfac132f93
+namespace_split_papercut_oracle.py     501ba2ce75fb9d670c09c500cff1c1b25053cc80b9fe0a83577d78ebe777813c
+external cc-oracle.py                  3f37ed778ed6658f60254706edac2db8ee8d82dc7e3fcfbb4e2c4774383a3607
+```
+
+The retained actual receipt still reports map hash
+`b3ac9adc5a060e6c080db9e4b8cb1d21fde589f9dbcc06036c97b3eea588b818`
+and 141 owners / 20 destinations / 87 sites / five caller files. The requested
+fix changes receipt scope disclosure only; it does not change source shape,
+mapping, policy, or oracle.
+
+## Verification
+
+- Warm targeted suites after the fix: 71 tests / 525 assertions, zero failures
+  or errors (`namespace-split-test` and `mcp-namespace-split-test`).
+- `~/bin/clj-kondo` on the two changed Clojure files: zero errors, zero warnings.
+- Formatter and `git diff --check`: pass.
+- Tree-derived counter evaluation: values above, arithmetic closed.
+- No `make test` was run. Astra's retained cold receipt at
+  `/var/tmp/forge/rows-sublime-4/make-test-receipt.json` remains the cold gate:
+  exit 0, 497.989 s, identical before/after source inventory hash
+  `c9bf36fccac68b297e2adc4ab3a75bee49ed45aead8980c745d568326ae26ced`.
+
+Verdict: **GO-WITH-FIX** with the five files named above. The only newly exposed
+escape is a documented, encoded, bounded macro/dynamic non-claim; it is not a
+design hole in the frozen static Cell C contract.
diff --git a/src/clj_surgeon/namespace_split.clj b/src/clj_surgeon/namespace_split.clj
index bf6a10c89c2f3cb5face162b01404811dce465c3..0b2bc573fa4de8e577824df43dd0be0674c4d0b4
--- a/src/clj_surgeon/namespace_split.clj
+++ b/src/clj_surgeon/namespace_split.clj
@@ -927,7 +927,9 @@
        :stale_references {:count (count stale) :sites stale :expected [] :scope scope}
        :facades {:forms facades :expected expected-facades :unexpected unexpected-facades
                  :policy "no-new-forwarders; retain existing unmapped forwarding owners"
-                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets] :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"}}
+                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets]
+                         :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"
+                         :excludes ["macro expansion" "arbitrary forwarding bodies" "dynamic resolution"]}}
        :exactly_once {:columns [:owner :destination_count :elsewhere_count] :rows counts
                       :preexisting_namesakes
                       (mapv (fn [x] (assoc x :after_hashes
diff --git a/test/clj_surgeon/namespace_split_test.clj b/test/clj_surgeon/namespace_split_test.clj
index 66cec67229f25ff49eb1b6b06d7e921e5eb91c8a..0f03fccdd6014131a146c403c7611df8ca4b672b
--- a/test/clj_surgeon/namespace_split_test.clj
+++ b/test/clj_surgeon/namespace_split_test.clj
@@ -937,7 +937,12 @@
                 "(def legacy (fn [x] (u/moved x)))"]]
     (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
                      (str "(ns app.views (:require [app.util :as u]))\n" body "\n"))]
-      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body))))
+      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body)))
+  (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
+                   "(ns app.views (:require [app.util :as u]))\n(defmacro legacy [& xs] `(u/moved ~@xs))\n")
+        fact (:facades (split/review-facts compiled))]
+    (is (= [] (:forms fact)))
+    (is (some #{"macro expansion"} (get-in fact [:scope :excludes])))))
 
 ;; @spec NS-SPLIT-062
 (deftest retention-policy-reports-existing-forwarders

Completed the adversarial delta, applied the bounded facade-scope fix, verified targeted suites/lint/counters/hashes, and wrote the requested verdict.
  ✓ Read repository/skill instructions and retained batch-4 evidence
  ✓ Inspect implementation and run targeted red/adversarial probes
  ✓ Verify receipt bounds, text/structured agreement, counters, and frozen hashes
  ✓ Apply any small required fixes and write the verdict file
  ✓ Run proportionate targeted verification and report
diff --git a//var/tmp/forge/rows4-sol-fence/probes.clj b//var/tmp/forge/rows4-sol-fence/probes.clj
new file mode 100644
index 0000000000000000000000000000000000000000..110b48546318002e08a21bac95e7b83867659d3f
--- /dev/null
+++ b//var/tmp/forge/rows4-sol-fence/probes.clj
@@ -0,0 +1,116 @@
+(ns rows4-sol-fence.probes
+  (:require [cheshire.core :as json]
+            [clj-surgeon.mcp-namespace-split :as tool]
+            [clj-surgeon.namespace-split :as split]
+            [clj-surgeon.namespace-split-test :as fixture]
+            [clj-surgeon.split-proof-gate :as gate]
+            [clojure.string :as str]
+            [clojure.test :refer [deftest is run-tests testing]]))
+
+(defn facts-with [path source]
+  (split/review-facts
+    (-> (fixture/negative-fixture)
+        (assoc-in [:guard-sources path] source)
+        (assoc-in [:future-sources path] source))))
+
+(def boundary-metrics (atom nil))
+
+(deftest stale-reference-scope-escape-turns-red
+  (let [outside "(ns app.escape (:require [app.views :as old]))\n(def escaped (old/moved 1))\n"
+        facts (split/review-facts (fixture/negative-fixture))]
+    (testing "a real source outside declared roots is intentionally absent from the captured transaction"
+      (is (str/includes? outside "old/moved"))
+      (is (pos? (get-in facts [:stale_references :count] 0))
+          "RED: outside-root stale references escape; receipt scope must make that exclusion explicit"))))
+
+(deftest string-and-reader-conditional-stale-reference-probes
+  (let [string-facts (facts-with "test/app/string_escape.clj"
+                                 "(ns app.string-escape)\n(def escaped \"app.views/moved\")\n")
+        reader-facts (facts-with "test/app/reader_escape.cljc"
+                                 "(ns app.reader-escape)\n#?(:clj (def escaped app.views/moved) :cljs (def escaped app.views/moved))\n")]
+    (is (pos? (get-in string-facts [:stale_references :count] 0))
+        "RED: string-spelled references are intentionally excluded and must be disclosed")
+    (is (pos? (get-in reader-facts [:stale_references :count] 0))
+        "reader-conditional qualified symbols must not evade the structural token scan")))
+
+(deftest macro-forwarding-facade-turns-red
+  (let [tick (str (char 96))
+        source (str "(ns app.views (:require [app.util :as u]))\n"
+                    "(defmacro legacy [& xs] " tick "(u/moved ~@xs))\n")
+        facts (facts-with "src/app/views.clj" source)]
+    (is (seq (get-in facts [:facades :forms]))
+        "RED: macro forwarders are outside the facade grammar; receipt scope must say so")))
+
+(deftest different-arity-duplicate-is-caught
+  (let [compiled (fixture/negative-fixture)
+        bad (update-in compiled [:future-sources "src/app/util.clj"]
+                       str "\n(defn moved [x y] (+ x y))\n")
+        facts (split/review-facts bad)]
+    (is (= [["moved" 2 0]] (get-in facts [:exactly_once :rows])))
+    (is (false? (get-in facts [:exactly_once :passed])))))
+
+(deftest metadata-and-docstring-changes-are-body-changes
+  (doseq [[label rewrite]
+          [[:metadata #(str/replace % "(defn moved" "(defn ^:private moved")]
+           [:docstring #(str/replace % "[x]" "\"changed docs\" [x]")]]]
+    (let [compiled (fixture/negative-fixture)
+          bad (update-in compiled [:future-sources "src/app/util.clj"] rewrite)
+          facts (split/review-facts bad)]
+      (is (= 1 (get-in facts [:bodies_preserved :unequal])) (name label)))))
+
+(deftest negative-facts-are-losslessly-encoded-and-dual-format-bounded
+  (let [hostile "quote \" slash \\ literal-newline \\n line-separator\u2028bidi\u202e"
+        path "test/app/hostile\nname.clj"
+        before (str "(ns app.hostile)\n;; " hostile "\n(def escaped \"app.views/moved\")\n")
+        after (str/replace before ";; " ";;  ")
+        facts (split/review-facts
+                (-> (fixture/negative-fixture)
+                    (assoc-in [:guard-sources path] before)
+                    (assoc-in [:future-sources path] after)))
+        encoded (-> facts :comment_edits first :lines first (nth 2))
+        decode #(json/parse-string (str "\"" % "\""))
+        base {:ok true :facts facts}
+        edn-bytes #(alength (.getBytes (pr-str %) "UTF-8"))
+        json-bytes #(alength (.getBytes (json/generate-string % {:escape-non-ascii true}) "UTF-8"))
+        limit (- gate/max-receipt-bytes 256)
+        fitting-n (loop [low 0 high 60000]
+                    (if (= low high)
+                      low
+                      (let [mid (quot (inc (+ low high)) 2)
+                            r (assoc base :padding (apply str (repeat mid "\\")))]
+                        (if (<= (gate/receipt-size r) limit)
+                          (recur mid high)
+                          (recur low (dec mid))))))
+        fit (assoc base :padding (apply str (repeat fitting-n "\\")))
+        over (update fit :padding str "\\")]
+    (reset! boundary-metrics
+            {:edn (edn-bytes fit) :json (json-bytes fit) :max (gate/receipt-size fit)
+             :over-edn (edn-bytes over) :over-json (json-bytes over)
+             :over-max (gate/receipt-size over) :limit limit})
+    (is (= (str ";; " hostile) (decode encoded)))
+    (is (not (re-find #"[\n\u2028\u202e]" encoded)))
+    (is (= (max (edn-bytes fit) (json-bytes fit)) (gate/receipt-size fit)))
+    (is (<= (edn-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (<= (json-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (= fit (gate/bounded-receipt! fit)))
+    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"exceeds 65536"
+                          (gate/bounded-receipt! over)))))
+
+(deftest proof-text-is-the-structured-result
+  (doseq [result [{:ok true :state "committed-probe-only" :verification_complete false
+                   :proof {:tier :warm :status :pending
+                           :next_call {:op :proof-status :receipt "/var/tmp/r.edn"}}}
+                  {:ok true :state "committed" :verification_complete true
+                   :proof {:tier :cold :status :complete :next_call nil}}]]
+    (let [[header payload] (str/split (tool/summary result) #"\n" 2)]
+      (is (= (json/parse-string (json/generate-string result))
+             (json/parse-string payload)))
+      (is (= (:verification_complete result)
+             (get (json/parse-string payload) "verification_complete")))
+      (is (= (name (get-in result [:proof :status]))
+             (get-in (json/parse-string payload) ["proof" "status"])))
+      (is (= (= :pending (get-in result [:proof :status]))
+             (str/includes? header "proof pending"))))))
+
+(def result (run-tests 'rows4-sol-fence.probes))
+result
diff --git a/CHANGELOG.md b/CHANGELOG.md
index 0b12645111b8523053fa39bbdc5a12c1fcec61b3..bfa599293d51973c4129287eff80ca7926422391
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -11,6 +11,7 @@
 - Namespace-split receipts now carry candidate comment edits, scoped stale-reference
   and facade scans, actual per-owner definition counts, and body hashes separating
   raw equality from authorized replay. Contradictory evidence refuses before write.
+  Facade scope explicitly names macro/dynamic exclusions.
   Committed incomplete receipts explain proof tier/status and name `:proof-status`
   as the next read; `verification_complete` retains its existing meaning.
 
diff --git a/README.md b/README.md
index 2e8eff2df58925f2e6f07dababf92d605c9b4054..7dc749e80219cd76555ce6194d4750f8340a0f3a
--- a/README.md
+++ b/README.md
@@ -842,6 +842,8 @@
   `defn`/`apply` forwarders,
   with retention-policy expectations and scan scope. Existing unmapped forwarders
   are listed in `:expected`; new wrappers appear in `:unexpected` and refuse.
+  The scope explicitly excludes macro expansion, arbitrary forwarding bodies and
+  dynamic resolution.
 - `:exactly_once`: per-owner destination/elsewhere counts. Pre-existing
   namesakes in unrelated namespaces have separate file/hash evidence; adding
   another copy still fails.
diff --git a/docs/intent/helper-extraction/namespace-split-design.md b/docs/intent/helper-extraction/namespace-split-design.md
index ab007ace2b6d710e161a62fe50c7b2c39762c373..b129608f6938a10b7be9466b2ba039316327a6e7
--- a/docs/intent/helper-extraction/namespace-split-design.md
+++ b/docs/intent/helper-extraction/namespace-split-design.md
@@ -476,6 +476,8 @@
 scans cover symbol, Var, partial and fn aliases plus single/multi-arity direct
 call/apply bodies. Retention explicitly permits existing unmapped forwarding
 owners, reports them as expected, and refuses newly introduced wrappers.
+The receipt scope explicitly excludes macro expansion, arbitrary forwarding
+bodies and dynamic resolution.
 
 The delta fence also binds permitted forwarders to canonical mapped targets and
 forwarding-kind/arity positions. Any forwarding arity is enumerated, including
diff --git a/docs/observations/rows4-fence.md b/docs/observations/rows4-fence.md
new file mode 100644
index 0000000000000000000000000000000000000000..5c053387822384d340eb66148b2b6b1ac59bd040
--- /dev/null
+++ b/docs/observations/rows4-fence.md
@@ -0,0 +1,154 @@
+GO-WITH-FIX
+
+# Sol fence: rows sublime batch 4 (`a95ac349`)
+
+Reviewed `astra/namespace-split` at `a95ac349` (`65a542bb` plus trunk merge).
+The review worktree began at `f8cc0c4d`; its tree was byte-identical to
+`a95ac349` (`0a098fb4add3ef3f0177676a356e5366dc8a164c`). I read Astra's report
+and the retained in-batch Sol fence and did not repeat its probes. This delta
+attacked the five requested escape shapes, dual serialization boundaries,
+proof/text agreement, merge counters, and the frozen Cell C inputs.
+
+The fixes are small and are applied in this worktree:
+
+- `src/clj_surgeon/namespace_split.clj` now publishes facade exclusions for
+  macro expansion, arbitrary forwarding bodies, and dynamic resolution.
+- `test/clj_surgeon/namespace_split_test.clj` retains the macro counterexample
+  as a bounded-scope witness without adding a new `deftest` or moving census
+  pins.
+- `docs/intent/helper-extraction/namespace-split-design.md`, `README.md`, and
+  `CHANGELOG.md` name the same boundary.
+
+No design hole remains inside the claimed static/captured transaction contract.
+
+## Five transaction-negative attacks
+
+Executed red-if-undetected tests are retained at
+`/var/tmp/forge/rows4-sol-fence/probes.clj`. They ran as seven tests / 24
+assertions. Three assertions intentionally went red at declared exclusions;
+the other attacks were caught. The exclusions are now all carried by the
+receipt itself.
+
+1. **Stale reference outside roots:** red, as expected. A real source outside
+   `roots=[src,test]` is absent from the transaction snapshot. The receipt does
+   not claim otherwise: `stale_references.scope.roots` names the exact roots,
+   file count, and snapshot hash. This is bounded scope, not a false zero over
+   the repository.
+2. **Reader conditional or string:** a `.cljc` reader conditional containing
+   `app.views/moved` produced two `retired-symbol` sites, one in each branch.
+   The same spelling inside a string produced zero and made the red assertion
+   fail. `stale_references.scope.excludes` already says `strings`, `dynamic
+   resolution`, semicolon prose, and unresolved unqualified symbols.
+3. **Macro facade:** `(defmacro legacy [& xs] \`(u/moved ~@xs))` produced no
+   facade row. That is outside the deliberately syntactic facade grammar, not
+   evidence of absence. Before this review the report/design described that
+   limit, but the structured receipt only carried a positive `scan` sentence.
+   The applied fix adds explicit receipt-local exclusions. General macro
+   expansion remains unclaimed.
+4. **Owner duplicated under another arity:** two top-level `moved` definitions
+   produced `[["moved" 2 0]]` and `passed=false`. Adding an arity within the
+   one destination definition leaves exactly-once at `[["moved" 1 0]]` but
+   independently produces `bodies_preserved equal=0, unequal=1`. The combined
+   prepublication gate therefore refuses either shape.
+5. **Metadata/docstring drift:** changing `moved` metadata and changing its
+   docstring each independently produced `equal=0, unequal=1`. The preservation
+   hash covers complete owner bytes after only the enumerated authorized replay;
+   it is not a body-expression-only hash.
+
+These facts remain transaction facts, not timeless guarantees. Outside roots,
+strings/dynamic construction, macro expansion, and arbitrary forwarding bodies
+remain explicit non-claims.
+
+## Encoding and receipt ceiling
+
+`review-facts` applies lossless JSON string-content encoding to every string in
+the complete negative-fact tree. The hostile probe included a newline in a file
+name plus quote, backslash, U+2028, and U+202E content and round-tripped after
+wrapping once in quotes and JSON-decoding. No raw newline/U+2028/U+202E survived
+in the encoded scalar.
+
+The constructed maximum-fit boundary measured both serializations:
+
+```text
+limit after fixed transport reservation: 65,280 bytes
+fitting EDN:                           65,250 bytes
+fitting escaped JSON:                  65,279 bytes
+charged maximum:                       65,279 bytes
+one more backslash, EDN:               65,252 bytes
+one more backslash, escaped JSON:      65,281 bytes -> receipt-size-bound
+```
+
+Thus the larger escaped-JSON representation controls the boundary; EDN being
+smaller cannot mint publication authority. The final real Cell C pending receipt,
+projected with the new exclusion field, is still below every 65,536-byte public
+ceiling: EDN 62,698; escaped JSON 63,249; full MCP text 63,373 bytes. Headroom is
+2,838 / 2,287 / 2,163 bytes respectively.
+
+## Proof context and text/structured agreement
+
+No disagreement path was found. `proof-context` derives `proof.status` and both
+copies of `next_call` from the finalized `verification_complete` value.
+`committed-facts!` selects those fields together from the immutable original
+receipt. The MCP summary appends the entire escaped JSON result, rather than a
+lossy projection.
+
+For both pending and complete synthetic receipts, and for the actual final Cell C
+pending receipt, parsing the payload after the first text line was equal to the
+structured result. `verification_complete`, `proof.status`, and `next_call` were
+identical; the human header said `proof pending` exactly when structured status
+was pending. This satisfies text superset structured by byte-complete inclusion.
+
+## Merge counters are tree-derived
+
+Confirmed by executing the same source readers the tests call, not by reading the
+literal assertions:
+
+```edn
+{:round-one 1019
+ :adopted 591
+ :total 1610
+ :arithmetic-closes true
+ :non-mcp 246
+ :per-prefix {"WTL-" 53, "PERF-SENT-" 50, "OP-ALG-" 39,
+              "TEST-ISO-" 20, "MEASURE-" 4,
+              "TELEMETRY-EVENTS-" 1, "ROUTING-" 3}}
+```
+
+`deftest-count` reads every manifest namespace source and counts actual top-level
+`deftest` forms. The intent census derives spec documents from the tree, parses
+their IDs, removes `MCP-OP-*`, and independently groups the requested prefixes.
+The literals are ratchet expectations over those derived values, not restatements.
+
+## Frozen Cell C contract
+
+**Yes, unchanged.** Comparing the landed wave-5/6 build `2b39bd37` to
+`a95ac349` gives identical SHA-256 values for the frozen inputs:
+
+```text
+D1-request.edn                         5e7e5052274a773a24005c62108fd831ca415314bc314a93d44d2563704e50ea
+cellC/manifest.edn                     1982c78be180554afdcae7f35c4c3905aaf9fed83cdd86f7b1edd1cfac132f93
+namespace_split_papercut_oracle.py     501ba2ce75fb9d670c09c500cff1c1b25053cc80b9fe0a83577d78ebe777813c
+external cc-oracle.py                  3f37ed778ed6658f60254706edac2db8ee8d82dc7e3fcfbb4e2c4774383a3607
+```
+
+The retained actual receipt still reports map hash
+`b3ac9adc5a060e6c080db9e4b8cb1d21fde589f9dbcc06036c97b3eea588b818`
+and 141 owners / 20 destinations / 87 sites / five caller files. The requested
+fix changes receipt scope disclosure only; it does not change source shape,
+mapping, policy, or oracle.
+
+## Verification
+
+- Warm targeted suites after the fix: 71 tests / 525 assertions, zero failures
+  or errors (`namespace-split-test` and `mcp-namespace-split-test`).
+- `~/bin/clj-kondo` on the two changed Clojure files: zero errors, zero warnings.
+- Formatter and `git diff --check`: pass.
+- Tree-derived counter evaluation: values above, arithmetic closed.
+- No `make test` was run. Astra's retained cold receipt at
+  `/var/tmp/forge/rows-sublime-4/make-test-receipt.json` remains the cold gate:
+  exit 0, 497.989 s, identical before/after source inventory hash
+  `c9bf36fccac68b297e2adc4ab3a75bee49ed45aead8980c745d568326ae26ced`.
+
+Verdict: **GO-WITH-FIX** with the five files named above. The only newly exposed
+escape is a documented, encoded, bounded macro/dynamic non-claim; it is not a
+design hole in the frozen static Cell C contract.
diff --git a/src/clj_surgeon/namespace_split.clj b/src/clj_surgeon/namespace_split.clj
index bf6a10c89c2f3cb5face162b01404811dce465c3..0b2bc573fa4de8e577824df43dd0be0674c4d0b4
--- a/src/clj_surgeon/namespace_split.clj
+++ b/src/clj_surgeon/namespace_split.clj
@@ -927,7 +927,9 @@
        :stale_references {:count (count stale) :sites stale :expected [] :scope scope}
        :facades {:forms facades :expected expected-facades :unexpected unexpected-facades
                  :policy "no-new-forwarders; retain existing unmapped forwarding owners"
-                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets] :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"}}
+                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets]
+                         :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"
+                         :excludes ["macro expansion" "arbitrary forwarding bodies" "dynamic resolution"]}}
        :exactly_once {:columns [:owner :destination_count :elsewhere_count] :rows counts
                       :preexisting_namesakes
                       (mapv (fn [x] (assoc x :after_hashes
diff --git a/test/clj_surgeon/namespace_split_test.clj b/test/clj_surgeon/namespace_split_test.clj
index 66cec67229f25ff49eb1b6b06d7e921e5eb91c8a..0f03fccdd6014131a146c403c7611df8ca4b672b
--- a/test/clj_surgeon/namespace_split_test.clj
+++ b/test/clj_surgeon/namespace_split_test.clj
@@ -937,7 +937,12 @@
                 "(def legacy (fn [x] (u/moved x)))"]]
     (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
                      (str "(ns app.views (:require [app.util :as u]))\n" body "\n"))]
-      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body))))
+      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body)))
+  (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
+                   "(ns app.views (:require [app.util :as u]))\n(defmacro legacy [& xs] `(u/moved ~@xs))\n")
+        fact (:facades (split/review-facts compiled))]
+    (is (= [] (:forms fact)))
+    (is (some #{"macro expansion"} (get-in fact [:scope :excludes])))))
 
 ;; @spec NS-SPLIT-062
 (deftest retention-policy-reports-existing-forwarders

codex
GO-WITH-FIX.

Verdict: [rows4-fence.md](/home/forge/src/clj-surgeon-fence/docs/observations/rows4-fence.md)

Applied the required bounded fix across:

- [namespace_split.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/namespace_split.clj)
- [namespace_split_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/namespace_split_test.clj)
- [namespace-split-design.md](/home/forge/src/clj-surgeon-fence/docs/intent/helper-extraction/namespace-split-design.md)
- [README.md](/home/forge/src/clj-surgeon-fence/README.md)
- [CHANGELOG.md](/home/forge/src/clj-surgeon-fence/CHANGELOG.md)

The receipt now explicitly excludes macro expansion, arbitrary forwarding bodies, and dynamic resolution. Outside-root and string references remain clearly bounded exclusions; reader conditionals, arity duplication, metadata changes, and docstring changes are detected.

Verification:

- Targeted suites: 71 tests / 525 assertions, all green.
- Receipt boundary: 65,279-byte escaped JSON admitted; 65,281 bytes refused.
- Patched Cell C receipt remains below ceiling: 63,249-byte JSON, 63,373-byte MCP text.
- Counters derived from tree: adopted 591, total 1610, non-MCP 246, TEST-ISO 20.
- Frozen Cell C hashes unchanged.
- `~/bin/clj-kondo`: zero errors/warnings.
- No `make test` run; Astra’s retained 497.989-second exit-0 receipt was used.
diff --git a//var/tmp/forge/rows4-sol-fence/probes.clj b//var/tmp/forge/rows4-sol-fence/probes.clj
new file mode 100644
index 0000000000000000000000000000000000000000..110b48546318002e08a21bac95e7b83867659d3f
--- /dev/null
+++ b//var/tmp/forge/rows4-sol-fence/probes.clj
@@ -0,0 +1,116 @@
+(ns rows4-sol-fence.probes
+  (:require [cheshire.core :as json]
+            [clj-surgeon.mcp-namespace-split :as tool]
+            [clj-surgeon.namespace-split :as split]
+            [clj-surgeon.namespace-split-test :as fixture]
+            [clj-surgeon.split-proof-gate :as gate]
+            [clojure.string :as str]
+            [clojure.test :refer [deftest is run-tests testing]]))
+
+(defn facts-with [path source]
+  (split/review-facts
+    (-> (fixture/negative-fixture)
+        (assoc-in [:guard-sources path] source)
+        (assoc-in [:future-sources path] source))))
+
+(def boundary-metrics (atom nil))
+
+(deftest stale-reference-scope-escape-turns-red
+  (let [outside "(ns app.escape (:require [app.views :as old]))\n(def escaped (old/moved 1))\n"
+        facts (split/review-facts (fixture/negative-fixture))]
+    (testing "a real source outside declared roots is intentionally absent from the captured transaction"
+      (is (str/includes? outside "old/moved"))
+      (is (pos? (get-in facts [:stale_references :count] 0))
+          "RED: outside-root stale references escape; receipt scope must make that exclusion explicit"))))
+
+(deftest string-and-reader-conditional-stale-reference-probes
+  (let [string-facts (facts-with "test/app/string_escape.clj"
+                                 "(ns app.string-escape)\n(def escaped \"app.views/moved\")\n")
+        reader-facts (facts-with "test/app/reader_escape.cljc"
+                                 "(ns app.reader-escape)\n#?(:clj (def escaped app.views/moved) :cljs (def escaped app.views/moved))\n")]
+    (is (pos? (get-in string-facts [:stale_references :count] 0))
+        "RED: string-spelled references are intentionally excluded and must be disclosed")
+    (is (pos? (get-in reader-facts [:stale_references :count] 0))
+        "reader-conditional qualified symbols must not evade the structural token scan")))
+
+(deftest macro-forwarding-facade-turns-red
+  (let [tick (str (char 96))
+        source (str "(ns app.views (:require [app.util :as u]))\n"
+                    "(defmacro legacy [& xs] " tick "(u/moved ~@xs))\n")
+        facts (facts-with "src/app/views.clj" source)]
+    (is (seq (get-in facts [:facades :forms]))
+        "RED: macro forwarders are outside the facade grammar; receipt scope must say so")))
+
+(deftest different-arity-duplicate-is-caught
+  (let [compiled (fixture/negative-fixture)
+        bad (update-in compiled [:future-sources "src/app/util.clj"]
+                       str "\n(defn moved [x y] (+ x y))\n")
+        facts (split/review-facts bad)]
+    (is (= [["moved" 2 0]] (get-in facts [:exactly_once :rows])))
+    (is (false? (get-in facts [:exactly_once :passed])))))
+
+(deftest metadata-and-docstring-changes-are-body-changes
+  (doseq [[label rewrite]
+          [[:metadata #(str/replace % "(defn moved" "(defn ^:private moved")]
+           [:docstring #(str/replace % "[x]" "\"changed docs\" [x]")]]]
+    (let [compiled (fixture/negative-fixture)
+          bad (update-in compiled [:future-sources "src/app/util.clj"] rewrite)
+          facts (split/review-facts bad)]
+      (is (= 1 (get-in facts [:bodies_preserved :unequal])) (name label)))))
+
+(deftest negative-facts-are-losslessly-encoded-and-dual-format-bounded
+  (let [hostile "quote \" slash \\ literal-newline \\n line-separator\u2028bidi\u202e"
+        path "test/app/hostile\nname.clj"
+        before (str "(ns app.hostile)\n;; " hostile "\n(def escaped \"app.views/moved\")\n")
+        after (str/replace before ";; " ";;  ")
+        facts (split/review-facts
+                (-> (fixture/negative-fixture)
+                    (assoc-in [:guard-sources path] before)
+                    (assoc-in [:future-sources path] after)))
+        encoded (-> facts :comment_edits first :lines first (nth 2))
+        decode #(json/parse-string (str "\"" % "\""))
+        base {:ok true :facts facts}
+        edn-bytes #(alength (.getBytes (pr-str %) "UTF-8"))
+        json-bytes #(alength (.getBytes (json/generate-string % {:escape-non-ascii true}) "UTF-8"))
+        limit (- gate/max-receipt-bytes 256)
+        fitting-n (loop [low 0 high 60000]
+                    (if (= low high)
+                      low
+                      (let [mid (quot (inc (+ low high)) 2)
+                            r (assoc base :padding (apply str (repeat mid "\\")))]
+                        (if (<= (gate/receipt-size r) limit)
+                          (recur mid high)
+                          (recur low (dec mid))))))
+        fit (assoc base :padding (apply str (repeat fitting-n "\\")))
+        over (update fit :padding str "\\")]
+    (reset! boundary-metrics
+            {:edn (edn-bytes fit) :json (json-bytes fit) :max (gate/receipt-size fit)
+             :over-edn (edn-bytes over) :over-json (json-bytes over)
+             :over-max (gate/receipt-size over) :limit limit})
+    (is (= (str ";; " hostile) (decode encoded)))
+    (is (not (re-find #"[\n\u2028\u202e]" encoded)))
+    (is (= (max (edn-bytes fit) (json-bytes fit)) (gate/receipt-size fit)))
+    (is (<= (edn-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (<= (json-bytes fit) (- gate/max-receipt-bytes 256)))
+    (is (= fit (gate/bounded-receipt! fit)))
+    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"exceeds 65536"
+                          (gate/bounded-receipt! over)))))
+
+(deftest proof-text-is-the-structured-result
+  (doseq [result [{:ok true :state "committed-probe-only" :verification_complete false
+                   :proof {:tier :warm :status :pending
+                           :next_call {:op :proof-status :receipt "/var/tmp/r.edn"}}}
+                  {:ok true :state "committed" :verification_complete true
+                   :proof {:tier :cold :status :complete :next_call nil}}]]
+    (let [[header payload] (str/split (tool/summary result) #"\n" 2)]
+      (is (= (json/parse-string (json/generate-string result))
+             (json/parse-string payload)))
+      (is (= (:verification_complete result)
+             (get (json/parse-string payload) "verification_complete")))
+      (is (= (name (get-in result [:proof :status]))
+             (get-in (json/parse-string payload) ["proof" "status"])))
+      (is (= (= :pending (get-in result [:proof :status]))
+             (str/includes? header "proof pending"))))))
+
+(def result (run-tests 'rows4-sol-fence.probes))
+result
diff --git a/CHANGELOG.md b/CHANGELOG.md
index 0b12645111b8523053fa39bbdc5a12c1fcec61b3..bfa599293d51973c4129287eff80ca7926422391
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -11,6 +11,7 @@
 - Namespace-split receipts now carry candidate comment edits, scoped stale-reference
   and facade scans, actual per-owner definition counts, and body hashes separating
   raw equality from authorized replay. Contradictory evidence refuses before write.
+  Facade scope explicitly names macro/dynamic exclusions.
   Committed incomplete receipts explain proof tier/status and name `:proof-status`
   as the next read; `verification_complete` retains its existing meaning.
 
diff --git a/README.md b/README.md
index 2e8eff2df58925f2e6f07dababf92d605c9b4054..7dc749e80219cd76555ce6194d4750f8340a0f3a
--- a/README.md
+++ b/README.md
@@ -842,6 +842,8 @@
   `defn`/`apply` forwarders,
   with retention-policy expectations and scan scope. Existing unmapped forwarders
   are listed in `:expected`; new wrappers appear in `:unexpected` and refuse.
+  The scope explicitly excludes macro expansion, arbitrary forwarding bodies and
+  dynamic resolution.
 - `:exactly_once`: per-owner destination/elsewhere counts. Pre-existing
   namesakes in unrelated namespaces have separate file/hash evidence; adding
   another copy still fails.
diff --git a/docs/intent/helper-extraction/namespace-split-design.md b/docs/intent/helper-extraction/namespace-split-design.md
index ab007ace2b6d710e161a62fe50c7b2c39762c373..b129608f6938a10b7be9466b2ba039316327a6e7
--- a/docs/intent/helper-extraction/namespace-split-design.md
+++ b/docs/intent/helper-extraction/namespace-split-design.md
@@ -476,6 +476,8 @@
 scans cover symbol, Var, partial and fn aliases plus single/multi-arity direct
 call/apply bodies. Retention explicitly permits existing unmapped forwarding
 owners, reports them as expected, and refuses newly introduced wrappers.
+The receipt scope explicitly excludes macro expansion, arbitrary forwarding
+bodies and dynamic resolution.
 
 The delta fence also binds permitted forwarders to canonical mapped targets and
 forwarding-kind/arity positions. Any forwarding arity is enumerated, including
diff --git a/docs/observations/rows4-fence.md b/docs/observations/rows4-fence.md
new file mode 100644
index 0000000000000000000000000000000000000000..5c053387822384d340eb66148b2b6b1ac59bd040
--- /dev/null
+++ b/docs/observations/rows4-fence.md
@@ -0,0 +1,154 @@
+GO-WITH-FIX
+
+# Sol fence: rows sublime batch 4 (`a95ac349`)
+
+Reviewed `astra/namespace-split` at `a95ac349` (`65a542bb` plus trunk merge).
+The review worktree began at `f8cc0c4d`; its tree was byte-identical to
+`a95ac349` (`0a098fb4add3ef3f0177676a356e5366dc8a164c`). I read Astra's report
+and the retained in-batch Sol fence and did not repeat its probes. This delta
+attacked the five requested escape shapes, dual serialization boundaries,
+proof/text agreement, merge counters, and the frozen Cell C inputs.
+
+The fixes are small and are applied in this worktree:
+
+- `src/clj_surgeon/namespace_split.clj` now publishes facade exclusions for
+  macro expansion, arbitrary forwarding bodies, and dynamic resolution.
+- `test/clj_surgeon/namespace_split_test.clj` retains the macro counterexample
+  as a bounded-scope witness without adding a new `deftest` or moving census
+  pins.
+- `docs/intent/helper-extraction/namespace-split-design.md`, `README.md`, and
+  `CHANGELOG.md` name the same boundary.
+
+No design hole remains inside the claimed static/captured transaction contract.
+
+## Five transaction-negative attacks
+
+Executed red-if-undetected tests are retained at
+`/var/tmp/forge/rows4-sol-fence/probes.clj`. They ran as seven tests / 24
+assertions. Three assertions intentionally went red at declared exclusions;
+the other attacks were caught. The exclusions are now all carried by the
+receipt itself.
+
+1. **Stale reference outside roots:** red, as expected. A real source outside
+   `roots=[src,test]` is absent from the transaction snapshot. The receipt does
+   not claim otherwise: `stale_references.scope.roots` names the exact roots,
+   file count, and snapshot hash. This is bounded scope, not a false zero over
+   the repository.
+2. **Reader conditional or string:** a `.cljc` reader conditional containing
+   `app.views/moved` produced two `retired-symbol` sites, one in each branch.
+   The same spelling inside a string produced zero and made the red assertion
+   fail. `stale_references.scope.excludes` already says `strings`, `dynamic
+   resolution`, semicolon prose, and unresolved unqualified symbols.
+3. **Macro facade:** `(defmacro legacy [& xs] \`(u/moved ~@xs))` produced no
+   facade row. That is outside the deliberately syntactic facade grammar, not
+   evidence of absence. Before this review the report/design described that
+   limit, but the structured receipt only carried a positive `scan` sentence.
+   The applied fix adds explicit receipt-local exclusions. General macro
+   expansion remains unclaimed.
+4. **Owner duplicated under another arity:** two top-level `moved` definitions
+   produced `[["moved" 2 0]]` and `passed=false`. Adding an arity within the
+   one destination definition leaves exactly-once at `[["moved" 1 0]]` but
+   independently produces `bodies_preserved equal=0, unequal=1`. The combined
+   prepublication gate therefore refuses either shape.
+5. **Metadata/docstring drift:** changing `moved` metadata and changing its
+   docstring each independently produced `equal=0, unequal=1`. The preservation
+   hash covers complete owner bytes after only the enumerated authorized replay;
+   it is not a body-expression-only hash.
+
+These facts remain transaction facts, not timeless guarantees. Outside roots,
+strings/dynamic construction, macro expansion, and arbitrary forwarding bodies
+remain explicit non-claims.
+
+## Encoding and receipt ceiling
+
+`review-facts` applies lossless JSON string-content encoding to every string in
+the complete negative-fact tree. The hostile probe included a newline in a file
+name plus quote, backslash, U+2028, and U+202E content and round-tripped after
+wrapping once in quotes and JSON-decoding. No raw newline/U+2028/U+202E survived
+in the encoded scalar.
+
+The constructed maximum-fit boundary measured both serializations:
+
+```text
+limit after fixed transport reservation: 65,280 bytes
+fitting EDN:                           65,250 bytes
+fitting escaped JSON:                  65,279 bytes
+charged maximum:                       65,279 bytes
+one more backslash, EDN:               65,252 bytes
+one more backslash, escaped JSON:      65,281 bytes -> receipt-size-bound
+```
+
+Thus the larger escaped-JSON representation controls the boundary; EDN being
+smaller cannot mint publication authority. The final real Cell C pending receipt,
+projected with the new exclusion field, is still below every 65,536-byte public
+ceiling: EDN 62,698; escaped JSON 63,249; full MCP text 63,373 bytes. Headroom is
+2,838 / 2,287 / 2,163 bytes respectively.
+
+## Proof context and text/structured agreement
+
+No disagreement path was found. `proof-context` derives `proof.status` and both
+copies of `next_call` from the finalized `verification_complete` value.
+`committed-facts!` selects those fields together from the immutable original
+receipt. The MCP summary appends the entire escaped JSON result, rather than a
+lossy projection.
+
+For both pending and complete synthetic receipts, and for the actual final Cell C
+pending receipt, parsing the payload after the first text line was equal to the
+structured result. `verification_complete`, `proof.status`, and `next_call` were
+identical; the human header said `proof pending` exactly when structured status
+was pending. This satisfies text superset structured by byte-complete inclusion.
+
+## Merge counters are tree-derived
+
+Confirmed by executing the same source readers the tests call, not by reading the
+literal assertions:
+
+```edn
+{:round-one 1019
+ :adopted 591
+ :total 1610
+ :arithmetic-closes true
+ :non-mcp 246
+ :per-prefix {"WTL-" 53, "PERF-SENT-" 50, "OP-ALG-" 39,
+              "TEST-ISO-" 20, "MEASURE-" 4,
+              "TELEMETRY-EVENTS-" 1, "ROUTING-" 3}}
+```
+
+`deftest-count` reads every manifest namespace source and counts actual top-level
+`deftest` forms. The intent census derives spec documents from the tree, parses
+their IDs, removes `MCP-OP-*`, and independently groups the requested prefixes.
+The literals are ratchet expectations over those derived values, not restatements.
+
+## Frozen Cell C contract
+
+**Yes, unchanged.** Comparing the landed wave-5/6 build `2b39bd37` to
+`a95ac349` gives identical SHA-256 values for the frozen inputs:
+
+```text
+D1-request.edn                         5e7e5052274a773a24005c62108fd831ca415314bc314a93d44d2563704e50ea
+cellC/manifest.edn                     1982c78be180554afdcae7f35c4c3905aaf9fed83cdd86f7b1edd1cfac132f93
+namespace_split_papercut_oracle.py     501ba2ce75fb9d670c09c500cff1c1b25053cc80b9fe0a83577d78ebe777813c
+external cc-oracle.py                  3f37ed778ed6658f60254706edac2db8ee8d82dc7e3fcfbb4e2c4774383a3607
+```
+
+The retained actual receipt still reports map hash
+`b3ac9adc5a060e6c080db9e4b8cb1d21fde589f9dbcc06036c97b3eea588b818`
+and 141 owners / 20 destinations / 87 sites / five caller files. The requested
+fix changes receipt scope disclosure only; it does not change source shape,
+mapping, policy, or oracle.
+
+## Verification
+
+- Warm targeted suites after the fix: 71 tests / 525 assertions, zero failures
+  or errors (`namespace-split-test` and `mcp-namespace-split-test`).
+- `~/bin/clj-kondo` on the two changed Clojure files: zero errors, zero warnings.
+- Formatter and `git diff --check`: pass.
+- Tree-derived counter evaluation: values above, arithmetic closed.
+- No `make test` was run. Astra's retained cold receipt at
+  `/var/tmp/forge/rows-sublime-4/make-test-receipt.json` remains the cold gate:
+  exit 0, 497.989 s, identical before/after source inventory hash
+  `c9bf36fccac68b297e2adc4ab3a75bee49ed45aead8980c745d568326ae26ced`.
+
+Verdict: **GO-WITH-FIX** with the five files named above. The only newly exposed
+escape is a documented, encoded, bounded macro/dynamic non-claim; it is not a
+design hole in the frozen static Cell C contract.
diff --git a/src/clj_surgeon/namespace_split.clj b/src/clj_surgeon/namespace_split.clj
index bf6a10c89c2f3cb5face162b01404811dce465c3..0b2bc573fa4de8e577824df43dd0be0674c4d0b4
--- a/src/clj_surgeon/namespace_split.clj
+++ b/src/clj_surgeon/namespace_split.clj
@@ -927,7 +927,9 @@
        :stale_references {:count (count stale) :sites stale :expected [] :scope scope}
        :facades {:forms facades :expected expected-facades :unexpected unexpected-facades
                  :policy "no-new-forwarders; retain existing unmapped forwarding owners"
-                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets] :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"}}
+                 :scope {:file (get-in request [:source :file]) :expected_match [:owner :kind :arity_targets]
+                         :scan "def symbol/Var/partial/fn aliases and single/multi-arity direct defn/apply forwarders"
+                         :excludes ["macro expansion" "arbitrary forwarding bodies" "dynamic resolution"]}}
        :exactly_once {:columns [:owner :destination_count :elsewhere_count] :rows counts
                       :preexisting_namesakes
                       (mapv (fn [x] (assoc x :after_hashes
diff --git a/test/clj_surgeon/namespace_split_test.clj b/test/clj_surgeon/namespace_split_test.clj
index 66cec67229f25ff49eb1b6b06d7e921e5eb91c8a..0f03fccdd6014131a146c403c7611df8ca4b672b
--- a/test/clj_surgeon/namespace_split_test.clj
+++ b/test/clj_surgeon/namespace_split_test.clj
@@ -937,7 +937,12 @@
                 "(def legacy (fn [x] (u/moved x)))"]]
     (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
                      (str "(ns app.views (:require [app.util :as u]))\n" body "\n"))]
-      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body))))
+      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body)))
+  (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
+                   "(ns app.views (:require [app.util :as u]))\n(defmacro legacy [& xs] `(u/moved ~@xs))\n")
+        fact (:facades (split/review-facts compiled))]
+    (is (= [] (:forms fact)))
+    (is (some #{"macro expansion"} (get-in fact [:scope :excludes])))))
 
 ;; @spec NS-SPLIT-062
 (deftest retention-policy-reports-existing-forwarders

tokens used
225,113
GO-WITH-FIX.

Verdict: [rows4-fence.md](/home/forge/src/clj-surgeon-fence/docs/observations/rows4-fence.md)

Applied the required bounded fix across:

- [namespace_split.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/namespace_split.clj)
- [namespace_split_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/namespace_split_test.clj)
- [namespace-split-design.md](/home/forge/src/clj-surgeon-fence/docs/intent/helper-extraction/namespace-split-design.md)
- [README.md](/home/forge/src/clj-surgeon-fence/README.md)
- [CHANGELOG.md](/home/forge/src/clj-surgeon-fence/CHANGELOG.md)

The receipt now explicitly excludes macro expansion, arbitrary forwarding bodies, and dynamic resolution. Outside-root and string references remain clearly bounded exclusions; reader conditionals, arity duplication, metadata changes, and docstring changes are detected.

Verification:

- Targeted suites: 71 tests / 525 assertions, all green.
- Receipt boundary: 65,279-byte escaped JSON admitted; 65,281 bytes refused.
- Patched Cell C receipt remains below ceiling: 63,249-byte JSON, 63,373-byte MCP text.
- Counters derived from tree: adopted 591, total 1610, non-MCP 246, TEST-ISO 20.
- Frozen Cell C hashes unchanged.
- `~/bin/clj-kondo`: zero errors/warnings.
- No `make test` run; Astra’s retained 497.989-second exit-0 receipt was used.
