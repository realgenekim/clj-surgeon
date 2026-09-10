(ns clj-surgeon.gate-memory
  "TEST-ISO-015 -- THE gate's available-memory reader. One reader, one answer.

   WHY THIS NAMESPACE EXISTS. On 2026-09-10 the skiff (Darwin arm64 25.5.0)
   ran `make install`; its preflight printed

       memory                 read from vm_stat + sysctl hw.memsize

   and then `make test` refused with

       gate-refused: available memory is unknown {:os \"Mac OS X\"}

   Both lines were produced on the same box, minutes apart, about the same
   question. The preflight had never called a reader at all: it ran
   `command -v vm_stat` and PRINTED A SOURCE, which is a second implementation
   of the rule -- and a second implementation is free to be green while the
   real one refuses. The preflight's own temp-base check already carries the
   correct instruction in its comment: ASK THE RATCHET ITSELF, never a second
   implementation of its rule. This namespace is the ratchet to ask.

   So: `bin/install-preflight` shells out to `-main` here, and
   `clj-surgeon.battery-parallel-runner` -- the coordinator `make test` runs --
   calls `available-mib` here. There is no third copy, and the preflight's
   green is now the gate's green BY CONSTRUCTION rather than by agreement.

   (`test/gate_slot.py` holds the per-acquire re-read for the box-wide
   semaphore, in python, because that process must not pay a JVM/bb start on
   every slot. `test/oracles/test_gate_slot.py` pins the two to the same
   arithmetic; `test/gate_memory_one_reader_test.sh` pins THIS reader to what
   the preflight prints.)

   IT IS SMALL AND ITS REQUIRES ARE SMALL ON PURPOSE. The preflight runs on a
   box that has just been handed the repository and may be about to learn that
   its babashka is too old; loading the whole coordinator to ask how much
   memory there is would make an unrelated analysis error look like a memory
   fault."
  (:require
   [babashka.process :as proc]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def reclaimable-classes
  "The vm_stat classes counted as available on darwin.

   `MemAvailable` has no darwin equivalent. These are the closest honest
   analogue: pages the VM can hand a new JVM without swapping."
  ["Pages free" "Pages inactive" "Pages speculative" "Pages purgeable"])

(defn shell-result
  "Exit code, trimmed stdout and trimmed stderr of a command.

   A reader that can only answer `nil` cannot say WHY it could not answer, and
   that is exactly what the skiff receipt cost: `available memory is unknown`
   named the OS and nothing else, so a missing `vm_stat`, a `vm_stat` that
   exited non-zero, and a `vm_stat` whose output did not parse were one
   indistinguishable failure. Every mode is carried out of here by name.

   `apply`, NOT `(process opts argv)`, AND THAT IS THE WHOLE DARWIN DEFECT.
   babashka.process takes the command as VARARGS after the options map; handed
   a collection it calls `str` on it and tries to launch a program literally
   named `(vm_stat)`. Measured 2026-09-10 on bb 1.12.209, bb 1.13.219 and JVM
   babashka/process 0.6.25 -- all three: `Cannot run program \"(echo\"`. So the
   old reader's `vm_stat` shell-out could not have succeeded on ANY box, and
   `available memory is unknown` on the skiff was not a parse failure or a
   missing binary: the command was never run. Nothing caught it because linux
   answers from /proc/meminfo and never reaches this call."
  [& argv]
  (try
    (let [{:keys [exit out err]} @(apply proc/process {:out :string :err :string} argv)]
      {:exit exit :out (str/trim (or out "")) :err (str/trim (or err ""))})
    (catch Exception error
      {:exit :not-executable :out "" :err (or (.getMessage error) (str error))})))

(defn- unknown!
  "The one refusal shape. It names the step that could not answer, so the next
   operator reads a diagnosis instead of a platform name."
  [source step detail]
  (throw (ex-info "gate-refused: available memory is unknown"
                  (merge {:os (System/getProperty "os.name")
                          :source source
                          :step step
                          :remedy (str "set GATE_MEMAVAIL_MIB to the MiB this box "
                                       "may lend the gate")}
                         detail))))

(defn source
  "Which reader answers on this box: `:linux` or `:darwin`.

   `GATE_MEMORY_SOURCE` forces one. That escape is not a convenience: it is the
   only way the darwin path is witnessed at all, because the boxes that run
   this suite are linux and the box that reported the defect is a laptop
   nobody can reach from here. `GATE_SLOT_BACKEND` gives the socket backend the
   same escape for the same reason."
  []
  (let [declared (some-> (System/getenv "GATE_MEMORY_SOURCE") str/trim str/lower-case)]
    (cond
      (contains? #{nil "" "auto"} declared) (if (.exists (io/file "/proc/meminfo")) :linux :darwin)
      (= "linux" declared) :linux
      (= "darwin" declared) :darwin
      :else (throw (ex-info "gate-refused: unknown GATE_MEMORY_SOURCE"
                            {:value declared :expected ["auto" "linux" "darwin"]})))))

(defn linux-mib-from
  "MiB from /proc/meminfo text, or a typed refusal naming what was missing."
  [text]
  (or (some-> (re-find #"MemAvailable:\s+(\d+)" (or text "")) second parse-long (quot 1024))
      (unknown! :linux "/proc/meminfo" {:reason :no-memavailable-line})))

(defn- linux-mib []
  (let [file (io/file "/proc/meminfo")]
    (when-not (.exists file)
      (unknown! :linux "/proc/meminfo" {:reason :absent}))
    ;; JDK buffered slurp calls available(), which procfs rejects on this
    ;; host. NIO reads it directly.
    (linux-mib-from (java.nio.file.Files/readString
                      (java.nio.file.Paths/get "/proc/meminfo" (make-array String 0))))))

(defn darwin-mib-from
  "MiB from `vm_stat` output and the `hw.memsize` byte count.

   Free + inactive + speculative + purgeable pages, CAPPED by hw.memsize so a
   misparse cannot invent capacity. Pure, so the parse is witnessed without
   spawning anything -- the spawning path is witnessed by
   `test/gate_memory_one_reader_test.sh` with fakes on PATH."
  [stat total-bytes]
  (let [text (or stat "")
        page (or (some-> (re-find #"page size of (\d+) bytes" text) second parse-long) 4096)
        pages (keep (fn [label]
                      (some-> (re-find (re-pattern (str "(?m)^" label ":\\s+(\\d+)")) text)
                              second parse-long))
                    reclaimable-classes)]
    (when (empty? pages)
      (unknown! :darwin "vm_stat" {:reason :no-reclaimable-classes
                                   :expected reclaimable-classes}))
    (quot (min (* (reduce + pages) page) total-bytes) (* 1024 1024))))

(defn- darwin-mib []
  (let [stat (shell-result "vm_stat")]
    (when-not (= 0 (:exit stat))
      (unknown! :darwin "vm_stat" {:reason :command-failed
                                   :exit (:exit stat) :stderr (:err stat)}))
    (let [memsize (shell-result "sysctl" "-n" "hw.memsize")]
      (when-not (= 0 (:exit memsize))
        (unknown! :darwin "sysctl -n hw.memsize" {:reason :command-failed
                                                  :exit (:exit memsize) :stderr (:err memsize)}))
      (let [total (parse-long (:out memsize))]
        (when-not total
          (unknown! :darwin "sysctl -n hw.memsize" {:reason :unparsed :output (:out memsize)}))
        (darwin-mib-from (:out stat) total)))))

(defn available-mib
  "Available MiB, or a typed refusal naming the step and the documented
   override. `GATE_MEMAVAIL_MIB` is honoured FIRST on every platform: a box
   whose memory the gate cannot read may declare it rather than be locked out
   of its own suite."
  []
  (if-let [declared (System/getenv "GATE_MEMAVAIL_MIB")]
    (or (parse-long (str/trim declared))
        (throw (ex-info "gate-refused: GATE_MEMAVAIL_MIB is not an integer MiB count"
                        {:value declared})))
    (case (source)
      :linux (linux-mib)
      :darwin (darwin-mib))))

(defn preflight-line
  "The ONE line `bin/install-preflight` prints for memory, computed by the
   reader `make test` uses. `OK <n>` or `REFUSED <message> <data>`; never a
   claim about which source is installed."
  []
  (try
    (let [declared (System/getenv "GATE_MEMAVAIL_MIB")
          mib (available-mib)]
      (format "OK %d MiB available (%s)"
              mib
              (if declared
                "declared by GATE_MEMAVAIL_MIB"
                (str "read by the gate's " (name (source)) " reader"))))
    (catch Throwable error
      (format "REFUSED %s %s" (ex-message error) (pr-str (ex-data error))))))

(defn -main
  "`bb --classpath test -m clj-surgeon.gate-memory` prints `preflight-line`.
   Exit 0 either way: the preflight reports, it does not adjudicate."
  [& _]
  (println (preflight-line))
  (flush))
