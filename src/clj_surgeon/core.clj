(ns clj-surgeon.core
  "clj-surgeon: structural operations on Clojure namespaces.

   Babashka CLI. Returns EDN.

   Usage:
     clj-surgeon <command> [<file>] [--flag value …]
     clj-surgeon -h            top-level help
     clj-surgeon <command> -h  per-command help

   Examples:
     clj-surgeon ls src/foo.clj
     clj-surgeon mv src/foo.clj --form my-fn --before other-fn --dry-run
     clj-surgeon extract src/foo.clj --forms '[a b c]' --to src/foo/sub.clj --apply"
  (:require [clj-surgeon.outline :as outline]
            [clj-surgeon.forms :as forms]
            [clj-surgeon.forward-refs :as fwd]
            [clj-surgeon.move :as move]
            [clj-surgeon.analyze :as analyze]
            [clj-surgeon.rename :as rename]
            [clj-surgeon.fix-declares :as fix-declares]
            [clj-surgeon.extract :as extract]
            [clj-surgeon.cljc.merge :as cljc-merge]
            [clj-surgeon.cljc.split :as cljc-split]
            [clj-surgeon.cljc.require-ops :as cljc-req]
            [clj-surgeon.cljc.analyze :as cljc-analyze]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

;; ============================================================
;; Op implementations — same as before, but take a normalized opts map
;; ============================================================

(defn run-outline [{:keys [file]}]
  (let [result (outline/outline file)
        ns-name (:ns result)
        forward-refs (when ns-name
                       (fwd/detect-forward-refs file ns-name))]
    (assoc result :forward-refs (or forward-refs []))))

(defn run-mv [opts]
  (move/move-form opts))

(defn run-declares [{:keys [file]}]
  (let [ol (outline/outline file)
        declares (->> (:forms ol)
                      (filter #(= 'declare (:type %))))
        zloc (analyze/file->zloc file)
        topo (analyze/topological-sort zloc)
        truly-cyclic (set (:cycles topo))
        fwd (when (:ns ol)
              (set (map #(str (:name %))
                        (fwd/detect-forward-refs file (:ns ol)))))]
    {:file file
     :declares
     (mapv (fn [d]
             (let [name-str (str (:name d))
                   has-forward-ref? (contains? fwd name-str)
                   in-cycle? (contains? truly-cyclic name-str)]
               {:name name-str
                :line (:line d)
                :needed? (or in-cycle? has-forward-ref?)}))
           declares)
     :summary {:total (count declares)
               :removable (count (remove #(or (contains? truly-cyclic (str (:name %)))
                                              (contains? fwd (str (:name %))))
                                         declares))
               :needed (count (filter #(or (contains? truly-cyclic (str (:name %)))
                                           (contains? fwd (str (:name %))))
                                      declares))}}))

(defn run-deps [{:keys [file form]}]
  (let [zloc (analyze/file->zloc file)
        deps (analyze/intra-ns-deps zloc)]
    (if form
      (first (filter #(= form (:name %)) deps))
      deps)))

(defn run-topo [{:keys [file]}]
  (let [zloc (analyze/file->zloc file)]
    (analyze/topological-sort zloc)))

(defn run-closure [{:keys [file form]}]
  (let [zloc (analyze/file->zloc file)]
    (analyze/extraction-closure zloc form)))

(defn run-ls-deps [{:keys [file form]}]
  (let [zloc (analyze/file->zloc file)
        deps (analyze/intra-ns-deps zloc)]
    (analyze/dep-tree deps form)))

(defn run-cljc-merge [{:keys [clj cljs out]}]
  (let [cljc-src (cljc-merge/merge-files (slurp clj) (slurp cljs))]
    (if out
      (do (spit out cljc-src) {:wrote out :bytes (count cljc-src)})
      cljc-src)))

(defn run-cljc-split [{:keys [file clj-out cljs-out]}]
  (let [{:keys [clj cljs] :as result} (cljc-split/split-file (slurp file))]
    (cond-> result
      clj-out  (do (spit clj-out clj)   (assoc :wrote-clj clj-out))
      cljs-out (do (spit cljs-out cljs) (assoc :wrote-cljs cljs-out)))))

(defn run-cljc-add-require [{:keys [file platform ns as out]}]
  (let [updated (cljc-req/add-require (slurp file)
                                      {:platform platform :ns ns :as as})]
    (if out
      (do (spit out updated) {:wrote out :bytes (count updated)})
      updated)))

(defn run-cljc-analyze [{:keys [file clj cljs]}]
  (cond
    file (cljc-analyze/analyze-cljc (slurp file))
    (and clj cljs) (cljc-analyze/analyze-pair (slurp clj) (slurp cljs))
    :else {:error "supply <file> (a .cljc) or --clj + --cljs"}))

;; ============================================================
;; Output rendering
;; ============================================================

(defn- pprint-outline
  "Custom printer for :ls / :outline output. Header keys pprinted normally,
   but :forms is laid out one form per line so output is greppable and
   scannable."
  [result]
  (let [{:keys [forms]} result
        header (dissoc result :forms)]
    (print "{")
    (let [pairs (seq header)]
      (doseq [[i [k v]] (map-indexed vector pairs)]
        (when (pos? i) (print "\n "))
        (pr k) (print " ") (pr v)))
    (when (seq header) (print "\n "))
    (print ":forms")
    (if (empty? forms)
      (println " []}")
      (do (println " [")
          (doseq [f forms]
            (print "  ") (pr f) (println))
          (println " ]}")))))

(defn- has-forms? [r]
  (and (map? r) (contains? r :forms)))

(defn- print-result [result]
  (cond
    (string? result)    (println result)
    (has-forms? result) (pprint-outline result)
    :else               (pp/pprint result)))

;; ============================================================
;; Command table
;; ============================================================
;;
;; Each command:
;;   :summary    one-liner shown in top-level help
;;   :usage      usage string shown in per-command help
;;   :details    optional multi-line description for -h
;;   :positional [keys...]  positional arg names; bound from CLI in order
;;   :flags      {flag-key {:doc str :type :string/:bool/:edn}}
;;   :run        fn that takes the normalized opts map

(declare commands)  ;; forward — :help references it

(defn- help-cmd [_]
  (println "clj-surgeon — structural operations on Clojure namespaces\n")
  (println "Usage: clj-surgeon <command> [<file>] [--flag value …]\n")
  (println "Commands:")
  (let [names (sort (keys commands))
        w (apply max (map count names))]
    (doseq [n names
            :let [c (commands n)]]
      (printf "  %-20s %s%n" n (:summary c ""))))
  (println "\nGlobal flags:")
  (println "  -h, --help              show this help")
  (println "\nRun 'clj-surgeon <command> -h' for per-command help.")
  nil)

(def commands
  {"ls"
   {:summary    "Outline a file — every top-level form with line ranges, names, args"
    :usage      "clj-surgeon ls <file>"
    :positional [:file]
    :run        run-outline}

   "outline"
   {:summary    "Alias for 'ls'"
    :usage      "clj-surgeon outline <file>"
    :positional [:file]
    :run        run-outline}

   "deps"
   {:summary    "Intra-namespace dependency graph (or one form's deps)"
    :usage      "clj-surgeon deps <file> [--form <name>]"
    :positional [:file]
    :flags      {:form {:doc "limit to a single form's deps" :type :string}}
    :run        run-deps}

   "topo"
   {:summary    "Topologically sort the file's forms"
    :usage      "clj-surgeon topo <file>"
    :positional [:file]
    :run        run-topo}

   "ls-deps"
   {:summary    "Transitive dep tree for one form"
    :usage      "clj-surgeon ls-deps <file> --form <name>"
    :positional [:file]
    :flags      {:form {:doc "the form to start the tree from" :type :string}}
    :run        run-ls-deps}

   "ls-extract"
   {:summary    "Minimal extractable unit for one form (form + private helpers)"
    :usage      "clj-surgeon ls-extract <file> --form <name>"
    :positional [:file]
    :flags      {:form {:doc "the form to extract" :type :string}}
    :run        run-closure}

   "declares"
   {:summary    "Audit which (declare …) forms are still needed"
    :usage      "clj-surgeon declares <file>"
    :positional [:file]
    :run        run-declares}

   "mv"
   {:summary    "Reorder a form within a file"
    :usage      "clj-surgeon mv <file> --form <name> --before <name> [--apply]"
    :details    "Default mode is dry-run (plan only). Pass --apply to write changes to the file."
    :positional [:file]
    :flags      {:form   {:doc "form to move" :type :string}
                 :before {:doc "move :form before this name" :type :string}
                 :apply  {:doc "apply changes (default: dry-run plan)" :type :bool}}
    :run        (fn [{:keys [apply] :as opts}]
                  ;; move/move-form takes :dry-run as its safety flag;
                  ;; invert :apply for compat.
                  (move/move-form (assoc opts :dry-run (not apply))))}

   "fix-declares"
   {:summary    "Plan / execute removal of unnecessary declares"
    :usage      "clj-surgeon fix-declares <file> [--apply]"
    :details    "Default mode is dry-run (plan only). Pass --apply to write changes."
    :positional [:file]
    :flags      {:apply {:doc "apply changes (default: dry-run plan)" :type :bool}}
    :run        (fn [{:keys [file apply]}]
                  (if apply
                    (fix-declares/execute! file)
                    (fix-declares/plan file)))}

   "extract"
   {:summary    "Move forms to a new namespace"
    :usage      "clj-surgeon extract <file> --forms '[a b c]' --to <newfile> [--apply]"
    :details    "Default mode is dry-run (plan only). Pass --apply to write changes."
    :positional [:file]
    :flags      {:forms {:doc "EDN vector of form names" :type :edn}
                 :to    {:doc "target file path" :type :string}
                 :apply {:doc "apply changes (default: dry-run plan)" :type :bool}}
    :run        (fn [{:keys [apply] :as opts}]
                  (if apply (extract/execute! opts) (extract/plan opts)))}

   "rename-ns"
   {:summary    "Rename a namespace prefix across the repo"
    :usage      "clj-surgeon rename-ns --from <old> --to <new> [--root <dir>] [--apply]"
    :details    "Default mode is dry-run (plan only). Pass --apply to write changes."
    :flags      {:from  {:doc "old namespace prefix" :type :string}
                 :to    {:doc "new namespace prefix" :type :string}
                 :root  {:doc "repo root (default cwd)" :type :string}
                 :apply {:doc "apply changes (default: dry-run plan)" :type :bool}}
    :run        (fn [{:keys [apply] :as opts}]
                  (if apply (rename/execute! opts) (rename/plan opts)))}

   "cljc-merge"
   {:summary    "Combine parallel CLJ + CLJS files into one CLJC"
    :usage      "clj-surgeon cljc-merge --clj <file> --cljs <file> [--out <file>]"
    :flags      {:clj  {:doc "input .clj path" :type :string}
                 :cljs {:doc "input .cljs path" :type :string}
                 :out  {:doc "output .cljc path; omitted = stdout" :type :string}}
    :run        run-cljc-merge}

   "cljc-split"
   {:summary    "Split a CLJC file into parallel CLJ + CLJS sources"
    :usage      "clj-surgeon cljc-split <file> [--clj-out <file>] [--cljs-out <file>]"
    :positional [:file]
    :flags      {:clj-out  {:doc "output .clj path" :type :string}
                 :cljs-out {:doc "output .cljs path" :type :string}}
    :run        run-cljc-split}

   "cljc-add-require"
   {:summary    "Platform-aware require addition for a CLJC file"
    :usage      "clj-surgeon cljc-add-require <file> --platform <:clj|:cljs|:cljc> --ns <ns> [--as <alias>] [--out <file>]"
    :positional [:file]
    :flags      {:platform {:doc ":clj | :cljs | :cljc" :type :edn}
                 :ns       {:doc "namespace to require" :type :edn}
                 :as       {:doc "alias" :type :edn}
                 :out      {:doc "output path; omitted = stdout" :type :string}}
    :run        run-cljc-add-require}

   "cljc-analyze"
   {:summary    "Structured classification of CLJC requires + forms"
    :usage      "clj-surgeon cljc-analyze <file>\n       clj-surgeon cljc-analyze --clj <file> --cljs <file>"
    :positional [:file]
    :flags      {:clj  {:doc "input .clj path (when analyzing a pair)" :type :string}
                 :cljs {:doc "input .cljs path (when analyzing a pair)" :type :string}}
    :run        run-cljc-analyze}

   "help"
   {:summary "Show top-level help"
    :usage   "clj-surgeon help"
    :run     help-cmd}})

;; ============================================================
;; Per-command help
;; ============================================================

(defn- print-cmd-help [cmd-name]
  (let [c (commands cmd-name)]
    (println (str cmd-name " — " (:summary c)))
    (println)
    (println "Usage:")
    (println "  " (:usage c))
    (when-let [d (:details c)]
      (println)
      (println d))
    (when-let [flags (:flags c)]
      (println)
      (println "Flags:")
      (doseq [[k {:keys [doc type]}] flags]
        (printf "  --%-15s %s%n" (name k) (str doc " (" (name type) ")"))))
    nil))

;; ============================================================
;; Arg parsing
;; ============================================================

(defn- parse-flag-val [type s]
  (case type
    :bool   true
    :edn    (read-string s)
    :string s
    s))

(defn- parse-cmd-args
  "Parse args for a known command. Returns the normalized opts map.
   - First non-flag arg fills in :file (or whatever the command declares
     as its first positional).
   - --flag value pairs fill in flag keys.
   - --bool-flag stands alone."
  [cmd args]
  (let [{:keys [positional flags]} cmd
        flag-info (or flags {})]
    (loop [args args
           opts {}
           pos-remaining positional]
      (if (empty? args)
        opts
        (let [a (first args)]
          (cond
            (or (= a "-h") (= a "--help"))
            (assoc opts :help true)

            (str/starts-with? a "--")
            (let [k (keyword (subs a 2))
                  info (flag-info k)
                  type (:type info :string)]
              (if (= type :bool)
                (recur (rest args) (assoc opts k true) pos-remaining)
                (recur (drop 2 args)
                       (assoc opts k (parse-flag-val type (second args)))
                       pos-remaining)))

            ;; positional
            (seq pos-remaining)
            (recur (rest args)
                   (assoc opts (first pos-remaining) a)
                   (rest pos-remaining))

            ;; trailing unrecognized
            :else
            (do (binding [*out* *err*]
                  (println "Warning: unrecognized arg:" a))
                (recur (rest args) opts pos-remaining))))))))

;; ============================================================
;; Legacy keyword-style args (REPL-flavored): :op :ls :file foo.clj
;; ============================================================

(defn- legacy-keyword-style? [args]
  (and (seq args)
       (let [a (first args)]
         (and (string? a) (str/starts-with? a ":")))))

(defn- parse-val-legacy [s]
  (cond
    (= s "true") true
    (= s "false") false
    (.startsWith s ":") (keyword (subs s 1))
    (.startsWith s "[") (read-string s)
    (.startsWith s "{") (read-string s)
    :else s))

(defn- legacy-cmd-from-op [op-kw]
  ;; map old :op keywords to new subcommand names
  (let [k (name op-kw)]
    (case k
      "ls" "ls"
      "outline" "ls"
      "mv" "mv"
      "deps" "deps"
      "topo" "topo"
      "declares" "declares"
      "ls-extract" "ls-extract"
      "ls-deps" "ls-deps"
      "rename-ns"  "rename-ns"
      "rename-ns!" "rename-ns"
      "fix-declares"  "fix-declares"
      "fix-declares!" "fix-declares"
      "extract"  "extract"
      "extract!" "extract"
      "cljc-merge" "cljc-merge"
      "cljc-split" "cljc-split"
      "cljc-add-require" "cljc-add-require"
      "cljc-analyze" "cljc-analyze"
      nil)))

(defn- legacy-needs-execute? [op-kw]
  (str/ends-with? (name op-kw) "!"))

(defn- parse-legacy [args]
  (let [pairs (->> args
                   (partition 2)
                   (map (fn [[k v]] [(keyword (subs k 1)) (parse-val-legacy v)]))
                   (into {}))
        op (:op pairs)
        cmd-name (legacy-cmd-from-op op)]
    (when-not cmd-name
      (binding [*out* *err*]
        (println "Unknown :op" op))
      (System/exit 1))
    {:cmd-name cmd-name
     :opts (cond-> (dissoc pairs :op)
             (legacy-needs-execute? op) (assoc :apply true))}))

;; ============================================================
;; Dispatch
;; ============================================================

(defn- dispatch [args]
  (cond
    (or (empty? args) (= (first args) "-h") (= (first args) "--help"))
    (do (help-cmd nil) (System/exit 0))

    (legacy-keyword-style? args)
    (let [{:keys [cmd-name opts]} (parse-legacy args)
          c (commands cmd-name)]
      (when-let [anchor (or (:file opts) (:clj opts) (:cljs opts))]
        (forms/init-from-file! anchor))
      (print-result ((:run c) opts)))

    :else
    (let [[cmd-name & rest-args] args
          c (commands cmd-name)]
      (cond
        (nil? c)
        (do (binding [*out* *err*]
              (println "Unknown command:" cmd-name)
              (println "Run 'clj-surgeon -h' for available commands."))
            (System/exit 1))

        :else
        (let [opts (parse-cmd-args c rest-args)]
          (cond
            (:help opts)
            (print-cmd-help cmd-name)

            :else
            (do (when-let [anchor (or (:file opts) (:clj opts) (:cljs opts))]
                  (forms/init-from-file! anchor))
                (print-result ((:run c) opts)))))))))

(defn -main [& args]
  (dispatch args))
