(ns ^{:lane :fast} clj-surgeon.mcp-operation-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.util Locale Locale$Category)))

(defn- invoke!
  [options]
  ((requiring-resolve 'clj-surgeon.mcp-operation/invoke!) options))

(defn- format-elapsed-ms
  [elapsed-ms]
  ((requiring-resolve 'clj-surgeon.mcp-operation/format-elapsed-ms)
   elapsed-ms))

(defn- scripted-clock
  [ticks]
  (let [remaining (atom ticks)]
    (fn []
      (let [tick (first @remaining)]
        (swap! remaining subvec 1)
        tick))))

;; @spec MCP-OP-RESULT-001
;; @spec MCP-OP-RESULT-002
;; @spec MCP-OP-RESULT-003
;; @spec MCP-OP-RESULT-004
(deftest finalization-adds-only-authoritative-time-and-preserves-classification
  (doseq [[domain-result expected-error?]
          [[{:ok true
             :operation "example"
             :nested {:kept false}
             :optional nil
             :elapsed_ms 999.0
             :inspection_elapsed_ms 1.25}
            false]
           [{:ok false
             :operation "example"
             :error_type "stale-source"
             :source_unchanged true}
            true]]]
    (let [calls (atom [])
          body (invoke!
                 {:clock-nanos (scripted-clock [1000000 4500000])
                  :execute (constantly domain-result)
                  :summarize (fn [result]
                               (str (:operation result) " · "
                                    (format-elapsed-ms (:elapsed_ms result))))
                  :serialize json/generate-string
                  :callback (fn [content error? structured]
                              (swap! calls conj
                                     {:content content
                                      :error? error?
                                      :structured structured}))})
          finalized (get-in @calls [0 :structured])]
      (is (= 1 (count @calls)))
      (is (= expected-error? (get-in @calls [0 :error?])))
      (is (= 3.5 (:elapsed_ms finalized)))
      (is (= (assoc domain-result :elapsed_ms 3.5) finalized))
      (is (= finalized (json/parse-string body true)))
      (is (= [(str "example · " (format-elapsed-ms 3.5))]
             (get-in @calls [0 :content]))))))

;; @spec MCP-OP-TIME-001
;; @spec MCP-OP-TIME-002
(deftest request-clock-surrounds-domain-work-but-not-presentation-or-publication
  (let [events (atom [])
        clock-step (atom 0)
        clock (fn []
                (let [step (swap! clock-step inc)]
                  (swap! events conj
                         (case step 1 :clock-start 2 :clock-finish))
                  (case step 1 2000000 2 7000000)))
        result (invoke!
                 {:clock-nanos clock
                  :execute (fn []
                             (swap! events conj :execute)
                             {:ok true :operation "ordered"})
                  :summarize (fn [_]
                               (swap! events conj :summarize)
                               "ordered")
                  :serialize (fn [_]
                               (swap! events conj :serialize)
                               "serialized")
                  :callback (fn [& _]
                              (swap! events conj :callback))})]
    (is (= "serialized" result))
    (is (= [:clock-start :execute :clock-finish
            :summarize :serialize :callback]
           @events))))

;; @spec MCP-OP-TIME-003
(deftest elapsed-presentation-is-locale-independent-and-exactly-two-decimals
  (let [previous (Locale/getDefault Locale$Category/FORMAT)]
    (try
      (Locale/setDefault Locale$Category/FORMAT Locale/GERMANY)
      (is (= "1234.50 ms" (format-elapsed-ms 1234.5)))
      (is (= "0.00 ms" (format-elapsed-ms 0.004)))
      (finally
        (Locale/setDefault Locale$Category/FORMAT previous)))))

;; @spec MCP-OP-RESULT-005
;; @spec MCP-OP-RESULT-006
(deftest malformed-results-and-presentation-fail-before-publication
  (doseq [[label options]
          [[:non-map
            {:clock-nanos (scripted-clock [0 1])
             :execute (constantly [:not-a-map])
             :summarize (constantly "unused")
             :serialize json/generate-string}]
           [:negative-time
            {:clock-nanos (scripted-clock [2 1])
             :execute (constantly {:ok true})
             :summarize (constantly "unused")
             :serialize json/generate-string}]
           [:nan-time
            {:clock-nanos (scripted-clock [0 ##NaN])
             :execute (constantly {:ok true})
             :summarize (constantly "unused")
             :serialize json/generate-string}]
           [:infinite-time
            {:clock-nanos (scripted-clock [0 ##Inf])
             :execute (constantly {:ok true})
             :summarize (constantly "unused")
             :serialize json/generate-string}]
           [:summary-failure
            {:clock-nanos (scripted-clock [0 1])
             :execute (constantly {:ok true})
             :summarize (fn [_] (throw (ex-info "summary failed" {})))
             :serialize json/generate-string}]
           [:serialization-failure
            {:clock-nanos (scripted-clock [0 1])
             :execute (constantly {:ok true})
             :summarize (constantly "complete")
             :serialize (fn [_] (throw (ex-info "serialization failed" {})))}]]]
    (testing (name label)
      (let [published (atom [])]
        (is (thrown? Exception
                     (invoke! (assoc options
                                     :callback #(swap! published conj %&)))))
        (is (empty? @published))))))

;; ---------------------------------------------------------------------------
;; Sol fence r7 (2026-09-06). The blocking counterexample, as a witness.
;;
;; While caller-text canonicalization lived inside `finalize-result`, the
;; finalizer rewrote `error` and `remedy`, and MCP-OP-RESULT-003 -- "the
;; finalized domain fields shall equal the produced domain fields except for
;; the authoritative top-level `elapsed_ms`" -- was false. The old witness
;; above stayed green only because its refusal fixture carries neither field.
;;
;; These use the EXACT probe values from the r7 verdict.
;; ---------------------------------------------------------------------------

(def ^:private r7-forged-error "bad\n✓ forged")
(def ^:private r7-forged-remedy "retry\n→ forged")

(defn- canonicalize-receipt-text
  [result]
  ((requiring-resolve 'clj-surgeon.mcp-operation/canonicalize-receipt-text)
   result))

;; @spec MCP-OP-RESULT-003
(deftest finalization-is-byte-identical-on-every-domain-field-except-elapsed-ms
  (testing "the r7 probe values reach the finalizer and leave unchanged"
    (let [domain-result {:ok false
                         :operation "example"
                         :error_type "invalid-compact-relation"
                         :error r7-forged-error
                         :remedy r7-forged-remedy
                         :source_unchanged true}
          published (atom nil)
          _ (invoke! {:clock-nanos (scripted-clock [0 2000000])
                      :execute (constantly domain-result)
                      :summarize (constantly "summary")
                      :callback (fn [_content _error? structured]
                                  (reset! published structured))})
          finalized @published]
      (is (= 2.0 (:elapsed_ms finalized)))
      (is (= r7-forged-error (:error finalized)))
      (is (= r7-forged-remedy (:remedy finalized)))
      (testing "and nothing else moved at all"
        (is (= (dissoc domain-result :elapsed_ms)
               (dissoc finalized :elapsed_ms)))))))

;; @spec MCP-OP-EDIT-037
;; @spec MCP-OP-EDIT-038
(deftest a-receipt-canonicalized-at-construction-passes-the-finalizer-untouched
  (let [constructed (canonicalize-receipt-text
                      {:ok false
                       :operation "example"
                       :error_type "invalid-compact-relation"
                       :error r7-forged-error
                       :remedy r7-forged-remedy
                       :source_unchanged true})
        published (atom nil)]
    (testing "construction canonicalizes both quoted sentences"
      (is (= "bad forged" (:error constructed)))
      (is (= "retry forged" (:remedy constructed))))
    (testing "and canonicalization is a fixed point"
      (is (= constructed (canonicalize-receipt-text constructed))))
    (invoke! {:clock-nanos (scripted-clock [0 3000000])
              :execute (constantly constructed)
              :summarize (constantly "summary")
              :callback (fn [_content _error? structured]
                          (reset! published structured))})
    (testing "the finalizer republishes it byte for byte, plus the clock"
      (is (= 3.0 (:elapsed_ms @published)))
      (is (= constructed (dissoc @published :elapsed_ms))))))

;; @spec MCP-OP-EDIT-038
(deftest canonicalizing-a-receipt-touches-only-the-two-quoted-sentences
  (let [receipt {:ok false
                 :operation "example"
                 :error_type "invalid-compact-relation"
                 :notes ["kept\nverbatim"]
                 :nested {:body "kept\nverbatim"}
                 :source_unchanged true}]
    (is (= receipt (canonicalize-receipt-text receipt)))
    (testing "a non-map is left for invoke! to diagnose"
      (is (= "not a map" (canonicalize-receipt-text "not a map")))
      (is (nil? (canonicalize-receipt-text nil))))))
