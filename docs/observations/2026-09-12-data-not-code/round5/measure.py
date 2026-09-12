#!/usr/bin/env python3
"""Frozen edit-to-verdict instrument. Design only: no measurement in this block.

init --output DIR --model NAME --effort NAME records HEAD before task 1.
cell --output DIR --checkout ROOT --arm NATIVE|PROBE --task T1..T6|N1|N2
     --repetition 1..3 [--control]
Warm setup, stale-result controls, lease and attestations are prerequisites
specified in ../round3/probe-measure-preregistration.md, never measured here.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

HERE = Path(__file__).resolve().parent
TASKS = json.loads((HERE / 'tasks/manifest.json').read_text())
NONTEST = {'N1': 'clj-surgeon.forms', 'N2': 'clj-surgeon.analyze'}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args], text=True).strip()


def write_new(path, value):
    with path.open('x') as stream:
        json.dump(value, stream, indent=2)
        stream.write('\n')
        stream.flush()
        os.fsync(stream.fileno())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['init', 'cell'])
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--checkout', type=Path, default=Path.cwd())
    parser.add_argument('--model')
    parser.add_argument('--effort')
    parser.add_argument('--arm', choices=['NATIVE', 'PROBE'])
    parser.add_argument('--task', choices=[r['task'] for r in TASKS] + list(NONTEST))
    parser.add_argument('--repetition', type=int, choices=range(1, 7))
    parser.add_argument('--control', action='store_true')
    args = parser.parse_args()
    checkout = args.checkout.resolve()
    output = args.output.resolve()
    assert output.is_relative_to('/var/tmp/forge/probe-measure-fx')
    output.mkdir(parents=True, exist_ok=True)
    subject = git(checkout, 'rev-parse', 'HEAD')
    assert not git(checkout, 'status', '--porcelain'), 'checkout must start clean'
    runner_hash = digest(Path(__file__).read_bytes())
    manifest_hash = digest((HERE / 'tasks/manifest.json').read_bytes())
    for row in TASKS:
        assert digest((HERE / 'tasks' / row['patch']).read_bytes()) == row['sha256']
    if args.action == 'init':
        assert args.model and args.effort
        write_new(output / 'subject.json', dict(subject_sha=subject,
                  runner_sha256=runner_hash, manifest_sha256=manifest_hash,
                  model=args.model, effort=args.effort, recorded_utc_ns=time.time_ns(),
                  source='git rev-parse HEAD', tasks=TASKS))
        return
    frozen = json.loads((output / 'subject.json').read_text())
    assert subject == frozen['subject_sha'], 'subject changed after freeze'
    assert runner_hash == frozen['runner_sha256']
    assert manifest_hash == frozen['manifest_sha256']
    assert args.arm and args.task and args.repetition
    if not args.control:
        assert args.repetition <= 3
    else:
        assert args.arm == 'NATIVE' and args.task in ['T1', 'T4']
    row = next((r for r in TASKS if r['task'] == args.task), None)
    ns = row['namespace'] if row else NONTEST[args.task]
    cell = output / (f'{"control-" if args.control else ""}'
                     f'{args.task}-{args.arm}-{args.repetition}')
    cell.mkdir()  # never overwrite or silently replace an unknown cell
    env = dict(os.environ, TMPDIR='/var/tmp/forge/probe-measure-fx',
               JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/probe-measure-fx')
    if row:
        assert digest((checkout / row['file']).read_bytes()) == row['before_sha256']
        patch = HERE / 'tasks' / row['patch']
        subprocess.run(['git', 'apply', '--check', str(patch)], cwd=checkout, check=True)
    if args.arm == 'NATIVE':
        expr = (f"(require '{ns}) (let [r (clojure.test/run-tests '{ns})] "
                '(prn r) (shutdown-agents) '
                '(System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))')
        argv = ['clojure', '-J-Xmx1024m', '-M:clj-surgeon/test-deps', '-e', expr]
    else:
        identity = checkout / '.clj-surgeon/probe.edn'
        assert identity.is_file(), 'attested warm setup required before T0'
        (cell / 'probe-identity.edn').write_bytes(identity.read_bytes())
        argv = ['bb', '-Xmx1g', '-Djava.io.tmpdir=' + env['TMPDIR'], '-cp',
                str(checkout / 'src') + ':' + str(checkout / 'libs/clj-splice/src'),
                '-m', 'clj-surgeon.core', ':probe', ':ns', ns]
    write_new(cell / 'request.json', dict(subject_sha=subject, task=args.task,
              arm=args.arm, argv=argv, runner_sha256=runner_hash, namespace=ns))
    # T0 is immediately before applying the frozen edit (empty edit for N1/N2).
    t0_utc = time.time_ns()
    t0 = time.monotonic_ns()
    try:
        if row:
            subprocess.run(['git', 'apply', str(patch)], cwd=checkout, check=True)
        command_start = time.monotonic_ns()
        with (cell / 'stdout').open('w') as out, (cell / 'stderr').open('w') as err:
            result = subprocess.run(argv, cwd=checkout, env=env, stdout=out, stderr=err)
        command_end = time.monotonic_ns()
        # Parse actual EDN; neither exit zero nor English prose certifies a verdict.
        parse_expr = '''(require '[clojure.edn :as e] '[cheshire.core :as j])
(let [s (slurp (first *command-line-args*))
      values (keep #(try (e/read-string %) (catch Exception _ nil))
                   (clojure.string/split-lines s))
      r (if (= "PROBE" (second *command-line-args*))
          (e/read-string s) (last (filter map? values)))]
  (print (j/generate-string r)))'''
        parsed = subprocess.run(['bb', '-Xmx1g', '-e', parse_expr, str(cell / 'stdout'), args.arm],
                                env=env, capture_output=True, text=True, check=True)
        receipt = json.loads(parsed.stdout)
        assert isinstance(receipt, dict), 'missing verdict is unknown'
        kind = receipt.get('error-type') or receipt.get('error_type')
        if args.arm == 'NATIVE':
            good = (result.returncode == 0 and receipt.get('fail') == 0
                    and receipt.get('error') == 0
                    and (receipt.get('test', 0) > 0 if row else True))
        else:
            good = (result.returncode == 0 and receipt.get('state') == 'probe-passed'
                    and receipt.get('tests', 0) > 0 and receipt.get('failures') == 0)
        if row:
            good = good and digest((checkout / row['file']).read_bytes()) == row['after_sha256']
        verdict = dict(status='accepted' if good else ('refused' if kind else 'failed'),
                       typed_kind=kind, receipt=receipt, exit=result.returncode,
                       subject_sha=subject, t0_monotonic_ns=t0, t0_utc_ns=t0_utc,
                       command_wall_ms=(command_end-command_start)/1e6)
    except Exception as exc:
        verdict = dict(status='unknown', error=str(exc), subject_sha=subject,
                       t0_monotonic_ns=t0, t0_utc_ns=t0_utc)
    write_new(cell / 'verdict.json', verdict)
    # T1 is sampled after the verdict receipt has been written, flushed and fsynced.
    t1 = time.monotonic_ns()
    write_new(cell / 'timing.json', dict(t0_monotonic_ns=t0, t1_monotonic_ns=t1,
              t1_utc_ns=time.time_ns(), complete_wall_ms=(t1-t0)/1e6,
              units='ms', endpoint='verdict.json durable write'))
    # Preserve the complete edited tree diff. Caller restores only in its owned copy.
    (cell / 'subject.diff').write_bytes(subprocess.check_output(
        ['git', 'diff', '--binary', 'HEAD'], cwd=checkout))


if __name__ == '__main__':
    main()
