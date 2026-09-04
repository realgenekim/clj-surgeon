(ns writer.routes
  "Reitit routes — all handlers as named defns with #'var refs."
  (:require
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.fulcrologic.guardrails.core :refer [=> >defn >defn-]]
   [hiccup2.core :as h]
   [taoensso.timbre :as log]
   [writer.book-recovery :as book-recovery]
   [writer.debug-guard :as debug-guard]
   [writer.dispatch :as dispatch]
   [writer.editor-conflict :as editor-conflict]
   [writer.editor-journal :as editor-journal]
   [writer.email-cockpit :as email-cockpit]
   [writer.handlers.book-workshop :as book]
   [writer.handlers.chat :as chat-handlers]
   [writer.handlers.distillery :as distillery]
   [writer.handlers.file-document :as file-document]
   [writer.handlers.markdown-preview :as markdown-preview]
   [writer.handlers.multi-draft :as multi-draft]
   [writer.handlers.nav :as nav-handlers]
   [writer.handlers.research-chat :as research-chat]
   [writer.handlers.rewrite :as rewrite-handlers]
   [writer.handlers.smw-write :as smw-write-handlers]
   [writer.handlers.transform :as transform]
   [writer.http :as http]
   [writer.keymap :as keymap]
   [writer.llm.cli :as cli]
   [writer.llm.config :as llm-config]
   [writer.llm.dispatch :as llm-dispatch]
   [writer.nav :as nav]
   [writer.prompt :as prompt]
   [writer.prompt-plan :as prompt-plan]
   [writer.rewrite.match :as rewrite-match]
   [writer.sse :as sse]
   [writer.state :as state]
   [writer.storage :as storage]
   [writer.views.ai-answers-spike :as ai-spike]
   [writer.views.book-workshop :as book-workshop]
   [writer.views.components :as components]
   [writer.views.dashboard :as dashboard]
   [writer.views.draft-chat :as draft-chat]
   [writer.views.draft-history :as draft-history]
   [writer.views.layout :as layout]
   [writer.views.mockups :as mockups]
   [writer.views.nav-spike :as nav-spike]
   [writer.views.timeline :as timeline]))

;; ---------------------------------------------------------------------------
;; Page handlers
;; ---------------------------------------------------------------------------













































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































(defn make-routes []
  [;; Pages
   ["/" {:get {:handler #'handle-home}}]

   ;; SSE
   ["/api/sse" {:get {:handler #'sse/handle-sse
                      :muuntaja false}}]

   ;; Draft/editor
   ["/api"
    ["/sync-draft" {:post {:handler (var handle-sync-draft)}}]
    ["/draft/render-markdown"
     {:post {:handler (var markdown-preview/handle-render)}}]]
   ["/api/platform" {:post {:handler #'nav-handlers/handle-platform-switch}}]
   ["/api/tab" {:post {:handler #'nav-handlers/handle-editor-tab}}]
   ["/api/top-tab" {:post {:handler #'nav-handlers/handle-top-tab}}]
   ["/api/email/open" {:post {:handler #'email-cockpit/handle-open}}]
   ["/api/email/open-visible" {:post {:handler #'email-cockpit/handle-open-visible}}]
   ["/api/email/open-status" {:post {:handler #'email-cockpit/handle-open-status}}]
   ["/api/email/select" {:post {:handler #'email-cockpit/handle-select}}]
   ["/api/email/sync" {:post {:handler #'email-cockpit/handle-sync}}]
   ["/api/email/context" {:post {:handler #'email-cockpit/handle-context-save}}]
   ["/api/email/copy" {:post {:handler #'email-cockpit/handle-copy}}]
   ["/api/keydown" {:post {:handler #'handle-keydown}}]
   ["/api/keymap" {:get {:handler #'handle-keymap-info}}]

   ;; Question nav (server-owned state)
   ["/api/nav/enter-question" {:post {:handler #'nav-handlers/handle-enter-question}}]
   ["/api/nav/move" {:post {:handler #'nav-handlers/handle-move}}]
   ["/api/nav/enter-answer" {:post {:handler #'nav-handlers/handle-enter-answer}}]
   ["/api/nav/escape" {:post {:handler #'nav-handlers/handle-escape}}]
   ["/api/nav/toggle-collapse" {:post {:handler #'nav-handlers/handle-toggle-collapse}}]
   ["/api/nav/drill-in" {:post {:handler #'nav-handlers/handle-drill-in}}]
   ["/api/nav/drill-out" {:post {:handler #'nav-handlers/handle-drill-out}}]
   ["/api/nav/select" {:post {:handler #'nav-handlers/handle-select}}]
   ["/api/nav/click-answer" {:post {:handler #'nav-handlers/handle-click-answer}}]
   ["/api/clear" {:post {:handler #'nav-handlers/handle-clear}}]
   ["/api/clear-all" {:post {:handler #'nav-handlers/handle-clear-all}}]
   ["/api/save" {:post {:handler #'handle-save}}]
   ["/api/draft/accept-server" {:post {:handler #'handle-accept-server-draft}}]
   ["/api/save-draft" {:post {:handler #'handle-save-draft}}]
   ["/api/drafts" {:get {:handler #'handle-list-drafts}}]
   ["/api/copy" {:post {:handler #'handle-copy}}]
   ["/api/load-session" {:post {:handler #'handle-load-session}}]

   ;; AI Chat + Fanout + Critique
   ["/api/chat" {:post {:handler #'chat-handlers/handle-chat}}]
   ["/api/ai/context" {:post {:handler #'handle-ai-context}}]
   ["/api/prompt/plan" {:get {:handler #'handle-prompt-plan}}]
   ["/api/chat/propose" {:post {:handler #'chat-handlers/handle-propose}}]
   ["/api/editor/snapshot" {:get {:handler #'transform/handle-editor-snapshot}}]
   ["/api/editor/propose-replacement" {:post {:handler #'transform/handle-propose-exact}}]
   ["/api/editor/propose-node-replacement" {:post {:handler #'transform/handle-propose-inactive-exact}}]
   ["/api/editor/proposal-visible" {:post {:handler #'transform/handle-proposal-visible}}]
   ["/api/editor/proposal-status" {:post {:handler #'transform/handle-proposal-status}}]
   ["/api/editorial-diff/mode" {:post {:handler #'handle-editorial-diff-mode}}]
   ["/api/transform" {:post {:handler #'transform/handle-transform}}]
   ["/api/transform/refine" {:post {:handler #'transform/handle-refine}}]
   ["/api/transform/open" {:post {:handler #'transform/handle-open}}]
   ["/api/transform/select" {:post {:handler #'transform/handle-select}}]
   ["/api/transform/copy" {:post {:handler #'transform/handle-copy}}]
   ["/api/transform/apply" {:post {:handler #'transform/handle-apply}}]
   ["/api/transform/more" {:post {:handler #'transform/handle-more}}]
   ["/api/transform/cancel" {:post {:handler #'transform/handle-cancel}}]
   ["/api/transform/set-model" {:post {:handler #'transform/handle-set-model}}]
   ["/api/transform/format" {:post {:handler #'transform/handle-format}}]
   ["/api/draft/undo" {:post {:handler #'transform/handle-draft-undo}}]
   ["/api/draft/push-undo" {:post {:handler #'transform/handle-push-undo}}]
   ["/api/chat-edit" {:post {:handler #'chat-handlers/handle-chat-edit}}]
   ["/api/research-chat" {:post {:handler #'research-chat/handle-research-chat}}]
   ["/api/research-chat/clear" {:post {:handler #'research-chat/handle-clear-research-chat}}]
   ["/api/fanout" {:post {:handler #'handle-fanout}}]
   ["/api/cancel-fanout" {:post {:handler #'handle-cancel-fanout}}]
   ["/api/clear-fanout-history" {:post {:handler #'nav-handlers/handle-clear-fanout-history}}]
   ["/api/critique" {:post {:handler #'handle-critique}}]
   ["/api/apply-pill" {:post {:handler #'handle-apply-pill}}]
   ["/api/cherry-pick/toggle" {:post {:handler #'handle-cherry-pick-toggle}}]
   ["/api/cherry-pick-mark" {:post {:handler #'handle-cherry-pick-mark}}]
   ["/api/cherry-pick-apply" {:post {:handler #'handle-cherry-pick-apply}}]
   ["/api/archive-pill" {:post {:handler #'handle-archive-pill}}]
   ["/api/convert-synthesis-pills" {:post {:handler #'handle-convert-synthesis-pills}}]
   ["/api/archive-synthesis-pill" {:post {:handler #'handle-archive-synthesis-pill}}]
   ["/api/apply-edit-pill" {:post {:handler #'handle-apply-edit-pill}}]
   ["/api/dismiss-edit-pill" {:post {:handler #'handle-dismiss-edit-pill}}]
   ["/api/apply-all-edit-pills" {:post {:handler #'handle-apply-all-edit-pills}}]
   ["/api/dismiss-all-edit-pills" {:post {:handler #'handle-dismiss-all-edit-pills}}]
   ["/api/autocomplete" {:post {:handler #'handle-autocomplete}}]
   ["/api/clear-chat" {:post {:handler #'nav-handlers/handle-clear-chat}}]
   ["/api/model" {:post {:handler #'handle-switch-model}}]
   ["/api/fleet" {:post {:handler #'handle-switch-fleet}}]
   ["/api/fleets" {:get {:handler #'handle-get-fleets}}]
   ["/api/toggle-examples" {:post {:handler #'handle-toggle-examples}}]

   ;; Distillery (Option Distillation Studio)
   ["/api/distillery/enter" {:post {:handler #'distillery/handle-enter}}]
   ["/api/distillery/move" {:post {:handler #'distillery/handle-move}}]
   ["/api/distillery/unmerge" {:post {:handler #'distillery/handle-unmerge}}]
   ["/api/distillery/move-all" {:post {:handler #'distillery/handle-move-all}}]
   ["/api/distillery/delete" {:post {:handler #'distillery/handle-delete}}]
   ["/api/distillery/reorder" {:post {:handler #'distillery/handle-reorder}}]
   ["/api/distillery/add" {:post {:handler #'distillery/handle-add}}]
   ["/api/distillery/edit" {:post {:handler #'distillery/handle-edit}}]
   ["/api/distillery/edit-start" {:post {:handler #'distillery/handle-edit-start}}]
   ["/api/distillery/edit-cancel" {:post {:handler #'distillery/handle-edit-cancel}}]
   ["/api/distillery/split" {:post {:handler #'distillery/handle-split}}]
   ["/api/distillery/focus" {:post {:handler #'distillery/handle-focus}}]
   ["/api/distillery/insert-pill" {:post {:handler #'distillery/handle-insert-pill}}]
   ["/api/distillery/save" {:post {:handler #'distillery/handle-save}}]
   ["/api/distillery/save-center" {:post {:handler #'distillery/handle-save-center}}]
   ["/api/distillery/apply" {:post {:handler #'distillery/handle-apply}}]
   ["/api/distillery/chat" {:post {:handler #'distillery/handle-chat}}]
   ["/api/distillery/fanout" {:post {:handler #'distillery/handle-fanout}}]
   ["/api/distillery/merge-ai" {:post {:handler #'distillery/handle-merge-ai}}]
   ["/api/distillery/clear-ai" {:post {:handler #'distillery/handle-clear-ai}}]
   ["/api/distillery/undo" {:post {:handler #'distillery/handle-undo}}]
   ["/api/distillery/drag" {:post {:handler #'distillery/handle-drag}}]
   ["/api/distillery/indent" {:post {:handler #'distillery/handle-indent}}]
   ["/api/distillery/toggle-collapse" {:post {:handler #'distillery/handle-toggle-collapse}}]
   ["/api/distillery/reorder-outline" {:post {:handler #'distillery/handle-reorder-outline}}]

   ;; Bucket operations (manuscript + workbenches)
   ["/api/distillery/pluck" {:post {:handler #'distillery/handle-pluck}}]
   ["/api/distillery/pluck-leftovers" {:post {:handler #'distillery/handle-pluck-leftovers}}]
   ["/api/distillery/pluck-trash" {:post {:handler #'distillery/handle-pluck-trash}}]
   ["/api/distillery/merge-bucket" {:post {:handler #'distillery/handle-merge-bucket}}]
   ["/api/distillery/untrash" {:post {:handler #'distillery/handle-untrash}}]
   ["/api/distillery/view-bucket" {:post {:handler #'distillery/handle-view-bucket}}]
   ["/api/distillery/unarchive-bucket" {:post {:handler #'distillery/handle-unarchive-bucket}}]
   ["/api/distillery/clear-bucket" {:post {:handler #'distillery/handle-clear-bucket}}]
   ["/api/distillery/import-bucket" {:post {:handler #'distillery/handle-import-bucket}}]
   ["/api/distillery/summarize-buckets" {:post {:handler #'distillery/handle-summarize-buckets}}]
   ["/api/distillery/copy-from-drafts" {:post {:handler #'distillery/handle-copy-from-drafts}}]

   ;; Multi-Lane Distillery
   ["/api/multi-draft/accumulate" {:post {:handler #'multi-draft/handle-accumulate}}]
   ["/api/multi-draft/accumulate-model" {:post {:handler #'multi-draft/handle-accumulate-model}}]
   ["/api/multi-draft/remove" {:post {:handler #'multi-draft/handle-remove}}]
   ["/api/multi-draft/use" {:post {:handler #'multi-draft/handle-use}}]
   ["/api/multi-draft/merge" {:post {:handler #'multi-draft/handle-merge}}]
   ["/api/multi-draft/merge-pill" {:post {:handler #'multi-draft/handle-merge-pill}}]
   ["/api/multi-draft/use-assembly" {:post {:handler #'multi-draft/handle-use-assembly}}]
   ["/api/multi-draft/move" {:post {:handler #'multi-draft/handle-move}}]
   ["/api/multi-draft/toggle-mode" {:post {:handler #'multi-draft/handle-toggle-mode}}]
   ["/api/multi-draft/clear" {:post {:handler #'multi-draft/handle-clear}}]
   ["/api/multi-draft/nav" {:post {:handler #'multi-draft/handle-nav}}]
   ["/api/multi-draft/delete-pill" {:post {:handler #'multi-draft/handle-delete-pill}}]
   ["/api/multi-draft/open" {:post {:handler #'multi-draft/handle-open}}]
   ["/api/multi-draft/shift-lane" {:post {:handler #'multi-draft/handle-shift-lane}}]
   ["/api/multi-draft/split-pill" {:post {:handler #'multi-draft/handle-split-pill}}]
   ["/api/multi-draft/new-lane" {:post {:handler #'multi-draft/handle-new-lane}}]
   ["/api/multi-draft/load-assembly" {:post {:handler #'multi-draft/handle-load-assembly}}]
   ["/api/multi-draft/start-rename" {:post {:handler #'multi-draft/handle-start-rename}}]
   ["/api/multi-draft/rename-lane" {:post {:handler #'multi-draft/handle-rename-lane}}]
   ["/api/multi-draft/assembly-reorder" {:post {:handler #'multi-draft/handle-assembly-reorder}}]
   ["/api/multi-draft/assembly-delete" {:post {:handler #'multi-draft/handle-assembly-delete}}]
   ["/api/multi-draft/assembly-to-lane" {:post {:handler #'multi-draft/handle-assembly-to-lane}}]
   ["/api/multi-draft/assembly-merge" {:post {:handler #'multi-draft/handle-assembly-merge}}]
   ["/api/multi-draft/move-pill-lane" {:post {:handler #'multi-draft/handle-move-pill-lane}}]
   ["/api/multi-draft/reorder-pill" {:post {:handler #'multi-draft/handle-reorder-pill}}]

   ;; Draft History
   ["/api/history/select" {:post {:handler #'handle-history-select}}]
   ["/api/history/nav" {:post {:handler #'handle-history-nav}}]
   ["/api/history/load" {:post {:handler #'handle-history-load}}]
   ["/api/history/accumulate" {:post {:handler #'handle-history-accumulate}}]

   ;; Book Workshop
   ["/api/field-focus" {:post {:handler #'book/handle-field-focus}}]
   ["/api/book/focus" {:post {:handler #'book/handle-focus}}]
   ["/api/book/nav" {:post {:handler #'book/handle-nav}}]
   ["/api/book/reorder" {:post {:handler #'book/handle-reorder}}]
   ["/api/book/drag" {:post {:handler #'book/handle-drag}}]
   ["/api/book/move-within-project" {:post {:handler #'book/handle-move-within-project}}]
   ["/api/book/new-container" {:post {:handler #'book/handle-new-container}}]
   ["/api/book/indent" {:post {:handler #'book/handle-indent}}]
   ["/api/book/toggle" {:post {:handler #'book/handle-toggle}}]
   ["/api/book/new-node" {:post {:handler #'book/handle-new-node}}]
   ["/api/book/delete" {:post {:handler #'book/handle-delete}}]
   ["/api/book/duplicate" {:post {:handler #'book/handle-duplicate}}]
   ["/api/book/update" {:post {:handler #'book/handle-update}}]
   ["/api/book/outline" {:get {:handler #'book/handle-outline}}]
   ["/api/book/replace-inactive-node" {:post {:handler #'book/handle-replace-inactive-node}}]
   ["/api/book/rename-node" {:post {:handler #'book/handle-rename-node}}]
   ["/api/book/rename-active-node" {:post {:handler #'book/handle-rename-active-node}}]
   ["/api/book/switch-project" {:post {:handler #'book/handle-switch-project}}]
   ["/api/book/new-project" {:post {:handler #'book/handle-new-project}}]
   ["/api/book/start-rename-project" {:post {:handler #'book/handle-start-rename-project}}]
   ["/api/book/cancel-rename-project" {:post {:handler #'book/handle-cancel-rename-project}}]
   ["/api/book/rename-project" {:post {:handler #'book/handle-rename-project}}]
   ["/api/book/edit-in-draft" {:post {:handler #'book/handle-edit-in-draft}}]
   ["/api/book/open-node" {:post {:handler #'book/handle-open-node}}]
   ["/api/book/open-visible-request" {:post {:handler #'book/handle-open-visible-request}}]
   ["/api/book/open-visible" {:post {:handler #'book/handle-open-visible}}]
   ["/api/book/open-status" {:post {:handler #'book/handle-open-status}}]
   ["/api/book/upsert-post" {:post {:handler #'book/handle-upsert-post}}]
   ["/api/book/node-ref" {:post {:handler #'smw-write-handlers/handle-node-ref}}]
   ["/api/book/draft/prepend-section" {:post {:handler #'smw-write-handlers/handle-prepend-section}}]
   ["/api/book/write-visible-commit" {:post {:handler #'smw-write-handlers/handle-visible-commit}}]
   ["/api/book/write-visible-confirm" {:post {:handler #'smw-write-handlers/handle-visible-confirm}}]
   ["/api/book/write-status" {:post {:handler #'smw-write-handlers/handle-status}}]
   ["/api/book/save-from-draft" {:post {:handler #'book/handle-save-from-draft}}]
   ["/file/open" {:get {:handler #'file-document/handle-open-link}}]
   ["/api/file/open-visible-request" {:post {:handler #'file-document/handle-open-visible-request}}]
   ["/api/file/open" {:post {:handler #'file-document/handle-open}}]
   ["/api/file/open-visible" {:post {:handler #'file-document/handle-open-visible}}]
   ["/api/file/open-status" {:post {:handler #'file-document/handle-open-status}}]
   ["/api/file/save" {:post {:handler #'file-document/handle-save}}]
   ["/api/file/history" {:post {:handler #'file-document/handle-history}}]
   ["/api/file/read" {:post {:handler #'file-document/handle-read}}]
   ["/api/file/replace" {:post {:handler #'file-document/handle-replace}}]
   ["/api/file/restore" {:post {:handler #'file-document/handle-restore}}]
   ["/api/book/move-node" {:post {:handler #'book/handle-move-node}}]
   ["/api/book/move-node-by-uuid" {:post {:handler #'book/handle-move-node-by-uuid}}]
   ["/api/book/compile" {:post {:handler #'book/handle-compile}}]
   ["/api/book/compile-to-doc" {:post {:handler #'book/handle-compile-to-doc}}]
   ["/api/book/restore-posts-backup" {:post {:handler #'handle-restore-posts-from-backup}}]
   ["/api/book/check-orphans" {:get {:handler #'handle-check-orphans}}]
   ["/api/book/recover-orphans" {:post {:handler #'handle-recover-orphans}}]
   ["/api/book/restore-orphan" {:post {:handler #'handle-restore-single-orphan}}]
   ["/api/book/link-orphan" {:post {:handler #'handle-link-orphan}}]
   ["/api/book/dismiss-orphan" {:post {:handler #'handle-dismiss-orphan}}]
   ["/api/book/dismiss-all-orphans" {:post {:handler #'handle-dismiss-all-orphans}}]
   ["/api/book/toggle-orphan-expanded" {:post {:handler #'handle-toggle-orphan-expanded}}]

   ;; Debug — non-authoritative diagnostic projection (bd 3jz). /summary is the
   ;; canonical name; /state is kept as an alias for existing diagnostic
   ;; consumers (both now fail closed with typed preview wrappers).
   ["/api/debug/state" {:get {:handler #'handle-debug-state}}]
   ["/api/debug/summary" {:get {:handler #'handle-debug-state}}]

   ;; Goal management
   ["/api/goals" {:get {:handler #'handle-get-goals}}]
   ["/api/goal" {:post {:handler #'handle-switch-goal}}]

   ;; Project management
   ["/api/projects" {:get {:handler #'handle-list-projects}
                     :post {:handler #'handle-create-project}}]
   ["/api/projects/start-create" {:post {:handler #'handle-start-create-project}}]
   ["/api/projects/cancel-create" {:post {:handler #'handle-cancel-create-project}}]
   ["/api/project" {:post {:handler #'handle-switch-project}}]
   ["/api/reload-projects" {:post {:handler #'handle-reload-projects}}]
   ["/api/explorer/toggle" {:post {:handler #'handle-explorer-toggle}}]
   ["/api/explorer/select" {:post {:handler #'handle-explorer-select}}]
   ["/api/outline/jump" {:post {:handler #'handle-outline-jump}}]
   ["/api/inbox/toggle" {:post {:handler #'handle-inbox-toggle}}]
   ["/api/inbox/refresh" {:post {:handler #'handle-inbox-refresh}}]
   ["/api/inbox/import" {:post {:handler #'handle-inbox-import}}]
   ["/api/inbox/dismiss" {:post {:handler #'handle-inbox-dismiss}}]
   ["/api/log-action" {:post {:handler #'handle-log-action}}]
   ["/api/notify" {:post {:handler #'handle-notify}}]
   ["/api/notify/clear" {:post {:handler #'handle-notify-clear}}]

   ;; Dashboard
   ["/dashboard" {:get {:handler #'handle-dashboard}}]

   ;; Mockup pages
   ["/mockups" {:get {:handler #'mockups/handle-mockups-index}}]
   ["/mockups/" {:get {:handler #'mockups/handle-mockups-index}}]
   ["/mockups/a" {:get {:handler #'mockups/handle-mockup-a}}]
   ["/mockups/b" {:get {:handler #'mockups/handle-mockup-b}}]
   ["/mockups/c" {:get {:handler #'mockups/handle-mockup-c}}]
   ["/mockups/d" {:get {:handler #'mockups/handle-mockup-d}}]

   ;; Nav spike (keyboard architecture PoC)
   ["/mockups/nav-spike" {:get {:handler #'nav-spike/handle-nav-spike}}]
   ["/mockups/nav-spike/sse" {:get {:handler #'nav-spike/handle-spike-sse
                                    :muuntaja false}}]
   ["/mockups/nav-spike/move" {:post {:handler #'nav-spike/handle-spike-move}}]
   ["/mockups/nav-spike/escape" {:post {:handler #'nav-spike/handle-spike-escape}}]
   ["/mockups/nav-spike/simulate-push" {:post {:handler #'nav-spike/handle-spike-simulate-push}}]

   ;; AI Answers spike (question/answer/dropdown nav PoC)
   ["/mockups/ai-answers" {:get {:handler #'ai-spike/handle-page}}]
   ["/mockups/ai-answers/sse" {:get {:handler #'ai-spike/handle-spike-sse
                                     :muuntaja false}}]
   ["/mockups/ai-answers/move" {:post {:handler #'ai-spike/handle-spike-move}}]
   ["/mockups/ai-answers/enter-question" {:post {:handler #'ai-spike/handle-spike-enter-question}}]
   ["/mockups/ai-answers/enter-answer" {:post {:handler #'ai-spike/handle-spike-enter-answer}}]
   ["/mockups/ai-answers/escape" {:post {:handler #'ai-spike/handle-spike-escape}}]
   ["/mockups/ai-answers/select" {:post {:handler #'ai-spike/handle-spike-select}}]
   ["/mockups/ai-answers/toggle-collapse" {:post {:handler #'ai-spike/handle-spike-toggle-collapse}}]
   ["/mockups/ai-answers/drill-in" {:post {:handler #'ai-spike/handle-spike-drill-in}}]
   ["/mockups/ai-answers/drill-out" {:post {:handler #'ai-spike/handle-spike-drill-out}}]
   ["/mockups/ai-answers/toggle-pick" {:post {:handler #'ai-spike/handle-spike-toggle-pick}}]
   ["/mockups/ai-answers/click-answer" {:post {:handler #'ai-spike/handle-spike-click-answer}}]
   ["/mockups/ai-answers/reload" {:post {:handler #'ai-spike/handle-spike-reload}}]

   ;; Dev reload endpoint
   ["/dev/reload-check" {:get {:handler
                               (fn [req]
                                 (if-let [h (try (requiring-resolve 'browser-reload.core/reload-check-handler)
                                                 (catch Exception _ nil))]
                                   (h req)
                                   {:status 404}))}}]])
