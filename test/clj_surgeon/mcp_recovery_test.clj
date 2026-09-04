(ns ^{:lane :fast} clj-surgeon.mcp-recovery-test
  (:require
   [clj-surgeon.mcp-recovery :as recovery]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]))

(deftest open-session-requires-a-real-session-id
  (let [calls (atom [])
        sender (fn [request payload]
                 (swap! calls conj [request payload])
                 (if (= "initialize" (:method payload))
                   {:status 200 :session-id "session-1" :body {:result {}}}
                   {:status 202}))]
    (is (= "session-1" (:session-id (recovery/open-session! "http://mcp" sender))))
    (is (= ["initialize" "notifications/initialized"]
           (mapv (comp :method second) @calls)))))

(deftest request-turns-expired-sessions-into-a-typed-immediate-failure
  (let [session {:url "http://mcp" :session-id "stale" :next-id (atom 1)}]
    (try
      (recovery/request! session "tools/list" {}
                         (fn [& _] {:status 404 :body nil}))
      (is false "expected an expired-session refusal")
      (catch Exception error
        (is (= :invalid-mcp-session (:error-type (ex-data error))))
        (is (= 404 (:status (ex-data error))))))))

(deftest response-parsing-accepts-json-and-streamable-http-events
  (let [parse-response-body
        (ns-resolve 'clj-surgeon.mcp-recovery 'parse-response-body)]
    (is (= {:result {:ok true}}
           (parse-response-body "{\"result\":{\"ok\":true}}")))
    (is (= {:result {:ok true}}
           (parse-response-body
             "id: 17\nevent: message\ndata: {\"result\":{\"ok\":true}}\n\n")))))

(deftest probe-proves-catalog-semantics-and-a-guarded-write
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "clj-surgeon-recovery-probe"
                 (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file root "src" "sample" "core.clj")
        calls (atom [])
        open-session #(hash-map :kind (if (= % "surgeon") :surgeon :cclsp))
        request (fn [_ method _]
                  (is (= "tools/list" method))
                  {:tools [{:name "inspect_clojure"}
                           {:name "apply_clojure_changes"}]})
        tool-call
        (fn [session tool arguments]
          (swap! calls conj [(:kind session) tool arguments])
          (cond
            (= tool "resolve_var_surface")
            {:structuredContent {:ok true
                                 :lsp_session "lsp-1"
                                 :definitions [{:owner "target"}]
                                 :references []}}

            (= tool "apply_clojure_changes")
            {:structuredContent {:ok true :verification_complete true}}

            (= "outline" (get-in arguments [:requests 0 :operation]))
            {:structuredContent
             {:ok true
              :results [{:outline {:ns "sample.core"
                                   :forms [{:name "target" :type "defn"}]}}]}}

            :else
            {:structuredContent
             {:ok true
              :results [{:forms [{:source_anchor
                                  {:file "src/sample/core.clj"
                                   :source_sha256 "abc"
                                   :owner "target"}}]}]}}))]
    (try
      (.mkdirs (.getParentFile source))
      (spit source "(ns sample.core)\n(defn target [] :ok)\n")
      (let [result (recovery/probe! {:workspace (.getPath root)
                                     :surgeon-url "surgeon"
                                     :cclsp-url "cclsp"
                                     :open-session open-session
                                     :request request
                                     :tool-call tool-call})]
        (is (:ok result))
        (is (= "sample.core/target" (get-in result [:semantic :subject])))
        (is (= "lsp-1" (get-in result [:semantic :lsp-session])))
        (is (true? (get-in result [:mutation :verification-complete])))
        (is (false? (.exists (io/file root ".clj-surgeon-recovery"))))
        (is (= ["inspect_clojure" "inspect_clojure"
                "resolve_var_surface" "apply_clojure_changes"]
               (mapv second @calls))))
      (finally
        (doseq [file (reverse (file-seq root))]
          (.delete file))))))

(deftest semantic-refusal-retains-an-executable-exact-source-fallback
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "clj-surgeon-semantic-fallback"
                 (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file root "src" "sample" "core.clj")
        tool-call
        (fn [_session tool arguments]
          (cond
            (= tool "resolve_var_surface")
            {:structuredContent {:ok false
                                 :status "warming"
                                 :error_type "semantic-provider-warming"}}

            (= "outline" (get-in arguments [:requests 0 :operation]))
            {:structuredContent
             {:ok true
              :results [{:outline {:ns "sample.core"
                                   :forms [{:name "target" :type "defn"}]}}]}}

            :else
            {:structuredContent
             {:ok true
              :results [{:forms [{:source_anchor
                                  {:file "src/sample/core.clj"
                                   :source_sha256 "abc"
                                   :owner "target"}}]}]}}))]
    (try
      (.mkdirs (.getParentFile source))
      (spit source "(ns sample.core)\n(defn target [] :ok)\n")
      (let [error (try
                    (recovery/probe!
                      {:workspace (.getPath root)
                       :surgeon-url "surgeon"
                       :cclsp-url "cclsp"
                       :open-session (fn [url] {:url url})
                       :request (fn [_ _ _]
                                  {:tools [{:name "inspect_clojure"}
                                           {:name "apply_clojure_changes"}]})
                       :tool-call tool-call})
                    nil
                    (catch clojure.lang.ExceptionInfo failure failure))
            data (ex-data error)]
        (is (= :semantic-witness (:phase data)))
        (is (= "semantic-provider-warming" (:error-type data)))
        (is (= :exact-source (:safe-route data)))
        (is (= {:structural-read :ready
                :structural-write :ready
                :semantic-surface :warming
                :source-anchor :retained}
               (:capabilities data)))
        (is (= ["clj-surgeon" ":op" ":cat"
                ":file" "src/sample/core.clj" ":form" "target"]
               (:fallback-command data))))
      (finally
        (doseq [file (reverse (file-seq root))]
          (.delete file))))))

(deftest semantic-witness-outlives-one-bounded-cclsp-child-recovery
  ;; Field regression: the SMW worktree's cclsp request completed in 21.683 s
  ;; after one child recovery, but the outer recovery client expired at 10 s.
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "clj-surgeon-recovery-timeout"
                 (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file root "src" "sample" "core.clj")
        semantic-sessions (atom [])
        open-session #(hash-map :kind (if (= % "surgeon") :surgeon :cclsp))
        request (fn [_ _ _]
                  {:tools [{:name "inspect_clojure"}
                           {:name "apply_clojure_changes"}]})
        tool-call
        (fn [session tool arguments]
          (cond
            (= tool "resolve_var_surface")
            (do
              (swap! semantic-sessions conj session)
              (when (< (long (or (:timeout-ms session) 0)) 60000)
                (throw (java.net.http.HttpTimeoutException. "request timed out")))
              {:structuredContent {:status "ok"
                                   :lsp_session "lsp-after-recovery"
                                   :definition {:owner "target"}
                                   :references []}})

            (= tool "apply_clojure_changes")
            {:structuredContent {:ok true :verification_complete true}}

            (= "outline" (get-in arguments [:requests 0 :operation]))
            {:structuredContent
             {:ok true
              :results [{:outline {:ns "sample.core"
                                   :forms [{:name "target" :type "defn"}]}}]}}

            :else
            {:structuredContent
             {:ok true
              :results [{:forms [{:source_anchor
                                  {:file "src/sample/core.clj"
                                   :source_sha256 "abc"
                                   :owner "target"}}]}]}}))]
    (try
      (.mkdirs (.getParentFile source))
      (spit source "(ns sample.core)\n(defn target [] :ok)\n")
      (let [result (recovery/probe! {:workspace (.getPath root)
                                     :surgeon-url "surgeon"
                                     :cclsp-url "cclsp"
                                     :open-session open-session
                                     :request request
                                     :tool-call tool-call})]
        (is (:ok result))
        (is (= "lsp-after-recovery"
               (get-in result [:semantic :lsp-session])))
        (is (= [60000] (mapv :timeout-ms @semantic-sessions))))
      (finally
        (doseq [file (reverse (file-seq root))]
          (.delete file))))))
