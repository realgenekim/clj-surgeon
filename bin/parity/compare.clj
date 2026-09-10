#!/usr/bin/env bb
;; Byte-parity comparator.  Reads two run directories produced by bin/parity-run
;; and the declared-volatile field list, and returns PARITY or DIVERGENCE.
;;
;; Everything is compared byte for byte.  The ONLY latitude is the declared list
;; in bin/parity/volatile-fields.edn; a difference in any other field is a
;; divergence and is reported with the exact path to the field and both values.
(ns parity.compare
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]))

(defn- read-edn [f] (with-open [r (java.io.PushbackReader. (io/reader f))]
                      (edn/read {:eof ::eof} r)))

(defn- slurp-or [f d] (if (.exists (io/file f)) (str/trim (slurp f)) d))

(def ^:dynamic *decl* nil)

(defn- volatile-keys [decl]
  (into {} (for [{:keys [key match to]} (:fields decl)
                 :when (= match :key)]
             [key to])))

(defn- path-token [decl] (some (fn [{:keys [match to]}] (when (= match :path-in) to)) (:fields decl)))

(defn- key-under-rules [decl]
  (reduce (fn [m {:keys [key match under to]}]
            (if (= match :key-under) (assoc-in m [under key] to) m))
          {} (:fields decl)))

(defn- regex-rules [decl]
  (for [{:keys [match pattern to]} (:fields decl) :when (= match :regex)]
    [(re-pattern pattern) to]))

(defn- scrub-string
  "Replace every absolute path under one of the run roots with the declared token,
  then apply the declared regex rules. Nothing else is touched."
  ([s roots token] (scrub-string s roots token nil))
  ([s roots token rules]
   (let [s (reduce (fn [acc r] (str/replace acc r token)) s roots)]
     (reduce (fn [acc [re to]] (str/replace acc re to)) s (or rules [])))))

(defn normalise
  "Apply the DECLARED normalisation and nothing else."
  [form roots decl]
  (let [vk (volatile-keys decl)
        tok (path-token decl)
        rules (regex-rules decl)
        under (key-under-rules decl)]
    (walk/postwalk
      (fn [x]
        (cond
          (map? x) (reduce-kv (fn [m k v]
                                (assoc m k
                                  (cond
                                    (contains? vk k) (get vk k)
                                    (and (contains? under k) (map? v))
                                    (reduce-kv (fn [vm k2 v2]
                                                 (assoc vm k2 (get (get under k) k2 v2)))
                                               {} v)
                                    :else v)))
                              {} x)
          (string? x) (scrub-string x roots (or tok "<RUN-ROOT>") rules)
          :else x))
      form)))

;; ---------------------------------------------------------------------------
;; structural diff: return [path-vector a b] for the first N differences
(defn diffs
  ([a b] (diffs a b [] []))
  ([a b path acc]
   (cond
     (= a b) acc
     (and (map? a) (map? b))
     (let [ks (sort-by str (distinct (concat (keys a) (keys b))))]
       (reduce (fn [acc k] (diffs (get a k ::absent) (get b k ::absent) (conj path k) acc)) acc ks))
     (and (sequential? a) (sequential? b) (= (count a) (count b)))
     (reduce (fn [acc i] (diffs (nth a i) (nth b i) (conj path i) acc)) acc (range (count a)))
     :else (conj acc [path a b]))))

(defn- trunc [x] (let [s (pr-str x)] (if (> (count s) 300) (str (subs s 0 300) " …") s)))

(defn -main [& [dir-a dir-b decl-file]]
  (let [decl (read-edn decl-file)
        root-a (slurp-or (str dir-a "/fixture-root.txt") "")
        root-b (slurp-or (str dir-b "/fixture-root.txt") "")
        roots (remove str/blank? [root-a root-b])
        findings (atom [])
        note (fn [kind msg] (swap! findings conj [kind msg]))]

    ;; 1. exit code
    (let [ea (slurp-or (str dir-a "/exit.txt") "?") eb (slurp-or (str dir-b "/exit.txt") "?")]
      (when-not (= ea eb) (note :exit-code (format "exit code A=%s B=%s" ea eb))))

    ;; 2. tree bytes
    (let [ma (slurp-or (str dir-a "/tree-manifest.txt") "") mb (slurp-or (str dir-b "/tree-manifest.txt") "")]
      (when-not (= ma mb)
        (let [la (into {} (for [l (str/split-lines ma) :when (seq l)] (let [[h p] (str/split l #"  " 2)] [p h])))
              lb (into {} (for [l (str/split-lines mb) :when (seq l)] (let [[h p] (str/split l #"  " 2)] [p h])))
              only-a (sort (remove (set (keys lb)) (keys la)))
              only-b (sort (remove (set (keys la)) (keys lb)))
              changed (sort (for [[p h] la :when (and (contains? lb p) (not= h (get lb p)))] p))]
          (doseq [p only-a] (note :tree (str "only in A: " p)))
          (doseq [p only-b] (note :tree (str "only in B: " p)))
          (doseq [p changed] (note :tree (str "bytes differ: " p))))))

    ;; 3. stdout receipt object, after DECLARED normalisation only
    (let [oa (str dir-a "/stdout.edn") ob (str dir-b "/stdout.edn")]
      (if-not (and (.exists (io/file oa)) (.exists (io/file ob)))
        (note :receipt "stdout.edn missing on one side")
        (let [ra (normalise (read-edn oa) roots decl)
              rb (normalise (read-edn ob) roots decl)]
          (doseq [[p a b] (take 40 (diffs ra rb))]
            (note :receipt (format "%s: A=%s B=%s" (pr-str p) (trunc a) (trunc b)))))))

    ;; 4. the receipt FILE on disk, if the operation wrote one
    (let [oa (str dir-a "/receipt.edn") ob (str dir-b "/receipt.edn")]
      (when (and (.exists (io/file oa)) (.exists (io/file ob)))
        (let [ra (normalise (read-edn oa) roots decl)
              rb (normalise (read-edn ob) roots decl)]
          (doseq [[p a b] (take 40 (diffs ra rb))]
            (note :receipt-file (format "%s: A=%s B=%s" (pr-str p) (trunc a) (trunc b)))))))

    ;; 5. refusal / stderr text
    (let [rules (regex-rules decl)
          ea (scrub-string (slurp-or (str dir-a "/stderr.txt") "") roots (or (path-token decl) "<RUN-ROOT>") rules)
          eb (scrub-string (slurp-or (str dir-b "/stderr.txt") "") roots (or (path-token decl) "<RUN-ROOT>") rules)]
      (when-not (= ea eb) (note :stderr "stderr text differs after path normalisation")))

    (let [f @findings]
      (if (empty? f)
        (do (println "PARITY") (System/exit 0))
        (do (println "DIVERGENCE" (count f))
            (doseq [[k m] f] (println (str "  [" (name k) "] " m)))
            (System/exit 1))))))

(apply -main *command-line-args*)
