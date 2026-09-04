


































































































































































































































































































































(defn handle-apply-edit-pill
  "POST /api/apply-edit-pill — apply a single edit pill's find/replace to the draft."
  [request]
  (let [{:keys [pill-idx draft]} (http/parse-json-body request)
        ;; Sync browser draft to atom if provided (avoid stale-read race)
        _ (when (and draft (seq draft))
            (state/set-draft! draft :browser-sync true))
        pill-idx (int pill-idx)
        pill (get-in @state/app-state [:edit-pills :pills pill-idx])]
    (if-not pill
      (do (sse/push-notification! "Edit pill not found")
          {:status 204})
      (let [old-draft (:draft @state/app-state) ;; OK: re-read after sync
            summary (execute-search-and-replace! {:find (:find pill)
                                                  :replace (:replace pill)
                                                  :scope (:scope pill)})
            new-draft (:draft @state/app-state) ;; OK: re-read after search-and-replace
            changed? (not= old-draft new-draft)]
        (if changed?
          (do (state/mark-edit-pill-applied! pill-idx)
              (state/log-event! {:type "edit-pill.apply" :pill-idx pill-idx
                                 :find (:find pill) :replace (:replace pill)})
              (push-tab-aware-draft! (or (sse/diff-highlight-range old-draft new-draft) {}))
              (sse/push-chat-panel!)
              (sse/push-notification! (or summary "Applied edit"))
              (state/save-session!))
          (do (sse/push-notification! (str "No match found in draft"))
              (sse/push-chat-panel!)))
        {:status 204}))))












(defn handle-apply-all-edit-pills
  "POST /api/apply-all-edit-pills — apply all pending edit pills in order."
  [request]
  (let [{browser-draft :draft} (try (http/parse-json-body request) (catch Exception _ {}))
        ;; Sync browser draft to atom if provided (avoid stale-read race)
        _ (when (and browser-draft (seq browser-draft))
            (state/set-draft! browser-draft :browser-sync true))
        pills (get-in @state/app-state [:edit-pills :pills])
        pending (keep-indexed (fn [idx p]
                                (when (and (not (:applied? p)) (not (:dismissed? p)))
                                  [idx p]))
                              pills)
        results (doall
                  (for [[idx pill] pending]
                    (let [summary (execute-search-and-replace!
                                    {:find (:find pill) :replace (:replace pill)
                                     :scope (:scope pill)})]
                      (state/mark-edit-pill-applied! idx)
                      summary)))]
    (state/log-event! {:type "edit-pill.apply-all" :count (count results)})
    (push-tab-aware-draft! {})
    (sse/push-chat-panel!)
    (sse/push-notification! (str "Applied " (count results) " edit" (when (> (count results) 1) "s")))
    (state/save-session!)
    {:status 204}))















































































