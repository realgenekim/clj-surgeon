(ns clj-surgeon.mcp-combinable-transaction-test
  "Characterization contract for the combinable-transaction steering note.

  A caller that commits an edits-only transaction and then, seconds later, a
  create_files-only transaction in the same workspace has spent two receipts
  where one atomic call would have carried both under mutual rollback. The
  server notices that adjacent pair and says so in the second receipt.

  The note is a nudge. It never refuses, never alters ok or committed, never
  appears on a refused transaction, and never argues from payload size."
  ;; @spec MCP-OP-EDIT-032
  (:require
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (io.modelcontextprotocol.common McpTransportContext)
   (io.modelcontextprotocol.server McpAsyncServerExchange)
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def ttl-ms 600000)

(def receipt-a (apply str (repeat 64 "a")))
(def receipt-b (apply str (repeat 64 "b")))
(def receipt-c (apply str (repeat 64 "c")))

(defn- load-api []
  (try
    (require 'clj-surgeon.mcp-combinable-transaction)
    (ns-publics 'clj-surgeon.mcp-combinable-transaction)
    (catch java.io.FileNotFoundException _ nil)))

(defn- call [api name & args]
  (if-let [entrance (get api name)]
    (apply entrance args)
    (throw (ex-info (str "ratified combinable-transaction entrance is absent: "
                         name)
                    {:name name}))))

(defmacro ^{:clj-kondo/lint-as 'clojure.core/let} with-combinable-api
  [[api expression] & body]
  `(if-let [~api ~expression]
     (do ~@body)
     (is false "ratified combinable-transaction production entrance is absent")))

(defn- registry [api clock & {:as opts}]
  (call api 'new-registry
        (merge {:clock #(long @clock)
                :boot-epoch "boot-A"}
               opts)))

(defn- committed-result
  [workspace receipt]
  {:ok true
   :committed true
   :operation "edit_clojure"
   :workspace_root workspace
   :receipt_hash receipt
   :next_action "none"})

(defn- refused-result
  [workspace]
  {:ok false
   :operation "edit_clojure"
   :error_type "stale-guard"
   :workspace_root workspace
   :source_unchanged true})

(def edits-only-params
  {:edits [{:file "src/demo.clj"
            :within {:form "route"}
            :from ":done"
            :to ":complete"}]})

(def create-only-params
  {:create_files [{:file "src/demo/helper.clj"
                   :content "(ns demo.helper)\n"}]})

(def mixed-params
  (merge edits-only-params create-only-params))

;; ---------------------------------------------------------------------------
;; Shape classification
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-EDIT-032
(deftest transaction-shape-names-each-verb-shape
  (with-combinable-api [api (load-api)]
    (is (= :edits-only (call api 'transaction-shape edits-only-params)))
    (is (= :create-only (call api 'transaction-shape create-only-params)))
    (is (= :mixed (call api 'transaction-shape mixed-params)))
    (testing "every non-creating mutation verb is one edits-side shape"
      (is (= :edits-only
             (call api 'transaction-shape
                   {:programs [{:file "src/demo.clj" :program "(transform)"}]})))
      (is (= :edits-only
             (call api 'transaction-shape
                   {:delete_owners [{:file "src/demo.clj" :owners ["gone"]}]})))
      (is (= :mixed
             (call api 'transaction-shape
                   (merge create-only-params
                          {:delete_owners [{:file "src/demo.clj"
                                            :owners ["gone"]}]})))))
    (testing "a request with no supplied work has no shape"
      (is (nil? (call api 'transaction-shape {})))
      (is (nil? (call api 'transaction-shape {:edits [] :create_files []})))
      (is (nil? (call api 'transaction-shape nil))))
    (testing "public JSON string keys classify identically"
      (is (= :create-only
             (call api 'transaction-shape
                   {"create_files" [{"file" "src/demo/helper.clj"
                                     "content" "(ns demo.helper)\n"}]})))
      (is (= :edits-only
             (call api 'transaction-shape
                   {"edits" [{"file" "src/demo.clj"}]}))))
    (testing "the prepared-confirmation route is edits-only by its own schema"
      (is (= :edits-only
             (call api 'transaction-shape
                   {:confirm receipt-a
                    :fill {"arguments.edits[0].to" "(def alpha :new)"}}))))))

;; ---------------------------------------------------------------------------
;; The note, in both orders
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-EDIT-032
(deftest create-only-after-edits-only-is-named-combinable
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)
          first-result (call api 'attach-note! store "session-A"
                             edits-only-params
                             (committed-result "/workspace/demo" receipt-a))]
      (is (not (contains? first-result :combinable_note))
          "the first transaction of a session names no prior receipt")
      (swap! clock + 4000)
      (let [second-result (call api 'attach-note! store "session-A"
                                create-only-params
                                (committed-result "/workspace/demo" receipt-b))
            note (:combinable_note second-result)]
        (is (map? note) (pr-str second-result))
        (is (= receipt-a (:prior_receipt_hash note)))
        (is (= (str "these two transactions were combinable: one atomic call "
                    "carrying both edits and create_files would have produced "
                    "one receipt with mutual rollback")
               (:hint note)))))))

;; @spec MCP-OP-EDIT-032
(deftest edits-only-after-create-only-is-named-combinable
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)]
      (call api 'attach-note! store "session-A" create-only-params
            (committed-result "/workspace/demo" receipt-a))
      (swap! clock + 4000)
      (let [note (:combinable_note
                  (call api 'attach-note! store "session-A" edits-only-params
                        (committed-result "/workspace/demo" receipt-b)))]
        (is (map? note))
        (is (= receipt-a (:prior_receipt_hash note))
            "the mirror order is the same combinable pair")))))

;; @spec MCP-OP-EDIT-032
(deftest a-repeated-shape-is-never-named
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/demo" receipt-a))
      (swap! clock + 4000)
      (is (nil? (:combinable_note
                 (call api 'attach-note! store "session-A" edits-only-params
                       (committed-result "/workspace/demo" receipt-b))))
          "two edits-only transactions are not the pair this verb fuses")
      (swap! clock + 4000)
      (is (= receipt-b
             (:prior_receipt_hash
              (:combinable_note
               (call api 'attach-note! store "session-A" create-only-params
                     (committed-result "/workspace/demo" receipt-c)))))
          (str "a repeated shape still refreshes the memo, so the pair names "
               "the immediately prior receipt and never an older one")))))

;; ---------------------------------------------------------------------------
;; Every suppression
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-EDIT-032
(deftest a-mixed-transaction-is-never-named
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)]
      (testing "a mixed commit after an edits-only commit is already fused"
        (call api 'attach-note! store "session-A" edits-only-params
              (committed-result "/workspace/demo" receipt-a))
        (swap! clock + 4000)
        (is (nil? (:combinable_note
                   (call api 'attach-note! store "session-A" mixed-params
                         (committed-result "/workspace/demo" receipt-b))))))
      (testing "and a create-only commit after that mixed commit is not a pair"
        (swap! clock + 4000)
        (is (nil? (:combinable_note
                   (call api 'attach-note! store "session-A" create-only-params
                         (committed-result "/workspace/demo" receipt-c)))))))))

;; @spec MCP-OP-EDIT-032
(deftest a-different-workspace-root-is-never-named
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/alpha" receipt-a))
      (swap! clock + 4000)
      (is (nil? (:combinable_note
                 (call api 'attach-note! store "session-A" create-only-params
                       (committed-result "/workspace/beta" receipt-b))))
          "two workspaces could not have shared one transaction"))))

;; @spec MCP-OP-EDIT-032
(deftest an-expired-memo-is-never-named
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/demo" receipt-a))
      (swap! clock + ttl-ms)
      (is (nil? (:combinable_note
                 (call api 'attach-note! store "session-A" create-only-params
                       (committed-result "/workspace/demo" receipt-b))))
          "a ten-minute-old transaction is no longer an adjacent pair"))
    (let [clock (atom 1000)
          store (registry api clock)]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/demo" receipt-a))
      (swap! clock + (dec ttl-ms))
      (is (map? (:combinable_note
                 (call api 'attach-note! store "session-A" create-only-params
                       (committed-result "/workspace/demo" receipt-b))))
          "one millisecond inside the ten-minute window still pairs"))))

;; @spec MCP-OP-EDIT-032
(deftest a-refusal-between-forgets-the-pair
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/demo" receipt-a))
      (swap! clock + 1000)
      (call api 'attach-note! store "session-A" edits-only-params
            (refused-result "/workspace/demo"))
      (swap! clock + 1000)
      (is (nil? (:combinable_note
                 (call api 'attach-note! store "session-A" create-only-params
                       (committed-result "/workspace/demo" receipt-b))))
          "a refusal between the two commits breaks the adjacent pair"))))

;; @spec MCP-OP-EDIT-032
(deftest a-refused-transaction-never-carries-the-note
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)
          refusal (refused-result "/workspace/demo")]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/demo" receipt-a))
      (swap! clock + 1000)
      (let [returned (call api 'attach-note! store "session-A"
                           create-only-params refusal)]
        (is (= refusal returned)
            "a refused transaction is returned exactly as the kernel wrote it")
        (is (not (contains? returned :combinable_note)))))))

;; @spec MCP-OP-EDIT-032
(deftest a-preview-neither-names-nor-forgets
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)
          preview {:ok true
                   :operation "edit_clojure-preview"
                   :committed false
                   :workspace_root "/workspace/demo"}]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/demo" receipt-a))
      (swap! clock + 1000)
      (is (= preview (call api 'attach-note! store "session-A"
                           edits-only-params preview))
          "a preview commits nothing and is returned untouched")
      (swap! clock + 1000)
      (is (map? (:combinable_note
                 (call api 'attach-note! store "session-A" create-only-params
                       (committed-result "/workspace/demo" receipt-b))))
          "and a preview does not break the adjacent pair"))))

;; @spec MCP-OP-EDIT-032
(deftest sessions-never-leak-into-each-other
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/demo" receipt-a))
      (swap! clock + 1000)
      (is (nil? (:combinable_note
                 (call api 'attach-note! store "session-B" create-only-params
                       (committed-result "/workspace/demo" receipt-b))))
          "another session's transaction is not this caller's prior receipt")
      (testing "an absent session key disables the memo entirely"
        (let [result (committed-result "/workspace/demo" receipt-c)]
          (is (= result (call api 'attach-note! store nil
                              create-only-params result)))
          (is (= result (call api 'attach-note! store "" create-only-params
                              result))))))))

;; @spec MCP-OP-EDIT-032
(deftest memory-is-bounded-and-evicts-the-oldest-session
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock :max-sessions 2)]
      (doseq [session ["session-A" "session-B" "session-C"]]
        (swap! clock + 1000)
        (call api 'attach-note! store session edits-only-params
              (committed-result "/workspace/demo" receipt-a)))
      (let [stats (call api 'registry-stats store)]
        (is (= 2 (:memo-count stats)) (pr-str stats))
        (is (= 2 (:max-sessions stats)))
        (is (= "boot-A" (:boot-epoch stats))))
      (swap! clock + 1000)
      (is (nil? (:combinable_note
                 (call api 'attach-note! store "session-A" create-only-params
                       (committed-result "/workspace/demo" receipt-b))))
          "the evicted oldest session has no prior receipt to name")
      (is (map? (:combinable_note
                 (call api 'attach-note! store "session-C" create-only-params
                       (committed-result "/workspace/demo" receipt-c))))
          "the retained session still pairs"))))

;; @spec MCP-OP-EDIT-032
(deftest resetting-the-registry-forgets-every-session
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/demo" receipt-a))
      (call api 'reset-registry! store)
      (is (= 0 (:memo-count (call api 'registry-stats store))))
      (swap! clock + 1000)
      (is (nil? (:combinable_note
                 (call api 'attach-note! store "session-A" create-only-params
                       (committed-result "/workspace/demo" receipt-b))))))))

;; ---------------------------------------------------------------------------
;; The note says atomicity, never payload size
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-EDIT-032
(deftest the-note-argues-atomicity-and-never-payload-savings
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)
          _ (call api 'attach-note! store "session-A" edits-only-params
                  (committed-result "/workspace/demo" receipt-a))
          _ (swap! clock + 1000)
          result (call api 'attach-note! store "session-A" create-only-params
                       (committed-result "/workspace/demo" receipt-b))
          note (:combinable_note result)
          hint (str/lower-case (str (:hint note)))]
      (is (= #{:prior_receipt_hash :hint} (set (keys note)))
          "the note publishes exactly one prior receipt and one hint")
      (is (str/includes? hint "rollback"))
      (is (str/includes? hint "atomic"))
      (doseq [forbidden ["token" "byte" "cheaper" "cost" "savings" "faster"]]
        (is (not (str/includes? hint forbidden))
            (str "the benefit is atomicity, never " forbidden))))))

;; @spec MCP-OP-EDIT-032
(deftest the-note-never-alters-committed-semantics
  (with-combinable-api [api (load-api)]
    (let [clock (atom 1000)
          store (registry api clock)
          committed (committed-result "/workspace/demo" receipt-b)]
      (call api 'attach-note! store "session-A" edits-only-params
            (committed-result "/workspace/demo" receipt-a))
      (swap! clock + 1000)
      (let [result (call api 'attach-note! store "session-A"
                         create-only-params committed)]
        (is (true? (:ok result)))
        (is (true? (:committed result)))
        (is (= "none" (:next_action result)))
        (is (= receipt-b (:receipt_hash result)))
        (is (= committed (dissoc result :combinable_note))
            "the note is the only field the steering layer adds")))))

;; ---------------------------------------------------------------------------
;; Real handler integration — the exact adoption gap the verb exists to fuse
;; ---------------------------------------------------------------------------

(defn- delete-tree! [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- invoke-handler [handler exchange params]
  (let [result (promise)]
    (handler exchange params
             (fn [content error? structured]
               (deliver result {:content content
                                :error? error?
                                :structured structured})))
    (deref result 10000 {:timeout true})))

;; @spec MCP-OP-EDIT-032
(deftest real-edit-then-create-transactions-are-named-combinable
  (let [root (.toFile (Files/createTempDirectory
                       "clj-surgeon-combinable-"
                       (make-array FileAttribute 0)))
        source-file (io/file root "src/demo.clj")
        exchange-a (McpAsyncServerExchange.
                    "sdk-session-A" nil nil nil McpTransportContext/EMPTY)
        exchange-b (McpAsyncServerExchange.
                    "sdk-session-B" nil nil nil McpTransportContext/EMPTY)]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file "(ns demo)\n\n(defn route [] :done)\n")
      (mcp-tool/init! {:project-root (.getPath root)
                       :receipt-dir (.getPath (io/file root "receipts"))})
      (let [edited (invoke-handler
                    mcp-tool/handle-edit-clojure exchange-a
                    {:edits [{:file "src/demo.clj"
                              :within {:form "route"}
                              :from ":done"
                              :to ":complete"}]})
            edited-receipt (get-in edited [:structured :receipt_hash])
            created (invoke-handler
                     mcp-tool/handle-edit-clojure exchange-a
                     {:create_files [{:file "src/demo/helper.clj"
                                      :content "(ns demo.helper)\n"}]})
            other-session (invoke-handler
                           mcp-tool/handle-edit-clojure exchange-b
                           {:create_files [{:file "src/demo/other.clj"
                                            :content "(ns demo.other)\n"}]})]
        (is (true? (get-in edited [:structured :ok])) (pr-str edited))
        (is (nil? (get-in edited [:structured :combinable_note])))
        (is (true? (get-in created [:structured :ok])) (pr-str created))
        (is (= edited-receipt
               (get-in created [:structured :combinable_note
                                :prior_receipt_hash]))
            "the second receipt names the first receipt by hash")
        (is (true? (get-in created [:structured :committed]))
            "the note never alters committed semantics")
        (is (nil? (get-in other-session [:structured :combinable_note]))
            "a different SDK session never inherits the pair")
        (testing "a server restart forgets every memo"
          (mcp-tool/init! {:project-root (.getPath root)
                           :receipt-dir (.getPath (io/file root "receipts"))})
          (let [after (invoke-handler
                       mcp-tool/handle-edit-clojure exchange-a
                       {:edits [{:file "src/demo.clj"
                                 :within {:form "route"}
                                 :from ":complete"
                                 :to ":done"}]})]
            (is (true? (get-in after [:structured :ok])) (pr-str after))
            (is (nil? (get-in after [:structured :combinable_note]))))))
      (finally
        (mcp-tool/init! nil)
        (delete-tree! root)))))
