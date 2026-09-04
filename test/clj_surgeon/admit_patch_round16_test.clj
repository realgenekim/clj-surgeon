(ns clj-surgeon.admit-patch-round16-test
  "Round sixteen's witnesses: the admit gate's INPUT CONTRACT, measured.

  A leaf of its own rather than more lines in `clj-surgeon.admit-patch-test`,
  and not for tidiness: `default-ceilings-admit-every-source-in-this-repository`
  requires every source in this repository to sit inside a 4x margin of the
  shipped parser node ceiling, and round sixteen's additions took
  `admit_patch_test.clj` to 52,127 nodes against a 200,000 ceiling -- 208,508
  at 4x, red by 8,508. The gate found it; the fix is a second file, which is
  what the gate was asking for.

  Fixtures and the stub config come from `clj-surgeon.admit-patch-test`, so
  both leaves refuse the same patches against the same sources.

  Origin: the 2026-09-04 arm-G replay, in which the gate was mandated as the
  write path on a real repository with no verification profile, refused every
  call on `no-focused-test-profile`, and ran 2.6-2.8x native's wall."
  (:require
   [clj-surgeon.admit-patch-test
    :refer [base-sources clean-multi-file-patch core-source delete-tree!
            stub-config temp-dir write-sources!]]
   [clj-surgeon.mcp-admit-tool :as admit]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))


;; @spec MCP-OP-ADMIT-153
;; @spec MCP-OP-ADMIT-154
(deftest a-workspace-with-no-verification-profile-is-admissible-in-one-call
  (testing "the missing-profile refusal begins with the words and names the call"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (false? (:ok result)))
          (is (= :verification-incomplete (:error-type result)))
          (is (= :no-focused-test-profile (get-in result [:tests :reason])))
          (is (str/starts-with? (str (:error result))
                                "this workspace has no verification profile")
              (str "the refusal's first words name the state, not an internal "
                   "repair verb: " (pr-str (:error result))))
          (is (str/includes? (str (:error result)) "\"commands\"")
              "the refusal spells the one call that supplies the profile")
          (is (map? (get-in result [:next_call :arguments :verify]))
              "the follow-up proposes the inline shape, not the word that failed")
          (is (= core-source (slurp (io/file root "src/app/core.clj")))))
        (finally (delete-tree! root)))))
  (testing "the caller's own commands run inside the snapshot and admit the patch"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [marker "GATE16-INLINE-MARKER"
              result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "commit"
                        :verify {:commands
                                 [["sh" "-c"
                                   (str "grep -q 'fnil inc 0' src/app/core.clj"
                                        " && echo " marker)]
                                  "true"]}})]
          (is (:ok result) (pr-str (:error result)))
          (is (true? (:committed result)))
          (is (true? (:verification_complete result)))
          (is (= "inline" (get-in result [:tests :verify_mode])))
          (let [rows (get-in result [:tests :commands])]
            (is (= 2 (count rows)) (pr-str rows))
            (is (zero? (long (:exit (first rows)))))
            (is (str/includes? (str (:output_tail (first rows))) marker)
                "the receipt carries the command's own last lines verbatim")
            (is (str/includes? (str (:command (first rows))) "fnil inc 0")
                "the receipt names each command as the caller gave it"))
          (is (str/includes? (slurp (io/file root "src/app/core.clj"))
                             "(fnil inc 0)")))
        (finally (delete-tree! root)))))
  (testing "a command that verifies the WORKSPACE rather than the snapshot fails"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "commit"
                        :verify {:commands
                                 [["sh" "-c"
                                   "grep -q ':ticks inc)' src/app/core.clj"]]}})]
          (is (false? (:ok result))
              "the pre-image text is gone from the snapshot the commands see")
          (is (false? (:committed result)))
          (is (= core-source (slurp (io/file root "src/app/core.clj")))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-155
(deftest the-harness-own-patch-shape-needs-no-caller-digests
  (let [v4a (str "*** Begin Patch\n"
                 "*** Update File: src/app/core.clj\n"
                 "@@\n"
                 "-  (update state :ticks inc))\n"
                 "+  (update state :ticks (fnil inc 0)))\n"
                 "*** End Patch\n")
        git-shape (str "diff --git a/src/app/core.clj b/src/app/core.clj\n"
                       "index 1111111..2222222 100644\n"
                       "--- a/src/app/core.clj\n"
                       "+++ b/src/app/core.clj\n"
                       "@@ -4,7 +4,7 @@\n"
                       " \n"
                       " (defn handle-tick\n"
                       "   [state]\n"
                       "-  (update state :ticks inc))\n"
                       "+  (update state :ticks (fnil inc 0)))\n"
                       " \n"
                       " (defn label\n"
                       "   [state]\n")]
    (doseq [[label patch] [["V4A" v4a] ["diff --git" git-shape]]]
      (testing (str label " admits with zero digests supplied")
        (let [root (temp-dir)]
          (try
            (write-sources! root base-sources)
            (let [result (admit/execute-request!
                           (stub-config root)
                           {:patch patch :mode "commit" :verify "focused"
                            :allow_partial true})]
              (is (:ok result) (pr-str (:error result)))
              (is (true? (:committed result)))
              (is (= "derived" (:pre_image_binding result))
                  (str "the gate froze and re-read the pre-image itself; "
                       "`unbound` read as no protection and cost the field "
                       "agent four to five minutes of digest arithmetic"))
              (is (str/includes? (slurp (io/file root "src/app/core.clj"))
                                 "(fnil inc 0)")))
            (finally (delete-tree! root)))))))
  (testing "a tree that moves under a digest-free commit still refuses by name"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [drifted (str core-source "\n;; a competing seat\n")
              result (admit/execute-request!
                       (assoc (stub-config root)
                              :admit-before-commit!
                              (fn [] (spit (io/file root "src/app/core.clj")
                                           drifted)))
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused" :allow_partial true})]
          (is (false? (:ok result)))
          (is (= :source-hash-mismatch (:error-type result)))
          (is (true? (:source-unchanged result)))
          (is (= ["src/app/core.clj"] (mapv :file (:drifted result)))
              "the typed stale-snapshot refusal names the file that moved")
          (is (str/includes? (str (:error result)) "src/app/core.clj"))
          (is (= drifted (slurp (io/file root "src/app/core.clj")))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-156
(deftest a-failing-verification-carries-the-failing-commands-own-lines
  (testing "an inline command that fails hands back its exit code and lines"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [assertion "FAIL in (handle-tick-test) expected 1 actual 2"
              result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "commit"
                        :verify {:commands [["sh" "-c"
                                             (str "echo '" assertion "' >&2;"
                                                  " exit 3")]]}})]
          (is (false? (:ok result)))
          (is (= :verification-failed (:error-type result)))
          (is (= 3 (:failing_command_exit result)))
          (is (str/includes? (str (:failing_command_output_tail result))
                             assertion))
          (is (str/includes? (str (:error result)) assertion)
              "the failing assertion's own line is in the refusal sentence")
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))
          ;; @spec MCP-OP-ADMIT-134
          (let [config-atom (deref #'admit/runtime-config)
                previous @config-atom
                captured (atom nil)]
            (try
              (reset! config-atom (stub-config root {:admit-test-runner nil}))
              (admit/handle-admit-clojure-patch
                nil
                {"patch" clean-multi-file-patch
                 "mode" "commit"
                 "verify" {"commands" [["sh" "-c"
                                        (str "echo '" assertion "' >&2;"
                                             " exit 3")]]}}
                (fn [content _error? _result]
                  (reset! captured (str (first content)))))
              (is (str/includes? (str @captured) assertion)
                  "text face spells what structuredContent carries")
              (finally (reset! config-atom previous)))))
        (finally (delete-tree! root)))))
  (testing "a repository-declared runner that FAILS also carries its lines"
    (let [root (temp-dir)]
      (try
        (write-sources! root (assoc base-sources
                                    "test/app/core_test.clj"
                                    "(ns app.core-test)\n"))
        (let [assertion "FAIL in (core-test) expected :a actual :b"
              command ["sh" "-c"
                       (str "echo '" assertion "' >&2;"
                            " printf '{\"app.core-test\" {:tests 1"
                            " :failures 1 :errors 0}}' > \"$2\"; exit 1")
                       "focused-runner" "{snapshot}" "{report}"]
              result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil
                                          :focused-test {:command command}})
                       {:patch clean-multi-file-patch
                        :mode "commit" :verify "focused"})]
          (is (false? (:ok result)))
          (is (= :verification-failed (:error-type result)))
          (is (str/includes? (str (:failing_command_output_tail result))
                             assertion))
          (is (str/includes? (str (:error result)) assertion))
          (is (= core-source (slurp (io/file root "src/app/core.clj")))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-ADMIT-157
(deftest propose-runs-the-verification-writes-nothing-and-never-refuses-for-it
  (let [tree-hash (fn [root]
                    (mapv (fn [file] [(.getPath file) (slurp file)])
                          (sort-by #(.getPath %)
                                   (filter #(.isFile %) (file-seq root)))))]
    (testing "a failing verification is a FIELD in propose, not a refusal"
      (let [root (temp-dir)]
        (try
          (write-sources! root base-sources)
          (let [before (tree-hash root)
                result (admit/execute-request!
                         (stub-config root {:admit-test-runner nil})
                         {:patch clean-multi-file-patch
                          :mode "propose"
                          :verify {:commands [["sh" "-c"
                                               "echo BROKEN >&2; exit 7"]]}})]
            (is (true? (:ok result)) (pr-str (:error result)))
            (is (= :admit-patch-proposed (:operation result)))
            (is (= "propose" (:mode result)))
            (is (false? (:verify_ok result)))
            (is (false? (:committed result)))
            (is (false? (:mutation_attempted result)))
            (is (= 7 (:failing_command_exit result)))
            (is (str/includes? (str (:failing_command_output_tail result))
                               "BROKEN"))
            (is (= before (tree-hash root))
                "propose wrote nothing: the tree is byte-identical")
            (is (= "commit" (get-in result [:next_call :arguments :mode]))
                "the loop's next hop is named on the receipt"))
          (finally (delete-tree! root)))))
    (testing "a passing verification proposes without writing either"
      (let [root (temp-dir)]
        (try
          (write-sources! root base-sources)
          (let [before (tree-hash root)
                result (admit/execute-request!
                         (stub-config root {:admit-test-runner nil})
                         {:patch clean-multi-file-patch
                          :mode "propose"
                          :verify {:commands ["true"]}})]
            (is (true? (:ok result)) (pr-str (:error result)))
            (is (= :admit-patch-proposed (:operation result)))
            (is (true? (:verify_ok result)))
            (is (false? (:committed result)))
            (is (= before (tree-hash root))))
          (finally (delete-tree! root)))))))

;; @spec MCP-OP-ADMIT-153
;; @spec MCP-OP-ADMIT-155
;; @spec MCP-OP-ADMIT-157
(deftest the-tool-description-answers-a-naive-readers-next-call
  (let [description admit/admit-tool-description]
    (testing "it stays inside the catalog's description budget"
      (is (<= (count description) 2600)
          (str "description is " (count description) " characters")))
    (doseq [[what fragment]
            [["the patch is the harness's own text" "apply_patch"]
             ["both grammars" "*** Begin Patch"]
             ["and the git one" "diff --git"]
             ["digests are never required" "never compute digests"]
             ["the derived binding" "pre_image_binding: derived"]
             ["the propose loop" "propose"]
             ["the loop is spelled out" "read the failing lines"]
             ["inline verify is spelled as JSON" "\"commands\""]
             ["with a real example" "make test"]
             ["the venue is stated" "with your patch"]
             ["a failing command blocks the write" "blocks the write"]
             ;; @spec MCP-OP-SHELL-ARGV-001
             ["no shell is ever spawned for you" "NEVER handed to"]
             ["and the escape hatch is named" "[\"sh\", \"-c\""]]]
      (testing what
        (is (str/includes? description fragment))))))

;; @spec MCP-OP-ADMIT-153
(deftest an-overlay-past-the-ceiling-is-a-named-reason-not-a-silent-green
  (let [root (temp-dir)]
    (try
      (write-sources! root base-sources)
      (with-redefs [admit/inline-overlay-max-files 0]
        (let [result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "commit"
                        :verify {:commands ["true"]}})]
          (is (false? (:ok result)))
          (is (= :verification-incomplete (:error-type result)))
          (is (= :inline-verify-workspace-too-large
                 (get-in result [:tests :reason]))
              "the ceiling is a NAMED reason, never a check quietly skipped")
          (is (false? (:verification_complete result)))
          (is (str/includes? (str (get-in result [:tests :overlay_error]))
                             "ceiling"))
          (is (= core-source (slurp (io/file root "src/app/core.clj"))))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-153
;; @spec MCP-OP-SHELL-ARGV-001
(deftest an-inline-command-string-is-split-never-handed-to-a-shell
  (testing "a plain command line costs the caller nothing"
    (is (= ["make" "test"] (admit/inline-command-argv "make test")))
    (is (= ["clojure" "-M:test"] (admit/inline-command-argv " clojure  -M:test ")))
    (is (= ["npx" "jest"] (admit/inline-command-argv "npx jest"))))
  (testing "an array is exec'd exactly as given, shell or not -- the caller's own"
    (is (= ["sh" "-c" "make test | tee log"]
           (admit/inline-command-argv ["sh" "-c" "make test | tee log"]))))
  (testing "a string that would NEED a shell is refused, never reduced"
    (doseq [line ["make test | tee log"
                  "make test && make lint"
                  "make test; rm -rf /"
                  "echo $(whoami)"
                  "echo `whoami`"
                  "TMPDIR=/var/tmp/forge make runtests-unit"
                  "cat *.clj"]]
      (testing line
        (is (nil? (admit/inline-command-argv line))
            "no clj-surgeon code path turns a caller string into sh -c"))))
  (testing "and the refusal names the array form"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "commit"
                        :verify {:commands ["make test | tee log"]}})]
          (is (false? (:ok result)))
          (is (= :invalid-admit-request (:error-type result)))
          (is (str/includes? (str (:error result)) "array"))
          (is (str/includes? (str (:error result)) "sh"))
          (is (= core-source (slurp (io/file root "src/app/core.clj")))))
        (finally (delete-tree! root)))))
  (testing "a string command really does run, through exec"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "commit"
                        :verify {:commands ["grep -q (fnil src/app/core.clj"]}})]
          ;; `grep -q (fnil src/app/core.clj` has no metacharacter our set
          ;; names except the paren, so this is the REFUSAL arm on purpose:
          ;; the point is that the split never became a shell either way.
          (is (false? (:ok result))))
        (finally (delete-tree! root)))))
  (testing "a metacharacter-free string command runs and passes"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "commit"
                        :verify {:commands ["grep -q fnil src/app/core.clj"]}})]
          (is (:ok result) (pr-str (:error result)))
          (is (true? (:committed result)))
          (is (= ["grep" "-q" "fnil" "src/app/core.clj"]
                 (:argv (first (get-in result [:tests :commands]))))))
        (finally (delete-tree! root))))))
