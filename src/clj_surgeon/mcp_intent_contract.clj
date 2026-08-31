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

(defn audit-current-repository
  "Audit the repository's MCP operation intent leaves and all executable witnesses."
  ([] (audit-current-repository "."))
  ([root]
   (let [spec-files
         [(io/file root
                   "docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md")
          (io/file root
                   "docs/intent/read-request-normalization/read-request-normalization-specs.md")
          (io/file root
                   "docs/intent/prepared-request/prepared-request-specs.md")
          (io/file root
                   "docs/intent/prepared-request-actions/prepared-request-actions-specs.md")
          (io/file root
                   "docs/intent/write-refusal-completeness/write-refusal-completeness-specs.md")]]
     (audit-contract
       {:spec-text (str/join "\n" (map slurp spec-files))
        :implementation-sources
        (merge (read-sources root ["src"] [".clj" ".cljc" ".cljs"])
               (read-sources root ["Makefile"] ["Makefile"]))
        :test-sources
        (read-sources root ["test"] [".clj" ".cljc" ".cljs" ".pl"])}))))
