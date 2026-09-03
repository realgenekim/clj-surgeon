(ns clj-surgeon.scope-stream
  "A bounded, streaming walk over a scope: one file at a time, nothing retained.

   The reader hands each admitted file's source to a planner callback and drops
   it the moment the callback returns. No repository-wide vector of sources, no
   repository-wide map of paths, no lazy sequence whose head can retain the
   walker: heap is bounded by one file plus whatever the caller chooses to keep.

   Every ceiling here is an ADMISSION CONTRACT, not a wall. Work is admitted
   exactly through the limit and the next unit is refused BEFORE the read that
   would breach it, with a `next_call` that narrows the scope. Aggregate bytes
   are counted from bytes actually read, never from directory entries alone, so
   a file that grows during the walk is stopped against the remaining budget.

   One thing IS retained for the whole stream: the sorted list of discovered
   relative paths. It is charged to the work budget rather than quietly
   excluded from it - a scope of many small files costs more in that list than
   in any one parser reservation - and a path list that does not fit the budget
   is refused before the first source is read.

   Adopted by no verb yet. This is the kernel; adoption is a separate build."
  (:require
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.txn-journal :as journal]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io ByteArrayOutputStream)
   (java.nio.file FileVisitOption FileVisitResult Files LinkOption Path SimpleFileVisitor)
   (java.nio.file.attribute BasicFileAttributes)))

(def default-limits
  "Admission ceilings a request may lower. None may be raised past `hard-limits`.

   `parse-factor` is measured, not guessed: a rewrite-clj node tree cost 54.6
   heap bytes per source byte on this repository's corpus on 2026-09-03
   (docs/observations/2026-09-03-memory-design-sol-answer-2.md reports about 45
   on its own corpus). 56 is the rounded-up figure the accountant reserves with."
  {:max-walk-entries 200000
   :max-depth 40
   :max-file-bytes (* 2 1024 1024)
   :max-aggregate-bytes (* 512 1024 1024)
   :max-receipt-records 1000
   :max-receipt-bytes (* 64 1024)
   :work-budget-bytes (* 192 1024 1024)
   :parse-factor 56})

(def hard-limits
  {:max-walk-entries 2000000
   :max-depth 64
   :max-file-bytes (* 16 1024 1024)
   :max-aggregate-bytes (* 8 1024 1024 1024)
   :max-receipt-records 10000
   :max-receipt-bytes (* 512 1024)
   :work-budget-bytes (* 1024 1024 1024)
   :parse-factor 512})

(def path-entry-overhead-bytes
  "Heap one retained relative path costs, apart from its characters.

   A `String` object header plus its byte-array header and length field is
   about 48 bytes on a 64-bit JVM with compressed object pointers, and the
   vector slot that holds the reference is another 8. The characters are
   counted separately at 2 bytes each, which is the worst case: a Latin-1
   compact string costs 1. Over-reserving a path list is the safe direction."
  56)

(defn- path-list-bytes
  "What the discovered-path list costs while the walk holds it.

   `collect-entries` retains and sorts every matching relative path and keeps
   the vector for the whole stream, so this is resident under every per-file
   reservation - not an alternative to it. Charging only the largest parser
   reservation, as this reader originally did, makes a scope of many small
   files look free."
  [relatives]
  (reduce (fn [total ^String relative]
            (+ total path-entry-overhead-bytes (* 2 (count relative))))
          0
          relatives))

(def default-skip-dirs
  #{".git" ".hg" ".svn" "target" "node_modules" ".cpcache" ".clj-surgeon"})

(defn- limits-for
  [request]
  (reduce-kv (fn [acc k v] (assoc acc k (min (get request k v) (get hard-limits k v))))
             {}
             default-limits))

(defn- refusal
  [error-type message extra]
  (merge {:ok false
          :error-type error-type
          :error message
          :complete false
          :source_unchanged true}
         extra))

;; ------------------------------------------------------------ bounded read

(defn- read-bounded
  "Read at most `cap` bytes, plus one, so an overrun is observable.

   Returns {:bytes n :truncated? b :source s}. The extra byte is what makes the
   difference between 'exactly at the ceiling' and 'one past it' a fact rather
   than an inference from the directory entry."
  [^Path path cap]
  (let [buffer (byte-array 65536)
        out (ByteArrayOutputStream.)]
    (with-open [in (Files/newInputStream path (make-array java.nio.file.OpenOption 0))]
      (loop [total 0]
        (if (> total cap)
          {:bytes total :truncated? true :source nil}
          (let [read (.read in buffer)]
            (if (neg? read)
              {:bytes total :truncated? false
               :source (String. (.toByteArray out) "UTF-8")}
              (do (.write out buffer 0 read)
                  (recur (+ total read))))))))))

;; ------------------------------------------------------------------- walk

(defn- collect-entries
  "Walk the tree once, counting every visited entry, and return the admitted
   relative paths in sorted order or a typed refusal.

   Only paths are held here - never source, never attributes - and the walk
   terminates on the first entry that breaches a bound rather than dropping it
   silently, because a dropped file leaves the found count lying."
  [^Path root {:keys [max-walk-entries max-depth skip-dirs extensions]}]
  (let [entries (volatile! 0)
        found (volatile! (transient []))
        refused (volatile! nil)
        depth-of (fn [^Path p] (.getNameCount (.relativize root p)))
        visitor
        (proxy [SimpleFileVisitor] []
          (preVisitDirectory [dir attrs]
            (vswap! entries inc)
            (cond
              (> @entries max-walk-entries)
              (do (vreset! refused
                           (refusal :scope-walk-entries-exceeded
                                    (str "The walk reached its ceiling of " max-walk-entries
                                         " visited entries at " (str dir))
                                    {:path (str dir)
                                     :max-entries max-walk-entries
                                     :observed-at-least @entries
                                     :next_call {:op :scope/stream
                                                 :scope {:narrow-to "a subtree with fewer entries"}}}))
                  FileVisitResult/TERMINATE)

              (and (not= dir root) (contains? skip-dirs (str (.getFileName dir))))
              FileVisitResult/SKIP_SUBTREE

              (> (depth-of dir) max-depth)
              (do (vreset! refused
                           (refusal :scope-too-deep
                                    (str (str dir) " is " (depth-of dir)
                                         " path segments below the root, past the "
                                         max-depth "-segment bound this walk admits")
                                    {:path (str dir) :depth (depth-of dir)
                                     :max-depth max-depth
                                     :next_call {:op :scope/stream
                                                 :scope {:narrow-to "a shallower root"}}
                                     :remedy "The bound refuses rather than truncates, so no file silently leaves the found count."}))
                  FileVisitResult/TERMINATE)

              :else FileVisitResult/CONTINUE))

          (visitFile [^Path file ^BasicFileAttributes attrs]
            (vswap! entries inc)
            (cond
              (> @entries max-walk-entries)
              (do (vreset! refused
                           (refusal :scope-walk-entries-exceeded
                                    (str "The walk reached its ceiling of " max-walk-entries
                                         " visited entries at " (str file))
                                    {:path (str file)
                                     :max-entries max-walk-entries
                                     :observed-at-least @entries
                                     :next_call {:op :scope/stream
                                                 :scope {:narrow-to "a subtree with fewer entries"}}}))
                  FileVisitResult/TERMINATE)

              (> (depth-of file) max-depth)
              (do (vreset! refused
                           (refusal :scope-too-deep
                                    (str (str file) " is " (depth-of file)
                                         " path segments below the root, past the "
                                         max-depth "-segment bound this walk admits")
                                    {:path (str file) :depth (depth-of file)
                                     :max-depth max-depth
                                     :next_call {:op :scope/stream
                                                 :scope {:narrow-to "a shallower root"}}}))
                  FileVisitResult/TERMINATE)

              :else
              (let [name (str (.getFileName file))
                    matches? (some #(str/ends-with? name %) extensions)]
                (cond
                  (not matches?) FileVisitResult/CONTINUE

                  (.isSymbolicLink attrs)
                  (do (vreset! refused
                               (refusal :scope-symlink-refused
                                        (str (str file) " is a symbolic link; this walk does not follow links")
                                        {:path (str file)
                                         :next_call {:op :scope/stream
                                                     :scope {:narrow-to "a subtree without symbolic links"}}}))
                      FileVisitResult/TERMINATE)

                  :else
                  (do (vswap! found conj! (str (.relativize root file)))
                      FileVisitResult/CONTINUE)))))

          (visitFileFailed [file _exception]
            (vswap! entries inc)
            FileVisitResult/CONTINUE))]
    (Files/walkFileTree root #{} Integer/MAX_VALUE visitor)
    (or @refused
        {:ok true
         :walk-entries @entries
         :relatives (vec (sort (persistent! @found)))})))

;; ------------------------------------------------------------------ stream

(defn stream-scope!
  ;; @spec MCP-OP-MEM-020
  ;; @spec MCP-OP-MEM-001
  "Walk `root`, hand each admitted file to `plan-fn`, and retain none of it.

   `plan-fn` receives {:relative :path :bytes :sha256 :source} and its return
   value is dropped unless `:collect` is supplied, in which case
   `(collect entry result)` produces one bounded receipt record. The reader
   itself holds one source at a time and releases it before the next read.

   Returns a bounded summary receipt, or one typed refusal naming the ceiling,
   the path it was reached at, and a `next_call` that narrows the scope."
  [root plan-fn {:keys [extensions collect] :as request}]
  (let [limits (limits-for request)
        extensions (or extensions [".clj" ".cljc" ".cljs" ".edn"])
        skip-dirs (or (:skip-dirs request) default-skip-dirs)
        real-root (mcp-paths/real-root root)
        walked (collect-entries real-root (assoc limits
                                                 :skip-dirs skip-dirs
                                                 :extensions extensions))]
    (if-not (:ok walked)
      walked
      (let [{:keys [max-file-bytes max-aggregate-bytes work-budget-bytes parse-factor
                    max-receipt-records max-receipt-bytes]} limits
            held (path-list-bytes (:relatives walked))]
        (if (> held work-budget-bytes)
          ;; The list of discovered paths is itself reserved work. Refusing it
          ;; here, before the first source is read, is the difference between
          ;; accounting a cost and bounding it.
          (refusal :scope-work-budget-exceeded
                   (str "Holding " (count (:relatives walked))
                        " discovered paths would reserve " held
                        " bytes of heap, above the " work-budget-bytes
                        "-byte work budget")
                   {:reserved-for :discovered-path-list
                    :path-list-bytes held
                    :discovered-files (count (:relatives walked))
                    :work-budget-bytes work-budget-bytes
                    :next_call {:op :scope/stream
                                :scope {:narrow-to "a subtree whose file list fits the work budget"}}
                    :remedy "Narrow the scope: the walk holds every matching path for the whole stream, so the path list alone must fit the budget."})
        (loop [remaining (:relatives walked)
               aggregate 0
               files 0
               largest 0
               reserved-peak held
               records []
               record-bytes 0]
          (if (empty? remaining)
            {:ok true
             :complete true
             :work {:walk-entries (:walk-entries walked)
                    :files-discovered (count (:relatives walked))
                    :files-read files
                    :source-bytes aggregate
                    :largest-file-bytes largest
                    :receipt-records (count records)
                    :receipt-bytes record-bytes}
             :reserved {:heap-reserved-peak-bytes reserved-peak
                        :path-list-bytes held
                        :discovered-files (count (:relatives walked))
                        :work-budget-bytes work-budget-bytes
                        :parse-factor parse-factor
                        :aggregate-bytes aggregate
                        :aggregate-bytes-max max-aggregate-bytes}
             :records records}
            (let [relative (first remaining)
                  resolved (mcp-paths/resolve-source-path real-root relative)]
              (if-not (:ok resolved)
                (refusal :scope-path-refused (:error resolved)
                         {:path relative :next_call nil})
                (let [^Path canonical (:canonical resolved)
                      ;; the cap is the smaller of the per-file ceiling and what
                      ;; the aggregate budget has left, so a file that grew
                      ;; since discovery is stopped against the REMAINING budget
                      cap (min max-file-bytes (- max-aggregate-bytes aggregate))
                      {:keys [bytes truncated? source]} (read-bounded canonical cap)]
                  (cond
                    (and truncated? (> bytes max-file-bytes))
                    (refusal :scope-source-too-large
                             (str relative " is larger than the " max-file-bytes
                                  "-byte ceiling this walk admits")
                             {:path relative :max-bytes max-file-bytes
                              :observed-at-least bytes
                              :next_call {:op :scope/stream
                                          :scope {:exclude relative}}
                              :remedy (str "Exclude " relative ", or narrow the scope. Excluding a file may change semantics; say so in the plan.")})

                    truncated?
                    (refusal :scope-aggregate-bytes-exceeded
                             (str "Reading " relative " would pass the "
                                  max-aggregate-bytes "-byte aggregate ceiling")
                             {:path relative :max-bytes max-aggregate-bytes
                              :observed-at-least (+ aggregate bytes)
                              :files-read files
                              :next_call {:op :scope/stream
                                          :scope {:narrow-to "a subtree that fits the aggregate budget"}}})

                    (> (+ held (* bytes parse-factor)) work-budget-bytes)
                    (refusal :scope-work-budget-exceeded
                             (str "Parsing " relative " would reserve "
                                  (+ held (* bytes parse-factor))
                                  " bytes of heap beside the retained path list, above the "
                                  work-budget-bytes "-byte work budget")
                             {:path relative
                              :bytes bytes
                              :parse-factor parse-factor
                              :reserved-for :file-parse
                              :path-list-bytes held
                              :work-budget-bytes work-budget-bytes
                              :next_call nil
                              :remedy "One admitted file must fit the work budget beside the discovered path list; there is no correctness-preserving way to split it here."})

                    :else
                    (let [reserved (+ held (* bytes parse-factor))
                          entry {:relative relative
                                 :path (:path resolved)
                                 :bytes bytes
                                 :sha256 (journal/sha256-string source)
                                 :source source}
                          result (plan-fn entry)
                          record (when collect (collect (dissoc entry :source) result))
                          record-size (if record (count (pr-str record)) 0)]
                      (cond
                        (and record (>= (count records) max-receipt-records))
                        (refusal :scope-receipt-too-large
                                 (str "The receipt reached its ceiling of "
                                      max-receipt-records " records at " relative)
                                 {:path relative
                                  :max-records max-receipt-records
                                  :observed-at-least (inc (count records))
                                  :next_call {:op :scope/stream
                                              :scope {:narrow-to "a scope whose receipt fits, or a summary projection"}}
                                  :remedy "A mutation receipt is refused rather than truncated: a truncated receipt hides work that was done."})

                        (and record (> (+ record-bytes record-size) max-receipt-bytes))
                        (refusal :scope-receipt-too-large
                                 (str "The receipt would reach " (+ record-bytes record-size)
                                      " bytes at " relative ", above the "
                                      max-receipt-bytes "-byte ceiling")
                                 {:path relative
                                  :max-bytes max-receipt-bytes
                                  :observed-at-least (+ record-bytes record-size)
                                  :next_call {:op :scope/stream
                                              :scope {:narrow-to "a scope whose receipt fits, or a summary projection"}}
                                  :remedy "A mutation receipt is refused rather than truncated: a truncated receipt hides work that was done."})

                        :else
                        (recur (rest remaining)
                               (+ aggregate bytes)
                               (inc files)
                               (max largest bytes)
                               (max reserved-peak reserved)
                               (if record (conj records record) records)
                               (+ record-bytes record-size)))))))))))))))
