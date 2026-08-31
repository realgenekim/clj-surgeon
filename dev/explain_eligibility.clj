(ns explain-eligibility
  "Dev-mode diagnostic: names every failing eligible-descriptor condition.
  Born in caller #1's tweezer session 2026-08-30 (four opaque refusals, one
  labeled checklist). Rung-3 residue: a recurring question converted to a
  predicate. Seed of the W8 discoverability wish."
  (:require [clj-surgeon.mcp-prepared-request]))

(defn- p
  "Resolve a private var from the projector namespace by name."
  [s]
  @(ns-resolve (quote clj-surgeon.mcp-prepared-request) (symbol s)))

(defn explain
  "Returns {:eligible? bool :failing [condition-keywords]} for a read result map."
  [result]
  (let [rows (:results result) row (first rows) forms (:forms row) file (:file row)
        suffix (when (string? file) ((p "suffix") file)) file-hash (:file_hash row)
        src-chars (when (vector? forms) (reduce + 0 (map #(count (:source %)) forms)))
        owners (when (vector? forms) (mapv :name forms))
        checks [[:ok (true? (:ok result))]
                [:read-complete (true? (:read_complete result))]
                [:next-action-none (= "none" (:next_action result))]
                [:operation (= "inspect_clojure" (:operation result))]
                [:one-request (= 1 (:request_count result))]
                [:one-file (= 1 (:file_count result))]
                [:one-row (= 1 (count rows))]
                [:row-forms-op (= "forms" (:operation row))]
                [:rel-file (boolean (and (string? file) ((p "project-relative-file?") file)))]
                [:known-suffix (boolean suffix)]
                [:file-sha ((p "exact-sha256?") file-hash)]
                [:forms-vec (vector? forms)]
                [:count-1-6 (boolean (and (vector? forms) (<= 1 (count forms) 6)))]
                [:count=form_count (= (count forms) (:form_count row))]
                [:chars=row (= src-chars (:source_character_count row))]
                [:chars=result (= src-chars (:source_character_count result))]
                [:owners-distinct (= (count owners) (count (distinct owners)))]
                [:every-form-evidence
                 (boolean (and (vector? forms) suffix
                               (every? #((p "form-evidence?") file file-hash
                                         (get (p "supported-platforms") suffix) %)
                                       forms)))]
                [:canonical-root ((p "canonical-root?") (:workspace_root result))]
                [:hashes-match (= {file file-hash} (:file_hashes result))]
                [:no-artifacts-result ((p "absent-artifacts?") result)]
                [:no-artifacts-row ((p "absent-artifacts?") row)]]
        failing (mapv first (remove second checks))]
    {:eligible? (empty? failing) :failing failing}))
