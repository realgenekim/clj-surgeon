#!/usr/bin/env python3
"""Owned per-arm server on 8301. No model/benchmark orchestration.
start --run-dir ROOT/servers/ARM --fixture ROOT/fixtures/ARM --sha FULL40
stop --run-dir ROOT/servers/ARM
"""
import argparse,importlib.util,json,os,re,signal,subprocess,sys,time,tempfile
from pathlib import Path
from datetime import datetime,timezone
from urllib.request import urlopen
ROOT=Path('/var/tmp/forge/astra-fair2-data-fx')
SOURCE=ROOT/'server-src'
PORT=8332

def module(name,path):
 s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
adapter=module('frozen_adapter',Path('/var/tmp/forge/astra-fair-epoch2-fx/bench/fair-epoch2/adapter.py'))
watch=module('frozen_watch',Path('/var/tmp/forge/astra-fair-epoch2-fx/bench/anvil-arms/watch.py'))

def write(p,x):
 # A completed same-directory inode is published exclusively: readers can never
 # observe a partially serialized ready/owner/stop receipt, and no file is replaced.
 fd,temp=tempfile.mkstemp(prefix='.'+p.name+'-',suffix='.publishing',dir=p.parent)
 try:
  with os.fdopen(fd,'w') as f:
   json.dump(x,f,indent=2,sort_keys=True);f.write('\n');f.flush();os.fsync(f.fileno())
  os.link(temp,p)
 finally:
  os.unlink(temp)
def stamp():return datetime.now(timezone.utc).isoformat()
def git(*args):return subprocess.run(['/usr/bin/git','-C',str(SOURCE),*args],check=True,capture_output=True,text=True).stdout.strip()
def check_source(sha):
 if not re.fullmatch('[0-9a-f]{40}',sha) or git('rev-parse','HEAD')!=sha or git('status','--porcelain'):
  raise ValueError('server source must be clean at exactly requested full SHA')

def parse_ready(text):
 """Parse only the server's flat ready-map subset; never evaluate EDN."""
 token=re.compile(r'\s*("(?:[^"\\]|\\.)*"|:[A-Za-z0-9_./-]+|-?[0-9]+|true|false|nil|[{},])')
 pos=0;tokens=[]
 while pos<len(text.rstrip()):
  m=token.match(text,pos)
  if not m:raise ValueError('unsupported ready EDN token')
  if m[1]!=',':tokens.append(m[1])
  pos=m.end()
 if len(tokens)<2 or tokens[0]!='{' or tokens[-1]!='}' or len(tokens)%2:
  raise ValueError('ready EDN must be a flat map')
 out={}
 for key,val in zip(tokens[1:-1:2],tokens[2:-1:2]):
  if not key.startswith(':') or key[1:] in out:raise ValueError('invalid/duplicate ready key')
  if val.startswith(':'):value=val[1:]
  elif val=='nil':value=None
  else:value=json.loads(val)
  out[key[1:]]=value
 return out

def launch_command(run,fixture):
 return ['/home/forge/bin/suite-run','/usr/bin/taskset','-c','12,13','/usr/local/bin/clojure','-J-Xms64m','-J-Xmx512m',
         '-X:clj-surgeon/mcp',':project-dir',json.dumps(str(fixture)),':port',str(PORT),
         ':telemetry',':full',':telemetry-dir',json.dumps(str(run/'telemetry')),
         ':run-id',json.dumps('astra-'+run.name),':ready-file',json.dumps(str(run/'ready.edn')),':nrepl-port',':none']

def ready_evidence(run,fixture,sha,owned):
 raw=(run/'ready.edn').read_bytes();data=parse_ready(raw.decode())
 expected={'project-root':str(fixture),'host':'127.0.0.1','port':PORT,'server':'clj-surgeon','ok':True,
           'url':f'http://127.0.0.1:{PORT}/mcp'}
 if any(data.get(k)!=v for k,v in expected.items()):raise ValueError('server-owned ready binding differs')
 pid=data.get('pid');info=watch.proc_stat(pid) if isinstance(pid,int) else None
 if not info or owned.get(pid)!=info['starttime']:raise ValueError('ready PID is not owned descendant')
 if Path(f'/proc/{pid}/cwd').resolve()!=SOURCE.resolve():raise ValueError('server PID cwd differs')
 if not adapter.pid_listens(pid,PORT):raise ValueError('server PID does not own listener')
 check_source(sha)
 health_url=f'http://127.0.0.1:{PORT}/healthz'
 with urlopen(health_url,timeout=2) as response:health=response.read(1048576)
 evidence={'mcp_url':expected['url'],'healthz_url':health_url,'server_sha':sha,'project_root':str(fixture),
           'port_pid':pid,'server_cwd':str(SOURCE.resolve()),'healthz_sha256':adapter.digest(health),
           'server_starttime':info['starttime'],'ready_edn_sha256':adapter.digest(raw)}
 adapter.validate_ready(evidence,expected['url'],sha,fixture,health)
 return evidence,health

def cleanup_owned(recorded):
 # Identity checks prevent reused PIDs from becoming signal targets.
 def scan():
  for pid,start in watch.descendants_of(os.getpid()).items():
   if pid not in (os.getpid(),os.getppid(),1):recorded.setdefault(pid,start)
 def live():
  return [pid for pid,start in recorded.items() if (info:=watch.proc_stat(pid)) and info['starttime']==start and info['state']!='Z']
 for sig in [signal.SIGTERM,signal.SIGKILL]:
  deadline=time.monotonic()+1
  while True:
   scan()
   for pid in live():
    try:os.kill(pid,sig)
    except ProcessLookupError:pass
   if not live() or time.monotonic()>=deadline:break
   time.sleep(.05)
 return live()

def supervise(run):
 plan=json.loads((run/'plan.json').read_text());fixture=Path(plan['fixture']);started=time.monotonic()
 if watch.set_child_subreaper()!='ok':raise ValueError('subreaper unavailable')
 stopped=False
 def halt(*_):
  nonlocal stopped;stopped=True
 signal.signal(signal.SIGTERM,halt);signal.signal(signal.SIGINT,halt)
 write(run/'owner.json',{'supervisor_pid':os.getpid(),'supervisor_starttime':watch.proc_stat(os.getpid())['starttime'],'started_utc':stamp()})
 recorded={};proc=None;reason='unknown'
 try:
  check_source(plan['sha'])
  env=dict(os.environ,TMPDIR=str(run/'tmp'),JAVA_TOOL_OPTIONS=f'-Xms64m -Xmx512m -Djava.io.tmpdir={run/"tmp"}',PYTHONDONTWRITEBYTECODE='1')
  with (run/'server.log').open('x') as log:
   proc=subprocess.Popen(plan['command'],cwd=SOURCE,env=env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
  deadline=started+plan['startup_timeout_s'];last_error=None
  while time.monotonic()<deadline and not stopped and not (run/'stop-request.json').exists():
   if proc.poll() is not None:raise ValueError('server exited before readiness: '+str(proc.returncode))
   if (run/'ready.edn').exists():
    try:
     owned=watch.descendants_of(os.getpid());recorded.update({p:s for p,s in owned.items() if p!=os.getpid()})
     evidence,health=ready_evidence(run,fixture,plan['sha'],owned)
     (run/'healthz.json').write_bytes(health)
     evidence.update(startup_wall_s=time.monotonic()-started,startup_completed_utc=stamp())
     write(run/'ready.json',evidence);break
    except (OSError,ValueError) as error:last_error=str(error)
   time.sleep(.2)
  else:raise ValueError('startup timed out/stopped; last readiness refusal: '+str(last_error))
  reason='stopped'
  # No /proc scans during model task: poll the server subprocess and stop sentinel.
  # The supervisor is detached; the 1800-second lifetime cap bounds abandoned runs.
  lifetime=time.monotonic()+1800
  while not stopped and not (run/'stop-request.json').exists() and time.monotonic()<lifetime:
   if proc.poll() is not None:reason='server-exited';break
   time.sleep(.2)
  if time.monotonic()>=lifetime:reason='lifetime-cap'
 except Exception as error:
  reason='startup-or-runtime-failed';write(run/'failure.json',{'error':str(error),'utc':stamp()})
 finally:
  survivors=cleanup_owned(recorded)
  if proc:
   try:proc.wait(timeout=1)
   except subprocess.TimeoutExpired:pass
  while True:
   try:
    if os.waitpid(-1,os.WNOHANG)[0]==0:break
   except ChildProcessError:break
  write(run/'stopped.json',{'reason':reason,'survivors':survivors,'utc':stamp(),'total_lifetime_s':time.monotonic()-started})
 return 0 if not survivors and reason=='stopped' else 2

def stop(run):
 if (run/'stopped.json').exists():return json.loads((run/'stopped.json').read_text())
 owner=json.loads((run/'owner.json').read_text());info=watch.proc_stat(owner['supervisor_pid'])
 if not info or info['starttime']!=owner['supervisor_starttime']:raise ValueError('owner absent/reused; refusing signal')
 if not (run/'stop-request.json').exists():write(run/'stop-request.json',{'utc':stamp()})
 deadline=time.monotonic()+6
 while time.monotonic()<deadline:
  if (run/'stopped.json').exists():return json.loads((run/'stopped.json').read_text())
  time.sleep(.1)
 raise ValueError('owned supervisor did not confirm stop; inspect retained logs')

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('action',choices=['start','stop','supervise','dry-run']);p.add_argument('--run-dir',required=True);p.add_argument('--fixture');p.add_argument('--sha');p.add_argument('--startup-timeout',type=int,default=90);args=p.parse_args()
 run=adapter.confined(args.run_dir)
 if args.action=='stop':
  result=stop(run);print(json.dumps(result));return 2 if result.get('survivors') else 0
 if args.action=='supervise':return supervise(run)
 if args.fixture is None or args.sha is None:p.error('start/dry-run needs fixture and sha')
 fixture=adapter.confined(args.fixture)
 if run.exists():raise ValueError('run directory exists; refusing overwrite')
 if not fixture.is_dir() or run.is_relative_to(fixture) or fixture.is_relative_to(run):raise ValueError('fixture must exist and be disjoint')
 if not 1<=args.startup_timeout<=120:raise ValueError('startup timeout must be1..120s')
 command=launch_command(run,fixture)
 if args.action=='dry-run':print(json.dumps({'source':str(SOURCE),'command':command,'sha':args.sha},indent=2));return 0
 check_source(args.sha)
 # Refuse occupied port without contacting/killing its owner.
 for table in ['tcp','tcp6']:
  if any(int(line.split()[1].split(':')[1],16)==PORT and line.split()[3]=='0A' for line in Path('/proc/net/'+table).read_text().splitlines()[1:]):raise ValueError('8301 already occupied')
 run.mkdir(parents=True,exist_ok=False);(run/'tmp').mkdir();(run/'telemetry').mkdir()
 write(run/'plan.json',{'command':command,'fixture':str(fixture),'sha':args.sha,'startup_timeout_s':args.startup_timeout,'helper_sha256':adapter.file_digest(Path(__file__))})
 with (run/'supervisor.log').open('x') as log:
  proc=subprocess.Popen([sys.executable,str(Path(__file__).resolve()),'supervise','--run-dir',str(run)],stdin=subprocess.DEVNULL,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 deadline=time.monotonic()+args.startup_timeout+3
 while time.monotonic()<deadline:
  if (run/'ready.json').exists():print((run/'ready.json').read_text());return 0
  if proc.poll() is not None:raise ValueError('supervisor exited: '+str(proc.returncode))
  time.sleep(.2)
 stop(run);raise ValueError('startup deadline reached; owned supervisor stopped')

if __name__=='__main__':
 try:sys.exit(main())
 except (OSError,ValueError,subprocess.CalledProcessError) as error:print('SERVER LIFECYCLE REFUSED: '+str(error),file=sys.stderr);sys.exit(2)
