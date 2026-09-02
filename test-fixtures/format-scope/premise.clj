(ns fixture.premise
  (:require [zzz.last :as z]
            ;; this comment must travel with the clause it precedes
            [aaa.first :as a])
  (:import (java.util Date)
           (java.io File)))

;; a comment block between forms
;; that the scoped formatter must never see

(defn authorize [user]
  ;;no space after the semicolons, which 0.29.0 rewrites
  (if (admin? user) (grant) (deny)))

(defn balance [credit debit]
      (- credit debit))

(defn touch []
  [(Date.) (File. "x") ;;an end-of-line comment, inside the form
   (a/one) (z/two)])

(defn doc-lines []
  "line one
     line two with   runs of spaces
  line three")
