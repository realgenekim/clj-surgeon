(ns clj-surgeon.mcp-formatter-test
  (:require
   [clj-surgeon.mcp-formatter :as formatter]
   [clj-surgeon.mcp-http-server :as http-server]
   [clojure.java.io :as io]
   [clojure.string :as str]
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
                  ["npx" (str "@chrisoakman/standard-clojure-style@"
                              formatter/formatter-version)
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

;; @spec MCP-OP-FMT-012
(deftest the-default-formatter-command-is-version-pinned
  (is (= ["npx" "@chrisoakman/standard-clojure-style@0.29.0" "fix" "{files}"]
         formatter/default-command)
      "an unpinned npx resolves whatever is newest, and the clause-normalised
       stream that bounds a scoped format was measured against one version")
  (is (= "0.29.0" formatter/formatter-version))
  (testing "the http server's default check command is pinned to the same version"
    (is (some #(= ["npx" "@chrisoakman/standard-clojure-style@0.29.0"
                   "check" "{files}"]
                  %)
              (get-in http-server/default-verification-profiles
                      ["fast" :commands]))))
  (testing "so the check counterpart is still recognised and removed"
    (is (not-any?
          #(str/includes? (str %) "standard-clojure-style")
          (get-in (formatter/verification-profiles-after-format
                    http-server/default-verification-profiles
                    formatter/default-command)
                  ["fast" :commands])))))
