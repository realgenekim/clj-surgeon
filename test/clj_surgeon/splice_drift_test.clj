(ns clj-surgeon.splice-drift-test
  "Witnesses for the byte-preserving splice guarantee and its receipt field.

   Evidence: docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md
   receipt 09:01Z — reducer_session.clj reformatted by exactly 158 lines in three
   runs and reducer_lab.clj by exactly 156 in two, deterministic per file."
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn- drift
  [& args]
  (apply (requiring-resolve 'clj-surgeon.splice-drift/drift-outside-spans) args))

(defn- gate
  [request]
  ((requiring-resolve 'clj-surgeon.splice-drift/gate) request))

(def ^:private churned-source
  (str "(ns app.foo\n"
       "  (:require [clojure.string :as str]\n            [clojure.set :as set])\n"
       "  (:import (java.util Date)\n           (java.io File)))\n\n"
       "(defn a [x]\n  ;; keep me\n  (str/join \",\"    [x x]))\n\n"
       "(defn b [y]\n      (set/union #{y}   #{1}))\n"))

;; @spec MCP-OP-CLOSE-005
(deftest identical-candidates-score-zero-without-alignment-work
  (let [result (drift churned-source churned-source
                      [{:offset 0 :length 11}])]
    (is (= 0 (:byte-drift-outside-span result)))
    (is (= 0 (:line-drift-outside-span result)))
    (is (= :exact (:span-alignment result)))))

;; @spec MCP-OP-CLOSE-005
(deftest a-change-inside-the-named-span-is-still-drift-from-what-was-asked
  ;; Round three, probe R5: rewriting the caller's own replacement text is
  ;; churn too. The gap-only number stays zero and stays honest; the number
  ;; that gates a commit counts the span as well.
  (let [reference "(defn a [] (new-call   1))\n(defn b [] (untouched   x))\n"
        span-offset (str/index-of reference "(new-call   1)")
        spans [{:offset span-offset :length (count "(new-call   1)")}]
        candidate (str/replace reference "(new-call   1)" "(new-call 1  )")
        result (drift reference candidate spans)]
    (is (= 0 (:byte-drift-outside-span result))
        "nothing outside the span moved, and that is all this number claims")
    (is (= :exact (:span-alignment result)))
    (is (pos? (:byte-drift-from-expected result))
        "but the bytes on disk would not be the bytes the request asked for")
    (is (false? (:ok (gate {:file "a.clj" :reference reference
                            :candidate candidate :spans spans
                            :commit? true})))
        "so a commit refuses")))

;; @spec MCP-OP-CLOSE-005
(deftest reformatting-outside-the-span-is-counted-in-bytes-and-lines
  (let [reference "(defn a [] (new))\n(defn b [] (untouched   x))\n"
        span-offset (str/index-of reference "(new)")
        candidate (str/replace reference "(untouched   x)" "(untouched x)")
        result (drift reference candidate
                      [{:offset span-offset :length (count "(new)")}])]
    (is (pos? (:byte-drift-outside-span result))
        "collapsing whitespace in an untouched form is drift")
    (is (= 2 (:byte-drift-outside-span result)))
    (is (= 1 (:line-drift-outside-span result)))
    (is (= :unlocatable (:span-alignment result))
        "an untouched gap that is not verbatim in the candidate is not aligned")))

;; @spec MCP-OP-CLOSE-005
(deftest a-missing-untouched-gap-is-reported-not-guessed
  (let [reference "(defn a [] (new))\n(defn b [] 2)\n(defn c [] 3)\n"
        span-offset (str/index-of reference "(new)")
        candidate "(defn a [] (new))\n(defn c [] 3)\n"
        result (drift reference candidate
                      [{:offset span-offset :length (count "(new)")}])]
    (is (= :unlocatable (:span-alignment result))
        "an untouched form vanished; no correspondence is guessed")
    (is (pos? (:byte-drift-outside-span result)))))

;; @spec MCP-OP-CLOSE-006
;; @spec MCP-OP-CLOSE-007
(deftest the-gate-refuses-drift-on-commit-and-reports-it-on-preview
  (let [reference "(defn a [] (new))\n(defn b [] (untouched   x))\n"
        span-offset (str/index-of reference "(new)")
        spans [{:offset span-offset :length (count "(new)")}]
        candidate (str/replace reference "(untouched   x)" "(untouched x)")
        clean reference]
    (testing "commit mode refuses positive drift and reports the number"
      (let [result (gate {:file "src/app/foo.clj"
                          :reference reference :candidate candidate
                          :spans spans :commit? true})]
        (is (false? (:ok result)))
        (is (= :byte-drift-outside-span (:error-type result)))
        (is (= 2 (:byte-drift-outside-span result)))
        (is (true? (:source-unchanged result)))
        (is (false? (:mutation-attempted result)))))
    (testing "preview mode allows it and shows the number"
      (let [result (gate {:file "src/app/foo.clj"
                          :reference reference :candidate candidate
                          :spans spans :commit? false})]
        (is (true? (:ok result)))
        (is (= 2 (:byte-drift-outside-span result)))))
    (testing "a byte-preserving candidate commits"
      (let [result (gate {:file "src/app/foo.clj"
                          :reference reference :candidate clean
                          :spans spans :commit? true})]
        (is (true? (:ok result)))
        (is (= 0 (:byte-drift-outside-span result)))))))

;; @spec MCP-OP-CLOSE-004
(deftest a-compiled-change-proves-itself-equal-to-the-raw-splice
  (testing "forms-scoped replace preserves every byte outside the matched span"
    (let [result (transaction/compile-transaction
                   {"src/app/foo.clj" churned-source}
                   {:changes [{:id :swap
                               :in ["src/app/foo.clj"]
                               :forms '[a]
                               :find "(str/join \",\"    [x x])"
                               :do [:replace "(str/join \",\" [x x x])"]
                               :expect {:matches 1}}]
                    :expect {:changes 1 :edits 1 :files 1}})]
      (is (:ok result))
      (is (= 0 (:byte-drift-outside-span result))
          "the compiler splices; this is the independent proof, not a hope")
      (is (str/includes? (get (:future-sources result) "src/app/foo.clj")
                         "(set/union #{y}   #{1})")
          "the untouched form keeps its odd spacing verbatim"))))

;; @spec MCP-OP-CLOSE-004
(deftest a-compiler-that-reprints-outside-the-span-is-refused
  (let [reprint (fn [source _edit]
                  (str/replace source "(set/union #{y}   #{1})"
                               "(set/union #{y} #{1})"))
        result (with-redefs-fn
                 {#'clj-surgeon.intent-transaction/apply-edits
                  (fn [source edits]
                    (reduce reprint source edits))}
                 #(transaction/compile-transaction
                   {"src/app/foo.clj" churned-source}
                   {:changes [{:id :swap
                               :in ["src/app/foo.clj"]
                               :forms '[a]
                               :find "(str/join \",\"    [x x])"
                               :do [:replace "(str/join \",\" [x x x])"]
                               :expect {:matches 1}}]
                    :expect {:changes 1 :edits 1 :files 1}}))]
    (is (= :reprint-outside-span-refused (:error-type result)))
    (is (= "src/app/foo.clj" (:file result)))))

(defn- temp-workspace []
  (let [dir (java.io.File/createTempFile "clj-surgeon-drift" "")]
    (.delete dir)
    (.mkdirs dir)
    dir))

;; @spec MCP-OP-CLOSE-006
;; @spec MCP-OP-CLOSE-008
(deftest a-whole-file-reformat-between-compile-and-commit-is-refused
  (let [workspace (temp-workspace)
        source-file (io/file workspace "foo.clj")
        receipt-file (io/file workspace "receipt.edn")
        original churned-source]
    (try
      (spit source-file original)
      (testing "the l1 reproduction: prepare-compiled! reformats untouched forms"
        (let [result
              (transaction/execute-change!
                {:spec {:changes [{:id :swap
                                   :in [(.getPath source-file)]
                                   :forms '[a]
                                   :find "(str/join \",\"    [x x])"
                                   :do [:replace "(str/join \",\" [x x x])"]
                                   :expect {:matches 1}}]
                        :expect {:changes 1 :edits 1 :files 1}}
                 :receipt-out (.getPath receipt-file)
                 :prepare-compiled!
                 (fn [compiled]
                   (update compiled :future-sources
                           (fn [sources]
                             (into {}
                                   (map (fn [[file source]]
                                          [file (str/replace
                                                  source
                                                  "(set/union #{y}   #{1})"
                                                  "(set/union #{y} #{1})")]))
                                   sources))))})]
          (is (false? (:ok result)))
          (is (= :byte-drift-outside-span (:error-type result)))
          (is (pos? (:byte-drift-outside-span result)))
          (is (= original (slurp source-file))
              "a refused write leaves every byte on disk unchanged")))
      (testing "an unformatted commit publishes a zero drift receipt"
        (let [result
              (transaction/execute-change!
                {:spec {:changes [{:id :swap
                                   :in [(.getPath source-file)]
                                   :forms '[a]
                                   :find "(str/join \",\"    [x x])"
                                   :do [:replace "(str/join \",\" [x x x])"]
                                   :expect {:matches 1}}]
                        :expect {:changes 1 :edits 1 :files 1}}
                 :receipt-out (.getPath receipt-file)})]
          (is (:ok result))
          (is (= 0 (:byte-drift-outside-span result)))
          (is (str/includes? (slurp source-file) "(set/union #{y}   #{1})"))))
      (finally
        (doseq [file (reverse (file-seq workspace))]
          (io/delete-file file true))))))

;; @spec MCP-OP-CLOSE-010
(deftest junk-adjacent-to-a-span-is-drift-not-a-longer-span
  (let [reference "AA\nSPAN\nBB"
        candidate "AA\nSPANJUNKJUNKJUNK\nBB"
        result (drift reference candidate [{:offset 3 :length 4}])]
    (is (pos? (:byte-drift-outside-span result))
        "a cursor-based search let 16 bytes of junk be absorbed by the span")
    (is (= :unlocatable (:span-alignment result)))))

;; @spec MCP-OP-CLOSE-011
(deftest whitespace-added-after-an-edited-form-is-drift
  (let [reference "(a)\n(keep)\n(b)\n(keep)\n(c)"
        candidate "(a)\n(keep)\n(b)   \n(keep)\n(c)"
        result (drift reference candidate [{:offset 11 :length 3}])]
    (is (pos? (:byte-drift-outside-span result))
        "trailing whitespace after the span floated the following gap")
    (is (= :unlocatable (:span-alignment result)))))

;; @spec MCP-OP-CLOSE-012
(deftest an-insertion-at-a-zero-length-span-is-drift
  (let [result (drift "abcdef" "abZZZcdef" [{:offset 2 :length 0}])]
    (is (pos? (:byte-drift-outside-span result))
        "a zero-length span must not absorb inserted content")
    (is (= :unlocatable (:span-alignment result)))))

;; @spec MCP-OP-CLOSE-010
(deftest gaps-are-compared-at-their-own-offsets-not-searched-for
  (testing "an unchanged candidate with no spans at all"
    (is (= 0 (:byte-drift-outside-span (drift "abc" "abc" []))))
    (is (pos? (:byte-drift-outside-span (drift "abc" "aXc" []))))) 
  (testing "a span may change content while keeping its length"
    (is (= 0 (:byte-drift-outside-span
               (drift "abcdef" "abZZef" [{:offset 2 :length 2}])))))
  (testing "a span that changes length makes every later offset unknowable"
    (let [result (drift "abcdef" "abZef" [{:offset 2 :length 2}])]
      (is (pos? (:byte-drift-outside-span result)))
      (is (= :unlocatable (:span-alignment result))
          "unverified is the honest answer, and it fails closed at commit"))))
