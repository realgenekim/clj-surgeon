#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.edn :as edn]
         '[clj-surgeon.diff-impact :refer [dependencies impact content-edges select-impact]]
         '[clj-surgeon.receipt-artifacts :as artifacts]
         '[clojure.string :as str])

;; @spec DATACODE-ENV-002
(defn environment-receipt []
  {:tmpdir (System/getenv "TMPDIR")
   :java-io-tmpdir (System/getProperty "java.io.tmpdir")
   :destination-envelope (artifacts/current-envelope)})

(defn forbidden-overrides [env]
  (sort (filter #(or (= "CLJ_SURGEON_ARTIFACT_ROOT" %)
                     (re-matches #"CLJ_SURGEON_.*_TMP" %)) (keys env))))

(defn gate-environment? [{:keys [tmpdir java-io-tmpdir destination-envelope]}]
  (and (string? tmpdir)
       (boolean (re-matches #"/var/tmp/forge/clj-surgeon-suite-[0-9]+-[0-9a-f]{8}" tmpdir))
       (= tmpdir java-io-tmpdir (first (:roots destination-envelope)))))

;; Read declarations, never require the subject or consult lane selection.
(defn declaration [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (binding [*read-eval* false]
      (let [form (read {:eof nil :read-cond :allow :features #{:clj}} r)]
        (when (and (seq? form) (= 'ns (first form))) form)))))

(defn command! [args]
  (:out (process/check @(process/process (vec args) {:out :string :err :string}))))

(defn repository-files []
  (let [root (fs/real-path ".")]
    (->> (str/split (command! ["git" "ls-files" "--cached" "--others" "--exclude-standard" "-z" "--"]) #"\u0000")
         (remove str/blank?)
         (filter #(and (fs/regular-file? %)
                       (fs/starts-with? (fs/real-path %) root)))
         (map str) set)))

(defn repository-nodes [files]
  (vec
    (keep (fn [file]
            (when-let [form (declaration file)]
              (let [test-side? (str/starts-with? file "test/")]
                {:file file :namespace (second form)
                 :lane (or (:lane (meta (second form)))
                           (:lane (first (filter map? (drop 2 form)))))
                 :test? (and test-side? (str/ends-with? (str (second form)) "-test"))
                 :requires (dependencies form)
                 ;; Both src and test helpers contribute through the require closure.
                 ;; @spec DIFF-IMPACT-006
                 :content-edges (do
                                  (when (> (fs/size file) 8388608)
                                    (throw (ex-info "Diff-impact source exceeds 8 MiB"
                                                    {:error-type :impact-input-limit :file file})))
                                  (content-edges (slurp file) files))})))
          (sort (filter #(re-find #"^(src|test)/.*\.cljc?$" %) files)))))

(defn completed? [process-exit counters]
  (and (= 0 process-exit) (pos-int? (:test counters))
       (= 0 (:fail counters) (:error counters))))

 ;; @spec DIFF-IMPACT-007
(defn selection-sha [text]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")]
    (.update md (.getBytes text "UTF-8"))
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest md)))))

(defn run-selected-scope!
  [selected scope output phase sha selection-ms]
  (let [start (System/nanoTime)
        _ (require 'clj-surgeon.battery-parallel-runner)
        groups ((resolve 'clj-surgeon.battery-parallel-runner/selection-suites)
                (mapv :namespace selected) scope)
        receipts
        (mapv (fn [[suite members]]
                (let [work (str (io/file output phase (name scope) suite))
                      log (str work ".log")
                      opts {"--suite" suite "--selected" (pr-str members)
                            "--selection-sha" sha "--work-dir" work}]
                  (io/make-parents log)
                  (with-open [out (io/writer log)]
                    (binding [*out* out *err* out]
                      (try
                        ((resolve 'clj-surgeon.battery-parallel-runner/run-suite!) opts)
                        (catch Exception e
                          (if (fs/exists? (str work "/receipt.edn"))
                            (edn/read-string (slurp (str work "/receipt.edn")))
                            {:state :refused :suite suite :partial true :selected members
                             :selection-sha sha :error (.getMessage e) :data (ex-data e)})))))))
          groups)
        wall (quot (- (System/nanoTime) start) 1000000)]
    {:scope scope :partial true :selection-sha sha
     :selected (vec (mapcat val groups))
     :selection-wall-ms selection-ms :wall-ms wall :total-wall-ms (+ selection-ms wall)
     :exit (if (every? #(= :passed (:state %)) receipts) 0 1)
     :receipts receipts :runs (vec (mapcat :runs receipts))}))

(defn main [args]
  ;; Direct script invocation must not bypass the launcher's environment gate.
  (when-let [overrides (seq (forbidden-overrides (System/getenv)))]
    (throw (ex-info "Diff-impact refused: environment overrides are set" {:overrides overrides})))
  (when-not (gate-environment? (environment-receipt))
    (throw (ex-info "Diff-impact refused: use test/diff-impact for a narrow gate environment"
                    (environment-receipt))))
  (let [[base output phase scope-arg] args
        started (System/nanoTime)
        scopes (case scope-arg nil (if (= phase "fixed-point") [:fast :all] [:all]) "fast" [:fast] "all" [:all]
                     (throw (ex-info "Scope must be fast or all" {:error-type :invalid-impact-scope})))]
    (when-not (and base output (#{"before" "after" "merged" "fixed-point" "list"} phase))
      (throw (ex-info "Usage: bb test/diff_impact.clj BASE OUTPUT_DIR before|after|merged|fixed-point|list [fast|all]" {})))
    (let [nodes (repository-nodes (repository-files))
          changed-files (remove str/blank? (str/split (command! ["git" "diff" "--name-only" "-z" base "--"]) #"\u0000"))
          selection (select-impact nodes changed-files)
          selected (:namespaces selection)
          inventory (assoc selection :base base
                           :head (str/trim (command! ["git" "rev-parse" "HEAD"]))
                           :environment (environment-receipt))]
      (when (fs/exists? (str output "/results-" phase ".edn"))
        (throw (ex-info "Never overwrite a run" {:output output :phase phase})))
      (fs/create-dirs output)
      (spit (str output "/impact-" phase ".edn") (pr-str inventory))
      (spit (str output "/impact-" phase ".json") (json/generate-string inventory {:pretty true}))
      (println "Selected" (count selected) "test namespaces across all lanes")
      (println "edge-counts" (pr-str (:edge-counts inventory)))
      (println "selection-edge-counts" (pr-str (:selection-edge-counts inventory)))
      (doseq [{n :namespace reasons :reasons} selected
              {:keys [edge-kind file]} reasons]
        (println "selected" n "via" (name edge-kind) file))
      ;; @spec DIFF-IMPACT-004 -- uncertainty holds even a partially selected diff.
      (when (seq (:unmatched-files inventory))
        (let [result (select-keys inventory [:status :reason :changed-files :unmatched-files :namespaces])]
          (println (pr-str result))
          (when-not (= phase "list")
            (spit (str output "/results-" phase ".edn") (str (pr-str result) "\n"))
            (flush)
            (System/exit 1))))
      (when (and (empty? selected) (empty? (:unmatched-files inventory)))
        (let [result (select-keys inventory [:status :changed-files :unmatched-files :namespaces])]
          (println (pr-str result))
          (when-not (= phase "list")
            (spit (str output "/results-" phase ".edn") (str (pr-str result) "\n")))))
      (when (and (seq selected) (not= phase "list"))
        (let [selection-ms (quot (- (System/nanoTime) started) 1000000)
              sha (selection-sha (slurp (str output "/impact-" phase ".edn")))
              observations (mapv (fn [scope]
                                   (let [scope-start (System/nanoTime)
                                         r (try (run-selected-scope! selected scope output phase sha selection-ms)
                                                (catch Exception e
                                                  (let [wall (quot (- (System/nanoTime) scope-start) 1000000)]
                                                    {:scope scope :partial true :selection-sha sha
                                                     :selected (mapv :namespace selected)
                                                     :state :refused :exit 1 :runs [] :receipts []
                                                     :selection-wall-ms selection-ms :wall-ms wall
                                                     :total-wall-ms (+ selection-ms wall)
                                                     :error (.getMessage e) :data (ex-data e)})))]
                                     (spit (str output "/results-" phase ".edn")
                                           (str (pr-str r) "\n") :append true)
                                     (println "TOOL" (name scope) "wall-ms" (:wall-ms r)
                                              "total-wall-ms" (:total-wall-ms r) "exit" (:exit r))
                                     (flush)
                                     r)) scopes)]
          (System/exit (if (every? #(zero? (:exit %)) observations) 0 1)))))))

(when-not (#{"--self-test" "--library"} (first *command-line-args*))
  (main *command-line-args*))

(when (= ["--self-test"] *command-line-args*)
  ;; @spec DATACODE-ENV-002 -- mutate every environment boundary independently.
  (let [tmp "/var/tmp/forge/clj-surgeon-suite-42-deadbeef"
        environment {:tmpdir tmp :java-io-tmpdir tmp
                     :destination-envelope {:roots [tmp "/home/seat/.local/state/clj-surgeon" "/work"]}}]
    (assert (gate-environment? environment))
    (doseq [bad [(assoc environment :tmpdir "/var/tmp/forge")
                 (assoc environment :tmpdir nil)
                 (assoc environment :java-io-tmpdir "/var/tmp/forge")
                 (assoc-in environment [:destination-envelope :roots 0] "/var/tmp/forge")]]
      (assert (not (gate-environment? bad)))))
  (doseq [name ["CLJ_SURGEON_MEMORY_TMP" "CLJ_SURGEON_FUTURE_TMP" "CLJ_SURGEON_ARTIFACT_ROOT"]
          value ["" "/var/tmp/forge"]]
    (assert (= [name] (forbidden-overrides {name value "TMPDIR" "/var/tmp/forge"}))))
  (assert (empty? (forbidden-overrides {"TMPDIR" "/var/tmp/forge" "CLJ_SURGEON_EVENTS_FILE" "events"})))
  (println "Environment self-check: broad/mismatched roots and empty/future overrides refused")
  ;; Sol F1, 2026-09-13: generate the dependency-depth class, then mutate
  ;; every edge. Fixture declarations are data; no fixture namespace is loaded.
  (doseq [depth (range 1 9)]
    (let [names (mapv #(symbol (str "fixture.n" %)) (range (inc depth)))
          leaf (peek names)
          nodes (mapv (fn [i n]
                        {:namespace n :test? (= n leaf)
                         :requires (dependencies
                                     (if (zero? i) (list 'ns n)
                                       (list 'ns n (list :require [(names (dec i)) :as 'dep]))))})
                      (range) names)
          nodes (conj nodes {:namespace 'unrelated-test :test? true :requires #{}})
          selected (set (map :namespace (impact nodes #{(first names)})))]
      (println "Chain depth" depth "selected" selected)
      (assert (= #{leaf} selected) (str "Every leaf selected at depth " depth))
      (doseq [edge (range 1 (inc depth))]
        (assert (empty? (impact (assoc-in nodes [edge :requires] #{}) #{(first names)}))
                (str "Cut edge " edge " disconnects depth " depth)))))
  (let [nodes [{:namespace 'source :requires #{}}
               {:namespace 'middle :requires #{'source}}
               {:namespace 'far :requires #{'middle}}
               {:namespace 'fast-test :requires #{'source} :test? true :lane :fast}
               {:namespace 'battery-test :requires #{'middle 'source} :test? true :lane :battery}
               {:namespace 'bb-excluded-test :requires #{'middle} :test? true}
               {:namespace 'too-far-test :requires #{'far} :test? true}
               {:namespace 'unrelated-test :requires #{} :test? true}]
        selected (impact nodes #{'source})]
    (assert (= #{'fast-test 'battery-test 'bb-excluded-test 'too-far-test} (set (map :namespace selected))))
    (assert (= 4 (count selected)))
    (assert (= [["source"]] (:paths (first selected))))
    (assert (empty? (impact nodes #{})))
    (assert (= #{'a.x 'a.y} (dependencies '(ns test (:require (a [x :as x] y))))))
    (assert (= #{'x 'y} (dependencies '(ns test (:require [x :as a]) (:use y))))))
  (assert (completed? 0 {:test 1 :pass 1 :fail 0 :error 0}))
  (doseq [[exit counters] [[0 nil] [1 {:test 1 :fail 0 :error 0}]
                           [0 {:test 0 :fail 0 :error 0}] [0 {:test 1 :fail 1 :error 0}]]]
    (assert (not (completed? exit counters))))
  (let [nodes [{:namespace 'changed :requires #{'cycle}}
               {:namespace 'cycle :requires #{'changed}}
               {:namespace 'helper-test :test? true :requires #{'cycle 'other}}
               {:namespace 'leaf-test :test? true :requires #{'helper-test}}]
        selected (impact nodes #{'changed 'other})]
    (assert (= #{'helper-test 'leaf-test} (set (map :namespace selected))))
    (assert (= #{["helper-test" "cycle" "changed"] ["helper-test" "other"]}
               (set (:paths (second selected)))))
    (assert (= selected (impact (reverse nodes) #{'other 'changed}))))
  (doseq [clause [":require" ":use" ":require-macros"]
          spec ["fixture.n0" "[fixture.n0]" "(fixture.n0)"
                "[fixture.n0 :as dep]" "(fixture.n0 :refer [v])"
                "[fixture [n0 :as dep]]" "(fixture n0)"
                "[fixture (n0)]" "^:fixture [fixture.n0 :as-alias dep]"
                "\"fixture.n0\"" "[\"fixture.n0\" :as dep]"
                "#?(:clj [fixture.n0] :cljs [unrelated])"
                "#?@(:clj [[fixture.n0]] :cljs [[unrelated]])"
                "#_[unrelated] [fixture.n0] :reload"]]
    (let [deps (dependencies (str "(ns fixture.leaf (" clause " " spec "))"))]
      (assert (= #{'fixture.n0} deps) (str clause " " spec))
      (assert (= ['fixture.leaf]
                 (mapv :namespace (impact [{:namespace 'fixture.leaf :test? true :requires deps}]
                                    #{'fixture.n0}))))))
  ;; The exact path in Sol's fence report, with both intermediate namespaces.
  (assert (= [["clj-surgeon.mcp-prepared-confirmation" "clj-surgeon.mcp-contract"
               "clj-surgeon.mcp-extraction"]]
             (:paths (first (impact
                              (mapv (fn [[n dep]] {:namespace n :requires #{dep}
                                                   :test? (= n 'clj-surgeon.mcp-prepared-wire-test)})
                                    '[[clj-surgeon.mcp-prepared-wire-test clj-surgeon.mcp-prepared-confirmation]
                                      [clj-surgeon.mcp-prepared-confirmation clj-surgeon.mcp-contract]
                                      [clj-surgeon.mcp-contract clj-surgeon.mcp-extraction]])
                              #{'clj-surgeon.mcp-extraction})))))
  (println "Diff-impact self-check: depths 1..8, every edge cut, unrelated negative, cycles, multiple seeds, test intermediates, shared libspec shapes, Sol F1 and completion checks passed"))
