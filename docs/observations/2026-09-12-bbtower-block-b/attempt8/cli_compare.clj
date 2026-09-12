(require '[babashka.fs :as fs]
         '[babashka.process :as proc]
         '[clojure.edn :as edn]
         '[clojure.string :as str])

(let [output "/var/tmp/forge/bbtower-fx/attempt8"
      fixture (str output "/cli-fixture")
      app (str fixture "/app_shell.clj")
      reader (str fixture "/source_reader.clj")
      original-app (slurp "test/fixtures/intent_transaction/app_shell.clj")
      original-reader (slurp "test/fixtures/intent_transaction/source_reader.clj")
      request {:intents [{:files [app reader] :from ":body" :to ":body.intent-page" :expect-count 2}
                         {:files [app] :from "\"/app.css\"" :to "\"/intent.css\"" :expect-count 1}
                         {:files [reader] :from "[:title \"Workbench\"]"
                          :to "[:title \"Intent Workbench\"]" :expect-count 1}]
               :expect {:intent-count 3 :edit-count 4 :changed-file-count 2}}
      args ["-m" "clj-surgeon.core" ":op" ":change!" ":spec-file" "-"
            ":receipt-out" (str fixture "/receipt.edn")]
      rows (atom [])]
  (fs/create-dirs fixture)
  (spit (str output "/cli-request.edn") (pr-str request))
  (try
    (doseq [[runtime command] [[:bb ["bb" "-Xmx1g" "-Djava.io.tmpdir=/var/tmp/forge/bbtower-fx"]]
                               [:jvm ["clojure" "-J-Xms64m" "-J-Xmx1g" "-M:clj-surgeon/test-deps"]]]]
      (spit app original-app)
      (spit reader original-reader)
      (let [started (System/nanoTime)
            result @(proc/process (into command args) {:in (pr-str request) :out :string :err :string})
            wall (long (/ (- (System/nanoTime) started) 1000000))
            receipt (edn/read-string (:out result))
            saved (slurp (:receipt-file receipt))
            normalized (-> (:out result)
                           (str/replace (:receipt-file receipt) "RECEIPT-PATH")
                           (str/replace (:receipt-hash receipt) "RECEIPT-HASH"))]
        (spit (str output "/cli-" (name runtime) ".stdout") (:out result))
        (spit (str output "/cli-" (name runtime) ".stderr") (:err result))
        (spit (str output "/cli-" (name runtime) ".normalized") normalized)
        (spit (str output "/cli-" (name runtime) "-receipt.edn") saved)
        (swap! rows conj {:runtime runtime :wall-ms wall :exit (:exit result)
                          :stdout-chars (count (:out result)) :receipt-chars (count saved)
                          :workspace-status (:workspace_status receipt)})
        (fs/delete-if-exists (:receipt-file receipt))))
    (spit (str output "/cli-comparison.edn") (pr-str @rows))
    (doseq [row @rows] (prn row))
    (finally (fs/delete-tree fixture))))
