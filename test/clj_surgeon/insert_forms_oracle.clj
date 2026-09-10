(ns clj-surgeon.insert-forms-oracle
  "Independent acceptance walk. Shares rewrite-clj, never production selection/splice/hash helpers."
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser])
  (:import
   (java.security MessageDigest)))

(defn digest [s]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (.update md (.getBytes ^String s "UTF-8"))
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest md)))))
(defn width [s] (alength (.getBytes ^String s "UTF-8")))
(defn assert-law [condition law]
  (when-not condition (throw (ex-info (name law) {:law law}))))
(defn inventory [s]
  (let [line-offsets (vec (cons 0 (map inc (keep-indexed #(when (= %2 \newline) %1) s))))
        coordinate (fn [row col] (+ (get line-offsets (dec row)) (dec col)))]
    (letfn [(entry [n]
              (let [{:keys [row col end-row end-col]} (meta n)
                    start (if (= :forms (node/tag n)) 0 (coordinate row col))
                    end (if (= :forms (node/tag n)) (count s) (coordinate end-row end-col))]
                {:node n :tag (node/tag n) :start start :end end :text (subs s start end)
                 :entries (when (node/inner? n) (mapv entry (node/children n)))}))]
      (entry (parser/parse-string-all s)))))
(defn code [entry]
  (vec (remove #(#{:newline :whitespace :comma :comment :uneval} (:tag %)) (:entries entry))))
(defn metadata-value [entry]
  (if (#{:meta :meta*} (:tag entry)) (recur (last (code entry))) entry))
(defn operator [entry]
  (let [head (:text (first (code (metadata-value entry))))]
    (get {"clojure.core/defn" "defn" "clojure.core/defn-" "defn-"
          "clojure.core/def" "def" "clojure.core/ns" "ns"
          "clojure.test/deftest" "deftest" "clojure.test/testing" "testing"} head head)))
(defn only [xs] (assert-law (= 1 (count xs)) :unique-selection) (first xs))
(defn defn-body [form arity]
  (let [cs (code form) header-tail (drop 2 cs)
        header-tail (if (str/starts-with? (or (:text (first header-tail)) "") "\"") (rest header-tail) header-tail)
        header-tail (if (= :map (:tag (first header-tail))) (rest header-tail) header-tail)
        container (if (= :vector (:tag (first header-tail))) form
                      (nth (vec header-tail) (dec arity)))
        params-tail (if (= container form) header-tail (code container))
        params (first params-tail) tail (vec (rest params-tail))
        prepost? (and (= :map (:tag (first tail))) (> (count tail) 1))]
    {:container container :header (if prepost? (first tail) params)
     :children (if prepost? (subvec tail 1) tail)}))
(defn destination [root request]
  (let [a (:anchor request) roots (code root)
        owned (only (filter #(let [form (metadata-value %)]
                               (and (= :list (:tag form)) (= (get-in a [:owner :kind]) (operator form))
                                    (= (get-in a [:owner :name]) (:text (metadata-value (second (code form))))))) roots))
        form (metadata-value owned)]
    (if (= "top-level" (:scope a))
      (let [boundary (+ (.indexOf roots owned) (if (= "after" (:position a)) 1 0))]
        {:owner owned :previous (when (pos? boundary) (nth roots (dec boundary)))
         :children roots :boundary boundary :container root})
      (let [initial (if (= "deftest" (operator form))
                      {:container form :header (second (code form)) :children (subvec (code form) 2)}
                      (defn-body form (:arity a)))
            selected (reduce (fn [parent segment]
                               (let [block (only (filter #(and (= :list (:tag %)) (= "testing" (operator %))
                                                            (= (:label segment) (edn/read-string (:text (second (code %))))))
                                                   (:children parent)))
                                     cs (code block)]
                                 {:container block :header (second cs) :children (subvec cs 2)}))
                             initial (:testing_path a))
            boundary (case (get-in a [:boundary :position])
                       "first" 0 "last" (count (:children selected)) "after-child" (get-in a [:boundary :child]))]
        (assoc selected :owner owned :boundary boundary
               :previous (if (zero? boundary) (:header selected) (nth (:children selected) (dec boundary))))))))
(defn token-tree [entry]
  (if (:entries entry)
    [(:tag entry) (mapv token-tree (remove #(#{:whitespace :newline :comma :comment} (:tag %)) (:entries entry)))]
    [(:tag entry) (:text entry)]))
(defn gap-policy [source destination]
  (let [left (:previous destination)
        end (or (:end left) 0)
        comment (when left (re-find #"^[ \t,]*;[^\n\r]*" (subs source end)))
        start (+ end (count comment))
        whitespace (if left (re-find #"^[ \t,\r\n]*" (subs source start)) "")
        breaks (count (filter #{\newline} whitespace))
        used (if (pos? breaks) whitespace "")
        p (+ start (count used))
        peers (:children destination)
        peer-breaks (for [[a b] (partition 2 1 peers)
                          :let [n (count (filter #{\newline} (subs source (:end a) (:start b))))]
                          :when (pos? n)] n)]
    {:p p :breaks breaks :count (if (pos? breaks) breaks (or (first peer-breaks) 1))
     :indent (if (pos? breaks) (last (str/split used #"\n" -1)) "")
     :closing? (and (not= :forms (get-in destination [:container :tag]))
                    (= p (dec (get-in destination [:container :end]))))}))

(defn exact-added [source payload destination p]
  (let [anchor (if (= :forms (get-in destination [:container :tag]))
                 (:owner destination)
                 (or (first (:children destination)) (:container destination)))
        start (:start anchor)
        line-start (inc (.lastIndexOf ^String source "\n" (max 0 (dec start))))
        target-column (+ (.codePointCount ^String source line-start start)
                         (if (and (not= :forms (get-in destination [:container :tag]))
                                  (empty? (:children destination))) 2 0))
        newline (if (str/includes? source "\r\n") "\r\n" "\n")
        literal-spans (for [entry (tree-seq (comp seq :entries) :entries (inventory payload))
                            :when (or (str/starts-with? (:text entry) "\"")
                                      (str/starts-with? (:text entry) "#\""))]
                        [(:start entry) (:end entry)])
        literal? (fn [pos] (some (fn [[a b]] (< a pos b)) literal-spans))
        lines (re-seq #"[^\r\n]*(?:\r\n|\n|\r|$)" payload)
        offsets (reductions + 0 (map count lines))
        rows (map vector offsets lines)
        measured (for [[offset line] rows :when (and (not (str/blank? line)) (not (literal? offset)))]
                   (count (take-while #{\space} line)))
        minimum (if (seq measured) (apply min measured) 0)
        adjusted (apply str
                        (for [[offset line] rows]
                          (let [ending (re-find #"(?:\r\n|\n|\r)$" line)
                                ending-start (+ offset (- (count line) (count ending)))
                                normalized (if (and ending (not (literal? ending-start)))
                                             (str (subs line 0 (- (count line) (count ending))) newline) line)]
                            (if (or (str/blank? line) (literal? offset)) normalized
                                (str (apply str (repeat target-column " ")) (subs normalized minimum))))))
        {:keys [breaks count indent closing?]} (gap-policy source destination)
          trailing (clojure.core/count (filter #{\newline} (or (re-find #"(?:\r?\n)+$" adjusted) "")))
          payload-start (if (pos? breaks) (min target-column (clojure.core/count indent)) 0)]
      (str (when (and (pos? p) (zero? breaks)) (apply str (repeat count newline)))
           (subs adjusted payload-start)
           (if closing?
             (when (pos? trailing) (apply str (repeat target-column " ")))
             (str (apply str (repeat (max 0 (- count trailing)) newline))
                  (when (< p (clojure.core/count source)) indent))))))

(defn verify [a b request receipt]
  (try
    (assert-law (= (digest a) (:source_hash receipt)) :source-hash)
    (assert-law (= (digest b) (:result_hash receipt)) :result-hash)
    (let [before (inventory a) after (inventory b) selected (destination before request)
          p (:p (gap-policy a selected))
          delta (- (count b) (count a))
          _ (assert-law (pos? delta) :nonempty-splice)
          added (subs b p (+ p delta))
          payload (code (inventory (get-in request [:payload :text])))
          top? (= "top-level" (get-in request [:anchor :scope]))
          original (vec (remove #(#{:whitespace :newline :comma :comment} (:tag %)) (:entries before)))
          future (vec (remove #(#{:whitespace :newline :comma :comment} (:tag %)) (:entries after)))
          post-selection (when-not top? (destination after request))
          children (if top? (code after) (:children post-selection))
          inserted (subvec children (:boundary selected) (+ (:boundary selected) (count payload)))
          rest-roots (if top? (vec (remove (set inserted) future)) future)
          owner-index (.indexOf original (:owner selected))]
      (assert-law (= b (str (subs a 0 p) added (subs a p))) :one-splice)
      (assert-law (= added (exact-added a (get-in request [:payload :text]) selected p)) :exact-added-bytes)
      (assert-law (= (get-in request [:payload :forms]) (count payload)) :payload-count)
      (assert-law (= (mapv token-tree payload) (mapv token-tree inserted)) :inserted-tokens)
      (assert-law (= (count original) (count rest-roots)) :root-count)
      (doseq [[i old new] (map vector (range) original rest-roots)]
        (if (and (not top?) (= owner-index i))
          (let [at (- p (:start old))]
            (assert-law (= (:text old) (str (subs (:text new) 0 at) (subs (:text new) (+ at delta)))) :owner-splice))
          (assert-law (= (digest (:text old)) (digest (:text new))) :other-root-hash)))
      (assert-law (= (width added) (:bytes_added receipt) (- (width b) (width a))) :bytes-added)
      (assert-law (= {:offset (width (subs a 0 p)) :length (width added) :sha256 (digest added)} (:splice receipt)) :splice-receipt)
      (assert-law (= (digest (subs a 0 p)) (get-in receipt [:preservation :prefix_sha256])) :prefix-hash)
      (assert-law (= (digest (subs a p)) (get-in receipt [:preservation :suffix_sha256])) :suffix-hash)
      (let [line-at #(inc (count (filter #{\newline} (subs b 0 %))))]
        (assert-law (= {:start (line-at p) :end (line-at (dec (+ p delta)))} (:line_range receipt)) :line-range)
        (when (:inserted_form_ranges receipt)
          (assert-law (= (mapv (fn [i x] {:ordinal (inc i) :start_line (line-at (:start x)) :end_line (line-at (dec (:end x)))})
                           (range) inserted) (:inserted_form_ranges receipt)) :form-ranges)))
      {:ok true :other-forms-checked (if top? (count original) (dec (count original)))})
    (catch Exception e {:ok false :law (or (:law (ex-data e)) :invalid-structure)})))
