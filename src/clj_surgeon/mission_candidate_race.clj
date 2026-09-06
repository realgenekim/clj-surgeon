(ns clj-surgeon.mission-candidate-race
  "Owned, bounded candidate requests; completion order never implies quality.

  request-one! must itself bound and clean its provider subprocess. Thread
  termination alone is not proof of child-process cleanup. close! must report
  terminated? true before the caller may commit; false means hold, not success."
  (:refer-clojure :exclude [next!])
  (:import
   (java.util.concurrent Callable CancellationException ExecutionException ExecutorCompletionService Executors Future TimeUnit)))

(defn- refused [index kind]
  {:index index :usable false :error_type kind})

(defn start!
  "Start exactly k (1..5) owned requests. No shared Clojure future pool."
  [k request-one!]
  (when-not (and (integer? k) (<= 1 k 5) (ifn? request-one!))
    (throw (ex-info "Invalid candidate race configuration" {:error_type "candidate-race-config"})))
  (let [executor (Executors/newFixedThreadPool k)
        queue (ExecutorCompletionService. executor)
        completed (atom [])
        closed? (atom false)
        consumed (atom 0)
        futures (mapv
                  (fn [index]
                    (.submit queue
                      ^Callable
                      (reify Callable
                        (call [_]
                          (let [candidate
                                (try
                                  (let [value (request-one! index)]
                                    (if (map? value)
                                      (assoc value :index index)
                                      (refused index "candidate-response-invalid")))
                                  (catch InterruptedException _
                                    (.interrupt (Thread/currentThread))
                                    (refused index "candidate-request-interrupted"))
                                  (catch Throwable _
                                    (refused index "candidate-request-failed")))]
                            (swap! completed conj candidate)
                            candidate)))))
                  (range k))]
    {:executor executor :queue queue :futures futures :completed completed
     :closed? closed? :consumed consumed :k k :close-result (atom nil)}))

(defn next!
  "Return the next completed candidate, nil after exhaustion/close.

  Exactly one caller consumes this handle; candidate indexes retain submission
  identity. Short queue polls permit a concurrent close to release a reader."
  [{:keys [queue futures consumed closed? k]}]
  (loop []
    (when (and (not @closed?) (< @consumed k))
      (if-let [^Future future (.poll ^ExecutorCompletionService queue 100 TimeUnit/MILLISECONDS)]
        (let [index (.indexOf ^java.util.List futures future)]
          (swap! consumed inc)
          (try
            (.get future)
            (catch CancellationException _ (refused index "candidate-request-cancelled"))
            (catch ExecutionException _ (refused index "candidate-request-failed"))))
        (recur)))))

(defn close!
  "Cancel unfinished requests, wait at most five seconds, retain all responses.

  cancelled is cancellation accepted by Future, not proof an external child
  died. A cooperatively interrupted request can occur in both lists. If workers
  ignore interruption, terminated? is false and completed is only this snapshot.
  Repeated close calls can observe later termination/completion."
  [{:keys [executor futures completed closed? close-result]}]
  (locking close-result
    (reset! closed? true)
    (let [cancelled (or (:cancelled @close-result)
                        (vec (keep-indexed
                               (fn [index ^Future f]
                                 (when (and (not (.isDone f)) (.cancel f true)) index))
                               futures)))]
      (.shutdownNow ^java.util.concurrent.ExecutorService executor)
      (let [terminated? (try
                          (.awaitTermination ^java.util.concurrent.ExecutorService executor 5 TimeUnit/SECONDS)
                          (catch InterruptedException _
                            (.interrupt (Thread/currentThread))
                            (.isTerminated ^java.util.concurrent.ExecutorService executor)))
            result {:terminated? terminated? :completed @completed :cancelled cancelled}]
        (reset! close-result result)
        result))))
