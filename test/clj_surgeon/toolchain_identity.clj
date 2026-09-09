(ns clj-surgeon.toolchain-identity
  "A VERSION IS NOT A BANNER.

   Frame 7 section 4, verbatim: \"`Picked up JAVA_TOOL_OPTIONS...` is **not** a
   Java version. Capture stderr/stdout and parsed version separately, require
   successful commands, and refuse missing or ambiguous identity.\"

   The defect this closes is on disk. `/var/tmp/forge/ship/20260909T080543Z-2dfbc286fed9/gates-prewarm.edn`
   records

     :toolchain-java \"Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge\"

   because the writer took `head -1` of `java -version` and this box exports
   JAVA_TOOL_OPTIONS. Every landing then agreed with every other landing about
   a fact none of them had observed: the check compared one banner to another
   banner and passed on a box with no Java at all, as long as the environment
   variable was set. That is a control with zero operating effectiveness.

   So: run the command, keep stdout and stderr as captured bytes, take the
   FIRST NON-BANNER line as the version, keep the banners as `:diagnostic`, and
   make a failed or banner-only command `:status :unknown` -- never a version."
  (:require
   [babashka.process :as proc]
   [clojure.string :as str]))

(defn sha256
  "Lowercase hex SHA-256 of `s` as UTF-8; identical under JVM Clojure and bb."
  [^String s]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")]
    (->> (.digest md (.getBytes (str s) "UTF-8"))
         (map #(format "%02x" %))
         (apply str))))

(def banner-prefixes
  "Lines a launcher prints BEFORE the thing you asked for."
  ["Picked up JAVA_TOOL_OPTIONS"
   "Picked up _JAVA_OPTIONS"
   "Picked up JAVA_OPTIONS"
   "OpenJDK 64-Bit Server VM warning"
   "Warning:"
   "WARNING:"
   "Note:"])

(defn banner-line? [line]
  (boolean (some #(str/starts-with? (str/triml (str line)) %) banner-prefixes)))

(defn parse-version
  "The first non-banner, non-blank line of `out`, trimmed; nil when there is
   none. nil is a REFUSAL upstream, never an empty version string."
  [out]
  (some-> (->> (str/split-lines (str out))
               (remove str/blank?)
               (remove banner-line?)
               first)
          str/trim))

(defn diagnostics
  "The banner lines, kept so nothing is discarded and nothing is promoted."
  [out]
  (->> (str/split-lines (str out))
       (remove str/blank?)
       (filterv banner-line?)))

(defn capture
  "Run `argv` and return a structured identity record:

     {:argv [...] :exit 0 :status :ok|:unknown
      :version \"openjdk version ...\"        ; parsed, non-banner
      :diagnostic [\"Picked up JAVA_TOOL_OPTIONS: ...\"]
      :stdout-sha256 ... :stderr-sha256 ...
      :stdout-first \"...\" :stderr-first \"...\"}

   `:status :ok` requires exit 0 AND a parsed version. Everything else is
   `:unknown` with `:reason`, and an `:unknown` toolchain entry can never
   discharge a runtime obligation."
  [argv]
  (let [{:keys [exit out err]}
        (try @(apply proc/process {:out :string :err :string} argv)
             (catch Exception e {:exit -1 :out "" :err (str (ex-message e))}))
        combined (str out "\n" err)
        version (parse-version combined)]
    (cond-> {:argv (vec argv)
             :exit exit
             :stdout-sha256 (sha256 out)
             :stderr-sha256 (sha256 err)
             :stdout-bytes (count (.getBytes (str out) "UTF-8"))
             :stderr-bytes (count (.getBytes (str err) "UTF-8"))
             :diagnostic (diagnostics combined)}
      (and (= 0 exit) (some? version))
      (assoc :status :ok :version version)

      (not (and (= 0 exit) (some? version)))
      (assoc :status :unknown
             :reason (cond (not= 0 exit) :command-failed
                           (nil? version) :banner-only-output
                           :else :ambiguous)))))

(def gate-toolchain
  "The tools the landing gate's own recipes actually invoke, plus the ones
   section 4 names for a landing receipt. `~/bin/clj-kondo` is the PAVED
   entrance and is recorded by that path on purpose."
  [[:java ["java" "-version"]]
   [:clojure ["clojure" "--version"]]
   [:bb ["bb" "--version"]]
   [:kondo [(str (System/getProperty "user.home") "/bin/clj-kondo") "--version"]]
   [:python ["python3" "--version"]]
   [:git ["git" "--version"]]
   [:make ["make" "--version"]]
   ;; SOL-EC-004: the RESOLVED EXECUTABLE PATH, not the word "sh". The old form
   ;; printed `$0` first, so the recorded shell identity was the string "sh" --
   ;; identical on every box, and the resolved path on the second line was
   ;; discarded. An identity that cannot distinguish two shells is not one.
   [:sh ["sh" "-c" "readlink -f /bin/sh 2>/dev/null || command -v sh"]]
   [:swipl ["swipl" "--version"]]])

(defn snapshot
  "Every gate tool captured once. Returns
   `{:manifest-sha256 <over the ok versions> :tools {...} :unknown [...]}`.
   `:unknown` is the list a consumer refuses on -- present, named, and never
   an empty map that reads like agreement."
  []
  (let [tools (into {} (for [[k argv] gate-toolchain] [k (capture argv)]))
        unknown (vec (sort (map name (keep (fn [[k v]] (when (= :unknown (:status v)) k)) tools))))]
    {:manifest-sha256 (sha256 (pr-str (into (sorted-map)
                                            (for [[k v] tools] [k (:version v)]))))
     :unknown unknown
     :tools tools}))
