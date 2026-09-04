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
          (is (= "ABSENT" (:status l)))
          (is (= "alias-only" (:evidence l)))
          (is (some #(str/includes? % "runDraftFormatter") (:searches l))
              "the search that followed the alias must be quoted")
          (is (= "INCOMPLETE (4 of 5)" (:status structured)))
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
    (let [{:keys [structured text]} (thread! fixture-root {:budget_bytes 10240})]
      (is (true? (:ok structured)))
      (is (every? #(nil? (:body %)) (:legs structured)))
      (is (every? #(nil? (:body %))
                  (mapcat :co_primaries (:legs structured))))
      (is (= #{"sibling" "governance-template" "next-call" "menu-caller"
               "route" "tests(js)" "tests" "implementation" "js-function"
               "handler"}
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
    (let [{:keys [structured]} (thread! fixture-root {:budget_bytes 10240})]
      (is (<= (:text_bytes structured) 10240))
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
                     :command "@node --test test/js/browser_runtime_classic_script_test.js"
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
    (is (= 24576 ft/default-budget-bytes))
    (let [{:keys [structured]} (thread! fixture-root)]
      (is (empty? (:elided structured))
          (str "the default budget elided " (pr-str (map :leg (:elided structured)))
               "; the whole point of raising it was that it must not"))))

  (testing "the stated order elides context first and the edit sites last"
    (is (= [:sibling :governance-template :secondary-tests :next-call :menu
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

  (testing "at 10240 every leg still names its range, its hash and its anchor"
    (let [{:keys [structured text]} (thread! fixture-root {:budget_bytes 10240})]
      (is (true? (:ok structured)))
      (is (<= (:text_bytes structured) 10240))
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
      (let [{:keys [structured]} (thread! fixture-root {:budget_bytes 10240})]
        (is (nil? (:next_call structured)))
        (is (some #(= "next-call" (:leg %)) (:elided structured)))))))

;; @spec MCP-OP-THREAD-026
(deftest the-receipt-byte-counts-describe-the-delivered-text
  (testing "text_bytes is the size of the text block the caller receives"
    (doseq [budget [nil 32768 12000]]
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
      (is (integer? (:text_bytes structured))))))

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
          (is (not (str/includes? text "COMPLETE"))))
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
      (is (false? (commented-out? clj 1 false))
          "a live definition is not commented out")
      (is (true? (commented-out? clj 3 false))
          "line 3 is inside a (comment …) form")
      (is (true? (commented-out? clj 4 false))
          "line 4 is discarded by #_")
      (is (true? (commented-out? js 2 true))
          "line 2 is inside a /* … */ block")
      (is (false? (commented-out? js 4 true))
          "line 4 is live code after the block closed"))))
