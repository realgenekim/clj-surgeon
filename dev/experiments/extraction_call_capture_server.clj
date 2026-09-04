(ns extraction-call-capture-server
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [extraction-tool-surface :as surface]))

(defn- write-json-atomically! [path value]
  (let [target (io/file path)
        parent (.getParentFile target)
        stage (io/file parent (str "." (.getName target) ".tmp." (random-uuid)))]
    (.mkdirs parent)
    (spit stage (json/generate-string value))
    (java.nio.file.Files/move
      (.toPath stage)
      (.toPath target)
      (into-array java.nio.file.CopyOption
                  [java.nio.file.StandardCopyOption/ATOMIC_MOVE
                   java.nio.file.StandardCopyOption/REPLACE_EXISTING]))))

;; @spec MCP-OP-TIME-005
;; Published through the shared finalizer, not beside it — see the note in
;; `owner-aware-call-capture-server`. A capture server that invents its own
;; top-level clock produces a corpus no canonical reader can summarize.
(defn capture-handler [calls capture-file tool]
  (fn [_exchange params callback]
    (mcp-operation/invoke!
      {:execute
       (fn []
         (let [call {:index (inc (count @calls))
                     :tool_id (:id tool)
                     :tool_name (:name tool)
                     :params params}
               observed (swap! calls conj call)]
           (write-json-atomically!
             capture-file
             {:schema "clj-surgeon.extraction-call-capture.v1"
              :calls observed})
           {:ok true
            :captured true
            :call_count (count observed)
            :selected_tool (:name tool)
            :source_unchanged true
            :next_action "none"}))
       :summarize
       (constantly (str (:name tool)
                        "\n  captured · no mutation\n"
                        "✓ offline scorer owns validation"))
       :callback callback})))

(defn capture-tool [arm calls capture-file tool]
  (let [base (surface/production-tool)
        projected (surface/tool-surface arm)]
    (cond-> (assoc tool :tool-fn (capture-handler calls capture-file tool))
      (= (:id tool) (:id base))
      (assoc :description (:description projected)
             :schema (:schema projected)))))

(defn capture-tools
  "Project the full production catalog with no-effect handlers.

  Only the apply_clojure_changes description and schema vary by arm."
  [arm capture-file]
  (let [calls (atom [])]
    (mapv #(capture-tool arm calls capture-file %)
          (mcp-tool/tools-for-profile :full))))

(defn- public-surface [tool]
  (select-keys tool [:id :name :description :schema :output-schema
                     :annotations :structured?]))

(defn start
  "Start one isolated extraction capture server. No production registry changes."
  [{:keys [arm capture-file surface-receipt-file] :as opts}]
  (when-not (#{:control :treatment} arm)
    (throw (ex-info "arm must be :control or :treatment" {:arm arm})))
  (when-not capture-file
    (throw (ex-info "capture-file is required" {})))
  (let [tools (capture-tools arm capture-file)
        server-opts (dissoc opts :arm :capture-file :surface-receipt-file)]
    (when surface-receipt-file
      (write-json-atomically!
        surface-receipt-file
        {:schema "clj-surgeon.extraction-call-surface.v1"
         :arm (name arm)
         :instructions mcp-server/server-instructions
         :tools (mapv public-surface tools)}))
    (with-redefs [mcp-tool/all-tools (constantly tools)]
      (http-server/start server-opts))))
