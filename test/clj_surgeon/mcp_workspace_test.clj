(ns clj-surgeon.mcp-workspace-test
  (:require
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(defn- temp-dir
  []
  (.toFile
    (java.nio.file.Files/createTempDirectory
      "clj-surgeon-workspace-"
      (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists file)
    (doseq [entry (reverse (file-seq file))]
      (.delete entry))))

(deftest canonical-root-is-closed-and-explicit
  (let [root (temp-dir)]
    (try
      (let [canonical (.getPath (.getCanonicalFile root))]
        (is (= {:ok true :workspace-root canonical}
               (workspace/canonical-root canonical)))
        (doseq [[label value message]
                [["blank" "" "non-blank"]
                 ["relative" "." "absolute"]
                 ["missing" (str (io/file root "missing")) "existing"]]]
          (testing label
            (let [result (workspace/canonical-root value)]
              (is (false? (:ok result)))
              (is (= "invalid-workspace-root" (:error_type result)))
              (is (= ["workspace_root"] (:path result)))
              (is (re-find (re-pattern message) (:error result)))))))
      (finally
        (delete-tree! root)))))

(deftest canonical-root-collapses-repository-symlink-aliases
  (let [parent (temp-dir)
        root (doto (io/file parent "root") .mkdirs)
        alias (io/file parent "alias")]
    (try
      (java.nio.file.Files/createSymbolicLink
        (.toPath alias)
        (.toPath root)
        (make-array java.nio.file.attribute.FileAttribute 0))
      (is (= (workspace/canonical-root (.getPath root))
             (workspace/canonical-root (.getPath alias))))
      (let [builds (atom [])
            router (workspace/router
                     {:project-root (.getPath root)
                      :workspace-context-factory
                      (fn [workspace-root]
                        (swap! builds conj workspace-root)
                        {})})]
        (is (:ok (workspace/resolve-request
                   router {:workspace_root (.getPath root) :requests []})))
        (is (:ok (workspace/resolve-request
                   router {:workspace_root (.getPath alias) :requests []})))
        (is (= 1 (count @builds)))
        (is (= 1 (count (workspace/cached-roots router)))))
      (finally
        (delete-tree! parent)))))

(deftest contexts-are-lazy-canonical-deduplicated-and-isolated
  (let [root-a (temp-dir)
        root-b (temp-dir)
        builds (atom [])
        base {:project-root (.getPath root-a)
              :shared :base
              :workspace-context-factory
              (fn [root]
                (swap! builds conj root)
                {:workspace-marker (.getName (io/file root))})}
        router (workspace/router base)]
    (try
      (is (empty? (workspace/cached-roots router)))
      (let [a1 (workspace/resolve-request
                 router {:workspace_root (.getPath root-a) :requests []})
            a2 (workspace/resolve-request
                 router {:workspace_root (str (io/file root-a ".")) :requests []})
            b (workspace/resolve-request
                router {:workspace_root (.getPath root-b) :changes []})]
        (is (:ok a1))
        (is (= (:workspace-root a1) (:workspace-root a2)))
        (is (= 2 (count @builds)))
        (is (= 2 (count (workspace/cached-roots router))))
        (is (= :base (get-in a1 [:config :shared])))
        (is (not= (get-in a1 [:config :workspace-marker])
                  (get-in b [:config :workspace-marker])))
        (is (not (contains? (:params a1) :workspace_root)))
        (is (= [] (get-in a1 [:params :requests])))
        (is (= [] (get-in b [:params :changes]))))
      (finally
        (delete-tree! root-a)
        (delete-tree! root-b)))))

(deftest omitted-root-uses-and-caches-the-canonical-default
  (let [root (temp-dir)
        router (workspace/router {:project-root (.getPath root)})]
    (try
      (let [result (workspace/resolve-request router {:requests []})]
        (is (:ok result))
        (is (= (.getPath (.getCanonicalFile root))
               (:workspace-root result)))
        (is (= [(:workspace-root result)]
               (workspace/cached-roots router))))
      (finally
        (delete-tree! root)))))

(deftest receipt-directories-are-deterministic-and-workspace-isolated
  (let [root-a (temp-dir)
        root-b (temp-dir)]
    (try
      (let [a1 (workspace/receipt-dir (.getPath root-a))
            a2 (workspace/receipt-dir (.getCanonicalPath root-a))
            b (workspace/receipt-dir (.getPath root-b))]
        (is (= a1 a2) "one canonical workspace has one receipt directory")
        (is (not= a1 b) "different workspaces cannot share default receipts")
        (is (.endsWith a1 (str java.io.File/separator "receipts"))))
      (finally
        (delete-tree! root-a)
        (delete-tree! root-b)))))
