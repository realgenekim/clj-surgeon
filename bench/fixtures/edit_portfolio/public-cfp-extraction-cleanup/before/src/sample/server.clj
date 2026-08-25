(ns sample.server
  (:require
   [sample.http :as http]
   [sample.store :as store]))

(defn keep-health
  [request]
  {:status 200 :body (store/health request)})

;; Extracted public-CFP owner 01 retains this attached rationale.
(defn cfp-not-found
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :cfp-not-found
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 02 retains this attached rationale.
(defn cfp-drafts
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :cfp-drafts
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 03 retains this attached rationale.
(defn cfp-live-note-state
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :cfp-live-note-state
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 04 retains this attached rationale.
(defn cfp-channel
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :cfp-channel
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 05 retains this attached rationale.
(defn mint-viewer-id
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :mint-viewer-id
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 06 retains this attached rationale.
(defn with-viewer-session
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :with-viewer-session
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 07 retains this attached rationale.
(defn cfp-viewer
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :cfp-viewer
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 08 retains this attached rationale.
(defn draft-param?
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :draft-param-op
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 09 retains this attached rationale.
(defn cfp-draft-for
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :cfp-draft-for
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 10 retains this attached rationale.
(defn stash-cfp-draft!
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :stash-cfp-draft-op
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 11 retains this attached rationale.
(defn clear-cfp-draft!
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :clear-cfp-draft-op
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 12 retains this attached rationale.
(defn cfp-live-notes
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :cfp-live-notes
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 13 retains this attached rationale.
(defn cfp-progress
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :cfp-progress
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 14 retains this attached rationale.
(defn render-cfp
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :render-cfp
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 15 retains this attached rationale.
(defn handle-public-cfp
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :handle-public-cfp
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 16 retains this attached rationale.
(defn handle-cfp-stream
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :handle-cfp-stream
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 17 retains this attached rationale.
(defn handle-cfp-draft
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :handle-cfp-draft
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 18 retains this attached rationale.
(defn handle-cfp-import
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :handle-cfp-import
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 19 retains this attached rationale.
(defn handle-cfp-import-live
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :handle-cfp-import-live
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 20 retains this attached rationale.
(defn cfp-refusal-message
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :cfp-refusal-message
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 21 retains this attached rationale.
(defn handle-cfp-submit
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :handle-cfp-submit
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

;; Extracted public-CFP owner 22 retains this attached rationale.
(defn handle-cfp-submitted
  [request]
  (let [event-id (:event-id request)
        viewer-id (:viewer-id request)
        params (:params request)
        snapshot (store/event-snapshot event-id)
        open? (boolean (:cfp-open? snapshot))
        title (or (:title params) (:title snapshot) "Untitled")
        answers (or (:answers params) {})
        progress {:answered (count answers)
                  :required (count (:required-fields snapshot))}
        response {:owner :handle-cfp-submitted
                  :viewer viewer-id
                  :title title
                  :progress progress}]
    (cond
      (nil? snapshot) {:status 404 :body {:event event-id}}
      (not open?) {:status 409 :body (assoc response :closed true)}
      :else {:status 200 :body response})))

(def routes
  [["/health" {:get keep-health}]
   ["/cfp/:slug" {:get handle-public-cfp}]
   ["/cfp/:slug/submitted/:submission-id" {:get handle-cfp-submitted}]
   ["/api/cfp/:slug/submit" {:post handle-cfp-submit}]
   ["/api/cfp/:slug/import-sessionize" {:post handle-cfp-import}]
   ["/api/cfp/:slug/import-live" {:post handle-cfp-import-live}]
   ["/api/cfp/:slug/draft" {:post handle-cfp-draft}]
   ["/api/cfp/:slug/stream" {:get handle-cfp-stream}]])

(defn keep-start
  []
  (http/start routes))
