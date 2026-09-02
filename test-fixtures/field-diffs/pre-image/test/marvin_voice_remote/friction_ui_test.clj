(ns marvin-voice-remote.friction-ui-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [marvin-voice-remote.channel :as channel]
   [marvin-voice-remote.friction-ui :as friction-ui]
   [marvin-voice-remote.reducer.echo-guard :as echo-guard]
   [marvin-voice-remote.reducer.policy :as policy]))

;; INTENT-TEST: BARGE-IN-SURFACE-DEFAULT-F2
;; INTENT-TEST: LONG-TTS-PROD-DEFAULT-F4
;; INTENT-TEST: ECHO-OBSERVE-PROD-DEFAULT-F5
(deftest production-friction-policy-defaults-are-safe-and-reversible
  (testing "the production policy enables only the two safe first rungs"
    (is (= 3 (:policy/version policy/current)))
    (is (false? (:barge-in? policy/current)))
    (is (true? (:long-tts-buffer? policy/current)))
    (is (= :observe (get-in policy/current [:echo-guard :mode])))
    (is (= :off (:mode echo-guard/default-policy))
        "the standalone classifier remains inert; production chooses observe"))
  (testing "the bridge4 request can narrow either browser mechanism per surface"
    (with-redefs [policy/effective (constantly policy/current)]
      (is (= {:barge-in? false :long-tts-buffer? true}
             (select-keys (friction-ui/effective-state :bridge4 {})
                          [:barge-in? :long-tts-buffer?])))
      (is (= {:barge-in? true :long-tts-buffer? false}
             (select-keys
               (friction-ui/effective-state
                 :bridge4
                 {:query-params {"barge" "1" "longttsbuffer" "0"}})
               [:barge-in? :long-tts-buffer?])))))
  (testing "an explicit page OFF wins even if the process barge gate is ON"
    (with-redefs [policy/effective (constantly (assoc policy/current :barge-in? true))]
      (is (true? (:barge-in? (friction-ui/effective-state :bridge4 {}))))
      (is (false? (:barge-in?
                    (friction-ui/effective-state
                      :bridge4 {:query-params {"barge" "0"}})))))))

;; INTENT-TEST: FRICTION-POLICY-NOTE-UI
(deftest friction-ui-note-reflects-effective-state-including-off
  (with-redefs [policy/effective (constantly policy/current)]
    (testing "the bridge4 default states the real enablement and the legacy gap"
      (let [text (friction-ui/note-text (friction-ui/effective-state :bridge4))]
        (is (str/includes? text "long-reply buffer ON (disable: ?longttsbuffer=0)"))
        (is (str/includes? text "barge-in OFF"))
        (is (str/includes? text "echo guard unavailable on this legacy surface"))))
    (testing "request-level inversion changes the server-rendered words"
      (let [text (friction-ui/note-text
                   (friction-ui/effective-state
                     :bridge4
                     {:query-params {"barge" "1" "longttsbuffer" "0"}}))]
        (is (str/includes? text "long-reply buffer OFF"))
        (is (str/includes? text "barge-in ON (disable: ?barge=0)"))))
    (testing "reducer surfaces report observe honestly"
      (let [text (friction-ui/note-text (friction-ui/effective-state :voice-lab))]
        (is (str/includes? text "echo guard OBSERVE only"))
        (is (not (str/includes? text "long-reply buffer")))
        (is (not (str/includes? text "barge-in")))))))

;; INTENT-TEST: BARGE-IN-SURFACE-DEFAULT-F2
;; INTENT-TEST: LONG-TTS-PROD-DEFAULT-F4
;; INTENT-TEST: ECHO-OBSERVE-PROD-DEFAULT-F5
;; INTENT-TEST: FRICTION-POLICY-NOTE-UI
(deftest served-friction-notes-match-the-bootstrapped-state
  (with-redefs [policy/effective (constantly policy/current)]
    (testing "bridge4's default note and executable bootstrap agree"
      (let [html (:body (channel/handle-bridge4-page {:params {"seat" "bridge"}}))]
        (is (str/includes? html "id=friction-note"))
        (is (str/includes? html "long-reply buffer ON"))
        (is (str/includes? html "barge-in OFF"))
        (is (str/includes? html "longTtsBuffer:true"))
        (is (str/includes? html "barge:false"))))
    (testing "bridge4's explicit OFF/ON request changes both note and bootstrap"
      (let [html (:body
                   (channel/handle-bridge4-page
                     {:params {"seat" "bridge"}
                      :query-params {"longttsbuffer" "0" "barge" "1"}}))]
        (is (str/includes? html "long-reply buffer OFF"))
        (is (str/includes? html "barge-in ON (disable: ?barge=0)"))
        (is (str/includes? html "longTtsBuffer:false"))
        (is (str/includes? html "barge:true"))))
    (testing "Code Director inherits the same server-resolved bridge4 controls"
      (let [html (:body
                   (channel/handle-code-director-page
                     {:params {"mode" "desk"}
                      :query-params {"longttsbuffer" "0" "barge" "0"}}))]
        (is (str/includes? html "id=friction-note"))
        (is (str/includes? html "longTtsBuffer:false"))
        (is (str/includes? html "barge:false"))))
    (testing "the reducer lab surface reports observe, not quarantine"
      (let [html (:body (channel/handle-voice-lab-page {}))]
        (is (str/includes? html "id=friction-note"))
        (is (str/includes? html "echo guard OBSERVE only"))
        (is (not (str/includes? html "echo guard QUARANTINE")))))))
