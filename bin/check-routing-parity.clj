#!/usr/bin/env bb
;; Routing-table parity guard.
;;
;; The decision table is COPIED BY HAND into every prompt surface. Nothing
;; generates it, so nothing but this check stops one copy from drifting into a
;; different rule than the seat next to it. It asserts:
;;   1. every rendering's table is BYTE-IDENTICAL to the canonical section's ---
;;      the FIVE table copies listed in `renderings`, and nothing else;
;;   2. no rendering carries a byte-order mark the canonical file does not;
;;   3. the managed plate's pointer heading really exists in the canonical file;
;;   4. every document the plate cites exists on disk.
;;
;; BYTES, NOT LINES (Sol fence r4, finding 2). This used `str/split-lines`,
;; which discards exactly the differences a hand-copy introduces: a stray CR
;; before a newline, trailing spaces, a BOM. Sol added one 0x0d byte to a
;; rendering and the checker stayed green. The region is now compared as a RAW
;; BYTE SLICE: the file is read as bytes and carried through ISO-8859-1, which
;; round-trips every byte to exactly one char, so line-finding never normalizes
;; anything. No split-lines, no trim, no decoding on the comparison path.
;;
;; SCOPE LIMIT. The compact plate (`resources/clj-surgeon-agent-routing.md`) is a
;; REVIEWED SUMMARY that points at the canonical section --- it is deliberately
;; not a byte-parity rendering of the table, and no byte comparison is made
;; against it. Only its pointer heading and its citations are checked here; that
;; its prose still says what the canonical section says is established by review,
;; not by this gate.
;; Run: bb bin/check-routing-parity.clj   (exit 0 green, exit 2 on drift)

(require '[clojure.string :as str]
         '[babashka.fs :as fs])

(def canonical "skills/clj-surgeon/SKILL.md")

(def renderings
  ["skill.md"
   "skills/safe-refactor/SKILL.md"
   "CLAUDE.md"
   "AGENTS.md"
   "docs/observations/2026-09-06-routing-prompt-surfaces.md"])

(def plate "resources/clj-surgeon-agent-routing.md")

(def pointer-heading "## Edit routing (policy revision 1, 2026-09-06)")

(def ^:private bom "ï»¿") ; EF BB BF, as bytes carried through ISO-8859-1

(defn file-bytes
  "The file's exact bytes, one byte per char. ISO-8859-1 is the only charset
   that round-trips arbitrary bytes, so every comparison below is a BYTE
   comparison wearing a string's clothes -- no decoding, no normalization."
  [path]
  (String. (java.nio.file.Files/readAllBytes (.toPath (java.io.File. (str path))))
           "ISO-8859-1"))

(def ^:private header "| Situation | Route |")

(defn table
  "The RAW BYTE SLICE of the contiguous run of table rows beginning at the
   decision table's header. Lines are located by splitting on \\n ALONE, so a
   \\r stays inside the row's bytes and a trailing space is never trimmed."
  [path]
  (let [text (file-bytes path)
        idx (loop [from 0]
              (let [i (.indexOf text header (int from))]
                (cond
                  (neg? i) nil
                  (or (zero? i) (= \newline (.charAt text (dec i)))) i
                  :else (recur (inc i)))))]
    (when idx
      (loop [end idx]
        (let [nl (.indexOf text "\n" (int end))
              line-end (if (neg? nl) (count text) nl)
              next-start (if (neg? nl) (count text) (inc nl))
              more? (and (< next-start (count text))
                         (= \| (.charAt text next-start)))]
          (if more?
            (recur next-start)
            (subs text idx line-end)))))))

(defn- byte-difference
  "The first differing byte, with its index and both values in hex -- the only
   report that can name a difference `split-lines` used to erase."
  [expected actual]
  (let [n (min (count expected) (count actual))
        i (first (remove nil? (map (fn [i e a] (when (not= e a) i))
                                   (range n) expected actual)))]
    (if i
      {:byte-index i
       :canonical-byte (format "0x%02x" (int (.charAt ^String expected i)))
       :rendered-byte (format "0x%02x" (int (.charAt ^String actual i)))
       :canonical-context (pr-str (subs expected (max 0 (- i 20)) (min (count expected) (+ i 20))))
       :rendered-context (pr-str (subs actual (max 0 (- i 20)) (min (count actual) (+ i 20))))}
      {:byte-index n
       :problem "one region is a prefix of the other"
       :canonical-bytes (count expected)
       :rendered-bytes (count actual)})))

(defn -main []
  (let [expected (table canonical)
        failures (atom [])
        fail! (fn [m] (swap! failures conj m))]
    (when-not expected
      (fail! {:check :canonical-table-present :path canonical
              :problem "no `| Situation | Route |` table found in the canonical section"}))
    (doseq [path renderings]
      (let [actual (table path)]
        (cond
          (nil? actual)
          (fail! {:check :table-present :path path
                  :problem "no routing decision table found"})
          (not= expected actual)
          (fail! {:check :table-parity :path path
                  :problem "routing table bytes differ from the canonical section"
                  :canonical-bytes (count expected)
                  :rendered-bytes (count actual)
                  :first-difference (byte-difference expected actual)})))
      (when (not= (str/includes? (file-bytes canonical) bom)
                  (str/includes? (file-bytes path) bom))
        (fail! {:check :byte-order-mark :path path
                :problem "this rendering's byte-order mark does not match the canonical file's"
                :canonical-has-bom (str/includes? (file-bytes canonical) bom)
                :rendered-has-bom (str/includes? (file-bytes path) bom)})))
    (let [plate-text (slurp plate)
          canonical-text (slurp canonical)]
      (when-not (str/includes? canonical-text pointer-heading)
        (fail! {:check :plate-pointer-resolves :path canonical
                :problem (str "the plate points at heading " (pr-str pointer-heading)
                              " but the canonical file does not contain it")}))
      (when-not (str/includes? plate-text "Edit routing (policy revision 1, 2026-09-06)")
        (fail! {:check :plate-names-the-heading :path plate
                :problem "the plate no longer names the canonical section heading"}))
      (doseq [cited (re-seq #"docs/observations/[A-Za-z0-9._-]+\.md" plate-text)]
        (when-not (fs/exists? cited)
          (fail! {:check :plate-citation-resolves :path plate
                  :problem (str "the plate cites " cited ", which does not exist")}))))
    (if (seq @failures)
      (do (prn {:ok false :operation :check-routing-parity
                :canonical canonical :failures @failures})
          (System/exit 2))
      (prn {:ok true :operation :check-routing-parity
            :canonical canonical
            :table-bytes (count expected)
            :table-rows (inc (count (re-seq #"\n" expected)))
            :renderings-checked (count renderings)
            :plate plate
            :pointer-heading pointer-heading}))))

(-main)
