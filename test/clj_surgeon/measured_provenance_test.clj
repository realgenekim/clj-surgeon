(ns clj-surgeon.measured-provenance-test
  "PROVENANCE: the measured tag rides the VALUE, not the name.

  Sol's round-two review (2026-09-04 §1) broke the first repair of this
  invariant in one move. The repair bound the partition to a VOCABULARY of
  declared field names. So: bind an existing clock read to a local, publish
  the same number under the declared `:elapsed_ms` AND an undeclared
  `:verification_wall_ms`, and

      {:undeclared-field 1.137065, :hashed-field 1.137065,
       :unpartitioned-paths []}

  — the undeclared field reached the parity hash, the diagnostic reported
  nothing, and all sixteen invariant assertions passed. No clock-read COUNT
  changed, and a vocabulary can only describe the names somebody already
  thought of.

  The rule these witnesses hold is the one that survives that attack:

  1. a clock reading is TAGGED where the clock is read, and the publication
     boundary relocates every reading it finds under ANY key at all;
  2. a tagged reading never reaches the wire — inside the partition it is a
     bare number, and outside it the boundary REFUSES rather than publishes;
  3. every site that hands a public result to the MCP SDK is enumerated and
     routes through that boundary — the review's second finding was a failure
     path that did not;
  4. the intent chain says the same wire the code does;
  5. the in-repository consumer of the wire reads the partition, and refuses
     the old top-level shape rather than reporting a silent zero.

  @spec MCP-OP-TIME-005
  @spec MCP-OP-RESULT-001
  @spec MCP-OP-RESULT-003
  @spec MCP-OP-SCHEMA-001"
  (:require
   [babashka.process :as process]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.measured :as measured]))

(def superseded-reading-key
  "The tag a reading USED to be a one-key map under, spelled literally.

  Round three published a reading as `{:clj-surgeon.measured/reading n}` and
  called `value` its one door. The round-three review opened it with a keyword
  lookup and put an undeclared clock field into the parity hash with every
  witness green (2026-09-04 §1a). A reading is now an opaque type, and its
  shape can no longer be spelled literally by anyone — including by this
  witness, which is the point.

  So the literal keyword is kept here as the shape a reading must NOT have.
  `a-literal-map-is-not-a-reading` below is the ratchet against the map coming
  back."
  :clj-surgeon.measured/reading)

(defn- reading
  [n]
  (measured/reading n))

;; @spec MCP-OP-TIME-005
(deftest a-literal-map-is-not-a-reading
  (testing "the map shape, whose keyword lookup was the §1a bypass, is gone"
    (is (false? (measured/reading? {superseded-reading-key 2.5}))
        "a one-key map is a reading again; a keyword lookup reopens §1a")
    (is (true? (measured/reading? (measured/reading 2.5)))
        "the opaque reading is not recognised by its own predicate")
    (is (nil? (get (measured/reading 2.5) superseded-reading-key))
        "a reading still answers a keyword lookup with its number")))

(defn- fixed-clock
  [start-ns delta-ns]
  (let [reads (atom 0)]
    #(if (zero? (first (swap-vals! reads inc))) start-ns (+ start-ns delta-ns))))

(defn- publish
  "One public MCP result, as the shared finalizer publishes it."
  [domain-result clock]
  (let [seen (atom nil)]
    (mcp-operation/invoke!
      {:clock-nanos clock
       :execute (constantly domain-result)
       :summarize (constantly "ok")
       :serialize pr-str
       :callback (fn [_ _ result] (reset! seen result))})
    @seen))

;; ============================================================
;; 1. The tag rides the value
;; ============================================================

;; @spec MCP-OP-TIME-005
(deftest a-clock-reading-is-relocated-under-whatever-key-it-is-published-under
  (testing "the reviewer's counterexample: one read, two names, one undeclared"
    (let [duration (reading 1.137065)
          result (publish {:ok false
                           :status :failed
                           :error-type :hot-verification-connection-failed
                           :elapsed_ms duration
                           :verification_wall_ms duration}
                          (fixed-clock 0 1000000))
          hashed (measured/hashed-channel result)]
      (is (nil? (:verification_wall_ms result))
          (str "an undeclared clock field was published beside the partition: "
               (pr-str result)))
      (is (= 1.137065 (get-in result [measured/measured-key :verification_wall_ms]))
          "the undeclared reading was dropped rather than relocated")
      (is (false? (contains? hashed :verification_wall_ms))
          (str "the undeclared clock field is inside the hash subject: "
               (pr-str hashed)))
      (is (= {:ok false
              :status :failed
              :error-type :hot-verification-connection-failed}
             hashed)
          "the hashed channel lost or gained a deterministic fact"))))

;; @spec MCP-OP-TIME-005
(deftest the-diagnostic-sees-an-undeclared-reading
  (testing "a diagnostic blind to its own subject is a false green"
    (let [raw {:ok false :verification_wall_ms (reading 2.5)}]
      (is (= [[:verification_wall_ms]]
             (vec (measured/unpartitioned-measured-paths raw)))
          (str "the diagnostic reported no unpartitioned measured field: "
               (pr-str (vec (measured/unpartitioned-measured-paths raw))))))))

;; @spec MCP-OP-RESULT-001
(deftest a-tagged-reading-never-reaches-the-wire
  (testing "inside the partition a reading is a bare number, not an object"
    (let [result (publish {:ok true
                           :receipt {:resources {:bytes_scanned 12
                                                 :scan_ms (reading 1.5)}}}
                          (fixed-clock 0 2500000))]
      (is (every? number? (vals (get result measured/measured-key)))
          (str "a tagged reading survived into the published partition: "
               (pr-str (get result measured/measured-key))))
      (is (= 1.5 (get-in result [:receipt :resources
                                 measured/measured-key :scan_ms]))
          "a nested reading was not unwrapped into its own partition")
      (is (= [] (vec (measured/unpartitioned-measured-paths result)))
          (str "measured values published outside the partition: "
               (pr-str (vec (measured/unpartitioned-measured-paths result))))))))

;; @spec MCP-OP-RESULT-003
(deftest the-boundary-refuses-a-result-it-cannot-partition
  (testing "a reading with no key to relocate is a typed refusal, not a wire object"
    (let [thrown (try
                   (publish {:ok true :rows [(reading 3.0)]}
                            (fixed-clock 0 1000000))
                   ::no-throw
                   (catch Exception error (ex-data error)))]
      (is (not= ::no-throw thrown)
          "an unpartitionable reading was published instead of refused")
      (is (= :unpartitioned-measured-field (:error-type thrown))
          (str "the refusal is not typed: " (pr-str thrown))))))

;; ============================================================
;; 2. Every public-result publish site routes through the boundary
;; ============================================================

(defn- form-region
  "The text of the top-level form named `form` in `path`."
  [path form]
  (let [lines (str/split-lines (slurp path))
        start (first (keep-indexed
                       (fn [i line]
                         (when (and (str/starts-with? line "(def")
                                    (= form (second (str/split (str/trim line)
                                                               #"[\s\[]+"))))
                           i))
                       lines))]
    (when start
      (->> (drop (inc start) lines)
           (take-while #(not (str/starts-with? % "(def")))
           (cons (nth lines start))
           (str/join "\n")))))

(defn- scan-forms
  "`{[path form] hits}` for every line under `root` matching `pattern`, named by
   the top-level form it sits in."
  [root pattern]
  (frequencies
    (mapcat
      (fn [^java.io.File file]
        (:hits
         (reduce (fn [{:keys [form hits]} line]
                   (let [code (or (first (str/split line #";;")) "")
                         form' (if (str/starts-with? line "(def")
                                 (second (str/split (str/trim line) #"[\s\[]+"))
                                 form)]
                     {:form form'
                      :hits (cond-> hits
                              (re-find pattern code)
                              (conj [(.getPath file) form']))}))
                 {:form nil :hits []}
                 (str/split-lines (slurp file)))))
      (->> (file-seq (io/file root))
           (filter #(.isFile ^java.io.File %))
           (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
           (sort-by #(.getPath ^java.io.File %))))))

(defn- publish-sites
  "`{[path form] calls}` for every call of the SDK's structured publish verb."
  ([] (publish-sites "src"))
  ([root] (scan-forms root #"\(structured-call-result")))

(def ^:private sdk-result-pattern
  "Every way to build an SDK `CallToolResult` WITHOUT the sanctioned helper.

  The round-three review named this gap (§2): `publish-sites` greps for the
  literal `(structured-call-result`, so a publish site that assembles the SDK
  result by hand — exactly what `structured-call-result` itself does — was
  invisible, and the docstring's promise that \"a NEW publish site fails this
  test\" was narrower than it read. The builder is now scanned too, and it is
  allowed in exactly one form.

  CONSTRUCTION, not the setter: `.structuredContent` is also how a CLIENT
  reads a response (`mcp-semantic-client/normalize-result`), and a scan that
  cannot tell a reader from a publisher is one somebody will re-bless."
  #"CallToolResult/builder|CallToolResult\.")

(def sdk-result-construction-site
  "The ONE form in `src/` that may assemble an SDK result.

  Everything else publishes by calling it, which is what makes
  `public-result-publish-sites` an inventory rather than a sample."
  {["src/clj_surgeon/mcp_server.clj" "structured-call-result"] 1})

;; @spec MCP-OP-RESULT-002
(deftest the-sdk-result-is-assembled-in-exactly-one-form
  (testing "a hand-built CallToolResult is a publish site the grep cannot see"
    (let [scanned (scan-forms "src" sdk-result-pattern)]
      (is (= sdk-result-construction-site scanned)
          (str "the SDK result is assembled outside `structured-call-result`: "
               (pr-str scanned))))))

;; @spec MCP-OP-RESULT-002
(deftest the-sdk-scan-catches-a-hand-built-publish-site
  (testing "the ratchet goes RED when the round-three gap is reintroduced"
    (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                             (str "sdk-publish-plant-" (System/nanoTime))))
          victim (io/file root "clj_surgeon" "planted_direct_publish.clj")]
      (.mkdirs (.getParentFile victim))
      (spit victim
            (str "(ns clj-surgeon.planted-direct-publish)\n\n"
                 "(defn planted-direct-publish\n"
                 "  [json-mapper content structured]\n"
                 "  (-> (McpSchema$CallToolResult/builder)\n"
                 "      (.textContent content)\n"
                 "      (.structuredContent json-mapper structured)\n"
                 "      (.build)))\n"))
      (try
        (let [scanned (scan-forms root sdk-result-pattern)]
          (is (= {[(.getPath victim) "planted-direct-publish"] 1} scanned)
              (str "the scan did not see a hand-built SDK result: "
                   (pr-str scanned))))
        (finally
          (.delete victim)
          (.delete (.getParentFile victim))
          (.delete (io/file root)))))))

(def public-result-publish-sites
  "Every form in `src/` that hands a structured result to the MCP SDK, and the
  boundary verb each one goes through.

  Sol's round-two review, finding 2: `recovery/recover!` was not the only
  bypass of `mcp-operation/invoke!`. The SDK adapter's own `catch` built an
  `mcp-adapter-failure` map and published it directly, so a throw anywhere in
  execution, finalization, summary rendering or serialization produced a public
  result with no `measured` block at all — invalid against every canonical
  output schema, which requires one.

  A NEW publish site fails this test until it is named here with the boundary
  it routes through, and the boundary verb must actually appear in the form."
  {["src/clj_surgeon/mcp_server.clj" "create-structured-async-tool"]
   {:calls 2
    :via ["mcp-operation/finalize-failure"]
    :why (str "the ordinary path is published by the tool-fn's own "
              "mcp-operation/invoke!; the adapter's catch routes its failure "
              "through the same finalizer")}})

;; @spec MCP-OP-RESULT-002
(deftest every-public-result-publish-site-routes-through-the-boundary
  (testing "a result that skips the finalizer carries no measured partition"
    (let [scanned (publish-sites)
          declared (into {} (map (fn [[k v]] [k (:calls v)]))
                         public-result-publish-sites)]
      (is (= (set (keys declared)) (set (keys scanned)))
          (str "undeclared public-result publish sites: "
               (pr-str (sort (remove (set (keys declared)) (keys scanned))))
               " ; declared sites that no longer exist: "
               (pr-str (sort (remove (set (keys scanned)) (keys declared))))))
      (is (= declared scanned)
          "a form's publish-call count changed; re-read it and re-justify")
      (doseq [[[path form] {:keys [via]}] public-result-publish-sites]
        (let [region (form-region path form)]
          (doseq [verb via]
            (is (and region (str/includes? region verb))
                (str path " " form " does not route through " verb))))))))

;; ============================================================
;; 3. The intent chain says the same wire the code does
;; ============================================================

(def intent-chain-files
  "HLD, LLD and EARS rows for the MCP operation contract."
  ["docs/high-level-design.md"
   "docs/intent/mcp-operation-contract/mcp-operation-contract-design.md"
   "docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md"])

;; @spec MCP-OP-SCHEMA-001
(deftest the-intent-chain-names-the-partition
  (testing "a design document that disagrees with the wire is a second contract"
    (let [offenders
          (vec (for [path intent-chain-files
                     [n line] (map-indexed vector (str/split-lines (slurp path)))
                     :when (and (str/includes? line "elapsed_ms")
                                (not (str/includes? line "measured")))]
                 [path (inc n) (str/trim line)]))]
      (is (= [] offenders)
          (str "the intent chain still describes a top-level request clock: "
               (pr-str offenders))))))

;; ============================================================
;; 4. The in-repository consumer of the wire reads the partition
;; ============================================================

;; @spec MCP-OP-TIME-005
(deftest the-benchmark-event-reader-reads-the-partition
  (testing "a reader of the old shape reports a silent zero, not an error"
    (let [{:keys [exit out err]}
          (process/sh {:out :string :err :string}
                      "bb" "bench/event_timing.clj" "--self-test")]
      (is (zero? exit)
          (str "bench/event_timing.clj --self-test failed: "
               (str/trim (str out err)))))))
