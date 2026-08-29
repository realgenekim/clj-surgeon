(ns formatter-process-canary
  (:require
   [clj-surgeon.mcp-extraction-plan :as extraction-plan]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io])
  (:import
   (java.security MessageDigest)))

(def moved-forms
  ["date-fmt" "datetime-fmt" "->local-date" "fmt-date"
   "fmt-date-range" "fmt-instant" "->instant" "when-fmt"
   "relative-when" "fmt-when" "fmt-cfp-window" "iso-date-fmt"
   "fmt-close-date" "cfp-public-url" "not-blank"])

(defn sha256 [file]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (java.nio.file.Files/readAllBytes (.toPath (io/file file))))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn formatter-command [arm]
  (case arm
    "npx" ["npx" "@chrisoakman/standard-clojure-style" "fix" "{files}"]
    "direct" [(or (System/getenv "STANDARD_CLJ") "standard-clj") "fix" "{files}"]
    (throw (ex-info "Unknown formatter arm" {:arm arm}))))

(defn run-canary! [workspace arm]
  (let [source "src/cfp_scheduler_killer/views.clj"
        target "src/cfp_scheduler_killer/views/format.clj"
        plan (extraction-plan/plan!
               {:project-root workspace}
               {:mode "plan-extraction"
                :file source
                :to target
                :forms moved-forms
                :require_policy "minimal"})
        request {:extraction
                 {:file source
                  :to target
                  :forms moved-forms
                  :require_policy "minimal"
                  :public_forms ["not-blank"]
                  :caller_changes []
                  :ignored_caller_files []
                  :source_hash (:source_hash plan)}
                 :verify "exact"}
        config {:project-root workspace
                :receipt-dir (.getPath (io/file workspace "receipts"))
                :formatter (formatter-command arm)
                :verification-profile-source :project
                :verification-profiles
                {"exact" {:acceptance :exact-exit
                          :timeout-ms 120000
                          :commands
                          [["/bin/test" "-s" target]]}}}
        started (System/nanoTime)
        result (mcp-tool/execute-request! config request)
        wall-ms (/ (- (System/nanoTime) started) 1000000.0)]
    {:arm arm
     :ok (:ok result)
     :wall-ms wall-ms
     :server-elapsed-ms (:elapsed_ms result)
     :formatter (:formatter result)
     :telemetry (:telemetry result)
     :verification (:verification result)
     :verification-complete (:verification_complete result)
     :next-action (:next_action result)
     :source-sha256 (sha256 (io/file workspace source))
     :target-sha256 (when (.exists (io/file workspace target))
                      (sha256 (io/file workspace target)))
     :error-type (:error_type result)
     :error (:error result)}))

(defn -main [& [workspace arm]]
  (when-not (and workspace arm)
    (throw (ex-info "Usage: formatter-process-canary WORKSPACE npx|direct" {})))
  (prn (run-canary! (.getCanonicalPath (io/file workspace)) arm)))
