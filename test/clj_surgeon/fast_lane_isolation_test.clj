(ns ^{:lane :fast} clj-surgeon.fast-lane-isolation-test
  "TEST-ISO-006 -- the fast lane's JVM runs on a THROWAWAY `user.home` and a
   throwaway `java.io.tmpdir`, both created per run and deleted when the run
   ends.

   This is the `unrepresentable` rung, not the `detected` rung, and the
   difference is the whole point. A witness that scans for `$HOME` catches a
   spelling; a witness that snapshots the real home catches a write after it
   happened. Launching the JVM on a home directory that IS NOT the seat's
   means a fast-lane test cannot read the seat's real state at all -- there
   is nothing there to read -- and cannot leave anything in it, because the
   directory it writes to is deleted with the run root.

   Why a startup `-D` flag and nothing else: `clj-surgeon.tmp-leak-support`'s
   namespace docstring records the measurement that a RUNTIME
   `System/setProperty` is NOT honored for real file creation on either bb or
   a real JVM -- `getProperty` reflects the new value while the JDK keeps
   using the one captured at bootstrap. A first cut of the temp-dir ratchet
   did exactly that and ran GREEN while leaking thousands of directories. The
   same trap applies to `user.home`, so the isolation is done the only way
   that works: the parent creates the directory and RE-EXECS the suite with
   `-Duser.home=<dir>` and `HOME=<dir>` present before the child's own
   bootstrap."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn- prop [k] (System/getProperty k))

(def ^:private isolated-root-prefix "clj-surgeon-suite-")

;; @spec TEST-ISO-006
(deftest the-fast-lane-tmpdir-is-a-private-per-run-root
  (let [tmp (io/file (prop "java.io.tmpdir"))]
    (is (str/starts-with? (.getName tmp) isolated-root-prefix)
        (str "java.io.tmpdir is " (.getPath tmp) " -- the fast lane must run on "
             "a private per-run root named " isolated-root-prefix "<pid>-<hex>, "
             "not on a shared temp base"))
    (is (.isDirectory tmp))))

;; @spec TEST-ISO-006
(deftest the-fast-lane-home-is-a-throwaway-inside-the-run-root
  (let [home (io/file (prop "user.home"))
        tmp (io/file (prop "java.io.tmpdir"))
        root-name (.getName tmp)]
    (testing "user.home is INSIDE this run's own temp root"
      (is (str/includes? (.getCanonicalPath home) root-name)
          (str "user.home is " (.getPath home) " -- a fast-lane JVM must be "
               "launched with -Duser.home inside its own run root (" root-name
               "), so that reading the seat's real home is not merely "
               "discouraged but impossible")))
    (testing "it exists and is writable, so a test that uses $HOME still works"
      (is (.isDirectory home))
      (is (.canWrite home)))
    (testing "the throwaway is EMPTY of the seat's real state"
      (doseq [tell [".m2" ".gitlibs" ".ssh" ".config" ".claude" "src" "secrets"]]
        (is (not (.exists (io/file home tell)))
            (str "the throwaway home contains " tell
                 " -- it is not a throwaway, it is the seat's real home"))))))

;; @spec TEST-ISO-006
(deftest every-home-spelling-agrees-so-a-child-cannot-escape
  (testing "HOME in the environment matches user.home"
    (is (= (.getCanonicalPath (io/file (prop "user.home")))
           (.getCanonicalPath (io/file (or (System/getenv "HOME") "/nonexistent"))))
        (str "user.home=" (prop "user.home") " but $HOME=" (System/getenv "HOME")
             " -- a subprocess reads $HOME, not the JVM property, so the two "
             "must name the same throwaway or the isolation leaks through the "
             "first thing that shells out")))
  (testing "TMPDIR matches java.io.tmpdir for the same reason"
    (is (= (.getCanonicalPath (io/file (prop "java.io.tmpdir")))
           (.getCanonicalPath (io/file (or (System/getenv "TMPDIR") "/nonexistent")))))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-003
;; ROUND FIVE -- the round-three landing review's finding 3, as a witness.
;;
;; The manifest promises `:fast` performs no read outside its own
;; `java.io.tmpdir` subtree and is N-wide safe. `scope-stream-test` rooted its
;; fixtures at `$CLJ_SURGEON_MEMORY_TMP` or, absent that, the literal
;; `/home/forge/tmp` -- a SEAT-ABSOLUTE path, shared by every clone in the
;; concurrency battery and outside every ratchet's window. Round four moved it
;; onto `java.io.tmpdir`. This is the ratchet that keeps it there, and it is
;; deliberately a SOURCE scan over the whole fast lane rather than a check of
;; the one namespace that was wrong: the review found this by reading one
;; file, and a fix that only pins the file it read closes an instance, not a
;; class.
;;
;; Reach, stated: `Files/createTempDirectory` and `createTempFile` derive from
;; `java.io.tmpdir` by construction and need no check; TEST-ISO-006 has
;; already made that root a throwaway. What this cannot see is a root computed
;; at runtime from a value it does not recognise -- the per-namespace write
;; probe (TEST-ISO-003, `ns-isolation`) is the behavioural half that catches
;; the leavings whatever spelling produced them.
;; ---------------------------------------------------------------------------

(def ^:private seat-absolute-fixture-shapes
  "Spellings that root a fixture somewhere other than this run's own temp
   root. Each is a real defect shape, not a hypothetical one: the first is
   what `scope-stream-test` and `txn-journal-test` both did."
  [[#"\(System/getenv\s+\"[A-Z_]*TMP[A-Z_]*\"\)\s+\"/"
    "an environment temp override falling back to a seat-absolute literal"]
   [#"\(io/file\s+\"/(?:home|tmp|var|Users)"
    "an absolute path literal as a fixture root"]
   [#"\"/home/[a-z]+/tmp"
    "the seat's shared /home/<user>/tmp"]])

(deftest no-fast-lane-namespace-roots-a-fixture-outside-its-own-tmpdir
  (let [manifest (requiring-resolve 'clj-surgeon.lane-manifest/manifest)
        namespaces-for (requiring-resolve 'clj-surgeon.lane-manifest/namespaces-for)
        source-of (fn [n]
                    (io/file "test" (str (-> (str n)
                                             (str/replace "-" "_")
                                             (str/replace "." "/"))
                                         ".clj")))
        offenders (for [n (namespaces-for :fast)
                        :let [src (slurp (source-of n))]
                        [re what] seat-absolute-fixture-shapes
                        :let [hit (re-find re src)]
                        :when hit
                        ;; this witness names the shapes it hunts, in itself
                        :when (not= n 'clj-surgeon.fast-lane-isolation-test)]
                    (format "%s -- %s (%s)" n what (pr-str hit)))]
    (is (some? manifest) "sanity: the manifest loaded")
    (is (empty? offenders)
        (str (count offenders) " fast-lane namespace(s) rooting fixtures "
             "outside this run's own java.io.tmpdir subtree. The lane's rule "
             "is that it reads nothing outside its own temp root, and a "
             "seat-absolute root is also shared by every clone the "
             "concurrency battery runs at once: "
             (str/join "; " offenders)))))
