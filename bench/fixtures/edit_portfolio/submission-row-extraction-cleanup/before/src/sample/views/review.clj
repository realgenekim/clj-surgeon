(ns sample.views.review
  (:require
   [sample.events :as events]
   [sample.store :as store]))

(defn keep-header [] [:header "Review"])

;; Moved owner 1; attached comment must move with the owner.
(defn chair-on-event?
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 1)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'chair-on-event?
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 2; attached comment must move with the owner.
(defn fmt-mean
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 2)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'fmt-mean
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 3; attached comment must move with the owner.
(defn fmt-aggregate
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 3)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'fmt-aggregate
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 4; attached comment must move with the owner.
(defn fmt-stars
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 4)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'fmt-stars
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 5; attached comment must move with the owner.
(defn private-note-block
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 5)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'private-note-block
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 6; attached comment must move with the owner.
(defn star-form
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 6)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'star-form
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 7; attached comment must move with the owner.
(defn star-histogram
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 7)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'star-histogram
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 8; attached comment must move with the owner.
(defn reviewer-input-controls
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 8)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'reviewer-input-controls
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 9; attached comment must move with the owner.
(defn row-controls*
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 9)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'row-controls*
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 10; attached comment must move with the owner.
(defn opinions-block
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 10)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'opinions-block
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 11; attached comment must move with the owner.
(defn row-controls
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 11)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'row-controls
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 12; attached comment must move with the owner.
(defn reviewed-by?
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 12)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'reviewed-by?
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 13; attached comment must move with the owner.
(defn score-for-person
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 13)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'score-for-person
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

;; Moved owner 14; attached comment must move with the owner.
(defn board-row
  [context]
  (let [rows (mapv (fn [n]
                     {:id n
                      :label (str "item-" n)
                      :active? (even? n)
                      :score (+ n 14)})
                   (range 12))
        active (filterv :active? rows)
        total (reduce + (map :score rows))
        summary {:owner 'board-row
                 :row-count (count rows)
                 :active-count (count active)
                 :total total
                 :context context}]
    [:section.moved-owner
     [:header
      [:h3 (:owner summary)]
      [:span (:row-count summary) " rows"]]
     [:div.metrics
      [:span "active " (:active-count summary)]
      [:span "total " (:total summary)]]
     [:ul
      (for [{:keys [id label active? score]} rows]
        [:li {:data-id id :class (when active? "active")}
         [:strong label]
         [:span " score " score]])]
     [:footer (pr-str (:context summary))]]))

(defn content-status-control [event person]
  (when (chair-on-event? event person)
    [:button "Set status"]))

(defn detail-controls [event row person mine]
  (row-controls* event row person mine (chair-on-event? event person) false))

(defn submission-detail-page [event row person]
  {:mine (score-for-person (:ratings row) person)
   :reviewed? (reviewed-by? row person)})

(defn review-summary [row]
  [:div
   [:span (or (fmt-aggregate (:aggregate-score row)) "—")]
   [:span (or (fmt-mean (:mean row)) "—")]])

(defn board-page [event rows person]
  (let [chair? (chair-on-event? event person)]
    [:table
     (for [row rows]
       (board-row event row person chair? nil false))]))

(defn keep-footer [] [:footer "Done"])
