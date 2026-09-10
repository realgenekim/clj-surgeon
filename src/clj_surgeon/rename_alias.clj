(ns clj-surgeon.rename-alias
  (:require [clj-surgeon.insert-forms :as insert]))
(def read-request insert/read-request)

(defn plan [_sources _request]
  {:state "refused" :committed false :mutation_attempted false :source_unchanged true
   :ok false :version 1 :operation "rename_alias" :error-type :not-implemented
   :at [] :error "rename_alias v1 witnesses precede implementation."
   :next_action "revise-request" :remedy "Wait for the witnessed implementation."})
(defn execute!
  ([request] (plan nil request))
  ([request _hooks] (execute! request)))
