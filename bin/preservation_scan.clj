;; preservation_scan.clj — an INDEPENDENT Clojure source scanner.
;;
;; Deliberately does not require anything from clj-surgeon's src/. The whole
;; point of the preservation brief is that its evidence is re-derived by code
;; that does not share a defect with the transform under review, so this file
;; carries its own reader.
;;
;; It is a *textual* datum scanner, not the Clojure reader: it returns exact
;; byte spans for top-level forms and comments so that body byte-identity is
;; comparable, which `read-string` would destroy.
(ns preservation-scan
  (:require [clojure.string :as str]))

(def ^:private closers {\( \) \[ \] \{ \}})

(defn- ws? [^Character c]
  (or (Character/isWhitespace ^char c) (= c \,)))

(defn- terminator? [c]
  (or (ws? c) (contains? #{\( \) \[ \] \{ \} \" \; \'} c)))

(declare read-datum)

(defn- read-token ^long [^String s ^long i]
  (let [n (.length s)]
    (loop [j i]
      (if (or (>= j n) (and (> j i) (terminator? (.charAt s j))))
        j
        (recur (inc j))))))

(defn- read-string-lit ^long [^String s ^long i]
  ;; s[i] is the opening quote
  (let [n (.length s)]
    (loop [j (inc i)]
      (cond
        (>= j n) j
        (= (.charAt s j) \\) (recur (+ j 2))
        (= (.charAt s j) \") (inc j)
        :else (recur (inc j))))))

(defn- read-char-lit ^long [^String s ^long i]
  ;; s[i] is a backslash; at least one char belongs to the literal
  (let [n (.length s)]
    (if (>= (inc i) n)
      (inc i)
      (loop [j (+ i 2)]
        (if (or (>= j n) (terminator? (.charAt s j)))
          j
          (recur (inc j)))))))

(defn- skip-ws ^long [^String s ^long i cbuf]
  (let [n (.length s)]
    (loop [j i]
      (cond
        (>= j n) j
        (ws? (.charAt s j)) (recur (inc j))
        (= (.charAt s j) \;)
        (let [e (loop [k j] (if (or (>= k n) (= (.charAt s k) \newline)) k (recur (inc k))))]
          (vswap! cbuf conj {:start j :end e :text (subs s j e)})
          (recur e))
        :else j))))

(defn- read-coll ^long [^String s ^long i cbuf close]
  ;; s[i] is the opening delimiter
  (let [n (.length s)]
    (loop [j (inc i)]
      (let [j (skip-ws s j cbuf)]
        (cond
          (>= j n) j
          (= (.charAt s j) close) (inc j)
          ;; tolerate an unbalanced closer rather than running off the file
          (contains? #{\) \] \}} (.charAt s j)) (inc j)
          :else (let [e (read-datum s j cbuf)]
                  (recur (if (<= e j) (inc j) e))))))))

(defn- read-dispatch ^long [^String s ^long i cbuf]
  (let [n (.length s)
        c2 (when (< (inc i) n) (.charAt s (inc i)))]
    (cond
      (nil? c2) (inc i)
      (= c2 \") (read-string-lit s (inc i))                    ; #"regex"
      (= c2 \{) (read-coll s (inc i) cbuf \})                  ; #{set}
      (= c2 \() (read-coll s (inc i) cbuf \))                  ; #(fn)
      (= c2 \_) (let [j (skip-ws s (+ i 2) cbuf)
                      j (read-datum s j cbuf)                  ; the discarded datum
                      j (skip-ws s j cbuf)]
                  (if (or (>= j n) (contains? #{\) \] \}} (.charAt s j)))
                    j
                    (read-datum s j cbuf)))
      (= c2 \?) (let [j (+ i 2)
                      j (if (and (< j n) (= (.charAt s j) \@)) (inc j) j)
                      j (skip-ws s j cbuf)]
                  (read-datum s j cbuf))                       ; #?( ...) / #?@( ...)
      (= c2 \') (let [j (skip-ws s (+ i 2) cbuf)] (read-datum s j cbuf))
      (= c2 \=) (let [j (skip-ws s (+ i 2) cbuf)] (read-datum s j cbuf))
      (= c2 \#) (read-token s (+ i 2))                         ; ##Inf
      (= c2 \^) (let [j (skip-ws s (+ i 2) cbuf)
                      j (read-datum s j cbuf)
                      j (skip-ws s j cbuf)]
                  (read-datum s j cbuf))
      (= c2 \:) (let [j (read-token s (inc i))                 ; #:ns{...} / #::{...}
                      j (skip-ws s j cbuf)]
                  (read-datum s j cbuf))
      :else (let [j (read-token s (inc i))                     ; #inst "..." tagged
                  j (skip-ws s j cbuf)]
              (read-datum s j cbuf)))))

(defn read-datum
  "Read one datum beginning at i (s[i] must be the datum's first char).
   Returns the exclusive end index. Comments encountered are appended to cbuf."
  ^long [^String s ^long i cbuf]
  (let [n (.length s)]
    (if (>= i n)
      i
      (let [c (.charAt s i)]
        (cond
          (= c \") (read-string-lit s i)
          (= c \\) (read-char-lit s i)
          (contains? closers c) (read-coll s i cbuf (closers c))
          (= c \#) (read-dispatch s i cbuf)
          (contains? #{\' \` \@} c) (let [j (skip-ws s (inc i) cbuf)] (read-datum s j cbuf))
          (= c \~) (let [j (if (and (< (inc i) n) (= (.charAt s (inc i)) \@)) (+ i 2) (inc i))
                         j (skip-ws s j cbuf)]
                     (read-datum s j cbuf))
          (= c \^) (let [j (skip-ws s (inc i) cbuf)
                         j (read-datum s j cbuf)
                         j (skip-ws s j cbuf)]
                     (read-datum s j cbuf))
          :else (read-token s i))))))

(defn line-index
  "Vector of 0-based char offsets at which each 1-based line starts."
  [^String s]
  (persistent!
   (reduce (fn [acc i] (if (= (.charAt s i) \newline) (conj! acc (inc i)) acc))
           (transient [0])
           (range (.length s)))))

(defn line-of [idx ^long pos]
  (loop [lo 0 hi (dec (count idx))]
    (if (>= lo hi)
      (inc lo)
      (let [mid (quot (+ lo hi 1) 2)]
        (if (<= (long (nth idx mid)) pos) (recur mid hi) (recur lo (dec mid)))))))

(defn scan
  "Scan a whole source string. Returns
   {:forms [{:start :end :text :line}] :comments [{:start :end :text :line}]}"
  [^String s]
  (let [cbuf (volatile! [])
        n (.length s)
        idx (line-index s)
        forms (loop [i 0 acc []]
                (let [i (skip-ws s i cbuf)]
                  (if (>= i n)
                    acc
                    (let [e (read-datum s i cbuf)
                          e (if (<= e i) (inc i) e)]
                      (recur e (conj acc {:start i :end e :text (subs s i e)}))))))]
    {:forms (mapv #(assoc % :line (line-of idx (:start %))) forms)
     :comments (mapv #(assoc % :line (line-of idx (:start %))) @cbuf)}))

(defn children
  "Element texts of a collection form (s must start with its opening delimiter)."
  [^String s]
  (let [cbuf (volatile! [])
        n (.length s)]
    (loop [j 1 acc []]
      (let [j (skip-ws s j cbuf)]
        (if (or (>= j n) (contains? #{\) \] \}} (.charAt s j)))
          acc
          (let [e (read-datum s j cbuf)
                e (if (<= e j) (inc j) e)]
            (recur e (conj acc (subs s j e)))))))))

(defn strip-meta
  "Drop leading ^meta prefixes from a datum's text, returning the datum itself."
  [^String s]
  (let [cbuf (volatile! [])]
    (loop [t (str/trim s) guard 0]
      (if (or (> guard 8) (not (str/starts-with? t "^")))
        t
        (let [j (skip-ws t 1 cbuf)
              e (read-datum t j cbuf)
              rest' (str/trim (subs t (min (count t) e)))]
          (recur rest' (inc guard)))))))

(defn meta-prefix
  "The ^meta prefixes that strip-meta would drop, as one string."
  [^String s]
  (let [t (str/trim s) d (strip-meta s)]
    (str/trim (subs t 0 (max 0 (- (count t) (count d)))))))

(def def-heads
  #{"def" "defn" "defn-" "defmacro" "defmulti" "defmethod" "defprotocol"
    "defrecord" "deftype" "definterface" "defonce" "definline" "deftest"
    "deftest-" "defspec" "defstruct" "defentity" "defcomponent"})

(defn form-owner
  "Owner descriptor for a top-level form, or nil if the form defines no owner."
  [{:keys [text line start end]}]
  (when (str/starts-with? (str/triml text) "(")
    (let [cs (children text)
          head (some-> (first cs) str/trim)
          nm-raw (second cs)]
      (when (and head nm-raw (contains? def-heads head))
        (let [nm (strip-meta nm-raw)
              mp (meta-prefix nm-raw)
              dispatch (when (= head "defmethod") (nth cs 2 nil))
              private? (or (str/ends-with? head "-")
                           (str/includes? mp ":private")
                           (some->> (nth cs 2 nil) (re-find #":private\s+true") boolean))]
          {:kind head
           :name nm
           :dispatch dispatch
           :id (if dispatch (str head " " nm " " (str/trim dispatch)) nm)
           :private? (boolean private?)
           :meta mp
           :line line
           :start start
           :end end
           :text text})))))

(defn ns-form [forms]
  (first (filter #(let [cs (children (:text %))]
                    (= "ns" (some-> (first cs) str/trim)))
                 forms)))

(defn ns-name-of [nsf]
  (when nsf (some-> (second (children (:text nsf))) strip-meta str/trim)))

(defn- clause-of [nsf kw]
  (->> (children (:text nsf))
       (drop 2)
       (filter #(str/starts-with? (str/triml %) "("))
       (filter #(= kw (some-> (first (children %)) str/trim)))
       (mapcat #(rest (children %)))
       vec))

(defn requires-of
  "Ordered require specs of an ns form: [{:raw :lib :as :refer}]."
  [nsf]
  (when nsf
    (->> (concat (clause-of nsf ":require") (clause-of nsf ":use"))
         (map (fn [spec]
                (let [t (str/trim spec)]
                  (if (str/starts-with? t "[")
                    (let [cs (children t)
                          lib (some-> (first cs) str/trim)
                          m (loop [xs (rest cs) acc {}]
                              (if (< (count xs) 2)
                                acc
                                (recur (drop 2 xs)
                                       (assoc acc (str/trim (first xs)) (str/trim (second xs))))))]
                      {:raw t :lib lib :as (get m ":as") :refer (get m ":refer")})
                    {:raw t :lib t :as nil :refer nil}))))
         vec)))

(defn imports-of [nsf]
  (when nsf (mapv str/trim (clause-of nsf ":import"))))

(defn alias-map
  "alias-string -> lib-string for a file, including the file's own ns under nil."
  [nsf]
  (into {} (for [{:keys [lib as]} (requires-of nsf) :when as] [as lib])))

(defn refer-map
  "referred-symbol-string -> lib-string"
  [nsf]
  (into {} (for [{:keys [lib refer]} (requires-of nsf)
                 :when (and refer (str/starts-with? refer "["))
                 sym (children refer)]
             [(str/trim sym) lib])))


(defn masked
  "s with the *contents* of comments and string literals replaced by spaces, so a
   regex over the result cannot match text that is not code."
  ^String [^String s]
  (let [{:keys [forms comments]} (scan s)
        sb (StringBuilder. s)]
    (doseq [{:keys [start end]} comments]
      (doseq [i (range start end)] (.setCharAt sb i \space)))
    (doseq [{:keys [text start]} forms]
      (let [n (count ^String text)]
        (loop [j 0]
          (when (< j n)
            (let [c (.charAt ^String text j)]
              (cond
                (= c \") (let [e (read-string-lit text j)]
                           (doseq [k (range (inc j) (max (inc j) (dec e)))]
                             (.setCharAt sb (+ start k) \space))
                           (recur e))
                (= c \\) (recur (read-char-lit text j))
                (= c \;) (recur (loop [k j] (if (or (>= k n) (= (.charAt ^String text k) \newline)) k (recur (inc k)))))
                :else (recur (inc j))))))))
    (.toString sb)))

(def qualified-sym-re
  #"(?<![\w./*+!?<>=:-])([A-Za-z][\w.*+!?<>=$-]*)/([A-Za-z*+!?<>=$_-][\w.*+!?<>=$-]*)")

(defn qualified-sites-with-pos
  "Like qualified-sites but keeps every occurrence with its own position."
  [^String s]
  (let [{:keys [comments forms]} (scan s)
        masked (StringBuilder. s)]
    (doseq [{:keys [start end]} comments]
      (doseq [i (range start end)] (.setCharAt masked i \space)))
    (doseq [{:keys [text start]} forms]
      (let [n (count text)]
        (loop [j 0]
          (when (< j n)
            (let [c (.charAt ^String text j)]
              (cond
                (= c \") (let [e (read-string-lit text j)]
                           (doseq [k (range (inc j) (max (inc j) (dec e)))]
                             (.setCharAt masked (+ start k) \space))
                           (recur e))
                (= c \\) (recur (read-char-lit text j))
                (= c \;) (recur (loop [k j] (if (or (>= k n) (= (.charAt ^String text k) \newline)) k (recur (inc k)))))
                :else (recur (inc j))))))))
    (let [m (.toString masked)
          idx (line-index s)
          matcher (re-matcher qualified-sym-re m)]
      (loop [acc []]
        (if (.find matcher)
          (recur (conj acc {:alias (.group matcher 1)
                            :name (.group matcher 2)
                            :text (.group matcher 0)
                            :start (.start matcher)
                            :line (line-of idx (.start matcher))}))
          acc)))))

(def bare-sym-re
  #"(?<![\w./*+!?<>=$:-])([A-Za-z*+!?<>=$_-][\w*+!?<>=$-]*)(?![\w./*+!?<>=$-])")

(defn bare-symbols
  "Unqualified symbol-shaped tokens in a source string, outside strings/comments."
  [^String s]
  (let [{:keys [comments forms]} (scan s)
        masked (StringBuilder. s)]
    (doseq [{:keys [start end]} comments]
      (doseq [i (range start end)] (.setCharAt masked i \space)))
    (doseq [{:keys [text start]} forms]
      (let [n (count text)]
        (loop [j 0]
          (when (< j n)
            (let [c (.charAt ^String text j)]
              (cond
                (= c \") (let [e (read-string-lit text j)]
                           (doseq [k (range (inc j) (max (inc j) (dec e)))]
                             (.setCharAt masked (+ start k) \space))
                           (recur e))
                (= c \\) (recur (read-char-lit text j))
                (= c \;) (recur (loop [k j] (if (or (>= k n) (= (.charAt ^String text k) \newline)) k (recur (inc k)))))
                :else (recur (inc j))))))))
    (let [m (.toString masked)]
      (into #{} (map second) (re-seq bare-sym-re m)))))

(declare tokens*)

(defn tokens
  "Flatten source text into an ordered token stream: whitespace and indentation
   are discarded, everything else is kept verbatim. Comparing two token streams
   is how the brief decides body identity 'modulo the documented indentation
   rules' without a whitespace heuristic that could hide a real edit.

   Each token carries its absolute :start offset, and :row/:col when a line index
   is supplied, so clj-kondo's analysis can be joined to it by position."
  ([^String s] (tokens s nil))
  ([^String s idx]
   (let [raw (tokens* s)]
     (if (nil? idx)
       raw
       (mapv (fn [t]
               (let [r (line-of idx (:start t))
                     ls (nth idx (dec r))]
                 (assoc t :row r :col (inc (- (long (:start t)) (long ls))))))
             raw)))))

(defn- tokens* [^String s]
  (let [n (.length s)]
    (loop [i 0 acc []]
      (if (>= i n)
        acc
        (let [c (.charAt s i)]
          (cond
            (ws? c) (recur (inc i) acc)

            (= c \;)
            (let [e (loop [k i] (if (or (>= k n) (= (.charAt s k) \newline)) k (recur (inc k))))]
              (recur e (conj acc {:kind :comment :start i :text (str/trim (subs s i e))})))

            (= c \")
            (let [e (read-string-lit s i)] (recur e (conj acc {:kind :str :start i :text (subs s i e)})))

            (= c \\)
            (let [e (read-char-lit s i)] (recur e (conj acc {:kind :chr :start i :text (subs s i e)})))

            (contains? #{\( \) \[ \] \{ \}} c)
            (recur (inc i) (conj acc {:kind :delim :start i :text (str c)}))

            (= c \#)
            (let [c2 (when (< (inc i) n) (.charAt s (inc i)))]
              (cond
                (nil? c2) (recur (inc i) (conj acc {:kind :prefix :start i :text "#"}))
                (= c2 \") (let [e (read-string-lit s (inc i))]
                            (recur e (conj acc {:kind :regex :start i :text (subs s i e)})))
                (contains? #{\{ \( \_ \? \' \= \^ \:} c2)
                (recur (+ i 2) (conj acc {:kind :prefix :start i :text (subs s i (+ i 2))}))
                (= c2 \#) (let [e (read-token s (+ i 2))]
                            (recur e (conj acc {:kind :tok :start i :text (subs s i e)})))
                :else (let [e (read-token s (inc i))]
                        (recur e (conj acc {:kind :tag :start i :text (subs s i e)})))))

            (contains? #{\' \` \@ \^} c)
            (recur (inc i) (conj acc {:kind :prefix :start i :text (str c)}))

            (= c \~)
            (let [e (if (and (< (inc i) n) (= (.charAt s (inc i)) \@)) (+ i 2) (inc i))]
              (recur e (conj acc {:kind :prefix :start i :text (subs s i e)})))

            :else
            (let [e (read-token s i)
                  e (if (<= e i) (inc i) e)]
              (recur e (conj acc {:kind :tok :start i :text (subs s i e)})))))))))

;; ---------------------------------------------------------------- node tree
;;
;; A structural view of the same bytes. `tokens` above is a flat stream and
;; cannot say whether a symbol sits in a binding position, inside a quote, or
;; inside a macro form the scanner does not model. The canonicaliser needs that
;; distinction: a bare symbol may only be rewritten to a Var when it is provably
;; a Var reference. (PB-FENCE-001.)

(declare read-node)

(defn- read-node-seq [^String s ^long i cbuf close]
  ;; s[i] is the opening delimiter; returns [children end]
  (let [n (.length s)]
    (loop [j (inc i) acc []]
      (let [j (skip-ws s j cbuf)]
        (cond
          (>= j n) [acc j]
          (= (.charAt s j) close) [acc (inc j)]
          (contains? #{\) \] \}} (.charAt s j)) [acc (inc j)]
          :else (let [[node e] (read-node s j cbuf)
                      e (if (<= e j) (inc j) e)]
                  (recur e (conj acc node))))))))

(defn read-node
  "Read one datum at i as a node. Returns [node end]."
  [^String s ^long i cbuf]
  (let [n (.length s)
        c (.charAt s i)]
    (cond
      (= c \") (let [e (read-string-lit s i)] [{:kind :str :start i :text (subs s i e)} e])
      (= c \\) (let [e (read-char-lit s i)] [{:kind :chr :start i :text (subs s i e)} e])

      (contains? closers c)
      (let [[kids e] (read-node-seq s i cbuf (closers c))]
        [{:kind (case c \( :list \[ :vector \{ :map) :open (str c) :children kids
          :text (subs s i (min e n))} e])

      (= c \#)
      (let [c2 (when (< (inc i) n) (.charAt s (inc i)))]
        (cond
          (nil? c2) [{:kind :token :text "#"} (inc i)]
          (= c2 \") (let [e (read-string-lit s (inc i))] [{:kind :regex :start i :text (subs s i e)} e])
          (= c2 \{) (let [[kids e] (read-node-seq s (inc i) cbuf \})]
                      [{:kind :set :open "#{" :children kids :text (subs s i (min e n))} e])
          (= c2 \() (let [[kids e] (read-node-seq s (inc i) cbuf \))]
                      [{:kind :anon-fn :open "#(" :children kids :text (subs s i (min e n))} e])
          (= c2 \_) (let [j (skip-ws s (+ i 2) cbuf)
                          [d e] (read-node s j cbuf)
                          e (max e (inc j))
                          j2 (skip-ws s e cbuf)]
                      (if (or (>= j2 n) (contains? #{\) \] \}} (.charAt s j2)))
                        [{:kind :prefixed :prefix "#_" :children [d] :text (subs s i (min e n))} e]
                        (let [[d2 e2] (read-node s j2 cbuf)]
                          [{:kind :prefixed :prefix "#_" :children [d d2]
                            :text (subs s i (min e2 n))} e2])))
          (contains? #{\? \' \= \^ \:} c2)
          (let [j (skip-ws s (+ i 2) cbuf)
                j (if (and (= c2 \?) (< j n) (= (.charAt s j) \@)) (skip-ws s (inc j) cbuf) j)
                [d e] (read-node s j cbuf)
                e (max e (inc j))]
            [{:kind :prefixed :prefix (subs s i (+ i 2)) :children [d] :text (subs s i (min e n))} e])
          (= c2 \#) (let [e (read-token s (+ i 2))] [{:kind :token :text (subs s i e)} e])
          :else (let [j (read-token s (inc i))
                      j2 (skip-ws s j cbuf)
                      [d e] (read-node s j2 cbuf)]
                  [{:kind :prefixed :prefix (subs s i j) :children [d] :text (subs s i (min e n))} e])))

      (contains? #{\' \` \@} c)
      (let [j (skip-ws s (inc i) cbuf)
            [d e] (read-node s j cbuf)
            e (max e (inc j))]
        [{:kind :prefixed :prefix (str c) :children [d] :text (subs s i (min e n))} e])

      (= c \~)
      (let [j0 (if (and (< (inc i) n) (= (.charAt s (inc i)) \@)) (+ i 2) (inc i))
            j (skip-ws s j0 cbuf)
            [d e] (read-node s j cbuf)
            e (max e (inc j))]
        [{:kind :prefixed :prefix (subs s i j0) :children [d] :text (subs s i (min e n))} e])

      (= c \^)
      (let [j (skip-ws s (inc i) cbuf)
            [m e1] (read-node s j cbuf)
            e1 (max e1 (inc j))
            j2 (skip-ws s e1 cbuf)
            [d e2] (read-node s j2 cbuf)
            e2 (max e2 (inc j2))]
        [{:kind :prefixed :prefix "^" :children [m d] :text (subs s i (min e2 n))} e2])

      :else (let [e (read-token s i)
                  e (if (<= e i) (inc i) e)]
              [{:kind :token :text (subs s i e)} e]))))

(defn nodes
  "Top-level nodes of a source string."
  [^String s]
  (let [cbuf (volatile! []) n (.length s)]
    (loop [i 0 acc []]
      (let [i (skip-ws s i cbuf)]
        (if (>= i n)
          acc
          (let [[node e] (read-node s i cbuf)
                e (if (<= e i) (inc i) e)]
            (recur e (conj acc node))))))))

(defn node
  "The single node a datum's text represents, or nil."
  [^String s]
  (first (nodes s)))

(defn all-tokens
  "Every :token/:regex text in a node subtree."
  [nd]
  (if (contains? #{:token :regex} (:kind nd))
    [(:text nd)]
    (mapcat all-tokens (:children nd))))

(defn sym-token?
  "Does this token text look like a symbol (not a keyword, number, or nil/bool)?"
  [t]
  (boolean (and t (re-matches #"[A-Za-z*+!?<>=$_&.-][\w*+!?<>=$/.-]*" t)
                (not (contains? #{"nil" "true" "false" "&" "_"} t)))))
