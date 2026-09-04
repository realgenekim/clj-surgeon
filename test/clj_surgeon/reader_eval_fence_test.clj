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
   so `(System/getProperty \"java.class.path\")` inside it is BABASHKA's
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
;; The CONFIGURATION half of the same vector: the build file's :paths
;; ---------------------------------------------------------------------------

(defn- plant-escaping-paths-tree!
  "A directory holding one real source and a build file whose `:paths` names a
   tree OUTSIDE it. Returns the outside tree.

   This is the round-23 review's finding 3, and it is deliberately the SAME
   premise as the reader plant above with the payload changed from code to
   data: the caller's only power is to write a file in a directory, and the
   question is whether the op will follow what that file says."
  [^java.io.File root build-file-name entry]
  (let [src (io/file root "src")
        outside (io/file root "..", (str (.getName root) "-outside"))]
    (.mkdirs src)
    (spit (io/file src "a.clj") "(ns a)
")
    (.mkdirs outside)
    (spit (io/file outside "secret.clj")
          "(ns secret-outside)
(def token :leaked)
")
    (spit (io/file root build-file-name)
          (case build-file-name
            "project.clj" (str "(defproject x \"1\" :source-paths [" (pr-str entry) "])
")
            (str "{:paths [" (pr-str entry) "]}
")))
    outside))

;; @spec MCP-OP-SHELL-ARGV-006
(deftest no-real-launcher-follows-a-build-file-path-out-of-the-tree
  ;; The reviewer's plant, at both REAL launchers, in both spellings. Before
  ;; the fence: exit 0, and the op enumerated and printed a tree the caller
  ;; never named — namespace, requires, and every def name with its line
  ;; range. `secret-outside` standing in stdout IS the finding.
  ;; The matrix is deliberately asymmetric, and the reason is wall clock, not
  ;; coverage. One `:jvm` drive costs ~65 s on this box (a cold JVM plus the
  ;; full test classpath plus compiling `core`); one `:bb` drive costs ~0.3 s.
  ;; What varies per BUILD FILE is `source-paths-from-config`, whose
  ;; three-shape parity the reader witness above already drives at both
  ;; launchers; what varies per SPELLING is the fence itself, which is this
  ;; witness's subject. So both spellings run at BOTH launchers, and the
  ;; second and third build-file shapes run at the cheap one. A matrix that
  ;; is too slow to run is a witness that gets deleted.
  (doseq [[runtime build-files]
          [[:jvm ["deps.edn"]]
           [:bb ["deps.edn" "bb.edn" "project.clj"]]]
          build-file build-files
          [label entry] [[:relative "../%s-outside"]
                         [:absolute :absolute]]]
    (testing (str runtime " / " build-file " / " (name label))
      (let [root (.toFile (java.nio.file.Files/createTempDirectory
                            "escaping-paths-fence"
                            (make-array java.nio.file.attribute.FileAttribute 0)))
            spelled (if (= entry :absolute)
                      (.getPath (io/file (.getParentFile root)
                                         (str (.getName root) "-outside")))
                      (format entry (.getName root)))
            outside (plant-escaping-paths-tree! root build-file spelled)]
        (try
          (let [{:keys [out exit]}
                (run-launcher runtime [":op" ":ls-tree" ":dir" (.getPath root)])]
            (is (not (str/includes? out "secret-outside"))
                (str "the " (name runtime) " launcher FOLLOWED " build-file
                     "'s :paths out of the tree the caller named and printed "
                     "the namespace it found there — entry " (pr-str spelled)
                     ", exit " exit
                     ", stdout " (pr-str (subs out 0 (min 400 (count out))))))
            (is (not (str/includes? out "def token"))
                (str "the " (name runtime) " launcher printed a def name from "
                     "outside the caller's tree — exit " exit))
            ;; The refusal NAMES the entry as the caller spelled it: a
            ;; counted skip the caller cannot map back to a line of their own
            ;; build file is a number, not a refusal.
            (is (str/includes? out spelled)
                (str "the refusal did not name the entry the caller spelled ("
                     (pr-str spelled) ") — stdout "
                     (pr-str (subs out 0 (min 400 (count out))))))
            ;; And it names ONLY that. Where the two differ — the relative
            ;; spelling — the resolved TARGET must not appear: the target is a
            ;; fact about the box, and a refusal that publishes it hands over
            ;; the very path it just declined to read. Where the caller spelled
            ;; the entry absolutely the two are the same string, and echoing
            ;; the caller's own text back is the contract, not a leak.
            (when (= label :relative)
              (is (not (str/includes? out (.getCanonicalPath outside)))
                  (str "the refusal named the resolved TARGET tree rather than "
                       "the entry the caller spelled — stdout "
                       (pr-str (subs out 0 (min 400 (count out))))))))
          (finally
            (fs/delete-tree root)
            (fs/delete-tree outside)))))))

;; @spec MCP-OP-SHELL-ARGV-006
(deftest a-non-string-paths-entry-never-reaches-io-file
  ;; The round-23 review's §2 parity divergence, which is the tell for this
  ;; finding: bb read a 10,001-deep nested vector out of `:paths` fine and
  ;; then died one frame later inside `io/file`, because nothing validated
  ;; that the entries were strings. Same input, two launchers, two exits.
  (doseq [runtime [:jvm :bb]]
    (testing (name runtime)
      (let [root (.toFile (java.nio.file.Files/createTempDirectory
                            "nonstring-paths-fence"
                            (make-array java.nio.file.attribute.FileAttribute 0)))]
        (try
          (.mkdirs (io/file root "src"))
          (spit (io/file root "src" "a.clj") "(ns a)
")
          (spit (io/file root "deps.edn") "{:paths [[\"src\"] 42 :src]}
")
          (let [{:keys [out err exit]}
                (run-launcher runtime [":op" ":ls-tree" ":dir" (.getPath root)])]
            (is (not (str/includes? (str out err) "Coercions"))
                (str "a non-string :paths entry reached io/file and threw the "
                     "protocol error instead of being refused — exit " exit
                     ", stderr " (pr-str (subs err 0 (min 300 (count err))))))
            (is (not (str/includes? (str out err) "StackOverflow"))
                (str "a non-string :paths entry overflowed rather than being "
                     "refused — exit " exit)))
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
