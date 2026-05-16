(ns clj-surgeon.forms-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clj-surgeon.forms :as forms]))

;; ============================================================
;; classify — the heart of form classification
;; ============================================================

(deftest test-classify-core-forms
  (testing "every core Clojure defining form classifies to its canonical kind"
    (is (= :def         (forms/classify "def")))
    (is (= :defn        (forms/classify "defn")))
    (is (= :defn-       (forms/classify "defn-")))
    (is (= :defonce     (forms/classify "defonce")))
    (is (= :defmacro    (forms/classify "defmacro")))
    (is (= :defmethod   (forms/classify "defmethod")))
    (is (= :defmulti    (forms/classify "defmulti")))
    (is (= :defprotocol (forms/classify "defprotocol")))
    (is (= :defrecord   (forms/classify "defrecord")))
    (is (= :deftype     (forms/classify "deftype")))
    (is (= :declare     (forms/classify "declare")))))

(deftest test-classify-explicit-aliases
  (testing "Guardrails >defn and >defn- are recognized"
    (is (= :defn  (forms/classify ">defn")))
    (is (= :defn- (forms/classify ">defn-")))))

(deftest test-classify-namespace-qualified-auto-detection
  (testing "alias/defn auto-resolves regardless of what alias is used"
    (is (= :defn  (forms/classify "mu/defn")))
    (is (= :defn  (forms/classify "m/defn")))
    (is (= :defn  (forms/classify "malli/defn")))
    (is (= :defn  (forms/classify "s/defn")))
    (is (= :defn  (forms/classify "schema.core/defn"))))
  (testing "alias/defn- auto-resolves as private"
    (is (= :defn- (forms/classify "mu/defn-")))
    (is (= :defn- (forms/classify "m/defn-"))))
  (testing "other core forms work through / too"
    (is (= :def      (forms/classify "my.ns/def")))
    (is (= :defmacro (forms/classify "util/defmacro")))
    (is (= :defonce  (forms/classify "state/defonce")))))

(deftest test-classify-non-defining-forms
  (testing "non-defining forms return nil"
    (is (nil? (forms/classify "ns")))
    (is (nil? (forms/classify "let")))
    (is (nil? (forms/classify "if")))
    (is (nil? (forms/classify "str/join")))
    (is (nil? (forms/classify nil)))))

(deftest test-classify-no-false-positives-on-slash
  (testing "qualified symbols that aren't defining forms return nil"
    (is (nil? (forms/classify "str/join")))
    (is (nil? (forms/classify "db/query")))
    (is (nil? (forms/classify "my.ns/default")))
    (is (nil? (forms/classify "my.ns/defn-routes")))))

;; ============================================================
;; defining-form? — boolean predicate
;; ============================================================

(deftest test-defining-form?
  (testing "true for all recognized forms"
    (is (forms/defining-form? "defn"))
    (is (forms/defining-form? ">defn"))
    (is (forms/defining-form? "mu/defn"))
    (is (forms/defining-form? "def"))
    (is (forms/defining-form? "declare")))
  (testing "false for non-defining forms"
    (is (not (forms/defining-form? "ns")))
    (is (not (forms/defining-form? "let")))
    (is (not (forms/defining-form? nil)))))

;; ============================================================
;; private-form? — the bug that was: >defn- must be private
;; ============================================================

(deftest test-private-form?
  (testing "defn- variants are all private"
    (is (forms/private-form? "defn-"))
    (is (forms/private-form? ">defn-"))
    (is (forms/private-form? "mu/defn-"))
    (is (forms/private-form? "m/defn-")))
  (testing "public forms are not private"
    (is (not (forms/private-form? "defn")))
    (is (not (forms/private-form? ">defn")))
    (is (not (forms/private-form? "mu/defn")))
    (is (not (forms/private-form? "def")))
    (is (not (forms/private-form? nil)))))

;; ============================================================
;; has-arglists? — defn-like forms have arg vectors
;; ============================================================

(deftest test-has-arglists?
  (testing "defn and defn- variants have arglists"
    (is (forms/has-arglists? "defn"))
    (is (forms/has-arglists? "defn-"))
    (is (forms/has-arglists? ">defn"))
    (is (forms/has-arglists? ">defn-"))
    (is (forms/has-arglists? "mu/defn"))
    (is (forms/has-arglists? "mu/defn-")))
  (testing "def, defonce, defmacro etc do not"
    (is (not (forms/has-arglists? "def")))
    (is (not (forms/has-arglists? "defonce")))
    (is (not (forms/has-arglists? "defmacro")))
    (is (not (forms/has-arglists? "defmulti")))
    (is (not (forms/has-arglists? nil)))))

;; ============================================================
;; Integration: the >defn- extraction-closure bug
;; ============================================================

(deftest test-private-form-bug-regression
  (testing "the exact bug: (= \"defn-\" type) missed >defn- and mu/defn-"
    (is (forms/private-form? ">defn-")
        ">defn- must be private — extraction-closure was missing this")
    (is (forms/private-form? "mu/defn-")
        "mu/defn- must be private — same class of bug")))

;; ============================================================
;; deftest classification (PR #13, escherize)
;; ============================================================

(deftest test-classify-deftest
  (testing "deftest is recognized as a defining form"
    (is (= :deftest (forms/classify "deftest")))
    (is (forms/defining-form? "deftest")))
  (testing "deftest is not private"
    (is (not (forms/private-form? "deftest"))))
  (testing "deftest does not have arglists"
    (is (not (forms/has-arglists? "deftest")))))

;; ============================================================
;; .clj-surgeon.edn — project-local aliases (pure tests)
;; ============================================================

(deftest test-project-aliases-classify
  (testing "project aliases extend classify with Metabase-style macros"
    (with-redefs [forms/project-aliases (atom {"defendpoint"   {:kind :defn}
                                               "defenterprise" {:kind :defn}
                                               "defsetting"    {:kind :def}})]
      (is (= :defn (forms/classify "defendpoint")))
      (is (= :defn (forms/classify "defenterprise")))
      (is (= :def  (forms/classify "defsetting")))
      (is (forms/defining-form? "defendpoint"))
      (is (forms/has-arglists? "defendpoint")
          "defendpoint mapped to :defn must report has-arglists? true")
      (is (not (forms/has-arglists? "defsetting"))
          "defsetting mapped to :def must report has-arglists? false")
      (is (not (forms/private-form? "defendpoint"))
          "defendpoint not private"))))

(deftest test-project-aliases-empty-by-default
  (testing "empty project aliases do not affect classification"
    (with-redefs [forms/project-aliases (atom {})]
      (is (nil? (forms/classify "defendpoint")))
      (is (= :defn (forms/classify "defn")))
      (is (= :defn (forms/classify "mu/defn"))))))

(deftest test-precedence-core-beats-project
  (testing "core-forms wins over project-aliases for the same key"
    (with-redefs [forms/project-aliases (atom {"defn" {:kind :def}})]
      (is (= :defn (forms/classify "defn"))
          "core 'defn' must remain :defn even if config tries to override"))))

(deftest test-precedence-explicit-beats-project
  (testing "in-source explicit-aliases wins over project-aliases"
    (with-redefs [forms/project-aliases (atom {">defn" {:kind :def}})]
      (is (= :defn (forms/classify ">defn"))
          "in-source '>defn' must remain :defn"))))

(deftest test-precedence-project-beats-ns-qualified-split
  (testing "project-aliases wins over ns-qualified split-on-/"
    (with-redefs [forms/project-aliases (atom {"my/defn" {:kind :def}})]
      (is (= :def (forms/classify "my/defn"))
          "project-aliases must win over ns-qualified split"))))

(deftest test-project-aliases-still-allows-tier-4-passthrough
  (testing "project-aliases that don't match still let ns-qualified split work"
    (with-redefs [forms/project-aliases (atom {"defendpoint" {:kind :defn}})]
      (is (= :defn (forms/classify "mu/defn"))
          "mu/defn still resolves via tier-4 ns-qualified split"))))

;; ============================================================
;; spec — full spec map lookup
;; ============================================================

(deftest test-spec-returns-kind-for-core-forms
  (testing "core forms return {:kind kw} with no fields"
    (is (= {:kind :defn} (forms/spec "defn")))
    (is (= {:kind :def}  (forms/spec "def")))))

(deftest test-spec-returns-project-spec-with-fields
  (testing "project aliases return full spec including :fields"
    (let [dummy-fn (fn [_] :extracted)]
      (with-redefs [forms/project-aliases (atom {"defendpoint" {:kind :defn
                                                                :fields {:route dummy-fn}}})]
        (let [s (forms/spec "defendpoint")]
          (is (= :defn (:kind s)))
          (is (fn? (get-in s [:fields :route]))))))))

;; ============================================================
;; End-to-end: real .clj-surgeon.edn read from temp directory
;; ============================================================

(defn- mk-tmp-dir [name]
  (let [d (java.nio.file.Files/createTempDirectory
           name (into-array java.nio.file.attribute.FileAttribute []))]
    (.toFile d)))

(defn- spit-edn [dir filename content]
  (let [f (io/file dir filename)]
    (io/make-parents f)
    (spit f content)
    f))

(defn- rm-rf [^java.io.File f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)]
      (rm-rf child)))
  (.delete f))

(deftest test-init-from-file-reads-real-config
  (let [dir (mk-tmp-dir "clj-surgeon-e2e-")]
    (try
      (spit-edn dir ".clj-surgeon.edn"
                "{:aliases {\"defendpoint\"   :defn\n            \"defenterprise\" :defn\n            \"defsetting\"    :def}}\n")
      (let [src-file (spit-edn dir "src/foo.clj" "(ns foo)\n(defendpoint x [a] a)\n")]
        (testing "init reads + parses the config file"
          (reset! forms/project-aliases {})
          (forms/init-from-file! (.getPath src-file))
          (is (= {"defendpoint"   {:kind :defn}
                  "defenterprise" {:kind :defn}
                  "defsetting"    {:kind :def}}
                 @forms/project-aliases)))
        (testing "classify uses the loaded aliases"
          (is (= :defn (forms/classify "defendpoint")))
          (is (= :def  (forms/classify "defsetting")))
          (is (forms/has-arglists? "defendpoint"))
          (is (not (forms/has-arglists? "defsetting")))))
      (finally
        (rm-rf dir)
        (reset! forms/project-aliases {})))))

(deftest test-init-from-file-walks-up-from-deep-file
  (let [dir (mk-tmp-dir "clj-surgeon-walkup-")]
    (try
      (spit-edn dir ".clj-surgeon.edn"
                "{:aliases {\"defendpoint\" :defn}}\n")
      (let [deep-file (spit-edn dir "src/a/b/c/deep.clj" "(ns a.b.c.deep)\n")]
        (reset! forms/project-aliases {})
        (forms/init-from-file! (.getPath deep-file))
        (is (= {"defendpoint" {:kind :defn}} @forms/project-aliases)
            "config should be found by walking up from deep nested file"))
      (finally
        (rm-rf dir)
        (reset! forms/project-aliases {})))))

(deftest test-init-from-file-no-config-yields-empty
  (let [dir (mk-tmp-dir "clj-surgeon-noconfig-")]
    (try
      (let [src-file (spit-edn dir "src/foo.clj" "(ns foo)\n")]
        (reset! forms/project-aliases {"stale" {:kind :defn}})
        (forms/init-from-file! (.getPath src-file))
        (is (= {} @forms/project-aliases)
            "missing config resets to empty, doesn't carry stale state"))
      (finally
        (rm-rf dir)
        (reset! forms/project-aliases {})))))

(deftest test-init-from-file-malformed-edn-throws
  (let [dir (mk-tmp-dir "clj-surgeon-malformed-")]
    (try
      (spit-edn dir ".clj-surgeon.edn" "{:aliases {\"x\" :defn") ;; unbalanced
      (let [src-file (spit-edn dir "src/foo.clj" "(ns foo)\n")]
        (reset! forms/project-aliases {})
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"invalid EDN"
                              (forms/init-from-file! (.getPath src-file)))))
      (finally
        (rm-rf dir)
        (reset! forms/project-aliases {})))))

(deftest test-init-from-file-non-string-key-throws
  (let [dir (mk-tmp-dir "clj-surgeon-badkey-")]
    (try
      (spit-edn dir ".clj-surgeon.edn"
                "{:aliases {defendpoint :defn}}\n")    ;; symbol key, not string
      (let [src-file (spit-edn dir "src/foo.clj" "(ns foo)\n")]
        (reset! forms/project-aliases {})
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #":aliases keys must be strings"
                              (forms/init-from-file! (.getPath src-file)))))
      (finally
        (rm-rf dir)
        (reset! forms/project-aliases {})))))

(deftest test-init-from-file-closest-config-wins
  (let [dir (mk-tmp-dir "clj-surgeon-closest-")]
    (try
      (spit-edn dir ".clj-surgeon.edn" "{:aliases {\"outer\" :defn}}\n")
      (spit-edn dir "sub/.clj-surgeon.edn" "{:aliases {\"inner\" :def}}\n")
      (let [inner-file (spit-edn dir "sub/src/foo.clj" "(ns foo)\n")]
        (reset! forms/project-aliases {})
        (forms/init-from-file! (.getPath inner-file))
        (is (= {"inner" {:kind :def}} @forms/project-aliases)
            "closest config wins (sub/.clj-surgeon.edn, not root)"))
      (finally
        (rm-rf dir)
        (reset! forms/project-aliases {})))))
