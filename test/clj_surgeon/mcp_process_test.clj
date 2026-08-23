(ns clj-surgeon.mcp-process-test
  (:require
   [clj-surgeon.mcp-process :as process]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(deftest effective-path-makes-agent-tools-one-shot-without-dropping-caller-path
  (let [path (process/effective-path "/home/gene" "/custom/bin:/usr/bin")
        entries (str/split path (re-pattern java.io.File/pathSeparator))]
    (is (= "/home/gene/bin" (first entries)))
    (is (= "/home/gene/.local/bin" (second entries)))
    (is (some #{"/custom/bin"} entries))
    (is (some #{"/usr/bin"} entries))
    (is (= (count entries) (count (distinct entries))))))

(deftest configure-environment-publishes-the-complete-path
  (let [environment (java.util.HashMap. {"PATH" "/custom/bin"})]
    (is (identical? environment
                    (process/configure-environment! environment)))
    (is (str/includes? (.get environment "PATH")
                       (str (System/getProperty "user.home") "/bin")))
    (is (str/ends-with? (.get environment "PATH") "/custom/bin"))))
