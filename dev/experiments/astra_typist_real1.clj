(ns astra-typist-real1
  "Fake-provider hand-drive of the real-1 frozen fixture. No provider or wall claim."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mission-cli :as cli]
   [clj-surgeon.mission-typist-executor :as executor]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def file "src/clj_surgeon/diagnostic_delta.clj")
(def owners ["field" "finding-identity" "valid-finding?" "representative-difference" "diagnostic-delta"])

(defn -main [proof-json]
  (let [{:keys [root gate accept]} (json/parse-string (slurp proof-json) true)
        profiles {"real1-gate" {:commands [["bb" "-cp" "src:test" "-e" gate]]
                                :measured-ms 62 :evidence "Fable-real1-bb-gate"}
                  "real1-witness" {:commands [["bb" "-cp" "src:test" "-e" accept]]
                                   :measured-ms 100 :evidence "fake-harness-witness-budget-not-a-measurement"}}
        request {:workspace_root root :intent "Rename finding-identity to finding-fingerprint and field to finding-field; preserve all behavior, docs and local bindings."
                 :owners (mapv (fn [owner]
                                 (cond-> {:file file :owner owner}
                                   (= owner "field") (assoc :new-owner "finding-field")
                                   (= owner "finding-identity") (assoc :new-owner "finding-fingerprint"))) owners)
                 :proof-files ["deps.edn" ".clj-surgeon.edn" "test/clj_surgeon/diagnostic_delta_test.clj"]
                 :verification {:profile "real1-gate"} :acceptance_profile "real1-witness"
                 :typist {:mission-class :rename
                          :source-policy {file {:generated? false :reader-conditionals? false :format-sensitive? false}}
                          :budget {:max-files 1 :max-changed-chars 12000}
                          :provider {:id :openrouter :model "openai/gpt-oss-120b" :upstream "Cerebras"}
                          ;; Synthetic rate is ONLY for fake transport admission tests.
                          :rate {:mission-class :rename :provider :openrouter :model "openai/gpt-oss-120b"
                                 :upstream "Cerebras" :verified 1 :attempted 1
                                 :evidence "FAKE-TRANSPORT-ONLY-NOT-PROVIDER-EVIDENCE"}}}
        home (str (io/file (.getParentFile (io/file root)) "ledger"))
        opened (cli/propose! {:verb "owner_forms" :request request :profiles profiles :state-home home})]
    (when-not (= :ready (:state opened)) (throw (ex-info "Planning refused" opened)))
    (let [authority (get-in opened [:plan :typist])
          sources (get-in authority [:basis :sources])
          replacements (mapv (fn [{:keys [file owner start end]}]
                               {:file file :owner owner
                                :form (-> (subs (get sources file) start end)
                                          (str/replace "finding-identity" "finding-fingerprint")
                                          (str/replace "(field " "(finding-field ")
                                          (str/replace "(defn- field" "(defn- finding-field"))})
                             (get-in authority [:basis :owners]))
          opts {:id (:id opened) :workspace root :state-home home
                :receipt-dir (str (io/file (.getParentFile (io/file root)) "receipts"))}
          before (slurp (io/file root file))]
      (with-redefs [executor/request-candidates! (fn [_] [{:usable true :content (json/generate-string replacements)}])]
        (let [applied (cli/apply! opts)]
          (when-not (= :verified (:state applied)) (throw (ex-info "Apply did not verify" applied)))
          (let [undone (cli/undo! opts)]
            (when-not (and (= :undone (:state undone)) (= before (slurp (io/file root file))))
              (throw (ex-info "Undo not exact" undone)))
            (prn {:fake-provider true :mission (:id opened) :owners (count replacements)
                  :planned true :verified true :undo-exact true
                  :artifacts (get-in applied [:receipt :artifacts])})))))))
