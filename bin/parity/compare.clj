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
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.walk :as walk]))

(defn- read-edn [f] (with-open [r (java.io.PushbackReader. (io/reader f))]
                      (edn/read {:eof ::eof} r)))

(defn sha256 [^String s]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest d (.getBytes s "UTF-8"))))))
(defn- exists? [f] (.exists (io/file f)))
(declare normalise)
(defn- slurp-or [f d] (if (exists? f) (str/trim (slurp f)) d))

;; ---------------------------------------------------------------------------
;; contract 1: a rule is honoured only if its evidence can be RE-DERIVED from
;; retained bytes.
;;
;; A checked-in row asserting its own provenance is not evidence: a hand edit can
;; invent both the observation and the rule that cites it. So the comparator
;; re-opens the capture the observation names, recomputes every digest, and
;; RE-DERIVES the difference from those bytes. A rule citing a run with no
;; retained capture refuses, naming the run.

(def ^:private here (.getParent (io/file *file*)))
(def ^:private evidence-file (str here "/observed-volatility.edn"))
(def ^:private generator-file (str here "/observe-volatility.clj"))

(defn- rule-name [{:keys [key pattern glob match]}]
  (str (or key pattern glob (some-> match name)) " (" (or (some-> match name) "tree-exclude") ")"))

(defn- diff-paths-of
  "Re-derive, from the RETAINED bytes, every path that differed between the two
  captured sides. This is what an observation must appear in."
  [dir]
  (let [dv (fn dv [a b path acc]
             (cond
               (= a b) acc
               (and (map? a) (map? b))
               (reduce (fn [acc k] (dv (get a k ::absent) (get b k ::absent) (conj path k) acc))
                       acc (sort-by str (distinct (concat (keys a) (keys b)))))
               (and (sequential? a) (sequential? b) (= (count a) (count b)))
               (reduce (fn [acc i] (dv (nth a i) (nth b i) (conj path i) acc)) acc (range (count a)))
               :else (conj acc path)))
        object-paths
        (mapcat (fn [f]
                  (let [a (str dir "/A/" f) b (str dir "/B/" f)]
                    (if (and (exists? a) (exists? b)) (dv (read-edn a) (read-edn b) [] []) [])))
                ["stdout.edn" "receipt.edn"])
        tree-paths
        (let [a (str dir "/A/full-tree-manifest.txt") b (str dir "/B/full-tree-manifest.txt")]
          (if (and (exists? a) (exists? b))
            (let [->m (fn [f] (into {} (for [l (str/split-lines (slurp f)) :when (seq l)]
                                         (let [[h p] (str/split l #"  " 2)] [p h]))))
                  la (->m a) lb (->m b)]
              (map (fn [p] ["tree" p])
                   (distinct (concat (remove (set (keys lb)) (keys la))
                                     (remove (set (keys la)) (keys lb))
                                     (for [[p h] la :when (and (contains? lb p) (not= h (get lb p)))] p)))))
            []))]
    (set (concat object-paths tree-paths))))

(def ^:private capture-cache (atom {}))
(def ^:dynamic *doc* nil)

;; --- commit-bound provenance ----------------------------------------------
;; A generator digest checked against the file lying beside the comparator is
;; self-consistent and proves nothing: edit both and the story still hangs
;; together. The evidence therefore names the REPOSITORY COMMIT that owns the
;; generator, and the bytes are read out of git, not the working tree.
(defn- git [& args]
  (let [{:keys [exit out]} (apply shell/sh (concat ["git" "-C" here] args))]
    (when (zero? exit) (str/trim-newline out))))

(defn- git-blob [commit path]
  (let [{:keys [exit out]} (shell/sh "git" "-C" here "cat-file" "blob" (str commit ":" path))]
    (when (zero? exit) out)))

(defn verify-provenance
  "nil when the evidence document is bound to a real commit on this branch whose
  generator blob is the one that produced it."
  [doc]
  (let [{:keys [source-commit generator-sha256 stable-build]} doc
        rel "bin/parity/observe-volatility.clj"]
    (cond
      (not (string? source-commit))
      "REFUSING: observed-volatility.edn does not name the :source-commit that produced it. Provenance that names no commit is provenance nobody can check."
      (nil? (git "cat-file" "-e" (str source-commit "^{commit}")))
      (str "REFUSING: :source-commit " (pr-str source-commit) " is not a commit in this repository.")
      (not (zero? (:exit (shell/sh "git" "-C" here "merge-base" "--is-ancestor" source-commit "HEAD"))))
      (str "REFUSING: :source-commit " (pr-str source-commit) " is not an ancestor of HEAD. Evidence generated off this branch cannot authorise rules on it.")
      (nil? (git-blob source-commit rel))
      (str "REFUSING: commit " (pr-str source-commit) " contains no " rel " — it cannot be the generator's provenance authority.")
      (not= generator-sha256 (sha256 (git-blob source-commit rel)))
      (str "REFUSING: the generator blob at " (pr-str source-commit) " does not hash to the :generator-sha256 the evidence records. A modified generator with refreshed digests is exactly what this check exists to stop.")
      (not (string? stable-build))
      "REFUSING: observed-volatility.edn does not name the :stable-build both sides were built from."
      (not (zero? (:exit (shell/sh "git" "-C" here "cat-file" "-e" (str stable-build "^{commit}")))))
      (str "REFUSING: :stable-build " (pr-str stable-build) " is not a commit — the executor both sides ran must be nameable.")
      :else nil)))

;; --- 005: a rule is authorised by REPLAY, not by citation -------------------
;; Citing a real volatile path is not enough: any real difference could otherwise
;; buy permission to normalise an unrelated invariant. A rule is honoured only if,
;; replayed ALONE over the retained bytes, its selector picks out exactly the path
;; it cites, that difference disappears under it, and the difference is still
;; there under every OTHER rule — so the permission is paid for by the difference
;; it actually explains.

(defn- selector-shape-ok?
  [{:keys [match key under]} path]
  (case match
    :key       (= (last path) key)
    :key-under (and (= (last path) key) (= (last (butlast path)) under))
    (:regex :path-in) (not= (first path) "tree")
    true))

(defn- glob->re
  "A tree-exclusion glob as a regex over a manifest-relative path. `**` spans
  directories, `*` does not; every other character is literal."
  [g]
  (let [esc (fn [s] (clojure.string/replace s #"([.+^$(){}\[\]|?\\])" "\\\\$1"))
        parts (clojure.string/split g #"\*\*" -1)
        body (clojure.string/join ".*"
               (for [part parts]
                 (clojure.string/join "[^/]*"
                   (map esc (clojure.string/split part #"\*" -1)))))]
    (re-pattern (str "^/?" body "$"))))

(defn- tree-path-of [path] (when (= (first path) "tree") (str/replace (second path) #"^/" "")))

(defn verify-capture
  "nil when the cited observation is backed by retained bytes that still show it."
  [evidence-root {:keys [run specimen path] :as cited} cited-digest]
  (let [dir (str evidence-root "/" run "/" (name specimen))
        capture-file (str dir "/capture.edn")]
    (cond
      (not (exists? capture-file))
      (str "no capture is retained for run " (pr-str run) " specimen " (pr-str specimen)
           " under " evidence-root "/. An observation may only exist because a run produced it; "
           "a row that names a run nobody kept the bytes of is an assertion, not evidence.")
      :else
      (let [capture (read-edn capture-file)
            recomputed (into (sorted-map)
                             (for [[f _] (:files capture)]
                               [f (if (exists? (str dir "/" f)) (sha256 (slurp (str dir "/" f))) "MISSING")]))
            bad (for [[f h] (:files capture) :when (not= h (get recomputed f))] f)]
        (cond
          (seq bad)
          (str "the retained capture for run " (pr-str run) " specimen " (pr-str specimen)
               " does not hash to what capture.edn records: " (pr-str (vec bad)))
          (not= (sha256 (pr-str recomputed)) (:capture-digest capture))
          (str "the capture digest for run " (pr-str run) " specimen " (pr-str specimen) " does not recompute")
          (not= (:source-commit capture) (:source-commit *doc*))
          (str "the capture for run " (pr-str run) " names :source-commit " (pr-str (:source-commit capture))
               " but the evidence document names " (pr-str (:source-commit *doc*)))
          (not= (:generator-sha256 capture) (:generator-sha256 *doc*))
          (str "the capture for run " (pr-str run) " names a different generator than the authenticated document")
          (not= (:stable-build capture) (:stable-build *doc*))
          (str "the capture for run " (pr-str run) " names a different :stable-build than the authenticated document")
          (and cited-digest (not= cited-digest (:capture-digest capture)))
          (str "the observation cites capture digest " cited-digest
               " but the retained capture is " (:capture-digest capture))
          (not (contains? (or (get @capture-cache dir)
                              (get (swap! capture-cache assoc dir (diff-paths-of dir)) dir))
                          path))
          (str "the retained capture for run " (pr-str run) " specimen " (pr-str specimen)
               " does NOT show " (pr-str path) " differing. The bytes are the evidence; "
               "the row is only a claim about them.")
          :else nil)))))

(defn- capture-roots
  "The two fixture roots the retained capture was produced in. They are the only
  input that differed between the sides, and the :path-in rule is about them, so
  the replay has to know them exactly as the live comparison does."
  [dir]
  (vec (remove str/blank?
         (for [side ["A" "B"]]
           (let [f (str dir "/" side "/stdout.edn")]
             (when (exists? f)
               (let [m (read-edn f)]
                 (or (:workspace_root m) (get-in m [:structured :workspace_root])))))))))

(defn- normalised-diff-paths
  "The set of paths still differing in the retained capture under DECL."
  [dir decl]
  (let [dv (fn dv [a b path acc]
             (cond
               (= a b) acc
               (and (map? a) (map? b))
               (reduce (fn [acc k] (dv (get a k ::absent) (get b k ::absent) (conj path k) acc))
                       acc (sort-by str (distinct (concat (keys a) (keys b)))))
               (and (sequential? a) (sequential? b) (= (count a) (count b)))
               (reduce (fn [acc i] (dv (nth a i) (nth b i) (conj path i) acc)) acc (range (count a)))
               :else (conj acc path)))]
    (set (mapcat (fn [f]
                   (let [a (str dir "/A/" f) b (str dir "/B/" f)]
                     (if (and (exists? a) (exists? b))
                       (let [roots (capture-roots dir)]
                         (dv (normalise (read-edn a) roots decl) (normalise (read-edn b) roots decl) [] []))
                       [])))
                 ["stdout.edn" "receipt.edn"]))))

(def ^:private replay-cache (atom {}))
(defn- replay [dir decl]
  (let [k [dir (pr-str decl)]]
    (or (get @replay-cache k)
        (get (swap! replay-cache assoc k (normalised-diff-paths dir decl)) k))))

(defn authorise-rule
  "nil when replaying THIS rule alone over the retained bytes explains the exact
  difference it cites, and no other rule already does."
  [evidence-root decl rule {:keys [run specimen path]}]
  (let [dir (str evidence-root "/" run "/" (name specimen))
        tree? (= (first path) "tree")
        exclusion? (contains? rule :glob)]
    (cond
      (and exclusion? (not tree?))
      (str "a tree exclusion cites " (pr-str path) ", which is not a tree path")
      (and (not exclusion?) tree?)
      (str "a field rule cites the tree path " (pr-str path) "; only a tree exclusion can be paid for by one")

      exclusion?
      (let [p (tree-path-of path)
            mine (re-matches (glob->re (:glob rule)) p)
            others (filter #(and (not= % rule) (re-matches (glob->re (:glob %)) p)) (:tree-excludes decl))]
        (cond
          (nil? mine) (str "its glob " (pr-str (:glob rule)) " does not match its own evidence path " (pr-str p))
          (seq others) (str "the path " (pr-str p) " is already excluded by " (pr-str (mapv :glob others))
                            " — this exclusion is not what explains that difference")
          :else nil))

      (not (selector-shape-ok? rule path))
      (str "its selector does not select its evidence path " (pr-str path)
           " — a rule may only be paid for by the difference it actually explains")

      :else
      ;; NECESSARY and SUFFICIENT, replayed over the retained bytes:
      ;;   with the whole declaration the cited difference must be gone, and
      ;;   with this rule REMOVED it must come back.
      ;; Sufficiency alone would let a redundant rule ride on someone else's work;
      ;; necessity alone would let a rule that explains nothing sit in the list.
      ;; Together they stop the laundering attack: a :candidate_hash rule citing
      ;; the genuine [:elapsed_ms] observation is not necessary to explain
      ;; [:elapsed_ms] — the :elapsed_ms rule already does — so it refuses.
      (let [full    (replay dir {:fields (vec (:fields decl))})
            without (replay dir {:fields (vec (remove #(= % rule) (:fields decl)))})]
        (cond
          (contains? full path)
          (str "the declaration as a whole does NOT remove the difference at " (pr-str path)
               " that it cites")
          (not (contains? without path))
          (str "removing this rule leaves the difference at " (pr-str path) " explained anyway — "
               "the rule is not necessary for its own evidence, so that observation does not pay for it")
          :else nil)))))

(defn- observation-for [observations {:keys [run specimen path]}]
  (first (filter (fn [o] (and (= (:run o) run) (= (:specimen o) specimen) (= (:path o) path)))
                 observations)))

(defn validate-declaration
  "Return a vector of refusal strings; empty means the declaration may be used."
  [decl doc]
  (let [observations (:observations doc)
        evidence-root (str here "/" (or (:evidence-root doc) "evidence"))
        file-refusals
        (cond-> (if-let [bad (verify-provenance doc)] [bad] [])
          (not= (:generator-sha256 doc)
                (and (exists? generator-file) (sha256 (slurp generator-file))))
          (conj (str "REFUSING: observed-volatility.edn was generated by a different "
                     "observe-volatility.clj than the one in this tree. Regenerate the evidence; "
                     "an attestation that does not name the code that produced it attests to nothing."))
          (not= (:file-digest doc) (sha256 (pr-str (mapv #(into (sorted-map) %) observations))))
          (conj "REFUSING: observed-volatility.edn does not hash to its recorded :file-digest — it was edited by hand after generation."))
        check
        (fn [what rule]
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
              (nil? (observation-for observations evidence))
              [(str "REFUSING: " nm " names an observation that is not recorded in "
                    "observed-volatility.edn: " (pr-str evidence))]
              :else
              (if-let [bad (verify-capture evidence-root evidence
                                           (:capture-digest (observation-for observations evidence)))]
                [(str "REFUSING: " nm " — " bad)]
                (if-let [bad (authorise-rule evidence-root decl rule evidence)]
                  [(str "REFUSING: " nm " cites " (pr-str (:path evidence)) " but " bad)]
                  [])))))]
    (vec (concat file-refusals
                 (mapcat #(check "field rule" %) (:fields decl))
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
          ;; the run-root scrub happens ONLY when a :path-in rule is declared.
          ;; Applying it by default would make the rule unremovable and therefore
          ;; unpayable: nothing could ever show it was necessary.
          (string? x) (scrub-string x (if tok roots []) (or tok "<RUN-ROOT>") rules)
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
        doc (if (exists? evidence-file) (read-edn evidence-file) {})
        refusals (binding [*doc* doc] (validate-declaration decl doc))]
    (when (seq refusals)
      (println "REFUSED" (count refusals))
      (doseq [r refusals] (println (str "  " r)))
      (System/exit 2))

    (let [specimens (let [f (str here "/specimens.edn")]
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
      ;; The specimen's declaration is a TWO-WAY contract. Expected present and
      ;; absent is a divergence; expected ABSENT and present is a divergence too,
      ;; naming the declaration — otherwise a `false` entry silently switches the
      ;; receipt comparison off for a specimen that does publish one.
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
              :else nil))
          ;; the other direction: declared absent, but here
          (when (and (= file "receipt.edn") (or a b))
            (cond
              (= expects-receipt ::unset)
              (refuse (str "REFUSING: a receipt was captured but specimen " (pr-str specimen-name)
                           " does not declare :expects-receipt in bin/parity/specimens.edn. "
                           "A comparison whose predicted artifact state is unstated cannot be trusted either way."))
              (false? expects-receipt)
              (note :presence (str "receipt.edn is PRESENT (" (str/join " and " (remove nil? [(when a "A") (when b "B")]))
                                   ") but specimen " (pr-str specimen-name)
                                   " declares :expects-receipt false — the declaration is wrong, or the operation changed what it publishes"))
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
