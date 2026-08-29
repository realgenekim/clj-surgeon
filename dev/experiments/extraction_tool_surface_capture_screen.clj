(ns extraction-tool-surface-capture-screen
  "Full-catalog capture server and fail-closed offline extraction surface scorer."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.experiments.mcp-candidate-admission :as admission]
   [clj-surgeon.extract :as extract]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-extraction :as extraction]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-workspace-sources :as workspace-sources]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is run-tests]]
   [extraction-call-capture-server :as capture-server]
   [extraction-tool-surface :as surface]
   [owner-aware-mcp-surface-observer :as observer])
  (:import
   (java.math BigInteger)
   (java.nio.file Files)
   (java.security MessageDigest)))

(def expected-order
  [{:run-id "01-control" :position 1 :arm :control}
   {:run-id "02-treatment" :position 2 :arm :treatment}
   {:run-id "03-treatment" :position 3 :arm :treatment}
   {:run-id "04-control" :position 4 :arm :control}])

(def expected-logical-arguments-sha256
  "01d502300c9e6af22e22e69f5680a4ed767ecc7fa64e4c9bce1d91b78bdfba47")

(def expected-future-hashes
  ["6ed498052c8a30531047b1d1c9bd23c609bc32355403e8412b7cfda178a5f822"
   "bdaf9cdc5b748b22563c575d8a8278c3634ef8b44d2b187f4e23374ca9e9c0f1"])

(def expected-forms
  ["date-fmt" "datetime-fmt" "->local-date" "fmt-date" "fmt-date-range"
   "fmt-instant" "->instant" "when-fmt" "relative-when" "fmt-when"
   "fmt-cfp-window" "iso-date-fmt" "fmt-close-date" "cfp-public-url"
   "not-blank"])

(def expected-final-text
  "Captured.")

(def production-instructions mcp-server/server-instructions)

(def expected-target-surface-sha256
  {:control "9fd5471d4bc47eb9bf37b4cd0eefca352238fff7319872258d4f38d0a3a465e0"
   :treatment "e29ecae49417c0bb27575763c92a41460b7e2fa8de5178a151d5a5ba7b326180"})

(def expected-client-target-surface-sha256
  {:control "91d7b6c61b16ead67f3f3cae47eb1a8ba53d266b480b19f0697f50a50dc8a508"
   :treatment "d602120b9e3b96d8c136551f5516800674adddbbd06e33a4a94326a35314ffef"})

(def expected-client-peer-surface-sha256
  "7c71c9881902ef527d18562e0d0f94c897728bcf2c3d7d15b2cc07aacf4059c4")

(defn- sha256-bytes [^bytes bytes]
  (let [digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes))]
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn file-sha256 [path]
  (sha256-bytes (Files/readAllBytes (.toPath (io/file path)))))

(defn- source-sha256 [source]
  (sha256-bytes (.getBytes source "UTF-8")))

(defn- read-json [path]
  (json/parse-string (slurp (io/file path)) true))

(defn- read-json-lines [path]
  (with-open [reader (io/reader path)]
    (mapv #(json/parse-string % true) (remove empty? (line-seq reader)))))

(defn- json-roundtrip [value]
  (json/parse-string (json/generate-string value) true))

(defn- public-tool [tool]
  (-> (select-keys tool [:id :name :description :schema :output-schema
                         :annotations :structured?])
      (update :id name)))

(defn route-evidence [events]
  (let [started (->> events (filter #(= "item.started" (:type %)))
                     (mapv :item))
        external (filterv #(contains? #{"mcp_tool_call" "command_execution"
                                        "file_change" "agent_message"}
                                      (:type %)) started)
        mcps (filterv #(= "mcp_tool_call" (:type %)) started)
        commands (filterv #(= "command_execution" (:type %)) started)
        files (filterv #(= "file_change" (:type %)) started)
        completed (->> events
                       (filter #(and (= "item.completed" (:type %))
                                     (= "mcp_tool_call" (get-in % [:item :type]))))
                       (mapv :item))
        messages (->> events
                      (filter #(and (= "item.completed" (:type %))
                                    (= "agent_message" (get-in % [:item :type]))))
                      (mapv #(get-in % [:item :text])))
        first-action (first external)
        structured (or (get-in (first completed) [:result :structured_content])
                       (get-in (first completed) [:result :structuredContent]))
        ok? (and (= {:type "mcp_tool_call" :server "clj-surgeon"
                     :tool "apply_clojure_changes"}
                    (select-keys first-action [:type :server :tool]))
                 (= 1 (count mcps)) (empty? commands) (empty? files)
                 (= [expected-final-text] messages)
                 (true? (:captured structured))
                 (true? (:source_unchanged structured)))]
    {:ok ok? :first-external-action (select-keys first-action [:type :server :tool])
     :mcp-calls (count mcps) :shell-calls (count commands)
     :file-change-actions (count files) :final-messages messages
     :capture-result (select-keys structured
                                  [:captured :source_unchanged :call_count])}))

(defn compiler-evidence [repo-root arguments]
  (let [fixture (.toPath (io/file repo-root
                                  "bench/fixtures/edit_portfolio/sessionize-format-extraction/before"))
        validated (contract/validate-tool-params arguments)]
    (if-not (:ok validated)
      {:ok false :validation validated}
      (let [sources (workspace-sources/read-all fixture)
            request (get-in validated [:params :extraction])
            source-path (.getCanonicalPath (io/file (str fixture) (:file request)))
            target-path (.getCanonicalPath (io/file (str fixture) (:to request)))
            compiled
            (extraction/compile-extraction
              (assoc request :file source-path :to target-path
                     :source (get sources source-path)
                     :target-ns (extract/file-path->ns-name
                                  target-path ["src" "test" "dev"])
                     :workspace-sources sources))
            hashes (when (:ok compiled)
                     (->> (:future-sources compiled) (sort-by key)
                          (mapv (comp source-sha256 val))))]
        {:ok (and (:ok compiled) (= 15 (:form-count compiled))
                  (= 2 (count (:future-sources compiled)))
                  (= expected-future-hashes hashes))
         :form-count (:form-count compiled)
         :file-count (count (:future-sources compiled))
         :future-hashes hashes :error-type (:error-type compiled)}))))

(defn- validate-client-catalog [advertised registry expected-server]
  (let [advertised-tools (:tools advertised)
        client-tools (:tool-projection registry)
        client-peer-tools (filterv #(not= "apply_clojure_changes" (:name %))
                                   client-tools)
        peer-sha (surface/sha256 client-peer-tools)
        catalog
        (owner-aware-mcp-surface-observer/validate-catalog
          advertised-tools registry expected-server)]
    (assoc catalog
           :ok (and (:ok catalog)
                    (= expected-client-peer-surface-sha256 peer-sha))
           :client-peer-surface-sha256 peer-sha)))

(defn- transition-ms [timing phase]
  (some #(when (= phase (:phase %)) (:observer-elapsed-ms %))
        (:transitions timing)))

(defn surface-evidence [advertised-path registry-path arm expected-server]
  (let [advertised (read-json advertised-path)
        registry (read-json registry-path)
        expected-tools (json-roundtrip
                         (mapv public-tool
                               (capture-server/capture-tools arm "/dev/null")))
        client-target (first (filter #(= "apply_clojure_changes" (:name %))
                                     (:tool-projection registry)))
        client-target-sha (some-> client-target surface/sha256)]
    {:ok (and (= (name arm) (:arm advertised))
              (= production-instructions (:instructions advertised))
              (= expected-tools (:tools advertised))
              (:ok (validate-client-catalog advertised registry expected-server))
              (= (expected-client-target-surface-sha256 arm)
                 client-target-sha))
     :server-surface-exact (= expected-tools (:tools advertised))
     :client (validate-client-catalog advertised registry expected-server)
     :client-target-surface-sha256 client-target-sha
     :advertised-catalog-sha256 (surface/sha256 (:tools advertised))
     :client-catalog-sha256 (surface/sha256 (:tool-projection registry))}))

(defn score-run
  [{:keys [repo-root run-id position arm advertised registry capture events timing
           logical-arguments workspace-manifest expected-server workspace
           codex-home server-pid codex-status]}]
  (let [advertised-value (read-json advertised)
        registry-value (read-json registry)
        capture-value (read-json capture)
        events-value (read-json-lines events)
        timing-value (edn/read-string (slurp timing))
        manifest-value (read-json workspace-manifest)
        arguments (get-in capture-value [:calls 0 :params])
        surface-result (surface-evidence advertised registry arm expected-server)
        surface-ok (:ok surface-result)
        client (:client surface-result)
        public (admission/authorize (:schema (surface/tool-surface arm)) arguments)
        route (route-evidence events-value)
        compiler (compiler-evidence repo-root arguments)
        logical-sha (file-sha256 logical-arguments)
        requested-workspace (some-> (:workspace_root arguments) io/file .getCanonicalPath)
        isolated-workspace (some-> workspace io/file .getCanonicalPath)
        workspace-root-exact? (= isolated-workspace requested-workspace)
        workspace-clean? (= {:before [] :after []} manifest-value)
        clocks {:pre-first-call-ms
                (transition-ms timing-value :initial-tool-call-materialization)
                :complete-wall-ms (:process-wall-ms timing-value)}
        target-surface-sha (surface/sha256 (surface/tool-surface arm))
        codex-exit (some-> codex-status parse-long)
        correct? (and (zero? (or codex-exit -1))
                      surface-ok (:ok client) (:ok public)
                      (= (expected-target-surface-sha256 arm) target-surface-sha)
                      (= 1 (count (:calls capture-value)))
                      (= "apply_clojure_changes"
                         (get-in capture-value [:calls 0 :tool_name]))
                      (:ok route) (:ok compiler)
                      (= expected-logical-arguments-sha256 logical-sha)
                      workspace-root-exact? workspace-clean?
                      (every? number? (vals clocks)))]
    {:schema :clj-surgeon.extraction-tool-surface-capture-run/v1
     :run-id run-id :position position :arm arm :correct correct?
     :codex-exit-status codex-exit
     :isolation {:workspace workspace :codex-home codex-home
                 :server-pid (some-> server-pid parse-long)}
     :identity {:target-surface-sha256 target-surface-sha
                :client-target-surface-sha256
                (:client-target-surface-sha256 surface-result)
                :advertised-catalog-sha256 (surface/sha256 (:tools advertised-value))
                :client-catalog-sha256
                (surface/sha256 (:tool-projection registry-value))
                :logical-arguments-sha256 logical-sha}
     :surface {:exact surface-ok :client client :public-admission public}
     :route route :compiler compiler
     :workspace-root-exact workspace-root-exact?
     :workspace-clean workspace-clean?
     :clocks clocks}))

(defn- midpoint [values]
  (/ (reduce + (map double values)) (double (count values))))

(defn cohort-report [runs]
  (let [runs (vec runs)
        identity (mapv #(select-keys % [:run-id :position :arm]) runs)
        exact-order? (= expected-order identity)
        complete? (= 4 (count runs))
        correct? (and complete? (every? :correct runs))
        stable-client-catalogs?
        (and complete?
             (every? (fn [arm]
                       (= 1 (count (distinct
                                     (map #(get-in % [:identity
                                                      :client-catalog-sha256])
                                          (filter (comp #{arm} :arm) runs))))))
                     [:control :treatment]))
        isolated? (and complete?
                       (= 4 (count (distinct (map #(get-in % [:isolation :workspace]) runs))))
                       (= 4 (count (distinct (map #(get-in % [:isolation :codex-home]) runs))))
                       (= 4 (count (distinct (map #(get-in % [:isolation :server-pid]) runs)))))
        controls (filterv #(= :control (:arm %)) runs)
        treatments (filterv #(= :treatment (:arm %)) runs)
        midpoint-for (fn [rows field]
                       (let [values (mapv #(get-in % [:clocks field]) rows)]
                         (when (and (= 2 (count rows))
                                    (every? number? values))
                           (midpoint values))))
        c-pre (midpoint-for controls :pre-first-call-ms)
        t-pre (midpoint-for treatments :pre-first-call-ms)
        c-wall (midpoint-for controls :complete-wall-ms)
        t-wall (midpoint-for treatments :complete-wall-ms)
        reduction #(when (and %1 %2 (pos? %1)) (* 100.0 (/ (- %1 %2) %1)))
        pre-reduction (reduction c-pre t-pre)
        wall-reduction (reduction c-wall t-wall)
        paired-wins? (and complete?
                          (every? number?
                                  [(get-in runs [0 :clocks :pre-first-call-ms])
                                   (get-in runs [1 :clocks :pre-first-call-ms])
                                   (get-in runs [2 :clocks :pre-first-call-ms])
                                   (get-in runs [3 :clocks :pre-first-call-ms])])
                          (< (get-in runs [1 :clocks :pre-first-call-ms])
                             (get-in runs [0 :clocks :pre-first-call-ms]))
                          (< (get-in runs [2 :clocks :pre-first-call-ms])
                             (get-in runs [3 :clocks :pre-first-call-ms])))
        promoted? (and exact-order? correct? isolated? stable-client-catalogs?
                       paired-wins?
                       (>= (or pre-reduction -1) 20.0)
                       (>= (or wall-reduction -1) 10.0))]
    {:schema :clj-surgeon.extraction-tool-surface-capture-cohort/v1
     :ok promoted? :complete complete? :exact-order exact-order?
     :all-correct correct? :isolated-runs isolated?
     :stable-client-catalogs stable-client-catalogs?
     :treatment-wins-both-pairs paired-wins?
     :pre-first-call {:control-midpoint-ms c-pre :treatment-midpoint-ms t-pre
                      :reduction-percent pre-reduction :gate 20.0}
     :complete-wall {:control-midpoint-ms c-wall :treatment-midpoint-ms t-wall
                     :reduction-percent wall-reduction :gate 10.0}}))

(defn- synthetic-events [prefix]
  (vec (concat prefix
               [{:type "item.started"
                 :item {:id "m" :type "mcp_tool_call" :server "clj-surgeon"
                        :tool "apply_clojure_changes"}}
                {:type "item.completed"
                 :item {:id "m" :type "mcp_tool_call"
                        :result {:structured_content
                                 {:captured true :source_unchanged true}}}}
                {:type "item.completed"
                 :item {:id "a" :type "agent_message"
                        :text expected-final-text}}])))

(defn- synthetic-run [expected pre wall]
  (merge expected {:correct true
                   :identity {:client-catalog-sha256
                              (str "catalog-" (name (:arm expected)))}
                   :isolation {:workspace (str "/w/" (:run-id expected))
                               :codex-home (str "/h/" (:run-id expected))
                               :server-pid (:position expected)}
                   :clocks {:pre-first-call-ms pre :complete-wall-ms wall}}))

(deftest full-catalog-treatment-is-one-variable
  (let [control (capture-server/capture-tools :control "/tmp/control")
        treatment (capture-server/capture-tools :treatment "/tmp/treatment")
        changed (keep-indexed #(when-not (= (dissoc %2 :tool-fn)
                                            (dissoc (nth treatment %1) :tool-fn))
                                 %1)
                              control)]
    (is (= 1 (count changed)))
    (is (= :clj-change (:id (nth control (first changed)))))
    (is (= (mapv :id control) (mapv :id treatment)))
    (is (= production-instructions mcp-server/server-instructions))
    ;; The report uses descriptive keys; this explicit pair prevents arm swaps.
    (is (= {:control-sha256 (expected-target-surface-sha256 :control)
            :treatment-sha256 (expected-target-surface-sha256 :treatment)}
           (select-keys (surface/surface-report)
                        [:control-sha256 :treatment-sha256])))))

(deftest client-comparison-is-name-joined-and-server-surface-is-json-normalized
  (let [advertised-tools
        (json-roundtrip
          (mapv public-tool
                (capture-server/capture-tools :control "/tmp/control")))
        client-tools
        (->> advertised-tools
             (map #(-> % (assoc :input-schema (:schema %))
                       (dissoc :schema :id :structured?)))
             (sort-by :name)
             vec)
        advertised {:tools advertised-tools}
        peer-sha (surface/sha256
                   (filterv #(not= "apply_clojure_changes" (:name %))
                            client-tools))
        registry {:schema "clj-surgeon.codex-mcp-registry.v1"
                  :ok true
                  :observation-source
                  (assoc observer/registry-observation-source
                         :server-selector {:field "name" :value "clj-surgeon"})
                  :server "clj-surgeon"
                  :tool-names (mapv :name client-tools)
                  :tool-projection client-tools}]
    (is (not= (mapv :name advertised-tools) (mapv :name client-tools)))
    (with-redefs [expected-client-peer-surface-sha256 peer-sha]
      (is (:ok (validate-client-catalog advertised registry "clj-surgeon")))
      (is (false?
            (:ok (validate-client-catalog
                   advertised
                   (assoc registry :tool-projection
                          (vec (concat (rest client-tools)
                                       [(first client-tools)])))
                   "clj-surgeon"))))
      (is (false?
            (:ok (validate-client-catalog
                   advertised
                   (update registry :tool-projection conj (first client-tools))
                   "clj-surgeon")))))))

(deftest first-action-and-no-effect-falsifiers
  (is (:ok (route-evidence (synthetic-events []))))
  (doseq [type ["command_execution" "agent_message" "file_change"]]
    (is (false? (:ok (route-evidence
                       (synthetic-events [{:type "item.started"
                                           :item {:id "bad" :type type}}]))))))
  (is (false? (:ok (route-evidence
                     (conj (synthetic-events [])
                           {:type "item.started"
                            :item {:id "m2" :type "mcp_tool_call"
                                   :server "clj-surgeon"
                                   :tool "apply_clojure_changes"}}))))))

(deftest cohort-gates-order-completeness-and-both-clocks
  (let [passing (mapv synthetic-run expected-order
                      [100.0 70.0 75.0 110.0]
                      [200.0 150.0 155.0 210.0])]
    (is (:ok (cohort-report passing)))
    (is (false? (:ok (cohort-report (pop passing)))))
    (is (false? (:ok (cohort-report (assoc passing 0 (nth passing 1)
                                           1 (nth passing 0))))))
    (is (false? (:ok (cohort-report (assoc-in passing [1 :correct] false)))))
    (is (false? (:ok (cohort-report
                       (assoc-in passing [1 :isolation :workspace]
                                 (get-in passing [0 :isolation :workspace]))))))
    (is (false? (:ok (cohort-report
                       (assoc-in passing [1 :identity :client-catalog-sha256]
                                 "drift")))))
    (is (false? (:ok (cohort-report
                       (assoc-in passing [1 :clocks :pre-first-call-ms] 99.0)))))
    (is (false? (:ok (cohort-report
                       (-> passing
                           (assoc-in [1 :clocks :complete-wall-ms] 200.0)
                           (assoc-in [2 :clocks :complete-wall-ms] 200.0))))))))

(deftest frozen-fixture-compiles-to-canonical-future
  (let [root (.getCanonicalPath (io/file "."))
        request {:workspace_root root
                 :extraction {:file "src/cfp_scheduler_killer/views.clj"
                              :to "src/cfp_scheduler_killer/views/format.clj"
                              :forms expected-forms :require_policy "minimal"
                              :public_forms ["not-blank"] :caller_changes []
                              :ignored_caller_files []}
                 :verify "exact"}]
    (is (:ok (compiler-evidence root request)))))

(defn- parse-args [args]
  (when (odd? (count args))
    (throw (ex-info "Expected --key value pairs" {:args args})))
  (into {} (map (fn [[k v]] [(keyword (subs k 2)) v]) (partition 2 args))))

(defn -main [& args]
  (case (first args)
    "self-test"
    (let [{:keys [fail error]} (run-tests 'extraction-tool-surface-capture-screen)]
      (when (pos? (+ fail error)) (System/exit 1)))

    "score-run"
    (let [opts (parse-args (rest args))
          result (score-run (-> opts (update :position parse-long)
                                (update :arm keyword)))]
      (println (pr-str result))
      (when-not (:correct result) (System/exit 1)))

    "surface-preflight"
    (let [opts (parse-args (rest args))
          result (surface-evidence (:advertised opts) (:registry opts)
                                   (keyword (:arm opts))
                                   (:expected-server opts))]
      (println (pr-str result))
      (when-not (:ok result) (System/exit 1)))

    "cohort-report"
    (let [opts (parse-args (rest args))
          runs (mapv (comp edn/read-string slurp io/file)
                     (str/split (:runs opts) #","))
          result (cohort-report runs)]
      (println (pr-str result))
      (when-not (:ok result) (System/exit 1)))

    (throw (ex-info "Expected self-test, surface-preflight, score-run, or cohort-report"
                    {:args args}))))
