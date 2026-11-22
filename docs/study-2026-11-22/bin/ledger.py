#!/usr/bin/env python3
# Repository experiment ledger: Python stdlib, sequential read-only git commands.
import argparse, collections, csv, hashlib, json, re, subprocess, time
from pathlib import Path
V = ['won and routed','lost and suspended','inconclusive','never measured']
OUTPUTS = ['branches.tsv','siblings.tsv','observations.tsv','ledger.tsv','ledger.md','commits_by_verdict.tsv','METHOD-B.md']
COMMANDS = collections.Counter()
NUM = re.compile(r'\b\d[\d,.]*\s*(?:ms\b|s\b|seconds?\b|minutes?\b|hours?\b|%|[x×])|\b(?:wall|ratio)\b.{0,80}\d|\d.{0,80}\b(?:wall|ratio)\b',re.I)
DATE = re.compile(r'20\d\d-\d\d-\d\d')
WORDS = ['won','lost','suspended','inconclusive','retire','route','admit','refuse','never measured','not measured']

def safe(x):
    s=re.sub(r'/(?:home|Users)/[A-Za-z0-9_.-]+','~',str(x))  # every user's home on any box, not only this seat's
    s=re.sub(r'(?i)\b(?:sk-[a-z0-9_-]{12,}|gh[pousr]_[a-z0-9_]{12,}|github_pat_[a-z0-9_]+|xox[baprs]-[a-z0-9-]+|AKIA[A-Z0-9]{16})','[REDACTED]',s)
    s=re.sub(r'(?i)((?:api[_-]?key|api[_-]?token|access[_-]?token|secret|password|authorization)\s*[=:]\s*)(?:"[^"\n]*"|\S+)',r'\1[REDACTED]',s)
    s=re.sub(r"(?i)((?:[a-z0-9_]*(?:api[_-]?key|api[_-]?token|access[_-]?token|secret|password)|authorization|cookie|token)[\"']?\s*[:=]\s*)(?:\"[^\"\n]*\"|'[^'\n]*'|[^,;\s}\]]+)",r'\1[REDACTED]',s)
    s=re.sub(r'\beyJ[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+\b','[REDACTED]',s)
    s=re.sub(r'(?i)bearer\s+[a-z0-9._~+/-]+=*','Bearer [REDACTED]',s)
    s=re.sub(r'(?i)(https?://)[^/@\s]+:[^/@\s]+@',r'\1[REDACTED]@',s)
    return s.replace('\t',' ').replace('\r',' ').replace('\n',' ').replace('\x00','').strip()

def git(repo,*args,ok=True,raw=False,data=None):
    COMMANDS[args[0]]+=1
    r=subprocess.run(['git','--no-optional-locks','-C',str(repo),*args],input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=not raw)
    if ok and r.returncode: raise RuntimeError(safe(r.stderr.decode(errors='replace') if raw else r.stderr))
    return r.stdout if ok else r

def base(repo):
    for b in ['main','origin/main','master','origin/master']:
        if git(repo,'rev-parse','--verify',b,ok=False).returncode==0: return b
    raise RuntimeError('No default branch')

def prefix(b):
    b=re.sub(r'^refs/(heads|remotes)/','',b)
    b=re.sub(r'^origin/','',b)
    return (b.split('/',1)[0] if '/' in b else b.split('-',1)[0]) or 'unassigned'

def topic(p):
    parts=[Path(p).stem]+list(reversed(Path(p).parts[:-1]))
    for part in parts:
        m=DATE.search(part)
        if m: return part[m.end():].strip('-_') or part
    return Path(p).stem

def tokens(s):
    return set(re.findall('[a-z0-9]+',s.lower()))-set('result results report captains log cohort study hypothesis preregistration md the and of a docs observations'.split())

def anchor(s):
    s=s.lower()
    for key,terms in [
        ('strictly-better-evidence',['strictly-better']),
        ('namespace-split',['namespace-split','plan-2-cellc']),
        ('alias-migration',['alias-migration','astra-alias-replication']),
        ('fan-out',['fanout','fan-out','pair-1']),
        ('per-form-writes',['per-form','per_form']),
        ('direct-cclsp-clients',['direct-cclsp']),
        ('bridge-wall-clock-ideal-program',['captains-log-bridge-wall-clock-ideal-program'])]:
        if any(t in s for t in terms): return key
    return None

ANCHORS={
'namespace-split':('2026-09-07-plan-2-cellC-result.md',r'D7 verb CLI, landed','won and routed',r'namespace.split'),
'alias-migration':('2026-09-06-strictly-better-evidence.md',r'whole-repository alias migration.*42\.9','won and routed',r'alias.migration'),
'fan-out':('2026-09-07-pair-1-result.md',r'0\.68x','lost and suspended',r'fan.out'),
'per-form-writes':('2026-09-02-captains-log-bridge-wall-clock-ideal-program.md',r'the tool over native work at about 2x wall','lost and suspended',r'per.form'),
'direct-cclsp-clients':('2026-08-27-captains-log-direct-cclsp-clients-retired.md',r'309.*566','lost and suspended',r'direct.*cclsp'),
'strictly-better-evidence':('2026-09-06-strictly-better-evidence.md',r'whole-repository alias migration.*42\.9','inconclusive',None),
'bridge-wall-clock-ideal-program':('2026-09-02-captains-log-bridge-wall-clock-ideal-program.md',r'87\s*%|87 percent','lost and suspended',r'per.form')}

def history(repo):
    scopes=['docs/observations','docs/experiments','docs/intent','docs/vision.md','docs/tech-tree.md','AGENTS.md','CLAUDE.md','skill.md','logs','records']
    raw=git(repo,'log','--all','--date-order','--no-renames','--root','--format=@@%H %ad','--date=short','--raw','--no-abbrev','--diff-filter=AM','--',*scopes)
    files={}; commit=date=''
    for l in raw.splitlines():
        if l.startswith('@@'): commit,date=l[2:].split(' ',1)
        elif l.startswith(':') and '\t' in l:
            meta,p=l.split('\t',1)
            if p.startswith('"'):
                try: p=json.loads(p)
                except ValueError: pass
            fields=meta.split()
            f=files.setdefault(p,{'blob':fields[3],'commit':commit,'first':date,'last':date})
            f['first']=min(f['first'],date); f['last']=max(f['last'],date)
            if fields[4]=='A': f['added']=min(f.get('added',date),date)
    wanted={p:f for p,f in files.items() if p.startswith(('docs/observations/','docs/experiments/','docs/intent/')) or p in ['AGENTS.md','CLAUDE.md','skill.md','docs/vision.md','docs/tech-tree.md']}
    ids=list(dict.fromkeys(f['blob'] for f in wanted.values()))
    data=git(repo,'cat-file','--batch',raw=True,data=('\n'.join(ids)+'\n').encode())
    blobs={}; pos=0
    for oid in ids:
        end=data.index(b'\n',pos); size=int(data[pos:end].split()[2]); body=data[end+1:end+1+size]
        blobs[oid]='' if b'\x00' in body else body.decode('utf-8',errors='replace')
        pos=end+size+2
    for p,f in wanted.items(): f['text']=blobs[f['blob']]
    return files,wanted

def main():
    start=time.monotonic()
    ap=argparse.ArgumentParser()
    ap.add_argument('--surgeon',type=Path,default=Path.home()/'src/clj-surgeon')
    ap.add_argument('--records',type=Path,default=Path.home()/'src/clj-surgeon-records')
    ap.add_argument('--out',type=Path,default=Path('docs/study-2026-11-22'))
    a=ap.parse_args(); a.out.mkdir(parents=True,exist_ok=True)
    protected={p:hashlib.sha256(p.read_bytes()).hexdigest() for p in a.out.rglob('*') if p.is_file() and p.relative_to(a.out).as_posix() not in OUTPUTS and p.resolve()!=Path(__file__).resolve()}
    graph={}; dates={}
    for l in git(a.surgeon,'log','--all','--format=%H%x09%ad%x09%P','--date=iso-strict').splitlines():
        oid,date,parents=l.split('\t'); graph[oid]=parents.split(); dates[oid]=date
    def reach(oid):
        seen=set(); todo=[oid]
        while todo:
            c=todo.pop()
            if c not in seen: seen.add(c); todo.extend(graph.get(c,[]))
        return seen
    mainset=reach(git(a.surgeon,'rev-parse',base(a.surgeon)).strip())
    branches=[]; memberships={}; excluded=[]
    refs=git(a.surgeon,'for-each-ref','--format=%(refname)%09%(objectname)%09%(symref)%09%(subject)','refs/heads','refs/remotes')
    for l in refs.splitlines():
        ref,oid,sym,subj=l.split('\t',3)
        if sym: excluded.append(ref); continue
        cs=reach(oid); memberships[ref]=cs-mainset
        branches.append(dict(branch=ref,raw_prefix=prefix(ref),family='',first_commit_date=min(dates[c] for c in cs),last_commit_date=dates[oid],commits_not_on_main=len(cs-mainset),merged_into_main='yes' if oid in mainset else 'no',last_subject=subj))
    freq=collections.Counter(b['raw_prefix'] for b in branches)
    for b in branches: b['family']=b['raw_prefix'] if freq[b['raw_prefix']]>=3 and b['raw_prefix']!='unassigned' else 'misc'
    siblings=[]; sibling_sets={}; outside=[]
    for p in sorted(a.surgeon.parent.glob('clj-surgeon-*')):
        if not p.is_dir() or git(p,'rev-parse','--show-toplevel',ok=False).returncode: continue
        oid=git(p,'rev-parse','HEAD').strip()
        br=git(p,'symbolic-ref','--quiet','--short','HEAD',ok=False)
        br=br.stdout.strip() if br.returncode==0 else '(detached)'
        ahead=int(git(p,'rev-list','--count',base(p)+'..HEAD'))
        entries=git(p,'status','--porcelain=v1','-z','--untracked-files=all').split('\x00'); i=dirty=0
        while i<len(entries):
            e=entries[i]; i+=1
            if e:
                dirty+=1
                if e[0] in 'RC' or e[1] in 'RC': i+=1
        raw=prefix(br) if br!='(detached)' else 'unassigned'
        siblings.append(dict(path=str(p),branch=br,head=oid,commits_ahead_of_main=ahead,dirty_files=dirty,last_commit_date=git(p,'show','-s','--format=%cI','HEAD').strip(),likely_family=raw if freq[raw]>=3 else 'misc',raw_prefix=raw))
        sibling_sets[str(p)]=reach(oid)-mainset if oid in graph else set()
        if oid not in graph: outside.append(str(p))
    obs=[]; corpus=[]; index={}; snapshots=[]
    for label,repo in [('surgeon',a.surgeon),('records',a.records)]:
        allfiles,files=history(repo)
        current=git(repo,'ls-tree','-r','--name-only','HEAD','docs/observations').splitlines()
        snapshots.append((label,git(repo,'rev-parse','HEAD').strip(),len(current),sum(p.startswith('docs/observations/') for p in allfiles),sum(p.endswith('.md') for p in current)))
        for p,f in sorted(files.items()):
            ev=dict(repo=label,file=p,**f); corpus.append(ev); index[label,p]=ev
            if not p.startswith('docs/observations/'): continue
            text=f['text']; lines=text.splitlines(); m=DATE.search(p)
            obs.append(dict(date=m.group() if m else f.get('added',f['first']),repo=label,file=p,series=topic(p),headline=next((l.lstrip('# ').strip() for l in lines if l.startswith('#')),''),first_numeric_claim=next((l.strip() for l in lines if NUM.search(l)),''),verdict_words=', '.join(w for w in WORDS if re.search(r'\b'+re.escape(w)+r'\w*\b',text,re.I)),blob=f['blob'],source_commit=f['commit']))
    groups={}
    def group(k,kind):
        return groups.setdefault(k,dict(name=k,kind=kind,dates=[],members=[],evidence=[],commits=set(),branch_sum=0))
    for o in obs:
        stem=re.sub(r'-(?:result|results|report|preregistration|hypothesis)$','',o['series'].lower())
        k=anchor(o['series']) or stem+'@'+o['date']
        g=group(k,'series'); g['dates'].append(o['date']); g['members'].append(o['repo']+':'+o['file']); g['evidence'].append(index[o['repo'],o['file']])
    # A single captain's log contains two distinct requested experiments:
    # clock composition and the per-form editing comparison.
    pf=group('per-form-writes','series')
    for f in corpus:
        if Path(f['file']).name=='2026-09-02-captains-log-bridge-wall-clock-ideal-program.md':
            pf['evidence'].append(f); pf['members'].append(f['repo']+':'+f['file']+' (per-form comparison facet)'); pf['dates'].append('2026-09-02')
    for raw in sorted(freq):
        k=anchor(raw) or 'family:'+raw; g=group(k,'family'); g['kind']='family'
        for b in branches:
            if b['raw_prefix']!=raw: continue
            g['dates'] += [b['first_commit_date'][:10],b['last_commit_date'][:10]]
            g['members'].append(b['branch']); g['branch_sum']+=b['commits_not_on_main']; g['commits'].update(memberships[b['branch']])
        ts=tokens(raw)
        if len(ts)>=2:
            for f in corpus:
                if ts<=tokens(topic(f['file'])) and f not in g['evidence']: g['evidence'].append(f)
    for s in siblings:
        raw=s['raw_prefix']; k=anchor(s['branch']) or anchor(Path(s['path']).name) or 'sibling:'+Path(s['path']).name
        g=group(k,'sibling'); g['members'].append(s['path']+' [branch '+s['branch']+']'); g['dates'].append(s['last_commit_date'][:10]); g['commits'].update(sibling_sets[s['path']])
        st=tokens(s['branch'].split('/',1)[-1]) if s['branch']!='(detached)' else set()
        if len(st)>=2:
            for f in corpus:
                if st<=tokens(topic(f['file'])) and f not in g['evidence']: g['evidence'].append(f)
    preregs=[f for f in corpus if f['file'].startswith('docs/experiments/')]
    doctrine=[f for f in corpus if f['file'] in ['AGENTS.md','CLAUDE.md','skill.md','docs/vision.md','docs/tech-tree.md'] or ('routing' in f['file'] and f['file'].startswith('docs/intent/'))]
    def cite(f): return f['repo']+':'+f['file']+'@'+f['commit'][:12]
    ledger=[]; checks=[]
    for k,g in sorted(groups.items()):
        ev=sorted(g['evidence'],key=lambda f:(not f['file'].endswith('.md'),f['first'],f['repo'],f['file']))
        ts=tokens(k.split('@')[0].replace('family:',''))
        pres=[f for f in preregs if len(ts)>=2 and ts<=tokens(topic(f['file']))]
        hyp=''
        for f in pres+ev:
            lines=f['text'].splitlines()
            for i,l in enumerate(lines):
                if re.search(r'\b(hypothesis|we predict|prediction|we expect|claim to test)\b',l,re.I):
                    h=l.strip()
                    if len(h)<30: h=next((x.strip() for x in lines[i+1:i+7] if x.strip() and not x.startswith('#')),h)
                    hyp=h+' ['+cite(f)+']'; break
            if hyp: break
        if not hyp and ev:
            f=ev[0]; h=next((l.strip() for l in f['text'].splitlines() if l.strip() and not l.startswith(('#','|','```','>'))),'')
            if h: hyp=h+' ['+cite(f)+'; first-observation statement, hypothesis not explicit]'
        if not hyp: hyp='Unknown: no unambiguous preregistration or first observation matched.'
        receipts=[]; decisions=[]
        for f in ev:
            lines=f['text'].splitlines()
            n=next((l.strip() for l in lines if NUM.search(l)),'')
            if n: receipts.append((f,n))
            for l in lines:
                if re.search(r'(?i)(?:^#{1,4}.*(?:verdict|decision)|^(?:verdict|decision|disposition)\s*[:—-])',re.sub(r'[*_`]', '', l.strip())): decisions.append((f,l.strip()))
        verdict='inconclusive' if receipts else 'never measured'
        result='; '.join(l+' ['+cite(f)+']' for f,l in receipts[:2]); decision=''
        loss=next(((f,l) for f,l in decisions if re.search(r'\b(?:lost|loss|suspended|retired|falsified|native faster|native wins)\b',l,re.I) and not re.search(r'\b(?:not|never)\s+(?:lost|suspended|retired|falsified)',l,re.I)),None)
        if loss: verdict='lost and suspended'; decision=loss[1]+' ['+cite(loss[0])+']'
        if k in ANCHORS:
            fname,pat,want,routepat=ANCHORS[k]
            fs=sorted([f for f in corpus if Path(f['file']).name==fname],key=lambda f:f['repo']!='records')
            f=fs[0] if fs else None
            line=next((l.strip() for l in f['text'].splitlines() if re.search(pat,l,re.I)),'') if f else ''
            routes=[]
            if routepat:
                for d in doctrine:
                    for l in d['text'].splitlines():
                        if re.search(routepat,l,re.I): routes.append((d,l.strip()))
            if line and (want!='won and routed' or routes):
                verdict=want; result=line+' ['+cite(f)+']'
                flines=f['text'].splitlines()
                # Preserve enough neighboring text to make wrapped claims legible.
                idx=next(i for i,l in enumerate(flines) if l.strip()==line)
                if k in ['bridge-wall-clock-ideal-program','direct-cclsp-clients','per-form-writes']:
                    result=' '.join(l.strip() for l in flines[max(0,idx-1):idx+3] if l.strip())+' ['+cite(f)+']'
                if k=='namespace-split':
                    native=next((l.strip() for l in flines if l.startswith('Same map_hash') and '447 s' in l),'')
                    if native: result+='; '+native+' ['+cite(f)+']'
                if k=='bridge-wall-clock-ideal-program':
                    tooltime=next((l.strip() for l in flines if '3–4%' in l or '3-4%' in l),'')
                    if tooltime: result+='; '+tooltime+' ['+cite(f)+']'
                if k=='fan-out': g['dates'].append('2026-09-08')
                # The primary observation's opening statement is more defensible
                # than a later sentence merely containing the word prediction.
                paragraphs=re.split(r'\n\s*\n',f['text'])
                candidate=next((q for q in paragraphs if q.strip() and not q.lstrip().startswith(('#','|','```'))),'')
                if candidate:
                    hyp=' '.join(candidate.split())+' ['+cite(f)+'; first-observation statement, hypothesis not explicit]'
                if k=='fan-out':
                    second=next((l.strip() for l in f['text'].splitlines() if '0.59x' in l),'')
                    if second: result+='; '+second+' ['+cite(f)+']'
                decision=''
                if k=='direct-cclsp-clients': decision='Surface/security retirement; scan counts are not a performance comparison. '
                if routes:
                    routes.sort(key=lambda x:(x[0]['file']!='skill.md',not bool(re.search('routed|suspend|retir|lost|2x|2×|performance',x[1],re.I))))
                    d,l=routes[0]; decision+=l+' ['+cite(d)+']'
                else: decision+=next((l.strip() for l in f['text'].splitlines() if re.search('verdict|retired|suspend|falsified',l,re.I)),'')+' ['+cite(f)+']'
                if k=='fan-out' and '2026-09-08' not in decision: decision='Suspended 2026-09-08. '+decision
                checks.append(k+': receipt '+('and doctrine verified' if routes else 'verified'))
            else: checks.append(k+': INCOMPLETE anchor witness')
        g['verdict']=verdict
        ledger.append(dict(name=k,kind=g['kind'],dates=min(g['dates'])+'..'+max(g['dates']),commits=g['branch_sum'] if g['kind']=='family' else len(g['commits']),hypothesis=hyp,result=result or 'No numeric receipt found in matched evidence.',verdict=verdict,decision_evidence=decision,unique_non_main_commits=len(g['commits']),members=' ; '.join(g['members']),merge_rule='Explicit anchor program; cross-date receipts retained, scope is defined by cited receipt.' if k in ANCHORS else 'Same normalized topic and observation date across repos; separate family/sibling identity unless explicit anchor topic match.'))
    assert len(checks)==len(ANCHORS) and not any('INCOMPLETE' in c for c in checks), 'Incomplete known-anchor evidence: '+str(checks)
    classes=collections.defaultdict(set)
    for g in groups.values():
        for c in g['commits']: classes[c].add(g['verdict'])
    buckets=collections.Counter(); ambiguous=0
    for c in graph:
        if c in mainset: buckets['on main']+=1
        elif len(classes[c])==1: buckets[next(iter(classes[c]))]+=1
        else:
            buckets['unassigned']+=1
            if len(classes[c])>1: ambiguous+=1
    assert sum(buckets.values())==len(graph)
    bf=collections.Counter(c for cs in memberships.values() for c in cs)
    cv=[]
    for v in V+['on main','unassigned']:
        gs=[g for g in groups.values() if g['verdict']==v and g['commits']]
        cv.append(dict(verdict=v,unique_commits_exclusive=buckets[v],raw_family_branch_commit_sum=sum(g['branch_sum'] for g in gs),family_sibling_union_nonexclusive=len(set().union(*(g['commits'] for g in gs))) if gs else (len(mainset) if v=='on main' else 0),note='Exclusive partition; cross-verdict overlap -> unassigned; main precedence. Other columns nonadditive.'))
    def tsv(name,rows):
        with (a.out/name).open('w',newline='') as f:
            w=csv.DictWriter(f,list(rows[0]),delimiter='\t',lineterminator='\n'); w.writeheader()
            for row in rows: w.writerow({k:safe(v) for k,v in row.items()})
    for name,rows in [('branches.tsv',branches),('siblings.tsv',siblings),('observations.tsv',obs),('ledger.tsv',ledger),('commits_by_verdict.tsv',cv)]: tsv(name,rows)
    def md(x): return safe(x).replace('|',r'\|')
    with (a.out/'ledger.md').open('w') as f:
        f.write('# Experiment ledger\n\nFull membership and exact source commits are in ledger.tsv. Numeric candidates may be incidental, planned, or historical. Inconclusive does not certify a benchmark.\n\n')
        cols=['name','kind','dates','commits','hypothesis','result','verdict','decision_evidence','merge_rule']
        f.write('| '+' | '.join(cols)+' |\n| '+' | '.join('---' for _ in cols)+' |\n')
        for row in ledger: f.write('| '+' | '.join(md(row[k]) for k in cols)+' |\n')
    counts=dict(reachable_commits=len(graph),branches=len(branches),refs_including_symbolic=len(refs.splitlines()),symbolic_refs_excluded=len(excluded),named_families=sum(n>=3 and p!='unassigned' for p,n in freq.items()),raw_prefixes=len(freq),unassigned_to_named_family=sum(b['family']=='misc' for b in branches),siblings=len(siblings),sibling_heads_outside_commit_universe=len(outside),ledger_rows=len(ledger),non_main_commits_on_multiple_branches=sum(n>1 for n in bf.values()),excess_branch_commit_memberships=sum(n-1 for n in bf.values()),cross_verdict_ambiguous_commits=ambiguous)
    for label,head,cur,hist,mdcount in snapshots: counts[label+'_observation_files_HEAD']=cur; counts[label+'_observation_files_all_history']=hist; counts[label+'_observation_markdown_HEAD']=mdcount
    for v in V: counts['rows_'+v]=sum(r['verdict']==v for r in ledger)
    for v in V+['on main','unassigned']: counts['commits_'+v]=buckets[v]
    elapsed=time.monotonic()-start
    method='''# Part B method

Run: `python3 docs/study-2026-11-22/bin/ledger.py --surgeon ~/src/clj-surgeon --records ~/src/clj-surgeon-records --out docs/study-2026-11-22`

Python standard library and sequential Git read commands only; no concurrent subprocesses. No source repository writes, checkout, reset, fetch, or commit. Writes are the seven Part B deliverables; other output files are hashed before and after. Python CSV consumers should set csv.field_size_limit(100000000): exhaustive members fields can exceed the default 128 KiB. Documents are data, never executed. Home paths and recognized credentials are redacted before output.

## Snapshot and counts

'''
    for label,head,cur,hist,mdcount in snapshots: method+=f'- {label} HEAD `{head}`: {cur} observation files at HEAD; {hist} distinct all-history observation paths; {mdcount} Markdown files at HEAD.\n'
    method+='\n'+'\n'.join(f'- {k}: {v}' for k,v in counts.items())+'\n'
    method+=f'- Runtime: {elapsed:.3f} seconds (script through table generation; final method write/hash check excluded).\n'
    method+='''
## Commands and family rules

Every Git command uses `git --no-optional-locks -C REPO`. Read commands: `rev-parse --verify DEFAULT`/`rev-parse HEAD`; `log --all --format=%H%x09%ad%x09%P --date=iso-strict`; `for-each-ref --format=... refs/heads refs/remotes`; `symbolic-ref --quiet --short HEAD`; `rev-list --count main..HEAD`; `status --porcelain=v1 -z --untracked-files=all`; `show -s --format=%cI HEAD`; `ls-tree -r --name-only HEAD docs/observations`; `log --all --date-order --no-renames --root --format=@@%H %ad --date=short --raw --no-abbrev --diff-filter=AM -- SCOPES`; `cat-file --batch`. SCOPES: docs/observations, docs/experiments, docs/intent, docs/vision.md, docs/tech-tree.md, AGENTS.md, CLAUDE.md, skill.md, logs, records. Logs/records history metadata is inventoried; bodies are not used to classify verdicts.

Symbolic remote HEAD aliases excluded; otherwise retain both local and remote refs. Strip refs/heads or refs/remotes then origin/; raw prefix is before first slash or hyphen. Prefixes with fewer than three branches become misc; raw_prefix retained. Unassigned-to-named-family includes misc. Prefix before slash takes precedence over splitting a slash-free name on hyphen. First commit date is minimum committer timestamp in whole reachable ancestry, not branch creation; last date is tip committer timestamp. Ancestry closures reproduce rev-list main..branch and merge-base --is-ancestor; default main, else origin/main, master, origin/master. Siblings are actual Git directory matches clj-surgeon-*, including records. Dirty count includes individual untracked files; rename pairs count once.

## Observations and merge rules

Every observation path ever added/modified is retained, including fixtures and historical deletions. Representative text is the first AM blob in all-ref date-order traversal, with exact commit and blob identifiers; alternate variants are not combined. Filename date (else dated parent) takes precedence. Undated paths use earliest reachable addition date from the batched history (equivalent to per-path git log --all --diff-filter=A --format=%ad --date=short). The first leading # line is headline, even for scripts. Numeric extraction takes first line with numeric x/×/s/ms/seconds/minutes/hours/% or a number near wall/ratio. It does not establish that a numeric mention is an actual measurement. Verdict words are lexical indicators only. Series is the dated stem's topic, else dated parent directory topic, else basename.

Ledger merge: exact normalized topic (trailing result/report/preregistration/hypothesis removed) plus same calendar date merges across repositories. The captain's log supplies two separately labeled facets (clock composition and per-form comparison), since these are distinct requested anchors. Seven explicit anchor programs additionally combine clearly named cross-date receipts: namespace-split/plan-2-cellC, alias-migration/astra-alias-replication, fanout/fan-out/pair-1, per-form-writes, direct-cclsp-clients, strictly-better-evidence, bridge-wall-clock-ideal-program. Individual subcohorts remain in members/receipts. Differently named/different-date series otherwise remain distinct. Sibling copies remain separate experiments unless their full branch topic or directory name identifies an explicit anchor program; a common seat prefix is insufficient to merge a sibling into an aggregate family. Misc raw prefixes remain separate experiments. Family evidence linkage requires at least two topic tokens (except explicit anchors); broad bridge/cohort/arm families remain unattributed. Evidence-only links do not consume the original series row. Branch/sibling memberships can overlap; commit reconciliation below exposes this ambiguity. members and merge_rule expose every merge.

Hypotheses prefer topic-matched preregistrations, then earliest matching observation; take verbatim hypothesis/prediction/expectation line, or next paragraph if heading only. Otherwise label the first observation statement as nonexplicit hypothesis. No source means Unknown. Results keep first numeric candidate per observation, at most two receipts per row, untruncated; exact repo:path@commit provenance retained. This can include planned or incidental numbers, explicitly marked as a limitation. Source text is not treated as authoritative instructions.

## Verdict gates and known anchors

No numeric candidate -> never measured; otherwise inconclusive. These describe matched evidence availability, not proof of actual experimental fate. A direct Verdict/Decision/Disposition line saying lost/suspended/retired/falsified can yield lost and suspended. Winner promotion is conservative: only reviewed anchor receipt plus routing/doctrine witness yields won and routed. Doctrine sources are representative reachable AGENTS.md, CLAUDE.md, skill.md, docs/vision.md, docs/tech-tree.md, routing intent files, cited at exact commits. This does not prove installed-seat parity. cclsp retirement is a surface/security decision, not a performance loss: scan counts are labeled accordingly. Captain's log performance loss is distinct from its model/tool time composition.

'''
    method+='\n'.join('- '+c for c in checks)+'\n'
    method+='''
## Commit reconciliation and limits

Universe: every commit reachable by --all in surgeon, counted once. Main ancestry has precedence. For each remaining commit, collect verdict classes of containing family/sibling rows. One class -> that class; multiple classes or uncovered -> unassigned. unique_commits_exclusive sums to the universe. Branch sums and family_sibling_union_nonexclusive deliberately overlap and are nonadditive. The number of distinct non-main commits on more than one branch and extra branch memberships are reported separately. Sibling HEAD outside the universe has unknown attribution and is not fabricated into the total. Ledger commits sum main..branch per ref for family rows, use the sibling union for sibling/anchor rows, and are zero for unattached observation series. Family commits sum main..branch per ref; sibling histories are not added to raw family sums. Branch containment cannot show which experiment authored a shared ancestor.

Brief counts are reference expectations, not forced outputs: actual trees include nested fixture files, and all-history counts can be larger. Git has no durable branch creation timestamps. Broad topic attribution, overlapping experiments, nonnumeric receipts, alternate historical variants, and installed routing state can remain unknown. Numeric candidates may be plans, baselines, or incidental counts: inspect source before using a row as a benchmark claim. No semantic full-text model validates every receipt. This ledger is an auditable index, not independent acceptance of historical claims.

'''
    method+='Git invocation counts: '+', '.join(f'{k}={v}' for k,v in sorted(COMMANDS.items()))+'.\n'
    method+='Files written: '+', '.join(OUTPUTS)+'. Script: bin/ledger.py.\n'
    (a.out/'METHOD-B.md').write_text(method)
    assert all(p.exists() and hashlib.sha256(p.read_bytes()).hexdigest()==h for p,h in protected.items()),'Part A file changed'
    print('METHOD-B counts')
    for k,v in counts.items(): print(f'{k}\t{v}')
    print(f'runtime_seconds\t{elapsed:.3f}\nFiles written:')
    for n in OUTPUTS: print(safe(a.out/n))
    print(safe(Path(__file__)))

if __name__=='__main__': main()
