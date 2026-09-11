(ns clj-splice.core-test
  (:require [clj-splice.core :as s]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]))

(defn sha [text]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (.getBytes ^String text StandardCharsets/UTF_8)))))
(defn slice [source {:keys [start end]}]
  (String. (java.util.Arrays/copyOfRange (.getBytes ^String source StandardCharsets/UTF_8)
                                        (int start) (int end)) StandardCharsets/UTF_8))
(def samples ["" " ; tail\r\n" "\"é😀\" (def x 1)" "\"é😀\"\r\n(def x 1)"
              "\"a\nb\" (def x 1)" "\"a\r\nb\" (def x 1)"
              "a\rb" "a\r\fb" "a\r\nb\nc" "#_#_1 2 3" "#::ev{}" "#:é{}"
              "#?(:clj 1)" "#?@(:clj [1])" "#\"[()]\" \\(" "^:m (def x 1)"
              "\uFEFF(ns x)" "'x `x ~x @x #tag [1]"])
(defn corpus []
  (mapv slurp (sort-by str (filter #(.endsWith (.getName %) ".clj")
                                  (file-seq (io/file (io/resource "clj_splice/fixtures")))))))

;; INTENT-TEST: SPLICE-001
;; @spec SPLICE-001
(deftest original-intervals
  (let [source "\"é😀\" (def x 1)" inv (s/spans source)
        form ((:nodes inv) (second (:roots inv)))]
    (is (= [9 18 1 7 "f5c134bf6c2d0b925524242efe4dd5564dbec67f832b7e484f0a4bd1963c8939"]
           ((juxt :start :end :row :col :sha256) form)))
    (is (= "\"é😀\" ; hi\n(def x 1)" (s/splice source [9 9] "; hi\n"))))
  (doseq [source samples]
    (let [{:keys [nodes roots gaps] :as inv} (s/spans source)
          parts (concat (mapcat vector (butlast gaps) (map nodes roots)) [(last gaps)])]
      (is (= [source inv]
             [(apply str (map #(slice source %) parts)) (s/recount source)]) source)))
  (let [inv (s/spans "#_#_1 2 3")]
    (is (= [2 1 ["3"]] [(count (:roots inv)) (:effective-count inv)
                            (mapv #(slice (:source inv) ((:nodes inv) %)) (:effective-roots inv))])))
  (is (= :token (:tag (second (:nodes (s/spans "\uFEFF(ns x)")))))))

(deftest literal-and-synthetic-coordinates
  (doseq [[source expected] [["#::ev{}" [[:map-qualifier 1 5 "::ev"]]]
                             ["#:é{}" [[:map-qualifier 1 4 ":é"]]]
                             ["#?(:clj 1)" [[:conditional-prefix 1 2 "?"]]]
                             ["#?@(:clj [1])" [[:conditional-prefix 1 3 "?@"]]]]]
    (is (= expected (mapv #(conj ((juxt :synthetic :start :end) %) (slice source %))
                          (filter :synthetic (:nodes (s/spans source)))))))
  (let [source "\"a\r\nb\" #\"[()]\" \\(" inv (s/spans source)]
    (is (= [[:string "\"a\r\nb\""] [:regex "#\"[()]\""]]
           (mapv #(vector (:literal %) (slice source %)) (filter :literal (:nodes inv)))))))

;; INTENT-TEST: SPLICE-002
;; @spec SPLICE-002
(deftest interval-boundaries
  (is (= "a😀b" (s/splice "é😀z" [[[6 7] "b"] [[0 2] "a"]])))
  (is (= :clj-splice/invalid-interval
         (try (s/splice "abcd" [[[0 2] "x"] [[1 3] "y"]]) nil
              (catch Exception e (:clj-splice/category (ex-data e))))))
  (doseq [[interval replacement expected] [[[0 0] "x" "xé😀z"] [[2 6] "a" "éaz"]
                                           [[7 7] "!" "é😀z!"] [[0 7] "" ""]]]
    (is (= expected (s/splice "é😀z" interval replacement))))
  (doseq [[source interval replacement category]
          [["é😀z" [1 2] "" :clj-splice/encoding-boundary]
           ["é😀z" [2 3] "" :clj-splice/encoding-boundary]
           ["x" [-1 0] "" :clj-splice/invalid-interval]
           ["x" [1 0] "" :clj-splice/invalid-interval]
           ["x" [0 2] "" :clj-splice/invalid-interval]
           [(str (char 0xD800)) [0 0] "" :clj-splice/invalid-unicode]
           ["x" [0 0] (str (char 0xDC00)) :clj-splice/invalid-unicode]]]
    (is (= category (try (s/splice source interval replacement) nil
                        (catch Exception e (:clj-splice/category (ex-data e)))))))
  (is (= :clj-splice/parse-error
         (try (s/spans "(") nil (catch Exception e (:clj-splice/category (ex-data e)))))))

(deftest frozen-corpus-properties
  (is (= 12 (count (corpus))))
  (doseq [source (corpus)]
    (let [{:keys [roots nodes gaps]} (s/spans source)
          parts (concat (mapcat vector (butlast gaps) (map nodes roots)) [(last gaps)])
          candidate (s/splice source [0 0] "; splice\n")
          after (s/spans candidate)]
      (is (= [source (mapv #(select-keys (nodes %) [:start :end :sha256]) roots)]
             [(apply str (map #(slice source %) parts))
              (mapv #(-> (select-keys ((:nodes after) %) [:start :end :sha256])
                         (update :start - 9) (update :end - 9)) (:roots after))])))))

(deftest public-fence-and-runtime-pin
  (let [ids (set (map (comp name :id) (read-string (slurp (io/resource "clj_splice/intents.edn")))))
        code (slurp (io/resource "clj_splice/core.clj"))
        tests (slurp (io/resource "clj_splice/core_test.clj"))]
    (is (= [ids ids]
           [(set (map second (re-seq #"(?m)^;; INTENT: (SPLICE-[0-9]+)" code)))
            (set (map second (re-seq #"(?m)^;; INTENT-TEST: (SPLICE-[0-9]+)" tests)))])))
  (is (= '#{spans splice recount} (set (keys (ns-publics 'clj-splice.core)))))
  (if-let [bb (System/getProperty "babashka.version")]
    ;; Release pins: 1.12.209 embeds 1.2.50; 1.13.219 embeds 1.2.55.
    ;; Never reload parser deftypes in SCI.
    (is (contains? #{"1.12.209" "1.13.219"} bb))
    (is (= "1.2.50" (get-in (read-string (slurp "deps.edn"))
                            [:deps 'rewrite-clj/rewrite-clj :mvn/version])))))

(defn projection-hash []
  (sha (pr-str (mapv (fn [source]
                      (let [inv (s/spans source)]
                        [(:roots inv) (:effective-roots inv) (:gaps inv)
                         (mapv #(mapv % [:id :parent :children :tag :start :end :row :col
                                         :end-row :end-col :synthetic :literal :sha256]) (:nodes inv))]))
                    (concat samples (corpus))))))
