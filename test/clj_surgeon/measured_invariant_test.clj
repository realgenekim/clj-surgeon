(ns clj-surgeon.measured-invariant-test
  "The ruling as an INVARIANT: a measured field enters a receipt ONLY through
  the partition, and no `System/exit` lives outside a CLI entrypoint.

  The first landing made the ruling true at four sites. A review then showed
  the same measured data reaching the hash subject by a SECOND route — the
  shared MCP operation finalizer attached its wall-clock reading as a top-level
  `:elapsed_ms`, and `hashed-channel`, which removes only a block keyed
  `:measured`, carried it straight through:

      {:public-result   {:ok true, :receipt {:stable :fact}, :elapsed_ms 2.5},
       :hashed-channel  {:ok true, :receipt {:stable :fact}, :elapsed_ms 2.5},
       :elapsed-survives-hash true}

  A site fix answers one site. What follows is the rule instead:

  1. the publication boundary partitions every measured field it publishes;
  2. a SOURCE SCAN enumerates every clock read in `src/` and classifies it, so
     a clock site nobody classified fails this file rather than shipping;
  3. the parity hash of a real public result is stable across two runs whose
     clocks tick differently;
  4. `System/exit` appears only inside a `-main`, because a library that exits
     kills the caller — and the caller is the MCP server.

  @spec MCP-OP-MEM-003
  @spec MCP-OP-MEM-005
  @spec MCP-OP-MEM-011
  @spec MCP-OP-TIME-005
  @spec MCP-OP-EXIT-001"
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.measured :as measured]))

;; ============================================================
;; A source scanner: which top-level form does a line sit in
;; ============================================================

(def scanned-roots
  "Every directory whose Clojure this invariant governs.

  `dev/experiments` is here because the round-three review found it (§3,
  residual a): it is on the `:clj-surgeon/mcp-test` classpath (`deps.edn`), its
  capture servers REPLACE the `:tool-fn` of a registered production tool, and
  yet being outside `src/` it was reached by no scan in this file. Three of
  them read `System/nanoTime` and published a top-level `elapsed_ms` straight
  to the SDK callback, bypassing the finalizer entirely — so every arm-run
  corpus they captured was invalid against the canonical output schema, and
  the sweep table that claimed \"every other in-repository elapsed_ms reader
  was swept\" had no `dev/` row. A scan whose boundary is a directory name
  rather than a classpath is a scan with a blind spot in it."
  ["src" "dev/experiments"])

(defn- src-files
  ([] (mapcat src-files scanned-roots))
  ([root]
   (->> (file-seq (io/file root))
        (filter #(.isFile ^java.io.File %))
        (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
        (sort-by #(.getPath ^java.io.File %)))))

(defn- site-path
  "`file` named as it would be named under `src/`, whatever root it was read
   from — so a scan of a scratch copy is comparable with a scan of the tree.

  A file under one of the real `scanned-roots` keeps the name it already has;
  only a scratch root is rewritten."
  [^java.io.File file root]
  (let [path (.getPath file)]
    (cond
      (some #(str/starts-with? path (str % "/")) scanned-roots) path
      (str/starts-with? path (str root "/")) (str "src/" (subs path (inc (count root))))
      :else path)))

(defn- sites
  "Every line of `file` matching `pattern`, named by the top-level form it sits
   in. Line numbers are deliberately NOT part of the identity: an inventory
   pinned to line numbers has to be re-blessed on every unrelated edit, and one
   that is re-blessed reflexively stops being a ratchet.

   `;;` comments are cut before matching: a comment EXPLAINING why a call was
   removed must not read as the call.

   The hit count is a count of CALLS, `(count (re-seq pattern code))`, not of
   matching lines. Round-six review §6: this folded one hit per matching line,
   so a line that already matched absorbed an unlimited number of further clock
   reads or laundering calls without moving the declared number, and the
   reviewer found the shape live in the tree at
   `worktree_lifecycle/valid-future-expiry?` -- two `Instant/parse` reads on one
   line, declared 1, green. `re-seq` also counts the SAME spelling twice, which
   is the case the reviewer's own example is and which a `distinct` over
   alternatives would still get wrong."
  ([^java.io.File file pattern] (sites file pattern "src"))
  ([^java.io.File file pattern root]
   (:hits
    (reduce (fn [{:keys [form hits]} line]
              (let [code (or (first (str/split line #";;")) "")
                    form' (if (str/starts-with? line "(def")
                            (second (str/split (str/trim line) #"[\s\[]+"))
                            form)]
                {:form form'
                 :hits (into hits
                             (repeat (count (re-seq pattern code))
                                     [(site-path file root) form']))}))
            {:form nil :hits []}
            (str/split-lines (slurp file)))))) 

(def ^:private measured-namespace-file
  "The ONE file allowed to read a clock raw."
  "src/clj_surgeon/measured.clj")

(def ^:private sanctioned-measured-require
  "The ONLY way `src/` may name the measured namespace.

  Not a style rule. Every scan in this file is written against the `measured/`
  spelling, so any OTHER way of naming the namespace is a hole in every scan at
  once — which is how the round-three review's second bypass worked
  (2026-09-04 §1b): a brand-new namespace requiring
  `[clj-surgeon.measured :as measured :refer [raw-nanos]]` satisfied the alias
  witness, called `(raw-nanos)` under a bare name the clock scan does not
  match, published the result under an undeclared key, and left all nine tests
  green with the field in the parity hash.

  So the rule is the whole line: one alias, no `:refer`, no `:use`, and no
  fully-qualified `clj-surgeon.measured/...` call either."
  "[clj-surgeon.measured :as measured]")

(defn- scan
  "`{[path form] hits}` over every `.clj` under the scanned roots EXCEPT the
   measured namespace itself, which is where the raw reads are allowed to live."
  ([pattern]
   (apply merge-with + {} (map #(scan pattern %) scanned-roots)))
  ([pattern root]
   (frequencies
    (mapcat #(sites % pattern root)
            (remove #(= measured-namespace-file (site-path % root))
                    (src-files root))))))

(def ^:private clock-source-class-roots
  "The places a program can obtain a time FROM NOTHING, named as strings.

  Strings, not class symbols, because babashka's sci exposes only an
  allow-listed set of class symbols -- `java.util.Calendar` does not resolve
  there at all -- while `Class/forName` reaches every class in both runtimes.

  This is a list of ROOTS, not of classes: everything else is derived by
  closure below. The round-five review's finding 3 (2026-09-04 §3) is that the
  previous `clock-source-classes` was itself a hand-written list of eleven
  classes, so `java.time.OffsetDateTime` -- already trusted enough to sit in
  the RETURN types -- was never a SOURCE, and `java.util.Calendar` was in
  neither. Four ordinary spellings fell outside all 46 alternatives."
  ["java.lang.System"
   "java.io.File"
   "java.nio.file.Files"
   "java.nio.file.attribute.BasicFileAttributes"
   "java.nio.file.attribute.FileTime"
   "java.time.Clock"
   "java.time.Instant"
   "java.util.Date"
   "java.util.Calendar"
   "java.sql.Timestamp"])

(defn- class-named
  "`Class/forName`, or nil. Nil is a real answer: `java.sql.Timestamp` need not
   be on every classpath this file runs on."
  [^String n]
  (try (Class/forName n) (catch Throwable _ nil)))

(def ^:private time-value-parents
  "What makes a class a TIME VALUE rather than merely a number holder.

  `java.time.temporal.Temporal` is the JDK's own answer for `java.time`, and it
  deliberately EXCLUDES `Duration` and `Period`, which are `TemporalAmount`s --
  an amount is a literal constant in a timeout, not a clock read, and
  `(Duration/ofSeconds 3)` appears three times in `src/` as exactly that."
  (keep class-named ["java.time.temporal.Temporal"
                     "java.util.Date"
                     "java.util.Calendar"
                     "java.time.Clock"
                     "java.nio.file.attribute.FileTime"]))

(defn- time-type?
  [^Class c]
  (boolean (and c (not (.isPrimitive c))
                (some #(.isAssignableFrom ^Class % c) time-value-parents))))

(def ^:private clock-source-classes
  "Every JDK class a program reads a TIME from, DERIVED by closure from the
   roots: a class is a clock source when a clock source hands one back."
  (loop [seen (set (keep class-named clock-source-class-roots))
         queue (vec seen)]
    (if (empty? queue)
      seen
      (let [^Class c (peek queue)
            found (->> (.getMethods c)
                       (map #(.getReturnType ^java.lang.reflect.Method %))
                       (filter time-type?)
                       distinct
                       (remove seen))]
        (recur (into seen found) (into (pop queue) found))))))

(def ^:private clock-numeric-return-types
  "A raw epoch or duration number. `File/length` and `Files/size` also return
   `long` and are not clock reads, which is what the name fragments are for."
  #{Long/TYPE Integer/TYPE})

(def ^:private clock-name-fragments
  "The morphemes a JDK time accessor is spelled with. Case-carrying fragments
   (`odified`, `poch`, `ano`, `illis`) match both `lastModified` and
   `getLastModifiedTime` without a case-insensitive match that would drag in
   every `now`-containing identifier in the tree."
  ["time" "Time" "now" "Instant" "instant" "illis" "ano" "poch" "odified"
   "Date" "Clock" "clock" "ystem"])

(defn- derived-clock-expressions
  "Every spelling of a clock read, derived from `clock-source-classes`:

  - a STATIC returning a time value is a factory and needs no clock morpheme in
    its name -- that is what makes `Calendar/getInstance` and
    `OffsetDateTime/now` visible;
  - any method returning a raw `long`/`int` needs a clock morpheme, so
    `File/length` stays out;
  - an INSTANCE method returning a time value needs one too, so `.plus`,
    `.with` and `.parse` do not become alternatives;
  - a public ZERO-ARGUMENT CONSTRUCTOR of a time value is a clock read that
    `.getMethods` can never see -- `(java.util.Date.)` -- and is spelled
    `SimpleName.`;
  - every static spelling is ALSO emitted as the dot special form,
    `(. Class method`, whose text contains no `Class/method` at all;
  - every method name is ALSO emitted as a QUOTED STRING, and every source
    class as its FULLY-QUALIFIED NAME in quotes, because a reflective read
    spells neither as a source token: round-six review findings 2 and 3 showed
    `(.getMethod (Class/forName \"java.lang.System\") \"nanoTime\" ...)` publishing
    a sixteen-digit monotonic clock value in a receipt field inside the hashed
    parity subject with the 159-spelling derivation green, and `Class/forName`
    cannot be reached without the qualified name as a string."
  []
  (vec
   (sort
    (distinct
     (concat
      (for [^Class c clock-source-classes
            ^java.lang.reflect.Method m (.getMethods c)
            :let [nm (.getName m)
                  rt (.getReturnType m)
                  static? (java.lang.reflect.Modifier/isStatic (.getModifiers m))
                  morpheme? (some #(str/includes? nm %) clock-name-fragments)]
            :when (or (and static? (time-type? rt))
                      (and (contains? clock-numeric-return-types rt) morpheme?)
                      (and (time-type? rt) morpheme?))
            spelling (concat
                       (if static?
                         [(str (.getSimpleName c) "/" nm)
                          (str "(. " (.getSimpleName c) " " nm)
                          ;; `..` expands to the dot special form with a
                          ;; PARENTHESISED member, which is the grammar
                          ;; round-eight review finding 1 found unmatched.
                          (str "(.. " (.getSimpleName c) " " nm)]
                         [(str "." nm)])
                       ;; `memfn` names the member as an ordinary source token
                       ;; and expands to the parenthesised member form. It is
                       ;; spelled for STATIC and INSTANCE names alike: `memfn`
                       ;; itself appears nowhere in this repository, so the
                       ;; alternative collides with nothing.
                       [(str "(memfn " nm)]
                       ;; The STRING form only for a name that CARRIES A CLOCK
                       ;; MORPHEME. `nanoTime`, `currentTimeMillis` and `now`
                       ;; do; the bare factory names a static-returning-a-time
                       ;; also produces do not, and `Instant/from` as the four
                       ;; characters `"from"` is a JSON schema key in
                       ;; `mcp_schema.clj` and five `dev/experiments` files, not
                       ;; a clock read. A morpheme-free name is still closed on
                       ;; the reflective route, and closed at the tighter end of
                       ;; it: `Class/forName` cannot reach `Calendar/getInstance`
                       ;; without the string `"java.util.Calendar"`, which the
                       ;; class alternative below carries and which collides with
                       ;; nothing.
                       (when morpheme? [(str "\"" nm "\"")]))]
        spelling)
      ;; The class, named the only way `Class/forName` accepts it.
      (for [^Class c clock-source-classes]
        (str "\"" (.getName c) "\""))
      ;; And the class as a BARE SOURCE SYMBOL -- round-eight review finding 2.
      ;; The morpheme narrowing's argument for withholding a string form from a
      ;; morpheme-free method name was that `Class/forName` cannot reach
      ;; `Calendar/getInstance` without the string `"java.util.Calendar"`. True,
      ;; and `Class/forName` is not the only route to a class: on the JVM a
      ;; class IS an ordinary source symbol, so
      ;; `(.getMethod java.util.Calendar "getInstance" (into-array Class []))`
      ;; reaches the factory with NEITHER the class (bare) nor the method
      ;; (morpheme-free) a clock alternative. The reviewer's N7 carried it to a
      ;; receipt through a POSITIONAL field read, `(.get cal 14)`, so no
      ;; accessor name is spelled anywhere in the route.
      ;;
      ;; A class gets the bare form EXACTLY WHEN it carries a clock member the
      ;; morpheme narrowing withholds a string form from -- which is the gap,
      ;; stated as a rule rather than as a list. `java.io.File`'s clock members
      ;; are all spelled `lastModified...`, so its string form already closes
      ;; every reflective route to it and a bare form would buy nothing while
      ;; matching every `^java.io.File` type hint in the tree (measured: 40
      ;; lines). `java.time.Instant` and `java.util.Calendar` carry `from` and
      ;; `getInstance`, and they are what this emits for.
      (for [^Class c clock-source-classes
            ;; The withheld case, exactly: a STATIC returning a time value is
            ;; derived with no morpheme required (that is what makes
            ;; `Calendar/getInstance` and `OffsetDateTime/now` visible), and a
            ;; morpheme-free name is the one that gets no string form. Every
            ;; other derived shape already requires a morpheme and so already
            ;; has one.
            :when (some (fn [^java.lang.reflect.Method m]
                          (let [nm (.getName m)]
                            (and (java.lang.reflect.Modifier/isStatic (.getModifiers m))
                                 (time-type? (.getReturnType m))
                                 (not (some #(str/includes? nm %) clock-name-fragments)))))
                        (.getMethods c))]
        (.getName c))
      (for [^Class c clock-source-classes
            :when (time-type? c)
            ^java.lang.reflect.Constructor k (.getConstructors c)
            :when (zero? (alength (.getParameterTypes k)))]
        (str (.getSimpleName c) ".")))))))

(defn- clock-expression-alternative
  "One derived spelling as a regex alternative.

  An INSTANCE spelling gets a trailing word boundary so `.lastModified` does
  not swallow `.lastModifiedTime` -- both are separate alternatives and the
  scan should say which one it found. A CONSTRUCTOR spelling gets a LEADING
  word boundary, so `Date.` does not match inside `LocalDate.`. A DOT SPECIAL
  FORM is matched with flexible whitespace, because `(.  System  nanoTime)` is
  the same read, and with an OPTIONAL FULLY-QUALIFIED PREFIX on the class:
  round-six review finding 3 planted `(. java.lang.System nanoTime)` and the
  scan did not see it, because the alternative anchored a whitespace class
  immediately
  before the SIMPLE name and a qualified class carries a `.` there instead. The
  slash form already had that tolerance -- its docstring says so -- and the dot
  form did not; the two now agree.

  The member position admits a PARENTHESISED member as well as a bare one,
  because Clojure's `.` special form has two legal member spellings --
  `(. obj member)` and `(. obj (method args*))` -- and round-eight review
  finding 1 planted `(. System (nanoTime))` at the receipt-building site with
  the gate at its exact baseline of 27 tests and 156 assertions. The second
  spelling is not exotic: it is the form on Clojure's own reference page for
  `.`, and it is what `..` and `memfn` -- macros in `clojure.core` whose entire
  job is to emit it -- expand to. `(.. recv (member))` and `(memfn member)`
  are two further faces per derived name, for the same reason: a derivation
  over names must also enumerate the GRAMMAR the names can appear in.

  A QUOTED STRING spelling is matched
  literally, quotes included: it is a method or a class named the way a
  reflective call names one, which is to say never as a source token. A STATIC
  spelling is quoted literally, and it deliberately matches a fully-qualified
  call too (`java.time.Instant/now` contains `Instant/now`)."
  [expression]
  (cond
    (str/starts-with? expression "\"")
    (java.util.regex.Pattern/quote expression)

    (str/starts-with? expression ".")
    (str "\\" expression "\\b")

    (str/starts-with? expression "(memfn ")
    (str "\\(memfn\\s+"
         (java.util.regex.Pattern/quote (str/trim (subs expression 7)))
         "\\b")

    (or (str/starts-with? expression "(. ")
        (str/starts-with? expression "(.. "))
    (let [dots (if (str/starts-with? expression "(.. ") "\\.\\." "\\.")
          [cls & members] (rest (str/split expression #"\s+"))]
      (str "\\(" dots "\\s+(?:[\\w.]*\\.)?"
           (java.util.regex.Pattern/quote cls)
           (apply str (map #(str "\\s+\\(?\\s*" (java.util.regex.Pattern/quote %)) members))
           "\\b"))

    (str/ends-with? expression ".")
    (str "\\b" (java.util.regex.Pattern/quote (subs expression 0 (dec (count expression)))) "\\.")

    ;; A BARE SOURCE SYMBOL naming a clock class -- round-eight review finding
    ;; 2. Matched where the class is used AS A VALUE, which is the reflective
    ;; route the finding names (`(.getMethod java.util.Calendar ...)`), and not
    ;; in the two positions where a fully-qualified class name is not a value:
    ;;
    ;;   - `Class/member` CALL POSITION -- `(java.nio.file.Files/exists p)` is
    ;;     an ordinary qualified call the slash spelling already classifies on
    ;;     its own method name. Without the `(?![/.])` lookahead this
    ;;     alternative matched every fully-qualified call in the tree: 55 new
    ;;     sites, measured, most of them `Files/`.
    ;;   - A TYPE HINT -- `^java.io.File` is a compiler instruction and reads no
    ;;     clock. Without `^` in the lookbehind that is 40 more lines, measured.
    ;;
    ;; This file's own argument is why the two exclusions are there rather than
    ;; the entries: an allow-list padded with entries that are not clock routes
    ;; is an allow-list nobody reads carefully.
    (and (str/includes? expression ".")
         (not (str/includes? expression "/")))
    (str "(?<![\\w.$^])" (java.util.regex.Pattern/quote expression) "\\b(?![/.$])")

    :else
    (java.util.regex.Pattern/quote expression)))

(def clock-spellings
  "The derivation's output, held so a witness can assert what it produced."
  (derived-clock-expressions))

(def clock-expressions-the-ratchet-must-carry
  "One representative spelling per JDK time shape, with the site that proved it
  matters.

  This is not the pattern — the pattern is derived. It is a fail-first witness
  ON the derivation: each of these is planted in a receipt-publishing function
  and the scan must name the form it sits in. A derivation that silently
  stopped producing one of these would otherwise look exactly like a clean
  tree."
  {"System/nanoTime" "the monotonic clock behind every duration"
   "System/currentTimeMillis" "the wall clock behind every epoch stamp"
   "Instant/now" "the java.time wall clock"
   "Instant/ofEpochMilli" "an epoch number turned back into a time value"
   "LocalDateTime/now" "the local-time wall clock"
   "Clock/systemUTC" "an injectable clock, still a clock"
   ".getTime" "java.util.Date's epoch accessor"
   ".lastModified" "the file mtime the round-four review found published at mcp_admit_tool.clj:737"
   ".lastModifiedTime" "the java.nio spelling of the same read"
   "Files/getLastModifiedTime" "the static java.nio spelling"
   ".toMillis" "a FileTime converted to an epoch number"
   ".toEpochMilli" "an Instant converted to an epoch number"
   "FileTime/fromMillis" "an epoch number turned back into a file time"
   ;; The round-five review's finding 3: four ordinary spellings outside the
   ;; 46 the hand-written CLASS list produced, planted together and all green.
   "OffsetDateTime/now" "a java.time wall clock whose class was in clock-return-types but in no source list"
   "Calendar/getInstance" "java.util.Calendar, which was in neither list"
   ".getTimeInMillis" "Calendar's epoch accessor"
   "(. System nanoTime" "the DOT SPECIAL FORM: the text is not `System/nanoTime` at all"
   ;; Round-six review finding 4: the babashka floor was short by exactly one
   ;; entry of the JVM-minus-babashka difference, and nothing checked it, so
   ;; the scanning gate's clock pattern was strictly weaker in the runtime it
   ;; actually runs in. `the-babashka-clock-floor-is-the-complete-jvm-difference`
   ;; below is the ratchet; this is the entry it was missing.
   "(. Calendar getInstance" "the dot special form of the java.util.Calendar factory — the third spelling babashka cannot derive, and the one the floor did not carry"
   "\"getTimeInMillis\"" "the FOURTH spelling babashka underives — Calendar's epoch accessor as a STRING, found by the manifest witness on its first run, one commit after the floor was corrected by hand"
   ;; Round-six review findings 2 and 3: neither the class nor the method is a
   ;; source token in `(.getMethod (Class/forName "java.lang.System") "nanoTime" ...)`,
   ;; and the dot special form's alternative was anchored so that a
   ;; fully-qualified class did not match it. Both published a raw sixteen-digit
   ;; nanoTime in a receipt field inside the hashed parity subject.
   "\"nanoTime\"" "the monotonic clock's method named as a STRING, which is what .getMethod takes"
   "\"currentTimeMillis\"" "the wall clock's method as a string"
   "\"java.lang.System\"" "the clock source class's FULLY-QUALIFIED NAME as a string: Class/forName cannot be reached without one"
   "(. java.lang.System nanoTime" "the dot special form with a fully-qualified class — the simple-name form matched and this one did not"
   ;; Round-eight review finding 1. `(. System (nanoTime))` is four tokens, it
   ;; is the spelling on Clojure's own reference page for the `.` special form,
   ;; it needs no reflection and no Reading, and it published a raw sixteen-digit
   ;; monotonic clock value into an undeclared receipt field inside the hashed
   ;; parity subject with the round-eight gate green at 27 tests and 156
   ;; assertions. `..` and `memfn` are the two `clojure.core` macros whose
   ;; entire job is to emit that spelling.
   "(.. System nanoTime" "the parenthesised-member grammar reached through `..`, whose expansion IS `(. System (nanoTime))`"
   "(memfn nanoTime" "`memfn`, which names the member as an ordinary source token and expands to the same form"
   ;; The three the floor witness named on its first run after the grammar
   ;; widened: java.util.Calendar is the class babashka's GraalVM image
   ;; underives (57 methods on the JVM, 6 here), so its `..` and `memfn` faces
   ;; are exactly the new JVM-minus-babashka difference. Found by the ratchet,
   ;; verbatim, not by hand.
   "(.. Calendar getInstance" "the `..` face of the java.util.Calendar factory — babashka underives it, so the floor is what carries it"
   "(memfn getInstance" "the `memfn` face of the same factory"
   "(memfn getTimeInMillis" "the `memfn` face of Calendar's epoch accessor"
   ;; Round-eight review finding 2: the class as a BARE SOURCE SYMBOL. Named
   ;; by the floor witness on the first run after the emission landed —
   ;; babashka's thin reflection cannot see `Calendar/getInstance`, so it
   ;; cannot see that Calendar is a class the morpheme narrowing withholds a
   ;; string form from, and so cannot derive the bare form either.
   "java.util.Calendar" "the clock source class as an ordinary source SYMBOL: `(.getMethod java.util.Calendar \"getInstance\" ...)` reaches the factory with neither the class nor the method a clock alternative"
   "Date." "a CONSTRUCTOR reads the clock, and a constructor is not a method, so .getMethods cannot see one"})

(def ^:private clock-pattern
  "Every way a JVM program reads a clock, DERIVED from the JDK rather than
   listed, unioned with the floor above.

   The union is a FLOOR, not a list. Babashka is a GraalVM native image and
   carries reflection metadata only for the classes it registered:
   `(.getMethods (Class/forName \"java.util.Calendar\"))` answers 57 methods on
   the JVM and 6 under babashka, so `Calendar/getInstance` and
   `.getTimeInMillis` are underivable there and the scan would otherwise be
   weaker in the runtime the scanning gate actually runs in. The witness below
   asserts the derivation produces every floor entry on a runtime whose
   reflection is complete, so the floor is derived evidence rather than a list
   of the spellings somebody thought of."
  (re-pattern
   (str/join "|" (map clock-expression-alternative
                      (sort (distinct (concat clock-spellings
                                              (keys clock-expressions-the-ratchet-must-carry))))))))

(def ^:private laundering-sentinel
  "A number no clock produces and no fixture carries, so finding it outside a
   `:measured` block is proof a public verb handed a reading's number back."
  987654.321)

(def ^:private laundering-tick-sentinel
  "The same idea for a START TICK, which holds a `long` rather than a double.

  A tick is the other opaque value in the namespace and `start-nanos` opens it,
  so a probe that only ever hands out `Reading`s cannot see that verb launder.
  It is integral because `start-nanos` coerces with `long`, and a double
  sentinel would come back truncated and unrecognisable."
  987654321)

(defn- sentinel-outside-a-measured-block?
  "Does `laundering-sentinel` appear in `x` anywhere a caller could read it as
   an ordinary number — that is, anywhere except under `measured-key`?

  Inside the block the number is the publication itself: `measured` and
  `attach` BUILD that block and must be allowed to."
  [x]
  (letfn [(walk [node]
            (cond
              (or (= laundering-sentinel node)
                  (= laundering-tick-sentinel node)) true
              (map? node) (boolean (some (fn [[k v]]
                                           (or (walk k)
                                               (and (not= k measured/measured-key)
                                                    (walk v))))
                                         node))
              (or (vector? node) (seq? node) (set? node))
              (boolean (some walk node))
              :else false))]
    (walk x)))

(defn- reading-arguments
  "The argument pool for the READING probe: the sentinel as a TAGGED reading,
   in the placements a caller has — bare, under a key, inside a vector — plus
   the key those maps use, so a two-argument reader can be handed both halves.

  Nothing in this pool carries the sentinel as a bare number, so the sentinel
  coming BACK outside a measured block means the var produced it."
  []
  [(measured/reading laundering-sentinel)
   {:probe (measured/reading laundering-sentinel)}
   [(measured/reading laundering-sentinel)]
   (measured/->Tick laundering-tick-sentinel)
   :probe])

(defn- block-arguments
  "The argument pool for the BLOCK probe: an already-published measured block,
   whose contents are bare numbers by construction, and its key."
  []
  [(measured/measured {:probe (measured/reading laundering-sentinel)})
   :probe])

(defn- argument-combinations
  [pool n]
  (reduce (fn [acc _] (for [args acc a pool] (conj args a)))
          [[]]
          (range n)))

(defn- probed-arities
  "Every fixed arity of `v` this probe can call. Variadic tails are dropped:
   the fixed prefix is probed by the arity that names it.

  A callable var with no `:arglists` — a protocol method under some runtimes, a
  keyword, a set — is probed at arity 1 and 2, the arities such a value has."
  [v]
  (let [declared (->> (:arglists (meta v))
                      (map #(count (take-while (complement #{'&}) %)))
                      distinct
                      sort
                      vec)]
    (if (seq declared) declared [1 2])))

(defn- yields-the-sentinel?
  "Does calling `v` over `pool` hand the sentinel back outside a measured
   block?"
  [v pool]
  (boolean
    (some (fn [n]
            (some (fn [args]
                    (try
                      (sentinel-outside-a-measured-block? (apply @v args))
                      (catch Throwable _ false)))
                  (argument-combinations pool n)))
          (probed-arities v))))

(def escape-hatch-spellings-the-ratchet-must-carry
  "Every spelling that reaches a reading's number, with the route it opens.

  This is the escape-hatch counterpart of
  `clock-expressions-the-ratchet-must-carry`, and it exists for the reason the
  round-five review gave (2026-09-04 §1): every alternative of the pattern was
  anchored on the literal text `measured/`, so the protocol's own method —
  which the JVM compiles to a munged Java interface method, `-launder` →
  `_launder` — was reachable as PLAIN INTEROP with no namespace token at all.
  The reviewer planted `(._launder (measured/elapsed-ms started))` at
  `src/clj_surgeon/mcp_hot_verify.clj` and published an undeclared
  clock-derived field inside the parity hash subject with nineteen tests and
  seventy-five assertions green.

  A hand-written alternation is a list of the spellings somebody thought of.
  The pattern below is DERIVED from the namespace's interns, the protocol's
  methods and the type's declared fields; this map is the fail-first witness ON
  that derivation, one entry per route."
  {"measured/raw-nanos" "the monotonic clock, untagged"
   "measured/raw-ms" "the wall clock, untagged"
   "measured/value" "THE one laundering verb"
   "measured/-launder" "the protocol method `value` is built on"
   "measured/unwrap-readings" "the private verb that strips tags at any depth (round-four §1)"
   "measured/start-nanos" "the private verb that opens a start tick"
   "._launder" "the protocol method as MUNGED Java interop — the round-five review's finding 1"
   ".-launder" "the same method spelled as an unmunged field access"
   ;; Round-six review findings 1a/1b/1c: a call that names its target as a
   ;; STRING, and the DOT SPECIAL FORM. Every alternative above is a source
   ;; TOKEN, and `(.getMethod (class r) "_launder" ...)`,
   ;; `(Reflector/invokeInstanceMethod r "_launder" ...)` and `(. r _launder)`
   ;; spell no such token — all three published a clock-derived number in an
   ;; undeclared receipt field inside the hashed parity subject with the
   ;; round-six gate green at 24 tests and 126 assertions.
   "\"_launder\"" "the munged protocol method named as a STRING, which is the only way .getMethod and Reflector/invokeInstanceMethod can name it"
   "\"-launder\"" "the unmunged protocol method as a string, the name Reflector takes for a Clojure-compiled interface method"
   "\"launderable\"" "the opaque type's field named as a string, which is what .getDeclaredField takes"
   "(. _launder" "the DOT SPECIAL FORM of the munged interop route: `(. r _launder)` is `(._launder r)` with two characters moved"
   "(. -launder" "the dot special form of the unmunged spelling"
   "(. launderable" "the dot special form of the field access"
   ;; Round-eight review finding 1: the dot special form has a SECOND member
   ;; spelling, `(. obj (member args*))`, and `..` and `memfn` are the
   ;; `clojure.core` macros that emit it. `(. rr (_launder))` and
   ;; `((memfn _launder) rr)` each published a reading's number in an
   ;; undeclared receipt field inside the hashed parity subject with the
   ;; round-eight gate green at 27 tests and 156 assertions.
   "(.. _launder" "the PARENTHESISED member spelling of the munged interop route, which `..` emits"
   "(.. -launder" "the same through `..` on the unmunged spelling"
   "(.. launderable" "the same through `..` on the field access"
   "(memfn _launder" "`memfn` names the member as an ordinary source token and expands to the parenthesised member form"
   "(memfn -launder" "`memfn` on the unmunged spelling"
   "(memfn launderable" "`memfn` on the field name"
   ".-launderable" "the deftype's private field, which babashka's interop does not enforce"
   ".launderable" "the same field spelled as a method call"
   "launderable" "the same field named bare, as a reflective lookup spells it"})

(defn- untagged-clock-intern?
  "Does calling `v` with NO arguments hand back a bare number?

  That is what `raw-nanos` and `raw-ms` are: a clock read with no tag on it.
  `start` returns a tick, `wall-clock-ms` returns a reading, and every other
  intern throws — so this predicate names the untagged clock verbs without
  knowing any of their names."
  [v]
  (try (number? (@v)) (catch Throwable _ false)))

(defn- laundering-interns
  "Every intern of `clj-surgeon.measured` — PUBLIC OR PRIVATE — that either
  reads a clock untagged or hands a tagged value's number back.

  `ns-interns`, not `ns-publics`, and that is the point: `unwrap-readings` and
  `start-nanos` are private, privacy in Clojure is a resolution convention
  rather than a boundary, and a pattern that only knows the public names cannot
  cost a site that reached past them."
  []
  (->> (ns-interns 'clj-surgeon.measured)
       (filter (fn [[_ v]] (and (ifn? @v)
                                (or (untagged-clock-intern? v)
                                    (yields-the-sentinel? v (reading-arguments))))))
       (map (comp name first))
       sort
       vec))

(defn- protocol-method-names
  "Every method name of every protocol defined in `clj-surgeon.measured`, from
  the protocol map's own `:sigs`.

  `:sigs` rather than reflection on the compiled interface, because sci does
  not compile a protocol to a Java interface at all and the scan has to be the
  same in both runtimes. The JVM's compiled interface is checked against this
  list by the witness below."
  []
  (->> (ns-interns 'clj-surgeon.measured)
       vals
       (mapcat (fn [v]
                 (let [x (try @v (catch Throwable _ nil))]
                   (when (and (map? x) (map? (:sigs x)))
                     (map name (keys (:sigs x)))))))
       distinct
       sort
       vec))

(defn- opaque-type-field-names
  "Every declared field of the opaque types, by reflection on the JVM.

  EMPTY UNDER BABASHKA — sci's `deftype` is a `SciType` with no declared
  fields — which is exactly why `escape-hatch-spellings-the-ratchet-must-carry`
  carries the field spelling as a floor as well: `(.-launderable r)` is a live
  route under babashka and an unreflectable one. The witness below proves the
  JVM derivation produces it, so the floor entry is derived evidence rather
  than a name somebody thought of."
  []
  (->> [(measured/reading 1.0) (measured/start)]
       (mapcat (fn [x] (try (->> (.getDeclaredFields (class x))
                                 (remove #(java.lang.reflect.Modifier/isStatic
                                           (.getModifiers ^java.lang.reflect.Field %)))
                                 (remove #(.isSynthetic ^java.lang.reflect.Field %))
                                 (map #(.getName ^java.lang.reflect.Field %)))
                            (catch Throwable _ nil))))
       ;; The compiler's own fields — `__cached_class__0`, `const__3` — are
       ;; static and carry no reading; a scan alternative built from one would
       ;; be noise with a word boundary on it.
       (remove #(str/starts-with? % "__"))
       distinct
       sort
       vec))

(defn- reflective-member-names
  "Every name of the measured namespace's own JAVA surface that a STRING can
  name: each protocol method as written AND as the JVM munges it, and each
  declared field of the opaque types.

  This is the exact set `.getMethod`, `.getDeclaredField` and
  `clojure.lang.Reflector/invokeInstanceMethod` accept as a string argument, and
  it is derived from the same two sources the token spellings are derived from,
  so a method or field added later grows all three forms together."
  []
  (vec
   (sort
    (distinct
     (concat (mapcat (fn [m] [m (munge m)]) (protocol-method-names))
             (opaque-type-field-names))))))

(defn- escape-hatch-spellings
  "Every SPELLING that reaches an untagged number out of the measured
  namespace, derived from three sources and never typed out:

  - each laundering or untagged-clock intern, as `measured/<name>`;
  - each protocol method, as BOTH `.<name>` and `.<munged-name>` — the JVM
    munges `-launder` to `_launder` and compiles it to an ordinary Java
    interface method, so `(._launder r)` is the sanctioned door with no
    namespace token in it at all (round-five review finding 1);
  - each declared field of the opaque types, bare and as `.-<name>`, because
    babashka's interop does not enforce a deftype field's privacy;
  - each of those JAVA MEMBER names ALSO as a QUOTED STRING and ALSO as the DOT
    SPECIAL FORM, which is round-six review findings 1a/1b/1c: every
    alternative above is a source TOKEN, and `(.getMethod (class r)
    \"_launder\" ...)`, `(clojure.lang.Reflector/invokeInstanceMethod r
    \"_launder\" ...)` and `(. r _launder)` each reached the protocol method
    while spelling no token at all, publishing a clock-derived number in an
    undeclared receipt field inside the hashed parity subject with the
    round-six gate green.

  The `measured/<var>` spellings deliberately get NO string form, and that
  exclusion is EVIDENCED rather than assumed. A string cannot name a Clojure
  var by itself; it needs `resolve`, `ns-resolve`, `find-var`,
  `requiring-resolve` or `intern`, and every one of those is already an offence
  under the naming rule's `:reflective` clause -- which
  `the-require-witness-catches-a-planted-reflective-resolution` proves by
  planting one. Giving `\"value\"` an alternative would instead cost the two JSON
  schema keys literally named `value` in `mcp_schema.clj` and `mcp_contract.clj`
  an allow-list entry apiece, and an allow-list padded with entries that are not
  clock routes is an allow-list nobody reads carefully. A JAVA member name is a
  different case and gets the string form: `.getMethod`, `.getDeclaredField` and
  `Reflector/invokeInstanceMethod` take one directly, with no measured token
  anywhere in the call."
  []
  (let [members (reflective-member-names)
        ;; The dot form's member position also accepts the FIELD-ACCESS
        ;; spelling, `(. r -launderable)`, which is not a reflective name.
        dot-members (distinct
                      (concat members
                              (map #(str "-" %) (opaque-type-field-names))))]
    (vec
     (sort
      (distinct
       (concat (map #(str "measured/" %) (laundering-interns))
               (mapcat (fn [m] [(str "." m) (str "." (munge m))])
                       (protocol-method-names))
               (mapcat (fn [f] [f (str ".-" f) (str "." f)])
                       (opaque-type-field-names))
               (map #(str "\"" % "\"") members)
               (map #(str "(. " %) dot-members)
               ;; The dot special form's PARENTHESISED member spelling, and the
               ;; two `clojure.core` macros whose expansion IS that spelling.
               ;; Round-eight review finding 1: `(. rr (_launder))` and
               ;; `((memfn _launder) rr)` name the member as an ordinary source
               ;; token and matched no alternative.
               (map #(str "(.. " %) dot-members)
               (map #(str "(memfn " %) members)))))))

(def derived-escape-hatch-spellings
  "The derivation's output, held so a witness can assert what it produced."
  (escape-hatch-spellings))

(defn- escape-hatch-alternative
  "One spelling as a regex alternative.

  A TOKEN spelling is quoted literally, with a lookahead that refuses a longer
  identifier, so `measured/value` does not swallow a hypothetical
  `measured/value-of` and each alternative names exactly the door it found.

  A QUOTED STRING spelling is matched literally WITH its quotes -- the whole
  point is that the text is `\"_launder\"` and not `._launder`, so a lookahead
  for a longer identifier would be meaningless.

  A DOT SPECIAL FORM spelling carries the member only, because the receiver is
  whatever the caller had in hand: the alternative admits a bare receiver
  (`(. r _launder)`) or a single parenthesised one (`(. (class r) _launder)`).
  A receiver with NESTED parentheses is a declared residual, and a narrow one:
  such a receiver has to produce a reading, which under these roots means
  naming `measured/...`, and that names a token this pattern already matches.

  The MEMBER position admits a parenthesis too. Clojure's `.` special form has
  two legal member spellings and round-eight review finding 1 published a
  reading's number through the second, `(. rr (_launder))`, with the gate at
  its exact baseline. `(.. r (_launder))` and `((memfn _launder) r)` are the
  same grammar reached through the two `clojure.core` macros that emit it, and
  each is its own face here: the receiver-shaped `..` alternative and a
  receiver-free `memfn` one, since `memfn` takes the member alone."
  [spelling]
  (cond
    (str/starts-with? spelling "\"")
    (java.util.regex.Pattern/quote spelling)

    (str/starts-with? spelling "(memfn ")
    (str "\\(memfn\\s+"
         (java.util.regex.Pattern/quote (str/trim (subs spelling 7)))
         "\\b")

    (or (str/starts-with? spelling "(. ")
        (str/starts-with? spelling "(.. "))
    (let [dots (if (str/starts-with? spelling "(.. ") "\\.\\." "\\.")
          member (str/trim (subs spelling (if (= dots "\\.\\.") 4 3)))]
      (str "\\(" dots "\\s+(?:\\([^()]*\\)|\\S+)\\s+\\(?\\s*"
           (java.util.regex.Pattern/quote member)
           "\\b"))

    :else
    (str (java.util.regex.Pattern/quote spelling) "(?![-\\w])")))

(def ^:private escape-hatch-pattern
  "Every expression that hands back an UNTAGGED number from the measured
   namespace, DERIVED from the namespace rather than listed.

   The round-five review's blocking finding 1 (2026-09-04 §1) killed the list:
   every alternative of it was anchored on the literal text `measured/`, and a
   protocol method is a munged Java interface method, so `(._launder r)` walked
   through the pattern, through the require rule, and through the public-var
   probe — all nineteen tests green with a clock number in the parity hash.

   The union with `escape-hatch-spellings-the-ratchet-must-carry` is a FLOOR,
   not a list: every entry of it is asserted below to be produced by the
   derivation in the runtime that can see it, and the floor exists only so the
   scan is byte-identical under babashka, where a deftype's declared fields are
   not reflectable at all."
  (re-pattern
   (str/join "|" (map escape-hatch-alternative
                      (sort (distinct (concat derived-escape-hatch-spellings
                                              (keys escape-hatch-spellings-the-ratchet-must-carry))))))))

;; ============================================================
;; 1. No raw clock read outside `clj-surgeon.measured`
;; ============================================================

(def clock-allow-list
  "Every form in `src/` outside `clj-surgeon.measured` that may read a clock
  RAW, with the count of reads in that form and why it is allowed.

  The first repair of this invariant inventoried clock reads and classified
  them `:receipt` or `:control`, comparing only READ COUNTS per form. Sol's
  round-two review walked straight through it (2026-09-04 §1): bind one
  existing read to a local, publish it under the declared name AND an
  undeclared one, and the count is unchanged, the new name is in nobody's
  vocabulary, and the undeclared field sails into the parity hash with every
  witness green.

  So the rule is no longer 'a clock read is classified'. It is: **a clock read
  whose value can be PUBLISHED does not happen outside `clj-surgeon.measured`.**
  Receipt code calls `measured/start` and `measured/elapsed-ms`, which return a
  TAGGED reading, and the publication boundary relocates every reading it
  finds under any key at all. This list is therefore `:control` ONLY — a lease
  deadline, an expiry sweep, a retention cutoff, a transaction id, a poll loop,
  a file timestamp, the battery harness's own row. A `:receipt` entry here
  would be a contradiction and the test below refuses one.

  A THIRD kind arrived with round-eight review finding 2, and it is named here
  so a reader is not left to infer it: a form the scan matches WITHOUT A READ
  HAPPENING AT ALL — a clock class named inside a string literal or a
  docstring. A source-TEXT scan cannot tell a string literal from source, and
  the honest form of that limit is an entry a reader can see rather than a
  special case in the pattern nobody would find. Such an entry says
  `NOT A READ` in its `:why`, and its count still ratchets.

  Adding a control clock read means adding a line here and saying why the value
  is never published. That is the whole cost, and it is the cost on purpose."
  {["src/clj_surgeon/ls_tree_snapshot.clj" "prune!"]
   {:reads 2 :channel :control :why "snapshot expiry sweep: the cutoff and each candidate file's mtime"}
   ["src/clj_surgeon/ls_tree_snapshot.clj" "touch!"]
   {:reads 1 :channel :control :why "file mtime, not a receipt field"}
   ["src/clj_surgeon/ls_tree_snapshot.clj" "write-snapshot!"]
   {:reads 1 :channel :control :why "snapshot creation stamp on disk"}
   ["src/clj_surgeon/mcp_change_buffer.clj" "now-ms"]
   {:reads 1 :channel :control :why "buffer lease clock"}
   ["src/clj_surgeon/mcp_cold_verify.clj" "now-ms"]
   {:reads 1 :channel :control :why "job store lease clock"}
   ["src/clj_surgeon/mcp_combinable_transaction.clj" "new-registry"]
   {:reads 1 :channel :control :why "registry lease clock seam"}
   ["src/clj_surgeon/mcp_prepared_confirmation.clj" "new-registry"]
   {:reads 1 :channel :control :why "registry lease clock seam"}
   ["src/clj_surgeon/mcp_process.clj" "call-with-analyzer-contract-mission"]
   {:reads 1 :channel :control :why "mission lease expiry"}
   ["src/clj_surgeon/mcp_process.clj" "claim-analyzer-mission-launch!"]
   {:reads 1 :channel :control :why "mission lease claim"}
   ["src/clj_surgeon/mcp_process.clj" "record-analyzer-mission-exit!"]
   {:reads 1 :channel :control :why "mission lease exit stamp"}
   ["src/clj_surgeon/mcp_telemetry.clj" "emit!"]
   {:reads 1 :channel :control :why "telemetry row timestamp, never a public result"}
   ["src/clj_surgeon/mcp_alias_migration.clj" "prune-details!"]
   {:reads 1 :channel :control :why "prune ordering: newest detail files kept, by file mtime"}
   ["src/clj_surgeon/mcp_telemetry.clj" "prune!"]
   {:reads 2 :channel :control :why "telemetry retention cutoff and each candidate file's mtime"}
   ["src/clj_surgeon/mcp_tool.clj" "refusal-fact-line"]
   {:reads 2 :channel :control :why "the ONE print deadline for a refusal's fact line: a wall instant taken once to set the budget and once to spend it, compared against itself and never rendered into a fact or any receipt field"}
   ["src/clj_surgeon/memory_battery_runner.clj" "measure-once"]
   {:reads 2 :channel :control :why "the battery harness's own wall row"}
   ["src/clj_surgeon/memory_battery_runner.clj" "run-battery"]
   {:reads 2 :channel :control :why "battery run start/finish stamps"}
   ["src/clj_surgeon/memory_battery_runner.clj" "write-receipt!"]
   {:reads 1 :channel :control :why "receipt filename stamp"}
   ["src/clj_surgeon/txn_journal.clj" "begin!"]
   {:reads 1 :channel :control :why "transaction started-at stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "evidence-stat"]
   {:reads 3 :channel :control :why "the mtime/ctime pair that dates a tombstone's evidence on disk, plus the java.nio ATTRIBUTE NAME `\"lastModifiedTime\"` the pair is read by — three spellings on two lines. Was declared 2 because the scan counted matching LINES (round-six review §6) and because the string spelling did not exist until this round; the value is a lock-age basis and reaches no receipt"}
   ["src/clj_surgeon/txn_journal.clj" "finish!"]
   {:reads 1 :channel :control :why "transaction finished-at stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "legacy-lock-dead?"]
   {:reads 1 :channel :control :why "lock liveness cutoff"}
   ["src/clj_surgeon/txn_journal.clj" "lock-age-basis-ms"]
   {:reads 2 :channel :control :why "the newest of a lock file's mtime and ctime, the liveness basis"}
   ["src/clj_surgeon/txn_journal.clj" "lock-broken-line"]
   {:reads 1 :channel :control :why "broken-lock journal line stamp"}
   ["src/clj_surgeon/txn_journal.clj" "mark-break-linked!"]
   {:reads 1 :channel :control :why "break-link stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "new-txid"]
   {:reads 1 :channel :control :why "transaction id"}
   ["src/clj_surgeon/txn_journal.clj" "path-stat"]
   {:reads 1 :channel :control :why "the on-disk stat identity of a path; :mtime-ns is an identity field, never a receipt"}
   ["src/clj_surgeon/txn_journal.clj" "process-start-ticks"]
   {:reads 1 :channel :control :why "a process's start instant, half of the holder identity that makes a lease checkable"}
   ["src/clj_surgeon/txn_journal.clj" "prune-broken-locks!"]
   {:reads 1 :channel :control :why "broken-lock retention cutoff"}
   ["src/clj_surgeon/txn_journal.clj" "recover!"]
   {:reads 2 :channel :control :why "recovery stamps on disk"}
   ["src/clj_surgeon/txn_journal.clj" "release-receipt!"]
   {:reads 1 :channel :control :why "lease release stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "retained-transactions"]
   {:reads 1 :channel :control :why "retention cutoff"}
   ["src/clj_surgeon/txn_journal.clj" "stamp-broken-at!"]
   {:reads 2 :channel :control :why "the broken-at stamp on disk, read once and rendered once"}
   ["src/clj_surgeon/txn_journal.clj" "stamp-tombstone!"]
   {:reads 1 :channel :control :why "tombstone stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "touch-tombstone!"]
   {:reads 1 :channel :control :why "writes that stamp onto the tombstone file; the value is the caller's, not a new read"}
   ["src/clj_surgeon/txn_journal.clj" "write-lease!"]
   {:reads 1 :channel :control :why "lease acquired-at stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "write-lock!"]
   {:reads 1 :channel :control :why "lock stamp on disk"}
   ["src/clj_surgeon/workspace_onboarding.clj" "await-cclsp-workspace!"]
   {:reads 2 :channel :control :why "readiness poll deadline"}
   ["src/clj_surgeon/worktree_lifecycle.clj" "instant-string?"]
   {:reads 1 :channel :control :why "parses a CALLER's timestamp string to decide whether it is one; the predicate returns a boolean and the parsed value is discarded"}
   ["src/clj_surgeon/worktree_lifecycle.clj" "valid-future-expiry?"]
   {:reads 2 :channel :control :why "compares two CALLER-supplied timestamp strings; returns a boolean, publishes neither. TWO `Instant/parse` reads on ONE line — the reviewer's live proof that the count was a count of matching lines (round-six review §6), re-blessed here at the number the calls actually come to"}
   ["src/clj_surgeon/worktree_lifecycle_io.clj" "capture-inventory"]
   {:reads 1 :channel :control :why "inventory captured-at stamp"}
   ["src/clj_surgeon/worktree_lifecycle_io.clj" "issue-current?"]
   {:reads 2 :channel :control :why "issue freshness cutoff: `Instant/now` against a CALLER-supplied expiry string it parses; the predicate returns a boolean"}
   ;; Round-eight review finding 2 widened the clock vocabulary to a clock
   ;; class named as a BARE SOURCE SYMBOL. These two are the WHOLE cost of that
   ;; on this tree, and neither is a read: both are the literal TEXT
   ;; `"(:import java.time.Instant)"` inside a replay fixture's edit payload.
   ;; They are named rather than excluded because a text scan cannot tell a
   ;; string literal from source, and the honest form of that limit is an entry
   ;; a reader can see, not a special case in the pattern nobody would find.
   ;; The count is still a ratchet: adding a real read to either form moves it.
   ["dev/experiments/namespace_tolerance_replay.clj" "falsifier-report"]
   {:reads 1 :channel :control :why "NOT A READ: the fixture's replacement text is the string \"(:import java.time.Instant)\", which names a clock class and reads nothing"}
   ["dev/experiments/namespace_tolerance_replay_test.clj" "law-b-requires-direct-uncontested-namespace-clause-children"]
   {:reads 1 :channel :control :why "NOT A READ: the same fixture string in the test that drives it"}})

(def escape-hatch-allow-list
  "Every form in `src/` that calls a verb handing back an UNTAGGED number.

  `measured/value` strips a reading's tag; `measured/raw-nanos` and
  `measured/raw-ms` read the clock without one. Laundering is legitimate — a
  sum, a comparison, a telemetry row, a clock seam a caller may inject — and it
  must be a deliberate, greppable act rather than a side effect, so every site
  is named here with the reason it needs a bare number."
  {["src/clj_surgeon/mcp_operation.clj" "invoke!"]
   {:calls 1 :why "the injectable request-clock seam: callers pass a plain-long clock"}
   ["src/clj_surgeon/mcp_operation.clj" "finalize-result"]
   {:calls 1 :why "the boundary validates finiteness before it attaches the reading"}
   ["src/clj_surgeon/mcp_change_buffer.clj" "capture-verification-baseline!"]
   {:calls 1 :why "sums the per-check clocks into one derived reading"}
   ["src/clj_surgeon/mcp_change_buffer.clj" "run-verification!"]
   {:calls 2 :why "sums hot and per-check clocks into one derived reading"}
   ["src/clj_surgeon/mcp_tool.clj" "exact-terminal-response"]
   {:calls 2 :why "compares the verification clock against zero before publication"}
   ["src/clj_surgeon/mcp_tool.clj" "record-result!"]
   {:calls 1 :why "telemetry row, never a public result"}
   ["src/clj_surgeon/mcp_tool.clj" "execute-request-in-context!"]
   {:calls 1 :why "telemetry row, never a public result"}
   ["src/clj_surgeon/mcp_inspect_tool.clj" "execute-inspect-in-context!"]
   {:calls 1 :why "telemetry row, never a public result"}
   ["src/clj_surgeon/parse_admission.clj" "refusal"]
   {:calls 1 :why "the scan meter accumulates bare nanos across many files"}
   ["src/clj_surgeon/txn_journal.clj" "commit!"]
   {:calls 1 :why "keeps the widest commit window across the published paths"}
   ["dev/experiments/formatter_process_canary.clj" "run-canary!"]
   {:calls 1 :why "a canary report line, printed to stdout, never an MCP result"}})

(defn- scan-one-planted-line
  "Scan a one-line-bodied function planted under a scratch root."
  [pattern body]
  (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                           (str "measured-one-line-" (System/nanoTime))))
        victim (io/file root "clj_surgeon" "planted_line.clj")]
    (.mkdirs (.getParentFile victim))
    (spit victim
          (str "(ns clj-surgeon.planted-line)\n\n"
               "(defn publish-a-receipt\n"
               "  [subject]\n"
               "  " body ")\n"))
    (try (scan pattern root)
         (finally (.delete victim)
                  (.delete (.getParentFile victim))
                  (.delete (io/file root))))))

;; @spec MCP-OP-TIME-005
;; @spec MCP-OP-TIME-006
(deftest an-allow-list-count-counts-CALLS-and-not-matching-lines
  (testing "round-six review §6: a matching line absorbs unlimited extra calls"
    ;; `sites` folded over lines and conjed ONE hit per line on which the
    ;; pattern matched. So a declared count was a count of matching LINES, and
    ;; a line that already matched absorbed an unlimited number of further
    ;; clock reads or laundering calls without moving it. The reviewer found it
    ;; live in the tree rather than in theory: `worktree_lifecycle/
    ;; valid-future-expiry?` is one line carrying TWO `Instant/parse` reads and
    ;; was declared `:reads 1` with `(= declared scanned)` green.
    ;;
    ;; The scan is the ONLY defence for every producer under `src/` -- the
    ;; behavioural witnesses drive `finalize-result` with a `(constantly ...)`
    ;; execute, so they see the publication boundary and nothing upstream of
    ;; it. A counting rule with a per-line ceiling of one is a hole in the only
    ;; line of defence the producers have.
    (let [site {["src/clj_surgeon/planted_line.clj" "publish-a-receipt"] 2}]
      (is (= site (scan-one-planted-line
                    clock-pattern
                    "{:a (System/nanoTime) :b (System/currentTimeMillis)}"))
          "two clock reads on ONE line counted as fewer than two")
      (is (= site (scan-one-planted-line
                    clock-pattern
                    "{:a (Instant/parse subject) :b (Instant/parse subject)}"))
          "the reviewer's own shape -- two reads of the SAME spelling on one line")
      (is (= site (scan-one-planted-line
                    escape-hatch-pattern
                    "{:a (measured/value subject) :b (measured/value subject)}"))
          "two laundering calls on ONE line counted as fewer than two"))))

;; @spec MCP-OP-TIME-005
(deftest no-raw-clock-read-lives-outside-the-measured-namespace
  (testing "a published clock reading cannot be CONSTRUCTED outside the partition"
    (let [scanned (scan clock-pattern)
          declared (into {} (map (fn [[k v]] [k (:reads v)])) clock-allow-list)]
      (is (= (set (keys declared)) (set (keys scanned)))
          (str "raw clock reads with no allow-list entry: "
               (pr-str (sort (remove (set (keys declared)) (keys scanned))))
               " ; allow-listed sites that no longer exist: "
               (pr-str (sort (remove (set (keys scanned)) (keys declared))))))
      (is (= declared scanned)
          "a form's raw clock-read count changed; re-read it and re-justify")
      (is (= [] (sort (keep (fn [[site {:keys [channel]}]]
                              (when (not= :control channel) site))
                            clock-allow-list)))
          "a RECEIPT clock read may not be raw: it must return a tagged reading"))))

;; @spec MCP-OP-TIME-005
(deftest every-untagged-clock-verb-call-site-is-named
  (testing "laundering a reading back to a bare number is deliberate"
    (let [scanned (scan escape-hatch-pattern)
          declared (into {} (map (fn [[k v]] [k (:calls v)])) escape-hatch-allow-list)]
      (is (= (set (keys declared)) (set (keys scanned)))
          (str "untagged-clock verbs with no allow-list entry: "
               (pr-str (sort (remove (set (keys declared)) (keys scanned))))
               " ; allow-listed sites that no longer exist: "
               (pr-str (sort (remove (set (keys scanned)) (keys declared))))))
      (is (= declared scanned)
          "a form's untagged-clock call count changed; re-read it and re-justify"))))

;; @spec MCP-OP-TIME-006
(defn- escape-route-call
  "A spelling written as the SOURCE that actually reaches the number.

  A plant is evidence only if it is the shape production code would take, so a
  STRING spelling is planted inside the reflective call that gives a string its
  power — `.getMethod` plus `.invoke` — rather than as a bare literal in call
  position, and a DOT SPECIAL FORM spelling is reassembled with a receiver.
  (`witness through the production path`: a fix witnessed only through a tamper
  seam is a fix witnessed nowhere.)"
  [spelling]
  (cond
    ;; `memfn` takes the member alone and is applied to the receiver, which is
    ;; the shape the reviewer's N4 published through.
    (str/starts-with? spelling "(memfn ")
    (str "((memfn " (subs spelling 7) ") subject)")

    ;; `..` and the PARENTHESISED member: reassembled with a receiver and with
    ;; the member in the parens the macro emits, so the plant is the source
    ;; production code would actually take rather than the encoding.
    (str/starts-with? spelling "(.. ")
    (str "(.. subject (" (subs spelling 4) "))")

    (str/starts-with? spelling "(. ")
    (str "(. subject " (subs spelling 3) ")")

    (str/starts-with? spelling "\"")
    (str "(.invoke (.getMethod (class subject) " spelling
         " (into-array Class [])) subject (into-array Object []))")

    :else
    (str "(" spelling " subject)")))

(deftest the-escape-hatch-pattern-carries-every-route-to-a-readings-number
  (testing "the pattern is derived from the namespace, not typed out"
    ;; Checked in the CALL SHAPE, not on the raw spelling. A token spelling is
    ;; its own source text, but `(. _launder` is an ENCODING of the dot special
    ;; form whose receiver belongs to the caller -- matching the encoding
    ;; against the pattern would be asserting the alternative against itself.
    (let [missing (vec (sort (remove #(re-find escape-hatch-pattern
                                               (escape-route-call %))
                                     (keys escape-hatch-spellings-the-ratchet-must-carry))))]
      (is (= [] missing)
          (str "spellings that reach a reading's number and no alternative of "
               "escape-hatch-pattern matches, so their call sites cost nothing: "
               (pr-str (mapv (juxt identity escape-hatch-spellings-the-ratchet-must-carry)
                             missing)))))))

;; @spec MCP-OP-TIME-006
(deftest the-escape-hatch-pattern-is-derived-from-the-namespace-not-from-a-list
  (testing "the floor is evidence the derivation produced, not a list of names"
    (let [derived (set derived-escape-hatch-spellings)
          fields (opaque-type-field-names)
          field-spellings (set (mapcat (fn [f] [f (str ".-" f) (str "." f)]) fields))
          ;; Under babashka a deftype has no declared fields to reflect on, so
          ;; the field spellings are a floor there and derived evidence here.
          expected (cond->> (keys escape-hatch-spellings-the-ratchet-must-carry)
                     (empty? fields) (remove #(str/includes? % "launderable")))
          missing (vec (sort (remove derived expected)))]
      (is (= [] missing)
          (str "the derivation stopped producing spellings the ratchet carries, "
               "so the pattern is running on its floor alone: " (pr-str missing)))
      (is (contains? derived "._launder")
          (str "the MUNGED protocol-method spelling is not derived, which is "
               "round-five finding 1 exactly: " (pr-str derived)))
      (is (seq (protocol-method-names))
          "no protocol method was derived, so the interop route is unwatched")
      (is (= [] (vec (sort (remove derived field-spellings))))
          (str "a field spelling was derived in one form and not another: "
               (pr-str (sort field-spellings))))
      ;; Every method of every clj-surgeon interface the opaque type actually
      ;; implements must be in the derived set. On the JVM that is the compiled
      ;; `Launderable`; under sci there is no such interface and this is
      ;; vacuous, which is why `:sigs` is the derivation's source of truth.
      (let [iface-methods (->> [(measured/reading 1.0) (measured/start)]
                               (mapcat (fn [x] (try (seq (.getInterfaces (class x)))
                                                    (catch Throwable _ nil))))
                               (filter #(str/starts-with? (.getName ^Class %) "clj_surgeon"))
                               (mapcat (fn [^Class c] (map #(.getName ^java.lang.reflect.Method %)
                                                           (.getMethods c))))
                               distinct sort vec)]
        (is (= [] (vec (remove #(contains? derived (str "." %)) iface-methods)))
            (str "the opaque type implements a clj-surgeon interface method the "
                 "derivation does not spell: " (pr-str iface-methods)))))))

;; @spec MCP-OP-TIME-006
(def ^:private round-six-review-plants
  "The five call sites the round-six review published a clock number through,
  verbatim, with the gate green.

  Each is a line of ordinary Clojure planted at
  `src/clj_surgeon/mcp_hot_verify.clj:114` — the receipt-building site the two
  round-five plants used — and each put a clock-derived number in an UNDECLARED
  field inside the HASHED PARITY SUBJECT while the scanning gate reported 24
  tests and 126 assertions, 0 failures. The class they share is one sentence:
  every alternative of both derived patterns was a source TOKEN, and each of
  these names its target as a STRING, or by the DOT SPECIAL FORM, or with a
  fully-qualified prefix the dot-form alternative was anchored against.

  Held as data so the witness below cannot drift from the review, and so a
  future reviewer can add a plant without writing a test."
  [{:label "A — .getMethod names the method as a STRING"
    :pattern :escape
    :source "(.invoke (.getMethod (class rr) \"_launder\" (into-array Class [])) rr (into-array Object []))"}
   {:label "B — clojure.lang.Reflector, what the compiler itself emits for an un-hinted interop call"
    :pattern :escape
    :source "(clojure.lang.Reflector/invokeInstanceMethod rr \"_launder\" (into-array Object []))"}
   {:label "H — the DOT SPECIAL FORM, round-five finding 1 with two characters moved"
    :pattern :escape
    :source "(. rr _launder)"}
   {:label "D — Class/forName plus reflection: neither the class nor the method is a token"
    :pattern :clock
    :source "(.invoke (.getMethod (Class/forName \"java.lang.System\") \"nanoTime\" (into-array Class [])) nil (into-array Object []))"}
   {:label "K — the dot special form with a FULLY-QUALIFIED class"
    :pattern :clock
    :source "(. java.lang.System nanoTime)"}
   ;; D2 and K2 are not the reviewer's. They exist because sabotaging the fix
   ;; on a `git archive` copy showed that D and K alone do NOT witness it: both
   ;; name `java.lang.System`, which the FLOOR carries by name, so removing the
   ;; optional fully-qualified prefix from the dot-form alternative left all
   ;; twenty-five tests green, and removing the string forms from the clock
   ;; DERIVATION cost only the derivation witness. A rule that never fires
   ;; produces no error. These two name classes the floor does not, so the only
   ;; thing that can catch them is the derivation and the prefix tolerance —
   ;; which is the property the fix actually claims.
   {:label "D2 — Class/forName on a class the FLOOR does not name, so only the DERIVED class string sees it"
    :pattern :clock
    :source "(.invoke (.getMethod (Class/forName \"java.util.Calendar\") \"getInstance\" (into-array Class [])) nil (into-array Object []))"}
   {:label "K2 — the fully-qualified dot form on a class the floor does not name, so only the PREFIX TOLERANCE sees it"
    :pattern :clock
    :source "(. java.time.Instant now)"}])

;; @spec MCP-OP-TIME-005
;; @spec MCP-OP-TIME-006
(def ^:private round-eight-review-plants
  "The round-eight review's plants, verbatim, planted at the same
  `src/clj_surgeon/mcp_hot_verify.clj:114` receipt-building site and each green
  at the gate's baseline of 27 tests and 156 assertions, 0 failures.

  Round six's lesson was *a derivation over names cannot see a call that names
  nothing*. Round seven fixed that and inherited the sibling the reviewer
  states as the sentence this round should keep: **a derivation over names must
  also enumerate the GRAMMAR the names can appear in.** Clojure's `.` special
  form has TWO member spellings —

      (. instance-expr member-symbol)
      (. instance-expr (method-symbol args*))

  — and both `..` and `memfn`, macros in `clojure.core` whose entire job is to
  emit the second, produce only the parenthesised one. The three-form rule
  (call / quoted string / dot special form) was right in shape and one
  production short.

  N1 is the one to read first: four tokens, the spelling on Clojure's own
  reference page for the `.` special form, no reflection and no `Reading`
  anywhere, publishing a raw sixteen-digit `nanoTime` into an undeclared
  receipt field INSIDE the hashed parity subject with the gate green.

  N7 is finding 2: the morpheme narrowing's argument was that `Class/forName`
  is the only route to a class, so a clock class needed no spelling but the
  quoted one. On the JVM a class is an ordinary source symbol, so
  `(.getMethod java.util.Calendar \"getInstance\" ...)` reaches the factory with
  neither `java.util.Calendar` (bare) nor `\"getInstance\"` (morpheme-free) a
  clock alternative.

  Two of the reviewer's seven are NOT here and their absence is the record:

  - **N5** `(let [c System] (. c nanoTime))` does not compile. `System`
    evaluates to a `java.lang.Class` object and `(. c nanoTime)` is then an
    INSTANCE member access on it — `IllegalArgumentException: No matching field
    found: nanoTime for class java.lang.Class`. Reaching a static through a
    class held in a local must go back through `.getMethod` with a method-name
    STRING, which the derivation has carried since round seven. The route does
    not exist; it is outside the route set, not inside it and unwatched.
    (Asserted by `the-round-eight-N5-route-does-not-compile` below.)
  - **N6** `(.getDeclaredMethod (class rr) (str \"_lau\" \"nder\") ...)` spells
    the member name nowhere: it exists only at runtime. No source-text scan can
    see it and this branch does not claim to. It is a declared residual, and
    finding 3 widens that declaration from FIELDS to any member reached by a
    computed name."
  [{:label "N1 — the dot special form's PARENTHESISED member, the spelling in Clojure's own reference for `.`"
    :pattern :clock
    :source "(. System (nanoTime))"}
   {:label "N2 — `..`, whose expansion IS the parenthesised member form"
    :pattern :clock
    :source "(.. System (nanoTime))"}
   {:label "N3 — the parenthesised member on the escape hatch: round-six plant H with two characters added"
    :pattern :escape
    :source "(. rr (_launder))"}
   {:label "N4 — `memfn`, which expands to N3 and names the member as an ordinary source token"
    :pattern :escape
    :source "((memfn _launder) rr)"}
   {:label "N7 — a clock source class as a BARE SOURCE SYMBOL, which no alternative spelled at all"
    :pattern :clock
    :source "(.getMethod java.util.Calendar \"getInstance\" (into-array Class []))"}])

;; @spec MCP-OP-TIME-005
;; @spec MCP-OP-TIME-006
(deftest every-round-six-review-plant-is-seen-by-the-scan-that-owns-it
  (testing "a call that names its target as a string or a dot form is scanned"
    (doseq [{:keys [label pattern source]} (concat round-six-review-plants
                                                   round-eight-review-plants)]
      (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                               (str "measured-r6-plant-" (System/nanoTime))))
            victim (io/file root "clj_surgeon" "planted_r6.clj")]
        (.mkdirs (.getParentFile victim))
        (spit victim
              (str "(ns clj-surgeon.planted-r6)\n\n"
                   "(defn publish-a-receipt\n"
                   "  [rr]\n"
                   "  {:ok true :receipt {:verification_wall_ms " source "}})\n"))
        (try
          (let [scanned (scan (case pattern
                                :escape escape-hatch-pattern
                                :clock clock-pattern)
                              root)]
            (is (= #{["src/clj_surgeon/planted_r6.clj" "publish-a-receipt"]}
                   (set (keys scanned)))
                (str "round-six review plant " label
                     " reaches a receipt field unseen by the "
                     (name pattern) " scan: " (pr-str scanned)
                     " — source: " source)))
          (finally
            (.delete victim)
            (.delete (.getParentFile victim))
            (.delete (io/file root))))))))

;; @spec MCP-OP-TIME-005
(deftest the-round-eight-N5-route-does-not-compile
  (testing "a static reached through a class held in a local is not a route at all"
    ;; The round-eight review's N5, recorded as OUTSIDE the route set rather
    ;; than inside it and unwatched. `System` evaluates to a `java.lang.Class`
    ;; OBJECT, so `(. c nanoTime)` is an instance member access on that object
    ;; and there is no such member. To read a static through a class held in a
    ;; local you must go back through `.getMethod` with a method-name STRING,
    ;; which the derivation has carried since round seven.
    ;;
    ;; This is a witness and not a comment because the claim is about the
    ;; RUNTIME, and a runtime that started accepting the form would reopen the
    ;; route silently. If this test ever fails, the escape is real and the
    ;; alternative set is short again.
    (is (thrown? Throwable (eval '(let [c System] (. c nanoTime))))
        (str "(let [c System] (. c nanoTime)) evaluated instead of throwing, so "
             "a clock static IS reachable through a class bound to a local and "
             "no alternative spells that route"))))

;; @spec MCP-OP-TIME-005
;; @spec MCP-OP-TIME-006
(deftest the-computed-member-name-residual-is-declared-at-its-real-width
  (testing "round-eight review §4: the residual is stated, so it is checked"
    ;; `MCP-OP-TIME-005`'s final clause used to promise to catch "any other
    ;; route that spells no class name and no method name the scan can read".
    ;; No source-text scan can do that, and a requirement a scan provably
    ;; cannot satisfy is a requirement that reads as green forever. The
    ;; requirement is now bounded by what a text scan can DECIDE and defers the
    ;; rest to the residual declared in `src/clj_surgeon/measured.clj`, which
    ;; round eight widened from FIELDS to any member reached by a computed
    ;; name.
    ;;
    ;; This witness asserts the residual is REAL — the scan does not see these
    ;; — so that the declaration cannot quietly become false in either
    ;; direction. If a future round closes one of these routes, this test goes
    ;; red and the declaration is corrected in the same commit rather than
    ;; drifting into an overclaim nobody re-read.
    (let [computed-member "(.invoke (.getDeclaredMethod (class subject) (str \"_lau\" \"nder\") (into-array Class [])) subject (into-array Object []))"
          computed-ns "(resolve (symbol (str \"clj-surgeon\" \".measured\") \"value\"))"]
      (is (= {} (scan-one-planted-line escape-hatch-pattern
                                       (str "{:wall_ms " computed-member "}")))
          (str "a COMPUTED member name is now seen by the escape-hatch scan. "
               "That is good news and a documentation bug: widen what "
               "`MCP-OP-TIME-005` promises and narrow the residual paragraph "
               "in src/clj_surgeon/measured.clj, which currently declares this "
               "route as accepted and unreachable by a text scan."))
      (is (= {} (scan-one-planted-line escape-hatch-pattern
                                       (str "{:wall_ms " computed-ns "}")))
          (str "a namespace assembled at runtime is now seen by the "
               "escape-hatch scan; same correction, one level up — the "
               "residual paragraph declares this route too")))))

;; @spec MCP-OP-TIME-006
(deftest the-escape-hatch-scanner-catches-every-route-planted-in-a-receipt
  (testing "each route, planted where a receipt is built"
    (doseq [[spelling why] (sort escape-hatch-spellings-the-ratchet-must-carry)]
      (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                               (str "measured-escape-route-" (System/nanoTime))))
            victim (io/file root "clj_surgeon" "planted_route.clj")]
        (.mkdirs (.getParentFile victim))
        (spit victim
              (str "(ns clj-surgeon.planted-route)\n\n"
                   "(defn publish-a-receipt\n"
                   "  [subject]\n"
                   "  {:ok true :receipt {:wall_ms " (escape-route-call spelling) "}})\n"))
        (try
          (let [planted (scan escape-hatch-pattern root)]
            (is (= {["src/clj_surgeon/planted_route.clj" "publish-a-receipt"] 1}
                   planted)
                (str "the escape-hatch scan did not see a planted " spelling
                     " (" why "): " (pr-str planted))))
          (finally
            (.delete victim)
            (.delete (.getParentFile victim))
            (.delete (io/file root))))))))

;; @spec MCP-OP-TIME-005
(def ^:private reflective-namespace-spelling
  "The measured namespace named as a QUOTED SYMBOL, a STRING or a KEYWORD.

  Every var-resolution API in Clojure takes a namespace this way, and none of
  them respects `^:private`: `(ns-resolve 'clj-surgeon.measured
  'unwrap-readings)` reaches the private tag-stripper at any depth. The
  round-five review's blocking finding 2 is that the naming rule was anchored
  on the SLASH -- `clj-surgeon.measured/` -- and this spelling has a space
  after the namespace instead, so the rule fell through returning nil and there
  was no `measured/` token for the verb scan to check either.

  The sanctioned require `[clj-surgeon.measured :as measured]` names the
  namespace bare, after a `[` or a space, so it is not this. Nothing under the
  scanned roots needs to resolve a measured var at runtime, which is why this
  can be an offence outright rather than a heuristic."
  #"(?:'|\(quote\s+|\"|:)clj-surgeon\.measured(?![-\w.])")

(def ^:private var-resolution-api
  "The verbs that turn a namespace name back into a var.

  The spelling rule above catches the ARGUMENT; this catches the CALL, so a
  resolution whose namespace argument is built some way the first regex does
  not spell is still an offence as long as both sit on one line."
  #"\b(?:ns-resolve|requiring-resolve|find-var|ns-interns|ns-publics|ns-map|intern)\b|\(\s*(?:resolve|var)\s")

(defn- measured-naming-offence
  "Why `code` names the measured namespace outside the sanctioned require, or
   nil when it does not.

   Four doors, each of which defeats every `measured/`-spelled scan in this
   file, and prose that merely MENTIONS the namespace is none of them."
  [code]
  (cond
    (re-find #"clj-surgeon\.measured/" code) :fully-qualified
    (not (re-find #"clj-surgeon\.measured(?![-\w.])" code)) nil
    (or (re-find reflective-namespace-spelling code)
        (re-find var-resolution-api code)) :reflective
    (re-find #":refer" code) :refer
    (re-find #":use" code) :use
    (when-let [m (re-find #"clj-surgeon\.measured\s+:as\s+([\w-]+)" code)]
      (not= "measured" (second m))) :alias))

(defn- measured-naming-offenders
  "Every line under `root` that names the measured namespace in any way other
   than the one sanctioned require."
  ([] (vec (mapcat measured-naming-offenders scanned-roots)))
  ([root]
   (vec
    (for [^java.io.File file (remove #(= measured-namespace-file (site-path % root))
                                     (src-files root))
          [n line] (map-indexed vector (str/split-lines (slurp file)))
          :let [code (str/trim (or (first (str/split line #";;")) ""))
                offence (measured-naming-offence code)]
          :when offence]
      [(site-path file root) (inc n) offence code]))))

;; @spec MCP-OP-TIME-005
(deftest the-measured-namespace-is-named-only-by-the-sanctioned-require
  (testing "a name the scanner does not know is a hole in every scanner"
    (let [offenders (measured-naming-offenders)]
      (is (= [] offenders)
          (str "clj-surgeon.measured named other than `"
               sanctioned-measured-require "`: " (pr-str offenders))))))

;; @spec MCP-OP-TIME-005
(deftest the-require-witness-catches-a-planted-refer
  (testing "the round-three review's second bypass, replanted"
    (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                             (str "measured-refer-plant-" (System/nanoTime))))
          victim (io/file root "clj_surgeon" "planted_refer.clj")]
      (.mkdirs (.getParentFile victim))
      (spit victim
            (str "(ns clj-surgeon.planted-refer\n"
                 "  (:require\n"
                 "   [clj-surgeon.measured :as measured :refer [raw-nanos]]))\n\n"
                 "(defn publish-an-undeclared-clock-field\n"
                 "  [started]\n"
                 "  {:ok false\n"
                 "   :verification_wall_ms (/ (double (- (raw-nanos) started))\n"
                 "                            1000000.0)})\n"))
      (try
        (let [offenders (measured-naming-offenders root)]
          (is (= [["src/clj_surgeon/planted_refer.clj" 3 :refer
                   "[clj-surgeon.measured :as measured :refer [raw-nanos]]))"]]
                 offenders)
              (str "the require witness did not see a planted :refer: "
                   (pr-str offenders))))
        (finally
          (.delete victim)
          (.delete (.getParentFile victim))
          (.delete (io/file root)))))))

;; @spec MCP-OP-TIME-006
(deftest the-require-witness-catches-a-planted-reflective-resolution
  (testing "round-five review finding 2: `ns-resolve` spells the namespace without a slash"
    ;; `unwrap-readings` is private, and privacy in Clojure is a RESOLUTION
    ;; CONVENTION rather than a boundary. The round-five defence against
    ;; reaching past it was the naming rule plus the public-var scan, and
    ;; `(ns-resolve 'clj-surgeon.measured 'unwrap-readings)` defeated both:
    ;; `measured-naming-offence` fired `:fully-qualified` only on
    ;; `clj-surgeon.measured/` — with a TRAILING SLASH — and there is no
    ;; `measured/` token on the line for `unknown-measured-verbs` to check. The
    ;; reviewer put an undeclared clock field in the parity hash subject with
    ;; nineteen tests green.
    (doseq [[label spelling]
            [[:ns-resolve "((ns-resolve 'clj-surgeon.measured 'unwrap-readings) x)"]
             [:resolve "((resolve 'clj-surgeon.measured/unwrap-readings) x)"]
             [:find-var "((find-var 'clj-surgeon.measured/unwrap-readings) x)"]
             [:requiring-resolve "((requiring-resolve 'clj-surgeon.measured/unwrap-readings) x)"]
             [:quote-form "((ns-resolve (quote clj-surgeon.measured) (quote unwrap-readings)) x)"]
             [:string-name "((ns-resolve (symbol \"clj-surgeon.measured\") (quote unwrap-readings)) x)"]
             [:ns-interns "((get (ns-interns 'clj-surgeon.measured) 'unwrap-readings) x)"]
             [:intern "(intern 'clj-surgeon.measured 'sneak identity)"]]]
      (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                               (str "measured-reflective-plant-" (System/nanoTime))))
            victim (io/file root "clj_surgeon" "planted_reflective.clj")]
        (.mkdirs (.getParentFile victim))
        (spit victim
              (str "(ns clj-surgeon.planted-reflective\n"
                   "  (:require\n"
                   "   [clj-surgeon.measured :as measured]))\n\n"
                   "(defn publish-an-undeclared-clock-field\n"
                   "  [x]\n"
                   "  {:ok false\n"
                   "   :verification_wall_ms " spelling "})\n"))
        (try
          (let [offenders (measured-naming-offenders root)]
            (is (= 1 (count offenders))
                (str label ": the require witness saw " (count offenders)
                     " offences on a reflective resolution: " (pr-str offenders)))
            (is (contains? #{:reflective :fully-qualified}
                           (nth (first offenders) 2 nil))
                (str label ": the offence is not typed as a resolution: "
                     (pr-str offenders))))
          (finally
            (.delete victim)
            (.delete (.getParentFile victim))
            (.delete (io/file root))))))))

;; @spec MCP-OP-TIME-005
(deftest a-reading-does-not-open-to-anything-but-the-laundering-verb
  (testing "the round-three review's first bypass is now unrepresentable"
    (let [r (measured/elapsed-ms (measured/start))]
      ;; §1a, verbatim in effect: `(:clj-surgeon.measured/reading r)` was the
      ;; whole bypass. It must not yield a number by any map-shaped route.
      (doseq [[route opened]
              [[:keyword-lookup (:clj-surgeon.measured/reading r)]
               [:get (get r :clj-surgeon.measured/reading)]
               [:get-with-default (get r :clj-surgeon.measured/reading nil)]
               [:first-entry (try (key (first r)) (catch Exception _ nil))]
               [:vals (try (first (vals r)) (catch Exception _ nil))]
               [:seq-first (try (first (seq r)) (catch Exception _ nil))]
               [:deref (try (deref r) (catch Exception _ nil))]]]
        (is (not (number? opened))
            (str "a reading opened to a bare number through " route ": "
                 (pr-str opened))))
      (is (false? (map? r)) "a reading is a map again; §1a reopens")
      (is (false? (coll? r)) "a reading is a collection again; §1a reopens")
      (is (number? (measured/value r))
          "the one laundering verb no longer works")
      (is (= "#clj-surgeon.measured/reading" (pr-str r))
          (str "a reading prints as something other than a stable opaque "
               "marker, so a leak would be non-reproducible as well as wrong: "
               (pr-str (pr-str r))))
      (is (false? (measured/reading? {:clj-surgeon.measured/reading 1.0}))
          "a literal one-key map is a reading again; the map shape is back"))))

;; @spec MCP-OP-TIME-005
(deftest a-readings-hash-carries-no-clock-bits
  (testing "round-four review §3: `(hash r)` was a clock-derived integer"
    ;; The type withholds its number from `pr-str`, `str`, `bean`, `seq`,
    ;; `deref`, `into {}`, cheshire and `read-string` — the reviewer proved all
    ;; of those shut. `hashCode` was the one accessor still consulting it, so
    ;; `:some_field (hash r)` put a clock-varying integer into the parity hash
    ;; subject with no verb any scan matches. `hashCode` never needed the
    ;; number: these types are compared, not bucketed.
    (let [a (measured/reading 1.5)
          b (measured/reading 987654.321)
          t1 (measured/start)
          t2 (measured/start)]
      (is (= (hash a) (hash b))
          (str "two readings with different numbers hash differently, so a "
               "hash is a clock-derived number: " (hash a) " vs " (hash b)))
      (is (= (hash t1) (hash t2))
          (str "two start ticks hash differently: " (hash t1) " vs " (hash t2)))
      (is (= a (measured/reading 1.5))
          "equality stopped consulting the number, which breaks the type's own tests")
      (is (false? (= a b))
          "two readings with different numbers became equal")
      (is (= (hash a) (hash (measured/reading 1.5)))
          "equal readings must hash equally; the contract is not optional"))))

;; @spec MCP-OP-TIME-005
(deftest the-clock-scanner-catches-a-planted-raw-read
  (testing "the ratchet goes RED when the defect is reintroduced"
    (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                             (str "measured-plant-" (System/nanoTime))))
          victim (io/file root "clj_surgeon" "planted.clj")]
      (.mkdirs (.getParentFile victim))
      (spit victim
            (str "(ns clj-surgeon.planted)\n\n"
                 "(defn publish-an-undeclared-clock-field\n"
                 "  [started]\n"
                 "  (let [duration-ms (/ (double (- (System/nanoTime) started))\n"
                 "                       1000000.0)]\n"
                 "    {:ok false :verification_wall_ms duration-ms}))\n"))
      (try
        (let [planted (scan clock-pattern root)]
          (is (= {["src/clj_surgeon/planted.clj" "publish-an-undeclared-clock-field"] 1}
                 planted)
              (str "the scanner did not see a planted raw clock read: "
                   (pr-str planted))))
        (finally
          (.delete victim)
          (.delete (.getParentFile victim))
          (.delete (io/file root)))))))


;; @spec MCP-OP-TIME-007
(def ^:private jdk-reflection-is-complete?
  "Can this runtime reflect over a JDK class it was not built to know about?

  Babashka is a GraalVM native image and carries reflection metadata only for
  registered classes: `java.util.Calendar` answers 57 methods on the JVM and 6
  under babashka. The derivation is therefore PROVEN on the JVM and RUN off the
  floor under babashka -- and this predicate is measured rather than sniffed
  from a property, so a future babashka that registers the class simply starts
  proving the same assertions."
  (delay (<= 20 (count (.getMethods ^Class (class-named "java.util.Calendar"))))))

;; @spec MCP-OP-TIME-007
(deftest the-derived-clock-pattern-carries-every-jdk-time-shape
  (testing "the derivation, not a list, is what makes .lastModified visible"
    (let [derived (set (derived-clock-expressions))
          reflection-thin (set (when-not @jdk-reflection-is-complete?
                                 ["Calendar/getInstance" ".getTimeInMillis"
                                  "(. Calendar getInstance"
                                  "\"getTimeInMillis\""
                                  ;; The `..` and `memfn` faces of the same
                                  ;; underivable class, round-eight finding 1.
                                  "(.. Calendar getInstance"
                                  "(memfn getInstance"
                                  "(memfn getTimeInMillis"
                                  ;; And the bare class symbol, round-eight
                                  ;; finding 2: a class earns that form by
                                  ;; carrying a morpheme-free clock static, and
                                  ;; `getInstance` is not visible here.
                                  "java.util.Calendar"]))
          ;; A spelling the derivation CANNOT produce by construction, and must
          ;; not: the derivation emits the SIMPLE class name, and the
          ;; fully-qualified dot form is a property of the ALTERNATIVE -- the
          ;; optional `(?:[\w.]*\.)?` prefix round-six review finding 3 forced
          ;; onto it. It is carried in the floor anyway so the plant witness
          ;; below drives the exact source the reviewer published a raw
          ;; nanoTime through; asserting it were derived would assert the
          ;; alternative against itself.
          alternative-shape #{"(. java.lang.System nanoTime"}
          missing (vec (sort (remove (some-fn derived reflection-thin
                                              alternative-shape)
                                     (keys clock-expressions-the-ratchet-must-carry))))]
      (is (= [] missing)
          (str "the JDK derivation no longer produces these clock spellings, so "
               "a read written with one of them would be invisible to the scan: "
               (pr-str missing)))
      (is (< 60 (count derived))
          (str "the derivation produced only " (count derived)
               " spellings; reflection is not finding the JDK time methods"))
      (is (contains? derived "Date.")
          "a zero-argument constructor of a time value is not derived, so "
          )
      (is (contains? derived "(. System nanoTime")
          "the dot special form is not derived")
      (is (< 15 (count clock-source-classes))
          (str "the class CLOSURE produced only " (count clock-source-classes)
               " classes; it is a hand-written list again"))
      (is (every? #(contains? (set (map (fn [^Class c] (.getName c)) clock-source-classes)) %)
                  ["java.time.OffsetDateTime" "java.time.LocalTime" "java.util.Date"])
          (str "a class the round-five review named is not in the closure: "
               (pr-str (sort (map (fn [^Class c] (.getName c)) clock-source-classes)))))
      (is (not-any? #(str/includes? (.getName ^Class %) "Duration") clock-source-classes)
          "Duration is a TemporalAmount, and (Duration/ofSeconds 3) is a timeout constant"))))

(def ^:private jvm-clock-manifest-path
  "The clock spellings DERIVED ON A RUNTIME WHOSE REFLECTION IS COMPLETE.

  Regenerated by `make clock-spellings-manifest`, which runs the same
  `derived-clock-expressions` under the JVM and writes the result here. It is
  not a list of spellings somebody thought of and it is not editable by hand
  with any honest effect: the witness below compares it against a live
  derivation in both directions."
  "test/fixtures/clock-spellings-jvm.edn")

(defn- jvm-clock-manifest
  []
  (let [f (io/file jvm-clock-manifest-path)]
    (when (.exists f)
      (read-string (slurp f)))))

;; @spec MCP-OP-TIME-007
(deftest the-babashka-clock-floor-is-the-complete-jvm-difference
  (testing "the floor is the WHOLE difference, not the part somebody noticed"
    ;; Round-six review finding 4. The file's own argument for accepting a
    ;; floor is that `the-derived-clock-pattern-carries-every-jdk-time-shape`
    ;; proves the derivation produces every floor entry on a runtime whose
    ;; reflection is complete -- so the floor is derived evidence rather than a
    ;; list. That argument only holds if the floor is the COMPLETE
    ;; JVM-minus-this-runtime difference. It was computed by hand, it was short
    ;; by exactly one entry (`(. Calendar getInstance`), and NOTHING CHECKED IT:
    ;; the reviewer planted `(. java.util.Calendar getInstance)` and it was
    ;; green under babashka and under the JVM.
    ;;
    ;; The two sets cannot be derived in one process -- that is the whole
    ;; problem -- so the JVM half is a checked-in manifest and this witness is
    ;; the comparison, in BOTH directions: the difference must be covered by the
    ;; floor, and on a complete runtime the manifest must not be stale.
    (let [manifest (jvm-clock-manifest)
          here (set clock-spellings)
          floor (set (keys clock-expressions-the-ratchet-must-carry))]
      (is (seq manifest)
          (str "the JVM clock manifest is missing or empty at "
               jvm-clock-manifest-path
               " -- regenerate it with `make clock-spellings-manifest`; without "
               "it this runtime's floor is unchecked, which is the round-six "
               "state exactly"))
      (let [underived (set/difference (set manifest) here)
            uncovered (vec (sort (set/difference underived floor)))]
        (is (= [] uncovered)
            (str "the JVM derives clock spellings this runtime cannot, and the "
                 "floor does not carry them, so the scan is STRICTLY WEAKER "
                 "here than on the JVM for: " (pr-str uncovered)
                 " -- add each to clock-expressions-the-ratchet-must-carry with "
                 "the route it opens. (This runtime underives "
                 (count underived) " spellings in total.)")))
      (when @jdk-reflection-is-complete?
        (let [stale (vec (sort (set/difference here (set manifest))))]
          (is (= [] stale)
              (str "the derivation produces spellings the checked-in JVM "
                   "manifest does not, so the manifest is STALE and every "
                   "floor conclusion drawn from it is unfounded: " (pr-str stale)
                   " -- regenerate with `make clock-spellings-manifest`")))))))

;; @spec MCP-OP-TIME-007
(deftest the-clock-scanner-catches-every-jdk-time-shape-planted-in-a-receipt
  (testing "each derived spelling, planted where a receipt is built"
    (doseq [[expression why] (sort clock-expressions-the-ratchet-must-carry)]
      (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                               (str "measured-clock-shape-" (System/nanoTime))))
            victim (io/file root "clj_surgeon" "planted_shape.clj")
            call (cond
                   (str/starts-with? expression ".") (str "(" expression " subject)")
                   ;; The dot special form is already an open paren.
                   (str/starts-with? expression "(") (str expression ")")
                   ;; A STRING spelling is planted in the shape that gives a
                   ;; string its power: a fully-qualified class name reaches
                   ;; `Class/forName`, a method name reaches `.getMethod`.
                   ;; Round-six review findings 2 and 3.
                   (str/starts-with? expression "\"")
                   (if (str/includes? expression ".")
                     (str "(Class/forName " expression ")")
                     (str "(.getMethod (class subject) " expression
                          " (into-array Class []))"))
                   ;; A BARE CLASS SYMBOL is planted the way finding 2's N7
                   ;; used it: as a VALUE handed to reflection, which is what
                   ;; makes it a route at all.
                   (and (str/includes? expression ".")
                        (not (str/includes? expression "/")))
                   (str "(.getMethods " expression ")")
                   :else (str "(" expression ")"))]
        (.mkdirs (.getParentFile victim))
        (spit victim
              (str "(ns clj-surgeon.planted-shape)\n\n"
                   "(defn publish-a-receipt\n"
                   "  [subject]\n"
                   "  {:ok true :receipt {:written_at " call "}})\n"))
        (try
          (let [planted (scan clock-pattern root)]
            (is (= {["src/clj_surgeon/planted_shape.clj" "publish-a-receipt"] 1}
                   planted)
                (str "the clock scan did not see a planted " expression
                     " (" why "): " (pr-str planted))))
          (finally
            (.delete victim)
            (.delete (.getParentFile victim))
            (.delete (io/file root))))))))

;; ============================================================
;; 1b. The NAMESPACE's public surface, derived rather than enumerated
;; ============================================================
;;
;; The round-four review closed the TYPE and left the NAMESPACE open
;; (2026-09-04 §1). `measured/unwrap-readings` was a public verb that turned a
;; tagged reading into a bare number at any depth; the escape-hatch scan
;; enumerates laundering verbs BY NAME and had never heard of it, so a one-line
;; plant at `src/clj_surgeon/mcp_hot_verify.clj` reproduced round three's §1a
;; verbatim in effect — an undeclared clock field inside the parity hash
;; subject, `unpartitioned []`, twelve invariant tests green. `measured/field`
;; was the same class from an already-published block, and the branch's own
;; recovery fix called `unwrap-readings` twice in `src/` with no allow-list
;; entry and no witness said anything.
;;
;; The diagnosis is round three's own sentence: **a name the scanner does not
;; know is a hole in every scanner at once.** So neither of the two witnesses
;; below is a list of names.
;;
;;   (i)  a PROBE over `(ns-publics 'clj-surgeon.measured)`, by reflection:
;;        every public fn var is called, at every arity it declares, with a
;;        tagged reading carrying a sentinel number — and it is an offence for
;;        the sentinel to come back anywhere outside a `:measured` block.
;;        Adding a public verb that launders costs a `sanctioned-laundering-vars`
;;        entry, and the entry is refused unless the verb is also in
;;        `escape-hatch-pattern`, which then makes its `src/` call sites cost an
;;        allow-list line. That is the cost on purpose.
;;
;;   (ii) a SOURCE scan asserting that every `measured/<verb>` reference in the
;;        scanned roots names a var that is public in the namespace TODAY. A
;;        call to a private or absent verb is an offence with no list to
;;        consult, which is what makes the reviewer's plant go red: once
;;        `unwrap-readings` is private, `(measured/unwrap-readings ...)` in
;;        `src/` fails this witness by name-independent construction.
;;
;; RESIDUAL, declared and accepted (round-four review §3, sharpened by round
;; six §7): reflection over the type's fields BY ANY ROUTE, NAMED OR
;; POSITIONAL. The named route computes the name —
;; `(.getDeclaredField (class r) (str "launder" "able"))`. The positional route
;; spells no name at all — `(first (.getDeclaredFields (class r)))` plus
;; `setAccessible`, the round-six reviewer's plant F. Neither is visible to a
;; textual scan. The earlier wording said "with a computed field name" and did
;; not cover the positional half, which is a residual understating its own
;; class. The reviewer's sentence stands: "Textual scanning cannot close that,
;; and a deliberate attacker is not the threat model here — record it, do not
;; chase it." A JVM without a security manager cannot prevent reflection, so
;; this is a property of the platform, not a gap in the ratchet.

(def sanctioned-laundering-vars
  "The public vars of `clj-surgeon.measured` that MAY turn a TAGGED READING
  into a bare number, each with the reason.

  An entry here is not enough on its own: the witness below refuses one whose
  name `escape-hatch-pattern` does not match, so sanctioning a verb also makes
  every one of its `src/` call sites cost an `escape-hatch-allow-list` line."
  {"value" "THE ONE door out of a reading; every src call site is named in escape-hatch-allow-list"
   "-launder" "the protocol method `value` is built on, and the only implementation of it"})

(def sanctioned-block-readers
  "The public vars that MAY hand back a number out of an already-published
  measured block.

  DECLARED RESIDUAL, and it is a property of the wire rather than a gap: a
  measured block holds BARE NUMBERS because MEM-005 requires a plain JSON
  number in the published receipt, so `(get-in x [:measured :elapsed_ms])`
  reaches one and no type can prevent that. What this witness can hold is that
  the namespace offers no CONVENIENCE VERB for it — reaching into a block is at
  least as visible as naming the key the partition is defined by. `field` was
  such a verb and the round-four review took it (§1b)."
  {"measured-key" "the well-known key the partition is DEFINED by; its lookup is an ordinary `get`"})

(defn- public-measured-callable-vars
  "Every public var of the measured namespace whose value can be CALLED.

  `ifn?`, not `fn?`: a protocol method is not a `fn` under babashka, and
  `-launder` is precisely the door the pattern exists for — a probe that skips
  it because of its runtime representation is the blind spot again."
  []
  (->> (ns-publics 'clj-surgeon.measured)
       (filter (fn [[_ v]] (ifn? @v)))
       (sort-by first)
       vec))

(defn- public-measured-inert-vars
  "Every public var of the measured namespace the probe did NOT call, with
   whether it is callable after all — the partition has to be total."
  []
  (->> (ns-publics 'clj-surgeon.measured)
       (remove (fn [[_ v]] (ifn? @v)))
       (map (fn [[sym v]] [(name sym) (ifn? @v)]))
       (sort-by first)
       vec))

(defn- sentinel-yielding-var-names
  [pool]
  (->> (public-measured-callable-vars)
       (filter (fn [[_ v]] (yields-the-sentinel? v pool)))
       (map (comp name first))
       sort
       vec))

;; @spec MCP-OP-TIME-006
(deftest the-measured-namespace-exposes-no-unsanctioned-laundering-verb
  (testing "the public surface is derived by reflection, not by a list of names"
    (let [launderers (sentinel-yielding-var-names (reading-arguments))
          unsanctioned (vec (remove (set (keys sanctioned-laundering-vars)) launderers))
          skipped (public-measured-inert-vars)]
      (is (seq (public-measured-callable-vars))
          "the probe found no callable public vars, so it is not probing")
      (is (= [] (vec (filter second skipped)))
          (str "the probe skipped a public var that IS callable: " (pr-str skipped)))
      (is (= [] unsanctioned)
          (str "public vars of clj-surgeon.measured turn a tagged reading into a "
               "bare number outside the measured block with no sanction: "
               (pr-str unsanctioned)))
      (is (= (sort (keys sanctioned-laundering-vars)) launderers)
          (str "the sanctioned set no longer matches what the probe actually "
               "finds; a sanctioned verb that no longer launders means the probe "
               "went blind. probe found: " (pr-str launderers)))
      (is (= [] (vec (remove #(re-find escape-hatch-pattern (str "measured/" %))
                             (sort (keys sanctioned-laundering-vars)))))
          (str "a sanctioned laundering verb that escape-hatch-pattern does not "
               "match: its src call sites would cost nothing")))))

;; @spec MCP-OP-TIME-006
(deftest the-measured-namespace-exposes-no-verb-that-reads-out-of-a-block
  (testing "a published block holds bare numbers; no verb makes reaching in cheap"
    (let [readers (sentinel-yielding-var-names (block-arguments))
          unsanctioned (vec (remove (set (keys sanctioned-block-readers)) readers))]
      (is (= [] unsanctioned)
          (str "public vars of clj-surgeon.measured hand a number out of a "
               "published measured block with no sanction: " (pr-str unsanctioned)))
      (is (= (sort (keys sanctioned-block-readers)) readers)
          (str "the sanctioned block readers no longer match what the probe "
               "finds; probe found: " (pr-str readers))))))

(def ^:private measured-verb-pattern
  "Any `measured/<verb>` reference. The verb is captured so it can be checked
   against the namespace's ACTUAL public vars rather than a list."
  #"measured/([A-Za-z0-9?!*<>=+_'-]+)")

(defn- unknown-measured-verbs
  "Every `measured/<verb>` reference under `root` naming something that is not
   a public var of `clj-surgeon.measured` today."
  ([] (vec (mapcat unknown-measured-verbs scanned-roots)))
  ([root]
   (let [public-names (set (map name (keys (ns-publics 'clj-surgeon.measured))))]
     (vec
      (for [^java.io.File file (remove #(= measured-namespace-file (site-path % root))
                                       (src-files root))
            [n line] (map-indexed vector (str/split-lines (slurp file)))
            :let [code (or (first (str/split line #";;")) "")]
            verb (map second (re-seq measured-verb-pattern code))
            :when (not (contains? public-names verb))]
        [(site-path file root) (inc n) verb])))))

;; @spec MCP-OP-TIME-006
(deftest every-measured-verb-named-in-source-is-a-public-var
  (testing "a call to a private or absent measured verb is an offence by construction"
    (let [offenders (unknown-measured-verbs)]
      (is (= [] offenders)
          (str "measured/<verb> references that are not public vars of "
               "clj-surgeon.measured: " (pr-str offenders))))))

;; @spec MCP-OP-TIME-006
(deftest the-public-var-witness-catches-the-reviewers-unwrap-plant
  (testing "round-four review §1a, replanted: the laundering verb by name"
    (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                             (str "measured-unwrap-plant-" (System/nanoTime))))
          victim (io/file root "clj_surgeon" "planted_unwrap.clj")]
      (.mkdirs (.getParentFile victim))
      (spit victim
            (str "(ns clj-surgeon.planted-unwrap\n"
                 "  (:require\n"
                 "   [clj-surgeon.measured :as measured]))\n\n"
                 "(defn publish-an-undeclared-clock-field\n"
                 "  [started]\n"
                 "  {:ok false\n"
                 "   :verification_wall_ms (measured/unwrap-readings\n"
                 "                           (measured/elapsed-ms started))})\n"))
      (try
        (let [offenders (unknown-measured-verbs root)]
          (is (= [["src/clj_surgeon/planted_unwrap.clj" 8 "unwrap-readings"]]
                 offenders)
              (str "the public-var witness did not see the planted laundering "
                   "verb: " (pr-str offenders))))
        (finally
          (.delete victim)
          (.delete (.getParentFile victim))
          (.delete (io/file root)))))))

;; ============================================================
;; 2. The publication boundary
;; ============================================================

(defn- measured-field
  "One field of `x`'s published measured block.

  A test-local `get-in`, not a verb from the namespace: `measured/field` was a
  public convenience for exactly this and the round-four review took it as a
  laundering route out of a published block (§1b)."
  [x k]
  (get-in x [measured/measured-key k]))

(defn- fixed-clock
  "A clock that ticks by `delta-ns` on its second read."
  [start-ns delta-ns]
  (let [reads (atom 0)]
    #(if (zero? (first (swap-vals! reads inc))) start-ns (+ start-ns delta-ns))))

(defn- publish
  "One public MCP result, as the shared finalizer publishes it."
  [domain-result clock]
  (let [seen (atom nil)]
    (mcp-operation/invoke!
      {:clock-nanos clock
       :execute (constantly domain-result)
       :summarize (constantly "ok")
       :serialize pr-str
       :callback (fn [_ _ result] (reset! seen result))})
    @seen))

;; @spec MCP-OP-TIME-005
(deftest the-request-clock-does-not-survive-the-hashed-channel
  (testing "the reviewer's exact subject: a real public result with a receipt"
    (let [result (publish {:ok true :receipt {:stable :fact}}
                          (fixed-clock 1000000 2500000))
          hashed (measured/hashed-channel result)]
      (is (false? (contains? hashed :elapsed_ms))
          (str "the request clock is inside the hash subject: " (pr-str hashed)))
      (is (= 2.5 (measured-field result :elapsed_ms))
          "the meter went dark; MEM-005's argument is that an unpublished cost
           is one nobody notices regressing")
      (is (= {:ok true :receipt {:stable :fact}} hashed)
          "the hashed channel lost or gained a deterministic fact"))))

;; @spec MCP-OP-TIME-005
(deftest no-measured-field-is-published-outside-the-partition
  (testing "every family of measured receipt field the source scan found"
    (let [domain {:ok true
                  :operation "apply_clojure_changes"
                  :verification {:ok true :elapsed_ms 12.5 :exit 0}
                  :cold-verification {:status :passed :job_elapsed_ms 44.0}
                  :inspection_elapsed_ms 3.25
                  :receipt {:resources {:bytes_scanned 12 :scan_ms 1.5}
                            :commit-window {:max-ns 900 :reopens 0}}}
          result (publish domain (fixed-clock 0 1000000))]
      (is (= [] (measured/unpartitioned-measured-paths result))
          (str "measured fields published outside the partition: "
               (pr-str (measured/unpartitioned-measured-paths result))))
      (is (= 12.5 (measured-field (:verification result) :elapsed_ms))
          "a nested verification clock was dropped rather than partitioned")
      (is (= 1.5 (measured-field (get-in result [:receipt :resources]) :scan_ms))
          "the ls-tree meter was dropped rather than partitioned")
      (is (= 12 (get-in result [:receipt :resources :bytes_scanned]))
          "bytes_scanned is a COUNT and stays in the hashed channel")
      (is (= 0 (get-in result [:receipt :commit-window :reopens]))
          "a deterministic sibling of a measured field was moved with it"))))

(def ^:private sentinel
  "A number no ordinary field carries, so finding it anywhere in a hashed
   channel is proof a planted reading was laundered rather than partitioned."
  424242.5)

(defn- publish-outcome
  "Publish `domain-result` and classify what the boundary did with it.

   `[:published result]`, `[:refused error-type]`, or `[:threw class-name]` —
   and the third is a failure by itself: an unpartitionable reading must be a
   TYPED refusal, never a raw JVM exception the caller cannot dispatch on."
  [domain-result]
  (try
    [:published (publish domain-result (fixed-clock 0 1000000))]
    (catch clojure.lang.ExceptionInfo error
      (if-let [t (:error-type (ex-data error))]
        [:refused t]
        [:threw (.getName (class error))]))
    (catch Exception error
      [:threw (.getName (class error))])))

(defn- sentinel-in-hash?
  "Does the sentinel number survive anywhere in `x`'s hashed channel?"
  [x]
  (let [found (atom false)]
    ((fn walk [node]
       (cond
         @found nil
         (= sentinel node) (reset! found true)
         (map? node) (doseq [[k v] node] (walk k) (walk v))
         (or (vector? node) (seq? node) (set? node)) (doseq [v node] (walk v))
         :else nil))
     (measured/hashed-channel x))
    @found))

;; @spec MCP-OP-TIME-005
(deftest a-clock-reading-never-becomes-a-raw-number-in-any-placement
  (testing "the :control channel's promise, enforced at the boundary"
    ;; The `clock-allow-list`'s 33 `:control` entries each assert in a `:why`
    ;; string that their value is never published. The round-three review's
    ;; §1d is that nothing checked it: the test read the `:channel` keyword and
    ;; nothing else, so a control form could be edited to publish its value and
    ;; the counts would not move.
    ;;
    ;; A `:why` string cannot be made true by a test. What CAN be made true —
    ;; and is the property the `:why` strings are really claiming — is that a
    ;; reading reaching the finalizer from ANYWHERE, in any placement a control
    ;; site could contrive, either lands inside the partition or is refused by
    ;; type. Never a bare number in the hash subject, and never a raw JVM
    ;; exception.
    (doseq [[placement domain expected]
            [[:nested-under-an-undeclared-key
              {:ok true :receipt {:inner {:verification_wall_ms
                                          (measured/reading sentinel)}}}
              :published]
             [:inside-a-row-of-a-vector
              {:ok true :rows [{:file "a.clj"}
                               {:file "b.clj"
                                :lease_wall_ms (measured/reading sentinel)}]}
              :published]
             [:directly-in-a-vector
              {:ok true :rows [(measured/reading sentinel)]}
              :refused]
             [:directly-in-a-set
              {:ok true :s #{(measured/reading sentinel)}}
              :refused]
             [:as-a-map-key
              {:ok true :m {(measured/reading sentinel) :v}}
              :refused]
             [:inside-a-string-keyed-sorted-map
              {:ok true :staged (sorted-map "a.clj" 1
                                            "b.clj" (measured/reading sentinel))}
              :refused]
             ;; Round-five review §4, second residual: the three walkers walked
             ;; CLOJURE collections only, so a reading inside a Java one was
             ;; neither unwrapped, relocated, nor diagnosed -- it reached the
             ;; published result untouched with `unpartitioned []`, and the
             ;; failure only became visible when cheshire refused to encode it.
             [:inside-a-java-list
              {:ok true :rows (java.util.ArrayList. [(measured/reading sentinel)])}
              :refused]
             [:inside-a-java-map
              {:ok true :m (java.util.HashMap. {"b.clj" (measured/reading sentinel)})}
              :refused]]]
      (let [[outcome payload] (publish-outcome domain)]
        (is (= expected outcome)
            (str placement " was " outcome " (" (pr-str payload)
                 "); an unpartitionable reading must be a typed refusal and a"
                 " partitionable one must be relocated"))
        (case outcome
          :published (is (false? (sentinel-in-hash? payload))
                         (str placement ": a clock reading reached the hash "
                              "subject as a bare number: " (pr-str payload)))
          :refused (is (= :unpartitioned-measured-field payload)
                       (str placement ": the refusal is not typed: "
                            (pr-str payload)))
          :threw (is false
                     (str placement ": the boundary threw an untyped "
                          payload " instead of refusing")))))))

;; @spec MCP-OP-MEM-011
;; @spec MCP-OP-TIME-005
(deftest the-parity-hash-is-stable-across-two-runs-with-different-clock-ticks
  (testing "one operation, one unchanged subject, two clocks, one hash"
    (let [domain {:ok true
                  :records [{:file "a.clj" :forms 3} {:file "b.clj" :forms 1}]
                  :receipt {:resources {:bytes_scanned 4096 :scan_ms 41.5}}}
          a (publish domain (fixed-clock 0 1000000))
          b (publish domain (fixed-clock 500 71000000))]
      (is (not= (measured-field a :elapsed_ms) (measured-field b :elapsed_ms))
          "the two clocks ticked identically, so this proves nothing")
      (is (true? (= (pr-str (measured/hashed-channel a))
                    (pr-str (measured/hashed-channel b))))
          (str "two publications of one result hash differently; A "
               (pr-str (measured/hashed-channel a))
               " B " (pr-str (measured/hashed-channel b)))))))

(deftest a-reading-inside-a-java-collection-is-diagnosed
  (testing "round-five review §4: the walkers walked Clojure collections only"
    ;; The reviewer's exact probe, at the tip that shipped:
    ;;   attach fed a reading in ArrayList   => {:ok true, :measured {:xs [#reading]}}
    ;;   partition-measured w/ ArrayList     => {:ok true, :xs [#reading]}
    ;;   unpartitioned paths w/ ArrayList    => []
    ;; A `Reading` cannot be encoded to JSON, so the failure was loud rather
    ;; than silent -- but it was a blind spot in the DIAGNOSTIC, and a blind
    ;; spot is what every finding in five rounds has been.
    (let [xs (java.util.ArrayList. [(measured/reading 1.0)])
          m (doto (java.util.HashMap.) (.put "k" (measured/reading 2.0)))
          nested (java.util.ArrayList. [{:inner (measured/reading 3.0)}])]
      (is (seq (measured/unpartitioned-measured-paths
                 (measured/attach {:ok true} {:xs xs})))
          "a reading inside a java.util.List handed to `attach` is not diagnosed")
      (is (seq (measured/unpartitioned-measured-paths {:ok true :m m}))
          "a reading inside a java.util.Map is not diagnosed")
      (is (seq (measured/unpartitioned-measured-paths {:ok true :rows nested}))
          "a reading under a Clojure map inside a java.util.List is not diagnosed")
      (is (= [] (measured/unpartitioned-measured-paths
                  {:ok true :rows (java.util.ArrayList. ["a" 1])}))
          "a Java collection with nothing measured in it became an offence")))
  (testing "round-six review §5: an ARRAY is neither a Map nor a Collection"
    ;; The round declared this class closed and it was closed for `ArrayList`
    ;; only. An `Object[]` satisfies neither `java.util.Map` nor
    ;; `java.util.Collection`, so the walker returned `[]`, the boundary's typed
    ;; refusal never fired, and the reading travelled to the encoder -- loud,
    ;; because cheshire refuses it, but loud in the WRONG PLACE: an encoder
    ;; stack trace instead of `:unpartitioned-measured-field` naming the path.
    (let [arr (into-array Object [(measured/reading 4.0)])
          nested (into-array Object [{:inner (measured/reading 5.0)}])]
      (is (seq (measured/unpartitioned-measured-paths {:ok true :xs arr}))
          "a reading inside a Java ARRAY is not diagnosed")
      (is (seq (measured/unpartitioned-measured-paths {:ok true :xs nested}))
          "a reading under a Clojure map inside a Java ARRAY is not diagnosed")
      (is (= [] (measured/unpartitioned-measured-paths
                  {:ok true :xs (into-array Object ["a" 1])}))
          "a Java array with nothing measured in it became an offence")
      (is (= [] (measured/unpartitioned-measured-paths
                  {:ok true :xs (into-array Long [1 2 3])}))
          "a primitive-ish array with nothing measured in it became an offence")))
  (testing "round-six review §5: an ITERATOR is REFUSED, not walked"
    ;; The honest answer for an Iterator is a typed refusal rather than a
    ;; diagnosis. Walking it CONSUMES it: the diagnostic would hand the
    ;; boundary a verdict about a value it had just destroyed, and the encoder
    ;; downstream would then serialise an exhausted iterator. So the walker
    ;; reports the path itself as unpartitioned and lets the boundary refuse.
    (let [it (.iterator (java.util.ArrayList. ["a" "b"]))]
      (is (seq (measured/unpartitioned-measured-paths {:ok true :xs it}))
          "an Iterator in a result is not refused")
      (is (.hasNext it)
          "the refusal CONSUMED the iterator it refused, which is the reason it
           is refused rather than walked"))))

(deftest the-partition-shares-structure
  (testing "partitioning a large result may not copy it"
    (let [untouched {:file "a.clj" :forms [1 2 3]}
          x {:rows [untouched] :receipt {:scan_ms 1.0}}
          y (measured/partition-measured x)]
      (is (identical? (first (:rows x)) (first (:rows y)))
          "a sub-value carrying no measured field was rebuilt")
      (is (identical? (measured/partition-measured untouched) untouched)
          "a value with nothing to relocate is not returned identically"))))

;; ============================================================
;; 3. `System/exit` belongs to entrypoints
;; ============================================================

(def exit-allow-list
  "The ONLY forms in `src/` that may call `System/exit`: CLI entrypoints.

  A `-main` owns the process — an exit code is its return value. Anything else
  in `src/` is a library, and a library that exits kills whatever called it:
  the MCP server, a test runner, another tool's JVM. Two such calls lived in
  `run-ls-tree` and `run-fresh-scan` until this witness (`inb-eca3b1`); the
  discovery suite had to shell out to a subprocess to test the op at all."
  #{"-main"})

;; @spec MCP-OP-EXIT-001
(deftest system-exit-appears-only-inside-a-cli-entrypoint
  (testing "a library that exits kills its caller"
    ;; `src/` only: the rule is about a LIBRARY exiting under its caller. The
    ;; `dev/experiments/` scripts are standalone entrypoints run as their own
    ;; process, and an exit code is their return value.
    (let [found (scan #"System/exit" "src")
          offenders (sort (remove #(contains? exit-allow-list (second %))
                                  (keys found)))]
      (is (= [] offenders)
          (str "System/exit outside a -main: " (pr-str offenders)))
      (is (seq found)
          "the scanner found no System/exit at all, so it is not scanning"))))
