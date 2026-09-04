(ns clj-surgeon.failure-report-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.failure-report :as report]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(def private-receipt
  {:operation :clj-surgeon-recover
   :terminal-state :fallback-safe
   :error-type :invalid-mcp-session
   :agent-session-restart-required false
   :next-action :report-failure-and-use-cli-fallback
   :workspace "/Users/person/private/project"
   :source "(def secret :value)"
   :prompt "private prompt"
   :url "http://127.0.0.1:7890/mcp"})

(deftest issue-drafts-omit-source-prompts-urls-and-paths
  (let [draft (report/issue-draft private-receipt)
        serialized (pr-str draft)]
    (is (= :invalid-mcp-session
           (:error-type (report/sanitized-failure private-receipt))))
    (is (not (str/includes? serialized "/Users/person")))
    (is (not (str/includes? serialized "def secret")))
    (is (not (str/includes? serialized "private prompt")))
    (is (not (str/includes? serialized "127.0.0.1")))
    (is (= (report/failure-fingerprint private-receipt)
           (report/failure-fingerprint
             (assoc private-receipt :workspace "/different/private/path"))))))

(deftest report-failure-deduplicates-a-local-bead-by-fingerprint
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "clj-surgeon-report-dedupe"
                 (make-array java.nio.file.attribute.FileAttribute 0)))
        commands (atom [])
        runner
        (fn [_ command]
          (swap! commands conj command)
          (if (some #{"list"} command)
            {:exit 0 :out (json/generate-string [{:id "clj-surgeon-existing"}])}
            {:exit 0 :out (json/generate-string {:id "clj-surgeon-existing"})}))]
    (try
      (.mkdirs (io/file root ".beads"))
      (let [result (report/report-failure! {:receipt private-receipt
                                            :tool-root (.getPath root)
                                            :runner runner})
            serialized (pr-str @commands)]
        (is (:reported result))
        (is (:deduplicated result))
        (is (= "clj-surgeon-existing" (:issue-id result)))
        (is (some #{"update"} (second @commands)))
        (is (not (str/includes? serialized "/Users/person")))
        (is (not (str/includes? serialized "private prompt"))))
      (finally
        (doseq [file (reverse (file-seq root))]
          (.delete file))))))

(deftest off-laptop-reporting-returns-data-without-writing
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "clj-surgeon-report-draft"
                 (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (let [result (report/report-failure! {:receipt private-receipt
                                             :tool-root (.getPath root)
                                             :runner (fn [& _]
                                                       (throw (Exception.
                                                                "must not run")))})]
        (is (:ok result))
        (is (false? (:reported result)))
        (is (= :local-beads-unavailable (:reason result)))
        (is (map? (:issue-draft result))))
      (finally
        (doseq [file (reverse (file-seq root))]
          (.delete file))))))
