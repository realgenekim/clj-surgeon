(ns bench.source-reader)

(defn source-reader-shell
  [project-id projects artifact current-location reader-region show-all?]
  [:html
   [:head
    [:title "Workbench"]]
   ;; Keep this explanation attached to the page body.
   [:body
    [:main {:data-project project-id
            :data-count (count projects)
            :data-show-all show-all?}
     [:span.tab-label artifact]
     [:section {:data-location current-location}
      reader-region]]]])

(defn static-reader-shell [artifact]
  [:html
   [:head [:title "Workbench"]]
   [:body [:span.tab-label artifact]]])
