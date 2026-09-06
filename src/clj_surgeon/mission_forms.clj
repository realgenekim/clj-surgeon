(ns clj-surgeon.mission-forms
  "Pure owner-keyed candidate lowering. Never evaluates candidate forms or writes files."
  (:require
   [clj-surgeon.mission-candidate :as candidate]
   [clj-surgeon.mission-forms-source :as source]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(defn refusal
  ([reason] (refusal reason {}))
  ([reason data] (merge data {:ok false :error-type reason :mutation-attempted false})))

(def trivia #{:whitespace :newline :comma})

;; Comments are no longer protected syntax. They were protected because a model
;; asked to re-emit a definition drops them and the splice would delete them
;; SILENTLY; `compile-forms` now makes that loss loud (`:forms-comment-lost`,
;; and `:forms-comment-moved` when the text survives against a different
;; EXPRESSION -- compared by that expression's identity, never its ordinal)
;; instead of trading the whole class away. `^meta`, `#_` and `#=`
;; are untouched -- `#_{:clj-kondo/ignore [...]}` is an :uneval node and stays
;; protected syntax; a `;; clj-kondo` directive COMMENT follows the attachment rule.
(def protected #{:meta :uneval :reader-macro :eval})

(defn definition [source]
  (when (> (count (take 2049 (filter #{\( \[ \{} source))) 2048)
    (throw (ex-info "Bound exceeded" {:error-type :candidate-parser-budget})))
  (let [tree (parser/parse-string-all source)
        nodes (tree-seq node/inner? node/children tree)
        top (remove #(contains? source/ignorable (node/tag %)) (node/children tree))
        form (first top)
        children (when (= :list (node/tag form))
                   (remove #(contains? trivia (node/tag %)) (node/children form)))
        [head name] (map node/string (take 2 children))
        tail (drop 2 children)
        doc? (and (some-> tail first node/string (.startsWith "\""))
                  (or (not= "def" head) (> (count tail) 1)))
        args (if doc? (rest tail) tail)
        attr? (and (contains? #{"defn" "defn-"} head)
                   (or (= :map (some-> args first node/tag))
                       (and (= :list (some-> args first node/tag))
                            (= :map (some-> args last node/tag)))))]
    (when (some #(contains? protected (node/tag %)) nodes)
      (throw (ex-info "Protected syntax" {:error-type :forms-protected-syntax})))
    (when attr?
      (throw (ex-info "Attribute metadata requires preservation" {:error-type :forms-protected-syntax})))
    (when-not (and (= 1 (count top)) (= :list (node/tag form))
                   (contains? #{"def" "defn" "defn-"} head)
                   (= :token (some-> children second node/tag)))
      (throw (ex-info "One named definition required" {:error-type :forms-invalid-definition})))
    {:head head :name name :docstring (when doc? (node/string (first tail)))
     :source (node/string form)
     ;; The span's own bytes, trimmed. `:source` is the FORM only, so it drops a
     ;; leading or trailing comment that lives inside the span; `:span-source`
     ;; keeps it. They are identical whenever the span holds no comment.
     :span-source (.trim ^String source)
     :comments (source/comment-nodes source)}))

(defn valid-replacement? [r]
  (and (map? r) (= #{:file :owner :form} (set (keys r)))
       (every? string? ((juxt :file :owner :form) r))
       (<= 1 (count (:form r)) (:file-chars candidate/limits))))

(defn reconcile-comments
  "Decide the exact replacement bytes for one owner, comments included.

   The replacement is kept as SOURCE TEXT throughout -- `:span-source`, not a
   re-printed reader value -- so a comment the model DID emit is spliced
   verbatim. Preservation is judged on TEXT AND THE ATTACHED EXPRESSION'S
   IDENTITY: a comment must come back with the same text against the same
   expression, on the same side, at the same depth-path. Its ordinal is NOT the
   test -- swapping two body expressions leaves every ordinal intact and every
   guard wrong. Dropped is `:forms-comment-lost`; re-attached to a different
   expression is `:forms-comment-moved`, carrying the `:from` and `:to`
   expression texts. Inserting a new expression before a commented one changes
   no identity and is accepted.

   Nothing is ever placed by position on the model's behalf -- a machine
   guessing where a `;; clj-kondo/ignore` or a `;; why` note belongs corrupts
   its meaning more quietly than dropping it would. `opts` may carry
   `:comment-follows-rewrite true`, the caller's explicit statement that this
   mission rewrites the guarded expression and the comment should follow the new
   one at the same position; it accepts an identity change only when side,
   depth-path and ordinal are unchanged, and it re-admits the ordinal rule for
   those comments, so a swap is accepted under it. It is opt-in for that reason
   and is never entered as a fallback."
  ([owner-span replacement-span original replacement]
   (reconcile-comments owner-span replacement-span original replacement nil))
  ([owner-span _replacement-span original replacement opts]
   (let [text (source/align-replacement (source/owner-comment-positions owner-span)
                                        (:span-source replacement)
                                        (:source replacement))]
     (if (and (empty? (:comments original)) (empty? (source/comment-nodes text)))
       text
       ;; Preservation is checked against the bytes that will ACTUALLY be spliced,
       ;; never against the raw candidate text. That is what makes duplication and
       ;; loss both impossible rather than merely unlikely.
       (let [{:keys [preserved lost moved]} (source/comment-preservation owner-span text opts)]
         (cond
           preserved text

           (seq moved)
           (throw (ex-info "Owner comment re-attached to a different expression"
                           (cond-> {:error-type :forms-comment-moved :moved moved
                                    :next_call (str "Re-emit the form with each comment against the "
                                                    "same expression it guards; :from and :to give the "
                                                    "expression each comment was and is attached to. "
                                                    "If this mission intends to rewrite the guarded "
                                                    "expression, re-emit the comment on the rewritten "
                                                    "expression, or set :comment-follows-rewrite true "
                                                    "on the basis to accept an identity change at the "
                                                    "same position.")}
                             (seq lost) (assoc :lost lost))))

           :else
           (throw (ex-info "Owner comments dropped by replacement"
                           {:error-type :forms-comment-lost :lost lost
                            :next_call (str "Re-emit the form with its comments verbatim and "
                                            "against the same expressions.")}))))))))

(defn compile-forms
  "Replace only frozen owners. New names are planner-owned :new-owner values.
   Returns staged source; formatting and independent acceptance remain mandatory
   before commit. Model output contains no old-context text or source offsets."
  [basis replacements]
  (cond
    (not (candidate/valid-basis? basis)) (refusal :candidate-invalid-basis)
    (not (and (vector? replacements) (seq replacements)
              (<= (count replacements) (:changes candidate/limits))
              (every? valid-replacement? replacements))) (refusal :forms-invalid-replacements)
    :else
    (try
      (let [owner-key (juxt :file :owner)
            groups (group-by owner-key (:owners basis))
            keys (mapv owner-key replacements)]
        (when-not (= (count keys) (count (set keys)))
          (throw (ex-info "Duplicate replacement" {:error-type :forms-duplicate-owner})))
        (let [changes
              (mapv
                (fn [r]
                  (let [owners (get groups (owner-key r))]
                    (when-not (= 1 (count owners))
                      (throw (ex-info "Unknown or ambiguous owner" {:error-type :forms-unknown-owner})))
                    (let [{:keys [file owner new-owner start end]} (first owners)
                          before (subs (get-in basis [:sources file]) start end)
                          original (definition before)
                          replacement (definition (:form r))]
                      (when-not (and (= owner (:name original))
                                     (= (or new-owner owner) (:name replacement))
                                     (= (:head original) (:head replacement))
                                     (= (:docstring original) (:docstring replacement)))
                        (throw (ex-info "Definition identity mismatch" {:error-type :forms-owner-mismatch})))
                      {:file file :before before
                       :after (reconcile-comments before (:form r) original replacement
                                                 (select-keys basis [:comment-follows-rewrite]))})))
                replacements)]
          (candidate/compile-candidate basis changes)))
      (catch StackOverflowError _ (refusal :candidate-parser-depth))
      (catch Exception e
        (refusal (or (:error-type (ex-data e)) :candidate-unparseable)
                 (dissoc (ex-data e) :error-type))))))
