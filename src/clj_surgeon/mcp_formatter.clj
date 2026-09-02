(ns clj-surgeon.mcp-formatter
  "Format staged candidate sources before a transaction writes live files."
  (:require
   [clj-surgeon.mcp-exact-verify :as exact-verify]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def default-command
  ["npx" "@chrisoakman/standard-clojure-style" "fix" "{files}"])

(defn verification-profiles-after-format
  "Remove the formatter's corresponding check command after formatting has
   become a mandatory pre-commit stage. Other checks and profiles are exact."
  [profiles formatter-command]
  (let [check-command (mapv #(if (= "fix" %) "check" %) formatter-command)]
    (into {}
          (map (fn [[profile spec]]
                 [profile
                  (if (map? spec)
                    (update spec :commands
                            (fn [commands]
                              (vec (remove #(= check-command %) commands))))
                    spec)]))
          profiles)))

(defn- suffix
  [file]
  (or (some->> (re-find #"\.(?:clj|cljs|cljc)$" file) str)
      ".clj"))

(defn format-candidates!
  "Format future source strings through one closed command. Live project files
   are never passed to the formatter."
  ([project-root command future-sources]
   (format-candidates! project-root command future-sources
                       exact-verify/run-process!))
  ([project-root command future-sources run-process!]
   (if-not (and (vector? command)
                (seq command)
                (every? #(and (string? %) (not (str/blank? %))) command)
                (some #{"{files}"} command))
     {:ok false
      :error-type :invalid-formatter-command
      :error "Formatter command must be a non-empty string vector containing {files}"
      :source-unchanged true}
     (let [staged (mapv (fn [[file source]]
                          (let [temp (java.io.File/createTempFile
                                       "clj-surgeon-candidate-" (suffix file))]
                            (spit temp source)
                            {:file file :temp temp}))
                        (sort-by key future-sources))]
       (try
         (let [temp-files (mapv #(str (:temp %)) staged)
               result (run-process!
                        project-root
                        (exact-verify/expand-command command temp-files))]
           (if (and (:finished? result) (zero? (:exit result)))
             (let [formatted (into (sorted-map)
                                   (map (fn [{:keys [file temp]}]
                                          [file (slurp temp)]))
                                   staged)]
               {:ok true
                :status :complete
                :file-count (count formatted)
                :changed-file-count
                (count (filter (fn [[file source]]
                                 (not= source (get future-sources file)))
                               formatted))
                :elapsed_ms (:elapsed_ms result)
                :future-sources formatted})
             {:ok false
              :error-type (if (:finished? result)
                            :formatter-failed
                            :formatter-timeout)
              :error "Formatter failed on staged candidate files"
              :command (first command)
              :exit (:exit result)
              :elapsed_ms (:elapsed_ms result)
              :output (:output result)
              :source-unchanged true}))
         (finally
           (doseq [{:keys [temp]} staged]
             (io/delete-file temp true))))))))
