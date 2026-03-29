(ns writer.state
  "Application state: single atom, session persistence, event logging."
  (:require [closed-record.core :as cr]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- =>]]
            [taoensso.timbre :as log]
            [writer.rewrite.extract :as rewrite-extract])
  (:import [java.time Instant]
           [java.util Timer TimerTask UUID]))

;; ---------------------------------------------------------------------------
;; Forward declarations (transition! defined after settle-editing-state + log-event!)
;; ---------------------------------------------------------------------------

(declare transition!)

;; ---------------------------------------------------------------------------
;; I/O Guard — bind to false in tests to suppress all file writes
;; ---------------------------------------------------------------------------

(def ^:dynamic *io-enabled*
  "When false, all file I/O (save-session!, log-event!, WAL, MRU) is suppressed.
   Tests bind this to false so they never corrupt drafts/ or logs/."
  true)

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn short-id
  "Generate an 8-char hex ID from a random UUID. Greppable in events.jsonl."
  []
  (subs (str (UUID/randomUUID)) 0 8))

;; ---------------------------------------------------------------------------
;; App state — single source of truth
;; ---------------------------------------------------------------------------

(defonce app-state
  (atom (cr/closed-record-recursive
         {:platform :linkedin
          :draft "" :context "" :leftovers "" :cursor-pos 0
          :project "default"
          :creating-project? false
          :goal nil
          :state-version 0 ;; Monotonic counter — bumped on transitions, sent to browser for optimistic locking

           ;; AI (Phase 2+)
          :chat {:history [] :loading? false}
          :fanout {:runs [] :active-run-idx nil :loading-models #{}}
          :critique {:progress 0 :total 0 :responses [] :synthesis nil
                     :synthesis-pills [] :timestamp nil :pending-requests []}
          :autocomplete {:options [] :loading? false :error nil :timestamp nil}

           ;; Option Distillation Studio
          :distillery {:left-paras [] :center-paras []
                       :focus-col :left :focus-index 0
                       :left-focus-index 0 :center-focus-index 0
                       :editing? false :edit-col nil :edit-idx nil
                       :ai-responses [] :ai-loading? false
                       :ai-history [] :ai-prompt-length 0
                       :spark-history []
                       ;; N-Lane mode (multi-draft comparison)
                       :mode :two-lane ;; :two-lane | :n-lane
                       :lanes [] ;; [{:id "uuid" :label "model" :pills ["p1" "p2"]}]
                       :assembled-lane [] ;; pills in assembly lane
                       :focus-lane 0
                       :n-lane-focus-index 0
                       :renaming-lane nil
                       ;; Bucket mode — manuscript (left) + workbenches (right)
                       :bucket-mode? false ;; explicit toggle (B key), not inferred from content
                       :buckets []         ;; [{:id "uuid" :pills [{:text ".." :level 0 :collapsed? false}]}]
                       :leftovers-pills [] ;; [{:text ".." :from-section "" :stashed-at ""}]
                       :trash-pills []     ;; [{:text ".." :trashed-at ""}]
                       :active-bucket nil  ;; index of last-used bucket (for > shortcut)
                       :viewing-bucket nil  ;; index of bucket being inspected (nil = grid view)
                       ;; AI cherry-pick paragraphs — flat list across all responses
                       :ai-paragraphs []}  ;; [{:text :model :response-idx :para-idx :picked?}]
          :distillery-undo nil ;; snapshot of :distillery before last destructive action
          :draft-undo nil ;; previous draft text for undo (format, etc.)

           ;; Book Workshop — multi-level outliner for organizing book/post material
          :book-workshop {:projects [{:id "phoenix-2"
                                      :title "The Phoenix Project Follow-Up"
                                      :nodes [{:id "ch1" :level 0 :type :chapter :title "The 2 AM Call"
                                               :context "Brent is a 15-year veteran. The cost of being irreplaceable."
                                               :draft "" :leftovers "" :collapsed? false}
                                              {:id "ch1-f1" :level 1 :type :fragment :title ""
                                               :context "" :draft "Brent's phone rings at 2 AM. Again. Third time this week."
                                               :leftovers "" :collapsed? false}
                                              {:id "ch1-f2" :level 1 :type :fragment :title ""
                                               :context "" :draft "The deployment that broke everything — a Friday afternoon push."
                                               :leftovers "" :collapsed? false}
                                              {:id "ch2" :level 0 :type :chapter :title "The Eye-Roll Heard Round the Zoom"
                                               :context "Brent on the Zoom. Coding experiment happening live."
                                               :draft "" :leftovers "" :collapsed? false}
                                              {:id "ch2-s1" :level 1 :type :section :title "The Zoom Call"
                                               :context "" :draft "" :leftovers "" :collapsed? false}
                                              {:id "ch2-s1-f1" :level 2 :type :fragment :title ""
                                               :context "" :draft "Brent sighing so audibly it comes through the mic"
                                               :leftovers "" :collapsed? false}
                                              {:id "ch2-s1-f2" :level 2 :type :fragment :title ""
                                               :context "" :draft "Someone demonstrates vibe coding live — builds in 3 minutes what took 2 days"
                                               :leftovers "" :collapsed? false}
                                              {:id "ch2-s2" :level 1 :type :section :title "The Crack"
                                               :context "" :draft "" :leftovers "" :collapsed? false}
                                              {:id "ch2-s2-f1" :level 2 :type :fragment :title ""
                                               :context "" :draft "Late at night, after the kids are asleep, Brent opens a terminal."
                                               :leftovers "" :collapsed? false}
                                              {:id "ch2-s2-f2" :level 2 :type :fragment :title ""
                                               :context "" :draft "He tries it. Just to prove it's garbage. And... it's not garbage."
                                               :leftovers "" :collapsed? false}
                                              {:id "ch3" :level 0 :type :chapter :title "The Reddy Lessons"
                                               :context "Admiral Biehn's framework. What delivering actually means."
                                               :draft "" :leftovers "" :collapsed? false}
                                              {:id "ch4" :level 0 :type :chapter :title "The Organizational Rewiring"
                                               :context "Based on Dustin Warner / NRC Health conversations."
                                               :draft "" :leftovers "" :collapsed? false}
                                              {:id "ch4-f1" :level 1 :type :fragment :title ""
                                               :context "" :draft "2 developers shipping in 4 hours. Sprint planning becomes intolerable."
                                               :leftovers "" :collapsed? false}
                                              {:id "ch5" :level 0 :type :chapter :title "The Language Barrier Dissolves"
                                               :context "The most counterintuitive insight from Dustin."
                                               :draft "" :leftovers "" :collapsed? false}
                                              {:id "ch5-f1" :level 1 :type :fragment :title ""
                                               :context "" :draft "Squads grouped by spoken language, not technical skill"
                                               :leftovers "" :collapsed? false}
                                              {:id "ch5-f2" :level 1 :type :fragment :title ""
                                               :context "" :draft "Dustin at midnight in the Vietnamese channel, AI translating"
                                               :leftovers "" :collapsed? false}
                                              {:id "ch6" :level 0 :type :chapter :title "The Personal Connection"
                                               :context "Gene's mom story. Language as barrier vs bridge."
                                               :draft "" :leftovers "" :collapsed? false}
                                              {:id "ch6-f1" :level 1 :type :fragment :title ""
                                               :context "" :draft "Used ChatGPT to capture mom's medical symptoms in Korean"
                                               :leftovers "" :collapsed? false}]
                                      :focus-idx 0}
                                     {:id "nrc-posts"
                                      :title "NRC / Dustin Warner Posts"
                                      :nodes [{:id "p1" :level 0 :type :chapter :title "The Rewiring Multiplier"
                                               :context "The hook that landed: individual dev speed is table stakes, team wiring is the real multiplier."
                                               :draft "" :leftovers "" :collapsed? false}
                                              {:id "p1-h1" :level 1 :type :fragment :title "Hook Option A"
                                               :context "" :draft "I was so excited when Dustin Warner proved to me that the bigger AI multiplier isn't individual developer speed — it's rewiring the shape of software teams"
                                               :leftovers "" :collapsed? false}
                                              {:id "p1-h2" :level 1 :type :fragment :title "Hook Option B"
                                               :context "" :draft "I just got off a call with Dustin Warner that melted my head."
                                               :leftovers "" :collapsed? false}
                                              {:id "p1-b1" :level 1 :type :section :title "The Evidence"
                                               :context "" :draft "" :leftovers "" :collapsed? false}
                                              {:id "p1-b1-f1" :level 2 :type :fragment :title ""
                                               :context "" :draft "2 developers, 4-hour delivery cycles"
                                               :leftovers "" :collapsed? false}
                                              {:id "p1-b1-f2" :level 2 :type :fragment :title ""
                                               :context "" :draft "Sprint planning abandoned — cost of delay too high"
                                               :leftovers "" :collapsed? false}
                                              {:id "p2" :level 0 :type :chapter :title "The Translation Layer Bottleneck"
                                               :context "Leadership systems angle — the manager as communication hub anti-pattern."
                                               :draft "" :leftovers "" :collapsed? false}
                                              {:id "p2-f1" :level 1 :type :fragment :title ""
                                               :context "" :draft "When coordination goes hourly, human translation layers become the bottleneck"
                                               :leftovers "" :collapsed? false}
                                              {:id "p3" :level 0 :type :chapter :title "The Mom Story — AI as Universal Translator"
                                               :context "Personal, emotional angle. Gene's 84-year-old mother."
                                               :draft "" :leftovers "" :collapsed? false}
                                              {:id "p3-f1" :level 1 :type :fragment :title ""
                                               :context "" :draft "Used ChatGPT to capture medical symptoms in Korean"
                                               :leftovers "" :collapsed? false}]
                                      :focus-idx 0}
                                     {:id "posts"
                                      :title "Posts"
                                      :nodes [{:id "nrc-dustin-warner" :level 0 :type :chapter :title "NRC / Dustin Warner"
                                               :context "" :draft "" :leftovers "" :collapsed? false}
                                              {:id "datastar-revelations" :level 0 :type :chapter :title "Datastar Revelations"
                                               :context "" :draft "" :leftovers "" :collapsed? false}
                                              {:id "eais-gaming" :level 0 :type :chapter :title "EAIS Gaming"
                                               :context "" :draft "" :leftovers "" :collapsed? false}
                                              {:id "edl-letter" :level 0 :type :chapter :title "EDL Letter"
                                               :context "" :draft "" :leftovers "" :collapsed? false}
                                              {:id "musicians-and-ai" :level 0 :type :chapter :title "Musicians and AI"
                                               :context "" :draft "" :leftovers "" :collapsed? false}
                                              {:id "post-primark" :level 0 :type :chapter :title "Post Primark"
                                               :context "" :draft "" :leftovers "" :collapsed? false}
                                              {:id "forum-problem-statements" :level 0 :type :chapter :title "Forum Problem Statements"
                                               :context "" :draft "" :leftovers "" :collapsed? false}]
                                      :focus-idx 0}
                                     {:id "done"
                                      :title "Done / Published"
                                      :nodes []
                                      :focus-idx 0}]
                          :active-project-idx 0
                          :editing-node nil
                          :orphan-expanded false
                          :imported-node-id nil}

           ;; UI
          :picked-texts #{} ;; cherry-pick keys (first 80 chars) — survives reload
          :cherry-pick-active false ;; Phase 3: server-owned cherry-pick toggle
          :ui {:active-top-tab :draft-chat
               :model :opus
               :fleet :balanced5
               :active-editor-tab :draft
               :notification nil
               :examples-collapsed? false
               :focused-field :none ;; :none | :title | :context | :draft | :chat (which input has browser focus)
               :nav-mode :none ;; :none | :question | :answer (derived from focus target)
               :question-index -1 ;; index into timeline questions (derived)
               :answer-index -1 ;; index into answers within focused question (derived)
               :focus-index -1 ;; flat index into visible-nav-items tree
               :collapsed #{} ;; set of collapsed question indices
               :history-focus-idx 0 ;; focused row in Draft History tab
               :ai-panel-open true} ;; AI panel visible (Distillery RIGHT pane)

           ;; Explorer pane (left-side project tree)
          :explorer {:visible? false :import-expanded? false}

           ;; Transform modal (Phase 4: server-owned)
          :transform {:active? false
                      :selected-text ""
                      :instruction ""
                      :options [] ;; [{:text "..." :model "opus" :elapsed-ms N}]
                      :option-index 0
                      :loading? false
                      :model :opus ;; :sonnet or :opus
                      :history []} ;; [{:before "old" :after "new" :instruction "..."}]

           ;; Examples (Phase 2+)
          :examples {:posts [] :sort-order :desc}})))

;; ---------------------------------------------------------------------------
;; Platform limits
;; ---------------------------------------------------------------------------

(def platform-limits
  {:twitter 280
   :linkedin 3000})

(>defn char-limit
       "Get character limit for current platform."
       ([]
        [=> int?]
        (char-limit (:platform @app-state)))
       ([platform]
        [keyword? => int?]
        (get platform-limits platform 3000)))

;; ---------------------------------------------------------------------------
;; Goal context tabs — separate atom (dynamic keys per goal)
;; ---------------------------------------------------------------------------

(defonce context-tabs-data (atom {}))

;; ---------------------------------------------------------------------------
;; Unified undo/redo stack (depth 100)
;; Separate atoms — transient, not persisted to session file.
;; ---------------------------------------------------------------------------

(def ^:private undo-max-depth 100)

(defonce undo-stack (atom [])) ;; past snapshots, most recent last
(defonce redo-stack (atom [])) ;; future snapshots (after undo), most recent last

(defn- capture-undo-snapshot
  "Capture current draft + distillery state for undo."
  [label]
  {:label label
   :timestamp (java.time.Instant/now)
   :draft (:draft @app-state)
   :distillery (:distillery @app-state)})

(defn push-undo!
  "Save current state to undo stack before a mutation.
   Clears redo stack (new action after undo = discard forward history).
   Call BEFORE the mutation, not after."
  [label]
  (let [snapshot (capture-undo-snapshot label)]
    (swap! undo-stack (fn [s] (vec (take-last undo-max-depth (conj s snapshot)))))
    (reset! redo-stack [])))

(defn pop-undo!
  "Undo: restore previous state from stack. Returns label or nil if empty."
  []
  (when (seq @undo-stack)
    (let [current (capture-undo-snapshot "present")
          prev (peek @undo-stack)]
      (swap! undo-stack pop)
      (swap! redo-stack conj current)
      (swap! app-state
             (fn [st]
               (-> st
                   (assoc :draft (:draft prev))
                   (assoc :distillery (:distillery prev)))))
      (:label prev))))

(defn pop-redo!
  "Redo: restore next state from redo stack. Returns label or nil if empty."
  []
  (when (seq @redo-stack)
    (let [current (capture-undo-snapshot "present")
          next-state (peek @redo-stack)]
      (swap! redo-stack pop)
      (swap! undo-stack conj current)
      (swap! app-state
             (fn [st]
               (-> st
                   (assoc :draft (:draft next-state))
                   (assoc :distillery (:distillery next-state)))))
      (:label next-state))))

(defn undo-depth
  "Number of undo entries available."
  []
  (count @undo-stack))

;; ---------------------------------------------------------------------------
;; State setters — every mutation goes through a named fn
;; Key paths are defined once here; callers never swap! directly.
;; ---------------------------------------------------------------------------

(declare log-event!)

;; === Draft sync (debounced from browser) ===

(>defn set-draft! [text]
       [string? => any?]
       (swap! app-state assoc :draft text))

(>defn add-picked-text! [key]
       [string? => any?]
       (swap! app-state update :picked-texts conj key))

(defn toggle-cherry-pick!
  "Toggle cherry-pick mode on/off. Returns new active state."
  []
  (let [new-state (swap! app-state update :cherry-pick-active not)]
    (:cherry-pick-active new-state)))

(>defn set-context! [text]
       [string? => any?]
       (swap! app-state assoc :context text))

(>defn set-cursor-pos! [pos]
       [int? => any?]
       (swap! app-state assoc :cursor-pos pos))

(defn sync-draft-tx
  "Pure: batch update draft fields — only updates keys present in the map.
   Guards against blanking non-empty fields."
  [st {:keys [draft context leftovers cursor-pos]}]
  (cond-> st
    ;; Guard: don't blank a non-empty draft
    (and draft (not (and (str/blank? draft) (not (str/blank? (:draft st))))))
    (assoc :draft draft)
    ;; Guard: don't blank non-empty context
    (and context (not (and (str/blank? context) (not (str/blank? (:context st))))))
    (assoc :context context)
    ;; Guard: don't blank non-empty leftovers
    (and leftovers (not (and (str/blank? leftovers) (not (str/blank? (:leftovers st))))))
    (assoc :leftovers leftovers)
    ;; Cursor position
    cursor-pos
    (assoc :cursor-pos cursor-pos)))

(>defn sync-draft!
       "Batch update draft fields atomically — only updates keys present in the map.
   Guards:
   1. Reject syncs from a stale browser whose state-version is behind the server's.
   2. Never overwrite a non-empty draft/context with an empty string.
   3. Project field is ignored — server owns the project (game engine pattern).
   See docs/datastar/2026-03-24-client-state-boofarama.md"
       [{:keys [draft context leftovers cursor-pos context-tabs state-version] :as params}]
       [map? => any?]
       ;; Guard: reject if browser's state-version is stale (permissive: nil = allow)
       (when (and state-version
                  (< state-version (:state-version @app-state)))
         (let [server-ver (:state-version @app-state)]
           (log/warn :sync-draft/stale-version
                     :browser-version state-version
                     :server-version server-ver
                     :draft-length (count (or draft ""))
                     :server-project (:project @app-state)
                     :action :rejected)
           (log-event! {:type "sync-draft.rejected"
                        :reason "stale-version"
                        :browser-version state-version
                        :server-version server-ver
                        :draft-preview (when draft (subs draft 0 (min 80 (count draft))))})
           (throw (ex-info (str "Stale sync rejected — browser version " state-version
                                " < server version " server-ver)
                           {:reason "stale-version"
                            :browser-version state-version
                            :server-version server-ver}))))
       ;; Single atomic swap for draft + context + cursor-pos (no interleaving)
       (let [prev-context (:context @app-state)]
         (swap! app-state sync-draft-tx params)
         ;; Log context change (outside swap — side effect)
         (when (and context (not= prev-context context) (not (str/blank? context)))
           (log-event! {:type "context.set" :length (count context) :text context})))
       ;; Context tabs (separate atom — still atomic within its own atom)
       (when context-tabs
         (doseq [[k v] context-tabs]
           (let [prev (get @context-tabs-data (keyword k))]
             (swap! context-tabs-data assoc (keyword k) v)
             (when (and (not= prev v) (not (str/blank? v)))
               (log-event! {:type "context-tab.set" :tab (name k) :length (count v) :text v}))))))

;; === Goal management ===

(defn- keyword-or-nil? [x] (or (nil? x) (keyword? x)))

(>defn set-goal! [goal-kw]
       [keyword-or-nil? => any?]
       (swap! app-state assoc :goal goal-kw))

(>defn set-context-tab-content!
       "Set content for a single context subtab."
       [tab-key content]
       [keyword? string? => any?]
       (swap! context-tabs-data assoc tab-key content))

(>defn set-all-context-tabs!
       "Replace all context tab contents at once."
       [tabs-map]
       [map? => any?]
       (reset! context-tabs-data (or tabs-map {})))

;; === Platform & UI nav ===

(>defn set-platform! [platform-kw]
       [keyword? => any?]
       (swap! app-state assoc :platform platform-kw))

(>defn set-editor-tab! [tab-kw]
       [keyword? => any?]
       (swap! app-state assoc-in [:ui :active-editor-tab] tab-kw))

(>defn set-top-tab! [tab-kw]
       [keyword? => any?]
       (transition! :set-top-tab {:tab tab-kw}
                    #(assoc-in % [:ui :active-top-tab] tab-kw)))

(defn toggle-orphan-expanded! []
  (swap! app-state update-in [:book-workshop :orphan-expanded] not))

(defn import-node-tx
  "Pure: insert a book node before Trash in the active project, set focus on it."
  [st bw-idx node]
  (let [nodes (get-in st [:book-workshop :projects bw-idx :nodes])
        trash-idx (or (first (keep-indexed
                              (fn [i n] (when (= "Trash" (:title n)) i))
                              nodes))
                      (count nodes))
        new-nodes (vec (concat (subvec nodes 0 trash-idx)
                               [node]
                               (subvec nodes trash-idx)))]
    (-> st
        (assoc-in [:book-workshop :projects bw-idx :nodes] new-nodes)
        (assoc-in [:book-workshop :projects bw-idx :focus-idx] trash-idx)
        (assoc-in [:book-workshop :imported-node-id] (:id node)))))

(defn import-node!
  "Import a node into the active book project (before Trash). Marks for glow."
  [node]
  (let [bw-idx (get-in @app-state [:book-workshop :active-project-idx])]
    (when bw-idx
      (swap! app-state import-node-tx bw-idx node))))

(>defn set-model! [model-kw]
       [keyword? => any?]
       (swap! app-state assoc-in [:ui :model] model-kw))

(>defn set-fleet! [fleet-kw]
       [keyword? => any?]
       (swap! app-state assoc-in [:ui :fleet] fleet-kw))

(defn toggle-examples-collapsed! []
  (swap! app-state update-in [:ui :examples-collapsed?] not))

;; === Explorer Pane State ===

(defn toggle-explorer! []
  (swap! app-state update-in [:explorer :visible?] not))

(defn explorer-visible? []
  (get-in @app-state [:explorer :visible?]))

(defn toggle-import-expanded! []
  (swap! app-state update-in [:explorer :import-expanded?] not))

;; === Question Nav State — pure tx functions + stateful wrappers ===

(defn enter-question-nav-tx
  "Pure: enter question nav mode, focus first question."
  [st question-count]
  (update st :ui assoc
          :nav-mode :question
          :question-index (if (pos? question-count) 0 -1)
          :answer-index -1))

(>defn enter-question-nav! [question-count]
       [int? => any?]
       (swap! app-state enter-question-nav-tx question-count))

(defn exit-nav-tx
  "Pure: exit question/answer nav mode."
  [st]
  (update st :ui assoc
          :nav-mode :none :question-index -1 :answer-index -1))

(defn exit-nav! []
  (swap! app-state exit-nav-tx))

(defn nav-set-question-tx
  "Pure: jump to a specific question index (from dropdown)."
  [st idx question-count]
  (let [clamped (max 0 (min idx (dec question-count)))]
    (update st :ui assoc
            :nav-mode :question
            :question-index clamped
            :answer-index -1)))

(>defn nav-set-question! [idx question-count]
       [int? int? => any?]
       (swap! app-state nav-set-question-tx idx question-count))

(defn nav-next-question-tx
  "Pure: move to next question (j key)."
  [st question-count]
  (update-in st [:ui :question-index]
             (fn [idx] (min (inc (or idx 0)) (dec question-count)))))

(>defn nav-next-question! [question-count]
       [int? => any?]
       (swap! app-state nav-next-question-tx question-count))

(defn nav-prev-question-tx
  "Pure: move to previous question (k key)."
  [st]
  (update-in st [:ui :question-index]
             (fn [idx] (max (dec (or idx 0)) 0))))

(defn nav-prev-question! []
  (swap! app-state nav-prev-question-tx))

(defn enter-answer-nav-tx
  "Pure: enter answer nav mode (Enter key on a question)."
  [st]
  (update st :ui assoc
          :nav-mode :answer :answer-index 0))

(defn enter-answer-nav! []
  (swap! app-state enter-answer-nav-tx))

(defn nav-click-answer-tx
  "Pure: jump directly to a specific answer."
  [st question-index answer-index question-count]
  (update st :ui assoc
          :nav-mode :answer
          :question-index (min question-index (dec question-count))
          :answer-index answer-index))

(>defn nav-click-answer! [question-index answer-index question-count]
       [int? int? int? => any?]
       (swap! app-state nav-click-answer-tx question-index answer-index question-count))

(defn nav-next-answer-tx
  "Pure: move to next answer (j key in answer mode)."
  [st answer-count]
  (update-in st [:ui :answer-index]
             (fn [idx] (min (inc (or idx 0)) (dec answer-count)))))

(>defn nav-next-answer! [answer-count]
       [int? => any?]
       (swap! app-state nav-next-answer-tx answer-count))

(defn nav-prev-answer-tx
  "Pure: move to previous answer (k key in answer mode)."
  [st]
  (update-in st [:ui :answer-index]
             (fn [idx] (max (dec (or idx 0)) 0))))

(defn nav-prev-answer! []
  (swap! app-state nav-prev-answer-tx))

(defn nav-toggle-collapse-tx
  "Pure: toggle collapse state for the given question index."
  [st qi]
  (if (and qi (>= qi 0))
    (update-in st [:ui :collapsed]
               (fn [s] (let [s (or s #{})]
                         (if (contains? s qi) (disj s qi) (conj s qi)))))
    st))

(defn nav-toggle-collapse! []
  (let [qi (get-in @app-state [:ui :question-index])]
    (swap! app-state nav-toggle-collapse-tx qi)))

;; === Tree-grid nav (flat traversal) ===

(defn nav-tree-move-tx
  "Pure: move focus in the flat visible tree."
  [st flat-items direction]
  (let [current (get-in st [:ui :focus-index] 0)
        new-idx (case direction
                  :next (min (inc current) (dec (count flat-items)))
                  :prev (max (dec current) 0)
                  current)
        target (nth flat-items new-idx nil)]
    (if target
      (update st :ui assoc
              :focus-index new-idx
              :question-index (:qi target)
              :answer-index (or (:ai target) -1)
              :nav-mode (if (= (:type target) :question) :question :answer))
      st)))

(>defn nav-tree-move! [flat-items direction]
       [sequential? keyword? => any?]
       (swap! app-state nav-tree-move-tx flat-items direction))

(defn nav-tree-enter!
  "Enter tree nav mode. Sets focus to first item."
  []
  (swap! app-state update :ui assoc
         :nav-mode :question
         :focus-index 0
         :question-index 0
         :answer-index -1))

(>defn nav-tree-select!
       "Jump to a specific flat index (from dropdown or click).
   `flat-items` is precomputed."
       [flat-items flat-idx]
       [sequential? int? => any?]
       (let [clamped (max 0 (min flat-idx (dec (count flat-items))))
             target (nth flat-items clamped nil)]
         (when target
           (swap! app-state update :ui assoc
                  :focus-index clamped
                  :question-index (:qi target)
                  :answer-index (or (:ai target) -1)
                  :nav-mode (if (= (:type target) :question) :question :answer)))))

(>defn nav-tree-click!
       "Click-to-select by qi/ai. Finds the matching flat index.
   `flat-items` is precomputed."
       [flat-items qi ai]
       [sequential? int? int? => any?]
       (let [target (if (neg? ai)
                      {:type :question :qi qi}
                      {:type :answer :qi qi :ai ai})
             idx (first (keep-indexed (fn [i item] (when (= item target) i)) flat-items))]
         (when idx
           (nav-tree-select! flat-items idx))))

(defn nav-tree-collapse!
  "Toggle collapse on the question containing the focused item.
   `flat-items` is precomputed. Returns updated flat-items for re-render."
  []
  (let [qi (get-in @app-state [:ui :question-index])]
    (when (and qi (>= qi 0))
      (swap! app-state update-in [:ui :collapsed]
             (fn [s] (let [s (or s #{})]
                       (if (contains? s qi) (disj s qi) (conj s qi))))))))

(defn nav-escape!
  "Escape: exit nav entirely."
  []
  (let [mode (get-in @app-state [:ui :nav-mode])]
    (case mode
      :answer (exit-nav!)
      :question (exit-nav!)
      nil)))

;; === Transform modal (Phase 4) ===

(defn transform-open!
  "Open transform modal with selected text."
  [selected-text]
  (swap! app-state assoc :transform
         {:active? true
          :selected-text (or selected-text "")
          :instruction ""
          :options []
          :option-index 0
          :loading? false
          :history (get-in @app-state [:transform :history] [])}))

(defn transform-close!
  "Close transform modal."
  []
  (swap! app-state update :transform assoc
         :active? false :options [] :option-index 0 :loading? false))

(defn transform-set-loading!
  "Set transform to loading state. Also sets active? true
   (modal may have been opened by JS before server knew about it)."
  []
  (swap! app-state update :transform assoc :active? true :loading? true :options []))

(defn transform-set-options!
  "Set transform options after Claude responds."
  [options]
  (swap! app-state update :transform assoc
         :options (vec options) :option-index 0 :loading? false))

(defn transform-select-option!
  "Navigate to a specific option index."
  [idx]
  (let [max-idx (dec (count (get-in @app-state [:transform :options])))]
    (swap! app-state update :transform assoc
           :option-index (max 0 (min idx max-idx)))))

(defn transform-apply-tx
  "Pure: apply selected transform option to draft.
   Returns [new-state :applied], [new-state :no-match], or [state :no-option]."
  [st]
  (let [{:keys [selected-text options option-index]} (:transform st)
        option (nth options option-index nil)]
    (if-not option
      [st :no-option]
      (if-not (str/includes? (:draft st) selected-text)
        [(update st :transform assoc
                 :active? false :options [] :option-index 0 :loading? false)
         :no-match]
        (let [new-draft (str/replace-first (:draft st) selected-text (:text option))]
          [(-> st
               (assoc :draft new-draft)
               (update-in [:transform :history]
                          (fn [h] (vec (take-last 10 (conj (or h [])
                                                           {:before selected-text
                                                            :after (:text option)
                                                            :instruction (get-in st [:transform :instruction] "")})))))
               (update :transform assoc
                       :active? false :options [] :option-index 0 :loading? false))
           :applied])))))

(defn transform-apply!
  "Apply selected option: update draft, record history, close modal.
   Returns :applied, :no-match, or :no-option."
  []
  (let [result (atom nil)]
    (swap! app-state
           (fn [st]
             (let [[new-st status] (transform-apply-tx st)]
               (reset! result status)
               new-st)))
    @result))

;; === Clear / Reset ===

(defn clear-draft! []
  (swap! app-state assoc :draft ""))

(defn clear-all!
  "Reset draft, context, goal, and chat history."
  []
  (swap! app-state assoc
         :draft "" :context "" :goal nil
         :chat {:history [] :loading? false})
  (reset! context-tabs-data {}))

(defn clear-chat-tx
  "Pure: clear AI panel — chat, critique, fanout, autocomplete.
   Preserves in-flight critique."
  [st]
  (let [critique (:critique st)
        {:keys [progress total]} critique
        critique-in-flight? (and total (pos? total) (< progress total))
        critique-reset {:progress 0 :total 0 :responses [] :synthesis nil
                        :synthesis-pills [] :timestamp nil :pending-requests []}]
    (assoc st
           :chat {:history [] :loading? false}
           :critique (if critique-in-flight? critique critique-reset)
           :fanout {:runs [] :active-run-idx nil :loading-models #{}}
           :autocomplete {:options [] :loading? false :error nil :timestamp nil})))

(defn clear-chat!
  "Clear the entire AI panel: chat, critique, fanout, autocomplete."
  []
  (swap! app-state clear-chat-tx))

;; === Distillery (Option Distillation Studio) ===

;; --- Pill format: supports both legacy strings and new maps ---
;; Old: "paragraph text"
;; New: {:text "paragraph text" :level 0}
;; normalize-pill converts at load boundary; pill-text extracts text everywhere.

(defn pill-text
  "Extract text from a pill (string or map)."
  [pill]
  (if (string? pill) pill (:text pill)))

(defn pill-level
  "Extract indent level from a pill (0 for legacy string pills)."
  [pill]
  (if (string? pill) 0 (or (:level pill) 0)))

(defn normalize-pill
  "Convert a pill to map format. Strings become {:text s :level 0 :collapsed? false}."
  [pill]
  (if (string? pill)
    {:text pill :level 0 :collapsed? false}
    (merge {:collapsed? false} pill)))

(defn make-pill
  "Create a new pill map."
  ([text] {:text text :level 0 :collapsed? false})
  ([text level] {:text text :level level :collapsed? false}))

(defn make-book-node
  "Create a book workshop node with all required fields.
   Single source of truth for the node schema — use this everywhere."
  [{:keys [id level type title context draft leftovers collapsed?]
    :or {id (str (UUID/randomUUID))
         level 0 type :section title "" context "" draft ""
         leftovers "" collapsed? false}}]
  {:id id :level level :type type :title title
   :context context :draft draft :leftovers leftovers :collapsed? collapsed?})

(defn- normalize-pills
  "Normalize a vector of pills (strings or maps) to all maps."
  [pills]
  (mapv normalize-pill pills))

(defn- distillery-left-length
  "Total char count of left paras joined by \\n\\n."
  [st]
  (let [paras (get-in st [:distillery :left-paras])]
    (if (empty? paras) 0
        (+ (reduce + (map (comp count pill-text) paras))
           (* 2 (dec (count paras)))))))

(defn- distillery-center-length
  "Total char count of center paras joined by \\n\\n."
  [st]
  (let [paras (get-in st [:distillery :center-paras])]
    (if (empty? paras) 0
        (+ (reduce + (map (comp count pill-text) paras))
           (* 2 (dec (count paras)))))))

(defn- record-spark!
  "Append a {left center} snapshot to spark-history. Max 30 points."
  []
  (let [st @app-state
        left (distillery-left-length st)
        center (distillery-center-length st)]
    (swap! app-state update-in [:distillery :spark-history]
           (fn [h] (vec (take-last 30 (conj (or h []) {:l left :c center})))))))

(defn- log-distillery-event!
  "Log event + record spark point."
  [event-map]
  (log-event! event-map)
  (record-spark!))

(defn- sync-draft-from-distillery!
  "Keep :draft in sync with distillery content.
   Uses center if non-empty (that's the post being built), otherwise left."
  [st]
  (let [center (get-in st [:distillery :center-paras])
        left (get-in st [:distillery :left-paras])]
    (assoc st :draft (if (seq center)
                       (str/join "\n\n" (map pill-text center))
                       (str/join "\n\n" (map pill-text left))))))

(defn- clamp-focus
  "Clamp focus-index to valid range for the focused column.
   When focused column is empty, switch focus to the other column."
  [st]
  (let [col (get-in st [:distillery :focus-col])
        paras (get-in st [:distillery (if (= col :center) :center-paras :left-paras)])
        other-col (if (= col :center) :left :center)
        other-paras (get-in st [:distillery (if (= other-col :center) :center-paras :left-paras)])]
    (if (empty? paras)
      (if (seq other-paras)
        (-> st
            (assoc-in [:distillery :focus-col] other-col)
            (assoc-in [:distillery :focus-index] (max 0 (dec (count other-paras)))))
        (-> st
            (assoc-in [:distillery :focus-col] :left)
            (assoc-in [:distillery :focus-index] 0)))
      (let [max-idx (max 0 (dec (count paras)))
            idx (get-in st [:distillery :focus-index])]
        (assoc-in st [:distillery :focus-index] (min idx max-idx))))))

(defn- snapshot-distillery
  "Push current state to unified undo stack. Called inside swap! but push-undo!
   reads @app-state (the pre-swap value), which is correct.
   Returns st unchanged (no longer writes :distillery-undo)."
  [st]
  ;; Side effect inside swap! is safe here: push-undo! is idempotent per action
  ;; and swap! retries are extremely rare (single-threaded HTTP handlers).
  (push-undo! "distillery")
  st)

(defn distillery-undo!
  "Undo last action using unified undo stack."
  []
  (when-let [label (pop-undo!)]
    (log-distillery-event! {:type "distillery.undo"
                            :restored label
                            :left-length (distillery-left-length @app-state)
                            :center-length (distillery-center-length @app-state)})))

(declare pill-children-end-idx)
(declare adopt-level)

(defn distillery-drag-pill-tx
  "Pure: move pill + children from (from-col, from-idx) to (to-col, to-idx)."
  [st from-col from-idx to-col to-idx]
  (let [from-key (if (= from-col :center) :center-paras :left-paras)
        to-key (if (= to-col :center) :center-paras :left-paras)
        from-paras (normalize-pills (get-in st [:distillery from-key]))
        pill (get from-paras from-idx)]
    (if (nil? pill) st
        (let [end-idx (pill-children-end-idx from-paras from-idx)
              block (subvec from-paras from-idx end-idx)
              block-size (count block)
              remaining (vec (concat (subvec from-paras 0 from-idx) (subvec from-paras end-idx)))]
          (if (= from-key to-key)
            ;; Same column: reorder
            (let [adj-idx (if (> to-idx from-idx)
                            (- to-idx block-size) to-idx)
                  clamped (max 0 (min adj-idx (count remaining)))
                  new-paras (vec (concat (subvec remaining 0 clamped) block (subvec remaining clamped)))]
              (-> st
                  snapshot-distillery
                  (assoc-in [:distillery from-key] new-paras)
                  (assoc-in [:distillery :focus-col] to-col)
                  (assoc-in [:distillery :focus-index] clamped)
                  (cond-> (= from-col :left) sync-draft-from-distillery!)))
            ;; Cross-column: remove from source, insert into target
            (let [to-paras (get-in st [:distillery to-key])
                  clamped (min to-idx (count to-paras))
                  block (adopt-level block to-paras clamped)
                  new-to (vec (concat (subvec to-paras 0 clamped) block (subvec to-paras clamped)))]
              (-> st
                  snapshot-distillery
                  (assoc-in [:distillery from-key] remaining)
                  (assoc-in [:distillery to-key] new-to)
                  (assoc-in [:distillery :focus-col] to-col)
                  (assoc-in [:distillery :focus-index] clamped)
                  sync-draft-from-distillery!)))))))

(defn- col->path
  "Map column keyword to the distillery path for its pills vector."
  [st col]
  (case col
    :bucket (let [bi (get-in st [:distillery :viewing-bucket])]
              [:distillery :buckets bi :pills])
    :center [:distillery :center-paras]
    [:distillery :left-paras]))

(>defn distillery-drag-pill!
       "Move pill + children from (from-col, from-idx) to (to-col, to-idx).
   Handles within-column reorder and cross-column moves (including bucket)."
       [from-col from-idx to-col to-idx]
       [keyword? int? keyword? int? => any?]
       (swap! app-state distillery-drag-pill-tx from-col from-idx to-col to-idx)
       (log-distillery-event! {:type "distillery.drag"
                               :from-col (name from-col) :from-idx from-idx
                               :to-col (name to-col) :to-idx to-idx
                               :left-length (distillery-left-length @app-state)
                               :center-length (distillery-center-length @app-state)}))

(defn distillery-apply-in-place-tx
  "Pure: scan CENTER pills for draft_quote and replace with fix.
   Returns [new-state :center] if found, [state nil] if not."
  [st draft_quote fix]
  (let [center (get-in st [:distillery :center-paras])
        idx (when draft_quote
              (first (keep-indexed
                      (fn [i para]
                        (when (str/includes? (pill-text para) draft_quote) i))
                      center)))]
    (if idx
      [(-> st
           snapshot-distillery
           (update-in [:distillery :center-paras idx]
                      (fn [pill]
                        (if (string? pill)
                          (str/replace-first pill draft_quote fix)
                          (update pill :text str/replace-first draft_quote fix))))
           sync-draft-from-distillery!)
       :center]
      [st nil])))

(>defn distillery-apply-in-place!
       "Scan CENTER pills for draft_quote and replace with fix in-place.
   Also syncs :draft from distillery after replacement.
   Returns :center if found in center, nil if not found."
       [draft_quote fix]
       [string? string? => any?]
       (let [found (atom nil)]
         (swap! app-state
                (fn [st]
                  (let [[new-st result] (distillery-apply-in-place-tx st draft_quote fix)]
                    (reset! found result)
                    new-st)))
         @found))

(declare rebuild-ai-paragraphs! build-ai-paragraphs)

(defn enter-distillery-tx
  "Pure: split current draft into left-paras, clear center."
  [st]
  (let [draft (:draft st)
        paras (if (str/blank? draft) []
                  (mapv make-pill (remove str/blank? (str/split draft #"\n\n+"))))]
    (-> st
        (assoc-in [:distillery :left-paras] paras)
        (assoc-in [:distillery :center-paras] [])
        (assoc-in [:distillery :focus-col] :left)
        (assoc-in [:distillery :focus-index] 0)
        (assoc-in [:distillery :editing?] false)
        ;; Reset ephemeral bucket state; keep :leftovers-pills (persistent)
        (assoc-in [:distillery :buckets] [])
        (assoc-in [:distillery :trash-pills] [])
        (assoc-in [:distillery :active-bucket] nil)
        (update :state-version inc))))

(defn import-latest-fanout-to-distillery!
  "Copy the latest fanout run's results into :distillery :ai-responses.
   Converts fanout format {model [{:text :elapsed-ms}]} → distillery format [{:model :text :elapsed-ms}].
   Only imports if distillery has no AI responses yet (don't clobber existing distillery work).
   Returns count of imported paragraphs, or 0 if nothing imported."
  []
  (let [st @app-state
        runs (get-in st [:fanout :runs] [])
        active-idx (get-in st [:fanout :active-run-idx])
        run (when active-idx (get runs active-idx))
        existing-ai (get-in st [:distillery :ai-responses])]
    (if-not (and run (empty? existing-ai))
      0
      (let [results (:results run {})
            model-order (or (:model-order run) (keys results))
            ai-responses (vec (for [model model-order
                                    shot (get results model [])]
                                {:model (name model)
                                 :text (:text shot)
                                 :elapsed-ms (or (:elapsed-ms shot) 0)
                                 :response-id (or (:response-id shot) (short-id))
                                 :timestamp (or (:timestamp shot) (str (java.time.Instant/now)))}))]
        (if (empty? ai-responses)
          0
          (do (swap! app-state assoc-in [:distillery :ai-responses] ai-responses)
              (rebuild-ai-paragraphs!)
              (log-event! {:type "distillery.import-fanout"
                           :response-count (count ai-responses)
                           :from-run (:id run)})
              (count (get-in @app-state [:distillery :ai-paragraphs]))))))))

(defn enter-distillery! []
  (swap! app-state enter-distillery-tx)
  (import-latest-fanout-to-distillery!)
  (log-distillery-event! {:type "distillery.enter"
                          :left-length (distillery-left-length @app-state)
                          :center-length 0
                          :pill-count (count (get-in @app-state [:distillery :left-paras]))}))

(defn- adopt-level
  "Adjust block levels so the first pill matches the level of the card above the insert point.
   Children maintain their relative depth. If inserting at top or into empty lane, first pill becomes L0."
  [block target-paras insert-at]
  (let [above-level (if (pos? insert-at)
                      (pill-level (nth target-paras (dec insert-at)))
                      0)
        current-level (pill-level (first block))
        delta (- above-level current-level)]
    (if (zero? delta)
      block
      (mapv (fn [pill] (update pill :level #(max 0 (+ % delta)))) block))))

(defn distillery-move-to-center-tx
  "Pure: move pill + children at idx from left to center."
  [st idx]
  (let [paras (normalize-pills (get-in st [:distillery :left-paras]))
        para (get paras idx)]
    (if (nil? para) st
        (let [end-idx (pill-children-end-idx paras idx)
              block (subvec paras idx end-idx)
              center (get-in st [:distillery :center-paras])
              center-mem (get-in st [:distillery :center-focus-index] 0)
              insert-at (if (empty? center)
                          0
                          (min (inc center-mem) (count center)))
              adopted-block (adopt-level block center insert-at)
              new-left-count (- (count paras) (count block))
              left-focus (min idx (max 0 (dec new-left-count)))]
          (-> st
              snapshot-distillery
              (assoc-in [:distillery :left-paras]
                        (vec (concat (subvec paras 0 idx) (subvec paras end-idx))))
              (assoc-in [:distillery :center-paras]
                        (vec (concat (subvec center 0 insert-at) adopted-block (subvec center insert-at))))
              (assoc-in [:distillery :center-focus-index] insert-at)
              (assoc-in [:distillery :left-focus-index] left-focus)
              (assoc-in [:distillery :focus-col] :left)
              (assoc-in [:distillery :focus-index] left-focus)
              sync-draft-from-distillery!)))))

(>defn distillery-move-to-center!
       "Move pill + children at idx from left to center (after center's remembered focus position). Sync draft."
       [idx]
       [int? => any?]
       (swap! app-state distillery-move-to-center-tx idx)
       (log-distillery-event! {:type "distillery.merge"
                               :left-length (distillery-left-length @app-state)
                               :center-length (distillery-center-length @app-state)}))

(defn distillery-unmerge-tx
  "Pure: move pill + children at idx from center to left."
  [st idx]
  (let [center (normalize-pills (get-in st [:distillery :center-paras]))
        para (get center idx)]
    (if (nil? para) st
        (let [end-idx (pill-children-end-idx center idx)
              block (subvec center idx end-idx)
              left (get-in st [:distillery :left-paras])
              left-mem (get-in st [:distillery :left-focus-index] 0)
              insert-at (min (inc left-mem) (count left))
              adopted-block (adopt-level block left insert-at)
              new-center-count (- (count center) (count block))
              center-focus (min idx (max 0 (dec new-center-count)))]
          (-> st
              snapshot-distillery
              (assoc-in [:distillery :center-paras]
                        (vec (concat (subvec center 0 idx) (subvec center end-idx))))
              (assoc-in [:distillery :left-paras]
                        (vec (concat (subvec left 0 insert-at) adopted-block (subvec left insert-at))))
              (assoc-in [:distillery :left-focus-index] insert-at)
              (assoc-in [:distillery :center-focus-index] center-focus)
              (assoc-in [:distillery :focus-col] :center)
              (assoc-in [:distillery :focus-index] center-focus)
              sync-draft-from-distillery!)))))

(>defn distillery-unmerge!
       "Move pill + children at idx from center to left (after left's remembered focus position). Sync draft."
       [idx]
       [int? => any?]
       (swap! app-state distillery-unmerge-tx idx)
       (log-distillery-event! {:type "distillery.unmerge"
                               :left-length (distillery-left-length @app-state)
                               :center-length (distillery-center-length @app-state)}))

(defn distillery-move-all-to-center!
  "Move all left pills to center (append). Clears left."
  []
  (swap! app-state
         (fn [st]
           (let [left (get-in st [:distillery :left-paras])
                 center (get-in st [:distillery :center-paras])]
             (-> st
                 snapshot-distillery
                 (assoc-in [:distillery :center-paras] (vec (concat center left)))
                 (assoc-in [:distillery :left-paras] [])
                 (assoc-in [:distillery :focus-col] :center)
                 (assoc-in [:distillery :focus-index] 0)
                 sync-draft-from-distillery!))))
  (log-distillery-event! {:type "distillery.move-all"
                          :left-length 0
                          :center-length (distillery-center-length @app-state)}))

;; --- Bucket operations (manuscript + workbenches) ---

(>defn distillery-pluck-to-bucket!
       "Move pill (+ children) from manuscript at idx to bucket n. Auto-create bucket if needed."
       [idx bucket-n]
       [int? int? => any?]
       (swap! app-state
              (fn [st]
                (let [paras (normalize-pills (get-in st [:distillery :left-paras]))
                      para (get paras idx)]
                  (if (nil? para) st
                      (let [end-idx (pill-children-end-idx paras idx)
                            block (subvec paras idx end-idx)
                            buckets (get-in st [:distillery :buckets])
                            ;; Auto-create buckets up to bucket-n
                            buckets (loop [b buckets]
                                      (if (> (count b) bucket-n)
                                        b
                                        (recur (conj b {:id (str (random-uuid)) :pills []}))))
                            ;; Flatten to level 0 — buckets are flat staging areas
                            flat-block (mapv #(assoc % :level 0) block)
                            ;; Append block to target bucket
                            updated-buckets (update-in buckets [bucket-n :pills]
                                                       (fn [pills] (vec (concat pills flat-block))))
                            ;; Remove block from manuscript
                            new-left (vec (concat (subvec paras 0 idx) (subvec paras end-idx)))
                            new-focus (min idx (max 0 (dec (count new-left))))]
                        (-> st
                            snapshot-distillery
                            (assoc-in [:distillery :left-paras] new-left)
                            (assoc-in [:distillery :buckets] updated-buckets)
                            (assoc-in [:distillery :active-bucket] bucket-n)
                            (assoc-in [:distillery :focus-index] new-focus)
                            (assoc-in [:distillery :left-focus-index] new-focus)
                            sync-draft-from-distillery!)))))))

(>defn distillery-pluck-to-active-bucket!
       "Pluck to whatever bucket was last used (> shortcut). Creates bucket 0 if none."
       [idx]
       [int? => any?]
       (let [active (or (get-in @app-state [:distillery :active-bucket]) 0)]
         (distillery-pluck-to-bucket! idx active)))

(>defn distillery-pluck-to-leftovers!
       "Move pill (+ children) from manuscript to leftovers."
       [idx]
       [int? => any?]
       (swap! app-state
              (fn [st]
                (let [paras (normalize-pills (get-in st [:distillery :left-paras]))
                      para (get paras idx)]
                  (if (nil? para) st
                      (let [end-idx (pill-children-end-idx paras idx)
                            block (subvec paras idx end-idx)
                            ;; Find parent section: walk backwards from idx to find level-0 ancestor
                            section-name (loop [i (dec idx)]
                                           (cond
                                             (neg? i) ""
                                             (zero? (pill-level (nth paras i))) (pill-text (nth paras i))
                                             :else (recur (dec i))))
                            ;; Build leftover entries from block
                            now-str (str (java.time.Instant/now))
                            leftover-items (mapv (fn [pill]
                                                   {:text (pill-text pill)
                                                    :from-section section-name
                                                    :stashed-at now-str})
                                                 block)
                            new-left (vec (concat (subvec paras 0 idx) (subvec paras end-idx)))
                            new-focus (min idx (max 0 (dec (count new-left))))]
                        (-> st
                            snapshot-distillery
                            (assoc-in [:distillery :left-paras] new-left)
                            (update-in [:distillery :leftovers-pills] (fn [lf] (vec (concat lf leftover-items))))
                            (assoc-in [:distillery :focus-index] new-focus)
                            (assoc-in [:distillery :left-focus-index] new-focus)
                            sync-draft-from-distillery!)))))))

(>defn distillery-pluck-to-trash!
       "Soft-delete pill (+ children) from manuscript to trash."
       [idx]
       [int? => any?]
       (swap! app-state
              (fn [st]
                (let [paras (normalize-pills (get-in st [:distillery :left-paras]))
                      para (get paras idx)]
                  (if (nil? para) st
                      (let [end-idx (pill-children-end-idx paras idx)
                            block (subvec paras idx end-idx)
                            trash-items (mapv (fn [pill]
                                                {:text (pill-text pill)
                                                 :trashed-at (str (java.time.Instant/now))})
                                              block)
                            new-left (vec (concat (subvec paras 0 idx) (subvec paras end-idx)))
                            new-focus (min idx (max 0 (dec (count new-left))))]
                        (-> st
                            snapshot-distillery
                            (assoc-in [:distillery :left-paras] new-left)
                            (update-in [:distillery :trash-pills] (fn [t] (vec (concat t trash-items))))
                            (assoc-in [:distillery :focus-index] new-focus)
                            (assoc-in [:distillery :left-focus-index] new-focus)
                            sync-draft-from-distillery!)))))))

(>defn distillery-merge-bucket!
       "Insert all pills from bucket n into manuscript after focus position. Empty the bucket."
       [bucket-n]
       [int? => any?]
       (swap! app-state
              (fn [st]
                (let [buckets (get-in st [:distillery :buckets])]
                  (if (or (>= bucket-n (count buckets))
                          (empty? (get-in buckets [bucket-n :pills])))
                    st
                    (let [bucket-pills (get-in buckets [bucket-n :pills])
                          paras (get-in st [:distillery :left-paras])
                          focus-idx (get-in st [:distillery :focus-index] 0)
                          ;; Insert after focused pill (or at end if past bounds)
                          insert-at (min (inc focus-idx) (count paras))
                          new-left (vec (concat (subvec paras 0 insert-at)
                                                bucket-pills
                                                (subvec paras insert-at)))
                          ;; Remove the bucket (shift remaining buckets)
                          new-buckets (vec (concat (subvec buckets 0 bucket-n)
                                                   (subvec buckets (inc bucket-n))))]
                      (-> st
                          snapshot-distillery
                          (assoc-in [:distillery :left-paras] new-left)
                          (assoc-in [:distillery :buckets] new-buckets)
                          ;; Focus on last inserted pill
                          (assoc-in [:distillery :focus-index] (+ insert-at (dec (count bucket-pills))))
                          (assoc-in [:distillery :left-focus-index] (+ insert-at (dec (count bucket-pills))))
                          ;; Adjust active-bucket if needed
                          (update-in [:distillery :active-bucket]
                                     (fn [ab]
                                       (when ab
                                         (cond
                                           (= ab bucket-n) nil
                                           (> ab bucket-n) (dec ab)
                                           :else ab))))
                          sync-draft-from-distillery!)))))))

(>defn distillery-untrash!
       "Move pill from trash back to manuscript at focus position."
       [trash-idx]
       [int? => any?]
       (swap! app-state
              (fn [st]
                (let [trash (get-in st [:distillery :trash-pills])]
                  (if (or (empty? trash) (>= trash-idx (count trash)))
                    st
                    (let [item (nth trash trash-idx)
                          pill (make-pill (:text item))
                          paras (get-in st [:distillery :left-paras])
                          focus-idx (get-in st [:distillery :focus-index] 0)
                          insert-at (min (inc focus-idx) (count paras))
                          new-left (vec (concat (subvec paras 0 insert-at)
                                                [pill]
                                                (subvec paras insert-at)))
                          new-trash (vec (concat (subvec trash 0 trash-idx)
                                                 (subvec trash (inc trash-idx))))]
                      (-> st
                          snapshot-distillery
                          (assoc-in [:distillery :left-paras] new-left)
                          (assoc-in [:distillery :trash-pills] new-trash)
                          (assoc-in [:distillery :focus-index] insert-at)
                          (assoc-in [:distillery :left-focus-index] insert-at)
                          sync-draft-from-distillery!)))))))

;; ---------------------------------------------------------------------------
;; AI cherry-pick to buckets (Phase 4) — COPY, not move
;; ---------------------------------------------------------------------------

(>defn distillery-copy-ai-para-to-bucket!
       "Copy AI paragraph at flat-idx to bucket n. Does NOT remove from ai-paragraphs.
        Auto-creates bucket, auto-advances focus, auto-enables bucket mode."
       [flat-idx bucket-n]
       [int? int? => any?]
       (swap! app-state
              (fn [st]
                (let [paras (get-in st [:distillery :ai-paragraphs])
                      para (get paras flat-idx)]
                  (if (nil? para) st
                      (let [pill {:text (:text para) :level 0}
                            buckets (get-in st [:distillery :buckets])
                            ;; Auto-create buckets up to bucket-n
                            buckets (loop [b buckets]
                                      (if (> (count b) bucket-n) b
                                          (recur (conj b {:id (str (random-uuid)) :pills []}))))
                            updated-buckets (update-in buckets [bucket-n :pills] conj pill)
                            ;; Auto-advance focus to next paragraph
                            new-focus (min (inc flat-idx) (max 0 (dec (count paras))))]
                        (-> st
                            (assoc-in [:distillery :buckets] updated-buckets)
                            (assoc-in [:distillery :active-bucket] bucket-n)
                            (assoc-in [:distillery :bucket-mode?] true)
                            (assoc-in [:distillery :focus-index] new-focus)
                            ;; Mark paragraph as picked
                            (assoc-in [:distillery :ai-paragraphs flat-idx :picked?] true))))))))

(>defn distillery-copy-ai-para-to-leftovers!
       "Copy AI paragraph at flat-idx to leftovers."
       [flat-idx]
       [int? => any?]
       (swap! app-state
              (fn [st]
                (let [paras (get-in st [:distillery :ai-paragraphs])
                      para (get paras flat-idx)]
                  (if (nil? para) st
                      (let [now-str (str (java.time.Instant/now))
                            item {:text (:text para)
                                  :from-section (str "AI:" (:model para))
                                  :stashed-at now-str}
                            new-focus (min (inc flat-idx) (max 0 (dec (count paras))))]
                        (-> st
                            (update-in [:distillery :leftovers-pills] conj item)
                            (assoc-in [:distillery :focus-index] new-focus)
                            (assoc-in [:distillery :ai-paragraphs flat-idx :picked?] true))))))))

(>defn distillery-copy-ai-para-to-trash!
       "Copy AI paragraph at flat-idx to trash (discard)."
       [flat-idx]
       [int? => any?]
       (swap! app-state
              (fn [st]
                (let [paras (get-in st [:distillery :ai-paragraphs])
                      para (get paras flat-idx)]
                  (if (nil? para) st
                      (let [item {:text (:text para) :trashed-at (str (java.time.Instant/now))}
                            new-focus (min (inc flat-idx) (max 0 (dec (count paras))))]
                        (-> st
                            (update-in [:distillery :trash-pills] conj item)
                            (assoc-in [:distillery :focus-index] new-focus)
                            (assoc-in [:distillery :ai-paragraphs flat-idx :picked?] true))))))))

(defn distillery-paste-full-ai-response!
  "Insert the full AI response (identified by current right-focus paragraph) into draft."
  []
  (let [st @app-state
        paras (get-in st [:distillery :ai-paragraphs])
        focus-idx (get-in st [:distillery :focus-index])
        para (get paras focus-idx)]
    (when para
      (let [responses (get-in st [:distillery :ai-responses])
            resp (get responses (:response-idx para))
            full-text (:text resp)]
        (push-undo! "paste-full-response")
        (swap! app-state update :draft
               (fn [d] (str d (when (and d (not (.endsWith ^String (or d "") "\n\n"))) "\n\n") full-text)))))))

(defn distillery-toggle-bucket-mode!
  "Toggle between bucket mode (manuscript + workbenches) and classic 2-lane."
  []
  (swap! app-state update-in [:distillery :bucket-mode?] not))

(defn distillery-view-bucket!
  "Zoom into a bucket to see its full contents. nil = back to grid."
  [bucket-idx]
  (swap! app-state assoc-in [:distillery :viewing-bucket] bucket-idx))

(defn distillery-import-bucket!
  "Create a new bucket from external text. Splits on double-newline into pills (level 0).
   Enables bucket mode if not already on."
  [label text]
  (let [pills (mapv make-pill (remove str/blank? (str/split text #"\n\n+")))]
    (swap! app-state
           (fn [st]
             (let [bucket {:id (str (random-uuid)) :pills pills}
                   buckets (get-in st [:distillery :buckets])
                   new-idx (count buckets)]
               (-> st
                   (update-in [:distillery :buckets] conj bucket)
                   (assoc-in [:distillery :bucket-mode?] true)
                   (assoc-in [:distillery :active-bucket] new-idx)))))))

(>defn distillery-delete-para!
       "Delete pill at idx from the specified column. Sync draft if left."
       [col idx]
       [keyword? int? => any?]
       (swap! app-state
              (fn [st]
                (let [path (col->path st col)
                      paras (get-in st path)]
                  (if (or (nil? (get paras idx)) (empty? paras)) st
                      (-> st
                          snapshot-distillery
                          (assoc-in path
                                    (vec (concat (subvec paras 0 idx) (subvec paras (inc idx)))))
                          clamp-focus
                          (cond-> (= col :left) sync-draft-from-distillery!))))))
       (log-event! {:type (str "distillery.delete-" (name col))
                    :left-length (distillery-left-length @app-state)
                    :center-length (distillery-center-length @app-state)}))

(defn distillery-reorder-tx
  "Pure: move pill at idx in direction (-1 = up, +1 = down) within column."
  [st col idx direction]
  (let [col-key (if (= col :center) :center-paras :left-paras)
        paras (get-in st [:distillery col-key])
        target (+ idx direction)]
    (if (or (< target 0) (>= target (count paras))) st
        (let [a (get paras idx)
              b (get paras target)
              new-paras (-> paras (assoc idx b) (assoc target a))]
          (-> st
              snapshot-distillery
              (assoc-in [:distillery col-key] new-paras)
              (assoc-in [:distillery :focus-index] target)
              (cond-> (= col :left) sync-draft-from-distillery!))))))

(>defn distillery-reorder!
       "Move pill at idx in direction (-1 = up, +1 = down) within column. Sync draft if left."
       [col idx direction]
       [keyword? int? int? => any?]
       (swap! app-state distillery-reorder-tx col idx direction)
       (log-distillery-event! {:type "distillery.reorder"
                               :col (name col) :direction direction
                               :left-length (distillery-left-length @app-state)
                               :center-length (distillery-center-length @app-state)}))

;; --- Outline operations (ported from book workshop) ---

(defn- pill-children-end-idx
  "Return the index just past the last child of pill at idx.
   Children are consecutive pills with level > pill's level."
  [pills idx]
  (let [level (pill-level (nth pills idx))]
    (loop [i (inc idx)]
      (if (or (>= i (count pills)) (<= (pill-level (nth pills i)) level))
        i
        (recur (inc i))))))

(defn distillery-indent-tx
  "Pure: indent focused pill + children in column."
  [st col idx]
  (let [col-key (if (= col :center) :center-paras :left-paras)
        paras (get-in st [:distillery col-key])
        pill (get paras idx)
        end-idx (pill-children-end-idx paras idx)
        max-child-level (apply max (map pill-level (subvec paras idx end-idx)))]
    (cond
      (zero? idx) st
      (>= max-child-level 3) st
      (not (<= (pill-level pill) (pill-level (nth paras (dec idx))))) st
      :else
      (-> st
          snapshot-distillery
          (update-in [:distillery col-key]
                     (fn [ps]
                       (reduce (fn [acc i]
                                 (update-in acc [i] #(update % :level inc)))
                               (normalize-pills ps) (range idx end-idx))))
          (cond-> (= col :left) sync-draft-from-distillery!)))))

(>defn distillery-indent! [col idx]
       [keyword? int? => any?]
       (swap! app-state distillery-indent-tx col idx)
       (log-distillery-event! {:type "distillery.indent" :col (name col) :idx idx}))

(defn distillery-outdent-tx
  "Pure: outdent focused pill + children in column."
  [st col idx]
  (let [col-key (if (= col :center) :center-paras :left-paras)
        paras (get-in st [:distillery col-key])
        pill (get paras idx)
        end-idx (pill-children-end-idx paras idx)]
    (if (zero? (pill-level pill))
      st
      (-> st
          snapshot-distillery
          (update-in [:distillery col-key]
                     (fn [ps]
                       (reduce (fn [acc i]
                                 (update-in acc [i] #(update % :level dec)))
                               (normalize-pills ps) (range idx end-idx))))
          (cond-> (= col :left) sync-draft-from-distillery!)))))

(>defn distillery-outdent! [col idx]
       [keyword? int? => any?]
       (swap! app-state distillery-outdent-tx col idx)
       (log-distillery-event! {:type "distillery.outdent" :col (name col) :idx idx}))

(>defn distillery-toggle-collapse!
       "Toggle collapsed state of focused pill in column."
       [col idx]
       [keyword? int? => any?]
       (swap! app-state
              (fn [st]
                (let [col-key (if (= col :center) :center-paras :left-paras)
                      paras (get-in st [:distillery col-key])
                      pill (get paras idx)
                      end-idx (pill-children-end-idx paras idx)]
                  ;; Only toggle if pill has children
                  (if (= end-idx (inc idx))
                    st ;; No children, nothing to collapse
                    (-> st
                        (update-in [:distillery col-key]
                                   (fn [ps]
                                     (let [nps (normalize-pills ps)]
                                       (update-in nps [idx] #(update % :collapsed? not))))))))))
       (log-distillery-event! {:type "distillery.toggle-collapse" :col (name col) :idx idx}))

(defn visible-pills
  "Filter pills to only show visible ones (excluding children of collapsed pills).
   Returns vector of [original-idx pill] pairs to preserve indexing."
  [pills]
  (loop [i 0 result [] skip-until -1]
    (if (>= i (count pills))
      result
      (let [pill (nth pills i)]
        (if (< i skip-until)
          (recur (inc i) result skip-until)
          (let [collapsed? (:collapsed? pill)
                end-idx (if collapsed? (pill-children-end-idx pills i) -1)]
            (recur (inc i)
                   (conj result [i pill])
                   (if collapsed? end-idx skip-until))))))))

(defn distillery-reorder-outline-tx
  "Pure: move pill + children block up/down within column, respecting hierarchy."
  [st col idx direction]
  (let [col-key (if (= col :center) :center-paras :left-paras)
        paras (normalize-pills (get-in st [:distillery col-key]))
        pill (get paras idx)
        level (pill-level pill)
        end-idx (pill-children-end-idx paras idx)
        node-block (subvec paras idx end-idx)]
    (case direction
      -1 ;; Move up
      (let [prev-idx (loop [i (dec idx)]
                       (cond
                         (< i 0) nil
                         (= (pill-level (nth paras i)) level) i
                         (< (pill-level (nth paras i)) level) nil
                         :else (recur (dec i))))]
        (if prev-idx
          (let [prev-block (subvec paras prev-idx idx)
                before (subvec paras 0 prev-idx)
                after (subvec paras end-idx)
                new-paras (vec (concat before node-block prev-block after))]
            (-> st
                snapshot-distillery
                (assoc-in [:distillery col-key] new-paras)
                (assoc-in [:distillery :focus-index] prev-idx)
                (cond-> (= col :left) sync-draft-from-distillery!)))
          st))
      1 ;; Move down
      (let [next-idx (loop [i end-idx]
                       (cond
                         (>= i (count paras)) nil
                         (= (pill-level (nth paras i)) level) i
                         (< (pill-level (nth paras i)) level) nil
                         :else (recur (inc i))))]
        (if next-idx
          (let [next-end (pill-children-end-idx paras next-idx)
                next-block (subvec paras next-idx next-end)
                before (subvec paras 0 idx)
                after (subvec paras next-end)
                new-paras (vec (concat before next-block node-block after))]
            (-> st
                snapshot-distillery
                (assoc-in [:distillery col-key] new-paras)
                (assoc-in [:distillery :focus-index] (+ idx (count next-block)))
                (cond-> (= col :left) sync-draft-from-distillery!)))
          ;; No same-level sibling — try crossing parent boundary
          (if (and (< end-idx (count paras))
                   (< (pill-level (nth paras end-idx)) level))
            (let [parent-idx end-idx
                  before (subvec paras 0 idx)
                  between (subvec paras end-idx (inc parent-idx))
                  after (subvec paras (inc parent-idx))
                  new-paras (vec (concat before between node-block after))
                  new-focus (+ (count before) (count between))]
              (-> st
                  snapshot-distillery
                  (assoc-in [:distillery col-key] new-paras)
                  (assoc-in [:distillery :focus-index] new-focus)
                  (cond-> (= col :left) sync-draft-from-distillery!)))
            st)))
      st)))

(>defn distillery-reorder-outline!
       "Move pill + children block up/down within column, respecting hierarchy."
       [col idx direction]
       [keyword? int? int? => any?]
       (swap! app-state distillery-reorder-outline-tx col idx direction)
       (log-distillery-event! {:type "distillery.reorder-outline"
                               :col (name col) :direction direction
                               :left-length (distillery-left-length @app-state)
                               :center-length (distillery-center-length @app-state)}))

(>defn distillery-add-pill!
       "Insert new pill with text at position idx in column. Sync draft if left."
       [col idx text]
       [keyword? int? string? => any?]
       (swap! app-state
              (fn [st]
                (let [p (col->path st col)
                      paras (get-in st p)
                      clamped-idx (min idx (count paras))
                      new-paras (vec (concat (subvec paras 0 clamped-idx)
                                             [(make-pill text)]
                                             (subvec paras clamped-idx)))]
                  (-> st
                      (assoc-in p new-paras)
                      (assoc-in [:distillery :focus-index] clamped-idx)
                      (cond-> (= col :left) sync-draft-from-distillery!))))))

(defn distillery-edit-pill-tx
  "Pure: replace pill text at idx in column."
  [st col idx new-text]
  (let [col-key (if (= col :center) :center-paras :left-paras)
        old-pill (get-in st [:distillery col-key idx])
        new-pill (if (string? old-pill)
                   (make-pill new-text)
                   (assoc old-pill :text new-text))]
    (-> st
        snapshot-distillery
        (assoc-in [:distillery col-key idx] new-pill)
        (cond-> (= col :left) sync-draft-from-distillery!))))

(>defn distillery-edit-pill!
       "Replace pill text at idx in column. Sync draft if left."
       [col idx new-text]
       [keyword? int? string? => any?]
       (swap! app-state distillery-edit-pill-tx col idx new-text)
       (log-distillery-event! {:type "distillery.edit-pill"
                               :col (name col) :idx idx
                               :new-length (count new-text)
                               :left-length (distillery-left-length @app-state)
                               :center-length (distillery-center-length @app-state)}))

(>defn distillery-start-edit!
       "Mark a pill as being edited. Server renders textarea instead of div."
       [col idx]
       [keyword? int? => any?]
       (swap! app-state
              (fn [st]
                (-> st
                    (assoc-in [:distillery :editing?] true)
                    (assoc-in [:distillery :edit-col] col)
                    (assoc-in [:distillery :edit-idx] idx)
                    (assoc-in [:distillery :focus-col] col)
                    (assoc-in [:distillery :focus-index] idx)))))

(>defn distillery-stop-edit!
       "Clear editing state."
       []
       [=> any?]
       (swap! app-state
              (fn [st]
                (-> st
                    (assoc-in [:distillery :editing?] false)
                    (assoc-in [:distillery :edit-col] nil)
                    (assoc-in [:distillery :edit-idx] nil)))))

(defn distillery-split-pill-tx
  "Pure: split a pill on every newline boundary — each line becomes its own pill."
  [st col idx]
  (let [col-key (if (= col :center) :center-paras :left-paras)
        paras (get-in st [:distillery col-key])
        text (get paras idx)]
    (if (nil? text)
      st
      (let [pill-lvl (pill-level text)
            chunks (mapv #(make-pill % pill-lvl)
                         (remove str/blank? (str/split (pill-text text) #"\n")))]
        (if (<= (count chunks) 1)
          st
          (let [new-paras (vec (concat (subvec paras 0 idx)
                                       chunks
                                       (subvec paras (inc idx))))]
            (-> st
                snapshot-distillery
                (assoc-in [:distillery col-key] new-paras)
                (cond-> (= col :left) sync-draft-from-distillery!))))))))

(>defn distillery-split-pill!
       "Split a pill on every newline boundary — each line becomes its own pill."
       [col idx]
       [keyword? int? => any?]
       (swap! app-state distillery-split-pill-tx col idx)
       (log-distillery-event! {:type "distillery.split"
                               :col (name col) :idx idx
                               :left-length (distillery-left-length @app-state)
                               :center-length (distillery-center-length @app-state)}))

(defn distillery-set-focus-tx
  "Pure: set which column and pill index has keyboard focus.
   Also updates lane memory so merge/unmerge insert at the right position."
  [st col idx]
  (let [path (col->path st col)
        mem-key (if (= col :center) :center-focus-index :left-focus-index)
        max-idx (max 0 (dec (count (get-in st path))))
        clamped (max 0 (min idx max-idx))]
    (-> st
        (assoc-in [:distillery :focus-col] col)
        (assoc-in [:distillery :focus-index] clamped)
        (cond-> (not= col :bucket)
          (assoc-in [:distillery mem-key] clamped)))))

(>defn distillery-set-focus! [col idx]
       [keyword? int? => any?]
       (swap! app-state distillery-set-focus-tx col idx))

(defn distillery-switch-lane-tx
  "Pure: switch lane (h/l). Saves current focus-index to per-lane memory,
   restores target lane's remembered position (clamped to current size)."
  [st direction]
  (let [cur-col (get-in st [:distillery :focus-col])
        cur-idx (get-in st [:distillery :focus-index])
        target-col (case direction
                     :left (when (= cur-col :center) :left)
                     :right (when (= cur-col :left) :center)
                     nil)]
    (if-not target-col
      st
      (let [target-key (if (= target-col :center) :center-paras :left-paras)
            target-paras (get-in st [:distillery target-key])]
        (if (empty? target-paras)
          st
          (let [cur-mem-key (if (= cur-col :left) :left-focus-index :center-focus-index)
                target-mem-key (if (= target-col :left) :left-focus-index :center-focus-index)
                saved-idx (get-in st [:distillery target-mem-key] 0)
                max-idx (max 0 (dec (count target-paras)))
                clamped (max 0 (min saved-idx max-idx))]
            (-> st
                (assoc-in [:distillery cur-mem-key] cur-idx)
                (assoc-in [:distillery :focus-col] target-col)
                (assoc-in [:distillery :focus-index] clamped))))))))

(>defn distillery-switch-lane! [direction]
       [keyword? => any?]
       (swap! app-state distillery-switch-lane-tx direction))

(defn distillery-save-to-drafts!
  "MANUSCRIPT (left) → replaces Draft. Stay in Distillery."
  []
  (swap! app-state
         (fn [st]
           (let [left (get-in st [:distillery :left-paras])
                 joined (str/join "\n\n" (map pill-text left))]
             (-> st
                 (assoc :draft joined)
                 (assoc-in [:distillery :center-paras] [])
                 (assoc-in [:distillery :focus-col] :left)
                 (assoc-in [:distillery :focus-index] 0)))))
  (log-distillery-event! {:type "distillery.save-to-drafts"
                          :left-length (distillery-left-length @app-state)
                          :center-length 0}))

(defn distillery-apply-to-draft!
  "MANUSCRIPT (left) → replaces Draft, clear ephemeral state, switch to Draft/Chat view."
  []
  (distillery-save-to-drafts!)
  ;; Clear ephemeral bucket state; keep leftovers (persistent)
  (swap! app-state
         (fn [st]
           (-> st
               (assoc-in [:distillery :buckets] [])
               (assoc-in [:distillery :trash-pills] [])
               (assoc-in [:distillery :active-bucket] nil)
               (assoc-in [:distillery :bucket-mode?] false))))
  (set-top-tab! :draft-chat)
  (log-distillery-event! {:type "distillery.apply-to-draft"
                          :draft-length (count (:draft @app-state))}))

(>defn distillery-set-ai-loading! [loading?]
       [boolean? => any?]
       (swap! app-state assoc-in [:distillery :ai-loading?] loading?))

(>defn distillery-set-ai-prompt-length! [len]
       [int? => any?]
       (swap! app-state assoc-in [:distillery :ai-prompt-length] len))

(declare split-into-pills)

(defn build-ai-paragraphs
  "Split each AI response into paragraphs on double-newline. Returns flat vec of
   {:text :model :response-idx :para-idx :picked?} for j/k navigation and cherry-pick."
  [ai-responses]
  (vec (mapcat (fn [resp-idx {:keys [model text]}]
                 (map-indexed (fn [para-idx para-text]
                                {:text para-text :model model
                                 :response-idx resp-idx :para-idx para-idx
                                 :picked? false})
                              (split-into-pills text)))
               (range) ai-responses)))

(defn- rebuild-ai-paragraphs!
  "Recompute :ai-paragraphs from :ai-responses. Call after any response change."
  []
  (swap! app-state assoc-in [:distillery :ai-paragraphs]
         (build-ai-paragraphs (get-in @app-state [:distillery :ai-responses]))))

(>defn distillery-add-ai-response!
       "Add a model response to the distillery AI panel."
       [model text elapsed-ms]
       [string? string? number? => any?]
       (swap! app-state update-in [:distillery :ai-responses] conj
              {:model model :text text :elapsed-ms elapsed-ms
               :response-id (short-id)
               :timestamp (str (java.time.Instant/now))})
       (rebuild-ai-paragraphs!))

(>defn distillery-add-ai-history!
       "Append a message to the distillery AI conversation history."
       [role content]
       [keyword? string? => any?]
       (swap! app-state update-in [:distillery :ai-history] conj
              {:role role :content content}))

(defn distillery-clear-ai-responses! []
  (swap! app-state assoc-in [:distillery :ai-responses] [])
  (swap! app-state assoc-in [:distillery :ai-paragraphs] [])
  (swap! app-state assoc-in [:distillery :ai-loading?] false))

(defn distillery-clear-ai-history! []
  (swap! app-state assoc-in [:distillery :ai-history] []))

(defn distillery-merge-ai-response-tx
  "Pure: cherry-pick an AI response into center-paras after the focused pill."
  [st response-idx]
  (let [responses (get-in st [:distillery :ai-responses])
        resp (get responses response-idx)
        center (get-in st [:distillery :center-paras])
        focus-idx (get-in st [:distillery :focus-index] 0)
        insert-at (if (seq center)
                    (min (inc focus-idx) (count center))
                    0)]
    (if (nil? resp) st
        (let [new-center (into (subvec center 0 insert-at)
                               (cons (make-pill (:text resp))
                                     (subvec center insert-at)))]
          (-> st
              (assoc-in [:distillery :center-paras] new-center)
              (assoc-in [:distillery :focus-index] insert-at))))))

(>defn distillery-merge-ai-response!
       "Cherry-pick an AI response into center-paras after the focused pill."
       [response-idx]
       [int? => any?]
       (swap! app-state distillery-merge-ai-response-tx response-idx))

;; === Multi-Lane Distillery (N-Lane mode) ===

(defn split-into-pills
  "Split a full draft text into paragraph-level pills."
  [text]
  (->> (str/split (or text "") #"\n\n+")
       (map str/trim)
       (remove str/blank?)
       vec))

(defn multi-lane-accumulate!
  "Split text into pills and add as a new lane."
  [text label]
  (let [pills (split-into-pills text)
        lane {:id (str (UUID/randomUUID))
              :label (or label "Draft")
              :pills pills
              :starred? false}]
    (swap! app-state update-in [:distillery :lanes] conj lane)
    ;; Auto-switch to n-lane mode
    (swap! app-state assoc-in [:distillery :mode] :n-lane)))

(defn multi-lane-remove!
  "Remove a lane by id."
  [lane-id]
  (swap! app-state update-in [:distillery :lanes]
         (fn [lanes] (vec (remove #(= (:id %) lane-id) lanes)))))

(defn multi-lane-use!
  "Copy lane pills to draft (joined with double newlines), switch to Draft & Chat."
  [lane-id]
  (let [lanes (get-in @app-state [:distillery :lanes])
        lane (first (filter #(= (:id %) lane-id) lanes))
        text (when lane (str/join "\n\n" (:pills lane)))]
    (when text
      (swap! app-state assoc :draft text)
      (swap! app-state assoc-in [:ui :active-top-tab] :draft-chat))))

(defn multi-lane-move-pill-tx
  "Pure: move a pill from one lane to another at a specific index."
  [st from-lane-id pill-idx to-lane-id to-idx]
  (let [lanes (get-in st [:distillery :lanes])
        from-lane (first (filter #(= (:id %) from-lane-id) lanes))
        pill (nth (:pills from-lane) pill-idx nil)]
    (if-not pill st
            (let [updated-from (update from-lane :pills
                                       (fn [pills] (vec (concat (subvec pills 0 pill-idx)
                                                                (subvec pills (inc pill-idx))))))
                  updated-lanes (mapv (fn [lane]
                                        (cond
                                          (= (:id lane) from-lane-id) updated-from
                                          (= (:id lane) to-lane-id)
                                          (update lane :pills
                                                  (fn [pills]
                                                    (let [idx (min to-idx (count pills))]
                                                      (vec (concat (subvec pills 0 idx)
                                                                   [pill]
                                                                   (subvec pills idx))))))
                                          :else lane))
                                      lanes)]
              (assoc-in st [:distillery :lanes] updated-lanes)))))

(defn multi-lane-move-pill!
  "Move a pill from one lane to another at a specific index."
  [from-lane-id pill-idx to-lane-id to-idx]
  (swap! app-state multi-lane-move-pill-tx from-lane-id pill-idx to-lane-id to-idx))

(defn multi-lane-merge-to-assembly-tx
  "Pure: move focused pill from focused lane to assembly lane."
  [st]
  (let [{:keys [lanes focus-lane n-lane-focus-index]} (:distillery st)
        lane (nth lanes focus-lane nil)
        pill (when lane (nth (:pills lane) n-lane-focus-index nil))]
    (if-not pill st
            (let [updated-lanes (update-in (vec lanes) [focus-lane :pills]
                                           (fn [pills]
                                             (vec (concat (subvec pills 0 n-lane-focus-index)
                                                          (subvec pills (inc n-lane-focus-index))))))
                  updated-assembly (conj (or (get-in st [:distillery :assembled-lane]) []) pill)
                  new-max (dec (count (:pills (nth updated-lanes focus-lane))))]
              (-> st
                  (assoc-in [:distillery :lanes] updated-lanes)
                  (assoc-in [:distillery :assembled-lane] updated-assembly)
                  (assoc-in [:distillery :n-lane-focus-index] (max 0 (min n-lane-focus-index new-max))))))))

(defn multi-lane-merge-to-assembly!
  "Move focused pill from focused lane to the Assembly lane."
  []
  (swap! app-state multi-lane-merge-to-assembly-tx))

(defn multi-lane-use-assembly!
  "Copy assembled pills to draft, switch to Draft & Chat."
  []
  (let [pills (get-in @app-state [:distillery :assembled-lane] [])]
    (when (seq pills)
      (swap! app-state assoc :draft (str/join "\n\n" pills))
      (swap! app-state assoc-in [:ui :active-top-tab] :draft-chat))))

(defn multi-lane-toggle-mode!
  "Toggle between :two-lane and :n-lane mode."
  []
  (swap! app-state update-in [:distillery :mode]
         (fn [m] (if (= m :n-lane) :two-lane :n-lane))))

(defn multi-lane-clear!
  "Clear all lanes."
  []
  (swap! app-state update :distillery assoc
         :lanes [] :assembled-lane [] :focus-lane 0 :n-lane-focus-index 0))

(defn multi-lane-nav-tx
  "Pure: navigate within N-lane mode."
  [st direction]
  (let [{:keys [lanes focus-lane n-lane-focus-index]} (:distillery st)
        lane-count (count lanes)
        dir-str (name direction)]
    (cond
      (= direction :next-pill)
      (let [max-idx (if (= focus-lane -1)
                      (dec (count (get-in st [:distillery :assembled-lane])))
                      (dec (count (:pills (nth lanes focus-lane nil)))))]
        (assoc-in st [:distillery :n-lane-focus-index]
                  (min (inc n-lane-focus-index) (max 0 max-idx))))

      (= direction :prev-pill)
      (assoc-in st [:distillery :n-lane-focus-index]
                (max (dec n-lane-focus-index) 0))

      (= direction :next-lane)
      (assoc-in st [:distillery :focus-lane]
                (if (= focus-lane -1) 0 (min (inc focus-lane) (dec lane-count))))

      (= direction :prev-lane)
      (assoc-in st [:distillery :focus-lane]
                (if (= focus-lane 0) -1 (max (dec focus-lane) -1)))

      ;; Click-to-focus: "focus-{lane-idx}-{pill-idx}" (lane-idx can be -1 for assembly)
      (str/starts-with? dir-str "focus-")
      (let [suffix (subs dir-str 6)
            last-dash (str/last-index-of suffix "-")
            li (parse-long (subs suffix 0 last-dash))
            pi (parse-long (subs suffix (inc last-dash)))]
        (-> st
            (assoc-in [:distillery :focus-lane] li)
            (assoc-in [:distillery :n-lane-focus-index] pi)))

      :else st)))

(defn multi-lane-nav!
  "Navigate within N-lane mode."
  [direction]
  (swap! app-state multi-lane-nav-tx direction))

;; === Chat lifecycle ===

(>defn add-chat-message! [role content]
       [string? string? => any?]
       (swap! app-state update-in [:chat :history] conj
              (cond-> {:role role :content content :timestamp (str (Instant/now))}
                (= role "assistant") (assoc :response-id (short-id)))))

(>defn set-chat-loading! [loading?]
       [boolean? => any?]
       (swap! app-state assoc-in [:chat :loading?] loading?))

;; === Session recovery ===

(defn load-draft-from-session!
  "Load draft/context/platform from an external session file (e.g. Electron JSON)."
  [{:keys [draft context platform]}]
  (when draft (swap! app-state assoc :draft draft))
  (when context (swap! app-state assoc :context context))
  (when platform (swap! app-state assoc :platform platform)))

(def ^:private posts-book-nodes
  "Canonical list of legacy projects to surface in the Posts book project."
  [{:id "nrc-dustin-warner" :level 0 :type :chapter :title "NRC / Dustin Warner"
    :context "" :draft "" :leftovers "" :collapsed? false}
   {:id "datastar-revelations" :level 0 :type :chapter :title "Datastar Revelations"
    :context "" :draft "" :leftovers "" :collapsed? false}
   {:id "eais-gaming" :level 0 :type :chapter :title "EAIS Gaming"
    :context "" :draft "" :leftovers "" :collapsed? false}
   {:id "edl-letter" :level 0 :type :chapter :title "EDL Letter"
    :context "" :draft "" :leftovers "" :collapsed? false}
   {:id "musicians-and-ai" :level 0 :type :chapter :title "Musicians and AI"
    :context "" :draft "" :leftovers "" :collapsed? false}
   {:id "post-primark" :level 0 :type :chapter :title "Post Primark"
    :context "" :draft "" :leftovers "" :collapsed? false}
   {:id "forum-problem-statements" :level 0 :type :chapter :title "Forum Problem Statements"
    :context "" :draft "" :leftovers "" :collapsed? false}])

(defn- ensure-posts-project!
  "Ensure the Posts book project exists in book-workshop. If the saved session
   predates the Posts project, inject it before 'Done / Published'."
  []
  (let [projects (get-in @app-state [:book-workshop :projects])
        has-posts? (some #(= "posts" (:id %)) projects)]
    (when-not has-posts?
      (let [done-idx (or (first (keep-indexed (fn [i p] (when (= "done" (:id p)) i)) projects))
                         (count projects))
            posts-project {:id "posts" :title "Posts" :nodes posts-book-nodes :focus-idx 0}
            new-projects (vec (concat (subvec (vec projects) 0 done-idx)
                                      [posts-project]
                                      (subvec (vec projects) done-idx)))]
        (swap! app-state assoc-in [:book-workshop :projects] new-projects)
        (log/info :migration/posts-project-created {:inserted-at done-idx})))))

(defn load-legacy-into-books!
  "One-time migration: read legacy session files and populate Posts book project nodes.
   READ-ONLY from legacy files — does not modify or delete them."
  []
  ;; First ensure the Posts project exists (saved sessions may predate it)
  (ensure-posts-project!)
  (let [projects (get-in @app-state [:book-workshop :projects])
        posts-idx (first (keep-indexed (fn [i p] (when (= "posts" (:id p)) i)) projects))]
    (when posts-idx
      (let [nodes (get-in @app-state [:book-workshop :projects posts-idx :nodes])]
        (doseq [[node-idx node] (map-indexed vector nodes)]
          (let [session-file (io/file "drafts" (:id node) "current-session.edn")]
            (when (.exists session-file)
              (try
                (let [session (edn/read-string (slurp session-file))
                      draft (or (:draft session) "")
                      context (or (:context session) "")]
                  (when (or (seq draft) (seq context))
                    (swap! app-state assoc-in
                           [:book-workshop :projects posts-idx :nodes node-idx :draft] draft)
                    (swap! app-state assoc-in
                           [:book-workshop :projects posts-idx :nodes node-idx :context] context)
                    (log/info :migration/loaded-legacy {:node-id (:id node)
                                                        :draft-len (count draft)
                                                        :context-len (count context)})))
                (catch Exception e
                  (log/warn :migration/failed {:node-id (:id node) :error (.getMessage e)}))))))
        (log/info :migration/complete {:posts-project-idx posts-idx
                                       :node-count (count nodes)})))))

;; === Fanout lifecycle ===

(def ^:private max-fanout-runs 20)

(defn start-fanout-run-tx
  "Pure: append a new fanout run to state."
  [st models run-id user-prompt fleet-key {:keys [mode has-context? shots-per-model]}]
  (let [runs (get-in st [:fanout :runs] [])
        new-run (cond-> {:id run-id
                         :prompt (or user-prompt "")
                         :timestamp (str (java.time.Instant/now))
                         :fleet (name (or fleet-key :unknown))
                         :results {}
                         :model-order []
                         :shots-per-model (or shots-per-model 3)}
                  mode (assoc :mode mode)
                  (some? has-context?) (assoc :has-context? has-context?))
        new-runs (vec (take-last max-fanout-runs (conj runs new-run)))
        new-idx (dec (count new-runs))]
    (assoc st :fanout {:runs new-runs
                       :active-run-idx new-idx
                       :loading-models (set models)})))

(defn start-fanout-run!
  "Append a new fanout run. Caps at max-fanout-runs (drops oldest)."
  [models run-id user-prompt fleet-key & {:keys [mode has-context? shots-per-model] :as opts}]
  (swap! app-state start-fanout-run-tx models run-id user-prompt fleet-key opts))

(defn accumulate-fanout-result-tx
  "Pure: record one shot result into the correct run."
  [st run-id model result total-shots]
  (let [result (cond-> result
                 (not (:response-id result)) (assoc :response-id (short-id)))
        runs (get-in st [:fanout :runs] [])
        idx (or (first (keep-indexed (fn [i r] (when (= (:id r) run-id) i)) runs))
                (get-in st [:fanout :active-run-idx]))
        run (get-in st [:fanout :runs idx])
        results (or (:results run) {})
        current-shots (get results model [])
        new-shots (conj current-shots result)
        new-results (assoc results model new-shots)
        model-order (if (contains? results model)
                      (:model-order run)
                      (conj (or (:model-order run) []) model))
        still-loading (if (= (count new-shots) total-shots)
                        (disj (get-in st [:fanout :loading-models]) model)
                        (get-in st [:fanout :loading-models]))]
    (-> st
        (assoc-in [:fanout :runs idx :results] new-results)
        (assoc-in [:fanout :runs idx :model-order] model-order)
        (assoc-in [:fanout :loading-models] still-loading))))

(defn accumulate-fanout-result!
  "Atomically record one shot result into the correct run (by run-id)."
  [run-id model result total-shots]
  (swap! app-state accumulate-fanout-result-tx run-id model result total-shots))

(defn clear-fanout-loading!
  "Force-clear loading-models set (handles stale/hung futures)."
  []
  (swap! app-state assoc-in [:fanout :loading-models] #{}))

(defn clear-fanout-history!
  "Reset fanout to empty state."
  []
  (swap! app-state assoc :fanout {:runs [] :active-run-idx nil :loading-models #{}}))

;; === Critique lifecycle ===

(defn reset-critique-tx
  "Pure: initialize critique state for a new run."
  [st total {:keys [pending-requests]}]
  (assoc st :critique
         {:progress 0 :total total :responses [] :synthesis nil
          :synthesis-pills [] :timestamp (str (Instant/now))
          :pending-requests (vec (or pending-requests []))}))

(defn reset-critique!
  [total & {:keys [pending-requests] :as opts}]
  (swap! app-state reset-critique-tx total opts))

(defn add-critique-response-tx
  "Pure: add one critique response and bump progress."
  [st label model text {:keys [error? pills]}]
  (update st :critique
          (fn [c] (-> c
                      (update :responses conj
                              (cond-> {:label label :model model :text text :error? (boolean error?)}
                                (seq pills) (assoc :pills (vec pills))))
                      (update :progress inc)))))

(defn add-critique-response!
  [label model text & {:keys [error? pills] :as opts}]
  (swap! app-state add-critique-response-tx label model text opts))

(defn- ensure-response-pills!
  "Ensure pills are populated for a response (extract from text if needed)."
  [response-idx]
  (let [resp (get-in @app-state [:critique :responses response-idx])]
    (when (and resp (not (seq (:pills resp))) (:text resp))
      (when-let [pills (seq (rewrite-extract/extract-rewrites-from-json-text
                             (:text resp)))]
        (swap! app-state assoc-in
               [:critique :responses response-idx :pills] (vec pills))))))

(>defn archive-rewrite!
       "Toggle :archived? flag on a specific pill within a critique response."
       [response-idx pill-idx]
       [int? int? => any?]
       (ensure-response-pills! response-idx)
       (swap! app-state update-in [:critique :responses response-idx :pills pill-idx :archived?] not))

(>defn mark-rewrite-applied!
       "Set :applied? true on a critique pill (response or synthesis)."
       [response-idx pill-idx]
       [int? int? => any?]
       (if (neg? response-idx)
         (swap! app-state assoc-in [:critique :synthesis-pills pill-idx :applied?] true)
         (do (ensure-response-pills! response-idx)
             (swap! app-state assoc-in [:critique :responses response-idx :pills pill-idx :applied?] true))))

(>defn set-synthesis-text! [text]
       [string? => any?]
       (swap! app-state assoc-in [:critique :synthesis] text))

(>defn set-synthesis-rewrites! [pills]
       [sequential? => any?]
       (let [normalized (mapv #(merge {:title "" :problem "" :fix "" :severity "medium"
                                       :draft_quote "" :archived? false :applied? false} %)
                              pills)]
         (swap! app-state assoc-in [:critique :synthesis-pills] normalized)))

(>defn archive-synthesis-rewrite!
       "Toggle :archived? flag on a synthesis pill."
       [pill-idx]
       [int? => any?]
       (swap! app-state update-in [:critique :synthesis-pills pill-idx :archived?] not))

;; === Autocomplete lifecycle ===

(defn reset-autocomplete!
  "Clear autocomplete state for a new run."
  []
  (swap! app-state assoc :autocomplete {:options [] :loading? true :error nil
                                        :timestamp (str (Instant/now))}))

(>defn set-autocomplete-options!
       "Store parsed autocomplete options."
       [options]
       [sequential? => any?]
       (swap! app-state update :autocomplete merge {:options options :loading? false :error nil}))

(>defn set-autocomplete-error!
       "Store autocomplete error message."
       [error-msg]
       [string? => any?]
       (swap! app-state update :autocomplete merge {:options [] :loading? false :error error-msg}))

;; ---------------------------------------------------------------------------
;; Session persistence
;; ---------------------------------------------------------------------------

(declare save-active-project!)

(defn project-dir
  "Return the current project directory path."
  []
  (str "drafts/" (:project @app-state) "/"))

(defn- session-file
  "Return the session file path for the current project."
  []
  (str (project-dir) "current-session.edn"))

(defn- goal-context-file
  "Return the goal-context file path for a given goal in the current project."
  [goal-kw]
  (str (project-dir) "goal-contexts/" (name goal-kw) ".edn"))

(defn save-goal-context!
  "Persist current context-tabs-data to per-project per-goal EDN file.
   Suppressed when *io-enabled* is false."
  []
  (when *io-enabled*
    (when-let [goal (:goal @app-state)]
      (try
        (let [f (goal-context-file goal)
              data @context-tabs-data]
          (when (seq data)
            (io/make-parents f)
            (spit f (pr-str data))
            (log/debug :goal-context :saved :goal goal)))
        (catch Exception e
          (log/warn e "Failed to save goal context"))))))

(defn load-goal-context!
  "Restore context-tabs-data from per-project per-goal EDN file."
  [goal-kw]
  (try
    (let [f (goal-context-file goal-kw)]
      (if (.exists (io/file f))
        (let [data (edn/read-string (slurp f))]
          (when (map? data)
            (reset! context-tabs-data data)
            (log/info :goal-context :loaded :goal goal-kw :tabs (count data))))
        (reset! context-tabs-data {})))
    (catch Exception e
      (log/warn e "Failed to load goal context")
      (reset! context-tabs-data {}))))

(defn- ai-results-file
  "Return the ai-results file path for the current project."
  []
  (str (project-dir) "ai-results.edn"))

(defn save-ai-results!
  "Persist fanout/critique/autocomplete state as plain EDN (bypasses closed-record).
   Suppressed when *io-enabled* is false."
  []
  (when *io-enabled*
    (try
      (let [st @app-state
            data {:fanout (:fanout st)
                  :critique (:critique st)
                  :autocomplete (:autocomplete st)}
            has-content? (or (seq (get-in data [:fanout :runs]))
                             (seq (get-in data [:critique :responses]))
                             (seq (get-in data [:autocomplete :options])))]
        (when has-content?
          (let [f (ai-results-file)]
            (io/make-parents f)
            (spit f (pr-str data))
            (log/debug :ai-results :saved))))
      (catch Exception e
        (log/warn e "Failed to save AI results")))))

(defn- migrate-fanout-data
  "Migrate old flat fanout format {:results {} :model-order []} to run-based format."
  [fanout]
  (if (and (map? fanout) (contains? fanout :results) (not (contains? fanout :runs)))
    ;; Old format — wrap in a single run
    (let [run {:id "migrated"
               :prompt "(migrated)"
               :timestamp (str (java.time.Instant/now))
               :fleet "unknown"
               :results (:results fanout)
               :model-order (:model-order fanout)}]
      {:runs (if (seq (:results fanout)) [run] [])
       :active-run-idx (if (seq (:results fanout)) 0 nil)
       :loading-models #{}})
    fanout))

(defn load-ai-results!
  "Restore fanout/critique/autocomplete from separate EDN file (bypasses closed-record)."
  []
  (try
    (let [f (ai-results-file)]
      (when (.exists (io/file f))
        (let [data (edn/read-string (slurp f))]
          (when (map? data)
            (when-let [fanout (:fanout data)]
              (swap! app-state assoc :fanout
                     (-> (migrate-fanout-data fanout)
                         (assoc :loading-models #{}))))
            (when-let [critique (:critique data)]
              (swap! app-state assoc :critique critique))
            (when-let [autocomplete (:autocomplete data)]
              (swap! app-state assoc :autocomplete autocomplete))
            (log/info :ai-results :loaded
                      :fanout-runs (count (get-in data [:fanout :runs] []))
                      :critique-responses (count (get-in data [:critique :responses] []))
                      :autocomplete-options (count (get-in data [:autocomplete :options] [])))))))
    (catch Exception e
      (log/warn e "Failed to load AI results"))))

(defn settle-editing-state
  "Pure fn: if editing a book node, sync draft/context back into the node tree.
   Designed to be used inside a single swap! — no interleaving possible.
   Also callable standalone via sync-draft-to-book-node!."
  [st]
  (if-let [editing-node (get-in st [:book-workshop :editing-node])]
    (let [{:keys [project-idx node-idx]} editing-node]
      (let [node (get-in st [:book-workshop :projects project-idx :nodes node-idx])]
        (cond-> st
          true (assoc-in [:book-workshop :projects project-idx :nodes node-idx :draft]
                         (or (:draft st) ""))
          true (assoc-in [:book-workshop :projects project-idx :nodes node-idx :context]
                         (or (:context st) ""))
          (contains? node :leftovers)
          (assoc-in [:book-workshop :projects project-idx :nodes node-idx :leftovers]
                    (or (:leftovers st) "")))))
    st))

(defn- sync-draft-to-book-node!
  "When editing-node is set, copy current draft/context back into the book node.
   Uses a single atomic swap! via settle-editing-state — no interleaving possible."
  []
  (swap! app-state settle-editing-state))

(defn save-session!
  "Persist app-state to EDN file. Suppressed when *io-enabled* is false."
  []
  (when *io-enabled*
    (try
      (sync-draft-to-book-node!)
      (let [f (session-file)]
        (io/make-parents f)
        (spit f (pr-str (cr/to-map-recursive @app-state)))
        (save-active-project!)
        (save-ai-results!)
        (save-goal-context!)
        (log/debug :session :saved))
      (catch Exception e
        (log/warn e "Failed to save session")))))

(defn- deep-merge
  "Recursively merge maps so new default keys in nested maps are preserved."
  [a b]
  (merge-with (fn [va vb]
                (if (and (map? va) (map? vb))
                  (deep-merge va vb)
                  vb))
              a b))

;; ---------------------------------------------------------------------------
;; Collection-item defaults — the fix for closed-record + evolving shapes
;; ---------------------------------------------------------------------------
;; deep-merge handles map-in-map merging, but vectors are opaque to it.
;; When a saved session has lanes [{:id "x" :pills [...]}] (no :starred?),
;; deep-merge keeps the old-shape vector, then closed-record-recursive locks
;; it down → INVALID KEY ACCESS on the new key.
;;
;; Fix: register canonical item shapes here. normalize-state merges defaults
;; into every map in each collection before closing. Add new keys here and
;; old sessions just work.

(def ^:private collection-defaults
  "Paths to vectors-of-maps and their canonical item defaults."
  {[:distillery :lanes] {:id "" :label "" :pills [] :starred? false}
   [:distillery :buckets] {:id "" :pills []}})

(defn- ensure-trash-nodes
  "Ensure every book project has a Trash chapter at the end of its nodes."
  [state]
  (update-in state [:book-workshop :projects]
             (fn [projects]
               (mapv (fn [project]
                       (let [nodes (:nodes project)
                             has-trash? (some #(and (= 0 (:level %)) (= "Trash" (:title %))) nodes)]
                         (if has-trash?
                           project
                           (update project :nodes conj
                                   (make-book-node {:type :chapter :title "Trash" :collapsed? true})))))
                     projects))))

(defn- normalize-state
  "Walk known collection paths and merge item-level defaults into each map.
   Migrates renamed keys, then ensures old saved sessions get new keys
   before closed-record-recursive."
  [state]
  (-> (cond-> state
        ;; Migrate :merge-studio → :distillery (renamed Mar 24, 2026)
        (= :merge-studio (get-in state [:ui :active-top-tab]))
        (assoc-in [:ui :active-top-tab] :distillery))
      ;; Ensure :orphan-expanded exists in :book-workshop (added 2026-03-27)
      (update :book-workshop #(merge {:orphan-expanded false :imported-node-id nil} %))
      (as-> s
            (reduce-kv
             (fn [s path defaults]
               (update-in s path
                          (fn [items]
                            (if (sequential? items)
                              (mapv #(if (map? %) (merge defaults %) %) items)
                              items))))
             s
             collection-defaults))
      ;; Ensure :synthesis-pills exists in :critique (added 2026-03-28)
      (update :critique #(merge {:synthesis-pills []} %))
      ensure-trash-nodes
      ;; Normalize distillery pills: string → {:text s :level 0} (added 2026-03-27)
      (update-in [:distillery :left-paras] (fn [ps] (if (sequential? ps) (normalize-pills ps) ps)))
      (update-in [:distillery :center-paras] (fn [ps] (if (sequential? ps) (normalize-pills ps) ps)))
      ;; Ensure bucket fields exist (added 2026-03-28)
      (update :distillery #(merge {:bucket-mode? false :buckets [] :leftovers-pills [] :trash-pills [] :active-bucket nil :viewing-bucket nil :ai-paragraphs []} %))
      ;; Rebuild ai-paragraphs from ai-responses (added 2026-03-28)
      (as-> s (assoc-in s [:distillery :ai-paragraphs]
                        (build-ai-paragraphs (get-in s [:distillery :ai-responses]))))
      ;; Ensure book nodes have :leftovers field (added 2026-03-27)
      (update-in [:book-workshop :projects]
                 (fn [projects]
                   (mapv (fn [p]
                           (update p :nodes
                                   (fn [nodes]
                                     (mapv #(merge {:leftovers ""} %) nodes))))
                         projects)))))

(defn load-session!
  "Restore app-state from EDN file."
  []
  (try
    (let [f (session-file)]
      (when (.exists (io/file f))
        (let [data (edn/read-string (slurp f))]
          (when (map? data)
            (let [defaults (cr/to-map-recursive @app-state)
                  saved (dissoc data :fanout :critique :autocomplete :project :state-version)
                  merged (deep-merge defaults saved)
                  ;; Log new keys that were added from defaults (not in saved session)
                  new-ui-keys (when (and (map? (:ui defaults)) (map? (:ui saved)))
                                (seq (remove (set (keys (:ui saved))) (keys (:ui defaults)))))]
              (when new-ui-keys
                (log/info :session :new-default-keys :keys new-ui-keys))
              (reset! app-state
                      (cr/closed-record-recursive (normalize-state merged))))
            (log/info :session :loaded
                      :project (:project @app-state)
                      :draft-length (count (:draft @app-state))
                      :platform (:platform @app-state))
            ;; Load goal context if goal is set
            (when-let [goal (:goal @app-state)]
              (load-goal-context! goal))))))
    (catch Exception e
      (log/warn e "Failed to load session"))))

;; ---------------------------------------------------------------------------
;; Auto-save timer
;; ---------------------------------------------------------------------------

(defonce ^:private autosave-timer (atom nil))

(defn stop-autosave! []
  (when-let [t @autosave-timer]
    (.cancel ^Timer t)
    (reset! autosave-timer nil)))

(defn start-autosave!
  "Start auto-save timer (every 30 seconds)."
  []
  (stop-autosave!)
  (let [timer (Timer. "session-autosave" true)
        task (proxy [TimerTask] []
               (run [] (save-session!)))]
    (.schedule timer task (long 30000) (long 30000))
    (reset! autosave-timer timer)
    (log/info :autosave :started)))

;; ---------------------------------------------------------------------------
;; Event logging (append-only JSONL)
;; ---------------------------------------------------------------------------

(def ^:private events-file "logs/events.jsonl")

(>defn log-event!
       "Append event to JSONL log. Suppressed when *io-enabled* is false."
       [event-map]
       [map? => any?]
       (when *io-enabled*
         (try
           (io/make-parents events-file)
           (let [entry (merge {:timestamp (str (Instant/now))
                               :project (:project @app-state)}
                              event-map)]
             (spit events-file (str (json/write-str entry) "\n") :append true))
           (catch Exception e
             (log/warn e "Failed to log event" event-map)))))

;; ---------------------------------------------------------------------------
;; Write-Ahead Log (append-only JSONL for state transitions)
;; Logs before/after snapshots of corruption-prone keys on every named transition.
;; ~300 bytes/entry, ~60KB/day. Open logs/wal.jsonl to diagnose corruption.
;; ---------------------------------------------------------------------------

(def ^:private wal-file "logs/wal.jsonl")

(defn- wal-snapshot
  "Extract corruption-diagnostic keys from state. No draft text — just lengths."
  [st]
  {:project (:project st)
   :state-version (:state-version st)
   :top-tab (get-in st [:ui :active-top-tab])
   :editing-node (get-in st [:book-workshop :editing-node])
   :draft-len (count (or (:draft st) ""))
   :context-len (count (or (:context st) ""))})

(defn- log-wal!
  "Append a transition entry to the WAL. Suppressed when *io-enabled* is false."
  [transition-name params before-snapshot after-snapshot]
  (when *io-enabled*
    (try
      (io/make-parents wal-file)
      (let [entry {:timestamp (str (Instant/now))
                   :transition (name transition-name)
                   :params params
                   :before before-snapshot
                   :after after-snapshot}]
        (spit wal-file (str (json/write-str entry) "\n") :append true))
      (catch Exception e
        (log/warn e "Failed to log WAL entry" transition-name)))))

(defn transition-tx
  "Pure: settle editing state, apply tx-fn, bump :state-version."
  [st tx-fn]
  (-> st
      settle-editing-state
      tx-fn
      (update :state-version inc)))

(defn transition!
  "Execute a named state transition atomically. Settles editing state,
   applies tx-fn, bumps :state-version, logs before/after to WAL.
   tx-fn: (fn [state] new-state) — pure state transformation."
  [transition-name params tx-fn]
  (let [before-snapshot (wal-snapshot @app-state)
        after (swap! app-state transition-tx tx-fn)
        after-snapshot (wal-snapshot after)]
    (log-wal! transition-name params before-snapshot after-snapshot)
    (log-event! {:type "transition" :transition (name transition-name) :params params})
    after))

;; ---------------------------------------------------------------------------
;; Project management
;; ---------------------------------------------------------------------------

(def ^:private active-project-file "drafts/.active-project.edn")
(def ^:private mru-file "drafts/.projects-mru.edn")
(def ^:private max-mru 20)

(defn save-active-project!
  "Persist current project name to bootstrap file.
   Suppressed when *io-enabled* is false."
  []
  (when *io-enabled*
    (try
      (io/make-parents active-project-file)
      (spit active-project-file (pr-str (:project @app-state)))
      (catch Exception e
        (log/warn e "Failed to save active project")))))

(defn load-active-project!
  "Restore active project from bootstrap file before loading session."
  []
  (try
    (when (.exists (io/file active-project-file))
      (let [proj (edn/read-string (slurp active-project-file))]
        (when (string? proj)
          (swap! app-state assoc :project proj)
          (log/info :active-project :loaded :project proj))))
    (catch Exception e
      (log/warn e "Failed to load active project"))))

(defn set-project!
  "Switch to a different project. Bumps state-version to invalidate stale browser syncs."
  [project-name]
  (transition! :set-project {:project project-name}
               #(assoc % :project project-name))
  (save-active-project!))

(defn set-creating-project!
  "Toggle the inline project-creation input."
  [active?]
  (swap! app-state assoc :creating-project? active?))

(defn list-projects
  "Scan drafts/ for project subdirectories. Returns sorted vector of names."
  []
  (let [drafts-dir (io/file "drafts")]
    (if (.exists drafts-dir)
      (->> (.listFiles drafts-dir)
           (filter #(.isDirectory %))
           (map #(.getName %))
           sort
           vec)
      [])))

(defn load-mru
  "Load MRU list from disk. Returns vector of project names."
  []
  (try
    (if (.exists (io/file mru-file))
      (let [data (edn/read-string (slurp mru-file))]
        (if (vector? data) data ["default"]))
      ["default"])
    (catch Exception _
      ["default"])))

(defn save-mru!
  "Persist MRU list to disk. Suppressed when *io-enabled* is false."
  [mru]
  (when *io-enabled*
    (try
      (io/make-parents mru-file)
      (spit mru-file (pr-str (vec (take max-mru mru))))
      (catch Exception e
        (log/warn e "Failed to save MRU")))))

(defn touch-mru!
  "Move project-name to front of MRU list."
  [project-name]
  (let [mru (load-mru)
        updated (into [project-name] (remove #{project-name} mru))]
    (save-mru! updated)))

(defn migrate-to-projects!
  "One-time migration: move drafts/*.txt and drafts/current-session.* into drafts/default/.
   Safe to call multiple times — skips if drafts/default/ already exists."
  []
  (let [default-dir (io/file "drafts/default")]
    (when-not (.exists default-dir)
      (let [drafts-dir (io/file "drafts")
            files-to-move (when (.exists drafts-dir)
                            (->> (.listFiles drafts-dir)
                                 (filter #(.isFile %))
                                 (filter #(or (.endsWith (.getName %) ".txt")
                                              (.startsWith (.getName %) "current-session")))))]
        (when (seq files-to-move)
          (.mkdirs default-dir)
          (doseq [f files-to-move]
            (let [target (io/file default-dir (.getName f))]
              (io/copy f target)
              (.delete f)))
          (log/info :migration :complete :files-moved (count files-to-move)))))))
