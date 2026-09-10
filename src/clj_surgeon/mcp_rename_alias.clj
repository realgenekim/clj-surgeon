(ns clj-surgeon.mcp-rename-alias
  (:require [clj-surgeon.rename-alias :as rename]
            [clj-surgeon.insert-forms :as insert]))
(defn handle [_exchange params callback]
  (let [receipt (rename/execute! params)]
    (callback [(insert/receipt-text receipt)] false receipt)))
