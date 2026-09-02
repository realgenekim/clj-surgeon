(ns clj-surgeon.mcp-format-scope-real-test
  "The premise probe, committed and executable: run the REAL pinned formatter.

   The design doc's central measured claim is that
   `standard-clojure-style fix` on a complete top-level form in isolation
   behaves the same as it does inside the whole file, and that the
   clause-normalised stream admits everything it does. That claim was measured
   once from a scratch script and then asserted in prose. This test executes
   the real binary over a committed fixture so the claim is checked rather than
   remembered.

   It is skipped, loudly and explicitly, unless `CLJ_SURGEON_REAL_FORMATTER` is
   set — `make mcp-test-formatter` sets it. `make mcp-test` does not, so the
   ordinary suite never depends on `npx`, a network, or an npm cache."
  (:require
   [clj-surgeon.format-scope :as format-scope]
   [clj-surgeon.mcp-formatter :as formatter]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private fixture-path "test-fixtures/format-scope/premise.clj")

(defn- enabled?
  []
  (some? (System/getenv "CLJ_SURGEON_REAL_FORMATTER")))

(defn- run-real-formatter!
  "Format `texts` as separate files through the pinned binary. Returns the
   formatted texts in order, or a map with :skip when the binary is unusable."
  [texts]
  (let [dir (io/file (str (System/getProperty "java.io.tmpdir")
                          "/clj-surgeon-real-fmt-" (System/nanoTime)))]
    (.mkdirs dir)
    (try
      (let [files (vec (map-indexed
                         (fn [index text]
                           (let [file (io/file dir (format "f%02d.clj" index))]
                             (spit file text)
                             file))
                         texts))
            result (apply shell/sh
                          (concat ["npx" "--yes"
                                   (str "@chrisoakman/standard-clojure-style@"
                                        formatter/formatter-version)
                                   "fix"]
                                  (mapv #(.getPath %) files)
                                  [:dir (.getPath dir)]))]
        (if (zero? (:exit result))
          (mapv #(format-scope/trim-trailing-newlines (slurp %)) files)
          {:skip (str "exit " (:exit result) ": "
                      (str/trim (str (:out result) (:err result))))}))
      (catch Exception error
        {:skip (str (.getName (class error)) ": " (.getMessage error))})
      (finally
        (doseq [file (reverse (file-seq dir))]
          (io/delete-file file true))))))

;; @spec MCP-OP-FMT-012
(deftest the-real-pinned-formatter-agrees-with-the-clause-normalised-stream
  (if-not (enabled?)
    (println "  SKIPPED clj-surgeon.mcp-format-scope-real-test:"
             "set CLJ_SURGEON_REAL_FORMATTER=1 (make mcp-test-formatter)"
             "to execute the real binary")
    (let [source (slurp (io/file fixture-path))
          spans (format-scope/top-level-form-spans source)
          forms (mapv (fn [{:keys [start end]}] (subs source start end)) spans)
          formatted (run-real-formatter! forms)]
      (if (:skip formatted)
        (do (println "  SKIPPED real formatter probe:" (:skip formatted))
            (is true "the binary is unavailable; the gate reported why"))
        (do
          (is (= 5 (count forms))
              "an ns form with a comment inside its :require, and four defns
               carrying an unspaced comment, an end-of-line comment, an if, a
               non-commutative call and a multi-line string")
          (testing "every form the real formatter touches keeps its stream"
            (doseq [[index before after] (map vector (range) forms formatted)]
              (is (some? (format-scope/clause-normalised-stream after))
                  (str "form " index " came back parseable"))
              (is (= (format-scope/clause-normalised-stream before)
                     (format-scope/clause-normalised-stream after))
                  (str "form " index " is admissible under the check that"
                       " bounds a scoped format"))))
          (testing "and the ns clauses really do come back sorted"
            (let [ns-after (first formatted)]
              (is (< (str/index-of ns-after "[aaa.first")
                     (str/index-of ns-after "[zzz.last"))
                  "requires sorted")
              (is (< (str/index-of ns-after "(java.io File)")
                     (str/index-of ns-after "(java.util Date)"))
                  "imports sorted")
              (is (not= (first forms) ns-after)
                  "so this fixture really exercises the sanctioned reorder")))
          (testing "an if form's branches are not reordered by the real binary"
            (let [authorize (nth formatted 1)]
              (is (< (str/index-of authorize "(grant)")
                     (str/index-of authorize "(deny)")))))
          (testing "the comment shapes 0.29.0 rewrites are admitted as layout"
            (let [authorize (nth formatted 1)
                  touch (nth formatted 3)
                  ns-after (first formatted)]
              (is (str/includes? authorize ";; no space after the semicolons")
                  "the real binary really does insert the space — this is the
                   shape that made the wire route refuse an ordinary edit")
              (is (str/includes?
                    touch ";; an end-of-line comment, inside the form"))
              (is (str/includes?
                    ns-after ";; this comment must travel with the clause it precedes")
                  "and the comment inside the :require list survives the sort")))
          (testing "a comment inside the require list stays with its clause"
            (let [ns-after (first formatted)
                  comment-at (str/index-of
                               ns-after
                               ";; this comment must travel with the clause")
                  aaa-at (str/index-of ns-after "[aaa.first")
                  zzz-at (str/index-of ns-after "[zzz.last")]
              (is (< comment-at aaa-at)
                  "the real binary keeps it in front of the clause it preceded")
              (is (< aaa-at zzz-at))))
          (testing "no byte inside a multi-line string is touched"
            (let [doc-lines (nth formatted 4)]
              (is (str/includes? doc-lines "line two with   runs of spaces")
                  "runs of spaces inside a string are content, not layout")
              (is (str/includes? doc-lines "\n     line two"))))
          (testing "the scoped stage over the whole fixture touches no byte
                    between forms"
            (let [guard {fixture-path
                         {:reference source
                          :spans (mapv (fn [{:keys [start end]}]
                                         {:offset start :length (- end start)})
                                       spans)}}
                  result (formatter/format-scoped-candidates!
                           "." formatter/default-command
                           {fixture-path source} guard
                           (fn [_ _ sources]
                             (let [ordered (vec (sort (keys sources)))
                                   out (run-real-formatter!
                                         (mapv #(get sources %) ordered))]
                               (if (:skip out)
                                 {:ok false :error-type :formatter-failed
                                  :error (:skip out) :source-unchanged true}
                                 {:ok true :status :complete
                                  :file-count (count sources)
                                  :changed-file-count (count sources)
                                  :elapsed_ms 0.0
                                  :future-sources (zipmap ordered out)}))))
                  after (get (:future-sources result) fixture-path)]
              (is (:ok result) (pr-str result))
              (is (str/includes? after ";; a comment block between forms")
                  "the comment block between forms is byte-identical")
              (is (str/includes? after ";; that the scoped formatter must never see")))))))))
