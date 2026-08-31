(ns clj-surgeon.mcp-prepared-confirmation-test
  (:require
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-prepared-request :as prepared-request]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   (io.modelcontextprotocol.common McpTransportContext)
   (io.modelcontextprotocol.server McpAsyncServerExchange)))

(def digest-a (apply str (repeat 64 "a")))
(def digest-b (apply str (repeat 64 "b")))

(defn- load-api []
  (try
    (require 'clj-surgeon.mcp-prepared-confirmation)
    (ns-publics 'clj-surgeon.mcp-prepared-confirmation)
    (catch java.io.FileNotFoundException _ nil)))

(defn- call [api name & args]
  (apply (get api name) args))

(defmacro ^{:clj-kondo/lint-as 'clojure.core/let} with-confirmation-api
  [[api expression] & body]
  `(if-let [~api ~expression]
     (do ~@body)
     (is false "ratified prepared-confirmation production entrance is absent")))

(defn- descriptor
  ([] (descriptor "alpha" "(def alpha :old)"))
  ([owner source]
   {:tool "edit_clojure"
    :executable false
    :write_authority false
    :arguments {:workspace_root "/canonical/workspace"
                :edits [{:file "src/demo.clj"
                         :within {:form owner}
                         :from source
                         :to nil
                         :matches 1}]}
    :caller_holes ["arguments.edits[0].to"]}))

(defn- prepared-result
  ([] (prepared-result (descriptor)))
  ([value]
   {:ok true
    :operation "inspect_clojure"
    :prepared_request value
    :file_hashes {"src/demo.clj" digest-a}}))

(defn- registry [api clock & {:as opts}]
  (call api 'new-registry
        (merge {:clock #(long @clock)
                :boot-epoch "boot-A"
                :digest-fn prepared-request/descriptor-sha256}
               opts)))

(defn- refusal-shape? [value expected-type]
  (and (= false (:ok value))
       (= expected-type (:error_type value))
       (= true (:source_unchanged value))
       (= false (:mutation_attempted value))
       (= false (:write_authority value))
       (string? (:failed_stage value))
       (not-any? #(contains? value %)
                 [:source :replacement :selected_candidate :prepared_request
                  :next_call :receipt :inverse :terminal_response])))

;; @spec MCP-OP-PREP-ACT-001
(deftest confirmation-publication-is-budget-atomic-and-stateful
  (with-confirmation-api [api (load-api)]
    (let [clock (atom 0)
          store (registry api clock)
          prepared (prepared-result)
          overflow (call api 'attach-confirmation!
                         store "session-A" prepared (constantly 32769))]
      (is (identical? prepared overflow))
      (is (= 0 (:live-count (call api 'registry-stats store))))
      (let [surfaced (call api 'attach-confirmation!
                           store "session-A" prepared (constantly 32768))
            confirmation (:prepared_confirmation surfaced)]
        (is (= {:descriptor_sha256
                (prepared-request/descriptor-sha256 (descriptor))
                :expires_in_ms 300000
                :session_bound true
                :commit_single_use true
                :executable false
                :write_authority false}
               confirmation))
        (is (= true (:ok (call api 'lookup! store "session-A"
                               (:descriptor_sha256 confirmation)))))
        (is (refusal-shape?
              (call api 'lookup! (registry api clock) "session-A"
                    (:descriptor_sha256 confirmation))
              "prepared-confirmation-unknown"))))))

;; @spec MCP-OP-PREP-ACT-002
(deftest registry-is-bounded-expiring-deterministic-and-source-free
  (with-confirmation-api [api (load-api)]
    (let [clock (atom 0)
          store (registry api clock :per-session-live 2 :global-live 3
                          :per-session-tombstones 2 :global-tombstones 3)
          register (fn [session owner]
                     (call api 'register! store session
                           (descriptor owner (str "(def " owner " :old)"))
                           {"src/demo.clj" digest-a}))]
      (doseq [owner ["a" "b" "c"]]
        (swap! clock inc)
        (register "session-A" owner))
      (let [stats (call api 'registry-stats store)]
        (is (= 2 (:live-count stats)))
        (is (= 1 (:tombstone-count stats)))
        (is (empty? (set/intersection
                      #{:descriptor :descriptor-bytes :workspace-root :file-hashes
                        :fill :identity :source-hash}
                      (set (:tombstone-fields stats))))))
      (reset! clock 300003)
      (is (= 0 (:live-count (call api 'registry-stats store))))
      (call api 'end-session! store "session-A")
      (is (= {:live-count 0 :tombstone-count 0}
             (select-keys (call api 'registry-stats store)
                          [:live-count :tombstone-count]))))))

;; @spec MCP-OP-PREP-ACT-003
(deftest digest-collision-removes-both-and-disables-the-boot
  (with-confirmation-api [api (load-api)]
    (let [clock (atom 0)
          store (registry api clock :digest-fn (constantly digest-a))]
      (is (= true (:ok (call api 'register! store "session-A"
                             (descriptor "a" "(def a 1)")
                             {"src/demo.clj" digest-a}))))
      (let [collision (call api 'register! store "session-A"
                            (descriptor "b" "(def b 2)")
                            {"src/demo.clj" digest-a})]
        (is (refusal-shape? collision
                            "prepared-confirmation-hash-collision"))
        (is (= {:enabled false :live-count 0}
               (select-keys (call api 'registry-stats store)
                            [:enabled :live-count])))
        (call api 'reset-registry! store)
        (is (= false (:enabled (call api 'registry-stats store))))))))

;; @spec MCP-OP-PREP-ACT-004
(deftest sdk-session-key-is-the-only-join-and-cross-session-is-unknown
  (with-confirmation-api [api (load-api)]
    (let [clock (atom 0)
          store (registry api clock)
          exchange (McpAsyncServerExchange.
                     "sdk-session-A" nil nil nil McpTransportContext/EMPTY)
          registered (call api 'register! store "sdk-session-A"
                           (descriptor) {"src/demo.clj" digest-a})
          digest (:descriptor_sha256 registered)
          other (call api 'lookup! store "sdk-session-B" digest)
          never (call api 'lookup! store "sdk-session-B" digest-b)]
      (is (= "sdk-session-A" (call api 'exchange-session-key exchange)))
      (is (nil? (call api 'exchange-session-key nil)))
      (is (nil? (call api 'exchange-session-key
                      {:session_id "forged" :workspace_root "/canonical/workspace"})))
      (is (= other never))
      (is (refusal-shape? other "prepared-confirmation-unknown")))))

;; @spec MCP-OP-PREP-ACT-005
(deftest compact-shape-and-holes-are-exact-before-lookup
  (with-confirmation-api [api (load-api)]
    (let [valid {:confirm digest-a
                 :fill {"arguments.edits[0].to" "(def alpha :new)"}}
          invalids [(assoc valid :workspace_root "/forged")
                    (assoc valid :preview false)
                    (assoc valid :preview nil)
                    (assoc valid :confirm "ABC")
                    (assoc valid :fill {})
                    (assoc-in valid [:fill "arguments.edits[0].to"] " ")
                    (assoc-in valid [:fill "arguments.edits[0].to"] 7)
                    (assoc valid :preview_sha256 digest-b)]]
      (is (= true (:ok (call api 'validate-confirm-request valid))))
      (doseq [request invalids]
        (is (refusal-shape? (call api 'validate-confirm-request request)
                            "invalid-prepared-confirmation")))
      (let [mismatch (call api 'validate-holes
                           ["arguments.edits[0].to"]
                           {"unexpected" "(x)"})]
        (is (= ["arguments.edits[0].to"] (:expected mismatch)))
        (is (= ["unexpected"] (:provided mismatch)))
        (is (= ["arguments.edits[0].to"] (:missing mismatch)))
        (is (= ["unexpected"] (:extra mismatch)))))))

;; @spec MCP-OP-PREP-ACT-006
(deftest reconstruction-fills-only-declared-nulls-and-rechecks-all-files
  (with-confirmation-api [api (load-api)]
    (let [replacement "(def alpha :new)"
          rebuilt (call api 'reconstruct-arguments
                        (descriptor)
                        {"arguments.edits[0].to" replacement})]
      (is (= replacement (get-in rebuilt [:edits 0 :to])))
      (is (= "/canonical/workspace" (:workspace_root rebuilt)))
      (is (= true (:ok (contract/validate-tool-params
                         (dissoc rebuilt :workspace_root)))))
      (is (= true (:ok (call api 'validate-snapshot
                             {"src/a.clj" digest-a "src/b.clj" digest-b}
                             {"src/a.clj" digest-a "src/b.clj" digest-b}))))
      (is (refusal-shape?
            (call api 'validate-snapshot
                  {"src/a.clj" digest-a "src/b.clj" digest-b}
                  {"src/a.clj" digest-a "src/b.clj" digest-a})
            "prepared-confirmation-snapshot-drift")))))

;; @spec MCP-OP-PREP-ACT-007
(deftest commit-consumes-once-before-every-terminal-transaction-outcome
  (with-confirmation-api [api (load-api)]
    (let [clock (atom 0)
          store (registry api clock)
          registered (call api 'register! store "session-A" (descriptor)
                           {"src/demo.clj" digest-a})
          digest (:descriptor_sha256 registered)]
      (is (= true (:ok (call api 'consume! store "session-A" digest))))
      (is (refusal-shape? (call api 'lookup! store "session-A" digest)
                          "prepared-confirmation-consumed"))
      (is (= false (:mutation_succeeded
                     (call api 'lookup! store "session-A" digest false)))))))

;; @spec MCP-OP-PREP-ACT-008
(deftest refusal-vocabulary-is-closed-source-free-and-nonexecutable
  (with-confirmation-api [api (load-api)]
    (let [types ["invalid-prepared-confirmation"
                 "prepared-confirmation-unknown"
                 "prepared-confirmation-expired"
                 "prepared-confirmation-evicted"
                 "prepared-confirmation-consumed"
                 "prepared-confirmation-hash-collision"
                 "prepared-confirmation-hole-mismatch"
                 "prepared-confirmation-snapshot-drift"
                 "prepared-confirmation-preview-limit"]]
      (doseq [type types]
        (is (refusal-shape? (call api 'confirmation-refusal type "stage" {})
                            type))))))

;; @spec MCP-OP-PREP-ACT-009
(deftest preview-is-pure-compiler-only-with-throwing-effect-capabilities
  (with-confirmation-api [api (load-api)]
    (let [effects (atom [])
          thrower (fn [name]
                    (fn [& _]
                      (swap! effects conj name)
                      (throw (ex-info "effect reached" {:effect name}))))
          result (call api 'compile-preview
                       {:writer (thrower :writer)
                        :receipt (thrower :receipt)
                        :formatter (thrower :formatter)
                        :verifier (thrower :verifier)
                        :rollback (thrower :rollback)
                        :process (thrower :process)}
                       {:descriptor-sha256 digest-a
                        :fill {"arguments.edits[0].to" "(def alpha :new)"}
                        :snapshot-guards {"src/demo.clj" digest-a}
                        :sources {"src/demo.clj" "(def alpha :old)\n"}
                        :compile-fn
                        (fn [_]
                          {:ok true
                           :future-sources
                           {"src/demo.clj" "(def alpha :new)\n"}})})]
      (is (:ok result))
      (is (empty? @effects))
      (is (= false (:mutation_attempted result)))
      (is (= true (:source_unchanged result))))))

;; @spec MCP-OP-PREP-ACT-010
(deftest preview-success-has-one-closed-inert-complete-shape
  (with-confirmation-api [api (load-api)]
    (let [result (call api 'preview-result
                       {:descriptor-sha256 digest-a
                        :fill {"arguments.edits[0].to" "(def alpha :new)"}
                        :snapshot-guards {"src/demo.clj" digest-a}
                        :sources {"src/demo.clj" "(def alpha :old)\n"}
                        :future-sources
                        {"src/demo.clj" "(def alpha :new)\n"}})]
      (is (= #{:ok :operation :lifecycle :committed :mutation_attempted
               :write_authority :receipt :source_unchanged :descriptor_sha256
               :fill_sha256 :snapshot_guards :future_file_hashes :changed_files
               :changed_characters :diff :verification_forecast :preview_sha256
               :next_action}
             (set (keys result))))
      (is (= ["edit_clojure-preview" "preview" false false false false true]
             ((juxt :operation :lifecycle :committed :mutation_attempted
                    :write_authority :receipt :source_unchanged) result)))
      (is (str/includes? (:diff result) "(def alpha :new)"))
      (is (not-any? #(contains? result %)
                    [:inverse :receipt_path :verification_complete
                     :terminal_response :next_call :commit_token])))))

;; @spec MCP-OP-PREP-ACT-011
(deftest preview-bounds-are-exact-and-never-return-partial-diffs
  (with-confirmation-api [api (load-api)]
    (let [base {:ok true :diff "x"}
          bytes-16384 (apply str (repeat 16384 "x"))
          bytes-16385 (str bytes-16384 "x")
          lines-256 (str/join "\n" (repeat 256 "x"))
          lines-257 (str/join "\n" (repeat 257 "x"))]
      (is (:ok (call api 'enforce-preview-bounds
                     (assoc base :diff bytes-16384) (constantly 32768))))
      (is (= "prepared-preview-output-limit"
             (:error_type (call api 'enforce-preview-bounds
                                (assoc base :diff bytes-16385)
                                (constantly 1)))))
      (is (:ok (call api 'enforce-preview-bounds
                     (assoc base :diff lines-256) (constantly 1))))
      (is (= "prepared-preview-output-limit"
             (:error_type (call api 'enforce-preview-bounds
                                (assoc base :diff lines-257)
                                (constantly 1)))))
      (let [overflow (call api 'enforce-preview-bounds base (constantly 32769))]
        (is (= "prepared-preview-output-limit" (:error_type overflow)))
        (is (not (contains? overflow :diff)))))))

;; @spec MCP-OP-PREP-ACT-012
(deftest verification-forecast-is-the-exact-honest-no-verifier-fact
  (with-confirmation-api [api (load-api)]
    (is (= {:will_run false
            :profile nil
            :reason "edit_clojure-does-not-authorize-transaction-verification"}
           (call api 'verification-forecast)))))

;; @spec MCP-OP-PREP-ACT-013
(deftest preview-never-authorizes-commit-refreshes-ttl-or-exceeds-three-uses
  (with-confirmation-api [api (load-api)]
    (let [clock (atom 0)
          store (registry api clock)
          registered (call api 'register! store "session-A" (descriptor)
                           {"src/demo.clj" digest-a})
          digest (:descriptor_sha256 registered)]
      (doseq [expected [1 2 3]]
        (is (= expected (:preview_count
                          (call api 'use-preview! store "session-A" digest)))))
      (is (refusal-shape? (call api 'use-preview! store "session-A" digest)
                          "prepared-confirmation-preview-limit"))
      (is (= 300000 (:expires_at
                      (call api 'lookup! store "session-A" digest))))
      (is (refusal-shape?
            (call api 'validate-confirm-request
                  {:confirm digest :fill {"arguments.edits[0].to" "(x)"}
                   :preview_sha256 digest-b})
            "invalid-prepared-confirmation")))))

;; @spec MCP-OP-PREP-ACT-014
(deftest unsupported-and-ineligible-siblings-are-identical-and-classic-route-stays-valid
  (with-confirmation-api [api (load-api)]
    (let [clock (atom 0)
          store (registry api clock)
          prepared (prepared-result)
          unsupported (call api 'attach-confirmation!
                            store nil prepared (constantly 1))
          ineligible {:ok false :operation "inspect_clojure"}]
      (is (identical? prepared unsupported))
      (is (identical? ineligible
                      (call api 'attach-confirmation!
                            store "session-A" ineligible (constantly 1))))
      (is (= true (:ok (contract/validate-tool-params
                         (dissoc
                           (call api 'reconstruct-arguments
                                 (:prepared_request prepared)
                                 {"arguments.edits[0].to"
                                  "(def alpha :new)"})
                           :workspace_root)))))
      (is (= 0 (:live-count (call api 'registry-stats store)))))))

;; @spec MCP-OP-PREP-ACT-015
(deftest coaching-is-byte-identical-and-telemetry-is-strictly-allowlisted
  (with-confirmation-api [api (load-api)]
    (let [expected (str "If you independently decide to edit these exact selections, fill the "
                        "null replacement at every path listed in `caller_holes`. Then submit "
                        "`prepared_request.arguments` once to `edit_clojure`. Otherwise, ignore "
                        "`prepared_request`.")
          hostile {:digest digest-a :root "/secret" :file "src/secret.clj"
                   :owner "pwn" :fill "replacement" :diff "secret"
                   :session-key "sdk-secret" :preview-count 2
                   :refusal-class "expired" :request-bytes 10}]
      (is (= expected prepared-request/coaching-text))
      (is (= {:digest digest-a :preview-count 2
              :refusal-class "expired" :request-bytes 10}
             (call api 'telemetry-fields hostile))))))

;; @spec MCP-OP-PREP-ACT-016
(deftest promotion-claims-remain-projected-until-complete-measurement
  (with-confirmation-api [api (load-api)]
    (is (= {:w1 "projected" :w2 "unpromoted" :install_authorized false}
           (call api 'promotion-status {})))
    (is (= "projected" (:w1 (call api 'promotion-status
                                  {:smaller-request true}))))))

;; @spec MCP-OP-PREP-ACT-017
(deftest clock-digest-boot-session-capacity-and-effect-seams-are-injected
  (with-confirmation-api [api (load-api)]
    (let [clock (atom 41)
          digests (atom [])
          store (registry api clock
                          :digest-fn (fn [value]
                                       (swap! digests conj value)
                                       digest-b)
                          :per-session-live 1 :global-live 1)
          result (call api 'register! store "injected-session"
                       (descriptor) {"src/demo.clj" digest-a})]
      (is (= digest-b (:descriptor_sha256 result)))
      (is (= 1 (count @digests)))
      (is (= {:boot-epoch "boot-A" :now 41
              :per-session-live 1 :global-live 1}
             (select-keys (call api 'registry-stats store)
                          [:boot-epoch :now :per-session-live :global-live]))))))

;; @spec MCP-OP-PREP-ACT-018
(deftest implementation-never-authorizes-install-or-shared-runtime-publication
  (with-confirmation-api [api (load-api)]
    (is (= false (:install_authorized
                   (call api 'promotion-status
                         {:frozen-red true :implementation true
                          :surgeon2-verification false :measurement false
                          :gene-install-approval false}))))
    (is (= false (:install_authorized
                   (call api 'promotion-status
                         {:frozen-red true :implementation true
                          :surgeon2-verification true :measurement true
                          :gene-install-approval false}))))))

(defn- delete-tree! [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (.delete child))))

(defn- invoke-handler [handler exchange params]
  (let [result (promise)]
    (handler exchange params
             (fn [content error? structured]
               (deliver result {:content content
                                :error? error?
                                :structured structured})))
    (deref result 10000 {:timeout true})))

(deftest real-handler-preview-stales-and-fresh-confirmation-commits-once
  ;; Real-program-derived minimized owner form from the prepared-request route.
  (let [root-path (java.nio.file.Files/createTempDirectory
                    "prepared-confirm-integration-"
                    (make-array java.nio.file.attribute.FileAttribute 0))
        root (.toFile root-path)
        source-file (io/file root "src/demo.clj")
        receipt-dir (io/file root "receipts")
        exchange-a (McpAsyncServerExchange.
                     "sdk-session-A" nil nil nil McpTransportContext/EMPTY)
        exchange-b (McpAsyncServerExchange.
                     "sdk-session-B" nil nil nil McpTransportContext/EMPTY)
        inspect-request
        {:requests [{:id "forms"
                     :operation "forms"
                     :file "src/demo.clj"
                     :forms ["alpha"]
                     :expect {:forms 1}}]
         :expect {:requests 1 :files 1}}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file "(ns demo)\n(def alpha :old)\n")
      (mcp-tool/init! {:project-root (.getPath root)
                       :receipt-dir (.getPath receipt-dir)})
      (let [inspected (invoke-handler inspect-tool/handle-inspect
                                      exchange-a inspect-request)
            confirmation (get-in inspected [:structured :prepared_confirmation])
            digest (:descriptor_sha256 confirmation)
            fill {"arguments.edits[0].to" "(def alpha :new)"}
            compact {:confirm digest :fill fill}
            cross-session (invoke-handler mcp-tool/handle-edit-clojure
                                          exchange-b compact)
            preview (invoke-handler mcp-tool/handle-edit-clojure
                                    exchange-a (assoc compact :preview true))]
        (is (re-matches #"[0-9a-f]{64}" digest))
        (is (= "prepared-confirmation-unknown"
               (get-in cross-session [:structured :error_type])))
        (is (= "edit_clojure-preview"
               (get-in preview [:structured :operation])))
        (is (= "(ns demo)\n(def alpha :old)\n" (slurp source-file)))
        (spit source-file "(ns demo)\n\n(def alpha :old)\n")
        (let [stale (invoke-handler mcp-tool/handle-edit-clojure
                                    exchange-a compact)]
          (is (= "prepared-confirmation-snapshot-drift"
                 (get-in stale [:structured :error_type])))
          (is (= "(ns demo)\n\n(def alpha :old)\n" (slurp source-file))))
        (let [fresh-inspected (invoke-handler inspect-tool/handle-inspect
                                              exchange-a inspect-request)
              fresh-digest (get-in fresh-inspected
                                   [:structured :prepared_confirmation
                                    :descriptor_sha256])
              fresh {:confirm fresh-digest :fill fill}
              committed (invoke-handler mcp-tool/handle-edit-clojure
                                        exchange-a fresh)
              replay (invoke-handler mcp-tool/handle-edit-clojure
                                     exchange-a fresh)]
          (is (= true (get-in committed [:structured :ok])))
          (is (= "(ns demo)\n\n(def alpha :new)\n" (slurp source-file)))
          (is (= "prepared-confirmation-consumed"
                 (get-in replay [:structured :error_type])))))
      (finally
        (mcp-tool/init! nil)
        (delete-tree! root)))))
