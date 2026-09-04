(ns clj-surgeon.mcp-relation-census-launcher-test
  "Every relation-census witness that drives a REAL LAUNCHER as a subprocess.

   A namespace of its own for a reason that is a gate, not taste: the trunk's
   `default-ceilings-admit-every-source-in-this-repository` requires the
   shipped parse-node ceiling to keep a 4x margin over the largest source in
   this repository, and `mcp_relation_census_test.clj` had grown past that
   line (50,214 nodes at 563c300d against a 50,000 budget) BEFORE round twenty
   added anything. The launcher witnesses are the natural seam: they share one
   piece of machinery (`raw-launcher`), they are the slowest witnesses in the
   lane because each assertion costs a JVM or a babashka process, and nothing
   else in the census test namespace uses them.

   Round nineteen's item 1 and round twenty's item 1 both live here because
   they are the same subject one frame apart: what the public CLI entrance
   PRINTS, and whether it is bounded."
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [babashka.process :as proc]
   [clj-surgeon.relation-census :as census]))

(def ^:private repo-root (.getCanonicalPath (java.io.File. ".")))

;; ---------------------------------------------------------------------------
;; ROUND NINETEEN, item 1 — Sol's round-eighteen BLOCKING finding.
;;
;; Round seventeen bounded the census op's exits and round eighteen bounded
;; both entrances' last steps, and the brief then declared the LAUNCHER's own
;; refusals out of scope because they "belong to no op". The reviewer's ruling,
;; and it stands: "'Belongs to no op' is not a valid bound exemption. It can
;; explain why these names do not belong in `cli-refusal-types`, but it cannot
;; exempt the public CLI entrance from the global CENSUS-014 promise that no
;; refusal field is unbounded."
;;
;; Reproduced at 3b7904a through the REAL launcher, not a fn call:
;;
;;   duplicate EXIT=1 BYTES=20228 MAX_A_RUN=10001 MARKERS=0 :duplicate-argument
;;   invalid   EXIT=1 BYTES=10064 MAX_A_RUN=10001 MARKERS=0 :invalid-arguments
;;
;; `parse-args` throws BEFORE dispatch, so neither `run-relation-census`'s last
;; step nor `run`'s shape exit ever sees the value; `-main`'s catch-all printed
;; `(merge (ex-data e) {:error (.getMessage e)})` verbatim.
;;
;; THE RULE: the bound is a property of the EXIT, not of the op. Every refusal
;; the CLI can print — op or launcher — leaves through one bounded exit, and it
;; is the same `census/bound-refusal` the op's exit uses, for the reason that
;; function's own docstring gives: a bound enforced at some of the sites is not
;; a bound, it is those sites' habit.
;;
;; And the launcher's names are DECLARED, in `census/launcher-refusal-types`,
;; so the enumeration that makes the bound total covers them: an unenumerated
;; refusal is one no witness can drive, which is how this one shipped.
;; ---------------------------------------------------------------------------

(def ^:private hostile-argument-length
  "The length the reviewer drove, so the receipt above is the drive below."
  10001)

(defn- hostile-argument
  []
  (apply str (repeat hostile-argument-length \a)))

(defn- raw-launcher
  "Run one REAL launcher as a subprocess and return its raw bytes.

   Raw, and not `edn/read-string`, because the finding is about the BYTES the
   launcher publishes: a reader that parses the map back has already thrown
   away the question of how big the thing on stdout was."
  [runtime args]
  (let [launcher (case runtime
                   :bb ["bb" "-cp" (str repo-root "/src")
                        "-m" "clj-surgeon.core"]
                   :jvm ["java" "-cp" (System/getProperty "java.class.path")
                         "clojure.main" "-m" "clj-surgeon.core"])
        {:keys [out err exit]}
        (apply proc/shell {:out :string :err :string :continue true}
               (concat launcher args))]
    {:out out :err err :exit exit
     :parsed (try (edn/read-string out) (catch Exception _ nil))}))

(defn- launcher-drives
  "One drive per name `census/launcher-refusal-types` declares.

   Both are raised by `parse-args`, before dispatch knows which op it is
   building — which is exactly why they escaped every bound the op grew."
  []
  (let [big (hostile-argument)]
    [{:label :duplicate-argument
      :error-type :duplicate-argument
      :args [":op" ":relation-census" ":doors" big ":doors" big]}
     {:label :invalid-arguments
      :error-type :invalid-arguments
      :args [":op" ":relation-census" ":doors" (str "[1" big "]")]}
     ;; Both dispatch refusals for an op nobody defines: `run-op`'s, which the
     ;; launcher reaches for an ordinary invocation, and `-main`'s, which it
     ;; reaches under `--help`. Two sites, one name, and only one of them was
     ;; bounded when this set was first written.
     {:label :unknown-operation
      :error-type :unknown-operation
      :args [":op" big]}
     {:label :unknown-operation-under-help
      :error-type :unknown-operation
      :args [":op" big ":help" "true"]}]))

(defn- printed-leaf-lengths
  "The RENDERED length of every leaf a parsed refusal carries.

   `pr-str`, and every leaf rather than every string, because Opus's
   round-nineteen item 1 is exactly the gap between those two measures: what
   the caller reads is printed output, and a 10,001-character keyword is
   10,002 characters on their terminal however `string?` answers about it. A
   bound that asks `string?` is a bound on the type the author happened to
   picture."
  [parsed]
  (->> (tree-seq coll? seq parsed)
       (remove coll?)
       (map #(count (pr-str %)))))

;; @spec MCP-OP-CENSUS-014
(deftest every-refusal-the-launcher-itself-prints-is-bounded-at-its-exit
  (let [drives (launcher-drives)
        marker-slack 64
        ceiling (+ census/max-refusal-field-chars marker-slack)]

    (testing "the drives cover every refusal the launcher declares it can print"
      (is (= census/launcher-refusal-types
             (set (map :error-type drives)))
          (str "declared: " (pr-str census/launcher-refusal-types)
               "; driven: " (pr-str (set (map :error-type drives))))))

    (doseq [runtime [:jvm :bb]
            {:keys [label error-type args]} drives]
      (let [{:keys [out exit parsed]} (raw-launcher runtime args)]
        (testing (str runtime " " label " refuses as the declared type")
          (is (= 1 exit)
              (str runtime " " label " exited " exit ": " (pr-str out)))
          (is (map? parsed)
              (str runtime " " label " printed no readable refusal: "
                   (pr-str (subs (str out) 0 (min 400 (count (str out)))))))
          (is (= error-type (:error-type parsed))
              (str runtime " " label " refused "
                   (pr-str (:error-type parsed)))))

        (testing (str runtime " " label " is bounded at the launcher's exit")
          (let [longest (reduce max 0 (printed-leaf-lengths parsed))]
            (is (<= longest ceiling)
                (str runtime " " label " published a " longest
                     "-character field, over the " ceiling
                     "-character ceiling — the launcher's refusal is "
                     "unbounded")))
          (is (str/includes? (str out) "[truncated:")
              (str runtime " " label
                   " truncated nothing and said nothing: the caller's own "
                   "10,001-character argument came back whole"))
          (is (not (re-find (re-pattern (str "a{" hostile-argument-length "}"))
                            (str out)))
              (str runtime " " label
                   " echoed the whole hostile argument back"))
          (is (< (alength (.getBytes (str out) "UTF-8")) 8192)
              (str runtime " " label " published "
                   (alength (.getBytes (str out) "UTF-8"))
                   " bytes")))))))

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
  (let [big (hostile-argument)]
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
      (let [{:keys [out exit parsed]} (raw-launcher runtime args)]
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
          (let [longest (reduce max 0 (printed-leaf-lengths parsed))]
            (is (<= longest ceiling)
                (str runtime " " label " published a leaf that RENDERS as "
                     longest " characters, over the " ceiling
                     "-character ceiling — the bound is enforced on one type "
                     "inside the value rather than on the value")))
          (is (str/includes? (str out) "[truncated:")
              (str runtime " " label
                   " truncated nothing and said nothing: the caller's own "
                   "10,001-character argument came back whole"))
          (is (not (re-find (re-pattern (str "a{" hostile-argument-length "}"))
                            (str out)))
              (str runtime " " label
                   " echoed the whole hostile argument back"))
          (is (< (alength (.getBytes (str out) "UTF-8")) 8192)
              (str runtime " " label " published "
                   (alength (.getBytes (str out) "UTF-8")) " bytes")))))))

