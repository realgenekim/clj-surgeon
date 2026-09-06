;; Same bounded prototype over Clojure data; no eval or request execution.
(require '[cheshire.core :as json])
(import '[java.security MessageDigest])

(defn sha [s]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256")
                                          (.getBytes s "UTF-8")))))
(defn need [ok reason]
  (when-not ok (throw (ex-info reason {}))))
(defn specimen [{:keys [kind request receipt]}]
  (let [raw (slurp request) proof (slurp receipt)
        q (json/parse-string raw true) record (json/parse-string proof true)
        envelope (get-in record [:response :result]) s (:structuredContent envelope)]
    (need (= q (:request record)) "request-receipt-mismatch")
    (need (and (not (true? (:isError envelope))) (true? (:ok s))) "receipt-not-success")
    (case kind
      "inspect"
      (let [requests (:requests q) files (count (set (map :file requests)))]
        (need (and (vector? requests)
                   (every? #(and (#{"outline" "match"} (:operation %))
                                  (string? (:file %))) requests)) "unsupported-inspect-shape")
        (need (and (= (get-in q [:expect :requests]) (count requests))
                   (= (get-in q [:expect :files]) files)) "request-cardinality-mismatch")
        (need (and (true? (:read_complete s)) (= (:request_count s) (count requests))
                   (= (:file_count s) files)) "receipt-cardinality-mismatch"))
      "fanout"
      (let [edits (:edits q)]
        (need (and (vector? edits) (every? #(and (string? (:file %))
                   (integer? (:matches %)) (pos? (:matches %))) edits)) "unsupported-fanout-shape")
        (need (and (true? (:committed s)) (= (:files s) (count (set (map :file edits))))
                   (= (:edits s) (reduce + (map :matches edits)))) "receipt-cardinality-mismatch"))
      "alias"
      (do (need (and (= "alias_migration" (:op q)) (integer? (get-in q [:expect :files])))
                "unsupported-alias-shape")
          (need (and (true? (:committed s)) (= (:files s) (get-in q [:expect :files])))
                "receipt-cardinality-mismatch"))
      (throw (ex-info "unsupported-kind" {})))
    (str "## " kind "\n\n```json\n" raw "```\n\n"
         "Archived successful execution; request SHA256 " (sha raw)
         "; receipt SHA256 " (sha proof) ".\n"
         "This validates the retained specimen, not current source freshness, replay safety, or task semantics.\n")))
(try
  (let [entries (json/parse-string (slurp (first *command-line-args*)) true)
        ;; Realize before printing: a refused batch emits no partial document.
        documents (mapv specimen entries)]
    (print (clojure.string/join "\n" documents)))
  (catch Exception e (binding [*out* *err*] (println "REFUSED" (.getMessage e)))
         (System/exit 2)))
