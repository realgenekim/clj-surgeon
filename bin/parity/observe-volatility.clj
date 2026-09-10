#!/usr/bin/env bb
;; observe-volatility — produce the evidence that a normalisation rule is allowed
;; to exist, AND retain the bytes that evidence was derived from.
;;
;;   observe-volatility.clj <run-dir> <run-id> <specimen> <stable-build-sha> \
;;                          <evidence-root> [<fixture-A> <fixture-B>]
;;
;; Compares the two captured sides of a STABLE-vs-STABLE run with NO
;; normalisation at all — and, when the fixtures are given, their trees with NO
;; exclusions — and prints, as EDN, every path that differed between two runs of
;; one build.
;;
;; A CHECKED-IN ROW ASSERTING ITS OWN PROVENANCE IS NOT EVIDENCE. So this also
;; copies the exact captured sides it compared into
;; <evidence-root>/<run>/<specimen>/{A,B}/ and writes a capture.edn naming the
;; stable build sha, this generator's own source digest, and a sha256 per
;; retained file. bin/parity/compare.clj re-opens those bytes, recomputes the
;; digests, and RE-DERIVES the difference before it will honour any rule that
;; cites the observation. A row whose run has no retained capture refuses.
(ns parity.observe
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.java.shell :as shell] [clojure.string :as str]))

(defn- git-head [dir]
  (let [{:keys [exit out]} (shell/sh "git" "-C" dir "rev-parse" "HEAD")]
    (when (zero? exit) (str/trim-newline out))))

(defn- read-edn [f] (with-open [r (java.io.PushbackReader. (io/reader f))] (edn/read {:eof ::eof} r)))
(defn- exists? [f] (.exists (io/file f)))

(defn sha256 [^String s]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest d (.getBytes s "UTF-8"))))))
(defn sha256-file [f] (sha256 (slurp f)))

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

(defn full-manifest
  "sha256 of every file in the tree, NO exclusions: an exclusion must be paid for
  exactly the way a field rule is."
  [dir]
  (->> (file-seq (io/file dir))
       (filter #(.isFile %))
       (map (fn [f] [(subs (.getPath f) (count dir)) (try (sha256-file f) (catch Exception _ "unreadable"))]))
       (sort-by first)))

(defn manifest->text [m] (str/join "\n" (map (fn [[p h]] (str h "  " p)) m)))

(defn -main [& [run-dir run-id specimen stable-build evidence-root fx-a fx-b]]
  (let [spec (keyword specimen)
        dest (str evidence-root "/" run-id "/" specimen)
        _ (doseq [s ["A" "B"]] (.mkdirs (io/file (str dest "/" s))))
        retained (atom {})
        retain! (fn [side name content]
                  (let [p (str dest "/" side "/" name)]
                    (spit p content)
                    (swap! retained assoc (str side "/" name) (sha256 content))))
        out (atom [])]

    ;; retain the receipt objects exactly as captured, then derive from the copies
    (doseq [side ["A" "B"] file ["stdout.edn" "receipt.edn"]]
      (let [src (str run-dir "/" side "/" file)]
        (when (exists? src) (retain! side file (slurp src)))))

    (doseq [file ["stdout.edn" "receipt.edn"]]
      (let [a (str dest "/A/" file) b (str dest "/B/" file)]
        (when (and (exists? a) (exists? b))
          (doseq [[p va vb] (diffs (read-edn a) (read-edn b))]
            (swap! out conj {:run run-id :specimen spec :path p :source file})))))

    ;; retain the UNEXCLUDED tree manifests, then derive tree observations from them
    (when (and fx-a fx-b (exists? fx-a) (exists? fx-b))
      (retain! "A" "full-tree-manifest.txt" (manifest->text (full-manifest fx-a)))
      (retain! "B" "full-tree-manifest.txt" (manifest->text (full-manifest fx-b)))
      (let [->m (fn [side] (into {} (for [l (str/split-lines (slurp (str dest "/" side "/full-tree-manifest.txt")))
                                          :when (seq l)]
                                      (let [[h p] (str/split l #"  " 2)] [p h]))))
            la (->m "A") lb (->m "B")]
        (doseq [p (sort (distinct (concat (remove (set (keys lb)) (keys la))
                                          (remove (set (keys la)) (keys lb))
                                          (for [[p h] la :when (and (contains? lb p) (not= h (get lb p)))] p))))]
          (swap! out conj {:run run-id :specimen spec :path ["tree" p] :source "full-tree-manifest.txt"}))))

    (let [files (into (sorted-map) @retained)
          capture-digest (sha256 (pr-str files))
          capture {:run run-id :specimen spec
                   :stable-build stable-build
                   ;; the commit that OWNS these generator bytes; the comparator
                   ;; reads the blob out of git at this commit rather than trusting
                   ;; the file lying beside it
                   :source-commit (git-head (str (.getParent (io/file *file*)) "/../.."))
                   :generator-sha256 (sha256-file *file*)
                   :files files
                   :capture-digest capture-digest
                   :created (str (java.time.Instant/now))}]
      (spit (str dest "/capture.edn") (with-out-str (clojure.pprint/pprint capture)))
      (binding [*print-length* nil]
        (doseq [o @out] (prn (assoc o :capture-digest capture-digest)))))))

(require 'clojure.pprint)
(apply -main *command-line-args*)
