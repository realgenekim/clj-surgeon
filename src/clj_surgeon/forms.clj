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
                  \"defsetting\"    :def}}"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

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
   "declare"     :declare})

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

(defn- validate-aliases
  "Throw if `m` isn't a string->valid-kind map. Returns `m` on success."
  [m source]
  (when-not (map? m)
    (throw (ex-info (str source ": :aliases must be a map") {:got m})))
  (doseq [[k v] m]
    (when-not (string? k)
      (throw (ex-info (str source ": :aliases key must be a string")
                      {:key k :value v})))
    (when-not (contains? valid-kinds v)
      (throw (ex-info (str source ": :aliases value for '" k
                           "' must be one of " (sort valid-kinds))
                      {:key k :value v :valid valid-kinds}))))
  m)

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

(defn- lookup
  "One-level lookup: core, in-source explicit, project. No / handling."
  [s]
  (or (core-forms s)
      (explicit-aliases s)
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
    (or (lookup type-str)
        (when-let [idx (str/index-of type-str "/")]
          (lookup (subs type-str (inc idx)))))))

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
