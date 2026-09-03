(ns clj-surgeon.read-hook-test
  "Executable falsifiers for docs/intent/read-hook.

  These are deliberately subprocess tests. The contract under test is what a
  process named `rg`, first on an agent's PATH, writes to its own streams and
  returns as its own status; there is no pure decision function hiding behind
  that boundary, and a witness that stopped at the decision function would pass
  while the shim printed nothing.

  The read path is stood up as a raw-socket stub speaking the same streamable
  HTTP shape the real server speaks (JSON for `initialize`, one SSE frame for
  `tools/call`), so every case here is hermetic: no clj-surgeon server, no
  fixed port, no network."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; the tree under test

(def ^:private core-clj
  "(ns app.core
  \"Core of the fixture app.\"
  (:require [clojure.string :as str]))

(def started (System/currentTimeMillis))

(defn tick
  [state]
  (assoc state :at (System/currentTimeMillis)))

(defn label
  [state]
  (str/join \"-\" [(:name state) (:at state)]))
")

(def ^:private util-clj
  "(ns app.util
  (:require [clojure.set :as set]))

(defn merge-tags
  [a b]
  (set/union a b))

(defn stamp
  [m]
  (assoc m :stamped-at (System/currentTimeMillis)))
")

(def ^:private nested-clj
  "(ns app.deep.nested)

(def zero 0)

(defn now
  []
  (System/currentTimeMillis))
")

(defn- write!
  [root rel content]
  (let [f (fs/file root rel)]
    (fs/create-dirs (fs/parent f))
    (spit f content)
    (str f)))

(defn- clojure-tree!
  "A workspace whose `src` holds Clojure source and nothing else."
  []
  (let [root (str (fs/create-temp-dir {:prefix "read-hook-clj-"}))]
    (write! root "deps.edn" "{:paths [\"src\"]}\n")
    (write! root "src/app/core.clj" core-clj)
    (write! root "src/app/util.clj" util-clj)
    (write! root "src/app/deep/nested.clj" nested-clj)
    (write! root "notes.txt" "System/currentTimeMillis outside src\n")
    root))

(defn- mixed-tree!
  "The same workspace with one non-Clojure file INSIDE `src`."
  []
  (let [root (clojure-tree!)]
    (write! root "src/app/README.md" "# app\n\nUses System/currentTimeMillis.\n")
    root))

(def ^:private tree-files
  "The `ls-tree` receipt rows for `clojure-tree!`, dir = src, hand written."
  [{:file "app/core.clj" :ns "app.core" :form_count 5 :line_count 17}
   {:file "app/deep/nested.clj" :ns "app.deep.nested" :form_count 3 :line_count 7}
   {:file "app/util.clj" :ns "app.util" :form_count 3 :line_count 11}])

;; ---------------------------------------------------------------------------
;; the read-path stub

(defn- read-headers
  [^java.io.InputStream in]
  (let [sb (StringBuilder.)]
    (loop []
      (let [b (.read in)]
        (cond
          (neg? b) nil
          :else (do (.append sb (char b))
                    (if (str/ends-with? (str sb) "\r\n\r\n")
                      (str sb)
                      (recur))))))))

(defn- content-length
  [headers]
  (or (some->> (str/split-lines (or headers ""))
               (keep #(second (re-matches #"(?i)content-length:\s*(\d+)\s*" %)))
               first
               parse-long)
      0))

(defn- read-body
  [^java.io.InputStream in n]
  (let [buf (byte-array n)]
    (loop [off 0]
      (if (>= off n)
        (String. buf "UTF-8")
        (let [r (.read in buf off (- n off))]
          (if (neg? r) (String. buf 0 off "UTF-8") (recur (+ off r))))))))

(defn- respond!
  [^java.io.OutputStream out status content-type body extra-headers]
  (let [bytes (.getBytes (str body) "UTF-8")
        head (str "HTTP/1.1 " status "\r\n"
                  (when content-type (str "Content-Type: " content-type "\r\n"))
                  (str/join (for [[k v] extra-headers] (str k ": " v "\r\n")))
                  "Content-Length: " (alength bytes) "\r\n"
                  "Connection: close\r\n\r\n")]
    (.write out (.getBytes head "UTF-8"))
    (.write out bytes)
    (.flush out)))

(defn- tools-call-result
  [{:keys [root files file-count read-complete truncated]}]
  {:content [{:type "text" :text "inspect_clojure · ls-tree"}]
   :structuredContent
   {:ok true
    :operation "ls-tree"
    :mode "ls-tree"
    :dir "src"
    :workspace_root root
    :format "names"
    :files files
    :file_count (or file-count (count files))
    :returned (count files)
    :omitted 0
    :read_complete (if (nil? read-complete) true read-complete)
    :truncated (boolean truncated)
    :next_action "none"}
   :isError false})

(defn- start-stub!
  "A one-thread HTTP/1.1 stub on an OS-assigned port. Returns {:url :stop! :calls}."
  [opts]
  (let [server (java.net.ServerSocket. 0 16 (java.net.InetAddress/getByName "127.0.0.1"))
        calls (atom [])
        running (atom true)
        worker
        (future
          (while @running
            (try
              (with-open [sock (.accept server)]
                (let [in (.getInputStream sock)
                      out (.getOutputStream sock)
                      headers (read-headers in)
                      body (read-body in (content-length headers))
                      payload (try (json/parse-string body true) (catch Exception _ nil))
                      method (:method payload)]
                  (swap! calls conj method)
                  (case method
                    "initialize"
                    (respond! out "200 OK" "application/json;charset=utf-8"
                              (json/generate-string
                                {:jsonrpc "2.0" :id (:id payload)
                                 :result {:protocolVersion "2025-06-18"
                                          :capabilities {:tools {}}
                                          :serverInfo {:name "stub" :version "0"}}})
                              {"Mcp-Session-Id" "stub-session"})

                    "tools/call"
                    (respond! out "200 OK" "text/event-stream;charset=utf-8"
                              (str "event: message\r\ndata: "
                                   (json/generate-string
                                     {:jsonrpc "2.0" :id (:id payload)
                                      :result (tools-call-result opts)})
                                   "\r\n\r\n")
                              {})

                    (respond! out "202 Accepted" nil "" {}))))
              (catch Exception _ nil))))]
    {:url (str "http://127.0.0.1:" (.getLocalPort server) "/mcp")
     :calls calls
     :stop! (fn []
              (reset! running false)
              (try (.close server) (catch Exception _ nil))
              (future-cancel worker))}))

;; ---------------------------------------------------------------------------
;; running the hook exactly as an arm would

(def ^:private repo-root (.getCanonicalPath (io/file ".")))

(defn- install-shim!
  "A private bin directory holding the hook under the name ripgrep is called by."
  []
  (let [bin (str (fs/create-temp-dir {:prefix "read-hook-bin-"}))]
    (fs/create-sym-link (fs/path bin "rg") (fs/path repo-root "bin" "rg-clj"))
    bin))

(defn- real-rg []
  (some #(when (fs/executable? %) (str %))
        ["/usr/bin/rg" "/usr/local/bin/rg" "/bin/rg"]))

(defn- run
  "Invoke the hook as `rg`, from `dir`, with the hook's own bin first on PATH."
  [{:keys [dir bin env]} & args]
  @(proc/process
     (into [(str bin "/rg")] args)
     {:dir dir
      :out :string
      :err :string
      :continue true
      :extra-env (merge {"PATH" (str bin ":" (System/getenv "PATH"))
                         "SURGEON_URL" ""
                         "SURGEON_ROUTE_LOG" ""}
                        env)}))

(defn- run-rg
  "The same invocation through the real ripgrep, deterministically ordered."
  [dir & args]
  @(proc/process
     (into [(real-rg) "--sort" "path"] args)
     {:dir dir :out :string :err :string :continue true}))

(defn- route-records
  [log]
  (when (fs/exists? log)
    (->> (str/split-lines (slurp (str log)))
         (remove str/blank?)
         (mapv #(json/parse-string % true)))))

(defmacro ^:private with-served
  "Bind a Clojure tree, a stub read path serving its true file set, a shim, and
  the stub's own record of the JSON-RPC methods it was asked for."
  [[root-sym ctx-sym log-sym calls-sym
    & {:keys [files file-count read-complete truncated]}]
   & body]
  `(let [~root-sym (clojure-tree!)
         stub# (start-stub! {:root ~root-sym
                             :files (or ~files tree-files)
                             :file-count ~file-count
                             :read-complete ~read-complete
                             :truncated ~truncated})
         bin# (install-shim!)
         ~calls-sym (:calls stub#)
         ~log-sym (str (fs/path ~root-sym "route.jsonl"))
         ~ctx-sym {:dir ~root-sym
                   :bin bin#
                   :env {"SURGEON_URL" (:url stub#)
                         "SURGEON_ROUTE_LOG" ~log-sym
                         "SURGEON_WORKSPACE_ROOT" ~root-sym}}]
     (try ~@body
          (finally
            ((:stop! stub#))
            (fs/delete-tree ~root-sym {:force true})
            (fs/delete-tree bin# {:force true})))))

;; ---------------------------------------------------------------------------
;; MCP-OP-READ-HOOK-001 — byte identity

;; @spec MCP-OP-READ-HOOK-001
(deftest served-answer-is-byte-identical-to-ripgrep
  (with-served [root ctx log calls]
    (doseq [args [["-n" "-C" "5" "System/currentTimeMillis|\\(ns app" "src"]
                  ["-n" "-C" "8" "System/currentTimeMillis|\\(ns " "src"]
                  ["-n" "System/currentTimeMillis" "src"]
                  ["-l" "System/currentTimeMillis" "src"]
                  ["--files-with-matches" "System/currentTimeMillis" "src"]
                  ["-n" "-e" "System/currentTimeMillis" "src"]]]
      (testing (str/join " " args)
        (let [hooked (apply run ctx args)
              native (apply run-rg root args)]
          (is (= (:out native) (:out hooked))
              "served stdout must be ripgrep's own bytes")
          (is (= "" (:err hooked)))
          (is (= "surgeon"
                 (:served_by (last (route-records log))))
              "this invocation must have been served, not fallen back")
          (is (some #{"tools/call"} @calls)
              (str "the read path must actually have been asked; a hook that "
                   "quietly searched ripgrep's own candidate set would pass "
                   "every other assertion here")))))))

;; ---------------------------------------------------------------------------
;; MCP-OP-READ-HOOK-002 / -008 — total, silent fallback

;; @spec MCP-OP-READ-HOOK-002
;; @spec MCP-OP-READ-HOOK-008
(deftest unservable-invocations-fall-back-to-real-ripgrep
  (let [root (mixed-tree!)
        stub (start-stub! {:root root :files tree-files})
        bin (install-shim!)
        log (str (fs/path root "route.jsonl"))
        ctx {:dir root :bin bin
             :env {"SURGEON_URL" (:url stub)
                   "SURGEON_ROUTE_LOG" log
                   "SURGEON_WORKSPACE_ROOT" root}}]
    (try
      (testing "a non-Clojure candidate file under the path argument"
        (let [hooked (run ctx "-n" "System/currentTimeMillis" "src")
              native @(proc/process [(real-rg) "-n" "System/currentTimeMillis" "src"]
                                    {:dir root :out :string :err :string :continue true})]
          (is (= (set (str/split-lines (:out native)))
                 (set (str/split-lines (:out hooked)))))
          (is (= "" (:err hooked)) "the hook writes nothing of its own")
          (is (= "fallback" (:served_by (last (route-records log)))))))
      (testing "a file, not a directory, as the path argument"
        (let [hooked (run ctx "-n" "System/currentTimeMillis" "src/app/core.clj")]
          (is (str/includes? (:out hooked) "System/currentTimeMillis"))
          (is (= "" (:err hooked)))
          (is (= "fallback" (:served_by (last (route-records log)))))))
      (finally
        ((:stop! stub))
        (fs/delete-tree root {:force true})
        (fs/delete-tree bin {:force true})))))

;; @spec MCP-OP-READ-HOOK-002
(deftest unsupported-flags-fall-back
  (with-served [root ctx log calls]
    (doseq [args [["-n" "--max-depth" "1" "System/currentTimeMillis" "src"]
                  ["-n" "--sortr" "path" "System/currentTimeMillis" "src"]
                  ["--files"]
                  ["-n" "--nonsense-flag-the-hook-never-heard-of" "x" "src"]]]
      (testing (str/join " " args)
        (let [hooked (apply run ctx args)
              native @(proc/process (into [(real-rg)] args)
                                    {:dir root :out :string :err :string :continue true})]
          (is (= (:exit native) (:exit hooked)))
          (is (= (set (str/split-lines (:err native)))
                 (set (str/split-lines (:err hooked))))
              "an error must be ripgrep's error, not the hook's")
          (is (= "fallback" (:served_by (last (route-records log))))))))))

;; @spec MCP-OP-READ-HOOK-002
(deftest unreachable-read-path-falls-back
  (let [root (clojure-tree!)
        bin (install-shim!)
        log (str (fs/path root "route.jsonl"))]
    (try
      (testing "no SURGEON_URL at all"
        (let [hooked (run {:dir root :bin bin :env {"SURGEON_ROUTE_LOG" log}}
                          "-n" "System/currentTimeMillis" "src")]
          (is (str/includes? (:out hooked) "System/currentTimeMillis"))
          (is (= "" (:err hooked)))
          (is (= "fallback" (:served_by (last (route-records log)))))))
      (testing "a SURGEON_URL nothing is listening on"
        (let [dead (let [s (java.net.ServerSocket. 0)
                         p (.getLocalPort s)]
                     (.close s)
                     (str "http://127.0.0.1:" p "/mcp"))
              hooked (run {:dir root :bin bin
                           :env {"SURGEON_URL" dead
                                 "SURGEON_ROUTE_LOG" log
                                 "SURGEON_HOOK_TIMEOUT_MS" "2000"}}
                          "-n" "System/currentTimeMillis" "src")]
          (is (str/includes? (:out hooked) "System/currentTimeMillis"))
          (is (= "" (:err hooked)))
          (is (= "fallback" (:served_by (last (route-records log)))))))
      (testing "a truncated receipt is refused"
        (let [stub (start-stub! {:root root :files (take 2 tree-files)
                                 :file-count 3 :read-complete false :truncated true})]
          (try
            (let [hooked (run {:dir root :bin bin
                               :env {"SURGEON_URL" (:url stub)
                                     "SURGEON_ROUTE_LOG" log
                                     "SURGEON_WORKSPACE_ROOT" root}}
                              "-n" "System/currentTimeMillis" "src")]
              (is (= 4 (count (re-seq #"System/currentTimeMillis" (:out hooked))))
                  "every match in the tree, not the two the receipt carried")
              (is (= "fallback" (:served_by (last (route-records log))))))
            (finally ((:stop! stub))))))
      (finally
        (fs/delete-tree root {:force true})
        (fs/delete-tree bin {:force true})))))

;; ---------------------------------------------------------------------------
;; MCP-OP-READ-HOOK-003 — the route log is the cohort's meter

;; @spec MCP-OP-READ-HOOK-003
(deftest every-invocation-appends-exactly-one-route-record
  (with-served [root ctx log calls]
    (run ctx "-n" "System/currentTimeMillis" "src")
    (run ctx "-n" "System/currentTimeMillis" "src/app/core.clj")
    (let [records (route-records log)]
      (is (= 2 (count records)) "one record per invocation, no more and no fewer")
      (is (= ["surgeon" "fallback"] (mapv :served_by records)))
      (is (= [["src"] ["src/app/core.clj"]] (mapv :paths records))
          "the path arguments, verbatim")
      (doseq [r records]
        (is (vector? (:flags r)))
        (is (contains? r :reason))
        (is (nat-int? (:ms r)))
        (is (nat-int? (:bytes r))))
      (is (= (count (.getBytes ^String (:out (run ctx "-n" "System/currentTimeMillis" "src"))
                              "UTF-8"))
             (:bytes (last (route-records log))))
          "bytes is the size of the answer that was actually printed"))))

;; ---------------------------------------------------------------------------
;; MCP-OP-READ-HOOK-004 — exit status

;; @spec MCP-OP-READ-HOOK-004
(deftest exit-status-is-ripgreps-own
  (with-served [root ctx log calls]
    (is (= 0 (:exit (run ctx "-n" "System/currentTimeMillis" "src")))
        "0 when something matched")
    (is (= 1 (:exit (run ctx "-n" "ZZZ-no-such-token-ZZZ" "src")))
        "1 when nothing matched, even though the hook served it")
    (is (= 2 (:exit (run ctx "-n" "(unclosed" "src")))
        "2 on a pattern ripgrep rejects")))

;; ---------------------------------------------------------------------------
;; MCP-OP-READ-HOOK-005 — no exec loop

;; @spec MCP-OP-READ-HOOK-005
(deftest real-ripgrep-is-never-the-hook-itself
  (with-served [root ctx log calls]
    (testing "the hook is first on PATH under the name rg and still terminates"
      (let [hooked (run ctx "-n" "System/currentTimeMillis" "src")]
        (is (= 0 (:exit hooked)))
        (is (str/includes? (:out hooked) "System/currentTimeMillis"))))
    (testing "a second copy of the hook earlier on PATH is skipped too"
      (let [decoy (install-shim!)]
        (try
          (let [hooked (run (update ctx :env assoc
                                    "PATH" (str decoy ":" (:bin ctx) ":"
                                                (System/getenv "PATH")))
                            "-n" "System/currentTimeMillis" "src")]
            (is (= 0 (:exit hooked)))
            (is (str/includes? (:out hooked) "System/currentTimeMillis")))
          (finally (fs/delete-tree decoy {:force true})))))))

;; ---------------------------------------------------------------------------
;; MCP-OP-READ-HOOK-006 — the filename prefix survives the substitution

;; @spec MCP-OP-READ-HOOK-006
(deftest filename-prefix-survives-the-substitution
  (with-served [root ctx log calls]
    (let [hooked (run ctx "-n" "System/currentTimeMillis" "src")]
      (is (= "surgeon" (:served_by (last (route-records log)))))
      (is (every? #(str/starts-with? % "src/")
                  (remove str/blank? (str/split-lines (:out hooked))))
          "a directory argument prints a filename prefix on every line"))))

;; ---------------------------------------------------------------------------
;; MCP-OP-READ-HOOK-007 — the read path's set is the set, and it is falsifiable

;; @spec MCP-OP-READ-HOOK-007
(deftest a-read-path-set-that-disagrees-with-ripgrep-is-refused
  (testing "a read path that omits a file the tree holds"
    (with-served [root ctx log calls :files (vec (butlast tree-files))]
      (let [hooked (run ctx "-n" "System/currentTimeMillis" "src")]
        (is (= 4 (count (re-seq #"System/currentTimeMillis" (:out hooked))))
            "every match, because the disagreement forced a fallback")
        (is (= "fallback" (:served_by (last (route-records log)))))
        (is (= "discovery-mismatch" (:reason (last (route-records log))))))))
  (testing "a read path that names a file the tree does not hold"
    (with-served [root ctx log calls
                  :files (conj tree-files
                               {:file "app/ghost.clj" :ns "app.ghost"
                                :form_count 1 :line_count 1})]
      (let [hooked (run ctx "-n" "System/currentTimeMillis" "src")]
        (is (= 4 (count (re-seq #"System/currentTimeMillis" (:out hooked)))))
        (is (= "fallback" (:served_by (last (route-records log)))))
        (is (= "discovery-mismatch" (:reason (last (route-records log)))))))))
