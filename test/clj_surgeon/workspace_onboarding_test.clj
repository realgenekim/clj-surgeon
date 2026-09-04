(ns clj-surgeon.workspace-onboarding-test
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clj-surgeon.workspace-onboarding :as onboarding]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]))

;; RATCHET (2026-09-04, inb-9483a4): every temp dir this namespace created
;; (cclsp-onboarding, cclsp-prune, kondo-config, up-workspace, up-restart,
;; workspace-onboarding, concurrent-up) went unswept. Track them and sweep
;; after each test.
(def ^:private temp-roots (atom []))
(use-fixtures :each (tmp-leak/tracking-temp-dir-fixture temp-roots))

(def options
  {:surgeon-url "http://127.0.0.1:7889/mcp"
   :cclsp-url "http://127.0.0.1:7891/mcp"})

(deftest cclsp-config-is-rooted-and-json-shaped
  (let [source (onboarding/cclsp-config-source
                 {:workspace "/tmp/a workspace"
                  :lsp-command "/opt/homebrew/bin/clojure-lsp"
                  :kondo-config-dir "/tmp/repository/.clj-kondo"})
        parsed (json/parse-string source true)
        server (first (:servers parsed))]
    (is (= "/tmp/a workspace" (:rootDir server)))
    (is (= ["/opt/homebrew/bin/clojure-lsp"] (:command server)))
    (is (= ["clj" "cljs" "cljc" "edn"] (:extensions server)))
    (is (= ["" "src" "test" "dev"] (:sourceRoots server)))
    (is (= 10000 (:requestTimeout server)))
    (is (= 120000 (:initializationTimeout server)))
    (is (= "/tmp/repository/.clj-kondo"
           (get-in server [:initializationOptions :kondo-config-dir])))
    (is (str/ends-with? source "}\n"))))

(deftest definition-vocabulary-discovery-is-nearest-and-repository-bounded
  (testing "the pure ancestor order stops at the repository root"
    (is (= ["/repo/server2/src" "/repo/server2" "/repo"]
           (onboarding/ancestor-directories "/repo/server2/src" "/repo")))
    (is (= :workspace-outside-repository
           (try
             (onboarding/ancestor-directories "/other/server2" "/repo")
             nil
             (catch clojure.lang.ExceptionInfo error
               (:error-type (ex-data error)))))))
  (testing "the real boundary selects a parent config for a nested workspace"
    (let [root (.toFile
                 (tmp-leak/track!
                   temp-roots
                   (java.nio.file.Files/createTempDirectory
                     "clj-surgeon-kondo-config"
                     (make-array java.nio.file.attribute.FileAttribute 0))))
          workspace (io/file root "server2")
          config (io/file root ".clj-kondo" "config.edn")]
      (.mkdirs (io/file root ".git"))
      (.mkdirs workspace)
      (.mkdirs (.getParentFile config))
      (spit config "{}")
      (is (= (.getCanonicalPath (.getParentFile config))
             (onboarding/effective-kondo-config-dir workspace)))
      (let [nearer (io/file workspace ".clj-kondo" "config.edn")]
        (.mkdirs (.getParentFile nearer))
        (spit nearer "{}")
        (is (= (.getCanonicalPath (.getParentFile nearer))
               (onboarding/effective-kondo-config-dir workspace)))))))

(deftest shared-cclsp-config-preserves-other-servers-and-deduplicates-roots
  (let [before (str "{\"servers\":["
                    "{\"extensions\":[\"ts\"],\"command\":[\"ts-lsp\"],\"rootDir\":\"/repo/a\"},"
                    "{\"extensions\":[\"clj\"],\"command\":[\"old\"],\"rootDir\":\"/repo/a\"}]}\n")
        options {:workspace "/repo/a" :lsp-command "/bin/clojure-lsp"}
        once (onboarding/upsert-cclsp-workspace before options)
        twice (onboarding/upsert-cclsp-workspace once options)]
    (is (= once twice))
    (is (str/includes? once "ts-lsp"))
    (is (not (str/includes? once "\"old\"")))
    (is (= 2 (count (re-seq #"/repo/a" once))))
    (is (str/includes? once "/bin/clojure-lsp"))))

(deftest shared-cclsp-config-prunes-only-missing-managed-roots
  (let [managed-live {"extensions" ["clj" "cljs" "cljc" "edn"]
                      "command" ["/bin/clojure-lsp"]
                      "rootDir" "/repo/live"
                      "requestTimeout" 10000
                      "initializationTimeout" 45000
                      "sourceRoots" ["src"]}
        managed-missing (assoc managed-live "rootDir" "/repo/missing")
        marked-missing (assoc managed-missing "managedBy" "clj-surgeon")
        unmanaged-clojure {"extensions" ["clj"]
                           "command" ["custom-lsp"]
                           "rootDir" "/custom/missing"}
        unrelated {"extensions" ["ts"]
                   "command" ["ts-lsp"]
                   "rootDir" "/typescript/missing"}
        before (str (json/generate-string
                      {"servers" [managed-live managed-missing marked-missing
                                  unmanaged-clojure unrelated]})
                    "\n")
        after (onboarding/upsert-cclsp-workspace
                before
                {:workspace "/repo/live"
                 :lsp-command "/bin/clojure-lsp"
                 :existing-workspace-roots #{"/repo/live"}})
        servers (get (json/parse-string after) "servers")]
    (is (= #{"/repo/live" "/custom/missing" "/typescript/missing"}
           (set (map #(get % "rootDir") servers))))
    (is (= "clj-surgeon" (get (first servers) "managedBy")))
    (is (= "custom-lsp" (first (get (second servers) "command"))))))

(deftest shared-cclsp-config-does-not-reorder-an-already-onboarded-workspace
  (let [a {:workspace "/repo/a" :lsp-command "/bin/clojure-lsp"}
        b {:workspace "/repo/b" :lsp-command "/bin/clojure-lsp"}
        initial (-> "{\"servers\":[]}\n"
                    (onboarding/upsert-cclsp-workspace a)
                    (onboarding/upsert-cclsp-workspace b))
        after-a (onboarding/upsert-cclsp-workspace initial a)
        after-b (onboarding/upsert-cclsp-workspace after-a b)]
    (is (= initial after-a)
        "selecting an existing workspace must not restart the shared provider")
    (is (= initial after-b)
        "workspace selection order must not rewrite shared configuration")))

(deftest managed-block-is-bounded-and-mcp-first
  (let [block (onboarding/workspace-mcp-block options)]
    (is (str/includes? block "[mcp_servers.clj-surgeon]"))
    (is (str/includes? block "required = true"))
    (is (str/includes? block
                       "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\", \"transform_clojure\", \"relation_census\", \"alias_migration\"]"))
    (is (not (str/includes? block "[mcp_servers.cclsp]")))
    (is (not (str/includes? block "resolve_var_surface")))
    (is (not (str/includes? block "rename")))))

(deftest upsert-preserves-unmanaged-settings-and-is-idempotent
  (let [before "model = \"gpt-5\"\n\n[features]\napps = true\n"
        once (onboarding/upsert-workspace-block before options)
        twice (onboarding/upsert-workspace-block once options)]
    (is (str/starts-with? once before))
    (is (= 1 (count (re-seq
                      #"# BEGIN clj-surgeon workspace tools"
                      once))))
    (is (= once twice))))

(deftest upsert-migrates-existing-canonical-tool-tables-without-duplicates
  (let [before (str "model = \"gpt-5\"\n\n"
                    "[mcp_servers.cclsp]\n"
                    "command = \"old-cclsp\"\n\n"
                    "[mcp_servers.cclsp.env]\n"
                    "OLD_ROOT = \"/tmp/old\"\n\n"
                    "[mcp_servers.unrelated]\n"
                    "command = \"keep-me\"\n")
        after (onboarding/upsert-workspace-block before options)]
    (is (not (str/includes? after "[mcp_servers.cclsp]")))
    (is (= 1 (count (re-seq #"\[mcp_servers\.clj-surgeon\]" after))))
    (is (not (str/includes? after "old-cclsp")))
    (is (not (str/includes? after "OLD_ROOT")))
    (is (str/includes? after
                       "[mcp_servers.unrelated]\ncommand = \"keep-me\"\n"))
    (is (str/starts-with? after "model = \"gpt-5\"\n\n"))
    (is (= after (onboarding/upsert-workspace-block after options)))))

(deftest upsert-recovers-a-managed-block-after-an-older-tool-table
  (let [managed (onboarding/workspace-mcp-block
                  {:surgeon-url "http://127.0.0.1:7001/mcp"
                   :cclsp-url "http://127.0.0.1:7002/mcp"})
        before (str "[mcp_servers.cclsp]\ncommand = \"old-cclsp\"\n\n"
                    managed "\n")
        after (onboarding/upsert-workspace-block before options)]
    (is (not (str/includes? after "[mcp_servers.cclsp]")))
    (is (= 1 (count (re-seq #"\[mcp_servers\.clj-surgeon\]" after))))
    (is (str/includes? after "http://127.0.0.1:7889/mcp"))
    (is (not (str/includes? after "old-cclsp")))
    (is (= after (onboarding/upsert-workspace-block after options)))))

(deftest upsert-replaces-only-the-managed-block
  (let [before (onboarding/upsert-workspace-block
                 "model = \"gpt-5\"\n"
                 options)
        after (onboarding/upsert-workspace-block
                before
                {:surgeon-url "http://127.0.0.1:8123/mcp"
                 :cclsp-url "http://127.0.0.1:8124/mcp"})]
    (is (str/includes? after "model = \"gpt-5\""))
    (is (str/includes? after "http://127.0.0.1:8123/mcp"))
    (is (not (str/includes? after "http://127.0.0.1:7889/mcp")))))

(deftest malformed-managed-blocks-and-non-loopback-urls-refuse
  (doseq [source [onboarding/managed-begin
                  onboarding/managed-end
                  (str onboarding/managed-begin "\n"
                       onboarding/managed-begin "\n"
                       onboarding/managed-end)]]
    (is (= :invalid-managed-workspace-block
           (:error-type
             (ex-data
               (try
                 (onboarding/upsert-workspace-block source options)
                 (catch Exception e e)))))))
  (doseq [[field bad-url] [[:surgeon-url "http://localhost:7889/mcp"]]]
    (is (= {:error-type :invalid-workspace-mcp-url
            :field field
            :value bad-url}
           (ex-data
             (try
               (onboarding/workspace-mcp-block (assoc options field bad-url))
               (catch Exception e e)))))))

(deftest installation-is-atomic-and-preserves-existing-config
  (let [root (.toFile
               (tmp-leak/track!
                 temp-roots
                 (java.nio.file.Files/createTempDirectory
                   "clj-surgeon-workspace-onboarding"
                   (make-array java.nio.file.attribute.FileAttribute 0))))
        codex-dir (io/file root ".codex")
        target (io/file codex-dir "config.toml")]
    (.mkdirs codex-dir)
    (spit target "model = \"gpt-5\"\n")
    (let [receipt (onboarding/install-workspace-config!
                    (assoc options :workspace (.getPath root)))
          installed (slurp target)]
      (is (:ok receipt))
      (is (:changed receipt))
      (is (:restart-required receipt))
      (is (str/starts-with? installed "model = \"gpt-5\"\n"))
      (is (not (str/includes? installed "[mcp_servers.cclsp]")))
      (is (= installed
             (slurp target)))
      (is (false?
            (:changed
              (onboarding/install-workspace-config!
                (assoc options :workspace (.getPath root))))))
      (is (false?
            (:restart-required
              (onboarding/install-workspace-config!
                (assoc options :workspace (.getPath root)))))))))

(deftest cclsp-config-installation-uses-the-canonical-workspace
  (let [root (.toFile
               (tmp-leak/track!
                 temp-roots
                 (java.nio.file.Files/createTempDirectory
                   "clj-surgeon-cclsp-onboarding"
                   (make-array java.nio.file.attribute.FileAttribute 0))))
        target (io/file root "state" "cclsp.json")
        receipt (onboarding/install-cclsp-config!
                  {:workspace (.getPath root)
                   :config-file (.getPath target)
                   :lsp-command "/opt/homebrew/bin/clojure-lsp"})]
    (is (:ok receipt))
    (is (= (.getCanonicalPath root) (:workspace receipt)))
    (is (str/includes? (slurp target) (.getCanonicalPath root)))))

(deftest cclsp-config-installation-prunes-a-missing-managed-worktree
  (let [root (.toFile
               (tmp-leak/track!
                 temp-roots
                 (java.nio.file.Files/createTempDirectory
                   "clj-surgeon-cclsp-prune"
                   (make-array java.nio.file.attribute.FileAttribute 0))))
        missing (io/file root "deleted-worktree")
        target (io/file root "state" "cclsp.json")]
    (.mkdirs (.getParentFile target))
    (spit target
          (str (json/generate-string
                 {"servers" [(onboarding/cclsp-server
                               (.getPath missing)
                               "/opt/homebrew/bin/clojure-lsp")]})
               "\n"))
    (let [receipt (onboarding/install-cclsp-workspace! {:workspace (.getPath root) :config-file (.getPath target) :lsp-command "/opt/homebrew/bin/clojure-lsp"})
          servers (get (json/parse-string (slurp target)) "servers")]
      (is (= [(.getPath missing)] (:pruned-workspaces receipt)))
      (is (= [(.getCanonicalPath root)]
             (mapv #(get % "rootDir") servers))))))

(deftest concurrent-cclsp-registrations-are-lossless-and-read-back-verified
  (let [parent (io/file (System/getProperty "java.io.tmpdir")
                        (str "clj-surgeon-concurrent-up-" (random-uuid)))
        workspace-a (doto (io/file parent "workspace-a") .mkdirs)
        workspace-b (doto (io/file parent "workspace-b") .mkdirs)
        config-file (io/file parent "cclsp.json")
        unrelated {"extensions" ["ts"]
                   "command" ["typescript-language-server" "--stdio"]
                   "rootDir" (.getCanonicalPath parent)}
        original-upsert onboarding/upsert-cclsp-workspace
        first-entered (promise)
        second-entered (promise)
        release-first (promise)
        calls (atom 0)]
    (try
      (spit config-file
            (str (json/generate-string {"servers" [unrelated]} {:pretty true}) "\n"))
      (with-redefs [onboarding/upsert-cclsp-workspace
                    (fn [source options]
                      (case (swap! calls inc)
                        1 (do (deliver first-entered true)
                              @release-first)
                        2 (deliver second-entered true)
                        nil)
                      (original-upsert source options))]
        (let [install #(onboarding/install-cclsp-workspace!
                         {:workspace (.getPath %)
                          :config-file (.getPath config-file)
                          :lsp-command "/usr/local/bin/clojure-lsp"})
              first-result (future (install workspace-a))]
          (is (= true (deref first-entered 1000 ::timeout)))
          (let [second-result (future (install workspace-b))]
            (is (= ::timeout (deref second-entered 100 ::timeout))
                "the second read-modify-write waits outside the locked boundary")
            (deliver release-first true)
            (is (:persisted @first-result))
            (is (:persisted @second-result)))))
      (let [servers (get (json/parse-string (slurp config-file)) "servers")
            roots (set (map #(get % "rootDir") servers))]
        (is (= 3 (count servers)))
        (is (contains? roots (.getCanonicalPath workspace-a)))
        (is (contains? roots (.getCanonicalPath workspace-b)))
        (is (some #(= unrelated %) servers)))
      (finally
        (try (fs/delete-tree parent) (catch Throwable _ nil))))))

(deftest up-is-one-idempotent-shared-stack-entrance
  (let [root (.toFile
               (tmp-leak/track!
                 temp-roots
                 (java.nio.file.Files/createTempDirectory
                   "clj-surgeon-up-workspace"
                   (make-array java.nio.file.attribute.FileAttribute 0))))
        state (io/file root "state")
        commands (atom [])
        probes (atom [])
        runner (fn [directory command]
                 (swap! commands conj {:directory directory :command command})
                 {:command command :exit 0})
        readiness-probe
        (fn [url workspace]
          (swap! probes conj {:url url :workspace workspace})
          {:ok true
           :workspace workspace
           :config-generation 2})
        arguments {:workspace (.getPath root)
                   :tool-root (.getCanonicalPath (io/file "."))
                   :state-dir (.getPath state)
                   :lsp-command "/opt/homebrew/bin/clojure-lsp"
                   :runner runner
                   :readiness-probe readiness-probe}
        first-result (onboarding/up! arguments)
        second-result (onboarding/up! arguments)]
    (is (:ok first-result))
    (is (:shared first-result))
    (is (:cclsp-config-changed first-result))
    (is (:restart-required first-result))
    (is (false? (:cclsp-config-changed second-result)))
    (is (false? (:restart-required second-result)))
    (is (false? (:cclsp-server-restarted second-result)))
    (is (false? (:agent-session-restart-required second-result)))
    (is (= 2 (count @commands)))
    (is (not-any? #(some #{"cclsp-stop"} (:command %)) @commands)
        "onboarding another workspace must not interrupt active shared sessions")
    (is (= 2 (count (filter #(some #{"mcp-start"} (:command %)) @commands))))
    (is (= 2 (count @probes)))
    (is (every? #(= onboarding/shared-cclsp-url (:url %)) @probes))
    (is (every? #(some (fn [argument]
                         (str/starts-with? argument "CCLSP_STATE_DIR="))
                       (:command %))
                @commands))
    (is (every? #(= (.getCanonicalPath root) (:workspace %)) @probes))
    (is (= 2 (get-in first-result [:readiness :config-generation])))
    (is (= onboarding/shared-surgeon-url
           (get-in first-result [:servers :clj-surgeon])))
    (is (= onboarding/shared-cclsp-url
           (get-in first-result [:servers :cclsp])))
    (is (not (str/includes? (slurp (:codex-config first-result))
                            "[mcp_servers.cclsp]")))
    (is (= 1 (count (re-seq
                      (re-pattern (java.util.regex.Pattern/quote
                                    (.getCanonicalPath root)))
                      (slurp (:cclsp-config first-result))))))))

(deftest internal-semantic-provider-restart-does-not-invalidate-agent-sessions
  (let [root (.toFile
               (tmp-leak/track!
                 temp-roots
                 (java.nio.file.Files/createTempDirectory
                   "clj-surgeon-up-restart"
                   (make-array java.nio.file.attribute.FileAttribute 0))))
        state (io/file root "state")
        runner (fn [_ command]
                 (let [state-argument (first (filter #(str/starts-with?
                                                        % "CCLSP_STATE_DIR=")
                                                     command))
                       state-dir (subs state-argument (count "CCLSP_STATE_DIR="))
                       status (io/file state-dir "last-start.edn")]
                   (.mkdirs (.getParentFile status))
                   (spit status "{:server-restarted true}\n")
                   {:command command :exit 0}))
        result (onboarding/up!
                 {:workspace (.getPath root)
                  :tool-root (.getCanonicalPath (io/file "."))
                  :state-dir (.getPath state)
                  :lsp-command "/opt/homebrew/bin/clojure-lsp"
                  :runner runner
                  :readiness-probe (fn [_ workspace]
                                     {:ok true :workspace workspace})})]
    (is (:ok result))
    (is (:cclsp-server-restarted result))
    (is (false? (:agent-session-restart-required result)))
    (is (:restart-required result))))
