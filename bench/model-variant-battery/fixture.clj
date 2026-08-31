(ns clj-surgeon.model-variant-battery-fixture)

(defn fill-literal [] :cold)

(defn fill-qualified-call [x] (legacy/transform x))

(defn fill-map-value [] {:status :draft :retries 2})

(defn fill-selected-arity ([] :old) ([x] x))

(defn fill-thread-tail [x] (-> x normalize persist))

(defn fill-branch-call [x] (if (nil? x) :missing (render x)))
