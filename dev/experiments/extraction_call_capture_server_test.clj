(ns extraction-call-capture-server-test
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [extraction-call-capture-server :as server]
   [extraction-tool-surface :as surface]))

(deftest projected-capture-tools-retain-exact-surface-identity
  (let [control (server/capture-tool :control "/tmp/control.json")
        treatment (server/capture-tool :treatment "/tmp/treatment.json")]
    (doseq [[arm tool] [[:control control] [:treatment treatment]]]
      (let [projected (surface/tool-surface arm)]
        (is (= (:name projected) (:name tool)))
        (is (= (:description projected) (:description tool)))
        (is (= (:schema projected) (:schema tool)))
        (is (= (:output-schema projected) (:output-schema tool)))
        (is (= (:annotations projected) (:annotations tool)))))
    (is (not= (:description control) (:description treatment)))
    (is (not= (:schema control) (:schema treatment)))))

(deftest capture-handler-records-once-and-never-claims-a-write
  (let [file (java.io.File/createTempFile "extraction-call-capture-" ".json")
        response (promise)
        params {:extraction {:file "src/sample.clj"
                             :to "src/moved.clj"
                             :forms ["moved"]
                             :require_policy "minimal"
                             :public_forms []
                             :caller_changes []
                             :ignored_caller_files []}}
        handler (server/capture-handler (.getPath file))]
    (try
      (handler nil params
               (fn [content error? structured]
                 (deliver response {:content content
                                    :error? error?
                                    :structured structured})))
      (let [captured (json/parse-string (slurp file) true)]
        (is (= [params] (mapv :params (:calls captured))))
        (is (= 1 (get-in @response [:structured :call_count])))
        (is (true? (get-in @response [:structured :source_unchanged])))
        (is (false? (:error? @response))))
      (finally
        (io/delete-file file true)))))
