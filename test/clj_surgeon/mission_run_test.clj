(ns clj-surgeon.mission-run-test
  {:lane :battery}
  (:require
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-cli :as cli]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn invoke-run [opts]
  ;; Resolve at test time so the pre-implementation run produces useful REDs.
  (if-let [entry (get (ns-publics 'clj-surgeon.mission-cli) 'run!)]
    (entry opts)
    {:ok false :error_type "missing-run"}))

(deftest run-stops-before-planning-unsupported-requests
  (doseq [opts [{} {:verb "helper_extraction"}
                {:verb "owner_forms" :id "M-existing"}]]
    (let [calls (atom [])]
      (with-redefs [cli/propose! #(swap! calls conj [:propose %])
                    cli/apply! #(swap! calls conj [:apply %])]
        (is (= "mission-run-request" (:error_type (invoke-run opts))))
        (is (empty? @calls))))))

(deftest run-stops-on-every-not-ready-state
  (doseq [proposal [{:ok false :error_type "mission-bad-input"}
                    {:id "M-1" :state :blocked :decision {:because "missing authority"}}
                    {:id "M-1" :state :failed}
                    {:id "M-1" :state :verified}
                    {:id "M-1" :state :proposed}]]
    (let [calls (atom [])]
      (with-redefs [cli/propose! (fn [_] proposal)
                    cli/apply! #(swap! calls conj %)]
        (let [result (invoke-run {:verb "owner_forms"})]
          (is (false? (:ok result)))
          (is (= (if (false? (:ok proposal))
                   "mission-bad-input" "mission-not-ready")
                 (:error_type result)))
          (when-not (false? (:ok proposal))
            (is (= (:id proposal) (:id result)))
            (is (= (:decision proposal) (:decision result))))
          (is (empty? @calls)))))))

(deftest run-uses-new-id-root-and-saved-authority
  (let [calls (atom [])
        opts {:verb "owner_forms" :request {:workspace_root "/request"}
              :workspace "/wrong" :id nil :state-home "/state"
              :receipt-dir "/receipts" :profiles {:bad "override"}
              :spec {:untrusted "not forwarded"} :config "/config"}
        terminal {:state :verified :receipt {:committed true}}]
    (with-redefs [cli/propose! (fn [o] (swap! calls conj [:propose o])
                                 {:state :ready :id "M-new" :root "/saved"})
                  cli/apply! (fn [o] (swap! calls conj [:apply o]) terminal)]
      (is (= terminal (invoke-run opts)))
      (is (= [[:propose opts]
              [:apply {:id "M-new" :workspace "/saved"
                       :state-home "/state" :receipt-dir "/receipts"}]]
             @calls)))))

(deftest run-preserves-apply-failure
  (let [failure {:ok false :error_type "mission-stale-snapshot"}]
    (with-redefs [cli/propose! (fn [_] {:id "M-1" :root "/fixture" :state :ready})
                  cli/apply! (fn [_] failure)]
      (is (= failure (invoke-run {:verb "owner_forms"}))))))

(deftest one-process-run-persists-before-execution-without-replanning
  ;; Real-1 motivated the one-process entrance: two cold startups surrounded
  ;; a sub-three-second executor. Freeze a diagnostic-delta-shaped owner here;
  ;; actual source transformation remains the executor's independent contract.
  (let [saved (atom nil) calls (atom [])
        request {:workspace_root "/fixture" :intent "rename finding-identity"
                 :owners [{:file "diagnostic_delta.clj" :owner "finding-identity"}]
                 :verification {:profile "gate"} :acceptance_profile "accept"}
        plan {:ok true :sources {"/fixture/diagnostic_delta.clj"
                                 "(defn finding-identity [finding] (:message finding))"}
              :typist {:dossier {:intent "frozen"} :route {:k 1}}}
        profiles {"gate" {:commands [["bb" "gate.clj"]]}}
        route {:plan (fn [r p] (swap! calls conj [:plan r p]) plan)
               :execute! (fn [r config]
                           (is (= :applied (:state @saved)))
                           (is (= plan (:plan @saved) (:plan config)))
                           (is (= request r (:intent @saved)))
                           (swap! calls conj [:execute])
                           {:committed true :undo_receipt "undo" :receipt_hash "sha"})}]
    (with-redefs-fn {#'cli/verbs {"owner_forms" route}
                     #'cli/state-dir-for (fn [& _] "/ledger")
                     #'cli/admitted-profiles (fn [& _] profiles)
                     #'cli/stale? (fn [_] nil)
                     #'mission/next-id (fn [_] "M-new")
                     #'mission/read-mission (fn [& _] @saved)
                     #'mission/read-all (fn [_] [])
                     #'cli/save! (fn [_ m] (swap! calls conj [:save (:state m)])
                                   (reset! saved m))}
      #(let [result (invoke-run {:verb "owner_forms" :request request})]
         (is (= :verified (:state result)))
         (is (= [:plan :save :save :execute :save] (mapv first @calls)))
         (is (= [:ready :applied :verified]
                (mapv second (filter (comp #{:save} first) @calls))))))))

(deftest run-help-and-parsing-are-explicit-about-write-authority
  (let [help (mission/help-text "run")]
    (is (str/includes? help "run --spec-file"))
    (is (str/includes? help "owner_forms"))
    (is (str/includes? help "immediately"))
    (is (str/includes? help "propose"))
    (is (str/includes? help "docs/mission-typist.md"))
    (is (not (str/includes? help "THE SPEC")))
    (is (str/includes? (mission/help-text nil) "run --spec-file")))
  (let [direct (shell/sh "bin/mission" "help" "run")
        flag (shell/sh "bin/mission" "run" "--help")]
    (is (= 0 (:exit direct) (:exit flag)))
    (is (= (:out direct) (:out flag))))
  (is (= {:positional ["run"] :spec-file "-" :state-home "/state"}
         (cli/parse-flags ["run" "--spec-file" "-" "--state-home" "/state"]))))

(defn with-fixture [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "mission-run-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try (f root)
         (finally (doseq [p (reverse (file-seq root))] (io/delete-file p true))))))

(deftest launcher-routes-run-to-exactly-one-jvm
  (with-fixture
    (fn [root]
      (let [stub (io/file root "clojure")
            args-file (io/file root "args")]
        (spit stub (str "#!/usr/bin/env bash\nprintf '%s\\n' \"$@\" > \"$MISSION_RUN_ARGS\"\n"
                        "printf '{:state :verified}\\n'\n"))
        (.setExecutable stub true)
        (let [result (shell/sh "bin/mission" "run" "--spec-file" "-"
                               :env (assoc (into {} (System/getenv))
                                           "PATH" (str root ":" (System/getenv "PATH"))
                                           "MISSION_RUN_ARGS" (str args-file))
                               :in "{:verb \"owner_forms\"}")]
          (is (= 0 (:exit result)))
          (is (= {:state :verified} (edn/read-string (:out result))))
          (is (= ["-M:clj-surgeon/mcp" "-m" "clj-surgeon.mission-cli"
                  "run" "--spec-file" "-"]
                 (when (.exists args-file) (str/split-lines (slurp args-file))))))))))

(deftest public-run-refusal-is-edn-nonzero-and-preserves-source
  (with-fixture
    (fn [root]
      (let [source (io/file root "diagnostic_delta.clj")
            before "(defn finding-identity [finding] (:message finding))\n"
            spec (io/file root "request.edn")]
        (spit source before)
        ;; Missing verification must fail before any provider call or source write.
        (spit spec (pr-str {:verb "owner_forms"
                            :request {:workspace_root (str root)
                                      :intent "rename finding-identity"
                                      :owners [{:file "diagnostic_delta.clj"
                                                :owner "finding-identity"}]}}))
        (let [result (shell/sh "bin/mission" "run" "--spec-file" (str spec)
                               "--state-home" (str (io/file root "state")))
              receipt (try (edn/read-string (:out result))
                           (catch Exception _ nil))]
          (is (= 1 (:exit result)))
          (is (= "mission-not-ready" (:error_type receipt)))
          (is (string? (:id receipt)))
          (is (= before (slurp source)))
          (is (not (str/includes? (:err result) "Exception"))))))))
