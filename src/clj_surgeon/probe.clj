(ns clj-surgeon.probe
  "Babashka client and closed identity contract for a warm MCP test probe."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))
;; @spec BB-PROBE-001
(def cold-gate :landing-gate)

(def identity-files
  ["deps.edn" "bb.edn" "src/clj_surgeon/probe.clj"
   "src/clj_surgeon/mcp_hot_verify.clj" "src/clj_surgeon/mcp_http_server.clj"])

(defn fingerprint [root]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")]
    (doseq [path identity-files]
      (.update md (.getBytes (str path "\u0000" (slurp (io/file root path)) "\u0000") "UTF-8")))
    (format "%064x" (java.math.BigInteger. 1 (.digest md)))))

(defn image-identity [root]
  {:root (.getCanonicalPath (io/file root))
   :generation (str (random-uuid))
   :fingerprint (fingerprint root)})

(defn refusal [kind message]
  ;; forwarded-refusal-kind: callers supply literal kinds or forward the
  ;; original exception kind; this constructor does not manufacture names.
  {:state :probe-refused :error-type kind :error message
   :proof_pending [cold-gate]})

;; @spec BB-PROBE-002
(defn request-problem [image current request]
  (cond
    (not (and (map? request) (= #{:ns :image} (set (keys request)))
              (string? (:ns request))
              (re-matches #"[A-Za-z][A-Za-z0-9_.-]*" (:ns request))))
    (refusal :invalid-probe-request "Supply one test namespace and the warm image identity.")
    (or (not= image (:image request)) (not= (:fingerprint image) current))
    (refusal :stale-probe-image "Restart make warm PORT=<port> in this worktree and retry.")
    :else nil))

;; @spec BB-PROBE-001
(defn verdict [reloaded summary elapsed]
  (let [failures (+ (:fail summary 0) (:error summary 0))]
    {:state (if (and (pos? (:test summary 0)) (zero? failures)) :probe-passed :probe-failed)
     :proof_pending [cold-gate]
     :reloaded reloaded :tests (:test summary 0)
     :assertions (+ (:pass summary 0) failures) :failures failures
     :elapsed_ms elapsed}))

;; @spec BB-PROBE-004 -- the whole UTF-8 encoding, before the servlet writer.
(def response-byte-bound 16384)
(def response-name-bound 64)

;; @spec BB-PROBE-004
(defn encode-response [result]
  (let [wire (pr-str result)
        size #(alength (.getBytes ^String % "UTF-8"))
        encoded (size wire)]
    (if (<= encoded response-byte-bound)
      wire
      (let [names (vec (:reloaded result))
            total (count names)
            cause (:error-type result)
            base (cond-> (assoc (select-keys result [:state :proof_pending :tests :assertions
                                                     :failures :elapsed_ms])
                                :error-type :probe-response-truncated
                                :reloaded-count total)
                   (and (keyword? cause) (<= (size (pr-str cause)) 128)) (assoc :cause cause)
                   (:error result) (assoc :error "Probe detail exceeded the response bound; see :truncated."))]
        ;; The closed verdict's scalar fields fit even with an empty prefix.
        ;; Count names only after UTF-8 encoding: one name may exceed the bound.
        (loop [n (min response-name-bound total)]
          (let [candidate (pr-str (assoc base :reloaded (subvec names 0 n)
                                    :truncated {:bound response-byte-bound
                                                :encoded encoded :omitted (- total n)}))]
            (if (<= (size candidate) response-byte-bound)
              candidate
              (recur (dec n)))))))))

(defn read-bounded [reader limit]
  (let [buf (char-array (inc limit))
        n (loop [offset 0]
            (let [n (.read ^java.io.Reader reader buf offset (- (alength buf) offset))]
              (cond (neg? n) offset
                    (= (alength buf) (+ offset n)) (inc limit)
                    :else (recur (+ offset n)))))]
    (when (> n limit) (throw (ex-info "Probe message exceeds bound" {:error-type :probe-message-too-large})))
    (edn/read-string (String. buf 0 n))))

(defn cli! [{:keys [ns image-file]}]
  (try
    (let [root (.getCanonicalPath (io/file "."))
          descriptor (with-open [r (io/reader (or image-file ".clj-surgeon/probe.edn"))]
                       (read-bounded r 8192))
          image (:image descriptor)
          request {:ns (str ns) :image image}
          problem (or (when-not (= root (:root image))
                        (refusal :stale-probe-image "Image belongs to another worktree; run make warm here."))
                      (request-problem image (fingerprint root) request))]
      (if problem problem
        (let [port (:port descriptor)]
          (when-not (and (integer? port) (< 9000 port 65536))
            (throw (ex-info "Warm port must be above 9000" {:error-type :invalid-probe-port})))
          (let [post (requiring-resolve 'babashka.http-client/post)
                response (post (str "http://127.0.0.1:" port "/probe")
                               {:headers {"Content-Type" "application/edn"}
                                :body (pr-str request) :timeout 60000 :as :stream})]
            (with-open [r (io/reader (:body response))] (read-bounded r 16384))))))
    (catch Exception e
      (refusal (or (:error-type (ex-data e)) :probe-connection-failed) (.getMessage e)))
    (finally (flush))))
