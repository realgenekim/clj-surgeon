(require '[clj-surgeon.mcp-formatter :as formatter])

;; Field witness: packet 53079781 at c2ec3039 could not write HOME/.npm.
;; The shell supplies an empty, read-only npm cache. This is the product
;; formatter entry point, including staging, its real command, and launcher.
(let [result (formatter/format-candidates!
               (System/getProperty "user.dir") formatter/default-command
               {"app.clj" "(ns app)\n(def x    1)\n"})]
  (prn (dissoc result :future-sources))
  (assert (:ok result) "Product formatter must work with an unwritable home npm cache")
  (assert (= "(ns app)\n\n(def x 1)\n" (get-in result [:future-sources "app.clj"]))))
