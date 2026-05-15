(ns clj-surgeon.outline
  "Parse a Clojure file and return structured outline of all top-level forms.
   For CLJC files (and any file containing reader conditionals), forms inside
   #?(:clj ...) / #?@(:cljs [...]) are surfaced too, each tagged with the
   platforms it applies to."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clj-surgeon.cljc.walk :as cwalk]
            [clj-surgeon.forms :as forms]
            [clojure.string :as str]))

(defn- resolve-user-fields
  "Run each user-supplied extractor fn against the form zloc. Returns a
   map field-key -> value, omitting nil results (extractor signaled
   absence).

   When an extractor throws, attach context so the user can find the
   broken field — which macro, which field key, which form line. Then
   re-throw."
  [user-fields zloc type-str line]
  (into {} (for [[k f] user-fields
                 :let [v (try (f zloc)
                              (catch Exception e
                                (throw (ex-info
                                        (str ".clj-surgeon.edn: extractor for "
                                             type-str " :fields " k
                                             " threw at line " line ": "
                                             (.getMessage e))
                                        {:macro type-str
                                         :field k
                                         :line line}
                                        e))))]
                 :when (some? v)]
             [k v])))

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

(defn- collapse-ws [s]
  (when s (str/replace s #"\s+" " ")))

(defn- extract-arglist
  "Get arglist from a defn form. Descends into :meta nodes so meta-tagged
   arglists like `^String [k]` are found — extract-name handles meta the
   same way. Source whitespace (including newlines) is collapsed to single
   spaces — :ls output should be one line per form."
  [zloc]
  (let [type-str (some-> zloc z/down z/string)]
    (when (forms/has-arglists? type-str)
      (loop [child (some-> zloc z/down)]
        (when child
          (let [tag (n/tag (z/node child))]
            (cond
              (= :vector tag) (collapse-ws (z/string child))
              (= :meta tag)   (let [inner (some-> child z/down z/rightmost)]
                                (if (and inner (= :vector (n/tag (z/node inner))))
                                  (collapse-ws (z/string inner))
                                  (recur (z/right child))))
              :else           (recur (z/right child)))))))))

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
        ;; .clj / .cljs files have a single implicit platform; suppress
        ;; the :platforms key for those. Only emit when the file is .cljc
        ;; (where reader conditionals can split forms across platforms).
        show-platforms? (= "cljc" ext)
        forms  (mapv (fn [{:keys [zloc platforms]}]
                       (let [node (z/node zloc)
                             m (meta node)
                             type-str (some-> zloc z/down z/string)
                             spec (forms/spec type-str)
                             user-fields (:fields spec)
                             extracted (when user-fields
                                         (resolve-user-fields user-fields zloc
                                                              type-str (:row m)))
                             ;; If user provided :fields, respect their
                             ;; complete spec — don't fall back to legacy
                             ;; extractors for fields they didn't declare.
                             name-val (cond
                                        user-fields (:name extracted)
                                        (forms/defining-form? type-str)
                                        (extract-name zloc))
                             arglist (cond
                                       user-fields (:arglist extracted)
                                       name-val (extract-arglist zloc))
                             form-line (:row m)
                             comment-start (when form-line
                                             (preceding-comments lines form-line))
                             ;; Extra user-declared fields (everything except
                             ;; :name and :arglist which are already merged)
                             extras (when extracted
                                      (dissoc extracted :name :arglist))]
                         (cond-> {:type (symbol (or type-str "?"))}
                           show-platforms? (assoc :platforms (vec (sort platforms)))
                           form-line (assoc :line form-line)
                           (:end-row m) (assoc :end-line (:end-row m))
                           name-val (assoc :name (if (symbol? name-val)
                                                   name-val
                                                   (symbol (str name-val))))
                           arglist (assoc :args arglist)
                           (seq extras) (merge extras)
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
     :form-count (count (filter #(forms/defining-form? (str (:type %))) forms))
     :forms (vec (remove #(= 'ns (:type %)) forms))
     :forward-refs []})) ;; forward-refs filled in by core with clj-kondo data
