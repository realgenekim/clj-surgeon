(ns fixtures.show-form-migration
  (:require
   [jsonista.core :as json]
   [next.jdbc :as jdbc]))

(def connection-options
  {:pool-size 8
   :timeout-ms 5000})

;; Production-migration-derived shape: the observed agent needed to read this
;; complete named writer from a large database namespace. Names and data are
;; minimized; metadata, comments, nested calls, and platform context remain.
(defn ^:private upsert-starred-post!
  "Write one account/post relationship and return its durable ordinal."
  [{:keys [account-id post-id ordinal payload]}]
  (jdbc/execute-one!
    connection-options
    ["insert into starred_posts
       (account_id, post_id, ordinal, payload)
     values (?, ?, ?, ?::jsonb)
     on conflict (account_id, post_id, ordinal)
     do update set payload = excluded.payload
     returning ordinal"
     account-id
     post-id
     ordinal
     (json/write-value-as-string payload)]))

#?(:clj
   (defn load-starred-post
     [account-id post-id]
     (jdbc/execute-one! connection-options
                        ["select * from starred_posts
                          where account_id = ? and post_id = ?"
                         account-id post-id]))
   :cljs
   (defn load-starred-post
     [_account-id _post-id]
     (throw (js/Error. "Server-only operation"))))

(comment
  (upsert-starred-post!
    {:account-id 7
     :post-id 42
     :ordinal 3
     :payload {:timestamp 1.7040672E12}}))
