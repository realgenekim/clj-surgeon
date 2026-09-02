(ns clj-surgeon.forms
  "Single source of truth for defining-form classification + per-form field
   extraction.

   Classification: a form's macro symbol is mapped to a *kind* keyword
   (:defn, :def, :defn-, etc.) so downstream ops (:deps, :topo, :ls-extract,
   :fix-declares) know what counts as a definition and what is private.

   Field extraction: for forms recognized via `.clj-surgeon.edn`, the user
   declares `:fields` — a map of field-name → extractor function. Each
   extractor takes a zloc (pointing at the form list) and returns the value
   to emit in `:ls` output.

   Resolution order (4 tiers):
   1. Core forms — `defn`, `def`, `defmacro`, etc.
   2. Explicit aliases — `>defn`, `>defn-` (ecosystem macros)
   3. Project aliases — from `.clj-surgeon.edn`
   4. Namespace-qualified split-on-/ — `mu/defn` → `defn` → tier 1-3"
  (:require
   [clj-surgeon.fields :as fields]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
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
   "deftest"     :deftest})

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

(defn- infer-kind
  "If user didn't declare :kind, infer from name suffix.
   Trailing `-` → :defn-. Anything else → :defn."
  [type-str]
  (if (.endsWith ^String type-str "-")
    :defn-
    :defn))

;; ============================================================
;; SCI sandbox — evaluate user :fields fn-forms
;;
;; IMPORTANT: babashka itself runs on SCI. When our code calls
;; sci/eval-form to evaluate a user's (fn [z] ...) extractor,
;; we're creating SCI-inside-SCI — a nested interpreter. This
;; works fine from the CLI (one level of nesting), but the test
;; suite adds another level (bb runs run_all.clj via SCI, which
;; loads our code via SCI, which calls sci/eval-form).
;;
;; At three levels deep, SCI's namespace alias resolution breaks.
;; Specifically: {:namespaces {'rewrite-clj.zip {...}}
;;               :aliases    {'z 'rewrite-clj.zip}}
;; ...causes "Unable to resolve symbol: z/sexpr" in the test suite
;; even though it works perfectly from the CLI.
;;
;; The fix: use bare symbol namespace keys ('z, 'n, 'str) directly
;; instead of full names + aliases. Do NOT "clean up" these keys
;; to proper namespace names — it will break the test suite.
;;
;; See: test/clj_surgeon/edn_config_integration_test.clj
;;   - In-process tests exercise the nested SCI path
;;   - Subprocess tests (test-cli-subprocess-*) shell out to a
;;     fresh `bb` process, avoiding nesting entirely — these test
;;     what the user actually sees and are immune to SCI nesting
;;     issues by design.
;; See also: https://github.com/realgenekim/clj-surgeon/issues/14
;; ============================================================

(defn- sci-opts
  "SCI context for evaluating user extractor fns from .clj-surgeon.edn.
   Uses bare symbol namespace keys ('z, 'n, 'str) — this is the only
   approach that works reliably inside babashka's nested SCI environment.
   See the comment block above for why. Do not change these to full
   namespace names."
  []
  {:namespaces
   {'z   {'down z/down 'up z/up 'right z/right 'left z/left
          'rightmost z/rightmost 'leftmost z/leftmost
          'node z/node 'string z/string 'sexpr z/sexpr
          'next z/next 'prev z/prev 'of-string z/of-string}
    'n   {'tag n/tag 'children n/children}
    'str {'upper-case str/upper-case 'lower-case str/lower-case
          'capitalize str/capitalize 'trim str/trim
          'starts-with? str/starts-with? 'ends-with? str/ends-with?
          'includes? str/includes? 'join str/join
          'split str/split 'split-lines str/split-lines
          'replace str/replace 'blank? str/blank?
          'index-of str/index-of 'last-index-of str/last-index-of
          'triml str/triml 'trimr str/trimr}}
   :bindings (into {} (for [[s f] fields/public] [s f]))})

(defn- compile-field
  "Take an extractor form from `.clj-surgeon.edn` and turn it into a real
   fn. If it's a symbol, resolve in stdlib. If it's a (fn ...) form,
   evaluate in SCI sandbox. Otherwise throw."
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
  "Turn a raw alias spec from `.clj-surgeon.edn` into the runtime form.
   Returns {:kind kw :fields {k compiled-fn}}."
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
   invalid extractor forms."
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

;; @spec MCP-OP-EXTRACT-021
(defn project-config
  "Read the raw `.clj-surgeon.edn` map governing `start`, or nil when there is
  none or it cannot be parsed.

  The alias reader ignores top-level keys it does not know, so a new section is
  additive: a workspace can declare things about itself here without any risk to
  form classification."
  [start]
  (when-let [f (find-config-file start)]
    (try (let [parsed (edn/read-string (slurp f))]
           (when (map? parsed) (assoc parsed :config-file (.getPath f))))
         (catch Exception _ nil))))

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
   explicit aliases, project aliases."
  [s]
  (or (core-forms s)
      (explicit-aliases s)
      (:kind (@project-aliases s))))

(defn- lookup-spec
  "Look up the full spec map. Core/explicit return {:kind kw} with no
   fields. Project entries return their full compiled spec."
  [s]
  (or (when-let [k (core-forms s)] {:kind k})
      (when-let [k (explicit-aliases s)] {:kind k})
      (@project-aliases s)))

(defn classify
  "Classify a form's type-str into a canonical kind (:defn, :defn-, :def, etc.)
   or nil if it's not a defining form.

   Resolution order:
   1. Exact match against core Clojure forms
   2. Exact match against explicit aliases (>defn, >defn-)
   3. Exact match against project aliases from .clj-surgeon.edn
   4. Namespace-qualified: split on / and match local part against tiers 1-3
      (mu/defn -> 'defn' -> :defn, works for any alias)"
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

(defn spec-with-project-aliases
  "Pure form-spec lookup using an explicit project-alias map.

   Built-in and explicit aliases retain precedence. Namespaced macro names
   fall back to their local name, matching `spec`."
  [aliases type-str]
  (letfn [(lookup [s]
            (or (when-let [k (core-forms s)] {:kind k})
                (when-let [k (explicit-aliases s)] {:kind k})
                (get aliases s)))]
    (when type-str
      (or (lookup type-str)
          (when-let [idx (str/index-of type-str "/")]
            (lookup (subs type-str (inc idx))))))))

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
  "Does this form type have an arglist vector? (defn, defn-, >defn, mu/defn, etc.)
   Used by the default extract-arglist path — when a project alias provides
   custom :fields, the user controls arglist extraction directly."
  [type-str]
  (#{:defn :defn-} (classify type-str)))
