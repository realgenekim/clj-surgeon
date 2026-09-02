(ns clj-surgeon.mcp-close-losers
  "Close the measured-loser write shapes at the MCP server.

   Gene, 2026-09-02: \"Close all surgeon paths that are losers.\" The winners
   share the same public verbs, so the losers are closed as typed refusals that
   hand back the measured winner, not by removing a tool.

   Evidence:
   docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md
   receipt 09:01Z — separation by verb shape, 3 of 3 against 0 of 4. Every run
   using an owner of kind namespace plus find and insert or replace reformatted
   about 425 lines of untouched source; every run using `within` plus `from`/`to`
   or the dedicated `require_change` verb reformatted none.

   Design: docs/intent/mcp-operation-contract/close-losers-design.md. Pure."
  (:require
   [clojure.string :as str]))

(def ^:private lib-alias-pattern
  #"^\[\s*([A-Za-z0-9_.$%&*+\-!?<>=|/]+)\s+:as\s+([A-Za-z0-9_.$%&*+\-!?<>=|]+)\s*\]$")

;; @spec MCP-OP-CLOSE-013
(defn field
  "Read one request field by name whether the transport left string keys or
   keyword keys.

   `mcp-tool/execute-request!` round-trips every request through
   `json/parse-string ... true`, so the map that reaches validation carries
   KEYWORD keys while a hand-built test map carries string keys. `mcp-contract`
   already reads every field through an accessor of this shape. A refusal that
   reads only string keys is dead on the real server and green in its own unit
   tests, which is a verifier blind to its own subject, so the normalization
   happens once, here, at the entry of the refusal."
  [m key]
  (when (map? m)
    (if (contains? m key)
      (get m key)
      (get m (keyword key)))))

;; Every action that re-stages a whole owner. Under `owner {:kind "namespace"}`
;; that owner is the whole file, so each of these is the measured loser. The
;; kernel already refuses `delete`, `rename_binding`, and an insertion without
;; `find` under a namespace owner with their own typed reasons; they keep them.
(def ^:private restaging-actions
  [["replace" :replace]
   ["insert_before" :insert-before]
   ["insert_after" :insert-after]
   ["assoc_entry" :assoc-entry]])

(defn- namespace-owner?
  [change]
  (let [owner (field change "owner")]
    (and (map? owner) (= "namespace" (field owner "kind")))))

;; @spec MCP-OP-CLOSE-014
(defn- refused-action
  "The action on a namespace-scoped change that re-stages the whole file."
  [change]
  (some (fn [[wire-name action]]
          (when (some? (field change wire-name)) action))
        restaging-actions))

(defn- within
  [change]
  (let [owner-name (field (field change "owner") "name")]
    {:namespace (if (str/blank? (str owner-name)) true owner-name)}))

(defn- per-file-matches
  [change files]
  (let [expect (field change "expect")]
    (cond
      (= 1 (count files)) (or (field expect "matches") 1)
      (field expect "each_file") (field expect "each_file")
      :else 1)))

;; @spec MCP-OP-CLOSE-014
(defn- assoc-entry-replacement
  "The map source `assoc_entry` would have produced, spliced before the closing
   brace exactly as the kernel does."
  [find entry]
  (let [closing (when (string? find) (str/last-index-of find "}"))
        entry-key (field entry "key")
        entry-value (field entry "value")]
    (when (and closing entry-key entry-value)
      (str (subs find 0 closing) " " entry-key " " entry-value
           (subs find closing)))))

(defn- require-insertion
  "The lib and alias of a one-entry require insertion, or nil."
  [change action]
  (when (#{:insert-before :insert-after} action)
    (let [forms (field change (if (= :insert-before action)
                                "insert_before"
                                "insert_after"))]
      (when (and (sequential? forms) (= 1 (count forms)))
        (when-let [[_ lib alias] (re-matches lib-alias-pattern
                                             (str/trim (str (first forms))))]
          {:lib lib :as alias})))))

;; @spec MCP-OP-CLOSE-002
(defn- redirect
  "Fill an edit_clojure call from the refused request's own fields. Returns the
   call and the fields the caller must still supply.

   The workspace router strips `workspace_root` before validation, so it is
   attached by `attach-workspace-root` at the boundary that actually knows it."
  [change action]
  (let [files (vec (field change "files"))]
    (if-let [added (require-insertion change action)]
      ;; Y-5: nine namespaces, zero churn. The schema binds require_change to
      ;; symbol_migration, so this call is not complete until the caller adds
      ;; the migration rows it already holds.
      {:next-call
       {:tool "edit_clojure"
        :arguments {:require-change
                    {:add added
                     :files (mapv (fn [file] {:file file}) files)}}}
       :missing ["symbol_migration"]}
      (let [matches (per-file-matches change files)
            from (field change "find")
            to (case action
                 :replace (field change "replace")
                 :assoc-entry (assoc-entry-replacement
                                from (field change "assoc_entry"))
                 nil)
            edit-for (fn [file]
                       (cond-> {:file file
                                :within (within change)
                                :from from
                                :matches matches}
                         to (assoc :to to)))]
        {:next-call
         {:tool "edit_clojure"
          :arguments {:edits (mapv edit-for files)}}
         :missing
         (case action
           ;; @spec MCP-OP-CLOSE-016
           ;; One complete form to one complete form. These arguments replay
           ;; against edit_clojure exactly as handed to the caller.
           :replace nil
           ;; `assoc_entry` matched its map by value, but `from` matches by
           ;; exact syntax, so only the caller's own source spelling can
           ;; complete it. The replacement itself is derived.
           :assoc-entry ["from"]
           ;; `from` and `to` are one complete form each. An insertion adds a
           ;; sibling, so it has no one-form-to-one-form spelling until the
           ;; caller widens both to the enclosing form, which needs the source
           ;; we deliberately have not read here.
           ["from" "to"])}))))

;; @spec MCP-OP-CLOSE-001
;; @spec MCP-OP-CLOSE-003
;; @spec MCP-OP-CLOSE-009
;; @spec MCP-OP-CLOSE-013
(defn refuse-closed-losers
  "Refuse the measured-loser shapes on the direct `changes` route, or nil.

   Editor gestures never reach here: `edit_clojure`'s `edits`, `require_change`,
   `delete_owners`, `create_files`, `programs`, extraction and every
   `inspect_clojure` mode are the winners and stay open."
  [params]
  (let [changes (field params "changes")]
    (when (sequential? changes)
      (first
        (keep-indexed
          (fn [index change]
            (when (map? change)
              (when-let [action (and (namespace-owner? change)
                                     (some? (field change "find"))
                                     (refused-action change))]
                (let [{:keys [next-call missing]} (redirect change action)]
                  (cond->
                   {:ok false
                    :error-type :whole-file-reprint-refused
                    :error
                    (str "Refusing change " index
                         ": an owner of kind \"namespace\" restages the whole"
                         " file, which reformatted about 425 lines of untouched"
                         " source across five files in the 2026-09-02 cohort."
                         " Express this change on its own owner instead.")
                    :reason :owner-kind-namespace-write
                    :change-index index
                    :change-id (field change "id")
                    :next-call next-call
                    :next-action (if (seq missing)
                                   "fill_next_call_then_call_edit_clojure_once"
                                   "call_edit_clojure_once")
                    :remedy
                    (if (seq missing)
                      (str "Copy next_call, supply " (str/join " and " missing)
                           ", then call edit_clojure once. No source was"
                           " changed.")
                      (str "Call edit_clojure once with the returned next_call."
                           " No source was changed."))
                    :source-unchanged true
                    :mutation-attempted false
                    :write-authority false}
                    (seq missing) (assoc :missing (vec missing)))))))
          changes)))))

;; @spec MCP-OP-CLOSE-002
(defn attach-workspace-root
  "Complete a closed-shape refusal's redirect with the canonical workspace the
   router resolved. Any other result is returned unchanged."
  [result workspace-root]
  (if (and (map? result)
           (map? (:next_call result))
           (string? workspace-root)
           (not (str/blank? workspace-root)))
    (assoc-in result [:next_call :arguments :workspace_root] workspace-root)
    result))
