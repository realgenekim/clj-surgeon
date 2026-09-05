(ns clj-surgeon.spawn-ledger
  "TEST-ISO-002 -- AN APPEND-ONLY RECORD OF EVERY CHILD PROCESS THIS JVM
   STARTED, so a child that has already exited is still evidence.

   THE DEFECT THIS EXISTS FOR, from the round-three landing review's finding 6:
   `mcp-inspect-tool-test` is declared `:fast` -- a lane whose written rule is
   `No child process` -- and it drives `/bin/sh -c 'printf cold-ok'` through
   the production cold-verify helper, then WAITS for it to finish. Two
   independent controls were blind to it:

     the SOURCE SCAN  looks for `ProcessBuilder`, `babashka.process` and the
                      like in the TEST file. The spawn is in
                      `clj-surgeon.mcp-process/run-bounded!`, three namespaces
                      away. A scanner derived from names cannot see a call
                      that names nothing (the scanner-brief lesson).

     the PID DIFF     `ProcessHandle/descendants` is a set of LIVE children.
                      The test waits for the child to exit before returning,
                      so at the closing probe there is nothing to see. Round
                      one's 40 ms sampler missed it for the same reason.

   Both controls observe STATE. A process that ran and exited leaves no state.
   So this records the EVENT instead: the production spawn helpers append one
   entry per launch, and the fixture diffs the ledger across a namespace the
   same way it diffs the live pid set. Verify records of execution, never
   source text and never a snapshot that the subject can outlive.

   REACH, stated rather than left to be inferred: this sees a spawn only from
   a repository-owned helper that calls `record!`. A test that builds its own
   `ProcessBuilder` is caught by the source scan and, if it is still running,
   by the pid diff -- and if it is neither, it is invisible here. The
   `no-src-spawn-site-is-unrecorded` witness in
   `clj-surgeon.ns-isolation-test` holds the src half of that closed by
   enumerating the spawn sites and requiring each to record."
  (:require [clojure.string :as str])
  (:import (java.util List)))

(defonce ledger
  ^{:doc "Append-only within one JVM. Never rewritten, never trimmed: it is an
          event log, and a ledger that forgets is a corrupted witness."}
  (atom []))

(defn record!
  "Append one launch. Returns `pid`, so a call site can wrap its own value."
  [pid command]
  (swap! ledger conj
         {:pid pid
          :command (str (if (instance? List command)
                          (str/join " " (map str command))
                          (str command)))
          :at-ns (System/nanoTime)})
  pid)

(defn snapshot
  "The ledger as it stands. A value, so two of them bound a window."
  []
  @ledger)

(defn recorded-between
  "The entries appended between snapshots `before` and `after`. Append-only
   means a count is a cursor."
  [before after]
  (vec (drop (count (or before [])) (or after []))))
