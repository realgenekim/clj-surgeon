import json,re,sys,os
sys.path.insert(0,'/var/tmp/forge/plan2/N2')
from map import MAPPING, FOUNDATION, PUBLIC
ROOT='/home/forge/src/cc-split-N2'
SRC=ROOT+'/src/cfp_scheduler_killer/views.clj'
lines=open(SRC).read().split('\n')
forms=json.load(open('/var/tmp/forge/plan2/N2/forms.json'))
usage=json.load(open('/var/tmp/forge/plan2/N2/usage.json'))
name2ns={n:ns for ns,l in MAPPING.items() for n in l}
form_by_name={f['name']:f for f in forms if f['name']}

DECL={210:'organizer-layout',1304:'review'}  # 1-based start lines of declares to keep
DROP={3059}

DOCS={
 'auth':'Public entry surfaces — the landing page and sign-in.',
 'avatar':'Deterministic speaker/organizer avatars: initials and the face pool.',
 'committee':'Committee roster: cards, member rows, and the committee page.',
 'communications':'Organizer communications — the comms log page and the inform composer.',
 'dashboard':'The event dashboard: readiness checklist, activity feed, and its regions.',
 'event-setup':'Creating and configuring an event — the list, marquee, slug check, and detail pages.',
 'format':'Formatting foundations: dates, instants, relative time, and blank handling.',
 'form-builder':'The CFP form builder — field grid, edit panel, preview, and the builder page.',
 'form-controls':'Form-control foundations shared by every form surface: inputs, errors, required marks.',
 'integrations':'Outward-facing plumbing: exports, API docs, Slack, and the settings page.',
 'live-drafts':'The speaker-facing live lane — draft status and notes, shared by portal and public CFP.',
 'log':'The event log — summary, region, and page.',
 'organizer-layout':'Organizer chrome: sidebar, header, breadcrumb, shell, and the time-travel bar.',
 'people':'Person pages and profile links.',
 'portal':'The speaker portal — tasks, submissions, and the edit/profile forms.',
 'public-cfp':'The public call-for-papers surface, its markdown rendering, and the success page.',
 'replay':'Replay controls — the progress bar and the replay page.',
 'review':'Review and triage: the board, submission detail, capture, stars, and notices.',
 'schedule':'The schedule grid, agenda, and their placement controls.',
 'shell':'Page shell foundations: doctype, assets, cache-busting, and the Datastar script tag.',
}

def clean(text):
    out=[];instr=False;esc=False;i=0
    while i<len(text):
        c=text[i]
        if instr:
            if esc: esc=False
            elif c=='\\': esc=True
            elif c=='"': instr=False
            out.append(' ')
        else:
            if c=='"': instr=True; out.append(' ')
            elif c==';':
                while i<len(text) and text[i]!='\n': i+=1
                out.append('\n'); continue
            else: out.append(c)
        i+=1
    return ''.join(out)

# assign forms to namespaces, in source order, with preceding comment block
assign={ns:[] for ns in MAPPING}
prev_end=24  # end of ns form
for f in forms:
    if f['start']==1: continue
    ns=None
    if f['name'] in name2ns: ns=name2ns[f['name']]
    elif f['start'] in DECL: ns=DECL[f['start']]
    elif f['start'] in DROP:
        prev_end=f['end']; continue
    else:
        raise SystemExit("unassigned form at %d: %s"%(f['start'],f['first']))
    pre=lines[prev_end:f['start']-1]
    while pre and pre[0].strip()=='': pre.pop(0)
    while pre and pre[-1].strip()=='': pre.pop()
    body=lines[f['start']-1:f['end']]
    assign[ns].append(dict(name=f['name'],pre=pre,body=body,private=f['first'].startswith('(defn-') or f['private']))
    prev_end=f['end']

# which private names must become public
needs_public=set()
for name,refs in usage.items():
    ns=name2ns[name]
    for r in refs:
        if name2ns[r]!=ns: needs_public.add(r)
priv_public=set()
for ns,items in assign.items():
    for it in items:
        if it['name'] in needs_public and it['private']:
            priv_public.add(it['name'])
            b=it['body']
            b[0]=re.sub(r'^\(defn-\s',  '(defn ', b[0])
            b[0]=re.sub(r'^\(def\s+\^:private\s+', '(def ', b[0])
            it['private']=False

EXT_ALIASES={'committees':'[cfp-scheduler-killer.committees :as committees]',
 'events':'[cfp-scheduler-killer.events :as events]',
 'exports':'[cfp-scheduler-killer.exports :as exports]',
 'forms':'[cfp-scheduler-killer.forms :as forms]',
 'portal':'[cfp-scheduler-killer.portal :as portal]',
 'reviews':'[cfp-scheduler-killer.reviews :as reviews]',
 'schedule':'[cfp-scheduler-killer.schedule :as schedule]',
 'submissions':'[cfp-scheduler-killer.submissions :as submissions]',
 'json':'[clojure.data.json :as json]',
 'str':'[clojure.string :as str]',
 'ds':'[datastar-kit.ds :as ds]',
 'page':'[hiccup.page :as page]',
 'h':'[hiccup2.core :as h]'}
IMPORTS={'LocalDate':('java.time','LocalDate'),'ZoneId':('java.time','ZoneId'),
         'DateTimeFormatter':('java.time.format','DateTimeFormatter')}

manifest={}
for ns,items in assign.items():
    text='\n'.join('\n'.join(it['pre']+it['body']) for it in items)
    c=clean(text)
    exts=sorted(a for a in EXT_ALIASES if re.search(r'(?<![\w./-])'+re.escape(a)+r'/',c))
    imps=sorted(k for k in IMPORTS if re.search(r'(?<![\w.-])'+k+r'[/.\s)]',c))
    # view refers
    refers={}
    for it in items:
        for r in usage.get(it['name'],[]):
            tns=name2ns[r]
            if tns!=ns: refers.setdefault(tns,set()).add(r)
    req=[]
    for tns in sorted(refers):
        rs=' '.join(sorted(refers[tns]))
        req.append('[cfp-scheduler-killer.views.%s :refer [%s]]'%(tns,rs))
    req+= [EXT_ALIASES[a] for a in exts]
    body_parts=[]
    hdr=['(ns cfp-scheduler-killer.views.%s'%ns, '  "%s"'%DOCS[ns]]
    req=sorted(req)
    if req:
        hdr.append('  (:require')
        for i,r in enumerate(req):
            hdr.append('   '+r + (')' if i==len(req)-1 else ''))
    if imps:
        byp={}
        for k in imps: byp.setdefault(IMPORTS[k][0],[]).append(IMPORTS[k][1])
        hdr.append('  (:import')
        ks=sorted(byp)
        for i,pk in enumerate(ks):
            hdr.append('   (%s %s)'%(pk,' '.join(sorted(byp[pk]))) + (')' if i==len(ks)-1 else ''))
    hdr[-1]=hdr[-1]+')'
    out='\n'.join(hdr)+'\n\n'+'\n\n'.join('\n'.join(it['pre']+it['body']) for it in items)+'\n'
    path=ROOT+'/src/cfp_scheduler_killer/views/'+ns.replace('-','_')+'.clj'
    os.makedirs(os.path.dirname(path),exist_ok=True)
    open(path,'w').write(out)
    manifest[ns]=dict(path=path,forms=[it['name'] for it in items],requires=req)
json.dump(manifest,open('/var/tmp/forge/plan2/N2/manifest.json','w'),indent=1)
print("publicized:",sorted(priv_public))
for ns in sorted(manifest): print(ns,len(manifest[ns]['forms']))
