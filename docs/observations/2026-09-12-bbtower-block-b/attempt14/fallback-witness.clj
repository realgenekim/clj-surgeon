(require '[clj-surgeon.mcp-formatter :as formatter]
         '[clj-surgeon.mcp-process :as process])

;; Exercise the real fallback command; only binary discovery is made absent.
(with-redefs [process/resolve-executable (constantly nil)]
  (let [result (formatter/format-candidates!
                 (System/getProperty "user.dir") formatter/default-command
                 {"app.clj" "(ns app)\n(defn f\n[x]\n(+ x 1))\n"})]
    (prn (dissoc result :future-sources))
    (assert (:ok result))
    (assert (false? (get-in result [:formatter :resolved?])))
    (assert (= "(ns app)\n(defn f\n  [x]\n  (+ x 1))\n" (get-in result [:future-sources "app.clj"]))))
  (doseq [directory ["npm-cache" "npm-logs"]]
    (assert (.isDirectory (clojure.java.io/file (process/selected-temp-root) directory)))))
