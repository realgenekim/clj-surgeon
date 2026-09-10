#!/usr/bin/env bb
;; Byte-parity comparator.
;;
;;   compare.clj <dir-A> <dir-B> <volatile-fields.edn> [<specimen-name>]
;;
;; Exit 0 PARITY · 1 DIVERGENCE · 2 REFUSED (the declaration or the captured
;; evidence is not fit to compare — never a parity verdict).
;;
;; Two contracts this file is the sole enforcer of:
;;
;;   1. THE DECLARATION CANNOT BE WIDENED SILENTLY.  Every rule must carry a
;;      non-empty :reason and an :evidence entry naming a stored run, specimen
;;      and field path that is recorded in bin/parity/observed-volatility.edn.
;;      A rule missing either, or naming an observation nobody made, REFUSES the
;;      whole run and names the rule.  A normalisation is permission for the
;;      candidate to change a field unnoticed; it has to be paid for.
;;
;;   2. ABSENCE IS A VALUE.  An artifact captured on one side and missing on the
;;      other is a DIVERGENCE naming the missing path — never a skipped
;;      comparison.  Optional artifacts are optional only where the specimen
;;      declares them so.
(ns parity.compare
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]))

(defn- read-edn [f] (with-open [r (java.io.PushbackReader. (io/reader f))]
                      (edn/read {:eof ::eof} r)))
(defn- exists? [f] (.exists (io/file f)))
(defn- slurp-or [f d] (if (exists? f) (str/trim (slurp f)) d))

;; ---------------------------------------------------------------------------
;; contract 1: the declaration is validated before anything is compared

(def ^:private evidence-file
  (str (.getParent (io/file *file*)) "/observed-volatility.edn"))

(defn- rule-name [{:keys [key pattern glob match]}]
  (str (or key pattern glob (some-> match name)) " (" (or (some-> match name) "tree-exclude") ")"))

(defn- observed? [observations {:keys [run specimen path]}]
  (some (fn [o] (and (= (:run o) run) (= (:specimen o) specimen) (= (:path o) path)))
        observations))

(defn validate-declaration
  "Return a vector of refusal strings; empty means the declaration may be used."
  [decl observations]
  (let [check (fn [what rule]
                (let [{:keys [reason evidence]} rule
                      nm (str what " " (rule-name rule))]
                  (cond
                    (not (string? reason))
                    [(str "REFUSING: " nm " has no :reason. A normalisation is permission for the "
                          "candidate to change a field unnoticed; state the physical reason two runs "
                          "of ONE build cannot agree on it.")]
                    (str/blank? reason)
                    [(str "REFUSING: " nm " has an empty :reason.")]
                    (not (map? evidence))
                    [(str "REFUSING: " nm " has no :evidence map. Name the observation: "
                          "{:run <run-id> :specimen <name> :path [<field path>]}.")]
                    (not (and (:run evidence) (:specimen evidence) (:path evidence)))
                    [(str "REFUSING: " nm " :evidence must carry :run, :specimen and :path; got "
                          (pr-str evidence))]
                    (not (observed? observations evidence))
                    [(str "REFUSING: " nm " names an observation that is not recorded in "
                          "observed-volatility.edn: " (pr-str evidence) ". A rule may only exist "
                          "because a run showed the field differing between two runs of one build.")]
                    :else [])))]
    (vec (concat (mapcat #(check "field rule" %) (:fields decl))
                 (mapcat #(check "tree exclusion" %) (:tree-excludes decl))))))

;; ---------------------------------------------------------------------------
;; normalisation — the declared latitude, and nothing else

(defn- volatile-keys [decl]
  (into {} (for [{:keys [key match to]} (:fields decl) :when (= match :key)] [key to])))
(defn- key-under-rules [decl]
  (reduce (fn [m {:keys [key match under to]}]
            (if (= match :key-under) (assoc-in m [under key] to) m)) {} (:fields decl)))
(defn- path-token [decl]
  (some (fn [{:keys [match to]}] (when (= match :path-in) to)) (:fields decl)))
(defn- regex-rules [decl]
  (for [{:keys [match pattern to]} (:fields decl) :when (= match :regex)]
    [(re-pattern pattern) to]))

(defn- scrub-string
  ([s roots token] (scrub-string s roots token nil))
  ([s roots token rules]
   (let [s (reduce (fn [acc r] (str/replace acc r token)) s roots)]
     (reduce (fn [acc [re to]] (str/replace acc re to)) s (or rules [])))))

(defn normalise [form roots decl]
  (let [vk (volatile-keys decl) tok (path-token decl)
        rules (regex-rules decl) under (key-under-rules decl)]
    (walk/postwalk
      (fn [x]
        (cond
          (map? x) (reduce-kv (fn [m k v]
                                (assoc m k
                                  (cond
                                    (contains? vk k) (get vk k)
                                    (and (contains? under k) (map? v))
                                    (reduce-kv (fn [vm k2 v2] (assoc vm k2 (get (get under k) k2 v2))) {} v)
                                    :else v)))
                              {} x)
          (string? x) (scrub-string x roots (or tok "<RUN-ROOT>") rules)
          :else x))
      form)))

;; ---------------------------------------------------------------------------
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

(defn- trunc [x] (let [s (pr-str x)] (if (> (count s) 300) (str (subs s 0 300) " …") s)))

;; ---------------------------------------------------------------------------
;; contract 2: absence is a value
;;
;; :required true  — the harness always writes it; missing on BOTH sides means
;;                   the capture itself failed, which is a REFUSAL, not parity.
;; :required false — governed by the specimen's :expects-receipt.
(def artifacts
  [{:file "exit.txt"          :required true  :kind :text}
   {:file "stdout.edn"        :required true  :kind :edn}
   {:file "stderr.txt"        :required true  :kind :text}
   {:file "tree-manifest.txt" :required true  :kind :manifest}
   {:file "receipt.edn"       :required false :kind :edn}
   {:file "receipt-path.txt"  :required false :kind :text}])

(defn -main [& [dir-a dir-b decl-file specimen-name]]
  (let [decl (read-edn decl-file)
        observations (if (exists? evidence-file) (:observations (read-edn evidence-file)) [])
        refusals (validate-declaration decl observations)]
    (when (seq refusals)
      (println "REFUSED" (count refusals))
      (doseq [r refusals] (println (str "  " r)))
      (System/exit 2))

    (let [specimens (let [f (str (.getParent (io/file *file*)) "/specimens.edn")]
                      (if (exists? f) (read-edn f) {}))
          spec (get specimens (keyword (or specimen-name "unknown")) {})
          expects-receipt (get spec :expects-receipt ::unset)
          root-a (slurp-or (str dir-a "/fixture-root.txt") "")
          root-b (slurp-or (str dir-b "/fixture-root.txt") "")
          roots (remove str/blank? [root-a root-b])
          rules (regex-rules decl)
          tok (or (path-token decl) "<RUN-ROOT>")
          findings (atom []) hard (atom [])
          note (fn [kind msg] (swap! findings conj [kind msg]))
          refuse (fn [msg] (swap! hard conj msg))]

      ;; ---- presence, before content -------------------------------------
      (doseq [{:keys [file required]} artifacts]
        (let [a (exists? (str dir-a "/" file)) b (exists? (str dir-b "/" file))]
          (cond
            (and a (not b)) (note :presence (str file " captured on A, MISSING on B — absence is a value, not a skipped comparison"))
            (and b (not a)) (note :presence (str file " captured on B, MISSING on A — absence is a value, not a skipped comparison"))
            (and (not a) (not b))
            (cond
              required (refuse (str "REFUSING: " file " was captured on NEITHER side. The harness always writes it, so this run captured nothing to compare; that is a capture failure, never parity."))
              (= file "receipt.edn")
              (cond
                (= expects-receipt ::unset)
                (refuse (str "REFUSING: no receipt was captured on either side and specimen "
                             (pr-str specimen-name) " does not declare :expects-receipt in "
                             "bin/parity/specimens.edn. Absence can only be accepted where it was predicted."))
                (true? expects-receipt)
                (note :presence "receipt.edn is MISSING on both sides but this specimen declares :expects-receipt true")
                :else nil)
              :else nil))))

      ;; a published receipt path that names a file nobody captured
      (doseq [[d side] [[dir-a "A"] [dir-b "B"]]]
        (let [named (slurp-or (str d "/receipt-path.txt") "")]
          (when (and (seq named) (not (exists? (str d "/receipt.edn"))))
            (note :presence (str side " published receipt_path " named " but no receipt.edn was captured")))))

      (when (seq @hard)
        (println "REFUSED" (count @hard))
        (doseq [r @hard] (println (str "  " r)))
        (System/exit 2))

      ;; ---- exit code -----------------------------------------------------
      (let [ea (slurp-or (str dir-a "/exit.txt") ::absent) eb (slurp-or (str dir-b "/exit.txt") ::absent)]
        (when-not (= ea eb) (note :exit-code (format "exit code A=%s B=%s" ea eb))))

      ;; ---- tree bytes ----------------------------------------------------
      (when (and (exists? (str dir-a "/tree-manifest.txt")) (exists? (str dir-b "/tree-manifest.txt")))
        (let [ma (slurp-or (str dir-a "/tree-manifest.txt") "") mb (slurp-or (str dir-b "/tree-manifest.txt") "")]
          (when-not (= ma mb)
            (let [->m (fn [m] (into {} (for [l (str/split-lines m) :when (seq l)]
                                         (let [[h p] (str/split l #"  " 2)] [p h]))))
                  la (->m ma) lb (->m mb)]
              (doseq [p (sort (remove (set (keys lb)) (keys la)))] (note :tree (str "only in A: " p)))
              (doseq [p (sort (remove (set (keys la)) (keys lb)))] (note :tree (str "only in B: " p)))
              (doseq [p (sort (for [[p h] la :when (and (contains? lb p) (not= h (get lb p)))] p))]
                (note :tree (str "bytes differ: " p)))))))

      ;; ---- receipt objects, after DECLARED normalisation only -------------
      (doseq [[file kind] [["stdout.edn" :receipt] ["receipt.edn" :receipt-file]]]
        (when (and (exists? (str dir-a "/" file)) (exists? (str dir-b "/" file)))
          (let [ra (normalise (read-edn (str dir-a "/" file)) roots decl)
                rb (normalise (read-edn (str dir-b "/" file)) roots decl)]
            (doseq [[p a b] (take 40 (diffs ra rb))]
              (note kind (format "%s: A=%s B=%s" (pr-str p) (trunc a) (trunc b)))))))

      ;; ---- refusal / stderr text -----------------------------------------
      (when (and (exists? (str dir-a "/stderr.txt")) (exists? (str dir-b "/stderr.txt")))
        (let [ea (scrub-string (slurp-or (str dir-a "/stderr.txt") "") roots tok rules)
              eb (scrub-string (slurp-or (str dir-b "/stderr.txt") "") roots tok rules)]
          (when-not (= ea eb) (note :stderr "stderr text differs after declared normalisation"))))

      (let [f @findings]
        (if (empty? f)
          (do (println "PARITY") (System/exit 0))
          (do (println "DIVERGENCE" (count f))
              (doseq [[k m] f] (println (str "  [" (name k) "] " m)))
              (System/exit 1)))))))

(apply -main *command-line-args*)
