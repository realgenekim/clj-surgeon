"""Original-tip destination trace, including actual write syscalls and descendants."""
import pathlib,os,subprocess,json
live=pathlib.Path('/home/forge/src/clj-surgeon-regns')
base=pathlib.Path('/var/tmp/forge/regns-fx/round7/trace-bytes');base.mkdir()
root=base/'repo';root.mkdir()
archive=base/'source.tar'
subprocess.run(['git','-C',str(live),'archive','4a43f8dbe817670d58726ebe14a3a86586fee88e','--output='+str(archive)],check=True)
subprocess.run(['tar','-xf',str(archive),'-C',str(root)],check=True);archive.unlink()
tmp=base/'temp';tmp.mkdir()
env=dict(os.environ,TMPDIR=str(tmp),JAVA_TOOL_OPTIONS='-Djava.io.tmpdir='+str(tmp));env.pop('CLJ_SURGEON_TMPDIR_REEXEC',None)
def run(cmd,name):
    with (base/(name+'.log')).open('w') as log:
        result=subprocess.run(['strace','-f','-yy','-s','256','-e','trace=%file,write,writev,pwrite64,pwritev,ftruncate,copy_file_range,sendfile','-o',str(base/(name+'.strace'))]+cmd,cwd=root,env=env,stdout=log,stderr=subprocess.STDOUT)
    print(name,result.returncode,flush=True)
    return result.returncode
setup=['bb','-Djava.io.tmpdir='+str(tmp),'-m','clj-surgeon.registration-gate-fixture']
assert run(setup+['seed','seed.json'],'seed')==0
assert run(setup+['10'],'mask')==0
argv=json.loads((root/'seed.json').read_text())['argv']
assert run(argv,'gate')==6
