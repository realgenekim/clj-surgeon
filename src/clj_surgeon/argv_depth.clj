(ns clj-surgeon.argv-depth
  "The nesting ceiling this tool applies to text it is about to READ, and the
   reader-free scanner that measures it.

   It lives in its own namespace because TWO namespaces need the same number
   and the same scanner: `clj-surgeon.core` bounds one CLI argument, and
   `clj-surgeon.study` bounds a build file the same op DISCOVERS under the
   caller's `:dir`. A second number, or a second scanner, is a second thing to
   keep in step — and `clj-surgeon.core` already requires `clj-surgeon.study`,
   so the shared definitions cannot live in either of them.")

(def max-argument-nesting-depth
  "How deeply one CLI argument may nest before it is refused unread.

   Opus's round-twenty-one item 4. `edn/read-string` is recursive, so a
   10,001-deep nested argument overflowed the reader's stack and left both
   real launchers as an untyped `StackOverflowError` — an `Error`, which
   `-main`'s `catch Exception` never saw. Nothing was evaluated and no caller
   value was published unbounded, which is why the reviewer ruled it
   non-blocking; what it broke is the claim that every refusal the launcher
   prints leaves through ONE bounded exit, because a raw stack trace is a
   refusal no enumeration can drive.

   256, and the number is a CEILING rather than a measurement of what the
   reader survives: every argument this CLI accepts is a path, a keyword, a
   flat list of door symbols or a one-level map, so a legitimate request is
   two or three deep and the ceiling is three orders of magnitude of slack.
   A bound set where the stack happens to give out would move with the JVM,
   the platform and the thread; a bound set where the REQUESTS are is a
   property of the tool."
  256)

(def ^:private opening-delimiters
  "The three characters that open a nesting level in EDN."
  #{\[ \{ \(})

(def ^:private closing-delimiters
  "The three characters that close one."
  #{\] \} \)})

;; @spec MCP-OP-SHELL-ARGV-005
;; @spec MCP-OP-SHELL-ARGV-004
(defn scanned-nesting-depth
  "The deepest run of open delimiters in `s`, measured WITHOUT a reader.

   Character scanning, deliberately: the whole point is to answer \"is this
   too deep to read\" before anything recursive touches it, and a reader that
   throws on depth has already used the stack it was supposed to protect.

   EDN strings and character literals are skipped, so a path or a door name
   containing a bracket is not counted as nesting. It stops as soon as the
   ceiling is exceeded, so the scan is bounded by the answer rather than by
   the argument."
  [^String s]
  (let [n (.length s)]
    (loop [i 0 depth 0 deepest 0 in-string? false escaped? false]
      (if (or (>= i n) (> deepest max-argument-nesting-depth))
        deepest
        (let [c (.charAt s i)]
          (cond
            in-string?
            (recur (inc i) depth deepest
                   (not (and (not escaped?) (= c \")))
                   (and (not escaped?) (= c \\)))

            (= c \") (recur (inc i) depth deepest true false)

            ;; A character literal: `\\[` is a bracket the caller wrote, not a
            ;; delimiter, so the next character is consumed whatever it is.
            (= c \\) (recur (+ i 2) depth deepest false false)

            (opening-delimiters c)
            (let [d (inc depth)] (recur (inc i) d (max deepest d) false false))

            (closing-delimiters c)
            (recur (inc i) (dec depth) deepest false false)

            :else (recur (inc i) depth deepest false false)))))))

;; @spec MCP-OP-CENSUS-034
(defn refuse-over-nested!
  "Throw the DECLARED launcher refusal when one argument nests past the
   ceiling. Named separately from `parse-val` so the two branches that read
   cannot drift into checking different things."
  [^String s]
  (let [measured (scanned-nesting-depth s)]
    (when (> measured max-argument-nesting-depth)
      (throw (ex-info
               (str "an argument nests at least " measured
                    " deep, past the " max-argument-nesting-depth
                    "-level ceiling; it is refused unread, because a reader "
                    "deep enough to measure it is a reader deep enough to "
                    "overflow")
               {:error-type :argument-nesting-too-deep
                :ceiling max-argument-nesting-depth
                :measured measured
                :value s})))))

;; @spec MCP-OP-SHELL-ARGV-007
(defn refuse-over-nested-build-file!
  "Apply the CLI's own nesting ceiling to a build file's bytes, before reading.

   The gap round twenty-two disclosed and the round-23 review reproduced. Argv
   is bounded by `refuse-over-nested!` at 256 levels; the build file this op
   DISCOVERS under the caller's `:dir` was read with no depth bound at all, so
   a 10,001-deep `:paths` overflowed the reader's stack and left through
   `-main`'s last-resort `catch Throwable` at both launchers.

   Why close a gap the review ruled non-blocking: that exit is the LAST RESORT,
   and its own docstring says it exists because `max-argument-nesting-depth` is
   a guess about which `Error` a caller can reach. Taking it on an input that
   can be MEASURED before it is read is using the airbag as a brake. And a
   ceiling enforced on argv but not on a file the same op reads is not a
   ceiling, it is one code path's habit.

   It names the build FILE and not the file's bytes. The argv refusal carries
   `:value` because there the caller's argument IS the subject; here the
   subject is a file the caller can look at, its name is the actionable fact,
   and 20 KB of nested brackets is not information.

   NOTE, on this branch: the discovery kernel this guards lives in
   `clj-surgeon.study`, not in `clj-surgeon.core`, so the control follows its
   kernel there — the same absorption round six made for the trunk's other
   `:ls-tree` controls. The ceiling and the scanner stay shared, here."
  [build-file ^String text]
  (let [measured (scanned-nesting-depth text)]
    (when (> measured max-argument-nesting-depth)
      (throw (ex-info
               (str "the build file " (str build-file)
                    " nests at least " measured " deep, past the "
                    max-argument-nesting-depth "-level ceiling; it is refused "
                    "unread, because a reader deep enough to measure it is a "
                    "reader deep enough to overflow")
               {:error-type :build-file-nesting-too-deep
                :build-file (str build-file)
                :ceiling max-argument-nesting-depth
                :measured measured})))))
