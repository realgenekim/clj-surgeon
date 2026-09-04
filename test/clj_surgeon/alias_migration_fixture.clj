(ns clj-surgeon.alias-migration-fixture
  "Deterministic fan-out corpus for alias_migration.

  Every file is emitted twice from the same template: the pre-migration source
  and the canonical post-migration source. The canonical oracle is therefore
  derived from the fixture description, never from the planner under test."
  (:require
   [clojure.string :as str]))

(def from-lib "acid.fanout.store")
(def from-var "find-event")
(def to-lib "acid.fanout.store2")
(def to-var "fetch-event")
(def alias-policy ["store2" "st2" "es" "store-2"])

(defn- ns-form
  [namespace-name requires]
  (str "(ns " namespace-name "\n"
       "  (:require\n"
       (str/join "\n" (map #(str "   " %) requires))
       "))\n"))

(defn- render
  [{:keys [ns requires-pre requires-post body-pre body-post]}]
  {:pre (str (ns-form ns requires-pre) "\n" body-pre)
   :post (str (ns-form ns requires-post) "\n" body-post)})

;; ---------------------------------------------------------------------------
;; the twelve requiring namespaces plus three that must not change

(def file-specs
  [;; 1. docstring, comment, string literal, and a let local all named for the var
   {:file "src/acid/fanout/t01.clj"
    :ns "acid.fanout.t01"
    :alias "store2"
    :sites 3
    :requires-pre ["[acid.fanout.store :as store]" "[acid.fanout.util :as util]"]
    :requires-post ["[acid.fanout.store2 :as store2]" "[acid.fanout.util :as util]"]
    :protected [";; a comment about store/find-event"
                "\"store/find-event\""
                "\"Look up one event; see store/find-event.\""]
    :body-pre
    (str "(defn lookup\n"
         "  \"Look up one event; see store/find-event.\"\n"
         "  [id]\n"
         "  ;; a comment about store/find-event\n"
         "  (let [find-event (store/find-event id)]\n"
         "    (util/tag \"store/find-event\" find-event)))\n"
         "\n"
         "(defn lookup-two\n"
         "  [a b]\n"
         "  [(store/find-event a) (store/find-event b)])\n")
    :body-post
    (str "(defn lookup\n"
         "  \"Look up one event; see store/find-event.\"\n"
         "  [id]\n"
         "  ;; a comment about store/find-event\n"
         "  (let [find-event (store2/fetch-event id)]\n"
         "    (util/tag \"store/find-event\" find-event)))\n"
         "\n"
         "(defn lookup-two\n"
         "  [a b]\n"
         "  [(store2/fetch-event a) (store2/fetch-event b)])\n")}

   ;; 2. an #_ discard and a reader conditional whose :cljs branch has no site
   {:file "src/acid/fanout/t02.clj"
    :ns "acid.fanout.t02"
    :alias "store2"
    :sites 3
    :requires-pre ["[acid.fanout.store :as store]"]
    :requires-post ["[acid.fanout.store2 :as store2]"]
    ;; both regions are invariant under ANY migration of the old lib: a
    ;; discard and a branch this platform does not select
    :protected ["#_(store/find-event :disabled)"
                ":cljs (js-lookup id))"]
    :body-pre
    (str "(defn read-one\n"
         "  [id]\n"
         "  #_(store/find-event :disabled)\n"
         "  #?(:clj (store/find-event id) :cljs (js-lookup id)))\n"
         "\n"
         "(defn read-many\n"
         "  [ids]\n"
         "  (mapv store/find-event ids))\n"
         "\n"
         "(defn read-first\n"
         "  [ids]\n"
         "  (store/find-event (first ids)))\n")
    :body-post
    (str "(defn read-one\n"
         "  [id]\n"
         "  #_(store/find-event :disabled)\n"
         "  #?(:clj (store2/fetch-event id) :cljs (js-lookup id)))\n"
         "\n"
         "(defn read-many\n"
         "  [ids]\n"
         "  (mapv store2/fetch-event ids))\n"
         "\n"
         "(defn read-first\n"
         "  [ids]\n"
         "  (store2/fetch-event (first ids)))\n")}

   ;; 3. a different alias
   {:file "src/acid/fanout/t03.clj"
    :ns "acid.fanout.t03"
    :alias "store2"
    :sites 3
    :requires-pre ["[acid.fanout.store :as st]" "[clojure.string :as str]"]
    :requires-post ["[acid.fanout.store2 :as store2]" "[clojure.string :as str]"]
    :protected []
    :body-pre
    (str "(defn describe\n"
         "  [id]\n"
         "  (str/upper-case (str (st/find-event id))))\n"
         "\n"
         "(defn describe-all\n"
         "  [ids]\n"
         "  [(st/find-event (first ids)) (st/find-event (last ids))])\n")
    :body-post
    (str "(defn describe\n"
         "  [id]\n"
         "  (str/upper-case (str (store2/fetch-event id))))\n"
         "\n"
         "(defn describe-all\n"
         "  [ids]\n"
         "  [(store2/fetch-event (first ids)) (store2/fetch-event (last ids))])\n")}

   ;; 4. fully qualified use and no alias
   {:file "src/acid/fanout/t04.clj"
    :ns "acid.fanout.t04"
    :alias "store2"
    :sites 3
    :requires-pre ["acid.fanout.store"]
    :requires-post ["[acid.fanout.store2 :as store2]"]
    :protected []
    :body-pre
    (str "(defn qualified-one\n"
         "  [id]\n"
         "  (acid.fanout.store/find-event id))\n"
         "\n"
         "(defn qualified-many\n"
         "  [ids]\n"
         "  [(acid.fanout.store/find-event (first ids))\n"
         "   (acid.fanout.store/find-event (last ids))])\n")
    :body-post
    (str "(defn qualified-one\n"
         "  [id]\n"
         "  (store2/fetch-event id))\n"
         "\n"
         "(defn qualified-many\n"
         "  [ids]\n"
         "  [(store2/fetch-event (first ids))\n"
         "   (store2/fetch-event (last ids))])\n")}

   ;; 5. an existing alias named store2 forces the second policy entry
   {:file "src/acid/fanout/t05.clj"
    :ns "acid.fanout.t05"
    :alias "st2"
    :sites 3
    :collided ["store2"]
    :requires-pre ["[acid.fanout.store :as store]"
                   "[acid.fanout.secondary :as store2]"]
    :requires-post ["[acid.fanout.store2 :as st2]"
                    "[acid.fanout.secondary :as store2]"]
    :protected ["(store2/other id)"]
    :body-pre
    (str "(defn combined\n"
         "  [id]\n"
         "  [(store/find-event id) (store2/other id)])\n"
         "\n"
         "(defn combined-two\n"
         "  [a b]\n"
         "  [(store/find-event a) (store/find-event b)])\n")
    :body-post
    (str "(defn combined\n"
         "  [id]\n"
         "  [(st2/fetch-event id) (store2/other id)])\n"
         "\n"
         "(defn combined-two\n"
         "  [a b]\n"
         "  [(st2/fetch-event a) (st2/fetch-event b)])\n")}

   ;; 6. requires the old lib with :refer
   {:file "src/acid/fanout/t06.clj"
    :ns "acid.fanout.t06"
    :alias "store2"
    :sites 3
    :requires-pre ["[acid.fanout.store :refer [find-event]]"]
    :requires-post ["[acid.fanout.store2 :as store2]"]
    :protected []
    :body-pre
    (str "(defn referred-one\n"
         "  [id]\n"
         "  (find-event id))\n"
         "\n"
         "(defn referred-many\n"
         "  [ids]\n"
         "  [(find-event (first ids)) (find-event (last ids))])\n")
    :body-post
    (str "(defn referred-one\n"
         "  [id]\n"
         "  (store2/fetch-event id))\n"
         "\n"
         "(defn referred-many\n"
         "  [ids]\n"
         "  [(store2/fetch-event (first ids)) (store2/fetch-event (last ids))])\n")}

   ;; 7. a second required namespace that also exports find-event, aliased
   {:file "src/acid/fanout/t07.clj"
    :ns "acid.fanout.t07"
    :alias "store2"
    :sites 3
    :requires-pre ["[acid.fanout.store :as store]" "[acid.fanout.mirror :as mirror]"
                   "[acid.fanout.store-pg :as store-pg]"]
    :requires-post ["[acid.fanout.store2 :as store2]" "[acid.fanout.mirror :as mirror]"
                    "[acid.fanout.store-pg :as store-pg]"]
    :protected ["(mirror/find-event id)"
                "[acid.fanout.store-pg :as store-pg]"
                "(store-pg/write! id)"]
    :body-pre
    (str "(defn both\n"
         "  [id]\n"
         "  [(store/find-event id) (mirror/find-event id) (store-pg/write! id)])\n"
         "\n"
         "(defn both-two\n"
         "  [a b]\n"
         "  [(store/find-event a) (store/find-event b)])\n")
    :body-post
    (str "(defn both\n"
         "  [id]\n"
         "  [(store2/fetch-event id) (mirror/find-event id) (store-pg/write! id)])\n"
         "\n"
         "(defn both-two\n"
         "  [a b]\n"
         "  [(store2/fetch-event a) (store2/fetch-event b)])\n")}

   ;; 8. a LOCAL named like the first policy entry is not a collision: a local
   ;;    cannot shadow the qualifier of store2/fetch-event. Plus metadata
   ;;    carrying the var.
   {:file "src/acid/fanout/t08.clj"
    :ns "acid.fanout.t08"
    :alias "store2"
    :sites 3
    :requires-pre ["[acid.fanout.store :as store]"]
    :requires-post ["[acid.fanout.store2 :as store2]"]
    :protected ["^{:doc \"store/find-event\"}"
                "(let [store2 {:kind :local}]"]
    :body-pre
    (str "(defn shadowing\n"
         "  [id]\n"
         "  (let [store2 {:kind :local}]\n"
         "    [store2 (store/find-event id)]))\n"
         "\n"
         "(defn annotated\n"
         "  [id]\n"
         "  ^{:doc \"store/find-event\"} [(store/find-event id) (store/find-event id)])\n")
    :body-post
    (str "(defn shadowing\n"
         "  [id]\n"
         "  (let [store2 {:kind :local}]\n"
         "    [store2 (store2/fetch-event id)]))\n"
         "\n"
         "(defn annotated\n"
         "  [id]\n"
         "  ^{:doc \"store/find-event\"} [(store2/fetch-event id) (store2/fetch-event id)])\n")}

   ;; 9. commas, nested destructuring, and a two-arity fn
   {:file "src/acid/fanout/t09.clj"
    :ns "acid.fanout.t09"
    :alias "store2"
    :sites 3
    :requires-pre ["[acid.fanout.store :as store]"]
    :requires-post ["[acid.fanout.store2 :as store2]"]
    :protected ["{:keys [id, tag]}"]
    :body-pre
    (str "(defn destructured\n"
         "  [{:keys [id, tag]}]\n"
         "  [tag (store/find-event id)])\n"
         "\n"
         "(defn arities\n"
         "  ([id] (store/find-event id))\n"
         "  ([id _extra] (store/find-event id)))\n")
    :body-post
    (str "(defn destructured\n"
         "  [{:keys [id, tag]}]\n"
         "  [tag (store2/fetch-event id)])\n"
         "\n"
         "(defn arities\n"
         "  ([id] (store2/fetch-event id))\n"
         "  ([id _extra] (store2/fetch-event id)))\n")}

   ;; 10. TWO real ns-level alias collisions push the policy to its third entry,
   ;;     while loop and as-> locals of the same names are deliberately NOT
   ;;     collisions.
   {:file "src/acid/fanout/t10.clj"
    :ns "acid.fanout.t10"
    :alias "es"
    :sites 3
    :collided ["store2" "st2"]
    :requires-pre ["[acid.fanout.store :as store]"
                   "[acid.fanout.secondary :as store2]"
                   "[acid.fanout.mirror :as st2]"]
    :requires-post ["[acid.fanout.store2 :as es]"
                    "[acid.fanout.secondary :as store2]"
                    "[acid.fanout.mirror :as st2]"]
    :protected ["[acid.fanout.secondary :as store2]"
                "[acid.fanout.mirror :as st2]"]
    :body-pre
    (str "(defn looping\n"
         "  [ids]\n"
         "  (loop [remaining ids store2 []]\n"
         "    (if-let [id (first remaining)]\n"
         "      (recur (rest remaining) (conj store2 (store/find-event id)))\n"
         "      store2)))\n"
         "\n"
         "(defn threading\n"
         "  [id]\n"
         "  (as-> id st2\n"
         "    (store/find-event st2)\n"
         "    (store/find-event st2)))\n")
    :body-post
    (str "(defn looping\n"
         "  [ids]\n"
         "  (loop [remaining ids store2 []]\n"
         "    (if-let [id (first remaining)]\n"
         "      (recur (rest remaining) (conj store2 (es/fetch-event id)))\n"
         "      store2)))\n"
         "\n"
         "(defn threading\n"
         "  [id]\n"
         "  (as-> id st2\n"
         "    (es/fetch-event st2)\n"
         "    (es/fetch-event st2)))\n")}

   ;; 11. letfn, and a top-level DEF named for a policy entry: also not a
   ;;     collision, because `store2` reads the var and `store2/x` reads the alias
   {:file "src/acid/fanout/t11.clj"
    :ns "acid.fanout.t11"
    :alias "store2"
    :sites 3
    :requires-pre ["[acid.fanout.store :as store]"]
    :requires-post ["[acid.fanout.store2 :as store2]"]
    :protected ["(defn store2\n  [x]\n  x)"]
    :body-pre
    (str "(defn store2\n"
         "  [x]\n"
         "  x)\n"
         "\n"
         "(defn helpers\n"
         "  [id]\n"
         "  (letfn [(inner [n] (store/find-event n))]\n"
         "    [(inner id) (store/find-event id) (store/find-event id)]))\n")
    :body-post
    (str "(defn store2\n"
         "  [x]\n"
         "  x)\n"
         "\n"
         "(defn helpers\n"
         "  [id]\n"
         "  (letfn [(inner [n] (store2/fetch-event n))]\n"
         "    [(inner id) (store2/fetch-event id) (store2/fetch-event id)]))\n")}

   ;; 12. two aliases for the same lib, plus an unnamed top-level form
   {:file "src/acid/fanout/t12.clj"
    :ns "acid.fanout.t12"
    :alias "store2"
    :sites 3
    :requires-pre ["[acid.fanout.store :as store :as-alias s]"]
    :requires-post ["[acid.fanout.store2 :as store2]"]
    :protected ["(comment \"store/find-event stays here\")"]
    :body-pre
    (str "(comment \"store/find-event stays here\")\n"
         "\n"
         "(defn mixed\n"
         "  [id]\n"
         "  [(store/find-event id) (s/find-event id)])\n"
         "\n"
         "(def preloaded (store/find-event :boot))\n")
    :body-post
    (str "(comment \"store/find-event stays here\")\n"
         "\n"
         "(defn mixed\n"
         "  [id]\n"
         "  [(store2/fetch-event id) (store2/fetch-event id)])\n"
         "\n"
         "(def preloaded (store2/fetch-event :boot))\n")}

   ;; 13. the shapes the real anchor contains, reduced from
   ;;     curtaincall-cfp src/cfp_scheduler_killer/replay.clj:119-128 and
   ;;     src/cfp_scheduler_killer/sinks.clj:693. A `binding` vector's
   ;;     left-hand side is a VAR reference, not a local; the docstring one
   ;;     line above it mentions the same symbol and must not move.
   {:file "src/acid/fanout/t13.clj"
    :ns "acid.fanout.t13"
    :alias "store2"
    :sites 0
    :lib-only? true
    :requires-pre ["[acid.fanout.store :as store]"]
    :requires-post ["[acid.fanout.store :as store]"]
    :protected ["(via store/*clock*) an hour before the CFP opens"
                ":store/plain-keyword"]
    :body-pre
    (str "(defn replay!\n"
         "  \"Born ON the simulated timeline: creation facts are stamped\n"
         "   (via store/*clock*) an hour before the CFP opens.\"\n"
         "  [sim-birth]\n"
         "  (binding [store/*clock* sim-birth]\n"
         "    (store/other-var sim-birth)))\n"
         "\n"
         "(def installed\n"
         "  (alter-var-root #'store/*default-sinks-fn* identity))\n"
         "\n"
         "(def ^{:validator store/valid?} guarded 1)\n"
         "\n"
         "(defn redefs [state]\n"
         "  (with-redefs [store/other-var (constantly true)\n"
         "                store/valid? #(deref state)]\n"
         "    (store/other-var 1)))\n"
         "\n"
         "(defn resolved []\n"
         "  @(var-get (requiring-resolve 'acid.fanout.store/*clock*)))\n"
         "\n"
         "(defn tagged [] [:store/plain-keyword])\n")
    :body-post
    (str "(defn replay!\n"
         "  \"Born ON the simulated timeline: creation facts are stamped\n"
         "   (via store/*clock*) an hour before the CFP opens.\"\n"
         "  [sim-birth]\n"
         "  (binding [store/*clock* sim-birth]\n"
         "    (store/other-var sim-birth)))\n"
         "\n"
         "(def installed\n"
         "  (alter-var-root #'store/*default-sinks-fn* identity))\n"
         "\n"
         "(def ^{:validator store/valid?} guarded 1)\n"
         "\n"
         "(defn redefs [state]\n"
         "  (with-redefs [store/other-var (constantly true)\n"
         "                store/valid? #(deref state)]\n"
         "    (store/other-var 1)))\n"
         "\n"
         "(defn resolved []\n"
         "  @(var-get (requiring-resolve 'acid.fanout.store/*clock*)))\n"
         "\n"
         "(defn tagged [] [:store/plain-keyword])\n")}])

(def bystander-specs
  [{:file "src/acid/fanout/n01.clj"
    :ns "acid.fanout.n01"
    :requires-pre ["[acid.fanout.util :as util]"]
    :requires-post ["[acid.fanout.util :as util]"]
    :protected []
    :body-pre "(defn untouched [x] (util/tag \"store/find-event\" x))\n"
    :body-post "(defn untouched [x] (util/tag \"store/find-event\" x))\n"}
   {:file "src/acid/fanout/n02.clj"
    :ns "acid.fanout.n02"
    :requires-pre ["[acid.fanout.mirror :as mirror]"]
    :requires-post ["[acid.fanout.mirror :as mirror]"]
    :protected []
    :body-pre "(defn mirrored [x] (mirror/find-event x))\n"
    :body-post "(defn mirrored [x] (mirror/find-event x))\n"}
   {:file "src/acid/fanout/n03.clj"
    :ns "acid.fanout.n03"
    :requires-pre ["[clojure.string :as str]"]
    :requires-post ["[clojure.string :as str]"]
    :protected []
    :body-pre "(defn joined [xs] (str/join \",\" xs))\n"
    :body-post "(defn joined [xs] (str/join \",\" xs))\n"}])

;; ---------------------------------------------------------------------------
;; the libraries the corpus requires, so the tree can actually load

(def support-files
  {"src/acid/fanout/store.clj"
   (str "(ns acid.fanout.store)\n\n"
        "(def ^:dynamic *clock* nil)\n\n"
        "(def ^:dynamic *default-sinks-fn* nil)\n\n"
        "(defn valid? [_] true)\n\n"
        "(defn find-event [id] {:old id})\n\n"
        "(defn other-var [id] {:other-old id})\n")
   "src/acid/fanout/store2.clj"
   "(ns acid.fanout.store2)\n\n(defn fetch-event [id] {:new id})\n"
   "src/acid/fanout/secondary.clj"
   "(ns acid.fanout.secondary)\n\n(defn other [id] {:other id})\n"
   "src/acid/fanout/mirror.clj"
   "(ns acid.fanout.mirror)\n\n(defn find-event [id] {:mirror id})\n"
   ;; a prefix-sharing sibling: a naive s/acid.fanout.store/.../ corrupts it
   "src/acid/fanout/store_pg.clj"
   "(ns acid.fanout.store-pg)\n\n(defn write! [id] {:pg id})\n"
   "src/acid/fanout/store_checkpoint.clj"
   "(ns acid.fanout.store-checkpoint)\n\n(defn mark [id] {:checkpoint id})\n"
   "src/acid/fanout/util.clj"
   "(ns acid.fanout.util)\n\n(defn tag [label value] [label value])\n\n(defn js-lookup [id] {:js id})\n"})

(def var-mode-specs
  "Targets of the VAR migration. t13 exercises lib-only shapes and never
  mentions the migrated var, so it is out of scope for that mode."
  (vec (remove :lib-only? file-specs)))

(def test-file-specs
  [;; the ns NAME shares the old lib's prefix; a naive rename makes
   ;; acid.fanout.event-store-test out of it. Its local `event-store` also
   ;; collides with the first lib-mode policy entry.
   {:file "test/acid/fanout/store_test.clj"
    :ns "acid.fanout.store-test"
    :alias "estore"
    :sites 2
    :collided ["event-store"]
    ;; a REAL ns-level alias collision on the first lib-mode policy entry,
    ;; carried by a prefix-sharing sibling
    :requires-pre ["[acid.fanout.store :as store]"
                   "[acid.fanout.store-pg :as event-store]"]
    :requires-post ["[acid.fanout.store :as store]"
                    "[acid.fanout.store-pg :as event-store]"]
    :protected ["(ns acid.fanout.store-test"
                "[acid.fanout.store-pg :as event-store]"
                "(event-store/write! id)"]
    :body-pre
    (str "(defn check\n"
         "  [id]\n"
         "  (let [found (store/find-event id)]\n"
         "    [found (store/other-var id) (event-store/write! id)]))\n")
    :body-post
    (str "(defn check\n"
         "  [id]\n"
         "  (let [found (store/find-event id)]\n"
         "    [found (store/other-var id) (event-store/write! id)]))\n")}])

;; ---------------------------------------------------------------------------
;; the lib-only migration: acid.fanout.store -> acid.fanout.event-store

(def lib-to-lib "acid.fanout.event-store")
(def lib-alias-policy ["event-store" "estore" "es" "event-store-2"])

(def lib-targets
  "Every file the lib-only migration changes, under scope src/** and test/**."
  (into (sorted-set "test/acid/fanout/store_test.clj")
        (map :file) file-specs))

(def untouched-siblings
  "Prefix-sharing namespaces a substring rename corrupts. None may change."
  ["src/acid/fanout/store_pg.clj"
   "src/acid/fanout/store_checkpoint.clj"])

(defn sha256
  [text]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %)
                    (.digest digest (.getBytes ^String text "UTF-8"))))))

(defn corpus
  "Return {:pre {path text} :post {path text} :manifest {...}}."
  []
  (let [rendered (mapv #(assoc % :rendered (render %))
                       (concat file-specs bystander-specs test-file-specs))]
    {:pre (into (sorted-map)
                (concat support-files
                        (map (juxt :file #(get-in % [:rendered :pre])) rendered)))
     :post (into (sorted-map)
                 (concat support-files
                         (map (juxt :file #(get-in % [:rendered :post])) rendered)))
     :manifest
     {:from {:lib from-lib :var from-var}
      :to {:lib to-lib :var to-var :alias-policy alias-policy}
      :targets (into (sorted-set) (map :file var-mode-specs))
      :files (into (sorted-map)
                   (map (fn [spec]
                          [(:file spec)
                           {:alias (:alias spec)
                            :sites (:sites spec)
                            :collided (vec (:collided spec))}]))
                   var-mode-specs)
      :alias-histogram (into (sorted-map) (frequencies (map :alias var-mode-specs)))
      :sites (reduce + 0 (map :sites var-mode-specs))
      :collisions-resolved (reduce + 0 (map #(count (:collided %)) var-mode-specs))
      :lib {:from {:lib from-lib :var nil}
            :to {:lib lib-to-lib :var nil :alias-policy lib-alias-policy}
            :targets lib-targets
            :defining-file "src/acid/fanout/store.clj"
            :renamed-file "src/acid/fanout/event_store.clj"
            :untouched-siblings untouched-siblings}
      :protected
      (into (sorted-map)
            (map (fn [spec]
                   [(:file spec)
                    (mapv (fn [region] {:region region :sha256 (sha256 region)})
                          (:protected spec))]))
            (concat file-specs bystander-specs test-file-specs))}}))

(defn behaviour-suite
  "One assertion per migrated site, as a Babashka-runnable source file."
  []
  (str "(ns acid.fanout.fan-test\n"
       "  (:require\n"
       (str/join "\n"
                 (map #(str "   [" (:ns %) " :as " (last (str/split (:ns %) #"\.")) "]")
                      file-specs))
       "\n   [clojure.string]))\n\n"
       "(def failures (atom []))\n\n"
       "(defn check [label actual]\n"
       "  (let [text (pr-str actual)]\n"
       "    (when-not (and (clojure.string/includes? text \":new\")\n"
       "                   (not (clojure.string/includes? text \":old\")))\n"
       "      (swap! failures conj label))))\n\n"
       "(defn -main [& _]\n"
       "  (check :t01 (t01/lookup 1))\n"
       "  (check :t01b (t01/lookup-two 1 2))\n"
       "  (check :t02 (t02/read-one 1))\n"
       "  (check :t02b (t02/read-many [1]))\n"
       "  (check :t02c (t02/read-first [1]))\n"
       "  (check :t03 (t03/describe-all [1 2]))\n"
       "  (check :t04 (t04/qualified-one 1))\n"
       "  (check :t04b (t04/qualified-many [1 2]))\n"
       "  (check :t05 (t05/combined-two 1 2))\n"
       "  (check :t06 (t06/referred-one 1))\n"
       "  (check :t06b (t06/referred-many [1 2]))\n"
       "  (check :t07 (t07/both-two 1 2))\n"
       "  (check :t08 (t08/shadowing 1))\n"
       "  (check :t08b (t08/annotated 1))\n"
       "  (check :t09 (t09/destructured {:id 1 :tag :a}))\n"
       "  (check :t09b (t09/arities 1))\n"
       "  (check :t10 (t10/looping [1]))\n"
       "  (check :t10b (t10/threading 1))\n"
       "  (check :t11 (t11/helpers 1))\n"
       "  (check :t12 (t12/mixed 1))\n"
       "  (check :t12c t12/preloaded)\n"
       "  (println (pr-str {:base-count 21 :failures @failures})))\n"))
