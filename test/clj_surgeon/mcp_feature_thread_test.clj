(ns clj-surgeon.mcp-feature-thread-test
  "Witnesses for the `feature_thread` verb.

  The NAMED TEST CASE is Gene's own request: social-media-writer,
  Edit -> Dequote/Format, five owners in two languages, from the 2026-09-03
  codex session. The fixture and its provenance live under
  `test-fixtures/feature-thread/smw-dequote/` (README.md, MANIFEST.tsv, and the
  session's own patch). Every assertion below fails when the verb is wrong, not
  merely when it is silent."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-feature-thread :as ft]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; The fixture
;; ---------------------------------------------------------------------------

(def fixture-root "test-fixtures/feature-thread/smw-dequote")
(def after-root "test-fixtures/feature-thread/smw-dequote-after")

(def smw-conventions
  (edn/read-string (slurp (io/file fixture-root ".clj-surgeon/feature-thread.edn"))))

(def dequote-seeds
  "The seeds the mayor's ground truth names for Edit -> Dequote/Format."
  {:subject "formatDraft"
   :also ["/api/transform/format" "mechanical-format"]})

(def five-owners
  "GROUND TRUTH: the five owners, two languages, from the transcript."
  {"menu-caller" "src/writer/views/components.clj"
   "js-function" "resources/public/js/editor-commands.js"
   "route" "src/writer/routes.clj"
   "handler" "src/writer/handlers/transform.clj"
   "tests" "test/writer/handlers/transform_apply_test.clj"})

(def js-test-owner "test/js/browser_runtime_classic_script_test.js")

(defn call!
  "One call through the MCP entrance. Returns `{:text :error? :structured}`."
  [params]
  (let [captured (atom nil)]
    (ft/init! nil)
    (ft/handle-feature-thread
      nil params
      (fn [content error? structured]
        (reset! captured {:text (first content)
                          :error? error?
                          :structured structured})))
    @captured))

(defn thread!
  ([root] (thread! root {}))
  ([root extra]
   (call! (merge {:subject (:subject dequote-seeds)
                  :also (:also dequote-seeds)
                  :config smw-conventions
                  :scope {:workspace_root root}}
                 extra))))

(defn leg
  [structured id]
  (first (filter #(= id (:id %)) (:legs structured))))

(defn files-named
  "Every repository file the receipt names, anywhere."
  [structured]
  (set (concat (keep :file (:legs structured))
               (mapcat #(keep :file (:co_primaries %)) (:legs structured))
               (mapcat #(keep :file (:also %)) (:legs structured))
               (keep :file (get-in structured [:sibling :legs]))
               (keep :file (get-in structured [:rules :governance]))
               (mapcat #(map :file (:unreadable %)) (:legs structured)))))

;; ---------------------------------------------------------------------------
;; ASSERTION 1 -- RECALL: all five owners from one seed set in ONE call
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-004
;; @spec MCP-OP-THREAD-005
;; @spec MCP-OP-THREAD-006
;; @spec MCP-OP-THREAD-008
;; @spec MCP-OP-THREAD-013
(deftest t1-smw-thread-returns-six-legs-with-bodies
  (testing "Edit -> Dequote/Format: six owners, two languages, one call"
    (let [{:keys [text error? structured]} (thread! fixture-root)]
      (is (false? error?))
      (is (true? (:ok structured)))
      (is (= "COMPLETE (6 of 6)" (:status structured)))
      (is (true? (:complete structured)))
      (is (= 6 (:legs_found structured)))
      (is (= [] (:legs_missing structured)))

      (testing "every ground-truth owner is named, at the leg the truth assigns it"
        (doseq [[id file] five-owners]
          (let [l (leg structured id)]
            (is (some? l) (str "no leg " id))
            (is (= "FOUND" (:status l)) (str id " is not FOUND"))
            (is (= file (:file l))
                (str id " named " (:file l) ", ground truth is " file)))))

      (testing "the JavaScript test witness is carried as a co-primary leg"
        (is (contains? (files-named structured) js-test-owner)
            "the node test witness is not named anywhere in the receipt"))

      (testing "every FOUND leg carries a body, a range and a hash of that body"
        (doseq [l (:legs structured)]
          (is (integer? (:from l)))
          (is (integer? (:to l)))
          (is (<= (:from l) (:to l)))
          (is (re-matches #"[0-9a-f]{64}" (:sha256 l)))
          (is (string? (:body l)) (str (:id l) " carries no body"))
          (is (= (:sha256 l) (ft/sha256-hex (:body l)))
              (str (:id l) " hash does not cover the body it shipped"))
          (is (string? (:anchor l)) (str (:id l) " carries no insertion anchor"))))

      (testing "the text block names each owner"
        (doseq [[_ file] five-owners]
          (is (str/includes? text file)))))))

;; ---------------------------------------------------------------------------
;; ASSERTION 2 -- RANGES, not files. The transcript read editor-commands.js at
;; FOUR guessed line ranges before the JS half was in hand.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-005
;; @spec MCP-OP-THREAD-006
(deftest ranges-not-files-the-receipt-returns-the-exact-body
  (let [{:keys [structured]} (thread! fixture-root)
        js (leg structured "js-function")
        handler (leg structured "handler")
        route (leg structured "route")]
    (testing "the JavaScript leg is an exact range whose body is the function"
      (is (= 389 (:from js)))
      (is (= 454 (:to js)))
      (is (str/starts-with? (:body js) "async function formatDraft() {"))
      (is (str/ends-with? (str/trimr (:body js)) "}"))
      (is (= "brace-window(lexed,closed)" (:boundary js))
          "a lexed close is the claim; a window would have to say so"))

    (testing "the body is the exact bytes at that range in the fixture"
      (let [lines (str/split (slurp (io/file fixture-root (:file js))) #"\n" -1)
            slice (str/join "\n" (subvec (vec lines) (dec (:from js)) (:to js)))]
        (is (= slice (:body js)))
        (is (= (ft/sha256-hex slice) (:sha256 js)))))

    (testing "Clojure legs are PARSED, and say so"
      (is (= "form(parsed)" (:boundary handler)))
      (is (= "handle-format" (:form_name handler)))
      (is (str/starts-with? (:body handler) "(defn handle-format"))
      (is (str/starts-with? (:boundary route) "form(parsed, member of"))
      (is (= 2148 (:from route)) "the route entry is one line, not a window")
      (is (= 2148 (:to route))))))

;; ---------------------------------------------------------------------------
;; ASSERTION 3 -- NO FALSE MEMBERS
;; ---------------------------------------------------------------------------

(def allowed-members
  "Every file the receipt is allowed to name on this fixture. A file outside
  this set is a false member and the test says which one."
  #{"src/writer/views/components.clj"
    "resources/public/js/editor-commands.js"
    "src/writer/routes.clj"
    "src/writer/handlers/transform.clj"
    "test/writer/handlers/transform_apply_test.clj"
    "test/js/browser_runtime_classic_script_test.js"
    "test/writer/intent_contract_test.clj"
    "docs/intent/registry.edn"
    "Makefile"})

;; @spec MCP-OP-THREAD-004
(deftest no-false-members-in-the-receipt
  (let [{:keys [structured]} (thread! fixture-root)
        named (files-named structured)
        extra (set/difference named allowed-members)]
    (is (empty? extra)
        (str "the receipt named files that are not members of this thread: "
             (sort extra)))
    (is (>= (count named) 6)
        "precision is only meaningful next to recall")))

;; ---------------------------------------------------------------------------
;; ASSERTION 4 -- TYPED REFUSAL: an unreadable leg is NAMED, and the status
;; never reads complete with a leg missing.
;; ---------------------------------------------------------------------------

(defn- copy-tree!
  [^java.io.File from ^java.io.File to]
  (.mkdirs to)
  (doseq [^java.io.File f (.listFiles from)]
    (let [target (io/file to (.getName f))]
      (if (.isDirectory f)
        (copy-tree! f target)
        (io/copy f target)))))

(defn- scratch-copy!
  [source-root prefix]
  (let [dir (io/file (str (java.nio.file.Files/createTempDirectory
                            prefix
                            (into-array java.nio.file.attribute.FileAttribute []))))]
    (copy-tree! (io/file source-root) dir)
    dir))

(defn- delete-tree!
  [^java.io.File f]
  (when (.isDirectory f)
    (doseq [c (.listFiles f)] (delete-tree! c)))
  (.delete f))

;; @spec MCP-OP-THREAD-004
;; @spec MCP-OP-THREAD-013
(deftest unreadable-js-leg-is-a-typed-absence
  (testing "with the JavaScript unreadable, the receipt names WHICH leg and WHY"
    (let [scratch (scratch-copy! fixture-root "feature-thread-unreadable")]
      (try
        (let [js (io/file scratch "resources/public/js/editor-commands.js")]
          (is (.setReadable js false false)
              "this witness needs a filesystem that honours a read bit")
          (let [{:keys [text structured]} (thread! (.getPath scratch))
                l (leg structured "js-function")]
            (is (= "ABSENT" (:status l)))
            (is (= "INCOMPLETE (5 of 6)" (:status structured)))
            (is (false? (:complete structured)))
            (is (= ["js-function"] (:legs_missing structured)))
            (is (seq (:searches l)) "an absent leg must quote its searches")
            (is (some #(str/includes? % "editor-commands.js") (map :file (:unreadable l)))
                "the leg must name the file it could not read")
            (is (= #{"unreadable"} (set (map :reason (:unreadable l))))
                "the reason must be typed, not blank")
            (is (str/includes? text "INCOMPLETE (5 of 6)"))
            (is (str/includes? text "unreadable"))
            (is (not (str/includes? text "COMPLETE (6 of 6)")))))
        (finally
          (.setReadable (io/file scratch "resources/public/js/editor-commands.js")
                        true false)
          (delete-tree! scratch))))))

;; @spec MCP-OP-THREAD-013
(deftest a-leg-whose-files-are-gone-is-absent-not-omitted
  (let [scratch (scratch-copy! fixture-root "feature-thread-missing")]
    (try
      (delete-tree! (io/file scratch "resources/public/js"))
      (let [{:keys [structured]} (thread! (.getPath scratch))
            l (leg structured "js-function")]
        (is (= 6 (count (:legs structured)))
            "every declared leg plus the automatic implementation leg is rendered")
        (is (= "ABSENT" (:status l)))
        (is (seq (:searches l)))
        (is (= "INCOMPLETE (5 of 6)" (:status structured))))
      (finally (delete-tree! scratch)))))

;; ---------------------------------------------------------------------------
;; THE SECOND FIXTURE -- the tree at the moment it BROKE
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-010
(deftest dequote-format-thread-is-correct-on-the-broken-tree
  (testing "the transcript's own patch applied: the new command's thread"
    (let [{:keys [structured text]}
          (call! {:subject "dequoteFormatSelection"
                  :also ["/api/transform/format" "mechanical-format"]
                  :config smw-conventions
                  :scope {:workspace_root after-root}})]
      (is (= "COMPLETE (6 of 6)" (:status structured)))
      (is (= "resources/public/js/editor-commands.js"
             (:file (leg structured "js-function"))))
      (is (str/starts-with? (:body (leg structured "js-function"))
                            "async function dequoteFormatSelection() {"))
      (is (= "src/writer/views/components.clj"
             (:file (leg structured "menu-caller"))))
      (is (str/includes? (:body (leg structured "menu-caller")) "Dequote/Format"))

      (testing "the INTENT comment above the new function reaches the rules row"
        (is (contains? (set (get-in structured [:rules :intents]))
                       "EDITOR-DEQUOTE-016")))

      (testing "and is RESOLVED to its registry row"
        (is (some #(and (= "docs/intent/registry.edn" (:file %))
                        (str/includes? (:match %) "EDITOR-DEQUOTE-016"))
                  (get-in structured [:rules :governance]))))

      (is (str/includes? text "EDITOR-DEQUOTE-016")))))

;; ---------------------------------------------------------------------------
;; The rules row: the governance tail the transcript shows the agent needed
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-010
(deftest rules-carry-the-wiring-contract-and-the-governance-tail
  (let [{:keys [structured]} (thread! fixture-root)
        rules (:rules structured)]
    (testing "the durable path the handler routes through"
      (is (some #{"editor-dispatch/fold-editor-snapshot-and-tx!"} (:durable_path rules))
          "the durable, conflict-aware editor path is the contract to mirror"))
    (testing "the statuses it refuses with"
      (is (contains? (set (:refusal_statuses rules)) "409"))
      (is (contains? (set (:refusal_statuses rules)) "400")))
    (testing "the INTENT identifiers present in the located bodies"
      (is (set/subset? #{"EDITOR-CONF-005" "EDITOR-DURA-007"}
                       (set (:intents rules)))))
    (testing "the governance rows: intent registry, contract test, test target"
      (is (some #(= "docs/intent/registry.edn" (:file %)) (:governance rules))
          "the registry entries are members of this feature and are RETURNED"))
    (testing "the assert line makes the receipt a write instrument"
      (is (str/includes? (:assert rules) "typed refusal")))))

;; ---------------------------------------------------------------------------
;; Admission
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-001
(deftest admission-refuses-before-reading-a-file
  (testing "an unknown field"
    (let [{:keys [error? structured text]}
          (call! {:subject "formatDraft" :nonsense 1
                  :scope {:workspace_root fixture-root}})]
      (is (true? error?))
      (is (= "feature-thread-unknown-field" (:error_type structured)))
      (is (= ["nonsense"] (:unknown_fields structured)))
      (is (str/includes? text "nonsense"))))

  (testing "a blank subject"
    (let [{:keys [structured]} (call! {:subject "   "
                                       :scope {:workspace_root fixture-root}})]
      (is (= "feature-thread-invalid-subject" (:error_type structured)))))

  (testing "a malformed also"
    (let [{:keys [structured]} (call! {:subject "x" :also "not-a-vector"
                                       :scope {:workspace_root fixture-root}})]
      (is (= "feature-thread-invalid-also" (:error_type structured)))))

  (testing "a workspace root that is not a directory"
    (let [{:keys [structured]}
          (call! {:subject "x" :config smw-conventions
                  :scope {:workspace_root "/var/tmp/forge/no-such-root-9f3c"}})]
      (is (= "invalid-workspace-root" (:error_type structured))))))

;; @spec MCP-OP-THREAD-002
(deftest budget-is-admitted-defaulted-and-never-silently-clamped
  (testing "a budget above the hard cap is refused, with the cap named"
    (let [{:keys [error? structured text]}
          (thread! fixture-root {:budget_bytes (inc ft/hard-cap-bytes)})]
      (is (true? error?))
      (is (= "feature-thread-budget-above-cap" (:error_type structured)))
      (is (= ft/hard-cap-bytes (:hard_cap_bytes structured)))
      (is (str/includes? text (str ft/hard-cap-bytes)))))

  (testing "a non-integer budget is refused"
    (let [{:keys [structured]} (thread! fixture-root {:budget_bytes 0})]
      (is (= "feature-thread-invalid-budget" (:error_type structured)))))

  (testing "the default is applied when none is passed"
    (let [{:keys [structured]} (thread! fixture-root)]
      (is (= ft/default-budget-bytes (:budget_bytes structured))))))

;; @spec MCP-OP-THREAD-003
(deftest conventions-are-data-and-their-absence-names-the-path
  (testing "an inline convention set is used and its source is named"
    (let [{:keys [structured]} (thread! fixture-root)]
      (is (= "inline" (:conventions_source structured)))))

  (testing "the workspace dotfile is read when no config is passed"
    (let [{:keys [structured]}
          (call! {:subject "formatDraft" :also ["/api/transform/format"]
                  :scope {:workspace_root fixture-root}})]
      (is (= ".clj-surgeon/feature-thread.edn" (:conventions_source structured)))
      (testing "and with only formatDraft as a definition seed, the automatic
                implementation leg DEDUPES against the js-function leg"
        (is (= "COMPLETE (5 of 5)" (:status structured)))
        (is (= "N/A" (:status (leg structured "implementation"))))
        (is (str/includes? (:reason (leg structured "implementation"))
                           "already a leg of this receipt")))))

  (testing "a workspace with no convention set names the path it searched"
    (let [scratch (scratch-copy! fixture-root "feature-thread-noconv")]
      (try
        (delete-tree! (io/file scratch ".clj-surgeon"))
        (let [{:keys [structured text]}
              (call! {:subject "formatDraft"
                      :scope {:workspace_root (.getPath scratch)}})]
          (is (= "feature-thread-conventions-absent" (:error_type structured)))
          (is (str/includes? text ".clj-surgeon/feature-thread.edn")))
        (finally (delete-tree! scratch)))))

  (testing "a convention set with FEWER than the five leg roles is refused"
    (let [{:keys [structured]}
          (thread! fixture-root {:config (update smw-conventions :legs pop)})]
      (is (= "feature-thread-conventions-invalid" (:error_type structured)))
      (is (str/includes? (:error structured) "at least the five"))
      (is (str/includes? (:error structured) "found 4")))))

;; ---------------------------------------------------------------------------
;; The JavaScript body: a lexer, never a parser
;; ---------------------------------------------------------------------------

(defn- lex-close
  [source]
  (ft/lexed-brace-match source 0 (count source)))

;; @spec MCP-OP-THREAD-006
(deftest javascript-bodies-are-lexed-brace-matches-or-labelled-windows
  (testing "every real failure of naive brace matching is lexical"
    (is (= (count "function a() { const s = '}'; }")
           (lex-close "function a() { const s = '}'; }"))
        "a brace inside a single-quoted string closes nothing")
    (is (= (count "function a() { const s = \"}\"; }")
           (lex-close "function a() { const s = \"}\"; }"))
        "a brace inside a double-quoted string closes nothing")
    (is (= (count "function a() { const s = `x${ {y: 1} }z`; }")
           (lex-close "function a() { const s = `x${ {y: 1} }z`; }"))
        "template interpolation opens and closes its own scope")
    (is (= (count "function a() { /* } */ return 1; }")
           (lex-close "function a() { /* } */ return 1; }"))
        "a brace inside a block comment closes nothing")
    (is (= (count "function a() { // }\n return 1; }")
           (lex-close "function a() { // }\n return 1; }"))
        "a brace inside a line comment closes nothing")
    (is (= (count "function a() { const r = /\\d{2}/; return r; }")
           (lex-close "function a() { const r = /\\d{2}/; return r; }"))
        "a brace inside a regex literal closes nothing")
    (is (= (count "function a() { if (x) { y(); } return 1; }")
           (lex-close "function a() { if (x) { y(); } return 1; }"))
        "nested balanced braces are ordinary counting"))

  (testing "a body the lexer cannot close DOWNGRADES loudly; it never claims"
    (let [lines (vec (concat ["function broken() {"]
                             (repeat 20 "  doThing();")))
          source (str/join "\n" lines)
          {:keys [boundary from to body]} (ft/script-body source lines 1)]
      (is (str/starts-with? boundary "line-window(")
          "an unclosed body must be labelled a window")
      (is (str/includes? boundary "unclosed at L1"))
      (is (<= from 1))
      (is (>= to 1))
      (is (string? body))))

  (testing "the fixture's own function closes and the range is exact"
    (let [source (slurp (io/file fixture-root "resources/public/js/editor-commands.js"))
          lines (str/split source #"\n" -1)
          {:keys [from to boundary]} (ft/script-body source lines 389)]
      (is (= 389 from))
      (is (= 454 to))
      (is (= "brace-window(lexed,closed)" boundary)))))

;; ---------------------------------------------------------------------------
;; The alias hop
;; ---------------------------------------------------------------------------

(defn- write-file!
  [root relative content]
  (let [f (io/file root relative)]
    (.mkdirs (.getParentFile f))
    (spit f content)
    f))

(def alias-conventions
  {:repo-label "alias-fixture"
   :legs [{:id "menu-caller" :kind :use :globs ["src/views.clj"]}
          {:id "js-function" :kind :def :globs ["js/*.js"]}
          {:id "route" :kind :route :globs ["src/routes.clj"]}
          {:id "handler" :kind :handler :globs ["src/handlers.clj"]}
          {:id "tests" :kind :test :globs ["test/*.clj"]}]})

(defn- alias-fixture!
  [target-present?]
  (let [root (io/file (str (java.nio.file.Files/createTempDirectory
                             "feature-thread-alias"
                             (into-array java.nio.file.attribute.FileAttribute []))))]
    (write-file! root "src/views.clj" "(ns views)\n(def menu {:onclick \"formatDraft()\"})\n")
    (write-file! root "src/routes.clj"
                 "(ns routes)\n(def table [[\"/api/x/format\" {:post {:handler #'handlers/handle-format}}]])\n")
    (write-file! root "src/handlers.clj" "(ns handlers)\n(defn handle-format [r] r)\n")
    (write-file! root "test/t.clj"
                 "(ns t)\n(deftest x (post \"/api/x/format\") (handlers/handle-format nil))\n")
    (write-file! root "js/commands.js" "const formatDraft = runDraftFormatter;\n")
    (when target-present?
      (write-file! root "js/impl.js"
                   "// the implementation, under another name\nasync function runDraftFormatter() {\n  return 1;\n}\n"))
    root))

;; @spec MCP-OP-THREAD-007
(deftest an-alias-is-followed-one-hop-or-reported-alias-only
  (testing "the alias target is found: the receipt names the implementation"
    (let [root (alias-fixture! true)]
      (try
        (let [{:keys [structured]}
              (call! {:subject "formatDraft" :also ["/api/x/format"]
                      :config alias-conventions
                      :scope {:workspace_root (.getPath root)}})
              l (leg structured "js-function")]
          (is (= "FOUND" (:status l)))
          (is (= "js/impl.js" (:file l))
              "the alias line is not the implementation")
          (is (str/includes? (:evidence l) "one hop"))
          (is (str/includes? (:evidence l) "runDraftFormatter"))
          (is (str/includes? (:body l) "async function runDraftFormatter")))
        (finally (delete-tree! root)))))

  (testing "the alias target is absent: alias-only, never a four-of-five as five"
    (let [root (alias-fixture! false)]
      (try
        (let [{:keys [structured text]}
              (call! {:subject "formatDraft" :also ["/api/x/format"]
                      :config alias-conventions
                      :scope {:workspace_root (.getPath root)}})
              l (leg structured "js-function")]
          ;; @spec MCP-OP-THREAD-007
          ;; @spec MCP-OP-THREAD-024
          ;; Round-five review, finding 9: THREAD-024 lists `alias-only` among
          ;; the fallback evidences that make a leg CANDIDATE, and the code said
          ;; ABSENT. They now agree, and CANDIDATE is the truthful one: the verb
          ;; HAS a located range — the alias line — it simply does not vouch for
          ;; it as the definition.
          (is (= "CANDIDATE" (:status l)))
          (is (= "alias-only" (:evidence l)))
          (is (= "js/commands.js" (:file l))
              "the located range is the alias site itself")
          (is (seq (:weak_reason l))
              "a CANDIDATE names why it is only a candidate")
          (is (nil? (:anchor l))
              "a CANDIDATE never names an insertion point")
          (is (some #(str/includes? % "runDraftFormatter") (:searches l))
              "the search that followed the alias must be quoted")
          (is (= "INCOMPLETE (4 of 5)" (:status structured))
              "a CANDIDATE still does not count toward COMPLETE")
          (is (str/includes? text "alias-only")))
        (finally (delete-tree! root))))))

;; ---------------------------------------------------------------------------
;; The sibling
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-009
(deftest sibling-is-resolved-and-never-counts-toward-the-five-leg-status
  (testing "an explicit mirror resolves the feature the new one must copy"
    (let [{:keys [structured]}
          (call! {:subject "dequoteFormatSelection"
                  :also ["/api/transform/format" "mechanical-format"]
                  :mirror "formatDraft"
                  :budget_bytes ft/hard-cap-bytes
                  ;; Locations mode, deliberately: the sibling is a set of
                  ;; RANGES and asking for it should not also cost six leg
                  ;; bodies plus three co-menu-item peer bodies. In edit-basis
                  ;; mode on this fixture the structured face reaches the
                  ;; trunk's 32640-byte cap, which no `budget_bytes` can raise,
                  ;; and the stated order correctly cuts the sibling first.
                  :mode "locations"
                  :config smw-conventions
                  :scope {:workspace_root after-root}})
          sib (:sibling structured)]
      (is (= "FOUND" (:status sib)))
      (is (= "explicit-mirror" (:rule sib)))
      (is (= "formatDraft" (:seed sib)))
      (is (some #(and (= "js-function" (:id %))
                      (= "resources/public/js/editor-commands.js" (:file %)))
                (:legs sib))
          "the sibling's own JavaScript leg is located")
      (testing "the sibling's bodies are elided to ranges by default"
        (is (every? #(nil? (:body %)) (:legs sib))))
      (testing "and the sibling never changes the leg status"
        (is (= 6 (:legs_declared structured)))
        (is (= 6 (count (:legs structured)))))))

  (testing "an unresolvable sibling states the rule it applied"
    (let [root (alias-fixture! true)]
      (try
        (let [{:keys [structured]}
              (call! {:subject "formatDraft"
                      :config (assoc alias-conventions :sibling {:rule :none})
                      :scope {:workspace_root (.getPath root)}})]
          (is (= "ABSENT" (get-in structured [:sibling :status])))
          (is (= "none" (get-in structured [:sibling :rule]))))
        (finally (delete-tree! root))))))

;; ---------------------------------------------------------------------------
;; Budget and elision
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-011
(deftest over-budget-elides-in-the-stated-order-and-names-every-cut
  (testing "a small budget elides bodies, cheapest evidence first"
    (let [{:keys [structured text]} (thread! fixture-root {:budget_bytes 11000})]
      (is (true? (:ok structured)))
      (is (<= (:text_bytes structured) 11000)
          "the receipt must actually fit the budget it was given")
      (is (seq (:elided structured)) "an elision must be recorded")
      (testing "every elision names its leg, its bytes, its range and how to refetch"
        (doseq [e (:elided structured)]
          (is (string? (:leg e)))
          (is (string? (:reason e)))
          (is (string? (:refetch e)))
          (is (str/includes? text (:leg e))
              (str "the text does not name the elision of " (:leg e)))))
      (testing "the handler body is the LAST thing cut"
        (let [cut (set (map :leg (:elided structured)))]
          (when (not (contains? cut "handler"))
            (is (string? (:body (leg structured "handler")))
                "the handler kept its body while cheaper bodies were cut"))))
      (testing "an elided leg still carries its range and its hash"
        (doseq [l (:legs structured)
                :when (and (= "FOUND" (:status l)) (nil? (:body l)))]
          (is (integer? (:from l)))
          (is (re-matches #"[0-9a-f]{64}" (:sha256 l)))
          (is (string? (:refetch l)))))))

  (testing "every body elided still names every cut"
    (let [{:keys [structured text]} (thread! fixture-root {:budget_bytes 11264})]
      (is (true? (:ok structured)))
      (is (every? #(nil? (:body %)) (:legs structured)))
      (is (every? #(nil? (:body %))
                  (mapcat :co_primaries (:legs structured))))
      (is (= #{"sibling" "peers" "after-context" "governance-template"
               "peer-rows" "next-call" "menu-caller" "route" "tests(js)"
               "tests" "implementation" "js-function" "handler"}
             (set (map :leg (:elided structured))))
          "every step of the stated order is recorded when every body is cut")
      (is (str/includes? text "elided handler")))

  (testing "a budget nothing can fit REFUSES rather than truncating a body"
    (let [{:keys [error? structured text]} (thread! fixture-root {:budget_bytes 200})]
      (is (true? error?))
      (is (= "feature-thread-budget-exceeded" (:error_type structured)))
      (is (str/includes? text "budget")))))

  (testing "at the budget boundary"
    (let [exact (thread! fixture-root {:budget_bytes 16384})
          over (thread! fixture-root {:budget_bytes 16383})]
      (is (<= (get-in exact [:structured :text_bytes]) 16384))
      (is (<= (get-in over [:structured :text_bytes]) 16383))
      (is (true? (get-in exact [:structured :ok])))
      (is (true? (get-in over [:structured :ok])))))

  (testing "and at the fleet's smaller soft budget"
    (let [{:keys [structured]} (thread! fixture-root {:budget_bytes 11264})]
      (is (<= (:text_bytes structured) 11264))
      (is (seq (:elided structured))))))

;; ---------------------------------------------------------------------------
;; text is a superset of structuredContent
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-012
(deftest the-text-block-is-a-superset-of-the-structured-receipt
  (testing "on the real receipt, every structured leaf appears in the text"
    (let [{:keys [text structured]} (thread! fixture-root)]
      (doseq [[path value] (#'ft/leaf-paths structured [])
              :when (or (string? value) (number? value) (boolean? value))]
        (is (str/includes? text (str value))
            (str "the text block drops " (str/join "." path))))))

  (testing "the guarantee is mechanical: a field the designed lines forget still lands"
    (let [{:keys [structured]} (thread! fixture-root)
          smuggled (assoc structured :a_field_no_line_renders "smuggled-value-9f3c")
          text (ft/render-receipt smuggled)]
      (is (str/includes? text "smuggled-value-9f3c"))
      (is (str/includes? text "a_field_no_line_renders"))))

  (testing "and it holds on a refusal too"
    (let [{:keys [text structured]} (thread! fixture-root {:budget_bytes 200})]
      (doseq [[path value] (#'ft/leaf-paths structured [])
              :when (or (string? value) (number? value) (boolean? value))]
        (is (str/includes? text (str value))
            (str "the refusal text drops " (str/join "." path)))))))

;; ---------------------------------------------------------------------------
;; The MCP entrance contract
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-001
(deftest the-entrance-publishes-a-clock-and-a-structured-receipt
  (let [{:keys [text structured error?]} (thread! fixture-root)]
    (is (false? error?))
    (is (number? (:elapsed_ms structured)))
    (is (<= 0 (:elapsed_ms structured)))
    (is (str/includes? text (format "%.2f ms" (:elapsed_ms structured))))
    (is (= "feature_thread" (:operation structured)))
    (is (= "feature-thread/v2" (:receipt structured)))
    (testing "the declared tool is read-only"
      (is (= "feature_thread" (:name ft/feature-thread-tool)))
      (is (true? (get-in ft/feature-thread-tool [:annotations :read-only])))
      (is (false? (get-in ft/feature-thread-tool [:annotations :destructive]))))
    (testing "the structured half stays inside the trunk's public budget"
      (is (<= (:structured_bytes structured) ft/trunk-public-byte-budget)))
    (testing "the receipt round-trips as JSON"
      (is (map? (json/parse-string (json/generate-string structured) true))))))

;; ---------------------------------------------------------------------------
;; THE WARM-UP METER -- reported honestly
;; ---------------------------------------------------------------------------

(deftest warm-up-meter-rounds-to-a-complete-thread
  (testing "the human baseline in the transcript is SIX batched read rounds"
    (let [{:keys [structured]} (thread! fixture-root)
          bodies (+ (count (filter :body (:legs structured)))
                    (count (filter :body
                                   (mapcat :co_primaries (:legs structured)))))
          sites 7
          missing-after-receipt (- sites bodies)
          verb-rounds (+ 1 (if (pos? missing-after-receipt) 1 0))]
      (is (= sites bodies)
          (str "at the default budget every leg AND every co-primary arrives"
               " with its body in round one"))
      (is (= 1 verb-rounds)
          (str "rounds to a complete thread: " verb-rounds
               " (one call; nothing left to read afterwards). Human baseline: 6."))
      (is (= 0 missing-after-receipt)
          "nothing the caller must still read after the receipt"))))

;; ---------------------------------------------------------------------------
;; ROUND TWO -- the six sites the real edit touched
;;
;; Ground truth for every assertion below is the `diff -u` of the two fixture
;; trees: components.clj @106, editor-commands.js @453, transform.clj @131,
;; registry.edn @401, browser_runtime_classic_script_test.js @96,
;; transform_apply_test.clj @383. Round one covered five of the six; each
;; witness here closes one of the gaps, and each fails when its behaviour is
;; reverted.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-014
(deftest conventions-may-declare-more-than-the-five-leg-roles
  (testing "a SIXTH declared leg is accepted and resolved like any other"
    (let [six (update smw-conventions :legs conj
                      {:id "extra-def" :kind :def :globs ["src/**/*.clj"]})
          {:keys [structured]} (thread! fixture-root {:config six})]
      (is (true? (:ok structured))
          "a convention set with six leg roles must not be refused")
      (is (some #(= "extra-def" (:id %)) (:legs structured))
          "the sixth declared leg is missing from the receipt")
      (is (= "src/writer/handlers/transform.clj"
             (:file (leg structured "extra-def"))))))

  (testing "FOUR leg roles is still a typed refusal that names the count"
    (let [{:keys [structured]}
          (thread! fixture-root {:config (update smw-conventions :legs pop)})]
      (is (= "feature-thread-conventions-invalid" (:error_type structured)))
      (is (str/includes? (:error structured) "at least the five"))
      (is (str/includes? (:error structured) "found 4")))))

;; @spec MCP-OP-THREAD-015
(deftest def-legs-recognise-clojure-definitions
  (testing "the definition-shaped search covers (defn / (defn- / (def"
    (let [[[label regex]] (ft/searches-for-kind
                            :def {:identifiers ["mechanical-format"] :routes []})]
      (is (= "definition-shaped" label))
      (is (re-find (re-pattern regex) "(defn mechanical-format")
          "a Clojure defn is definition-shaped")
      (is (re-find (re-pattern regex) "(defn- mechanical-format")
          "so is a private defn")
      (is (re-find (re-pattern regex) "(def mechanical-format")
          "and so is a plain def")))

  (testing "and a :def leg over Clojure sources therefore lands on the form"
    (let [{:keys [structured]} (thread! fixture-root)
          impl (leg structured "implementation")]
      (is (= "FOUND" (:status impl)))
      (is (= "src/writer/handlers/transform.clj" (:file impl)))
      (is (= "mechanical-format" (:form_name impl))))))

;; @spec MCP-OP-THREAD-016
(deftest the-implementation-leg-is-automatic-deduped-and-honestly-uncounted
  (testing "a five-leg conventions file gets the definition leg for free"
    (let [{:keys [structured text]} (thread! fixture-root)
          impl (leg structured "implementation")]
      (is (= 5 (count (:legs smw-conventions)))
          "the fixture's conventions file declares five roles and is unchanged")
      (is (= 6 (count (:legs structured))))
      (is (= "FOUND" (:status impl)))
      (testing "at the form the real edit inserted after"
        (is (= "src/writer/handlers/transform.clj" (:file impl)))
        (is (= 81 (:from impl)))
        (is (= 132 (:to impl)))
        (is (= "form(parsed)" (:boundary impl)))
        (is (= "after:L132" (:anchor impl))
            "the transcript's patch put mechanical-format-selection at L133"))
      (testing "and it counts toward the status"
        (is (= "COMPLETE (6 of 6)" (:status structured)))
        (is (= 6 (:legs_declared structured))))
      (is (str/includes? text "leg implementation"))))

  (testing "it never duplicates a leg the receipt already carries"
    (let [{:keys [structured]} (thread! fixture-root)
          impl (leg structured "implementation")
          others (remove #(= "implementation" (:id %)) (:legs structured))]
      (is (not-any? #(and (= (:file %) (:file impl))
                          (= (:from %) (:from impl))
                          (= (:to %) (:to impl)))
                    others)
          "the implementation leg repeated a range another leg already shipped")
      (is (not= (:from (leg structured "handler")) (:from impl))
          "the handler form is not re-printed as the implementation leg")))

  (testing "no seed naming a definition is N/A -- not ABSENT, and NOT counted"
    (let [{:keys [structured text]}
          (call! {:subject "/api/transform/format"
                  :config smw-conventions
                  :scope {:workspace_root fixture-root}})
          impl (leg structured "implementation")]
      (is (= "N/A" (:status impl)))
      (is (= "no seed names a definition" (:reason impl)))
      (is (= 5 (:legs_declared structured))
          "an inapplicable leg must not make a whole thread read INCOMPLETE")
      (is (not (str/includes? (:status structured) "of 6")))
      (is (str/includes? text "n/a (no seed names a definition)"))))

  (testing "a conventions file that declares its own implementation leg keeps it"
    (let [own (update smw-conventions :legs conj
                      {:id "implementation" :kind :def
                       :globs ["src/**/handlers/*.clj"]})
          {:keys [structured]} (thread! fixture-root {:config own})]
      (is (= 6 (count (:legs structured))))
      (is (= 1 (count (filter #(= "implementation" (:id %)) (:legs structured))))
          "the automatic leg was added a second time"))))

;; @spec MCP-OP-THREAD-017
(deftest governance-rows-carry-an-entry-end-and-an-anchor
  (let [{:keys [structured text]} (thread! fixture-root)
        rows (get-in structured [:rules :governance])
        conf (first (filter #(str/includes? (:match %) "EDITOR-CONF-005") rows))]
    (testing "the matched registry entry ends where the parser says it ends"
      (is (some? conf) "the EDITOR-CONF-005 row is missing")
      (is (= 382 (:line conf)))
      (is (= 382 (:form_start conf)))
      (is (= 400 (:form_end conf)))
      (is (= "after:L400" (:anchor conf))
          "the real edit inserted :EDITOR-DEQUOTE-016 at L401"))

    (testing "a hit whose entry does not resolve says so rather than guessing"
      (is (some #(= "unparsed" (:anchor %)) rows)
          "the redacted registry has hits with no resolvable entry, and the
           receipt must label them rather than invent an anchor")
      (doseq [r rows]
        (is (string? (:anchor r)) (str "row " (:file r) ":" (:line r)))
        (is (string? (:refetch r)))))

    (testing "one template row: the matched entry with the HIGHEST line"
      (let [t (get-in structured [:rules :governance_template])]
        (is (some? t))
        (is (= "docs/intent/registry.edn" (:file t)))
        (is (= 382 (:from t)))
        (is (= 400 (:to t)))
        (is (= "after:L400" (:anchor t)))
        (is (>= (:line conf) (apply max (map :line (filter :form_end rows))))
            "the template is not the last matched entry")
        (is (str/includes? text "governance-template"))
        (is (nil? (:body t)) "a template entry is a range, never inlined")))))

;; @spec MCP-OP-THREAD-018
(deftest the-tests-leg-has-one-primary-per-language
  (let [{:keys [structured text]} (thread! fixture-root)
        tests (leg structured "tests")
        co (:co_primaries tests)
        js (first (filter #(= js-test-owner (:file %)) co))]
    (testing "the Clojure primary is the deftest that calls the handler"
      (is (= "test/writer/handlers/transform_apply_test.clj" (:file tests)))
      (is (= "after:L384" (:anchor tests))))

    (testing "the JavaScript primary is a LEG row with its own boundary and hash"
      (is (some? js) "the node test file is not a co-primary of the tests leg")
      (is (= 63 (:from js)))
      (is (= 94 (:to js))
          "the enclosing test( call ends at L94; the real edit appended at L96")
      (is (= "after:L94" (:anchor js)))
      (is (= "js" (:language js)))
      (is (str/includes? (:boundary js) "brace-window(lexed,closed)"))
      (is (str/includes? (:boundary js) "test-call at L63")
          "the anchor must name the enclosing call, not the assertion line")
      (is (= (:sha256 js) (ft/sha256-hex (:body js))))
      (is (str/starts-with? (:body js) "test("))
      (is (str/includes? text "leg tests(js)")
          "a co-primary is rendered as a leg row, never as an also row"))

    (testing "and it is not ALSO printed as a secondary witness"
      (is (not-any? #(= js-test-owner (:file %)) (:also tests))))))

;; @spec MCP-OP-THREAD-019
(deftest the-rules-row-names-how-to-run-the-tests-leg
  (let [{:keys [structured text]} (thread! fixture-root)
        verify (get-in structured [:rules :verify])]
    (testing "the Makefile target whose recipe NAMES the JavaScript test file"
      (is (some #(= {:target "test-js" :line 233
                     :command "node --test test/js/browser_runtime_classic_script_test.js"
                     :make_prefix "@"
                     :for js-test-owner
                     :evidence "names-the-file"}
                    %)
                verify)
          (str "no verify row names the node test; rows were " (pr-str verify))))

    (testing "and, for the Clojure test, the alias-running target, labelled"
      (is (some #(and (= "runtests-unit" (:target %))
                      (= 283 (:line %))
                      (= "clojure -M:test:run-tests unit" (:command %))
                      (= "alias" (:evidence %)))
                verify)
          "the Clojure test is run by an alias target and must say so"))

    (is (str/includes? text "verify test-js"))

    (testing "no Makefile is a reason, never a silent empty row"
      (let [scratch (scratch-copy! fixture-root "feature-thread-nomake")]
        (try
          (.delete (io/file scratch "Makefile"))
          (let [{:keys [structured]} (thread! (.getPath scratch))]
            (is (= [] (get-in structured [:rules :verify])))
            (is (= "no Makefile at the workspace root"
                   (get-in structured [:rules :verify_reason]))))
          (finally (delete-tree! scratch)))))))

;; @spec MCP-OP-THREAD-020
(deftest the-budget-default-and-the-elision-order-are-edit-aware
  (testing "the default fits the six-leg receipt with every body"
    (is (= 28672 ft/default-budget-bytes))
    (let [{:keys [structured]} (thread! fixture-root)]
      (is (empty? (:elided structured))
          (str "the default budget elided " (pr-str (map :leg (:elided structured)))
               "; the whole point of raising it was that it must not"))))

  (testing "the stated order elides context first and the edit sites last"
    (is (= [:sibling :peers :after-context :governance-template
            :secondary-tests :peer-rows :next-call :menu
            :route :tests-js :tests :implementation :js-function :handler]
           ft/elision-order)))

  (testing "under pressure the handler and the seeds' definitions keep their bodies"
    (let [{:keys [structured]} (thread! fixture-root {:budget_bytes 21000})
          cut (set (map :leg (:elided structured)))]
      (is (seq cut) "21000 bytes must force at least one cut")
      (is (contains? cut "sibling"))
      (is (string? (:body (leg structured "handler")))
          "the handler body was cut while cheaper context survived")
      (is (string? (:body (leg structured "implementation")))
          "the definition the seed names was cut before the sibling")))

  (testing "at the ranges-only floor every leg still names its range, its hash and its anchor"
    (let [{:keys [structured text]} (thread! fixture-root {:budget_bytes 11264})]
      (is (true? (:ok structured)))
      (is (<= (:text_bytes structured) 11264))
      (doseq [l (:legs structured)
              :when (= "FOUND" (:status l))]
        (is (integer? (:from l)) (str (:id l)))
        (is (re-matches #"[0-9a-f]{64}" (:sha256 l)) (str (:id l)))
        (is (string? (:anchor l)) (str (:id l)))
        (is (str/includes? text (:sha256 l)) (str (:id l))))
      (testing "and the text is still a superset of the structured receipt"
        (doseq [[path value] (#'ft/leaf-paths structured [])
                :when (or (string? value) (number? value) (boolean? value))]
          (is (str/includes? text (str value))
              (str "the text block drops " (str/join "." path))))))))

;; @spec MCP-OP-THREAD-021
(deftest the-assert-line-costs-the-caller-no-calls
  (testing "a naive reader obeyed the old wording with six refetch calls"
    (let [{:keys [structured text]} (thread! fixture-root)
          line (get-in structured [:rules :assert])]
      (is (str/includes? line "do NOT re-read")
          "the assert line must forbid re-reading the ranges it just shipped")
      (is (str/includes? line "admit_clojure_patch"))
      (is (str/includes? line "enforces nothing itself")
          "the line must be advisory: this verb cannot issue that refusal")
      (is (not (str/includes? line "re-hash each leg")))
      (is (str/includes? text "do NOT re-read")))))

;; @spec MCP-OP-THREAD-022
(deftest the-header-names-the-number-the-budget-governs
  (let [{:keys [structured text]} (thread! fixture-root {:budget_bytes 32768})
        header (first (str/split-lines text))]
    (is (str/includes? header (str "text=" (:text_bytes structured)
                                   "B (budget 32768B)"))
        (str "the header does not say which number the budget governs: " header))
    (is (str/includes? header (str "structured=" (:structured_bytes structured)
                                   "B (trunk cap " ft/trunk-public-byte-budget "B)")))
    (is (str/includes? header (str "total=" (:receipt_bytes structured) "B")))
    (is (not (str/includes? header "used="))
        "`used=` beside `budget=` read as an overrun; it is gone")
    (is (str/includes? header "status=COMPLETE (6 of 6) — legs, not bytes")
        "COMPLETE is about legs and the header must say so")))

;; ---------------------------------------------------------------------------
;; ROUND-ONE REVIEW (Opus, 2026-09-04, NO-GO) -- the two false greens and the
;; four findings, each as a witness that fails without the fix.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-023
(deftest a-regex-after-a-keyword-is-not-a-division
  (testing "B1: `return /[}]/` lexed as division truncated a body and called it closed"
    (let [src "function trapReturnRegex(s) {\n  return /[}]/.test(s);\n}\n"
          lines (str/split src #"\n" -1)
          {:keys [from to boundary body]} (ft/script-body src lines 1)]
      (is (= 1 from))
      (is (= 3 to)
          "the body must run to the function's own closing brace, not to the
           brace inside the regex literal")
      (is (= "brace-window(lexed,closed)" boundary))
      (is (str/ends-with? (str/trim body) "}"))))

  (testing "every keyword that ends in an identifier character"
    (doseq [kw ["return" "typeof" "case" "in" "of" "new" "delete" "void"
                "yield" "do" "else" "instanceof" "throw" "await"]]
      (let [src (str "function f(s) {\n  " kw " /[}]/;\n}\n")
            lines (str/split src #"\n" -1)]
        (is (= 3 (:to (ft/script-body src lines 1)))
            (str "a regex after `" kw "` was lexed as a division")))))

  (testing "and a genuine division is still a division"
    (let [src "function g(a, b) {\n  const r = a / b / 2;\n  return r;\n}\n"
          lines (str/split src #"\n" -1)]
      (is (= 4 (:to (ft/script-body src lines 1))))))

  (testing "the token scanner itself"
    (is (= "return" (ft/word-before "  return /x/" 9)))
    (is (= "a" (ft/word-before "a / b" 2)))
    (is (nil? (ft/word-before "( /x/" 2)))))

;; @spec MCP-OP-THREAD-024
(deftest a-comment-mention-is-a-candidate-and-never-completes-a-thread
  (testing "B2: a subject that exists only in comments and strings"
    (let [scratch (scratch-copy! fixture-root "feature-thread-ghost")]
      (try
        (spit (io/file scratch "src/writer/views/components.clj")
              "\n;; TODO: someday add ghostFeature to the Edit menu\n"
              :append true)
        (spit (io/file scratch "src/writer/routes.clj")
              "\n;; note: ghostFeature will get a route one day\n"
              :append true)
        (spit (io/file scratch "src/writer/handlers/transform.clj")
              "\n;; ghostFeature will live here\n"
              :append true)
        (spit (io/file scratch "resources/public/js/editor-commands.js")
              "\nconst NOTE = \"ghostFeature is not implemented\";\n"
              :append true)
        (let [{:keys [structured text]}
              (call! {:subject "ghostFeature"
                      :config smw-conventions
                      :scope {:workspace_root (.getPath scratch)}})]
          (is (str/starts-with? (:status structured) "INCOMPLETE")
              (str "a feature that exists only in comments must never read"
                   " COMPLETE; got " (:status structured)))
          (is (false? (:complete structured)))
          (doseq [l (:legs structured)]
            (is (not= "FOUND" (:status l))
                (str "leg " (:id l) " was promoted to FOUND by a mention:"
                     " evid=" (:evidence l) " boundary=" (:boundary l))))
          (testing "a candidate says WHY it is only a candidate"
            (doseq [l (:legs structured)
                    :when (= "CANDIDATE" (:status l))]
              (is (string? (:weak_reason l)))
              (is (str/includes? text (:weak_reason l)))))
          (testing "and a fallback hit is never stamped identifier(def)"
            (doseq [l (:legs structured)]
              (is (not= "identifier(def)" (:evidence l))
                  (str "leg " (:id l) " called a fallback hit a definition")))))
        (finally (delete-tree! scratch)))))

  (testing "the strength rule itself"
    (is (= "FOUND" (:status (ft/leg-strength {:boundary "form(parsed)"
                                              :evidence "route-literal"}))))
    (is (= "FOUND" (:status (ft/leg-strength
                              {:boundary "brace-window(lexed,closed)"
                               :evidence "identifier(def)"}))))
    (is (= "CANDIDATE"
           (:status (ft/leg-strength {:boundary "form(parsed)"
                                      :evidence "route-assembled"})))
        "an assembled route match is a lead, not the leg")
    (is (= "CANDIDATE"
           (:status (ft/leg-strength
                      {:boundary "line-window(no-enclosing-top-level-form)"
                       :evidence "route-literal"})))
        "a line window does not know where the form ends")
    (is (= "CANDIDATE"
           (:status (ft/leg-strength {:boundary "form(parsed)"
                                      :evidence "identifier-or-route"
                                      :in-comment? true})))))

  (testing "the comment lexer"
    (is (true? (ft/comment-mention? ";; formatDraft here" 3 true)))
    (is (false? (ft/comment-mention? "(formatDraft)" 1 true)))
    (is (false? (ft/comment-mention? "(x \"a;b\" formatDraft)" 9 true))
        "a semicolon inside a string does not start a comment")
    (is (true? (ft/comment-mention? "// formatDraft" 3 false)))
    (is (false? (ft/comment-mention? "const formatDraft = 1;" 6 false)))))

;; @spec MCP-OP-THREAD-025
(deftest the-receipt-hands-over-a-binding-the-write-gate-can-use
  (let [{:keys [structured text]} (thread! fixture-root)
        n (:next_call structured)]
    (is (= "admit_clojure_patch" (:tool n))
        "the receipt must name the call that can actually bind a pre-image")
    (testing "the digests are over WHOLE FILES -- admit's own subject"
      (doseq [[file sha] (:expect_pre_sha256 n)]
        (is (= sha (ft/sha256-hex (slurp (io/file fixture-root (name file)))))
            (str "expect_pre_sha256 for " (name file)
                 " is not the digest of the whole file"))))
    (testing "every located leg's file is covered"
      (doseq [l (:legs structured)
              :when (contains? #{"FOUND" "CANDIDATE"} (:status l))]
        (is (contains? (set (map name (keys (:expect_pre_sha256 n)))) (:file l))
            (str (:id l) "'s file is missing from expect_pre_sha256"))))
    (is (str/includes? text "next_call admit_clojure_patch"))

    (testing "and it is elided under budget pressure with a named refetch"
      (let [{:keys [structured]} (thread! fixture-root {:budget_bytes 11264})]
        (is (nil? (:next_call structured)))
        (is (some #(= "next-call" (:leg %)) (:elided structured)))))))

;; @spec MCP-OP-THREAD-026
(deftest the-receipt-byte-counts-describe-the-delivered-text
  ;; @spec MCP-OP-THREAD-026
  ;; Several budgets, because this has failed twice for the same reason: a
  ;; number the receipt prints ABOUT ITSELF leaked into the superset haystack,
  ;; so the completion line flipped with the digit count and `measure`'s
  ;; fixpoint never settled. Once it was the clock; once it was the header's
  ;; own byte counts, at budget 12000 only.
  (testing "text_bytes is the size of the text block the caller receives"
    (doseq [budget [nil 32768 20000 15000 13000 12000 11264]]
      (let [{:keys [text structured]}
            (thread! fixture-root (if budget {:budget_bytes budget} {}))]
        (is (= (:text_bytes structured) (ft/utf8-bytes text))
            (str "budget " budget ": receipt claims " (:text_bytes structured)
                 " bytes of text and delivered " (ft/utf8-bytes text))))))

  (testing "the clock rides inside the measured receipt at a fixed width"
    (is (= ft/receipt-tail-bytes (count (ft/receipt-tail nil))))
    (is (= ft/receipt-tail-bytes (count (ft/receipt-tail 478.511625))))
    (is (= ft/receipt-tail-bytes (count (ft/receipt-tail 0))))
    (let [{:keys [text structured]} (thread! fixture-root)]
      (is (str/includes? text (str "elapsed_ms=" (:elapsed_ms structured))))))

  (testing "a refusal quotes the budget the CALLER asked for"
    (let [{:keys [structured text]} (thread! fixture-root {:budget_bytes 200})]
      (is (= 200 (:budget_bytes structured)))
      (is (str/includes? (:error structured) "budget of 200"))
      (is (not (str/includes? text "-95"))
          "an internal reserve subtraction must never leak into a refusal")
      (is (nil? (:receipt_bytes structured))
          "receipt_bytes means text+structured everywhere or it is absent")
      (is (nil? (:text_bytes structured))
          "text_bytes means the DELIVERED text everywhere or it is absent")
      (is (integer? (:would_be_text_bytes structured))
          "the refusal still says how big the receipt would have been"))))

;; @spec MCP-OP-THREAD-027
(deftest subject-and-also-have-named-admission-ceilings
  (testing "an oversized subject is refused BEFORE any file is read"
    (let [{:keys [error? structured text]}
          (thread! fixture-root {:subject (apply str (repeat 10001 \x))})]
      (is (true? error?))
      (is (= "feature-thread-subject-too-long" (:error_type structured)))
      (is (= "subject" (:field structured))
          "the refusal must name the field the caller got wrong")
      (is (= ft/max-subject-chars (:max_subject_chars structured)))
      (is (str/includes? text (str ft/max-subject-chars)))
      (is (nil? (:legs structured)) "no leg was resolved, so no file was read")))

  (testing "too many also seeds"
    (let [{:keys [structured]}
          (thread! fixture-root {:also (vec (repeat (inc ft/max-also-seeds) "x"))})]
      (is (= "feature-thread-also-too-many" (:error_type structured)))
      (is (= "also" (:field structured)))))

  (testing "an oversized also seed"
    (let [{:keys [structured]}
          (thread! fixture-root {:also [(apply str (repeat 600 \y))]})]
      (is (= "feature-thread-also-seed-too-long" (:error_type structured)))
      (is (= "also" (:field structured)))))

  (testing "and the ceiling is a real bound, asserted AT the ceiling"
    (let [{:keys [structured]}
          (thread! fixture-root {:subject (apply str (repeat ft/max-subject-chars \z))})]
      (is (not= "feature-thread-subject-too-long" (:error_type structured))
          "a subject exactly at the ceiling is admitted"))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR -- B3: the automatic implementation leg must be WALKED over its
;; own globs, its N/A reason must name the seed it is about, and a leg that was
;; never scanned must never be silently dropped from the denominator.
;; ---------------------------------------------------------------------------

(defn- write-file!
  [^java.io.File root relative ^String content]
  (let [f (io/file root relative)]
    (.mkdirs (.getParentFile f))
    (spit f content)
    f))

(defn- lines-of
  [^java.io.File root relative]
  (str/split (slurp (io/file root relative)) #"\n" -1))

(def ^:private mechanical-format-range
  "`(defn mechanical-format …)` in the fixture handler, 1-based inclusive."
  [81 132])

(defn- mechanical-format-source
  [^java.io.File root]
  (let [ls (lines-of root "src/writer/handlers/transform.clj")
        [from to] mechanical-format-range]
    (str/join "\n" (subvec (vec ls) (dec from) to))))

(defn- move-implementation-out-of-scope!
  "Move `(defn mechanical-format …)` verbatim into src/writer/other/dup.clj --
  still under src/, still .clj, still matched by the automatic leg's own
  `src/**/*.clj`, and outside EVERY glob the convention set declares."
  [^java.io.File root]
  (let [body (mechanical-format-source root)
        ls (vec (lines-of root "src/writer/handlers/transform.clj"))
        [from to] mechanical-format-range]
    (write-file! root "src/writer/handlers/transform.clj"
                 (str/join "\n" (concat (subvec ls 0 (dec from)) (subvec ls to))))
    (write-file! root "src/writer/other/dup.clj"
                 (str "(ns writer.other.dup)\n\n" body "\n"))))

;; @spec MCP-OP-THREAD-028
(deftest the-automatic-implementation-leg-is-scanned-over-its-own-globs
  (testing "a definition a seed names, outside every DECLARED glob, is found"
    (let [scratch (scratch-copy! fixture-root "feature-thread-falsefx")]
      (try
        (move-implementation-out-of-scope! scratch)
        (is (str/includes? (slurp (io/file scratch "src/writer/other/dup.clj"))
                           "(defn mechanical-format")
            "precondition: the definition really is in the out-of-scope file")
        (is (not (str/includes?
                   (slurp (io/file scratch "src/writer/handlers/transform.clj"))
                   "(defn mechanical-format"))
            "precondition: it is no longer in any declared leg's file")
        (let [{:keys [text structured]} (thread! (.getPath scratch))
              impl (leg structured "implementation")]
          (is (= "FOUND" (:status impl))
              (str "the implementation leg reported " (:status impl)
                   " while the definition sat unread in src/writer/other/dup.clj"))
          (is (= "src/writer/other/dup.clj" (:file impl)))
          (is (str/includes? text "src/writer/other/dup.clj")
              "the receipt never names the file that holds the definition")
          (is (not (str/includes? text "COMPLETE (5 of 5)"))
              "COMPLETE with the definition leg uncounted is a false green")
          (is (= "COMPLETE (6 of 6)" (:status structured))))
        (finally (delete-tree! scratch))))))

;; @spec MCP-OP-THREAD-029
(deftest two-definitions-of-one-seed-are-both-named
  (testing "the second owner of a seed's name is reported, not silently dropped"
    (let [scratch (scratch-copy! fixture-root "feature-thread-twodef")]
      (try
        (write-file! scratch "src/writer/other/dup.clj"
                     (str "(ns writer.other.dup)\n\n"
                          (mechanical-format-source scratch) "\n"))
        (let [{:keys [text structured]} (thread! (.getPath scratch))
              impl (leg structured "implementation")
              named (set (concat [(:file impl)] (map :file (:also impl))))]
          (is (= "FOUND" (:status impl)))
          (is (contains? named "src/writer/other/dup.clj")
              (str "the receipt names only " named
                   " -- the second definition of the seed is unmentioned"))
          (is (str/includes? text "src/writer/other/dup.clj")))
        (finally (delete-tree! scratch))))))

;; @spec MCP-OP-THREAD-030
(deftest an-uncounted-implementation-leg-says-which-seed-and-whether-it-was-scanned
  (testing "the N/A reason names the SEED it is about, never an unrelated leg"
    (let [{:keys [structured]} (call! {:subject "formatDraft"
                                       :config smw-conventions
                                       :scope {:workspace_root fixture-root}})
          impl (leg structured "implementation")]
      (is (= "N/A" (:status impl)))
      (is (str/includes? (:reason impl) "formatDraft")
          (str "the reason does not name the seed it is about: " (:reason impl)))))

  (testing "a leg whose globs were never walked is UNSCANNED and IS counted"
    (let [conv (ft/normalize-conventions smw-conventions "test")
          conventions (:conventions conv)
          auto (ft/implementation-leg conventions)
          cache (ft/make-cache fixture-root)
          resolved (ft/resolve-implementation
                     cache [] {:identifiers ["mechanical-format"] :routes []}
                     auto [] [])]
      (is (= "UNSCANNED" (:status resolved))
          (str "a leg whose globs " (:globs auto)
               " were not in the walk reported " (:status resolved)))
      (is (str/includes? (str (:reason resolved)) "src/**/*.clj"))
      (is (false? (:complete (ft/thread-status [{:id "a" :status "FOUND"} resolved])))
          "an UNSCANNED leg must never be dropped from the denominator"))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR -- B1': an unescaped `/` inside a regex CHARACTER CLASS
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-031
(deftest a-slash-inside-a-regex-character-class-does-not-end-the-regex
  (testing "`/[/}]/` is valid JavaScript; the `}` inside it closes nothing"
    (doseq [[label literal] [["char class holding a slash and a brace" "/[/}]/"]
                             ["negated class" "/[^/}]/"]
                             ["class holding both braces" "/[/{}]/"]
                             ["escaped closing bracket in the class" "/[/\\]}]/"]
                             ["the natural instance" "/[/\\\\]/"]]]
      (let [src (str "function trapRegexCharClassSlash(s) {\n"
                     "  const re = " literal ";\n"
                     "  return re.test(s);\n"
                     "}\n")
            lines (str/split src #"\n" -1)
            {:keys [to boundary body]} (ft/script-body src lines 1)]
        (is (= 4 to)
            (str label ": the body ended at L" to
                 " -- the `}` inside " literal " was counted as the function's"
                 " closing brace"))
        (is (= "brace-window(lexed,closed)" boundary) label)
        (is (str/ends-with? (str/trim (str body)) "}") label))))

  (testing "a split on a character class, the shape that happened to recover"
    (let [src (str "function splitPathSegments(p) {\n"
                   "  return p.split(/[/\\\\]/);\n"
                   "}\n")
          lines (str/split src #"\n" -1)]
      (is (= 3 (:to (ft/script-body src lines 1))))))

  (testing "and a character class is still exited by its own `]`"
    (let [src (str "function afterClass(a, b) {\n"
                   "  const m = /[abc]/;\n"
                   "  const q = {x: 1};\n"
                   "  return m.test(a) ? q : b;\n"
                   "}\n")
          lines (str/split src #"\n" -1)]
      (is (= 5 (:to (ft/script-body src lines 1)))
          "a `]` that closes the class must return the lexer to regex body")))

  (testing "a wrong range is never labelled closed, end to end through the verb"
    (let [scratch (scratch-copy! fixture-root "feature-thread-charclass")]
      (try
        (write-file! scratch "resources/public/js/charclass.js"
                     (str "function trapRegexCharClassSlash(s) {\n"
                          "  const re = /[/}]/;\n"
                          "  return re.test(s);\n"
                          "}\n"))
        (let [{:keys [structured]}
              (call! {:subject "trapRegexCharClassSlash"
                      :config smw-conventions
                      :scope {:workspace_root (.getPath scratch)}})
              js (leg structured "js-function")]
          (is (= "resources/public/js/charclass.js" (:file js)))
          (is (= 4 (:to js))
              (str "the published body stops at L" (:to js)
                   " with boundary " (:boundary js)))
          (is (str/includes? (str (:body js)) "return re.test(s);")
              "the receipt published a truncated function body"))
        (finally (delete-tree! scratch))))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR -- B2': `(comment …)`, `#_` and multi-line `/* … */`
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-032
(deftest a-comment-form-a-discard-and-a-block-comment-are-comment-mentions
  (testing "a hit whose enclosing top-level form is `(comment …)`"
    (let [scratch (scratch-copy! fixture-root "feature-thread-commentform")]
      (try
        (spit (io/file scratch "src/writer/handlers/transform.clj")
              "\n\n(comment\n  (widgetize {:draft \"x\"}))\n"
              :append true)
        (let [{:keys [text structured]}
              (call! {:subject "widgetize"
                      :config smw-conventions
                      :scope {:workspace_root (.getPath scratch)}})
              h (leg structured "handler")]
          (is (= "CANDIDATE" (:status h))
              (str "a mention inside (comment …) was reported " (:status h)
                   " with weak_reason " (pr-str (:weak_reason h))))
          (is (str/includes? (str (:weak_reason h)) "comment"))
          (is (str/starts-with? (:status structured) "INCOMPLETE")
              (str "a thread whose handler leg is only a (comment …) decoy read "
                   (:status structured)))
          (is (str/includes? text "INCOMPLETE")))
        (finally (delete-tree! scratch)))))

  (testing "a hit discarded by `#_`"
    (let [scratch (scratch-copy! fixture-root "feature-thread-discard")]
      (try
        (spit (io/file scratch "src/writer/handlers/transform.clj")
              "\n\n#_(widgetize {:draft \"x\"})\n"
              :append true)
        (let [{:keys [structured]}
              (call! {:subject "widgetize"
                      :config smw-conventions
                      :scope {:workspace_root (.getPath scratch)}})
              h (leg structured "handler")]
          (is (= "CANDIDATE" (:status h))
              (str "a mention after #_ was reported " (:status h)))
          (is (str/includes? (str (:weak_reason h)) "comment")))
        (finally (delete-tree! scratch))))))

;; @spec MCP-OP-THREAD-032
(deftest a-multi-line-block-comment-is-a-comment-mention
  (testing "a subject mentioned only inside a JavaScript /* … */ block"
    (let [scratch (scratch-copy! fixture-root "feature-thread-blockcomment")]
      (try
        (write-file! scratch "resources/public/js/ghost.js"
                     (str "/*\n"
                          " * function ghostFeature(x) { return x; }\n"
                          " */\n"
                          "function realThing() { return 1; }\n"))
        (let [{:keys [structured]}
              (call! {:subject "ghostFeature"
                      :config smw-conventions
                      :scope {:workspace_root (.getPath scratch)}})
              js (leg structured "js-function")]
          (is (= "CANDIDATE" (:status js))
              (str "a mention inside /* … */ was reported " (:status js)))
          (is (str/includes? (str (:weak_reason js)) "comment")))
        (finally (delete-tree! scratch)))))

  (testing "the predicate itself, over the file's own text"
    (let [clj "(defn live [] :ok)\n(comment\n  (live))\n#_(live)\n"
          js "/*\n * ghost()\n */\nghost();\n"
          commented-out? (or (resolve 'clj-surgeon.mcp-feature-thread/commented-out?)
                             (constantly ::no-such-predicate))]
      (is (false? (commented-out? clj 1 true))
          "a live definition is not commented out")
      (is (true? (commented-out? clj 3 true))
          "line 3 is inside a (comment …) form")
      (is (true? (commented-out? clj 4 true))
          "line 4 is discarded by #_")
      (is (true? (commented-out? js 2 false))
          "line 2 is inside a /* … */ block")
      (is (false? (commented-out? js 4 false))
          "line 4 is live code after the block closed"))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR -- 3.5: the walk's containment test and its duplicates
;; ---------------------------------------------------------------------------

(defn- symlink!
  [^java.io.File link ^java.io.File target]
  (java.nio.file.Files/createSymbolicLink
    (.toPath link) (.toPath target)
    (into-array java.nio.file.attribute.FileAttribute [])))

;; @spec MCP-OP-THREAD-033
(deftest the-walk-confines-to-the-root-and-lists-each-file-once
  (testing "a sibling directory whose name merely EXTENDS the root's is OUTSIDE it"
    (let [base (io/file (str (java.nio.file.Files/createTempDirectory
                               "feature-thread-confine"
                               (into-array java.nio.file.attribute.FileAttribute []))))
          root (io/file base "repo")
          evil (io/file base "repo-evil")]
      (try
        (write-file! root "src/own.clj" "(ns own)\n")
        (write-file! evil "stolen.clj" "(ns stolen)\n")
        (symlink! (io/file root "hop") evil)
        (let [{:keys [ok paths]} (ft/walk-relative-paths (.getPath root) nil ["**/*.clj"])]
          (is ok)
          (is (not-any? #(str/includes? % "stolen") paths)
              (str "a file in the sibling `repo-evil` was reported as a path"
                   " inside `repo`: " (pr-str paths))))
        (finally (delete-tree! base)))))

  (testing "a symlink to a directory INSIDE the tree does not list its files twice"
    (let [root (io/file (str (java.nio.file.Files/createTempDirectory
                               "feature-thread-dupwalk"
                               (into-array java.nio.file.attribute.FileAttribute []))))]
      (try
        (write-file! root "src/a.clj" "(ns a)\n")
        (symlink! (io/file root "alias") (io/file root "src"))
        (let [{:keys [ok paths]} (ft/walk-relative-paths (.getPath root) nil ["**/*.clj"])]
          (is ok)
          (is (= (vec (distinct paths)) (vec paths))
              (str "the same file was walked more than once: " (pr-str paths))))
        (finally (delete-tree! root))))))

;; ---------------------------------------------------------------------------
;; ROUND SIX -- review findings 6 and 9: what the receipt SAYS about a cut and
;; about a candidate-only hit
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-029
(deftest an-na-implementation-row-never-calls-a-candidate-a-definition
  (testing "a string-only hit is a CANDIDATE lead, and the N/A row says so"
    (let [root (io/file (str (java.nio.file.Files/createTempDirectory
                               "feature-thread-ghost"
                               (into-array java.nio.file.attribute.FileAttribute []))))]
      (try
        (write-file! root "src/views.clj" "(ns views)\n(def menu {:onclick \"other()\"})\n")
        (write-file! root "src/routes.clj" "(ns routes)\n(def table [[\"/api/x\" {}]])\n")
        (write-file! root "src/handlers.clj" "(ns handlers)\n(defn handle-x [r] r)\n")
        (write-file! root "test/t.clj" "(ns t)\n(deftest x 1)\n")
        (write-file! root "js/commands.js"
                     (str "function unrelated() {\n"
                          "  const message = \"ghostOnly is not defined here\";\n"
                          "  return message;\n"
                          "}\n"))
        (let [{:keys [structured]}
              (call! {:subject "ghostOnly"
                      :config alias-conventions
                      :scope {:workspace_root (.getPath root)}})
              js (leg structured "js-function")
              impl (leg structured "implementation")]
          (is (= "CANDIDATE" (:status js))
              (str "the string mention is a lead: " (pr-str js)))
          (is (= "N/A" (:status impl)))
          (is (not (str/includes? (:reason impl) "is already a leg of this receipt"))
              (str "the N/A row calls a CANDIDATE a definition: "
                   (pr-str (:reason impl))))
          (is (str/includes? (:reason impl) "ghostOnly")
              "the N/A row names the seed")
          (is (str/includes? (:reason impl) "CANDIDATE")
              (str "the N/A row names the reason: " (pr-str (:reason impl)))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-THREAD-045
(deftest an-elision-forced-by-the-structured-cap-says-so
  (testing "the label names what bound"
    (is (= "public-budget" (ft/elision-reason :text-budget)))
    (is (= "structured-cap" (ft/elision-reason :structured-cap))))

  (testing "the remedy at the structured cap is not a budget the cap ignores"
    (let [advice (ft/elision-remedy :structured-cap)]
      (is (not (str/includes? advice "larger budget_bytes"))
          (str "the caller is told to raise a budget that cannot lift this"
               " cut: " (pr-str advice)))
      (is (str/includes? advice "mode=locations")
          (str "the remedy names what actually works: " (pr-str advice)))
      (is (str/includes? advice (str ft/trunk-public-byte-budget))
          "the remedy quotes the cap that bound"))
    (is (str/includes? (ft/elision-remedy :text-budget) "larger budget_bytes")
        "a text-budget cut IS undone by a larger budget"))

  (testing "no cut anywhere pairs a structured-cap reason with budget advice"
    (doseq [root [fixture-root after-root]
            budget [32768 24576 20000 11264]]
      (let [{:keys [structured]}
            (call! {:subject "dequoteFormatSelection"
                    :also ["/api/transform/format" "mechanical-format"]
                    :mirror "formatDraft"
                    :budget_bytes budget
                    :config smw-conventions
                    :scope {:workspace_root root}})]
        (doseq [c (:elided structured)]
          (is (not (and (str/includes? (str (:reason c)) "structured-cap")
                        (str/includes? (str (:refetch c)) "larger budget_bytes")))
              (str "an elision forced by the structured cap advises a budget the"
                   " hard cap forbids: " (pr-str c))))))))

;; ---------------------------------------------------------------------------
;; ROUND SIX -- review finding 3: a route leg is a route-table ENTRY
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-044
(deftest a-route-leg-is-a-parsed-route-entry-never-a-string-in-another-form
  (testing "the handler docstring that MENTIONS /api/save is not the route leg"
    (let [{:keys [structured]}
          (call! {:subject "saveDraft" :also ["/api/save"]
                  :config smw-conventions
                  :scope {:workspace_root fixture-root}})
          l (leg structured "route")]
      (is (= "src/writer/routes.clj" (:file l)))
      (is (not (and (= "FOUND" (:status l)) (= 392 (:from l))))
          (str "the route leg is the docstring of (defn handle-save …) reported"
               " as FOUND: " (pr-str (select-keys l [:status :from :to :evidence
                                                     :boundary]))))
      (is (or (= 2121 (:from l)) (= "CANDIDATE" (:status l)))
          (str "a route leg is the real route-table entry at L2121, or an"
               " honest CANDIDATE — never a string occurrence in another form: "
               (pr-str (select-keys l [:status :from :to :evidence]))))
      (when (= "FOUND" (:status l))
        (is (str/includes? (str (:boundary l)) "member of")
            "a FOUND route leg is narrowed to its own table entry"))))

  (testing "the docstring hit is still REPORTED, as a candidate lead, not dropped"
    (let [{:keys [structured]}
          (call! {:subject "saveDraft" :also ["/api/save"]
                  :config smw-conventions
                  :scope {:workspace_root fixture-root}})
          l (leg structured "route")
          leads (concat (:also l) (when (= "CANDIDATE" (:status l)) [l]))]
      (is (some #(= 392 (:from %)) leads)
          (str "the docstring occurrence is a lead the receipt still names: "
               (pr-str (mapv #(select-keys % [:from :to :evidence]) leads))))))

  (testing "the route leg of the NAMED case is unchanged: a real entry, FOUND"
    (let [{:keys [structured]} (thread! fixture-root)
          l (leg structured "route")]
      (is (= "FOUND" (:status l)))
      (is (= "src/writer/routes.clj" (:file l)))
      (is (= 2148 (:from l)) (str "the route entry line: " (pr-str l)))))

  (testing "a route literal inside a MAP is a route entry too"
    (let [root (io/file (str (java.nio.file.Files/createTempDirectory
                               "feature-thread-route-map"
                               (into-array java.nio.file.attribute.FileAttribute []))))]
      (try
        (write-file! root "src/views.clj" "(ns views)\n(def menu {:onclick \"go()\"})\n")
        (write-file! root "src/routes.clj"
                     (str "(ns routes)\n"
                          "(defn handle-go\n  \"POST /api/go — the decoy docstring.\"\n  [r] r)\n"
                          "(def table\n  [{:path \"/api/go\" :post #'handle-go}])\n"))
        (write-file! root "src/handlers.clj" "(ns handlers)\n(defn handle-go [r] r)\n")
        (write-file! root "test/t.clj" "(ns t)\n(deftest x (post \"/api/go\"))\n")
        (write-file! root "js/commands.js" "function go() { return 1; }\n")
        (let [{:keys [structured]}
              (call! {:subject "go" :also ["/api/go"]
                      :config alias-conventions
                      :scope {:workspace_root (.getPath root)}})
              l (leg structured "route")]
          (is (= "FOUND" (:status l)))
          (is (str/includes? (:body l) ":path \"/api/go\"")
              (str "the map entry is the route leg: "
                   (pr-str (select-keys l [:status :from :to :boundary]))))
          (is (not (str/includes? (:body l) "decoy docstring"))
              "the docstring that merely MENTIONS the route is not the leg"))
        (finally (delete-tree! root))))))

;; ---------------------------------------------------------------------------
;; ROUND SIX -- review finding 2: a conventions file may not reach outside the
;; workspace
;; ---------------------------------------------------------------------------

(def ^:private outside-canary "CANARY-OUTSIDE-THREAD6")

(defn- escaping-fixture!
  "A workspace root with a sibling `outside/` directory holding a canary file.
   Returns `[base root outside]`."
  []
  (let [base (io/file (str (java.nio.file.Files/createTempDirectory
                             "feature-thread-escape"
                             (into-array java.nio.file.attribute.FileAttribute []))))
        root (io/file base "repo")
        outside (io/file base "outside")]
    (write-file! outside "secret.clj"
                 (str "(ns secret)\n;; " outside-canary "\n"
                      "(defn formatDraft [x] x)\n"))
    (write-file! root "src/own.clj" "(ns own)\n")
    [base root outside]))

(defn- escaping-conventions
  [glob]
  {:repo-label "escape-fixture"
   :legs [{:id "menu-caller" :kind :use :globs ["src/*.clj"]}
          {:id "js-function" :kind :def :globs [glob]}
          {:id "route" :kind :route :globs ["src/*.clj"]}
          {:id "handler" :kind :handler :globs ["src/*.clj"]}
          {:id "tests" :kind :test :globs ["src/*.clj"]}]})

;; @spec MCP-OP-THREAD-043
(deftest a-convention-glob-may-not-name-a-path-outside-the-workspace
  (testing "a relative glob that climbs out of the root is a typed refusal"
    (let [[base root _] (escaping-fixture!)]
      (try
        (let [{:keys [structured text error?]}
              (call! {:subject "formatDraft"
                      :config (escaping-conventions "../outside/*.clj")
                      :scope {:workspace_root (.getPath root)}})]
          (is error? "an escaping glob is a refusal, not a receipt")
          (is (= "feature-thread-conventions-escaping-glob"
                 (:error_type structured)))
          (is (= "legs[1].globs" (:field structured))
              "the refusal names the field it refused")
          (is (str/includes? (:error structured) "../outside/*.clj")
              "the refusal names the glob AS SPELLED")
          (is (nil? (:legs structured))
              "no leg was resolved: the refusal precedes the walk")
          (is (not (str/includes? text outside-canary))
              "nothing outside the root was read")
          (is (not (str/includes? (json/generate-string structured)
                                  outside-canary))
              "nothing outside the root was read"))
        (finally (delete-tree! base)))))

  (testing "an absolute glob is a typed refusal and its target is never resolved"
    (let [[base root outside] (escaping-fixture!)]
      (try
        (let [{:keys [structured text error?]}
              (call! {:subject "formatDraft"
                      :config (escaping-conventions
                                (str (.getPath outside) "/*.clj"))
                      :scope {:workspace_root (.getPath root)}})]
          (is error?)
          (is (= "feature-thread-conventions-escaping-glob"
                 (:error_type structured)))
          (is (nil? (:legs structured)))
          (is (not (str/includes? text outside-canary)))
          (is (not (str/includes? (json/generate-string structured)
                                  outside-canary))))
        (finally (delete-tree! base)))))

  (testing "the same refusal comes from the conventions FILE, not only inline"
    (let [[base root _] (escaping-fixture!)]
      (try
        (write-file! root ".clj-surgeon/feature-thread.edn"
                     (pr-str (escaping-conventions "../outside/*.clj")))
        (let [{:keys [structured error?]}
              (call! {:subject "formatDraft"
                      :scope {:workspace_root (.getPath root)}})]
          (is error?)
          (is (= "feature-thread-conventions-escaping-glob"
                 (:error_type structured)))
          (is (str/includes? (:error structured) "../outside/*.clj")))
        (finally (delete-tree! base)))))

  (testing "a `~` glob is refused with the same type"
    (let [[base root _] (escaping-fixture!)]
      (try
        (let [{:keys [structured error?]}
              (call! {:subject "formatDraft"
                      :config (escaping-conventions "~/outside/*.clj")
                      :scope {:workspace_root (.getPath root)}})]
          (is error?)
          (is (= "feature-thread-conventions-escaping-glob"
                 (:error_type structured))))
        (finally (delete-tree! base)))))

  (testing "a scope path that climbs out of the root is refused before any read"
    (let [[base root _] (escaping-fixture!)]
      (try
        (let [{:keys [structured error?]}
              (call! {:subject "formatDraft"
                      :config (escaping-conventions "js/*.js")
                      :scope {:workspace_root (.getPath root)
                              :paths ["../outside"]}})]
          (is error?)
          (is (= "feature-thread-scope-path-escapes-workspace"
                 (:error_type structured)))
          (is (str/includes? (:error structured) "../outside")))
        (finally (delete-tree! base)))))

  (testing "a SYMLINKED directory cannot be refused by shape, so nothing it holds is read or published"
    (let [[base root outside] (escaping-fixture!)]
      (try
        (symlink! (io/file root "hop") outside)
        (let [{:keys [structured text]}
              (call! {:subject "formatDraft"
                      :config (escaping-conventions "hop/*.clj")
                      :scope {:workspace_root (.getPath root)}})
              json (json/generate-string structured)]
          (is (not (str/includes? text outside-canary))
              "a symlinked directory's contents are never read")
          (is (not (str/includes? json outside-canary)))
          (is (not (str/includes? json "\"hop/secret.clj\""))
              "a path reached through a symlink out of the root is never published"))
        (finally (delete-tree! base))))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR -- 3.3 (a make recipe is not a shell command) and 3.4 (dup unreadable)
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-019
(deftest a-verify-row-hands-back-a-runnable-shell-command
  (testing "the Makefile recipe prefix is not part of the command"
    (let [{:keys [structured]} (thread! fixture-root)
          rows (get-in structured [:rules :verify])]
      (is (seq rows) "the fixture Makefile has verify rows")
      (is (not-any? #(re-find #"^[@+-]" (str (:command %))) rows)
          (str "a Makefile recipe prefix was handed to the caller as a shell"
               " command: " (pr-str (map :command rows))))
      (is (some #(= "@" (:make_prefix %)) rows)
          "the prefix that was stripped must still be named, not erased")
      (is (some #(str/starts-with? (str (:command %)) "node --test") rows)
          "the `test-js` recipe should be runnable as printed"))))

;; @spec MCP-OP-THREAD-013
(deftest an-unreadable-file-is-listed-once-per-leg
  (testing "several searches over the same unreadable file make ONE entry"
    (let [scratch (scratch-copy! fixture-root "feature-thread-dupunread")]
      (try
        (let [h (io/file scratch "src/writer/handlers/transform.clj")]
          (is (.setReadable h false false)
              "this witness needs a filesystem that honours a read bit")
          (let [{:keys [structured]} (thread! (.getPath scratch))
                l (leg structured "handler")
                us (:unreadable l)]
            (is (= "ABSENT" (:status l)))
            (is (seq us))
            (is (= (vec (distinct us)) (vec us))
                (str "the same unreadable file was listed more than once: "
                     (pr-str us)))))
        (finally
          (.setReadable (io/file scratch "src/writer/handlers/transform.clj") true false)
          (delete-tree! scratch))))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR -- an insertion anchor is a claim only a FOUND leg may make
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-034
(deftest a-candidate-leg-carries-no-insertion-anchor
  (testing "a lead names where to READ, never where to WRITE"
    (let [scratch (scratch-copy! fixture-root "feature-thread-candanchor")]
      (try
        (spit (io/file scratch "src/writer/handlers/transform.clj")
              "\n\n(comment\n  (widgetize {:draft \"x\"}))\n"
              :append true)
        (let [{:keys [text structured]}
              (call! {:subject "widgetize"
                      :config smw-conventions
                      :scope {:workspace_root (.getPath scratch)}})
              h (leg structured "handler")]
          (is (= "CANDIDATE" (:status h)))
          (is (nil? (:anchor h))
              (str "a CANDIDATE leg offered an insertion anchor: "
                   (pr-str (:anchor h))))
          (is (not-any? #(and (str/starts-with? % "leg handler ")
                              (str/includes? % "anchor="))
                        (str/split-lines text))
              (str "the receipt printed an insertion point for a leg it does"
                   " not vouch for: "
                   (pr-str (filter #(str/starts-with? % "leg handler ")
                                   (str/split-lines text))))))
        (finally (delete-tree! scratch)))))

  (testing "a FOUND leg still carries one"
    (let [{:keys [structured]} (thread! fixture-root)]
      (doseq [l (:legs structured)
              :when (= "FOUND" (:status l))]
        (is (string? (:anchor l))
            (str "FOUND leg " (:id l) " lost its anchor"))))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR -- 3.1: the clock is not part of the superset haystack
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-026
(deftest the-clock-cannot-swallow-a-structured-leaf
  (testing "a leaf whose digits appear inside elapsed_ms is still reported"
    (let [{:keys [structured]} (thread! fixture-root)
          ;; Which leaves does the DESIGNED text omit? Ask the receipt itself
          ;; with the clock unstamped, so the witness cannot go stale when the
          ;; fixture's numbers move.
          only (->> (str/split-lines (ft/render-receipt (assoc structured
                                                               :elapsed_ms nil)))
                    (filter #(str/includes? % "structured-only"))
                    last)
          [_ path digits] (re-find #"·\s([A-Za-z0-9_.]+)=(\d{3,})[\s·]" (str only " "))]
      (is (some? digits)
          (str "the witness needs a numeric structured-only leaf; line was "
               (pr-str only)))
      (let [;; an operation clock whose DIGITS contain that leaf's value
            clocked (assoc structured
                           :elapsed_ms (Double/parseDouble (str "229.5" digits)))
            text (ft/render-receipt clocked)]
        (is (str/includes? text (str "elapsed_ms=229.5" digits))
            "the witness needs the clock it constructed")
        (is (str/includes? text (str path "=" digits))
            (str "the clock's digits made the structured leaf `" path "="
                 digits "` look present in the text, so the completion line"
                 " dropped it")))))

  (testing "twenty-five identical requests: text_bytes equals the delivered text"
    (let [deltas (vec (for [_ (range 25)]
                        (let [{:keys [text structured]} (thread! fixture-root)]
                          (- (:text_bytes structured)
                             (count (.getBytes ^String text "UTF-8"))))))]
      (is (= #{0} (set deltas))
          (str "text_bytes disagreed with the delivered text on "
               (count (remove zero? deltas)) " of 25 runs; deltas seen: "
               (pr-str (frequencies deltas)))))))

;; @spec MCP-OP-THREAD-026
(deftest a-budget-refusal-names-would-be-text-bytes
  (testing "the count on a refusal describes the receipt that COULD NOT be sent"
    (let [{:keys [error? structured text]} (thread! fixture-root {:budget_bytes 1})]
      (is (true? error?))
      (is (= "feature-thread-budget-exceeded" (:error_type structured)))
      (is (nil? (:text_bytes structured))
          (str "`text_bytes` on a refusal described a text nobody was"
               " delivered: declared " (:text_bytes structured)
               ", delivered " (count (.getBytes ^String text "UTF-8"))))
      (is (number? (:would_be_text_bytes structured))
          "the refusal must still say how big the receipt would have been")
      (is (str/includes? text "would_be_text_bytes=")
          "and the facts line must use the same name"))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR -- 3.2: next_call must be executable for the NORMAL edit
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-025
(deftest next-call-hands-back-a-selectable-per-leg-pre-image
  (let [{:keys [structured text]} (thread! fixture-root)
        n (:next_call structured)
        by-leg (:by_leg n)]
    (testing "admit_clojure_patch requires EXACTLY the files the patch touches"
      (is (map? by-leg)
          (str "next_call offers no per-leg selection, so the caller whose"
               " patch touches the handler and its test must hand back all six"
               " digests and be refused: " (pr-str (keys n)))))

    (testing "every leg that has a file is selectable by leg id"
      (doseq [l (:legs structured)
              :when (and (:file l) (not= "N/A" (:status l)))]
        (is (contains? by-leg (keyword (:id l)))
            (str "leg " (:id l) " is not selectable in by_leg")))
      (is (= (set (keys (:expect_pre_sha256 n)))
             (set (mapcat (comp keys val) by-leg)))
          "the union of the per-leg maps is the whole-file map"))

    (testing "the wording tells the caller to pass a SUBSET"
      (is (str/includes? (str (:note n)) "subset")
          (str "note was " (pr-str (:note n))))
      (is (str/includes? (get-in structured [:rules :assert]) "subset")
          "the assert line still tells the caller to pass the whole map"))

    (testing "and the text reader can see the same selection"
      (is (str/includes? text "by_leg")))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR / round-three spec 4 -- the classic-script statement
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-035
(deftest a-script-leg-states-whether-the-subject-is-exported
  (testing "no export, no module.exports, no window.X = : say so"
    (let [{:keys [text structured]} (thread! fixture-root)
          js (leg structured "js-function")]
      (is (= "FOUND" (:status js)))
      (is (= "none (classic script; functions are globals)" (:export js))
          (str "the js leg said " (pr-str (:export js))
               "; a reader must not have to search for a registration site"))
      (is (str/includes? text "export=none (classic script; functions are globals)"))))

  (testing "a registration site, when there is one, is NAMED with its line"
    (let [scratch (scratch-copy! fixture-root "feature-thread-export")]
      (try
        (spit (io/file scratch "resources/public/js/editor-commands.js")
              "\nwindow.formatDraft = formatDraft;\n" :append true)
        (let [{:keys [structured]} (thread! (.getPath scratch))
              js (leg structured "js-function")]
          (is (str/includes? (str (:export js)) "window.formatDraft = formatDraft;")
              (str "the registration site was not named: " (pr-str (:export js))))
          (is (re-find #"resources/public/js/editor-commands\.js:L\d+" (str (:export js)))
              "and it must carry the file and line the reader would open"))
        (finally (delete-tree! scratch)))))

  (testing "a Clojure leg makes no such claim"
    (let [{:keys [structured]} (thread! fixture-root)]
      (is (nil? (:export (leg structured "handler")))
          "`export` is a JavaScript question and belongs only to script legs"))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR / addendum -- after_context: the lines the anchor points AT
;; ---------------------------------------------------------------------------

(defn- sed-lines
  "`sed -n '<from>,<to>p' <file>` computed in process, so the witness compares
  the receipt against the file rather than against the code that made it."
  [root file from to]
  (let [lines (str/split (slurp (io/file root file)) #"\n" -1)]
    (vec (take (inc (- to from)) (drop (dec from) lines)))))

;; @spec MCP-OP-THREAD-036
(deftest an-anchor-carries-the-source-lines-it-points-at
  (let [{:keys [text structured]} (thread! fixture-root)]
    (testing "every FOUND leg with an anchor carries after_context"
      (doseq [l (:legs structured)
              :when (= "FOUND" (:status l))]
        (is (seq (:after_context l))
            (str "leg " (:id l) " names an insertion point and not one line"
                 " of what is there"))
        (is (<= 3 (count (:after_context l)) 6)
            (str "leg " (:id l) " carried " (count (:after_context l))
                 " lines; the addendum asks for 3-6"))))

    (testing "and those lines are VERBATIM the file's own"
      (doseq [l (:legs structured)
              :when (and (= "FOUND" (:status l)) (seq (:after_context l)))]
        (let [from (inc (:to l))
              to (+ (:to l) (count (:after_context l)))]
          (is (= (sed-lines fixture-root (:file l) from to)
                 (vec (:after_context l)))
              (str "leg " (:id l) ": after_context is not sed -n '"
                   from "," to "p' " (:file l))))))

    (testing "the range is named so the reader can check it"
      (let [h (leg structured "handler")]
        (is (= (inc (:to h)) (:after_context_from h)))
        (is (= (+ (:to h) (count (:after_context h))) (:after_context_to h)))))

    (testing "and the text face carries them too"
      (is (str/includes? text "AFTER<<")))

    (testing "a CANDIDATE has no anchor and therefore no after_context"
      (let [scratch (scratch-copy! fixture-root "feature-thread-aftercand")]
        (try
          (spit (io/file scratch "src/writer/handlers/transform.clj")
                "\n\n(comment\n  (widgetize {:draft \"x\"}))\n" :append true)
          (let [{:keys [structured]}
                (call! {:subject "widgetize"
                        :config smw-conventions
                        :scope {:workspace_root (.getPath scratch)}})]
            (is (nil? (:after_context (leg structured "handler")))))
          (finally (delete-tree! scratch)))))

    (testing "and it is elided with the body under budget pressure"
      (let [{:keys [structured]} (thread! fixture-root {:budget_bytes 11264})]
        (doseq [l (:legs structured)
                :when (:elided_reason l)]
          (is (nil? (:after_context l))
              (str "leg " (:id l) " kept its after_context after its body was"
                   " elided for budget")))))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR / round-three spec 2 -- the request contract
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-037
(deftest the-rules-carry-the-request-contract-of-the-two-sides
  (testing "what the handler reads from the body and what the JS posts"
    (let [{:keys [text structured]} (thread! fixture-root)
          rc (get-in structured [:rules :request_contract])]
      (is (some? rc) "rules carries no request_contract row")
      (is (= "/api/transform/format" (:route rc)))
      (is (= ["sync"] (vec (:handler_reads rc)))
          (str "handler_reads was " (pr-str (:handler_reads rc))
               "; the handler destructures {:keys [sync]} off the parsed body"))
      (is (= ["sync"] (vec (:js_posts rc)))
          (str "js_posts was " (pr-str (:js_posts rc))
               "; the JS calls postJSON('/api/transform/format', {sync})"))
      (is (true? (:agree? rc)))
      (is (str/includes? text "request_contract"))))

  (testing "a disagreement is reported, and says WHICH side has the extra key"
    (let [scratch (scratch-copy! fixture-root "feature-thread-contract")]
      (try
        (let [f (io/file scratch "resources/public/js/editor-commands.js")]
          (spit f (str/replace (slurp f)
                               "postJSON('/api/transform/format', {sync})"
                               "postJSON('/api/transform/format', {sync, selection})")))
        (let [{:keys [structured]} (thread! (.getPath scratch))
              rc (get-in structured [:rules :request_contract])]
          (is (= ["sync"] (vec (:handler_reads rc))))
          (is (= ["selection" "sync"] (sort (:js_posts rc))))
          (is (false? (:agree? rc)))
          (is (= ["selection"] (vec (:only_in_js rc)))
              "the key the browser sends and the handler never reads")
          (is (empty? (:only_in_handler rc))))
        (finally (delete-tree! scratch)))))

  (testing "a destructure of something that is NOT the request body is not a read"
    (let [{:keys [structured]} (thread! fixture-root)
          rc (get-in structured [:rules :request_contract])]
      (is (not (contains? (set (:handler_reads rc)) "reason"))
          (str "`(let [{:keys [reason] :as conflict} (ex-data e)]` is not a"
               " read of the request body; handler_reads was "
               (pr-str (:handler_reads rc)))))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR / round-three spec 3 -- negative evidence, and the probe param
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-038
(deftest the-receipt-carries-negative-evidence
  (testing "a probed identifier that is not in the tree is REPORTED absent"
    (let [{:keys [text structured]} (thread! fixture-root {:probe ["dequote" "widgetize"]})
          absent (:absent structured)]
      (is (= #{"dequote" "widgetize"} (set (map :identifier absent)))
          (str "absent was " (pr-str absent)
               "; a reader must never have to run `rg -i dequote` to learn"
               " there is nothing"))
      (is (every? #(seq (str (:searched %))) absent)
          "an absence with no search behind it is an opinion")
      (is (every? #(str/includes? (str (:searched %)) "rg -n") absent))
      (is (str/includes? text "absent dequote"))))

  (testing "a seed that IS there is not reported absent"
    (let [{:keys [structured]} (thread! fixture-root)]
      (is (not-any? #{"formatDraft" "mechanical-format"}
                    (map :identifier (:absent structured)))
          (str "a located seed was called absent: "
               (pr-str (map :identifier (:absent structured)))))))

  (testing "an identifier that exists ONLY in a comment is absent"
    (let [scratch (scratch-copy! fixture-root "feature-thread-absentcomment")]
      (try
        (spit (io/file scratch "src/writer/handlers/transform.clj")
              "\n;; TODO: ghostThing should be written one day\n" :append true)
        (let [{:keys [structured]} (thread! (.getPath scratch) {:probe ["ghostThing"]})]
          (is (contains? (set (map :identifier (:absent structured))) "ghostThing")
              "a mention in a `;;` comment is not an occurrence"))
        (finally (delete-tree! scratch)))))

  (testing "probe is validated like any other field"
    (let [{:keys [structured]} (thread! fixture-root {:probe "dequote"})]
      (is (false? (:ok structured)))
      (is (= "feature-thread-invalid-probe" (:error_type structured))))
    (let [{:keys [structured]}
          (thread! fixture-root {:probe (vec (repeat 40 "x"))})]
      (is (false? (:ok structured)))
      (is (= "feature-thread-probe-too-many" (:error_type structured))))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR / round-three spec 1 -- co-menu-item peers
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-039
(deftest the-use-leg-names-its-co-menu-item-peers
  (let [{:keys [text structured]} (thread! fixture-root {:budget_bytes 32768})
        menu (leg structured "menu-caller")
        peers (:peers menu)
        by-id (into {} (map (juxt :identifier identity) peers))]
    (testing "every other command bound in the same menu form is a peer"
      (is (= #{"openTransformFromSelection" "expound" "bulletize"}
             (set (map :identifier peers)))
          (str "peers were " (pr-str (map :identifier peers))))
      (is (not-any? #{"formatDraft"} (map :identifier peers))
          "the subject is not its own peer")
      (is (not-any? #{"saveDraft" "navigateBookNodeHistory"} (map :identifier peers))
          "a command in a DIFFERENT menu form is not a peer of this one"))

    (testing "a peer whose definition exists is located exactly"
      (let [p (by-id "openTransformFromSelection")]
        (is (= "FOUND" (:status p)))
        (is (= "resources/public/js/editor-commands.js" (:file p)))
        (is (= 332 (:from p)))
        (is (string? (:sha256 p)))
        (is (string? (:anchor p)))
        (is (= "co-menu-item" (:evidence p)))
        (is (str/includes? (str (:body p)) "function openTransformFromSelection"))
        (is (str/starts-with? (str (:boundary p)) "brace-window(lexed,closed"))))

    (testing "a peer with NO definition is named absent, with the search run"
      (doseq [id ["expound" "bulletize"]]
        (let [p (by-id id)]
          (is (= "ABSENT" (:status p)) (str id " was " (:status p)))
          (is (seq (str (:searched p)))
              (str id " is called absent with no search behind it"))
          (is (nil? (:body p))))))

    (testing "and the text face carries them"
      (is (str/includes? text "peer openTransformFromSelection"))
      (is (str/includes? text "peer expound")))

    (testing "the default budget still holds the whole thread WITH its peers"
      (let [{:keys [structured]} (thread! fixture-root)]
        (is (empty? (:elided structured))
            (str "the default budget elided "
                 (pr-str (map :leg (:elided structured)))))
        (is (= 3 (count (:peers (leg structured "menu-caller")))))))

    (testing "and under pressure peer bodies go FIRST, after the sibling only"
      (let [{:keys [structured]} (thread! fixture-root {:budget_bytes 24000})
            cut (map :leg (:elided structured))
            menu (leg structured "menu-caller")]
        (is (= ["sibling" "peers"] (take 2 cut))
            (str "the stated order was not followed; elided was " (pr-str cut)))
        (is (every? #(nil? (:body %)) (:peers menu))
            "a peer body survived its own elision step")
        (is (every? #(and (:from %) (:sha256 %) (:refetch %))
                    (filter ft/located? (:peers menu)))
            "a cut peer must keep its range, its hash and its refetch")
        (is (:body menu)
            "the menu leg's OWN body is not cut by the peers step")))))

;; ---------------------------------------------------------------------------
;; ROUND FOUR / round-three spec 5 -- the two defects the real-repo recall found
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-THREAD-040
(deftest the-implementation-leg-never-takes-a-definition-from-a-test-file
  (testing "the automatic leg does not inherit the TEST leg's globs"
    (let [globs (:globs (ft/implementation-leg smw-conventions))]
      (is (seq globs))
      (is (not-any? #(str/starts-with? % "test/") globs)
          (str "`implementation` means the definition the seed names, and a"
               " definition inside a test file is a double, not the"
               " implementation; globs were " (pr-str globs)))
      (is (some #{"resources/public/js/*.js"} globs)
          "it still inherits the non-test script globs")
      (is (some #{"src/**/*.clj"} globs))))

  ;; The real-repo shape, exactly: the seed's own definition is already the
  ;; js-function leg, so the implementation leg EXCLUDES that range and takes
  ;; the next definition-shaped hit -- which, with the test globs inherited,
  ;; is a stub inside a test file. On social-media-writer @2df99c98 that made
  ;; `formatDraft` report `implementation FOUND
  ;; test/js/editor_conflict_response_test.js` and read `4 of 6`.
  (testing "a stub in a test file is not the implementation of a located leg"
    (let [scratch (scratch-copy! fixture-root "feature-thread-testimpl")]
      (try
        (write-file! scratch "test/js/ghost_def_test.js"
                     "function formatDraft() {\n  return 'stub';\n}\n")
        (let [;; the subject ALONE, as the real replay called it: with
              ;; `mechanical-format` in `also` the leg finds that Clojure
              ;; definition first and the defect never shows.
              {:keys [structured]}
              (call! {:subject "formatDraft"
                      :config smw-conventions
                      :scope {:workspace_root (.getPath scratch)}})
              impl (leg structured "implementation")]
          (is (not (str/starts-with? (str (:file impl)) "test/"))
              (str "the implementation leg was located inside a test file: "
                   (:file impl) " L" (:from impl))))
        (finally (delete-tree! scratch))))))

;; @spec MCP-OP-THREAD-041
(deftest an-absent-leg-says-whether-it-could-search-at-all
  (testing "a leg with no seed of its kind names the missing INPUT and a remedy"
    (let [{:keys [text structured]}
          (call! {:subject "formatDraft"
                  :config smw-conventions
                  :scope {:workspace_root fixture-root}})
          route (leg structured "route")]
      (is (= "ABSENT" (:status route)))
      (is (= "no-seed-of-this-leg-kind" (:absent_cause route))
          (str "the route leg said " (pr-str (:absent_cause route))
               "; `no seed of the kind this leg needs` is jargon with no"
               " remedy, and it is indistinguishable from `I searched and"
               " found nothing`"))
      (is (str/includes? (str (:remedy route)) "also")
          "a caller must be told how to supply the seed")
      (is (str/includes? text "no-seed-of-this-leg-kind"))))

  (testing "a leg that DID search and found nothing says that instead"
    (let [scratch (scratch-copy! fixture-root "feature-thread-cause")]
      (try
        (delete-tree! (io/file scratch "resources/public/js"))
        (let [{:keys [structured]} (thread! (.getPath scratch))
              js (leg structured "js-function")]
          (is (= "ABSENT" (:status js)))
          (is (= "searched-and-absent" (:absent_cause js)))
          (is (seq (:searches js))))
        (finally (delete-tree! scratch)))))

  (testing "either way the leg is COUNTED"
    (let [{:keys [structured]}
          (call! {:subject "formatDraft"
                  :config smw-conventions
                  :scope {:workspace_root fixture-root}})]
      (is (contains? (set (:legs_missing structured)) "route")
          (str "an unnamed route is counted as missing, deliberately: the verb"
               " cannot tell an unnamed route from an absent one, and the safe"
               " direction is INCOMPLETE")))))

;; @spec MCP-OP-THREAD-042
(deftest a-route-entry-may-name-its-handler-var-unqualified
  (testing "`#'handle-x` joins to the handler exactly like `#'ns/handle-x`"
    (let [scratch (scratch-copy! fixture-root "feature-thread-bareVar")]
      (try
        (let [f (io/file scratch "src/writer/routes.clj")]
          (spit f (str/replace (slurp f)
                               "#'transform/handle-format"
                               "#'handle-format")))
        (let [{:keys [structured]} (thread! (.getPath scratch))
              h (leg structured "handler")]
          (is (= "FOUND" (:status h))
              (str "the route names `#'handle-format` with no namespace and the"
                   " handler leg came back " (:status h) "; on"
                   " social-media-writer @2df99c98 that is how `/api/save` is"
                   " written and it cost saveDraft its handler leg"))
          (is (= "src/writer/handlers/transform.clj" (:file h)))
          (is (= "handler-join" (:evidence h)))
          (is (= "COMPLETE (6 of 6)" (:status structured))))
        (finally (delete-tree! scratch)))))

  (testing "and the qualified form still works"
    (let [{:keys [structured]} (thread! fixture-root)]
      (is (= "handler-join" (:evidence (leg structured "handler"))))
      (is (= "transform/handle-format" (:route_handler structured))))))

;; @spec MCP-OP-THREAD-012
(deftest every-body-is-byte-for-byte-identical-in-both-faces
  (testing "the text face never re-formats what the structured face carries"
    (let [{:keys [text structured]} (thread! fixture-root {:budget_bytes 32768})
          bodies (concat (keep :body (:legs structured))
                         (mapcat #(keep :body (:co_primaries %)) (:legs structured))
                         (mapcat #(keep :body (:peers %)) (:legs structured)))
          ctx (mapcat :after_context (:legs structured))]
      (is (<= 6 (count bodies)) "the witness needs bodies to compare")
      (is (<= 12 (count ctx)) "and anchor context to compare")
      (doseq [b bodies]
        (is (str/includes? text b)
            (str "a structured body is not VERBATIM in the text face; a caller"
                 " reading the text and a caller reading the structure would"
                 " write different patches. First 80 chars: "
                 (pr-str (subs b 0 (min 80 (count b)))))))
      (doseq [l ctx]
        (is (str/includes? text l)
            (str "an after_context line is not verbatim in the text face: "
                 (pr-str l)))))))
