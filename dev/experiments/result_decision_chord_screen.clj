(ns result-decision-chord-screen
  "Pure scorer for the post-inspect decision-chord capture experiment."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-compact-location :as compact-location]
   [clj-surgeon.mcp-contract :as contract]
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(def source-file "src/sample/core.clj")

(def source-before
  (str "(ns sample.core)\n\n"
       "(defn greet [name]\n"
       "  (str \"Hello, \" name))\n"))

(def source-after
  (str "(ns sample.core)\n\n"
       "(defn greet [name]\n"
       "  (str \"Welcome, \" name))\n"))

(def expected-order
  [{:position 1 :arm :control}
   {:position 2 :arm :treatment}
   {:position 3 :arm :treatment}
   {:position 4 :arm :control}])

(def expected-inspect-request
  {:requests [{:id "target"
               :operation "forms"
               :file source-file
               :forms ["greet"]
               :expect {:forms 1}}]
   :expect {:files 1 :requests 1}})

(defn read-json [path]
  (json/parse-string (slurp (io/file path)) true))

(defn read-json-lines [path]
  (with-open [reader (io/reader path)]
    (mapv #(json/parse-string % true) (line-seq reader))))

(defn sha256-file [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [input (io/input-stream path)]
      (let [buffer (byte-array 8192)]
        (loop []
          (let [read-count (.read input buffer)]
            (when (pos? read-count)
              (.update digest buffer 0 read-count)
              (recur))))))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn canonical-data [value]
  (cond
    (map? value) (into (sorted-map-by #(compare (str %1) (str %2)))
                       (map (fn [[key nested]] [key (canonical-data nested)]))
                       value)
    (vector? value) (mapv canonical-data value)
    (sequential? value) (mapv canonical-data value)
    :else value))

(defn normalized-arguments [arguments]
  (cond-> arguments
    (contains? arguments :workspace_root)
    (assoc :workspace_root "<workspace>")))

(defn canonical-json [value]
  (json/generate-string (canonical-data value)))

(defn compile-edit
  ([arguments]
   (compile-edit source-before arguments))
  ([source arguments]
   (let [validated (contract/validate-tool-params arguments)]
     (if-not (:ok validated)
       validated
       (let [spec (contract/tool-params->transaction (:params validated))
             sources {source-file source}
             prepared (compact-location/normalize-spec
                        sources spec (:compact-location-normalization validated))]
         (if (:error prepared)
           prepared
           (assoc prepared
                  :compiled
                  (transaction/compile-transaction sources (:spec prepared)))))))))

(defn correct-future? [compiled]
  (let [file-result (-> compiled :compiled :files first)]
    (and (nil? (:error compiled))
         (nil? (get-in compiled [:compiled :error]))
         (= 1 (get-in compiled [:compiled :changed-file-count]))
         (= source-file (:file file-result))
         (= source-after
            (get-in compiled [:compiled :future-sources source-file])))))

(def external-item-types
  #{"mcp_tool_call" "command_execution" "file_change" "agent_message"})

(defn strict-lifecycle [events timing capture-calls]
  (let [item-events
        (->> events
             (map-indexed
               (fn [index event]
                 (let [item (:item event)]
                   {:index index
                    :event-type (:type event)
                    :item-id (:id item)
                    :item-type (:type item)
                    :server (:server item)
                    :tool (:tool item)
                    :arguments (:arguments item)
                    :text (:text item)})))
             (filterv #(contains? external-item-types (:item-type %))))
        lifecycle-events (filterv #(not= "agent_message" (:item-type %)) item-events)
        starts (filterv #(= "item.started" (:event-type %)) lifecycle-events)
        completions (filterv #(= "item.completed" (:event-type %)) lifecycle-events)
        start-ids (mapv :item-id starts)
        completion-ids (mapv :item-id completions)
        starts-by-id (into {} (map (juxt :item-id identity)) starts)
        completions-by-id (into {} (map (juxt :item-id identity)) completions)
        unique-ids? (and (every? string? (concat start-ids completion-ids))
                         (= (count start-ids) (count (distinct start-ids)))
                         (= (count completion-ids) (count (distinct completion-ids)))
                         (= (set start-ids) (set completion-ids)))
        paired? (and unique-ids?
                     (every?
                       (fn [item-id]
                         (let [start (get starts-by-id item-id)
                               complete (get completions-by-id item-id)]
                           (and (< (:index start) (:index complete))
                                (= (select-keys start [:item-type :server :tool])
                                   (select-keys complete [:item-type :server :tool])))))
                       start-ids))
        mcp-starts (filterv #(= "mcp_tool_call" (:item-type %)) starts)
        inspect-start (first (filter #(= "inspect_clojure" (:tool %)) mcp-starts))
        edit-start (first (filter #(= "edit_clojure" (:tool %)) mcp-starts))
        inspect-complete (get completions-by-id (:item-id inspect-start))
        edit-complete (get completions-by-id (:item-id edit-start))
        messages (filterv #(and (= "item.completed" (:event-type %))
                                (= "agent_message" (:item-type %)))
                          item-events)
        final-message (first messages)
        forbidden (filterv #(contains? #{"command_execution" "file_change"}
                                       (:item-type %))
                           item-events)
        exact-order?
        (and inspect-start inspect-complete edit-start edit-complete final-message
             (< (:index inspect-start) (:index inspect-complete)
                (:index edit-start) (:index edit-complete)
                (:index final-message)))
        observation-by
        (fn [event-kind item-id]
          (first (filter #(and (= event-kind (:event-kind %))
                               (= item-id (:item-id %)))
                         (:observations timing))))
        inspect-clock (observation-by :mcp-tool-call-completed
                                      (:item-id inspect-start))
        edit-clock (observation-by :mcp-tool-call-started
                                   (:item-id edit-start))
        inspect-to-edit-ms
        (when (and inspect-clock edit-clock
                   (< (:observer-monotonic-ns inspect-clock)
                      (:observer-monotonic-ns edit-clock)))
          (/ (- (:observer-monotonic-ns edit-clock)
                (:observer-monotonic-ns inspect-clock))
             1000000.0))
        process-wall-ms (:process-wall-ms timing)
        clock-bounded? (and (number? inspect-to-edit-ms)
                            (pos? inspect-to-edit-ms)
                            (number? process-wall-ms)
                            (pos? process-wall-ms)
                            (<= inspect-to-edit-ms process-wall-ms))
        event-arguments (mapv :arguments mcp-starts)
        capture-arguments (mapv :params capture-calls)
        joined-arguments? (= (mapv normalized-arguments event-arguments)
                             (mapv normalized-arguments capture-arguments))
        ok? (and paired?
                 (= ["inspect_clojure" "edit_clojure"] (mapv :tool mcp-starts))
                 (= 2 (count starts))
                 (= 2 (count capture-calls))
                 (empty? forbidden)
                 (= 1 (count messages))
                 (= "Captured." (:text final-message))
                 exact-order?
                 joined-arguments?
                 clock-bounded?)]
    {:ok ok?
     :paired-lifecycle paired?
     :tool-order (mapv :tool mcp-starts)
     :exact-order exact-order?
     :joined-arguments joined-arguments?
     :forbidden-action-count (count forbidden)
     :final-messages (mapv :text messages)
     :inspect-to-edit-ms inspect-to-edit-ms
     :process-wall-ms process-wall-ms
     :clock-bounded clock-bounded?}))

(defn workspace-files [workspace]
  (let [root (io/file workspace)]
    (->> (file-seq root)
         (filter #(.isFile ^java.io.File %))
         (mapv #(str (.relativize (.toPath root) (.toPath ^java.io.File %))))
         sort
         vec)))

(defn score-capture
  [{:keys [arm position capture events timing workspace codex-home server-pid]}]
  (let [calls (:calls (read-json capture))
        inspect-call (first calls)
        next-call (second calls)
        event-values (when events (read-json-lines events))
        timing-value (when timing (edn/read-string (slurp timing)))
        route (when (and event-values timing-value)
                (strict-lifecycle event-values timing-value calls))
        actual-source (when workspace
                        (slurp (io/file workspace source-file)))
        source-exact? (or (nil? workspace) (= source-before actual-source))
        workspace-clean? (or (nil? workspace)
                             (= [source-file] (workspace-files workspace)))
        requested-root (get-in next-call [:params :workspace_root])
        workspace-root-exact?
        (or (nil? workspace)
            (nil? requested-root)
            (= (.getCanonicalPath (io/file workspace))
               (.getCanonicalPath (io/file requested-root))))
        compiled (when (= "edit_clojure" (:tool_name next-call))
                   (compile-edit (or actual-source source-before) (:params next-call)))
        inspect-request-exact?
        (= expected-inspect-request
           (dissoc (:params inspect-call) :workspace_root))
        argument-bytes (when next-call
                         (count (.getBytes
                                  (canonical-json
                                    (normalized-arguments (:params next-call)))
                                  java.nio.charset.StandardCharsets/UTF_8)))
        correct? (and (= 2 (count calls))
                      (= "inspect_clojure" (:tool_name inspect-call))
                      (= "edit_clojure" (:tool_name next-call))
                      inspect-request-exact?
                      source-exact?
                      workspace-clean?
                      workspace-root-exact?
                      (or (nil? route) (:ok route))
                      (correct-future? compiled))]
    {:schema :clj-surgeon.result-decision-chord-capture-run/v1
     :arm arm
     :position position
     :correct correct?
     :call-count (count calls)
     :tool-order (mapv :tool_name calls)
     :inspect-to-edit-ms (:inspect-to-edit-ms route)
     :logical-edit-argument-bytes argument-bytes
     :route route
     :inspect-request-exact inspect-request-exact?
     :workspace-root-exact workspace-root-exact?
     :source-exact source-exact?
     :workspace-clean workspace-clean?
     :isolation {:workspace workspace
                 :codex-home codex-home
                 :server-pid server-pid
                 :capture capture
                 :events events
                 :capture-sha256 (when capture (sha256-file capture))
                 :events-sha256 (when events (sha256-file events))}
     :compiled (select-keys (:compiled compiled)
                            [:match-count :changed-file-count :error-type])}))

(defn midpoint [values]
  (/ (reduce + values) (double (count values))))

(defn cohort-report [runs]
  (let [runs (vec runs)
        identity (mapv #(select-keys % [:position :arm]) runs)
        complete? (= 4 (count runs))
        correct? (and complete? (every? :correct runs))
        exact-order? (= expected-order identity)
        controls (filterv #(= :control (:arm %)) runs)
        treatments (filterv #(= :treatment (:arm %)) runs)
        clock-values #(mapv :inspect-to-edit-ms %)
        c-values (clock-values controls)
        t-values (clock-values treatments)
        clocks-complete? (and (= 2 (count c-values))
                              (= 2 (count t-values))
                              (every? #(and (number? %) (pos? %))
                                      (concat c-values t-values)))
        c-midpoint (when clocks-complete? (midpoint c-values))
        t-midpoint (when clocks-complete? (midpoint t-values))
        reduction (when clocks-complete?
                    (* 100.0 (/ (- c-midpoint t-midpoint) c-midpoint)))
        identities (mapv :isolation runs)
        isolated? (and complete?
                       (every? #(= 4 (count (distinct (map % identities))))
                               [:workspace :codex-home :server-pid :capture :events]))
        control-bytes (mapv :logical-edit-argument-bytes controls)
        treatment-bytes (mapv :logical-edit-argument-bytes treatments)
        bytes-complete? (and (= 2 (count control-bytes))
                             (= 2 (count treatment-bytes))
                             (every? #(and (integer? %) (pos? %))
                                     (concat control-bytes treatment-bytes)))
        bytes-not-increased? (and bytes-complete?
                                  (<= (midpoint treatment-bytes)
                                      (midpoint control-bytes)))
        paired-wins? (and clocks-complete?
                          (< (:inspect-to-edit-ms (nth runs 1))
                             (:inspect-to-edit-ms (nth runs 0)))
                          (< (:inspect-to-edit-ms (nth runs 2))
                             (:inspect-to-edit-ms (nth runs 3))))
        keep? (and exact-order? correct? isolated? bytes-not-increased?
                   paired-wins? (>= reduction 20.0))]
    {:schema :clj-surgeon.result-decision-chord-capture-cohort/v1
     :ok keep?
     :complete complete?
     :all-correct correct?
     :exact-order exact-order?
     :isolated-runs isolated?
     :argument-bytes-not-increased bytes-not-increased?
     :treatment-wins-both-pairs paired-wins?
     :inspect-to-edit {:control-midpoint-ms c-midpoint
                       :treatment-midpoint-ms t-midpoint
                       :reduction-percent reduction
                       :keep-gate-percent 20.0
                       :stop-below-percent 10.0}}))

(defn parse-options [args]
  (loop [remaining args
         parsed {}]
    (if (empty? remaining)
      parsed
      (let [[key value & more] remaining]
        (when-not (and key value (.startsWith ^String key "--"))
          (throw (ex-info "Expected --key value pairs" {:remaining remaining})))
        (recur more (assoc parsed (keyword (subs key 2)) value))))))

(defn -main [mode & args]
  (let [options (parse-options args)]
    (case mode
      "score-run"
      (println
        (pr-str
          (score-capture
            {:arm (keyword (:arm options))
             :position (parse-long (:position options))
             :capture (:capture options)
             :events (:events options)
             :timing (:timing options)
             :workspace (:workspace options)
             :codex-home (:codex-home options)
             :server-pid (parse-long (:server-pid options))})))

      "cohort-report"
      (let [runs (mapv #(edn/read-string (slurp %))
                       (.split ^String (:runs options) ","))]
        (println (pr-str (cohort-report runs))))

      (throw (ex-info "Unknown mode" {:mode mode})))))
