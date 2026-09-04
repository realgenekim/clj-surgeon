(ns writer.handlers.transform
  "Inline Transform (Cmd+E) handlers — text rewriting, formatting, undo."
  (:require
   [clojure.data.json :as json]
   [clojure.string :as str]
   [taoensso.timbre :as log]
   [writer.dispatch :as editor-dispatch]
   [writer.editor-conflict :as editor-conflict]
   [writer.editor-journal :as editor-journal]
   [writer.editorial-diff :as editorial-diff]
   [writer.editorial-proposal :as editorial-proposal]
   [writer.http :as http]
   [writer.llm.config :as llm-config]
   [writer.llm.dispatch :as dispatch]
   [writer.prompt :as prompt]
   [writer.sse :as sse]
   [writer.state :as state]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- transform-model
  "Current transform model: session state, else llm-config feature default.
   nil default on the get-in — closed records throw on no-default reads of
   missing keys (old sessions lack :model)."
  []
  (or (get-in @state/app-state [:transform :model] nil)
      (llm-config/feature-model :transform)))

(defn- call-transform-model
  "Single-prompt transform call — backend routing lives in writer.llm.dispatch."
  [model-kw sys-prompt user-prompt]
  (dispatch/call-text model-kw sys-prompt user-prompt :max-tokens 8192))

(defn parse-transform-response
  "Parse LLM response into a vector of {:text ... :why ...} options.
   Tries multiple strategies:
   1. Strip markdown fences, parse as JSON array
   2. Find JSON array bounds [..] within the response
   3. Fallback: wrap cleaned text as a single option
   Returns a non-empty vector of maps."
  [raw-response]
  (let [;; Strategy 1: strip markdown fences from start/end
        cleaned (-> raw-response
                    (str/replace #"(?s)^```\w*\n?" "")
                    (str/replace #"\n?```\s*$" "")
                    str/trim)
        normalize (fn [parsed]
                    (when (and (sequential? parsed) (seq parsed))
                      (vec (map (fn [item]
                                  (if (map? item)
                                    {:text (str (:text item)) :why (:why item)}
                                    {:text (str item) :why nil}))
                                parsed))))
        ;; Try parsing, log on failure for debugging
        try-parse (fn [label s]
                    (try
                      (let [parsed (json/read-str s :key-fn keyword)]
                        (normalize parsed))
                      (catch Exception e
                        (log/warn :transform-parse/fail label
                                  :error (.getMessage e)
                                  :first-100 (subs s 0 (min 100 (count s))))
                        nil)))
        ;; Strategy 1: direct parse of cleaned text
        result (try-parse :strategy-1-cleaned cleaned)]
    (or result
        ;; Strategy 2: find outermost [...] in the response
        (when-let [start (str/index-of cleaned "[")]
          (when-let [end (str/last-index-of cleaned "]")]
            (when (< start end)
              (try-parse :strategy-2-brackets (subs cleaned start (inc end))))))
        ;; Fallback: wrap cleaned text as single option (at least fences are stripped)
        (do (log/warn :transform-parse/fallback
                      :raw-len (count raw-response)
                      :cleaned-len (count cleaned)
                      :starts-with (subs cleaned 0 (min 80 (count cleaned))))
            [{:text cleaned :why nil}]))))

(defn mechanical-format
  "Pure text formatting: remove pasted Markdown blockquote markers, strip leading
   spaces, join wrapped lines within paragraphs, preserve paragraph breaks (double
   newlines), normalize unordered bullets to - prefix, and preserve ordered-list
   item boundaries while reflowing wrapped continuations. Preserves leading/trailing
   newline structure."
  [text]
  (let [;; Capture leading/trailing newlines to restore after formatting
        leading (re-find #"^\n+" text)
        trailing (re-find #"\n+$" text)
        ;; Treat pasted Markdown quotations as ordinary draft prose. A bare `>`
        ;; becomes an empty line, exposing the paragraph boundary before reflow.
        unquoted (str/replace text #"(?m)^[ \t]*>[ \t]?" "")
        collapsed (str/replace unquoted #"\n{3,}" "\n\n")
        paragraphs (str/split collapsed #"\n\n")
        formatted (map (fn [para]
                         (let [lines (str/split-lines para)
                               trimmed (map str/trim lines)
                               non-empty (vec (remove str/blank? trimmed))]
                           (loop [result [] i 0 prose-acc [] list-item nil]
                             (if (>= i (count non-empty))
                               (let [result (cond-> result
                                              list-item (conj list-item)
                                              (seq prose-acc) (conj (str/join " " prose-acc)))]
                                 (str/join "\n" result))
                               (let [line (nth non-empty i)
                                     unordered? (re-matches #"^[-•*][\s•*-]+.*" line)
                                     ordered? (re-matches #"^\d{1,9}[.)]\s+.*" line)
                                     normalized (cond
                                                  unordered? (str "- " (str/replace line #"^[-•*][\s•*-]+" ""))
                                                  ordered? line)]
                                 (cond
                                   normalized
                                   (let [result (cond-> result
                                                  list-item (conj list-item)
                                                  (seq prose-acc) (conj (str/join " " prose-acc)))]
                                     (recur result (inc i) [] normalized))

                                   list-item
                                   (recur result (inc i) [] (str list-item " " line))

                                   :else
                                   (recur result (inc i) (conj prose-acc line) nil)))))))
                       paragraphs)
        body (str/join "\n\n" (remove str/blank? formatted))
        ;; Collapse double-newlines between consecutive bullet lines into single.
        ;; LinkedIn pastes bullets as separate paragraphs — we want them tight.
        ;; Loop because each replace only handles adjacent pairs.
        body (loop [s body]
               (let [s2 (str/replace s #"(?m)(^- .+)\n\n(- )" "$1\n$2")]
                 (if (= s s2) s2 (recur s2))))]
    (str (or leading "") body (or trailing ""))))

;; ---------------------------------------------------------------------------
;; Negotiation-thread helpers (Rung 2 Slice 1)
;; ---------------------------------------------------------------------------

(defn- sha256-hex
  "Hex SHA-256 of a string (UTF-8) — the thread's tamper-evident record of the
   exact suggested text a negotiation turn produced."
  [^String s]
  (let [d (.digest (java.security.MessageDigest/getInstance "SHA-256")
                   (.getBytes (or s "") "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

(defn- new-round-id []
  (str (java.util.UUID/randomUUID)))

(defn- primary-option-text
  "Text of the currently focused option, whatever its map/string shape."
  [options option-index]
  (let [o (nth options (or option-index 0) nil)]
    (when o (if (map? o) (:text o) (str o)))))

(defn- make-thread-entry
  "One append-only negotiation turn: the instruction, when it happened, and the
   digest of the suggestion it produced."
  [instruction suggested-text]
  {:instruction (or instruction "")
   :at (str (java.time.Instant/now))
   :suggested-sha256 (sha256-hex (or suggested-text ""))})

(defn- author-edited?
  "True when the browser reports a proposal after-text that differs from the
   model's current suggestion — the author has hand-edited it. Only decidable
   for an exact-replacement proposal whose editable hunk the browser sends;
   selection-mode counters carry no hunk and are never treated as author edits."
  [exact? selected-text current-suggested edited-hunk]
  (boolean
   (and exact? (string? edited-hunk) current-suggested
        (let [after-hunk (:after (editorial-diff/review-hunk selected-text current-suggested))]
          (not= edited-hunk after-hunk)))))

(defn build-refine-user-prompt
  "Compose the refine user prompt from SERVER-OWNED memory: the base selection,
   the options presented in the previous round (a counter-instruction may say
   \"all of these…\"), every standing constraint accumulated on the thread, and
   the new counter-instruction. The client never sends this history."
  [selected-text options thread instruction]
  (str "BASE TEXT (the selection being rewritten):\n\"" selected-text "\"\n\n"
       "OPTIONS PRESENTED IN THE PREVIOUS ROUND"
       " (your counter-instruction may refer to \"all of these\"):\n"
       (str/join "\n"
                 (map-indexed
                  (fn [i o]
                    (let [t (if (map? o) (:text o) (str o))
                          w (when (map? o) (:why o))]
                      (str (inc i) ". " t (when (seq w) (str "   — " w)))))
                  options))
       "\n\n"
       (when (seq thread)
         (str "STANDING CONSTRAINTS FROM THIS NEGOTIATION (honor ALL of them):\n"
              (str/join "\n" (map #(str "- " (:instruction %)) thread))
              "\n\n"))
       "NEW COUNTER-INSTRUCTION: " instruction "\n\n"
       "Provide 6 DIVERSE revised replacements that honor the standing constraints "
       "and respond to the counter-instruction. Explain WHY each is good.\n"
       "Return ONLY a JSON array of objects: [{\"text\": \"replacement\", \"why\": \"brief reason\"}]"))

;; ---------------------------------------------------------------------------
;; Handlers
;; ---------------------------------------------------------------------------

(defn handle-open
  "Open transform modal with selected text."
  [request]
  (let [{:keys [selected]} (http/parse-json-body request)]
    (state/transform-open! selected)
    (state/log-event! {:type "transform.open" :selected-length (count (or selected ""))})
    (sse/push-transform-modal!))
  {:status 204})

(defn handle-select
  "Navigate transform options (j/k)."
  [request]
  (let [{:keys [index]} (http/parse-json-body request)]
    (state/transform-select-option! (int index))
    (sse/push-transform-modal!))
  {:status 204})

(defn handle-copy
  "Record a successful browser clipboard copy of one presented candidate.
   The response event already owns the full option batch; this event supplies
   the otherwise-missing human choice signal and stable round join."
  [request]
  (let [{:keys [index]} (http/parse-json-body request)
        st @state/app-state
        {:keys [active? options round-id thread-id]} (:transform st)
        idx (when (number? index) (int index))
        option (when (and active? idx (<= 0 idx) (< idx (count options)))
                 (nth options idx))]
    (if-not option
      {:status 409 :body "Transform candidate is no longer active"}
      (do
        (state/log-event! {:type "transform.option.copy"
                           :round-id round-id
                           :thread-id (or thread-id round-id)
                           :option-index idx
                           :option-number (inc idx)
                           :option option
                           :editor-sync-key (state/editor-sync-key st)
                           :state-version (:state-version st)})
        {:status 204}))))

(defn handle-editor-snapshot
  "Return the exact server-owned editor frame used to fence an external proposal."
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json"
             "Cache-Control" "no-store"}
   :body (json/write-str (state/editor-frame-data @state/app-state))})

(defn propose-exact!
  "Open a browser-visible, non-mutating whole-editor replacement proposal —
   the shared core behind both the browser propose endpoint and the
   chat->proposal pipe. Fences identity/revision/before-text exactly (throws
   ExceptionInfo → 409); on success it seeds the negotiation thread with the
   opening turn and returns a ring 201 carrying the proposal id.
   `params` is a plain map (never a ring request): {:before :after :why
   :instruction :editor-sync-key :state-version :provenance}."
  [{:keys [before after why instruction editor-sync-key state-version provenance]}]
  (if-not (and (string? before) (string? after)
               (string? editor-sync-key) (integer? state-version))
    {:status 400 :body "before, after, editor-sync-key, and state-version are required"}
    (try
      (let [proposal-id (str (java.util.UUID/randomUUID))]
        (state/transform-open-exact!
         {:before before :after after :why why :instruction instruction
          :editor-sync-key editor-sync-key :state-version state-version
          :proposal-id proposal-id :provenance provenance})
        (state/set-selected-diff-mode!
         (:recommended-mode (editorial-diff/diff-view before after)))
        ;; Fences passed — record the opening negotiation turn.
        (state/transform-append-thread-entry!
         (make-thread-entry (or instruction "Exact replacement proposed") after))
        (state/log-event! {:type "editor.proposal.open"
                           :proposal-id proposal-id
                           :before-length (count before)
                           :after-length (count after)
                           :source (when (map? provenance)
                                     (some-> (:source provenance) name))})
        (sse/push-transform-modal!)
        {:status 201
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:proposal-id proposal-id
                                :status "pending-browser"})})
      (catch clojure.lang.ExceptionInfo e
        {:status 409
         :headers {"Content-Type" "application/json"}
         :body (json/write-str (ex-data e))}))))

(defn handle-propose-exact
  "Open a browser-visible, non-mutating whole-editor replacement proposal."
  [request]
  (propose-exact! (http/parse-json-body request)))

(defn- uuid-string? [value]
  (and (string? value)
       (boolean
        (re-matches
         #"(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
         value))))

(defn handle-propose-inactive-exact
  "Queue a non-mutating exact replacement for an inactive stable-ID Book node.
   The server resolves Before text and fences it by SHA-256; opening the target
   later revalidates and surfaces the ordinary Transform review card."
  [request]
  (let [{:keys [project-id node-id expected-sha256 after why instruction provenance]}
        (http/parse-json-body request)]
    (if-not (and (uuid-string? project-id)
                 (uuid-string? node-id)
                 (string? expected-sha256)
                 (re-matches #"[0-9a-f]{64}" expected-sha256)
                 (string? after))
      {:status 400
       :body "UUID project-id, UUID node-id, 64-character expected-sha256, and after are required"}
      (try
        (let [proposal-id (str (java.util.UUID/randomUUID))
              proposal (state/stage-inactive-editorial-proposal!
                        {:project-id project-id :node-id node-id
                         :expected-sha256 expected-sha256 :after after
                         :why why :instruction instruction
                         :proposal-id proposal-id :provenance provenance})]
          (state/log-event! {:type "editor.proposal.queued"
                             :proposal-id (:proposal-id proposal)
                             :project-id project-id :node-id node-id
                             :before-length (count (:before proposal))
                             :after-length (count after)
                             :idempotent? (boolean (:idempotent? proposal))})
          (sse/push-notify! (str "Editorial proposal ready for "
                                 (:node-title proposal) " — open it to review"))
          {:status 201
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:proposal-id (:proposal-id proposal)
                                  :project-id project-id
                                  :node-id node-id
                                  :node-title (:node-title proposal)
                                  :status "queued"
                                  :expected-sha256 expected-sha256
                                  :after-sha256 (:after-sha256 proposal)
                                  :idempotent (boolean (:idempotent? proposal))})})
        (catch clojure.lang.ExceptionInfo e
          {:status 409
           :headers {"Content-Type" "application/json"}
           :body (json/write-str (ex-data e))})))))

(defn handle-proposal-visible
  "Browser acknowledgement that the exact proposal is present in its modal DOM."
  [request]
  (let [{:keys [proposal-id]} (http/parse-json-body request)]
    (if (and (string? proposal-id)
             (state/transform-mark-visible! proposal-id))
      (do (state/log-event! {:type "editor.proposal.visible"
                             :proposal-id proposal-id})
          {:status 204})
      {:status 409 :body "Proposal is no longer active"})))

(defn handle-proposal-status
  "Return lifecycle status for the current exact replacement proposal."
  [request]
  (let [{:keys [proposal-id]} (http/parse-json-body request)
        status (state/editorial-proposal-status proposal-id)]
    (if status
      {:status 200
       :headers {"Content-Type" "application/json"
                 "Cache-Control" "no-store"}
       :body (json/write-str {:proposal-id proposal-id
                              :status (name status)})}
      {:status 404 :body "Proposal not found"})))

(defn handle-apply
  "Fold the visible editor snapshot and apply one transform as a single
   admitted, durable command. The response carries the complete authoritative
   frame; SSE only refreshes projections and is never the apply acknowledgement."
  [request]
  (let [{req-index :index sync :sync edited-hunk :edited-hunk}
        (http/parse-json-body request)]
    (if-not (and (map? sync)
                 (or (nil? edited-hunk) (string? edited-hunk)))
      {:status 400 :body "Visible editor snapshot required"}
      (try
        (let [status (volatile! :no-option)
              selected-length (volatile! 0)
              replacement-length (volatile! 0)
              applied-round-id (volatile! nil)
              applied-thread-id (volatile! nil)
              applied-option-index (volatile! nil)
              applied-option-count (volatile! 0)
              applied-option (volatile! nil)
              applied-editor-sync-key (volatile! nil)
              applied-state-version (volatile! nil)
              author-edited-proposal? (volatile! false)
              applied-range (volatile! nil)
              res (editor-dispatch/fold-editor-snapshot-and-tx!
                   sync
                   (fn [st]
                     (let [st (if (some? req-index)
                                (assoc-in st [:transform :option-index] (int req-index))
                                st)
                           {:keys [selected-text options option-index mode round-id thread-id]}
                           (:transform st)
                           option (nth options option-index nil)
                           suggested (when option (if (map? option) (:text option) (str option)))
                           editable-value-required? (and (= :exact-replacement mode)
                                                         (nil? edited-hunk))
                           replacement (if (and (= :exact-replacement mode)
                                                (string? edited-hunk)
                                                suggested)
                                         (-> (editorial-proposal/start
                                              {:before selected-text
                                               :suggested suggested})
                                             (editorial-proposal/edit-review-hunk edited-hunk)
                                             editorial-proposal/replacement)
                                         suggested)
                           st (if (and option (not= suggested replacement))
                                (assoc-in st [:transform :options option-index]
                                          (if (map? option)
                                            (assoc option :text replacement)
                                            {:text replacement}))
                                st)
                           [next-st result] (if editable-value-required?
                                              [st :editable-value-required]
                                              (state/transform-apply-tx st))
                           _ (vreset! applied-round-id round-id)
                           _ (vreset! applied-thread-id thread-id)
                           _ (vreset! applied-option-index option-index)
                           _ (vreset! applied-option-count (count options))
                           _ (vreset! applied-option option)
                           _ (vreset! applied-editor-sync-key (state/editor-sync-key st))
                           _ (vreset! applied-state-version (:state-version st))
                           _ (vreset! author-edited-proposal? (not= suggested replacement))
                           _ (when (= :applied result)
                               (let [draft (or (:draft st) "")
                                     exact-start (when (and (seq selected-text)
                                                            (str/includes? draft selected-text))
                                                   (.indexOf ^String draft
                                                             ^String selected-text))
                                     start (if (and (some? exact-start)
                                                    (not (neg? exact-start)))
                                             exact-start
                                             0)]
                                 (vreset! applied-range
                                          {:start start
                                           :end (+ start (count (or replacement "")))})))]
                       (vreset! status result)
                       (vreset! selected-length (count (or selected-text "")))
                       (vreset! replacement-length (count (or replacement "")))
                       next-st)))]
          (cond
            (not (editor-dispatch/book-ack-ok? (:book res)))
            (let [ack (get-in res [:book :ack])]
              (if (:timeout ack)
                {:status 202 :body "Transform save outcome unknown"}
                {:status 500 :body "Transform was not confirmed durable"}))

            :else
            (do
              (case @status
                :applied
                ;; Verbatim before/after/instruction were just computed by
                ;; transform-apply-tx (state.clj) and landed as the newest
                ;; entry in [:transform :history] by the swap! inside
                ;; fold-editor-snapshot-and-tx! above — read them back from
                ;; state rather than re-deriving the match logic here.
                (let [{:keys [before after instruction]}
                      (peek (get-in @state/app-state [:transform :history]))
                      thread (get-in @state/app-state [:transform :thread])]
                  (state/log-event! {:type "transform.apply"
                                     :outcome "APPLY"
                                     :round-id @applied-round-id
                                     :thread-id (or @applied-thread-id @applied-round-id)
                                     :option-index @applied-option-index
                                     :option-number (when (some? @applied-option-index)
                                                      (inc @applied-option-index))
                                     :option-count @applied-option-count
                                     :chosen-option @applied-option
                                     :editor-sync-key @applied-editor-sync-key
                                     :state-version @applied-state-version
                                     :replacement-length @replacement-length
                                     :author-edited-proposal? @author-edited-proposal?
                                     :before before
                                     :after after
                                     :instruction instruction
                                     :thread thread}))

                :no-match
                (do (state/log-event! {:type "transform.apply.no-match"
                                       :selected-length @selected-length})
                    (sse/push-notification!
                     "Apply failed: selected text changed since transform opened"))

                :editor-conflict
                (do (state/log-event! {:type "transform.apply.editor-conflict"
                                       :selected-length @selected-length})
                    (sse/push-notification!
                     "Apply refused: transform belongs to a different editor revision"))

                :editable-value-required
                (do (state/log-event! {:type "transform.apply.editable-value-required"
                                       :proposal-id (get-in @state/app-state
                                                            [:transform :proposal-id])})
                    (sse/push-notification!
                     "Apply refused: reload to activate the editable proposal UI"))

                nil)
              (sse/push-transform-modal!)
              (sse/push-book-trees!)
              (let [frame (cond-> (assoc (state/editor-frame-data @state/app-state)
                                         :transform-status (name @status))
                            @applied-range
                            (assoc :transform-range @applied-range))]
                {:status 200
                 :headers {"Content-Type" "application/json"
                           "X-State-Version" (str (:state-version frame))
                           "X-Editor-Sync-Key" (:editor-sync-key frame)}
                 :body (json/write-str frame)}))))
        (catch clojure.lang.ExceptionInfo e
          (let [conflict (ex-data e)]
            (if (#{"stale-version" "stale-editor" "conflict-changed" "invalid-editor-state"
                   "unsettled-editor-projection"}
                 (:reason conflict))
              {:status 409
               :headers {"Content-Type" "application/json"}
               :body (json/write-str
                      (assoc conflict :error "Transform editor conflict"))}
              (throw e))))))))

(defn handle-more
  "More like this: ask Claude for variations of the selected option."
  [request]
  (let [{:keys [index]} (http/parse-json-body request)
        idx (try (int index) (catch Exception _ 0))
        captured-state @state/app-state
        {:keys [options selected-text round-id thread-id]} (:transform captured-state)
        liked (nth options idx nil)
        liked-text (if (map? liked) (:text liked) (str liked))
        instruction (str "I liked this one: \"" liked-text "\". Give me more variations in this direction.")
        plan (prompt/resolve-prompt-plan captured-state :transform-more)
        sys-prompt (prompt/build-system-prompt
                    captured-state :feature :transform-more :plan plan)
        model-kw (transform-model)
        user-prompt (str "SELECTED TEXT: \"" selected-text "\"\n"
                         "INSTRUCTION: " instruction "\n\n"
                         "Provide 6 DIVERSE alternative replacements.\n"
                         "Return ONLY a JSON array of objects: [{\"text\": \"replacement\", \"why\": \"brief reason\"}]")
        service-tier (dispatch/service-tier
                      model-kw sys-prompt
                      [{:role "user" :content user-prompt}])
        start-ms (System/currentTimeMillis)
        next-round-id (new-round-id)
        effective-thread-id (or thread-id round-id next-round-id)
        editor-sync-key (state/editor-sync-key captured-state)
        state-version (:state-version captured-state)]
    (prompt/record-prompt-plan! plan :transform-more)
    (state/log-event! {:type "transform.request"
                       :round-id next-round-id
                       :thread-id effective-thread-id
                       :source-round-id round-id
                       :source-option-index idx
                       :kind "more-like-this"
                       :instruction instruction
                       :selected (or selected-text "")
                       :selected-length (count (or selected-text ""))
                       :editor-sync-key editor-sync-key
                       :state-version state-version})
    (state/transform-set-loading! start-ms next-round-id effective-thread-id)
    (swap! state/app-state update :transform assoc
           :instruction instruction :service-tier service-tier)
    (sse/push-transform-modal!)
    (future
      (try
        (let [result (call-transform-model model-kw sys-prompt user-prompt)
              cleaned (-> result
                          (str/replace #"(?s)^```\w*\n?" "")
                          (str/replace #"\n?```\s*$" "")
                          str/trim)
              new-options (try
                            (let [parsed (json/read-str cleaned :key-fn keyword)]
                              (when (sequential? parsed)
                                (vec (map (fn [item]
                                            (if (map? item) item {:text (str item) :why nil}))
                                          parsed))))
                            (catch Exception _
                              [{:text result :why nil}]))
              elapsed-ms (- (System/currentTimeMillis) start-ms)]
          (let [presented (or new-options [{:text result :why nil}])]
            (state/log-event! {:type "transform.response"
                               :round-id next-round-id
                               :thread-id effective-thread-id
                               :source-round-id round-id
                               :kind "more-like-this"
                               :option-count (count presented)
                               :options presented
                               :elapsed elapsed-ms
                               :editor-sync-key editor-sync-key
                               :state-version state-version})
            (state/transform-set-options! presented elapsed-ms))
          (sse/push-transform-modal!))
        (catch Exception e
          (state/transform-set-options! [{:text (str "Error: " (.getMessage e)) :why nil}]
                                        (- (System/currentTimeMillis) start-ms))
          (sse/push-transform-modal!)))))
  {:status 204})

(defn handle-format
  "POST /api/transform/format — one fenced, durable editor command.
   The visible browser snapshot and the mechanical rewrite commit together;
   the HTTP response carries the authoritative frame, so SSE delivery is not
   the acknowledgement."
  [request]
  (let [{:keys [sync]}
        (try (http/parse-json-body request) (catch Exception _ {}))]
    (if-not (map? sync)
      {:status 400 :body "Visible editor snapshot required"}
      (try
        (let [journal-headers (editor-journal/journal-receipt sync)
              before-draft (:draft sync)
              res (editor-dispatch/fold-editor-snapshot-and-tx!
                   sync
                   (fn [st]
                     (let [formatted (mechanical-format (:draft st))]
                       (state/sync-draft-tx
                        st {:draft formatted :allow-blank-draft? true}))))
              frame (state/editor-frame-data (:state res))
              session-ack (when (editor-dispatch/book-ack-ok? (:book res))
                            (state/save-session!))]
          (cond
            (not (editor-dispatch/book-ack-ok? (:book res)))
            (if (get-in res [:book :ack :timeout])
              {:status 202 :body "Format save outcome unknown"}
              {:status 500 :body "Format was not confirmed durable"})

            (and session-ack (not (:ok session-ack)))
            (if (:timeout session-ack)
              {:status 202 :body "Format session save outcome unknown"}
              {:status 500 :body "Format session was not confirmed durable"})

            :else
            (do
              (state/log-event! {:type "transform.format"
                                 :before-length (count (or before-draft ""))
                                 :after-length (count (:draft frame))
                                 :before-sha256 (sha256-hex (or before-draft ""))
                                 :after-sha256 (sha256-hex (:draft frame))
                                 :browser-version (:state-version sync)
                                 :server-version (:state-version frame)
                                 :editor-sync-key (:editor-sync-key frame)})
              ;; Secondary projections may refresh over SSE, but the browser
              ;; applies the editor frame from this response.
              (sse/push-book-trees!)
              {:status 200
               :headers (merge {"Content-Type" "application/json"
                                "X-State-Version" (str (:state-version frame))
                                "X-Editor-Sync-Key" (:editor-sync-key frame)}
                               journal-headers)
               :body (json/write-str frame)})))
        (catch clojure.lang.ExceptionInfo e
          (let [{:keys [reason] :as conflict} (ex-data e)]
            (if (#{"stale-version" "stale-editor" "conflict-changed"
                   "invalid-editor-state" "unsettled-editor-projection"
                   "invalid-journal-receipt"} reason)
              (do
                (state/log-event! {:type "transform.format.rejected"
                                   :reason reason
                                   :browser-version (:browser-version conflict)
                                   :server-version (:server-version conflict)})
                ;; The browser can only PRESENT a conflict it can read: the
                ;; view-model is what supplies outcome, conflict-id, actions and
                ;; cross-document. Without it a Format 409 is undisplayable and
                ;; degrades to a bare refusal notification.
                ;; INTENT: EDITOR-CONF-005
                ;; @spec EDITOR-CONF-005
                {:status 409
                 :headers {"Content-Type" "application/json"}
                 :body (json/write-str
                        (assoc (editor-conflict/view-model "transform.format" conflict)
                               :error "Format editor conflict"
                               :saved false))})
              (throw e))))))))

(defn handle-draft-undo
  "POST /api/draft/undo — restore previous state from unified undo stack."
  [_request]
  (when-let [label (state/pop-undo!)]
    (sse/push-draft-change! {:reason :accepted-operation})
    (when (= :distillery (get-in @state/app-state [:ui :active-top-tab]))
      (sse/push-distillery!))
    (sse/push-notification! (str "Undo: " label))
    (state/log-event! {:type "draft.undo" :restored label}))
  {:status 204})

(defn handle-push-undo
  "POST /api/draft/push-undo — save current draft to undo stack.
   Called by client-side insertAtCursor before cherry-pick/autocomplete inserts
   so the pre-insert state is undoable."
  [request]
  (let [{:keys [label]} (http/parse-json-body request)]
    (state/push-undo! (or label "insert")))
  {:status 204})

(defn handle-set-model
  "Switch transform model. Rebuilds :transform as a plain map — assoc-in of
   :model would throw on old closed records that lack the key (2026-07-09)."
  [request]
  (let [{:keys [model]} (http/parse-json-body request)]
    (swap! state/app-state update :transform
           #(assoc (into {} %) :model (keyword model)))
    (sse/push-transform-modal!))
  {:status 204})

(defn handle-cancel
  "Close transform modal."
  [_request]
  (let [st @state/app-state
        {:keys [round-id thread-id options instruction option-index]} (:transform st)]
    (state/transform-close!)
    (state/log-event! {:type "transform.cancel"
                       :round-id round-id
                       :thread-id (or thread-id round-id)
                       :option-count (count options)
                       :option-index option-index
                       :instruction instruction
                       :editor-sync-key (state/editor-sync-key st)
                       :state-version (:state-version st)}))
  (sse/push-transform-modal!)
  {:status 204})

(defn refine-transform!
  "Negotiate the open transform proposal IN PLACE: re-roll the model with the
   accumulated thread constraints + the previous round's presented options + a
   new counter-instruction, then atomically install the refined options onto
   the SAME proposal card (thread with memory). Fenced exactly like Apply —
   same node identity + revision, and (exact-replacement) the same proposal id —
   and refuses (409) rather than clobber a hand-edited proposal or a stale
   editor. `edited-hunk` (browser-owned) is the current visible after-text; when
   it diverges from the model's suggestion the author has edited it and their
   work wins. Returns a ring response."
  [{:keys [instruction proposal-id edited-hunk]}]
  (let [st @state/app-state
        {:keys [active? options option-index mode selected-text thread
                base-editor-sync-key base-state-version]} (:transform st)
        exact? (= :exact-replacement mode)
        current-suggested (primary-option-text options option-index)]
    (cond
      (str/blank? instruction)
      {:status 400 :body "A refine instruction is required"}

      (not (and active? (seq options)))
      {:status 409 :body "No open proposal to refine"}

      (and exact? proposal-id (not= proposal-id (:proposal-id (:transform st))))
      {:status 409 :body "Refine targets a different proposal"}

      (or (not= base-editor-sync-key (state/editor-sync-key st))
          (not= base-state-version (:state-version st)))
      (do (state/log-event! {:type "transform.refine.editor-conflict"
                             :instruction instruction})
          (sse/push-notification! "Refine refused — the editor moved since the proposal opened")
          {:status 409 :body "Refine targets a stale editor revision"})

      (author-edited? exact? selected-text current-suggested edited-hunk)
      (do (state/log-event! {:type "transform.refine.author-edited"
                             :instruction instruction})
          (sse/push-notification! "Refine refused — your edits to the proposal are kept")
          {:status 409 :body "The proposal has unsaved author edits"})

      :else
      (let [start-ms (System/currentTimeMillis)
            source-round-id (get-in st [:transform :round-id])
            round-id (new-round-id)
            thread-id (or (get-in st [:transform :thread-id])
                          source-round-id
                          round-id)
            plan (prompt/resolve-prompt-plan st :transform-refine)
            sys-prompt (prompt/build-system-prompt
                        st :feature :transform-refine :plan plan)
            model-kw (transform-model)
            user-prompt (build-refine-user-prompt
                         selected-text options thread instruction)
            service-tier (dispatch/service-tier
                          model-kw sys-prompt
                          [{:role "user" :content user-prompt}])]
        (prompt/record-prompt-plan! plan :transform-refine)
        (state/transform-set-refining! instruction start-ms)
        (swap! state/app-state assoc-in [:transform :service-tier] service-tier)
        (state/log-event! {:type "transform.refine.request"
                           :round-id round-id
                           :thread-id thread-id
                           :source-round-id source-round-id
                           :instruction instruction
                           :selected-length (count (or selected-text ""))
                           :editor-sync-key (state/editor-sync-key st)
                           :state-version (:state-version st)})
        (sse/push-transform-modal!)
        (try
          (let [result (call-transform-model model-kw sys-prompt user-prompt)
                elapsed-ms (- (System/currentTimeMillis) start-ms)
                new-options (parse-transform-response result)
                new-primary (primary-option-text new-options 0)
                status (state/apply-transform-refine!
                        {:proposal-id proposal-id
                         :round-id round-id
                         :new-options new-options
                         :new-instruction instruction
                         :thread-entry (make-thread-entry instruction new-primary)
                         :elapsed-ms elapsed-ms})]
            (if (= :refined status)
              (let [depth (count (get-in @state/app-state [:transform :thread]))]
                (state/log-event! {:type "transform.refine"
                                   :round-id round-id
                                   :thread-id thread-id
                                   :source-round-id source-round-id
                                   :instruction instruction
                                   :selected-length (count (or selected-text ""))
                                   :option-count (count new-options)
                                   :options new-options
                                   :history-length depth
                                   :elapsed elapsed-ms
                                   :editor-sync-key (state/editor-sync-key st)
                                   :state-version (:state-version st)})
                (sse/push-transform-modal!)
                (sse/push-notification! (str "Refined — round " depth))
                {:status 204})
              ;; The generation moved under us between pre-check and install.
              (do (state/transform-clear-loading!)
                  (state/log-event! {:type "transform.refine.conflict"
                                     :reason (name status)
                                     :instruction instruction})
                  (sse/push-transform-modal!)
                  (sse/push-notification! "Refine refused — the proposal changed")
                  {:status 409 :body "Refine conflict"})))
          (catch Exception e
            (state/transform-clear-loading!)
            (sse/push-transform-modal!)
            (sse/push-notification! (str "Refine error: " (.getMessage e)))
            (log/error :transform-refine/error (.getMessage e))
            {:status 500
             :headers {"Content-Type" "application/json"}
             :body (json/write-str {:error (.getMessage e)})}))))))

(defn handle-refine
  "POST /api/transform/refine {proposal-id, instruction, edited-hunk} — revise
   the SAME open proposal with a counter-instruction (negotiation with memory)."
  [request]
  (refine-transform! (http/parse-json-body request)))

(defn handle-transform
  "POST /api/transform — transform selected text with a natural language instruction.

   Server-decides negotiation: when a selection proposal for THIS editor
   generation is already open (a prior round exists on the thread), a further
   instruction is a counter-offer and is routed to refine — the same card
   remembers, the client stays dumb. Otherwise it is a fresh first round."
  [request]
  (let [{:keys [selected instruction]} (http/parse-json-body request)
        st @state/app-state
        {:keys [active? options mode thread base-editor-sync-key base-state-version]}
        (:transform st)
        ;; The selection is server-owned: /api/transform/open captured it from
        ;; the textarea and it is the string Apply will match against. While a
        ;; transform is open, ALWAYS prefer it over anything the client sent —
        ;; the modal's old textContent scrape appended UI chrome ("Copy") and
        ;; made every Apply fail :no-match (bd 7x3). The body param remains a
        ;; fallback only for a caller posting without a prior open.
        selected (or (when active?
                       (not-empty (get-in st [:transform :selected-text])))
                     selected)
        counter? (and active? (seq options) (seq thread)
                      (not= :exact-replacement mode)
                      (= base-editor-sync-key (state/editor-sync-key st))
                      (= base-state-version (:state-version st)))]
    (if counter?
      ;; A counter-instruction on an open round → negotiate in place (memory).
      (refine-transform! {:instruction instruction})
      ;; Fresh first round.
      (let [round-id (new-round-id)
            plan (prompt/resolve-prompt-plan st :transform)
            sys-prompt (prompt/build-system-prompt
                        st :feature :transform :plan plan)
            model-kw (transform-model)
            user-prompt (str "SELECTED TEXT: \"" selected "\"\n"
                             "INSTRUCTION: " instruction "\n\n"
                             "Provide 6 DIVERSE alternative replacements — explore different directions, tones, and angles. For each, explain WHY it's good and what makes it different from the others.\n"
                             "Return ONLY a JSON array of objects: [{\"text\": \"replacement\", \"why\": \"brief reason + what makes this direction unique\"}]")
            service-tier (dispatch/service-tier
                          model-kw sys-prompt
                          [{:role "user" :content user-prompt}])
            start-ms (System/currentTimeMillis)
            editor-sync-key (state/editor-sync-key st)
            state-version (:state-version st)]
        (prompt/record-prompt-plan! plan :transform)
        (state/log-event! {:type "transform.request"
                           :round-id round-id
                           :thread-id round-id
                           :instruction instruction
                           :selected (or selected "")
                           :selected-length (count (or selected ""))
                           :history-length 0
                           :editor-sync-key editor-sync-key
                           :state-version state-version})
        (state/transform-set-loading! start-ms round-id)
        (swap! state/app-state update :transform assoc
               :selected-text (or selected "") :instruction (or instruction "")
               :service-tier service-tier)
        (sse/push-transform-modal!)
        (try
          (let [result (call-transform-model model-kw sys-prompt user-prompt)
                elapsed-ms (- (System/currentTimeMillis) start-ms)
                options (parse-transform-response result)]
            (state/log-event! {:type "transform.response"
                               :round-id round-id
                               :thread-id round-id
                               :option-count (count options)
                               :options options
                               :elapsed elapsed-ms
                               :editor-sync-key editor-sync-key
                               :state-version state-version})
            (state/transform-set-options! options elapsed-ms)
            (swap! state/app-state update :transform assoc :instruction instruction)
            ;; Seed the negotiation thread's opening turn so the NEXT instruction
            ;; is recognized as a counter-offer (server-decides routing above).
            (state/transform-append-thread-entry!
             (make-thread-entry instruction (primary-option-text options 0)))
            (sse/push-transform-modal!)
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/write-str {:options options
                                    :elapsed elapsed-ms})})
          (catch Exception e
            {:status 500
             :headers {"Content-Type" "application/json"}
             :body (json/write-str {:error (.getMessage e)})}))))))
