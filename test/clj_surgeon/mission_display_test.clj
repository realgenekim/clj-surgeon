(ns clj-surgeon.mission-display-test
  {:lane :battery}
  (:require
   [clj-surgeon.mission :as mission]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(defn display [m opts]
  (if-let [f (try (requiring-resolve 'clj-surgeon.mission-display/show-result)
                  (catch Exception _ nil))]
    (f m opts)
    m))

(defn printed [m]
  (with-out-str (clojure.pprint/pprint m)))

(def saved
  ;; Faithful minimized executor receipt: protected-content refusal survives
  ;; beside plan/source data that made the previous `show` impractically large.
  {:id "M-1" :state :failed :effective_state :failed :verb "owner_forms"
   :root "/fixture" :question "Rename diagnostic identities"
   :snapshot {"a.clj" "sha"} :intent {:source "RAW-SOURCE-CANARY"}
   :plan {:sources {"a.clj" "RAW-SOURCE-CANARY"}
          :typist {:route {:executor :typist :k 1
                           :provider {:id :openrouter :model "openai/gpt-oss-120b"
                                      :upstream "Cerebras"}}}}
   :receipt {:committed false :verification-complete false
             :error-type :typist-all-candidates-rejected
             :candidates [{:index 0 :compiled false
                           :refusal {:error-type :forms-protected-content
                                     :lost {:comments ["important comment"]}
                                     :next_call {:operation "inspect"}}}]}
   :effective_next_action [:plan "M-1"] :graph ["M-1 [failed]"]})

(deftest bounded-show-exposes-authoritative-receipt-and-refusal
  (let [view (display saved {:workspace "/fixture"})]
    (is (= :saved-mission (:authority view)))
    (is (= :failed (:state view)))
    (is (false? (get-in view [:receipt :committed])))
    (is (= :typist-all-candidates-rejected (get-in view [:receipt :error-type])))
    (is (= {:comments ["important comment"]}
           (get-in view [:receipt :candidates 0 :refusal :lost])))
    (is (= {:operation "inspect"}
           (get-in view [:receipt :candidates 0 :refusal :next_call])))
    (is (not (str/includes? (printed view) "RAW-SOURCE-CANARY")))
    (is (= 1 (get-in view [:receipt :candidate-count])))
    (is (nil? (:receipt (display (dissoc saved :receipt) {}))))
    (is (= ["bin/mission" "show" "M-1" "--workspace" "/fixture" "--full"]
           (get-in view [:details :argv])))))

(deftest large-diagnostics-have-explicit-omissions-within-four-kib
  (let [candidate {:index 0 :compiled false :refusal {:lost (vec (repeat 30 (apply str (repeat 300 "秘密"))))}}
        view (display (assoc-in saved [:receipt :candidates] (vec (repeat 50 candidate))) {})]
    (is (<= (alength (.getBytes (printed view) "UTF-8")) 4096))
    (is (= 50 (get-in view [:receipt :candidate-count])))
    (is (:truncated view))
    (is (pos? (get-in view [:receipt :candidates-omitted] 0)))
    (is (= :failed (:state view))))
  (let [view (display {:ok false :error_type "mission-unknown-id"
                       :error (apply str (repeat 3000 "秘密"))} {})]
    (is (<= (alength (.getBytes (printed view) "UTF-8")) 4096))
    (is (:truncated view))))

(deftest explicit-full-show-retains-the-original-view
  (is (= saved (display saved {:full true})))
  (is (str/includes? (mission/help-text "show") "--full")))

(defn with-ledger [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "mission-display-' quoted-" (make-array java.nio.file.attribute.FileAttribute 0)))
        state (io/file root "state")
        dir (mission/workspace-state-dir (str root) (str state))]
    (try
      (mission/write-mission! dir (assoc saved :root (str root)))
      (f root state)
      (finally (doseq [p (reverse (file-seq root))] (io/delete-file p true))))))

(deftest public-show-is-a-readable-receipt-and-full-is-explicit
  (with-ledger
    (fn [root state]
      (let [base ["bin/mission" "show" "M-1" "--workspace" (str root) "--state-home" (str state)]
            compact (apply shell/sh base)
            full (apply shell/sh (conj base "--full"))
            view (edn/read-string (:out compact))]
        (is (= 0 (:exit compact) (:exit full)))
        (is (= :saved-mission (:authority view)))
        (is (= view ((requiring-resolve 'clj-surgeon.mission-cli/show)
                     {:id "M-1" :workspace (str root) :state-home (str state)})))
        (is (false? (get-in view [:receipt :committed])))
        (is (not (str/includes? (:out compact) "RAW-SOURCE-CANARY")))
        (is (str/includes? (:out full) "RAW-SOURCE-CANARY"))))))

(deftest missing-mission-is-nonzero-with-an-executable-recovery
  (with-ledger
    (fn [root state]
      (let [result (shell/sh "bin/mission" "show" "M-999"
                             "--workspace" (str root) "--state-home" (str state))
            receipt (edn/read-string (:out result))
            argv (get-in receipt [:example :argv])]
        (is (= 1 (:exit result)))
        (is (= "mission-unknown-id" (:error_type receipt)))
        (is (= receipt ((requiring-resolve 'clj-surgeon.mission-cli/show)
                        {:id "M-999" :workspace (str root) :state-home (str state)})))
        (is (= ["bin/mission" "list" "--workspace" (str root)
                "--state-home" (str state)] argv))
        (when (seq argv)
          (is (= 0 (:exit (apply shell/sh argv))))
          (is (= 0 (:exit (shell/sh "bash" "-c" (get-in receipt [:example :command]))))))))))

(deftest mission-write-refusal-carries-a-runnable-inspection-example
  (with-ledger
    (fn [root state]
      (let [spec (io/file root "refused.edn")]
        (spit spec (pr-str {:verb "helper_extraction" :request {:workspace_root (str root)}}))
        (let [result (shell/sh "bin/mission" "run" "--spec-file" (str spec)
                               "--state-home" (str state))
              receipt (edn/read-string (:out result))
              argv (get-in receipt [:example :argv])]
          (is (= 1 (:exit result)))
          (is (= "mission-run-request" (:error_type receipt)))
          (is (= "list" (second argv)))
          (when (seq argv)
            (is (= 0 (:exit (apply shell/sh argv))))))))))

(deftest missing-workspace-refuses-without-a-stacktrace
  ;; Field report: show M-1 --state-home ... dereferenced nil workspace.
  (let [r (shell/sh "bin/mission" "show" "M-1" "--state-home" "/var/tmp/forge/astra-live-real1-fx/state")]
    (is (= 1 (:exit r)))
    (is (not (.contains (:err r) "NullPointerException")))
    (when (seq (:out r))
      (let [value (edn/read-string (:out r))]
        (is (= :mission-workspace-required (:error-type value)))
        (is (= value ((requiring-resolve 'clj-surgeon.mission-cli/show) {:id "M-1" :state-home "/var/tmp/forge/astra-live-real1-fx/state"})))
        (is (= 0 (:exit (apply shell/sh (get-in value [:example :argv])))))))))
