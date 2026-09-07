import re,sys,json
sys.path.insert(0,'/var/tmp/forge/plan2/N2')
from map import MAPPING
ROOT='/home/forge/src/cc-split-N2'
name2ns={n:ns for ns,l in MAPPING.items() for n in l}
alias={ns:'v-'+ns for ns in MAPPING}
files=[ROOT+'/src/cfp_scheduler_killer/server.clj']+[ROOT+'/test/cfp_scheduler_killer/%s.clj'%f for f in ('comms_test','forms_test','polish_test','views_test')]
NAMEPAT=r"[A-Za-z0-9\*\+\!\-\_\?\<\>\=]+"
for path in files:
    s=open(path).read()
    used=set()
    unknown=set()
    def rep(m):
        n=m.group(1)
        if n in name2ns:
            used.add(name2ns[n]); return alias[name2ns[n]]+'/'+n
        unknown.add(n); return m.group(0)
    def repq(m):
        n=m.group(1)
        if n in name2ns:
            used.add(name2ns[n]); return 'cfp-scheduler-killer.views.'+name2ns[n]+'/'+n
        unknown.add(n); return m.group(0)
    s=re.sub(r"(?<![\w.\-])cfp-scheduler-killer\.views/("+NAMEPAT+")",repq,s)
    s=re.sub(r"(?<![\w.\-])views/("+NAMEPAT+")",rep,s)
    # ns require rewrite
    reqs='\n'.join(sorted('   [cfp-scheduler-killer.views.%s :as %s]'%(ns,alias[ns]) for ns in used))
    if '[cfp-scheduler-killer.views :as views]' in s:
        indent=re.search(r'^([ \t]*)\[cfp-scheduler-killer\.views :as views\]',s,re.M).group(1)
        block='\n'.join(indent+'[cfp-scheduler-killer.views.%s :as %s]'%(ns,alias[ns]) for ns in sorted(used))
        s=re.sub(r'^[ \t]*\[cfp-scheduler-killer\.views :as views\]',lambda m: block,s,count=1,flags=re.M)
    elif used:
        # insert after first require entry
        ms=list(re.finditer(r'^([ \t]*)\[cfp-scheduler-killer\.[^\n]*\]$',s,re.M))
        m=ms[-1]
        indent=m.group(1)
        block='\n'.join(indent+'[cfp-scheduler-killer.views.%s :as %s]'%(ns,alias[ns]) for ns in sorted(used))
        s=s[:m.end()]+'\n'+block+s[m.end():]
    open(path,'w').write(s)
    print(path.split('/')[-1],'used:',sorted(used),'| unresolved views/ syms:',sorted(unknown))
