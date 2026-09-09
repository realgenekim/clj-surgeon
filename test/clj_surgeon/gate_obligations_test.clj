(ns ^{:lane :fast} clj-surgeon.gate-obligations-test
  "Witnesses for the obligation inventory and for toolchain identity.

   Frame 7 item 1 (section 4 of
   `docs/observations/2026-09-09-frame7-protocol-and-consumer-contract.md`).
   Each test below is one sentence of that section that a consumer can
   otherwise get wrong silently."
  (:require
   [clj-surgeon.gate-obligations :as gob]
   [clj-surgeon.toolchain-identity :as tc]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; a policy the tests own, so the assertions are about the DERIVATION and not
;; about whichever namespaces happen to be in the tree this week
;; ---------------------------------------------------------------------------

(def ^:private fake-suites
  {"mcp" '[a.one-test a.two-test b.three-test]
   "fast" '[a.one-test a.two-test]
   "bb" '[c.four-test]
   "alias" '[d.five-test]
   "battery" '[e.six-test]})

(def ^:private fake-policy
  {:stage-manifest ["admit-transaction-recovery-battery" "battery-fresh"
                    "alias-migration-test" "mcp-test" "test-bb"
                    "repository-hygiene" "intent-audit"]
   :suite-namespaces #(get fake-suites %)})

(deftest every-section-4-obligation-is-present-and-named
  (testing "R1..R10 exactly, in order, with section 4's own ids"
    (let [inv (gob/inventory fake-policy)]
      (is (= [:R1 :R2 :R3 :R4 :R5 :R6 :R7 :R8 :R9 :R10]
             (mapv :id (:obligations inv))))
      (is (= (:stage-manifest fake-policy) (:stage-manifest inv)))
      (is (string? (:policy-sha256 inv))))))

(deftest a-suite-obligation-carries-the-identities-not-only-a-count
  (testing "the whole point of the target: names, not totals"
    (let [inv (gob/inventory fake-policy)
          by-id (into {} (map (juxt :id identity)) (:obligations inv))]
      (is (= ["a.one-test" "a.two-test" "b.three-test"]
             (:selected-test-identities (:R4 by-id))))
      (is (= ["c.four-test"] (:selected-test-identities (:R5 by-id))))
      (is (= ["d.five-test"] (:selected-test-identities (:R3 by-id))))
      (is (= {:fast 2 :integration 1} (get-in (:R4 by-id) [:scope :phases]))
          "the fast-before-integration barrier is part of R4's scope"))))

(deftest the-policy-hash-moves-when-a-single-namespace-is-dropped
  (testing "a renamed same-count substitution and a silent omission both move it"
    (let [base (:policy-sha256 (gob/inventory fake-policy))
          dropped (:policy-sha256
                   (gob/inventory (assoc fake-policy :suite-namespaces
                                         #(if (= "bb" %) '[] (get fake-suites %)))))
          renamed (:policy-sha256
                   (gob/inventory (assoc fake-policy :suite-namespaces
                                         #(if (= "bb" %) '[c.four-RENAMED-test]
                                              (get fake-suites %)))))]
      (is (not= base dropped) "a dropped BB namespace must move the policy hash")
      (is (not= base renamed) "a same-count rename must move it too")
      (is (not= dropped renamed)))))

(deftest r9-and-r10-can-never-be-discharged-by-a-gate-receipt
  (testing "acceptance/review and publication freshness are out of band"
    (let [by-id (into {} (map (juxt :id identity)) (:obligations (gob/inventory fake-policy)))]
      (is (= :out-of-band (get-in (:R9 by-id) [:discharge :by])))
      (is (= :out-of-band (get-in (:R10 by-id) [:discharge :by])))
      (is (nil? (:stage (:R9 by-id))))
      (is (nil? (:stage (:R10 by-id))))))
  (testing "and R8 is the consumer's own derivation, not an executed stage"
    (let [by-id (into {} (map (juxt :id identity)) (:obligations (gob/inventory fake-policy)))]
      (is (= :consumer-derivation (get-in (:R8 by-id) [:discharge :by]))))))

(deftest the-inventory-is-readable-by-a-real-edn-reader
  (testing "section 5: an actual EDN data writer, one form, read back identically"
    (let [inv (gob/inventory fake-policy)
          text (pr-str inv)
          back (edn/read-string text)]
      (is (= (:policy-sha256 inv) (:policy-sha256 back)))
      (is (= (mapv :id (:obligations inv)) (mapv :id (:obligations back)))))))

(deftest an-unknown-recipe-is-named-never-an-empty-one
  (testing "\"A changed/unknown recipe refuses consumption\" -- so it must be visible"
    (let [r (gob/recipe "no-such-target-anywhere-in-this-makefile")]
      (is (= ["no-such-target-anywhere-in-this-makefile"] (:unresolved r)))
      (is (empty? (:targets r)))))
  (testing "a real gate target resolves, transitively, with a digest"
    (let [r (gob/recipe "mcp-test")]
      (is (empty? (:unresolved r)))
      (is (contains? (set (:targets r)) "mcp-test-checks")
          "mcp-test's checks are part of its recipe, per section 4")
      (is (re-matches #"[0-9a-f]{64}" (:sha256 r))))))

(deftest a-policy-hash-covers-the-rules-and-not-the-data-the-rules-read
  (testing "the battery ledger is DATA: the freshness check reads it, and it is expected"
    (testing "to change between the gate running and the landing merging"
      (let [inv (gob/inventory (gob/runner-policy))
            r2 (first (filter #(= :R2 (:id %)) (:obligations inv)))
            ledger (first (filter #(= "docs/observations/battery-ledger.edn" (:path %))
                                  (:required-inputs r2)))]
        (is (= :data (:role ledger))
            (str "the ledger must be :data -- hashing it into the policy makes every "
                 "landing that races a battery run refuse for a file the landing delta "
                 "allowlist already permits to change"))
        (is (string? (:sha256 ledger))
            "its digest is still recorded and reported; it is simply not policy"))))
  (testing "the rules themselves are POLICY and every one of them is hashed"
    (let [inv (gob/inventory (gob/runner-policy))]
      (is (every? #(= :policy (:role %))
                  (for [o (:obligations inv)
                        i (:required-inputs o)
                        :when (not= "docs/observations/battery-ledger.edn" (:path i))]
                    i))
          "the ledger is the only :data input today; a new one needs its own reason")))
  (testing "changing a DATA input's bytes does not move the policy hash, and"
    (testing "changing a POLICY input's bytes does"
      ;; Driven through `obligations` with a substituted digest rather than by
      ;; touching the real tree: a test that rewrote the repository's battery
      ;; ledger to prove a point about hashing would be a worse bug than the one
      ;; it is checking for.
      (let [base (gob/obligations fake-policy)
            with (fn [role sha]
                   (mapv #(assoc % :required-inputs
                                 [{:path "x" :role role :sha256 sha :bytes 1}])
                         base))
            h #(gob/sha256 (pr-str (#'gob/volatile-free %)))]
        (is (= (h (with :data "aaaa")) (h (with :data "bbbb")))
            "a data input's bytes are not policy")
        (is (not= (h (with :policy "aaaa")) (h (with :policy "bbbb")))
            "a policy input's bytes are")
        (is (not= (h (with :data "aaaa")) (h (with :policy "aaaa")))
            "and the ROLE itself is hashed, so reclassifying an input is visible")))))

(deftest the-required-inputs-of-a-gate-obligation-exist-in-this-tree
  (testing "a missing required input is named :absent, never dropped"
    (let [inv (gob/inventory (gob/runner-policy))
          absent (for [o (:obligations inv)
                       i (:required-inputs o)
                       :when (= :absent (:status i))]
                   [(:id o) (:path i)])]
      (is (empty? absent)
          (str "required inputs the tree does not have: " (pr-str (vec absent)))))))

(deftest the-real-tree-agrees-with-section-4s-independently-read-counts
  (testing "R3=2, R4=62 (55 fast + 7 integration), R5=49 at the pinned snapshot"
    (let [by-id (into {} (map (juxt :id identity))
                      (:obligations (gob/inventory (gob/runner-policy))))]
      (is (= 2 (count (:selected-test-identities (:R3 by-id)))))
      (is (= 49 (count (:selected-test-identities (:R5 by-id)))))
      (is (= (+ (get-in (:R4 by-id) [:scope :phases :fast])
                (get-in (:R4 by-id) [:scope :phases :integration]))
             (count (:selected-test-identities (:R4 by-id))))))))

;; ---------------------------------------------------------------------------
;; a banner is not a version
;; ---------------------------------------------------------------------------

(def ^:private real-java-output
  "The exact shape this box produces, banner first."
  (str "Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge\n"
       "openjdk version \"21.0.12\" 2026-07-21\n"
       "OpenJDK Runtime Environment (build 21.0.12+7)\n"))

(deftest the-java-tool-options-banner-is-never-the-version
  (testing "the exact string the historical ship receipt recorded as :toolchain-java"
    (is (tc/banner-line? "Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge"))
    (is (= "openjdk version \"21.0.12\" 2026-07-21" (tc/parse-version real-java-output)))
    (is (= ["Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge"]
           (tc/diagnostics real-java-output))
        "the banner is kept as a diagnostic -- captured, never promoted")))

(deftest banner-only-output-is-unknown-and-unknown-is-not-a-version
  (testing "a box where `java` prints only the banner refuses instead of agreeing"
    (is (nil? (tc/parse-version "Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge\n")))
    (is (nil? (tc/parse-version "")))
    (is (nil? (tc/parse-version nil)))))

(deftest two-different-boxes-with-the-same-banner-do-not-agree
  (testing "the defect, stated as an inequality: banner equality is not identity"
    (let [a (str "Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge\n"
                 "openjdk version \"21.0.12\" 2026-07-21\n")
          b (str "Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge\n"
                 "openjdk version \"17.0.9\" 2023-10-17\n")]
      (is (= (first (str/split-lines a)) (first (str/split-lines b)))
          "head -1 -- the old writer -- calls these two boxes identical")
      (is (not= (tc/parse-version a) (tc/parse-version b))
          "the repaired reader does not"))))
