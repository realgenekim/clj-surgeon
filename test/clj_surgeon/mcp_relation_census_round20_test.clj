(ns clj-surgeon.mcp-relation-census-round20-test
  "Round-twenty witnesses for the relation-census lane.

   A namespace of its own, and the reason is a gate rather than taste: the
   trunk's `default-ceilings-admit-every-source-in-this-repository` requires
   the shipped parser ceiling to keep a 4x margin over the largest source in
   this repository, and `mcp_relation_census_test.clj` IS that source — at
   50,960 nodes against a 200,000 ceiling it leaves 4x headroom only while
   nothing is added to it. Round twenty's witnesses therefore live here, and
   so should round twenty-one's. The shared drive machinery stays in the
   census test namespace, public rather than copied, because two sets that
   agree until they do not is the defect class this lane exists to close."
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clj-surgeon.mcp-relation-census-test :as census-test]
   [clj-surgeon.relation-census :as census]))

;; ---------------------------------------------------------------------------
;; ROUND TWENTY, item 1 — Opus's round-nineteen BLOCKING finding.
;;
;; Round nineteen gave the launcher ONE bounded exit and called the bound
;; total. It was not. `census/bound-refusal` postwalked STRINGS only, and
;; `core/parse-val` mints a KEYWORD out of any CLI value beginning with `:`
;; and READS any value beginning with `[` or `{`, so the caller controls a
;; non-string leaf that rides straight through the bound. Measured by the
;; reviewer at the real launchers, and reproduced at this branch's tip:
;;
;;   jvm-dup-keyword EXIT=1 BYTES=20287 MAX_A_RUN=10001 MARKERS=0 :duplicate-argument
;;   bb-dup-keyword  EXIT=1 BYTES=20226 MAX_A_RUN=10001 MARKERS=0 :duplicate-argument
;;   jvm-dup-symbol  EXIT=1 BYTES=20289 MAX_A_RUN=10001 MARKERS=0 :duplicate-argument
;;   jvm-kw-vector   EXIT=1 BYTES=11667 MAX_A_RUN=10001 MARKERS=1 :doors-not-a-string
;;   jvm-map         EXIT=1 BYTES=11672 MAX_A_RUN=10001 MARKERS=1 :doors-not-a-string
;;
;; The last two are the OP's own entrance exit, not the launcher's, so this is
;; one root cause reaching two exits: a bound applied to one type inside a
;; value is not a bound on the value.
;;
;; The round-nineteen witness was blind twice over — every drive built its
;; hostile argument as a string, and the assertion filtered the parsed tree
;; with `string?` before measuring — which is the round-eighteen lesson one
;; frame over: an enumeration that describes a subset of what an entrance
;; emits is green over the rest.
;;
;; THE RULE: THE BOUND IS OVER THE VALUE AS PRINTED, not over one type inside
;; it. A keyword, a symbol, a vector of them and a nested map are bounded
;; exactly as a string is, at BOTH real launchers and at both exits, and this
;; witness drives all four through both as subprocesses.
;; ---------------------------------------------------------------------------

(defn- printed-value-drives
  "One drive per non-string shape `core/parse-val` can mint from CLI text.

   Two exits, deliberately: `:duplicate-argument` is the LAUNCHER's own
   refusal, raised by `parse-args` before dispatch; `:doors-not-a-string` is
   the OP's entrance exit. One root cause reaches both, so one witness drives
   both."
  []
  (let [big (census-test/hostile-argument)]
    [{:label :keyword-at-the-launchers-exit
      :error-type :duplicate-argument
      :args [":op" ":relation-census"
             ":doors" (str ":" big) ":doors" (str ":" big)]}
     {:label :symbol-at-the-launchers-exit
      :error-type :duplicate-argument
      :args [":op" ":relation-census"
             ":doors" (str "[" big "]") ":doors" (str "[" big "]")]}
     {:label :keyword-vector-at-the-ops-exit
      :error-type :doors-not-a-string
      :args [":op" ":relation-census" ":dir" "." ":doors" (str "[:" big "]")]}
     {:label :nested-map-at-the-ops-exit
      :error-type :doors-not-a-string
      :args [":op" ":relation-census" ":dir" "." ":doors" (str "{:k :" big "}")]}]))

;; @spec MCP-OP-CENSUS-014
(deftest no-refusal-either-real-launcher-prints-carries-an-unbounded-printed-value
  (let [drives (printed-value-drives)
        marker-slack 64
        ;; AT the ceiling, never at a constant: the assertion moves when the
        ;; declared bound moves, which is what makes it a witness for the rule
        ;; rather than for today's number.
        ceiling (+ census/max-refusal-field-chars marker-slack)]

    (testing "every driven name is DECLARED at the exit it leaves through"
      (doseq [{:keys [label error-type]} drives]
        (is (contains? (into census/launcher-refusal-types
                             census/cli-refusal-types)
                       error-type)
            (str label " drives " (pr-str error-type)
                 ", which neither declared refusal set contains, so no "
                 "enumeration witness could see it"))))

    (doseq [runtime [:jvm :bb]
            {:keys [label error-type args]} drives]
      (let [{:keys [out exit parsed]} (census-test/raw-launcher runtime args)]
        (testing (str runtime " " label " refuses as the declared type")
          (is (= 1 exit)
              (str runtime " " label " exited " exit ": " (pr-str out)))
          (is (map? parsed)
              (str runtime " " label " printed no readable refusal: "
                   (pr-str (subs (str out) 0 (min 400 (count (str out)))))))
          (is (= error-type (:error-type parsed))
              (str runtime " " label " refused "
                   (pr-str (:error-type parsed)))))

        (testing (str runtime " " label " is bounded AS PRINTED")
          (let [longest (reduce max 0 (census-test/printed-leaf-lengths parsed))]
            (is (<= longest ceiling)
                (str runtime " " label " published a leaf that RENDERS as "
                     longest " characters, over the " ceiling
                     "-character ceiling — the bound is enforced on one type "
                     "inside the value rather than on the value")))
          (is (str/includes? (str out) "[truncated:")
              (str runtime " " label
                   " truncated nothing and said nothing: the caller's own "
                   "10,001-character argument came back whole"))
          (is (not (re-find (re-pattern (str "a{" census-test/hostile-argument-length "}"))
                            (str out)))
              (str runtime " " label
                   " echoed the whole hostile argument back"))
          (is (< (alength (.getBytes (str out) "UTF-8")) 8192)
              (str runtime " " label " published "
                   (alength (.getBytes (str out) "UTF-8")) " bytes")))))))

