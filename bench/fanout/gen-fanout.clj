#!/usr/bin/env bb
;; gen-fanout.clj — the sl1 fan-out rung generator, pure and deterministic.
;;
;;   bb bench/fanout/gen-fanout.clj --n 21 --seed 7 --out /home/forge/tmp/arms/e3/fanout
;;
;; Emits, under --out:
;;   repo-<N>/        the PRE state: 100 namespaces under src/, N of which require
;;                    acid.fanout.store and use its var find-event at exactly 3 sites
;;   canonical-<N>/   the POST state, produced by rendering the SAME data structure
;;                    with post?=true — the oracle is DERIVED, never hand-written
;;   manifest-<N>.edn targets, old/new alias per file, site count, and the sha256 of
;;                    every protected (decoy) region
;;
;; Determinism: one LCG, one seed, one traversal order.  Re-running with the same
;; --n/--seed writes byte-identical trees; bench/fanout/sabotage-FAN.sh proves it.
;;
;; Nesting: targets are the first N of ONE seeded permutation of 0..99, so the N=5
;; target set is a subset of N=10 is a subset of N=21 …  The slope is the same files
;; growing, not five unrelated tasks (docs/observations/2026-09-02-slope-spec-sl1.md).
(ns gen-fanout
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.java.io :as io])
  (:import [java.security MessageDigest]))

;; ---------------------------------------------------------------- determinism ----
(defn make-rng [seed] (atom (bit-and (long seed) 0x7FFFFFFF)))
(defn nxt! [rng]
  (swap! rng (fn [s] (bit-and (unchecked-add (unchecked-multiply s 1103515245) 12345)
                              0x7FFFFFFF))))
(defn rnd [rng n] (mod (nxt! rng) n))

(defn shuffle-det [rng coll]
  (loop [v (vec coll) i (dec (count coll))]
    (if (pos? i)
      (let [j (rnd rng (inc i))
            a (nth v i) b (nth v j)]
        (recur (assoc v i b j a) (dec i)))
      v)))

(defn sha256 [^String s]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest md (.getBytes s "UTF-8"))))))

;; ------------------------------------------------------------------- constants ---
(def total-ns 100)
(def old-lib "acid.fanout.store")
(def old-var "find-event")
(def new-lib "acid.fanout.store2")
(def new-var "fetch-event")
(def alias-policy ["store2" "st2" "es" "store-2"])
(def old-alias-pool ["store" "st" "s" "db" "repo" "k"])
(def util-libs ["acid.fanout.util-a" "acid.fanout.util-b" "acid.fanout.util-c"
                "acid.fanout.util-d"])

;; a 30-shape body bank: not trivially regular by construction
(def bodies
  ["(inc x)" "(dec x)" "(* x 2)" "(str x \"-a\")" "(when (pos? x) x)"
   "(if (even? x) x (- x))" "(let [y (inc x)] (* y y))" "(reduce + (range (max 0 x)))"
   "(mapv inc (range 3))" "(some-> x inc str)" "(cond-> x (pos? x) inc)"
   "(->> (range (max 0 x)) (filter even?) (into []))" "(keyword (str \"k\" x))"
   "(assoc {} :x x)" "(get {:a 1} :a x)" "(vec (repeat 2 x))"
   "(apply + [x 1])" "(max x 0)" "(min x 10)" "(hash-map :v x)"
   "(name (keyword (str x)))" "(count (str x))" "(first [x x])"
   "(second [x x])" "(last [x x])" "(vec (seq [x]))" "(vec (set [x]))"
   "(vec (sort [x 1]))" "(vec (distinct [x x]))" "(into #{} [x])"])

;; ------------------------------------------------------------------ the specs ----
(defn ns-name-of [i] (format "acid.fanout.ns-%03d" i))
(defn file-of [i cljc?] (format "src/acid/fanout/ns_%03d.%s" i (if cljc? "cljc" "clj")))

(defn prebinds-for
  "Bindings ALREADY present in this file, chosen so that the alias policy has to walk
   down its list.  Returns [clause-strings, blocked-aliases]."
  [pattern]
  (case (int pattern)
    0 [[] #{}]
    1 [[(format "[%s :as store2]" (nth util-libs 0))] #{"store2"}]
    2 [[(format "[%s :as store2]" (nth util-libs 0))
        (format "[%s :as st2]" (nth util-libs 1))] #{"store2" "st2"}]
    3 [[(format "[%s :as store2]" (nth util-libs 0))
        (format "[%s :as st2]" (nth util-libs 1))
        (format "[%s :refer [es]]" (nth util-libs 2))] #{"store2" "st2" "es"}]))

(defn pick-new-alias [blocked]
  (or (first (remove blocked alias-policy))
      (throw (ex-info "alias policy exhausted" {:blocked blocked}))))

(defn make-spec [rng i target-ordinal]
  (let [target? (some? target-ordinal)
        cljc?   (zero? (mod i 10))
        pattern (when target? (mod target-ordinal 4))
        [pre blocked] (if target? (prebinds-for pattern) [[] #{}])
        old-alias (when target? (nth old-alias-pool (mod target-ordinal
                                                        (count old-alias-pool))))
        extra-n (+ 1 (rnd rng 3))
        extras  (mapv (fn [k] (format "[%s :as u%d]"
                                      (nth util-libs (mod (+ i k) (count util-libs)))
                                      (inc k)))
                      (range extra-n))]
    {:idx i
     :ns-name (ns-name-of i)
     :file (file-of i cljc?)
     :cljc? cljc?
     :target? target?
     :target-ordinal target-ordinal
     :old-alias old-alias
     :new-alias (when target? (pick-new-alias blocked))
     :prebinds pre
     :extras extras
     :ns-doc? (zero? (mod i 3))
     :require-comment? (zero? (mod i 5))
     :discard? (zero? (mod i 20))
     :indent (if (even? i) 2 4)
     :require-order (rnd rng 3)               ; where the store clause sits
     :body-seed (rnd rng 30)
     :string-decoy? (or target? (zero? (mod i 4)))}))

;; ------------------------------------------------------------------ rendering ----
(defn store-clause [spec post?]
  (if post?
    (format "[%s :as %s]" new-lib (:new-alias spec))
    (format "[%s :as %s]" old-lib (:old-alias spec))))

(defn require-entries
  "Vector of {:kind :clause|:comment :text s}.  A comment is never last."
  [spec post?]
  (let [base (concat (:prebinds spec) (:extras spec))
        base (if (:target? spec)
               (let [pos (case (int (:require-order spec))
                           0 0
                           1 (quot (count base) 2)
                           2 (count base))
                     [a b] (split-at pos base)]
                 (concat a [(store-clause spec post?)]
                         [(format "[acid.fanout.other :as other]")] b))
               base)
        clauses (mapv (fn [c] {:kind :clause :text c}) base)]
    (if (and (:require-comment? spec) (> (count clauses) 1))
      (vec (concat [(first clauses)]
                   [{:kind :comment :text ";; the util aliases below are shared fleet-wide"}]
                   (rest clauses)))
      clauses)))

(defn ns-form [spec post?]
  (let [entries (require-entries spec post?)
        pad (apply str (repeat 12 \space))
        head (str "(ns " (:ns-name spec))
        doc  (when (:ns-doc? spec)
               (format "  \"Namespace %s. Historical note: find-event moved.\""
                       (:ns-name spec)))
        n (count entries)
        lines (map-indexed
                (fn [k e]
                  (let [prefix (if (zero? k) "  (:require " pad)
                        suffix (if (= k (dec n)) "))" "")]
                    (str prefix (:text e) suffix)))
                entries)]
    (str/join "\n" (remove nil? (concat [head] [doc] lines)))))

(defn ind [spec] (apply str (repeat (:indent spec) \space)))

(defn filler-defn [spec k]
  (let [i (:idx spec)
        body (nth bodies (mod (+ (:body-seed spec) k) 30))]
    (format "(defn f%d-%d\n%s[x]\n%s%s)" i k (ind spec) (ind spec) body)))

;; --- the three SITES (the only body text that differs between pre and post) ------
(defn site-forms [spec post?]
  (let [i (:idx spec)
        a (if post? (:new-alias spec) (:old-alias spec))
        v (if post? new-var old-var)
        in (ind spec)]
    [(format "(defn site-a-%d\n%s[id]\n%s(%s/%s id))" i in in a v)
     (format "(defn site-b-%d\n%s[id]\n%s(let [ev (%s/%s id)]\n%s  (assoc ev :seen true)))"
             i in in a v in)
     (format "(defn site-c-%d\n%s[ids]\n%s(mapv %s/%s ids))" i in in a v)]))

;; --- the DECOYS: byte-identical in pre and post; the sha256'd protected regions --
(defn decoy-forms [spec]
  (let [i (:idx spec) in (ind spec)]
    (cond-> [{:label "local-binding"
              :text (format "(defn local-%d\n%s[id]\n%s(let [find-event (fn [z] {:id z :kind :local})]\n%s  (find-event id)))" i in in in)}
             {:label "string-literal"
              :text (format "(def label-%d \"find-event\")" i)}
             {:label "docstring-token"
              :text (format "(defn doc-%d\n%s\"Superseded by fetch-event; the old name find-event stays in this docstring.\"\n%s[id]\n%s{:id id :kind :doc})" i in in in)}
             {:label "other-namespace"
              :text (format "(defn other-%d\n%s[id]\n%s(other/find-event id))" i in in)}
             {:label "comment-token"
              :text (format ";; historical: find-event was defined here before ns-%03d was split" i)}]
      (:discard? spec)
      (conj {:label "discard"
             :text (format "#_(defn dead-%d\n%s[id]\n%s(find-event id))" i in in)})
      (:cljc? spec)
      (conj {:label "reader-conditional"
             :text (format "(defn platform-%d\n%s[x]\n%s#?(:clj (str \"jvm-\" x \"-find-event\")\n%s   :cljs (str \"js-\" x \"-find-event\")))" i in in in)}))))

(defn render-file [spec post?]
  (let [forms (concat [(ns-form spec post?)]
                      (when (:target? spec) (map :text (decoy-forms spec)))
                      (when (:target? spec) (site-forms spec post?))
                      (map #(filler-defn spec %) (range 3))
                      (when-not (:target? spec)
                        (cond-> []
                          (:string-decoy? spec)
                          (conj (format "(def note-%d \"find-event is not used here\")" (:idx spec)))
                          (:cljc? spec)
                          (conj (format "(defn platform-%d\n%s[x]\n%s#?(:clj (str \"jvm-\" x)\n%s   :cljs (str \"js-\" x)))"
                                        (:idx spec) (ind spec) (ind spec) (ind spec))))))]
    (str (str/join "\n\n" forms) "\n")))

;; ------------------------------------------------------------- library sources ---
(def lib-files
  {"libsrc/acid/fanout/store.clj"
   "(ns acid.fanout.store)\n\n(defn find-event\n  \"The retiring lookup.\"\n  [id]\n  {:id id :kind :event})\n"
   "libsrc/acid/fanout/store2.clj"
   "(ns acid.fanout.store2)\n\n(defn fetch-event\n  \"The replacement lookup; same contract as store/find-event.\"\n  [id]\n  {:id id :kind :event})\n"
   "libsrc/acid/fanout/other.clj"
   "(ns acid.fanout.other)\n\n(defn find-event\n  \"A DIFFERENT namespace that also exports find-event. Never migrate this one.\"\n  [id]\n  {:id id :kind :other})\n"
   "libsrc/acid/fanout/util_a.clj" "(ns acid.fanout.util-a)\n\n(defn tag [x] (str \"a-\" x))\n"
   "libsrc/acid/fanout/util_b.clj" "(ns acid.fanout.util-b)\n\n(defn tag [x] (str \"b-\" x))\n"
   "libsrc/acid/fanout/util_c.clj" "(ns acid.fanout.util-c)\n\n(def es :util-c-es)\n\n(defn tag [x] (str \"c-\" x))\n"
   "libsrc/acid/fanout/util_d.clj" "(ns acid.fanout.util-d)\n\n(defn tag [x] (str \"d-\" x))\n"})

(defn test-file [specs]
  (let [targets (filter :target? specs)]
    (str
      "(ns acid.fanout.fan-test\n  (:require [clojure.test :refer [deftest is]]\n"
      (str/join "\n" (map #(format "            [%s :as t%d]" (:ns-name %) (:idx %)) targets))
      "))\n\n"
      (str/join "\n"
        (map (fn [s]
               (let [i (:idx s)]
                 (format (str "(deftest ns-%03d-test\n"
                              "  (is (= {:id 1 :kind :event} (t%d/site-a-%d 1)))\n"
                              "  (is (= true (:seen (t%d/site-b-%d 2)))) \n"
                              "  (is (= 3 (count (t%d/site-c-%d [1 2 3]))))\n"
                              "  (is (= {:id 4 :kind :local} (t%d/local-%d 4)))\n"
                              "  (is (= \"find-event\" t%d/label-%d))\n"
                              "  (is (= {:id 5 :kind :doc} (t%d/doc-%d 5)))\n"
                              "  (is (= {:id 6 :kind :other} (t%d/other-%d 6))))")
                         i i i i i i i i i i i i i i i)))
             targets))
      "\n")))

(defn load-all-file [specs]
  (str "(require '[clojure.string :as str])\n"
       "(def all '[" (str/join " " (map :ns-name specs)) "])\n"
       "(doseq [n all] (require n))\n"
       "(println (str \"LOAD-OK namespaces=\" (count all)))\n"))

(defn run-tests-file []
  (str "(require '[clojure.test :as t] 'acid.fanout.fan-test)\n"
       "(let [r (t/run-tests 'acid.fanout.fan-test)]\n"
       "  (println (str \"FAN-TEST tests=\" (:test r) \" assertions=\" (+ (:pass r) (:fail r) (:error r))\n"
       "                \" failures=\" (:fail r) \" errors=\" (:error r)))\n"
       "  (System/exit (if (pos? (+ (:fail r) (:error r))) 1 0)))\n"))

(def bb-edn "{:paths [\"src\" \"libsrc\" \"test\"]}\n")
(def deps-edn "{:paths [\"src\" \"libsrc\" \"test\"]\n :deps {}}\n")
(def fan-test-sh "#!/usr/bin/env bash\nset -euo pipefail\ncd \"$(dirname \"$0\")/..\"\nexec bb test/run_fan_tests.clj\n")

;; ------------------------------------------------------------------- emission ----
(defn spit! [root rel content]
  (let [f (io/file root rel)]
    (io/make-parents f)
    (spit f content)))

(defn emit-tree! [root specs post?]
  (doseq [[rel c] lib-files] (spit! root rel c))
  (doseq [s specs] (spit! root (:file s) (render-file s post?)))
  (spit! root "test/acid/fanout/fan_test.clj" (test-file specs))
  (spit! root "test/load_all.clj" (load-all-file specs))
  (spit! root "test/run_fan_tests.clj" (run-tests-file))
  (spit! root "bb.edn" bb-edn)
  (spit! root "deps.edn" deps-edn)
  (spit! root "bin/fan-test" fan-test-sh)
  (.setExecutable (io/file root "bin/fan-test") true false))

(defn manifest [n seed specs]
  {:n n :seed seed :total-namespaces total-ns
   :old {:lib old-lib :var old-var}
   :new {:lib new-lib :var new-var}
   :alias-policy alias-policy
   :targets (vec (for [s (filter :target? specs)]
                   {:file (:file s) :ns (:ns-name s)
                    :old-alias (:old-alias s) :new-alias (:new-alias s)
                    :sites 3
                    :protected (vec (for [d (decoy-forms s)]
                                      {:label (:label d)
                                       :sha256 (sha256 (:text d))
                                       :text (:text d)}))}))
   :non-targets (vec (map :file (remove :target? specs)))})

(defn build [n seed]
  (let [rng (make-rng seed)
        perm (shuffle-det rng (range total-ns))
        target-set (into {} (map-indexed (fn [ord i] [i ord]) (take n perm)))
        rng2 (make-rng (+ seed 991))]
    (mapv (fn [i] (make-spec rng2 i (get target-set i))) (range total-ns))))

(defn -main [& args]
  (let [m (apply hash-map args)
        n (Integer/parseInt (get m "--n" "21"))
        seed (Integer/parseInt (get m "--seed" "7"))
        out (get m "--out" "/home/forge/tmp/arms/e3/fanout")
        specs (build n seed)
        repo (str out "/repo-" n)
        canon (str out "/canonical-" n)]
    (doseq [d [repo canon]]
      (when (.exists (io/file d))
        (throw (ex-info (str "refusing to overwrite " d " — remove it first") {}))))
    (emit-tree! repo specs false)
    (emit-tree! canon specs true)
    (spit (str out "/manifest-" n ".edn")
          (with-out-str (pp/pprint (manifest n seed specs))))
    (println (format "gen-fanout: n=%d seed=%d namespaces=%d targets=%d out=%s"
                     n seed total-ns (count (filter :target? specs)) out))
    (println (format "gen-fanout: alias histogram %s"
                     (pr-str (frequencies (keep :new-alias specs)))))))

(apply -main *command-line-args*)
