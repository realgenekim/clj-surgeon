(ns clj-surgeon.runner-membership
  "TEST-ISO-001 -- RESOLVING A NAMED RUNNER TO THE NAMESPACES IT ACTUALLY RUNS.

   THE DEFECT THIS EXISTS FOR, verbatim from the round-three landing review
   (`docs/observations/suite-spike-round3-review-sol.md`, finding 4):

     `Any new orphan can therefore be put in `excluded` with the false reason
      \"`make test-fast`\" and pass this witness because that unrelated target
      exists. The implementation comment claims \"the OTHER runner that runs
      it\"; no membership check exists. The authorized-window archive-copy
      sabotage did exactly this and passed: {:test 1, :pass 5, :fail 0,
      :error 0}.`

   The old predicate asked `does a target with this name exist?`. That is a
   SPELLING check over the Makefile, and the reviewer's sabotage is the proof
   that a spelling is not a membership: `make test-fast` exists, and it does
   not run `clj-surgeon.analyzer-contract-test`, and the exclusion claiming it
   does was accepted. Same class as the marker audit that checks for a marker
   rather than for the behaviour: presence is not proof.

   So this namespace RESOLVES. Given `make <target>` or
   `:clj-surgeon/<alias>` it follows the runner's OWN selection to a concrete
   namespace set:

     an alias        -> deps.edn `:main-opts`
                        -m clj-surgeon.mcp-test-runner <lane>...  -> the lane
                        manifest's namespaces for those lanes; any other
                        `-m <ns>` -> that runner source file's own
                        `clj-surgeon.*-test` spellings.
     a make target   -> its prerequisites AND its recipe lines:
                        `-M:clj-surgeon/<alias>` -> the alias;
                        `$(MAKE) ... <target>`   -> that target;
                        `bb test/run_all.clj`    -> the babashka lane;
                        any literal `clj-surgeon.*-test` in the recipe.

   IT FAILS CLOSED. A runner whose selection this cannot follow comes back in
   `:unresolved`, and an exclusion pointing at it is REFUSED -- `I could not
   work out what that runs` must never read the same as `it runs your
   namespace`. Pure and dependency-light on purpose: no child process, no
   `clojure -Spath`, and bb can load it, so the witness is reachable from the
   fast lane and from a shell in one second."
  (:require
   [clj-surgeon.lane-manifest :as lm]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def ^:private test-ns-pattern #"clj-surgeon(?:\.[a-z0-9\-]+)+-test")

(defn namespaces-named-in
  "Every `clj-surgeon.*-test` namespace symbol spelled in `text`."
  [text]
  (into #{} (map symbol) (re-seq test-ns-pattern (or text ""))))

(defn- ns->source-file
  [ns-sym]
  (io/file "test" (str (-> (str ns-sym)
                           (str/replace "-" "_")
                           (str/replace "." "/"))
                       ".clj")))

(defn- read-if-present
  [^java.io.File f]
  (when (.isFile f) (slurp f)))

;; ---------------------------------------------------------------------------
;; the Makefile, as data
;; ---------------------------------------------------------------------------

(defn make-target
  "`{:prerequisites [..] :recipe \"...\"}` for `target` in `makefile-text`, or
   nil when no rule declares it. Only a real rule line counts -- a target named
   inside another recipe or in `.PHONY` is a mention, not a definition, and it
   was exactly that looseness (`(str/includes? makefile (str \" \" t \" \"))`)
   that let the old predicate accept anything appearing anywhere."
  [makefile-text target]
  (let [lines (str/split-lines (or makefile-text ""))
        head (re-pattern (str "^" (java.util.regex.Pattern/quote target) ":(?!=)(.*)$"))]
    (loop [[line & more] lines]
      (when line
        (if-let [[_ prereqs] (re-find head line)]
          {:prerequisites (vec (remove str/blank? (str/split (str/trim (or prereqs "")) #"\s+")))
           :recipe (str/join "\n" (take-while #(str/starts-with? % "\t") more))}
          (recur more))))))

;; ---------------------------------------------------------------------------
;; deps.edn, as data
;; ---------------------------------------------------------------------------

(defn alias-main-opts
  "The `:main-opts` of `:clj-surgeon/<alias>` in `deps-text`, or nil."
  [deps-text alias-name]
  (some-> (edn/read-string (or deps-text "{}"))
          :aliases
          (get (keyword "clj-surgeon" alias-name))
          :main-opts
          vec))

;; ---------------------------------------------------------------------------
;; the resolver
;; ---------------------------------------------------------------------------

(declare resolve-runner)

(defn- bb-lane-namespaces
  []
  (namespaces-named-in (read-if-present (io/file "test" "run_all.clj"))))

(defn- resolve-main-opts
  [main-opts ctx]
  (let [opts (vec main-opts)
        i (.indexOf opts "-m")]
    (cond
      (neg? i)
      {:namespaces #{} :unresolved [(str "main-opts name no -m entry point: " (pr-str opts))]}

      (= "clj-surgeon.mcp-test-runner" (nth opts (inc i) nil))
      ;; The lane runner selects by LANE NAME, and the manifest is the
      ;; authority for what a lane contains -- so this follows the runner's
      ;; own selection rather than restating it.
      (let [lane-args (subvec opts (+ i 2))
            lanes (mapv (comp keyword str/lower-case) lane-args)
            unknown (remove (set lm/lanes) lanes)]
        (if (seq unknown)
          {:namespaces #{} :unresolved [(str "lane runner named unknown lane(s) " (pr-str unknown))]}
          {:namespaces (into #{} (mapcat lm/namespaces-for) lanes) :unresolved []}))

      :else
      (let [runner-ns (nth opts (inc i) nil)
            src (read-if-present (ns->source-file (symbol runner-ns)))]
        (if src
          {:namespaces (namespaces-named-in src) :unresolved []}
          {:namespaces #{}
           :unresolved [(str "runner namespace " runner-ns
                             " has no source file under test/, so what it runs "
                             "cannot be read")]})))))

(defn- resolve-alias
  [alias-name {:keys [deps-text] :as ctx}]
  (if-let [opts (alias-main-opts deps-text alias-name)]
    (resolve-main-opts opts ctx)
    {:namespaces #{}
     :unresolved [(str ":clj-surgeon/" alias-name
                       " is not an alias in deps.edn with :main-opts")]}))

(defn- resolve-target
  [target {:keys [makefile-text] :as ctx} seen]
  (if-let [{:keys [prerequisites recipe]} (make-target makefile-text target)]
    (let [aliases (map second (re-seq #"-M:(?:[a-zA-Z0-9._/\-]*:)*clj-surgeon/([a-z0-9\-]+)" recipe))
          sub-targets (concat prerequisites
                              (map second (re-seq #"\$\(MAKE\)(?:\s+--[a-z\-]+)*\s+([a-z0-9\-]+)" recipe)))
          bb-lane? (str/includes? recipe "bb test/run_all.clj")
          parts (concat (map #(resolve-alias % ctx) aliases)
                        (map #(resolve-runner (str "make " %) ctx seen) sub-targets)
                        (when bb-lane? [{:namespaces (bb-lane-namespaces) :unresolved []}])
                        [{:namespaces (namespaces-named-in recipe) :unresolved []}])
          namespaces (into #{} (mapcat :namespaces) parts)]
      {:namespaces namespaces
       ;; A recipe that yields nothing readable is unresolved; a recipe that
       ;; yields SOMETHING is taken at what it yields. The sub-parts' own
       ;; unresolved notes are carried only when the whole target came back
       ;; empty, so a target that also runs a benchmark script does not report
       ;; the script as a failure to resolve tests.
       :unresolved (if (seq namespaces)
                     []
                     (into [(str "`make " target "` resolves to no test namespace")]
                           (mapcat :unresolved parts)))})
    {:namespaces #{}
     :unresolved [(str "no rule in the Makefile defines the target `" target "`")]}))

(defn resolve-runner
  "The namespaces `runner` actually runs, as
   `{:namespaces #{..} :unresolved [..]}`. `runner` is either
   `\"make <target>\"` or `\":clj-surgeon/<alias>\"`.

   `ctx` supplies `:makefile-text` and `:deps-text` so the witness can drive
   this over a sabotaged copy without touching the working tree."
  ([runner ctx] (resolve-runner runner ctx #{}))
  ([runner ctx seen]
   (cond
     (contains? seen runner)
     {:namespaces #{} :unresolved [(str "cycle at " runner)]}

     (str/starts-with? runner "make ")
     (resolve-target (str/trim (subs runner 5)) ctx (conj seen runner))

     (str/starts-with? runner ":clj-surgeon/")
     (resolve-alias (subs runner (count ":clj-surgeon/")) ctx)

     :else
     {:namespaces #{} :unresolved [(str "not a runner reference: " (pr-str runner))]})))

(defn runners-named-in
  "The runner references a reason string names, in the order it names them."
  [reason]
  (concat (map #(str "make " (second %)) (re-seq #"`make ([a-z0-9\-]+)`" reason))
          (map #(str ":clj-surgeon/" (second %)) (re-seq #":clj-surgeon/([a-z0-9\-]+)" reason))))

(defn exclusion-violations
  "For every entry of `excluded`, the reason it does NOT prove membership --
   empty when every exclusion redirects to a runner that really runs it.

   Three refusal kinds, each naming its subject and its evidence:
     :no-runner-named   the reason names no `make <t>` and no :clj-surgeon/<a>
     :unresolved-runner every runner it names exists but what it runs could
                        not be read -- FAIL CLOSED, never assume membership
     :not-a-member      the runner resolves, and this namespace is not in it"
  [excluded ctx]
  (vec
   (for [[s reason] (sort-by key excluded)
         :let [runners (vec (runners-named-in reason))
               resolutions (mapv #(vector % (resolve-runner % ctx)) runners)
               member (some (fn [[r {:keys [namespaces]}]]
                              (when (contains? namespaces s) r))
                            resolutions)]
         :when (not member)
         :let [unresolved (mapcat (comp :unresolved second) resolutions)]]
     (cond
       (empty? runners)
       {:namespace s :kind :no-runner-named :reason reason
        :message (str "excluded namespace " s " names no runner at all. An "
                      "exclusion is a REDIRECTION: its reason must name a "
                      "`make <target>` or a :clj-surgeon/<alias> that RUNS it "
                      "(TEST-ISO-001). Its reason was: " (pr-str reason))}

       (seq unresolved)
       {:namespace s :kind :unresolved-runner :reason reason
        :message (str "excluded namespace " s " names runner(s) "
                      (str/join ", " runners) " whose selection could not be "
                      "resolved, so membership is UNPROVEN and this fails "
                      "closed: " (str/join "; " unresolved))}

       :else
       {:namespace s :kind :not-a-member :reason reason
        :message (str "excluded namespace " s " names runner(s) "
                      (str/join ", " runners)
                      " -- and none of them RUNS it. `" (first runners)
                      "` runs "
                      (let [ns-set (:namespaces (second (first resolutions)))]
                        (str (count ns-set) " namespace(s), not including this one"))
                      ". A target that merely EXISTS is a spelling, not a "
                      "runner: this is the round-three review's finding 4 "
                      "sabotage. Point the exclusion at the runner that "
                      "really runs it, adopt the namespace into a lane, or "
                      "delete it.")}))))

(defn repo-context
  "The live tree's Makefile and deps.edn."
  []
  {:makefile-text (slurp (io/file "Makefile"))
   :deps-text (slurp (io/file "deps.edn"))})
