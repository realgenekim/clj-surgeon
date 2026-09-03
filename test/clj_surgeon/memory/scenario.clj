(ns clj-surgeon.memory.scenario
  "The per-file work both memory arms perform, so the arms differ only in what
   they retain.

   The scenario is one alias migration: every generated namespace requires
   `scope.old.lib` and must require `scope.new.lib` instead. The parse is the
   real cost; the edit is structural, through the parsed node tree."
  (:require
   [clj-surgeon.memory.fixture :as fixture]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as p]
   [rewrite-clj.zip :as z])
  (:import
   (java.security MessageDigest)))

(defn sha256
  "Lowercase hex SHA-256 of `source` as UTF-8 bytes."
  [^String source]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes source "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn- ns-node?
  [node]
  (and (= :list (n/tag node))
       (= 'ns (some-> (first (n/children node)) n/sexpr))))

(defn- migrate-ns-node
  [node]
  (let [zloc (z/of-node node)
        found (z/find-value zloc z/next (symbol fixture/old-lib))]
    (when-not found
      (throw (ex-info "Generated namespace does not require the retired lib"
                      {:error-type :scenario-fixture-drift})))
    (z/root (z/replace found (symbol fixture/new-lib)))))

(defn plan-file
  "Parse `source`, migrate its require, and return the compact plan facts.

   Nothing in the returned map retains the parse tree or a zipper location:
   the caller receives the pre-image hash, the result hash and the replacement
   text, which is exactly what the transaction manifest and staging need."
  [path ^String source]
  (let [root (p/parse-string-all source)
        children (vec (n/children root))
        index (first (keep-indexed (fn [i c] (when (ns-node? c) i)) children))
        _ (when-not index
            (throw (ex-info "Generated file has no ns form"
                            {:error-type :scenario-fixture-drift :path path})))
        migrated (migrate-ns-node (nth children index))
        result (n/string (n/replace-children root (assoc children index migrated)))]
    {:path path
     :source-hash (sha256 source)
     :result-hash (sha256 result)
     :result result}))

(defn fact
  "The plan facts without the replacement text: what a bounded index retains."
  [plan]
  (dissoc plan :result))
