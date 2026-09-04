(ns clj-surgeon.mcp-test-runner
  "The JVM test lanes' entry point (TEST-ISO-001).

   Takes LANE NAMES as argv (`fast`, `integration`, `battery`) and runs
   exactly the namespaces `clj-surgeon.lane-manifest` declares for them, in
   manifest order. Namespaces are `require`d at RUNTIME rather than in this
   ns form on purpose: a static require of all 49 would load every battery
   namespace -- and every cold-launcher helper they pull in -- into the fast
   lane's JVM, which is the isolation the partition exists to buy.

   Temp-dir hygiene is unchanged (MCP-OP-TMPHYG-001..008, inb-9483a4):
   `tmp-leak-support/secure-tmpdir!` refuses a RAM-backed base and re-execs
   this suite as a child with a private `-Djava.io.tmpdir`."
  (:require
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.string :as str]
   [clojure.test :refer [run-tests]]))

(defn lane-namespaces
  "Resolves `lanes` (a seq of lane keywords) -- or, when `explicit` is
   non-empty, that explicit list of namespace symbols -- to the namespaces to
   run.

   Returns {:namespaces [syms]} on success."
  [lanes explicit]
  (if (seq explicit)
    ;; @spec TEST-ISO-001 -- a namespace with no lane declaration is a TYPED
    ;; REFUSAL naming its subject and its remedy, never a silent skip. A
    ;; skip is indistinguishable from a green suite with less in it, which is
    ;; the failure mode `mcp-formatter-test` has been living in unnoticed.
    (let [undeclared (vec (remove lm/manifest explicit))]
      (if (seq undeclared)
        {:refusal :lane-undeclared
         :namespaces undeclared
         :message (str/join "\n" (map lm/refusal-message undeclared))}
        {:namespaces (vec explicit)}))
    (let [unknown (vec (remove (set lm/lanes) lanes))]
      (if (seq unknown)
        {:refusal :unknown-lane
         :namespaces unknown
         :message (format "lane-refused: unknown lane(s) %s; known lanes are %s."
                          (str/join ", " unknown)
                          (str/join ", " (map name lm/lanes)))}
        {:namespaces (vec (mapcat lm/namespaces-for lanes))}))))

(defn lane-metadata-refusal
  "@spec TEST-ISO-001 -- after loading, every namespace must CARRY the lane
   the manifest assigned it. Checked at runtime, not only by a source scan:
   a scan reads a spelling, `the-ns` reads what the JVM actually loaded, and
   only the second can catch a file whose ns form the reader took differently
   than the scan did. Returns nil when every namespace agrees."
  [namespaces]
  (let [wrong (vec (keep (fn [n]
                           (let [want (lm/lane-of n)
                                 got (:lane (meta (find-ns n)))]
                             (when (not= want got)
                               (format "%s declares :lane %s but the manifest assigns %s"
                                       n (pr-str got) (pr-str want)))))
                         namespaces))]
    (when (seq wrong)
      {:refusal :lane-metadata-mismatch
       :namespaces wrong
       :message (str "lane-refused: " (count wrong)
                     " namespace(s) whose own ns metadata disagrees with "
                     "clj-surgeon.lane-manifest (TEST-ISO-001):\n  "
                     (str/join "\n  " wrong))})))

(defn- parse-lanes
  [args]
  (mapv (comp keyword str/lower-case str) args))

(defn -main
  [& args]
  (let [{:keys [refused root]}
        (tmp-leak/secure-tmpdir! {:main-ns "clj-surgeon.mcp-test-runner"} args)
        _ (when refused (System/exit 97))
        lanes (parse-lanes args)
        _ (when (empty? lanes)
            (binding [*out* *err*]
              (println (str "lane-refused: no lane named. Usage: -m "
                            "clj-surgeon.mcp-test-runner <lane>... where lane is "
                            (str/join ", " (map name lm/lanes)) ".")))
            (System/exit 96))
        {:keys [refusal message namespaces]} (lane-namespaces lanes nil)
        _ (when refusal
            (binding [*out* *err*] (println message))
            (System/exit 96))
        _ (println (format "lanes: %s -- %d namespace(s)"
                           (str/join "+" (map name lanes)) (count namespaces)))
        _ (doseq [n namespaces] (require n))
        _ (when-let [{:keys [message]} (lane-metadata-refusal namespaces)]
            (binding [*out* *err*] (println message))
            (System/exit 96))
        tmp-root root
        tmp-before (tmp-leak/tmp-entries)
        result (apply run-tests namespaces)
        leak-fail (tmp-leak/report-and-sweep-leak! tmp-root tmp-before)]
    (System/exit (+ (:fail result) (:error result) leak-fail))))
