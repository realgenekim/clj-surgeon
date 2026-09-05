"""Provision an untouched helper-extraction fixture; refuse existing paths."""
from pathlib import Path
import subprocess

ROOT = Path('/var/tmp/forge/astra-fair2-data-fx')
ORACLE = Path(__file__).with_name('accept.py').resolve()

def provision(target):
    target.mkdir(parents=True, exist_ok=False)
    def put(name, text):
        p = target/name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text)
    put('deps.edn', '{:paths ["src"]}\n')
    put('.clj-surgeon.edn', '{:verification-profiles {"helper-proof" {:commands [["/usr/bin/python3" "'+str(ORACLE)+'" "."]]}}}\n')
    put('src/acme/core.clj', '''(ns acme.core)
(defn response [status body]
  {:status status :body body :headers {"content-type" "text/plain"}})
(defn ok [body] (response 200 body))
(defn missing [body] (response 404 body))
(defn stamp [x] (str "retained:" x))
''')
    for i in range(21):
        put(f'src/acme/caller_{i}.clj', f'''(ns acme.caller-{i} (:require [acme.core :as core]))
;; core/ok and core/missing in this comment are intentional.
(def decoy "core/ok acme.core/missing")
(defn run [body] [(core/ok body) (core/missing body) (core/stamp body)])
''')
    put('bin/fan-test', '#!/bin/sh\nexec /usr/bin/python3 '+str(ORACLE)+' .\n')
    (target/'bin/fan-test').chmod(0o755)
    put('test/README.md', 'Independent behavioral oracle lives outside this fixture.\n')
    subprocess.run(['git','init','-q',str(target)],check=True)
    subprocess.run(['git','-C',str(target),'add','.'],check=True)
    subprocess.run(['git','-C',str(target),'-c','user.name=Astra','-c','user.email=astra@local','commit','-qm','Frozen helper fixture'],check=True)

if __name__ == '__main__':
    provision(ROOT/'baseline')
