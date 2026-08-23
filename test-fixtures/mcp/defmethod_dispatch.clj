(ns mcp.defmethod-dispatch)

(defmulti render :kind)

(defmethod render :card
  [item]
  [:article.card (:title item) :old])

(defmethod render :panel
  [item]
  [:section.panel (:title item) :old])
