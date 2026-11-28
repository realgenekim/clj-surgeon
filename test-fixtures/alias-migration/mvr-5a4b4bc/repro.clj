;; Reproduce the 5a4b4bc alias migration against all nine shipped goldens.
;; Checkout: astra/namespace-split, or archive/astra/namespace-split when the branch is gone.
;; bb test-fixtures/alias-migration/mvr-5a4b4bc/repro.clj <fixture-dir> <astra-checkout> [scratch-dir]
;; CLJ_SURGEON_ASTRA may replace the second argument (pass "" there to supply scratch).
;; Requirements: bb, clojure (JVM), git, cp, tar, patch.
;; Only scratch copies are mutated; seed, goldens and the checkout are unchanged.
(require '[babashka.fs :as fs]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as shell])
(import '(java.nio.file Files FileVisitOption LinkOption)
        '(java.security MessageDigest))

(def fixture (str (fs/canonicalize (or (not-empty (first *command-line-args*)) (fs/parent *file*)))))
(def astra (some-> (or (not-empty (second *command-line-args*)) (System/getenv "CLJ_SURGEON_ASTRA")) fs/canonicalize str))
(when-not (and astra (fs/exists? (fs/path astra ".git"))
            (fs/regular-file? (fs/path astra "src/clj_surgeon/mcp_alias_migration.clj")))
  (binding [*out* *err*]
    (println "Required: astra checkout (second argument or CLJ_SURGEON_ASTRA) holding src/clj_surgeon/mcp_alias_migration.clj for clj-surgeon.mcp-alias-migration; src/clj_surgeon/alias_migration.clj alone is insufficient:" astra))
  (System/exit 1))
(def scratch (str (fs/canonicalize (or (nth *command-line-args* 2 nil) (fs/create-temp-dir {:prefix "mvr-repro-"})))))
(when (some #(or (fs/starts-with? scratch %) (fs/starts-with? % scratch)) [fixture astra])
  (throw (ex-info "Scratch must neither contain nor be inside fixture or astra checkout" {:scratch scratch})))
(fs/create-dirs scratch)
;; Run the checkout on a JVM: its nrepl dependency uses classes unavailable in bb.
(when (System/getProperty "babashka.version")
  (let [config (edn/read-string (slurp (str astra "/deps.edn")))
        deps {:paths [(str astra "/src")]
              :deps (merge {'cheshire/cheshire {:mvn/version "5.13.0"}}
                           (:deps config) (select-keys (get-in config [:aliases :clj-surgeon/test-deps :extra-deps])
                                            '[babashka/fs babashka/process nrepl/nrepl]))}]
    (System/exit (:exit (process/shell {:continue true :dir scratch :extra-env {"CLJ_CACHE" (str scratch "/.cpcache")}}
                          "clojure" "-Srepro" "-Sdeps" (pr-str deps) "-M"
                          (str (fs/absolutize *file*)) fixture astra scratch)))))
(require '[clj-surgeon.mcp-alias-migration :as migration])
(def seed (str scratch "/seed"))
(def manifest (slurp (str fixture "/fixture-manifest.txt")))
(def digests (into {} (map (fn [[_ hash file]] [file hash])
                        (re-seq #"(?m)^([0-9a-f]{64})  (.+)$" manifest))))
(def callers (mapv #(str "src/marvin_voice_remote/" % ".clj")
               ["app_route" "bridge3_new" "channel" "codex_app_server"
                "director_control" "director_outbox" "reducer_session" "server" "tts"]))
(defn path [f] (.toPath (io/file f)))
(defn sha [bytes]
  (apply str (map #(format "%02x" (bit-and 255 %))
               (.digest (MessageDigest/getInstance "SHA-256") bytes))))
(defn snapshot [root]
  ;; No FOLLOW_LINKS: hash symlink text, never the target.
  (with-open [stream (Files/walk (path root) (make-array FileVisitOption 0))]
    (into (sorted-map)
          (keep (fn [p]
                  (let [relative (str (.relativize (path root) p))]
                    (cond
                      (Files/isSymbolicLink p)
                      [relative [:symlink (str (Files/readSymbolicLink p))]]
                      (Files/isRegularFile p (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                      [relative [:file (sha (Files/readAllBytes p))]]))))
          (iterator-seq (.iterator stream)))))
(defn command! [& args]
  (let [r (apply shell/sh args)]
    (when-not (zero? (:exit r)) (throw (ex-info "Command failed" (assoc r :args args))))))
(defn check! [label failures]
  (println (str label ":") (empty? failures))
  (doseq [file failures] (println "FAIL:" label file))
  (when (seq failures) (System/exit 1)))
(defn changed-paths [before after]
  (sort (filter #(not= (get before %) (get after %))
                (into (set (keys before)) (keys after)))))
(defn check-golden! [label tree]
  (check! label (remove #(= (get tree %) [:file (get digests (str "golden/" (fs/file-name %)))]) callers)))
(defn extract! [root]
  (assert (not (fs/exists? root {:nofollow-links true})) (str "Scratch already exists: " root))
  (fs/create-dirs root)
  (command! "tar" "-xzf" (str fixture "/seed.tar.gz") "-C" root))
;; Verify the shipped archive before extraction, then verify its members without following links.
(doseq [file ["seed.tar.gz" "golden.patch" "request.json"]]
  (assert (= (get digests file) (sha (Files/readAllBytes (path (str fixture "/" file))))) file))
(extract! seed)
(doseq [[file [kind value]] (snapshot seed)]
  (assert (if (= kind :file)
            (= value (get digests (str "seed/" file)))
            (.contains manifest (str "# symlink " (sha (.getBytes value "UTF-8"))
                                     "  seed/" file " -> " value "\n"))) file))
(let [golden (str scratch "/patched-seed")]
  (extract! golden)
  (command! "patch" "--batch" "-p0" "-d" golden "-i" (str fixture "/golden.patch"))
  (check-golden! "patch-reproduces-golden" (snapshot golden)))
(defn run-case [label remove-link?]
  (let [root (str scratch "/" label)
        _ (when (Files/exists (path root) (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
            (throw (ex-info "Copy already exists; preserve previous evidence" {:root root})))
        _ (command! "cp" "-a" "--" seed root)
        link (path (str root "/src/datastar_kit"))
        _ (when remove-link?
            (assert (Files/isSymbolicLink link))
            (Files/delete link))
        before (snapshot root)
        params (assoc (json/parse-string (slurp (str fixture "/request.json")) true) :workspace_root root)
        validated (migration/validate-request params)
        _ (assert (:ok validated) (pr-str validated))
        planned (migration/plan! root (:request validated))
        plan (:plan planned)
        attempted (volatile! false)
        committed (when (:ok planned)
                    (migration/commit!
                      {:project-root root :receipt-dir (str scratch "/receipts/" label)
                       :attempted attempted}
                      (str (:root planned))
                      (migration/plan->spec plan (:paths planned) (:destination planned))
                      (mapv #(get (:paths planned) (:file %)) (:files plan))))
        after (snapshot root)
        touched (vec (changed-paths before after))]
    (spit (str scratch "/" label "-plan.edn") (pr-str planned))
    (spit (str scratch "/" label "-apply.edn") (pr-str committed))
    (println "CASE" label)
    (println "symlink-present:" (Files/isSymbolicLink link))
    (println "planner-ok:" (:ok planned))
    (when-not (:ok planned) (println "planner-refusal:" (pr-str planned)))
    (println "planned-totals:" (pr-str (:totals plan)))
    (println "apply:" (pr-str (select-keys committed [:committed :error :error-type :source-unchanged :verification])))
    (println "files-touched:" (count touched) (pr-str touched))
    (check! "untouched-rest" (changed-paths (apply dissoc before callers) (apply dissoc after callers)))
    (check! "touched-set-equals-nine-callers"
            (concat (remove (set touched) callers) (remove (set callers) touched)))
    (check-golden! "all-nine-golden-byte-identical" after)
    {:plan plan :after (dissoc after "src/datastar_kit") :touched touched
     :committed? (true? (:committed committed))}))

(let [initial (snapshot seed)
      link (path (str seed "/src/datastar_kit"))]
  (println "checkout:" (:out (shell/sh "git" "-C" astra "rev-parse" "HEAD")))
  (println "scratch:" scratch)
  (println "entry-points: validate-request -> plan! -> plan->spec -> commit!")
  (println "seed-symlink:" (Files/isSymbolicLink link) "target:" (str (Files/readSymbolicLink link)))
  (let [with-link (run-case "with-symlink" false)
        without-link (run-case "without-symlink" true)]
    (println "CROSS-CASE")
    (println "plans-identical:" (= (:plan with-link) (:plan without-link)))
    (println "touched-files-identical:" (= (:touched with-link) (:touched without-link)))
    (println "result-trees-identical-except-removed-symlink:" (= (:after with-link) (:after without-link)))
    (println "seed-unchanged:" (= initial (snapshot seed)))
    (shutdown-agents)
    (System/exit (if (and (:committed? with-link) (:committed? without-link)
                          (= (:plan with-link) (:plan without-link))
                          (= (:touched with-link) (:touched without-link))
                          (= (:after with-link) (:after without-link))
                          (= initial (snapshot seed))) 0 1))))
