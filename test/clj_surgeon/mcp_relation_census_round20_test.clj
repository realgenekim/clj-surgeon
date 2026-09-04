(ns clj-surgeon.mcp-relation-census-round20-test
  "Round-twenty PARITY witnesses for the relation-census lane.

   A namespace of its own, and the reason is a gate rather than taste: the
   trunk's `default-ceilings-admit-every-source-in-this-repository` requires
   the shipped parse-node ceiling to keep a 4x margin over the largest source
   in this repository, and `mcp_relation_census_test.clj` was ALREADY over
   that line at 563c300d — 50,214 nodes against a 50,000 budget — before round
   twenty added anything to it. The subprocess-driven launcher witnesses moved
   to `mcp-relation-census-launcher-test` for the same reason; what is left
   here is the in-process cross-entrance comparison, which is where round
   twenty's second blocking finding is decided."
  (:require
   [clj-surgeon.core :as core]
   [clj-surgeon.mcp-relation-census :as census-tool]
   [clj-surgeon.relation-census :as census]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

;; ---------------------------------------------------------------------------
;; ROUND TWENTY, item 2 — the PARITY half of Opus's round-nineteen BLOCKING
;; finding.
;;
;; The reviewer's receipt, at 563c300d, for one request spelled the same way
;; at both entrances:
;;
;;   CLI  :dir <nonexistent> :file <escaping link>
;;          {:ok true, :files-scanned 1, :read-complete true, :arms 1}
;;   TOOL {:project-root <nonexistent>} {:files [<escaping link>]}
;;          {:ok false, :error_type "invalid-workspace-root"}
;;
;; That is the defect class this lane has spent six rounds closing, in its
;; worst form: not two entrances naming one observation differently, but one
;; entrance REFUSING what the other READS.
;;
;; The round-nineteen parity enumeration cannot see it. Every row it drives
;; gives the CLI a `:file` and no `:dir`, and gives the tool a project-root
;; that exists, so no row asks either entrance what it does when the workspace
;; does not resolve. This enumeration asks exactly that, prints its own
;; derivation the way the round-nineteen one does, and asserts the
;; disagreeing set is empty — it is a second table rather than two more rows
;; in the first because the first table's drive shape is fixed (`:file` alone
;; against an existing root) and these rows are about the ROOT.
;; ---------------------------------------------------------------------------

(defn- workspace-parity-fixture!
  "A workspace with an escaping link, an arm inside, and a secret outside."
  [^java.io.File parent]
  (let [ws (io/file parent "ws")
        outside (io/file parent "outside")
        arm "(ns app.arm)\n(defmethod fold-event :arm [state event] state)\n"]
    (.mkdirs (io/file ws "src/app"))
    (.mkdirs outside)
    (spit (io/file ws "src/app/arm.clj") arm)
    (spit (io/file outside "secret.clj") arm)
    (Files/createSymbolicLink
      (.toPath (io/file ws "src/app/escape.clj"))
      (.toPath (io/file "../../../outside/secret.clj"))
      (make-array FileAttribute 0))
    {:ws ws :outside outside}))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-018
(deftest the-two-entrances-refuse-an-unresolvable-workspace-alike
  (let [parent (.toFile (Files/createTempDirectory
                          "census20-parity" (make-array FileAttribute 0)))]
    (try
      (let [{:keys [ws outside]} (workspace-parity-fixture! parent)
            named (.getCanonicalPath ^java.io.File ws)
            missing (str (.getCanonicalPath parent) "/does-not-exist")
            expected "invalid-workspace-root"
            rows [{:shape :unresolvable-root-escaping-link
                   :relative "src/app/escape.clj"
                   :absolute (str named "/src/app/escape.clj")}
                  {:shape :unresolvable-root-outside-absolute
                   :relative "src/app/arm.clj"
                   :absolute (str (.getCanonicalPath ^java.io.File outside)
                                  "/secret.clj")}]
            observed
            (doall
              (for [{:keys [shape relative absolute]} rows]
                (let [tool (census-tool/execute-request!
                             {:project-root missing} {:files [relative]})
                      cli (core/run-relation-census
                            {:dir missing :file absolute})]
                  {:shape shape
                   :tool (:error_type tool)
                   :cli (some-> (:error-type cli) name)
                   :cli-ok (:ok cli)
                   :cli-scanned (:files-scanned cli 0)})))]

        (println "WORKSPACE-PARITY-ENUMERATION:")
        (doseq [{:keys [shape tool cli cli-scanned]} observed]
          (println (format "  %-36s expected %-24s tool %-24s cli %-24s scanned %d  agree %s"
                           (name shape) expected (pr-str tool) (pr-str cli)
                           cli-scanned (= expected tool cli))))

        (testing "the enumeration this witness printed is the one it compared"
          (is (= (set (map :shape rows)) (set (map :shape observed))))
          (is (= 2 (count observed))
              (str "the enumeration is " (count observed) " shapes, not two")))

        (testing "EVERY shape in the printed enumeration agrees"
          (let [disagreeing (into (sorted-set)
                                  (comp (remove (fn [{:keys [tool cli]}]
                                                  (= expected tool cli)))
                                        (map :shape))
                                  observed)]
            (is (= #{} disagreeing)
                (str "these shapes do not get one refusal from both entrances: "
                     (pr-str disagreeing) " — full enumeration: "
                     (pr-str (vec observed))))))

        (doseq [{:keys [shape cli-ok cli-scanned]} observed]
          (testing (str shape " reads NOTHING at the CLI")
            (is (false? cli-ok)
                (str shape ": the CLI published a receipt over a tree the "
                     "request never named"))
            (is (zero? cli-scanned)
                (str shape ": the CLI scanned " cli-scanned
                     " file(s) before refusing"))))

        (testing "the CLI's name for it is DECLARED"
          (is (contains? census/cli-refusal-types :invalid-workspace-root)
              "an undeclared refusal is one no enumeration witness can drive")))
      (finally
        (doseq [f (reverse (file-seq parent))] (.delete ^java.io.File f))))))

;; ---------------------------------------------------------------------------
;; ROUND TWENTY-TWO, items 3 and 5 — Opus's round-twenty-one findings.
;;
;; The round-twenty table above asks what the two entrances do when the root
;; does NOT resolve. It cannot see what they do when the root resolves to
;; something that is not a tree, because its drive shape is fixed. The
;; reviewer ran that shape and the entrances disagreed:
;;
;;   SHAPE          TOOL_ERROR_TYPE          CLI_ERROR_TYPE        AGREE
;;   dir-is-a-file  "invalid-workspace-root" "no-fold-arms-found"  false
;;   dotdot         "no-fold-arms-found"     "no-fold-arms-found"  true
;;   dot            "no-fold-arms-found"     "no-fold-arms-found"  true
;;   double-slash   "no-fold-arms-found"     "no-fold-arms-found"  true
;;   trailing-slash "no-fold-arms-found"     "no-fold-arms-found"  true
;;   plain          "no-fold-arms-found"     "no-fold-arms-found"  true
;;
;; The CLI's answer for `dir-is-a-file` is not merely a different NAME, it is
;; a false description: the caller named a FILE and the receipt says the TREE
;; defines no fold arms, with `:files-scanned 0` — the shape of a completeness
;; claim over a tree that was never a tree. `census-workspace` reached
;; `.toRealPath`, which succeeds on a regular file, so "is there a tree here
;; at all" was never asked. The same launcher answers the same argument
;; `:workspace-root-not-a-directory` under `:ls-tree`, so the CLI was
;; inconsistent with ITSELF, one op over.
;;
;; The four path forms AGREE today and are enumerated here anyway. Agreeing
;; without a witness is exactly the state round nineteen described: it is not
;; a property of the tool, it is a fact about this afternoon.
;; ---------------------------------------------------------------------------

(defn- root-shape-fixture!
  "A workspace that resolves and holds NO fold arms, plus a regular file in
   it. No arms, deliberately: the interesting answer is about the ROOT, and a
   tree with arms would return a receipt that says nothing about root shape."
  [^java.io.File parent]
  (let [ws (io/file parent "ws")]
    (.mkdirs (io/file ws "src/app"))
    (spit (io/file ws "src/app/plain.clj") "(ns app.plain)\n(def x 1)\n")
    (spit (io/file ws "deps.edn") "{:paths [\"src\"]}\n")
    ws))

;; @spec MCP-OP-CENSUS-018
;; @spec MCP-OP-CENSUS-035
(deftest the-two-entrances-name-one-observation-for-every-root-shape
  (let [parent (.toFile (Files/createTempDirectory
                          "census22-root-shapes" (make-array FileAttribute 0)))]
    (try
      (let [ws (root-shape-fixture! parent)
            base (.getCanonicalPath ^java.io.File ws)
            rows [{:shape :plain
                   :root base
                   :expected "no-fold-arms-found"}
                  {:shape :trailing-slash
                   :root (str base "/")
                   :expected "no-fold-arms-found"}
                  {:shape :double-slash
                   :root (str base "//")
                   :expected "no-fold-arms-found"}
                  {:shape :dot
                   :root (str base "/.")
                   :expected "no-fold-arms-found"}
                  {:shape :dotdot
                   :root (str base "/src/..")
                   :expected "no-fold-arms-found"}
                  ;; The row the round-twenty table's fixed drive shape cannot
                  ;; see, and the one that diverged.
                  {:shape :dir-is-a-file
                   :root (str base "/deps.edn")
                   :expected "invalid-workspace-root"}]
            observed
            (doall
              (for [{:keys [shape root expected]} rows]
                (let [tool (census-tool/execute-request! {:project-root root} {})
                      cli (core/run-relation-census {:dir root})]
                  {:shape shape
                   :expected expected
                   :tool (:error_type tool)
                   :cli (some-> (:error-type cli) name)
                   :cli-ok (:ok cli)
                   :cli-scanned (:files-scanned cli 0)})))]

        (println "ROOT-SHAPE-PARITY-ENUMERATION:")
        (doseq [{:keys [shape expected tool cli cli-scanned]} observed]
          (println (format "  %-16s expected %-24s tool %-24s cli %-24s scanned %d  agree %s"
                           (name shape) expected (pr-str tool) (pr-str cli)
                           cli-scanned (= expected tool cli))))

        (testing "the enumeration this witness printed is the one it compared"
          (is (= (set (map :shape rows)) (set (map :shape observed))))
          (is (= 6 (count observed))
              (str "the enumeration is " (count observed) " shapes, not six")))

        (testing "EVERY shape in the printed enumeration agrees"
          (let [disagreeing (into (sorted-set)
                                  (comp (remove (fn [{:keys [expected tool cli]}]
                                                  (= expected tool cli)))
                                        (map :shape))
                                  observed)]
            (is (= #{} disagreeing)
                (str "these shapes do not get one answer from both entrances: "
                     (pr-str disagreeing) " — full enumeration: "
                     (pr-str (vec observed))))))

        (testing "a :dir that is a FILE is refused, not described as an empty tree"
          (let [row (first (filter #(= :dir-is-a-file (:shape %)) observed))]
            (is (= "invalid-workspace-root" (:cli row))
                (str "the CLI answered " (pr-str (:cli row))
                     " about a regular file — a completeness-shaped receipt "
                     "over a non-tree"))
            (is (false? (:cli-ok row)))))

        (testing "the CLI answers its own :ls-tree op consistently"
          ;; The same launcher, the same argument, a different op: `:ls-tree`
          ;; has refused a non-directory root since MCP-OP-SHELL-ARGV-002.
          ;; What must not happen is the two ops disagreeing about whether a
          ;; regular file is a workspace.
          (let [refusal (core/ls-tree-root-refusal (str (.getCanonicalPath ^java.io.File ws)
                                                        "/deps.edn"))]
            (is (some? refusal)
                ":ls-tree accepted a regular file as its root"))))
      (finally
        (doseq [f (reverse (file-seq parent))] (.delete ^java.io.File f))))))

;; ---------------------------------------------------------------------------
;; ROUND TWENTY-TWO, item 2 — Opus's round-twenty-one finding about the
;; EVIDENCE rather than about the code.
;;
;; Round twenty offered the receipt "bound -> string?-only 118/28" as proof
;; that `bound-refusal-leaf`'s non-string branch (8c93b878) closes round
;; nineteen's blocking finding. It does not reproduce. Re-driven on a
;; `git archive` export at this branch's tip:
;;
;;   SABOTAGE 1  (leaf bound reverted to `(if (string? x) …)`)
;;     :BATTERY-RESULT {:test 26, :pass 1312, :fail 0, :error 0}
;;   SABOTAGE 1b (WHOLE-FIELD bound removed entirely)
;;     :BATTERY-RESULT {:test 26, :pass 1312, :fail 0, :error 0}
;;
;; So the reviewer's ruling is right and is worse than they measured: NEITHER
;; bound was pinned. Every committed drive carries exactly ONE over-long leaf,
;; and either bound alone catches that shape, so each was masking the other
;; and a sabotage of either was invisible. In the reviewer's words: "A bound
;; nothing can make red is not a ratchet."
;;
;; Two repairs, one per bound:
;;
;;   - the WHOLE-FIELD bound gets a launcher drive of its own,
;;     `:many-small-leaves-at-the-ops-exit` — ten thousand two-character
;;     keywords, every leaf tiny and the field thirty thousand characters,
;;     which is the exact case `bound-refusal`'s docstring was written for.
;;   - the LEAF bound gets the witness below, at the FUNCTION boundary rather
;;     than at a launcher, and the reason is stated rather than hidden: the
;;     three identity fields are exempt from the whole-field bound, so an
;;     over-ceiling NON-STRING leaf inside one of them is the only input the
;;     leaf bound alone can save — and no argv builds one today, because
;;     `:anchor`'s `:given` and `:absolute` are `(str …)` and its `:kind` is
;;     one of two short keywords. The honest statement is therefore that the
;;     non-string leaf branch is DEFENCE IN DEPTH with no launcher-reachable
;;     input at this tip, and the honest ratchet is one that drives the
;;     function with the input the rule is about.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
(deftest the-leaf-bound-is-what-saves-an-exempt-fields-non-string-leaf
  (let [big (apply str (repeat 10001 \a))
        ceiling census/max-refusal-field-chars
        printed-len #(count (pr-str %))]

    (testing "an over-ceiling NON-STRING leaf inside an exempt field is bounded"
      (doseq [k census/refusal-identity-keys]
        (let [refusal {:ok false
                       :error-type :invalid-workspace-root
                       k {:kind (keyword big) :given "/x" :absolute "/x"}}
              bounded (census/bound-refusal refusal)
              leaf (:kind (get bounded k))]
          (is (<= (printed-len leaf) (+ ceiling 64))
              (str k " published a leaf that RENDERS as " (printed-len leaf)
                   " characters. It is EXEMPT from the whole-field bound by "
                   "`refusal-identity-keys`, so the leaf bound is the only "
                   "thing between it and the caller's terminal — and a leaf "
                   "bound that asks `string?` is a bound on the type its "
                   "author happened to picture"))
          (is (map? (get bounded k))
              (str k " was collapsed into prose: the exemption exists so a "
                   "structured field a caller reads BY KEY survives")))))

    (testing "the whole-field bound is a SECOND bound, not the same one"
      ;; Many tiny leaves: every leaf is under the ceiling, the field is not.
      ;; This is the shape the launcher drive `:many-small-leaves-at-the-ops-
      ;; exit` drives through both real launchers.
      (let [wide (vec (repeat 10000 :a))
            bounded (census/bound-refusal {:ok false :value wide})]
        (is (<= (printed-len (:value bounded)) (+ ceiling 64))
            (str ":value rendered as " (printed-len (:value bounded))
                 " characters with every leaf two characters long — the "
                 "NUMBER of leaves is caller-controlled too"))))))
