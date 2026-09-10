(ns clj-surgeon.insert-forms)

(defn not-implemented []
  {:state "refused" :committed false :mutation_attempted false
   :source_unchanged true :ok false :operation "insert_forms"
   :error-type :not-implemented :error "insert_forms is not implemented."
   :next_action "revise-request"})

(defn plan [_source _request] (not-implemented))
(defn execute! ([_request] (not-implemented)) ([_request _hooks] (not-implemented)))
(defn read-request [_text] (not-implemented))
