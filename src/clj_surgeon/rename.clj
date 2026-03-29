(ns clj-surgeon.rename
  "Rename a namespace prefix across an entire project.
   Walks the AST to find ns declarations and :require entries,
   renames structurally (not text replace), computes file moves.

   ALL PLANNING FUNCTIONS ARE PURE. Only `execute!` has side effects."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clojure.string :as str]
            [clojure.java.io :as io]))

;; ============================================================
;; Pure: Rename symbols within a namespace form's AST
;; ============================================================

(defn- rename-symbol-str
  "If a symbol string starts with `from-prefix`, replace with `to-prefix`.
   E.g., (rename-symbol-str \"ns-surgeon.core\" \"ns-surgeon\" \"clj-surgeon\")
         => \"clj-surgeon.core\""
  [s from-prefix to-prefix]
  (if (or (= s from-prefix)
          (str/starts-with? s (str from-prefix ".")))
    (str to-prefix (subs s (count from-prefix)))
    s))

(defn- ns-prefix->dir
  "Convert a namespace prefix to its directory path.
   \"ns-surgeon\" => \"ns_surgeon\", \"clj-surgeon\" => \"clj_surgeon\""
  [prefix]
  (str/replace prefix "-" "_"))

(defn- find-clj-files
  "Recursively find all .clj files under a root directory."
  [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile %))
       (filter #(re-matches #".*\.clj[csx]?$" (.getName %)))
       (map #(.getPath %))
       sort
       vec))

;; ============================================================
;; Pure: Analyze a single file for renames needed
;; ============================================================

(defn- find-ns-form
  "Find the (ns ...) form in a zipper. Returns the zloc or nil."
  [zloc]
  (loop [z zloc]
    (when z
      (if (and (z/list? z)
               (= "ns" (some-> z z/down z/string)))
        z
        (recur (z/right z))))))

(defn- ns-name-str
  "Get the namespace name string from an (ns ...) form."
  [ns-zloc]
  (some-> ns-zloc z/down z/right z/string))

(defn analyze-file
  "Analyze a single .clj file for namespace renames needed.
   Returns {:file :ns-rename :require-renames} or nil if no changes needed.
   PURE — reads file but does not modify it."
  [file from-prefix to-prefix]
  (let [source (slurp file)
        zloc (z/of-string source {:track-position? true})
        ns-zloc (find-ns-form zloc)
        ns-name (when ns-zloc (ns-name-str ns-zloc))
        ;; Check if ns declaration needs renaming
        ns-rename (when (and ns-name
                             (or (= ns-name from-prefix)
                                 (str/starts-with? ns-name (str from-prefix "."))))
                    {:old ns-name
                     :new (rename-symbol-str ns-name from-prefix to-prefix)})
        ;; Find all require entries that reference the old prefix
        require-renames (when ns-zloc
                          (let [children (loop [z (z/down ns-zloc), acc []]
                                           (if (nil? z)
                                             acc
                                             (recur (z/right z)
                                                    (conj acc z))))]
                            (->> children
                                 (filter #(and (z/list? %)
                                               (= ":require" (some-> % z/down z/string))))
                                 (mapcat (fn [req-form]
                                           ;; Walk require vectors
                                           (loop [z (some-> req-form z/down z/right), acc []]
                                             (if (nil? z)
                                               acc
                                               (let [entry (when (z/vector? z)
                                                             (let [first-sym (some-> z z/down z/string)]
                                                               (when (and first-sym
                                                                          (or (= first-sym from-prefix)
                                                                              (str/starts-with? first-sym (str from-prefix "."))))
                                                                 {:old first-sym
                                                                  :new (rename-symbol-str first-sym from-prefix to-prefix)})))]
                                                 (recur (z/right z)
                                                        (if entry (conj acc entry) acc)))))))
                                 vec
                                 not-empty)))]
    (when (or ns-rename require-renames)
      {:file file
       :ns-rename ns-rename
       :require-renames (or require-renames [])})))

;; ============================================================
;; Pure: Build the complete rename plan
;; ============================================================

(defn plan
  "Build a complete rename plan for a project.
   Returns EDN with all renames, file moves, and non-clj files to review.
   PURE — no side effects."
  [{:keys [from to root]
    :or {root "."}}]
  (let [from-str (str from)
        to-str (str to)
        src-dirs (filter #(.isDirectory (io/file %))
                         [(str root "/src") (str root "/test")])
        clj-files (mapcat find-clj-files src-dirs)
        ;; Analyze each file
        file-analyses (->> clj-files
                           (keep #(analyze-file % from-str to-str))
                           vec)
        ;; Compute file moves (directory renames based on ns prefix)
        from-dir (ns-prefix->dir from-str)
        to-dir (ns-prefix->dir to-str)
        file-moves (->> clj-files
                        (filter #(str/includes? % (str "/" from-dir "/")))
                        (mapv (fn [f]
                                {:from f
                                 :to (str/replace f
                                                  (str "/" from-dir "/")
                                                  (str "/" to-dir "/"))})))
        ;; Find non-clj files that might reference the old name
        all-files (->> (file-seq (io/file root))
                       (filter #(.isFile %))
                       (remove #(str/includes? (.getPath %) "/.git/"))
                       (remove #(re-matches #".*\.clj[csx]?$" (.getName %))))
        non-clj-matches (->> all-files
                             (filter (fn [f]
                                       (try
                                         (let [content (slurp f)]
                                           (or (str/includes? content from-str)
                                               (str/includes? content from-dir)))
                                         (catch Exception _ false))))
                             (mapv #(.getPath %)))]
    {:from from-str
     :to to-str
     :file-analyses file-analyses
     :file-moves file-moves
     :non-clj-files non-clj-matches
     :summary {:files-to-update (count file-analyses)
               :files-to-move (count file-moves)
               :non-clj-to-review (count non-clj-matches)}}))

;; ============================================================
;; Pure: Apply renames to source text (returns new source string)
;; ============================================================

(defn rename-source
  "Apply namespace renames to a source string. Returns the new source.
   PURE — string in, string out."
  [source from-prefix to-prefix]
  (let [zloc (z/of-string source {:track-position? true})
        ns-zloc (find-ns-form zloc)]
    (if (nil? ns-zloc)
      source
      ;; Walk the entire ns form and rename matching symbols
      (let [renamed (loop [z ns-zloc]
                      (let [z' (z/next z)]
                        (if (z/end? z')
                          z'
                          (if (and (= :token (n/tag (z/node z')))
                                   (let [s (z/string z')]
                                     (and (not (str/starts-with? s ":"))
                                          (not (str/starts-with? s "\""))
                                          (or (= s from-prefix)
                                              (str/starts-with? s (str from-prefix "."))))))
                            (recur (z/replace z' (n/token-node
                                                  (symbol (rename-symbol-str
                                                           (z/string z')
                                                           from-prefix to-prefix)))))
                            (recur z')))))]
        (z/root-string renamed)))))

;; ============================================================
;; Effects: Execute the rename plan
;; ============================================================

(defn execute!
  "Execute a rename plan. Side effects: moves files, rewrites source.
   Returns a log of actions taken."
  [{:keys [from to root]
    :or {root "."}}]
  (let [rename-plan (plan {:from from :to to :root root})
        from-str (str from)
        to-str (str to)
        from-dir (ns-prefix->dir from-str)
        to-dir (ns-prefix->dir to-str)
        log (atom [])]
    ;; 1. Create target directories
    (doseq [{:keys [to]} (:file-moves rename-plan)]
      (let [parent (.getParentFile (io/file to))]
        (when-not (.exists parent)
          (.mkdirs parent)
          (swap! log conj {:action :mkdir :path (.getPath parent)}))))
    ;; 2. Rename source in each file and write to new location (or same location)
    (doseq [{:keys [from to]} (:file-moves rename-plan)]
      (let [source (slurp from)
            new-source (rename-source source from-str to-str)]
        (spit to new-source)
        (swap! log conj {:action :write :from from :to to})))
    ;; 3. Update non-moved files that have require changes
    (let [moved-files (set (map :from (:file-moves rename-plan)))]
      (doseq [{:keys [file]} (:file-analyses rename-plan)
              :when (not (contains? moved-files file))]
        (let [source (slurp file)
              new-source (rename-source source from-str to-str)]
          (when (not= source new-source)
            (spit file new-source)
            (swap! log conj {:action :update :file file})))))
    ;; 4. Delete old directories if empty
    (doseq [src-dir ["src" "test"]]
      (let [old-dir (io/file root src-dir from-dir)]
        (when (.isDirectory old-dir)
          (let [remaining (seq (.listFiles old-dir))]
            (when-not remaining
              (.delete old-dir)
              (swap! log conj {:action :rmdir :path (.getPath old-dir)}))))))
    {:actions @log
     :non-clj-files (:non-clj-files rename-plan)
     :summary {:actions-taken (count @log)
               :non-clj-to-review (count (:non-clj-files rename-plan))}}))
