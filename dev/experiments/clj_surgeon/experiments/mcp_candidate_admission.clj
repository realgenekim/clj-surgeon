(ns clj-surgeon.experiments.mcp-candidate-admission)

(defn- public-key [value]
  (if (keyword? value) (name value) (str value)))

(defn- present? [param-keys field]
  (contains? param-keys (public-key field)))

(declare matches-constraint?)

(defn- matches-composite? [param-keys constraint]
  (and
    (or (not (:required constraint))
        (every? #(present? param-keys %) (:required constraint)))
    (or (not (:allOf constraint))
        (every? #(matches-constraint? param-keys %) (:allOf constraint)))
    (or (not (:anyOf constraint))
        (some #(matches-constraint? param-keys %) (:anyOf constraint)))
    (or (not (:oneOf constraint))
        (= 1 (count (filter #(matches-constraint? param-keys %)
                            (:oneOf constraint)))))
    (or (not (:not constraint))
        (not (matches-constraint? param-keys (:not constraint))))))

(defn- matches-constraint? [param-keys constraint]
  (matches-composite? param-keys constraint))

(defn authorize
  "Authorize top-level request shape against one advertised public schema.

  The shared kernel remains responsible for value and nested validation. This
  membrane makes the public property set and branch selector executable before
  any shared handler can observe the request."
  [schema params]
  (let [param-keys (set (map public-key (keys params)))
        property-keys (set (map public-key (keys (:properties schema))))
        unexpected (vec (sort (remove property-keys param-keys)))
        branches (or (:oneOf schema) [schema])
        matching-branches (count (filter #(matches-constraint? param-keys %)
                                         branches))]
    (if (and (empty? unexpected) (= 1 matching-branches))
      {:ok true}
      {:ok false
       :error "The invoked public tool does not authorize this request shape"
       :error-type :public-schema-denied
       :unexpected-fields unexpected
       :matching-branches matching-branches
       :mutation-attempted false
       :write-authority false})))
