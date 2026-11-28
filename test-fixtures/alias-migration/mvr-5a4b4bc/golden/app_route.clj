(ns marvin-voice-remote.app-route
  "S1 (ARCHITECTURE.md §7, D1, D9) — the two no-regret trust/provenance
   surfaces that don't touch a single existing page:

   1. GET /app — D1's CANONICAL route. During S1 it is an ALIAS ONLY: a
      redirect to today's accepted daily driver (`/bridge4`), preserving the
      `?seat=` query param a caller arrived with. It becomes the real single
      content-addressed bundle at S5c ('the sacred surface flips last'); the
      numbered routes (`/bridge3`, `/bridge4`, `/voice-lab`) start redirecting
      the OTHER way (numbered -> /app) only after that flip, never before.
      Until then this route is purely additive: it creates a new front door,
      it does not move or touch the existing one.

   2. GET /api/release-manifest — D9 ('every deployable emits a
      machine-readable manifest... manifests displayable on every surface').
      Reports only what this running artifact can HONESTLY attest about
      itself right now, reusing the read-only-resource-or-nil pattern from
      `reducer-lab` (S12/S13: identity is READ, never invented — a missing
      build-sha.txt must render 'unavailable', never a git-shelled-out guess
      or a fabricated timestamp). It also names, in `:manifest/incomplete`,
      the D9 fields this endpoint cannot yet attest (container digest,
      lockfile hashes, test-result ids, ...) — the honest gap is itself part
      of the conformance, not a TODO to hide."
  (:require
   [marvin-voice-remote.json :as mjson]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [marvin-voice-remote.reducer.policy :as policy]
   [ring.util.codec :as codec]))

;; --- Read-only resource helpers (mirrors reducer_lab.clj's pattern) -------

(defn- resource-line
  "A classpath resource read as one trimmed line, or nil if it is absent,
   unreadable, or blank. Never guesses at a fallback identity."
  [path]
  (try
    (some-> (io/resource path) slurp str/trim not-empty)
    (catch Exception _ nil)))

(defn- protocol-version
  "The wire protocol version from resources/protocol/marvin-voice-1.edn, or
   nil if the resource is missing/unreadable/malformed. Read fresh here
   (rather than depending on reducer.protocol) so this manifest can still
   report honestly even if that namespace fails to load."
  []
  (try
    (some-> (io/resource "protocol/marvin-voice-1.edn")
            slurp
            edn/read-string
            :protocol/version)
    (catch Exception _ nil)))

;; --- TASK 1: GET /app — canonical route, alias-only during S1 ------------

(defn handle-app
  "GET /app (D1 canonical route; S1 alias only, see ns docstring).
   302s to /bridge4, carrying ?seat= through unchanged when present;
   when absent, redirects with no seat param at all."
  [req]
  (let [seat (get-in req [:query-params "seat"])
        location (if (seq seat)
                   (str "/bridge4?seat=" (codec/url-encode seat))
                   "/bridge4")]
    {:status 302
     :headers {"Location" location}
     :body ""}))

(defn- redirect
  [location]
  {:status 302
   :headers {"Location" location}
   :body ""})

(defn- query-suffix
  [req]
  (when-let [query (:query-string req)]
    (when (seq query) (str "?" query))))

(defn handle-tap-to-talk
  "Stable product alias for the reducer-owned tap-to-talk surface. The query
   string is carried byte-for-byte so seat and mobile host parameters survive."
  [req]
  (redirect (str "/bridge3-new" (query-suffix req))))

(defn handle-hf
  "Stable product alias for the proven Bridge 4 hands-free surface."
  [req]
  (redirect (str "/bridge4" (query-suffix req))))

(def director-seat
  "The isolated mailbox seat owned by the Director SSE integration. It is not
   configurable from the request: a caller must never be able to turn the
   /code-director surface into an accidental bridge-cos submission path."
  "code-director-sse")

(defn director-request
  "Force the isolated Director seat into a request before the unchanged Bridge
   4 page handler sees it. Both key shapes are set because Ring middleware and
   direct handler tests do not always normalize params identically."
  [request]
  (-> request
      (assoc-in [:params :seat] director-seat)
      (assoc-in [:params "seat"] director-seat)
      (assoc-in [:query-params :seat] director-seat)
      (assoc-in [:query-params "seat"] director-seat)))

;; --- TASK 2: GET /api/release-manifest — D9 honest self-identity ---------

(def ^:private incomplete-fields
  "D9 lists these as part of a full release manifest; this endpoint cannot
   yet attest any of them (no signing/build pipeline plumbs them through to
   the running artifact). Naming the gap is the conformance move, not a
   silent omission."
  ["container digest"
   "dependency lockfile hashes"
   "test-result ids"
   "EAS build ID + artifact digest"
   "signing identity"
   "config hash"
   "clean-commit + tree hash"
   ;; Added on sol's pre-deploy review (2026-07-26, F4): the list above stopped
   ;; short of D9's own enumeration (ARCHITECTURE.md §D9), and a
   ;; "here is what I cannot attest" list that is itself incomplete is the
   ;; failure mode it was written against — it reads as a full accounting while
   ;; four required fields are simply absent from both halves of the manifest.
   "toolchain versions (JDK/Clojure CLI/Node/Xcode as BUILT, not as running)"
   "native runtime fingerprint"
   "protocol hash"
   "bundle hash (content-addressed web bundle)"])

(defn- namespaced-key->json-key
  "clojure.data.json's default key-fn drops a keyword's namespace (:a/b -> \"b\"),
   which would silently collide/confuse fields like :build/sha vs :protocol/version.
   Render the full `ns/name` (via symbol) so every key on the wire is unambiguous."
  [k]
  (if (keyword? k) (str (symbol k)) (str k)))

(defn handle-release-manifest
  "GET /api/release-manifest (D9). JSON of everything the server can
   HONESTLY report about its own running identity. Any field the server
   cannot attest reports \"unavailable\" rather than a fabricated value
   (S12/S13). Stays behind the default auth gate — not added to any public
   route list."
  [_req]
  (let [build-sha (resource-line "build-sha.txt")
        build-time (resource-line "build-time.txt")
        proto (protocol-version)]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"
               "Cache-Control" "no-store"}
     :body (mjson/write
             {:service/name "marvin-voice-remote"
              :build/sha (or build-sha "unavailable")
              :build/time (or build-time "unavailable")
              :runtime/clojure-version (clojure-version)
              :runtime/jvm-version (System/getProperty "java.version")
              :reducer/policy-version (:policy/version policy/current)
              :protocol/version (or proto "unavailable")
              :manifest/incomplete incomplete-fields}
             :key-fn namespaced-key->json-key)}))
