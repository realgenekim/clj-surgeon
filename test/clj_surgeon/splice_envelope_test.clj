(ns clj-surgeon.splice-envelope-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms]
   [clj-surgeon.insert-forms-support]
   [clj-surgeon.rename-alias]
   [clj-surgeon.rename-alias-test]
   [clojure.edn]
   [clojure.java.io]
   [clojure.string]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-011
;; INTENT-TEST: INSERT-FORMS-011

;; @spec INSERT-FORMS-015
;; INTENT-TEST: INSERT-FORMS-015

(def probe-owners
  ["probe.clj" "mcp_hot_verify.clj" "mcp_http_server.clj"])

(defn refusal-spellings [forms]
  (letfn [(literals [value]
            (filter #(and (keyword? %) (not (#{:error-type :kind} %)))
                    (tree-seq coll? seq value)))]
    (set
      (mapcat
        (fn [node]
          (cond
            (map? node) (mapcat #(literals (get node %)) [:error-type :kind])
            (seq? node)
            (concat
              ;; Includes ((requiring-resolve 'probe/refusal) :literal ...).
              (when (some #(and (symbol? %) (= "refusal" (name %)))
                          (tree-seq coll? seq (first node)))
                (literals (second node)))
              (mapcat (fn [[k v]] (when (#{:error-type :kind} k) (literals v)))
                      (partition 2 1 node)))))
        (tree-seq coll? seq forms)))))

(defn probe-vocabulary []
  (into {}
        (for [file probe-owners
              :let [forms (binding [*read-eval* false]
                            (read-string (str "[" (slurp (str "src/clj_surgeon/" file)) "]")))]]
          [file (refusal-spellings forms)])))

(defn remedy-vocabulary [verb]
  ;; Read code as data, never eval; case constants include grouped clauses.
  (if (= verb "probe")
    (set (mapcat val (probe-vocabulary)))
    (let [path (str "src/clj_surgeon/" (clojure.string/replace verb "-" "_") "_plan.clj")
          forms (binding [*read-eval* false] (read-string (str "[" (slurp path) "]")))
          remedy (first (filter #(and (seq? %) (= 'defn (first %)) (= 'remedy (second %))) forms))
          dispatch (first (filter #(and (seq? %) (= 'case (first %))) (tree-seq coll? seq remedy)))]
      (set (mapcat #(if (seq? %) % [%]) (take-nth 2 (butlast (drop 2 dispatch))))))))

(deftest bounded-input-path-encoding
  (testing "Strict decoding is shared; both disk entrances retain their stage"
    (clj-surgeon.insert-forms-support/with-file clj-surgeon.rename-alias-test/header
      (fn [dir file insertion]
        (java.nio.file.Files/write (.toPath file) (byte-array [(unchecked-byte 255)])
          (make-array java.nio.file.OpenOption 0))
        (let [rename (assoc (clj-surgeon.rename-alias-test/request
                              {clj-surgeon.rename-alias-test/file clj-surgeon.rename-alias-test/header} 0)
                            :workspace_root (.getCanonicalPath dir))]
          (doseq [[execute request] [[clj-surgeon.insert-forms/execute! insertion]
                                     [clj-surgeon.rename-alias/execute! rename]]]
            (is (= [:malformed-utf8 [-1]]
                   [(:error-type (execute request))
                    (vec (java.nio.file.Files/readAllBytes (.toPath file)))])))))))
  (doseq [verb ["insert-forms" "rename-alias" "probe"]]
    (let [registry (clojure.edn/read-string (slurp (str "docs/intent/" verb "/refusals.edn")))
          rows (:refusals registry)
          owner-kinds (:owner-refusals registry)
          registered (into (set (map :type rows)) (mapcat val owner-kinds))]
      (when-not (= verb "probe")
        (is (= {:unsupported-source [:capability :none]
                :malformed-utf8 [:semantic "Lossy decoding can replace malformed UTF-8 bytes before an edit."]}
              (into {} (for [{:keys [type class native_failure]} rows
                             :when (#{:unsupported-source :malformed-utf8} type)]
                         [type [class native_failure]]))) verb))
      (is (= (remedy-vocabulary verb) registered) (str verb " refusal vocabulary must be complete"))
      (when (= verb "probe")
        (doseq [[file kinds] (probe-vocabulary)]
          (is (seq kinds) (str file " must contribute refusal kinds"))
          (is (= #{} (set (remove registered kinds)))
              (str file " emits unregistered refusal kinds"))
          (is (= #{} (set (remove kinds (get owner-kinds file))))
              (str file " registers owner refusal kinds it never emits"))))
      (is (and (seq rows) (= (count rows) (count (set (map :type rows))))
               (every? (fn [row]
                         (and (every? #(contains? row %) [:promise :native_failure :native_method
                                                          :minimal_reproducer :class :existing_check
                                                          :owner :witness :retirement_condition])
                              (or (not= :none (:native_failure row))
                                  (#{:capability :protocol :resource} (:class row))))) rows))
          verb)))
  (testing "insert-forms-reader-safety-and-limits"
    (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source
      (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :extra (reduce (fn [x _] [x]) nil (range 600)))
      :limit-exceeded)

    (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
      (fn [_ file req]
        (let [text (str "(def x \"" (char 0xd800) "\")")
              result (clj-surgeon.insert-forms/execute! (assoc req :payload {:text text :forms 1}))]
          (is (= :invalid-request (:error-type result)))
          (is (= clj-surgeon.insert-forms-support/source (slurp file))))))
    (let [s (str (apply str (repeat 50001 "x ")) clj-surgeon.insert-forms-support/source)]
      (clj-surgeon.insert-forms-support/refused s (clj-surgeon.insert-forms-support/request s) :limit-exceeded))
    (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload
                                                                                        {:text (str (apply str (repeat 513 "'")) "x") :forms 1}) :limit-exceeded)
    (let [s (str (apply str (repeat 32768 " ")) "(defn a [] 1)")
          req (assoc (clj-surgeon.insert-forms-support/request s) :payload {:text (apply str (repeat 1000 "1\n")) :forms 1000})
          result (clj-surgeon.insert-forms/plan s req)]
      (is (= :limit-exceeded (:error-type result)))
      (is (= true (:source_unchanged result))))
    (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
      (fn [_ file req]
        (let [huge (assoc-in req [:anchor :owner :name] (apply str (repeat 2097153 "a")))
              result (clj-surgeon.insert-forms/execute! huge)]
          (is (= :limit-exceeded (:error-type result)))
          (is (= clj-surgeon.insert-forms-support/source (slurp file))))))
    (is (= :invalid-request (:error-type (clj-surgeon.insert-forms/read-request "{:value #inst \"2026-01-01\"}"))))
    (doseq [s ["#=(throw (Exception.)) (defn a [] 1)" "#?(:clj (defn a [] 1))"
               "\ufeff(defn a [] 1)"]]
      (clj-surgeon.insert-forms-support/refused s (clj-surgeon.insert-forms-support/request s) :unsupported-source))
    (doseq [text ["#=(throw (Exception.))" "#?(:clj 1)"]]
      (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload {:text text :forms 1})
        :unsupported-payload-syntax))
    (doseq [text [(apply str (repeat 1048577 "x"))
                  (str (apply str (repeat 513 "(")) "0" (apply str (repeat 513 ")")))]]
      (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload {:text text :forms 1})
        :limit-exceeded))
    (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc-in (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) [:payload :forms] 1001) :limit-exceeded)
    (let [s (apply str (repeat 8388609 " "))]
      (clj-surgeon.insert-forms-support/refused s (clj-surgeon.insert-forms-support/request s) :limit-exceeded)))
  (testing "insert-forms-path-confinement"

    (doseq [path ["../escape.clj" "/absolute.clj" "missing.clj" "src/example.cljs" "src/\u0000.clj"]]
      (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
        (fn [_ file req]
          (let [r (clj-surgeon.insert-forms/execute! (assoc req :file path))]
            (is (= (if (= path "src/example.cljs") :unsupported-source :invalid-path) (:error-type r)))
            (is (= clj-surgeon.insert-forms-support/source (slurp file)))))))
    (doseq [link-type [:symbolic :hard]]
      (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
        (fn [dir file req]
          (let [link (.toPath (clojure.java.io/file dir "alias.clj"))]
            (if (= :hard link-type)
              (Files/createLink link (.toPath file))
              (Files/createSymbolicLink link (.toPath file) (make-array java.nio.file.attribute.FileAttribute 0)))
            (is (= :invalid-path (:error-type (clj-surgeon.insert-forms/execute! (assoc req :file "alias.clj")))))
            (is (= clj-surgeon.insert-forms-support/source (slurp file)))))))))

(deftest exactly-one-edn-request
  (testing "Red-team servlet spelling and refusal literal families"
    (doseq [form ['(refusal :direct "no")
                  '(probe/refusal :direct "no")
                  '((requiring-resolve 'clj-surgeon.probe/refusal) :direct "no")
                  '(emit :kind :direct)
                  '(assoc {} :error-type :direct)
                  '(hash-map :error-type :direct)
                  '{:kind :direct}
                  '{:error-type :direct}
                  '(refusal (or (:error-type data) :direct) "no")]]
      (doseq [owner [form (list 'def 'guard form)
                     (list 'defn- 'probe-guard '[request] form)
                     (list 'do form)
                     (list 'defmethod 'guard :request '[request] form)]]
        (is (= #{:direct} (refusal-spellings [owner])) (pr-str owner)))))
  (doseq [[text expected] [["{}" {}] ["{} ; final comment" {}]
                           ["{} {}" :invalid-request] ["{:a 1 :a 2}" :invalid-request]
                           ["#foo {}" :invalid-request] ["#=(throw (Exception.))" :invalid-request]
                           ["{" :invalid-request]]]
    (let [r (clj-surgeon.insert-forms/read-request text)]
      (is (= expected (or (:error-type r) r))))))
