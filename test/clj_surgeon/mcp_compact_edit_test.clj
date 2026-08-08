(ns clj-surgeon.mcp-compact-edit-test
  (:require
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clojure.test :refer [deftest is testing]]))

(deftest delete-subform-source-preserves-concrete-syntax
  (doseq [[label source find expected]
          [["first child and contiguous leading comments"
            (str "(defn render []\n"
                 "  ;; belongs to the conditional\n"
                 "  (when visible? [:button \"Zoom\"])\n"
                 "  [:main])")
            "(when visible? [:button \"Zoom\"])"
            (str "(defn render []\n"
                 "  [:main])")]
           ["middle child without a comment"
            (str "(defn render []\n"
                 "  [:header]\n"
                 "  (when visible? [:button \"Zoom\"])\n"
                 "  [:main])")
            "(when visible? [:button \"Zoom\"])"
            (str "(defn render []\n"
                 "  [:header]\n"
                 "  [:main])")]
           ["last child and contiguous leading comments"
            (str "(defn render []\n"
                 "  [:main]\n"
                 "  ;; belongs to the conditional\n"
                 "  (when visible? [:button \"Zoom\"]))")
            "(when visible? [:button \"Zoom\"])"
            (str "(defn render []\n"
                 "  [:main])")]
           ["inline child"
            "(let [x 1] (inc x) x)"
            "(inc x)"
            "(let [x 1] x)"]
           ["same-line trailing comment belongs to deleted child"
            (str "(defn render []\n"
                 "  (when visible? [:button \"Zoom\"]) ;; obsolete control\n"
                 "  [:main])")
            "(when visible? [:button \"Zoom\"])"
            (str "(defn render []\n"
                 "  [:main])")]
           ["next child's leading comment remains"
            (str "(defn render []\n"
                 "  (when visible? [:button \"Zoom\"])\n"
                 "  ;; belongs to main\n"
                 "  [:main])")
            "(when visible? [:button \"Zoom\"])"
            (str "(defn render []\n"
                 "  ;; belongs to main\n"
                 "  [:main])")]
           ["blank line detaches a preceding comment"
            (str "(defn render []\n"
                 "  ;; section heading\n"
                 "\n"
                 "  (when visible? [:button \"Zoom\"])\n"
                 "  [:main])")
            "(when visible? [:button \"Zoom\"])"
            (str "(defn render []\n"
                 "  ;; section heading\n"
                 "\n"
                 "  [:main])")]]]
    (testing label
      (let [result (change-buffer/delete-subform-source source find)]
        (is (:ok result))
        (is (= expected (:source result)))))))

(deftest delete-subform-source-refuses-with-stable-data
  (doseq [[label source find error-type match-count]
          [["missing target" "(defn f [] (inc x))" "(dec x)" :no-match 0]
           ["ambiguous target" "(defn f [] (inc x) (inc x))" "(inc x)"
            :ambiguous-match 2]
           ["complete owner" "(defn f [] (inc x))" "(defn f [] (inc x))"
            :basis-edit-covers-owner nil]]]
    (testing label
      (let [result (change-buffer/delete-subform-source source find)]
        (is (false? (:ok result)))
        (is (= error-type (:error-type result)))
        (when (some? match-count)
          (is (= match-count (:match-count result))))))))

(deftest delete-subform-source-covers-the-field-shape
  ;; Minimized from the obsolete per-card video control that motivated compact
  ;; basis deletion. The surrounding Hiccup and attached comment are essential.
  (let [source
        (str "(defn post-card [post]\n"
             "  [:article.post\n"
             "   [:div.media]\n"
             "   [:footer]\n\n"
             "   ;; Native fullscreen supersedes this separate control.\n"
             "   (when (= (:media-type post) :video)\n"
             "     [:button.zoom {:type \"button\"} \"Zoom\"])])")
        find
        (str "(when (= (:media-type post) :video)\n"
             "  [:button.zoom {:type \"button\"} \"Zoom\"])")
        result (change-buffer/delete-subform-source source find)]
    (is (:ok result))
    (is (= (str "(defn post-card [post]\n"
                "  [:article.post\n"
                "   [:div.media]\n"
                "   [:footer]])")
           (:source result)))))
