;; @spec MCP-OP-ADMIT-130
;;
;; A bounded no-OOM proof for the ADMIT path, at an explicit -Xmx and with a
;; numeric pass line.
;;
;; Why this exists and is not the memory battery. The battery measures the
;; READ path: its arms are corpus TREES (100/1k/10k source files, plus cljc,
;; giant and nested shapes) driven through the ops registry. Nothing in it
;; drives `admit_clojure_patch`, and the admit path's memory question is a
;; different one: `default-lint-runner` reads TWO analyzer images and holds
;; both parsed structures live at once, so its worst case is shaped by
;; findings DIVERSITY, not by file count. MCP-OP-ADMIT-122's 16 MiB read
;; ceiling bounds what this JVM READS back; it does not bound what parsing
;; that much retains. This arm measures the retention.
;;
;; It is deliberately NOT wired into `make test`, `make test-fast` or
;; `make mcp-test`: it starts its own bounded JVM and runs in tens of
;; seconds. `admit-analyzer-memory-self-test` is its only entry point.

(require '[clj-surgeon.mcp-admit-tool :as admit]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(def scales [100 1000 10000])
(def findings-per-file 3)

(defn- source
  [n]
  (str "(ns app.f" n ")\n\n(defn go\n  [state]\n  (update state :n inc))\n"))

(defn- images
  [n]
  (mapv (fn [i]
          {:file (str "src/app/f" i ".clj")
           :operation :update
           :pre (source i)
           :post (str (source i) "\n;; touched\n")})
        (range n)))

(defn- findings-edn
  "One analyzer answer with `n` * findings-per-file DISTINCT findings.

  Distinct on purpose: identical rows would be collapsed by the delta's sets
  and would flatter the measurement, which is the mistake the round-one
  review called out in its own single-fixture ceiling probe."
  [n tag]
  (let [rows (for [i (range n)
                   j (range findings-per-file)]
               (str "{:filename \"src/app/f" i ".clj\" :row " (inc j)
                    " :col " (inc j) " :level :warning :type :unused-binding"
                    " :message \"" tag " unused binding q" i "_" j
                    " shadows an outer one\"}"))]
    (str "{:findings [" (str/join " " rows) "]}")))

(defn- fake-analyzer!
  "An analyzer that answers from a file, chosen by which image it was given.

  Named so `clj-kondo-command?` is false: this arm measures what the gate
  RETAINS from a large answer, not the admission wrapper."
  [dir pre-file post-file]
  (let [script (io/file dir "fake-analyzer")]
    (spit script
          (str "#!/bin/sh\n"
               "case \"$*\" in\n"
               "  *clj-surgeon-admit-post*) cat " (.getPath post-file) " ;;\n"
               "  *) cat " (.getPath pre-file) " ;;\n"
               "esac\n"))
    (.setExecutable script true false)
    (.getCanonicalPath script)))

(defn- used-mib
  []
  (let [runtime (Runtime/getRuntime)]
    (quot (- (.totalMemory runtime) (.freeMemory runtime)) (* 1024 1024))))

(defn- max-mib
  []
  (quot (.maxMemory (Runtime/getRuntime)) (* 1024 1024)))

(defn- delete-tree!
  [^java.io.File file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))] (.delete child))))

(defn- arm
  [n]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                       "admit-analyzer-memory"
                       (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (let [pre-file (io/file dir "pre.edn")
            post-file (io/file dir "post.edn")
            _ (spit pre-file (findings-edn n "pre"))
            _ (spit post-file (findings-edn n "post"))
            analyzer (fake-analyzer! dir pre-file post-file)
            bytes (+ (.length pre-file) (.length post-file))
            root (io/file dir "workspace")
            _ (.mkdirs root)
            config {:project-root (.getPath root)
                    :admit-analyzer-command [analyzer "{files}"]}
            _ (System/gc)
            before (used-mib)
            started (System/currentTimeMillis)
            result (admit/default-lint-runner config (images n))
            elapsed (- (System/currentTimeMillis) started)
            peak (used-mib)
            budget (long (* 0.80 (max-mib)))
            ok? (and (true? (:ran result)) (<= peak budget))]
        (println (format (str "%s n=%d findings=%d analyzer-bytes=%d "
                              "ran=%s introduced=%s heap-start-MiB=%d "
                              "heap-peak-MiB=%d budget-MiB=%d max-heap-MiB=%d "
                              "wall-ms=%d")
                         (if ok? "PASS" "FAIL")
                         n (* 2 n findings-per-file) bytes
                         (str (:ran result))
                         (str (:introduced-count result))
                         before peak budget (max-mib) elapsed))
        (when-not ok?
          (println "  detail:" (pr-str (select-keys result
                                                    [:ran :ok :error-type
                                                     :error :cap
                                                     :observed-bytes]))))
        ok?)
      (finally (delete-tree! dir)))))

(let [results (mapv arm scales)]
  (println (format "admit-analyzer-memory-self-test: %d/%d arms passed at -Xmx%dm"
                   (count (filter true? results)) (count results) (max-mib)))
  (shutdown-agents)
  (System/exit (if (every? true? results) 0 1)))
