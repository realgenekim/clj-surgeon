(ns clj-surgeon.battery-state-admission-test
  {:lane :battery}
  (:require
   [clj-surgeon.battery-parallel-runner :as bp]
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files LinkOption)
   (java.nio.file.attribute FileAttribute)))

(defn- snapshot [root]
  (let [path (.toPath (io/file (str root)))]
    (if (Files/exists path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
      (with-open [paths (Files/walk path (make-array java.nio.file.FileVisitOption 0))]
        (into (sorted-map)
              (for [p (iterator-seq (.iterator paths))]
                [(str p) (cond
                           (Files/isSymbolicLink p) [:link (str (Files/readSymbolicLink p))]
                           (Files/isDirectory p (make-array LinkOption 0)) :directory
                           :else (vec (Files/readAllBytes p)))])))
      {})))

(defn drive-production!
  "Child fixture: actual environment selection and production battery I/O;
   no substituted admission, writer, filesystem, or runner functions."
  [{:keys [workspace envelope selected rejected work]}]
  (System/setProperty "user.dir" workspace)
  (binding [artifacts/*destination-envelope*
            (artifacts/destination-envelope [envelope] :launcher)]
    (let [observe (fn [f]
                    (let [before [(snapshot selected) (snapshot rejected)]
                          result (try (f) {:kind nil}
                                      (catch Exception e
                                        {:kind (:error-type (ex-data e))
                                         :path (:path (ex-data e))}))]
                      (assoc result :unchanged?
                             (= before [(snapshot selected) (snapshot rejected)]))))
          result
          (binding [*out* (java.io.StringWriter.)]
            {:read (observe #(bp/read-wall-record bp/walls-path bp/walls-seed-path))
             :prepare (observe #(bp/prepare-suite!
                                  {"--suite" "battery" "--prereqs" "0"
                                   "--java-opts" "-J-Xmx1024m" "--work-dir" work}))
             :write (observe #(bp/write-walls! bp/walls-path
                                [{:namespace 'fixture/wall :elapsed-ms 11}]
                                [] bp/walls-seed-path))
             :finish (observe #(bp/finish-suite!
                                 {:suite "battery" :battery? true :lanes-n 1
                                  :work-dir (doto (io/file work) (.mkdirs))
                                  :effective-walls-path bp/walls-path
                                  :battery-namespaces [] :prereqs? false}
                                 [] 0))})]
      (prn result))))

;; @spec STATE-HOME-009
;; @spec STATE-HOME-010
(deftest walls-production-state-home-admission-matrix
  ;; Sol F1, 00566756: roots inside checkout / outside envelope wrote anyway.
  ;; Descendant redirects prove final-path admission independently of root admission.
  (let [container (Files/createTempDirectory "battery-admission-" (make-array FileAttribute 0))]
    (try
      (doseq [location [:inside :outside-envelope :descendant-inside
                        :descendant-outside :admitted]]
        (testing (name location)
          (let [cell (.resolve container (name location))
                allowed (.resolve cell "allowed")
                workspace (.resolve allowed "checkout")
                selected (case location
                           :inside (.resolve workspace "rejected")
                           :outside-envelope (.resolve cell "rejected")
                           (.resolve allowed "state"))
                rejected (case location
                           :descendant-inside (.resolve workspace "rejected")
                           :descendant-outside (.resolve cell "rejected")
                           selected)
                kind (case location
                       (:inside :descendant-inside) :state-root-inside-workspace
                       (:outside-envelope :descendant-outside) :state-root-outside-envelope
                       nil)
                _ (Files/createDirectories workspace (make-array FileAttribute 0))
                _ (when (contains? #{:descendant-inside :descendant-outside} location)
                    (Files/createDirectories selected (make-array FileAttribute 0))
                    (Files/createSymbolicLink (.resolve selected "battery") rejected
                      (make-array FileAttribute 0)))
                args {:workspace (str workspace) :envelope (str allowed)
                      :selected (str selected) :rejected (str rejected)
                      :work (str (.resolve cell "work"))}
                expression (str "(require 'clj-surgeon.battery-state-admission-test) "
                                "(clj-surgeon.battery-state-admission-test/drive-production! "
                                (pr-str args) ") (shutdown-agents)")
                child (shell/sh "clojure" "-J-Xmx1024m" "-M:clj-surgeon/test-deps" "-e" expression
                                :env (assoc (into {} (System/getenv))
                                            "CLJ_SURGEON_STATE_HOME" (str selected)))
                result (when (zero? (:exit child))
                         (edn/read-string (last (str/split-lines (:out child)))))]
            (is (zero? (:exit child)) (pr-str child))
            (doseq [op [:read :prepare :write :finish]]
              (is (= kind (get-in result [op :kind])) (pr-str [location op result]))
              (when kind
                (is (get-in result [op :unchanged?]) (pr-str [location op result]))
                (is (string? (get-in result [op :path])) (pr-str [location op result]))))
            (if kind
              (is (= {} (snapshot rejected)) "No created bytes OR directories below refused destination")
              (is (.isFile (io/file (str selected) "battery" "namespace-walls.edn")))))))
      (finally
        (with-open [paths (Files/walk container (make-array java.nio.file.FileVisitOption 0))]
          (doseq [path (reverse (sort (iterator-seq (.iterator paths))))]
            (Files/delete path)))))))
