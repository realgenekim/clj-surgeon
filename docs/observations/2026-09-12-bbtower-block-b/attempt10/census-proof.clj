(require '[clojure.edn :as edn]
         '[clojure.java.shell :as sh]
         '[clojure.set :as set]
         '[clojure.string :as str])

(let [git (fn [& args]
            (let [r (apply sh/sh "git" args)]
              (assert (zero? (:exit r)) (:err r))
              (:out r)))
      files (str/split-lines (git "ls-files" "test/*_test.clj" "test/**/*_test.clj"))
      names (fn [source]
              (set (map second (re-seq #"(?m)^\(deftest\s+([^\s()\[\]{}]+)" source))))
      changes (vec (for [f files
                        :let [before (names (git "show" (str "dcd6d26b:" f)))
                              after (names (slurp f))]
                        :when (not= before after)]
                    {:file f :removed (set/difference before after)
                     :added (set/difference after before)}))
      path "test/clj_surgeon/deftest_census.edn"
      before (edn/read-string (git "show" (str "dcd6d26b:" path)))
      after (edn/read-string (slurp path))]
  (assert (empty? changes) (pr-str changes))
  (assert (empty? (set/difference before after)))
  (prn {:test-files (count files) :test-name-changes changes
        :census-before (count before) :census-after (count after)
        :existing-tests-adopted (count (set/difference after before))
        :census-removed []}))
