(ns myapp.ui.dashboard
  "Dashboard — mirrors the divergent dom/dom-server alias pattern."
  (:require
   [clojure.string :as str]
   #?(:cljs [fulcro.dom :as dom]
      :clj  [fulcro.dom-server :as dom])
   #?@(:cljs [[semantic-ui.modal :refer [ui-modal]]
              [semantic-ui.modal-header :refer [ui-modal-header]]])))

;; ---- Shared helpers ----

(defn format-title [title]
  (str/upper-case (or title "Untitled")))

(defn- render-header [title]
  (dom/div {:class "header"} (format-title title)))

;; ---- CLJS-only: browser rendering ----

#?(:cljs
   (defn render-modal [title content]
     (ui-modal {:open true}
               (ui-modal-header {} (render-header title))
               (dom/div {} content))))

#?(:cljs
   (defn render-dashboard [this props]
     (dom/div {:class "dashboard"}
              (render-header (:title props))
              (render-modal "Details" (:body props)))))

;; ---- CLJ-only: server-side rendering ----

#?(:clj
   (defn render-page [title body]
     (dom/div {}
              (render-header title)
              (dom/div {:class "content"} body))))
