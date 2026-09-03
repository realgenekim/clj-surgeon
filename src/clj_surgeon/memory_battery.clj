(ns clj-surgeon.memory-battery
  "Pass lines, verdict, and report rendering for the tree-scale memory battery.

  PURE. No JVM instrumentation, no I/O, no heavy requires — this namespace
  loads under babashka so its witness test can run in the millisecond fast
  suite. The instrumented runner that produces the cells lives in
  `clj-surgeon.memory-battery-runner`.

  The pass lines are the ones ratified in
  `docs/observations/2026-09-03-memory-design-sol-answer.md` section 8, and
  they exist in exactly one place: `pass-lines` below."
  (:require
   [clojure.string :as str]))

;; ============================================================
;; The constants — one place, referenced by runner, report, and test
;; ============================================================

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(def pass-lines
  "Every numeric pass line for the memory battery, adopted verbatim from Sol's
  measured design (2026-09-03, answer 2).

  - :reserved-peak-mb        attributable reserved (admission-accounted) peak
                             ceiling; UNMEASURED until an admission accountant
                             exists, never silently passed
  - :peak-headroom-mb        TREND. Process-wide sampled peak used heap is
                             reported against the pre-op used heap plus this
                             much...
  - :peak-xmx-percent        ...or this percent of -Xmx, whichever is TIGHTER.
                             At 512m with a JVM starting near 24 MiB used, the
                             start+headroom term binds at about 248 MiB — NOT
                             the 409.6 MiB that 80% of -Xmx would allow.
  - :scale-peak-slack-mb     TREND. Peak at :scale-large-n is reported against
                             peak at :scale-small-n plus this much
  - :scale-retained-slack-mb persistent growth (after-GC heap once the result is
                             released, minus start) at :scale-large-n may exceed
                             the same at :scale-small-n by at most this much
  - :scale-held-slack-mb     RESULT retention (after-GC heap while the result is
                             still referenced) at :scale-large-n may exceed the
                             same at :scale-small-n by at most this much. Sol's
                             exact line:
                               max(held_mb at N=10,000)
                                 <= max(held_mb at N=1,000) + 2.0 MiB
                             2.0 is derived from measurement, not taste: twice
                             the full-match arm's 1.0 MiB held at N=1,000 and ten
                             times the largest 0.2 MiB jitter seen in a bounded
                             arm — wide enough not to fire on noise, tight enough
                             to catch the measured 1.0 -> 9.8 MiB growth.

  Two further lines from Sol's set are NOT implemented by this battery and are
  recorded as boundaries on MCP-OP-MEM-011 rather than as constants here: the
  450 x 1.9 MiB aggregate-admission case, and injected-conflict rollback. See
  docs/memory-battery.md."
  {:reserved-peak-mb        192
   :peak-headroom-mb        224
   :peak-xmx-percent        80
   :scale-peak-slack-mb     32
   :scale-retained-slack-mb 8
   :scale-held-slack-mb     2.0
   :scale-small-n           1000
   :scale-large-n           10000})

(def exit-codes
  "Process exit contract for `make memory-battery`.

  Three TERMINAL states, not two. `:incomplete` exists because an all-green run
  that never observed a line is not a pass: exit 0 would let a release gate
  through on lines nobody measured. It is distinct from `:fail` because nothing
  measured actually broke — the remedy is to measure, not to fix."
  {:pass         0
   :fail         1
   :refusal      2
   :tool-failure 3
   :incomplete   4})

(def tree-scales
  "The file counts the battery builds and measures, for the default profile."
  [100 1000 10000])

(defn tree-dir-name
  "Directory under the corpus root for one (profile, N). The default profile
  keeps its bare `<N>` name. MUST agree with the generator's function of the
  same name; the self-test asserts that for every arm."
  [profile n]
  (if (= :default profile) (str n) (str (name profile) "-" n)))

;; @spec MCP-OP-MEM-011
(def extra-corpus-arms
  "Adversarial corpus shapes measured ALONGSIDE the default trees, each as its
  own arm rather than mixed into them: an operation can be bounded over 10,000
  ordinary files and unbounded over one pathological file, and averaging the
  two hides exactly the case worth measuring.

  Deliberately small: these are the shapes Sol called cheap. The expensive ones
  he named stay documented boundaries on MCP-OP-MEM-011 rather than arms — a
  17 KiB-mean profile (roughly 4x the 10,000-file battery's weight) and
  450 x 1.9 MiB (~855 MiB of source, which only becomes cheap once aggregate
  admission exists and parsing never starts).

  MUST agree with the generator's `profile-arms`; the self-test asserts that."
  [[:cljc 100] [:giant 1] [:nested 1]])

(defn corpus-arms
  "Every [profile n] the battery measures, default scales first."
  [scales]
  (into (vec (for [n (sort scales)] [:default n])) extra-corpus-arms))

;; ============================================================
;; Pass lines as pure predicates
;; ============================================================

;; @spec MCP-OP-MEM-001
(defn peak-budget-mb
  "The tighter of `heap-start-mb + :peak-headroom-mb` and
  `:peak-xmx-percent` of `xmx-mb`. Returns a double."
  [heap-start-mb xmx-mb]
  (double
    (min (+ (double heap-start-mb) (double (:peak-headroom-mb pass-lines)))
         (/ (* (:peak-xmx-percent pass-lines) (double xmx-mb)) 100.0))))

(def trend-lines
  "Lines REPORTED on every run but never gated.

  `peak_mb` is an honest measurement and a poor requirement: it is a 5 ms
  sampled, process-wide used-heap peak that contains garbage, and G1 moves it
  with `-Xmx` and collector scheduling. Re-running an identical cell
  (cli-ls-tree, N=1,000, fresh) moved it 274.8 -> 246.5 MB — a 28.3 MB swing
  that crossed the verdict line on work that had not changed at all. A gate that
  flips on a rerun of the same work is a flaky gate, and a flaky gate is
  eventually disabled, taking the honest signal with it.

  So these two are trend/regression signals under identical JVM, collector and
  heap settings. The HARD lines are the ones whose instruments do not drift:
  `:oom`, output parity against the attested reference, the attributable
  reserved peak once an admission accountant exists, `held_mb` across N, and
  persistent growth across N."
  #{:peak-over-budget :peak-scales-with-n})

(defn- cell-identity
  "What a reported line is ABOUT. The corpus profile is part of it: the giant
  and nested arms both sit at N=1, so without it two different findings print
  as if they were the same one."
  [cell]
  (assoc (select-keys cell [:op :n :phase :rep])
         :profile (:profile cell :default)))

(defn- cell-failures
  [xmx-mb {:keys [oom? heap-start-mb heap-used-peak-mb result-hash reference-hash]
           :as cell}]
  (let [budget (peak-budget-mb (or heap-start-mb 0) xmx-mb)]
    (cond-> []
      oom?
      (conj (merge (cell-identity cell)
                   {:line :oom
                    :detail "the operation exhausted the configured heap"}))

      (and (some? heap-used-peak-mb) (> (double heap-used-peak-mb) budget))
      (conj (merge (cell-identity cell)
                   {:line :peak-over-budget
                    :observed (double heap-used-peak-mb)
                    :limit budget}))

      (and (some? reference-hash) (not= result-hash reference-hash))
      (conj (merge (cell-identity cell)
                   {:line :reference-mismatch
                    :observed result-hash
                    :limit reference-hash})))))

(defn- worst
  "Largest value of `k` across cells, or nil when there are none."
  [cells k]
  (when-let [vs (seq (keep k cells))]
    (double (apply max (map double vs)))))

(defn- scale-check
  "One cross-N pass line. Returns [:pass], [:fail m] or [:unmeasured m]."
  [op line field slack small-cells large-cells]
  (let [small (worst small-cells field)
        large (worst large-cells field)]
    (cond
      (or (nil? small) (nil? large))
      [:unmeasured {:op op
                    :line line
                    :detail (str "no measured cells at N="
                                 (if (nil? small)
                                   (:scale-small-n pass-lines)
                                   (:scale-large-n pass-lines)))}]

      (> large (+ small (double slack)))
      [:fail {:op op
              :profile :default
              :line line
              :observed large
              :limit (+ small (double slack))
              :small-n-observed small
              :slack-mb slack}]

      :else [:pass])))

(defn- reserved-check
  "Sol's `reserved_peak <= 192 MiB` line. Reserved peak is the admission
  accountant's attributable figure, which is a different quantity from the
  process-wide sampled peak. Until an accountant exists this line is
  UNMEASURED, never silently passed."
  [op op-cells]
  (let [observed (worst op-cells :heap-reserved-peak-mb)
        limit (double (:reserved-peak-mb pass-lines))]
    (cond
      (nil? observed)
      [:unmeasured {:op op
                    :line :reserved-peak-over-budget
                    :detail (str "no operation on this branch reports an "
                                 "attributable reserved peak; the sampled "
                                 "process-wide peak is not a substitute")}]

      (> observed limit)
      [:fail {:op op :line :reserved-peak-over-budget
              :observed observed :limit limit}]

      :else [:pass])))

(defn- reference-check
  "Output parity is only a pass line if an unbounded reference hash actually
  exists to compare against. A missing reference is UNMEASURED, never a pass."
  [op op-cells]
  (let [without (filter #(nil? (:reference-hash %)) op-cells)]
    (if (seq without)
      [:unmeasured {:op op
                    :line :reference-mismatch
                    :detail (str "no unbounded reference hash for N="
                                 (str/join "," (sort (map :n without)))
                                 "; run the reference pass first")}]
      [:pass])))

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(defn verdict
  "Apply every pass line to a battery observation.

  Input:  {:xmx-mb <int> :cells [cell ...]}
  A cell: {:op :n :phase :rep :wall-ms :files :bytes
           :heap-start-mb :heap-used-peak-mb :heap-after-gc-mb
           :heap-reserved-peak-mb :heap-result-retained-mb
           :heap-held-after-release-mb :heap-after-release-start-mb
           :oom? :result-hash :reference-hash}

  Output: {:status #{:pass :fail :incomplete}
           :pass? bool :complete? bool :exit int
           :failures [...] :trends [...] :unmeasured [...]}

  `:trends` carries the lines in `trend-lines`: measured, reported, and never
  part of the terminal state. See that var for why the peak lines are there.

  `:complete?` is false when a line could not be evaluated (for example the
  10,000-file case was skipped for wall-clock reasons, or no admission
  accountant reports a reserved peak). An unmeasured line is never counted as
  a pass: with no failures and something unobserved the terminal state is
  `:incomplete`, `:pass?` is false, and the exit code is the distinct nonzero
  `:incomplete` code so a release gate blocks on it."
  [{:keys [xmx-mb cells]}]
  (let [measured (remove :skipped? cells)
        per-cell (vec (mapcat #(cell-failures xmx-mb %) measured))
        by-op (group-by :op measured)
        small-n (:scale-small-n pass-lines)
        large-n (:scale-large-n pass-lines)
        op-results
        (for [op (sort-by str (keys by-op))
              :let [op-cells (get by-op op)
                    ;; Cross-N lines compare the DEFAULT corpus only. The
                    ;; adversarial arms exist at one size each; comparing a
                    ;; 1.9 MiB single file against 10,000 ordinary ones would
                    ;; be a statement about two different corpora.
                    ;; NB: (#{:default nil} nil) is nil, not truthy — a cell
                    ;; with no :profile key IS the default corpus.
                    default-cells (filter #(= :default (:profile % :default))
                                          op-cells)
                    small (filter #(= small-n (:n %)) default-cells)
                    large (filter #(= large-n (:n %)) default-cells)]
              result
              (into [(reserved-check op op-cells)
                     (reference-check op op-cells)]
                    (for [[line field slack]
                          [[:peak-scales-with-n :heap-used-peak-mb
                            (:scale-peak-slack-mb pass-lines)]
                           ;; What the RESULT costs to hold. This is the line
                           ;; that catches an operation sizing its answer by the
                           ;; repository rather than by the work asked of it.
                           [:held-scales-with-n :heap-result-retained-mb
                            (:scale-held-slack-mb pass-lines)]
                           ;; PERSISTENT growth (after-release minus start),
                           ;; not the absolute post-GC heap: two cells can end
                           ;; at the same used heap while one call left five
                           ;; times as much behind it.
                           [:retained-scales-with-n :heap-after-release-start-mb
                            (:scale-retained-slack-mb pass-lines)]]]
                      (scale-check op line field slack small large)))]
          result)
        op-failures (vec (keep (fn [[status m]]
                                 (when (= :fail status) m))
                               op-results))
        unmeasured (vec (keep (fn [[status m]]
                                (when (= :unmeasured status) m))
                              op-results))
        observed (vec (concat per-cell op-failures))
        failures (vec (remove #(trend-lines (:line %)) observed))
        trends (vec (filter #(trend-lines (:line %)) observed))
        status (cond
                 (seq failures) :fail
                 (seq unmeasured) :incomplete
                 :else :pass)]
    {:status status
     :pass? (= :pass status)
     :complete? (empty? unmeasured)
     :failures failures
     :trends trends
     :unmeasured unmeasured
     :exit (get exit-codes status)}))

;; ============================================================
;; Reference attestation — parity is only a pass line against a reference
;; that measured THIS code, THIS corpus, on THIS JVM
;; ============================================================

(def attested-fields
  "Every field the cached unbounded reference is bound to. A difference in any
  one of them means the cached hashes describe a different experiment.

  `:head-sha` is deliberately NOT here. It is recorded in the reference for
  forensics but not compared: binding parity to HEAD would invalidate the
  reference on every unrelated commit, and the cost of rebuilding it is a
  minutes-long 4 GiB pass. `:src-digest` covers every clj-surgeon source file,
  so any change that could alter an operation's output already invalidates it —
  which is the property parity actually needs."
  [:ops :ops-digest :src-digest :generator-digest :corpus-digests :jvm])

;; @spec MCP-OP-MEM-011
(defn reference-staleness
  "nil when `reference` was produced by exactly this code, generator, corpus and
  JVM; otherwise a typed map naming what differs.

  Before this existed, `make memory-battery` accepted any `reference-hashes.edn`
  that happened to exist under a root shared across worktrees, so output parity
  could be `ok` against hashes from other code over a different corpus."
  [attestation reference]
  (cond
    (nil? reference)
    {:reason :no-reference
     :detail "no unbounded reference has been recorded; run the reference pass"}

    (nil? (:attestation reference))
    {:reason :unattested-reference
     :detail (str "the cached reference carries no attestation, so nothing "
                  "binds it to this code, corpus or JVM; rebuild it")}

    (some #(= :unavailable (get attestation %)) attested-fields)
    {:reason :attestation-unavailable
     :fields (vec (filter #(= :unavailable (get attestation %)) attested-fields))
     :detail "this run could not compute its own identity, so it cannot claim parity"}

    :else
    (let [found (:attestation reference)
          diffs (vec (for [k attested-fields
                           :when (not= (get attestation k) (get found k))]
                       {:field k :expected (get attestation k) :found (get found k)}))]
      (when (seq diffs)
        {:reason :stale-reference
         :fields (mapv :field diffs)
         :differences diffs
         :detail "the cached reference measured a different experiment"}))))

;; ============================================================
;; One-screen table
;; ============================================================

(defn- fmt
  [x width]
  (let [s (cond
            (nil? x) "-"
            (double? x) (format "%.1f" x)
            (float? x) (format "%.1f" (double x))
            :else (str x))]
    (str s (apply str (repeat (max 0 (- width (count s))) \space)))))

(defn- rfmt
  [x width]
  (let [s (cond
            (nil? x) "-"
            (double? x) (format "%.1f" x)
            (float? x) (format "%.1f" (double x))
            :else (str x))]
    (str (apply str (repeat (max 0 (- width (count s))) \space)) s)))

(defn cell-verdict
  "The per-cell verdict shown in the table: :OOM, :DIFF, :trend or :ok.

  `:trend` is deliberately lower-case: it is a reported signal, not a failure,
  and the table should not make a reader think the run broke."
  [xmx-mb cell]
  (let [lines (set (map :line (cell-failures xmx-mb cell)))]
    (cond
      (:skipped? cell) :skipped
      (contains? lines :oom) :OOM
      (contains? lines :reference-mismatch) :DIFF
      (contains? lines :peak-over-budget) :trend
      :else :ok)))

(defn render-table
  "Render the one-screen receipt table. Pure: takes the observation, returns
  a string."
  [{:keys [xmx-mb cells] :as observation}]
  (let [v (verdict observation)
        header (str (fmt "op" 26) (rfmt "prof" 8) (rfmt "N" 7) (rfmt "phase" 7)
                    (rfmt "wall_ms" 9) (rfmt "peak_mb" 9)
                    (rfmt "held_mb" 9) (rfmt "excl_mb" 9)
                    (rfmt "grow_mb" 9) (rfmt "afterGC_mb" 11)
                    (rfmt "files" 7) (rfmt "bytes" 11)
                    (rfmt "OOM?" 6) (rfmt "verdict" 9))
        rows (for [c (sort-by (juxt #(str (:op %))
                                    #(if (= :default (:profile % :default)) 0 1)
                                    #(str (:profile % :default))
                                    :n
                                    #(str (:phase %)))
                              cells)]
               (str (fmt (name (:op c)) 26)
                    (rfmt (name (:profile c :default)) 8)
                    (rfmt (:n c) 7)
                    (rfmt (name (:phase c)) 7)
                    (rfmt (:wall-ms c) 9)
                    (rfmt (:heap-used-peak-mb c) 9)
                    (rfmt (:heap-result-retained-mb c) 9)
                    (rfmt (:heap-held-after-release-mb c) 9)
                    (rfmt (:heap-after-release-start-mb c) 9)
                    (rfmt (:heap-after-gc-mb c) 11)
                    (rfmt (:files c) 7)
                    (rfmt (:bytes c) 11)
                    (rfmt (if (:oom? c) "yes" "no") 6)
                    (rfmt (name (cell-verdict xmx-mb c)) 9)))
        sep (apply str (repeat (count header) \-))]
    (str/join
      "\n"
      (concat
        [(str "memory battery — Xmx " xmx-mb "m, pass lines " (pr-str pass-lines))
         (str "peak_mb = continuously sampled process-wide used-heap PEAK "
              "(not a post-GC delta); held_mb = after-GC used heap while the "
              "result is still referenced, minus start (the receipt's retained "
              "size, INCLUDING any cache or leak the call created); excl_mb = "
              "held minus after-release, the result-EXCLUSIVE retention; "
              "grow_mb = after-release minus start, the PERSISTENT growth the "
              "call left behind (this is the gated leak figure); afterGC_mb = "
              "absolute after-GC used heap once the result is released.")
         (str "TREND lines are reported, never gated: peak_mb is a sampled "
              "process-wide figure that G1 moves by tens of MB on identical "
              "work. HARD lines: oom, reference parity, reserved peak, "
              "held_mb across N, persistent growth across N.")
         sep header sep]
        rows
        [sep
         (str "verdict: "
              (case (:status v)
                :pass "PASS"
                :incomplete "INCOMPLETE"
                "FAIL")
              ;; A run that both failed and left lines unobserved says so.
              (when (and (= :fail (:status v)) (not (:complete? v)))
                " (INCOMPLETE)")
              "   exit " (:exit v))]
        (for [f (:failures v)]
          (str "  FAIL " (name (:line f)) " " (pr-str (dissoc f :line))))
        (for [t (:trends v)]
          (str "  TREND " (name (:line t)) " " (pr-str (dissoc t :line))))
        (for [u (:unmeasured v)]
          (str "  UNMEASURED " (name (:line u)) " " (pr-str (dissoc u :line))))))))

;; ============================================================
;; Makefile gate introspection — the battery must stay OUT of `make test`
;; ============================================================

(defn parse-makefile-targets
  "Parse Makefile text into {target {:prerequisites [..] :recipe [..]}}.

  Recipe lines are tab-indented; a target line is an unindented
  `name: prerequisites`. Pattern rules, `.PHONY`, and variable assignments are
  ignored."
  [text]
  (loop [lines (str/split-lines text)
         current nil
         acc {}]
    (if-let [line (first lines)]
      (cond
        (str/starts-with? line "\t")
        (recur (rest lines) current
               (if current
                 (update-in acc [current :recipe] (fnil conj [])
                            (str/trim line))
                 acc))

        :else
        (if-let [[_ target prereqs]
                 (re-matches #"^([A-Za-z0-9_][A-Za-z0-9_.\-/]*)\s*:{1,2}\s*(.*?)\s*$"
                             line)]
          ;; `X := v` is a variable assignment, not a target.
          (if (or (str/starts-with? target ".") (str/starts-with? prereqs "="))
            (recur (rest lines) nil acc)
            (recur (rest lines) target
                   (assoc acc target
                          {:prerequisites (vec (remove str/blank?
                                                       (str/split prereqs #"\s+")))
                           :recipe (get-in acc [target :recipe] [])})))
          ;; Any other unindented line ends the previous target's recipe.
          (recur (rest lines) nil acc)))
      acc)))

(def ^:private make-invocation
  #"\$\(MAKE\)(?:\s+(?:--no-print-directory|-C\s+\S+|-s))*\s+([A-Za-z0-9_][A-Za-z0-9_.\-/]*)")

(defn- referenced-targets
  [{:keys [prerequisites recipe]}]
  (into (set prerequisites)
        (mapcat #(map second (re-seq make-invocation %)))
        recipe))

(defn target-closure
  "Every make target reachable from `target`, including `target` itself, by
  prerequisite or by an explicit `$(MAKE) ... <target>` recipe line."
  [targets target]
  (loop [frontier #{target}
         seen #{}]
    (if-let [t (first frontier)]
      (recur (into (disj frontier t)
                   (remove seen (referenced-targets (get targets t {}))))
             (conj seen t))
      seen)))
