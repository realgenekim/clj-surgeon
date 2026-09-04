(ns clj-surgeon.mcp-workspace
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.security MessageDigest)))

(defn- refusal
  "One stable workspace-root refusal, carrying its own cause and remedy.

  `workspace_root` is an ENVELOPE key on every receipt this server publishes —
  the routed root, rendered structurally rather than as a discriminating fact —
  so on the one refusal that is ABOUT that value it was suppressed, and the
  receipt named no other fact that separated a blank root from a relative one.
  `workspace_root_given` is the caller's own text, `pr-str`d so nil and \"\"
  stay distinguishable, and it is not an envelope key.

  The remedy is carried rather than implied: a refusal renderer that has none
  to show still has to say what the caller does next, and the alternative it
  had been reduced to was a text block pointing at a remedy that was not
  there."
  [message value]
  {:ok false
   :operation "workspace-route"
   :error_type "invalid-workspace-root"
   :reason "invalid-workspace-root"
   :path ["workspace_root"]
   :workspace_root value
   :workspace_root_given (pr-str value)
   :error message
   :source_unchanged true
   :next_action "pass_an_existing_absolute_workspace_root"
   :remedy (str "Resend with workspace_root set to an absolute path naming a "
                "directory that already exists; " (pr-str value)
                " is not one. No next_call is composed because only the caller "
                "knows which workspace it meant.")})

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

(defn workspace-id
  "Return the stable digest that names one canonical workspace root."
  [canonical-workspace-root]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes ^String canonical-workspace-root "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff (int %))) digest))))

(defn state-dir
  "Return the deterministic local-state directory for one workspace.

   Every durable per-workspace artefact - receipts, transaction journals,
   pre-image objects, projection caches - hangs off this one root, so a
   workspace's state is found, quota'd and cleaned in one place. `state-home`
   overrides the user home the directory hangs from, which is what test
   isolation needs; it is not a request parameter."
  ([workspace-root] (state-dir workspace-root nil))
  ([workspace-root state-home]
   (let [{:keys [ok workspace-root] :as resolved} (canonical-root workspace-root)]
     (when-not ok
       (throw (ex-info (:error resolved) resolved)))
     (str (io/file (or state-home (System/getProperty "user.home"))
                   ".local" "state" "clj-surgeon" "workspaces"
                   (workspace-id workspace-root))))))

(defn receipt-dir
  "Return the deterministic local-state receipt directory for one workspace."
  [workspace-root]
  (str (io/file (state-dir workspace-root) "receipts")))

(defn transactions-dir
  "Return the deterministic local-state transaction directory for one workspace.

   Transaction journals, their sorted manifests and their pre-image object
   store live beside the receipts rather than in a project-local directory, so
   a mutation never writes bookkeeping into the tree it is mutating."
  ([workspace-root] (transactions-dir workspace-root nil))
  ([workspace-root state-home]
   (str (io/file (state-dir workspace-root state-home) "transactions"))))

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
