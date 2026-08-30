(ns rename-verb-proxy
  "Experiment-only MCP adapter for the preregistered rename-verb screen."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool])
  (:import
   (java.nio.file Files StandardCopyOption)))

(def expected-verb
  {"op" "rename-symbol"
   "from" "jitter-ms"
   "to" "retry-jitter-ms"})

(def verb-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"op" {:type "string" :enum ["rename-symbol"]}
    "from" {:type "string" :enum ["jitter-ms"]}
    "to" {:type "string" :enum ["retry-jitter-ms"]}}
   :required ["op" "from" "to"]})

(def complete-edit-request
  {"edits"
   [{"file" "src/bench/retry.clj"
     "within" {"form" "jitter-ms"}
     "from" "jitter-ms"
     "to" "retry-jitter-ms"
     "matches" 1}
    {"file" "src/bench/retry.clj"
     "within" {"form" "retry-delay-ms"}
     "from" "jitter-ms"
     "to" "retry-jitter-ms"
     "matches" 1}
    {"file" "src/bench/retry.clj"
     "within" {"form" "scheduled-at-ms"}
     "from" "jitter-ms"
     "to" "retry-jitter-ms"
     "matches" 1}
    {"file" "src/bench/retry.clj"
     "within" {"form" "retry-window-ms"}
     "from" "jitter-ms"
     "to" "retry-jitter-ms"
     "matches" 1}
    {"file" "src/bench/retry.clj"
     "within" {"form" "retry-budget-ms"}
     "from" "jitter-ms"
     "to" "retry-jitter-ms"
     "matches" 1}
    {"file" "src/bench/worker.clj"
     "within" {"form" "next-job"}
     "from" "retry/jitter-ms"
     "to" "retry/retry-jitter-ms"
     "matches" 1}
    {"file" "src/bench/worker.clj"
     "within" {"form" "park-worker"}
     "from" "retry/jitter-ms"
     "to" "retry/retry-jitter-ms"
     "matches" 1}
    {"file" "src/bench/worker.clj"
     "within" {"form" "worker-deadline"}
     "from" "retry/jitter-ms"
     "to" "retry/retry-jitter-ms"
     "matches" 1}]})

(def instructions
  (str "This isolated experiment exposes one edit_clojure tool backed by the "
       "published transaction compiler. Complete the supplied rename in one "
       "call. The server owns the workspace."))

(defn string-keyed [value]
  (json/parse-string (json/generate-string value)))

(defn lower-verb [params]
  (let [request (string-keyed params)]
    (if (= expected-verb request)
      {:ok true :request complete-edit-request}
      {:ok false
       :error_type "invalid-rename-verb"
       :error "Expected exactly op=rename-symbol, from=jitter-ms, to=retry-jitter-ms"
       :source_unchanged true
       :mutation_attempted false
       :write_authority false
       :next_action "correct_request"})))

(defn- write-json-atomically! [path value]
  (let [output (java.io.File. path)
        _ (some-> output .getParentFile .mkdirs)
        temporary (java.io.File.
                    (str path ".tmp."
                         (.pid (java.lang.ProcessHandle/current))))]
    (spit temporary (str (json/generate-string value {:pretty true}) "\n"))
    (Files/move (.toPath temporary)
                (.toPath output)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))))

(defn recording-handler [arm capture-file]
  (fn [exchange params callback]
    (let [request (string-keyed params)
          lowered (if (= :V arm) (lower-verb request)
                      {:ok true :request request})]
      (write-json-atomically!
        capture-file
        {:schema "clj-surgeon.rename-verb-proxy-capture.v1"
         :arm (name arm)
         :emitted_request request
         :lowered_request (when (:ok lowered) (:request lowered))})
      (if-not (:ok lowered)
        (callback [(:error lowered)] true lowered)
        (mcp-tool/handle-edit-clojure
          exchange (:request lowered)
          (fn [content error? result]
            (write-json-atomically!
              capture-file
              {:schema "clj-surgeon.rename-verb-proxy-capture.v1"
               :arm (name arm)
               :emitted_request request
               :lowered_request (:request lowered)
               :result result
               :error error?})
            (callback content error? result)))))))

(defn screen-tool [arm capture-file]
  (let [base mcp-tool/edit-clojure-tool]
    (cond-> (assoc base :tool-fn (recording-handler arm capture-file))
      (= :V arm)
      (assoc :description
             (str "Rename one supplied symbol across the frozen fixture. "
                  "Call exactly once with op=rename-symbol, from, and to. "
                  "The experiment adapter expands only the predeclared sites "
                  "and delegates to the published edit transaction.")
             :schema verb-schema))))

(defn start
  "Start one isolated V or T server without changing the product registry."
  [{:keys [arm capture-file surface-receipt-file] :as opts}]
  (when-not (#{:V :T} arm)
    (throw (ex-info "arm must be :V or :T" {:arm arm})))
  (when-not capture-file
    (throw (ex-info "capture-file is required" {})))
  (let [tool (screen-tool arm capture-file)
        server-opts (dissoc opts :arm :capture-file :surface-receipt-file)]
    (when surface-receipt-file
      (write-json-atomically!
        surface-receipt-file
        {:schema "clj-surgeon.rename-verb-proxy-surface.v1"
         :arm (name arm)
         :instructions instructions
         :tool {:name (:name tool)
                :description (:description tool)
                :input_schema (:schema tool)
                :output_schema (:output-schema tool)
                :annotations (:annotations tool)}}))
    (with-redefs [mcp-tool/all-tools (constantly [tool])
                  mcp-server/server-instructions instructions]
      (http-server/start server-opts))))
