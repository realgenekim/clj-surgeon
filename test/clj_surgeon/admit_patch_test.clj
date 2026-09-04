(ns clj-surgeon.admit-patch-test
  "Witness tests for the admit_clojure_patch form-identity gate.

  Fixtures are literal source pairs and real unified diffs generated from
  those pairs with `diff -u`, so every hunk header in this file is arithmetic
  a patch producer actually emitted rather than a hand-counted guess."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.form-identity :as form-identity]
   [clj-surgeon.mcp-admit-tool :as admit]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-process :as process]
   [clj-surgeon.mcp-server :as server]
   [clj-surgeon.mcp-tool :as tool]
   [clj-surgeon.mcp-write-refusal :as write-refusal]
   [clj-surgeon.patch-apply :as patch-apply]
   [clj-surgeon.workspace-lock :as workspace-lock]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

;; ---------------------------------------------------------------------------
;; Fixtures
;; ---------------------------------------------------------------------------

(def core-source
  (str "(ns app.core\n"
       "  (:require\n"
       "   [clojure.string :as str]))\n"
       "\n"
       "(defn handle-tick\n"
       "  [state]\n"
       "  (update state :ticks inc))\n"
       "\n"
       "(defn label\n"
       "  [state]\n"
       "  ;; upper-case for the banner\n"
       "  (str/upper-case (:name state)))\n"))

(def util-source
  (str "(ns app.util)\n"
       "\n"
       "(defn clamp\n"
       "  [value low high]\n"
       "  (max low (min high value)))\n"))

(def embed-source
  (str "(ns app.embed)\n"
       "\n"
       "(def bootstrap-script\n"
       "  \"function bootstrap(root) {\n"
       "     const state = { ticks: 0, name: 'unset', ready: false };\n"
       "     const timer = setInterval(function () {\n"
       "       state.ticks = state.ticks + 1;\n"
       "       if (state.ticks > 10) { clearInterval(timer); state.ready = true; }\n"
       "     }, 100);\n"
       "     return state;\n"
       "   }\")\n"
       "\n"
       "(defn script\n"
       "  []\n"
       "  bootstrap-script)\n"))

(def base-sources
  {"src/app/core.clj" core-source
   "src/app/util.clj" util-source})

;; A clean two-file patch: real code changes, nothing else moved.
(def clean-multi-file-patch
  (str "--- a/src/app/core.clj\n"
       "+++ b/src/app/core.clj\n"
       "@@ -4,7 +4,7 @@\n"
       " \n"
       " (defn handle-tick\n"
       "   [state]\n"
       "-  (update state :ticks inc))\n"
       "+  (update state :ticks (fnil inc 0)))\n"
       " \n"
       " (defn label\n"
       "   [state]\n"
       "--- a/src/app/util.clj\n"
       "+++ b/src/app/util.clj\n"
       "@@ -2,4 +2,4 @@\n"
       " \n"
       " (defn clamp\n"
       "   [value low high]\n"
       "-  (max low (min high value)))\n"
       "+  (long (max low (min high value))))\n"))

;; The shadowed-declaration class: a second top-level handle-tick.
(def duplicate-definition-patch
  (str "--- a/src/app/core.clj\n"
       "+++ b/src/app/core.clj\n"
       "@@ -10,3 +10,7 @@\n"
       "   [state]\n"
       "   ;; upper-case for the banner\n"
       "   (str/upper-case (:name state)))\n"
       "+\n"
       "+(defn handle-tick\n"
       "+  [state]\n"
       "+  (update state :ticks (fnil inc 0)))\n"))

;; One real edit plus a comment reformat inside an owner whose code is intact.
(def comment-reformat-patch
  (str "--- a/src/app/core.clj\n"
       "+++ b/src/app/core.clj\n"
       "@@ -4,9 +4,9 @@\n"
       " \n"
       " (defn handle-tick\n"
       "   [state]\n"
       "-  (update state :ticks inc))\n"
       "+  (update state :ticks (fnil inc 0)))\n"
       " \n"
       " (defn label\n"
       "   [state]\n"
       "-  ;; upper-case for the banner\n"
       "+  ;; Upper-case for the banner.\n"
       "   (str/upper-case (:name state)))\n"))

;; A whitespace-only reprint of a form nothing needed to touch.
(def whitespace-reprint-patch
  (str "--- a/src/app/util.clj\n"
       "+++ b/src/app/util.clj\n"
       "@@ -1,5 +1,5 @@\n"
       " (ns app.util)\n"
       " \n"
       "-(defn clamp\n"
       "-  [value low high]\n"
       "-  (max low (min high value)))\n"
       "+(defn clamp [value low high]\n"
       "+  (max low\n"
       "+       (min high value)))\n"))

;; Drops one closing paren: the post image cannot be read.
(def unreadable-post-image-patch
  (str "--- a/src/app/core.clj\n"
       "+++ b/src/app/core.clj\n"
       "@@ -4,7 +4,7 @@\n"
       " \n"
       " (defn handle-tick\n"
       "   [state]\n"
       "-  (update state :ticks inc))\n"
       "+  (update state :ticks inc)\n"
       " \n"
       " (defn label\n"
       "   [state]\n"))

;; Removes clojure.string from the ns form while fully qualifying its use.
(def require-removal-patch
  (str "--- a/src/app/core.clj\n"
       "+++ b/src/app/core.clj\n"
       "@@ -1,6 +1,4 @@\n"
       "-(ns app.core\n"
       "-  (:require\n"
       "-   [clojure.string :as str]))\n"
       "+(ns app.core)\n"
       " \n"
       " (defn handle-tick\n"
       "   [state]\n"
       "@@ -9,4 +7,4 @@\n"
       " (defn label\n"
       "   [state]\n"
       "   ;; upper-case for the banner\n"
       "-  (str/upper-case (:name state)))\n"
       "+  (clojure.string/upper-case (:name state)))\n"))

;; Edits the interior of a long code-shaped string; its opening quote is
;; nowhere near the hunk.
(def opaque-string-patch
  (str "--- a/src/app/embed.clj\n"
       "+++ b/src/app/embed.clj\n"
       "@@ -5,7 +5,7 @@\n"
       "      const state = { ticks: 0, name: 'unset', ready: false };\n"
       "      const timer = setInterval(function () {\n"
       "        state.ticks = state.ticks + 1;\n"
       "-       if (state.ticks > 10) { clearInterval(timer); state.ready = true; }\n"
       "+       if (state.ticks > 25) { clearInterval(timer); state.ready = true; }\n"
       "      }, 100);\n"
       "      return state;\n"
       "    }\")\n"))

(def stale-context-patch
  (str "--- a/src/app/util.clj\n"
       "+++ b/src/app/util.clj\n"
       "@@ -3,3 +3,3 @@\n"
       " (defn clamp\n"
       "   [value low high]\n"
       "-  (max high (min low value)))\n"
       "+  (long (max low (min high value))))\n"))

(def file-creation-patch
  (str "--- /dev/null\n"
       "+++ b/src/app/new.clj\n"
       "@@ -0,0 +1,1 @@\n"
       "+(ns app.new)\n"))

(def non-source-target-patch
  (str "--- a/README.md\n"
       "+++ b/README.md\n"
       "@@ -1,1 +1,1 @@\n"
       "-old\n"
       "+new\n"))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-134
;; Round four's superset assertion is used by witnesses both above and below
;; its definition; it is declared here so the file reads in narrative order
;; rather than in dependency order.
(declare ^:private assert-text-names-every-structured-leaf!)

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-admit-test"
             (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (.delete child))))

(defn- write-sources!
  [root sources]
  (doseq [[relative source] sources]
    (let [target (io/file root relative)]
      (.mkdirs (.getParentFile target))
      (spit target source)))
  root)

(defn- stub-config
  "One admit config whose verification seams are inert and observable."
  [root & [overrides]]
  (merge {:project-root (.getPath root)
          :admit-lint-runner (fn [_ _] {:ran true :ok true
                                        :introduced-count 0
                                        :removed-count 0
                                        :blocking-introduced []})
          :admit-test-runner
          (fn [_ {:keys [namespaces]}]
            {:ran true
             :namespace-results (into {} (map (fn [n] [n {:tests 1 :failures 0
                                                          :errors 0}]))
                                      namespaces)
             :tests-run (count namespaces)
             :passed (count namespaces) :failed 0 :skipped 0
             :namespaces (vec namespaces)})}
         overrides))

(defn- delta-for
  [file pre post patch]
  (let [applied (patch-apply/apply-patch {file pre} patch)
        image (first (:files applied))]
    (is (:ok applied) (str "fixture patch must apply: " (:error applied)))
    (form-identity/form-identity-delta
      {:file file
       :pre (:pre image)
       :post (:post image)
       :hunk-spans (:hunk-spans image)})))

(defn- hazard-types
  [delta-or-receipt]
  (set (map :type (:hazards delta-or-receipt))))

(defn- hazard-of
  [delta-or-receipt hazard-type]
  (first (filter #(= hazard-type (:type %)) (:hazards delta-or-receipt))))

;; ---------------------------------------------------------------------------
;; Registration
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-001
(deftest registers-one-admit-tool-in-the-full-profile
  (let [names (mapv :name (tool/tools-for-profile :full))]
    (is (= 1 (count (filter #{"admit_clojure_patch"} names))))
    (is (= ["inspect_clojure" "apply_clojure_changes" "edit_clojure"
            "transform_clojure" "alias_migration" "admit_clojure_patch"]
           names)))
  (let [registered (into {} (map (juxt :name identity))
                         (server/public-tool-registry))]
    (is (= #{:preview :committed :typed-refusal}
           (:outcome-classes (get registered "admit_clojure_patch"))))))

;; ---------------------------------------------------------------------------
;; Request admission
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-002
(deftest confines-every-patch-target-to-the-resolved-workspace-root
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [escape (str "--- a/../outside.clj\n"
                        "+++ b/../outside.clj\n"
                        "@@ -1,1 +1,1 @@\n"
                        "-a\n"
                        "+b\n")
            result (admit/execute-request!
                     (stub-config root)
                     {:patch escape :mode "commit" :verify "none" :allow_partial true})]
        (is (false? (:ok result)))
        (is (some? (:error-type result)))
        (is (true? (:source-unchanged result))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-003
(deftest refuses-a-missing-blank-or-unparseable-patch
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (doseq [patch [nil "" "   " "this is not a diff at all"]]
        (testing (pr-str patch)
          (let [result (admit/execute-request!
                         (stub-config root)
                         (cond-> {:mode "preview" :verify "none"}
                           (some? patch) (assoc :patch patch)))]
            (is (false? (:ok result)))
            (is (= :invalid-patch (:error-type result)))
            (is (true? (:source-unchanged result))))))
      (is (= core-source (slurp (io/file root "src/app/core.clj"))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-004
(deftest defaults-to-preview-and-focused-and-refuses-other-vocabularies
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [defaulted (admit/execute-request!
                        (stub-config root)
                        {:patch clean-multi-file-patch})]
        (is (:ok defaulted))
        (is (= "preview" (:mode defaulted)))
        (is (false? (:committed defaulted)))
        (is (true? (get-in defaulted [:lint_delta :ran])))
        (is (true? (get-in defaulted [:tests :ran]))))
      (doseq [[field value] [[:mode "apply"] [:verify "everything"]]]
        (testing (str field)
          (let [result (admit/execute-request!
                         (stub-config root)
                         (assoc {:patch clean-multi-file-patch} field value))]
            (is (false? (:ok result)))
            (is (= :invalid-admit-request (:error-type result))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-005
;; @spec MCP-OP-ADMIT-070
(deftest a-non-source-target-refuses-in-preview-and-in-commit
  (let [root (temp-dir)]
    (try
      (write-sources! root (assoc base-sources "README.md" "old\n"))
      (doseq [mode ["preview" "commit"]]
        (testing mode
          (let [result (admit/execute-request!
                         (stub-config root)
                         {:patch non-source-target-patch
                          :mode mode :verify "none" :allow_partial true})]
            (is (false? (:ok result))
                "a preview that returned ok would advertise a commit that refuses")
            (is (= :unsupported-patch-target (:error-type result)))
            (is (= ["passthrough"] (mapv :kind (:files result))))
            (is (= "old\n" (slurp (io/file root "README.md")))))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Patch application
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-010
(deftest applies-every-hunk-to-one-frozen-snapshot
  (let [applied (patch-apply/apply-patch base-sources clean-multi-file-patch)]
    (is (:ok applied))
    (is (= ["src/app/core.clj" "src/app/util.clj"]
           (mapv :file (:files applied))))
    (is (= base-sources (into {} (map (juxt :file :pre)) (:files applied))))
    (is (str/includes? (:post (first (:files applied)))
                       "(update state :ticks (fnil inc 0))"))
    (is (str/includes? (:post (second (:files applied)))
                       "(long (max low (min high value))))"))
    ;; Everything the hunks did not name is byte-identical.
    (is (str/includes? (:post (first (:files applied)))
                       ";; upper-case for the banner"))))

;; @spec MCP-OP-ADMIT-011
(deftest refuses-a-hunk-whose-context-does-not-match
  (let [applied (patch-apply/apply-patch base-sources stale-context-patch)]
    (is (false? (:ok applied)))
    (is (= :patch-does-not-apply (:error-type applied)))
    (is (= "src/app/util.clj" (:file applied)))
    (is (= 0 (:hunk-index applied))))
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch stale-context-patch :mode "commit" :verify "none" :allow_partial true})]
        (is (false? (:ok result)))
        (is (= :patch-does-not-apply (:error-type result)))
        (is (= util-source (slurp (io/file root "src/app/util.clj")))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-012
(deftest preview-writes-nothing
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch clean-multi-file-patch
                      :mode "preview" :verify "none"})]
        (is (:ok result))
        (is (false? (:committed result)))
        (is (= core-source (slurp (io/file root "src/app/core.clj"))))
        (is (= util-source (slurp (io/file root "src/app/util.clj")))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-013
(deftest publishes-the-pre-and-post-line-span-of-every-hunk
  (let [applied (patch-apply/apply-patch base-sources clean-multi-file-patch)
        core (first (:files applied))
        util (second (:files applied))]
    (is (= 1 (:hunk-count core)))
    ;; Only the removed/added lines, not the surrounding context.
    (is (= {:pre [[7 7]] :post [[7 7]]} (:hunk-spans core)))
    (is (= {:pre [[5 5]] :post [[5 5]]} (:hunk-spans util))))
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch clean-multi-file-patch :verify "none"})]
        (is (= {:pre [[7 7]] :post [[7 7]]}
               (:hunk_line_spans (first (:files result))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-095
(deftest admits-whole-file-creation-from-either-grammar
  (let [parsed (patch-apply/parse-patch file-creation-patch)]
    (is (:ok parsed))
    (is (= :add (:operation (first (:files parsed))))
        "a /dev/null source names a creation in either grammar"))
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch file-creation-patch :mode "commit" :verify "focused" :allow_partial true})]
        (is (:ok result))
        (is (true? (:committed result)))
        (is (= "created" (:pre_image_binding result)))
        (is (= "(ns app.new)\n" (slurp (io/file root "src/app/new.clj")))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Form-identity delta
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-020
(deftest reports-owners-added-removed-and-changed-by-defining-form-name
  (let [delta (delta-for "src/app/core.clj" core-source core-source
                         comment-reformat-patch)]
    (is (= [] (get-in delta [:owners :added])))
    (is (= [] (get-in delta [:owners :removed])))
    (is (= ["handle-tick"] (get-in delta [:owners :changed]))
        "label's code did not change, so it is not a changed owner"))
  (let [added (delta-for "src/app/core.clj" core-source core-source
                         duplicate-definition-patch)]
    (is (= #{:duplicate-definition} (hazard-types added)))))

;; @spec MCP-OP-ADMIT-021
(deftest counts-a-presentation-only-reprint-as-drift
  (let [delta (delta-for "src/app/util.clj" util-source util-source
                         whitespace-reprint-patch)]
    (is (pos? (:byte-drift-outside-hunks delta))
        "a whitespace-only reprint moved bytes for no structural reason")
    (is (= [] (get-in delta [:owners :changed]))
        "no owner's code changed")
    (is (= {} (:protected-node-drift delta))
        "no comment, metadata, conditional, or discard was involved")))

;; @spec MCP-OP-ADMIT-022
(deftest counts-a-changed-inter-owner-run-as-drift
  (let [pre util-source
        gap-patch (str "--- a/src/app/util.clj\n"
                       "+++ b/src/app/util.clj\n"
                       "@@ -1,3 +1,4 @@\n"
                       " (ns app.util)\n"
                       " \n"
                       "+;; clamping helpers\n"
                       " (defn clamp\n")
        delta (delta-for "src/app/util.clj" pre pre gap-patch)]
    (is (pos? (:byte-drift-outside-hunks delta)))
    (is (= [] (get-in delta [:owners :changed])))))

;; @spec MCP-OP-ADMIT-023
(deftest reports-protected-node-drift-per-owner
  (let [delta (delta-for "src/app/core.clj" core-source core-source
                         comment-reformat-patch)]
    (is (contains? (:protected-node-drift delta) "label"))
    (is (= 1 (get-in delta [:protected-node-drift "label" :comment :pre-count])))
    (is (= 1 (get-in delta [:protected-node-drift "label" :comment :post-count])))
    (is (true? (get-in delta [:protected-node-drift "label" :comment :text-changed])))
    (is (not (contains? (:protected-node-drift delta) "handle-tick")))
    (is (pos? (:byte-drift-outside-hunks delta))
        "the comment reformat moved bytes with no structural change"))
  (testing "a deleted comment is reported even when the owner's code changed"
    (let [deletion-patch (str "--- a/src/app/core.clj\n"
                              "+++ b/src/app/core.clj\n"
                              "@@ -9,4 +9,3 @@\n"
                              " (defn label\n"
                              "   [state]\n"
                              "-  ;; upper-case for the banner\n"
                              "-  (str/upper-case (:name state)))\n"
                              "+  (str/lower-case (:name state)))\n")
          delta (delta-for "src/app/core.clj" core-source core-source
                           deletion-patch)]
      (is (= ["label"] (get-in delta [:owners :changed])))
      (is (= -1 (get-in delta [:protected-node-drift "label" :comment :delta]))))))

;; @spec MCP-OP-ADMIT-024
(deftest a-clean-patch-drifts-nothing
  (let [applied (patch-apply/apply-patch base-sources clean-multi-file-patch)]
    (doseq [image (:files applied)]
      (let [delta (form-identity/form-identity-delta
                    {:file (:file image)
                     :pre (:pre image)
                     :post (:post image)
                     :hunk-spans (:hunk-spans image)})]
        (testing (:file image)
          (is (= 0 (:byte-drift-outside-hunks delta)))
          (is (= {} (:protected-node-drift delta)))
          (is (= [] (:hazards delta)))
          (is (= [] (get-in delta [:owners :added])))
          (is (= [] (get-in delta [:owners :removed])))
          (is (= 1 (count (get-in delta [:owners :changed])))))))))

;; ---------------------------------------------------------------------------
;; Hazards
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-030
(deftest every-hazard-carries-type-file-span-and-class
  (let [delta (delta-for "src/app/core.clj" core-source core-source
                         duplicate-definition-patch)
        hazard (hazard-of delta :duplicate-definition)]
    (is (= "src/app/core.clj" (:file hazard)))
    (is (= "handle-tick" (:owner hazard)))
    (is (= :refusal (:class hazard)))
    (is (vector? (:span hazard)))
    (is (= 2 (count (:span hazard))))
    (is (string? (:message hazard)))))

;; @spec MCP-OP-ADMIT-031
(deftest an-unreadable-post-image-is-a-refusal-class-hazard
  (let [delta (delta-for "src/app/core.clj" core-source core-source
                         unreadable-post-image-patch)
        hazard (hazard-of delta :unreadable-post-image)]
    (is (some? hazard))
    (is (= :refusal (:class hazard)))
    (is (= "src/app/core.clj" (:file hazard)))))

;; @spec MCP-OP-ADMIT-032
(deftest a-duplicate-top-level-definition-is-a-refusal-class-hazard
  (let [delta (delta-for "src/app/core.clj" core-source core-source
                         duplicate-definition-patch)
        hazard (hazard-of delta :duplicate-definition)]
    (is (= :refusal (:class hazard)))
    (is (= "handle-tick" (:owner hazard)))
    (is (= 2 (count (:spans hazard))) "every defining span, in source order")
    (is (apply < (map first (:spans hazard))))
    (is (true? (:introduced-by-patch hazard))))
  (testing "legitimately repeated defining forms are not duplicates"
    (let [multi (str "(ns app.render)\n"
                     "\n"
                     "(defmulti render :kind)\n"
                     "\n"
                     "(defmethod render :card [x] x)\n")
          patch (str "--- a/src/app/render.clj\n"
                     "+++ b/src/app/render.clj\n"
                     "@@ -3,3 +3,5 @@\n"
                     " (defmulti render :kind)\n"
                     " \n"
                     " (defmethod render :card [x] x)\n"
                     "+\n"
                     "+(defmethod render :list [x] x)\n")
          delta (delta-for "src/app/render.clj" multi multi patch)]
      (is (nil? (hazard-of delta :duplicate-definition))))))

;; @spec MCP-OP-ADMIT-033
(deftest a-lost-require-is-a-refusal-class-hazard
  (let [delta (delta-for "src/app/core.clj" core-source core-source
                         require-removal-patch)
        hazard (hazard-of delta :require-removed)]
    (is (some? hazard))
    (is (= :refusal (:class hazard)))
    (is (= ["clojure.string"] (:libraries hazard)))))

;; @spec MCP-OP-ADMIT-034
(deftest an-opaque-string-edit-is-informational-and-never-refuses
  (let [delta (delta-for "src/app/embed.clj" embed-source embed-source
                         opaque-string-patch)
        hazard (hazard-of delta :opaque-string-edit)]
    (is (some? hazard))
    (is (= :informational (:class hazard)))
    (is (= "bootstrap-script" (:owner hazard)))
    (is (empty? (filter #(= :refusal (:class %)) (:hazards delta))))))

;; ---------------------------------------------------------------------------
;; Verification
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-040
(deftest focused-verification-publishes-a-location-independent-lint-delta
  (let [root (temp-dir)
        seen (atom nil)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root
                                  {:admit-lint-runner
                                   (fn [_ images]
                                     (reset! seen (mapv :file images))
                                     {:ran true :ok true :introduced-count 0
                                      :removed-count 1
                                      :blocking-introduced []})})
                     {:patch clean-multi-file-patch :verify "focused"})]
        (is (:ok result))
        (is (= ["src/app/core.clj" "src/app/util.clj"] @seen))
        (is (true? (get-in result [:lint_delta :ran])))
        (is (= 1 (get-in result [:lint_delta :removed-count]))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-041
(deftest focused-verification-derives-test-namespaces-from-touched-sources
  (let [root (temp-dir)
        seen (atom nil)]
    (try
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj"
                                  "(ns app.core-test)\n"))
      (let [result (admit/execute-request!
                     (stub-config root
                                  {:admit-test-runner
                                   (fn [_ {:keys [namespaces]}]
                                     (reset! seen (vec namespaces))
                                     {:ran true :tests-run 3 :passed 3 :failed 0
                                      :skipped 0
                                      :namespace-results
                                      (into {} (map (fn [n] [n {:tests 3
                                                                :failures 0
                                                                :errors 0}]))
                                            namespaces)
                                      :namespaces (vec namespaces)})})
                     {:patch clean-multi-file-patch :verify "focused"})]
        (is (:ok result))
        (is (= ["app.core-test"] @seen)
            "app.util-test has no file, so it is derived but not run")
        (is (= ["app.core-test"] (get-in result [:tests :namespaces])))
        (is (= 3 (get-in result [:tests :passed]))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-042
;; @spec MCP-OP-ADMIT-126
(deftest verification-none-runs-nothing-and-claims-nothing
  (let [root (temp-dir)
        touched (atom false)
        config (stub-config
                 root
                 {:admit-lint-runner
                  (fn [_ _] (reset! touched true) {:ran true :ok true})
                  :admit-test-runner
                  (fn [_ _] (reset! touched true) {:ran true})})]
    (try
      (write-sources! root base-sources)
      (testing "preview is where a declined verification belongs"
        (let [result (admit/execute-request!
                       config
                       {:patch clean-multi-file-patch
                        :mode "preview" :verify "none" :allow_partial true})]
          (is (:ok result))
          (is (false? @touched))
          (is (false? (get-in result [:lint_delta :ran])))
          (is (false? (get-in result [:tests :ran])))
          (is (false? (:verification_complete result)))))
      (testing "and in commit mode allow_partial does not buy it a write"
        (let [result (admit/execute-request!
                       config
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "none" :allow_partial true})]
          (is (false? (:ok result)))
          (is (false? (:committed result)))
          (is (false? @touched) "nothing was run, in either mode")
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-043
(deftest an-unavailable-or-failing-check-never-reads-as-complete
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (testing "unavailable analyzer"
        (let [result (admit/execute-request!
                       (stub-config root
                                    {:admit-lint-runner
                                     (fn [_ _] {:ran false :ok false
                                                :status :unverified
                                                :error-type :clj-kondo-executable-unavailable})})
                       {:patch clean-multi-file-patch :verify "focused"})]
          (is (:ok result))
          (is (false? (:verification_complete result)))
          (is (= :unverified (get-in result [:lint_delta :status])))))
      (testing "failing focused tests"
        (let [result (admit/execute-request!
                       (stub-config root
                                    {:admit-test-runner
                                     (fn [_ {:keys [namespaces]}]
                                       {:ran true :tests-run 3 :passed 1
                                        :failed 2 :skipped 0
                                        :namespaces (vec namespaces)})})
                       {:patch clean-multi-file-patch :verify "focused"})]
          (is (false? (:verification_complete result)))
          (is (= 2 (get-in result [:tests :failed])))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-044
(deftest verification-is-complete-only-when-every-check-ran-and-passed
  (let [root (temp-dir)]
    (try
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj"
                                  "(ns app.core-test)\n"))
      (let [result (admit/execute-request!
                     (stub-config root
                                  {:admit-test-runner
                                   (fn [_ {:keys [namespaces]}]
                                     {:ran true :tests-run 12 :passed 12
                                      :failed 0 :skipped 0
                                      :namespace-results
                                      (into {} (map (fn [n] [n {:tests 12
                                                                :failures 0
                                                                :errors 0}]))
                                            namespaces)
                                      :namespaces (vec namespaces)})})
                     {:patch clean-multi-file-patch
                      :mode "commit" :verify "focused"})]
        (is (:ok result))
        (is (true? (:committed result)))
        (is (true? (:verification_complete result)))
        (is (= :namespace-report (get-in result [:tests :evidence])))
        (is (= :complete (:verification_status result))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Receipt, commit, refusal
;; ---------------------------------------------------------------------------

(def receipt-keys
  #{:ok :operation :mode :committed :mutation_attempted
    :files :owners :protected_node_drift
    :byte_drift_outside_hunks :hazards :lint_delta :tests :hashes
    :pre_image_binding :verification_status :verification_reasons
    :verification_complete :next_call :source-unchanged})

;; @spec MCP-OP-ADMIT-050
(deftest every-receipt-carries-the-closed-key-set
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (doseq [[label params]
              [["preview" {:patch clean-multi-file-patch :verify "none"}]
               ["commit" {:patch clean-multi-file-patch
                          :mode "commit" :verify "none" :allow_partial true}]
               ["hazard refusal" {:patch duplicate-definition-patch
                                  :mode "commit" :verify "none" :allow_partial true}]]]
        (testing label
          (let [result (admit/execute-request! (stub-config root) params)]
            (is (empty? (remove (set (keys result)) receipt-keys))
                (str "missing: " (pr-str (remove (set (keys result))
                                                 receipt-keys))))
            (is (map? (:hashes result)))
            (is (vector? (:hazards result))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-051
(deftest a-refusal-class-hazard-blocks-the-commit-and-returns-a-full-receipt
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch duplicate-definition-patch
                      :mode "commit" :verify "none" :allow_partial true})]
        (is (false? (:ok result)))
        (is (false? (:committed result)))
        (is (true? (:source-unchanged result)))
        (is (= core-source (slurp (io/file root "src/app/core.clj"))))
        (is (contains? (hazard-types result) :duplicate-definition))
        (is (= "preview" (get-in result [:next_call :arguments :mode])))
        (is (= :duplicate-definition (get-in result [:next_call :blocked_by])))
        (is (= "admit_clojure_patch" (get-in result [:next_call :tool])))
        (is (nil? (get-in result [:next_call :arguments :patch]))
            "a refusal never echoes the payload that caused it")
        (is (= "patch" (get-in result [:next_call :patch_field])))
        (is (re-matches #"[0-9a-f]{64}"
                        (get-in result [:next_call :patch_sha256]))
            "the follow-up is bound to the same patch by digest"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-052
(deftest a-clean-commit-writes-atomically-and-proves-read-back
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch clean-multi-file-patch
                      :mode "commit" :verify "focused" :allow_partial true})]
        (is (:ok result))
        (is (true? (:committed result)))
        (is (= :admit-patch! (:operation result)))
        (is (= "unbound" (:pre_image_binding result))
            "a commit that carried no preview binding says so on the receipt")
        (is (str/includes? (slurp (io/file root "src/app/core.clj"))
                           "(fnil inc 0)"))
        (is (str/includes? (slurp (io/file root "src/app/util.clj"))
                           "(long (max low"))
        (is (= 2 (count (:hashes result))))
        (doseq [[_ {:keys [pre post]}] (:hashes result)]
          (is (re-matches #"[0-9a-f]{64}" pre))
          (is (re-matches #"[0-9a-f]{64}" post))
          (is (not= pre post))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-053
(deftest a-stale-snapshot-refuses-the-commit-without-writing
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [drifted (str util-source "\n;; a competing seat wrote here\n")
            result (admit/execute-request!
                     (assoc (stub-config root)
                            :admit-before-commit!
                            (fn [] (spit (io/file root "src/app/util.clj")
                                         drifted)))
                     {:patch clean-multi-file-patch
                      :mode "commit" :verify "focused" :allow_partial true})]
        (is (false? (:ok result)))
        (is (= :source-hash-mismatch (:error-type result)))
        (is (true? (:source-unchanged result)))
        (is (= drifted (slurp (io/file root "src/app/util.clj"))))
        (is (= core-source (slurp (io/file root "src/app/core.clj")))
            "the earlier file is restored, not half-committed"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-054
(deftest every-admit-call-emits-one-content-free-telemetry-event
  (let [root (temp-dir)
        events (atom [])]
    (try
      (write-sources! root base-sources)
      (doseq [params [{:patch clean-multi-file-patch :verify "none"}
                      {:patch duplicate-definition-patch
                       :mode "commit" :verify "none" :allow_partial true}]]
        (admit/execute-request!
          (assoc (stub-config root)
                 :telemetry {:mode :metrics
                             :session-id "admit-test"
                             :lock (Object.)
                             :emit! (fn [event] (swap! events conj event))})
          params))
      (is (= 2 (count @events)))
      (doseq [event @events]
        (is (= "admit_clojure_patch" (:tool event)))
        (is (map? (:request_shape event)))
        (is (map? (:outcome event)))
        (is (not (contains? event :patch)))
        (is (not (str/includes? (pr-str event) "handle-tick"))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-055
(deftest a-refusal-is-never-an-empty-payload
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (doseq [params [{:patch "garbage" :verify "none"}
                      {:patch stale-context-patch :mode "commit" :verify "none" :allow_partial true}
                      {:patch non-source-target-patch
                       :mode "commit" :verify "none" :allow_partial true}
                      {:patch duplicate-definition-patch
                       :mode "commit" :verify "none" :allow_partial true}]]
        (testing (pr-str params)
          (let [result (admit/execute-request! (stub-config root) params)]
            (is (false? (:ok result)))
            (is (keyword? (:error-type result)))
            (is (string? (:error result)))
            (is (seq (:error result)))
            (is (true? (:source-unchanged result)))
            (is (some? (:next_call result))))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Red-team witnesses. Each of these reproduces one probe from the adversarial
;; review at scratchpad/redteam-admit/p1..p9.clj, so the class it found cannot
;; return unnoticed.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-060
(deftest every-escape-shape-refuses-before-any-write
  (let [root (temp-dir)
        outside (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [victim (io/file outside "victim.clj")
            _ (spit victim "(ns victim)\n(def x 1)\n")
            before (slurp victim)
            hunk "@@ -2,1 +2,1 @@\n-(def x 1)\n+(def x 2)\n"
            shapes
            {"parent traversal" (str "--- a/../victim.clj\n"
                                     "+++ b/../victim.clj\n" hunk)
             "absolute with a/b prefix" (str "--- a" (.getPath victim) "\n"
                                             "+++ b" (.getPath victim) "\n" hunk)
             "absolute raw" (str "--- " (.getPath victim) "\n"
                                 "+++ " (.getPath victim) "\n" hunk)
             "NUL byte" (str "--- a/src/app/co" (char 0) "re.clj\n"
                             "+++ b/src/app/co" (char 0) "re.clj\n"
                             "@@ -7,1 +7,1 @@\n"
                             "-  (update state :ticks inc))\n"
                             "+  (update state :ticks dec))\n")
             "percent-encoded traversal" (str "--- a/%2e%2e/victim.clj\n"
                                              "+++ b/%2e%2e/victim.clj\n" hunk)
             "backslash traversal" (str "--- a/..\\victim.clj\n"
                                        "+++ b/..\\victim.clj\n" hunk)}]
        (doseq [[label patch] shapes]
          (testing label
            (let [result (admit/execute-request!
                           (stub-config root)
                           {:patch patch :mode "commit" :verify "none" :allow_partial true})]
              (is (false? (:ok result)))
              (is (keyword? (:error-type result)))
              (is (false? (:committed result)))
              (is (= before (slurp victim))))))
        (testing "a symlink that resolves outside the root"
          (Files/createSymbolicLink
            (.toPath (io/file root "src/app/link.clj"))
            (.toPath victim)
            (make-array FileAttribute 0))
          (doseq [mode ["preview" "commit"]]
            (let [result (admit/execute-request!
                           (stub-config root)
                           {:patch (str "--- a/src/app/link.clj\n"
                                        "+++ b/src/app/link.clj\n" hunk)
                            :mode mode :verify "none" :allow_partial true})]
              (is (false? (:ok result)))
              (is (= :path-outside-project (:error-type result)))
              (is (= before (slurp victim))))))
        (testing "two file headers naming one file refuse before the transaction"
          (let [patch (str "--- a/src/app/core.clj\n+++ b/src/app/core.clj\n"
                           "@@ -7,1 +7,1 @@\n"
                           "-  (update state :ticks inc))\n"
                           "+  (update state :ticks dec))\n"
                           "--- a/src/app/core.clj\n+++ b/src/app/core.clj\n"
                           "@@ -1,1 +1,1 @@\n-(ns app.core\n+(ns app.core2\n")
                result (admit/execute-request!
                         (stub-config root)
                         {:patch patch :mode "commit" :verify "none" :allow_partial true})]
            (is (false? (:ok result)))
            (is (= :duplicate-patch-target (:error-type result))
                "an ambiguous target is refused, not discovered by a failed write")
            (is (true? (:source-unchanged result)))
            (is (= core-source (slurp (io/file root "src/app/core.clj")))))))
      (finally
        (delete-tree! root)
        (delete-tree! outside)))))

;; @spec MCP-OP-ADMIT-061
(deftest a-later-file-failure-leaves-every-earlier-file-original
  (let [good-core (str "--- a/src/app/core.clj\n+++ b/src/app/core.clj\n"
                       "@@ -7,1 +7,1 @@\n"
                       "-  (update state :ticks inc))\n"
                       "+  (update state :ticks (fnil inc 0)))\n")
        stale-util (str "--- a/src/app/util.clj\n+++ b/src/app/util.clj\n"
                        "@@ -5,1 +5,1 @@\n"
                        "-  (NOPE low (min high value)))\n"
                        "+  (long (max low (min high value))))\n")
        root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch (str good-core stale-util)
                      :mode "commit" :verify "none" :allow_partial true})]
        (is (false? (:ok result)))
        (is (= :patch-does-not-apply (:error-type result)))
        (is (true? (:source-unchanged result)))
        (is (= core-source (slurp (io/file root "src/app/core.clj")))
            "the first file is never written when a later one cannot apply")
        (is (= util-source (slurp (io/file root "src/app/util.clj")))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-062
(deftest a-file-that-moves-under-the-commit-refuses-and-keeps-the-newer-bytes
  (doseq [[label target] [["first file" "src/app/core.clj"]
                          ["second file" "src/app/util.clj"]]]
    (testing label
      (let [root (temp-dir)]
        (try
          (write-sources! root base-sources)
          (let [drifted (str (get base-sources target) "\n;; a competing seat\n")
                result (admit/execute-request!
                         (assoc (stub-config root)
                                :admit-before-commit!
                                (fn [] (spit (io/file root target) drifted)))
                         {:patch clean-multi-file-patch
                          :mode "commit" :verify "focused" :allow_partial true})]
            (is (false? (:ok result)))
            (is (= :source-hash-mismatch (:error-type result)))
            (is (true? (:source-unchanged result)))
            (is (= drifted (slurp (io/file root target)))
                "the competing write is preserved, never overwritten")
            (doseq [[other source] base-sources
                    :when (not= other target)]
              (is (= source (slurp (io/file root other)))
                  "no other file was left half-committed")))
          (finally (delete-tree! root)))))))

;; @spec MCP-OP-ADMIT-063
(deftest a-preview-binds-the-commit-to-the-bytes-it-inspected
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [preview (admit/execute-request!
                      (stub-config root)
                      {:patch clean-multi-file-patch :verify "none"})
            binding (get-in preview [:next_call :arguments :expect_pre_sha256])]
        (is (:ok preview))
        (is (= "commit" (get-in preview [:next_call :arguments :mode])))
        (is (= #{"src/app/core.clj" "src/app/util.clj"}
               (set (map name (keys binding))))
            "the follow-up carries a pre-image hash for every touched file")
        (is (= (get-in preview [:hashes "src/app/core.clj" :pre])
               (get binding "src/app/core.clj")))
        (testing "an untouched workspace still commits"
          (let [result (admit/execute-request!
                         (stub-config root)
                         {:patch clean-multi-file-patch :mode "commit"
                          :verify "focused" :allow_partial true
                          :expect_pre_sha256 binding})]
            (is (:ok result))
            (is (true? (:committed result)))
            (is (= "bound" (:pre_image_binding result)))))
        (testing "a workspace that moved after the preview refuses"
          (write-sources! root base-sources)
          (let [concurrent (str core-source
                                "\n(def SECRET (System/getenv \"AWS\"))\n")
                _ (spit (io/file root "src/app/core.clj") concurrent)
                result (admit/execute-request!
                         (stub-config root)
                         {:patch clean-multi-file-patch :mode "commit"
                          :verify "none" :expect_pre_sha256 binding})]
            (is (false? (:ok result)))
            (is (= :source-hash-mismatch (:error-type result)))
            (is (false? (:committed result)))
            (is (= concurrent (slurp (io/file root "src/app/core.clj")))
                "the concurrent edit survives the refused commit"))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-064
(deftest a-wrapper-cannot-hide-a-duplicate-definition
  (let [base (str "(ns app.a\n"
                  "  (:require\n"
                  "   [clojure.string :as str]\n"
                  "   [clojure.set :as set]))\n"
                  "\n"
                  "(defn tick\n"
                  "  [s]\n"
                  "  (inc s))\n")
        delta (fn [post]
                (form-identity/form-identity-delta
                  {:file "src/app/a.clj" :pre base :post post
                   :hunk-spans {:pre [[1 8]] :post [[1 40]]}}))
        duplicate? (fn [post]
                     (some #(= :duplicate-definition (:type %))
                           (:hazards (delta post))))]
    (testing "wrappers a line differ cannot see through"
      (doseq [[label wrapper]
              [["plain" "\n(defn tick\n  [s]\n  (dec s))\n"]
               ["reader conditional" "\n#?(:clj\n   (defn tick\n     [s]\n     (dec s)))\n"]
               ["do" "\n(do\n  (defn tick\n    [s]\n    (dec s)))\n"]
               ["metadata" "\n^{:x 1}\n(defn tick\n  [s]\n  (dec s))\n"]
               ["declare in front" "\n(declare tick)\n\n(defn tick\n  [s]\n  (dec s))\n"]]]
        (testing label
          (is (duplicate? (str base wrapper))
              "a second binding of tick is a duplicate however it is wrapped"))))
    (testing "forms that are read and discarded are not definitions"
      (is (not (duplicate? (str base "\n(comment\n  (defn tick\n    [s]\n    (dec s)))\n"))))
      (is (not (duplicate? (str base "\n#_(defn tick\n  [s]\n  (dec s))\n")))))
    (testing "a declare beside its own defn is idiomatic, not a duplicate"
      (is (not (duplicate? (str "(ns app.a)\n\n(declare tick)\n\n"
                                "(defn tick\n  [s]\n  (inc s))\n")))))
    (testing "one reader conditional's branches are one definition"
      (is (not (duplicate?
                 (str "(ns app.a)\n\n#?(:clj (defn tick [s] (inc s))\n"
                      "   :cljs (defn tick [s] (dec s)))\n")))))
    (testing "defmethod repeats by design"
      (is (not (duplicate?
                 (str "(ns app.a)\n\n(defmulti render :kind)\n\n"
                      "(defmethod render :card [x] x)\n\n"
                      "(defmethod render :list [x] x)\n")))))))

;; @spec MCP-OP-ADMIT-064
(deftest a-declare-shielded-duplicate-refuses-a-commit
  (let [root (temp-dir)
        src "(ns app.a)\n\n(defn tick\n  [s]\n  (inc s))\n"]
    (try
      (write-sources! root {"src/app/a.clj" src})
      (let [patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                       "@@ -5,1 +5,7 @@\n"
                       "   (inc s))\n"
                       "+\n"
                       "+(declare tick)\n"
                       "+\n"
                       "+(defn tick\n"
                       "+  [s]\n"
                       "+  (dec s))\n")
            result (admit/execute-request!
                     (stub-config root)
                     {:patch patch :mode "commit" :verify "none" :allow_partial true})]
        (is (false? (:ok result)))
        (is (= :duplicate-definition (:error-type result)))
        (is (false? (:committed result)))
        (is (= src (slurp (io/file root "src/app/a.clj")))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-065
(deftest a-lost-require-is-found-through-prefix-lists-and-reader-conditionals
  (let [requires (fn [source]
                   (form-identity/ns-requires
                     (:node (first (filter #(= :ns (:form-kind %))
                                           (:units (form-identity/decompose
                                                     source)))))))
        lost? (fn [pre post]
                (some #(= :require-removed (:type %))
                      (:hazards (form-identity/form-identity-delta
                                  {:file "src/app/a.clj" :pre pre :post post
                                   :hunk-spans {:pre [[1 6]] :post [[1 6]]}}))))]
    (testing "a prefix list names every member"
      (is (= #{"clojure.string" "clojure.set"}
             (set (keys (requires
                          (str "(ns app.a\n  (:require\n"
                               "   [clojure [string :as str] [set :as set]]))\n")))))))
    (testing "dropping one member of a prefix list is a lost require"
      (let [pre (str "(ns app.a\n  (:require\n"
                     "   [clojure [string :as str] [set :as set]]))\n"
                     "\n(defn tick [s] (inc s))\n")
            post (str/replace pre " [set :as set]" "")]
        (is (lost? pre post))))
    (testing "a require lost from an ns carrying a reader conditional"
      (let [pre (str "(ns app.a\n  (:require\n"
                     "   [clojure.string :as str]\n"
                     "   [clojure.set :as set])\n"
                     "  #?(:clj (:import (java.io File))))\n"
                     "\n(defn tick [s] (inc s))\n")
            post (str/replace pre "\n   [clojure.set :as set]" "")]
        (is (lost? pre post))))
    (testing "renaming only an alias is not a lost require"
      (let [pre (str "(ns app.a\n  (:require\n"
                     "   [clojure.string :as str]))\n"
                     "\n(defn tick [s] (inc s))\n")
            post (str/replace pre ":as str" ":as s")]
        (is (not (lost? pre post)))))
    (testing "a bare symbol and a refer'd libspec both name one library"
      (is (= {"clojure.set" #{}}
             (requires "(ns app.a\n  (:require clojure.set))\n")))
      (is (= {"clojure.set" #{"union"}}
             (requires (str "(ns app.a\n  (:require\n"
                            "   [clojure.set :as set :refer [union]]))\n")))
          "the referred symbols travel with the library"))))

;; @spec MCP-OP-ADMIT-066
(deftest an-oversized-patch-is-a-typed-refusal-not-an-exception
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (doseq [[label characters]
              [["just over the cap" (inc admit/max-patch-bytes)]
               ["25 MB" (* 25 1024 1024)]]]
        (testing label
          (let [huge (str clean-multi-file-patch
                          (apply str (repeat characters \z)))
                result (admit/execute-request!
                         (stub-config root)
                         {:patch huge :mode "commit" :verify "none" :allow_partial true})]
            (is (false? (:ok result))
                "an oversized payload must not escape as an exception")
            (is (= :patch-too-large (:error-type result)))
            (is (false? (:committed result)))
            (is (nil? (get-in result [:next_call :arguments :patch])))
            (is (= core-source (slurp (io/file root "src/app/core.clj")))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-067
(deftest the-delta-stays-linear-on-a-large-file
  (let [forms 4000
        body (str/join "\n\n" (map #(str "(defn f" % "\n  [s]\n  (inc s))")
                                   (range forms)))
        pre (str "(ns app.big)\n\n" body "\n")
        post (str/replace-first pre
                                "(defn f0\n  [s]\n  (inc s))"
                                "(defn f0\n  [s]\n  (dec s))")
        lines (count (str/split pre #"\n" -1))
        started (System/nanoTime)
        delta (form-identity/form-identity-delta
                {:file "src/app/big.clj" :pre pre :post post
                 :hunk-spans {:pre [[3 5]] :post [[3 5]]}})
        elapsed-ms (/ (double (- (System/nanoTime) started)) 1000000.0)]
    (is (< 16000 lines) "the fixture is the size the bound is stated for")
    (is (= ["f0"] (get-in delta [:owners :changed])))
    (is (= 0 (:byte-drift-outside-hunks delta)))
    (is (< elapsed-ms 2000.0)
        (str "form-identity-delta took " (long elapsed-ms)
             " ms on " lines " lines; a linear line index keeps this bounded"))))

;; @spec MCP-OP-ADMIT-068
(deftest verification-runs-against-the-snapshot-before-anything-is-written
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (testing "blocking analyzer findings write nothing"
        (let [result (admit/execute-request!
                       (stub-config root
                                    {:admit-lint-runner
                                     (fn [_ _] {:ran true :ok false
                                                :introduced-count 7
                                                :removed-count 0
                                                :blocking-introduced
                                                [{:type :unresolved-symbol}]})})
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (false? (:ok result)))
          (is (= :verification-failed (:error-type result)))
          (is (false? (:committed result)))
          (is (false? (:verification_complete result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))))
      (testing "failing focused tests write nothing"
        (let [result (admit/execute-request!
                       (stub-config root
                                    {:admit-test-runner
                                     (fn [_ {:keys [namespaces]}]
                                       {:ran true :tests-run 4 :passed 1
                                        :failed 3 :skipped 0
                                        :namespaces (vec namespaces)})})
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (false? (:ok result)))
          (is (= :verification-failed (:error-type result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))
          (is (= "preview" (get-in result [:next_call :arguments :mode])))))
      (testing "a runner that exits zero without running tests is not evidence"
        (write-sources! root (assoc base-sources
                                    "test/app/core_test.clj"
                                    "(ns app.core-test)\n"))
        (let [result (admit/execute-request!
                       (stub-config root
                                    {:admit-test-runner
                                     (fn [_ {:keys [namespaces]}]
                                       {:ran true :tests-run 0 :passed 0
                                        :failed 0 :skipped 0
                                        :namespaces (vec namespaces)})})
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          ;; @spec MCP-OP-ADMIT-105
          ;; This test used to assert the defect: "no check failed, so the
          ;; commit is allowed". A runner that ran nothing is not a check that
          ;; did not fail; it is a check that did not happen.
          (is (false? (:ok result)))
          (is (false? (:committed result)))
          (is (= :verification-incomplete (:error-type result)))
          (is (false? (:verification_complete result))
              "process exit status is not a test result")
          (is (= ["app.core-test"] (get-in result [:tests :namespaces])))
          (is (= :no-test-evidence (get-in result [:tests :reason])))
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))))
      (testing "nothing to attribute a test result to is its own reason"
        (let [bare (temp-dir)]
          (try
            (write-sources! bare base-sources)
            (let [result (admit/execute-request!
                           (stub-config bare
                                        {:admit-test-runner
                                         (fn [_ {:keys [namespaces]}]
                                           {:ran true :tests-run 0 :passed 0
                                            :failed 0 :skipped 0
                                            :namespaces (vec namespaces)})})
                           {:patch clean-multi-file-patch
                            :mode "preview" :verify "focused"})]
              (is (false? (:verification_complete result)))
              (is (= [] (get-in result [:tests :namespaces])))
              (is (= :no-mapped-test-namespace (get-in result [:tests :reason]))))
            (finally (delete-tree! bare)))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-068
(deftest a-declared-focused-runner-must-be-pointed-at-the-snapshot
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     {:project-root (.getPath root)
                      :focused-test {:command ["true" "{namespaces}"]}
                      :admit-lint-runner (fn [_ _] {:ran true :ok true
                                                    :introduced-count 0
                                                    :removed-count 0
                                                    :blocking-introduced []})}
                     {:patch clean-multi-file-patch :verify "focused"})]
        (is (false? (get-in result [:tests :ran])))
        (is (= :test-command-not-snapshot-bound (get-in result [:tests :reason])))
        (is (false? (:verification_complete result))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-069
(deftest a-refusal-fits-the-public-budget-and-never-echoes-the-patch
  (let [root (temp-dir)]
    (try
      (let [filler (apply str (repeat 3000 "; filler a real patch could carry\n"))
            src (str "(ns app.big)\n\n" filler "(defn tick\n  [s]\n  (inc s))\n")
            _ (write-sources! root {"src/app/big.clj" src})
            lines (vec (str/split src #"\n" -1))
            hunks (apply str (for [i (range 3 2400)]
                               (str "@@ -" i ",1 +" i ",1 @@\n"
                                    "-" (nth lines (dec i)) "\n"
                                    "+; edited " i "\n")))
            patch (str "--- a/src/app/big.clj\n+++ b/src/app/big.clj\n"
                       hunks
                       "@@ -2500,1 +2500,1 @@\n-NOPE\n+NOPE2\n")
            result (admit/execute-request!
                     (stub-config root)
                     {:patch patch :mode "commit" :verify "none" :allow_partial true})
            bytes (write-refusal/json-bytes result)]
        (is (< (count patch) admit/max-patch-bytes)
            "the fixture is admitted, so the refusal is the thing under test")
        (is (false? (:ok result)))
        (is (= :patch-does-not-apply (:error-type result)))
        (is (nil? (get-in result [:next_call :arguments :patch])))
        (is (not (str/includes? (pr-str result) "; filler a real patch"))
            "no refusal carries the payload that caused it")
        (is (<= bytes write-refusal/public-byte-budget)
            (str "refusal serialized to " bytes " bytes; the shared budget is "
                 write-refusal/public-byte-budget)))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-071
(deftest a-caller-supplied-workspace-root-is-the-routers-contract
  ;; Characterization only. The router has always honoured workspace_root for
  ;; every workspace-routed tool, and the admit gate neither widens nor narrows
  ;; it. Changing that is a routing decision for the whole server, recorded in
  ;; the design document as out of scope for this branch.
  (let [configured (temp-dir)
        elsewhere (temp-dir)]
    (try
      (write-sources! configured base-sources)
      (write-sources! elsewhere {"src/app/core.clj" core-source
                                 "src/app/util.clj" util-source})
      (let [result (admit/execute-request!
                     (stub-config configured)
                     {:patch clean-multi-file-patch :mode "commit"
                      :verify "focused" :allow_partial true
                      :workspace_root (.getPath elsewhere)})]
        (is (:ok result))
        (is (= (.getCanonicalPath elsewhere) (:workspace-root result))
            "the request routes to the caller's canonical root")
        (is (str/includes? (slurp (io/file elsewhere "src/app/core.clj"))
                           "(fnil inc 0)"))
        (is (= core-source (slurp (io/file configured "src/app/core.clj")))
            "the configured root is untouched by a request routed elsewhere"))
      (finally
        (delete-tree! configured)
        (delete-tree! elsewhere)))))

;; ---------------------------------------------------------------------------
;; Red-team round two. Probes at scratchpad/redteam-admit2/r1..r6.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-084
(deftest concurrent-commits-never-lose-an-edit-they-claimed
  (dotimes [_ 3]
    (let [root (temp-dir)
          base (str "(ns app.a)\n\n"
                    (apply str (for [i (range 20)] (str "(def v" i " 0)\n"))))]
      (try
        (write-sources! root {"src/app/a.clj" base})
        (let [config {:project-root (.getPath root)}
              patches (vec (for [i (range 8)]
                             [i (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                                     "@@ -" (+ 3 i) ",1 +" (+ 3 i) ",1 @@\n"
                                     "-(def v" i " 0)\n"
                                     "+(def v" i " " (inc i) ")\n")]))
              results (doall (pmap (fn [[i patch]]
                                     [i (admit/execute-request!
                                          config
                                          {:patch patch :mode "commit"
                                           :verify "none"})])
                                   patches))
              final (slurp (io/file root "src/app/a.clj"))
              claimed (set (map first (filter (comp :committed second) results)))
              present (set (filter #(str/includes?
                                      final (str "(def v" % " " (inc %) ")"))
                                   (range 8)))]
          (is (= claimed (set/intersection claimed present))
              (str "edits claimed as committed but absent from the file: "
                   (pr-str (sort (set/difference claimed present)))))
          (is (empty? (filter #(= :transaction-recovery-required
                                  (:error-type (second %)))
                              results))
              "a serialised writer never reaches the kernel's manual-recovery state")
          (doseq [[_ result] results]
            (is (or (:committed result) (false? (:ok result)))
                "every request either committed or refused")))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-084
(deftest the-write-lock-is-keyed-by-workspace-and-is-advisory-across-processes
  (let [root (temp-dir)]
    (try
      (is (nil? (workspace-lock/advisory-lock-file (.getPath root)))
          "no state directory means no lock file is scattered into the tree")
      (.mkdirs (io/file root ".clj-surgeon"))
      (let [file (workspace-lock/advisory-lock-file (.getPath root))]
        (is (some? file))
        (is (= "write.lock" (.getName file)))
        (workspace-lock/call-with-workspace-write-lock
          (.getPath root) (fn [] (is (.isFile file))))) 
      (testing "the monitor serialises threads on one root"
        (let [order (atom [])
              running (atom 0)
              worker (fn [i]
                       (workspace-lock/call-with-workspace-write-lock
                         (.getPath root)
                         (fn []
                           (is (= 1 (swap! running inc))
                               "two threads inside the lock at once")
                           (Thread/sleep 5)
                           (swap! order conj i)
                           (swap! running dec))))]
          (doall (pmap worker (range 6)))
          (is (= 6 (count @order)))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-080
(deftest only-a-report-the-runner-wrote-counts-as-test-evidence
  (let [root (temp-dir)
        patch (str "--- a/src/app/core.clj\n+++ b/src/app/core.clj\n"
                   "@@ -7,1 +7,1 @@\n"
                   "-  (update state :ticks inc))\n"
                   "+  (update state :ticks (fnil inc 0)))\n")]
    (try
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj" "(ns app.core-test)\n"))
      (testing "a command that prints a summary and writes no report"
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test
                        {:command ["/bin/bash" "-c"
                                   (str "printf 'Ran 7 tests containing 21 "
                                        "assertions.\\n0 failures, 0 errors\\n'")
                                   "{snapshot}" "{report}" "{namespaces}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch patch :mode "preview" :verify "focused"})]
          (is (false? (:verification_complete result))
              "stdout a command chose to print is not a test result")
          ;; @spec MCP-OP-ADMIT-107
          (is (= :report-file-absent (get-in result [:tests :reason]))
              "a clean exit that wrote nothing names the report it did not write")))
      (testing "a command with no {report} placeholder is refused the credit"
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test {:command ["/bin/true" "{snapshot}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch patch :mode "preview" :verify "focused"})]
          (is (= :test-command-not-report-bound
                 (get-in result [:tests :reason])))))
      (testing "a written report is evidence, and its numbers are believed"
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test
                        {:command ["/bin/bash" "-c"
                                   (str "printf '{\"app.core-test\" {:tests 4 "
                                        ":failures 0 :errors 0}}' > \"$1\"")
                                   "--" "{report}" "{snapshot}" "{namespaces}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch patch :mode "preview" :verify "focused"})]
          (is (true? (:verification_complete result)))
          (is (= :complete (:verification_status result)))
          (is (= 4 (get-in result [:tests :namespace-results
                                   "app.core-test" :tests])))))
      (testing "a report naming other namespaces is not evidence for these"
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test
                        {:command ["/bin/bash" "-c"
                                   (str "printf '{\"other.ns-test\" {:tests 9 "
                                        ":failures 0 :errors 0}}' > \"$1\"")
                                   "--" "{report}" "{snapshot}" "{namespaces}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch patch :mode "preview" :verify "focused"})]
          (is (false? (:verification_complete result)))
          (is (= :report-namespaces-do-not-match
                 (get-in result [:tests :reason])))))
      (testing "a report with failures is a blocking check"
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test
                        {:command ["/bin/bash" "-c"
                                   (str "printf '{\"app.core-test\" {:tests 4 "
                                        ":failures 2 :errors 0}}' > \"$1\"")
                                   "--" "{report}" "{snapshot}" "{namespaces}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch patch :mode "commit" :verify "focused"})]
          (is (false? (:ok result)))
          (is (= :verification-failed (:error-type result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-080
(deftest a-test-report-is-read-in-edn-json-or-junit-xml
  (is (= {"app.a-test" {:tests 3 :failures 0 :errors 0}}
         (admit/parse-test-report "{\"app.a-test\" {:tests 3 :failures 0 :errors 0}}")))
  (is (= {"app.a-test" {:tests 5 :failures 1 :errors 0}}
         (admit/parse-test-report
           "{\"app.a-test\": {\"tests\": 5, \"failures\": 1, \"errors\": 0}}")))
  (is (= {"app.a-test" {:tests 4 :failures 0 :errors 1}}
         (admit/parse-test-report
           (str "<testsuites><testsuite name=\"app.a-test\" tests=\"4\" "
                "failures=\"0\" errors=\"1\"></testsuite></testsuites>"))))
  (is (nil? (admit/parse-test-report "Ran 7 tests containing 21 assertions.")))
  (is (nil? (admit/parse-test-report ""))))

;; @spec MCP-OP-ADMIT-081
(deftest the-focused-test-profile-loads-from-the-server-or-the-repository
  (let [root (temp-dir)]
    (try
      (is (nil? (admit/resolve-focused-test {:project-root (.getPath root)}))
          "no server config and no repository file means no profile")
      (.mkdirs (io/file root ".clj-surgeon"))
      (spit (io/file root ".clj-surgeon" "focused-test.edn")
            (pr-str {:command ["repo" "{snapshot}" "{report}" "{namespaces}"]
                     :timeout-ms 1000}))
      (let [from-file (admit/resolve-focused-test {:project-root (.getPath root)})]
        (is (= ["repo" "{snapshot}" "{report}" "{namespaces}"]
               (:command from-file)))
        (is (= :repository-file (:profile-source from-file))))
      ;; @spec MCP-OP-ADMIT-110
      (let [both (admit/resolve-focused-test
                   {:project-root (.getPath root)
                    :focused-test {:command ["server" "{snapshot}"
                                             "{report}" "{namespaces}"]}})]
        (is (= "repo" (first (:command both)))
            "the tree outranks the start configuration")
        (is (= :repository-file (:profile-source both))))
      ;; @spec MCP-OP-ADMIT-110
      ;; The shape the field actually ships: the tree states its coverage and
      ;; declares no command, the server states how to run one. Whole-map
      ;; precedence in either direction loses one of the two halves.
      (spit (io/file root ".clj-surgeon" "focused-test.edn")
            (pr-str {:namespaces {"app.core" ["app.core-test"]}}))
      (let [merged (admit/resolve-focused-test
                     {:project-root (.getPath root)
                      :focused-test {:command ["server" "{snapshot}"
                                               "{report}" "{namespaces}"]
                                     :timeout-ms 4321}})]
        (is (= "server" (first (:command merged)))
            "the tree declared no command, so the server's survives")
        (is (= 4321 (:timeout-ms merged)))
        (is (= {"app.core" ["app.core-test"]} (:namespaces merged))
            "the tree's coverage statement is read, not discarded")
        (is (= :server-config (:profile-source merged)))
        (is (= :repository-file (:namespaces-source merged))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-082
(deftest the-receipt-states-which-requested-checks-produced-a-result
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      ;; @spec MCP-OP-ADMIT-105
      (testing "no requested check produced a result: refused, nothing written"
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :admit-lint-runner
                        (fn [_ _] {:ran false :ok false :status :unverified
                                   :error-type :clj-kondo-unavailable})}
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (= :unverified (:verification_status result)))
          (is (false? (:committed result)) "the write is what is at stake")
          (is (false? (:mutation_attempted result)))
          (is (false? (:ok result))
              "the caller asked for verification and did not get any")
          (is (= :verification-incomplete (:error-type result)))
          (is (= [:clj-kondo-unavailable :no-focused-test-profile]
                 (:verification_reasons result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))))
      (write-sources! root base-sources)
      ;; @spec MCP-OP-ADMIT-105
      (testing "one check ran clean and one could not: partial is not enough"
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (= :partial (:verification_status result)))
          (is (false? (:committed result)))
          (is (false? (:ok result)))
          (is (= :verification-incomplete (:error-type result)))
          (is (false? (:verification_complete result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))))
      (write-sources! root base-sources)
      (testing "verification was not requested"
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch clean-multi-file-patch
                        :mode "preview" :verify "none"})]
          (is (= :unverified (:verification_status result)))
          (is (= [:verification-not-requested] (:verification_reasons result)))
          (is (:ok result)
              "nothing was asked for, so nothing is owed -- in preview"))
        ;; @spec MCP-OP-ADMIT-126
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "none" :allow_partial true})]
          (is (false? (:ok result))
              "and allow_partial does not turn it back into a commit")
          (is (false? (:committed result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-085
(deftest a-trimmed-payload-reports-the-total-it-omitted
  (let [rows (vec (repeat 400 {:type :opaque-string-edit
                               :file "src/app/big.clj"
                               :owner "some-owner-name"
                               :message (apply str (repeat 200 \x))}))
        receipt {:ok true :hazards rows :files []}
        bounded (write-refusal/bound-public-payload receipt [:hazards :files])]
    (is (true? (:payload_truncated bounded)))
    (is (<= (write-refusal/json-bytes bounded)
            write-refusal/public-byte-budget))
    (is (= (- (count rows) (count (:hazards bounded)))
           (get-in bounded [:payload_omitted :hazards]))
        "the omitted count is cumulative across every trimming step")
    (is (< 1 (get-in bounded [:payload_omitted :hazards]))
        "this fixture needs several steps, which is the point")
    (is (pos? (:payload_omitted_bytes bounded)))
    (is (= (- (write-refusal/json-bytes receipt)
              (write-refusal/json-bytes
                (dissoc bounded :payload_truncated :payload_truncation
                        :payload_omitted :payload_omitted_bytes)))
           (:payload_omitted_bytes bounded))
        "bytes omitted are measured against the original payload")))

;; @spec MCP-OP-ADMIT-083
(deftest a-definition-is-found-under-any-wrapper-that-still-evaluates
  (let [base "(ns app.a)\n\n(defn f [x] x)\n"
        duplicate? (fn [post]
                     (some #(= :duplicate-definition (:type %))
                           (:hazards (form-identity/form-identity-delta
                                       {:file "src/app/a.clj" :pre base
                                        :post post
                                        :hunk-spans {:pre [[1 3]]
                                                     :post [[1 40]]}}))))]
    (doseq [[label wrapper]
            [["when" "(when true (defn f [x] 9))"]
             ["let" "(let [] (defn f [x] 9))"]
             ["binding" "(binding [] (defn f [x] 9))"]
             ["try" "(try (defn f [x] 9) (catch Exception _ nil))"]
             ["if" "(if true (defn f [x] 9))"]
             ["eval quote" "(eval '(defn f [x] 9))"]
             ["intern" "(intern *ns* 'f (fn [x] 9))"]
             ["nested do" "(do (do (defn f [x] 9)))"]
             ["deep" "(when true (let [] (try (defn f [x] 9))))"]]]
      (testing label
        (is (duplicate? (str base "\n" wrapper "\n")))))
    (testing "data that is never evaluated is not a definition"
      (is (not (duplicate? (str base "\n(def sample '(defn f [x] 9))\n"))))
      (is (not (duplicate? (str base "\n(comment (defn f [x] 9))\n"))))
      (is (not (duplicate? (str base "\n#_(defn f [x] 9)\n")))))
    (testing "the hazard names the wrapper it was found under"
      (let [delta (form-identity/form-identity-delta
                    {:file "src/app/a.clj" :pre base
                     :post (str base "\n(when true (defn f [x] 9))\n")
                     :hunk-spans {:pre [[1 3]] :post [[1 40]]}})
            wrapper-unit (last (filter #(= :form (:kind %))
                                       (:units (form-identity/decompose
                                                 (str base
                                                      "\n(when true (defn f [x] 9))\n")))))
            definitions (form-identity/definitions (:node wrapper-unit))]
        (is (some #(= :duplicate-definition (:type %)) (:hazards delta)))
        (is (= ["when"] (:wrapper-path (first definitions)))
            "the receipt can say where a hidden definition was found")))))

;; @spec MCP-OP-ADMIT-083
(deftest one-reader-conditional-branch-may-still-bind-a-symbol-twice
  (let [base "(ns app.a)\n\n(defn g [] 1)\n"
        duplicate? (fn [post]
                     (some #(= :duplicate-definition (:type %))
                           (:hazards (form-identity/form-identity-delta
                                       {:file "src/app/a.clj" :pre base
                                        :post post
                                        :hunk-spans {:pre [[1 3]]
                                                     :post [[1 40]]}}))))]
    (is (not (duplicate? (str base "\n#?(:clj (defn f [] :jvm)\n"
                              "   :cljs (defn f [] :js))\n")))
        "one symbol per branch is one definition; only one branch is ever live")
    (is (duplicate? (str base "\n#?(:clj (do (defn f [] 1) (defn f [] 2)))\n"))
        "two bindings inside ONE branch really are two bindings")))

;; @spec MCP-OP-ADMIT-083
(deftest a-reader-conditional-libspec-is-present-not-removed
  (let [pre (str "(ns app.a\n  (:require\n   [clojure.string :as str]\n"
                 "   [clojure.set :as set]))\n\n(defn f [] 1)\n")
        hazards (fn [post]
                  (set (map :type (:hazards (form-identity/form-identity-delta
                                              {:file "src/app/a.clj" :pre pre
                                               :post post
                                               :hunk-spans {:pre [[1 6]]
                                                            :post [[1 6]]}})))))]
    (is (empty? (hazards (str "(ns app.a\n  (:require\n   [clojure.string :as str]\n"
                              "   #?(:clj [clojure.set :as set])))\n\n(defn f [] 1)\n")))
        "moving a libspec into a reader conditional keeps it required")
    (is (contains? (hazards "(defn f [] 1)\n") :namespace-form-removed)
        "deleting the ns form loses every require at once")
    (is (contains? (hazards (str "(ns app.a\n  (:require\n"
                                 "   [clojure.string :as str]))\n\n(defn f [] 1)\n"))
                   :require-removed))))

;; @spec MCP-OP-ADMIT-083
(deftest dropping-a-referred-symbol-is-a-lost-require
  ;; @spec MCP-OP-ADMIT-117
  ;; The fixture now still USES the dropped symbol unqualified, which is the
  ;; only reading under which this is a defect: the library stays required, so
  ;; a qualified call would keep working and only a bare use breaks.
  (let [pre (str "(ns app.a\n  (:require\n"
                 "   [clojure.set :refer [union difference]]))\n\n"
                 "(defn f [a b] (union (difference a b)))\n")
        post (str "(ns app.a\n  (:require\n"
                  "   [clojure.set :refer [union]]))\n\n"
                  "(defn f [a b] (union (difference a b)))\n")
        delta (form-identity/form-identity-delta
                {:file "src/app/a.clj" :pre pre :post post
                 :hunk-spans {:pre [[1 5]] :post [[1 5]]}})
        hazard (first (filter #(= :require-removed (:type %)) (:hazards delta)))]
    (is (some? hazard))
    (is (= :refusal (:class hazard)))
    (is (= [{:library "clojure.set" :symbols ["difference"]}]
           (:referred-symbols-removed hazard))
        "the lost symbol is named, not just the library it came from")
    (is (= [{:file "src/app/a.clj" :library "clojure.set" :line 5
             :symbol "difference" :via :refer}]
           (:reference-sites hazard))
        "and so is the site that still needs it")))

;; @spec MCP-OP-ADMIT-117
(deftest dropping-an-unused-referred-symbol-is-admitted-as-a-note
  (let [pre (str "(ns app.a\n  (:require\n"
                 "   [clojure.set :refer [union difference]]))\n\n"
                 "(defn f [a b] (union a b))\n")
        post (str "(ns app.a\n  (:require\n"
                  "   [clojure.set :refer [union]]))\n\n"
                  "(defn f [a b] (union a b))\n")
        delta (form-identity/form-identity-delta
                {:file "src/app/a.clj" :pre pre :post post
                 :hunk-spans {:pre [[1 5]] :post [[1 5]]}})
        hazard (first (filter #(= :require-removed (:type %)) (:hazards delta)))]
    (is (some? hazard) "the hazard is reported, not suppressed")
    (is (= :note (:class hazard)))
    (is (str/includes? (:message hazard) "dead-refer removal"))
    (is (= [{:library "clojure.set" :symbols ["difference"]}]
           (:referred-symbols-removed hazard)))
    (is (= [] (:reference-sites hazard))))
  (testing "a use that survives only as a qualified call is not a use of the refer"
    (let [pre (str "(ns app.a\n  (:require\n"
                   "   [clojure.set :as st :refer [union difference]]))\n\n"
                   "(defn f [a b] (union (st/difference a b)))\n")
          post (str "(ns app.a\n  (:require\n"
                    "   [clojure.set :as st :refer [union]]))\n\n"
                    "(defn f [a b] (union (st/difference a b)))\n")
          hazard (hazard-of (form-identity/form-identity-delta
                              {:file "src/app/a.clj" :pre pre :post post
                               :hunk-spans {:pre [[1 5]] :post [[1 5]]}})
                            :require-removed)]
      (is (= :note (:class hazard))
          "clojure.set is still required, so st/difference keeps working"))))

;; @spec MCP-OP-ADMIT-086
(deftest the-admission-limit-is-counted-in-bytes
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [multibyte (apply str (repeat (/ admit/max-patch-bytes 2) "é"))
            patch (str clean-multi-file-patch multibyte)]
        (is (< (count patch) admit/max-patch-bytes)
            "the fixture is under the limit measured in characters")
        (is (> (admit/patch-bytes patch) admit/max-patch-bytes)
            "and over it measured in bytes, which is the meter that matters")
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch patch :mode "commit" :verify "none" :allow_partial true})]
          (is (false? (:ok result)))
          (is (= :patch-too-large (:error-type result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Red-team round three. Probes at scratchpad/redteam-admit3/x1..x5.
;; ---------------------------------------------------------------------------

(defn- script!
  [root name body]
  (let [file (io/file root name)]
    (spit file body)
    (.setExecutable file true)
    (.getPath file)))

;; @spec MCP-OP-ADMIT-089
(deftest a-runner-that-exits-nonzero-is-never-a-complete-verification
  (let [root (temp-dir)
        patch (str "--- a/src/app/core.clj\n+++ b/src/app/core.clj\n"
                   "@@ -7,1 +7,1 @@\n"
                   "-  (update state :ticks inc))\n"
                   "+  (update state :ticks (fnil inc 0)))\n")
        clean-report (str "printf '{\"app.core-test\" {:tests 5 :failures 0 "
                          ":errors 0}}' > \"$2\"\n")]
    (try
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj" "(ns app.core-test)\n"))
      (testing "a clean report from a command that exited three"
        (let [command (script! root "nonzero.sh"
                               (str "#!/bin/sh\n" clean-report "exit 3\n"))
              result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test {:command [command "{snapshot}" "{report}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch patch :mode "commit" :verify "focused"})]
          (is (false? (:verification_complete result))
              "a run that did not finish the way it meant to is not a proof")
          (is (= :partial (:verification_status result)))
          (is (= [:runner-exit-nonzero] (:verification_reasons result)))
          (is (= 3 (get-in result [:tests :runner_exit])))
          (is (= :runner-exit-nonzero (get-in result [:tests :reason])))
          ;; @spec MCP-OP-ADMIT-105
          (is (false? (:committed result))
              "a check that could not be trusted does not carry a write")
          (is (= :verification-incomplete (:error-type result)))))
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj" "(ns app.core-test)\n"))
      (testing "the same report from a command that exited zero"
        (let [command (script! root "zero.sh"
                               (str "#!/bin/sh\n" clean-report "exit 0\n"))
              result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test {:command [command "{snapshot}" "{report}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch patch :mode "commit" :verify "focused"})]
          (is (true? (:verification_complete result)))
          (is (= :complete (:verification_status result)))
          (is (= 0 (get-in result [:tests :exit])))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-090
(deftest one-definition-per-platform-is-one-definition
  (let [pre "(ns app.a)\n\n(def x 0)\n"
        duplicate? (fn [post]
                     (some #(= :duplicate-definition (:type %))
                           (:hazards (form-identity/form-identity-delta
                                       {:file "src/app/a.cljc" :pre pre
                                        :post post
                                        :hunk-spans {:pre [[1 3]]
                                                     :post [[1 40]]}}))))]
    (testing "legal cljc: one definition per platform"
      (is (not (duplicate?
                 (str "(ns app.a)\n\n"
                      "#?(:clj  (defn parse [s] (Long/parseLong s))\n"
                      "   :cljs (defn parse [s] (js/parseInt s))\n"
                      "   :default (defn parse [s] s))\n")))
          "one reader conditional carrying three disjoint branches")
      (is (not (duplicate?
                 (str "(ns app.a)\n\n"
                      "#?(:clj (defn parse [s] (Long/parseLong s)))\n"
                      "#?(:cljs (defn parse [s] (js/parseInt s)))\n")))
          "two adjacent reader conditionals with disjoint branches")
      (is (not (duplicate?
                 (str "(ns app.a)\n\n"
                      "#?(:clj (defn parse [s] 1))\n\n"
                      "(defn other [s] s)\n\n"
                      "#?(:cljs (defn parse [s] 2))\n")))
          "and the same, spread across the file"))
    (testing "two definitions one reader would both evaluate"
      (is (duplicate? (str "(ns app.a)\n\n"
                           "#?(:clj (do (defn parse [s] s)\n"
                           "            (defn parse [s] (inc s))))\n"))
          "two bindings inside a single branch")
      (is (duplicate? (str "(ns app.a)\n\n"
                           "#?(:clj (defn parse [s] 1))\n"
                           "#?(:clj (defn parse [s] 2))\n"))
          "the same platform named by two separate conditionals")
      (is (duplicate? (str "(ns app.a)\n\n"
                           "(defn parse [s] s)\n"
                           "#?(:clj (defn parse [s] (inc s)))\n"))
          "an unconditional definition plus a conditional one"))
    (testing "the hazard names the platforms it counted"
      (let [delta (form-identity/form-identity-delta
                    {:file "src/app/a.cljc" :pre pre
                     :post (str "(ns app.a)\n\n"
                                "#?(:clj (defn parse [s] 1))\n"
                                "#?(:clj (defn parse [s] 2))\n")
                     :hunk-spans {:pre [[1 3]] :post [[1 40]]}})
            hazard (first (filter #(= :duplicate-definition (:type %))
                                  (:hazards delta)))]
        (is (= [":clj"] (:platforms hazard)))
        (is (str/includes? (:message hazard) "2 times for one reader"))))
    (testing "effective-count is the rule, stated once"
      (is (= 1 (form-identity/effective-count
                 [{:name "p" :platform ":clj"} {:name "p" :platform ":cljs"}])))
      (is (= 2 (form-identity/effective-count
                 [{:name "p" :platform ":clj"} {:name "p" :platform ":clj"}])))
      (is (= 2 (form-identity/effective-count
                 [{:name "p"} {:name "p" :platform ":clj"}]))))))

;; @spec MCP-OP-ADMIT-087
(deftest every-commit-receipt-discloses-how-far-its-lock-reached
  (doseq [[label state-dir? expected]
          [["no state directory" false :process]
           ["with a state directory" true :cross-process]]]
    (testing label
      (let [root (temp-dir)]
        (try
          (write-sources! root base-sources)
          (when state-dir? (.mkdirs (io/file root ".clj-surgeon")))
          (let [result (admit/execute-request!
                         (stub-config root)
                         {:patch clean-multi-file-patch
                          :mode "commit" :verify "focused" :allow_partial true})]
            (is (true? (:committed result)))
            (is (= expected (:lock_scope result))
                "the guarantee a commit actually had is on the receipt")
            (if state-dir?
              (is (str/ends-with? (:lock_path result) "/.clj-surgeon/write.lock"))
              (is (nil? (:lock_path result))
                  "no cross-process lock means no lock path to name")))
          (finally (delete-tree! root))))))
  (testing "a preview claims no lock, because it takes none"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (.mkdirs (io/file root ".clj-surgeon"))
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch clean-multi-file-patch :verify "none"})]
          (is (= :none (:lock_scope result))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-088
(deftest a-lock-that-cannot-be-taken-is-a-typed-refusal
  (testing "the lock path is already a directory"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (.mkdirs (io/file root ".clj-surgeon" "write.lock"))
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "none" :allow_partial true})]
          (is (false? (:ok result)))
          (is (= :workspace-lock-unavailable (:error-type result))
              "not an unexplained tool failure")
          (is (false? (:committed result)))
          (is (str/includes? (:error result) "write.lock"))
          (is (some? (:next_call result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj")))))
        (finally (delete-tree! root)))))
  (testing "the state directory is not writable"
    (let [root (temp-dir)
          directory (io/file root ".clj-surgeon")]
      (try
        (write-sources! root base-sources)
        (.mkdirs directory)
        (.setWritable directory false)
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "none" :allow_partial true})]
          (is (false? (:ok result)))
          (is (= :workspace-lock-unavailable (:error-type result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj")))))
        (finally
          (.setWritable directory true)
          (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-085
(deftest the-omitted-byte-count-measures-content-not-its-own-annotations
  (let [rows (vec (repeat 900 {:type :duplicate-definition
                               :owner "a-very-long-owner-name-to-take-bytes"
                               :message (apply str (repeat 80 \z))}))
        receipt {:ok true :hazards rows :files []}
        once (write-refusal/bound-public-payload receipt [:hazards :files])
        twice (write-refusal/bound-public-payload once [:hazards :files])
        annotations [:payload_truncated :payload_truncation
                     :payload_omitted :payload_omitted_bytes]
        content (fn [value] (write-refusal/json-bytes
                              (apply dissoc value annotations)))]
    (is (= (- (content receipt) (content once))
           (:payload_omitted_bytes once))
        "the figure is a content delta, with the annotation keys excluded")
    (is (= (:payload_omitted_bytes once) (:payload_omitted_bytes twice))
        "re-bounding an already-bounded payload cannot inflate its own report")
    (is (= (:payload_omitted once) (:payload_omitted twice)))))

;; ---------------------------------------------------------------------------
;; The grammar the agents actually write. Field result from arm Z: 85 admit
;; calls, 59 refused, 32 of them with one identical message naming a grammar
;; the caller was never going to emit. A gate must sit on the caller's route
;; at the byte level or it is not on the route at all.
;; ---------------------------------------------------------------------------

(def apply-patch-single
  (str "*** Begin Patch\n"
       "*** Update File: src/app/core.clj\n"
       "@@ (defn handle-tick\n"
       "   [state]\n"
       "-  (update state :ticks inc))\n"
       "+  (update state :ticks (fnil inc 0)))\n"
       "*** End Patch\n"))

(def apply-patch-multi
  (str "*** Begin Patch\n"
       "*** Update File: src/app/core.clj\n"
       "@@\n"
       "-  (update state :ticks inc))\n"
       "+  (update state :ticks (fnil inc 0)))\n"
       "*** Update File: src/app/util.clj\n"
       "@@\n"
       "-  (max low (min high value)))\n"
       "+  (long (max low (min high value))))\n"
       "*** End Patch\n"))

;; @spec MCP-OP-ADMIT-091
(deftest both-grammars-are-detected-and-applied
  (testing "the grammar is read off the first non-blank line"
    (is (= :apply-patch (patch-apply/detect-grammar apply-patch-single)))
    (is (= :unified-diff (patch-apply/detect-grammar clean-multi-file-patch)))
    (is (= :unified-diff (patch-apply/detect-grammar
                           (str "diff --git a/x.clj b/x.clj\n"
                                "--- a/x.clj\n+++ b/x.clj\n@@ -1,1 +1,1 @@\n-a\n+b\n"))))
    (is (nil? (patch-apply/detect-grammar "just some prose"))))
  (testing "an apply_patch payload applies to the same effect as a diff"
    (let [from-v4a (patch-apply/apply-patch base-sources apply-patch-single)
          from-diff (patch-apply/apply-patch base-sources clean-multi-file-patch)]
      (is (:ok from-v4a))
      (is (= :apply-patch (:grammar (patch-apply/parse-patch apply-patch-single))))
      (is (= (:post (first (:files from-diff)))
             (:post (first (:files from-v4a))))
          "the same edit, written either way, produces the same bytes")))
  (testing "a multi-file apply_patch payload"
    (let [applied (patch-apply/apply-patch base-sources apply-patch-multi)]
      (is (:ok applied))
      (is (= ["src/app/core.clj" "src/app/util.clj"]
             (mapv :file (:files applied))))
      (is (str/includes? (:post (first (:files applied))) "(fnil inc 0)"))
      (is (str/includes? (:post (second (:files applied))) "(long (max low")))))

;; @spec MCP-OP-ADMIT-091
(deftest a-v4a-hunk-is-located-by-content-not-by-line-number
  (let [shifted (str ";; a header comment nobody mentioned\n"
                     ";; and another\n"
                     ";; and a third\n"
                     core-source)
        applied (patch-apply/apply-patch {"src/app/core.clj" shifted}
                                         apply-patch-single)]
    (is (:ok applied) "no line numbers means nothing to be wrong about")
    (is (str/includes? (:post (first (:files applied))) "(fnil inc 0)"))
    (is (str/starts-with? (:post (first (:files applied)))
                          ";; a header comment nobody mentioned"))
    (is (= {:pre [[10 10]] :post [[10 10]]}
           (:hunk-spans (first (:files applied))))
        "spans are still real line numbers, so drift and binding work"))
  (testing "the @@ text is a hint that disambiguates, not a requirement"
    (let [twice (str "(ns app.d)\n\n"
                     "(defn alpha\n  [s]\n  (inc s))\n\n"
                     "(defn beta\n  [s]\n  (inc s))\n")
          patch (str "*** Begin Patch\n*** Update File: src/app/d.clj\n"
                     "@@ (defn beta\n"
                     "-  (inc s))\n"
                     "+  (dec s))\n"
                     "*** End Patch\n")
          applied (patch-apply/apply-patch {"src/app/d.clj" twice} patch)]
      (is (:ok applied))
      (is (str/includes? (:post (first (:files applied)))
                         "(defn beta\n  [s]\n  (dec s))")
          "the anchor selected the second occurrence")
      (is (str/includes? (:post (first (:files applied)))
                         "(defn alpha\n  [s]\n  (inc s))")
          "and left the first alone"))
    (let [source (str "(ns app.e)\n\n(defn alpha\n  [s]\n  (inc s))\n")
          wrong-anchor (str "*** Begin Patch\n*** Update File: src/app/e.clj\n"
                            "@@ (defn something-that-is-not-there\n"
                            "-  (inc s))\n"
                            "+  (dec s))\n"
                            "*** End Patch\n")
          applied (patch-apply/apply-patch {"src/app/e.clj" source}
                                           wrong-anchor)]
      (is (:ok applied)
          "a hint the author got wrong must not refuse a patch that applies"))))

;; @spec MCP-OP-ADMIT-091
(deftest every-whole-file-construct-parses-to-its-operation
  (doseq [[label patch expected]
          [["Add File"
            (str "*** Begin Patch\n*** Add File: src/app/new.clj\n"
                 "+(ns app.new)\n*** End Patch\n")
            :add]
           ["Delete File"
            (str "*** Begin Patch\n*** Delete File: src/app/util.clj\n"
                 "*** End Patch\n")
            :delete]
           ["Move to"
            (str "*** Begin Patch\n*** Update File: src/app/util.clj\n"
                 "*** Move to: src/app/moved.clj\n@@\n"
                 "-  (max low (min high value)))\n"
                 "+  (long (max low (min high value))))\n"
                 "*** End Patch\n")
            :move]]]
    (testing label
      (let [parsed (patch-apply/parse-patch patch)]
        (is (:ok parsed))
        (is (= expected (:operation (first (:files parsed)))))))))

;; @spec MCP-OP-ADMIT-093
(deftest an-unparseable-payload-names-the-grammars-it-tried
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (testing "prose, or a diff body with no headers at all"
        (doseq [patch ["please change inc to dec in handle-tick"
                       "@@ -1,1 +1,1 @@\n-a\n+b\n"]]
          (let [result (admit/execute-request!
                         (stub-config root)
                         {:patch patch :verify "none"})]
            (is (false? (:ok result)))
            (is (= :invalid-patch (:error-type result)))
            (is (= [:apply-patch :unified-diff] (:grammars-tried result)))
            (is (= (str/trim (first (str/split-lines patch)))
                   (str/trim (str (:offending-line result))))
                "the refusal quotes the line that stopped it")
            (is (str/includes? (:error result) "neither accepted grammar"))
            (is (= patch-apply/expected-headers
                   (get-in result [:next_call :expected_headers]))
                "and the follow-up shows what a first line must look like"))))
      (testing "a malformed apply_patch payload"
        (let [patch (str "*** Begin Patch\n"
                         "*** Rewrite File: src/app/core.clj\n"
                         "-a\n+b\n*** End Patch\n")
              result (admit/execute-request!
                       (stub-config root)
                       {:patch patch :verify "none"})]
          (is (false? (:ok result)))
          (is (= :invalid-patch (:error-type result)))
          (is (= :apply-patch (:grammar result))
              "the grammar was recognised; the directive was not")
          (is (= "*** Rewrite File: src/app/core.clj"
                 (:offending-line result)))))
      (testing "an apply_patch body line outside any hunk"
        (let [patch (str "*** Begin Patch\n"
                         "*** Update File: src/app/core.clj\n"
                         "-  (update state :ticks inc))\n"
                         "*** End Patch\n")
              result (admit/execute-request!
                       (stub-config root)
                       {:patch patch :verify "none"})]
          (is (false? (:ok result)))
          (is (= :invalid-patch (:error-type result)))
          (is (str/includes? (:error result) "outside any hunk"))))
      (is (= core-source (slurp (io/file root "src/app/core.clj"))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-092
(deftest the-body-delimits-a-hunk-and-the-declared-counts-are-advisory
  (let [source "(ns app.a)\n\n(defn one\n  [s]\n  (inc s))\n\n(defn two [s] s)\n"]
    (testing "a header that undercounts still applies its whole body"
      ;; The old parser stopped at the declared count, applied the truncated
      ;; hunk and dropped the rest, leaving an owner cut off mid-form. That
      ;; was the self-inflicted unreadable post image.
      (let [miscounted (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                            "@@ -3,1 +3,0 @@\n"
                            "-(defn one\n"
                            "-  [s]\n"
                            "-  (inc s))\n")
            applied (patch-apply/apply-patch {"src/app/a.clj" source} miscounted)
            image (first (:files applied))]
        (is (:ok applied))
        (is (= "(ns app.a)\n\n\n(defn two [s] s)\n" (:post image))
            "the whole owner goes, which is what the body says")
        ))
    (testing "a header that overcounts stops at the next hunk"
      ;; 19 of 77 field payloads declared more lines than they carried; the
      ;; count-terminated parser ran into the next @@ and refused them all.
      (let [overcounted (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                             "@@ -3,9 +3,9 @@\n"
                             "-(defn one\n"
                             "+(defn one!\n"
                             "@@ -7,9 +7,9 @@\n"
                             "-(defn two [s] s)\n"
                             "+(defn two [s] (inc s))\n")
            applied (patch-apply/apply-patch {"src/app/a.clj" source} overcounted)]
        (is (:ok applied))
        (is (str/includes? (:post (first (:files applied))) "(defn one!"))
        (is (str/includes? (:post (first (:files applied)))
                           "(defn two [s] (inc s))"))))
    (testing "a hunk with an empty body is still a refusal"
      (let [empty-hunk (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                            "@@ -3,1 +3,1 @@\n"
                            "@@ -5,1 +5,1 @@\n-  (inc s))\n+  (dec s))\n")
            applied (patch-apply/apply-patch {"src/app/a.clj" source} empty-hunk)]
        (is (false? (:ok applied)))
        (is (= :invalid-patch (:error-type applied)))))
    (testing "strict matching still refuses a body that does not belong"
      (let [wrong (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                       "@@ -3,1 +3,1 @@\n-(defn NOPE\n+(defn one!\n")]
        (is (= :patch-does-not-apply
               (:error-type (patch-apply/apply-patch {"src/app/a.clj" source}
                                                     wrong))))))))

;; @spec MCP-OP-ADMIT-098
(deftest a-workspace-absolute-header-path-names-the-file-it-means
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (testing "an absolute path inside the workspace is normalised"
        ;; Ten field payloads wrote the worktree path into the header; every
        ;; one was the first admit call of its run, and every one was refused.
        (let [patch (str "*** Begin Patch\n*** Update File: "
                         (.getPath root) "/src/app/core.clj\n@@\n"
                         "-  (update state :ticks inc))\n"
                         "+  (update state :ticks (fnil inc 0)))\n"
                         "*** End Patch\n")
              result (admit/execute-request!
                       (stub-config root)
                       {:patch patch :mode "commit" :verify "focused" :allow_partial true})]
          (is (:ok result) (pr-str (:error result)))
          (is (= ["src/app/core.clj"] (mapv :file (:files result)))
              "the receipt names the project-relative path")
          (is (str/includes? (slurp (io/file root "src/app/core.clj"))
                             "(fnil inc 0)"))))
      (testing "an absolute path outside the workspace still refuses"
        (let [outside (temp-dir)]
          (try
            (spit (io/file outside "victim.clj") "(ns victim)\n(def x 1)\n")
            (let [patch (str "*** Begin Patch\n*** Update File: "
                             (.getPath outside) "/victim.clj\n@@\n"
                             "-(def x 1)\n+(def x 2)\n*** End Patch\n")
                  result (admit/execute-request!
                           (stub-config root)
                           {:patch patch :mode "commit" :verify "focused" :allow_partial true})]
              (is (false? (:ok result)))
              (is (= :invalid-relative-source-path (:error-type result))
                  "normalisation never widens confinement")
              (is (= "(ns victim)\n(def x 1)\n"
                     (slurp (io/file outside "victim.clj")))))
            (finally (delete-tree! outside)))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-094
(deftest a-commit-leaves-nothing-of-its-own-in-version-control
  (let [root (temp-dir)
        git (fn [& args]
              (apply shell/sh (concat ["git" "-c" "user.email=t@t"
                                       "-c" "user.name=t"]
                                      args
                                      [:dir (.getPath root)])))]
    (try
      (write-sources! root base-sources)
      (.mkdirs (io/file root ".clj-surgeon"))
      (spit (io/file root ".clj-surgeon" "focused-test.edn")
            (pr-str {:command ["x" "{snapshot}" "{report}" "{namespaces}"]}))
      ;; @spec MCP-OP-ADMIT-119
      ;; This workspace ships a profile, so `allow_partial` cannot waive its
      ;; commit -- the waiver is for a tree with no profile at all. The commit
      ;; has to earn a complete verification, which is what the stub runner
      ;; gives once the sources have suites to attribute results to.
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj" "(ns app.core-test)\n"
                                  "test/app/util_test.clj" "(ns app.util-test)\n"))
      (git "init" "-q" ".")
      (git "add" "-A")
      (git "commit" "-qm" "base")
      (is (str/blank? (:out (git "status" "--short")))
          "the fixture starts clean")
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch clean-multi-file-patch
                      :mode "commit" :verify "focused"})
            status (->> (str/split-lines (str (:out (git "status" "--short"))))
                        (remove str/blank?)
                        (map str/trim)
                        set)]
        (is (true? (:committed result)))
        (is (= :cross-process (:lock_scope result)))
        (is (= #{"M src/app/core.clj" "M src/app/util.clj"} status)
            (str "git status must show only the patched files, saw: "
                 (pr-str status))))
      (testing "the repository's own declaration stays tracked"
        (is (str/includes? (:out (git "ls-files" ".clj-surgeon/"))
                           "focused-test.edn")))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-094
(deftest snapshot-and-report-artefacts-never-touch-the-workspace
  (let [root (temp-dir)
        seen (atom nil)]
    (try
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj" "(ns app.core-test)\n"))
      (admit/execute-request!
        (stub-config root
                     {:admit-test-runner
                      (fn [_ {:keys [namespaces snapshot-root]}]
                        (reset! seen snapshot-root)
                        {:ran true
                         :namespace-results
                         (into {} (map (fn [n] [n {:tests 1 :failures 0
                                                   :errors 0}]))
                               namespaces)})})
        {:patch clean-multi-file-patch :mode "commit" :verify "focused"})
      (is (some? @seen))
      (is (not (str/starts-with? @seen (.getPath root)))
          "the snapshot venue lives outside the workspace entirely")
      (is (not (.exists (io/file @seen)))
          "and is removed when the admission ends")
      (is (= #{"src" "test"} (set (map #(.getName %) (.listFiles root))))
          "no artefact of the gate is left in the tree")
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Whole-file operations. Field result z2, rung L: the task begins by creating
;; a file, so `unsupported-patch-operation` fired in all six gate runs. The one
;; run that worked around it and then admitted the rest in a single verified
;; commit was the only gate run in either cohort that never fell back to
;; apply_patch, and it beat three of six natives.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-095
(deftest a-created-file-is-admitted-against-an-empty-pre-image
  (doseq [[label patch]
          [["apply_patch Add File"
            (str "*** Begin Patch\n*** Add File: src/app/clock.clj\n"
                 "+(ns app.clock)\n"
                 "+\n"
                 "+(defn now\n"
                 "+  []\n"
                 "+  (System/currentTimeMillis))\n"
                 "*** End Patch\n")]
           ["unified /dev/null"
            (str "--- /dev/null\n+++ b/src/app/clock.clj\n@@ -0,0 +1,5 @@\n"
                 "+(ns app.clock)\n"
                 "+\n"
                 "+(defn now\n"
                 "+  []\n"
                 "+  (System/currentTimeMillis))\n")]]]
    (testing label
      (let [root (temp-dir)]
        (try
          (write-sources! root base-sources)
          (let [preview (admit/execute-request!
                          (stub-config root)
                          {:patch patch :verify "none"})]
            (is (:ok preview))
            (is (= [:add] (mapv :operation (:files preview))))
            (is (= ["src/app/clock.clj::app.clock" "src/app/clock.clj::now"]
                   (get-in preview [:owners :added]))
                "every owner of a new file is an added owner")
            (is (= [] (get-in preview [:owners :changed])))
            (is (= 0 (:byte_drift_outside_hunks preview)))
            (is (= [] (:hazards preview)))
            (is (= "created" (:pre_image_binding preview))
                "there was no pre-image to bind to")
            (is (not (.exists (io/file root "src/app/clock.clj")))
                "preview still writes nothing"))
          (let [result (admit/execute-request!
                         (stub-config root)
                         {:patch patch :mode "commit" :verify "focused" :allow_partial true})]
            (is (:ok result))
            (is (true? (:committed result)))
            (is (str/includes? (slurp (io/file root "src/app/clock.clj"))
                               "(defn now"))
            (is (= core-source (slurp (io/file root "src/app/core.clj")))
                "and nothing else moved"))
          (finally (delete-tree! root)))))))

;; @spec MCP-OP-ADMIT-095
(deftest a-creation-is-fenced-by-the-absence-of-its-target
  (let [root (temp-dir)
        patch (str "*** Begin Patch\n*** Add File: src/app/core.clj\n"
                   "+(ns app.core)\n*** End Patch\n")]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch patch :mode "commit" :verify "none" :allow_partial true})]
        (is (false? (:ok result)))
        (is (= :target-already-exists (:error-type result)))
        (is (= core-source (slurp (io/file root "src/app/core.clj")))
            "a creation never overwrites"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-095
(deftest hazards-of-a-created-file-are-computed-on-its-post-image-alone
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (testing "a new file that defines one symbol twice"
        (let [patch (str "*** Begin Patch\n*** Add File: src/app/dup.clj\n"
                         "+(ns app.dup)\n"
                         "+\n"
                         "+(defn go [s] s)\n"
                         "+\n"
                         "+(defn go [s] (inc s))\n"
                         "*** End Patch\n")
              result (admit/execute-request!
                       (stub-config root)
                       {:patch patch :mode "commit" :verify "none" :allow_partial true})]
          (is (false? (:ok result)))
          (is (= :duplicate-definition (:error-type result)))
          (is (not (.exists (io/file root "src/app/dup.clj"))))))
      (testing "a new file that does not read"
        (let [patch (str "*** Begin Patch\n*** Add File: src/app/bad.clj\n"
                         "+(ns app.bad)\n"
                         "+(defn go [s] (inc s)\n"
                         "*** End Patch\n")
              result (admit/execute-request!
                       (stub-config root)
                       {:patch patch :mode "commit" :verify "none" :allow_partial true})]
          (is (false? (:ok result)))
          (is (= :unreadable-post-image (:error-type result)))
          (is (not (.exists (io/file root "src/app/bad.clj"))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-096
(deftest a-deleted-file-is-admitted-as-owners-removed
  (doseq [[label patch]
          [["apply_patch Delete File"
            "*** Begin Patch\n*** Delete File: src/app/util.clj\n*** End Patch\n"]
           ["unified /dev/null"
            (str "--- a/src/app/util.clj\n+++ /dev/null\n@@ -1,5 +0,0 @@\n"
                 "-(ns app.util)\n-\n-(defn clamp\n-  [value low high]\n"
                 "-  (max low (min high value)))\n")]]]
    (testing label
      (let [root (temp-dir)]
        (try
          (write-sources! root base-sources)
          (let [result (admit/execute-request!
                         (stub-config root)
                         {:patch patch :mode "commit" :verify "focused" :allow_partial true})]
            (is (:ok result) (pr-str (:error result)))
            (is (= [:delete] (mapv :operation (:files result))))
            (is (= ["src/app/util.clj::app.util" "src/app/util.clj::clamp"]
                   (get-in result [:owners :removed])))
            (is (= [] (get-in result [:owners :added])))
            (is (true? (:committed result)))
            (is (not (.exists (io/file root "src/app/util.clj"))))
            (is (= core-source (slurp (io/file root "src/app/core.clj")))))
          (finally (delete-tree! root)))))))

;; @spec MCP-OP-ADMIT-096
(deftest deleting-a-namespace-something-still-requires-refuses
  (let [root (temp-dir)
        patch "*** Begin Patch\n*** Delete File: src/app/util.clj\n*** End Patch\n"]
    (try
      (write-sources! root
                      {"src/app/util.clj" util-source
                       "src/app/caller.clj"
                       (str "(ns app.caller\n"
                            "  (:require\n"
                            "   [app.util :as util]))\n"
                            "\n(defn go [x] (util/clamp x 0 9))\n")})
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch patch :mode "commit" :verify "focused" :allow_partial true})
            hazard (hazard-of result :namespace-form-removed)]
        (is (false? (:ok result)))
        (is (= :namespace-form-removed (:error-type result)))
        (is (= :refusal (:class hazard)))
        (is (= :workspace (:scope hazard)))
        (is (= ["src/app/caller.clj"] (:dependents hazard))
            "the refusal names the caller that would stop loading")
        (is (.exists (io/file root "src/app/util.clj"))))
      (finally (delete-tree! root))))
  (testing "a namespace nothing requires deletes cleanly"
    (let [root (temp-dir)]
      (try
        (write-sources! root {"src/app/util.clj" util-source
                              "src/app/core.clj" core-source})
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch "*** Begin Patch\n*** Delete File: src/app/util.clj\n*** End Patch\n"
                        :mode "commit" :verify "focused" :allow_partial true})]
          (is (:ok result))
          (is (empty? (:hazards result)))
          (is (not (.exists (io/file root "src/app/util.clj")))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-097
(deftest a-move-is-a-rename-and-an-edit-in-one-transaction
  (let [root (temp-dir)
        patch (str "*** Begin Patch\n"
                   "*** Update File: src/app/util.clj\n"
                   "*** Move to: src/app/numbers.clj\n"
                   "@@\n"
                   "-  (max low (min high value)))\n"
                   "+  (long (max low (min high value))))\n"
                   "*** End Patch\n")]
    (try
      (write-sources! root base-sources)
      (let [preview (admit/execute-request!
                      (stub-config root)
                      {:patch patch :verify "none"})]
        (is (:ok preview))
        (is (= [:move] (mapv :operation (:files preview))))
        (is (= "src/app/numbers.clj" (:move_to (first (:files preview)))))
        (is (= ["src/app/util.clj::clamp"] (get-in preview [:owners :changed]))
            "a move still reports the edit it carries"))
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch patch :mode "commit" :verify "focused" :allow_partial true})]
        (is (:ok result))
        (is (true? (:committed result)))
        (is (not (.exists (io/file root "src/app/util.clj")))
            "the source is gone")
        (is (str/includes? (slurp (io/file root "src/app/numbers.clj"))
                           "(long (max low"))
        (is (= core-source (slurp (io/file root "src/app/core.clj")))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-095
;; @spec MCP-OP-ADMIT-096
(deftest one-payload-may-create-update-and-delete-together
  (let [root (temp-dir)
        patch (str "*** Begin Patch\n"
                   "*** Add File: src/app/clock.clj\n"
                   "+(ns app.clock)\n"
                   "+\n"
                   "+(defn now [] 1)\n"
                   "*** Update File: src/app/core.clj\n"
                   "@@\n"
                   "-  (update state :ticks inc))\n"
                   "+  (update state :ticks (fnil inc 0)))\n"
                   "*** Delete File: src/app/util.clj\n"
                   "*** End Patch\n")]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch patch :mode "commit" :verify "focused" :allow_partial true})]
        (is (:ok result) (pr-str (:error result)))
        (is (true? (:committed result)))
        (is (= [:add :update :delete] (mapv :operation (:files result))))
        (is (= "unbound" (:pre_image_binding result))
            "a mixed payload still has pre-images to bind")
        (is (= ["src/app/clock.clj::app.clock" "src/app/clock.clj::now"]
               (get-in result [:owners :added])))
        (is (= ["src/app/core.clj::handle-tick"]
               (get-in result [:owners :changed])))
        (is (= ["src/app/util.clj::app.util" "src/app/util.clj::clamp"]
               (get-in result [:owners :removed])))
        (is (.exists (io/file root "src/app/clock.clj")))
        (is (not (.exists (io/file root "src/app/util.clj"))))
        (is (str/includes? (slurp (io/file root "src/app/core.clj"))
                           "(fnil inc 0)")))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Round four. The reader's leniency was a silent-truncation engine: a body
;; line it could not classify ended the hunk, the rest of the lines fell
;; through the top-level loop unnoticed, and the gate committed a fraction of
;; the requested edit with ok true.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-100
(deftest an-unclassifiable-body-line-refuses-and-never-truncates
  (let [source "(ns app.a)\n\n(defn f [] 1)\n(defn g [] 2)\n(defn h [] 3)\n"]
    (testing "M1: a context line that lost its leading space"
      (let [root (temp-dir)]
        (try
          (write-sources! root {"src/app/a.clj" source})
          (let [patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                           "@@ -3,3 +3,3 @@\n"
                           " (defn f [] 1)\n"
                           "(defn g [] 2)\n"
                           "-(defn h [] 3)\n"
                           "+(defn h [] 99)\n")
                result (admit/execute-request!
                         (stub-config root)
                         {:patch patch :mode "commit" :verify "none" :allow_partial true})]
            (is (false? (:ok result))
                "this used to commit a no-op and report success")
            (is (= :hunk-truncated (:error-type result)))
            (is (= 5 (:patch-line result)) "the refusal numbers the line")
            (is (= "(defn g [] 2)" (:offending-line result)))
            (is (false? (:committed result)))
            (is (= source (slurp (io/file root "src/app/a.clj")))))
          (finally (delete-tree! root)))))
    (testing "M3: two requested edits, one applicable"
      (let [root (temp-dir)]
        (try
          (write-sources! root {"src/app/a.clj" source})
          (let [patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                           "@@ -3,2 +3,2 @@\n"
                           "-(defn f [] 1)\n+(defn f [] 9)\n"
                           "GARBAGE\n"
                           "-(defn g [] 2)\n+(defn g [] 8)\n")
                result (admit/execute-request!
                         (stub-config root)
                         {:patch patch :mode "commit" :verify "none" :allow_partial true})]
            (is (false? (:ok result))
                "half a change is not a success")
            (is (= :hunk-truncated (:error-type result)))
            (is (= source (slurp (io/file root "src/app/a.clj")))))
          (finally (delete-tree! root)))))
    (testing "a body-marked line belonging to no hunk"
      (let [patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                       "@@ -3,1 +3,1 @@\n-(defn f [] 1)\n+(defn f [] 9)\n"
                       "diff \n"
                       "-(defn g [] 2)\n")
            applied (patch-apply/apply-patch {"src/app/a.clj" source} patch)]
        (is (false? (:ok applied)))
        (is (= :hunk-truncated (:error-type applied)))))))

;; @spec MCP-OP-ADMIT-100
(deftest a-removed-line-that-renders-as-a-file-header-is-still-a-removed-line
  ;; M2: the author deletes three lines, one of whose text begins "-- ", so it
  ;; renders as "--- ". Reading that as a file header ended the hunk and the
  ;; gate deleted one line instead of three.
  (let [source (str "(ns app.a)\n\n(defn doc []\n  \"Notes\n"
                    "-- deprecated, see b\n   more\"\n  1)\n")
        patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                   "@@ -3,5 +3,2 @@\n"
                   " (defn doc []\n"
                   "-  \"Notes\n"
                   "--- deprecated, see b\n"
                   "-   more\"\n"
                   "+  \"Notes\"\n"
                   "   1)\n")
        parsed (patch-apply/parse-patch patch)
        hunk (first (:hunks (first (:files parsed))))
        applied (patch-apply/apply-patch {"src/app/a.clj" source} patch)]
    (is (:ok parsed))
    (is (= 6 (count (:body hunk))) "every body line is read")
    (is (= ["  \"Notes" "-- deprecated, see b" "   more\""]
           (mapv second (filter #(= :remove (first %)) (:body hunk))))
        "all three removals, including the one that looks like a header")
    (is (:ok applied))
    (is (= "(ns app.a)\n\n(defn doc []\n  \"Notes\"\n  1)\n"
           (:post (first (:files applied)))))
    (testing "a genuine file-header pair is still a header"
      (let [two-file (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                          "@@ -3,1 +3,1 @@\n-(defn f [] 1)\n+(defn f [] 9)\n"
                          "--- a/src/app/b.clj\n+++ b/src/app/b.clj\n"
                          "@@ -1,1 +1,1 @@\n-(ns app.b)\n+(ns app.b2)\n")
            parsed (patch-apply/parse-patch two-file)]
        (is (:ok parsed))
        (is (= ["src/app/a.clj" "src/app/b.clj"] (mapv :file (:files parsed))))))))

;; @spec MCP-OP-ADMIT-102
(deftest a-patch-that-changes-nothing-is-refused
  (let [root (temp-dir)
        source "(ns app.a)\n\n(defn f [] 1)\n"]
    (try
      (write-sources! root {"src/app/a.clj" source})
      (doseq [[label patch]
              [["context only"
                (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                     "@@ -3,1 +3,1 @@\n (defn f [] 1)\n")]
               ["a removal and an identical addition"
                (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                     "@@ -3,1 +3,1 @@\n-(defn f [] 1)\n+(defn f [] 1)\n")]]]
        (testing label
          (doseq [mode ["preview" "commit"]]
            (let [result (admit/execute-request!
                           (stub-config root)
                           {:patch patch :mode mode :verify "none" :allow_partial true})]
              (is (false? (:ok result))
                  "a receipt that reports success for no change is a false green")
              (is (= :no-op-patch (:error-type result)))
              (is (false? (:committed result)))
              (is (= source (slurp (io/file root "src/app/a.clj"))))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-099
(deftest exactly-one-terminating-newline-is-removed-before-parsing
  (let [source "(ns app.a)\n\n(defn f [] 1)\n\n"
        hunk (str "@@ -3,1 +3,1 @@\n-(defn f [] 1)\n+(defn f [] 9)\n")
        header "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"]
    (testing "a payload ending in one newline"
      (is (:ok (patch-apply/apply-patch {"src/app/a.clj" source}
                                        (str header hunk)))))
    (testing "a payload ending in a real blank line still applies"
      ;; 7 of 109 field payloads ended this way and were refused as
      ;; patch-does-not-apply because the blank was annexed as context.
      (is (:ok (patch-apply/apply-patch {"src/app/a.clj" source}
                                        (str header hunk "\n")))))
    (testing "a payload with no terminating newline at all"
      (is (:ok (patch-apply/apply-patch
                 {"src/app/a.clj" source}
                 (str header "@@ -3,1 +3,1 @@\n-(defn f [] 1)\n+(defn f [] 9)")))))
    (testing "a trailing blank line inside a hunk is content, not trimmed"
      (let [with-blank (str header "@@ -3,2 +3,2 @@\n"
                            "-(defn f [] 1)\n+(defn f [] 9)\n \n")
            applied (patch-apply/apply-patch {"src/app/a.clj" source} with-blank)]
        (is (:ok applied))
        (is (= "(ns app.a)\n\n(defn f [] 9)\n\n" (:post (first (:files applied)))))))))

;; @spec MCP-OP-ADMIT-101
(deftest a-v4a-context-line-for-a-blank-source-line-is-not-dropped
  (let [source "(ns app.a)\n\n(defn f [] 1)\n\n(defn g [] 2)\n"
        patch (str "*** Begin Patch\n*** Update File: src/app/a.clj\n@@\n"
                   "-(defn f [] 1)\n"
                   "+(defn f [] 9)\n"
                   " \n"
                   " (defn g [] 2)\n"
                   "*** End Patch\n")
        parsed (patch-apply/parse-patch patch)
        hunk (first (:hunks (first (:files parsed))))
        applied (patch-apply/apply-patch {"src/app/a.clj" source} patch)]
    (is (= 4 (count (:body hunk)))
        "the single-space context line is a line, not whitespace to skip")
    (is (= [:remove :add :context :context] (mapv first (:body hunk))))
    (is (:ok applied))
    (is (= "(ns app.a)\n\n(defn f [] 9)\n\n(defn g [] 2)\n"
           (:post (first (:files applied)))))))

;; @spec MCP-OP-ADMIT-100
(deftest a-bare-hunk-marker-inside-a-unified-payload-is-a-hunk
  ;; Field payloads mix the grammars inside one file section: a unified header
  ;; pair, then `@@ -n,m +n,m @@` for one hunk and a bare `@@` for the next.
  ;; The bare marker is the sibling grammar's, so it is recognised and its hunk
  ;; is located by content -- refusing it would be reading the marker of one
  ;; grammar as an unclassifiable line only because the header was the other's.
  (let [source (str "(ns app.a)\n\n(defn f [] 1)\n\n(defn g [] 2)\n"
                    "\n(defn h [] 3)\n")
        patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                   "@@ -3,1 +3,1 @@\n"
                   "-(defn f [] 1)\n+(defn f [] 9)\n"
                   "@@\n"
                   "-(defn h [] 3)\n+(defn h [] 99)\n")
        parsed (patch-apply/parse-patch patch)
        applied (patch-apply/apply-patch {"src/app/a.clj" source} patch)]
    (is (:ok parsed))
    (is (= 2 (count (:hunks (first (:files parsed))))))
    (is (= 3 (:pre-start (first (:hunks (first (:files parsed)))))))
    (is (nil? (:pre-start (second (:hunks (first (:files parsed))))))
        "the bare marker carries no arithmetic, so it is located by content")
    (is (:ok applied))
    (is (= "(ns app.a)\n\n(defn f [] 9)\n\n(defn g [] 2)\n\n(defn h [] 99)\n"
           (:post (first (:files applied)))))
    (testing "with a context anchor"
      (let [anchored (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                          "@@ (defn h\n"
                          "-(defn h [] 3)\n+(defn h [] 99)\n")]
        (is (:ok (patch-apply/apply-patch {"src/app/a.clj" source} anchored)))))))

;; ---------------------------------------------------------------------------
;; Field bytes: what `git diff` actually emits, and what the gate did with it
;; ---------------------------------------------------------------------------

(def ^:private field-diff-dir "test-fixtures/field-diffs")

(defn- field-diff
  [name]
  (slurp (io/file field-diff-dir name)))

(defn- field-pre-image
  [file]
  (slurp (io/file field-diff-dir "pre-image" file)))

(def ^:private field-diff-shape
  "The two z3 native diffs the z5 replay fed back through the gate, as real
  `git diff` bytes: file order and hunk count per file section."
  {"z3-g1-N-1-frozen.diff"
   [["src/marvin_voice_remote/channel.clj" 8]
    ["src/marvin_voice_remote/friction_ui.clj" 3]
    ["test/marvin_voice_remote/channel_test.clj" 1]
    ["test/marvin_voice_remote/friction_ui_test.clj" 4]]
   "z3-g2-N-2-frozen.diff"
   [["src/marvin_voice_remote/channel.clj" 11]
    ["src/marvin_voice_remote/friction_ui.clj" 2]
    ["test/marvin_voice_remote/channel_test.clj" 1]
    ["test/marvin_voice_remote/friction_ui_test.clj" 4]]})

;; @spec MCP-OP-ADMIT-103
(deftest git-extended-headers-survive-every-file-section
  (doseq [[name shape] (sort field-diff-shape)]
    (testing name
      (let [patch (field-diff name)
            parsed (patch-apply/parse-patch patch)]
        (is (:ok parsed)
            (str "real git bytes must parse: " (:error-type parsed) " "
                 (:error parsed)))
        (is (= (mapv first shape) (mapv :file (:files parsed))))
        (is (= (mapv second shape) (mapv #(count (:hunks %)) (:files parsed))))
        (let [sources (into {} (map (fn [[file _]] [file (field-pre-image file)]))
                            shape)
              applied (patch-apply/apply-patch sources patch)]
          (is (:ok applied)
              (str "real git bytes must apply to the real pre-image: "
                   (:error applied)))
          (is (= (count shape) (count (:files applied))))
          (doseq [image (:files applied)]
            (is (not= (:pre image) (:post image))
                (str (:file image) " must change"))))))))

;; @spec MCP-OP-ADMIT-104
(deftest a-binary-file-section-is-a-typed-refusal
  (let [patch (str "diff --git a/src/app/core.clj b/src/app/core.clj\n"
                   "index 1b8081f..c0ff307 100644\n"
                   "--- a/src/app/core.clj\n"
                   "+++ b/src/app/core.clj\n"
                   "@@ -1,1 +1,1 @@\n"
                   "-(ns app.core)\n"
                   "+(ns app.core2)\n"
                   "diff --git a/resources/logo.png b/resources/logo.png\n"
                   "index 0000000..1111111 100644\n"
                   "Binary files a/resources/logo.png and b/resources/logo.png"
                   " differ\n")
        parsed (patch-apply/parse-patch patch)]
    (is (false? (:ok parsed)))
    (is (= :binary-patch-unsupported (:error-type parsed))
        "a binary section is refused, never silently skipped")
    (is (= "resources/logo.png" (:file parsed)))))

;; ---------------------------------------------------------------------------
;; The fail-open commit
;; ---------------------------------------------------------------------------

(def ^:private core-test-sources
  (assoc base-sources "test/app/core_test.clj" "(ns app.core-test)\n"))

;; @spec MCP-OP-ADMIT-105
(deftest a-commit-refuses-unless-verification-completed
  (let [root (temp-dir)]
    (try
      (write-sources! root core-test-sources)
      (testing "a runner that produced no evidence writes nothing"
        (let [result (admit/execute-request!
                       (stub-config root
                                    {:admit-test-runner
                                     (fn [_ {:keys [namespaces]}]
                                       {:ran true :tests-run 0 :passed 0
                                        :failed 0 :skipped 0
                                        :namespaces (vec namespaces)})})
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (false? (:ok result)))
          (is (= :verification-incomplete (:error-type result)))
          (is (false? (:committed result)))
          (is (false? (:mutation_attempted result)))
          (is (true? (:source-unchanged result)))
          (is (= "preview" (get-in result [:next_call :arguments :mode])))
          (is (= :verification-incomplete
                 (get-in result [:next_call :blocked_by])))
          (is (str/includes? (:error result) "no-test-evidence"))
          (is (= core-source (slurp (io/file root "src/app/core.clj")))
              "four rung-L commits and the z5 replay landed on this line")))
      (testing "complete verification still commits"
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (:ok result))
          (is (true? (:committed result)))
          (is (true? (:mutation_attempted result)))
          (is (= :complete (:verification_status result)))
          (is (not= core-source (slurp (io/file root "src/app/core.clj"))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-106
(deftest allow-partial-waives-an-absent-profile-and-nothing-else
  (testing "no focused-test profile exists: allow_partial commits"
    (let [root (temp-dir)]
      (try
        (write-sources! root core-test-sources)
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch clean-multi-file-patch :mode "commit"
                        :verify "focused" :allow_partial true})]
          (is (:ok result))
          (is (true? (:committed result)))
          (is (= :no-focused-test-profile (get-in result [:tests :reason])))
          (is (not= core-source (slurp (io/file root "src/app/core.clj")))))
        (finally (delete-tree! root)))))
  (testing "a profile that exists and produced nothing is not waivable"
    (let [root (temp-dir)]
      (try
        (write-sources! root core-test-sources)
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test
                        {:command ["sh" "-c" "exit 3" "runner"
                                   "{snapshot}" "{report}" "{namespaces}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch clean-multi-file-patch :mode "commit"
                        :verify "focused" :allow_partial true})]
          (is (false? (:ok result)))
          (is (= :verification-incomplete (:error-type result)))
          (is (false? (:committed result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj")))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-107
(deftest a-runner-that-wrote-no-report-names-the-report-the-argv-and-its-tail
  (testing "non-zero exit with no report is a runner failure, never partial"
    (let [root (temp-dir)]
      (try
        (write-sources! root core-test-sources)
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test
                        {:command
                         ["sh" "-c"
                          (str "echo \"Could not locate bin/gate-report.clj\""
                               " 1>&2; exit 7")
                          "runner" "{snapshot}" "{report}" "{namespaces}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})
              tests (:tests result)]
          (is (false? (:ok result)))
          (is (= :verification-incomplete (:error-type result)))
          (is (false? (:committed result)))
          (is (= :unverified (:verification_status result))
              "a runner that could not run is unverified, never partial")
          (is (= :verification-runner-failed (:reason tests)))
          (is (= 7 (:runner_exit tests)))
          (is (str/includes? (str (:runner_output_tail tests))
                             "Could not locate bin/gate-report.clj"))
          (is (str/includes? (str (:report_file tests))
                             ".clj-surgeon-focused-test-report"))
          (is (some #{"runner"} (:command_argv tests)))
          (is (= (.getPath root) (:command_cwd tests)))
          (is (= core-source (slurp (io/file root "src/app/core.clj")))))
        (finally (delete-tree! root)))))
  (testing "a clean exit that wrote no report names the absent report file"
    (let [root (temp-dir)]
      (try
        (write-sources! root core-test-sources)
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test
                        {:command ["sh" "-c" "exit 0" "runner"
                                   "{snapshot}" "{report}" "{namespaces}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch clean-multi-file-patch
                        :mode "preview" :verify "focused"})
              tests (:tests result)]
          (is (= :report-file-absent (:reason tests)))
          (is (= :unverified (:verification_status result)))
          (is (str/includes? (str (:report_file tests))
                             ".clj-surgeon-focused-test-report"))
          (is (vector? (:command_argv tests))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-108
(deftest an-expect-pre-set-mismatch-names-both-file-sets
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch clean-multi-file-patch :mode "commit"
                      :verify "none"
                      :expect_pre_sha256 {"src/app/core.clj" "deadbeef"
                                          "src/app/other.clj" "cafe"}})]
        (is (false? (:ok result)))
        (is (= :invalid-admit-request (:error-type result)))
        (is (= ["src/app/core.clj" "src/app/util.clj"] (:files_touched result)))
        (is (= ["src/app/core.clj" "src/app/other.clj"] (:files_named result)))
        (is (= ["src/app/util.clj"] (:missing result)))
        (is (= ["src/app/other.clj"] (:unexpected result)))
        (is (str/includes? (:error result) "src/app/util.clj"))
        (is (str/includes? (:error result) "src/app/other.clj"))
        (is (= core-source (slurp (io/file root "src/app/core.clj")))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-091
(deftest the-z4-hunk-mismatch-was-the-patch-not-the-matcher
  ;; The z4 refusal read `Hunk 0 of src/marvin_voice_remote/reducer_session.clj
  ;; does not match the file; its first line is
  ;; "            [clojure.data.json :as json]"`. The rollout's bytes carry a
  ;; bare `@@` hunk whose first context line is that require vector on a line
  ;; of its own, indented 13 spaces. In the pre-image at ab267f9 the form is
  ;; inline after `  (:require `, and every genuine continuation is indented
  ;; 12. The context line appears nowhere in the file, at any indent, so
  ;; `git apply` refuses it too. This pins that the fix to the git extended
  ;; headers did not loosen the matcher onto bytes that genuinely disagree.
  (let [pre (str "(ns marvin-voice-remote.reducer-session\n"
                 "  (:require [clojure.data.json :as json]\n"
                 "            [clojure.string :as str]\n"
                 "            [clojure.walk :as walk]\n"
                 "            [marvin-voice-remote.reducer.core :as core]))\n")
        patch (str "*** Begin Patch\n"
                   "*** Update File: src/marvin_voice_remote/reducer_session.clj\n"
                   "@@\n"
                   "             [clojure.data.json :as json]\n"
                   "             [clojure.string :as str]\n"
                   "             [clojure.walk :as walk]\n"
                   "+            [marvin-voice-remote.clock :as clock]\n"
                   "             [marvin-voice-remote.reducer.core :as core]\n"
                   "*** End Patch\n")
        applied (patch-apply/apply-patch
                  {"src/marvin_voice_remote/reducer_session.clj" pre} patch)]
    (is (false? (:ok applied)))
    (is (= :patch-does-not-apply (:error-type applied)))
    (is (str/includes? (:error applied) "does not match the file"))
    ;; The raw patch line carries 13 spaces; the leading one is the context
    ;; marker, so the line the matcher looks for is the 12-space form -- which
    ;; is exactly the string the field refusal quoted.
    (is (= "            [clojure.data.json :as json]"
           (:offending-line applied))
        "the same bytes the z4 refusal named")
    (is (not (str/includes? pre "\n            [clojure.data.json :as json]"))
        "that line is absent from the pre-image: the form is inline")
    (is (str/includes? pre "  (:require [clojure.data.json :as json]")
        "in the file it sits on the (:require line, not on one of its own")))

;; ---------------------------------------------------------------------------
;; The repository's own coverage statement
;; ---------------------------------------------------------------------------

(defn- write-focused-profile!
  [root profile]
  (let [file (io/file root ".clj-surgeon" "focused-test.edn")]
    (.mkdirs (.getParentFile file))
    (spit file (pr-str profile))
    root))

;; @spec MCP-OP-ADMIT-109
;; @spec MCP-OP-ADMIT-110
;; @spec MCP-OP-ADMIT-113
(deftest the-repo-profile-mapping-selects-the-focused-namespaces
  (let [root (temp-dir)]
    (try
      ;; The tree says app.core is covered by app.suite-test -- NOT by the
      ;; app.core-test the path convention would derive, which also exists.
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj" "(ns app.core-test)\n"
                                  "test/app/suite_test.clj" "(ns app.suite-test)\n"))
      (write-focused-profile! root {:namespaces {"app.core" ["app.suite-test"]}})
      (let [seen (atom nil)
            result (admit/execute-request!
                     {:project-root (.getPath root)
                      ;; the server supplies only the command, as on Anvil
                      :focused-test {:command ["server" "{snapshot}" "{report}"
                                               "{namespaces}"]}
                      :admit-lint-runner (fn [_ _] {:ran true :ok true})
                      :admit-test-runner
                      (fn [_ {:keys [namespaces]}]
                        (reset! seen (vec namespaces))
                        {:ran true
                         :namespace-results
                         (into {} (map (fn [n] [n {:tests 1 :failures 0
                                                   :errors 0}]))
                               namespaces)
                         :namespaces (vec namespaces)})}
                     {:patch clean-multi-file-patch
                      :mode "preview" :verify "focused"})]
        (is (= ["app.suite-test"] @seen)
            "the mapping wins over the path convention")
        (is (= ["app.suite-test"] (get-in result [:tests :namespaces])))
        (is (= {"src/app/core.clj" ["app.suite-test"]}
               (get-in result [:tests :focused_namespaces]))
            "the receipt says which suite covered which touched file")
        (is (= :repository-file
               (get-in result [:tests :profile_source_namespaces]))
            "the coverage statement came from the tree")
        (is (= :server-config (get-in result [:tests :profile_source]))
            "the command still came from the server that was launched to run it")
        (is (= :complete (:verification_status result))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-111
(deftest a-missing-focused-namespace-is-a-typed-unverified-reason
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      ;; The tree names a suite that is not in the tree. z4's profile named
      ;; marvin-voice-remote.bridge3-new-test for channel.clj; nothing checked.
      (write-focused-profile! root
                              {:namespaces {"app.core" ["app.absent-test"]}})
      (let [result (admit/execute-request!
                     {:project-root (.getPath root)
                      :focused-test {:command ["server" "{snapshot}" "{report}"
                                               "{namespaces}"]}
                      :admit-lint-runner (fn [_ _] {:ran true :ok true})
                      :admit-test-runner
                      (fn [_ _] (throw (ex-info "runner must not be invoked"
                                                {})))}
                     {:patch clean-multi-file-patch
                      :mode "commit" :verify "focused"})
            missing (first (get-in result [:tests :missing_focused_namespaces]))]
        (is (= :focused-namespace-missing (get-in result [:tests :reason])))
        (is (= :unverified (:verification_status result))
            "a suite that cannot be found is not half a verification")
        (is (= "src/app/core.clj" (:file missing)))
        (is (= "app.absent-test" (:namespace missing)))
        (is (some #(str/includes? % "test/app/absent_test.clj")
                  (:paths_tried missing))
            "the refusal names the path it looked in")
        ;; @spec MCP-OP-ADMIT-105
        (is (false? (:ok result)))
        (is (= :verification-incomplete (:error-type result)))
        (is (false? (:committed result)))
        (is (= core-source (slurp (io/file root "src/app/core.clj")))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-109
;; @spec MCP-OP-ADMIT-112
;; @spec MCP-OP-ADMIT-113
(deftest an-unmapped-file-falls-back-to-the-path-convention
  (let [root (temp-dir)]
    (try
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj" "(ns app.core-test)\n"
                                  "test/app/util_test.clj" "(ns app.util-test)\n"))
      ;; The mapping covers core and says nothing about util.
      (write-focused-profile! root {:namespaces {"app.core" ["app.core-test"]}})
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch clean-multi-file-patch
                      :mode "preview" :verify "focused"})]
        (is (= {"src/app/core.clj" ["app.core-test"]
                "src/app/util.clj" ["app.util-test"]}
               (get-in result [:tests :focused_namespaces]))
            "mapped where mapped, convention where not")
        (is (= ["app.core-test" "app.util-test"]
               (get-in result [:tests :namespaces]))))
      (finally (delete-tree! root))))
  (testing "a touched test file is its own focused namespace"
    (let [root (temp-dir)]
      (try
        (write-sources! root (assoc base-sources
                                    "test/app/core_test.clj"
                                    (str "(ns app.core-test)\n"
                                         "(def marker 1)\n")))
        (let [patch (str "--- a/test/app/core_test.clj\n"
                         "+++ b/test/app/core_test.clj\n"
                         "@@ -1,2 +1,2 @@\n"
                         " (ns app.core-test)\n"
                         "-(def marker 1)\n"
                         "+(def marker 2)\n")
              result (admit/execute-request!
                       (stub-config root)
                       {:patch patch :mode "preview" :verify "focused"})]
          (is (= {"test/app/core_test.clj" ["app.core-test"]}
                 (get-in result [:tests :focused_namespaces]))
              "editing a suite runs that suite, not <suite>-test")
          (is (= ["app.core-test"] (get-in result [:tests :namespaces]))))
        (finally (delete-tree! root))))))

;; ---------------------------------------------------------------------------
;; Sewing: a require the extraction made dead
;; ---------------------------------------------------------------------------

(defn- require-delta
  [pre post]
  (form-identity/form-identity-delta
    {:file "src/app/a.clj" :pre pre :post post
     :hunk-spans {:pre [[1 40]] :post [[1 40]]}}))

;; @spec MCP-OP-ADMIT-114
(deftest a-dead-require-removal-is-admitted-as-a-note
  (testing "the lib is referenced nowhere in the patched image"
    (let [pre (str "(ns app.a\n  (:require\n"
                   "   [clojure.set :as set]\n"
                   "   [clojure.string :as str]))\n\n"
                   "(defn f [s] (str/upper-case s))\n")
          post (str "(ns app.a\n  (:require\n"
                    "   [clojure.string :as str]))\n\n"
                    "(defn f [s] (str/upper-case s))\n")
          hazard (hazard-of (require-delta pre post) :require-removed)]
      (is (some? hazard) "the hazard is still reported, not suppressed")
      (is (= :note (:class hazard)))
      (is (= ["clojure.set"] (:libraries hazard)))
      (is (str/includes? (:message hazard) "dead-require removal"))
      (is (= [] (:reference-sites hazard)))))
  (testing "a mention in a string or a comment is not a reference"
    (let [pre (str "(ns app.a\n  (:require\n"
                   "   [clojure.set :as set]))\n\n"
                   ";; set/union used to live here\n"
                   "(defn f [] \"set/union\")\n")
          post (str "(ns app.a)\n\n"
                    ";; set/union used to live here\n"
                    "(defn f [] \"set/union\")\n")
          hazard (hazard-of (require-delta pre post) :require-removed)]
      (is (= :note (:class hazard))
          "the check is structural; text that is not code is not a use")))
  (testing "a discarded form is not a reference"
    (let [pre (str "(ns app.a\n  (:require\n"
                   "   [clojure.set :as set]))\n\n"
                   "(defn f [] #_(set/union) 1)\n")
          post (str "(ns app.a)\n\n(defn f [] #_(set/union) 1)\n")
          hazard (hazard-of (require-delta pre post) :require-removed)]
      (is (= :note (:class hazard)))))
  (testing "end to end: the gate admits and commits the sewing patch"
    (let [root (temp-dir)
          src (str "(ns app.a\n  (:require\n"
                   "   [clojure.set :as set]\n"
                   "   [clojure.string :as str]))\n\n"
                   "(defn f [s] (str/upper-case s))\n")
          patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                     "@@ -1,4 +1,3 @@\n"
                     " (ns app.a\n"
                     "   (:require\n"
                     "-   [clojure.set :as set]\n"
                     "    [clojure.string :as str]))\n")]
      (try
        (write-sources! root {"src/app/a.clj" src})
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch patch :mode "commit" :verify "focused" :allow_partial true})]
          (is (:ok result) (str "refused: " (:error result)))
          (is (true? (:committed result)))
          (is (= 1 (count (:hazards result)))
              "the note rides along on the receipt rather than vanishing")
          (is (= "note" (name (:class (first (:hazards result))))))
          (is (not (str/includes? (slurp (io/file root "src/app/a.clj"))
                                  "clojure.set"))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-115
;; @spec MCP-OP-ADMIT-116
(deftest a-require-removal-with-a-remaining-alias-reference-is-refused-and-names-the-site
  (let [pre (str "(ns app.a\n  (:require\n"
                 "   [clojure.set :as st]))\n\n"
                 "(defn f [a b]\n"
                 "  (st/union a b))\n")
        post (str "(ns app.a)\n\n"
                  "(defn f [a b]\n"
                  "  (st/union a b))\n")
        hazard (hazard-of (require-delta pre post) :require-removed)]
    (is (= :refusal (:class hazard))
        "the alias the pre-image ns form bound is still in use")
    (is (= [{:file "src/app/a.clj" :line 4 :symbol "st/union" :via :alias}]
           (:reference-sites hazard)))
    (is (str/includes? (:message hazard) "st/union"))
    (testing "a use that exists only inside a reader-conditional branch counts"
      (let [pre (str "(ns app.a\n  (:require\n"
                     "   [clojure.set :as st]))\n\n"
                     "(defn f [a b]\n"
                     "  #?(:clj (st/union a b) :cljs a))\n")
            post (str "(ns app.a)\n\n"
                      "(defn f [a b]\n"
                      "  #?(:clj (st/union a b) :cljs a))\n")
            hazard (hazard-of (require-delta pre post) :require-removed)]
        (is (= :refusal (:class hazard)))
        (is (= [4] (mapv :line (:reference-sites hazard))))))
    (testing "the refusal's next_call names what would lift it"
      (let [root (temp-dir)
            patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                       "@@ -1,3 +1,1 @@\n"
                       "-(ns app.a\n"
                       "-  (:require\n"
                       "-   [clojure.set :as st]))\n"
                       "+(ns app.a)\n")]
        (try
          (write-sources! root {"src/app/a.clj" pre})
          (let [result (admit/execute-request!
                         (stub-config root)
                         {:patch patch :mode "commit" :verify "none" :allow_partial true})
                lift (get-in result [:next_call :lifted_by])]
            (is (false? (:ok result)))
            (is (= :require-removed (:error-type result)))
            (is (false? (:committed result)))
            (is (some? lift) "every hazard refusal says what would lift it")
            (is (str/includes? (:description lift) "reference"))
            (is (= [{:file "src/app/a.clj" :line 4 :symbol "st/union"
                     :via :alias}]
                   (:sites lift))
                "file and line, so the caller can go and fix it")
            (is (= pre (slurp (io/file root "src/app/a.clj")))))
          (finally (delete-tree! root)))))))

;; @spec MCP-OP-ADMIT-115
(deftest a-require-removal-with-a-remaining-refer-use-is-refused
  (let [pre (str "(ns app.a\n  (:require\n"
                 "   [clojure.set :refer [union]]))\n\n"
                 "(defn f [a b]\n"
                 "  (union a b))\n")
        post (str "(ns app.a)\n\n"
                  "(defn f [a b]\n"
                  "  (union a b))\n")
        hazard (hazard-of (require-delta pre post) :require-removed)]
    (is (= :refusal (:class hazard))
        "a referred symbol still used unqualified is still a reference")
    (is (= [{:file "src/app/a.clj" :line 4 :symbol "union" :via :refer}]
           (:reference-sites hazard))))
  (testing "a fully qualified use keeps the refusal"
    (let [pre (str "(ns app.a\n  (:require\n"
                   "   [clojure.set :as st]))\n\n"
                   "(defn f [a b] (clojure.set/union a b))\n")
          post (str "(ns app.a)\n\n(defn f [a b] (clojure.set/union a b))\n")
          hazard (hazard-of (require-delta pre post) :require-removed)]
      (is (= :refusal (:class hazard)))
      (is (= [:fully-qualified] (mapv :via (:reference-sites hazard)))))))

;; ---------------------------------------------------------------------------
;; Rung L: the ladder out of the gate
;; ---------------------------------------------------------------------------

(def ^:private z8-absolute-prefix "/home/tester/acid/wt/z8-g1-Z-0/")

(defn- z8-patch
  "One of the three rung-L patches that committed at a non-complete status.

  Verbatim cohort bytes but for the absolute path prefix, which is re-homed
  onto this test's workspace root; the gate relativizes any absolute path that
  lies inside its root, so the grammar, hunks and content the gate sees are the
  cohort's own."
  [root]
  (str/replace (slurp (io/file field-diff-dir
                               "z8-commit-z8-g1-Z-0-67.patch"))
               z8-absolute-prefix
               (str (.getPath root) "/")))

;; @spec MCP-OP-ADMIT-120
(deftest a-partial-status-from-rung-l-cannot-commit
  (testing "the field call, verbatim: mode commit, verify none"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch (z8-patch root)
                        :mode "commit" :verify "none"})]
          (is (false? (:ok result))
              "z8 committed this exact call at verification_status unverified")
          (is (false? (:committed result)))
          (is (false? (:mutation_attempted result)))
          (is (= :verification-incomplete (:error-type result)))
          (is (= :unverified (:verification_status result)))
          (is (not (.exists (io/file root "src/marvin_voice_remote/clock.clj")))
              "the file the cohort wrote must not appear"))
        (finally (delete-tree! root)))))
  (testing "the refusal proposes the verify that could lift it"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch (z8-patch root)
                        :mode "commit" :verify "none"})]
          (is (= "focused" (get-in result [:next_call :arguments :verify]))))
        (finally (delete-tree! root)))))
  (testing "a focused run that comes back partial cannot commit either"
    ;; clock.clj is deliberately unmapped in the rung-L profile, so the focused
    ;; selection is empty and the status is partial on no-mapped-test-namespace
    ;; -- the reason 13 of z8's 17 refusals carried.
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :focused-test {:command ["sh" "-c" "exit 0" "runner"
                                                 "{snapshot}" "{report}"
                                                 "{namespaces}"]}
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch (z8-patch root)
                        :mode "commit" :verify "focused"})]
          (is (= :partial (:verification_status result)))
          (is (false? (:committed result)))
          (is (= :verification-incomplete (:error-type result)))
          (is (not (.exists (io/file root "src/marvin_voice_remote/clock.clj")))))
        (finally (delete-tree! root)))))
  (testing "allow_partial does not rescue it while a profile exists"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (write-focused-profile! root {:command ["sh" "-c" "exit 0" "runner"
                                                "{snapshot}" "{report}"
                                                "{namespaces}"]})
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch (z8-patch root) :mode "commit"
                        :verify "focused" :allow_partial true})]
          (is (false? (:committed result)))
          (is (= :verification-incomplete (:error-type result))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-118
;; @spec MCP-OP-ADMIT-119
;; @spec MCP-OP-ADMIT-120
(deftest no-commit-is-possible-at-any-non-complete-status
  (let [statuses [:partial :unverified]
        ;; Every reason the runner, the evidence check, the namespace plan or
        ;; the profile resolver can put on `[:tests :reason]`, plus nil.
        reasons [nil
                 :no-focused-test-profile
                 :focused-test-profile-has-no-command
                 :test-command-not-snapshot-bound
                 :test-command-not-report-bound
                 :no-mapped-test-namespace
                 :no-snapshot-venue
                 :focused-namespace-missing
                 :verification-runner-failed
                 :report-file-absent
                 :unreadable-test-report
                 :report-namespaces-do-not-match
                 :no-test-evidence
                 :tests-not-run
                 :tests-failed
                 :runner-exit-nonzero
                 :verification-not-requested]]
    (testing "no status short of complete may commit, whatever the reason"
      (doseq [status statuses
              reason reasons
              verify ["focused" "none"]
              allow? [true false]
              absent? [true false]
              ;; @spec MCP-OP-ADMIT-126
              lint [{:ran true :ok true} {:ran true :ok false}
                    {:ran false :ok false} nil]]
        (let [verification {:verification_status status
                            :verification_reasons (if reason [reason] [])
                            :lint_delta lint
                            :tests (cond-> {:profile_absent absent?}
                                     reason (assoc :reason reason))}
              refusal (admit/incomplete-commit-refusal-reason
                        verification verify allow?)
              ;; @spec MCP-OP-ADMIT-126
              ;; The waiver buys the ONE honest case: a tree with no focused
              ;; profile, where the analyzer did answer, at status partial,
              ;; under verify focused. Anything else is zero detectors and a
              ;; caller's flag.
              analyzer-read? (and (true? (:ran lint))
                                  (not (false? (:ok lint))))
              waived? (and allow? absent? analyzer-read?
                           (= :partial status)
                           (= "focused" verify))]
          (if waived?
            (is (nil? refusal)
                (str "the one waiver: " status " " reason " lint=" (pr-str lint)))
            (is (some? refusal)
                (str "must refuse: status=" status " reason=" reason
                     " verify=" verify " allow_partial=" allow?
                     " profile_absent=" absent? " lint=" (pr-str lint)))))))
    (testing "complete always commits"
      (doseq [verify ["focused" "none"]
              allow? [true false]
              absent? [true false]]
        (is (nil? (admit/incomplete-commit-refusal-reason
                    {:verification_status :complete
                     :tests {:profile_absent absent?}}
                    verify allow?)))))))

;; @spec MCP-OP-ADMIT-126
(deftest allow-partial-waives-only-a-half-verification-that-happened
  (let [dead-analyzer
        {:admit-lint-runner (fn [_ _] {:ran false :ok false
                                       :status :unverified
                                       :error-type :clj-kondo-unavailable
                                       :error (str "clj-kondo did not produce"
                                                   " readable findings")})}
        live-analyzer {:admit-lint-runner (fn [_ _] {:ran true :ok true})}
        drive (fn [config params]
                ;; Through the production path, to a real tree, and the write
                ;; is read back off disk rather than believed from a field.
                (let [root (temp-dir)]
                  (try
                    (write-sources! root core-test-sources)
                    (let [result (admit/execute-request!
                                   (merge {:project-root (.getPath root)}
                                          config)
                                   (merge {:patch clean-multi-file-patch
                                           :mode "commit"}
                                          params))]
                      (assoc (select-keys result
                                          [:ok :committed :error-type
                                           :verification_status
                                           :detectors_not_run])
                             :wrote?
                             (not= core-source
                                   (slurp (io/file root "src/app/core.clj")))))
                    (finally (delete-tree! root)))))]
    (testing "W1 dead analyzer, no profile: zero detectors ran, nothing waived"
      (let [result (drive dead-analyzer {:verify "focused"
                                         :allow_partial true})]
        (is (false? (:wrote? result))
            (str "allow_partial wrote the workspace with the analyzer dead "
                 "and the suite absent -- the state MCP-OP-ADMIT-124 exists "
                 "to name, waived by a gate that never read the word"))
        (is (false? (:ok result)))
        (is (false? (:committed result)))
        (is (= :verification-incomplete (:error-type result)))
        (is (= :unverified (:verification_status result)))))
    (testing "W2 verify none plus allow_partial is rung L one rung over"
      (let [result (drive live-analyzer {:verify "none" :allow_partial true})]
        (is (false? (:wrote? result))
            (str "a gate a caller can turn off is a caller's gate; "
                 "MCP-OP-ADMIT-120 closed verify none and allow_partial "
                 "re-opened it on every repository without a profile"))
        (is (false? (:ok result)))
        (is (false? (:committed result)))
        (is (= :unverified (:verification_status result)))))
    (testing "W3 verify none without the flag still refuses, unchanged"
      (let [result (drive live-analyzer {:verify "none"})]
        (is (false? (:wrote? result)))
        (is (false? (:ok result)))))
    (testing "W4 live analyzer, genuinely absent profile: still commits"
      (let [result (drive live-analyzer {:verify "focused"
                                         :allow_partial true})]
        (is (true? (:ok result)))
        (is (true? (:committed result)))
        (is (true? (:wrote? result))
            "the one honest case the waiver was written for must survive")
        (is (= :partial (:verification_status result)))
        (is (= [{:detector "focused-tests" :reason :no-focused-test-profile}]
               (:detectors_not_run result))
            "and the receipt still names the half that did not run")))))

;; @spec MCP-OP-ADMIT-118
(deftest a-profile-that-names-no-command-is-not-an-absent-profile
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (write-focused-profile! root {:namespaces {"app.core" ["app.core-test"]}})
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj" "(ns app.core-test)\n"))
      (let [result (admit/execute-request!
                     {:project-root (.getPath root)
                      :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                     {:patch clean-multi-file-patch :mode "commit"
                      :verify "focused" :allow_partial true})]
        (is (= :focused-test-profile-has-no-command
               (get-in result [:tests :reason]))
            "a tree that ships a profile and no command is its own state")
        (is (false? (get-in result [:tests :profile_absent])))
        (is (= :unverified (:verification_status result)))
        (is (false? (:committed result))
            "allow_partial is written for an absent profile, not a broken one")
        (is (= :verification-incomplete (:error-type result))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-114
(deftest the-z7-sewing-patch-the-gate-refused-now-admits
  ;; Cohort z7 (rung R3) refused 13 of 14 gate calls with `require-removed`.
  ;; This is one of the two payloads recoverable from those rollouts, verbatim
  ;; but for the absolute path prefix: an extraction that lifts the exact
  ;; verification profile into its own namespace and sews the requires shut.
  (let [root (temp-dir)
        pre-image (io/file field-diff-dir "z7-pre-image")
        files ["src/clj_surgeon/mcp_change_buffer.clj"
               "src/clj_surgeon/mcp_formatter.clj"
               "src/clj_surgeon/mcp_tool.clj"
               "test/clj_surgeon/mcp_change_buffer_test.clj"]]
    (try
      (doseq [f files]
        (let [target (io/file root f)]
          (.mkdirs (.getParentFile target))
          (io/copy (io/file pre-image f) target)))
      (let [patch (str/replace (slurp (io/file field-diff-dir
                                               "z7-g2-Z-2-sewing.patch"))
                               #"/home/tester/acid/wt/z7-g\d-Z-\d/"
                               (str (.getPath root) "/"))
            result (admit/execute-request!
                     {:project-root (.getPath root)
                      :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                     {:patch patch :mode "preview" :verify "none"})
            notes (filterv #(= :require-removed (:type %)) (:hazards result))]
        (is (:ok result) (str "the field refused this: " (:error result)))
        (is (= 5 (count (:files result))))
        (is (= 2 (count notes)))
        (is (every? #(= :note (:class %)) notes)
            "both are dead-require removals the extraction's sewing made")
        (is (= #{["clj-surgeon.mcp-process"] ["clj-surgeon.mcp-change-buffer"]}
               (set (map :libraries notes))))
        (is (empty? (form-identity/refusal-hazards (:hazards result)))
            "nothing in this patch is refusal-class any more"))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; The analyzer read ceiling
;; ---------------------------------------------------------------------------

(defn- analyzer-heavy-images
  "One image set whose clj-kondo findings exceed the shipped 12,000-byte read
  cap, in the shape the field replay met: many small namespaces, each carrying
  several findings the analyzer names.

  Pre and post are identical, so a runner that reads the output in full must
  publish a delta of zero over a positive baseline; a runner that cannot read
  it must say so in a type of its own."
  [file-count]
  (mapv (fn [index]
          (let [tag (format "%02d" index)
                source (str "(ns fx.ns-" tag "\n"
                            "  (:require\n"
                            "   [clojure.string :as str]\n"
                            "   [clojure.set :as set]))\n"
                            "\n"
                            "(defn widen-" tag "\n"
                            "  [value]\n"
                            "  (let [unused-alpha 1\n"
                            "        unused-beta 2\n"
                            "        unused-gamma 3]\n"
                            "    value))\n")]
            {:file (str "src/fx/ns_" tag ".clj")
             :pre source
             :post source}))
        (range file-count)))

;; @spec MCP-OP-ADMIT-121
(deftest analyzer-output-over-the-read-ceiling-is-typed-not-unavailable
  (let [root (temp-dir)
        images (analyzer-heavy-images 24)]
    (try
      (testing "a read that hit the ceiling is a failure of its own"
        (let [result (admit/default-lint-runner
                       {:project-root (.getPath root)
                        :admit-analyzer-visible-bytes 2000}
                       images)]
          (is (false? (:ran result)))
          (is (= :analyzer-output-truncated (:error-type result))
              (str "the analyzer ran and answered; the gate could not read the "
                   "answer, which is not the same fact as an absent analyzer"))
          (is (= 2000 (:cap result)) "the refusal names the ceiling it hit")
          (is (> (long (:observed-bytes result)) 2000)
              "the refusal names the size that exceeded it")
          (is (= "clj-kondo" (:detector result)))
          (is (string? (:remedy result)))
          (is (str/includes? (str (:remedy result)) "2000"))
          (is (str/includes? (str (:remedy result)) "narrow")
              "the remedy names both routes: raise the ceiling or narrow the patch")
          (is (str/includes? (str (:error result))
                             (str (:observed-bytes result))))))
      (testing "an absent analyzer keeps the type that describes it"
        (let [result (admit/default-lint-runner
                       {:project-root (.getPath root)
                        :admit-analyzer-command
                        ["clj-kondo-absent-by-design" "--lint" "{files}"]}
                       images)]
          (is (false? (:ran result)))
          (is (= :clj-kondo-unavailable (:error-type result))
              "clj-kondo-unavailable is reserved for an analyzer that did not answer")))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-122
(deftest the-analyzer-read-ceiling-is-not-the-receipt-budget
  (let [root (temp-dir)
        images (analyzer-heavy-images 24)
        lint (fn [overrides]
               (admit/default-lint-runner
                 (merge {:project-root (.getPath root)} overrides) images))]
    (try
      (let [at-receipt-budget
            (lint {:admit-analyzer-visible-bytes
                   change-buffer/exact-verification-visible-bytes})
            observed (long (:observed-bytes at-receipt-budget))]
        (testing "the fixture is the shape the field replay met"
          (is (= :analyzer-output-truncated (:error-type at-receipt-budget)))
          (is (> observed
                 (long change-buffer/exact-verification-visible-bytes))
              "a real fan-out image out-talks the receipt budget"))
        (testing "the ceiling decides, and it decides at the ceiling"
          (let [over (lint {:admit-analyzer-visible-bytes (quot observed 2)})
                under (lint {:admit-analyzer-visible-bytes (* 4 observed)})]
            (is (= :analyzer-output-truncated (:error-type over)))
            (is (> (long (:observed-bytes over)) (long (:cap over)))
                "the refusal fires because the observed size passed the ceiling it names")
            (is (true? (:ran under))
                "and clears the moment the ceiling stands above the observed size")))
        (testing "the shipped default reads the analyzer in full"
          (let [shipped (lint {})]
            (is (true? (:ran shipped))
                "the receipt budget is no longer the detector's ceiling")
            (is (true? (:ok shipped)))
            (is (pos? (long (:baseline-count shipped))))
            (is (= (:baseline-count shipped) (:future-count shipped)))
            (is (zero? (long (:introduced-count shipped))))
            (is (zero? (long (:removed-count shipped))))))
        (testing "reading more does not publish more"
          (let [shipped (lint {})]
            (is (nil? (:findings shipped))
                "the analyzer's raw findings never reach the receipt")
            (is (nil? (:output shipped)))
            (is (>= 20 (count (:introduced shipped))))
            (is (>= 20 (count (:removed shipped)))))))
      (finally (delete-tree! root)))))

(defn- egater-fixture!
  "Materialize one frozen E-GATE-R arm: its 21-file pre-image and its patch.

  These are the bytes the field replay of 2026-09-04 ran, k=1 and k=6, copied
  out of `/home/forge/tmp/arms` unchanged. On both, the shipped gate reported
  `clj-kondo-unavailable` while clj-kondo was installed and answering."
  [root shape]
  (let [pre-image (io/file field-diff-dir (str "egater-" shape "-pre-image"))]
    (doseq [file (file-seq pre-image)
            :when (.isFile file)]
      (let [relative (subs (.getPath file) (inc (count (.getPath pre-image))))
            target (io/file root relative)]
        (.mkdirs (.getParentFile target))
        (io/copy file target)))
    (slurp (io/file field-diff-dir (str "egater-" shape ".diff")))))

;; @spec MCP-OP-ADMIT-128
(defn- analyzer-precondition
  "The environment the vendored-fixture witnesses need, named, or nil.

  These two drive the REAL analyzer over frozen field images, so they can go
  red for two unrelated reasons: the fix regressed, or this box is not set
  up. Failing loud is right in both cases -- a silent skip would let a
  regression pass as an absent tool -- but the message has to say which, and
  a bare `(:ran lint)` false cannot."
  []
  (let [gate (io/file (process/clj-kondo-admission-path))]
    (cond
      (not (and (.isFile gate) (.canExecute gate)))
      (str "PRECONDITION UNMET, not a regression: the analyzer admission "
           "wrapper is not an executable file at " (.getPath gate)
           " -- run `make install-clj-kondo-admission`")

      (not (process/resolve-executable "clj-kondo"))
      (str "PRECONDITION UNMET, not a regression: clj-kondo is not "
           "resolvable on this box's PATH")

      :else nil)))

;; @spec MCP-OP-ADMIT-122
(deftest a-real-fan-out-patch-gets-a-lint-delta-that-ran
  (let [unmet (analyzer-precondition)]
    (is (nil? unmet) unmet)
    (when-not unmet
      (doseq [shape ["k1" "k6"]]
        (testing (str "E-GATE-R shape " shape)
          (let [root (temp-dir)]
            (try
              (let [patch (egater-fixture! root shape)
                    result (admit/execute-request!
                             {:project-root (.getPath root)}
                             {:patch patch :mode "preview" :verify "focused"})
                    lint (:lint_delta result)]
                (is (:ok result) (str "the gate refused this: " (:error result)))
                (is (= 21 (count (:files result))))
                (is (true? (:ran lint))
                    (str "the analyzer half never ran in the field: "
                         (pr-str (select-keys lint [:error-type :cap
                                                    :observed-bytes]))))
                (is (true? (:ok lint)))
                (is (pos? (long (:baseline-count lint))))
                (is (= (:baseline-count lint) (:future-count lint)))
                (is (zero? (long (:introduced-count lint)))
                    "the E-GATE-R finding: this caller introduced nothing")
                (is (zero? (long (:blocking-introduced-count lint))))
                (is (false? (:mutation_attempted result)))
                (is (true? (:source-unchanged result))))
              (finally (delete-tree! root)))))))))

;; @spec MCP-OP-ADMIT-122
(deftest a-denser-fan-out-image-yields-a-larger-baseline
  (let [unmet (analyzer-precondition)
        _ (is (nil? unmet) unmet)
        baselines
        (into {}
              (map (fn [shape]
                     (let [root (temp-dir)]
                       (try
                         (let [patch (egater-fixture! root shape)
                               result (admit/execute-request!
                                        {:project-root (.getPath root)}
                                        {:patch patch :mode "preview"
                                         :verify "focused"})]
                           [shape (long (get-in result [:lint_delta
                                                        :baseline-count]))])
                         (finally (delete-tree! root))))))
              ["k1" "k6"])]
    (is (or (some? unmet) (> (get baselines "k6") (get baselines "k1")))
        (str "k=6 requires more libraries per namespace than k=1, so it must "
             "produce more findings; equal counts would mean neither read was "
             "real: " (pr-str baselines)))))

;; @spec MCP-OP-ADMIT-123
;; @spec MCP-OP-ADMIT-124
(deftest a-receipt-whose-detector-did-not-run-names-it-in-text-and-structure
  (let [root (temp-dir)
        text-of (fn [result] (#'admit/summary (assoc result :elapsed_ms 1.0)))]
    (try
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj"
                                  "(ns app.core-test)\n"))
      (testing "the analyzer forced absent, through the production path"
        (let [result (admit/execute-request!
                       (-> (stub-config root)
                           (dissoc :admit-lint-runner)
                           (assoc :admit-analyzer-command
                                  ["clj-kondo-absent-by-design"
                                   "--lint" "{files}"]))
                       {:patch clean-multi-file-patch :verify "focused"})
              text (text-of result)]
          (is (= :unverified (:verification_status result)))
          (is (false? (:verification_complete result)))
          (is (= [{:detector "clj-kondo" :reason :clj-kondo-unavailable}]
                 (:detectors_not_run result))
              "the analyzer is the one detector that produced no reading")
          (is (str/includes? text "verification_status=unverified")
              "the text block carries the word, not only the boolean")
          (is (str/includes? text "did not run"))
          ;; @spec MCP-OP-ADMIT-123
          (testing "the text block names every detector and reason detectors_not_run carries"
            ;; Renamed 2026-09-04 (inb-cbca17, admit gate round 3): this
            ;; block only ever walked :detectors_not_run, which detector-note
            ;; alone already makes true -- it is not evidence that the WHOLE
            ;; text block is a superset of the structured receipt. The real
            ;; claim is asserted separately below.
            (doseq [{:keys [detector reason]} (:detectors_not_run result)]
              (is (str/includes? text detector)
                  (str "the text block never names the detector " detector))
              (is (str/includes? text (name reason))
                  (str "the text block never names the reason " (name reason)))))
          ;; @spec MCP-OP-ADMIT-132
          ;; @spec MCP-OP-ADMIT-134
          (testing "this ok=true receipt's next_call and every other leaf"
            ;; Renamed again, round four (Sol blocker 3). Round three called
            ;; this block "really is a superset of the structured receipt"
            ;; while asserting only that next_call appeared -- an overclaim
            ;; corrected by relabelling the one above it and then repeated
            ;; here. The superset claim for the ok branch is now carried by
            ;; assert-text-names-every-structured-leaf!, which walks the
            ;; receipt as JSON with no exclusions; what this block still
            ;; earns is the narrower fact that a SUCCESSFUL receipt renders
            ;; its next_call at all, which nothing did before round three.
            (is (some? (:next_call result))
                "the fixture must actually carry a next_call, or this proves nothing")
            (is (str/includes? text (json/generate-string (:next_call result)))
                (str "the text block drops next_call entirely on a successful "
                     "receipt; a caller reading only the text has no follow-up "
                     "call at all"))
            (assert-text-names-every-structured-leaf! result "detectors-not-run-preview"))
          (testing "and no field in it reads as clean"
            (is (empty? (:hazards result)))
            (is (str/includes? text "not a clean bill of health")
                (str "hazards 0 beside a silent detector is exactly the shape "
                     "a reader scores as a pass")))))
      (testing "both detectors live, and nothing is named"
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch clean-multi-file-patch :verify "focused"})
              text (text-of result)]
          (is (= :complete (:verification_status result)))
          (is (empty? (:detectors_not_run result)))
          (is (str/includes? text "verification_status=complete"))
          (is (not (str/includes? text "did not run")))))
      (testing "verify none names both detectors rather than passing over them"
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch clean-multi-file-patch :verify "none"})
              text (text-of result)]
          (is (= [{:detector "clj-kondo" :reason :verification-not-requested}
                  {:detector "focused-tests" :reason :verification-not-requested}]
                 (:detectors_not_run result)))
          (doseq [{:keys [detector reason]} (:detectors_not_run result)]
            (is (str/includes? text detector))
            (is (str/includes? text (name reason))))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; A detector is silent when it produced no reading, not when its process
;; failed to exit
;; ---------------------------------------------------------------------------

(defn- focused-runner
  "One focused-test profile command built from a shell body.

  `$0` is the runner's own name, `$1` the snapshot root, `$2` the report path
  and `$3...` the namespaces -- the argv the gate expands. These fixtures go
  through the real runner rather than stubbing it out, because the defect
  being pinned lives in what the gate concludes from a runner that exited."
  [body]
  ["sh" "-c" body "runner" "{snapshot}" "{report}" "{namespaces}"])

(defn- report-writing-runner
  "A runner that writes one report row per requested namespace, then exits."
  [tests failures trailer]
  (focused-runner
    (str "report=\"$2\"; shift 2; { printf '{'; "
         "for ns in \"$@\"; do printf '\"%s\" {:tests " tests
         " :failures " failures " :errors 0} ' \"$ns\"; done; "
         "printf '}'; } > \"$report\"" trailer)))

(def ^:private silent-focused-runner-shapes
  "The four runner shapes the round-one adversarial review reproduced.

  Every one of them exits, so `:ran` is true on all four; not one of them
  leaves the gate holding a usable reading of the suite."
  [{:label "R1 the report names namespaces nobody asked for"
    :command (focused-runner
               (str "printf '{\"other.ns-test\" {:tests 3 :failures 0"
                    " :errors 0}}' > \"$2\""))
    :reason :report-namespaces-do-not-match
    :status :partial}
   {:label "R2 the report says zero tests"
    :command (report-writing-runner 0 0 "")
    :reason :no-test-evidence
    :status :partial}
   {:label "R3 a clean report from a runner that exited three"
    :command (report-writing-runner 3 0 "; exit 3")
    :reason :runner-exit-nonzero
    :status :partial}
   {:label "R4 the runner exited three and wrote nothing"
    :command (focused-runner "exit 3")
    :reason :verification-runner-failed
    :status :unverified}])

;; @spec MCP-OP-ADMIT-125
(deftest a-runner-that-exited-without-a-reading-is-a-silent-detector
  (doseq [{:keys [label command reason status]} silent-focused-runner-shapes]
    (testing label
      (let [root (temp-dir)]
        (try
          (write-sources! root core-test-sources)
          (write-focused-profile! root {:command command :timeout-ms 60000})
          (let [result (admit/execute-request!
                         {:project-root (.getPath root)
                          :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                         {:patch clean-multi-file-patch :verify "focused"})
                text (#'admit/summary (assoc result :elapsed_ms 1.0))]
            (is (true? (:ran (:tests result)))
                (str "the runner's process exited, which is the whole reason "
                     ":ran cannot answer this question"))
            (is (= status (:verification_status result)))
            (is (= [{:detector "focused-tests" :reason reason}]
                   (:detectors_not_run result))
                (str "the focused half produced no reading and the receipt "
                     "published " (pr-str (:detectors_not_run result))))
            (testing "and the text block carries what structure names"
              (is (str/includes? text "did not run"))
              (doseq [{:keys [detector reason]} (:detectors_not_run result)]
                (is (str/includes? text detector))
                (is (str/includes? text (name reason))))
              (is (str/includes? text "not a clean bill of health")
                  (str "hazards " (count (:hazards result))
                       " beside a silent detector is the shape a reader "
                       "scores as a pass"))))
          (finally (delete-tree! root)))))))

;; @spec MCP-OP-ADMIT-125
(deftest a-check-that-ran-and-failed-is-a-reading-not-a-silent-detector
  (testing "a suite that ran and failed"
    (let [root (temp-dir)]
      (try
        (write-sources! root core-test-sources)
        (write-focused-profile! root {:command (report-writing-runner 3 1 "")
                                      :timeout-ms 60000})
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch clean-multi-file-patch :verify "focused"})
              text (#'admit/summary (assoc result :elapsed_ms 1.0))]
          (is (= :tests-failed (get-in result [:tests :reason])))
          (is (= [] (:detectors_not_run result))
              (str "a suite that ran and failed produced exactly the reading "
                   "this gate asked for, and is already blocking"))
          (is (not (str/includes? text "did not run"))))
        (finally (delete-tree! root)))))
  (testing "an analyzer that ran and introduced a blocking finding"
    (let [root (temp-dir)]
      (try
        (write-sources! root core-test-sources)
        (let [result (admit/execute-request!
                       (stub-config root
                                    {:admit-lint-runner
                                     (fn [_ _] {:ran true :ok false
                                                :introduced-count 1
                                                :blocking-introduced-count 1})})
                       {:patch clean-multi-file-patch :verify "focused"})
              text (#'admit/summary (assoc result :elapsed_ms 1.0))]
          (is (= [] (:detectors_not_run result))
              "the analyzer answered; what it answered was bad news")
          (is (not (str/includes? text "did not run"))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-125
(deftest a-refusal-that-consulted-no-detector-never-publishes-an-empty-list
  (let [root (temp-dir)
        source "(ns app.a)\n\n(defn f [] 1)\n"
        no-op-patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                         "@@ -3,1 +3,1 @@\n (defn f [] 1)\n")]
    (try
      (write-sources! root {"src/app/a.clj" source})
      (testing "a no-op patch is refused before any detector is consulted"
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch no-op-patch :verify "focused"})
              text (#'admit/summary (assoc result :elapsed_ms 1.0))]
          (is (= :no-op-patch (:error-type result)))
          (is (= [{:detector "clj-kondo" :reason :verification-not-attempted}
                  {:detector "focused-tests"
                   :reason :verification-not-attempted}]
                 (:detectors_not_run result))
              (str "[] here is the affirmative claim that every requested "
                   "detector answered, on a receipt where none was asked"))
          (doseq [{:keys [detector reason]} (:detectors_not_run result)]
            (is (str/includes? text detector))
            (is (str/includes? text (name reason))))))
      (testing "a refusal that never reached verification says nothing at all"
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch "this is not a unified diff" :verify "focused"})]
          (is (false? (:ok result)))
          (is (nil? (:detectors_not_run result))
              (str "never asked and everything answered are different facts "
                   "and must not share a value"))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; An admission failure is a fact about this deployment, not one word
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-127
(deftest every-admission-failure-type-survives-the-analyzer-boundary
  (let [cases
        [["the admission wrapper is not installed"
          {:finished? false :launch-error true
           :admission-error {:error-type :clj-kondo-admission-unavailable
                             :gate "/no/such/wrapper"}}
          :clj-kondo-admission-unavailable]
         ["clj-kondo itself is not on this server's PATH"
          {:finished? false :launch-error true
           :admission-error {:error-type :clj-kondo-executable-unavailable
                             :requested-executable "clj-kondo"}}
          :clj-kondo-executable-unavailable]
         ["the box was loaded and the analyzer was deferred"
          {:finished? true :exit 75
           :admission {:status :pressure-deferred
                       :error-type :clj-kondo-pressure-deferred}}
          :clj-kondo-pressure-deferred]
         ["the wrapper waited past its deadline for the lock"
          {:finished? true :exit 1
           :admission {:status :admission-timeout
                       :error-type :clj-kondo-admission-timeout}}
          :clj-kondo-admission-timeout]
         ["the bounded run was interrupted"
          {:finished? false
           :admission-error {:error-type :process-interrupted
                             :admission {:status :delegated}}}
          :process-interrupted]
         ["an admitted run is not an admission failure"
          {:finished? true :exit 0 :admission {:status :admitted}}
          nil]
         ["nor is a command that needs no admission"
          {:finished? true :exit 0 :admission {:status :not-required}}
          nil]]]
    (doseq [[label raw expected] cases]
      (testing label
        (is (= expected
               (:error-type (admit/analyzer-admission-failure raw))))))
    (testing "every type this can publish reads as unverifiable"
      (doseq [[label _ expected] cases
              :when expected]
        (is (contains? admit/unverifiable-lint-error-types expected)
            (str label ": a type outside the set would score as partial"))))))

;; @spec MCP-OP-ADMIT-127
(deftest an-admission-failure-keeps-its-type-through-to-the-receipt
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (testing "this server cannot find its own admission wrapper"
        (binding [process/*clj-kondo-admission-path*
                  "/nonexistent/clj-kondo-admission"]
          (let [result (admit/execute-request!
                         {:project-root (.getPath root)}
                         {:patch clean-multi-file-patch :verify "focused"})
                lint (:lint_delta result)]
            (is (= :clj-kondo-admission-unavailable (:error-type lint))
                (str "a deployment fault published as a missing analyzer: "
                     (pr-str (select-keys lint [:error-type :error]))))
            (is (= "/nonexistent/clj-kondo-admission" (:gate lint))
                "the ex-data names the exact path; so must the receipt")
            (is (string? (:remedy lint))
                "rung 1 carries :cap and :observed-bytes; this carries its own")
            (is (= :unverified (:verification_status result)))
            (is (= [{:detector "clj-kondo"
                     :reason :clj-kondo-admission-unavailable}
                    {:detector "focused-tests"
                     :reason :no-focused-test-profile}]
                   (:detectors_not_run result))))))
      (testing "clj-kondo itself is not on this server's PATH"
        ;; `expand-command` absolutizes a bare `clj-kondo` against its own
        ;; search paths before admission ever sees it, so the executable is
        ;; taken out from under the gate by naming it and emptying the PATH
        ;; the admission wrapper resolves against.
        (binding [process/*executable-path* "/nonexistent-bin"]
          (let [result (admit/execute-request!
                         {:project-root (.getPath root)
                          :admit-analyzer-command
                          ["/nonexistent/clj-kondo" "--lint" "{files}"]}
                         {:patch clean-multi-file-patch :verify "focused"})
                lint (:lint_delta result)]
            (is (= :clj-kondo-executable-unavailable (:error-type lint)))
            (is (string? (:remedy lint)))
            (is (= :unverified (:verification_status result))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-128
(deftest the-admission-wrapper-resolves-without-the-jvms-working-directory
  (let [home (temp-dir)
        classpath (->> (str/split (System/getProperty "java.class.path") #":")
                       (map #(.getCanonicalPath (io/file %)))
                       (str/join ":"))
        builder (doto (ProcessBuilder.
                        ^java.util.List
                        [(str (System/getProperty "java.home") "/bin/java")
                         "-cp" classpath
                         (str "-Duser.home=" (.getPath home))
                         "clojure.main" "-e"
                         (str "(require 'clj-surgeon.mcp-process)"
                              "(print (str \"ADMISSION-PATH=\""
                              " (clj-surgeon.mcp-process/"
                              "clj-kondo-admission-path)))")])
                  (.directory (io/file "/"))
                  (.redirectErrorStream true))
        _ (.remove (.environment builder) "CLJ_SURGEON_CLJ_KONDO_ADMISSION")
        process (.start builder)
        output (slurp (.getInputStream process))
        exit (.waitFor process)
        path (second (re-find #"ADMISSION-PATH=(\S+)" output))]
    (try
      (is (zero? exit) output)
      (is (some? path) output)
      (is (.isFile (io/file (str path)))
          (str "a workspace-routed server started outside a clj-surgeon "
               "checkout resolved its admission wrapper to " (pr-str path)
               " and would report clj-kondo-unavailable for every admit call "
               "on every workspace it routes"))
      (is (.canExecute (io/file (str path))))
      (finally (delete-tree! home)))))

;; ---------------------------------------------------------------------------
;; Nothing leaves the handler's edge without a receipt
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-129
(deftest an-error-at-the-handlers-edge-becomes-a-typed-refusal
  (let [config-atom (deref #'admit/runtime-config)
        previous @config-atom
        root (temp-dir)
        drive (fn [thrown]
                (let [captured (atom nil)]
                  (reset! config-atom
                          {:project-root (.getPath root)
                           :admit-lint-runner (fn [_ _] (throw thrown))})
                  (admit/handle-admit-clojure-patch
                    nil
                    {"patch" clean-multi-file-patch "verify" "focused"}
                    (fn [content error? result]
                      (reset! captured {:text (first content)
                                        :error? error?
                                        :result result})))
                  @captured))]
    (try
      (write-sources! root base-sources)
      (testing "an OutOfMemoryError below the read ceiling"
        ;; Constructed, never provoked: exhausting the heap inside the suite
        ;; would prove nothing this does not, and would take the suite with it.
        (let [{:keys [text error? result]}
              (drive (OutOfMemoryError. "Java heap space"))]
          (is (some? result)
              (str "an Error escaped the handler with no receipt at all, and "
                   "a caller that receives nothing cannot tell a refusal "
                   "from a write"))
          (is (false? (:ok result)))
          (is (true? error?))
          (is (= :analyzer-memory-exhausted (:error-type result)))
          (is (pos? (long (:max_heap_mib result)))
              "the heap is the number that would lift this")
          (is (str/includes? (str (:error result)) "MiB"))
          (is (string? (:remedy result)))
          (testing "and it does not claim a safety it cannot know"
            (is (nil? (:source-unchanged result))
                (str "the gate cannot know how far an escaped Error got, and "
                     "a false 'source unchanged' terminates investigation"))
            (is (not (str/includes? (str text) "source unchanged"))))))
      (testing "any other Error is typed too, rather than escaping"
        (let [{:keys [result]} (drive (StackOverflowError.))]
          (is (some? result))
          (is (false? (:ok result)))
          (is (= :admit-tool-error (:error-type result)))))
      (testing "an ordinary Exception keeps the type it always had"
        (let [{:keys [result]} (drive (ex-info "boom" {}))]
          (is (= :admit-tool-failure (:error-type result)))
          (is (true? (:source-unchanged result))
              "an Exception is caught below, where the gate does know")))
      (finally
        (reset! config-atom previous)
        (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-130
(deftest the-admit-paths-memory-bound-is-a-target-not-an-argument
  ;; Reads the repository from the working directory, as the intent audit
  ;; witnesses in this repo already do; both are run from the repo root.
  (let [script "test/admit_analyzer_memory_selftest.clj"
        makefile (slurp "Makefile")]
    (is (.isFile (io/file script))
        (str "the memory battery's arms are corpus TREES driven through the "
             "ops registry; nothing in it drives the admit path, whose "
             "memory question is findings DIVERSITY with both parsed images "
             "live at once. The 16 MiB read ceiling bounds what is READ, not "
             "what parsing it retains, and an argument is not a receipt"))
    (is (str/includes? makefile "\nadmit-analyzer-memory-self-test:")
        "an arm nobody can run is not a receipt")
    (testing "at an explicit heap the JVM was actually given"
      (is (str/includes? makefile "ADMIT_ANALYZER_MEMORY_XMX ?= 512m"))
      (is (str/includes? makefile
                         "-J-Xmx$(ADMIT_ANALYZER_MEMORY_XMX)")
          "a heap bound the JVM never received is not a bound"))
    (testing "and never wired into a fast gate by accident"
      (is (= 2 (count (re-seq #"admit-analyzer-memory-self-test" makefile)))
          (str "exactly the .PHONY line and the target itself; a third "
               "mention means a tens-of-seconds JVM entered a gate, which "
               "is a decision, not a diff")))
    (testing "the arm reports numerically, per scale"
      (let [source (slurp script)]
        (is (str/includes? source "(def scales [100 1000 10000])"))
        (is (str/includes? source "\"PASS\" \"FAIL\""))
        (is (str/includes? source "heap-peak-MiB=%d budget-MiB=%d"))
        (is (str/includes? source "(System/exit (if (every? true? results) 0 1))")
            "a self-test that cannot fail the shell is a log line")))))

;; ---------------------------------------------------------------------------
;; MCP-OP-ADMIT-131 / MCP-OP-ADMIT-132: the refusal text is a superset of
;; structuredContent, and next_call renders verbatim.
;;
;; Landing review round 3 (inb-cbca17): admit_clojure_patch, the catalog's
;; only write tool, sat outside the trunk's text superset ratchet
;; (MCP-OP-ALIAS-059). Every refusal's `remedy` and `next_call` were absent
;; from `content[0].text`, including the two named cases: the number that
;; would lift an `analyzer-memory-exhausted` refusal, and the follow-up call
;; a `verification-incomplete` refusal itself proposes.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-131
(def ^:private known-non-kind-regex-artifacts
  "Two matches the enumeration regexes below produce that are demonstrably
  not error-type kind literals -- verified by reading the exact source line
  each comes from, not guessed:

  \"error\" -- mcp_admit_tool.clj's `stale-snapshot-refusal` builds
  `(select-keys (refusal ...) [:ok :operation :committed :source-unchanged
  :error-type :error :next_call :drifted])`; `:error-type\\s+:error` there is
  two adjacent KEYS of a key vector, not a kind assignment.

  \"else\" -- the `:else` branch keyword of the `cond` inside
  `edge-throwable-refusal`, picked up by the same keyword-token scan that
  finds that cond's three real kinds."
  #{"error" "else"})

;; @spec MCP-OP-ADMIT-131
(defn- kind-pairs
  "Literal `:error-type :kind` key/value pairs anywhere in `text`."
  [text]
  (into (sorted-set)
        (remove known-non-kind-regex-artifacts)
        (map second (re-seq #":error-type\s+:([a-z][a-z][a-z-]*)" text))))

;; @spec MCP-OP-ADMIT-131
(defn- refusal-call-kinds
  "Kinds passed as the literal first (or second, after `context`) argument
  to a `(refusal ...)` call."
  [text]
  (into (sorted-set) (map second)
        (re-seq #"\(refusal\s*(?:context\s*)?\n?\s*:([a-z][a-z][a-z-]*)" text)))

;; @spec MCP-OP-ADMIT-131
(defn- path-refusal-kinds
  "Kinds passed to `mcp-paths/path-refusal`, which `freeze-sources` widens
  into the admit gate's own `:error-type` via `(keyword (:error_type ...))`."
  [text]
  (into (sorted-set) (map second)
        (re-seq #"(?s)path-refusal\s*\n?\s*:([a-z][a-z][a-z-]*)" text)))

;; @spec MCP-OP-ADMIT-131
(defn- edge-throwable-kinds
  "The literal keywords `edge-throwable-refusal`'s own `cond` can return."
  [text]
  (let [start (str/index-of text "(defn- edge-throwable-refusal")
        end (str/index-of text "(defn handle-admit-clojure-patch")
        body (subs text start end)
        cond-at (str/index-of body "cond")
        window (subs body cond-at (min (count body) (+ cond-at 400)))]
    (into (sorted-set)
          (remove known-non-kind-regex-artifacts)
          (map second (re-seq #":([a-z][a-z][a-z-]*)" window)))))

;; @spec MCP-OP-ADMIT-131
(defn- hazard-refusal-kinds
  "Hazard `:type` values built with class `:refusal` in `form_identity.clj`
  -- these become the admit gate's own `:error-type` via
  `(:type (first blocking))` when `refusal-hazards` finds one blocking.
  A hazard built class `:note` or `:informational` never blocks, so its
  type never reaches the top-level receipt and is excluded here."
  [text]
  (letfn [(refusal-class? [tail]
            (= (re-find #":refusal|:note|:informational" tail) ":refusal"))]
    (into (sorted-set)
          (concat
            (keep (fn [[_ typ tail]] (when (refusal-class? tail) typ))
                  (re-seq #"\(hazard\s+:([a-z][a-z-]*)((?:.|\n){0,220})" text))
            (keep (fn [[_ typ tail]] (when (refusal-class? tail) typ))
                  (re-seq #":type\s+:([a-z][a-z-]*)((?:.|\n){0,220})" text))))))

;; @spec MCP-OP-ADMIT-131
(defn- commit-compiled-kinds
  "Kinds `intent-transaction/commit-compiled!` itself can return, scoped to
  that one function's body so an unrelated `:error-type` elsewhere in
  intent_transaction.clj -- reachable from other verbs, not from the admit
  gate's commit path -- is not swept in."
  [text]
  (let [start (str/index-of text "(defn commit-compiled!")
        end (str/index-of text "(defn- reverse-edit")
        body (subs text start end)]
    (into (sorted-set)
          (concat
            (kind-pairs body)
            (map second (re-seq #"refuse!\s*\n?\s*:([a-z][a-z][a-z-]*)" body))
            ;; `:error-type (if rolled-back? :a :b)` -- both branches
            (mapcat (fn [[_ a b]] [a b])
                    (re-seq (re-pattern
                              (str "\\:error-type\\s*\\(if\\s+\\S+\\s*\\n?\\s*"
                                   ":([a-z][a-z-]*)\\s*\\n?\\s*:([a-z][a-z-]*)\\)"))
                            body))))))

;; @spec MCP-OP-ADMIT-131
(defn- admit-refusal-kinds-in-source
  "Every `:error-type` value the admit gate's top-level receipt can carry,
  read from the source rather than from a maintained list.

  Closed over the exact set of namespaces whose values reach that field: the
  gate's own literal refusals and edge-of-handler classification; the two
  namespaces it calls directly and widens their `:error-type`
  (`patch-apply/parse-patch` and `patch-apply/apply-parsed`,
  `mcp-paths/resolve-source-path` and `resolve-new-source-path` via the
  gate's own `freeze-sources`); the one namespace whose hazard `:type` it
  widens when a hazard blocks (`form-identity/refusal-hazards`); and the one
  function whose `:error-type` it widens on commit
  (`intent-transaction/commit-compiled!`). A kind constructed anywhere else
  in those files, outside the functions the admit gate actually calls, is
  not reachable from `execute-request!` and is deliberately not swept in
  (commit-compiled-kinds' function-body scoping is the concrete guard)."
  []
  (let [admit (slurp "src/clj_surgeon/mcp_admit_tool.clj")
        patch-apply (slurp "src/clj_surgeon/patch_apply.clj")
        mcp-paths (slurp "src/clj_surgeon/mcp_paths.clj")
        form-identity (slurp "src/clj_surgeon/form_identity.clj")
        intent-tx (slurp "src/clj_surgeon/intent_transaction.clj")]
    (into (sorted-set)
          (concat (kind-pairs admit)
                  (refusal-call-kinds admit)
                  (edge-throwable-kinds admit)
                  (refusal-call-kinds patch-apply)
                  (path-refusal-kinds mcp-paths)
                  (hazard-refusal-kinds form-identity)
                  (commit-compiled-kinds intent-tx)))))

;; @spec MCP-OP-ADMIT-131
(deftest the-derived-refusal-kind-enumeration-is-not-empty-and-is-stable
  ;; A regression here (an empty set, or a set that lost a real kind to a
  ;; regex miss) would silently turn the sweep below into a no-op that still
  ;; reports green. Pinning the count is a tripwire on the derivation itself,
  ;; not a claim that this exact number is meaningful.
  (let [kinds (admit-refusal-kinds-in-source)]
    (is (>= (count kinds) 30)
        (str "expected at least 30 derived kinds, got " (count kinds) ": "
             (pr-str kinds)))
    (is (contains? kinds "analyzer-memory-exhausted"))
    (is (contains? kinds "verification-incomplete"))
    (is (contains? kinds "invalid-relative-source-path")
        "a kind the manual enumeration in this round's review missed, and the derivation caught")
    (is (not (contains? kinds "error")))
    (is (not (contains? kinds "else")))))

;; @spec MCP-OP-ADMIT-131
(defn- leaves-of
  "Independent recursive leaf walk over `v`, as [path value] pairs.

  Deliberately reimplemented rather than calling
  `clj-surgeon.mcp-admit-tool/admit-leaf-entries`: a bug shared by both sides
  of an equality check proves nothing."
  [path v]
  (cond
    (map? v)
    (mapcat (fn [[k cv]] (leaves-of (str path (when (seq path) ".") (name k)) cv))
            v)

    (sequential? v)
    (apply concat
           (map-indexed (fn [i cv] (leaves-of (str path "[" i "]") cv)) v))

    (nil? v) []

    :else [[path v]]))

;; @spec MCP-OP-ADMIT-131
(def ^:private admit-envelope-keys-for-witness
  "The same envelope this round's renderer excludes -- reimplemented here,
  not required from the tool namespace, so this witness does not depend on
  the implementation agreeing with itself about what its own envelope is.

  `:files` and `:hashes` are excluded for the same reason the renderer
  excludes them: per-file hunk spans and pre/post digests are diff
  metadata the caller already sent, not the cause of the refusal, and the
  digest a caller actually needs back is next_call's own
  expect_pre_sha256, checked separately below via the next_call assertion."
  #{:ok :operation :error-type :error :next_call :remedy :elapsed_ms
    :workspace-root :detectors_not_run :source-unchanged :mode
    :files :hashes})

;; @spec MCP-OP-ADMIT-131
;; @spec MCP-OP-ADMIT-132
(defn- assert-refusal-text-superset!
  "Every leaf of `structured` (a refusal receipt: :ok false) that differs
  from the closed empty-receipt baseline for its mode appears in the text
  block, `remedy` renders as its own line, and `next_call` renders verbatim
  (or a stated, bounded pointer to it)."
  [structured label]
  (let [text (#'admit/summary structured)
        baseline (#'admit/empty-receipt (or (:mode structured) "preview"))
        leaves (->> (apply dissoc structured admit-envelope-keys-for-witness)
                    (remove (fn [[k v]] (= v (get baseline k))))
                    (mapcat (fn [[k v]] (leaves-of (name k) v)))
                    (filter (fn [[_ v]] (or (string? v) (number? v)
                                            (boolean? v) (keyword? v)
                                            (symbol? v)))))]
    (is (false? (:ok structured)) (str label " · fixture is not a refusal"))
    (is (str/includes? text (let [kind (:error-type structured)]
                              (if (keyword? kind) (name kind) (str kind))))
        (str label " · the text does not name the error type"))
    (when-let [error (:error structured)]
      (is (str/includes? text error)
          (str label " · the text drops the error sentence")))
    (when-let [remedy (:remedy structured)]
      (is (str/includes? text remedy)
          (str label " · the text drops the remedy")))
    (doseq [[path v] leaves]
      (let [rendered (if (or (keyword? v) (symbol? v)) (name v) (str v))
            prefix (subs rendered 0 (min (count rendered) 40))]
        (is (or (str/includes? text rendered) (str/includes? text prefix))
            (str label " · the text drops leaf " path "=" rendered))))
    (if-let [call (:next_call structured)]
      (let [encoded (json/generate-string call)]
        (is (or (str/includes? text encoded)
                (and (str/includes? text "next_call")
                     (str/includes? text (str (count encoded)))))
            (str label " · the text drops the next_call the caller must send")))
      (is (str/includes? text "next_call")
          (str label " · an absent next_call is omitted rather than stated")))
    text))

;; @spec MCP-OP-ADMIT-131
(deftest every-admit-refusal-kind-renders-every-structured-leaf-in-its-text-block
  ;; Synthetic receipts, one per derived kind, exactly as
  ;; MCP-OP-ALIAS-059's every-refusal-kind test drives alias_migration:
  ;; cheap enough to cover the whole enumeration, and the two named live
  ;; reproductions below (analyzer-memory-exhausted, verification-incomplete)
  ;; carry the real production path for the two kinds the review named.
  (doseq [kind (map name admit/admit-refusal-kinds)]
    (testing kind
      (assert-refusal-text-superset!
        {:ok false
         :operation :admit-patch-refused
         :mode "commit"
         :error-type kind
         :error (str "one sentence stating the " kind " cause")
         :remedy (str "Resend the next_call; it corrects " kind ".")
         :elapsed_ms 1.25
         :source-unchanged true
         :committed false
         :mutation_attempted false
         ;; a nested map, to prove the walk actually recurses -- the review
         ;; named exactly this shape (lint_delta's cap/observed-bytes)
         :lint_delta {:ran false :ok false :cap 999 :observed-bytes 1234
                      :detector "clj-kondo"}
         :files [(str kind "-file.clj")]
         :next_call {:tool "admit_clojure_patch"
                     :arguments {:mode "preview" :verify "focused"}
                     :patch_field "patch"
                     :patch_sha256 "deadbeef"
                     :blocked_by kind}}
        kind))))

;; @spec MCP-OP-ADMIT-131
(deftest a-refusal-with-no-next-call-states-its-absence
  (assert-refusal-text-superset!
    {:ok false
     :operation :admit-patch-refused
     :mode "preview"
     :error-type "patch-too-large"
     :error "patch is 999999 UTF-8 bytes; the admission limit is 262144"
     :elapsed_ms 0.5
     :source-unchanged true
     :patch_bytes 999999
     :next_call nil}
    "patch-too-large-no-next-call"))

;; ---------------------------------------------------------------------------
;; Live reproductions of the two kinds the review named directly
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-131
(deftest a-verification-incomplete-refusal-carries-the-analyzer-diagnostic-and-next-call
  ;; The review, verbatim: "verification-incomplete (the dropped next_call
  ;; is MCP-OP-ADMIT-120's own affordance)". Reproduced through the real
  ;; production path: a lint runner that reports a genuine truncation (the
  ;; E-GATE-R field shape), verify=focused, mode=commit, no allow_partial.
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [truncated-lint
            (fn [_ _]
              {:ran false :ok false :status :unverified
               :detector "clj-kondo" :error-type :analyzer-output-truncated
               :cap 2000 :observed-bytes 5000
               :remedy (str "clj-kondo answered with 5000 bytes of findings "
                            "and this gate reads at most 2000; raise the "
                            "analyzer read ceiling "
                            "(:admit-analyzer-visible-bytes) or narrow the "
                            "patch to fewer files")
               :error (str "clj-kondo findings were cut at 2000 bytes of "
                           "5000; the analyzer ran and the gate could not "
                           "read its answer")})
            result (admit/execute-request!
                     (stub-config root {:admit-lint-runner truncated-lint})
                     {:patch clean-multi-file-patch :mode "commit"
                      :verify "focused"})
            text (#'admit/summary (assoc result :elapsed_ms 1.0))]
        (is (false? (:ok result)))
        (is (= :verification-incomplete (:error-type result)))
        (is (false? (:committed result)))
        (is (= 2000 (get-in result [:lint_delta :cap])))
        (is (= 5000 (get-in result [:lint_delta :observed-bytes])))
        (is (some? (:next_call result))
            "MCP-OP-ADMIT-120: the refusal proposes the verify that could lift it")
        (is (= "focused" (get-in result [:next_call :arguments :verify])))
        (testing "every leaf, including the nested analyzer diagnostic, is in the text"
          (assert-refusal-text-superset! (assoc result :elapsed_ms 1.0)
                                          "verification-incomplete-live")))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-131
(deftest an-analyzer-memory-exhausted-refusal-carries-its-remedy-and-heap-number-in-text
  ;; The review, verbatim: "analyzer-memory-exhausted (the dropped remedy is
  ;; the number that lifts it)". Reproduced through the real
  ;; edge-throwable-refusal classifier, same construction as
  ;; MCP-OP-ADMIT-129's own witness -- an OutOfMemoryError is constructed,
  ;; never provoked.
  (let [config-atom (deref #'admit/runtime-config)
        previous @config-atom
        root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (reset! config-atom
              {:project-root (.getPath root)
               :admit-lint-runner (fn [_ _] (throw (OutOfMemoryError. "Java heap space")))})
      (let [captured (atom nil)
            _ (admit/handle-admit-clojure-patch
                nil
                {"patch" clean-multi-file-patch "verify" "focused"}
                (fn [content error? result]
                  (reset! captured {:text (first content) :result result})))
            {:keys [text result]} @captured]
        (is (= :analyzer-memory-exhausted (:error-type result)))
        (is (pos? (long (:max_heap_mib result))))
        (is (string? (:remedy result)))
        (is (str/includes? (str text) (str (:remedy result)))
            "the text drops the remedy -- the exact route that lifts this refusal")
        (is (str/includes? (str text) (str (:max_heap_mib result)))
            "the text drops the heap number the remedy itself refers to")
        (testing "every leaf of the OOM receipt is in the text"
          (assert-refusal-text-superset! (assoc result :elapsed_ms 1.0)
                                          "analyzer-memory-exhausted-live")))
      (finally
        (reset! config-atom previous)
        (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; MCP-OP-ADMIT-132: expect_pre_sha256 is copyable from the text alone
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-132
(deftest a-preview-of-an-existing-file-carries-expect-pre-sha256-in-its-text
  ;; The tool description: "Copy expect_pre_sha256 from a preview's
  ;; next_call to bind the [commit] transaction." Before this round nothing
  ;; rendered next_call on the :ok true branch at all, so that instruction
  ;; was not satisfiable from content[0].text.
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch clean-multi-file-patch :mode "preview"
                      :verify "focused"})
            text (#'admit/summary (assoc result :elapsed_ms 1.0))
            expect-pre (get-in result [:next_call :arguments :expect_pre_sha256])]
        (is (:ok result))
        (is (map? expect-pre)
            "fixture must actually touch existing files, or expect_pre_sha256 is never populated")
        (is (str/includes? text "expect_pre_sha256")
            "the field the description tells the caller to copy is absent from the text")
        (is (str/includes? text (json/generate-string (:next_call result)))
            "expect_pre_sha256 is not readable from the text alone")
        (doseq [[file sha] expect-pre]
          (is (str/includes? text sha)
              (str "the text drops the pre-image hash for " file))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Round four, blocker 2 (MCP-OP-ADMIT-134): "every leaf" was false by
;; explicit exclusion and by shape, and the witness copied the policy
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-134
(defn- json-leaves
  "A second, independent leaf walk -- over the receipt AS JSON.

  Deliberately not `clj-surgeon.mcp-admit-tool/admit-leaf-entries`, and
  deliberately not a Clojure walk at all. The claim under test is `the text
  names everything structuredContent spells`, and the only authority on what
  structuredContent spells is the JSON encoder that produces it. Round
  three's witness walked the Clojure map holding its OWN copy of the
  renderer's eleven-key exclusion list, so the two sides agreed about what
  may be missing by construction -- the reviewer's word was `tautological`.
  This side shares no function and no constant with the renderer: it encodes
  the receipt, parses it back, and reports every leaf the JSON actually has,
  value-less shapes included."
  [path v]
  (cond
    (and (map? v) (seq v))
    (mapcat (fn [[k cv]] (json-leaves (str path (when (seq path) ".") k) cv)) v)

    (map? v) [[path "{}"]]

    (and (sequential? v) (seq v))
    (apply concat
           (map-indexed (fn [i cv] (json-leaves (str path "[" i "]") cv)) v))

    (sequential? v) [[path "[]"]]

    (nil? v) [[path "null"]]

    (= v "") [[path "\"\""]]

    :else [[path (str v)]]))

;; @spec MCP-OP-ADMIT-134
(defn- structured-leaves
  [receipt]
  (json-leaves "" (json/parse-string (json/generate-string receipt))))

;; @spec MCP-OP-ADMIT-134
(defn- assert-text-names-every-structured-leaf!
  "Every leaf structuredContent spells appears in the text block as
  `path=value`.

  No exclusion list on this side, because the implementation is not allowed
  one either: a receipt has two faces and the text face must not say less.
  Leaves longer than the renderer's per-leaf ceiling are checked by their own
  witness rather than here, so this assertion needs no constant from the
  implementation at all."
  [receipt label]
  (let [receipt (assoc receipt :elapsed_ms (or (:elapsed_ms receipt) 1.0))
        text (#'admit/summary receipt)]
    (is (not-any? set? (tree-seq coll? seq receipt))
        (str label " · a receipt leaf is a Clojure set; JSON has no sets, so "
             "structuredContent's ordering of it is undefined and this "
             "witness cannot bind the text to it"))
    (doseq [[path value] (structured-leaves receipt)
            :when (<= (count value) 200)]
      (is (str/includes? text (str path "=" value))
          (str label " · the text block never names " path "=" value)))
    text))

;; @spec MCP-OP-ADMIT-134
(deftest a-refusal-text-names-the-files-and-hashes-its-structure-carries
  ;; Sol's round-three receipt, verbatim: `{:probe :shape-exclusions,
  ;; :contains-files false, :contains-pre-hash false, ...}`. The renderer
  ;; excluded :files and :hashes by name, on the reasoning that they are
  ;; "diff metadata the caller already sent". The caller who reads only the
  ;; text is exactly the caller who cannot go and look them up.
  (assert-text-names-every-structured-leaf!
    {:ok false
     :operation :admit-patch-refused
     :mode "preview"
     :error-type :source-hash-mismatch
     :error "the workspace changed while this admission was being verified"
     :elapsed_ms 1.25
     :source-unchanged true
     :files ["src/app/core.clj" "src/app/util.clj"]
     :hashes {"src/app/core.clj" {:pre "PRE-CORE-DIGEST" :post "POST-CORE-DIGEST"}
              "src/app/util.clj" {:pre "PRE-UTIL-DIGEST" :post "POST-UTIL-DIGEST"}}
     :next_call {:tool "admit_clojure_patch" :blocked_by "source-hash-mismatch"}}
    "files-and-hashes"))

;; @spec MCP-OP-ADMIT-134
(deftest a-value-less-shape-renders-the-characters-structured-content-spells
  ;; The second half of Sol's receipt: :contains-empty false, :contains-map
  ;; false, :contains-nil false. structuredContent spells `[]`, `{}`, `null`
  ;; and `""`; a text that spells none of them is a strict subset of it.
  (assert-text-names-every-structured-leaf!
    {:ok false
     :operation :admit-patch-refused
     :mode "preview"
     :error-type :verification-incomplete
     :error "the analyzer ran and the gate could not read its answer"
     :elapsed_ms 1.0
     :source-unchanged true
     :detectors_not_run []
     :protected_node_drift {}
     :verification_reasons []
     :lock_scope nil
     :focused_report_path ""
     :next_call {:tool "admit_clojure_patch" :blocked_by "verification-incomplete"}}
    "value-less-shapes"))

;; @spec MCP-OP-ADMIT-134
(deftest a-live-refusal-text-names-every-leaf-of-its-own-receipt
  ;; The same claim through the production path rather than a fixture: a real
  ;; commit refusal, whose receipt carries whatever the gate actually put in
  ;; it -- not whatever this test remembered to write down.
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [truncated-lint
            (fn [_ _]
              {:ran false :ok false :status :unverified
               :detector "clj-kondo" :error-type :analyzer-output-truncated
               :cap 2000 :observed-bytes 5000
               :error "the analyzer ran and the gate could not read its answer"})
            result (admit/execute-request!
                     (stub-config root {:admit-lint-runner truncated-lint})
                     {:patch clean-multi-file-patch :mode "commit"
                      :verify "focused"})]
        (is (false? (:ok result)))
        (assert-text-names-every-structured-leaf! result "live-commit-refusal"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-136
(defn- generated-source
  [index value]
  (str "(ns app.gen" index ")\n"
       "\n"
       "(defn value\n"
       "  []\n"
       "  " value ")\n"))

;; @spec MCP-OP-ADMIT-136
(defn- generated-sources
  "`n` ordinary one-owner files, the shape a wide but unremarkable patch
  touches."
  [n]
  (into {}
        (map (fn [i] [(str "src/app/gen" i ".clj") (generated-source i 1)]))
        (range n)))

;; @spec MCP-OP-ADMIT-136
(defn- generated-patch
  [n]
  (apply str
         (map (fn [i]
                (str "--- a/src/app/gen" i ".clj\n"
                     "+++ b/src/app/gen" i ".clj\n"
                     "@@ -1,5 +1,5 @@\n"
                     " (ns app.gen" i ")\n"
                     " \n"
                     " (defn value\n"
                     "   []\n"
                     "-  1)\n"
                     "+  2)\n"))
              (range n))))

;; @spec MCP-OP-ADMIT-136
(defn- wide-preview-receipt
  "One `:ok true` preview of `n` one-line changes, through the entrance."
  [n]
  (let [root (temp-dir)]
    (try
      (write-sources! root (generated-sources n))
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch (generated-patch n) :mode "preview"})]
        (is (true? (:ok result))
            (str n "-file preview must succeed: " (:error result)))
        result)
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-136
(deftest a-twenty-file-preview-text-names-every-leaf-of-its-own-receipt
  ;; Round four's blocking finding, as a fixture. The reviewer called
  ;; `assert-text-names-every-structured-leaf!` -- this file's own witness,
  ;; unmodified -- on a live twenty-file preview and it failed 68 assertions:
  ;; structuredContent was 15,086 bytes, well under the 32,640-byte public
  ;; budget and not truncated, and the text dropped 68 leaves including
  ;; `source-unchanged`, `pre_image_binding`, `lock_scope` and
  ;; `mutation_attempted` -- stranded at the tail of a path-alphabetical sort
  ;; by a fact-section budget of half the public one. The suite was green only
  ;; because every fixture in it sat under that bound. This one does not.
  (assert-text-names-every-structured-leaf!
    (wide-preview-receipt 20) "twenty-file-preview"))

;; @spec MCP-OP-ADMIT-136
(deftest a-forty-file-preview-text-names-every-leaf-of-its-own-receipt
  ;; Twice as wide: the reviewer measured 396 of 796 leaves absent from the
  ;; text while the receipt still read `ok`. Here the structured face is the
  ;; one that must give ground -- 40 files of facts cannot be spelled inside
  ;; the public budget -- and whatever survives into structuredContent must be
  ;; named, leaf for leaf, in the text.
  (assert-text-names-every-structured-leaf!
    (wide-preview-receipt 40) "forty-file-preview"))

;; @spec MCP-OP-ADMIT-134
(deftest the-fact-walk-has-no-exclusion-list-to-get-wrong
  ;; The structural half of blocker 2. Round three's exclusion set had eleven
  ;; members and its witness had eleven copies of them. The repair is not a
  ;; better-maintained list; it is no list. This assertion fails the day one
  ;; is reintroduced, which is the day the EARS text has to justify it.
  (is (= #{} @#'admit/admit-receipt-fact-exclusions)
      (str "a key was excluded from the fact walk; name it and its reason in "
           "MCP-OP-ADMIT-134's EARS text, and give it a witness, before "
           "relaxing this")))

;; @spec MCP-OP-ADMIT-134
(deftest a-leaf-past-the-per-fact-ceiling-is-cut-with-the-cut-stated
  ;; At the ceiling and one past it, read from the implementation rather than
  ;; retyped: the assertion is about behaviour AT the bound, never about the
  ;; number.
  (let [ceiling @#'admit/max-admit-receipt-fact-characters
        text-of (fn [value]
                  (#'admit/summary
                    {:ok false :operation :admit-patch-refused :mode "preview"
                     :error-type :invalid-patch :error "e" :elapsed_ms 1.0
                     :source-unchanged true :long_leaf value :next_call nil}))]
    (testing "exactly at the ceiling, the value renders whole"
      (let [value (apply str (repeat ceiling "x"))]
        (is (str/includes? (text-of value) (str "long_leaf=" value)))
        (is (not (str/includes? (text-of value) "characters in structuredContent")))))
    (testing "one character past it, the value is cut and the cut is counted"
      (let [value (apply str (repeat (inc ceiling) "x"))
            text (text-of value)]
        (is (not (str/includes? text (str "long_leaf=" value)))
            "the whole value cannot have rendered")
        (is (str/includes? text (str "long_leaf=" (subs value 0 ceiling)
                                     "…[+1 characters in structuredContent]"))
            "the text names the field, what it printed, and exactly what it cut")))))

;; @spec MCP-OP-ADMIT-134
(deftest a-receipt-past-the-fact-section-budget-states-how-many-it-dropped
  ;; The count elision, at the bound. A leaf is never dropped silently: the
  ;; text states the exact number that live in structuredContent instead, and
  ;; "exact" is checked against this witness's own independent leaf count.
  (let [budget @#'admit/admit-fact-section-byte-budget
        wide (into {} (for [i (range 4000)]
                        [(keyword (format "leaf%04d" i))
                         (apply str (repeat 40 "y"))]))
        result (merge {:ok false :operation :admit-patch-refused :mode "preview"
                       :error-type :invalid-patch :error "e" :elapsed_ms 1.0
                       :source-unchanged true :next_call nil}
                      wide)
        text (#'admit/summary result)
        fact-line (->> (str/split-lines text)
                       (filter #(str/starts-with? % "facts · "))
                       first)
        marker (re-find #"\[\+(\d+) more facts in structuredContent\]" text)
        printed (->> (str/split (subs fact-line (count "facts · ")) #" · ")
                     (remove #(str/starts-with? % "[+"))
                     count)
        total (count (structured-leaves result))]
    (is (some? fact-line))
    (is (some? marker)
        (str "a receipt whose leaves exceed the " budget
             "-byte fact budget must say so, not stop"))
    (is (< printed total) "some leaves were in fact elided")
    (is (<= (count fact-line) budget)
        "the fact section stayed inside the budget it states")
    (is (= (- total printed) (Integer/parseInt (second marker)))
        (str "the stated omitted count must equal this witness's own count of "
             "leaves minus the facts actually printed: " total " - " printed))))

;; ---------------------------------------------------------------------------
;; Round four, blocker 3 (MCP-OP-ADMIT-134): the SUCCESS branch was never a
;; superset either, and the relabelled witness only checked next_call
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-134
(deftest a-successful-preview-and-commit-text-names-every-leaf-of-its-receipt
  ;; Sol's round-three receipt, verbatim: a real two-file COMMIT carried file
  ;; records, pre/post hashes, focused test namespaces and detectors_not_run
  ;; [], and its text was
  ;;
  ;;   "admit_clojure_patch\n  admit-patch! · 2 file(s) · owners +0 ~2 -0 ·
  ;;    drift 0 bytes · hazards 0 · 1.00 ms\nverification_complete=true
  ;;    verification_status=complete\nnext_call · none — this receipt has no
  ;;    follow-up call"
  ;;
  ;; -- :text-has-first-file false, :text-has-first-pre false,
  ;;    :text-has-first-test false.
  ;;
  ;; The ok branch rendered COUNTS, not the receipt's leaves: two file(s), not
  ;; which two; hashes 0 drift, not the digests. The round-three witness that
  ;; called itself "really ... a superset" asserted only that next_call
  ;; appeared. This drives the same preview and the same commit through
  ;; execute-request! and holds the ok branch to the identical rule the
  ;; refusal branch already obeys.
  (let [root (temp-dir)]
    (try
      (write-sources! root (assoc base-sources
                                  "test/app/core_test.clj" "(ns app.core-test)\n"
                                  "test/app/util_test.clj" "(ns app.util-test)\n"))
      (let [config (stub-config root)
            preview (admit/execute-request!
                      config {:patch clean-multi-file-patch :verify "focused"})]
        (is (true? (:ok preview)))
        (is (seq (:files preview)) "the fixture must carry file records")
        (is (seq (:hashes preview)) "the fixture must carry pre-image digests")
        (assert-text-names-every-structured-leaf! preview "ok-preview")

        (let [commit (admit/execute-request!
                       config {:patch clean-multi-file-patch :mode "commit"
                               :verify "focused"
                               :expect_pre_sha256 (get-in preview
                                                          [:next_call :arguments
                                                           :expect_pre_sha256])})]
          (is (true? (:ok commit)) (str "commit refused: " (:error commit)))
          (is (true? (:committed commit)))
          (is (= [] (:detectors_not_run commit))
              "the fixture must carry the empty-list shape the review named")
          (is (seq (get-in commit [:tests :namespaces]))
              "the fixture must carry focused test namespaces")
          (assert-text-names-every-structured-leaf! commit "ok-commit")))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Round four, blocker 4 (MCP-OP-ADMIT-135): a reachable next_call became
;; non-copyable text while the description told the caller to copy it
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-135
(defn- next-call-text
  [call]
  (#'admit/summary {:ok false :operation :admit-patch-refused :mode "preview"
                    :error-type :invalid-patch :error "an error sentence"
                    :elapsed_ms 1.0 :source-unchanged true :next_call call}))

;; @spec MCP-OP-ADMIT-135
(deftest a-next-call-renders-verbatim-at-any-size
  ;; Sol's round-three receipt, verbatim:
  ;;   {:probe :next-call-bound, :padding-length 968, :encoded-length 1025,
  ;;    :verbatim false, :pointer true}
  ;; The tool description (mcp_admit_tool.clj:66) tells a caller to copy
  ;; expect_pre_sha256 out of next_call. Above 1,024 characters the text
  ;; replaced it with a pointer at structuredContent -- which a text-only
  ;; caller, the only caller this ratchet exists for, cannot read.
  (doseq [padding [1 2048 8192]]
    (testing (str "padding " padding)
      (let [call {:tool "admit_clojure_patch"
                  :arguments {:mode "commit" :verify "focused"
                              :expect_pre_sha256
                              {"src/app/core.clj" (apply str (repeat padding "a"))}}}
            encoded (json/generate-string call)
            text (next-call-text call)]
        (is (str/includes? text (str "next_call · " encoded))
            (str "a " (count encoded) "-character next_call must render "
                 "verbatim; the caller is told to copy it"))
        (is (not (str/includes? text "in structuredContent.next_call"))
            "no pointer may stand where the call itself belongs")))))

;; @spec MCP-OP-ADMIT-135
(deftest an-ordinary-wide-preview-can-be-copied-out-of-its-own-text
  ;; Sol, verbatim: "A routine 14-file preview produced 1,554 characters, so a
  ;; text-only caller cannot perform the instructed copy/send operation. This
  ;; is not merely a synthetic boundary case." Driven through the real
  ;; entrance, then the JSON is parsed BACK OUT of the text and used, so the
  ;; assertion is that the text is sendable rather than that it is long.
  (let [root (temp-dir)
        n 14
        sources (into {} (for [i (range n)]
                           [(str "src/app/m" i ".clj")
                            (str "(ns app.m" i ")\n\n(defn f\n  [x]\n  (inc x))\n")]))
        patch (apply str
                     (for [i (range n)]
                       (str "--- a/src/app/m" i ".clj\n"
                            "+++ b/src/app/m" i ".clj\n"
                            "@@ -1,5 +1,5 @@\n"
                            " (ns app.m" i ")\n"
                            " \n"
                            " (defn f\n"
                            "   [x]\n"
                            "-  (inc x))\n"
                            "+  (inc (inc x)))\n")))]
    (try
      (write-sources! root sources)
      (let [preview (admit/execute-request!
                      (stub-config root) {:patch patch :verify "none"})
            encoded (json/generate-string (:next_call preview))
            text (#'admit/summary (assoc preview :elapsed_ms 1.0))]
        (is (true? (:ok preview)) (str "preview refused: " (:error preview)))
        (is (= n (count (:files preview))))
        (is (> (count encoded) 1024)
            (str "the fixture must actually exceed round three's ceiling, or "
                 "this proves nothing; it is " (count encoded) " characters"))
        (is (str/includes? text (str "next_call · " encoded))
            "a routine 14-file preview's next_call must be copyable from text")
        (testing "and what is copied out of the text is what the gate meant"
          (let [line (->> (str/split-lines text)
                          (filter #(str/starts-with? % "next_call · "))
                          first)
                ;; parsed WITHOUT keywordising: the expect_pre_sha256 keys are
                ;; file paths, and turning "src/app/m11.clj" into a keyword is
                ;; the witness corrupting the thing it is checking
                recovered (json/parse-string (subs line (count "next_call · ")))]
            (is (= (json/parse-string encoded) recovered)
                "the JSON parsed back out of the text is the receipt's own call")
            (is (= n (count (get-in recovered ["arguments" "expect_pre_sha256"])))
                "every pre-image digest the commit needs survived the render"))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-135
(deftest a-next-call-that-alone-exceeds-the-public-budget-is-a-typed-refusal
  ;; The other end of the rule. If the call genuinely cannot be published,
  ;; the honest answer is a refusal naming the size and the budget -- never a
  ;; pointer, which is the same failure one level down: a text-only client has
  ;; no structuredContent to be pointed at.
  (let [budget write-refusal/public-byte-budget
        huge {:tool "admit_clojure_patch"
              :arguments {:mode "commit"
                          :expect_pre_sha256
                          {"src/app/core.clj" (apply str (repeat (inc budget) "a"))}}}
        published (#'admit/bound-receipt
                    {:ok true :operation :admit-patch-preview :mode "preview"
                     :files [] :next_call huge})]
    (is (false? (:ok published))
        "a next_call the budget cannot carry must refuse, not publish")
    (is (= :next-call-exceeds-public-budget (:error-type published)))
    (is (str/includes? (str (:error published)) (str (count (json/generate-string huge))))
        "the refusal names the exact size")
    (is (str/includes? (str (:error published)) (str budget))
        "and the budget that would have to change")
    (is (some? (:remedy published)) "and what the caller can do about it")))

;; @spec MCP-OP-ADMIT-135
(deftest the-next-call-is-the-last-thing-a-crowded-receipt-gives-up
  ;; The stated elision order. Other leaves elide first; the next_call is
  ;; rendered after them and never elided.
  (let [call {:tool "admit_clojure_patch"
              :arguments {:mode "commit" :verify "focused"
                          :expect_pre_sha256
                          {"src/app/core.clj" (apply str (repeat 2000 "a"))}}}
        encoded (json/generate-string call)
        crowded (merge {:ok false :operation :admit-patch-refused :mode "preview"
                        :error-type :invalid-patch :error "e" :elapsed_ms 1.0
                        :source-unchanged true :next_call call}
                       (into {} (for [i (range 4000)]
                                  [(keyword (format "leaf%04d" i))
                                   (apply str (repeat 40 "y"))])))
        text (#'admit/summary crowded)]
    (is (str/includes? text "more facts in structuredContent]")
        "the fixture must actually be over the fact budget")
    (is (str/includes? text (str "next_call · " encoded))
        (str "the next_call is rendered last and never elided; everything "
             "else gives ground before it does"))))

;; ---------------------------------------------------------------------------
;; Round four, blocker 1 (MCP-OP-ADMIT-133): the refusal enumeration was
;; derived by SCANNING SOURCE, and a kind built dynamically left it green
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-133
(def ^:private observed-refusal-kinds
  "Every `:error-type` the admit entrance actually published during this
  namespace's run.

  Round three enumerated the gate's refusal kinds by reading five source
  files for literal shapes. That derivation was already wrong -- it missed
  `:workspace-lock-unavailable`, which this suite drives live -- and it was
  wrong in a way no witness could detect, because a kind built dynamically
  has no literal to scan for. The reviewer planted exactly such a kind and
  both enumeration witnesses stayed green.

  So the enumeration is derived from EXECUTION instead. The recording point
  is the gate's own refusal constructor, which every published receipt passes
  through, and the driver is this whole suite: every kind any of its tests
  provokes is seen, including the ones no fixture was written for on
  purpose."
  (atom #{}))

;; @spec MCP-OP-ADMIT-133
(def ^:private ^:dynamic *inside-the-entrance* false)

;; @spec MCP-OP-ADMIT-133
(defn- record-and-check-refusal-kinds
  "Record at the gate's own refusal constructor, which is the one point every
  published refusal passes -- `bound-receipt` for everything
  `execute-request!` returns, and the handler's edge for the three kinds only
  the MCP surface can produce."
  [run-tests!]
  (reset! observed-refusal-kinds #{})
  (let [guard admit/checked-refusal-kind!
        execute admit/execute-request!
        handle admit/handle-admit-clojure-patch]
    (with-redefs [;; recorded only while a call is genuinely inside one of
                  ;; the two public entrances -- a witness that calls the
                  ;; guard directly to prove it rejects a planted kind must
                  ;; not thereby report that kind as something the gate
                  ;; produces
                  admit/checked-refusal-kind!
                  (fn [receipt]
                    (when (and *inside-the-entrance*
                               (map? receipt) (false? (:ok receipt)))
                      (swap! observed-refusal-kinds conj (:error-type receipt)))
                    (guard receipt))
                  admit/execute-request!
                  (fn [& args]
                    (binding [*inside-the-entrance* true] (apply execute args)))
                  admit/handle-admit-clojure-patch
                  (fn [& args]
                    (binding [*inside-the-entrance* true] (apply handle args)))]
      (run-tests!)))
  (testing "MCP-OP-ADMIT-133: the enumeration is the set the entrance produces"
    (let [observed @observed-refusal-kinds
          enumerated admit/admit-refusal-kinds]
      (is (seq observed) "the suite drove no refusal at all; the driver is broken")
      (is (empty? (set/difference observed enumerated))
          (str "the entrance published kinds the enumeration has never heard "
               "of: " (pr-str (set/difference observed enumerated))))
      (is (empty? (set/difference enumerated observed))
          (str "the enumeration claims kinds no fixture drives, so nothing "
               "proves they exist or that their text is a superset: "
               (pr-str (set/difference enumerated observed))))
      (is (= enumerated observed)
          (str "enumerated " (count enumerated) ", observed " (count observed))))))

(use-fixtures :once record-and-check-refusal-kinds)

;; ---------------------------------------------------------------------------
;; MCP-OP-ADMIT-133: one live fixture per enumerated kind the rest of this
;; suite does not already provoke. Each drives the real entrance; the :once
;; fixture above records what it produced and holds the enumeration to it.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-133
(defn- refusal-kind-of
  [root sources params & [overrides]]
  (write-sources! root sources)
  (let [receipt (admit/execute-request! (stub-config root overrides) params)]
    (is (false? (:ok receipt))
        (str "fixture did not refuse; it returned " (pr-str (:error-type receipt))))
    (:error-type receipt)))

;; @spec MCP-OP-ADMIT-133
(deftest an-invalid-workspace-root-refuses-at-the-router
  (let [root (temp-dir)]
    (try
      (is (= :invalid-workspace-root
             (refusal-kind-of root base-sources
                              {:patch clean-multi-file-patch :verify "focused"
                               :workspace_root "relative/not/canonical"})))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-133
(deftest a-binary-patch-is-refused-as-unsupported
  (let [root (temp-dir)]
    (try
      (is (= :binary-patch-unsupported
             (refusal-kind-of
               root base-sources
               {:patch (str "diff --git a/src/app/core.clj b/src/app/core.clj\n"
                            "index 0000000..1111111 100644\n"
                            "Binary files a/src/app/core.clj and "
                            "b/src/app/core.clj differ\n")
                :verify "none"})))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-133
(deftest two-hunks-that-overlap-refuse-before-anything-is-applied
  ;; Out-of-order unified hunks: the second declares a :pre-start behind the
  ;; cursor the first left, which is the shape locate-hunk refuses.
  (let [root (temp-dir)]
    (try
      (is (= :overlapping-hunks
             (refusal-kind-of
               root base-sources
               {:patch (str "--- a/src/app/core.clj\n"
                            "+++ b/src/app/core.clj\n"
                            "@@ -9,2 +9,2 @@\n"
                            " (defn label\n"
                            "-  [state]\n"
                            "+  [state ]\n"
                            "@@ -5,2 +5,2 @@\n"
                            " (defn handle-tick\n"
                            "-  [state]\n"
                            "+  [state  ]\n")
                :verify "none"})))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-133
(deftest a-source-path-that-is-a-directory-is-not-a-regular-file
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (.mkdirs (io/file root "src/app/dir.clj"))
      (is (= :source-not-regular-file
             (refusal-kind-of
               root {}
               {:patch (str "--- a/src/app/dir.clj\n"
                            "+++ b/src/app/dir.clj\n"
                            "@@ -1,1 +1,1 @@\n"
                            "-(ns app.dir)\n"
                            "+(ns app.dir2)\n")
                :verify "none"})))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-133
(deftest a-creation-whose-parent-is-a-regular-file-refuses
  (let [root (temp-dir)]
    (try
      (is (= :target-parent-not-directory
             (refusal-kind-of
               root base-sources
               {:patch (str "--- /dev/null\n"
                            "+++ b/src/app/core.clj/child.clj\n"
                            "@@ -0,0 +1,1 @@\n"
                            "+(ns app.child)\n")
                :verify "none"})))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-133
(deftest a-source-path-under-a-regular-file-is-an-invalid-source-path
  ;; ENOTDIR from .toRealPath, which is a FileSystemException and not the
  ;; NoSuchFileException the source-file-not-found catch takes.
  (let [root (temp-dir)]
    (try
      (is (= :invalid-source-path
             (refusal-kind-of
               root base-sources
               {:patch (str "--- a/src/app/core.clj/child.clj\n"
                            "+++ b/src/app/core.clj/child.clj\n"
                            "@@ -1,1 +1,1 @@\n"
                            "-(ns app.child)\n"
                            "+(ns app.child2)\n")
                :verify "none"})))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-133
(deftest an-uninitialised-server-refuses-at-the-handlers-edge
  ;; server-not-initialized is one of three kinds only the MCP surface can
  ;; produce; execute-request! has no path to it.
  (let [config-atom (deref #'admit/runtime-config)
        previous @config-atom
        received (promise)]
    (try
      (reset! config-atom nil)
      (admit/handle-admit-clojure-patch
        nil {"patch" clean-multi-file-patch "verify" "focused"}
        (fn [result & _] (deliver received result)))
      (is (some? (deref received 5000 nil))
          "the handler must answer even with no server configured")
      (finally (reset! config-atom previous)))))

;; @spec MCP-OP-ADMIT-133
(defn- slurp-safe
  [file]
  (try (slurp file) (catch Exception _ nil)))

;; @spec MCP-OP-ADMIT-133
(defn- transaction-fixture-sources
  "Files across two directories; the second is made unwritable so its write
  fails after the first directory's writes have already landed."
  [n]
  (into {"src/b/util.clj" util-source
         ;; the focused-test files the gate derives by path convention;
         ;; commit mode refuses verify=none outright (MCP-OP-ADMIT-120), so
         ;; this fixture has to reach the transaction through a real
         ;; verification that passes
         "test/b/util_test.clj" "(ns app.util-test)\n"}
        (mapcat (fn [i]
                  [[(format "src/a/f%03d.clj" i)
                    (format "(ns app.f%03d)\n\n(defn f\n  [x]\n  (inc x))\n" i)]
                   [(format "test/a/f%03d_test.clj" i)
                    (format "(ns app.f%03d-test)\n" i)]])
                (range n))))

;; @spec MCP-OP-ADMIT-133
(defn- transaction-fixture-patch
  [n]
  (str (apply str
              (for [i (range n)]
                (format (str "--- a/src/a/f%03d.clj\n+++ b/src/a/f%03d.clj\n"
                             "@@ -1,5 +1,5 @@\n (ns app.f%03d)\n \n (defn f\n"
                             "   [x]\n-  (inc x))\n+  (inc (inc x)))\n")
                        i i i)))
       "--- a/src/b/util.clj\n"
       "+++ b/src/b/util.clj\n"
       "@@ -2,4 +2,4 @@\n"
       " \n"
       " (defn clamp\n"
       "   [value low high]\n"
       "-  (max low (min high value)))\n"
       "+  (long (max low (min high value))))\n"))

;; @spec MCP-OP-ADMIT-133
(deftest a-write-that-fails-mid-transaction-rolls-back-and-says-so
  ;; src/b is unwritable, so the last file's write fails after every file in
  ;; src/a has landed. Nothing else touches the tree, so every rollback
  ;; succeeds and the receipt is the rolled-back kind.
  (let [root (temp-dir)
        n 8]
    (try
      (write-sources! root (transaction-fixture-sources n))
      (shell/sh "chmod" "555" (.getPath (io/file root "src/b")))
      (let [receipt (admit/execute-request!
                      (stub-config root)
                      {:patch (transaction-fixture-patch n) :mode "commit"
                       :verify "focused"})]
        (is (false? (:ok receipt)))
        (is (= :transaction-write-failed (:error-type receipt))
            (str "reasons=" (pr-str (:verification_reasons receipt))
                 " dnr=" (pr-str (:detectors_not_run receipt))
                 " status=" (pr-str (:verification_status receipt))
                 " err=" (pr-str (:error receipt))))
        (is (true? (:source-unchanged receipt))
            "a rolled-back transaction leaves the workspace as it found it"))
      (finally
        (shell/sh "chmod" "755" (.getPath (io/file root "src/b")))
        (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-133
(deftest a-rollback-that-cannot-restore-a-file-demands-manual-recovery
  ;; The one enumerated kind no single-threaded fixture can produce, because
  ;; it exists to report exactly the case a single thread cannot create: a
  ;; THIRD PARTY changed a file between the transaction's write and its
  ;; rollback, so the recovery refuses to overwrite bytes it did not write.
  ;;
  ;; The window is widened rather than raced for. src/b is unwritable, so the
  ;; failing write happens only after all 64 files in src/a have landed, and a
  ;; watcher clobbers the first of them for the whole duration of those
  ;; writes. It is stopped and joined before the assertion.
  (let [root (temp-dir)
        n 64
        _ (write-sources! root (transaction-fixture-sources n))
        first-file (io/file root "src/a/f000.clj")
        original (slurp-safe first-file)
        stop? (atom false)
        watcher (Thread.
                  (fn []
                    (while (not @stop?)
                      (let [current (slurp-safe first-file)]
                        (when (and current (not= current original)
                                   (not= current ";; CLOBBERED\n"))
                          (try (spit first-file ";; CLOBBERED\n")
                               (catch Exception _ nil)))))))]
    (try
      (shell/sh "chmod" "555" (.getPath (io/file root "src/b")))
      (.start watcher)
      (let [receipt (admit/execute-request!
                      (stub-config root)
                      {:patch (transaction-fixture-patch n) :mode "commit"
                       :verify "focused"})]
        (reset! stop? true)
        (.join watcher 5000)
        (is (false? (:ok receipt)))
        (is (= :transaction-recovery-required (:error-type receipt))
            (str "expected the unrecovered kind; got " (:error-type receipt)))
        (is (not (true? (:source-unchanged receipt)))
            (str "a transaction that could not restore a file must not claim "
                 "the workspace is unchanged")))
      (finally
        (reset! stop? true)
        (.join watcher 5000)
        (shell/sh "chmod" "755" (.getPath (io/file root "src/b")))
        (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-133
(deftest an-unenumerated-refusal-kind-cannot-be-published
  ;; The reviewer's own sabotage, kept as a standing witness. A kind built
  ;; dynamically has no literal for a source scan to find, so the guard is
  ;; placed where the receipt is published rather than where it is written.
  (let [planted (keyword (str "planted" "-runtime-kind"))]
    (is (not (contains? admit/admit-refusal-kinds planted)))
    (is (thrown-with-msg?
          IllegalArgumentException #"refusal kind is not enumerated"
          (#'admit/bound-receipt
            {:ok false :operation :admit-patch-refused :mode "preview"
             :error-type planted :error "a kind nothing enumerated"}))
        "bound-receipt is outside every catch on the entrance's path")
    (is (thrown-with-msg?
          IllegalArgumentException #"refusal kind is not enumerated"
          (admit/checked-refusal-kind! {:ok false :error-type nil}))
        "a refusal with no kind at all is unenumerated too"))
  (testing "and it is an IllegalArgumentException, not an ex-info"
    ;; an ex-info carrying an :error-type is exactly what this namespace's
    ;; catch clauses turn back into a receipt; the violation would launder
    ;; itself into the surface the guard protects
    (let [thrown (try (admit/checked-refusal-kind!
                        {:ok false :error-type :nope})
                      (catch Throwable t t))]
      (is (instance? IllegalArgumentException thrown))
      (is (nil? (ex-data thrown)))))
  (testing "every enumerated kind passes, and no success is ever blocked"
    (doseq [kind admit/admit-refusal-kinds]
      (is (map? (admit/checked-refusal-kind! {:ok false :error-type kind}))
          (str "the guard rejected its own enumerated kind " kind)))
    (is (map? (admit/checked-refusal-kind! {:ok true :error-type nil})))))

;; @spec MCP-OP-ADMIT-133
(def ^:private admit-refusal-kinds-not-reachable-from-the-entrance
  "Kinds the source scan finds in the files the gate calls that the entrance
  cannot publish, each with the reason it cannot.

  This list is the complement, not the enumeration. It exists so the source
  scan stays useful -- a NEW kind constructed in one of those files is caught
  by the test below -- without letting the scan pretend to be the authority
  it demonstrably is not. Every member was driven to ground before it was
  written here:

  `clj-kondo-unavailable` and `analyzer-output-truncated` are lint-RESULT
  error types. Measured: they surface as a `detectors_not_run` reason and a
  `verification_reasons` entry, while the top-level kind is
  `verification-incomplete`. They are already correctly enumerated by
  `unverifiable-lint-error-types`.

  `patch-source-missing`, `invalid-compiled-transaction` and
  `transaction-write-exception` are defensive guards on invariants the gate
  itself establishes: `freeze-sources` supplies an entry for every parsed
  file under the same key `apply-parsed` looks up; `compiled-transaction`
  hardcodes `:ok true`; and the outer transaction catch can only see throws
  from code whose every path is already taken by an earlier `catch
  ExceptionInfo`, plus `*on-write-boundary*`, which this gate never binds.
  Each is reachable only by calling those functions directly.

  `invalid-target-path` has both branches dead on this platform: the `nil?
  parent` branch cannot fire because the lexical path is always root-anchored
  and absolute, and the catch-all needs an `InvalidPathException` for a
  string that already passed `relative-source-path?`, which rejects absolute,
  `.`, `..` and NUL. A related gap was found while proving this and is filed
  rather than papered over: a creation target with a 300-character basename
  returns `admit-tool-failure` from an escaped ENAMETOOLONG IOException,
  where a typed path refusal is what should fire."
  #{"analyzer-output-truncated"
    "clj-kondo-unavailable"
    "invalid-compiled-transaction"
    "invalid-target-path"
    "patch-source-missing"
    "transaction-write-exception"})

;; @spec MCP-OP-ADMIT-133
(deftest the-source-scan-survives-only-as-a-complement
  ;; It is no longer the enumeration; it is a tripwire on the enumeration. A
  ;; kind constructed in one of the files the gate calls must be either
  ;; enumerated or named unreachable with a reason -- never merely absent.
  (let [scanned (admit-refusal-kinds-in-source)
        enumerated (into (sorted-set) (map name) admit/admit-refusal-kinds)]
    (is (empty? (set/difference scanned enumerated
                                admit-refusal-kinds-not-reachable-from-the-entrance))
        (str "a kind is constructed in the files the admit gate calls and is "
             "neither enumerated nor justified as unreachable: "
             (pr-str (set/difference
                       scanned enumerated
                       admit-refusal-kinds-not-reachable-from-the-entrance))))
    (is (empty? (set/intersection
                  enumerated admit-refusal-kinds-not-reachable-from-the-entrance))
        "a kind cannot be both enumerated and declared unreachable")
    (is (empty? (set/difference
                  admit-refusal-kinds-not-reachable-from-the-entrance scanned))
        (str "a kind is excused as unreachable that the scan no longer even "
             "finds; delete the excuse rather than carrying it"))))

;; @spec MCP-OP-ADMIT-133
;; @spec MCP-OP-ADMIT-135
(deftest a-preview-whose-next-call-cannot-fit-refuses-through-the-entrance
  ;; The oversize refusal, driven through execute-request! rather than
  ;; constructed. expect_pre_sha256 carries one path and one digest per file,
  ;; so sixty files under deep paths put the follow-up call past the public
  ;; budget on an otherwise clean preview -- a receipt the gate would
  ;; happily have published, with a call the caller could not have sent.
  (let [root (temp-dir)
        n 60
        dir-a (apply str (repeat 200 "a"))
        dir-b (apply str (repeat 200 "b"))
        path (fn [i] (str "src/" dir-a "/" dir-b "/"
                          (apply str (repeat 200 "c")) i ".clj"))
        sources (into {} (for [i (range n)]
                           [(path i)
                            (str "(ns n" i ")\n\n(defn f\n  [x]\n  (inc x))\n")]))
        patch (apply str
                     (for [i (range n)]
                       (str "--- a/" (path i) "\n"
                            "+++ b/" (path i) "\n"
                            "@@ -1,5 +1,5 @@\n"
                            " (ns n" i ")\n \n (defn f\n   [x]\n"
                            "-  (inc x))\n+  (inc (inc x)))\n")))]
    (try
      (write-sources! root sources)
      (let [receipt (admit/execute-request!
                      (stub-config root) {:patch patch :verify "none"})]
        (is (false? (:ok receipt)))
        (is (= :next-call-exceeds-public-budget (:error-type receipt)))
        (is (> (:next_call_characters receipt) write-refusal/public-byte-budget))
        (is (= write-refusal/public-byte-budget (:public_byte_budget receipt))
            "the refusal names the one budget, not a second one of its own")
        (is (str/includes? (:remedy receipt) "fewer files")
            "and the lever that would change the answer"))
      (finally (delete-tree! root)))))
