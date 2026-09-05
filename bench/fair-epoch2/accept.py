"""Independent fresh-process behavior and protected-byte acceptance; no model judgment."""
from pathlib import Path
import subprocess, sys, json

def accept(root):
    root = Path(root).resolve()
    expected = '(ns acme.core)\n(defn stamp [x] (str "retained:" x))\n'
    core = (root/'src/acme/core.clj').read_text()
    # Formatting is not the oracle. A fresh reader resolves the actual Vars below.
    assert '(defn stamp [x] (str "retained:" x))' in core, 'retained owner changed'
    for i in range(21):
        source = (root/f'src/acme/caller_{i}.clj').read_text()
        assert ';; core/ok and core/missing in this comment are intentional.' in source
        assert '(def decoy "core/ok acme.core/missing")' in source
    code = '''(require 'acme.core 'acme.response)
    (assert (every? #(nil? (ns-resolve 'acme.core %)) '[response ok missing]))
    (doseq [s [nil "hello" "" 42 {:x [1 2]}]]
      (assert (= {:status 201 :body s :headers {"content-type" "text/plain"}}
                 ((requiring-resolve 'acme.response/response) 201 s)))
      (doseq [i (range 21)]
        (let [run (requiring-resolve (symbol (str "acme.caller-" i) "run"))]
          (assert (= [{:status 200 :body s :headers {"content-type" "text/plain"}}
                      {:status 404 :body s :headers {"content-type" "text/plain"}}
                      (str "retained:" s)] (run s))))))
    (println "BEHAVIOR-PASS 105 caller cases + 5 direct cases")'''
    p = subprocess.run(['bb','-cp','src','-e',code],cwd=root,capture_output=True,text=True,timeout=30)
    if p.returncode:
        raise ValueError(p.stderr[-4000:])
    return {'accepted':True,'caller_cases':105,'direct_cases':5,'proof':p.stdout.strip()}

if __name__ == '__main__':
    try:
        print(json.dumps(accept(sys.argv[1])))
    except Exception as e:
        print(json.dumps({'accepted':False,'error':str(e)}))
        sys.exit(1)
