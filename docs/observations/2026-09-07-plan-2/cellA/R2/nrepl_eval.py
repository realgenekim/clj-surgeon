#!/usr/bin/env python3
"""Minimal bencode nREPL client: send an eval op, print value/out/err until done."""
import socket, sys, os

def bencode(obj):
    if isinstance(obj, bytes): return str(len(obj)).encode() + b":" + obj
    if isinstance(obj, str): return bencode(obj.encode())
    if isinstance(obj, int): return b"i" + str(obj).encode() + b"e"
    if isinstance(obj, list): return b"l" + b"".join(bencode(x) for x in obj) + b"e"
    if isinstance(obj, dict):
        return b"d" + b"".join(bencode(k) + bencode(v) for k, v in sorted(obj.items())) + b"e"
    raise TypeError(obj)

class Dec:
    def __init__(self, sock):
        self.s = sock; self.buf = b""
    def _need(self, n):
        while len(self.buf) < n:
            d = self.s.recv(65536)
            if not d: raise EOFError
            self.buf += d
    def _peek(self):
        self._need(1); return self.buf[0:1]
    def read(self):
        c = self._peek()
        if c == b"i":
            i = self.buf.index(b"e"); v = int(self.buf[1:i]); self.buf = self.buf[i+1:]; return v
        if c == b"l":
            self.buf = self.buf[1:]; out = []
            while self._peek() != b"e": out.append(self.read())
            self.buf = self.buf[1:]; return out
        if c == b"d":
            self.buf = self.buf[1:]; out = {}
            while self._peek() != b"e":
                k = self.read(); out[k] = self.read()
            self.buf = self.buf[1:]; return out
        # bytestring
        while b":" not in self.buf: self._need(len(self.buf)+1)
        i = self.buf.index(b":"); n = int(self.buf[:i]); self._need(i+1+n)
        v = self.buf[i+1:i+1+n]; self.buf = self.buf[i+1+n:]
        try: return v.decode()
        except UnicodeDecodeError: return v

def main():
    port = int(open(os.path.join(os.environ.get("NREPL_DIR", "/home/forge/src/cc-split-R2"), ".nrepl-port")).read().strip())
    code = sys.stdin.read() if (len(sys.argv) < 2 or sys.argv[1] == "-") else open(sys.argv[1]).read()
    s = socket.create_connection(("127.0.0.1", port), timeout=30)
    s.settimeout(1800)
    s.sendall(bencode({"op": "eval", "code": code, "id": "1"}))
    d = Dec(s); status = 0
    while True:
        m = d.read()
        if "out" in m: sys.stdout.write(m["out"])
        if "err" in m: sys.stderr.write(m["err"])
        if "value" in m: sys.stdout.write("=> " + str(m["value"]) + "\n")
        if "ex" in m: sys.stderr.write("EX " + str(m["ex"]) + "\n"); status = 1
        st = m.get("status") or []
        if "done" in st: break
        if "error" in st: status = 1
    s.close(); sys.exit(status)

main()
