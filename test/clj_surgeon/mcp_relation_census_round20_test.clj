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
