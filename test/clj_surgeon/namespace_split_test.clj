(ns clj-surgeon.namespace-split-test
  {:lane :fast}
  (:require [clj-surgeon.namespace-split :as split]
            [clj-surgeon.mcp-extraction :as kernel]
            [clj-surgeon.mcp-extraction-test :as memory]
            [clojure.edn :as edn]
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
