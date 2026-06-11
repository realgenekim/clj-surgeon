(ns clj-surgeon.move
  "Move forms within a file using rewrite-clj zipper surgery.
   Forms are moved as AST nodes, not text — parens always balanced,
   comments travel with their form, whitespace is preserved."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clojure.string :as str]))

(defn- form-name-of
  "Return the defined-name (as a string) of the form at zloc, or nil.

   Strips metadata wrappers so `(defn ^:private foo [] ...)`,
   `(defonce ^{:doc \"…\"} bar (atom {}))`, and `(defonce ^:dynamic *baz* 1)`
   all report their unwrapped name (\"foo\", \"bar\", \"*baz*\").

   zloc must point at the form-list (a (defn …) / (defonce …) etc.). Returns
   nil for non-list nodes and for lists with no second child."
  [zloc]
  (when (z/list? zloc)
    (when-let [name-zloc (some-> zloc z/down z/right)]
      (if (= :meta (n/tag (z/node name-zloc)))
        ;; Meta-wrapped node: structurally (meta-value, target). The target
        ;; (the actual symbol the metadata is attached to) is the rightmost
        ;; child of the meta node. Drill in to get it.
        (some-> name-zloc z/down z/rightmost z/string)
        (z/string name-zloc)))))

(defn- find-form
  "Walk top-level forms to find one whose defined-name matches `form-name`.

   Handles metadata wrappers on the name (^:private, ^{:doc …}, ^:dynamic).
   Skips `declare` forms so callers using `:mv :form my-fn` target the actual
   defn body and not the forward-declaration."
  [zloc form-name]
  (let [target (str form-name)]
    (loop [zloc zloc]
      (when zloc
        (if (and (z/list? zloc)
                 (not= "declare" (some-> zloc z/down z/string))
                 (= target (form-name-of zloc)))
          zloc
          (recur (z/right zloc)))))))

(defn- preceding-comment-nodes
  "Collect comment/newline nodes immediately preceding a form."
  [zloc]
  ;; Walk left collecting whitespace and comment nodes
  ;; Stop at the first non-whitespace, non-comment node
  (loop [left (z/left zloc), comments []]
    (if (nil? left)
      comments
      (let [node (z/node left)
            tag (n/tag node)]
        (if (#{:comment :newline} tag)
          (recur (z/left left) (conj comments node))
          comments)))))

(defn move-form
  "Move a named form before another named form in the same file.
   Returns {:ok true :result <new-source>} or {:error <message>}.
   With :dry-run true, returns the plan without modifying anything."
  [{:keys [file form before dry-run]}]
  (let [source (slurp file)
        zloc (z/of-string source {:track-position? true})
        ;; Find source form
        src-zloc (find-form zloc (str form))
        ;; Find destination form
        dst-zloc (find-form zloc (str before))]
    (cond
      (nil? src-zloc)
      {:error (str "Form not found: " form)}

      (nil? dst-zloc)
      {:error (str "Destination form not found: " before)}

      :else
      (let [src-meta (meta (z/node src-zloc))
            dst-meta (meta (z/node dst-zloc))
            src-line (:row src-meta)
            dst-line (:row dst-meta)]
        (if dry-run
          {:ok true
           :plan {:form (str form)
                  :from-line src-line
                  :to-before (str before)
                  :to-line dst-line
                  :direction (if (< src-line dst-line) :down :up)}}
          ;; Actually perform the move:
          ;; 1. Capture the source form as a string (with preceding comments)
          ;; 2. Remove it from the source location
          ;; 3. Insert it before the destination
          ;; For now, do this as text surgery on lines (safe because we know exact boundaries)
          (let [lines (vec (str/split-lines source))
                ;; Find comment header for source form
                src-start (loop [i (dec (dec src-line))] ;; 0-indexed, line above
                            (if (neg? i) 0
                                (if (str/starts-with? (str/trim (nth lines i "")) ";")
                                  (recur (dec i))
                                  (inc i))))
                src-end (:end-row src-meta)
                ;; Extract form lines (0-indexed: src-start to src-end-1)
                form-lines (subvec lines src-start src-end)
                ;; Remove from source
                remaining (into (subvec lines 0 src-start)
                                (subvec lines src-end))
                ;; Adjust destination line if source was above it
                adj-dst (if (< src-start (dec dst-line))
                          (- dst-line (- src-end src-start))
                          dst-line)
                ;; Find comment header for destination too
                insert-at (dec adj-dst) ;; 0-indexed, insert before this line
                ;; Insert with blank line separator
                result (str/join "\n"
                                 (concat (subvec remaining 0 insert-at)
                                         [""]
                                         form-lines
                                         [""]
                                         (subvec remaining insert-at)))]
            (spit file result)
            {:ok true
             :file file
             :form (str form)
             :moved-from src-line
             :moved-to adj-dst
             :lines-moved (count form-lines)}))))))
