(ns cli-mcp-transport-differential
  (:require
   [cheshire.core :as json]
   [clj-surgeon.core :as core]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-contract :as mcp-contract]
   [clj-surgeon.mcp-inspect :as mcp-inspect]
   [clj-surgeon.show-form :as show-form]
   [clojure.string :as str])
  (:import
   (java.nio.charset StandardCharsets)
   (java.security MessageDigest)))

(def candidate-commit
  "a2a928db68ea896536133c017ac439634fa070cd")

(defn sha256
  [value]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (str value) StandardCharsets/UTF_8))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn byte-evidence
  [text]
  (let [bytes (.getBytes (str text) StandardCharsets/UTF_8)]
    {:text (str text)
     :utf8-hex (apply str (map #(format "%02x" (bit-and 0xff %)) bytes))
     :byte-count (alength bytes)
     :sha256 (sha256 text)}))

(defn canonical-data
  [value]
  (cond
    (map? value)
    [:map
     (->> value
          (map (fn [[key item]]
                 [(canonical-data key) (canonical-data item)]))
          (sort-by pr-str)
          vec)]

    (vector? value) [:vector (mapv canonical-data value)]
    (set? value) [:set (->> value (map canonical-data) (sort-by pr-str) vec)]
    (sequential? value) [:sequence (mapv canonical-data value)]
    :else [:scalar value]))

(defn data-hash
  [value]
  (sha256 (pr-str (canonical-data value))))

(defn cli-stdin-envelope
  [operation intended-edn]
  (let [stdin-text (str (pr-str intended-edn) "\n")
        argv ["clj-surgeon" ":op" (str ":" (name operation))
              ":spec-file" "-"]
        shell-program
        (str "exec \"$CLJ_SURGEON\" :op :" (name operation)
             " :spec-file - < \"$CLJ_SURGEON_REQUEST\"")]
    {:intended-edn intended-edn
     :intended-hash (data-hash intended-edn)
     :argv argv
     :argv-bytes (byte-evidence (str/join "\u0000" argv))
     :stdin-bytes (byte-evidence stdin-text)
     :shell-program-bytes (byte-evidence shell-program)
     :parsed-request (core/parse-spec-document stdin-text "captured stdin")}))

(defn mcp-json-envelope
  [intended-json]
  (let [json-text (json/generate-string intended-json)
        transmitted (json/parse-string json-text)]
    {:intended-json-object intended-json
     :intended-hash (data-hash intended-json)
     :transmitted-bytes (byte-evidence json-text)
     :transmitted-request transmitted}))

(defn legacy-printf-shell-program
  [intended-edn]
  (str "printf '%s\\n' '" (pr-str intended-edn) "'"))

(defn literal-heredoc-shell-program
  [intended-edn]
  (str "/bin/cat <<'CLJ_SURGEON_EDN'\n"
       (pr-str intended-edn) "\n"
       "CLJ_SURGEON_EDN\n"))

(def read-sources
  {"src/sample/alpha.clj"
   (str "(ns sample.alpha)\n"
        ";; attached comment survives\n"
        "(defn alpha [] :alpha)\n"
        "(defn beta [] \"β\")\n")
   "src/sample/gamma.clj"
   (str "(ns sample.gamma)\n"
        "(def gamma {:message \"O'Reilly\"})\n")})

(def read-cli-spec
  {:reads [{:file "src/sample/alpha.clj" :forms ['alpha 'beta]}
           {:file "src/sample/gamma.clj" :forms ['gamma]}]
   :expect {:file-count 2 :form-count 3}})

(def read-mcp-request
  {"requests"
   [{"id" "alpha-beta"
     "operation" "forms"
     "file" "src/sample/alpha.clj"
     "forms" ["alpha" "beta"]
     "expect" {"forms" 2}}
    {"id" "gamma"
     "operation" "forms"
     "file" "src/sample/gamma.clj"
     "forms" ["gamma"]
     "expect" {"forms" 1}}]
   "expect" {"requests" 2 "files" 2}})

(def change-source
  (str "(ns sample.change)\n"
       "(defn render []\n"
       "  {:message \"O'Reilly said \\\"hi\\\"; path C:\\\\tmp\\nnext\"})\n"))

(def old-message
  "{:message \"O'Reilly said \\\"hi\\\"; path C:\\\\tmp\\nnext\"}")

(def new-message
  "{:message \"O'Reilly said \\\"done\\\"; path C:\\\\tmp\\nnext\"}")

(def change-sources
  {"src/sample/change.clj" change-source})

(def change-cli-spec
  {:changes
   [{:id :message
     :in ["src/sample/change.clj"]
     :forms ['render]
     :find old-message
     :do [:replace new-message]
     :expect {:matches 1 :each-form 1}}]
   :expect {:changes 1 :edits 1 :files 1}})

(def change-mcp-request
  {"changes"
   [{"id" "message"
     "files" ["src/sample/change.clj"]
     "forms" ["render"]
     "find" old-message
     "replace" new-message
     "expect" {"matches" 1 "each_form" 1}}]
   "expect" {"changes" 1 "edits" 1 "files" 1}})

(def selector-source
  (str "(ns sample.selector)\n"
       "(defn editor-gesture-schema [] :schema)\n"
       "(defn execute-request-in-context! [] :ok)\n"))

(def selector-sources
  {"src/sample/selector.clj" selector-source})

(def requested-missing-owner "editor-gesture-scheam")

(def selector-cli-spec
  {:reads [{:file "src/sample/selector.clj"
            :forms [(symbol requested-missing-owner)]}]
   :expect {:file-count 1 :form-count 1}})

(def selector-mcp-request
  {"requests"
   [{"id" "selector"
     "operation" "forms"
     "file" "src/sample/selector.clj"
     "forms" [requested-missing-owner]
     "expect" {"forms" 1}}]
   "expect" {"requests" 1 "files" 1}})

(defn snapshot-map
  [sources]
  (into {}
        (map (fn [[file source]]
               [file {:file file :source source :hash (sha256 source)}]))
        sources))

(defn cli-read-outcomes
  [sources spec]
  (mapv (fn [{:keys [file forms]}]
          (show-form/select-form file (get sources file) {:forms forms}))
        (:reads spec)))

(defn normalized-read-forms
  [outcomes]
  (mapv (fn [[outcome form]]
          {:file (or (:file form) (:file outcome))
           :name (str (:name form))
           :source (:source form)
           :line (:line form)
           :end-line (or (:end-line form) (:end_line form))})
        (mapcat (fn [outcome]
                  (map (fn [form] [outcome form]) (:forms outcome)))
                outcomes)))

(defn normalized-mcp-read-forms
  [outcome]
  (normalized-read-forms (:results outcome)))

(defn normalized-hypotheses
  [selection-failure]
  (mapv (fn [candidate]
          {:owner (:owner candidate)
           :rank (:rank candidate)
           :ranking-basis (keyword (name (or (:ranking-basis candidate)
                                             (:ranking_basis candidate))))
           :authority (:authority candidate)})
        (:hypotheses selection-failure)))

(defn normalized-selector-facts
  [outcome]
  (let [failures (or (:selection-failures outcome)
                     (:selection_failures outcome))]
    {:available-owners (or (:available-owners outcome)
                           (:available_owners outcome))
     :available-owner-count (or (:available-owner-count outcome)
                                (:available_owner_count outcome))
     :selection-failures
     (mapv (fn [failure]
             {:requested-owner (or (:requested-owner failure)
                                   (:requested_owner failure))
              :failure-kind (keyword (name (or (:failure-kind failure)
                                               (:failure_kind failure))))
              :match-count (or (:match-count failure) (:match_count failure))
              :hypotheses (normalized-hypotheses failure)})
           failures)}))

(defn read-differential
  []
  (let [cli (cli-stdin-envelope :cat read-cli-spec)
        cli-outcome (cli-read-outcomes read-sources (:parsed-request cli))
        mcp (mcp-json-envelope read-mcp-request)
        validated (mcp-inspect/validate-inspect-params (:transmitted-request mcp))
        mcp-outcome (mcp-inspect/evaluate-snapshots
                      (:params validated) (snapshot-map read-sources))
        cli-facts (normalized-read-forms cli-outcome)
        mcp-facts (normalized-mcp-read-forms mcp-outcome)]
    {:stratum :batched-exact-read
     :cli (assoc cli
                 :kernel-outcome cli-outcome
                 :kernel-outcome-hash (data-hash cli-outcome))
     :mcp (assoc mcp
                 :normalized-request (:params validated)
                 :normalized-request-hash (data-hash (:params validated))
                 :kernel-outcome mcp-outcome
                 :kernel-outcome-hash (data-hash mcp-outcome))
     :equivalence {:correct (and (:ok validated) (:ok mcp-outcome))
                   :semantic-facts-equal (= cli-facts mcp-facts)
                   :cli-facts-hash (data-hash cli-facts)
                   :mcp-facts-hash (data-hash mcp-facts)}}))

(defn change-differential
  []
  (let [cli (cli-stdin-envelope :change change-cli-spec)
        cli-compiled (transaction/compile-transaction
                       change-sources (:parsed-request cli))
        mcp (mcp-json-envelope change-mcp-request)
        validated (mcp-contract/validate-tool-params (:transmitted-request mcp))
        mcp-spec (when (:ok validated)
                   (mcp-contract/tool-params->transaction (:params validated)))
        mcp-compiled (transaction/compile-transaction change-sources mcp-spec)]
    {:stratum :generic-intent-change
     :cli (assoc cli
                 :compiled-intent cli-compiled
                 :compiled-intent-hash (data-hash cli-compiled))
     :mcp (assoc mcp
                 :normalized-request (:params validated)
                 :normalized-request-hash (data-hash (:params validated))
                 :compiled-transaction mcp-spec
                 :compiled-transaction-hash (data-hash mcp-spec)
                 :compiled-intent mcp-compiled
                 :compiled-intent-hash (data-hash mcp-compiled))
     :equivalence {:correct (and (:ok validated)
                                 (:ok cli-compiled)
                                 (:ok mcp-compiled))
                   :transaction-edn-equal (= (:parsed-request cli) mcp-spec)
                   :compiled-intent-equal (= cli-compiled mcp-compiled)
                   :future-hashes-equal
                   (= (mapv #(select-keys % [:file :source-hash :result-hash])
                            (:files cli-compiled))
                      (mapv #(select-keys % [:file :source-hash :result-hash])
                            (:files mcp-compiled)))}}))

(defn corrected-selector-inputs
  [owner]
  {:cli (assoc-in selector-cli-spec [:reads 0 :forms] [(symbol owner)])
   :mcp (assoc-in selector-mcp-request ["requests" 0 "forms"] [owner])})

(defn selector-differential
  []
  (let [cli (cli-stdin-envelope :cat selector-cli-spec)
        cli-refusal (first (cli-read-outcomes selector-sources
                                              (:parsed-request cli)))
        mcp (mcp-json-envelope selector-mcp-request)
        validated (mcp-inspect/validate-inspect-params (:transmitted-request mcp))
        mcp-refusal (mcp-inspect/evaluate-snapshots
                      (:params validated) (snapshot-map selector-sources))
        cli-facts (normalized-selector-facts cli-refusal)
        mcp-facts (normalized-selector-facts mcp-refusal)
        owner (get-in cli-facts [:selection-failures 0 :hypotheses 0 :owner])
        corrected (corrected-selector-inputs owner)
        cli-retry (cli-stdin-envelope :cat (:cli corrected))
        cli-success (cli-read-outcomes selector-sources
                                       (:parsed-request cli-retry))
        mcp-retry (mcp-json-envelope (:mcp corrected))
        retry-validated
        (mcp-inspect/validate-inspect-params (:transmitted-request mcp-retry))
        mcp-success (mcp-inspect/evaluate-snapshots
                      (:params retry-validated) (snapshot-map selector-sources))
        cli-success-facts (normalized-read-forms cli-success)
        mcp-success-facts (normalized-mcp-read-forms mcp-success)]
    {:stratum :exact-selector-refusal-retry
     :cli (assoc cli
                 :kernel-refusal cli-refusal
                 :kernel-refusal-hash (data-hash cli-refusal)
                 :retry cli-retry
                 :retry-outcome cli-success
                 :retry-outcome-hash (data-hash cli-success))
     :mcp (assoc mcp
                 :normalized-request (:params validated)
                 :normalized-request-hash (data-hash (:params validated))
                 :kernel-refusal mcp-refusal
                 :kernel-refusal-hash (data-hash mcp-refusal)
                 :retry (assoc mcp-retry :normalized-request
                               (:params retry-validated))
                 :retry-outcome mcp-success
                 :retry-outcome-hash (data-hash mcp-success))
     :equivalence {:correct (and (:ok validated)
                                 (:ok retry-validated)
                                 (:ok mcp-success))
                   :owner-hypothesis-facts-equal (= cli-facts mcp-facts)
                   :selected-owner owner
                   :selection-authority false
                   :retry-semantic-facts-equal
                   (= cli-success-facts mcp-success-facts)
                   :cli-facts-hash (data-hash cli-facts)
                   :mcp-facts-hash (data-hash mcp-facts)}}))

(defn report
  []
  (let [strata [(read-differential)
                (change-differential)
                (selector-differential)]]
    {:schema :clj-surgeon.cli-mcp-transport-differential/v1
     :candidate-commit candidate-commit
     :model-calls 0
     :analyzer-launches 0
     :strata strata
     :all-correct (every? #(get-in % [:equivalence :correct]) strata)
     :all-semantic-parity
     (every? true?
             [(get-in (nth strata 0) [:equivalence :semantic-facts-equal])
              (get-in (nth strata 1) [:equivalence :transaction-edn-equal])
              (get-in (nth strata 1) [:equivalence :compiled-intent-equal])
              (get-in (nth strata 2) [:equivalence :owner-hypothesis-facts-equal])
              (get-in (nth strata 2) [:equivalence :retry-semantic-facts-equal])])}))

(defn -main
  [& _args]
  (prn (report)))
