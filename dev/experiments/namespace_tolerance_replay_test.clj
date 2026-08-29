(ns namespace-tolerance-replay-test
  (:require
   [clojure.test :refer [deftest is run-tests testing]]
   [namespace-tolerance-replay :as replay]))

(def file "src/sample/app.clj")
(def source
  "(ns sample.app\n  (:require [old.core :as old]))\n(defn f [] 1)\n")
(def sources {file source})

(deftest law-a-requires-exact-uncontested-direct-namespace-name
  (let [edit (replay/base-edit file)
        lowered (replay/lower-law-a sources edit)]
    (is (= {"namespace" "sample.app"} (get lowered "within")))
    (is (= "sample.app" (get-in lowered ["within" "namespace"])))
    (doseq [[label candidate-sources candidate-edit]
            [[:wrong-name sources
              (assoc-in edit ["within" "form"] "sample.wrong")]
             [:competing-owner
              {file (str source "(def sample.app 1)\n")} edit]
             [:multiple-namespaces
              {file (str source "(ns sample.other)\n")} edit]
             [:reader-conditional
              {file "#?(:clj (ns sample.app (:require [old.core :as old])))\n"}
              edit]]]
      (testing (name label)
        (is (nil? (replay/lower-law-a candidate-sources candidate-edit)))))))

(deftest law-b-requires-a-single-file-and-an-exact-same-kind-clause-count
  (let [edit {"files" [file]
              "from" "(:require [old.core :as old])"
              "to" "(:require [new.core :as new])"
              "matches" 1}
        lowered (replay/lower-law-b sources edit)]
    (is (= file (get lowered "file")))
    (is (nil? (get lowered "files")))
    (is (= {"namespace" true} (get lowered "within")))
    (doseq [[label candidate]
            [[:non-namespace
              (assoc edit "from" "(defn f [] 1)" "to" "(defn f [] 2)")]
             [:stale-count (assoc edit "matches" 2)]
             [:kind-mismatch (assoc edit "to" "(:import java.time.Instant)")]
             [:empty-files (assoc edit "files" [])]
             [:many-files (assoc edit "files" [file "src/sample/other.clj"])]
             [:file-and-files (assoc edit "file" file)]]]
      (testing (name label)
        (is (nil? (replay/lower-law-b sources candidate)))))))

(deftest optional-law-c-requires-the-same-unique-complete-named-owner
  (let [edit {"files" [file]
              "from" "(defn f [] 1)"
              "to" "(defn f [] 2)"
              "matches" 1}
        lowered (replay/lower-law-c sources edit)]
    (is (= file (get lowered "file")))
    (is (= {"form" "f"} (get lowered "within")))
    (doseq [[label candidate-sources candidate]
            [[:zero {file "(ns sample.app)\n(defn other [] 1)\n"} edit]
             [:many {file (str source "(defn f [] 1)\n")} edit]
             [:anonymous sources
              (assoc edit "from" "(+ 1 2)" "to" "(+ 2 3)")]
             [:different-kind sources (assoc edit "to" "(def f 2)")]
             [:different-name sources (assoc edit "to" "(defn g [] 2)")]
             [:nested-only
              {file "(ns sample.app)\n(def nested '(defn f [] 1))\n"} edit]
             [:stale-count sources (assoc edit "matches" 2)]]]
      (testing (name label)
        (is (nil? (replay/lower-law-c candidate-sources candidate)))))))

(deftest retained-replay-preserves-the-two-law-stop-and-separates-law-c
  (let [result (replay/report)]
    (is (:experiment-green result))
    (is (= 8 (:capture-count result)))
    (is (= 7 (get-in result [:two-law :exact-run-count])))
    (is (false? (get-in result [:two-law :all-eight-exact])))
    (is (= 8 (get-in result [:optional-law-c :exact-run-count])))
    (is (get-in result [:optional-law-c :all-eight-exact]))
    (is (get-in result [:candidate-migration :expanded-before-tolerance]))
    (is (get-in result
                [:candidate-migration :all-preserve-23-owners-27-matches]))
    (is (:all-falsifiers-refuse result))
    (is (zero? (:model-calls result)))
    (is (zero? (:mutation-actions result)))))

(let [{:keys [fail error]} (run-tests)]
  (when (pos? (+ fail error))
    (System/exit 1)))
