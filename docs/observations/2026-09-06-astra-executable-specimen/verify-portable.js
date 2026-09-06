// Verify in a relocated copy, with a different cwd and fresh generated outputs.
const fs=require('fs'),os=require('os'),path=require('path'),cp=require('child_process'),assert=require('assert');
const root=fs.mkdtempSync(path.join(os.tmpdir(),'surgeon-specimen-'));
try {
  const copy=path.join(root,'copy'), output=path.join(root,'new-results');
  fs.cpSync(__dirname,copy,{recursive:true});
  const manifest=JSON.parse(fs.readFileSync(path.join(copy,'specimens.json')));
  for(const e of manifest) {
    assert(!path.isAbsolute(e.request)&&!path.isAbsolute(e.receipt),'fixture paths must be relative');
    assert(fs.statSync(path.join(copy,e.request)).isFile());
    assert(fs.statSync(path.join(copy,e.receipt)).isFile());
  }
  const r=cp.spawnSync(process.execPath,[path.join(copy,'hand-drive.js'),output],
    {cwd:root,encoding:'utf8',timeout:10000});
  assert.equal(r.status,0,r.stderr||r.error?.message);
  const result=JSON.parse(r.stdout);
  assert.equal(result.positive_runs,10);assert.equal(result.negative_runs,8);
  assert.equal(result.byte_identical_examples,3);
  assert.equal(fs.readFileSync(path.join(copy,'results/runs.json'),'utf8'),
    fs.readFileSync(path.join(__dirname,'results/runs.json'),'utf8'),'historical receipt changed');
  console.log('PASS: relocated example, unrelated cwd, 10 successful renders, 8 refusals, 3 exact examples; historical receipt untouched');
} finally {fs.rmSync(root,{recursive:true,force:true});}
