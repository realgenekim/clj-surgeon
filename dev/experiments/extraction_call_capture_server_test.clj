(ns extraction-call-capture-server-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [extraction-call-capture-server :as server]
   [extraction-tool-surface :as surface]))

(def public-fields
  [:id :name :description :schema :output-schema :annotations :structured?])

(deftest projected-capture-catalog-retains-production-order-and-peer-surfaces
  (let [production (mcp-tool/tools-for-profile :full)
        control (server/capture-tools :control "/tmp/control.json")
        treatment (server/capture-tools :treatment "/tmp/treatment.json")
        apply-index (.indexOf (mapv :id production) :clj-change)]
    (is (= (mapv :id production) (mapv :id control) (mapv :id treatment)))
    (is (= (mapv :name production) (mapv :name control) (mapv :name treatment)))
    (is (= (mapv #(select-keys % public-fields) production)
           (mapv #(select-keys % public-fields) control)))
    (doseq [index (remove #{apply-index} (range (count production)))]
      (is (= (select-keys (nth production index) public-fields)
             (select-keys (nth treatment index) public-fields))))
    (let [projected (surface/tool-surface :treatment)
          treatment-apply (nth treatment apply-index)]
      (is (= (:description projected) (:description treatment-apply)))
      (is (= (:schema projected) (:schema treatment-apply)))
      (is (= (:output-schema projected) (:output-schema treatment-apply)))
      (is (= (:annotations projected) (:annotations treatment-apply))))))

(deftest every-catalog-handler-records-its-tool-and-never-claims-a-write
  (let [file (java.io.File/createTempFile "extraction-call-capture-" ".json")
        tools (server/capture-tools :treatment (.getPath file))]
    (try
      (doseq [[index tool] (map-indexed vector tools)]
        (let [response (promise)
              params {:probe (:name tool)}]
          ((:tool-fn tool) nil params
                           (fn [content error? structured]
                             (deliver response {:content content
                                                :error? error?
                                                :structured structured})))
          (is (= (:name tool) (get-in @response [:structured :selected_tool])))
          (is (= (inc index) (get-in @response [:structured :call_count])))
          (is (true? (get-in @response [:structured :source_unchanged])))
          (is (false? (:error? @response)))))
      (let [captured (json/parse-string (slurp file) true)
            calls (:calls captured)]
        (is (= (mapv (comp name :id) tools) (mapv :tool_id calls)))
        (is (= (mapv :name tools) (mapv :tool_name calls)))
        (is (= (mapv (comp :probe :params) calls) (mapv :name tools))))
      (finally
        (io/delete-file file true)))))
