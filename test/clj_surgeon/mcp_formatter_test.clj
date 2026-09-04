(ns ^{:lane :fast} clj-surgeon.mcp-formatter-test
  "Boundary witnesses over `clj-surgeon.mcp-formatter`, live in production at
   `mcp-tool.clj:16,367,778,785,790` and `mcp_http_server.clj:5,40`.

   LANE: :fast (adopted 2026-09-04, round three). It qualifies on the lane's
   own rules rather than by convenience -- it launches NO child process (the
   process runner is injected at every call site), binds no socket, touches no
   network, and stages only through `File/createTempFile`, which resolves to
   the fast lane's throwaway `java.io.tmpdir` root (TEST-ISO-006). The two
   `\"/tmp\"` arguments below are project-root STRINGS handed to an injected
   runner that never touches the filesystem; nothing is written there.

   It was an ORPHAN until round three: required by no runner and no Make
   target, three green tests over live production paths that nothing ran. The
   round-two review called that a blocking coverage loss. The class ratchet is
   `lane-manifest-test/every-exclusion-names-a-runner-that-actually-exists`:
   an exclusion must now redirect to a runner that exists, so a namespace can
   no longer be declared into orphanhood."
  (:require
   [clj-surgeon.mcp-formatter :as formatter]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-formatter-test-"
      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(deftest formatter-operates-on-staged-candidates-not-live-files
  (let [root (temp-dir)
        live-file (io/file root "src/app.clj")
        before "(ns app)\n(defn f [] :old)\n"
        candidate "(ns app)\n(defn f [] :new)\n"]
    (try
      (.mkdirs (.getParentFile live-file))
      (spit live-file before)
      (let [result
            (formatter/format-candidates!
              (.getPath root) ["format" "{files}"]
              {"src/app.clj" candidate}
              (fn [project-root command]
                (is (= (.getPath root) project-root))
                (is (= "format" (first command)))
                (is (not= (.getCanonicalPath live-file)
                          (.getCanonicalPath (io/file (second command)))))
                (spit (second command)
                      "(ns app)\n\n(defn f\n  []\n  :new)\n")
                {:finished? true :exit 0 :elapsed_ms 1.5 :output ""}))]
        (is (:ok result))
        (is (= 1 (:file-count result)))
        (is (= 1 (:changed-file-count result)))
        (is (= "(ns app)\n\n(defn f\n  []\n  :new)\n"
               (get-in result [:future-sources "src/app.clj"])))
        (is (= before (slurp live-file))))
      (finally
        (delete-tree! root)))))

(deftest formatter-refuses-invalid-command-failure-and-timeout
  (is (= :invalid-formatter-command
         (:error-type (formatter/format-candidates!
                        "/tmp" ["format"] {"a.clj" "(ns a)"}))))
  (doseq [[label process-result expected]
          [["failure" {:finished? true :exit 2 :elapsed_ms 2.0 :output "bad"}
            :formatter-failed]
           ["timeout" {:finished? false :exit nil :elapsed_ms 120000.0
                       :output "late"}
            :formatter-timeout]]]
    (testing label
      (let [result (formatter/format-candidates!
                     "/tmp" ["format" "{files}"] {"a.clj" "(ns a)"}
                     (fn [_ _] process-result))]
        (is (false? (:ok result)))
        (is (= expected (:error-type result)))
        (is (true? (:source-unchanged result)))))))

(deftest staged-formatting-removes-only-its-redundant-post-commit-check
  (let [profiles
        {"fast" {:commands
                 [["clj-kondo" "--lint" "{files}"]
                  ["npx" "@chrisoakman/standard-clojure-style"
                   "check" "{files}"]
                  ["make" "focused"]]}
         "full" ["make" "test"]}
        result (formatter/verification-profiles-after-format
                 profiles formatter/default-command)]
    (is (= [["clj-kondo" "--lint" "{files}"]
            ["make" "focused"]]
           (get-in result ["fast" :commands])))
    (is (= ["make" "test"] (get result "full")))
    (is (= profiles
           (formatter/verification-profiles-after-format
             profiles ["custom-format" "--write" "{files}"])))))
