(ns ^{:lane :fast} clj-surgeon.battery-ledger-test
  "TEST-ISO-009a/b's witness -- the battery receipt ledger and its freshness
   tripwire, driven through EVERY state it can be in.

   LANE: :fast. The tripwire's decision is arithmetic over an event log, so
   the whole of it is exercised without a `git` call, without a clone, and
   without waiting 26 h: the clock is a parameter and the ancestry lookup is
   injected. That is the reason the decision was written as a pure function in
   the first place -- a tripwire whose refusals can only be reproduced by
   letting a day pass is a tripwire nobody ever proves."
  (:require
   [clj-surgeon.battery-ledger :as ledger]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private now
  "2026-09-05T00:00:00Z, as ms. A fixed instant: the witness must not depend
   on when it is run."
  (.toEpochMilli (java.time.Instant/parse "2026-09-05T00:00:00Z")))

(defn- ago
  "An ISO instant `hours` before `now`."
  [hours]
  (str (java.time.Instant/ofEpochMilli (long (- now (* hours 3600000))))))

(defn- entry
  [& {:keys [hours verdict sha] :or {hours 1 verdict :pass sha "abc1234"}}]
  {:sha sha :started (ago hours) :wall_s 727 :verdict verdict :host "anvil"})

(defn- ledger-text [& entries] (str/join "\n" (map ledger/entry-line entries)))

(def ^:private in-tree (constantly 3))
(def ^:private not-an-ancestor (constantly nil))

(defn- check [text behind-fn]
  (ledger/freshness (ledger/parse-ledger text) now behind-fn))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-009a -- the ledger is an append-only event log
;; ---------------------------------------------------------------------------

(deftest an-entry-round-trips-through-one-line
  (let [e (entry)]
    (is (not (str/includes? (ledger/entry-line e) "\n"))
        "one entry is ONE line -- appending must never need to read the file")
    (is (= [e] (ledger/parse-ledger (ledger/entry-line e))))))

(deftest appending-never-rewrites-what-is-already-there
  (let [f (io/file (System/getProperty "java.io.tmpdir")
                   (str "battery-ledger-" (System/nanoTime) ".edn"))]
    (try
      (ledger/append-entry! (.getPath f) (entry :sha "aaaaaaa"))
      (ledger/append-entry! (.getPath f) (entry :sha "bbbbbbb"))
      (let [entries (ledger/parse-ledger (slurp f))]
        (is (= 2 (count entries)))
        (is (= ["aaaaaaa" "bbbbbbb"] (mapv :sha entries))
            "order is arrival order, and the first entry survived the second")
        (is (= :pass (:verdict (first entries)))))
      (finally (io/delete-file f true)))))

(deftest a-failed-battery-is-still-recorded
  (testing "the ledger records what HAPPENED, including a red run -- a ledger
            that only holds successes cannot tell you the gate is broken"
    (let [entries (ledger/parse-ledger (ledger-text (entry :verdict :fail)))]
      (is (= :fail (:verdict (first entries)))))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-009b -- the tripwire, in BOTH states and every refusal
;; ---------------------------------------------------------------------------

(deftest a-fresh-receipt-on-this-tree-passes
  (let [r (check (ledger-text (entry :hours 25) (entry :hours 2)) in-tree)]
    (is (:ok r) (str "expected fresh, got " (pr-str r)))
    (is (= 2.0 (:age-hours r)) "age is measured from the NEWEST entry")
    (is (= 3 (:commits-behind r)))))

(deftest an-empty-ledger-refuses
  (doseq [text ["" nil "   \n\n"]]
    (let [r (check text in-tree)]
      (is (false? (:ok r)))
      (is (= :no-entries (:reason r)))
      (is (str/includes? (:message r) ledger/ledger-path))
      (is (str/includes? (:remedy r) "make test-battery")))))

(deftest a-receipt-older-than-the-budget-refuses-and-names-the-age
  (testing "26 h is the boundary: at it, fresh; past it, refused"
    (is (:ok (check (ledger-text (entry :hours 26)) in-tree))
        "exactly at the budget still passes -- the nightly's two-hour margin")
    (let [r (check (ledger-text (entry :hours 40)) in-tree)]
      (is (false? (:ok r)))
      (is (= :stale (:reason r)))
      (is (str/includes? (:message r) "40.0 h old")
          (str "the refusal must name HOW stale, got " (pr-str (:message r))))
      (is (str/includes? (:message r) "26 h")))))

(deftest a-failed-newest-receipt-refuses
  (let [r (check (ledger-text (entry :hours 30 :verdict :pass)
                              (entry :hours 1 :verdict :fail))
                 in-tree)]
    (is (false? (:ok r)))
    (is (= :last-run-failed (:reason r))
        "a green entry from yesterday must not paper over a red one from today")
    (is (str/includes? (:message r) ":fail"))))

(deftest a-receipt-from-a-tree-this-one-does-not-descend-from-refuses
  (let [r (check (ledger-text (entry :hours 1)) not-an-ancestor)]
    (is (false? (:ok r)))
    (is (= :not-an-ancestor (:reason r)))
    (is (str/includes? (:message r) "abc1234"))))

(deftest a-receipt-more-than-thirty-commits-behind-refuses
  (testing "N=30 is the boundary"
    (is (:ok (check (ledger-text (entry :hours 1)) (constantly 30))))
    (let [r (check (ledger-text (entry :hours 1)) (constantly 31))]
      (is (false? (:ok r)))
      (is (= :too-far-behind (:reason r)))
      (is (str/includes? (:message r) "31 commits behind")))))

(deftest a-corrupt-line-refuses-rather-than-shortening-the-ledger
  (testing "a receipt that does not read must not be able to make the ledger
            look merely shorter -- silence is the failure mode this whole
            mechanism exists to remove"
    (let [r (check (str (ledger/entry-line (entry :hours 1)) "\n{:sha \"x\" :started")
                   in-tree)]
      (is (false? (:ok r)))
      (is (= :unreadable-entry (:reason r))))))

(deftest every-refusal-carries-the-remedy
  (doseq [[label text behind] [["empty" "" in-tree]
                               ["stale" (ledger-text (entry :hours 40)) in-tree]
                               ["failed" (ledger-text (entry :verdict :fail)) in-tree]
                               ["orphan sha" (ledger-text (entry)) not-an-ancestor]
                               ["far behind" (ledger-text (entry)) (constantly 99)]]]
    (testing label
      (let [r (check text behind)]
        (is (false? (:ok r)))
        (is (str/includes? (:remedy r) "flock /home/forge/tmp/suite.lock make test-battery")
            "a refusal without the exact command is a puzzle, not an alarm")))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-009a -- the runner really writes it, and the gate really reads it
;; ---------------------------------------------------------------------------

(deftest the-make-targets-are-wired-to-this-mechanism
  (let [makefile (slurp (io/file "Makefile"))]
    (testing "test-battery appends a receipt whatever the verdict"
      (is (re-find #"(?m)^test-battery:" makefile))
      (is (str/includes? makefile "battery_ledger.clj append")
          "make test-battery must append its receipt, or the ledger is fiction")
      (is (str/includes? makefile "verdict=fail")
          "a FAILING battery must still be recorded -- a ledger of successes
           only cannot distinguish a broken gate from an absent one"))
    (testing "battery-fresh is a target of its own"
      (is (re-find #"(?m)^battery-fresh:" makefile))
      (is (str/includes? makefile "battery_ledger.clj check")))
    (testing "the records-lane exemption has an END-TO-END witness with a real
              `git` history, and something actually invokes it -- the fast
              witness above proves the CLASSIFIER, not the git plumbing that
              feeds it, and a self-test no target runs is a diary entry"
      (is (re-find #"(?m)^battery-fresh-records-lane-test:" makefile))
      (is (str/includes? makefile "test/battery_fresh_records_lane_test.sh"))
      (is (re-find #"(?m)^\t\$\(MAKE\) --no-print-directory battery-fresh-records-lane-test$"
                   makefile)
          "it must be invoked by a lane, not merely defined"))))

;; @spec TEST-ISO-009b -- records-lane classification; no Git in fast lane.
(deftest only-regular-records-lane-content-is-exempt
  (testing "the records lane is a PREFIX now, not a closed three-path set --
            because writing a NEW file is the records lane's normal act, and a
            closed set covered only the commits nobody makes"
    (let [z (apply str (repeat 40 "0"))
          a (apply str (repeat 40 "a"))
          b (apply str (repeat 40 "b"))
          raw (fn [status old new osha nsha p]
                (str ":" old " " new " " osha " " nsha " " status "\u0000" p "\u0000"))
          modify (fn [p] (raw "M" "100644" "100644" a b p))
          add    (fn [p] (raw "A" "000000" "100644" z b p))
          delete (fn [p] (raw "D" "100644" "000000" a z p))
          journal "docs/observations/2026-09-06-live-astra-typist-commentary.md"
          brand-new "docs/observations/2026-09-08-a-log-written-today.md"
          good (modify journal)]
      (testing "modify, ADD and DELETE of any regular file in the lane"
        (doseq [d [(modify journal) (add brand-new) (delete brand-new)
                   (str (add brand-new) (modify journal))]]
          (is (ledger/archive-only-diff? d) (pr-str d))))
      (testing "every merge parent must independently prove records-lane content"
        (doseq [[second-out exit expected] [[good 0 true] ["" 0 false]
                                            [good 1 false]
                                            [(modify "src/x.clj") 0 false]]]
          (with-redefs-fn {#'ledger/sh (fn [& args]
                                       (if (= "p2" (nth args (- (count args) 3)))
                                         {:exit exit :out second-out}
                                         {:exit 0 :out good}))}
            #(is (= expected (#'ledger/archive-only-commit? "commit p1 p2"))))))
      (testing "what the widening must NOT reach"
        (doseq [bad ["" "garbage" (subs good 0 (dec (count good)))
                     ;; the ledger may never exempt its own commit
                     (modify "docs/observations/battery-ledger.edn")
                     (add "docs/observations/battery-ledger.edn")
                     ;; outside the lane, including the rest of docs/
                     (modify "docs/intent/test-isolation/test-isolation-specs.md")
                     (modify "docs/plans/battery-archival-distance.md")
                     (modify "src/x.clj")
                     (add "test/y_test.clj")
                     ;; executables, symlinks, submodules, type and mode changes
                     (raw "M" "100644" "100755" a b journal)
                     (raw "A" "000000" "100755" z b brand-new)
                     (raw "M" "120000" "120000" a b journal)
                     (raw "M" "160000" "160000" a b journal)
                     ;; a mixed commit is a CODE commit, whatever else it touched
                     (str good (modify "src/x.clj"))]]
          (is (not (ledger/archive-only-diff? bad)) (pr-str bad)))))))

;; @spec TEST-ISO-009b -- the defect this exemption exists for, at its numbers.
(deftest records-lane-churn-cannot-expire-a-battery-but-code-still-can
  (let [distance (fn [raw ignored]
                   (constantly {:commits-behind (- raw ignored)
                                :raw-commits-behind raw
                                :ignored-archive-commits ignored}))
        fresh (ledger-text (entry :hours 1))]
    (testing "40 records-only commits since the receipt: STILL FRESH. Before
              this, one busy records day expired a green battery and the seat
              paid a seven-minute rerun for paperwork it had just written"
      (let [r (check fresh (distance 40 40))]
        (is (:ok r) (str "expected fresh, got " (pr-str r)))
        (is (= 0 (:commits-behind r)))
        (is (= 40 (:raw-commits-behind r)) "the RAW distance is still reported")
        (is (= 40 (:ignored-archive-commits r)))))
    (testing "31 CODE commits still refuse -- the 30 budget is untouched"
      (let [r (check fresh (distance 31 0))]
        (is (false? (:ok r)))
        (is (= :too-far-behind (:reason r)))
        (is (str/includes? (:message r) "31 commits behind"))))
    (testing "records commits cannot BUY slack for code: 40 records + 31 code
              is still a refusal, because only the 31 are counted"
      (let [r (check fresh (distance 71 40))]
        (is (false? (:ok r)))
        (is (= :too-far-behind (:reason r)))
        (is (str/includes? (:message r) "31 commits behind"))))
    (testing "and exactly 30 code commits under a mountain of records passes"
      (is (:ok (check fresh (distance 970 940)))))))

(deftest archive-distance-keeps-the-raw-audit-and-failure-authority
  (let [distance (constantly {:commits-behind 22 :raw-commits-behind 55
                              :ignored-archive-commits 33})
        r (check (ledger-text (entry)) distance)]
    (is (:ok r))
    (is (= 22 (:commits-behind r)))
    (is (= 55 (:raw-commits-behind r)))
    (is (= 33 (:ignored-archive-commits r)))
    (is (= :last-run-failed (:reason (check (ledger-text (entry :verdict :fail)) distance))))))
