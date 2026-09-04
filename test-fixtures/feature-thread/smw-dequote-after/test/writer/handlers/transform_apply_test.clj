(ns writer.handlers.transform-apply-test
  "Single-flight regression coverage for Transform Apply in a Book node."
  (:require
   [clojure.data.json :as json]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [closed-record.core :as cr]
   [writer.handlers.transform :as transform]
   [writer.sse :as sse]
   [writer.state :as state]))

(defn- isolate-state [f]
  (let [original (cr/to-map-recursive @state/app-state)]
    (binding [state/*io-enabled* false]
      (try (f)
           (finally
             (reset! state/app-state (cr/closed-record-recursive original)))))))

(use-fixtures :each isolate-state)

(defn- json-request [m]
  {:body (java.io.ByteArrayInputStream.
           (.getBytes (json/write-str m) "UTF-8"))})

(deftest ordinary-proposal-refuses-apply-after-active-node-switch
  (testing "a selection proposal opened on A cannot mutate B even when B contains the selection"
    (let [project-idx (get-in @state/app-state [:book-workshop :active-project-idx])
          project-id "transform-project"
          node-a (state/make-book-node {:id "node-a" :title "Node A" :level 0
                                        :draft "A contains TARGET text"})
          node-b (state/make-book-node {:id "node-b" :title "Node B" :level 0
                                        :draft "B also contains TARGET text"})]
      (swap! state/app-state
             (fn [st]
               (-> st
                   (assoc :draft (:draft node-a) :state-version 40)
                   (assoc-in [:book-workshop :projects project-idx :id] project-id)
                   (assoc-in [:book-workshop :projects project-idx :nodes] [node-a node-b])
                   (assoc-in [:book-workshop :editing-node]
                             {:project-id project-id :node-id (:id node-a)
                              :project-idx project-idx :node-idx 0}))))
      (state/transform-open! "TARGET")
      (state/transform-set-options! [{:text "REPLACEMENT"}])
      (is (= "book-node:node-a"
             (get-in @state/app-state [:transform :base-editor-sync-key])))
      (is (= 40 (get-in @state/app-state [:transform :base-state-version])))
      (swap! state/app-state
             state/transition-tx
             (fn [st]
               (-> st
                   (assoc :draft (:draft node-b))
                   (assoc-in [:book-workshop :editing-node]
                             {:project-id project-id :node-id (:id node-b)
                              :project-idx project-idx :node-idx 1}))))
      (with-redefs [sse/push-transform-modal! (constantly nil)
                    sse/push-book-trees! (constantly nil)
                    sse/push-notification! (constantly nil)
                    sse/push-draft-sync-conflict! (constantly nil)
                    sse/push-notify! (constantly nil)
                    state/log-event! (constantly nil)]
        (let [before-b (get-in @state/app-state
                               [:book-workshop :projects project-idx :nodes 1 :draft])
              sync {:draft before-b :context (:context @state/app-state)
                    :leftovers (:leftovers @state/app-state) :cursor-pos 0
                    :state-version (:state-version @state/app-state)
                    :editor-sync-key (state/editor-sync-key @state/app-state)}
              response (transform/handle-apply (json-request {:sync sync}))
              frame (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= "editor-conflict" (:transform-status frame)))
          (is (= before-b (:draft frame)))
          (is (= before-b
                 (get-in @state/app-state
                         [:book-workshop :projects project-idx :nodes 1 :draft])))
          (is (= "A contains TARGET text"
                 (get-in @state/app-state
                         [:book-workshop :projects project-idx :nodes 0 :draft]))))))))

(deftest apply-folds-visible-snapshot-and-returns-authoritative-frame
  (testing "a late browser snapshot, node transform, and returned revision are one command"
    (let [project-idx (get-in @state/app-state [:book-workshop :active-project-idx])
          project-id "transform-project"
          node-id "transform-node"
          node (state/make-book-node {:id node-id :title "Transform me" :level 0
                                      :draft "server older TARGET text"})]
      (swap! state/app-state
             (fn [st]
               (-> st
                   (assoc :draft "server older TARGET text"
                          :context "ctx" :leftovers "left" :state-version 40)
                   (assoc-in [:book-workshop :projects project-idx :id] project-id)
                   (assoc-in [:book-workshop :projects project-idx :nodes] [node])
                   (assoc-in [:book-workshop :editing-node]
                             {:project-id project-id :node-id node-id
                              :project-idx project-idx :node-idx 0}))))
      (state/transform-open! "TARGET")
      (swap! state/app-state update :transform assoc :instruction "improve")
      (state/transform-set-options! [{:text "REPLACEMENT"}])
      (with-redefs [sse/push-transform-modal! (constantly nil)
                    sse/push-book-trees! (constantly nil)
                    sse/push-notification! (constantly nil)
                    sse/push-draft-sync-conflict! (constantly nil)
                    sse/push-notify! (constantly nil)
                    state/log-event! (constantly nil)]
        (let [sync {:draft "browser latest TARGET text"
                    :context "ctx" :leftovers "left" :cursor-pos 0
                    :state-version 40 :editor-sync-key (state/editor-sync-key @state/app-state)}
              response (transform/handle-apply (json-request {:sync sync}))
              frame (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= "applied" (:transform-status frame)))
          (is (= "browser latest REPLACEMENT text" (:draft frame)))
          (is (= {:start 15 :end 26} (:transform-range frame)))
          (is (= 41 (:state-version frame)))
          (is (= "browser latest REPLACEMENT text"
                 (get-in @state/app-state
                         [:book-workshop :projects project-idx :nodes 0 :draft])))
          (is (= (:editor-sync-key frame) (state/editor-sync-key @state/app-state))))))))

(deftest exact-proposal-is-non-mutating-until-browser-apply
  (let [before "browser-owned complete draft"
        after "approved replacement draft"]
    (state/set-draft! before)
    (let [key (state/editor-sync-key @state/app-state)
          version (:state-version @state/app-state)]
      (with-redefs [sse/push-transform-modal! (constantly nil)
                    sse/push-book-trees! (constantly nil)
                    state/log-event! (constantly nil)]
        (let [response (transform/handle-propose-exact
                         (json-request {:before before :after after
                                        :editor-sync-key key :state-version version
                                        :why "clearer"}))
              proposal-id (:proposal-id (json/read-str (:body response) :key-fn keyword))]
          (is (= 201 (:status response)))
          (is (= before (:draft @state/app-state)) "opening never changes prose")
          (is (= :pending-browser (get-in @state/app-state [:transform :proposal-status])))
          (is (= 204 (:status (transform/handle-proposal-visible
                                (json-request {:proposal-id proposal-id})))))
          (is (= :visible (get-in @state/app-state [:transform :proposal-status])))
          (let [sync {:draft before :context (:context @state/app-state)
                      :leftovers (:leftovers @state/app-state) :cursor-pos 0
                      :state-version version :editor-sync-key key}
                applied (transform/handle-apply
                          (json-request {:sync sync :edited-hunk after}))
                frame (json/read-str (:body applied) :key-fn keyword)]
            (is (= 200 (:status applied)))
            (is (= "applied" (:transform-status frame)))
            (is (= after (:draft frame)))
            (is (= {:start 0 :end (count after)} (:transform-range frame)))))))))

(deftest large-whole-value-proposal-opens-in-before-after-mode
  (let [original-mode @state/selected-diff-mode
        before (apply str (repeat 1200 "a"))
        after (apply str (repeat 1200 "b"))]
    (try
      (state/set-draft! before)
      (let [key (state/editor-sync-key @state/app-state)
            version (:state-version @state/app-state)]
        (with-redefs [sse/push-transform-modal! (constantly nil)
                      state/log-event! (constantly nil)]
          (is (= 201
                 (:status
                   (transform/handle-propose-exact
                     (json-request {:before before :after after
                                    :editor-sync-key key :state-version version
                                    :why "whole document replacement"})))))
          (is (= :before-after @state/selected-diff-mode))))
      (finally
        (state/set-selected-diff-mode! original-mode)))))

(deftest exact-proposal-refuses-to-overwrite-newer-browser-text
  (let [before "original complete draft"
        after "proposed complete draft"]
    (state/set-draft! before)
    (let [key (state/editor-sync-key @state/app-state)
          version (:state-version @state/app-state)]
      (with-redefs [sse/push-transform-modal! (constantly nil)
                    sse/push-book-trees! (constantly nil)
                    sse/push-notification! (constantly nil)
                    state/log-event! (constantly nil)]
        (is (= 201 (:status (transform/handle-propose-exact
                              (json-request {:before before :after after
                                             :editor-sync-key key :state-version version})))))
        (let [newer "I typed after seeing the proposal"
              sync {:draft newer :context (:context @state/app-state)
                    :leftovers (:leftovers @state/app-state) :cursor-pos 0
                    :state-version version :editor-sync-key key}
              response (transform/handle-apply
                         (json-request {:sync sync :edited-hunk after}))
              frame (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= "no-match" (:transform-status frame)))
          (is (= newer (:draft frame)) "the browser's newer prose wins"))))))

(deftest exact-proposal-applies-an-author-edited-hunk-without-losing-hidden-text
  (let [before "Opening stays.\n\nOld paragraph.\n\nEnding stays."
        suggested "Opening stays.\n\nSuggested paragraph.\n\nEnding stays."]
    (state/set-draft! before)
    (let [key (state/editor-sync-key @state/app-state)
          version (:state-version @state/app-state)]
      (with-redefs [sse/push-transform-modal! (constantly nil)
                    sse/push-book-trees! (constantly nil)
                    state/log-event! (constantly nil)]
        (is (= 201 (:status (transform/handle-propose-exact
                              (json-request {:before before :after suggested
                                             :editor-sync-key key :state-version version})))))
        (let [sync {:draft before :context (:context @state/app-state)
                    :leftovers (:leftovers @state/app-state) :cursor-pos 0
                    :state-version version :editor-sync-key key}
              response (transform/handle-apply
                         (json-request {:sync sync :edited-hunk "Gene's paragraph."}))
              frame (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= "applied" (:transform-status frame)))
          (is (= "Opening stays.\n\nGene's paragraph.\n\nEnding stays."
                 (:draft frame))))))))

(deftest exact-proposal-refuses-an-old-browser-that-omits-the-editable-value
  (let [before "original complete draft"
        after "proposed complete draft"]
    (state/set-draft! before)
    (let [key (state/editor-sync-key @state/app-state)
          version (:state-version @state/app-state)]
      (with-redefs [sse/push-transform-modal! (constantly nil)
                    sse/push-book-trees! (constantly nil)
                    sse/push-notification! (constantly nil)
                    state/log-event! (constantly nil)]
        (is (= 201 (:status (transform/handle-propose-exact
                              (json-request {:before before :after after
                                             :editor-sync-key key :state-version version})))))
        (let [sync {:draft before :context (:context @state/app-state)
                    :leftovers (:leftovers @state/app-state) :cursor-pos 0
                    :state-version version :editor-sync-key key}
              response (transform/handle-apply (json-request {:sync sync}))
              frame (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= "editable-value-required" (:transform-status frame)))
          (is (= before (:draft frame)))
          (is (true? (get-in @state/app-state [:transform :active?]))))))))

(deftest apply-event-carries-verbatim-before-after-instruction-and-thread
  (testing "transform.apply is enriched with :before/:after/:instruction/:thread (bd social-media-writer-pq3), not just lengths"
    (let [project-idx (get-in @state/app-state [:book-workshop :active-project-idx])
          project-id "transform-project"
          node-id "transform-node"
          before-text "server older TARGET text"
          node (state/make-book-node {:id node-id :title "Transform me" :level 0
                                      :draft before-text})]
      (swap! state/app-state
             (fn [st]
               (-> st
                   (assoc :draft before-text :context "ctx" :leftovers "left" :state-version 40)
                   (assoc-in [:book-workshop :projects project-idx :id] project-id)
                   (assoc-in [:book-workshop :projects project-idx :nodes] [node])
                   (assoc-in [:book-workshop :editing-node]
                             {:project-id project-id :node-id node-id
                              :project-idx project-idx :node-idx 0}))))
      (state/transform-open! "TARGET")
      (swap! state/app-state update :transform assoc
             :instruction "make it pop"
             :round-id "round-123" :thread-id "thread-123")
      (state/transform-set-options! [{:text "FIRST"}
                                     {:text "SECOND"}
                                     {:text "REPLACEMENT" :why "best"}])
      (state/transform-append-thread-entry!
        {:instruction "make it pop" :at "2026-07-14T00:00:00Z" :suggested-sha256 "abc"})
      (let [events (atom [])]
        (with-redefs [sse/push-transform-modal! (constantly nil)
                      sse/push-book-trees! (constantly nil)
                      sse/push-notification! (constantly nil)
                      sse/push-draft-sync-conflict! (constantly nil)
                      sse/push-notify! (constantly nil)
                      state/log-event! (fn [m] (swap! events conj m))]
          (let [sync {:draft before-text :context "ctx" :leftovers "left" :cursor-pos 0
                      :state-version 40 :editor-sync-key (state/editor-sync-key @state/app-state)}
                response (transform/handle-apply (json-request {:sync sync :index 2}))
                apply-event (first (filter #(= "transform.apply" (:type %)) @events))]
            (is (= 200 (:status response)))
            (is (some? apply-event) "a transform.apply event must be logged")
            (is (= "TARGET" (:before apply-event)))
            (is (= "REPLACEMENT" (:after apply-event)))
            (is (= "make it pop" (:instruction apply-event)))
            (is (= "APPLY" (:outcome apply-event)))
            (is (= "round-123" (:round-id apply-event)))
            (is (= "thread-123" (:thread-id apply-event)))
            (is (= 2 (:option-index apply-event)))
            (is (= 3 (:option-number apply-event)))
            (is (= 3 (:option-count apply-event)))
            (is (= {:text "REPLACEMENT" :why "best"} (:chosen-option apply-event)))
            (is (false? (:author-edited-proposal? apply-event)))
            (is (= 1 (count (:thread apply-event))) "the thread vector at apply time is carried")
            (is (= "make it pop" (:instruction (first (:thread apply-event)))))))))))

(deftest copied-options-remain-attributable-after-the-modal-closes
  (state/transform-open! "source passage")
  (swap! state/app-state update :transform assoc
         :round-id "round-copy" :thread-id "thread-copy")
  (state/transform-set-options! [{:text "one" :why "first"}
                                 {:text "two" :why "second"}
                                 {:text "three" :why "third"}])
  (let [events (atom [])]
    (with-redefs [sse/push-transform-modal! (constantly nil)
                  state/log-event! (fn [m] (swap! events conj m))]
      (is (= 204 (:status (transform/handle-copy (json-request {:index 1})))))
      (is (= 204 (:status (transform/handle-copy (json-request {:index 2})))))
      (is (= 204 (:status (transform/handle-cancel nil))))
      (let [[copy-two copy-three cancel] @events]
        (is (= ["transform.option.copy" "transform.option.copy" "transform.cancel"]
               (mapv :type @events)))
        (is (= [2 3] (mapv :option-number [copy-two copy-three])))
        (is (= [{:text "two" :why "second"}
                {:text "three" :why "third"}]
               (mapv :option [copy-two copy-three])))
        (is (every? #(= "round-copy" (:round-id %)) @events))
        (is (every? #(= "thread-copy" (:thread-id %)) @events))
        (is (= 3 (:option-count cancel)))))))

(deftest round-identities-can-land-on-a-pre-deploy-transform-map
  (swap! state/app-state update :transform
         #(dissoc (into {} %) :round-id :thread-id))
  (state/transform-set-loading! 1234 "new-round" "new-thread")
  (is (= "new-round" (get-in @state/app-state [:transform :round-id])))
  (is (= "new-thread" (get-in @state/app-state [:transform :thread-id])))
  (is (true? (get-in @state/app-state [:transform :loading?]))))

(deftest exact-proposal-rejects-stale-frame-before-opening
  (let [before "current editor text"]
    (state/set-draft! before)
    (let [key (state/editor-sync-key @state/app-state)
          version (:state-version @state/app-state)
          original-transform (:transform @state/app-state)]
      (with-redefs [sse/push-transform-modal! (constantly nil)
                    state/log-event! (constantly nil)]
        (doseq [[label request]
                [["identity" {:before before :after "new"
                              :editor-sync-key "book-node:someone-else"
                              :state-version version}]
                 ["revision" {:before before :after "new"
                              :editor-sync-key key
                              :state-version (dec version)}]
                 ["before text" {:before "stale editor text" :after "new"
                                 :editor-sync-key key
                                 :state-version version}]]]
          (testing label
            (is (= 409 (:status (transform/handle-propose-exact
                                  (json-request request)))))))
        (is (= original-transform (:transform @state/app-state))
            "a rejected proposal cannot disturb the current modal state")))))

(deftest format-folds-visible-book-snapshot-and-returns-authoritative-frame
  (let [project-idx (get-in @state/app-state [:book-workshop :active-project-idx])
        project-id "format-project"
        node-id "format-node"
        server-text "server older text"
        visible-text "Browser latest line one\nline two.  "
        node (state/make-book-node {:id node-id :title "Format me" :level 0
                                    :draft server-text})]
    (swap! state/app-state
           (fn [st]
             (-> st
                 (assoc :draft server-text :context "ctx" :leftovers "left"
                        :state-version 40)
                 (assoc-in [:book-workshop :projects project-idx :id] project-id)
                 (assoc-in [:book-workshop :projects project-idx :nodes] [node])
                 (assoc-in [:book-workshop :editing-node]
                           {:project-id project-id :node-id node-id
                            :project-idx project-idx :node-idx 0}))))
    (let [tree-pushes (atom 0)]
      (with-redefs [sse/push-book-trees! #(swap! tree-pushes inc)
                    sse/push-draft-sync-conflict! (constantly nil)
                    state/log-event! (constantly nil)]
        (let [sync {:draft visible-text :context "ctx" :leftovers "left"
                    :cursor-pos 0 :state-version 40
                    :editor-sync-key "book-node:format-node"}
              response (transform/handle-format (json-request {:sync sync}))
              frame (json/read-str (:body response) :key-fn keyword)
              expected (transform/mechanical-format visible-text)]
          (is (= 200 (:status response)))
          (is (= 41 (:state-version frame)))
          (is (= expected (:draft frame)))
          (is (= expected (:draft @state/app-state)))
          (is (= expected
                 (get-in @state/app-state
                         [:book-workshop :projects project-idx :nodes 0 :draft])))
          (is (= 1 @tree-pushes)))))))

(deftest dequote-format-rewrites-only-the-selected-range
  (let [project-idx (get-in @state/app-state [:book-workshop :active-project-idx])
        project-id "dequote-project"
        node-id "dequote-node"
        selected "> The best engineering organizations have always done this.\n> They make every developer more productive."
        visible-text (str "Keep this heading.\n" selected "\nKeep this ending.")
        start (count "Keep this heading.\n")
        end (+ start (count selected))
        expected-selection (transform/mechanical-format selected)
        expected (str "Keep this heading.\n" expected-selection "\nKeep this ending.")
        node (state/make-book-node {:id node-id :title "Dequote me" :level 0
                                    :draft "server older text"})]
    (swap! state/app-state
           (fn [st]
             (-> st
                 (assoc :draft "server older text" :context "ctx" :leftovers "left"
                        :state-version 40)
                 (assoc-in [:book-workshop :projects project-idx :id] project-id)
                 (assoc-in [:book-workshop :projects project-idx :nodes] [node])
                 (assoc-in [:book-workshop :editing-node]
                           {:project-id project-id :node-id node-id
                            :project-idx project-idx :node-idx 0}))))
    (with-redefs [sse/push-book-trees! (constantly nil)
                  sse/push-draft-sync-conflict! (constantly nil)
                  state/log-event! (constantly nil)]
      (let [sync {:draft visible-text :context "ctx" :leftovers "left"
                  :cursor-pos end :state-version 40
                  :editor-sync-key "book-node:dequote-node"}
            response (transform/handle-format
                       (json-request {:sync sync :selection {:start start :end end}}))
            frame (json/read-str (:body response) :key-fn keyword)]
        (is (= 200 (:status response)))
        (is (= expected (:draft frame)))
        (is (= start (:selection-start frame)))
        (is (= (+ start (count expected-selection)) (:selection-end frame)))
        (is (= expected (:draft @state/app-state)))
        (is (= expected
               (get-in @state/app-state
                       [:book-workshop :projects project-idx :nodes 0 :draft])))))))

(deftest dequote-format-refuses-an-empty-selection
  (let [before (:draft @state/app-state)
        sync {:draft before :context (:context @state/app-state)
              :leftovers (:leftovers @state/app-state)
              :cursor-pos 0 :state-version (:state-version @state/app-state)
              :editor-sync-key (state/editor-sync-key @state/app-state)}]
    (is (= 400 (:status (transform/handle-format
                         (json-request {:sync sync :selection {:start 0 :end 0}})))))
    (is (= before (:draft @state/app-state)))))

(deftest format-refuses-a-stale-visible-snapshot-without-mutating-prose
  (let [before "authoritative current prose"]
    (state/set-draft! before)
    (let [version (:state-version @state/app-state)
          key (state/editor-sync-key @state/app-state)]
      (with-redefs [sse/push-draft-sync-conflict! (constantly nil)
                    state/log-event! (constantly nil)]
        (let [response (transform/handle-format
                         (json-request
                           {:sync {:draft "stale browser prose"
                                   :context (:context @state/app-state)
                                   :leftovers (:leftovers @state/app-state)
                                   :state-version (dec version)
                                   :editor-sync-key key}}))]
          (is (= 409 (:status response)))
          (is (= before (:draft @state/app-state)))
          (is (= version (:state-version @state/app-state))))))))

(deftest format-requires-a-complete-visible-editor-command
  (is (= 400 (:status (transform/handle-format (json-request {:draft "legacy"}))))))
