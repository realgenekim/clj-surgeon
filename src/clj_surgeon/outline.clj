(ns clj-surgeon.outline
  "Parse a Clojure file and return structured outline of all top-level forms.
   For CLJC files (and any file containing reader conditionals), forms inside
   #?(:clj ...) / #?@(:cljs [...]) are surfaced too, each tagged with the
   platforms it applies to."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clj-surgeon.cljc.walk :as cwalk]
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

(defn- file-extension [file]
  (let [s (str file)
        i (.lastIndexOf s ".")]
    (when (pos? i) (subs s (inc i)))))

(defn outline
  "Return outline of all top-level forms in a Clojure file.
   Returns EDN map with :ns, :file, :lines, :forms, :forward-refs.

   Each form includes :platforms — the set of platforms (#{:clj}, #{:cljs},
   #{:clj :cljs}, etc.) under which it appears. For .clj/.cljs files this
   reflects the file extension; for .cljc files it surfaces reader-conditional
   structure, so a `#?(:clj (defn foo ...))` shows up as a real form with
   :platforms #{:clj}."
  [file]
  (let [source (slurp file)
        lines (str/split-lines source)
        total-lines (count lines)
        zloc (z/of-string source {:track-position? true})
        ext   (file-extension file)
        defaults (cwalk/platforms-for-extension ext)
        walked (cwalk/top-level-forms source defaults)
        forms  (mapv (fn [{:keys [zloc platforms]}]
                       (let [node (z/node zloc)
                             m (meta node)
                             type-str (some-> zloc z/down z/string)
                             name-str (when (def-form? type-str)
                                        (extract-name zloc))
                             arglist (when name-str (extract-arglist zloc))
                             form-line (:row m)
                             comment-start (when form-line
                                             (preceding-comments lines form-line))]
                         (cond-> {:type (symbol (or type-str "?"))
                                  :platforms (vec (sort platforms))}
                           form-line (assoc :line form-line)
                           (:end-row m) (assoc :end-line (:end-row m))
                           name-str (assoc :name (symbol name-str))
                           arglist (assoc :args arglist)
                           (and form-line comment-start (< comment-start form-line))
                           (assoc :comment-start comment-start))))
                     walked)
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
