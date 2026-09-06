(ns clj-surgeon.diagnostic-delta
  "Pure comparison of verifier diagnostics across one structural transaction."
  (:require
   [clojure.string :as str]))

(def blocking-levels
  #{:warning :error})

(defn- finding-field
  [finding key]
  (or (get finding key)
      (get finding (name key))))

(defn normalize-filename
  "Return one stable project-relative spelling for a diagnostic filename."
  [filename]
  (some-> filename
          str
          (str/replace "\\" "/")
          (str/replace #"^\./+" "")))

(defn finding-fingerprint
  "Return the location-independent identity used for diagnostic multiset deltas.

  Row and column are deliberately excluded: an unrelated edit can move an
  existing finding without introducing it. Multiplicity remains significant."
  [finding]
  {:filename (normalize-filename (finding-field finding :filename))
   :type (some-> (finding-field finding :type) keyword)
   :level (some-> (finding-field finding :level) keyword)
   :message (finding-field finding :message)})

(defn- valid-finding?
  [finding]
  (and (map? finding)
       (string? (:filename (finding-fingerprint finding)))
       (keyword? (:type (finding-fingerprint finding)))
       (keyword? (:level (finding-fingerprint finding)))
       (string? (:message (finding-fingerprint finding)))))

(defn- findings
  [snapshot]
  (cond
    (and (map? snapshot) (vector? (:findings snapshot)))
    (:findings snapshot)

    (vector? snapshot)
    snapshot

    :else
    ::invalid))

(defn- representative-difference
  [left right]
  (let [right-counts (frequencies (map finding-fingerprint right))]
    (:selected
      (reduce
        (fn [{:keys [remaining] :as state} finding]
          (let [identity (finding-fingerprint finding)
                remaining-right-count (get remaining identity 0)]
            (if (pos? remaining-right-count)
              (assoc state :remaining (update remaining identity dec))
              (update state :selected conj finding))))
        {:remaining right-counts :selected []}
        left))))

(defn diagnostic-delta
  "Compare baseline and future diagnostic snapshots as location-independent
  multisets. Returns stable refusal data for malformed snapshots."
  [baseline future]
  (let [baseline-findings (findings baseline)
        future-findings (findings future)]
    (if-not (and (vector? baseline-findings)
                 (vector? future-findings)
                 (every? valid-finding? baseline-findings)
                 (every? valid-finding? future-findings))
      {:ok false
       :error-type :invalid-diagnostic-snapshot
       :error "Diagnostic snapshots must contain a vector of complete findings"}
      (let [introduced (representative-difference future-findings baseline-findings)
            removed (representative-difference baseline-findings future-findings)
            blocking (filterv #(contains? blocking-levels
                                          (:level (finding-fingerprint %)))
                              introduced)]
        {:ok (empty? blocking)
         :baseline-count (count baseline-findings)
         :future-count (count future-findings)
         :introduced-count (count introduced)
         :removed-count (count removed)
         :unchanged-count (- (count future-findings) (count introduced))
         :blocking-introduced-count (count blocking)
         :introduced introduced
         :removed removed
         :blocking-introduced blocking}))))
