(ns extraction-call-capture-server
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [extraction-tool-surface :as surface]))

(def instructions
  "One capture-only clj-surgeon tool is available. It records arguments and never reads or writes project source.")

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

(defn capture-handler [capture-file]
  (let [calls (atom [])]
    (fn [_exchange params callback]
      (let [started (System/nanoTime)
            call {:index (inc (count @calls))
                  :params params}
            observed (swap! calls conj call)]
        (write-json-atomically!
          capture-file
          {:schema "clj-surgeon.extraction-call-capture.v1"
           :calls observed})
        (callback
          ["apply_clojure_changes\n  captured · no mutation\n✓ offline scorer owns validation"]
          false
          {:ok true
           :captured true
           :call_count (count observed)
           :source_unchanged true
           :elapsed_ms (/ (- (System/nanoTime) started) 1000000.0)
           :next_action "none"})))))

(defn capture-tool [arm capture-file]
  (let [base (surface/production-tool)
        projected (surface/tool-surface arm)]
    (assoc base
           :description (:description projected)
           :schema (:schema projected)
           :tool-fn (capture-handler capture-file))))

(defn start
  "Start one isolated extraction capture server. No production registry changes."
  [{:keys [arm capture-file surface-receipt-file] :as opts}]
  (when-not (#{:control :treatment} arm)
    (throw (ex-info "arm must be :control or :treatment" {:arm arm})))
  (when-not capture-file
    (throw (ex-info "capture-file is required" {})))
  (let [tool (capture-tool arm capture-file)
        server-opts (dissoc opts :arm :capture-file :surface-receipt-file)]
    (when surface-receipt-file
      (write-json-atomically!
        surface-receipt-file
        {:schema "clj-surgeon.extraction-call-surface.v1"
         :arm (name arm)
         :instructions instructions
         :tool {:name (:name tool)
                :description (:description tool)
                :input-schema (:schema tool)
                :output-schema (:output-schema tool)
                :annotations (:annotations tool)}}))
    (with-redefs [mcp-tool/all-tools (constantly [tool])
                  mcp-server/server-instructions instructions]
      (http-server/start server-opts))))
