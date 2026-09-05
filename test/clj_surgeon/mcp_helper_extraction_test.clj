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
  {:lane :excluded}
  (:require
   [clj-surgeon.helper-extraction-fixture :as fixture]
   [clj-surgeon.mcp-helper-extraction :as mcp-helper]
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

(defn- tree-of
  "`{path source}` for one fixture variant at one phase (`:pre` or `:post`)."
  [variant phase]
  (into {}
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
   :counts fixture/canonical-counts
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
   :recovery_required {:journal "txn-77" :reason "read-back mismatch"}})

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
        (is (= (:restoration_read_back kernel) (:restoration_read_back receipt))
            "the read-back is carried through, not regenerated")
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
          "the kernel's recovery-required evidence is carried through"))))

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
        (doseq [[path source] (tree-of :happy :pre)]
          (let [target (io/file root path)]
            (io/make-parents target)
            (spit target source)))
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
