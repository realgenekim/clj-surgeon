#!/usr/bin/env bb
;; fan_check.clj — checks 1, 2, 3 and 6 of the FAN acceptance (sl1 "Acceptance").
;; Called by rescore-FAN.sh; prints one CHECK line per check with COMPUTED numbers and
;; exits non-zero if any failed.  It never prints a verdict word over a missing number:
;; a manifest, canonical tree or worktree it cannot read is a FAIL naming the reason.
;;
;;   bb fan_check.clj <worktree> <manifest.edn> <canonical-dir> <base-sha>
(ns fan-check
  (:require [clojure.string :as str]
            [clojure.set :as set]
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

;; Every consumed listing in this file is audited fail-closed (Sol round-2 review,
;; finding 4): `git diff` (name+exit checked below, now stderr too), `git ls-files`
;; (name+exit, now stderr too), the manifest read (below, a named ERROR instead of
;; a raw stack trace), and rescore-FAN.sh's own base-sha resolution
;; (rescore-FAN.sh:38-42, already `[ -n "$BASE" ]`-guarded -- unchanged here).

;; ---- the git binary this scorer trusts, and the base inventory's trust root -------
;; Sol round-3 review, finding 2 (BLOCKER): round 2's fix cross-checked git's UNTRACKED
;; listing against an independent filesystem walk -- but the other side of that
;; cross-check, "what src/ held at the base commit", was itself fetched with
;; `git ls-tree` through the same PATH-resolved binary.  One shim that returns an empty
;; `ls-files` AND appends the planted extra to `ls-tree` made both sides agree on a lie
;; and the full gate reported 6/6 with a real untracked file on disk.
;;
;; THE INDEPENDENCE ARGUMENT, in two parts.  Neither part is "we asked git nicely".
;;
;; (1) `git` is NEVER resolved through PATH.  It is $FAN_GIT if that names an
;;     executable file, else the first of a fixed list of ABSOLUTE candidates.  A shim
;;     dropped earlier in PATH is therefore never executed at all.  The resolved path,
;;     its --exec-path and its --version are PRINTED on every run, and -- for the
;;     candidate path -- the reported exec-path must be a real directory under the
;;     binary's OWN install prefix, which an `exec /usr/bin/git "$@"` proxy parked
;;     anywhere else fails.  This is the defence for an ordinary run.
;;
;; (2) The base inventory is not taken from ANY listing subcommand.  It is rebuilt from
;;     the object store BY CONTENT ADDRESS, and every byte on the way is verified in
;;     this process:
;;
;;       base sha (an INPUT -- from the caller/fixtures, never from a listing)
;;         -> `cat-file commit <base>`   : sha1("commit " len "\0" bytes) MUST equal base
;;         -> the tree line inside those verified bytes
;;         -> `cat-file tree <id>`       : sha1("tree " len "\0" bytes) MUST equal <id>
;;         -> recursively, every subtree under src/, each verified the same way
;;         -> the set of non-tree paths, which IS the baseline
;;
;;     `ls-tree`'s answer is then only a CLAIM, cross-checked against that set; any
;;     disagreement is a typed refusal.  This is independent of the binary in the only
;;     sense that matters: a shim may return whatever bytes it likes, but to be believed
;;     it must return bytes whose SHA-1 is the id it was asked for.  Forging the
;;     inventory therefore costs a SHA-1 preimage, not a shell script -- which is why
;;     `cat-file --batch` through the same binary IS an acceptable channel here, while
;;     `ls-tree` through the same binary is not: one is content-addressed and the other
;;     is the binary's unverifiable word.
;;
;;     The residual freedom a fully hostile binary keeps is WHICH commit it hands back,
;;     not what that commit contains -- and when the caller passes a full 40-hex base
;;     (rescore-FAN.sh always does) even that is closed, because rev-parse must resolve
;;     it to itself.
;;
;; FAN_GIT is a seam, not a waiver: it changes WHICH binary runs, never WHETHER any
;; check runs, and the self-tests use it to make the ADVERSARY's binary the trusted one
;; -- strictly harder than the PATH attack.  Its use is printed as a WARNING.

(def ^:private git-candidates
  ["/usr/bin/git" "/bin/git" "/usr/local/bin/git" "/opt/homebrew/bin/git"])

(defn- die! [fmt & args]
  (println (apply format fmt args))
  (System/exit 1))

(defn- resolve-git []
  (let [override (System/getenv "FAN_GIT")
        override? (boolean (seq override))
        cands (if override? [override] git-candidates)
        found (first (filter (fn [p] (let [f (io/file p)] (and (.isFile f) (.canExecute f))))
                             cands))]
    (when-not found
      (die! "CHECK 1 file-set: ERROR base-inventory git-binary none of %s is an executable file"
            (pr-str (vec cands))))
    (let [ep (sh found "--exec-path")
          ver (sh found "--version")]
      (when-not (and (zero? (:exit ep)) (seq (str/trim (:out ep))))
        (die! "CHECK 1 file-set: ERROR base-inventory git-binary %s --exec-path exit=%d %s"
              found (:exit ep) (str/trim (:err ep))))
      (let [exec-path (str/trim (:out ep))
            prefix (str/replace found #"/(bin|sbin|libexec)/git$" "")]
        (println (format "fan_check: git=%s exec-path=%s version=%s resolution=%s"
                          found exec-path (str/trim (:out ver))
                          (if override? "FAN_GIT-override" "absolute-candidate")))
        (if override?
          (println "fan_check: WARNING FAN_GIT override in use -- the install-prefix check is skipped; the content-addressed base inventory below is what defends this run")
          (do
            (when-not (.isDirectory (io/file exec-path))
              (die! "CHECK 1 file-set: ERROR base-inventory git-binary %s reports exec-path %s, which is not a directory"
                    found exec-path))
            (when-not (str/starts-with? exec-path (str prefix "/"))
              (die! "CHECK 1 file-set: ERROR base-inventory git-binary %s reports exec-path %s outside its own install prefix %s"
                    found exec-path prefix))))
        found))))

(def ^:private git (delay (resolve-git)))
(defn- g       [wt & args] (apply sh (concat [@git "-C" wt] args)))
(defn- g-bytes [wt & args] (apply sh (concat [@git "-C" wt] args [:out-enc :bytes])))

(defn- sha1-hex ^String [^bytes bs]
  (let [md (MessageDigest/getInstance "SHA-1")]
    (apply str (map #(format "%02x" %) (.digest md bs)))))

(defn- cat-bytes ^bytes [^bytes a ^bytes b]
  (let [out (byte-array (+ (alength a) (alength b)))]
    (System/arraycopy a 0 out 0 (alength a))
    (System/arraycopy b 0 out (alength a) (alength b))
    out))

(defn- verified-object
  "Raw CONTENT bytes of git object `id`, verified by content address:
   sha1(\"<type> <len>\\0\" + content) must equal `id`.  A binary that forges these
   bytes has to exhibit a SHA-1 preimage; nothing weaker is accepted."
  ^bytes [wt type id]
  (let [r (g-bytes wt "cat-file" type id)
        e (str/trim (str (:err r)))]
    (when-not (zero? (:exit r))
      (die! "CHECK 1 file-set: ERROR base-inventory object %s cat-file %s exit=%d %s" id type (:exit r) e))
    (when (seq e)
      (die! "CHECK 1 file-set: ERROR base-inventory object %s cat-file %s stderr: %s" id type e))
    (let [^bytes content (:out r)
          header (.getBytes (str type " " (alength content) (char 0)) "ISO-8859-1")
          got (sha1-hex (cat-bytes header content))]
      (when-not (= got id)
        (die! "CHECK 1 file-set: ERROR base-inventory object %s: the %d bytes returned for it hash to sha1=%s -- the store did not return the object it was asked for"
              id (alength content) got))
      content)))

(defn- parse-tree
  "Entries of a raw git tree object: [{:mode \"100644\" :name \"x\" :id \"<40 hex>\"} ...].
   Wire format is `<octal mode> <name>\\0<20 raw sha bytes>`, repeated; names are raw
   bytes, decoded here as UTF-8 to match what `sh` decodes `ls-tree -z` output as."
  [^bytes bs]
  (loop [i 0 acc []]
    (if (>= i (alength bs))
      acc
      (let [sp (loop [j i] (if (= 32 (aget bs j)) j (recur (inc j))))
            mode (String. bs i (- sp i) "US-ASCII")
            nul (loop [j (inc sp)] (if (zero? (aget bs j)) j (recur (inc j))))
            nm (String. bs (inc sp) (- nul (inc sp)) "UTF-8")
            id (apply str (map #(format "%02x" (aget bs %)) (range (inc nul) (+ (inc nul) 20))))]
        (recur (+ (inc nul) 20) (conj acc {:mode mode :name nm :id id}))))))

(defn- object-store-src-inventory
  "Every non-tree path under src/ at `base`, rebuilt from the OBJECT STORE and verified
   at every step by content address (see the independence argument above).  Never calls
   a listing subcommand."
  [wt base]
  (let [rp (g wt "rev-parse" "--verify" (str base "^{commit}"))
        _ (when-not (zero? (:exit rp))
            (die! "CHECK 1 file-set: ERROR base-inventory rev-parse %s exit=%d %s"
                  base (:exit rp) (str/trim (:err rp))))
        full (str/trim (:out rp))
        _ (when-not (re-matches #"[0-9a-f]{40}" full)
            (die! "CHECK 1 file-set: ERROR base-inventory rev-parse %s did not resolve to a 40-hex commit: %s"
                  base (pr-str full)))
        _ (when (and (re-matches #"[0-9a-f]{40}" base) (not= full base))
            (die! "CHECK 1 file-set: ERROR base-inventory rev-parse resolved the 40-hex base %s to a different commit %s"
                  base full))
        commit (verified-object wt "commit" full)
        tline (first (str/split-lines (String. ^bytes commit "UTF-8")))
        tm (re-matches #"tree ([0-9a-f]{40})" (str tline))
        _ (when-not tm
            (die! "CHECK 1 file-set: ERROR base-inventory commit %s does not begin with a tree line: %s"
                  full (pr-str tline)))
        root (second tm)
        src (first (filter #(= "src" (:name %)) (parse-tree (verified-object wt "tree" root))))
        _ (when-not src
            (die! "CHECK 1 file-set: ERROR base-inventory commit %s has no src/ entry in its root tree %s" full root))
        _ (when-not (= "40000" (:mode src))
            (die! "CHECK 1 file-set: ERROR base-inventory src is not a tree at %s (mode=%s)" full (:mode src)))]
    (letfn [(walk [prefix id]
              (mapcat (fn [{:keys [mode name id]}]
                        (let [p (str prefix "/" name)]
                          (if (= "40000" mode) (walk p id) [p])))
                      (parse-tree (verified-object wt "tree" id))))]
      {:files (into #{} (walk "src" (:id src))) :commit full :tree root})))

(defn walk-src
  "An EXHAUSTIVE inventory of every directory ENTRY under `<wt>/src`, by TYPE, paths
   relative to `wt` (e.g. \"src/acid/fanout/ns_003.clj\").  Never `file-seq`, which
   silently drops an unreadable directory's contents with no signal at all (Sol
   round-2 review, finding 1/4), and -- since Sol round-3 review, finding 3 (BLOCKER)
   -- never `.isFile`/`.isDirectory` either, because BOTH of those FOLLOW SYMLINKS.

   Git does not follow links: it inventories a symlink as ONE leaf path (mode 120000)
   and never looks through it.  The round-3 walk recorded only `.isFile` paths and
   recursed on `.isDirectory`, ignoring everything else, so a symlink to an EMPTY
   directory, a dangling symlink and a FIFO all vanished from the walk entirely --
   and with a successful-but-empty `ls-files` the complete gate reported 6/6.  A
   symlink to a NONEMPTY directory was worse than missing: the walk reported paths
   THROUGH the link, which git can never list.

   So: every name a successful directory listing returns becomes exactly ONE
   classified entry or ONE NAMED ERROR -- never a silent skip.

     regular file  -> :files, and an entry
     directory     -> recursed into (and counted)
     symlink       -> :symlinks, and an entry, exactly as git lists it; the TARGET's
                      type is recorded for the reader (\"file\"/\"dir\"/\"other\"/
                      \"dangling\") and the link is NEVER followed
     anything else -> :errors :unclassifiable-entry (FIFO, socket, device)
     unstattable   -> :errors :unclassifiable-entry naming the exception
     unenterable   -> :errors :unenterable-dir, naming readable/executable/perms -- a
                      directory whose read AND search permission are not both proven,
                      or whose open throws, at ANY mode and any depth (Sol round-3
                      review, finding 4: mode 0400 lists NAMES but stats nothing, so
                      counting directories found against directories entered proved
                      nothing at all)

   Returns {:files #{} :symlinks {path target-kind} :entries #{} :dirs-found N
            :dirs-entered M :entries-seen K :errors [{:kind :path :detail}]}.
   `:entries` is files + symlink leaves -- the set CHECK 1 reconciles against git's
   own view, which lists a link and not its target."
  [wt]
  (let [wt-path (.toPath (io/file wt))
        dirs-found (atom 0) dirs-entered (atom 0) entries-seen (atom 0)
        files (atom []) symlinks (atom []) errors (atom [])
        ;; NOT a `\`->`/` normalization: this repo is POSIX-only (no sudo, no
        ;; Windows lane), and `\` is a legal POSIX filename byte a manifest path
        ;; can legitimately contain (inb-9c18e2, --selftest-backslash) -- rewriting
        ;; it would corrupt exactly the path this program already had to fix once.
        ;; `.relativize(...).toString()` already yields `/`-separated components
        ;; verbatim on this JVM's (POSIX) file separator.
        rel (fn [^java.nio.file.Path p] (.toString (.relativize wt-path p)))
        err! (fn [kind ^java.nio.file.Path p detail]
               (swap! errors conj {:kind kind :path (rel p) :detail detail}))
        no-follow (into-array java.nio.file.LinkOption [java.nio.file.LinkOption/NOFOLLOW_LINKS])
        follow (into-array java.nio.file.LinkOption [])
        attrs (fn [^java.nio.file.Path p opts]
                (java.nio.file.Files/readAttributes
                  p java.nio.file.attribute.BasicFileAttributes ^"[Ljava.nio.file.LinkOption;" opts))
        link-target-kind (fn [^java.nio.file.Path p]
                           ;; the ONLY place the link is resolved, and only to LABEL it
                           (try (let [a (attrs p follow)]
                                  (cond (.isDirectory a) "dir"
                                        (.isRegularFile a) "file"
                                        :else "other"))
                                (catch Exception _ "dangling")))]
    (letfn [(walk [^java.nio.file.Path d]
              (swap! dirs-found inc)
              ;; ENTERABILITY IS TESTED, NOT INFERRED (Sol round-3 review, finding 4).
              ;; The round-3 walk inferred pruning from `.listFiles` returning null.
              ;; At mode 0400 it returns the NAMES -- opendir needs r -- while every
              ;; child stat fails, because that needs x; both counters therefore stayed
              ;; equal and every child was dropped in silence.  So: read AND search
              ;; permission are asserted explicitly, the open is then ATTEMPTED and its
              ;; exception caught, and a directory that cannot be entered is one typed
              ;; error at ANY mode -- 0400, 0200, 0000 alike, at any depth.
              (let [readable (java.nio.file.Files/isReadable d)
                    executable (java.nio.file.Files/isExecutable d)
                    perms (try (java.nio.file.attribute.PosixFilePermissions/toString
                                 (java.nio.file.Files/getPosixFilePermissions d no-follow))
                               (catch Exception ex (str "unreadable-mode(" (.getSimpleName (class ex)) ")")))
                    names (if-not (and readable executable)
                            (do (err! :unenterable-dir d
                                      (format "readable=%s executable=%s perms=%s -- a directory that cannot be entered is an error at any mode"
                                              readable executable perms))
                                ::failed)
                            (try
                              (with-open [s (java.nio.file.Files/newDirectoryStream d)]
                                ;; a DirectoryStream is Iterable; `into` must FORCE it
                                ;; before `with-open` closes the stream underneath it.
                                (into [] (iterator-seq (.iterator ^Iterable s))))
                              (catch Exception e
                                (err! :unenterable-dir d
                                      (format "perms=%s open failed: %s: %s" perms
                                              (.getSimpleName (class e)) (.getMessage e)))
                                ::failed)))]
                (when-not (= ::failed names)
                  (swap! dirs-entered inc)
                  (doseq [^java.nio.file.Path e names]
                    (swap! entries-seen inc)
                    (let [a (try (attrs e no-follow)
                                 (catch Exception ex
                                   (err! :unclassifiable-entry e
                                         (str (.getSimpleName (class ex)) ": " (.getMessage ex)))
                                   nil))]
                      (when a
                        (cond
                          ;; symlink FIRST: it is a leaf to git and to us, whatever it points at
                          (.isSymbolicLink a) (swap! symlinks conj [(rel e) (link-target-kind e)])
                          (.isDirectory a) (walk e)
                          (.isRegularFile a) (swap! files conj (rel e))
                          :else (err! :unclassifiable-entry e
                                      "not a regular file, a directory or a symlink (isOther)"))))))))]
      (walk (.toPath (io/file wt "src"))))
    (let [fs (into #{} @files) sl (into {} @symlinks)]
      {:files fs :symlinks sl
       :entries (into fs (keys sl))
       :dirs-found @dirs-found :dirs-entered @dirs-entered :entries-seen @entries-seen
       :errors @errors})))

(defn probe-walk
  "`fan_check.clj --probe-walk <worktree>` -- print the walk's inventory, one greppable
   line per entry, so a self-test can assert on the CLASSIFICATION and not merely on a
   downstream verdict.  A probe: it reports, it does not judge."
  [wt]
  (let [w (walk-src wt)]
    (println (format "WALK-PROBE dirs-found=%d dirs-entered=%d entries-seen=%d files=%d symlinks=%d errors=%d"
                      (:dirs-found w) (:dirs-entered w) (:entries-seen w)
                      (count (:files w)) (count (:symlinks w)) (count (:errors w))))
    (doseq [f (sort (:files w))] (println "WALK-PROBE file" f))
    (doseq [[p k] (sort (:symlinks w))] (println (format "WALK-PROBE symlink %s -> %s" p k)))
    (doseq [e (:errors w)] (println (format "WALK-PROBE error %s %s %s" (:kind e) (:path e) (:detail e))))
    (System/exit 0)))

(defn -main [wt manifest-path canon base]
  (let [m (try (read-string (slurp manifest-path))
               (catch Exception e
                 (println (format "CHECK 1 file-set: ERROR manifest unreadable: %s" (.getMessage e)))
                 (System/exit 1)))
        targets (:targets m)
        target-files (set (map :file targets))
        ;; NUL framing (-z) only ever produces EMPTY separators between records --
        ;; never a separator that is nonempty whitespace -- so the right predicate
        ;; is `empty?`. `str/blank?` is also true for a legal POSIX path that is
        ;; itself all whitespace (e.g. a file literally named " "), which would
        ;; silently drop a real record instead of just the framing artifacts.
        split-nul (fn [s] (remove empty? (str/split s (re-pattern (str (char 0))))))

        ;; -z / NUL-separated, raw bytes: `--name-only` (no -z) C-quotes any path
        ;; containing a backslash regardless of core.quotePath, so a legal POSIX
        ;; path with a literal "\" component never string-matches the manifest's
        ;; raw spelling (inb-9c18e2). -z disables that quoting entirely.
        gd (g wt "diff" "-z" "--name-only" base)
        _ (when-not (zero? (:exit gd))
            (println "CHECK 1 file-set: FAIL git diff failed:" (str/trim (:err gd)))
            (System/exit 1))
        ;; A listing can exit 0 and STILL be incomplete: stock Git prints an
        ;; unreadable-directory WARNING on stderr while returning 0 and an empty
        ;; stdout for that subtree (Sol round-2 review, finding 1, BLOCKER -- the
        ;; real `chmod 000` case). Reject nonempty stderr on every listing this
        ;; check trusts, whether or not its exit code was 0.
        _ (when (seq (str/trim (:err gd)))
            (println (format "CHECK 1 file-set: ERROR listing-incomplete git diff stderr: %s"
                              (str/trim (:err gd))))
            (System/exit 1))
        ;; Every git listing this check consumes is fail-closed: a listing process
        ;; that cannot be trusted must never read as an empty set (a missing
        ;; untracked-extra listing would let a real extra file disappear and the
        ;; gate false-PASS). Check `ls-files`'s own exit exactly as `diff`'s is
        ;; checked above, before any of its output is parsed.
        untracked (g wt "ls-files" "-z" "--others" "--exclude-standard")
        _ (when-not (zero? (:exit untracked))
            (println (format "CHECK 1 file-set: ERROR git ls-files exit=%d %s"
                              (:exit untracked) (str/trim (:err untracked))))
            (System/exit 1))
        _ (when (seq (str/trim (:err untracked)))
            (println (format "CHECK 1 file-set: ERROR listing-incomplete git ls-files stderr: %s"
                              (str/trim (:err untracked))))
            (System/exit 1))
        changed (into #{} (split-nul (:out gd)))
        extra-untracked (into #{} (split-nul (:out untracked)))
        changed-all (into changed extra-untracked)

        ;; ---- independent completeness cross-check (Sol round-2 review, finding 1
        ;; residual: "If the contract must defend against a shim that silently
        ;; lies without stderr, CHECK 1 needs an independent inventory rather than
        ;; trusting the same listing process.") -----------------------------------
        ;; An enumeration that shares no code with EITHER git call above: a PATH
        ;; shim keyed on `ls-files` specifically (the reviewer's exact repro --
        ;; exit 0, empty OR partial stdout, no stderr at all) defeats both checks
        ;; above, so this cross-checks the listing against ground truth that never
        ;; calls `ls-files`: `ls-tree` reads straight from git's OBJECT DATABASE
        ;; (unaffected by working-tree permissions, and not matched by an
        ;; `ls-files`-keyed shim) for "what src/ held at the base commit", and the
        ;; hand-rolled `walk-src` above -- never `file-seq` -- for "what src/
        ;; holds right now". Every fanout target is an in-place edit
        ;; (gen-fanout.clj never adds or removes a file), so absent a real
        ;; injected file these two sets are exactly equal; any gap is either a
        ;; vanished/unreadable file (pruning) or a real untracked file the git
        ;; listing above failed to report.
        walk (walk-src wt)
        ;; Every entry the walk could not turn into exactly one classified inventory
        ;; row is a typed, named refusal (Sol round-3 review, findings 3 and 4).
        ;; Counting directories was never a completeness proof.
        _ (when (seq (:errors walk))
            (println (format "CHECK 1 file-set: ERROR listing-incomplete walk-entries errors=%d dirs-found=%d dirs-entered=%d entries-seen=%d %s"
                              (count (:errors walk)) (:dirs-found walk) (:dirs-entered walk)
                              (:entries-seen walk)
                              (pr-str (vec (take 4 (map (juxt :kind :path :detail) (:errors walk)))))))
            (System/exit 1))
        base-tree (g wt "ls-tree" "-r" "--name-only" "-z" base "--" "src")
        _ (when-not (zero? (:exit base-tree))
            (println (format "CHECK 1 file-set: ERROR listing-incomplete git ls-tree exit=%d %s"
                              (:exit base-tree) (str/trim (:err base-tree))))
            (System/exit 1))
        _ (when (seq (str/trim (:err base-tree)))
            (println (format "CHECK 1 file-set: ERROR listing-incomplete git ls-tree stderr: %s"
                              (str/trim (:err base-tree))))
            (System/exit 1))
        ;; `ls-tree`'s answer is a CLAIM.  The BASELINE is the content-addressed
        ;; rebuild above; the claim is cross-checked against it, and any disagreement
        ;; is a typed refusal (Sol round-3 review, finding 2 -- the coordinated shim
        ;; that forged `ls-files` and `ls-tree` together).
        store-inv (object-store-src-inventory wt base)
        baseline-src (:files store-inv)
        lstree-src (into #{} (split-nul (:out base-tree)))
        only-lstree (sort (set/difference lstree-src baseline-src))
        only-store (sort (set/difference baseline-src lstree-src))
        _ (when (or (seq only-lstree) (seq only-store))
            (println (format "CHECK 1 file-set: ERROR base-inventory mismatch commit=%s tree=%s object-store=%d ls-tree=%d only-in-ls-tree=%s only-in-object-store=%s"
                              (:commit store-inv) (:tree store-inv)
                              (count baseline-src) (count lstree-src)
                              (pr-str (vec (take 4 only-lstree)))
                              (pr-str (vec (take 4 only-store)))))
            (System/exit 1))
        ;; git lists a symlink as ONE leaf path and never looks through it, so the
        ;; set reconciled against git's view is files + symlink leaves, not files
        ;; alone (Sol round-3 review, finding 3).
        walked-src (:entries walk)
        vanished (sort (set/difference baseline-src walked-src))
        _ (when (seq vanished)
            (println (format "CHECK 1 file-set: ERROR listing-incomplete vanished=%d %s (present at base, absent from the independent filesystem walk)"
                              (count vanished) (pr-str (vec (take 4 vanished)))))
            (System/exit 1))
        unreported (sort (remove extra-untracked (set/difference walked-src baseline-src)))
        _ (when (seq unreported)
            (println (format "CHECK 1 file-set: ERROR listing-incomplete unreported=%d %s (present on disk, absent from git's untracked listing)"
                              (count unreported) (pr-str (vec (take 4 unreported)))))
            (System/exit 1))]

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
    ;; Reuses the SAME `walk` this file already validated fail-closed for CHECK 1
    ;; (every entry classified or named, exit before this code is ever reached)
    ;; instead of a second, unchecked `file-seq` call over src/ (Sol round-2 review,
    ;; finding 4).  It reads `:files` -- REGULAR files -- and not `:entries`, because
    ;; slurping a symlink means reading through it, which is the thing the walk
    ;; refuses to do.  That costs nothing: gen-fanout plants no links, so any symlink
    ;; under src/ is a path the manifest does not own and CHECK 1 has already failed
    ;; on it as an extra (or refused the run outright) before CHECK 6 is reached.
    (let [all-src (map #(io/file wt %)
                        (filter #(re-find #"\.cljc?$" %) (:files walk)))
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

(let [args *command-line-args*]
  (if (= "--probe-walk" (first args))
    (probe-walk (second args))
    (apply -main args)))
