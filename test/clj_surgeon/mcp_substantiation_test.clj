(ns clj-surgeon.mcp-substantiation-test
  (:require
   [clojure.test :refer [deftest is testing]]))

(defn- invoke-or-empty
  [symbol & args]
  (try
    (if-let [f (requiring-resolve symbol)]
      (apply f args)
      {})
    (catch java.io.FileNotFoundException _
      {})))

(deftest closed-chained-event-envelope
  (let [event (invoke-or-empty
                'clj-surgeon.mcp-substantiation/close-event
                {:sequence 7
                 :previous-event-sha256 (apply str (repeat 64 "a"))
                 :call-id "call-7"
                 :event {:kind :call-start}})]
    (is (= "clj-surgeon.substantiation-event.v1" (:schema event)))
    (is (= 7 (:sequence event)))
    (is (= (apply str (repeat 64 "a")) (:previous-event-sha256 event)))
    (is (re-matches #"[0-9a-f]{64}" (or (:event-sha256 event) "")))
    (is (= "call-7" (:call-id event)))))

(deftest privacy-tokens-preserve-equality-without-content
  (let [evidence (invoke-or-empty
                   'clj-surgeon.mcp-substantiation/privacy-evidence
                   {:secret (.getBytes "session-secret" "UTF-8")
                    :key-id "session-key-1"
                    :subjects ["src/secret.clj" "src/secret.clj" "src/other.clj"]})]
    (is (true? (:equal-inputs-equal evidence)))
    (is (true? (:different-inputs-different evidence)))
    (is (false? (:contains-raw-subject evidence)))
    (is (false? (:contains-plain-digest evidence)))
    (is (= "session-key-1" (:key-id evidence)))))

(deftest segment-is-private-new-and-append-only
  (let [evidence (invoke-or-empty
                   'clj-surgeon.mcp-substantiation/segment-evidence
                   {:first-event {:kind :first}
                    :second-event {:kind :second}})]
    (is (true? (:exclusive-create evidence)))
    (is (= "rw-------" (:permissions evidence)))
    (is (true? (:append-preserved-prefix evidence)))
    (is (true? (:rewrite-refused evidence)))
    (is (true? (:active-retention-refused evidence)))))

(deftest ledger-write-failures-are-loud-and-gap-visible
  (let [evidence (invoke-or-empty
                   'clj-surgeon.mcp-substantiation/write-failure-evidence)]
    (is (true? (:start-failure-blocked-execution evidence)))
    (is (true? (:finish-failure-preserved-result evidence)))
    (is (true? (:unhealthy-latched evidence)))
    (is (true? (:alarm-emitted evidence)))
    (is (true? (:next-call-blocked evidence)))))

(deftest caller-identity-comes-only-from-exchange
  (let [identity (invoke-or-empty
                   'clj-surgeon.mcp-substantiation/caller-identity
                   {:session-id "session-1"
                    :client-name "codex"
                    :client-version "1.2.3"
                    :request {:caller-model "forged-model"
                              :caller-model-source "forged"}})]
    (is (string? (:session-token identity)))
    (is (= "codex" (:client-name identity)))
    (is (= "1.2.3" (:client-version identity)))
    (is (= "unknown" (:caller-model identity)))
    (is (= "not-exposed" (:caller-model-source identity)))))

(deftest all-public-tools-project-closed-call-shapes
  (let [evidence (invoke-or-empty
                   'clj-surgeon.mcp-substantiation/public-tool-evidence
                   [:inspect-clojure :edit-clojure
                    :apply-clojure-changes :transform-clojure])]
    (is (true? (:inspect-observed evidence)))
    (is (true? (:edit-observed evidence)))
    (is (true? (:apply-observed evidence)))
    (is (true? (:transform-observed evidence)))
    (is (true? (:public-results-identical evidence)))))

(deftest read-normalization-stages-are-exact
  (let [facts (invoke-or-empty
                'clj-surgeon.mcp-substantiation/read-normalization-facts
                {:requests [{:file "src/demo.clj" :forms ["alpha"]}
                            {:file "src/demo.clj" :forms ["beta"]}]})]
    (is (= 2 (:omitted-operation-count facts)))
    (is (= 2 (:omitted-id-count facts)))
    (is (= 2 (:generated-id-count facts)))
    (is (= :mixed-request-ids (:mixed-refusal facts)))
    (is (true? (:explicit-control-preserved facts)))))

(deftest prepared-request-lifecycle-requires-exact-skeleton
  (let [facts (invoke-or-empty
                'clj-surgeon.mcp-substantiation/prepared-request-facts
                {:emitted-skeleton {:edits [{:file "src/demo.clj"
                                             :from "old"
                                             :to nil}]}
                 :consumed-request {:edits [{:file "src/demo.clj"
                                             :from "old"
                                             :to "new"}]}})]
    (is (= 1 (:emitted facts)))
    (is (= 1 (:exact-consumed facts)))
    (is (= 1 (:changed-shape-refused facts)))
    (is (= 1 (:committed facts)))
    (is (= 0 (:failed-committed facts)))))

(deftest write-refusal-counters-and-continuation-are-inert
  (let [facts (invoke-or-empty
                'clj-surgeon.mcp-substantiation/write-refusal-facts
                {:result {:ok false
                          :error :expect-count-mismatch
                          :evidence {:rows [{:owner "alpha"}
                                            {:owner "beta"}]
                                     :continuation {:offset 2}}}})]
    (is (= 1 (:firings facts)))
    (is (= 2 (:row-count facts)))
    (is (= 0 (:omitted-row-count facts)))
    (is (= 1 (:continuation-count facts)))
    (is (true? (:inert facts)))))

(deftest recovery-chain-classification-honors-bounds
  (let [facts (invoke-or-empty
                'clj-surgeon.mcp-substantiation/recovery-classification
                {:max-completed-calls 7
                 :max-elapsed-ms 600000})]
    (is (= :same-file-reread (:same-file-reread facts)))
    (is (= :direct-corrected-retry (:direct-corrected-retry facts)))
    (is (= :other (:other facts)))
    (is (= :included (:seventh-call facts)))
    (is (= :included (:ten-minute-edge facts)))))

(deftest classifier-episode-projection-is-complete-or-explicitly-unknown
  (let [episode (invoke-or-empty
                  'clj-surgeon.mcp-substantiation/classifier-episode
                  {:refusal {:owner-tokens ["o1"]
                             :location-rows 1
                             :candidate-cap-reached false}
                   :recovery-read {:semantic-kinds [:forms]}})]
    (is (= ["o1"] (get-in episode [:refusal :owner-names])))
    (is (= 1 (get-in episode [:refusal :location-rows])))
    (is (= [:forms] (get-in episode [:recovery-read :semantic-kinds])))
    (is (contains? (get episode :recovery-read {}) :duplicate-groups))
    (is (= "unknown" (:caller-model episode)))))

(deftest feature-envelope-admits-elaborator-without-schema-change
  (let [facts (invoke-or-empty
                'clj-surgeon.mcp-substantiation/feature-envelope-facts
                [{:feature "prepared_request.emitted" :count 1}
                 {:feature "elaborator.receipt" :count 1}
                 {:feature "future.unknown" :count 1}])]
    (is (true? (:common-shape facts)))
    (is (= 2 (:registered-count facts)))
    (is (= 1 (:unknown-retained-count facts)))
    (is (= 1 (:unknown-excluded-from-claims facts)))
    (is (true? (:elaborator-accepted facts)))))

(deftest marker-and-report-refuse-claims-upgrades
  (let [report (invoke-or-empty
                 'clj-surgeon.mcp-substantiation/substantiation-report
                 {:marker {:name "pre-install"
                           :sha256 (apply str (repeat 64 "b"))}
                  :events []
                  :projection-rate-ms-per-byte 3.5237})]
    (is (= (apply str (repeat 64 "b")) (:marker-sha256 report)))
    (is (= 0 (:measured-count report)))
    (is (= :measured (:count-evidence-class report)))
    (is (= :projected (:decode-seconds-evidence-class report)))
    (is (false? (:promotion-authority report)))))

(deftest overhead-and-no-model-gates-are-closed
  (let [verdict (invoke-or-empty
                  'clj-surgeon.mcp-substantiation/overhead-verdict
                  {:event-bytes 32768
                   :projection-p95-ms 0.49
                   :append-p95-ms 4.99
                   :live-p95-delta-ms 5.0
                   :model-calls 0
                   :network-calls 0})]
    (is (true? (:event-bound-pass verdict)))
    (is (true? (:pure-projection-pass verdict)))
    (is (true? (:append-pass verdict)))
    (is (true? (:live-pass verdict)))
    (is (true? (:no-model-network-pass verdict)))))
