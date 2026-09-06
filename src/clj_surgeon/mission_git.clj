(ns clj-surgeon.mission-git
  "Explicit-stage Git publication of a verified mission. No source writes,
   staging, hooks, signing, or push. The caller supplies saved ledger provenance."
  (:require
   [clojure.string :as str])
  (:import
   (java.nio.channels FileChannel)
   (java.nio.file Files LinkOption OpenOption StandardOpenOption)
   (java.security MessageDigest)))

(def max-bytes 1048576)
(defn refuse [type] {:ok false :error-type type :source-mutation-attempted false
                     :git-ref-updated false :index-staging false :hooks-run false})
(defn fail! [type] (throw (ex-info "Mission Git boundary refused" {:error-type type})))
(defn digest [bytes]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))
(defn hash? [s] (and (string? s) (boolean (re-matches #"[0-9a-f]{64}" s))))
(defn oid? [s] (and (string? s) (boolean (re-matches #"(?:[0-9a-f]{40}|[0-9a-f]{64})" s))))
(defn path? [s]
  (and (string? s) (<= 1 (count s) 512)
       (not (re-find #"[\x00-\x20\x7f:\\]" s))
       (not (str/starts-with? s "/"))
       (every? #(not (contains? #{"" "." ".." ".git"} %)) (str/split s #"/" -1))))
(defn branch? [s]
  (and (string? s) (boolean (re-matches #"refs/heads/[A-Za-z0-9][A-Za-z0-9._/-]{0,199}" s))
       (not (contains? #{"refs/heads/main" "refs/heads/MCP/main"} s))
       (not (str/includes? s ".."))
       (every? #(and (seq %) (not (str/starts-with? % "."))
                  (not (str/ends-with? % ".")) (not (str/ends-with? % ".lock")))
               (str/split s #"/" -1))))
(defn valid-provenance? [{:keys [id state workspace-root ledger-sha256 receipt-sha256 gate acceptance files]}]
  (and (string? id) (re-matches #"M-[0-9]{1,12}" id) (= state :verified)
       (string? workspace-root) (<= 1 (count workspace-root) 4096)
       (str/starts-with? workspace-root "/") (not (re-find #"[\x00-\x1f]" workspace-root))
       (hash? ledger-sha256) (hash? receipt-sha256)
       (true? (:ok gate)) (hash? (:sha256 gate))
       (true? (:ok acceptance)) (hash? (:sha256 acceptance))
       (not= (:sha256 gate) (:sha256 acceptance))
       (map? files) (<= 1 (count files) 64)
       (every? (fn [[p v]] (and (path? p) (hash? (:before-sha256 v))
                             (hash? (:after-sha256 v))
                             (not= (:before-sha256 v) (:after-sha256 v)))) files)))

(defn message [p]
  (str "Record verified mission " (:id p) "\n\n"
       "Mission: " (:id p) "\n"
       "Ledger-SHA256: " (:ledger-sha256 p) "\n"
       "Receipt-SHA256: " (:receipt-sha256 p) "\n"
       "Gate-SHA256: " (get-in p [:gate :sha256]) "\n"
       "Acceptance-SHA256: " (get-in p [:acceptance :sha256]) "\n"
       "Hooks: skipped (commit-tree)\nSigning: not requested\n"
       (apply str (for [[file hashes] (sort-by key (:files p))]
                    (str "Verified-File: " file " " (:after-sha256 hashes) "\n")))))

(defn plan [p o]
  (cond
    (not (valid-provenance? p)) (refuse :git-invalid-provenance)
    (not= (:workspace-root p) (:workspace-root o)) (refuse :git-wrong-root)
    (not (and (branch? (:branch o)) (oid? (:head o)) (oid? (:tree o)))) (refuse :git-unsupported-head)
    (not (and (vector? (:staged-paths o))
              (= (count (:staged-paths o)) (count (:files p)))
              (= (set (:staged-paths o)) (set (keys (:files p)))))) (refuse :git-staged-scope)
    (not (every? (fn [[path hashes]]
                   (let [f (get-in o [:files path])]
                     (and (= (:before-sha256 hashes) (:head-sha256 f))
                          (= (:after-sha256 hashes) (:index-sha256 f) (:live-sha256 f))
                          (contains? #{"100644" "100755"} (:head-mode f))
                          (= (:head-mode f) (:index-mode f) (:live-mode f))))) (:files p)))
    (refuse :git-stale-or-unsupported-files)
    :else {:ok true :commit-argv ["commit-tree" (:tree o) "-p" (:head o) "-F" "-"]
           :message (message p) :source-mutation-attempted false
           :hooks-run false :index-staging false}))

(defn execute!
  "Observer also revalidates saved ledger bytes. Runner receives argv and stdin,
   returns UTF-8 stdout, and throws on errors. Never runs shell command strings."
  [p observe run]
  (try
    (let [initial (observe) planned (plan p initial)]
      (if-not (:ok planned) planned
        (do
          (when-not (= initial (observe)) (fail! :git-observation-drift))
          (let [new-oid (str/trim (run (:commit-argv planned) (:message planned)))]
            (when-not (oid? new-oid) (fail! :git-invalid-commit-object))
            (when-not (= initial (observe)) (fail! :git-observation-drift))
            (try (run ["update-ref" (:branch initial) new-oid (:head initial)] nil) (catch Exception _ (throw (ex-info "Ref update outcome requires inspection" {:error-type :git-ref-update-uncertain :possible-commit new-oid}))))
            {:ok true :mission-id (:id p) :commit new-oid :parent (:head initial)
             :git-ref-updated true :source-mutation-attempted false :index-staging false
             :hooks-run false :signing-requested false :receipt-sha256 (:receipt-sha256 p)
             :concurrency "Repository lock; external Git writers may not honor it."}))))
    (catch Exception e (cond-> (refuse (or (:error-type (ex-data e)) :git-boundary-failed)) (= :git-ref-update-uncertain (:error-type (ex-data e))) (assoc :git-ref-updated :unknown :possible-commit (:possible-commit (ex-data e)))))))

(defn run-git!
  "Bounded argv subprocess; deadline includes input delivery."
  [root argv input]
  (let [cmd (into ["git" "--no-optional-locks" "-c" "core.fsmonitor=false"
                   "-c" "core.hooksPath=/dev/null" "-c" "commit.gpgSign=false"] argv)]
    ((requiring-resolve 'clj-surgeon.mission-git-process/run-process!) root cmd input 10000)))

(defn bounded-bytes [path]
  (when-not (and (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
              (<= (Files/size path) max-bytes)) (fail! :git-unsupported-file))
  (with-open [s (Files/newInputStream path (into-array OpenOption [LinkOption/NOFOLLOW_LINKS]))]
    (let [b (.readNBytes s (inc max-bytes))]
      (when (> (alength b) max-bytes) (fail! :git-file-limit)) b)))

(defn live-file [root relative]
  (let [base (.toPath (java.io.File. root))]
    (loop [path base parts (str/split relative #"/")]
      (if-let [part (first parts)]
        (let [next (.resolve path ^String part)]
          (when (Files/isSymbolicLink next) (fail! :git-symlink))
          (recur next (rest parts)))
        {:live-sha256 (digest (bounded-bytes path))
         :live-mode (if (Files/isExecutable path) "100755" "100644")}))))

(defn observe! [p run]
  (let [root (:workspace-root p)
        actual (str/trim (run ["rev-parse" "--show-toplevel"] nil))
        head (str/trim (run ["rev-parse" "--verify" "HEAD"] nil))
        branch (str/trim (run ["symbolic-ref" "-q" "HEAD"] nil))]
    (when-not (= root actual (.getCanonicalPath (java.io.File. root))) (fail! :git-wrong-root))
    (when-not (and (oid? head) (branch? branch)) (fail! :git-unsupported-head))
    {:workspace-root root :head head :branch branch
     :tree (str/trim (run ["write-tree"] nil))
     :staged-paths (vec (remove empty? (str/split (run ["diff" "--cached" "--no-ext-diff" "--name-only" "-z" "--"] nil) #"\u0000")))
     :files (into {} (for [path (keys (:files p))]
                       (let [tree-entry (run ["--literal-pathspecs" "ls-tree" "-z" head "--" path] nil)
                             index-entry (run ["--literal-pathspecs" "ls-files" "--stage" "-z" "--" path] nil)
                             hm (re-matches #"(100644|100755) blob [0-9a-f]+\t[^\u0000]+\u0000" tree-entry)
                             im (re-matches #"(100644|100755) [0-9a-f]+ 0\t[^\u0000]+\u0000" index-entry)]
                         (when-not (and hm im) (fail! :git-unsupported-file))
                         [path (merge (live-file root path)
                                      {:head-mode (second hm) :index-mode (second im)
                                       :head-sha256 (digest (.getBytes ^String (run ["show" (str head ":" path)] nil) "UTF-8"))
                                       :index-sha256 (digest (.getBytes ^String (run ["show" (str ":" path)] nil) "UTF-8"))})])))}))

(defn commit!
  "Commit exact staged mission bytes. ledger-current? must reread/hash the saved
   ledger under this lock. It is required on every observation, never inferred."
  [p ledger-current?]
  (try
    (when-not (and (valid-provenance? p) (ifn? ledger-current?)) (fail! :git-invalid-provenance))
    (let [root (:workspace-root p) run (partial run-git! root)
          _ (try (doseq [identity ["GIT_AUTHOR_IDENT" "GIT_COMMITTER_IDENT"]]
                   (when (str/blank? (run ["var" identity] nil)) (fail! :git-identity-unavailable)))
                 (catch Exception _ (fail! :git-identity-unavailable)))
          git-dir (str/trim (run ["rev-parse" "--absolute-git-dir"] nil))
          lock-path (.toPath (java.io.File. git-dir "mission-commit.lock"))]
      (with-open [channel (FileChannel/open lock-path
                            (into-array OpenOption [StandardOpenOption/CREATE StandardOpenOption/WRITE LinkOption/NOFOLLOW_LINKS]))]
        (if-let [_lock (.tryLock channel)]
          (try (execute! p #(do (when-not (true? (ledger-current?)) (fail! :git-stale-ledger))
                                (observe! p run)) run)
               (finally (.close channel)))
          (refuse :git-lock-busy))))
    (catch Exception e
      (cond-> (refuse (or (:error-type (ex-data e)) :git-boundary-failed))
        (= :git-identity-unavailable (:error-type (ex-data e)))
        (assoc :decision "Configure repository-local user.name and user.email explicitly, then rerun mission commit. No identity was configured by this command.")))))
