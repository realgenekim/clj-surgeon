(ns clj-surgeon.mission-forms-source-test
  {:lane :fast}
  (:require
   [clj-surgeon.mission-forms :as forms]
   [clj-surgeon.mission-forms-source :as source]
   [clj-surgeon.mission-plain-forms :as plain]
   [clojure.test :refer [deftest is testing]]))

;; One fixture file, three comments in three positions relative to the owner
;; span, and untouched bytes on both sides. Offsets are derived, never typed:
;; a hand-typed offset is a second source of truth for the span.
(def prefix "(ns demo)\n\n")
(def owner-span
  (str ";; leading note\n"
       "(defn- field\n"
       "  [x]\n"
       "  ;; inner reason\n"
       "  (get x :value))"))
(def suffix "\n\n(def untouched 3)\n")
(def file-source (str prefix owner-span suffix))
(def owner-start (count prefix))
(def owner-end (+ owner-start (count owner-span)))

(def basis
  {:sources {"src/a.clj" file-source}
   :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
             :start owner-start :end owner-end}]
   :budget {:max-files 1 :max-changed-chars 4000}})

(defn replacement [form] {:file "src/a.clj" :owner "field" :form form})

(def faithful
  (str ";; leading note\n"
       "(defn- finding-field\n"
       "  [x]\n"
       "  ;; inner reason\n"
       "  (get x :value2))"))

(def bare "(defn- finding-field [x] (get x :value2))")

(deftest comments-are-read-as-source-not-data
  (testing "every comment in a span is seen, in order, verbatim"
    (is (= [";; leading note" ";; inner reason"] (source/comment-nodes owner-span))))
  (testing "position classifies leading, interior and trailing"
    (is (= [{:text ";; up" :position :leading}
            {:text ";; in" :position :interior}
            {:text "; down" :position :trailing}]
           (source/classified-comments ";; up\n(def a ;; in\n 1)\n; down"))))
  (testing "a semicolon inside a string literal is not a comment"
    (is (= [] (source/comment-nodes "(def a \"; not a comment ;; either\")")))
    (is (= [";; but this is"]
           (source/comment-nodes "(def a \"; not\") ;; but this is"))))
  (testing "a multi-line comment block is a comment per line"
    (is (= [";; one" ";; two" ";; three"]
           (source/comment-nodes ";; one\n;; two\n;; three\n(def a 1)"))))
  (testing "UTF-8 survives the parse"
    (is (= [";; naïve — 日本語"] (source/comment-nodes ";; naïve — 日本語\n(def a 1)")))))

(deftest lower-replacement-demands-exactly-one-form
  (is (= "(defn- finding-field [x] (get x :value2))"
         (str (:form (source/lower-replacement (str ";; keep\n" bare))))))
  (doseq [[label text] [["two definitions" "(def a 1)\n(def b 2)"]
                        ["comment only" ";; nothing but a comment"]]]
    (testing label
      (is (= :forms-replacement-not-one-form
             (:error-type (ex-data (try (source/lower-replacement text)
                                        (catch clojure.lang.ExceptionInfo e e)))))))))

(deftest comment-preservation-is-a-multiset-difference
  (is (= {:preserved true} (source/comment-preservation owner-span faithful)))
  (is (= {:preserved false :lost [";; leading note" ";; inner reason"]}
         (source/comment-preservation owner-span bare)))
  (testing "a comment carried twice and re-emitted once is lost once"
    (is (= {:preserved false :lost [";; twice"]}
           (source/comment-preservation ";; twice\n(def a ;; twice\n 1)" ";; twice\n(def a 1)")))))

(deftest splice-owner-leaves-every-outside-byte-identical
  (let [spliced (source/splice-owner file-source owner-start owner-end faithful)]
    (is (= (str prefix faithful suffix) spliced))
    (testing "byte comparison outside the span"
      (is (= (seq (.getBytes prefix "UTF-8"))
             (seq (.getBytes (subs spliced 0 owner-start) "UTF-8"))))
      (is (= (seq (.getBytes suffix "UTF-8"))
             (seq (.getBytes (subs spliced (- (count spliced) (count suffix))) "UTF-8"))))))
  (testing "a span that does not land on child boundaries refuses, it does not guess"
    (is (= :forms-source-span-unaligned
           (:error-type (ex-data (try (source/splice-owner file-source (inc owner-start) owner-end bare)
                                      (catch clojure.lang.ExceptionInfo e e))))))))

(deftest owner-comments-round-trip-through-compile-forms
  (let [r (forms/compile-forms basis [(replacement faithful)])]
    (is (:ok r))
    (is (= (str prefix faithful suffix) (get-in r [:future-sources "src/a.clj"])))
    (testing "the comments are in the staged bytes"
      (is (= [";; leading note" ";; inner reason"]
             (source/comment-nodes (get-in r [:future-sources "src/a.clj"])))))
    (is (false? (:mutation-attempted r)))))

(deftest a-dropped-comment-is-loud-not-silent
  (let [r (forms/compile-forms basis [(replacement bare)])]
    (is (false? (:ok r)))
    (is (= :forms-comment-lost (:error-type r)))
    (is (= [";; leading note" ";; inner reason"] (:lost r)))
    (is (string? (:next_call r)))
    (is (false? (:mutation-attempted r)))))

(deftest carry-comments-moves-only-what-has-one-legal-position
  (testing "a lost leading comment is carried above the form"
    (let [span ";; leading note\n(defn- field [x] (get x :value))"
          b (assoc basis :sources {"src/a.clj" span}
                   :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                             :start 0 :end (count span)}])
          r (forms/compile-forms b [(replacement bare)] {:carry-comments true})]
      (is (:ok r))
      (is (= (str ";; leading note\n" bare) (get-in r [:future-sources "src/a.clj"])))))
  (testing "a lost trailing comment is carried below the form"
    (let [span "(defn- field [x] (get x :value))\n; tail note"
          b (assoc basis :sources {"src/a.clj" span}
                   :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                             :start 0 :end (count span)}])
          r (forms/compile-forms b [(replacement bare)] {:carry-comments true})]
      (is (:ok r))
      (is (= (str bare "\n; tail note") (get-in r [:future-sources "src/a.clj"])))))
  (testing "an interior comment is refused even under carry: its position is meaning"
    (let [r (forms/compile-forms basis [(replacement bare)] {:carry-comments true})]
      (is (false? (:ok r)))
      (is (= :forms-comment-lost (:error-type r)))
      (is (= [";; inner reason"] (:lost r)))))
  (testing "carry is off by default"
    (is (= :forms-comment-lost (:error-type (forms/compile-forms basis [(replacement bare)]))))))

(deftest carried-source-still-reads-as-clojure
  (let [span ";; leading note\n(defn- field [x] (get x :value))\n; tail note"
        b (assoc basis :sources {"src/a.clj" span}
                 :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                           :start 0 :end (count span)}])
        staged (get-in (forms/compile-forms b [(replacement bare)] {:carry-comments true})
                       [:future-sources "src/a.clj"])]
    (is (= [";; leading note" "; tail note"] (source/comment-nodes staged)))
    (testing "the staged bytes load: the one form reads back as the renamed defn-"
      (is (= '(defn- finding-field [x] (get x :value2))
             (read-string staged))))))

(deftest protected-syntax-scope-is-unchanged
  (doseq [[label span] [["discard" "(defn- field [] #_discard 1)"]
                        ["metadata" "(defn- ^:private field [] 1)"]
                        ["reader eval" "(defn- field [] #=(+ 1 2))"]
                        ["attribute map" "(defn- field {:private true} [] 1)"]]]
    (testing label
      (let [b (assoc basis :sources {"src/a.clj" span}
                     :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                               :start 0 :end (count span)}])]
        (is (= :forms-protected-syntax
               (:error-type (forms/compile-forms b [(replacement bare)]))))))))

(deftest plain-route-accepts-a-comment-inside-a-form-only
  (let [span "(defn- field [x]\n  ;; inner reason\n  (get x :value))"
        b (assoc basis :sources {"src/a.clj" span}
                 :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                           :start 0 :end (count span)}])
        faithful-plain "(defn- finding-field [x]\n  ;; inner reason\n  (get x :value2))"]
    (testing "a comment inside a form reaches mission-forms and survives"
      (let [r (plain/compile-response b faithful-plain)]
        (is (:ok r))
        (is (= faithful-plain (get-in r [:future-sources "src/a.clj"])))))
    (testing "dropping it refuses loudly rather than deleting it"
      (is (= :forms-comment-lost (:error-type (plain/compile-response b bare)))))
    (testing "a top-level comment belongs to no owner span and still refuses"
      (is (= :forms-protected-syntax
             (:error-type (plain/compile-response b (str ";; stray\n" faithful-plain))))))))

(deftest cljfmt-is-optional-and-says-so
  ;; deps.edn carries rewrite-clj, not cljfmt. The seam must report that
  ;; honestly instead of pretending it formatted.
  (let [r (source/format-replacement bare)]
    (is (= bare (:source r)))
    (when-not (:formatted? r)
      (is (= :cljfmt-absent (:reason r))))))

(deftest an-echoed-neighbouring-comment-is-not-duplicated
  ;; RED first, from a live fake-provider run through mission-typist-executor:
  ;; asked to re-emit an owner whose span is the defn ALONE, the model faithfully
  ;; echoed the `;;` line above it and the `;` line below it. Both live OUTSIDE
  ;; the span, so splicing them left a second copy of each in the file. The
  ;; replacement's leading/trailing comments are now kept only where the owner
  ;; span itself carried one; preservation is judged on the bytes actually spliced.
  (let [span "(defn- field [x]\n  ;; interior reason\n  (get x :value))"
        file (str ";; neighbour above\n" span "\n; neighbour below\n")
        b {:sources {"src/a.clj" file}
           :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                     :start (count ";; neighbour above\n")
                     :end (+ (count ";; neighbour above\n") (count span))}]
           :budget {:max-files 1 :max-changed-chars 4000}}
        echoed (str ";; neighbour above\n"
                    "(defn- finding-field [x]\n  ;; interior reason\n  (get x :value2))"
                    "\n; neighbour below")
        r (forms/compile-forms b [(replacement echoed)])
        staged (get-in r [:future-sources "src/a.clj"])]
    (is (:ok r) (pr-str r))
    (is (= [";; neighbour above" ";; interior reason" "; neighbour below"]
           (source/comment-nodes staged))
        "each comment appears exactly once")
    (is (= (str ";; neighbour above\n"
                "(defn- finding-field [x]\n  ;; interior reason\n  (get x :value2))"
                "\n; neighbour below\n")
           staged))))
