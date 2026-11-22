(require '[clojure.string :as str])
(defn forms [path] (let [s (slurp path)] (loop [r (java.io.PushbackReader. (java.io.StringReader. s)) acc []]
  (let [f (try (read {:read-cond :allow :eof ::eof} r) (catch Exception e ::eof))]
    (if (= ::eof f) acc (recur r (conj acc f)))))))
(defn named [path nm] (first (filter #(and (seq? %) (= nm (second %))) (forms path))))
(defn var-name [h] (if (and (seq? h) (= 'var (first h))) (str (second h)) (str h)))
(defn routes->lines [v] (vec (for [[path m] v, [method {h :handler}] (sort-by str m)] (str (name method) " " path " " (var-name h)))))
(def cfp "/home/forge/src/curtaincall-cfp/src/cfp_scheduler_killer/")
(def mvr "/home/forge/src/marvin-voice-remote/src/marvin_voice_remote/")
;; I1 make-routes: (defn make-routes [] (with-additive [ ... ]))
(def mr (named (str cfp "server.clj") 'make-routes))
(def mr-vec (second (nth mr 3)))
(def i1 (routes->lines mr-vec))
;; I2 routes def
(def ra (named (str cfp "routes_additive.clj") 'routes))
(def i2 (routes->lines (nth ra 2)))
;; I3 step case pairs
(def step (named (str mvr "reducer/core.clj") 'step))
(def case-form (first (filter #(and (seq? %) (= 'case (first %))) (tree-seq coll? seq step))))
(def i3 (vec (for [[k v] (partition 2 (drop 2 case-form)) :when (keyword? k)] (str k " " v))))
;; I4 handle-rough-place case: ex-data type -> status
(def hrp (named (str cfp "handlers/schedule.clj") 'handle-rough-place))
(def hrp-case (first (filter #(and (seq? %) (= 'case (first %)) (= '(:type (ex-data e)) (second %))) (tree-seq coll? seq hrp))))
(def i4 (vec (for [[k v] (partition 2 (drop 2 hrp-case)) :when (keyword? k)] (str k " " (:status v)))))
(doseq [[nm v] [["i1-make-routes" i1] ["i2-routes-additive" i2] ["i3-step-case" i3] ["i4-rough-place-case" i4]]]
  (spit (str "/var/tmp/forge/claude-1011/-home-forge-src-worktrees-clj-surgeon-seat/cca23770-0634-4a43-90f2-9abfa918f2b7/scratchpad/tasks/expected-" nm ".txt") (str/join "\n" v))
  (println nm (count v) "distinct" (count (set v))) (println "  " (first v)) (println "  " (last v)))
(println "case default (last odd element):" (last case-form) "| hrp default:" (last hrp-case))
(println "E4 /people/outreach:" (filter #(str/includes? % "/people/outreach") i1))
(println "E1 stale-command body:" (:body (second (first (filter (fn [[k v]] (= k :store/stale-command)) (partition 2 (drop 2 hrp-case)))))))
(println "E2 latest-reply:" (filter #(str/includes? % "latest-reply") i3))
