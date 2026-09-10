(ns clj-surgeon.memory.heap
  "Heap meters for the memory arms.

   Two numbers are reported and they mean different things. `heap-used-peak-mb`
   is the maximum of the process-wide used heap sampled on a timer: it includes
   garbage that no collector has reached yet, so it measures how close the arm
   came to the ceiling. `heap-after-gc-peak-mb` is the maximum collection usage
   published by the memory pools after a collection: it measures what the arm
   RETAINS, which is the promise a streaming reader actually makes."
  (:import
   (java.lang.management ManagementFactory MemoryPoolMXBean MemoryType)))

(def ^:private mb (* 1024 1024))

(defn- heap-used []
  (.getUsed (.getHeapMemoryUsage (ManagementFactory/getMemoryMXBean))))

(defn- heap-pools []
  (filterv (fn [^MemoryPoolMXBean pool] (= MemoryType/HEAP (.getType pool)))
           (ManagementFactory/getMemoryPoolMXBeans)))

(defn- after-gc-used []
  (reduce (fn [acc ^MemoryPoolMXBean pool]
            (if-let [usage (.getCollectionUsage pool)]
              (+ acc (.getUsed usage))
              acc))
          0
          (heap-pools)))

(defn- to-mb [bytes] (double (/ (double bytes) mb)))

(defn measure
  "Run `body-fn` with a sampler attached and return {:result r :memory m}."
  [body-fn]
  (let [start (heap-used)
        peak (atom start)
        after-gc-peak (atom 0)
        running (atom true)
        sampler (doto (Thread.
                        (fn []
                          (while @running
                            (swap! peak max (heap-used))
                            (swap! after-gc-peak max (after-gc-used))
                            (Thread/sleep 10)))
                        "memory-heap-sampler")
                  (.setDaemon true)
                  (.start))]
    (try
      (let [t0 (System/nanoTime)
            result (body-fn)
            wall-ms (quot (- (System/nanoTime) t0) 1000000)]
        (reset! running false)
        (.join sampler 1000)
        (System/gc)
        (Thread/sleep 50)
        {:result result
         :memory {:xmx-mb (to-mb (.maxMemory (Runtime/getRuntime)))
                  :heap-used-start-mb (to-mb start)
                  :heap-used-peak-mb (to-mb @peak)
                  :heap-used-end-mb (to-mb (heap-used))
                  :heap-after-gc-peak-mb (to-mb @after-gc-peak)
                  :wall-ms wall-ms}})
      (finally
        (reset! running false)))))

(defn emit-receipt!
  "Print one machine-readable receipt line the parent test parses."
  [receipt]
  (println (str "#MEMORY-RECEIPT " (pr-str receipt)))
  (flush))
