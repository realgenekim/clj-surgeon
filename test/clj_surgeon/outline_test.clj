(ns clj-surgeon.outline-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.outline :as outline]
            [clojure.string :as str]))

(def simple-ns
  "(ns my.app
  (:require [clojure.string :as str]))

(def version \"1.0\")

;; The main entry point
(defn -main
  \"Start the app.\"
  [& args]
  (println \"Hello\" (str/join args)))

(defn- helper [x]
  (inc x))

(defonce state (atom {}))
")

(def forward-ref-ns
  "(ns my.forward
  (:require [clojure.string :as str]))

(defn caller []
  (callee 42))

(def some-val 10)

(defn callee [x]
  (+ x some-val))
")

(defn outline-from-string [source]
  (let [tmp (java.io.File/createTempFile "ns-surgeon-test" ".clj")]
    (spit tmp source)
    (try
      (outline/outline (.getAbsolutePath tmp))
      (finally
        (.delete tmp)))))

(deftest string-symbols-are-optional-and-source-line-aware
  (let [source (str "(ns demo)\n\n"
                    "(defn- page []\n"
                    "  (str \"function onsetReady(e,now){}\"\n"
                    "       \"\\n\"\n"
                    "       \"var P={};\"\n"
                    "       \"first line\nfunction kwCheck(now){}\"\n"
                    "       \"escaped\\nfunction bargeTh(){}\"\n"
                    "       \"const hfControlsTick = function(){};\"))\n")
        default (outline/outline-source "demo.clj" source)
        explicit-off (outline/outline-source
                       "demo.clj" source {} {:include-string-symbols false})
        requested (outline/outline-source
                    "demo.clj" source {} {:include-string-symbols true})
        form (first (:forms requested))]
    (is (= default explicit-off)
        "default-off output remains byte-for-byte data-identical")
    (is (not-any? #(contains? % :string-symbols) (:forms default)))
    (is (= [{:name "onsetReady" :kind :function :line 4 :owner 'page}
            {:name "P" :kind :var :line 6 :owner 'page}
            {:name "kwCheck" :kind :function :line 8 :owner 'page}
            {:name "bargeTh" :kind :function :line 9 :owner 'page}
            {:name "hfControlsTick" :kind :const :line 10 :owner 'page}]
           (:string-symbols form)))
    (is (false? (:string-symbols-truncated form)))))

(deftest string-symbols-handle-no-strings-and-cap-results
  (is (= {:symbols [] :truncated? false}
         (outline/string-symbols-for-form
           {:source "(defn plain [] 1)" :line 20 :name 'plain})))
  (let [declarations (str/join ";" (map #(str "var symbol" % "=0")
                                         (range 513)))
        result (outline/string-symbols-for-form
                 {:source (str "(defn many [] \"" declarations "\")")
                  :line 1
                  :name 'many})]
    (is (= 512 (count (:symbols result))))
    (is (true? (:truncated? result)))
    (is (= "symbol511" (:name (last (:symbols result)))))))

(deftest string-symbols-cover-the-bounded-js-declaration-grammar
  (let [result (outline/string-symbols-for-form
                 {:source (str "(defn declarations []\n"
                               "  (str \"let localThing=1;\"\n"
                               "       \"$assigned=function(){};\"\n"
                               "       \"handler:function(){}\"))")
                  :line 40
                  :name 'declarations})]
    (is (= [{:name "localThing" :kind :let :line 41
             :owner 'declarations}
            {:name "$assigned" :kind :assignment :line 42
             :owner 'declarations}
            {:name "handler" :kind :property :line 43
             :owner 'declarations}]
           (:symbols result)))
    (is (false? (:truncated? result)))))

(deftest test-basic-outline
  (let [result (outline-from-string simple-ns)]
    (testing "namespace detection"
      (is (= 'my.app (:ns result))))

    (testing "form count"
      (is (= 4 (:form-count result))))

    (testing "form types"
      (let [types (mapv :type (:forms result))]
        (is (= '[def defn defn- defonce] types))))

    (testing "form names"
      (let [names (mapv :name (:forms result))]
        (is (= '[version -main helper state] names))))

    (testing "line boundaries"
      (let [main-form (first (filter #(= '-main (:name %)) (:forms result)))]
        (is (some? main-form))
        (is (= 7 (:line main-form)))
        (is (= 10 (:end-line main-form)))))

    (testing "comment headers detected"
      (let [main-form (first (filter #(= '-main (:name %)) (:forms result)))]
        (is (= 6 (:comment-start main-form)))))

    (testing "arglists"
      (let [main-form (first (filter #(= '-main (:name %)) (:forms result)))]
        (is (= "[& args]" (:args main-form)))))))

(deftest test-form-boundaries-precise
  (let [result (outline-from-string simple-ns)
        forms (:forms result)]
    (testing "first form starts at correct line"
      (is (= 4 (:line (first forms)))))

    (testing "last form ends at correct line"
      (is (= 15 (:end-line (last forms)))))))

(deftest test-various-def-types
  (let [source "(ns my.types)

(def a 1)
(defn b [] 2)
(defn- c [] 3)
(defonce d (atom 4))
(defmacro e [x] `(inc ~x))
(defmulti f class)
(defprotocol G (h [this]))
(declare z)
"
        result (outline-from-string source)
        types (mapv :type (:forms result))]
    (is (= '[def defn defn- defonce defmacro defmulti defprotocol declare]
           types))))

(deftest test-metadata-handling
  (let [source "(ns my.meta)

(def ^:private secret 42)

(def ^:dynamic *binding* nil)

(defn ^:deprecated old-fn [] :old)
"
        result (outline-from-string source)
        names (mapv :name (:forms result))]
    (testing "metadata stripped from names"
      (is (every? some? names))
      (is (not-any? #(str/starts-with? (str %) "^") (map str names))))
    (testing "^:private def name is 'secret', not '^:private'"
      (is (some #(= 'secret %) names)))
    (testing "^:dynamic def name is '*binding*'"
      (is (some #(= '*binding* %) names)))))

(deftest test-guardrails-defn
  (let [source "(ns my.guardrails)

(>defn validated-fn
  [x y]
  [int? string? => map?]
  {:x x :y y})
"
        result (outline-from-string source)
        form (first (:forms result))]
    (is (= '>defn (:type form)))
    (is (= 'validated-fn (:name form)))))

(deftest test-empty-file
  (let [result (outline-from-string "(ns my.empty)\n")]
    (is (= 'my.empty (:ns result)))
    (is (= 0 (:form-count result)))
    (is (empty? (:forms result)))))

(defn- outline-from-cljc-string [source]
  (let [tmp (java.io.File/createTempFile "ns-surgeon-test" ".cljc")]
    (spit tmp source)
    (try
      (outline/outline (.getAbsolutePath tmp))
      (finally (.delete tmp)))))

(deftest test-cljc-reader-conditional-forms
  (testing "Forms inside #?(:clj ...) and #?(:cljs ...) are surfaced in the
            outline with the correct :platforms tags. This is a NEW capability:
            previously such forms were silently skipped because they live inside
            reader-macro nodes rather than top-level lists."
    (let [source "(ns my.app)

(defn shared [] :ok)

#?(:clj
   (defn jvm-only [] (System/currentTimeMillis)))

#?(:cljs
   (defn js-only [] (.now js/Date)))
"
          result (outline-from-cljc-string source)
          by-name (into {} (map (juxt :name identity)) (:forms result))]
      (is (= 3 (:form-count result)))
      (is (= [:clj :cljs] (:platforms (by-name 'shared))))
      (is (= [:clj]       (:platforms (by-name 'jvm-only))))
      (is (= [:cljs]      (:platforms (by-name 'js-only)))))))

(deftest test-clj-file-platforms-tag
  (testing ".clj file has every form tagged with :platforms [:clj]"
    (let [result (outline-from-string "(ns my.x) (defn f [])")]
      (is (= [:clj] (:platforms (first (:forms result))))))))

;; ============================================================
;; PR #12: meta-tagged arglist fix (escherize)
;; ============================================================

(deftest test-meta-tagged-arglist
  (testing "outer meta on arglist (^String [a]) is stripped — arglist is [a]"
    (let [result (outline-from-string
                  "(ns my.x)
                   (defn outer-hint
                     \"doc\"
                     ^String [a b]
                     a)")
          form (first (filter #(= 'outer-hint (:name %)) (:forms result)))]
      (is (= "[a b]" (:args form))
          "function-level return-type hint must not appear in :args")))
  (testing "param meta inside arglist ([^String s]) is preserved"
    (let [result (outline-from-string
                  "(ns my.x)
                   (defn inner-hint
                     \"doc\"
                     [^String s x]
                     s)")
          form (first (filter #(= 'inner-hint (:name %)) (:forms result)))]
      (is (= "[^String s x]" (:args form))
          "parameter-level hint must remain attached to the param")))
  (testing "both outer and param meta together"
    (let [result (outline-from-string
                  "(ns my.x)
                   (defn both
                     \"doc\"
                     ^Long [^String s ^Integer i]
                     i)")
          form (first (filter #(= 'both (:name %)) (:forms result)))]
      (is (= "[^String s ^Integer i]" (:args form))
          "outer hint stripped, param hints kept")))
  (testing "no meta — baseline still works"
    (let [result (outline-from-string
                  "(ns my.x) (defn plain [a b] a)")
          form (first (filter #(= 'plain (:name %)) (:forms result)))]
      (is (= "[a b]" (:args form))))))

;; ============================================================
;; PR #13: deftest classification (escherize)
;; ============================================================

(deftest test-deftest-classification
  (testing "plain deftest is recognized as a defining form"
    (let [result (outline-from-string
                  "(ns my.tests (:require [clojure.test :refer [deftest is]]))
                   (deftest plain (is true))")
          form (first (filter #(= 'deftest (:type %)) (:forms result)))]
      (is (some? form))
      (is (= 'plain (:name form)))))
  (testing "deftest with metadata between symbol and name (e.g. ^:integration)"
    (let [result (outline-from-string
                  "(ns my.tests (:require [clojure.test :refer [deftest is]]))
                   (deftest ^:integration tagged (is true))")
          form (first (filter #(= 'deftest (:type %)) (:forms result)))]
      (is (some? form))
      (is (= 'tagged (:name form))
          "deftest with ^:integration meta on name should resolve to 'tagged'")))
  (testing "deftest counted in :form-count"
    (let [result (outline-from-string
                  "(ns my.tests (:require [clojure.test :refer [deftest is]]))
                   (deftest a (is true))
                   (deftest ^:slow b (is true))")]
      (is (= 2 (:form-count result))))))

;; ---------------------------------------------------------------------------
;; Multimethod dispatch (field case: curtain-call src/cfp_scheduler_killer/folds.clj,
;; 2026-09-02 session 4 — 117 defmethod arms collapsed to one owner "fold-event").
;; ---------------------------------------------------------------------------

(def folds-arms
  "(ns cfp-scheduler-killer.folds)

(defmulti fold-event (fn [_state payload] (:type payload)))

(defmethod fold-event \"schedule.locked\"
  [state payload]
  ;; INTENT: LENS-004
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (assoc-in state [:events slug :settings :schedule-locked?] true)
    state))

(defmethod fold-event \"schedule.unlocked\"
  [state payload]
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (assoc-in state [:events slug :settings :schedule-locked?] false)
    state))

(defmethod fold-event :agenda/published
  [state payload]
  state)

(defmethod fold-event [::sink :registered]
  [state payload]
  state)

(defn event-by-id [state id] nil)
")

(deftest outline-emits-defmethod-dispatch-source-spelling
  ;; @spec MCP-OP-DISPATCH-001
  (let [forms (:forms (outline/outline-source "folds.clj" folds-arms))
        arms (filterv #(= 'defmethod (:type %)) forms)]
    (is (= 4 (count arms)))
    (is (= ["fold-event" "fold-event" "fold-event" "fold-event"]
           (mapv (comp str :name) arms)))
    (testing "the dispatch value keeps its exact source spelling"
      (is (= ["\"schedule.locked\"" "\"schedule.unlocked\""
              ":agenda/published" "[::sink :registered]"]
             (mapv :dispatch arms))))
    (testing "non-defmethod owners carry no dispatch field"
      (is (not-any? #(contains? % :dispatch)
                    (remove #(= 'defmethod (:type %)) forms))))))
