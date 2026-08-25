(ns sample.server
  (:require
   [sample.http :as http]
   [sample.store :as store]))

(defn keep-health
  [request]
  {:status 200 :body (store/health request)})

;; Dev-only SSE details belong with the extracted dev handlers.
(defn- visible-sse-registrations
  [registrations]
  (mapv #(select-keys % [:id :path]) registrations))

(defn handle-sse-state
  [_request]
  {:status 200
   :body (visible-sse-registrations @http/registrations)})

(defn keep-home
  [_request]
  {:status 200 :body "home"})

;; Hot reload is a development concern.
(defn handle-reload-check
  [request]
  {:status 200 :body (http/reload-token request)})

;; Browser telemetry is accepted only by the development surface.
(defn handle-telemetry-beacon
  [request]
  (http/accept-beacon request))

(def routes
  [["/health" {:get keep-health}]
   ["/" {:get keep-home}]
   ["/dev/reload-check" {:get handle-reload-check}]
   ["/dev/telemetry" {:post handle-telemetry-beacon}]
   ["/dev/sse-state" {:get handle-sse-state}]
   ["/dev/sse-visible" {:get visible-sse-registrations}]])

(defn keep-start
  []
  (http/start routes))
