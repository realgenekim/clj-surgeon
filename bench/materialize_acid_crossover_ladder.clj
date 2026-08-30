#!/usr/bin/env bb

(ns materialize-acid-crossover-ladder
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint])
  (:import
   (java.math BigInteger)
   (java.security MessageDigest)))

(def repo-root
  (fs/absolutize (or (first *command-line-args*) ".")))

(def fixture-root
  (fs/path repo-root "bench" "fixtures" "edit_portfolio"))

(def task-text
  (str
    "Rename the active feature API `acid-crossover.flags/legacy-mode?` to "
    "`acid-crossover.flags/command-mode?` everywhere under src/acid_crossover. "
    "Determine the affected files, owners, and exact edit sites from the source "
    "before changing anything.\n\n"
    "Update the related `:legacy-mode` keyword and exact `\"legacy-mode\"` wire "
    "literal only where they participate in the same active feature records. "
    "Preserve migration-history strings and comments that merely mention the old "
    "name, and preserve every unrelated byte.\n\n"
    "Apply the complete discovered decision as one coherent mutation action if "
    "your available editor supports it. Verify all changed files and report the "
    "discovery and mutation routes, including whether the mutation succeeded on "
    "the first attempt.\n"))

(defn sha256
  [source]
  (format "%064x"
          (BigInteger. 1
                       (.digest (MessageDigest/getInstance "SHA-256")
                                (.getBytes source "UTF-8")))))

(defn names
  [state]
  (case state
    :before {:symbol "legacy-mode?"
             :keyword ":legacy-mode"
             :wire "legacy-mode"}
    :after {:symbol "command-mode?"
            :keyword ":command-mode"
            :wire "command-mode"}))

(defn flags-source
  [state]
  (let [{:keys [symbol keyword wire]} (names state)]
    (str
      "(ns acid-crossover.flags)\n\n"
      "(def migration-history\n"
      "  \"legacy-mode remains in the migration guide\")\n\n"
      "(defn " symbol " [settings]\n"
      "  {:enabled? (= " keyword " (:mode settings))\n"
      "   :wire-name \"" wire "\"})\n")))

(defn caller-source
  [state namespace owner reference-count]
  (let [{:keys [symbol keyword wire]} (names state)
        references (apply str
                          (repeat reference-count
                                  (str "    (flags/" symbol " settings)\n")))]
    (str
      "(ns " namespace "\n"
      "  (:require\n"
      "   [acid-crossover.flags :as flags]))\n\n"
      ";; Historical prose is not an active feature identifier: legacy-mode.\n"
      "(def migration-history\n"
      "  \"legacy-mode remains searchable in release notes\")\n\n"
      "(defn " owner " [settings]\n"
      "  {:checks\n"
      "   [\n"
      references
      "   ]\n"
      "   :mode " keyword "\n"
      "   :wire-name \"" wire "\"})\n")))

(def file-specs
  [{:path "src/acid_crossover/flags.clj"
    :before (flags-source :before)
    :after (flags-source :after)
    :changes 3}
   {:path "src/acid_crossover/ui.clj"
    :before (caller-source :before "acid-crossover.ui" "command-panel" 3)
    :after (caller-source :after "acid-crossover.ui" "command-panel" 3)
    :changes 5}
   {:path "src/acid_crossover/jobs.clj"
    :before (caller-source :before "acid-crossover.jobs" "job-policy" 2)
    :after (caller-source :after "acid-crossover.jobs" "job-policy" 2)
    :changes 4}
   {:path "src/acid_crossover/audit.clj"
    :before (caller-source :before "acid-crossover.audit" "audit-entry" 2)
    :after (caller-source :after "acid-crossover.audit" "audit-entry" 2)
    :changes 4}
   {:path "src/acid_crossover/api.clj"
    :before (caller-source :before "acid-crossover.api" "request-policy" 6)
    :after (caller-source :after "acid-crossover.api" "request-policy" 6)
    :changes 8}
   {:path "src/acid_crossover/checks.clj"
    :before (caller-source :before "acid-crossover.checks" "acceptance-check" 6)
    :after (caller-source :after "acid-crossover.checks" "acceptance-check" 6)
    :changes 8}])

(def rungs
  [{:id :acid-crossover-03 :files 1 :changes 3}
   {:id :acid-crossover-08 :files 2 :changes 8}
   {:id :acid-crossover-16 :files 4 :changes 16}
   {:id :acid-crossover-32 :files 6 :changes 32}])

(defn write-source!
  [task-dir snapshot {:keys [path] :as spec}]
  (let [target (fs/path task-dir (name snapshot) path)]
    (fs/create-dirs (fs/parent target))
    (spit (str target) (get spec snapshot))))

(defn capsule
  [{:keys [id files changes]} specs]
  {:id id
   :decision-boundary :affected-surface-not-supplied
   :provenance
   {:kind :realistic-scale-family
    :source "one expanded feature-rename family with reference and literal migration"
    :minimized
    (str "prefix-stable rung with " changes " exact changes across " files
         " files; active identifiers coexist with preserved history decoys")}
   :targets (mapv :path specs)
   :hashes
   (into (sorted-map)
         (map (fn [{:keys [path before after]}]
                [path {:before (sha256 before)
                       :after (sha256 after)}]))
         specs)
   :verification {:exact-bytes-secondary true
                  :meaning-preserved true
                  :parse-clojure true}
   :expected {:changed-files files :edits changes}})

(defn materialize-rung!
  [{:keys [id files] :as rung}]
  (let [task-dir (fs/path fixture-root (name id))
        specs (subvec (vec file-specs) 0 files)]
    (doseq [spec specs
            snapshot [:before :after]]
      (write-source! task-dir snapshot spec))
    (spit (str (fs/path task-dir "task.txt")) task-text)
    (with-open [writer (io/writer (str (fs/path task-dir "capsule.edn")))]
      (binding [*out* writer]
        (pprint/pprint (capsule rung specs))))))

(doseq [rung rungs]
  (materialize-rung! rung))

(println "Materialized acid crossover ladder:" (mapv :id rungs))
