(ns clj-surgeon.mission-plain-forms
  "Optional one-file plain-Clojure response decoder; never evaluates or writes.

  compile-response accepts frozen mission-forms basis plus raw response text.
  Each expected new-owner-or-owner must occur exactly once. Model text supplies
  no paths, original owner names or span authority. Lowering remains exclusively
  mission-forms/compile-forms; proof and commit are still required downstream.

  This prototype permits plain def/defn/defn- lists, with arbitrary legitimate
  string contents. Anonymous functions, sets and regex literals are framed without evaluation.
  Other dispatch macros and character literals outside strings remain unsupported;
  metadata refuses as protected syntax. Comments inside a form are carried to
  mission-forms, which refuses `:forms-comment-lost` rather than dropping one
  silently; a top-level comment belongs to no owner span and still refuses.
  Nothing unescapes JSON, strips fences, or repairs a refused response.
  Fixed preparse limits: 262144 UTF8 bytes, 2048 opening delimiters, depth 64,
  and at most the existing compiler's 128 changes. These are admission limits,
  not an arbitrary-Clojure grammar or a performance claim."
  (:require
   [clj-surgeon.mission-candidate :as candidate]
   [clj-surgeon.mission-forms :as forms]))

(def limits {:bytes 262144 :delimiters 2048 :depth 64
             :forms (:changes candidate/limits)})

(defn refuse! [code]
  (throw (ex-info "Plain response refused" {:error-type code})))

(defn response-spans
  "Bounded lexical framing before any rewrite parser. Returns exact list spans."
  [response]
  (when-not (string? response) (refuse! :plain-invalid-response))
  (when (or (> (count response) (:bytes limits))
            (> (alength (.getBytes ^String response "UTF-8")) (:bytes limits)))
    (refuse! :candidate-parser-budget))
  (loop [i 0 stack [] quoted? false escaped? false start nil opens 0 spans []]
    (if (= i (count response))
      (do
        (when (or quoted? (seq stack)) (refuse! :candidate-unparseable))
        (when (empty? spans) (refuse! :plain-empty-response))
        spans)
      (let [c (.charAt ^String response i)
            opening? (contains? #{\( \[ \{} c)
            closing (get {\) \( \] \[ \} \{} c)]
        (cond
          quoted?
          (recur (inc i) stack (or escaped? (not= c \"))
                 (and (not escaped?) (= c \\)) start opens spans)

          (or (Character/isWhitespace c) (= c \,))
          (recur (inc i) stack false false start opens spans)

          (= c \#)
          (if (and (seq stack) (< (inc i) (count response))
                   (contains? #{\( \{ \"} (.charAt ^String response (inc i))))
            (recur (inc i) stack false false start opens spans)
            (refuse! :plain-unsupported-reader-syntax))

          (= c \\) (refuse! :plain-unsupported-reader-syntax)
          ;; A comment INSIDE a form belongs to that owner's span and is carried
          ;; through to mission-forms verbatim. A comment at top level belongs to
          ;; no span, so accepting it would drop it silently -- still refused.
          (= c \;)
          (do
            (when (empty? stack) (refuse! :forms-protected-syntax))
            (let [nl (.indexOf ^String response (int \newline) (int i))]
              (recur (if (neg? nl) (count response) nl)
                     stack false false start opens spans)))

          (= c \^) (refuse! :forms-protected-syntax)

          opening?
          (do
            (when (and (empty? stack) (not= c \()) (refuse! :forms-invalid-definition))
            (when (>= opens (:delimiters limits)) (refuse! :candidate-parser-budget))
            (when (>= (count stack) (:depth limits)) (refuse! :candidate-parser-depth))
            (when (and (empty? stack) (>= (count spans) (:forms limits)))
              (refuse! :candidate-parser-budget))
            (recur (inc i) (conj stack c) false false
                   (if (empty? stack) i start) (inc opens) spans))

          closing
          (do
            (when-not (= closing (peek stack)) (refuse! :candidate-unparseable))
            (let [remaining (pop stack)]
              (recur (inc i) remaining false false
                     (when (seq remaining) start) opens
                     (if (empty? remaining) (conj spans [start (inc i)]) spans))))

          (empty? stack) (refuse! :forms-invalid-definition)
          (= c \") (recur (inc i) stack true false start opens spans)
          :else (recur (inc i) stack false false start opens spans))))))

(defn compile-response
  "Compile exact owner coverage from plain forms using unchanged kernel authority."
  [basis response]
  (try
    (when-not (candidate/valid-basis? basis) (refuse! :candidate-invalid-basis))
    (when-not (and (= 1 (count (:sources basis)))
                   (= 1 (count (set (map :file (:owners basis))))))
      (refuse! :plain-one-file-required))
    (when (> (count (:owners basis)) (:forms limits)) (refuse! :candidate-parser-budget))
    (let [expected (group-by #(or (:new-owner %) (:owner %)) (:owners basis))]
      (when (some #(not= 1 (count %)) (vals expected)) (refuse! :plain-ambiguous-owner))
      (let [spans (response-spans response)
            definitions (mapv (fn [[start end]] (forms/definition (subs response start end))) spans)
            names (mapv :name definitions)]
        (when-not (= (count names) (count (set names))) (refuse! :forms-duplicate-owner))
        (when (some #(not (contains? expected %)) names) (refuse! :forms-unknown-owner))
        (when-not (= (set names) (set (keys expected))) (refuse! :plain-owner-coverage))
        (let [replacements (mapv (fn [{:keys [name source]}]
                                   (assoc (select-keys (first (get expected name)) [:file :owner]) :form source))
                                 definitions)
              compiled (forms/compile-forms basis replacements)]
          (if (:ok compiled) (assoc compiled :replacements replacements) compiled))))
    (catch StackOverflowError _ (forms/refusal :candidate-parser-depth))
    (catch Exception e (forms/refusal (or (:error-type (ex-data e)) :candidate-unparseable)))))
