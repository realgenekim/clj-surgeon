(ns clj-surgeon.format-scope-test
  "Witnesses for confining a managed formatter to the forms a change edited.

   Evidence for why this exists:
   docs/observations/2026-09-02-captains-log-the-big-aha-and-reset.md, \"churn
   attributed\" — `l1` turned 93 lines of work into +508/-476 because the
   formatter was handed whole staged files."
  (:require
   [clj-surgeon.format-scope :as format-scope]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private churn-source
  (str "(ns app.reducer\n"
       "  (:require [clojure.string :as str]\n"
       "            [clojure.set :as set]))\n"
       "\n"
       ";; a comment block between forms\n"
       ";; that nothing may reformat\n"
       "\n"
       "(defn session [x]\n"
       "  ;; keep me\n"
       "  (str/join \",\"    [x x]))\n"
       "\n"
       "(defn merge-in [y]\n"
       "      (set/union #{y}   #{1}))\n"))

(defn- text-at
  [source {:keys [start end]}]
  (subs source start end))

;; @spec MCP-OP-FMT-001
(deftest top-level-forms-exclude-every-byte-between-them
  (let [spans (format-scope/top-level-form-spans churn-source)]
    (is (= 3 (count spans)))
    (is (str/starts-with? (text-at churn-source (first spans)) "(ns app.reducer"))
    (is (str/ends-with? (text-at churn-source (first spans)) ":as set]))"))
    (testing "the comment block between forms belongs to no form"
      (is (not-any? #(str/includes? (text-at churn-source %)
                                    "a comment block between forms")
                    spans)))
    (testing "a comment inside a form travels with that form"
      (is (str/includes? (text-at churn-source (second spans)) ";; keep me")))
    (testing "spans are ascending and disjoint"
      (is (apply < (mapcat (juxt :start :end) spans))))))

;; @spec MCP-OP-FMT-001
(deftest a-source-with-no-forms-yields-no-spans
  (is (= [] (format-scope/top-level-form-spans "")))
  (is (= [] (format-scope/top-level-form-spans "\n\n")))
  (is (= [] (format-scope/top-level-form-spans ";; only a comment\n"))))

;; @spec MCP-OP-FMT-001
(deftest a-reader-conditional-is-one-top-level-form
  (let [source "#?(:clj 1\n   :cljs 2)\n(def x 1)\n"
        spans (format-scope/top-level-form-spans source)]
    (is (= 2 (count spans)))
    (is (= "#?(:clj 1\n   :cljs 2)" (text-at source (first spans))))))

;; @spec MCP-OP-FMT-002
(deftest only-the-forms-an-edit-touches-are-selected
  (let [ns-offset (str/index-of churn-source "[clojure.set")
        body-offset (str/index-of churn-source "(set/union")]
    (testing "an edit inside the ns form selects the ns form and nothing else"
      (let [selected (format-scope/enclosing-form-spans
                       churn-source [{:offset ns-offset :length 20}])]
        (is (= 1 (count selected)))
        (is (str/starts-with? (text-at churn-source (first selected)) "(ns "))))
    (testing "an edit inside one defn selects that defn only"
      (let [selected (format-scope/enclosing-form-spans
                       churn-source [{:offset body-offset :length 10}])]
        (is (= 1 (count selected)))
        (is (str/includes? (text-at churn-source (first selected)) "merge-in"))))
    (testing "two edits in two forms select both, ascending"
      (let [selected (format-scope/enclosing-form-spans
                       churn-source
                       [{:offset body-offset :length 10}
                        {:offset ns-offset :length 20}])]
        (is (= 2 (count selected)))
        (is (< (:start (first selected)) (:start (second selected))))))
    (testing "no edits select no forms"
      (is (= [] (format-scope/enclosing-form-spans churn-source []))))))

;; @spec MCP-OP-FMT-002
(deftest a-zero-length-edit-on-a-form-boundary-selects-that-form
  (let [source "(def a 1)\n\n(def b 2)\n"
        at-start (format-scope/enclosing-form-spans source [{:offset 0 :length 0}])
        between (format-scope/enclosing-form-spans source [{:offset 10 :length 0}])]
    (is (= [{:start 0 :end 9}] at-start))
    (is (= [] between)
        "an insertion in the whitespace between forms encloses no form")))

;; @spec MCP-OP-FMT-003
(deftest a-form-that-grows-does-not-move-a-form-not-yet-spliced
  (let [source "(a)\nGAP\n(bb)\nEND"
        regions [{:start 0 :end 3} {:start 8 :end 12}]
        {:keys [source spans]}
        (format-scope/splice-forms source regions ["(aaaaaaa)" "(b)"])]
    (is (= "(aaaaaaa)\nGAP\n(b)\nEND" source))
    (is (= [{:offset 0 :length 9} {:offset 14 :length 3}] spans))
    (testing "each result span really holds its formatted text"
      (is (= "(aaaaaaa)" (subs source 0 9)))
      (is (= "(b)" (subs source 14 17))))))

;; @spec MCP-OP-FMT-003
(deftest splicing-nothing-changes-nothing
  (let [{:keys [source spans]} (format-scope/splice-forms "(a)\n(b)\n" [] [])]
    (is (= "(a)\n(b)\n" source))
    (is (= [] spans))))

;; @spec MCP-OP-FMT-004
(deftest the-scope-proof-passes-only-when-the-gaps-survive-verbatim
  (let [pre "(a)\nGAP\n(bb)\nEND"
        regions [{:start 0 :end 3} {:start 8 :end 12}]
        {post :source spans :spans}
        (format-scope/splice-forms pre regions ["(aaaaaaa)" "(b)"])]
    (testing "a splice that only replaced the forms is exact"
      (let [proof (format-scope/scope-drift pre post regions spans)]
        (is (= 0 (:byte-drift-outside-forms proof)))
        (is (= :exact (:span-alignment proof)))))
    (testing "one byte changed in a gap is drift, even though the forms match"
      (let [tampered (str/replace post "GAP" "gap")
            proof (format-scope/scope-drift pre tampered regions spans)]
        (is (pos? (:byte-drift-outside-forms proof)))
        (is (= :unlocatable (:span-alignment proof)))))
    (testing "trailing bytes appended after the last form are drift"
      (let [proof (format-scope/scope-drift pre (str post "\n;; junk")
                                            regions spans)]
        (is (pos? (:byte-drift-outside-forms proof)))
        (is (= :unlocatable (:span-alignment proof)))))))

;; @spec MCP-OP-FMT-005
(deftest a-formatter-that-reorders-tokens-inside-a-form-is-refused
  ;; Red-team probe p2/p2b: a token MULTISET admits four rewrites that change
  ;; what the code does, and probe p2b landed one of them on disk end to end.
  ;; The stream is order-sensitive everywhere; the single sanctioned reordering
  ;; — the sibling clauses of an ns `:require` / `:import` list — is normalised
  ;; away by sorting whole clause subtrees, so a clause MOVING is admitted and a
  ;; symbol moving BETWEEN two clauses is not.
  (let [admissible?
        (fn [a b]
          (let [before (format-scope/clause-normalised-stream a)
                after (format-scope/clause-normalised-stream b)]
            (boolean (and before after (= before after)))))]
    (testing "the four bag-preserving corruptions are refused"
      (is (not (admissible?
                 "(defn authorize [user] (audit 2) (if (admin? user) (grant) (deny)))"
                 "(defn authorize [user] (audit 2) (if (admin? user) (deny) (grant)))"))
          "swap-if-branches")
      (is (not (admissible?
                 "(defn balance [credit debit] (audit 2) (- credit debit))"
                 "(defn balance [credit debit] (audit 2) (- debit credit))"))
          "swap-non-commutative-args")
      (is (not (admissible?
                 "(defn go [x] (audit 2) (compare (first x) (second)))"
                 "(defn go [x] (audit 2) (compare (first) (second x)))"))
          "move-token-to-sibling")
      (is (not (admissible?
                 (str "(ns app.t\n  (:require [app.safe :refer [check]]\n"
                      "            [app.unsafe :refer []]\n"
                      "            [app.clock :as clock]))")
                 (str "(ns app.t\n  (:require [app.safe :refer []]\n"
                      "            [app.unsafe :refer [check]]\n"
                      "            [app.clock :as clock]))")))
          "move-:refer-between-requires"))
    (testing "the two sanctioned changes stay admissible"
      (is (admissible?
            "(ns app.t\n  (:require [zzz.last :as z]\n            [aaa.first :as a]))"
            "(ns app.t\n  (:require\n   [aaa.first :as a]\n   [zzz.last :as z]))")
          "requires sorted — what standard-clojure-style fix visibly does")
      (is (admissible? "(defn go [] (inc    1))" "(defn go []\n  (inc 1))")
          "pure whitespace"))
    (testing "an :import list is normalised the same way"
      (is (admissible?
            "(ns a (:import (java.util Date) (java.io File)))"
            "(ns a\n  (:import\n   (java.io File)\n   (java.util Date)))")))
    (testing "a dropped or rewritten token is refused"
      (is (not (admissible? "(ns a (:require [b] [c]))" "(ns a (:require [b]))")))
      (is (not (admissible? "(def x  [1  2])" "(def x  [9  9])"))))
    (testing "a swallowed comment is refused"
      (is (not (admissible? "(defn a [] ;; keep\n  1)" "(defn a []\n  1)"))))
    (testing "text that does not parse has no stream, and nil never matches nil"
      (is (nil? (format-scope/clause-normalised-stream "(unclosed")))
      (is (not (admissible? "(unclosed" "(also unclosed"))))))

;; @spec MCP-OP-FMT-005
(deftest the-real-formatters-comment-spacing-is-layout
  ;; Red-team probe p11, on the REAL pinned binary and the REAL wire route with
  ;; no doubles: `standard-clojure-style` 0.29.0 rewrites `;;foo` to `;; foo`,
  ;; and the check refused the whole `apply_clojure_changes` transaction,
  ;; accusing the formatter of changing code. An ordinary comment in ordinary
  ;; source made the tool unusable.
  (let [admissible?
        (fn [a b]
          (let [before (format-scope/clause-normalised-stream a)
                after (format-scope/clause-normalised-stream b)]
            (boolean (and before after (= before after)))))]
    (testing "the three shapes the real formatter actually produces are layout"
      (is (admissible? "(defn go []\n  ;;x\n  (a))"
                       "(defn go []\n  ;; x\n  (a))")
          ";;foo -> ;; foo")
      (is (admissible? "(defn go []\n  ;;;x\n  (a))"
                       "(defn go []\n  ;;; x\n  (a))")
          ";;;foo -> ;;; foo, and the semicolon run itself is still compared")
      (is (admissible? "(defn go []\n  (a) ;;t\n  (b))"
                       "(defn go []\n  (a) ;; t\n  (b))")
          "an end-of-line comment")
      (is (admissible? "(defn go []\n  ;; note   \n  (a))"
                       "(defn go []\n  ;; note\n  (a))")
          "trailing whitespace on a comment"))
    (testing "and everything that is not that spacing is still refused"
      (is (not (admissible? "(defn go []\n  ;; a\n  (x))"
                            "(defn go []\n  ;; b\n  (x))"))
          "comment TEXT changed")
      (is (not (admissible? "(defn go []\n  ;; a\n  (x))"
                            "(defn go []\n  (x))"))
          "comment DELETED")
      (is (not (admissible? "(defn go []\n  (x))"
                            "(defn go []\n  ;; injected\n  (x))"))
          "comment ADDED")
      (is (not (admissible? "(defn go []\n  ;; a\n  (x)\n  (y))"
                            "(defn go []\n  (x)\n  ;; a\n  (y))"))
          "comment MOVED across a token")
      (is (not (admissible? "(defn go []\n  ;; a\n  (x))"
                            "(defn go []\n  ;;; a\n  (x))"))
          "the number of semicolons is content, not layout"))))

;; @spec MCP-OP-FMT-005
(deftest a-comment-reattached-to-a-different-require-clause-is-refused
  ;; Red-team probe p8 (i-b, i-c). The clause sort is what makes a sorted
  ;; `:require` admissible, and a comment inside that list was a sortable
  ;; sibling of its own — so a comment could be detached from the require it
  ;; explains and reattached to another one, and it committed. The real
  ;; formatter moves a comment WITH its clause and never across, so sorting
  ;; whole GROUPS (leading comments plus the clause they precede) admits what
  ;; the formatter does and refuses what it never does.
  (let [admissible?
        (fn [a b]
          (let [before (format-scope/clause-normalised-stream a)
                after (format-scope/clause-normalised-stream b)]
            (boolean (and before after (= before after)))))]
    (testing "reattaching a comment to a different clause is refused"
      (is (not (admissible?
                 (str "(ns app.t\n  (:require\n"
                      "   ;; we need this one for parsing\n"
                      "   [aaa.first :as a]\n   [zzz.last :as z]))")
                 (str "(ns app.t\n  (:require\n   [aaa.first :as a]\n"
                      "   ;; we need this one for parsing\n"
                      "   [zzz.last :as z]))")))))
    (testing "the sanctioned sibling: clauses sorted, the comment travels along"
      (is (admissible?
            (str "(ns app.t\n  (:require\n   ;; note\n"
                 "   [zzz.last :as z]\n   [aaa.first :as a]))")
            (str "(ns app.t\n  (:require\n   [aaa.first :as a]\n"
                 "   ;; note\n   [zzz.last :as z]))"))))
    (testing "moving a comment out of the clause list is refused"
      (is (not (admissible?
                 "(ns app.t\n  (:require\n   ;; note\n   [aaa.first :as a]))"
                 "(ns app.t\n  ;; note\n  (:require\n   [aaa.first :as a]))"))))
    (testing "changing the comment's text inside the clause list is refused"
      (is (not (admissible?
                 "(ns app.t\n  (:require\n   ;; note\n   [aaa.first :as a]))"
                 "(ns app.t\n  (:require\n   ;; NOTE\n   [aaa.first :as a]))"))))
    (testing "a trailing comment after the last clause is not dropped"
      (is (not (admissible?
                 "(ns app.t\n  (:require\n   [aaa.first :as a]\n   ;; tail\n   ))"
                 "(ns app.t\n  (:require\n   [aaa.first :as a]))"))))
    (testing "and the require sort itself still works with no comments at all"
      (is (admissible?
            "(ns app.t\n  (:require [zzz.last :as z]\n            [aaa.first :as a]))"
            "(ns app.t\n  (:require\n   [aaa.first :as a]\n   [zzz.last :as z]))")))))

;; @spec MCP-OP-FMT-005
(deftest one-form-in-one-form-out
  (is (true? (format-scope/one-form? "(defn a [] 1)\n")))
  (is (true? (format-scope/one-form? "#?(:clj 1 :cljs 2)")))
  (is (false? (format-scope/one-form? "(a)\n(b)\n")))
  (is (false? (format-scope/one-form? "")))
  (is (false? (format-scope/one-form? ";; only a comment")))
  (is (false? (format-scope/one-form? "(unclosed"))))

;; @spec MCP-OP-FMT-003
(deftest a-formatter-trailing-newline-is-not-part-of-the-span
  (is (= "(def a 1)" (format-scope/trim-trailing-newlines "(def a 1)\n")))
  (is (= "(def a 1)" (format-scope/trim-trailing-newlines "(def a 1)\r\n\r\n")))
  (is (= "(def a 1)" (format-scope/trim-trailing-newlines "(def a 1)"))))

;; @spec MCP-OP-FMT-010
(deftest a-staged-candidate-that-does-not-match-its-guard-reference-is-refused
  ;; Red-team probe p3(c): an earlier staging step churns the file, the scoped
  ;; format then rewrites the guard to point at the CHURNED image, and the
  ;; commit gate — which measures against that guard — sees drift 0. The churn
  ;; is laundered through the very gate that exists to catch it.
  (let [reference "(def a 1)\n\n;; important comment\n\n(def b 2)\n"
        churned "(def a 1)\n\n;; IMPORTANT COMMENT WAS REWRITTEN\n\n(def b 2)\n"
        plan (format-scope/file-plan
               "t.clj" churned
               {:reference reference :spans [{:offset 7 :length 1}]})]
    (is (false? (:ok plan)))
    (is (= :format-scope-candidate-mismatch (:error-type plan)))
    (is (= "t.clj" (:file plan)))
    (is (true? (:source-unchanged plan)))
    (is (false? (:mutation-attempted plan)))
    (is (false? (:write-authority plan))))
  (testing "a candidate that IS its guard's reference plans normally"
    (let [source "(def a 1)\n"
          plan (format-scope/file-plan
                 "t.clj" source
                 {:reference source :spans [{:offset 5 :length 1}]})]
      (is (= [{:start 0 :end 9}] (:regions plan))))))

;; @spec MCP-OP-FMT-011
(deftest an-unparseable-candidate-with-named-spans-is-refused
  ;; Red-team probe p3(b) / p4(b2): an unparseable candidate yields no
  ;; top-level forms, which today is a silent no-op that still overwrites the
  ;; guard with the unparseable bytes.
  (let [bad "(def a 1\n"
        plan (format-scope/file-plan
               "t.clj" bad {:reference bad :spans [{:offset 5 :length 1}]})]
    (is (false? (:ok plan)))
    (is (= :format-scope-unparseable-candidate (:error-type plan)))
    (is (= "t.clj" (:file plan)))
    (is (true? (:source-unchanged plan))))
  (testing "a guard that names no spans has nothing to scope and is not a refusal"
    (let [bad "(def a 1\n"
          plan (format-scope/file-plan "t.clj" bad {:reference bad :spans []})]
      (is (= [] (:regions plan)))))
  (testing "a file of only comments with a named span is refused, not skipped"
    (let [source ";; only a comment\n"
          plan (format-scope/file-plan
                 "t.clj" source
                 {:reference source :spans [{:offset 3 :length 1}]})]
      (is (= :format-scope-unparseable-candidate (:error-type plan))))))

;; @spec MCP-OP-FMT-006
(deftest an-unmeasurable-or-exempt-staged-file-is-decided-purely
  (testing "no guard entry cannot be scoped"
    (let [plan (format-scope/file-plan "t.clj" "(def a 1)\n" nil)]
      (is (= :format-scope-unmeasurable (:error-type plan)))))
  (testing "a guard entry with no reference bytes cannot be scoped"
    (let [plan (format-scope/file-plan "t.clj" "(def a 1)\n"
                                       {:reference nil :spans []})]
      (is (= :format-scope-unmeasurable (:error-type plan)))))
  (testing "an exemption recorded in the guard leaves the file alone"
    (let [plan (format-scope/file-plan "t.clj" "(def a 1)\n"
                                       {:exempt :created-file})]
      (is (= [] (:regions plan)))
      (is (nil? (:error-type plan))))))
