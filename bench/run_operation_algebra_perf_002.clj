#!/usr/bin/env bb

(ns run-operation-algebra-perf-002
  ;; @spec OP-ALG-PERF-002
  (:require
   [babashka.process :as process]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.charset StandardCharsets)
   (java.security MessageDigest)))

(def frozen-source
  (str "(ns sample.change)\n"
       "(defn render []\n"
       "  {:message \"O'Reilly said \\\"hi\\\"; path C:\\\\tmp\\nnext\"})\n"))

(def frozen-request
  {:changes
   [{:id :message
     :in ["src/sample/change.clj"]
     :forms ['render]
     :find "{:message \"O'Reilly said \\\"hi\\\"; path C:\\\\tmp\\nnext\"}"
     :do [:replace
          "{:message \"O'Reilly said \\\"done\\\"; path C:\\\\tmp\\nnext\"}"]
     :expect {:matches 1 :each-form 1}}]
   :expect {:changes 1 :edits 1 :files 1}})

(def blocked-commands
  ["clj-kondo" "clojure-lsp" "standard-clj-format" "npx" "node" "bun"])

(defn sha256
  [value]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (str value) StandardCharsets/UTF_8))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn file-sha256
  [file]
  (sha256 (slurp file)))

(defn run!
  [argv opts]
  (let [started (System/nanoTime)
        result @(process/process argv opts)
        ended (System/nanoTime)]
    (assoc result
           :elapsed-ns (- ended started)
           :elapsed-ms (/ (double (- ended started)) 1000000.0))))

(defn assert-success!
  [result message]
  (when-not (zero? (:exit result))
    (throw (ex-info message (select-keys result [:exit :out :err]))))
  result)

(defn git-value
  [repository & args]
  (let [result (run! (into ["git" "-C" repository] args)
                     {:out :string :err :string})]
    (assert-success! result "Git command failed")
    (str/trim (:out result))))

(defn materialize!
  [harness-root repository source-ref destination]
  (let [script (str (io/file harness-root
                             "bench/materialize_benchmark_candidate.sh"))
        result (run! [script repository source-ref destination]
                     {:out :string :err :string})]
    (assert-success! result "Candidate materialization failed")
    (let [receipt-file (io/file destination "candidate-receipt.edn")
          receipt (edn/read-string (slurp receipt-file))]
      {:root (.getCanonicalPath (io/file destination))
       :wrapper (.getCanonicalPath
                  (io/file destination (:cli-wrapper receipt)))
       :receipt receipt
       :receipt-sha256 (file-sha256 receipt-file)
       :materialization-ms (:elapsed-ms result)})))

(defn write-blocking-shims!
  [directory event-file]
  (.mkdirs (io/file directory))
  (doseq [command blocked-commands]
    (let [shim (io/file directory command)]
      (spit shim
            (str "#!/bin/sh\n"
                 "printf '%s\\n' '" command "' >> '" event-file "'\n"
                 "exit 86\n"))
      (.setExecutable shim true)))
  ;; The candidate wrapper needs Babashka, but no other development runtime.
  (let [bb-path (str/trim
                  (:out (assert-success!
                          (run! ["sh" "-c" "command -v bb"]
                                {:out :string :err :string})
                          "Babashka is unavailable")))
        link (io/file directory "bb")]
    (java.nio.file.Files/createSymbolicLink
      (.toPath link)
      (.toPath (io/file bb-path))
      (make-array java.nio.file.attribute.FileAttribute 0))))

(defn percentile
  [values probability]
  (let [ordered (vec (sort values))
        index (max 0 (dec (long (Math/ceil (* probability (count ordered))))))]
    (nth ordered index)))

(defn median
  [values]
  (let [ordered (vec (sort values))
        n (count ordered)
        midpoint (quot n 2)]
    (if (odd? n)
      (nth ordered midpoint)
      (/ (+ (nth ordered (dec midpoint)) (nth ordered midpoint)) 2.0))))

(defn summarize
  [rows]
  (let [values (mapv :elapsed-ms rows)]
    {:runs (count rows)
     :p50-ms (median values)
     :p95-ms (percentile values 0.95)
     :min-ms (apply min values)
     :max-ms (apply max values)
     :values-ms values}))

(defn regression-percent
  [baseline candidate]
  (* 100.0 (- (/ candidate baseline) 1.0)))

(defn run-arm!
  [{:keys [arm candidate workspace request-text raw-dir env phase sequence]}]
  (let [result (run! [(:wrapper candidate) ":op" ":change" ":spec-file" "-"]
                     {:dir workspace
                      :in request-text
                      :out :string
                      :err :string
                      :extra-env env})
        prefix (format "%02d-%s-%s" sequence (name phase) (name arm))
        stdout-file (io/file raw-dir (str prefix ".stdout.edn"))
        stderr-file (io/file raw-dir (str prefix ".stderr.txt"))]
    (spit stdout-file (:out result))
    (spit stderr-file (:err result))
    (assert-success! result "Timed CLI preview failed")
    (let [parsed (edn/read-string (:out result))
          row {:sequence sequence
               :phase phase
               :arm arm
               :source-commit (get-in candidate [:receipt :source-commit])
               :elapsed-ns (:elapsed-ns result)
               :elapsed-ms (:elapsed-ms result)
               :exit (:exit result)
               :stdout-bytes (count (.getBytes (:out result)
                                               StandardCharsets/UTF_8))
               :stdout-sha256 (sha256 (:out result))
               :stderr-bytes (count (.getBytes (:err result)
                                               StandardCharsets/UTF_8))
               :stderr-sha256 (sha256 (:err result))
               :parsed-sha256 (sha256 (pr-str parsed))}]
      (spit (io/file raw-dir (str prefix ".run.edn"))
            (str (pr-str row) "\n"))
      (assoc row :parsed parsed))))

(defn -main
  [& [repository pre-ref post-ref output-root measured-runs-text]]
  (when-not (every? some? [repository pre-ref post-ref output-root])
    (binding [*out* *err*]
      (println "usage: run_operation_algebra_perf_002.clj REPOSITORY PRE_COMMIT POST_COMMIT OUTPUT [RUNS_PER_ARM]"))
    (System/exit 64))
  (let [measured-runs (parse-long (or measured-runs-text "20"))
        _ (when-not (and measured-runs (>= measured-runs 8))
            (throw (ex-info "RUNS_PER_ARM must be at least 8"
                            {:runs-per-arm measured-runs-text})))
        repository (.getCanonicalPath (io/file repository))
        harness-root (.getCanonicalPath (io/file "."))
        output (io/file output-root)]
    (when (.exists output)
      (throw (ex-info "Refusing to replace benchmark output" {:output output-root})))
    (.mkdirs output)
    (let [candidate-dir (io/file output "candidates")
          raw-dir (io/file output "raw")
          workspace (io/file output "workspace")
          shim-dir (io/file output "blocked-bin")
          blocked-events (io/file output "blocked-command-events.txt")
          _ (.mkdirs candidate-dir)
          _ (.mkdirs raw-dir)
          _ (.mkdirs (io/file workspace "src/sample"))
          _ (spit blocked-events "")
          _ (write-blocking-shims! shim-dir (.getCanonicalPath blocked-events))
          pre-commit (git-value repository "rev-parse" "--verify"
                                (str pre-ref "^{commit}"))
          post-commit (git-value repository "rev-parse" "--verify"
                                 (str post-ref "^{commit}"))
          pre (materialize! harness-root repository pre-commit
                            (str (io/file candidate-dir "pre")))
          post (materialize! harness-root repository post-commit
                             (str (io/file candidate-dir "post")))
          request-text (str (pr-str frozen-request) "\n")
          _ (spit (io/file workspace "src/sample/change.clj") frozen-source)
          _ (spit (io/file output "request.edn") request-text)
          clean-path (str (.getCanonicalPath shim-dir)
                          ":/usr/bin:/bin:/usr/sbin:/sbin")
          environment {"PATH" clean-path}
          candidates {:pre pre :post post}
          warmup-order [:pre :post]
          measured-order (vec (mapcat (fn [replicate]
                                        (if (odd? replicate)
                                          [:pre :post]
                                          [:post :pre]))
                                      (range 1 (inc measured-runs))))
          counter (atom 0)
          invoke (fn [phase arm]
                   (run-arm! {:arm arm
                              :candidate (get candidates arm)
                              :workspace (.getCanonicalPath workspace)
                              :request-text request-text
                              :raw-dir raw-dir
                              :env environment
                              :phase phase
                              :sequence (swap! counter inc)}))
          warmups (mapv #(invoke :warmup %) warmup-order)
          rows (mapv #(invoke :measured %) measured-order)
          pre-rows (filterv #(= :pre (:arm %)) rows)
          post-rows (filterv #(= :post (:arm %)) rows)
          reference-output (:parsed (first warmups))
          all-rows (concat warmups rows)
          output-parity (every? #(= reference-output (:parsed %)) all-rows)
          blocked-invocations (->> (slurp blocked-events)
                                   str/split-lines
                                   (remove str/blank?)
                                   count)
          pre-summary (summarize pre-rows)
          post-summary (summarize post-rows)
          p50-regression (regression-percent (:p50-ms pre-summary)
                                             (:p50-ms post-summary))
          p95-regression (regression-percent (:p95-ms pre-summary)
                                             (:p95-ms post-summary))
          gate (and output-parity
                    (zero? blocked-invocations)
                    (= measured-runs
                       (:runs pre-summary)
                       (:runs post-summary))
                    (<= p50-regression 5.0)
                    (<= p95-regression 5.0))
          report
          {:schema :clj-surgeon.operation-algebra-perf-002/v1
           :gate-passed gate
           :protocol {:warmups-per-arm 1
                      :measured-runs-per-arm measured-runs
                      :counterbalanced true
                      :warmup-order warmup-order
                      :measured-order measured-order
                      :clock :system-nano-time
                      :operation [:change :preview]
                      :models 0
                      :mcp-launches 0
                      :shared-ports-touched 0}
           :identity
           {:repository repository
            :pre {:source-commit pre-commit
                  :source-tree (get-in pre [:receipt :source-tree])
                  :archive-sha256 (get-in pre [:receipt :archive-sha256])
                  :wrapper-sha256 (get-in pre [:receipt :cli-wrapper-sha256])
                  :receipt-sha256 (:receipt-sha256 pre)}
            :post {:source-commit post-commit
                   :source-tree (get-in post [:receipt :source-tree])
                   :archive-sha256 (get-in post [:receipt :archive-sha256])
                   :wrapper-sha256 (get-in post [:receipt :cli-wrapper-sha256])
                   :receipt-sha256 (:receipt-sha256 post)}
            :harness-commit (git-value harness-root "rev-parse" "HEAD")
            :harness-tree (git-value harness-root "rev-parse" "HEAD^{tree}")
            :harness-status (git-value harness-root "status" "--short")
            :harness-file "bench/run_operation_algebra_perf_002.clj"
            :harness-sha256 (file-sha256 *file*)
            :materializer-sha256
            (file-sha256 (io/file harness-root
                                  "bench/materialize_benchmark_candidate.sh"))
            :fixture-source-sha256 (sha256 frozen-source)
            :request-sha256 (sha256 request-text)}
           :equivalence {:all-output-maps-equal output-parity
                         :parsed-output-sha256
                         (sha256 (pr-str reference-output))}
           :launch-evidence
           {:blocked-commands blocked-commands
            :blocked-command-invocations blocked-invocations
            :blocked-event-log-sha256 (file-sha256 blocked-events)
            :path clean-path
            :analyzer-launches 0
            :formatter-launches 0
            :verifier-launches 0}
           :statistics {:pre pre-summary
                        :post post-summary
                        :regression {:p50-percent p50-regression
                                     :p95-percent p95-regression
                                     :maximum-allowed-percent 5.0}}
           :raw-runs (mapv #(dissoc % :parsed) rows)}
          report-file (io/file output "receipt.edn")]
      (spit report-file (str (pr-str report) "\n"))
      (spit (io/file output "receipt.sha256")
            (str (file-sha256 report-file) "  receipt.edn\n"))
      (prn report)
      (when-not gate
        (System/exit 1)))))

(apply -main *command-line-args*)
