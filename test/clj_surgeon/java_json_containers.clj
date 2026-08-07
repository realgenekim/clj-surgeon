(ns clj-surgeon.java-json-containers)

(defn convert
  "Recursively copy Clojure JSON shapes into the Java SDK container types."
  [value]
  (cond
    (map? value)
    (let [result (java.util.LinkedHashMap.)]
      (doseq [[key child] value]
        (.put result key (convert child)))
      result)

    (vector? value)
    (java.util.ArrayList. (map convert value))

    :else value))
