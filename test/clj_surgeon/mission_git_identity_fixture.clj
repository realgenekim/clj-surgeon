(ns clj-surgeon.mission-git-identity-fixture
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]))

(defn isolated [environment expression]
  (let [env (merge (into {} (remove #(str/starts-with? (key %) "GIT_") (System/getenv))) environment)
        r (shell/sh "bb" "-cp" (.getCanonicalPath (io/file "src")) "-e" expression :env env)]
    (when-not (zero? (:exit r)) (throw (ex-info "Identity fixture child failed" {:exit (:exit r)})))
    (edn/read-string (:out r))))

(defn commit [provenance environment]
  (isolated environment
            (str "(require '[clj-surgeon.mission-git :as g]) (prn (g/commit! "
                 (pr-str provenance) " (constantly true)))")))
