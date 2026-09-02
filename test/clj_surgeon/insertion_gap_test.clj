(ns clj-surgeon.insertion-gap-test
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(defn- compile-owner-insertion
  [source owner side inserted]
  (transaction/compile-transaction
    {"src/sample.clj" source}
    {:changes [{:id :owner-insertion
                :in ["src/sample.clj"]
                :forms [owner]
                :do [side [inserted]]
                :expect {:matches 1}}]
     :expect {:changes 1 :edits 1 :files 1}}))

;; @spec MCP-OP-INSERT-007
(deftest named-owner-insertion-accepts-mechanically-owned-comments
  ;; Minimized from the 2026-09-02 channel.clj treatment refusals.
  (let [source (str "(ns sample)\n\n"
                    ";; Render the existing channel.\n"
                    "(defn render-channel [] :existing)\n")
        expected (str "(ns sample)\n\n"
                      "(defn render-clock [] :clock)\n\n"
                      ";; Render the existing channel.\n"
                      "(defn render-channel [] :existing)\n")
        result (compile-owner-insertion
                 source 'render-channel :insert-left
                 "(defn render-clock [] :clock)")]
    (is (:ok result))
    (is (= expected (get-in result [:future-sources "src/sample.clj"])))))

;; @spec MCP-OP-INSERT-007
(deftest named-owner-insertion-preserves-leading-comment-ownership
  (let [source (str "(ns sample)\n\n"
                    "(defn owner-a [] :a)\n\n"
                    ";; Documents owner B, not the inserted owner.\n"
                    "(defn owner-b [] :b)\n")
        result (compile-owner-insertion
                 source 'owner-b :insert-left
                 "(defn inserted-owner [] :inserted)")
        future (get-in result [:future-sources "src/sample.clj"])
        inserted-index (some-> future (str/index-of "(defn inserted-owner"))
        comment-index (some-> future (str/index-of ";; Documents owner B"))
        owner-b-index (some-> future (str/index-of "(defn owner-b"))]
    (is (:ok result))
    (is (and inserted-index comment-index owner-b-index
             (< inserted-index comment-index owner-b-index))
        "the inserted owner must precede B's still-attached leading comment")
    (when future
      (is (= 1 (count (re-seq #"Documents owner B" future)))))))

;; @spec MCP-OP-INSERT-007
(deftest named-owner-insertion-preserves-same-line-trailing-comment-ownership
  (let [source (str "(ns sample)\n\n"
                    "(defn owner-a [] :a) ; documents owner A\n"
                    "(defn owner-b [] :b)\n")
        expected (str "(ns sample)\n\n"
                      "(defn owner-a [] :a) ; documents owner A\n"
                      "(defn inserted-owner [] :inserted)\n"
                      "(defn owner-b [] :b)\n")
        result (compile-owner-insertion
                 source 'owner-a :insert-right
                 "(defn inserted-owner [] :inserted)")]
    (is (:ok result))
    (is (= expected (get-in result [:future-sources "src/sample.clj"])))))

;; @spec MCP-OP-INSERT-007
;; @spec MCP-OP-INSERT-008
(deftest named-owner-insertion-preserves-existing-gap-exactly-once
  (let [source (str "(ns sample)\n\n"
                    "(defn owner-a [] :a)\n\n"
                    ";; unique-gap-marker owned by B\n"
                    "(defn owner-b [] :b)\n")
        inserted "(defn inserted-owner [] :inserted)"
        result (compile-owner-insertion source 'owner-b :insert-left inserted)
        future (get-in result [:future-sources "src/sample.clj"])]
    (is (:ok result))
    (when future
      (is (= 1 (count (re-seq #"unique-gap-marker" future))))
      (is (= source (str/replace-first future
                                       (re-pattern
                                         (str (java.util.regex.Pattern/quote inserted)
                                              "\\n\\n"))
                                       ""))
          "removing only the insertion and fresh separator must recover every original byte"))))

;; @spec MCP-OP-INSERT-004
;; @spec MCP-OP-INSERT-010
(deftest nested-insertion-uses-anchor-line-indentation-after-earlier-blank-line
  (let [file "src/sample.clj"
        source (str "(ns sample)\n\n"
                    "(defn render []\n"
                    "  (str\n"
                    "    \"prefix\"\n"
                    "    \"target\"))\n")
        target-start (str/index-of source "\"target\"")
        old-indent (second (re-find #"(?m)(?:^|\n)([ \\t]*)$"
                                    (subs source 0 target-start)))
        expected (str "(ns sample)\n\n"
                      "(defn render []\n"
                      "  (str\n"
                      "    \"prefix\"\n"
                      "    \"target\"\n"
                      "    (when enabled? (render-extra))))\n")
        result
        (transaction/compile-transaction
          {file source}
          {:changes [{:id :nested-anchor
                      :in [file]
                      :forms ['render]
                      :find "\"target\""
                      :do [:insert-right ["(when enabled? (render-extra))"]]
                      :expect {:matches 1}}]
           :expect {:changes 1 :edits 1 :files 1}})]
    (is (= "" old-indent)
        "causal control: the pre-fix multiline regex selects the earlier blank line")
    (is (:ok result))
    (is (= expected (get-in result [:future-sources file])))))

;; @spec MCP-OP-INSERT-009
(deftest subform-comment-gap-still-refuses
  (let [source "(ns sample)\n(def xs [:a\n ;; belongs to :c\n :c])\n"
        result
        (transaction/compile-transaction
          {"src/sample.clj" source}
          {:changes [{:id :nested-comment
                      :in ["src/sample.clj"]
                      :forms ['xs]
                      :find ":c"
                      :do [:insert-left [":b"]]
                      :expect {:matches 1}}]
           :expect {:changes 1 :edits 1 :files 1}})]
    (is (= :ambiguous-insertion-gap (:error-type result)))
    (is (nil? (:future-sources result)))
    (is (not (true? (:write-authority result))))))

;; @spec MCP-OP-INSERT-009
(deftest named-owner-insertion-refuses-unowned-detached-top-level-source
  (let [source (str "(ns sample)\n\n"
                    "(defn owner-a [] :a)\n"
                    "#_(defn discarded-owner [] :discarded)\n"
                    "(defn owner-b [] :b)\n")
        result (compile-owner-insertion
                 source 'owner-b :insert-left
                 "(defn inserted-owner [] :inserted)")]
    (is (= :ambiguous-insertion-gap (:error-type result)))
    (is (nil? (:future-sources result)))
    (is (not (true? (:write-authority result))))))
