#!/usr/bin/env python3
"""Reconcile the generated study and check rendered artifacts for privacy leaks."""
import collections
import csv
import importlib.util
import pathlib
import re
import sys

sys.dont_write_bytecode=True
root=pathlib.Path(__file__).resolve().parent.parent
spec=importlib.util.spec_from_file_location('miner',root/'bin/mine-sessions.py')
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def read(name):
    return list(csv.DictReader((root/name).open(),delimiter='\t'))

rows=read('calls.tsv')
counts={r['metric']:r['value'] for r in read('counts.tsv')}
assert len(rows)==int(counts['surgeon_calls_found'])
assert sum(int(r['calls']) for r in read('calls_per_week.tsv'))==len(rows)
assert sum(int(r['calls']) for r in read('fallback_by_op.tsv'))==len(rows)
assert sum(int(r['fallbacks']) for r in read('fallback_by_op.tsv'))==sum(int(r['fallback']) for r in rows)
assert sum(int(r['retries']) for r in read('fallback_by_op.tsv'))==sum(int(r['retry']) for r in read('retry_evidence.tsv'))
assert sum(int(r['repaired']) for r in read('fallback_by_op.tsv'))==sum(int(r['repaired']) for r in read('retry_evidence.tsv'))
assert len(read('retry_evidence.tsv'))==len(rows)
assert len(read('native_turns.tsv'))==int(counts['native_comparison_turns'])
assert sum(int(r['occurrences']) for r in read('candidates.tsv'))==int(counts['candidate_turns'])
for code in read('refusal_codes.tsv'):
    assert int(code['count'])==sum(r['outcome']=='refusal' and r['refusal_code']==code['code'] for r in rows)
for k in ['success','refusal','error','timeout','unknown']:
    assert int(counts['outcome_'+k])==sum(r['outcome']==k for r in rows)
for store in ('codex','claude'):
    for surface in ('cli','mcp'):
        assert int(counts['surgeon_calls_'+store+'_'+surface])==sum(r['store']==store and r['surface']==surface for r in rows)
assert len(re.findall(r'^## \d+\.',(root/'friction_top20.md').read_text(),re.M))==20
bad=[]
for p in root.iterdir():
    if p.suffix not in ('.tsv','.md'):continue
    for i,line in enumerate(p.open(),1):
        if re.search(r'/(?:home|Users)/[^/\s]+',line):bad.append((p.name,i,'home path'))
        if m.secret_line(line):bad.append((p.name,i,'secret pattern'))
assert not bad, 'Privacy findings (locations only): '+repr(bad)
print('Reconciliation and privacy checks: PASS')
print('METHOD counts')
for metric,value in sorted(counts.items()):print(f'{metric}: {value}')
print('Files written:')
for p in sorted(root.rglob('*')):
    if p.is_file():print(str(p.relative_to(root)))
