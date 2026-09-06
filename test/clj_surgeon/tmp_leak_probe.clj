(ns clj-surgeon.tmp-leak-probe
  "RATCHET witness apparatus (2026-09-04, inb-9483a4, round two).

   A minimal test entry point that drives the REAL
   `clj-surgeon.tmp-leak-support/secure-tmpdir!` and then reports, on
   stdout, the facts a witness needs to check the mechanism from outside:
   which role this process is (parent before the re-exec, or the isolated
   child), its heap ceiling, its java.io.tmpdir, and the argv it was handed.

   It exists because `secure-tmpdir!` cannot be unit-tested in-process: on
   the accepted path it re-executes the suite and calls `System/exit`, which
   would tear down the very suite running the test. Driving THIS namespace
   in a subprocess is how the refusal branches, the JVM-flag forwarding, the
   arg forwarding, the descendant TMPDIR inheritance and the shutdown sweep
   get executed by a gate instead of asserted about.

   Never referenced by any runner's namespace list; `test/tmp_leak_ratchet_test.sh`
   is its only caller.

   @spec MCP-OP-TMPHYG-003
   @spec MCP-OP-TMPHYG-006"
  (:require
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.java.shell :as shell]
   [clojure.string :as str]))

(defn- max-mb
  []
  (long (/ (.maxMemory (Runtime/getRuntime)) 1024 1024)))

(defn- report!
  [role args]
  (println (format "PROBE role=%s max-mb=%d tmpdir=%s node-cache=%s args=%s"
                   role (max-mb) (System/getProperty "java.io.tmpdir")
                   (or (System/getenv "NODE_DISABLE_COMPILE_CACHE") "<unset>")
                   (pr-str (vec args)))))

(defn -main
  [& args]
  (report! (if (System/getenv "CLJ_SURGEON_TMPDIR_REEXEC") "child-pre" "parent") args)
  (let [{:keys [refused root]} (tmp-leak/secure-tmpdir!
                                 {:bb-script "test/tmp_leak_probe.clj"
                                  :main-ns "clj-surgeon.tmp-leak-probe"}
                                 args)
        _ (when refused (System/exit 97))
        before (tmp-leak/tmp-entries)
        argset (set args)]
    (report! "child" args)
    (println (format "PROBE root=%s" root))
    ;; @spec MCP-OP-TMPHYG-005 -- reuse the existing JVM/BB argv probes.
    (when (contains? argset "--node-cache-env")
      (let [{:keys [exit out]} (shell/sh "sh" "-c" "printf %s \"$NODE_DISABLE_COMPILE_CACHE\"")]
        (when-not (zero? exit)
          (throw (ex-info "Node-cache environment descendant probe failed" {:exit exit})))
        (println (str "PROBE descendant-node-cache=" out))))
    (when (contains? argset "--leak-subprocess")
      ;; A descendant that picks its OWN temp location: the only thing that
      ;; keeps it inside the isolated root is TMPDIR in the child's env.
      (let [out (:out (shell/sh "sh" "-c" "mktemp -d"))]
        (println (format "PROBE subprocess-tmpdir=%s" (str/trim out)))))
    (when (contains? argset "--sleep")
      (println "PROBE sleeping")
      (flush)
      (Thread/sleep 60000))
    (let [leak (tmp-leak/report-and-sweep-leak! root before)]
      (println (format "PROBE leak-exit=%d" leak))
      (flush)
      (System/exit leak))))
