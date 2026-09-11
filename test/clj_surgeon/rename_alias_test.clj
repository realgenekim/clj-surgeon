(ns clj-surgeon.rename-alias-test
  {:lane :fast}
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.insert-forms-oracle]
            [clj-surgeon.insert-forms-plan]
            [clj-surgeon.insert-forms-support]
            [clj-surgeon.rename-alias]
            [clj-surgeon.rename-alias-oracle]
            [clj-surgeon.rename-alias-plan]
            [clj-surgeon.txn-journal]
            [clojure.edn]
            [clojure.java.io]
            [clojure.string]
))


(def header "(ns demo (:require [example.events :as events]))\n")

(def file "src/example.clj")

(defn request [sources n]
  {:version 1 :workspace_root "/fixture" :scope {:paths (vec (sort (keys sources))) :expect_files (count sources)}
   :lib "example.events" :old_alias "events" :new_alias "ev"
   :expect {:references {:total n}} :guards (into {} (map (fn [[f s]] [f {:sha256 (clj-surgeon.insert-forms-support/sha s)}]) sources))})

(defn accept [source expected n & [change]]
  (let [sources {file source} req (merge (request sources n) change) result (clj-surgeon.rename-alias/plan sources req)]
    (is (= [true {file expected} n true]
           [(:ok result) (:candidates result) (get-in result [:receipt :references_changed])
            (when (:ok result)
              (:ok (clj-surgeon.rename-alias-oracle/verify source (get-in result [:candidates file]) req (first (get-in result [:detail :per_file])))))])
        (pr-str result))
    result))

(defn refuse [sources req kind]
  (let [result (clj-surgeon.rename-alias/plan sources req)]
    (is (= [kind {:state "refused" :committed false :mutation_attempted false :source_unchanged true} sources]
           [(:error-type result)
            (select-keys result [:state :committed :mutation_attempted :source_unchanged])
            (merge sources (:candidates result))]) (pr-str result))
    result))

(defn rejected [s kind & [change]]
  (refuse {file s} (merge (request {file s} 0) change) kind))


(def candidate-source (str clj-surgeon.rename-alias-test/header "(def y events/x)\n(def untouched 1)\n"))

(defn fault-result [seam fault]
  (clj-surgeon.insert-forms-support/with-file candidate-source
    (fn [dir file _]
      (let [req (assoc (clj-surgeon.rename-alias-test/request {clj-surgeon.rename-alias-test/file candidate-source} 1) :workspace_root (.getCanonicalPath dir))
            result (with-bindings {seam fault} (clj-surgeon.rename-alias/execute! req))]
        (is (= :candidate-structure-mismatch (:error-type result)) (pr-str result))
        (is (= candidate-source (slurp file)))
        result))))


(def scope-source (str clj-surgeon.rename-alias-test/header "events/x\n"))

(defn repository-run [selected skipped prepare]
  (clj-surgeon.insert-forms-support/with-file selected
    (fn [dir target _]
      (let [other (clojure.java.io/file dir "src/skipped.clj")
            sources (cond-> {clj-surgeon.rename-alias-test/file selected} skipped (assoc "src/skipped.clj" skipped))]
        (when skipped (spit other skipped))
        (when prepare (prepare dir))
        (let [req (assoc (clj-surgeon.rename-alias-test/request sources 1) :workspace_root (.getCanonicalPath dir)
                         :scope {:repository true :expect_files (count sources)})
              result (clj-surgeon.rename-alias/execute! req)]
          (is (= "committed" (:state result)) (pr-str result))
          (is (= (-> selected (clojure.string/replace ":as events" ":as ev") (clojure.string/replace "events/" "ev/")) (slurp target)))
          (when skipped (is (= skipped (slurp other)))))))))


;; @spec RENAME-ALIAS-001
;; INTENT-TEST: RENAME-ALIAS-001

(deftest rename-alias-e4-verbatim
  (testing "rename-alias-e4-verbatim"
  (let [f "src/cfp_scheduler_killer/views/schedule.clj"
        a (slurp "test-fixtures/rename-alias/e4-schedule.clj")
        req (assoc (request {f a} 31) :lib "cfp-scheduler-killer.events")
        r (refuse {f a} req :expect-count-mismatch)]
    (is (= "5f086f7789fdb556ba6e819b26d6eba8ec0345e22b7aaa152b2e1a03648234a1" (clj-surgeon.insert-forms-support/sha a)))
    (is (= 2 (:actual_count r)))
    (is (= [153 1112] (mapv :line (get-in r [:write_refusal_evidence :items]))))
    (is (= [537 4661] (mapv #(get-in % [:address :preorder]) (get-in r [:write_refusal_evidence :items]))))
    (let [r (clj-surgeon.rename-alias/plan {f a} (assoc-in req [:expect :references :total] 2)) b (get-in r [:candidates f])]
      (is (:ok r) (pr-str r))
      (when b
        (is (= "02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c" (clj-surgeon.insert-forms-support/sha b)))
        (is (= [4 153 1112] (keep-indexed #(when (not= %2 (nth (clojure.string/split-lines b) %1)) (inc %1)) (clojure.string/split-lines a))))
        (is (= 29 (count (re-seq #"events/" b))))
        (let [strings (fn [s] (map :text (filter #(and (= :token (:tag %)) (clojure.string/starts-with? (:text %) "\""))
                                               (tree-seq (comp seq :entries) :entries (clj-surgeon.insert-forms-oracle/inventory s)))))
              routes (filter #(clojure.string/includes? % "events/") (strings b))]
          (is (= 29 (count routes)))
          (is (= 22 (count (distinct (map :text
                                           (filter #(and (= :list (:tag %)) (= "str" (clj-surgeon.insert-forms-oracle/operator %))
                                                         (clojure.string/includes? (or (:text (second (clj-surgeon.insert-forms-oracle/code %))) "") "events/"))
                                                   (tree-seq (comp seq :entries) :entries (clj-surgeon.insert-forms-oracle/inventory b))))))))
          (is (= (frequencies (strings a)) (frequencies (strings b))))))
      (is (= 3 (get-in r [:receipt :forms_changed])))
      (is (= 32 (get-in r [:receipt :other_forms_checked]))))))
)



;; @spec RENAME-ALIAS-002
;; INTENT-TEST: RENAME-ALIAS-002


;; @spec RENAME-ALIAS-003
;; INTENT-TEST: RENAME-ALIAS-003


;; @spec RENAME-ALIAS-004
;; INTENT-TEST: RENAME-ALIAS-004


;; @spec RENAME-ALIAS-005
;; INTENT-TEST: RENAME-ALIAS-005

(deftest rename-alias-role-selection
  (testing "rename-alias-prefix-trap"
  (accept "(ns demo (:require [example.events :as ev]))\n[ev/anything event/x ev/foo-bar ev/z?]"
          "(ns demo (:require [example.events :as next]))\n[next/anything event/x next/foo-bar next/z?]"
          3 {:old_alias "ev" :new_alias "next"}))
  (testing "rename-alias-reader-roles"
  (accept "(ns ^{:tag events/T} demo (:require [example.events :as events]))"
          "(ns ^{:tag ev/T} demo (:require [example.events :as ev]))" 1)
  (doseq [[a b n] [["[::events/kw :events/kw ::kw]" "[::ev/kw :events/kw ::kw]" 1]
                    ["['events/x #'events/y `events/z]" "['ev/x #'ev/y `ev/z]" 3]
                    ["`[events/x ~events/y ~@events/z]" "`[ev/x ~ev/y ~@ev/z]" 3]
                    ["^events/T ^{::events/k events/v} x" "^ev/T ^{::ev/k ev/v} x" 3]
                    ["(let [{::events/keys [x]} m] x)" "(let [{::ev/keys [x]} m] x)" 1]
                    ["[#::events{:x events/y} #:events{:x events/y}]" "[#::ev{:x ev/y} #:events{:x ev/y}]" 3]
                    ["#events/tag [events/x ::events/k]" "#events/tag [ev/x ::ev/k]" 2]
                    ["(comment events/x)" "(comment ev/x)" 1]
                    ["(quote (alias events/x))" "(quote (alias ev/x))" 1]]]
    (accept (str header a) (str (clojure.string/replace header ":as events" ":as ev") b) n)))
  (testing "rename-alias-string-decoy"
  (let [body "(def x \"events/x \\\"events/z\\\"\n events/y\")\n#\"events/x\" \\e ; events/x\n(comment events/x)"]
    (accept (str header body) (str (clojure.string/replace header ":as events" ":as ev") (clojure.string/replace body "(comment events/x)" "(comment ev/x)")) 1)))
  (testing "rename-alias-discard-decoy"
  (doseq [body ["#_events/x" "#_[::events/k #_events/x events/y]" "#_(ns wrong (:require [x :as ev]))"]]
    (accept (str header body) (str (clojure.string/replace header ":as events" ":as ev") body) 0))
  (rejected (str header "#_[events/x") :source-parse-error)
  (doseq [body ["#_#?(:clj events/x)" "#_#=(events/x)"]]
    (rejected (str header body) :unsupported-source)))
)



;; @spec RENAME-ALIAS-006
;; INTENT-TEST: RENAME-ALIAS-006

(deftest rename-alias-binding-matrix
  (testing "rename-alias-binding-matrix"
  (doseq [[s kind] [["(ns demo (:require [example.events :as events] [other :as ev]))" :alias-collision]
                    ["(ns demo (:require [example.events :as events] [other :as-alias ev]))" :alias-collision]
                    ["(ns demo (:require [example.events :as events] [example.events :as e]))" :ambiguous-alias-binding]
                    ["(ns demo (:require [example.events :as events] [other :as events]))" :ambiguous-alias-binding]
                    ["(ns demo (:require [other :as events]))" :alias-library-mismatch]
                    ["(ns demo)" :old-alias-absent]
                    [(str header "ev/x") :new-alias-capture]
                    [(str header "::ev/k") :new-alias-capture]
                    [(str header "#::ev{:k 1}") :new-alias-capture]
                    ["(ns demo (:require [example.events :as events :refer [x] :rename {y z}]))" :unsupported-libspec]
                    ["(ns demo (:require [example.events :as events :as x]))" :unsupported-libspec]]]
    (rejected s kind))
  (doseq [s ["(ns demo (:require [example.events :as-alias events]))"
             "(ns demo (:require [example.events :as events :refer [ev x] :rename {x y} :exclude [z]]))"
             "(ns demo (:require [example.events :as events :refer :all :rename {x y}]))"
             (str header "(let [ev 1] [ev :ev/x #_ev/x])")]]
    (accept s (clojure.string/replace s #":as(-alias)? events" ":as$1 ev") 0))
  (let [sources {"a.clj" "(ns a (:require [example.events :as events] [other :as ev]))"
                 "b.clj" "(ns b)"}]
    (refuse sources (request sources 0) :old-alias-absent))
  (let [sources {"a.clj" "(ns a (:require [example.events :as events] [other :as ev]))"
                 "b.clj" (str header "(require 'other)")}]
    (refuse sources (request sources 0) :unsupported-namespace-mutation))
  (accept "(ns demo (:require [_ :as events]))" "(ns demo (:require [_ :as ev]))" 0 {:lib "_"})
  (accept "(ns demo (:require [example.events :as events :refer [x'] :rename {x' y'}]))"
          "(ns demo (:require [example.events :as ev :refer [x'] :rename {x' y'}]))" 0)
  (doseq [alias ["nil" "&" "a/b" "a b" ":x" "#x" ""]]
    (rejected header :invalid-request {:new_alias alias}))
  (rejected header :invalid-request {:new_alias "events"})
  (rejected "(ns demo (:require [events :as events]))" :ambiguous-alias-namespace {:lib "events"}))
)



;; @spec RENAME-ALIAS-016
;; INTENT-TEST: RENAME-ALIAS-016


;; @spec RENAME-ALIAS-016
;; INTENT-TEST: RENAME-ALIAS-016


;; @spec RENAME-ALIAS-016
;; INTENT-TEST: RENAME-ALIAS-016


;; @spec RENAME-ALIAS-016
;; INTENT-TEST: RENAME-ALIAS-016

(deftest rename-alias-scope
  (testing "repository-ignores-non-clojure-symlinks"
  (repository-run scope-source nil
                  (fn [dir]
                    (let [real (clojure.java.io/file dir "README.txt") link (clojure.java.io/file dir "README-link.txt")]
                      (spit real "ordinary repository furniture")
                      (java.nio.file.Files/createSymbolicLink (.toPath link) (.toPath real)
                        (make-array java.nio.file.attribute.FileAttribute 0))))))
  (testing "comment-ancestry-exempts-mutations-but-keeps-references"
  (doseq [head ["comment" "clojure.core/comment"]]
    (repository-run (str clj-surgeon.rename-alias-test/header "(" head " (do (require '[y :as z]) (events/x)))\n")
                    "(ns skipped)\n(comment (require '[other :as q]))\n" nil)))
  (testing "skipped-namespace-mutations-do-not-refuse"
  (repository-run scope-source "(ns skipped)\n(require '[other :as q])\n" nil))
  (testing "skipped-duplicate-bindings-do-not-refuse"
  (repository-run scope-source "(ns skipped (:require [q.r :as u] [q.r :as v]))\n" nil))
)



;; @spec RENAME-ALIAS-007
;; INTENT-TEST: RENAME-ALIAS-007

(deftest rename-alias-stale-and-counts
  (testing "rename-alias-scope-guards-counts"
  (let [a (str header "events/x events/y") sources {file a "src/b.clj" "(ns b)"}
        req (assoc (request sources 2) :scope {:repository true :expect_files 2})]
    (let [r (clj-surgeon.rename-alias/plan sources req)]
      (is (:ok r))
      (is (= ["src/b.clj" file] (mapv :file (get-in r [:detail :per_file]))))
      (is (= 0 (:references_changed (first (get-in r [:detail :per_file])))))
      (is (= 1 (get-in r [:detail :per_file 0 :preservation :other_forms_checked]))))
    (refuse sources (assoc-in req [:scope :expect_files] 1) :scope-file-count-mismatch)
    (refuse sources (update req :guards dissoc "src/b.clj") :invalid-guard)
    (refuse sources (assoc-in req [:guards file :sha256] (apply str (repeat 64 "0"))) :source-hash-mismatch)
    (let [r (refuse sources (assoc req :expect {:references {:per_file {file 31 "src/b.clj" 0}}}) :expect-count-mismatch)]
      (is (= {file 2 "src/b.clj" 0} (:actual_count r)))
      (is (= [file] (:mismatched_files r)))
      (is (= 2 (count (get-in r [:write_refusal_evidence :items]))))))
  (let [req (request {file header} 0)
        receipt {:version 1 :read_complete true :workspace_root "/fixture" :file file :sha256 (clj-surgeon.insert-forms-support/sha header)}]
    (is (:ok (clj-surgeon.rename-alias/plan {file header} (assoc-in req [:guards file] {:read_receipt receipt})))))
  (clj-surgeon.insert-forms-support/with-file header
    (fn [dir target _]
      (let [req (assoc (request {file header} 0) :workspace_root (.getCanonicalPath dir)
                       :scope {:repository true :expect_files 1})
            r (clj-surgeon.rename-alias/execute! req {:before-recheck #(spit (clojure.java.io/file dir "extra.clj") "(ns extra)")})]
        (is (= :scope-changed-before-commit (:error-type r)))
        (is (= false (:mutation_attempted r)))
        (is (= header (slurp target))))))
  (doseq [scope [{:file file :expect_files 1} {:paths [file] :expect_files 1} {:repository true :expect_files 1}]]
    (is (:ok (clj-surgeon.rename-alias/plan {file header} (assoc (request {file header} 0) :scope scope))))))
)



;; @spec RENAME-ALIAS-008
;; INTENT-TEST: RENAME-ALIAS-008


;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012

(deftest rename-alias-parse
  (testing "rename-alias-source-boundaries"
  (doseq [[s kind] [["(def x 1)" :ns-not-found]
                    ["(ns a) (ns b)" :multiple-ns-forms]
                    ["1 (ns demo)" :unsupported-ns-shape]
                    ["(ns demo (:use foo))" :unsupported-ns-shape]
                    ["(ns demo (:load \"foo\"))" :unsupported-ns-shape]
                    ["(ns demo (:require (foo [bar :as events])))" :unsupported-ns-shape]
                    [(str header "(alias 'x 'y)") :unsupported-namespace-mutation]
                    [(str header "(clojure.core/in-ns 'x)") :unsupported-namespace-mutation]
                    [(str header "#?(:clj events/x)") :unsupported-source]
                    [(str "\uFEFF" header) :unsupported-source]
                    [(str header "\r\n") :unsupported-source]]]
    (rejected s kind))
  (accept (str (clojure.string/replace header "\n" "\r\n") "[\"λ\"\t events/x]\r\n")
          (str (clojure.string/replace (clojure.string/replace header "\n" "\r\n") ":as events" ":as ev") "[\"λ\"\t ev/x]\r\n") 1)
  (refuse {"src/x.cljc" header} (request {"src/x.cljc" header} 0) :unsupported-source)
  ;; Shared path/encoding/resource mechanisms live in splice-envelope-test;
  ;; this one scope-path adapter witness remains specific to rename.
  (refuse {"../outside.clj" header} (request {"../outside.clj" header} 0) :invalid-path))
  (testing "candidate-parse-refuses"
  (clj-surgeon.insert-forms-support/with-file candidate-source
    (fn [dir file _]
      (let [req (assoc (clj-surgeon.rename-alias-test/request {clj-surgeon.rename-alias-test/file candidate-source} 1) :workspace_root (.getCanonicalPath dir))
            result (binding [clj-surgeon.rename-alias-plan/*candidate-text* (constantly "(")] (clj-surgeon.rename-alias/execute! req))]
        (is (= :candidate-parse-error (:error-type result)))
        (is (= candidate-source (slurp file)))))))
)



;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012


;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012


;; @spec RENAME-ALIAS-012
;; INTENT-TEST: RENAME-ALIAS-012

(deftest rename-alias-candidate-integrity
  (testing "candidate-role-recount-refuses"
  (fault-result #'clj-surgeon.rename-alias-plan/*candidate-roles* (constantly [])))
  (testing "candidate-form-preservation-refuses"
  (fault-result #'clj-surgeon.rename-alias-plan/*preservation-tree*
                #(clj-surgeon.insert-forms-plan/tree (str (:source %) " ") :candidate)))
  (testing "candidate-inverse-identity-refuses"
  (fault-result #'clj-surgeon.rename-alias-plan/*inverse-evidence* #(assoc-in % [0 :before] "broken")))
)



;; @spec RENAME-ALIAS-010
;; INTENT-TEST: RENAME-ALIAS-010


;; @spec RENAME-ALIAS-014
;; INTENT-TEST: RENAME-ALIAS-014

(deftest rename-alias-preservation-address
  (testing "rename-alias-receipt-oracle"
  (let [source (str header (clojure.string/join " " (repeat 30 "events/x")))]
    (clj-surgeon.insert-forms-support/with-file source
      (fn [dir target _]
        (let [req (assoc (request {file source} 31) :workspace_root (.getCanonicalPath dir))
              r (clj-surgeon.rename-alias/execute! req)
              detail (clojure.edn/read-string (slurp (:receipt_details_path r)))
              evidence (:write_refusal_evidence r)]
          (is (= :expect-count-mismatch (:error-type r)))
          (is (= source (slurp target)))
          (is (= 30 (:actual_count r) (:available_count evidence)))
          (is (<= (:returned_count evidence) 10))
          (is (= 30 (+ (:returned_count evidence) (:omitted_count evidence))))
          (is (= 30 (count (get-in detail [:receipt :write_refusal_evidence :items]))))
          (is (false? (:authority evidence)))
          (is (false? (:write_authority evidence)))
          (is (< (clj-surgeon.insert-forms-oracle/width (clj-surgeon.rename-alias/receipt-text r)) 4097))))))
  (let [a (str header "; neighbor\n#_events/x\n(def x events/x)\n(def x 2)\n")
        r (clj-surgeon.rename-alias/plan {file a} (request {file a} 1)) detail (first (get-in r [:detail :per_file]))]
    (is (:ok r))
    (is (= 2 (:forms_changed detail)))
    (is (= 2 (count (:changed_forms detail))))
    (is (= 2 (get-in detail [:preservation :other_forms_checked])))
    (is (true? (get-in detail [:preservation :other_forms_unchanged])))
    (doseq [f (get-in detail [:preservation :other_forms])]
      (is (= (:before_sha256 f) (:after_sha256 f))))
    (when-let [b (get-in r [:candidates file])]
      (let [req (request {file a} 1)]
        (is (:ok (clj-surgeon.rename-alias-oracle/verify a b req detail)))
        (doseq [bad [(clojure.string/replace b "; neighbor" "; corrupted")
                     (clojure.string/replace b "#_events/x" "#_ev/x")
                     (clojure.string/replace b "(def x 2)" "(def x 3)")
                     (clojure.string/replace b "(def x ev/x)" "(def x events/x)")]]
          (is (false? (:ok (clj-surgeon.rename-alias-oracle/verify a bad req detail)))))
        (doseq [bad [(assoc detail :result_hash (apply str (repeat 64 "0")))
                     (assoc detail :references_changed 31)
                     (assoc detail :inverse_splices [])
                     (assoc-in detail [:preservation :other_forms] [])]]
          (is (false? (:ok (clj-surgeon.rename-alias-oracle/verify a b req bad)))))))
    (is (not (contains? (:receipt r) :terminal_response)))
    (is (false? (get-in r [:receipt :verification_complete])))
    (is (= 4 (count (remove #(#{:whitespace :newline :comment :comma} (:tag %)) (:entries (clj-surgeon.insert-forms-oracle/inventory a))))))))
  (testing "column-one-reference-addresses"
  ;; Line 1 exercises the traversal directly: an executable file must start with ns.
  (doseq [[source line preorder] [["events/x" 1 0] [(str clj-surgeon.rename-alias-test/header "events/x\n") 2 9]]]
    (let [root (clj-surgeon.rename-alias-plan/source! source clj-surgeon.rename-alias-test/file)
          site (first (clj-surgeon.rename-alias-plan/references root nil "events" clj-surgeon.rename-alias-test/file))]
      (is (= [line 1 line preorder]
             [(:line site) (:col (first (filter #(= (:start site) (:start %)) (:children root))))
              (:end_line site) (get-in site [:address :preorder])]))))
  (let [source (str clj-surgeon.rename-alias-test/header "events/x\n")
        result (clj-surgeon.rename-alias-plan/plan {clj-surgeon.rename-alias-test/file source} (clj-surgeon.rename-alias-test/request {clj-surgeon.rename-alias-test/file source} 0))
        site (first (get-in result [:write_refusal_evidence :items]))]
    (is (= :expect-count-mismatch (:error-type result)))
    (is (= 2 (:line site)))
    (is (= 9 (get-in site [:address :preorder])))))
)
