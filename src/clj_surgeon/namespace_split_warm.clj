(ns clj-surgeon.namespace-split-warm
  "Optional, bounded nREPL probe. A warm image is never fresh-process proof."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.walk :as walk])
  (:import
   (java.io PushbackInputStream)
   (java.net InetSocketAddress Socket)
   (java.nio.charset StandardCharsets)))

;; @spec NS-SPLIT-029
;; INTENT: NS-SPLIT-029
(defn selection
  "Pure reload order and test closure from final namespace edges. Unrelated
  namespaces are never explicitly reloaded. Edges point from user to dependency."
  [touched test-libs edges]
  (let [affected (loop [seen (set touched)]
                   (let [next (into seen (for [[a b] edges :when (seen b)] a))]
                     (if (= seen next) seen (recur next))))
        tests (sort (filter #(or (affected %) (some (fn [lib] (= % (str lib "-test"))) touched)) test-libs))
        selected (into (set touched) tests)
        direct (reduce (fn [m [a b]] (update m a (fnil conj #{}) b)) {} edges)
        runtime (set/difference (set touched) (set test-libs))
        dependencies (into {} (for [lib selected]
                                [lib (loop [seen (get direct lib #{})]
                                       (let [next (into seen (mapcat #(get direct % #{}) seen))]
                                         (if (= seen next)
                                           (cond-> (set/intersection selected seen)
                                             ((set tests) lib) (into runtime))
                                           (recur next))))]))
        ordered (loop [remaining selected result []]
                  (if (empty? remaining) result
                    (let [ready (sort (filter #(empty? (set/intersection remaining (get dependencies % #{}))) remaining))]
                      (when (empty? ready) (throw (ex-info "Warm reload graph contains a cycle" {})))
                      (recur (apply disj remaining ready) (into result ready)))))]
    {:reload (vec ordered) :tests (vec tests)}))

(defn eval!
  "One bounded eval on loopback; the codec is supplied by BB or the JVM's nREPL
  dependency. No subprocess and no new JVM. Only the final EDN value is retained."
  [port code timeout-ms]
  (let [codec (if (System/getProperty "babashka.version") "bencode.core" "nrepl.bencode")
        read! (requiring-resolve (symbol codec "read-bencode"))
        write! (requiring-resolve (symbol codec "write-bencode"))
        deadline (+ (System/nanoTime) (* 1000000 timeout-ms))
        left #(max 1 (int (Math/ceil (/ (- deadline (System/nanoTime)) 1e6))))
        decode #(walk/postwalk (fn [x] (if (bytes? x) (String. ^bytes x StandardCharsets/UTF_8) x)) %)]
    (with-open [socket (Socket.)]
      (.connect socket (InetSocketAddress. "127.0.0.1" (int port)) (left))
      (let [out (.getOutputStream socket)
            in (PushbackInputStream. (.getInputStream socket))
            id (str (random-uuid))]
        (write! out {"op" "eval" "id" id "code" code})
        (.flush out)
        (loop [value nil statuses #{} n 0]
          (when (or (> n 10000) (>= (System/nanoTime) deadline))
            (throw (ex-info "Warm probe timed out" {:error-type :warm-timeout})))
          (.setSoTimeout socket (left))
          (let [response (decode (read! in))
                ours? (= id (get response "id"))
                statuses (if ours? (into statuses (get response "status")) statuses)
                value (if ours? (or (get response "value") value) value)]
            (cond
              (seq (set/intersection statuses #{"eval-error" "error" "interrupted"}))
              (throw (ex-info "Warm evaluation failed" {:statuses statuses}))
              (statuses "done") (if value (edn/read-string value)
                                  (throw (ex-info "Warm evaluation returned no value" {})))
              :else (recur value statuses (inc n)))))))))

(defn discover!
  "A stale/foreign port is unavailable. Discovery only evaluates cwd; it never
  reloads code. The port file itself must be confined to the workspace."
  [root]
  (try
    (let [root (.getCanonicalFile (io/file (str root)))
          file (io/file root ".nrepl-port")]
      (when (and (.isFile file) (<= (.length file) 16)
                 (= (.getCanonicalFile file) (.getAbsoluteFile file)))
        (let [port (parse-long (str/trim (slurp file)))]
          (when (and port (<= 1 port 65535)
                     (= (str root) (eval! port "(.getCanonicalPath (java.io.File. (System/getProperty \"user.dir\")))" 300)))
            {:port port}))))
    (catch Exception _ nil)))

;; @spec NS-SPLIT-070
;; INTENT: NS-SPLIT-070
(defn probe-code [{:keys [reload tests]}]
  ;; Libraries remain printed data. Catch at the require that failed so the
  ;; receipt names the destination even when nREPL would only say eval-error.
  (str "(do (require 'clojure.test) "
       "(let [loads (loop [libs " (pr-str reload) " loaded []] "
       "(if-let [lib (first libs)] "
       "(let [ok? (try (require (symbol lib) :reload) true (catch Throwable _ false))] "
       "(if ok? (recur (rest libs) (conj loaded lib)) {:loaded loaded :load_errors [lib]})) "
       "{:loaded loaded :load_errors []}))] "
       "(assoc loads :summary "
       "(if (seq (:load_errors loads)) {:test 0 :pass 0 :fail 0 :error 0} "
       "(binding [*out* (java.io.Writer/nullWriter) *err* (java.io.Writer/nullWriter)] "
       (if (seq tests)
         (str "(apply clojure.test/run-tests (map symbol " (pr-str tests) "))")
         "{:test 0 :pass 0 :fail 0 :error 0}") ")))))"))

(defn probe!
  [live selection]
  (let [started (System/nanoTime)
        result (try
                 (let [evidence (eval! (:port live) (probe-code selection) 60000)
                       summary (:summary evidence)]
                   (if (and (map? summary) (every? #(nat-int? (get summary %)) [:test :pass :fail :error])
                            (vector? (:loaded evidence)) (every? string? (:loaded evidence))
                            (vector? (:load_errors evidence)) (every? string? (:load_errors evidence)))
                     {:ok (and (empty? (:load_errors evidence)) (zero? (+ (:fail summary) (:error summary))))
                      :summary summary :loaded (:loaded evidence) :load_errors (:load_errors evidence)}
                     {:ok false :error "Warm probe returned no valid test summary"}))
                 (catch Exception e {:ok false :error (.getMessage e)}))]
    (merge {:name "warm-probe" :duration_ms (/ (- (System/nanoTime) started) 1e6)
            :status (if (:ok result) "passed" "failed") :exit (if (:ok result) 0 1)
            :failures (or (get-in result [:summary :fail]) 0)
            :errors (or (get-in result [:summary :error]) (if (:ok result) 0 1))}
           selection result)))
