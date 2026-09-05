(ns ^{:lane :battery} clj-surgeon.mcp-feature-thread-sed-test
  "The one `feature_thread` witness that shells out -- in the battery lane
   because /usr/bin/sed is a child process.

   ROUND-EIGHT REVIEW, finding 5 gave this assertion its reason: every range
   digest excluded the range's final LF, and the witness built its slice with
   split/join -- which silently drops that byte -- so implementation and
   witness shared one error and agreed with each other about a file neither
   had read. The fix was to cross-check one case against `sed` itself, an
   oracle with no shared code. That is exactly the right assertion and it is
   kept verbatim.

   ROUND FIVE of the suite spike moved it out of
   `clj-surgeon.mcp-feature-thread-test`, which is `:fast`, and the fast
   lane's rule is `No child process`. Sixty-nine deftests should not leave the
   merge gate because one of them runs `sed`; the one that runs `sed` moves
   instead. Same trade as `clj-surgeon.mcp-inspect-cold-job-test`, same
   reason: a lane is a statement about what a namespace DOES, and the fix is
   to make the statement true."
  (:require
   [clj-surgeon.mcp-feature-thread :as ft]
   [clj-surgeon.mcp-feature-thread-test :as ftt]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is]]))

;; @spec MCP-OP-THREAD-052
(deftest a-range-digest-agrees-with-sed-itself
  ;; "for the leg the transcript read four times"
  (let [{:keys [structured]} (ftt/thread! ftt/fixture-root {:budget_bytes 32768})
        js (ftt/leg structured "js-function")
        out (:out (shell/sh "sed" "-n" (str (:from js) "," (:to js) "p")
                            (.getPath (io/file ftt/fixture-root (:file js)))))]
    (is (= (ft/sha256-hex out) (:sha256 js))
        "the published digest is not a digest of what sed prints")))
