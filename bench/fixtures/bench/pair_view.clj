(ns bench.pair-view)

(defn route-event [state event]
  (let [event-type (:type event)
        payload (:payload event)]
    (case event-type
      :start
      (assoc state :status :running :started-at (:at event))

      :pause
      (assoc state :status :paused :reason (:reason payload))

      :resume
      (assoc state :status :running :resumed-at (:at event))

      :finish
      ;; Completion intentionally keeps the audit payload beside the status.
      (assoc state :status :done :audit (:audit payload))

      :cancel
      (assoc state :status :cancelled :reason (:reason payload))

      :retry
      (update state :attempts (fnil inc 0))

      :archive
      (assoc state :archived? true :archived-at (:at event))

      :restore
      (dissoc state :archived? :archived-at)

      (assoc state :last-unknown-event event-type))))

(defn classify-request [request]
  (let [actor (:actor request)
        resource (:resource request)]
    (cond
      (nil? actor)
      {:decision :deny :reason :missing-actor}

      (:suspended? actor)
      {:decision :deny :reason :suspended}

      (and (:admin? actor) (:sensitive? resource))
      {:decision :allow :reason :admin}

      (:public? resource)
      {:decision :allow :reason :public}

      (contains? (:owners resource) (:id actor))
      {:decision :allow :reason :owner}

      (seq (:delegations actor))
      (cond
        (contains? (:delegations actor) (:id resource))
        {:decision :allow :reason :delegated}

        :else
        {:decision :deny :reason :wrong-delegation})

      :else
      {:decision :deny :reason :no-policy})))

(defn prepare-request [request defaults cache]
  (let [request-id (:id request)
        actor-id (get-in request [:actor :id])
        resource-id (get-in request [:resource :id])
        cache-key [actor-id resource-id]
        cached-decision (get cache cache-key)
        timeout-ms (or (:timeout-ms request) (:timeout-ms defaults))
        retry-limit (or (:retry-limit request) (:retry-limit defaults))
        audit-context {:request-id request-id
                       :actor-id actor-id
                       :resource-id resource-id}]
    {:request-id request-id
     :actor-id actor-id
     :resource-id resource-id
     :cache-key cache-key
     :cached-decision cached-decision
     :timeout-ms timeout-ms
     :retry-limit retry-limit
     :audit-context audit-context}))

(def normalize-record
  #(select-keys % [:type :line :end-line]))

(defn route-two-dimensions [mode event]
  [(case mode
     :online
     (case event
       :start :run
       :stop :halt)

     :offline
     :queue)

   (case event
     :start
     (case mode
       :online :immediate
       :offline :deferred)

     :stop
     :halt)])
