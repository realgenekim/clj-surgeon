(ns fixtures.containing-line-owner)

(defmacro defcache [& _]
  nil)

(declare old-reader send-result)

(defcache 'first-cache '[account-id]
  '(let [reader (old-reader account-id)]
     (send-result reader)))

;; This comment is attached to the selected top-level form.
(defcache 'selected-cache '[account-id]
  '(let [reader (old-reader account-id)]
     ;; Preserve this comment and the multiline let layout.
     (send-result reader)))

(defcache 'final-cache '[account-id]
  '(let [reader (old-reader account-id)]
     (send-result reader)))
