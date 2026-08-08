(ns clj-surgeon.mcp-workspace
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.security MessageDigest)))

(defn- refusal
  [message value]
  {:ok false
   :operation "workspace-route"
   :error_type "invalid-workspace-root"
   :reason "invalid-workspace-root"
   :path ["workspace_root"]
   :workspace_root value
   :error message
   :source_unchanged true
   :next_action "pass_an_existing_absolute_workspace_root"})

(defn canonical-root
  "Return one existing canonical absolute directory, or a stable refusal."
  [value]
  (cond
    (not (and (string? value) (not (str/blank? value))))
    (refusal "workspace_root must be a non-blank absolute directory" value)

    (not (.isAbsolute (io/file value)))
    (refusal "workspace_root must be absolute" value)

    :else
    (try
      (let [root (.getCanonicalFile (io/file value))]
        (if (.isDirectory root)
          {:ok true :workspace-root (.getPath root)}
          (refusal "workspace_root must be an existing directory" value)))
      (catch Exception error
        (assoc (refusal "workspace_root could not be canonicalized" value)
               :cause (.getMessage error))))))

(defn receipt-dir
  "Return the deterministic local-state receipt directory for one workspace."
  [workspace-root]
  (let [{:keys [ok workspace-root] :as resolved} (canonical-root workspace-root)]
    (when-not ok
      (throw (ex-info (:error resolved) resolved)))
    (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                          (.getBytes ^String workspace-root "UTF-8"))
          workspace-id (apply str (map #(format "%02x" (bit-and 0xff (int %)))
                                       digest))]
      (str (io/file (System/getProperty "user.home")
                    ".local" "state" "clj-surgeon" "workspaces"
                    workspace-id "receipts")))))

(defn router
  "Create one shared, lazy, canonical-root context router."
  [base-config]
  {:base-config base-config
   :contexts (atom {})})

(defn- build-context
  [{:keys [base-config]} workspace-root]
  (let [factory (:workspace-context-factory base-config)
        context (if factory
                  (factory workspace-root)
                  (assoc base-config :project-root workspace-root))]
    (-> base-config
        (merge context)
        (assoc :project-root workspace-root)
        (dissoc :workspace-context-factory))))

(defn resolve-request
  "Resolve and remove workspace_root, returning one cached isolated context."
  [{:keys [base-config contexts] :as workspace-router} params]
  (let [requested (or (:workspace_root params)
                      (:project-root base-config))
        resolved (canonical-root requested)]
    (if-not (:ok resolved)
      resolved
      (let [root (:workspace-root resolved)
            candidate (delay (build-context workspace-router root))
            context-delay
            (get (swap! contexts
                        #(if (contains? % root)
                           %
                           (assoc % root candidate)))
                 root)]
        {:ok true
         :workspace-root root
         :config @context-delay
         :params (dissoc params :workspace_root)}))))

(defn cached-roots
  "Return canonical roots with initialized or pending isolated contexts."
  [{:keys [contexts]}]
  (vec (sort (keys @contexts))))
