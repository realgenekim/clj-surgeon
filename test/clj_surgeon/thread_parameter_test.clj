(ns clj-surgeon.thread-parameter-test
  "Pure witnesses for Astra's thread-parameter prototype.

  The canonical POST comes from thread-parameter-fixture, never from the
  planner. No witness performs I/O."
  {:lane :excluded}
  (:require
   [clj-surgeon.thread-parameter :as thread-parameter]
   [clj-surgeon.thread-parameter-fixture :as fixture]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn- plan-of
  ([variant] (plan-of variant (fixture/request)))
  ([variant request]
   (thread-parameter/plan request (fixture/sources variant))))

(defn- apply-edits [source edits]
  (reduce (fn [text {:keys [original replacement]}]
            (is (str/includes? text original) (str "missing original " (pr-str original)))
            (str/replace-first text original replacement))
          source
          edits))

(defn- planned-source [plan file]
  (let [source (some #(when (= file (:file %)) (:source %)) (fixture/sources))
        edits (some #(when (= file (:file %)) (:edits %))
                    (get-in plan [:plan :files]))]
    (apply-edits source edits)))

(defn- refused? [answer error-type]
  (and (false? (:ok answer)) (= error-type (:error_type answer))))

(deftest canonical-post-is-produced-for-all-eleven-direct-call-sites
  (let [answer (plan-of :happy)]
    (is (:ok answer) (pr-str answer))
    (is (= (set (map :file fixture/canonical-files))
           (set (map :file (get-in answer [:plan :files])))))
    (doseq [{:keys [file post]} fixture/canonical-files]
      (testing file
        (is (= post (planned-source answer file)))))))

(deftest plan-is-whole-form-find-replace-and-receipt-is-constant-sized
  (let [answer (plan-of :happy)
        files (get-in answer [:plan :files])
        edits (mapcat :edits files)]
    (is (= fixture/canonical-counts (:receipt answer)))
    (is (= 9 (count edits)))
    (is (every? #(= #{:original :replacement} (set (keys %))) edits))
    (is (every? #(or (str/starts-with? (:original %) "(defn ")
                   (str/starts-with? (:original %) "(defn- "))
                edits))
    (is (not (str/includes? (pr-str (:receipt answer)) "src/app/"))
        "the receipt contains counts, not an N-sized caller list")))

(deftest protected-comments-strings-and-reader-discards-are-byte-identical
  (let [answer (plan-of :happy)
        actual (planned-source answer "src/app/alias_one.clj")]
    (is (str/includes? actual ";; a/submit! in commentary is protected"))
    (is (str/includes? actual "\"a/submit!\""))
    (is (str/includes? actual "#_(a/submit! :discarded)"))
    (is (not (str/includes? actual "#_(a/submit! :discarded nil)")))))

(deftest owner-resolution-and-multi-arity-fail-closed
  (let [ambiguous (plan-of :ambiguous-owner)
        multi (plan-of :multi-arity)]
    (is (refused? ambiguous "thread-parameter-ambiguous-owner") (pr-str ambiguous))
    (is (= fixture/owner-file (:file ambiguous)))
    (is (= 2 (count (:owners ambiguous))))
    (is (nil? (:next_call ambiguous)))
    (is (refused? multi "thread-parameter-multi-arity-unsupported") (pr-str multi))
    (is (= "submit!" (:var multi)))
    (is (nil? (:next_call multi)))))

(deftest apply-partial-and-first-class-uses-refuse-instead-of-guessing
  (doseq [form ["(def submit-fn a/submit!)"
                "(defn batch [xs] (apply a/submit! xs))"
                "(def submit-later (partial a/submit! :x))"]]
    (let [sources (mapv #(if (= "src/app/alias_one.clj" (:file %))
                           (update % :source str "\n" form "\n")
                           %)
                        (fixture/sources))
          answer (thread-parameter/plan (fixture/request) sources)]
      (is (refused? answer "thread-parameter-indirect-reference") (pr-str answer))
      (is (= "src/app/alias_one.clj" (:file answer)))
      (is (= form (:form answer)))
      (is (nil? (:next_call answer))))))

(deftest a-supported-caller-outside-write-scope-refuses
  (let [request (fixture/request
                  {:request {:from {:file fixture/owner-file :var "submit!"}
                             :param {:name "opts" :default "nil" :position :last}
                             :scope {:paths [fixture/owner-file]}}})
        answer (plan-of :happy request)]
    (is (refused? answer "thread-parameter-caller-outside-scope") (pr-str answer))
    (is (= ["src/app/alias_one.clj" "src/app/alias_two.clj"
            "src/app/qualified.clj" "src/app/referred.clj"]
           (:files_outside_scope answer)))
    (is (nil? (:next_call answer)))))
