(ns marvin-voice-remote.acid-l-acceptance-test
  "ARM-INDEPENDENT acceptance suite for rung L — `L-spec.md`, \"one server-owned
   wall clock\" (`marvin-voice-remote.clock`).

   Written WITHOUT reference to any implementation's private choices. Every
   assertion is derived from a NUMBERED spec clause and from one of exactly two
   observable surfaces:

     * the SOURCE BYTES under `src/` (a sweep, not a diff), and
     * the PUBLIC behavior of the named functions with the shared clock frozen.

   The clock namespace is resolved DYNAMICALLY (`requiring-resolve`), never in
   the `ns` form, for one reason: this file must COMPILE against the unmodified
   repo at ab267f9, where `marvin-voice-remote.clock` does not exist. A missing
   clock therefore FAILS assertions; it never breaks the build.

   Twelve deftests, one per clause group, so partial credit is countable:

     acid-l-1  clause 1     the clock namespace exists with the pinned shape
     acid-l-2  clause 2,9   exactly one file in src/ names System/currentTimeMillis
     acid-l-3  clause 2     all ten adopter files require the ns and call it
     acid-l-4  clause 3     the three *now-ms* vars DELEGATE, and still WIN when bound
     acid-l-5  clause 4a    auth/make-session-cookie mints from the shared clock
     acid-l-6  clause 4b    auth/verify-session-cookie READS the shared clock
     acid-l-7  clause 4c    the /bridge4 cache-bust tokens are one server-owned read
     acid-l-8  clause 4d    director-control/now-ms delegates
     acid-l-9  clause 4e    reducer.shadow record!/snapshot stamp from the clock
     acid-l-10 clause 4f    reducer-lab-page's cache-bust token
     acid-l-11 clause 6     NO overreach: Instant/now, nanoTime, JS Date.now() intact
     acid-l-12 clause 5,7   unbound behavior unchanged + the bridge4 golden holds

   Tests 11 and 12 are GUARDS: they pass on the unmodified repo by design. They
   exist so an implementation that sweeps too widely, or that moves a golden
   byte, is caught. Tests 1-10 are the discriminators and all fail before the
   change."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [marvin-voice-remote.auth :as auth]
   [marvin-voice-remote.bridge3-new :as b3n]
   [marvin-voice-remote.channel :as channel]
   [marvin-voice-remote.reducer-lab :as lab]
   [marvin-voice-remote.reducer-session :as rs]
   [marvin-voice-remote.reducer.shadow :as shadow]
   [marvin-voice-remote.sse-registry :as sse]))

;; ---------------------------------------------------------------------------
;; Harness — dynamic resolution, so a missing clock ns fails a test, never a build
;; ---------------------------------------------------------------------------

(def ^:private clock-ns 'marvin-voice-remote.clock)

(defn- clock-var
  "The named Var in `marvin-voice-remote.clock`, or nil if the namespace or the
   name is absent. Never throws."
  [nm]
  (try (requiring-resolve (symbol (name clock-ns) (name nm)))
       (catch Throwable _ nil)))

(def ^:private no-clock
  "Sentinel returned by `frozen` when the shared clock cannot be bound. It is a
   VALUE, not an exception, so the failure shows up as a failed `is` with a
   readable message instead of an error."
  ::clock-absent)

(defn- frozen
  "Run `f` with the shared clock pinned to `ms`. Returns `f`'s value, or the
   `no-clock` sentinel when `marvin-voice-remote.clock/*now-ms-fn*` is missing
   or is not dynamic."
  [ms f]
  (if-let [v (clock-var "*now-ms-fn*")]
    (try (with-bindings* {v (fn [] (long ms))} f)
         (catch IllegalStateException _ no-clock))
    no-clock))

(defn- src-files
  "Every .clj file under src/, as {relative-path -> contents}. The suite runs
   from the repo root (kaocha's cwd), the same place `bin/kaocha` runs."
  []
  (into (sorted-map)
        (comp (filter #(and (.isFile ^java.io.File %)
                            (str/ends-with? (.getName ^java.io.File %) ".clj")))
              (map (fn [^java.io.File f]
                     [(str/replace (.getPath f) "\\" "/") (slurp f)])))
        (file-seq (io/file "src"))))

(defn- count-occurrences [^String s ^String sub]
  (loop [from 0 n 0]
    (if-let [i (str/index-of s sub from)]
      (recur (+ i (count sub)) (inc n))
      n)))

;; The ten namespaces that clause 2 says must adopt the shared clock.
(def ^:private adopter-files
  ["src/marvin_voice_remote/auth.clj"
   "src/marvin_voice_remote/blob.clj"
   "src/marvin_voice_remote/bridge3_new.clj"
   "src/marvin_voice_remote/channel.clj"
   "src/marvin_voice_remote/codex_app_server.clj"
   "src/marvin_voice_remote/director_control.clj"
   "src/marvin_voice_remote/reducer_lab.clj"
   "src/marvin_voice_remote/reducer_session.clj"
   "src/marvin_voice_remote/reducer/shadow.clj"
   "src/marvin_voice_remote/sse_registry.clj"])

(def ^:private clock-file "src/marvin_voice_remote/clock.clj")

;; A frozen instant with no relation to now: 2026-05-05T05:05:05.005Z-ish. Far
;; from wall time, so a value that "happens to match" cannot happen.
(def ^:private F 1500000000000)

(defn- millis-cache-busts
  "Every `?v=<n>` token in `html` whose n is a 13+ digit epoch-milliseconds
   value, as a set of longs. The build-time-derived `?v=` tokens (small ints)
   are deliberately excluded: they are NOT clock reads."
  [html]
  (into #{}
        (comp (map second)
              (filter #(>= (count %) 13))
              (map parse-long))
        (re-seq #"\?v=(\d+)" html)))

(defn- bridge4-html
  "Serve /bridge4 through the public ring handler, exactly as
   `scripts/check_pages.clj` and `friction_ui_test.clj` do."
  []
  (:body (channel/handle-bridge4-page {:params {"seat" "bridge"}
                                       :query-params {}})))

(def ^:private git-sha
  (try (str/trim (slurp (io/resource "build-sha.txt"))) (catch Exception _ "nosha")))

(defn- normalize
  "`scripts/check_pages.clj`'s normalization: strip the legitimately-dynamic
   bytes (build sha, ?v= cache-bust, per-load SSE client id, \"deployed X ago\")
   so the remainder is a pure function of state."
  [html]
  (-> html
      (str/replace git-sha "BUILDSHA")
      (str/replace #"\?v=\d+" "?v=V")
      (str/replace #"p-[0-9a-f]{12}" "p-CID")
      (str/replace #"deployed [^<]*" "deployed X ")
      (str/replace #"BUILDSHA · [^<]*" "BUILDSHA · X ")))

;; ---------------------------------------------------------------------------
;; 1. CLAUSE 1 — the clock namespace exists, with the pinned shape
;; ---------------------------------------------------------------------------

(deftest acid-l-1-clock-namespace-has-the-pinned-shape
  (testing "marvin-voice-remote.clock/*now-ms-fn* is a dynamic Var holding a 0-arg fn"
    (let [v (clock-var "*now-ms-fn*")]
      (is (some? v) "marvin-voice-remote.clock/*now-ms-fn* must exist")
      (when v
        (is (true? (:dynamic (meta v)))
            "*now-ms-fn* must be ^:dynamic — the whole point is that tests can bind it")
        (is (fn? @v) "*now-ms-fn* must hold a 0-arg function, not a number")
        (is (number? (@v)) "calling it must yield epoch milliseconds"))))
  (testing "marvin-voice-remote.clock/now-ms reads through that var"
    (let [nm (clock-var "now-ms")]
      (is (some? nm) "marvin-voice-remote.clock/now-ms must exist")
      (when nm
        (is (= F (frozen F #(nm)))
            "now-ms must return whatever *now-ms-fn* is bound to")
        (is (instance? Long (nm))
            "now-ms must return a long"))))
  (testing "marvin-voice-remote.clock/fixed builds a pinned 0-arg clock fn"
    (let [fx (clock-var "fixed")]
      (is (some? fx) "marvin-voice-remote.clock/fixed must exist")
      (when fx
        (is (fn? (fx 42)) "fixed must return a function")
        (is (= 42 ((fx 42))) "the returned function must yield its argument")))))

;; ---------------------------------------------------------------------------
;; 2. CLAUSES 2 + 9 — exactly one file in src/ may name System/currentTimeMillis
;; ---------------------------------------------------------------------------

(deftest acid-l-2-only-the-clock-file-names-system-currenttimemillis
  (let [hits (->> (src-files)
                  (filter (fn [[_ body]] (str/includes? body "System/currentTimeMillis")))
                  (map key)
                  sort
                  vec)]
    (testing "the sweep finds the clock file and nothing else"
      (is (= [clock-file] hits)
          (str "exactly one file under src/ may contain the text "
               "System/currentTimeMillis, and it must be " clock-file
               ". Found: " (pr-str hits))))
    (testing "the clock file names it exactly once — one canonical wall-clock read"
      (let [body (get (src-files) clock-file)]
        (is (some? body) (str clock-file " must exist"))
        (when body
          (is (= 1 (count-occurrences body "System/currentTimeMillis"))
              "clock.clj must read the machine clock in exactly one place"))))))

;; ---------------------------------------------------------------------------
;; 3. CLAUSE 2 — every adopter requires the ns under the alias `clock` and calls it
;; ---------------------------------------------------------------------------

(deftest acid-l-3-all-ten-adopters-require-and-call-the-shared-clock
  (let [files (src-files)]
    (doseq [p adopter-files]
      (testing p
        (let [body (get files p)]
          (is (some? body) (str p " must exist"))
          (when body
            (is (re-find #"\[marvin-voice-remote\.clock\s+:as\s+clock\]" body)
                (str p " must require [marvin-voice-remote.clock :as clock]"))
            (is (str/includes? body "(clock/now-ms)")
                (str p " must call (clock/now-ms)"))))))
    (testing "clock.clj itself requires nothing from this project (no cycles)"
      (let [body (get files clock-file)]
        (when body
          (is (not (re-find #"\(:require[^)]*marvin-voice-remote\." body))
              "clock.clj must not require any marvin-voice-remote namespace"))))))

;; ---------------------------------------------------------------------------
;; 4. CLAUSE 3 — the three per-namespace clocks DELEGATE, and a binding still wins
;; ---------------------------------------------------------------------------

(deftest acid-l-4-existing-now-ms-vars-delegate-but-a-local-binding-still-wins
  (doseq [[label v] [["sse-registry/*now-ms*"    #'sse/*now-ms*]
                     ["reducer-session/*now-ms*" #'rs/*now-ms*]
                     ["bridge3-new/*now-ms*"     #'b3n/*now-ms*]]]
    (testing (str label " is still a dynamic 0-arg clock fn")
      (is (true? (:dynamic (meta v))) (str label " must stay ^:dynamic"))
      (is (fn? @v) (str label " must still hold a 0-arg fn")))
    (testing (str label " DELEGATES to the shared clock when it is not itself bound")
      (is (= F (frozen F #(long (@v))))
          (str label "'s default must read through marvin-voice-remote.clock; "
               "existing production behavior is unchanged, but one freeze now "
               "freezes the whole server.")))
    (testing (str label " still WINS over the shared clock when a test binds it")
      (is (= 777 (frozen F #(with-bindings* {v (fn [] 777)} (fn [] (long (@v))))))
          (str "binding " label " must still override — the existing suite "
               "binds these vars and must keep passing")))))

;; ---------------------------------------------------------------------------
;; 5. CLAUSE 4a — auth/make-session-cookie mints its issued-at from the clock
;;    SERVER-OWNED: the issued-at stamp is minted by the server; no client
;;    supplies it, and no client can move it.
;; ---------------------------------------------------------------------------

(deftest acid-l-5-make-session-cookie-stamps-the-shared-clock
  (let [cookie (frozen F #(auth/make-session-cookie))]
    (is (string? cookie)
        "make-session-cookie must return a string under a frozen shared clock")
    (when (string? cookie)
      (let [[tag ts _sig] (str/split cookie #"\|")]
        (is (= "ok" tag) "the cookie shape \"ok|<issued-ms>|<hmac>\" is unchanged")
        (is (= (str F) ts)
            (str "the issued-at field must be the SERVER clock's value " F
                 ", got " (pr-str ts)))))))

;; ---------------------------------------------------------------------------
;; 6. CLAUSE 4b — auth/verify-session-cookie READS the same clock
;; ---------------------------------------------------------------------------

(deftest acid-l-6-verify-session-cookie-reads-the-shared-clock
  (let [cookie  (frozen F #(auth/make-session-cookie))
        day-ms  (* 24 60 60 1000)]
    (is (string? cookie) "precondition: a cookie minted under the frozen clock")
    (when (string? cookie)
      (testing "three days later (inside the 7-day max age) it still verifies"
        (is (map? (frozen (+ F (* 3 day-ms)) #(auth/verify-session-cookie cookie)))
            "verify must consult the shared clock, not the machine clock"))
      (testing "eight days later (past the 7-day max age) it does not"
        (is (nil? (frozen (+ F (* 8 day-ms)) #(auth/verify-session-cookie cookie)))
            "an expired cookie must fail when the SHARED clock says it expired")))))

;; ---------------------------------------------------------------------------
;; 7. CLAUSE 4c — /bridge4's cache-bust tokens are ONE server-owned clock read
;;    SERVER-OWNED: the `?v=` cache-bust token is minted server-side and
;;    rendered into the page. The browser never computes it; the page's own JS
;;    `Date.now()` calls are CLIENT-owned and are a different thing entirely
;;    (see acid-l-11).
;; ---------------------------------------------------------------------------

(deftest acid-l-7-bridge4-cache-busts-come-from-the-shared-clock
  (let [html (frozen F #(bridge4-html))]
    (is (string? html) "the /bridge4 handler must still serve a page")
    (when (string? html)
      (let [busts (millis-cache-busts html)]
        (testing "the page still carries millisecond cache-bust tokens"
          (is (seq busts)
              "bridge4 must still emit at least one ?v=<epoch-ms> cache-bust token"))
        (testing "every one of them is the frozen server clock"
          (is (= #{F} busts)
              (str "with the shared clock frozen at " F
                   " every millisecond ?v= token must equal it (they are two "
                   "separate reads today and can differ by 1 ms). Found: "
                   (pr-str (sort busts)))))))))

;; ---------------------------------------------------------------------------
;; 8. CLAUSE 4d — director-control/now-ms delegates (name and privacy pinned)
;; ---------------------------------------------------------------------------

(deftest acid-l-8-director-control-now-ms-delegates
  (let [v (ns-resolve 'marvin-voice-remote.director-control 'now-ms)]
    (is (some? v) "director-control/now-ms must keep its name")
    (when v
      (is (true? (:private (meta v)))
          "director-control/now-ms must stay private")
      (is (= F (frozen F #(long (v))))
          "director-control/now-ms must read through the shared clock"))))

;; ---------------------------------------------------------------------------
;; 9. CLAUSE 4e — reducer.shadow stamps records and exports from the clock
;; ---------------------------------------------------------------------------

(deftest acid-l-9-shadow-record-and-snapshot-stamp-the-shared-clock
  (let [seat "acid-l-clock"]
    (shadow/clear! seat)
    (testing "snapshot's :shadow/exported is a shared-clock read"
      (is (= F (frozen F #(:shadow/exported (shadow/snapshot seat))))
          "shadow/snapshot must stamp :shadow/exported from the shared clock"))
    (testing "record!'s :shadow/at is a shared-clock read"
      (frozen F #(shadow/record! seat :server/acid-l-probe {:source :acid-l}))
      (let [recs (:records (shadow/snapshot seat))]
        (is (= 1 (count recs))
            "the probe must have been recorded exactly once")
        (is (= F (:shadow/at (first recs)))
            "shadow/record! must stamp :shadow/at from the shared clock")))
    (shadow/clear! seat)))

;; ---------------------------------------------------------------------------
;; 10. CLAUSE 4f — reducer-lab-page's stylesheet cache-bust (server-owned)
;; ---------------------------------------------------------------------------

(deftest acid-l-10-reducer-lab-page-cache-bust-comes-from-the-shared-clock
  (let [html (frozen F #(lab/reducer-lab-page "acid-l" {}))]
    (is (string? html) "reducer-lab-page must still render")
    (when (string? html)
      (is (= [(str "/style.css?v=" F)]
             (vec (re-seq #"/style\.css\?v=\d+" html)))
          (str "the stylesheet cache-bust must be the frozen server clock " F)))))

;; ---------------------------------------------------------------------------
;; 11. CLAUSE 6 — GUARD: no overreach. Other time sources are NOT swept.
;;     `Instant/now` and `System/nanoTime` are out of scope; JS `Date.now()`
;;     inside the page strings is CLIENT-owned and must not be touched.
;; ---------------------------------------------------------------------------

(deftest acid-l-11-other-time-sources-are-untouched
  (let [files  (src-files)
        all    (str/join "\n" (vals files))
        chan   (get files "src/marvin_voice_remote/channel.clj")]
    (testing "java.time Instant/now and System/nanoTime are out of scope"
      (is (>= (+ (count-occurrences all "Instant/now")
                 (count-occurrences all "System/nanoTime"))
              21)
          "clause 6 forbids sweeping Instant/now or System/nanoTime; at least 21 remain"))
    (testing "the page's CLIENT-owned Date.now() calls survive"
      (is (some? chan) "channel.clj must exist")
      (when chan
        (is (>= (count-occurrences chan "Date.now(") 23)
            "JS Date.now() inside the served page strings is client-owned: the browser's clock, not the server's")))))

;; ---------------------------------------------------------------------------
;; 12. CLAUSES 5 + 7 — GUARD: unbound behavior unchanged, and the golden holds
;; ---------------------------------------------------------------------------

(deftest acid-l-12-unbound-behavior-and-the-bridge4-golden-are-unchanged
  (testing "with nothing bound, every clock still reads real wall time"
    (let [wall (System/currentTimeMillis)
          near? (fn [x] (and (number? x) (< (abs (- (long x) wall)) 30000)))]
      (is (near? (@#'sse/*now-ms*)) "sse-registry/*now-ms* unbound must be wall time")
      (is (near? (@#'rs/*now-ms*))  "reducer-session/*now-ms* unbound must be wall time")
      (is (near? (@#'b3n/*now-ms*)) "bridge3-new/*now-ms* unbound must be wall time")
      (when-let [nm (clock-var "now-ms")]
        (is (near? (nm)) "clock/now-ms unbound must be wall time"))))
  (testing "the default /bridge4 page is still byte-identical to its golden"
    (let [golden (io/file "test/golden/bridge4.html")]
      (is (.exists golden) "test/golden/bridge4.html must exist")
      (when (.exists golden)
        (let [expected (slurp golden)
              actual   (normalize (bridge4-html))]
          (is (= expected actual)
              (str "/bridge4 must stay byte-identical to test/golden/bridge4.html "
                   "after normalization. expected " (count expected)
                   " bytes, got " (count actual))))))))
