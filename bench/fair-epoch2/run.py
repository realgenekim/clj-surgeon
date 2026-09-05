"""Serial epoch-2 driver using the existing attested adapter and watcher."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,sys,time,threading
from datetime import datetime,timezone
ROOT=Path('/var/tmp/forge/astra-fair2-data-fx')
HERE=Path(__file__).resolve().parent
SHA='13c12401ac05586fbf5bb2c3b7be9a6258234fcc'

def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def snapshot(p):
    return {str(f.relative_to(p)):[digest(f),f.stat().st_mode&0o777]
            for f in sorted(p.rglob('*')) if f.is_file() and '.git' not in f.relative_to(p).parts}
def write(p,v):
    with p.open('x') as f:json.dump(v,f,indent=2)
def invoke(argv,log,cwd=None):
    with log.open('x') as f:
        return subprocess.run(argv,cwd=cwd,stdout=f,stderr=subprocess.STDOUT,
            env=dict(os.environ,SLOT_OWNER='astra',SUITE_NICE='0',PYTHONDONTWRITEBYTECODE='1'),timeout=1200).returncode
def note(text):
    with Path('/var/tmp/forge/fable-to-astra.md').open('a') as f:
        f.write('\n### ASTRA EXPERIMENT '+datetime.now(timezone.utc).strftime('%H:%MZ')+'\n\n'+text+'\n')
def run(name,model,route):
    a=ROOT/'runs'/name;a.mkdir(parents=True,exist_ok=False)
    wt=ROOT/'fixtures'/name
    subprocess.run(['git','clone','-q','--no-hardlinks',str(ROOT/'baseline-v2'),str(wt)],check=True)
    # Clone uses the frozen commit, never the hand-drive's unstaged changes.
    before=snapshot(wt)
    manifest=json.loads((ROOT/'frozen.json').read_text())
    for p,h in manifest['files'].items():
        if digest(Path(p))!=h:raise ValueError('frozen apparatus changed: '+p)
    write(a/'subject.json',{'model_requested':model,'route':route,'fixture':before,
        'source_commit':subprocess.check_output(['git','-C',str(wt),'rev-parse','HEAD'],text=True).strip(),
        'frozen':manifest,'utc':datetime.now(timezone.utc).isoformat()})
    started=time.monotonic();stop=threading.Event()
    def sample():
        with (a/'load.jsonl').open('x') as f:
            while not stop.is_set():
                f.write(json.dumps({'monotonic':time.monotonic(),'load':Path('/proc/loadavg').read_text().strip()})+'\n');f.flush();stop.wait(1)
    monitor=threading.Thread(target=sample);monitor.start()
    server=ROOT/'servers'/name;arm=ROOT/'arms'/name;result={'name':name,'model_requested':model,'route':route,'accepted':False}
    try:
        if route=='tool':
            rc=invoke([sys.executable,str(HERE/'server_lifecycle.py'),'start','--run-dir',str(server),'--fixture',str(wt),'--sha',SHA],a/'startup.log')
            if rc:raise ValueError('server attestation refused')
        cmd=[sys.executable,str(HERE/'adapter.py'),'run','--arm',str(arm),'--worktree',str(wt),'--prompt',str(ROOT/(route+'.txt')),'--model',model,'--cpus','12,13','--canonical',str(ROOT/'baseline-v2'),'--max-wall','600']
        if route=='tool':cmd+=['--mcp-url','http://127.0.0.1:8332/mcp','--server-sha',SHA,'--ready',str(server/'ready.json')]
        rc=invoke(cmd,a/'adapter.log')
        if rc:raise ValueError('adapter refused: '+str(rc))
        identity=json.loads((arm/'adapter-result.json').read_text())
        if not identity.get('valid_measurement'):raise ValueError('model attestation incomplete')
        result['resolved_model']=identity['resolved_model']
        after=snapshot(wt)
        protected={p:v for p,v in before.items() if not p.startswith('src/')}
        if any(after.get(p)!=v for p,v in protected.items()):raise ValueError('protected fixture changed')
        if set(after)-set(before)-{'src/acme/response.clj'}:raise ValueError('unexpected files')
        if set(before)-set(after):raise ValueError('unexpected deletions')
        if invoke([sys.executable,str(HERE/'accept.py'),str(wt)],a/'acceptance.log'):raise ValueError('behavioral acceptance failed')
        for p,h in manifest['files'].items():
            if digest(Path(p))!=h:raise ValueError('frozen apparatus changed during task: '+p)
        result['protected_bytes_match']=True
        if route=='tool':
            events=[json.loads(l) for p in (server/'telemetry').glob('*.jsonl') for l in p.read_text().splitlines()]
            calls=[e for e in events if e.get('event')=='tool.call' and e.get('tool')=='helper_extraction']
            write(a/'helper-events.json',calls)
            if not calls:raise ValueError('public helper call unobserved')
            result['helper_calls']=len(calls)
            result['helper_refusals']=[e.get('response',{}).get('error_type') for e in calls if e.get('response',{}).get('ok') is False]
        # Adapter froze all tracked/untracked changes; bind the artifact to accepted bytes.
        result['diff_sha256']=digest(arm/'diff.patch')
        write(a/'final-tree.json',after)
        result['accepted']=True
    except Exception as e:result['failure']=str(e)
    finally:
        result['complete_wall_s']=time.monotonic()-started
        result['ended_utc']=datetime.now(timezone.utc).isoformat()
        stop.set();monitor.join()
        if route=='tool' and (server/'owner.json').exists():
            result['cleanup_rc']=invoke([sys.executable,str(HERE/'server_lifecycle.py'),'stop','--run-dir',str(server)],a/'cleanup.log')
        write(a/'result.json',result)
        note(name+': '+json.dumps(result))
        print(json.dumps(result),flush=True)
    return result['accepted']

if __name__=='__main__':
    schedule=json.loads((ROOT/'schedule.json').read_text())
    for row in schedule:
        if not run(**row):sys.exit(2)
