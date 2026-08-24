(ns clj-surgeon.workspace-onboarding
  "Install the bounded project-local Codex entrance for one repo-rooted stack."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mcp-source-anchor :as source-anchor]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def managed-begin "# BEGIN clj-surgeon workspace tools")
(def managed-end "# END clj-surgeon workspace tools")
(def managed-placeholder "[clj_surgeon_workspace_placeholder]")
(def shared-surgeon-url "http://127.0.0.1:7888/mcp")
(def shared-cclsp-url "http://127.0.0.1:7890/mcp")
(def clojure-extensions ["clj" "cljs" "cljc" "edn"])

(defn- cclsp-health-url
  [cclsp-url]
  (str/replace cclsp-url #"/mcp/?$" "/healthz"))

(defn- read-cclsp-health
  [cclsp-url]
  (json/parse-string (slurp (cclsp-health-url cclsp-url)) true))

(defn await-cclsp-workspace!
  "Wait until the shared provider has hot-loaded one canonical workspace."
  ([cclsp-url workspace]
   (await-cclsp-workspace! cclsp-url workspace 15000))
  ([cclsp-url workspace timeout-ms]
   (let [health-url (cclsp-health-url cclsp-url)
         deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop [last-health nil]
       (let [health (try
                      (read-cclsp-health cclsp-url)
                      (catch Exception _ last-health))]
         (cond
           (and health (:config_error health))
           (throw (ex-info "cclsp rejected the shared workspace configuration"
                           {:error-type :cclsp-config-reload-failed
                            :workspace workspace
                            :health-url health-url
                            :health health}))

           (and (:ok health)
                (not (:config_reloading health))
                (some #{workspace} (:workspace_roots health)))
           {:ok true
            :workspace workspace
            :pid (:pid health)
            :health-url health-url
            :config-generation (:config_generation health)}

           (< (System/currentTimeMillis) deadline)
           (do (Thread/sleep 100)
               (recur health))

           :else
           (throw (ex-info "cclsp did not hot-load the workspace before the readiness deadline"
                           {:error-type :cclsp-workspace-not-ready
                            :workspace workspace
                            :health-url health-url
                            :health health
                            :remedy (str "Check " health-url
                                         ", then retry the same clj-surgeon up command.")}))))))))

(defn ancestor-directories
  "Return workspace and each parent through repository-root, nearest first."
  [workspace repository-root]
  (let [workspace-path (-> (io/file workspace) .getCanonicalFile .toPath .normalize)
        root-path (-> (io/file repository-root) .getCanonicalFile .toPath .normalize)]
    (when-not (.startsWith workspace-path root-path)
      (throw (ex-info "Workspace must be inside the repository root"
                      {:error-type :workspace-outside-repository
                       :workspace (str workspace-path)
                       :repository-root (str root-path)})))
    (loop [path workspace-path
           result []]
      (let [result (conj result (str path))]
        (if (= path root-path)
          result
          (recur (.getParent path) result))))))

(defn- repository-root-file
  [workspace]
  (let [workspace-file (.getCanonicalFile (io/file workspace))]
    (loop [directory workspace-file]
      (cond
        (.exists (io/file directory ".git")) directory
        (nil? (.getParentFile directory)) workspace-file
        :else (recur (.getParentFile directory))))))

(defn effective-kondo-config-dir
  "Return the nearest repository-bounded .clj-kondo config directory."
  [workspace]
  (let [repository-root (repository-root-file workspace)]
    (some (fn [directory]
            (let [config (io/file directory ".clj-kondo" "config.edn")]
              (when (.isFile config)
                (.getCanonicalPath (.getParentFile config)))))
          (ancestor-directories workspace (.getPath repository-root)))))

(defn cclsp-server
  ([workspace lsp-command]
   (cclsp-server workspace lsp-command nil))
  ([workspace lsp-command kondo-config-dir]
   (cond-> {"extensions" clojure-extensions
            "command" [lsp-command]
            "rootDir" workspace
            "requestTimeout" 10000
            "initializationTimeout" 120000
            "sourceRoots" (source-anchor/workspace-source-roots workspace)
            "managedBy" "clj-surgeon"}
     kondo-config-dir
     (assoc "initializationOptions"
            {"kondo-config-dir" kondo-config-dir}))))

(defn cclsp-config-source
  "Return a cclsp JSON config rooted at one canonical workspace."
  [{:keys [workspace lsp-command kondo-config-dir]}]
  (when-not (and (string? workspace) (not (str/blank? workspace)))
    (throw (ex-info "workspace is required"
                    {:error-type :invalid-workspace :workspace workspace})))
  (when-not (and (string? lsp-command) (not (str/blank? lsp-command)))
    (throw (ex-info "lsp-command is required"
                    {:error-type :invalid-lsp-command :lsp-command lsp-command})))
  (str (json/generate-string
         {"servers" [(cclsp-server workspace lsp-command kondo-config-dir)]}
         {:pretty true})
       "\n"))

(defn- clojure-server?
  [server]
  (boolean
    (some (set clojure-extensions) (get server "extensions" []))))

(defn managed-clojure-server?
  "Recognize current marked entries and the exact legacy shape published by clj-surgeon."
  [server]
  (let [command (get server "command")]
    (and (clojure-server? server) (or (= "clj-surgeon" (get server "managedBy")) (and (= (set clojure-extensions) (set (get server "extensions" []))) (= 10000 (get server "requestTimeout")) (contains? #{45000 120000} (get server "initializationTimeout")) (vector? (get server "sourceRoots")) (= 1 (count command)) (string? (first command)) (str/ends-with? (first command) "clojure-lsp"))))))

(defn upsert-cclsp-workspace
  "Return one deterministic multi-root cclsp config with workspace upserted.

  When existing-workspace-roots is supplied, remove only missing entries that
  carry clj-surgeon's marker or exact legacy signature."
  [source {:keys [workspace lsp-command kondo-config-dir existing-workspace-roots]}]
  (let [parsed (if (str/blank? source)
                 {"servers" []}
                 (json/parse-string source))
        servers (get parsed "servers")]
    (when-not (and (map? parsed) (vector? servers))
      (throw (ex-info "cclsp config must contain a servers array"
                      {:error-type :invalid-cclsp-config})))
    (let [servers (if (set? existing-workspace-roots)
                    (filterv
                      (fn [server]
                        (let [root (get server "rootDir")]
                          (or (= workspace root)
                              (not (managed-clojure-server? server))
                              (contains? existing-workspace-roots root))))
                      servers)
                    servers)
          replacement (cclsp-server workspace lsp-command kondo-config-dir)
          [found? servers]
          (reduce
            (fn [[found? updated] server]
              (if (and (= workspace (get server "rootDir"))
                       (clojure-server? server))
                (if found?
                  [true updated]
                  [true (conj updated replacement)])
                [found? (conj updated server)]))
            [false []]
            servers)
          servers (cond-> servers
                    (not found?) (conj replacement))
          updated (assoc parsed "servers" servers)]
      (str (json/generate-string updated {:pretty true}) "\n"))))

(defonce ^:private cclsp-config-locks (atom {}))

(defn- cclsp-config-lock
  [target]
  (let [path (.getPath target)]
    (or (get @cclsp-config-locks path)
        (get (swap! cclsp-config-locks
                    #(if (contains? % path)
                       %
                       (assoc % path (Object.))))
             path))))

(defn- exact-server-persisted?
  [source expected]
  (let [parsed (json/parse-string source)]
    (boolean (some #(= expected %) (get parsed "servers")))))

(defn install-cclsp-workspace!
  "Atomically upsert one canonical workspace and prune missing managed roots."
  [{:keys [workspace config-file lsp-command kondo-config-dir]}]
  (let [root (.getCanonicalFile (io/file workspace))
        target (.getCanonicalFile (io/file config-file))
        kondo-config-dir (or kondo-config-dir
                             (effective-kondo-config-dir (.getPath root)))]
    (when-not (.isDirectory root)
      (throw (ex-info "Workspace must be an existing directory"
                      {:error-type :invalid-workspace :workspace workspace})))
    (.mkdirs (.getParentFile target))
    (let [monitor (cclsp-config-lock target)
          lock-file (io/file (str (.getPath target) ".lock"))]
      (locking monitor
        (with-open [lock-handle (java.io.RandomAccessFile. lock-file "rw")
                    channel (.getChannel lock-handle)]
          (let [_file-lock (.lock channel)
                before (if (.isFile target) (slurp target) "")
                parsed-before (if (str/blank? before)
                                {"servers" []}
                                (json/parse-string before))
                existing-workspace-roots
                (->> (get parsed-before "servers" []) (keep #(get % "rootDir")) (filter string?) (filter #(.isDirectory (io/file %))) set)
                expected (cclsp-server (.getPath root)
                                       lsp-command
                                       kondo-config-dir)
                after (upsert-cclsp-workspace
                        before {:workspace (.getPath root)
                                :lsp-command lsp-command
                                :kondo-config-dir kondo-config-dir
                                :existing-workspace-roots existing-workspace-roots})
                before-roots (set (keep #(get % "rootDir")
                                        (get parsed-before "servers" [])))
                after-roots (set (keep #(get % "rootDir")
                                       (get (json/parse-string after) "servers" [])))
                pruned-workspaces (sort (remove after-roots before-roots))]
            (when (not= before after)
              (file-ops/atomic-write! target after))
            (let [read-back (slurp target)]
              (when-not (exact-server-persisted? read-back expected)
                (throw (ex-info "cclsp workspace registration was not persisted exactly"
                                {:error-type :cclsp-registration-not-persisted
                                 :workspace (.getPath root)
                                 :config-file (.getPath target)}))))
            {:ok true
             :operation :upsert-shared-cclsp-workspace
             :workspace (.getPath root)
             :kondo-config-dir kondo-config-dir
             :config-file (.getPath target)
             :changed (not= before after)
             :pruned-workspaces pruned-workspaces
             :persisted true}))))))

(defn install-cclsp-config!
  "Atomically install one repo-rooted cclsp config."
  [{:keys [workspace config-file lsp-command]}]
  (let [root (.getCanonicalFile (io/file workspace))
        target (.getCanonicalFile (io/file config-file))]
    (when-not (.isDirectory root)
      (throw (ex-info "Workspace must be an existing directory"
                      {:error-type :invalid-workspace :workspace workspace})))
    (.mkdirs (.getParentFile target))
    (file-ops/atomic-write!
      target
      (cclsp-config-source
        {:workspace (.getPath root) :lsp-command lsp-command}))
    {:ok true
     :operation :install-workspace-cclsp-config
     :workspace (.getPath root)
     :config-file (.getPath target)}))

(defn- marker-count [source marker]
  (loop [from 0
         result 0]
    (let [at (.indexOf ^String source ^String marker from)]
      (if (neg? at)
        result
        (recur (+ at (count marker)) (inc result))))))

(defn loopback-mcp-url?
  "True when URL names an explicit IPv4 loopback MCP endpoint."
  [url]
  (boolean
    (and (string? url)
         (re-matches #"http://127\.0\.0\.1:[0-9]+/mcp" url))))

(defn workspace-mcp-block
  "Return the complete managed Codex TOML block for three bounded tools."
  [{:keys [surgeon-url cclsp-url]}]
  (when-not (loopback-mcp-url? surgeon-url)
    (throw (ex-info "surgeon-url must be an explicit loopback MCP URL"
                    {:error-type :invalid-workspace-mcp-url
                     :field :surgeon-url
                     :value surgeon-url})))
  (when-not (loopback-mcp-url? cclsp-url)
    (throw (ex-info "cclsp-url must be an explicit loopback MCP URL"
                    {:error-type :invalid-workspace-mcp-url
                     :field :cclsp-url
                     :value cclsp-url})))
  (str managed-begin "\n"
       "[mcp_servers.clj-surgeon]\n"
       "url = \"" surgeon-url "\"\n"
       "required = true\n"
       "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\"]\n\n"
       "[mcp_servers.cclsp]\n"
       "url = \"" cclsp-url "\"\n"
       "required = true\n"
       "enabled_tools = [\"resolve_var_surface\", \"find_references\", "
       "\"get_incoming_calls\", \"get_outgoing_calls\"]\n"
       managed-end))

(defn- managed-tool-header? [line]
  (boolean
    (re-matches
      #"\s*\[mcp_servers\.(?:clj-surgeon|cclsp)(?:\.[^]]+)?\]\s*(?:#.*)?\r?\n?"
      line)))

(defn- any-table-header? [line]
  (boolean (re-matches #"\s*\[[^]]+\].*\r?\n?" line)))

(defn- remove-existing-tool-tables
  "Remove canonical tool tables and their subtables; preserve other sections."
  [source]
  (->> (str/split source #"(?<=\n)" -1)
       (reduce
         (fn [{:keys [lines skipping?]} line]
           (cond
             (managed-tool-header? line)
             {:lines lines :skipping? true}

             (and skipping? (any-table-header? line))
             {:lines (conj lines line) :skipping? false}

             skipping?
             {:lines lines :skipping? true}

             :else
             {:lines (conj lines line) :skipping? false}))
         {:lines [] :skipping? false})
       :lines
       str/join))

(defn upsert-workspace-block
  "Add or replace exactly one managed block while preserving all other bytes."
  [source options]
  (let [source (or source "")
        begin-count (marker-count source managed-begin)
        end-count (marker-count source managed-end)
        block (workspace-mcp-block options)]
    (when (str/includes? source managed-placeholder)
      (throw (ex-info "Codex config contains the reserved managed placeholder"
                      {:error-type :invalid-managed-workspace-block})))
    (when-not (or (and (zero? begin-count) (zero? end-count))
                  (and (= 1 begin-count) (= 1 end-count)))
      (throw (ex-info "Codex config has incomplete or duplicate managed markers"
                      {:error-type :invalid-managed-workspace-block
                       :begin-count begin-count
                       :end-count end-count})))
    (if (zero? begin-count)
      (let [without-tools (remove-existing-tool-tables source)]
        (str without-tools
             (cond
               (str/blank? without-tools) ""
               (str/ends-with? without-tools "\n\n") ""
               (str/ends-with? without-tools "\n") "\n"
               :else "\n\n")
             block "\n"))
      (let [begin (.indexOf ^String source managed-begin)
            end-marker (.indexOf ^String source managed-end)
            end (+ end-marker (count managed-end))]
        (when (< end-marker begin)
          (throw (ex-info "Managed Codex markers are out of order"
                          {:error-type :invalid-managed-workspace-block
                           :begin-count begin-count
                           :end-count end-count})))
        (-> (str (subs source 0 begin)
                 managed-placeholder
                 (subs source end))
            remove-existing-tool-tables
            (str/replace managed-placeholder block))))))

(defn install-workspace-config!
  "Atomically install the managed block under WORKSPACE/.codex/config.toml."
  [{:keys [workspace] :as options}]
  (let [root (.getCanonicalFile (io/file workspace))]
    (when-not (.isDirectory root)
      (throw (ex-info "Workspace must be an existing directory"
                      {:error-type :invalid-workspace
                       :workspace workspace})))
    (let [codex-dir (io/file root ".codex")
          target (io/file codex-dir "config.toml")
          before (if (.isFile target) (slurp target) "")
          after (upsert-workspace-block before options)]
      (.mkdirs codex-dir)
      (file-ops/atomic-write! target after)
      {:ok true
       :operation :install-workspace-mcp
       :workspace (.getPath root)
       :config-file (.getPath target)
       :changed (not= before after)
       :surgeon-url (:surgeon-url options)
       :cclsp-url (:cclsp-url options)
       :restart-required (not= before after)})))

(defn- command-path
  [command]
  (some (fn [directory]
          (let [candidate (io/file directory command)]
            (when (and (.isFile candidate) (.canExecute candidate))
              (.getPath candidate))))
        (str/split (or (System/getenv "PATH") "")
                   (re-pattern (java.io.File/pathSeparator)))))

(defn- source-tool-root
  []
  (let [pointer-file
        (some-> (System/getenv "CLJ_SURGEON_CONTROL_PLANE_ROOT_FILE") io/file)
        pointed-root
        (when (and pointer-file (.isFile pointer-file))
          (some-> (slurp pointer-file) str/trim not-empty))]
    (or (System/getenv "CLJ_SURGEON_HOME")
        pointed-root
        (some-> (io/resource "clj_surgeon/workspace_onboarding.clj")
                .toURI
                io/file
                .getParentFile
                .getParentFile
                .getParentFile
                .getPath))))

(defn run-command!
  "Run one inherited-IO command and return its exit status."
  [directory command]
  (let [process (-> (ProcessBuilder. (into-array String command))
                    (.directory (io/file directory))
                    .inheritIO
                    .start)]
    {:command command :exit (.waitFor process)}))

(defn- require-command!
  [runner directory command]
  (let [result (runner directory command)]
    (when-not (zero? (:exit result))
      (throw (ex-info "Shared MCP lifecycle command failed"
                      {:error-type :mcp-lifecycle-failed
                       :command command
                       :exit (:exit result)})))
    result))

(defn up!
  "Idempotently onboard one workspace onto the shared hot MCP stack."
  [{:keys [workspace tool-root state-dir lsp-command runner readiness-probe
           surgeon-url cclsp-url]}]
  (let [workspace-file (.getCanonicalFile
                         (io/file (or workspace
                                      (System/getProperty "user.dir"))))
        tool-root-file (.getCanonicalFile
                         (io/file (or tool-root (source-tool-root))))
        state-dir-file (.getCanonicalFile
                         (io/file (or state-dir
                                      (str (io/file
                                             (System/getProperty "user.home")
                                             ".local" "state" "clj-surgeon")))))
        lsp-command (or lsp-command (command-path "clojure-lsp"))
        runner (or runner run-command!)
        cclsp-config (io/file state-dir-file "cclsp.json")]
    (when-not (.isDirectory workspace-file)
      (throw (ex-info "Workspace must be an existing directory"
                      {:error-type :invalid-workspace
                       :workspace workspace})))
    (when-not (and (.isDirectory tool-root-file)
                   (.isFile (io/file tool-root-file "Makefile")))
      (throw (ex-info "clj-surgeon source root with Makefile is required"
                      {:error-type :invalid-tool-root
                       :tool-root (.getPath tool-root-file)})))
    (when-not lsp-command
      (throw (ex-info "clojure-lsp is required"
                      {:error-type :missing-clojure-lsp})))
    (let [cclsp (install-cclsp-workspace!
                  {:workspace (.getPath workspace-file)
                   :config-file (.getPath cclsp-config)
                   :lsp-command lsp-command})
          make-base ["make" "--no-print-directory" "-C"
                     (.getPath tool-root-file)]
          cclsp-state-dir (io/file state-dir-file "cclsp")
          make-env [(str "CCLSP_CONFIG=" (.getPath cclsp-config))
                    (str "CCLSP_STATE_DIR=" (.getPath cclsp-state-dir))]
          commands [(into make-base (concat ["mcp-start"] make-env))]
          command-results (mapv #(require-command!
                                   runner (.getPath tool-root-file) %)
                                commands)
          start-status-file (io/file cclsp-state-dir "last-start.edn")
          start-status (when (.isFile start-status-file)
                         (edn/read-string (slurp start-status-file)))
          cclsp-restarted? (true? (:server-restarted start-status))
          installed (install-workspace-config!
                      {:workspace (.getPath workspace-file)
                       :surgeon-url (or surgeon-url shared-surgeon-url)
                       :cclsp-url (or cclsp-url shared-cclsp-url)})
          readiness ((or readiness-probe await-cclsp-workspace!)
                     (or cclsp-url shared-cclsp-url)
                     (.getPath workspace-file))]
      {:ok true
       :operation :clj-surgeon-up
       :workspace (.getPath workspace-file)
       :shared true
       :servers {:clj-surgeon (or surgeon-url shared-surgeon-url)
                 :cclsp (or cclsp-url shared-cclsp-url)}
       :cclsp-config (.getPath cclsp-config)
       :kondo-config-dir (:kondo-config-dir cclsp)
       :cclsp-config-changed (:changed cclsp)
       :cclsp-server-restarted cclsp-restarted?
       :agent-session-restart-required cclsp-restarted?
       :commands (mapv :command command-results)
       :codex-config (:config-file installed)
       :codex-config-changed (:changed installed)
       :restart-required (or (:changed installed) cclsp-restarted?)
       :readiness readiness})))

(defn -main
  [& [operation & arguments]]
  (try
    (println
      (pr-str
        (case operation
          "cclsp-config"
          (let [[workspace config-file lsp-command] arguments]
            (when-not (and workspace config-file lsp-command)
              (throw (ex-info
                       (str "Usage: workspace-onboarding cclsp-config "
                            "WORKSPACE CONFIG_FILE LSP_COMMAND")
                       {:error-type :invalid-arguments})))
            (install-cclsp-config!
              {:workspace workspace
               :config-file config-file
               :lsp-command lsp-command}))

          "install"
          (let [[workspace surgeon-url cclsp-url] arguments]
            (when-not (and workspace surgeon-url cclsp-url)
              (throw (ex-info
                       (str "Usage: workspace-onboarding install "
                            "WORKSPACE SURGEON_URL CCLSP_URL")
                       {:error-type :invalid-arguments})))
            (install-workspace-config!
              {:workspace workspace
               :surgeon-url surgeon-url
               :cclsp-url cclsp-url}))

          (throw (ex-info "Operation must be cclsp-config or install"
                          {:error-type :invalid-operation :operation operation})))))
    (catch Exception e
      (binding [*out* *err*]
        (println
          (pr-str
            (merge {:ok false :error (.getMessage e)} (ex-data e)))))
      (System/exit 1))))
