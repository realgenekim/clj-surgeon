(ns clj-surgeon.helper-extraction-fixture
  "Deterministic corpus for `helper_extraction` (selected-helper closure).

  Every file is emitted twice from ONE description: the pre-extraction source
  and the canonical post-extraction source. The oracle is therefore derived
  from the fixture's own description, never from the planner under test.

  There are no hand-built per-caller lists: the twenty-eight ordinary callers
  are generated from two templates and an index, exactly as the verb's own
  contract forbids hand-built per-caller tables in its request.

  VARIANTS. `files` takes a variant keyword. `:happy` is the only variant with
  a canonical POST; every other variant is a fixture for one typed refusal and
  renders `:post nil`, because a refusal writes no bytes and therefore has no
  canonical post-state to compare against.

    :happy                      six helpers move; source-local lowering only,
                                and `html-response` is MULTI-ARITY delegating
                                to itself by name (one owner, self-reference
                                is not a dependency)
    :private-dependency         a moved helper calls a retained `defn-`
    :retained-dependency-direct a moved helper calls a retained PUBLIC var
    :retained-dependency-chain  the same edge in the source -> C -> destination
                                -> source shape: the source requires C, and C
                                reaches a selected helper fully qualified. Its
                                require graph is ACYCLIC AND ITS ORIGINAL DOES
                                NOT LOAD -- C is compiled while the source is
                                still loading and cannot resolve the qualified
                                symbol. That is asserted, not assumed, and it
                                is why an acyclic graph is not a load proof.
                                The valid-original refusal is proved on
                                :retained-dependency-direct instead.
    :namespace-sensitive        a moved body contains `::ok`
    :ambiguous-owner            `declare` plus `defn` of one selected name
    :unsupported-binding        a caller binds the source by prefix list
    :ambiguous-reference        a bare symbol is `:refer`ed from two namespaces
    :alias-policy-exhausted     one caller binds every alias-policy entry
    :target-exists              the destination file is already on disk
    :caller-outside-scope       a supported reference under an admitted root
                                (`test`) that `scope.paths` does not authorize

  Contract of record: docs/plans/helper-closure-extraction.md revision 3 and
  docs/intent/helper-extraction/helper-extraction-specs.md."
  (:require
   [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; the four decisions the request carries

(def source-lib "acid.web.http")
(def source-file "src/acid/web/http.clj")
(def dest-lib "acid.web.response")
(def dest-file "src/acid/web/response.clj")

(def helpers
  "The six selected helpers, in the order the request names them."
  ["html-response" "see-other" "text-response"
   "plain-not-found" "json-response" "with-etag"])

(def alias-policy ["response" "resp"])

(def scope-paths ["src/**"])

(def admitted-roots
  "Revision 3 rule 4: discovery runs over the admitted roots; `scope.paths` is
  a write-authorization subset of them."
  ["src" "test"])

;; ---------------------------------------------------------------------------
;; rendering: one description, two sources

(defn- ns-form
  [namespace-name requires]
  (if (seq requires)
    (str "(ns " namespace-name "\n"
         "  (:require\n"
         (str/join "\n" (map #(str "   " %) requires))
         "))\n")
    (str "(ns " namespace-name ")\n")))

(defn- render
  "PRE and POST for one file spec. A spec with no `:requires-post` and no
  `:body-post` is invariant under the extraction and renders POST = PRE."
  [{:keys [ns requires-pre requires-post body-pre body-post]}]
  (let [pre (when body-pre (str (ns-form ns requires-pre) "\n" body-pre))]
    {:pre pre
     :post (if (or requires-post body-post)
             (str (ns-form ns (or requires-post requires-pre)) "\n"
                  (or body-post body-pre))
             pre)}))

(defn- pad
  [i]
  (format "%02d" i))

;; ---------------------------------------------------------------------------
;; the source namespace, acid.web.http

(defn- source-owners
  "The top-level forms of the source, in definition order, as a vector of
  `{:name :selected? :text}`. Variants add or amend forms; nothing else about
  the source moves, so a diff between two variants is exactly the case under
  test."
  [variant]
  (let [see-other-body (if (= variant :private-dependency)
                         (str "(defn see-other\n"
                              "  [location]\n"
                              "  {:status 303\n"
                              "   :headers {\"location\" location \"date\" (header-date)}\n"
                              "   :body \"\"})\n")
                         (str "(defn see-other\n"
                              "  [location]\n"
                              "  {:status 303 :headers {\"location\" location} :body \"\"})\n"))
        with-etag-body (case variant
                         (:retained-dependency-direct :retained-dependency-chain)
                         (str "(defn with-etag\n"
                              "  [response etag]\n"
                              "  (assoc-in response [:headers \"etag\"] (strong-etag etag)))\n")
                         (str "(defn with-etag\n"
                              "  [response etag]\n"
                              "  (assoc-in response [:headers \"etag\"] etag))\n"))
        json-body (if (= variant :namespace-sensitive)
                    (str "(defn json-response\n"
                         "  [data]\n"
                         "  (assoc (html-response (codec/encode data) \"json\")\n"
                         "         :outcome ::ok))\n")
                    (str "(defn json-response\n"
                         "  [data]\n"
                         "  (html-response (codec/encode data) \"json\"))\n"))]
    (vec
     (concat
      (when (= variant :ambiguous-owner)
        [{:name "json-response" :selected? false
          :text "(declare json-response)\n"}])
      [{:name "header-date" :selected? false
        :text (str "(defn- header-date\n"
                   "  \"Retained PRIVATE. In :private-dependency a moved helper calls it.\"\n"
                   "  []\n"
                   "  \"Thu, 01 Jan 1970 00:00:00 GMT\")\n")}
       {:name "parse-json-body" :selected? false
        :text (str "(defn parse-json-body\n"
                   "  \"Retained PUBLIC. The twenty mixed callers keep using this one.\"\n"
                   "  [request]\n"
                   "  [(codec/decode (:body request)) (header-date)])\n")}]
      (when (contains? #{:retained-dependency-direct :retained-dependency-chain} variant)
        [{:name "strong-etag" :selected? false
          :text (str "(defn strong-etag\n"
                     "  \"Retained PUBLIC, referenced by a MOVED helper: MCP-OP-HELPER-019.\"\n"
                     "  [etag]\n"
                     (if (= variant :retained-dependency-chain)
                       "  (str \"W/\" (c01/salt) \"-\" etag))\n"
                       "  (str \"W/\" etag))\n"))}])
      [{:name "text-response" :selected? true
        :text (str "(defn text-response\n"
                   "  [body]\n"
                   "  {:status 200 :headers {\"content-type\" \"text/plain\"} :body body})\n")}
       {:name "with-etag" :selected? true :text with-etag-body}
       ;; MULTI-ARITY, DELEGATING TO ITSELF BY NAME -- the real application's
       ;; html-response shape (Astra, 05:29). It is ONE owner, its
       ;; self-reference is not a dependency of any kind, and the extraction
       ;; must carry the whole arity list across byte-for-byte.
       {:name "html-response" :selected? true
        :text (str "(defn html-response\n"
                   "  ([body] (html-response body nil))\n"
                   "  ([body etag]\n"
                   "   (with-etag {:status 200\n"
                   "               :headers {\"content-type\" \"text/html\"}\n"
                   "               :body body}\n"
                   "              etag)))\n")}
       {:name "json-response" :selected? true :text json-body}
       (assoc {:name "see-other" :selected? true} :text see-other-body)
       {:name "plain-not-found" :selected? true
        :text (str "(defn plain-not-found\n"
                   "  []\n"
                   "  (text-response \"not found\"))\n")}
       {:name "handle-health" :selected? false
        :text (str "(defn handle-health\n"
                   "  \"Retained PUBLIC that CALLS a moved helper: the source-local use\n"
                   "   MCP-OP-HELPER-015 lowers through the extraction's own rewrite.\"\n"
                   "  [_request]\n"
                   "  (text-response \"ok\"))\n")
        ;; the source-local use, lowered by the extraction's own source
        ;; rewrite (MCP-OP-HELPER-015), never by a second caller change
        :text-post (str "(defn handle-health\n"
                        "  \"Retained PUBLIC that CALLS a moved helper: the source-local use\n"
                        "   MCP-OP-HELPER-015 lowers through the extraction's own rewrite.\"\n"
                        "  [_request]\n"
                        "  (response/text-response \"ok\"))\n")}]))))

(defn owner-text
  "The exact bytes of one top-level owner of the source, as the fixture
  describes it. A witness comparing the destination against this is comparing
  against the DESCRIPTION, never against the planner's own output."
  [variant owner-name]
  (some #(when (= owner-name (:name %)) (:text %)) (source-owners variant)))

(defn- source-requires
  [variant]
  (cond-> ["[acid.web.codec :as codec]"]
    (= variant :retained-dependency-chain) (conj "[acid.app.c01 :as c01]")))

(defn- source-spec
  [variant]
  (let [owners (source-owners variant)]
    {:file source-file
     :ns source-lib
     :partition :source
     :alias (when (= variant :happy) "response")
     :sites 1                            ; handle-health -> text-response
     :retained-sites 0
     :protected []
     :requires-pre (source-requires variant)
     :body-pre (str/join "\n" (map :text owners))
     :requires-post (when (= variant :happy)
                      (conj (source-requires variant)
                            (str "[" dest-lib " :as response]")))
     :body-post (when (= variant :happy)
                  (str/join "\n" (map #(or (:text-post %) (:text %))
                                      (remove :selected? owners))))}))

(defn- destination-spec
  "The canonical destination. Rendered for :happy only; every other variant
  refuses and therefore creates no destination at all."
  [variant]
  (when (= variant :happy)
    (let [owners (filter :selected? (source-owners variant))]
      {:file dest-file
       :ns dest-lib
       :partition :destination
       :alias nil
       :sites 0
       :retained-sites 0
       :protected []
       :requires-pre nil                 ; it does not exist before the write
       :body-pre nil
       :requires-post ["[acid.web.codec :as codec]"]
       :body-post (str/join "\n" (map :text owners))})))

;; ---------------------------------------------------------------------------
;; the generated callers

(defn- moved-only-spec
  "A caller whose every use of the source is a selected helper: the require is
  REPLACED. Three sites."
  [i {:keys [protected]}]
  (let [n (pad i)]
    {:file (str "src/acid/app/m" n ".clj")
     :ns (str "acid.app.m" n)
     :partition :moved-only
     :alias "response"
     :sites 3
     :retained-sites 0
     :protected (vec protected)
     :requires-pre [(str "[" source-lib " :as http]")]
     :requires-post [(str "[" dest-lib " :as response]")]
     :body-pre
     (str (when protected
            (str ";; see http/json-response for the envelope shape\n\n"))
          "(defn render-" n "\n"
          "  [body]\n"
          "  (http/html-response body \"e" n "\"))\n"
          "\n"
          "(defn redirect-" n "\n"
          "  [to]\n"
          "  (http/see-other to))\n"
          "\n"
          "(defn missing-" n "\n"
          "  []\n"
          (when protected "  ;; label kept for the log: \"http/json-response\"\n")
          "  (http/plain-not-found))\n")
     :body-post
     (str (when protected
            (str ";; see http/json-response for the envelope shape\n\n"))
          "(defn render-" n "\n"
          "  [body]\n"
          "  (response/html-response body \"e" n "\"))\n"
          "\n"
          "(defn redirect-" n "\n"
          "  [to]\n"
          "  (response/see-other to))\n"
          "\n"
          "(defn missing-" n "\n"
          "  []\n"
          (when protected "  ;; label kept for the log: \"http/json-response\"\n")
          "  (response/plain-not-found))\n")}))

(defn- mixed-spec
  "A caller that uses BOTH a selected helper and the retained
  `parse-json-body`: the old require is RETAINED and one new require is added.
  Two moved sites, one retained site."
  [i {:keys [protected extra-body]}]
  (let [n (pad i)]
    {:file (str "src/acid/app/x" n ".clj")
     :ns (str "acid.app.x" n)
     :partition :mixed
     :alias "response"
     :sites 2
     :retained-sites 1
     :protected (vec protected)
     :requires-pre [(str "[" source-lib " :as http]")]
     :requires-post [(str "[" source-lib " :as http]")
                     (str "[" dest-lib " :as response]")]
     :body-pre
     (str (or extra-body "")
          "(defn encode-" n "\n"
          (when protected "  \"Encode through http/json-response.\"\n")
          "  [request]\n"
          (when protected "  #_(http/json-response {:disabled true})\n")
          "  (http/json-response (http/parse-json-body request)))\n"
          "\n"
          "(defn text-" n "\n"
          "  [body]\n"
          "  (http/text-response body))\n")
     :body-post
     (str (or extra-body "")
          "(defn encode-" n "\n"
          (when protected "  \"Encode through http/json-response.\"\n")
          "  [request]\n"
          (when protected "  #_(http/json-response {:disabled true})\n")
          "  (response/json-response (http/parse-json-body request)))\n"
          "\n"
          "(defn text-" n "\n"
          "  [body]\n"
          "  (response/text-response body))\n")}))

(defn- untouched-spec
  "Requires the source, uses only a retained helper: not in the footprint and
  gains no mutation authority (revision 3 rule 4)."
  [i {:keys [protected]}]
  (let [n (pad i)]
    {:file (str "src/acid/app/u" n ".clj")
     :ns (str "acid.app.u" n)
     :partition :untouched
     :alias nil
     :sites 0
     :retained-sites 1
     :protected (vec protected)
     :requires-pre [(str "[" source-lib " :as http]")]
     :body-pre
     (str (when protected ";; nothing here calls http/json-response\n\n")
          "(defn decode-" n "\n"
          "  [request]\n"
          "  (http/parse-json-body request))\n")}))

;; ---------------------------------------------------------------------------
;; the hand-written special callers (one shape each, none of them a table)

(def ^:private refer-caller
  "m02: binds a selected helper with :refer. Moved-only."
  {:file "src/acid/app/m02.clj"
   :ns "acid.app.m02"
   :partition :moved-only
   :alias "response"
   :sites 2
   :retained-sites 0
   :protected []
   :requires-pre [(str "[" source-lib " :refer [json-response see-other]]")]
   :requires-post [(str "[" dest-lib " :as response]")]
   :body-pre
   (str "(defn ok\n"
        "  [data]\n"
        "  (json-response data))\n"
        "\n"
        "(defn away\n"
        "  [to]\n"
        "  (see-other to))\n")
   :body-post
   (str "(defn ok\n"
        "  [data]\n"
        "  (response/json-response data))\n"
        "\n"
        "(defn away\n"
        "  [to]\n"
        "  (response/see-other to))\n")})

(def ^:private first-class-caller
  "m03: uses a selected helper as a VALUE, not at head position
  (MCP-OP-HELPER-019's discovery half: references, not only calls)."
  {:file "src/acid/app/m03.clj"
   :ns "acid.app.m03"
   :partition :moved-only
   :alias "response"
   :sites 2
   :retained-sites 0
   :protected []
   :requires-pre [(str "[" source-lib " :as http]")]
   :requires-post [(str "[" dest-lib " :as response]")]
   :body-pre
   (str "(defn render-all\n"
        "  [bodies]\n"
        "  (mapv http/plain-not-found bodies))\n"
        "\n"
        "(defn away\n"
        "  [to]\n"
        "  (http/see-other to))\n")
   :body-post
   (str "(defn render-all\n"
        "  [bodies]\n"
        "  (mapv response/plain-not-found bodies))\n"
        "\n"
        "(defn away\n"
        "  [to]\n"
        "  (response/see-other to))\n")})

(def ^:private alias-collision-caller
  "m04: binds a local named `response`, so the first alias-policy entry
  collides and the second (`resp`) is chosen (MCP-OP-HELPER-007)."
  {:file "src/acid/app/m04.clj"
   :ns "acid.app.m04"
   :partition :moved-only
   :alias "resp"
   :sites 2
   :retained-sites 0
   :collided ["response"]
   :protected ["(assoc response :wrapped true)"]
   :requires-pre [(str "[" source-lib " :as http]")]
   :requires-post [(str "[" dest-lib " :as resp]")]
   :body-pre
   (str "(defn wrap\n"
        "  [body]\n"
        "  (let [response (http/html-response body \"e04\")]\n"
        "    (assoc response :wrapped true)))\n"
        "\n"
        "(defn away\n"
        "  [to]\n"
        "  (http/see-other to))\n")
   :body-post
   (str "(defn wrap\n"
        "  [body]\n"
        "  (let [response (resp/html-response body \"e04\")]\n"
        "    (assoc response :wrapped true)))\n"
        "\n"
        "(defn away\n"
        "  [to]\n"
        "  (resp/see-other to))\n")})

(def ^:private qualified-only-caller
  "fq01: uses a selected helper fully qualified and does NOT require the
  source. Its own partition class; NO ALIAS is chosen.

  Its ORIGINAL is valid but only bootstrap-loads: nothing here requires
  `acid.web.http`, so the qualified symbol resolves at call time only because
  something else in the program loaded the source first (`bootstrap-order`).
  Astra's 05:07Z correction: the rewritten qualified DESTINATION symbol must
  have a sound load path of its own, so the plan ADDS a plain require of the
  destination. The POST therefore never carries a bare qualified symbol with
  no require, and the caller loads standalone after the write."
  {:file "src/acid/app/fq01.clj"
   :ns "acid.app.fq01"
   :partition :qualified-only
   :alias nil
   :sites 1
   :retained-sites 0
   :protected []
   :requires-pre nil
   :body-pre
   (str "(defn direct\n"
        "  [data]\n"
        "  (" source-lib "/json-response data))\n")
   :requires-post [dest-lib]
   :body-post
   (str "(defn direct\n"
        "  [data]\n"
        "  (" dest-lib "/json-response data))\n")})

(def ^:private support-specs
  "Namespaces the corpus needs that are not callers of the source."
  [{:file "src/acid/web/codec.clj"
    :ns "acid.web.codec"
    :partition :support
    :alias nil :sites 0 :retained-sites 0 :protected []
    :requires-pre nil
    :body-pre (str "(defn encode\n  [data]\n  (pr-str data))\n"
                   "\n"
                   "(defn decode\n  [text]\n  {:decoded text})\n")}])

;; ---------------------------------------------------------------------------
;; the per-variant refusal callers

(def ^:private prefix-list-caller
  "Binds the source through a prefix list: grammar v1 does not close over."
  {:file "src/acid/app/pl01.clj"
   :ns "acid.app.pl01"
   :partition :moved-only
   :alias "response" :sites 1 :retained-sites 0 :protected []
   :requires-pre ["(acid.web [http :as http])"]
   :body-pre (str "(defn one\n  [body]\n  (http/html-response body \"pl\"))\n")})

(def ^:private ambiguous-reference-caller
  "A bare `json-response` :refer'ed from two namespaces: the symbol cannot be
  resolved to one owner."
  {:file "src/acid/app/ar01.clj"
   :ns "acid.app.ar01"
   :partition :moved-only
   :alias "response" :sites 1 :retained-sites 0 :protected []
   :requires-pre [(str "[" source-lib " :refer [json-response]]")
                  "[acid.web.mirror :refer [json-response]]"]
   :body-pre (str "(defn one\n  [data]\n  (json-response data))\n")})

(def ^:private mirror-support-spec
  {:file "src/acid/web/mirror.clj"
   :ns "acid.web.mirror"
   :partition :support
   :alias nil :sites 0 :retained-sites 0 :protected []
   :requires-pre nil
   :body-pre (str "(defn json-response\n  [data]\n  {:mirror data})\n")})

(def ^:private alias-exhausted-caller
  "Binds every entry of the alias policy, so no alias is available."
  {:file "src/acid/app/ae01.clj"
   :ns "acid.app.ae01"
   :partition :moved-only
   :alias nil :sites 1 :retained-sites 0
   :collided ["response" "resp"]
   :protected []
   :requires-pre [(str "[" source-lib " :as http]")
                  "[acid.web.alt :as response]"
                  "[acid.web.alt2 :as resp]"]
   :body-pre (str "(defn one\n  [body]\n  (http/html-response body \"ae\"))\n")})

(def ^:private alias-exhausted-support-specs
  [{:file "src/acid/web/alt.clj" :ns "acid.web.alt" :partition :support
    :alias nil :sites 0 :retained-sites 0 :protected []
    :requires-pre nil :body-pre "(defn other\n  [x]\n  x)\n"}
   {:file "src/acid/web/alt2.clj" :ns "acid.web.alt2" :partition :support
    :alias nil :sites 0 :retained-sites 0 :protected []
    :requires-pre nil :body-pre "(defn other\n  [x]\n  x)\n"}])

(def ^:private outside-scope-caller
  "Under the admitted root `test`, which `scope.paths` (src/**) does not
  authorize for writing: MCP-OP-HELPER-021."
  {:file "test/acid/app/o01_test.clj"
   :ns "acid.app.o01-test"
   :partition :moved-only
   :alias "response" :sites 1 :retained-sites 0 :protected []
   :requires-pre [(str "[" source-lib " :as http]")]
   :body-pre (str "(defn check\n  [body]\n  (http/html-response body \"o01\"))\n")})

(def ^:private occupied-destination-spec
  "The destination path already holds a namespace: MCP-OP-HELPER target-exists."
  {:file dest-file
   :ns dest-lib
   :partition :support
   :alias nil :sites 0 :retained-sites 0 :protected []
   :requires-pre nil
   :body-pre (str "(defn already-here\n  [x]\n  x)\n")})

(def ^:private chain-third-caller
  "C, the third namespace of the :retained-dependency-chain shape.

  THE ORIGINAL IS VALID AND LOADS. The source requires C, and C reaches a
  selected helper FULLY QUALIFIED with no require of the source, so the PRE
  require graph carries no edge from C back to the source and no static cycle
  exists (`static-require-graph` proves it rather than asserting it).

  After the write C would gain a require of the destination (rule 1 of the
  05:07Z correction: a qualified-only caller gets a sound load path), and the
  destination would need the source's retained `strong-etag` -- giving
  source -> C -> destination -> source. The plan refuses at the moved ->
  retained-public edge before that arrangement is reached."
  {:file "src/acid/app/c01.clj"
   :ns "acid.app.c01"
   :partition :qualified-only
   :alias nil :sites 1 :retained-sites 0 :protected []
   :requires-pre nil
   :body-pre (str "(defn salt\n"
                  "  []\n"
                  "  \"c01\")\n"
                  "\n"
                  "(defn direct\n"
                  "  [data]\n"
                  "  (" source-lib "/json-response data))\n")})

;; ---------------------------------------------------------------------------
;; the ordinary caller population

(def ^:private generated-moved-only
  (into [(moved-only-spec 1 {:protected [";; see http/json-response for the envelope shape"
                                         "\"http/json-response\""]})]
        (map #(moved-only-spec % {}))
        [5 6 7 8]))

(def ^:private generated-mixed
  (into [(mixed-spec 1 {:protected ["\"Encode through http/json-response.\""
                                    "#_(http/json-response {:disabled true})"]})
         (mixed-spec 2 {:protected ["\"http/json-response is the envelope\""]
                        :extra-body (str "(def label \"http/json-response is the envelope\")\n\n")})]
        (map #(mixed-spec % {}))
        (range 3 21)))

(def ^:private generated-untouched
  (into [(untouched-spec 1 {:protected [";; nothing here calls http/json-response"]})]
        (map #(untouched-spec % {}))
        [2 3]))

(def ^:private caller-specs
  "The thirty-two callers of the happy corpus: 8 moved-only, 20 mixed,
  1 qualified-only, 3 untouched."
  (vec (sort-by :file
                (concat generated-moved-only
                        [refer-caller first-class-caller alias-collision-caller]
                        generated-mixed
                        [qualified-only-caller]
                        generated-untouched))))

;; ---------------------------------------------------------------------------
;; assembly

(defn- variant-specs
  "The whole corpus for one variant, in a stable order: source, destination,
  supports, then callers by path. Only :happy carries a canonical POST."
  [variant]
  (let [extra (case variant
                :unsupported-binding [prefix-list-caller]
                :ambiguous-reference [mirror-support-spec ambiguous-reference-caller]
                :alias-policy-exhausted (conj (vec alias-exhausted-support-specs)
                                              alias-exhausted-caller)
                :caller-outside-scope [outside-scope-caller]
                :target-exists [occupied-destination-spec]
                :retained-dependency-chain [chain-third-caller]
                [])
        base (concat [(source-spec variant)]
                     (when-let [d (destination-spec variant)] [d])
                     support-specs
                     caller-specs
                     extra)]
    (vec (sort-by :file base))))

(defn files
  "Ordered entries `{:file :pre :post :partition :alias :sites}` for `variant`.

  `:post` is nil for every refusal variant, because a refusal writes nothing.
  `:retained-sites`, `:protected` and `:collided` are carried alongside."
  [variant]
  (mapv (fn [spec]
          (let [{:keys [pre post]} (render spec)
                happy? (= variant :happy)]
            {:file (:file spec)
             :pre pre
             :post (when happy? post)
             :partition (:partition spec)
             :alias (:alias spec)
             :sites (:sites spec)
             :retained-sites (:retained-sites spec)
             :protected (vec (:protected spec))
             :collided (vec (:collided spec))}))
        (variant-specs variant)))

(defn sources
  "The planner's input shape: `[{:file :source}]` for every file of `variant`
  that exists BEFORE the write. The destination is excluded from :happy,
  because it does not exist yet."
  [variant]
  (into []
        (comp (remove #(and (= :happy variant) (= dest-file (:file %))))
              (map (fn [{:keys [file pre]}] {:file file :source pre})))
        (files variant)))

(defn in-scope?
  "True when `path` is authorized for writing by `scope-paths` (v1: src/**)."
  [path]
  (str/starts-with? path "src/"))

;; ---------------------------------------------------------------------------
;; the ORIGINAL is valid: a require graph computed from the fixture's own
;; description, so a witness can show a tree loads before it asks the planner
;; anything. Nothing here claims anything about trees the fixture does not
;; describe.

(defn- libspec-ns
  "The namespace a rendered libspec string names, or nil for grammar this
  fixture deliberately leaves unsupported (a prefix list names no single ns)."
  [libspec]
  (let [trimmed (str/trim libspec)]
    (when-not (str/starts-with? trimmed "(")
      (first (str/split (str/replace trimmed #"^\[|\]$" "") #"\s+")))))

(defn static-require-graph
  "`{namespace #{required-namespace}}` for one `variant` at one `phase`
  (`:pre` or `:post`), read off the rendered sources."
  [variant phase]
  (into {}
        (keep (fn [{:keys [ns requires-pre requires-post]}]
                (let [requires (if (= phase :post)
                                 (or requires-post requires-pre)
                                 requires-pre)]
                  (when ns [ns (into #{} (keep libspec-ns) requires)]))))
        (variant-specs variant)))

(defn cyclic-namespaces
  "Every namespace of `graph` that can reach itself. Empty means the tree has
  no static require cycle and therefore loads."
  [graph]
  (letfn [(reaches? [start target seen]
            (boolean
             (some (fn [n]
                     (or (= n target)
                         (and (not (contains? seen n))
                              (reaches? n target (conj seen n)))))
                   (get graph start))))]
    (into (sorted-set) (filter #(reaches? % % #{%})) (keys graph))))

(def caller-partitions
  "The four caller partition classes of revision 3, in receipt order."
  [:moved-only :mixed :qualified-only :untouched])

(def canonical-partition
  "Derived from the specs, never typed: the caller partition the receipt must
  report for the happy corpus."
  (into {}
        (map (fn [class]
               [class (count (filter #(= class (:partition %)) caller-specs))]))
        caller-partitions))

(def canonical-receipt-partition
  "The same partition under the receipt's own snake_case spelling."
  (into {} (map (fn [[class n]]
                  [(keyword (str/replace (name class) "-" "_")) n]))
        canonical-partition))

(def canonical-counts
  "Every O(1) number the happy receipt must carry, derived from the specs."
  (let [entries (files :happy)
        footprint (filter #(contains? #{:moved-only :mixed :qualified-only :source}
                                      (:partition %))
                          entries)]
    {:helpers (count helpers)
     :source-retired (count helpers)
     :destination-created true
     ;; the source is counted ONCE in the footprint (MCP-OP-HELPER-015)
     :caller-files (count footprint)
     :partition canonical-partition
     :sites (reduce + 0 (map :sites entries))
     :retained-sites (reduce + 0 (map :retained-sites entries))
     :alias-histogram (into (sorted-map)
                            (frequencies (keep :alias
                                               (filter #(contains? #{:moved-only :mixed}
                                                                   (:partition %))
                                                       entries))))}))

(defn sha256
  [text]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %)
                    (.digest digest (.getBytes ^String text "UTF-8"))))))

(defn protected-regions
  "`{file [{:region :sha256}]}` for every decoy byte range that must survive
  the extraction unchanged."
  [variant]
  (into (sorted-map)
        (keep (fn [{:keys [file protected]}]
                (when (seq protected)
                  [file (mapv (fn [region] {:region region :sha256 (sha256 region)})
                              protected)])))
        (files variant)))

(def protected-region-count
  (reduce + 0 (map count (vals (protected-regions :happy)))))

(defn request
  "The closed-field request for `variant`. `expect` is OPTIONAL
  (MCP-OP-HELPER-017) and is omitted unless `overrides` supplies it."
  ([] (request {}))
  ([overrides]
   (merge {:op "helper_extraction"
           :workspace_root "/workspace"
           :from {:file source-file}
           :helpers helpers
           :to {:lib dest-lib :alias_policy alias-policy}
           :scope {:paths scope-paths}
           :verification {:profile "helper-proof"}}
          overrides)))
