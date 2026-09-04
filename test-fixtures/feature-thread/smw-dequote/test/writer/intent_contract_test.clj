(ns writer.intent-contract-test
  "Linked Intent Development contract test.

   Asserts traceability in BOTH directions between docs/intent/registry.edn and
   the code/test tags that witness it:

     - every :active intent has at least one code witness  (`INTENT: <id>`)
     - every :active intent has at least one test witness  (`INTENT-TEST: <id>`)
     - every tag in the tree names an intent the registry knows

   One direction alone lets you delete an intent by deleting its code, or
   invent one by writing a tag. This test is discovered by the ordinary runner
   (`make runtests-once`), because a gate you have to remember to run is not a
   gate."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def registry-path "docs/intent/registry.edn")

;; The colon immediately after INTENT makes these regexes disjoint:
;; "INTENT-TEST:" can never match the code-witness pattern.
(def code-tag-re #"INTENT:\s*([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)*)")
(def test-tag-re #"INTENT-TEST:\s*([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)*)")

(def scanned-roots ["src" "resources/public/js" "test"])

(def scanned-extensions #{".clj" ".cljc" ".cljs" ".js"})

(defn- scanned-files []
  (->> scanned-roots
       (map io/file)
       (filter #(.exists ^java.io.File %))
       (mapcat file-seq)
       (filter #(.isFile ^java.io.File %))
       (filter (fn [^java.io.File f]
                 (some #(str/ends-with? (.getName f) %) scanned-extensions)))))

(defn- tags-in
  "Set of intent ids matched by `re` across every scanned file, excluding the
   registry itself and this contract test (both mention ids as data)."
  [re]
  (->> (scanned-files)
       (remove #(str/includes? (.getPath ^java.io.File %) "intent_contract_test"))
       (mapcat (fn [f] (map second (re-seq re (slurp f)))))
       (map keyword)
       set))

(defn- registry []
  (edn/read-string (slurp registry-path)))

(deftest registry-is-well-formed
  (let [rows (registry)]
    (is (seq rows) "the intent registry must not be empty")
    (doseq [row rows]
      (testing (str (:id row))
        (is (keyword? (:id row)))
        (is (#{:active :retired :superseded} (:status row)))
        (is (string? (:ears row)))
        (is (seq (:misreadings row))
            "a row without misreadings is a label, not a contract")
        (is (seq (:tests row)))))
    (is (= (count rows) (count (set (map :id rows))))
        "intent ids must be unique")))

(deftest every-active-intent-has-a-code-and-test-witness
  (let [active (->> (registry) (filter #(= :active (:status %))) (map :id) set)
        code (tags-in code-tag-re)
        tests (tags-in test-tag-re)]
    (is (empty? (set/difference active code))
        (str "active intents with no `INTENT:` code witness: "
             (pr-str (set/difference active code))))
    (is (empty? (set/difference active tests))
        (str "active intents with no `INTENT-TEST:` test witness: "
             (pr-str (set/difference active tests))))))

(deftest every-tag-names-a-known-intent
  (let [known (->> (registry) (map :id) set)
        tagged (set/union (tags-in code-tag-re) (tags-in test-tag-re))]
    (is (empty? (set/difference tagged known))
        (str "tags naming intents the registry does not know: "
             (pr-str (set/difference tagged known))))))
