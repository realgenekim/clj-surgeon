(ns writer.views.components
  "Shared UI components: header, platform selector, char count, notifications."
  (:require
   [clojure.string :as str]
   [writer.prompt :as prompt]
   [writer.state :as state]
   [writer.views.ds :as ds]))

;; Unicode display characters — named vars so intent is clear
(def STAR "\u2605")           ;; ★  star icon for Merge Studio
(def MIDDLE-DOT "\u00B7")     ;; ·  separator
(def LEFT-TRIANGLE "\u25C0")  ;; ◀  collapse button
(def DOWN-ARROW "\u2193")     ;; ↓  sort indicator

(defn goal-selector
  "Goal dropdown for the header."
  [{:keys [goal]}]
  (let [goals-config (prompt/load-goals-config)
        goals (get goals-config :goals)]
    [:div#goal-selector.goal-selector
     [:select.goal-select
      ;; :sync — goal switch outer-morphs #editor-pane; without carrying the
      ;; unsent keystrokes, the morph replaces the textarea with server text
      ;; and the user's last ~500ms of typing is unrecoverable
      {:data-star-on:change (ds/post-action* "/api/goal"
                                             {:goal (ds/js "evt.target.value||''")
                                              :sync (ds/js "(typeof collectDraftSync==='function'?collectDraftSync():null)")})}
      [:option {:value "" :selected (nil? goal)} "No Goal"]
      (for [[k {:keys [label]}] (sort-by (comp :label val) goals)]
        [:option {:value (name k) :selected (= k goal)} label])]]))

(defn- platform-selector
  [{:keys [platform]}]
  [:div#platform-selector.platform-selector
   [:button.platform-btn.twitter
    {:class (if (= platform :twitter) "active" "inactive")
     :data-platform "twitter"
     :data-star-on:click (ds/post-action* "/api/platform" {:platform "twitter"})}
    "Twitter"]
   [:button.platform-btn.linkedin
    {:class (if (= platform :linkedin) "active" "inactive")
     :data-platform "linkedin"
     :data-star-on:click (ds/post-action* "/api/platform" {:platform "linkedin"})}
    "LinkedIn"]])

(defn- menu-item
  [menu-id label accelerator attrs]
  [:button.app-menu-item
   (merge {:type "button"
           :role "menuitem"
           :popovertarget menu-id
           :popovertargetaction "hide"}
          attrs)
   [:span label]
   (when accelerator [:kbd accelerator])])

(defn- menu-shortcut
  [label accelerator]
  [:div.app-menu-shortcut
   [:span label]
   [:kbd accelerator]])

(defn- menu
  [id label & content]
  (let [anchor (str "--" id)]
    (list
      [:button.app-menu-trigger
       {:type "button"
        :popovertarget id
        :style (str "anchor-name:" anchor)}
       label]
      (into [:div.app-menu-popover
             {:id id
              :popover "auto"
              :role "menu"
              :aria-label label
              :style (str "position-anchor:" anchor)}]
            content))))

(defn- active-book-label
  [state]
  (let [workshop (:book-workshop state)
        project (get (:projects workshop) (:active-project-idx workshop))
        node-id (get-in workshop [:editing-node :node-id])
        node (some #(when (= node-id (:id %)) %) (:nodes project))
        project-title (or (:title project) (:project state))]
    (cond
      (and project-title (:title node)) (str project-title " › " (:title node))
      project-title project-title
      :else "Write")))

(defn top-tabs
  "Compact application menu bar. Retains #top-tabs for existing SSE morphs."
  [state]
  (let [breadcrumb (active-book-label state)]
    [:nav#top-tabs.app-menu-bar {:aria-label "Application menu"}
     [:span.app-menu-brand {:aria-label "Social Media Writer"} "Writer"]
     (menu "app-menu-file" "File"
           (menu-item "app-menu-file" "Save" "⌘S" {:onclick "saveDraft()"})
           (menu-item "app-menu-file" "Copy draft" nil
                      {:data-star-on:click (ds/copy-draft-to-clipboard)}))
     (menu "app-menu-edit" "Edit"
           (menu-item "app-menu-edit" "Transform selection…" "⌥T"
                      {:onclick "openTransformFromSelection()"})
           (menu-item "app-menu-edit" "Send selection to AI" "⌥E"
                      {:onclick "expound()"})
           (menu-item "app-menu-edit" "List selection" "⌥L"
                      {:onclick "bulletize()"})
           [:div.app-menu-separator {:role "separator"}]
           (menu-item "app-menu-edit" "Format draft" nil
                      {:onclick "formatDraft()"})
           (menu-item "app-menu-edit" "Undo last server edit" nil
                      {:data-star-on:click (ds/post-action* "/api/draft/undo" {})}))
     (menu "app-menu-view" "View"
           (menu-item "app-menu-view" "Draft & Chat" "⌥1"
                      {:data-star-on:click "switchTopTab('draft-chat')"})
           (menu-item "app-menu-view" (str STAR " Distillery") "⌥2"
                      {:data-star-on:click "switchTopTab('distillery')"})
           (menu-item "app-menu-view" "Draft History" "⌥3"
                      {:data-star-on:click "switchTopTab('draft-history')"})
           (menu-item "app-menu-view" "Book Writer" "⌥4"
                      {:data-star-on:click "switchTopTab('book-workshop')"})
           (menu-item "app-menu-view" "Research" "⌥5"
                      {:data-star-on:click "switchTopTab('research-chat')"})
           (menu-item "app-menu-view" "Email" "⌥6"
                      {:data-star-on:click "switchTopTab('email')"})
           [:div.app-menu-separator {:role "separator"}]
           (menu-item "app-menu-view" "Toggle AI pane" "⌘\\"
                      {:data-star-on:click (ds/post-action* "/api/keydown" {:key "meta+\\"})}))
     (menu "app-menu-go" "Go"
           (menu-item "app-menu-go" "Previous book node" "⌘["
                      {:onclick "navigateBookNodeHistory(-1)"})
           (menu-item "app-menu-go" "Next book node" "⌘]"
                      {:onclick "navigateBookNodeHistory(1)"}))
     (menu "app-menu-tools" "Tools"
           [:div.app-menu-control
            [:span.app-menu-label "Goal"]
            (goal-selector state)]
           [:div.app-menu-control
            [:span.app-menu-label "Platform"]
            (platform-selector state)]
           [:div.app-menu-separator {:role "separator"}]
           [:a.app-menu-item {:href "/dashboard" :role "menuitem"} [:span "Dashboard"]]
           [:a.app-menu-item {:href "/mockups" :role "menuitem"} [:span "Mockups"]])
     (menu "app-menu-help" "Help"
           (menu-shortcut "Save" "⌘S")
           (menu-shortcut "Transform" "⌥T")
           (menu-shortcut "Switch workspace" "⌥1–6")
           (menu-shortcut "Location" "⌘L"))
     [:div.app-menu-spacer
      [:label.draft-view-control
       {:id "draft-view-control"
        :title "Toggle Draft Markdown preview (Option-P)"
        :hidden true
        :data-star-ignore-morph ""}
       [:select#draft-view-mode {:aria-label "Draft view"}
        [:option {:value "edit"} "Edit"]
        [:option {:value "preview"} "Preview"]]
       [:kbd.draft-view-shortcut "⌥P"]]]
     [:div.app-menu-breadcrumb {:title breadcrumb} breadcrumb]
     [:div#editor-save-status.app-menu-status
      {:data-state "ready"
       :data-star-ignore-morph ""
       :aria-live "polite"}
      "Ready"]]))

(defn char-count-display
  "Character count + word count with warning/danger styling."
  [{:keys [draft platform]}]
  (let [cnt (count (or draft ""))
        words (if (str/blank? draft) 0
                  (count (str/split (str/trim draft) #"\s+")))
        limit (state/char-limit platform)
        cls (cond
              (> cnt limit) "danger"
              (> cnt (int (* limit 0.8))) "warning"
              :else "")]
    [:span#char-count-display {:class (str "char-count " cls)}
     [:span#char-count (str cnt)]
     " / "
     [:span#char-limit (str limit)]
     [:span#word-count {:style "margin-left:8px; color:#888; font-size:11px;"} (str words " words")]]))

;; ---------------------------------------------------------------------------
;; Chat send buttons — shared by Draft and Distillery
;; ---------------------------------------------------------------------------

(defn chat-send-buttons
  "Render Send + Fan Out buttons with keyboard hints.
   input-id: the textarea element ID to read message from.
   send-endpoint: server endpoint for single-model chat (e.g. \"/api/chat\").
   fanout-endpoint: server endpoint for fleet fanout (e.g. \"/api/fanout\").
   extra-send-params: map of extra JS expressions to include in send body.
   extra-fanout-params: map of extra JS expressions for fanout body.
   opts: {:sync-draft? true} to sync draft before sending (Draft tab needs this)."
  [{:keys [input-id send-endpoint fanout-endpoint edit-endpoint
           extra-send-params extra-fanout-params extra-edit-params
           sync-draft?]}]
  (let [get-msg (str "let m=document.getElementById('" input-id "');"
                     "if(!m||!m.value.trim())return;")
        clear-msg (str "m.value=''")
        ;; Build send action
        send-params (merge {:message (ds/js "m.value.trim()")} extra-send-params)
        send-fetch (ds/post-action* send-endpoint send-params)
        ;; Build fanout action
        fanout-params (merge {:message (ds/js "m.value.trim()")} extra-fanout-params)
        fanout-fetch (ds/post-action* fanout-endpoint fanout-params)
        ;; Build fanout+synthesize action
        fanout-synth-params (merge {:message (ds/js "m.value.trim()") :synthesize true} extra-fanout-params)
        fanout-synth-fetch (ds/post-action* fanout-endpoint fanout-synth-params)
        ;; Build edit action (structured JSON edits)
        edit-params (merge {:message (ds/js "m.value.trim()")} extra-edit-params)
        edit-fetch (when edit-endpoint (ds/post-action* edit-endpoint edit-params))
        ;; Optional draft sync prefix
        sync-prefix (when sync-draft?
                      (str "let d=document.getElementById('draft-editor');"
                           "let c=document.getElementById('context-editor');"
                           "await fetch('/api/sync-draft',{method:'POST',"
                           "headers:{'Content-Type':'application/json'},"
                           "body:JSON.stringify({draft:d?d.value:'',context:c?c.value:'','cursor-pos':d?d.selectionStart:0,'state-version':getStateVersion(),'editor-sync-key':getEditorSyncKey()})});"))
        ;; Wrap in async IIFE when sync-draft? is true (because await needs async context)
        wrap (if sync-draft? #(str "(async()=>{" % "})()") identity)
        send-action (wrap (str get-msg (or sync-prefix "") send-fetch ";" clear-msg))
        fanout-action (wrap (str get-msg (or sync-prefix "") fanout-fetch ";" clear-msg))
        fanout-synth-action (wrap (str get-msg (or sync-prefix "") fanout-synth-fetch ";" clear-msg))
        edit-action (when edit-fetch (wrap (str get-msg (or sync-prefix "") edit-fetch ";" clear-msg)))]
    {:send-action send-action
     :fanout-action fanout-action
     :fanout-synth-action fanout-synth-action
     :edit-action edit-action
     :buttons
     [:div.chat-send-row
      [:button.chat-send-btn
       {:data-star-on:click send-action}
       "Send" [:span.kbd-hint " Enter"]]
      (when edit-endpoint
        [:button.chat-send-btn
         {:data-star-on:click "sendChatEdit()"
          :style "background:#f39c12; border-color:#f39c12; color:#fff"}
         "Chat to Edit" [:span.kbd-hint " \u2318E"]])
      [:div {:style "display:inline-flex; border:2px solid #27ae60; border-radius:6px; overflow:hidden; gap:0;"}
       [:button.chat-send-btn.chat-send-fanout-btn
        {:data-star-on:click fanout-action
         :style "border-radius:0; border-right:1px solid #27ae60; margin:0;"}
        "Fan Out" [:span.kbd-hint " \u2318\u21E7Enter"]]
       [:button.chat-send-btn.chat-send-fanout-btn
        {:data-star-on:click fanout-synth-action
         :style "border-radius:0; margin:0;"}
        "+Synth"]]]
     :keydown-handler
     (str "if(evt.key==='Enter'&&!evt.shiftKey&&!evt.metaKey&&!evt.ctrlKey)"
          "{evt.preventDefault();" send-action "}"
          "if(evt.key==='Enter'&&(evt.metaKey||evt.ctrlKey)&&evt.shiftKey)"
          "{evt.preventDefault();" fanout-action "}"
          (when edit-endpoint
            "if(evt.key==='e'&&(evt.metaKey||evt.ctrlKey)&&!evt.shiftKey){evt.preventDefault();sendChatEdit()}"))}))
