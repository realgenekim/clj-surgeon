(ns acme.checkout-client
  (:require
   [acme.checkout-policy :as policy]))

(defn request-options []
  (assoc (policy/resilience-budget)
         :client-profile "resilient"))
