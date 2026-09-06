(ns clj-surgeon.mission-typist-test
  {:lane :fast}
  (:require
   [clj-surgeon.mission-typist :as typist]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def eligible
  {:enabled? true
   :mission-class :rename
   :intent "Rename the selected binding"
   :discovery-complete? true
   :owners [{:file "src/a.clj" :owner "run" :start 0 :end 16}]
   :sources {"src/a.clj" "(defn run [] 1)\n"}
   :source-policy {"src/a.clj" {:generated? false :reader-conditionals? false
                                :format-sensitive? false}}
   :gate {:id "unit" :measured-ms 10 :evidence "receipt:unit"}
   :acceptance {:id "independent-behavior" :evidence "receipt:witness"}
   :commit {:atomic? true :rollback? true}
   :budget {:max-files 1 :max-changed-chars 128}
   :provider {:id :openrouter :model "openai/gpt-oss-120b" :upstream "Cerebras"}
   :rate {:mission-class :rename :provider :openrouter :upstream "Cerebras"
          :model "openai/gpt-oss-120b" :verified 9 :attempted 10
          :evidence "cohort:rename"}})

(deftest admission-and-rate-boundaries
  (is (= {:ok true :executor :native} (typist/route {:enabled? false})))
  (doseq [[verified expected] [[0 5] [69 5] [70 5] [71 3] [84 3] [85 1] [100 1]]]
    (let [r (typist/route (assoc eligible :rate (assoc (:rate eligible)
                                                  :verified verified :attempted 100)))]
      (is (:ok r))
      (is (= expected (:k r)))))
  (is (= :typist (:executor (typist/route eligible)))))

(deftest admission-refusals
  (doseq [[label change] [[:missing-intent #(dissoc % :intent)]
                          [:unknown-class #(assoc % :mission-class :architecture)]
                          [:discovery #(assoc % :discovery-complete? false)]
                          [:empty-owners #(assoc % :owners [])]
                          [:missing-source #(assoc % :sources {})]
                          [:unsafe-path #(assoc-in % [:owners 0 :file] "../a.clj")]
                          [:absolute-path #(assoc-in % [:owners 0 :file] "/src/a.clj")]
                          [:empty-span #(assoc-in % [:owners 0 :end] 0)]
                          [:past-end #(assoc-in % [:owners 0 :end] 999)]
                          [:unknown-policy #(assoc % :source-policy {})]
                          [:generated #(assoc-in % [:source-policy "src/a.clj" :generated?] true)]
                          [:conditional #(assoc-in % [:source-policy "src/a.clj" :reader-conditionals?] true)]
                          [:format-sensitive #(assoc-in % [:source-policy "src/a.clj" :format-sensitive?] true)]
                          [:slow-gate #(assoc-in % [:gate :measured-ms] 5000)]
                          [:no-measurement #(update % :gate dissoc :evidence)]
                          [:negative-ms #(assoc-in % [:gate :measured-ms] -1)]
                          [:no-witness #(dissoc % :acceptance)]
                          [:same-witness #(assoc % :acceptance (:gate %))]
                          [:not-atomic #(assoc-in % [:commit :atomic?] false)]
                          [:no-rollback #(assoc-in % [:commit :rollback?] false)]
                          [:no-budget #(dissoc % :budget)]
                          [:zero-budget #(assoc-in % [:budget :max-changed-chars] 0)]
                          [:unknown-rate #(dissoc % :rate)]
                          [:wrong-class #(assoc-in % [:rate :mission-class] :move)]
                          [:wrong-provider #(assoc-in % [:rate :provider] :groq)]
                          [:wrong-upstream #(assoc-in % [:rate :upstream] "Other")]
                          [:wrong-model #(assoc-in % [:rate :model] "other")]
                          [:impossible-rate #(assoc-in % [:rate :verified] 11)]
                          [:empty-rate #(assoc-in % [:rate :attempted] 0)]
                          [:unpinned #(update % :provider dissoc :upstream)]]]
    (testing (name label)
      (let [r (typist/route (change eligible))]
        (is (false? (:ok r)))
        (is (= :typist-route-refused (:error-type r)))
        (is (keyword? (:condition r)))
        (is (string? (:decision r)))))))

(deftest dossier-is-frozen-source-evidence
  (let [raw (typist/dossier (assoc eligible :candidate-format :clojure-forms))]
    (is (= :clojure-forms (get-in raw [:dossier :candidate-format])))
    (is (re-find #"plain Clojure" (:prompt raw)))
    (is (not= (:dossier-hash raw) (:dossier-hash (typist/dossier eligible)))))
  (is (= :candidate-format (:condition (typist/route (assoc eligible :candidate-format :unknown)))))
  (is (= :candidate-format
         (:condition (typist/route (-> eligible
                                     (assoc :candidate-format :clojure-forms)
                                     (assoc-in [:sources "src/other.clj"] "(def other 1)"))))))
  (let [d (typist/dossier eligible)]
    (is (:ok d))
    (is (= "(defn run [] 1)\n" (get-in d [:dossier :owners 0 :source])))
    (is (= 64 (count (:dossier-hash d))))
    (is (= (:dossier-hash d) (:dossier-hash (typist/dossier eligible))))
    (is (not= (:dossier-hash d)
              (:dossier-hash (typist/dossier (assoc eligible :intent "Different intent")))))))

(deftest dossier-never-forwards-unapproved-metadata
  (doseq [path [[:owners 0 :secret] [:owners 0 "secret"] [:provider :key]
                [:rate :key] [:gate :key] [:acceptance :key] [:budget :key]]]
    (let [result (typist/dossier (assoc-in eligible path "SENTINEL-SECRET"))]
      (is (:ok result))
      (is (not (str/includes? (:prompt result) "SENTINEL-SECRET"))))))

(deftest dossier-bounds-before-projection
  (doseq [facts [(assoc eligible :intent (apply str (repeat 16385 "x")))
                 (assoc eligible :owners (vec (repeat 257 (first (:owners eligible)))))
                 (assoc-in eligible [:sources "src/a.clj"] (apply str (repeat 262145 "x")))
                 (assoc-in eligible [:owners 0 :new-owner] {:bad :authority})]]
    (is (false? (:ok (typist/dossier facts)))))
  (let [r (typist/dossier (assoc-in eligible [:owners 0 :new-owner] "renamed"))]
    (is (= :owner-forms (get-in r [:dossier :candidate-format])))
    (is (= "renamed" (get-in r [:dossier :owners 0 :new-owner])))))

(deftest generation-policy-is-frozen
  (let [default (typist/route eligible)
        requested (assoc eligible :max-tokens 4096 :fallback {:provider :groq :max-tokens 4096})
        route (typist/route requested)]
    (is (= {:max-tokens 8192 :timeout-s 30} (:generation default)))
    (is (= {:max-tokens 4096 :timeout-s 30 :fallback {:provider :groq :max-tokens 4096}}
           (:generation route)))
    (is (= (:k default) (:k route)))
    (is (= (:generation route) (get-in (typist/dossier requested) [:dossier :route :generation])))
    (is (= (:generation route) (:generation (typist/route (assoc requested :unrelated-existing-field :ignored))))))
  (doseq [change [{:max-tokens nil} {:max-tokens true} {:max-tokens 0} {:max-tokens 8193}
                  {:max-tokens 1.5} {:max-tokens "4096"} {:fallback nil}
                  {:fallback {}} {:fallback {:provider :groq :max-tokens 1}}
                  {:max-tokens 4096 :fallback {:provider :groq :max-tokens 4097}}
                  {:max-tokens 4096 :fallback {:provider :other :max-tokens 1}}
                  {:max-tokens 4096 :fallback {:provider :groq :max-tokens false}}
                  {:max-tokens 4096 :fallback {:provider :groq :max-tokens 1 :extra true}}]]
    (let [r (typist/route (merge eligible change))]
      (is (false? (:ok r)))
      (is (= :generation-budget (:condition r)))))
  (let [groq (-> eligible
                 (assoc :provider {:id :groq :model "openai/gpt-oss-120b" :upstream "Groq"})
                 (assoc-in [:rate :provider] :groq)
                 (assoc-in [:rate :upstream] "Groq"))]
    (is (:ok (typist/route groq)))
    (is (= :generation-budget
           (:condition (typist/route (assoc groq :max-tokens 4096 :fallback {:provider :groq :max-tokens 4096})))))))
