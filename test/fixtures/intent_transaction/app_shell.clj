(ns fixture.app-shell)

;; Minimized from a field session that edited coordinated Clojure UI forms.
(defn ide-shell [project-id]
  [:html
   [:head
    [:link {:href (#(str "/assets" %) "/app.css")}]]
   [:body
    [:main {:data-project project-id}
     "The text :body is not a structural tag."]]])
