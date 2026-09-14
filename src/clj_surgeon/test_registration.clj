(ns clj-surgeon.test-registration
  "One entrance and one diagnostic for test namespace registration."
  (:require
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.spawn-ledger :as spawn]
   [clj-surgeon.test-census :as census]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.zip :as z]))

(def manifest-file "test/clj_surgeon/lane_manifest.clj")
(def witness-file "test/clj_surgeon/lane_manifest_test.clj")
(def census-file census/census-file)
(def control-root "docs/observations/2026-09-12-bbtower-block-b/attempt22")

(defn- refuse! [kind data]
  (throw (ex-info (name kind) (assoc data :error-type kind))))

(defn- children [loc]
  (take-while some? (iterate z/right (z/down loc))))

(defn- forms [source]
  (children (z/of-string* source)))

(defn- unique [locations context]
  (when-not (= 1 (count locations))
    (refuse! :register-ambiguous (assoc context :matches (count locations))))
  (first locations))

(defn- owner [source name]
  (unique (filter #(let [v (z/sexpr %)]
                     (and (seq? v) (= name (second v)))) (forms source))
          {:form name}))

(defn- literal [loc]
  (if (= :quote (z/tag loc)) (z/down loc) loc))

(defn- value [loc]
  (let [v (z/sexpr (literal loc))]
    (if (map? v)
      (into {} (map (fn [[k v]] [(if (and (seq? k) (= 'quote (first k))) (second k) k) v])) v)
      v)))

(defn- initializer [source name]
  (literal (last (children (owner source name)))))

(defn- collection-value [source name tag]
  (let [loc (initializer source name)]
    (when-not (= tag (z/tag loc)) (refuse! :register-ambiguous {:form name :expected tag}))
    (value loc)))

(defn- update-collection [source name tag f]
  (let [loc (initializer source name)]
    (when-not (= tag (z/tag loc)) (refuse! :register-ambiguous {:form name :expected tag}))
    (z/root-string (f loc))))

(defn- append-map [source name key v quoted-key?]
  (update-collection source name :map
                     #(-> % (z/append-child (if quoted-key? (list 'quote key) key))
                          (z/append-child v))))

(defn- append-set [source name members]
  (update-collection source name :set #(reduce z/append-child % (sort members))))

(defn- pin-loc [source]
  (let [o (owner source 'every-manifest-entry-exists-on-disk)]
    (unique (for [loc (take-while #(not (z/end? %)) (iterate z/next (z/of-string (z/string o))))
                  :when (= :list (z/tag loc))
                  :let [v (z/sexpr loc)]
                  :when (and (= '= (first v)) (= 3 (count v))
                             (integer? (second v)) (= '(count runtimes) (nth v 2)))]
              (z/right (z/down loc)))
            {:form 'every-manifest-entry-exists-on-disk :expression '(= _ (count runtimes))})))

(defn- replace-pin [source count-value]
  ;; Locate in the full CST after the unique bounded owner check.
  (pin-loc source)
  (let [o (owner source 'every-manifest-entry-exists-on-disk)
        found (z/find-next (z/down o) z/next
                           #(and (= :list (z/tag %))
                                 (= (z/sexpr %) (list '= (z/sexpr (pin-loc source)) '(count runtimes)))))]
    (z/root-string (z/replace (z/right (z/down found)) count-value))))

(defn- ns-info [source]
  (let [loc (unique (filter #(let [v (z/sexpr %)] (and (seq? v) (= 'ns (first v))))
                      (forms source)) {:form 'ns})
        v (z/sexpr loc)
        lanes (keep :lane (cons (meta (second v)) (filter map? (drop 2 v))))]
    (when (> (count (set lanes)) 1)
      (refuse! :register-conflict {:form 'ns :actual (first lanes) :expected (second lanes)}))
    {:namespace (second v) :lane (first lanes)
     :tests (census/deftest-names (second v) source)}))

(defn- namespace-file [n]
  (str "test/" (-> (str n) (str/replace "." "/") (str/replace "-" "_")) ".clj"))

(defn shell-quote [s] (str "'" (str/replace (str s) "'" "'\"'\"'") "'"))

(defn invocation [{:keys [namespace lane runtime bb-ineligible]}]
  (str "make register-test-ns NS=" (shell-quote namespace)
       " LANE=" (shell-quote (name (or lane :battery)))
       " RUNTIME=" (shell-quote (name (or runtime :jvm)))
       (when bb-ineligible (str " BB_INELIGIBLE=" (shell-quote (pr-str bb-ineligible))))))

;; INTENT: REGNS-005
;; @spec REGNS-005
(defn controls-valid? [n runtime registration controls]
  (boolean
    (or (let [state (lm/portability-state n controls registration)]
          (or (= :portable (:state state))
              (and (= :jvm runtime) (= :bb-ineligible (:state state)))))
        (and (= :jvm runtime)
             (= :bb-load-incompatible (:reason (lm/portability-refusal n controls)))
             (not (str/blank? (get-in controls [:bb-load :message])))))))

;; INTENT: REGNS-001
;; INTENT: REGNS-006
;; @spec REGNS-001
;; @spec REGNS-006
(defn checklist [{:keys [namespace file lane requested-lane runtime manifest-lane
                         registered-runtime pin expected-count adopted? tests census
                         controls-valid? repository-count?] :as model}]
  (let [rows [{:surface 1 :name "lane/runtime" :file manifest-file :form '[manifest portability-runtimes]
               :actual [manifest-lane registered-runtime] :expected [(or lane requested-lane) runtime]}
              {:surface 2 :name "ns metadata" :file file :form 'ns
               :actual lane :expected (or lane requested-lane)}
              {:surface 3 :name "runtime count" :file witness-file :form 'every-manifest-entry-exists-on-disk
               :actual pin :expected expected-count}
              {:surface 4 :name "adoption" :file witness-file :form 'adopted-since-round-one
               :actual (boolean adopted?) :expected true :value namespace}
              {:surface 5 :name "census/control" :file [census-file (str control-root "/portability-controls.edn")]
               :form '[deftest-census portability-controls]
               :actual {:missing-tests (set/difference tests census) :controls-valid? (boolean controls-valid?)}
               :expected {:missing-tests #{} :controls-valid? true} :value tests}]
        ;; Repository sweeps emit the pin once, outside namespace checklists.
        rows (if repository-count? (filterv #(not= 3 (:surface %)) rows) rows)
        missing (vec (remove #(= (:actual %) (:expected %)) rows))]
    {:ok (empty? missing) :namespace namespace :missing missing :checklist rows
     :message (str "Registration checklist for " namespace ": "
                   (str/join "; " (map #(str (:surface %) " " (:name %) " " (:file %) " -> " (:form %)
                                             " actual=" (pr-str (:actual %)) " expected=" (pr-str (:expected %))) rows))
                   ". Remedy: " (invocation (assoc model :lane (or lane requested-lane))))}))

(defn control-paths [n]
  (into {} (for [[k suffix] [[:jvm "jvm-test"] [:bb "bb-test"] [:bb-load "bb-load"]]]
             [k (str control-root "/controls/" n "-" suffix ".control.edn")])))

(defn- read-controls [snapshot n]
  (into {} (keep (fn [[k path]] (when-let [s (snapshot path)] [k (edn/read-string s)])))
        (control-paths n)))

(defn- projection [snapshot]
  (let [inventory (some-> (snapshot (str control-root "/portability-controls.edn")) edn/read-string)
        md (snapshot (str control-root "/portability-census.md"))]
    {:inventory inventory
     :population-valid? (and inventory md
                          (str/includes? md (str "All " (count inventory) " assigned namespaces are listed.")))
     :lines (when md (str/split-lines md))}))

(defn- projection-valid? [snapshot n runtime lane]
  (let [{:keys [inventory population-valid? lines]}
        (or (:registration-projection (meta snapshot)) (projection snapshot))
        entry (get inventory n)
        row-prefix (str "| " n " | " runtime " | " lane " |")]
    (and entry population-valid?
         (some #(and (str/starts-with? % row-prefix)
                     (str/includes? % (str "| " (name (:classification entry)))))
               lines))))

(defn- model-base [snapshot]
  {:measurements (some-> (snapshot lm/runtime-evidence-path) edn/read-string)
   :manifest (collection-value (snapshot manifest-file) 'manifest :map)
   :runtimes (collection-value (snapshot manifest-file) 'portability-runtimes :map)
   :adoptions (collection-value (snapshot witness-file) 'adopted-since-round-one :set)
   :historical (collection-value (snapshot witness-file) 'round-one-jvm-namespaces :set)
   :census (edn/read-string (snapshot census-file))
   :pin (z/sexpr (pin-loc (snapshot witness-file)))
   :registrations (collection-value (snapshot manifest-file) 'bb-ineligibilities :map)
   :disk (into {} (for [[path source] snapshot :when (re-matches #"test/.*_test\.clj" path)]
                    [path (ns-info source)]))})

(defn model [snapshot {:keys [namespace lane runtime bb-ineligible]}]
  (let [file (namespace-file namespace)
        {:keys [manifest runtimes adoptions historical census pin registrations disk measurements]}
        (or (:registration-base (meta snapshot)) (model-base snapshot))
        info (disk file)
        registration (registrations namespace)
        controls (read-controls snapshot namespace)]
    (merge info {:namespace namespace :file file :requested-lane lane
                 :runtime (or runtime (runtimes namespace) :jvm)
                 :manifest-lane (manifest namespace) :registered-runtime (when-let [r (runtimes namespace)] (if registration :jvm (get-in measurements [namespace :runtime] r)))
                 :pin pin
                 :expected-count (count (into (set (keys runtimes)) (map :namespace (vals disk))))
                 :adopted? (or (contains? adoptions namespace) (contains? historical namespace))
                 :tests (or (:tests info) #{}) :census census
                 :registration registration :bb-ineligible bb-ineligible
                 :controls-valid? (and (controls-valid? namespace (or runtime (runtimes namespace))
                                         (or bb-ineligible registration) controls)
                                       (projection-valid? snapshot namespace (or runtime (runtimes namespace)) (:lane info)))})))

;; INTENT: REGNS-002
;; INTENT: REGNS-007
;; INTENT: REGNS-008
;; @spec REGNS-002
;; @spec REGNS-007
;; @spec REGNS-008
(defn- validate! [snapshot {:keys [namespace lane runtime bb-ineligible] :as request}]
  (when-not (and (symbol? namespace) (re-matches #"[a-z][a-z0-9-]*(?:\.[a-z][a-z0-9-]*)*-test" (str namespace))
                 (#{:fast :integration :battery} lane) (#{:jvm :bb} runtime))
    (refuse! :register-invalid-request {:request request}))
  (when-not (snapshot (namespace-file namespace))
    (refuse! :register-source-missing {:file (namespace-file namespace)}))
  (when (and bb-ineligible
             (not (and (= :jvm runtime) (map? bb-ineligible)
                       (set? (:reasons bb-ineligible)) (seq (:reasons bb-ineligible))
                       (every? lm/bb-capability-vocabulary (:reasons bb-ineligible))
                       (string? (:detail bb-ineligible)) (not (str/blank? (:detail bb-ineligible))))))
    (refuse! :register-invalid-request {:field :bb-ineligible :value bb-ineligible}))
  (let [m (model snapshot request)]
    (when-not (= namespace (:namespace (ns-info (snapshot (namespace-file namespace)))))
      (refuse! :register-conflict {:field :namespace :actual (:namespace (ns-info (snapshot (namespace-file namespace)))) :expected namespace}))
    (when-not (:lane m) (refuse! :register-metadata-missing {:file (:file m) :form 'ns}))
    (doseq [[field actual expected] [[:metadata (:lane m) lane]
                                     [:manifest (:manifest-lane m) lane]
                                     [:runtime (:registered-runtime m) runtime]
                                     [:bb-ineligible (:registration m) (or bb-ineligible (:registration m))]]]
      (when (and (some? actual) (not= actual expected))
        (refuse! :register-conflict {:field field :actual actual :expected expected})))
    (doseq [[k row] (read-controls snapshot namespace)]
      (doseq [[field expected] [[:namespace namespace] [:runtime (if (= k :bb-load) :bb k)]]]
        (when-not (= expected (get row field))
          (refuse! :register-conflict {:field field :form k :actual (get row field) :expected expected}))))
    (when-let [text (snapshot (str control-root "/portability-controls.edn"))]
      (when-let [entry (get (edn/read-string text) namespace)]
        (doseq [[k expected] (assoc (control-paths namespace) :bb-load
                               (str control-root "/controls/" namespace "-bb-load.edn"))]
          (when-not (= expected (get entry k))
            (refuse! :register-conflict {:file (str control-root "/portability-controls.edn")
                                         :form namespace :field k :actual (get entry k) :expected expected})))))
    m))

;; INTENT: REGNS-003
;; INTENT: REGNS-004
;; @spec REGNS-003
;; @spec REGNS-004
(defn plan [snapshot {:keys [namespace lane runtime bb-ineligible] :as request}]
  (try
    (let [m (validate! snapshot request)
          ms (cond-> (snapshot manifest-file)
               (nil? (:manifest-lane m)) (append-map 'manifest namespace lane true)
               (nil? (:registered-runtime m)) (append-map 'portability-runtimes namespace runtime false)
               (and bb-ineligible (nil? (:registration m))) (append-map 'bb-ineligibilities namespace bb-ineligible true))
          ws (cond-> (snapshot witness-file)
               (not= (:pin m) (:expected-count m)) (replace-pin (:expected-count m))
               (not (:adopted? m)) (append-set 'adopted-since-round-one #{namespace}))
          census-candidate (atom (snapshot census-file))
          derived (census/derived-census
                    (for [n (keys (collection-value ms 'manifest :map))]
                      [n (snapshot (namespace-file n))]))
          census-result (binding [*out* (java.io.StringWriter.)]
                          (census/regenerate-census! census-file derived snapshot
                            (fn [_ text] (reset! census-candidate text))))
          _ (when-not (:ok census-result)
              (refuse! :register-removed-tests (select-keys census-result [:removed])))
          cs @census-candidate
          candidate (into {} (remove (fn [[p s]] (= s (snapshot p))))
                          {manifest-file ms witness-file ws census-file cs})
          changes (mapv (fn [[p s]]
                          {:file p
                           :forms (if (= p census-file)
                                    [{:form 'deftest-census :before-count (count (:census m))
                                      :after-count (count (edn/read-string s))}]
                                    (vec (concat
                                           (for [[owner-name tag] (if (= p manifest-file)
                                                                    [['manifest :map] ['portability-runtimes :map] ['bb-ineligibilities :map]]
                                                                    [['adopted-since-round-one :set]])
                                                 :let [before (count (collection-value (snapshot p) owner-name tag))
                                                       after (count (collection-value s owner-name tag))]
                                                 :when (not= before after)]
                                             {:form owner-name :before-count before :after-count after})
                                           (when (and (= p witness-file) (not= (:pin m) (:expected-count m)))
                                             [{:form 'every-manifest-entry-exists-on-disk
                                               :before-count (:pin m) :after-count (:expected-count m)}]))))})
                        (sort-by key candidate))]
      (doseq [[_ s] candidate] (dorun (map z/sexpr (forms s))))
      {:ok true :candidate candidate :changes changes :model m :oracle (checklist m)})
    (catch clojure.lang.ExceptionInfo e (merge {:ok false :changes [] :error (ex-message e)} (ex-data e)))
    (catch Exception e {:ok false :changes [] :error-type :register-unsupported-syntax :error (ex-message e)})))

(defn- safe-file [root path]
  (let [root (.getCanonicalFile (io/file root))
        file (io/file root path)
        resolved (.getCanonicalFile file)]
    (when-not (.startsWith (.toPath resolved) (.toPath root))
      (refuse! :register-path-escape {:file path :root (str root)}))
    file))

(defn- read-source [root path]
  (let [file (safe-file root path)]
    (when (.isFile file)
      (when (> (.length file) (* 4 1024 1024))
        (refuse! :register-source-too-large {:file path}))
      (slurp file))))

(defn snapshot [root n]
  (let [base (.toPath (.getCanonicalFile (io/file root)))
        test-dir (safe-file root "test")
        entries (->> (tree-seq #(and (.isDirectory ^java.io.File %)
                                  (not (java.nio.file.Files/isSymbolicLink (.toPath ^java.io.File %))))
                       #(seq (.listFiles ^java.io.File %)) test-dir)
                  (take 10001) vec)
        _ (when (> (count entries) 10000) (refuse! :register-source-limit {:entries 10000}))
        files (->> entries
                   (filter #(and (.isFile ^java.io.File %) (re-find #"_test\.clj$" (.getName ^java.io.File %))))
                   (take 1025) vec)]
    (when (> (count files) 1024) (refuse! :register-source-limit {:limit 1024}))
    (into {} (keep (fn [p] (when-let [s (read-source root p)] [p s])))
          (distinct (concat [manifest-file witness-file census-file lm/runtime-evidence-path
                             (str control-root "/portability-controls.edn")
                             (str control-root "/portability-census.md")]
                            (vals (control-paths n))
                            (map #(str (.relativize base (.toPath (.getAbsoluteFile ^java.io.File %)))) files))))))

(defn oracle [root request]
  (checklist (model (snapshot root (:namespace request)) request)))

(defn repository-checklist [root]
  (let [s (snapshot root 'clj-surgeon.lane-manifest-test)
        base (model-base s)
        projection-data (projection s)
        excluded (collection-value (s manifest-file) 'excluded :map)
        discovered (set (map :namespace (vals (:disk base))))
        unregistered (set/difference discovered (set (keys (:manifest base))) (set (keys excluded)))
        expected-count (count (into discovered (keys (:runtimes base))))
        count-row (when (not= (:pin base) expected-count)
                    {:ok false :scope :repository :surface 3 :name "runtime count"
                     :file witness-file :form 'every-manifest-entry-exists-on-disk
                     :actual (:pin base) :expected expected-count
                     :namespaces (vec (sort unregistered))
                     :message (str "Repository registration count: runtime count " witness-file
                                   " -> every-manifest-entry-exists-on-disk actual=" (:pin base)
                                   " expected=" expected-count
                                   "; unregistered namespaces=" (pr-str (vec (sort unregistered)))
                                   ". Reconcile the repository pin with the discovered/runtime union.")})]
    (cond-> (vec (for [[_ {:keys [namespace lane]}] (sort-by key (:disk base))
                       :when (not (contains? excluded namespace))
                       :let [paths (or (lm/namespace-runtime-controls namespace) (control-paths namespace))
                             controls (into {} (keep (fn [[_ p]] (when-let [v (read-source root p)] [p v]))) paths)
                             ;; The three historical attempt23 overrides remain authoritative.
                             controls (into {} (for [[k p] paths :let [v (controls p)] :when v]
                                                 [(get (control-paths namespace) k) v]))
                             snapshot (with-meta (merge s controls)
                                        {:registration-base base :registration-projection projection-data})
                             r (checklist (assoc (model snapshot {:namespace namespace :lane (or lane (get (:manifest base) namespace) :battery)
                                                                  :runtime (if (get (:registrations base) namespace) :jvm (get-in base [:measurements namespace :runtime] (get (:runtimes base) namespace :jvm)))})
                                                 :repository-count? true))]
                       :when (not (:ok r))]
                   r))
      count-row (conj count-row))))

(defn- digest [s]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest md (.getBytes (str s) "UTF-8"))))))

(defn- write-tracked! [root originals path text]
  (let [f (safe-file root path)]
    (when-not (contains? @originals path)
      (swap! originals assoc path (when (.isFile f) (slurp f))))
    (.mkdirs (.getParentFile f))
    (spit f text)))

(defn control-provenance? [row]
  (and (vector? (:command row)) (seq (:command row))
       (every? string? (:command row))
       (string? (get-in row [:subject :root]))
       (boolean (re-matches #"[0-9a-f]{64}" (or (get-in row [:subject :source-sha256]) "")))
       (integer? (:exit row)) (keyword? (:status row))
       (or (= "load" (:mode row)) (map? (:result row)))
       (number? (:wall-ms row)) (<= 0 (:wall-ms row))
       (pos-int? (:pid row)) (pos-int? (:start-ticks row))))

;; INTENT: REGNS-010
;; @spec REGNS-010
(defn stale-controls [previous executed]
  (into {} (for [[k row] previous
                 :let [fresh (executed k)]
                 :when (or (not (control-provenance? row))
                           (not= (:subject row) (:subject fresh))
                           (not= (:command row) (:command fresh)))]
             [k {:previous row :executed fresh}])))

(defn- control! [root originals n runtime mode _]
  (let [prefix (str control-root "/controls/" n "-" (name runtime) "-" mode)
        raw (str prefix ".edn")
        log (str prefix ".log")
        scratch (System/getProperty "java.io.tmpdir")
        subject {:source-sha256 (digest (read-source root (namespace-file n))) :root root}
        command (into (if (= :bb runtime)
                        ["bb" "-Xmx1024m" (str "-Djava.io.tmpdir=" scratch)
                         (str control-root "/portability_runner.clj")]
                        ["clojure" "-J-Xmx1024m" (str "-J-Djava.io.tmpdir=" scratch)
                         "-Sdeps" (pr-str {:paths ["src" "test" "dev/experiments" control-root]})
                         "-M:clj-surgeon/test-deps" "-m" "portability-runner"])
                      [(name runtime) (str n) raw mode])
        _ (doseq [p [raw log]] (write-tracked! root originals p ""))
        builder (doto (ProcessBuilder. ^java.util.List command)
                  (.directory (io/file root)) (.redirectErrorStream true)
                  (.redirectOutput (safe-file root log)))
        ;; Each control owns its nested runner root; inheriting the parent's
        ;; sweep sentinel would let an exiting child delete the caller's root.
        _ (.remove (.environment builder) "CLJ_SURGEON_TMPDIR_REEXEC")
        _ (.put (.environment builder) "TMPDIR" scratch)
        _ (.put (.environment builder) "JAVA_TOOL_OPTIONS" (str "-Djava.io.tmpdir=" scratch))
        started (System/nanoTime)
        process (.start builder)
        _ (spawn/record! (.pid process) command)
        start-ticks (try
                      (with-open [r (java.io.RandomAccessFile. (str "/proc/" (.pid process) "/stat") "r")]
                        (let [stat (.readLine r)]
                          (Long/parseLong (nth (str/split (str/trim (subs stat (inc (.lastIndexOf stat ")")))) #"\s+") 19))))
                      (catch Exception _ nil))
        done? (.waitFor process 600 java.util.concurrent.TimeUnit/SECONDS)
        _ (when-not done? (.destroyForcibly process))
        exit (if done? (.exitValue process) 124)
        row (try (edn/read-string (slurp (safe-file root raw))) (catch Exception _ nil))
        result (assoc (or row {:namespace n :runtime runtime :status :process-failed})
                      :mode mode :exit exit :command command :subject subject :raw-receipt raw
                      :pid (.pid process) :start-ticks start-ticks
                      :wall-ms (/ (double (- (System/nanoTime) started)) 1000000.0))]
    (write-tracked! root originals (str prefix ".command.edn") (str (pr-str {:command command :subject subject}) "\n"))
    (write-tracked! root originals (str prefix ".control.edn") (str (pr-str result) "\n"))
    (when-not (= (:source-sha256 subject) (digest (read-source root (namespace-file n))))
      (refuse! :register-control-stale {:controls {runtime result} :reason :source-changed-during-execution}))
    result))

(defn- project-controls! [root originals n request controls]
  (let [path (str control-root "/portability-controls.edn")
        old (or (read-source root path) "{}\n")
        inventory (edn/read-string old)
        classification (cond (= :load-failed (get-in controls [:bb-load :status])) :bb-load-incompatible
                             (every? #(= :passed (get-in controls [% :status])) [:jvm :bb]) :portable
                             :else :non-portable)
        paths (assoc (control-paths n) :bb-load (str control-root "/controls/" n "-bb-load.edn"))
        entry (assoc paths :classification classification)
        existing (get inventory n)
        _ (when (and existing (not= existing entry))
            (refuse! :register-conflict {:file path :form n :actual existing :expected entry}))
        next-inventory (assoc inventory n entry)
        candidate (if existing old (z/root-string (-> (z/of-string old) (z/append-child n) (z/append-child entry))))
        md-path (str control-root "/portability-census.md")
        md (or (read-source root md-path)
               "# Portability census\n\nAll 0 assigned namespaces are listed.\n\n| Namespace | Assignment | Cadence | JVM | bb | Status / account |\n|---|---|---|---|---|---|\n\nSummary: {}\n")
        cell (fn [k]
               (if-let [r (controls k)]
                 (str "[" (name (:status r))
                      (when-let [v (:result r)] (str " (" (:test v) "/" (:fail v) "/" (:error v) ")"))
                      "](" (str/replace (paths k) (str control-root "/") "") ")") "not run"))
        row (str "| " n " | " (:runtime request) " | " (:lane request)
                 " | " (cell :jvm) " | " (cell (if (:bb controls) :bb :bb-load))
                 " | " (name classification) " |")
        lines (str/split-lines md)
        present (some #(str/starts-with? % (str "| " n " |")) lines)
        md (if present md
               (str/replace md "\n\nSummary:" (str "\n" row "\n\nSummary:")))
        md (-> md
               (str/replace #"All \d+ assigned namespaces are listed\."
                            (str "All " (count next-inventory) " assigned namespaces are listed."))
               (str/replace #"(?m)^Summary: .*" (str "Summary: " (pr-str (frequencies (map :classification (vals next-inventory)))))))]
    (when-not (str/includes? md row)
      (refuse! :register-conflict {:file md-path :form n :actual present :expected row}))
    (when-not (= old candidate) (write-tracked! root originals path candidate))
    (when-not (= (read-source root md-path) md) (write-tracked! root originals md-path md))))

(defn- rollback! [root originals]
  (doseq [[p old] @originals]
    (if (nil? old) (io/delete-file (safe-file root p) true) (spit (safe-file root p) old))))

;; INTENT: REGNS-009
;; @spec REGNS-009
(defn- acquire-lock! [root]
  (let [lock (safe-file root ".clj-surgeon-register.lock")
        holder {:pid (.pid (java.lang.ProcessHandle/current)) :root root}
        prepared (java.nio.file.Files/createTempFile
                   (.toPath (io/file root)) ".register-holder-" ".edn"
                   (into-array java.nio.file.attribute.FileAttribute []))]
    (try
      (spit (.toFile prepared) (pr-str holder))
      (try
        ;; Publish complete holder bytes and exclusive ownership together.
        (java.nio.file.Files/createLink (.toPath lock) prepared)
        lock
        (catch java.nio.file.FileAlreadyExistsException _
          (refuse! :register-busy
                   {:lock (str lock) :holder (edn/read-string (slurp lock))})))
      (finally (java.nio.file.Files/deleteIfExists prepared)))))

;; INTENT: REGNS-012
;; @spec REGNS-012 -- canonical root owns enrollment and control destinations.
(defn register! [root request]
  (let [root (.getCanonicalPath (io/file root))
        originals (atom {})
        acquired (atom nil)]
    (try
      ;; Invalid requests must not be masked by another registration's lock.
      (let [before (snapshot root (:namespace request))
            planned (plan before request)]
        (if-not (:ok planned) planned
          (do
            (reset! acquired (acquire-lock! root))
            (let [n (:namespace request)
                  previous (read-controls before n)]
              (when-not (= before (snapshot root n)) (refuse! :register-stale {}))
              (doseq [[p text] (:candidate planned)] (write-tracked! root originals p text))
              (let [subject {:source-sha256 (digest (before (namespace-file n))) :root root}
                    load-row (control! root originals n :bb "load" subject)
                    controls (cond-> {:bb-load load-row
                                      :jvm (control! root originals n :jvm "test" subject)}
                               (= :loaded (:status load-row))
                               (assoc :bb (control! root originals n :bb "test" subject)))
                    stale (stale-controls previous controls)]
                (when (seq stale)
                  (refuse! :register-control-stale {:stale stale :controls controls}))
                (when-not (and (every? control-provenance? (vals controls))
                               (= :passed (get-in controls [:jvm :status]))
                               (zero? (get-in controls [:jvm :exit] -1))
                               (controls-valid? n (:runtime request)
                                 (or (:bb-ineligible request) (get-in planned [:model :registration])) controls))
                  (refuse! :register-control-failed {:controls controls}))
                (project-controls! root originals n request controls)
                (let [result (oracle root request)]
                  (when-not (:ok result) (refuse! :register-incomplete result)))
                {:ok true :state (if (empty? @originals) :unchanged :registered)
                 :changes (:changes planned)
                 :artifacts (vec (sort (remove (set (keys (:candidate planned))) (keys @originals))))
                 :controls controls})))))
      (catch Exception e
        (when @acquired (rollback! root originals))
        (merge {:ok false :error (ex-message e)}
               (when @acquired {:state :rolled-back})
               (if (ex-data e) (ex-data e) {:error-type :register-io-failed})))
      (finally
        (when-let [lock @acquired] (io/delete-file lock true))))))

(def usage
  "make register-test-ns NS=clj-surgeon.foo-test LANE=battery RUNTIME=jvm [BB_INELIGIBLE='{:reasons #{:sci-host-interop} :detail \"reason\"}']\nAuthor ns lane metadata is required. Runs focused controls; repeats execute fresh controls and preserve enrollment bytes. Stale saved controls refuse after execution. Refusals exit nonzero.")

(defn -main [& args]
  (if (= ["--help"] (vec args))
    (println usage)
    (let [result (try
                   (when (seq args) (refuse! :register-invalid-request {:arguments args}))
                   (register! "." {:namespace (symbol (or (System/getenv "NS") ""))
                                   :lane (keyword (or (System/getenv "LANE") ""))
                                   :runtime (keyword (or (System/getenv "RUNTIME") ""))
                                   :bb-ineligible (when-let [s (not-empty (System/getenv "BB_INELIGIBLE"))]
                                                    (edn/read-string s))})
                   (catch Exception e {:ok false :error-type :register-invalid-request :error (ex-message e)}))]
      (prn result)
      (shutdown-agents)
      (System/exit (if (:ok result) 0 1)))))
