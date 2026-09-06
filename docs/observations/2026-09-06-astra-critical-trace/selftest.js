'use strict';
const fs=require('fs'),path=require('path'),os=require('os'),cp=require('child_process'),assert=require('assert');
const {CLOCK,report,analyze}=require('./trace.js'),script=path.join(__dirname,'trace.js');
let checks=0;function check(name,f){f();checks++;console.log('PASS '+name);}
const event=(event,ms,rest={})=>({event,ns:String(BigInt(ms)*1000000n),clock:CLOCK,task:'synthetic',pid:1,utc:'2026-09-06T21:00:00.000Z',...rest});
const start=(id,t,deps=[])=>event('start',t,{span:id,kind:'external',deps});
const stop=(id,t)=>event('span-end',t,{span:id,code:0});
const overlap=[event('begin',0),start('a',10),start('b',20),stop('a',40),stop('b',50),event('end',60)];
check('overlap union distinct from summed spans and full task gaps',()=>{
 const r=report(overlap);assert.equal(r.wall_ms,60);assert.equal(r.span_sum_ms,60);assert.equal(r.span_union_ms,40);assert.equal(r.unattributed_ms,20);
 assert.deepEqual(r.uncovered_intervals.map(x=>[x.from_ms,x.to_ms]),[[0,10],[50,60]]);assert.equal(r.declared_dependency_path.duration_ms,30);
 assert(r.spans.every(x=>x.label.startsWith('EXTERNAL BRACKET')));assert.equal(r.jvm_ready,'UNKNOWN');
});
check('declared sequential path omits wait gap without calling it savings',()=>{
 const r=report([event('begin',0),start('a',5),stop('a',15),start('b',30,['a']),stop('b',50),event('end',60)]);
 assert.equal(r.declared_dependency_path.duration_ms,30);assert.deepEqual(r.declared_dependency_path.spans,['a','b']);assert.equal(r.unattributed_ms,30);
});
for(const [name,events] of [
 ['unknown dependency',[event('begin',0),start('a',1,['missing'])]],
 ['dependency still open',[event('begin',0),start('a',1),start('b',2,['a'])]],
 ['cycle forward edge',[event('begin',0),start('a',1,['b']),start('b',2,['a'])]],
 ['self dependency',[event('begin',0),start('a',1,['a'])]],
 ['duplicate closed span',[event('begin',0),start('a',1),stop('a',2),start('a',3)]],
 ['open span at end',[event('begin',0),start('a',1),event('end',2)]],
 ['clock mismatch',[event('begin',0),{...event('end',1),clock:'other'}]],
 ['task mismatch',[event('begin',0),{...event('end',1),task:'other'}]],
 ['event after end',[event('begin',0),event('end',1),start('a',2)]],
 ['reversed timestamp',[event('begin',10),event('end',1)]],
 ['process bypasses close',[event('begin',0),event('start',1,{span:'p',kind:'process',deps:[],argv_hash:'a'.repeat(64)}),stop('p',2)]]
])check('refuses '+name,()=>assert.throws(()=>analyze(events)));
const dir=fs.mkdtempSync(path.join(__dirname,'selftest-')),ledger=path.join(dir,'trace.json');
const run=(...a)=>cp.spawnSync(process.execPath,[script,...a],{encoding:'utf8',timeout:5000});
const must=(...a)=>{const r=run(...a);assert.equal(r.status,0,r.stderr);return r;};
async function child(args){return await new Promise((resolve,reject)=>{const p=cp.spawn(process.execPath,[script,...args],{stdio:'ignore'});p.on('error',reject);p.on('close',c=>resolve(c));});}
(async()=>{try{
 must('begin',ledger,'public');
 check('real command failure recorded terminal and exit propagated',()=>{assert.equal(run('run',ledger,'failure','-','--',process.execPath,'-e','process.exit(7)').status,7);});
 check('nonexistent command terminal spawn error no raw argv receipt',()=>{
  assert.equal(run('run',ledger,'missing','failure','--','/var/tmp/forge/no-command-SECRET_MARKER').status,127);
  const txt=fs.readFileSync(ledger,'utf8');assert(!txt.includes('SECRET_MARKER'));assert(txt.includes('spawn-error'));
 });
 const codes=await Promise.all(['x','y'].map(id=>child(['run',ledger,id,'-','--',process.execPath,'-e','setTimeout(()=>{},150)'])));
 check('concurrent invocations produce valid complete append events',()=>assert.deepEqual(codes,[0,0]));
 must('mark-start',ledger,'external','x,y');
 check('duplicate mark and premature task end refuse without corrupting ledger',()=>{assert.equal(run('mark-start',ledger,'external','-').status,2);assert.equal(run('end',ledger).status,2);});
 must('mark-end',ledger,'external');must('end',ledger);
 check('public report retains failures and brackets plus process timing',()=>{
  const r=JSON.parse(must('report',ledger).stdout);assert.equal(r.spans.find(x=>x.id==='failure').code,7);assert.equal(r.spans.find(x=>x.id==='missing').code,127);
  assert.equal(r.spans.find(x=>x.id==='external').kind,'external');assert(r.spans.find(x=>x.id==='x').observed_spawn_to_close_ms>0);assert.equal(r.spans.length,5);
 });
 check('begin cannot overwrite existing receipt',()=>assert.equal(run('begin',ledger,'other').status,2));
 console.log(JSON.stringify({checks,failures:0,scope:'synthetic + local Node processes only; no timeout60s execution or escaped-descendant confinement test'}));
}finally{fs.rmSync(dir,{recursive:true,force:true});}})().catch(e=>{console.error(e.stack);process.exitCode=1;});
