import json,re,sys
sys.path.insert(0,'/var/tmp/forge/plan2/N2')
from map import MAPPING
forms=json.load(open('/var/tmp/forge/plan2/N2/forms.json'))
usage=json.load(open('/var/tmp/forge/plan2/N2/usage.json'))
name2ns={n:ns for ns,l in MAPPING.items() for n in l}
order={f['name']:i for i,f in enumerate(forms) if f['name']}
for f in forms:
    n=f['name']
    if not n or n not in name2ns: continue
    for r in usage.get(n,[]):
        if name2ns[r]==name2ns[n] and order[r]>order[n]:
            print("FWD",name2ns[n],n,"->",r)
