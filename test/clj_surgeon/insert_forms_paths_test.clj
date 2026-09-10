(ns clj-surgeon.insert-forms-paths-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-015
;; INTENT-TEST: INSERT-FORMS-015
(deftest insert-forms-path-confinement

  (doseq [path ["../escape.clj" "/absolute.clj" "missing.clj" "src/example.cljs" "src/\u0000.clj"]]
    (h/with-file h/source
      (fn [_ file req]
        (let [r (insert/execute! (assoc req :file path))]
          (is (= (if (= path "src/example.cljs") :unsupported-source :invalid-path) (:error-type r)))
          (is (= h/source (slurp file)))))))
  (doseq [link-type [:symbolic :hard]]
    (h/with-file h/source
      (fn [dir file req]
        (let [link (.toPath (io/file dir "alias.clj"))]
          (if (= :hard link-type)
            (Files/createLink link (.toPath file))
            (Files/createSymbolicLink link (.toPath file) (make-array java.nio.file.attribute.FileAttribute 0)))
          (is (= :invalid-path (:error-type (insert/execute! (assoc req :file "alias.clj")))))
          (is (= h/source (slurp file))))))))
