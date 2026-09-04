;; @spec MCP-OP-TIME-007
;; Writes the clock spellings DERIVED UNDER THIS RUNTIME to the manifest the
;; babashka floor is checked against. Run it on the JVM and only on the JVM:
;; `make clock-spellings-manifest`. Never hand-edit the manifest -- the witness
;; compares it against a live derivation in both directions, so an edited
;; manifest is a lie the next run reports.
(require '[clj-surgeon.measured-invariant-test :as mit])
(let [spellings (vec (sort mit/clock-spellings))]
  (spit "test/fixtures/clock-spellings-jvm.edn"
        (str ";; DERIVED, NEVER HAND-EDITED.\n"
             ";; Regenerate with: make clock-spellings-manifest\n"
             ";; JVM " (System/getProperty "java.version") "\n"
             (pr-str spellings) "\n"))
  (println "clock-spellings manifest:" (count spellings) "spellings, JVM"
           (System/getProperty "java.version")))
