(ns clj-surgeon.thread-parameter-fixture
  "Canonical PRE/POST corpus for the thread-parameter planner.

  POST is rendered from this description, never from the planner under test."
  (:require
   [clojure.string :as str]))

(def owner-file "src/app/a.clj")

(defn request
  ([] (request {}))
  ([overrides]
   (merge {:request {:from {:file owner-file :var "submit!"}
                     :param {:name "opts" :default "nil" :position :last}
                     :scope {:paths ["src/**"]}}}
          overrides)))

(def ^:private owner-pre
  (str "(ns app.a)\n\n"
       "(defn submit!\n"
       "  \"Submit one request.\"\n"
       "  [request]\n"
       "  (assoc request :submitted true))\n"))

(def ^:private owner-post
  (str "(ns app.a)\n\n"
       "(defn submit!\n"
       "  \"Submit one request.\"\n"
       "  [request opts]\n"
       "  (assoc request :submitted true))\n"))

(def ^:private descriptions
  [{:file "src/app/alias_one.clj"
    :ns "app.alias-one"
    :requires "[app.a :as a]"
    :pre [(str "(defn send-one [x]\n"
               "  ;; a/submit! in commentary is protected\n"
               "  [\"a/submit!\" #_(a/submit! :discarded) (a/submit! x)])")
          "(defn send-pair [x y]\n  [(a/submit! x) (a/submit! y)])"]
    :post [(str "(defn send-one [x]\n"
                "  ;; a/submit! in commentary is protected\n"
                "  [\"a/submit!\" #_(a/submit! :discarded) (a/submit! x nil)])")
           "(defn send-pair [x y]\n  [(a/submit! x nil) (a/submit! y nil)])"]}

   {:file "src/app/alias_two.clj"
    :ns "app.alias-two"
    :requires "[app.a :as api]"
    :pre ["(defn forward [x]\n  (api/submit! x))"
          "(defn forward-later [x]\n  (when x (api/submit! x)))"]
    :post ["(defn forward [x]\n  (api/submit! x nil))"
           "(defn forward-later [x]\n  (when x (api/submit! x nil)))"]}

   {:file "src/app/referred.clj"
    :ns "app.referred"
    :requires "[app.a :refer [submit!]]"
    :pre ["(defn bare [x]\n  (submit! x))"
          "(defn bare-pair [x y]\n  [(submit! x) (submit! y)])"]
    :post ["(defn bare [x]\n  (submit! x nil))"
           "(defn bare-pair [x y]\n  [(submit! x nil) (submit! y nil)])"]}

   {:file "src/app/qualified.clj"
    :ns "app.qualified"
    :requires nil
    :pre ["(defn qualified [x]\n  (app.a/submit! x))"
          "(defn qualified-pair [x y]\n  [(app.a/submit! x)\n   (app.a/submit! y)])"]
    :post ["(defn qualified [x]\n  (app.a/submit! x nil))"
           "(defn qualified-pair [x y]\n  [(app.a/submit! x nil)\n   (app.a/submit! y nil)])"]}])

(defn- render-file
  [{:keys [ns requires] :as description} version]
  (str "(ns " ns
       (when requires (str "\n  (:require " requires ")"))
       ")\n\n"
       (str/join "\n\n" (get description version))
       "\n"))

(def canonical-files
  (into [{:file owner-file :pre owner-pre :post owner-post}]
        (map (fn [{:keys [file] :as description}]
               {:file file
                :pre (render-file description :pre)
                :post (render-file description :post)}))
        descriptions))

(def canonical-counts
  {:owner-files 1 :caller-files 4 :call-sites 11 :changed-files 5 :edits 9})

(defn sources
  ([] (sources :happy))
  ([variant]
   (let [base (mapv #(select-keys % [:file :pre]) canonical-files)
         source-entries (mapv #(hash-map :file (:file %) :source (:pre %)) base)]
     (case variant
       :happy source-entries
       :ambiguous-owner
       (mapv #(if (= owner-file (:file %))
                (update % :source str "\n(defn submit! [request] request)\n")
                %)
             source-entries)
       :multi-arity
       (mapv #(if (= owner-file (:file %))
                (assoc % :source
                       (str "(ns app.a)\n\n"
                            "(defn submit!\n"
                            "  ([request] (submit! request nil))\n"
                            "  ([request opts] (assoc request :opts opts)))\n"))
                %)
             source-entries)
       :indirect-reference
       (mapv #(if (= "src/app/alias_one.clj" (:file %))
                (update % :source str "\n(def submit-fn a/submit!)\n")
                %)
             source-entries)))))

(defn canonical-source [file]
  (some #(when (= file (:file %)) (:post %)) canonical-files))
