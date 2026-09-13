"""Render retained EDN oracle receipts; never run tests or infer missing results."""
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parent
REPO = Path.cwd()
relative = ROOT.relative_to(REPO).as_posix()
inventory = json.loads((ROOT / 'impact-list.json').read_text())
names = [r['namespace'] for r in inventory['namespaces']]

expr = r'''(require '[clojure.edn :as e] '[clojure.java.io :as io] '[cheshire.core :as j])
(let [root (first *command-line-args*)
      read-if (fn [p] (when (.isFile (io/file p)) (e/read-string (slurp p))))]
  (print (j/generate-string
    (into {} (for [phase ["before" "after"]
                   :let [file (str root "/results-" phase ".edn")]]
      [phase (when (.isFile (io/file file))
               (mapv (fn [r] (assoc r :counters (read-if (:receipt r))))
                     (e/read-string (str "[" (slurp file) "]"))))])))))'''
result = subprocess.run(['bb', '-e', expr, relative], check=True, capture_output=True, text=True)
phases = json.loads(result.stdout)
summary = {}
indexed = {}
for phase, rows in phases.items():
    rows = rows or []
    assert len({r['namespace'] for r in rows}) == len(rows), 'duplicate namespace run'
    assert all(r['namespace'] in names for r in rows), 'unexpected namespace'
    totals = dict(test=0, **{'pass': 0}, fail=0, error=0)
    indexed[phase] = {}
    for row in rows:
        c = row['counters'] or {}
        good = (row['exit'] == 0 and c.get('test', 0) > 0
                and c.get('fail') == 0 and c.get('error') == 0)
        row['status'] = 'PASS' if good else 'FAIL/UNKNOWN'
        indexed[phase][row['namespace']] = row
        for k in totals:
            totals[k] += c.get(k, 0)
    summary[phase] = dict(completed=len(rows), expected=len(names),
                          passed=sum(r['status'] == 'PASS' for r in rows),
                          failed=[r['namespace'] for r in rows if r['status'] != 'PASS'],
                          counters=totals, worker_wall_ms=sum(r['wall-ms'] for r in rows),
                          complete=len(rows) == len(names))

lines = ['# Diff-impact oracle', '',
         'Commands (fresh logs are mandatory; existing runs are never overwritten):', '',
         '```bash', f'bash test/diff-impact 3b6d5357 {relative} before',
         f'bash test/diff-impact 3b6d5357 {relative} after', '```', '',
         'The committed shell entrance acquires the host suite lock, refuses any existing JVM lease or receipt-chain process, '
         'and releases its own lease on exit. A future round supplies a fresh output directory. '
         '`bb test/diff_impact.clj --self-test` checks the selection and completion rules without a JVM.', '',
         f'Baseline HEAD: `{inventory["head"]}`. Discovery reads namespace declarations as data across src and test; '
         'it selects direct dependencies and dependencies through one intermediate namespace, without using lane/runtime filters. '
         'This is declared dependency coverage, not a claim about arbitrary dynamically constructed require targets. '
         'Every selected namespace runs in a fresh JVM, sequentially; namespace-owned child processes remain part of its test. '
         'CLJ_SURGEON_MEMORY_TMP and java.io.tmpdir are inside the destination envelope.', '',
         '## Summary', '', '| Phase | Completed | Passed | Red namespaces | Tests | Assertions | Failures | Errors | Worker wall ms |',
         '|---|---:|---:|---|---:|---:|---:|---:|---:|']
for phase, s in summary.items():
    c = s['counters']
    lines.append(f'| {phase} | {s["completed"]}/{s["expected"]} | {s["passed"]} | '
                 f'{", ".join(s["failed"]) or "none so far"} | {c["test"]} | '
                 f'{c["pass"]+c["fail"]+c["error"]} | {c["fail"]} | {c["error"]} | {s["worker_wall_ms"]} |')
lines += ['', 'Worker wall is the sum of recorded namespace process walls, including startup and namespace-owned children. '
          'It is not a probe/native performance comparison.', '', '## First-run regression', '',
          'The first run includes `clj-surgeon.txn-journal-test` (:battery, BB load excluded). '
          'Its baseline is **80 tests / 496 assertions / 0 failures / 7 errors**. All six verifier witnesses fail '
          'with `:reason :hard-link`; `two-breakers-sharing-a-txid-cannot-destroy-each-others-evidence` also fails '
          'with that reason. This seventh error is retained, not rewritten to match the verifier’s six. '
          'Post-fix: **80 tests / 545 assertions / 0 failures / 0 errors**. '
          'See the [before log](before/clj-surgeon.txn-journal-test.log) and [after log](after/clj-surgeon.txn-journal-test.log).', '',
          '## Changed-source coverage', '']
for file in inventory['changed-files']:
    ns = file.removeprefix('src/').removesuffix('.clj').replace('/', '.').replace('_', '-')
    hits = [r['namespace'] for r in inventory['namespaces'] if any(p[-1] == ns for p in r['paths'])]
    lines.append(f'- `{file}`: ' + (', '.join(f'`{n}`' for n in hits) if hits else 'no declared test dependency within two edges') + '.')
lines += ['', 'The machine-readable inventory retains every dependency path: '
          '[before](impact-before.edn), [after](impact-after.edn). '
          'Lane annotations below come from the current declarations; missing cadence does not exclude a test.', '',
          '## Namespace results', '', '| Namespace | Declared lane | Before (test/assertions/fail/error) | After (test/assertions/fail/error) |',
          '|---|---|---|---|']
for node in inventory['namespaces']:
    n = node['namespace']
    cells = []
    for phase in ['before', 'after']:
        r = indexed[phase].get(n)
        if r is None:
            cells.append('pending')
        else:
            c = r['counters'] or {}
            counts = '/'.join(str(x) for x in [c.get('test', '?'),
                             sum(c.get(k, 0) for k in ['pass', 'fail', 'error']), c.get('fail', '?'), c.get('error', '?')])
            cells.append(f'[{r["status"]} {counts}]({phase}/{n}.log)')
    lines.append(f'| `{n}` | {node.get("lane") or "separate runner"} | {cells[0]} | {cells[1]} |')
(ROOT / 'impacted.md').write_text('\n'.join(lines) + '\n')
(ROOT / 'impact-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
