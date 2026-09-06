# r4 verdict

**Overall LAND: no.**

```text
(3) RUNNER SAFETY: HOLD — TYPIST_OFFLINE remains bypassable
(5) COST ACCOUNTING: GO
```

## (3) Runner safety — HOLD

The claimed boundary at [bin/typist-run](/home/forge/src/clj-surgeon-fence/bin/typist-run:175) is not hard:

- Absolute and relative `codex` paths execute from a spawned shell.
- Resetting `PATH` or using `env -i` bypasses the shim.
- A spawned Python process can use `os.execv`, `os.system`, `os.popen`, or `subprocess(..., shell=True)`.
- `socket.socket.connect` is not wrapped.
- Spawned Python connects through `urllib`, `http.client`, and `socket`.
- Direct `os.spawn*` wrappers are incorrect: those functions take `mode` before `path`, contrary to [the wrapper’s assumption](/home/forge/src/clj-surgeon-fence/bin/typist-run:259). They create a child and return 127 instead of refusing before spawn with exit 6.
- A renamed executable also defeats the basename filter.

The built-in test only checks a descendant that preserves the shimmed `PATH`, so it passes despite these escapes.

Adversarial execution:

```text
$ TYPIST_OFFLINE=1 python3 - <<'PY'
[inline probe importing bin/typist-run, using harmless /bin/sh symlinks named
codex, enumerating subprocess/os.exec*/spawn*/posix_spawn, shell/Python
descendants, and local-only network sentinels]
PY
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
[repeated typed refusals from the direct cases]
fake=/var/tmp/forge/sol-r4-bypass-nwcl6y2q/fake-bin/codex
runner_OFFLINE=True shim=/var/tmp/forge/typist-offline-shim-dh_tf7pt
== direct subprocess/os shell surfaces ==
subprocess.run absolute fake codex: REFUSED exit=6
subprocess.Popen relative ./codex: REFUSED exit=6
subprocess.call codex: REFUSED exit=6
subprocess.check_call codex: REFUSED exit=6
subprocess.check_output codex: REFUSED exit=6
subprocess.run shell=True codex: REFUSED exit=6
subprocess.run shell=True absolute fake: REFUSED exit=6
os.system codex: REFUSED exit=6
os.system absolute fake: REFUSED exit=6
os.popen codex: REFUSED exit=6
os.popen absolute fake: REFUSED exit=6
== direct os.exec*/spawn*/posix_spawn ==
os.execl absolute: child-exit=6
os.execle absolute: child-exit=6
os.execlp PATH: child-exit=6
os.execv absolute: child-exit=6
os.execve absolute: child-exit=6
os.execvp PATH: child-exit=6
os.execvpe PATH: child-exit=6
os.spawnl absolute: RETURN rc=127 stdout='' stderr=''
os.spawnle absolute: RETURN rc=127 stdout='' stderr=''
os.spawnlp PATH: RETURN rc=127 stdout='' stderr=''
os.spawnv absolute: RETURN rc=127 stdout='' stderr=''
os.spawnve absolute: RETURN rc=127 stdout='' stderr=''
os.spawnvp PATH: RETURN rc=127 stdout='' stderr=''
os.spawnvpe PATH: RETURN rc=127 stdout='' stderr=''
os.posix_spawn absolute: REFUSED exit=6
os.posix_spawnp PATH: REFUSED exit=6
== from bash/python descendants ==
bash -c codex inherited PATH: RETURN rc=6 stdout='' stderr='typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)'
bash -c absolute fake codex: RETURN rc=42 stdout='FAKE_CODEX_RAN' stderr=''
bash -c relative ./codex: RETURN rc=42 stdout='FAKE_CODEX_RAN' stderr=''
bash PATH reset then codex: RETURN rc=42 stdout='FAKE_CODEX_RAN' stderr=''
bash env -i then codex: RETURN rc=42 stdout='FAKE_CODEX_RAN' stderr=''
bash python -c re-exec absolute codex: RETURN rc=42 stdout='FAKE_CODEX_RAN' stderr=''
bash python -c os.system absolute codex: RETURN rc=0 stdout='FAKE_CODEX_RAN' stderr=''
bash python -c os.popen absolute codex: RETURN rc=0 stdout='FAKE_CODEX_RAN' stderr=''
bash python -c subprocess shell=True absolute codex: RETURN rc=42 stdout='FAKE_CODEX_RAN' stderr=''
== in-process network surfaces (no external endpoint) ==
urllib.request.urlopen: REFUSED exit=6
http.client.HTTPConnection: REFUSED exit=6
http.client.HTTPSConnection: REFUSED exit=6
socket.create_connection: REFUSED exit=6
socket.socket.connect AF_UNIX: RETURN rc=None stdout='' stderr=''
== network from spawned Python (loopback ephemeral only) ==
loopback-test-port=41819
spawned python urllib connects: RETURN rc=0 stdout='OK' stderr=''
spawned python http.client connects: RETURN rc=0 stdout='OK' stderr=''
spawned python socket connects: RETURN rc=0 stdout='CONNECTED' stderr=''
```

Additional indirect cases:

```text
$ PYTHONDONTWRITEBYTECODE=1 TYPIST_OFFLINE=1 python3 - <<'PY'
[inline env-i, re-exec, renamed-executable, saved-function and shell-function probes]
PY
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
FAKE_CODEX_RAN
subprocess env -i PATH reset: rc=42 stdout='FAKE_CODEX_RAN' stderr=''
subprocess python -c re-exec: rc=42 stdout='FAKE_CODEX_RAN' stderr=''
direct renamed symlink: rc=43 stdout='RENAMED_EXEC_RAN' stderr=''
saved _real_subprocess_run: REFUSED exit=6
saved _real_os_system: rc=10752 stdout='' stderr=''
bash function codex uses absolute: rc=42 stdout='FAKE_CODEX_RAN' stderr=''
```

`strace` confirms `spawnv` creates a child before the eventual exit 127:

```text
$ TYPIST_OFFLINE=1 strace -f -e trace=clone,fork,vfork,execve python3 - <<'PY'
[inline os.spawnv probe against a harmless fake codex]
PY
execve("/usr/bin/python3", ["python3", "-"], 0x7ffc63afdb40 /* 55 vars */) = 0
clone(child_stack=NULL, flags=CLONE_CHILD_CLEARTID|CLONE_CHILD_SETTID|SIGCHLDstrace: Process 1860080 attached
, child_tidptr=0x7d564c4f64d0) = 1860080
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
[pid 1860080] +++ exited with 127 +++
--- SIGCHLD {si_signo=SIGCHLD, si_code=CLD_EXITED, si_pid=1860080, si_uid=1011, si_status=127, si_utime=0, si_stime=0} ---
spawnv-result=127
+++ exited with 0 +++
```

Required fix: this guarantee cannot be provided by Python monkeypatching plus a mutable `PATH`. Offline execution needs an OS-enforced process/network boundary or a strictly confined broker; otherwise any allowed general-purpose child can spawn or connect.

## (5) Cost accounting — GO

The implementation now serializes cost fields without the generic three-decimal rounding at [bin/typist-run](/home/forge/src/clj-surgeon-fence/bin/typist-run:1151), prints sufficient precision, preserves `:total_usd` at [bin/typist-run](/home/forge/src/clj-surgeon-fence/bin/typist-run:3026), and keeps unpriced values unknown.

Independent receipt execution:

```text
$ TYPIST_OFFLINE=1 python3 - <<'PY'
[inline creation of mixed and unpriced receipts, real --cost-report execution,
and printing of generated receipt cost fields]
PY
== mixed exit=0 ==

COST REPORT   prices dated 2026-09-06   fx: /var/tmp/forge/sol-r4-cost-mixed-j7gg0g4g
provider/upstream           calls     prompt  completion  reasoning  no-usage      est USD
groq                            2         10          10          0         1  $0.12345678
TOTAL                           2                                           1  $0.12345678
reasoning tokens are INCLUDED in the completion column (both providers bill them as output).
1 call(s) have no rate and are UNKNOWN, not zero.
cost report written to /var/tmp/forge/sol-r4-cost-mixed-j7gg0g4g/cost-report-1788664901.edn
-- receipt cost fields --
        :cost_usd 0.12345678
        :cost_source "provider"}]
 :total_usd 0.12345678
 :unpriced_calls 1}
== unpriced exit=0 ==

COST REPORT   prices dated 2026-09-06   fx: /var/tmp/forge/sol-r4-cost-unpriced-ugt_7uq5
provider/upstream           calls     prompt  completion  reasoning  no-usage      est USD
groq                            1          0           0          0         1      unknown
TOTAL                           1                                           1      unknown
reasoning tokens are INCLUDED in the completion column (both providers bill them as output).
1 call(s) have no rate and are UNKNOWN, not zero.
cost report written to /var/tmp/forge/sol-r4-cost-unpriced-ugt_7uq5/cost-report-1788664901.edn
-- receipt cost fields --
        :cost_usd nil
        :cost_source "no rate"}]
 :total_usd nil
 :unpriced_calls 1}
```

## Required suite

```text
$ bin/typist-run-test
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of claude refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: network open refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
== scrubber ==
scrub('OpenRouter request failed: gsk_DUM')                ok  'OpenRouter request failed: <redacted>'
scrub('key sk-or-v1_abcDEF-09 rejected')                   ok  'key <redacted> rejected'
scrub('Authorization: Bearer sk-live.ABC-')                ok  'Authorization: <redacted>'
scrub('nothing secret here')                               ok  'nothing secret here'
scrub('two gsk_AAA and sk-or-BBB')                         ok  'two <redacted> and <redacted>'
scrub is pure (same input, same output)                    ok
scrub passes non-strings through                           ok
scrub is total (no key survives a receipt string)          ok
== typed errors ==
classify HTTPError 404 -> http-4xx                         ok
classify HTTPError 502 -> http-5xx                         ok
classify TimeoutError -> timeout                           ok
classify URLError -> transport                             ok
classify ValueError -> parse                               ok
classify Exception('gsk_DUMMY123') -> transport, no text   ok
generic_error carries no exception text                    ok
== routing pin ==
  openrouter request body: {"max_tokens": 6000, "model": "openai/gpt-oss-120b", "provider": {"allow_fallbacks": false, "order": ["Cerebras", "Groq"]}, "temperature": 0.0, "usage": {"include": true}}
openrouter pins the upstream order unconditionally         ok
openrouter uses the REGISTRY model                         ok  openai/gpt-oss-120b
no --model flag is registered                              ok
a pinned upstream is accepted                              ok  None
an UNPINNED upstream is the typed refusal upstream-mismatch ok  upstream-mismatch
a missing upstream is refused too                          ok
groq carries NO provider block (it is not a router)        ok
groq uses the registry model                               ok
the retired TYPIST_OPENROUTER_ORDER cannot change the pin  ok
== key fence ==
no TYPIST_*_KEY_FILE read remains                          ok
groq key path is fixed                                     ok
openrouter key path is fixed                               ok
--keys-dir redirects the groq key file                     ok
preflight passes on a mode-600 dummy key                   ok
preflight refuses a key file that is not mode 600          ok
preflight refuses a missing key file                       ok
== --keys-dir fence ==
--keys-dir /home/forge is refused                          ok
--keys-dir /tmp is refused                                 ok
--keys-dir /var/tmp/forge/../../etc is refused             ok
== write fence ==
TYPIST_FX=/home/forge is refused                           ok
--fixture /tmp/x is refused                                ok
a symlink under the fence pointing outside is refused      ok
NW with an out-of-fence workspace exits 4                  ok
...and NOTHING was created under TYPIST_FX                 ok
NW with a workspace outside the scratch fence is refused   ok
...and it refused BEFORE materializing anything            ok
...and it refused before any codex call                    ok
== offline guard ==
TYPIST_OFFLINE=1 is read once, at import                   ok
offline refuses a codex spawn (exit 6)                     ok  exit=6
offline refuses a claude spawn (exit 6)                    ok  exit=6
offline refuses a /usr/local/bin/codex spawn (exit 6)      ok  exit=6
offline leaves ordinary tools (clojure) alone              ok
the guard sits at the subprocess CHOKEPOINT, not at call sites ok  exit=6
offline refuses a network open (exit 6)                    ok  exit=6
offline refuses os.system('codex ...') (exit 6)            ok  exit=6
offline refuses a shell string (shell=True) (exit 6)       ok  exit=6
a bash -c trampoline hits the PATH shim (exit 6)           ok  exit=6 'typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)'
the offline PATH shim is installed under /var/tmp/forge    ok
offline still RUNS an ordinary tool (echo)                 ok  exit=0 'ordinary'
== cost accounting ==
a candidate with missing usage is (None, None), never zero ok
a missing completion count is unknown, not zero            ok
a complete usage pair prices from the dated table          ok  0.00075 table:2026-09-06:groq
a provider-reported cost wins over the table               ok
run_cost of one priced + one unpriced is the priced one    ok
run_cost of all-unpriced is None, never 0.0                ok
run_cost sums every priced candidate                       ok
candidate_cost keeps the provider's own figure verbatim    ok
candidate_cost refuses to price a call with no usage       ok
--cost-report: an unpriceable groq row prints unknown      ok
--cost-report: the TOTAL of only-unpriced rows is unknown  ok
--cost-report: no $0.0000 anywhere in the table            ok
--cost-report: the receipt's :total_usd is nil, not 0      ok
--cost-report: a mixed group keeps the provider's figure   ok
--cost-report: the table prints at least 6 decimals        ok
--cost-report: the RECEIPT round-trips 0.12345678 exactly  ok
--cost-report: :total_usd is the provider figure, unrounded ok
--cost-report: no truncated 0.123 in the receipt           ok
--cost-report: the unpriced member is still counted UNKNOWN ok

no codex session was created by this suite                 ok

typist-run-test: all checks ok
```

Final pins and cleanliness:

```text
$ git rev-parse HEAD
f8156d39bee63c39b659b5b608a4c1be060314bb
$ git rev-parse fable/typist-real-repo
f8156d39bee63c39b659b5b608a4c1be060314bb
$ git rev-parse origin/fable/typist-real-repo
f8156d39bee63c39b659b5b608a4c1be060314bb
$ git rev-parse origin/MCP/main
645636b6e0011c9da0466307bb23b8ea6d1ed0f8
$ git status --short
[no output]
```

`origin/MCP/main` advanced externally from `249b774aad…` to `645636b6e…` during review; the reviewed branch SHA remained exact. No source edits were made. No prohibited port was contacted—the only TCP connection probe used loopback port `41819`—and `make mcp-test` was not run.