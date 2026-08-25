(ns sample.server
  (:require
   [sample.handlers.public-cfp :as public-cfp]
   [sample.http :as http]
   [sample.store :as store]))

(defn keep-health
  [request]
  {:status 200 :body (store/health request)})

(def routes
  [["/health" {:get keep-health}]
   ["/cfp/:slug" {:get public-cfp/handle-public-cfp}]
   ["/cfp/:slug/submitted/:submission-id" {:get public-cfp/handle-cfp-submitted}]
   ["/api/cfp/:slug/submit" {:post public-cfp/handle-cfp-submit}]
   ["/api/cfp/:slug/import-sessionize" {:post public-cfp/handle-cfp-import}]
   ["/api/cfp/:slug/import-live" {:post public-cfp/handle-cfp-import-live}]
   ["/api/cfp/:slug/draft" {:post public-cfp/handle-cfp-draft}]
   ["/api/cfp/:slug/stream" {:get public-cfp/handle-cfp-stream}]])

(defn keep-start
  []
  (http/start routes))
