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
(deftest t1-smw-thread-returns-five-legs-with-bodies
  (testing "Edit -> Dequote/Format: five owners, two languages, one call"
    (let [{:keys [text error? structured]} (thread! fixture-root)]
      (is (false? error?))
      (is (true? (:ok structured)))
      (is (= "COMPLETE (5 of 5)" (:status structured)))
      (is (true? (:complete structured)))
      (is (= 5 (:legs_found structured)))
      (is (= [] (:legs_missing structured)))

      (testing "every ground-truth owner is named, at the leg the truth assigns it"
        (doseq [[id file] five-owners]
          (let [l (leg structured id)]
            (is (some? l) (str "no leg " id))
            (is (= "FOUND" (:status l)) (str id " is not FOUND"))
            (is (= file (:file l))
                (str id " named " (:file l) ", ground truth is " file)))))

      (testing "the JavaScript test witness is carried as a secondary member"
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
            (is (= "INCOMPLETE (4 of 5)" (:status structured)))
            (is (false? (:complete structured)))
            (is (= ["js-function"] (:legs_missing structured)))
            (is (seq (:searches l)) "an absent leg must quote its searches")
            (is (some #(str/includes? % "editor-commands.js") (map :file (:unreadable l)))
                "the leg must name the file it could not read")
            (is (= #{"unreadable"} (set (map :reason (:unreadable l))))
                "the reason must be typed, not blank")
            (is (str/includes? text "INCOMPLETE (4 of 5)"))
            (is (str/includes? text "unreadable"))
            (is (not (str/includes? text "COMPLETE (5 of 5)")))))
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
        (is (= 5 (count (:legs structured))) "five legs are always rendered")
        (is (= "ABSENT" (:status l)))
        (is (seq (:searches l)))
        (is (= "INCOMPLETE (4 of 5)" (:status structured))))
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
      (is (= "COMPLETE (5 of 5)" (:status structured)))
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
      (is (str/includes? (:assert rules) "stale pre-image")))))

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
      (is (= "COMPLETE (5 of 5)" (:status structured)))))

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

  (testing "a convention set that is not five leg roles is refused"
    (let [{:keys [structured]}
          (thread! fixture-root {:config (update smw-conventions :legs pop)})]
      (is (= "feature-thread-conventions-invalid" (:error_type structured)))
      (is (str/includes? (:error structured) "exactly five")))))

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
      (testing "and the sibling never changes the five-leg status"
        (is (= 5 (:legs_declared structured)))
        (is (= 5 (count (:legs structured)))))))

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
    (let [{:keys [structured text]} (thread! fixture-root {:budget_bytes 8000})]
      (is (true? (:ok structured)))
      (is (every? #(nil? (:body %)) (:legs structured)))
      (is (= 7 (count (:elided structured)))
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
          bodies (count (filter :body (:legs structured)))
          missing-after-receipt (- 5 bodies)
          verb-rounds (+ 1 (if (pos? missing-after-receipt) 1 0))]
      (is (= 5 bodies)
          "at the default budget every leg arrives with its body in round one")
      (is (= 1 verb-rounds)
          (str "rounds to a complete thread: " verb-rounds
               " (one call; nothing left to read afterwards). Human baseline: 6."))
      (is (= 0 missing-after-receipt)
          "nothing the caller must still read after the receipt"))))
