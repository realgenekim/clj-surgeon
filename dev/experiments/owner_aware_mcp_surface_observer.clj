(ns owner-aware-mcp-surface-observer
  "Fail-closed comparison of a server receipt with Codex's MCP registry.

  The Codex apps cache is a different artifact and is never evidence that a
  configured MCP tool was visible to the caller."
  (:require
   [capture-codex-mcp-registry :as registry-capture]
   [cheshire.core :as json]
   [clojure.java.io :as io]))

(def registry-schema "clj-surgeon.codex-mcp-registry.v1")

(def registry-observation-source
  registry-capture/mcp-registry-observation-source)

(defn- fail [error-type message data]
  {:ok false
   :error-type error-type
   :error message
   :data data})

(defn- normalized-annotations [annotations]
  (or annotations {}))

(defn- input-schema-relation
  "Codex may retain the advertised schema or drop only its top-level anyOf.
  Nested schema structure remains exact."
  [advertised observed]
  (cond
    (= advertised observed)
    {:ok true :normalization :none}

    (and (contains? advertised :anyOf)
         (not (contains? observed :anyOf))
         (= (dissoc advertised :anyOf) observed))
    {:ok true :normalization :drop-top-level-any-of}

    :else
    (fail :client-input-schema-mismatch
          "Codex-observed input schema differs beyond the permitted top-level anyOf drop"
          {:advertised advertised :observed observed})))

(defn compare-tool-surfaces
  "Compare one advertised tool with one Codex-observed MCP tool.

  The only accepted projection differences are annotations null versus {} and
  removal of the advertised input schema's top-level anyOf."
  [advertised observed]
  (let [input-relation (input-schema-relation (:input-schema advertised)
                                              (:input-schema observed))
        advertised-other (-> advertised
                             (dissoc :input-schema :annotations)
                             (assoc :annotations
                                    (normalized-annotations
                                      (:annotations advertised))))
        observed-other (-> observed
                           (dissoc :input-schema :annotations)
                           (assoc :annotations
                                  (normalized-annotations
                                    (:annotations observed))))]
    (cond
      (not (:ok input-relation)) input-relation
      (not= advertised-other observed-other)
      (fail :client-tool-surface-mismatch
            "Codex-observed tool surface differs beyond permitted projection normalization"
            {:advertised advertised-other :observed observed-other})
      :else
      {:ok true
       :normalizations
       (cond-> []
         (not= (:annotations advertised) (:annotations observed))
         (conj :annotations-null-empty-object)
         (= :drop-top-level-any-of (:normalization input-relation))
         (conj :drop-top-level-any-of))})))

(defn validate-observation
  "Validate exact client-visible MCP evidence for one expected tool.

  A codex_apps_tools cache cannot pass because it has neither the registry
  receipt schema nor the app-server JSON-RPC provenance."
  [advertised-receipt registry-receipt expected-server expected-tool]
  (let [expected-source
        (assoc registry-observation-source
               :server-selector {:field "name" :value expected-server})
        tool-names (:tool-names registry-receipt)
        projections (:tool-projection registry-receipt)]
    (cond
      (not= registry-schema (:schema registry-receipt))
      (fail :registry-source-mismatch
            "Evidence is not a Codex MCP registry receipt"
            {:expected-schema registry-schema
             :observed-schema (:schema registry-receipt)})

      (not= expected-source (:observation-source registry-receipt))
      (fail :registry-provenance-mismatch
            "MCP registry receipt has unexpected observation provenance"
            {:expected expected-source
             :observed (:observation-source registry-receipt)})

      (not (true? (:ok registry-receipt)))
      (fail :registry-observation-failed
            "Codex MCP registry receipt is not successful"
            {:ok (:ok registry-receipt)})

      (not= expected-server (:server registry-receipt))
      (fail :registry-server-mismatch
            "Codex MCP registry receipt selected the wrong server"
            {:expected expected-server :observed (:server registry-receipt)})

      (not= [expected-tool] tool-names)
      (fail :registry-tool-set-mismatch
            "Codex MCP registry did not expose the exact expected tool set"
            {:expected [expected-tool] :observed tool-names})

      (not= 1 (count projections))
      (fail :registry-tool-projection-count-mismatch
            "Codex MCP registry receipt must contain one tool projection"
            {:expected 1 :observed (count projections)})

      (not= expected-tool (get-in advertised-receipt [:tool :name]))
      (fail :advertised-tool-mismatch
            "Server receipt advertises an unexpected tool"
            {:expected expected-tool
             :observed (get-in advertised-receipt [:tool :name])})

      :else
      (let [comparison (compare-tool-surfaces (:tool advertised-receipt)
                                              (first projections))]
        (if (:ok comparison)
          {:ok true
           :source :codex-mcp-registry
           :server expected-server
           :tool expected-tool
           :normalizations (:normalizations comparison)}
          comparison)))))

(defn- parse-pairs [args]
  (when (odd? (count args))
    (throw (ex-info "Expected --key value pairs" {:args args})))
  (into {} (map (fn [[key value]] [(keyword (subs key 2)) value]))
        (partition 2 args)))

(defn- read-json [path]
  (json/parse-string (slurp (io/file path)) true))

(defn -main [& args]
  (let [{:keys [advertised registry server tool]} (parse-pairs args)
        result (validate-observation (read-json advertised)
                                     (read-json registry)
                                     server tool)]
    (println (json/generate-string result))
    (when-not (:ok result)
      (throw (ex-info "Client-visible MCP surface validation failed" result)))))
