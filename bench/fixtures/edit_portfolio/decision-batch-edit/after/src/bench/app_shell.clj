(ns bench.app-shell)

;; This fixture is minimized from a real multi-surface viewer change.
(defn ide-shell [project-id]
  [:html
   [:head
    [:link {:href (#(str "/assets" %) "/command-center.css")}]]
   [:body.ide-shell-page
    [:main {:data-project project-id}
     "The text :body is not a structural tag."]]])

(defn email-shell []
  [:html
   [:head
    [:link {:href (#(str "/assets" %) "/app.css")}]]
   [:body
    [:main "Email"]]])
