(ns clj-surgeon.insert-forms-parity-test
  {:lane :battery}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-018
;; INTENT-TEST: INSERT-FORMS-018
(deftest insert-forms-cli-mcp-parity

  (doseq [prefix [[":insert-forms!"] [":op" ":insert-forms!"]]]
    (h/with-file h/source
      (fn [dir file req]
        (let [request-file (io/file dir "request.edn")
              _ (spit request-file (pr-str req))
              result (apply shell/sh (concat ["clojure" "-J-Xmx1g" "-M:clj-surgeon/test-deps" "-m" "clj-surgeon.core"]
                                             prefix [":request-file" (str request-file)]))
              receipt (try (edn/read-string (:out result)) (catch Exception _ nil))]
          (is (= 0 (:exit result)) (pr-str result))
          (is (= "committed" (:state receipt)))
          (is (= true (:write_verified receipt)))
          (is (= "(defn a [] 1)\n(defn b [] 3)\n\n(defn ab [] 2)\n" (slurp file))))))))
