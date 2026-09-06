(ns clj-surgeon.mission-git-test
  {:lane :fast}
  (:require
   [clj-surgeon.mission-git :as g]
   [clojure.test :refer [deftest is testing]]))

(def h1 (apply str (repeat 64 "1")))
(def h2 (apply str (repeat 64 "2")))
(def h3 (apply str (repeat 64 "3")))
(def oid (apply str (repeat 40 "a")))
(def provenance
  {:id "M-0001" :state :verified :workspace-root "/repo"
   :ledger-sha256 h1 :receipt-sha256 h2
   :gate {:ok true :sha256 h1} :acceptance {:ok true :sha256 h3}
   :files {"src/a.clj" {:before-sha256 h1 :after-sha256 h2}}})
(def observed
  {:workspace-root "/repo" :branch "refs/heads/work" :head oid :tree oid
   :staged-paths ["src/a.clj"]
   :files {"src/a.clj" {:head-sha256 h1 :index-sha256 h2 :live-sha256 h2
                        :head-mode "100644" :index-mode "100644" :live-mode "100644"}}})

(deftest exact-plan
  (let [r (g/plan provenance observed)]
    (is (:ok r))
    (is (= ["commit-tree" oid "-p" oid "-F" "-"] (:commit-argv r)))
    (is (re-find #"Mission: M-0001" (:message r)))
    (is (re-find #"Hooks: skipped" (:message r)))
    (is (false? (:source-mutation-attempted r)))))

(deftest admission-matrix
  (doseq [[label p o]
          [["state" (assoc provenance :state :planned) observed]
           ["proof" (assoc-in provenance [:acceptance :ok] false) observed]
           ["same proof" (assoc-in provenance [:acceptance :sha256] h1) observed]
           ["wrong root" provenance (assoc observed :workspace-root "/other")]
           ["main" provenance (assoc observed :branch "refs/heads/main")]
           ["MCP/main" provenance (assoc observed :branch "refs/heads/MCP/main")]
           ["detached" provenance (assoc observed :branch nil)]
           ["extra staged" provenance (assoc observed :staged-paths ["src/a.clj" "README.md"])]
           ["missing staged" provenance (assoc observed :staged-paths [])]
           ["stale live" provenance (assoc-in observed [:files "src/a.clj" :live-sha256] h3)]
           ["partial stage" provenance (assoc-in observed [:files "src/a.clj" :index-sha256] h3)]
           ["wrong HEAD" provenance (assoc-in observed [:files "src/a.clj" :head-sha256] h3)]
           ["mode" provenance (assoc-in observed [:files "src/a.clj" :index-mode] "100755")]
           ["bad path" (assoc provenance :files {"../a" {:before-sha256 h1 :after-sha256 h2}}) observed]
           ["metadata" (assoc provenance :id "M-1\nForged: true") observed]]]
    (testing label (is (false? (:ok (g/plan p o)))))))

(deftest execution-sequence
  (let [calls (atom []) new-oid (apply str (repeat 40 "b"))
        result (g/execute! provenance (constantly observed)
                           (fn [argv input]
                             (swap! calls conj [argv input])
                             (if (= "commit-tree" (first argv)) (str new-oid "\n") "")))]
    (is (:ok result))
    (is (= ["update-ref" "refs/heads/work" new-oid oid] (first (second @calls))))
    (is (= 2 (count @calls)))
    (is (:git-ref-updated result))
    (is (false? (:hooks-run result)))))

(deftest drift-does-not-advance-ref
  (let [n (atom 0) calls (atom [])]
    (is (false? (:ok (g/execute! provenance
                       #(if (= 1 (swap! n inc)) observed (assoc observed :head h3))
                       (fn [& args] (swap! calls conj args) oid)))))
    (is (empty? @calls))))
