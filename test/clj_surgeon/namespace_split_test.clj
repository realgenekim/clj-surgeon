(ns clj-surgeon.namespace-split-test
  {:lane :fast}
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-extraction :as kernel]
   [clj-surgeon.mcp-extraction-test :as memory]
   [clj-surgeon.mcp-process :as analyzer-process]
   [clj-surgeon.namespace-split :as split]
   [clj-surgeon.namespace-split-io :as boundary]
   [clj-surgeon.namespace-split-warm :as warm]
   [clj-surgeon.synchronous-verification :as proof]
   [clj-surgeon.verification-process :as process]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(def sources
  (into {} (for [file ["src/app/views.clj" "test/app/caller.clj"]]
             [file (slurp (str "test-fixtures/namespace-split/" file))])))
(def analysis (edn/read-string (slurp "test-fixtures/namespace-split/analysis.edn")))
(def request
  {:workspace_root "/project" :source {:file "src/app/views.clj" :lib "app.views"}
   :destinations [{:lib "app.util" :file "src/app/util.clj" :forms ["helper"] :alias_policy ["u"]}
                  {:lib "app.portal" :file "src/app/portal.clj" :forms ["first-view" "later"] :alias_policy ["portal" "vp"]}
                  {:lib "app.other" :file "src/app/other.clj" :forms ["other"] :alias_policy ["o"]}]
   :promotion_policy "promote-required" :source_retirement "delete"
   :roots ["src" "test"] :verification {:profile "unit"}})
(defn compile-fixture
  ([] (compile-fixture request))
  ([r] (split/compile-split r {:sources sources :analysis analysis :source-paths ["src" "test"]})))

;; @spec NS-SPLIT-001
;; @spec NS-SPLIT-002
;; @spec NS-SPLIT-003
;; @spec NS-SPLIT-004
;; @spec NS-SPLIT-005
;; @spec NS-SPLIT-006
(deftest three-destinations-are-one-final-program
  (let [request (assoc request :destinations [{:lib "app.util" :file "src/app/util.clj" :forms ["a"] :alias_policy ["u"]}
                                              {:lib "app.other" :file "src/app/other.clj" :forms ["b"] :alias_policy ["o"]}])
        r (split/compile-split request {:sources {"src/app/views.clj" "(ns app.views)\n(def a 1) (def b a)\n"}
                                        :analysis {:var-usages [{:filename "src/app/views.clj" :to 'app.views :name 'a
                                                                 :row 2 :col 18 :end-row 2 :end-col 19}]}
                                        :source-paths ["src"]})]
    (is (:ok r))
    (is (str/includes? (get-in r [:future-sources "src/app/other.clj"]) "(def b u/a)")))
  (let [r (compile-fixture) f (:future-sources r)]
    (is (:ok r) (pr-str r))
    (is (nil? (get f "src/app/views.clj")))
    (is (= #{"src/app/views.clj" "src/app/util.clj" "src/app/portal.clj" "src/app/other.clj" "test/app/caller.clj"}
           (set (keys f))))
    (is (= #{["app.portal" "app.util"] ["app.other" "app.util"]}
           (set (filter #(every? #{"app.portal" "app.util" "app.other"} %) (get-in r [:projection :projected_ns_graph :edges])))))
    (is (= ["helper"] (mapv :form (get-in r [:projection :promotions]))))
    (is (str/includes? (get f "src/app/util.clj" "") "(defn helper"))
    (is (str/includes? (get f "src/app/util.clj" "") ";; helper comment travels."))
    (is (str/includes? (get f "src/app/portal.clj" "") "(declare later)"))
    (is (str/includes? (get f "src/app/portal.clj" "") "(u/helper"))
    (is (str/includes? (get f "test/app/caller.clj" "") "[app.portal :as vp]"))
    (is (str/includes? (get f "test/app/caller.clj" "") "(vp/first-view)"))
    (is (str/includes? (get f "test/app/caller.clj" "") "#'o/other"))
    (is (str/includes? (get f "test/app/caller.clj" "") "(defn shadow [helper] (helper 42))"))))

;; @spec NS-SPLIT-007
;; @spec NS-SPLIT-008
(deftest independent-mapping-policy-and-identity-blockers
  (doseq [[r expected]
          [[(update-in request [:destinations 0 :forms] conj "other") :duplicate-owner]
           [(assoc-in request [:destinations 0 :forms] []) :unmapped-owner]
           [(assoc-in request [:destinations 0 :lib] "wrong.lib") :destination-lib-path-mismatch]
           [(assoc request :promotion_policy []) :undecided-promotion]
           [(assoc request :snapshot_hash "stale") :snapshot-drift]
           [(assoc request :constraints {:forbidden_edges [["app.portal" "app.util"]]}) :architecture-violation]]]
    (let [result (compile-fixture r)]
      (is (false? (:ok result)))
      (is (some #{expected} (map :type (:blockers result))) (pr-str result)))))

;; @spec NS-SPLIT-007
(deftest cycles-are-computed-from-all-future-destinations
  (let [r (compile-fixture
            (assoc request :destinations
                   [{:lib "app.a" :file "src/app/a.clj" :forms ["helper" "first-view"] :alias_policy ["a"]}
                    {:lib "app.b" :file "src/app/b.clj" :forms ["later" "other"] :alias_policy ["b"]}]))]
    (is (false? (:ok r)))
    (is (= [["app.a" "app.b"]] (get-in r [:projection :cycle_sccs])))))

;; @spec NS-SPLIT-009
;; @spec NS-SPLIT-010
(deftest whole-file-set-publish-and-guarded-inverse
  (let [compiled (compile-fixture)]
    (let [io (memory/memory-io sources nil)
          failed (kernel/commit! compiled (assoc io :delete-file! (fn [_] (throw (ex-info "delete denied" {})))))]
      (is (false? (:ok failed)))
      (is (= sources @(:state io))))
    (doseq [failure [nil 1 2 3 4]]
      (let [io (memory/memory-io sources failure)
            result (kernel/commit! compiled io)]
        (if failure
          (do (is (false? (:ok result)))
              (is (= sources @(:state io))))
          (do (is (:ok result) (pr-str result))
              (is (not (contains? @(:state io) "src/app/views.clj")))
              (is (false? (:ok (kernel/undo! (assoc (:receipt result) :receipt-hash "tampered") io))))
              (is (not (contains? @(:state io) "src/app/views.clj")))
              (is (:ok (kernel/undo! (:receipt result) io)))
              (is (= sources @(:state io)))))))))

;; @spec NS-SPLIT-011
;; @spec NS-SPLIT-012
(deftest analysis-projects-the-same-compiler-without-source-bodies
  (let [r (compile-fixture)]
    (is (= (:projection r) (split/analysis-projection r)))
    (is (seq (get-in r [:projection :edges])))
    (is (empty? (get-in r [:projection :unknowns])))
    (is (= 4 (get-in r [:projection :counts :forms])))
    (is (= 1 (get-in r [:projection :counts :caller_files])))
    (is (= false (:verification_complete (split/receipt r []))))))

;; @spec NS-SPLIT-003
;; @spec NS-SPLIT-006
(deftest quoted-private-vars-need-no-promotion-or-kondo-site
  (doseq [value ["::portal/value" "#'portal/trim"]]
    (let [s (update sources "src/app/views.clj" str "\n(def external " value ")\n")
          request (update-in request [:destinations 2 :forms] conj "external")
          r (split/compile-split request {:sources s :analysis analysis :source-paths ["src" "test"]})]
      (is (:ok r))
      (is (str/includes? (get-in r [:future-sources "src/app/other.clj"]) "[clojure.string :as portal]"))))
  (let [request (assoc request :destinations [{:lib "app.all" :file "src/app/all.clj"
                                               :forms ["helper" "first-view" "later" "other"] :alias_policy ["a"]}])
        sources (update sources "test/app/caller.clj" str "\n(def private-var #'app.views/helper)\n")
        result (split/compile-split request {:sources sources :analysis analysis :source-paths ["src" "test"]})]
    (is (:ok result))
    (is (empty? (get-in result [:projection :promotions])))
    (is (str/includes? (get-in result [:future-sources "src/app/all.clj"]) "(defn- helper"))
    (is (str/includes? (get-in result [:future-sources "test/app/caller.clj"]) "#'a/helper"))))

;; @spec NS-SPLIT-006
(deftest caller-namespace-trivia-and-metadata-survive
  (let [sources (update sources "test/app/caller.clj" str/replace
                        "(ns app.caller" "(ns ^{:keep true} app.caller \"keep this doc\"")
        result (split/compile-split request {:sources sources :analysis analysis :source-paths ["src" "test"]})]
    (is (:ok result))
    (is (str/includes? (get-in result [:future-sources "test/app/caller.clj"])
          "(ns ^{:keep true} app.caller \"keep this doc\""))))

;; @spec NS-SPLIT-011
(deftest generated-kondo-refs-without-locations-remain-honest
  (let [s (assoc sources "test/app/load_only.clj" "(ns app.load-only (:require [app.views :as old]))\n42\n")
        result (split/compile-split request {:sources s :analysis analysis :source-paths ["src" "test"]})]
    (is (:ok result))
    (is (= 2 (get-in result [:projection :counts :caller_files])))
    (is (str/includes? (get-in result [:future-sources "test/app/load_only.clj"]) "[app.portal :as portal]")))
  (let [s (update sources "test/app/caller.clj" str "\n(def unresolved 'v/missing)\n")
        result (split/compile-split request {:sources s :analysis analysis :source-paths ["src" "test"]})]
    (is (false? (:ok result)))
    (is (= :unresolved-qualified-reference (get-in result [:projection :unknowns 0 :reason]))))
  (doseq [[lib okay?] [['clojure.core true] ['app.views false]]]
    (let [a (update analysis :var-usages conj {:filename "src/app/views.clj" :from 'app.views
                                               :name 'helper :to lib})
          result (split/compile-split request {:sources sources :analysis a :source-paths ["src" "test"]})]
      (is (= okay? (:ok result)))
      (when-not okay? (is (= :unlocated-source-reference (get-in result [:projection :unknowns 0 :reason])))))))

(defn paper-split [source caller]
  (split/compile-split
    (assoc request :destinations [{:lib "app.new" :file "src/app/new.clj" :forms ["x"] :alias_policy ["fresh"]}])
    {:sources (cond-> {"src/app/views.clj" source} caller (assoc "test/app/caller.clj" caller))
     :analysis {} :source-paths ["src" "test"]}))

;; INTENT-TEST: NS-SPLIT-017
(deftest caller-requires-have-no-layout-paper-cuts
  ;; Faithful minimal Cell C server header: comment, three-space indentation,
  ;; retired standalone require, unchanged surrounding lines.
  (doseq [indent ["   " "\t" "      "]
          old-position [0 1 2]]
    (let [entries ["[app.alpha :as a]" "[clojure.string :as str]"]
          lines (vec (concat (take old-position entries) ["[app.views :as views]"] (drop old-position entries)))
          header #(str "(ns app.caller\n  (:require\n" indent ";; keep this exact comment\n"
                       (str/join "\n" (map (partial str indent) %)) "))\n;; untouched\n(def keep 7)\n")
          r (paper-split "(ns app.views)\n(def x 1)\n" (header lines))]
      (is (:ok r))
      (is (= (header (assoc lines old-position "[app.new :as fresh]"))
             (get-in r [:future-sources "test/app/caller.clj"])))))
  (let [r (paper-split "(ns app.views)\n(def x 1)\n"
                       "(ns app.caller (:require [app.views :as views]))\n(def untouched 1)\n")]
    (is (:ok r))
    (is (str/includes? (get-in r [:future-sources "test/app/caller.clj"]) "[app.new :as fresh]"))))

;; NS-SPLIT-019 superseded by NS-SPLIT-022: preserve explicit string values;
;; the monolith's misleading doc token no longer travels to any destination.

;; INTENT-TEST: NS-SPLIT-018
(deftest retired-prose-is-advisory
  ;; Cell C replay_test.clj mentions views/dev-strip without requiring views.
  (let [r (paper-split "(ns app.views)\n(def x 1)\n"
                       "(ns app.caller)\n;; views/x used to draw this\n")]
    (is (= [{:file "test/app/caller.clj" :line 2 :text ";; views/x used to draw this"}]
           (:prose_mentions (split/receipt r [])))))
  (let [caller (str "(ns app.caller (:require [app.views :as views]))\n"
                    ";; views/x was painted here\n"
                    "(def text \"See app.views/x.\n   views/x remains in prose.\")\n"
                    ";; previews/x and app.views-extra/x are unrelated\n")
        r (paper-split "(ns app.views)\n(def x 1)\n" caller)
        mentions (:prose_mentions (split/receipt r []))]
    (is (:ok r))
    (is (empty? (:blockers r)))
    (is (= [";; views/x was painted here" "(def text \"See app.views/x."
            "   views/x remains in prose.\")"] (mapv :text mentions)))
    (is (= ["test/app/caller.clj"] (vec (distinct (map :file mentions)))))
    (doseq [{:keys [file line text]} mentions]
      (is (= text (nth (str/split-lines (get-in r [:future-sources file])) (dec line)))))))

;; INTENT-TEST: NS-SPLIT-016
(deftest baseline-lint-is-relative-and-new-errors-block
  (with-redefs [analyzer-process/run-bounded!
                (constantly {:admission {:status :admitted} :finished? true :exit 3
                             :out "{:analysis {} :summary {:error 108}}"})]
    (is (= :analysis-unavailable
           (try (boundary/analyze! {}) nil
                (catch clojure.lang.ExceptionInfo error (:error-type (ex-data error)))))
        "Missing findings are unknown, never an empty clean baseline"))
  (let [check (ns-resolve 'clj-surgeon.namespace-split-io 'lint-comparison)
        error {:level :error :type :unresolved-symbol :message "Unresolved symbol: old"}
        warning {:level :warning :type :unused-binding :message "Unused binding x"}
        run (fn [findings] {:findings findings :check {:exit 3 :duration_ms 2}})]
    (is (some? check) "The baseline/candidate comparison must exist")
    (when check
      (doseq [[before after delta introduced]
              [[[error warning] [error warning] 0 0]
               [[error] [] -1 0]
               [[error] [error error] 1 1]
               [[error] [(assoc error :message "Unresolved symbol: NEW")] 0 1]
               [[error] [error warning] 0 0]]]
        (let [r (check (run before) (run after))]
          (is (= delta (get-in r [:delta :error])))
          (is (= introduced (:introduced_errors r)))
          (is (= (if (pos? introduced) "failed" "passed") (:status r)))
          (is (= (count (filter #(= :error (:level %)) before)) (get-in r [:baseline :error])))
          (is (str/includes? (:note r) "baseline lint")))))))

;; INTENT-TEST: NS-SPLIT-020
(deftest proof-rows-name-the-command
  (let [check (ns-resolve 'clj-surgeon.namespace-split-io 'proof-check)]
    (is (some? check))
    (when check
      (is (= {:name "kaocha unit" :profile "split-unit" :command ["/work/bin/kaocha" "unit" "--fail-fast"]
              :exit 0 :duration_ms 22197 :status "passed"}
             (check "split-unit" {:command ["/work/bin/kaocha" "unit" "--fail-fast"]
                                  :exit 0 :elapsed_ms 22197 :finished? true}))))))

(defn with-paper-workspace [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "split-paper-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (doseq [[file content] (assoc sources "deps.edn" "{:paths [\"src\" \"test\"]}")]
        (let [p (io/file root file)] (.mkdirs (.getParentFile p)) (spit p content)))
      (f root (assoc request :workspace_root (str root)))
      (finally (doseq [p (reverse (file-seq root))] (.delete p))))))

;; INTENT-TEST: NS-SPLIT-016
(deftest candidate-lint-blocks-before-publication
  (with-paper-workspace
    (fn [root request]
      (let [calls (atom 0) proofs (atom 0)]
        (with-redefs [boundary/analyze! (fn [_]
                                          {:analysis analysis
                                           :findings (if (= 1 (swap! calls inc)) []
                                                         [{:level :error :type :unresolved-symbol :message "new damage"}])
                                           :check {:exit 3 :duration_ms 1}})
                      proof/verification-preflight (constantly nil)
                      proof/run-proof! (fn [& _] (swap! proofs inc) {:ok true})]
          (let [r (boundary/execute! {:verification-profiles {"unit" {:commands [["/bin/true"]]}}
                                      :receipt-dir (str root "/receipts")} request)]
            (is (= "refused" (:state r)))
            (is (= "lint-regression" (:error_type r)))
            (is (false? (:mutation_attempted r)))
            (is (= 0 @proofs))
            (doseq [[file content] sources] (is (= content (slurp (io/file root file)))))
            (is (not (.exists (io/file root "src/app/util.clj"))))))))))

;; INTENT-TEST: NS-SPLIT-021
(deftest early-oracle-failure-restores-without-suite
  (with-paper-workspace
    (fn [root request]
      (let [ran (atom [])]
        (with-redefs [boundary/analyze! (fn [_] {:analysis analysis :findings [] :check {:exit 0 :duration_ms 1}})
                      proof/verification-preflight (constantly nil)
                      process/expand-command (fn [argv _] argv)
                      process/run-process! (fn [_ argv _ _]
                                             (swap! ran conj argv)
                                             {:exit 1 :elapsed_ms 2 :finished? true :output "owner oracle FAIL"})]
          (let [r (boundary/execute! {:verification-profiles {"unit" {:commands [["owner-oracle"] ["kaocha" "unit"]]}}
                                      :receipt-dir (str root "/receipts")} request)]
            (is (= "rolled-back" (:state r)))
            (is (= [["owner-oracle"]] @ran))
            (is (= "owner-oracle" (:name (first (filter :command (:checks r))))))
            (doseq [[file content] sources] (is (= content (slurp (io/file root file)))))
            (is (not (.exists (io/file root "src/app/util.clj"))))))))))

(deftest paper-cut-intents-cannot-evaporate
  (let [registry (:intents (edn/read-string (slurp "docs/intent/helper-extraction/namespace-split-papercuts.edn")))
        known (set (map (comp name :id) registry))
        active (set (map (comp name :id) (filter #(= :active (:status %)) registry)))
        tags (fn [files pattern] (set (mapcat #(map second (re-seq pattern (slurp %))) files)))
        code (tags ["src/clj_surgeon/split_proof_gate.clj" "src/clj_surgeon/namespace_split.clj" "src/clj_surgeon/namespace_split_io.clj" "src/clj_surgeon/namespace_split_warm.clj"
                    "src/clj_surgeon/mcp_tool.clj" "src/clj_surgeon/synchronous_verification.clj"
                    "test/oracles/namespace_split_papercut_oracle.py"
                    "test/oracles/cell_b_oracle.sh" "test/oracles/cell_b_preservation.clj"]
                   #"(?m)^(?:;;|#) INTENT: (NS-SPLIT-[0-9]+)")
        tests (tags ["test/clj_surgeon/split_proof_gate_test.clj" "test/clj_surgeon/split_proof_gate_boundary_test.clj"
                     "test/clj_surgeon/namespace_split_test.clj"
                     "test/clj_surgeon/mcp_namespace_split_test.clj"
                     "test/clj_surgeon/receipt_artifacts_boundary_test.clj"
                     "test/clj_surgeon/admit_patch_test.clj"
                     "test/clj_surgeon/cell_b_oracle_test.clj"
                     "test/clj_surgeon/namespace_split_warm_test.clj"
                     "test/oracles/test_namespace_split_papercut_oracle.py"
                     "test/oracles/test_cell_b_oracle.py"]
                    #"(?m)^(?:;;|#) INTENT-TEST: (NS-SPLIT-[0-9]+)")]
    (is (every? code active))
    (is (every? tests active))
    (is (every? known code))
    (is (every? known tests))))

(defn paper-request [groups]
  (assoc request :destinations
         (mapv (fn [[lib names]] {:lib (str "app." lib) :file (str "src/app/" lib ".clj")
                                  :forms names :alias_policy [lib]}) groups)))

(defn paper-compile [request sources analysis]
  (split/compile-split request {:sources sources :analysis analysis :source-paths ["src" "test"]}))

(defn literal-usage [file source token to name]
  (let [start (str/index-of source token)
        prefix (subs source 0 start)
        row (inc (count (filter #{\newline} prefix)))
        col (- (inc start) (inc (or (str/last-index-of prefix "\n") -1)))]
    {:filename file :row row :col col :end-row row :end-col (+ col (count token))
     :name name :to to}))

;; INTENT-TEST: NS-SPLIT-022
(deftest destination-docstrings-describe-the-destination
  (doseq [form ["(def ^:private x 1)" "(def ^{:private true} x 1)" "(defn x {:private true} [] 1)"]]
    (let [r (paper-split (str "(ns app.views)\n" form "\n") nil)]
      (is (str/includes? (get-in r [:future-sources "src/app/new.clj"]) "no public forms."))))
  (let [r (paper-request [["one" ["a" "b" "c" "d"]] ["two" ["hidden"]]])
        source "(ns app.views \"This file uses form POST.\")\n(def a 1)\n(def b 2)\n(def c 3)\n(def d 4)\n(defn- hidden [] 5)\n"
        result (paper-compile r {"src/app/views.clj" source} {})]
    (is (:ok result))
    (is (str/includes? (get-in result [:future-sources "src/app/one.clj"])
          "\"Split from app.views: 4 forms — a, b, c…\""))
    (is (str/includes? (get-in result [:future-sources "src/app/two.clj"])
          "\"Split from app.views: 1 forms — no public forms.\""))
    (doseq [text (remove nil? (vals (:future-sources result)))]
      (is (not (str/includes? text "This file uses form POST."))))
    (doseq [doc ["Formatting helpers." "First line.\nA \"quote\" and literal \\n."]]
      (let [r (assoc-in r [:destinations 0 :doc] doc)
            result (paper-compile r {"src/app/views.clj" source} {})]
        (is (empty? (boundary/validate-request r)))
        (is (= doc (nth (edn/read-string (get-in result [:future-sources "src/app/one.clj"])) 2)))))
    (is (seq (boundary/validate-request (assoc-in r [:destinations 0 :doc] 42))))))

;; INTENT-TEST: NS-SPLIT-023
(deftest destination-imports-follow-short-class-usage
  ;; Real kondo facts identify the class even when the token is fully qualified.
  (doseq [[short full] [["ZoneId/of" "java.time.ZoneId/of"]
                        ["ZoneId" "java.time.ZoneId"]
                        ["ZoneId." "java.time.ZoneId."]]]
    (let [source (str "(ns app.views (:import (java.time ZoneId Instant)))\n"
                      "(def a " short ")\n(def b " full ")\n")
          facts (for [[name token] [["a" short] ["b" full]]
                      :let [u (literal-usage "src/app/views.clj" source (str "(def " name " " token) nil nil)]]
                  (assoc u :col (+ 7 (:col u)) :class "java.time.ZoneId"))
          result (paper-compile (paper-request [["one" ["a"]] ["two" ["b"]]])
                                {"src/app/views.clj" source} {:java-class-usages facts})]
      (is (:ok result))
      (is (str/includes? (get-in result [:future-sources "src/app/one.clj"])
            "(:import (java.time ZoneId))"))
      (is (not (str/includes? (get-in result [:future-sources "src/app/two.clj"]) ":import")))))
  (let [source "(ns app.views (:import java.time.Instant))\n(defn a [^Instant x] x)\n"
        fact (assoc (literal-usage "src/app/views.clj" source "Instant x" nil nil)
                    :end-col 18 :class "java.time.Instant")
        result (paper-compile (paper-request [["one" ["a"]]]) {"src/app/views.clj" source}
                              {:java-class-usages [fact]})]
    (is (str/includes? (get-in result [:future-sources "src/app/one.clj"])
          "(:import java.time.Instant)"))))

;; INTENT-TEST: NS-SPLIT-024
;; @spec NS-SPLIT-034
;; INTENT-TEST: NS-SPLIT-034
(deftest qualified-call-continuations-track-head-width
  (let [caller (str "(ns app.caller (:require [app.views :as views]))\n"
                    "(def z (views/x (views/x 1\n                         2)\n                3))\n")
        result (paper-compile (paper-request [["l" ["x"]]])
                 {"src/app/views.clj" "(ns app.views)\n(defn x [a b] [a b])\n"
                  "test/app/caller.clj" caller}
                 {:var-usages (for [col [9 18]]
                                {:filename "test/app/caller.clj" :to 'app.views :name 'x
                                 :row 2 :col col :end-row 2 :end-col (+ col 7)})})]
    (is (str/includes? (get-in result [:future-sources "test/app/caller.clj"])
          "(def z (l/x (l/x 1\n                 2)\n            3))")))
  (doseq [caller? [false true]
          anonymous? [false true]
          alias ["longer" "q"]
          aligned? [false true]]
    (let [head (if caller? "views/x" "x")
          old-col (+ 3 (count head) 1 (if anonymous? 1 0))
          indent (if aligned? old-col 4)
          call (str "(defn y []\n  " (when anonymous? "#") "(" head " 1\n" (apply str (repeat indent " ")) "2))\n")
          source (str "(ns app.views)\n(defn x [a b] [a b])\n" (when-not caller? call))
          caller (str "(ns app.caller (:require [app.views :as views]))\n" call)
          file (if caller? "test/app/caller.clj" "src/app/views.clj")
          sources (cond-> {"src/app/views.clj" source} caller? (assoc file caller))
          request (paper-request (cond-> [[alias ["x"]]] (not caller?) (conj ["dest" ["y"]])))
          usage (literal-usage file (get sources file) (str head " 1") 'app.views 'x)
          usage (assoc usage :end-col (+ (:col usage) (count head)))
          result (paper-compile request sources {:var-usages [usage]})
          target (if caller? file "src/app/dest.clj")
          new-head (str alias "/x")
          new-indent (+ indent (- (count new-head) (count head)))]
      (is (:ok result))
      (is (str/includes? (get-in result [:future-sources target])
            (str "(" new-head " 1\n" (apply str (repeat new-indent " ")) "2)")))))
  ;; String contents remain byte-identical; every other owned line moves,
  ;; including calls with the first argument on a later line.
  (doseq [body ["(v/x\n       1\n       2)" "(v/x \"one\n            string\"\n       2)"]]
    (let [source "(ns app.views)\n(defn x [a b] [a b])\n"
          caller (str "(ns app.caller (:require [app.views :as v]))\n(def y " body ")\n")
          result (paper-compile (paper-request [["longer" ["x"]]])
                                {"src/app/views.clj" source "test/app/caller.clj" caller}
                                {:var-usages [(literal-usage "test/app/caller.clj" caller "v/x" 'app.views 'x)]})]
      (is (str/includes? (get-in result [:future-sources "test/app/caller.clj"])
            (-> body
                (str/replace "v/x" "longer/x")
                (str/replace #"\n       (?=\S)" "\n            ")))))))

;; INTENT-TEST: NS-SPLIT-025
(deftest destination-requires-share-source-layout
  (doseq [indent ["   " "\t" "      "]]
    (let [source (str "(ns app.views\n  (:require [z.lib :as z]\n" indent "[a.lib :as a]))\n"
                      "(def x 1)\n(def y [x (z/f) (a/f)])\n")
          result (paper-compile (paper-request [["one" ["x"]] ["two" ["y"]]])
                                {"src/app/views.clj" source}
                                {:var-usages [(let [u (literal-usage "src/app/views.clj" source "x (z/f)" 'app.views 'x)]
                                                (assoc u :end-col (inc (:col u))))]})]
      (is (str/includes? (get-in result [:future-sources "src/app/two.clj"])
            (str "(:require\n" indent "[a.lib :as a]\n" indent
                 "[app.one :as one]\n" indent "[z.lib :as z])")))))
  (let [result (compile-fixture)]
    (is (str/includes? (get-in result [:future-sources "src/app/other.clj"])
          "(:require\n   [app.util :as u]"))))

;; INTENT-TEST: NS-SPLIT-026
(deftest caller-require-groups-remain-in-place
  (doseq [file ["src/app/caller.clj" "test/app/caller.clj"]
          core-only? [false true]]
    (let [caller (str "(ns app.caller\n  (:require [clojure.test :refer [is]]\n"
                      (when-not core-only? "            [clojure.string :as str]\n")
                      (when-not core-only? "            [ring.mock.request :as mock]\n")
                      "            [app.views :as views]))\n")
          result (paper-compile (paper-request [["new" ["x"]]])
                                {"src/app/views.clj" "(ns app.views)\n(def x 1)\n" file caller} {})]
      (is (= (str/replace caller "[app.views :as views]" "[app.new :as new]")
             (get-in result [:future-sources file]))))))

;; INTENT-TEST: NS-SPLIT-027
(deftest unrequired-qualified-refs-are-advisory
  (let [source (str "(ns app.views (:require [app.present :as p]))\n"
                    "(def x [(app.store/now-inst) (app.present/now-inst) (p/now-inst)\n"
                    "        (java.time.ZoneId/of \"UTC\") \"app.false/x\"])\n;; app.comment/x\n")
        result (paper-compile (paper-request [["new" ["x"]]]) {"src/app/views.clj" source}
                              {:java-class-usages [(assoc (literal-usage "src/app/views.clj" source
                                                                         "java.time.ZoneId/of" nil nil)
                                                     :class "java.time.ZoneId")]})
        text (get-in result [:future-sources "src/app/new.clj"])
        rows (:unrequired_qualified_refs (split/receipt result []))]
    (is (:ok result))
    (is (= [{:file "src/app/new.clj" :line 7 :token "app.store/now-inst" :lib "app.store"}] rows))
    (is (not (str/includes? text "[app.store")))
    (doseq [{:keys [line token]} rows]
      (is (str/includes? (nth (str/split-lines text) (dec line)) token)))))

;; @spec NS-SPLIT-028
;; INTENT-TEST: NS-SPLIT-028
;; Oracle examples: d9205abc forms_test.clj:13 and polish_test.clj:3 have
;; test-helpers AFTER views, inside an otherwise grouped project block.
(deftest retired-require-is-replaced-at-its-exact-position
  (doseq [prefix ["forms" "polish"]
          indent ["            " "   "]]
    (let [caller (str "(ns app." prefix "-test\n"
                      "  (:require [clojure.test :refer [is]]\n"
                      indent "[app.store :as store]\n"
                      indent "[app.views :as views]\n"
                      indent "[app.test-helpers :refer [with-temp-store]]))\n")
          file (str "test/app/" prefix "_test.clj")
          result (paper-compile (paper-request [["z" ["y"]] ["a" ["x"]]])
                                {"src/app/views.clj" "(ns app.views)\n(def x 1)\n(def y 2)\n"
                                 file caller} {})]
      (is (:ok result))
      (is (= (str/replace caller "[app.views :as views]"
               (str "[app.a :as a]\n" indent "[app.z :as z]"))
             (get-in result [:future-sources file]))))))

;; @spec NS-SPLIT-032
;; INTENT-TEST: NS-SPLIT-032
;; @spec NS-SPLIT-034
;; INTENT-TEST: NS-SPLIT-034
(deftest sol-nested-continuations-respect-form-ownership
  ;; Sol r4 probe literals on 14c0501f; Fable's 2026-09-08 ruling supersedes
  ;; the r3/r4 expected bytes: all nested layout moves, string contents do not.
  (doseq [[old new body expected]
          [["v" "longer"
            "(v/x 1\n       (do\n       :sentinel))"
            "(longer/x 1\n            (do\n            :sentinel))"]
           ["v" "longer"
            (str "(v/x 1\n"
                 "       (do\n"
                 "       ;; nested comment\n"
                 "       \"alpha\n"
                 "       beta\"\n"
                 "       :sentinel\n"
                 "       )\n"
                 "       :outer)")
            (str "(longer/x 1\n"
                 "            (do\n"
                 "            ;; nested comment\n"
                 "            \"alpha\n"
                 "       beta\"\n"
                 "            :sentinel\n"
                 "            )\n"
                 "            :outer)")]
           ["views" "v"
            (str "(views/x 1\n"
                 "          (do\n"
                 "          ;; nested comment\n"
                 "          \"alpha\n"
                 "          beta\"\n"
                 "          :sentinel\n"
                 "          )\n"
                 "          :outer)")
            (str "(v/x 1\n"
                 "      (do\n"
                 "      ;; nested comment\n"
                 "      \"alpha\n"
                 "          beta\"\n"
                 "      :sentinel\n"
                 "      )\n"
                 "      :outer)")]]]
    (let [head (str old "/x")
          new-head (str new "/x")
          parsed (#'split/parse-file "literal.clj" body)
          actual (#'split/splice body
                   (split/aligned-reference-edits parsed
                     [{:start 1 :end (+ 1 (count head)) :text new-head}]))
          header (str "(ns app.caller (:require [app.views :as " old "]))\n")
          ;; Keep the call at column zero through the full compiler too.
          caller (str header body "\n(identity :outside)\n")
          result (paper-compile (paper-request [[new ["x"]]])
                   {"src/app/views.clj" "(ns app.views)\n(defn x [& args] args)\n"
                    "test/app/caller.clj" caller}
                   {:var-usages [(literal-usage "test/app/caller.clj" caller head 'app.views 'x)]})]
      (is (= expected actual))
      (is (= (edn/read-string (str/replace body head new-head)) (edn/read-string actual)))
      (is (:ok result))
      (is (= (str "(ns app.caller (:require [app." new " :as " new "]))\n"
                  expected "\n(identity :outside)\n")
             (get-in result [:future-sources "test/app/caller.clj"])))))
  ;; Retain the collection boundaries from r4, with a deeper nested body and
  ;; unequal indentation so preserving relative layout is observable.
  (doseq [[open close] [["(do" ")"] ["[" "]"] ["{:key" "}"] ["#{" "}"] ["#(identity" ")"]]
          [head replacement delta] [["v/x" "longer/x" 5] ["views/x" "v/x" -4]]]
    (let [source (str "(" head " 1\n       " open "\n         (do\n           :sentinel)\n       " close "\n        :outer)")
          parsed (#'split/parse-file "literal.clj" source)
          actual (#'split/splice source
                   (split/aligned-reference-edits parsed
                     [{:start 1 :end (inc (count head)) :text replacement}]))
          spaces (fn [width] (apply str (repeat (+ width delta) " ")))]
      (is (= (str "(" replacement " 1\n" (spaces 7) open
                  "\n" (spaces 9) "(do\n" (spaces 11) ":sentinel)\n"
                  (spaces 7) close "\n" (spaces 8) ":outer)")
             actual)))))

;; @spec NS-SPLIT-033
;; INTENT-TEST: NS-SPLIT-033
;; @spec NS-SPLIT-035
;; INTENT-TEST: NS-SPLIT-035
(deftest sol-fixture-requires-replace-in-place
  ;; Exact ns headers from d9205abc, graded by Sol at forms:13 / polish:3.
  (doseq [[prefix caller] (edn/read-string (slurp "test-fixtures/namespace-split/sol-r3-caller-headers.edn"))]
    (let [result (#'split/caller-header (#'split/parse-file "header.clj" caller)
                   "cfp-scheduler-killer.views"
                   '[[cfp-scheduler-killer.views.z :as z]
                     [cfp-scheduler-killer.views.a :as a]])]
      (is (= (str/replace caller "[cfp-scheduler-killer.views :as views]"
               (str "[cfp-scheduler-killer.views.a :as a]\n"
                    "            [cfp-scheduler-killer.views.z :as z]"))
             result)
          prefix))))

;; @spec NS-SPLIT-030
;; INTENT-TEST: NS-SPLIT-030
(deftest warm-proof-mode-is-validated
  (with-paper-workspace
    (fn [root request]
      (doseq [mode [:warm :bogus]]
        (let [r (boundary/execute!
                  {:verification-profiles {"unit" {:proof mode :commands [["/bin/true"]]}}
                   :receipt-dir (str root "/receipts")} request)]
          (is (= "refused" (:state r)))
          (is (= (if (= :warm mode) "warm-probe-unavailable" "invalid-proof-mode") (:error_type r)))
          (is (false? (:mutation_attempted r))))))))

;; @spec NS-SPLIT-029
;; INTENT-TEST: NS-SPLIT-029
(deftest warm-selection-is-bounded
  (is (= {:reload ["app.new" "app.caller" "app.caller-test" "app.new-test"]
          :tests ["app.caller-test" "app.new-test"]}
         (warm/selection ["app.new" "app.caller"]
                         ["app.new-test" "app.caller-test" "unrelated-test"]
                         [["app.caller" "app.new"] ["app.caller-test" "app.caller"]])))
  (is (= {:reload ["app.new" "app.deep-test"] :tests ["app.deep-test"]}
         (warm/selection ["app.new"] ["app.deep-test"]
                         [["app.intermediate" "app.new"] ["app.deep-test" "app.intermediate"]]))))

;; Cell B 92a7ca14: minimal faithful retained dependency + mixed caller + comment trap.
(defn partial-fixture
  ([] (partial-fixture false true))
  ([reverse? comment?]
   (let [source (str "(ns app.views \"keep source doc\")\n;; retained helper\n(defn- helper [] 1)\n"
                     ";; moved prose\n(defn moved [] (helper))\n"
                     "(defn stay [] " (if reverse? "(moved)" ":kept") ")\n"
                     (when comment? "(comment (stay) (let [e 1] (print (moved))))\n"))
         caller "(ns app.caller (:require [app.views :as v]))\n(def answer [(v/moved) (v/stay)])\n"
         uses (concat [(literal-usage "src/app/views.clj" source "helper))" 'app.views 'helper)
                       (literal-usage "test/app/caller.clj" caller "v/moved" 'app.views 'moved)
                       (literal-usage "test/app/caller.clj" caller "v/stay" 'app.views 'stay)]
                      (when (or reverse? comment?)
                        [(literal-usage "src/app/views.clj" source "moved)" 'app.views 'moved)]))
         ;; literal-usage includes suffix only to distinguish the declaration token.
         uses (mapv #(if (= 'helper (:name %)) (update % :end-col - 2)
                         (if (and (= 'moved (:name %)) (= "src/app/views.clj" (:filename %)))
                           (update % :end-col dec) %)) uses)]
     {:request (-> (paper-request [["calendar" ["moved"]]])
                   (dissoc :source_retirement)
                   (assoc :source {:file "src/app/views.clj" :lib "app.views" :retain true
                                   :alias_policy ["v"] :comment_policy "remove-moved-invocations"}
                          :promotion_policy ["helper"]))
      :input {:sources {"src/app/views.clj" source "test/app/caller.clj" caller}
              :source-paths ["src" "test"]
              :analysis {:var-usages uses :var-definitions [{:ns 'app.views :name 'helper :private true}]}}})))

;; @spec NS-SPLIT-037
;; INTENT-TEST: NS-SPLIT-037
(deftest partial-retention-preserves-source-and-undo
  (let [{:keys [request input]} (partial-fixture)
        result (split/compile-split request input)
        src (get-in result [:future-sources "src/app/views.clj"] "")]
    (is (:ok result) (pr-str (:blockers result)))
    (is (str/includes? (or src "") "(ns app.views \"keep source doc\")"))
    (is (str/includes? (or src "") "(defn stay [] :kept)"))
    (is (not (str/includes? (or src "") "(defn moved")))
    (is (= [] (:deleted-files result)))
    (is (= 1 (get-in result [:projection :counts :forms])))
    (when (:ok result)
      (let [io (memory/memory-io (:sources input) nil)
            committed (kernel/commit! result io)]
        (is (:ok committed))
        (is (contains? @(:state io) "src/app/views.clj"))
        (is (:ok (kernel/undo! (:receipt committed) io)))
        (is (= (:sources input) @(:state io)))))))

;; @spec NS-SPLIT-038
;; INTENT-TEST: NS-SPLIT-038
(deftest partial-reference-directions-and-promotions
  (let [{:keys [request input]} (partial-fixture)
        r (split/compile-split request input)
        dst (get-in r [:future-sources "src/app/calendar.clj"] "")
        caller (get-in r [:future-sources "test/app/caller.clj"] "")]
    (is (:ok r) (pr-str (:blockers r)))
    (is (= ["helper"] (mapv :form (get-in r [:projection :promotions]))))
    (is (str/includes? dst "[app.views :as v]"))
    (is (str/includes? dst "(v/helper)"))
    (is (str/includes? caller "[app.views :as v]"))
    (is (str/includes? caller "(calendar/moved) (v/stay)"))
    (is (str/includes? (or (get-in r [:future-sources "src/app/views.clj"]) "") "(defn helper"))
    (is (some #{:undecided-promotion}
              (map :type (:blockers (split/compile-split (assoc request :promotion_policy []) input))))))
  (let [{:keys [request input]} (partial-fixture true false)
        r (split/compile-split request input)]
    (is (some #{:cycle} (map :type (:blockers r)))))
  ;; Retained -> moved alone is valid and requires the destination in the source.
  (let [{:keys [request input]} (partial-fixture true false)
        input (update-in input [:analysis :var-usages] #(filterv (fn [u] (not= 'helper (:name u))) %))
        input (update-in input [:sources "src/app/views.clj"] str/replace "(helper)" "(inc 1)")
        ;; Changed token width moves stay's reference by one column only on its own row: unchanged.
        r (split/compile-split request input)]
    (is (:ok r) (pr-str (:blockers r)))
    (is (str/includes? (or (get-in r [:future-sources "src/app/views.clj"]) "") "(defn stay [] (calendar/moved))"))))

;; @spec NS-SPLIT-039
;; INTENT-TEST: NS-SPLIT-039
(deftest partial-comment-removal-is-narrow
  (let [{:keys [request input]} (partial-fixture)
        r (split/compile-split request input)
        src (or (get-in r [:future-sources "src/app/views.clj"]) "")]
    (is (str/includes? src "(comment (stay) (let [e 1]"))
    (is (not (str/includes? src "(print")))
    (is (= 1 (count (get-in r [:projection :facts :comment_removals]))))
    (is (some #{:cycle}
              (map :type (:blockers (split/compile-split (update request :source dissoc :comment_policy) input)))))))

;; @spec NS-SPLIT-040
;; INTENT-TEST: NS-SPLIT-040
(deftest facts-only-shares-the-plan-without-emission
  (let [{:keys [request input]} (partial-fixture)
        full (split/compile-split request input)
        facts (with-redefs-fn {#'split/aligned-reference-edits (fn [& _] (throw (ex-info "emitter invoked" {})))}
                #(split/compile-split (assoc request :plan_only "facts") input))]
    (is (:ok facts) (pr-str (:blockers facts)))
    (is (= (get-in full [:projection :facts]) (get-in facts [:projection :facts]))))
  (let [{:keys [request input]} (partial-fixture)
        r (split/compile-split (assoc request :plan_only "facts") input)
        facts (get-in r [:projection :facts])]
    (is (nil? (:future-sources r)))
    (is (= ["helper"] (:retained_dependencies facts)))
    (is (= 3 (count (:owners facts))))
    (is (every? #(every? (fn [k] (contains? % k)) [:file :line :col]) (:references facts)))
    (is (= (split/snapshot-hash (:sources input)) (:snapshot_hash facts)))
    (is (not-any? #{:source :text :future-sources :external_requires :entries}
                  (mapcat keys (filter map? (tree-seq coll? seq facts)))))))

;; @spec NS-SPLIT-041
;; INTENT-TEST: NS-SPLIT-041
(deftest partial-and-facts-request-contract
  (let [{:keys [request]} (partial-fixture)]
    (is (empty? (boundary/validate-request request)))
    (is (empty? (boundary/validate-request (assoc request :plan_only "facts"))))
    (is (seq (boundary/validate-request (assoc request :source_retirement "delete"))))
    (is (seq (boundary/validate-request (assoc-in request [:source :retain] "true"))))))

;; @spec NS-SPLIT-044
;; INTENT-TEST: NS-SPLIT-044
(deftest analysis-output-has-a-separate-finite-budget
  (let [called (atom nil)]
    (with-redefs [analyzer-process/run-bounded!
                  (fn [opts] (reset! called opts)
                    {:admission {:status :admitted} :finished? true :exit 0
                     :out "{:analysis {} :findings [] :summary {}}"})]
      (boundary/analyze! {})
      (is (= (* 64 1024 1024) (:visible-byte-limit @called)))
      (is (= (* 16 1024 1024) boundary/max-bytes)))))

;; @spec NS-SPLIT-045
;; INTENT-TEST: NS-SPLIT-045
(deftest partial-headers-retire-only-newly-unused-dependencies
  (let [src "(ns app.views (:import (java.time ZoneId LocalDate)))\n(defn moved [] (ZoneId/of \"UTC\"))\n(defn stay [] LocalDate)\n"
        caller "(ns app.caller (:require [app.views :as v]))\n(def answer (v/moved))\n"
        {:keys [request]} (partial-fixture)
        input {:sources {"src/app/views.clj" src "test/app/caller.clj" caller}
               :source-paths ["src" "test"]
               :analysis {:var-usages [(literal-usage "test/app/caller.clj" caller "v/moved" 'app.views 'moved)]
                          :java-class-usages [(assoc (literal-usage "src/app/views.clj" src "ZoneId/of" nil nil) :class 'java.time.ZoneId)
                                              {:filename "src/app/views.clj" :row 3 :col 17 :end-row 3 :end-col 26 :class 'java.time.LocalDate}]}}
        r (split/compile-split request input)]
    (is (:ok r) (pr-str (:blockers r)))
    (is (not (str/includes? (get-in r [:future-sources "test/app/caller.clj"]) "app.views")))
    (is (str/includes? (get-in r [:future-sources "src/app/views.clj"]) "(java.time LocalDate)"))
    (is (str/includes? (get-in r [:future-sources "src/app/calendar.clj"]) "ZoneId"))
    (is (= ["java.time.ZoneId"] (mapv :class (get-in r [:projection :facts :java_class_references]))))))

;; @spec NS-SPLIT-040
(deftest facts-include-located-external-dependencies-without-libspecs
  (let [{:keys [request input]} (partial-fixture false false)
        source "(ns app.views (:require [clojure.string :as str]))\n(defn- helper [] 1)\n(defn moved [] (str/upper-case \"hello\"))\n(defn stay [] :kept)\n"
        input (-> input (assoc-in [:sources "src/app/views.clj"] source)
                  (assoc-in [:analysis :var-usages]
                            [(literal-usage "src/app/views.clj" source "str/upper-case" 'clojure.string 'upper-case)]))
        r (split/compile-split (assoc request :plan_only "facts") input)]
    (is (= [{:file "src/app/views.clj" :line 3 :col 17 :owner "moved"
             :var "upper-case" :to-lib "clojure.string" :token "str/upper-case"}]
           (get-in r [:projection :facts :external_references])))))

;; @spec NS-SPLIT-037
(deftest partial-retention-refuses-a-moved-declaration-stub
  (let [{:keys [request input]} (partial-fixture false false)
        input (update-in input [:sources "src/app/views.clj"] str "\n(declare moved)\n")
        r (split/compile-split request input)]
    (is (false? (:ok r)))
    (is (some #{:retained-declaration-of-moved-owner} (map :type (:blockers r))))))

(defn negative-fixture []
  (split/compile-split
    (assoc (paper-request [["util" ["moved"]]]) :roots ["src" "test"])
    {:sources {"src/app/views.clj" "(ns app.views)\n(defn moved [x] (+ x 1))\n"
               "test/app/quiet_test.clj" "(ns app.quiet-test)\n(def unrelated 42)\n"}
     :analysis {} :source-paths ["src" "test"]}))

;; @spec NS-SPLIT-060
;; INTENT-TEST: NS-SPLIT-060
(deftest comment-facts-record-policy-and-lines
  (let [{:keys [request input]} (partial-fixture)
        facts (:facts (split/receipt (split/compile-split request input) []))]
    (is (= "remove-moved-invocations" (:comment_policy facts)))
    (is (= 1 (count (:comment_edits facts))))
    (is (= "src/app/views.clj" (get-in facts [:comment_edits 0 :file])))
    (is (str/includes? (get-in facts [:comment_edits 0 :lines 0 2] "") "(print (moved))"))
    (is (not (str/includes? (get-in facts [:comment_edits 0 :lines 0 3] "") "(print"))))
  (let [facts (:facts (split/receipt (negative-fixture) []))]
    (is (= "preserve" (:comment_policy facts)))
    (is (= [] (:comment_edits facts)))))

;; @spec NS-SPLIT-061
;; INTENT-TEST: NS-SPLIT-061
(deftest negative-facts-detect-stale-test-reference
  (let [compiled (negative-fixture)
        good (split/review-facts compiled)
        bad (split/review-facts
              (assoc-in compiled [:future-sources "test/app/quiet_test.clj"]
                        "(ns app.quiet-test (:require [app.views :as old]))\n(def answer (old/moved 1))\n"))]
    (is (= [] (get-in good [:stale_references :sites])))
    (is (= ["src" "test"] (get-in good [:stale_references :scope :roots])))
    (is (pos? (get-in bad [:stale_references :count] 0)))
    (is (some #(= "old/moved" (:token %)) (get-in bad [:stale_references :sites])))
    (is (every? #(= "test/app/quiet_test.clj" (:file %)) (get-in bad [:stale_references :sites])))))

;; @spec NS-SPLIT-062
;; INTENT-TEST: NS-SPLIT-062
(deftest negative-facts-detect-retained-facade
  (let [compiled (negative-fixture)
        bad (assoc-in compiled [:future-sources "src/app/views.clj"]
                      "(ns app.views (:require [app.util :as u]))\n(defn moved [x] (u/moved x))\n")]
    (is (= [] (get-in (split/review-facts compiled) [:facades :forms])))
    (is (= ["moved"] (mapv :owner (get-in (split/review-facts bad) [:facades :forms]))))))

;; @spec NS-SPLIT-063
;; INTENT-TEST: NS-SPLIT-063
(deftest negative-facts-detect-duplicate-owner
  (let [compiled (negative-fixture)
        bad (update-in compiled [:future-sources "test/app/quiet_test.clj"]
                       (fn [_] "(ns app.quiet-test)\n(defn moved [x] (+ x 1))\n"))]
    (is (= [["moved" 1 0]] (get-in (split/review-facts compiled) [:exactly_once :rows])))
    (is (= [["moved" 1 1]] (get-in (split/review-facts bad) [:exactly_once :rows])))
    (is (false? (get-in (split/review-facts bad) [:exactly_once :passed])))))

;; @spec NS-SPLIT-064
;; INTENT-TEST: NS-SPLIT-064
(deftest negative-facts-detect-body-change
  (let [compiled (negative-fixture)
        good (get (split/review-facts compiled) :bodies_preserved)
        bad (get (split/review-facts
                   (update-in compiled [:future-sources "src/app/util.clj"] str/replace "(+ x 1)" "(+ x 2)"))
                 :bodies_preserved)]
    (is (= [1 0] ((juxt :equal :unequal) good)))
    (is (= [0 1] ((juxt :equal :unequal) bad)))
    (is (= [1 0] ((juxt :raw_equal :raw_unequal) good)))
    (is (= "moved" (ffirst (:rows good))))
    (is (= 64 (count (second (first (:rows good))))))
    (is (not= (second (first (:rows bad))) (nth (first (:rows bad)) 2 nil)))))

;; @spec NS-SPLIT-061
;; @spec NS-SPLIT-063
(deftest negative-facts-scan-unchanged-files-and-distinguish-namesakes
  (let [compiled (negative-fixture)
        old "(ns app.quiet-test)\n(defn moved [x] :independent)\n"
        compiled (assoc-in compiled [:guard-sources "test/app/quiet_test.clj"] old)
        good (split/review-facts compiled)
        duplicate (assoc-in compiled [:future-sources "test/app/quiet_test.clj"]
                            (str old "(defn moved [x] :independent)\n"))]
    (is (= [["moved" 1 0]] (get-in good [:exactly_once :rows])))
    (is (= 1 (count (get-in good [:exactly_once :preexisting_namesakes]))))
    (is (= [["moved" 1 1]] (get-in (split/review-facts duplicate) [:exactly_once :rows]))))
  (doseq [text ["(ns app.quiet-test)\n(def answer (app.views/moved 1))"
                "(ns app.quiet-test (:require [app.views :refer [moved]]))\n(def answer (moved 1))"]]
    (let [compiled (assoc-in (negative-fixture) [:guard-sources "test/app/quiet_test.clj"] text)]
      (is (pos? (get-in (split/review-facts compiled) [:stale_references :count] 0))))))

;; @spec NS-SPLIT-060
;; @spec NS-SPLIT-057
(deftest negative-comment-evidence-is-lossless-and-safe
  (let [compiled (negative-fixture)
        before "(ns app.quiet-test)\n;;   quote \"x\" \\ newline\u2028forge\u202E\n(def unrelated 42)\n"
        after (str/replace before ";;   quote" ";;    quote")
        compiled (-> compiled
                     (assoc-in [:guard-sources "test/app/quiet_test.clj"] before)
                     (assoc-in [:future-sources "test/app/quiet_test.clj"] after))
        facts (split/review-facts compiled)
        row (get-in facts [:comment_edits 0 :lines 0])
        decode #(json/parse-string (str "\"" % "\""))]
    (is (= ";;   quote \"x\" \\ newline\u2028forge\u202E" (decode (nth row 2))))
    (is (= ";;    quote \"x\" \\ newline\u2028forge\u202E" (decode (nth row 3))))
    (is (not (re-find #"[\u2028\u202E]" (pr-str facts))))))

;; @spec NS-SPLIT-064
(deftest body-facts-separate-raw-bytes-from-authorized-replay
  (let [{:keys [request input]} (partial-fixture)
        compiled (split/compile-split request input)
        good (:bodies_preserved (split/review-facts compiled))]
    (is (= [1 0 0 1] ((juxt :equal :unequal :raw_equal :raw_unequal) good)))
    (is (= :same (nth (first (:rows good)) 2)))
    (is (= 64 (count (nth (first (:rows good)) 3 ""))))
    (is (not= (second (first (:rows good))) (nth (first (:rows good)) 3 nil)))
    (doseq [[from to] [["(v/helper)" "(v/helper )"] ["(v/helper)" "(inc (v/helper))"]]]
      (is (= 1 (get-in (split/review-facts
                         (update-in compiled [:future-sources "src/app/calendar.clj"] str/replace from to))
                       [:bodies_preserved :unequal]))))))

;; @spec NS-SPLIT-060
;; @spec NS-SPLIT-063
(deftest sol-namesake-identity-does-not-hide-comments-or-invent-duplicates
  ;; Sol batch-4 fence: source order views/v becomes destination util/v.
  (let [source "(ns app.views)\n;; comment A\n(defn moved [x] (+ x 1))\n"
        namesake "(ns app.v)\n;; comment B\n(defn moved [x] :independent)\n"
        compiled (split/compile-split
                   (assoc (paper-request [["util" ["moved"]]]) :roots ["src" "test"])
                   {:sources {"src/app/views.clj" source "src/app/v.clj" namesake}
                    :analysis {} :source-paths ["src" "test"]})
        swapped (-> compiled
                    (update-in [:future-sources "src/app/util.clj"] str/replace ";; comment A" ";; comment B")
                    (assoc-in [:future-sources "src/app/v.clj"] (str/replace namesake ";; comment B" ";; comment A")))]
    (is (= [] (:comment_edits (split/review-facts compiled))))
    (is (= 2 (reduce + (map (comp count :lines) (:comment_edits (split/review-facts swapped)))))))
  (let [caller "(ns app.quiet-test (:require [app.views :as old]))\n(defn moved [x] (old/moved x))\n"
        compiled (split/compile-split
                   (assoc (paper-request [["util" ["moved"]]]) :roots ["src" "test"])
                   {:sources {"src/app/views.clj" "(ns app.views)\n(defn moved [x] (+ x 1))\n"
                              "test/app/quiet_test.clj" caller}
                    :analysis {:var-usages [(literal-usage "test/app/quiet_test.clj" caller "old/moved" 'app.views 'moved)]}
                    :source-paths ["src" "test"]})]
    (is (str/includes? (get-in compiled [:future-sources "test/app/quiet_test.clj"]) "util/moved"))
    (is (= [["moved" 1 0]] (get-in (split/review-facts compiled) [:exactly_once :rows])))))

;; @spec NS-SPLIT-062
(deftest sol-facade-alias-and-arity-matrix
  (doseq [body ["(defn legacy ([x] (u/moved x)))"
                "(defn legacy ([x] (u/moved x)) ([x y] (u/moved x y)))"
                "(def legacy #'u/moved)" "(def legacy (partial u/moved 1))"
                "(def legacy (fn [x] (u/moved x)))"]]
    (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
                     (str "(ns app.views (:require [app.util :as u]))\n" body "\n"))]
      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :forms]))) body))))

;; @spec NS-SPLIT-062
(deftest retention-policy-reports-existing-forwarders
  (let [source "(ns app.views)\n(defn moved [x] (+ x 1))\n(defn stay [x] (moved x))\n"
        request (-> (paper-request [["util" ["moved"]]])
                    (dissoc :source_retirement)
                    (assoc :source {:file "src/app/views.clj" :lib "app.views" :retain true}))
        usage (literal-usage "src/app/views.clj" source "(moved x)" 'app.views 'moved)
        usage (-> usage (update :col inc) (update :end-col - 3))
        compiled (split/compile-split request {:sources {"src/app/views.clj" source}
                                               :analysis {:var-usages [usage]} :source-paths ["src" "test"]})
        fact (:facades (split/review-facts compiled))]
    (is (:ok compiled) (pr-str (:blockers compiled)))
    (is (= ["stay"] (mapv :owner (:forms fact))))
    (is (= ["stay"] (mapv :owner (:expected fact))))
    (is (= [] (:unexpected fact)))))

;; @spec NS-SPLIT-062
(deftest sol-facade-expectations-bind-arity-and-target
  (let [compiled (assoc-in (negative-fixture) [:future-sources "src/app/views.clj"]
                   "(ns app.views (:require [app.util :as u]))\n(defn legacy ([] :other) ([x] (u/moved x)))\n")]
    (is (= ["legacy"] (mapv :owner (get-in (split/review-facts compiled) [:facades :unexpected])))))
  (let [source "(ns app.views)\n(defn moved [x] x)\n(defn other [x] (inc x))\n(defn legacy [x] (app.views/moved x))\n"
        request (-> (paper-request [["util" ["moved" "other"]]])
                    (dissoc :source_retirement)
                    (assoc :source {:file "src/app/views.clj" :lib "app.views" :retain true}))
        compiled (split/compile-split request
                   {:sources {"src/app/views.clj" source} :source-paths ["src" "test"]
                    :analysis {:var-usages [(literal-usage "src/app/views.clj" source "app.views/moved" 'app.views 'moved)]}})
        changed (update-in compiled [:future-sources "src/app/views.clj"] str/replace "util/moved" "util/other")]
    (is (= [] (get-in (split/review-facts compiled) [:facades :unexpected])))
    (is (= ["legacy"] (mapv :owner (get-in (split/review-facts changed) [:facades :unexpected]))))
    (let [changed-arity (update-in compiled [:future-sources "src/app/views.clj"]
                                   str/replace "[x] (util/moved x)" "[x y] (util/moved x y)")]
      (is (= ["legacy"] (mapv :owner (get-in (split/review-facts changed-arity) [:facades :unexpected])))))))
