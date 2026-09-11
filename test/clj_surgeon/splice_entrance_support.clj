(ns clj-surgeon.splice-entrance-support
  (:require [clj-surgeon.insert-forms-support :as h]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [is]]))

(defn cli [prefix path]
  ;; bb does not apply JAVA_TOOL_OPTIONS: set its property explicitly in every child.
  (apply shell/sh ["bb" "-e"
                   (str "(System/setProperty \"java.io.tmpdir\" " (pr-str (System/getProperty "java.io.tmpdir")) ") "
                        "(require '[clj-surgeon.core :as core]) (apply core/-main *command-line-args*)")
                   "--" prefix ":request-file" (str path)]))
(defn exercise [verb source expected request callback]
  (doseq [prefix [(str ":" verb "!") ":op"]]
    (h/with-file source
      (fn [dir target _]
        (let [req (assoc request :workspace_root (.getCanonicalPath dir))
              path (io/file dir "request.edn")
              _ (spit path (pr-str req))
              result (if (= prefix ":op")
                       (shell/sh "bb" "-e"
                         (str "(System/setProperty \"java.io.tmpdir\" " (pr-str (System/getProperty "java.io.tmpdir")) ") "
                              "(require '[clj-surgeon.core :as core]) (apply core/-main *command-line-args*)")
                         "--" ":op" (str ":" verb "!") ":request-file" (str path))
                       (cli prefix path))
              receipt (edn/read-string (:out result))]
          (is (= [0 "committed" true expected]
                 [(:exit result) (:state receipt) (:write_verified receipt) (slurp target)]) (pr-str result))
          (when (not= prefix ":op")
            (spit target source)
            (let [[content error? mcp] (callback req)
                  detail (slurp (:receipt_details_path mcp))]
              (is (= [false (dissoc receipt :elapsed_ms :receipt_details_path :receipt_hash) mcp
                      (:receipt_hash mcp) req]
                     [error? (dissoc mcp :elapsed_ms :receipt_details_path :receipt_hash)
                      (edn/read-string (first content)) (h/sha detail) (:request (edn/read-string detail))])))
            (spit target source)
            (let [bad (assoc req :preview true) _ (spit path (pr-str bad))
                  result (cli prefix path) receipt (edn/read-string (:out result))
                  [_ error? mcp] (callback bad)]
              (is (= [2 :invalid-request :invalid-request false "refused" source]
                     [(:exit result) (:error-type receipt) (:error-type mcp) error? (:state receipt) (slurp target)])))
            (let [result (cli prefix (io/file dir "missing.edn"))]
              (is (= [1 "failed"] [(:exit result) (:state (edn/read-string (:out result)))])))))))))
