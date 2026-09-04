(ns marvin-voice-remote.friction-ui
  "Server-owned projection of the production voice-friction controls.

   The effective reducer policy is the authority. A request may narrow the two
   bridge4-local browser mechanisms with explicit query flags; the resulting
   map drives both the bootstrap bytes and the note rendered beside them."
  (:require
   [clojure.string :as str]
   [marvin-voice-remote.reducer.policy :as policy]))

(def ^:private enabled-values #{"1" "on" "true" "yes"})
(def ^:private disabled-values #{"0" "off" "false" "no"})

(defn- request-param [req k]
  (or (get-in req [:query-params k])
      (get-in req [:params k])
      (get-in req [:params (keyword k)])))

(defn- flag-value [default raw]
  (let [v (some-> raw str str/trim str/lower-case)]
    (cond
      (contains? enabled-values v) true
      (contains? disabled-values v) false
      :else (boolean default))))

;; INTENT: BARGE-IN-SURFACE-DEFAULT-F2
;; INTENT: LONG-TTS-PROD-DEFAULT-F4
(defn effective-state
  "Actual server-resolved friction state for a rendered surface.

   `/bridge4` (and Code Director, which reuses it) can override the safe
   process defaults per page load with `?barge=0|1` and
   `?longttsbuffer=0|1`. Reducer surfaces expose the echo mode they really run.
   Unsupported mechanisms are named explicitly rather than shown as OFF."
  ([surface] (effective-state surface {}))
  ([surface req]
   (let [cfg       (policy/effective)
         echo-mode (get-in cfg [:echo-guard :mode] :off)]
     (case surface
       :bridge4
       {:surface              surface
        :barge-in?            (flag-value (:barge-in? cfg) (request-param req "barge"))
        :long-tts-buffer?     (flag-value (:long-tts-buffer? cfg)
                                          (request-param req "longttsbuffer"))
        :echo-guard-mode      :unavailable}

       :voice-lab
       {:surface              surface
        :barge-in?            :not-applicable
        :long-tts-buffer?     :not-applicable
        :echo-guard-mode      echo-mode}

       :bridge3-new
       {:surface              surface
        :barge-in?            :not-applicable
        :long-tts-buffer?     :not-applicable
        :echo-guard-mode      echo-mode}

       {:surface              surface
        :barge-in?            :not-applicable
        :long-tts-buffer?     :not-applicable
        :echo-guard-mode      :unavailable}))))

(defn- on-off [enabled?]
  (if enabled? "ON" "OFF"))

;; INTENT: FRICTION-POLICY-NOTE-UI
(defn note-parts
  "Terse strings derived only from an `effective-state` value."
  [{:keys [barge-in? long-tts-buffer? echo-guard-mode]}]
  (cond-> []
    (boolean? long-tts-buffer?)
    (conj (str "long-reply buffer " (on-off long-tts-buffer?)
               (when long-tts-buffer? " (disable: ?longttsbuffer=0)")))

    (boolean? barge-in?)
    (conj (str "barge-in " (on-off barge-in?)
               (when barge-in? " (disable: ?barge=0)")))

    (= :off echo-guard-mode)
    (conj "echo guard OFF")

    (= :observe echo-guard-mode)
    (conj "echo guard OBSERVE only (disable: policy mode :off)")

    (= :quarantine echo-guard-mode)
    (conj "echo guard QUARANTINE (disable: policy mode :off)")

    (= :unavailable echo-guard-mode)
    (conj "echo guard unavailable on this legacy surface")))

(defn note-text [state]
  (str "Friction: " (str/join " · " (note-parts state))))

(defn note-html [state]
  (str "<div id=friction-note class=friction-note>" (note-text state) "</div>"))

(defn note-view [state]
  [:div {:id "friction-note" :class "friction-note"} (note-text state)])
