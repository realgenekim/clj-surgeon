import pathlib,re,json,collections
base=pathlib.Path('/var/tmp/forge/regns-fx/round7/trace-bytes')
root=base/'repo';rows=[];unknown=[]
mutators={'creat','mkdir','mkdirat','unlink','unlinkat','rename','renameat','renameat2','link','linkat','symlink','symlinkat','rmdir','truncate','chmod','utime','utimes','utimensat'}
fd_writes={'write','writev','pwrite64','pwritev','ftruncate','copy_file_range','sendfile'}
for phase in ['seed','mask','gate']:
    pending={};cwd={}
    for number,line in enumerate((base/(phase+'.strace')).open(),1):
        pid=line.split()[0]
        if '<unfinished ...>' in line:
            pending[pid]=line.replace('<unfinished ...>','').rstrip('\n');continue
        if ' resumed>' in line:
            line=pending.pop(pid)+line.split(' resumed>',1)[1]
        call=re.search(r'^\d+\s+(\w+)\(',line)
        if not call:continue
        op=call[1]
        m=re.search(r'AT_FDCWD<([^>]+)>',line)
        if m:cwd[pid]=m[1]
        success=not re.search(r'= -1',line)
        if op=='chdir' and success:
            q=re.findall(r'"([^"]+)"',line)
            if q:cwd[pid]=str(pathlib.Path(cwd.get(pid,str(root)))/q[0])
        paths=[];kind='metadata'
        if op in {'open','openat','creat'}:
            if op!='creat' and not re.search(r'O_WRONLY|O_RDWR|O_CREAT|O_TRUNC',line):continue
            kind='write-open'
            m=re.search(r'= \d+<([^>]+)>',line)
            paths=[m[1].split('<')[0]] if m else re.findall(r'"([^"]+)"',line)[:1]
        elif op in fd_writes:
            kind='write-syscall'
            # sendfile takes the output first; copy_file_range takes it third.
            fds=re.findall(r'\d+<(/[^>]+)>',line)
            if fds:paths=[fds[-1] if op=='copy_file_range' else fds[0]]
            elif '<pipe:' not in line and '<UNIX' not in line and '<TCP' not in line and '<{eventfd-' not in line:
                unknown.append({'phase':phase,'line':number,'syscall':line.strip()})
        elif op in mutators:
            paths=re.findall(r'"([^"]+)"',line)
        else:continue
        for path in paths:
            path=path.split('<')[0]
            p=pathlib.Path(path)
            if not p.is_absolute():p=pathlib.Path(cwd.get(pid,str(root)))/p
            p=p.resolve()
            rows.append({'phase':phase,'line':number,'pid':int(pid),'operation':op,
                         'kind':kind,'success':success,'path':str(p),
                         'outside_copy':not p.is_relative_to(root)})
    assert not pending,pending
summary={'records':len(rows),'actual_write_syscalls':sum(r['kind']=='write-syscall' and r['success'] for r in rows),
         'outside_paths':len({r['path'] for r in rows if r['outside_copy']}),
         'successful_outside_paths':len({r['path'] for r in rows if r['outside_copy'] and r['success']}),
         'live_tree_records':[r for r in rows if pathlib.Path(r['path']).is_relative_to('/home/forge/src/clj-surgeon-regns')],
         'unresolved_fd_calls':unknown}
(base/'write-destinations.json').write_text(json.dumps({'summary':summary,'records':rows},indent=2)+'\n')
(base/'outside-write-paths.txt').write_text('\n'.join(sorted({r['path'] for r in rows if r['outside_copy']}))+'\n')
print(json.dumps(summary,indent=2))
