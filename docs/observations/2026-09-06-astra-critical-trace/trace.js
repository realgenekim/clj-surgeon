'use strict';
const fs=require('fs'),os=require('os'),cp=require('child_process'),crypto=require('crypto');
const hash=x=>crypto.createHash('sha256').update(x).digest('hex');
const CLOCK=hash(os.hostname()+'\n'+fs.readFileSync('/proc/sys/kernel/random/boot_id','utf8'));
const now=()=>process.hrtime.bigint().toString(), id=x=>typeof x==='string'&&/^[A-Za-z0-9_.-]{1,80}$/.test(x);
const need=(x,m)=>{if(!x)throw Error(m);};
const wait=ms=>new Promise(r=>setTimeout(r,ms));
function read(path){
 const s=fs.lstatSync(path);need(s.isFile()&&!s.isSymbolicLink()&&s.size<=4194304,'ledger-file-or-budget');
 const raw=fs.readFileSync(path,'utf8');need(raw.endsWith('\n'),'partial-event');
 return raw.trimEnd().split('\n').map(l=>{need(Buffer.byteLength(l)<=4096,'event-budget');return JSON.parse(l);});
}
function analyze(events,complete=false){
 need(Array.isArray(events)&&events.length>0,'empty-ledger');
 const first=events[0],spans=new Map();let end=null,last=-1n;
 need(first.event==='begin'&&id(first.task),'missing-begin');
 for(let i=0;i<events.length;i++){
  const e=events[i];need(e&&e.task===first.task&&e.clock===CLOCK,'task-or-clock-mismatch');
  need(typeof e.ns==='string'&&/^\d+$/.test(e.ns)&&Number.isInteger(e.pid)&&e.pid>0&&typeof e.utc==='string'&&Number.isFinite(Date.parse(e.utc)),'event-metadata');
  const t=BigInt(e.ns);need(t>=last&&end===null,'event-order');last=t;
  if(i===0)continue;
  if(e.event==='end'){need([...spans.values()].every(s=>s.end!==undefined),'open-spans');end=t;continue;}
  need(id(e.span),'span-id');
  if(e.event==='start'){
   need(!spans.has(e.span)&&['process','external'].includes(e.kind)&&Array.isArray(e.deps),'duplicate-or-invalid-start');
   need(new Set(e.deps).size===e.deps.length&&e.deps.every(d=>id(d)&&d!==e.span),'invalid-dependencies');
   for(const d of e.deps)need(spans.has(d)&&spans.get(d).end!==undefined&&spans.get(d).end<=t,'dependency-not-complete');
   if(e.kind==='process')need(/^[a-f0-9]{64}$/.test(e.argv_hash||''),'argv-hash');
   spans.set(e.span,{id:e.span,kind:e.kind,deps:e.deps,start:t,phase:'start'});continue;
  }
  const s=spans.get(e.span);need(s&&s.end===undefined,'unknown-or-closed-span');
  if(e.event==='span-end'){
   need(s.kind==='external'||s.phase==='close','process-not-closed');
   need(Number.isInteger(e.code)&&e.code>=0&&e.code<=255,'terminal-code');s.end=t;s.code=e.code;continue;
  }
  need(s.kind==='process','external-process-event');
  if(e.event==='spawn'){need(s.phase==='start'&&Number.isInteger(e.child_pid)&&e.child_pid>0,'spawn-order');s.phase='spawn';s.spawn=t;}
  else if(e.event==='spawn-error'){need(s.phase==='start','spawn-error-order');s.phase='spawn-error';}
  else if(e.event==='exit'){need(s.phase==='spawn','exit-order');need(e.code===null||Number.isInteger(e.code),'exit-code');s.phase='exit';}
  else if(e.event==='close'){need(['exit','spawn-error'].includes(s.phase),'close-order');s.phase='close';s.close=t;}
  else throw Error('unknown-event');
 }
 if(complete)need(end!==null,'task-not-ended');
 return {first,spans,end};
}
function locked(path,fn){
 const lock=path+'.lock',deadline=Date.now()+2000;let held=false;
 while(!held){try{fs.mkdirSync(lock,{mode:0o700});held=true;}catch(e){if(e.code!=='EEXIST')throw e;need(Date.now()<deadline,'ledger-lock-timeout');Atomics.wait(new Int32Array(new SharedArrayBuffer(4)),0,0,10);}}
 try{return fn();}finally{fs.rmdirSync(lock);}
}
function append(path,body){return locked(path,()=>{
 const events=read(path),state=analyze(events);const e={...body,task:state.first.task,clock:CLOCK,ns:now(),utc:new Date().toISOString(),pid:process.pid};
 analyze([...events,e]);const bytes=Buffer.from(JSON.stringify(e)+'\n');need(bytes.length<=4096,'event-budget');
 const fd=fs.openSync(path,'a');try{need(fs.writeSync(fd,bytes)===bytes.length,'short-append');}finally{fs.closeSync(fd);}return e;
});}
function begin(path,task){need(id(task),'invalid-task');locked(path,()=>{
 const e={event:'begin',task,clock:CLOCK,ns:now(),utc:new Date().toISOString(),pid:process.pid};fs.writeFileSync(path,JSON.stringify(e)+'\n',{flag:'wx',mode:0o600});
});}
function deps(text){need(typeof text==='string','missing-dependencies');return text==='-'?[]:text.split(',');}
function report(events){
 const {first,spans,end}=analyze(events,true),start=BigInt(first.ns),ms=x=>Number(x)/1e6;
 const all=[...spans.values()],ranges=all.map(s=>[s.start,s.end]).sort((a,b)=>a[0]<b[0]?-1:a[0]>b[0]?1:0),union=[];
 for(const r of ranges){const p=union[union.length-1];if(p&&r[0]<=p[1]){if(r[1]>p[1])p[1]=r[1];}else union.push([...r]);}
 let cursor=start,covered=0n;const gaps=[];
 for(const [a,b] of union){if(a>cursor)gaps.push({from_ms:ms(cursor-start),to_ms:ms(a-start),label:'unattributed'});covered+=b-a;cursor=b;}
 if(cursor<end)gaps.push({from_ms:ms(cursor-start),to_ms:ms(end-start),label:'unattributed'});
 const paths=new Map();let longest={duration:0n,path:[]};
 for(const s of all){let best={duration:0n,path:[]};for(const d of s.deps){const p=paths.get(d);if(p.duration>best.duration)best=p;}
  const p={duration:best.duration+s.end-s.start,path:[...best.path,s.id]};paths.set(s.id,p);if(p.duration>longest.duration)longest=p;
 }
 return {task:first.task,clock:CLOCK,event_count:events.length,wall_ms:ms(end-start),span_sum_ms:ms(all.reduce((n,s)=>n+s.end-s.start,0n)),span_union_ms:ms(covered),unattributed_ms:ms(end-start-covered),uncovered_intervals:gaps,
  declared_dependency_path:{label:'DECLARED DAG longest duration path; not causal savings',duration_ms:ms(longest.duration),spans:longest.path},
  spans:all.map(s=>({id:s.id,kind:s.kind,label:s.kind==='external'?'EXTERNAL BRACKET (includes inter-call/model gaps)':'process spawn-through-close/cleanup bracket',deps:s.deps,duration_ms:ms(s.end-s.start),observed_spawn_to_close_ms:s.spawn!==undefined&&s.close!==undefined?ms(s.close-s.spawn):null,code:s.code})),
  provider_first_token:'UNKNOWN',provider_last_token:'UNKNOWN',jvm_ready:'UNKNOWN',service_presence:'UNKNOWN',fork_exec_separation:'UNKNOWN',descendant_runtime_ready:'UNKNOWN'};
}
function alive(pid){try{process.kill(-pid,0);return true;}catch(e){if(e.code==='ESRCH')return false;throw e;}}
function send(pid,sig){try{process.kill(-pid,sig);}catch(e){if(e.code!=='ESRCH')throw e;}}
async function run(path,span,ds,argv){
 need(argv.length>0,'missing-command');append(path,{event:'start',span,kind:'process',deps:ds,argv_hash:hash(JSON.stringify(argv))});
 let child,timer,expired=false,spawnError=false;
 try{child=cp.spawn(argv[0],argv.slice(1),{stdio:'inherit',detached:true});}
 catch(e){append(path,{event:'spawn-error',span,error_code:'spawn-failed'});append(path,{event:'close',span,code:null,signal:null});append(path,{event:'span-end',span,code:127});return 127;}
 let failure;
 const record=body=>{try{append(path,{span,...body});}catch(e){failure=e;if(child.pid)send(child.pid,'SIGKILL');}};
 return await new Promise((resolve,reject)=>{
  child.on('spawn',()=>record({event:'spawn',child_pid:child.pid}));
  child.on('error',e=>{spawnError=true;record({event:'spawn-error',error_code:/^[A-Z0-9_]+$/.test(e.code||'')?e.code:'spawn-failed'});});
  child.on('exit',(code,signal)=>record({event:'exit',code,signal}));
  timer=setTimeout(()=>{expired=true;if(child.pid){send(child.pid,'SIGTERM');setTimeout(()=>send(child.pid,'SIGKILL'),500).unref();}},60000);
  child.on('close',async(code,signal)=>{clearTimeout(timer);try{
   record({event:'close',code,signal});let cleanup=false,remaining=false;
   if(child.pid&&alive(child.pid)){cleanup=true;send(child.pid,'SIGTERM');await wait(100);if(alive(child.pid)){send(child.pid,'SIGKILL');await wait(100);}remaining=alive(child.pid);}
   if(failure)throw failure;const result=expired?124:remaining||cleanup?125:spawnError?127:code===null?128:Math.min(255,Math.max(0,code));
   append(path,{event:'span-end',span,code:result,signal,timeout:expired,group_cleanup:cleanup,group_remaining:remaining});resolve(result);
  }catch(e){reject(e);}});
 });
}
async function main(args){const [op,path,...rest]=args;need(path,'missing-path');
 if(op==='begin'){need(rest.length===1,'usage');begin(path,rest[0]);}
 else if(op==='run'){need(rest[2]==='--'&&rest.length>=4,'usage');return await run(path,rest[0],deps(rest[1]),rest.slice(3));}
 else if(op==='mark-start'){need(rest.length===2,'usage');append(path,{event:'start',span:rest[0],kind:'external',deps:deps(rest[1])});}
 else if(op==='mark-end'){need(rest.length===1,'usage');locked(path,()=>{const s=analyze(read(path)).spans.get(rest[0]);need(s&&s.kind==='external'&&s.end===undefined,'not-open-external');});append(path,{event:'span-end',span:rest[0],code:0});}
 else if(op==='end'){need(rest.length===0,'usage');append(path,{event:'end'});}
 else if(op==='report'){need(rest.length===0,'usage');console.log(JSON.stringify(report(read(path)),null,2));}
 else throw Error('unknown-command');return 0;
}
module.exports={CLOCK,analyze,report,main};
if(require.main===module)main(process.argv.slice(2)).then(c=>{process.exitCode=c;}).catch(e=>{console.error('REFUSED '+e.message);process.exitCode=2;});
