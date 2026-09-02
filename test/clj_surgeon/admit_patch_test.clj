(ns clj-surgeon.admit-patch-test
  "Witness tests for the admit_clojure_patch form-identity gate.

  Fixtures are literal source pairs and real unified diffs generated from
  those pairs with `diff -u`, so every hunk header in this file is arithmetic
  a patch producer actually emitted rather than a hand-counted guess."
  (:require
   [clj-surgeon.form-identity :as form-identity]
   [clj-surgeon.mcp-admit-tool :as admit]
   [clj-surgeon.mcp-server :as server]
   [clj-surgeon.mcp-tool :as tool]
   [clj-surgeon.mcp-write-refusal :as write-refusal]
   [clj-surgeon.patch-apply :as patch-apply]
   [clj-surgeon.workspace-lock :as workspace-lock]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
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
            "transform_clojure" "admit_clojure_patch"]
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
                     {:patch escape :mode "commit" :verify "none"})]
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
                          :mode mode :verify "none"})]
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
                     {:patch stale-context-patch :mode "commit" :verify "none"})]
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

;; @spec MCP-OP-ADMIT-014
(deftest refuses-whole-file-creation-and-deletion
  (let [parsed (patch-apply/parse-patch file-creation-patch)]
    (is (:ok parsed) "the construct parses; the policy refuses it")
    (is (= :add (:operation (first (:files parsed))))
        "a /dev/null source names a creation in either grammar"))
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch file-creation-patch :mode "commit" :verify "none"})]
        (is (false? (:ok result)))
        (is (= :unsupported-patch-operation (:error-type result)))
        (is (not (.exists (io/file root "src/app/new.clj")))))
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
(deftest verification-none-runs-nothing-and-claims-nothing
  (let [root (temp-dir)
        touched (atom false)]
    (try
      (write-sources! root base-sources)
      (let [result (admit/execute-request!
                     (stub-config root
                                  {:admit-lint-runner
                                   (fn [_ _] (reset! touched true) {:ran true :ok true})
                                   :admit-test-runner
                                   (fn [_ _] (reset! touched true) {:ran true})})
                     {:patch clean-multi-file-patch
                      :mode "commit" :verify "none"})]
        (is (:ok result))
        (is (true? (:committed result)))
        (is (false? @touched))
        (is (false? (get-in result [:lint_delta :ran])))
        (is (false? (get-in result [:tests :ran])))
        (is (false? (:verification_complete result))))
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
  #{:ok :operation :mode :committed :files :owners :protected_node_drift
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
                          :mode "commit" :verify "none"}]
               ["hazard refusal" {:patch duplicate-definition-patch
                                  :mode "commit" :verify "none"}]]]
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
                      :mode "commit" :verify "none"})]
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
                      :mode "commit" :verify "none"})]
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
                      :mode "commit" :verify "none"})]
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
                       :mode "commit" :verify "none"}]]
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
                      {:patch stale-context-patch :mode "commit" :verify "none"}
                      {:patch file-creation-patch :mode "commit" :verify "none"}
                      {:patch duplicate-definition-patch
                       :mode "commit" :verify "none"}]]
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
                           {:patch patch :mode "commit" :verify "none"})]
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
                            :mode mode :verify "none"})]
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
                         {:patch patch :mode "commit" :verify "none"})]
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
                      :mode "commit" :verify "none"})]
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
                          :mode "commit" :verify "none"})]
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
                          :verify "none" :expect_pre_sha256 binding})]
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
                     {:patch patch :mode "commit" :verify "none"})]
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
                         {:patch huge :mode "commit" :verify "none"})]
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
          (is (:ok result) "no check failed, so the commit is allowed")
          (is (true? (:committed result)))
          (is (false? (:verification_complete result))
              "process exit status is not a test result")
          (is (= ["app.core-test"] (get-in result [:tests :namespaces])))
          (is (= :no-test-evidence (get-in result [:tests :reason])))))
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
                     {:patch patch :mode "commit" :verify "none"})
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
                      :verify "none"
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
          (is (= :no-test-evidence (get-in result [:tests :reason])))))
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
      (let [from-server (admit/resolve-focused-test
                          {:project-root (.getPath root)
                           :focused-test {:command ["server" "{snapshot}"
                                                    "{report}" "{namespaces}"]}})]
        (is (= "server" (first (:command from-server))))
        (is (= :server-config (:profile-source from-server))
            "the start configuration outranks the repository file"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-082
(deftest the-receipt-states-which-requested-checks-produced-a-result
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (testing "no requested check produced a result: committed, ok false"
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :admit-lint-runner
                        (fn [_ _] {:ran false :ok false :status :unverified
                                   :error-type :clj-kondo-unavailable})}
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (= :unverified (:verification_status result)))
          (is (true? (:committed result)) "the write still happened")
          (is (false? (:ok result))
              "the caller asked for verification and did not get any")
          (is (= :verification-unverified (:error-type result)))
          (is (= [:clj-kondo-unavailable :no-focused-test-profile]
                 (:verification_reasons result)))))
      (write-sources! root base-sources)
      (testing "one check ran clean and one could not: partial, ok true"
        (let [result (admit/execute-request!
                       {:project-root (.getPath root)
                        :admit-lint-runner (fn [_ _] {:ran true :ok true})}
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (= :partial (:verification_status result)))
          (is (true? (:committed result)))
          (is (:ok result))
          (is (false? (:verification_complete result)))))
      (write-sources! root base-sources)
      (testing "verification was not requested"
        (let [result (admit/execute-request!
                       (stub-config root)
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "none"})]
          (is (= :unverified (:verification_status result)))
          (is (= [:verification-not-requested] (:verification_reasons result)))
          (is (:ok result) "nothing was asked for, so nothing is owed")))
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
  (let [pre (str "(ns app.a\n  (:require\n"
                 "   [clojure.set :refer [union difference]]))\n\n"
                 "(defn f [] (union))\n")
        post (str "(ns app.a\n  (:require\n"
                  "   [clojure.set :refer [union]]))\n\n"
                  "(defn f [] (union))\n")
        delta (form-identity/form-identity-delta
                {:file "src/app/a.clj" :pre pre :post post
                 :hunk-spans {:pre [[1 5]] :post [[1 5]]}})
        hazard (first (filter #(= :require-removed (:type %)) (:hazards delta)))]
    (is (some? hazard))
    (is (= :refusal (:class hazard)))
    (is (= [{:library "clojure.set" :symbols ["difference"]}]
           (:referred-symbols-removed hazard))
        "the lost symbol is named, not just the library it came from")))

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
                       {:patch patch :mode "commit" :verify "none"})]
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
          (is (true? (:committed result))
              "a check that could not be trusted still does not block the write")))
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
                          :mode "commit" :verify "none"})]
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
                        :mode "commit" :verify "none"})]
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
                        :mode "commit" :verify "none"})]
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
(deftest whole-file-constructs-are-parsed-then-refused-by-name
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
        (is (:ok parsed) "the construct parses; the policy refuses it")
        (is (= expected (:operation (first (:files parsed))))))
      (let [root (temp-dir)]
        (try
          (write-sources! root base-sources)
          (let [result (admit/execute-request!
                         (stub-config root)
                         {:patch patch :mode "commit" :verify "none"})]
            (is (false? (:ok result)))
            (is (= :unsupported-patch-operation (:error-type result)))
            (is (= :apply-patch (:grammar result)))
            (is (= expected (:operation (first (:unsupported result))))
                "the refusal names the construct, not a parse failure")
            (is (not (.exists (io/file root "src/app/new.clj"))))
            (is (= util-source (slurp (io/file root "src/app/util.clj")))))
          (finally (delete-tree! root)))))))

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
(deftest a-hunk-body-that-overruns-its-header-is-refused-not-truncated
  (let [source "(ns app.a)\n\n(defn one\n  [s]\n  (inc s))\n\n(defn two [s] s)\n"
        ;; The header admits one removed line; the body removes three. Trusting
        ;; the header applied a truncated hunk and dropped the rest, leaving an
        ;; owner cut off mid-form -- the self-inflicted unreadable post image.
        miscounted (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                        "@@ -3,1 +3,0 @@\n"
                        "-(defn one\n"
                        "-  [s]\n"
                        "-  (inc s))\n")
        applied (patch-apply/apply-patch {"src/app/a.clj" source} miscounted)]
    (is (false? (:ok applied)))
    (is (= :hunk-body-overruns-header (:error-type applied)))
    (is (= "-  [s]" (:offending-line applied)))
    (is (= "@@ -3,1 +3,0 @@" (:header applied)))
    (is (true? (:source-unchanged applied))))
  (testing "a header that is merely generous still applies"
    (let [source "(ns app.a)\n\n(def x 1)\n"
          patch (str "--- a/src/app/a.clj\n+++ b/src/app/a.clj\n"
                     "@@ -3,1 +3,1 @@\n-(def x 1)\n+(def x 2)\n")]
      (is (:ok (patch-apply/apply-patch {"src/app/a.clj" source} patch))))))

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
      (git "init" "-q" ".")
      (git "add" "-A")
      (git "commit" "-qm" "base")
      (is (str/blank? (:out (git "status" "--short")))
          "the fixture starts clean")
      (let [result (admit/execute-request!
                     (stub-config root)
                     {:patch clean-multi-file-patch
                      :mode "commit" :verify "none"})
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
