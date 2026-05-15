(ns clj-surgeon.forms
  "Single source of truth for defining-form classification + per-form field
   extraction.

   Classification: a form's macro symbol is mapped to a *kind* keyword
   (:defn, :def, :defn-, etc.) so downstream ops (:deps, :topo, :ls-extract,
   :fix-declares) know what counts as a definition and what is private.

   Field extraction: for forms recognized via `.clj-surgeon.edn`, the user
   declares `:fields` — a map of field-name → extractor function. Each
   extractor takes a zloc (pointing at the form list) and returns the value
   to emit in `:ls` output. Functions are real Clojure forms in the EDN
   file, evaluated in an SCI sandbox at config-load time. clj-surgeon ships
   a stdlib of named extractors (`->defn-name`, `->defn-arg-list`,
   `->first-keyword`, …) in `clj-surgeon.fields/public`.

   Example `.clj-surgeon.edn`:

       {:aliases
        {\"defenterprise\"
         {:fields {:name         ->defn-name
                   :docstring    ->defn-docstring
                   :ee-namespace (fn [z]
                                   ;; symbol immediately after the optional docstring
                                   (let [c (-> z z/down z/right z/right)]
                                     (z/sexpr (if (->defn-docstring z) (z/right c) c))))
                   :arglist      ->defn-arg-list}}

         \"defendpoint\"
         {:fields {:route (fn [z]
                            [(-> z z/down z/right z/sexpr)
                             (-> z z/down z/right z/right z/sexpr)])}}

         \"defsetting\"
         {:fields {:name ->defn-name}}}}

   Resolution: walk up from each source file looking for `.clj-surgeon.edn`.
   Closest config wins. No config = pure core-forms classification (defn,
   def, etc. only)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-surgeon.fields :as fields]
            [rewrite-clj.node :as n]
            [rewrite-clj.zip :as z]
            [sci.core :as sci]))

;; ============================================================
;; Core defining forms — these map to themselves
;; ============================================================

(def core-forms
  "Standard Clojure defining forms, keyed by their string name."
  {"def"         :def
   "defn"        :defn
   "defn-"       :defn-
   "defonce"     :defonce
   "defmacro"    :defmacro
   "defmethod"   :defmethod
   "defmulti"    :defmulti
   "defprotocol" :defprotocol
   "defrecord"   :defrecord
   "deftype"     :deftype
   "declare"     :declare
   "deftest"     :def})

;; ============================================================
;; Explicit aliases — ecosystem macros with non-standard names
;; ============================================================

(def explicit-aliases
  "Custom defining forms whose local name doesn't match a core form.
   Add ecosystem macros here (one line each)."
  {">defn"  :defn
   ">defn-" :defn-})

;; ============================================================
;; Project-local config — from .clj-surgeon.edn
;; ============================================================

(def project-aliases
  "Singleton: map of macro-name-string -> spec map. Each spec is
   `{:kind <kw> :fields {field-key compiled-fn}}`. Populated by
   `init-from-file!`. Defaults empty."
  (atom {}))

(defn- private-from-name?
  "Privacy convention: a macro name ending in `-` is private. Matches
   `defn-`, `>defn-`, `mu/defn-`."
  [type-str]
  (.endsWith ^String type-str "-"))

(defn- infer-kind
  "If user didn't declare :kind, infer from name suffix.
   Trailing `-` → :defn-. Anything else → :defn."
  [type-str]
  (if (private-from-name? type-str)
    :defn-
    :defn))

;; ============================================================
;; SCI sandbox — evaluate user :fields fn-forms
;; ============================================================

(def ^:private zip-bindings
  "Subset of rewrite-clj.zip / rewrite-clj.node functions we expose to SCI
   via the `z` and `n` namespace aliases. Add more if extractors need them."
  {'rewrite-clj.zip  (into {} (for [s '[down up right left rightmost leftmost
                                        node string sexpr next prev of-string]]
                                [s (deref (resolve (symbol "rewrite-clj.zip" (str s))))]))
   'rewrite-clj.node (into {} (for [s '[tag children]]
                                [s (deref (resolve (symbol "rewrite-clj.node" (str s))))]))})

(defn- sci-opts
  "SCI context that exposes rewrite-clj.zip + rewrite-clj.node via the
   `z` and `n` aliases, and the clj-surgeon stdlib (->defn-name etc.) as
   bare-symbol bindings. Code in `.clj-surgeon.edn` gets these for free."
  []
  {:namespaces zip-bindings
   :aliases    {'z 'rewrite-clj.zip
                'n 'rewrite-clj.node}
   :bindings   (into {} (for [[s f] fields/public] [s f]))})

(defn- compile-field
  "Take an extractor form from `.clj-surgeon.edn` and turn it into a real
   fn. If it's already a fn, return it. If it's a symbol, resolve in stdlib.
   Otherwise eval as SCI code."
  [form macro-name field-key]
  (cond
    (fn? form) form

    (symbol? form)
    (or (get fields/public form)
        (throw (ex-info (str ".clj-surgeon.edn: " macro-name " :fields "
                             field-key " — unknown extractor symbol: " form
                             "\n  Available: " (sort (keys fields/public)))
                        {:macro macro-name :field field-key :symbol form})))

    (and (seq? form) (= 'fn (first form)))
    (try (sci/eval-form (sci/init (sci-opts)) form)
         (catch Exception e
           (throw (ex-info (str ".clj-surgeon.edn: " macro-name " :fields "
                                field-key " — eval failed: " (.getMessage e))
                           {:macro macro-name :field field-key :form form} e))))

    :else
    (throw (ex-info (str ".clj-surgeon.edn: " macro-name " :fields "
                         field-key " — value must be a stdlib symbol or a fn form, got: "
                         (pr-str form))
                    {:macro macro-name :field field-key :form form}))))

(defn- compile-spec
  "Turn a raw alias spec from `.clj-surgeon.edn` into the runtime form
   used by classify + outline. Returns {:kind kw :fields {k compiled-fn}}."
  [macro-name raw-value]
  (let [base (cond
               (keyword? raw-value) {:kind raw-value}
               (map? raw-value)     raw-value
               :else
               (throw (ex-info (str ".clj-surgeon.edn: " macro-name
                                    " — value must be a kind keyword or a spec map")
                               {:macro macro-name :value raw-value})))
        kind (or (:kind base) (infer-kind macro-name))
        fields (when-let [fs (:fields base)]
                 (when-not (map? fs)
                   (throw (ex-info (str ".clj-surgeon.edn: " macro-name
                                        " — :fields must be a map")
                                   {:macro macro-name :fields fs})))
                 (into {} (for [[fk fv] fs]
                            [fk (compile-field fv macro-name fk)])))]
    (cond-> {:kind kind}
      fields (assoc :fields fields))))

;; ============================================================
;; Config file resolution
;; ============================================================

(defn- find-config-file
  "Walk up from `start` (a file or dir path) looking for `.clj-surgeon.edn`.
   Return the java.io.File for the first one found, or nil."
  [start]
  (let [start-file (some-> start io/file .getAbsoluteFile)
        start-dir (if (and start-file (.isDirectory start-file))
                    start-file
                    (some-> start-file .getParentFile))]
    (loop [dir start-dir]
      (when dir
        (let [cfg (io/file dir ".clj-surgeon.edn")]
          (if (.exists cfg)
            cfg
            (recur (.getParentFile dir))))))))

(defn- read-config
  "Read + compile a `.clj-surgeon.edn` file. Throws on malformed EDN or
   invalid extractor forms so the user sees the problem at load time
   instead of silent misclassification at runtime."
  [f]
  (let [parsed (try (edn/read-string (slurp f))
                    (catch Exception e
                      (throw (ex-info (str (.getPath f) ": invalid EDN — "
                                           (.getMessage e))
                                      {:file (.getPath f)} e))))
        aliases (:aliases parsed {})]
    (when-not (map? aliases)
      (throw (ex-info (str (.getPath f) ": :aliases must be a map")
                      {:file (.getPath f) :got aliases})))
    (into {} (for [[k v] aliases]
               (do (when-not (string? k)
                     (throw (ex-info (str (.getPath f)
                                          ": :aliases keys must be strings; got "
                                          (pr-str k))
                                     {:file (.getPath f) :key k})))
                   [k (compile-spec k v)])))))

(defn load-project-aliases
  "Find `.clj-surgeon.edn` by walking up from `start` (a file or dir path).
   Return the compiled aliases map, or `{}` if no config found."
  [start]
  (if-let [f (find-config-file start)]
    (read-config f)
    {}))

(defn init-from-file!
  "Resolve `.clj-surgeon.edn` near `file-path`, populate `project-aliases`.
   Call once per CLI invocation, from the top-level dispatch."
  [file-path]
  (reset! project-aliases (load-project-aliases file-path)))

;; ============================================================
;; Classification: the one function everything else calls
;; ============================================================

(defn- lookup-kind
  "Single-level lookup returning kind kw. Tiers: core forms, in-source
   explicit aliases, project aliases. Project entries store the kind in
   `:kind` of their spec map."
  [s]
  (or (core-forms s)
      (explicit-aliases s)
      (:kind (@project-aliases s))))

(defn- lookup-spec
  "Look up the full spec map. Bare kinds (core, explicit) return
   `{:kind kw}` with no fields. Project entries return their full compiled
   spec including `:fields`."
  [s]
  (or (when-let [k (core-forms s)] {:kind k})
      (when-let [k (explicit-aliases s)] {:kind k})
      (@project-aliases s)))

(defn classify
  "Classify a form's type-str into a canonical kind, or nil if not a
   defining form.

   Resolution at each step: core-forms → in-source explicit-aliases →
   project-aliases. If the full type-str doesn't match anywhere, try the
   local part after `/` (so `mu/defn` and `api.macros/defendpoint` resolve
   via their unqualified names)."
  [type-str]
  (when type-str
    (or (lookup-kind type-str)
        (when-let [idx (str/index-of type-str "/")]
          (lookup-kind (subs type-str (inc idx)))))))

(defn spec
  "Return the full spec map for a type-str — `{:kind <kw> :fields <map>}`
   or nil if not a defining form. Same tier resolution as `classify`."
  [type-str]
  (when type-str
    (or (lookup-spec type-str)
        (when-let [idx (str/index-of type-str "/")]
          (lookup-spec (subs type-str (inc idx)))))))

;; ============================================================
;; Derived predicates — used across outline, analyze, extract
;; ============================================================

(defn defining-form?
  "Is this type-str a recognized defining form?"
  [type-str]
  (some? (classify type-str)))

(defn private-form?
  "Is this a private defining form (defn-, >defn-, mu/defn-, etc.)?"
  [type-str]
  (= :defn- (classify type-str)))

(defn has-arglists?
  "Does this form type have an arglist vector? Used only by the legacy
   `extract-arglist` path — when a project alias provides custom `:fields`,
   the user controls arglist extraction directly."
  [type-str]
  (contains? #{:defn :defn-} (classify type-str)))
