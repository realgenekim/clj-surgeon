(ns myapp.ui.mutations
  "Mutation split — mirrors real Fulcro db_mutations.cljc pattern.
   Same mutation name, completely different implementations per platform."
  (:require
   [clojure.string :as str]
   #?(:cljs [fulcro.mutations :as m])
   #?(:clj  [pathom.connect :as pco])
   #?(:cljs [myapp.ui.toast :as toast])
   #?(:clj  [myapp.db.queries :as db])))

;; ---- Shared helpers ----

(defn validate-post-id [post-id]
  (and (string? post-id)
       (str/includes? post-id "/")))

(defn- format-result [ok? message]
  {:ok? ok? :message message})

;; ---- The mutation split: same name, different implementations ----

#?(:cljs
   (m/defmutation toggle-star
     "CLIENT: optimistic update + remote call"
     [params]
     (action [{:keys [state]}]
             (let [{:keys [post-id starred?]} params]
               (swap! state assoc-in [:post post-id :starred?] starred?)))
     (remote [env]
             (m/returning env nil))
     (ok-action [{:keys [result]}]
                (let [{:keys [ok? message]} (-> result :body :toggle-star)]
                  (when (not ok?)
                    (toast/toast! message))))
     (error-action [{:keys [result]}]
                   (toast/toast! "Network error")))
   :clj
   (do
     (pco/defmutation toggle-star
       "SERVER: database update"
       [env params]
       {}
       (let [{:keys [post-id starred?]} params]
         (when (validate-post-id post-id)
           (let [result (db/star-post! post-id starred?)]
             (format-result (some? result) "ok")))))))

;; ---- CLJ-only: resolver registry ----

#?(:clj
   (def resolvers [toggle-star]))

(comment
  ;; REPL testing
  (validate-post-id "subreddit/abc123"))
