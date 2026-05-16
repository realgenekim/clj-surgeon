(ns fixtures.mb-shapes
  "Representative Metabase macro shapes for selector DSL testing.
   NOT compiled or run — only parsed structurally. Symbols can be
   undefined, requires can be dummied. Source only."
  (:require [api.macros]
            [malli.util :as mu]))

;; ---- api.macros/defendpoint ----

(api.macros/defendpoint :get "/:key"
  "Fetch a custom GeoJSON file."
  [{k :key, :as _route-params} :- [:map [:key ms/NonBlankString]]
   _query-params
   _body
   _request
   respond
   raise]
  (println "endpoint body"))

(api.macros/defendpoint :get "/"
  "Load a custom GeoJSON file."
  [_route-params
   {:keys [url], :as _query-params} :- [:map [:url ms/NonBlankString]]
   _body
   _request
   respond
   raise]
  (println "endpoint body 2"))

;; ---- defenterprise ----

(defenterprise enable-custom-viz?
  "Should we enable custom visualizations?"
  metabase-enterprise.custom-viz-plugin.settings
  []
  false)

(defenterprise multi-arg-fn
  "Doc."
  metabase-enterprise.somewhere
  [a b c]
  (+ a b c))

;; ---- defsetting ----

(defsetting application-name
  (deferred-tru "Replace the word.")
  :encryption :no
  :visibility :public
  :default    "Metabase")

(defsetting site-name
  (deferred-tru "Site name.")
  :encryption :no
  :default    "Metabase")

;; ---- mu/defn with meta arglist ----

(mu/defn prefix :- ::api-keys.schema/prefix
  "Returns prefix."
  ^String [k :- [:or ::secret ::prefix]]
  (subs k 0 3))

;; ---- mu/defn without meta arglist ----

(mu/defn generate-key :- ::key.secret
  "Generates a key."
  []
  "mb_abc")
