// Identical argv on both arms. Caller supplies a frozen, read-only verification.
const fs = require('fs'), path = require('path'), cp = require('child_process'), os = require('os'), assert = require('assert');
const {performance} = require('perf_hooks');
const [tracer, out, cwd, ...argv] = process.argv.slice(2);
if (!tracer || !out || !cwd || !argv.length) throw Error('tracer out cwd argv required');
fs.mkdirSync(out,{recursive:true});
const rows=[];
function execute(args, directory) {
  const load1=os.loadavg()[0], start=performance.now();
  const r=cp.spawnSync(args[0],args.slice(1),{cwd:directory,encoding:'utf8',timeout:60000});
  return {wall_ms:performance.now()-start,load1,status:r.status,signal:r.signal,stdout:r.stdout,stderr:r.stderr,error:r.error?.message};
}
function traced(i) {
  const trace=path.join(out,'pair-'+i+'.jsonl');
  const begin=execute(['node',tracer,'begin',trace,'verification-'+i],cwd);
  if(begin.status!==0) throw Error('begin failed '+begin.stderr);
  const run=execute(['node',tracer,'run',trace,'verify','-','--',...argv],cwd);
  const end=execute(['node',tracer,'end',trace],cwd);
  const report=execute(['node',tracer,'report',trace],cwd);
  fs.writeFileSync(path.join(out,'pair-'+i+'-report.json'),report.stdout);
  return {...run,setup_ms:begin.wall_ms,finish_ms:end.wall_ms,report_ms:report.wall_ms,
    lifecycle_statuses:[begin.status,end.status,report.status]};
}
for(let i=1;i<=5;i++) {
  for(const arm of i%2?['direct','traced']:['traced','direct']) {
    const result=arm==='direct'?execute(argv,cwd):traced(i);
    rows.push({pair:i,arm,...result});
    // Persist before assertion so failed attempts survive too.
    fs.writeFileSync(path.join(out,'runs.json'),JSON.stringify(rows,null,2)+'\n');
    if(result.status!==0 || result.lifecycle_statuses?.some(s=>s!==0)) throw Error('failed run retained: '+arm+' '+i);
  }
}
const median=a=>a.sort((x,y)=>x-y)[Math.floor(a.length/2)];
for(const r of rows) assert.equal(r.stdout,rows[0].stdout,'verification outputs differ');
const direct=median(rows.filter(r=>r.arm==='direct').map(r=>r.wall_ms));
const tracedWall=median(rows.filter(r=>r.arm==='traced').map(r=>r.wall_ms));
const summary={utc:new Date().toISOString(),pairs:5,direct_median_ms:direct,traced_run_median_ms:tracedWall,
  overhead_ms:tracedWall-direct,ratio_traced_over_direct:tracedWall/direct,
  traced_whole_lifecycle_median_ms:median(rows.filter(r=>r.arm==='traced').map(r=>r.wall_ms+r.setup_ms+r.finish_ms+r.report_ms)),
  scope:'Shared-box fresh processes, same frozen verification argv. Setup/end/report costs separate. Not a whole-task speed comparison.'};
fs.writeFileSync(path.join(out,'summary.json'),JSON.stringify(summary,null,2)+'\n');
console.log(JSON.stringify(summary,null,2));
