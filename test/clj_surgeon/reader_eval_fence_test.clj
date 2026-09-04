(ns ^{:lane :battery} clj-surgeon.reader-eval-fence-test
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
        ;; The sibling is built from the PARENT, not as `<root>/../<name>`.
        ;; The first draft used the `..` spelling and leaked eight directories
        ;; into the suite's temp root, which the trunk's tmp-leak ratchet
        ;; caught: the cleanup deletes `root` first, and `<root>/../<name>` is
        ;; then a path whose own prefix no longer exists, so the delete threw
        ;; and the sibling survived. A cleanup that depends on the order two
        ;; unrelated deletions happen in is not cleanup.
        outside (io/file (.getParentFile root) (str (.getName root) "-outside"))]
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
            ;; The sibling FIRST: it is the one a failed delete leaves behind,
            ;; and it must not depend on `root` still being there.
            (fs/delete-tree outside)
            (fs/delete-tree root)))))))

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

;; @spec MCP-OP-SHELL-ARGV-006
(deftest the-fence-does-not-refuse-an-ordinary-path-under-a-symlinked-root
  ;; The regression the first fence shipped, pinned here so it cannot come
  ;; back. `core_discovery_test/a-symlinked-root-is-descended-just-like-its-
  ;; target` caught it, and this is the same defect stated in the fence's own
  ;; terms: when `:dir` is a SYMLINK, a lexically-normalised `src` resolves
  ;; under the LINK while `canonical-root` resolves to the TARGET, so an
  ;; ordinary `{:paths ["src"]}` was measured against the wrong root and
  ;; refused. The whole scan came back `0 files` with one refused entry.
  ;;
  ;; It is the declared round-19 rule — the workspace is the RESOLVED tree, so
  ;; a link to a real tree IS that tree — broken by a fence that mixed two
  ;; frames of reference. A fence's false POSITIVES need a witness as much as
  ;; its false negatives: this one would have refused every symlinked
  ;; workspace in the fleet while looking exactly like a working control.
  (doseq [runtime [:jvm :bb]]
    (testing (name runtime)
      (let [parent (.toFile (java.nio.file.Files/createTempDirectory
                              "symlinked-root-fence"
                              (make-array java.nio.file.attribute.FileAttribute 0)))
            target (io/file parent "target")
            link (io/file parent "link")]
        (try
          (.mkdirs (io/file target "src"))
          (spit (io/file target "src" "core.clj") "(ns core)\n(defn f [])\n")
          (spit (io/file target "deps.edn") "{:paths [\"src\"]}\n")
          (java.nio.file.Files/createSymbolicLink
            (.toPath link) (.toPath target)
            (make-array java.nio.file.attribute.FileAttribute 0))
          (let [{:keys [out exit]}
                (run-launcher runtime [":op" ":ls-tree" ":dir" (.getPath link)])]
            (is (str/includes? out "core.clj")
                (str "an ordinary :paths entry was refused under a SYMLINKED "
                     "root — the fence measured the entry against the resolved "
                     "target while resolving the entry under the link. exit "
                     exit ", stdout " (pr-str (subs out 0 (min 400 (count out))))))
            (is (not (str/includes? out "source_paths_outside_project"))
                "nothing was outside the project root; the counter must be absent"))
          (finally (fs/delete-tree parent)))))))

;; @spec MCP-OP-SHELL-ARGV-007
(deftest a-deeply-nested-build-file-is-refused-typed-not-overflowed
  ;; The round-23 review's §2, the gap the builder disclosed. `refuse-over-nested!`
  ;; guards `parse-val`'s two collection branches, so argv is bounded — but the
  ;; build file the op DISCOVERS under the caller's :dir is read with no depth
  ;; bound at all. A 10,001-deep `:paths` overflows the reader's stack and
  ;; leaves through `-main`'s last-resort `catch Throwable`.
  ;;
  ;; The review ruled that non-blocking and it was right: nothing is evaluated,
  ;; no caller bytes are published, the exit is bounded and typed `1`. But
  ;; `:invalid-arguments` + "the launcher failed with java.lang.StackOverflowError
  ;; and no message" is the LAST-RESORT exit — the one whose whole docstring
  ;; says it exists because the depth bound is a guess that might be wrong.
  ;; Taking it on an input we can measure before reading is using the airbag as
  ;; a brake. The same ceiling that governs argv governs a build file's bytes.
  (doseq [runtime [:jvm :bb]]
    (testing (name runtime)
      (let [root (.toFile (java.nio.file.Files/createTempDirectory
                            "deep-build-file"
                            (make-array java.nio.file.attribute.FileAttribute 0)))]
        (try
          (.mkdirs (io/file root "src"))
          (spit (io/file root "src" "a.clj") "(ns a)\n")
          (spit (io/file root "deps.edn")
                (str "{:paths " (str/join (repeat 10001 "["))
                     (str/join (repeat 10001 "]")) "}\n"))
          (let [{:keys [out err exit]}
                (run-launcher runtime [":op" ":ls-tree" ":dir" (.getPath root)])
                text (str out err)]
            (is (not (str/includes? text "StackOverflowError"))
                (str "a build file the op discovered overflowed the reader "
                     "instead of being refused unread by the same ceiling that "
                     "governs argv — exit " exit ", output "
                     (pr-str (subs text 0 (min 400 (count text))))))
            (is (str/includes? text "build-file-nesting-too-deep")
                (str "the refusal is not the TYPED one; it took the last-resort "
                     "Throwable exit — exit " exit ", output "
                     (pr-str (subs text 0 (min 400 (count text))))))
            ;; Measured and named, so the caller learns which ceiling and by
            ;; how much — the argv refusal's own contract, applied here.
            (is (str/includes? text "deps.edn")
                "the refusal must name the build file it refused")
            (is (= 1 exit) "a refused build file is still exit 1"))
          (finally (fs/delete-tree root)))))))

;; ---------------------------------------------------------------------------
;; The class: no evaluating reader anywhere in src/
;; ---------------------------------------------------------------------------

(def evaluating-reader-names
  "The `clojure.core` fns that READ AND EVALUATE, by unqualified name.

   Round-23 review finding 4 — the oracle correction, taken on the rung the
   house rule names: *if an oracle existed and MISSED the bug, correct the
   oracle in the same fix.* This set held `read-string` and `load-string`
   only, which matched MCP-OP-SHELL-ARGV-005 as that requirement was written
   but left five siblings of the same defect invisible. There was no present
   violation either way — the allow-list is honestly empty on the wider set
   too — so this buys nothing today and everything tomorrow, which is the
   whole point of a ratchet.

   Why each one is here:

   - `read-string`  honours `*read-eval*` (true by default), so `#=(…)` runs.
   - `read`         the SAME reader over a `PushbackReader`, and it honours
                    `*read-eval*` identically. It was the one most likely to
                    be reached for as the \"fix\" for `read-string`.
   - `read+string`  `read`, returning the text beside the value; same reader,
                    same `*read-eval*`.
   - `load-string`  compiles its input outright.
   - `load-reader`  the same, from a Reader.
   - `load-file`    the same, from a path — and a PATH is the shape this
                    round's finding 3 is about, so a build file naming one is
                    exactly the vector to keep shut.
   - `load`         the same, from classpath resource names.
   - `eval`         not a reader, and included deliberately. Every entry above
                    is dangerous only because it ENDS in evaluation; a rule
                    that fences the readers and leaves the evaluator open
                    fences a spelling rather than a capability. The two SCI
                    evaluators in `src/` are `sci/eval-form` and
                    `sci/eval-string+` — namespace-qualified to `sci`, not to
                    `clojure.core` — so they are not hits here, which is
                    correct: they are declared features behind an explicit
                    allow-list interpreter, and their bounds are a separate
                    question from this one."
  #{"read-string" "read" "read+string"
    "load-string" "load-reader" "load-file" "load"
    "eval"})

(def collides-with-ordinary-locals
  "The subset of `evaluating-reader-names` that is ALSO a plausible local name,
   and is therefore matched in OPERATOR POSITION only.

   Found by running the widened set, not by reasoning about it. The corrected
   oracle immediately reported five violations in a repository the round-23
   review had swept and called clean — and every one was a false positive:

     src/clj_surgeon/core.clj:1301   (let [read (try {:source (slurp p)} …)]
     src/clj_surgeon/core.clj:1304     (if-let [error (:unreadable read)]
     src/clj_surgeon/core.clj:1309     (let [source (:source read)

   `read` there is a LOCAL holding the result of a slurp, and the same shape
   accounts for the other four files. That is the failure mode a widened
   ratchet actually has: not silence, but noise, and a gate that cries wolf
   five times on its first run is a gate somebody switches off. So the wide
   set is kept and the MATCH RULE is made precise instead of the set made
   narrow again.

   The rule, per tier:

   - `read-string`, `read+string`, `load-string`, `load-reader`, `load-file`
     are matched in ANY position, exactly as the round-23 oracle matched its
     two. Nothing in this repository binds a local by those names and a
     higher-order `(map read-string xs)` evaluates just as surely as a direct
     call, so the original strength is preserved unchanged.

   - `read`, `load` and `eval` are matched in operator position, or with a
     namespace (`clojure.core/read`), or as interop. Bare and in argument
     position they are overwhelmingly a local, as the five hits above show.

   What this gives up, stated rather than hidden: a bare higher-order
   reference to one of the three — `(map eval forms)` — is not a hit. It is
   the price of the three names being ordinary English, it is narrower than
   the gap round twenty-three shipped (which missed all six in every
   position), and it is closed the moment the reference is namespaced."
  #{"read" "load" "eval"})

(def evaluating-interop-names
  "The same capability reached by Java interop, matched on the WHOLE symbol.

   `evaluating-reader-names` is matched by unqualified name against an alias
   set for `clojure.core`, which cannot see these: `(namespace 'Compiler/load)`
   is `\"Compiler\"`, and `Compiler` is not an alias of `clojure.core`, so
   every one of these walked past that check. They are the same three
   capabilities one layer down — the reader and the compiler clojure.core
   itself calls — and a fence the caller can step around by writing
   `RT/readString` instead of `read-string` is a fence around a spelling.

   Matched on both the bare and the fully-qualified interop spellings, since
   an `:import` makes the short form legal."
  #{"Compiler/load" "Compiler/eval" "Compiler/loadFile"
    "clojure.lang.Compiler/load" "clojure.lang.Compiler/eval"
    "clojure.lang.Compiler/loadFile"
    "RT/readString" "clojure.lang.RT/readString"})

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

(defn- evaluating-reader-calls-in
  "Every evaluating-reader call in one SOURCE TEXT, as `[label symbol]`.

   Split out from the file-reading fn so the detector can be driven on
   synthetic sources. Round-23 review finding 4 was that the oracle's NAME SET
   was too narrow, and a witness for that cannot be written against `src/`:
   the repository is clean, so the correct set and the wrong one both produce
   an empty result there. The subject of that witness is the DETECTOR, and a
   detector that can only be pointed at a real tree cannot be tested until
   the tree is dirty."
  [label text]
  (let [root (p/parse-string-all text)
        forms (try (n/sexpr root) (catch Exception _ nil))
        aliases (core-aliases (when (seq? forms) forms))
        hits (atom [])]
    (letfn [(operator? [node parent]
              ;; The head of a list — the position where a call actually
              ;; evaluates. Whitespace and comment children are skipped so
              ;; `(\n  read r)` is still operator position.
              (and parent
                   (= :list (n/tag parent))
                   (identical? node
                               (first (remove #(or (n/whitespace? %)
                                                   (n/comment? %))
                                              (n/children parent))))))
            (walk [node parent]
              (when (= :token (n/tag node))
                (let [v (try (n/sexpr node) (catch Exception _ nil))]
                  (when (and (symbol? v)
                             (or (contains? evaluating-interop-names (str v))
                                 (and (contains? evaluating-reader-names (name v))
                                      (contains? aliases (namespace v))
                                      ;; The two-tier rule. A name that also
                                      ;; reads as an ordinary local counts only
                                      ;; where it is being CALLED, or where it
                                      ;; carries a namespace and so cannot be a
                                      ;; local at all.
                                      (or (not (contains? collides-with-ordinary-locals
                                                          (name v)))
                                          (some? (namespace v))
                                          (operator? node parent)))))
                    (swap! hits conj [label (str v)]))))
              (when (n/inner? node)
                (doseq [child (n/children node)] (walk child node))))]
      (walk root nil))
    @hits))

(defn- evaluating-reader-calls
  "Every call to an evaluating reader in one source, as `[line symbol]`.

   Parsed with rewrite-clj and walked over TOKEN nodes only, so a docstring
   or a comment naming the fn — three of which exist in `src/` and describe
   this very rule — is not a hit."
  [^java.io.File file]
  (evaluating-reader-calls-in (.getName file) (slurp file)))

;; @spec MCP-OP-SHELL-ARGV-005
(deftest the-oracle-names-every-evaluator-it-claims-to-fence
  ;; Round-23 review finding 4, as a witness rather than a promise. This
  ;; drives the DETECTOR, not the repository: `src/` is clean, so the narrow
  ;; set and the corrected one both return empty there and the ratchet's
  ;; own regression is invisible at its only call site. Shrink
  ;; `evaluating-reader-names` back to the round-23 pair and this goes red
  ;; naming the five it stopped seeing.
  (doseq [[label source expected]
          [["read"         "(ns x)\n(defn f [r] (read r))"                    "read"]
           ["read+string"  "(ns x)\n(defn f [r] (read+string r))"             "read+string"]
           ["read-string"  "(ns x)\n(defn f [s] (read-string s))"             "read-string"]
           ["load-string"  "(ns x)\n(defn f [s] (load-string s))"             "load-string"]
           ["load-reader"  "(ns x)\n(defn f [r] (load-reader r))"             "load-reader"]
           ["load-file"    "(ns x)\n(defn f [p] (load-file p))"               "load-file"]
           ["load"         "(ns x)\n(defn f [p] (load p))"                    "load"]
           ["eval"         "(ns x)\n(defn f [form] (eval form))"              "eval"]
           ["qualified"    "(ns x)\n(defn f [s] (clojure.core/read-string s))" "clojure.core/read-string"]
           ["aliased"      "(ns x (:require [clojure.core :as c]))\n(defn f [s] (c/read-string s))" "c/read-string"]
           ["Compiler/load"   "(ns x)\n(defn f [r] (Compiler/load r))"        "Compiler/load"]
           ["RT/readString"   "(ns x)\n(defn f [s] (RT/readString s))"        "RT/readString"]
           ["clojure.lang.RT" "(ns x)\n(defn f [s] (clojure.lang.RT/readString s))"
                                                            "clojure.lang.RT/readString"]]]
    (testing label
      (let [hits (set (map second (evaluating-reader-calls-in label source)))]
        (is (contains? hits expected)
            (str "the class ratchet does not see " (pr-str expected)
                 " — a source calling it would pass the gate. Saw: "
                 (pr-str (vec (sort hits))))))))
  (testing "a local named `read` is not a reader call"
    ;; The five false positives the widened set produced on its first run,
    ;; reduced to their shape. This is the assertion that keeps the two-tier
    ;; rule honest: delete the tier and this goes red.
    (let [local (str "(ns x)\n"
                     "(defn f [p]\n"
                     "  (let [read (try {:source (slurp p)} (catch Exception e {:err e}))]\n"
                     "    (if-let [e (:err read)] e (:source read))))")]
      (is (empty? (evaluating-reader-calls-in "local" local))
          (str "a local binding named `read` was reported as a reader call: "
               (pr-str (evaluating-reader-calls-in "local" local)))))
    ;; …but calling it IS a hit, so the tier narrows the position and never
    ;; the capability.
    (is (seq (evaluating-reader-calls-in "call" "(ns x)\n(defn f [r] (read r))"))
        "a real (read r) call must still be a hit"))
  (testing "and it still does not fire on prose that merely names them"
    ;; The other half of the correction. Widening a set is how a ratchet
    ;; starts crying wolf, and three docstrings in src/ describe this very
    ;; rule by name; a detector that reads them as violations gets switched
    ;; off, which is worse than the narrow set it replaced.
    (let [prose (str "(ns x\n  \"This docstring names read-string, load-file, "
                     "eval and RT/readString, and calls none of them.\")\n"
                     ";; nor does this comment: load-string, read+string\n"
                     "(defn f [s] (clojure.edn/read-string s))")]
      (is (empty? (evaluating-reader-calls-in "prose" prose))
          (str "the ratchet fired on prose or on clojure.edn/read-string: "
               (pr-str (evaluating-reader-calls-in "prose" prose)))))))

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
