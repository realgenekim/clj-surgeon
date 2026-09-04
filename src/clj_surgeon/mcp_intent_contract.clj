(ns clj-surgeon.mcp-intent-contract
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def ^:private spec-pattern
  #"(?m)^- \[([ xD])\] \*\*(MCP-OP-[A-Z0-9-]+)\*\*:")

(def ^:private witness-pattern
  #"@spec\s+(MCP-OP-[A-Z0-9-]+)")

(defn- parse-specs
  [spec-text]
  (into {}
        (map (fn [[_ status intent]]
               [intent (case status
                         " " :active-gap
                         "x" :implemented
                         "D" :deferred)]))
        (re-seq spec-pattern spec-text)))

(defn- source-witnesses
  [sources]
  (into #{}
        (mapcat (fn [[_ text]]
                  (map second (re-seq witness-pattern text))))
        sources))

(defn- unknown-witness-violations
  [known source-kind witnesses]
  (for [intent (sort (remove known witnesses))]
    {:type :unknown-intent-witness
     :intent intent
     :source-kind source-kind}))

;; @spec MCP-OP-TRACE-001
;; @spec MCP-OP-TRACE-002
;; @spec MCP-OP-TRACE-003
;; @spec MCP-OP-TRACE-004
(defn audit-contract
  "Audit one linked-intent leaf from literal spec and witness source texts."
  [{:keys [spec-text implementation-sources test-sources]}]
  (let [specs (parse-specs spec-text)
        known (set (keys specs))
        implementation-witnesses (source-witnesses implementation-sources)
        test-witnesses (source-witnesses test-sources)
        missing
        (mapcat
          (fn [[intent status]]
            (case status
              :active-gap
              (when-not (contains? test-witnesses intent)
                [{:type :missing-test-witness
                  :intent intent
                  :source-kind :test}])

              :implemented
              (cond-> []
                (not (contains? implementation-witnesses intent))
                (conj {:type :missing-implementation-witness
                       :intent intent
                       :source-kind :implementation})

                (not (contains? test-witnesses intent))
                (conj {:type :missing-test-witness
                       :intent intent
                       :source-kind :test}))

              :deferred []))
          (sort-by key specs))
        violations
        (vec
          (concat
            missing
            (unknown-witness-violations
              known :implementation implementation-witnesses)
            (unknown-witness-violations known :test test-witnesses)))]
    {:ok (empty? violations)
     :specs specs
     :implementation-witnesses implementation-witnesses
     :test-witnesses test-witnesses
     :violations violations}))

(defn- source-file?
  [^java.io.File file extensions]
  (and (.isFile file)
       (some #(str/ends-with? (.getName file) %) extensions)))

(defn- read-sources
  [root paths extensions]
  (into {}
        (for [path paths
              :let [base (io/file root path)]
              file (if (.exists base) (file-seq base) [])
              :when (source-file? file extensions)]
          [(.getPath file) (slurp file)])))

(def excluded-spec-docs
  "Repo-relative intent spec documents deliberately kept OUT of the repository
   audit, each mapped to a one-line reason. This map is normally EMPTY: an entry
   is an admission that a discovered leaf does not yet satisfy the contract, so
   it must name a reason and must be removed once the leaf is repaired. An entry
   naming a file that does not exist is an ORPHAN LISTING and fails loudly."
  {"docs/intent/embedded-elaborator/embedded-elaborator-specs.md"
   "frozen-red pre-product leaf (frozen-red-declaration.md, 2026-08-30): 19 MCP-OP-ELAB active-gap specs whose red namespace clj-surgeon.mcp-embedded-elaborator-test is not in this tree, so every one reports missing-test-witness; re-include when the elaborator red surface lands."

   "docs/intent/substantiation-telemetry/substantiation-telemetry-specs.md"
   "frozen-red pre-product leaf (substantiation-telemetry-frozen-red.md, 2026-08-30): 19 MCP-OP-SUBST specs marked [x] to record Gene's advance ratification, not shipped code, so every one reports missing implementation AND test witnesses; re-include when substantiation telemetry ships."})

(defn- spec-doc-file?
  [^java.io.File file]
  (and (.isFile file)
       (boolean (re-matches #".+-specs\.md" (.getName file)))))

(defn spec-doc-paths
  "Derive the audited intent spec documents by SCANNING docs/intent/<leaf>/<name>-specs.md.

   Derived, never listed: a new lane adds a FILE and touches no shared vector, so
   two lanes can never conflict on this registry. Returns repo-relative paths
   sorted lexicographically, which makes the concatenated spec text deterministic.

   `excluded` is a map of repo-relative path -> one-line reason and defaults to
   `excluded-spec-docs`; it is an argument so a witness can drive the scan against
   a fixture root without the repository's own exclusions following it there.

   Fails loudly rather than silently shrinking: an `excluded` entry whose file is
   absent under `root` is an ORPHAN LISTING, and an empty scan means the intent
   tree moved."
  ([] (spec-doc-paths "." excluded-spec-docs))
  ([root] (spec-doc-paths root excluded-spec-docs))
  ([root excluded]
   (let [orphans (remove #(.isFile (io/file root %)) (sort (keys excluded)))]
     (when (seq orphans)
       (throw (ex-info (str "excluded-spec-docs names spec documents that do not exist: "
                            (str/join ", " orphans))
                       {:type :orphan-spec-doc-listing
                        :paths (vec orphans)
                        :root (str root)}))))
   (let [intent-dir (io/file root "docs" "intent")
         leaves (sort-by (fn [^java.io.File d] (.getName d))
                         (filter (fn [^java.io.File d] (.isDirectory d))
                                 (or (seq (.listFiles intent-dir)) [])))
         found (->> leaves
                    (mapcat (fn [^java.io.File leaf]
                              (for [^java.io.File f (or (seq (.listFiles leaf)) [])
                                    :when (spec-doc-file? f)]
                                (str "docs/intent/" (.getName leaf) "/" (.getName f)))))
                    (remove #(contains? excluded %))
                    sort
                    vec)]
     (when (empty? found)
       (throw (ex-info (str "no intent spec documents found under "
                            (.getPath intent-dir))
                       {:type :no-spec-docs-found
                        :root (str root)
                        :searched (.getPath intent-dir)})))
     found)))

(defn audit-current-repository
  "Audit the repository's MCP operation intent leaves and all executable witnesses."
  ([] (audit-current-repository "."))
  ([root]
   ;; Trunk's derived scan REPLACES this branch's hand-kept vector, and it
   ;; subsumes it: `spec-doc-paths` discovers every
   ;; `docs/intent/<leaf>/<name>-specs.md`, so the census specs this branch
   ;; added to the list are found by the scan rather than listed.
   (let [spec-files (map #(io/file root %) (spec-doc-paths root))]
     (audit-contract
       {:spec-text (str/join "\n" (map slurp spec-files))
        :implementation-sources
        (merge (read-sources root ["src"] [".clj" ".cljc" ".cljs"])
               (read-sources root ["Makefile"] ["Makefile"]))
        :test-sources
        (read-sources root ["test"] [".clj" ".cljc" ".cljs" ".pl"])}))))
