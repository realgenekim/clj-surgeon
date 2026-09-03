#!/usr/bin/env bb
;; MEM-005 red witness — reproduce the memory battery's two adversarial shape
;; findings as a fast, standalone, subprocess-based check.
;;
;; The battery (docs/observations/2026-09-03-memory-battery-baseline.md, round 2)
;; found that `cli-ls-tree` peaks at 386.4 MB on ONE 1.9 MiB file and 285.7 MB on
;; ONE 111 KB 300-deep file, against a 248 MB budget — heap sized by a file's
;; SHAPE, not by the repository's size. This witness isolates that to one
;; `outline-source` call per JVM so the failure is reproducible in seconds
;; instead of minutes, and so a fix can be measured against it directly.
;;
;; It is HEAVY (several JVMs, each with an explicit -Xmx) and is NOT in
;; `make test`, `make test-fast`, or `make mcp-test`. Run it with `make memory-red`,
;; which takes the exclusive suite lock.
;;
;; Exit 0 = every assertion held. Exit 1 = an assertion did not hold.
;; While MEM-005 is unimplemented this exits 0 — the assertions describe the
;; DEFECT. The green re-run inverts them via --expect green.

(ns red-witness
  (:require [babashka.process :as p]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(load-file "bench/memory_battery/generate_tree.clj")

(def source-for (resolve 'generate-tree/source-for))

;; ------------------------------------------------------------------
;; Pass lines — every number here is measured, none is a judgement call
;; ------------------------------------------------------------------

(def budget-mb
  "The memory battery's per-operation peak budget at -Xmx512m: start + 224 MiB.
  Quoted from its round-2 verdict lines (`:limit 247.8`), rounded down."
  247.8)

(def giant-oom-xmx
  "The smallest -Xmx in {128m 192m 256m 384m 512m} at which one outline of the
  1.9 MiB flat file throws OutOfMemoryError. Measured on anvil 2026-09-03:
  128m OOM; 192m completes at peak 186.5 MB. Not a judgement: the ladder is in
  docs/observations/2026-09-03-mem-005-parser-admission.md."
  "128m")

(def battery-xmx
  "The battery's own heap ceiling, so the peak numbers here are comparable to
  its published cells."
  "512m")

(def nested-warmups
  "Outlines of an ordinary file executed before the adversarial one. 0 = cold
  (interpreted parser frames), 200 = the parser's hot path is JIT-compiled."
  200)

;; ------------------------------------------------------------------

(defn- classpath [root]
  (let [cache (io/file root "classpath.txt")]
    (when-not (.exists cache)
      (let [r (p/shell {:out :string :err :string}
                       "clojure" "-A:clj-surgeon/memory-battery" "-Spath")]
        (io/make-parents cache)
        (spit cache (str/trim (:out r)))))
    (str (str/trim (slurp cache)) ":bench/parser_admission")))

(defn- fixture!
  "Write one adversarial fixture, byte-identical to the memory battery's corpus
  for that profile, and return its path."
  [root profile]
  (let [f (io/file root "fixtures" (str (name profile) ".clj"))]
    (io/make-parents f)
    (spit f (source-for profile 0 1))
    (.getPath f)))

(defn- probe
  "Run ONE outline in ONE fresh JVM at an explicit -Xmx and return its facts."
  [cp path xmx warmups]
  (let [r (p/shell {:out :string :err :string :continue true}
                   "timeout" "600"
                   "java" (str "-Xmx" xmx) "-cp" cp
                   "clojure.main" "-m" "shape-probe" path (str warmups))
        line (last (remove str/blank? (str/split-lines (str (:out r)))))]
    (cond
      (and line (str/starts-with? (str/trim line) "{"))
      (assoc (edn/read-string line) :xmx xmx)

      ;; A JVM that dies of heap exhaustion or stack exhaustion often cannot
      ;; describe itself; its death IS the fact. Read it from stderr rather
      ;; than reporting an unverified "probe-failed".
      (str/includes? (str (:err r)) "OutOfMemoryError")
      {:outcome :out-of-memory :xmx xmx :warmups warmups :via :stderr}

      (str/includes? (str (:err r)) "StackOverflowError")
      {:outcome :stack-overflow :xmx xmx :warmups warmups :via :stderr}

      :else
      {:outcome :probe-failed :xmx xmx :exit (:exit r)
       :stderr (str/join " " (take-last 3 (str/split-lines (str (:err r)))))})))

(defn- check [label ok? detail]
  (println (format "%-6s %-46s %s" (if ok? "PASS" "FAIL") label (pr-str detail)))
  ok?)

(defn -main [& args]
  (let [opts (apply hash-map args)
        root (get opts "--root" "/home/forge/tmp/admit/parser-red")
        expect (get opts "--expect" "red")
        _ (.mkdirs (io/file root))
        cp (classpath root)
        nested (fixture! root :nested)
        giant (fixture! root :giant)
        cold (probe cp nested battery-xmx 0)
        warm (probe cp nested battery-xmx nested-warmups)
        big (probe cp giant giant-oom-xmx 0)
        big-512 (probe cp giant battery-xmx 0)]
    (println)
    (println (format "MEM-005 red witness — expect=%s budget=%.1f MB" expect budget-mb))
    (println "----------------------------------------------------------------------")
    (doseq [[label r] [["nested cold" cold] ["nested warm" warm]
                       [(str "giant  " giant-oom-xmx) big]
                       [(str "giant  " battery-xmx) big-512]]]
      (println (format "%-16s %-9s Xmx=%-5s warm=%-4s peak=%7.1f MB  %6d ms  %d source bytes"
                       label (name (:outcome r)) (:xmx r) (str (:warmups r))
                       (double (or (:peak-mb r) 0.0)) (long (or (:wall-ms r) 0))
                       (long (or (:source-bytes r) 0)))))
    (println "----------------------------------------------------------------------")
    (let [results
          (if (= expect "red")
            [(check "nested cold: outline does not complete"
                    (= :stack-overflow (:outcome cold))
                    (select-keys cold [:outcome :wall-ms]))
             (check "nested warm: one 111 KB file over budget"
                    (and (= :completed (:outcome warm))
                         (> (:peak-mb warm) budget-mb))
                    (select-keys warm [:outcome :peak-mb]))
             (check (str "giant " giant-oom-xmx ": one 1.9 MiB file OOMs")
                    (= :out-of-memory (:outcome big))
                    (select-keys big [:outcome :peak-mb]))]
            [(check "nested cold: typed refusal, no crash"
                    (= :completed (:outcome cold))
                    (select-keys cold [:outcome :wall-ms]))
             (check "nested cold: refuses in under 50 ms"
                    (< (long (or (:wall-ms cold) 99999)) 50)
                    (select-keys cold [:wall-ms]))
             (check "nested warm: typed refusal, under budget"
                    (and (= :completed (:outcome warm))
                         (< (:peak-mb warm) budget-mb))
                    (select-keys warm [:outcome :peak-mb]))
             (check "nested warm: refuses in under 50 ms"
                    (< (long (or (:wall-ms warm) 99999)) 50)
                    (select-keys warm [:wall-ms]))
             (check (str "giant " giant-oom-xmx ": typed refusal, no OOM")
                    (= :completed (:outcome big))
                    (select-keys big [:outcome :peak-mb]))
             (check (str "giant " giant-oom-xmx ": refuses in under 50 ms")
                    (< (long (or (:wall-ms big) 99999)) 50)
                    (select-keys big [:wall-ms]))])]
      (println)
      (if (every? true? results)
        (do (println (format "memory-red: %d/%d assertions held (expect=%s)"
                             (count results) (count results) expect))
            (System/exit 0))
        (do (println (format "memory-red: %d/%d assertions held (expect=%s) — FAIL"
                             (count (filter true? results)) (count results) expect))
            (System/exit 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
