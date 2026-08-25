(ns sample.server
  (:require
   [sample.handlers.dev :as dev]
   [sample.http :as http]
   [sample.store :as store]))

(defn keep-health
  [request]
  {:status 200 :body (store/health request)})

(defn keep-home
  [_request]
  {:status 200 :body "home"})

(def routes
  [["/health" {:get keep-health}]
   ["/" {:get keep-home}]
   ["/dev/reload-check" {:get dev/handle-reload-check}]
   ["/dev/telemetry" {:post dev/handle-telemetry-beacon}]
   ["/dev/sse-state" {:get dev/handle-sse-state}]
   ["/dev/sse-visible" {:get dev/visible-sse-registrations}]])

(defn keep-start
  []
  (http/start routes))
