(ns clj-splice.core
  "Original-source UTF-8 intervals over validated Unicode strings. No edit policy."
  (:require [rewrite-clj.parser :as parser]
            [rewrite-clj.node :as node])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]))

(defn- fail! [category data]
  (throw (ex-info (name category) (assoc data :clj-splice/category category))))

(defn- index-source [source]
  (when-not (string? source) (fail! :clj-splice/invalid-input {}))
  (let [size (count source)]
    (loop [i 0 offset 0 offsets (transient []) lines [0]]
      (if (= i size)
        {:utf16->byte (persistent! (conj! offsets offset)) :line-starts lines
         :bytes (.getBytes ^String source StandardCharsets/UTF_8)}
        (let [c (int (.charAt ^String source i))
              next-c (when (< (inc i) size) (int (.charAt ^String source (inc i))))
              pair? (<= 0xD800 c 0xDBFF)
              _ (when (or (and pair? (not (and next-c (<= 0xDC00 next-c 0xDFFF))))
                          (<= 0xDC00 c 0xDFFF))
                  (fail! :clj-splice/invalid-unicode {:utf16-index i}))
              width (if pair? 2 1)
              cost (cond pair? 4 (< c 128) 1 (< c 2048) 2 :else 3)
              newline-pair? (and (= c 13) (#{10 12} next-c))
              lines (cond newline-pair? (conj lines (+ i 2))
                          (= c 13) (conj lines (inc i))
                          (and (= c 10) (or (zero? i) (not= 13 (int (.charAt ^String source (dec i))))))
                          (conj lines (inc i))
                          :else lines)]
          (recur (+ i width) (+ offset cost)
                 (cond-> (conj! offsets offset) pair? (conj! nil)) lines))))))

(defn- coordinate [index row col]
  (let [start (when (and (integer? row) (pos? row)) (get (:line-starts index) (dec row)))
        i (when (and start (integer? col) (pos? col)) (+ start (dec col)))
        b (when i (get (:utf16->byte index) i))]
    (when-not b (fail! :clj-splice/invalid-coordinate {:row row :col col}))
    i))

(defn- digest [bs start end]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (.update md ^bytes bs (int start) (int (- end start)))
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest md)))))

(def ^:private trivia #{:whitespace :newline :comment :comma})

;; INTENT: SPLICE-001
;; @spec SPLICE-001: intervals and gaps describe original bytes, never rendering.
(defn spans
  "Return source once, a node table keyed by preorder ID, physical/effective root
  IDs, N+1 gap intervals and UTF-16/UTF-8 coordinate data. :start/:end are bytes;
  :utf16-start/:utf16-end support consumers that slice Unicode strings.
  Synthetic prefixes carry explicit provenance; no owner classifications."
  [source]
  (let [index (index-source source)
        root (try (parser/parse-string-all source)
                  (catch Exception e
                    (throw (ex-info "Source parse failed"
                                    (assoc (ex-data e) :clj-splice/category :clj-splice/parse-error) e))))
        table (atom [])]
    (letfn [(visit [n parent parent-start parent-tag]
              (let [id (count @table) tag (node/tag n) m (meta n)
                    synthetic (when (and (not= tag :forms) (not (:row m)))
                                (cond (= tag :map-qualifier) :map-qualifier
                                      (and (= parent-tag :reader-macro) (= tag :token)
                                           (#{"?" "?@"} (node/string n))) :conditional-prefix
                                      :else (fail! :clj-splice/unsupported-coordinate {:tag tag :parent parent})))
                    a (cond (= tag :forms) 0 synthetic (inc parent-start)
                            :else (coordinate index (:row m) (:col m)))
                    b (cond (= tag :forms) (count source)
                            synthetic (+ a (count (node/string n)))
                            :else (coordinate index (:end-row m) (:end-col m)))
                    _ (when (and synthetic (not= (subs source a b) (node/string n)))
                        (fail! :clj-splice/invalid-coordinate {:id id :tag tag :utf16-index a}))
                    entry (merge (select-keys m [:row :col :end-row :end-col])
                                 {:id id :parent parent :tag tag :start (get (:utf16->byte index) a)
                                  :end (get (:utf16->byte index) b) :utf16-start a :utf16-end b
                                  :synthetic synthetic
                                  :literal (cond (= tag :regex) :regex (= tag :multi-line) :string
                                                 (and (= tag :token) (< a b) (= \" (nth source a))) :string
                                                 :else nil)})]
                (swap! table conj entry)
                (let [children (if (node/inner? n) (mapv #(visit % id a tag) (node/children n)) [])]
                  (swap! table assoc id (assoc entry :children children)))
                id))]
      (visit root nil 0 nil)
      (let [nodes @table roots (filterv #(not (trivia (:tag (nodes %)))) (:children (nodes 0)))
            effective (filterv #(not= :uneval (:tag (nodes %))) roots)
            gaps (mapv (fn [a b] {:start a :end b})
                       (into [0] (map #(:end (nodes %)) roots))
                       (conj (mapv #(:start (nodes %)) roots) (alength ^bytes (:bytes index))))
            nodes (reduce (fn [table id]
                            (let [{:keys [start end]} (table id)]
                              (assoc-in table [id :sha256] (digest (:bytes index) start end)))) nodes roots)]
        (merge (dissoc index :bytes)
               {:source source :nodes nodes :roots roots :effective-roots effective
                :effective-count (count effective) :gaps gaps})))))

(defn- splice-edits [source edits]
  (let [index (index-source source) size (alength ^bytes (:bytes index))
        _ (doseq [[interval replacement] edits]
            (when-not (and (vector? interval) (= 2 (count interval))
                           (every? integer? interval) (<= 0 (first interval) (second interval) size))
              (fail! :clj-splice/invalid-interval {:interval interval :size size}))
            (index-source replacement))
        edits (->> edits
                   (map-indexed (fn [i [[start end] :as edit]] [[start end i] edit]))
                   (sort-by first)
                   (map second))
        wanted (set (mapcat first edits))
        endpoints (into {} (keep-indexed (fn [i b] (when (and b (wanted b)) [b i]))
                                         (:utf16->byte index)))]
    (loop [pending edits previous 0 parts []]
      (if-let [[[start end] replacement] (first pending)]
        (do
          (when-not (and (integer? start) (integer? end) (<= 0 start end size)
                         (<= previous start))
            (fail! :clj-splice/invalid-interval {:start start :end end :size size}))
          (let [a (get endpoints start) b (get endpoints end)]
            (when-not (and a b) (fail! :clj-splice/encoding-boundary {:start start :end end}))
            (recur (next pending) end
                   (conj parts (subs source (if (zero? previous) 0 (endpoints previous)) a) replacement))))
        (apply str (conj parts (subs source (if (zero? previous) 0 (endpoints previous)))))))))

;; INTENT: SPLICE-002
;; @spec SPLICE-002: only the authorized half-open byte intervals change.
(defn splice
  "Replace [start,end) in source using UTF-8 byte coordinates. Equal endpoints
  insert. The two-argument batch accepts [[interval replacement] ...] against one
  snapshot, sorted by (start, end, argument-index). Equal points insert in argument
  order. A pair overlaps exactly when end > other.start and start < other.end;
  overlap refuses regardless of argument order. Points at interval boundaries
  are allowed; points strictly inside intervals refuse. All endpoints must be
  scalar boundaries."
  ([source interval replacement] (splice-edits source [[interval replacement]]))
  ([source edits] (splice-edits source edits)))

(defn recount
  "Parse candidate once; return spans restricted to the structural projection:
  :nodes, :roots, :effective-roots, :effective-count and :gaps.
  Source and coordinate-index tables are omitted; node coordinates are retained."
  [candidate]
  (select-keys (spans candidate) [:nodes :roots :effective-roots :effective-count :gaps]))
