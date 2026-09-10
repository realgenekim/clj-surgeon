#!/usr/bin/env bash
# oracle.sh — INDEPENDENT oracle for the E4 intent on curtaincall-cfp @ 00e8f0fa.
#
#   E4 intent: in src/cfp_scheduler_killer/views/schedule.clj, change the require
#   alias of cfp-scheduler-killer.events from `events` to `ev`, and rewrite the
#   alias USES (there are exactly 2). Everything else must be untouched — in
#   particular the 29 URL string literals that also contain the bytes `events/`.
#
# This oracle does NOT use the repository's gates to decide correctness, and it
# does not use clj-surgeon to compute form boundaries (the tool under test must
# not be its own judge). It uses:
#   A. clj-kondo's static analysis (--config '{:analysis true ...}') for the
#      alias binding and the exact set of var usages through the new alias;
#   B. babashka's bundled rewrite-clj as an independent reader for top-level
#      form boundaries — every form other than the ns form and the two forms
#      that legitimately hold an alias use must be BYTE-identical to 00e8f0fa,
#      and so must every inter-form gap (whitespace + top-level comments);
#   C. the multiset of string literals: every route string must survive with its
#      original `events/` bytes;
#   D. SECONDARY, recorded but never decisive: the repository's own gate for
#      this namespace (clj-kondo findings vs baseline; optional focused kaocha).
#
# Usage: oracle.sh <worktree> [--gate] [--new-alias ev]
# Exit 0 = PASS (A,B,C all hold). Exit 1 = FAIL. The (D) result is printed
# either way and never changes the exit code.
set -uo pipefail

WT=${1:?usage: oracle.sh <worktree> [--gate]}
shift || true
GATE=0
NEW_ALIAS=ev
while [ $# -gt 0 ]; do
  case "$1" in
    --gate) GATE=1; shift;;
    --new-alias) NEW_ALIAS=$2; shift 2;;
    *) echo "oracle.sh: unknown arg $1" >&2; exit 2;;
  esac
done

BASE_COMMIT=00e8f0fa
FILE=src/cfp_scheduler_killer/views/schedule.clj
TARGET_NS=cfp-scheduler-killer.events
KONDO=$HOME/bin/clj-kondo
TMP=$(mktemp -d /var/tmp/forge/oracle.XXXXXX)   # /var/tmp only, never /tmp
trap 'rm -rf "$TMP"' EXIT

[ -f "$WT/$FILE" ] || { echo "oracle: no $FILE under $WT" >&2; exit 2; }

# Pristine bytes straight from git object store — independent of any worktree.
git -C "$WT" show "$BASE_COMMIT:$FILE" > "$TMP/base.clj" || exit 2
cp "$WT/$FILE" "$TMP/cand.clj"

# ---- A. clj-kondo analysis (alias binding + exact var-usage set) ------------
"$KONDO" --lint "$TMP/cand.clj" --config '{:analysis true :output {:format :edn}}' \
  > "$TMP/cand.edn" 2> "$TMP/cand.kondo.err"
"$KONDO" --lint "$TMP/base.clj" --config '{:analysis true :output {:format :edn}}' \
  > "$TMP/base.edn" 2> /dev/null

cat > "$TMP/oracle.bb" <<'BBEOF'
(require '[rewrite-clj.parser :as p]
         '[rewrite-clj.node :as n]
         '[clojure.string :as str])
(import '[java.security MessageDigest])

(def args *command-line-args*)
(def base-file (nth args 0))
(def cand-file (nth args 1))
(def cand-edn  (nth args 2))
(def target-ns (symbol (nth args 3)))
(def new-alias (symbol (nth args 4)))
(def old-alias 'events)

(defn sha [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

;; ---- independent top-level segmentation (rewrite-clj, not clj-surgeon) ----
(defn segments [f]
  (let [root (p/parse-file-all f)]
    (loop [cs (n/children root), gap (StringBuilder.), out []]
      (if-let [c (first cs)]
        (if (n/whitespace-or-comment? c)
          (recur (rest cs) (.append gap (n/string c)) out)
          (recur (rest cs) (StringBuilder.)
                 (conj out {:gap (str gap) :form (n/string c)})))
        (conj out {:gap (str gap) :form nil})))))

(defn form-name [s]
  (let [m (re-find #"^\(\s*(?:ns|def[a-z-]*|comment)\s+\^?[^\s\[]*?([A-Za-z0-9*+!_'?<>=/.-]+)" s)]
    (if (str/starts-with? s "(ns ") "ns" (or (second m) "?"))))

(def base (segments base-file))
(def cand (segments cand-file))

(defn strings-of [f]
  (let [root (p/parse-file-all f)]
    (->> (tree-seq (fn [x] (and (n/inner? x) (seq (n/children x)))) n/children root)
         (filter #(= :token (n/tag %)))
         (keep (fn [x] (try (let [v (n/sexpr x)] (when (string? v) v)) (catch Exception _ nil)))))))

(def results (atom []))
(defn chk [id ok msg] (swap! results conj {:id id :ok ok :msg msg}) ok)

;; ---------------- A. alias binding + var usages ----------------
(def an (:analysis (read-string (slurp cand-edn))))
(def nsu (filter #(= target-ns (:to %)) (:namespace-usages an)))
(def vu  (filter #(= target-ns (:to %)) (:var-usages an)))

(chk "A1-alias-binding"
     (and (= 1 (count nsu)) (= new-alias (:alias (first nsu))))
     (str "namespace-usages -> " target-ns ": " (count nsu)
          ", alias=" (pr-str (map :alias nsu)) " (want exactly 1 with alias " new-alias ")"))

(chk "A2-usage-count" (= 2 (count vu))
     (str "var-usages -> " target-ns ": " (count vu) " (want 2)"))

(chk "A3-usage-set"
     (= #{{:name 'day-hours :alias new-alias :from-var 'agenda-days :arity 1}
          {:name 'day-hours :alias new-alias :from-var 'schedule-page :arity 1}}
        (set (map #(select-keys % [:name :alias :from-var :arity]) vu)))
     (str "usage set = " (pr-str (sort-by :from-var (map #(select-keys % [:name :alias :from-var :arity]) vu)))))

(chk "A4-no-stale-alias"
     (empty? (filter #(= old-alias (:alias %)) (concat nsu vu)))
     (str "residual uses of old alias `" old-alias "`: "
          (count (filter #(= old-alias (:alias %)) (concat nsu vu)))))

;; ---------------- B. preservation of every other top-level form ------------
(def allowed #{"ns" "agenda-days" "schedule-page"})

(chk "B1-form-count" (= (count base) (count cand))
     (str "top-level forms: base " (dec (count base)) ", cand " (dec (count cand))))

(def changed
  (when (= (count base) (count cand))
    (->> (map vector base cand (range))
         (keep (fn [[b c i]]
                 (when (and (:form b) (not= (sha (:form b)) (sha (:form c))))
                   {:idx i :name (form-name (:form b))}))))))

(def unintended (remove #(allowed (:name %)) changed))

(chk "B2-only-intended-forms-changed" (and (some? changed) (empty? unintended))
     (str "forms changed: " (pr-str (map :name changed))
          " | UNINTENDED: " (pr-str (map :name unintended))))

(def gap-mismatch
  (when (= (count base) (count cand))
    (->> (map vector base cand (range))
         (keep (fn [[b c i]] (when (not= (:gap b) (:gap c)) i))))))

(chk "B3-gaps-and-comments-identical" (empty? gap-mismatch)
     (str "inter-form gaps (whitespace + top-level comments) differing at indices: "
          (pr-str gap-mismatch)))

;; the two changed bodies must differ from baseline ONLY in the alias token
(defn unrewrite [s] (str/replace s (str new-alias "/day-hours") (str old-alias "/day-hours")))
(def body-exact
  (when (= (count base) (count cand))
    (every? (fn [[b c]]
              (or (nil? (:form b))
                  (not (#{"agenda-days" "schedule-page"} (form-name (:form b))))
                  (= (:form b) (unrewrite (:form c)))))
            (map vector base cand))))
(chk "B4-changed-bodies-differ-only-by-alias-token" (true? body-exact)
     "the two alias-holding forms are byte-identical to 00e8f0fa once the new alias token is mapped back")

;; ---------------- C. route strings preserved -------------------------------
(def bs (strings-of base-file))
(def cs (strings-of cand-file))
(def broute (filter #(str/includes? % "events/") bs))
(def croute (filter #(str/includes? % "events/") cs))
(def lost (remove (set cs) broute))

(chk "C1-route-string-count" (= (count broute) (count croute))
     (str "string literals containing `events/`: base " (count broute) ", cand " (count croute)))

(chk "C2-every-route-string-survives" (empty? lost)
     (str "BROKEN ROUTE STRINGS: " (count lost)
          (when (seq lost) (str " e.g. " (pr-str (take 3 (distinct lost)))))))

(chk "C3-string-multiset-identical" (= (frequencies bs) (frequencies cs))
     (str "string-literal multiset differs in " (count (remove (set cs) bs)) " baseline strings, "
          (count (remove (set bs) cs)) " new strings"))

;; ---------------- report ---------------------------------------------------
(doseq [{:keys [id ok msg]} @results]
  (println (if ok "  PASS" "  FAIL") id "--" msg))
(let [bad (remove :ok @results)]
  (println)
  (println (if (empty? bad) "ORACLE: PASS" (str "ORACLE: FAIL (" (count bad) " checks: "
                                                (str/join ", " (map :id bad)) ")")))
  (System/exit (if (empty? bad) 0 1)))
BBEOF

echo "=== oracle: $WT ==="
bb "$TMP/oracle.bb" "$TMP/base.clj" "$TMP/cand.clj" "$TMP/cand.edn" "$TARGET_NS" "$NEW_ALIAS"
RC=$?

# ---- D. SECONDARY: the repository's own gate (recorded, never decisive) -----
BF=$(bb -e '(println (count (:findings (read-string (slurp (first *command-line-args*))))))' "$TMP/base.edn" 2>/dev/null)
CF=$(bb -e '(println (count (:findings (read-string (slurp (first *command-line-args*))))))' "$TMP/cand.edn" 2>/dev/null)
echo "  [secondary] clj-kondo findings (bare config): baseline $BF, candidate $CF -> $([ "$BF" = "$CF" ] && echo GREEN || echo RED)"
RK=$( cd "$WT" && "$KONDO" --lint "$FILE" 2>&1 | tail -1 )
echo "  [secondary] clj-kondo in-repo (repo .clj-kondo config): $RK"
if [ "$GATE" = 1 ]; then
  ( cd "$WT" && bin/kaocha unit \
      --focus cfp-scheduler-killer.placeholder-promotion-test \
      --focus cfp-scheduler-killer.public-widgets-test ) > "$TMP/kaocha.log" 2>&1
  KRC=$?
  echo "  [secondary] repo gate (kaocha unit, 2 focused ns): $(tail -3 "$TMP/kaocha.log" | tr '\n' ' ') -> $([ $KRC -eq 0 ] && echo GREEN || echo RED)"
fi
echo "  [note] the secondary signals are NOT part of the verdict; the spike found them green on a file with 19 broken routes."
exit $RC
