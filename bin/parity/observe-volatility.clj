#!/usr/bin/env bb
;; observe-volatility — produce the evidence that a normalisation rule is allowed to exist.
;;
;;   observe-volatility.clj <run-dir> <run-id> <specimen> [<fixture-A> <fixture-B>]
;;
;; Compares the two captured sides of a STABLE-vs-STABLE run with NO
;; normalisation at all and prints, as EDN, every field path and tree path that
;; differed between two runs of one build. That output is the only admissible
;; source for the :evidence of a rule in volatile-fields.edn: a rule may exist
;; because a run showed the field differing, and for no other reason.
;;
;; When the two fixture directories are given it also diffs them with NO tree
;; exclusions, so an exclusion has to be paid for the same way a field rule is.
(ns parity.observe
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.string :as str] [clojure.java.shell :as shell]))

(defn- read-edn [f] (with-open [r (java.io.PushbackReader. (io/reader f))] (edn/read {:eof ::eof} r)))
(defn- exists? [f] (.exists (io/file f)))

(defn diffs
  ([a b] (diffs a b [] []))
  ([a b path acc]
   (cond
     (= a b) acc
     (and (map? a) (map? b))
     (reduce (fn [acc k] (diffs (get a k ::absent) (get b k ::absent) (conj path k) acc))
             acc (sort-by str (distinct (concat (keys a) (keys b)))))
     (and (sequential? a) (sequential? b) (= (count a) (count b)))
     (reduce (fn [acc i] (diffs (nth a i) (nth b i) (conj path i) acc)) acc (range (count a)))
     :else (conj acc [path a b]))))

(defn- trunc [x] (let [s (pr-str x)] (if (> (count s) 200) (str (subs s 0 200) "…") s)))

(defn- full-manifest [dir]
  (->> (file-seq (io/file dir))
       (filter #(.isFile %))
       (map #(subs (.getPath %) (count dir)))
       sort))

(defn -main [& [run-dir run-id specimen fx-a fx-b]]
  (let [out (atom [])]
    (doseq [file ["stdout.edn" "receipt.edn"]]
      (let [a (str run-dir "/A/" file) b (str run-dir "/B/" file)]
        (when (and (exists? a) (exists? b))
          (doseq [[p va vb] (diffs (read-edn a) (read-edn b))]
            (swap! out conj {:run run-id :specimen (keyword specimen) :path p
                             :a (trunc va) :b (trunc vb) :source file})))))
    (when (and fx-a fx-b (exists? fx-a) (exists? fx-b))
      (let [ma (full-manifest fx-a) mb (full-manifest fx-b)
            common (filter (set mb) ma)]
        (doseq [rel common]
          (let [fa (io/file (str fx-a rel)) fb (io/file (str fx-b rel))]
            (when (or (not= (.length fa) (.length fb))
                      (not= (:out (shell/sh "sha256sum" (.getPath fa)))
                            (str/replace (:out (shell/sh "sha256sum" (.getPath fb))) (.getPath fb) (.getPath fa))))
              (swap! out conj {:run run-id :specimen (keyword specimen)
                               :path ["tree" rel] :source "fixture-tree"
                               :a "differs" :b "differs"}))))
        (doseq [rel (remove (set mb) ma)]
          (swap! out conj {:run run-id :specimen (keyword specimen) :path ["tree" rel]
                           :source "fixture-tree" :a "present" :b ::absent}))
        (doseq [rel (remove (set ma) mb)]
          (swap! out conj {:run run-id :specimen (keyword specimen) :path ["tree" rel]
                           :source "fixture-tree" :a ::absent :b "present"}))))
    (binding [*print-length* nil]
      (doseq [o @out] (prn o)))))

(apply -main *command-line-args*)
