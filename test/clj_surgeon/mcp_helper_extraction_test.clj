(ns clj-surgeon.mcp-helper-extraction-test
  "RED witnesses for the I/O BOUNDARY of `helper_extraction`.

  Split from `clj-surgeon.helper-extraction-test` so the pure planner can be
  built and run with this boundary entirely absent. Everything that needs the
  boundary namespace, the filesystem, or a child process lives here:

    - tool registration and the closed input schema (001);
    - the admitted verification profiles (011);
    - the `terminal-receipt` MAPPER (020, 022);
    - a project whose root sits under an ancestor directory named `src`;
    - the LOAD PROOFS the pure witnesses deliberately do not claim.

  WHY THE LOAD PROOFS ARE HERE. An acyclic require graph is NOT a proof that
  Clojure loads a tree: a required namespace can reach a Var of the namespace
  that requires it before that one has finished loading, and the compile fails
  with a graph that has no cycle in it at all. Proving a tree loads means
  loading it, in a real process, which is a thing a pure planner witness must
  not do. These witnesses therefore spawn babashka, which is one more reason
  both namespaces are `excluded` from the JVM gate lanes.

  THIS NAMESPACE DOES NOT LOAD UNTIL THE BOUNDARY EXISTS. Until it is green it
  runs under `make mcp-helper-extraction-red`.

  Boundary surfaces these witnesses bind to:
    `(mcp-helper/tool)`                  registration map with :inputSchema
    `(mcp-helper/admitted-profiles)`     {name profile-capability}
    `(mcp-helper/terminal-states)`       the four terminal states
    `(mcp-helper/terminal-receipt {:kernel _ :verification _ :plan _})`
                                         a PURE MAPPING from facts the kernel
                                         and the profile produced onto the
                                         receipt
    `(mcp-helper/plan request)`          reads a real tree under
                                         `workspace_root` and plans
    The boundary adds one refusal the planner cannot reach:
    `helper-extraction-verification-preflight-unavailable`, because a
    profile's capability is a fact about the registry, not about a request"
  {:lane :battery}
  (:require
   [clj-surgeon.helper-extraction-fixture :as fixture]
   [clj-surgeon.mcp-helper-extraction :as mcp-helper]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clj-surgeon.mcp-schema :as mcp-schema]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; loading is proved by LOADING, in a fresh process

(def ^:private tmp-root
  (or (System/getenv "CLJ_SURGEON_HELPER_TMP") "/var/tmp/forge/helper-fx"))

(defn- delete-tree!
  [^java.io.File file]
  (when (.isDirectory file)
    (run! delete-tree! (.listFiles file)))
  (.delete file))

(def ^:private project-marker
  "A materialized fixture tree is not a PROJECT until it carries one.

  Learned the hard way: without it the boundary's workspace resolution finds no
  admitted roots, `plan` reports the source file is not among the sources it was
  handed, and every witness downstream of it goes green for the wrong reason --
  nothing is written, so `nothing was left behind` is trivially true. A
  restoration witness on a tree that was never staged proves nothing at all."
  {"deps.edn" "{:paths [\"src\"]}\n"})

(defn- tree-of
  "`{path source}` for one fixture variant at one phase (`:pre` or `:post`),
  as a real project the boundary can route to."
  [variant phase]
  (into project-marker
        (keep (fn [entry] (when-let [source (get entry phase)]
                            [(:file entry) source])))
        (fixture/files variant)))

(defn- fresh-load
  "Materialize `tree`, then `require` `namespaces` in a FRESH babashka process
  and print `expr` (default `:loaded`). Returns `{:exit :out :err}`; exit 0
  means the tree really loads, and `:out` is what `expr` evaluated to."
  ([label tree namespaces] (fresh-load label tree namespaces ":loaded"))
  ([label tree namespaces expr]
   (let [root (io/file tmp-root (str label "-" (System/nanoTime)))]
     (try
       (doseq [[path source] tree]
         (let [target (io/file root path)]
           (io/make-parents target)
           (spit target source)))
       (shell/sh "bb" "-cp" "src" "-e"
                 (str "(require "
                      (str/join " " (map #(str "'" %) namespaces))
                      ") (println " expr ")")
                 :dir (str root))
       (finally (delete-tree! root))))))

(def ^:private configured-profiles
  "The verification profiles these witnesses configure.

  THERE IS NO BUILT-IN `helper-proof`: admission comes only from the configured
  verification-profiles path, the same one `alias_migration`'s `verify` reads,
  so a witness that wants a runnable profile must configure one exactly as a
  workspace would. `/bin/true` is a real synchronous command that really runs
  and really succeeds, so a failure below is the one the witness injected and
  never a proof that could not have passed in the first place."
  {"helper-proof" {:commands [["/bin/true"]]}
   "noop-proof" {:commands [["/bin/true"]]}})


;; ---------------------------------------------------------------------------
;; the fixture's own trees, proved by loading them

;; @spec MCP-OP-HELPER-019
(deftest the-valid-original-tree-really-loads
  (testing "the pure witness
            `helper-extraction-test/a-valid-original-with-a-retained-public-dependency-still-refuses`
            asserts a refusal on this fixture. Its meaning depends on the
            original being a tree that works, and this is where that is
            established -- by loading it."
    (let [{:keys [exit err]} (fresh-load "retained-direct-pre"
                                         (tree-of :retained-dependency-direct :pre)
                                         ['acid.web.http])]
      (is (zero? exit) (str "the original must load: " err)))))

;; @spec MCP-OP-HELPER-019
(deftest an-acyclic-require-graph-is-not-a-load-proof
  (testing "the source -> C -> destination -> source fixture: its require
            graph is ACYCLIC and its original still does not load, because C
            is compiled while the source that requires it is still loading and
            cannot resolve acid.web.http/json-response.

            This is recorded as evidence, not as a defect. It is why the
            valid-original refusal is proved on :retained-dependency-direct,
            and it is a finding about the supported grammar rather than a
            claim about programs in general: a source -> third -> destination
            chain whose ORIGINAL loads is not constructible here, because the
            third namespace must statically reach the source that requires it."
    (is (empty? (fixture/cyclic-namespaces
                 (fixture/static-require-graph :retained-dependency-chain :pre)))
        "the graph says acyclic")
    (let [{:keys [exit err]} (fresh-load "chain-pre"
                                         (tree-of :retained-dependency-chain :pre)
                                         ['acid.web.http])]
      (is (not (zero? exit))
          "and the loader says otherwise: the false proof, made visible")
      (is (str/includes? (str err) "acid.web.http/json-response")
          "naming the unresolvable forward reference"))))

;; @spec MCP-OP-HELPER-014
(deftest a-qualified-only-caller-gains-its-load-path
  (let [pre-tree (tree-of :happy :pre)]
    (testing "BEFORE: the caller requires nothing, so it loads only when
              something else has already loaded the source"
      (is (zero? (:exit (fresh-load "fq01-bootstrap" pre-tree
                                    ['acid.web.http 'acid.app.fq01])))
          "with the source loaded first, the original loads")
      (is (not (zero? (:exit (fresh-load "fq01-standalone" pre-tree
                                         ['acid.app.fq01]))))
          "and on its own it does NOT: that is what an absent load path looks
           like, and what the write has to repair"))
    (testing "AFTER: the canonical post-extraction caller loads alone, with
              nothing else required first"
      (is (zero? (:exit (fresh-load "fq01-post" (tree-of :happy :post)
                                    ['acid.app.fq01])))))))

;; @spec MCP-OP-HELPER-003
(deftest the-multi-arity-owner-behaves-identically-before-and-after
  (testing "`html-response` is two arities delegating to itself by name. The
            pure witness compares the moved BYTES against the fixture's own
            description; this one runs BOTH arities in a fresh process on each
            tree and compares what they actually return."
      (let [q (fn [text] (str (char 34) text (char 34)))
            arities (fn [ns-name]
                      (str "(pr-str [(" ns-name "/html-response " (q "b") ") "
                           "(" ns-name "/html-response " (q "b") " " (q "e") ")])"))
            before (fresh-load "multiarity-pre" (tree-of :happy :pre)
                               ['acid.web.http] (arities "acid.web.http"))
            after (fresh-load "multiarity-post" (tree-of :happy :post)
                              ['acid.web.response] (arities "acid.web.response"))]
        (is (zero? (:exit before)) (:err before))
        (is (zero? (:exit after)) (:err after))
        (is (str/includes? (:out before) "text/html")
            "the original really answers on both arities")
        (is (= (:out before) (:out after))
            "and the moved definition answers identically on both arities: the
             arity list, the self-delegation and the nil default all survive
             the move"))))

;; ---------------------------------------------------------------------------
;; registration and profiles

;; @spec MCP-OP-HELPER-001
(deftest the-server-advertises-helper-extraction-with-a-closed-field-set
  (let [tool (mcp-helper/tool)]
    (is (= "helper_extraction" (:name tool)))
    (is (= #{"op" "workspace_root" "from" "helpers" "to" "scope"
             "verification" "expect"}
           (set (keys (get-in tool [:inputSchema :properties]))))
        "the input schema is closed: no per-file, per-owner or per-site field")
    (is (not (contains? (set (get-in tool [:inputSchema :required])) "expect"))
        "MCP-OP-HELPER-017: expect is optional")))

;; @spec MCP-OP-HELPER-002
;; @spec MCP-OP-HELPER-025
;; @spec MCP-OP-HELPER-011
(deftest only-synchronous-rollback-capable-profiles-are-admitted
  (testing "capability is a property of the CONFIGURED profile. There is no
            built-in `helper-proof`, so admission is asserted over the map a
            workspace supplies, never over a name this verb declares itself."
    (let [admitted (mcp-helper/admitted-profiles configured-profiles)]
      (is (seq admitted))
      (is (contains? admitted "helper-proof")
          "a configured synchronous command profile is admissible")
      (is (every? (fn [profile]
                    (and (:synchronous? profile) (:rollback-capable? profile)))
                  (vals admitted))
          "capability is validated BEFORE writing, not discovered afterwards")))
  (testing "and an asynchronous or warm-JVM profile is NOT admitted"
    (is (empty? (mcp-helper/admitted-profiles
                 {"cold" {:cold ["make" "test"]}
                  "hot" {:hot ["some.ns/law"]}}))
        "a :cold job is asynchronous and a :hot law runs in a warm JVM: both
         are exactly the proofs MCP-OP-HELPER-011/022 refuse to gate on")))

;; @spec MCP-OP-HELPER-011
;; @spec MCP-OP-HELPER-016
(deftest a-profile-that-cannot-run-refuses-before-anything-is-staged
  (testing "THE BOUNDARY OWNS THIS REFUSAL. The pure planner treats
            `verification.profile` as an opaque string; whether a named
            profile exists, is synchronous and is rollback-capable is a fact
            about the registry above, which is why
            `helper-extraction-test/the-declared-refusal-set-is-complete`
            deliberately does not expect the planner to emit it."
    (let [result (mcp-helper/plan
                  (fixture/request {:verification {:profile "no-such-profile"}})
                  configured-profiles)]
      (is (false? (:ok result)) (pr-str result))
      (is (= "helper-extraction-verification-preflight-unavailable"
             (:error_type result)))
      (is (= "no-such-profile" (:profile result)))
      (is (true? (:source_unchanged result)))
      (is (nil? (get-in result [:plan :transactions])) "nothing staged")
      (is (nil? (:next_call result))
          "MCP-OP-HELPER-016: a weaker profile is never suggested")))
  (testing "and it is in the BOUNDARY's refusal set, which is the planner's
            plus the refusals only an I/O boundary can raise"
    (is (contains? (set (mcp-helper/refusal-types))
                   "helper-extraction-verification-preflight-unavailable"))))
;; ---------------------------------------------------------------------------
;; the terminal-receipt MAPPER
;;
;; `terminal-receipt` is a PURE MAPPING from facts the kernel and the profile
;; actually produced onto the receipt. Every witness below INJECTS those facts
;; and asserts the receipt reflects exactly them. None of them hands it empty
;; input and then demands a number, because that would force production to
;; manufacture evidence -- to hardcode a caller count or assert fresh_process
;; with nothing to base it on. The negative witness pins the other side: with
;; no evidence the mapper must claim nothing.
;;
;; WHAT THIS IS NOT: these witnesses do not execute the verifier, the kernel,
;; or a rollback. Boundary tests must later run the actual profile and an
;; actual staged-write rollback and feed their real results through this
;; mapper. A green mapper test is not evidence that a rollback restores bytes.

(def ^:private fixture-plan
  "A plan-shaped value derived from the FIXTURE, not from the planner. The
  mapper witnesses need a plan to fold counts from; taking it from the fixture
  keeps this namespace runnable with the pure planner absent, and keeps the
  mapper's oracle the description rather than the planner's own output."
  {:destination {:lib fixture/dest-lib :file fixture/dest-file}
   :helpers fixture/helpers
   ;; `plan-counts` projects the plan's own `:receipt` onto whatever the kernel
   ;; reported, so a plan-shaped value without one produces a receipt with no
   ;; flat counts at all. An earlier version of this def carried `:counts`
   ;; instead and the mapper faithfully echoed it -- the exemplars looked
   ;; plausible and validated against NO schema variant.
   :receipt {:helpers (:helpers fixture/canonical-counts)
             :source_retired (:source-retired fixture/canonical-counts)
             :caller_files (:caller-files fixture/canonical-counts)
             :source_file (:source-file fixture/canonical-counts)
             :changed_files (:changed-files fixture/canonical-counts)
             :sites (:sites fixture/canonical-counts)
             :retained_sites (:retained-sites fixture/canonical-counts)
             :alias_histogram (:alias-histogram fixture/canonical-counts)
             :partition fixture/canonical-receipt-partition
             :closure {:roots fixture/admitted-roots
                       :authorized_paths fixture/scope-paths
                       :grammar "supported-libspecs-only"
                       :dynamic_references "not-claimed"
                       ;; finding 7's fix: symlinks a walk produces are pruned,
                       ;; and the receipt says how many rather than leaving the
                       ;; reader to wonder whether any were silently read
                       :pruned_symlinks 0}
             :destination_lib fixture/dest-lib}
   :partition fixture/canonical-receipt-partition})

(def ^:private profile-result
  "A profile result as the acceptance-owned helper-proof would return it."
  {:profile "helper-proof"
   :structural_callers 28
   :helper_behaviors 24
   :compiled_callers 0
   :fresh_process true
   :ok true})

(def ^:private committed-kernel
  {:status :committed
   :destination_created true
   :undo_receipt "undo-1" :receipt_hash "hash-1" :elapsed_ms 9310})

(defn- restored-kernel
  [status]
  {:status status
   :restored true
   :restored_files ["src/acid/web/http.clj" "src/acid/app/m01.clj"]
   :restoration_read_back {"src/acid/web/http.clj" "sha-a"
                           "src/acid/app/m01.clj" "sha-b"}
   :destination_removed true})

(def ^:private rollback-failed-kernel
  {:status :rollback-failed
   :restored false
   :unrestored_files ["src/acid/web/http.clj"]
   ;; the shape `finish-failure!` actually builds: the receipt to invert by
   ;; hand, why the automatic inverse did not verify, and the kernel's own
   ;; recovery result. An earlier version named the first key :journal, which
   ;; no production path emits -- a fixture that agrees with nothing.
   :recovery_required {:receipt "/local/state/undo-77.edn"
                       :reason "read-back mismatch"
                       :recovery {:ok false :error "restore did not verify"}}
   :details_path "/local/state/helper-extraction-detail.edn"})

(def ^:private completion-shaped-counts
  "Receipt fields that assert the extraction ACTUALLY HAPPENED.

  A restored receipt describes a write that was undone, so none of these may
  carry a positive number: a reader scanning `source_retired 6` next to
  `restored true` reads a partial extraction where there is none, and a fleet
  counting retirements would double-count a rollback as work done. An attempted
  count is a fact about the PLAN, and belongs under a `planned_` key where its
  tense is visible in the name."
  [:source_retired :sites :retained_sites])

(defn- assert-no-completion-claim!
  "Nothing in `receipt` may claim completed work when the write was restored."
  [receipt]
  (is (contains? #{0 nil} (:source_retired receipt))
      (str "a restored receipt retired nothing: " (pr-str (:source_retired receipt))))
  (is (not (true? (:destination_created receipt)))
      "and created no destination")
  (doseq [field completion-shaped-counts]
    (let [value (get receipt field)]
      (is (or (nil? value) (and (number? value) (zero? value)))
          (str field " is success-shaped and must be 0 or absent on a restored "
               "receipt; an attempted count belongs under planned_"
               (name field) ". Found: " (pr-str value))))))

;; @spec MCP-OP-HELPER-020
(deftest the-four-terminal-states-are-distinct
  (is (= #{:committed :verification-failed :verification-timeout :rollback-failed}
         (set (mcp-helper/terminal-states)))))

;; @spec MCP-OP-HELPER-020
(deftest a-handled-failure-reports-the-restoration-the-kernel-actually-did
  (doseq [status [:verification-failed :verification-timeout]]
    (testing status
      (let [kernel (restored-kernel status)
            receipt (mcp-helper/terminal-receipt
                     {:kernel kernel
                      :verification (assoc profile-result :ok false)
                      :plan fixture-plan})]
        (is (= (name status) (:status receipt)))
        (is (false? (:committed receipt)))
        (is (true? (:restored receipt)) "reflecting the kernel's own :restored")
        (is (true? (:source_unchanged receipt))
            "unchanged is claimed only because the kernel restored it")
        (is (false? (:destination_created receipt)))
        (is (some? (:restoration_read_back receipt))
            "the read-back reaches the receipt")
        (is (= (count (:restored_files kernel))
               (get-in receipt [:restoration_read_back :files]))
            "as CONSTANT-SIZE evidence of what the kernel restored -- a count
             and an aggregate digest, never the per-file map, which would make
             an ordinary failure receipt grow with the tree (finding 5)")
        (is (string? (get-in receipt [:restoration_read_back :aggregate_sha256])))
        (is (false? (get-in receipt [:verification :ok]))))))
  (testing "a failed restoration is never reported as unchanged"
    (let [receipt (mcp-helper/terminal-receipt
                   {:kernel rollback-failed-kernel
                    :verification (assoc profile-result :ok false)
                    :plan fixture-plan})]
      (is (= "rollback-failed" (:status receipt)))
      (is (false? (:committed receipt)))
      (is (false? (:restored receipt)))
      (is (false? (:source_unchanged receipt))
          "MCP-OP-HELPER-020: it NEVER claims unchanged")
      (is (= (:unrestored_files rollback-failed-kernel) (:files receipt))
          "it names the files the kernel could not restore")
      (is (= (:recovery_required rollback-failed-kernel)
             (:recovery_required receipt))
          "the kernel's recovery-required evidence is carried through")
      (is (contains? #{0 nil} (:source_retired receipt))
          (str "a rollback that did not complete knows even less about what "
               "was retired than one that did, so it may never report a "
               "positive count: " (pr-str (:source_retired receipt)))))))

;; @spec MCP-OP-HELPER-020
;; @spec MCP-OP-HELPER-022
(deftest a-committed-receipt-reflects-exactly-the-profile-result-it-was-given
  (let [receipt (mcp-helper/terminal-receipt
                 {:kernel committed-kernel
                  :verification profile-result
                  :plan fixture-plan})
        verification (:verification receipt)]
    (is (true? (:committed receipt)))
    (is (= "committed" (:kernel_status receipt))
        "the kernel's outcome is its own field")
    (testing "the executed profile names itself and its TYPED checks, and the
              receipt reflects the injected numbers exactly"
      (is (= "checks-completed" (:status verification))
          "the verification status is about the checks, not about the commit")
      (is (= "helper-proof" (:profile verification)))
      (is (= 28 (:structural_callers verification)))
      (is (= 24 (:helper_behaviors verification)))
      (is (= 0 (:compiled_callers verification)))
      (is (true? (:fresh_process verification)))
      (is (true? (:ok verification))))
    (testing "an ambiguous coverage integer is not a typed check"
      (is (not (contains? verification :covered_callers))
          "a bare covered_callers integer cannot say WHAT was covered"))))

;; @spec MCP-OP-HELPER-022
(deftest a-compiled-caller-claim-must-be-backed-by-compiles-that-happened
  (testing "a profile that reports compiled callers without the per-compile
            evidence must not reach the receipt as a claim"
    (let [receipt (mcp-helper/terminal-receipt
                   {:kernel committed-kernel
                    :verification (assoc profile-result
                                         :compiled_callers 28
                                         :compiled_evidence [])
                    :plan fixture-plan})
          verification (:verification receipt)]
      (is (or (zero? (:compiled_callers verification))
              (= (:compiled_callers verification)
                 (count (:compiled_evidence verification))))
          "claiming 28 compiled callers with no evidence of any compile is the
           false green this witness exists to prevent")
      (is (not (true? (:ok verification)))
          "and the profile result is not ok when its own claim is unbacked"))))

;; @spec MCP-OP-HELPER-020
;; @spec MCP-OP-HELPER-022
(deftest with-no-evidence-the-mapper-claims-nothing
  (testing "empty or missing kernel and profile facts must never become a
            proof, a restoration, or a fresh-process claim"
    (doseq [input [{}
                   {:kernel {} :verification {}}
                   {:kernel {:status :verification-failed} :verification nil}]]
      (testing (pr-str input)
        (let [receipt (mcp-helper/terminal-receipt input)
              verification (:verification receipt)]
          (is (not (true? (:committed receipt))))
          (is (not (true? (:restored receipt)))
              "restoration is never assumed")
          (is (not (true? (:source_unchanged receipt)))
              "unchanged is a claim, and it needs evidence")
          (is (not (true? (:ok verification)))
              "no proof without a profile result")
          (is (not (true? (:fresh_process verification)))
              "fresh_process is a fact about an execution that happened")
          (is (= "unknown" (:status verification))
              "the honest answer is unknown, never a manufactured number")
          (is (not-any? number? ((juxt :structural_callers :helper_behaviors
                                       :compiled_callers)
                                 verification))
              "and it invents no counts"))))))


;; ---------------------------------------------------------------------------
;; a throw AFTER staging (Astra, 05:56/05:59)
;;
;; Everything below runs `execute!` on a real materialized tree and then
;; compares EVERY file against the fixture's PRE bytes. A rollback that is
;; asserted only through the receipt it wrote is a receipt checking itself; the
;; filesystem is the witness here.
;;
;; SEAMS. Witness (b) uses a REAL production seam: `:receipt-dir` in the config
;; map, pointed at a directory this process cannot create, so the receipt
;; publication genuinely fails where it fails in the field. Witness (a) needs a
;; proof step that THROWS, and `execute!` today calls `run-proof!` directly with
;; no injection point, so it is written against a DOCUMENTED seam name -- an
;; optional `:run-proof!` fn in the same config map -- which the boundary must
;; expose. Nothing here stubs production.

(defn- materialize!
  [root tree]
  (doseq [[path source] tree]
    (let [target (io/file root path)]
      (io/make-parents target)
      (spit target source)))
  root)

(defn- tree-on-disk
  [root paths]
  (into {} (keep (fn [path]
                   (let [file (io/file root path)]
                     (when (.isFile file) [path (slurp file)]))))
        paths))

(defn- with-materialized-happy-tree
  "Materialize the happy PRE tree, run `f` with the project root, and always
  clean up. `f` returns whatever it likes; the tree is handed back with it."
  [label f]
  (let [root (io/file tmp-root (str label "-" (System/nanoTime)))
        pre (tree-of :happy :pre)]
    (try
      (materialize! root pre)
      (let [result (f (str root))]
        {:result result
         :pre pre
         :after (tree-on-disk root (keys pre))
         :destination-present? (.isFile (io/file root fixture/dest-file))})
      (finally (delete-tree! root)))))

(defn- assert-restored!
  "Every PRE byte is back and the destination is gone."
  [{:keys [pre after destination-present?]}]
  (doseq [[path source] pre]
    (testing path
      (is (= source (get after path))
          "restored byte-for-byte from the pre-extraction tree")))
  (is (= (set (keys pre)) (set (keys after)))
      "and no file the transaction touched was left behind or deleted")
  (is (false? destination-present?)
      "and the destination the transaction created is gone"))

;; @spec MCP-OP-HELPER-020
(deftest a-proof-that-throws-after-staging-restores-every-byte
  (testing "an exception from the verification step is a proof that did not
            complete, and an incomplete proof may never leave a commit standing.
            SEAM: `:run-proof!` in the execute! config map -- the boundary must
            expose it; today `execute!` calls `run-proof!` directly."
    (let [outcome (with-materialized-happy-tree
                    "proof-throws"
                    (fn [root]
                      (mcp-helper/execute!
                       {:verification-profiles configured-profiles
                        :run-proof! (fn [& _]
                                      (throw (ex-info "proof exploded" {})))}
                       (fixture/request
                        {:workspace_root root
                         :verification {:profile "noop-proof"}}))))
          receipt (:result outcome)]
      (assert-restored! outcome)
      (is (not (true? (:committed receipt))) "never committed")
      (is (not (true? (:ok receipt))))
      (is (true? (:restored receipt)) "the receipt reports the restoration")
      (assert-no-completion-claim! receipt)
      (is (true? (:source_unchanged receipt))
          "and claims unchanged only because the rollback verified")
      (is (contains? #{"verification-failed" "verification-timeout"}
                     (:status receipt))
          (str "the kernel's typed failure state, never a bare error: "
               (pr-str (:status receipt))))
      (is (false? (:destination_created receipt)))
      (is (seq (:restoration_read_back receipt))
          "carrying the kernel's own read-back of what it restored")
      (is (some? (:cause_error receipt))
          "and naming the exception that caused it"))))

;; @spec MCP-OP-HELPER-020
(deftest a-receipt-publication-that-fails-restores-every-byte
  (testing "publishing the receipt can fail after the bytes are staged -- here
            because the receipt directory cannot be created. The tree must come
            back exactly as it was, and `source_unchanged` may be claimed only
            because the rollback's read-back proves it."
    (let [locked (io/file tmp-root (str "locked-" (System/nanoTime)))]
      (try
        (.mkdirs locked)
        (.setWritable locked false false)
        (let [outcome (with-materialized-happy-tree
                        "receipt-fails"
                        (fn [root]
                          (mcp-helper/execute!
                           {:verification-profiles configured-profiles
                            :receipt-dir (str (io/file locked "receipts"))}
                           (fixture/request
                            {:workspace_root root
                             :verification {:profile "noop-proof"}}))))
              receipt (:result outcome)]
          (assert-restored! outcome)
          (is (not (true? (:committed receipt))) "never committed")
          (is (not (true? (:ok receipt))))
          (is (string? (:status receipt)) "a typed terminal state")
          (is (not= "committed" (:status receipt)))
          (if (true? (:restored receipt))
            (do
              (is (true? (:source_unchanged receipt))
                  "unchanged is claimed only alongside a verified restoration")
              (is (seq (:restoration_read_back receipt))
                  "and the read-back is the evidence for that claim"))
            (do
              (is (false? (:source_unchanged receipt))
                  "a rollback that did not verify NEVER claims unchanged")
              (is (seq (:files receipt)) "it names the files")
              (is (some? (:recovery_required receipt))))))
        (finally
          (.setWritable locked true true)
          (delete-tree! locked))))))


;; @spec MCP-OP-HELPER-009
(deftest the-details-path-is-published-outside-the-workspace
  (testing "the receipt carries counts only; the per-caller detail goes to a
            `details_path`. That path is the BOUNDARY's fact -- the pure
            receipt names none -- and it must live in the kernel's own
            local-state receipt directory, never inside the workspace this
            verb just mutated."
    (let [receipt-dir (io/file tmp-root (str "receipts-" (System/nanoTime)))
          outcome (with-materialized-happy-tree
                    "details-path"
                    (fn [root]
                      (mcp-helper/execute!
                       {:verification-profiles configured-profiles
                        :receipt-dir (str receipt-dir)}
                       (fixture/request
                        {:workspace_root root
                         :verification {:profile "helper-proof"}}))))
          receipt (:result outcome)
          details (:details_path receipt)]
      (try
        (is (string? details)
            (str "the boundary publishes a details_path: " (pr-str receipt)))
        (when (string? details)
          (is (str/starts-with? details (str receipt-dir))
              "under the kernel receipt directory it was configured with")
          (is (not (str/includes? details "/acid/"))
              "and never inside the workspace tree it just mutated"))
        (is (not (contains? receipt :files))
            "and the receipt itself still carries no file list")
        (finally (delete-tree! receipt-dir))))))

;; ---------------------------------------------------------------------------
;; the public boundary: a project that lives UNDER an ancestor named `src`

;; @spec MCP-OP-HELPER-001
;; @spec MCP-OP-HELPER-012
(deftest an-ancestor-directory-named-src-does-not-influence-the-destination
  (testing "the project root is <tmp>/src/ancestor/project, so a path walk
            that looks for the nearest `src` above a file, rather than
            resolving relative to the project root, infers a namespace like
            ancestor.project.src.acid.web.response and writes to the wrong
            place. The destination namespace must equal `to.lib` exactly and
            its path must be project-relative."
    (let [root (io/file tmp-root "src" "ancestor" "project")]
      (try
        (materialize! root (tree-of :happy :pre))
        (let [result (mcp-helper/plan
                      (fixture/request {:workspace_root (str root)})
                      configured-profiles)]
          (if (:ok result)
            (let [destination (get-in result [:plan :destination])]
              (is (= fixture/dest-lib (:lib destination))
                  "the destination namespace is exactly to.lib")
              (is (= fixture/dest-file (:file destination))
                  "and its path is project-relative")
              (is (not (str/includes? (str (:file destination)) "ancestor"))
                  "no ancestor directory leaks into the path")
              (is (not (str/starts-with? (str (:file destination)) "/"))
                  "and it is not absolute"))
            (do
              (is (false? (:ok result)))
              (is (some? (:limitation result))
                  "if the seam cannot take an explicit project-relative path,
                   the refusal must NAME that limitation rather than pass
                   silently or guess a namespace")
              (is (nil? (:next_call result))))))
        (finally (delete-tree! (io/file tmp-root "src")))))))

;; ---------------------------------------------------------------------------
;; THE FENCE REVIEW (Sol, r1, candidate ee03b49a) -- one witness per finding
;;
;; Each witness reproduces the reviewer's OWN probe rather than a paraphrase of
;; it, so a fix is measured against the thing that actually failed. The review
;; is docs/observations/2026-09-05-helper-extraction-fence-review-r1.md; its
;; verdict was NO-GO on eleven executed findings, six of them merge blockers.
;;
;; The reviewer's closing note is the reason these live here rather than in the
;; planner file: "the current test only checks that the result begins with the
;; path supplied by its own test config." A witness that can only see what the
;; test handed the code is not a fence.

(defn- with-workspace
  "Materialize `tree` under a fresh root, call `(f root)`, always clean up."
  [label tree f]
  (let [root (io/file tmp-root (str label "-" (System/nanoTime)))]
    (try
      ;; an empty tree materializes no files, and without this the directory
      ;; never exists: a child process given it as its cwd fails to LAUNCH, and
      ;; the witness measures its own harness instead of the product
      (.mkdirs root)
      (materialize! root tree)
      (f root)
      (finally (delete-tree! root)))))

(defn- real-commit!
  "The PRODUCTION kernel commit Var that `execute!` itself defaults to.

  It is `clj-surgeon.mcp-extraction/commit!`. An earlier version of this witness
  resolved `clj-surgeon.extract/commit!`, which does not exist: `requiring-resolve`
  returned nil, the wrapper invoked nil, `execute!` caught THAT throw, and the
  witness passed with a restored-looking receipt for a tree no kernel had ever
  written. It proved nothing and said it proved the critical finding. Callers
  must fail loudly rather than inherit a nil."
  []
  (let [resolved (requiring-resolve 'clj-surgeon.mcp-extraction/commit!)]
    (when-not resolved
      (throw (ex-info (str "the production kernel commit Var did not resolve; "
                           "this witness cannot inject a post-commit throw and "
                           "must not report a result")
                      {:var 'clj-surgeon.mcp-extraction/commit!})))
    resolved))

(def ^:private mini-tree
  "The reviewer's finding-4 tree: a source file whose PATH does not end in its
  declared namespace path."
  {".clj-surgeon.edn" "{:source-roots [\"lib\"]}\n"
   "lib/odd.clj" "(ns demo.core)\n\n(defn helper [x] (inc x))\n"})

;; @spec MCP-OP-HELPER-020
(deftest finding-1-a-throw-after-a-real-kernel-commit-does-not-leave-the-write-standing
  (testing "PROBE2 throw-after-kernel-commit => {:destination-exists true,
            :source-equals-pre false}. `(extraction/commit! compiled)` is
            evaluated BEFORE the try that claims to encompass the write, so the
            first possible written byte is outside the guard that owns the
            inverse receipt.

            SEAM: a `:commit!` fn in the execute! config map, defaulting to the
            kernel's own. `:run-proof!` already exists; there is no seam for the
            commit step, and the boundary must expose one -- a throw AFTER a
            real commit cannot be witnessed any other way without redefining a
            production var."
    (let [;; what the wrapper OBSERVED between the real commit and the throw.
          ;; Without it the witness cannot tell a post-commit throw from a
          ;; pre-commit one, and a rollback of nothing looks like a rollback.
          observed (atom nil)
          resolve-error (atom nil)
          commit-var (try (real-commit!)
                          (catch Throwable error (reset! resolve-error error) nil))
          source-pre (some #(when (= fixture/source-file (:file %)) (:pre %))
                           (fixture/files :happy))
          outcome (with-materialized-happy-tree
                    "post-commit-throw"
                    (fn [root]
                      (try
                        (mcp-helper/execute!
                         {:verification-profiles configured-profiles
                          :commit!
                          (fn [compiled]
                            (let [result (commit-var compiled)
                                  destination (io/file root fixture/dest-file)
                                  source (io/file root fixture/source-file)]
                              ;; the kernel really wrote, and we can see it
                              (reset! observed
                                      {:kernel-ok (boolean (:ok result))
                                       :destination-exists (.isFile destination)
                                       :source-changed
                                       (and (.isFile source)
                                            (not= (fixture/sha256 source-pre)
                                                  (fixture/sha256 (slurp source))))})
                              (throw (ex-info "injected throw after kernel commit"
                                              {:kernel-ok (:ok result)}))))}
                         (fixture/request
                          {:workspace_root root
                           :verification {:profile "helper-proof"}}))
                        (catch Throwable error
                          {::threw true ::message (.getMessage error)}))))
          receipt (:result outcome)
          injected (atom (some? @observed))]
      (is (nil? @resolve-error)
          (str "the production kernel commit Var must resolve: "
               (some-> @resolve-error .getMessage)))
      ;; without this, an execute! that IGNORES the seam commits normally and
      ;; the restoration assertions below fail for a reason that has nothing to
      ;; do with the finding. A witness that cannot tell "not injected" from
      ;; "injected and lost" is not measuring the defect.
      (is (true? @injected)
          "the `:commit!` seam is not exposed by execute!, so the post-commit
           throw could not be injected at all. Everything below is unmeasured
           until the boundary accepts this seam.")
      (testing "and the throw really was AFTER a real write, observed from
                inside the wrapper before it threw"
        (is (true? (:kernel-ok @observed))
            (str "the real kernel returned success: " (pr-str @observed)))
        (is (true? (:destination-exists @observed))
            "the destination existed on disk at that moment")
        (is (true? (:source-changed @observed))
            "and the source no longer hashed to its pre-image, so there was
             something for the rollback below to undo"))
      (is (not (::threw receipt))
          (str "a Throwable after the kernel commit must never escape the "
               "boundary: " (pr-str receipt)))
      (when @injected
        (assert-restored! outcome)
        (is (not (true? (:committed receipt))))
        (is (string? (:status receipt)) "it comes back as a terminal receipt")
        (is (contains? #{"verification-failed" "verification-timeout"
                         "rollback-failed"}
                       (:status receipt))
            (pr-str (:status receipt)))))))

;; @spec MCP-OP-HELPER-011
(deftest finding-2-empty-configured-authority-admits-nothing-not-even-a-built-in
  (testing "PROBE configured-empty-builtins =>
            {:admitted-with-no-config [\"fast\"], :selected \"fast\",
             :preflight-with-empty-config nil}. Admission merged the server's
            own registry into the configured map, so a workspace that
            configures NOTHING still gated a write on the server's `fast`."
    (is (empty? (mcp-helper/admitted-profiles {}))
        "an empty configured map admits nothing")
    (is (empty? (mcp-helper/admitted-profiles nil))
        "and neither does nil")
    (testing "`fast` is a real server profile, and it is still not admissible
              from an empty configured authority"
      (is (nil? (get (mcp-helper/admitted-profiles {}) "fast")))
      (let [refusal (mcp-helper/plan
                     (fixture/request {:verification {:profile "fast"}})
                     {})]
        (is (false? (:ok refusal)) (pr-str refusal))
        (is (= "helper-extraction-verification-preflight-unavailable"
               (:error_type refusal)))))))

;; @spec MCP-OP-HELPER-011
(deftest finding-3-malformed-and-unrunnable-profiles-refuse-before-staging
  (testing "PROBE profile-shape-admission admitted a profile whose :commands
            held a STRING, and the malformed profile then ran a real extraction
            before ending as a timeout and rolling back. PROBE2
            missing-absolute-executable-preflight => nil, i.e. no refusal."
    (doseq [[label spec]
            [[:string-command {:acceptance :helper :timeout-ms 10
                               :commands ["/bin/true"]}]
             [:nonexistent-absolute {:commands [["/nonexistent/definitely/not-here"]]}]
             [:hot {:hot ["some.ns/law"]}]
             [:cold {:cold ["make" "test"]}]
             [:non-integer-timeout {:commands [["/bin/true"]] :timeout-ms "soon"}]
             [:empty-argv {:commands [[]]}]]]
      (testing label
        (is (nil? (get (mcp-helper/admitted-profiles {"p" spec}) "p"))
            "it is not an admissible profile")
        (let [outcome (with-materialized-happy-tree
                        (str "bad-profile-" (name label))
                        (fn [root]
                          (try
                            (mcp-helper/execute!
                             {:verification-profiles {"p" spec}}
                             (fixture/request {:workspace_root root
                                               :verification {:profile "p"}}))
                            (catch Throwable error
                              {::threw true ::message (.getMessage error)}))))
              receipt (:result outcome)]
          (is (not (::threw receipt))
              (str "a malformed profile is a typed refusal, never a throw: "
                   (pr-str receipt)))
          (is (false? (:ok receipt)) (pr-str receipt))
          (is (= "helper-extraction-verification-preflight-unavailable"
                 (:error_type receipt))
              "refused BEFORE staging")
          (is (nil? (:next_call receipt)))
          (is (false? (:destination-present? outcome))
              "and nothing reached the disk")
          (doseq [[path source] (:pre outcome)]
            (is (= source (get (:after outcome) path))
                (str path " must be untouched"))))))))

;; @spec MCP-OP-HELPER-012
(deftest finding-4-a-source-path-that-disagrees-with-its-namespace-refuses
  (testing "PROBE destination-source-path-mismatch => {:ok true, :destination
            {:file \"demo/extracted.clj\"}}. `lib/odd.clj` declares
            `(ns demo.core)`, so no source root decomposes the path; the
            boundary set the root prefix to \"\" and invented a destination at
            the project root, outside the admitted source root."
    (with-workspace
      "dest-mismatch" mini-tree
      (fn [root]
        (let [result (mcp-helper/plan
                      {:op "helper_extraction"
                       :workspace_root (str root)
                       :from {:file "lib/odd.clj"}
                       :helpers ["helper"]
                       :to {:lib "demo.extracted" :alias_policy ["ex"]}
                       :scope {:paths ["lib/**"]}
                       :verification {:profile "helper-proof"}}
                      configured-profiles)]
          (is (false? (:ok result)) (pr-str result))
          (is (= "helper-extraction-destination-not-derivable"
                 (:error_type result)))
          (is (nil? (:next_call result))
              "inventing a destination is not a continuation the server may make"))))
    (testing "and a source whose path DOES decompose puts the destination under
              that same admitted root"
      (with-workspace
        "dest-derivable"
        {".clj-surgeon.edn" "{:source-roots [\"lib\"]}\n"
         "lib/demo/core.clj" "(ns demo.core)\n\n(defn helper [x] (inc x))\n"}
        (fn [root]
          (let [result (mcp-helper/plan
                        {:op "helper_extraction"
                         :workspace_root (str root)
                         :from {:file "lib/demo/core.clj"}
                         :helpers ["helper"]
                         :to {:lib "demo.extracted" :alias_policy ["ex"]}
                         :scope {:paths ["lib/**"]}
                         :verification {:profile "helper-proof"}}
                        configured-profiles)]
            (is (:ok result) (pr-str result))
            (is (= "lib/demo/extracted.clj"
                   (get-in result [:plan :destination :file]))
                "derived from the source's own admitted root, not the project root")))))))

;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-020
(deftest finding-5-a-restored-failure-receipt-is-constant-size
  (testing "PROBE failure-receipt-growth => {:n1-bytes 282, :n1000-bytes 37029,
            :n1000-restored-files 1000, :n1000-read-back 1000}. A receipt whose
            size tracks the number of files it restored is a file list wearing a
            different name."
    (let [receipt-for (fn [n]
                        (let [files (mapv #(str "src/acid/app/f" % ".clj")
                                          (range n))]
                          (mcp-helper/terminal-receipt
                           {:kernel {:status :verification-failed
                                     :restored true
                                     :restored_files files
                                     :restoration_read_back
                                     (into {} (map (fn [f] [f "sha"])) files)
                                     :destination_removed true
                                     :details_path "/local/state/details.edn"}
                            :verification (assoc profile-result :ok false)
                            :plan fixture-plan})))
          one (receipt-for 1)
          many (receipt-for 1000)
          bytes-of #(count (pr-str %))]
      (is (< (bytes-of many) (* 2 (bytes-of one)))
          (str "the receipt must not grow with the restored-file count: "
               {:n1 (bytes-of one) :n1000 (bytes-of many)}))
      (is (< (bytes-of many) 4096)
          "and it stays inside a constant bound")
      (is (not (contains? many :restored_files))
          "the manifest belongs in the details file, not the receipt")
      (is (some? (:details_path many))
          "which the receipt names once")
      (testing "the constant-size EVIDENCE survives: a count and an aggregate"
        (let [evidence (:restoration_read_back many)]
          (is (= 1000 (:files evidence))
              "how many files were restored")
          (is (string? (:aggregate_sha256 evidence))
              "one digest over all of them, not one entry per file")
          (is (< (abs (- (count (pr-str evidence))
                         (count (pr-str (:restoration_read_back one)))))
                 16)
              "and its printed size moves only by the digits of the count
               itself between 1 file and 1000, never by the files"))))
    (testing "rollback-failed keeps its explicit unrestored-file authority"
      (let [receipt (mcp-helper/terminal-receipt
                     {:kernel rollback-failed-kernel
                      :verification (assoc profile-result :ok false)
                      :plan fixture-plan})]
        (is (seq (:files receipt)))
        (is (some? (:recovery_required receipt)))
        (is (false? (:source_unchanged receipt)))))))

;; @spec MCP-OP-HELPER-009
(deftest finding-6-a-receipt-directory-inside-the-workspace-is-refused
  (testing "PROBE details-dir-inside-workspace => {:status \"committed\",
            :inside-workspace? true}. The verb published its per-caller detail
            INTO the tree it had just mutated."
    (doseq [[label relative]
            [[:direct ".local-receipts"]
             [:nested "src/.receipts"]]]
      (testing label
        (let [outcome (with-materialized-happy-tree
                        (str "receipt-inside-" (name label))
                        (fn [root]
                          (let [result (mcp-helper/execute!
                                        {:verification-profiles configured-profiles
                                         :receipt-dir (str (io/file root relative))}
                                        (fixture/request
                                         {:workspace_root root
                                          :verification {:profile "helper-proof"}}))]
                            {:result result
                             :published (.exists (io/file root relative))})))
              {:keys [result published]} (:result outcome)]
          (is (false? (:ok result)) (pr-str result))
          (is (string? (:error_type result))
              "a typed refusal, not a committed receipt")
          (is (not (true? (:committed result))))
          (is (false? published)
              "and nothing was published inside the workspace"))))))

;; @spec MCP-OP-HELPER-012
(deftest finding-7-symlinks-under-an-admitted-root-are-pruned-not-fatal
  (testing "PROBE2 symlink-walk => {:ok false, :error_type
            \"helper-extraction-unreadable-source\", :file \"src/escape.clj\"}.
            Fail-closed is right for confidentiality and wrong for the fence
            rule: symlinks a walk produces are DROPPED, and one unrelated
            symlink under an admitted root could deny every extraction."
    (with-workspace
      "symlink-walk" (tree-of :happy :pre)
      (fn [root]
        (let [outside (io/file tmp-root (str "outside-" (System/nanoTime) ".clj"))]
          (try
            (spit outside "(ns escapee)\n\n(defn f [] :out)\n")
            (java.nio.file.Files/createSymbolicLink
             (.toPath (io/file root "src/escape.clj"))
             (.toPath outside)
             (make-array java.nio.file.attribute.FileAttribute 0))
            (java.nio.file.Files/createSymbolicLink
             (.toPath (io/file root "src/inward.clj"))
             (.toPath (io/file root "src/acid/app/m01.clj"))
             (make-array java.nio.file.attribute.FileAttribute 0))
            (let [result (mcp-helper/plan
                          (fixture/request {:workspace_root (str root)})
                          configured-profiles)]
              (is (:ok result)
                  (str "an outward AND an inward symlink are both pruned, and "
                       "the operation still succeeds: " (pr-str result)))
              (let [planned (set (map :file (get-in result [:plan :files])))]
                (is (not (contains? planned "src/escape.clj"))
                    "the outward link is not counted")
                (is (not (contains? planned "src/inward.clj"))
                    "and neither is the inward one")))
            (finally (.delete outside))))))))

;; @spec MCP-OP-HELPER-012
(deftest finding-8-a-traversing-configured-source-root-is-refused-explicitly
  (testing "PROBE configured-root-traversal => {:ok true}. The outside file was
            not enumerated, which is safe, but the boundary called the
            traversal an ADMITTED root while the closure receipt still said
            [\"src\" \"test\"]. Admission and closure evidence must not disagree."
    (with-workspace
      "root-traversal"
      (assoc (tree-of :happy :pre)
             ".clj-surgeon.edn" "{:source-roots [\"src\" \"../sibling\"]}\n")
      (fn [root]
        (let [result (mcp-helper/plan
                      (fixture/request {:workspace_root (str root)})
                      configured-profiles)]
          (if (:ok result)
            (is (not (some #(str/includes? (str %) "..")
                           (get-in result [:receipt :closure :roots])))
                (str "a traversing root is never reported as admitted: "
                     (pr-str (get-in result [:receipt :closure :roots]))))
            (do
              (is (string? (:error_type result))
                  "or it is refused explicitly, never silently ignored")
              (is (nil? (:next_call result))))))))))

;; @spec MCP-OP-HELPER-020
(deftest finding-9-a-timed-out-proof-still-reports-that-a-fresh-process-ran
  (testing "PROBE subprocess-timeout => {:timed_out true, :fresh_process false}
            with process evidence showing the child ran for 58.1 ms. A timed-out
            child has :exit nil, and `fresh_process` was computed from
            `(some :exit outcomes)`, so a process that demonstrably started was
            reported as not fresh."
    (with-workspace
      "proof-timeout" {}
      (fn [root]
        (let [proof (mcp-helper/run-proof!
                     (str root) "slow"
                     {:synchronous? true :rollback-capable? true
                      :fresh-process? true
                      :commands [["/bin/sleep" "1"]] :timeout-ms 40})]
          (is (true? (:timed_out proof)))
          (is (false? (:ok proof)))
          (is (true? (:fresh_process proof))
              (str "the child started; whether it finished is a different fact: "
                   (pr-str (:process_evidence proof)))))))))

;; @spec MCP-OP-HELPER-010
(deftest finding-10-the-public-uninitialized-refusal-carries-next-call-nil
  (testing "the reviewer's structural probe of `handle-helper-extraction` found
            the server-not-initialized map has no :next_call at all. Every
            refusal the PUBLIC handler emits goes through the closed envelope,
            initialization and workspace routing included."
    (let [captured (atom nil)]
      (try
        (mcp-tool/init! nil)
        (mcp-tool/handle-helper-extraction
         nil
         (json/parse-string (json/generate-string (fixture/request)) true)
         (fn [content error? structured]
           (reset! captured {:content content :error? error? :result structured})))
        (let [{:keys [error? result]} @captured]
          (is (true? error?))
          (is (false? (:ok result)))
          (is (= "helper_extraction" (:operation result)))
          (is (= "server-not-initialized" (:error_type result)))
          (is (contains? result :next_call)
              "the field is present and explicitly null, never merely absent")
          (is (nil? (:next_call result)))
          (is (true? (:source_unchanged result))))
        (finally (mcp-tool/init! nil))))))

;; @spec MCP-OP-HELPER-001
(deftest finding-11-the-registered-output-schema-names-the-rollback-authority
  (testing "the registered output property key set omitted `files`,
            `recovery_required` and `cause_error` -- the three fields a
            rollback-failed receipt exists to carry -- while requiring
            `elapsed_ms` that a pre-staging refusal never supplies."
    (let [schema mcp-schema/helper-extraction-output-schema
          properties (set (keys (:properties schema)))]
      (doseq [field ["files" "recovery_required" "cause_error"]]
        (is (contains? properties field)
            (str "a rollback-failed receipt carries " field
                 ", so the schema must declare it")))
      ;; The reviewer also read `elapsed_ms` as wrongly required. That one is
      ;; WITHDRAWN, verified at the wire by the boundary builder:
      ;; `mcp-operation/finalize-result` (line 39) assoc's :elapsed_ms onto
      ;; every published result, refusals included, and
      ;; `mcp-server-test/exposes-exactly-nine-typed-tools` requires every
      ;; registered tool's output schema to require it. The refusal the review
      ;; quoted was the raw domain map, not the published receipt.
      (is (contains? (set (:required schema)) "ok")))))

;; @spec MCP-OP-HELPER-001
;; @spec MCP-OP-HELPER-009
(deftest the-wire-carries-the-whole-receipt-through-the-public-tool-fn
  (testing "Astra's wire witness: one full execute! through the PUBLIC callback,
            asserting what an agent actually receives. A receipt that is correct
            inside the boundary and hollow on the wire is not a receipt."
    (with-materialized-happy-tree
      "wire"
      (fn [root]
        (let [receipt-dir (io/file tmp-root (str "wire-receipts-" (System/nanoTime)))
              captured (atom nil)]
          (try
            (.mkdirs receipt-dir)
            (mcp-tool/init! {:project-root (str root)
                             :receipt-dir (str receipt-dir)
                             :verification-profiles configured-profiles})
            (mcp-tool/handle-helper-extraction
             nil
             (json/parse-string
              (json/generate-string
               (fixture/request {:workspace_root (str root)
                                 :verification {:profile "helper-proof"}}))
              true)
             (fn [content error? structured]
               (reset! captured {:content content :error? error?
                                 :result structured})))
            (let [{:keys [content error? result]} @captured
                  summary (str content)]
              (is (false? error?) (pr-str result))
              (is (:ok result) (pr-str result))
              (is (true? (:committed result)))
              (testing "structuredContent carries the counts at the TOP level"
                (is (= (:helpers fixture/canonical-counts) (:helpers result)))
                (is (= (:caller-files fixture/canonical-counts)
                       (:caller_files result)))
                (is (= (:sites fixture/canonical-counts) (:sites result)))
                (is (= fixture/canonical-receipt-partition (:partition result)))
                (is (= (:alias-histogram fixture/canonical-counts)
                       (into (sorted-map) (:alias_histogram result))))
                (is (map? (:closure result)))
                (is (= fixture/admitted-roots (get-in result [:closure :roots]))))
              (testing "with no nulls where a number or a map belongs"
                (doseq [field [:helpers :caller_files :sites :partition
                               :closure :alias_histogram :verification]]
                  (is (some? (get result field))
                      (str field " is null on the wire"))))
              (testing "and the verification is TYPED, never a bare count"
                (is (= "helper-proof" (get-in result [:verification :profile])))
                (is (= "checks-completed" (get-in result [:verification :status])))
                (is (true? (get-in result [:verification :fresh_process])))
                (is (not (contains? (:verification result) :covered_callers))))
              (testing "and the human-readable summary says the same numbers"
                (doseq [needle [(str (:helpers fixture/canonical-counts))
                                (str (:caller-files fixture/canonical-counts))
                                (str (:sites fixture/canonical-counts))]]
                  (is (str/includes? summary needle)
                      (str "the content summary omits " needle ": " summary)))))
            (finally
              (mcp-tool/init! nil)
              (delete-tree! receipt-dir))))))))

;; ---------------------------------------------------------------------------
;; the REAL wire, not a mapper fixture
;;
;; The two witnesses below drive `execute!` all the way through the public
;; tool-fn and read what an agent actually receives. The mapper witnesses above
;; assert that injected facts are mapped faithfully; these assert that the facts
;; reaching the mapper are the real ones. Both are needed, and neither
;; substitutes for the other.

(def ^:private failing-proof-profiles
  "A configured profile whose command really runs and really FAILS, so the
  failure path is exercised end to end rather than simulated."
  {"failing-proof" {:commands [["/bin/false"]]}})

(defn- read-details
  [details-path]
  (when (and (string? details-path) (.isFile (io/file details-path)))
    (slurp details-path)))

;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-020
(deftest a-real-failing-proof-returns-a-bounded-receipt-on-the-wire
  (testing "the whole failure path through the PUBLIC callback: the proof runs,
            really fails, the kernel rolls back, and what comes out on the wire
            must be bounded no matter how many files were staged. The per-file
            evidence lives in the external details artifact, not in the reply."
    (with-materialized-happy-tree
      "wire-failing-proof"
      (fn [root]
        (let [receipt-dir (io/file tmp-root (str "fail-receipts-" (System/nanoTime)))
              captured (atom nil)]
          (try
            (.mkdirs receipt-dir)
            (mcp-tool/init! {:project-root (str root)
                             :receipt-dir (str receipt-dir)
                             :verification-profiles failing-proof-profiles})
            (mcp-tool/handle-helper-extraction
             nil
             (json/parse-string
              (json/generate-string
               (fixture/request {:workspace_root (str root)
                                 :verification {:profile "failing-proof"}}))
              true)
             (fn [content error? structured]
               (reset! captured {:content content :error? error?
                                 :result structured})))
            (let [{:keys [content result]} @captured
                  rendered (str content)]
              (is (some? result) "the callback answered")
              (is (not (true? (:committed result)))
                  "a failing proof never leaves a commit standing")
              (is (contains? #{"verification-failed" "verification-timeout"}
                             (:status result))
                  (pr-str (:status result)))
              (testing "the receipt is BOUNDED: no per-file lists on the wire"
                (is (not (contains? result :restored_files))
                    "the restored-file manifest is not a wire field")
                (when (true? (:restored result))
                  (assert-no-completion-claim! result))
                (let [evidence (:restoration_read_back result)]
                  (when (some? evidence)
                    (is (not (and (map? evidence)
                                  (some #(str/includes? (str %) ".clj")
                                        (keys evidence))))
                        "and neither are the per-file hashes")
                    (is (some? (:aggregate_sha256 evidence))
                        "the aggregate digest stands in for them")))
                (is (< (count (pr-str result)) 4096)
                    (str "the whole structured receipt stays inside a constant "
                         "bound; " (count (pr-str result)) " bytes for a "
                         (:changed-files fixture/canonical-counts)
                         "-file extraction")))
              (testing "and it prescribes nothing it cannot offer"
                (when (nil? (:next_call result))
                  (is (not (re-find #"(?i)retry" rendered))
                      (str "a receipt whose next_call is nil must not tell the "
                           "reader to retry: there is nothing to retry WITH, "
                           "and a prescription with no continuation is how a "
                           "caller ends up hammering a failing proof. "
                           rendered))))
              (testing "the per-file evidence is in the external details artifact"
                (let [details-path (:details_path result)
                      details (read-details details-path)]
                  (is (string? details-path))
                  (is (str/starts-with? (str details-path) (str receipt-dir))
                      "under the local-state receipt directory")
                  (is (not (str/starts-with? (str details-path) (str root)))
                      "and never inside the workspace it mutated")
                  (is (some? details)
                      (str "the details artifact exists: " details-path))
                  (when details
                    (is (str/includes? details "src/acid/web/http.clj")
                        "carrying the per-file evidence the receipt omits")
                    (is (or (str/includes? details "/bin/false")
                            (str/includes? details "failing-proof"))
                        "and the proof's own failure output")))))
            (finally
              (mcp-tool/init! nil)
              (delete-tree! receipt-dir))))))))

;; @spec MCP-OP-HELPER-011
(deftest admission-is-rooted-in-the-requests-workspace-not-the-servers
  (testing "a server started against one workspace must not lend its own
            verification authority to a request routed to another. The seam is
            the router's `:workspace-context-factory`, which is how the HTTP
            server publishes a per-workspace context in production."
    (let [server-ws (io/file tmp-root (str "server-ws-" (System/nanoTime)))
          request-ws (io/file tmp-root (str "request-ws-" (System/nanoTime)))
          receipt-dir (io/file tmp-root (str "route-receipts-" (System/nanoTime)))
          call (fn [profile]
                 (let [captured (atom nil)]
                   (mcp-tool/handle-helper-extraction
                    nil
                    (json/parse-string
                     (json/generate-string
                      (fixture/request {:workspace_root (str request-ws)
                                        :verification {:profile profile}}))
                     true)
                    (fn [_content _error? structured] (reset! captured structured)))
                   @captured))]
      (try
        (materialize! server-ws (tree-of :happy :pre))
        (materialize! request-ws (tree-of :happy :pre))
        (.mkdirs receipt-dir)
        (mcp-tool/init!
         {:project-root (str server-ws)
          :receipt-dir (str receipt-dir)
          ;; X: the SERVER's own authority
          :verification-profiles {"server-only" {:commands [["/bin/true"]]}}
          :workspace-context-factory
          (fn [workspace-root]
            ;; Y: the authority of the workspace the request names
            (if (= (str workspace-root) (str request-ws))
              {:verification-profiles {"workspace-only" {:commands [["/bin/true"]]}}}
              {}))})
        (testing "naming the SERVER's profile X refuses, even though the server
                  really does have it"
          (let [result (call "server-only")]
            (is (false? (:ok result)) (pr-str result))
            (is (= "helper-extraction-verification-preflight-unavailable"
                   (:error_type result))
                "the server's authority does not travel to another workspace")
            (is (nil? (:next_call result)))))
        (testing "and naming the REQUEST workspace's profile Y proceeds"
          (let [result (call "workspace-only")]
            (is (:ok result)
                (str "the routed workspace's own configured profile is the one "
                     "that governs: " (pr-str result)))
            (is (true? (:committed result)))
            (is (= "workspace-only" (get-in result [:verification :profile])))))
        (finally
          (mcp-tool/init! nil)
          (delete-tree! server-ws)
          (delete-tree! request-ws)
          (delete-tree! receipt-dir))))))

;; ---------------------------------------------------------------------------
;; THE SCHEMA MATRIX (MCP-OP-HELPER-020)
;;
;; `mcp-schema/helper-extraction-output-schema` declares the receipt's five
;; faces as an `:oneOf` matrix. The witnesses below are GENERATED FROM that
;; matrix rather than transcribed from it, so a variant that gains a required
;; field or a pinned constant is covered on the next run without anyone
;; remembering to add a case. A hand-listed matrix test drifts from the schema
;; the moment the schema moves, and drifts silently.
;;
;; The exemplars come from real output wherever the runtime can produce it:
;; the four terminal faces are mapped by the production `terminal-receipt` from
;; injected kernel facts, so what is validated is a receipt this server can
;; actually emit rather than a literal written to satisfy its own assertion.

(defn- disposition-alternatives
  "The `exactly one of these` alternatives a branch declares, normalized to
  `[{:required [field]} ...]`.

  Accepts the declarative row (`:exactly-one-of`, either field names or
  `{:required [...]}` maps) and the compiled JSON-Schema form (`:oneOf`), and
  REFUSES anything else rather than quietly treating it as no constraint."
  [branch]
  (when-let [row (:exactly-one branch)]
    (mapcat (fn [group]
              (mapv (fn [alternative]
                      (cond
                        (string? alternative) {:required [alternative]}
                        (and (map? alternative) (seq (:required alternative)))
                        {:required (vec (:required alternative))}
                        :else
                        (throw (ex-info
                                (str "an exactly-one alternative this witness "
                                     "cannot read; teach it rather than let "
                                     "the constraint go unchecked")
                                {:branch (:title branch)
                                 :alternative alternative}))))
                    group))
            row)))

(def ^:private declared-types
  "field -> the JSON type(s) the schema declares for it, from the OUTER
  properties map. The per-branch rows state requiredness and constants; the
  types live once, above them, and apply wherever the field appears."
  (into {}
        (keep (fn [[field spec]]
                (when-let [declared (:type spec)] [field declared])))
        (:properties mcp-schema/helper-extraction-output-schema)))

(defn- type-ok?
  [declared value]
  (let [types (if (coll? declared) (set declared) #{declared})]
    (boolean
     (some (fn [t]
             (case t
               "integer" (integer? value)
               "number" (number? value)
               "boolean" (boolean? value)
               "string" (string? value)
               "object" (map? value)
               "array" (sequential? value)
               "null" (nil? value)
               ;; an unknown type name must not read as satisfied
               (throw (ex-info "a JSON type this witness cannot check"
                               {:type t}))))
           types))))

(defn- wrong-typed-value
  "A value of a type the field does not declare."
  [declared]
  (let [types (if (coll? declared) (set declared) #{declared})]
    (cond
      (not (contains? types "string")) "not-of-the-declared-type"
      (not (contains? types "integer")) 42
      (not (contains? types "boolean")) true
      :else {})))

(defn- alternative-satisfied?
  "Does `nested` wear this face of an object that declares alternatives?

  Required subkeys present, every pinned const and enum honoured, and NO
  forbidden subkey present -- the last is what stops a not-run verification
  smuggling in the typed counts of a proof that never ran."
  [alternative nested]
  (and (every? #(contains? nested (keyword %)) (:required alternative))
       (every? (fn [[sub constraint]]
                 (let [present? (contains? nested (keyword sub))
                       value (get nested (keyword sub))]
                   (cond
                     (not present?) (not (contains? constraint :const))
                     (contains? constraint :const) (= (:const constraint) value)
                     (:enum constraint) (contains? (set (:enum constraint)) value)
                     :else true)))
               (:properties alternative))
       (not-any? #(contains? nested (keyword %)) (:forbidden alternative))))

(defn- schema-check
  "Validate `receipt` against one `:oneOf` branch. Returns nil when valid, or a
  keyword naming the first violation.

  This understands EXACTLY the JSON-Schema constructs the matrix uses, and
  THROWS on any construct it does not — a validator that silently ignores an
  unknown keyword would pass a receipt it never checked, which is the same
  false green this suite exists to catch."
  [branch receipt]
  (let [known #{:title :description :properties :required :not :objects
                :exactly-one :allOf}]
    (when-let [unknown (seq (remove known (keys branch)))]
      (throw (ex-info (str "the schema matrix grew a construct this witness "
                           "does not validate; teach it or the branch goes "
                           "unchecked")
                      {:unknown (vec unknown) :branch (:title branch)}))))
  (or
   ;; :not {:required [...]} -- the refusal branch's "carries no status"
   (when-let [forbidden (get-in branch [:not :required])]
     (when (every? #(contains? receipt (keyword %)) forbidden)
       :forbidden-field-present))
   ;; the compiled `:allOf` form of the same rule. It is checked through the
   ;; declarative `:exactly-one` row below rather than twice; what matters is
   ;; that a branch carrying `:allOf` and NO `:exactly-one` never slips past
   ;; unvalidated.
   (when (and (seq (:allOf branch)) (empty? (:exactly-one branch)))
     (throw (ex-info (str "a branch carries the compiled :allOf form with no "
                          ":exactly-one row to read it from")
                     {:branch (:title branch)})))
   ;; EXACTLY ONE OF, e.g. a terminal face carries `details_path` or
   ;; `details_unavailable` and never both: an absent artifact is said out loud
   ;; rather than left as a missing path, because a caller reads silence as
   ;; nothing-more-to-see, and BOTH would be the receipt contradicting itself
   ;; about whether the detail exists.
   (when-let [alternatives (disposition-alternatives branch)]
     (let [satisfied (count (filter (fn [alternative]
                                      (every? #(contains? receipt (keyword %))
                                              (:required alternative)))
                                    alternatives))]
       (when-not (= 1 satisfied)
         (if (zero? satisfied) :no-disposition :more-than-one-disposition))))
   ;; :not {:anyOf [{:required [f]} ...]} -- the compiled `:absent` row, e.g.
   ;; rollback-failed must not carry source_retired at all, because how much of
   ;; the source is still defined is genuinely not knowable from that receipt
   (when-let [alternatives (get-in branch [:not :anyOf])]
     (when (some (fn [alternative]
                   (every? #(contains? receipt (keyword %))
                           (:required alternative)))
                 alternatives)
       :forbidden-field-present))
   (some (fn [field]
           (when-not (contains? receipt (keyword field)) :missing-required))
         (:required branch))
   ;; the declared TYPE of every field the receipt actually carries. A count
   ;; that arrives as a string reads fine in a log and breaks the first caller
   ;; that does arithmetic on it.
   (some (fn [[field declared]]
           (when (and (contains? receipt (keyword field))
                      (not (type-ok? declared (get receipt (keyword field)))))
             :type-mismatch))
         declared-types)
   (some (fn [[field constraint]]
           (let [present? (contains? receipt (keyword field))
                 value (get receipt (keyword field))]
             (cond
               (not present?) nil
               (and (contains? constraint :const)
                    (not= (:const constraint) value)) :const-mismatch
               (and (= "null" (:type constraint)) (some? value)) :type-mismatch
               (and (:enum constraint)
                    (not (contains? (set (:enum constraint)) value))) :enum-mismatch
               :else nil)))
         (:properties branch))
   ;; @spec MCP-OP-HELPER-020 -- the NESTED shapes. Sol's r4 kept finding 11
   ;; open because a declared object field said `{:type "object"}` and nothing
   ;; more: `recovery_required {}` satisfied it, and a receipt naming a
   ;; recovery authority with no receipt, reason or recovery inside it is the
   ;; field's whole purpose missing while the schema says present.
   (some (fn [[field spec]]
           (let [known-sub #{:required :constants :types :alternatives
                             :forbidden :description}]
             (when-let [unknown (seq (remove known-sub (keys spec)))]
               (throw (ex-info (str "the :objects row grew a construct this "
                                    "witness does not validate")
                               {:field field :unknown (vec unknown)
                                :branch (:title branch)}))))
           (let [nested (get receipt (keyword field))]
             (when (contains? receipt (keyword field))
               (or (when-not (map? nested) :object-not-a-map)
                   (some (fn [sub]
                           (when-not (contains? nested (keyword sub))
                             :missing-required-subkey))
                         (:required spec))
                   (some (fn [[sub pinned]]
                           (when (and (contains? nested (keyword sub))
                                      (not= pinned (get nested (keyword sub))))
                             :sub-const-mismatch))
                         (:constants spec))
                   ;; a typed SUBKEY, e.g. closure.pruned_symlinks is an
                   ;; integer: the outer type map cannot reach inside an object
                   (some (fn [[sub constraint]]
                           (when (and (contains? nested (keyword sub))
                                      (not (type-ok? (:type constraint)
                                                     (get nested (keyword sub)))))
                             :sub-type-mismatch))
                         (:types spec))
                   ;; ALTERNATIVES: the object itself wears one of several
                   ;; faces, discriminated on a field of its own. `verification`
                   ;; is either an EXECUTED proof with its typed counts, or a
                   ;; NOT-RUN answer that forbids them -- and a receipt that
                   ;; satisfies neither is the bare `{:status "unknown"}` face
                   ;; that must never reach the wire.
                   (when-let [alternatives (:alternatives spec)]
                     (let [satisfied (filter #(alternative-satisfied? % nested)
                                             alternatives)]
                       (when-not (= 1 (count satisfied))
                         (if (zero? (count satisfied))
                           :no-alternative-satisfied
                           :more-than-one-alternative))))))))
         (:objects branch))))

(defn- valid-against
  "Every branch title `receipt` validates against."
  [receipt]
  (into #{}
        (keep (fn [branch]
                (when (nil? (schema-check branch receipt)) (:title branch))))
        (:oneOf mcp-schema/helper-extraction-output-schema)))

(defn- branch-named
  [title]
  (some #(when (= title (:title %)) %)
        (:oneOf mcp-schema/helper-extraction-output-schema)))

(def ^:private restored-exemplar-kernel
  {:restored true
   :restored_files ["src/acid/web/http.clj" "src/acid/app/m01.clj"]
   :restoration_read_back {"src/acid/web/http.clj" "sha-a"
                           "src/acid/app/m01.clj" "sha-b"}
   :destination_removed true
   :details_path "/local/state/helper-extraction-detail.edn"
   :elapsed_ms 41.0})

(defn- exemplar
  "A receipt for one face, mapped by the PRODUCTION `terminal-receipt` from
  injected kernel facts wherever a mapper can produce it."
  [title]
  (case title
    "committed"
    (assoc (mcp-helper/terminal-receipt
            {:kernel (assoc committed-kernel
                            :undo_receipt "/local/state/undo-1.edn"
                            :receipt_hash "abc123"
                            :details_path "/local/state/detail.edn"
                            :elapsed_ms 93.0)
             :verification profile-result
             :plan fixture-plan})
           :elapsed_ms 93.0)

    ("verification-failed" "verification-timeout")
    (assoc (mcp-helper/terminal-receipt
            {:kernel (assoc restored-exemplar-kernel :status (keyword title))
             :verification (assoc profile-result :ok false)
             :plan fixture-plan})
           :elapsed_ms 41.0)

    "rollback-failed"
    (assoc (mcp-helper/terminal-receipt
            {:kernel (assoc rollback-failed-kernel :elapsed_ms 12.0)
             :verification (assoc profile-result :ok false)
             :plan fixture-plan})
           :elapsed_ms 12.0)

    ;; THE ONE LITERAL, and the reason it is one: no mapper produces a
    ;; refusal. A refusal never reaches the kernel, so `terminal-receipt` is
    ;; not on its path, and building it from a real `plan` call would either
    ;; pull the pure planner into this namespace -- undoing the split that lets
    ;; the planner builder run his half with this boundary absent -- or need a
    ;; materialized workspace to route to. The shape below is the envelope
    ;; `refusal` builds plus the finalizer's clock; if it ever drifts from what
    ;; the boundary emits, `the-public-uninitialized-refusal-carries-next-call-nil`
    ;; and the preflight witnesses are the ones that catch it, because they read
    ;; real refusals off the real wire.
    "refusal"
    {:ok false
     :operation "helper_extraction"
     :error_type "helper-extraction-private-dependency"
     :error "A selected helper references a retained private var of the source."
     :next_call nil
     :source_unchanged true
     :target_unchanged true
     :committed false
     :mutation_attempted false
     :write_authority false
     :decision "whether to select that var too"
     :elapsed_ms 3.0}))

;; @spec MCP-OP-HELPER-020
(deftest every-output-variant-validates-against-exactly-one-face
  (doseq [{:keys [title]} (:oneOf mcp-schema/helper-extraction-output-schema)]
    (testing title
      (let [receipt (exemplar title)]
        (is (= #{title} (valid-against receipt))
            (str "the five faces are DISJOINT: a receipt that satisfies two of "
                 "them makes `oneOf` a menu instead of a discrimination. "
                 (pr-str receipt)))))))

;; @spec MCP-OP-HELPER-020
(deftest removing-any-required-field-invalidates-its-variant
  (doseq [{:keys [title required]} (:oneOf mcp-schema/helper-extraction-output-schema)]
    (testing title
      (let [receipt (exemplar title)]
        (doseq [field required]
          (testing (str "without " field)
            (is (some? (schema-check (branch-named title)
                                     (dissoc receipt (keyword field))))
                (str field " is declared required by the " title
                     " variant, so a receipt missing it must not validate. If "
                     "this fails the field is required in name only."))))))))

;; @spec MCP-OP-HELPER-020
(deftest contradicting-any-pinned-constant-invalidates-its-variant
  (doseq [{:keys [title properties]} (:oneOf mcp-schema/helper-extraction-output-schema)]
    (testing title
      (let [receipt (exemplar title)]
        (doseq [[field constraint] properties
                :when (contains? constraint :const)]
          (testing (str "with a contradicted " field)
            (let [pinned (:const constraint)
                  contradiction (cond
                                  (boolean? pinned) (not pinned)
                                  (number? pinned) (inc pinned)
                                  ;; a terminal word swapped for another
                                  ;; face's: the exact confusion `status` and
                                  ;; `kernel_status` exist to prevent
                                  (= "committed" pinned) "rollback-failed"
                                  :else "committed")]
              (is (some? (schema-check (branch-named title)
                                       (assoc receipt (keyword field) contradiction)))
                  (str field " is pinned to " (pr-str pinned) " on the " title
                       " face; " (pr-str contradiction) " must not validate")))))))))

;; @spec MCP-OP-HELPER-020
(deftest the-named-contradictions-the-review-called-out-are-all-rejected
  (testing "the three the fence review named by hand, asserted by name so a
            regression is readable without decoding the generated matrix"
    (is (some? (schema-check (branch-named "committed")
                             (assoc (exemplar "committed")
                                    :kernel_status "rollback-failed")))
        "a committed receipt wearing another face's kernel word")
    (is (some? (schema-check (branch-named "refusal")
                             (assoc (exemplar "refusal") :source_unchanged false)))
        "a refusal that admits it changed the source is not a refusal")
    (is (some? (schema-check (branch-named "committed")
                             (dissoc (exemplar "committed") :receipt_hash)))
        "a committed receipt naming an undo document without the hash that
         binds it is an inverse nobody can prove they are applying to the
         right transaction")))

;; ---------------------------------------------------------------------------
;; the NESTED shapes (Sol r4 kept finding 11 open on these)
;;
;; A declared `{:type "object"}` says a field is a map and nothing else, so
;; `recovery_required {}` satisfied the schema while carrying none of the
;; authority a human has to act on. The matrix's `:objects` row states each
;; object field's required subkeys and pinned sub-constants; the two witnesses
;; below are GENERATED from that row, so a subkey added to the schema is
;; covered on the next run rather than when someone remembers.

(defn- object-rows
  "`[[branch field spec] ...]` for every declared object shape in the matrix."
  []
  (for [branch (:oneOf mcp-schema/helper-extraction-output-schema)
        [field spec] (:objects branch)]
    [branch field spec]))

;; @spec MCP-OP-HELPER-020
(deftest the-matrix-declares-the-shape-of-every-object-field-it-requires
  (testing "an object field that is required but shapeless is finding 11: the
            schema says present, and present is not what the caller needs"
    (is (seq (object-rows))
        "the :objects row is where nested shape is stated; an empty one means
         every declared object is still just {:type \"object\"}")
    (doseq [[branch field spec] (object-rows)]
      (testing (str (:title branch) " / " field)
        (is (seq (:required spec))
            (str field " is declared as an object with no required subkeys, "
                 "so an empty map satisfies it"))))))

;; @spec MCP-OP-HELPER-020
(deftest removing-any-required-subkey-invalidates-its-object
  (doseq [[branch field spec] (object-rows)]
    (let [receipt (exemplar (:title branch))]
      (testing (str (:title branch) " / " field)
        (is (contains? receipt (keyword field))
            (str "the exemplar must actually carry " field " or the removals "
                 "below prove nothing"))
        (doseq [sub (:required spec)]
          (testing (str "without " sub)
            (is (some? (schema-check
                        branch
                        (update receipt (keyword field) dissoc (keyword sub))))
                (str sub " is declared required inside " field " on the "
                     (:title branch) " face; a receipt missing it must not "
                     "validate. If this fails, the subkey is required in name "
                     "only -- exactly the gap that kept finding 11 open."))))))))

;; @spec MCP-OP-HELPER-020
(deftest contradicting-any-pinned-subconstant-invalidates-its-object
  (doseq [[branch field spec] (object-rows)
          [sub pinned] (:constants spec)]
    (let [receipt (exemplar (:title branch))
          contradiction (cond
                          (boolean? pinned) (not pinned)
                          (number? pinned) (inc pinned)
                          :else (str pinned "-contradicted"))]
      (testing (str (:title branch) " / " field " / " sub)
        (is (some? (schema-check
                    branch
                    (assoc-in receipt [(keyword field) (keyword sub)]
                              contradiction)))
            (str sub " inside " field " is pinned to " (pr-str pinned)
                 "; " (pr-str contradiction) " must not validate. "
                 "restoration_read_back's manifest_in is the one that matters "
                 "most: it is the pointer that makes the O(1) receipt honest "
                 "about where the per-file evidence went."))))))

;; ---------------------------------------------------------------------------
;; the refusal on the REAL public path
;;
;; Every refusal witness above reaches the boundary through `mcp-helper/plan`
;; or `execute!`. This one goes through the registered tool map's `:tool-fn` --
;; the exact entry the MCP server dispatches to -- because the normalization a
;; refusal envelope needs is applied on that path and nowhere else. A refusal
;; that is well-formed one function call below the wire is not a well-formed
;; refusal.

(defn- registered-tool-fn
  []
  (let [tool (some #(when (= "helper_extraction" (:name %)) %)
                   (mcp-tool/tools-for-profile :full))]
    (is (some? tool) "helper_extraction is registered in the full profile")
    (:tool-fn tool)))

;; @spec MCP-OP-HELPER-010
;; @spec MCP-OP-HELPER-021
(deftest a-refusal-on-the-registered-public-path-is-a-complete-envelope
  (testing "scope.paths given as DIRECTORY NAMES rather than globs authorizes
            no file, so every discovered caller is outside the write
            authorization and the request refuses. What matters here is not the
            refusal -- other witnesses prove that -- but that the receipt an
            agent receives from the REGISTERED entry is a complete envelope and
            that the tree is untouched."
    (let [outcome
          (with-materialized-happy-tree
           "public-refusal"
           (fn [root]
        (let [receipt-dir (io/file tmp-root (str "pub-receipts-" (System/nanoTime)))
              captured (atom nil)]
          (try
            (.mkdirs receipt-dir)
            (mcp-tool/init! {:project-root (str root)
                             :receipt-dir (str receipt-dir)
                             :verification-profiles configured-profiles})
            ((registered-tool-fn)
             nil
             (json/parse-string
              (json/generate-string
               (fixture/request {:workspace_root (str root)
                                 :scope {:paths ["src" "test"]}
                                 :verification {:profile "helper-proof"}}))
              true)
             (fn [content error? structured]
               (reset! captured {:content content :error? error?
                                 :result structured})))
            @captured
            (finally
              (mcp-tool/init! nil)
              (delete-tree! receipt-dir))))))
          {:keys [error? result]} (:result outcome)]
      (is (true? error?))
      (is (= "helper-extraction-caller-outside-scope" (:error_type result))
          (pr-str result))
      (testing "the envelope is complete"
        (is (= "helper_extraction" (:operation result)))
        (is (false? (:mutation_attempted result))
            "nothing was staged, and the receipt says so in a FIELD rather than
             leaving the reader to infer it from an absence")
        (is (false? (:write_authority result))
            "and no write authority was ever taken")
        (is (true? (:source_unchanged result)))
        (is (contains? result :next_call))
        (is (nil? (:next_call result))))
      (testing "it validates against exactly the refusal branch"
        (is (= #{"refusal"} (valid-against result))
            (str "the receipt an agent actually receives must satisfy the "
                 "registered output schema, and exactly one face of it: "
                 (pr-str result))))
      (testing "and the tree is byte-identical afterwards"
        (assert-restored! outcome)))))

;; ---------------------------------------------------------------------------
;; the DISPOSITION pair, and the shapes an empty map would satisfy
;;
;; Two more ways a declared field can be present and useless: a face that
;; permits both `details_path` and `details_unavailable` says nothing about
;; whether the detail exists, and an object field whose required subkeys are
;; declared but never checked against an EMPTY map is satisfied by `{}`. Both
;; are generated from the matrix, so a new disposition pair or object row is
;; covered without an edit.

;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-020
(deftest exactly-one-disposition-holds-on-every-face-that-declares-a-pair
  (let [rows (for [branch (:oneOf mcp-schema/helper-extraction-output-schema)
                   :let [alternatives (disposition-alternatives branch)]
                   :when alternatives]
               [branch alternatives])]
    (is (seq rows)
        "no face declares an exactly-one-of pair. A terminal receipt must say
         either where the detail went or that it could not be written: leaving
         both optional lets a receipt be silent about the difference, and a
         caller reads silence as nothing-more-to-see.")
    (doseq [[branch alternatives] rows]
      (testing (:title branch)
        (let [receipt (exemplar (:title branch))
              fields (mapv (comp keyword first :required) alternatives)]
          (is (nil? (schema-check branch receipt))
              (str "the exemplar satisfies exactly one disposition: "
                   (pr-str (select-keys receipt fields))))
          (testing "BOTH present is a receipt contradicting itself"
            (is (some? (schema-check
                        branch
                        (reduce (fn [acc field] (assoc acc field "either"))
                                receipt fields)))
                (str "a receipt carrying every one of " (pr-str fields)
                     " claims the detail both went somewhere and could not be "
                     "written")))
          (testing "NEITHER present says nothing at all"
            (is (some? (schema-check branch (apply dissoc receipt fields)))
                (str "a receipt carrying none of " (pr-str fields)
                     " leaves the reader to guess"))))))))

;; @spec MCP-OP-HELPER-020
(deftest an-empty-map-never-satisfies-a-declared-object-shape
  (doseq [[branch field spec] (object-rows)]
    (testing (str (:title branch) " / " field)
      (let [receipt (exemplar (:title branch))]
        (is (some? (schema-check branch (assoc receipt (keyword field) {})))
            (str field " is declared with required subkeys "
                 (pr-str (vec (:required spec)))
                 ", so an EMPTY map must not satisfy it. `{}` passing here is "
                 "finding 11 exactly: the schema says the field is present and "
                 "the caller gets nothing."))))))

;; @spec MCP-OP-HELPER-015
(deftest a-pinned-count-is-rejected-in-both-directions
  (testing "the generated const witness contradicts a pinned number by
            incrementing it, which catches a missing check but not an
            off-by-one that only guards one side. Any count pinned to a
            constant is probed BELOW it as well."
    (doseq [branch (:oneOf mcp-schema/helper-extraction-output-schema)
            [field constraint] (:properties branch)
            :when (and (contains? constraint :const)
                       (number? (:const constraint)))]
      (let [receipt (exemplar (:title branch))
            pinned (:const constraint)]
        (testing (str (:title branch) " / " field " pinned to " pinned)
          (doseq [wrong [(dec pinned) (inc pinned)]]
            (is (some? (schema-check branch (assoc receipt (keyword field) wrong)))
                (str field " is pinned to " pinned "; " wrong
                     " must not validate. source_file is the one this exists "
                     "for: the source is counted ONCE however many "
                     "source-local uses it carries, so both 0 and 2 are "
                     "wrong and for different reasons."))))))))

;; ---------------------------------------------------------------------------
;; the THROW path, and declared types
;;
;; Sol r6: `finish-failure!` is reached with `proof nil` when a Throwable ends
;; a staged transaction before the profile ever answered, and the mapper emits
;; `verification {:status "unknown"}` for it. That is a real production face,
;; and the matrix rejects it. The exemplar below is built from production
;; `terminal-receipt` with exactly those kernel facts and NO profile result, so
;; the schema is measured against a receipt the server actually publishes
;; rather than against one the witness was able to construct.

(defn- bare-unknown-receipt
  "The mapper's answer when it is handed NO profile result at all.

  v7 documents this as UNPUBLISHABLE: `execute!` always knows which profile was
  requested, so a verification that names none never leaves the boundary. It is
  kept here as a NEGATIVE -- the shape the schema must refuse -- so it cannot
  reach the wire silently if some future path forgets to say why the proof did
  not run."
  []
  (assoc (mcp-helper/terminal-receipt
          {:kernel (assoc restored-exemplar-kernel :status :verification-failed)
           :verification nil
           :plan fixture-plan})
         :elapsed_ms 41.0))

;; @spec MCP-OP-HELPER-022
(deftest the-bare-unknown-verification-face-can-never-reach-the-wire
  (testing "a verification that says only `unknown` says nothing a caller can
            act on: not which profile, not whether a process started, not why.
            The schema must refuse it outright."
    (let [receipt (bare-unknown-receipt)]
      (is (= "unknown" (get-in receipt [:verification :status])))
      (is (empty? (valid-against receipt))
          (str "the bare unknown face must validate against NO branch: "
               (pr-str (:verification receipt)))))))

;; @spec MCP-OP-HELPER-020
;; @spec MCP-OP-HELPER-022
(deftest the-real-throw-path-receipt-is-a-declared-face
  (testing "the production path: a Throwable from the proof step ends a STAGED
            transaction, the kernel rolls back, and the receipt reports a
            not-run verification that still names the profile and the reason.
            Driven through execute! with an injected `:run-proof!` rather than
            hand-built, so what the schema is measured against is what the
            server publishes."
    (let [outcome (with-materialized-happy-tree
                    "throw-path-face"
                    (fn [root]
                      (mcp-helper/execute!
                       {:verification-profiles configured-profiles
                        :run-proof! (fn [& _]
                                      (throw (ex-info "proof exploded" {})))}
                       (fixture/request
                        {:workspace_root root
                         :verification {:profile "helper-proof"}}))))
          receipt (:result outcome)
          verification (:verification receipt)]
      (is (= "verification-failed" (:status receipt)) (pr-str receipt))
      (testing "the verification wears the NOT-RUN face, completely"
        (is (= "unknown" (:status verification)) (pr-str verification))
        (is (= "helper-proof" (:profile verification))
            "it names the profile that was requested; execute! always knows it")
        (is (false? (:ok verification)))
        (is (false? (:fresh_process verification))
            "no child process ran, and the receipt says so rather than omitting it")
        (is (some? (:reason verification))
            "and it says WHY the proof did not run")
        (is (not-any? #(contains? verification %)
                      [:structural_callers :helper_behaviors :compiled_callers])
            "carrying no typed count of a proof that never happened"))
      (testing "and every declared object row is complete on the REAL receipt"
        (doseq [[field spec] (:objects (branch-named "verification-failed"))
                :let [nested (get receipt (keyword field))]]
          (testing field
            (is (some? nested))
            (is (empty? (remove #(contains? (or nested {}) (keyword %))
                                (:required spec)))
                (str field " is missing "
                     (pr-str (vec (remove #(contains? (or nested {}) (keyword %))
                                          (:required spec))))
                     " on the receipt execute! actually publishes. The mapper "
                     "exemplars carry it because the fixture plan supplies it; "
                     "only a production-path receipt can show that the real "
                     "path does not.")))))
      (is (= #{"verification-failed"} (valid-against receipt))
          (str "and the whole receipt validates against exactly one branch: "
               (pr-str (valid-against receipt)))))))

;; @spec MCP-OP-HELPER-020
(deftest every-declared-type-is-enforced
  (testing "a field declared `integer` that arrives as a string reads fine in a
            log and breaks the first caller that does arithmetic on it"
    (is (seq declared-types) "the schema declares types at all")
    (doseq [branch (:oneOf mcp-schema/helper-extraction-output-schema)
            :let [receipt (exemplar (:title branch))]
            [field declared] declared-types
            :when (contains? receipt (keyword field))]
      (testing (str (:title branch) " / " field " : " (pr-str declared))
        (let [wrong (wrong-typed-value declared)]
          (is (some? (schema-check branch (assoc receipt (keyword field) wrong)))
              (str field " is declared " (pr-str declared) "; "
                   (pr-str wrong) " must not validate")))))))

;; @spec MCP-OP-HELPER-012
(deftest the-closure-grammar-cannot-be-contradicted
  (testing "`closure.grammar` is the sentence the receipt uses to bound its own
            claim. A receipt that says it closed over a grammar it did not is
            worse than one that says nothing, because it stops the reader
            asking."
    (doseq [branch (:oneOf mcp-schema/helper-extraction-output-schema)
            :let [spec (get-in branch [:objects "closure"])]
            :when spec
            [sub pinned] (:constants spec)]
      (testing (str (:title branch) " / closure." sub)
        (is (some? (schema-check
                    branch
                    (assoc-in (exemplar (:title branch))
                              [:closure (keyword sub)]
                              (str pinned "-but-not-really"))))
            (str "closure." sub " is pinned to " (pr-str pinned)))))))

;; ---------------------------------------------------------------------------
;; the nested types and the alternative faces

;; @spec MCP-OP-HELPER-020
(deftest every-typed-subkey-is-enforced
  (testing "the outer type map cannot reach inside an object, so a subkey's
            declared type is only real if the nested row checks it"
    (let [rows (for [[branch field spec] (object-rows)
                     [sub constraint] (:types spec)]
                 [branch field sub constraint])]
      (is (seq rows) "object rows declare subkey types at all")
      (doseq [[branch field sub constraint] rows]
        (testing (str (:title branch) " / " field "." sub)
          (let [receipt (exemplar (:title branch))
                wrong (wrong-typed-value (:type constraint))]
            (when (contains? (get receipt (keyword field)) (keyword sub))
              (is (some? (schema-check
                          branch
                          (assoc-in receipt [(keyword field) (keyword sub)] wrong)))
                  (str field "." sub " is declared " (pr-str (:type constraint))
                       "; " (pr-str wrong) " must not validate")))))))))

;; @spec MCP-OP-HELPER-022
(deftest a-not-run-verification-may-not-carry-the-counts-of-a-proof
  (testing "the counts are the proof's OUTPUT. A verification that did not run
            and still reports structural_callers is claiming coverage it never
            measured -- the false green in its purest form."
    (let [rows (for [[branch field spec] (object-rows)
                     alternative (:alternatives spec)
                     :when (seq (:forbidden alternative))
                     forbidden (:forbidden alternative)]
                 [branch field alternative forbidden])]
      (is (seq rows) "some alternative forbids fields")
      (doseq [[branch field alternative forbidden] rows]
        (testing (str (:title branch) " / " field " / " (:title alternative)
                      " + " forbidden)
          ;; put the object into its forbidding face, then smuggle the field in
          (let [face (merge {:status "unknown" :ok false :fresh_process false
                             :reason "the proof did not run"}
                            (into {} (map (fn [[k v]] [(keyword k) (:const v)]))
                                  (:properties alternative)))
                receipt (assoc (exemplar (:title branch)) (keyword field)
                               (assoc face (keyword forbidden) 28))]
            (is (some? (schema-check branch receipt))
                (str forbidden " is forbidden on the " (:title alternative)
                     " face of " field "; a receipt carrying it must not "
                     "validate"))))))))

;; @spec MCP-OP-HELPER-022
(deftest an-executed-verification-may-not-wear-the-not-run-status
  (testing "the two faces are discriminated on status, so a proof that ran and
            reports `unknown` satisfies neither and is refused"
    (doseq [[branch field spec] (object-rows)
            :when (seq (:alternatives spec))
            :let [receipt (exemplar (:title branch))]
            :when (contains? receipt (keyword field))]
      (testing (str (:title branch) " / " field)
        (is (some? (schema-check
                    branch
                    (assoc-in receipt [(keyword field) :status] "unknown")))
            (str "an executed " field " relabelled `unknown` keeps its typed "
                 "counts, which the not-run face forbids, and loses the enum "
                 "the executed face requires: it must satisfy neither"))))))
