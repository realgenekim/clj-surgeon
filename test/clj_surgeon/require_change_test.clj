(ns clj-surgeon.require-change-test
  {:lane :fast}
  (:require
   [clj-surgeon.mcp-extraction :as kernel]
   [clj-surgeon.mcp-extraction-test :as memory]
   [clj-surgeon.require-change :as change]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.parser :as parser]))

(def names ["bridge3_new" "capture_archive" "channel" "codex_app_server"
            "director_control" "director_outbox" "groq" "reducer_session"])
(def files (mapv #(str "src/marvin_voice_remote/" % ".clj") names))
(def sources (into {} (map (fn [f] [f (slurp (io/file "test-fixtures/require-change/seed" (.getName (io/file f))))]) files)))
(def request {:op "require_change" :workspace_root "/project"
              :add {:lib "marvin-voice-remote.json" :alias_policy ["json" "mjson" "m-json" "json-policy"]}
              :files (mapv #(hash-map :file %) files)
              :expect {:files 8 :adds 8 :removes 0}
              :verification {:profile "row3"}})
(def expressible-files (filterv #(not (str/ends-with? % "codex_app_server.clj")) files))
(def expressible-request (assoc request :files (mapv #(hash-map :file %) expressible-files)
                           :expect {:files 7 :adds 7 :removes 0}))

;; @spec REQUIRE-CHANGE-005
;; @spec REQUIRE-CHANGE-008
(deftest natural-sorted-append-cannot-satisfy-frozen-zero-churn
  ;; Sol O5 requires append, but O4 forbids moving the inherited closer.
  (let [r (change/compile-change request sources)]
    (is (= "unsupported-layout" (:error_type r)))
    (is (= "src/marvin_voice_remote/codex_app_server.clj" (get-in r [:evidence :file])))
    (is (false? (:ok r))) (is (:source_unchanged r))
    (is (empty? (:future-sources r)))))

(defn one [source & [overrides]]
  (change/compile-change
    (merge request {:add {:lib "m.target" :alias_policy ["json" "mjson"]}
                    :files [{:file "src/a.clj"}] :expect {:files 1 :adds 1 :removes 0}} overrides)
    {"src/a.clj" source}))

;; @spec REQUIRE-CHANGE-001
;; @spec REQUIRE-CHANGE-003
;; @spec REQUIRE-CHANGE-005
;; @spec REQUIRE-CHANGE-006
;; @spec REQUIRE-CHANGE-012
(deftest expressible-historical-headers-in-one-intent
  ;; Provenance: Marvin 9f9cf614; capture_archive uses d170f3d5's valid hanging
  ;; header because the golden target shares the opener (fixture README).
  (let [r (change/compile-change expressible-request sources)]
    (is (:ok r) (pr-str (dissoc r :guard-sources)))
    (is (= {:files 7 :adds 7 :removes 0} (:counts r)))
    (is (= (set expressible-files) (set (keys (:future-sources r)))))
    (is (= (repeat 7 "mjson") (map :alias (:decisions r))))
    (is (= (repeat 7 "first-free") (map :reason (:decisions r))))
    (doseq [{:keys [file collisions]} (:decisions r)]
      (is (= [{:alias "json" :lib "clojure.data.json"}] collisions))
      (let [future (get-in r [:future-sources file])
            target-lines (filter #(str/includes? % "[marvin-voice-remote.json :as mjson]") (str/split-lines future))]
        (is (= 1 (count target-lines)))
        (is (= (get sources file) (str/replace future (str (first target-lines) "\n") "")))
        (is (some? (parser/parse-string-all future)))))))

;; @spec REQUIRE-CHANGE-002
;; @spec REQUIRE-CHANGE-003
(deftest alias-order-reuse-and-no-op
  (let [r (one "(ns a (:require [m.target :as mjson]\n                 [z :as json]))\n"
               {:expect {:files 0 :adds 0 :removes 0}})]
    (is (:ok r)) (is (= "reuse-existing" (get-in r [:decisions 0 :reason])))
    (is (= "mjson" (get-in r [:decisions 0 :alias])))
    (is (= {} (:future-sources r))))
  (let [r (one "(ns a\n  (:require\n   [z :as z]))\n")]
    (is (:ok r)) (is (= "json" (get-in r [:decisions 0 :alias])))))

;; @spec REQUIRE-CHANGE-004
;; @spec REQUIRE-CHANGE-008
;; @spec REQUIRE-CHANGE-009
;; @spec REQUIRE-CHANGE-010
(deftest typed-refusal-matrix
  (doseq [[label source overrides error]
          [["exhaustion" "(ns a (:require [a :as json]\n                 [b :as mjson]))" {} "alias-policy-exhausted"]
           ["duplicate target" "(ns a (:require [m.target :as json]\n                 [m.target :as mjson]))" {} "duplicate-target"]
           ["duplicate alias" "(ns a (:require [a :as json]\n                 [b :as json]))" {} "ambiguous-alias"]
           ["two clauses" "(ns a (:require [a :as a]) (:require [b :as b]))" {} "ambiguous-require"]
           ["conditional" "(ns a (:require #?(:clj [a :as a])))" {} "unsupported-require"]
           ["refer" "(ns a (:require [a :refer [x]]))" {} "unsupported-require"]
           ["missing" "(ns a)" {} "ambiguous-require"]
           ["target wrong alias" "(ns a (:require [m.target :as other]))" {} "target-alias-outside-policy"]
           ["count" "(ns a\n (:require\n  [z :as z]))" {:expect {:files 8 :adds 8 :removes 0}} "expect-mismatch"]
           ["hash" "(ns a\n (:require\n  [z :as z]))" {:files [{:file "src/a.clj" :source_hash (apply str (repeat 64 "0"))}]} "source-hash-mismatch"]]]
    (testing label
      (let [r (one source overrides)]
        (is (false? (:ok r))) (is (= error (:error_type r)) (pr-str r))
        (is (empty? (:future-sources r))) (is (:source_unchanged r)))))
  (let [r (one "(ns a (:require [a :as json]\n                 [b :as mjson]))")]
    (is (= "src/a.clj" (get-in r [:evidence :file])))
    (is (= [{:alias "json" :lib "a"} {:alias "mjson" :lib "b"}]
           (get-in r [:evidence :bound_aliases])))))

;; @spec REQUIRE-CHANGE-005
;; @spec REQUIRE-CHANGE-006
(deftest exact-layout-matrix
  (doseq [[source expected]
          [["(ns a\n  (:require\n   ;; attached\n   [z :as z]))\n"
            "(ns a\n  (:require\n   [m.target :as json]\n   ;; attached\n   [z :as z]))\n"]
           ["(ns a (:require [a :as a] ; trailing stays\n                 ;; group\n                 [z :as z]))\n"
            "(ns a (:require [a :as a] ; trailing stays\n                 [m.target :as json]\n                 ;; group\n                 [z :as z]))\n"]
           ["(ns a\n (:require\n  [z :as z]\n  [a :as a]))\n"
            "(ns a\n (:require\n  [m.target :as json]\n  [z :as z]\n  [a :as a]))\n"]
           ["(ns a\r\n (:require\r\n  [a :as a]\r\n  [z :as z]))\r\n"
            "(ns a\r\n (:require\r\n  [a :as a]\r\n  [m.target :as json]\r\n  [z :as z]))\r\n"]
           ["(ns a\n (:require\n  [a :as a]\n ))\n"
            "(ns a\n (:require\n  [a :as a]\n  [m.target :as json]\n ))\n"]]]
    (let [r (one source)]
      (is (:ok r) (pr-str r))
      (is (= expected (get-in r [:future-sources "src/a.clj"]))))))

;; @spec REQUIRE-CHANGE-007
(deftest removal-owns-one-entire-line
  (let [r (one "(ns a\n (:require\n  [a :as a]\n  [z :as z]))\n"
               {:files [{:file "src/a.clj" :remove {:lib "a" :as "a"}}]
                :expect {:files 1 :adds 1 :removes 1}})]
    (is (:ok r))
    (is (= "(ns a\n (:require\n  [m.target :as json]\n  [z :as z]))\n" (get-in r [:future-sources "src/a.clj"]))))
  (doseq [source ["(ns a\n (:require\n  ;; a comment\n  [a :as a]\n  [z :as z]))"
                  "(ns a (:require [a :as a]\n                 [z :as z]))"
                  "(ns a\n (:require\n  [a :as a] ; keep me\n  [z :as z]))"]]
    (is (= "unsupported-removal"
           (:error_type (one source {:files [{:file "src/a.clj" :remove {:lib "a" :as "a"}}]
                                     :expect {:files 1 :adds 1 :removes 1}}))))))

;; @spec REQUIRE-CHANGE-011
;; @spec REQUIRE-CHANGE-012
(deftest one-transaction-and-independent-inverse-replay
  (let [c (change/compile-change expressible-request sources)]
    (doseq [failure [nil 1 4 7]]
      (let [memory (memory/memory-io sources failure)
            r (kernel/commit! c memory)]
        (if failure
          (do (is (false? (:ok r))) (is (= sources @(:state memory))))
          (do (is (:ok r) (pr-str r))
              (is (= 7 (count (get-in r [:receipt :files]))))
              ;; Replay retained originals without using the compiler's inverse.
              (is (= (select-keys sources expressible-files) (into {} (map (juxt :file :original-source)) (get-in r [:receipt :files]))))
              (is (:ok (kernel/undo! (:receipt r) memory)))
              (is (= sources @(:state memory)))))))))

;; @spec REQUIRE-CHANGE-007
(deftest singleton-removal-and-addition-own-the-same-line
  (let [r (one "(ns a\n (:require\n  [old.lib :as json]\n ))\n"
               {:files [{:file "src/a.clj" :remove {:lib "old.lib" :as "json"}}]
                :expect {:files 1 :adds 1 :removes 1}})]
    (is (:ok r))
    (is (= "(ns a\n (:require\n  [m.target :as mjson]\n ))\n" (get-in r [:future-sources "src/a.clj"])))))

;; @spec REQUIRE-CHANGE-008
(deftest frozen-whole-line-stripping-destroys-two-natural-openers
  (doseq [file ["capture_archive" "reducer_session"]]
    (let [golden (slurp (str "test-fixtures/require-change/golden/" file ".clj"))
          stripped (str/replace golden #"(?m)^.*\[marvin-voice-remote.json :as mjson\].*\n" "")
          r (one stripped)]
      (is (false? (:ok r)))
      (is (contains? #{"require-parse-failed" "ambiguous-require"} (:error_type r)))
      (is (:source_unchanged r)))))
