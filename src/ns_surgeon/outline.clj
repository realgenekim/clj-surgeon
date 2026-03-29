(ns ns-surgeon.outline
  "Parse a Clojure file and return structured outline of all top-level forms."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clojure.string :as str]))

(def ^:private def-types
  "Top-level defining forms we care about."
  #{"def" "defn" "defn-" "defonce" "defmacro" "defmethod" "defmulti"
    "defprotocol" "defrecord" "deftype" "declare"
    ">defn" ">defn-"})

(defn- def-form? [type-str]
  (contains? def-types type-str))

(defn- extract-name
  "Get the name from the second child of a form. Handles metadata like ^:private.
   Walks past meta nodes to find the actual symbol name."
  [zloc]
  (loop [child (some-> zloc z/down z/right)]
    (when child
      (let [s (z/string child)
            tag (n/tag (z/node child))]
        ;; Skip metadata nodes (^:private, ^:dynamic, ^String, etc.)
        (if (= :meta tag)
          ;; Meta node wraps the actual symbol — get the last child
          (let [inner (some-> child z/down z/rightmost z/string)]
            (or inner s))
          ;; Regular symbol
          (if (or (= :token tag) (= :symbol tag))
            s
            (recur (z/right child))))))))

(defn- extract-arglist
  "Get arglist from a defn form."
  [zloc]
  (let [type-str (some-> zloc z/down z/string)]
    (when (contains? #{"defn" "defn-" ">defn" ">defn-"} type-str)
      ;; Walk children to find first vector (the arglist)
      (loop [child (some-> zloc z/down)]
        (when child
          (if (z/vector? child)
            (z/string child)
            (recur (z/right child))))))))

(defn- preceding-comments
  "Look backwards from a form's start line to find attached comment lines.
   Comments must be contiguous (no blank lines between them and the form)."
  [lines form-line]
  (let [idx (dec form-line)] ;; 0-indexed
    (loop [i (dec idx), comment-start form-line]
      (if (neg? i)
        comment-start
        (let [line (str/trim (nth lines i ""))]
          (if (str/starts-with? line ";")
            (recur (dec i) (inc i)) ;; 1-indexed line number
            comment-start))))))

(defn outline
  "Return outline of all top-level forms in a Clojure file.
   Returns EDN map with :ns, :file, :lines, :forms, :forward-refs."
  [file]
  (let [source (slurp file)
        lines (str/split-lines source)
        total-lines (count lines)
        zloc (z/of-string source {:track-position? true})
        ;; Collect all top-level forms
        forms (loop [zloc zloc, acc []]
                (if (nil? zloc)
                  acc
                  (let [node (z/node zloc)
                        m (meta node)]
                    (if (and (z/list? zloc) m)
                      (let [type-str (some-> zloc z/down z/string)
                            name-str (when (def-form? type-str)
                                       (extract-name zloc))
                            arglist (when name-str (extract-arglist zloc))
                            form-line (:row m)
                            comment-start (preceding-comments lines form-line)]
                        (recur (z/right zloc)
                               (conj acc
                                     (cond-> {:type (symbol (or type-str "?"))
                                              :line form-line
                                              :end-line (:end-row m)}
                                       name-str (assoc :name (symbol name-str))
                                       arglist (assoc :args arglist)
                                       (< comment-start form-line)
                                       (assoc :comment-start comment-start)))))
                      (recur (z/right zloc) acc)))))
        ;; Build definition line lookup
        def-lines (into {}
                        (for [f forms :when (:name f)]
                          [(:name f) (:line f)]))
        ;; Extract ns name (special case — ns form name is always the direct second child)
        ns-name (some-> zloc
                        (z/find-value z/next 'ns)
                        z/up       ;; back to (ns ...)
                        z/down     ;; ns
                        z/right    ;; writer.state
                        z/string
                        symbol)]
    {:ns ns-name
     :file file
     :lines total-lines
     :form-count (count (filter :name forms))
     :forms (vec (remove #(= 'ns (:type %)) forms))
     :forward-refs []})) ;; forward-refs filled in by core with clj-kondo data
