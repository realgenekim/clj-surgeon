#!/usr/bin/env python3
import socket, sys, os

def enc(o):
    if isinstance(o, bytes): return str(len(o)).encode()+b":"+o
    if isinstance(o, str): b=o.encode(); return str(len(b)).encode()+b":"+b
    if isinstance(o, int): return b"i"+str(o).encode()+b"e"
    if isinstance(o, list): return b"l"+b"".join(enc(x) for x in o)+b"e"
    if isinstance(o, dict):
        return b"d"+b"".join(enc(k)+enc(v) for k,v in sorted(o.items()))+b"e"
    raise ValueError(o)

class Dec:
    def __init__(self, sock):
        self.s=sock; self.buf=b""
    def more(self):
        d=self.s.recv(65536)
        if not d: raise EOFError
        self.buf+=d
    def peek(self):
        while not self.buf: self.more()
        return self.buf[0:1]
    def take(self,n):
        while len(self.buf)<n: self.more()
        r=self.buf[:n]; self.buf=self.buf[n:]; return r
    def val(self):
        c=self.peek()
        if c==b"i":
            self.take(1); n=b""
            while self.peek()!=b"e": n+=self.take(1)
            self.take(1); return int(n)
        if c==b"l":
            self.take(1); r=[]
            while self.peek()!=b"e": r.append(self.val())
            self.take(1); return r
        if c==b"d":
            self.take(1); r={}
            while self.peek()!=b"e":
                k=self.val(); r[k]=self.val()
            self.take(1); return r
        n=b""
        while self.peek()!=b":": n+=self.take(1)
        self.take(1)
        return self.take(int(n)).decode("utf-8","replace")

def main():
    port=int(open(sys.argv[1] if len(sys.argv)>2 else "/home/forge/src/cc-split-R1/.nrepl-port").read().strip()) if False else None
    portfile=os.environ.get("NREPL_PORT_FILE","/home/forge/src/cc-split-R1/.nrepl-port")
    port=int(open(portfile).read().strip())
    code=sys.stdin.read() if (len(sys.argv)>1 and sys.argv[1]=="-") else " ".join(sys.argv[1:])
    s=socket.create_connection(("127.0.0.1",port)); s.settimeout(1800)
    s.sendall(enc({"op":"eval","code":code,"id":"1"}))
    d=Dec(s); out=[]; status_done=False; rc=0
    while not status_done:
        m=d.val()
        if "out" in m: sys.stdout.write(m["out"])
        if "err" in m: sys.stderr.write(m["err"])
        if "value" in m: out.append(m["value"])
        if "ex" in m: rc=1
        st=m.get("status") or []
        if "done" in st: status_done=True
    for v in out: print("=> "+v)
    s.close(); sys.exit(rc)

main()
