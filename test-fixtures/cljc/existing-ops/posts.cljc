(ns myapp.model.posts
  "Post model — heavy use of #?(:clj ...) for server-side resolvers.
   Mirrors the fulcro-reddit reddit_posts.cljc pattern."
  (:require
   [clojure.string :as str]
   #?(:clj [myapp.db.queries :as queries])
   #?(:cljs [myapp.lib.jsutils :as jsu])))

;; ---- Shared data transforms ----

(defn normalize-title [title]
  (-> title str/trim str/lower-case))

(declare enrich-post)

(defn format-post [post]
  (-> post
      (update :title normalize-title)
      enrich-post))

(defn- add-slug [post]
  (assoc post :slug
         (-> (:title post)
             (str/replace #"[^a-z0-9]+" "-")
             (str/replace #"^-|-$" ""))))

(defn enrich-post [post]
  (-> post
      add-slug
      (assoc :enriched? true)))

;; ---- CLJ-only: resolvers ----

#?(:clj
   (defn resolve-posts
     "Server resolver — depends on shared format-post."
     [env query-params]
     (->> (queries/get-posts env query-params)
          (mapv format-post))))

#?(:clj
   (defn resolve-post-detail
     "Single post detail resolver."
     [env post-id]
     (when-let [post (queries/get-post-by-id env post-id)]
       (format-post post))))

;; ---- CLJS-only: client transforms ----

#?(:cljs
   (defn client-format-post
     "Client-side post enrichment."
     [post]
     (-> (format-post post)
         (assoc :display-date (jsu/format-date (:created-at post))))))

#?(:cljs
   (defn post-summary
     "Render-ready summary for the post list."
     [post]
     {:title (normalize-title (:title post))
      :slug (:slug (enrich-post post))}))
