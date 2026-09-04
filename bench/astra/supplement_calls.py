#!/usr/bin/env python3
"""Conservative runtime-event supplement; never execute or interpret arbitrary JS."""
import collections
import hashlib
import json
from pathlib import Path
import sys

ROOT=Path('/var/tmp/forge/astra-program')

def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p): return [(n,json.loads(l)) for n,l in enumerate(p.read_text().splitlines(),1)]

def classify(item):
    if item.get('type')=='FileChange': return 'file-change'
    command=item.get('command',[])
    if len(command)!=3 or command[:2]!=['/bin/bash','-lc']: return 'unclassified-runtime-command'
    text=command[2]
    if text in ('bin/fan-test','./bin/fan-test'): return 'behavioral-test-command'
    if text=='bb test/load_all.clj && git status --short': return 'namespace-load-check-and-status'
    if text.startswith('git diff --check &&'): return 'static-diff-and-residue-check'
    if text.startswith('pwd && rg -l '): return 'target-discovery-and-status'
    if text.startswith('for f in $(rg -l '): return 'source-read-loop'
    if text.startswith("sed -n "): return 'file-reading-and-search'
    return 'unclassified-runtime-command'


def build(arm):
    rows=read(arm/'rollout.jsonl'); watch=read(arm/'watch.jsonl')
    calls=[(n,r) for n,r in rows if r.get('type')=='response_item' and r.get('payload',{}).get('type') in ('custom_tool_call','function_call')]
    outputs={r['payload']['call_id']:(n,r) for n,r in rows if r.get('payload',{}).get('type') in ('custom_tool_call_output','function_call_output')}
    wcalls=[(n,r) for n,r in watch if r.get('kind')=='call']
    if len(calls)!=len(wcalls): raise ValueError('outer-call count differs')
    out=[]
    for seq,((line,r),(wline,w)) in enumerate(zip(calls,wcalls),1):
        p=r['payload']; args=p.get('input',p.get('arguments',''))
        if not isinstance(args,str) or hashlib.sha256(args.encode()).hexdigest()!=w['args_sha256']:
            raise ValueError('watch/rollout arguments hash differs')
        end,_=outputs[p['call_id']]
        enclosed=[]
        for iline,ir in rows:
            ip=ir.get('payload',{}); item=ip.get('item',{})
            if line<iline<end and ip.get('type')=='item_completed' and item.get('type') in ('CommandExecution','FileChange','McpToolCall'):
                witness=dict(rollout_line=iline,item_type=item['type'],item_id=item.get('id'),
                             category=classify(item),status=item.get('status'),exit_code=item.get('exit_code'),
                             command=item.get('command'),files=sorted(item.get('changes',{})),
                             started_at_ms=ip.get('started_at_ms'),completed_at_ms=ip.get('completed_at_ms'))
                output=item.get('stdout',item.get('formatted_output','')) or ''
                witness['output_receipts']=[s for s in output.splitlines() if s.startswith(('FAN-TEST ','LOAD-OK ','Ran '))]
                enclosed.append(witness)
        out.append(dict(seq=seq,call_id=p['call_id'],rollout_call_line=line,rollout_output_line=end,
                        watch_line=wline,outer_tool=p.get('name'),args_len=len(args),
                        watcher_return_ordinal=w['n'],watcher_test_call=w['test_call'],
                        watcher_apply_patch=w['apply_patch'],watcher_patch_files=w['patch_files'],
                        enclosing_interval_unambiguous=not any(line<ln<end for ln,_ in calls),
                        runtime_events=enclosed))
    items=[(n,r['payload']) for n,r in rows if r.get('payload',{}).get('type')=='item_completed']
    types=collections.Counter(p['item']['type'] for _,p in items)
    categories=collections.Counter(e['category'] for c in out for e in c['runtime_events'])
    reasoning=[dict(rollout_line=n,started_at_ms=p.get('started_at_ms'),completed_at_ms=p.get('completed_at_ms')) for n,p in items if p['item']['type']=='Reasoning']
    return dict(schema='astra-runtime-supplement-v1',arm=str(arm),sources={name:digest(arm/name) for name in ['rollout.jsonl','watch.jsonl','run.json']},
                counts=dict(outer_actions=len(calls),assistant_message_records=sum(r.get('payload',{}).get('type')=='message' and r['payload'].get('role')=='assistant' for _,r in rows),
                            completed_item_types=dict(types),runtime_categories=dict(categories)),
                calls=out,reasoning_item_clocks=reasoning,
                caveats=['Assistant messages are not API round trips; API request count is not established here.',
                         'Runtime events mapped only by enclosing request/output ordinal interval; overlapping calls are marked ambiguous.',
                         'Known literal runtime commands get narrow labels; unknown commands remain unclassified.',
                         'FileChange events witness files changed, not behavioral correctness.',
                         'Completed-item clocks are reported as emitted, not attributed to gaps or actual CPU time.'])

if __name__=='__main__':
    arm=Path(sys.argv[1]).resolve(); dest=Path(sys.argv[2]).resolve()
    if not arm.is_relative_to(ROOT) or not dest.is_relative_to(ROOT): raise ValueError('program root only')
    data=build(arm)
    with dest.open('x') as f: json.dump(data,f,indent=2,sort_keys=True); f.write('\n')
