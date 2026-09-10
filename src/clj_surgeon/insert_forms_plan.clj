(ns clj-surgeon.insert-forms-plan
  "Pure, byte-preserving insertion planning. CST nodes supply coordinates only."
  (:refer-clojure :exclude [bytes])
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser])
  (:import
   (java.io PushbackReader StringReader)
   (java.nio ByteBuffer)
   (java.nio.charset CodingErrorAction StandardCharsets)
   (java.security MessageDigest)))

(def limits {:request 2097152 :source 8388608 :payload 1048576 :forms 1000 :depth 512})
(defn bytes [^String s] (.getBytes s StandardCharsets/UTF_8))
(defn sha [s]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (if (string? s) (bytes s) s)))))
(defn refuse! [kind at message & [data]]
  (throw (ex-info message (merge {:error-type kind :at at} data))))
(defn refusal [e]
  (merge {:state "refused" :committed false :mutation_attempted false
          :source_unchanged true :ok false :operation "insert_forms"
          :error (.getMessage ^Exception e)
          :next_action (if (#{:source-hash-mismatch :source-changed-before-commit}
                            (:error-type (ex-data e))) "refresh-source" "revise-request")
          :remedy "Revise the field named by at using the reported contract."}
         (ex-data e)))
(defn bounded! [s kind]
  (when (> (alength (bytes s)) (limits kind))
    (refuse! :limit-exceeded [kind] "Input exceeds byte limit."
             {:expected (limits kind) :actual (alength (bytes s))})))
(defn decode [bs kind]
  (try
    (str (.decode (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
           (ByteBuffer/wrap bs)))
    (catch Exception _ (refuse! (if (= kind :request) :invalid-request :unsupported-source)
                         [kind] "Strict UTF-8 required."))))

;; Lexical preflight is bounded before the recursive CST parser. It never reads data.
;; @spec INSERT-FORMS-011
;; INTENT: INSERT-FORMS-011
(defn lexical! [^String s kind]
  (bounded! s kind)
  (let [size (count s) unsupported (if (= kind :source) :unsupported-source
                                     (if (= kind :request) :invalid-request :unsupported-payload-syntax))]
    (when (str/starts-with? s "\uFEFF") (refuse! unsupported [kind] "BOM is unsupported."))
    (loop [i 0 mode :code depth 0 literal-start nil literals []]
      (if (>= i size)
        literals
        (let [c (.charAt s i) next-c (when (< (inc i) size) (.charAt s (inc i)))]
          (case mode
            :comment (recur (inc i) (if (#{\newline \return} c) :code :comment) depth nil literals)
            :string (cond
                      (= c \\) (recur (+ i 2) :string depth literal-start literals)
                      (= c \") (recur (inc i) :code depth nil (conj literals [literal-start (inc i)]))
                      :else (recur (inc i) :string depth literal-start literals))
            :code (cond
                    (= c \;) (recur (inc i) :comment depth nil literals)
                    (= c \") (recur (inc i) :string depth i literals)
                    (= c \\) (let [end (loop [j (min size (+ i 2))]
                                         (if (and (< j size)
                                                  (not (or (Character/isWhitespace (.charAt s j))
                                                           (contains? #{\( \) \[ \] \{ \} \" \;} (.charAt s j)))))
                                           (recur (inc j)) j))]
                               (recur end :code depth nil literals))
                    (and (= c \#) (or (and (= kind :request) (not (#{\{ \:} next-c)))
                                       (#{\= \?} next-c)
                                      (and (= next-c \_) (not= kind :source))))
                    (refuse! unsupported [kind] "Unsupported reader syntax.")
                    (#{\( \[ \{} c)
                    (do (when (>= depth (:depth limits))
                          (refuse! :limit-exceeded [kind] "CST depth exceeds 512."))
                        (recur (inc i) :code (inc depth) nil literals))
                    (#{\) \] \}} c) (recur (inc i) :code (dec depth) nil literals)
                    :else (recur (inc i) :code depth nil literals))))))))

(defn read-request [text]
  (try
    (lexical! text :request)
    (with-open [r (PushbackReader. (StringReader. text))]
      (let [eof (Object.) opts {:eof eof :readers {}
                                :default (fn [& _] (refuse! :invalid-request [] "Tagged EDN is forbidden."))}
            value (edn/read opts r)]
        (when-not (and (map? value) (identical? eof (edn/read opts r)))
          (refuse! :invalid-request [] "Request must contain exactly one EDN map."))
        value))
    (catch Exception e
      (refusal (if (:error-type (ex-data e)) e
                 (ex-info "Invalid EDN request." {:error-type :invalid-request :at []}))))))

(defn closed! [m required optional at kind]
  (when-not (and (map? m) (every? #(contains? m %) required)
                 (every? (into (set required) optional) (keys m)))
    (refuse! kind at "Closed map has missing or unsupported fields.")))
(defn positive! [x at]
  (when-not (and (integer? x) (pos? x)) (refuse! :invalid-request at "Positive integer required.")))
(defn validate! [r]
  (bounded! (pr-str r) :request)
  (closed! r [:version :workspace_root :file :guard :anchor :payload] [] [] :invalid-request)
  (when-not (and (= 1 (:version r)) (integer? (:version r))
                 (string? (:workspace_root r)) (string? (:file r)))
    (refuse! :invalid-request [] "Version 1 and string paths required."))
  (let [{:keys [anchor payload]} r
        body? (= "body" (:scope anchor))]
    (closed! anchor (if body? [:scope :owner :expect :boundary] [:scope :owner :expect :position])
             (if body? [:testing_path :arity] []) [:anchor] :invalid-request)
    (closed! (:owner anchor) [:kind :name] [] [:anchor :owner] :invalid-request)
    (when-not (and (#{"top-level" "body"} (:scope anchor)) (= 1 (:expect anchor))
                   (integer? (:expect anchor))
                   (#{"def" "defn" "defn-" "deftest" "ns"} (get-in anchor [:owner :kind]))
                   (string? (get-in anchor [:owner :name]))
                   (try (let [text (get-in anchor [:owner :name]) value (edn/read-string text)]
                          (and (symbol? value) (= text (str value))))
                        (catch Exception _ false)))
      (refuse! :invalid-request [:anchor] "Unsupported scope, owner, or expectation."))
    (if body?
      (let [b (:boundary anchor) after? (= "after-child" (:position b))]
        (closed! b (if after? [:position :child] [:position]) [] [:anchor :boundary] :invalid-request)
        (when-not (#{"first" "last" "after-child"} (:position b))
          (refuse! :invalid-request [:anchor :boundary] "Unknown body boundary."))
        (when after? (positive! (:child b) [:anchor :boundary :child]))
        (when (contains? anchor :arity) (positive! (:arity anchor) [:anchor :arity]))
        (when-not (vector? (get anchor :testing_path []))
          (refuse! :invalid-request [:anchor :testing_path] "Testing path must be a vector."))
        (doseq [[i seg] (map-indexed vector (:testing_path anchor))]
          (closed! seg [:label :expect] [] [:anchor :testing_path i] :invalid-request)
          (when-not (and (string? (:label seg)) (= 1 (:expect seg)) (integer? (:expect seg)))
            (refuse! :invalid-request [:anchor :testing_path i] "Literal label and expect 1 required."))))
      (when-not (#{"before" "after"} (:position anchor))
        (refuse! :invalid-request [:anchor :position] "Position must be before or after.")))
    (closed! payload [:text :forms] [] [:payload] :invalid-request)
    (when-not (string? (:text payload)) (refuse! :invalid-request [:payload :text] "Text required."))
    (positive! (:forms payload) [:payload :forms])
    (when (> (:forms payload) (:forms limits)) (refuse! :limit-exceeded [:payload :forms] "At most 1000 forms.")))
  r)

;; @spec INSERT-FORMS-004
;; @spec INSERT-FORMS-012
;; INTENT: INSERT-FORMS-004
;; INTENT: INSERT-FORMS-012
(defn guard! [source r]
  (let [g (:guard r)
        _ (when-not (map? g) (refuse! :invalid-guard [:guard] "Guard must be a map."))
        hash (cond
               (= #{:sha256} (set (keys g))) (:sha256 g)
               (= #{:read_receipt} (set (keys g)))
               (let [p (:read_receipt g)]
                 (closed! p [:version :read_complete :workspace_root :file :sha256] [] [:guard] :invalid-guard)
                 (when-not (and (= 1 (:version p)) (integer? (:version p))
                                (true? (:read_complete p)) (= (:file p) (:file r))
                                (= (:workspace_root p) (:workspace_root r)))
                   (refuse! :invalid-guard [:guard] "Portable read receipt does not match this target."))
                 (:sha256 p))
               :else (refuse! :invalid-guard [:guard] "Supply exactly one guard alternative."))]
    (when-not (and (string? hash) (re-matches #"[0-9a-f]{64}" hash))
      (refuse! :invalid-guard [:guard] "Lowercase SHA-256 required."))
    (when-not (= hash (sha source))
      (refuse! :source-hash-mismatch [:guard] "Captured source differs from guard."
               {:expected hash :actual (sha source)}))))

(defn starts [^String s]
  (loop [i 0 result [0]]
    (if (>= i (count s)) result
        (recur (inc i) (cond-> result (= \newline (.charAt s i)) (conj (inc i)))))))
(defn tree [source kind]
  (try
    (let [root (parser/parse-string-all source) ls (starts source)]
      (letfn [(wrap [node]
                (let [{:keys [row col end-row end-col]} (meta node)
                      start (if (= :forms (n/tag node)) 0 (+ (nth ls (dec row)) (dec col)))
                      end (if (= :forms (n/tag node)) (count source) (+ (nth ls (dec end-row)) (dec end-col)))]
                  {:tag (n/tag node) :start start :end end :line row
                   :source source
                   :children (when (n/inner? node) (mapv wrap (n/children node)))}))]
        (wrap root)))
    (catch Exception e
      (refuse! (case kind :source :source-parse-error :payload :payload-parse-error :candidate-parse-error)
               [kind] (.getMessage e)
               {:line (or (:row (ex-data e)) 1) :column (or (:col (ex-data e)) 1)}))))
(defn raw [node]
  (when node (subs (:source node) (:start node) (:end node))))

(def trivia-tags #{:whitespace :newline :comment :comma :uneval})
(defn effective [node] (filterv #(not (trivia-tags (:tag %))) (:children node)))
(defn unwrap [node]
  (if (#{:meta :meta*} (:tag node)) (recur (last (effective node))) node))
(defn head [node] (raw (first (effective (unwrap node)))))
(def canonical {"clojure.core/def" "def" "clojure.core/defn" "defn"
                "clojure.core/defn-" "defn-" "clojure.core/ns" "ns"
                "clojure.test/deftest" "deftest" "clojure.test/testing" "testing"})
(defn kind [node] (let [h (head node)] (get canonical h h)))
(defn owner? [node owner]
  (let [form (unwrap node)]
    (and (= :list (:tag form)) (= (:kind owner) (kind form))
         (= (:name owner) (raw (unwrap (second (effective form))))))))

;; @spec INSERT-FORMS-006
;; INTENT: INSERT-FORMS-006
(defn one! [candidates at]
  (when-not (= 1 (count candidates))
    (refuse! (if (empty? candidates) :anchor-not-found :anchor-multiple-matches)
             at "Expected exactly one structural match."
             {:expected 1 :actual (count candidates)
              :candidates (mapv #(select-keys % [:line :start :end]) (take 10 candidates))
              :candidates_truncated (> (count candidates) 10)}))
  (first candidates))

;; @spec INSERT-FORMS-008
;; INTENT: INSERT-FORMS-008
(defn body-of [owner anchor]
  (let [form (unwrap owner) children (effective form) k (kind form)
        malformed #(refuse! :unsupported-owner-shape [:anchor :owner] "Malformed or unsupported body header.")
        finish (fn [container params tail]
                 (let [tail (if (and (= :map (:tag (first tail))) (> (count tail) 1)) (subvec tail 1) tail)
                       header (if (seq tail) (last (take-while #(< (:end %) (:start (first tail))) (effective container))) params)]
                   {:container container :body tail :header (or header params)}))]
    (case k
      "deftest" (do (when (:arity anchor) (refuse! :invalid-request [:anchor :arity] "deftest has no arity selector."))
                    {:container form :body (subvec children 2) :header (second children)})
      ("defn" "defn-")
      (let [tail (subvec children 2)
            tail (if (str/starts-with? (or (raw (first tail)) "") "\"") (subvec tail 1) tail)
            tail (if (= :map (:tag (first tail))) (subvec tail 1) tail)]
        (cond
          (= :vector (:tag (first tail)))
          (do (when (:arity anchor) (refuse! :invalid-request [:anchor :arity] "Single arity forbids arity selector."))
              (finish form (first tail) (subvec tail 1)))
          (= :list (:tag (first tail)))
          (let [arities (if (= :map (:tag (last tail))) (pop tail) tail)]
            (when-not (every? #(and (= :list (:tag %)) (= :vector (:tag (first (effective %))))) arities) (malformed))
            (when-not (:arity anchor) (refuse! :anchor-ambiguous [:anchor :arity] "Multi-arity owner requires arity."))
            (when (> (:arity anchor) (count arities))
              (refuse! :anchor-index-out-of-range [:anchor :arity] "Arity is outside owner."
                       {:arity_count (count arities) :actual (:arity anchor)}))
            (let [container (nth arities (dec (:arity anchor))) cs (effective container)]
              (finish container (first cs) (subvec cs 1))))
          :else (malformed)))
      (malformed))))

;; @spec INSERT-FORMS-001
;; @spec INSERT-FORMS-002
;; @spec INSERT-FORMS-005
;; @spec INSERT-FORMS-007
;; INTENT: INSERT-FORMS-001
;; INTENT: INSERT-FORMS-002
;; INTENT: INSERT-FORMS-005
;; INTENT: INSERT-FORMS-007
(defn resolve-anchor [root anchor]
  (let [roots (effective root) owner (one! (filterv #(owner? % (:owner anchor)) roots) [:anchor :owner])]
    (if (= "top-level" (:scope anchor))
      (let [index (.indexOf roots owner) index (if (= "after" (:position anchor)) (inc index) index)]
        {:owner owner :container root :body roots :index index
         :left (when (pos? index) (nth roots (dec index))) :column-node (unwrap owner)})
      (let [selected (reduce
                       (fn [{:keys [body]} [i segment]]
                         (let [match (one! (filterv (fn [node]
                                                      (let [cs (effective node) label (second cs)]
                                                        (and (= :list (:tag node)) (= "testing" (kind node))
                                                             (str/starts-with? (or (raw label) "") "\"")
                                                             (= (:label segment) (edn/read-string (raw label)))))) body)
                                           [:anchor :testing_path i])]
                           {:container match :body (subvec (effective match) 2) :header (second (effective match))}))
                       (body-of owner anchor) (map-indexed vector (:testing_path anchor)))
            {:keys [body header container]} selected
            boundary (:boundary anchor)
            index (case (:position boundary) "first" 0 "last" (count body) "after-child" (:child boundary))]
        (when (> index (count body))
          (refuse! :anchor-index-out-of-range [:anchor :boundary :child] "Child is outside selected body."
                   {:child_count (count body) :actual index}))
        (assoc selected :owner owner :index index :left (if (zero? index) header (nth body (dec index)))
               :column-node (or (first body) container) :empty? (empty? body))))))

(defn column [^String source node]
  (let [p (:start node) start (inc (.lastIndexOf source "\n" (max 0 (dec p))))
        prefix (subs source start p)]
    (when (str/includes? prefix "\t") (refuse! :unsupported-indentation [:anchor] "Tab in measured source indentation."))
    (.codePointCount source start p)))
(defn offset [^String source left]
  (let [p (or (:end left) 0)]
    (if (nil? left) 0
        (if-let [comment (re-find #"\A[ \t,]*;[^\r\n]*(?:\r\n|\n|\z)" (subs source p))]
          (+ p (count comment)) p))))
(defn newline-style [source]
  (let [crlf (count (re-seq #"\r\n" source)) lf (count (re-seq #"\n" source))]
    (when (or (not= (count (re-seq #"\r" source)) crlf) (and (pos? crlf) (not= crlf lf)))
      (refuse! :unsupported-source [:source] "Mixed or bare-CR source newlines are unsupported."))
    (if (pos? crlf) "\r\n" "\n")))

;; @spec INSERT-FORMS-010
;; INTENT: INSERT-FORMS-010
(defn indent [payload literals c newline]
  (let [inside? (fn [i] (some (fn [[a b]] (< a i b)) literals))
        lines (vec (re-seq #"[^\r\n]*(?:\r\n|\n|\r|\z)" payload))
        entries (loop [xs lines p 0 out []]
                  (if-let [s (first xs)] (recur (next xs) (+ p (count s)) (conj out [p s])) out))
        measured (for [[p s] entries :when (and (not (inside? p)) (not (str/blank? s)))]
                   (let [indentation (re-find #"^[ \t]*" s)]
                     (when (str/includes? indentation "\t")
                       (refuse! :unsupported-indentation [:payload] "Tab in payload indentation."))
                     (count indentation)))
        m (if (seq measured) (apply min measured) 0)]
    (apply str
           (for [[p original-line] entries]
             (let [s original-line
                   original-length (count s)
                   s (if (or (inside? p) (str/blank? s)) s
                         (str (apply str (repeat c " ")) (subs s m)))
                   ending (re-find #"(?:\r\n|\n|\r)$" s)
                   end-index (+ p (- original-length (count (or ending ""))))]
               (if (and ending (not (inside? end-index)))
                 (str (subs s 0 (- (count s) (count ending))) newline) s))))))
(defn spellings [root]
  (if (:children root)
    (if (#{:string :multi-line :regex} (:tag root)) [(raw root)]
        (mapcat spellings (:children root)))
    (when-not (trivia-tags (:tag root)) [(raw root)])))
(defn root-inventory [root]
  (filterv #(not (#{:whitespace :newline :comment :comma} (:tag %))) (:children root)))

;; @spec INSERT-FORMS-013
;; @spec INSERT-FORMS-016
;; @spec INSERT-FORMS-017
;; INTENT: INSERT-FORMS-013
;; INTENT: INSERT-FORMS-016
;; INTENT: INSERT-FORMS-017
(defn facts [source candidate inserted p c r before after selection payload-root]
  (let [byte-p (alength (bytes (subs source 0 p))) length (alength (bytes inserted))
        line-at #(inc (count (re-seq #"\n" (subs candidate 0 %))))
        originals (root-inventory before) future (root-inventory after)
        top? (= "top-level" (get-in r [:anchor :scope]))
        inserted-roots (filterv #(and (<= p (:start %)) (<= (:end %) (+ p (count inserted))))
                         (if top? future (:body (resolve-anchor after (:anchor r)))))
        retained (if top? (filterv #(not (some #{%} inserted-roots)) future) future)
        entries (mapv (fn [i a b]
                        {:before_index (inc i) :after_index (inc (.indexOf future b))
                         :before_sha256 (sha (raw a)) :after_sha256 (sha (raw b))})
                      (range) originals retained)
        owner-index (.indexOf originals (:owner selection))
        other (if top? entries (vec (concat (take owner-index entries) (drop (inc owner-index) entries))))]
    (when-not (and (= (count originals) (count retained))
                   (= (get-in r [:payload :forms]) (count inserted-roots))
                   (every? #(= (:before_sha256 %) (:after_sha256 %)) other)
                   (= (spellings payload-root) (mapcat spellings inserted-roots)))
      (refuse! :candidate-structure-mismatch [:candidate] "Inserted siblings or preserved root forms differ."))
    {:receipt
     {:operation "insert_forms" :version 1 :file (:file r)
      :source_hash (sha source) :result_hash (sha candidate)
      :bytes_added length :forms_inserted (get-in r [:payload :forms]) :anchor_column c
      :splice {:offset byte-p :length length :sha256 (sha inserted)}
      :line_range {:start (line-at p) :end (line-at (+ p (dec (count inserted))))}
      :inserted_form_ranges (mapv (fn [i form] {:ordinal (inc i) :start_line (line-at (:start form))
                                                :end_line (line-at (dec (:end form)))}) (range) inserted-roots)
      :preservation {:prefix_sha256 (sha (subs source 0 p)) :suffix_sha256 (sha (subs source p))
                     :other_forms_checked (count other) :other_forms_unchanged true}
      :verification_complete false :verification {:tier "parse+byte-preservation" :behavior "not-run"}
      :concurrency "cooperative-lock+final-recheck" :next_action "none"}
     :detail {:request r :source_bytes (alength (bytes source)) :result_bytes (alength (bytes candidate))
              :resolved_anchor (select-keys selection [:index :empty?])
              :preservation_entries entries
              :inverse_splice {:offset byte-p :remove_sha256 (sha inserted) :remove_length length}}}))

;; @spec INSERT-FORMS-003
;; @spec INSERT-FORMS-009
;; INTENT: INSERT-FORMS-003
;; INTENT: INSERT-FORMS-009
(defn plan [source r]
  (try
    (validate! r)
    (bounded! source :source)
    (guard! source r)
    (lexical! source :source)
    (let [newline (newline-style source) payload (get-in r [:payload :text])
          literals (lexical! payload :payload) before (tree source :source)
          payload-root (tree payload :payload) count-forms (count (effective payload-root))]
      (when-not (= count-forms (get-in r [:payload :forms]))
        (refuse! :payload-form-count-mismatch [:payload :forms] "Payload form count differs."
                 {:expected (get-in r [:payload :forms]) :actual count-forms}))
      (let [selection (resolve-anchor before (:anchor r)) p (offset source (:left selection))
            c (+ (column source (:column-node selection)) (if (:empty? selection) 2 0))
            adjusted (indent payload literals c newline) adjusted-root (tree adjusted :payload)
            _ (when-not (= (spellings payload-root) (spellings adjusted-root))
                (refuse! :candidate-structure-mismatch [:payload] "Reindentation changed tokens."))
            inserted (str (when-not (or (zero? p) (= \newline (nth source (dec p)))) newline)
                          adjusted (when-not (str/ends-with? adjusted "\n") newline))
            candidate (str (subs source 0 p) inserted (subs source p)) after (tree candidate :candidate)]
        (merge {:ok true :candidate candidate :offset p :inserted inserted}
               (facts source candidate inserted p c r before after selection adjusted-root))))
    (catch Exception e
      (refusal (if (:error-type (ex-data e)) e
                   (ex-info (.getMessage e) {:error-type :invalid-request :at []}))))))
