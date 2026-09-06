Overall LAND: **yes — LAND `c371e84c7fe0635c5b239211d0a78120a64e07af`.**

- **(b) NO REAL KEY REACHABLE — GO.** Full-suite `strace` recorded zero `/home/forge/secrets` opens. After deliberately unpinning `KEYS_DIR`, an in-process `provider_key` call resolved the real path but refused with exit 6 before opening it; its trace also had zero hits. The suite’s no-session and no-ledger-growth witnesses passed.
- **(a) Chokepoint-not-sandbox surfaces — GO.** An executed offline, preflight-blocked bench receipt carried `:offline true` and `:offline_contract "chokepoint-not-sandbox"`. The shim comment explicitly lists its bypasses and says neither mechanism catches everything or provides confinement.

This judges the stated witnessed contract, not sandbox/confinement.

### Command and output transcript

```text
$ git rev-parse HEAD origin/fable/typist-real-repo origin/MCP/main
c371e84c7fe0635c5b239211d0a78120a64e07af
c371e84c7fe0635c5b239211d0a78120a64e07af
825a7e64ebf062f5225506d4301b812a8a9f7747
[exit 0]
```

```text
$ strace -f -e trace=openat -o /var/tmp/forge/sol-fence-r6-typist-fx/strace.log bin/typist-run-test
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of claude refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: network open refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run: refusing — offline: spawn of codex refused (TYPIST_OFFLINE=1)
typist-run-test — the OFFLINE fence tests for bin/typist-run (2026-09-06).
OFFLINE CONTRACT: chokepoint-not-sandbox -- TYPIST_OFFLINE=1 refuses the RUNNER'S OWN spawn/connect paths;
  it does NOT confine children and is NOT a sandbox. No OS boundary is available to this user on
  this box (unshare -rn: uid_map not permitted; no bwrap/firejail; no sudo). A general-purpose child
  could still spawn or connect; what this suite proves is that it DID NOT (the ~/.codex/sessions
  path set is unchanged) and that it COULD NOT HAVE SPENT (no real key is reachable: every runner
  subprocess runs --keys-dir /var/tmp/forge/typist-real-fx/keys-test).

in-process key lookup is pinned to the dummy fence         ok  ['/var/tmp/forge/typist-real-fx/keys-test/groq.edn', '/var/tmp/forge/typist-real-fx/keys-test/openrouter.edn']
NO in-process key path touches /home/forge/secrets         ok  ['/var/tmp/forge/typist-real-fx/keys-test/groq.edn', '/var/tmp/forge/typist-real-fx/keys-test/openrouter.edn']
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
in-process key lookup is pinned to the dummy fence         ok  ['/var/tmp/forge/typist-real-fx/keys-test/groq.edn', '/var/tmp/forge/typist-real-fx/keys-test/openrouter.edn']
NO in-process key path touches /home/forge/secrets         ok  ['/var/tmp/forge/typist-real-fx/keys-test/groq.edn', '/var/tmp/forge/typist-real-fx/keys-test/openrouter.edn']
== --keys-dir fence ==
--keys-dir /home/forge is refused                          ok  typist-run: refusing — keys-dir outside the test fence (/home/forge does not resolve under /var/tmp/forge)
--keys-dir /tmp is refused                                 ok  typist-run: refusing — keys-dir outside the test fence (/tmp does not resolve under /var/tmp/forge)
--keys-dir /var/tmp/forge/../../etc is refused             ok  typist-run: refusing — keys-dir outside the test fence (/var/etc does not resolve under /var/tmp/forge)
== write fence ==
TYPIST_FX=/home/forge is refused                           ok  typist-run: refusing — TYPIST_FX artifact root outside the write fence (/home/forge resolves to /home/forge, which is under neither /var/tmp/forge nor /home/forge/src/clj-surgeon-fence)
--fixture /tmp/x is refused                                ok  typist-run: refusing — --fixture preimage directory outside the write fence (/tmp/x resolves to /tmp/x, which is under neither /var/tmp/forge nor /home/forge/src/clj-surgeon-fence)
a symlink under the fence pointing outside is refused      ok  typist-run: refusing — --fixture preimage directory outside the write fence (/var/tmp/forge/fence-symlink-mqb4v24y/looks-inside resolves to /home/forge, which is under neither /var/tmp/forge nor /home/forge/src/clj-surgeon-fence)
NW with an out-of-fence workspace exits 4                  ok  rc=4 typist-run: refusing — TYPIST_FX artifact root outside the write fence (/home/forge/does-not-exist-842b2241adc4 resolves to /home/forge/does-not-exist-842b2241adc4, which is under neither /var/tmp/forge nor /home/forge/src/clj-surgeon-fence)
...and NOTHING was created under TYPIST_FX                 ok  /home/forge/does-not-exist-842b2241adc4
NW with a workspace outside the scratch fence is refused   ok  typist-run: refusing — NW: sandbox unavailable — workspace not under the fence (/home/forge/src/clj-surgeon-fence/target/fence-probe-fx/warm-ws-scope-roots resolves to /home/forge/src/clj-surgeon-fence/target/fence-probe-fx/warm-ws-scope-roots, not under /var/tmp/forge). Arm NW runs codex with --dangerously-bypass-approvals-and-sandbox; its ONLY containment is this directory, so a workspace outside it is an unsandboxed child with no fence at all.
...and it refused BEFORE materializing anything            ok  /home/forge/src/clj-surgeon-fence/target/fence-probe-fx
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
the offline PATH shim is installed under /var/tmp/forge    ok  /var/tmp/forge/typist-offline-shim-z7il_wj4
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
--cost-report: an unpriceable groq row prints unknown      ok  groq                            1          0           0          0         1      unknown
--cost-report: the TOTAL of only-unpriced rows is unknown  ok  TOTAL                           1                                           1      unknown
--cost-report: no $0.0000 anywhere in the table            ok
--cost-report: the receipt's :total_usd is nil, not 0      ok  :total_usd nil
--cost-report: a mixed group keeps the provider's figure   ok  groq                            2         10          10          0         1  $0.12345678
--cost-report: the table prints at least 6 decimals        ok  groq                            2         10          10          0         1  $0.12345678
--cost-report: the RECEIPT round-trips 0.12345678 exactly  ok  :cost_usd 0.12345678
--cost-report: :total_usd is the provider figure, unrounded ok  :total_usd 0.12345678
--cost-report: no truncated 0.123 in the receipt           ok
--cost-report: the unpriced member is still counted UNKNOWN ok
== no real key reachable ==
--print-key-paths exits 0 and prints the contract          ok  exit=0 ['offline=True offline_contract=chokepoint-not-sandbox']
groq key path resolves under the dummy fence               ok  /var/tmp/forge/typist-real-fx/keys-test/groq.edn
openrouter key path resolves under the dummy fence         ok  /var/tmp/forge/typist-real-fx/keys-test/openrouter.edn
NO resolved key path touches /home/forge/secrets           ok  {'groq': '/var/tmp/forge/typist-real-fx/keys-test/groq.edn', 'openrouter': '/var/tmp/forge/typist-real-fx/keys-test/openrouter.edn', 'spark': '(none -- no provider key)'}
--print-key-paths never prints a key VALUE                 ok
without --keys-dir the groq path is still the fixed registry path ok  '/home/forge/secrets/groq.edn'
the registry path witness resolves and opens nothing       ok  KEYS_DIR=/var/tmp/forge/typist-real-fx/keys-test
== syscall witness ==
no openat under strace names /home/forge/secrets           ok  n/a: this pass is ALREADY traced (TracerPid=2387137) -- the outer tracer's log is the witness

== no-spend witness ==
the ~/.codex/sessions PATH SET is identical before and after ok  added=[] removed=[]
~/.clj-surgeon/events.jsonl did not grow                   ok  before=(True, 20781) after=(True, 20781)
no receipt under the suite fx tree records a provider call ok  [] (fx=/var/tmp/forge/typist-real-fx/suite-d046c909)

typist-run-test: all checks ok
[exit 0]
```

```text
$ grep -c '/home/forge/secrets' /var/tmp/forge/sol-fence-r6-typist-fx/strace.log
0
[exit 1: grep’s normal no-match status]
```

The hostile probe used throwaway copies:

```text
$ cp bin/typist-run bin/typist-run-test /var/tmp/forge/sol-fence-r6-typist-fx/
[no output; exit 0]
```

```text
$ strace -f -e trace=openat -o /var/tmp/forge/sol-fence-r6-typist-fx/adversarial.log python3 -c 'import importlib.machinery, importlib.util; p="/var/tmp/forge/sol-fence-r6-typist-fx/typist-run-test"; loader=importlib.machinery.SourceFileLoader("typist_run_test_probe", p); spec=importlib.util.spec_from_loader("typist_run_test_probe", loader); t=importlib.util.module_from_spec(spec); loader.exec_module(t); m=t.load_runner(); m.KEYS_DIR=None; print("KEYS_DIR=%r resolved=%s" % (m.KEYS_DIR, m.key_file_for(m.PROVIDERS["groq"]))); m.provider_key(m.PROVIDERS["groq"])'
typist-run: refusing — TYPIST_OFFLINE=1 and the resolved key path is not under /var/tmp/forge: /home/forge/secrets/groq.edn (offline mode forbids opening a real key; pass --keys-dir)
in-process key lookup is pinned to the dummy fence         ok  ['/var/tmp/forge/typist-real-fx/keys-test/groq.edn', '/var/tmp/forge/typist-real-fx/keys-test/openrouter.edn']
NO in-process key path touches /home/forge/secrets         ok  ['/var/tmp/forge/typist-real-fx/keys-test/groq.edn', '/var/tmp/forge/typist-real-fx/keys-test/openrouter.edn']
KEYS_DIR=None resolved=/home/forge/secrets/groq.edn
[exit 6]
```

```text
$ grep -c '/home/forge/secrets' /var/tmp/forge/sol-fence-r6-typist-fx/adversarial.log
0
[exit 1: grep’s normal no-match status]
```

Offline, preflight-blocked bench:

```text
$ TYPIST_OFFLINE=1 TYPIST_FX=/var/tmp/forge/sol-fence-r6-typist-fx/bench bin/typist-run --bench --providers groq --rounds 1 --keys-dir /var/tmp/forge/sol-fence-r6-typist-fx/empty-keys
bench: groq is BLOCKED — Groq key at /var/tmp/forge/sol-fence-r6-typist-fx/empty-keys/groq.edn (no-key); it will show refusals, never a time

BENCH  mission=scope-roots  dossier=typist-dossier.md  rounds=1  k=1  round-robin
--------  -------------------  ---------------  -------------------------  ------  ---  ---------------  ---------  --------
provider  model                rounds verified  first_verified_s (sorted)  median  max  med resp_wall_s  med tok/s  refusals
--------  -------------------  ---------------  -------------------------  ------  ---  ---------------  ---------  --------
groq      openai/gpt-oss-120b  0/1              n/a                        n/a     n/a  n/a              n/a        1
--------  -------------------  ---------------  -------------------------  ------  ---  ---------------  ---------  --------

bench table written to /var/tmp/forge/sol-fence-r6-typist-fx/bench/bench-1788666567.edn
[exit 0]
```

```text
$ rg -o ':offline true|:offline_contract "[^"]+"' /var/tmp/forge/sol-fence-r6-typist-fx/bench/bench-1788666567.edn
:offline true
:offline_contract "chokepoint-not-sandbox"
[exit 0]
```

Shim comment:

```text
$ sed -n '151,181p' bin/typist-run
#   (a) THE CHOKEPOINT TABLE below -- every spawn and every network entry
#       point in the standard library that this process can reach is wrapped
#       at import, in one place. A shell STRING (os.system, os.popen, or any
#       subprocess call with shell=True) is scanned as a string, so
#       `codex exec ...` inside it is refused with the same exit 6.
#   (b) THE PATH SHIM -- under TYPIST_OFFLINE the runner prepends a temp dir
#       holding executable `codex` and `claude` scripts that print the typed
#       refusal and exit 6, so a shell string this process never sees the
#       inside of (`bash -c 'codex ...'`, an alias, a Makefile, a child of a
#       child) that resolves the NAME `codex` gets the refusal instead of the
#       real binary.
#
#       SOL FENCE R4 DEMONSTRATED ITS BYPASSES, and they are written here
#       rather than left as an impression: the shim is a NAME redirect on one
#       inherited variable, not a boundary. A child that invokes the ABSOLUTE
#       path, that rewrites or resets PATH, that runs under `env -i`, or that
#       re-execs through anything restoring the original environment reaches
#       the real binary. (a) fails loud in-process and (b) covers name lookups
#       (a) structurally cannot see -- NEITHER catches everything, and the
#       pair is not confinement. That is why the contract is named
#       chokepoint-not-sandbox and why the suite proves a WITNESSED guarantee
#       (no real key reachable; no session created; ledger did not grow)
#       instead of an impossibility claim.
#
# THE CHOKEPOINT TABLE (wrapped at import when OFFLINE):
#   os.system, os.popen
#   os.execl/execle/execlp/execv/execve/execvp/execvpe
#   os.spawnl/spawnle/spawnlp/spawnv/spawnve/spawnvp/spawnvpe, os.posix_spawn
#   subprocess.run, subprocess.call, subprocess.check_call,
#   subprocess.check_output, subprocess.Popen
#   urllib.request.urlopen, http.client.HTTPConnection/HTTPSConnection,
[exit 0]
```

Final tree state:

```text
$ git status --short
[no output; exit 0]
```

The explicit `/var/tmp/forge/sol-fence-r6-typist-fx` scratch tree and test-generated `bin/__pycache__` were removed; neither is recoverable. No prohibited port or `make mcp-test` was used.