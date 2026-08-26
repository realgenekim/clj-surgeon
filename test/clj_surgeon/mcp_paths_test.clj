(ns clj-surgeon.mcp-paths-test
  (:require
   [clj-surgeon.mcp-paths :as paths]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files Path)
   (java.nio.file.attribute FileAttribute)))

(defn temp-root
  ^Path []
  (paths/real-root
    (.toString
      (Files/createTempDirectory "clj-surgeon-mcp-paths-"
                                 (make-array FileAttribute 0)))))

(deftest resolves-only-absent-targets-below-an-existing-real-parent
  (let [root (temp-root)
        src (.resolve root "src")]
    (Files/createDirectory src (make-array FileAttribute 0))
    (testing "an absent target is resolved without creating it"
      (let [result (paths/resolve-new-source-path root "src/moved.clj")]
        (is (:ok result))
        (is (= "src/moved.clj" (:relative result)))
        (is (= (.resolve src "moved.clj") (:canonical result)))
        (is (not (Files/exists (:canonical result)
                               (make-array java.nio.file.LinkOption 0))))))
    (testing "an existing target refuses"
      (spit (.toFile (.resolve src "taken.clj")) "(ns taken)")
      (is (= "target-already-exists"
             (:error_type
               (paths/resolve-new-source-path root "src/taken.clj")))))
    (testing "an absent nested target resolves without creating its parents"
      (let [result (paths/resolve-new-source-path root "missing/nested/moved.clj")]
        (is (:ok result))
        (is (= (.resolve root "missing/nested/moved.clj")
               (:canonical result)))
        (is (= [(.resolve root "missing")
                (.resolve root "missing/nested")]
               (:missing-parent-directories result)))
        (is (not (Files/exists (.resolve root "missing")
                               (make-array java.nio.file.LinkOption 0))))))
    (testing "lexical traversal refuses"
      (is (= "invalid-relative-source-path"
             (:error_type
               (paths/resolve-new-source-path root "../moved.clj")))))))
