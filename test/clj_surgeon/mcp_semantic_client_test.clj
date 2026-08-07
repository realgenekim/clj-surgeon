(ns clj-surgeon.mcp-semantic-client-test
  (:require
   [clj-surgeon.mcp-semantic-client :as semantic]
   [clojure.test :refer [deftest is]]))

(deftest init-preserves-a-hot-client-at-the-same-url
  (let [sentinel (Object.)]
    (reset! semantic/runtime {:url "http://127.0.0.1:7890/mcp" :client sentinel})
    (is (= {:url "http://127.0.0.1:7890/mcp" :connected true}
           (semantic/init! {:url "http://127.0.0.1:7890/mcp"})))
    (is (identical? sentinel (:client @semantic/runtime)))))

(deftest init-discards-a-client-when-the-provider-url-changes
  (let [closed? (atom false)
        fake-client (reify java.lang.AutoCloseable
                      (close [_] (reset! closed? true)))]
    (reset! semantic/runtime {:url "http://127.0.0.1:7890/mcp" :client fake-client})
    (with-redefs [semantic/close! #(do (reset! closed? true)
                                       (swap! semantic/runtime assoc :client nil))]
      (is (= {:url "http://127.0.0.1:7891/mcp" :connected false}
             (semantic/init! {:url "http://127.0.0.1:7891/mcp"}))))
    (is @closed?)))
