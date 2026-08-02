(ns clj-surgeon.move-test
  (:require
   [clj-surgeon.move :as move]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def test-source
  "(ns my.app)

(defn first-fn []
  :first)

(defn second-fn []
  :second)

(defn third-fn []
  (first-fn)
  :third)
")

(defn with-temp-file [source f]
  (let [tmp (java.io.File/createTempFile "ns-surgeon-move" ".clj")]
    (spit tmp source)
    (try
      (f (.getAbsolutePath tmp))
      (finally
        (.delete tmp)))))

(deftest test-dry-run
  (with-temp-file test-source
    (fn [path]
      (let [result (move/move-form {:file path
                                    :form "third-fn"
                                    :before "second-fn"
                                    :dry-run true})]
        (testing "dry run succeeds"
          (is (:ok result)))
        (testing "shows plan"
          (is (= "third-fn" (-> result :plan :form)))
          (is (= "second-fn" (-> result :plan :to-before))))
        (testing "file unchanged"
          (is (= test-source (slurp path))))))))

(deftest test-move-form-up
  (with-temp-file test-source
    (fn [path]
      (let [result (move/move-form {:file path
                                    :form "second-fn"
                                    :before "first-fn"})]
        (testing "move succeeds"
          (is (:ok result)))
        (testing "second-fn now appears before first-fn"
          (let [new-source (slurp path)
                second-pos (str/index-of new-source "second-fn")
                first-pos (str/index-of new-source "first-fn")]
            (is (some? second-pos))
            (is (some? first-pos))
            (is (< second-pos first-pos))))
        (testing "all forms still present"
          (let [new-source (slurp path)]
            (is (str/includes? new-source "first-fn"))
            (is (str/includes? new-source "second-fn"))
            (is (str/includes? new-source "third-fn"))))
        (testing "source is valid clojure (parens balanced)"
          (let [new-source (slurp path)
                opens (count (filter #(= \( %) new-source))
                closes (count (filter #(= \) %) new-source))]
            (is (= opens closes))))))))

(deftest test-move-form-down
  (with-temp-file test-source
    (fn [path]
      (let [result (move/move-form {:file path
                                    :form "first-fn"
                                    :before "third-fn"})]
        (testing "move succeeds"
          (is (:ok result)))
        (testing "first-fn now appears after second-fn but before third-fn"
          (let [new-source (slurp path)
                second-pos (str/index-of new-source "(defn second-fn")
                first-pos (str/index-of new-source "(defn first-fn")
                third-pos (str/index-of new-source "(defn third-fn")]
            (is (< second-pos first-pos))
            (is (< first-pos third-pos))))))))

(deftest test-move-nonexistent-form
  (with-temp-file test-source
    (fn [path]
      (let [result (move/move-form {:file path
                                    :form "nope"
                                    :before "first-fn"})]
        (is (:error result))
        (is (str/includes? (:error result) "nope"))))))

(deftest test-move-skips-declare
  (let [source "(ns my.app)

(declare my-fn)

(defn caller []
  (my-fn 42))

(defn my-fn [x]
  (+ x 1))
"]
    (with-temp-file source
      (fn [path]
        (testing "dry-run targets the defn, not the declare"
          (let [result (move/move-form {:file path
                                        :form "my-fn"
                                        :before "caller"
                                        :dry-run true})]
            (is (:ok result))
            ;; Should find the defn (line 8), NOT the declare (line 3)
            (is (> (-> result :plan :from-line) 5))))
        (testing "move actually moves the defn body"
          (let [result (move/move-form {:file path
                                        :form "my-fn"
                                        :before "caller"})]
            (is (:ok result))
            (let [new-source (slurp path)
                  defn-pos (str/index-of new-source "(defn my-fn")
                  caller-pos (str/index-of new-source "(defn caller")]
              ;; defn my-fn should now appear BEFORE defn caller
              (is (some? defn-pos))
              (is (some? caller-pos))
              (is (< defn-pos caller-pos)))))))))

(deftest test-move-with-declare-and-defn
  (let [source "(ns my.app)

(declare helper)

(defn main []
  (helper 1))

(defn middle []
  :ok)

(defn helper [x]
  (inc x))
"]
    (with-temp-file source
      (fn [path]
        (testing "moves defn helper before main, declare stays"
          (let [result (move/move-form {:file path
                                        :form "helper"
                                        :before "main"})]
            (is (:ok result))
            (let [new-source (slurp path)]
              ;; defn helper should appear before defn main
              (is (< (str/index-of new-source "(defn helper")
                     (str/index-of new-source "(defn main")))
              ;; declare should still be in the file (we don't auto-remove it)
              (is (str/includes? new-source "(declare helper)"))
              ;; all forms still present
              (is (str/includes? new-source "(defn middle")))))))))

(deftest test-move-with-comments
  (let [source "(ns my.app)

;; Helper function
;; Does important stuff
(defn helper []
  :help)

(defn main []
  :main)
"]
    (with-temp-file source
      (fn [path]
        (let [result (move/move-form {:file path
                                      :form "main"
                                      :before "helper"})]
          (is (:ok result))
          (testing "comments stay with their form"
            (let [new-source (slurp path)
                  ;; helper's comments should still precede helper
                  comment-pos (str/index-of new-source ";; Helper function")
                  helper-pos (str/index-of new-source "(defn helper")]
              (is (< comment-pos helper-pos)))))))))

;; ============================================================
;; Dependency validation: :mv refuses when the destination
;; creates new unresolved references (the Whac-A-Mole bug)
;; ============================================================

(deftest test-move-creates-new-forward-ref
  (let [source "(ns my.app)

(declare foo)

(defn bar []
  (foo 42))

(def config {:x 1})

(defn foo [x]
  (+ x (:x config)))
"]
    (with-temp-file source
      (fn [path]
        (testing "moving foo above bar creates new forward ref to config"
          ;; foo depends on config (line 8). Moving foo to before bar (line 5)
          ;; means foo references config before it's defined.
          ;; The tool detects this, refuses, and recommends :mv-with-deps.
          (let [result (move/move-form {:file path
                                        :form "foo"
                                        :before "bar"
                                        :dry-run true})]
            (is (= :would-strand-dependencies (:error-type result)))
            (is (= ["config"] (mapv :name (:stranded result))))
            (is (str/includes? (:recommended-command result)
                               ":op :mv-with-deps"))))))))

(deftest test-move-safe-when-deps-above
  (let [source "(ns my.app)

(def config {:x 1})

(declare foo)

(defn bar []
  (foo 42))

(defn foo [x]
  (+ x (:x config)))
"]
    (with-temp-file source
      (fn [path]
        (testing "moving foo above bar is safe because config is above both"
          ;; config is at line 3, destination is line 7, so foo's dependency
          ;; is already satisfied at the destination.
          (let [result (move/move-form {:file path
                                        :form "foo"
                                        :before "bar"
                                        :dry-run true})]
            (is (:ok result))))))))

;; ============================================================
;; Defining forms other than defn — defonce / def / defrecord
;;
;; The first reported regression was a (defonce ^:private cache (atom {}))
;; used 60 lines above its definition. Cold compile failed; warm REPL
;; masked it. `:mv` couldn't move the defonce up because find-form was
;; mis-parsing the metadata-wrapped symbol. These tests pin the contract
;; for ALL named top-level forms, with and without metadata.
;; ============================================================

(deftest test-move-defonce-no-metadata
  (let [source "(ns my.app)

(defn caller []
  (reset! my-state {}))

(defonce my-state (atom {}))
"]
    (with-temp-file source
      (fn [path]
        (testing "plain defonce can be moved"
          (let [result (move/move-form {:file path
                                        :form "my-state"
                                        :before "caller"})]
            (is (:ok result))
            (let [new-source (slurp path)
                  state-pos (str/index-of new-source "(defonce my-state")
                  caller-pos (str/index-of new-source "(defn caller")]
              (is (some? state-pos))
              (is (some? caller-pos))
              (is (< state-pos caller-pos)))))))))

(deftest test-move-def-no-metadata
  (let [source "(ns my.app)

(defn caller []
  (use thing))

(def thing 1)
"]
    (with-temp-file source
      (fn [path]
        (testing "plain def can be moved"
          (let [result (move/move-form {:file path
                                        :form "thing"
                                        :before "caller"})]
            (is (:ok result))
            (let [new-source (slurp path)
                  thing-pos (str/index-of new-source "(def thing")
                  caller-pos (str/index-of new-source "(defn caller")]
              (is (< thing-pos caller-pos)))))))))

(deftest test-move-defonce-with-private-meta
  ;; The regression-driver test: a private defonce used above its definition.
  ;; Reflects a forward-reference bug that took down CI for ~20h in a downstream
  ;; project, where :mv would have been the natural fix but rejected the form.
  (let [source "(ns my.app)

(defn invalidate! []
  (reset! cache {}))

(defonce ^:private cache (atom {}))
"]
    (with-temp-file source
      (fn [path]
        (testing "metadata-wrapped defonce name is recognized"
          (let [result (move/move-form {:file path
                                        :form "cache"
                                        :before "invalidate!"
                                        :dry-run true})]
            (is (:ok result) "dry-run plan should succeed, not error")
            (is (= "cache" (-> result :plan :form)))))
        (testing "metadata-wrapped defonce is actually moved"
          (let [result (move/move-form {:file path
                                        :form "cache"
                                        :before "invalidate!"})]
            (is (:ok result))
            (let [new-source (slurp path)
                  cache-pos (str/index-of new-source "(defonce ^:private cache")
                  use-pos (str/index-of new-source "(defn invalidate!")]
              (is (some? cache-pos) "defonce should still exist")
              (is (< cache-pos use-pos) "defonce should now precede its use"))))))))

(deftest test-move-defn-with-private-meta
  (let [source "(ns my.app)

(defn caller []
  (helper 42))

(defn ^:private helper [x]
  (inc x))
"]
    (with-temp-file source
      (fn [path]
        (testing "private defn name is recognized"
          (let [result (move/move-form {:file path
                                        :form "helper"
                                        :before "caller"
                                        :dry-run true})]
            (is (:ok result))
            (is (= "helper" (-> result :plan :form)))))
        (testing "private defn is moved, metadata travels with it"
          (let [result (move/move-form {:file path
                                        :form "helper"
                                        :before "caller"})]
            (is (:ok result))
            (let [new-source (slurp path)
                  ;; the ^:private MUST remain attached to the symbol
                  helper-pos (str/index-of new-source "(defn ^:private helper")
                  caller-pos (str/index-of new-source "(defn caller")]
              (is (some? helper-pos)
                  "the ^:private metadata must travel with the moved form")
              (is (< helper-pos caller-pos)))))))))

(deftest test-move-defn-with-map-meta
  ;; Map-form metadata: ^{:doc "..."} is the same node tag as ^:private but
  ;; with a richer payload. Used to live in the third-child fallback that
  ;; never actually worked because the "third child" is the arglist.
  (let [source "(ns my.app)

(defn caller []
  (doc-fn))

(defn ^{:doc \"Has docstring meta\"} doc-fn []
  :ok)
"]
    (with-temp-file source
      (fn [path]
        (let [result (move/move-form {:file path
                                      :form "doc-fn"
                                      :before "caller"
                                      :dry-run true})]
          (is (:ok result))
          (is (= "doc-fn" (-> result :plan :form))))))))

(deftest test-move-defonce-with-dynamic-meta-and-earmuffs
  ;; ^:dynamic is the canonical earmuffed-var case.
  (let [source "(ns my.app)

(defn reset-env! []
  (alter-var-root #'*my-var* (constantly nil)))

(defonce ^:dynamic *my-var* 42)
"]
    (with-temp-file source
      (fn [path]
        (let [result (move/move-form {:file path
                                      :form "*my-var*"
                                      :before "reset-env!"
                                      :dry-run true})]
          (is (:ok result))
          (is (= "*my-var*" (-> result :plan :form))))))))

(deftest test-move-defrecord-with-private-meta
  ;; defrecord is the third common defining form. Less common to mark
  ;; private but the contract should hold uniformly.
  (let [source "(ns my.app)

(defn make-point [x y]
  (->Point x y))

(defrecord ^:private Point [x y])
"]
    (with-temp-file source
      (fn [path]
        (let [result (move/move-form {:file path
                                      :form "Point"
                                      :before "make-point"
                                      :dry-run true})]
          (is (:ok result))
          (is (= "Point" (-> result :plan :form))))))))

(deftest test-move-error-message-still-clear-for-truly-missing-form
  ;; A correctness check: the metadata-handling improvement must not start
  ;; reporting false positives. Asking for a form that doesn't exist must
  ;; still produce the {:error "Form not found: ..."} response.
  (let [source "(ns my.app)

(defn ^:private real-fn []
  :ok)

(defn caller []
  (real-fn))
"]
    (with-temp-file source
      (fn [path]
        (let [result (move/move-form {:file path
                                      :form "fictional-fn"
                                      :before "caller"
                                      :dry-run true})]
          (is (:error result))
          (is (str/includes? (:error result) "fictional-fn")))))))
