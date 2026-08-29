(ns owner-aware-call-capture-server
  "Isolated capture-only MCP adapter for the owner-aware call screen."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [owner-aware-call-construction-screen :as screen])
  (:import
   (java.nio.file Files StandardCopyOption)))

(def instructions
  (str "This isolated benchmark exposes one capture-only edit_clojure tool. "
       "Construct the complete requested edit in one call. The adapter records "
       "arguments for offline validation and never reads or writes source."))

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

(defn capture-handler [capture-file]
  (let [calls (atom [])]
    (fn [_exchange params callback]
      (let [started (System/nanoTime)
            call {:index (inc (count @calls))
                  :params params}
            observed (swap! calls conj call)]
        (write-json-atomically!
          capture-file
          {:schema "clj-surgeon.owner-aware-call-capture.v1"
           :calls observed})
        (callback
          ["edit_clojure\n  captured · no mutation\n✓ offline scorer owns validation"]
          false
          {:ok true
           :captured true
           :call_count (count observed)
           :source_unchanged true
           :elapsed_ms (/ (- (System/nanoTime) started) 1000000.0)
           :next_action "none"})))))

(defn capture-tool [arm capture-file]
  (let [base (or (first (filter #(= :edit-clojure (:id %))
                                (mcp-server/public-tool-registry)))
                 (throw (ex-info "edit_clojure is absent from the registry" {})))
        surface (screen/tool-surface arm)]
    (assoc base
           :name (:name surface)
           :description (:description surface)
           :schema (:schema surface)
           :tool-fn (capture-handler capture-file))))

(defn start
  "Start one isolated, capture-only server. No production registry is changed."
  [{:keys [arm capture-file surface-receipt-file] :as opts}]
  (when-not (#{:control :candidate} arm)
    (throw (ex-info "arm must be :control or :candidate" {:arm arm})))
  (when-not capture-file
    (throw (ex-info "capture-file is required" {})))
  (let [tool (capture-tool arm capture-file)
        server-opts (dissoc opts :arm :capture-file :surface-receipt-file)]
    (when surface-receipt-file
      (write-json-atomically!
        surface-receipt-file
        {:schema "clj-surgeon.owner-aware-call-surface.v1"
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
