(ns clj-surgeon.forms
  "Single source of truth for defining-form classification.

   Every operation — :ls, :deps, :mv, :extract, :fix-declares, :topo —
   uses these predicates to decide what counts as a definition, what has
   arglists, and what is private.

   To add support for a custom macro (e.g. mu/defn, defendpoint):
   - If the local name after / matches a core form, it works automatically.
     (mu/defn -> 'defn' -> :defn)
   - For ecosystem macros with non-standard names (>defn etc.), add an
     entry to `explicit-aliases` in this file.
   - For project-specific macros (defendpoint, defenterprise, defsetting),
     create a `.clj-surgeon.edn` at the repo root:

       {:aliases {\"defendpoint\"   :defn
                  \"defenterprise\" :defn
                  \"defsetting\"    :def}}

   Rich form: an alias value may be a map `{:kind <kw> :fields {...}}`
   where `:fields` declares custom field extraction (see selectors.clj
   and docs/field-extraction-dsl.md):

       {:aliases
        {\"api.macros/defendpoint\"
         {:kind :defn
          :fields {:method  [:find-first :keyword]
                   :path    [:right-of :method]
                   :name    [:join \" \" :method :path]
                   :arglist [:find-first :vector]}}}}"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-surgeon.selectors :as sel]))

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
;; Explicit aliases — non-standard names that can't be auto-detected
;; from the local part after /
;; ============================================================

(def explicit-aliases
  "Custom defining forms whose local name doesn't match a core form.
   Add project-specific macros here (one line each)."
  {">defn"  :defn
   ">defn-" :defn-})

;; ============================================================
;; Project-local aliases — from .clj-surgeon.edn at the repo root
;; ============================================================

(def ^:private valid-kinds
  "Kinds an alias may map to. Same set as core-forms values."
  #{:def :defn :defn- :defonce :defmacro :defmethod :defmulti
    :defprotocol :defrecord :deftype :declare})

(defn- find-config-file
  "Walk up from `start` (a path string — file or dir) looking for a
   `.clj-surgeon.edn`. Return the java.io.File for the config, or nil."
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

(defn- validate-alias-value
  "Validate a single alias value. May be a bare kind keyword or a rich
   spec map {:kind <kw> :fields {...}}. Returns a normalized spec map
   {:kind <kw> :fields <map-or-nil>} on success; throws otherwise."
  [k v source]
  (cond
    ;; bare kind keyword: shorthand for {:kind v}
    (keyword? v)
    (do (when-not (contains? valid-kinds v)
          (throw (ex-info (str source ": :aliases value for '" k
                               "' must be one of " (sort valid-kinds))
                          {:key k :value v :valid valid-kinds})))
        {:kind v})

    ;; rich spec map
    (map? v)
    (let [kind (:kind v)]
      (when-not (contains? valid-kinds kind)
        (throw (ex-info (str source ": :aliases value for '" k
                             "' must have :kind in " (sort valid-kinds))
                        {:key k :value v :valid valid-kinds})))
      (when-let [fields (:fields v)]
        (when-not (map? fields)
          (throw (ex-info (str source ": :fields for '" k "' must be a map")
                          {:key k :fields fields})))
        (try (sel/validate-spec fields)
             (catch Exception e
               (throw (ex-info (str source ": invalid :fields for '" k
                                    "' — " (.getMessage e))
                               {:key k :fields fields} e)))))
      v)

    :else
    (throw (ex-info (str source ": :aliases value for '" k
                         "' must be a keyword or a spec map")
                    {:key k :value v}))))

(defn- validate-aliases
  "Validate the full aliases map. Returns a normalized map where every
   value is a spec map {:kind <kw> :fields <map-or-nil>}."
  [m source]
  (when-not (map? m)
    (throw (ex-info (str source ": :aliases must be a map") {:got m})))
  (into {}
        (for [[k v] m]
          (do (when-not (string? k)
                (throw (ex-info (str source ": :aliases key must be a string")
                                {:key k :value v})))
              [k (validate-alias-value k v source)]))))

(defn- read-config
  "Read + validate aliases from a `.clj-surgeon.edn` file. Throws on
   malformed EDN or invalid aliases so the user sees the problem instead
   of silent misclassification."
  [f]
  (let [parsed (try (edn/read-string (slurp f))
                    (catch Exception e
                      (throw (ex-info (str (.getPath f) ": invalid EDN — "
                                           (.getMessage e))
                                      {:file (.getPath f)} e))))]
    (validate-aliases (:aliases parsed {}) (.getPath f))))

(defn load-project-aliases
  "Find `.clj-surgeon.edn` by walking up from `start` (a file or dir path).
   Return the parsed/validated `:aliases` map, or `{}` if no config found."
  [start]
  (if-let [f (find-config-file start)]
    (read-config f)
    {}))

(def project-aliases
  "Singleton aliases map, populated by `init-from-file!`. Defaults empty —
   any caller that hasn't initialized gets no project-local aliases."
  (atom {}))

(defn init-from-file!
  "Resolve `.clj-surgeon.edn` near `file-path`, populate `project-aliases`.
   Call once per CLI invocation, from each top-level op entry point."
  [file-path]
  (reset! project-aliases (load-project-aliases file-path)))

;; ============================================================
;; Classification: the one function everything else calls
;; ============================================================

(defn- lookup-kind
  "One-level lookup returning kind kw. Tiers: core, in-source explicit,
   project. Project entries are spec maps, so we read :kind out."
  [s]
  (or (core-forms s)
      (explicit-aliases s)
      (:kind (@project-aliases s))))

(defn- lookup-spec
  "Look up the full spec map (with :kind + optional :fields). Bare kinds
   from core-forms/explicit-aliases return {:kind <kw>} with no :fields."
  [s]
  (or (when-let [k (core-forms s)] {:kind k})
      (when-let [k (explicit-aliases s)] {:kind k})
      (@project-aliases s)))

(defn classify
  "Classify a form's type-str into a canonical kind (:defn, :defn-, :def, etc.)
   or nil if it's not a defining form.

   Resolution order at each step:
     core-forms -> in-source explicit-aliases -> project-aliases (.clj-surgeon.edn)

   1. Try the full type-str (handles 'defn', 'defendpoint', '>defn').
   2. If type-str contains '/', try the local part after the slash
      (handles 'mu/defn' -> 'defn' -> :defn, and
      'api.macros/defendpoint' -> 'defendpoint' -> :defn when config has it)."
  [type-str]
  (when type-str
    (or (lookup-kind type-str)
        (when-let [idx (str/index-of type-str "/")]
          (lookup-kind (subs type-str (inc idx)))))))

(defn spec
  "Return the full spec map for a type-str — {:kind <kw> :fields <map-or-nil>}
   or nil if not a defining form. Same resolution as classify."
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
  "Does this form type have an arglist vector? (defn, defn-, >defn, mu/defn, etc.)"
  [type-str]
  (#{:defn :defn-} (classify type-str)))
