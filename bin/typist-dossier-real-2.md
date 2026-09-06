# Mission dossier — real-1 (FROZEN; identical bytes to every arm)

You are editing a real Clojure project (`clj-surgeon`). Make ONE bounded
mechanical change to ONE file and emit ONLY a unified diff.

## Intended change

In `src/clj_surgeon/diagnostic_delta.clj`, rename two things and every call
site of each:

1. The public `finding-identity` becomes **`finding-fingerprint`** — the
   definition and all of its call sites. After the change the name
   `finding-identity` must not appear anywhere in the file.

2. The private helper `field` becomes **`finding-field`** — the definition and
   all four of its call sites. After the change no `(defn- field` remains.

Nothing else changes: no behaviour, no argument order, no return value, no
docstring wording, no other file.

## TWO SPANS THAT MUST NOT CHANGE

The word `identity` appears twice in this file in places that are **not**
references to the function, and both must stay **byte-identical**:

- inside `representative-difference` there is a **local `let` binding named
  `identity`**; the line `(get remaining identity 0)` and the binding itself
  keep that name — it is a local, not the function;
- the docstring of `finding-identity` uses the word as a **concept**: the
  sentence `Return the location-independent identity used for diagnostic
  multiset deltas.` stays exactly as written.

A blind search-and-replace of `identity` fails this mission.

## The file, exactly as it is now (you may change ONLY this file)

### src/clj_surgeon/diagnostic_delta.clj

```clojure
(ns clj-surgeon.diagnostic-delta
  "Pure comparison of verifier diagnostics across one structural transaction."
  (:require
   [clojure.string :as str]))

(def blocking-levels
  #{:warning :error})

(defn- field
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

(defn finding-identity
  "Return the location-independent identity used for diagnostic multiset deltas.

  Row and column are deliberately excluded: an unrelated edit can move an
  existing finding without introducing it. Multiplicity remains significant."
  [finding]
  {:filename (normalize-filename (field finding :filename))
   :type (some-> (field finding :type) keyword)
   :level (some-> (field finding :level) keyword)
   :message (field finding :message)})

(defn- valid-finding?
  [finding]
  (and (map? finding)
       (string? (:filename (finding-identity finding)))
       (keyword? (:type (finding-identity finding)))
       (keyword? (:level (finding-identity finding)))
       (string? (:message (finding-identity finding)))))

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
  (let [right-counts (frequencies (map finding-identity right))]
    (:selected
      (reduce
        (fn [{:keys [remaining] :as state} finding]
          (let [identity (finding-identity finding)
                count-left (get remaining identity 0)]
            (if (pos? count-left)
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
                                          (:level (finding-identity %)))
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
```

### test/clj_surgeon/diagnostic_delta_test.clj (DO NOT CHANGE — this is the proof gate)

The real test namespace for this file, already updated to call
`delta/finding-fingerprint`. It is red until your change lands. Editing it
fails the mission.

## Proof gate

Your diff is applied with `git apply` and then this command must exit 0 in the
project root:

```
java -cp "src:test:$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main -e "(require 'clojure.test 'clj-surgeon.diagnostic-delta-test) (let [r (apply clojure.test/run-tests '(clj-surgeon.diagnostic-delta-test))] (System/exit (if (and (zero? (:fail r)) (zero? (:error r))) 0 1)))"
```

An independent behavioural check then confirms the end state directly — that
`finding-fingerprint` and `finding-field` resolve and `finding-identity` and
`field` do not, that the delta results are unchanged, and that both
do-not-change spans above are still present — and that every file other than
`src/clj_surgeon/diagnostic_delta.clj` is byte-identical to the preimage.
Touching `deps.edn`, `.clj-surgeon.edn` or the test file fails the mission.

## THE RULE

Emit **ONLY** a unified diff against the path shown above, using `a/` and `b/`
prefixes (the output of `git diff`). No prose, no explanation, no markdown
fences, no commentary before or after. Nothing but the diff.
