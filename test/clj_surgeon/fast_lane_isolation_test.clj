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
