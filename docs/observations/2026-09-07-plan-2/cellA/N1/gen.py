import sys, json, re, os, collections
sys.path.insert(0,'/var/tmp/forge/plan2/N1')
from map import OWNER, PLAN, NS, FILE

ALIAS = {'auth':'v-auth','avatar':'avatar','committee':'committee','communications':'comms',
 'dashboard':'dashboard','event-setup':'event-setup','format':'fmt','form-builder':'form-builder',
 'form-controls':'controls','integrations':'integrations','live-drafts':'drafts','log':'v-log',
 'organizer-layout':'layout','people':'people','portal':'v-portal','public-cfp':'public-cfp',
 'replay':'v-replay','review':'review','schedule':'v-schedule','shell':'shell'}

DOC = {
 'auth':'Landing and sign-in pages — the two surfaces a signed-out visitor sees.',
 'avatar':'Deterministic initials and colour for a person chip.',
 'committee':'Program-committee roster pages.',
 'communications':'Speaker communications: the comms console and the inform composer.',
 'dashboard':'The event dashboard — the organizer\'s home region and page.',
 'event-setup':'Creating and editing an event: the list, the wizard, the details page.',
 'format':'Dates, times and URLs — the one place a timestamp is formatted.',
 'form-builder':'The CFP form builder: grid, field editor, preview.',
 'form-controls':'Single form inputs and their error marks, shared by every form surface.',
 'integrations':'Exports, API docs, Slack and the settings page.',
 'live-drafts':'Live draft status strips shared by the portal and the public CFP.',
 'log':'The event log page and its SSE region.',
 'organizer-layout':'Organizer chrome: sidebar, header, breadcrumb, shell.',
 'people':'Person pages and profile links.',
 'portal':'The speaker portal — everything a speaker sees about their own talk.',
 'public-cfp':'The public call-for-papers page and its markdown-lite renderer.',
 'replay':'The replay page and its progress bar.',
 'review':'The review board: rows, opinions, stars, submission detail.',
 'schedule':'The schedule grid, agenda and placement forms.',
 'shell':'The HTML page skeleton every surface renders into.',
}

ROOT='/home/forge/src/cc-split-N1'
src=open('/var/tmp/forge/plan2/N1/views-orig.clj').read()
lines=src.split('\n')
forms=json.load(open('/var/tmp/forge/plan2/N1/forms.json'))
kondo=json.load(open('/var/tmp/forge/plan2/N1/kondo.json'))
vus=kondo['analysis']['var-usages']
vds={d['name']:d for d in kondo['analysis']['var-definitions']}

# ---- 1. assign forms to destinations -------------------------------------
DROP_DECLARE_ROW = 3059  # (declare datastar-script cfp-note portal-draft-status)
assign=[]  # (dest_or_None, start_line, end_line)
prev_end=0
for f in forms:
    s,e=f['start_line'],f['end_line']
    block_start=prev_end+1
    prev_end=e
    if f['head']=='ns':
        continue
    if f['head']=='declare':
        if s==DROP_DECLARE_ROW:
            continue
        dest=OWNER[f['name']]
    else:
        dest=OWNER[f['name']]
    assign.append((dest, block_start, e, f))

# ---- 2. cross-namespace usages -> rewrites + publicize --------------------
rewrites=collections.defaultdict(list)   # row -> [(col, endcol, newtext)]
publicize=set()
edges=set()
extern=collections.defaultdict(set)      # dest -> set of external ns
for u in vus:
    fv=u.get('from-var')
    if fv is None or fv not in OWNER: continue
    a=OWNER[fv]
    to=u['to']
    if to=='cfp-scheduler-killer.views':
        n=u['name']
        if n not in OWNER:  # shouldn't happen
            print('UNKNOWN internal usage', n, u['row']); continue
        b=OWNER[n]
        if a!=b:
            edges.add((a,b))
            rewrites[u['name-row']].append((u['name-col'], u['name-end-col'], ALIAS[b]+'/'+n))
            if vds[n].get('private'): publicize.add(n)
    elif to!='clojure.core':
        extern[a].add(to)

EXT_ALIAS={'cfp-scheduler-killer.committees':'committees','cfp-scheduler-killer.events':'events',
 'cfp-scheduler-killer.exports':'exports','cfp-scheduler-killer.forms':'forms',
 'cfp-scheduler-killer.portal':'portal','cfp-scheduler-killer.reviews':'reviews',
 'cfp-scheduler-killer.schedule':'schedule','cfp-scheduler-killer.submissions':'submissions',
 'clojure.data.json':'json','clojure.string':'str','datastar-kit.ds':'ds',
 'hiccup.page':'page','hiccup2.core':'h'}

# ---- 3. apply rewrites to a copy of the lines -----------------------------
out=list(lines)
for row, reps in rewrites.items():
    l=out[row-1]
    for col,endcol,new in sorted(reps, key=lambda r:-r[0]):
        old=l[col-1:endcol-1]
        assert '/' not in old, (row,old)
        l=l[:col-1]+new+l[endcol-1:]
    out[row-1]=l

# publicize private defs
for n in sorted(publicize):
    d=vds[n]; row=d['row']
    l=out[row-1]
    l2=re.sub(r'^\(defn-\s', '(defn ', l)
    if l2==l:
        l2=re.sub(r'^\(def\s+\^:private\s', '(def ', l)
    assert l2!=l, (n,l)
    out[row-1]=l2

# ---- 4. build the files ---------------------------------------------------
os.makedirs(ROOT+'/src/cfp_scheduler_killer/views', exist_ok=True)
per=collections.defaultdict(list)
for dest, bs, be, f in assign:
    body='\n'.join(out[bs-1:be]).strip('\n')
    per[dest].append(body)

view_reqs=collections.defaultdict(set)
for a,b in edges: view_reqs[a].add(b)

summary={}
for dest in PLAN:
    body='\n\n'.join(per[dest])
    reqs=[]
    for e in sorted(extern[dest]):
        reqs.append('   [%s :as %s]' % (e, EXT_ALIAS[e]))
    if dest=='review' and 'cfp-scheduler-killer.store/' in body:
        reqs.append('   [cfp-scheduler-killer.store]')
    for v in sorted(view_reqs[dest]):
        reqs.append('   [%s :as %s]' % (NS[v], ALIAS[v]))
    reqs.sort()
    imports=[]
    jt=[c for c in ['LocalDate','ZoneId'] if re.search(r'(?<![.\w])'+c+r'\b', body)]
    if jt: imports.append('   (java.time %s)' % ' '.join(jt))
    if re.search(r'(?<![.\w])DateTimeFormatter\b', body):
        imports.append('   (java.time.format DateTimeFormatter)')
    ns='(ns %s\n  "%s"' % (NS[dest], DOC[dest])
    if reqs:
        ns+='\n  (:require\n%s)' % '\n'.join(reqs)
    if imports:
        ns+='\n  (:import\n%s)' % '\n'.join(imports)
    ns+=')'
    text=ns+'\n\n'+body+'\n'
    open(ROOT+'/src/cfp_scheduler_killer/views/'+FILE[dest]+'.clj','w').write(text)
    summary[dest]=len(per[dest])

print('publicized:', sorted(publicize))
print('edges:')
for a,b in sorted(edges): print('  ',a,'->',b)
print('forms per file:', summary, 'total', sum(summary.values()))
