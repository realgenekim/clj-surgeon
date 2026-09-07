import sys, re, collections
sys.path.insert(0,'/var/tmp/forge/plan2/N1')
from map import OWNER, NS
ALIAS = {'auth':'v-auth','avatar':'avatar','committee':'committee','communications':'comms',
 'dashboard':'dashboard','event-setup':'event-setup','format':'fmt','form-builder':'form-builder',
 'form-controls':'controls','integrations':'integrations','live-drafts':'drafts','log':'v-log',
 'organizer-layout':'layout','people':'people','portal':'v-portal','public-cfp':'public-cfp',
 'replay':'v-replay','review':'review','schedule':'v-schedule','shell':'shell'}
ROOT='/home/forge/src/cc-split-N1/'
import os
FILES=os.environ['CALLER_FILES'].split(',')
SYMCH=r"[A-Za-z0-9!?*<>=+_'.&%-]"
for rel in FILES:
    p=ROOT+rel
    t=open(p).read()
    used=set()
    def sub_alias(m):
        n=m.group(1)
        if n not in OWNER:
            raise SystemExit('unknown view var %s in %s' % (n, rel))
        used.add(OWNER[n]); return ALIAS[OWNER[n]]+'/'+n
    def sub_full(m):
        n=m.group(1)
        if n not in OWNER: raise SystemExit('unknown '+n)
        return NS[OWNER[n]]+'/'+n
    t=re.sub(r'\bcfp-scheduler-killer\.views/('+SYMCH+r'+)', sub_full, t)
    t=re.sub(r'(?<![\w.-])views/('+SYMCH+r'+)', sub_alias, t)
    # require line
    m=re.search(r'^([ \t]*)\[cfp-scheduler-killer\.views :as views\]', t, re.M)
    if m:
        indent=m.group(1)
        reqs=('\n'+indent).join('[%s :as %s]' % (NS[d], ALIAS[d]) for d in sorted(used))
        t=t[:m.start()]+indent+reqs+t[m.end():]
    elif used:
        raise SystemExit('no require line but aliases used in '+rel)
    open(p,'w').write(t)
    print(rel, 'uses:', sorted(used))
