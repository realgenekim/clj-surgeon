#!/usr/bin/env python3
"""Untimed apparatus checks only; never invokes measure.py cell or a JVM."""
import ast
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
from types import SimpleNamespace

HERE = Path(__file__).resolve().parent
BASE = HERE.parent
CHECKOUT = Path(sys.argv[1]).resolve()
OUTPUT = Path(sys.argv[2]).resolve()
SUBJECT = '759974c718889c52a05a1d0746c9c21d0f00dcdb'
ORIGINAL = '7182573b67f7f02e821b3eb6376f93530b5a2882'

def git(*args):
    return subprocess.check_output(['git', '-C', str(CHECKOUT), *args])

def digest(data):
    return hashlib.sha256(data).hexdigest()

assert git('rev-parse', 'HEAD').decode().strip() == SUBJECT
assert not git('status', '--porcelain')
runner = BASE / 'measure.py'
source = runner.read_text()
old_source = subprocess.check_output(['git', 'show', ORIGINAL + ':' + str(runner.relative_to(Path.cwd()))]).decode()
# All assertions except the deliberately amended cleanliness assertion are identical.
assertions = lambda s: [ast.dump(n) for n in ast.walk(ast.parse(s)) if isinstance(n, ast.Assert)
                        and not (isinstance(n.msg, ast.Constant) and n.msg.value == 'checkout must start clean')]
assert assertions(source) == assertions(old_source)
main = next(n for n in ast.parse(source).body if isinstance(n, ast.FunctionDef) and n.name == 'main')
start = next(i for i,n in enumerate(main.body) if isinstance(n, ast.Assign) and any(isinstance(t, ast.Name) and t.id == 'status_paths' for t in n.targets))
clean_gate = compile(ast.Module(body=main.body[start:start+2], type_ignores=[]), str(runner), 'exec')

def gate(arm):
    try:
        exec(clean_gate, {'args': SimpleNamespace(arm=arm), 'checkout': CHECKOUT,
                         'git': lambda root, *args: git(*args).decode().strip()})
        return True
    except AssertionError:
        return False

checks = []
def check(name, expected):
    actual = {arm: gate(arm) for arm in ['PROBE', 'NATIVE', None]}
    assert actual == dict(zip(['PROBE', 'NATIVE', None], expected)), (name, actual)
    checks.append({'case': name, 'accepted': {str(k): v for k,v in actual.items()}})

check('clean', [True, True, True])
identity = CHECKOUT / '.clj-surgeon'
identity.mkdir()
(identity / 'identity-fixture').write_text('apparatus fixture; no live image\n')
check('root identity directory only', [True, False, False])
for relative in ['amendment1-untracked', '.clj-surgeon-other/file', 'nested/.clj-surgeon/file']:
    p = CHECKOUT / relative
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text('unrelated dirt\n')
    check('reject ' + relative, [False, False, False])
    p.unlink()
    while p.parent != CHECKOUT:
        p = p.parent
        p.rmdir()
p = CHECKOUT / 'README.md'
with p.open('ab') as stream:
    stream.write(b'\nAmendment apparatus tracked-dirt fixture\n')
check('tracked unstaged outside identity', [False, False, False])
git('add', '--', 'README.md')
check('tracked staged outside identity', [False, False, False])
git('restore', '--staged', '--worktree', '--', 'README.md')
(identity / 'identity-fixture').unlink()
identity.rmdir()
identity.write_text('a file is not the exempt directory\n')
check('root .clj-surgeon regular file', [False, False, False])
identity.unlink()

rows = json.loads((BASE / 'tasks/manifest.json').read_text())
results = []
for row in rows:
    patch = BASE / 'tasks' / row['patch']
    assert digest(patch.read_bytes()) == row['sha256']
    assert digest((CHECKOUT / row['file']).read_bytes()) == row['before_sha256']
    git('apply', '--check', str(patch))
    git('apply', str(patch))
    assert digest((CHECKOUT / row['file']).read_bytes()) == row['after_sha256']
    git('restore', '--', row['file'])
    results.append(dict(row, identity_gate='passed', apply_check_exit=0, after_identity='passed'))
assert (BASE / 'tasks/SHA256SUMS').read_text() == ''.join(r['sha256']+'  '+r['patch']+'\n' for r in rows)
non_tests = []
for task, ns, file in [('N1','clj-surgeon.forms','src/clj_surgeon/forms.clj'), ('N2','clj-surgeon.analyze','src/clj_surgeon/analyze.clj')]:
    data = (CHECKOUT / file).read_bytes()
    assert data == git('show', '3b6d5357:' + file)
    assert re.search(r'\(ns\s+' + re.escape(ns) + r'\b', data.decode())
    assert not re.search(r'\((?:[\w.-]+/)?deftest\s|:test\b', data.decode())
    non_tests.append(dict(task=task, namespace=ns, file=file, sha256=digest(data),
                          unchanged_from_design_subject=True, static_non_test_check='passed'))
assert not git('status', '--porcelain')
command = ['python3', str(runner), 'init', '--output', str(OUTPUT), '--model', 'gpt-6', '--effort', 'high']
subprocess.run(command, cwd=CHECKOUT, check=True)
frozen = json.loads((OUTPUT / 'subject.json').read_text())
assert frozen['subject_sha'] == SUBJECT
assert frozen['runner_sha256'] == digest(runner.read_bytes())
assert frozen['manifest_sha256'] == digest((BASE / 'tasks/manifest.json').read_bytes())
assert frozen['tasks'] == rows
assert sorted(p.name for p in OUTPUT.iterdir()) == ['subject.json']
receipt = dict(subject_sha=SUBJECT, runner_old_sha256=digest(old_source.encode()),
               runner_new_sha256=digest(runner.read_bytes()), other_assertions_unchanged=True,
               cleanliness_cases=checks, tasks=results, planted_non_tests=non_tests,
               init_command=command, init_cwd=str(CHECKOUT), init_exit=0,
               measured_cells_run=0, jvms_started=0, subject=frozen)
(HERE / 'verification.json').write_text(json.dumps(receipt, indent=2)+'\n')
print('PASS: cleanliness boundary; all other assertions unchanged; six before/apply/after gates; N1/N2 static identities; init; zero cells/JVMs')
