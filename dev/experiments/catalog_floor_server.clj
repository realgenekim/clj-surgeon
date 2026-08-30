(ns catalog-floor-server
  "Static no-effect MCP catalogs for the Codex catalog-floor experiment."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]))

(defn- pad [n]
  (apply str (take n (cycle "catalog surface padding "))))

(defn- no-effect-handler [tool-name]
  (fn [_exchange _params callback]
    (callback
      [(str tool-name "\n  refused · catalog-floor tools are measurement-only")]
      true
      {:ok false
       :error_type "catalog-floor-tool-called"
       :source_unchanged true
       :next_action "none"})))

(defn- probe-tool
  ([index description-chars]
   (probe-tool index description-chars {}))
  ([index description-chars properties]
   (let [tool-name (format "catalog_probe_%02d" index)]
     {:id (keyword tool-name)
      :name tool-name
      :description (str "Measurement-only tool. Never call. "
                        (pad description-chars))
      :schema {:type "object"
               :additionalProperties false
               :properties properties}
      :output-schema {:type "object"
                      :properties {"ok" {:type "boolean"}}
                      :required ["ok"]}
      :annotations {:title "Catalog floor probe"
                    :read-only true
                    :destructive false
                    :idempotent true
                    :open-world false}
      :outcome-classes #{:typed-refusal}
      :structured? true
      :tool-fn (no-effect-handler tool-name)})))

(defn- parameter-properties [count description-chars]
  (into {}
        (for [index (range count)]
          [(format "parameter_%03d" index)
           {:type "string"
            :description (str "Measurement-only parameter. "
                              (pad description-chars))}])))

(defn tools-for-arm
  "Return one static catalog. D/P/M are intentionally near 64 KiB by different
  dimensions: one description, many parameters, or many tools."
  [arm]
  (case arm
    :tiny [(probe-tool 0 16)]
    :description [(probe-tool 0 64000)]
    :parameters [(probe-tool 0 16 (parameter-properties 128 430))]
    :many (mapv #(probe-tool % 3900) (range 16))
    :real (mapv (fn [tool]
                  (assoc tool :tool-fn (no-effect-handler (:name tool))))
                (mcp-tool/tools-for-profile :full))
    (throw (ex-info "Unsupported catalog-floor arm"
                    {:arm arm
                     :supported [:tiny :description :parameters :many :real]}))))

(defn- public-tool [tool]
  (select-keys tool [:id :name :description :schema :output-schema
                     :annotations :structured?]))

(defn- write-json-atomically! [path value]
  (let [target (io/file path)
        parent (.getParentFile target)
        stage (io/file parent (str "." (.getName target) ".tmp." (random-uuid)))]
    (.mkdirs parent)
    (spit stage (str (json/generate-string value {:pretty true}) "\n"))
    (java.nio.file.Files/move
      (.toPath stage)
      (.toPath target)
      (into-array java.nio.file.CopyOption
                  [java.nio.file.StandardCopyOption/ATOMIC_MOVE
                   java.nio.file.StandardCopyOption/REPLACE_EXISTING]))))

(defn start
  "Start one pre-materialized static catalog server and block until stopped."
  [{:keys [arm surface-receipt-file] :as opts}]
  (let [tools (tools-for-arm arm)
        server-opts (dissoc opts :arm :surface-receipt-file)]
    (when surface-receipt-file
      (write-json-atomically!
        surface-receipt-file
        {:schema "clj-surgeon.catalog-floor-surface.v1"
         :arm (name arm)
         :instructions mcp-server/server-instructions
         :tools (mapv public-tool tools)}))
    (with-redefs [mcp-tool/all-tools (constantly tools)]
      (http-server/start server-opts))))
