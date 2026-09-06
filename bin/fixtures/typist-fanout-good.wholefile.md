FILE: src/fixture/util.clj
```clojure
(ns fixture.util
  "Path helpers shared by the scope expander."
  (:require [clojure.string :as str]))

(defn normalize-path
  "Trim a path and drop any trailing slash.

  With `{:strict? true}` also collapse repeated inner slashes."
  ([p] (normalize-path p nil))
  ([p opts]
   (let [t (str/replace (str/trim p) #"/+$" "")]
     (if (:strict? opts)
       (str/replace t #"/{2,}" "/")
       t))))
```

FILE: src/fixture/scope.clj
```clojure
(ns fixture.scope
  "Scope expansion for the helper-extraction fence."
  (:require [fixture.util :as util]))

(def default-roots
  "The roots this workspace admits when the caller names none."
  ["src"])

(defn scope-roots
  "The admitted roots for this scope expansion."
  []
  default-roots)

(defn expand
  "Every admitted root plus the caller's paths, normalised, in order."
  ([paths] (expand paths nil))
  ([paths opts]
   (vec (distinct (map #(util/normalize-path % opts)
                       (concat (scope-roots) paths))))))

(defn admits?
  "Does this scope admit the given path?"
  [p]
  (contains? (set (expand [])) (util/normalize-path p)))
```

FILE: src/fixture/paths.clj
```clojure
(ns fixture.paths
  "Path assembly built on the shared helpers."
  (:require [clojure.string :as str]
            [fixture.util :as util]))

(def op-name
  "The operation name reported in telemetry."
  "normalize")

(defn join
  "Join segments into one path.

  Example:
    (normalize \"a/b/\")  ;=> \"a/b\""
  ([segs] (join segs nil))
  ([segs opts]
   (util/normalize-path (str/join "/" segs) opts)))

(defn relative-to
  "Strip the root prefix from a path."
  [root p]
  (let [r (util/normalize-path root)
        n (util/normalize-path p)]
    (if (str/starts-with? n (str r "/"))
      (subs n (inc (count r)))
      n)))

(defn clean-all
  "Normalise every path in the collection."
  [paths]
  (mapv util/normalize-path paths))
```
