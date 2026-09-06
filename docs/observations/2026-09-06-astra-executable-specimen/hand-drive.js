// Bounded archived-data hand-drive; no provider/server/source mutation.
const fs = require('fs'), cp = require('child_process'), assert = require('assert'), path = require('path');
const {performance} = require('perf_hooks');
const dir = __dirname, manifest = JSON.parse(fs.readFileSync(dir+'/specimens.json')).map(e=>({...e,
  request:path.resolve(dir,e.request),receipt:path.resolve(dir,e.receipt)}));
// Explicit output location avoids rewriting the historical retained result.
const output = process.argv[2];
assert(output && path.isAbsolute(output),'supply a fresh absolute output directory');
fs.mkdirSync(output,{recursive:false});
const read = p => JSON.parse(fs.readFileSync(p));
const save = (name, data) => {const p=output+'/'+name+'.json';fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');return p;};
const arms = {native:['node',dir+'/specimen.js'],bb:['bb',dir+'/specimen.clj']};
function run(arm,path) {
  const t=performance.now(), [cmd,...args]=arms[arm];
  const r=cp.spawnSync(cmd,[...args,path],{encoding:'utf8',timeout:3000});
  return {arm,wall_ms:performance.now()-t,status:r.status,stdout:r.stdout,stderr:r.stderr,error:r.error?.message};
}
const rows=[];
// Both processes start afresh; five alternating-order pairs, shared-box component clocks.
for(let i=0;i<5;i++) for(const arm of i%2?['bb','native']:['native','bb']) {
  const r=run(arm,dir+'/specimens.json'); assert.equal(r.status,0,r.stderr);
  rows.push({repetition:i+1,...r});
}
const canonical=rows[0].stdout;
for(const r of rows) assert.equal(r.stdout,canonical);
const blocks=[...canonical.matchAll(/```json\n([\s\S]*?)```/g)].map(m=>m[1]);
assert.equal(blocks.length,manifest.length);
manifest.forEach((e,i)=>assert.equal(blocks[i],fs.readFileSync(e.request,'utf8')));
fs.writeFileSync(output+'/rendered.md',canonical);
const faults=[];
function negative(name, mutate, reason) {
  const q=read(manifest[0].request), receipt=read(manifest[0].receipt);
  mutate(q,receipt);
  const entry={kind:'inspect',request:save(name+'-request',q),receipt:save(name+'-receipt',receipt)};
  // A preceding valid entry must not leak partial documentation on a later refusal.
  const path=save(name+'-manifest',[manifest[1],entry]);
  for(const arm of ['native','bb']) {
    const r=run(arm,path);assert.equal(r.status,2);assert.equal(r.stdout,'');assert(r.stderr.includes(reason),r.stderr);
    faults.push({case:name,...r});
  }
}
negative('request-drift',(q)=>{q.requests[1].file=q.requests[0].file;},'request-receipt-mismatch');
negative('two-files-expect-three',(q,r)=>{q.requests[1].file=q.requests[0].file;r.request=q;},'request-cardinality-mismatch');
negative('failed-receipt',(q,r)=>{r.response.result.structuredContent.ok=false;},'receipt-not-success');
negative('receipt-count',(q,r)=>{r.response.result.structuredContent.file_count=2;},'receipt-cardinality-mismatch');
const summary={date:new Date().toISOString(),scope:'Archived data, fresh-process component clocks; shared box; no agent task speed claim',
  positive_runs:rows.length,negative_runs:faults.length,byte_identical_examples:blocks.length,
  median_ms:Object.fromEntries(Object.keys(arms).map(a=>[a,rows.filter(r=>r.arm===a).map(r=>r.wall_ms).sort((x,y)=>x-y)[2]]))};
save('runs',{summary,positive:rows.map(({stdout,...r})=>r),negative:faults});
console.log(JSON.stringify(summary,null,2));
