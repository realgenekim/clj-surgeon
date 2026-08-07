(ns clj-surgeon.mcp-server-test
  (:require
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-server :as server]
   [clj-surgeon.mcp-tool :as tool]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [nrepl.core :as nrepl]
   [nrepl.server :as nrepl-server])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-mcp-server-test-"
      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(deftest exposes-exactly-two-typed-tools
  (let [tools (server/make-tools nil ".")]
    (is (= 2 (count tools)))
    (is (= ["inspect_clojure" "apply_clojure_changes"]
           (mapv :name tools)))
    (is (= #'inspect-tool/handle-inspect (:tool-fn (first tools))))
    (is (= #'tool/handle-clj-change (:tool-fn (second tools))))
    (is (= false (get-in tools [0 :schema :additionalProperties])))
    (is (= inspect-tool/inspect-annotations
           (:annotations (first tools))))
    (is (str/includes? inspect-tool/tool-description
                       "(-> (form 'numeric-fields) initializer"))
    (is (str/includes? inspect-tool/tool-description
                       "read_complete=true is terminal"))
    (is (< (count server/server-instructions) 512))
    (is (str/includes? server/server-instructions
                       "PREFER inspect_clojure"))
    (is (str/includes? tool/tool-description
                       "avoids fragile patch-context mismatches"))
    (is (str/includes? server/server-instructions
                       "do not read first"))
    (is (str/includes? server/server-instructions
                       "verification_complete=true"))))

(deftest embedded-nrepl-redefines-the-live-handler-var
  (let [directory (temp-dir)
        port-file (io/file directory ".nrepl-port")
        original @#'tool/handle-clj-change
        embedded (server/start-embedded-nrepl! 0 (.getPath port-file))]
    (try
      (is (some? embedded))
      (is (= (:port embedded) (parse-long (slurp port-file))))
      (with-open [connection (nrepl/connect :port (:port embedded))]
        (let [client (nrepl/client connection 5000)
              code
              (str "(alter-var-root #'clj-surgeon.mcp-tool/handle-clj-change "
                   "(constantly (fn [_ _ callback] "
                   "(callback [\"hot-handler\"] false))))")
              replies (doall (nrepl/message client {:op "eval" :code code}))]
          (is (some #(contains? (set (:status %)) "done") replies))))
      (let [callback-result (atom nil)]
        ((:tool-fn tool/clj-change-tool)
         nil {} (fn [content error?]
                  (reset! callback-result {:content content :error? error?})))
        (is (= {:content ["hot-handler"] :error? false}
               @callback-result)))
      (finally
        (alter-var-root #'tool/handle-clj-change (constantly original))
        (when embedded (nrepl-server/stop-server embedded))
        (delete-tree! directory)))))
