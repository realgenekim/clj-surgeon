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

;; The faithful re-emission RENAMES the owner and leaves the guarded expression
;; alone. That is deliberate: `;; inner reason` guards `(get x :value)`, and
;; preservation now compares that expression's identity, so editing it in the
;; same breath is the REWRITE case (see a-rewritten-guarded-expression-* below),
;; not the faithful one. The leading note guards the owner definition itself,
;; whose text the rename does change -- accepted, because at the span's top level
;; the owner's identity is `:forms-owner-mismatch`'s job, not the comment rule's.
(def faithful
  (str ";; leading note\n"
       "(defn- finding-field\n"
       "  [x]\n"
       "  ;; inner reason\n"
       "  (get x :value))"))

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

(deftest attachment-names-the-guarded-expression-not-a-position
  ;; A comment's attachment names the EXPRESSION it guards -- that expression's
  ;; own canonical source, the side it sits on, and the depth-path of the
  ;; containing sexpr. The ordinal rides alongside as a tie-break; it is not the
  ;; identity. At the span's own top level the expression is the owner
  ;; definition, which the mission replaces by design, so `:expr` is the
  ;; sentinel `:owner-form` there.
  (testing "leading, interior and trailing attachments"
    (is (= [{:comment ";; up" :expr :owner-form :side :before :depth-path [] :ordinal 0}
            {:comment ";; in" :expr "1" :side :before :depth-path [0] :ordinal 2}
            {:comment "; down" :expr :owner-form :side :after :depth-path [] :ordinal 0}]
           (source/comment-attachments ";; up\n(def a ;; in\n 1)\n; down"))))
  (testing "the fixture owner span"
    (is (= [{:comment ";; leading note" :expr :owner-form :side :before :depth-path [] :ordinal 0}
            {:comment ";; inner reason" :expr "(get x :value)" :side :before
             :depth-path [0] :ordinal 3}]
           (source/comment-attachments owner-span))))
  (testing "extra blank lines and a second comment do not move an attachment"
    (is (= [[:owner-form :before []] [:owner-form :before []] [:owner-form :before []]
            ["(get x :value)" :before [0]]]
           (mapv (juxt :expr :side :depth-path)
                 (source/comment-attachments (str ";; one\n;; two\n\n" owner-span)))))))

;; RED first, and the reason this round exists (Astra, review of dce1d9ed):
;; "comment text AND structural attachment paths, no guessed positional
;; carryover." Text-only preservation lets a model move a comment -- most
;; dangerously a lint directive -- onto a different expression and pass.
(def two-body-span
  (str "(defn- field\n"
       "  [x]\n"
       "  (touch x)\n"
       "  ;; note: guards the get, not the touch\n"
       "  (get x :value))"))

(defn- span-basis [span]
  (assoc basis :sources {"src/a.clj" span}
         :owners [{:file "src/a.clj" :owner "field" :new-owner "finding-field"
                   :start 0 :end (count span)}]))

(deftest a-comment-moved-to-another-expression-refuses
  (let [moved-form (str "(defn- finding-field\n"
                        "  [x]\n"
                        "  ;; note: guards the get, not the touch\n"
                        "  (touch x)\n"
                        "  (get x :value))")
        r (forms/compile-forms (span-basis two-body-span) [(replacement moved-form)])]
    (testing "the text is all present, so text-only comparison would pass"
      (is (= (source/comment-nodes two-body-span) (source/comment-nodes moved-form))))
    (testing "expression-identity comparison refuses, with the from/to expressions"
      (is (false? (:ok r)))
      (is (= :forms-comment-moved (:error-type r)))
      (is (= [{:comment ";; note: guards the get, not the touch"
               :from {:expr "(get x :value)" :side :before :depth-path [0]}
               :to {:expr "(touch x)" :side :before :depth-path [0]}}]
             (:moved r)))
      (is (string? (:next_call r)))
      (is (false? (:mutation-attempted r))))))

(deftest the-same-text-against-the-same-expression-is-accepted
  (let [kept (str "(defn- finding-field\n"
                  "  [x]\n"
                  "  (touch x)\n"
                  "  ;; note: guards the get, not the touch\n"
                  "  (get x :value))")
        r (forms/compile-forms (span-basis two-body-span) [(replacement kept)])]
    (is (:ok r) (pr-str r))
    (is (= kept (get-in r [:future-sources "src/a.clj"])))))

(deftest a-lint-directive-comment-may-not-change-expressions
  ;; `#_{:clj-kondo/ignore [...]}` is an :uneval node and stays protected syntax
  ;; (see protected-syntax-scope-is-unchanged). The `;;` directive form is a
  ;; COMMENT, so it rides the attachment rule -- and it is the case where moving
  ;; the line silently re-points a suppression at innocent code.
  (let [span (str "(defn- field\n"
                  "  [x]\n"
                  "  (touch x)\n"
                  "  ;; clj-kondo/ignore\n"
                  "  (get x :value))")
        moved (str "(defn- finding-field\n"
                   "  [x]\n"
                   "  ;; clj-kondo/ignore\n"
                   "  (touch x)\n"
                   "  (get x :value))")
        r (forms/compile-forms (span-basis span) [(replacement moved)])]
    (is (= :forms-comment-moved (:error-type r)))
    (is (= [{:comment ";; clj-kondo/ignore"
             :from {:expr "(get x :value)" :side :before :depth-path [0]}
             :to {:expr "(touch x)" :side :before :depth-path [0]}}]
           (:moved r)))
    (is (false? (:mutation-attempted r)))))

(deftest staged-source-still-reads-as-clojure
  (let [span ";; leading note\n(defn- field [x] (get x :value))\n; tail note"
        faithful-span ";; leading note\n(defn- finding-field [x] (get x :value2))\n; tail note"
        staged (get-in (forms/compile-forms (span-basis span) [(replacement faithful-span)])
                       [:future-sources "src/a.clj"])]
    (is (= faithful-span staged))
    (is (= [";; leading note" "; tail note"] (source/comment-nodes staged)))
    (testing "the staged bytes load: the one form reads back as the renamed defn-"
      (is (= '(defn- finding-field [x] (get x :value2))
             (read-string staged)))))
  (testing "dropping either edge comment refuses; nothing is carried back in"
    (let [span ";; leading note\n(defn- field [x] (get x :value))\n; tail note"
          r (forms/compile-forms (span-basis span) [(replacement bare)])]
      (is (= :forms-comment-lost (:error-type r)))
      (is (= [";; leading note" "; tail note"] (:lost r))))))

(deftest protected-syntax-scope-is-unchanged
  (doseq [[label span] [["discard" "(defn- field [] #_discard 1)"]
                        ["metadata" "(defn- ^:private field [] 1)"]
                        ["reader eval" "(defn- field [] #=(+ 1 2))"]
                        ["attribute map" "(defn- field {:private true} [] 1)"]
                        ["clj-kondo ignore directive (an :uneval node)"
                         "(defn- field [] #_{:clj-kondo/ignore [:unused-value]} 1)"]]]
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
        faithful-plain "(defn- finding-field [x]\n  ;; inner reason\n  (get x :value))"]
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
                    "(defn- finding-field [x]\n  ;; interior reason\n  (get x :value))"
                    "\n; neighbour below")
        r (forms/compile-forms b [(replacement echoed)])
        staged (get-in r [:future-sources "src/a.clj"])]
    (is (:ok r) (pr-str r))
    (is (= [";; neighbour above" ";; interior reason" "; neighbour below"]
           (source/comment-nodes staged))
        "each comment appears exactly once")
    (is (= (str ";; neighbour above\n"
                "(defn- finding-field [x]\n  ;; interior reason\n  (get x :value))"
                "\n; neighbour below\n")
           staged))))

;; ---------------------------------------------------------------------------
;; RED first, and the reason THIS round exists (Astra, direct BB probe of
;; c1614bf9): "owner `(defn f [] ; guards risky (risky) (safe))`, replacement
;; `(defn f [] ; guards risky (safe) (risky))` => comment-preservation returns
;; {:preserved true}. Same path is NOT same expression."
;;
;; The previous round compared a structural PATH whose last element was an
;; ordinal. Swapping two body expressions leaves every ordinal intact and every
;; guard pointing at the wrong code, so the comparison is now the attached
;; expression's own identity.

(def guard-span
  (str "(defn- field\n"
       "  []\n"
       "  ; guards risky\n"
       "  (risky)\n"
       "  (safe))"))

(deftest swapping-the-guarded-expression-refuses-though-the-ordinal-is-intact
  (let [swapped (str "(defn- finding-field\n"
                     "  []\n"
                     "  ; guards risky\n"
                     "  (safe)\n"
                     "  (risky))")
        r (forms/compile-forms (span-basis guard-span) [(replacement swapped)])]
    (testing "the comment sits at the same ordinal in both, so the old rule passed"
      (is (= [3] (mapv :ordinal (source/comment-attachments guard-span))))
      (is (= [3] (mapv :ordinal (source/comment-attachments swapped)))))
    (testing "expression identity refuses, naming both expressions"
      (is (false? (:ok r)))
      (is (= :forms-comment-moved (:error-type r)))
      (is (= [{:comment "; guards risky"
               :from {:expr "(risky)" :side :before :depth-path [0]}
               :to {:expr "(safe)" :side :before :depth-path [0]}}]
             (:moved r)))
      (is (false? (:mutation-attempted r))))))

(deftest inserting-an-expression-before-a-commented-one-is-accepted
  ;; The previous round's ordinal rule refused this: inserting `(setup)` shifts
  ;; `(risky)` from ordinal 3 to 4 and the comment with it. Nothing MOVED --
  ;; `; guards risky` still guards `(risky)` -- so the narrowing is gone.
  (let [inserted (str "(defn- finding-field\n"
                      "  []\n"
                      "  (setup)\n"
                      "  ; guards risky\n"
                      "  (risky)\n"
                      "  (safe))")
        r (forms/compile-forms (span-basis guard-span) [(replacement inserted)])]
    (testing "the ordinal did change"
      (is (= [3] (mapv :ordinal (source/comment-attachments guard-span))))
      (is (= [4] (mapv :ordinal (source/comment-attachments inserted)))))
    (is (= {:preserved true} (source/comment-preservation guard-span inserted)))
    (is (:ok r) (pr-str r))
    (is (= inserted (get-in r [:future-sources "src/a.clj"])))))

(deftest identical-expressions-carrying-one-comment-text-tie-break-by-order
  ;; Two identical guarded expressions with the same comment text are
  ;; indistinguishable BY CONSTRUCTION -- there is no fact that separates them.
  ;; They are matched in document order, and swapping them is a no-op that must
  ;; be accepted rather than refused on a distinction that does not exist.
  (let [twins (str "(defn- field\n"
                   "  []\n"
                   "  ; guard\n"
                   "  (risky)\n"
                   "  ; guard\n"
                   "  (risky))")
        renamed (str "(defn- finding-field\n"
                     "  []\n"
                     "  ; guard\n"
                     "  (risky)\n"
                     "  ; guard\n"
                     "  (risky))")
        r (forms/compile-forms (span-basis twins) [(replacement renamed)])]
    (testing "the two attachments have equal identity and differ only by ordinal"
      (is (= [{:comment "; guard" :expr "(risky)" :side :before :depth-path [0] :ordinal 3}
              {:comment "; guard" :expr "(risky)" :side :before :depth-path [0] :ordinal 4}]
             (source/comment-attachments twins))))
    (is (= {:preserved true} (source/comment-preservation twins renamed)))
    (is (:ok r) (pr-str r))
    (is (= renamed (get-in r [:future-sources "src/a.clj"])))))

(deftest a-rewritten-guarded-expression-refuses-without-the-opt-in
  ;; The honest hard case: the mission MEANS to change the expression the
  ;; comment guards. Its identity changed, so the comment no longer demonstrably
  ;; guards what it used to. Refuse, name both expressions, and say in
  ;; :next_call what the caller may do about it.
  (let [rewritten (str "(defn- finding-field\n"
                       "  []\n"
                       "  ; guards risky\n"
                       "  (risky-v2)\n"
                       "  (safe))")
        r (forms/compile-forms (span-basis guard-span) [(replacement rewritten)])]
    (is (= :forms-comment-moved (:error-type r)))
    (is (= [{:comment "; guards risky"
             :from {:expr "(risky)" :side :before :depth-path [0]}
             :to {:expr "(risky-v2)" :side :before :depth-path [0]}}]
           (:moved r)))
    (is (re-find #":comment-follows-rewrite" (:next_call r)))
    (is (false? (:mutation-attempted r)))))

(deftest a-rewritten-guarded-expression-is-accepted-when-the-caller-opts-in
  (let [rewritten (str "(defn- finding-field\n"
                       "  []\n"
                       "  ; guards risky\n"
                       "  (risky-v2)\n"
                       "  (safe))")
        b (assoc (span-basis guard-span) :comment-follows-rewrite true)
        r (forms/compile-forms b [(replacement rewritten)])]
    (is (:ok r) (pr-str r))
    (is (= rewritten (get-in r [:future-sources "src/a.clj"])))
    (testing "the opt-in is an ordinal rule, and says so: a swap passes under it"
      (is (= {:preserved true}
             (source/comment-preservation
               guard-span
               "(defn- finding-field\n  []\n  ; guards risky\n  (safe)\n  (risky))"
               {:comment-follows-rewrite true}))))
    (testing "and it is opt-in only -- the same swap still refuses by default"
      (is (false? (:preserved (source/comment-preservation
                                guard-span
                                (str "(defn- finding-field\n  []\n  ; guards risky\n"
                                     "  (safe)\n  (risky))")))))))
  (testing "the opt-in does not resurrect a LOST comment"
    (is (= {:preserved false :lost ["; guards risky"]}
           (source/comment-preservation guard-span "(defn- finding-field [] (risky) (safe))"
                                        {:comment-follows-rewrite true})))))
