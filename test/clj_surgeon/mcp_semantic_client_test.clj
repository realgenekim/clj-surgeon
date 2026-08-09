(ns clj-surgeon.mcp-semantic-client-test
  (:require
   [clj-surgeon.mcp-semantic-client :as semantic]
   [clojure.test :refer [deftest is]]))

(deftest init-preserves-a-hot-client-at-the-same-url
  (let [sentinel (Object.)]
    (reset! semantic/runtime {:url "http://127.0.0.1:7890/mcp" :client sentinel})
    (is (= {:url "http://127.0.0.1:7890/mcp" :connected true}
           (semantic/init! {:url "http://127.0.0.1:7890/mcp"})))
    (is (identical? sentinel (:client @semantic/runtime)))))

(deftest init-discards-a-client-when-the-provider-url-changes
  (let [closed? (atom false)
        fake-client (reify java.lang.AutoCloseable
                      (close [_] (reset! closed? true)))]
    (reset! semantic/runtime {:url "http://127.0.0.1:7890/mcp" :client fake-client})
    (with-redefs [semantic/close! #(do (reset! closed? true)
                                       (swap! semantic/runtime assoc :client nil))]
      (is (= {:url "http://127.0.0.1:7891/mcp" :connected false}
             (semantic/init! {:url "http://127.0.0.1:7891/mcp"}))))
    (is @closed?)))

(deftest invalid-session-errors-are-distinguished-from-semantic-refusals
  (let [invalid-session-error?
        (ns-resolve 'clj-surgeon.mcp-semantic-client
                    'invalid-session-error?)]
    (is (true? (invalid-session-error?
                 "invalid-mcp-session: reconnect, then retry the same request")))
    (is (false? (invalid-session-error?
                  "No definition found for sample.core/missing")))))

(deftest resolve-var-reconnects-exactly-once-after-a-typed-expired-session
  (let [client-var (ns-resolve 'clj-surgeon.mcp-semantic-client 'client!)
        call-var (ns-resolve 'clj-surgeon.mcp-semantic-client
                             'call-resolve-var!)
        clients (atom [:stale-client :fresh-client])
        calls (atom [])
        closes (atom 0)]
    (with-redefs-fn
      {client-var #(let [client (first @clients)]
                     (swap! clients rest)
                     client)
       call-var (fn [client workspace-root qualified-var source-anchor]
                  (swap! calls conj [client workspace-root qualified-var source-anchor])
                  (if (= client :stale-client)
                    {:ok false
                     :error-type :invalid-mcp-session
                     :error "invalid-mcp-session"}
                    {:ok true :definition {:owner "target"}}))
       #'semantic/close! #(swap! closes inc)}
      #(is (= {:ok true :definition {:owner "target"}}
              (semantic/resolve-var! "/workspace"
                                     "sample.core/target"
                                     {:source_sha256 "abc"}))))
    (is (= 1 @closes))
    (is (= [[:stale-client "/workspace" "sample.core/target"
             {:source_sha256 "abc"}]
            [:fresh-client "/workspace" "sample.core/target"
             {:source_sha256 "abc"}]]
           @calls))))
