(ns split-closure-probe-test (:require [babashka.fs :as fs] [clj-surgeon.analyze :as a] [clojure.edn :as edn]
            [clojure.string :as str] [clojure.test :as t] [clojure.java.io :as io]))
(def repo (fs/parent (fs/parent (fs/path (io/resource "split_closure_probe_test.clj")))))
(when-not (find-ns 'split-closure-probe) (load-file (str (fs/path repo "bin/split-closure-probe"))))
(def sample (a/string->zloc "(ns demo) (def leaf 1) (defn- middle [] leaf) (defn top [leaf] [leaf (middle)])"))
(alias 'p 'split-closure-probe) (def request {:source {:lib "demo" :file "src/demo.clj"} :roots ["src" "test"]
              :destinations [{:lib "demo.a" :file "src/a.clj" :forms ["leaf" "middle"]}
                             {:lib "demo.b" :file "src/b.clj" :forms ["top"]}]})
(t/deftest literal-partition (let [out (p/probe request sample {} #{"src/a.clj"})]
    (doseq [owner ["leaf" "middle" "top"]] (t/is (str/includes? out (str "owner " owner " exactly-once"))))
    (t/is (every? #(str/includes? out %) ["unmapped=[]" "double-mapped=[]" "non-owners=1"
                                       "exists=true" "exists=false" "middle -> leaf" "top -> middle"]))
    (t/is (str/includes? (p/probe (assoc-in request [:destinations 0 :forms] ["absent"]) sample {} #{})
            "unknown=[\"absent\"]"))
    (t/is (not (str/includes? out "top -> leaf")))
    (t/is (str/ends-with? out "destinations=2 absent=1 sites=0 callers=0 edges=2"))))
(t/deftest broken-partition
  (let [bad (assoc-in request [:destinations 1 :forms] ["leaf" "leaf"]) out (p/probe bad sample {} #{})]
    (t/is (every? #(str/includes? out %) ["owner leaf more-than-once" "owner top not-at-all"
                                          "unmapped=[\"top\"]" "double-mapped=[\"leaf\"]"]))
    (t/is (str/includes? out "owners=3 distinct=3 mapped=1 unmapped=1 double=1"))))
(t/deftest static-occurrences
  (let [caller (a/string->zloc (str "#_{} (ns caller (:require [demo :as d] [elsewhere :as other]))"
  "(d/leaf) (d/leaf) demo/top #'d/middle :d/leaf \"d/leaf\" 'd/leaf (quote d/leaf) other/leaf d/missing ; d/leaf\n"))]
    (t/is (= 4 (p/sites "demo" #{"leaf" "middle" "top"} caller)))
    (t/is (= 0 (p/sites "unrelated" #{"leaf"} caller) (p/sites "demo" #{} caller)))
    (t/is (str/includes? (p/probe request sample {"test/c.clj" caller} #{}) "caller test/c.clj sites=4"))))
(t/deftest dependency-boundaries
  (doseq [[body edge?] [["(let [leaf 2] leaf)" false] ["'leaf" false] ["(fn [leaf] leaf)" false]
                      ["(let [x leaf] x)" true] ["(let [leaf 2] (fn [] leaf))" false] ["[leaf leaf]" true]]]
    (let [source (a/string->zloc (str "(ns demo) (def leaf 1) (defn middle [] " body ")"))]
      (t/is (= edge? (str/includes? (p/probe request source {} #{}) "middle -> leaf"))))))
(t/deftest frozen-cell-c-boundary (let [base (str (fs/path repo "test-fixtures/namespace-split/cellc-d9205abc") "/")
        receipt (edn/read-string (slurp (str base "sentinel-20261025-split-receipt.edn")))
        out (p/run-probe (str base "D1-request.edn") (str base "source"))]
    (t/is (every? #(str/includes? out %) ["missing-root=test" "caller src/cfp_scheduler_killer/server.clj sites=62"
                                          "owners=141 distinct=141 mapped=141 unmapped=0 double=0"]))
    (t/is (str/ends-with? out "destinations=20 absent=20 sites=62 callers=1 edges=226"))
    (t/is (= {:destinations 20 :forms 141 :caller_files 5 :caller_sites 87 :files 26} (:counts receipt)))
    (t/is (not (fs/exists? (fs/path base "source/test"))))))
(t/deftest def-family-only (let [literal "(ns demo) (declare pending) (comment (def hidden 1)) (println leaf)"
        defs "(def leaf 1) (defn n [] 1) (defn- p [] 1) (defmulti m identity) (defmethod m :x [_] 1)"
        more "(defmacro mac [] 1) (defonce once 1) (defrecord R []) (defprotocol P) (deftype T [])"
        out (p/probe request (a/string->zloc (str literal defs more)) {} #{})]
    (t/is (= ["P" "R" "T" "leaf" "m" "mac" "n" "once" "p"] (mapv second (re-seq #"(?m)^owner (\S+) " out))))
    (t/is (every? #(str/includes? out %) ["owners=10 distinct=9 mapped=1" "non-owners=4"]))))
(t/deftest pr88-referred-defaults-and-renames
  (doseq [refer ["[leaf]" ":all"] rename ["" " :rename {leaf renamed}"]
          [body expected] [["(leaf) (leaf)" 2] ["(let [leaf leaf] (leaf)) (leaf)" 2]
          ["(loop [leaf 1] leaf) (fn [leaf] leaf) (defn f [leaf] leaf)" 0]
          ["(fn f ([leaf] leaf) ([] (leaf)))" 1] ["(top)" (if (= refer ":all") 1 0)]
          ["(let [{:keys [leaf]} {}] (leaf)) (fn [[leaf]] leaf)" 0]
          ["#_(leaf) '(leaf) (quote leaf)" 0] ["(fn leaf [] (leaf))" 0]
          ["(let [x (leaf) leaf x] (leaf))" 1] ["(leaf)" 1] ["(let [leaf 0] leaf)" 0]
          ["(let [{:keys [x] :or {x (leaf)}} {}] x)" 1] ["(fn [{:keys [leaf] :or {leaf (leaf)}}] leaf)" 1]
          ["(defn f ([{:keys [x] :or {x (leaf)}}] x) ([] (leaf)))" 2] ["(fn [[{:keys [x] :or {x (leaf)}}]] x)" 1]
          ["(let [leaf identity] (fn [{:keys [x] :or {x (leaf)}}] x))" 0]]]
    (t/is (= expected (p/sites "demo" #{"leaf" "top"}
      (a/string->zloc (str "(ns caller (:require [demo :refer " refer rename "]))"
                          (if (empty? rename) body (str/replace body "leaf" "renamed")))))))))
