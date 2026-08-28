#!/usr/bin/env bb

(ns run-selector-continuation-benchmark
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn env
  ([name default] (or (System/getenv name) default))
  ([name] (System/getenv name)))

(defn now-ms [] (System/currentTimeMillis))

(def repo-root (-> *file* fs/parent fs/parent fs/canonicalize str))
(def pre-root (env "BENCH_PRE_ROOT" "/private/tmp/clj-surgeon-selector-continuation-pre"))
(def post-root
  (env "BENCH_POST_ROOT" "/private/tmp/clj-surgeon-selector-continuation-post"))
(def pre-commit (env "BENCH_PRE_COMMIT" "f5431352418caa5d75605644291db898753e311d"))
(def post-commit (env "BENCH_POST_COMMIT" "8125854c9c40c278898185fba6f685fb26131e29"))
(def model (env "BENCH_MODEL" "gpt-5.6-sol"))
(def reasoning (env "BENCH_REASONING" "high"))
(def port (parse-long (env "BENCH_PORT" "7895")))
(def lane-order
  (mapv keyword (str/split (env "BENCH_ORDER" "pre,post") #",")))
(def prompt-style (keyword (env "BENCH_PROMPT_STYLE" "manual-continuation")))
(def auth-file
  (env "CODEX_AUTH_FILE"
       (str (fs/path (System/getProperty "user.home") ".codex/auth.json"))))
(def result-root
  (or (env "BENCH_RESULT_DIR")
      (str (fs/create-temp-dir {:prefix "clj-surgeon-selector-continuation."}))))
(def corpus (str (fs/path result-root "corpus")))

(def task
  {:task-id "two-file-prefix-selector-miss"
   :completed-id "path-owner"
   :pending-id "contract-owner"
   :requested-owner "compiles-owner-relative-top-level-insertion"
   :expected-owner "validates-top-level-insertion-without-repeating-owner-source"})

(defn checked [argv options]
  (let [{:keys [exit out err] :as result}
        @(proc/process argv (merge {:out :string :err :string} options))]
    (when-not (zero? exit)
      (throw (ex-info "Benchmark command failed"
                      {:argv argv :exit exit :stderr err})))
    (assoc result :out (str/trim out))))

(defn git-head [root revision]
  (:out (checked ["git" "rev-parse" revision] {:dir root})))

(defn ensure-inputs! []
  (doseq [command ["codex" "clojure" "bb" "git" "curl"]]
    (checked ["sh" "-c" (str "command -v " command)] {}))
  (when-not (fs/regular-file? auth-file)
    (throw (ex-info "Codex authentication file is missing" {:auth-file auth-file})))
  (doseq [[lane root expected] [[:pre pre-root pre-commit]
                                [:post post-root post-commit]]]
    (when-not (= expected (git-head root "HEAD"))
      (throw (ex-info "Benchmark worktree is at the wrong commit"
                      {:lane lane :expected expected :actual (git-head root "HEAD")}))))
  (let [{:keys [exit]}
        @(proc/process ["lsof" "-nP" (str "-iTCP:" port) "-sTCP:LISTEN"]
                       {:out :string :err :string})]
    (when (zero? exit)
      (throw (ex-info "Benchmark port is already in use" {:port port})))))

(defn prepare-corpus! []
  (fs/create-dirs (fs/path corpus "src/clj_surgeon"))
  (fs/create-dirs (fs/path corpus "test/clj_surgeon"))
  (fs/copy (fs/path post-root "src/clj_surgeon/mcp_paths.clj")
           (fs/path corpus "src/clj_surgeon/mcp_paths.clj")
           {:replace-existing true})
  (fs/copy (fs/path post-root "test/clj_surgeon/mcp_contract_test.clj")
           (fs/path corpus "test/clj_surgeon/mcp_contract_test.clj")
           {:replace-existing true})
  (fs/copy (fs/path repo-root "bench/fixtures/selector_recovery/answer_schema.json")
           (fs/path result-root "answer_schema.json")
           {:replace-existing true}))

(defn wait-ready! [lane process ready-file stderr-file]
  (loop [attempt 0]
    (cond
      (fs/regular-file? ready-file)
      (let [ready (read-string (slurp ready-file))
            url (:url ready)]
        (checked ["curl" "--fail" "--silent" "--show-error"
                  (str (str/replace url #"/mcp$" "") "/healthz")]
                 {})
        url)

      (not (proc/alive? process))
      (throw (ex-info "MCP server exited before readiness"
                      {:lane lane :stderr (slurp stderr-file)}))

      (>= attempt 240)
      (throw (ex-info "MCP server did not become ready" {:lane lane}))

      :else
      (do (Thread/sleep 250) (recur (inc attempt))))))

(defn start-server! [lane root]
  (let [ready-file (str (fs/path result-root (str (name lane) "-ready.edn")))
        stdout-file (str (fs/path result-root (str (name lane) "-server.stdout")))
        stderr-file (str (fs/path result-root (str (name lane) "-server.stderr")))
        command ["clojure" "-J-Xms64m" "-J-Xmx512m" "-X:clj-surgeon/mcp"
                 ":project-dir" (pr-str corpus)
                 ":telemetry" ":metrics"
                 ":telemetry-dir" (pr-str (str (fs/path result-root (str (name lane) "-telemetry"))))
                 ":run-id" (pr-str (str "selector-continuation-" (name lane)))
                 ":nrepl-port" ":none"
                 ":port" (str port)
                 ":ready-file" (pr-str ready-file)]
        out (io/writer stdout-file)
        err (io/writer stderr-file)
        process (proc/process command {:dir root :out out :err err})
        url (wait-ready! lane process ready-file stderr-file)]
    {:process process :out out :err err :url url}))

(defn stop-server! [{:keys [process out err]}]
  (when process
    (proc/destroy-tree process)
    (try @process (catch Exception _)))
  (when out (.close out))
  (when err (.close err)))

(defn initial-request []
  {"workspace_root" corpus
   "requests"
   [{"id" (:completed-id task)
     "operation" "forms"
     "file" "src/clj_surgeon/mcp_paths.clj"
     "forms" ["resolve-source-path"]
     "expect" {"forms" 1}}
    {"id" (:pending-id task)
     "operation" "forms"
     "file" "test/clj_surgeon/mcp_contract_test.clj"
     "forms" [(:requested-owner task)]
     "expect" {"forms" 1}}]
   "expect" {"requests" 2 "files" 2}})

(defn prompt-text []
  (str
    "This is a read-only Clojure selector-continuation task. Do not modify files.\n\n"
    "First call inspect_clojure exactly once with this supplied two-request batch. "
    "Keep the request order and the supplied owners even if one owner may be wrong:\n"
    (json/generate-string (initial-request) {:pretty true})
    "\n\nThe second selector is intended to retrieve the existing test that proves a "
    "top-level insertion can be validated relative to its named owner without repeating "
    "the complete owner source. If the first call refuses, recover that exact existing "
    "owner from the read-only response evidence; do not guess.\n\n"
    (if (= prompt-style :retry-template)
      (str "If refused, follow the response's final retry instruction exactly. If retry_template "
           "is present, copy retry_template.arguments, replace only its null selector hole with "
           "one exact listed owner, and call inspect_clojure once. Do not reconstruct fields from "
           "the original request. If no retry_template is present, retry the complete original "
           "batch with only the wrong owner corrected. ")
      (str "If the refusal exposes continuation.completed_results and continuation.snapshot_guards, "
           "preserve the completed result and retry only continuation.pending_request_ids with the "
           "corrected owner and every supplied snapshot guard. If no continuation is exposed, retry "
           "the complete original batch with only the wrong owner corrected. "))
    "Use native search only "
    "if the refusal evidence is insufficient. Do not outline, read whole files, use Git history, "
    "use clj-surgeon CLI, or access anything outside this workspace. Stop after read_complete=true.\n\n"
    "Return only JSON matching the required schema. Set task_id to "
    (:task-id task) ", selected_owner to the corrected exact owner, and read_complete to true."))

(defn write-config! [codex-home url]
  (spit (str (fs/path codex-home "config.toml"))
        (str "[mcp_servers.clj-surgeon]\n"
             "url = \"" url "\"\n"
             "required = true\n"
             "enabled_tools = [\"inspect_clojure\"]\n"
             "default_tools_approval_mode = \"writes\"\n"
             "startup_timeout_sec = 5\n"
             "tool_timeout_sec = 45\n")))

(defn read-events [file]
  (->> (str/split-lines (slurp file))
       (remove str/blank?)
       (mapv #(json/parse-string % true))))

(defn event? [type item-type event]
  (and (= type (:type event)) (= item-type (get-in event [:item :type]))))

(defn mcp-event? [type event]
  (and (event? type "mcp_tool_call" event)
       (= "inspect_clojure" (get-in event [:item :tool]))))

(defn nested [m & paths]
  (some #(get-in m %) paths))

(defn structured-result [event]
  (nested event
          [:item :result :structured_content]
          [:item :result :structuredContent]))

(defn request-ids [event]
  (mapv :id (get-in event [:item :arguments :requests])))

(defn owner-for-id [event request-id]
  (some (fn [request]
          (when (= request-id (:id request))
            (get-in request [:forms 0])))
        (get-in event [:item :arguments :requests])))

(defn source-by-id [structured request-id]
  (let [result (some #(when (= request-id (:id %)) %)
                     (or (:results structured)
                         (get-in structured [:continuation :completed_results])))]
    (or (get-in result [:forms 0 :source]) "")))

(defn utf8-length [value]
  (count (.getBytes (str value) "UTF-8")))

(defn run-cell! [lane url]
  (let [run-dir (str (fs/path result-root (name lane)))
        codex-home (str (fs/path run-dir "codex-home"))
        events-file (str (fs/path run-dir "events.jsonl"))
        stderr-file (str (fs/path run-dir "stderr.txt"))]
    (fs/create-dirs codex-home)
    (fs/create-sym-link (fs/path codex-home "auth.json") auth-file)
    (write-config! codex-home url)
    (spit (str (fs/path run-dir "prompt.md")) (prompt-text))
    (let [command ["codex" "exec" "--json" "--ephemeral" "--ignore-rules"
                   "--skip-git-repo-check" "--sandbox" "read-only" "--color" "never"
                   "--output-schema" (str (fs/path result-root "answer_schema.json"))
                   "-m" model "-c" (str "model_reasoning_effort=\"" reasoning "\"")
                   "-C" corpus (prompt-text)]
          start (now-ms)
          process (proc/process command
                                {:dir corpus :in "" :out :pipe :err (io/file stderr-file)
                                 :extra-env {"PATH" "/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"
                                             "CODEX_HOME" codex-home}})]
      (with-open [reader (io/reader (:out process))
                  writer (io/writer events-file)]
        (doseq [line (line-seq reader)]
          (let [event (assoc (json/parse-string line true) :captured_ms (now-ms))]
            (.write writer (json/generate-string event))
            (.write writer "\n")
            (.flush writer))))
      (let [{:keys [exit]} @process
            events (read-events events-file)
            started (filterv #(mcp-event? "item.started" %) events)
            completed (filterv #(mcp-event? "item.completed" %) events)
            first-result (some-> (first completed) structured-result)
            second-result (some-> (second completed) structured-result)
            first-ids (some-> (first started) request-ids)
            second-ids (some-> (second started) request-ids)
            expected-second-ids (if (= lane :post)
                                  [(:pending-id task)]
                                  [(:completed-id task) (:pending-id task)])
            second-owner (owner-for-id (second started) (:pending-id task))
            agent-text (or (some->> events
                                    (filter #(event? "item.completed" "agent_message" %))
                                    last :item :text)
                           "")
            final-value (try (json/parse-string agent-text true) (catch Exception _ nil))
            continuation (get first-result :continuation)
            repeated? (some #{(:completed-id task)} second-ids)
            completed-source (if continuation
                               (source-by-id first-result (:completed-id task))
                               (source-by-id second-result (:completed-id task)))
            correct
            (and (zero? exit)
                 (= 2 (count started))
                 (= [(:completed-id task) (:pending-id task)] first-ids)
                 (= expected-second-ids second-ids)
                 (= (:expected-owner task) second-owner)
                 (true? (:read_complete second-result))
                 (= (:task-id task) (:task_id final-value))
                 (= (:expected-owner task) (:selected_owner final-value))
                 (true? (:read_complete final-value))
                 (if (= lane :post)
                   (and (= [(:completed-id task)] (:completed_request_ids continuation))
                        (= [(:pending-id task)] (:pending_request_ids continuation))
                        (= (:snapshot_guards continuation)
                           (get-in (second started) [:item :arguments :snapshot_guards])))
                   (nil? continuation)))]
        (spit (str (fs/path run-dir "final.json")) agent-text)
        {:lane (name lane)
         :correct correct
         :wall_ms (- (now-ms) start)
         :mcp_calls (count started)
         :native_fallback_calls (count (filter #(event? "item.started" "command_execution" %) events))
         :first_request_ids first-ids
         :second_request_ids second-ids
         :continuation_exposed (boolean continuation)
         :repeated_request_count (if repeated? 1 0)
         :repeated_source_bytes (if repeated? (utf8-length completed-source) 0)
         :completed_source_bytes (utf8-length completed-source)
         :result_bytes (reduce + 0 (map #(utf8-length (json/generate-string (get-in % [:item :result]))) completed))
         :exit_code exit}))))

(defn run-lane! [lane root]
  (let [server (start-server! lane root)]
    (try
      (run-cell! lane (:url server))
      (finally
        (stop-server! server)
        (Thread/sleep 500)))))

(defn sha256 [file]
  (:out (checked ["shasum" "-a" "256" file] {})))

(defn write-receipt! [rows]
  (let [receipt {:experiment "selector-continuation-counterfactual"
                 :pre_commit pre-commit
                 :post_commit post-commit
                 :model model
                 :reasoning reasoning
                 :corpus corpus
                 :rows rows}
        path (str (fs/path result-root "receipt.json"))]
    (spit path (json/generate-string receipt {:pretty true}))
    (spit (str (fs/path result-root "receipt.sha256")) (str (sha256 path) "\n"))
    receipt))

(defn self-test! []
  (prepare-corpus!)
  (assert (= [(:completed-id task) (:pending-id task)]
             (mapv #(get % "id") (get (initial-request) "requests"))))
  (json/parse-string (slurp (str (fs/path result-root "answer_schema.json"))))
  (println "selector continuation benchmark self-test passed:" result-root))

(defn run! []
  (ensure-inputs!)
  (when-not (= #{:pre :post} (set lane-order))
    (throw (ex-info "BENCH_ORDER must contain pre and post exactly once"
                    {:order lane-order})))
  (prepare-corpus!)
  (let [roots {:pre pre-root :post post-root}
        rows (mapv #(run-lane! % (roots %)) lane-order)
        receipt (write-receipt! rows)]
    (println (json/generate-string receipt {:pretty true}))
    (println "Raw benchmark directory:" result-root)))

(fs/create-dirs result-root)
(case (first *command-line-args*)
  "--self-test" (self-test!)
  (run!))
