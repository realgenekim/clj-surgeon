(ns acme.checkout-client
  (:require
   [acme.checkout-policy :as policy]))

(defn request-options []
  (assoc (policy/retry-budget)
         :client-profile "legacy"))
