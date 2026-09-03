(ns clj-surgeon.memory.fixture
  "A synthetic scope large enough to make the frozen read run out of heap.

   The scope is deterministic: every file is the same body with a different
   namespace name and index constant, so a reference implementation can be
   hand-derived without running either arm. Generation is one templated string
   reused for every file, so a three-hundred-megabyte scope costs one buffer
   and the write."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.file Files)))

(def old-lib
  "The namespace the scenario retires."
  "scope.old.lib")

(def new-lib
  "The namespace the scenario migrates to."
  "scope.new.lib")

(defn- body-block
  [block-index]
  (str "(defn handler-" block-index "\n"
       "  \"Block " block-index " of the generated namespace. The body is dense\n"
       "  enough that the parser builds a real node tree rather than one token.\"\n"
       "  [request options]\n"
       "  (let [route (ol/route request)\n"
       "        limit (get options :limit " block-index ")\n"
       "        window (into [] (map (fn [entry] (ol/decorate entry limit)))\n"
       "                     (ol/entries request))]\n"
       "    {:route route\n"
       "     :limit limit\n"
       "     :window window\n"
       "     :tag :block-" block-index "}))\n\n"))

(defn- template-body
  "One reusable body of at least `bytes` characters, built once per generation."
  [bytes]
  (let [builder (StringBuilder. (int (* 1.1 bytes)))]
    (loop [block 0]
      (when (< (.length builder) bytes)
        (.append builder ^String (body-block block))
        (recur (inc block))))
    (.toString builder)))

(defn file-source
  "The exact source of generated file `index` given a shared template body."
  [index body]
  (str "(ns scope.generated.ns" index "\n"
       "  \"Generated namespace " index " in the synthetic memory scope.\"\n"
       "  (:require\n"
       "   [" old-lib " :as ol]\n"
       "   [clojure.string :as str]))\n\n"
       "(def namespace-index " index ")\n\n"
       body))

(defn generate-scope!
  "Write `files` generated namespaces of about `bytes-per-file` under `root`.

   Returns the manifest facts a caller needs to state a pass line: the root,
   the file count and the exact aggregate byte count."
  [root {:keys [files bytes-per-file] :or {files 600 bytes-per-file (* 512 1024)}}]
  (let [dir (io/file root "src" "scope" "generated")
        _ (.mkdirs dir)
        body (template-body bytes-per-file)
        total (reduce
                (fn [acc index]
                  (let [source (file-source index body)
                        target (io/file dir (str "ns" index ".clj"))]
                    (spit target source)
                    (+ acc (count (.getBytes source "UTF-8")))))
                0
                (range files))]
    {:root (.getPath (io/file root))
     :files files
     :bytes total}))

(defn scope-files
  "Every generated file under `root`, in sorted path order."
  [root]
  (->> (io/file root "src" "scope" "generated")
       (.listFiles)
       (map #(.getPath ^java.io.File %))
       (filter #(str/ends-with? % ".clj"))
       (sort)
       (vec)))

(defn delete-tree!
  [root]
  (let [file (io/file root)]
    (when (.exists file)
      (doseq [child (reverse (file-seq file))]
        (Files/deleteIfExists (.toPath child))))))
