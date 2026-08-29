(ns result-decision-chord-capture-server
  "Full-catalog no-effect server for the post-inspect decision-chord screen."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def decision-chord
  (str "Requested evidence is complete for this snapshot. Do not reread the "
       "returned owner. Decide the requested change, then issue one guarded "
       "mutation. Read again only if evidence required for that decision is "
       "absent from this result."))

(def source-file "src/sample/core.clj")

(def source-before
  (str "(ns sample.core)\n\n"
       "(defn greet [name]\n"
       "  (str \"Hello, \" name))\n"))

(def inspect-request
  {:requests [{:id "target"
               :operation "forms"
               :file source-file
               :forms ["greet"]
               :expect {:forms 1}}]
   :expect {:files 1 :requests 1}})

(def frozen-inspect-result
  (let [validated (inspect/validate-inspect-params inspect-request)]
    (assoc
      (inspect/evaluate-snapshots
        (:params validated)
        {source-file {:file source-file
                      :source source-before
                      :hash (structural-lens/source-hash source-before)}})
      :elapsed_ms 0.0)))

(def frozen-inspect-content
  [(inspect/concise-summary frozen-inspect-result)])

(defn- write-json-atomically! [path value]
  (let [target (io/file path)
        parent (.getParentFile target)
        temporary (io/file parent (str "." (.getName target) ".tmp"))]
    (.mkdirs parent)
    (spit temporary (json/generate-string value {:pretty true}))
    (java.nio.file.Files/move
      (.toPath temporary)
      (.toPath target)
      (into-array java.nio.file.CopyOption
                  [java.nio.file.StandardCopyOption/REPLACE_EXISTING
                   java.nio.file.StandardCopyOption/ATOMIC_MOVE]))))

(defn append-decision-chord [content]
  (mapv (fn [item]
          (if (and (string? item) (not (str/includes? item decision-chord)))
            (str item "\n\n" decision-chord)
            item))
        content))

(defn- record-call! [calls capture-file phase tool params]
  (let [call {:index (inc (count @calls))
              :phase phase
              :tool_id (:id tool)
              :tool_name (:name tool)
              :params params}
        observed (swap! calls conj call)]
    (write-json-atomically!
      capture-file
      {:schema "clj-surgeon.result-decision-chord-capture.v1"
       :calls observed})
    observed))

(defn inspect-handler [arm calls capture-file tool]
  (fn [_exchange params callback]
    (let [observed (record-call! calls capture-file "inspect" tool params)]
      (if (= 1 (count observed))
        (callback
          (if (= :treatment arm)
            (append-decision-chord frozen-inspect-content)
            frozen-inspect-content)
          false
          frozen-inspect-result)
        (callback
          ["inspect_clojure\n  captured repeated read · no source returned"]
          false
          {:ok true
           :captured true
           :call_count (count observed)
           :selected_tool "inspect_clojure"
           :source_unchanged true
           :next_action "none"})))))

(defn capture-handler [calls capture-file tool]
  (fn [_exchange params callback]
    (let [started (System/nanoTime)
          observed (record-call! calls capture-file "next-action" tool params)]
      (callback
        [(str (:name tool)
              "\n  captured · no mutation\n"
              "✓ offline scorer owns validation")]
        false
        {:ok true
         :captured true
         :call_count (count observed)
         :selected_tool (:name tool)
         :source_unchanged true
         :elapsed_ms (/ (- (System/nanoTime) started) 1000000.0)
         :next_action "none"}))))

(defn capture-tools [arm capture-file]
  (let [calls (atom [])]
    (mapv (fn [tool]
            (assoc tool :tool-fn
                   (if (= "inspect_clojure" (:name tool))
                     (inspect-handler arm calls capture-file tool)
                     (capture-handler calls capture-file tool))))
          (mcp-tool/tools-for-profile :full))))

(defn- public-surface [tool]
  (select-keys tool [:id :name :description :schema :output-schema :annotations]))

(defn start
  "Start one isolated full-catalog decision-chord capture server."
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
        {:schema "clj-surgeon.result-decision-chord-surface.v1"
         :arm (name arm)
         :instructions mcp-server/server-instructions
         :tools (mapv public-surface tools)}))
    (with-redefs [mcp-tool/all-tools (constantly tools)]
      (http-server/start server-opts))))
