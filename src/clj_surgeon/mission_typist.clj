(ns clj-surgeon.mission-typist
  "Pure typist admission and source dossier projection. No I/O or write authority.
   Inputs are planner-owned facts; the CLI boundary must establish their provenance."
  (:require
   [clj-surgeon.mission :as mission]
   [clojure.string :as str]))

(def mechanical-classes #{:rename :thread-parameter :move-helpers :fanout :witness})

(defn nonblank? [s] (and (string? s) (<= (count s) 16384) (not (str/blank? s))))

(defn relative-source? [s]
  (and (nonblank? s)
       (not (str/starts-with? s "/"))
       (not (re-find #"[\\:\u0000]" s))
       (not-any? #{"" "." ".."} (str/split s #"/"))
       (str/ends-with? s ".clj")))

(defn owner-valid? [sources {:keys [file owner new-owner start end]}]
  (let [source (get sources file)]
    (and (relative-source? file) (nonblank? owner)
         (or (nil? new-owner) (nonblank? new-owner)) (string? source)
         (integer? start) (integer? end) (<= 0 start) (< start end)
         (<= end (count source)))))

(defn source-admitted? [policy file]
  (let [facts (get policy file)]
    (and (map? facts)
         (every? #(false? (get facts %))
                 [:generated? :reader-conditionals? :format-sensitive?]))))

(defn candidate-count [{:keys [verified attempted]}]
  ;; Integer comparisons avoid floating-point drift exactly at 70 and 85 percent.
  (cond (>= (* 100 verified) (* 85 attempted)) 1
        (<= (* 100 verified) (* 70 attempted)) 5
        :else 3))

(defn provider-admitted? [{:keys [id model upstream]}]
  (or (and (= id :openrouter) (= model "openai/gpt-oss-120b") (= upstream "Cerebras"))
      (and (= id :groq) (= model "openai/gpt-oss-120b") (= upstream "Groq"))
      (and (= id :spark) (= model "gpt-5.3-codex-spark") (= upstream "OpenAI"))))

(defn rate-valid? [class provider {:keys [verified attempted evidence] :as rate}]
  (and (integer? verified) (integer? attempted) (pos? attempted)
       (<= 0 verified attempted) (nonblank? evidence)
       (= class (:mission-class rate))
       (= (:id provider) (:provider rate))
       (= (:model provider) (:model rate))
       (= (:upstream provider) (:upstream rate))))

(defn generation-policy
  "Validate only explicit generation fields; project immutable transport authority."
  [facts]
  (let [max-tokens (get facts :max-tokens 8192)
        fallback? (contains? facts :fallback)
        fallback (:fallback facts)]
    (when (and (integer? max-tokens) (<= 1 max-tokens 8192)
               (or (not fallback?)
                   (and (= :openrouter (get-in facts [:provider :id]))
                        (map? fallback) (= #{:provider :max-tokens} (set (keys fallback)))
                        (= :groq (:provider fallback))
                        (integer? (:max-tokens fallback)) (<= 1 (:max-tokens fallback) 8192)
                        (<= (+ max-tokens (:max-tokens fallback)) 8192))))
      (cond-> {:max-tokens max-tokens :timeout-s 30}
        fallback? (assoc :fallback (select-keys fallback [:provider :max-tokens]))))))

(defn failed-condition
  [{:keys [mission-class intent discovery-complete? owners sources source-policy
           gate acceptance commit budget provider rate candidate-format] :as facts}]
  (cond
    (not (contains? #{nil :owner-forms :clojure-forms} candidate-format))
    [:candidate-format "Select owner-forms JSON or single-file plain Clojure definitions."]
    (and (= :clojure-forms candidate-format) (not= 1 (count sources)))
    [:candidate-format "Plain Clojure definitions require exactly one target file."]
    (not (contains? mechanical-classes mission-class))
    [:mechanical-class "Choose a supported mechanical mission class."]
    (not (and (true? discovery-complete?) (nonblank? intent)
              (vector? owners) (seq owners) (<= (count owners) 256)
              (map? sources) (<= (count sources) 64)
              (every? #(and (string? %) (<= (count %) 262144)) (vals sources))
              (<= (reduce + (map count (vals sources))) 4194304)
              (every? #(owner-valid? sources %) owners)
              (<= (reduce + (map #(- (:end %) (:start %)) owners)) 131072)))
    [:complete-dossier "Complete discovery and supply valid named source spans."]
    (not (every? #(source-admitted? source-policy (:file %)) owners))
    [:source-policy "Establish plain, non-generated, formatting-safe source authority."]
    (not (and (nonblank? (:id gate)) (nonblank? (:evidence gate))
              (number? (:measured-ms gate)) (<= 0 (:measured-ms gate))
              (< (:measured-ms gate) 5000)))
    [:cheap-gate "Supply measured proof below 5000 ms with a retained receipt."]
    (not (and (nonblank? (:id acceptance)) (nonblank? (:evidence acceptance))
              (not= (:id gate) (:id acceptance))
              (not= (:evidence gate) (:evidence acceptance))))
    [:independent-acceptance "Supply a distinct independent acceptance witness."]
    (not (and (true? (:atomic? commit)) (true? (:rollback? commit))))
    [:guarded-commit "Establish atomic commit and rollback authority."]
    (not (and (integer? (:max-files budget)) (pos? (:max-files budget))
              (<= (count (set (map :file owners))) (:max-files budget))
              (integer? (:max-changed-chars budget)) (pos? (:max-changed-chars budget))))
    [:bounded-scope "Declare a positive file and changed-character budget."]
    (not (provider-admitted? provider))
    [:pinned-provider "Select an admitted pinned model and upstream."]
    (nil? (generation-policy facts))
    [:generation-budget "Choose a positive primary token allocation; optional Groq fallback requires an exact provider/token map, OpenRouter primary, and at most 8192 reserved output tokens total."]
    (not (rate-valid? mission-class provider rate))
    [:verified-rate "Supply measured verified/attempted counts for this class and provider."]
    :else nil))

(defn route [facts]
  (if-not (true? (:enabled? facts))
    {:ok true :executor :native}
    (if-let [[condition decision] (failed-condition facts)]
      {:ok false :executor :typist :error-type :typist-route-refused
       :condition condition :decision decision :mutation-attempted false}
      {:ok true :executor :typist :k (candidate-count (:rate facts))
       :candidate-format (or (:candidate-format facts) :owner-forms)
       :generation (generation-policy facts)
       :provider (select-keys (:provider facts) [:id :model :upstream])
       :mission-class (:mission-class facts)
       :rate (select-keys (:rate facts) [:verified :attempted :mission-class :provider
                                         :model :upstream :evidence])
       :gate (select-keys (:gate facts) [:id :measured-ms :evidence])
       :acceptance (select-keys (:acceptance facts) [:id :evidence])
       :budget (select-keys (:budget facts) [:max-files :max-changed-chars])})))

(defn canonical-data [x]
  (cond (map? x) (into (sorted-map) (map (fn [[k v]] [k (canonical-data v)])) x)
        (vector? x) (mapv canonical-data x)
        :else x))

(defn dossier [facts]
  (let [decision (route facts)]
    (if (or (not (:ok decision)) (= :native (:executor decision)))
      decision
      (let [owners (mapv (fn [{:keys [file start end] :as owner}]
                           (let [source (get-in facts [:sources file])]
                             (assoc (select-keys owner [:file :owner :new-owner :start :end])
                                    :source (subs source start end)
                                    :file-sha256 (mission/sha256 source))))
                         (:owners facts))
            format (:candidate-format decision)
            frozen (canonical-data {:schema 2 :candidate-format format :intent (:intent facts)
                                    :owners owners :route decision})
            serialized (pr-str frozen)]
        {:ok true :dossier frozen :dossier-hash (mission/sha256 serialized)
         :prompt (str "Complete only the mechanical change described by this frozen dossier.\n"
                      (if (= :clojure-forms format)
                        "Return only plain Clojure definitions, separated by real newlines. Emit exactly one complete definition for every frozen owner. No JSON, no encoded source strings, no markdown fences.\n"
                        (str "Return only a JSON array of objects with exactly file, owner, form string fields.\n"
                             "owner is the ORIGINAL frozen owner name; form is one complete replacement definition.\n"))
                      "Use new-owner when declared as the definition name; otherwise keep the name.\n"
                      "Do not emit old context, offsets, markdown, or prose. No discovery, shell, or write authority.\n"
                      serialized)}))))
