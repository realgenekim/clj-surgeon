(ns clj-surgeon.admit-patch-round18-test
  "Round eighteen's witnesses: the overlay's own boundary.

  Round seventeen's review found three defects in the six-line copy loop that
  MCP-OP-ADMIT-153's whole contract stands on, and every one of them is a
  statement the gate makes about a tree it was blind to:

  - the copy FOLLOWED symbolic links and derived each destination from the
    link's CANONICAL target, so a workspace holding an ordinary symlink to a
    file outside it made the gate open that file for read and write at once
    and truncate it to zero -- under `preview` and `propose`, on a receipt
    publishing `mutation_attempted=false` (MCP-OP-ADMIT-158);
  - the copy carried no attributes, so a repository whose verify command is
    `./bin/check` had its own 0755 script land at 0644 in the overlay and got
    a guaranteed false refusal, reported with a TIMEOUT's status word
    (MCP-OP-ADMIT-159);
  - `output_tail` published the last lines of the FIRST 12,000 bytes of a
    command's output and called them the tail, with no truncation flag, so the
    failing assertion -- the one fact MCP-OP-ADMIT-156 exists to deliver --
    was absent for every output larger than that (MCP-OP-ADMIT-160).

  A leaf of its own for the reason round sixteen needed one: the parser node
  ceiling this repository enforces on itself.

  Every fixture here builds its own tree and tears it down WITHOUT following
  links, because a teardown that follows a link out of the fixture is the same
  defect these witnesses exist to refuse."
  (:require
   [clj-surgeon.admit-patch-test
    :refer [base-sources clean-multi-file-patch core-source stub-config
            temp-dir write-sources!]]
   [clj-surgeon.mcp-admit-tool :as admit]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]])
  (:import
   (java.nio.file Files LinkOption Path)
   (java.nio.file.attribute FileAttribute)))

(set! *warn-on-reflection* false)

(defn- path
  ^Path [file]
  (.toPath (io/file file)))

(def ^:private no-follow-opts
  (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn delete-tree-nofollow!
  "Remove a fixture tree without ever descending through a symbolic link.

  `clojure.java.io/file-seq` follows directory links, so the ordinary teardown
  used elsewhere in this suite would walk OUT of a fixture that contains the
  very link these witnesses plant -- and delete the victim it is asserting on."
  [file]
  (let [f (io/file file)]
    (when (Files/exists (path f) (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
      (when (and (Files/isDirectory (path f)
                                    (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                 (not (Files/isSymbolicLink (path f))))
        (doseq [child (or (seq (.listFiles f)) [])]
          (delete-tree-nofollow! child)))
      (Files/deleteIfExists (path f)))))

(defn- symlink!
  [link target]
  (let [l (io/file link)]
    (.mkdirs (.getParentFile l))
    (Files/createSymbolicLink (path l) (path target)
                              (make-array FileAttribute 0))))

(def ^:private victim-bytes "PRECIOUS-CONTENT-DO-NOT-TOUCH\n")

(defn- inline-verify
  [& commands]
  {:commands (vec commands)})

;; @spec MCP-OP-ADMIT-158
(deftest the-overlay-never-writes-outside-its-own-root
  (testing "a symlink to a FILE outside the workspace leaves that file untouched"
    (doseq [mode ["preview" "propose" "commit"]]
      (let [root (temp-dir)
            outside (temp-dir)]
        (try
          (write-sources! root base-sources)
          (let [victim (io/file outside "victim.txt")]
            (spit victim victim-bytes)
            (symlink! (io/file root "innocent-link.txt") victim)
            (let [before (.length victim)
                  result (admit/execute-request!
                           (stub-config root {:admit-test-runner nil})
                           {:patch clean-multi-file-patch
                            :mode mode
                            :verify (inline-verify "true")})
                  after (.length victim)]
              (is (= before after 30)
                  (str "mode " mode ": the overlay wrote outside its own root:"
                       " victim bytes " before " -> " after))
              (is (= victim-bytes (slurp victim))
                  (str "mode " mode ": the victim's bytes are unchanged"))
              (is (false? (:ok result))
                  (str "mode " mode ": an entry pointing outside the workspace"
                       " is refused by name, not silently dereferenced: "
                       (pr-str (select-keys result [:ok :error-type]))))
              (is (= :inline-verify-overlay-escape (:error-type result))
                  (pr-str (:error-type result)))
              (is (str/includes? (str (:error result)) "innocent-link.txt")
                  (str "the refusal names the entry AS SPELLED: "
                       (pr-str (:error result))))
              (is (= core-source (slurp (io/file root "src/app/core.clj")))
                  "the workspace itself is unchanged")))
          (finally (delete-tree-nofollow! root)
                   (delete-tree-nofollow! outside))))))
  (testing "a symlink to a DIRECTORY outside the workspace leaves it whole"
    (let [root (temp-dir)
          outside (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [secrets (io/file outside "secrets")
              files (mapv (fn [n]
                            (let [f (io/file secrets (str "secret-" n ".txt"))]
                              (.mkdirs (.getParentFile f))
                              (spit f victim-bytes)
                              f))
                          (range 5))]
          (symlink! (io/file root "vendor") secrets)
          (let [result (admit/execute-request!
                         (stub-config root {:admit-test-runner nil})
                         {:patch clean-multi-file-patch
                          :mode "propose"
                          :verify (inline-verify "true")})]
            (doseq [f files]
              (is (= victim-bytes (slurp f))
                  (str "every file beneath the linked directory is untouched: "
                       (.getName f) " is " (.length f) " bytes")))
            (is (false? (:ok result)))
            (is (= :inline-verify-overlay-escape (:error-type result)))
            (is (str/includes? (str (:error result)) "vendor")
                (pr-str (:error result)))))
        (finally (delete-tree-nofollow! root)
                 (delete-tree-nofollow! outside)))))
  (testing "a symlink CYCLE inside the workspace terminates rather than hangs"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (symlink! (io/file root "loop-a") (io/file root "loop-b"))
        (symlink! (io/file root "loop-b") (io/file root "loop-a"))
        (let [answer (promise)
              worker (future
                       (deliver answer
                                (admit/execute-request!
                                  (stub-config root {:admit-test-runner nil})
                                  {:patch clean-multi-file-patch
                                   :mode "propose"
                                   :verify (inline-verify "true")})))
              result (deref answer 60000 ::timed-out)]
          (future-cancel worker)
          (is (not= ::timed-out result)
              "the walk terminates on a symlink cycle rather than hanging"))
        (finally (delete-tree-nofollow! root)))))
  (testing "a symlink INSIDE the workspace is recreated as a link, not followed"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (symlink! (io/file root "alias.clj") (io/file root "src/app/util.clj"))
        (let [result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "propose"
                        :verify (inline-verify
                                  ["sh" "-c" "test -L alias.clj && test -f alias.clj"])})]
          (is (true? (:ok result)) (pr-str (:error result)))
          (is (true? (:verify_ok result))
              (str "the overlay copy of an in-workspace link is still a link: "
               (pr-str (get-in result [:tests :commands])))))
        (finally (delete-tree-nofollow! root))))))

;; @spec MCP-OP-ADMIT-159
(deftest the-overlay-preserves-the-modes-and-times-the-verify-runs-against
  (testing "a repository whose verify command is its own ./bin/check passes"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [check (io/file root "bin/check")]
          (.mkdirs (.getParentFile check))
          (spit check "#!/bin/sh\necho CHECK-OK\nexit 0\n")
          (.setExecutable check true false)
          (let [result (admit/execute-request!
                         (stub-config root {:admit-test-runner nil})
                         {:patch clean-multi-file-patch
                          :mode "commit"
                          :verify (inline-verify ["./bin/check"])})
                row (first (get-in result [:tests :commands]))]
            (is (true? (:ok result))
                (str "the repository's own executable script runs inside the"
                     " overlay: " (pr-str (:error result))))
            (is (= 0 (long (or (:exit row) -1)))
                (pr-str row))
            (is (str/includes? (str (:output_tail row)) "CHECK-OK")
                (pr-str row))))
        (finally (delete-tree-nofollow! root)))))
  (testing "the copy carries the source's mode and modification time"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [check (io/file root "bin/check")]
          (.mkdirs (.getParentFile check))
          (spit check "#!/bin/sh\nexit 0\n")
          (.setExecutable check true false)
          ;; A time far enough in the past that a copy stamped "now" cannot
          ;; coincide with it. Without this the witness is a race: on a fast
          ;; box the copy and the write can land in the same millisecond.
          (doseq [f [check (io/file root "src/app/core.clj")
                     (io/file root "src/app/util.clj")]]
            (.setLastModified f 1000000000000))
          (let [overlay (admit/overlay-snapshot! (.getPath root) [])
                copy (io/file (:root overlay) "bin/check")
                source (io/file root "src/app/core.clj")
                source-copy (io/file (:root overlay) "src/app/core.clj")]
            (is (true? (:ok overlay)) (pr-str overlay))
            (is (true? (.canExecute copy))
                (str "the overlay copy of a 0755 script is executable;"
                     " source perms "
                     (pr-str (Files/getPosixFilePermissions (path check)
                                                            no-follow-opts))
                     " copy perms "
                     (pr-str (Files/getPosixFilePermissions (path copy)
                                                            no-follow-opts))))
            (is (= (Files/getPosixFilePermissions (path check) no-follow-opts)
                   (Files/getPosixFilePermissions (path copy) no-follow-opts))
                "every permission bit survives the copy, not only the x bit")
            (is (= (.lastModified source) (.lastModified source-copy))
                (str "an incremental verify sees the tree's own times, not"
                     " 'now': source " (.lastModified source)
                     " copy " (.lastModified source-copy)))))
        (finally (delete-tree-nofollow! root)))))
  (testing "a command that cannot be LAUNCHED is not reported as a deadline"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [check (io/file root "bin/check")]
          (.mkdirs (.getParentFile check))
          (spit check "#!/bin/sh\nexit 0\n")
          (.setExecutable check false false)
          (let [result (admit/execute-request!
                         (stub-config root {:admit-test-runner nil})
                         {:patch clean-multi-file-patch
                          :mode "commit"
                          :verify (inline-verify ["./bin/check"])})
                row (first (get-in result [:tests :commands]))]
            (is (false? (:ok result)))
            (is (= "did-not-start" (:status row))
                (str "a launch failure has its own status word: a reader who"
                     " sees a timeout's shape concludes the suite hung: "
                     (pr-str row)))
            (is (= :inline-verify-command-did-not-start
                   (get-in result [:tests :reason]))
                (pr-str (get-in result [:tests :reason])))
            (is (str/includes? (str (:error result)) "Permission denied")
                (str "the refusal carries the launcher's own error text: "
                     (pr-str (:error result))))
            (is (not (str/includes? (str (:error result)) "did not finish"))
                (pr-str (:error result)))))
        (finally (delete-tree-nofollow! root)))))
  (testing "a verify cwd that does not exist is a launch failure, not a deadline"
    (let [root (temp-dir)]
      (try
        (write-sources! root base-sources)
        (let [result (admit/execute-request!
                       (stub-config root {:admit-test-runner nil})
                       {:patch clean-multi-file-patch
                        :mode "commit"
                        :verify {:commands ["true"] :cwd "no/such/dir"}})
              row (first (get-in result [:tests :commands]))]
          (is (false? (:ok result)))
          (is (= "did-not-start" (:status row)) (pr-str row))
          (is (= :inline-verify-command-did-not-start
                 (get-in result [:tests :reason]))
              (pr-str (get-in result [:tests :reason]))))
        (finally (delete-tree-nofollow! root))))))

;; @spec MCP-OP-ADMIT-156
;; @spec MCP-OP-ADMIT-160
(deftest output-tail-is-the-tail-of-the-output-and-says-when-it-cut
  ;; The assertion's last token is COMPUTED by the shell rather than written
  ;; in the command, so that neither face can pass by echoing the caller's own
  ;; argv back: the receipt carries the command verbatim, and a witness that
  ;; searched for a literal present in it would be vacuous.
  (let [assertion "FAIL in (handle-tick-test) expected 1 actual 2"
        noisy (str "i=0; while [ $i -lt 4000 ]; do"
                   " echo \"noise line $i padding padding padding padding\";"
                   " i=$((i+1)); done;"
                   " echo \"FAIL in (handle-tick-test) expected 1 actual"
                   " $((1+1))\"; exit 5")]
    (testing "the failing assertion survives 4,000 lines of noise before it"
      (let [root (temp-dir)]
        (try
          (write-sources! root base-sources)
          (let [result (admit/execute-request!
                         (stub-config root {:admit-test-runner nil})
                         {:patch clean-multi-file-patch
                          :mode "propose"
                          :verify (inline-verify ["sh" "-c" noisy])})
                row (first (get-in result [:tests :commands]))
                tail (str (:output_tail row))]
            (is (true? (:ok result)) (pr-str (:error result)))
            (is (false? (:verify_ok result)))
            (is (= 5 (long (:exit row))) (pr-str (dissoc row :output_tail)))
            (is (str/ends-with? (str/trimr tail) assertion)
                (str "output_tail must END with the command's last line."
                     " Its first line is "
                     (pr-str (first (str/split-lines tail)))
                     " and its last is "
                     (pr-str (last (str/split-lines tail)))))
            (is (str/includes? tail assertion)
                "the one fact that makes the next edit possible")
            (is (true? (:output_truncated row))
                (str "a cut output says it was cut: "
                     (pr-str (dissoc row :output_tail))))
            (is (and (number? (:output_bytes row))
                     (< 12000 (long (:output_bytes row))))
                (str "the row names how much output there was: "
                     (pr-str (:output_bytes row))))
            (is (and (number? (:output_omitted_bytes row))
                     (pos? (long (:output_omitted_bytes row))))
                (str "and how much of it the receipt omitted: "
                     (pr-str (:output_omitted_bytes row))))
            (is (= (long (:output_bytes row))
                   (+ (long (:output_omitted_bytes row))
                      (count (.getBytes tail "UTF-8"))))
                "the omitted count and the published tail account for all of it"))
          (finally (delete-tree-nofollow! root)))))
    (testing "the text face spells the same tail structuredContent carries"
      (let [root (temp-dir)]
        (try
          (write-sources! root base-sources)
          (let [config-atom (deref #'admit/runtime-config)
                previous @config-atom
                captured (atom nil)]
            (try
              (reset! config-atom (stub-config root {:admit-test-runner nil}))
              (admit/handle-admit-clojure-patch
                nil
                {"patch" clean-multi-file-patch
                 "mode" "commit"
                 "verify" {"commands" [["sh" "-c" noisy]]}}
                (fn [content _error? _result]
                  (reset! captured (str (first content)))))
              (is (str/includes? (str @captured) assertion)
                  (str "the text face carries the failing assertion, not the"
                       " head of the noise: "
                       (pr-str (subs (str @captured)
                                     0 (min 400 (count (str @captured)))))))
              (finally (reset! config-atom previous))))
          (finally (delete-tree-nofollow! root)))))))
