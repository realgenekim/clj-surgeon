# SOL caller report 2

## Outcome and comparison

Mission `M-1` reached `:verified`, with a receipt for 6 helpers, 31 caller files, 66 rewritten sites, 33 changed files, alias histogram `{"response" 29, "resp" 1}`, and a successful fresh-process `mission-proof`. `resume M-1` then selected undo, reported `:state :undone`, and verified whole-file read-back for all 33 files. The final recursive diff against a separately materialized fixture PRE was empty.

| Probe | First-command to verified | Model returns | Result |
|---|---:|---:|---|
| Probe 1 | 6m11s | 24 | Never verified |
| Probe 2 | 2m20.826416299s | 11 completed-command decision returns through the verified receipt | Verified |

Probe 2's UTC boundary stamps were `19:24:39.623283772` immediately before the first mission command and `19:27:00.449700071` immediately after its output showed `:state :verified`. The ledger's own verified transition was at `19:26:57.917246060`, 2m18.293962288s after the starting stamp; the externally observed, requested stamp-to-stamp measurement is 2m20.826416299s.

Return-count convention: I counted each completed command/tool result after which I had to decide the next command. I did not count one intermediate process-yield while the batched help command was still running. Counting the required post-receipt timestamp result as a return would make the number 12 rather than 11.

## Exact command chronology and wall times

The wall values below are `/usr/bin/time %e` where present; for `date`, the host command runner's displayed wall is used. Commands 3–9 were issued together as seven newline-separated commands, so there was no model return between them.

1. Start boundary (`0.1s` host wall):

   ```bash
   date -u +%T.%N
   ```

2. General help (`4.74s`):

   ```bash
   /usr/bin/time -f 'WALL_SECONDS=%e' ./bin/mission help
   ```

3. Open help (`4.89s`):

   ```bash
   /usr/bin/time -f 'HELP_OPEN_WALL_SECONDS=%e' ./bin/mission help open
   ```

4. Plan help (`4.99s`):

   ```bash
   /usr/bin/time -f 'HELP_PLAN_WALL_SECONDS=%e' ./bin/mission help plan
   ```

5. Show help (`4.94s`):

   ```bash
   /usr/bin/time -f 'HELP_SHOW_WALL_SECONDS=%e' ./bin/mission help show
   ```

6. Apply help (`4.59s`):

   ```bash
   /usr/bin/time -f 'HELP_APPLY_WALL_SECONDS=%e' ./bin/mission help apply
   ```

7. Resume help (`4.81s`):

   ```bash
   /usr/bin/time -f 'HELP_RESUME_WALL_SECONDS=%e' ./bin/mission help resume
   ```

8. Ready help (`4.88s`):

   ```bash
   /usr/bin/time -f 'HELP_READY_WALL_SECONDS=%e' ./bin/mission help ready
   ```

9. List help (the capture truncated after its JVM banner, so its per-command `%e` line was not observable; the seven-command runner was observed across a `11.0s` initial yield plus a `15.6s` completion wait):

   ```bash
   /usr/bin/time -f 'HELP_LIST_WALL_SECONDS=%e' ./bin/mission help list
   ```

10. List help re-read after the truncation (`5.02s`):

    ```bash
    /usr/bin/time -f 'HELP_LIST_REREAD_WALL_SECONDS=%e' ./bin/mission help list
    ```

11. Fixture materialization, omitting empty PRE entries (`0.02s`):

    ```bash
    /usr/bin/time -f 'MATERIALIZE_WALL_SECONDS=%e' bb -cp test:src -e '(require '\''clj-surgeon.helper-extraction-fixture) (doseq [{:keys [file pre]} (remove (comp empty? :pre) (clj-surgeon.helper-extraction-fixture/files :happy))] (let [f (java.io.File. "/var/tmp/forge/sol-mission-fx2/ws" file)] (.mkdirs (.getParentFile f)) (spit f pre)))'
    ```

12. Scratch marker/config write via the provided patch facility (`0.005s`), equivalent content exactly:

    ```diff
    *** Begin Patch
    *** Add File: /var/tmp/forge/sol-mission-fx2/ws/deps.edn
    +{:paths ["src" "test"]}
    *** Add File: /var/tmp/forge/sol-mission-fx2/ws/.clj-surgeon.edn
    +{:verification-profiles {"mission-proof" {:commands [["/bin/true"]]}}}
    *** End Patch
    ```

13. Open-and-plan (`5.06s`):

    ```bash
    printf '%s\n' '{:verb "helper_extraction", :question "Move the six HTTP response helpers into a focused response namespace", :request {:op "helper_extraction", :workspace_root "/var/tmp/forge/sol-mission-fx2/ws", :from {:file "src/acid/web/http.clj"}, :to {:lib "acid.web.response", :alias_policy ["response" "resp"]}, :helpers ["html-response" "see-other" "text-response" "plain-not-found" "json-response" "with-etag"], :scope {:paths ["src/**/*.clj"]}, :verification {:profile "mission-proof"}}}' | /usr/bin/time -f 'OPEN_WALL_SECONDS=%e' ./bin/mission open --spec-file - --state-home /var/tmp/forge/sol-mission-fx2/state
    ```

14. Ready query (`0.04s`):

    ```bash
    /usr/bin/time -f 'READY_WALL_SECONDS=%e' ./bin/mission ready --workspace /var/tmp/forge/sol-mission-fx2/ws --state-home /var/tmp/forge/sol-mission-fx2/state
    ```

15. Human index (`0.04s`):

    ```bash
    /usr/bin/time -f 'LIST_WALL_SECONDS=%e' ./bin/mission list --workspace /var/tmp/forge/sol-mission-fx2/ws --state-home /var/tmp/forge/sol-mission-fx2/state
    ```

16. Pre-apply mission/config inspection (`0.09s`):

    ```bash
    /usr/bin/time -f 'SHOW_READY_WALL_SECONDS=%e' ./bin/mission show M-1 --workspace /var/tmp/forge/sol-mission-fx2/ws --state-home /var/tmp/forge/sol-mission-fx2/state
    ```

17. Apply and verify (`5.24s`; receipt's operation elapsed was `53.19927ms`):

    ```bash
    /usr/bin/time -f 'APPLY_WALL_SECONDS=%e' ./bin/mission apply M-1 --workspace /var/tmp/forge/sol-mission-fx2/ws --state-home /var/tmp/forge/sol-mission-fx2/state
    ```

18. End boundary immediately after the verified receipt (`0.0s` host wall):

    ```bash
    date -u +%T.%N
    ```

19. Resume, which selected undo from `:verified` (`5.05s`):

    ```bash
    /usr/bin/time -f 'RESUME_UNDO_WALL_SECONDS=%e' ./bin/mission resume M-1 --workspace /var/tmp/forge/sol-mission-fx2/ws --state-home /var/tmp/forge/sol-mission-fx2/state
    ```

20. Independent fixture-PRE oracle materialization (`0.02s`):

    ```bash
    /usr/bin/time -f 'MATERIALIZE_PRE_ORACLE_WALL_SECONDS=%e' bb -cp test:src -e '(require '\''clj-surgeon.helper-extraction-fixture) (doseq [{:keys [file pre]} (remove (comp empty? :pre) (clj-surgeon.helper-extraction-fixture/files :happy))] (let [f (java.io.File. "/var/tmp/forge/sol-mission-fx2/fixture-pre" file)] (.mkdirs (.getParentFile f)) (spit f pre)))'
    ```

21. Recursive byte diff (`0.00s`, no output, exit 0). The only exclusions are the two intentionally added workspace markers, which are not fixture files:

    ```bash
    /usr/bin/time -f 'DIFF_PRE_WALL_SECONDS=%e' diff -ru --exclude=deps.edn --exclude=.clj-surgeon.edn /var/tmp/forge/sol-mission-fx2/fixture-pre /var/tmp/forge/sol-mission-fx2/ws
    ```

## What I wanted to type versus what I had to type

I wanted one compact intent command along the lines of `mission extract --from src/acid/web/http.clj --to acid.web.response --aliases response,resp --helpers ... --verify mission-proof`, followed by `mission apply M-1` and `mission resume M-1`, with workspace and state home remembered by the mission.

I had to pipe a full closed EDN request that duplicates `helper_extraction` as both `:verb` and `:request :op`, supplies the absolute workspace root, nests the source file and destination namespace, spells the alias policy and all six helpers as vectors, supplies a glob scope and verification profile, then repeat `--workspace` and `--state-home` on every later ledger command. The new help made that request discoverable and copyable without source-reading; `open` also performed planning immediately, so a separate `plan` command was unnecessary.

The ledger itself felt trustworthy and legible: `ready`, the compact fixed-column `list`, and `show` made the next action and admitted config explicit; the apply receipt carried useful closure/count/alias/proof facts; and `resume` made reversal obvious and independently checkable. The friction is mostly ceremony and cold-start latency: each help/open/apply/resume JVM invocation was about five seconds even though ready/list/show were warm-fast and the actual extraction receipt reported only 53ms. One surprising detail is that the ledger honored the custom state home for missions, while the apply receipt paths printed under `/home/forge/.local/state/...` rather than the supplied `/var/tmp/forge/sol-mission-fx2/state`.

## Refusals

None. I hit no refusal, so there was no refusal decision name to assess. The only retry was `help list`, and that was caused by output-capture truncation, not a mission refusal.

## Reach-for sentence

I would reach for the mission ledger over `rg` plus `apply_patch` for this 33-file/66-site extraction because its planned closure, proof receipt, and verified undo buy real safety, but for a one-site edit I would use `rg` plus `apply_patch` because the closed-spec ceremony and roughly five-second JVM startup outweigh the ledger benefit.
