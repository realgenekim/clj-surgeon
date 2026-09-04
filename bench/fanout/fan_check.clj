#!/usr/bin/env bb
;; fan_check.clj — checks 1, 2, 3 and 6 of the FAN acceptance (sl1 "Acceptance").
;; Called by rescore-FAN.sh; prints one CHECK line per check with COMPUTED numbers and
;; exits non-zero if any failed.  It never prints a verdict word over a missing number:
;; a manifest, canonical tree or worktree it cannot read is a FAIL naming the reason.
;;
;;   bb fan_check.clj <worktree> <manifest.edn> <canonical-dir> <base-sha>
(ns fan-check
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [rewrite-clj.parser :as p]
            [rewrite-clj.node :as n])
  (:import [java.security MessageDigest]))

(def failures (atom []))
(defn check! [n label ok? detail]
  (println (format "CHECK %d %s: %s %s" n label (if ok? "PASS" "FAIL") detail))
  (when-not ok? (swap! failures conj (format "CHECK %d %s" n label))))

(defn sha256 [^String s]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest md (.getBytes s "UTF-8"))))))

(defn norm
  "Form tree modulo WHITESPACE.  Comments, metadata and #_ discards are KEPT, so a
   dropped comment or a moved discard is a difference, which is what sl1 asks for."
  [nd]
  (cond
    (n/whitespace? nd) nil
    (n/inner? nd) (into [(n/tag nd)] (keep norm (n/children nd)))
    :else [(n/tag nd) (n/string nd)]))

(defn norm-file [f]
  (try
    {:ok true :tree (keep norm (n/children (p/parse-file-all (io/file f))))}
    (catch Exception e {:ok false :err (.getMessage e)})))

(defn -main [wt manifest-path canon base]
  (let [m (read-string (slurp manifest-path))
        targets (:targets m)
        target-files (set (map :file targets))
        ;; -z / NUL-separated, raw bytes: `--name-only` (no -z) C-quotes any path
        ;; containing a backslash regardless of core.quotePath, so a legal POSIX
        ;; path with a literal "\" component never string-matches the manifest's
        ;; raw spelling (inb-9c18e2). -z disables that quoting entirely.
        gd (sh "git" "-C" wt "diff" "-z" "--name-only" base)
        _ (when-not (zero? (:exit gd))
            (println "CHECK 1 file-set: FAIL git diff failed:" (str/trim (:err gd)))
            (System/exit 1))
        ;; Every git listing this check consumes is fail-closed: a listing process
        ;; that cannot be trusted must never read as an empty set (a missing
        ;; untracked-extra listing would let a real extra file disappear and the
        ;; gate false-PASS). Check `ls-files`'s own exit exactly as `diff`'s is
        ;; checked above, before any of its output is parsed.
        untracked (sh "git" "-C" wt "ls-files" "-z" "--others" "--exclude-standard")
        _ (when-not (zero? (:exit untracked))
            (println (format "CHECK 1 file-set: ERROR git ls-files exit=%d %s"
                              (:exit untracked) (str/trim (:err untracked))))
            (System/exit 1))
        ;; NUL framing (-z) only ever produces EMPTY separators between records --
        ;; never a separator that is nonempty whitespace -- so the right predicate
        ;; is `empty?`. `str/blank?` is also true for a legal POSIX path that is
        ;; itself all whitespace (e.g. a file literally named " "), which would
        ;; silently drop a real record instead of just the framing artifacts.
        split-nul (fn [s] (remove empty? (str/split s (re-pattern (str (char 0))))))
        changed (into #{} (split-nul (:out gd)))
        extra-untracked (into #{} (split-nul (:out untracked)))
        changed-all (into changed extra-untracked)]

    ;; ---- CHECK 1: file set equals the manifest's target set exactly, no extras ----
    (let [missing (sort (remove changed-all target-files))
          extras  (sort (remove target-files changed-all))]
      (check! 1 "file-set" (and (empty? missing) (empty? extras))
              (format "changed=%d expected=%d missing=%d %s extras=%d %s"
                      (count changed-all) (count target-files)
                      (count missing) (pr-str (vec (take 4 missing)))
                      (count extras) (pr-str (vec (take 4 extras))))))

    ;; ---- CHECK 2: form equality against the derived canonical -------------------
    (let [results (for [t targets
                        :let [a (norm-file (io/file wt (:file t)))
                              b (norm-file (io/file canon (:file t)))]]
                    {:file (:file t)
                     :parses (:ok a)
                     :equal (and (:ok a) (:ok b) (= (:tree a) (:tree b)))
                     :err (:err a)})
          unparseable (filter (complement :parses) results)
          unequal (filter #(and (:parses %) (not (:equal %))) results)]
      (check! 2 "form-equality"
              (and (empty? unparseable) (empty? unequal))
              (format "compared=%d equal=%d unparseable=%d %s unequal=%d %s"
                      (count results) (count (filter :equal results))
                      (count unparseable)
                      (pr-str (vec (take 2 (map (juxt :file :err) unparseable))))
                      (count unequal) (pr-str (vec (take 4 (map :file unequal)))))))

    ;; ---- CHECK 3: protected regions, sha256 from the manifest -------------------
    (let [rows (for [t targets
                     pr* (:protected t)
                     :let [body (try (slurp (io/file wt (:file t))) (catch Exception _ nil))]]
                 {:file (:file t) :label (:label pr*)
                  :manifest-ok (= (:sha256 pr*) (sha256 (:text pr*)))
                  :present (boolean (and body (str/includes? body (:text pr*))))})
          bad-manifest (remove :manifest-ok rows)
          gone (filter #(and (:manifest-ok %) (not (:present %))) rows)]
      (check! 3 "protected-regions"
              (and (empty? bad-manifest) (empty? gone))
              (format "regions=%d intact=%d manifest-sha-mismatch=%d damaged=%d %s"
                      (count rows) (count (filter :present rows))
                      (count bad-manifest) (count gone)
                      (pr-str (vec (take 4 (map (juxt :file :label) gone)))))))

    ;; ---- CHECK 6: residue, and no introduced alias shadows a binding ------------
    (let [src (io/file wt "src")
          all-src (filter #(and (.isFile %) (re-find #"\.cljc?$" (.getName %)))
                          (file-seq src))
          lib-re (re-pattern (str (str/replace (:lib (:old m)) "." "\\.") "(?![0-9A-Za-z_-])"))
          lib-hits (for [f all-src
                         :let [c (slurp f)]
                         :when (re-find lib-re c)]
                     (str f))
          alias-rows (for [t targets
                           :let [c (try (slurp (io/file wt (:file t))) (catch Exception _ ""))
                                 ;; ANY qualified use of the old var except the decoy
                                 ;; namespace's own -- an agent that migrates the alias
                                 ;; but not the var leaves `st2/find-event`, which a
                                 ;; regex keyed only on the OLD alias would score 0.
                                 old-site (re-pattern (str "(?<![A-Za-z0-9_.-])([A-Za-z0-9_.*+!?<>=-]+)/" (:var (:old m))))
                                 want (re-pattern (str "\\[" (str/replace (:lib (:new m)) "." "\\.")
                                                       "\\s+:as\\s+" (str/replace (:new-alias t) "-" "\\-")
                                                       "\\]"))
                                 ;; every alias bound in this file's ns form
                                 aliases (map second (re-seq #":as\s+([A-Za-z0-9*+!_'?<>=/.-]+)" c))
                                 referred (mapcat #(str/split (str/trim %) #"\s+")
                                                  (map second (re-seq #":refer\s+\[([^\]]*)\]" c)))]]
                       {:file (:file t)
                        :old-site-residue (count (remove #(= "other" (second %))
                                                   (re-seq old-site c)))
                        :new-alias-present (boolean (re-find want c))
                        :alias-bound-twice (> (count (filter #{(:new-alias t)} aliases)) 1)
                        :alias-shadows-refer (boolean (some #{(:new-alias t)} referred))})
          residue (filter #(pos? (:old-site-residue %)) alias-rows)
          wrong-alias (remove :new-alias-present alias-rows)
          shadowing (filter #(or (:alias-bound-twice %) (:alias-shadows-refer %)) alias-rows)]
      (check! 6 "residue-and-alias"
              (and (empty? lib-hits) (empty? residue) (empty? wrong-alias) (empty? shadowing))
              (format "src-files=%d old-lib-hits=%d %s old-site-residue=%d %s wrong-or-missing-alias=%d %s shadowing=%d %s"
                      (count all-src) (count lib-hits) (pr-str (vec (take 3 lib-hits)))
                      (count residue) (pr-str (vec (take 3 (map :file residue))))
                      (count wrong-alias) (pr-str (vec (take 4 (map :file wrong-alias))))
                      (count shadowing) (pr-str (vec (take 3 (map :file shadowing)))))))

    (if (seq @failures)
      (do (println (str "fan_check: FAILED " (str/join ", " @failures))) (System/exit 1))
      (do (println "fan_check: 4/4 structural checks passed") (System/exit 0)))))

(apply -main *command-line-args*)
