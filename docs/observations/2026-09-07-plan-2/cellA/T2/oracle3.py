import re,os,sys,collections
PLAN='/var/tmp/forge/plan2/split/plan.md'
ROOT='/home/forge/src/cc-split-T2/src/cfp_scheduler_killer/views/'
# parse plan: "## cfp-scheduler-killer.views.X (path)" then "- name"
cur=None; expected=[]
for line in open(PLAN):
    m=re.match(r'## cfp-scheduler-killer\.views\.[a-z-]+ \((src/cfp_scheduler_killer/views/[a-z_]+\.clj)\)',line)
    if m: cur=os.path.basename(m.group(1)); continue
    if line.startswith('## '): cur=None; continue
    m=re.match(r'- (\S+)\s*$',line)
    if m and cur:
        n=m.group(1)
        if n=='^:private': continue
        expected.append((n,cur))
# plan lost the * on row-controls*; the second 'row-controls' under review is row-controls*
seen=collections.Counter()
fixed=[]
for n,f in expected:
    seen[(n,f)]+=1
    if seen[(n,f)]==2 and n=='row-controls': n='row-controls*'
    fixed.append((n,f))
expected=fixed
# index definitions under views/
defs=collections.defaultdict(list)
for fn in sorted(os.listdir(ROOT)):
    if not fn.endswith('.clj'): continue
    for i,l in enumerate(open(ROOT+fn),1):
        m=re.match(r'\((def[a-z-]*)\s+(?:\^:private\s+)?([^\s\)\]]+)',l)
        if m and m.group(1)!='declare': defs[m.group(2)].append((fn,i))
bad=[]
for n,f in expected:
    locs=defs.get(n,[])
    if len(locs)!=1 or locs[0][0]!=f:
        bad.append((n,f,locs))
print("plan form names checked:",len(expected))
print("MISMATCHES:",len(bad))
for b in bad: print("  ",b)
