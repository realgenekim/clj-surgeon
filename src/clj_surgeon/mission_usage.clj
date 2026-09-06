(ns clj-surgeon.mission-usage
  "Bounded accounting from observed transport completion, never planned routes.")

(defn- nonnegative-finite? [x]
  (and (number? x) (not (neg? x)) (Double/isFinite (double x))))

(defn- token? [x]
  (and (integer? x) (<= 0 x Long/MAX_VALUE)))

(defn- metric [attempts key valid? convert]
  (let [values (keep (fn [attempt]
                       (let [v (get attempt key)]
                         (when (valid? attempt v) (convert v)))) attempts)]
    {:known-total (when (seq values) (reduce +' values))
     :unknown-attempts (- (count attempts) (count values))}))

(defn- records [candidate]
  (if (contains? candidate :attempts)
    (:attempts candidate)
    [(if (and (not (contains? candidate :request_started))
              (nonnegative-finite? (:request_wall_s candidate)))
       (assoc candidate :request_started true)
       candidate)]))

(defn summarize
  "At most five candidates and two attempts each. Reasoning is a subset of
  completion, reported separately. Unknown candidates have unknown attempt counts."
  [closed]
  (try
    (let [completed (:completed closed)
          cancelled (:cancelled closed)]
      (if-not (and (map? closed) (vector? completed) (<= (count completed) 5)
                   (every? map? completed) (vector? cancelled) (<= (count cancelled) 5)
                   (every? #(and (integer? %) (<= 0 % 4)) cancelled))
        {:status :unavailable :reason :invalid-completion-snapshot}
        (let [indices (mapv #(get %1 :index %2) completed (range))
              groups (mapv records completed)]
          (if-not (and (every? #(and (integer? %) (<= 0 % 4)) indices)
                       (= (count indices) (count (set indices)))
                       (every? #(and (vector? %) (<= 1 (count %) 2) (every? map? %)) groups))
            {:status :unavailable :reason :invalid-attempt-records}
            (let [cancelled (remove (set indices) (set cancelled))
                  attempts (vec (filter #(true? (:request_started %)) (mapcat identity groups)))
                  nonstarted (count (filter #(false? (:request_started %)) (mapcat identity groups)))
                  unknown (+ (count cancelled)
                             (count (filter #(some (fn [a] (not (boolean? (:request_started a)))) %) groups)))
                  metrics {:prompt-tokens (metric attempts :prompt_tokens (fn [_ v] (token? v)) identity)
                           :completion-tokens (metric attempts :completion_tokens (fn [_ v] (token? v)) identity)
                           :reasoning-tokens (metric attempts :reasoning_tokens (fn [_ v] (token? v)) identity)
                           :cost-usd (metric attempts :cost_usd
                                             (fn [a v] (and (= "provider-reported" (:cost_source a))
                                                            (nonnegative-finite? v))) bigdec)}]
              (merge metrics
                     {:status (if (and (true? (:terminated? closed)) (zero? unknown)
                                       (every? #(zero? (:unknown-attempts %)) (vals metrics)))
                                :complete :partial)
                      :completed-candidates (count completed)
                      :cancelled-candidates (count cancelled)
                      :unknown-usage-candidates unknown
                      :dispatched-attempts (count attempts)
                      :nonstarted-attempts nonstarted}))))))
    (catch Exception _ {:status :unavailable :reason :invalid-completion-snapshot})))
