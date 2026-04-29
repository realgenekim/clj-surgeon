(ns clj-surgeon.cljc.require-ops-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :as rt]
            [clj-surgeon.cljc.require-ops :as ops]
            [clj-surgeon.cljc.split :as split]))

(defn- parse-forms [src]
  (let [rdr (rt/string-push-back-reader src)]
    (->> (repeatedly #(r/read {:eof ::end :read-cond :preserve} rdr))
         (take-while #(not= ::end %))
         vec)))

(defn- requires-of
  "Set of require entries on a given platform after splitting."
  [cljc-src platform]
  (let [{clj :clj cljs :cljs} (split/split-file cljc-src)
        src (if (= :clj platform) clj cljs)
        [ns-form] (parse-forms src)
        require-form (->> (drop 2 ns-form)
                          (filter #(and (seq? %) (= :require (first %))))
                          first)]
    (set (rest require-form))))

(def ^:private cljc-base
  "(ns my.app
  (:require
   [clojure.string :as str]))

(defn upper [s] (str/upper-case s))
")

(deftest add-cljc-require
  (testing "Adding at :cljc puts the entry on both platforms as a SHARED entry
            (no platform splice introduced for it)."
    (let [out (ops/add-require cljc-base {:platform :cljc
                                          :ns       'clojure.set
                                          :as       'set})
          clj-reqs  (requires-of out :clj)
          cljs-reqs (requires-of out :cljs)]
      (is (contains? clj-reqs  '[clojure.set :as set]))
      (is (contains? cljs-reqs '[clojure.set :as set]))
      ;; Structural check on the CLJC source: clojure.set must appear OUTSIDE
      ;; any reader conditional, since both platforms have it identically.
      (is (str/includes? out "[clojure.set :as set]"))
      (let [;; Take the substring up to the first #?@ if any, and make sure
            ;; the new require sits in that shared region.
            splice-idx (str/index-of out "#?@")
            prefix     (if splice-idx (subs out 0 splice-idx) out)]
        (is (str/includes? prefix "clojure.set")
            "shared require should appear before any platform splice")))))

(deftest add-clj-only-require
  (testing "Adding at :clj wraps the entry in #?@(:clj [...]) of the CLJC source."
    (let [out (ops/add-require cljc-base {:platform :clj
                                          :ns       'clojure.java.io
                                          :as       'io})
          clj-reqs  (requires-of out :clj)
          cljs-reqs (requires-of out :cljs)]
      (is (contains? clj-reqs '[clojure.java.io :as io]))
      (is (not (contains? cljs-reqs '[clojure.java.io :as io])))
      ;; The CLJC source must contain a #?@(:clj ...) splice with this entry.
      (is (re-find #"(?s)#\?@\(:clj.*clojure\.java\.io.*\)" out)
          "expected the new require inside a #?@(:clj ...) splice"))))

(deftest add-cljs-only-require
  (testing "Adding at :cljs wraps the entry in #?@(:cljs [...]) of the CLJC source."
    (let [out (ops/add-require cljc-base {:platform :cljs
                                          :ns       'goog.string
                                          :as       'gstr})
          clj-reqs  (requires-of out :clj)
          cljs-reqs (requires-of out :cljs)]
      (is (contains? cljs-reqs '[goog.string :as gstr]))
      (is (not (contains? clj-reqs '[goog.string :as gstr])))
      (is (re-find #"(?s)#\?@\(:cljs.*goog\.string.*\)" out)
          "expected the new require inside a #?@(:cljs ...) splice"))))

(defn- thrown-msg [f]
  (try (f) nil
       (catch Exception e (.getMessage e))))

(deftest invalid-platform-throws
  (let [msg (thrown-msg #(ops/add-require cljc-base
                                          {:platform :clojure :ns 'foo :as 'f}))]
    (is (some? msg))
    (is (str/includes? msg "platform"))))

(deftest add-npm-string-require
  (testing "Adding a string ns (npm-style) at :cljs preserves the string
            form in the merged CLJC source — must not be coerced to a symbol."
    (let [out (ops/add-require cljc-base {:platform :cljs
                                          :ns       "react"
                                          :as       'react})
          cljs-reqs (requires-of out :cljs)]
      ;; Source must contain the string-quoted form, not a bare token.
      (is (str/includes? out "\"react\""))
      ;; And the parsed cljs-side require should be a vector starting with
      ;; the string "react" (NOT the symbol react).
      (is (some (fn [v] (and (vector? v) (= "react" (first v))))
                cljs-reqs)
          "cljs requires should contain a vector starting with the string \"react\""))))

(deftest alias-collision-throws
  (testing "Adding a require whose alias is already bound to a different ns
            on the same platform throws — silently producing a divergent
            same-side alias would break compilation."
    (let [msg (thrown-msg #(ops/add-require cljc-base
                                            {:platform :cljc
                                             :ns 'some.other
                                             :as 'str}))]
      (is (some? msg))
      (is (str/includes? msg "Alias")))))

(deftest add-require-to-source-without-require-form
  (testing "Adding to a CLJC source whose ns has no :require form yet creates one."
    (let [bare "(ns my.app.bare)\n\n(def x 1)\n"
          out  (ops/add-require bare {:platform :cljc :ns 'clojure.string :as 'str})]
      (is (contains? (requires-of out :clj)  '[clojure.string :as str]))
      (is (contains? (requires-of out :cljs) '[clojure.string :as str])))))
