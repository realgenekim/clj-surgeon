(ns clj-surgeon.tmp-leak-support-test
  "RATCHET (2026-09-04, inb-9483a4) witnesses for clj-surgeon.tmp-leak-support:
   `with-temp-dir` deletes its directory even when its body throws, and the
   tmpfs predicate that `secure-tmpdir!` refuses on correctly tells a
   RAM-backed directory from a real-disk one.

   `secure-tmpdir!` itself is NOT unit-tested here: on the non-refused path
   it calls (System/exit ...) after waiting on a re-exec'd child process --
   correct behaviour for a suite bootstrap, but calling it from inside a
   nested test would tear down the very suite process running this test.
   Its REFUSAL branches, its JVM-flag and argv forwarding, its descendant
   TMPDIR inheritance and its shutdown sweep are driven as real subprocesses
   by `test/tmp_leak_ratchet_test.sh` (`make tmp-leak-ratchet-self-test`,
   inside `mcp-test`). Round one claimed instead that \"every green suite run
   IS the end-to-end proof\"; the accepted path is not the requirement, and
   an independent review found the refusal failed open."
  (:require
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; @spec MCP-OP-TMPHYG-002
(deftest with-temp-dir-cleans-up-on-throw
  (testing "a deliberately failing body still deletes its temp dir"
    (let [captured (atom nil)]
      (is (thrown? clojure.lang.ExceptionInfo
            (tmp-leak/with-temp-dir [dir "tmp-leak-support-fail-witness-"]
              (reset! captured dir)
              (is (.exists dir) "the dir exists before the deliberate failure")
              (throw (ex-info "deliberate failure" {})))))
      (is (some? @captured) "the temp dir was created before the throw")
      (is (not (.exists @captured))
          "with-temp-dir deleted it in `finally` despite the throw"))))

;; @spec MCP-OP-TMPHYG-002
(deftest with-temp-dir-cleans-up-on-success
  (let [captured (atom nil)]
    (tmp-leak/with-temp-dir [dir "tmp-leak-support-ok-witness-"]
      (reset! captured dir)
      (is (.exists dir)))
    (is (not (.exists @captured)))))

;; @spec MCP-OP-TMPHYG-001
;; @spec MCP-OP-TMPHYG-003
(deftest ram-paths-are-refused-by-name-with-no-external-binary
  (testing "the literal prefixes ~/bin/seat-tmp-guard.sh already refused on"
    (is (true? (tmp-leak/literal-ram-path? "/tmp")))
    (is (true? (tmp-leak/literal-ram-path? "/tmp/clj-surgeon-suite-1-abc")))
    (is (true? (tmp-leak/literal-ram-path? "/dev/shm")))
    (is (true? (tmp-leak/literal-ram-path? "/dev/shm/anything"))))
  (testing "a real-disk path is not refused by name"
    (is (false? (tmp-leak/literal-ram-path? "/var/tmp/forge")))
    (is (false? (tmp-leak/literal-ram-path? "/var/tmpfsish")))))

;; @spec MCP-OP-TMPHYG-003
(deftest the-base-decision-is-a-typed-refusal
  (testing "a RAM-backed base refuses, and the refusal names it"
    (let [refusal (tmp-leak/base-refusal "/dev/shm")]
      (is (some? refusal))
      (is (contains? #{:ram-path-prefix :tmpfs} (:reason refusal)))
      (is (str/starts-with? (tmp-leak/refusal-message refusal) "tmp-refused: "))))
  (testing "the suite's own base is PROVEN real disk -- it would not be here otherwise"
    (is (nil? (tmp-leak/base-refusal (tmp-leak/env-or-current-tmpdir)))))
  (testing "an undeterminable fstype is a named refusal, not a pass.
            mount-fstype is TRI-STATE: nil coerced to \"not tmpfs\" is what
            failed open. The :unknown branch is EXECUTED by
            test/tmp_leak_ratchet_test.sh 3d, which shims findmnt to fail and
            points CLJ_SURGEON_MOUNTS_FILE at nothing -- the only way to reach
            it, since findmnt --target resolves any path to its nearest
            existing ancestor and the mounts table always matches / at worst."
    (let [message (tmp-leak/refusal-message
                    {:reason :unknown-fstype :base "/some/base"})]
      (is (str/starts-with? message "tmp-refused: "))
      (is (str/includes? message "UNDETERMINABLE")))))

;; @spec MCP-OP-TMPHYG-004
(deftest only-this-namespaces-own-run-roots-are-sweepable
  (testing "the shared base itself can never be swept"
    (is (false? (tmp-leak/own-isolated-root? "/var/tmp/forge")))
    (is (false? (tmp-leak/own-isolated-root? "/var/tmp/forge/other-seat-fixture"))))
  (testing "a per-run root is"
    (is (true? (tmp-leak/own-isolated-root? "/var/tmp/forge/clj-surgeon-suite-42-deadbeef")))))

;; @spec MCP-OP-TMPHYG-004
(deftest sweep-root-refuses-a-directory-it-did-not-create
  (testing "a directory that is not one of this namespace's per-run roots
            survives sweep-root!, which returns false"
    (tmp-leak/with-temp-dir [parent "tmp-leak-sweep-guard-"]
      (let [foreign (io/file parent "other-seat-precious-fixture")]
        (.mkdirs foreign)
        (is (false? (tmp-leak/sweep-root! foreign)))
        (is (.exists foreign) "the sweep must not delete what it did not create"))
      (let [ours (io/file parent "clj-surgeon-suite-42-deadbeef")]
        (.mkdirs ours)
        (is (true? (tmp-leak/sweep-root! ours)))
        (is (not (.exists ours)))))))

;; @spec MCP-OP-TMPHYG-012
(deftest the-make-layer-does-not-propagate-a-ram-tmpdir
  (testing "SELF_TEST_TMP is handed straight to shell harnesses that never
            reach the Clojure refusal, so the redirect has to happen at the
            Make layer. This EXECUTES make's own expansion (a throwaway target
            supplied with --eval against the real Makefile) rather than
            reading the assignment as text."
    (let [self-test-tmp (fn [tmpdir]
                          (let [{:keys [exit out]}
                                (shell/sh "env" (str "TMPDIR=" tmpdir)
                                          "make" "-s"
                                          "--eval=__tmphyg_print:; @echo $(SELF_TEST_TMP)"
                                          "__tmphyg_print")]
                            (when (zero? exit) (str/trim (or (last (str/split-lines out)) "")))))]
      (doseq [ram ["/tmp" "/tmp/x" "/dev/shm" "/dev/shm/y"]]
        (let [got (self-test-tmp ram)]
          (is (= "/var/tmp" got)
              (str "a RAM TMPDIR (" ram ") must be redirected, got " (pr-str got)))))
      (is (= "/var/tmp" (self-test-tmp ""))
          "an unset TMPDIR defaults to /var/tmp")
      (is (= "/var/tmp/forge" (self-test-tmp "/var/tmp/forge"))
          "a real-disk TMPDIR is honoured unchanged"))))

;; @spec MCP-OP-TMPHYG-013
(deftest sweep-root-does-not-claim-a-delete-it-did-not-perform
  (testing "a receipt must name a subject it actually acted on. sweep-root!
            returned true for every own-named root, whether or not the tree
            was still there afterwards -- a delete it did not perform read as
            a delete it did. It now reports whether the root is GONE."
    (tmp-leak/with-temp-dir [parent "tmp-leak-sweep-receipt-"]
      (let [locked (io/file parent "locked")
            root (io/file locked "clj-surgeon-suite-42-undeletable")]
        (.mkdirs root)
        (.setWritable locked false false)
        (try
          (is (.exists root) "precondition: the root is there before the sweep")
          (is (false? (tmp-leak/sweep-root! root))
              "an undeletable root must not be reported as swept")
          (is (.exists root) "and it really is still there")
          (finally (.setWritable locked true false))))
      (let [ours (io/file parent "clj-surgeon-suite-42-deletable")]
        (.mkdirs ours)
        (is (true? (tmp-leak/sweep-root! ours)))
        (is (not (.exists ours)))))))

;; @spec MCP-OP-TMPHYG-011
(deftest a-seam-sourced-fstype-can-never-prove-real-disk
  (let [findmnt (ns-resolve 'clj-surgeon.tmp-leak-support 'findmnt-fstype)
        table (ns-resolve 'clj-surgeon.tmp-leak-support 'mounts-table-fstype)
        seam (ns-resolve 'clj-surgeon.tmp-leak-support 'seam-mounts-file)
        disk "/var/tmp/forge"]
    (testing "with no seam set, a mounts-table ext4 answer IS proof of disk"
      (with-redefs-fn {findmnt (constantly nil)
                       table (constantly "ext4")
                       seam (constantly nil)}
        #(do (is (= "ext4" (tmp-leak/mount-fstype disk)))
             (is (nil? (tmp-leak/base-refusal disk))))))
    (testing "the SAME answer, sourced from the seam, is :unknown -- a refusal.
              An operator handing the check a forged table must not be able to
              convert `I cannot prove this is disk` into `proven disk`."
      (with-redefs-fn {findmnt (constantly nil)
                       table (constantly "ext4")
                       seam (constantly "/forged/mounts")}
        #(do (is (= :unknown (tmp-leak/mount-fstype disk)))
             (is (= :unknown-fstype (:reason (tmp-leak/base-refusal disk)))))))
    (testing "and the seam is still sound in the REFUSING direction"
      (with-redefs-fn {findmnt (constantly nil)
                       table (constantly "tmpfs")
                       seam (constantly "/forged/mounts")}
        #(is (= :tmpfs (:reason (tmp-leak/base-refusal disk))))))))

;; @spec MCP-OP-TMPHYG-010
(deftest no-gate-names-a-hard-coded-ram-path
  (testing "a refusal at the runner is worthless if the gates around it create
            directories in RAM by name. Two shapes are offenders: a literal
            /tmp/<name> used as a path, and a TMPDIR fallback that NAMES /tmp
            (which is taken in every shell without seat-tmp-guard.sh). Prose
            mentions of /tmp are not matches, and `/home/x/tmp/y` is not one."
    (let [pattern #"(?:^|[^A-Za-z0-9_.-])/tmp/[A-Za-z0-9_.]|TMPDIR:-/tmp\}"
          shells (fn [dir] (filter #(str/ends-with? (.getName ^java.io.File %) ".sh")
                                   (.listFiles (io/file dir))))
          ;; `bench/*.sh` is IN SCOPE: `make test` runs four of those harnesses,
          ;; so a hard-coded root in a bench self-test is a directory this
          ;; repo's own test command creates in RAM.
          files (concat [(io/file "Makefile")] (shells "test") (shells "bench"))
          offenders (for [^java.io.File f files
                          :when (.exists f)
                          [n line] (map-indexed (fn [i l] [(inc i) l])
                                                (str/split-lines (slurp f)))
                          ;; No exemption for `${TMPDIR:-/tmp}`: that form
                          ;; NAMES the RAM path and takes it whenever TMPDIR is
                          ;; unset -- which is every shell without
                          ;; seat-tmp-guard.sh. An audit written to permit the
                          ;; exact fallback it exists to forbid is backwards.
                          :when (re-find pattern line)]
                      (str (.getPath f) ":" n ": " (str/trim line)))]
      (is (empty? offenders)
          (str "hard-coded /tmp write targets: " (pr-str (vec offenders)))))))

(deftest tmpfs-predicate-tells-ram-from-disk
  (testing "/dev/shm is tmpfs-backed -- this is what secure-tmpdir! refuses on"
    (is (true? (tmp-leak/tmpfs? "/dev/shm"))))
  (testing "the suite's own current java.io.tmpdir base is NOT tmpfs -- the
            suite would not have gotten this far otherwise"
    (is (false? (tmp-leak/tmpfs? (tmp-leak/env-or-current-tmpdir))))))

;; @spec MCP-OP-TMPHYG-005
(deftest test-child-environment-fixes-node-cache-policy-in-both-home-modes
  (doseq [isolate-home? [false true]]
    (let [env (#'tmp-leak/child-environment "/var/tmp/clj-surgeon-suite-42-node" isolate-home?)]
      (is (= "1" (get env "NODE_DISABLE_COMPILE_CACHE")))
      (is (= "/var/tmp/clj-surgeon-suite-42-node" (get env "TMPDIR")))
      (is (= isolate-home? (contains? env "HOME"))))))

;; @spec MCP-OP-TMPHYG-013
(deftest darwin-has-a-mount-authority-of-its-own
  (testing "Both mount sources were Linux-only -- findmnt is util-linux and the
            table is /proc/mounts. On macOS neither could answer, mount-fstype
            returned :unknown, base-refusal correctly failed CLOSED, and EVERY
            JVM in the suite exited 97. The ratchet was not wrong; it had no
            authority to ask. `mount(8)` is darwin's own authority, and this is
            the part of it that can be wrong: the parse."
    ;; REAL macOS `mount` output, not a synthesised shape. It carries the two
    ;; cases a naive whitespace split gets wrong: a mount point containing a
    ;; SPACE, and nested volumes where the longest prefix must win over `/`.
    (let [table (str "/dev/disk3s1s1 on / (apfs, sealed, local, read-only, journaled)\n"
                     "devfs on /dev (devfs, local, nobrowse)\n"
                     "/dev/disk3s6 on /System/Volumes/VM (apfs, local, noexec, journaled, noatime, nobrowse)\n"
                     "/dev/disk3s2 on /System/Volumes/Data (apfs, local, journaled, nobrowse)\n"
                     "map auto_home on /System/Volumes/Data/home (autofs, automounted, nobrowse)\n"
                     "/dev/disk5s1 on /Volumes/Macintosh HD Backup (hfs, local, nodev, nosuid, journaled)")]
      (testing "the root volume answers for an unnested path"
        (is (= "apfs" (tmp-leak/parse-darwin-mount-table table "/")))
        ;; The per-user $TMPDIR every Mac shell sets. This is THE path the gate
        ;; must prove, and proving it is what unblocks `make test` on a laptop.
        (is (= "apfs" (tmp-leak/parse-darwin-mount-table
                        table "/private/var/folders/ab/cd/T/"))))
      (testing "a nested mount point beats the root -- longest prefix wins"
        (is (= "devfs" (tmp-leak/parse-darwin-mount-table table "/dev")))
        (is (= "autofs" (tmp-leak/parse-darwin-mount-table
                          table "/System/Volumes/Data/home/gene")))
        (is (= "apfs" (tmp-leak/parse-darwin-mount-table
                        table "/System/Volumes/Data/Users/gene/x"))))
      (testing "a mount point containing a space is read whole, not split on it"
        (is (= "hfs" (tmp-leak/parse-darwin-mount-table
                       table "/Volumes/Macintosh HD Backup/z"))))
      (testing "and a prefix that only LOOKS nested does not match"
        ;; /System/Volumes/VMware is not under /System/Volumes/VM.
        (is (= "apfs" (tmp-leak/parse-darwin-mount-table
                        table "/System/Volumes/VMware/x"))))
      (testing "an EMPTY table covers nothing; it does not invent a filesystem"
        (is (nil? (tmp-leak/parse-darwin-mount-table nil "/x")))
        (is (nil? (tmp-leak/parse-darwin-mount-table "" "/x"))))))
  ;; @spec MCP-OP-TMPHYG-013
  (testing "Sol SKIFF-INSTALL-FENCE-001. A mount point may CONTAIN the bytes
            ` on `. Splitting on the last one discarded the nested row as
            malformed, and the surviving `/` row then answered apfs for a target
            that was really on tmpfs -- crafted text making the ratchet PROVE
            real disk. The row you cannot read is exactly the row that may be
            covering your target."
    (let [evil (str "/dev/root on / (apfs, local)\n"
                    "/dev/ram on /private/var/folders/evil on ram (tmpfs, local)")
          answer (tmp-leak/parse-darwin-mount-table
                   evil "/private/var/folders/evil on ram/T")]
      (is (not= "apfs" answer)
          "the exact regression: crafted mount text proved real disk")
      (is (contains? #{"tmpfs" :unknown} answer)
          "a covering row is read, or the table refuses -- never a fallback")))
  ;; @spec MCP-OP-TMPHYG-013
  (testing "Sol SKIFF-INSTALL-FENCE-001 round 2. A trailing space is a legal
            Unix path byte. The parser captured the mount point correctly and
            then ran str/trim over it, so the covering row `/Volumes/ram `
            became `/Volumes/ram`, stopped covering the real target, and the `/`
            row proved apfs for a tmpfs path -- the same safety class as the
            crafted ` on ` row, reintroduced by a normalisation nobody asked for.
            Exactly one byte is removed: the space delimiting the option group."
    (let [;; TWO spaces before `(tmpfs`: the first is the last byte of the
          ;; mount point, the second is the delimiter.
          table (str "/dev/root on / (apfs, local)\n"
                     "/dev/ram on /Volumes/ram  (tmpfs, local)")
          answer (tmp-leak/parse-darwin-mount-table table "/Volumes/ram /T")]
      (is (not= "apfs" answer)
          "trimming the mount point let the root row prove past a tmpfs mount")
      (is (= "tmpfs" answer)))
    (testing "and the trimmed and untrimmed spellings are DISTINCT mount points"
      (let [table (str "/dev/root on / (apfs, local)\n"
                       "/dev/a on /Volumes/ram (hfs, local)\n"
                       "/dev/b on /Volumes/ram  (tmpfs, local)")]
        (is (= "hfs" (tmp-leak/parse-darwin-mount-table table "/Volumes/ram/T")))
        (is (= "tmpfs" (tmp-leak/parse-darwin-mount-table table "/Volumes/ram /T"))))))
  (testing "a mount point containing PARENTHESES still parses: the trailing
            group is the last (...) and flags never contain parentheses"
    (let [table (str "/dev/root on / (apfs, local)\n"
                     "/dev/d5 on /Volumes/My (Disk) Backup (hfs, local)")]
      (is (= "hfs" (tmp-leak/parse-darwin-mount-table
                     table "/Volumes/My (Disk) Backup/z")))))
  (testing "ANY row this parser cannot read poisons the WHOLE table to
            :unknown, which base-refusal treats as a refusal. A partially
            understood mount table must never answer for a target."
    (is (= :unknown (tmp-leak/parse-darwin-mount-table
                      "/dev/root on / (apfs, local)\ntotally bogus line" "/x")))
    (is (= :unknown (tmp-leak/parse-darwin-mount-table "garbage" "/x")))
    (is (= :unknown (tmp-leak/parse-darwin-mount-table
                      "/dev/root on / (apfs, local)\n/dev/x on /y (" "/y/z"))))
  (testing "Sol SKIFF-INSTALL-FENCE-001, second half: `mount` was run by BARE
            NAME through PATH, so a shim earlier on PATH could print
            `/dev/fake on / (apfs, local)` and base-refusal returned nil. The
            docstring's claim that this was non-redirectable was false. Only
            absolute paths are named now."
    (let [binaries @(resolve 'clj-surgeon.tmp-leak-support/darwin-mount-binaries)]
      (is (seq binaries))
      (is (every? #(clojure.string/starts-with? % "/") binaries)
          "a bare name is redirectable by PATH; an absolute path is not")))
  (testing "the remedy no longer advertises a directory that exists on one box"
    (let [message (tmp-leak/refusal-message (tmp-leak/base-refusal "/tmp"))]
      (is (not (clojure.string/includes? message "/var/tmp/forge"))
          "the remedy named this seat's scratch dir to every operator alive")
      (is (clojure.string/includes? message "TMPDIR")))))
