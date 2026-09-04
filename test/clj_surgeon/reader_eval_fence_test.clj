(ns clj-surgeon.reader-eval-fence-test
  "THE FENCE between a reader and a caller-influenced byte.

   Opus's round-twenty-one BLOCKING finding. Round twenty closed the
   reader-eval class at `core/parse-val` — the argv-TEXT path — and the
   review's own sweep command found a second one, one frame over, in the
   entrance the round-twenty enumeration did not walk:

     src/clj_surgeon/core.clj:290  (read-string (slurp (str build-file)))

   `clojure.core/read-string`, which honours `*read-eval*`, over a
   `deps.edn` / `bb.edn` / `project.clj` DISCOVERED UNDER THE DIRECTORY THE
   CALLER NAMED. So the caller does not even need to control argv text;
   controlling a directory is enough. Reproduced at both real launchers under
   the ordinary `:op :ls-tree :dir` invocation, at tip 0a91e720:

     $ cat $FX/evil-tree/deps.edn
     {:paths #=(clojure.core/spit \"$FX/PWNED-LSTREE.txt\" \"…\")}
     $ java -cp \"$CP\" clojure.main -m clj-surgeon.core :op :ls-tree :dir $FX/evil-tree
     EXIT=0
     src/a.clj  1 lines, 0 forms
     $ cat $FX/PWNED-LSTREE.txt
     READER EVAL EXECUTED via :op :ls-tree :dir

   **Exit 0, a green receipt, nothing printed.** The `parse-val` case at
   least printed a refusal while it executed; this one reports success, which
   is strictly worse as a signal and is the same defect.

   Two witnesses, because the instance and the class are two different
   subjects:

   - `no-real-launcher-evaluates-a-build-file-it-discovers` drives the
     instance through BOTH real launchers and asserts the side effect never
     happened.
   This namespace rides the `mcp-test` lane and NOT `test-fast`, for a
   mechanical reason worth writing down: `test-fast` is `bb test/run_all.clj`,
   so `(System/getProperty "java.class.path")` inside it is BABASHKA's
   classpath and the `:jvm` launcher drive cannot be built from it. Registered
   in `run_all` the JVM half of this witness fails for a reason that has
   nothing to do with its subject, which is the worst kind of red. The census
   launcher witnesses live in the same lane for the same reason.

   - `no-source-in-this-repository-calls-the-evaluating-reader` is the CLASS
     ratchet: it PARSES every source under `src/` and fails on any call to
     `clojure.core/read-string` or `clojure.core/load-string`, bare or
     aliased, outside an enumerated allow-list. Parsed rather than grepped,
     because the phrase `clojure.core/read-string` appears in three docstrings
     that are describing this very rule, and a text scan cannot tell a rule
     from its violation."
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [rewrite-clj.parser :as p]
   [rewrite-clj.node :as n]))

(def ^:private repo-root (.getCanonicalPath (java.io.File. ".")))

;; ---------------------------------------------------------------------------
;; The instance: a hostile build file under a caller-named :dir
;; ---------------------------------------------------------------------------

(defn- plant-hostile-tree!
  "A directory holding one source and one build file whose reader payload
   WRITES A FILE. The payload is `spit`, not `println`, because the proof has
   to survive the subprocess: stdout can be swallowed, a file on disk cannot."
  [^java.io.File root build-file-name marker]
  (let [src (io/file root "src")]
    (.mkdirs src)
    (spit (io/file src "a.clj") "(ns a)\n")
    (spit (io/file root build-file-name)
          (str "{:paths #=(clojure.core/spit "
               (pr-str (str marker)) " " (pr-str "READER EVAL EXECUTED") ")}\n"))))

(defn- launcher-argv
  [runtime]
  (case runtime
    :bb ["bb" "-cp" (str repo-root "/src") "-m" "clj-surgeon.core"]
    :jvm ["java" "-cp" (System/getProperty "java.class.path")
          "clojure.main" "-m" "clj-surgeon.core"]))

(defn- run-launcher
  [runtime args]
  (let [{:keys [out err exit]}
        (apply proc/shell {:out :string :err :string :continue true}
               (concat (launcher-argv runtime) args))]
    {:out out :err err :exit exit}))

;; @spec MCP-OP-SHELL-ARGV-004
(deftest no-real-launcher-evaluates-a-build-file-it-discovers
  (doseq [runtime [:jvm :bb]
          build-file ["deps.edn" "bb.edn" "project.clj"]]
    (testing (str runtime " / " build-file)
      (let [root (.toFile (java.nio.file.Files/createTempDirectory
                            "reader-eval-fence"
                            (make-array java.nio.file.attribute.FileAttribute 0)))
            marker (io/file root "PWNED-LSTREE.txt")]
        (try
          (plant-hostile-tree! root build-file marker)
          (let [{:keys [out err exit]}
                (run-launcher runtime [":op" ":ls-tree" ":dir" (.getPath root)])]
            (is (not (.exists marker))
                (str "the " (name runtime) " launcher EVALUATED " build-file
                     " while listing the tree the caller named"
                     " — exit " exit
                     ", stdout " (pr-str (subs out 0 (min 200 (count out))))
                     ", stderr " (pr-str (subs err 0 (min 200 (count err))))))
            ;; And the refusal-free path still works: an unevaluated build file
            ;; is DATA the op reads, so the op still finds the source.
            (is (or (str/includes? out "a.clj") (str/includes? out "total"))
                "the op must still list the tree once the reader is inert"))
          (finally (fs/delete-tree root)))))))

;; ---------------------------------------------------------------------------
;; The class: no evaluating reader anywhere in src/
;; ---------------------------------------------------------------------------

(def evaluating-reader-names
  "The `clojure.core` fns that READ AND EVALUATE. `read-string` honours
   `*read-eval*` (true by default), so `#=(…)` in its input runs; `load-string`
   compiles its input outright."
  #{"read-string" "load-string"})

(def allowed-evaluating-reader-sites
  "The enumerated, JUSTIFIED exceptions. TARGET: EMPTY.

   An entry here is `[namespace-file symbol]` plus the reason it is safe, and
   the reason may never be \"the caller cannot name this file today\" — that
   is a statement about today's call graph, which is the exact argument
   round twenty made about `parse-val` and round twenty-one refuted one frame
   over."
  #{})

(defn- source-files
  []
  (->> (file-seq (io/file repo-root "src"))
       (filter #(.isFile ^java.io.File %))
       (filter #(re-find #"\.clj[cs]?$" (.getName ^java.io.File %)))
       sort))

(defn- core-aliases
  "Aliases in this file's `ns` form that point at `clojure.core`, so an
   aliased call is caught too. `nil` (an unqualified symbol) is always in the
   set: `read-string` with no namespace IS `clojure.core/read-string`."
  [forms]
  (let [aliases (atom #{nil "clojure.core"})]
    (doseq [form forms]
      (when (and (seq? form) (= 'ns (first form)))
        (doseq [clause (rest form)
                :when (and (seq? clause) (= :require (first clause)))
                spec (rest clause)
                :when (vector? spec)]
          (let [[lib & opts] spec
                as (second (drop-while #(not= :as %) opts))]
            (when (and (= 'clojure.core lib) (symbol? as))
              (swap! aliases conj (name as)))))))
    @aliases))

(defn- evaluating-reader-calls
  "Every call to an evaluating reader in one source, as `[line symbol]`.

   Parsed with rewrite-clj and walked over TOKEN nodes only, so a docstring
   or a comment naming the fn — three of which exist in `src/` and describe
   this very rule — is not a hit."
  [^java.io.File file]
  (let [text (slurp file)
        root (p/parse-string-all text)
        forms (try (n/sexpr root) (catch Exception _ nil))
        aliases (core-aliases (when (seq? forms) forms))
        hits (atom [])]
    (letfn [(walk [node]
              (when (= :token (n/tag node))
                (let [v (try (n/sexpr node) (catch Exception _ nil))]
                  (when (and (symbol? v)
                             (contains? evaluating-reader-names (name v))
                             (contains? aliases (namespace v)))
                    (swap! hits conj [(.getName file) (str v)]))))
              (when (n/inner? node)
                (doseq [child (n/children node)] (walk child))))]
      (walk root))
    @hits))

;; @spec MCP-OP-SHELL-ARGV-005
(deftest no-source-in-this-repository-calls-the-evaluating-reader
  (let [found (set (mapcat evaluating-reader-calls (source-files)))
        unjustified (set/difference found allowed-evaluating-reader-sites)]
    (is (empty? unjustified)
        (str "src/ calls an EVALUATING reader at " (count unjustified)
             " site(s), each one a place where caller-influenced bytes can "
             "become code: " (pr-str (vec (sort unjustified)))
             " — use clojure.edn/read-string, or enumerate the site in "
             "`allowed-evaluating-reader-sites` with the reason it is safe"))
    (testing "the allow-list itself is the thing being driven to zero"
      (is (empty? allowed-evaluating-reader-sites)
          (str "the allow-list is not empty: " (pr-str allowed-evaluating-reader-sites))))))
