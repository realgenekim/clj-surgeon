import sys,os,json
sys.path.insert(0,'/var/tmp/forge/plan2/T1')
from map import MAP
ns=sys.argv[1]; target=sys.argv[2] if len(sys.argv)>2 else None
root='/home/forge/src/cc-split-T1'
files=[]
for dp,dns,fns in os.walk(root):
    dns[:]=[d for d in dns if d not in ('.git',)]
    for fn in fns:
        if fn.endswith(('.clj','.cljc','.cljs')):
            p=os.path.relpath(os.path.join(dp,fn),root)
            if p in ('src/cfp_scheduler_killer/views.clj',target): continue
            files.append(p)
names=MAP[ns]
out=sorted({p for p in files for n in names if n in open(root+'/'+p,errors='replace').read()})
print(json.dumps(out))
